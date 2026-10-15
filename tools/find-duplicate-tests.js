#!/usr/bin/env node
"use strict";
const tools = require("./test-tools");
const args = process.argv.slice(2);
const option = name => args.find(a => a.startsWith(name + "="))?.slice(name.length + 1);
const tests = tools.allTests(tools.inventory(tools.javaFiles(tools.testRoot)));
const scope = option("--scope") || "all";
if (!["all", "same-file", "cross-file"].includes(scope)) throw new Error("Invalid --scope");
const match = option("--match");
const groups = tools.duplicates(tests).filter(g => (scope === "all" || g.scope === scope)
  && (!match || g.methods.some(m => m.file.includes(match) || m.method.includes(match))));
// Cross-type copies remain candidates requiring manual review, never automatic deletion.
const report = { scannedFiles: new Set(tests.map(t => t.file)).size, scannedMethods: tests.length,
  scope, duplicateGroupCount: groups.length, duplicateMethodCount: groups.reduce((n, g) => n + g.methods.length, 0) };
if (args.includes("--summary-by-file")) {
  report.files = [...new Set(tests.map(t => t.file))].map(file => ({ file: tools.relative(file),
    totalMethods: tests.filter(t => t.file === file).length,
    duplicateMethods: groups.flatMap(g => g.methods).filter(m => m.file === tools.relative(file)).length }));
} else if (option("--unique-file")) {
  const locations = new Set(groups.flatMap(g => g.methods).map(m => m.file + ":" + m.line));
  const selected = tests.filter(t => t.file.includes(option("--unique-file"))).map(tools.location);
  report.methods = selected.filter(m => !locations.has(m.file + ":" + m.line));
  report.duplicateMethods = selected.filter(m => locations.has(m.file + ":" + m.line));
} else report.groups = groups;
console.log(JSON.stringify(report, null, 2));
if (args.includes("--check") && groups.length) process.exitCode = 1;
