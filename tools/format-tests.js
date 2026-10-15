#!/usr/bin/env node
"use strict";
const fs = require("node:fs");
const tools = require("./test-tools");
const args = process.argv.slice(2);
const files = tools.inventory(tools.javaFiles(tools.testRoot));
const rank = s => s.startsWith("import static ") ? 0 : /^import javax?\./.test(s) ? 1 : s.startsWith("import org.") ? 2 : 3;
const changed = [];
for (const file of files) {
  let raw = fs.readFileSync(file.file, "utf8").replace(/\r\n/g, "\n");
  const original = raw;
  const references = new Set(file.references);
  const imports = new Set();
  for (const line of file.imports) {
    if (line === "import static org.junit.jupiter.api.Assertions.*;") {
      for (const name of references) if (/^(assertAll|assertArrayEquals|assertDoesNotThrow|assertEquals|assertFalse|assertInstanceOf|assertIterableEquals|assertLinesMatch|assertNotEquals|assertNotNull|assertNotSame|assertNull|assertSame|assertThrows|assertThrowsExactly|assertTimeout|assertTimeoutPreemptively|assertTrue|fail)$/.test(name)) imports.add("import static org.junit.jupiter.api.Assertions." + name + ";");
    } else {
      const simple = line.match(/\.([\w$*]+);$/)?.[1];
      if (simple === "*" || references.has(simple)) imports.add(line);
    }
  }
  const ordered = [...imports].sort((a, b) => rank(a) - rank(b) || (a < b ? -1 : a > b ? 1 : 0));
  const importText = ordered.map((line, i) => (i && rank(line) !== rank(ordered[i - 1]) ? "\n" : "") + line).join("\n");
  const first = raw.search(/^import /m);
  const matches = [...raw.matchAll(/^import .*;\s*$/gm)];
  if (first >= 0) {
    const last = matches.at(-1);
    raw = raw.slice(0, first) + importText + "\n\n" + raw.slice(last.index + last[0].length).trimStart();
  }
  raw = raw.replace(/[ \t]+$/gm, "").replace(/\n{3,}/g, "\n\n");
  if (raw !== original) {
    changed.push(tools.relative(file.file));
    if (args.includes("--apply")) fs.writeFileSync(file.file, raw);
  }
}
console.log(JSON.stringify({ changedFiles: changed }, null, 2));
if (args.includes("--check") && changed.length) process.exitCode = 1;
