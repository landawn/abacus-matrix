import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

/** Parse Java with the JDK compiler, without compiling or resolving project dependencies. */
public class JavaTestInventory {
    private static SourcePositions positions;

    public static void main(String[] args) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK is required to inspect Java tests");
        }
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, java.nio.charset.StandardCharsets.UTF_8)) {
            var inputs = manager.getJavaFileObjectsFromStrings(List.of(args));
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of("-proc:none"), null, inputs);
            var units = task.parse();
            positions = Trees.instance(task).getSourcePositions();
            var files = new ArrayList<Object>();
            for (var unit : units) {
                var file = new LinkedHashMap<String, Object>();
                file.put("file", Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize().toString().replace('\\', '/'));
                file.put("package", unit.getPackageName() == null ? "" : unit.getPackageName().toString());
                file.put("imports", unit.getImports().stream().map(Object::toString).map(String::trim).toList());
                Set<String> references = new LinkedHashSet<>();
                new TreeScanner<Void, Void>() {
                    @Override public Void visitImport(ImportTree tree, Void unused) { return null; }
                    @Override public Void visitIdentifier(IdentifierTree identifier, Void unused) {
                        references.add(identifier.getName().toString());
                        return super.visitIdentifier(identifier, unused);
                    }
                }.scan(unit, null);
                file.put("references", references);
                var types = new ArrayList<Object>();
                for (var declaration : unit.getTypeDecls()) {
                    if (declaration instanceof ClassTree type) {
                        types.add(describe(unit, type));
                    }
                }
                file.put("types", types);
                files.add(file);
            }
            var errors = diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).toList();
            if (!errors.isEmpty()) {
                errors.forEach(System.err::println);
                System.exit(1);
            }
            System.out.println(json(files));
        }
    }

    private static Map<String, Object> describe(CompilationUnitTree unit, Tree tree) {
        var result = new LinkedHashMap<String, Object>();
        long start = positions.getStartPosition(unit, tree);
        long end = positions.getEndPosition(unit, tree);
        result.put("kind", tree.getKind().name());
        result.put("start", start);
        result.put("end", end);
        result.put("line", unit.getLineMap().getLineNumber(start));
        if (tree instanceof ClassTree type) {
            result.put("name", type.getSimpleName().toString());
            result.put("annotations", annotations(type.getModifiers()));
            result.put("modifiers", type.getModifiers().getFlags().stream().map(Object::toString).sorted().toList());
            result.put("extends", type.getExtendsClause() == null ? "" : type.getExtendsClause().toString());
            result.put("members", type.getMembers().stream().map(member -> describe(unit, member)).toList());
        } else if (tree instanceof MethodTree method) {
            result.put("name", method.getName().toString());
            result.put("annotations", annotations(method.getModifiers()));
            result.put("modifiers", method.getModifiers().getFlags().stream().map(Object::toString).sorted().toList());
            result.put("parameters", method.getParameters().stream().map(p -> Map.of("name", p.getName().toString(), "type", p.getType().toString())).toList());
            result.put("returnType", method.getReturnType() == null ? "" : method.getReturnType().toString());
            result.put("signature", method.getName() + "(" + String.join(",", method.getParameters().stream().map(p -> p.getType().toString()).toList()) + ")");
            result.put("bodyStart", method.getBody() == null ? -1 : positions.getStartPosition(unit, method.getBody()));
            result.put("normalized", method.getBody() == null ? "" : method.getBody().toString());
            Set<String> identifiers = new LinkedHashSet<>();
            List<Object> calls = new ArrayList<>();
            List<Object> arrays = new ArrayList<>();
            new TreeScanner<Void, Void>() {
                @Override public Void visitIdentifier(IdentifierTree identifier, Void unused) {
                    identifiers.add(identifier.getName().toString());
                    return super.visitIdentifier(identifier, unused);
                }
                @Override public Void visitMethodInvocation(MethodInvocationTree call, Void unused) {
                    Tree select = call.getMethodSelect();
                    String name = select instanceof MemberSelectTree member ? member.getIdentifier().toString() : select.toString();
                    String receiver = select instanceof MemberSelectTree member ? member.getExpression().toString() : "";
                    calls.add(Map.of("name", name, "receiver", receiver, "arity", call.getArguments().size(), "arguments", call.getArguments().stream().map(Object::toString).toList()));
                    return super.visitMethodInvocation(call, unused);
                }
                @Override public Void visitNewClass(NewClassTree call, Void unused) {
                    calls.add(Map.of("name", "<init>", "receiver", call.getIdentifier().toString().split("<")[0], "arity", call.getArguments().size(),
                            "arguments", call.getArguments().stream().map(Object::toString).toList()));
                    return super.visitNewClass(call, unused);
                }
                @Override public Void visitMemberReference(MemberReferenceTree reference, Void unused) {
                    calls.add(Map.of("name", reference.getName().toString(), "receiver", reference.getQualifierExpression().toString(), "arity", -1, "arguments", List.of()));
                    return super.visitMemberReference(reference, unused);
                }
                @Override public Void visitNewArray(NewArrayTree array, Void unused) {
                    if (array.getType() != null) {
                        var rows = array.getInitializers();
                        arrays.add(Map.of("type", array.getType().toString(), "dimensions", array.getDimensions().stream().map(Object::toString).toList(),
                                "rowLengths", rows == null ? List.of() : rows.stream().map(row -> row instanceof NewArrayTree a && a.getInitializers() != null
                                        ? a.getInitializers().size() : -1).toList(), "length", rows == null ? -1 : rows.size()));
                    }
                    return super.visitNewArray(array, unused);
                }
            }.scan(method.getBody(), null);
            result.put("identifiers", identifiers);
            result.put("calls", calls);
            result.put("arrays", arrays);
        } else {
            if (tree instanceof VariableTree field) {
                result.put("name", field.getName().toString());
            }
            result.put("normalized", tree.toString());
        }
        return result;
    }

    private static List<String> annotations(ModifiersTree modifiers) {
        return modifiers.getAnnotations().stream().map(Object::toString).toList();
    }

    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            return "{" + String.join(",", map.entrySet().stream().map(e -> json(e.getKey().toString()) + ":" + json(e.getValue())).toList()) + "}";
        }
        if (value instanceof Iterable<?> iterable) {
            var items = new ArrayList<String>();
            iterable.forEach(item -> items.add(json(item)));
            return "[" + String.join(",", items) + "]";
        }
        var escaped = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 32) escaped.append(String.format("\\u%04x", (int) c));
                    else escaped.append(c);
                }
            }
        }
        return escaped.append('"').toString();
    }
}
