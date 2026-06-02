"""
pipeline.py -- eligible-file discovery and whole-project orchestration.

Replaces the codex orchestrators (``eligible_jdoc_usage_files``,
``inventory_jdoc_*``, ``run_eligible_jdoc_cleanup``, ``validate_eligible_jdoc_usage``,
``validate_jdoc_usage_batch``, ``validate_owned_jdoc_usage``, ``verify_comment_only_diff``)
with three functions: :func:`eligible_files`, :func:`validate`, :func:`cleanup`,
plus a git-based :func:`verify_comment_only`.

It reuses the two existing standalone scripts rather than re-porting them:
``align_jdoc_examples`` (column alignment) and ``fix_javadoc_format4`` (move the
Usage Examples block before the ``@tags`` + collapse double blanks).
"""
from __future__ import annotations

import importlib
import os
import re
import subprocess
import sys
from typing import List, Optional, Tuple

from . import region, reports, fixes

_USAGE_PAT = re.compile(
    r"(?:Usage Examples|<b>Examples?:|Example usage|<b>Example\b|Typical usage pattern)", re.I
)
_PUBLIC_TYPE = re.compile(
    r"^\s*public\s+(?:abstract\s+|final\s+|sealed\s+|non-sealed\s+|strictfp\s+)*"
    r"\b(?:class|interface|enum|record)\b", re.M,
)
_PUBLIC_ANNOTATION = re.compile(r"^\s*public\s+@interface\b", re.M)
_PRE_COUNT = re.compile(r"<pre>\{@code")
_PACKAGE = re.compile(r"^\s*package\s+([A-Za-z0-9_.]+)\s*;", re.M)


# --------------------------------------------------------------------------- #
# small IO + lazy reuse of the sibling standalone scripts
# --------------------------------------------------------------------------- #
def read_text(path: str) -> str:
    with open(path, "r", encoding="utf-8", errors="ignore") as fh:
        return fh.read()


def _sibling(module_name: str):
    """Import a standalone script from scripts/ (its dir is on sys.path)."""
    scripts_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    if scripts_dir not in sys.path:
        sys.path.insert(0, scripts_dir)
    return importlib.import_module(module_name)


def find_java_files(paths: List[str]) -> List[str]:
    """Expand files/dirs to a sorted list of ``.java`` files."""
    out: List[str] = []
    for p in paths:
        if os.path.isdir(p):
            for root, _dirs, names in os.walk(p):
                if ".git" in root or os.sep + "target" in root:
                    continue
                for name in sorted(names):
                    if name.endswith(".java"):
                        out.append(os.path.join(root, name))
        elif os.path.isfile(p):
            out.append(p)
    return out


# --------------------------------------------------------------------------- #
# eligible-file discovery (port of collectEligibleFiles)
# --------------------------------------------------------------------------- #
def _strip_comments(text: str) -> str:
    out: List[str] = []
    in_block = in_line = in_string = in_char = escaped = False
    i, n = 0, len(text)
    while i < n:
        ch = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if in_line:
            if ch in "\n\r":
                in_line = False
                out.append(ch)
            else:
                out.append(" ")
            i += 1
            continue
        if in_block:
            if ch == "*" and nxt == "/":
                out.append("  ")
                i += 2
                in_block = False
                continue
            out.append(ch if ch in "\n\r" else " ")
            i += 1
            continue
        if escaped:
            escaped = False
            out.append(" ")
            i += 1
            continue
        if (in_string or in_char) and ch == "\\":
            escaped = True
            out.append(" ")
            i += 1
            continue
        if not in_string and not in_char and ch == "/" and nxt == "/":
            out.append("  ")
            i += 2
            in_line = True
            continue
        if not in_string and not in_char and ch == "/" and nxt == "*":
            out.append("  ")
            i += 2
            in_block = True
            continue
        if not in_char and ch == '"':
            in_string = not in_string
            out.append(" ")
            i += 1
            continue
        if not in_string and ch == "'":
            in_char = not in_char
            out.append(" ")
            i += 1
            continue
        out.append(" " if (in_string or in_char) else ch)
        i += 1
    return "".join(out)


def package_name(text: str) -> str:
    m = _PACKAGE.search(text)
    return m.group(1) if m else ""


