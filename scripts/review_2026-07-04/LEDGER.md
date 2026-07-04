# Deep Review 2026-07-04 — Findings Ledger & Dispositions

**Pass:** 13th full line-by-line review of `src/main/java/com/landawn/abacus/matrix/` (12 files, ~44,000 lines).
**Method:** 11 parallel review agents (one file each; last owns AbstractMatrix+ParallelMode) proposed findings → main thread independently verified every finding against source before any edit.
**Backward compatibility:** explicitly not a constraint.

## Headline result

- **Correctness bugs found: 0** across all 12 files (consistent with the prior 12 passes).
- All findings were Low-severity doc / consistency / design items.
- Every agent's per-file report is in this directory (`<Class>_findings.md`).

---

## Verification of the recurring cross-class themes (what the grep sweeps established)

| Theme | Agent claim | Verified reality | Disposition |
|---|---|---|---|
| `throws IllegalArgumentException` omitted on `update*` vs declared on `set*` | "internal inconsistency" (Double/Boolean/Byte) | **Uniform across all 9 classes**: `set{Main,Anti}Diagonal` → `throws ISE, IAE`; `update{Main,Anti}Diagonal`/`updateAll` → `throws [ISE,] E` (IAE undeclared). `@throws` javadoc lists IAE everywhere. IAE is unchecked ⇒ declaration optional. | By-design / decision-list (D2). Family-wide pattern, not a per-file wart. |
| `Matrices.isSameShape(this,other)` (arithmetic) vs instance `isSameShape(other)` (zipWith) | intra-file inconsistency (Double/Float/Boolean) | **Uniform family-wide**: primitives use static for add/subtract/and/or/xor, instance for zipWith; `Matrix<T>` uses static for zipWith. Both equivalent. | Cosmetic / decision-list (D1). |
| `toMultilineString` example `"[1, 2]\n[3, 4]"` | type-wrong for Boolean/Char | **All 10 files share the boilerplate.** Correct for numerics; valid for Char (digit chars render those glyphs); **impossible for Boolean** (renders true/false). | **FIXED (Boolean only).** Char/others left (family-uniform, not wrong). |
| `subtract` missing `@see #zipWith` that `add` has | Byte/Short | **Family-wide**: all 7 numeric classes' `add` carries `@see #zipWith`, `subtract` omits it. | **FIXED (all 7).** |
| `mapToInt/Long/Double` `@param mapper` omits "must not be null" | FloatMatrix outlier | Uniformly omitted across Int/Long/Float/Double `mapTo*`; but in **Float & Double** the sibling `map`/`mapToObj` *do* promise it ⇒ those two files internally inconsistent. Int/Long omit everywhere (internally consistent). Null contract documented via `@throws` on all. | **FIXED (Float ×3, Double ×2).** Int/Long left (internally consistent). |
| Region `forEach` example uses `int[] subSum[0]+=value` (non-thread-safe) while no-arg uses `AtomicInteger` | ShortMatrix | **Uniform across all 6 numeric classes'** region example; prior pass consciously left region-forEach. Examples run on tiny (never-parallel) matrices. | Decision-list (D3). |
| `map`/`mapToXxx`/`zipWith` omit the "action must be thread-safe" caveat that `forEach` documents | Matrix | **Family-wide omission** (verified IntMatrix.map etc.). These genuinely dispatch through `shouldRunInParallel`. | Decision-list (D4). |
| `mapToXxx` absent on Byte/Short/Char | Short "possible gap" | **By design**: only Int/Long/Float/Double (+generic Matrix) have `mapTo*`; targets = {Int,Long,Double}\self (+Float). Byte/Short/Char/Boolean have none, uniformly. | Decision-list (D7) — API addition, not a defect. |
| `LongMatrix #of(long[]...)` vs `#of(long[][])` | intra-file link inconsistency | **Uniform family-wide**: class-javadoc uses varargs form `of(x[]...)` (matches real signature), `copyOf` uses `of(x[][])`. Both resolve. | Won't-fix (not wrong). |

---

## APPLIED (Fix-now / Doc-only / zero-behavior hygiene)

| # | File(s) | Change | Category | Rationale |
|---|---|---|---|---|
| A1 | BooleanMatrix | `toMultilineString` javadoc example `"[1, 2]\n[3, 4]"` → `"[true, false]\n[false, true]"` | Doc | A boolean matrix can never render integer glyphs; the boilerplate was copied from a numeric sibling. Only class where the example is impossible. |
| A2 | Byte, Short, Int, Long, Float, Double, Char | Added `@see #zipWith(XxxMatrix, Throwables.XxxBinaryOperator)` to `subtract` javadoc | Doc | `add` already cross-references `zipWith`; `subtract` is exactly as specialized. Restores add/subtract symmetry family-wide. |
| A3 | Matrix | `equals`: `(Matrix<T>) obj` → `(Matrix<?>) obj` (+ local type) | Hygiene (zero-behavior) | Removes the lone unsuppressed unchecked-cast warning in the family (`equals` only reads `rowCount/columnCount/a`, all reachable via `Matrix<?>`); primitive siblings already cast to a non-generic type. Identical bytecode/behavior. |
| A4 | Float (×3), Double (×2) | `mapToInt/Long/Double` `@param mapper` gains "; must not be {@code null}" | Doc | Their own `map`/`mapToObj` promise it ⇒ removes a real within-file inconsistency. (Int/Long omit everywhere and are left internally consistent.) |

All applied changes are javadoc except A3 (a semantically-identical cast). Verified: `mvn -o clean compile` exit 0; `mvn -o javadoc:javadoc` exit 0 (only benign Maven/Unsafe warnings); full suite green (see REPORT.md).

---

## PROPOSED — design/consistency/API changes awaiting go/no-go (NOT applied)

See REPORT.md "Decision list" (D1–D10). None are correctness issues; each is a judgement call about churn vs. uniformity or an API-surface addition.
