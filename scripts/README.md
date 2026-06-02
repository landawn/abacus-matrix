# scripts/

Maintenance tooling for the **`com.landawn.abacus.matrix`** sources — chiefly the
Javadoc *"Usage Examples"* blocks (`<pre>{@code ... }</pre>`).

## `jdoc.py` — Javadoc Usage Examples toolkit

A single CLI over the `jdoc_tools/` package. This is the consolidated Python
rewrite of the ~50 Node.js scripts under `scripts/codex/` (which were written by
codex for the unrelated `com.landawn.abacus.util` project and are kept only as
reference). **Reports never write; fixers are dry-run unless `--apply`.**

```bash
python scripts/jdoc.py eligible  src/main/java                 # files that have Usage Examples
python scripts/jdoc.py report    usage-check  <file|dir>       # one read-only check (exit 1 on findings)
python scripts/jdoc.py fix       usage-spacing <file|dir>      # preview a fixer …
python scripts/jdoc.py fix       usage-spacing --apply <…>     # … then write it
python scripts/jdoc.py validate  src/main/java                 # run every check across the tree
python scripts/jdoc.py cleanup   --apply src/main/java         # run every fixer across the tree
python scripts/jdoc.py verify-comment-only <file>              # assert git diff touches only comments
```

Run `python scripts/jdoc.py -h` for the full list of report/fixer names.

### Layout
| File | Role |
|------|------|
| `jdoc.py` | CLI dispatcher (single entry point) |
| `jdoc_tools/region.py` | shared Javadoc/code-block primitives (port of `javadoc_region.js`) |
| `jdoc_tools/reports.py` | read-only checks |
| `jdoc_tools/fixes.py` | in-place fixers (dry-run by default) |
| `jdoc_tools/pipeline.py` | eligible-file discovery + `validate`/`cleanup` + git helpers |

### Other existing scripts (kept)
- `align_jdoc_examples.py` — display-width-aware column alignment of `{@code}` blocks (reused by `cleanup`/`validate`).
- `fix_javadoc_format4.py` — move the Usage Examples block before `@tags` + collapse double blanks (reused by `cleanup`).
- `verify_jdoc_test.py` + `JdocTestRunner.java` — Maven-free single-test runner (unrelated to the Javadoc tooling).
- `check_corruption.py` — scan sources for decompiler artifacts (default path retargeted to the matrix package).
- `add_missing_examples.py` — **legacy**, still hard-wired to the old `Dataset.java`; retarget before use.

### Notes on the rewrite
- `svn` was replaced with **`git`** (`verify-comment-only`, `changed-methods`).
- Heuristics hard-coded to the old project (e.g. the `N` / `CommonUtil` static
  facades, the `http`/`parser`/`poi` "owned" prefixes, and one-off literal
  replacement tables) were dropped or generalised. The `example-comments` /
  trailing-comment style checks encode that project's "// returns …" comment
  convention; tune them in `reports.py`/`fixes.py` if this project adopts a
  different style.
- Dropped as unnecessary: the codex `*_slice`/range helpers, the EOL fixups,
  and `audit_codex_scripts.js`.
