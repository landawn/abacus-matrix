# BooleanMatrix.java — Line-by-line Review Findings (2026-07-04)

File: `src/main/java/com/landawn/abacus/matrix/BooleanMatrix.java` (3,894 lines)

## Overall assessment

The file is **essentially clean**. I read every line and cross-checked the parent-class
helpers it relies on (`AbstractMatrix.isEmpty()` = `elementCount == 0`, `checkRepresentableShape`,
`checkMaterializableShape`, `ceilDiv`, `checkIsSquare`, `checkRowColumnIndex`). All index math,
row-major/column-major iteration, overflow guards, defensive-copy vs. live-view contracts,
diagonal/anti-diagonal indexing, reshape/resize/extend padding, stream iterators
(`hasNext`/`next`/`advance`/`count`/`toArray`), and the dozens of `@Usage Examples` I spot-checked
are **correct**. No off-by-one, null-handling, boundary, resource-leak, concurrency, dead-branch,
or copy/paste-type bugs were found.

Only a handful of **Low-severity** cosmetic Javadoc / consistency items are reported below. None are
behavioral defects. This is consistent with the 12 prior deep-review passes finding zero bugs.

---

## BUGS

**None.**

I specifically checked the historically error-prone spots and confirmed each is correct:
- `mainDiagonalCopy`/`antiDiagonalCopy` and their setters/updaters — anti-diagonal uses
  `a[i][columnCount - i - 1]` correctly (lines 1036, 1078, 1113).
- `mainDiagonalStream` `next()` = `a[cursor][cursor++]` (line 2976): `a[cursor]` is evaluated with
  the pre-increment value, then `[cursor++]` reads the same index and increments — yields `a[c][c]`,
  cursor→c+1. Correct.
- `rotate90` (`a[rowCount-j-1][i]`), `rotate270` (`a[j][columnCount-i-1]`), `transpose` (`a[j][i]`)
  all index correctly; each guards `columnCount == 0 → EMPTY` and re-checks `checkRepresentableShape`.
- `reshape` multi-row branch (lines 2255-2263) uses the original `columnCount` for the linear-index
  decode `a[cnt/columnCount][cnt%columnCount]`; `rowLen == 0` when `elementCount == 0` avoids any
  divide-by-zero on `N×0` inputs.
- `resize`/`extend` fill-with-`defaultValue` branches only fill the *new* cells (grown rows fully,
  grown columns from `columnCount`/around the pad band) and use literal `true` correctly because the
  branch is guarded by `if (defaultValue)` (so `defaultValue == true`).
- All stream methods short-circuit `if (isEmpty()) return Stream.empty();`, and `isEmpty()` is
  `elementCount == 0`, so an `N×0` matrix (e.g. `boolean[3][0]`) correctly yields an empty stream
  rather than hitting `a[i][0]` in `next()`.
- `stackVertically`/`stackHorizontally`/`countTrue`/`flatten` overflow guards are all present and
  correct.

---

## JAVADOC / COMMENTS / LOGS

### Low

**J1. `toMultilineString` Javadoc example uses integers, not booleans (copy/paste from a numeric sibling)**
- Lines 3767-3769:
  ```java
  /**
   * Renders this matrix as a multi-line string (one row per line, e.g. {@code "[1, 2]\n[3, 4]"}); a
   * zero-row matrix renders {@code "[]"}. Backs {@link #println()} and {@link #appendTo(Appendable)}.
   ```
- Why wrong: This is `BooleanMatrix`; its rows render as `true`/`false`, never `1`/`2`. The
  `"[1, 2]\n[3, 4]"` sample is verbatim boilerplate carried over from a numeric sibling
  (Int/Long/etc.) and is type-incorrect for this class. This is exactly the kind of sibling-residue
  earlier passes have been scrubbing.
- Suggested fix: change the example to a boolean one, e.g.
  `{@code "[true, false]\n[false, true]"}`.

*(No other Javadoc discrepancies found. I verified a broad sample of the `@Usage Examples` blocks —
`get`/`set`/`valueAbove/Below/Left/Right`, `fill(int,int,boolean[][])` clipping, `resize`/`extend`
grow/truncate results, `rotate*`/`transpose`, `reshape`, `repeatElements`/`repeatMatrix`,
`and`/`or`/`xor`, `zipWith` (binary + ternary), all `*Stream*` methods, `forEach(region)`,
`hashCode`(empty→1), `toString`, `boxed`/`unbox` — every asserted value is arithmetically/logically
correct.)*

