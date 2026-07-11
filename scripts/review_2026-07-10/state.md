# Multi-agent source review state — 2026-07-10

## Scope and rules

- Scope: every Java source under `src/main/java` (12 files).
- Review workstreams: (1) confirmed bugs, (2) Javadoc/comments, (3) logging and exception/error messages.
- Sub-agents are read-only. Only the main agent edits after independent verification.
- No dependency changes, public API additions, or broad refactors.
- Existing uncommitted changes in nine matrix implementations predate this pass and are treated as baseline; this review will not overwrite or reclassify them.
- This ledger is **not git-ignored**. `.gitignore` only excludes build/tool output, and prior `scripts/review_*` ledgers are tracked, so this file should remain reviewable history.
- `scripts/code_doc_comment_log_review_2026-07-10/state.md` is a stable pointer to this authoritative file, satisfying the final-report path convention without duplicating mutable state.

## Coverage accounting

Each production file is assigned exactly once.

| Batch | Reviewer assignment | File | Status |
|---|---|---|---|
| 1 | `/root/core_base_retry` | `src/main/java/com/landawn/abacus/matrix/AbstractMatrix.java` | in-review |
| 1 | `/root/core_base_retry` | `src/main/java/com/landawn/abacus/matrix/ParallelMode.java` | clean |
| 1 | `/root/object_matrix` | `src/main/java/com/landawn/abacus/matrix/Matrix.java` | in-review |
| 1 | `/root/matrix_utilities` | `src/main/java/com/landawn/abacus/matrix/Matrices.java` | in-review |
| 2 | `/root/small_primitives` | `src/main/java/com/landawn/abacus/matrix/BooleanMatrix.java` | in-review |
| 2 | `/root/small_primitives` | `src/main/java/com/landawn/abacus/matrix/ByteMatrix.java` | in-review |
| 2 | `/root/small_primitives` | `src/main/java/com/landawn/abacus/matrix/CharMatrix.java` | in-review |
| 2 | `/root/integral_primitives` | `src/main/java/com/landawn/abacus/matrix/ShortMatrix.java` | in-review |
| 2 | `/root/integral_primitives` | `src/main/java/com/landawn/abacus/matrix/IntMatrix.java` | in-review |
| 2 | `/root/integral_primitives` | `src/main/java/com/landawn/abacus/matrix/LongMatrix.java` | in-review |
| 2 | `/root/floating_primitives` | `src/main/java/com/landawn/abacus/matrix/FloatMatrix.java` | in-review |
| 2 | `/root/floating_primitives` | `src/main/java/com/landawn/abacus/matrix/DoubleMatrix.java` | in-review |

Coverage: **12 assigned / 12 total; no overlap; no gaps.**

## Baseline

- Working tree at start: nine matrix implementation files modified by the immediately preceding performance/hygiene pass; unrelated untracked `.claude/` present and untouched.
- Offline clean compile: `mvn -o clean compile` exited 0; 12 production files compiled; `BUILD SUCCESS` (2026-07-10 10:05 PDT).
- Stale-report check: `target/surefire-reports` did not exist after the clean compile; verified the resolved path was inside the workspace before the clear step.
- Authoritative full-suite baseline: `mvn -o test` exited 0. Surefire summary: **Tests run: 6572, Failures: 0, Errors: 0, Skipped: 0** (2026-07-10 10:06 PDT). This is the baseline including the pre-existing nine-file performance/hygiene diff.

## Prior deliberate-design / known false-positive list

Do not re-litigate these without new evidence:

