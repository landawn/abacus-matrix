# FloatMatrix.java — Line-by-Line Review (2026-07-04)

File: `src/main/java/com/landawn/abacus/matrix/FloatMatrix.java` (3965 lines)
Reviewer pass: 13th deep pass. Entire file read line-by-line.

Overall: **essentially clean.** No bugs found. All ~60 Javadoc worked examples were
hand-verified numerically (row-major/column-major order, rotation/transpose formulas,
anti-diagonal indexing, reshape, extend/resize fill logic, IEEE-754 special-value claims,
overflow guards) and are correct. The recently-added `mapToInt`/`mapToLong`/`mapToDouble`
methods were scrutinized specifically and are functionally correct. Only two Low-severity
consistency nits are reported below.

---

## Bugs

None.

Notes on things specifically checked and found correct (not findings):
- `mainDiagonalStream().nextFloat()` uses the `a[cursor][cursor++]` idiom (line 3128); the
  pre-increment ordering is correct (both indices read the same `cursor` before it advances).
- `resize`/`extend` use `Float.floatToRawIntBits(defaultValue) != 0` (lines 1819, 1964) as the
  "need to fill?" flag. Correct for all IEEE-754 values: only `+0.0f` has all-zero raw bits, so
  `-0.0f`, `NaN`, `±Infinity` all correctly trigger an explicit fill while `+0.0f` correctly
  relies on the zero-initialized allocation.
- `matrixMultiply` accumulates with `+=` into a zero-initialized result via
  `Matrices.forEachCartesianIndices`; each `(i,j)` cell is owned by one iteration, no race.
- `checkRepresentableShape` is applied everywhere a shape can invert to `0×N`
  (`transpose`, `rotate90/270`, `matrixMultiply`, `resize`, `extend`), and the zero-column
  early-returns guard the empty cases.
- `copyOf` (line 178) copies `null` rows through as `null`, then the constructor's
  rectangular-shape validation throws `IllegalArgumentException` — matches the documented
  contract (no NPE). Previously examined and confirmed in pass 10.
- `equals`/`hashCode` use `N.deepEquals`/`N.deepHashCode`, i.e. `floatToIntBits` semantics
  (NaN==NaN, +0.0f≠-0.0f), consistent with the class-level and method Javadoc.

---

## Javadoc / Comments / Logs

### Low — `mapToInt` / `mapToLong` / `mapToDouble` `@param mapper` omits the "must not be {@code null}" clause

- Lines: **1363**, **1395**, **1427**
- Verbatim:
  - 1363: `* @param mapper the function to convert float values to int`
  - 1395: `* @param mapper the function to convert float values to long`
  - 1427: `* @param mapper the function to convert float values to double`
- Why it is a discrepancy: every other mapping method in this class states the non-null
  requirement on the `mapper`/`action` `@param` line — `map` (line 1331: "…must not be
  {@code null}"), `mapToObj` (line 1463), `forEach` (lines 3769/3808) — and all three of these
  methods DO declare `@throws IllegalArgumentException if {@code mapper} is {@code null}` and
  call `N.checkArgNotNull(mapper, "mapper")`. The three recently-added `mapTo*` primitive
  variants are the only mapping methods whose `@param mapper` text drops the "must not be null"
  note, making them the lone outliers within the file.
- Suggested fix: append "; must not be {@code null}" to each of the three `@param mapper`
  descriptions to match `map`/`mapToObj`.

---

## Naming / Consistency / Design

### Low — Intra-file inconsistency: `add`/`subtract` use static `Matrices.isSameShape`, but `zipWith` uses the instance `isSameShape`

- Lines: **2679**, **2723** vs **3016**, **3072**
- Verbatim:
  - 2679 (`add`): `N.checkArgument(Matrices.isSameShape(this, other), …`
  - 2723 (`subtract`): `N.checkArgument(Matrices.isSameShape(this, other), …`
  - 3016 (`zipWith/2`): `N.checkArgument(isSameShape(other), …`
  - 3072 (`zipWith/3`): `N.checkArgument(isSameShape(other) && isSameShape(third), …`
- Why it is worth noting: within a single class the two element-wise families check the same
  invariant ("same shape") through two different spellings — a static helper in one place and an
  inherited instance method in the other. Both are behaviorally identical, so this is a
  readability/consistency nit only, not a bug.
- Caveat for the main thread: this pattern is almost certainly copy-pasted identically across
  all sibling primitive-matrix classes. Verify cross-class before "fixing," since normalizing
  only `FloatMatrix` would introduce family-wide divergence. If normalized, do it uniformly
  across the whole matrix family in one sweep.

### Very Low (observation, not recommending a change) — `columnStreams` has an `if (isEmpty()) return Stream.empty();` guard that `rowStreams` lacks

- Lines: `columnStreams(int,int)` guard at **3658**; `rowStreams(int,int)` (line **3567**) has no
  such guard.
- Both are correct: for any empty/zero-dimension matrix the underlying iterators naturally yield
  an empty stream, so the guard in `columnStreams` is redundant and its absence in `rowStreams`
  is harmless. Flagged only for symmetry awareness; no action needed. (Same static-vs-natural
  short-circuit asymmetry as `rowMajorStream`/`columnMajorStream`, which both keep the guard.)

---

## Summary

| Category | High | Medium | Low | Total |
|---|---|---|---|---|
| Bug | 0 | 0 | 0 | 0 |
| Javadoc/Comment/Log | 0 | 0 | 1 | 1 |
| Naming/Consistency/Design | 0 | 0 | 1 (+1 very-low observation) | 1–2 |

Most important finding: **none is material.** The single most actionable item is the Low
Javadoc nit — the three new `mapToInt`/`mapToLong`/`mapToDouble` methods (lines 1363/1395/1427)
omit "must not be {@code null}" from their `@param mapper` text, unlike every sibling mapping
method in the same file. No code changes warranted.