---

## NAMING / CONSISTENCY / DESIGN

### Low

**N1. `throws IllegalArgumentException` is declared inconsistently across mutator methods**
- Methods that call `N.checkArgNotNull(...)` but **omit** `IllegalArgumentException` from their
  `throws` clause:
  - `updateAll(Throwables.BooleanUnaryOperator)` — line 1144: `throws E`
  - `updateMainDiagonal` — line 994: `throws IllegalStateException, E`
  - `updateAntiDiagonal` — line 1108: `throws IllegalStateException, E`
- Methods that **do** declare it for the same kind of null-check:
  - `setMainDiagonal` — line 958: `throws IllegalStateException, IllegalArgumentException`
  - `setAntiDiagonal` — line 1072: `throws IllegalStateException, IllegalArgumentException`
  - `updateRow` / `updateColumn` — lines 840-841 / 880-881: `throws ..., IllegalArgumentException, E`
- Why it matters: purely cosmetic (IAE is unchecked, so the clause is optional), but within a single
  file the mixed convention is a consistency wart. All three of the above have a Javadoc
  `@throws IllegalArgumentException` line, so the signature and the doc disagree on whether it is
  "declared."
- Suggested fix: pick one convention (either always list `IllegalArgumentException` for methods that
  null-check, or never) and apply it uniformly here and across the primitive-matrix family.

**N2. Shape-equality check uses two different idioms in this file**
- `and`/`or`/`xor` (lines 2479, 2514, 2549) use the static form
  `Matrices.isSameShape(this, other)`.
- `zipWith` (lines 2852, 2910) uses the inherited instance form `isSameShape(other)`.
- Why it matters: both are equivalent; it is only a stylistic inconsistency within the same class.
- Suggested fix: standardize on the instance `isSameShape(other)` (shorter, already used by
  `zipWith`) for `and`/`or`/`xor` as well.

**N3. Validation *order* differs between diagonal updaters and row/column updaters**
- `updateMainDiagonal`/`updateAntiDiagonal` (lines 994-996, 1108-1110) check `checkIsSquare()`
  (→ ISE) **before** `N.checkArgNotNull(operator)` (→ IAE).
- `updateRow`/`updateColumn` (lines 842-844, 882-884) check `N.checkArgNotNull(operator)` (→ IAE)
  **before** `checkRowIndex`/`checkColumnIndex` (→ IOOBE).
- Why it matters: when a caller passes a `null` operator to a *non-square* matrix,
  `updateMainDiagonal` surfaces `IllegalStateException`, whereas the "argument-first" convention used
  by `updateRow` would surface `IllegalArgumentException`. This is a defensible design choice
  (state-precondition before argument validation) but is applied inconsistently within the class.
  Established memory policy governs *which* exception type maps to which failure, not the ordering, so
  this is only a minor internal-consistency note.
- Suggested fix: decide whether precondition (square/index) or argument-null checks run first and make
  all four families agree.

**N4. Boolean-specific API is asymmetric — `true`-only predicates, no unary `not`**
- Present: `countTrue()` (2581), `allTrue()` (2615), `anyTrue()` (2647); binary logicals
  `and`/`or`/`xor` (2477/2512/2547).
- Absent: any `countFalse()` / `allFalse()` / `anyFalse()`, and any unary `not()` (logical negation
  returning a new matrix — the natural companion to `and`/`or`/`xor`).
- Why it matters: this is a design/completeness observation, not a defect. Each missing operation is
  trivially expressible today (`elementCount() - countTrue()`, `!anyTrue()`, `map(v -> !v)`), so the
  omissions may be intentional. Flagging in case a fuller boolean-algebra surface is desired; a `not()`
  in particular reads as a conspicuous gap next to `and`/`or`/`xor`.
- Suggested fix (optional): consider adding `not()` and, if symmetry is wanted,
  `countFalse()`/`allFalse()`/`anyFalse()`.

---

## Cross-class items to verify in the main thread
- N1/N3 (throws-clause + validation-order conventions) should be reconciled against the numeric
  siblings so the whole family stays aligned — the fix, if adopted, is family-wide, not local.
- J1 (`"[1, 2]\n[3, 4]"` in `toMultilineString`): check whether the same integer-typed example was
  copy-pasted into the *numeric* siblings (where it is correct) and only BooleanMatrix needs the
  boolean-typed example.
