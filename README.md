# abacus-matrix

[![Maven Central](https://img.shields.io/maven-central/v/com.landawn.abacus/abacus-matrix.svg)](https://central.sonatype.com/artifact/com.landawn.abacus/abacus-matrix/3.7.6)
[![Javadocs](https://img.shields.io/badge/javadoc-3.7.6-brightgreen.svg)](https://www.javadoc.io/doc/com.landawn.abacus/abacus-matrix/3.7.6/index.html)

`abacus-matrix` is a Java library for working with rectangular 2D arrays as matrix types. It provides dedicated matrix implementations for primitive values and objects, with APIs for array-backed access, reshaping, traversal, and element-wise transformations without the overhead of a heavyweight numerical framework.

It includes `BooleanMatrix`, `ByteMatrix`, `CharMatrix`, `ShortMatrix`, `IntMatrix`, `LongMatrix`, `FloatMatrix`, `DoubleMatrix`, and generic `Matrix<T>`, plus the `Matrices` utility class for shared helpers such as zipping matrices and controlling optional parallel execution through `ParallelMode`.

## Highlights

- Rectangular, array-backed matrix types for primitive and object values.
- Common matrix operations such as row/column access, transpose, reshape, flatten, stacking, mapping, and slicing.
- Numeric matrix operations where supported, including add, subtract, and multiply on numeric primitive matrices.
- Element-wise zip/combine utilities for pairs or collections of matrices.
- Optional thread-local parallelization for larger workloads.
- Object-matrix interop helpers such as conversion to `Dataset`.

Many constructors and `of(...)` factories wrap the supplied 2D array directly for performance. If you need isolation from the original array, use copy-producing APIs instead of sharing the backing storage.

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

## Build Notes

- Requires Java 17.
- This module depends on `abacus-common` and `abacus-extra`.

## Related Projects

- [abacus-common](https://github.com/landawn/abacus-common)
- [abacus-extra](https://github.com/landawn/abacus-extra)
