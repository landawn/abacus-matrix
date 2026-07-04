# LongMatrix.java — Line-by-Line Review Findings (2026-07-04)

File: `C:\Users\haiyangl\Landawn\abacus-matrix\src\main\java\com\landawn\abacus\matrix\LongMatrix.java`
Lines reviewed: 1–3969 (entire file, every line).

**Overall: essentially clean.** Consistent with the 12 prior deep-review passes. Zero bugs found.
Only two very-low-severity cosmetic/consistency notes, plus one design observation that is
almost certainly intentional (documented for the main thread to confirm against siblings).

---

## 1. BUGS

None.

Notable correctness points that were specifically checked and confirmed **correct**:

- **N×0 matrices and the row/column-major stream iterators.** `rowMajorStream(from,to)` and
  `columnMajorStream(from,to)` (lines 3288, 3437) each guard with `if (isEmpty()) return ...empty()`.
  `isEmpty()` is `elementCount == 0` and `elementCount = (long) columnCount * rowCount`
  (AbstractMatrix lines 201, 535–536), so a `3×0` matrix is treated as empty. This prevents the
  `a[i][j++]` access on a zero-length row that would otherwise throw AIOOBE. No bug.
- **`mainDiagonalStream().nextLong()` (line 3124):** `return a[cursor][cursor++];` — Java evaluates
  `a[cursor]` before `cursor++`, so it reads `a[old][old]` then increments. Correct.
- **`diagonals()` (lines 518–545):** null/empty combination handling, length-mismatch check, and
  main-takes-precedence overlap ordering (anti filled first, then main) are all correct.
- **Integer-overflow guards** in `resize` (1848), `extend` (1981, 1986), `repeatElements`/`repeatMatrix`
  (2425–2430, 2479–2484), `stackVertically`/`stackHorizontally` (2607–2608, 2656–2658), `flatten`
  (2526), and `reshape` (2368) all cast to `long` before multiplying/adding. `elementCount` itself is
  a `long`. No int intermediates overflow.
- **`resize`/`extend` mixed grow/shrink cases** (grow rows + shrink columns, and vice versa) verified
  to produce correct fills and truncation.
- **`rotate90`/`rotate180`/`rotate270`/`transpose`** index formulas verified against worked examples;
  both loop-nesting branches produce identical results (cache-locality micro-opt only).
- **`reshape`** row-length / element-count math (lines 2377–2391) verified for single-row and
  multi-row source cases.
- **`toMultilineString()` (3848):** `Objectory` StringBuilder is recycled in a `finally`; no leak.
- Long-arithmetic overflow-wrap examples in `add`/`subtract`/`matrixMultiply`, and precision-loss
  examples in `toFloatMatrix`/`toDoubleMatrix`/`toIntMatrix`, are all numerically accurate.

---

## 2. JAVADOC / COMMENTS / LOGS

### Low — Inconsistent `{@link}` parameter form for the `of` factory
- **Location:** line 39 (class-level Javadoc) vs. lines 139, 158.
- **Quote (line 39):** `while {@link #of(long[]...)} generally wrap the supplied storage`
- **Quote (line 139):** `Unlike {@link #of(long[][])}, which wraps the caller's array`
- **Why:** The class Javadoc references the factory as `#of(long[]...)` (varargs ellipsis form) while
  the `copyOf` Javadoc (line 139) and its `@see` (line 158) reference the same method as
  `#of(long[][])`. Both resolve under modern javadoc, but the two forms are used inconsistently within
  a single file. Purely cosmetic.
- **Suggested fix:** Pick one form (the codebase elsewhere in this file favors `long[][]`) and use it
  consistently, e.g. change line 39 to `{@link #of(long[][])}`.
- **Confidence:** High that it's inconsistent; low that it matters. Likely present across siblings —
  main thread may choose to leave as-is to avoid churn.

---

## 3. NAMING / CONSISTENCY / DESIGN

### Low (likely intentional) — `toFloatMatrix()` has no companion `mapToFloat`, and no `@see`
- **Location:** `toFloatMatrix()` at lines 2915–2936; compare `toIntMatrix()` (2869) `@see #mapToInt`
  (2867) and `toDoubleMatrix()` (2963) `@see #mapToDouble` (2960).
- **Observation:** `LongMatrix` provides `mapToInt` (→IntMatrix) and `mapToDouble` (→DoubleMatrix),
  each cross-referenced from the matching `toXxxMatrix()` conversion. But `toFloatMatrix()` exists with
  **no** corresponding `mapToFloat` method and **no** `@see`. That is an asymmetry in the map/convert
  family within this class.
- **Cross-class check performed:** I grepped all sibling matrix classes. `IntMatrix` exhibits the
  **same** pattern — it has `mapToLong` and `mapToDouble` but **no `mapToFloat`**, despite also having
  a `toFloatMatrix()`. `FloatMatrix` has the full `mapToInt/mapToLong/mapToDouble` set. Memory notes
  record that a prior pass deliberately added the map* family to `FloatMatrix` but did **not** add
  `mapToFloat` to the integer matrices.
- **Conclusion:** The absence of `mapToFloat` on `LongMatrix` is **consistent with `IntMatrix`** and
  therefore almost certainly an intentional design choice (integer matrices skip a lazy-map path to
  the lossy `float` type). **Not recommended as an actionable finding** — reported only so the main
  thread can confirm the pattern is deliberate. If the maintainers ever decide to make the map*
  families exhaustive, adding `LongMatrix.mapToFloat` + `IntMatrix.mapToFloat` would close the gap; the
  `toFloatMatrix()` Javadoc could then gain an `@see #mapToFloat`.

### Verified-consistent (no action)
- `@see IntMatrix#toLongMatrix()` on `from(int[]...)` (line 199) — confirmed `IntMatrix.toLongMatrix()`
  exists (IntMatrix.java line 2979). Valid.
- Diagonal factory `@see` cross-references (`mainDiagonal`/`antiDiagonal`/`diagonals`) form a complete
  symmetric triangle.
- `get/set` keep `ArrayIndexOutOfBoundsException` (unchecked direct access); `valueAbove/Below/Left/Right`
  and range methods use `IndexOutOfBoundsException` via `checkRow/ColumnIndex` — matches the
  established validation-exception policy (null/value→IAE, index→IOOBE, get/set→AIOOBE).

---

## Summary

- Bugs: **0**
- Javadoc/Comment/Log: **1** (Low — trivial `{@link}` form inconsistency, line 39)
- Naming/Consistency/Design: **1** (Low — missing `mapToFloat`, but consistent with `IntMatrix`, so
  almost certainly intentional; not actionable)

No High or Medium findings. The file is production-clean.
