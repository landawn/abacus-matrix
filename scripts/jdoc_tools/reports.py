"""
reports.py -- read-only Javadoc "Usage Examples" checks.

Every function here is a pure reader: it takes a file path and its split lines
and returns a list of human-readable finding strings (``path:line: ...``). None
of them ever writes. The CLI prints the findings and exits non-zero when an
"issue" report returns anything (handy for CI / pre-commit); "info" reports
(``scan``) always exit zero.

Consolidates these former ``scripts/codex/`` scripts:
  javadoc_usage_check, javadoc_placeholder_report, javadoc_example_comment_report,
  javadoc_missing_call_comment_report, javadoc_sample_member_misuse_report,
  report_jdoc_comments_outside_pre, report_jdoc_sample_comments_outside_code_blocks,
  report_jdoc_standalone_sample_comments, report_suspicious_jdoc_strings,
  scan_owned_javadoc_comments, report_changed_javadoc_methods.
"""
from __future__ import annotations

import re
import subprocess
from typing import List

from . import region


# --------------------------------------------------------------------------- #
# usage-check: structural formatting of Usage Examples blocks
# --------------------------------------------------------------------------- #
def check_usage(path: str, lines: List[str]) -> List[str]:
    out: List[str] = []

    def report(line_no: int, kind: str, detail: str = "") -> None:
        out.append(f"{path}:{line_no}: {kind}{': ' + detail if detail else ''}")

    for start, end in region.iter_javadoc_blocks(lines):
        block = lines[start:end + 1]
        usage_headers = [i for i, ln in enumerate(block) if "Usage Examples" in ln]
        if not usage_headers:
            continue

        # first @tag that sits outside a <pre> code block
        in_pre = False
        first_tag = -1
        for i, ln in enumerate(block):
            if "<pre>{@code" in ln:
                in_pre = True
                continue
            if in_pre:
                if "</pre>" in ln:
                    in_pre = False
                continue
            if re.match(r"^\s*\* @", ln):
                first_tag = i
                break

        if first_tag >= 0 and any(idx > first_tag for idx in usage_headers):
            locs = ", ".join(str(start + idx + 1) for idx in usage_headers)
            report(start + 1, "usage_after_tag", f"usage lines {locs}")

        tag_prefix_line = next((ln for ln in block if re.match(r"^\s*\* @", ln)), None)
        for idx in usage_headers:
            if idx > 0 and not region.is_blank_javadoc_line(block[idx - 1]):
                report(start + idx + 1, "missing_blank_before_usage")
            prefix = region.line_prefix(block[idx])
            if prefix is not None:
                expected = region.line_prefix(tag_prefix_line) if tag_prefix_line else prefix
                if expected is not None and prefix != expected:
                    report(start + idx + 1, "usage_indent_mismatch")

        for idx, ln in enumerate(block):
            if "</pre>" in ln and idx + 1 < len(block) and not region.is_blank_javadoc_line(block[idx + 1]):
                report(start + idx + 1, "missing_blank_after_pre")
            if re.match(r"^\s*\*\s+\*\s*<p><b>Usage Examples", ln):
                report(start + idx + 1, "stray_duplicate_star")

        for idx in range(1, len(block)):
            if region.is_blank_javadoc_line(block[idx]) and region.is_blank_javadoc_line(block[idx - 1]):
                report(start + idx + 1, "double_blank_javadoc_line")

    return out


# --------------------------------------------------------------------------- #
# placeholders: unresolved "..." / hand-wavy markers left inside examples
# --------------------------------------------------------------------------- #
def _strip_string_and_char_literals(s: str) -> str:
    s = re.sub(r'"(?:\\.|[^"\\])*"', '""', s)
    s = re.sub(r"'(?:\\.|[^'\\])*'", "''", s)
    return s


def _has_placeholder(line: str) -> bool:
    code = _strip_string_and_char_literals(line)
    return (
        bool(re.search(r"\.\.\.\s*;", code))
        or bool(re.search(r"\(\s*\.\.\.\s*\)", code))
        or bool(re.search(r"\bcollect\(\s*\.\.\.\s*\)", code))
        or bool(re.search(r"<\s*\.\.\.\s*>", code))
        or bool(re.match(r"^\s*\*\s*\.\.\.\s*$", code))
        or "typical usage" in code
        or "edge case" in code
    )


