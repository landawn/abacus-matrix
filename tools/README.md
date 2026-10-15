# Matrix test maintenance

Run these commands from the repository root with Node.js 20+ and JDK 21+ on `PATH`. No npm packages are required. The tools inspect `com.landawn.abacus.matrix` in both source trees, including nested JUnit tests. `JavaTestInventory.java` uses the compiler syntax tree so comments, strings, multiline declarations, and annotation arguments do not truncate the inventory.

## Read-only audits

```powershell
node tools/find-duplicate-tests.js --check
node tools/find-duplicate-tests.js --scope=cross-file
node tools/find-duplicate-tests.js --match=IntMatrix --summary-by-file
node tools/find-nonasserting-tests.js --check
node tools/audit-public-method-test-gaps.js
node --test tools/test-tools.test.js
```

Duplicate detection compares syntax-tree bodies, parameters, and annotations. It reports whether fixtures and helpers share the same context. Cross-file matches require review; similar primitive tests may protect different behavior. `--check` exits unsuccessfully when duplicate groups remain in the selected scope.

The assertion audit follows local assertion helpers. Its self-comparison list includes legitimate singleton and reflexivity checks, so only methods without recognized assertions fail `--check`.

The public-method audit covers all twelve public types, overload signatures, and inherited declarations. It reports candidate calls based on name, arity, and class references, without type resolution. Equal-arity overloads, reflective calls, and inherited behavior require review. Compiler-generated enum methods are not source declarations. A missing direct reference is not proof of missing coverage; correlate candidates with JaCoCo and behavioral assertions. An empty input scan is an error.

## Preview and apply

These commands preview changes by default. Add `--apply` to write them, or `--check` to fail if normalization is needed:

```powershell
node tools/organize-tests.js --match=IntMatrix
node tools/standardize-test-names.js --details
node tools/format-tests.js
node tools/dedupe-matrix-tests.js --match=IntMatrix --report=target/test-deduplication.json
```

`organize-tests.js` groups canonical class tests by current operation and production declaration order. It preserves test bodies, comments, and helper bodies; rejects nested mutable fixtures, conflicting helpers, and incompatible lifecycle metadata; and qualifies explicit static method sources when relocating tests. Resolve reported name collisions with a meaningful scenario name before retrying. Preview generated grouping when names are ambiguous.

`standardize-test-names.js` updates legacy API aliases and naming case while preserving selectors that would collide. `format-tests.js` removes unused code imports, expands Jupiter assertion wildcard imports, sorts imports, and normalizes whitespace. When introducing new cases, review names first, organize groups, and run all three checks again.

`dedupe-matrix-tests.js` enables the organizer's `--dedupe` mode. It only removes equal bodies or a full body already present at the start of a stronger test, with matching fixture context and parameters. It records the retained method. Review the report before applying, then compile and compare coverage. `dedupe-util-tests.js` remains a compatibility alias for the matrix command.

`--report=<path>` writes a JSON report even in preview mode; its parent directory must exist. Other preview commands do not write source files.

## Historical ownership migration

```powershell
node tools/consolidate-test-classes.js
```

`test-ownership.json` records the reviewed routing of the eighteen former auxiliary classes. The migration command previews remaining moves, preserves required helpers within destination groups, and only writes/deletes files with `--apply`. It currently reports no remaining moves. Review ownership rules and source metadata before reusing this one-time migration for new classes; it is not a general-purpose class merger.

The test conventions, reproducible validation commands, and cleanup evidence are in [docs/testing](../docs/testing/README.md).
