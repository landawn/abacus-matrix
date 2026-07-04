# Code Review Findings — `Matrix.java`

File: `C:\Users\haiyangl\Landawn\abacus-matrix\src\main\java\com\landawn\abacus\matrix\Matrix.java`
Reviewer pass date: 2026-07-04
Scope: entire file read line-by-line (lines 1–3938).

## Executive summary

This is the generic (object-typed) `Matrix<T>`. Consistent with the 12 prior deep passes,
the file is **essentially clean**. I found **no bugs** of any severity — all bounds checks,
row-major/column-major iteration, diagonal precedence, reshape (single-row and multi-row
paths), rotate/transpose cache-branch bodies, overflow guards, null/empty/edge handling,
stream `advance`/`count`/`toArray` arithmetic, resource handling (`Objectory` recycle in
`finally`), and the documented example values (including the hashCode examples 32833 / 62 / 1,
which I recomputed and confirmed) are correct.

The items below are **Low / borderline** documentation-and-hygiene observations only. None are
behavioral defects. Several likely mirror a library-wide convention across the primitive
sibling classes; those are flagged for the main thread to cross-check before any edit, because
"fixing" `Matrix` alone could *introduce* an inconsistency.

---

## 1. Bugs

**None.**

Every bug-review dimension was checked (off-by-one, null/empty/negative-index, boundary,
resource leaks, exception swallowing, concurrency, generics/varargs pitfalls, dead branches,
row-major vs column-major, copy/paste of `rows`↔`cols`). No defensible finding.

Notable spots that were scrutinized and confirmed correct:
- `reshape` (2490–2525): single-row fast path and the `cnt / columnCount` multi-row path are
  both safe; the `columnCount == 0 ⇒ elementCount == 0 ⇒ rowLen == 0` chain prevents any
  division-by-zero, and the early `newRowCount==0 || newColumnCount==0 || isEmpty(a)` guard
  (2504) is correct.
- `diagonals` (382–415): anti-diagonal written first, main-diagonal second so main wins at the
  shared center cell (matches doc); empty/null-diagonal handling and length check are correct.
- `rotate90`/`rotate270`/`transpose` (2280/2371/2430): the two cache-oriented loop branches have
  identical bodies and cover the same index set; the `columnCount == 0` degenerate-shape guard is
  correct.
- `fill(int,int,T[][])` (1722–1737): `destRowIndex == rowCount` / `destColumnIndex == columnCount`
  edges produce zero-length copies (no OOB); null source rows are skipped as documented.
- Stream iterators `advance`/`count`/`toArray` (3016–3594): index arithmetic verified for
  row-major, column-major, and the nested column-of-columns iterator.

---

## 2. Javadoc / Comments / Logs

### 2.1 (Low) Parallel transform methods omit the thread-safety note that `forEach` documents
Lines: `1280`, `1317`, `1359`, `1395`, `1429`, `1465`, `1499`, `1534`, `1568`, `1602`, `2820`,
`2856`, `2903`, `2940` (all `map` / `mapToBoolean/Byte/Char/Short/Int/Long/Float/Double` /
`zipWith` overloads).

Verbatim (representative):
> `* The operation may be performed in parallel for large matrices.`

Contrast with `forEach` (lines 3614–3617 and 3650–3653):
> `* If parallelized, the order of execution is not guaranteed ... If parallelized, {@code action} must be thread-safe.`

Why it is a discrepancy: `map`, every `mapToXxx`, and every `zipWith` variant dispatch through
`Matrices.forEachIndices(..., Matrices.shouldRunInParallel(this))` (e.g. 1351, 1387, 2894, 2984),
so the user-supplied mapper/zipFunction can genuinely run on multiple threads. The output writes
target distinct `result[i][j]` cells (safe), but a mapper/zipFunction that touches shared mutable
state must be thread-safe — exactly the caveat `forEach` spells out and these methods do not. A
reader comparing the two families would reasonably conclude the omission is meaningful.

Suggested fix: append a sentence such as "If parallelized, the mapper must be thread-safe; the
order in which elements are visited is unspecified." to the parallel transform methods.

Cross-class caveat for the main thread: verify whether the primitive siblings (`IntMatrix`, etc.)
also omit this note on their `mapToXxx`/`zipWith`. If they all omit it, this is a library-wide
convention and either all classes should be updated together, or the item is intentional and
should be left as-is. Do **not** edit `Matrix` in isolation.

