"""
fixes.py -- in-place Javadoc example fixers.

Every fixer is a *pure* function ``fix_X(path, lines) -> (new_lines, count, details)``
that never touches the filesystem. The CLI computes the result, prints the
``details``, and only writes the file when ``--apply`` is given and the content
actually changed. This makes ``--check`` (dry-run) the safe default for all of
them, matching the original scripts' ``--apply`` guard.

Consolidates these former ``scripts/codex/`` scripts:
  fix_jdoc_usage_indent, fix_jdoc_usage_spacing, fix_jdoc_double_blank_lines,
  fix_javadoc_returns_void_comments, fix_javadoc_sample_member_misuse,
  fix_jdoc_literal_comment_artifacts, fix_assignment_style_public_jdoc_examples,
  replace_jdoc_type_placeholders, normalize_jdoc_literal_action_comments,
  normalize_jdoc_trailing_comment_style, normalize_jdoc_example_comments,
  move_jdoc_sample_comments_into_pre.

(``move_jdoc_usage_before_tags`` and ``align_jdoc_examples`` are intentionally
not re-implemented here -- they are already covered by the existing
``scripts/fix_javadoc_format4.py`` and ``scripts/align_jdoc_examples.py``.)
"""
from __future__ import annotations

import re
from typing import List, Tuple

from . import region
from .reports import _ARRAY_DECL, _code_of, _iter_full_code_blocks

FixResult = Tuple[List[str], int, List[str]]


def _is_standalone_sample_comment(line: str, comment_index: int) -> bool:
    return bool(re.match(r"^\s*\*\s*$", line[:comment_index]))


# --------------------------------------------------------------------------- #
# usage-indent: re-indent a Usage Examples block to the surrounding gutter
# --------------------------------------------------------------------------- #
_TAG_RE = re.compile(r"^(\s*)\*\s*@(param|return|throws|see|since|deprecated)\b")
_STAR_RE = re.compile(r"^(\s*)\*(.*)$")
_USAGE_RE = re.compile(r"<p><b>.*(?:Usage Examples|Example).*</b></p>")


