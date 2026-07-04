# DoubleMatrix.java — Deep Review Findings (2026-07-04)

File: `src/main/java/com/landawn/abacus/matrix/DoubleMatrix.java` (4077 lines)
Reviewer pass: 13th deep review. Read in full, line by line. Cross-checked selected
patterns against `AbstractMatrix`, `IntMatrix`, `LongMatrix`, `FloatMatrix`.

## Executive summary

The file is essentially clean. No correctness bugs were found. Boundary handling
(empty / 0×N / N×0 matrices, negative dimensions, overflow guards), floating-point
semantics (NaN / ±0.0 / ±Infinity), row-major vs column-major iteration, stream
iterator arithmetic (`advance`/`count`/`toArray`), and the copy/wrap ownership model
are all correct and consistent with sibling classes.

Only a few **cosmetic** consistency/doc nits are reported below, all Low severity.
Several plausible-looking leads were investigated and **rejected as non-issues** (see
the final section) so the main thread does not re-chase them.

---

## 1. BUGS

**None.**

Specifically verified as correct:
- `resize`/`extend` fill-skip optimization `Double.doubleToRawLongBits(defaultValue) != 0`
  correctly fills only when `defaultValue != +0.0` (so `-0.0`, `NaN`, `±Infinity` are all
  honored; `+0.0` relies on the already-zeroed allocation). Lines 1907, 2052.
- `reshape` never divides by zero: the `cnt % columnCount` / `cnt / columnCount` at line
  2447 only runs inside a loop bounded by `rowLen`, which is `0` whenever `elementCount == 0`
  (the only way `columnCount == 0`). The `newColumnCount == 0` case is short-circuited at
  line 2432, and `ceilDiv(elementCount, newColumnCount)` (2436) is guarded the same way.
- `rowMajorStream`/`columnMajorStream` `advance`/`count` use `(long)` casts before
  multiplying dimensions (lines 3443, 3454, 3592, 3604), so no int overflow; division by
  `columnCount`/`rowCount` is unreachable when either is 0 because `isEmpty()` short-circuits
  to an empty stream first (3408, 3557).
- `matrixMultiply` on an N×0 · 0×M product correctly yields an N×M zero matrix (empty inner
  `k` loop) and `checkRepresentableShape(rowCount, other.columnCount)` guards the 0-row case.
- Diagonal streams `nextDouble()` index arithmetic (`a[cursor][cursor++]` at 3240; the split
  read-then-increment at 3301-3302) is correct.
- `fill(destRowIndex, destColumnIndex, source)` boundary math (1652-1666) tolerates
  `destRowIndex == rowCount` and `destColumnIndex == columnCount` (zero-length copies), and
  skips `null` source rows.
- `rotate90` / `rotate270` / `transpose` index formulas verified against the doc examples.
- `equals`/`hashCode` both route through `N.deepEquals`/`N.deepHashCode`, i.e.
  `doubleToLongBits` semantics, matching the class- and method-level Javadoc (NaN == NaN,
  +0.0 != -0.0).

---

## 2. JAVADOC / COMMENTS / LOGS

### Low

**2.1 — `@throws IllegalArgumentException` documented but omitted from the `throws` clause on the diagonal/all updaters (internal inconsistency)**
Lines 1162, 1264, 1298, 1343.

```java
// setMainDiagonal DOES declare it:
public void setMainDiagonal(final double[] mainDiagonal) throws IllegalStateException, IllegalArgumentException {   // 1128

// updateMainDiagonal does NOT, though it throws + documents IAE:
public <E extends Exception> void updateMainDiagonal(final Throwables.DoubleUnaryOperator<E> operator) throws IllegalStateException, E {  // 1162
    checkIsSquare();
    N.checkArgNotNull(operator, "operator");   // throws IllegalArgumentException
```

`updateMainDiagonal` (1162) and `updateAntiDiagonal` (1264) both perform
`N.checkArgNotNull(operator, ...)` and carry `@throws IllegalArgumentException if operator is null`
in their Javadoc, yet their `throws` clauses list only `IllegalStateException, E` — omitting
`IllegalArgumentException`. The sibling setters `setMainDiagonal`/`setAntiDiagonal` (1128, 1230)
*do* list `IllegalArgumentException` in the `throws` clause. Likewise `updateAll(DoubleUnaryOperator)`
(1298) and `updateAll(IntBiFunction)` (1343) document `@throws IllegalArgumentException` (and the
latter `@throws NullPointerException`) while declaring only `throws E`.

