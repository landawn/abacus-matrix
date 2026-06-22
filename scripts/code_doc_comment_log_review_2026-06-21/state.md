# Code / Javadoc / Comment / Logging Review — State (2026-06-21)

Primary ledger path: `scripts/code_doc_comment_log_review_2026-06-21/state.md`
(Prompt also names `scripts/review_<YYYY-MM-DD>/state.md`; this ledger uses the more specific
final-report path and records the same required fields.)

**Git-ignore check:** `.gitignore` ignores only `/target/`, `/.factorypath`, `/classpath.txt`,
`/windows-browser-app/`. This ledger path is NOT git-ignored (tracked).

## Context from prior passes (do NOT re-litigate)
- `src/main/java` had **no changes since the 2026-06-17 review** (`git log` since 06-16 shows only
  commit 401c316, the 06-17 review pass itself). Working tree src is clean == committed post-06-17 state.
- Six prior multi-agent passes (06-09/10, 06-11, 06-12, 06-13, 06-14, 06-17) concluded the family is
  essentially clean. 06-14 found ZERO actionable items; 06-17 found ZERO confirmed bugs (only Javadoc +
  logging/exception-message polish). Memory guidance: weight effort toward **cross-variant divergence**
  detection; do not expect new defects.

## Scope guardrails
- No dependency/version bumps, no new public API, no signature changes, no refactors beyond minimal fix.
- Sub-agents READ-ONLY; main agent is the ONLY editor.
- Confirm code bugs by failing regression test OR main-agent concrete input→output trace before editing.
- Stage edits by workstream: (1) bug fixes, (2) Javadoc/comment-only, (3) logging/exception messages.
- Preserve public API & behavior unless a confirmed bug requires change (call out behavior changes).

## Deliberate-design / Known-false-positive list (carried forward from 06-17 ledger + memory)
1. Live backing-array exposure: `internalArray()`, `rowView(int)`, `mutateAsFlat(...)` are deliberate
   performance APIs (documented). Not leaks.
2. Null/empty-as-empty is a deliberate library convention in some APIs; only directly-comparable
   cross-variant inconsistencies are candidates.
3. Primitive arithmetic narrowing/wraparound (add/subtract/matmul on byte/short/char/etc.) is intentional
   and documented; caller-supplied map/zipWith operators are distinguished from built-in arithmetic.
4. Validation-exception policy (FIXED convention; do not "fix"): null/value→IAE (`N.checkArgNotNull`),
   negative/shape/empty-where-required→IAE, index/range/offset→IOOBE (centralized in AbstractMatrix),
   **get/set/get(Point)/set(Point) deliberately keep AIOOBE via raw access**, square/too-large→ISE,
   exhausted iterator→NoSuchElementException. Keep all 8 primitives + Matrix<T> aligned.
5. `Matrix<T>` intentionally rejects null arrays and `diagonals(null,null)` while primitives tolerate
   them — documented divergence, do NOT "fix".
6. `Matrices.isParallelizable(AbstractMatrix,long)` FORCE_ON returns true for negative count — 06-17
   deferred (low/suspected, no concrete caller bug). Do not change without a concrete failing trace.
7. `Matrix.get/set` raw array index messages — 06-17 deferred (changing hot direct-access path risks
   behavior change). Keep deferred unless a confirmed contract bug.
8. `AbstractMatrix.forEachIndices` region overload thresholds parallelism on region cell count vs full
   elementCount for the no-arg overload; region Javadoc doesn't spell out the heuristic basis — 06-14
   deliberately left as-is (omission of an internal heuristic detail, not a wrong statement; no sibling
   documents its threshold basis).
9. ReviewBugfix regression coverage settles: copy empty ranges, matmul factory construction, diagonal
   stream square validation, generic ArrayStoreException for narrowed runtime arrays, deep
   Matrix.hashCode(), row-major traversal in object-matrix updates/replacements, Matrix.repeat enum
   getDeclaringClass(), 3-arg checkShapeForZip "third" labeling.

## Coverage accounting — every src/main/java file assigned to exactly one agent (no overlap, no gaps)
Production Java files found: 12. Assigned: 12.

| Batch | Agent | Files | Status |
|-------|-------|-------|--------|
| 1 | A — shared/object infra | AbstractMatrix.java, Matrix.java, Matrices.java, ParallelMode.java | pending |
| 2 | B — integer primitives | ByteMatrix.java, ShortMatrix.java, IntMatrix.java, LongMatrix.java | pending |
| 3 | C — float/char/boolean | BooleanMatrix.java, CharMatrix.java, FloatMatrix.java, DoubleMatrix.java | pending |

## Build/Test baseline (sandbox disabled for ~/.m2 local-cache access, per 06-17 protocol)
- `rm -rf target/surefire-reports` then `mvn -o clean compile -q`: EXIT 0 (compiles clean).
- `mvn -o test -q`: EXIT 0 (testFailureIgnore — exit code not authoritative).
- Authoritative surefire XML `TEST-com.landawn.abacus.AbacusMatrixTestSuite.xml` (the only report file):
  header `tests=2462, failures=0, errors=0, skipped=0, flakes=0`; `<testcase>` element count = 6553.
  (Both counts are the same run; 2462 = suite header attr, 6553 = individual testcases. 06-17 recorded 6553.)
- **BASELINE = GREEN (0 failures / 0 errors).** Record AFTER-state against this.

