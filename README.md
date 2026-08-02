# abacus-matrix

[![Maven Central](https://img.shields.io/maven-central/v/com.landawn.abacus/abacus-matrix.svg)](https://central.sonatype.com/artifact/com.landawn.abacus/abacus-matrix/3.8.5)
[![Javadocs](https://img.shields.io/badge/javadoc-3.8.5-brightgreen.svg)](https://www.javadoc.io/doc/com.landawn.abacus/abacus-matrix/3.8.5/index.html)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)

`abacus-matrix` is a lightweight Java library for treating rectangular 2D arrays as matrices. It
provides dedicated, array-backed matrix types for every Java primitive plus objects, with a
consistent API for element access, reshaping, traversal, transformation, and — where it makes
sense — element-wise and linear-algebra arithmetic. It gives you matrix ergonomics without pulling
in a heavyweight numerical framework.

- **Small and focused** — one package, no runtime dependencies beyond `abacus-common`.
- **Primitive-specialized** — no boxing on the hot path; `int[][]`, `double[][]`, etc. stay primitive.
- **Consistent across types** — the same operations are named the same way on all nine matrix classes.

## Contents

- [Matrix types](#matrix-types)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Feature overview](#feature-overview)
- [Array sharing and copying](#array-sharing-and-copying)
- [Parallel execution](#parallel-execution)
- [Documentation](#documentation)
- [Requirements](#requirements)
- [Related projects](#related-projects)
- [License](#license)

## Matrix types

| Type | Backing storage | Notes |
| --- | --- | --- |
| `BooleanMatrix` | `boolean[][]` | logical operations |
| `ByteMatrix` | `byte[][]` | numeric arithmetic |
| `CharMatrix` | `char[][]` | code-unit arithmetic |
| `ShortMatrix` | `short[][]` | numeric arithmetic |
| `IntMatrix` | `int[][]` | numeric arithmetic |
| `LongMatrix` | `long[][]` | numeric arithmetic |
| `FloatMatrix` | `float[][]` | numeric arithmetic |
| `DoubleMatrix` | `double[][]` | numeric arithmetic |
| `Matrix<T>` | `T[][]` | reference semantics, `null` allowed |

The `Matrices` utility class adds cross-matrix helpers (zipping, stacking, shape checks, index
iteration), and `ParallelMode` controls optional parallel execution.

## Installation

**Maven**

```xml
<dependency>
    <groupId>com.landawn.abacus</groupId>
    <artifactId>abacus-matrix</artifactId>
    <version>3.8.5</version>
</dependency>
```

**Gradle**

```groovy
implementation 'com.landawn.abacus:abacus-matrix:3.8.5'
```

`abacus-matrix` builds on top of [`abacus-common`](https://github.com/landawn/abacus-common). It is
declared with `provided` scope, so it is **not** pulled in transitively — add it explicitly to your
build:

```xml
<dependency>
    <groupId>com.landawn.abacus</groupId>
    <artifactId>abacus-common</artifactId>
    <version>7.8.5</version>
</dependency>
```

## Quick start

```java
import com.landawn.abacus.matrix.IntMatrix;

// Create a matrix from a 2D array (wraps the array directly — see "Array sharing" below)
IntMatrix a = IntMatrix.of(new int[][] {
    {1, 2, 3},
    {4, 5, 6}
});

// Element access
a.get(0, 2);              // 3
a.set(1, 0, 40);          // mutate in place

// Shape and layout
a.rowCount();             // 2
a.columnCount();          // 3
IntMatrix t = a.transpose();       // 3 x 2
IntMatrix r = a.rotate90();        // rotate clockwise
IntMatrix s = a.reshape(3, 2);     // same data, new shape

// Element-wise arithmetic (matrices must share shape)
IntMatrix sum = a.add(a);
IntMatrix diff = a.subtract(a);

// Linear-algebra multiply (a is 2x3, b is 3x2 -> 2x2)
IntMatrix b = IntMatrix.of(new int[][] {{7, 8}, {9, 10}, {11, 12}});
IntMatrix product = a.matrixMultiply(b);

// Transform every element
IntMatrix doubled = a.map(v -> v * 2);

// Stream elements (row-major here; columnMajorStream() also available)
int total = a.rowMajorStream().sum();
```

Working with objects is the same shape of API via `Matrix<T>`:

```java
import com.landawn.abacus.matrix.Matrix;
import com.landawn.abacus.matrix.IntMatrix;

Matrix<String> grid = Matrix.of(new String[][] {
    {"a", "b"},
    {"c", "d"}
});

Matrix<String> upper = grid.map(String::toUpperCase);
IntMatrix lengths = grid.mapToInt(String::length);   // project to a primitive matrix
```

## Feature overview

- **Element access** — `get` / `set` by `(row, column)` or `Point`, plus row/column views and
  defensive copies (`rowView`, `rowCopy`, `columnCopy`, `setRow`, `setColumn`).
- **Shape operations** — `transpose`, `rotate90` / `rotate180` / `rotate270`,
  `flipHorizontally` / `flipVertically`, `reshape`, `resize`, `extend`, `repeatElements`,
  `repeatMatrix`.
- **Composition** — `stackVertically` / `stackHorizontally` on instances, and
  `Matrices.stackVertically` / `Matrices.stackHorizontally` over collections.
- **Diagonals** — read and write the main and anti-diagonals (`mainDiagonalCopy`,
  `setMainDiagonal`, `antiDiagonalCopy`, `setAntiDiagonal`).
- **Transformation** — `map`, `updateAll`, `forEach`, per-type `mapToInt` / `mapToLong` /
  `mapToDouble` on `Matrix<T>` and the numeric primitives, plus `boxed()` to lift a primitive
  matrix to `Matrix<Wrapper>`.
- **Arithmetic** — `add`, `subtract`, and `matrixMultiply` on the numeric primitive matrices.
  Integer types follow standard Java overflow semantics (silent wraparound).
- **Combining matrices** — instance `zipWith`, and `Matrices.zip` / `zipToInt` / `zipToLong` /
  `zipToDouble` / `zipToObj` for pairs, triples, or collections.
- **Streaming and flattening** — `rowMajorStream` / `columnMajorStream` (whole matrix, a single
  row/column, or a range), `rowStreams` / `columnStreams`, and `flatten` to a primitive list.
- **Interop** — `Matrix.toDataset` / `toTransposedDataset` to bridge to `abacus-common`'s
  `Dataset`.

> **Aggregations** (`sum`, `min`, `max`, `average`) are not dedicated methods — compute them
> through the streaming API, e.g. `matrix.rowMajorStream().sum()` for the whole matrix or
> `rowStreams()` / `columnStreams()` for per-row / per-column reductions.

## Array sharing and copying

Constructors and the `of(...)` factories **wrap the supplied 2D array directly** (after a
rectangular-shape check) — no defensive copy is made, so later changes to the array or the matrix
are visible through the other. This is intentional and fast. When you need isolation from the
original array, use a copy-producing API instead:

```java
int[][] data = {{1, 2}, {3, 4}};

IntMatrix wrapped = IntMatrix.of(data);       // shares 'data'
data[0][0] = 99;
wrapped.get(0, 0);                            // 99

IntMatrix owned = IntMatrix.copyOf(data);     // deep-copies 'data'
data[0][0] = -1;
owned.get(0, 0);                              // 99 (unaffected)
```

`copy()`, `copyOf(...)`, and the mapping/transformation methods all return independent matrices.
The matrix types are **not thread-safe**; guard external synchronization if you share an instance
across threads while mutating it.

## Parallel execution

Several element-wise operations can run in parallel for larger matrices. This is controlled
globally (thread-locally) through `Matrices` and `ParallelMode`:

```java
import com.landawn.abacus.matrix.Matrices;
import com.landawn.abacus.matrix.ParallelMode;

Matrices.setParallelMode(ParallelMode.FORCE_ON);   // always parallel, ignore size
Matrices.setParallelMode(ParallelMode.FORCE_OFF);  // always sequential
Matrices.setParallelMode(ParallelMode.AUTO);       // default: parallel only past a size threshold
```

`AUTO` is the default and keeps small operations sequential (to avoid dispatch overhead) while
larger ones run in parallel. The setting is stored in a `ThreadLocal`, so it only affects the
current thread. Use `Matrices.runWithParallelMode(mode, action)` to scope a mode change to a
single block of work.

## Documentation

- [API index](./docs/ai/API.md)
- [Release notes](./CHANGES.md)
- [Wiki](https://github.com/landawn/abacus-matrix/wiki)
- Type overviews:
  - [AbstractMatrix](https://htmlpreview.github.io/?https://github.com/landawn/abacus-matrix/blob/master/docs/AbstractMatrix_view.html)
  - [Matrix](https://htmlpreview.github.io/?https://github.com/landawn/abacus-matrix/blob/master/docs/Matrix_view.html)
  - [IntMatrix](https://htmlpreview.github.io/?https://github.com/landawn/abacus-matrix/blob/master/docs/IntMatrix_view.html)
  - [DoubleMatrix](https://htmlpreview.github.io/?https://github.com/landawn/abacus-matrix/blob/master/docs/DoubleMatrix_view.html)
  - [Matrices](https://htmlpreview.github.io/?https://github.com/landawn/abacus-matrix/blob/master/docs/Matrices_view.html)

## Requirements

- Java 17 or later.
- Runtime dependency: [`abacus-common`](https://github.com/landawn/abacus-common).

## Related projects

- [abacus-common](https://github.com/landawn/abacus-common)
- [abacus-extra](https://github.com/landawn/abacus-extra)

## License

Licensed under the [Apache License, Version 2.0](./LICENSE).
