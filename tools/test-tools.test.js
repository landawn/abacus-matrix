"use strict";
const { test, after } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const tools = require("./test-tools");
const organizer = require("./organize-tests");

const temporaryRoot = fs.mkdtempSync(path.join(os.tmpdir(), "matrix-test-tools-"));
after(() => {
  assert.equal(path.dirname(path.resolve(temporaryRoot)), path.resolve(os.tmpdir()));
  assert.ok(path.basename(temporaryRoot).startsWith("matrix-test-tools-"));
  fs.rmSync(temporaryRoot, { recursive: true });
});

function fixture(name, source) {
  const file = path.join(temporaryRoot, name);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, source);
  return file;
}

test("Java parsing respects comments, literals, annotation arguments, and nested contexts", () => {
  const file = fixture("ParserTest.java", [
    "class ParserTest {",
    "// @Test void commentedOut() {}",
    "@Test void literal() {",
    "  String value = \"a  b /* not a comment */ }\";",
    "  // Don't let this apostrophe or } hide the next test.",
    "  assertEquals(\"a  b /* not a comment */ }\", value);",
    "}",
    "@Test @DisplayName(\"closing ) brace }\") void later() { assertEquals(2, 2); }",
    "@ParameterizedTest @ValueSource(strings = {\"a\", \"b\"}) void parameterized(String input) { assertNotNull(input); }",
    "@Nested class First { int state = 1; @Test void sameBody() { assertEquals(1, state); } }",
    "@Nested class Second { int state = 2; @Test void sameBody() { assertEquals(1, state); } }",
    "}",
  ].join("\n"));
  const tests = tools.allTests(tools.inventory([file]));
  assert.deepEqual(tests.map(t => t.name), ["literal", "later", "parameterized", "sameBody", "sameBody"]);
  assert.match(tests[0].normalized, /a  b \/\* not a comment \*\//);
  assert.equal(tests[2].parameters[0].type, "String");
  const duplicates = tools.duplicates(tests);
  assert.equal(duplicates.length, 1);
  assert.equal(duplicates[0].sameContext, false);
  assert.notEqual(duplicates[0].methods[0].className, duplicates[0].methods[1].className);
});

test("different parameter sources and string whitespace are not duplicates", () => {
  const file = fixture("ArgumentsTest.java", [
    "class ArgumentsTest {",
    "@ParameterizedTest @ValueSource(ints={1}) void first(int value) { assertEquals(1,value); }",
    "@ParameterizedTest @ValueSource(ints={2}) void second(int value) { assertEquals(1,value); }",
    "@Test void oneSpace() { assertEquals(\"a b\", actual()); }",
    "@Test void twoSpaces() { assertEquals(\"a  b\", actual()); }",
    "}",
  ].join("\n"));
  assert.deepEqual(tools.duplicates(tools.allTests(tools.inventory([file]))), []);
});

test("organizing refuses to broaden nested mutable fixtures or lifecycle hooks", () => {
  const source = fixture("src/main/Sample.java", "public class Sample { public int value() { return 1; } }");
  const file = fixture("src/test/SampleTest.java", [
    "class SampleTest extends TestBase {",
    "  @Nested class Stateful {",
    "    int state = 1;",
    "    @BeforeEach void setup() { state++; }",
    "    @Test void testValue() { assertEquals(2, state); }",
    "  }",
    "}",
  ].join("\n"));
  const before = fs.readFileSync(file, "utf8");
  assert.throws(() => organizer.plan(tools.inventory([source, file])), /Nested lifecycle|mutable fixture/);
  assert.equal(fs.readFileSync(file, "utf8"), before);
});

test("organizing is read-only by default, preserves bodies, and is idempotent", () => {
  const source = fixture("src/main/Plain.java", "public class Plain { public int value() { return 1; } public int other() { return 2; } }");
  const file = fixture("src/test/PlainTest.java", [
    "@Tag(\"base-test\") class PlainTest extends TestBase {",
    "  @Test public void testOther() { assertEquals(2, new Plain().other()); }",
    "  @Nested class Examples { @Test public void testValue() { assertEquals(1, new Plain().value()); } }",
    "}",
  ].join("\n"));
  const input = tools.inventory([source, file]);
  const original = fs.readFileSync(file, "utf8");
  const result = organizer.plan(input);
  assert.equal(fs.readFileSync(file, "utf8"), original);
  assert.equal(result.outputs.length, 1);
  assert.match(result.outputs[0].content, /@Tag\("base-test"\)/);
  fs.writeFileSync(file, result.outputs[0].content);
  const reorganized = tools.inventory([source, file]);
  assert.deepEqual(tools.allTests(reorganized).map(t => t.normalized).sort(), tools.allTests(input).map(t => t.normalized).sort());
  assert.equal(organizer.plan(reorganized).outputs.length, 0);
});

test("method grouping prefers complete current names over shorter legacy prefixes", () => {
  assert.equal(organizer.operation({ name: "testOfDiagonals", calls: [] }, ["wrap", "ofDiagonals"], "IntMatrix"), "ofDiagonals");
  assert.equal(organizer.operation({ name: "testGetRows_Empty", calls: [] }, ["get", "rowCount"], "IntMatrix"), "rowCount");
  assert.equal(organizer.operation({ name: "testRandom", calls: [{ name: "randomRow" }] }, ["random", "randomRow"], "IntMatrix"), "randomRow");
  assert.equal(organizer.operation({ name: "testDiagonalLU2RD", calls: [{ name: "ofMainDiagonal" }] }, ["mainDiagonalCopy", "ofMainDiagonal"], "IntMatrix"), "ofMainDiagonal");
});

test("relocated parameterized tests retain an explicit static provider owner", () => {
  const source = fixture("src/main/Parameterized.java", "public class Parameterized { public int value() { return 1; } }");
  const file = fixture("src/test/ParameterizedTest.java", [
    "class ParameterizedTest extends TestBase {",
    "  static java.util.stream.Stream<Integer> cases() { return java.util.stream.Stream.of(1); }",
    "  @org.junit.jupiter.params.ParameterizedTest @MethodSource(\"cases\")",
    "  public void testValue(int expected) { assertEquals(expected, new Parameterized().value()); }",
    "}",
  ].join("\n"));
  const output = organizer.plan(tools.inventory([source, file])).outputs[0].content;
  assert.match(output, /@MethodSource\("ParameterizedTest#cases"\)/);
  assert.match(output, /static java\.util\.stream\.Stream<Integer> cases/);
});

test("deduplication retains distinct inputs and records the stronger surviving case", () => {
  const source = fixture("src/main/Dedupe.java", "public class Dedupe { public int value(int input) { return input; } }");
  const file = fixture("src/test/DedupeTest.java", [
    "class DedupeTest extends TestBase {",
    "  @Test void testValue() { int value = new Dedupe().value(1); assertEquals(1, value); }",
    "  @Test void testValue_Stronger() { int value = new Dedupe().value(1); assertEquals(1, value); assertTrue(value > 0); }",
    "  @Test void testValue_OtherInput() { int value = new Dedupe().value(2); assertEquals(2, value); }",
    "}",
  ].join("\n"));
  const result = organizer.plan(tools.inventory([source, file]), { dedupe: true });
  assert.equal(result.removed.length, 1);
  assert.equal(result.removed[0].removed.method, "testValue");
  assert.equal(result.removed[0].retained.method, "testValue_Stronger");
  assert.match(result.outputs[0].content, /testValue_OtherInput/);
});
