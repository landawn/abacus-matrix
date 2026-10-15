# Unit tests

Tests belong to the matrix class or shared contract they exercise. The cleanup preserves the original suite's package and tag filters and leaves production sources unchanged.

## Ownership and layout

The twelve production types each have a canonical `<Type>Test.java`. Tests for inherited behavior belong in `AbstractMatrixTest`; implementation-specific behavior belongs with the concrete type. `MatricesTest` owns utility operations and parallel-policy behavior.

Five suites retain contracts intentionally checked across implementations:

| Suite | Responsibility |
| --- | --- |
| `MatrixNullContractTest` | Null values, reference arguments, and functional arguments |
| `MatrixValidationContractTest` | Invalid arguments, exceptions, and validation order |
| `MatrixStorageContractTest` | Aliasing, copying, mutation, and storage ownership |
| `MatrixTraversalContractTest` | Traversal boundaries and callback behavior |
| `MatrixShapeContractTest` | Dimensions, empty shapes, and cross-type shape operations |

Canonical files put fixtures and helpers first, then `@Nested <Operation>Tests` groups in production declaration order. Inherited operations follow the concrete type's declarations. Within each group, ordinary cases precede boundary cases and invalid-input cases. This is source organization; tests must pass in any execution order.

Use `test<Operation>_<Scenario>` names with the current API name and a concrete scenario. Avoid date, generator, review-batch, assertion-count, or numbered suffixes. Small existing names such as `testWrap` remain valid when unambiguous. Keep regression explanations next to the case they explain.

## Writing and consolidating cases

- Keep distinct inputs, overloads, primitive representations, validation order, aliasing, overflow, floating-point behavior, and empty shapes unless a retained case explicitly covers the same contract.
- Parameterize cases that share setup and assertions. Give nested parameterized tests an explicit static provider owner, for example `@MethodSource("com.landawn.abacus.matrix.ParallelModeTest#modePairs")`.
- Assert observable results, including values and shape, rather than only successful execution. For copies and mappings, check independence or source preservation when relevant. Invoke `equals` directly when testing reflexivity.
- Use shared assertion helpers for repeated contracts while retaining useful failure context. Avoid extracting one-off setup or abstraction layers that obscure the case.
- Extend `MatrixTestSupport`. It saves the caller's thread-local parallel mode, starts the case with `FORCE_OFF`, and restores the prior mode afterward. Tests of the library's initial default must use a fresh thread. Cases covering parallel execution must select the mode explicitly and restore it in `finally`.
- Use exact body comparison as a review aid. Similar names, equal coverage, or matching method-call counts do not establish duplicate behavior. Record the surviving case when removing a test.

## Verification

Use JDK 21 or newer and Maven. The maintenance tools additionally require Node.js 20 or newer; they parse Java through the JDK compiler without resolving project dependencies.

```powershell
mvn clean test jacoco:report
node --test tools/test-tools.test.js
node tools/find-duplicate-tests.js --check
node tools/find-nonasserting-tests.js --check
node tools/organize-tests.js --check
node tools/standardize-test-names.js --check
node tools/format-tests.js --check
```

Surefire now fails the build on test failures or an empty test selection. The JaCoCo agent is passed through the existing JVM arguments. Reports appear under `target/surefire-reports` and `target/site/jacoco`.

Randomize classes and methods to check isolation. PowerShell requires quoting arguments containing `$`:

```powershell
mvn clean test jacoco:report '-Djunit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$Random' '-Djunit.jupiter.testclass.order.default=org.junit.jupiter.api.ClassOrderer$Random' '-Djunit.jupiter.execution.order.random.seed=20261015'
```

See [the tool guide](../../tools/README.md) for preview/apply commands and audit limitations.

## Coverage improvement

A subsequent coverage pass reaches **100% of methods, 99.83% of lines, and 99.21% of branches**, with all 7,567 invocations passing in normal and randomized order. See the [coverage improvement record](coverage-improvement.md) for added scenarios, reproducible results, and the remaining gaps. The cleanup figures below are the historical results before that pass.

## Cleanup record

The baseline test sources are from `fec223f1f78fe58a2a8d5307060bdfb2cbd52153`. Baseline coverage was measured using the same repaired JaCoCo wiring as the final suite. Every coverage run started with `clean` so execution data could not accumulate across runs.

| Measure | Baseline | After cleanup |
| --- | ---: | ---: |
| Top-level matrix test classes | 30 | 17 |
| Test method declarations | 6,837 | 6,627 |
| Executed cases, including parameterized invocations | 6,837 | 6,658 |
| Covered lines | 7,846 / 8,282 | 7,846 / 8,282 |
| Covered branches | 4,734 / 5,320 | 4,734 / 5,320 |
| Covered methods | 1,573 / 1,648 | 1,573 / 1,648 |

The cleanup moved 120 methods out of 18 auxiliary files and removed 114 redundant methods. After two duplicate enum cases were removed, the remaining 109 enum methods became seven methods with 30 invocations. Six new parameterized methods add 14 invocations. This accounts for the change in both declaration and execution counts.

Behavioral improvements include real enum serialization, all enum comparison pairs, exact parallel and sequential matrix results, selected-region traversal, long/short reshape padding and invalid dimensions, direct equality reflexivity checks, and a fresh-thread assertion of the default parallel policy. Explicit mode selection replaces accidental coverage from leaked thread-local state.

Both the normal and randomized final runs passed 6,658 cases with zero failures, errors, or skips. All seven tooling regression tests passed. Line and branch comparison found no previously covered line or branch lost. The exact-duplicate audit reports zero groups, and the assertion audit reports zero methods without assertions. Remaining self-comparison candidates exercise repeated singleton calls or equal independently created values; the audit is intentionally advisory for those cases.

Machine-readable records:

- [Baseline inventory](baseline-inventory.json) and [final inventory](final-inventory.json)
- [Removed tests and retained coverage](consolidation-manifest.json)
- [Moved methods and final owners](ownership-manifest.json)
- [Reviewed scenario-name refinements](name-refinements.json)
- [Clean and randomized coverage comparison](coverage-comparison.json)

Historical selectors in the manifests identify migration steps. `retainedAtCompletion`, `atCompletion`, and the parameterized replacement's `finalMethods` identify the final tests. Recorded line numbers are snapshots. The name-refinement record covers reviewed collision names; routine case and API-alias normalization is not listed individually.
