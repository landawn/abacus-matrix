"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { execFileSync } = require("node:child_process");

const repoRoot = path.resolve(__dirname, "..");
const sourceRoot = path.join(repoRoot, "src/main/java/com/landawn/abacus/matrix");
const testRoot = path.join(repoRoot, "src/test/java/com/landawn/abacus/matrix");
const testAnnotation = /^@(?:[\w.]+\.)?(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b/;
const lifecycleAnnotation = /^@(?:[\w.]+\.)?(?:BeforeEach|AfterEach|BeforeAll|AfterAll)\b/;

function javaFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const file = path.join(directory, entry.name);
    return entry.isDirectory() ? javaFiles(file) : entry.name.endsWith(".java") ? [file] : [];
  }).sort();
}

function inventory(files = [...javaFiles(sourceRoot), ...javaFiles(testRoot)]) {
  if (!files.length) throw new Error("No Java files selected; refusing to report a successful empty audit");
  return JSON.parse(execFileSync("java", [path.join(__dirname, "JavaTestInventory.java"), ...files], {
    cwd: repoRoot, encoding: "utf8", maxBuffer: 128 * 1024 * 1024, windowsHide: true,
  }));
}

function isTest(member) {
  return member.kind === "METHOD" && member.annotations.some(a => testAnnotation.test(a));
}

function isLifecycle(member) {
  return member.kind === "METHOD" && member.annotations.some(a => lifecycleAnnotation.test(a));
}

function allTests(files) {
  const result = [];
  function visit(file, type, ancestors) {
    const chain = [...ancestors, type];
    for (const member of type.members) {
      if (isTest(member)) result.push({ ...member, file: file.file, className: chain.map(t => t.name).join("."), chain });
      else if (member.members && member.annotations.some(a => /^@(?:[\w.]+\.)?Nested\b/.test(a))) visit(file, member, chain);
    }
  }
  for (const file of files) for (const type of file.types) visit(file, type, []);
  return result;
}

function relative(file) {
  return path.relative(repoRoot, file).replaceAll("\\", "/");
}

function location(test) {
  return { file: relative(test.file), className: test.className, method: test.name, signature: test.signature, line: test.line };
}

// Bodies alone cannot establish equivalence: fields, helpers, hooks, and annotations may change behavior.
function contextKey(test) {
  return JSON.stringify({
    file: test.file,
    annotations: test.annotations,
    contexts: test.chain.map(type => ({
      annotations: type.annotations.filter(a => !/^@(?:Nested|Tag\("(?:2025|2510|2511|2512)"\))$/.test(a)),
      extends: type.extends,
      members: type.members.filter(m => !isTest(m) && !m.members).map(m => m.normalized || m.signature),
    })).filter(c => c.annotations.length || c.members.length),
  });
}

function duplicates(tests) {
  const groups = new Map();
  for (const test of tests) {
    const key = JSON.stringify([test.parameters, test.annotations, test.normalized]);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(test);
  }
  return [...groups.values()].filter(group => group.length > 1).map(group => ({
    scope: new Set(group.map(t => t.file)).size > 1 ? "cross-file" : "same-file",
    sameContext: new Set(group.map(contextKey)).size === 1,
    methods: group.map(location),
  }));
}

module.exports = { repoRoot, sourceRoot, testRoot, javaFiles, inventory, isTest, isLifecycle, allTests, relative, location, contextKey, duplicates };
