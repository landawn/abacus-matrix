# Source review final report — 2026-07-10

## Scope and coverage

All 12 Java source files under `src/main/java` were assigned exactly once and reviewed line by line by the read-only agent listed in `state.md`. The main agent consolidated findings, checked callers/tests/sibling variants, personally confirmed each accepted bug, made all edits, and verified the result. No dependencies or versions changed, no public API was added, and no broad refactor was made.

## Workstream 1 — confirmed bug fixes

### BUG-001 — generic 1x1 repeat changed runtime storage behavior

- Severity/confidence: medium / confirmed.
- Trace: a heterogeneous `Matrix<Object>` backed by `Integer[]` and `String[]` rows took the new `copy()` shortcut for `repeatElements(1, 1)` or `repeatMatrix(1, 1)`. The copied rows retained narrow runtime component types; a subsequent valid `Object`-typed write could throw `ArrayStoreException`.
- Fix: removed only the unsafe generic 1x1 shortcuts, restoring the normal uniform-element-type allocation path. Primitive shortcuts remain.
- Behavior change: generic 1x1 repetition again returns storage compatible with the matrix's resolved element type.
- Regression: `MatrixTest#testRepeatByOne_UsesUniformElementTypeStorageForHeterogeneousRows`; confirmed RED with `ArrayStoreException`, then GREEN.

### BUG-002 — common element type depended on matrix order

- Severity/confidence: medium / confirmed.
- Trace: folding `ArrayList`, `Vector`, and `RandomAccess` pairwise resolved to `Object` in one order and `RandomAccess` in another.
- Fix: resolve candidates against every distinct original element type, rank by aggregate inheritance/interface distance and deterministic specificity tie-breakers.
- Behavior change: zip result storage is order-independent. In cases previously widened incorrectly to `Object[][]`, an incompatible operator result can now correctly raise `ArrayStoreException`.
- Regression: `MatricesTest#testZip_collectionBinaryOperator_commonElementTypeIsOrderIndependent`; confirmed RED (`Object` instead of `RandomAccess`), then GREEN for three permutations.

### BUG-003 — live backing rows were unsafe as mutation sources

- Severity/confidence: medium / confirmed.
- Scope: all nine concrete variants (`Matrix` and the eight primitive matrices), in `setColumn`, `setAntiDiagonal`, and offset `fill`.
- Trace: when the source array was a live backing row, an early target write changed a later source value. Two-dimensional offset fill had the analogous row-alias problem.
- Fix: added package-private snapshot helpers in `AbstractMatrix`; clone only source rows that are identity-equal to backing rows, with an exact-backing-array fast branch. Applied to all 27 affected call sites.
- Behavior change: these mutators now behave as if aliased source values were captured before writes begin.
- Regressions: one `testAliasedArraySourcesAreSnapshotted` per concrete test class, covering all three paths. Confirmed RED as 9 failures, then GREEN across 5,701 targeted tests.

## Workstream 2 — Javadoc and comments

153 contract/comment locations were corrected:

| Class | Locations |
|---|---:|
| `AbstractMatrix` | 3 |
| `ParallelMode` | 0 |
| `Matrix` | 16 |
| `Matrices` | 65 |
| `BooleanMatrix` | 6 |
| `ByteMatrix` | 6 |
| `CharMatrix` | 6 |
| `ShortMatrix` | 6 |
| `IntMatrix` | 10 |
| `LongMatrix` | 10 |
| `FloatMatrix` | 11 |
| `DoubleMatrix` | 14 |

The edits include 54 standardized abnormal-behavior warnings, 56 parallel-callback thread-safety contracts, 32 primitive constructor/factory null-input contracts, corrected lazy exception documentation and tags, runtime array-store contracts, backing-storage/view warnings, a locale-stable example, Dataset name constraints, and the correct 24-bit `float` significand wording. A structural scan found no undocumented public non-`@Override` method. Offline Javadoc generation passed.

## Workstream 3 — logging and error messages

- Logging audit: the sole production logger use is a debug-level, debug-guarded diagnostic in `Matrices`; its level, construction cost, context, and exception handling were appropriate. No logging code changed.
- Exception message: negative stream element counts now report `Matrix stream element count cannot be negative: <count>` instead of incorrectly saying the count is too large. Severity/confidence: low / confirmed. Exception type and oversized-count behavior are unchanged.
- Regression: `AbstractMatrixTest#testToArrayLength_reportsNegativeAndOversizedCountsPrecisely`.

## Tests added

13 regression/example tests were added:

- 9 aliased-source tests, one per concrete variant.
- 1 generic repeat storage test.
- 1 common-element-type order test.
- 1 stream-size message test.
- 1 locale-independent Javadoc example test.

## Verification

- Baseline: 6,572 tests, 0 failures/errors/skips.
- Targeted concrete-variant GREEN: 5,701 tests, 0 failures/errors/skips.
- Targeted resolver GREEN: 1 test, 0 failures/errors/skips.
- Targeted message GREEN: 1 test, 0 failures/errors/skips.
- Offline Javadoc: BUILD SUCCESS.
- Final full suite: 6,585 tests, 0 failures/errors/skips (baseline + 13).

Because report-directory deletion was not authorized after the automated approval quota was exhausted, final acceptance was based exclusively on fresh Maven console aggregates and command exit codes, not stale Surefire XML.

## Remaining findings and false positives

No accepted finding remains unfixed. Newly documented deliberate-design conclusions:

- Primitive 1x1 repeat shortcuts are safe; only the generic shortcut was storage-unsafe.
- `setRow` is overlap-safe through `System.arraycopy`; `setMainDiagonal` does not have the cross-element alias hazard.

The primary resumable ledger is `scripts/review_2026-07-10/state.md`. The compatibility pointer requested by the task is under `scripts/code_doc_comment_log_review_2026-07-10/`.
