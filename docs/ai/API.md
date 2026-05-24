# abacus-matrix API Index (v3.7.3)
- Build: unknown
- Java: 17
- Generated: 2026-05-24

## Packages
- com.landawn.abacus.matrix

## com.landawn.abacus.matrix
### Class AbstractMatrix (com.landawn.abacus.matrix.AbstractMatrix)
Shared implementation base for the matrix types in this package.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
##### elementType(...) -> Class<?>
- **Signature:** `public Class<?> elementType()`
- **Summary:** Returns the element type of this matrix.
- **Parameters:**
  - (none)
- **Returns:** the Class object representing the element type of this matrix
##### internalArray(...) -> A\[\]
- **Signature:** `@SuppressFBWarnings("EI_EXPOSE_REP") public A[] internalArray()`
- **Summary:** Returns the underlying two-dimensional array of this matrix.
- **Contract:**
  - This method exposes the internal array representation for performance reasons and should be used with caution as modifications to the returned array will directly affect the matrix.
  - If you need an independent matrix instance, use {@link #copy()} .
  - If you only need the data flattened into a single one-dimensional array, use {@link #flatten()} .
- **Parameters:**
  - (none)
- **Returns:** the underlying two-dimensional array (not a copy); its length equals {@code rowCount} (so a {@code 0} -row matrix yields a zero-length array, but a {@code rowCount × 0} matrix yields a {@code rowCount} -length array of zero-length rows)
##### rowView(...) -> A
- **Signature:** `public abstract A rowView(int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as a direct view backed by internal storage.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the specified row array (direct reference to internal storage)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowIndex} is negative or {@code >= rowCount}
##### rowCopy(...) -> A
- **Signature:** `public abstract A rowCopy(int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new array containing the values from the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowIndex} is negative or {@code >= rowCount}
##### columnCopy(...) -> A
- **Signature:** `public abstract A columnCopy(int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified column.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new array containing the values from the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code columnIndex} is negative or {@code >= columnCount}
##### rowCount(...) -> int
- **Signature:** `public int rowCount()`
- **Summary:** Returns the number of rows in this matrix.
- **Parameters:**
  - (none)
- **Returns:** the number of rows
##### columnCount(...) -> int
- **Signature:** `public int columnCount()`
- **Summary:** Returns the number of columns in this matrix.
- **Parameters:**
  - (none)
- **Returns:** the number of columns
##### elementCount(...) -> long
- **Signature:** `public long elementCount()`
- **Summary:** Returns the total number of elements in this matrix (rows x columns).
- **Parameters:**
  - (none)
- **Returns:** the total number of elements
##### isEmpty(...) -> boolean
- **Signature:** `public boolean isEmpty()`
- **Summary:** Returns {@code true} if this matrix is empty (contains no elements).
- **Contract:**
  - Returns {@code true} if this matrix is empty (contains no elements).
  - A matrix is considered empty if either the number of rows or columns is zero, resulting in a total count of zero elements.
- **Parameters:**
  - (none)
- **Returns:** {@code true} if the matrix has no elements (count == 0), {@code false} otherwise
##### println(...) -> String
- **Signature:** `public abstract String println()`
- **Summary:** Prints this matrix to standard output in a formatted, human-readable manner and returns the output string.
- **Parameters:**
  - (none)
- **Returns:** the formatted string representation of the matrix that was printed to standard output
##### copy(...) -> M
- **Signature:** `public abstract M copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a copy of this matrix with the same dimensions and values
- **Signature:** `public abstract M copy(int fromRowIndex, int toRowIndex)`
- **Summary:** Returns a copy of a row range from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new matrix containing the specified rows with dimensions (toRowIndex - fromRowIndex) × columnCount
- **Signature:** `public abstract M copy(int fromRowIndex, int toRowIndex, int fromColumnIndex, int toColumnIndex)`
- **Summary:** Returns a copy of a rectangular region from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new matrix containing the specified region with dimensions (toRowIndex - fromRowIndex) × (toColumnIndex - fromColumnIndex)
##### rotate90(...) -> M
- **Signature:** `public abstract M rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 90 degrees clockwise, with dimensions columnCount x rowCount
##### rotate180(...) -> M
- **Signature:** `public abstract M rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 180 degrees, with the same dimensions (rowCount x columnCount)
##### rotate270(...) -> M
- **Signature:** `public abstract M rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise (or equivalently, 90 degrees counter-clockwise).
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 270 degrees clockwise, with dimensions columnCount x rowCount
##### transpose(...) -> M
- **Signature:** `public abstract M transpose()`
- **Summary:** Returns a new matrix that is the transpose of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is the transpose of this matrix, with dimensions columnCount x rowCount
##### reshape(...) -> M
- **Signature:** `public M reshape(final int newColumnCount)`
- **Summary:** Returns a new matrix with the elements of this matrix rearranged into the specified number of columns.
- **Contract:**
  - If the total element count is not evenly divisible by the new column count, the last row will be padded with default values (0 for numeric types, false for boolean, null for objects).
- **Parameters:**
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix (must be positive)
- **Returns:** a new matrix with the specified number of columns
- **Signature:** `public abstract M reshape(int newRowCount, int newColumnCount)`
- **Summary:** Returns a new matrix with the elements of this matrix rearranged into the specified dimensions.
- **Contract:**
  - The new shape must have at least as many total elements as the original ( {@code newRowCount * newColumnCount >= elementCount()} ).
  - If the new shape has more elements, the extra positions are filled with default values (0 for numeric types, false for boolean, null for objects).
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix; must be non-negative
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix; must be non-negative
- **Returns:** a new matrix with the specified dimensions ( {@code newRowCount × newColumnCount} )
##### isSameShape(...) -> boolean
- **Signature:** `public boolean isSameShape(final M m)`
- **Summary:** Returns {@code true} if this matrix has the same shape (dimensions) as the specified matrix.
- **Contract:**
  - Returns {@code true} if this matrix has the same shape (dimensions) as the specified matrix.
  - Two matrices have the same shape if they have the same number of rows and columns.
- **Parameters:**
  - `m` (`M`) — the matrix to compare with
- **Returns:** {@code true} if both matrices have the same dimensions, {@code false} otherwise
##### repeatElements(...) -> M
- **Signature:** `public abstract M repeatElements(int rowRepeats, int columnRepeats)`
- **Summary:** Returns a new matrix with each element repeated the specified number of times in both dimensions.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat each element in the row direction (must be &gt; = 1)
  - `columnRepeats` (`int`) — number of times to repeat each element in the column direction (must be &gt; = 1)
- **Returns:** a new matrix with repeated elements, with dimensions (rowCount x rowRepeats) x (columnCount x columnRepeats)
- **See also:** <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">,MATLAB repelem function,</a>
##### repeatMatrix(...) -> M
- **Signature:** `public abstract M repeatMatrix(int rowRepeats, int columnRepeats)`
- **Summary:** Returns a new matrix formed by tiling this matrix the specified number of times in both dimensions.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat the matrix in the row direction (must be &gt; = 1)
  - `columnRepeats` (`int`) — number of times to repeat the matrix in the column direction (must be &gt; = 1)
- **Returns:** a new matrix with this matrix tiled, with dimensions (rowCount x rowRepeats) x (columnCount x columnRepeats)
- **See also:** <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">,MATLAB repmat function,</a>
##### extend(...) -> M
- **Signature:** `public abstract M extend(int padTop, int padBottom, int padLeft, int padRight)`
- **Summary:** Returns a new matrix grown by the specified non-negative pad widths on each side.
- **Parameters:**
  - `padTop` (`int`) — number of rows to add above the matrix (must be &gt; = 0)
  - `padBottom` (`int`) — number of rows to add below the matrix (must be &gt; = 0)
  - `padLeft` (`int`) — number of columns to add to the left of the matrix (must be &gt; = 0)
  - `padRight` (`int`) — number of columns to add to the right of the matrix (must be &gt; = 0)
- **Returns:** a new matrix grown by the specified pad widths, with new cells filled with the type's default value
##### flipHorizontally(...) -> M
- **Signature:** `public abstract M flipHorizontally()`
- **Summary:** Returns a new matrix that is a horizontal flip (mirror across the vertical axis) of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix with columns reversed within each row
- **See also:** #flipHorizontallyInPlace(), #flipVertically()
##### flipVertically(...) -> M
- **Signature:** `public abstract M flipVertically()`
- **Summary:** Returns a new matrix that is a vertical flip (mirror across the horizontal axis) of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix with rows reversed
- **See also:** #flipVerticallyInPlace(), #flipHorizontally()
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `public abstract void flipHorizontallyInPlace()`
- **Summary:** Flips this matrix horizontally in place (mirror across the vertical axis).
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally(), #flipVerticallyInPlace()
##### flipVerticallyInPlace(...) -> void
- **Signature:** `public abstract void flipVerticallyInPlace()`
- **Summary:** Flips this matrix vertically in place (mirror across the horizontal axis).
- **Parameters:**
  - (none)
- **See also:** #flipVertically(), #flipHorizontallyInPlace()
##### stackVertically(...) -> M
- **Signature:** `public abstract M stackVertically(M other)`
- **Summary:** Vertically stacks this matrix with the specified matrix.
- **Contract:**
  - The matrices must have the same number of columns.
- **Parameters:**
  - `other` (`M`) — the matrix to stack below this matrix (must not be {@code null} )
- **Returns:** a new matrix with combined rows and the same column count
- **See also:** #stackHorizontally(AbstractMatrix)
##### stackHorizontally(...) -> M
- **Signature:** `public abstract M stackHorizontally(M other)`
- **Summary:** Horizontally stacks this matrix with the specified matrix.
- **Contract:**
  - The matrices must have the same number of rows.
- **Parameters:**
  - `other` (`M`) — the matrix to stack to the right of this matrix (must not be {@code null} )
- **Returns:** a new matrix with combined columns and the same row count
- **See also:** #stackVertically(AbstractMatrix)
##### flatten(...) -> PL
- **Signature:** `public abstract PL flatten()`
- **Summary:** Flattens this matrix into a one-dimensional list.
- **Parameters:**
  - (none)
- **Returns:** a new list containing all elements in row-major order with size equal to {@code elementCount}
##### mutateAsFlat(...) -> void
- **Signature:** `public abstract <E extends Exception> void mutateAsFlat(Throwables.Consumer<? super A, E> action) throws E`
- **Summary:** Applies the specified operation to the flattened (row-major order) view of this matrix.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super A, E>`) — the operation to apply to the flattened array (receives array type A, not A\[\])
- **Throws:**
  - `E` — if the operation throws an exception
##### forEachIndices(...) -> void
- **Signature:** `public <E extends Exception> void forEachIndices(final Throwables.IntBiConsumer<E> action) throws E`
- **Summary:** Performs the specified action for each element position in the matrix.
- **Contract:**
  - <p> This method is useful when you need to access matrix positions without caring about the actual element values, or when the element access logic is handled inside the action.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code IntMatrix matrix = IntMatrix.of(new int\[\]\[\] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}); matrix.forEachIndices((i, j) -> { System.out.println("Position: (" + i + "," + j + ")"); }); // Count elements on the main diagonal AtomicInteger diagonalCount = new AtomicInteger(0); matrix.forEachIndices((i, j) -> { if (i == j) diagonalCount.incrementAndGet(); }); } </pre>
- **Parameters:**
  - `action` (`Throwables.IntBiConsumer<E>`) — the action to perform for each position, receives (rowIndex, columnIndex)
- **Throws:**
  - `E` — if the action throws an exception
- **Signature:** `public <E extends Exception> void forEachIndices(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.IntBiConsumer<E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element position in the specified rectangular region of the matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.IntBiConsumer<E>`) — the action to perform for each position, receives (rowIndex, columnIndex)
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds
  - `E` — if the action throws an exception
- **Signature:** `public <E extends Exception> void forEachIndices(final Throwables.BiIntObjConsumer<M, E> action) throws E`
- **Summary:** Performs the specified action for each element position in the matrix, providing the matrix itself as a parameter.
- **Contract:**
  - <p> This variant is useful when the action needs access to matrix elements or methods, allowing you to read/write values or use matrix operations within the action.
- **Parameters:**
  - `action` (`Throwables.BiIntObjConsumer<M, E>`) — the action to perform, receiving (rowIndex, columnIndex, matrix)
- **Throws:**
  - `E` — if the action throws an exception
- **Signature:** `public <E extends Exception> void forEachIndices(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.BiIntObjConsumer<M, E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element position in the specified rectangular region, providing the matrix itself.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.BiIntObjConsumer<M, E>`) — the action to perform, receiving (rowIndex, columnIndex, matrix)
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds
  - `E` — if the action throws an exception
##### adjacent4Points(...) -> Stream<Point>
- **Signature:** `public Stream<Point> adjacent4Points(final int rowIndex, final int columnIndex)`
- **Summary:** Returns a stream of points directly above, below, to the left of, and to the right of the specified position (the four cardinal directions).
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** a stream of adjacent points in the four cardinal directions (0 to 4 points depending on position)
##### adjacent8Points(...) -> Stream<Point>
- **Signature:** `public Stream<Point> adjacent8Points(final int rowIndex, final int columnIndex)`
- **Summary:** Returns a stream of all 8 points adjacent to the specified position, including the points directly above, below, to the left of, to the right of, and diagonally adjacent to the specified position.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** a stream of adjacent points in all 8 directions (0 to 8 points depending on position)
##### mainDiagonalPoints(...) -> Stream<Point>
- **Signature:** `public Stream<Point> mainDiagonalPoints()`
- **Summary:** Returns a stream of points along the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a stream of {@link Point} objects representing the main diagonal positions
##### antiDiagonalPoints(...) -> Stream<Point>
- **Signature:** `public Stream<Point> antiDiagonalPoints()`
- **Summary:** Returns a stream of points along the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a stream of {@link Point} objects representing the anti-diagonal positions
##### getMainDiagonal(...) -> A
- **Signature:** `public abstract A getMainDiagonal()`
- **Summary:** Returns a copy of the main diagonal elements (upper-left to lower-right) as the matrix's underlying array type.
- **Contract:**
  - The matrix must be square (rowCount == columnCount).
- **Parameters:**
  - (none)
- **Returns:** a new array containing the main diagonal values
##### setMainDiagonal(...) -> void
- **Signature:** `public abstract void setMainDiagonal(A mainDiagonal)`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the supplied array must contain exactly {@code rowCount} elements.
- **Parameters:**
  - `mainDiagonal` (`A`) — the new values for the main diagonal; must be non- {@code null} and have length equal to {@code rowCount}
##### getAntiDiagonal(...) -> A
- **Signature:** `public abstract A getAntiDiagonal()`
- **Summary:** Returns a copy of the anti-diagonal elements (upper-right to lower-left) as the matrix's underlying array type.
- **Contract:**
  - The matrix must be square (rowCount == columnCount).
- **Parameters:**
  - (none)
- **Returns:** a new array containing the anti-diagonal values
##### setAntiDiagonal(...) -> void
- **Signature:** `public abstract void setAntiDiagonal(A antiDiagonal)`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the supplied array must contain exactly {@code rowCount} elements.
- **Parameters:**
  - `antiDiagonal` (`A`) — the new values for the anti-diagonal; must be non- {@code null} and have length equal to {@code rowCount}
##### horizontalPoints(...) -> Stream<Point>
- **Signature:** `public Stream<Point> horizontalPoints()`
- **Summary:** Returns a stream of all points in the matrix in row-major order (horizontal traversal).
- **Parameters:**
  - (none)
- **Returns:** a stream of all {@link Point} objects in row-major order
- **Signature:** `public Stream<Point> horizontalPoints(final int rowIndex)`
- **Summary:** Returns a stream of points for a specific row in horizontal order (left to right).
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
- **Returns:** a stream of {@link Point} objects for all columns in the specified row
- **Signature:** `@SuppressWarnings("resource") public Stream<Point> horizontalPoints(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of points for a range of rows in row-major order (horizontal traversal).
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a stream of {@link Point} objects in the specified row range, in row-major order
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
##### verticalPoints(...) -> Stream<Point>
- **Signature:** `public Stream<Point> verticalPoints()`
- **Summary:** Returns a stream of all points in the matrix in column-major order (vertical traversal).
- **Parameters:**
  - (none)
- **Returns:** a stream of all {@link Point} objects in column-major order
- **Signature:** `public Stream<Point> verticalPoints(final int columnIndex)`
- **Summary:** Returns a stream of points for a specific column in vertical order (top to bottom).
- **Parameters:**
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** a stream of {@link Point} objects for all rows in the specified column
- **Signature:** `@SuppressWarnings("resource") public Stream<Point> verticalPoints(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of points for a range of columns in column-major order (vertical traversal).
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a stream of {@link Point} objects in the specified column range, in column-major order
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
##### rowPoints(...) -> Stream<Stream<Point>>
- **Signature:** `public Stream<Stream<Point>> rowPoints()`
- **Summary:** Returns a stream of streams where each inner stream represents a row of points.
- **Parameters:**
  - (none)
- **Returns:** a stream of streams, where each inner stream contains {@link Point} objects for one row
- **Signature:** `@SuppressWarnings("resource") public Stream<Stream<Point>> rowPoints(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of streams for a range of rows, where each inner stream represents a row of points.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a stream of streams, where each inner stream contains {@link Point} objects for one row
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
##### columnPoints(...) -> Stream<Stream<Point>>
- **Signature:** `public Stream<Stream<Point>> columnPoints()`
- **Summary:** Returns a stream of streams where each inner stream represents a column of points.
- **Parameters:**
  - (none)
- **Returns:** a stream of streams, where each inner stream contains {@link Point} objects for one column
- **Signature:** `@SuppressWarnings("resource") public Stream<Stream<Point>> columnPoints(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of streams for a range of columns, where each inner stream represents a column of points.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a stream of streams, where each inner stream contains {@link Point} objects for one column
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
##### mainDiagonalStream(...) -> ES
- **Signature:** `public abstract ES mainDiagonalStream()`
- **Summary:** Returns a stream of elements along the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a stream of diagonal elements
##### antiDiagonalStream(...) -> ES
- **Signature:** `public abstract ES antiDiagonalStream()`
- **Summary:** Returns a stream of elements along the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a stream of anti-diagonal elements
##### horizontalStream(...) -> ES
- **Signature:** `public abstract ES horizontalStream()`
- **Summary:** Returns a stream of all elements in row-major order (horizontal traversal).
- **Parameters:**
  - (none)
- **Returns:** a stream of all elements in row-major order
- **Signature:** `public abstract ES horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a specific row.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
- **Returns:** a stream of elements in the specified row
- **Signature:** `public abstract ES horizontalStream(final int fromRowIndex, final int toRowIndex)`
- **Summary:** Returns a stream of elements from a range of rows in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a stream of elements in the specified row range
##### verticalStream(...) -> ES
- **Signature:** `public abstract ES verticalStream()`
- **Summary:** Returns a stream of all elements in column-major order (vertical traversal).
- **Parameters:**
  - (none)
- **Returns:** a stream of all elements in column-major order
- **Signature:** `public abstract ES verticalStream(final int columnIndex)`
- **Summary:** Returns a stream of elements from a specific column.
- **Parameters:**
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** a stream of elements in the specified column
- **Signature:** `public abstract ES verticalStream(final int fromColumnIndex, final int toColumnIndex)`
- **Summary:** Returns a stream of elements from a range of columns in column-major order.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a stream of elements in the specified column range
##### rowStreams(...) -> RS
- **Signature:** `public abstract RS rowStreams()`
- **Summary:** Returns a stream of row streams.
- **Parameters:**
  - (none)
- **Returns:** a stream of row streams
- **Signature:** `public abstract RS rowStreams(final int fromRowIndex, final int toRowIndex)`
- **Summary:** Returns a stream of row streams for a range of rows.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a stream of row streams for the specified range
##### columnStreams(...) -> RS
- **Signature:** `public abstract RS columnStreams()`
- **Summary:** Returns a stream of column streams.
- **Parameters:**
  - (none)
- **Returns:** a stream of column streams
- **Signature:** `public abstract RS columnStreams(final int fromColumnIndex, final int toColumnIndex)`
- **Summary:** Returns a stream of column streams for a range of columns.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a stream of column streams for the specified range
##### accept(...) -> void
- **Signature:** `public <E extends Exception> void accept(final Throwables.Consumer<? super M, E> action) throws E`
- **Summary:** Executes the specified action with this matrix as the parameter.
- **Contract:**
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code IntMatrix matrix = IntMatrix.of(new int\[\]\[\] {{1, 2, 3, 4, 5}, {6, 7, 8, 9, 10}, {11, 12, 13, 14, 15}}); // Log matrix details matrix.accept(m -> { System.out.println("Matrix dimensions: " + m.rowCount() + "x" + m.columnCount()); m.println(); }); // Validate matrix before processing matrix.accept(m -> { if (m.isEmpty()) { throw new IllegalStateException("Matrix cannot be empty"); } }); // Modify matrix elements in place matrix.accept(m -> { for (int i = 0; i < m.rowCount(); i++) { m.set(i, 0, 0); // Set first column to 0 } }); } </pre>
- **Parameters:**
  - `action` (`Throwables.Consumer<? super M, E>`) — the consumer action to perform on this matrix
- **Throws:**
  - `E` — if the action throws an exception
##### apply(...) -> R
- **Signature:** `public <R, E extends Exception> R apply(final Throwables.Function<? super M, R, E> mapper) throws E`
- **Summary:** Applies the specified function to this matrix and returns the result.
- **Parameters:**
  - `mapper` (`Throwables.Function<? super M, R, E>`) — the function to apply to this matrix
- **Returns:** the result of applying the function to this matrix
- **Throws:**
  - `E` — if the function throws an exception

### Class BooleanMatrix (com.landawn.abacus.matrix.BooleanMatrix)
Matrix implementation backed by a rectangular {@code boolean\[\]\[\]} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### empty(...) -> BooleanMatrix
- **Signature:** `public static BooleanMatrix empty()`
- **Summary:** Returns a shared empty {@code 0 × 0} matrix instance.
- **Parameters:**
  - (none)
- **Returns:** the empty boolean matrix singleton (zero rows, zero columns)
##### of(...) -> BooleanMatrix
- **Signature:** `public static BooleanMatrix of(final boolean[]... a)`
- **Summary:** Creates a BooleanMatrix from a two-dimensional boolean array.
- **Parameters:**
  - `a` (`boolean[][]`) — the two-dimensional boolean array to create the matrix from, or null/empty for an empty matrix
- **Returns:** a new BooleanMatrix containing the provided data, or an empty BooleanMatrix if input is null or empty
##### random(...) -> BooleanMatrix
- **Signature:** `public static BooleanMatrix random(final int length)`
- **Summary:** Creates a new {@code 1 × length} matrix filled with random boolean values.
- **Parameters:**
  - `length` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new BooleanMatrix of dimensions {@code 1 × length} filled with random values
- **See also:** #random(int, int)
- **Signature:** `public static BooleanMatrix random(final int rowCount, final int columnCount)`
- **Summary:** Creates a new matrix of the specified dimensions filled with random boolean values.
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new BooleanMatrix of dimensions rowCount x columnCount filled with random values
##### repeat(...) -> BooleanMatrix
- **Signature:** `public static BooleanMatrix repeat(final int rowCount, final int columnCount, final boolean element)`
- **Summary:** Creates a new matrix of the specified dimensions where every element is the provided {@code element} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
  - `element` (`boolean`) — the boolean value to fill the matrix with
- **Returns:** a new BooleanMatrix of dimensions rowCount x columnCount filled with the specified element
##### mainDiagonal(...) -> BooleanMatrix
- **Signature:** `public static BooleanMatrix mainDiagonal(final boolean[] mainDiagonal)`
- **Summary:** Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
- **Parameters:**
  - `mainDiagonal` (`boolean[]`) — the array of main diagonal elements; may be {@code null} or empty, in which case an empty matrix is returned
- **Returns:** a square matrix with the specified main diagonal ( {@code n × n} where {@code n} is the diagonal length), or an empty matrix if {@code mainDiagonal} is {@code null} or empty
- **See also:** #antiDiagonal(boolean\[\]), #diagonals(boolean\[\], boolean\[\])
##### antiDiagonal(...) -> BooleanMatrix
- **Signature:** `public static BooleanMatrix antiDiagonal(final boolean[] antiDiagonal)`
- **Summary:** Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
- **Parameters:**
  - `antiDiagonal` (`boolean[]`) — the array of anti-diagonal elements; may be {@code null} or empty, in which case an empty matrix is returned
- **Returns:** a square matrix with the specified anti-diagonal ( {@code n × n} where {@code n} is the diagonal length), or an empty matrix if {@code antiDiagonal} is {@code null} or empty
- **See also:** #mainDiagonal(boolean\[\]), #diagonals(boolean\[\], boolean\[\])
##### diagonals(...) -> BooleanMatrix
- **Signature:** `public static BooleanMatrix diagonals(final boolean[] mainDiagonal, final boolean[] antiDiagonal) throws IllegalArgumentException`
- **Summary:** Creates a square matrix from the specified main diagonal and anti-diagonal elements.
- **Contract:**
  - If both arrays are provided, they must have the same length.
  - When both diagonals are provided and they overlap (at the center element of odd-sized matrices), the main diagonal value takes precedence.
- **Parameters:**
  - `mainDiagonal` (`boolean[]`) — the array of main diagonal elements (can be null or empty)
  - `antiDiagonal` (`boolean[]`) — the array of anti-diagonal elements (can be null or empty)
- **Returns:** a square matrix with the specified diagonals, or an empty matrix if both inputs are null or empty
- **Throws:**
  - `java.lang.IllegalArgumentException` — if both arrays are non-empty and have different lengths
##### unbox(...) -> BooleanMatrix
- **Signature:** `public static BooleanMatrix unbox(final Matrix<Boolean> x)`
- **Summary:** Converts a boxed Boolean Matrix to a primitive BooleanMatrix.
- **Contract:**
  - This conversion improves memory efficiency and performance when working with large matrices.
- **Parameters:**
  - `x` (`Matrix<Boolean>`) — the boxed Boolean Matrix to convert; must not be null
- **Returns:** a new BooleanMatrix with primitive boolean values
- **See also:** #boxed()

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public BooleanMatrix(final boolean[][] a)`
- **Summary:** Constructs a {@code BooleanMatrix} backed by the supplied two-dimensional array.
- **Contract:**
  - <p> If {@code a} is {@code null} , this creates an empty {@code 0x0} matrix.
  - Call {@link #copy()} if you need an independently owned matrix.
- **Parameters:**
  - `a` (`boolean[][]`) — the two-dimensional boolean array to wrap, or {@code null} for an empty matrix
##### get(...) -> boolean
- **Signature:** `public boolean get(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** the element at position (rowIndex, columnIndex)
- **Signature:** `public boolean get(final Point point)`
- **Summary:** Returns the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
- **Returns:** the boolean element at the specified point
- **See also:** #get(int, int)
##### set(...) -> void
- **Signature:** `public void set(final int rowIndex, final int columnIndex, final boolean value)`
- **Summary:** Sets the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
  - `value` (`boolean`) — the value to set
- **Signature:** `public void set(final Point point, final boolean value)`
- **Summary:** Sets the element at the specified point to the given value.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
  - `value` (`boolean`) — the new boolean value to set at the specified point
- **See also:** #set(int, int, boolean)
##### valueAbove(...) -> OptionalBoolean
- **Signature:** `public OptionalBoolean valueAbove(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly above the specified position, if it exists.
- **Contract:**
  - Returns the element directly above the specified position, if it exists.
  - This method provides safe access to the element directly above the given position without throwing an exception when at the top edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalBoolean containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
##### valueBelow(...) -> OptionalBoolean
- **Signature:** `public OptionalBoolean valueBelow(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly below the specified position, if it exists.
- **Contract:**
  - Returns the element directly below the specified position, if it exists.
  - This method provides safe access to the element directly below the given position without throwing an exception when at the bottom edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalBoolean containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
##### valueLeft(...) -> OptionalBoolean
- **Signature:** `public OptionalBoolean valueLeft(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the left of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the left of the specified position, if it exists.
  - This method provides safe access to the element directly to the left of the given position without throwing an exception when at the leftmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalBoolean containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
##### valueRight(...) -> OptionalBoolean
- **Signature:** `public OptionalBoolean valueRight(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the right of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the right of the specified position, if it exists.
  - This method provides safe access to the element directly to the right of the given position without throwing an exception when at the rightmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalBoolean containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
##### rowView(...) -> boolean\[\]
- **Signature:** `@Override public boolean[] rowView(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as a boolean array.
- **Contract:**
  - If you need an independent copy, use {@link #rowCopy(int)} instead.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the specified row array (direct reference to internal storage)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### rowCopy(...) -> boolean\[\]
- **Signature:** `@Override public boolean[] rowCopy(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new boolean array containing the values from the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### columnCopy(...) -> boolean\[\]
- **Signature:** `@Override public boolean[] columnCopy(final int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a copy of the specified column as a new boolean array.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new array containing the values from the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex &lt; 0 or columnIndex &gt; = columnCount
##### setRow(...) -> void
- **Signature:** `public void setRow(final int rowIndex, final boolean[] row) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified row by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of columns in the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to set (0-based)
  - `row` (`boolean[]`) — the array of values to copy into the row; must have length equal to the number of columns
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex is out of bounds or row length does not match column count
##### setColumn(...) -> void
- **Signature:** `public void setColumn(final int columnIndex, final boolean[] column) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified column by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of rows in the matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to set (0-based)
  - `column` (`boolean[]`) — the array of values to copy into the column; must have length equal to the number of rows
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex is out of bounds or column length does not match row count
##### updateRow(...) -> void
- **Signature:** `public <E extends Exception> void updateRow(final int rowIndex, final Throwables.BooleanUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in a row in-place by applying the specified operator to each element.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to update (0-based)
  - `operator` (`Throwables.BooleanUnaryOperator<E>`) — the operator to apply to each element in the row; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### updateColumn(...) -> void
- **Signature:** `public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.BooleanUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified column in-place by applying the specified operator to each element.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to update (0-based)
  - `operator` (`Throwables.BooleanUnaryOperator<E>`) — the operator to apply to each element in the column; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### getMainDiagonal(...) -> boolean\[\]
- **Signature:** `@Override public boolean[] getMainDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the main diagonal elements (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new boolean array containing a copy of the main diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setMainDiagonal(...) -> void
- **Signature:** `@Override public void setMainDiagonal(final boolean[] mainDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `mainDiagonal` (`boolean[]`) — the new values for the main diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if mainDiagonal array length does not equal rowCount
##### updateMainDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateMainDiagonal(final Throwables.BooleanUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the main diagonal (upper-left to lower-right) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.BooleanUnaryOperator<E>`) — the operator to apply to each diagonal element; receives current element value and returns new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### getAntiDiagonal(...) -> boolean\[\]
- **Signature:** `@Override public boolean[] getAntiDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the elements on the anti-diagonal from upper-right to lower-left.
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new boolean array containing a copy of the anti-diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setAntiDiagonal(...) -> void
- **Signature:** `@Override public void setAntiDiagonal(final boolean[] antiDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `antiDiagonal` (`boolean[]`) — the new values for the anti-diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if antiDiagonal array length does not equal rowCount
##### updateAntiDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateAntiDiagonal(final Throwables.BooleanUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the anti-diagonal (upper-right to lower-left) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.BooleanUnaryOperator<E>`) — the operator to apply to each anti-diagonal element; receives current element value and returns new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### updateAll(...) -> void
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.BooleanUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the matrix in-place by applying the specified operator.
- **Contract:**
  - Elements are processed in row-major order when executed sequentially.
- **Parameters:**
  - `operator` (`Throwables.BooleanUnaryOperator<E>`) — the operator to apply to each element; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Boolean, E> mapper) throws E`
- **Summary:** Updates all elements in the matrix in-place based on their position (row and column indices).
- **Parameters:**
  - `mapper` (`Throwables.IntBiFunction<? extends Boolean, E>`) — the function that receives row index and column index (0-based) and returns the new value for that position
- **Throws:**
  - `E` — if the mapper throws an exception
##### replaceIf(...) -> void
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.BooleanPredicate<E> predicate, final boolean newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on a predicate.
- **Parameters:**
  - `predicate` (`Throwables.BooleanPredicate<E>`) — the condition to test each element; elements for which this returns {@code true} will be replaced
  - `newValue` (`boolean`) — the value to use for replacing matching elements
- **Throws:**
  - `E` — if the predicate throws an exception
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final boolean newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on their position (row and column indices).
- **Parameters:**
  - `predicate` (`Throwables.IntBiPredicate<E>`) — the condition that tests row index and column index (0-based); elements at positions for which this returns {@code true} will be replaced
  - `newValue` (`boolean`) — the value to use for replacing matching elements
- **Throws:**
  - `E` — if the predicate throws an exception
##### map(...) -> BooleanMatrix
- **Signature:** `public <E extends Exception> BooleanMatrix map(final Throwables.BooleanUnaryOperator<E> mapper) throws E`
- **Summary:** Creates a new BooleanMatrix by applying a transformation function to each element.
- **Parameters:**
  - `mapper` (`Throwables.BooleanUnaryOperator<E>`) — the function to apply to each element; receives the current element value and returns the transformed value
- **Returns:** a new BooleanMatrix with transformed values
- **Throws:**
  - `E` — if the function throws an exception
- **See also:** #updateAll(Throwables.BooleanUnaryOperator)
##### mapToObj(...) -> Matrix<R>
- **Signature:** `public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.BooleanFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E`
- **Summary:** Creates a new Matrix by applying a function that converts boolean values to objects of type R.
- **Parameters:**
  - `mapper` (`Throwables.BooleanFunction<? extends R, E>`) — the function to convert boolean values to type R
  - `targetElementType` (`Class<R>`) — the Class object for type R
- **Returns:** a new Matrix containing the converted values
- **Throws:**
  - `E` — if the function throws an exception
##### fill(...) -> void
- **Signature:** `public void fill(final boolean value)`
- **Summary:** Fills all elements in the matrix with the specified value.
- **Parameters:**
  - `value` (`boolean`) — the boolean value to fill the matrix with
- **Signature:** `public void fill(final boolean[][] source)`
- **Summary:** Fills the matrix with values from the provided two-dimensional array, starting from position (0, 0).
- **Parameters:**
  - `source` (`boolean[][]`) — the two-dimensional boolean array to copy values from; must not be null
- **Signature:** `public void fill(final int destRowIndex, final int destColumnIndex, final boolean[][] source) throws IllegalArgumentException`
- **Summary:** Fills a portion of the matrix with values from the provided two-dimensional array.
- **Contract:**
  - If the input array extends beyond the matrix boundaries, only the overlapping portion is copied.
- **Parameters:**
  - `destRowIndex` (`int`) — the target row index in this matrix (0-based)
  - `destColumnIndex` (`int`) — the target column index in this matrix (0-based)
  - `source` (`boolean[][]`) — the source array to copy values from; must not be null
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code source} is {@code null} , or if the target indices are negative or exceed matrix dimensions
##### copy(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a copy of this matrix with full independence guarantee
- **Signature:** `@Override public BooleanMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a row range from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new BooleanMatrix containing the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if {@code fromRowIndex} &lt; 0, {@code toRowIndex} &gt; rowCount, or {@code fromRowIndex} &gt; {@code toRowIndex}
- **Signature:** `@Override public BooleanMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a rectangular region from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new BooleanMatrix containing the specified rectangular region
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if {@code fromRowIndex} &lt; 0, {@code toRowIndex} &gt; rowCount, {@code fromColumnIndex} &lt; 0, {@code toColumnIndex} &gt; columnCount, {@code fromRowIndex} &gt; {@code toRowIndex} , or {@code fromColumnIndex} &gt; {@code toColumnIndex}
##### resize(...) -> BooleanMatrix
- **Signature:** `public BooleanMatrix resize(final int newRowCount, final int newColumnCount)`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code false} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
- **Returns:** a new BooleanMatrix with the specified dimensions
- **See also:** #resize(int, int, boolean), #extend(int, int, int, int)
- **Signature:** `public BooleanMatrix resize(final int newRowCount, final int newColumnCount, final boolean defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded.
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code defaultValue} .
  - Use {@code extend} when the entire original content must be preserved.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code BooleanMatrix matrix = BooleanMatrix.of(new boolean\[\]\[\] { {true, false, true}, {false, true, false}, {true, false, true} }); // Grow: fill new cells with true BooleanMatrix grown = matrix.resize(4, 4, true); // Result: \[\[true, false, true, true\], // \[false, true, false, true\], // \[true, false, true, true\], // \[true, true, true, true\]\] // Truncate: defaultValue is ignored when shrinking BooleanMatrix truncated = matrix.resize(2, 2, true); // Result: \[\[true, false\], // \[false, true\]\] } </pre>
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
  - `defaultValue` (`boolean`) — the value used to fill cells that are added when a dimension grows; ignored when a dimension shrinks
- **Returns:** a new BooleanMatrix with the specified dimensions
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code newRowCount} or {@code newColumnCount} is negative, or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
- **See also:** #resize(int, int), #extend(int, int, int, int, boolean)
##### extend(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight)`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
- **Returns:** a new BooleanMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **See also:** #extend(int, int, int, int, boolean), #resize(int, int)
- **Signature:** `public BooleanMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final boolean defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
  - `defaultValue` (`boolean`) — the value to fill all new padding cells with
- **Returns:** a new BooleanMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any padding parameter is negative, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** #extend(int, int, int, int), #resize(int, int, boolean)
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `@Override public void flipHorizontallyInPlace()`
- **Summary:** Reverses the order of elements in each row in-place (horizontal flip).
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally(), #flipVerticallyInPlace()
##### flipVerticallyInPlace(...) -> void
- **Signature:** `@Override public void flipVerticallyInPlace()`
- **Summary:** Reverses the order of rows in-place (vertical flip).
- **Parameters:**
  - (none)
- **See also:** #flipVertically(), #flipHorizontallyInPlace()
##### flipHorizontally(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix flipHorizontally()`
- **Summary:** Creates a horizontally flipped copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new BooleanMatrix with each row reversed
- **See also:** #flipHorizontallyInPlace(), #flipVertically(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### flipVertically(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix flipVertically()`
- **Summary:** Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
- **Parameters:**
  - (none)
- **Returns:** a new BooleanMatrix with rows reversed
- **See also:** #flipVerticallyInPlace(), #flipHorizontally(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### rotate90(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix rotated 90 degrees clockwise (dimensions {@code columnCount × rowCount} ), or an empty matrix if this matrix has zero columns
##### rotate180(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix rotated 180 degrees clockwise
##### rotate270(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise (or 90 degrees counter-clockwise).
- **Parameters:**
  - (none)
- **Returns:** a new matrix rotated 270 degrees clockwise (dimensions {@code columnCount × rowCount} ), or an empty matrix if this matrix has zero columns
##### transpose(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix transpose()`
- **Summary:** Creates the transpose of this matrix by swapping rows and columns.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is the transpose of this matrix with dimensions {@code columnCount × rowCount} , or an empty matrix if this matrix has zero columns
##### reshape(...) -> BooleanMatrix
- **Signature:** `@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG") @Override public BooleanMatrix reshape(final int newRowCount, final int newColumnCount)`
- **Summary:** Reshapes this matrix to the specified dimensions.
- **Contract:**
  - The new shape must have at least as many total elements as the original ( {@code (long) newRowCount * newColumnCount >= elementCount()} ).
  - <p> If the new shape has greater capacity than the number of source elements, trailing positions in the result are left as {@code false} .
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix; must be non-negative
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix; must be non-negative
- **Returns:** a new BooleanMatrix with the specified shape
##### repeatElements(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats each element in the matrix the specified number of times in both dimensions.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat each element vertically; must be {@code > 0}
  - `columnRepeats` (`int`) — number of times to repeat each element horizontally; must be {@code > 0}
- **Returns:** a new BooleanMatrix with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is not positive, or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
- **See also:** #repeatMatrix(int, int)
##### repeatMatrix(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats the entire matrix the specified number of times in both dimensions.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat the matrix vertically; must be {@code > 0}
  - `columnRepeats` (`int`) — number of times to repeat the matrix horizontally; must be {@code > 0}
- **Returns:** a new BooleanMatrix with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is not positive, or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
- **See also:** #repeatElements(int, int)
##### flatten(...) -> BooleanList
- **Signature:** `@Override public BooleanList flatten()`
- **Summary:** Returns a list containing all matrix elements in row-major order.
- **Parameters:**
  - (none)
- **Returns:** a list of all elements in row-major order
##### mutateAsFlat(...) -> void
- **Signature:** `@Override public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super boolean[], E> action) throws E`
- **Summary:** Flattens all elements of this matrix into a single one-dimensional array, applies the given operation to that flattened array, and then copies the modified elements back into the matrix.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super boolean[], E>`) — the operation to apply to the flattened array
- **Throws:**
  - `E` — if the operation throws an exception
- **See also:** Arrays#mutateAsFlat(boolean\[\]\[\], Throwables.Consumer)
##### and(...) -> BooleanMatrix
- **Signature:** `public BooleanMatrix and(final BooleanMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise logical AND of this matrix with another matrix.
- **Contract:**
  - The matrices must have the same dimensions.
- **Parameters:**
  - `other` (`BooleanMatrix`) — the matrix to AND with this matrix
- **Returns:** a new BooleanMatrix containing the element-wise logical AND
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrices have different dimensions
##### or(...) -> BooleanMatrix
- **Signature:** `public BooleanMatrix or(final BooleanMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise logical OR of this matrix with another matrix.
- **Contract:**
  - The matrices must have the same dimensions.
- **Parameters:**
  - `other` (`BooleanMatrix`) — the matrix to OR with this matrix
- **Returns:** a new BooleanMatrix containing the element-wise logical OR
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrices have different dimensions
##### xor(...) -> BooleanMatrix
- **Signature:** `public BooleanMatrix xor(final BooleanMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise logical XOR of this matrix with another matrix.
- **Contract:**
  - The matrices must have the same dimensions.
- **Parameters:**
  - `other` (`BooleanMatrix`) — the matrix to XOR with this matrix
- **Returns:** a new BooleanMatrix containing the element-wise logical XOR
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrices have different dimensions
##### countTrue(...) -> long
- **Signature:** `public long countTrue()`
- **Summary:** Counts the number of {@code true} elements in this matrix.
- **Parameters:**
  - (none)
- **Returns:** the number of {@code true} elements in this matrix, as a non-negative {@code long}
##### allTrue(...) -> boolean
- **Signature:** `public boolean allTrue()`
- **Summary:** Returns {@code true} if all elements in this matrix are {@code true} .
- **Contract:**
  - Returns {@code true} if all elements in this matrix are {@code true} .
- **Parameters:**
  - (none)
- **Returns:** {@code true} if every element is {@code true} , or if the matrix is empty
##### anyTrue(...) -> boolean
- **Signature:** `public boolean anyTrue()`
- **Summary:** Returns {@code true} if any element in this matrix is {@code true} .
- **Contract:**
  - Returns {@code true} if any element in this matrix is {@code true} .
- **Parameters:**
  - (none)
- **Returns:** {@code true} if at least one element is {@code true}
##### stackVertically(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix stackVertically(final BooleanMatrix other) throws IllegalArgumentException`
- **Summary:** Stacks this matrix vertically with another matrix (vertical concatenation).
- **Contract:**
  - The matrices must have the same number of columns.
- **Parameters:**
  - `other` (`BooleanMatrix`) — the matrix to stack below this matrix (must have the same column count)
- **Returns:** a new BooleanMatrix with dimensions {@code (this.rowCount + other.rowCount) × this.columnCount}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , if {@code this.columnCount != other.columnCount} , or if the merged row count would overflow {@code Integer.MAX_VALUE}
- **See also:** #stackHorizontally(BooleanMatrix)
##### stackHorizontally(...) -> BooleanMatrix
- **Signature:** `@Override public BooleanMatrix stackHorizontally(final BooleanMatrix other) throws IllegalArgumentException`
- **Summary:** Stacks this matrix horizontally with another matrix (horizontal concatenation).
- **Contract:**
  - The matrices must have the same number of rows.
- **Parameters:**
  - `other` (`BooleanMatrix`) — the matrix to stack to the right of this matrix (must have the same row count)
- **Returns:** a new BooleanMatrix with dimensions {@code this.rowCount × (this.columnCount + other.columnCount)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , if {@code this.rowCount != other.rowCount} , or if the merged column count would overflow {@code Integer.MAX_VALUE}
- **See also:** #stackVertically(BooleanMatrix)
##### boxed(...) -> Matrix<Boolean>
- **Signature:** `public Matrix<Boolean> boxed()`
- **Summary:** Converts this primitive boolean matrix to a boxed Boolean Matrix.
- **Contract:**
  - <p> This conversion is useful when you need to work with APIs that require object types rather than primitives, or when you need null values in the matrix.
- **Parameters:**
  - (none)
- **Returns:** a new Matrix &lt; Boolean &gt; with the same dimensions and values as this matrix
- **See also:** #unbox(Matrix)
##### zipWith(...) -> BooleanMatrix
- **Signature:** `public <E extends Exception> BooleanMatrix zipWith(final BooleanMatrix other, final Throwables.BooleanBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Performs element-wise operation on two matrices using a binary operator.
- **Contract:**
  - The matrices must have the same dimensions.
- **Parameters:**
  - `other` (`BooleanMatrix`) — the second matrix (must have the same dimensions as this matrix)
  - `zipFunction` (`Throwables.BooleanBinaryOperator<E>`) — the binary operator to apply to corresponding elements; receives element from this matrix as first argument and element from {@code other} as second argument
- **Returns:** a new BooleanMatrix with the results of the element-wise operation
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices have different dimensions (shape mismatch), or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception
- **See also:** #zipWith(BooleanMatrix, BooleanMatrix, Throwables.BooleanTernaryOperator)
- **Signature:** `public <E extends Exception> BooleanMatrix zipWith(final BooleanMatrix other, final BooleanMatrix third, final Throwables.BooleanTernaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Performs element-wise operation on three matrices using a ternary operator.
- **Contract:**
  - All matrices must have the same dimensions.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code BooleanMatrix a = BooleanMatrix.of(new boolean\[\]\[\] {{true, false}, {true, true}}); BooleanMatrix b = BooleanMatrix.of(new boolean\[\]\[\] {{true, true}, {false, true}}); BooleanMatrix c = BooleanMatrix.of(new boolean\[\]\[\] {{false, true}, {true, false}}); // Majority vote: true if at least 2 out of 3 are true BooleanMatrix majority = a.zipWith(b, c, (x, y, z) -> (x && y) || (x && z) || (y && z)); // Conditional operation: if a then b else c BooleanMatrix conditional = a.zipWith(b, c, (x, y, z) -> x ?
- **Parameters:**
  - `other` (`BooleanMatrix`) — the second matrix (must have the same dimensions as this matrix)
  - `third` (`BooleanMatrix`) — the third matrix (must have the same dimensions as this matrix)
  - `zipFunction` (`Throwables.BooleanTernaryOperator<E>`) — the ternary operator to apply to corresponding elements; receives element from this matrix as first argument, element from {@code other} as second argument, and element from {@code third} as third argument
- **Returns:** a new BooleanMatrix with the results of the element-wise operation
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any matrices have different dimensions (shape mismatch), or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception
- **See also:** #zipWith(BooleanMatrix, Throwables.BooleanBinaryOperator)
##### mainDiagonalStream(...) -> Stream<Boolean>
- **Signature:** `@Override public Stream<Boolean> mainDiagonalStream()`
- **Summary:** Returns a stream of Boolean values from the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (same number of rows and columns).
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code BooleanMatrix matrix = BooleanMatrix.of(new boolean\[\]\[\] { {true, false, false}, {false, true, false}, {false, false, true} }); List<Boolean> diagonal = matrix.mainDiagonalStream().toList(); // \[true, true, true\] // Check if it's an identity-like matrix boolean allTrue = matrix.mainDiagonalStream().allMatch(b -> b); } </pre>
- **Parameters:**
  - (none)
- **Returns:** a Stream &lt; Boolean &gt; containing the diagonal elements from top-left to bottom-right
##### antiDiagonalStream(...) -> Stream<Boolean>
- **Signature:** `@Override public Stream<Boolean> antiDiagonalStream()`
- **Summary:** Returns a stream of Boolean values from the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a Stream &lt; Boolean &gt; containing the anti-diagonal elements from top-right to bottom-left
##### horizontalStream(...) -> Stream<Boolean>
- **Signature:** `@Override public Stream<Boolean> horizontalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
- **Parameters:**
  - (none)
- **Returns:** a Stream &lt; Boolean &gt; of all elements in row-major order, or an empty stream if the matrix is empty
- **Signature:** `@Override public Stream<Boolean> horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a single row.
- **Contract:**
  - <p> This method is particularly useful when you need to process or analyze a specific row of the matrix independently.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code BooleanMatrix matrix = BooleanMatrix.of(new boolean\[\]\[\] { {true, false, true}, {false, true, false} }); Stream<Boolean> firstRow = matrix.horizontalStream(0); // Stream of \[true, false, true\] // Check if any value in the second row is true boolean hasTrue = matrix.horizontalStream(1).anyMatch(b -> b); // Returns true } </pre>
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to stream (0-based)
- **Returns:** a Stream &lt; Boolean &gt; of elements from the specified row
- **Signature:** `@Override public Stream<Boolean> horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of rows in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a Stream &lt; Boolean &gt; of elements from the specified row range, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
##### verticalStream(...) -> Stream<Boolean>
- **Signature:** `@Override @Beta public Stream<Boolean> verticalStream()`
- **Summary:** Returns a stream of all elements in column-major order (vertical).
- **Parameters:**
  - (none)
- **Returns:** a Stream &lt; Boolean &gt; of all elements in column-major order, or an empty stream if the matrix is empty
- **Signature:** `@Override public Stream<Boolean> verticalStream(final int columnIndex)`
- **Summary:** Returns a stream of elements from a single column.
- **Contract:**
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code BooleanMatrix matrix = BooleanMatrix.of(new boolean\[\]\[\] { {true, false, true}, {true, true, false} }); Stream<Boolean> firstCol = matrix.verticalStream(0); // Stream of \[true, true\] // Check if all values in a column are true boolean allTrue = matrix.verticalStream(0).allMatch(b -> b); // Returns true } </pre>
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to stream (0-based)
- **Returns:** a Stream &lt; Boolean &gt; of elements from the specified column
- **Signature:** `@Override @Beta public Stream<Boolean> verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of columns in column-major order.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a Stream &lt; Boolean &gt; of elements from the specified column range in column-major order, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
##### rowStreams(...) -> Stream<Stream<Boolean>>
- **Signature:** `@Override public Stream<Stream<Boolean>> rowStreams()`
- **Summary:** Returns a stream of Stream &lt; Boolean &gt; objects, where each inner stream represents a complete row.
- **Parameters:**
  - (none)
- **Returns:** a Stream of Stream &lt; Boolean &gt; objects, one for each row in the matrix
- **Signature:** `@Override public Stream<Stream<Boolean>> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of Stream &lt; Boolean &gt; objects for a range of rows.
- **Contract:**
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code BooleanMatrix matrix = BooleanMatrix.of(new boolean\[\]\[\] { {true, true, false}, {false, true, true}, {true, false, true} }); // Process middle rows only List<Boolean> hasPattern = matrix.rowStreams(1, 3) .map(row -> { List<Boolean> list = row.toList(); return list.get(0) != list.get(2); // Check if first != last }) .toList(); // \[true, false\] } </pre>
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a Stream of Stream &lt; Boolean &gt; objects for the specified row range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
##### columnStreams(...) -> Stream<Stream<Boolean>>
- **Signature:** `@Override @Beta public Stream<Stream<Boolean>> columnStreams()`
- **Summary:** Returns a stream of Stream &lt; Boolean &gt; objects, where each inner stream represents a complete column.
- **Parameters:**
  - (none)
- **Returns:** a Stream of Stream &lt; Boolean &gt; objects, one for each column in the matrix, or an empty stream if the matrix is empty
- **Signature:** `@Override @Beta public Stream<Stream<Boolean>> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of Stream &lt; Boolean &gt; objects for a range of columns.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a Stream of Stream &lt; Boolean &gt; objects for the specified column range, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
##### forEach(...) -> void
- **Signature:** `public <E extends Exception> void forEach(final Throwables.BooleanConsumer<E> action) throws E`
- **Summary:** Performs the specified action for each element in this matrix.
- **Contract:**
  - Elements are processed in row-major order (row by row, left to right) when executed sequentially.
  - If parallelized, the order of execution is not guaranteed, but all elements will be processed exactly once.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code BooleanMatrix matrix = BooleanMatrix.of(new boolean\[\]\[\] {{true, false}, {false, true}}); // Count true values int\[\] trueCount = {0}; matrix.forEach(value -> { if (value) trueCount\[0\]++; }); // trueCount\[0\] is now 2 // Print all values matrix.forEach(value -> System.out.print(value ?
- **Parameters:**
  - `action` (`Throwables.BooleanConsumer<E>`) — the action to be performed for each element; receives each element value
- **Throws:**
  - `E` — if the action throws an exception
- **See also:** #forEach(int, int, int, int, Throwables.BooleanConsumer)
- **Signature:** `public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.BooleanConsumer<E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element in the specified sub-matrix region.
- **Contract:**
  - The operation may be parallelized internally if the sub-matrix is large enough to benefit from parallel processing.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code BooleanMatrix matrix = BooleanMatrix.of(new boolean\[\]\[\] { {true, false, true}, {false, true, false}, {true, true, true} }); // Process only the top-left 2x2 sub-matrix List<Boolean> center = new ArrayList<>(); matrix.forEach(0, 2, 0, 2, value -> center.add(value)); // center contains \[true, false, false, true\] // Count true values in bottom row int\[\] bottomRowTrue = {0}; matrix.forEach(2, 3, 0, 3, value -> { if (value) bottomRowTrue\[0\]++; }); // bottomRowTrue\[0\] is 3 } </pre>
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.BooleanConsumer<E>`) — the action to be performed for each element in the sub-matrix
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds or fromIndex &gt; toIndex
  - `E` — if the action throws an exception
##### println(...) -> String
- **Signature:** `@Override public String println()`
- **Summary:** Prints this matrix to standard output and returns the formatted string.
- **Contract:**
  - If the matrix is empty, {@code \[\]} is printed.
- **Parameters:**
  - (none)
- **Returns:** the formatted string representation of the matrix
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this matrix.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this matrix
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this matrix to the specified object for equality.
- **Contract:**
  - Returns {@code true} if and only if the given object is also a {@code BooleanMatrix} with the same dimensions and all corresponding elements are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with; may be {@code null}
- **Returns:** {@code true} if {@code obj} is a {@code BooleanMatrix} of the same shape and content, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this matrix

### Class ByteMatrix (com.landawn.abacus.matrix.ByteMatrix)
Matrix implementation backed by a rectangular {@code byte\[\]\[\]} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### empty(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix empty()`
- **Summary:** Creates an empty matrix with zero rows and zero columns.
- **Parameters:**
  - (none)
- **Returns:** an empty byte matrix
##### of(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix of(final byte[]... a)`
- **Summary:** Creates a ByteMatrix from a two-dimensional byte array.
- **Parameters:**
  - `a` (`byte[][]`) — the two-dimensional byte array to create the matrix from, or {@code null} /empty for an empty matrix
- **Returns:** a new {@code ByteMatrix} containing the provided data, or an empty {@code ByteMatrix} if input is {@code null} or empty
##### random(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix random(final int length)`
- **Summary:** Creates a new {@code 1 x length} matrix filled with random byte values uniformly distributed across the full byte range {@code \[Byte.MIN_VALUE, Byte.MAX_VALUE\]} .
- **Parameters:**
  - `length` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new {@code ByteMatrix} of dimensions {@code 1 x length} filled with random values
- **Signature:** `public static ByteMatrix random(final int rowCount, final int columnCount)`
- **Summary:** Creates a new matrix of the specified dimensions filled with random byte values uniformly distributed across the full byte range {@code \[Byte.MIN_VALUE, Byte.MAX_VALUE\]} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new {@code ByteMatrix} of dimensions {@code rowCount x columnCount} filled with random values
##### repeat(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix repeat(final int rowCount, final int columnCount, final byte element)`
- **Summary:** Creates a new matrix of the specified dimensions where every element is the provided {@code element} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
  - `element` (`byte`) — the byte value to fill the matrix with
- **Returns:** a new {@code ByteMatrix} of dimensions {@code rowCount x columnCount} with every cell set to {@code element}
##### range(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix range(final byte startInclusive, final byte endExclusive)`
- **Summary:** Creates a 1-row ByteMatrix containing a range of byte values from startInclusive to endExclusive.
- **Contract:**
  - If {@code startInclusive >= endExclusive} , a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`byte`) — the starting value (inclusive)
  - `endExclusive` (`byte`) — the ending value (exclusive)
- **Returns:** a new 1×n ByteMatrix where n = max(0, endExclusive - startInclusive)
- **Signature:** `public static ByteMatrix range(final byte startInclusive, final byte endExclusive, final byte step)`
- **Summary:** Creates a 1-row ByteMatrix containing a range of byte values with a specified step.
- **Contract:**
  - If the step would not reach endExclusive from startInclusive, a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`byte`) — the starting value (inclusive)
  - `endExclusive` (`byte`) — the ending value (exclusive)
  - `step` (`byte`) — the step size (must not be zero; can be positive or negative)
- **Returns:** a new 1×n ByteMatrix with values incremented by the step size
##### rangeClosed(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix rangeClosed(final byte startInclusive, final byte endInclusive)`
- **Summary:** Creates a 1-row ByteMatrix containing a closed range of byte values from startInclusive to endInclusive.
- **Contract:**
  - If {@code startInclusive > endInclusive} , a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`byte`) — the starting value (inclusive)
  - `endInclusive` (`byte`) — the ending value (inclusive)
- **Returns:** a new 1×n ByteMatrix where n = max(0, endInclusive - startInclusive + 1)
- **Signature:** `public static ByteMatrix rangeClosed(final byte startInclusive, final byte endInclusive, final byte step)`
- **Summary:** Creates a 1-row ByteMatrix containing a closed range of byte values with a specified step.
- **Contract:**
  - The end value is included only if it is reachable by stepping from start.
  - If the step would not reach endInclusive from startInclusive, a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`byte`) — the starting value (inclusive)
  - `endInclusive` (`byte`) — the ending value (inclusive, if reachable by stepping)
  - `step` (`byte`) — the step size (must not be zero; can be positive or negative)
- **Returns:** a new 1×n ByteMatrix with values incremented by the step size
##### mainDiagonal(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix mainDiagonal(final byte[] mainDiagonal)`
- **Summary:** Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
- **Parameters:**
  - `mainDiagonal` (`byte[]`) — the array of diagonal elements; may be {@code null} or empty
- **Returns:** a square {@code n×n} matrix with the specified main diagonal, where {@code n} is the array length, or an empty matrix if {@code mainDiagonal} is {@code null} or empty
- **See also:** #antiDiagonal(byte\[\]), #diagonals(byte\[\], byte\[\])
##### antiDiagonal(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix antiDiagonal(final byte[] antiDiagonal)`
- **Summary:** Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
- **Parameters:**
  - `antiDiagonal` (`byte[]`) — the array of anti-diagonal elements; may be {@code null} or empty
- **Returns:** a square {@code n×n} matrix with the specified anti-diagonal, where {@code n} is the array length, or an empty matrix if {@code antiDiagonal} is {@code null} or empty
- **See also:** #mainDiagonal(byte\[\]), #diagonals(byte\[\], byte\[\])
##### diagonals(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix diagonals(final byte[] mainDiagonal, final byte[] antiDiagonal) throws IllegalArgumentException`
- **Summary:** Creates a square matrix from the specified main diagonal and anti-diagonal elements.
- **Contract:**
  - If both arrays are provided, they must have the same length.
  - When both diagonals are provided and they overlap (at the center element of odd-sized matrices), the main diagonal value takes precedence.
- **Parameters:**
  - `mainDiagonal` (`byte[]`) — the array of main diagonal elements (can be null or empty)
  - `antiDiagonal` (`byte[]`) — the array of anti-diagonal elements (can be null or empty)
- **Returns:** a square matrix with the specified diagonals, or an empty matrix if both inputs are null or empty
- **Throws:**
  - `java.lang.IllegalArgumentException` — if both arrays are non-empty and have different lengths
##### unbox(...) -> ByteMatrix
- **Signature:** `public static ByteMatrix unbox(final Matrix<Byte> x)`
- **Summary:** Converts a boxed {@code Matrix<Byte>} to a primitive {@code ByteMatrix} .
- **Contract:**
  - This conversion improves memory efficiency and performance when working with large matrices.
- **Parameters:**
  - `x` (`Matrix<Byte>`) — the boxed {@code Matrix<Byte>} to convert; must not be {@code null}
- **Returns:** a new {@code ByteMatrix} with primitive byte values
- **See also:** #boxed()

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public ByteMatrix(final byte[][] a)`
- **Summary:** Constructs a {@code ByteMatrix} backed by the supplied two-dimensional array.
- **Contract:**
  - <p> If {@code a} is {@code null} , this creates an empty {@code 0x0} matrix.
  - Call {@link #copy()} if you need an independently owned matrix.
- **Parameters:**
  - `a` (`byte[][]`) — the two-dimensional byte array to wrap, or {@code null} for an empty matrix
##### get(...) -> byte
- **Signature:** `public byte get(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** the element at position (rowIndex, columnIndex)
- **Signature:** `public byte get(final Point point)`
- **Summary:** Returns the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
- **Returns:** the byte element at the specified point
- **See also:** #get(int, int)
##### set(...) -> void
- **Signature:** `public void set(final int rowIndex, final int columnIndex, final byte value)`
- **Summary:** Sets the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
  - `value` (`byte`) — the value to set
- **Signature:** `public void set(final Point point, final byte value)`
- **Summary:** Sets the element at the specified point to the given value.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
  - `value` (`byte`) — the new byte value to set at the specified point
- **See also:** #set(int, int, byte)
##### valueAbove(...) -> OptionalByte
- **Signature:** `public OptionalByte valueAbove(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly above the specified position, if it exists.
- **Contract:**
  - Returns the element directly above the specified position, if it exists.
  - This method provides safe access to the element directly above the given position without throwing an exception when at the top edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalByte containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
##### valueBelow(...) -> OptionalByte
- **Signature:** `public OptionalByte valueBelow(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly below the specified position, if it exists.
- **Contract:**
  - Returns the element directly below the specified position, if it exists.
  - This method provides safe access to the element directly below the given position without throwing an exception when at the bottom edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalByte containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
##### valueLeft(...) -> OptionalByte
- **Signature:** `public OptionalByte valueLeft(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the left of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the left of the specified position, if it exists.
  - This method provides safe access to the element directly to the left of the given position without throwing an exception when at the leftmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalByte containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
##### valueRight(...) -> OptionalByte
- **Signature:** `public OptionalByte valueRight(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the right of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the right of the specified position, if it exists.
  - This method provides safe access to the element directly to the right of the given position without throwing an exception when at the rightmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalByte containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
##### rowView(...) -> byte\[\]
- **Signature:** `@Override public byte[] rowView(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as a byte array.
- **Contract:**
  - If you need an independent copy, use {@link #rowCopy(int)} instead.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the specified row array (direct reference to internal storage)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### rowCopy(...) -> byte\[\]
- **Signature:** `@Override public byte[] rowCopy(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new byte array containing the values from the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### columnCopy(...) -> byte\[\]
- **Signature:** `@Override public byte[] columnCopy(final int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a copy of the specified column as a new byte array.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new array containing the values from the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex &lt; 0 or columnIndex &gt; = columnCount
##### setRow(...) -> void
- **Signature:** `public void setRow(final int rowIndex, final byte[] row) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified row by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of columns in the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to set (0-based)
  - `row` (`byte[]`) — the array of values to copy into the row; must have length equal to the number of columns
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex is out of bounds or row length does not match columnCount
##### setColumn(...) -> void
- **Signature:** `public void setColumn(final int columnIndex, final byte[] column) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified column by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of rows in the matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to set (0-based)
  - `column` (`byte[]`) — the array of values to copy into the column; must have length equal to the number of rows
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex is out of bounds or column length does not match rowCount
##### updateRow(...) -> void
- **Signature:** `public <E extends Exception> void updateRow(final int rowIndex, final Throwables.ByteUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified row by applying the given operator to each element.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to update (0-based)
  - `operator` (`Throwables.ByteUnaryOperator<E>`) — the unary operator to apply to each element in the row, taking a byte and returning a byte
- **Throws:**
  - `E` — if the operator throws an exception
##### updateColumn(...) -> void
- **Signature:** `public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.ByteUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified column by applying the given operator to each element.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to update (0-based)
  - `operator` (`Throwables.ByteUnaryOperator<E>`) — the unary operator to apply to each element in the column, taking a byte and returning a byte
- **Throws:**
  - `E` — if the operator throws an exception
##### getMainDiagonal(...) -> byte\[\]
- **Signature:** `@Override public byte[] getMainDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the main diagonal elements from upper-left to lower-right.
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new byte array containing the main diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setMainDiagonal(...) -> void
- **Signature:** `@Override public void setMainDiagonal(final byte[] mainDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `mainDiagonal` (`byte[]`) — the new values for the main diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if mainDiagonal array length does not equal rowCount
##### updateMainDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateMainDiagonal(final Throwables.ByteUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates all elements on the main diagonal (upper-left to lower-right) by applying the given operator.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - `operator` (`Throwables.ByteUnaryOperator<E>`) — the operator to apply to each diagonal element
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `E` — if the operator throws an exception
##### getAntiDiagonal(...) -> byte\[\]
- **Signature:** `@Override public byte[] getAntiDiagonal() throws IllegalStateException`
- **Summary:** Returns the elements on the anti-diagonal from upper-right to lower-left.
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new byte array containing the anti-diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setAntiDiagonal(...) -> void
- **Signature:** `@Override public void setAntiDiagonal(final byte[] antiDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `antiDiagonal` (`byte[]`) — the new values for the anti-diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if antiDiagonal array length does not equal rowCount
##### updateAntiDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateAntiDiagonal(final Throwables.ByteUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates all elements on the anti-diagonal (upper-right to lower-left) by applying the given operator.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - `operator` (`Throwables.ByteUnaryOperator<E>`) — the operator to apply to each anti-diagonal element
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `E` — if the operator throws an exception
##### updateAll(...) -> void
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.ByteUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the matrix by applying the given operator to each element.
- **Parameters:**
  - `operator` (`Throwables.ByteUnaryOperator<E>`) — the unary operator to apply to each element, taking a byte and returning a byte; must not be {@code null}
- **Throws:**
  - `E` — if the operator throws an exception
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Byte, E> mapper) throws E`
- **Summary:** Updates all elements in the matrix based on their position by applying the given mapper.
- **Parameters:**
  - `mapper` (`Throwables.IntBiFunction<? extends Byte, E>`) — the bi-function that takes {@code (rowIndex, columnIndex)} and returns the new byte value; must not be {@code null}
- **Throws:**
  - `E` — if the mapper throws an exception
##### replaceIf(...) -> void
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.BytePredicate<E> predicate, final byte newValue) throws E`
- **Summary:** Conditionally replaces elements in the matrix based on a predicate.
- **Parameters:**
  - `predicate` (`Throwables.BytePredicate<E>`) — the condition to test each element; returns {@code true} if the element should be replaced; must not be {@code null}
  - `newValue` (`byte`) — the value to use as replacement
- **Throws:**
  - `E` — if the predicate throws an exception
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final byte newValue) throws E`
- **Summary:** Conditionally replaces elements in the matrix based on their position.
- **Parameters:**
  - `predicate` (`Throwables.IntBiPredicate<E>`) — the bi-predicate that takes {@code (rowIndex, columnIndex)} and returns {@code true} if the element should be replaced; must not be {@code null}
  - `newValue` (`byte`) — the value to use as replacement
- **Throws:**
  - `E` — if the predicate throws an exception
##### map(...) -> ByteMatrix
- **Signature:** `public <E extends Exception> ByteMatrix map(final Throwables.ByteUnaryOperator<E> mapper) throws E`
- **Summary:** Creates a new ByteMatrix by applying the given function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.ByteUnaryOperator<E>`) — the unary operator to apply to each element, taking a byte and returning a byte; must not be {@code null}
- **Returns:** a new {@code ByteMatrix} with the transformed values; the original matrix is unchanged
- **Throws:**
  - `E` — if the function throws an exception
##### mapToObj(...) -> Matrix<R>
- **Signature:** `public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.ByteFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E`
- **Summary:** Creates a new object matrix by applying the given function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.ByteFunction<? extends R, E>`) — the function to transform each byte to an object of type {@code R} ; must not be {@code null}
  - `targetElementType` (`Class<R>`) — the class of the target element type (used for array creation); must not be {@code null}
- **Returns:** a new {@code Matrix<R>} with the transformed object values; the original matrix is unchanged
- **Throws:**
  - `E` — if the function throws an exception
##### fill(...) -> void
- **Signature:** `public void fill(final byte value)`
- **Summary:** Fills all elements of the matrix with the specified value.
- **Parameters:**
  - `value` (`byte`) — the value to fill the matrix with
- **Signature:** `public void fill(final byte[][] source)`
- **Summary:** Fills this matrix with values from another two-dimensional byte array, starting from position {@code \[0,0\]} .
- **Contract:**
  - If the source array is smaller than this matrix, only the overlapping portion is modified (cells outside the source remain unchanged).
  - If the source array is larger, only the portion that fits within this matrix is copied.
- **Parameters:**
  - `source` (`byte[][]`) — the source array to copy values from; must not be {@code null}
- **See also:** #fill(int, int, byte\[\]\[\])
- **Signature:** `public void fill(final int destRowIndex, final int destColumnIndex, final byte[][] source) throws IllegalArgumentException`
- **Summary:** Fills a portion of this matrix with values from another two-dimensional byte array.
- **Parameters:**
  - `destRowIndex` (`int`) — the target row index in this matrix; must be in {@code \[0, rowCount\]}
  - `destColumnIndex` (`int`) — the target column index in this matrix; must be in {@code \[0, columnCount\]}
  - `source` (`byte[][]`) — the source array to copy values from; must not be {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code source} is {@code null} , or if {@code destRowIndex} or {@code destColumnIndex} is negative or exceeds the corresponding matrix dimension
##### copy(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a copy of this matrix
- **Signature:** `@Override public ByteMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a row range from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new ByteMatrix containing the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds
- **Signature:** `@Override public ByteMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a rectangular region from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new ByteMatrix containing the specified region
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any indices are out of bounds
##### resize(...) -> ByteMatrix
- **Signature:** `public ByteMatrix resize(final int newRowCount, final int newColumnCount)`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code 0} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
- **Returns:** a new ByteMatrix with the specified dimensions
- **See also:** #resize(int, int, byte), #extend(int, int, int, int)
- **Signature:** `public ByteMatrix resize(final int newRowCount, final int newColumnCount, final byte defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded.
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code defaultValue} .
  - Use {@code extend} when the entire original content must be preserved.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code ByteMatrix matrix = ByteMatrix.of(new byte\[\]\[\] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}); // Grow: fill new cells with 9 ByteMatrix grown = matrix.resize(4, 4, (byte) 9); // Result: \[\[1, 2, 3, 9\], // \[4, 5, 6, 9\], // \[7, 8, 9, 9\], // \[9, 9, 9, 9\]\] // Truncate: defaultValue is ignored when shrinking ByteMatrix truncated = matrix.resize(2, 2, (byte) 9); // Result: \[\[1, 2\], // \[4, 5\]\] } </pre>
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
  - `defaultValue` (`byte`) — the value used to fill cells that are added when a dimension grows; ignored when a dimension shrinks
- **Returns:** a new ByteMatrix with the specified dimensions
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code newRowCount} or {@code newColumnCount} is negative, or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
- **See also:** #resize(int, int), #extend(int, int, int, int, byte)
##### extend(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight)`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
- **Returns:** a new ByteMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **See also:** #extend(int, int, int, int, byte), #resize(int, int)
- **Signature:** `public ByteMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final byte defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
  - `defaultValue` (`byte`) — the value to fill all new padding cells with
- **Returns:** a new ByteMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any padding parameter is negative, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** #extend(int, int, int, int), #resize(int, int, byte)
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `@Override public void flipHorizontallyInPlace()`
- **Summary:** Reverses the order of elements in each row horizontally in-place.
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally()
##### flipVerticallyInPlace(...) -> void
- **Signature:** `@Override public void flipVerticallyInPlace()`
- **Summary:** Reverses the order of rows in the matrix (vertical flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipVertically()
##### flipHorizontally(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix flipHorizontally()`
- **Summary:** Returns a new matrix that is a horizontal flip of this matrix (each row reversed).
- **Parameters:**
  - (none)
- **Returns:** a new ByteMatrix that is a horizontal flip of this matrix (each row reversed)
- **See also:** #flipHorizontallyInPlace(), #flipVertically(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### flipVertically(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix flipVertically()`
- **Summary:** Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
- **Parameters:**
  - (none)
- **Returns:** a new ByteMatrix that is a vertical flip of this matrix (rows in reversed order)
- **See also:** #flipVerticallyInPlace(), #flipHorizontally(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### rotate90(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix rotated 90 degrees clockwise with dimensions {@code (columnCount x rowCount)}
- **See also:** #rotate180(), #rotate270()
##### rotate180(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix rotated 180 degrees with the same dimensions
- **See also:** #rotate90(), #rotate270()
##### rotate270(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise (or 90 degrees counter-clockwise).
- **Parameters:**
  - (none)
- **Returns:** a new matrix rotated 270 degrees clockwise with dimensions {@code (columnCount x rowCount)}
- **See also:** #rotate90(), #rotate180()
##### transpose(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix transpose()`
- **Summary:** Creates the transpose of this matrix by swapping rows and columns.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is the transpose of this matrix with dimensions {@code columnCount × rowCount}
##### reshape(...) -> ByteMatrix
- **Signature:** `@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG") @Override public ByteMatrix reshape(final int newRowCount, final int newColumnCount)`
- **Summary:** Reshapes the matrix to new dimensions while preserving element order.
- **Contract:**
  - <p> The reshaping process follows these rules: <ul> <li> Elements are extracted from the original matrix in row-major order (left to right, top to bottom) </li> <li> Elements are placed into the new matrix in row-major order </li> <li> The new shape must have at least as many total elements as the original ( {@code newRowCount * newColumnCount >= elementCount()} ) </li> <li> If the new shape has more total elements, the additional positions are filled with zeros </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code ByteMatrix matrix = ByteMatrix.of(new byte\[\]\[\] {{1, 2, 3}, {4, 5, 6}}); ByteMatrix reshaped = matrix.reshape(3, 2); // Becomes \[\[1, 2\], \[3, 4\], \[5, 6\]\] ByteMatrix extended = matrix.reshape(2, 4); // Becomes \[\[1, 2, 3, 4\], \[5, 6, 0, 0\]\] } </pre>
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix; must be {@code >= 0}
- **Returns:** a new {@code ByteMatrix} with the specified shape containing this matrix's elements
- **See also:** #resize(int, int)
##### repeatElements(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Creates a new matrix by repeating each element multiple times.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat each element vertically
  - `columnRepeats` (`int`) — number of times to repeat each element horizontally
- **Returns:** a new matrix with repeated elements
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowRepeats or columnRepeats is not positive, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#repeatElements(int, int)
##### repeatMatrix(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Creates a new matrix by repeating the entire matrix multiple times.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat the matrix vertically
  - `columnRepeats` (`int`) — number of times to repeat the matrix horizontally
- **Returns:** a new matrix with the original matrix repeated
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowRepeats or columnRepeats is not positive, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#repeatMatrix(int, int)
##### flatten(...) -> ByteList
- **Signature:** `@Override public ByteList flatten()`
- **Summary:** Returns a list containing all matrix elements in row-major order.
- **Contract:**
  - This is useful for bulk operations or when you need all matrix values as a flat collection.
- **Parameters:**
  - (none)
- **Returns:** a new ByteList containing all elements in row-major order
- **See also:** #horizontalStream()
##### mutateAsFlat(...) -> void
- **Signature:** `@Override public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super byte[], E> action) throws E`
- **Summary:** Flattens all elements of this matrix into a single one-dimensional array, applies the given operation to that flattened array, and then copies the modified elements back into the matrix.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super byte[], E>`) — the operation to apply to the flattened array
- **Throws:**
  - `E` — if the operation throws an exception
- **See also:** Arrays#mutateAsFlat(byte\[\]\[\], Throwables.Consumer)
##### stackVertically(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix stackVertically(final ByteMatrix other) throws IllegalArgumentException`
- **Summary:** Stacks this matrix vertically with another matrix (row-wise concatenation).
- **Contract:**
  - The matrices must have the same number of columns.
- **Parameters:**
  - `other` (`ByteMatrix`) — the matrix to stack below this matrix
- **Returns:** a new ByteMatrix with other appended below this matrix
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , has a different column count, or if the merged row count would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackVertically(IntMatrix)
##### stackHorizontally(...) -> ByteMatrix
- **Signature:** `@Override public ByteMatrix stackHorizontally(final ByteMatrix other) throws IllegalArgumentException`
- **Summary:** Stacks this matrix horizontally with another matrix (column-wise concatenation).
- **Contract:**
  - The matrices must have the same number of rows.
- **Parameters:**
  - `other` (`ByteMatrix`) — the matrix to concatenate to the right of this matrix
- **Returns:** a new ByteMatrix with other appended to the right of this matrix
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , has a different row count, or if the merged column count would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackHorizontally(IntMatrix)
##### add(...) -> ByteMatrix
- **Signature:** `public ByteMatrix add(final ByteMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise addition with another matrix of the same dimensions.
- **Contract:**
  - If the sum exceeds the byte range (-128 to 127), the result will wrap around.
- **Parameters:**
  - `other` (`ByteMatrix`) — the matrix to add to this matrix; must have the same dimensions
- **Returns:** a new ByteMatrix containing the element-wise sum
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or has different dimensions (rows or columns don't match)
- **See also:** #subtract(ByteMatrix)
##### subtract(...) -> ByteMatrix
- **Signature:** `public ByteMatrix subtract(final ByteMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise subtraction with another matrix of the same dimensions.
- **Contract:**
  - If the difference goes below the byte range (-128 to 127), the result will wrap around.
- **Parameters:**
  - `other` (`ByteMatrix`) — the matrix to subtract from this matrix; must have the same dimensions
- **Returns:** a new ByteMatrix containing the element-wise difference
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or has different dimensions (rows or columns don't match)
- **See also:** #add(ByteMatrix)
##### matmul(...) -> ByteMatrix
- **Signature:** `public ByteMatrix matmul(final ByteMatrix other) throws IllegalArgumentException`
- **Summary:** Multiplies this matrix by another matrix (matrix multiplication).
- **Contract:**
  - The number of columns in this matrix must equal the number of rows in the other matrix.
  - If a non-wrapping product is required, widen via {@link #toIntMatrix()} (or {@link #toLongMatrix()} ) and multiply there.
- **Parameters:**
  - `other` (`ByteMatrix`) — the matrix to multiply with; must have row count equal to this matrix's column count
- **Returns:** a new ByteMatrix containing the matrix product with dimensions {@code (this.rowCount x other.columnCount)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , or if {@code this.columnCount != other.rowCount} (incompatible dimensions for multiplication)
##### boxed(...) -> Matrix<Byte>
- **Signature:** `public Matrix<Byte> boxed()`
- **Summary:** Converts this primitive byte matrix to a boxed Byte Matrix.
- **Contract:**
  - <p> This conversion is useful when you need to work with APIs that require object types rather than primitives, or when you need null values in the matrix.
- **Parameters:**
  - (none)
- **Returns:** a new Matrix &lt; Byte &gt; with the same dimensions and values as this matrix
- **See also:** #unbox(Matrix)
##### toIntMatrix(...) -> IntMatrix
- **Signature:** `public IntMatrix toIntMatrix()`
- **Summary:** Converts this ByteMatrix to an IntMatrix by widening each byte value to int.
- **Parameters:**
  - (none)
- **Returns:** a new IntMatrix with the same dimensions and values converted to int
- **See also:** #toLongMatrix(), #toFloatMatrix(), #toDoubleMatrix(), IntMatrix#from(byte\[\]\[\])
##### toLongMatrix(...) -> LongMatrix
- **Signature:** `public LongMatrix toLongMatrix()`
- **Summary:** Converts this ByteMatrix to a LongMatrix by widening each byte value to long.
- **Parameters:**
  - (none)
- **Returns:** a new LongMatrix with the same dimensions and values converted to long
- **See also:** #toIntMatrix(), #toFloatMatrix(), #toDoubleMatrix()
##### toFloatMatrix(...) -> FloatMatrix
- **Signature:** `public FloatMatrix toFloatMatrix()`
- **Summary:** Converts this ByteMatrix to a FloatMatrix by converting each byte value to float.
- **Parameters:**
  - (none)
- **Returns:** a new FloatMatrix with the same dimensions and values converted to float
- **See also:** #toIntMatrix(), #toLongMatrix(), #toDoubleMatrix()
##### toDoubleMatrix(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix toDoubleMatrix()`
- **Summary:** Converts this ByteMatrix to a DoubleMatrix by converting each byte value to double.
- **Parameters:**
  - (none)
- **Returns:** a new DoubleMatrix with the same dimensions and values converted to double
- **See also:** #toIntMatrix(), #toLongMatrix(), #toFloatMatrix()
##### zipWith(...) -> ByteMatrix
- **Signature:** `public <E extends Exception> ByteMatrix zipWith(final ByteMatrix other, final Throwables.ByteBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Applies a binary operation element-wise to this matrix and another matrix of the same shape.
- **Parameters:**
  - `other` (`ByteMatrix`) — the second matrix
  - `zipFunction` (`Throwables.ByteBinaryOperator<E>`) — the binary operation to apply to corresponding elements
- **Returns:** a new {@code ByteMatrix} containing the results
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} has a different shape than this matrix, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception
- **Signature:** `public <E extends Exception> ByteMatrix zipWith(final ByteMatrix other, final ByteMatrix third, final Throwables.ByteTernaryOperator<E> zipFunction) throws E`
- **Summary:** Applies a ternary operation element-wise to this matrix and two other matrices of the same shape.
- **Parameters:**
  - `other` (`ByteMatrix`) — the second matrix
  - `third` (`ByteMatrix`) — the third matrix
  - `zipFunction` (`Throwables.ByteTernaryOperator<E>`) — the ternary operation to apply to corresponding elements
- **Returns:** a new {@code ByteMatrix} containing the results
- **Throws:**
  - `E` — if the zip function throws an exception
##### mainDiagonalStream(...) -> ByteStream
- **Signature:** `@Override public ByteStream mainDiagonalStream()`
- **Summary:** Returns a stream of elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a ByteStream of diagonal elements
##### antiDiagonalStream(...) -> ByteStream
- **Signature:** `@Override public ByteStream antiDiagonalStream()`
- **Summary:** Returns a stream of elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a ByteStream of anti-diagonal elements
##### horizontalStream(...) -> ByteStream
- **Signature:** `@Override public ByteStream horizontalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
- **Parameters:**
  - (none)
- **Returns:** a ByteStream of all matrix elements in row-major order
- **See also:** #verticalStream(), #rowStreams()
- **Signature:** `@Override public ByteStream horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a specific row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to stream (0-based)
- **Returns:** a ByteStream of elements from the specified row
- **Signature:** `@Override public ByteStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of rows in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a ByteStream of elements from the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds
##### verticalStream(...) -> ByteStream
- **Signature:** `@Override @Beta public ByteStream verticalStream()`
- **Summary:** Returns a stream of all elements in column-major order (vertically).
- **Parameters:**
  - (none)
- **Returns:** a ByteStream of all matrix elements in column-major order
- **See also:** #horizontalStream(), #columnStreams()
- **Signature:** `@Override public ByteStream verticalStream(final int columnIndex)`
- **Summary:** Returns a stream of elements from a specific column.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to stream (0-based)
- **Returns:** a ByteStream of elements from the specified column
- **Signature:** `@Override @Beta public ByteStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of columns in column-major order.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a ByteStream of elements from the specified columns
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds
##### rowStreams(...) -> Stream<ByteStream>
- **Signature:** `@Override public Stream<ByteStream> rowStreams()`
- **Summary:** Returns a stream where each element is a ByteStream representing a row of the matrix.
- **Contract:**
  - This is useful for row-wise operations or when you need to apply stream operations to individual rows.
- **Parameters:**
  - (none)
- **Returns:** a Stream of ByteStream, one for each row in the matrix
- **See also:** #columnStreams(), #horizontalStream()
- **Signature:** `@Override public Stream<ByteStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream where each element is a ByteStream representing a row from the specified range.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a Stream of ByteStream, one for each row in the range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds
##### columnStreams(...) -> Stream<ByteStream>
- **Signature:** `@Override @Beta public Stream<ByteStream> columnStreams()`
- **Summary:** Returns a stream where each element is a ByteStream representing a column of the matrix.
- **Contract:**
  - This is useful for column-wise operations or when you need to apply stream operations to individual columns.
- **Parameters:**
  - (none)
- **Returns:** a Stream of ByteStream, one for each column in the matrix
- **See also:** #rowStreams(), #verticalStream()
- **Signature:** `@Override @Beta public Stream<ByteStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream where each element is a ByteStream representing a column from the specified range.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a Stream of ByteStream, one for each column in the range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds
##### forEach(...) -> void
- **Signature:** `public <E extends Exception> void forEach(final Throwables.ByteConsumer<E> action) throws E`
- **Summary:** Performs the specified action for each element in this matrix.
- **Parameters:**
  - `action` (`Throwables.ByteConsumer<E>`) — the consumer to apply to each element; must not be {@code null}
- **Throws:**
  - `E` — if the action throws an exception
- **Signature:** `public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.ByteConsumer<E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element in a rectangular sub-region of this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.ByteConsumer<E>`) — the consumer to apply to each element in the region
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds or fromIndex &gt; toIndex
  - `E` — if the action throws an exception
##### println(...) -> String
- **Signature:** `@Override public String println()`
- **Summary:** Prints this matrix to standard output and returns the formatted string.
- **Contract:**
  - If the matrix is empty, {@code \[\]} is printed.
- **Parameters:**
  - (none)
- **Returns:** the formatted string representation of the matrix
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this matrix based on its contents.
- **Contract:**
  - <p> This implementation is consistent with the {@link #equals(Object)} method: if two matrices are equal according to {@code equals()} , they will have the same hash code.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this matrix based on its contents
- **See also:** #equals(Object)
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this matrix to the specified object for equality.
- **Contract:**
  - Two ByteMatrix objects are considered equal if and only if: <ul> <li> They have the same number of rows </li> <li> They have the same number of columns </li> <li> All corresponding elements are equal </li> </ul> <p> This method performs a deep comparison of all matrix elements.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with this matrix
- **Returns:** {@code true} if the objects are equal ByteMatrix instances with identical contents, {@code false} otherwise
- **See also:** #hashCode()
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this matrix in a compact two-dimensional array format.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this matrix in two-dimensional array format
- **See also:** #println()

### Class CharMatrix (com.landawn.abacus.matrix.CharMatrix)
Matrix implementation backed by a rectangular {@code char\[\]\[\]} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### empty(...) -> CharMatrix
- **Signature:** `public static CharMatrix empty()`
- **Summary:** Creates an empty matrix with zero rows and zero columns.
- **Parameters:**
  - (none)
- **Returns:** an empty char matrix
##### of(...) -> CharMatrix
- **Signature:** `public static CharMatrix of(final char[]... a)`
- **Summary:** Creates a CharMatrix from a two-dimensional char array.
- **Parameters:**
  - `a` (`char[][]`) — the two-dimensional char array to create the matrix from, or null/empty for an empty matrix
- **Returns:** a new CharMatrix containing the provided data, or an empty CharMatrix if input is null or empty
##### random(...) -> CharMatrix
- **Signature:** `public static CharMatrix random(final int length)`
- **Summary:** Creates a new {@code 1 x length} matrix filled with random char values drawn uniformly from the full unsigned 16-bit range {@code \[0, 65535\]} .
- **Parameters:**
  - `length` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new CharMatrix of dimensions 1 x length filled with random values
- **Signature:** `public static CharMatrix random(final int rowCount, final int columnCount)`
- **Summary:** Creates a new matrix of the specified dimensions filled with random char values drawn uniformly from the full unsigned 16-bit range {@code \[0, 65535\]} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new CharMatrix of dimensions rowCount x columnCount filled with random values
##### repeat(...) -> CharMatrix
- **Signature:** `public static CharMatrix repeat(final int rowCount, final int columnCount, final char element)`
- **Summary:** Creates a new matrix of the specified dimensions where every element is the provided {@code element} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
  - `element` (`char`) — the char value to fill the matrix with
- **Returns:** a new CharMatrix of dimensions rowCount x columnCount filled with the specified element
##### range(...) -> CharMatrix
- **Signature:** `public static CharMatrix range(final char startInclusive, final char endExclusive)`
- **Summary:** Creates a single-row CharMatrix containing a range of char values.
- **Parameters:**
  - `startInclusive` (`char`) — the starting char value (inclusive)
  - `endExclusive` (`char`) — the ending char value (exclusive)
- **Returns:** a CharMatrix with one row containing the range of values
- **Signature:** `public static CharMatrix range(final char startInclusive, final char endExclusive, final int step)`
- **Summary:** Creates a single-row CharMatrix containing a range of char values with a step.
- **Parameters:**
  - `startInclusive` (`char`) — the starting char value (inclusive)
  - `endExclusive` (`char`) — the ending char value (exclusive)
  - `step` (`int`) — the step size (must not be zero; can be positive or negative)
- **Returns:** a new 1×n CharMatrix with values incremented by the step size
##### rangeClosed(...) -> CharMatrix
- **Signature:** `public static CharMatrix rangeClosed(final char startInclusive, final char endInclusive)`
- **Summary:** Creates a single-row CharMatrix containing a closed range of char values.
- **Parameters:**
  - `startInclusive` (`char`) — the starting char value (inclusive)
  - `endInclusive` (`char`) — the ending char value (inclusive)
- **Returns:** a CharMatrix with one row containing the range of values
- **Signature:** `public static CharMatrix rangeClosed(final char startInclusive, final char endInclusive, final int step)`
- **Summary:** Creates a single-row CharMatrix containing a closed range of char values with a step.
- **Parameters:**
  - `startInclusive` (`char`) — the starting char value (inclusive)
  - `endInclusive` (`char`) — the ending char value (inclusive, if reachable by stepping)
  - `step` (`int`) — the step size (must not be zero; can be positive or negative)
- **Returns:** a new 1×n CharMatrix with values incremented by the step size
##### mainDiagonal(...) -> CharMatrix
- **Signature:** `public static CharMatrix mainDiagonal(final char[] mainDiagonal)`
- **Summary:** Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
- **Parameters:**
  - `mainDiagonal` (`char[]`) — the array of main diagonal elements
- **Returns:** a square matrix with the specified main diagonal (n×n where n = diagonal length)
##### antiDiagonal(...) -> CharMatrix
- **Signature:** `public static CharMatrix antiDiagonal(final char[] antiDiagonal)`
- **Summary:** Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
- **Parameters:**
  - `antiDiagonal` (`char[]`) — the array of anti-diagonal elements
- **Returns:** a square matrix with the specified anti-diagonal (n×n where n = diagonal length)
##### diagonals(...) -> CharMatrix
- **Signature:** `public static CharMatrix diagonals(final char[] mainDiagonal, final char[] antiDiagonal) throws IllegalArgumentException`
- **Summary:** Creates a square matrix from the specified main diagonal and anti-diagonal elements.
- **Contract:**
  - If both arrays are provided, they must have the same length.
  - When both diagonals are provided and they overlap (at the center element of odd-sized matrices), the main diagonal value takes precedence.
- **Parameters:**
  - `mainDiagonal` (`char[]`) — the array of main diagonal elements (can be null or empty)
  - `antiDiagonal` (`char[]`) — the array of anti-diagonal elements (can be null or empty)
- **Returns:** a square matrix with the specified diagonals, or an empty matrix if both inputs are null or empty
- **Throws:**
  - `java.lang.IllegalArgumentException` — if both arrays are non-empty and have different lengths
##### unbox(...) -> CharMatrix
- **Signature:** `public static CharMatrix unbox(final Matrix<Character> x)`
- **Summary:** Converts a boxed Character Matrix to a primitive CharMatrix.
- **Contract:**
  - This conversion improves memory efficiency and performance when working with large matrices.
- **Parameters:**
  - `x` (`Matrix<Character>`) — the boxed Character Matrix to convert; must not be {@code null}
- **Returns:** a new CharMatrix with primitive char values
- **See also:** #boxed()

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public CharMatrix(final char[][] a)`
- **Summary:** Constructs a {@code CharMatrix} backed by the supplied two-dimensional array.
- **Contract:**
  - <p> If {@code a} is {@code null} , this creates an empty {@code 0x0} matrix.
  - Call {@link #copy()} if you need an independently owned matrix.
- **Parameters:**
  - `a` (`char[][]`) — the two-dimensional char array to wrap, or {@code null} for an empty matrix
##### get(...) -> char
- **Signature:** `public char get(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** the element at position (rowIndex, columnIndex)
- **Signature:** `public char get(final Point point)`
- **Summary:** Returns the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
- **Returns:** the char element at the specified point
- **See also:** #get(int, int)
##### set(...) -> void
- **Signature:** `public void set(final int rowIndex, final int columnIndex, final char value)`
- **Summary:** Sets the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
  - `value` (`char`) — the value to set
- **Signature:** `public void set(final Point point, final char value)`
- **Summary:** Sets the element at the specified point to the given value.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
  - `value` (`char`) — the new char value to set at the specified point
- **See also:** #set(int, int, char)
##### valueAbove(...) -> OptionalChar
- **Signature:** `public OptionalChar valueAbove(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly above the specified position, if it exists.
- **Contract:**
  - Returns the element directly above the specified position, if it exists.
  - This method provides safe access to the element directly above the given position without throwing an exception when at the top edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalChar containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
##### valueBelow(...) -> OptionalChar
- **Signature:** `public OptionalChar valueBelow(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly below the specified position, if it exists.
- **Contract:**
  - Returns the element directly below the specified position, if it exists.
  - This method provides safe access to the element directly below the given position without throwing an exception when at the bottom edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalChar containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
##### valueLeft(...) -> OptionalChar
- **Signature:** `public OptionalChar valueLeft(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the left of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the left of the specified position, if it exists.
  - This method provides safe access to the element directly to the left of the given position without throwing an exception when at the leftmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalChar containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
##### valueRight(...) -> OptionalChar
- **Signature:** `public OptionalChar valueRight(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the right of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the right of the specified position, if it exists.
  - This method provides safe access to the element directly to the right of the given position without throwing an exception when at the rightmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalChar containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
##### rowView(...) -> char\[\]
- **Signature:** `@Override public char[] rowView(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as a char array.
- **Contract:**
  - If you need an independent copy, use {@code Arrays.copyOf(matrix.rowView(i), matrix.columnCount())} .
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the specified row array (direct reference to internal storage)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### rowCopy(...) -> char\[\]
- **Signature:** `@Override public char[] rowCopy(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new char array containing the values from the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### columnCopy(...) -> char\[\]
- **Signature:** `@Override public char[] columnCopy(final int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a copy of the specified column as a new char array.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new array containing the values from the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex &lt; 0 or columnIndex &gt; = columnCount
##### setRow(...) -> void
- **Signature:** `public void setRow(final int rowIndex, final char[] row) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified row by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of columns in the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to set (0-based)
  - `row` (`char[]`) — the array of values to copy into the row; must have length equal to the number of columns
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex is out of bounds or row length does not match column count
##### setColumn(...) -> void
- **Signature:** `public void setColumn(final int columnIndex, final char[] column) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified column by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of rows in the matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to set (0-based)
  - `column` (`char[]`) — the array of values to copy into the column; must have length equal to the number of rows
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex is out of bounds or column length does not match row count
##### updateRow(...) -> void
- **Signature:** `public <E extends Exception> void updateRow(final int rowIndex, final Throwables.CharUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified row by applying the given operator to each element.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to update (0-based)
  - `operator` (`Throwables.CharUnaryOperator<E>`) — the operator to apply to each element in the row; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### updateColumn(...) -> void
- **Signature:** `public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.CharUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified column in-place by applying the given operator to each element.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to update (0-based)
  - `operator` (`Throwables.CharUnaryOperator<E>`) — the operator to apply to each element in the column; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### getMainDiagonal(...) -> char\[\]
- **Signature:** `@Override public char[] getMainDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the main diagonal elements (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new char array containing the main diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setMainDiagonal(...) -> void
- **Signature:** `@Override public void setMainDiagonal(final char[] mainDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `mainDiagonal` (`char[]`) — the new values for the main diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if mainDiagonal array length does not equal rowCount
##### updateMainDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateMainDiagonal(final Throwables.CharUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the main diagonal (upper-left to lower-right) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.CharUnaryOperator<E>`) — the operator to apply to each diagonal element
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### getAntiDiagonal(...) -> char\[\]
- **Signature:** `@Override public char[] getAntiDiagonal() throws IllegalStateException`
- **Summary:** Returns the elements on the anti-diagonal from upper-right to lower-left.
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new char array containing the anti-diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setAntiDiagonal(...) -> void
- **Signature:** `@Override public void setAntiDiagonal(final char[] antiDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `antiDiagonal` (`char[]`) — the new values for the anti-diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if antiDiagonal array length does not equal rowCount
##### updateAntiDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateAntiDiagonal(final Throwables.CharUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the elements on the anti-diagonal (upper-right to lower-left) using the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.CharUnaryOperator<E>`) — the operator to apply to each anti-diagonal element
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `E` — if the operator throws an exception
##### updateAll(...) -> void
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.CharUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the matrix using the specified operator in-place.
- **Parameters:**
  - `operator` (`Throwables.CharUnaryOperator<E>`) — the operator to apply to each element
- **Throws:**
  - `E` — if the operator throws an exception
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Character, E> mapper) throws E`
- **Summary:** Updates all elements in the matrix based on their position using a position-aware mapper.
- **Contract:**
  - This is useful when the new value depends on the element's location in the matrix.
- **Parameters:**
  - `mapper` (`Throwables.IntBiFunction<? extends Character, E>`) — the function that takes (rowIndex, columnIndex) and returns the new char value
- **Throws:**
  - `E` — if the mapper throws an exception
##### replaceIf(...) -> void
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.CharPredicate<E> predicate, final char newValue) throws E`
- **Summary:** Replaces all elements that match the predicate with the specified value.
- **Parameters:**
  - `predicate` (`Throwables.CharPredicate<E>`) — the predicate to test each element
  - `newValue` (`char`) — the value to replace matching elements with
- **Throws:**
  - `E` — if the predicate throws an exception
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final char newValue) throws E`
- **Summary:** Replaces all elements at positions that match the position-based predicate with the specified value.
- **Contract:**
  - <p> The predicate receives the row and column indices for each position and determines whether the element at that position should be replaced.
- **Parameters:**
  - `predicate` (`Throwables.IntBiPredicate<E>`) — the predicate that takes (rowIndex, columnIndex) and returns true for positions to replace
  - `newValue` (`char`) — the value to replace at matching positions
- **Throws:**
  - `E` — if the predicate throws an exception
##### map(...) -> CharMatrix
- **Signature:** `public <E extends Exception> CharMatrix map(final Throwables.CharUnaryOperator<E> mapper) throws E`
- **Summary:** Creates a new CharMatrix by applying a transformation function to each element.
- **Parameters:**
  - `mapper` (`Throwables.CharUnaryOperator<E>`) — the function to apply to each element; receives the current element value and returns the transformed value
- **Returns:** a new CharMatrix with transformed values
- **Throws:**
  - `E` — if the function throws an exception
- **See also:** #updateAll(Throwables.CharUnaryOperator)
##### mapToObj(...) -> Matrix<R>
- **Signature:** `public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.CharFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E`
- **Summary:** Creates a new object Matrix by applying the specified function to each char element.
- **Parameters:**
  - `mapper` (`Throwables.CharFunction<? extends R, E>`) — the mapping function that converts each char to an object of type R
  - `targetElementType` (`Class<R>`) — the class object representing the target element type (required for array creation; must not be {@code null} )
- **Returns:** a new Matrix &lt; R &gt; with the mapped object values
- **Throws:**
  - `E` — if the function throws an exception
##### fill(...) -> void
- **Signature:** `public void fill(final char value)`
- **Summary:** Fills all elements in the matrix with the specified value.
- **Parameters:**
  - `value` (`char`) — the value to fill the matrix with
- **Signature:** `public void fill(final char[][] source)`
- **Summary:** Fills the matrix with values from the specified two-dimensional array in-place, starting from position (0,0).
- **Contract:**
  - If the source array is smaller than the matrix, only the overlapping region is filled.
  - If the source array is larger, only the portion that fits is copied.
- **Parameters:**
  - `source` (`char[][]`) — the source array to copy values from (may be smaller or larger than the matrix)
- **Signature:** `public void fill(final int destRowIndex, final int destColumnIndex, final char[][] source) throws IllegalArgumentException`
- **Summary:** Fills a portion of the matrix with values from the specified two-dimensional array in-place, starting from a specified position.
- **Contract:**
  - If the source array extends beyond the matrix bounds from the starting position, only the portion that fits is copied.
- **Parameters:**
  - `destRowIndex` (`int`) — the target row index in this matrix (0-based); must satisfy {@code 0 <= destRowIndex <= rowCount}
  - `destColumnIndex` (`int`) — the target column index in this matrix (0-based); must satisfy {@code 0 <= destColumnIndex <= columnCount}
  - `source` (`char[][]`) — the source array to copy values from; must not be {@code null} (individual rows may be {@code null} and are skipped)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code source} is {@code null} or the target indices are out of range
##### copy(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a copy of this matrix
- **Signature:** `@Override public CharMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a row range from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new CharMatrix containing the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
- **Signature:** `@Override public CharMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a rectangular region from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new CharMatrix containing the specified region
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, fromRowIndex &gt; toRowIndex, fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
##### resize(...) -> CharMatrix
- **Signature:** `public CharMatrix resize(final int newRowCount, final int newColumnCount)`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code ' '} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
- **Returns:** a new CharMatrix with the specified dimensions
- **See also:** #resize(int, int, char), #extend(int, int, int, int)
- **Signature:** `public CharMatrix resize(final int newRowCount, final int newColumnCount, final char defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded.
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code defaultValue} .
  - Use {@code extend} when the entire original content must be preserved.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code CharMatrix matrix = CharMatrix.of(new char\[\]\[\] {{'a', 'b', 'c'}, {'d', 'e', 'f'}, {'g', 'h', 'i'}}); // Grow: fill new cells with 'x' CharMatrix grown = matrix.resize(4, 4, 'x'); // Result: \[\['a', 'b', 'c', 'x'\], // \['d', 'e', 'f', 'x'\], // \['g', 'h', 'i', 'x'\], // \['x', 'x', 'x', 'x'\]\] // Truncate: defaultValue is ignored when shrinking CharMatrix truncated = matrix.resize(2, 2, 'x'); // Result: \[\['a', 'b'\], // \['d', 'e'\]\] } </pre>
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
  - `defaultValue` (`char`) — the value used to fill cells that are added when a dimension grows; ignored when a dimension shrinks
- **Returns:** a new CharMatrix with the specified dimensions
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code newRowCount} or {@code newColumnCount} is negative, or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
- **See also:** #resize(int, int), #extend(int, int, int, int, char)
##### extend(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight)`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
- **Returns:** a new CharMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **See also:** #extend(int, int, int, int, char), #resize(int, int)
- **Signature:** `public CharMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final char defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
  - `defaultValue` (`char`) — the value to fill all new padding cells with
- **Returns:** a new CharMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any padding parameter is negative, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** #extend(int, int, int, int), #resize(int, int, char)
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `@Override public void flipHorizontallyInPlace()`
- **Summary:** Reverses the order of elements in each row horizontally (in-place).
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally(),for a non-mutating version
##### flipVerticallyInPlace(...) -> void
- **Signature:** `@Override public void flipVerticallyInPlace()`
- **Summary:** Reverses the order of rows in the matrix (vertical flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipVertically(),for a non-mutating version
##### flipHorizontally(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix flipHorizontally()`
- **Summary:** Creates a new matrix that is horizontally flipped (each row reversed).
- **Parameters:**
  - (none)
- **Returns:** a new CharMatrix with each row reversed
- **See also:** #flipHorizontallyInPlace(),for an in-place version, #flipVertically(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### flipVertically(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix flipVertically()`
- **Summary:** Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a vertical flip of this matrix (rows in reversed order)
- **See also:** #flipVerticallyInPlace(),for an in-place version, #flipHorizontally(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### rotate90(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 90 degrees clockwise
##### rotate180(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 180 degrees clockwise
##### rotate270(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 270 degrees clockwise
##### transpose(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix transpose()`
- **Summary:** Creates the transpose of this matrix by swapping rows and columns.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is the transpose of this matrix with dimensions columnCount × rowCount
##### reshape(...) -> CharMatrix
- **Signature:** `@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG") @Override public CharMatrix reshape(final int newRowCount, final int newColumnCount)`
- **Summary:** Reshapes the matrix to the specified dimensions.
- **Contract:**
  - The new shape must have at least as many total elements as the original ( {@code newRowCount * newColumnCount >= elementCount()} ).
  - If the new shape requires more elements than available in the source matrix, the remaining positions are filled with default char values (' ').
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix (must be &gt; = 0)
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix (must be &gt; = 0)
- **Returns:** a new CharMatrix with the specified dimensions
##### repeatElements(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats each element in both row and column directions.
- **Parameters:**
  - `rowRepeats` (`int`) — the number of times to repeat each element in the row direction; must be {@code > 0}
  - `columnRepeats` (`int`) — the number of times to repeat each element in the column direction; must be {@code > 0}
- **Returns:** a new CharMatrix with repeated elements
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is not positive, or the resulting dimensions would exceed {@link Integer#MAX_VALUE}
- **See also:** IntMatrix#repeatElements(int, int)
##### repeatMatrix(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats the entire matrix in both row and column directions.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat the matrix vertically; must be {@code > 0}
  - `columnRepeats` (`int`) — number of times to repeat the matrix horizontally; must be {@code > 0}
- **Returns:** a new CharMatrix with the repeated pattern
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is not positive, or the resulting dimensions would exceed {@link Integer#MAX_VALUE}
- **See also:** IntMatrix#repeatMatrix(int, int)
##### flatten(...) -> CharList
- **Signature:** `@Override public CharList flatten()`
- **Summary:** Returns a CharList containing all matrix elements in row-major order.
- **Parameters:**
  - (none)
- **Returns:** a new CharList containing all elements in row-major order
##### mutateAsFlat(...) -> void
- **Signature:** `@Override public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super char[], E> action) throws E`
- **Summary:** Flattens all elements of this matrix into a single one-dimensional array, applies the given operation to that flattened array, and then copies the modified elements back into the matrix.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super char[], E>`) — the operation to apply to the flattened array
- **Throws:**
  - `E` — if the operation throws an exception
- **See also:** Arrays#mutateAsFlat(char\[\]\[\], Throwables.Consumer)
##### stackVertically(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix stackVertically(final CharMatrix other) throws IllegalArgumentException`
- **Summary:** Vertically stacks this matrix on top of another matrix.
- **Contract:**
  - Both matrices must have the same number of columns.
- **Parameters:**
  - `other` (`CharMatrix`) — the matrix to stack below this matrix; must not be {@code null}
- **Returns:** a new CharMatrix with other appended below this matrix
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , the matrices have different column counts, or the merged row count would exceed {@link Integer#MAX_VALUE}
- **See also:** IntMatrix#stackVertically(IntMatrix)
##### stackHorizontally(...) -> CharMatrix
- **Signature:** `@Override public CharMatrix stackHorizontally(final CharMatrix other) throws IllegalArgumentException`
- **Summary:** Horizontally stacks this matrix to the left of another matrix.
- **Contract:**
  - Both matrices must have the same number of rows.
- **Parameters:**
  - `other` (`CharMatrix`) — the matrix to stack to the right of this matrix; must not be {@code null}
- **Returns:** a new CharMatrix with other appended to the right of this matrix
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , the matrices have different row counts, or the merged column count would exceed {@link Integer#MAX_VALUE}
- **See also:** IntMatrix#stackHorizontally(IntMatrix)
##### add(...) -> CharMatrix
- **Signature:** `public CharMatrix add(final CharMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise addition with another matrix.
- **Contract:**
  - Both matrices must have the same dimensions.
- **Parameters:**
  - `other` (`CharMatrix`) — the matrix to add to this matrix; must not be {@code null} and must have the same shape
- **Returns:** a new CharMatrix containing the element-wise sum
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or has different dimensions
##### subtract(...) -> CharMatrix
- **Signature:** `public CharMatrix subtract(final CharMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise subtraction of another matrix from this matrix.
- **Contract:**
  - Both matrices must have the same dimensions.
- **Parameters:**
  - `other` (`CharMatrix`) — the matrix to subtract from this matrix; must not be {@code null} and must have the same shape
- **Returns:** a new CharMatrix containing the element-wise difference
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or has different dimensions
##### matmul(...) -> CharMatrix
- **Signature:** `public CharMatrix matmul(final CharMatrix other) throws IllegalArgumentException`
- **Summary:** Performs matrix multiplication with another matrix.
- **Contract:**
  - The number of columns in this matrix must equal the number of rows in the other matrix.
- **Parameters:**
  - `other` (`CharMatrix`) — the matrix to multiply with this matrix; must not be {@code null}
- **Returns:** a new CharMatrix containing the matrix product
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or {@code this.columnCount != other.rowCount}
##### boxed(...) -> Matrix<Character>
- **Signature:** `public Matrix<Character> boxed()`
- **Summary:** Converts this CharMatrix to a Matrix of Character objects.
- **Contract:**
  - This is useful when you need to work with object-based operations or APIs that require Character objects instead of primitives.
- **Parameters:**
  - (none)
- **Returns:** a new Matrix containing Character objects with the same values and dimensions
- **See also:** #unbox(Matrix)
##### toIntMatrix(...) -> IntMatrix
- **Signature:** `public IntMatrix toIntMatrix()`
- **Summary:** Converts this CharMatrix to an IntMatrix.
- **Parameters:**
  - (none)
- **Returns:** a new IntMatrix with the same dimensions containing the int values of the characters
##### toLongMatrix(...) -> LongMatrix
- **Signature:** `public LongMatrix toLongMatrix()`
- **Summary:** Converts this CharMatrix to a LongMatrix.
- **Parameters:**
  - (none)
- **Returns:** a new LongMatrix with the same dimensions containing the long values of the characters
##### toFloatMatrix(...) -> FloatMatrix
- **Signature:** `public FloatMatrix toFloatMatrix()`
- **Summary:** Converts this CharMatrix to a FloatMatrix.
- **Parameters:**
  - (none)
- **Returns:** a new FloatMatrix with the same dimensions containing the float values of the characters
##### toDoubleMatrix(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix toDoubleMatrix()`
- **Summary:** Converts this CharMatrix to a DoubleMatrix.
- **Parameters:**
  - (none)
- **Returns:** a new DoubleMatrix with the same dimensions containing the double values of the characters
##### zipWith(...) -> CharMatrix
- **Signature:** `public <E extends Exception> CharMatrix zipWith(final CharMatrix other, final Throwables.CharBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Applies a binary operation element-wise to this matrix and another matrix.
- **Contract:**
  - Both matrices must have the same dimensions.
- **Parameters:**
  - `other` (`CharMatrix`) — the second matrix to zip with this matrix
  - `zipFunction` (`Throwables.CharBinaryOperator<E>`) — the binary operation to apply to corresponding elements
- **Returns:** a new CharMatrix containing the results of the zip operation
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices have different dimensions, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception
- **Signature:** `public <E extends Exception> CharMatrix zipWith(final CharMatrix other, final CharMatrix third, final Throwables.CharTernaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Applies a ternary operation element-wise to this matrix and two other matrices.
- **Contract:**
  - All three matrices must have the same dimensions.
- **Parameters:**
  - `other` (`CharMatrix`) — the second matrix to zip with
  - `third` (`CharMatrix`) — the third matrix to zip with
  - `zipFunction` (`Throwables.CharTernaryOperator<E>`) — the ternary operation to apply to corresponding elements
- **Returns:** a new CharMatrix containing the results of the zip operation
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any of the matrices have different dimensions, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception
##### mainDiagonalStream(...) -> CharStream
- **Signature:** `@Override public CharStream mainDiagonalStream()`
- **Summary:** Returns a stream of elements on the diagonal from upper-left to lower-right.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a CharStream containing the diagonal elements from top-left to bottom-right
##### antiDiagonalStream(...) -> CharStream
- **Signature:** `@Override public CharStream antiDiagonalStream()`
- **Summary:** Returns a stream of elements on the anti-diagonal from upper-right to lower-left.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a CharStream containing the diagonal elements from top-right to bottom-left
##### horizontalStream(...) -> CharStream
- **Signature:** `@Override public CharStream horizontalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
- **Parameters:**
  - (none)
- **Returns:** a CharStream containing all matrix elements traversed horizontally (left to right, top to bottom)
- **Signature:** `@Override public CharStream horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a specific row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to stream (0-based)
- **Returns:** a CharStream containing all elements from the specified row
- **Signature:** `@Override public CharStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a CharStream of elements from a range of rows, traversed horizontally.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a CharStream of elements from the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
##### verticalStream(...) -> CharStream
- **Signature:** `@Override @Beta public CharStream verticalStream()`
- **Summary:** Returns a stream of all elements in the matrix, traversed vertically (column by column).
- **Parameters:**
  - (none)
- **Returns:** a CharStream containing all matrix elements in column-major order
- **Signature:** `@Override public CharStream verticalStream(final int columnIndex)`
- **Summary:** Returns a stream of elements from a specific column.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to stream (0-based)
- **Returns:** a CharStream containing all elements from the specified column
- **Signature:** `@Override @Beta public CharStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of columns, traversed vertically.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a CharStream containing elements from the specified column range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds or fromColumnIndex &gt; toColumnIndex
##### rowStreams(...) -> Stream<CharStream>
- **Signature:** `@Override public Stream<CharStream> rowStreams()`
- **Summary:** Returns a stream of CharStreams, where each CharStream represents a row in the matrix.
- **Parameters:**
  - (none)
- **Returns:** a Stream of CharStreams, one for each row in the matrix
- **Signature:** `@Override public Stream<CharStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of CharStreams for a range of rows.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a Stream of CharStreams for the specified row range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds or fromRowIndex &gt; toRowIndex
##### columnStreams(...) -> Stream<CharStream>
- **Signature:** `@Override @Beta public Stream<CharStream> columnStreams()`
- **Summary:** Returns a stream of CharStreams, where each CharStream represents a column in the matrix.
- **Parameters:**
  - (none)
- **Returns:** a Stream of CharStreams, one for each column in the matrix
- **Signature:** `@Override @Beta public Stream<CharStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of CharStreams for a range of columns.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a Stream of CharStreams for the specified column range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds or fromColumnIndex &gt; toColumnIndex
##### forEach(...) -> void
- **Signature:** `public <E extends Exception> void forEach(final Throwables.CharConsumer<E> action) throws E`
- **Summary:** Performs the specified action for each element in this matrix.
- **Parameters:**
  - `action` (`Throwables.CharConsumer<E>`) — the action to be performed on each element
- **Throws:**
  - `E` — if the action throws an exception
- **Signature:** `public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.CharConsumer<E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element in a sub-region of this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.CharConsumer<E>`) — the action to be performed on each element in the sub-region
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds or fromIndex &gt; toIndex
  - `E` — if the action throws an exception
##### println(...) -> String
- **Signature:** `@Override public String println()`
- **Summary:** Prints this matrix to standard output and returns the formatted string.
- **Contract:**
  - If the matrix is empty, {@code \[\]} is printed.
- **Parameters:**
  - (none)
- **Returns:** the formatted string representation of the matrix
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this matrix.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this matrix
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this matrix to the specified object for equality.
- **Contract:**
  - Returns {@code true} if the given object is also a CharMatrix with the same dimensions and all corresponding elements are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this matrix

### Class DoubleMatrix (com.landawn.abacus.matrix.DoubleMatrix)
Matrix implementation backed by a rectangular {@code double\[\]\[\]} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### empty(...) -> DoubleMatrix
- **Signature:** `public static DoubleMatrix empty()`
- **Summary:** Creates an empty matrix with zero rows and zero columns.
- **Parameters:**
  - (none)
- **Returns:** an empty double matrix
##### of(...) -> DoubleMatrix
- **Signature:** `public static DoubleMatrix of(final double[]... a)`
- **Summary:** Creates a DoubleMatrix from a two-dimensional double array.
- **Parameters:**
  - `a` (`double[][]`) — the two-dimensional double array to create the matrix from, or null/empty for an empty matrix
- **Returns:** a new DoubleMatrix containing the provided data, or an empty DoubleMatrix if input is null or empty
##### from(...) -> DoubleMatrix
- **Signature:** `public static DoubleMatrix from(final int[]... a)`
- **Summary:** Creates a DoubleMatrix from a two-dimensional int array by converting int values to double.
- **Contract:**
  - <p> All rows must have the same length as the first row (rectangular array required).
- **Parameters:**
  - `a` (`int[][]`) — the two-dimensional int array to convert to a double matrix, or {@code null} /empty for an empty matrix
- **Returns:** a new DoubleMatrix with converted values, or an empty DoubleMatrix if input is {@code null} or empty
- **Signature:** `public static DoubleMatrix from(final long[]... a)`
- **Summary:** Creates a DoubleMatrix from a two-dimensional long array by converting long values to double.
- **Contract:**
  - <p> All rows must have the same length as the first row (rectangular array required).
  - </p> <p> <b> Note: </b> Long values with more than 53 significant bits may lose precision when converted to double, since double has a 52-bit mantissa.
- **Parameters:**
  - `a` (`long[][]`) — the two-dimensional long array to convert to a double matrix, or {@code null} /empty for an empty matrix
- **Returns:** a new DoubleMatrix with converted values, or an empty DoubleMatrix if input is {@code null} or empty
- **Signature:** `public static DoubleMatrix from(final float[]... a)`
- **Summary:** Creates a DoubleMatrix from a two-dimensional float array by widening float values to double.
- **Contract:**
  - <p> All rows must have the same length as the first row (rectangular array required).
- **Parameters:**
  - `a` (`float[][]`) — the two-dimensional float array to convert to a double matrix, or {@code null} /empty for an empty matrix
- **Returns:** a new DoubleMatrix with converted values, or an empty DoubleMatrix if input is {@code null} or empty
##### random(...) -> DoubleMatrix
- **Signature:** `public static DoubleMatrix random(final int length)`
- **Summary:** Creates a new {@code 1 x length} matrix filled with random double values uniformly distributed in {@code \[0.0, 1.0)} (as produced by {@link java.util.Random#nextDouble()} ).
- **Parameters:**
  - `length` (`int`) — the number of columns in the new matrix
- **Returns:** a new DoubleMatrix of dimensions {@code 1 x length} filled with random values
- **Signature:** `public static DoubleMatrix random(final int rowCount, final int columnCount)`
- **Summary:** Creates a new matrix of the specified dimensions filled with random double values uniformly distributed in {@code \[0.0, 1.0)} (as produced by {@link java.util.Random#nextDouble()} ).
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix
  - `columnCount` (`int`) — the number of columns in the new matrix
- **Returns:** a new DoubleMatrix of dimensions {@code rowCount x columnCount} filled with random values
##### repeat(...) -> DoubleMatrix
- **Signature:** `public static DoubleMatrix repeat(final int rowCount, final int columnCount, final double element)`
- **Summary:** Creates a new matrix of the specified dimensions where every element is the provided {@code element} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix
  - `columnCount` (`int`) — the number of columns in the new matrix
  - `element` (`double`) — the double value to fill the matrix with (may be {@code NaN} , {@code +/-Infinity} , or any other {@code double} value)
- **Returns:** a new DoubleMatrix of dimensions {@code rowCount x columnCount} filled with the specified element
##### mainDiagonal(...) -> DoubleMatrix
- **Signature:** `public static DoubleMatrix mainDiagonal(final double[] mainDiagonal)`
- **Summary:** Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
- **Parameters:**
  - `mainDiagonal` (`double[]`) — the array of main diagonal elements, or null/empty for an empty matrix
- **Returns:** a square matrix with the specified main diagonal, or an empty matrix if input is null or empty
##### antiDiagonal(...) -> DoubleMatrix
- **Signature:** `public static DoubleMatrix antiDiagonal(final double[] antiDiagonal)`
- **Summary:** Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
- **Parameters:**
  - `antiDiagonal` (`double[]`) — the array of anti-diagonal elements, or null/empty for an empty matrix
- **Returns:** a square matrix with the specified anti-diagonal, or an empty matrix if input is null or empty
##### diagonals(...) -> DoubleMatrix
- **Signature:** `public static DoubleMatrix diagonals(final double[] mainDiagonal, final double[] antiDiagonal) throws IllegalArgumentException`
- **Summary:** Creates a square matrix from the specified main diagonal and anti-diagonal elements.
- **Contract:**
  - If both arrays are provided, they must have the same length.
  - If both diagonals share a position (which happens for odd-sized matrices at the center element), the main diagonal value takes precedence.
- **Parameters:**
  - `mainDiagonal` (`double[]`) — the array of main diagonal elements (can be null or empty)
  - `antiDiagonal` (`double[]`) — the array of anti-diagonal elements (can be null or empty)
- **Returns:** a square matrix with the specified diagonals, or an empty matrix if both inputs are null or empty
- **Throws:**
  - `java.lang.IllegalArgumentException` — if both arrays are non-empty and have different lengths
##### unbox(...) -> DoubleMatrix
- **Signature:** `public static DoubleMatrix unbox(final Matrix<Double> x)`
- **Summary:** Converts a boxed Double matrix to a primitive DoubleMatrix.
- **Parameters:**
  - `x` (`Matrix<Double>`) — the boxed Double matrix to convert; must not be null
- **Returns:** a new DoubleMatrix with unboxed values (nulls become 0.0)
- **See also:** #boxed()

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public DoubleMatrix(final double[][] a)`
- **Summary:** Constructs a {@code DoubleMatrix} backed by the supplied two-dimensional array.
- **Contract:**
  - <p> If {@code a} is {@code null} , this creates an empty {@code 0x0} matrix.
  - Call {@link #copy()} if you need an independently owned matrix.
- **Parameters:**
  - `a` (`double[][]`) — the two-dimensional double array to wrap, or {@code null} for an empty matrix
##### get(...) -> double
- **Signature:** `public double get(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** the element at position (rowIndex, columnIndex)
- **Signature:** `public double get(final Point point)`
- **Summary:** Returns the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
- **Returns:** the double element at the specified point
- **See also:** #get(int, int)
##### set(...) -> void
- **Signature:** `public void set(final int rowIndex, final int columnIndex, final double value)`
- **Summary:** Sets the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
  - `value` (`double`) — the value to set
- **Signature:** `public void set(final Point point, final double value)`
- **Summary:** Sets the element at the specified point to the given value.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
  - `value` (`double`) — the new double value to set at the specified point
- **See also:** #set(int, int, double)
##### valueAbove(...) -> OptionalDouble
- **Signature:** `public OptionalDouble valueAbove(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly above the specified position, if it exists.
- **Contract:**
  - Returns the element directly above the specified position, if it exists.
  - This method provides safe access to the element directly above the given position without throwing an exception when at the top edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@code OptionalDouble} containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
##### valueBelow(...) -> OptionalDouble
- **Signature:** `public OptionalDouble valueBelow(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly below the specified position, if it exists.
- **Contract:**
  - Returns the element directly below the specified position, if it exists.
  - This method provides safe access to the element directly below the given position without throwing an exception when at the bottom edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@code OptionalDouble} containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
##### valueLeft(...) -> OptionalDouble
- **Signature:** `public OptionalDouble valueLeft(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the left of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the left of the specified position, if it exists.
  - This method provides safe access to the element directly to the left of the given position without throwing an exception when at the leftmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@code OptionalDouble} containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
##### valueRight(...) -> OptionalDouble
- **Signature:** `public OptionalDouble valueRight(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the right of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the right of the specified position, if it exists.
  - This method provides safe access to the element directly to the right of the given position without throwing an exception when at the rightmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@code OptionalDouble} containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
##### rowView(...) -> double\[\]
- **Signature:** `@Override public double[] rowView(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as a double array.
- **Contract:**
  - If you need an independent copy, use {@link #rowCopy(int)} .
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the specified row array (direct reference to internal storage)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### rowCopy(...) -> double\[\]
- **Signature:** `@Override public double[] rowCopy(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new double array containing the values from the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### columnCopy(...) -> double\[\]
- **Signature:** `@Override public double[] columnCopy(final int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a copy of the specified column as a new double array.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new array containing the values from the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex &lt; 0 or columnIndex &gt; = columnCount
##### setRow(...) -> void
- **Signature:** `public void setRow(final int rowIndex, final double[] row) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified row by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of columns in the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to set (0-based)
  - `row` (`double[]`) — the array of values to copy into the row; must have length equal to the number of columns
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex is out of bounds or row length does not match column count
##### setColumn(...) -> void
- **Signature:** `public void setColumn(final int columnIndex, final double[] column) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified column by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of rows in the matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to set (0-based)
  - `column` (`double[]`) — the array of values to copy into the column; must have length equal to the number of rows
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex is out of bounds or column length does not match row count
##### updateRow(...) -> void
- **Signature:** `public <E extends Exception> void updateRow(final int rowIndex, final Throwables.DoubleUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified row by applying the given operator to each element.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to update (0-based)
  - `operator` (`Throwables.DoubleUnaryOperator<E>`) — the operator to apply to each element in the row; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### updateColumn(...) -> void
- **Signature:** `public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.DoubleUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified column in-place by applying the given operator to each element.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to update (0-based)
  - `operator` (`Throwables.DoubleUnaryOperator<E>`) — the operator to apply to each element in the column; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### getMainDiagonal(...) -> double\[\]
- **Signature:** `@Override public double[] getMainDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the main diagonal elements (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new double array containing a copy of the main diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setMainDiagonal(...) -> void
- **Signature:** `@Override public void setMainDiagonal(final double[] mainDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `mainDiagonal` (`double[]`) — the new values for the main diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if mainDiagonal array length does not equal rowCount
##### updateMainDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateMainDiagonal(final Throwables.DoubleUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the main diagonal (upper-left to lower-right) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.DoubleUnaryOperator<E>`) — the operator to apply to each diagonal element; receives the current element value and returns the new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### getAntiDiagonal(...) -> double\[\]
- **Signature:** `@Override public double[] getAntiDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the anti-diagonal elements (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new double array containing a copy of the anti-diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setAntiDiagonal(...) -> void
- **Signature:** `@Override public void setAntiDiagonal(final double[] antiDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `antiDiagonal` (`double[]`) — the new values for the anti-diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if antiDiagonal array length does not equal rowCount
##### updateAntiDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateAntiDiagonal(final Throwables.DoubleUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the anti-diagonal (upper-right to lower-left) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.DoubleUnaryOperator<E>`) — the operator to apply to each anti-diagonal element; receives the current element value and returns the new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### updateAll(...) -> void
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.DoubleUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the matrix in-place by applying the specified operator.
- **Contract:**
  - Elements are processed in row-major order when executed sequentially.
- **Parameters:**
  - `operator` (`Throwables.DoubleUnaryOperator<E>`) — the operator to apply to each element; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Double, E> mapper) throws E`
- **Summary:** Updates all elements in the matrix in-place based on their position (row and column indices).
- **Parameters:**
  - `mapper` (`Throwables.IntBiFunction<? extends Double, E>`) — the function that receives row index and column index (0-based) and returns the new value for that position
- **Throws:**
  - `E` — if the mapper throws an exception
##### replaceIf(...) -> void
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.DoublePredicate<E> predicate, final double newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on a predicate.
- **Parameters:**
  - `predicate` (`Throwables.DoublePredicate<E>`) — the condition to test each element; elements for which this returns {@code true} will be replaced
  - `newValue` (`double`) — the value to use for replacing matching elements
- **Throws:**
  - `E` — if the predicate throws an exception
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final double newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on their position (row and column indices).
- **Parameters:**
  - `predicate` (`Throwables.IntBiPredicate<E>`) — the condition that tests row index and column index (0-based); elements at positions for which this returns {@code true} will be replaced
  - `newValue` (`double`) — the value to use for replacing matching elements
- **Throws:**
  - `E` — if the predicate throws an exception
##### map(...) -> DoubleMatrix
- **Signature:** `public <E extends Exception> DoubleMatrix map(final Throwables.DoubleUnaryOperator<E> mapper) throws E`
- **Summary:** Creates a new DoubleMatrix by applying a transformation function to each element.
- **Parameters:**
  - `mapper` (`Throwables.DoubleUnaryOperator<E>`) — the function to apply to each element; receives the current element value and returns the transformed value
- **Returns:** a new DoubleMatrix with transformed values
- **Throws:**
  - `E` — if the function throws an exception
- **See also:** #updateAll(Throwables.DoubleUnaryOperator)
##### mapToInt(...) -> IntMatrix
- **Signature:** `public <E extends Exception> IntMatrix mapToInt(final Throwables.DoubleToIntFunction<E> mapper) throws E`
- **Summary:** Creates a new IntMatrix by applying the specified function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.DoubleToIntFunction<E>`) — the mapping function that converts each double element to an int; must not be null
- **Returns:** a new IntMatrix with the mapped values (same dimensions as the original)
- **Throws:**
  - `E` — if the function throws an exception
##### mapToLong(...) -> LongMatrix
- **Signature:** `public <E extends Exception> LongMatrix mapToLong(final Throwables.DoubleToLongFunction<E> mapper) throws E`
- **Summary:** Creates a new LongMatrix by applying the specified function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.DoubleToLongFunction<E>`) — the mapping function that converts each double element to a long; must not be null
- **Returns:** a new LongMatrix with the mapped values (same dimensions as the original)
- **Throws:**
  - `E` — if the function throws an exception
##### mapToObj(...) -> Matrix<R>
- **Signature:** `public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.DoubleFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E`
- **Summary:** Creates a new object Matrix by applying the specified function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.DoubleFunction<? extends R, E>`) — the mapping function that converts each double element to type R; must not be null
  - `targetElementType` (`Class<R>`) — the class object representing the target element type (used for array creation); must not be null
- **Returns:** a new Matrix &lt; R &gt; with the mapped values (same dimensions as the original)
- **Throws:**
  - `E` — if the function throws an exception
##### fill(...) -> void
- **Signature:** `public void fill(final double value)`
- **Summary:** Fills the entire matrix with the specified value in-place.
- **Parameters:**
  - `value` (`double`) — the value to fill the matrix with
- **Signature:** `public void fill(final double[][] source)`
- **Summary:** Fills the matrix with values from the specified two-dimensional array in-place, starting from position (0,0).
- **Contract:**
  - If the source array is smaller than the matrix, only the overlapping region is filled.
  - If the source array is larger, only the portion that fits is copied.
- **Parameters:**
  - `source` (`double[][]`) — the source array to copy values from (may be smaller or larger than the matrix)
- **Signature:** `public void fill(final int destRowIndex, final int destColumnIndex, final double[][] source) throws IllegalArgumentException`
- **Summary:** Fills a portion of the matrix with values from the specified two-dimensional array in-place, starting from a specified position.
- **Contract:**
  - If the source array extends beyond the matrix bounds from the starting position, only the portion that fits is copied.
- **Parameters:**
  - `destRowIndex` (`int`) — the target row index in this matrix (0-based)
  - `destColumnIndex` (`int`) — the target column index in this matrix (0-based)
  - `source` (`double[][]`) — the source array to copy values from; must not be {@code null} . Individual {@code null} sub-arrays in {@code source} are skipped.
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code source} is {@code null} , or if the target indices are negative or exceed matrix dimensions
##### copy(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a copy of this matrix with full independence guarantee
- **Signature:** `@Override public DoubleMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a copy of a row range from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new DoubleMatrix containing a copy of the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds
- **Signature:** `@Override public DoubleMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a copy of a rectangular region from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new DoubleMatrix containing the specified region with dimensions (toRowIndex - fromRowIndex) × (toColumnIndex - fromColumnIndex)
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds, fromRowIndex &gt; toRowIndex, or fromColumnIndex &gt; toColumnIndex
##### resize(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix resize(final int newRowCount, final int newColumnCount)`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code 0.0} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
- **Returns:** a new DoubleMatrix with the specified dimensions
- **See also:** #resize(int, int, double), #extend(int, int, int, int)
- **Signature:** `public DoubleMatrix resize(final int newRowCount, final int newColumnCount, final double defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code defaultValue} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
  - `defaultValue` (`double`) — the double value used to fill any newly created cells
- **Returns:** a new DoubleMatrix with the specified dimensions
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code newRowCount} or {@code newColumnCount} is negative, or if {@code newRowCount * newColumnCount} would overflow {@code Integer.MAX_VALUE}
- **See also:** #resize(int, int), #extend(int, int, int, int, double)
##### extend(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight)`
- **Summary:** Returns a new matrix formed by adding {@code 0.0} -filled padding around every edge of this matrix.
- **Contract:**
  - Use {@code resize} when you need exact output dimensions regardless of the original size.
- **Parameters:**
  - `padTop` (`int`) — number of rows to add above; must be {@code >= 0}
  - `padBottom` (`int`) — number of rows to add below; must be {@code >= 0}
  - `padLeft` (`int`) — number of columns to add to the left; must be {@code >= 0}
  - `padRight` (`int`) — number of columns to add to the right; must be {@code >= 0}
- **Returns:** a new DoubleMatrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
- **See also:** #extend(int, int, int, int, double), #resize(int, int)
- **Signature:** `public DoubleMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final double defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix formed by adding {@code defaultValue} -filled padding around every edge of this matrix.
- **Parameters:**
  - `padTop` (`int`) — number of rows to add above; must be {@code >= 0}
  - `padBottom` (`int`) — number of rows to add below; must be {@code >= 0}
  - `padLeft` (`int`) — number of columns to add to the left; must be {@code >= 0}
  - `padRight` (`int`) — number of columns to add to the right; must be {@code >= 0}
  - `defaultValue` (`double`) — the double value used to fill all newly added cells
- **Returns:** a new DoubleMatrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any padding parameter is negative, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** #extend(int, int, int, int), #resize(int, int, double)
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `@Override public void flipHorizontallyInPlace()`
- **Summary:** Reverses the order of elements in each row (horizontal flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally()
##### flipVerticallyInPlace(...) -> void
- **Signature:** `@Override public void flipVerticallyInPlace()`
- **Summary:** Reverses the order of rows (vertical flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipVertically()
##### flipHorizontally(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix flipHorizontally()`
- **Summary:** Returns a new matrix that is a horizontal flip of this matrix (columns in reversed order within each row).
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a horizontal flip of this matrix (columns in reversed order within each row)
- **See also:** #flipHorizontallyInPlace(), #flipVertically(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### flipVertically(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix flipVertically()`
- **Summary:** Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a vertical flip of this matrix (rows in reversed order)
- **See also:** #flipVerticallyInPlace(), #flipHorizontally(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### rotate90(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 90 degrees clockwise
##### rotate180(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 180 degrees clockwise
##### rotate270(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 270 degrees clockwise
##### transpose(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix transpose()`
- **Summary:** Creates the transpose of this matrix by swapping rows and columns.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is the transpose of this matrix with dimensions columnCount × rowCount
##### reshape(...) -> DoubleMatrix
- **Signature:** `@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG") @Override public DoubleMatrix reshape(final int newRowCount, final int newColumnCount)`
- **Summary:** Reshapes the matrix to new dimensions while preserving element order.
- **Contract:**
  - <p> The new shape must have at least as many total elements as the original ( {@code newRowCount * newColumnCount >= elementCount()} ).
  - If the new shape has more total elements, the additional positions are filled with zeros.
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix; must be {@code >= 0}
- **Returns:** a new DoubleMatrix with the specified shape containing this matrix's elements
##### repeatElements(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats elements of the matrix in both row and column directions.
- **Parameters:**
  - `rowRepeats` (`int`) — the number of times each element is duplicated vertically (along the row axis); must be positive
  - `columnRepeats` (`int`) — the number of times each element is duplicated horizontally (along the column axis); must be positive
- **Returns:** a new matrix with dimensions {@code (rowCount * rowRepeats) x (columnCount * columnRepeats)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is not positive, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#repeatElements(int, int)
##### repeatMatrix(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats the entire matrix in both row and column directions.
- **Parameters:**
  - `rowRepeats` (`int`) — the number of times to tile the whole matrix vertically (along the row axis); must be positive
  - `columnRepeats` (`int`) — the number of times to tile the whole matrix horizontally (along the column axis); must be positive
- **Returns:** a new matrix with dimensions {@code (rowCount * rowRepeats) x (columnCount * columnRepeats)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is not positive, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#repeatMatrix(int, int)
##### flatten(...) -> DoubleList
- **Signature:** `@Override public DoubleList flatten()`
- **Summary:** Returns a DoubleList containing all matrix elements in row-major order (left-to-right, top-to-bottom).
- **Parameters:**
  - (none)
- **Returns:** a DoubleList containing all elements in row-major order
##### mutateAsFlat(...) -> void
- **Signature:** `@Override public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super double[], E> action) throws E`
- **Summary:** Flattens all elements of this matrix into a single one-dimensional array, applies the given operation to that flattened array, and then copies the modified elements back into the matrix.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super double[], E>`) — the operation to apply to the flattened array
- **Throws:**
  - `E` — if the operation throws an exception
- **See also:** Arrays#mutateAsFlat(double\[\]\[\], Throwables.Consumer)
##### stackVertically(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix stackVertically(final DoubleMatrix other) throws IllegalArgumentException`
- **Summary:** Vertically stacks this matrix with another matrix.
- **Contract:**
  - The matrices must have the same number of columns.
- **Parameters:**
  - `other` (`DoubleMatrix`) — the matrix to stack below this matrix; must not be null
- **Returns:** a new matrix with combined rows
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , the matrices have different number of columns, or the merged row count would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackVertically(IntMatrix)
##### stackHorizontally(...) -> DoubleMatrix
- **Signature:** `@Override public DoubleMatrix stackHorizontally(final DoubleMatrix other) throws IllegalArgumentException`
- **Summary:** Horizontally stacks this matrix with another matrix.
- **Contract:**
  - The matrices must have the same number of rows.
- **Parameters:**
  - `other` (`DoubleMatrix`) — the matrix to stack to the right of this matrix; must not be null
- **Returns:** a new matrix with combined columns
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , the matrices have different number of rows, or the merged column count would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackHorizontally(IntMatrix)
##### add(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix add(final DoubleMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise addition of this matrix with another matrix.
- **Contract:**
  - The matrices must have the same dimensions.
  - If either operand is {@code NaN} , the result at that position is {@code NaN} .
- **Parameters:**
  - `other` (`DoubleMatrix`) — the matrix to add to this matrix; must not be null
- **Returns:** a new matrix containing the element-wise sum
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrices have different dimensions
##### subtract(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix subtract(final DoubleMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise subtraction of another matrix from this matrix.
- **Contract:**
  - The matrices must have the same dimensions.
  - If either operand is {@code NaN} , the result at that position is {@code NaN} .
- **Parameters:**
  - `other` (`DoubleMatrix`) — the matrix to subtract from this matrix; must not be null
- **Returns:** a new matrix containing the element-wise difference
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrices have different dimensions
##### matmul(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix matmul(final DoubleMatrix other) throws IllegalArgumentException`
- **Summary:** Performs matrix multiplication (Cayley product) with another matrix.
- **Contract:**
  - The number of columns in this matrix must equal the number of rows in the other matrix.
- **Parameters:**
  - `other` (`DoubleMatrix`) — the matrix to multiply with this matrix; must not be null
- **Returns:** a new matrix containing the matrix product with dimensions (this.rowCount × other.columnCount)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrix dimensions are incompatible for multiplication (i.e., this.columnCount != other.rowCount)
##### boxed(...) -> Matrix<Double>
- **Signature:** `public Matrix<Double> boxed()`
- **Summary:** Converts this primitive double matrix to a boxed Double matrix.
- **Parameters:**
  - (none)
- **Returns:** a new Matrix &lt; Double &gt; containing boxed Double values (same dimensions as the original)
- **See also:** #unbox(Matrix)
##### toIntMatrix(...) -> IntMatrix
- **Signature:** `public IntMatrix toIntMatrix()`
- **Summary:** Converts this double matrix to an int matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code IntMatrix} with values converted from double to int
##### toLongMatrix(...) -> LongMatrix
- **Signature:** `public LongMatrix toLongMatrix()`
- **Summary:** Converts this double matrix to a long matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code LongMatrix} with values converted from double to long
##### toFloatMatrix(...) -> FloatMatrix
- **Signature:** `public FloatMatrix toFloatMatrix()`
- **Summary:** Converts this double matrix to a float matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code FloatMatrix} with values converted from double to float
##### zipWith(...) -> DoubleMatrix
- **Signature:** `public <E extends Exception> DoubleMatrix zipWith(final DoubleMatrix other, final Throwables.DoubleBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Applies a binary operation element-wise to this matrix and another matrix.
- **Contract:**
  - The matrices must have the same dimensions.
- **Parameters:**
  - `other` (`DoubleMatrix`) — the matrix to combine with this matrix; must have the same dimensions and must not be null
  - `zipFunction` (`Throwables.DoubleBinaryOperator<E>`) — the binary operation to apply to corresponding elements; must not be null
- **Returns:** a new matrix with the operation applied element-wise (same dimensions as the input matrices)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} or {@code zipFunction} is {@code null} , or the matrices have different dimensions
  - `E` — if the zip function throws an exception
- **Signature:** `public <E extends Exception> DoubleMatrix zipWith(final DoubleMatrix other, final DoubleMatrix third, final Throwables.DoubleTernaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Applies a ternary operation element-wise to this matrix and two other matrices.
- **Contract:**
  - All three matrices must have the same dimensions.
- **Parameters:**
  - `other` (`DoubleMatrix`) — the second matrix to combine; must have the same dimensions and must not be null
  - `third` (`DoubleMatrix`) — the third matrix to combine; must have the same dimensions and must not be null
  - `zipFunction` (`Throwables.DoubleTernaryOperator<E>`) — the ternary operation to apply to corresponding elements; must not be null
- **Returns:** a new matrix with the operation applied element-wise (same dimensions as the input matrices)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} , {@code third} , or {@code zipFunction} is {@code null} , or the matrices have different dimensions
  - `E` — if the zip function throws an exception
##### mainDiagonalStream(...) -> DoubleStream
- **Signature:** `@Override public DoubleStream mainDiagonalStream()`
- **Summary:** Returns a stream of elements from the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a DoubleStream of diagonal elements from upper-left to lower-right
##### antiDiagonalStream(...) -> DoubleStream
- **Signature:** `@Override public DoubleStream antiDiagonalStream()`
- **Summary:** Returns a stream of elements from the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a DoubleStream of diagonal elements from upper-right to lower-left
##### horizontalStream(...) -> DoubleStream
- **Signature:** `@Override public DoubleStream horizontalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
- **Parameters:**
  - (none)
- **Returns:** a DoubleStream of all matrix elements in row-major order, or an empty stream if the matrix is empty
- **Signature:** `@Override public DoubleStream horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a single row.
- **Contract:**
  - <p> This method is particularly useful when you need to process or analyze a specific row of the matrix independently.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to stream (0-based)
- **Returns:** a DoubleStream of elements in the specified row, from left to right
- **Signature:** `@Override public DoubleStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of rows in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a DoubleStream of elements in the specified row range, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
##### verticalStream(...) -> DoubleStream
- **Signature:** `@Override @Beta public DoubleStream verticalStream()`
- **Summary:** Creates a stream of all elements in the matrix in column-major order.
- **Parameters:**
  - (none)
- **Returns:** a DoubleStream of all matrix elements in column-major order
- **Signature:** `@Override public DoubleStream verticalStream(final int columnIndex)`
- **Summary:** Creates a stream of elements from a single column in the matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to stream (0-based)
- **Returns:** a DoubleStream of elements in the specified column, from top to bottom
- **Signature:** `@Override @Beta public DoubleStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a stream of elements from a range of columns in column-major order.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a DoubleStream of elements in the specified column range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the column indices are out of bounds
##### rowStreams(...) -> Stream<DoubleStream>
- **Signature:** `@Override public Stream<DoubleStream> rowStreams()`
- **Summary:** Creates a stream of streams, where each inner stream represents a complete row of the matrix.
- **Parameters:**
  - (none)
- **Returns:** a Stream of DoubleStreams, one for each row in the matrix
- **Signature:** `@Override public Stream<DoubleStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a stream of streams for a range of rows.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a Stream of DoubleStreams for the specified row range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the row indices are out of bounds
##### columnStreams(...) -> Stream<DoubleStream>
- **Signature:** `@Override @Beta public Stream<DoubleStream> columnStreams()`
- **Summary:** Creates a stream of streams, where each inner stream represents a complete column of the matrix.
- **Parameters:**
  - (none)
- **Returns:** a Stream of DoubleStreams, one for each column in the matrix
- **Signature:** `@Override @Beta public Stream<DoubleStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a stream of streams for a range of columns.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a Stream of DoubleStreams for the specified column range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the column indices are out of bounds
##### forEach(...) -> void
- **Signature:** `public <E extends Exception> void forEach(final Throwables.DoubleConsumer<E> action) throws E`
- **Summary:** Performs the specified action for each element in this matrix.
- **Parameters:**
  - `action` (`Throwables.DoubleConsumer<E>`) — the action to perform on each element; must not be null
- **Throws:**
  - `E` — if the action throws an exception
- **Signature:** `public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.DoubleConsumer<E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element in the specified sub-region of this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based, must be &gt; = 0 and &lt; = toRowIndex)
  - `toRowIndex` (`int`) — the ending row index (exclusive, must be &gt; = fromRowIndex and &lt; = rowCount)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based, must be &gt; = 0 and &lt; = toColumnIndex)
  - `toColumnIndex` (`int`) — the ending column index (exclusive, must be &gt; = fromColumnIndex and &lt; = columnCount)
  - `action` (`Throwables.DoubleConsumer<E>`) — the action to perform on each element in the sub-region; must not be null
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds or invalid
  - `E` — if the action throws an exception
##### println(...) -> String
- **Signature:** `@Override public String println()`
- **Summary:** Prints this matrix to standard output and returns the formatted string.
- **Parameters:**
  - (none)
- **Returns:** the formatted string representation of the matrix
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this matrix.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this matrix
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this matrix to the specified object for equality.
- **Contract:**
  - Returns {@code true} if the given object is also a DoubleMatrix with the same dimensions and all corresponding elements are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this matrix

### Class FloatMatrix (com.landawn.abacus.matrix.FloatMatrix)
Matrix implementation backed by a rectangular {@code float\[\]\[\]} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### empty(...) -> FloatMatrix
- **Signature:** `public static FloatMatrix empty()`
- **Summary:** Creates an empty matrix with zero rows and zero columns.
- **Parameters:**
  - (none)
- **Returns:** an empty float matrix
##### of(...) -> FloatMatrix
- **Signature:** `public static FloatMatrix of(final float[]... a)`
- **Summary:** Creates a {@code FloatMatrix} from a two-dimensional float array.
- **Contract:**
  - <p> <b> Important: </b> When the input is non-empty the provided array is used directly without defensive copying after rectangular-shape validation.
- **Parameters:**
  - `a` (`float[][]`) — the two-dimensional float array to create the matrix from, or {@code null} /empty for an empty matrix
- **Returns:** a new {@code FloatMatrix} wrapping the provided data, or the shared empty {@code FloatMatrix} if input is {@code null} or empty
##### from(...) -> FloatMatrix
- **Signature:** `public static FloatMatrix from(final int[]... a)`
- **Summary:** Creates a FloatMatrix from a two-dimensional int array by converting int values to float.
- **Contract:**
  - <p> <b> Note: </b> Int values with more than 24 significant bits may lose precision when converted to float, since float has a 23-bit mantissa.
  - </p> <p> <b> Requirements: </b> </p> <ul> <li> All rows must be non-null and have the same length as the first row (rectangular array required) </li> <li> The first row cannot be {@code null} if the array is non-empty </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code FloatMatrix matrix = FloatMatrix.from(new int\[\]\[\] {{1, 2}, {3, 4}}); // Creates a matrix with values {{1.0f, 2.0f}, {3.0f, 4.0f}} assert matrix.get(1, 0) == 3.0f; assert matrix.rowCount() == 2 && matrix.columnCount() == 2; } </pre>
- **Parameters:**
  - `a` (`int[][]`) — the two-dimensional int array to convert to a float matrix, or {@code null} /empty for an empty matrix
- **Returns:** a new {@code FloatMatrix} with converted values, or the shared empty {@code FloatMatrix} if input is {@code null} or empty
##### random(...) -> FloatMatrix
- **Signature:** `public static FloatMatrix random(final int length)`
- **Summary:** Creates a new {@code 1 x length} matrix filled with random float values uniformly distributed in {@code \[0.0f, 1.0f)} .
- **Parameters:**
  - `length` (`int`) — the number of columns in the new matrix
- **Returns:** a new {@code FloatMatrix} of dimensions {@code 1 x length} filled with random values in {@code \[0.0f, 1.0f)}
- **Signature:** `public static FloatMatrix random(final int rowCount, final int columnCount)`
- **Summary:** Creates a new matrix of the specified dimensions filled with random float values uniformly distributed in {@code \[0.0f, 1.0f)} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix
  - `columnCount` (`int`) — the number of columns in the new matrix
- **Returns:** a new {@code FloatMatrix} of dimensions {@code rowCount x columnCount} filled with random values in {@code \[0.0f, 1.0f)}
##### repeat(...) -> FloatMatrix
- **Signature:** `public static FloatMatrix repeat(final int rowCount, final int columnCount, final float element)`
- **Summary:** Creates a new matrix of the specified dimensions where every element is the provided {@code element} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix
  - `columnCount` (`int`) — the number of columns in the new matrix
  - `element` (`float`) — the float value to fill the matrix with (may be {@code NaN} , {@code +/-Infinity} , or {@code -0.0f} )
- **Returns:** a new {@code FloatMatrix} of dimensions {@code rowCount x columnCount} with every cell equal to {@code element}
##### mainDiagonal(...) -> FloatMatrix
- **Signature:** `public static FloatMatrix mainDiagonal(final float[] mainDiagonal)`
- **Summary:** Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
- **Parameters:**
  - `mainDiagonal` (`float[]`) — the array of main diagonal elements, or null/empty for an empty matrix
- **Returns:** a square matrix with the specified main diagonal, or an empty matrix if input is null or empty
##### antiDiagonal(...) -> FloatMatrix
- **Signature:** `public static FloatMatrix antiDiagonal(final float[] antiDiagonal)`
- **Summary:** Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
- **Parameters:**
  - `antiDiagonal` (`float[]`) — the array of anti-diagonal elements, or null/empty for an empty matrix
- **Returns:** a square matrix with the specified anti-diagonal, or an empty matrix if input is null or empty
##### diagonals(...) -> FloatMatrix
- **Signature:** `public static FloatMatrix diagonals(final float[] mainDiagonal, final float[] antiDiagonal) throws IllegalArgumentException`
- **Summary:** Creates a square matrix from the specified main diagonal and anti-diagonal elements.
- **Contract:**
  - If both arrays are provided, they must have the same length.
  - If both diagonals share a position (which happens for odd-sized matrices at the center element), the main diagonal value takes precedence.
- **Parameters:**
  - `mainDiagonal` (`float[]`) — the array of main diagonal elements (can be null or empty)
  - `antiDiagonal` (`float[]`) — the array of anti-diagonal elements (can be null or empty)
- **Returns:** a square matrix with the specified diagonals, or an empty matrix if both inputs are null or empty
- **Throws:**
  - `java.lang.IllegalArgumentException` — if both arrays are non-empty and have different lengths
##### unbox(...) -> FloatMatrix
- **Signature:** `public static FloatMatrix unbox(final Matrix<Float> x)`
- **Summary:** Converts a boxed {@code Matrix<Float>} to a primitive {@code FloatMatrix} .
- **Contract:**
  - This conversion improves memory efficiency and performance when working with large matrices.
- **Parameters:**
  - `x` (`Matrix<Float>`) — the boxed {@code Matrix<Float>} to convert; must not be {@code null}
- **Returns:** a new {@code FloatMatrix} with primitive float values, the same shape as {@code x}
- **See also:** #boxed()

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public FloatMatrix(final float[][] a)`
- **Summary:** Constructs a {@code FloatMatrix} backed by the supplied two-dimensional array.
- **Contract:**
  - <p> If {@code a} is {@code null} , this creates an empty {@code 0x0} matrix.
  - Call {@link #copy()} if you need an independently owned matrix.
- **Parameters:**
  - `a` (`float[][]`) — the two-dimensional float array to wrap, or {@code null} for an empty matrix
##### get(...) -> float
- **Signature:** `public float get(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** the element at position (rowIndex, columnIndex)
- **Signature:** `public float get(final Point point)`
- **Summary:** Returns the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
- **Returns:** the float element at the specified point
- **See also:** #get(int, int)
##### set(...) -> void
- **Signature:** `public void set(final int rowIndex, final int columnIndex, final float value)`
- **Summary:** Sets the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
  - `value` (`float`) — the value to set
- **Signature:** `public void set(final Point point, final float value)`
- **Summary:** Sets the element at the specified point to the given value.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
  - `value` (`float`) — the new float value to set at the specified point
- **See also:** #set(int, int, float)
##### valueAbove(...) -> OptionalFloat
- **Signature:** `public OptionalFloat valueAbove(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly above the specified position, if it exists.
- **Contract:**
  - Returns the element directly above the specified position, if it exists.
  - This method provides safe access to the element directly above the given position without throwing an exception when at the top edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@link OptionalFloat} containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
##### valueBelow(...) -> OptionalFloat
- **Signature:** `public OptionalFloat valueBelow(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly below the specified position, if it exists.
- **Contract:**
  - Returns the element directly below the specified position, if it exists.
  - This method provides safe access to the element directly below the given position without throwing an exception when at the bottom edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@link OptionalFloat} containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
##### valueLeft(...) -> OptionalFloat
- **Signature:** `public OptionalFloat valueLeft(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the left of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the left of the specified position, if it exists.
  - This method provides safe access to the element directly to the left of the given position without throwing an exception when at the left edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@link OptionalFloat} containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
##### valueRight(...) -> OptionalFloat
- **Signature:** `public OptionalFloat valueRight(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the right of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the right of the specified position, if it exists.
  - This method provides safe access to the element directly to the right of the given position without throwing an exception when at the right edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@link OptionalFloat} containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
##### rowView(...) -> float\[\]
- **Signature:** `@Override public float[] rowView(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as a float array.
- **Contract:**
  - If you need an independent copy, use {@link #rowCopy(int)} .
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the specified row array (direct reference to internal storage)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
- **See also:** #rowCopy(int)
##### rowCopy(...) -> float\[\]
- **Signature:** `@Override public float[] rowCopy(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new float array containing the values from the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### columnCopy(...) -> float\[\]
- **Signature:** `@Override public float[] columnCopy(final int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a copy of the specified column as a new array.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new array containing a copy of the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex &lt; 0 or columnIndex &gt; = columnCount
##### setRow(...) -> void
- **Signature:** `public void setRow(final int rowIndex, final float[] row) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified row by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of columns in the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to set (0-based)
  - `row` (`float[]`) — the array of values to copy into the row; must have length equal to the number of columns
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex is out of bounds or row length does not match column count
##### setColumn(...) -> void
- **Signature:** `public void setColumn(final int columnIndex, final float[] column) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified column by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of rows in the matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to set (0-based)
  - `column` (`float[]`) — the array of values to copy into the column; must have length equal to the number of rows
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code columnIndex} is out of bounds or {@code column.length} does not match {@code rowCount}
##### updateRow(...) -> void
- **Signature:** `public <E extends Exception> void updateRow(final int rowIndex, final Throwables.FloatUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in a row in-place by applying the specified operator to each element.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to update (0-based)
  - `operator` (`Throwables.FloatUnaryOperator<E>`) — the operator to apply to each element in the row; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### updateColumn(...) -> void
- **Signature:** `public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.FloatUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified column in-place by applying the given operator to each element.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to update (0-based)
  - `operator` (`Throwables.FloatUnaryOperator<E>`) — the operator to apply to each element in the column; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### getMainDiagonal(...) -> float\[\]
- **Signature:** `@Override public float[] getMainDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the main diagonal elements (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new float array containing the main diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setMainDiagonal(...) -> void
- **Signature:** `@Override public void setMainDiagonal(final float[] mainDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `mainDiagonal` (`float[]`) — the new values for the main diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if mainDiagonal array length does not equal rowCount
##### updateMainDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateMainDiagonal(final Throwables.FloatUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the main diagonal (upper-left to lower-right) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.FloatUnaryOperator<E>`) — the operator to apply to each diagonal element; receives the current element value and returns the new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### getAntiDiagonal(...) -> float\[\]
- **Signature:** `@Override public float[] getAntiDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the anti-diagonal elements (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new float array containing the anti-diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setAntiDiagonal(...) -> void
- **Signature:** `@Override public void setAntiDiagonal(final float[] antiDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `antiDiagonal` (`float[]`) — the new values for the anti-diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if antiDiagonal array length does not equal rowCount
##### updateAntiDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateAntiDiagonal(final Throwables.FloatUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the anti-diagonal (upper-right to lower-left) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.FloatUnaryOperator<E>`) — the operator to apply to each anti-diagonal element; receives the current element value and returns the new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### updateAll(...) -> void
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.FloatUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the matrix in-place by applying the specified operator.
- **Contract:**
  - Elements are processed in row-major order when executed sequentially.
- **Parameters:**
  - `operator` (`Throwables.FloatUnaryOperator<E>`) — the operator to apply to each element; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Float, E> mapper) throws E`
- **Summary:** Updates all elements in the matrix in-place based on their position (row and column indices).
- **Parameters:**
  - `mapper` (`Throwables.IntBiFunction<? extends Float, E>`) — the function that receives row index and column index (0-based) and returns the new value for that position; must not return {@code null} (auto-unboxing would throw {@link NullPointerException} )
- **Throws:**
  - `E` — if the mapper throws an exception
##### replaceIf(...) -> void
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.FloatPredicate<E> predicate, final float newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on a predicate.
- **Parameters:**
  - `predicate` (`Throwables.FloatPredicate<E>`) — the condition to test each element; elements for which this returns {@code true} will be replaced
  - `newValue` (`float`) — the value to use for replacing matching elements (may be {@code NaN} , {@code +/-Infinity} , or {@code -0.0f} )
- **Throws:**
  - `E` — if the predicate throws an exception
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final float newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on their position (row and column indices).
- **Parameters:**
  - `predicate` (`Throwables.IntBiPredicate<E>`) — the condition to test each position; receives row index and column index (0-based) and returns {@code true} if the element at that position should be replaced
  - `newValue` (`float`) — the value to use for replacing at matching positions
- **Throws:**
  - `E` — if the predicate throws an exception
##### map(...) -> FloatMatrix
- **Signature:** `public <E extends Exception> FloatMatrix map(final Throwables.FloatUnaryOperator<E> mapper) throws E`
- **Summary:** Creates a new FloatMatrix by applying the specified function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.FloatUnaryOperator<E>`) — the mapping function to apply to each element; must not be null
- **Returns:** a new FloatMatrix with the mapped values (same dimensions as the original)
- **Throws:**
  - `E` — if the function throws an exception
##### mapToObj(...) -> Matrix<R>
- **Signature:** `public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.FloatFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E`
- **Summary:** Creates a new object Matrix by applying the specified function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.FloatFunction<? extends R, E>`) — the mapping function that converts each float element to type R; must not be null
  - `targetElementType` (`Class<R>`) — the class object representing the target element type (used for array creation); must not be null
- **Returns:** a new Matrix &lt; R &gt; with the mapped values (same dimensions as the original)
- **Throws:**
  - `E` — if the function throws an exception
##### fill(...) -> void
- **Signature:** `public void fill(final float value)`
- **Summary:** Fills the entire matrix with the specified value in-place.
- **Parameters:**
  - `value` (`float`) — the value to fill the matrix with
- **Signature:** `public void fill(final float[][] source)`
- **Summary:** Fills the matrix with values from the specified two-dimensional array in-place, starting from position (0,0).
- **Contract:**
  - If the source array is smaller than the matrix, only the overlapping region is filled.
  - If the source array is larger, only the portion that fits is copied.
- **Parameters:**
  - `source` (`float[][]`) — the source array to copy values from (may be smaller or larger than the matrix)
- **Signature:** `public void fill(final int destRowIndex, final int destColumnIndex, final float[][] source) throws IllegalArgumentException`
- **Summary:** Fills a portion of the matrix with values from the specified two-dimensional array in-place, starting from a specified position.
- **Contract:**
  - If the source array extends beyond the matrix bounds from the starting position, only the portion that fits is copied.
- **Parameters:**
  - `destRowIndex` (`int`) — the starting row index in this matrix (0-based, must be in {@code \[0, rowCount\]} )
  - `destColumnIndex` (`int`) — the starting column index in this matrix (0-based, must be in {@code \[0, columnCount\]} )
  - `source` (`float[][]`) — the source array to copy values from; must not be {@code null} . Individual rows of {@code source} may be {@code null} and are skipped.
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code source} is {@code null} , or {@code destRowIndex} or {@code destColumnIndex} is negative or strictly greater than the corresponding matrix dimension
##### copy(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a copy of this matrix with full independence guarantee
- **Signature:** `@Override public FloatMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a copy of a row range from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new FloatMatrix containing a copy of the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the indices are out of bounds
- **Signature:** `@Override public FloatMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a copy of a rectangular region from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new FloatMatrix containing the specified region with dimensions (toRowIndex - fromRowIndex) × (toColumnIndex - fromColumnIndex)
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds, fromRowIndex &gt; toRowIndex, or fromColumnIndex &gt; toColumnIndex
##### resize(...) -> FloatMatrix
- **Signature:** `public FloatMatrix resize(final int newRowCount, final int newColumnCount)`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code 0.0f} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
- **Returns:** a new {@code FloatMatrix} with the specified dimensions
- **See also:** #resize(int, int, float), #extend(int, int, int, int)
- **Signature:** `public FloatMatrix resize(final int newRowCount, final int newColumnCount, final float defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code defaultValue} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
  - `defaultValue` (`float`) — the float value used to fill any newly created cells (may be {@code NaN} , {@code +/-Infinity} , or {@code -0.0f} )
- **Returns:** a new {@code FloatMatrix} with the specified dimensions
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code newRowCount} or {@code newColumnCount} is negative, or if {@code newRowCount * newColumnCount} would overflow {@code Integer.MAX_VALUE}
- **See also:** #resize(int, int), #extend(int, int, int, int, float)
##### extend(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight)`
- **Summary:** Returns a new matrix formed by adding {@code 0.0f} -filled padding around every edge of this matrix.
- **Contract:**
  - Use {@code resize} when you need exact output dimensions regardless of the original size.
- **Parameters:**
  - `padTop` (`int`) — number of rows to add above; must be {@code >= 0}
  - `padBottom` (`int`) — number of rows to add below; must be {@code >= 0}
  - `padLeft` (`int`) — number of columns to add to the left; must be {@code >= 0}
  - `padRight` (`int`) — number of columns to add to the right; must be {@code >= 0}
- **Returns:** a new FloatMatrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
- **See also:** #extend(int, int, int, int, float), #resize(int, int)
- **Signature:** `public FloatMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final float defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix formed by adding {@code defaultValue} -filled padding around every edge of this matrix.
- **Parameters:**
  - `padTop` (`int`) — number of rows to add above; must be {@code >= 0}
  - `padBottom` (`int`) — number of rows to add below; must be {@code >= 0}
  - `padLeft` (`int`) — number of columns to add to the left; must be {@code >= 0}
  - `padRight` (`int`) — number of columns to add to the right; must be {@code >= 0}
  - `defaultValue` (`float`) — the float value used to fill all newly added cells
- **Returns:** a new FloatMatrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any padding parameter is negative, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** #extend(int, int, int, int), #resize(int, int, float)
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `@Override public void flipHorizontallyInPlace()`
- **Summary:** Reverses the order of elements in each row (horizontal flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally()
##### flipVerticallyInPlace(...) -> void
- **Signature:** `@Override public void flipVerticallyInPlace()`
- **Summary:** Reverses the order of rows in the matrix (vertical flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipVertically()
##### flipHorizontally(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix flipHorizontally()`
- **Summary:** Returns a new matrix that is a horizontal flip of this matrix (columns in reversed order within each row).
- **Parameters:**
  - (none)
- **Returns:** a new FloatMatrix with each row reversed
- **See also:** #flipHorizontallyInPlace(),for an in-place version, #flipVertically(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### flipVertically(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix flipVertically()`
- **Summary:** Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a vertical flip of this matrix (rows in reversed order)
- **See also:** #flipVerticallyInPlace(),for an in-place version, #flipHorizontally(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### rotate90(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 90 degrees clockwise
##### rotate180(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 180 degrees clockwise
##### rotate270(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 270 degrees clockwise
##### transpose(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix transpose()`
- **Summary:** Creates the transpose of this matrix by swapping rows and columns.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is the transpose of this matrix with dimensions columnCount × rowCount
##### reshape(...) -> FloatMatrix
- **Signature:** `@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG") @Override public FloatMatrix reshape(final int newRowCount, final int newColumnCount)`
- **Summary:** Reshapes the matrix to new dimensions while preserving element order.
- **Contract:**
  - <p> The new shape must have at least as many total elements as the original ( {@code newRowCount * newColumnCount >= elementCount()} ).
  - If the new shape has more total elements, the additional positions are filled with zeros.
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix
- **Returns:** a new FloatMatrix with the specified shape containing this matrix's elements
##### repeatElements(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats elements in both row and column directions.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat each element in row direction; must be {@code > 0}
  - `columnRepeats` (`int`) — number of times to repeat each element in column direction; must be {@code > 0}
- **Returns:** a new {@code FloatMatrix} of dimensions {@code (rowCount*rowRepeats) x (columnCount*columnRepeats)} with repeated elements
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is not positive, or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#repeatElements(int, int)
##### repeatMatrix(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats the entire matrix in a tiled pattern.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat the matrix vertically; must be {@code > 0}
  - `columnRepeats` (`int`) — number of times to repeat the matrix horizontally; must be {@code > 0}
- **Returns:** a new {@code FloatMatrix} of dimensions {@code (rowCount*rowRepeats) x (columnCount*columnRepeats)} with the tiled pattern
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is not positive, or if either resulting dimension would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#repeatMatrix(int, int)
##### flatten(...) -> FloatList
- **Signature:** `@Override public FloatList flatten()`
- **Summary:** Returns a list containing all matrix elements in row-major order.
- **Parameters:**
  - (none)
- **Returns:** a new {@link FloatList} of all elements in row-major order
##### mutateAsFlat(...) -> void
- **Signature:** `@Override public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super float[], E> action) throws E`
- **Summary:** Flattens all elements of this matrix into a single one-dimensional array, applies the given operation to that flattened array, and then copies the (possibly modified) elements back into the matrix in row-major order.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super float[], E>`) — the operation to apply to the flattened array
- **Throws:**
  - `E` — if the operation throws an exception
- **See also:** Arrays#mutateAsFlat(float\[\]\[\], Throwables.Consumer)
##### stackVertically(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix stackVertically(final FloatMatrix other) throws IllegalArgumentException`
- **Summary:** Stacks this matrix vertically with another matrix.
- **Contract:**
  - The matrices must have the same number of columns.
- **Parameters:**
  - `other` (`FloatMatrix`) — the matrix to stack below this matrix; must not be null
- **Returns:** a new FloatMatrix with other stacked vertically below this matrix
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , the matrices don't have the same number of columns, or the merged row count would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackVertically(IntMatrix)
##### stackHorizontally(...) -> FloatMatrix
- **Signature:** `@Override public FloatMatrix stackHorizontally(final FloatMatrix other) throws IllegalArgumentException`
- **Summary:** Stacks this matrix horizontally with another matrix.
- **Contract:**
  - The matrices must have the same number of rows.
- **Parameters:**
  - `other` (`FloatMatrix`) — the matrix to stack to the right of this matrix; must not be null
- **Returns:** a new FloatMatrix with other stacked horizontally to the right
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , the matrices don't have the same number of rows, or the merged column count would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackHorizontally(IntMatrix)
##### add(...) -> FloatMatrix
- **Signature:** `public FloatMatrix add(final FloatMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise addition of this matrix with another matrix.
- **Contract:**
  - The matrices must have the same dimensions (same number of rows and columns).
  - If either operand is {@code NaN} , the result at that position is {@code NaN} .
- **Parameters:**
  - `other` (`FloatMatrix`) — the matrix to add to this matrix; must not be null
- **Returns:** a new FloatMatrix containing the element-wise sum (same dimensions as inputs)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrices have different dimensions
##### subtract(...) -> FloatMatrix
- **Signature:** `public FloatMatrix subtract(final FloatMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise subtraction of another matrix from this matrix.
- **Contract:**
  - The matrices must have the same dimensions (same number of rows and columns).
  - If either operand is {@code NaN} , the result at that position is {@code NaN} .
- **Parameters:**
  - `other` (`FloatMatrix`) — the matrix to subtract from this matrix; must not be null
- **Returns:** a new FloatMatrix containing the element-wise difference (same dimensions as inputs)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrices have different dimensions
##### matmul(...) -> FloatMatrix
- **Signature:** `public FloatMatrix matmul(final FloatMatrix other) throws IllegalArgumentException`
- **Summary:** Performs matrix multiplication of this matrix with another matrix.
- **Contract:**
  - The number of columns in this matrix must equal the number of rows in the other matrix.
  - Consider using {@link #toDoubleMatrix()} for higher precision if needed.
- **Parameters:**
  - `other` (`FloatMatrix`) — the matrix to multiply with this matrix; must not be null
- **Returns:** a new FloatMatrix containing the matrix product with dimensions (this.rowCount × other.columnCount)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrix dimensions are incompatible for multiplication (i.e., this.columnCount != other.rowCount)
##### boxed(...) -> Matrix<Float>
- **Signature:** `public Matrix<Float> boxed()`
- **Summary:** Converts this primitive float matrix to a boxed Float matrix.
- **Parameters:**
  - (none)
- **Returns:** a new Matrix containing boxed Float values
- **See also:** #unbox(Matrix)
##### toDoubleMatrix(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix toDoubleMatrix()`
- **Summary:** Converts this float matrix to a double matrix.
- **Parameters:**
  - (none)
- **Returns:** a new DoubleMatrix with converted values
##### toIntMatrix(...) -> IntMatrix
- **Signature:** `public IntMatrix toIntMatrix()`
- **Summary:** Converts this float matrix to an int matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code IntMatrix} with values converted from float to int
##### toLongMatrix(...) -> LongMatrix
- **Signature:** `public LongMatrix toLongMatrix()`
- **Summary:** Converts this float matrix to a long matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code LongMatrix} with values converted from float to long
##### zipWith(...) -> FloatMatrix
- **Signature:** `public <E extends Exception> FloatMatrix zipWith(final FloatMatrix other, final Throwables.FloatBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Performs element-wise operation on two matrices using the provided binary operator.
- **Contract:**
  - The matrices must have the same dimensions.
- **Parameters:**
  - `other` (`FloatMatrix`) — the second matrix; must not be null
  - `zipFunction` (`Throwables.FloatBinaryOperator<E>`) — the binary operator to apply element-wise; must not be null
- **Returns:** a new FloatMatrix with the results of the element-wise operation
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} or {@code zipFunction} is {@code null} , or the matrices have different dimensions
  - `E` — if the zip function throws an exception
- **Signature:** `public <E extends Exception> FloatMatrix zipWith(final FloatMatrix other, final FloatMatrix third, final Throwables.FloatTernaryOperator<E> zipFunction) throws E`
- **Summary:** Performs element-wise operation on three matrices using the provided ternary operator.
- **Contract:**
  - All matrices must have the same dimensions.
- **Parameters:**
  - `other` (`FloatMatrix`) — the second matrix; must not be null
  - `third` (`FloatMatrix`) — the third matrix; must not be null
  - `zipFunction` (`Throwables.FloatTernaryOperator<E>`) — the ternary operator to apply element-wise; must not be null
- **Returns:** a new FloatMatrix with the results of the element-wise operation
- **Throws:**
  - `E` — if the zip function throws an exception
##### mainDiagonalStream(...) -> FloatStream
- **Signature:** `@Override public FloatStream mainDiagonalStream()`
- **Summary:** Returns a stream of elements on the diagonal from upper-left to lower-right.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a FloatStream containing the diagonal elements from upper-left to lower-right
##### antiDiagonalStream(...) -> FloatStream
- **Signature:** `@Override public FloatStream antiDiagonalStream()`
- **Summary:** Returns a stream of elements on the anti-diagonal from upper-right to lower-left.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a FloatStream containing the anti-diagonal elements from upper-right to lower-left
##### horizontalStream(...) -> FloatStream
- **Signature:** `@Override public FloatStream horizontalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
- **Parameters:**
  - (none)
- **Returns:** a FloatStream containing all matrix elements traversed horizontally (left to right, top to bottom)
- **Signature:** `@Override public FloatStream horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a single row.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
- **Returns:** a FloatStream of elements from the specified row
- **Signature:** `@Override public FloatStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of rows in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a FloatStream of elements from the specified row range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### verticalStream(...) -> FloatStream
- **Signature:** `@Override @Beta public FloatStream verticalStream()`
- **Summary:** Returns a stream of all elements in the matrix, traversed vertically (column by column).
- **Parameters:**
  - (none)
- **Returns:** a FloatStream containing all matrix elements in column-major order
- **Signature:** `@Override public FloatStream verticalStream(final int columnIndex)`
- **Summary:** Returns a stream of elements from a single column.
- **Parameters:**
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** a FloatStream of elements from the specified column
- **Signature:** `@Override @Beta public FloatStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of columns in column-major order.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a FloatStream of elements from the specified column range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### rowStreams(...) -> Stream<FloatStream>
- **Signature:** `@Override public Stream<FloatStream> rowStreams()`
- **Summary:** Returns a stream where each element is a FloatStream representing a row.
- **Parameters:**
  - (none)
- **Returns:** a Stream of FloatStream, one for each row
- **Signature:** `@Override public Stream<FloatStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of FloatStream for a range of rows.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a Stream of FloatStream for the specified row range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### columnStreams(...) -> Stream<FloatStream>
- **Signature:** `@Override @Beta public Stream<FloatStream> columnStreams()`
- **Summary:** Returns a stream where each element is a FloatStream representing a column.
- **Parameters:**
  - (none)
- **Returns:** a Stream of FloatStream, one for each column
- **Signature:** `@Override @Beta public Stream<FloatStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of FloatStream for a range of columns.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a Stream of FloatStream for the specified column range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### forEach(...) -> void
- **Signature:** `public <E extends Exception> void forEach(final Throwables.FloatConsumer<E> action) throws E`
- **Summary:** Performs the specified action for each element in this matrix.
- **Contract:**
  - <p> When executed sequentially, the action is performed on all elements in row-major order (left to right, top to bottom).
- **Parameters:**
  - `action` (`Throwables.FloatConsumer<E>`) — the action to perform on each element; must not be {@code null}
- **Throws:**
  - `E` — if the action throws an exception
- **Signature:** `public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.FloatConsumer<E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element in a sub-region of this matrix.
- **Contract:**
  - <p> When executed sequentially, the action is performed on elements within the specified row and column ranges in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.FloatConsumer<E>`) — the action to perform on each element; must not be {@code null}
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if {@code fromRowIndex < 0} , {@code toRowIndex > rowCount} , {@code fromRowIndex > toRowIndex} , or the analogous condition for the column range
  - `E` — if the action throws an exception
##### println(...) -> String
- **Signature:** `@Override public String println()`
- **Summary:** Prints this matrix to standard output and returns the formatted string.
- **Parameters:**
  - (none)
- **Returns:** the formatted string representation of the matrix that was printed
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this matrix.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this matrix
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this matrix to the specified object for equality.
- **Contract:**
  - Returns {@code true} if the given object is also a {@code FloatMatrix} with the same dimensions (same {@code rowCount} and {@code columnCount} ) and all corresponding elements are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with (may be {@code null} )
- **Returns:** {@code true} if {@code obj} is a {@code FloatMatrix} of the same shape and content, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this matrix

### Class IntMatrix (com.landawn.abacus.matrix.IntMatrix)
Matrix implementation backed by a rectangular {@code int\[\]\[\]} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### empty(...) -> IntMatrix
- **Signature:** `public static IntMatrix empty()`
- **Summary:** Returns the shared empty {@code 0x0} matrix instance.
- **Parameters:**
  - (none)
- **Returns:** the canonical empty {@code IntMatrix} (singleton)
##### of(...) -> IntMatrix
- **Signature:** `public static IntMatrix of(final int[]... a)`
- **Summary:** Creates an {@code IntMatrix} from a two-dimensional int array.
- **Parameters:**
  - `a` (`int[][]`) — the two-dimensional int array to wrap, or {@code null} /empty for an empty matrix
- **Returns:** a new {@code IntMatrix} backed by {@code a} , or the shared empty matrix if {@code a} is {@code null} or empty
##### from(...) -> IntMatrix
- **Signature:** `public static IntMatrix from(final char[]... a)`
- **Summary:** Creates an {@code IntMatrix} from a two-dimensional {@code char} array by widening each {@code char} to its unsigned 16-bit numeric value (the same value as a Java {@code char} -to- {@code int} widening).
- **Contract:**
  - <p> All rows must have the same length as the first row (rectangular array required).
- **Parameters:**
  - `a` (`char[][]`) — the two-dimensional char array to convert, or {@code null} /empty for an empty matrix
- **Returns:** a new {@code IntMatrix} with the widened values, or the shared empty matrix if {@code a} is {@code null} or empty
- **Signature:** `public static IntMatrix from(final byte[]... a)`
- **Summary:** Creates an {@code IntMatrix} from a two-dimensional {@code byte} array by sign-extending each {@code byte} to {@code int} (negative bytes therefore yield negative ints).
- **Contract:**
  - <p> All rows must have the same length as the first row (rectangular array required).
- **Parameters:**
  - `a` (`byte[][]`) — the two-dimensional byte array to convert, or {@code null} /empty for an empty matrix
- **Returns:** a new {@code IntMatrix} with the widened values, or the shared empty matrix if {@code a} is {@code null} or empty
- **Signature:** `public static IntMatrix from(final short[]... a)`
- **Summary:** Creates an {@code IntMatrix} from a two-dimensional {@code short} array by sign-extending each {@code short} to {@code int} (negative shorts therefore yield negative ints).
- **Contract:**
  - <p> All rows must have the same length as the first row (rectangular array required).
- **Parameters:**
  - `a` (`short[][]`) — the two-dimensional short array to convert, or {@code null} /empty for an empty matrix
- **Returns:** a new {@code IntMatrix} with the widened values, or the shared empty matrix if {@code a} is {@code null} or empty
##### random(...) -> IntMatrix
- **Signature:** `public static IntMatrix random(final int length)`
- **Summary:** Creates a new {@code 1 x length} matrix filled with pseudo-random {@code int} values drawn uniformly from the entire {@code int} range.
- **Parameters:**
  - `length` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new {@code IntMatrix} of dimensions {@code 1 x length} filled with random values
- **Signature:** `public static IntMatrix random(final int rowCount, final int columnCount)`
- **Summary:** Creates a new matrix of the specified dimensions filled with pseudo-random {@code int} values drawn uniformly from the entire {@code int} range.
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new {@code IntMatrix} of dimensions {@code rowCount x columnCount} filled with random values
##### repeat(...) -> IntMatrix
- **Signature:** `public static IntMatrix repeat(final int rowCount, final int columnCount, final int element)`
- **Summary:** Creates a new matrix of the specified dimensions where every cell holds {@code element} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
  - `element` (`int`) — the int value to fill every cell with
- **Returns:** a new {@code IntMatrix} of dimensions {@code rowCount x columnCount} filled with {@code element}
##### range(...) -> IntMatrix
- **Signature:** `public static IntMatrix range(final int startInclusive, final int endExclusive)`
- **Summary:** Creates a 1-row {@code IntMatrix} containing the half-open range {@code \[startInclusive, endExclusive)} with step {@code 1} .
- **Contract:**
  - If {@code startInclusive >= endExclusive} , a {@code 1x0} matrix is returned.
- **Parameters:**
  - `startInclusive` (`int`) — the starting value (inclusive)
  - `endExclusive` (`int`) — the ending value (exclusive)
- **Returns:** a new {@code 1xn} {@code IntMatrix} where {@code n = max(0, endExclusive - startInclusive)}
- **Signature:** `public static IntMatrix range(final int startInclusive, final int endExclusive, final int step)`
- **Summary:** Creates a 1-row {@code IntMatrix} containing the half-open range {@code \[startInclusive, endExclusive)} stepped by {@code step} .
- **Contract:**
  - If the step direction does not advance from {@code startInclusive} toward {@code endExclusive} , a {@code 1x0} matrix is returned.
- **Parameters:**
  - `startInclusive` (`int`) — the starting value (inclusive)
  - `endExclusive` (`int`) — the ending value (exclusive)
  - `step` (`int`) — the step size (must not be zero; positive for ascending, negative for descending)
- **Returns:** a new {@code 1xn} {@code IntMatrix} of values from {@code startInclusive} stepped by {@code step}
##### rangeClosed(...) -> IntMatrix
- **Signature:** `public static IntMatrix rangeClosed(final int startInclusive, final int endInclusive)`
- **Summary:** Creates a 1-row {@code IntMatrix} containing the closed range {@code \[startInclusive, endInclusive\]} with step {@code 1} .
- **Contract:**
  - If {@code startInclusive > endInclusive} , a {@code 1x0} matrix is returned.
- **Parameters:**
  - `startInclusive` (`int`) — the starting value (inclusive)
  - `endInclusive` (`int`) — the ending value (inclusive)
- **Returns:** a new {@code 1xn} {@code IntMatrix} where {@code n = max(0, endInclusive - startInclusive + 1)}
- **Signature:** `public static IntMatrix rangeClosed(final int startInclusive, final int endInclusive, final int step)`
- **Summary:** Creates a 1-row {@code IntMatrix} containing the closed range {@code \[startInclusive, endInclusive\]} stepped by {@code step} .
- **Contract:**
  - {@code endInclusive} is included only if it is reachable from {@code startInclusive} via {@code step} ; otherwise the largest reachable value below it is the last element.
  - If the step direction does not advance toward {@code endInclusive} , a {@code 1x0} matrix is returned.
- **Parameters:**
  - `startInclusive` (`int`) — the starting value (inclusive)
  - `endInclusive` (`int`) — the ending value (inclusive, if reachable by stepping)
  - `step` (`int`) — the step size (must not be zero; positive for ascending, negative for descending)
- **Returns:** a new {@code 1xn} {@code IntMatrix} of values from {@code startInclusive} stepped by {@code step}
##### mainDiagonal(...) -> IntMatrix
- **Signature:** `public static IntMatrix mainDiagonal(final int[] mainDiagonal)`
- **Summary:** Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
- **Parameters:**
  - `mainDiagonal` (`int[]`) — the array of main-diagonal elements; may be {@code null} or empty
- **Returns:** a new {@code n x n} {@code IntMatrix} (where {@code n = mainDiagonal.length} ) with the supplied values on the main diagonal and {@code 0} elsewhere; the shared empty matrix if {@code mainDiagonal} is {@code null} or empty
- **See also:** #antiDiagonal(int\[\]), #diagonals(int\[\], int\[\])
##### antiDiagonal(...) -> IntMatrix
- **Signature:** `public static IntMatrix antiDiagonal(final int[] antiDiagonal)`
- **Summary:** Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
- **Parameters:**
  - `antiDiagonal` (`int[]`) — the array of anti-diagonal elements; may be {@code null} or empty
- **Returns:** a new {@code n x n} {@code IntMatrix} (where {@code n = antiDiagonal.length} ) with the supplied values on the anti-diagonal and {@code 0} elsewhere; the shared empty matrix if {@code antiDiagonal} is {@code null} or empty
- **See also:** #mainDiagonal(int\[\]), #diagonals(int\[\], int\[\])
##### diagonals(...) -> IntMatrix
- **Signature:** `public static IntMatrix diagonals(final int[] mainDiagonal, final int[] antiDiagonal) throws IllegalArgumentException`
- **Summary:** Creates a square matrix from the specified main diagonal and anti-diagonal elements.
- **Contract:**
  - If both arrays are provided, they must have the same length.
  - When both diagonals are provided and they overlap (at the center element of odd-sized matrices), the main diagonal value takes precedence.
- **Parameters:**
  - `mainDiagonal` (`int[]`) — the array of main diagonal elements (can be null or empty)
  - `antiDiagonal` (`int[]`) — the array of anti-diagonal elements (can be null or empty)
- **Returns:** a square matrix with the specified diagonals, or an empty matrix if both inputs are null or empty
- **Throws:**
  - `java.lang.IllegalArgumentException` — if both arrays are non-empty and have different lengths
##### unbox(...) -> IntMatrix
- **Signature:** `public static IntMatrix unbox(final Matrix<Integer> x)`
- **Summary:** Converts a boxed {@link Matrix Matrix&lt;Integer&gt;} to a primitive {@code IntMatrix} .
- **Parameters:**
  - `x` (`Matrix<Integer>`) — the boxed {@code Integer} matrix to convert; must not be {@code null}
- **Returns:** a new {@code IntMatrix} with primitive int values
- **See also:** #boxed()

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public IntMatrix(final int[][] a)`
- **Summary:** Constructs an {@code IntMatrix} backed by the supplied two-dimensional array.
- **Contract:**
  - <p> If {@code a} is {@code null} , this creates an empty {@code 0x0} matrix.
  - Call {@link #copy()} if you need an independently owned matrix.
- **Parameters:**
  - `a` (`int[][]`) — the two-dimensional int array to wrap, or {@code null} for an empty matrix
##### get(...) -> int
- **Signature:** `public int get(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** the element at position (rowIndex, columnIndex)
- **Signature:** `public int get(final Point point)`
- **Summary:** Returns the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
- **Returns:** the int element at the specified point
- **See also:** #get(int, int)
##### set(...) -> void
- **Signature:** `public void set(final int rowIndex, final int columnIndex, final int value)`
- **Summary:** Sets the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
  - `value` (`int`) — the value to set
- **Signature:** `public void set(final Point point, final int value)`
- **Summary:** Sets the element at the specified point to the given value.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
  - `value` (`int`) — the new int value to set at the specified point
- **See also:** #set(int, int, int)
##### valueAbove(...) -> OptionalInt
- **Signature:** `public OptionalInt valueAbove(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly above the specified position, if it exists.
- **Contract:**
  - Returns the element directly above the specified position, if it exists.
  - This method provides safe access without throwing an exception when at the top edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@link OptionalInt} containing the element at position {@code (rowIndex - 1, columnIndex)} , or empty if {@code rowIndex == 0}
##### valueBelow(...) -> OptionalInt
- **Signature:** `public OptionalInt valueBelow(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly below the specified position, if it exists.
- **Contract:**
  - Returns the element directly below the specified position, if it exists.
  - This method provides safe access without throwing an exception when at the bottom edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@link OptionalInt} containing the element at position {@code (rowIndex + 1, columnIndex)} , or empty if {@code rowIndex == rowCount - 1}
##### valueLeft(...) -> OptionalInt
- **Signature:** `public OptionalInt valueLeft(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the left of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the left of the specified position, if it exists.
  - This method provides safe access without throwing an exception when at the leftmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@link OptionalInt} containing the element at position {@code (rowIndex, columnIndex - 1)} , or empty if {@code columnIndex == 0}
##### valueRight(...) -> OptionalInt
- **Signature:** `public OptionalInt valueRight(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the right of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the right of the specified position, if it exists.
  - This method provides safe access without throwing an exception when at the rightmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an {@link OptionalInt} containing the element at position {@code (rowIndex, columnIndex + 1)} , or empty if {@code columnIndex == columnCount - 1}
##### rowView(...) -> int\[\]
- **Signature:** `@Override public int[] rowView(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as a live reference to the underlying {@code int\[\]} storage.
- **Contract:**
  - Use {@link #rowCopy(int)} if you need an independent copy.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the specified row as a direct reference to internal storage
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
- **See also:** #rowCopy(int)
##### rowCopy(...) -> int\[\]
- **Signature:** `@Override public int[] rowCopy(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified row as a new {@code int\[\]} .
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new int array of length {@code columnCount} containing the values of the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowIndex < 0} or {@code rowIndex >= rowCount}
- **See also:** #rowView(int), #columnCopy(int)
##### columnCopy(...) -> int\[\]
- **Signature:** `@Override public int[] columnCopy(final int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a copy of the specified column as a new int array.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new int array of length {@code rowCount} containing the values of the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code columnIndex < 0} or {@code columnIndex >= columnCount}
- **See also:** #rowCopy(int)
##### setRow(...) -> void
- **Signature:** `public void setRow(final int rowIndex, final int[] row) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified row by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of columns in the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to set (0-based)
  - `row` (`int[]`) — the array of values to copy into the row; must be non- {@code null} and of length {@code columnCount}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowIndex} is out of bounds, or {@code row.length != columnCount}
##### setColumn(...) -> void
- **Signature:** `public void setColumn(final int columnIndex, final int[] column) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified column by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of rows in the matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to set (0-based)
  - `column` (`int[]`) — the array of values to copy into the column; must be non- {@code null} and of length {@code rowCount}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code columnIndex} is out of bounds, or {@code column.length != rowCount}
##### updateRow(...) -> void
- **Signature:** `public <E extends Exception> void updateRow(final int rowIndex, final Throwables.IntUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in a row in-place by applying the specified operator to each element.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to update (0-based)
  - `operator` (`Throwables.IntUnaryOperator<E>`) — the operator to apply to each element in the row; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### updateColumn(...) -> void
- **Signature:** `public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.IntUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified column in-place by applying the specified operator to each element.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to update (0-based)
  - `operator` (`Throwables.IntUnaryOperator<E>`) — the operator to apply to each element in the column; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### getMainDiagonal(...) -> int\[\]
- **Signature:** `@Override public int[] getMainDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the main diagonal elements (upper-left to lower-right) as an array.
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new int array containing a copy of the main diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setMainDiagonal(...) -> void
- **Signature:** `@Override public void setMainDiagonal(final int[] mainDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `mainDiagonal` (`int[]`) — the new values for the main diagonal; must be non- {@code null} and of length {@code rowCount}
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if {@code mainDiagonal} is {@code null} or its length is not equal to {@code rowCount}
##### updateMainDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateMainDiagonal(final Throwables.IntUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the main diagonal (upper-left to lower-right) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.IntUnaryOperator<E>`) — the operator to apply to each diagonal element; receives current element value and returns new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### getAntiDiagonal(...) -> int\[\]
- **Signature:** `@Override public int[] getAntiDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the anti-diagonal elements (upper-right to lower-left) as an array.
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new int array containing a copy of the anti-diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setAntiDiagonal(...) -> void
- **Signature:** `@Override public void setAntiDiagonal(final int[] antiDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `antiDiagonal` (`int[]`) — the new values for the anti-diagonal; must be non- {@code null} and of length {@code rowCount}
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if {@code antiDiagonal} is {@code null} or its length is not equal to {@code rowCount}
##### updateAntiDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateAntiDiagonal(final Throwables.IntUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the anti-diagonal (upper-right to lower-left) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.IntUnaryOperator<E>`) — the operator to apply to each anti-diagonal element; receives current element value and returns new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### updateAll(...) -> void
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the matrix in-place by applying the specified operator.
- **Contract:**
  - Elements are processed in row-major order when executed sequentially.
- **Parameters:**
  - `operator` (`Throwables.IntUnaryOperator<E>`) — the operator to apply to each element; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Integer, E> mapper) throws E`
- **Summary:** Updates all elements in the matrix in-place based on their position (row and column indices).
- **Parameters:**
  - `mapper` (`Throwables.IntBiFunction<? extends Integer, E>`) — the function that receives row index and column index (0-based) and returns the new value for that position; the returned {@code Integer} is unboxed, so it must not be {@code null}
- **Throws:**
  - `E` — if the mapper throws an exception
##### replaceIf(...) -> void
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntPredicate<E> predicate, final int newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on a predicate.
- **Parameters:**
  - `predicate` (`Throwables.IntPredicate<E>`) — the condition to test each element; elements for which this returns {@code true} will be replaced
  - `newValue` (`int`) — the value to use for replacing matching elements
- **Throws:**
  - `E` — if the predicate throws an exception
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final int newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on their position (row and column indices).
- **Parameters:**
  - `predicate` (`Throwables.IntBiPredicate<E>`) — the condition that tests row index and column index (0-based); elements at positions for which this returns {@code true} will be replaced
  - `newValue` (`int`) — the value to use for replacing matching elements
- **Throws:**
  - `E` — if the predicate throws an exception
##### map(...) -> IntMatrix
- **Signature:** `public <E extends Exception> IntMatrix map(final Throwables.IntUnaryOperator<E> mapper) throws E`
- **Summary:** Creates a new IntMatrix by applying a transformation function to each element.
- **Parameters:**
  - `mapper` (`Throwables.IntUnaryOperator<E>`) — the function to apply to each element; receives the current element value and returns the transformed value
- **Returns:** a new IntMatrix with transformed values
- **Throws:**
  - `E` — if the function throws an exception
- **See also:** #updateAll(Throwables.IntUnaryOperator)
##### mapToLong(...) -> LongMatrix
- **Signature:** `public <E extends Exception> LongMatrix mapToLong(final Throwables.IntToLongFunction<E> mapper) throws E`
- **Summary:** Creates a new LongMatrix by applying a function that converts int values to long.
- **Parameters:**
  - `mapper` (`Throwables.IntToLongFunction<E>`) — the function to convert int values to long
- **Returns:** a new {@link LongMatrix} with the converted values
- **Throws:**
  - `E` — if the function throws an exception
- **See also:** #toLongMatrix()
##### mapToDouble(...) -> DoubleMatrix
- **Signature:** `public <E extends Exception> DoubleMatrix mapToDouble(final Throwables.IntToDoubleFunction<E> mapper) throws E`
- **Summary:** Creates a new DoubleMatrix by applying a function that converts int values to double.
- **Parameters:**
  - `mapper` (`Throwables.IntToDoubleFunction<E>`) — the function to convert int values to double
- **Returns:** a new {@link DoubleMatrix} with the converted values
- **Throws:**
  - `E` — if the function throws an exception
- **See also:** #toDoubleMatrix()
##### mapToObj(...) -> Matrix<R>
- **Signature:** `public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.IntFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E`
- **Summary:** Creates a new Matrix by applying a function that converts int values to objects of type R.
- **Parameters:**
  - `mapper` (`Throwables.IntFunction<? extends R, E>`) — the function to convert int values to type {@code R}
  - `targetElementType` (`Class<R>`) — the {@code Class} object for type {@code R} (used to allocate the {@code R\[\]\[\]} backing array); must not be {@code null}
- **Returns:** a new {@link Matrix Matrix&lt;R&gt;} containing the mapped values
- **Throws:**
  - `E` — if the function throws an exception
##### fill(...) -> void
- **Signature:** `public void fill(final int value)`
- **Summary:** Fills all elements of the matrix with the specified value.
- **Parameters:**
  - `value` (`int`) — the value to fill the matrix with
- **Signature:** `public void fill(final int[][] source)`
- **Summary:** Fills this matrix with values from another two-dimensional array, starting at position {@code (0, 0)} .
- **Contract:**
  - If the source array is larger, only the portion that fits is copied.
- **Parameters:**
  - `source` (`int[][]`) — the two-dimensional array to copy values from; must not be {@code null}
- **See also:** #fill(int, int, int\[\]\[\])
- **Signature:** `public void fill(final int destRowIndex, final int destColumnIndex, final int[][] source) throws IllegalArgumentException`
- **Summary:** Fills a region of this matrix with values from another two-dimensional array, starting at the specified destination position.
- **Parameters:**
  - `destRowIndex` (`int`) — the target row index in this matrix (0-based, must satisfy {@code 0 <= destRowIndex <= rowCount} )
  - `destColumnIndex` (`int`) — the target column index in this matrix (0-based, must satisfy {@code 0 <= destColumnIndex <= columnCount} )
  - `source` (`int[][]`) — the source array to copy values from; must not be {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code source} is {@code null} , if {@code destRowIndex < 0} or {@code destRowIndex > rowCount} , or if {@code destColumnIndex < 0} or {@code destColumnIndex > columnCount}
##### copy(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a copy of this matrix with full independence guarantee
- **Signature:** `@Override public IntMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a row range from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new IntMatrix containing the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
- **Signature:** `@Override public IntMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a submatrix defined by row and column ranges.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new IntMatrix containing the specified submatrix
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### resize(...) -> IntMatrix
- **Signature:** `public IntMatrix resize(final int newRowCount, final int newColumnCount)`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code 0} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
- **Returns:** a new IntMatrix with the specified dimensions
- **See also:** #resize(int, int, int), #extend(int, int, int, int)
- **Signature:** `public IntMatrix resize(final int newRowCount, final int newColumnCount, final int defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded.
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code defaultValue} .
  - Use {@code extend} when the entire original content must be preserved.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code IntMatrix matrix = IntMatrix.of(new int\[\]\[\] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}); // Grow: fill new cells with 9 IntMatrix grown = matrix.resize(4, 4, 9); // Result: \[\[1, 2, 3, 9\], // \[4, 5, 6, 9\], // \[7, 8, 9, 9\], // \[9, 9, 9, 9\]\] // Truncate: defaultValue is ignored when shrinking IntMatrix truncated = matrix.resize(2, 2, 9); // Result: \[\[1, 2\], // \[4, 5\]\] } </pre>
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
  - `defaultValue` (`int`) — the value used to fill cells that are added when a dimension grows; ignored when a dimension shrinks
- **Returns:** a new IntMatrix with the specified dimensions
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code newRowCount} or {@code newColumnCount} is negative, or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
- **See also:** #resize(int, int), #extend(int, int, int, int, int)
##### extend(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight)`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
- **Returns:** a new IntMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **See also:** #extend(int, int, int, int, int), #resize(int, int)
- **Signature:** `public IntMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final int defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
  - `defaultValue` (`int`) — the value to fill all new padding cells with
- **Returns:** a new IntMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any padding parameter is negative, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** #extend(int, int, int, int), #resize(int, int, int)
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `@Override public void flipHorizontallyInPlace()`
- **Summary:** Reverses the order of elements in each row in-place (horizontal flip).
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally(), #flipVerticallyInPlace()
##### flipVerticallyInPlace(...) -> void
- **Signature:** `@Override public void flipVerticallyInPlace()`
- **Summary:** Reverses the order of rows in-place (vertical flip).
- **Parameters:**
  - (none)
- **See also:** #flipVertically(), #flipHorizontallyInPlace()
##### flipHorizontally(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix flipHorizontally()`
- **Summary:** Returns a new matrix that is a horizontal flip of this matrix (columns in reversed order).
- **Parameters:**
  - (none)
- **Returns:** a new IntMatrix with each row reversed
- **See also:** #flipHorizontallyInPlace(), #flipVertically(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### flipVertically(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix flipVertically()`
- **Summary:** Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
- **Parameters:**
  - (none)
- **Returns:** a new IntMatrix with rows reversed
- **See also:** #flipVerticallyInPlace(), #flipHorizontally(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### rotate90(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 90 degrees clockwise
##### rotate180(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 180 degrees
##### rotate270(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 270 degrees clockwise
##### transpose(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix transpose()`
- **Summary:** Returns a new matrix that is the transpose of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code IntMatrix} of shape {@code columnCount x rowCount} that is the transpose of this matrix
##### reshape(...) -> IntMatrix
- **Signature:** `@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG") @Override public IntMatrix reshape(final int newRowCount, final int newColumnCount)`
- **Summary:** Reshapes this matrix to have the specified dimensions.
- **Contract:**
  - The new shape must have at least as many total cells as the original ( {@code (long) newRowCount * newColumnCount >= elementCount()} ).
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix; must be {@code >= 0}
- **Returns:** a new {@code IntMatrix} with the specified dimensions
##### repeatElements(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats elements in both row and column directions.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat each element in row direction
  - `columnRepeats` (`int`) — number of times to repeat each element in column direction
- **Returns:** a new IntMatrix with repeated elements
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowRepeats or columnRepeats is not positive, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">,MATLAB repelem function,</a>
##### repeatMatrix(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats the entire matrix in a tiled pattern.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat the matrix vertically
  - `columnRepeats` (`int`) — number of times to repeat the matrix horizontally
- **Returns:** a new IntMatrix with the tiled pattern
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowRepeats or columnRepeats is not positive, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">,MATLAB repmat function,</a>
##### flatten(...) -> IntList
- **Signature:** `@Override public IntList flatten()`
- **Summary:** Returns a new {@link IntList} containing all elements of this matrix in row-major order.
- **Parameters:**
  - (none)
- **Returns:** a new {@link IntList} of all elements in row-major order
##### mutateAsFlat(...) -> void
- **Signature:** `@Override public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super int[], E> action) throws E`
- **Summary:** Exposes the elements of this matrix to {@code action} as a single one-dimensional array laid out in row-major order, then propagates any modifications back into the matrix.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super int[], E>`) — the operation to apply to the flattened array
- **Throws:**
  - `E` — if the operation throws an exception
- **See also:** Arrays#mutateAsFlat(int\[\]\[\], Throwables.Consumer)
##### stackVertically(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix stackVertically(final IntMatrix other) throws IllegalArgumentException`
- **Summary:** Stacks this matrix vertically with another matrix (vertical concatenation).
- **Contract:**
  - The matrices must have the same number of columns.
- **Parameters:**
  - `other` (`IntMatrix`) — the matrix to stack below this matrix (must have the same column count)
- **Returns:** a new IntMatrix with dimensions (this.rowCount + other.rowCount) x this.columnCount
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , if {@code this.columnCount != other.columnCount} , or if the merged row count would exceed {@code Integer.MAX_VALUE}
- **See also:** #stackHorizontally(IntMatrix)
##### stackHorizontally(...) -> IntMatrix
- **Signature:** `@Override public IntMatrix stackHorizontally(final IntMatrix other) throws IllegalArgumentException`
- **Summary:** Stacks this matrix horizontally with another matrix (horizontal concatenation).
- **Contract:**
  - The matrices must have the same number of rows.
- **Parameters:**
  - `other` (`IntMatrix`) — the matrix to stack to the right of this matrix (must have the same row count)
- **Returns:** a new IntMatrix with dimensions this.rowCount x (this.columnCount + other.columnCount)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , if {@code this.rowCount != other.rowCount} , or if the merged column count would exceed {@code Integer.MAX_VALUE}
- **See also:** #stackVertically(IntMatrix)
##### add(...) -> IntMatrix
- **Signature:** `public IntMatrix add(final IntMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise addition with another matrix.
- **Contract:**
  - The matrices must have the same dimensions.
  - If you need a wider result, call {@link #toLongMatrix()} first or use {@link #mapToLong(Throwables.IntToLongFunction)} .
- **Parameters:**
  - `other` (`IntMatrix`) — the matrix to add to this matrix; must not be {@code null} and must have the same shape
- **Returns:** a new {@code IntMatrix} containing the element-wise sum
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , or if the matrices have different shapes
- **See also:** #subtract(IntMatrix), #zipWith(IntMatrix, Throwables.IntBinaryOperator)
##### subtract(...) -> IntMatrix
- **Signature:** `public IntMatrix subtract(final IntMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise subtraction ( {@code this - other} ).
- **Contract:**
  - The matrices must have the same dimensions.
- **Parameters:**
  - `other` (`IntMatrix`) — the matrix to subtract from this matrix; must not be {@code null} and must have the same shape
- **Returns:** a new {@code IntMatrix} containing the element-wise difference {@code this - other}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , or if the matrices have different shapes
- **See also:** #add(IntMatrix)
##### matmul(...) -> IntMatrix
- **Signature:** `public IntMatrix matmul(final IntMatrix other) throws IllegalArgumentException`
- **Summary:** Performs matrix multiplication (Cayley product) with another matrix.
- **Contract:**
  - The number of columns in this matrix must equal the number of rows in {@code other} .
- **Parameters:**
  - `other` (`IntMatrix`) — the matrix to multiply with; must not be {@code null}
- **Returns:** a new {@code IntMatrix} of shape {@code this.rowCount x other.columnCount} containing the matrix product
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , or if {@code this.columnCount != other.rowCount}
##### boxed(...) -> Matrix<Integer>
- **Signature:** `public Matrix<Integer> boxed()`
- **Summary:** Converts this primitive int matrix to a boxed {@link Matrix Matrix&lt;Integer&gt;} .
- **Parameters:**
  - (none)
- **Returns:** a new {@link Matrix Matrix&lt;Integer&gt;} containing the same values as boxed {@code Integer} instances
- **See also:** #unbox(Matrix)
##### toLongMatrix(...) -> LongMatrix
- **Signature:** `public LongMatrix toLongMatrix()`
- **Summary:** Converts this {@code int} matrix to a {@link LongMatrix} .
- **Parameters:**
  - (none)
- **Returns:** a new {@link LongMatrix} with the widened values
- **See also:** #mapToLong(Throwables.IntToLongFunction)
##### toFloatMatrix(...) -> FloatMatrix
- **Signature:** `public FloatMatrix toFloatMatrix()`
- **Summary:** Converts this {@code int} matrix to a {@link FloatMatrix} .
- **Parameters:**
  - (none)
- **Returns:** a new {@link FloatMatrix} with the converted values
##### toDoubleMatrix(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix toDoubleMatrix()`
- **Summary:** Converts this {@code int} matrix to a {@link DoubleMatrix} .
- **Parameters:**
  - (none)
- **Returns:** a new {@link DoubleMatrix} with the widened values
- **See also:** #mapToDouble(Throwables.IntToDoubleFunction)
##### zipWith(...) -> IntMatrix
- **Signature:** `public <E extends Exception> IntMatrix zipWith(final IntMatrix other, final Throwables.IntBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Performs element-wise operation on two matrices using a binary operator.
- **Contract:**
  - The matrices must have the same dimensions.
- **Parameters:**
  - `other` (`IntMatrix`) — the second matrix (must have the same dimensions as this matrix)
  - `zipFunction` (`Throwables.IntBinaryOperator<E>`) — the binary operator to apply to corresponding elements; receives the element from this matrix as first argument and the element from {@code other} as second argument
- **Returns:** a new {@code IntMatrix} with the results of the element-wise operation
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} or {@code zipFunction} is {@code null} , or if the matrices have different shapes
  - `E` — if the zip function throws an exception
- **See also:** #zipWith(IntMatrix, IntMatrix, Throwables.IntTernaryOperator)
- **Signature:** `public <E extends Exception> IntMatrix zipWith(final IntMatrix other, final IntMatrix third, final Throwables.IntTernaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Performs element-wise operation on three matrices using a ternary operator.
- **Contract:**
  - All matrices must have the same dimensions.
- **Parameters:**
  - `other` (`IntMatrix`) — the second matrix (must have the same dimensions as this matrix)
  - `third` (`IntMatrix`) — the third matrix (must have the same dimensions as this matrix)
  - `zipFunction` (`Throwables.IntTernaryOperator<E>`) — the ternary operator to apply to corresponding elements; receives the element from this matrix as first argument, the element from {@code other} as second argument, and the element from {@code third} as third argument
- **Returns:** a new {@code IntMatrix} with the results of the element-wise operation
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any of {@code other} , {@code third} , or {@code zipFunction} is {@code null} , or if any of the matrices have different shapes
  - `E` — if the zip function throws an exception
- **See also:** #zipWith(IntMatrix, Throwables.IntBinaryOperator)
##### mainDiagonalStream(...) -> IntStream
- **Signature:** `@Override public IntStream mainDiagonalStream()`
- **Summary:** Returns a stream of elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - (none)
- **Returns:** an IntStream of main-diagonal elements, or an empty stream if the matrix is empty
##### antiDiagonalStream(...) -> IntStream
- **Signature:** `@Override public IntStream antiDiagonalStream()`
- **Summary:** Returns a stream of elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - (none)
- **Returns:** an IntStream of anti-diagonal elements, or an empty stream if the matrix is empty
##### horizontalStream(...) -> IntStream
- **Signature:** `@Override public IntStream horizontalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
- **Parameters:**
  - (none)
- **Returns:** an IntStream of all elements in row-major order, or an empty stream if the matrix is empty
- **Signature:** `@Override public IntStream horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a single row.
- **Contract:**
  - <p> This method is particularly useful when you need to process or analyze a specific row of the matrix independently.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to stream (0-based)
- **Returns:** an {@link IntStream} of elements from the specified row
- **Signature:** `@Override public IntStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of rows in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** an IntStream of elements from the specified row range, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
##### verticalStream(...) -> IntStream
- **Signature:** `@Override @Beta public IntStream verticalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed vertically (top to bottom, left to right).
- **Parameters:**
  - (none)
- **Returns:** an IntStream of all elements in column-major order, or an empty stream if the matrix is empty
- **Signature:** `@Override public IntStream verticalStream(final int columnIndex)`
- **Summary:** Returns a stream of elements from a single column.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to stream (0-based)
- **Returns:** an {@link IntStream} of elements from the specified column
- **Signature:** `@Override @Beta public IntStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of columns in column-major order.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** an IntStream of elements from the specified column range in column-major order, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
##### rowStreams(...) -> Stream<IntStream>
- **Signature:** `@Override public Stream<IntStream> rowStreams()`
- **Summary:** Returns a stream of IntStream objects, where each IntStream represents a complete row.
- **Parameters:**
  - (none)
- **Returns:** a Stream of IntStream objects, one for each row in the matrix
- **Signature:** `@Override public Stream<IntStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of IntStream objects for a range of rows.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a Stream of IntStream objects for the specified row range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
##### columnStreams(...) -> Stream<IntStream>
- **Signature:** `@Override @Beta public Stream<IntStream> columnStreams()`
- **Summary:** Returns a stream of IntStream objects, where each IntStream represents a complete column.
- **Parameters:**
  - (none)
- **Returns:** a Stream of IntStream objects, one for each column in the matrix, or an empty stream if the matrix is empty
- **Signature:** `@Override @Beta public Stream<IntStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of IntStream objects for a range of columns.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a Stream of IntStream objects for the specified column range, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromColumnIndex &lt; 0, toColumnIndex &gt; columnCount, or fromColumnIndex &gt; toColumnIndex
##### forEach(...) -> void
- **Signature:** `public <E extends Exception> void forEach(final Throwables.IntConsumer<E> action) throws E`
- **Summary:** Performs the specified action for each element in this matrix.
- **Contract:**
  - Elements are processed in row-major order (row by row, left to right) when executed sequentially.
  - If parallelized, the order of execution is not guaranteed, but all elements will be processed exactly once.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code IntMatrix matrix = IntMatrix.of(new int\[\]\[\] {{1, 2}, {3, 4}}); // Collect all values List<Integer> values = new ArrayList<>(); matrix.forEach(value -> values.add(value)); // values now contains \[1, 2, 3, 4\] // Calculate sum using forEach (though horizontalStream().sum() is preferable) int\[\] sum = {0}; matrix.forEach(value -> sum\[0\] += value); // sum\[0\] is now 10 // Print all positive values matrix.forEach(value -> { if (value > 0) System.out.println(value); }); } </pre>
- **Parameters:**
  - `action` (`Throwables.IntConsumer<E>`) — the action to be performed for each element; receives each element value
- **Throws:**
  - `E` — if the action throws an exception
- **See also:** #forEach(int, int, int, int, Throwables.IntConsumer)
- **Signature:** `public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.IntConsumer<E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element in the specified sub-matrix region.
- **Contract:**
  - The operation may be parallelized internally if the sub-matrix is large enough to benefit from parallel processing.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.IntConsumer<E>`) — the action to be performed for each element in the sub-matrix
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds
  - `E` — if the action throws an exception
##### println(...) -> String
- **Signature:** `@Override public String println()`
- **Summary:** Prints this matrix to standard output and returns the formatted string that was printed.
- **Parameters:**
  - (none)
- **Returns:** the formatted multi-line string that was printed
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this matrix.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this matrix
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this matrix to the specified object for equality.
- **Contract:**
  - Returns {@code true} if the given object is also an {@code IntMatrix} with the same dimensions and all corresponding elements are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with; may be {@code null}
- **Returns:** {@code true} if {@code obj} is an {@code IntMatrix} with identical shape and elements, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this matrix

### Class LongMatrix (com.landawn.abacus.matrix.LongMatrix)
Matrix implementation backed by a rectangular {@code long\[\]\[\]} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### empty(...) -> LongMatrix
- **Signature:** `public static LongMatrix empty()`
- **Summary:** Returns a shared empty matrix with zero rows and zero columns.
- **Parameters:**
  - (none)
- **Returns:** a shared empty {@code LongMatrix} singleton
##### of(...) -> LongMatrix
- **Signature:** `public static LongMatrix of(final long[]... a)`
- **Summary:** Creates a {@code LongMatrix} from a two-dimensional long array.
- **Parameters:**
  - `a` (`long[][]`) — the two-dimensional long array to create the matrix from, or {@code null} /empty for an empty matrix
- **Returns:** a new {@code LongMatrix} wrapping the provided data, or the shared empty matrix if input is {@code null} or empty
##### from(...) -> LongMatrix
- **Signature:** `public static LongMatrix from(final int[]... a)`
- **Summary:** Creates a {@code LongMatrix} from a two-dimensional int array by widening each {@code int} to {@code long} .
- **Contract:**
  - <p> All rows must have the same length as the first row (rectangular array required).
  - The method validates array structure and throws an exception if the array is jagged (rows of different lengths).
- **Parameters:**
  - `a` (`int[][]`) — the two-dimensional int array to convert to a long matrix, or {@code null} /empty for an empty matrix
- **Returns:** a new {@code LongMatrix} with widened values, or the shared empty matrix if input is {@code null} or empty
##### random(...) -> LongMatrix
- **Signature:** `public static LongMatrix random(final int length)`
- **Summary:** Creates a new {@code 1 x length} matrix filled with random long values.
- **Parameters:**
  - `length` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new {@code LongMatrix} of dimensions {@code 1 x length} filled with random {@code long} values
- **Signature:** `public static LongMatrix random(final int rowCount, final int columnCount)`
- **Summary:** Creates a new matrix of the specified dimensions filled with random long values.
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new {@code LongMatrix} of dimensions {@code rowCount x columnCount} filled with random {@code long} values
##### repeat(...) -> LongMatrix
- **Signature:** `public static LongMatrix repeat(final int rowCount, final int columnCount, final long element)`
- **Summary:** Creates a new matrix of the specified dimensions where every element is the provided {@code element} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
  - `element` (`long`) — the long value to fill the matrix with
- **Returns:** a new {@code LongMatrix} of dimensions {@code rowCount x columnCount} filled with the specified element
##### range(...) -> LongMatrix
- **Signature:** `public static LongMatrix range(final long startInclusive, final long endExclusive)`
- **Summary:** Creates a single-row {@code LongMatrix} with sequential values from {@code startInclusive} to {@code endExclusive} .
- **Contract:**
  - If {@code startInclusive >= endExclusive} , a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`long`) — the starting value (inclusive)
  - `endExclusive` (`long`) — the ending value (exclusive)
- **Returns:** a new {@code 1×n} {@code LongMatrix} where {@code n = max(0, endExclusive - startInclusive)}
- **Signature:** `public static LongMatrix range(final long startInclusive, final long endExclusive, final long step)`
- **Summary:** Creates a single-row {@code LongMatrix} with values from {@code startInclusive} to {@code endExclusive} with the specified step.
- **Contract:**
  - If the step does not move from {@code startInclusive} toward {@code endExclusive} , a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`long`) — the starting value (inclusive)
  - `endExclusive` (`long`) — the ending value (exclusive)
  - `step` (`long`) — the step size (must not be zero; can be positive or negative)
- **Returns:** a new {@code 1×n} {@code LongMatrix} with values incremented by the step size
##### rangeClosed(...) -> LongMatrix
- **Signature:** `public static LongMatrix rangeClosed(final long startInclusive, final long endInclusive)`
- **Summary:** Creates a single-row {@code LongMatrix} with sequential values from {@code startInclusive} to {@code endInclusive} .
- **Contract:**
  - If {@code startInclusive > endInclusive} , a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`long`) — the starting value (inclusive)
  - `endInclusive` (`long`) — the ending value (inclusive)
- **Returns:** a new {@code 1×n} {@code LongMatrix} where {@code n = max(0, endInclusive - startInclusive + 1)}
- **Signature:** `public static LongMatrix rangeClosed(final long startInclusive, final long endInclusive, final long step)`
- **Summary:** Creates a single-row {@code LongMatrix} with values from {@code startInclusive} to {@code endInclusive} with the specified step.
- **Contract:**
  - The end value is included only if it is reachable by stepping from start.
  - If the step does not move from {@code startInclusive} toward {@code endInclusive} , a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`long`) — the starting value (inclusive)
  - `endInclusive` (`long`) — the ending value (inclusive, if reachable by stepping)
  - `step` (`long`) — the step size (must not be zero; can be positive or negative)
- **Returns:** a new {@code 1×n} {@code LongMatrix} with values incremented by the step size
##### mainDiagonal(...) -> LongMatrix
- **Signature:** `public static LongMatrix mainDiagonal(final long[] mainDiagonal)`
- **Summary:** Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
- **Parameters:**
  - `mainDiagonal` (`long[]`) — the array of main diagonal elements (from upper-left to lower-right); if {@code null} or empty, an empty matrix is returned
- **Returns:** a square {@code n×n} matrix with the specified main diagonal, where {@code n} is the array length, or the empty matrix if the input is {@code null} or empty
##### antiDiagonal(...) -> LongMatrix
- **Signature:** `public static LongMatrix antiDiagonal(final long[] antiDiagonal)`
- **Summary:** Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
- **Parameters:**
  - `antiDiagonal` (`long[]`) — the array of anti-diagonal elements (from upper-right to lower-left); if {@code null} or empty, an empty matrix is returned
- **Returns:** a square {@code n×n} matrix with the specified anti-diagonal, where {@code n} is the array length, or the empty matrix if the input is {@code null} or empty
##### diagonals(...) -> LongMatrix
- **Signature:** `public static LongMatrix diagonals(final long[] mainDiagonal, final long[] antiDiagonal) throws IllegalArgumentException`
- **Summary:** Creates a square matrix from the specified main diagonal and anti-diagonal elements.
- **Contract:**
  - If both arrays are non-empty, they must have the same length.
  - When both diagonals are provided and they overlap (at the center element of odd-sized matrices), the main diagonal value takes precedence (the main diagonal is written after the anti-diagonal).
- **Parameters:**
  - `mainDiagonal` (`long[]`) — the array of main diagonal elements (can be {@code null} or empty)
  - `antiDiagonal` (`long[]`) — the array of anti-diagonal elements (can be {@code null} or empty)
- **Returns:** a square matrix with the specified diagonals, or an empty matrix if both inputs are {@code null} or empty
- **Throws:**
  - `java.lang.IllegalArgumentException` — if both arrays are non-empty and have different lengths
##### unbox(...) -> LongMatrix
- **Signature:** `public static LongMatrix unbox(final Matrix<Long> x)`
- **Summary:** Converts a boxed {@code Matrix<Long>} to a primitive {@code LongMatrix} .
- **Contract:**
  - This is particularly beneficial when working with large matrices, as primitive arrays have less memory overhead and better cache locality than arrays of wrapper objects.
- **Parameters:**
  - `x` (`Matrix<Long>`) — the boxed Long matrix to convert (must not be null)
- **Returns:** a new LongMatrix with unboxed primitive values
- **See also:** #boxed()

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public LongMatrix(final long[][] a)`
- **Summary:** Constructs a {@code LongMatrix} backed by the supplied two-dimensional array.
- **Contract:**
  - <p> If {@code a} is {@code null} , this creates an empty {@code 0x0} matrix.
  - Call {@link #copy()} if you need an independently owned matrix.
- **Parameters:**
  - `a` (`long[][]`) — the two-dimensional long array to wrap, or {@code null} for an empty matrix
##### get(...) -> long
- **Signature:** `public long get(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** the element at position (rowIndex, columnIndex)
- **Signature:** `public long get(final Point point)`
- **Summary:** Returns the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
- **Returns:** the long element at the specified point
- **See also:** #get(int, int)
##### set(...) -> void
- **Signature:** `public void set(final int rowIndex, final int columnIndex, final long value)`
- **Summary:** Sets the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
  - `value` (`long`) — the value to set
- **Signature:** `public void set(final Point point, final long value)`
- **Summary:** Sets the element at the specified point to the given value.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
  - `value` (`long`) — the new long value to set at the specified point
- **See also:** #set(int, int, long)
##### valueAbove(...) -> OptionalLong
- **Signature:** `public OptionalLong valueAbove(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly above the specified position, if it exists.
- **Contract:**
  - Returns the element directly above the specified position, if it exists.
  - This method provides safe access to the element directly above the given position without throwing an exception when at the top edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalLong containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
##### valueBelow(...) -> OptionalLong
- **Signature:** `public OptionalLong valueBelow(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly below the specified position, if it exists.
- **Contract:**
  - Returns the element directly below the specified position, if it exists.
  - This method provides safe access to the element directly below the given position without throwing an exception when at the bottom edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalLong containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
##### valueLeft(...) -> OptionalLong
- **Signature:** `public OptionalLong valueLeft(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the left of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the left of the specified position, if it exists.
  - This method provides safe access to the element directly to the left of the given position without throwing an exception when at the leftmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalLong containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
##### valueRight(...) -> OptionalLong
- **Signature:** `public OptionalLong valueRight(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the right of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the right of the specified position, if it exists.
  - This method provides safe access to the element directly to the right of the given position without throwing an exception when at the rightmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalLong containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
##### rowView(...) -> long\[\]
- **Signature:** `@Override public long[] rowView(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as a long array.
- **Contract:**
  - Use {@link #rowCopy(int)} if you need an independent copy.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the specified row array (direct reference to internal storage)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
- **See also:** #rowCopy(int)
##### rowCopy(...) -> long\[\]
- **Signature:** `@Override public long[] rowCopy(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new long array containing the values from the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
- **See also:** #rowView(int)
##### columnCopy(...) -> long\[\]
- **Signature:** `@Override public long[] columnCopy(final int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a copy of the specified column as a new long array.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new array containing the values from the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex &lt; 0 or columnIndex &gt; = columnCount
##### setRow(...) -> void
- **Signature:** `public void setRow(final int rowIndex, final long[] row) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified row by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of columns in the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to set (0-based)
  - `row` (`long[]`) — the array of values to copy into the row; must have length equal to the number of columns
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowIndex} is out of bounds, or if {@code row.length} does not equal {@link #columnCount()}
##### setColumn(...) -> void
- **Signature:** `public void setColumn(final int columnIndex, final long[] column) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified column by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of rows in the matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to set (0-based)
  - `column` (`long[]`) — the array of values to copy into the column; must have length equal to the number of rows
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code columnIndex} is out of bounds, or if {@code column.length} does not equal {@link #rowCount()}
##### updateRow(...) -> void
- **Signature:** `public <E extends Exception> void updateRow(final int rowIndex, final Throwables.LongUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in a row in-place by applying the specified operator to each element.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to update (0-based)
  - `operator` (`Throwables.LongUnaryOperator<E>`) — the operator to apply to each element in the row; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### updateColumn(...) -> void
- **Signature:** `public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.LongUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in a column in-place by applying the specified operator to each element.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to update (0-based)
  - `operator` (`Throwables.LongUnaryOperator<E>`) — the operator to apply to each element in the column; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
##### getMainDiagonal(...) -> long\[\]
- **Signature:** `@Override public long[] getMainDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the main diagonal elements (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new long array containing a copy of the main diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setMainDiagonal(...) -> void
- **Signature:** `@Override public void setMainDiagonal(final long[] mainDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `mainDiagonal` (`long[]`) — the new values for the main diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if mainDiagonal array length does not equal rowCount
##### updateMainDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateMainDiagonal(final Throwables.LongUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the main diagonal (upper-left to lower-right) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.LongUnaryOperator<E>`) — the operator to apply to each diagonal element; receives current element value and returns new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### getAntiDiagonal(...) -> long\[\]
- **Signature:** `@Override public long[] getAntiDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the elements on the anti-diagonal from upper-right to lower-left.
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new long array containing a copy of the anti-diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setAntiDiagonal(...) -> void
- **Signature:** `@Override public void setAntiDiagonal(final long[] antiDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `antiDiagonal` (`long[]`) — the new values for the anti-diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if antiDiagonal array length does not equal rowCount
##### updateAntiDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateAntiDiagonal(final Throwables.LongUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates the values on the anti-diagonal (upper-right to lower-left) by applying the specified operator.
- **Contract:**
  - The matrix must be square.
- **Parameters:**
  - `operator` (`Throwables.LongUnaryOperator<E>`) — the operator to apply to each anti-diagonal element; receives current element value and returns new value
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square
  - `E` — if the operator throws an exception
##### updateAll(...) -> void
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.LongUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the matrix in-place by applying the specified operator.
- **Contract:**
  - Elements are processed in row-major order when executed sequentially.
- **Parameters:**
  - `operator` (`Throwables.LongUnaryOperator<E>`) — the operator to apply to each element; receives the current element value and returns the new value
- **Throws:**
  - `E` — if the operator throws an exception
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Long, E> mapper) throws E`
- **Summary:** Updates all elements in the matrix in-place based on their position (row and column indices).
- **Parameters:**
  - `mapper` (`Throwables.IntBiFunction<? extends Long, E>`) — the function that receives row index and column index (0-based) and returns the new value for that position
- **Throws:**
  - `E` — if the mapper throws an exception
##### replaceIf(...) -> void
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.LongPredicate<E> predicate, final long newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on a predicate.
- **Parameters:**
  - `predicate` (`Throwables.LongPredicate<E>`) — the condition to test each element; elements for which this returns {@code true} will be replaced
  - `newValue` (`long`) — the value to use for replacing matching elements
- **Throws:**
  - `E` — if the predicate throws an exception
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final long newValue) throws E`
- **Summary:** Conditionally replaces elements in-place based on their position (row and column indices).
- **Parameters:**
  - `predicate` (`Throwables.IntBiPredicate<E>`) — the condition that tests row index and column index (0-based); elements at positions for which this returns {@code true} will be replaced
  - `newValue` (`long`) — the value to use for replacing matching elements
- **Throws:**
  - `E` — if the predicate throws an exception
##### map(...) -> LongMatrix
- **Signature:** `public <E extends Exception> LongMatrix map(final Throwables.LongUnaryOperator<E> mapper) throws E`
- **Summary:** Creates a new LongMatrix by applying a transformation function to each element.
- **Parameters:**
  - `mapper` (`Throwables.LongUnaryOperator<E>`) — the function to apply to each element; receives the current element value and returns the transformed value
- **Returns:** a new LongMatrix with transformed values
- **Throws:**
  - `E` — if the function throws an exception
- **See also:** #updateAll(Throwables.LongUnaryOperator)
##### mapToInt(...) -> IntMatrix
- **Signature:** `public <E extends Exception> IntMatrix mapToInt(final Throwables.LongToIntFunction<E> mapper) throws E`
- **Summary:** Creates a new IntMatrix by applying the specified function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.LongToIntFunction<E>`) — the mapping function that converts each long element to an int; must not be null
- **Returns:** a new IntMatrix with the mapped values (same dimensions as the original)
- **Throws:**
  - `E` — if the function throws an exception
##### mapToDouble(...) -> DoubleMatrix
- **Signature:** `public <E extends Exception> DoubleMatrix mapToDouble(final Throwables.LongToDoubleFunction<E> mapper) throws E`
- **Summary:** Creates a new DoubleMatrix by applying the specified function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.LongToDoubleFunction<E>`) — the mapping function that converts each long element to a double; must not be null
- **Returns:** a new DoubleMatrix with the mapped values (same dimensions as the original)
- **Throws:**
  - `E` — if the function throws an exception
##### mapToObj(...) -> Matrix<R>
- **Signature:** `public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.LongFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E`
- **Summary:** Creates a new object Matrix by applying the specified function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.LongFunction<? extends R, E>`) — the mapping function that converts each long element to type R; must not be null
  - `targetElementType` (`Class<R>`) — the class object representing the target element type (used for array creation); must not be null
- **Returns:** a new Matrix &lt; R &gt; with the mapped values (same dimensions as the original)
- **Throws:**
  - `E` — if the function throws an exception
##### fill(...) -> void
- **Signature:** `public void fill(final long value)`
- **Summary:** Fills all elements of the matrix with the specified value.
- **Parameters:**
  - `value` (`long`) — the value to fill the matrix with
- **Signature:** `public void fill(final long[][] source)`
- **Summary:** Fills the matrix with values from another two-dimensional array, starting at position (0, 0).
- **Contract:**
  - If the source array is larger, only the portion that fits is copied.
- **Parameters:**
  - `source` (`long[][]`) — the two-dimensional array to copy values from
- **Signature:** `public void fill(final int destRowIndex, final int destColumnIndex, final long[][] source) throws IllegalArgumentException`
- **Summary:** Fills a region of the matrix with values from another two-dimensional array, starting at the specified position.
- **Parameters:**
  - `destRowIndex` (`int`) — the target row index in this matrix (0-based, must be {@code 0 <= destRowIndex <= rowCount} )
  - `destColumnIndex` (`int`) — the target column index in this matrix (0-based, must be {@code 0 <= destColumnIndex <= columnCount} )
  - `source` (`long[][]`) — the source array to copy values from; must not be {@code null} . Individual rows ( {@code source\[i\]} ) may be {@code null} and are skipped during copy.
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code source} is {@code null} , if {@code destRowIndex < 0} or {@code > rowCount} , or if {@code destColumnIndex < 0} or {@code > columnCount}
##### copy(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a copy of this matrix
- **Signature:** `@Override public LongMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a row range from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new LongMatrix containing the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if {@code fromRowIndex < 0} , {@code toRowIndex > rowCount} , or {@code fromRowIndex > toRowIndex}
- **Signature:** `@Override public LongMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a submatrix defined by row and column ranges.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new LongMatrix containing the specified submatrix
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if {@code fromRowIndex < 0} , {@code toRowIndex > rowCount} , {@code fromColumnIndex < 0} , {@code toColumnIndex > columnCount} , or if either {@code from} index exceeds its corresponding {@code to} index
##### resize(...) -> LongMatrix
- **Signature:** `public LongMatrix resize(final int newRowCount, final int newColumnCount)`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code 0L} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
- **Returns:** a new LongMatrix with the specified dimensions
- **See also:** #resize(int, int, long), #extend(int, int, int, int)
- **Signature:** `public LongMatrix resize(final int newRowCount, final int newColumnCount, final long defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code defaultValue} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
  - `defaultValue` (`long`) — the long value used to fill any newly created cells
- **Returns:** a new LongMatrix with the specified dimensions
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code newRowCount} or {@code newColumnCount} is negative, or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
- **See also:** #resize(int, int), #extend(int, int, int, int, long)
##### extend(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight)`
- **Summary:** Returns a new matrix formed by adding {@code 0L} -filled padding around every edge of this matrix.
- **Contract:**
  - Use {@code resize} when you need exact output dimensions regardless of the original size.
- **Parameters:**
  - `padTop` (`int`) — number of rows to add above; must be {@code >= 0}
  - `padBottom` (`int`) — number of rows to add below; must be {@code >= 0}
  - `padLeft` (`int`) — number of columns to add to the left; must be {@code >= 0}
  - `padRight` (`int`) — number of columns to add to the right; must be {@code >= 0}
- **Returns:** a new LongMatrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
- **See also:** #extend(int, int, int, int, long), #resize(int, int)
- **Signature:** `public LongMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final long defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix formed by adding {@code defaultValue} -filled padding around every edge of this matrix.
- **Parameters:**
  - `padTop` (`int`) — number of rows to add above; must be {@code >= 0}
  - `padBottom` (`int`) — number of rows to add below; must be {@code >= 0}
  - `padLeft` (`int`) — number of columns to add to the left; must be {@code >= 0}
  - `padRight` (`int`) — number of columns to add to the right; must be {@code >= 0}
  - `defaultValue` (`long`) — the long value used to fill all newly added cells
- **Returns:** a new LongMatrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any padding parameter is negative, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** #extend(int, int, int, int), #resize(int, int, long)
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `@Override public void flipHorizontallyInPlace()`
- **Summary:** Reverses the order of elements in each row (horizontal flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally(), #flipVerticallyInPlace()
##### flipVerticallyInPlace(...) -> void
- **Signature:** `@Override public void flipVerticallyInPlace()`
- **Summary:** Reverses the order of rows in the matrix (vertical flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipVertically(), #flipHorizontallyInPlace()
##### flipHorizontally(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix flipHorizontally()`
- **Summary:** Creates a new matrix that is horizontally flipped (each row reversed).
- **Parameters:**
  - (none)
- **Returns:** a new matrix with each row reversed
- **See also:** #flipHorizontallyInPlace(), #flipVertically(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### flipVertically(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix flipVertically()`
- **Summary:** Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
- **Parameters:**
  - (none)
- **Returns:** a new matrix with rows in reversed order
- **See also:** #flipVerticallyInPlace(), #flipHorizontally(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### rotate90(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix rotated 90 degrees clockwise
##### rotate180(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix rotated 180 degrees clockwise
##### rotate270(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix rotated 270 degrees clockwise
##### transpose(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix transpose()`
- **Summary:** Creates the transpose of this matrix by swapping rows and columns.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is the transpose of this matrix with dimensions columnCount × rowCount
##### reshape(...) -> LongMatrix
- **Signature:** `@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG") @Override public LongMatrix reshape(final int newRowCount, final int newColumnCount)`
- **Summary:** Reshapes the matrix to new dimensions while preserving element order.
- **Contract:**
  - <p> The new shape must have at least as many total elements as the original ( {@code newRowCount * newColumnCount >= elementCount()} ).
  - If the new shape has more total elements, the additional positions are filled with zeros.
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix (must be non-negative)
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix (must be non-negative)
- **Returns:** a new LongMatrix with the specified shape containing this matrix's elements
##### repeatElements(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats elements in the matrix by the specified factors in both row and column directions.
- **Parameters:**
  - `rowRepeats` (`int`) — the number of times to repeat each element in the row direction
  - `columnRepeats` (`int`) — the number of times to repeat each element in the column direction
- **Returns:** a new matrix with repeated elements
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is less than or equal to 0, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#repeatElements(int, int)
##### repeatMatrix(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats the entire matrix as a tile pattern by the specified factors in both row and column directions.
- **Parameters:**
  - `rowRepeats` (`int`) — the number of times to repeat the matrix in the row direction
  - `columnRepeats` (`int`) — the number of times to repeat the matrix in the column direction
- **Returns:** a new matrix with the original matrix repeated as tiles
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is less than or equal to 0, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#repeatMatrix(int, int)
##### flatten(...) -> LongList
- **Signature:** `@Override public LongList flatten()`
- **Summary:** Returns a list containing all matrix elements in row-major order (row by row, left to right).
- **Parameters:**
  - (none)
- **Returns:** a new LongList containing all elements in row-major order
##### mutateAsFlat(...) -> void
- **Signature:** `@Override public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super long[], E> action) throws E`
- **Summary:** Flattens all elements of this matrix into a single one-dimensional array, applies the given operation to that flattened array, and then copies the modified elements back into the matrix.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super long[], E>`) — the operation to apply to the flattened array
- **Throws:**
  - `E` — if the operation throws an exception
- **See also:** Arrays#mutateAsFlat(long\[\]\[\], Throwables.Consumer)
##### stackVertically(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix stackVertically(final LongMatrix other) throws IllegalArgumentException`
- **Summary:** Vertically stacks this matrix with another matrix.
- **Contract:**
  - The two matrices must have the same number of columns.
- **Parameters:**
  - `other` (`LongMatrix`) — the matrix to stack below this matrix
- **Returns:** a new matrix with rows from both matrices stacked vertically
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , if the matrices don't have the same number of columns, or if the merged row count would exceed {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackVertically(IntMatrix)
##### stackHorizontally(...) -> LongMatrix
- **Signature:** `@Override public LongMatrix stackHorizontally(final LongMatrix other) throws IllegalArgumentException`
- **Summary:** Horizontally stacks this matrix with another matrix.
- **Contract:**
  - The two matrices must have the same number of rows.
- **Parameters:**
  - `other` (`LongMatrix`) — the matrix to stack to the right of this matrix
- **Returns:** a new matrix with columns from both matrices stacked horizontally
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , if the matrices don't have the same number of rows, or if the merged column count would exceed {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackHorizontally(IntMatrix)
##### add(...) -> LongMatrix
- **Signature:** `public LongMatrix add(final LongMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise addition of this matrix with another matrix.
- **Contract:**
  - The two matrices must have the same dimensions.
- **Parameters:**
  - `other` (`LongMatrix`) — the matrix to add to this matrix; must not be {@code null}
- **Returns:** a new matrix containing the element-wise sum
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrices don't have the same shape
##### subtract(...) -> LongMatrix
- **Signature:** `public LongMatrix subtract(final LongMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise subtraction of another matrix from this matrix.
- **Contract:**
  - The two matrices must have the same dimensions.
- **Parameters:**
  - `other` (`LongMatrix`) — the matrix to subtract from this matrix; must not be {@code null}
- **Returns:** a new matrix containing the element-wise difference
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrices don't have the same shape
##### matmul(...) -> LongMatrix
- **Signature:** `public LongMatrix matmul(final LongMatrix other) throws IllegalArgumentException`
- **Summary:** Performs matrix multiplication (Cayley product) with another matrix.
- **Contract:**
  - The number of columns in this matrix must equal the number of rows in the specified matrix.
- **Parameters:**
  - `other` (`LongMatrix`) — the matrix to multiply with this matrix; must not be {@code null}
- **Returns:** a new matrix containing the matrix product
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} or the matrix dimensions are incompatible ( {@code this.columnCount != other.rowCount} )
##### boxed(...) -> Matrix<Long>
- **Signature:** `public Matrix<Long> boxed()`
- **Summary:** Converts this primitive long matrix to a boxed {@code Matrix<Long>} .
- **Contract:**
  - Use this method only when you need to work with generic Matrix API or when null values are required.
- **Parameters:**
  - (none)
- **Returns:** a new {@code Matrix<Long>} containing boxed values
- **See also:** #unbox(Matrix)
##### toIntMatrix(...) -> IntMatrix
- **Signature:** `public IntMatrix toIntMatrix()`
- **Summary:** Converts this long matrix to an int matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code IntMatrix} with values converted from long to int
- **See also:** #mapToInt(Throwables.LongToIntFunction)
##### toFloatMatrix(...) -> FloatMatrix
- **Signature:** `public FloatMatrix toFloatMatrix()`
- **Summary:** Converts this long matrix to a float matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code FloatMatrix} with values converted from long to float
##### toDoubleMatrix(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix toDoubleMatrix()`
- **Summary:** Converts this long matrix to a double matrix.
- **Contract:**
  - <p> <b> Note: </b> Very large long values (with absolute value greater than 2^53) may lose precision when converted to double, since double has only 53 bits of precision in its mantissa.
- **Parameters:**
  - (none)
- **Returns:** a new {@code DoubleMatrix} with values converted from long to double
- **See also:** #mapToDouble(Throwables.LongToDoubleFunction)
##### zipWith(...) -> LongMatrix
- **Signature:** `public <E extends Exception> LongMatrix zipWith(final LongMatrix other, final Throwables.LongBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Applies a binary operation element-wise to this matrix and another matrix.
- **Contract:**
  - The two matrices must have the same dimensions (same number of rows and columns).
- **Parameters:**
  - `other` (`LongMatrix`) — the second matrix to zip with this matrix; must have the same dimensions
  - `zipFunction` (`Throwables.LongBinaryOperator<E>`) — the binary operation to apply to corresponding elements from this and other
- **Returns:** a new LongMatrix with the results of the zip operation
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices don't have the same shape (rows and columns)
  - `E` — if the zip function throws an exception
- **Signature:** `public <E extends Exception> LongMatrix zipWith(final LongMatrix other, final LongMatrix third, final Throwables.LongTernaryOperator<E> zipFunction) throws E`
- **Summary:** Applies a ternary operation element-wise to this matrix and two other matrices.
- **Contract:**
  - All three matrices must have the same dimensions (same number of rows and columns).
- **Parameters:**
  - `other` (`LongMatrix`) — the second matrix to zip with; must have the same dimensions as this matrix
  - `third` (`LongMatrix`) — the third matrix to zip with; must have the same dimensions as this matrix
  - `zipFunction` (`Throwables.LongTernaryOperator<E>`) — the ternary operation to apply to corresponding elements from this, other, and third
- **Returns:** a new LongMatrix with the results of the zip operation
- **Throws:**
  - `E` — if the zip function throws an exception
##### mainDiagonalStream(...) -> LongStream
- **Signature:** `@Override public LongStream mainDiagonalStream()`
- **Summary:** Returns a stream of elements on the diagonal from upper-left to lower-right.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a stream of diagonal elements from upper-left to lower-right
##### antiDiagonalStream(...) -> LongStream
- **Signature:** `@Override public LongStream antiDiagonalStream()`
- **Summary:** Returns a stream of elements on the anti-diagonal from upper-right to lower-left.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a stream of diagonal elements from upper-right to lower-left
##### horizontalStream(...) -> LongStream
- **Signature:** `@Override public LongStream horizontalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
- **Parameters:**
  - (none)
- **Returns:** a stream of all matrix elements in row-major order, or an empty stream if the matrix is empty
- **Signature:** `@Override public LongStream horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a single row.
- **Contract:**
  - <p> This method is particularly useful when you need to process or analyze a specific row of the matrix independently.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to stream (0-based)
- **Returns:** a stream of elements from the specified row
- **Signature:** `@Override public LongStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of rows in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a stream of elements from the specified row range, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
##### verticalStream(...) -> LongStream
- **Signature:** `@Override @Beta public LongStream verticalStream()`
- **Summary:** Creates a stream of all elements in the matrix in column-major order (vertically).
- **Parameters:**
  - (none)
- **Returns:** a stream of all matrix elements in column-major order
- **Signature:** `@Override public LongStream verticalStream(final int columnIndex)`
- **Summary:** Creates a stream of elements from a specific column.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to stream
- **Returns:** a stream of elements from the specified column
- **Signature:** `@Override @Beta public LongStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a stream of elements from a range of columns in column-major order.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a stream of elements from the specified column range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the column indices are out of bounds
##### rowStreams(...) -> Stream<LongStream>
- **Signature:** `@Override public Stream<LongStream> rowStreams()`
- **Summary:** Creates a stream of row streams, where each element is a stream of a complete row.
- **Parameters:**
  - (none)
- **Returns:** a stream of row streams
- **Signature:** `@Override public Stream<LongStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a stream of row streams from a range of rows.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a stream of row streams from the specified range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the row indices are out of bounds
##### columnStreams(...) -> Stream<LongStream>
- **Signature:** `@Override @Beta public Stream<LongStream> columnStreams()`
- **Summary:** Creates a stream of column streams, where each element is a stream of a complete column.
- **Parameters:**
  - (none)
- **Returns:** a stream of column streams
- **Signature:** `@Override @Beta public Stream<LongStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a stream of column streams from a range of columns.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a stream of column streams from the specified range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the column indices are out of bounds
##### forEach(...) -> void
- **Signature:** `public <E extends Exception> void forEach(final Throwables.LongConsumer<E> action) throws E`
- **Summary:** Performs the specified action for each element in this matrix.
- **Parameters:**
  - `action` (`Throwables.LongConsumer<E>`) — the action to apply to each element; must not be {@code null}
- **Throws:**
  - `E` — if the action throws an exception
- **Signature:** `public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.LongConsumer<E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element in a sub-region of this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.LongConsumer<E>`) — the action to apply to each element in the specified region; must not be {@code null}
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the row/column indices are out of bounds {@code \[0, rowCount\]} / {@code \[0, columnCount\]} , or if {@code fromRowIndex > toRowIndex} or {@code fromColumnIndex > toColumnIndex}
  - `E` — if the action throws an exception
##### println(...) -> String
- **Signature:** `@Override public String println()`
- **Summary:** Prints this matrix to standard output and returns the formatted string.
- **Parameters:**
  - (none)
- **Returns:** the formatted string representation of the matrix
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this matrix.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this matrix
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this matrix to the specified object for equality.
- **Contract:**
  - Returns {@code true} if the given object is also a LongMatrix with the same dimensions and all corresponding elements are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this matrix

### Class Matrices (com.landawn.abacus.matrix.Matrices)
Utility and policy holder shared by the matrix implementations in this package.

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### getParallelMode(...) -> ParallelMode
- **Signature:** `public static ParallelMode getParallelMode()`
- **Summary:** Returns the current parallel processing setting for the current thread.
- **Contract:**
  - </p> <p> The returned value indicates how matrix operations should decide whether to use parallel processing: </p> <ul> <li> {@link ParallelMode#FORCE_ON} - Requests parallel execution whenever the runtime supports it </li> <li> {@link ParallelMode#FORCE_OFF} - Forces sequential execution regardless of matrix size </li> <li> {@link ParallelMode#AUTO} - Automatically decides based on matrix size (threshold: 8192 elements) </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code ParallelMode current = Matrices.getParallelMode(); // Check current setting before changing it if (current == ParallelMode.AUTO) { Matrices.setParallelMode(ParallelMode.FORCE_ON); } } </pre>
- **Parameters:**
  - (none)
- **Returns:** the current {@link ParallelMode} setting for this thread, never {@code null}
- **See also:** #setParallelMode(ParallelMode), ParallelMode
##### setParallelMode(...) -> void
- **Signature:** `public static void setParallelMode(final ParallelMode parallelMode) throws IllegalArgumentException`
- **Summary:** Sets the parallel processing behavior for matrix operations in the current thread.
- **Contract:**
  - </p> <p> Available settings: </p> <ul> <li> {@link ParallelMode#FORCE_ON} - Requests parallel processing for all matrix operations when the runtime supports it, regardless of matrix size.
- **Parameters:**
  - `parallelMode` (`ParallelMode`) — the {@link ParallelMode} setting to apply to the current thread, must not be {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code parallelMode} is {@code null}
- **See also:** #getParallelMode(), ParallelMode
##### isParallelizable(...) -> boolean
- **Signature:** `public static boolean isParallelizable(final AbstractMatrix<?, ?, ?, ?, ?> m)`
- **Summary:** Determines whether the given matrix should be processed using parallel execution.
- **Contract:**
  - Determines whether the given matrix should be processed using parallel execution.
  - <p> This method evaluates whether parallel processing should be used for operations on the specified matrix based on its total element count.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code IntMatrix matrix = IntMatrix.of(new int\[1000\]\[1000\]); if (Matrices.isParallelizable(matrix)) { // Matrix is large enough for parallel processing } } </pre>
- **Parameters:**
  - `m` (`AbstractMatrix<?, ?, ?, ?, ?>`) — the matrix to evaluate for parallelization, must not be {@code null}
- **Returns:** {@code true} if parallel processing should be used for this matrix; {@code false} for sequential processing
- **See also:** #isParallelizable(AbstractMatrix, long), #setParallelMode(ParallelMode)
- **Signature:** `public static boolean isParallelizable(final AbstractMatrix<?, ?, ?, ?, ?> m, final long count)`
- **Summary:** Determines whether a matrix operation should be processed using parallel execution based on the element count and current parallel settings.
- **Contract:**
  - Determines whether a matrix operation should be processed using parallel execution based on the element count and current parallel settings.
  - <p> This method makes the parallelization decision using a multifactor evaluation: </p> <ol> <li> <b> Runtime Support: </b> Parallel streams must be available in the runtime environment.
  - If not supported, always returns {@code false} .
  - </li> <li> <b> Thread Setting: </b> Checks the current thread's {@link ParallelMode} setting: <ul> <li> {@link ParallelMode#FORCE_ON} - Returns {@code true} whenever runtime support is available </li> <li> {@link ParallelMode#FORCE_OFF} - Always returns {@code false} </li> <li> {@link ParallelMode#AUTO} - Decides based on element count </li> </ul> </li> <li> <b> Element Count: </b> When using {@code AUTO} setting, returns {@code true} only if {@code count >= 8192} .
  - </li> </ol> <p> <b> Usage Examples: </b> </p> <pre> {@code IntMatrix matrix = IntMatrix.of(new int\[100\]\[100\]); boolean shouldParallelize = Matrices.isParallelizable(matrix, 5000); // Under AUTO this returns false (5000 < 8192); under FORCE_ON it returns true (when runtime support is available) } </pre>
- **Parameters:**
  - `m` (`AbstractMatrix<?, ?, ?, ?, ?>`) — the matrix being evaluated; only checked for {@code null} , the matrix's own element count is not consulted (the supplied {@code count} drives the decision)
  - `count` (`long`) — the number of elements to process; typically the total element count or a subset being operated on
- **Returns:** {@code true} if parallel processing should be used; {@code false} for sequential processing
- **See also:** #setParallelMode(ParallelMode), ParallelMode
##### isSameShape(...) -> boolean
- **Signature:** `public static <M extends AbstractMatrix<?, ?, ?, ?, ?>> boolean isSameShape(final M a, final M b)`
- **Summary:** Checks if two matrices have the same shape (identical dimensions).
- **Contract:**
  - Checks if two matrices have the same shape (identical dimensions).
  - <p> Two matrices are considered to have the same shape if and only if they have the same number of rows AND the same number of columns.
- **Parameters:**
  - `a` (`M`) — the first matrix to compare, must not be {@code null}
  - `b` (`M`) — the second matrix to compare, must not be {@code null}
- **Returns:** {@code true} if both matrices have the same number of rows and columns; {@code false} otherwise
- **Signature:** `public static <M extends AbstractMatrix<?, ?, ?, ?, ?>> boolean isSameShape(final M a, final M b, final M c)`
- **Summary:** Checks if three matrices have the same shape (identical dimensions).
- **Contract:**
  - Checks if three matrices have the same shape (identical dimensions).
  - <p> Three matrices are considered to have the same shape if they all have the same number of rows AND the same number of columns.
- **Parameters:**
  - `a` (`M`) — the first matrix to compare, must not be {@code null}
  - `b` (`M`) — the second matrix to compare, must not be {@code null}
  - `c` (`M`) — the third matrix to compare, must not be {@code null}
- **Returns:** {@code true} if all three matrices have the same number of rows and columns; {@code false} otherwise
- **Signature:** `public static <M extends AbstractMatrix<?, ?, ?, ?, ?>> boolean isSameShape(final Collection<? extends M> matrices)`
- **Summary:** Checks if all matrices in a collection have the same shape (identical dimensions).
- **Contract:**
  - Checks if all matrices in a collection have the same shape (identical dimensions).
  - </p> <p> Special cases: </p> <ul> <li> Empty collection: Returns {@code true} (vacuous truth) </li> <li> Single matrix: Returns {@code true} (trivially same shape) </li> <li> Multiple matrices: Returns {@code true} only if all have identical dimensions </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code List<IntMatrix> matrices = java.util.Arrays.asList(m1, m2, m3, m4); if (Matrices.isSameShape(matrices)) { // All matrices have the same dimensions } } </pre>
- **Parameters:**
  - `matrices` (`Collection<? extends M>`) — the collection of matrices to check, may be {@code null} or empty
- **Returns:** {@code true} if all matrices have the same number of rows and columns, or if the collection is {@code null} or empty; {@code false} if any matrix has different dimensions or if any element in the collection is {@code null}
##### newMatrixArray(...) -> T\[\]\[\]
- **Signature:** `public static <T> T[][] newMatrixArray(final int rowCount, final int columnCount, final Class<T> targetElementType)`
- **Summary:** Creates a new two-dimensional array with the specified dimensions and element type.
- **Contract:**
  - </p> <p> The requested dimensions must form a representable matrix shape: a positive {@code rowCount} , or a {@code columnCount} of zero.
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the two-dimensional array, must be non-negative
  - `columnCount` (`int`) — the number of columns in each row, must be non-negative
  - `targetElementType` (`Class<T>`) — the class of the element type; primitive types will be auto-wrapped, must not be {@code null}
- **Returns:** a new two-dimensional array of type {@code T\[\]\[\]} with the specified dimensions, never {@code null}
- **Performance:** <p> This utility method constructs a properly typed two-dimensional array at runtime, handling the complexity of creating generic arrays in Java.
##### runWithParallelMode(...) -> void
- **Signature:** `public static <E extends Exception> void runWithParallelMode(final ParallelMode parallelMode, final Throwables.Runnable<E> action) throws E`
- **Summary:** Executes the specified command with a temporary parallel processing setting, then restores the original setting.
- **Contract:**
  - The original {@link ParallelMode} setting is always restored, even if the command throws an exception.
  - </p> <p> This is particularly useful when you need to force parallel or sequential execution for a specific block of code without manually managing the setting changes.
- **Parameters:**
  - `parallelMode` (`ParallelMode`) — the temporary {@link ParallelMode} setting to use during command execution, must not be {@code null}
  - `action` (`Throwables.Runnable<E>`) — the command to execute, must not be {@code null}
- **Throws:**
  - `E` — if the command throws an exception during execution
- **See also:** #setParallelMode(ParallelMode), #getParallelMode()
##### forEachIndices(...) -> void
- **Signature:** `public static <E extends Exception> void forEachIndices(final int rowCount, final int columnCount, final Throwables.IntBiConsumer<E> action, final boolean inParallel) throws E`
- **Summary:** Executes a command for each position in a matrix grid defined by rows and columns.
- **Parameters:**
  - `rowCount` (`int`) — the number of rows to iterate over, must be non-negative
  - `columnCount` (`int`) — the number of columns to iterate over, must be non-negative
  - `action` (`Throwables.IntBiConsumer<E>`) — the command to execute for each position (i, j), receives row index and column index, must not be {@code null}
  - `inParallel` (`boolean`) — {@code true} to execute in parallel; {@code false} for sequential execution
- **Throws:**
  - `E` — if the command throws an exception during execution
- **See also:** #forEachIndices(int, int, int, int, Throwables.IntBiConsumer, boolean)
- **Signature:** `public static <E extends Exception> void forEachIndices(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.IntBiConsumer<E> action, final boolean inParallel) throws IndexOutOfBoundsException, E`
- **Summary:** Executes a command for each position in a specified subregion of a matrix grid.
- **Contract:**
  - </p> <p> Iteration strategy: </p> <ul> <li> If there are fewer or equal rows than columns, iterates by rows first (row-major order) </li> <li> If there are more rows than columns, iterates by columns first (column-major order) </li> <li> When parallel execution is enabled, the outer loop is parallelized while the inner loop remains sequential </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code // Process a subregion of a matrix int\[\]\[\] result = new int\[10\]\[10\]; Matrices.forEachIndices(2, 5, 3, 8, (i, j) -> result\[i\]\[j\] = i + j, false); } </pre>
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive), must be non-negative
  - `toRowIndex` (`int`) — the ending row index (exclusive), must be greater than or equal to fromRowIndex
  - `fromColumnIndex` (`int`) — the starting column index (inclusive), must be non-negative
  - `toColumnIndex` (`int`) — the ending column index (exclusive), must be greater than or equal to fromColumnIndex
  - `action` (`Throwables.IntBiConsumer<E>`) — the command to execute for each position (i, j), receives row index and column index, must not be {@code null}
  - `inParallel` (`boolean`) — {@code true} to execute in parallel; {@code false} for sequential execution
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is negative or if toRowIndex is less than fromRowIndex or toColumnIndex is less than fromColumnIndex
  - `E` — if the command throws an exception during execution
##### mapIndices(...) -> Stream<T>
- **Signature:** `public static <T> Stream<T> mapIndices(final int rowCount, final int columnCount, final Throwables.IntBiFunction<? extends T, ? extends Exception> mapper, final boolean inParallel)`
- **Summary:** Executes a function for each position in a matrix grid and returns the results as a stream.
- **Parameters:**
  - `rowCount` (`int`) — the number of rows to iterate over, must be non-negative
  - `columnCount` (`int`) — the number of columns to iterate over, must be non-negative
  - `mapper` (`Throwables.IntBiFunction<? extends T, ? extends Exception>`) — the function to apply at each position (i, j), receives row index and column index, must not be {@code null}
  - `inParallel` (`boolean`) — {@code true} to execute in parallel; {@code false} for sequential execution
- **Returns:** a {@link Stream} of results from applying the function at each position, never {@code null}
- **See also:** #mapIndices(int, int, int, int, Throwables.IntBiFunction, boolean)
- **Signature:** `@SuppressWarnings("resource") public static <T> Stream<T> mapIndices(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.IntBiFunction<? extends T, ? extends Exception> mapper, final boolean inParallel) throws IndexOutOfBoundsException`
- **Summary:** Executes a function for each position in a specified subregion of a matrix grid and returns the results as a stream.
- **Contract:**
  - </p> <p> The order of elements in the stream depends on whether there are more rows or columns: </p> <ul> <li> If rows is less than or equal to columns: Elements are ordered by rows first (row-major order) </li> <li> If rows is greater than columns: Elements are ordered by columns first (column-major order) </li> </ul> <p> <b> Usage Examples: </b> </p> <pre> {@code Stream<String> coords = Matrices.mapIndices(1, 4, 2, 5, (i, j) -> i + "," + j, false); // Generates coordinates for subregion } </pre>
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive), must be non-negative
  - `toRowIndex` (`int`) — the ending row index (exclusive), must be greater than or equal to fromRowIndex
  - `fromColumnIndex` (`int`) — the starting column index (inclusive), must be non-negative
  - `toColumnIndex` (`int`) — the ending column index (exclusive), must be greater than or equal to fromColumnIndex
  - `mapper` (`Throwables.IntBiFunction<? extends T, ? extends Exception>`) — the function to apply at each position (i, j), receives row index and column index, must not be {@code null}
  - `inParallel` (`boolean`) — {@code true} to execute in parallel; {@code false} for sequential execution
- **Returns:** a {@link Stream} of results from applying the function at each position, never {@code null}
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is negative or if toRowIndex is less than fromRowIndex or toColumnIndex is less than fromColumnIndex
##### mapIndicesToInt(...) -> IntStream
- **Signature:** `public static IntStream mapIndicesToInt(final int rowCount, final int columnCount, final Throwables.IntBinaryOperator<? extends Exception> mapper, final boolean inParallel)`
- **Summary:** Executes a function that returns {@code int} values for each position in a matrix grid and returns the results as an {@link IntStream} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows to iterate over, must be non-negative
  - `columnCount` (`int`) — the number of columns to iterate over, must be non-negative
  - `mapper` (`Throwables.IntBinaryOperator<? extends Exception>`) — the function to apply at each position (i, j), receives row index and column index, must not be {@code null}
  - `inParallel` (`boolean`) — {@code true} to execute in parallel; {@code false} for sequential execution
- **Returns:** an {@link IntStream} of results from applying the function at each position, never {@code null}
- **See also:** #mapIndicesToInt(int, int, int, int, Throwables.IntBinaryOperator, boolean)
- **Signature:** `@SuppressWarnings("resource") public static IntStream mapIndicesToInt(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.IntBinaryOperator<? extends Exception> mapper, final boolean inParallel) throws IndexOutOfBoundsException`
- **Summary:** Executes a function that returns {@code int} values for each position in a specified subregion of a matrix grid and returns the results as an {@link IntStream} .
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive), must be non-negative
  - `toRowIndex` (`int`) — the ending row index (exclusive), must be greater than or equal to fromRowIndex
  - `fromColumnIndex` (`int`) — the starting column index (inclusive), must be non-negative
  - `toColumnIndex` (`int`) — the ending column index (exclusive), must be greater than or equal to fromColumnIndex
  - `mapper` (`Throwables.IntBinaryOperator<? extends Exception>`) — the function to apply at each position (i, j), receives row index and column index, must not be {@code null}
  - `inParallel` (`boolean`) — {@code true} to execute in parallel; {@code false} for sequential execution
- **Returns:** an {@link IntStream} of results from applying the function at each position, never {@code null}
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is negative or if toRowIndex is less than fromRowIndex or toColumnIndex is less than fromColumnIndex
##### forEachCartesianIndices(...) -> void
- **Signature:** `public static <M extends AbstractMatrix<?, ?, ?, ?, ?>> void forEachCartesianIndices(final M a, final M b, final Throwables.IntTriConsumer<RuntimeException> action) throws IllegalArgumentException`
- **Summary:** Performs matrix multiplication iteration using a custom accumulator function.
- **Contract:**
  - It does NOT perform the actual multiplication arithmetic - that must be implemented in the command function.
  - </p> <p> For standard matrix multiplication C = A × B, the command would typically accumulate: {@code C\[i\]\[j\] += A\[i\]\[k\] * B\[k\]\[j\]} </p> <p> Index meanings: </p> <ul> <li> {@code i} - Row index in matrix A (and result matrix C) </li> <li> {@code j} - Column index in matrix B (and result matrix C) </li> <li> {@code k} - Common dimension (columns in A, rows in B) </li> </ul> <p> The matrices must satisfy the multiplication constraint: {@code a.columnCount == b.rowCount} .
- **Parameters:**
  - `a` (`M`) — the first matrix (left operand), must not be {@code null}
  - `b` (`M`) — the second matrix (right operand), must not be {@code null}
  - `action` (`Throwables.IntTriConsumer<RuntimeException>`) — the accumulator function called for each (i, j, k) triple in the multiplication, must not be {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code a} or {@code b} is {@code null} , if matrix dimensions are incompatible ( {@code a.columnCount != b.rowCount} ), or if {@code action} is {@code null}
- **See also:** #forEachCartesianIndices(AbstractMatrix, AbstractMatrix, Throwables.IntTriConsumer, boolean)
- **Signature:** `public static <M extends AbstractMatrix<?, ?, ?, ?, ?>> void forEachCartesianIndices(final M a, final M b, final Throwables.IntTriConsumer<RuntimeException> action, // NOSONAR final boolean inParallel) throws IllegalArgumentException`
- **Summary:** Performs matrix multiplication iteration using a custom accumulator function with explicit control over parallel execution.
- **Contract:**
  - </p> <p> When parallel execution is enabled, the outermost loop is parallelized while inner loops remain sequential.
  - To avoid concurrent writes to the same accumulator cell, the {@code k} loop (over {@code a.columnCount} = {@code b.rowCount} ) is never parallelized; when {@code rowsA} is not the smallest dimension, the parallel loop is over {@code b.columnCount} ( {@code j} ) instead.
- **Parameters:**
  - `a` (`M`) — the first matrix (left operand), must not be {@code null}
  - `b` (`M`) — the second matrix (right operand), must not be {@code null}
  - `action` (`Throwables.IntTriConsumer<RuntimeException>`) — the accumulator function called for each (i, j, k) triple in the multiplication, must not be {@code null}
  - `inParallel` (`boolean`) — {@code true} to force parallel execution; {@code false} for sequential execution
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code a} or {@code b} is {@code null} , if matrix dimensions are incompatible ( {@code a.columnCount != b.rowCount} ), or if {@code action} is {@code null}
- **See also:** #forEachCartesianIndices(AbstractMatrix, AbstractMatrix, Throwables.IntTriConsumer)
##### stackVertically(...) -> M
- **Signature:** `public static <M extends AbstractMatrix<?, ?, ?, ?, M>> M stackVertically(final Collection<? extends M> matrices)`
- **Summary:** Stacks the given matrices vertically (row-wise concatenation), n-ary form.
- **Contract:**
  - <p> All matrices must have the same column count.
  - Equivalent to chaining {@link AbstractMatrix#stackVertically(AbstractMatrix)} calls, but avoids the boilerplate when stacking three or more matrices.
- **Parameters:**
  - `matrices` (`Collection<? extends M>`) — the matrices to stack vertically, must not be {@code null} or empty
- **Returns:** a new matrix containing the rows of all input matrices, never {@code null}
- **See also:** AbstractMatrix#stackVertically(AbstractMatrix), #stackHorizontally(Collection)
##### stackHorizontally(...) -> M
- **Signature:** `public static <M extends AbstractMatrix<?, ?, ?, ?, M>> M stackHorizontally(final Collection<? extends M> matrices)`
- **Summary:** Stacks the given matrices horizontally (column-wise concatenation), n-ary form.
- **Contract:**
  - <p> All matrices must have the same row count.
  - Equivalent to chaining {@link AbstractMatrix#stackHorizontally(AbstractMatrix)} calls, but avoids the boilerplate when stacking three or more matrices.
- **Parameters:**
  - `matrices` (`Collection<? extends M>`) — the matrices to stack horizontally, must not be {@code null} or empty
- **Returns:** a new matrix containing the columns of all input matrices, never {@code null}
- **See also:** AbstractMatrix#stackHorizontally(AbstractMatrix), #stackVertically(Collection)
##### zip(...) -> ByteMatrix
- **Signature:** `public static <E extends Exception> ByteMatrix zip(final ByteMatrix a, final ByteMatrix b, final Throwables.ByteBinaryOperator<E> zipFunction) throws E`
- **Summary:** Combines two {@link ByteMatrix} objects element-wise using a binary operator.
- **Contract:**
  - </p> <p> Both matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`ByteMatrix`) — the first matrix, must not be {@code null}
  - `b` (`ByteMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.ByteBinaryOperator<E>`) — the binary operator to combine corresponding elements from both matrices, must not be {@code null}
- **Returns:** a new {@link ByteMatrix} containing the results of applying the function to each pair of elements, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(ByteMatrix, ByteMatrix, ByteMatrix, Throwables.ByteTernaryOperator), #zip(Collection, Throwables.ByteBinaryOperator), ByteMatrix#zipWith(ByteMatrix, Throwables.ByteBinaryOperator)
- **Signature:** `public static <E extends Exception> ByteMatrix zip(final ByteMatrix a, final ByteMatrix b, final ByteMatrix c, final Throwables.ByteTernaryOperator<E> zipFunction) throws E`
- **Summary:** Combines three {@link ByteMatrix} objects element-wise using a ternary operator.
- **Contract:**
  - </p> <p> All three matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`ByteMatrix`) — the first matrix, must not be {@code null}
  - `b` (`ByteMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`ByteMatrix`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.ByteTernaryOperator<E>`) — the ternary operator to combine corresponding elements from all three matrices, must not be {@code null}
- **Returns:** a new {@link ByteMatrix} containing the results of applying the function to each triple of elements, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(ByteMatrix, ByteMatrix, Throwables.ByteBinaryOperator), #zip(Collection, Throwables.ByteBinaryOperator), ByteMatrix#zipWith(ByteMatrix, ByteMatrix, Throwables.ByteTernaryOperator)
- **Signature:** `public static <E extends Exception> ByteMatrix zip(final Collection<ByteMatrix> coll, final Throwables.ByteBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link ByteMatrix} objects element-wise using a binary operator applied sequentially.
- **Contract:**
  - } </pre> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<ByteMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.ByteBinaryOperator<E>`) — the binary operator to combine elements sequentially, must not be {@code null}
- **Returns:** a new {@link ByteMatrix} containing the combined results, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(ByteMatrix, ByteMatrix, Throwables.ByteBinaryOperator), #zip(ByteMatrix, ByteMatrix, ByteMatrix, Throwables.ByteTernaryOperator), #zip(Collection, Throwables.ByteNFunction, Class)
- **Signature:** `public static <R, E extends Exception> Matrix<R> zip(final Collection<ByteMatrix> coll, final Throwables.ByteNFunction<? extends R, E> zipFunction, final Class<R> targetElementType) throws E`
- **Summary:** Combines multiple {@link ByteMatrix} objects element-wise using a function that operates on byte arrays.
- **Parameters:**
  - `coll` (`Collection<ByteMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.ByteNFunction<? extends R, E>`) — the function that takes an array of bytes (one from each matrix) and returns a result of type R, must not be {@code null}
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Collection, Throwables.ByteNFunction, boolean, Class), #zip(Collection, Throwables.ByteBinaryOperator)
- **Signature:** `public static <R, E extends Exception> Matrix<R> zip(final Collection<ByteMatrix> coll, final Throwables.ByteNFunction<? extends R, E> zipFunction, final boolean shareIntermediateArray, final Class<R> targetElementType) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link ByteMatrix} objects element-wise using a function that operates on byte arrays, with control over intermediate array sharing.
- **Contract:**
  - The {@code shareIntermediateArray} parameter controls memory optimization: </p> <ul> <li> {@code true} and sequential execution: Reuses the same intermediate array for all positions, reducing memory allocations but requiring the zip function to not retain references to the array </li> <li> {@code false} or parallel execution: Creates a new array for each position, safer but uses more memory </li> </ul> <p> <b> Warning: </b> When {@code shareIntermediateArray} is {@code true} , the zip function must NOT store references to the array, as it will be mutated for subsequent positions.
  - Only use this optimization if the function immediately processes and discards the array.
- **Parameters:**
  - `coll` (`Collection<ByteMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.ByteNFunction<? extends R, E>`) — the function that takes an array of bytes (one from each matrix) and returns a result of type R, must not be {@code null}
  - `shareIntermediateArray` (`boolean`) — {@code true} to reuse the intermediate array (sequential execution only); {@code false} to create new arrays for each position
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if any argument is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Collection, Throwables.ByteNFunction, Class), #zip(Collection, Throwables.ByteBinaryOperator)
- **Signature:** `public static <E extends Exception> IntMatrix zip(final IntMatrix a, final IntMatrix b, final Throwables.IntBinaryOperator<E> zipFunction) throws E`
- **Summary:** Combines two {@link IntMatrix} objects element-wise using a binary operator.
- **Contract:**
  - </p> <p> Both matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`IntMatrix`) — the first matrix, must not be {@code null}
  - `b` (`IntMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.IntBinaryOperator<E>`) — the binary operator to combine corresponding elements from both matrices, must not be {@code null}
- **Returns:** a new {@link IntMatrix} containing the results of applying the function to each pair of elements, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(IntMatrix, IntMatrix, IntMatrix, Throwables.IntTernaryOperator), #zip(Collection, Throwables.IntBinaryOperator), IntMatrix#zipWith(IntMatrix, Throwables.IntBinaryOperator)
- **Signature:** `public static <E extends Exception> IntMatrix zip(final IntMatrix a, final IntMatrix b, final IntMatrix c, final Throwables.IntTernaryOperator<E> zipFunction) throws E`
- **Summary:** Combines three {@link IntMatrix} objects element-wise using a ternary operator.
- **Contract:**
  - </p> <p> All three matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`IntMatrix`) — the first matrix, must not be {@code null}
  - `b` (`IntMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`IntMatrix`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.IntTernaryOperator<E>`) — the ternary operator to combine corresponding elements from all three matrices, must not be {@code null}
- **Returns:** a new {@link IntMatrix} containing the results of applying the function to each triple of elements, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(IntMatrix, IntMatrix, Throwables.IntBinaryOperator), #zip(Collection, Throwables.IntBinaryOperator), IntMatrix#zipWith(IntMatrix, IntMatrix, Throwables.IntTernaryOperator)
- **Signature:** `public static <E extends Exception> IntMatrix zip(final Collection<IntMatrix> coll, final Throwables.IntBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link IntMatrix} objects element-wise using a binary operator applied sequentially.
- **Contract:**
  - } </pre> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<IntMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.IntBinaryOperator<E>`) — the binary operator to combine elements sequentially, must not be {@code null}
- **Returns:** a new {@link IntMatrix} containing the combined results, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(IntMatrix, IntMatrix, Throwables.IntBinaryOperator), #zip(IntMatrix, IntMatrix, IntMatrix, Throwables.IntTernaryOperator), #zip(Collection, Throwables.IntNFunction, Class)
- **Signature:** `public static <R, E extends Exception> Matrix<R> zip(final Collection<IntMatrix> coll, final Throwables.IntNFunction<? extends R, E> zipFunction, final Class<R> targetElementType) throws E`
- **Summary:** Combines multiple {@link IntMatrix} objects element-wise using a function that operates on integer arrays.
- **Contract:**
  - </p> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<IntMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.IntNFunction<? extends R, E>`) — the function that takes an array of integers (one from each matrix) and returns a result of type R, must not be {@code null}
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Collection, Throwables.IntNFunction, boolean, Class)
- **Signature:** `public static <R, E extends Exception> Matrix<R> zip(final Collection<IntMatrix> coll, final Throwables.IntNFunction<? extends R, E> zipFunction, final boolean shareIntermediateArray, final Class<R> targetElementType) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link IntMatrix} objects element-wise using a function that operates on integer arrays, with control over intermediate array sharing.
- **Contract:**
  - The {@code shareIntermediateArray} parameter controls memory optimization: </p> <ul> <li> {@code true} and sequential execution: Reuses the same intermediate array for all positions, reducing memory allocations but requiring the zip function to not retain references to the array </li> <li> {@code false} or parallel execution: Creates a new array for each position, safer but uses more memory </li> </ul> <p> <b> Warning: </b> When {@code shareIntermediateArray} is {@code true} , the zip function must NOT store references to the array, as it will be mutated for subsequent positions.
  - Only use this optimization if the function immediately processes and discards the array.
  - </p> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<IntMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.IntNFunction<? extends R, E>`) — the function that takes an array of integers (one from each matrix) and returns a result of type R, must not be {@code null}
  - `shareIntermediateArray` (`boolean`) — {@code true} to reuse the intermediate array (sequential execution only); {@code false} to create new arrays for each position
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if any argument is {@code null}
  - `E` — if the zip function throws an exception during execution
- **Signature:** `public static <E extends Exception> LongMatrix zip(final LongMatrix a, final LongMatrix b, final Throwables.LongBinaryOperator<E> zipFunction) throws E`
- **Summary:** Combines two {@link LongMatrix} objects element-wise using a binary operator.
- **Contract:**
  - </p> <p> Both matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`LongMatrix`) — the first matrix, must not be {@code null}
  - `b` (`LongMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.LongBinaryOperator<E>`) — the binary operator to combine corresponding elements from both matrices, must not be {@code null}
- **Returns:** a new {@link LongMatrix} containing the results of applying the function to each pair of elements, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(LongMatrix, LongMatrix, LongMatrix, Throwables.LongTernaryOperator), #zip(Collection, Throwables.LongBinaryOperator), LongMatrix#zipWith(LongMatrix, Throwables.LongBinaryOperator)
- **Signature:** `public static <E extends Exception> LongMatrix zip(final LongMatrix a, final LongMatrix b, final LongMatrix c, final Throwables.LongTernaryOperator<E> zipFunction) throws E`
- **Summary:** Combines three {@link LongMatrix} objects element-wise using a ternary operator.
- **Contract:**
  - All three matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`LongMatrix`) — the first matrix, must not be {@code null}
  - `b` (`LongMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`LongMatrix`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.LongTernaryOperator<E>`) — the ternary operator to combine corresponding elements from all three matrices, must not be {@code null}
- **Returns:** a new {@link LongMatrix} containing the results of applying the function to each triple of elements, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(LongMatrix, LongMatrix, Throwables.LongBinaryOperator), #zip(Collection, Throwables.LongBinaryOperator), LongMatrix#zipWith(LongMatrix, LongMatrix, Throwables.LongTernaryOperator)
- **Signature:** `public static <E extends Exception> LongMatrix zip(final Collection<LongMatrix> coll, final Throwables.LongBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link LongMatrix} objects element-wise using a binary operator applied sequentially.
- **Contract:**
  - All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<LongMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.LongBinaryOperator<E>`) — the binary operator to combine elements sequentially, must not be {@code null}
- **Returns:** a new {@link LongMatrix} containing the combined results, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(LongMatrix, LongMatrix, Throwables.LongBinaryOperator), #zip(LongMatrix, LongMatrix, LongMatrix, Throwables.LongTernaryOperator), #zip(Collection, Throwables.LongNFunction, Class)
- **Signature:** `public static <R, E extends Exception> Matrix<R> zip(final Collection<LongMatrix> coll, final Throwables.LongNFunction<? extends R, E> zipFunction, final Class<R> targetElementType) throws E`
- **Summary:** Combines multiple {@link LongMatrix} objects element-wise using a function that operates on long arrays.
- **Parameters:**
  - `coll` (`Collection<LongMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.LongNFunction<? extends R, E>`) — the function that takes an array of longs (one from each matrix) and returns a result of type R, must not be {@code null}
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Collection, Throwables.LongNFunction, boolean, Class), #zip(Collection, Throwables.LongBinaryOperator)
- **Signature:** `public static <R, E extends Exception> Matrix<R> zip(final Collection<LongMatrix> coll, final Throwables.LongNFunction<? extends R, E> zipFunction, final boolean shareIntermediateArray, final Class<R> targetElementType) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link LongMatrix} objects element-wise using a function that operates on long arrays, with control over intermediate array sharing.
- **Contract:**
  - </p> <p> <b> Warning: </b> When {@code shareIntermediateArray} is {@code true} , the zip function must NOT store references to the array, as it will be mutated for subsequent positions.
  - Only use this optimization if the function immediately processes and discards the array.
  - </p> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<LongMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.LongNFunction<? extends R, E>`) — the function that takes an array of longs (one from each matrix) and returns a result of type R, must not be {@code null}
  - `shareIntermediateArray` (`boolean`) — {@code true} to reuse the intermediate array (sequential execution only); {@code false} to create new arrays for each position
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if any argument is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Collection, Throwables.LongNFunction, Class), #zip(Collection, Throwables.LongBinaryOperator)
- **Signature:** `public static <E extends Exception> DoubleMatrix zip(final DoubleMatrix a, final DoubleMatrix b, final Throwables.DoubleBinaryOperator<E> zipFunction) throws E`
- **Summary:** Combines two {@link DoubleMatrix} objects element-wise using a binary operator.
- **Contract:**
  - </p> <p> Both matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`DoubleMatrix`) — the first matrix, must not be {@code null}
  - `b` (`DoubleMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.DoubleBinaryOperator<E>`) — the binary operator to combine corresponding elements from both matrices, must not be {@code null}
- **Returns:** a new {@link DoubleMatrix} containing the results of applying the function to each pair of elements, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(DoubleMatrix, DoubleMatrix, DoubleMatrix, Throwables.DoubleTernaryOperator), #zip(Collection, Throwables.DoubleBinaryOperator), DoubleMatrix#zipWith(DoubleMatrix, Throwables.DoubleBinaryOperator)
- **Signature:** `public static <E extends Exception> DoubleMatrix zip(final DoubleMatrix a, final DoubleMatrix b, final DoubleMatrix c, final Throwables.DoubleTernaryOperator<E> zipFunction) throws E`
- **Summary:** Combines three {@link DoubleMatrix} objects element-wise using a ternary operator.
- **Contract:**
  - All three matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`DoubleMatrix`) — the first matrix, must not be {@code null}
  - `b` (`DoubleMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`DoubleMatrix`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.DoubleTernaryOperator<E>`) — the ternary operator to combine corresponding elements from all three matrices, must not be {@code null}
- **Returns:** a new {@link DoubleMatrix} containing the results of applying the function to each triple of elements, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(DoubleMatrix, DoubleMatrix, Throwables.DoubleBinaryOperator), #zip(Collection, Throwables.DoubleBinaryOperator), DoubleMatrix#zipWith(DoubleMatrix, DoubleMatrix, Throwables.DoubleTernaryOperator)
- **Signature:** `public static <E extends Exception> DoubleMatrix zip(final Collection<DoubleMatrix> coll, final Throwables.DoubleBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link DoubleMatrix} objects element-wise using a binary operator applied sequentially.
- **Contract:**
  - All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<DoubleMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.DoubleBinaryOperator<E>`) — the binary operator to combine elements sequentially, must not be {@code null}
- **Returns:** a new {@link DoubleMatrix} containing the combined results, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(DoubleMatrix, DoubleMatrix, Throwables.DoubleBinaryOperator), #zip(DoubleMatrix, DoubleMatrix, DoubleMatrix, Throwables.DoubleTernaryOperator), #zip(Collection, Throwables.DoubleNFunction, Class)
- **Signature:** `public static <R, E extends Exception> Matrix<R> zip(final Collection<DoubleMatrix> coll, final Throwables.DoubleNFunction<? extends R, E> zipFunction, final Class<R> targetElementType) throws E`
- **Summary:** Combines multiple {@link DoubleMatrix} objects element-wise using a function that operates on double arrays.
- **Parameters:**
  - `coll` (`Collection<DoubleMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.DoubleNFunction<? extends R, E>`) — the function that takes an array of doubles (one from each matrix) and returns a result of type R, must not be {@code null}
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Collection, Throwables.DoubleNFunction, boolean, Class), #zip(Collection, Throwables.DoubleBinaryOperator)
- **Signature:** `public static <R, E extends Exception> Matrix<R> zip(final Collection<DoubleMatrix> coll, final Throwables.DoubleNFunction<? extends R, E> zipFunction, final boolean shareIntermediateArray, final Class<R> targetElementType) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link DoubleMatrix} objects element-wise using a function that operates on double arrays, with control over intermediate array sharing.
- **Contract:**
  - </p> <p> <b> Warning: </b> When {@code shareIntermediateArray} is {@code true} , the zip function must NOT store references to the array, as it will be mutated for subsequent positions.
  - Only use this optimization if the function immediately processes and discards the array.
  - </p> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<DoubleMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.DoubleNFunction<? extends R, E>`) — the function that takes an array of doubles (one from each matrix) and returns a result of type R, must not be {@code null}
  - `shareIntermediateArray` (`boolean`) — {@code true} to reuse the intermediate array (sequential execution only); {@code false} to create new arrays for each position
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if any argument is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Collection, Throwables.DoubleNFunction, Class), #zip(Collection, Throwables.DoubleBinaryOperator)
- **Signature:** `public static <A, B, E extends Exception> Matrix<A> zip(final Matrix<A> a, final Matrix<B> b, final Throwables.BiFunction<? super A, ? super B, A, E> zipFunction) throws E`
- **Summary:** Combines two generic {@link Matrix} objects element-wise using a binary function.
- **Contract:**
  - </p> <p> Both matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`Matrix<A>`) — the first matrix, must not be {@code null}
  - `b` (`Matrix<B>`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.BiFunction<? super A, ? super B, A, E>`) — the function to combine corresponding elements from both matrices, must not be {@code null}
- **Returns:** a new {@link Matrix} of type A containing the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Matrix, Matrix, Throwables.BiFunction, Class), #zip(Matrix, Matrix, Matrix, Throwables.TriFunction), Matrix#zipWith(Matrix, Throwables.BiFunction)
- **Signature:** `public static <A, B, R, E extends Exception> Matrix<R> zip(final Matrix<A> a, final Matrix<B> b, final Throwables.BiFunction<? super A, ? super B, R, E> zipFunction, final Class<R> targetElementType) throws E`
- **Summary:** Combines two generic {@link Matrix} objects element-wise using a binary function, producing a result matrix with a potentially different element type.
- **Contract:**
  - </p> <p> Both matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`Matrix<A>`) — the first matrix, must not be {@code null}
  - `b` (`Matrix<B>`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.BiFunction<? super A, ? super B, R, E>`) — the function to combine corresponding elements from both matrices, must not be {@code null}
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Matrix, Matrix, Throwables.BiFunction), #zip(Matrix, Matrix, Matrix, Throwables.TriFunction, Class), Matrix#zipWith(Matrix, Throwables.BiFunction, Class)
- **Signature:** `public static <A, B, C, E extends Exception> Matrix<A> zip(final Matrix<A> a, final Matrix<B> b, final Matrix<C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, A, E> zipFunction) throws E`
- **Summary:** Combines three generic {@link Matrix} objects element-wise using a ternary function.
- **Contract:**
  - </p> <p> All three matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`Matrix<A>`) — the first matrix, must not be {@code null}
  - `b` (`Matrix<B>`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`Matrix<C>`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.TriFunction<? super A, ? super B, ? super C, A, E>`) — the function to combine corresponding elements from all three matrices, must not be {@code null}
- **Returns:** a new {@link Matrix} of type A containing the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Matrix, Matrix, Throwables.BiFunction), #zip(Matrix, Matrix, Matrix, Throwables.TriFunction, Class), Matrix#zipWith(Matrix, Matrix, Throwables.TriFunction)
- **Signature:** `public static <A, B, C, R, E extends Exception> Matrix<R> zip(final Matrix<A> a, final Matrix<B> b, final Matrix<C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, R, E> zipFunction, final Class<R> targetElementType) throws E`
- **Summary:** Combines three generic {@link Matrix} objects element-wise using a ternary function, producing a result matrix with a potentially different element type.
- **Contract:**
  - </p> <p> All three matrices must have identical dimensions (same number of rows and columns).
- **Parameters:**
  - `a` (`Matrix<A>`) — the first matrix, must not be {@code null}
  - `b` (`Matrix<B>`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`Matrix<C>`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.TriFunction<? super A, ? super B, ? super C, R, E>`) — the function to combine corresponding elements from all three matrices, must not be {@code null}
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Matrix, Matrix, Throwables.BiFunction, Class), #zip(Matrix, Matrix, Matrix, Throwables.TriFunction), Matrix#zipWith(Matrix, Matrix, Throwables.TriFunction, Class)
- **Signature:** `public static <T, E extends Exception> Matrix<T> zip(final Collection<Matrix<T>> coll, final Throwables.BinaryOperator<T, E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines multiple generic {@link Matrix} objects element-wise using a binary operator applied sequentially.
- **Contract:**
  - } </pre> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<Matrix<T>>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.BinaryOperator<T, E>`) — the binary operator to combine elements sequentially, must not be {@code null}
- **Returns:** a new {@link Matrix} of type T containing the combined results, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Matrix, Matrix, Throwables.BiFunction), #zip(Collection, Throwables.Function, Class)
- **Signature:** `public static <T, R, E extends Exception> Matrix<R> zip(final Collection<Matrix<T>> coll, final Throwables.Function<? super T[], R, E> zipFunction, final Class<R> targetElementType) throws E`
- **Summary:** Combines multiple generic {@link Matrix} objects element-wise using a function that operates on arrays.
- **Contract:**
  - </p> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<Matrix<T>>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.Function<? super T[], R, E>`) — the function that takes an array of values (one from each matrix) and returns a result of type R, must not be {@code null}
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Collection, Throwables.Function, boolean, Class), #zip(Collection, Throwables.BinaryOperator)
- **Signature:** `public static <T, R, E extends Exception> Matrix<R> zip(final Collection<Matrix<T>> coll, final Throwables.Function<? super T[], R, E> zipFunction, final boolean shareIntermediateArray, final Class<R> targetElementType) throws IllegalArgumentException, E`
- **Summary:** Combines multiple generic {@link Matrix} objects element-wise using a function that operates on arrays, with control over intermediate array sharing.
- **Contract:**
  - </p> <p> The {@code shareIntermediateArray} parameter controls memory optimization: </p> <ul> <li> {@code true} and sequential execution: Reuses the same intermediate array for all positions, reducing memory allocations but requiring the zip function to not retain references to the array </li> <li> {@code false} or parallel execution: Creates a new array for each position, safer but uses more memory </li> </ul> <p> <b> Warning: </b> When {@code shareIntermediateArray} is {@code true} , the zip function must NOT store references to the array, as it will be mutated for subsequent positions.
  - Only use this optimization if the function immediately processes and discards the array.
- **Parameters:**
  - `coll` (`Collection<Matrix<T>>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.Function<? super T[], R, E>`) — the function that takes an array of values (one from each matrix) and returns a result of type R, must not be {@code null}
  - `shareIntermediateArray` (`boolean`) — {@code true} to reuse the intermediate array (sequential execution only); {@code false} to create new arrays for each position
  - `targetElementType` (`Class<R>`) — the class of the result element type, must not be {@code null}
- **Returns:** a new {@link Matrix} of type R containing the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if any argument is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zip(Collection, Throwables.Function, Class), #zip(Collection, Throwables.BinaryOperator)
##### zipToInt(...) -> IntMatrix
- **Signature:** `public static <E extends Exception> IntMatrix zipToInt(final ByteMatrix a, final ByteMatrix b, final Throwables.ByteBiFunction<Integer, E> zipFunction) throws E`
- **Summary:** Combines two {@link ByteMatrix} objects element-wise using a function that returns {@code Integer} values, producing an {@link IntMatrix} .
- **Contract:**
  - </p> <p> Both matrices must have identical dimensions.
- **Parameters:**
  - `a` (`ByteMatrix`) — the first matrix, must not be {@code null}
  - `b` (`ByteMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.ByteBiFunction<Integer, E>`) — the function to combine corresponding elements, takes two bytes and returns an Integer, must not be {@code null}
- **Returns:** a new {@link IntMatrix} with the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToInt(ByteMatrix, ByteMatrix, ByteMatrix, Throwables.ByteTriFunction), #zipToInt(Collection, Throwables.ByteNFunction)
- **Signature:** `public static <E extends Exception> IntMatrix zipToInt(final ByteMatrix a, final ByteMatrix b, final ByteMatrix c, final Throwables.ByteTriFunction<Integer, E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines three {@link ByteMatrix} objects element-wise using a function that returns {@code Integer} values, producing an {@link IntMatrix} .
- **Contract:**
  - </p> <p> All three matrices must have identical dimensions.
- **Parameters:**
  - `a` (`ByteMatrix`) — the first matrix, must not be {@code null}
  - `b` (`ByteMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`ByteMatrix`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.ByteTriFunction<Integer, E>`) — the function to combine corresponding elements, takes three bytes and returns an Integer, must not be {@code null}
- **Returns:** a new {@link IntMatrix} with the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices have different shapes or if any argument is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToInt(ByteMatrix, ByteMatrix, Throwables.ByteBiFunction), #zipToInt(Collection, Throwables.ByteNFunction)
- **Signature:** `public static <E extends Exception> IntMatrix zipToInt(final Collection<ByteMatrix> coll, final Throwables.ByteNFunction<Integer, E> zipFunction) throws E`
- **Summary:** Combines multiple {@link ByteMatrix} objects element-wise using a function that returns {@code Integer} values, producing an {@link IntMatrix} .
- **Contract:**
  - </p> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<ByteMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.ByteNFunction<Integer, E>`) — the function that takes an array of bytes and returns an Integer, must not be {@code null}
- **Returns:** a new {@link IntMatrix} with the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToInt(Collection, Throwables.ByteNFunction, boolean), #zipToInt(ByteMatrix, ByteMatrix, Throwables.ByteBiFunction)
- **Signature:** `public static <E extends Exception> IntMatrix zipToInt(final Collection<ByteMatrix> coll, final Throwables.ByteNFunction<Integer, E> zipFunction, final boolean shareIntermediateArray) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link ByteMatrix} objects element-wise using a function that returns {@code Integer} values, with control over intermediate array sharing.
- **Contract:**
  - The {@code shareIntermediateArray} parameter controls memory optimization: </p> <ul> <li> {@code true} and sequential execution: Reuses the same intermediate array for all positions, reducing memory allocations but requiring the zip function to not retain references to the array </li> <li> {@code false} or parallel execution: Creates a new array for each position, safer but uses more memory </li> </ul> <p> <b> Warning: </b> When {@code shareIntermediateArray} is {@code true} , the zip function must NOT store references to the array, as it will be mutated for subsequent positions.
  - Only use this optimization if the function immediately processes and discards the array.
  - </p> <p> All matrices in the collection must have identical dimensions.
- **Parameters:**
  - `coll` (`Collection<ByteMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.ByteNFunction<Integer, E>`) — the function that takes an array of bytes and returns an Integer, must not be {@code null}
  - `shareIntermediateArray` (`boolean`) — {@code true} to reuse the intermediate array (sequential execution only); {@code false} to create new arrays for each position
- **Returns:** a new {@link IntMatrix} with the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToInt(Collection, Throwables.ByteNFunction)
##### zipToLong(...) -> LongMatrix
- **Signature:** `public static <E extends Exception> LongMatrix zipToLong(final IntMatrix a, final IntMatrix b, final Throwables.IntBiFunction<Long, E> zipFunction) throws E`
- **Summary:** Combines two {@link IntMatrix} objects element-wise using a function that returns {@code Long} values, producing a {@link LongMatrix} .
- **Contract:**
  - </p> <p> Both matrices must have identical dimensions.
- **Parameters:**
  - `a` (`IntMatrix`) — the first matrix, must not be {@code null}
  - `b` (`IntMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.IntBiFunction<Long, E>`) — the function to combine corresponding elements, takes two ints and returns a Long, must not be {@code null}
- **Returns:** a new {@link LongMatrix} with the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **Signature:** `public static <E extends Exception> LongMatrix zipToLong(final IntMatrix a, final IntMatrix b, final IntMatrix c, final Throwables.IntTriFunction<Long, E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines three {@link IntMatrix} objects element-wise using a function that returns {@code Long} values, producing a {@link LongMatrix} .
- **Contract:**
  - </p> <p> All three matrices must have identical dimensions.
- **Parameters:**
  - `a` (`IntMatrix`) — the first matrix, must not be {@code null}
  - `b` (`IntMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`IntMatrix`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.IntTriFunction<Long, E>`) — the function to combine corresponding elements, takes three ints and returns a Long, must not be {@code null}
- **Returns:** a new {@link LongMatrix} with the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices have different shapes or if any argument is {@code null}
  - `E` — if the zip function throws an exception during execution
- **Signature:** `public static <E extends Exception> LongMatrix zipToLong(final Collection<IntMatrix> coll, final Throwables.IntNFunction<Long, E> zipFunction) throws E`
- **Summary:** Combines multiple {@link IntMatrix} objects element-wise using a function that returns {@code Long} values.
- **Parameters:**
  - `coll` (`Collection<IntMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.IntNFunction<Long, E>`) — the function that takes an array of integers and returns a Long, must not be {@code null}
- **Returns:** a new {@link LongMatrix} with the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToLong(Collection, Throwables.IntNFunction, boolean)
- **Signature:** `public static <E extends Exception> LongMatrix zipToLong(final Collection<IntMatrix> coll, final Throwables.IntNFunction<Long, E> zipFunction, final boolean shareIntermediateArray) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link IntMatrix} objects element-wise using a function that returns {@code Long} values, with control over intermediate array sharing.
- **Contract:**
  - </p> <p> <b> Warning: </b> When {@code shareIntermediateArray} is {@code true} , the zip function must NOT store references to the array, as it will be mutated for subsequent positions.
- **Parameters:**
  - `coll` (`Collection<IntMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.IntNFunction<Long, E>`) — the function that takes an array of integers and returns a Long, must not be {@code null}
  - `shareIntermediateArray` (`boolean`) — {@code true} to reuse the intermediate array (sequential execution only); {@code false} to create new arrays for each position
- **Returns:** a new {@link LongMatrix} with the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
##### zipToDouble(...) -> DoubleMatrix
- **Signature:** `public static <E extends Exception> DoubleMatrix zipToDouble(final IntMatrix a, final IntMatrix b, final Throwables.IntBiFunction<Double, E> zipFunction) throws E`
- **Summary:** Combines two {@link IntMatrix} objects element-wise using a function that returns {@code Double} values, producing a {@link DoubleMatrix} .
- **Parameters:**
  - `a` (`IntMatrix`) — the first matrix, must not be {@code null}
  - `b` (`IntMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.IntBiFunction<Double, E>`) — the function to combine corresponding elements, takes two ints and returns a Double, must not be {@code null}
- **Returns:** a new {@link DoubleMatrix} with the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **Signature:** `public static <E extends Exception> DoubleMatrix zipToDouble(final IntMatrix a, final IntMatrix b, final IntMatrix c, final Throwables.IntTriFunction<Double, E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines three {@link IntMatrix} objects element-wise using a function that returns {@code Double} values, producing a {@link DoubleMatrix} .
- **Parameters:**
  - `a` (`IntMatrix`) — the first matrix, must not be {@code null}
  - `b` (`IntMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`IntMatrix`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.IntTriFunction<Double, E>`) — the function to combine corresponding elements, takes three ints and returns a Double, must not be {@code null}
- **Returns:** a new {@link DoubleMatrix} with the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices have different shapes or if any argument is {@code null}
  - `E` — if the zip function throws an exception during execution
- **Signature:** `public static <E extends Exception> DoubleMatrix zipToDouble(final Collection<IntMatrix> coll, final Throwables.IntNFunction<Double, E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link IntMatrix} objects element-wise using a function that returns {@code Double} values.
- **Parameters:**
  - `coll` (`Collection<IntMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.IntNFunction<Double, E>`) — the function that takes an array of integers and returns a Double, must not be {@code null}
- **Returns:** a new {@link DoubleMatrix} with the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToDouble(Collection, Throwables.IntNFunction, boolean)
- **Signature:** `public static <E extends Exception> DoubleMatrix zipToDouble(final Collection<IntMatrix> coll, final Throwables.IntNFunction<Double, E> zipFunction, final boolean shareIntermediateArray) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link IntMatrix} objects element-wise using a function that returns {@code Double} values, with control over intermediate array sharing.
- **Contract:**
  - </p> <p> <b> Warning: </b> When {@code shareIntermediateArray} is {@code true} , the zip function must NOT store references to the array.
- **Parameters:**
  - `coll` (`Collection<IntMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.IntNFunction<Double, E>`) — the function that takes an array of integers and returns a Double, must not be {@code null}
  - `shareIntermediateArray` (`boolean`) — {@code true} to reuse the intermediate array (sequential execution only); {@code false} to create new arrays for each position
- **Returns:** a new {@link DoubleMatrix} with the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
- **Signature:** `public static <E extends Exception> DoubleMatrix zipToDouble(final LongMatrix a, final LongMatrix b, final Throwables.LongBiFunction<Double, E> zipFunction) throws E`
- **Summary:** Combines two {@link LongMatrix} objects element-wise using a function that returns {@code Double} values, producing a {@link DoubleMatrix} .
- **Parameters:**
  - `a` (`LongMatrix`) — the first matrix, must not be {@code null}
  - `b` (`LongMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `zipFunction` (`Throwables.LongBiFunction<Double, E>`) — the function to combine corresponding elements, takes two longs and returns a Double, must not be {@code null}
- **Returns:** a new {@link DoubleMatrix} with the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToDouble(LongMatrix, LongMatrix, LongMatrix, Throwables.LongTriFunction), #zipToDouble(Collection, Throwables.LongNFunction)
- **Signature:** `public static <E extends Exception> DoubleMatrix zipToDouble(final LongMatrix a, final LongMatrix b, final LongMatrix c, final Throwables.LongTriFunction<Double, E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Combines three {@link LongMatrix} objects element-wise using a function that returns {@code Double} values, producing a {@link DoubleMatrix} .
- **Parameters:**
  - `a` (`LongMatrix`) — the first matrix, must not be {@code null}
  - `b` (`LongMatrix`) — the second matrix, must not be {@code null} and must have the same shape as {@code a}
  - `c` (`LongMatrix`) — the third matrix, must not be {@code null} and must have the same shape as {@code a} and {@code b}
  - `zipFunction` (`Throwables.LongTriFunction<Double, E>`) — the function to combine corresponding elements, takes three longs and returns a Double, must not be {@code null}
- **Returns:** a new {@link DoubleMatrix} with the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices have different shapes or if any argument is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToDouble(LongMatrix, LongMatrix, Throwables.LongBiFunction), #zipToDouble(Collection, Throwables.LongNFunction)
- **Signature:** `public static <E extends Exception> DoubleMatrix zipToDouble(final Collection<LongMatrix> coll, final Throwables.LongNFunction<Double, E> zipFunction) throws E`
- **Summary:** Combines multiple {@link LongMatrix} objects element-wise using a function that returns {@code Double} values.
- **Parameters:**
  - `coll` (`Collection<LongMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.LongNFunction<Double, E>`) — the function that takes an array of longs and returns a Double, must not be {@code null}
- **Returns:** a new {@link DoubleMatrix} with the combined values, never {@code null}
- **Throws:**
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToDouble(Collection, Throwables.LongNFunction, boolean), #zipToDouble(LongMatrix, LongMatrix, Throwables.LongBiFunction)
- **Signature:** `public static <E extends Exception> DoubleMatrix zipToDouble(final Collection<LongMatrix> coll, final Throwables.LongNFunction<Double, E> zipFunction, final boolean shareIntermediateArray) throws IllegalArgumentException, E`
- **Summary:** Combines multiple {@link LongMatrix} objects element-wise using a function that returns {@code Double} values, with control over intermediate array sharing.
- **Contract:**
  - </p> <p> <b> Warning: </b> When {@code shareIntermediateArray} is {@code true} , the zip function must NOT store references to the array.
- **Parameters:**
  - `coll` (`Collection<LongMatrix>`) — the collection of matrices to combine, must not be {@code null} or empty
  - `zipFunction` (`Throwables.LongNFunction<Double, E>`) — the function that takes an array of longs and returns a Double, must not be {@code null}
  - `shareIntermediateArray` (`boolean`) — {@code true} to reuse the intermediate array (sequential execution only); {@code false} to create new arrays for each position
- **Returns:** a new {@link DoubleMatrix} with the combined values, never {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code coll} is {@code null} , empty, if matrices have different shapes, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception during execution
- **See also:** #zipToDouble(Collection, Throwables.LongNFunction), #zipToDouble(LongMatrix, LongMatrix, Throwables.LongBiFunction)

#### Public Instance Methods
- (none)

### Class Matrix (com.landawn.abacus.matrix.Matrix)
Object matrix backed by a rectangular {@code T\[\]\[\]} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### empty(...) -> Matrix<T>
- **Signature:** `@SuppressWarnings("unchecked") public static <T> Matrix<T> empty()`
- **Summary:** Creates an empty matrix with zero rows and zero columns.
- **Parameters:**
  - (none)
- **Returns:** an empty matrix
##### of(...) -> Matrix<T>
- **Signature:** `@SafeVarargs public static <T> Matrix<T> of(final T[]... a)`
- **Summary:** Creates a Matrix from a two-dimensional array.
- **Contract:**
  - </p> <p> All rows must have the same length as the first row (rectangular array required).
  - The array must not be null.
- **Parameters:**
  - `a` (`T[][]`) — the two-dimensional array to create the matrix from (must not be null)
- **Returns:** a new Matrix containing the provided data
##### repeat(...) -> Matrix<T>
- **Signature:** `public static <T> Matrix<T> repeat(final int rowCount, final int columnCount, final T element) throws IllegalArgumentException`
- **Summary:** Creates a new matrix of the specified dimensions where every element is the provided {@code element} .
- **Contract:**
  - Because {@code element} must be non- {@code null} , this factory cannot be used to produce an empty-but-typed placeholder matrix.
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix (must be {@code >= 0} )
  - `columnCount` (`int`) — the number of columns in the new matrix (must be {@code >= 0} )
  - `element` (`T`) — the value to fill the matrix with (must not be {@code null} )
- **Returns:** a new Matrix of dimensions {@code rowCount × columnCount} filled with the specified element
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowCount} or {@code columnCount} is negative, or if {@code element} is {@code null}
##### mainDiagonal(...) -> Matrix<T>
- **Signature:** `public static <T> Matrix<T> mainDiagonal(final T[] mainDiagonal)`
- **Summary:** Creates a square diagonal matrix with the given values on the main diagonal (upper-left to lower-right).
- **Parameters:**
  - `mainDiagonal` (`T[]`) — the diagonal values (must not be null)
- **Returns:** a square matrix with the given diagonal values on the main diagonal
- **See also:** #diagonals(Object\[\], Object\[\]), #antiDiagonal(Object\[\])
##### antiDiagonal(...) -> Matrix<T>
- **Signature:** `public static <T> Matrix<T> antiDiagonal(final T[] antiDiagonal)`
- **Summary:** Creates a square diagonal matrix with the given values on the anti-diagonal (upper-right to lower-left).
- **Parameters:**
  - `antiDiagonal` (`T[]`) — the anti-diagonal values (must not be null)
- **Returns:** a square matrix with the given anti-diagonal values
- **See also:** #diagonals(Object\[\], Object\[\]), #mainDiagonal(Object\[\])
##### diagonals(...) -> Matrix<T>
- **Signature:** `@SuppressWarnings("null") public static <T> Matrix<T> diagonals(final T[] mainDiagonal, final T[] antiDiagonal) throws IllegalArgumentException`
- **Summary:** Creates a square matrix with values on both diagonals.
- **Contract:**
  - If diagonals intersect (odd dimension), the main diagonal value takes precedence.
  - At least one diagonal must be non-null, and two non-empty diagonals must have the same length.
- **Parameters:**
  - `mainDiagonal` (`T[]`) — the main diagonal values.
  - `antiDiagonal` (`T[]`) — the anti-diagonal values.
- **Returns:** a square matrix with the given diagonal values
- **Throws:**
  - `java.lang.IllegalArgumentException` — if both arrays are null, or if both diagonals are non-empty and have different lengths

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public Matrix(final T[][] a)`
- **Summary:** Constructs a {@code Matrix} backed by the supplied two-dimensional array.
- **Contract:**
  - </p> <p> The array must be rectangular (all rows must have the same length).
  - Call {@link #copy()} if you need an independently owned matrix.
- **Parameters:**
  - `a` (`T[][]`) — the two-dimensional array of elements (must not be null)
##### get(...) -> T
- **Signature:** `@MayReturnNull public T get(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** the element at position ( {@code rowIndex} , {@code columnIndex} ); may be {@code null} since {@code null} elements are permitted
- **Signature:** `@MayReturnNull public T get(final Point point)`
- **Summary:** Returns the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be {@code null} )
- **Returns:** the element at the specified point; may be {@code null} since {@code null} elements are permitted
##### set(...) -> void
- **Signature:** `public void set(final int rowIndex, final int columnIndex, final T value)`
- **Summary:** Sets the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
  - `value` (`T`) — the value to set; may be {@code null}
- **Signature:** `public void set(final Point point, final T value)`
- **Summary:** Sets the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be {@code null} )
  - `value` (`T`) — the value to set; may be {@code null}
##### valueAbove(...) -> Nullable<T>
- **Signature:** `public Nullable<T> valueAbove(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly above the specified position, if it exists.
- **Contract:**
  - Returns the element directly above the specified position, if it exists.
  - This method provides safe access without throwing an exception when at the top edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** a {@link Nullable} containing the element at position {@code (rowIndex - 1, columnIndex)} , or {@link Nullable#empty()} if {@code rowIndex == 0} . Note that a non-empty {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted in the matrix.
##### valueBelow(...) -> Nullable<T>
- **Signature:** `public Nullable<T> valueBelow(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly below the specified position, if it exists.
- **Contract:**
  - Returns the element directly below the specified position, if it exists.
  - This method provides safe access without throwing an exception when at the bottom edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** a {@link Nullable} containing the element at position {@code (rowIndex + 1, columnIndex)} , or {@link Nullable#empty()} if {@code rowIndex == rowCount - 1} . Note that a non-empty {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted in the matrix.
##### valueLeft(...) -> Nullable<T>
- **Signature:** `public Nullable<T> valueLeft(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the left of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the left of the specified position, if it exists.
  - This method provides safe access without throwing an exception when at the leftmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** a {@link Nullable} containing the element at position {@code (rowIndex, columnIndex - 1)} , or {@link Nullable#empty()} if {@code columnIndex == 0} . Note that a non-empty {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted in the matrix.
##### valueRight(...) -> Nullable<T>
- **Signature:** `public Nullable<T> valueRight(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the right of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the right of the specified position, if it exists.
  - This method provides safe access without throwing an exception when at the rightmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** a {@link Nullable} containing the element at position {@code (rowIndex, columnIndex + 1)} , or {@link Nullable#empty()} if {@code columnIndex == columnCount - 1} . Note that a non-empty {@code Nullable} may itself contain {@code null} since {@code null} elements are permitted in the matrix.
##### rowView(...) -> T\[\]
- **Signature:** `@Override public T[] rowView(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as an array.
- **Contract:**
  - If you need an independent copy, use {@link #rowCopy(int)} or call {@code .clone()} on the returned array.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code Matrix<String> matrix = Matrix.of(new String\[\]\[\] {{"A", "B"}, {"C", "D"}}); String\[\] rowData = matrix.rowView(0); rowData\[0\] = "X"; // This modifies the matrix directly // Matrix is now: \[\["X", "B"\], \["C", "D"\]\] // Use clone() if you need an independent copy String\[\] rowCopy = matrix.rowView(1).clone(); rowCopy\[0\] = "Y"; // Does not affect the matrix } </pre>
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the live internal row array
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowIndex} is negative or greater than or equal to {@code rowCount}
##### rowCopy(...) -> T\[\]
- **Signature:** `@Override public T[] rowCopy(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive (shallow) copy of the specified row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new array containing the values from the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowIndex} is negative or greater than or equal to {@code rowCount}
- **See also:** #rowView(int)
##### columnCopy(...) -> T\[\]
- **Signature:** `@Override public T[] columnCopy(final int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a copy of the specified column as a new array.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new array containing the values from the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code columnIndex} is negative or greater than or equal to {@code columnCount}
##### setRow(...) -> void
- **Signature:** `public void setRow(final int rowIndex, final T[] row) throws IllegalArgumentException`
- **Summary:** Replaces an entire row with values from the given array.
- **Contract:**
  - The array must have the same length as the number of columns in this matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index to replace (0-based)
  - `row` (`T[]`) — the new row data (must have exactly {@code columnCount} elements)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code row} is {@code null} , if {@code rowIndex} is out of bounds, or if {@code row.length} does not equal {@code columnCount}
##### setColumn(...) -> void
- **Signature:** `public void setColumn(final int columnIndex, final T[] column) throws IllegalArgumentException`
- **Summary:** Replaces an entire column with values from the given array.
- **Contract:**
  - The array must have the same length as the number of rows in this matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the column index to replace (0-based)
  - `column` (`T[]`) — the new column data (must have exactly {@code rowCount} elements)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code column} is {@code null} , if {@code columnIndex} is out of bounds, or if {@code column.length} does not equal {@code rowCount}
##### updateRow(...) -> void
- **Signature:** `public <E extends Exception> void updateRow(final int rowIndex, final Throwables.UnaryOperator<T, E> operator) throws E`
- **Summary:** Updates all elements in the specified row by applying the given operator.
- **Parameters:**
  - `rowIndex` (`int`) — the row index to update (0-based)
  - `operator` (`Throwables.UnaryOperator<T, E>`) — the operator to apply to each element (must not be {@code null} )
- **Throws:**
  - `E` — if the operator throws an exception
##### updateColumn(...) -> void
- **Signature:** `public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.UnaryOperator<T, E> operator) throws E`
- **Summary:** Updates all elements in the specified column by applying the given operator.
- **Parameters:**
  - `columnIndex` (`int`) — the column index to update (0-based)
  - `operator` (`Throwables.UnaryOperator<T, E>`) — the operator to apply to each element (must not be {@code null} )
- **Throws:**
  - `E` — if the operator throws an exception
##### getMainDiagonal(...) -> T\[\]
- **Signature:** `@Override public T[] getMainDiagonal() throws IllegalStateException`
- **Summary:** Returns the main diagonal elements (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a new array containing the diagonal elements from top-left to bottom-right
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setMainDiagonal(...) -> void
- **Signature:** `@Override public void setMainDiagonal(final T[] mainDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `mainDiagonal` (`T[]`) — the new values for the main diagonal; must have length equal to {@code rowCount} (treated as length 0 if {@code null} )
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if {@code mainDiagonal} array length does not equal {@code rowCount} (including when it is {@code null} and {@code rowCount > 0} )
##### updateMainDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateMainDiagonal(final Throwables.UnaryOperator<T, E> operator) throws IllegalStateException, E`
- **Summary:** Updates the main diagonal elements (upper-left to lower-right) by applying the given operator.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - `operator` (`Throwables.UnaryOperator<T, E>`) — the operator to apply to each diagonal element (must not be null)
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `E` — if the operator throws an exception
##### getAntiDiagonal(...) -> T\[\]
- **Signature:** `@Override public T[] getAntiDiagonal() throws IllegalStateException`
- **Summary:** Returns the anti-diagonal elements (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a new array containing the anti-diagonal elements from top-right to bottom-left
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setAntiDiagonal(...) -> void
- **Signature:** `@Override public void setAntiDiagonal(final T[] antiDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `antiDiagonal` (`T[]`) — the new values for the anti-diagonal; must have length equal to {@code rowCount} (treated as length 0 if {@code null} )
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if {@code antiDiagonal} array length does not equal {@code rowCount} (including when it is {@code null} and {@code rowCount > 0} )
##### updateAntiDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateAntiDiagonal(final Throwables.UnaryOperator<T, E> operator) throws IllegalStateException, E`
- **Summary:** Updates the anti-diagonal elements (upper-right to lower-left) by applying the given operator.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - `operator` (`Throwables.UnaryOperator<T, E>`) — the operator to apply to each anti-diagonal element (must not be null)
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `E` — if the operator throws an exception
##### updateAll(...) -> void
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.UnaryOperator<T, E> operator) throws E`
- **Summary:** Updates all elements in the matrix by applying the given operator.
- **Parameters:**
  - `operator` (`Throwables.UnaryOperator<T, E>`) — the operator to apply to each element (must not be null)
- **Throws:**
  - `E` — if the operator throws an exception
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends T, E> mapper) throws E`
- **Summary:** Updates all elements in the matrix based on their position.
- **Parameters:**
  - `mapper` (`Throwables.IntBiFunction<? extends T, E>`) — the function that takes row and column indices and returns the new value (must not be null)
- **Throws:**
  - `E` — if the mapper throws an exception
##### replaceIf(...) -> void
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.Predicate<? super T, E> predicate, final T newValue) throws E`
- **Summary:** Replaces all elements that match the predicate with the new value.
- **Parameters:**
  - `predicate` (`Throwables.Predicate<? super T, E>`) — the condition to test each element (must not be null)
  - `newValue` (`T`) — the value to use as replacement (can be null)
- **Throws:**
  - `E` — if the predicate throws an exception
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final T newValue) throws E`
- **Summary:** Replaces elements based on their position using a predicate.
- **Parameters:**
  - `predicate` (`Throwables.IntBiPredicate<E>`) — the condition based on position (must not be null)
  - `newValue` (`T`) — the value to use as replacement (can be null)
- **Throws:**
  - `E` — if the predicate throws an exception
##### map(...) -> Matrix<T>
- **Signature:** `public <E extends Exception> Matrix<T> map(final Throwables.UnaryOperator<T, E> mapper) throws E`
- **Summary:** Creates a new matrix by applying a transformation function to each element.
- **Parameters:**
  - `mapper` (`Throwables.UnaryOperator<T, E>`) — the transformation function (must not be {@code null} )
- **Returns:** a new matrix with transformed elements
- **Throws:**
  - `E` — if the function throws an exception
- **Signature:** `public <R, E extends Exception> Matrix<R> map(final Throwables.Function<? super T, R, E> mapper, final Class<R> targetElementType) throws E`
- **Summary:** Creates a new matrix by applying a transformation function to each element.
- **Contract:**
  - The target element type must be explicitly specified.
- **Parameters:**
  - `mapper` (`Throwables.Function<? super T, R, E>`) — the transformation function (must not be {@code null} )
  - `targetElementType` (`Class<R>`) — the class of the result element type (must not be {@code null} )
- **Returns:** a new matrix with transformed elements
- **Throws:**
  - `E` — if the function throws an exception
##### mapToBoolean(...) -> BooleanMatrix
- **Signature:** `public <E extends Exception> BooleanMatrix mapToBoolean(final Throwables.ToBooleanFunction<? super T, E> mapper) throws E`
- **Summary:** Creates a {@link BooleanMatrix} by applying a boolean-valued function to each element.
- **Contract:**
  - The mapper receives each element (which may be {@code null} ) and must return a primitive {@code boolean} .
  - <p> <b> Usage Examples: </b> </p> <pre> {@code Matrix<String> matrix = Matrix.of(new String\[\]\[\] {{"a", null}, {null, "b"}}); // Check for null values BooleanMatrix nullMask = matrix.mapToBoolean(x -> x == null); Matrix<Integer> numMatrix = Matrix.of(new Integer\[\]\[\] {{1, -2}, {3, -4}}); // Check if numbers are positive BooleanMatrix positive = numMatrix.mapToBoolean(x -> x > 0); } </pre>
- **Parameters:**
  - `mapper` (`Throwables.ToBooleanFunction<? super T, E>`) — the function that returns a boolean for each element (must not be {@code null} )
- **Returns:** a new {@link BooleanMatrix} with the same dimensions as this matrix
- **Throws:**
  - `E` — if the function throws an exception
##### mapToByte(...) -> ByteMatrix
- **Signature:** `public <E extends Exception> ByteMatrix mapToByte(final Throwables.ToByteFunction<? super T, E> mapper) throws E`
- **Summary:** Creates a byte matrix by applying a byte-valued function to each element.
- **Parameters:**
  - `mapper` (`Throwables.ToByteFunction<? super T, E>`) — the function that returns a byte for each element (must not be {@code null} )
- **Returns:** a new {@link ByteMatrix}
- **Throws:**
  - `E` — if the function throws an exception
##### mapToChar(...) -> CharMatrix
- **Signature:** `public <E extends Exception> CharMatrix mapToChar(final Throwables.ToCharFunction<? super T, E> mapper) throws E`
- **Summary:** Creates a char matrix by applying a char-valued function to each element.
- **Parameters:**
  - `mapper` (`Throwables.ToCharFunction<? super T, E>`) — the function that returns a char for each element (must not be {@code null} )
- **Returns:** a new {@link CharMatrix}
- **Throws:**
  - `E` — if the function throws an exception
##### mapToShort(...) -> ShortMatrix
- **Signature:** `public <E extends Exception> ShortMatrix mapToShort(final Throwables.ToShortFunction<? super T, E> mapper) throws E`
- **Summary:** Creates a short matrix by applying a short-valued function to each element.
- **Parameters:**
  - `mapper` (`Throwables.ToShortFunction<? super T, E>`) — the function that returns a short for each element (must not be {@code null} )
- **Returns:** a new {@link ShortMatrix}
- **Throws:**
  - `E` — if the function throws an exception
##### mapToInt(...) -> IntMatrix
- **Signature:** `public <E extends Exception> IntMatrix mapToInt(final Throwables.ToIntFunction<? super T, E> mapper) throws E`
- **Summary:** Creates an int matrix by applying an int-valued function to each element.
- **Parameters:**
  - `mapper` (`Throwables.ToIntFunction<? super T, E>`) — the function that returns an int for each element (must not be {@code null} )
- **Returns:** a new {@link IntMatrix}
- **Throws:**
  - `E` — if the function throws an exception
##### mapToLong(...) -> LongMatrix
- **Signature:** `public <E extends Exception> LongMatrix mapToLong(final Throwables.ToLongFunction<? super T, E> mapper) throws E`
- **Summary:** Creates a long matrix by applying a long-valued function to each element.
- **Parameters:**
  - `mapper` (`Throwables.ToLongFunction<? super T, E>`) — the function that returns a long for each element (must not be {@code null} )
- **Returns:** a new {@link LongMatrix}
- **Throws:**
  - `E` — if the function throws an exception
##### mapToFloat(...) -> FloatMatrix
- **Signature:** `public <E extends Exception> FloatMatrix mapToFloat(final Throwables.ToFloatFunction<? super T, E> mapper) throws E`
- **Summary:** Creates a float matrix by applying a float-valued function to each element.
- **Parameters:**
  - `mapper` (`Throwables.ToFloatFunction<? super T, E>`) — the function that returns a float for each element (must not be {@code null} )
- **Returns:** a new {@link FloatMatrix}
- **Throws:**
  - `E` — if the function throws an exception
##### mapToDouble(...) -> DoubleMatrix
- **Signature:** `public <E extends Exception> DoubleMatrix mapToDouble(final Throwables.ToDoubleFunction<? super T, E> mapper) throws E`
- **Summary:** Creates a double matrix by applying a double-valued function to each element.
- **Parameters:**
  - `mapper` (`Throwables.ToDoubleFunction<? super T, E>`) — the function that returns a double for each element (must not be {@code null} )
- **Returns:** a new {@link DoubleMatrix}
- **Throws:**
  - `E` — if the function throws an exception
##### fill(...) -> void
- **Signature:** `public void fill(final T value)`
- **Summary:** Fills all elements in the matrix with the specified value.
- **Parameters:**
  - `value` (`T`) — the value to fill the matrix with (can be null)
- **Signature:** `public void fill(final T[][] source)`
- **Summary:** Copies values into the matrix from another two-dimensional array.
- **Contract:**
  - If the source array is larger than this matrix, extra data is ignored.
  - If the source array is smaller than this matrix, the remaining cells are unchanged.
- **Parameters:**
  - `source` (`T[][]`) — the source two-dimensional array to copy values from (must not be null)
- **Signature:** `public void fill(final int destRowIndex, final int destColumnIndex, final T[][] source) throws IllegalArgumentException`
- **Summary:** Copies values into the matrix from another two-dimensional array starting at the specified position.
- **Contract:**
  - If the source data extends beyond the matrix bounds, it is truncated.
- **Parameters:**
  - `destRowIndex` (`int`) — the target row index (0-based, must be between 0 and rowCount inclusive)
  - `destColumnIndex` (`int`) — the target column index (0-based, must be between 0 and columnCount inclusive)
  - `source` (`T[][]`) — the source two-dimensional array to copy values from (must not be null)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code source} is {@code null} , or if the target indices are negative or exceed matrix dimensions
##### copy(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is a copy of this matrix with full independence guarantee
- **Signature:** `@Override public Matrix<T> copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a row range from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new Matrix containing the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
- **Signature:** `@Override public Matrix<T> copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a submatrix defined by row and column ranges.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new Matrix containing the specified submatrix
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### resize(...) -> Matrix<T>
- **Signature:** `public Matrix<T> resize(final int newRowCount, final int newColumnCount)`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code null} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
- **Returns:** a new Matrix with the specified dimensions
- **See also:** #resize(int, int, Object), #extend(int, int, int, int)
- **Signature:** `public Matrix<T> resize(final int newRowCount, final int newColumnCount, final T defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code defaultValue} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
  - `defaultValue` (`T`) — the value used to fill any newly created cells; may be {@code null}
- **Returns:** a new Matrix with the specified dimensions
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code newRowCount} or {@code newColumnCount} is negative, or if the resulting element count would overflow {@code Integer.MAX_VALUE}
- **See also:** #resize(int, int), #extend(int, int, int, int, Object)
##### extend(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> extend(final int padTop, final int padBottom, final int padLeft, final int padRight)`
- **Summary:** Returns a new matrix formed by adding {@code null} -filled padding around every edge of this matrix.
- **Contract:**
  - Use {@code resize} when you need exact output dimensions regardless of the original size.
- **Parameters:**
  - `padTop` (`int`) — number of rows to add above; must be {@code >= 0}
  - `padBottom` (`int`) — number of rows to add below; must be {@code >= 0}
  - `padLeft` (`int`) — number of columns to add to the left; must be {@code >= 0}
  - `padRight` (`int`) — number of columns to add to the right; must be {@code >= 0}
- **Returns:** a new Matrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
- **See also:** #extend(int, int, int, int, Object), #resize(int, int)
- **Signature:** `public Matrix<T> extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final T defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix formed by adding {@code defaultValue} -filled padding around every edge of this matrix.
- **Parameters:**
  - `padTop` (`int`) — number of rows to add above; must be {@code >= 0}
  - `padBottom` (`int`) — number of rows to add below; must be {@code >= 0}
  - `padLeft` (`int`) — number of columns to add to the left; must be {@code >= 0}
  - `padRight` (`int`) — number of columns to add to the right; must be {@code >= 0}
  - `defaultValue` (`T`) — the value used to fill all newly added cells; may be {@code null}
- **Returns:** a new Matrix with dimensions {@code (padTop+rowCount+padBottom) × (padLeft+columnCount+padRight)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any padding parameter is negative, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** #extend(int, int, int, int), #resize(int, int, Object)
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `@Override public void flipHorizontallyInPlace()`
- **Summary:** Reverses the order of elements in each row (horizontal flip).
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally()
##### flipVerticallyInPlace(...) -> void
- **Signature:** `@Override public void flipVerticallyInPlace()`
- **Summary:** Reverses the order of rows in the matrix (vertical flip).
- **Contract:**
  - It swaps row references rather than individual elements, so it remains correct even when rows have different runtime component types.
- **Parameters:**
  - (none)
- **See also:** #flipVertically()
##### flipHorizontally(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> flipHorizontally()`
- **Summary:** Creates a horizontally flipped copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new horizontally flipped matrix
- **See also:** #flipHorizontallyInPlace(), #flipVertically(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### flipVertically(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> flipVertically()`
- **Summary:** Creates a vertically flipped copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new vertically flipped matrix
- **See also:** #flipVerticallyInPlace(), #flipHorizontally(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### rotate90(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 90 degrees clockwise
##### rotate180(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 180 degrees clockwise
##### rotate270(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is this matrix rotated 270 degrees clockwise
##### transpose(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> transpose()`
- **Summary:** Returns a new matrix that is the transpose of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new matrix that is the transpose of this matrix, with dimensions {@code columnCount × rowCount} (an empty matrix with zero columns yields an empty {@code 0} -row matrix)
##### reshape(...) -> Matrix<T>
- **Signature:** `@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG") @Override public Matrix<T> reshape(final int newRowCount, final int newColumnCount)`
- **Summary:** Reshapes this matrix to have the specified dimensions.
- **Contract:**
  - The new shape must have at least as many total elements as the original ( {@code newRowCount * newColumnCount >= elementCount()} ).
  - If the new shape has more elements, the extra positions are filled with {@code null} .
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix (must be non-negative)
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix (must be non-negative)
- **Returns:** a new Matrix with the specified dimensions
##### repeatElements(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats each element in the matrix by the specified number of times in both directions.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat each element in the row direction (must be &gt; = 1)
  - `columnRepeats` (`int`) — number of times to repeat each element in the column direction (must be &gt; = 1)
- **Returns:** a new matrix with repeated elements, with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowRepeats &lt; 1 or columnRepeats &lt; 1, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** <a href="https://www.mathworks.com/help/matlab/ref/repelem.html">,MATLAB repelem function,</a>
##### repeatMatrix(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats the entire matrix as a tile pattern by the specified number of times.
- **Parameters:**
  - `rowRepeats` (`int`) — number of times to repeat the matrix in the row direction (must be &gt; = 1)
  - `columnRepeats` (`int`) — number of times to repeat the matrix in the column direction (must be &gt; = 1)
- **Returns:** a new matrix with the original matrix repeated, with dimensions {@code (rowCount * rowRepeats) × (columnCount * columnRepeats)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowRepeats &lt; 1 or columnRepeats &lt; 1, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** <a href="https://www.mathworks.com/help/matlab/ref/repmat.html">,MATLAB repmat function,</a>
##### flatten(...) -> List<T>
- **Signature:** `@Override public List<T> flatten()`
- **Summary:** Returns a list containing all matrix elements in row-major order.
- **Parameters:**
  - (none)
- **Returns:** a list of all elements in row-major order, with size equal to {@code rowCount * columnCount}
##### mutateAsFlat(...) -> void
- **Signature:** `@Override public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super T[], E> action) throws E`
- **Summary:** Applies an operation to the flattened (row-major order) view of this matrix.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super T[], E>`) — the operation to apply to the flattened array
- **Throws:**
  - `E` — if the operation throws an exception
- **See also:** Arrays.ff#mutateAsFlat(Object\[\]\[\], Throwables.Consumer)
##### stackVertically(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> stackVertically(final Matrix<T> other) throws IllegalArgumentException`
- **Summary:** Vertically stacks this matrix with another matrix.
- **Contract:**
  - The matrices must have the same number of columns.
- **Parameters:**
  - `other` (`Matrix<T>`) — the matrix to stack below this matrix (must not be null)
- **Returns:** a new vertically stacked matrix with dimensions (this.rowCount + other.rowCount) × columnCount
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , the matrices have different column counts, or the merged row count would overflow {@code Integer.MAX_VALUE}
- **See also:** #stackHorizontally(Matrix), IntMatrix#stackVertically(IntMatrix)
##### stackHorizontally(...) -> Matrix<T>
- **Signature:** `@Override public Matrix<T> stackHorizontally(final Matrix<T> other) throws IllegalArgumentException`
- **Summary:** Horizontally stacks this matrix with another matrix.
- **Contract:**
  - The matrices must have the same number of rows.
- **Parameters:**
  - `other` (`Matrix<T>`) — the matrix to stack to the right of this matrix (must not be null)
- **Returns:** a new horizontally stacked matrix with dimensions rowCount × (this.columnCount + other.columnCount)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , the matrices have different row counts, or the merged column count would overflow {@code Integer.MAX_VALUE}
- **See also:** #stackVertically(Matrix), IntMatrix#stackHorizontally(IntMatrix)
##### zipWith(...) -> Matrix<T>
- **Signature:** `public <B, E extends Exception> Matrix<T> zipWith(final Matrix<B> other, final Throwables.BiFunction<? super T, ? super B, T, E> zipFunction) throws E`
- **Summary:** Combines this matrix with another matrix element-wise using the specified function.
- **Contract:**
  - Both matrices must have the same dimensions.
- **Parameters:**
  - `other` (`Matrix<B>`) — the other matrix to zip with (must have the same dimensions, must not be null)
  - `zipFunction` (`Throwables.BiFunction<? super T, ? super B, T, E>`) — the binary function to apply to corresponding elements (must not be null)
- **Returns:** a new matrix with the results of the zip function
- **Throws:**
  - `E` — if the zip function throws an exception
- **Signature:** `public <B, R, E extends Exception> Matrix<R> zipWith(final Matrix<B> other, final Throwables.BiFunction<? super T, ? super B, R, E> zipFunction, final Class<R> targetElementType) throws IllegalArgumentException, E`
- **Summary:** Combines this matrix with another matrix element-wise using the specified function.
- **Contract:**
  - The matrices must have the same dimensions.
- **Parameters:**
  - `other` (`Matrix<B>`) — the other matrix to zip with (must have the same dimensions, must not be null)
  - `zipFunction` (`Throwables.BiFunction<? super T, ? super B, R, E>`) — the function to apply to corresponding elements (must not be null)
  - `targetElementType` (`Class<R>`) — the class of the result element type (must not be null)
- **Returns:** a new matrix with the results of the zip function
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices don't have the same shape, or if {@code zipFunction} or {@code targetElementType} is {@code null}
  - `E` — if the zip function throws an exception
- **Signature:** `public <B, C, E extends Exception> Matrix<T> zipWith(final Matrix<B> other, final Matrix<C> third, final Throwables.TriFunction<? super T, ? super B, ? super C, T, E> zipFunction) throws E`
- **Summary:** Combines three matrices element-wise using the specified ternary function.
- **Contract:**
  - All matrices must have the same dimensions.
- **Parameters:**
  - `other` (`Matrix<B>`) — the second matrix to zip with (must have the same dimensions, must not be null)
  - `third` (`Matrix<C>`) — the third matrix to zip with (must have the same dimensions, must not be null)
  - `zipFunction` (`Throwables.TriFunction<? super T, ? super B, ? super C, T, E>`) — the function to apply to corresponding elements (must not be null)
- **Returns:** a new matrix with the results of the zip function
- **Throws:**
  - `E` — if the zip function throws an exception
- **Signature:** `public <B, C, R, E extends Exception> Matrix<R> zipWith(final Matrix<B> other, final Matrix<C> third, final Throwables.TriFunction<? super T, ? super B, ? super C, R, E> zipFunction, final Class<R> targetElementType) throws IllegalArgumentException, E`
- **Summary:** Combines three matrices element-wise using the specified ternary function.
- **Contract:**
  - All matrices must have the same dimensions.
- **Parameters:**
  - `other` (`Matrix<B>`) — the second matrix to zip with (must have the same dimensions, must not be null)
  - `third` (`Matrix<C>`) — the third matrix to zip with (must have the same dimensions, must not be null)
  - `zipFunction` (`Throwables.TriFunction<? super T, ? super B, ? super C, R, E>`) — the function to apply to corresponding elements (must not be null)
  - `targetElementType` (`Class<R>`) — the class of the result element type (must not be null)
- **Returns:** a new matrix with the results of the zip function
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices don't have the same shape, or if {@code zipFunction} or {@code targetElementType} is {@code null}
  - `E` — if the zip function throws an exception
##### mainDiagonalStream(...) -> Stream<T>
- **Signature:** `@Override public Stream<T> mainDiagonalStream()`
- **Summary:** Returns a stream of elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a {@link Stream} of diagonal elements from top-left to bottom-right, or an empty stream if the matrix is empty
##### antiDiagonalStream(...) -> Stream<T>
- **Signature:** `@Override public Stream<T> antiDiagonalStream()`
- **Summary:** Returns a stream of elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a {@link Stream} of anti-diagonal elements from top-right to bottom-left, or an empty stream if the matrix is empty
##### horizontalStream(...) -> Stream<T>
- **Signature:** `@Override public Stream<T> horizontalStream()`
- **Summary:** Returns a stream of all elements in row-major order (horizontal).
- **Parameters:**
  - (none)
- **Returns:** a {@link Stream} of all elements in row-major order, or an empty stream if the matrix is empty
- **Signature:** `@Override public Stream<T> horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a single row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to stream
- **Returns:** a {@link Stream} of elements from the specified row
- **Signature:** `@Override public Stream<T> horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of rows in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a {@link Stream} of elements from the specified row range, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### verticalStream(...) -> Stream<T>
- **Signature:** `@Override @Beta public Stream<T> verticalStream()`
- **Summary:** Returns a stream of all elements in column-major order (vertical).
- **Parameters:**
  - (none)
- **Returns:** a {@link Stream} of all elements in column-major order, or an empty stream if the matrix is empty
- **Signature:** `@Override public Stream<T> verticalStream(final int columnIndex)`
- **Summary:** Returns a stream of elements from a single column.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to stream
- **Returns:** a {@link Stream} of elements from the specified column
- **Signature:** `@Beta @Override public Stream<T> verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of columns in column-major order.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a {@link Stream} of elements from the specified column range, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### rowStreams(...) -> Stream<Stream<T>>
- **Signature:** `@Override public Stream<Stream<T>> rowStreams()`
- **Summary:** Returns a stream of streams, where each inner stream represents a row.
- **Parameters:**
  - (none)
- **Returns:** a {@link Stream} of row streams, with one inner stream per row in the matrix
- **Signature:** `@Override public Stream<Stream<T>> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of streams for a range of rows.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a {@link Stream} of row streams for the specified range, with one inner stream per row
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### columnStreams(...) -> Stream<Stream<T>>
- **Signature:** `@Override @Beta public Stream<Stream<T>> columnStreams()`
- **Summary:** Returns a stream of streams, where each inner stream represents a column.
- **Parameters:**
  - (none)
- **Returns:** a {@link Stream} of column streams, or an empty stream if the matrix is empty
- **Signature:** `@Override @Beta public Stream<Stream<T>> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of streams for a range of columns.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a {@link Stream} of column streams for the specified range, or an empty stream if the matrix is empty
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
##### forEach(...) -> void
- **Signature:** `public <E extends Exception> void forEach(final Throwables.Consumer<? super T, E> action) throws E`
- **Summary:** Applies the given action to each element in the matrix.
- **Contract:**
  - Elements are processed in row-major order (row by row, left to right) when executed sequentially.
  - If parallelized, the order of execution is not guaranteed, but all elements will be processed exactly once.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super T, E>`) — the action to be performed for each element; receives each element value
- **Throws:**
  - `E` — if the action throws an exception
- **See also:** #forEach(int, int, int, int, Throwables.Consumer)
- **Signature:** `public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.Consumer<? super T, E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Applies the given action to each element in the specified sub-matrix region.
- **Contract:**
  - The operation may be parallelized internally if the sub-matrix is large enough to benefit from parallel processing.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.Consumer<? super T, E>`) — the action to be performed for each element; receives each element value
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if indices are out of bounds
  - `E` — if the action throws an exception
##### toRowDataset(...) -> Dataset
- **Signature:** `@Beta public Dataset toRowDataset(final Collection<String> columnNames) throws IllegalArgumentException`
- **Summary:** Converts this matrix to a Dataset with horizontally organized data.
- **Contract:**
  - <p> The column names are used in the order they appear in the collection, and must match the number of columns in the matrix exactly.
- **Parameters:**
  - `columnNames` (`Collection<String>`) — the names to assign to each column in the resulting Dataset; size must equal {@code columnCount}
- **Returns:** a Dataset containing the matrix data with the specified column names (one row per matrix row)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code columnNames} is {@code null} , or if its size does not equal {@code columnCount}
- **See also:** Dataset, #toColumnDataset(Collection)
##### toColumnDataset(...) -> Dataset
- **Signature:** `@Beta public Dataset toColumnDataset(final Collection<String> columnNames) throws IllegalArgumentException`
- **Summary:** Converts this matrix to a Dataset with vertically organized data.
- **Contract:**
  - Each row in this matrix becomes a column in the resulting Dataset, so the supplied names are assigned to the Dataset's columns in the order they appear in the collection and must match this matrix's {@code rowCount} exactly.
- **Parameters:**
  - `columnNames` (`Collection<String>`) — the column names of the resulting Dataset; size must equal {@code rowCount}
- **Returns:** a Dataset containing the matrix data organized vertically (one column per matrix row)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code columnNames} is {@code null} , or if its size does not equal {@code rowCount}
- **See also:** Dataset, RowDataset, #toRowDataset(Collection)
##### println(...) -> String
- **Signature:** `@Override public String println()`
- **Summary:** Prints this matrix to standard output and returns the printed string.
- **Parameters:**
  - (none)
- **Returns:** the string representation that was printed to standard output
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this matrix.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this matrix
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this matrix to the specified object for equality.
- **Contract:**
  - Returns {@code true} if the given object is also a {@code Matrix} with the same dimensions and equal contents.
  - value equality, with {@code null} equal only to {@code null} ); element arrays, if any, are compared deeply.
  - Two matrices with different declared element types may compare equal if their concrete elements are pairwise {@code equals} -equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with (may be {@code null} )
- **Returns:** {@code true} if the given object is a {@code Matrix} with the same dimensions and pairwise-equal elements; {@code false} otherwise (including when {@code obj} is {@code null} or is not a {@code Matrix} )
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this matrix

### Enum ParallelMode (com.landawn.abacus.matrix.ParallelMode)
Thread-local parallelization policy consulted by {@link Matrices} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
- (none)

#### Public Instance Methods
- (none)

### Class ShortMatrix (com.landawn.abacus.matrix.ShortMatrix)
Matrix implementation backed by a rectangular {@code short\[\]\[\]} .

**Thread-safety:** unspecified
**Nullability:** unspecified

#### Public Constructors
- (none)

#### Public Static Methods
##### empty(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix empty()`
- **Summary:** Creates an empty matrix with zero rows and zero columns.
- **Parameters:**
  - (none)
- **Returns:** an empty short matrix
##### of(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix of(final short[]... a)`
- **Summary:** Creates a ShortMatrix from a two-dimensional short array.
- **Parameters:**
  - `a` (`short[][]`) — the two-dimensional short array to create the matrix from, or null/empty for an empty matrix
- **Returns:** a new ShortMatrix containing the provided data, or an empty ShortMatrix if input is null or empty
##### random(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix random(final int length)`
- **Summary:** Creates a new {@code 1 x length} matrix filled with random short values.
- **Parameters:**
  - `length` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new ShortMatrix of dimensions 1 x length filled with random values
- **Signature:** `public static ShortMatrix random(final int rowCount, final int columnCount)`
- **Summary:** Creates a new matrix of the specified dimensions filled with random short values.
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
- **Returns:** a new ShortMatrix of dimensions rowCount x columnCount filled with random values
##### repeat(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix repeat(final int rowCount, final int columnCount, final short element)`
- **Summary:** Creates a new matrix of the specified dimensions where every element is the provided {@code element} .
- **Parameters:**
  - `rowCount` (`int`) — the number of rows in the new matrix; must be {@code >= 0}
  - `columnCount` (`int`) — the number of columns in the new matrix; must be {@code >= 0}
  - `element` (`short`) — the short value to fill the matrix with
- **Returns:** a new ShortMatrix of dimensions rowCount x columnCount filled with the specified element
##### range(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix range(final short startInclusive, final short endExclusive)`
- **Summary:** Creates a 1-row ShortMatrix with values from startInclusive to endExclusive.
- **Contract:**
  - If {@code startInclusive >= endExclusive} , a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`short`) — the starting value (inclusive)
  - `endExclusive` (`short`) — the ending value (exclusive)
- **Returns:** a new 1×n ShortMatrix where n = max(0, endExclusive - startInclusive)
- **Signature:** `public static ShortMatrix range(final short startInclusive, final short endExclusive, final short step)`
- **Summary:** Creates a 1-row ShortMatrix with values from startInclusive to endExclusive with the specified step.
- **Contract:**
  - If the step would not reach endExclusive from startInclusive, a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`short`) — the starting value (inclusive)
  - `endExclusive` (`short`) — the ending value (exclusive)
  - `step` (`short`) — the step size (must not be zero; can be positive or negative)
- **Returns:** a new 1×n ShortMatrix with values incremented by the step size
##### rangeClosed(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix rangeClosed(final short startInclusive, final short endInclusive)`
- **Summary:** Creates a 1-row ShortMatrix with values from startInclusive to endInclusive.
- **Contract:**
  - If {@code startInclusive > endInclusive} , a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`short`) — the starting value (inclusive)
  - `endInclusive` (`short`) — the ending value (inclusive)
- **Returns:** a new 1×n ShortMatrix where n = max(0, endInclusive - startInclusive + 1)
- **Signature:** `public static ShortMatrix rangeClosed(final short startInclusive, final short endInclusive, final short step)`
- **Summary:** Creates a 1-row ShortMatrix with values from startInclusive to endInclusive with the specified step.
- **Contract:**
  - The end value is included only if it is reachable by stepping from start.
  - If the step would not reach endInclusive from startInclusive, a 1×0 matrix is returned.
- **Parameters:**
  - `startInclusive` (`short`) — the starting value (inclusive)
  - `endInclusive` (`short`) — the ending value (inclusive, if reachable by stepping)
  - `step` (`short`) — the step size (must not be zero; can be positive or negative)
- **Returns:** a new 1×n ShortMatrix with values incremented by the step size
##### mainDiagonal(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix mainDiagonal(final short[] mainDiagonal)`
- **Summary:** Creates a square matrix from the specified main diagonal elements (upper-left to lower-right).
- **Parameters:**
  - `mainDiagonal` (`short[]`) — the array of diagonal elements; may be {@code null} or empty
- **Returns:** a square {@code n x n} matrix with the specified main diagonal, or an empty matrix if {@code mainDiagonal} is {@code null} or empty
- **See also:** #antiDiagonal(short\[\]), #diagonals(short\[\], short\[\])
##### antiDiagonal(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix antiDiagonal(final short[] antiDiagonal)`
- **Summary:** Creates a square matrix from the specified anti-diagonal elements (upper-right to lower-left).
- **Parameters:**
  - `antiDiagonal` (`short[]`) — the array of anti-diagonal elements; may be {@code null} or empty
- **Returns:** a square {@code n x n} matrix with the specified anti-diagonal, or an empty matrix if {@code antiDiagonal} is {@code null} or empty
- **See also:** #mainDiagonal(short\[\]), #diagonals(short\[\], short\[\])
##### diagonals(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix diagonals(final short[] mainDiagonal, final short[] antiDiagonal) throws IllegalArgumentException`
- **Summary:** Creates a square matrix from the specified main diagonal and anti-diagonal elements.
- **Contract:**
  - If both arrays are provided, they must have the same length.
  - When both diagonals are provided and they overlap (at the center element of odd-sized matrices), the main diagonal value takes precedence.
- **Parameters:**
  - `mainDiagonal` (`short[]`) — the array of main diagonal elements (can be null or empty)
  - `antiDiagonal` (`short[]`) — the array of anti-diagonal elements (can be null or empty)
- **Returns:** a square matrix with the specified diagonals, or an empty matrix if both inputs are null or empty
- **Throws:**
  - `java.lang.IllegalArgumentException` — if both arrays are non-empty and have different lengths
##### unbox(...) -> ShortMatrix
- **Signature:** `public static ShortMatrix unbox(final Matrix<Short> x)`
- **Summary:** Converts a boxed {@code Matrix<Short>} to a primitive {@code ShortMatrix} .
- **Contract:**
  - This is particularly beneficial when working with large matrices, as primitive arrays have less memory overhead and better cache locality than arrays of wrapper objects.
- **Parameters:**
  - `x` (`Matrix<Short>`) — the boxed Short matrix to convert; must not be {@code null}
- **Returns:** a new ShortMatrix with unboxed primitive values
- **See also:** #boxed()

#### Public Instance Methods
##### <init>(...) -> void
- **Signature:** `public ShortMatrix(final short[][] a)`
- **Summary:** Constructs a {@code ShortMatrix} backed by the supplied two-dimensional array.
- **Contract:**
  - <p> If {@code a} is {@code null} , this creates an empty {@code 0x0} matrix.
  - Call {@link #copy()} if you need an independently owned matrix.
- **Parameters:**
  - `a` (`short[][]`) — the two-dimensional short array to wrap, or {@code null} for an empty matrix
##### get(...) -> short
- **Signature:** `public short get(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** the element at position (rowIndex, columnIndex)
- **Signature:** `public short get(final Point point)`
- **Summary:** Returns the element at the specified point.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
- **Returns:** the short element at the specified point
- **See also:** #get(int, int)
##### set(...) -> void
- **Signature:** `public void set(final int rowIndex, final int columnIndex, final short value)`
- **Summary:** Sets the element at the specified row and column indices.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
  - `value` (`short`) — the value to set
- **Signature:** `public void set(final Point point, final short value)`
- **Summary:** Sets the element at the specified point to the given value.
- **Parameters:**
  - `point` (`Point`) — the point containing row and column indices (must not be null)
  - `value` (`short`) — the new short value to set at the specified point
- **See also:** #set(int, int, short)
##### valueAbove(...) -> OptionalShort
- **Signature:** `public OptionalShort valueAbove(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly above the specified position, if it exists.
- **Contract:**
  - Returns the element directly above the specified position, if it exists.
  - This method provides safe access to the element directly above the given position without throwing an exception when at the top edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalShort containing the element at position (rowIndex - 1, columnIndex), or empty if rowIndex == 0
##### valueBelow(...) -> OptionalShort
- **Signature:** `public OptionalShort valueBelow(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly below the specified position, if it exists.
- **Contract:**
  - Returns the element directly below the specified position, if it exists.
  - This method provides safe access to the element directly below the given position without throwing an exception when at the bottom edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalShort containing the element at position (rowIndex + 1, columnIndex), or empty if rowIndex == rowCount - 1
##### valueLeft(...) -> OptionalShort
- **Signature:** `public OptionalShort valueLeft(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the left of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the left of the specified position, if it exists.
  - This method provides safe access to the element directly to the left of the given position without throwing an exception when at the leftmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalShort containing the element at position (rowIndex, columnIndex - 1), or empty if columnIndex == 0
##### valueRight(...) -> OptionalShort
- **Signature:** `public OptionalShort valueRight(final int rowIndex, final int columnIndex)`
- **Summary:** Returns the element directly to the right of the specified position, if it exists.
- **Contract:**
  - Returns the element directly to the right of the specified position, if it exists.
  - This method provides safe access to the element directly to the right of the given position without throwing an exception when at the rightmost edge of the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the row index (0-based)
  - `columnIndex` (`int`) — the column index (0-based)
- **Returns:** an OptionalShort containing the element at position (rowIndex, columnIndex + 1), or empty if columnIndex == columnCount - 1
##### rowView(...) -> short\[\]
- **Signature:** `@Override public short[] rowView(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns the specified row as a short array.
- **Contract:**
  - If you need an independent copy, use {@code Arrays.copyOf(matrix.rowView(i), matrix.columnCount())} .
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** the specified row array (direct reference to internal storage)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
##### rowCopy(...) -> short\[\]
- **Signature:** `@Override public short[] rowCopy(final int rowIndex) throws IllegalArgumentException`
- **Summary:** Returns a defensive copy of the specified row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to retrieve (0-based)
- **Returns:** a new short array containing the values from the specified row
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex &lt; 0 or rowIndex &gt; = rowCount
- **See also:** #rowView(int)
##### columnCopy(...) -> short\[\]
- **Signature:** `@Override public short[] columnCopy(final int columnIndex) throws IllegalArgumentException`
- **Summary:** Returns a copy of the specified column as a new short array.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to retrieve (0-based)
- **Returns:** a new array containing the values from the specified column
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex &lt; 0 or columnIndex &gt; = columnCount
##### setRow(...) -> void
- **Signature:** `public void setRow(final int rowIndex, final short[] row) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified row by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of columns in the matrix.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to set (0-based)
  - `row` (`short[]`) — the array of values to copy into the row; must have length equal to the number of columns
- **Throws:**
  - `java.lang.IllegalArgumentException` — if rowIndex is out of bounds or row length does not match column count
##### setColumn(...) -> void
- **Signature:** `public void setColumn(final int columnIndex, final short[] column) throws IllegalArgumentException`
- **Summary:** Sets the values of the specified column by copying from the provided array.
- **Contract:**
  - The source array must have exactly the same length as the number of rows in the matrix.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to set (0-based)
  - `column` (`short[]`) — the array of values to copy into the column; must have length equal to the number of rows
- **Throws:**
  - `java.lang.IllegalArgumentException` — if columnIndex is out of bounds or column length does not match row count
##### updateRow(...) -> void
- **Signature:** `public <E extends Exception> void updateRow(final int rowIndex, final Throwables.ShortUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified row by applying the given operator to each element.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to update (0-based)
  - `operator` (`Throwables.ShortUnaryOperator<E>`) — the unary operator to apply to each element in the row, taking a short and returning a short
- **Throws:**
  - `E` — if the operator throws an exception
##### updateColumn(...) -> void
- **Signature:** `public <E extends Exception> void updateColumn(final int columnIndex, final Throwables.ShortUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the specified column by applying the given operator to each element.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to update (0-based)
  - `operator` (`Throwables.ShortUnaryOperator<E>`) — the unary operator to apply to each element in the column, taking a short and returning a short
- **Throws:**
  - `E` — if the operator throws an exception
##### getMainDiagonal(...) -> short\[\]
- **Signature:** `@Override public short[] getMainDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the main diagonal elements (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new short array containing the main diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setMainDiagonal(...) -> void
- **Signature:** `@Override public void setMainDiagonal(final short[] mainDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the main diagonal (upper-left to lower-right).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `mainDiagonal` (`short[]`) — the new values for the main diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if mainDiagonal array length does not equal rowCount
##### updateMainDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateMainDiagonal(final Throwables.ShortUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates all elements on the main diagonal from upper-left to lower-right by applying the given operator.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - `operator` (`Throwables.ShortUnaryOperator<E>`) — the operator to apply to each diagonal element
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `E` — if the operator throws an exception
##### getAntiDiagonal(...) -> short\[\]
- **Signature:** `@Override public short[] getAntiDiagonal() throws IllegalStateException`
- **Summary:** Returns a copy of the anti-diagonal elements (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount) for this operation.
- **Parameters:**
  - (none)
- **Returns:** a new short array containing the anti-diagonal elements
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
##### setAntiDiagonal(...) -> void
- **Signature:** `@Override public void setAntiDiagonal(final short[] antiDiagonal) throws IllegalStateException, IllegalArgumentException`
- **Summary:** Sets the elements on the anti-diagonal (upper-right to lower-left).
- **Contract:**
  - The matrix must be square (rowCount == columnCount), and the diagonal array must have exactly as many elements as the matrix has rows.
- **Parameters:**
  - `antiDiagonal` (`short[]`) — the new values for the anti-diagonal; must have length equal to rowCount
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `java.lang.IllegalArgumentException` — if antiDiagonal array length does not equal rowCount
##### updateAntiDiagonal(...) -> void
- **Signature:** `public <E extends Exception> void updateAntiDiagonal(final Throwables.ShortUnaryOperator<E> operator) throws IllegalStateException, E`
- **Summary:** Updates all elements on the anti-diagonal from upper-right to lower-left by applying the given operator.
- **Contract:**
  - The matrix must be square (same number of rows and columns).
- **Parameters:**
  - `operator` (`Throwables.ShortUnaryOperator<E>`) — the operator to apply to each anti-diagonal element
- **Throws:**
  - `java.lang.IllegalStateException` — if the matrix is not square (rowCount != columnCount)
  - `E` — if the operator throws an exception
##### updateAll(...) -> void
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.ShortUnaryOperator<E> operator) throws E`
- **Summary:** Updates all elements in the matrix by applying the given operator to each element.
- **Parameters:**
  - `operator` (`Throwables.ShortUnaryOperator<E>`) — the unary operator to apply to each element, taking a short and returning a short
- **Throws:**
  - `E` — if the operator throws an exception
- **Signature:** `public <E extends Exception> void updateAll(final Throwables.IntBiFunction<? extends Short, E> mapper) throws E`
- **Summary:** Updates all elements in the matrix based on their position by applying the given mapper.
- **Parameters:**
  - `mapper` (`Throwables.IntBiFunction<? extends Short, E>`) — the bi-function that takes (rowIndex, columnIndex) and returns the new short value
- **Throws:**
  - `E` — if the mapper throws an exception
##### replaceIf(...) -> void
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.ShortPredicate<E> predicate, final short newValue) throws E`
- **Summary:** Conditionally replaces elements in the matrix based on a predicate.
- **Parameters:**
  - `predicate` (`Throwables.ShortPredicate<E>`) — the condition to test each element; returns {@code true} if the element should be replaced
  - `newValue` (`short`) — the value to replace matching elements with
- **Throws:**
  - `E` — if the predicate throws an exception
- **Signature:** `public <E extends Exception> void replaceIf(final Throwables.IntBiPredicate<E> predicate, final short newValue) throws E`
- **Summary:** Conditionally replaces elements in the matrix based on their position.
- **Contract:**
  - The predicate receives the row and column indices (0-based) and returns {@code true} if the element at that position should be replaced with the new value.
- **Parameters:**
  - `predicate` (`Throwables.IntBiPredicate<E>`) — the bi-predicate that takes (rowIndex, columnIndex) and returns {@code true} if element should be replaced
  - `newValue` (`short`) — the value to replace matching elements with
- **Throws:**
  - `E` — if the predicate throws an exception
##### map(...) -> ShortMatrix
- **Signature:** `public <E extends Exception> ShortMatrix map(final Throwables.ShortUnaryOperator<E> mapper) throws E`
- **Summary:** Creates a new matrix by applying the given function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.ShortUnaryOperator<E>`) — the unary operator to apply to each element, taking a short and returning a short
- **Returns:** a new ShortMatrix with the transformed values; the original matrix is unchanged
- **Throws:**
  - `E` — if the function throws an exception
##### mapToObj(...) -> Matrix<R>
- **Signature:** `public <R, E extends Exception> Matrix<R> mapToObj(final Throwables.ShortFunction<? extends R, E> mapper, final Class<R> targetElementType) throws E`
- **Summary:** Creates a new object matrix by applying the given function to each element of this matrix.
- **Parameters:**
  - `mapper` (`Throwables.ShortFunction<? extends R, E>`) — the function to transform each short to an object of type R
  - `targetElementType` (`Class<R>`) — the class of the target element type (used for array creation)
- **Returns:** a new Matrix &lt; R &gt; with the transformed object values; the original matrix is unchanged
- **Throws:**
  - `E` — if the function throws an exception
##### fill(...) -> void
- **Signature:** `public void fill(final short value)`
- **Summary:** Fills all elements of the matrix with the specified value.
- **Parameters:**
  - `value` (`short`) — the value to fill the matrix with
- **Signature:** `public void fill(final short[][] source)`
- **Summary:** Fills the matrix with values from another two-dimensional array, starting at position (0, 0).
- **Contract:**
  - If the source array is larger, only the portion that fits is copied.
- **Parameters:**
  - `source` (`short[][]`) — the two-dimensional array to copy values from
- **Signature:** `public void fill(final int destRowIndex, final int destColumnIndex, final short[][] source) throws IllegalArgumentException`
- **Summary:** Fills a region of the matrix with values from another two-dimensional array, starting at the specified position.
- **Parameters:**
  - `destRowIndex` (`int`) — the target row index in this matrix (0-based, must be 0 &lt; = destRowIndex &lt; = rowCount)
  - `destColumnIndex` (`int`) — the target column index in this matrix (0-based, must be 0 &lt; = destColumnIndex &lt; = columnCount)
  - `source` (`short[][]`) — the source array to copy values from; must not be {@code null}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code source} is {@code null} , if destRowIndex &lt; 0 or &gt; rowCount, or if destColumnIndex &lt; 0 or &gt; columnCount
##### copy(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix copy()`
- **Summary:** Returns a copy of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a new ShortMatrix that is an independent copy of this matrix
- **Signature:** `@Override public ShortMatrix copy(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a subset of rows from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a new ShortMatrix containing an independent copy of the specified rows
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if fromRowIndex &lt; 0, toRowIndex &gt; rowCount, or fromRowIndex &gt; toRowIndex
- **Signature:** `@Override public ShortMatrix copy(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Creates a copy of a rectangular sub-region from this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a new ShortMatrix containing an independent copy of the specified rectangular region
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds or fromIndex &gt; toIndex
##### resize(...) -> ShortMatrix
- **Signature:** `public ShortMatrix resize(final int newRowCount, final int newColumnCount)`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded (excess rows removed from the bottom, excess columns removed from the right).
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code 0} .
  - Use {@code extend} when the entire original content must be preserved.
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
- **Returns:** a new ShortMatrix with the specified dimensions
- **See also:** #resize(int, int, short), #extend(int, int, int, int)
- **Signature:** `public ShortMatrix resize(final int newRowCount, final int newColumnCount, final short defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix whose dimensions are exactly {@code newRowCount × newColumnCount} , anchored at the top-left corner of this matrix.
- **Contract:**
  - <ul> <li> <b> If a dimension shrinks </b> \\u2014 elements beyond the new boundary are discarded.
  - </li> <li> <b> If a dimension grows </b> \\u2014 new cells are filled with {@code defaultValue} .
  - Use {@code extend} when the entire original content must be preserved.
  - </p> <p> <b> Usage Examples: </b> </p> <pre> {@code ShortMatrix matrix = ShortMatrix.of(new short\[\]\[\] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}}); // Grow: fill new cells with 9 ShortMatrix grown = matrix.resize(4, 4, (short) 9); // Result: \[\[1, 2, 3, 9\], // \[4, 5, 6, 9\], // \[7, 8, 9, 9\], // \[9, 9, 9, 9\]\] // Truncate: defaultValue is ignored when shrinking ShortMatrix truncated = matrix.resize(2, 2, (short) 9); // Result: \[\[1, 2\], // \[4, 5\]\] } </pre>
- **Parameters:**
  - `newRowCount` (`int`) — the row count of the returned matrix; must be {@code >= 0}
  - `newColumnCount` (`int`) — the column count of the returned matrix; must be {@code >= 0}
  - `defaultValue` (`short`) — the value used to fill cells that are added when a dimension grows; ignored when a dimension shrinks
- **Returns:** a new ShortMatrix with the specified dimensions
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code newRowCount} or {@code newColumnCount} is negative, or if {@code (long) newRowCount * newColumnCount} overflows {@code Integer.MAX_VALUE}
- **See also:** #resize(int, int), #extend(int, int, int, int, short)
##### extend(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight)`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
- **Returns:** a new ShortMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **See also:** #extend(int, int, int, int, short), #resize(int, int)
- **Signature:** `public ShortMatrix extend(final int padTop, final int padBottom, final int padLeft, final int padRight, final short defaultValue) throws IllegalArgumentException`
- **Summary:** Returns a new matrix formed by surrounding this matrix with padding on all four edges.
- **Parameters:**
  - `padTop` (`int`) — number of padding rows to add above the original matrix; must be {@code >= 0}
  - `padBottom` (`int`) — number of padding rows to add below the original matrix; must be {@code >= 0}
  - `padLeft` (`int`) — number of padding columns to add to the left of the original matrix; must be {@code >= 0}
  - `padRight` (`int`) — number of padding columns to add to the right of the original matrix; must be {@code >= 0}
  - `defaultValue` (`short`) — the value to fill all new padding cells with
- **Returns:** a new ShortMatrix with dimensions {@code (padTop + rowCount + padBottom) × (padLeft + columnCount + padRight)}
- **Throws:**
  - `java.lang.IllegalArgumentException` — if any padding parameter is negative, or if the resulting dimensions would overflow {@code Integer.MAX_VALUE}
- **See also:** #extend(int, int, int, int), #resize(int, int, short)
##### flipHorizontallyInPlace(...) -> void
- **Signature:** `@Override public void flipHorizontallyInPlace()`
- **Summary:** Reverses the order of elements in each row (horizontal flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipHorizontally()
##### flipVerticallyInPlace(...) -> void
- **Signature:** `@Override public void flipVerticallyInPlace()`
- **Summary:** Reverses the order of rows in the matrix (vertical flip in-place).
- **Parameters:**
  - (none)
- **See also:** #flipVertically()
##### flipHorizontally(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix flipHorizontally()`
- **Summary:** Returns a new matrix that is a horizontal flip of this matrix (each row reversed).
- **Parameters:**
  - (none)
- **Returns:** a new matrix with each row reversed
- **See also:** #flipHorizontallyInPlace(), #flipVertically(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### flipVertically(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix flipVertically()`
- **Summary:** Returns a new matrix that is a vertical flip of this matrix (rows in reversed order).
- **Parameters:**
  - (none)
- **Returns:** a new matrix with rows in reversed order
- **See also:** #flipVerticallyInPlace(), #flipHorizontally(), <a href="https://www.mathworks.com/help/matlab/ref/flip.html#btz149s-1">,MATLAB flip function,</a>
##### rotate90(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix rotate90()`
- **Summary:** Returns a new matrix that is this matrix rotated 90 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new ShortMatrix rotated 90 degrees clockwise with dimensions columnCount × rowCount
##### rotate180(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix rotate180()`
- **Summary:** Returns a new matrix that is this matrix rotated 180 degrees.
- **Parameters:**
  - (none)
- **Returns:** a new ShortMatrix rotated 180 degrees with the same dimensions
##### rotate270(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix rotate270()`
- **Summary:** Returns a new matrix that is this matrix rotated 270 degrees clockwise.
- **Parameters:**
  - (none)
- **Returns:** a new ShortMatrix rotated 270 degrees clockwise with dimensions columnCount × rowCount
##### transpose(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix transpose()`
- **Summary:** Returns the transpose of this matrix by swapping rows and columns.
- **Parameters:**
  - (none)
- **Returns:** a new ShortMatrix that is the transpose with dimensions columnCount × rowCount
##### reshape(...) -> ShortMatrix
- **Signature:** `@SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG") @Override public ShortMatrix reshape(final int newRowCount, final int newColumnCount)`
- **Summary:** Reshapes the matrix to new dimensions while preserving element order in row-major layout.
- **Contract:**
  - The new shape must have at least as many total elements as the original ( {@code newRowCount * newColumnCount >= elementCount()} ).
  - If the new shape has more total elements, the additional positions are filled with zeros (default value for short).
- **Parameters:**
  - `newRowCount` (`int`) — the number of rows in the reshaped matrix (must be non-negative)
  - `newColumnCount` (`int`) — the number of columns in the reshaped matrix (must be non-negative)
- **Returns:** a new ShortMatrix with the specified shape containing this matrix's elements in row-major order
##### repeatElements(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix repeatElements(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats each element in the matrix by the specified factors.
- **Parameters:**
  - `rowRepeats` (`int`) — the number of times to repeat each element in the row direction (must be positive)
  - `columnRepeats` (`int`) — the number of times to repeat each element in the column direction (must be positive)
- **Returns:** a new ShortMatrix with dimensions (rowCount * rowRepeats) × (columnCount * columnRepeats)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is less than or equal to 0
- **See also:** IntMatrix#repeatElements(int, int)
##### repeatMatrix(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix repeatMatrix(final int rowRepeats, final int columnRepeats) throws IllegalArgumentException`
- **Summary:** Repeats the entire matrix as a tile pattern.
- **Parameters:**
  - `rowRepeats` (`int`) — the number of times to repeat the matrix in the row direction (must be positive)
  - `columnRepeats` (`int`) — the number of times to repeat the matrix in the column direction (must be positive)
- **Returns:** a new ShortMatrix with dimensions (rowCount * rowRepeats) × (columnCount * columnRepeats) containing the tiled pattern
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code rowRepeats} or {@code columnRepeats} is less than or equal to 0
- **See also:** IntMatrix#repeatMatrix(int, int), #repeatElements(int, int)
##### flatten(...) -> ShortList
- **Signature:** `@Override public ShortList flatten()`
- **Summary:** Flattens the matrix into a one-dimensional list in row-major order.
- **Parameters:**
  - (none)
- **Returns:** a new ShortList containing all elements in row-major order
##### mutateAsFlat(...) -> void
- **Signature:** `@Override public <E extends Exception> void mutateAsFlat(final Throwables.Consumer<? super short[], E> action) throws E`
- **Summary:** Flattens all elements of this matrix into a single one-dimensional array, applies the given operation to that flattened array, and then copies the modified elements back into the matrix.
- **Parameters:**
  - `action` (`Throwables.Consumer<? super short[], E>`) — the operation to apply to the flattened array
- **Throws:**
  - `E` — if the operation throws an exception
- **See also:** Arrays#mutateAsFlat(short\[\]\[\], Throwables.Consumer)
##### stackVertically(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix stackVertically(final ShortMatrix other) throws IllegalArgumentException`
- **Summary:** Vertically stacks this matrix with another matrix.
- **Contract:**
  - The two matrices must have the same number of columns.
- **Parameters:**
  - `other` (`ShortMatrix`) — the matrix to stack below this matrix; must not be {@code null}
- **Returns:** a new matrix with rows from both matrices stacked vertically
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , if the matrices don't have the same number of columns, or if the merged row count would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackVertically(IntMatrix)
##### stackHorizontally(...) -> ShortMatrix
- **Signature:** `@Override public ShortMatrix stackHorizontally(final ShortMatrix other) throws IllegalArgumentException`
- **Summary:** Horizontally stacks this matrix with another matrix.
- **Contract:**
  - The two matrices must have the same number of rows.
- **Parameters:**
  - `other` (`ShortMatrix`) — the matrix to stack to the right of this matrix; must not be {@code null}
- **Returns:** a new matrix with columns from both matrices stacked horizontally
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , if the matrices don't have the same number of rows, or if the merged column count would overflow {@code Integer.MAX_VALUE}
- **See also:** IntMatrix#stackHorizontally(IntMatrix)
##### add(...) -> ShortMatrix
- **Signature:** `public ShortMatrix add(final ShortMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise addition of this matrix with another matrix.
- **Contract:**
  - The two matrices must have the same dimensions (same number of rows and columns).
  - If non-wrapping arithmetic is required, widen via {@link #toIntMatrix()} (or {@link #toLongMatrix()} ) before adding.
- **Parameters:**
  - `other` (`ShortMatrix`) — the matrix to add to this matrix (must have same dimensions and not be {@code null} )
- **Returns:** a new matrix containing the element-wise sum
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , or if the matrices don't have the same shape (same rows and columns)
##### subtract(...) -> ShortMatrix
- **Signature:** `public ShortMatrix subtract(final ShortMatrix other) throws IllegalArgumentException`
- **Summary:** Performs element-wise subtraction of another matrix from this matrix.
- **Contract:**
  - The two matrices must have the same dimensions (same number of rows and columns).
  - If non-wrapping arithmetic is required, widen via {@link #toIntMatrix()} (or {@link #toLongMatrix()} ) before subtracting.
- **Parameters:**
  - `other` (`ShortMatrix`) — the matrix to subtract from this matrix (must have same dimensions and not be {@code null} )
- **Returns:** a new matrix containing the element-wise difference (this - other)
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , or if the matrices don't have the same shape (same rows and columns)
##### matmul(...) -> ShortMatrix
- **Signature:** `public ShortMatrix matmul(final ShortMatrix other) throws IllegalArgumentException`
- **Summary:** Performs standard matrix multiplication with another matrix.
- **Contract:**
  - The number of columns in this matrix must equal the number of rows in the specified matrix.
  - If a non-wrapping product is required, widen via {@link #toIntMatrix()} (or {@link #toLongMatrix()} ) and multiply there.
- **Parameters:**
  - `other` (`ShortMatrix`) — the matrix to multiply with this matrix ( {@code this.columnCount} must equal {@code other.rowCount} ); must not be {@code null}
- **Returns:** a new matrix of dimension {@code (this.rowCount × other.columnCount)} containing the matrix product
- **Throws:**
  - `java.lang.IllegalArgumentException` — if {@code other} is {@code null} , or if {@code this.columnCount != other.rowCount} (incompatible dimensions for multiplication)
##### boxed(...) -> Matrix<Short>
- **Signature:** `public Matrix<Short> boxed()`
- **Summary:** Converts this primitive short matrix to a boxed {@code Matrix<Short>} .
- **Contract:**
  - Use this method only when you need to work with generic Matrix API or when null values are required.
- **Parameters:**
  - (none)
- **Returns:** a new {@code Matrix<Short>} containing boxed values
- **See also:** #unbox(Matrix)
##### toIntMatrix(...) -> IntMatrix
- **Signature:** `public IntMatrix toIntMatrix()`
- **Summary:** Converts this short matrix to an int matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code IntMatrix} with values converted from short to int
##### toLongMatrix(...) -> LongMatrix
- **Signature:** `public LongMatrix toLongMatrix()`
- **Summary:** Converts this short matrix to a long matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code LongMatrix} with values converted from short to long
##### toFloatMatrix(...) -> FloatMatrix
- **Signature:** `public FloatMatrix toFloatMatrix()`
- **Summary:** Converts this short matrix to a float matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code FloatMatrix} with values converted from short to float
##### toDoubleMatrix(...) -> DoubleMatrix
- **Signature:** `public DoubleMatrix toDoubleMatrix()`
- **Summary:** Converts this short matrix to a double matrix.
- **Parameters:**
  - (none)
- **Returns:** a new {@code DoubleMatrix} with values converted from short to double
##### zipWith(...) -> ShortMatrix
- **Signature:** `public <E extends Exception> ShortMatrix zipWith(final ShortMatrix other, final Throwables.ShortBinaryOperator<E> zipFunction) throws IllegalArgumentException, E`
- **Summary:** Applies a binary operation element-wise to this matrix and another matrix.
- **Contract:**
  - The two matrices must have the same dimensions.
- **Parameters:**
  - `other` (`ShortMatrix`) — the second matrix to zip with this matrix
  - `zipFunction` (`Throwables.ShortBinaryOperator<E>`) — the binary operation to apply to corresponding elements
- **Returns:** a new matrix with the results of the zip operation
- **Throws:**
  - `java.lang.IllegalArgumentException` — if the matrices don't have the same shape, or if {@code zipFunction} is {@code null}
  - `E` — if the zip function throws an exception
- **Signature:** `public <E extends Exception> ShortMatrix zipWith(final ShortMatrix other, final ShortMatrix third, final Throwables.ShortTernaryOperator<E> zipFunction) throws E`
- **Summary:** Applies a ternary operation element-wise to this matrix and two other matrices.
- **Contract:**
  - All three matrices must have the same dimensions.
- **Parameters:**
  - `other` (`ShortMatrix`) — the second matrix to zip with
  - `third` (`ShortMatrix`) — the third matrix to zip with
  - `zipFunction` (`Throwables.ShortTernaryOperator<E>`) — the ternary operation to apply to corresponding elements from all three matrices
- **Returns:** a new matrix with the results of the zip operation
- **Throws:**
  - `E` — if the zip function throws an exception
##### mainDiagonalStream(...) -> ShortStream
- **Signature:** `@Override public ShortStream mainDiagonalStream()`
- **Summary:** Returns a stream of elements on the main diagonal from upper-left to lower-right.
- **Contract:**
  - <p> The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a ShortStream of diagonal elements from upper-left to lower-right
##### antiDiagonalStream(...) -> ShortStream
- **Signature:** `@Override public ShortStream antiDiagonalStream()`
- **Summary:** Returns a stream of elements on the anti-diagonal from upper-right to lower-left.
- **Contract:**
  - <p> The matrix must be square (same number of rows and columns).
- **Parameters:**
  - (none)
- **Returns:** a ShortStream of anti-diagonal elements from upper-right to lower-left
##### horizontalStream(...) -> ShortStream
- **Signature:** `@Override public ShortStream horizontalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed horizontally (left to right, top to bottom).
- **Parameters:**
  - (none)
- **Returns:** a ShortStream of all matrix elements in row-major order
- **Signature:** `@Override public ShortStream horizontalStream(final int rowIndex)`
- **Summary:** Returns a stream of elements from a specific row.
- **Parameters:**
  - `rowIndex` (`int`) — the index of the row to stream (0-based)
- **Returns:** a ShortStream of elements from the specified row
- **Signature:** `@Override public ShortStream horizontalStream(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of rows in row-major order.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a ShortStream of elements from the specified row range in row-major order
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the row indices are out of bounds or fromRowIndex &gt; toRowIndex
##### verticalStream(...) -> ShortStream
- **Signature:** `@Override @Beta public ShortStream verticalStream()`
- **Summary:** Returns a stream of all elements in this matrix, traversed vertically (top to bottom, left to right).
- **Parameters:**
  - (none)
- **Returns:** a ShortStream of all matrix elements in column-major order
- **Signature:** `@Override public ShortStream verticalStream(final int columnIndex)`
- **Summary:** Returns a stream of elements from a specific column.
- **Parameters:**
  - `columnIndex` (`int`) — the index of the column to stream (0-based)
- **Returns:** a ShortStream of elements from the specified column
- **Signature:** `@Override @Beta public ShortStream verticalStream(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of elements from a range of columns in column-major order.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a ShortStream of elements from the specified column range in column-major order
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the column indices are out of bounds or fromColumnIndex &gt; toColumnIndex
##### rowStreams(...) -> Stream<ShortStream>
- **Signature:** `@Override public Stream<ShortStream> rowStreams()`
- **Summary:** Returns a stream of row streams, where each element is a stream representing a complete row.
- **Parameters:**
  - (none)
- **Returns:** a Stream of ShortStream objects, one for each row
- **Signature:** `@Override public Stream<ShortStream> rowStreams(final int fromRowIndex, final int toRowIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of row streams from a range of rows.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
- **Returns:** a Stream of ShortStream objects for rows in the specified range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the row indices are out of bounds or fromRowIndex &gt; toRowIndex
##### columnStreams(...) -> Stream<ShortStream>
- **Signature:** `@Override @Beta public Stream<ShortStream> columnStreams()`
- **Summary:** Returns a stream of column streams, where each element is a stream representing a complete column.
- **Parameters:**
  - (none)
- **Returns:** a Stream of ShortStream objects, one for each column
- **Signature:** `@Override @Beta public Stream<ShortStream> columnStreams(final int fromColumnIndex, final int toColumnIndex) throws IndexOutOfBoundsException`
- **Summary:** Returns a stream of column streams from a range of columns.
- **Parameters:**
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
- **Returns:** a Stream of ShortStream objects for columns in the specified range
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if the column indices are out of bounds or fromColumnIndex &gt; toColumnIndex
##### forEach(...) -> void
- **Signature:** `public <E extends Exception> void forEach(final Throwables.ShortConsumer<E> action) throws E`
- **Summary:** Performs the specified action for each element in this matrix.
- **Parameters:**
  - `action` (`Throwables.ShortConsumer<E>`) — the consumer to apply to each element
- **Throws:**
  - `E` — if the action throws an exception
- **Signature:** `public <E extends Exception> void forEach(final int fromRowIndex, final int toRowIndex, final int fromColumnIndex, final int toColumnIndex, final Throwables.ShortConsumer<E> action) throws IndexOutOfBoundsException, E`
- **Summary:** Performs the specified action for each element in a rectangular sub-region of this matrix.
- **Parameters:**
  - `fromRowIndex` (`int`) — the starting row index (inclusive, 0-based)
  - `toRowIndex` (`int`) — the ending row index (exclusive)
  - `fromColumnIndex` (`int`) — the starting column index (inclusive, 0-based)
  - `toColumnIndex` (`int`) — the ending column index (exclusive)
  - `action` (`Throwables.ShortConsumer<E>`) — the consumer to apply to each element in the region
- **Throws:**
  - `java.lang.IndexOutOfBoundsException` — if any index is out of bounds or fromIndex &gt; toIndex
  - `E` — if the action throws an exception
##### println(...) -> String
- **Signature:** `@Override public String println()`
- **Summary:** Prints this matrix to standard output and returns the formatted string.
- **Contract:**
  - If the matrix is empty, {@code \[\]} is printed.
- **Parameters:**
  - (none)
- **Returns:** the formatted string representation of the matrix
##### hashCode(...) -> int
- **Signature:** `@Override public int hashCode()`
- **Summary:** Returns a hash code value for this matrix.
- **Parameters:**
  - (none)
- **Returns:** a hash code value for this matrix
##### equals(...) -> boolean
- **Signature:** `@Override public boolean equals(final Object obj)`
- **Summary:** Compares this matrix to the specified object for equality.
- **Contract:**
  - Returns {@code true} if the given object is also a ShortMatrix with the same dimensions and all corresponding elements are equal.
- **Parameters:**
  - `obj` (`Object`) — the object to compare with
- **Returns:** {@code true} if the objects are equal, {@code false} otherwise
##### toString(...) -> String
- **Signature:** `@Override public String toString()`
- **Summary:** Returns a string representation of this matrix.
- **Parameters:**
  - (none)
- **Returns:** a string representation of this matrix