def check_placeholders(path: str, lines: List[str]) -> List[str]:
    out: List[str] = []
    in_javadoc = False
    in_pre = False
    current_method = ""
    for i, line in enumerate(lines):
        if region.is_active_javadoc_start(line):
            in_javadoc, in_pre, current_method = True, False, ""
            continue
        if in_javadoc and "<pre>{@code" in line:
            in_pre = True
            continue
        if in_javadoc and "</pre>" in line:
            in_pre = False
            continue
        if in_javadoc and "*/" in line:
            in_javadoc = in_pre = False
            j = i + 1
            while j < len(lines) and lines[j].strip() == "":
                j += 1
            while j < len(lines) and re.match(r"^(\s*@|\s*$)", lines[j]):
                j += 1
            signature = lines[j] if j < len(lines) else ""
            m = re.search(r"\b([A-Za-z_$][\w$]*)\s*\(", signature)
            current_method = m.group(1) if m else ""
            continue
        if not in_javadoc or not in_pre:
            continue
        if _has_placeholder(line):
            suffix = f":{current_method}" if current_method else ""
            out.append(f"{path}:{i + 1}{suffix}: {line.strip()}")
    return out


# --------------------------------------------------------------------------- #
# example-comments: trailing "// ..." comments that aren't in the house style
# --------------------------------------------------------------------------- #
_ACTION_VERBS = (
    "converts|adds|removes|fills|sorts|reverses|rotates|shuffles|copies|pads|"
    "prints|keeps|leaves|uses|treats|searches|starts|ends|invokes|transforms|"
    "collects|processes|sums|filters|switches|maintains|stops|logs|buffers|"
    "inserts|gets|reuses|waits|creates|closes|maps|contains"
)


def _is_acceptable_comment(comment: str) -> bool:
    text = re.sub(r"^//\s*", "", comment).strip()
    if re.match(r"^(returns|throws)\b", text):
        return not re.match(r"^returns\s+void\s*$", text, re.I)
    if re.match(r"^(true|false|null)\b", text):
        return True
    if re.match(r"^(Optional(?:Boolean|Char|Byte|Short|Int|Long|Float|Double)?|Nullable)\b", text):
        return True
    if re.match(r"""^(-?\d+(?:\.\d+)?(?:[a-zA-Z])?|\[[^\]]*\]|\{[^}]*\}|"[^"]*"|'[^']*')""", text):
        return True
    if re.match(r"^[a-z_$][\w$]*(?:\[[^\]]+\])?\s+(?:is|are|contains|has|becomes)\b", text):
        return True
    if re.match(r"^(" + _ACTION_VERBS + r")\b", text):
        return True
    if re.match(r"^[a-z_$][\w$]*(?:\[[^\]]+\])?\s+(?:is|are|contains|has|holds|becomes)\b", text):
        return True
    if re.match(r"^(no change|no exception thrown|same instance|empty result|unchanged|case-sensitive|case-insensitive)\b", text):
        return True
    return False


def check_example_comments(path: str, lines: List[str]) -> List[str]:
    out: List[str] = []
    for i, line in region.iter_code_lines(lines):
        m = re.match(r"^\s*\*\s+(.*)$", line)
        if not m:
            continue
        code = m.group(1)
        idx = region.comment_index_outside_literals(code)
        if idx < 0:
            continue
        before = code[:idx].strip()
        comment = code[idx:].strip()
        if not before or before.startswith("//"):
            continue
        if _is_acceptable_comment(comment):
            continue
        out.append(f"{path}:{i + 1}: * {code}")
    return out


# --------------------------------------------------------------------------- #
# missing-call-comments: bare static-utility calls lacking a "// returns ..."
# --------------------------------------------------------------------------- #
_NON_CALL_DECL = re.compile(
    r"^\s*(?:final\s+)?(?:var|void|int|long|boolean|double|float|char|byte|short|"
    r"String|Object|Class|List|Set|Map|Collection|Iterable|Iterator|Stream|"
    r"[A-Z][\w$<>?, ]+)\s+\w+\b"
)