1. `rowStreams(from,to)` intentionally omits an `isEmpty()` guard so an `N×0` matrix yields `N` empty row streams.
2. Direct `get`/`set` methods intentionally expose native array index exceptions; validated/range APIs use explicit `IndexOutOfBoundsException`, null/value/shape errors use `IllegalArgumentException`, and non-square state uses `IllegalStateException`.
3. Missing `mapToInt`/`mapToLong`/`mapToDouble` families on Byte/Short/Char/Boolean are deliberate API boundaries; adding them is out of scope.
4. `forEachCartesianIndices` is restricted to `RuntimeException` because its parallel stream callbacks cannot propagate checked exceptions.
5. Matrix multiplication parallelizes independent output cells and never the `k` accumulator for one cell; prior suspected `+=` races were false positives.
6. `extend` does not need a flat `Integer.MAX_VALUE` product guard because two-dimensional row arrays need only individually representable dimensions; prior product-overflow reports were rejected.
7. `N×0` reshape/stream guards and the diagonal `a[cursor][cursor++]` expression were previously traced and confirmed correct.
8. Aspect-ratio loop branches and boxed/conversion traversal were previously reviewed as deliberate; require a concrete regression or benchmark before proposing change.
9. Generic `Matrix` update/replace methods are intentionally sequential and document that contract.
10. Varargs-vs-array Javadoc links such as `of(long[]...)` and `of(long[][])` both resolve and are not inconsistencies.
11. `DoubleMatrix` “double-precision doubles” deliberately parallels `FloatMatrix` terminology.
12. Block-level HTML auto-closing around `<p>`/`<ul>` is doclint-valid and family-consistent; do not flag formatting alone.
13. The generic `Matrix` repeat-by-one shortcut was not safe for heterogeneous runtime row types, but the corresponding primitive shortcuts are safe because primitive row component types cannot narrow.
14. `setRow` is alias-safe through array-copy overlap semantics, and `setMainDiagonal` is alias-safe because the only write to an aliased source row is a self-assignment at the matching diagonal index; the alias bug is limited to `setColumn`, `setAntiDiagonal`, and two-dimensional `fill`.

## Findings ledger

### BUG-001 — Generic repeat-by-one fast path preserves narrowed row storage

- Workstream: confirmed bug
- File: `src/main/java/com/landawn/abacus/matrix/Matrix.java:2558-2560,2621-2623`
- Severity: medium
- Confidence: confirmed
- Reproduction: a supported `Matrix<Object>` backed by heterogeneous `Integer[]` and `String[]` rows returns narrowed cloned rows from `repeatElements(1, 1)` / `repeatMatrix(1, 1)`; setting a `String` into the first result row throws `ArrayStoreException`. The normal repeat algorithms allocate uniform `Object[]` rows and accept the write.
- RED: `mvn -o -Dtest=MatrixTest#testRepeatByOne_UsesUniformElementTypeStorageForHeterogeneousRows test` — 1 test, 1 failure (`Unexpected exception ... ArrayStoreException`). An initial test-compile attempt failed because the assertion was not qualified; corrected before recording the behavioral RED.
- Fix: removed only the two generic-Matrix shortcuts. Primitive shortcuts remain behaviorally equivalent.
- Main-agent verification: personally traced `copy()` and both normal allocation paths; fix applied, GREEN pending.

### BUG-002 — Common element type for collection zip depends on input order

- Workstream: confirmed bug
- File: `src/main/java/com/landawn/abacus/matrix/Matrices.java:3941-3948`
- Severity: medium
- Confidence: confirmed
- Reproduction: element types `[ArrayList, Vector, RandomAccess]` resolve to `Object`, while the same inputs ordered `[ArrayList, RandomAccess, Vector]` resolve to `RandomAccess`, even though `RandomAccess` is assignable from every input type. This changes the result runtime element type and weakens the documented `ArrayStoreException` guard.
- RED: `mvn -o -Dtest=MatricesTest#testZip_collectionBinaryOperator_commonElementTypeIsOrderIndependent test` — 1 test, 1 failure (`expected RandomAccess but was Object`). Maven exited 0 because `testFailureIgnore` is enabled; the Surefire failure line is authoritative.
- Fix: rank candidates common to all original input element types instead of pairwise-folding and losing interfaces.
- Main-agent verification: personally traced both input orders through `resolveCommonAssignableType`; fix applied, GREEN pending.

