# IntMatrix.java — Line-by-Line Review Findings (2026-07-04)

File: `src/main/java/com/landawn/abacus/matrix/IntMatrix.java` (~4038 lines)
Scope: full read, every line. Cross-class spot-checks against `AbstractMatrix`, `LongMatrix`, `FloatMatrix`, `DoubleMatrix`, `Short/Char/Byte Matrix` where a finding depended on inherited or sibling behavior.

## Summary

**No defensible findings. The file is clean.**

- Bugs: **None**
- Javadoc / Comments / Logs: **None**
- Naming / Consistency / Design: **None**

This is the 13th deep pass on this family and the result is consistent with the prior 12: no code bugs, no doc discrepancies, no naming/consistency issues in this file.

---

## Bugs

None.

Methods examined in detail for off-by-one / boundary / wrong-variable / overflow / iteration-order issues, all correct:

- **Factories** (`of`, `copyOf`, `from(char/byte/short[][])`, `random`, `randomRow`, `repeat`, `range*`, `mainDiagonal`/`antiDiagonal`/`diagonals`, `unbox`): validation order and rectangular checks correct. `diagonals` overlap precedence (main wins at center) matches its Javadoc; anti-diagonal written first then main overwrites center — correct.
- **Accessors** (`get`/`set` (int,int) & (Point), `valueAbove/Below/Left/Right`, `rowView/rowCopy/columnCopy`): edge handling and the documented AIOOBE (get/set) vs IOOBE (checked methods) split match the established exception policy.
- **Diagonal ops** (`mainDiagonalCopy/antiDiagonalCopy/set*/update*`): `a[i][columnCount - i - 1]` indexing for anti-diagonal is correct; square checks precede.
- **`updateAll(IntUnaryOperator)`**: sequential branch iterates row-major (matches the row-major fix recorded in memory); parallel branch via `Matrices.forEachIndices`. `updateAll(IntBiFunction)` documents the unboxing NPE.
- **`fill(int,int,int[][])`**: bounds `[0, rowCount]`/`[0, columnCount]`, `null`-row skip, and `N.min(...)` clamping of the copied region are all correct; `fill(int[][])` correctly delegates to `(0,0,source)`.
- **`copy`/`copy(range)`/`copy(rowRange,colRange)`**: `checkFromToIndex` used correctly; deep clones/`copyOfRange` produce independent arrays.
- **`resize`/`resize(default)`**: overflow pre-check, shrink-vs-grow branch, and the mixed grow-rows/shrink-cols (and vice versa) cases fill `defaultValue` only where cells are genuinely new. Verified by hand on 2×3→3×2 and 2×3→3×5.
- **`extend`/`extend(default)`**: per-edge overflow checks, interior copy at `(padTop,padLeft)`, and padding fills for top/bottom rows vs left/right gutters of content rows are all correct.
- **`flip*`, `rotate90/180/270`, `transpose`**: the `rowCount <= columnCount` cache-friendly branch pairs produce identical results; index maps (`a[rowCount-j-1][i]`, `a[j][columnCount-i-1]`, `a[j][i]`) verified against the documented example outputs.
- **`reshape`**: single-row fast path and multi-row `cnt`-based source addressing (`a[cnt/columnCount][cnt%columnCount]`) correct; `columnCount == 0` / empty source short-circuits avoid any division by zero (rowLen becomes 0).
- **`repeatElements`/`repeatMatrix`**: overflow guards + block/tile construction verified against examples.
- **`flatten`/`mutateViaFlatArray`**: overflow guard on flatten; mutateViaFlatArray delegates to `Arrays.mutateViaFlatArray`.
- **`stackVertically/Horizontally`**: correct mismatch messages (`MSG_VSTACK_COLUMN_MISMATCH` vs `MSG_HSTACK_ROW_MISMATCH`) and `long` overflow guards.
- **`add`/`subtract`/`matrixMultiply`/`zipWith(2)/zipWith(3)`**: shape checks, `checkRepresentableShape(rowCount, other.columnCount)` for the product, and the accumulate `result[i][j] += a[i][k]*other[k][j]` are correct.
- **Streams** (`mainDiagonalStream`, `antiDiagonalStream`, `rowMajorStream(*)`, `columnMajorStream(*)`, `rowStreams(*)`, `columnStreams(*)`): `hasNext`/`nextInt`/`advance`/`count`/`toArray` were checked arithmetically. Notably:
  - `a[cursor][cursor++]` in `mainDiagonalStream.nextInt()` evaluates to `a[old][old]` then increments — correct.
  - `advance`/`count` linear-index math in `rowMajorStream` (`(n+j)/columnCount`) and `columnMajorStream` (`(n+i)/rowCount`) are correct and mutually symmetric.
  - `columnStreams` inner stream captures `columnIndex = cursor++` before the outer cursor advances — correct per-column binding.
