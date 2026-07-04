# Deep Review 2026-07-04 — Final Report

**Scope:** every `.java` under `src/main/java/com/landawn/abacus/matrix/` (12 files, ~44,000 lines):
DoubleMatrix, IntMatrix, LongMatrix, FloatMatrix, ByteMatrix, ShortMatrix, CharMatrix, BooleanMatrix,
Matrix, AbstractMatrix, Matrices, ParallelMode.

**Method:** 11 parallel review agents (findings-only) → main-thread independent verification of every finding
against source (grep sweeps to establish true cross-class scope) → triage → apply verified doc/hygiene fixes →
compile + doclint + full test suite gate.

**BC:** not a constraint.

---

## 1. Bugs fixed

**None — zero correctness bugs were found**, consistent with the prior 12 passes. Areas specifically re-traced
and confirmed correct across the family: `N×0`/empty stream guards, `a[cursor][cursor++]` diagonal idiom,
row-major vs column-major `advance`/`count`/`toArray` arithmetic, `reshape` divide-by-zero avoidance,
`resize`/`extend` grow/shrink fill logic, overflow guards (`(long)` casts before multiply/allocate),
`matrixMultiply` `+=` accumulation race-freedom (parallelizes over output cells, never over `k`),
IEEE-754 raw-bits fill flags, `saturatedMultiply` `Long.MIN_VALUE*-1` case, and the exception-type policy
(get/set → AIOOBE; index/range → IOOBE; null/value/shape → IAE; state → ISE).

## 2. Doc / hygiene fixes applied (13 edits, 10 files)