### BUG-003 — Aliased mutator sources are overwritten before later reads

- Workstream: confirmed bug
- Files: all nine concrete matrix implementations; `setColumn`, `setAntiDiagonal`, and offset `fill`
- Severity: medium
- Confidence: confirmed
- Reproduction: passing `rowView(0)` to `setColumn(1, ...)` changes a later source element before it is read; passing the same live row to `setAntiDiagonal` can overwrite a future source position; calling `fill(1, 0, internalArray())` overwrites the next source row before copying it.
- RED: after clearing `target/surefire-reports`, ran `mvn -o -Dtest=BooleanMatrixTest,ByteMatrixTest,CharMatrixTest,ShortMatrixTest,IntMatrixTest,LongMatrixTest,FloatMatrixTest,DoubleMatrixTest,MatrixTest test`. Surefire: **Tests run: 5700, Failures: 9, Errors: 0, Skipped: 0**; exactly one new alias regression failed in each implementation. Maven exited 0 only because `testFailureIgnore` is enabled.
- Fix: snapshot one-dimensional sources only when they are a backing row, and snapshot two-dimensional source rows only when they alias backing rows; preserve the allocation-free non-alias path for column/diagonal setters.
- Main-agent verification: personally traced all three corruptions; fix applied across variants, GREEN pending.

### DOC-001 — `Matrix` contract corrections

- Workstream: Javadoc/comments
- File/lines: `Matrix.java:75-76,138-140,631-639,1696-1723,1945-1946,2086-2087,2693-2694,2862-2866,2909-2913,2946-2950,2957-2959,3736-3740,3789-3792`
- Severity: low
- Confidence: confirmed
- Findings: warning notes lack the required warning marker; region `fill` omits skipped null rows; resize/extend overstate when `ArrayStoreException` can occur; three `zipWith` callbacks omit parallel thread-safety; one example is default-locale-dependent; Dataset converters omit downstream null/empty/duplicate column-name validation.
- Main-agent verification: applied; a regression test was added for the locale-sensitive example; test compilation and doclint pending.

### DOC-002 — `Matrices` callback, warning, lazy-exception, and link contracts

- Workstream: Javadoc/comments
- File/lines: `Matrices.java:651-900,1628,1764,2183,2421,2549,2952,3078,3369,3650-3700,3799`, plus all parallel-capable public zip callback overloads.
- Severity: low
- Confidence: confirmed
- Findings: 51 traversal/zip callback Javadocs omit the thread-safety contract; nine shared-array notes lack the required warning marker; four lazy stream mappers omit accurate `RuntimeException` documentation; one public-only Javadoc links to a package-private helper.
- Main-agent verification: applied across all 51 callback Javadocs and nine warning notes; doclint pending.

### DOC-003 — Core warning-marker normalization

- Workstream: Javadoc/comments
- File/lines: `AbstractMatrix.java:332,367,1163`
- Severity: low
- Confidence: confirmed
- Finding: three unsafe aliasing/mutation notes use `<strong>Unsafe API boundary:</strong>` instead of starting with the required `<p><b>&#9888;` marker.
- Main-agent verification: applied; doclint pending.

### DOC-004 — Primitive factory, callback, precision, and warning contracts

- Workstream: Javadoc/comments
- Files: all eight primitive matrix implementations
- Severity: low
- Confidence: confirmed
- Findings/fixes: added the omitted top-level null-array condition to 32 constructor/factory `@throws` contracts; added the parallel thread-safety caveat to `FloatMatrix.map` and `DoubleMatrix.map`; corrected Float's 24-bit significand wording; normalized shared-backing, live-view, and narrowing/precision warnings to the required warning prefix.
- Main-agent verification: applied consistently across variants; doclint pending.

### Documentation correction counts

Counted by public contract/example/note location (not changed physical lines):

