#!/usr/bin/env python3
"""
jdoc.py -- one CLI for the Javadoc "Usage Examples" toolkit (jdoc_tools).

This is the consolidated Python replacement for the ~50 Node.js scripts that
used to live under ``scripts/codex/`` (written by codex for the unrelated
``com.landawn.abacus.util`` project). Everything is retargeted at this project
(``com.landawn.abacus.matrix``) and reachable through subcommands.

Quick start
-----------
    # which source files have Usage Examples blocks?
    python scripts/jdoc.py eligible src/main/java

    # report problems (read-only; exits 1 if any are found)
    python scripts/jdoc.py report usage-check src/main/java/com/landawn/abacus/matrix/IntMatrix.java

    # preview a fix, then apply it
    python scripts/jdoc.py fix usage-spacing src/main/java/com/landawn/abacus/matrix
    python scripts/jdoc.py fix usage-spacing --apply src/main/java/com/landawn/abacus/matrix

    # run every check / every fixer across the eligible tree
    python scripts/jdoc.py validate src/main/java
    python scripts/jdoc.py cleanup --apply src/main/java

Reports never write. Fixers are dry-run unless you pass --apply.
"""
from __future__ import annotations

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from jdoc_tools import region, reports, fixes, pipeline  # noqa: E402

try:  # make CJK printable on Windows consoles
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass


def _read(path: str):
    text = pipeline.read_text(path)
    return text, region.detect_newline(text), region.split_lines(text)


def _cmd_report(args) -> int:
    if args.name not in reports.REPORTS:
        print(f"unknown report: {args.name}\navailable: {', '.join(reports.REPORTS)}", file=sys.stderr)
        return 2
    fn, is_issue = reports.REPORTS[args.name]
    findings = 0
    for path in pipeline.find_java_files(args.paths):
        _text, _nl, lines = _read(path)
        if args.name == "scan":
            out = fn(path, lines, needle=args.needle)
        elif args.name == "standalone-sample-comments":
            out = fn(path, lines, all_kinds=args.all)
        else:
            out = fn(path, lines)
        for line in out:
            print(line)
        findings += len(out)
    print(f"{args.name.upper().replace('-', '_')}_COUNT {findings}")
    return 1 if (is_issue and findings) else 0


def _cmd_fix(args) -> int:
    name = args.name
    # the two fixers delegated to existing standalone scripts
    if name == "align":
        align = pipeline._sibling("align_jdoc_examples")
        return align.main((["--apply"] if args.apply else []) + args.paths)
    if name == "move-usage-before-tags":
        if not args.apply:
            print("move-usage-before-tags has no dry-run; re-run with --apply", file=sys.stderr)
            return 2
        format4 = pipeline._sibling("fix_javadoc_format4")
        for path in pipeline.find_java_files(args.paths):
            changed, n = format4.fix_file(path)
            if changed:
                print(f"{path}: moved/collapsed {n} block(s)")
        return 0

    if name not in fixes.FIXES:
        avail = ", ".join(list(fixes.FIXES) + ["align", "move-usage-before-tags"])
        print(f"unknown fixer: {name}\navailable: {avail}", file=sys.stderr)
        return 2

    fixer = fixes.FIXES[name]
    total = 0
    files_changed = 0
    for path in pipeline.find_java_files(args.paths):
        _text, newline, lines = _read(path)
        new_lines, count, details = fixer(path, lines)
        for line in details:
            print(f"{'' if args.apply else '[dry-run] '}{line}")
        if new_lines != lines:
            files_changed += 1
            total += max(count, 1)
            if args.apply:
                with open(path, "w", encoding="utf-8") as fh:
                    fh.write(region.join_lines(new_lines, newline))
    verb = "changed" if args.apply else "would change"
    print(f"{name.upper().replace('-', '_')}: {verb} {files_changed} file(s), {total} edit(s)")
    if not args.apply and files_changed:
        return 1
    return 0


def _cmd_eligible(args) -> int:
    for file, count, pkg in pipeline.eligible_files(args.root, args.exclude_package):
        print(f"{file}\t{count}\t{pkg}")
    return 0


def _cmd_validate(args) -> int:
    return pipeline.validate(args.root, args.exclude_package)


def _cmd_cleanup(args) -> int:
    return pipeline.cleanup(args.root, args.apply, args.exclude_package)


def _cmd_verify_comment_only(args) -> int:
    failed = 0
    files = pipeline.find_java_files(args.paths)
    for path in files:
        violations = pipeline.verify_comment_only(path)
        if violations:
            failed += 1
            print(f"Non-comment changed lines in {path}:")
            for line in violations[:50]:
                print(line)
            if len(violations) > 50:
                print(f"... {len(violations) - 50} more")
    print(f"COMMENT_ONLY_VERIFIED_FILE_COUNT {len(files)}")
    return 1 if failed else 0


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="jdoc.py",
        description="Javadoc Usage Examples reporting / normalization toolkit.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="reports: " + ", ".join(reports.REPORTS) + "\nfixers:  "
        + ", ".join(list(fixes.FIXES) + ["align", "move-usage-before-tags"]),
    )
    sub = p.add_subparsers(dest="command", required=True)

    sp = sub.add_parser("report", help="run a read-only check (exits 1 on findings)")
    sp.add_argument("name", help="check name (see epilog of -h)")
    sp.add_argument("paths", nargs="+", help=".java file(s) or directory(ies)")
    sp.add_argument("--needle", default="returns_this", help="for 'scan': text/alias to search")
    sp.add_argument("--all", action="store_true", help="for 'standalone-sample-comments': include HEADING lines")
    sp.set_defaults(func=_cmd_report)

    sp = sub.add_parser("fix", help="run an in-place fixer (dry-run unless --apply)")
    sp.add_argument("name", help="fixer name (see epilog of -h)")
    sp.add_argument("paths", nargs="+", help=".java file(s) or directory(ies)")
    sp.add_argument("--apply", action="store_true", help="write changes (default: preview)")
    sp.set_defaults(func=_cmd_fix)

    sp = sub.add_parser("eligible", help="list files with Usage Examples (file<TAB>count<TAB>package)")
    sp.add_argument("root", nargs="?", default="src/main/java")
    sp.add_argument("--exclude-package", action="append", default=[], help="package to skip (repeatable)")
    sp.set_defaults(func=_cmd_eligible)

    sp = sub.add_parser("inventory", help="alias of 'eligible'")
    sp.add_argument("root", nargs="?", default="src/main/java")
    sp.add_argument("--exclude-package", action="append", default=[])
    sp.set_defaults(func=_cmd_eligible)

    sp = sub.add_parser("validate", help="run all checks across the eligible tree")
    sp.add_argument("root", nargs="?", default="src/main/java")
    sp.add_argument("--exclude-package", action="append", default=[])
    sp.set_defaults(func=_cmd_validate)

    sp = sub.add_parser("cleanup", help="run all fixers across the eligible tree")
    sp.add_argument("root", nargs="?", default="src/main/java")
    sp.add_argument("--apply", action="store_true", help="write changes (default: preview)")
    sp.add_argument("--exclude-package", action="append", default=[])
    sp.set_defaults(func=_cmd_cleanup)

    sp = sub.add_parser("verify-comment-only", help="assert a file's git diff touches only comments")
    sp.add_argument("paths", nargs="+", help=".java file(s) or directory(ies)")
    sp.set_defaults(func=_cmd_verify_comment_only)

    return p


def main(argv=None) -> int:
    args = build_parser().parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main())
