#!/usr/bin/env node
"use strict";
const tools = require("./test-tools");
const tests = tools.allTests(tools.inventory(tools.javaFiles(tools.testRoot)));
function asserts(method, helpers, seen = new Set()) {
  if (seen.has(method)) return false;
  seen.add(method);
  return method.calls.some(call => /^(assert[A-Z]|fail$)/.test(call.name)
    || helpers.some(h => h.name === call.name && asserts(h, helpers, seen)));
}
const withoutAssertions = tests.filter(test => !asserts(test,
  test.chain.flatMap(type => type.members.filter(m => m.kind === "METHOD" && !tools.isTest(m))))).map(tools.location);
const selfComparisons = tests.filter(test => /assert(?:Equals|Same)\(\s*([^,;\n]+),\s*\1\s*\)/.test(test.normalized)).map(tools.location);
console.log(JSON.stringify({ scannedMethods: tests.length, withoutAssertions, selfComparisons,
  note: "Assertion presence and self-comparison checks are review aids, not proof that a test verifies its contract." }, null, 2));
if (process.argv.includes("--check") && withoutAssertions.length) process.exitCode = 1;