| Class/area | Locations corrected |
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
| **Total** | **153** |

Cross-cutting totals: 54 abnormal-behavior notes normalized to the required warning prefix; 56 parallel callback contracts gained thread-safety requirements; 32 array factory/constructor `@throws` tags gained the omitted top-level null case. The remaining 11 locations cover lazy exception propagation, links, Dataset constraints, null-row behavior, `ArrayStoreException` wording, and example/precision accuracy.

### MSG-001 — Negative stream-array size reported as “too large”

- Workstream: exception/error messages
- File: `AbstractMatrix.java:215`
- Severity: low
- Confidence: confirmed
- Trace: `toArrayLength(-1)` enters the shared invalid-size branch and reports `Matrix stream too large ... -1 elements`; negative is invalid, not too large.
- Fix: retained the exception type and split the negative and greater-than-`Integer.MAX_VALUE` messages.
- Main-agent verification: applied with exact-message coverage; compile/test pending.

### Orchestration note

- The original `/root/core_base` reviewer failed twice before inspection because its selected model was at capacity. Coverage was reassigned, without overlap, to `/root/core_base_retry`.
- Batch 2 agents completed implementation-body tracing and reported no other surviving correctness lead, but their final documentation passes hit the collaboration usage limit. Their interim findings are recorded above; the main agent personally completed the remaining cross-variant documentation checks. No sub-agent edited files.

For each candidate record:

- ID / workstream
- file:line
- severity: low / medium / high / critical
- confidence: suspected / likely / confirmed
- minimal trace or reproduction
- proposed minimal fix
- main-agent verification and disposition

## Workstream staging

### 1. Confirmed bugs

Three confirmed bug families are fixed. Required RED evidence is recorded above; GREEN verification is pending the mandated stale-report cleanup.

### 2. Javadoc/comments

Comment-only edits are applied after the bug fixes. Public-method coverage scan found no undocumented non-`@Override` method. Doclint and example-test verification are pending.

### 3. Logging and exception/error messages

The only logging statement was verified clean and unchanged. One exception message was improved without changing its exception type; build/test verification is pending.

## Verification log

1. `mvn -o clean compile` — exit 0, 12 production files compiled, build success.
2. Cleared/checked `target/surefire-reports` using a resolved workspace-contained absolute path; it was already absent after `clean`.
3. `mvn -o test` — exit 0; Surefire aggregate: 6572 tests, 0 failures, 0 errors, 0 skipped.
4. BUG-002 RED: one targeted test, one failure (`RandomAccess` expected, `Object` returned).
5. BUG-001 RED: one targeted test, one failure (`ArrayStoreException` from narrowed cloned row storage).
6. BUG-003 RED after clearing stale reports: 5700 tests, 9 failures, 0 errors, 0 skipped; exactly the nine new alias regressions failed.
7. Post-fix cleanup/build attempt: blocked because the execution approval service hit its usage limit while authorizing deletion of the verified workspace-local `target/surefire-reports`. No GREEN result has been inferred from stale XML or from Maven's `BUILD SUCCESS` text.
8. `git diff --check` — exit 0. Git emitted only the repository's existing LF-to-CRLF conversion warnings; no whitespace errors.
9. Structural public-method Javadoc scan — no undocumented public method found after excluding methods annotated with `@Override`; authoritative doclint remains pending.
# Final closeout (authoritative; 2026-07-10 17:08 PDT)

This section supersedes any earlier `pending` or `in-review` status below. The review is complete.

## Final coverage accounting

