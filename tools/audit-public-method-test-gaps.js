#!/usr/bin/env node
"use strict";
const tools = require("./test-tools");
const files = tools.inventory();
const tests = tools.allTests(files.filter(f => f.file.includes("/src/test/")));
const sources = files.filter(f => f.file.includes("/src/main/")).flatMap(file => file.types.map(type => ({ file, type })));
const results = sources.filter(({ type }) => type.modifiers.includes("public")).map(({ file, type }) => {
  const declarations = [{ file, type }];
  let parent = type.extends.split("<")[0];
  while (parent) {
    const found = sources.find(s => s.type.name === parent);
    if (!found || declarations.some(d => d.type === found.type)) break;
    declarations.push(found);
    parent = found.type.extends.split("<")[0];
  }
  const signatures = new Set();
  const methods = declarations.flatMap(owner => owner.type.members.filter(m => m.kind === "METHOD" && m.modifiers.includes("public")
    && (owner.type === type || m.name !== "<init>")).map(method => ({ owner, method }))).filter(({ method }) => {
      if (signatures.has(method.signature)) return false;
      signatures.add(method.signature);
      return true;
    });
  const candidates = methods.map(({ owner, method }) => {
    const references = tests.filter(test => test.calls.some(call => call.name === method.name
      && (call.arity === method.parameters.length || call.arity === -1)
      && (call.receiver === type.name || test.file.endsWith("/" + type.name + "Test.java")
        || test.identifiers.includes(type.name))));
    return { signature: method.signature, declaredIn: owner.type.name, line: method.line,
      candidateTestCount: references.length, candidates: references.slice(0, 3).map(tools.location) };
  });
  return { typeName: type.name, sourceFile: tools.relative(file.file), publicMethodCount: methods.length,
    methods: candidates, withoutDirectReferences: candidates.filter(m => m.candidateTestCount === 0) };
});
if (!results.length) throw new Error("No public matrix classes found; check the source root");
console.log(JSON.stringify({ classCount: results.length,
  note: "Signature/arity references are candidates, not measured coverage. Overloads with equal arity require review; use JaCoCo and behavioral assertions to confirm gaps.",
  totalWithoutDirectReferences: results.reduce((n, r) => n + r.withoutDirectReferences.length, 0), results }, null, 2));