def eligible_files(root: str, exclude_packages: Optional[List[str]] = None) -> List[Tuple[str, int, str]]:
    """Return ``[(file, code_block_count, package), ...]`` for public types whose
    Javadoc contains Usage Examples, sorted by path."""
    exclude = set(exclude_packages or [])
    result: List[Tuple[str, int, str]] = []
    for file in find_java_files([root]):
        text = read_text(file)
        pkg = package_name(text)
        if pkg in exclude:
            continue
        code = _strip_comments(text)
        if not _PUBLIC_TYPE.search(code):
            continue
        if _PUBLIC_ANNOTATION.search(code):
            continue
        if not region.active_javadocs_contain(text, _USAGE_PAT):
            continue
        result.append((file.replace("\\", "/"), region.count_in_active_javadocs(text, _PRE_COUNT), pkg))
    result.sort(key=lambda item: item[0])
    return result


# --------------------------------------------------------------------------- #
# verify-comment-only (git port of verify_comment_only_diff)
# --------------------------------------------------------------------------- #
def verify_comment_only(path: str) -> List[str]:
    """Return offending non-comment changed lines for ``path`` per ``git diff``."""
    try:
        diff = subprocess.run(
            ["git", "diff", "--", path], capture_output=True, text=True, check=False
        ).stdout
    except FileNotFoundError:
        return []
    violations: List[str] = []
    for line in region.split_lines(diff):
        if not line or not re.match(r"^[+-]", line):
            continue
        if re.match(r"^(\+\+\+|---)", line):
            continue
        content = line[1:].strip()
        if (content == "" or content.startswith("*") or content.startswith("//")
                or content.startswith("/*") or content.startswith("*/")):
            continue
        violations.append(line)
    return violations


# --------------------------------------------------------------------------- #
# validate: run every issue report (+ align check + comment-only) over a tree
# --------------------------------------------------------------------------- #
_VALIDATE_CHECKS = [
    "usage-check", "placeholders", "example-comments",
    "missing-call-comments", "sample-member-misuse",
]


def validate(root: str, exclude_packages: Optional[List[str]] = None) -> int:
    files = [item[0] for item in eligible_files(root, exclude_packages)]
    align = _sibling("align_jdoc_examples")
    failures = []

    for file in files:
        lines = region.split_lines(read_text(file))
        problem = None
        for name in _VALIDATE_CHECKS:
            fn, _ = reports.REPORTS[name]
            issues = fn(file, lines)
            if issues:
                problem = (name, issues)
                break
        if problem is None and align.realign(_read_keepends(file))[1]:
            problem = ("align (--check)", [f"{file}: would re-align"])
        if problem is None:
            v = verify_comment_only(file)
            if v:
                problem = ("verify-comment-only", v)
        if problem:
            failures.append((file, problem[0], problem[1]))

    for file, check, output in failures:
        print(f"FAIL {check} {file}")
        for line in output[:20]:
            print(f"  {line}")
        if len(output) > 20:
            print("  ...")
    print(f"VALIDATED_FILE_COUNT {len(files)}")
    print(f"FAILED_FILE_COUNT {len(failures)}")
    return 1 if failures else 0


def _read_keepends(path: str) -> List[str]:
    with open(path, "r", encoding="utf-8", newline="") as fh:
        return fh.readlines()


# --------------------------------------------------------------------------- #
# cleanup: run the fixers in dependency order over a tree, then align
# --------------------------------------------------------------------------- #
# Order mirrors run_eligible_jdoc_cleanup.js (structure first, then content,
# then column alignment last so it sees final text).
_CLEANUP_FIXES = [
    "usage-indent",
    "usage-spacing",
    "double-blank",
    "type-placeholders",
    "literal-artifacts",
    "returns-void",
    "sample-member-misuse",
    "literal-action-comments",
]


def cleanup(root: str, apply: bool, exclude_packages: Optional[List[str]] = None) -> int:
    files = [item[0] for item in eligible_files(root, exclude_packages)]
    print(f"ELIGIBLE_USAGE_FILE_COUNT {len(files)}")
    if not files:
        return 0

    format4 = _sibling("fix_javadoc_format4")
    align = _sibling("align_jdoc_examples")
    total_changes = 0

    for file in files:
        # 1) move Usage Examples before @tags + collapse double blanks (existing tool)
        if apply:
            format4.fix_file(file)
        # 2) the consolidated fixers, in order
        for name in _CLEANUP_FIXES:
            lines = region.split_lines(read_text(file))
            newline = region.detect_newline(read_text(file))
            new_lines, count, _details = fixes.FIXES[name](file, lines)
            total_changes += count
            if apply and new_lines != lines:
                with open(file, "w", encoding="utf-8") as fh:
                    fh.write(region.join_lines(new_lines, newline))
        # 3) align columns last (existing tool)
        align.process_file(file, apply, quiet=True)

    verb = "applied" if apply else "would apply"
    print(f"CLEANUP_DONE files={len(files)} fixer_changes={total_changes} ({verb})")
    if not apply:
        print("(dry run -- re-run with --apply to write)")
    return 0
