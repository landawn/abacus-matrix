# Unit test consolidation plan

Baseline: commit 48f91e4; 7,001 test method declarations. Existing exact-body audits pass, but variable renaming, fixture/local setup, and assertion subsets hide additional overlap. Ten canonical files need organization/formatting normalization.

1. Capture a clean passing suite and per-source-line JaCoCo baseline.
2. Review equivalent local-variable bodies and assertion subsets; remove only cases whose inputs, API overloads, assertions, and fixture context survive. Record the retained test for every removal.
3. Parameterize repeated cases sharing setup and assertions while preserving distinct values, primitive representations, shapes, invalid arguments, and exception behavior.
4. Use operation/scenario names, production declaration ordering, and consistent imports/formatting. Strengthen incomplete value, mutation, and copy assertions in consolidated cases.
5. Run maintenance checks, tool regression tests if tooling changes, normal Maven tests, and randomized tests. Compare line-level instruction and branch coverage against the baseline.

Implementation proceeds without a review pause. Production behavior and suite discovery stay outside this cleanup's scope. Results are recorded in the consolidation report after verification.

Completed: all five steps passed. See [results and replacement records](consolidation-results.md).
