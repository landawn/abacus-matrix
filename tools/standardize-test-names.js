#!/usr/bin/env node
"use strict";
const fs = require("node:fs");
const tools = require("./test-tools");
const { legacyNames } = require("./organize-tests");
const args = process.argv.slice(2);
const inventory = tools.inventory();
const files = inventory.filter(f => f.file.includes("/src/test/"));
const tests = tools.allTests(files);
const changes = [];
for (const file of files) {
  let raw = fs.readFileSync(file.file, "utf8");
  const used = new Set(tests.filter(t => t.file === file.file).map(t => t.className + "#" + t.name));
  for (const test of tests.filter(t => t.file === file.file).sort((a, b) => b.start - a.start)) {
    let name = test.name.startsWith("test") ? test.name : "test" + test.name[0].toUpperCase() + test.name.slice(1);
    const owner = file.types[0].name.replace(/Test$/, "");
    name = name.replace(new RegExp("^test" + owner + "_"), "test");
    name = name.replace(/^test_?([a-z])/, (_, c) => "test" + c.toUpperCase());
    const currentNames = inventory.find(f => f.file.endsWith("/" + owner + ".java"))?.types[0].members.filter(m => m.kind === "METHOD").map(m => m.name) || [];
    for (const [oldName, current] of Object.entries(legacyNames).sort((a, b) => b[0].length - a[0].length)) {
      const prefix = "test" + oldName[0].toUpperCase() + oldName.slice(1);
      const longerCurrentName = currentNames.some(n => n.length > oldName.length && name.startsWith("test" + n[0].toUpperCase() + n.slice(1)));
      if (name.startsWith(prefix) && !longerCurrentName && test.calls.some(c => c.name === current)) {
        name = "test" + current[0].toUpperCase() + current.slice(1) + name.slice(prefix.length);
        break;
      }
    }
    name = name.replace(/_([a-z])/g, (_, c) => "_" + c.toUpperCase());
    if (name !== test.name && used.has(test.className + "#" + name)) {
      // Preserve existing selectors when modernizing two historical names would collide.
      name = test.name;
    }
    const originalHeader = raw.slice(test.start, test.bodyStart);
    const updatedHeader = originalHeader.replace(new RegExp("\\b" + test.name + "\\s*\\("), name + "(")
      .replace(/^(\s*)(?!public\b)(void\s+)/m, "$1public $2");
    if (updatedHeader !== originalHeader) {
      raw = raw.slice(0, test.start) + updatedHeader + raw.slice(test.bodyStart);
      used.add(test.className + "#" + name);
      changes.push({ file: tools.relative(file.file), from: test.name, to: name });
    }
  }
  if (args.includes("--apply")) fs.writeFileSync(file.file, raw);
}
console.log(JSON.stringify({ renamedMethods: changes.length, changes: args.includes("--details") ? changes : undefined }, null, 2));
if (args.includes("--check") && changes.length) process.exitCode = 1;