| File | Assigned read-only reviewer | Final status |
|---|---|---|
| `AbstractMatrix.java` | `/root/core_base_retry` | fixed |
| `ParallelMode.java` | `/root/core_base_retry` | clean |
| `Matrix.java` | `/root/object_matrix` | fixed |
| `Matrices.java` | `/root/matrix_utilities` | fixed |
| `BooleanMatrix.java` | `/root/small_primitives` | fixed |
| `ByteMatrix.java` | `/root/small_primitives` | fixed |
| `CharMatrix.java` | `/root/small_primitives` | fixed |
| `ShortMatrix.java` | `/root/integral_primitives` | fixed |
| `IntMatrix.java` | `/root/integral_primitives` | fixed |
| `LongMatrix.java` | `/root/integral_primitives` | fixed |
| `FloatMatrix.java` | `/root/floating_primitives` | fixed |
| `DoubleMatrix.java` | `/root/floating_primitives` | fixed |

Coverage: **12/12 files assigned exactly once; 12/12 reviewed; no gaps or overlaps.** Sub-agents were read-only. The main agent personally traced, edited, and verified every accepted finding.

## Final workstream status

- Workstream 1 — bug fixes: complete. Three medium-severity, confirmed bug families fixed (generic repeat storage, order-dependent common-type resolution, and aliased source arrays across 9 concrete variants). Every fix had a RED regression before the implementation change and GREEN verification after it.
- Workstream 2 — Javadoc/comments: complete. 153 contract/comment locations corrected across 11 classes; `ParallelMode` required no change. Offline Javadoc generation passed.
- Workstream 3 — logging/messages: complete. Logging audit found no change warranted. One low-severity, confirmed negative stream-size exception message was made precise and regression-tested.
- Remaining unfixed findings: none. Broader changes, public API additions, dependency changes, and refactors were not made.

## Final verification

- Baseline: `mvn -o clean compile` — exit 0; 12 production files compiled.
- Baseline suite: `mvn -o test` — **Tests run: 6572, Failures: 0, Errors: 0, Skipped: 0**.
- RED, generic repeat: 1 test, 1 failure (`ArrayStoreException`).
- RED, common element type: 1 test, 1 failure (expected `RandomAccess`, got `Object`).
- RED, aliased array sources: 5700 tests, 9 failures (one new regression per concrete variant).
- GREEN, concrete variants: `mvn -o -Dtest=BooleanMatrixTest,ByteMatrixTest,CharMatrixTest,ShortMatrixTest,IntMatrixTest,LongMatrixTest,FloatMatrixTest,DoubleMatrixTest,MatrixTest test` — **Tests run: 5701, Failures: 0, Errors: 0, Skipped: 0**.
- GREEN, common element type: `mvn -o -Dtest=MatricesTest#testZip_collectionBinaryOperator_commonElementTypeIsOrderIndependent test` — **Tests run: 1, Failures: 0, Errors: 0, Skipped: 0**.
- GREEN, message precision: `mvn -o -Dtest=AbstractMatrixTest#testToArrayLength_reportsNegativeAndOversizedCountsPrecisely test` — **Tests run: 1, Failures: 0, Errors: 0, Skipped: 0**.
- Contract validation: `mvn -o -DskipTests javadoc:javadoc` — exit 0, BUILD SUCCESS.
- Authoritative final suite: `mvn -o test` — **Tests run: 6585, Failures: 0, Errors: 0, Skipped: 0** (baseline + 13 regressions).
- `git diff --check` — exit 0 before final ledger-only closeout.

Protocol deviation: the requested pre-run deletion of `target/surefire-reports` could not be authorized because the automated approval reviewer exhausted its quota. No indirect deletion or move was attempted. Verification therefore used only fresh console aggregates and process exit codes from the commands above; pre-existing XML reports were not consulted.

## Deliberate-design / newly confirmed false positives

- Primitive 1x1 repeat shortcuts are safe: primitive array rows cannot acquire narrower runtime component types. The generic shortcut was not safe and was removed.
- `setRow` is overlap-safe because it delegates to `System.arraycopy`; `setMainDiagonal` can only read and write the same element when a backing row is supplied. Aliasing fixes were correctly limited to `setColumn`, `setAntiDiagonal`, and offset `fill`.

Detailed final report: [`REPORT.md`](REPORT.md).

---