def fix_usage_indent(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    changed = 0
    for start, end in region.iter_javadoc_blocks(lines):
        expected = None
        for i in range(start, end + 1):
            m = _TAG_RE.match(lines[i])
            if m:
                expected = m.group(1)
                break
        if expected is None:
            for i in range(start + 1, end + 1):
                m = _STAR_RE.match(lines[i])
                if m and m.group(2).strip():
                    expected = m.group(1)
                    break
        if expected is None:
            continue

        for i in range(start + 1, end + 1):
            if not _USAGE_RE.search(lines[i]):
                continue
            block_start = i
            if block_start > start and re.match(r"^\s*\*\s*$", lines[block_start - 1]):
                block_start -= 1
            block_end = i
            while block_end <= end and "</pre>" not in lines[block_end]:
                block_end += 1
            if block_end <= end and block_end + 1 <= end and (
                re.match(r"^\s*\*\s*$", lines[block_end + 1]) or re.match(r"^\s*$", lines[block_end + 1])
            ):
                block_end += 1
            for j in range(block_start, block_end + 1):
                if re.match(r"^\s*$", lines[j]):
                    if lines[j] != expected + "*":
                        lines[j] = expected + "*"
                        changed += 1
                    continue
                m = _STAR_RE.match(lines[j])
                if m and m.group(1) != expected:
                    lines[j] = expected + "*" + m.group(2)
                    changed += 1
    return lines, changed, []


# --------------------------------------------------------------------------- #
# usage-spacing: exactly one blank gutter line before the header and after </pre>
# --------------------------------------------------------------------------- #
def _is_blank_javadoc(line: str) -> bool:
    return bool(re.match(r"^\s*\*\s*$", line))


def _blank_for(line: str) -> str:
    m = re.match(r"^(\s*)\*", line)
    return f"{m.group(1) if m else '     '}*"


def fix_usage_spacing(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    changed = 0

    # pass 1: collapse consecutive blank gutter lines inside Javadoc
    in_javadoc = False
    i = 0
    while i < len(lines):
        if not in_javadoc and region.is_active_javadoc_start(lines[i]):
            in_javadoc = True
            i += 1
            continue
        if in_javadoc and "*/" in lines[i]:
            in_javadoc = False
            i += 1
            continue
        if in_javadoc and i > 0 and _is_blank_javadoc(lines[i]) and _is_blank_javadoc(lines[i - 1]):
            del lines[i]
            changed += 1
            i -= 1
            continue
        i += 1

    # pass 2: enforce a single blank line around each usage block
    i = 0
    while i < len(lines):
        mask = region.active_javadoc_line_mask(lines)
        if not mask[i] or not re.search(r"<p><b>.*(?:Usage Examples|Examples?|Example usage).*</b></p>", lines[i]):
            i += 1
            continue
        blank = _blank_for(lines[i])
        while i > 0 and _is_blank_javadoc(lines[i - 1]) and i > 1 and _is_blank_javadoc(lines[i - 2]):
            del lines[i - 1]
            i -= 1
            changed += 1
        if i > 0 and not _is_blank_javadoc(lines[i - 1]):
            lines.insert(i, blank)
            i += 1
            changed += 1
        close = i + 1
        while close < len(lines) and "</pre>" not in lines[close]:
            close += 1
        if close >= len(lines):
            i += 1
            continue
        while close + 2 < len(lines) and _is_blank_javadoc(lines[close + 1]) and _is_blank_javadoc(lines[close + 2]):
            del lines[close + 2]
            changed += 1
        if close + 1 < len(lines) and not _is_blank_javadoc(lines[close + 1]):
            lines.insert(close + 1, blank)
            changed += 1
        i += 1
    return lines, changed, []


# --------------------------------------------------------------------------- #
# double-blank: drop consecutive blank gutter lines anywhere in a Javadoc block
# --------------------------------------------------------------------------- #
def fix_double_blank(path: str, lines: List[str]) -> FixResult:
    out: List[str] = []
    in_javadoc = False
    prev_blank = False
    count = 0
    for line in lines:
        if region.is_active_javadoc_start(line):
            in_javadoc = True
            prev_blank = False
        blank = in_javadoc and bool(re.match(r"^\s*\*\s*$", line))
        if blank and prev_blank:
            count += 1
            continue
        out.append(line)
        prev_blank = blank
        if in_javadoc and re.search(r"\*/\s*$", line):
            in_javadoc = False
            prev_blank = False
    return out, count, []


# --------------------------------------------------------------------------- #
# returns-void: "// returns void" -> "// no exception thrown"
# --------------------------------------------------------------------------- #
def fix_returns_void(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    count = 0
    details: List[str] = []
    for i, line in region.iter_code_lines(lines):
        if not re.search(r"//\s*returns void\s*$", line):
            continue
        comment_index = line.index("//")
        if re.match(r"^\s*\*\s*$", line[:comment_index]):
            continue
        lines[i] = re.sub(r"//\s*returns void\s*$", "// no exception thrown", line)
        count += 1
        details.append(f"{path}:{i + 1}: returns void -> no exception thrown")
    return lines, count, details


# --------------------------------------------------------------------------- #
# sample-member-misuse: array variable ".size()" -> ".length"
# --------------------------------------------------------------------------- #
def fix_sample_member_misuse(path: str, lines: List[str]) -> FixResult:
    new_lines = list(lines)
    count = 0
    details: List[str] = []
    for block_start, block in _iter_full_code_blocks(lines):
        array_vars = set()
        for ln in block:
            array_vars.update(_ARRAY_DECL.findall(_code_of(ln)))
        if not array_vars:
            continue
        for offset in range(len(block)):
            idx = block_start + offset
            for var in array_vars:
                pat = re.compile(r"\b" + re.escape(var) + r"\.size\(\)")
                hits = len(pat.findall(new_lines[idx]))
                if hits:
                    new_lines[idx] = pat.sub(f"{var}.length", new_lines[idx])
                    count += hits
                    details.append(f"{path}:{idx + 1}: {var}.size() -> {var}.length")
    return new_lines, count, details


# --------------------------------------------------------------------------- #
# literal-artifacts: strip a leaked "// returns" out of strings / URLs in code
# --------------------------------------------------------------------------- #
def _fix_string_artifacts(text: str) -> str:
    text = re.sub(r"://\s+returns\s+", "://", text, flags=re.I)
    text = re.sub(r"//\s+returns\s+", "//", text, flags=re.I)
    return text


def fix_literal_artifacts(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    count = 0
    for i, line in region.iter_code_lines(lines):
        idx = region.comment_index_outside_literals(line)
        fixed = _fix_string_artifacts(line) if idx < 0 else _fix_string_artifacts(line[:idx]) + line[idx:]
        if fixed != line:
            lines[i] = fixed
            count += 1
    return lines, count, []


# --------------------------------------------------------------------------- #
# assignment-style: "Type.call(...) = result" -> "Type.call(...);  // returns result"
# --------------------------------------------------------------------------- #
_ASSIGN_RE = re.compile(r"^(\s*\*\s+)([A-Z][\w$]*(?:\.[A-Za-z_$][\w$]*)+\s*\(.*\))\s*=\s*(.+?)\s*$")


def _result_comment(result: str) -> str:
    rhs = result.strip().rstrip()
    m = re.match(r"^throws\s+(?:an?\s+)?(.+)$", rhs, re.I)
    if m:
        return f"throws {m.group(1).strip()}"
    m = re.match(r"^(?:an?\s+)?([A-Za-z_$][\w$]*(?:Exception|Error)\b.*)$", rhs)
    if m:
        return f"throws {m.group(1).strip()}"
    return f"returns {rhs}"


def _is_public_method_declaration(decl: str) -> bool:
    return bool(re.match(r"^public\b", decl)) and not re.search(r"\b(class|interface|enum|@interface)\b", decl) and "(" in decl


def fix_assignment_style(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    count = 0
    details: List[str] = []
    for start, end in region.iter_javadoc_blocks(lines):
        decl = ""
        for i in range(end + 1, len(lines)):
            t = lines[i].strip()
            if not t or t.startswith("@"):
                continue
            decl = t
            break
        if not _is_public_method_declaration(decl):
            continue
        in_code = False
        for j in range(start, end + 1):
            if not in_code and "<pre>{@code" in lines[j]:
                in_code = True
                continue
            if in_code and "</pre>" in lines[j]:
                in_code = False
                continue
            if not in_code:
                continue
            m = _ASSIGN_RE.match(lines[j])
            if not m:
                continue
            prefix, call, result = m.group(1), m.group(2), m.group(3)
            if "//" in call or "//" in result:
                continue
            converted = f"{prefix}{call};   // {_result_comment(result)}"
            if converted != lines[j]:
                details.append(f"{path}:{j + 1}: {lines[j].strip()} -> {converted.strip()}")
                lines[j] = converted
                count += 1
    return lines, count, details


# --------------------------------------------------------------------------- #
# type-placeholders: "Type<...>" -> "Type<?, ?, ?>" inside code
# --------------------------------------------------------------------------- #
def fix_type_placeholders(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    count = 0
    for i, line in region.iter_code_lines(lines):
        updated = re.sub(r"\b([A-Z][A-Za-z0-9_.]*)<\.\.\.>", r"\1<?, ?, ?>", line)
        if updated != line:
            lines[i] = updated
            count += 1
    return lines, count, []


# --------------------------------------------------------------------------- #
# literal-action-comments: lowercase a leading action verb in trailing comments
# --------------------------------------------------------------------------- #
_ACTION_LOWER = [
    "Adds", "Removes", "Fills", "Sorts", "Reverses", "Rotates", "Shuffles",
    "Copies", "Pads", "Prints", "Keeps", "Leaves", "Uses", "Treats", "Searches",
    "Starts", "Ends", "Invokes", "Converts",
]


def fix_literal_action_comments(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    count = 0
    for i, line in region.iter_code_lines(lines):
        idx = region.comment_index_outside_literals(line)
        if idx < 0 or _is_standalone_sample_comment(line, idx):
            continue
        before, comment = line[:idx], line[idx:]
        text = re.sub(r"^//\s*", "", comment).lstrip()
        if re.match(r"^[\[{]|^[\"']", text):
            normalized = comment
        else:
            normalized = comment
            for word in _ACTION_LOWER:
                normalized = re.sub(r"^//\s*" + word + r"\b", "// " + word.lower(), normalized)
        if normalized != comment:
            lines[i] = before + normalized
            count += 1
    return lines, count, []


# --------------------------------------------------------------------------- #
# trailing-comment-style: normalise "Returns:/Throws:/Verb" comment phrasing
# --------------------------------------------------------------------------- #
_TRAILING_REPLACEMENTS = [
    (re.compile(r"^Remove\b"), "removes"), (re.compile(r"^Keep\b"), "keeps"),
    (re.compile(r"^Transform\b"), "transforms"), (re.compile(r"^Collect\b"), "collects"),
    (re.compile(r"^Use\b"), "uses"), (re.compile(r"^Filter\b"), "filters"),
    (re.compile(r"^Switch\b"), "switches"), (re.compile(r"^Maintain\b"), "maintains"),
    (re.compile(r"^Stop\b"), "stops"), (re.compile(r"^Only process\b"), "processes only"),
    (re.compile(r"^Sum\b"), "sums"), (re.compile(r"^Logs\b"), "logs"),
    (re.compile(r"^Log\b"), "logs"), (re.compile(r"^Buffer\b"), "buffers"),
    (re.compile(r"^Waits\b"), "waits"), (re.compile(r"^Get\b"), "gets"),
    (re.compile(r"^Reuse\b"), "reuses"), (re.compile(r"^Elements are\b"), "elements are"),
    (re.compile(r"^Inserts\b"), "inserts"), (re.compile(r"^File contains\b"), "file contains"),
    (re.compile(r"^Writer contains\b"), "writer contains"),
    (re.compile(r"^Output stream contains\b"), "output contains"),
    (re.compile(r"^Stream with\b"), "stream contains"),
    (re.compile(r"^Empty stream\b"), "stream is empty"),
    (re.compile(r"^New stream\b"), "creates a new stream"),
    (re.compile(r"^Auto-closed\b"), "closes automatically"),
    (re.compile(r"^Terminal operation\s*-\s*"), "closes stream as terminal operation; "),
    (re.compile(r"^User must close the reader\b"), "closes reader manually"),
]


def _normalize_trailing(comment: str, code: str) -> str:
    text = re.sub(r"^//\s*", "", comment).strip()
    lowered = (text[:1].lower() + text[1:]) if text else text
    if re.match(r"^Returns:?\s*", text):
        return ("// returns " + re.sub(r"^Returns:?\s*", "", text)).rstrip()
    if re.match(r"^Return:?\s*", text):
        return ("// returns " + re.sub(r"^Return:?\s*", "", text)).rstrip()
    if re.match(r"^Throws:?\s*", text):
        return ("// throws " + re.sub(r"^Throws:?\s*", "", text)).rstrip()
    if re.match(r"^Throw:?\s*", text):
        return ("// throws " + re.sub(r"^Throw:?\s*", "", text)).rstrip()
    if re.match(r"^IllegalStateException:?\s*", text):
        return ("// throws IllegalStateException; " + re.sub(r"^IllegalStateException:?\s*", "", text)).rstrip()
    if re.match(r"^Contains CSV\b", text) and re.search(r"\bcsv\b", code, re.I):
        return "// csv contains" + re.sub(r"^Contains CSV", " CSV", text)
    if re.match(r"^Terminal operation\s*-\s*closes stream\b", text):
        return "// closes stream as terminal operation"
    for pattern, replacement in _TRAILING_REPLACEMENTS:
        if pattern.search(text):
            return "// " + pattern.sub(replacement, text)
    if re.match(r"^[A-Z][a-z]", text) and re.search(r"\b(?:contains|is|are|has|becomes)\b", lowered):
        return "// " + lowered
    return comment


def fix_trailing_comment_style(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    count = 0
    for i, line in region.iter_code_lines(lines):
        idx = region.comment_index_outside_literals(line)
        if idx < 0 or _is_standalone_sample_comment(line, idx):
            continue
        before, comment = line[:idx], line[idx:]
        normalized = _normalize_trailing(comment, re.sub(r"^\s*\*\s?", "", before))
        if normalized != comment:
            lines[i] = before + normalized
            count += 1
    return lines, count, []


# --------------------------------------------------------------------------- #
# example-comments: rewrite/drop trailing comments toward "// returns ..."
# --------------------------------------------------------------------------- #
_DROP_KEYWORDS = re.compile(
    r"^//\s*(\d+\s*(bytes?|bits?|MB|KB)|seed\s*=|Limit\b|Hash by\b|Compare by\b|"
    r"Custom\b|hasNext logic|output logic|method may throw|Recursive\b|Skip\b|"
    r"Non-recursive\b|Fallback\b|Recovery\b|May throw\b|Only called\b|Default\b|"
    r"row\b|col\b|column\b|group by\b|aggregate\b|result column\b|"
    r"transform function\b|Duplicate\b|initial state\b|continue while\b|"
    r"generate next\b|supplier\b|next\b|hasNext\b|Bottom\b|Top\b|different\b|"
    r"another\b|Automatic\b|closed\b|weather may be null\b|Just verify\b|"
    r"equal ranges\b|Changed\b|Renamed\b|New field\b|Maps to\b)", re.I,
)


def _normalize_example_comment(comment: str) -> str:
    c = comment
    c = re.sub(r"^//\s*returns\s+throws\b", "// throws", c, flags=re.I)
    c = re.sub(r"^//\s*-->\s*", "// returns ", c, flags=re.I)
    c = re.sub(r"^//\s*Returns\b", "// returns", c)
    c = re.sub(r"^//\s*Return\b", "// returns", c)
    c = re.sub(r"^//\s*Throws\b", "// throws", c)
    c = re.sub(r"^//\s*Throw\b", "// throws", c)
    c = re.sub(r"^//\s*Prints?:\s*", "// returns void; prints ", c, flags=re.I)
    c = re.sub(r"^//\s*Creates?\s+", "// returns ", c, flags=re.I)
    c = re.sub(r"^//\s*Same\s+", "// returns same ", c, flags=re.I)
    c = re.sub(r"^//\s*([\[{].*)$", r"// returns \1", c)
    c = re.sub(r"^//\s*(true|false|null)\b", lambda m: f"// returns {m.group(1).lower()}", c, flags=re.I)
    c = re.sub(r"""^//\s*(\[[^\]]*\]|\{[^}]*\}|"[^"]*"|'.*'|-?\d[\w.]*)\b""", r"// returns \1", c)
    return c


def _should_drop_comment(code: str, comment: str) -> bool:
    trimmed = code.strip()
    c = comment.strip()
    if _DROP_KEYWORDS.match(c):
        return True
    if (re.search(r"[,({\[\]}]\s*$", trimmed) or re.match(r"^\s*(return|if|})\b", trimmed)
            or re.match(r"^\s*[\w.]+\s*->", trimmed) or re.match(r"""^\s*[\w()[\]."' -]+,?\s*$""", trimmed)):
        return True
    if (re.match(r"^\w[\w<>\[\], ?]*\s+\w+\s*=", trimmed) and not re.search(r"[A-Z][A-Za-z0-9_]*\s*\.", trimmed)
            and not re.search(r"\.\w+\s*\(", trimmed)):
        return True
    if re.match(r"^[),;]*$", trimmed):
        return True
    return False


def fix_example_comments(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    count = 0
    for i, line in region.iter_code_lines(lines):
        idx = region.comment_index_outside_literals(line)
        if idx < 0 or _is_standalone_sample_comment(line, idx):
            continue
        before, comment = line[:idx], line[idx:]
        code = re.sub(r"^\s*\*\s?", "", before)
        normalized = _normalize_example_comment(comment)
        if re.match(r"^//\s+(returns|throws)\b", normalized.strip()):
            if normalized != comment:
                lines[i] = before + normalized
                count += 1
            continue
        if _should_drop_comment(code, normalized):
            lines[i] = before.rstrip()
            count += 1
            continue
        if "=" in code or re.search(r"\.\w+\s*\(", code):
            lines[i] = before + re.sub(r"^//\s*", "// returns ", normalized)
            count += 1
    return lines, count, []


# --------------------------------------------------------------------------- #
# move-comments-into-pre: pull stray "// ..." sample comments inside the block
# --------------------------------------------------------------------------- #
def _is_sample_comment(line: str) -> bool:
    return bool(re.match(r"^\s*\*\s+//\s+\S", line))


def fix_move_comments_into_pre(path: str, lines: List[str]) -> FixResult:
    lines = list(lines)
    total = 0
    details: List[str] = []
    i = 0
    while i < len(lines):
        mask = region.active_javadoc_line_mask(lines)
        if not mask[i] or "Usage Examples" not in lines[i]:
            i += 1
            continue

        comments_before = []
        pre_index = -1
        j = i + 1
        while j < min(len(lines), i + 20) and mask[j]:
            if "<pre>{@code" in lines[j]:
                pre_index = j
                break
            if _is_sample_comment(lines[j]):
                comments_before.append({"index": j, "line": lines[j]})
            j += 1
        if pre_index < 0:
            i += 1
            continue

        j = i - 1
        while j >= 0 and mask[j] and _is_sample_comment(lines[j]):
            comments_before.insert(0, {"index": j, "line": lines[j]})
            j -= 1

        end_pre = pre_index + 1
        while end_pre < len(lines) and mask[end_pre] and "}</pre>" not in lines[end_pre]:
            end_pre += 1
        if end_pre >= len(lines) or not mask[end_pre]:
            i += 1
            continue

        comments_after = []
        j = end_pre + 1
        while j < len(lines) and mask[j] and _is_sample_comment(lines[j]):
            comments_after.append({"index": j, "line": lines[j]})
            j += 1

        comments = comments_before + comments_after
        if not comments:
            i += 1
            continue

        for c in comments_before:
            details.append(f"{path}:{c['index'] + 1}: move before pre {c['line'].strip()}")
        for c in comments_after:
            details.append(f"{path}:{c['index'] + 1}: move before close {c['line'].strip()}")
        total += len(comments)

        for c in sorted(comments, key=lambda c: c["index"], reverse=True):
            del lines[c["index"]]

        removed_before_pre = sum(1 for c in comments if c["index"] < pre_index)
        adjusted_pre = pre_index - removed_before_pre
        removed_before_end = sum(1 for c in comments if c["index"] < end_pre)
        adjusted_end = end_pre - removed_before_end
        insert_index = adjusted_pre + 1

        if insert_index < len(lines) and re.match(r"^\s*\*\s*$", lines[insert_index] if insert_index < len(lines) else ""):
            del lines[insert_index]
            adjusted_end -= 1
        if comments_before:
            for offset, c in enumerate(comments_before):
                lines.insert(insert_index + offset, c["line"])
            adjusted_end += len(comments_before)
        if comments_after:
            close_insert = adjusted_end
            if close_insert > 0 and not re.match(r"^\s*\*\s*$", lines[close_insert - 1]):
                lines.insert(close_insert, _blank_for(lines[close_insert]))
                adjusted_end += 1
            for offset, c in enumerate(comments_after):
                lines.insert(adjusted_end + offset, c["line"])

        i = insert_index + len(comments_before)
    return lines, total, details


# --------------------------------------------------------------------------- #
# registry -- name -> fixer
# --------------------------------------------------------------------------- #
FIXES = {
    "usage-indent": fix_usage_indent,
    "usage-spacing": fix_usage_spacing,
    "double-blank": fix_double_blank,
    "returns-void": fix_returns_void,
    "sample-member-misuse": fix_sample_member_misuse,
    "literal-artifacts": fix_literal_artifacts,
    "assignment-style": fix_assignment_style,
    "type-placeholders": fix_type_placeholders,
    "literal-action-comments": fix_literal_action_comments,
    "trailing-comment-style": fix_trailing_comment_style,
    "example-comments": fix_example_comments,
    "move-comments-into-pre": fix_move_comments_into_pre,
}
