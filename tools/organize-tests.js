#!/usr/bin/env node
"use strict";
const fs = require("node:fs");
const tools = require("./test-tools");

const upper = value => value.charAt(0).toUpperCase() + value.slice(1);
const normalizedName = value => value.toLowerCase().replace(/[^a-z0-9]/g, "");
const legacyNames = {
  of: "wrap", create: "from", getRows: "rowCount", getColumns: "columnCount",
  getRow: "rowCopy", getColumn: "columnCopy", streamLU2RD: "mainDiagonalStream",
  streamRU2LD: "antiDiagonalStream", setLU2RD: "setMainDiagonal", setRU2LD: "setAntiDiagonal",
  diagonalLU2RD: "mainDiagonalCopy", diagonalRU2LD: "antiDiagonalCopy",
  getLU2RD: "mainDiagonalCopy", getRU2LD: "antiDiagonalCopy",
  updateLU2RD: "updateMainDiagonal", updateRU2LD: "updateAntiDiagonal",
  streamH: "rowMajorStream", streamV: "columnMajorStream",
  streamR: "rowStreams", streamC: "columnStreams", componentType: "elementType",
  upOf: "valueAbove", downOf: "valueBelow", leftOf: "valueLeft", rightOf: "valueRight",
  row: "rowView", column: "columnCopy", reverseH: "flipHorizontallyInPlace", reverseV: "flipVerticallyInPlace",
  flipH: "flipHorizontally", flipV: "flipVertically", hstack: "stackHorizontally", vstack: "stackVertically",
  pointsLU2RD: "mainDiagonalPoints", pointsRU2LD: "antiDiagonalPoints",
  pointsH: "rowMajorPoints", pointsV: "columnMajorPoints", pointsR: "rowPoints", pointsC: "columnPoints",
  flatOp: "mutateViaFlatArray", pad: "resize", multiply: "matrixMultiply", println: "toMultilineString",
};

function operations(source, files) {
  const result = [];
  function add(type) {
    for (const m of type.members) if (m.kind === "METHOD" && !result.includes(m.name)) result.push(m.name);
    const parent = files.flatMap(f => f.types).find(t => t.name === type.extends.split("<")[0]);
    if (parent) add(parent);
  }
  add(source.types[0]);
  if (source.types[0].kind === "ENUM") result.push("values", "valueOf", "name", "ordinal", "toString", "equals", "hashCode", "compareTo", "getDeclaringClass");
  return [...new Set(result)];
}

function operation(test, names, owner) {
  let label = test.name.replace(/^test_?/, "").replace(new RegExp("^" + owner + "_?", "i"), "");
  if (/^constructor/i.test(label)) return "<init>";
  const matching = names.filter(n => n !== "<init>" && normalizedName(label).startsWith(normalizedName(n))).map(n => ({ name: n, length: n.length }));
  for (const [oldName, current] of Object.entries(legacyNames)) {
    if (normalizedName(label).startsWith(normalizedName(oldName)) && names.includes(current)) matching.push({ name: current, length: oldName.length });
  }
  if (matching.length) {
    matching.sort((a, b) => b.length - a.length);
    const invoked = matching.find(candidate => test.calls.some(call => call.name === candidate.name));
    if (invoked) return invoked.name;
    if (!test.calls.some(call => names.includes(call.name) && !/^(get|rowCount|columnCount|isEmpty|wrap|copyOf|empty|<init>)$/.test(call.name))) return matching[0].name;
  }
  const mentioned = names.filter(n => !/^(<init>|empty|wrap|from|get|of|create)$/.test(n) && normalizedName(label).includes(normalizedName(n)) && test.calls.some(c => c.name === n));
  if (mentioned.length) return mentioned.sort((a, b) => b.length - a.length)[0];
  const calls = test.calls.filter(c => names.includes(c.name) && !/^(get|rowCount|columnCount|isEmpty|wrap|copyOf|empty|<init>)$/.test(c.name));
  if (calls.length) return calls[0].name;
  return test.calls.find(c => names.includes(c.name))?.name || "contracts";
}