## Coverage status (final): all 3 batches reviewed; all 12 files reviewed.
| Batch | Agent | Status |
|-------|-------|--------|
| 1 | A — shared/object infra (AbstractMatrix, Matrix, Matrices, ParallelMode) | clean (no edits) |
| 2 | B — integer primitives (Byte, Short, Int, Long) | fixed (LongMatrix doc) / rest clean |
| 3 | C — float/char/boolean (Boolean, Char, Float, Double) | fixed (CharMatrix doc) / rest clean |

## Findings (status: lead / verified / fixed / clean / deferred / rejected)

### Workstream 1 — Bug fixes  → ZERO edits
- No correctness/edge/concurrency/overflow bugs reported by any of the 3 read-only agents.
  Floating-point semantics (NaN/Infinity/signed-zero in equals/hashCode/replaceIf/resize-extend raw-bits
  fast path) independently traced into abacus-common and confirmed CORRECT. matmul accumulation, range
  sizing, reshape/extend index math all re-verified clean. Result consistent with 6 prior passes.
- Carried-forward deferrals (items 6 & 7 in false-positive list) remain DEFERRED — no concrete failing
  trace produced; out of scope for this low-risk pass.

### Workstream 2 — Javadoc/comments  → 3 edits, 2 files (comment-only, no behavior change)
- FIXED [low/confirmed] `LongMatrix.diagonals(long[],long[])` @return + @throws — LongMatrix.java:465-466.
  Verified by grep: 7 of 8 primitive classes share one @return/@throws wording; LongMatrix was the lone
  outlier (semantically equivalent text). Aligned Long's two tags to the 7-class majority verbatim. Long's
  richer body prose (incl. "shared empty matrix is returned", line 445) is unchanged, so no info lost.
- FIXED [low/confirmed] `CharMatrix.updateRow` + `CharMatrix.updateColumn` — added the `updateRow(0,null)` /
  `updateColumn(0,null)` → IllegalArgumentException example line that all 8 siblings demonstrate (the IAE
  @throws tag was already present; only the in-<pre> example demonstration was missing). CharMatrix.java
  (~876, ~915).

### Workstream 3 — Logging/exception messages  → ZERO edits
- No new logging or exception-message issues. The 06-17 pass's family-wide message improvements
  (diagonal/zip/fill messages, guarded parallel-stream feature-detection catch in Matrices) are intact and
  uniformly applied — no variant was missed. No secrets/PII, no log-and-throw, no swallowed exceptions.

### REJECTED on verification (agent leads that did not survive source check)
- [Agent A F6] "Float/Double `random(int,int)` @throws omit the representable-shape note" — FALSE/STALE.
  FloatMatrix.java:242-243 already documents "or if the resulting shape is not representable" and the impl
  calls `checkRepresentableShape` (248); DoubleMatrix likewise. No edit.
- [Agent B] "align Byte/Short/Int `diagonals` wording UP to LongMatrix" — wrong direction (Long is the 1-of-8
  outlier); rejected in favor of aligning Long DOWN to the 7-class majority (less churn, correct majority).

### DEFERRED — verified-real but cosmetic / churn-not-justified (logged, not edited)
- forEach(Consumer) empty-matrix example reuses a non-zero accumulator ("// still returns 4") in Float/Double/
  Char vs the integer family's clearer fresh-counter "0 (no elements visited)" + null-action example. Real
  cross-variant style divergence but example-only and not incorrect; low value, 3 files. Left as-is.
- Pure cosmetic drift (no contract/accuracy impact): article "a/an" before Int*; `{@code}` wrapping of numeric
  literals/0x0; ByteMatrix stackHorizontally example `rowView(0)` vs `rowCopy(0)`; ByteMatrix updateColumn
  OOB example index 9 vs 5; Float/Double map(UnaryOperator) template phrasing; BooleanMatrix mapToObj @param/
  @return phrasing; Float/Double replaceIf @param phrasing (both accurate); ShortMatrix conversion-doc depth;
  internal local-variable name differences. All accurate as written — not edited (no-noise / minimal-change).

## New false positives appended to deliberate-design list
- Float/Double `random` @throws ALREADY documents the unrepresentable-shape case (impl calls
  checkRepresentableShape) — do not re-flag.
- `LongMatrix.diagonals` body prose is intentionally more detailed than siblings; only the @return/@throws
  TAGS were the divergence (now aligned). Body-prose richness is fine, not a divergence to "fix".

## Build/Test after-state (sandbox disabled for ~/.m2)
- `mvn -o clean test -q`: EXIT 0. Surefire XML: `tests=2462, errors=0, skipped=0, failures=0`; testcases=6553.
- BASELINE 2462/0/0 (6553) == AFTER 2462/0/0 (6553). No regressions. No new tests (no code fixes ⇒ none required).
- `mvn -o -DskipTests javadoc:javadoc`: EXIT 0, no errors/warnings on edited files (Javadoc well-formed).
- CRLF: working files are uniformly LF; Edit-tool insertions are LF; `git diff --stat` = 4 ins/2 del (no churn).

## Staging log
- 2026-06-21: Created ledger, coverage map; carried forward false-positive list.
- 2026-06-21: Established baseline (compile EXIT 0; suite 2462/0/0, 6553 testcases — GREEN).
- 2026-06-21: Dispatched 3 read-only review agents (one batch of 3), partitioned no-overlap/no-gap.
- 2026-06-21: Triaged + personally verified every lead against source. WS1/WS3 clean; WS2 3 edits.
- 2026-06-21: Applied WS2 comment-only edits (LongMatrix x1, CharMatrix x2). No WS1/WS3 edits.
- 2026-06-21: Re-verified: clean compile, full suite 2462/0/0 (== baseline), javadoc:javadoc clean.
- REVIEW COMPLETE.