1. **BooleanMatrix** — `toMultilineString` example `"[1, 2]\n[3, 4]"` → `"[true, false]\n[false, true]"`.
2. **Byte/Short/Int/Long/Float/Double/Char** (7) — added `@see #zipWith(...)` to `subtract` (symmetry with `add`).
3. **Matrix** — `equals` cast `(Matrix<T>)` → `(Matrix<?>)` (removes the family's only unsuppressed unchecked-cast warning; zero behavior change).
4. **Float** (×3) / **Double** (×2) — `mapToInt/Long/Double` `@param mapper` now states "must not be {@code null}", matching the same file's `map`/`mapToObj`.

### Verification gate — all green
- `mvn -o clean compile` → exit 0.
- `mvn -o javadoc:javadoc` → exit 0 (only benign Maven `sun.misc.Unsafe` warnings; no doclint errors in matrix sources).
- Full suite (`AbacusMatrixTestSuite`, parsed from surefire XML): **tests=2467, failures=0, errors=0, skipped=0, 6569 testcases.**

## 3. Decision list — proposed changes awaiting your go/no-go (none applied)

All are Low severity, no correctness impact. Each is a churn-vs-uniformity judgement or an API-surface addition.

**Consistency refactors (behavior-neutral):**

- **D1 — Unify `isSameShape` call style.** Primitives use `Matrices.isSameShape(this,other)` for add/subtract/and/or/xor but instance `isSameShape(other)` for zipWith; `Matrix<T>` uses static for zipWith. Standardize on the instance form (~18 sites). *Rec: yes, low value but cheap.*
- **D2 — Align `update*` `throws` clauses.** `set{Main,Anti}Diagonal` declare `IllegalArgumentException`; `update{Main,Anti}Diagonal`/`updateAll` don't (though `@throws` javadoc lists it). Uniform family-wide today. Add IAE to `update*` clauses (~45 sites) or leave. *Rec: leave — unchecked, already uniform, pure churn.*
- **D3 — Region `forEach` examples.** The 5-arg `forEach` example uses `int[] subSum; subSum[0] += value` (non-thread-safe) in all 6 numeric classes, while no-arg `forEach` uses `AtomicInteger`. Switch region examples to `AtomicInteger` for consistency (6 files) or leave (prior conscious decision). *Rec: leave, or switch for teaching-safety — your call.*
- **D4 — Thread-safety caveat on parallel transforms.** `map`/`mapToXxx`/`zipWith` (all classes) say only "may be performed in parallel"; `forEach` additionally says "the action must be thread-safe". These genuinely dispatch through `shouldRunInParallel`. Add the caveat family-wide. *Rec: yes — it's a real, if niche, correctness note for callers with stateful mappers.*

**Micro-cleanups:**

- **D5 — Matrices** `resolveCommonAssignableType`/`resolveCommonElementType` are `protected static` in a `final` class → change to package-private (`protected` grants nothing). *Rec: yes, trivial.*
- **D9 — AbstractMatrix** polish: (a) region `forEachIndices` validates bounds-before-null while full-matrix validates null-first — align order; (b) fold the redundant `action::accept` identity wrapper (already `//noinspection`); (c) rename abstract `length(A a)` param to drop `@SuppressWarnings("hiding")`; (d) unify the "other matrix" param name (`other`/`m`/`x`). *Rec: (b),(c) yes; (a),(d) optional.*
- **D10 — Matrix** `resize`/`extend` `@throws ArrayStoreException` is stated unconditionally but only fires when the matrix grows; add "when the operation adds new cells". *Rec: optional (borderline; `@throws` conventionally describes a "may throw" condition).*

**API-surface additions (BC-free, but new public API):**

- **D6 — BooleanMatrix** completeness: add `not()` (natural companion to `and`/`or`/`xor`) and optionally `countFalse()/allFalse()/anyFalse()`. *Rec: `not()` yes; the false-predicates optional.*
- **D7 — `mapToInt`/`mapToLong`/`mapToDouble` on Byte/Short/Char.** Today only Int/Long/Float/Double (+generic) have the `mapTo*` family; the small-int types offer only eager `toXxxMatrix` widening. Adding functional `mapTo*` would make the numeric family uniform. *Rec: your call — deliberate boundary today.*
- **D8 — Matrices** surface: (a) generic N-ary zip is named `zip` while primitives use `zipToObj` — rename for symmetry, or keep (suffix = family-change convention); (b) `zip(Collection ...)` param is `coll` but validation messages say `matrices[idx]` — rename param to `matrices`; (c) `ByteMatrix` has only `zipToInt` (no `zipToLong`/`zipToDouble`); (d) no zip helpers for Float/Short/Char/Boolean; (e) `forEachCartesianIndices` restricts its action to `RuntimeException` while `forEachIndices` allows checked `E`. *Rec: (b) yes (message/param mismatch); rest optional.*

## 4. Won't-fix (verified non-issues)

- `toMultilineString` example for **CharMatrix** — digit chars validly render `"[1, 2]"`; family-uniform; left.
- **LongMatrix** `#of(long[]...)` vs `#of(long[][])` — uniform family-wide; varargs form matches the real signature; both resolve.
- **DoubleMatrix** "double-precision doubles" — deliberate parallel to FloatMatrix "single-precision floats".
- `rowStreams(from,to)` missing `isEmpty()` guard — **required** so an `N×0` matrix yields N empty row-streams (verified all 8 classes omit it identically).
- No `mapToFloat` anywhere — `float` is never a `mapTo` target by convention.
- Various rejected agent leads (phantom div-by-zero, phantom `+=` races, `extend` product-overflow) — each traced and dismissed; recorded in the per-file `*_findings.md`.

---

Per-file agent reports: `DoubleMatrix_findings.md`, `IntMatrix_findings.md`, `LongMatrix_findings.md`,
`FloatMatrix_findings.md`, `ByteMatrix_findings.md`, `ShortMatrix_findings.md`, `CharMatrix_findings.md`,
`BooleanMatrix_findings.md`, `Matrix_findings.md`, `Matrices_findings.md`, `AbstractMatrix_ParallelMode_findings.md`.
Dispositions: `LEDGER.md`.
