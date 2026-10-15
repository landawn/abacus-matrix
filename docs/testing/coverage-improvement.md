# Coverage improvement

The follow-up to the test cleanup reaches **100% method coverage, 99.83% line coverage, and 99.21% branch coverage**. It adds 374 test methods and 909 invocations across eleven existing canonical test files. Production sources, test discovery, and coverage exclusions are unchanged.

The baseline is commit `95e951063f0b502e050194eda97e3024142abd9d`. Both final runs used JDK 25, compiler release 21, Maven 3.9.16, and JaCoCo 0.8.15. Every coverage run started with `clean`; execution data was not accumulated across runs.

## Results

| Measure | Before | After |
| --- | ---: | ---: |
| Lines | 7,846 / 8,282 (94.74%) | 8,268 / 8,282 (99.83%) |
| Branches | 4,734 / 5,320 (88.98%) | 5,278 / 5,320 (99.21%) |
| Methods | 1,573 / 1,648 (95.45%) | 1,648 / 1,648 (100%) |
| Instructions | 51,636 / 54,731 (94.35%) | 54,606 / 54,731 (99.77%) |
| Classes | 77 / 77 (100%) | 77 / 77 (100%) |
| Test method declarations | 6,627 | 7,001 |
| Test invocations | 6,658 | 7,567 |

The normal and randomized runs both passed all 7,567 invocations with zero failures, errors, or skips. They produced identical coverage counters and line-level instruction/branch coverage. No previously covered line or branch was lost. The exact-duplicate audit found zero groups, and the assertion audit found no methods without assertions. Organization, naming, formatting, and whitespace checks passed.

## What the tests exercise

- Stream iterator advancement, remaining counts, row transitions, and exhaustion, including negative and oversized advances.
- Matrix values and preserved inputs under explicit sequential and parallel arithmetic, mutation, and traversal modes.
- Resizing, padding, repetition, empty logical dimensions, copy independence, and argument/shape validation.
- Checked arithmetic, widened results, and final narrowing overflow.
- Every concrete matrix type's collection stacking, including dimension overflow checked with bounded memory.
- Independent argument arrays for parallel collection zip callbacks, even when array reuse is requested.
- Deterministic common-type inference for reference arrays, primitive arrays, interfaces, and equal-distance candidates.
- Class-loading failure during parallel-support detection, logging behavior, sequential index traversal, and the cached fallback policy.

Cases remain in their owning operation groups. Shared test support and explicit mode restoration keep them independent of execution order.

## Why coverage remains below 100% for lines and branches

The remaining **14 missed lines and 42 missed branches** are accounted for below. The [machine-readable record](coverage-improvement.json) identifies every source location and its reason.

| Remaining paths | Missed lines | Missed branches | Evidence or practical limit |
| --- | ---: | ---: | --- |
| Flatten, instance stacking, and reshape size guards | 9 | 19 | Reaching these guards requires valid backing arrays with billions of cells or combined rows. Constructors reject row aliasing. Collection stacking overflow is covered with repeated references to legal small matrices. |
| Primitive copying after validation | 0 | 8 | Every row is validated before the later null-row ternary. Its null arm would require racing mutation of the input array. |
| Primitive row-major stream chunk boundaries | 0 | 7 | Remaining data ends at a row boundary, so each copied chunk reaches the current row end. |
| Parallel class lookup null checks | 0 | 2 | The dependency's `ClassUtil.forName` returns a class or throws; it never returns null. Both exception and linkage-error fallback are tested. |
| Unsupported matrix subtype | 1 | 1 | The sealed hierarchy permits only the concrete types already handled by the factory. |
| Different concrete classes in typed collection stacking | 0 | 1 | The generic bound requires one concrete matrix type; mixing them requires a raw/unchecked call or heap pollution. |
| Common-type ancestry invariants | 1 | 4 | Ancestry-map membership implies assignability; distinct classes cannot be mutually assignable; a superclass is discovered through its unique class child. Bytecode confirms the missed `continue` belongs to a redundant negative check. |
| Character escaping IOException handler | 2 | 0 | The append target is a `StringBuilder`, which cannot throw that checked exception. |
| Exact-int narrowing call | 1 | 0 | The call is executed and its exception asserted, but it always throws before a following JaCoCo probe can record the line. |

The last item is covered behaviorally by `testMatrixMultiplyExact_RejectsFinalIntOverflowAfterValidLongSum` in [IntMatrixTest](../../src/test/java/com/landawn/abacus/matrix/IntMatrixTest.java). No production guards were removed and no source files were excluded to raise the percentage.

## Runtime fallback detail

The isolated-loader test first denies parallel implementation classes and checks policy decisions, index traversal, and Cartesian traversal on the calling thread. Stream construction in `abacus-common` 8.2.0 itself resolves parallel implementation classes, even for sequential streams. The test therefore makes those classes available before constructing streams, then verifies that the cached failed startup probe still produces sequential streams and correct mapped values. It does not claim that stream construction works while those classes remain absent.

## Reproduce

Run the commands in [the test guide](README.md#verification). The randomized run uses seed `20261015`. Detailed counters and remaining source locations are in [coverage-improvement.json](coverage-improvement.json); fresh HTML coverage is generated at `target/site/jacoco/index.html`.