function scenarioRank(test) {
  if (/throws|invalid|reject|exception|outofbound|negative|nullargument/i.test(test.name)) return 2;
  if (/empty|zero|single|boundary|overflow|underflow|null|nan|infinity|alias|min|max/i.test(test.name)) return 1;
  return 0;
}

function snippet(raw, member, indent) {
  const lineStart = raw.lastIndexOf("\n", member.start - 1) + 1;
  const originalIndent = member.start - lineStart;
  const text = raw.slice(member.start, member.end).replace(/\r\n/g, "\n");
  const preceding = raw.slice(0, lineStart).trimEnd();
  let comment = "";
  if (preceding.endsWith("*/")) {
    comment = preceding.slice(preceding.lastIndexOf("/*"));
  } else {
    const lines = preceding.split(/\r?\n/);
    const comments = [];
    while (lines.length && lines.at(-1).trimStart().startsWith("//")) comments.unshift(lines.pop().trimStart());
    comment = comments.filter(line => !/^\/\/\s*[=-]{3}/.test(line)).join("\n");
  }
  const prefix = comment ? comment.split(/\r?\n/).map(line => " ".repeat(indent) + line.trimStart()).join("\n") + "\n" : "";
  return prefix + text.split("\n").map((line, i) => line.trim() ? " ".repeat(indent) + (i === 0 ? line : line.slice(Math.min(originalIndent, line.search(/\S|$/)))) : "").join("\n");
}

function scenario(test) {
  const parts = [];
  if (/Javadoc/.test(test.className)) parts.push("DocumentedExample");
  const arrays = test.arrays || [];
  const rectangular = arrays.find(a => a.rowLengths.length && a.rowLengths.every(n => n >= 0));
  if (rectangular) {
    const widths = [...new Set(rectangular.rowLengths)];
    parts.push(widths.length === 1 ? "Shape" + rectangular.length + "By" + widths[0] : "RaggedRows");
  } else {
    const dimensions = arrays.find(a => a.dimensions.length > 1 && a.dimensions.every(d => /^\d+$/.test(d)));
    if (dimensions) parts.push("Shape" + dimensions.dimensions.join("By"));
    else if (test.identifiers.includes("matrix")) parts.push("Fixture");
    else if (test.calls.some(c => c.name === "empty")) parts.push("EmptyMatrix");
  }
  if (/\bnull\b/.test(test.normalized)) parts.push("NullInput");
  if (test.calls.some(c => c.name === "assertNotSame")) parts.push("IndependentStorage");
  if (test.calls.some(c => c.name === "assertSame")) parts.push("Identity");
  if (test.calls.some(c => c.name === "assertThrows")) parts.push("Validation");
  if (test.calls.some(c => c.name === "assertArrayEquals")) parts.push("ArrayValues");
  return parts.join("_") || "Values";
}

