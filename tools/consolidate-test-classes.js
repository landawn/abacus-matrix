#!/usr/bin/env node
"use strict";
const fs = require("node:fs");
const path = require("node:path");
const tools = require("./test-tools");

function main(args = process.argv.slice(2)) {
  const rules = JSON.parse(fs.readFileSync(path.join(__dirname, "test-ownership.json"), "utf8"));
  const files = tools.inventory(tools.javaFiles(tools.testRoot));
  const changes = new Map();
  const moved = [];
  const deleted = [];
  const text = file => fs.readFileSync(file.file, "utf8");
  const indent = (file, member, spaces) => {
    const raw = text(file);
    const start = raw.lastIndexOf("\n", member.start - 1) + 1;
    const oldIndent = member.start - start;
    return raw.slice(member.start, member.end).replace(/\r\n/g, "\n").split("\n")
      .map((line, index) => line.trim() ? " ".repeat(spaces) + (index ? line.slice(Math.min(oldIndent, line.search(/\S|$/))) : line) : "").join("\n");
  };
  for (const [sourceName, rule] of Object.entries(rules)) {
    const file = files.find(f => f.file.endsWith("/" + sourceName + ".java"));
    if (!file) continue;
    const type = file.types[0];
    if (type.members.some(m => m.members && m.annotations.includes("@Nested"))) throw new Error("Review nested source before migration: " + sourceName);
    const buckets = new Map();
    for (const test of type.members.filter(tools.isTest)) {
      const destination = rule.methods?.[test.name] || rule.default;
      if (!destination) throw new Error("No owner for " + sourceName + "." + test.name);
      const [targetName, group] = destination.split("/");
      const key = targetName + "/" + group;
      if (!buckets.has(key)) buckets.set(key, []);
      buckets.get(key).push(test);
      moved.push({ from: sourceName + "." + test.name, to: key + "." + test.name });
    }
    for (const [destination, selected] of buckets) {
      const [targetName, group] = destination.split("/");
      if (!changes.has(targetName)) changes.set(targetName, { imports: new Set(), groups: [] });
      const output = changes.get(targetName);
      file.imports.filter(i => i !== "import org.junit.jupiter.api.Tag;").forEach(i => output.imports.add(i));
      // Include only helpers reachable from selected tests, recursively, keeping helper scope intact.
      const helpers = type.members.filter(m => !tools.isTest(m));
      const required = new Set(selected.flatMap(m => m.identifiers));
      const included = new Set();
      let again = true;
      while (again) {
        again = false;
        for (const helper of helpers) {
          if (included.has(helper) || (!required.has(helper.name) && !tools.isLifecycle(helper))) continue;
          included.add(helper);
          (helper.identifiers || []).forEach(id => required.add(id));
          // A field initializer may reference another helper or constant.
          for (const possible of helpers) if (helper.normalized?.includes(possible.name)) required.add(possible.name);
          again = true;
        }
      }
      const members = [...included, ...selected].map(m => indent(file, m, 8));
      output.groups.push("    @Nested\n    class " + group + " {\n\n" + members.join("\n\n") + "\n    }");
    }
    deleted.push(file.file);
  }
  const outputs = [];
  for (const [name, change] of changes) {
    const existing = files.find(f => f.file.endsWith("/" + name + ".java"));
    const imports = change.imports;
    imports.add("import org.junit.jupiter.api.Nested;");
    imports.add("import com.landawn.abacus.TestBase;");
    let body;
    if (existing) {
      existing.imports.forEach(i => imports.add(i));
      const raw = text(existing);
      body = raw.slice(existing.types[0].start, existing.types[0].end - 1).trimEnd() + "\n\n" + change.groups.join("\n\n") + "\n}\n";
    } else {
      body = "/** Shared contracts intentionally exercised across matrix implementations. */\nclass " + name + " extends TestBase {\n\n" + change.groups.join("\n\n") + "\n}\n";
    }
    outputs.push({ file: path.join(tools.testRoot, name + ".java"), content: "package com.landawn.abacus.matrix;\n\n" + [...imports].sort().join("\n") + "\n\n" + body });
  }
  const report = { moved, deleted: deleted.map(tools.relative), changedFiles: outputs.map(o => tools.relative(o.file)) };
  const reportPath = args.find(a => a.startsWith("--report="))?.slice(9);
  if (reportPath) fs.writeFileSync(reportPath, JSON.stringify(report, null, 2) + "\n");
  if (args.includes("--apply")) {
    for (const output of outputs) fs.writeFileSync(output.file, output.content);
    for (const file of deleted) {
      if (path.dirname(path.resolve(file)) !== path.resolve(tools.testRoot)) throw new Error("Deletion outside matrix test directory");
      fs.unlinkSync(file);
    }
  }
  console.log(JSON.stringify(report, null, 2));
}
if (require.main === module) main();
module.exports = { main };
