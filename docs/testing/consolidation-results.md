# Unit test consolidation results

Implemented the [consolidation plan](consolidation-plan.md) against commit `48f91e44b39e959ebae1b68f139369730901fa37`. The suite now has fewer overlapping tests, consistent operation names, and stronger result checks. Production sources, build configuration, test discovery, and coverage exclusions are unchanged.

## Changes

- Removed **260 redundant methods** after reviewing inputs, overloads, fixture context, and assertions: 130 differed only in local names or final modifiers, 125 were assertion subsets of retained cases, and five were manually merged into complete sequence/conversion checks.
- Consolidated **17 methods into three**: a parameterized dimension test with nine named cases, a parameterized hash/equality contract covering all nine matrix types, and a separate null-filled object-matrix hash test. The original dimension inputs survive, with two additional cases for zero rows with columns and rows with zero columns.
- Standardized **140 method names**, including legacy collisions that automatic normalization previously left unchanged. Canonical operation groups follow production declaration order, with ordinary cases before boundaries and invalid inputs. Imports and whitespace are consistent.
- Strengthened **31 retained tests**: seven conversion cases, six vertical-stacking cases, fourteen boolean traversal cases, and four diagonal-construction cases. These check complete values or sequences; conversion cases also check independent storage where applicable. Repeated center-cell assertions are replaced by complete expected matrices.
- Moved iterator-count and exhaustion behavior, together with its helpers, from the shape contract to the traversal contract.

The [machine-readable record](consolidation-results.json) maps all 277 removed declarations to their final retained or replacement methods, including transitive replacements and renamed selectors. It also records name changes, strengthened cases, movement, and coverage counters. Line numbers are snapshots.

## Verification

| Measure | Before | After |
| --- | ---: | ---: |
| Top-level test classes | 17 | 17 |
| Test method declarations | 7,001 | 6,727 |
| Test invocations | 7,567 | 7,309 |
| Covered methods | 1,648 / 1,648 (100%) | 1,648 / 1,648 (100%) |
| Covered lines | 8,268 / 8,282 (99.83%) | 8,268 / 8,282 (99.83%) |
| Covered branches | 5,278 / 5,320 (99.21%) | 5,278 / 5,320 (99.21%) |
| Covered instructions | 54,606 / 54,731 | 54,606 / 54,731 |

Both clean Maven runs passed **7,309 invocations with zero failures, errors, or skips**. The second run randomized classes and methods with seed `20261015`. Per-source-line instruction and branch coverage is identical to the passing baseline and identical between final runs; execution data was not accumulated.

Duplicate, assertion-presence, naming, formatting, grouping, and whitespace checks passed. The exact-body audit reports zero duplicate groups; this is a review aid, not proof that no semantic overlap remains. Eight advisory self-comparison candidates were reviewed and retained because they verify singleton identity through repeated `empty()` factory calls. Maintenance tooling was unchanged.

Method coverage remains at 100%. The existing 14 missed lines and 42 missed branches are explained in the [coverage improvement record](coverage-improvement.md#why-coverage-remains-below-100-for-lines-and-branches); this cleanup does not exclude or rewrite production paths to change those percentages.

## Reproduce

Use the normal, randomized, and maintenance commands in the [test guide](README.md#verification). Verification used JDK 25, compiler release 21, Maven 3.9.16, and JaCoCo 0.8.15. Maven ran offline against the existing local dependency cache. The latest HTML coverage report is generated at `target/site/jacoco/index.html`.