- **`toMultilineString`/`hashCode`/`equals`/`toString`**: `Objectory` StringBuilder is recycled in a `finally`; `equals` compares `rowCount`/`columnCount`/`deepEquals`.

No resource leaks, no silent exception swallowing, no missing-`volatile`/race (class is not designed for concurrent mutation; parallel maps write disjoint cells), no integer-overflow gaps (all size computations are guarded with `(long)` casts before allocation).

---

## Javadoc / Comments / Logs

None.

Checks performed with no discrepancies found:

- Every worked numeric example (`add`=[[6,8],[10,12]], `matrixMultiply` 19/50, `zipWith` binary 12 / ternary 62, `rotate90` [7,4,1], `rotate270` [3,6,9], `transpose`, `antiDiagonalCopy` [3,5,7], `repeatElements`/`repeatMatrix`, overflow-wrap examples) was recomputed and matches.
- `@throws` clauses match actual thrown types, including the deliberate AIOOBE (get/set/Point) vs IndexOutOfBoundsException (checked methods) split and IAE-for-null-arg ordering.
- `@see` targets all resolve to existing methods (`#random(int,int)`, `#updateAll(...)`, `#zipWith(...)`, `#toLongMatrix()`, etc.). Cross-class `@see CharMatrix#toIntMatrix()` / `ByteMatrix#toIntMatrix()` / `ShortMatrix#toIntMatrix()` / `#from(int[][])` back-references are plausible and consistent with the family; not verifiable from this file alone but no reason to doubt.
- Class-level "Aggregations" note is accurate (`IntStream` provides `sum/min/max/average`; `rowMajorStream().sum()`, `rowStreams()/columnStreams()` all exist and return the right types).
- No stale "horizontal/vertical" residue in stream docs (they correctly say row-major/column-major); `flip*`/`stack*` legitimately use horizontal/vertical as method-name semantics.
- The `<p>Result dimensions:` immediately followed by `<ul>` (lines ~1996 and ~2045) is an unclosed `<p>` before a block element, but this is doclint-tolerated (HTML auto-closes `<p>` before `<ul>`), the build passes with it, and it is identical across the family — intentionally not reported as it would be a false positive.

---

## Naming / Consistency / Design

None.

Two candidates were investigated and **dismissed with cross-class evidence**:

1. **`rowStreams(from,to)` lacks the `if (isEmpty()) return Stream.empty();` guard** that `rowMajorStream(from,to)`, `columnMajorStream(from,to)`, and `columnStreams(from,to)` have.
   - Verified family-wide: `DoubleMatrix`, `LongMatrix`, `FloatMatrix`, `ShortMatrix`, `CharMatrix`, `ByteMatrix` all omit the guard in `rowStreams(from,to)` identically (each goes straight from `checkFromToIndex` to `Stream.of(...)`).
   - Behaviorally benign: `rowStreams` wraps `a[cursor]` directly and the iterator produces an empty stream for a 0×0 matrix without the guard.
   - Conclusion: intentional, consistent pattern — **not a finding**.

2. **No `mapToFloat`** despite `toFloatMatrix()` existing (and `mapToLong`/`mapToDouble` present).
   - Verified the family convention: `mapTo` targets are exactly `{int, long, double}` minus self. `IntMatrix` provides `mapToLong` + `mapToDouble` (int handled by `map`); `LongMatrix` provides `mapToInt` + `mapToDouble`; `FloatMatrix` provides `mapToInt` + `mapToLong` + `mapToDouble`. `float` is never a `mapTo` target anywhere.
   - Conclusion: `IntMatrix` has exactly the expected `mapTo` set — **not a finding**.

Method families are complete and symmetric: flip (4 variants), rotate (3), diagonal copy/set/update (main + anti), stack (V/H), value{Above,Below,Left,Right}, row/column view/copy, and all stream ranges are present.

---

## Cross-class items confirmed (not findings, recorded for traceability)

- `AbstractMatrix(A[], Class)` constructor validates `null` rows (`"Row 0 cannot be null"` / `"Row {} cannot be null"`) and rectangularity, so the `of`/`copyOf` Javadoc claim that a `null` row throws `IllegalArgumentException` is accurate (including the all-null-rows case, which trips the row-0 check).
- `checkRepresentableShape` rejects only `rowCount == 0 && columnCount != 0`; every call site in this file (`rotate*`, `transpose`, `matrixMultiply`, `resize`, `extend`, `reshape`, `random`, `repeat`) uses it correctly.
