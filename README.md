# abacus-matrix

[![Maven Central](https://img.shields.io/maven-central/v/com.landawn.abacus/abacus-matrix.svg)](https://central.sonatype.com/artifact/com.landawn.abacus/abacus-matrix)
[![Javadocs](https://img.shields.io/badge/javadoc-reference-brightgreen.svg)](https://www.javadoc.io/doc/com.landawn.abacus/abacus-matrix)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)

`abacus-matrix` provides mutable, rectangular, array-backed matrices for every Java primitive
type and for objects. Use it to manipulate grids, reshape and combine tabular data, transform
values, traverse rows and columns, and perform element-wise arithmetic or matrix multiplication.
Primitive matrices keep values in primitive arrays, avoiding boxing for storage and primitive
operations.

This guide describes the source in this checkout: **abacus-matrix 3.9.0, Java 21 or later**.
Dependency versions below match [pom.xml](./pom.xml); they do not identify the latest published
release. Published Javadocs and the generated API index may describe an earlier version.

## Contents

- [Installation](#installation)
- [Quick start](#quick-start)
- [Choosing a matrix type](#choosing-a-matrix-type)
- [Creating matrices](#creating-matrices)
- [Shape, indexes, and empty matrices](#shape-indexes-and-empty-matrices)
- [Storage ownership and mutation](#storage-ownership-and-mutation)
- [Accessing and updating values](#accessing-and-updating-values)
- [Copying regions and changing shape](#copying-regions-and-changing-shape)
- [Mapping and converting types](#mapping-and-converting-types)
- [Arithmetic and overflow](#arithmetic-and-overflow)
- [Boolean operations](#boolean-operations)
- [Stacking and zipping](#stacking-and-zipping)
- [Traversal, streams, and aggregation](#traversal-streams-and-aggregation)
- [Object matrices and Dataset interoperability](#object-matrices-and-dataset-interoperability)
- [Parallel execution](#parallel-execution)
- [Validation and common pitfalls](#validation-and-common-pitfalls)
- [API reference at a glance](#api-reference-at-a-glance)
- [Building and testing](#building-and-testing)
- [Further documentation](#further-documentation)
- [License](#license)

## Installation

Use JDK 21 or later. All matrix classes are in `com.landawn.abacus.matrix`.
The project declares both `abacus-common` and `abacus-extra` with Maven `provided` scope,
so applications should add both explicitly as compile and runtime dependencies. In particular,
flat-array mutation uses the `Arrays` utilities supplied by `abacus-extra`.

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>com.landawn.abacus</groupId>
        <artifactId>abacus-matrix</artifactId>
        <version>3.9.0</version>
    </dependency>
    <dependency>
        <groupId>com.landawn.abacus</groupId>
        <artifactId>abacus-common</artifactId>
        <version>8.3.0</version>
    </dependency>
    <dependency>
        <groupId>com.landawn.abacus</groupId>
        <artifactId>abacus-extra</artifactId>
        <version>3.8.12</version>
    </dependency>
</dependencies>
```

### Gradle

```groovy
dependencies {
    implementation 'com.landawn.abacus:abacus-matrix:3.9.0'
    implementation 'com.landawn.abacus:abacus-common:8.3.0'
    implementation 'com.landawn.abacus:abacus-extra:3.8.12'
}
```

Parallel stream implementations are included in the configured `abacus-common` dependency.
The library checks for their availability at runtime and falls back to sequential execution
if they cannot be loaded. See [Parallel execution](#parallel-execution).

If the checkout's version is unavailable from your configured repository, install this checkout
locally using the commands in [Building and testing](#building-and-testing).

## Quick start

This complete example creates a matrix, transforms its values, and multiplies it by another
matrix. `map`, `transpose`, and arithmetic methods return results without changing their inputs.

```java
import com.landawn.abacus.matrix.IntMatrix;

public class MatrixQuickStart {
    public static void main(String[] args) {
        IntMatrix a = IntMatrix.wrap(new int[][] {
            {1, 2, 3},
            {4, 5, 6}
        });
        IntMatrix b = IntMatrix.wrap(new int[][] {
            {7, 8},
            {9, 10},
            {11, 12}
        });

        System.out.println(a.rowCount());       // 2
        System.out.println(a.columnCount());    // 3
        System.out.println(a.get(1, 2));         // 6

        IntMatrix doubled = a.map(value -> value * 2);
        IntMatrix transposed = a.transpose();   // 3 x 2
        IntMatrix product = a.matrixMultiply(b);
        product.println();
        // [58, 64]
        // [139, 154]

        int total = a.rowMajorStream().sum();    // 21
        a.set(0, 0, 10);                         // changes a in place
        System.out.println(doubled.get(0, 0));  // 2 (independent result)
    }
}
```

The remaining Java examples are method-body snippets with their required imports shown.
Each snippet creates its own input matrices.

## Choosing a matrix type

| Class | Storage | Type-specific features |
| --- | --- | --- |
| `BooleanMatrix` | `boolean[][]` | `and`, `or`, `xor`, `countTrue`, `allTrue`, `anyTrue` |
| `ByteMatrix` | `byte[][]` | Arithmetic, checked arithmetic, widened results |
| `ShortMatrix` | `short[][]` | Arithmetic, checked arithmetic, widened results |
| `IntMatrix` | `int[][]` | Arithmetic, checked arithmetic, widened results |
| `LongMatrix` | `long[][]` | Arithmetic and checked arithmetic |
| `CharMatrix` | `char[][]` | Arithmetic on UTF-16 code-unit values; results narrow to `char` |
| `FloatMatrix` | `float[][]` | Floating-point arithmetic and numeric conversions |
| `DoubleMatrix` | `double[][]` | Floating-point arithmetic and numeric conversions |
| `Matrix<T>` | `T[][]` | Objects, nullable cells, mapping to all primitive types, Dataset export |

All nine types support copying, access, updates, shape transformations, stacking, mapping,
zipping, and traversal. `AbstractMatrix` defines the shared API; normally use a concrete class.
`Matrices` supplies static composition and index helpers. `ParallelMode` controls eligible
automatic parallel operations.

Choose a primitive class when cells represent primitive values. Choose `Matrix<T>` for strings,
domain objects, mixed values stored as `Object`, or cells that need to represent `null`.

## Creating matrices

### Arrays, dimensions, and generated values

`wrap(...)` and the public array constructors validate the input and share its row arrays.
`copyOf(...)` copies the array structure and is preferable when the caller will continue modifying
the input. Both require non-null, equal-length rows; `wrap` additionally rejects repeated row
array identities. See [Storage ownership and mutation](#storage-ownership-and-mutation).

```java
import com.landawn.abacus.matrix.IntMatrix;

IntMatrix wrapped = IntMatrix.wrap(new int[][] {{1, 2}, {3, 4}});
IntMatrix copied = IntMatrix.copyOf(new int[][] {{1, 2}, {3, 4}});
IntMatrix zeros = IntMatrix.wrap(new int[2][3]);
IntMatrix filled = IntMatrix.empty().resize(2, 3, 7);
// filled: [[7, 7, 7], [7, 7, 7]]

IntMatrix sequence = IntMatrix.range(1, 7).reshape(2, 3);
// sequence: [[1, 2, 3], [4, 5, 6]]
IntMatrix descending = IntMatrix.rangeClosed(5, 1, -2);
// descending: [[5, 3, 1]]

IntMatrix coordinates = IntMatrix.wrap(new int[2][3]);
coordinates.updateAll((row, column) -> row * 10 + column);
// coordinates: [[0, 1, 2], [10, 11, 12]]
```

| Factory | Behavior |
| --- | --- |
| `empty()` | A `0 x 0` matrix; primitive types may reuse a shared singleton |
| `wrap(array)` / constructor | Snapshot the outer array; share the supplied row arrays |
| `copyOf(array)` | Copy the outer array and each row |
| `range(start, end[, step])` | One row of values, excluding `end`; available on byte, short, char, int, and long types |
| `rangeClosed(start, end[, step])` | Range including the endpoint when reached by the step |
| `random(rows, columns[, generator])` | Primitive matrix filled using the type's random generator operations |
| `randomRow(columns[, generator])` | Primitive random matrix with one row |
| `ofMainDiagonal(values)` | Square matrix with the supplied main diagonal and default values elsewhere |
| `ofAntiDiagonal(values)` | Square matrix with the supplied anti-diagonal and default values elsewhere |
| `ofDiagonals(main, anti)` | Square matrix; one diagonal may be null, but not both; supplied lengths must agree |

A range step must be nonzero. Random factories accept a `java.util.random.RandomGenerator`,
so you can supply a seeded generator such as `new java.util.Random(42)` for repeatable input.
Floating-point random factories produce values in `[0.0, 1.0)`.

For odd-sized `ofDiagonals(...)` results, the main diagonal wins at the shared center cell.
Default values are `0` for numeric types, `'\0'` for `char`, `false` for boolean, and `null` for objects.

### Typed object storage

```java
import com.landawn.abacus.matrix.Matrices;
import com.landawn.abacus.matrix.Matrix;

Matrix<String> words = Matrix.wrap(new String[][] {{"one", "two"}});
Matrix<String> blank = Matrix.empty(String.class).resize(2, 3);
String[][] array = Matrices.newMatrixArray(2, 3, String.class);
Matrix<String> owned = Matrix.copyOf(String.class, array);
```

Object matrices retain a runtime element type for array allocation. Prefer explicit type tokens
when constructing typed empty matrices or changing element types. `Matrix.empty()` by itself
returns an `Object`-typed matrix.

## Shape, indexes, and empty matrices

Rows and columns use **zero-based indexes**. Shape is written as `rows x columns`.
`rowCount()` and `columnCount()` return `int`; `elementCount()` returns `long`.
`isEmpty()` means there are no cells, so it is true when either dimension is zero.

All index ranges are **half-open**: the `from` index is included and the `to` index is excluded.
For example, `copyRegion(0, 2, 1, 3)` selects rows 0 and 1, columns 1 and 2.
Methods taking a `Point` use `com.landawn.abacus.util.Sheet.Point`, constructed as
`Point.of(row, column)`.

The library preserves `0 x 0`, `N x 0`, and `0 x N` as distinct shapes:

```java
import com.landawn.abacus.matrix.IntMatrix;
import com.landawn.abacus.matrix.Matrix;

IntMatrix noRows = IntMatrix.empty().resize(0, 3);  // 0 x 3
IntMatrix noColumns = noRows.transpose();          // 3 x 0
long columns = noRows.columnStreams().count();     // 3 empty column streams
long cells = noColumns.elementCount();            // 0
Matrix<String> headersOnly = Matrix.empty(String.class, 3); // 0 x 3
```

A bare Java array cannot record the column count when its length is zero. Consequently,
`IntMatrix.wrap(new int[0][3])` has shape `0 x 0`. Use `resize(0, 3)`, a shape transformation,
or `Matrix.empty(elementType, columnCount)` to retain a zero-row matrix's width.
If exporting a zero-row backing array, carry `columnCount()` separately.

Element-wise binary operations require both dimensions to agree. Matrix multiplication requires
the left column count to equal the right row count. There is no implicit broadcasting.

## Storage ownership and mutation

### Shared rows versus independent copies

```java
import com.landawn.abacus.matrix.IntMatrix;

int[][] source = {{1, 2}, {3, 4}};
IntMatrix shared = IntMatrix.wrap(source);
IntMatrix owned = IntMatrix.copyOf(source);
source[0][0] = 99;
int sharedValue = shared.get(0, 0); // 99
int ownedValue = owned.get(0, 0);   // 1

source[0] = new int[] {-1, -1};
int retainedRow = shared.get(0, 0); // 99: the outer array was snapshotted

int[] liveRow = shared.rowView(1);
liveRow[0] = 30;                    // shared.get(1, 0) is now 30
int[] independentRow = shared.rowCopy(1);
independentRow[0] = -1;             // shared is unchanged
```

| API | Ownership / effect |
| --- | --- |
| `wrap`, public constructor | New outer array; shared row arrays |
| `copyOf`, `copy`, `copyRows`, `copyColumns`, `copyRegion` | Independent array storage |
| `rowView` | Live row array; writes affect the matrix |
| `rowCopy`, `columnCopy`, diagonal `*Copy` methods | Independent arrays |
| `unsafeBackingArray` | The actual internal outer array and rows |
| `flatten` | Independent list container in row-major order |
| Shape transformations, `map`, arithmetic, `zipWith`, stacking | Separate result storage; empty results may reuse a singleton |

For `Matrix<T>`, independent storage means new arrays or containers, **not cloned element
objects**. Mutating a referenced object can be visible through multiple matrices.

Every logical row must have its own array identity. `wrap(row, row)` throws
`IllegalArgumentException`; `copyOf(row, row)` succeeds because it clones each occurrence.
Avoid replacing rows through `unsafeBackingArray()`: rows must remain non-null, rectangular,
and identity-distinct, and dimensions are fixed for the lifetime of an instance.

### Which methods mutate?

Methods named `set*`, `update*`, `fill`, `replaceIf`, `copyFrom`, or ending in `InPlace` mutate
the receiver. `mutateViaFlatArray` also mutates it. Shape-changing methods return a result
instead of changing the receiver's dimensions.

```java
import com.landawn.abacus.matrix.IntMatrix;

IntMatrix matrix = IntMatrix.wrap(new int[][] {{4, 1}, {3, 2}});
IntMatrix doubled = matrix.map(value -> value * 2); // matrix unchanged
matrix.updateAll(value -> value + 10);             // matrix now [[14, 11], [13, 12]]
matrix.mutateViaFlatArray(java.util.Arrays::sort);  // matrix now [[11, 12], [13, 14]]
```

`mutateViaFlatArray` provides a temporary row-major array and copies its contents back only if
the action returns normally. A zero-row matrix does not invoke the action; a matrix with rows
but no columns invokes it once with an empty array. Other callback-based updates may leave
already-completed changes in place if a callback throws. For object matrices, even a temporary
array still contains references to the same element objects.

Matrices are mutable and **not thread-safe**. Coordinate concurrent access yourself, including
access through arrays shared by `wrap` or exposed by `rowView`.

## Accessing and updating values

```java
import com.landawn.abacus.matrix.IntMatrix;
import com.landawn.abacus.util.Sheet.Point;

IntMatrix matrix = IntMatrix.rangeClosed(1, 6).reshape(2, 3);
int value = matrix.get(Point.of(1, 2)); // 6
matrix.set(0, 1, 20);
matrix.setRow(1, new int[] {40, 50, 60});
matrix.setColumn(0, new int[] {10, 40});
matrix.updateRow(0, x -> x + 1);
matrix.updateColumn(2, x -> x * 2);
matrix.replaceIf(x -> x > 50, 50);
// matrix: [[11, 21, 8], [40, 50, 50]]
```

`setRow` requires exactly `columnCount()` values; `setColumn` requires exactly `rowCount()`
values. They copy values into existing storage. `updateAll(value -> ...)` maps existing values,
while `updateAll((row, column) -> ...)` generates replacements from coordinates.
`replaceIf` likewise has value-predicate and coordinate-predicate overloads.

| Operation | Methods / behavior |
| --- | --- |
| Cell access | `get(row, column)`, `set(row, column, value)`; both also accept `Point` |
| Neighbor values | `valueAbove`, `valueBelow`, `valueLeft`, `valueRight`; an absent neighbor returns an empty optional-like value |
| Neighbor coordinates | `adjacent4Points`, `adjacent8Points`; include only valid surrounding cells |
| Rows and columns | `rowView`, `rowCopy`, `columnCopy`, `setRow`, `setColumn`, `updateRow`, `updateColumn` |
| Diagonals | `mainDiagonalCopy`, `antiDiagonalCopy`, `setMainDiagonal`, `setAntiDiagonal`, `updateMainDiagonal`, `updateAntiDiagonal` |
| Whole matrix | `fill(value)`, `updateAll(...)`, `replaceIf(...)` |
| Partial overwrite | `copyFrom(source)` or `copyFrom(destRow, destColumn, source)` |

Neighbor lookups require a valid starting cell. Primitive neighbor methods return the matching
abacus-common optional type; object neighbors return `Nullable<T>`, which distinguishes an
absent neighbor from a present neighbor whose value is `null`.

Diagonals contain `min(rowCount(), columnCount())` elements, including for rectangular matrices.
The main diagonal visits `(i, i)`; the anti-diagonal visits `(i, columnCount() - 1 - i)`.
Diagonal setters require exactly that many values.

`copyFrom` copies only the overlap with the destination, skips null source rows, and accepts
source rows of different lengths. It preserves cells outside the copied area. Unlike constructors,
it does not require a rectangular source. Its destination coordinates may equal the corresponding
dimension, in which case there is nothing to copy.

## Copying regions and changing shape

### Reshape versus resize versus pad

| Method | Layout rule | Size constraint |
| --- | --- | --- |
| `reshape(rows, columns)` | Read and write in row-major order | New cell count must equal the old count |
| `reshapeAndPad(rows, columns)` | Row-major relayout, followed by default-valued cells | New cell count must be at least the old count |
| `reshapeAndPadToColumnCount(columns)` | Choose enough rows for the requested width | Width must be positive |
| `resize(rows, columns[, defaultValue])` | Keep cells at their existing coordinates in the top-left overlap | Can grow or shrink; new cells use the default |
| `pad(top, bottom, left, right[, defaultValue])` | Surround the matrix with a border | All padding widths must be nonnegative |

```java
import com.landawn.abacus.matrix.IntMatrix;

IntMatrix matrix = IntMatrix.rangeClosed(1, 6).reshape(2, 3);
IntMatrix reshaped = matrix.reshape(3, 2);           // [[1, 2], [3, 4], [5, 6]]
IntMatrix padded = matrix.reshapeAndPad(2, 4);       // [[1, 2, 3, 4], [5, 6, 0, 0]]
IntMatrix resized = matrix.resize(2, 4, -1);         // [[1, 2, 3, -1], [4, 5, 6, -1]]
IntMatrix cropped = matrix.resize(1, 2);            // [[1, 2]]
IntMatrix region = matrix.copyRegion(0, 2, 1, 3);   // [[2, 3], [5, 6]]
IntMatrix border = matrix.pad(1, 1, 1, 1, -1);       // 4 x 5; input starts at (1, 1)
```

`copyRows(from, to)` keeps all columns; `copyColumns(from, to)` keeps all rows.
These methods and `copyRegion` return copies rather than views. Empty slices retain the
dimension that was not sliced away.

### Transpose, rotate, flip, and repeat

For `[[1, 2, 3], [4, 5, 6]]`:

| Method | Result |
| --- | --- |
| `transpose()` | `[[1, 4], [2, 5], [3, 6]]` |
| `rotate90()` | `[[4, 1], [5, 2], [6, 3]]` (clockwise) |
| `rotate180()` | `[[6, 5, 4], [3, 2, 1]]` |
| `rotate270()` | `[[3, 6], [2, 5], [1, 4]]` (clockwise) |
| `flipHorizontally()` | `[[3, 2, 1], [6, 5, 4]]` (reverse columns) |
| `flipVertically()` | `[[4, 5, 6], [1, 2, 3]]` (reverse rows) |

`flipHorizontallyInPlace()` and `flipVerticallyInPlace()` modify the receiver.
Rotations and transposition return independent results.

`repeatElements(rowRepeats, columnRepeats)` expands each cell into a block.
`repeatMatrix(rowRepeats, columnRepeats)` tiles the entire matrix. Repeat counts are
nonnegative factors, not counts of extra copies:

```java
import com.landawn.abacus.matrix.IntMatrix;

IntMatrix row = IntMatrix.wrap(new int[][] {{1, 2}});
IntMatrix blocks = row.repeatElements(2, 2); // [[1, 1, 2, 2], [1, 1, 2, 2]]
IntMatrix tiles = row.repeatMatrix(2, 2);    // [[1, 2, 1, 2], [1, 2, 1, 2]]
```

## Mapping and converting types

`map` transforms each value into a new matrix. Primitive types provide
`mapToObj(mapper, targetElementType)`, and numeric types provide selected primitive projections
such as `mapToInt`, `mapToLong`, and `mapToDouble`. For the same primitive type, use `map`.
`Matrix<T>` provides `mapToBoolean`, `mapToByte`, `mapToChar`, `mapToShort`, `mapToInt`,
`mapToLong`, `mapToFloat`, and `mapToDouble`.

```java
import com.landawn.abacus.matrix.DoubleMatrix;
import com.landawn.abacus.matrix.IntMatrix;
import com.landawn.abacus.matrix.Matrix;

Matrix<String> words = Matrix.wrap(new String[][] {{"one", null}, {"three", "four"}});
Matrix<String> upper = words.map(s -> s == null ? "" : s.toUpperCase(java.util.Locale.ROOT));
IntMatrix lengths = words.mapToInt(s -> s == null ? 0 : s.length());
// lengths: [[3, 0], [5, 4]]
Matrix<Integer> boxedLengths = words.map(s -> s == null ? 0 : s.length(), Integer.class);
Matrix<String> labels = lengths.mapToObj(n -> "length=" + n, String.class);
DoubleMatrix halves = lengths.mapToDouble(n -> n / 2.0);
```

All primitive classes have `boxed()` and a static `unbox(Matrix<Wrapper>)`. Unboxing converts
null cells to the primitive default value, such as `0` or `false`. This differs from a custom
mapper that tries to unbox null, which can throw `NullPointerException`.

```java
import com.landawn.abacus.matrix.IntMatrix;
import com.landawn.abacus.matrix.Matrix;

Matrix<Integer> boxed = Matrix.wrap(new Integer[][] {{1, null, 3}});
IntMatrix primitive = IntMatrix.unbox(boxed);  // [[1, 0, 3]]
Matrix<Integer> roundTrip = primitive.boxed(); // [[1, 0, 3]]; null is not restored
```

Numeric classes also expose conversions such as `toIntMatrix()`, `toLongMatrix()`,
`toFloatMatrix()`, and `toDoubleMatrix()` where supported, plus selected `from(...)` array
factories. Conversions allocate new storage and follow Java numeric conversion rules.
For example, converting double to int truncates toward zero, maps `NaN` to zero, and saturates
out-of-range values to the int bounds. Converting a large integer to float or double can lose
precision. Use an explicit mapper when you need rounding or validation instead.

## Arithmetic and overflow

### Element-wise operations and matrix multiplication

`add` and `subtract` combine corresponding cells and require identical shapes.
`matrixMultiply` performs the linear-algebra product: an `m x k` left operand and `k x n`
right operand produce an `m x n` result.

Use `zipWith` for element-wise multiplication or division, and `map` for scalar operations:

```java
import com.landawn.abacus.matrix.DoubleMatrix;
import com.landawn.abacus.matrix.IntMatrix;

IntMatrix a = IntMatrix.wrap(new int[][] {{1, 2}, {3, 4}});
IntMatrix b = IntMatrix.wrap(new int[][] {{5, 6}, {7, 8}});
IntMatrix sum = a.add(b);                           // [[6, 8], [10, 12]]
IntMatrix difference = b.subtract(a);              // [[4, 4], [4, 4]]
IntMatrix elementProducts = a.zipWith(b, (x, y) -> x * y);
// [[5, 12], [21, 32]]
IntMatrix product = a.matrixMultiply(b);           // [[19, 22], [43, 50]]
DoubleMatrix scaled = a.mapToDouble(x -> x * 0.5);  // [[0.5, 1.0], [1.5, 2.0]]
```

Arithmetic returns new matrices. `Matrix<T>` has no built-in numeric arithmetic; use mapping
or zipping for types such as `BigDecimal`. `BooleanMatrix` provides logical operations instead.

### Choosing overflow behavior

Ordinary integer arithmetic uses Java overflow and narrowing semantics; it does not report
overflow. Choose the checked or widened methods when their contracts match your needs:

| Input type | `addExact` / `subtractExact` / `matrixMultiplyExact` | `addWidened` / `subtractWidened` | `matrixMultiplyWidened` |
| --- | --- | --- | --- |
| `ByteMatrix` | Return `ByteMatrix`; throw on overflow | `IntMatrix` | `LongMatrix` |
| `ShortMatrix` | Return `ShortMatrix`; throw on overflow | `IntMatrix` | `LongMatrix` |
| `IntMatrix` | Return `IntMatrix`; throw on overflow | `LongMatrix` | `LongMatrix` |
| `LongMatrix` | Return `LongMatrix`; throw on overflow | Not provided | Not provided |
| `CharMatrix`, `FloatMatrix`, `DoubleMatrix` | Not provided | Not provided | Not provided |

`IntMatrix.matrixMultiplyWidened` can still overflow its long accumulator. Use
`matrixMultiplyWidenedExact` to check long accumulation. `IntMatrix.matrixMultiplyExact`
also checks long intermediate sums and requires each finished cell to fit in int.
`LongMatrix.matrixMultiplyExact` checks each product and each intermediate sum; it can throw
even when later terms would bring the final mathematical result back into range.
Checked operations throw `ArithmeticException` and do not modify either operand.

```java
import com.landawn.abacus.matrix.IntMatrix;
import com.landawn.abacus.matrix.LongMatrix;

IntMatrix max = IntMatrix.wrap(new int[][] {{Integer.MAX_VALUE}});
IntMatrix one = IntMatrix.wrap(new int[][] {{1}});
IntMatrix wrapped = max.add(one);          // [[-2147483648]]
LongMatrix widened = max.addWidened(one);  // [[2147483648]]
try {
    max.addExact(one);
    throw new AssertionError("Expected overflow");
} catch (ArithmeticException expected) {
    // Choose a wider result type or handle out-of-range input.
}
```

Floating-point arithmetic follows Java floating-point behavior, including rounding, infinities,
and `NaN`. There is no built-in tolerance parameter for matrix equality; implement approximate
comparison explicitly when needed.

## Boolean operations

`and`, `or`, and `xor` combine equal-shaped boolean matrices and return new results.
Use `map(value -> !value)` for negation.

```java
import com.landawn.abacus.matrix.BooleanMatrix;

BooleanMatrix a = BooleanMatrix.wrap(new boolean[][] {{true, false}, {true, true}});
BooleanMatrix b = BooleanMatrix.wrap(new boolean[][] {{true, true}, {false, true}});
BooleanMatrix both = a.and(b);        // [[true, false], [false, true]]
BooleanMatrix either = a.or(b);       // all true
BooleanMatrix different = a.xor(b);   // [[false, true], [true, false]]
BooleanMatrix inverse = a.map(x -> !x);
long count = a.countTrue();           // 3
boolean all = a.allTrue();            // false
boolean any = a.anyTrue();            // true
```

On an empty boolean matrix, `countTrue()` is zero, `allTrue()` is true, and `anyTrue()` is false.
Count false cells with `elementCount() - countTrue()`.

## Stacking and zipping

Stacking concatenates matrices. Vertical stacking requires equal column counts; horizontal
stacking requires equal row counts. Inputs are copied into the result in encounter order.

```java
import java.util.List;
import com.landawn.abacus.matrix.IntMatrix;
import com.landawn.abacus.matrix.Matrices;

IntMatrix a = IntMatrix.wrap(new int[][] {{1, 2}});
IntMatrix b = IntMatrix.wrap(new int[][] {{3, 4}});
IntMatrix c = IntMatrix.wrap(new int[][] {{5, 6}});
IntMatrix vertical = a.stackVertically(b);           // [[1, 2], [3, 4]]
IntMatrix horizontal = a.stackHorizontally(b);       // [[1, 2, 3, 4]]
IntMatrix allRows = Matrices.stackVertically(List.of(a, b, c));
// [[1, 2], [3, 4], [5, 6]]
```

Zipping combines values at the same coordinate, so all inputs must have identical shapes:

```java
import java.util.List;
import com.landawn.abacus.matrix.DoubleMatrix;
import com.landawn.abacus.matrix.IntMatrix;
import com.landawn.abacus.matrix.Matrices;

IntMatrix a = IntMatrix.wrap(new int[][] {{1, 2}});
IntMatrix b = IntMatrix.wrap(new int[][] {{3, 4}});
IntMatrix c = IntMatrix.wrap(new int[][] {{5, 6}});
IntMatrix maximum = a.zipWith(b, Integer::max);            // [[3, 4]]
IntMatrix sum = a.zipWith(b, c, (x, y, z) -> x + y + z);   // [[9, 12]]
IntMatrix folded = Matrices.zip(List.of(a, b, c), (x, y) -> x + y);
DoubleMatrix mean = Matrices.zipToDouble(a, b, (x, y) -> ((double) x + y) / 2);
// mean: [[2.0, 3.0]]
```

Instance `zipWith` supports pairs and triples across all concrete types. The static `Matrices.zip`
families cover byte, int, long, double, and object matrices; use the instance methods for the
other primitive types. Cross-type static result families include `zipToInt`, `zipToLong`,
`zipToDouble`, and `zipToObj`; available overloads depend on the input type.

Collection stacking and zipping require a non-null, nonempty collection containing no null
matrices. Binary collection zips fold each coordinate in collection iteration order:
`((first op second) op third) ...`. For a single input, they return a copy without invoking
the binary operator. Collection callback overloads that receive all values at a coordinate
also exist; some have a `reuseBuffer` flag. When enabled, treat the supplied array as temporary
and do not retain it after the callback returns.

## Traversal, streams, and aggregation

Streams are **abacus-common streams**, in `com.landawn.abacus.util.stream`, rather than
`java.util.stream` types. Primitive matrices generally expose specialized streams;
`BooleanMatrix` exposes `Stream<Boolean>`, and `Matrix<T>` exposes `Stream<T>`.
Streams read matrix storage lazily; they are not snapshots. Copy first if you need a stable
snapshot, and obtain a fresh stream for each terminal operation.

```java
import com.landawn.abacus.matrix.IntMatrix;

IntMatrix matrix = IntMatrix.rangeClosed(1, 6).reshape(2, 3);
int[] rowOrder = matrix.rowMajorStream().toArray();       // [1, 2, 3, 4, 5, 6]
int[] columnOrder = matrix.columnMajorStream().toArray(); // [1, 4, 2, 5, 3, 6]
int total = matrix.rowMajorStream().sum();                // 21
long widerTotal = matrix.toLongMatrix().rowMajorStream().sum();
int firstRowTotal = matrix.rowMajorStream(0, 1).sum();     // 6
int secondColumnTotal = matrix.columnMajorStream(1, 2).sum(); // 7
var rowTotals = matrix.rowStreams().map(row -> row.sum()).toList(); // [6, 15]
var columnTotals = matrix.columnStreams().map(column -> column.sum()).toList(); // [5, 7, 9]
var minimum = matrix.rowMajorStream().min();              // OptionalInt containing 1
var average = matrix.rowMajorStream().average();          // OptionalDouble containing 3.5
```

Numeric aggregations such as sum, min, max, and average are stream operations, not dedicated
matrix methods. Min, max, and average return optional results for empty streams. The chosen
stream type also determines the aggregation's numeric range; convert before aggregating if
you need a wider result.

| Traversal family | Order / selection |
| --- | --- |
| `rowMajorStream()` | Left to right within each row, then the next row |
| `columnMajorStream()` | Top to bottom within each column, then the next column |
| `rowMajorStream(fromRow, toRow)` | All cells in a range of complete rows |
| `columnMajorStream(fromColumn, toColumn)` | All cells in a range of complete columns |
| `rowStreams`, `columnStreams` | One stream per row or column; range overloads available |
| `mainDiagonalStream`, `antiDiagonalStream` | Values along the corresponding diagonal |
| `rowMajorPoints`, `columnMajorPoints` | Coordinates in the corresponding order |
| `rowPoints`, `columnPoints` | Streams of coordinate streams |
| `mainDiagonalPoints`, `antiDiagonalPoints` | Coordinates along the corresponding diagonal |
| `forEach` | Value callback; overload for a rectangular region |
| `forEachIndices` | Coordinate callback; overloads for a region or for receiving the matrix too |

```java
import com.landawn.abacus.matrix.IntMatrix;
import com.landawn.abacus.matrix.Matrices;

IntMatrix matrix = IntMatrix.wrap(new int[2][3]);
matrix.forEachIndices((row, column) -> matrix.set(row, column, row + column));
// [[0, 1, 2], [1, 2, 3]]
var neighbors = matrix.adjacent4Points(0, 0).toList(); // points (0, 1), (1, 0)

// Generate values from dimensions without first allocating a matrix.
int[] values = Matrices.mapIndicesToInt(2, 3, (row, column) -> row * 10 + column, false).toArray();
// [0, 1, 2, 10, 11, 12]; false requests sequential execution
```

`Matrices` also provides `forEachIndices`, object-valued `mapIndices`, and
`forEachCartesianIndices` for combinations of coordinates from two matrices.
`flatten()` materializes a row-major primitive list (for example, `IntList`) or a `List<T>`.
It requires the cell count to fit in a single list and throws `IllegalStateException` when
it exceeds `Integer.MAX_VALUE`.

## Object matrices and Dataset interoperability

`Matrix<T>` accepts null cell values. Its callbacks receive those values, so handle null
explicitly when the operation requires it. Its runtime array type must accept every stored
value; Java array type checks still apply. To map to a different type, supply its class token,
as in `strings.map(String::length, Integer.class)`.

Use `Matrix<Object>` with an actual `Object[][]` for heterogeneous tables:

```java
import java.util.List;
import com.landawn.abacus.matrix.Matrix;
import com.landawn.abacus.util.Dataset;

Matrix<Object> table = Matrix.wrap(new Object[][] {
    {"Ada", 36},
    {"Grace", 40}
});
Dataset records = table.toDataset(List.of("name", "age"));
// Two records; columns: name = [Ada, Grace], age = [36, 40]
Dataset transposed = table.toTransposedDataset(List.of("person1", "person2"));
// Two columns: person1 = [Ada, 36], person2 = [Grace, 40]
```

`toDataset(names)` uses one name per matrix column; `toTransposedDataset(names)` uses one
name per matrix row. Names must be non-null, nonempty, and unique. Both methods copy the
containers but retain element references. Box a primitive matrix first to use these methods.

Dataset cannot represent every degenerate matrix shape: `toDataset` rejects `N x 0` for
positive `N`, and `toTransposedDataset` rejects `0 x N` for positive `N`. A `0 x 0` matrix
accepts an empty names collection.

For display, use `toString()` for the compact representation, `println()` for one row per
line, or `appendTo(Appendable)` to write to a destination such as a `StringBuilder`.
`accept(action)` passes the whole matrix to a callback; `apply(mapper)` returns a value
computed from the whole matrix.

## Parallel execution

Automatic parallelism is a **thread-local policy**, and every thread starts with
`ParallelMode.FORCE_OFF`. It affects operations that consult the policy, rather than every
operation or every returned stream.

| Mode | Behavior for eligible operations |
| --- | --- |
| `FORCE_OFF` | Sequential execution; default |
| `AUTO` | Parallel execution when runtime support exists and the operation's estimated work reaches the threshold |
| `FORCE_ON` | Bypass the work threshold, subject to runtime support and operation safety restrictions |

Prefer a scoped mode change so the previous setting is restored, including when the action throws:

```java
import com.landawn.abacus.matrix.IntMatrix;
import com.landawn.abacus.matrix.Matrices;
import com.landawn.abacus.matrix.ParallelMode;

IntMatrix matrix = IntMatrix.range(0, 10_000).reshape(100, 100);
ParallelMode previous = Matrices.getParallelMode();
Matrices.runWithParallelMode(ParallelMode.AUTO, () -> {
    matrix.updateAll(value -> value + 1);
});
boolean restored = Matrices.getParallelMode() == previous; // true
```

`Matrices.setParallelMode(mode)` changes the current thread's policy until changed again.
`Matrices.shouldRunInParallel(matrix)` and its work-count overload expose the policy decision.
The current `AUTO` threshold is 8,192 work items; an operation may estimate work using cells,
a subset, or multiplication cost. Treat the threshold as an implementation detail.

Parallel execution requires the parallel stream implementations supplied by `abacus-common`.
Support is detected when `Matrices` initializes; unavailable support causes a sequential
fallback even under `FORCE_ON`. Overloads with an explicit `inParallel` argument use that
argument instead of the thread-local policy, while still requiring runtime support.

Parallel callbacks can run concurrently in an unspecified invocation order. Keep them
thread-safe and independent of visitation order. Enabling parallelism does not make the matrix
safe for unrelated concurrent readers or writers. Callbacks can declare checked exceptions
through abacus-common's `Throwables` functional interfaces; consult the specific method's
contract for failure behavior.

## Validation and common pitfalls

| Symptom / situation | Contract and remedy |
| --- | --- |
| Missing `com.landawn.abacus` classes at runtime | Add both `abacus-common` and `abacus-extra` explicitly; Maven `provided` dependencies are not inherited as application runtime dependencies |
| Ragged input, null row, or repeated row in `wrap` | Construction requires rectangular, non-null, identity-distinct rows; use valid input or `copyOf` for repeated row identities |
| A source array edit changes the matrix | `wrap` shares row storage; use `copyOf` for isolation |
| Calling `map` or `resize` appears to do nothing | Keep the returned matrix; use `updateAll` for value mutation |
| Reshape fails | `reshape` requires the same cell count; `reshapeAndPad` can only keep or increase it; use `resize` or slicing to discard cells |
| Shape mismatch in arithmetic or zip | Supply identical dimensions; there is no broadcasting |
| Matrix multiplication fails | Left columns must equal right rows |
| Unexpected integer wraparound | Choose checked or widened arithmetic, or a suitably wider type |
| `0 x N` becomes `0 x 0` after wrapping an array | A zero-row array loses its width; preserve the matrix shape separately |
| `ArrayStoreException` with `Matrix<T>` | The runtime array type is narrower than the value being stored; construct compatible storage or use an explicit target type |
| No parallel execution under `FORCE_ON` | Check runtime support; operation safety restrictions can still require sequential execution |

Invalid non-null-required arguments, invalid dimensions, and incompatible shapes generally
throw `IllegalArgumentException`. Invalid indexes or ranges throw `IndexOutOfBoundsException`
or its `ArrayIndexOutOfBoundsException` subtype. These distinctions are documented per method.

Null cell values are valid in `Matrix<T>`. Other deliberate null exceptions include one missing
diagonal in `ofDiagonals` and skipped rows in `copyFrom`. `Matrices.isSameShape(collection)`
returns true for a null or empty collection, but false for a collection containing a null matrix;
this does not make such collections valid inputs to stacking or zipping.

A callback returning null when a primitive result must be unboxed can throw
`NullPointerException`; callback exceptions are not converted into argument-validation errors.
In-place callback operations are generally not transactional. Use a copy when you need to
retain the original if processing fails.

## API reference at a glance

These groups are entry points; see the source Javadocs for full overload signatures,
type-specific return types, exception details, and edge cases.

| Need | Main APIs |
| --- | --- |
| Inspect metadata | `rowCount`, `columnCount`, `elementCount`, `isEmpty`, `elementType`, `isSameShape` |
| Create | `wrap`, `copyOf`, `empty`, `range`, `rangeClosed`, `random`, diagonal factories |
| Read cells / rows / columns | `get`, `rowView`, `rowCopy`, `columnCopy`, diagonal copies, neighbor methods |
| Modify values | `set`, `setRow`, `setColumn`, `fill`, `update*`, `replaceIf`, `copyFrom` |
| Copy / slice | `copy`, `copyRows`, `copyColumns`, `copyRegion` |
| Relayout / grow / crop | `reshape`, `reshapeAndPad`, `reshapeAndPadToColumnCount`, `resize`, `pad` |
| Reorient | `transpose`, rotations, horizontal / vertical flips, in-place flips |
| Repeat | `repeatElements`, `repeatMatrix` |
| Transform types | `map`, `mapTo*`, `to*Matrix`, `boxed`, `unbox`, selected `from` factories |
| Calculate | `add`, `subtract`, `matrixMultiply`, supported checked / widened variants, `zipWith` |
| Combine | Instance stacking and `zipWith`; `Matrices.stack*`, `Matrices.zip*` |
| Traverse | `forEach`, `forEachIndices`, element streams, coordinate streams, neighbor points |
| Materialize / export | `flatten`, `mutateViaFlatArray`, `unsafeBackingArray`, `toDataset`, `toTransposedDataset` |
| Display / integrate | `toString`, `println`, `appendTo`, `accept`, `apply` |
| Control execution | `Matrices.getParallelMode`, `setParallelMode`, `runWithParallelMode`, `shouldRunInParallel` |

`equals` and `hashCode` reflect matrix contents and shape. Since a matrix is mutable, avoid
changing it while it is used as a hash-based collection key.

## Building and testing

Use Maven with JDK 21 or later. Run commands from the repository root:

```sh
# Compile production sources.
mvn compile

# Run the configured AbacusMatrixTestSuite.
mvn test

# Build and verify without requiring a GPG signing key.
mvn verify -Dgpg.skip=true

# Install this checkout's artifacts into your local Maven repository.
mvn install -Dgpg.skip=true
```

The POM attaches source and Javadoc JARs, runs JaCoCo reporting during `verify`, and binds GPG
signing to `verify`. The `gpg.skip` flag is useful for local builds without release signing keys.
See [testing documentation](./docs/testing/README.md) for the test organization.

## Further documentation

- [Package contract and overview](./src/main/java/com/landawn/abacus/matrix/package-info.java)
- [Shared API: AbstractMatrix](./src/main/java/com/landawn/abacus/matrix/AbstractMatrix.java)
- [Object matrices: Matrix](./src/main/java/com/landawn/abacus/matrix/Matrix.java)
- [Integer matrices: IntMatrix](./src/main/java/com/landawn/abacus/matrix/IntMatrix.java)
- [Floating-point matrices: DoubleMatrix](./src/main/java/com/landawn/abacus/matrix/DoubleMatrix.java)
- [Static helpers: Matrices](./src/main/java/com/landawn/abacus/matrix/Matrices.java)
- [ParallelMode](./src/main/java/com/landawn/abacus/matrix/ParallelMode.java)
- [Published Javadocs](https://www.javadoc.io/doc/com.landawn.abacus/abacus-matrix)
- [Generated API index](./docs/ai/API.md) (check its version header against your checkout)
- [Release notes](./CHANGES.md)
- [Project wiki](https://github.com/landawn/abacus-matrix/wiki)
- Related projects: [abacus-common](https://github.com/landawn/abacus-common), [abacus-extra](https://github.com/landawn/abacus-extra)

## License

Licensed under the [Apache License, Version 2.0](./LICENSE).