### 2.2 (Low, borderline) `resize`/`extend` `@throws ArrayStoreException` is stated unconditionally but only fires when cells are actually added
Lines: `1943–1944` (`resize`), `2084–2085` (`extend`).

Verbatim (`resize`):
> `@throws ArrayStoreException if {@code defaultValue} is non-{@code null} and not assignable to`
> `this matrix's runtime element type`

Why it is imprecise: `defaultValue` is only ever stored into *newly added* cells.
- `resize` (1957–1958): when both dimensions shrink-or-stay (`newRowCount <= rowCount &&
  newColumnCount <= columnCount`) it returns `copy(0, newRowCount, 0, newColumnCount)` and never
  touches `defaultValue` — so an incompatible non-null `defaultValue` does **not** throw here.
- `extend` (2096–2097): when all four pads are `0` it returns `copy()` and never fills.

Concrete scenario: a `Matrix<Number>` whose backing array is `Integer[][]` (elementType =
`Integer`). `m.resize(2, 2, 1.5)` (shrink) succeeds silently; `m.resize(4, 4, 1.5)` (grow) throws
`ArrayStoreException`. The Javadoc reads as if the shrink call would also throw.

Severity note: this is genuinely borderline — `@throws` conventionally describes a condition under
which a method *may* throw, and "no cells added ⇒ nothing to store" is arguably self-evident.
Flagging for completeness; easy to leave as-is. If touched, a qualifier like "…when the operation
adds new cells (i.e. the matrix grows in at least one dimension)" would be exact.

---

## 3. Naming / Consistency / Design

### 3.1 (Low) `equals` uses an unchecked generic cast without suppression
Line `3911`:
> `final Matrix<T> another = (Matrix<T>) obj;`

Why: `(Matrix<T>) obj` is an unchecked cast to a parameterized type and produces an
unchecked-cast compiler warning. The method only reads `int` fields and the erased `a` array from
`another`, so the generic parameter is not actually needed. The primitive siblings cast to a
non-generic type (e.g. `(IntMatrix) obj`) and therefore emit no such warning — making `Matrix`
the lone family member with an unsuppressed unchecked cast in `equals`.

Suggested fix (either): add `@SuppressWarnings("unchecked")` to `equals`, or cast to the raw type
and drop the generic — e.g. `final Matrix<?> another = (Matrix<?>) obj;` — which reads
`another.a`/`another.rowCount`/`another.columnCount` without an unchecked warning.

Note: the stream `toArray` overrides (3242, 3383) also perform unchecked `(A)` casts without
suppression, so the class already tolerates such warnings internally; this is hygiene, not
behavior.

### 3.2 (Informational, not a defect) `columnCopy` vs `rowCopy` runtime component type
Lines `692–696` (`rowCopy` → `a[rowIndex].clone()`) and `720–731` (`columnCopy` → `N.newArray(
elementType, rowCount)`).

`rowCopy` preserves each row's *actual* runtime component type; `columnCopy` always allocates the
resolved `elementType`. For a homogeneous matrix these are identical, so there is no observable
difference in normal use. Only matters for a matrix whose rows carry a more specific runtime
component type than `elementType` (permitted by the constructor). This is consistent with the
documented behavior of `rowView`/`rowCopy` and is almost certainly by design — recorded only so
the main thread need not re-investigate it.

### 3.3 (Informational) `@see IntMatrix#stack...` on a generic class
Lines `2735`, `2791`: `stackVertically`/`stackHorizontally` cross-reference
`IntMatrix#stackVertically(IntMatrix)` / `IntMatrix#stackHorizontally(IntMatrix)`. Valid and
harmless, just slightly arbitrary (a single primitive sibling singled out). Not worth changing on
its own.

---

## Verdict

- **Bugs:** 0
- **Javadoc/Comments/Logs:** 2 (both Low; 2.2 borderline)
- **Naming/Consistency/Design:** 1 Low (3.1) + 2 informational (3.2, 3.3)

The single most actionable item is **2.1** (thread-safety note absent from the parallel
`map`/`mapToXxx`/`zipWith` methods that `forEach` includes) — and even that should be resolved at
the family level after confirming the primitive siblings' wording, not in `Matrix` alone.