def _is_likely_call(code: str, facade: re.Pattern) -> bool:
    if "(" not in code or ")" not in code:
        return False
    if re.match(r"^\s*(if|for|while|switch|try|catch|return|throw|new\s|class\s|interface\s|enum\s)\b", code):
        return False
    if re.match(r"^\s*[@{}]", code):
        return False
    if "=" in code:
        return False
    if _NON_CALL_DECL.match(code):
        return False
    return bool(facade.match(code))


def _has_follow_up_comment(lines: List[str], index: int) -> bool:
    nxt = lines[index + 1].strip() if index + 1 < len(lines) else ""
    return bool(re.match(
        r"^\*\s*//\s*(?:returns|throws|[a-z_$][\w$]*(?:\[[^\]]+\])?\s+"
        r"(?:is|are|contains|has|becomes)|no exception thrown|no change|"
        r"same instance|empty result|unchanged)\b",
        nxt,
    ))


def _next_declaration(lines: List[str], end: int) -> str:
    for i in range(end + 1, len(lines)):
        s = lines[i].strip()
        if s:
            return s
    return ""


def _is_callable_declaration(line: str) -> bool:
    return (
        bool(re.match(r"^(?:@\w+(?:\([^)]*\))?\s*)*(?:public|protected)\s+", line))
        and "(" in line
        and not re.search(r"\b(class|interface|enum|record)\b", line)
    )


def check_missing_call_comments(path: str, lines: List[str]) -> List[str]:
    # Retargeted: the original hard-coded the ``util`` project's ``N`` / ``CommonUtil``
    # facades. Generalised to any capitalised static facade call, e.g.
    # ``Matrices.zip(...)`` -- a top-level ``Type.method(args);`` statement.
    facade = re.compile(r"^\s*(?:[A-Z][\w$]*)\.[A-Za-z_$][\w$]*\s*\([^;]*\)\s*;?$")
    out: List[str] = []
    for start, end in region.iter_javadoc_blocks(lines):
        if not _is_callable_declaration(_next_declaration(lines, end)):
            continue
        in_pre = False
        for index in range(start, end + 1):
            line = lines[index]
            if "<pre>{@code" in line:
                in_pre = True
                continue
            if not in_pre:
                continue
            if "</pre>" in line:
                in_pre = False
                continue
            m = re.match(r"^\s*\*\s?(.*)$", line)
            if not m:
                continue
            code = m.group(1).strip()
            if not code or code.startswith("//") or code.startswith("*"):
                continue
            if "//" in code:
                continue
            if _has_follow_up_comment(lines, index):
                continue
            if _is_likely_call(code, facade):
                out.append(f"{path}:{index + 1}: {line}")
    return out


# --------------------------------------------------------------------------- #
# sample-member-misuse: array.size() / collection.length confusion
# --------------------------------------------------------------------------- #
_PRIMITIVES = "boolean|byte|char|short|int|long|float|double"
_ARRAY_DECL = re.compile(
    r"\b(?:final\s+)?(?:" + _PRIMITIVES + r"|[A-Z_$][\w$]*(?:\s*<[^;=]+>)?)\s*\[\]\s+([A-Za-z_$][\w$]*)\b"
)
_COLLECTION_DECL = re.compile(
    r"\b(?:final\s+)?(?:List|ArrayList|LinkedList|Collection|Set|HashSet|LinkedHashSet|"
    r"SortedSet|NavigableSet|Map|HashMap|LinkedHashMap|SortedMap|NavigableMap|Deque|"
    r"ArrayDeque|Queue|PrimitiveList|BooleanList|CharList|ByteList|ShortList|IntList|"
    r"LongList|FloatList|DoubleList)\b(?:\s*<[^;=]+>)?\s+([A-Za-z_$][\w$]*)\b"
)


def _code_of(line: str) -> str:
    return re.sub(r"^\s*\*\s?", "", line)


def _iter_full_code_blocks(lines: List[str]):
    """Yield ``(block_start_index, [block lines])`` for each ``<pre>{@code``
    block (marker lines included), like the JS member-misuse scanners."""
    mask = region.active_javadoc_line_mask(lines)
    in_block = False
    block_start = 0
    block_lines: List[str] = []
    for i, line in enumerate(lines):
        if not mask[i]:
            if in_block:
                yield block_start, block_lines
                in_block, block_lines = False, []
            continue
        if not in_block and "<pre>{@code" in line:
            in_block, block_start, block_lines = True, i, [line]
            if "}</pre>" in line:
                yield block_start, block_lines
                in_block, block_lines = False, []
            continue
        if in_block:
            block_lines.append(line)
            if "}</pre>" in line:
                yield block_start, block_lines
                in_block, block_lines = False, []