function plan(files, options = {}) {
  const tests = tools.allTests(files.filter(f => f.file.includes("/src/test/")));
  const sources = files.filter(f => f.file.includes("/src/main/"));
  const outputs = [];
  const records = [];
  const removed = [];
  const ambiguousNames = [];
  for (const source of sources) {
    const owner = source.types[0]?.name;
    const file = files.find(f => f.file.endsWith("/" + owner + "Test.java"));
    if (!file || (options.match && !file.file.includes(options.match))) continue;
    const raw = fs.readFileSync(file.file, "utf8");
    const type = file.types[0];
    const selected = tests.filter(t => t.file === file.file);
    const names = operations(source, sources);
    const helpers = [];
    const helperKeys = new Map();
    function collectHelpers(container) {
      if (container !== type) {
        if (container.annotations.some(a => a !== "@Nested" && !/^@Tag\("(?:2025|2510|2511|2512)"\)$/.test(a))
          || (container.extends && container.extends !== "TestBase")) {
          throw new Error("Review nested class metadata before moving tests: " + container.name);
        }
        for (const member of container.members.filter(tools.isLifecycle)) {
          if (!type.members.some(outer => tools.isLifecycle(outer) && outer.normalized === member.normalized
              && JSON.stringify(outer.annotations) === JSON.stringify(member.annotations))) {
            throw new Error("Nested lifecycle is not equivalent to an outer hook: " + container.name + "." + member.name);
          }
        }
        for (const field of container.members.filter(m => m.kind === "VARIABLE")) {
          if (!/\bstatic\s+final\b/.test(field.normalized)) throw new Error("Review nested mutable fixture before hoisting: " + container.name + "." + field.name);
        }
      }
      for (const member of container.members) {
        if (tools.isTest(member)) continue;
        if (member.members && member.annotations.includes("@Nested")) collectHelpers(member);
        else {
          const key = member.signature || member.name || member.kind + member.start;
          const previous = helperKeys.get(key);
          if (previous) {
            if (previous.normalized !== member.normalized) throw new Error("Conflicting fixture/helper: " + file.file + " " + key);
          } else {
            helperKeys.set(key, member);
            helpers.push(member);
          }
        }
      }
    }
    collectHelpers(type);
    // Lifecycle hooks with different names but exactly equal bodies are redundant after hoisting.
    const helperBodies = new Set();
    const uniqueHelpers = helpers.filter(member => {
      if (!tools.isLifecycle(member)) return true;
      const key = JSON.stringify([member.annotations, member.normalized]);
      if (helperBodies.has(key)) return false;
      helperBodies.add(key);
      return true;
    });
    const selectedByBody = new Map();
    const contexts = new Map(selected.map(test => [test, tools.contextKey(test)]));
    const retained = [];
    for (const test of selected.sort((a, b) => b.normalized.length - a.normalized.length || a.chain.length - b.chain.length || a.start - b.start)) {
      const key = JSON.stringify([test.parameters, test.annotations, test.normalized, contexts.get(test)]);
      const stronger = options.dedupe && retained.find(candidate => contexts.get(candidate) === contexts.get(test)
        && candidate.normalized.startsWith(test.normalized.slice(0, -2))
        && candidate.normalized.slice(test.normalized.length - 2).trim().startsWith("assert")
        && JSON.stringify(candidate.parameters) === JSON.stringify(test.parameters));
      if (options.dedupe && (selectedByBody.has(key) || stronger)) {
        removed.push({ removed: tools.location(test), retained: tools.location(selectedByBody.get(key) || stronger), reason: selectedByBody.has(key) ? "Equal AST body and fixture context" : "Entire AST body retained as a prefix of a stronger test in the same fixture context" });
      } else {
        selectedByBody.set(key, test);
        retained.push(test);
      }
    }
    const groups = new Map();
    for (const test of retained) {
      const op = operation(test, names, owner);
      if (!groups.has(op)) groups.set(op, []);
      groups.get(op).push(test);
    }
    const groupText = [];
    for (const [op, members] of [...groups].sort((a, b) => {
      const rank = n => names.includes(n) ? names.indexOf(n) : 100000;
      return rank(a[0]) - rank(b[0]) || a[0].localeCompare(b[0]);
    })) {
      const usedNames = new Set();
      const rendered = [];
      for (const test of members.sort((a, b) => scenarioRank(a) - scenarioRank(b) || a.start - b.start)) {
        let name = test.name.startsWith("test") ? test.name.replace(/^test_([a-z])/, (_, c) => "test" + c.toUpperCase()) : "test" + upper(test.name);
        if (usedNames.has(name)) name += "_" + scenario(test);
        if (usedNames.has(name)) {
          throw new Error("Choose a distinct scenario name before moving " + test.className + "." + test.name);
        }
        usedNames.add(name);
        let body = snippet(raw, test, 8);
        const signatureEnd = test.bodyStart - test.start;
        const head = raw.slice(test.start, test.bodyStart).replace(new RegExp("\\b" + test.name + "\\s*\\("), name + "(");
        body = snippet(raw.slice(0, test.start) + head + raw.slice(test.bodyStart), { ...test, end: test.end + head.length - signatureEnd }, 8);
        // Keep the existing public test method convention consistently.
        body = body.replace(/^(\s*)(?!public\b)(void\s+)/m, "$1public $2");
        for (const annotation of test.annotations.filter(a => a.startsWith("@MethodSource"))) {
          const providers = [...annotation.matchAll(/"([^"]+)"/g)].map(m => m[1]);
          if (!providers.length) throw new Error("Use an explicit static @MethodSource before organizing " + test.name);
          for (const provider of providers.filter(p => !p.includes("#"))) {
            const helper = uniqueHelpers.find(m => m.kind === "METHOD" && m.name === provider && m.modifiers.includes("static"));
            if (!helper) throw new Error("Cannot safely relocate @MethodSource provider " + provider);
            const ownerName = (file.package ? file.package + "." : "") + type.name;
            body = body.replace('"' + provider + '"', '"' + ownerName + '#' + provider + '"');
          }
        }
        rendered.push(body);
        records.push({ from: tools.location(test), to: { file: tools.relative(file.file), className: type.name + "." + (op === "<init>" ? "Constructor" : upper(op)) + "Tests", method: name } });
      }
      const groupName = (op === "<init>" ? "Constructor" : upper(op)) + "Tests";
      groupText.push("    @Nested\n    class " + groupName + " {\n\n" + rendered.join("\n\n") + "\n    }");
    }
    const imports = new Set(file.imports.filter(s => s !== "import org.junit.jupiter.api.Tag;" || type.annotations.some(a => a.startsWith("@Tag"))));
    imports.add("import org.junit.jupiter.api.Nested;");
    const copyright = raw.match(/^\s*(\/\*[\s\S]*?\*\/)/)?.[1] || "";
    const importRank = s => s.startsWith("import static ") ? 0 : s.startsWith("import java.") || s.startsWith("import javax.") ? 1 : s.startsWith("import org.") ? 2 : 3;
    const orderedImports = [...imports].sort((a, b) => importRank(a) - importRank(b) || (a < b ? -1 : a > b ? 1 : 0));
    const importText = orderedImports.map((line, index) => (index && importRank(line) !== importRank(orderedImports[index - 1]) ? "\n" : "") + line).join("\n");
    const header = (copyright ? copyright + "\n" : "") + (file.package ? "package " + file.package + ";\n\n" : "") + importText + "\n\n";
    const helpersText = uniqueHelpers.sort((a, b) => (a.kind === "VARIABLE" ? 0 : tools.isLifecycle(a) ? 1 : 2) - (b.kind === "VARIABLE" ? 0 : tools.isLifecycle(b) ? 1 : 2) || a.start - b.start).map(m => snippet(raw, m, 4)).join("\n\n");
    const output = header + (type.annotations.length ? type.annotations.join("\n") + "\n" : "")
      + "class " + type.name + " extends " + (type.extends || "MatrixTestSupport") + " {\n\n" + (helpersText ? helpersText + "\n\n" : "") + groupText.join("\n\n") + "\n}\n";
    if (output.replace(/\r\n/g, "\n") !== raw.replace(/\r\n/g, "\n")) outputs.push({ file: file.file, content: output });
  }
  return { outputs, records, removed, ambiguousNames, scannedMethods: tests.length };
}

function main(args = process.argv.slice(2)) {
  const options = { match: args.find(a => a.startsWith("--match="))?.slice(8), dedupe: args.includes("--dedupe") };
  const result = plan(tools.inventory(), options);
  const report = { scannedMethods: result.scannedMethods, changedFiles: result.outputs.map(o => tools.relative(o.file)), removed: result.removed, ambiguousNames: result.ambiguousNames };
  const reportPath = args.find(a => a.startsWith("--report="))?.slice(9);
  if (reportPath) fs.writeFileSync(reportPath, JSON.stringify({ ...report, moved: result.records }, null, 2) + "\n");
  if (args.includes("--apply")) for (const output of result.outputs) fs.writeFileSync(output.file, output.content);
  if (args.includes("--check") && result.outputs.length) process.exitCode = 1;
  console.log(JSON.stringify(report, null, 2));
}

if (require.main === module) main();
module.exports = { plan, operation, scenarioRank, legacyNames, main };