Why it matters: `IllegalArgumentException` is unchecked, so this is not a compile/behavior
defect — purely a documentation/declaration style inconsistency *within the same file*. Either
convention is fine; they should just match.

Suggested fix (pick one and apply uniformly): add `IllegalArgumentException` to the `throws`
clauses of `updateMainDiagonal`/`updateAntiDiagonal`/`updateAll` to match the setters, **or**
drop it from the setters. Note this likely mirrors the same choice in all 8 primitive variants —
flag for the main thread to align cross-class.

**2.2 — Redundant wording "double-precision doubles" in class Javadoc**
Line 44.

```
* <p><b>IEEE 754 semantics:</b> elements are double-precision doubles. Be aware that
```

"double-precision doubles" is redundant (a `double` is by definition double-precision). Harmless;
likely a deliberate parallel to `FloatMatrix`'s "single-precision floats". Suggested fix (optional):
"elements are IEEE-754 double-precision values." Left to the main thread's discretion; not worth a
cross-class churn on its own.

---

## 3. NAMING / CONSISTENCY / DESIGN

### Low

**3.1 — Shape-equality check invoked two different ways within the file**
Lines 2768 / 2812 (static form) vs 3128 / 3184 (instance form).

```java
// add / subtract use the static Matrices form:
N.checkArgument(Matrices.isSameShape(this, other), ...);        // 2768, 2812
// zipWith (2-arg and 3-arg) use the inherited instance form:
N.checkArgument(isSameShape(other), ...);                        // 3128, 3184
```

Both are correct and equivalent; the mixed call style is a minor readability inconsistency.
Suggested fix (optional): standardize on `isSameShape(other)` throughout. Cosmetic only.

**3.2 — (Not a defect — recorded to pre-empt re-investigation) `mapToFloat` intentionally absent**
`DoubleMatrix` provides `mapToInt`, `mapToLong`, `mapToObj` but no `mapToFloat`. This is
**consistent** with the family convention: the numeric map targets are exactly `{Int, Long, Double}`
minus self (Int→{Long,Double}, Long→{Int,Double}, Double→{Int,Long}, Float→{Int,Long,Double}).
No change recommended.

---

## Rejected leads (verified NOT issues — do not re-chase)

- **`DoubleMatrix.this.rowCount` qualification in `columnMajorStream().advance()` (3592-3598)** —
  looked like a stray/inconsistent qualifier, but `IntMatrix` (3544-3550), `LongMatrix` (3475-3481),
  and `FloatMatrix` (3480-3486) all use the identical qualified form. Established, intentional pattern.
- **`extend` lacks a row×column *product* overflow check (unlike `resize`)** — not needed: the backing
  store is a jagged `double[newRowCount][newColumnCount]`; each row array (`newColumnCount`) and the row
  count (`newRowCount`) are individually bounded to `Integer.MAX_VALUE` (2039-2047), which is the only
  hard JVM limit. No single contiguous array of size `product` is allocated.
- **`updateAll(IntBiFunction)` returning `null` from mapper** — documented `NullPointerException`
  (auto-unboxing) is correct; delegation to `Matrices.forEachIndices` is the family norm.
- **Cross-class `@see` targets** — `IntMatrix#toDoubleMatrix`, `LongMatrix#toDoubleMatrix`,
  `FloatMatrix#toDoubleMatrix` (lines 207, 268, 327) all exist (verified all 6 siblings define
  `public DoubleMatrix toDoubleMatrix()`).
- **`checkIsSquare()` → `IllegalStateException`** (diagonal ops/streams) matches the documented
  state→ISE policy (`AbstractMatrix` line 2486-2488).
- **Doc worked examples** (diagonals overlap precedence, rotate/transpose results, `from(long)`
  precision-loss value `9.223372036854776E18`, NaN/Infinity arithmetic notes, `toString`/`toMultilineString`
  `"[]"` rendering) — all spot-checked numerically and found accurate.