def check_sample_member_misuse(path: str, lines: List[str]) -> List[str]:
    out: List[str] = []
    for block_start, block in _iter_full_code_blocks(lines):
        array_vars = set()
        collection_vars = set()
        for ln in block:
            code = _code_of(ln)
            array_vars.update(_ARRAY_DECL.findall(code))
            collection_vars.update(_COLLECTION_DECL.findall(code))
        for offset, ln in enumerate(block):
            code = _code_of(ln)
            for var in array_vars:
                if re.search(r"\b" + re.escape(var) + r"\.size\(\)", code):
                    out.append(f'{path}:{block_start + offset + 1}: array variable "{var}" uses .size(): {code.strip()}')
            for var in collection_vars:
                if re.search(r"\b" + re.escape(var) + r"\.length\b", code):
                    out.append(f'{path}:{block_start + offset + 1}: collection variable "{var}" uses .length: {code.strip()}')
    return out


# --------------------------------------------------------------------------- #
# comments-outside-pre / sample-comments-outside-code / standalone-sample
# --------------------------------------------------------------------------- #
def check_comments_outside_pre(path: str, lines: List[str]) -> List[str]:
    out: List[str] = []
    mask = region.active_javadoc_line_mask(lines)
    for i, line in enumerate(lines):
        if not mask[i] or "Usage Examples:</b></p>" not in line:
            continue
        j = i + 1
        while j < min(len(lines), i + 8) and mask[j]:
            if "<pre>{@code" in lines[j]:
                break
            if re.match(r"^\s*\*\s+//\s+", lines[j]):
                out.append(f"{path}:{j + 1}: {lines[j].strip()}")
            j += 1
    return out


def check_sample_comments_outside_code(path: str, lines: List[str]) -> List[str]:
    out: List[str] = []
    mask = region.active_javadoc_line_mask(lines)
    in_pre = False
    for i, line in enumerate(lines):
        if not mask[i]:
            in_pre = False
            continue
        if "<pre>{@code" in line:
            in_pre = True
            continue
        if in_pre and "}</pre>" in line:
            in_pre = False
            continue
        if not in_pre and re.match(r"^\s*\*\s+//\s+\S", line):
            out.append(f"{path}:{i + 1}: {line.strip()}")
    return out


_RESULT_COMMENT_PREFIXES = (
    "return", "throw", "print", "result", "represent", "useful", "using", "set ",
    "can be used", "default ", "high precision", "parse with", "convert to",
    "note:", "a java", "the default", "formatted with", "numeric =",
    "numberformatexception", "nullpointerexception", "illegalargumentexception",
    "arithmeticexception",
)


def check_standalone_sample_comments(path: str, lines: List[str], all_kinds: bool = False) -> List[str]:
    out: List[str] = []
    mask = region.active_javadoc_line_mask(lines)
    in_pre = False
    for i, line in enumerate(lines):
        if not mask[i]:
            in_pre = False
            continue
        if "<pre>{@code" in line:
            in_pre = True
            continue
        if in_pre and "}</pre>" in line:
            in_pre = False
            continue
        if not in_pre:
            continue
        m = re.match(r"^\s*\*\s+//\s+(.+)$", line)
        if not m:
            continue
        text = m.group(1).strip()
        lower = text.lower()
        kind = "NON_HEADING" if any(lower.startswith(p) for p in _RESULT_COMMENT_PREFIXES) else "HEADING"
        if all_kinds or kind != "HEADING":
            out.append(f"{kind}\t{path}:{i + 1}\t{text}")
    return out


# --------------------------------------------------------------------------- #
# suspicious-strings: a "// returns/throws" buried inside a string literal
# --------------------------------------------------------------------------- #
def _has_suspicious_string(line: str) -> bool:
    in_string = False
    escaped = False
    current = ""
    for ch in line:
        if not in_string:
            if ch == '"':
                in_string, escaped, current = True, False, '"'
            continue
        current += ch
        if escaped:
            escaped = False
            continue
        if ch == "\\":
            escaped = True
            continue
        if ch == '"':
            if re.search(r"//\s*(returns|throws)\b", current, re.I):
                return True
            in_string, current = False, ""
    return False


def check_suspicious_strings(path: str, lines: List[str]) -> List[str]:
    out: List[str] = []
    for i, line in region.iter_code_lines(lines):
        idx = region.comment_index_outside_literals(line)
        code = line if idx < 0 else line[:idx]
        if _has_suspicious_string(code):
            out.append(f"{path}:{i + 1}:{line}")
    return out


# --------------------------------------------------------------------------- #
# scan: generic needle search inside active Javadoc (info report, exit 0)
# --------------------------------------------------------------------------- #
_SCAN_ALIASES = {
    "returns_this": "// returns this",
    "returns_response": "// returns response",
    "returns_void": "// returns void",
}


def check_scan(path: str, lines: List[str], needle: str = "returns_this") -> List[str]:
    needle = _SCAN_ALIASES.get(needle, needle)
    out: List[str] = []
    mask = region.active_javadoc_line_mask(lines)
    for i, line in enumerate(lines):
        if mask[i] and needle in line:
            out.append(f"{path.replace(chr(92), '/')}:{i + 1}:{line.strip()}")
    return out


# --------------------------------------------------------------------------- #
# changed-methods: which Javadoc'd methods changed in the working tree (git)
# --------------------------------------------------------------------------- #
def _git_changed_new_lines(path: str) -> set:
    """New-file line numbers touched by ``git diff`` for ``path`` (was ``svn``)."""
    try:
        diff = subprocess.run(
            ["git", "diff", "--", path],
            capture_output=True, text=True, check=False,
        ).stdout
    except FileNotFoundError:
        return set()
    changed = set()
    new_line = 0
    for line in region.split_lines(diff):
        hunk = re.match(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@", line)
        if hunk:
            new_line = int(hunk.group(1))
            continue
        if not new_line:
            continue
        if line.startswith("+") and not line.startswith("+++"):
            changed.add(new_line)
            new_line += 1
        elif line.startswith("-") and not line.startswith("---"):
            continue
        else:
            new_line += 1
    return changed


def check_changed_methods(path: str, lines: List[str]) -> List[str]:
    changed_lines = _git_changed_new_lines(path)
    if not changed_lines:
        return []

    def signature_after(end: int) -> str:
        sig = ""
        for i in range(end + 1, min(len(lines), end + 30)):
            t = lines[i].strip()
            if not t or t.startswith("@"):
                continue
            sig += " " + t
            if "{" in t or ";" in t:
                break
        return re.sub(r"\s+", " ", sig).strip()

    methods: dict[str, list[str]] = {}
    for start, end in region.iter_javadoc_blocks(lines):
        if any((start + 1) <= ln <= (end + 1) for ln in changed_lines):
            sig = signature_after(end)
            m = re.search(r"\b([A-Za-z_$][\w$]*)\s*\(", sig)
            name = m.group(1) if m else "(class javadoc)"
            methods.setdefault(name, []).append(sig)

    out: List[str] = []
    for name, sigs in methods.items():
        out.append(f"{name}: {len(sigs)}")
        for sig in sigs[:4]:
            out.append(f"  {sig}")
        if len(sigs) > 4:
            out.append(f"  ... {len(sigs) - 4} more overload(s)")
    return out


# --------------------------------------------------------------------------- #
# registry -- name -> (function, is_issue_report)
# --------------------------------------------------------------------------- #
# is_issue_report=True  -> non-empty output means "problems found" (exit 1)
# is_issue_report=False -> informational (exit 0 regardless)
REPORTS = {
    "usage-check": (check_usage, True),
    "placeholders": (check_placeholders, True),
    "example-comments": (check_example_comments, True),
    "missing-call-comments": (check_missing_call_comments, True),
    "sample-member-misuse": (check_sample_member_misuse, True),
    "comments-outside-pre": (check_comments_outside_pre, False),
    "sample-comments-outside-code": (check_sample_comments_outside_code, False),
    "standalone-sample-comments": (check_standalone_sample_comments, False),
    "suspicious-strings": (check_suspicious_strings, True),
    "scan": (check_scan, False),
    "changed-methods": (check_changed_methods, False),
}
