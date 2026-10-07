// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.ai.m3scale.M3LeafAtom;
import com.synexia.ai.m3scale.M3LeafAtomizer;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Executed contracts over the existing statement, block and loop owners; no replacement engine. */
final class M3FunctionalJavaProofTest {
    private static final String PATH = "src/main/java/example/Example.java";
    private static final String SOURCE = """
            package example;
            import java.util.List;
            public final class Example {
                @FunctionalInterface
                interface Normalizer { String apply(String value); }
                public static java.util.function.Function<String, String> external = value -> value;
                private static final java.util.function.Function<String, String> trim = value -> { return value.trim(); };
                public static String trace(List<String> values) {
                    StringBuilder result = new StringBuilder();
                    final String prefix = "!";
                    final java.util.function.Function<String, String> stringifier = new java.util.function.Function<String, String>() {
                        public String apply(String item) { return String.valueOf(item); }
                    };
                    final Normalizer normalizer = stringifier::apply;
                    for (String value : values) {
                        result.append(external.apply(prefix + trim.apply(normalizer.apply(value)))).append('|');
                    }
                    return result.toString();
                }
            }
            """;

    @TempDir Path temporary;

    @Test
    void inventoryCompositionExecutesWithSourceCoverageAndStableTables() {
        RecipeRun first = execute(inventoryComposition(), SOURCE, PATH);
        RecipeRun second = execute(inventoryComposition(), SOURCE, PATH);
        assertTrue(first.getChangeset().getAllResults().isEmpty());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
        assertEquals(tableRows(first), tableRows(second), "Stable M3 evidence tables");
        List<M3RecipeFirstSourceCoverageTable.Row> coverage = first.getDataTables().values().stream()
                .flatMap(List::stream)
                .filter(M3RecipeFirstSourceCoverageTable.Row.class::isInstance)
                .map(M3RecipeFirstSourceCoverageTable.Row.class::cast).toList();
        assertEquals(1, coverage.size());
        assertEquals(PATH, coverage.getFirst().sourcePath());
        assertEquals(M3RecipeFirstSourceCoverageTable.Disposition.JAVA_DECLARATIONS_MAPPED,
                coverage.getFirst().disposition());
        assertFalse(coverage.getFirst().executedContractProof());
        assertFalse(coverage.getFirst().promotionAuthority());
    }

    @Test
    void statementLeavesPreserveExactSourceAndEnclosingMemberContext() {
        List<M3LeafAtom> leaves = M3LeafAtomizer.analyze(
                "example", PATH, SOURCE.getBytes(StandardCharsets.UTF_8), 1024 * 1024);
        assertTrue(leaves.stream().anyMatch(atom -> atom.kind() == M3LeafAtom.Kind.EXPRESSION));
        int end = 0;
        for (M3LeafAtom atom : leaves) {
            assertTrue(atom.startChar() >= end);
            assertEquals("Example", atom.owner());
            assertTrue(atom.contextKey().contains(atom.memberContractRoot()));
            assertEquals(SOURCE.substring(atom.startChar(), atom.endChar()), atom.sourceText());
            end = atom.endChar();
        }
        assertEquals(leaves, M3LeafAtomizer.analyze(
                "example", PATH, SOURCE.getBytes(StandardCharsets.UTF_8), 1024 * 1024));
        assertThrows(IllegalArgumentException.class, () -> M3LeafAtomizer.analyze(
                "example", PATH, SOURCE.getBytes(StandardCharsets.UTF_8), 1));
        assertThrows(IllegalArgumentException.class, () -> M3LeafAtomizer.analyze(
                "example", PATH, new byte[] {(byte) 0xff}, 1024));
    }

    @Test
    void existingLoopPatternRewritesToFixedPointAndPreservesExecutedEffects() throws Exception {
        String after = output(execute(functionalPipeline(), SOURCE, PATH), SOURCE);
        assertFalse(SOURCE.equals(after));
        Files.writeString(Path.of(System.getProperty("java.io.tmpdir")).resolve("functional-pipeline.java"), after);
        assertTrue(after.contains("forEachOrdered"));
        Path beforeClasses = compile(SOURCE, "before");
        Path afterClasses = compile(after, "after");
        String normalForm = oneFunctionalAtomFixture(after);
        Path atomClasses = compile(normalForm, "one-functional-atom");
        Files.writeString(Path.of(System.getProperty("java.io.tmpdir")).resolve("functional-one-atom.java"), normalForm);
        assertTrue(execute(functionalPipeline(), after, PATH)
                .getChangeset().getAllResults().isEmpty());
        try (URLClassLoader before = loader(beforeClasses); URLClassLoader transformed = loader(afterClasses); URLClassLoader atomLoader = loader(atomClasses)) {
            var beforeType = before.loadClass("example.Example");
            var afterType = transformed.loadClass("example.Example");
            var atomType = atomLoader.loadClass("example.Example");
            var atomMethod = atomType.getMethod("trace", List.class);
            var atomMock = externalMock();
            atomType.getField("external").set(null, atomMock);
            var original = beforeType.getMethod("trace", List.class);
            var beforeMock = externalMock();
            var afterMock = externalMock();
            beforeType.getField("external").set(null, beforeMock);
            afterType.getField("external").set(null, afterMock);
            var candidate = transformed.loadClass("example.Example").getMethod("trace", List.class);
            List<List<String>> cases = List.of(List.of(), List.of("a"), List.of("", "a", "a", "\u03bb", " \u2003 "),
                    java.util.Arrays.asList("a", null, "b"));
            for (List<String> values : cases) {
                org.mockito.Mockito.clearInvocations((Object) beforeMock, (Object) afterMock);
                var expectedArguments = values.stream().map(v -> "!" + String.valueOf(v).trim()).toList();
                var expected = expectedArguments.stream().map(v -> v + "|").collect(java.util.stream.Collectors.joining());
                assertEquals(expected, original.invoke(null, values));
                assertEquals(expected, candidate.invoke(null, values));
                org.mockito.Mockito.clearInvocations((Object) atomMock);
                assertEquals(expected, atomMethod.invoke(null, values));
                assertEquals(expectedArguments, externalArguments(atomMock));
                assertEquals(expectedArguments, externalArguments(beforeMock));
                assertEquals(expectedArguments, externalArguments(afterMock));
            }
            org.mockito.Mockito.clearInvocations((Object) beforeMock, (Object) afterMock);
            var beforeFailure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> original.invoke(null, (Object) null));
            var afterFailure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> candidate.invoke(null, (Object) null));
            assertEquals(beforeFailure.getCause().getClass(), afterFailure.getCause().getClass());
            org.mockito.Mockito.clearInvocations((Object) atomMock);
            var atomFailure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> atomMethod.invoke(null, (Object) null));
            assertEquals(beforeFailure.getCause().getClass(), atomFailure.getCause().getClass());
            org.mockito.Mockito.verifyNoInteractions(beforeMock, afterMock, atomMock);
        }
    }

    @Test
    void scopedLoopRecipeRefusesOtherFilesAndUnsafeControlFlow() {
        assertTrue(execute(new M3JsparrowLoopStreamRecipe("other/**/*.java", "ALL"), SOURCE, PATH)
                .getChangeset().getAllResults().isEmpty());
        String unsafe = SOURCE.replace("result.append(external.apply(prefix + trim.apply(normalizer.apply(value)))).append('|');",
                "if (value.isEmpty()) continue; result.append(external.apply(prefix + trim.apply(normalizer.apply(value)))).append('|');");
        assertTrue(execute(new M3JsparrowLoopStreamRecipe(PATH, "FOR_EACH"), unsafe, PATH)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void takeWhileAndFlatMapReuseTheSameBlockPrinterAndReachFixedPoint() throws Exception {
        String takeWhile = SOURCE.replace("result.append(external.apply(prefix + trim.apply(normalizer.apply(value)))).append('|');",
                "if (value.isEmpty()) break; result.append(external.apply(prefix + trim.apply(normalizer.apply(value)))).append('|');");
        verifyCompiledPattern(takeWhile, "TAKE_WHILE", "takewhile");
        String flatMap = SOURCE.replace("List<String> values", "List<List<String>> values")
                .replace("for (String value : values) {", "for (List<String> group : values) { for (String value : group) {")
                .replace("return result.toString();", "} return result.toString();");
        verifyCompiledPattern(flatMap, "FLAT_MAP", "flatmap");
    }

    @Test
    void upstreamTypedMethodRecipeHandlesStatementsInsideExistingBlocksAndLoops() throws Exception {
        String source = SOURCE.replace("result.append(external.apply(prefix + trim.apply(normalizer.apply(value)))).append('|');", "append(result, value);")
                .replace("public static String trace", "private static void append(StringBuilder out, String value) { out.append(value).append('|'); }\n    public static String trace");
        var recipe = new org.openrewrite.java.ChangeMethodName("example.Example append(..)", "appendAtom", false, false);
        String after = output(execute(recipe, source, PATH), source);
        assertTrue(after.contains("appendAtom(result, value)"));
        assertFalse(after.contains("append(result, value)"));
        compile(source, "typed-before");
        compile(after, "typed-after");
        assertTrue(execute(new org.openrewrite.java.ChangeMethodName(
                "example.Example append(..)", "appendAtom", false, false), after, PATH)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void existingLoopBlockPreservesClosingCommentsAndBareBodiesCompile() throws Exception {
        String commented = SOURCE.replace("result.append(external.apply(prefix + trim.apply(normalizer.apply(value)))).append('|');",
                "/* before effect */ result.append(external.apply(prefix + trim.apply(normalizer.apply(value)))).append('|');\n            /* closing loop note */");
        String after = output(execute(new M3JsparrowLoopStreamRecipe(PATH, "FOR_EACH"), commented, PATH), commented);
        assertTrue(after.contains("before effect"));
        assertTrue(after.contains("closing loop note"));
        compile(after, "comments");
        String bare = """
                package example;
                import java.util.List;
                public final class Example {
                    public static String trace(List<String> values) {
                        StringBuilder result = new StringBuilder();
                        for (String value : values) result.append(value).append('|');
                        return result.toString();
                    }
                }
                """;
        verifyCompiledPattern(bare, "FOR_EACH", "bare");
    }

    private void verifyCompiledPattern(String source, String mode, String name) throws Exception {
        String after = output(execute(new M3JsparrowLoopStreamRecipe(PATH, mode), source, PATH), source);
        assertFalse(source.equals(after));
        compile(source, name + "-before");
        compile(after, name + "-after");
        assertTrue(execute(new M3JsparrowLoopStreamRecipe(PATH, mode), after, PATH)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void cancellationStopsInventoryBeforeFileTablesAreAdmitted() {
        var ctx = context();
        ctx.putMessage(M3RecipeFirstJavaAtomInventoryRecipe.CANCELLATION_KEY,
                (BooleanSupplier) () -> true);
        var source = parse(SOURCE, PATH);
        var failure = assertThrows(org.openrewrite.internal.RecipeRunException.class,
                () -> new M3RecipeFirstJavaAtomInventoryRecipe().getVisitor().visit(source, ctx));
        assertTrue(failure.getCause() instanceof CancellationException);
    }


    @SuppressWarnings("unchecked")
    private static java.util.function.Function<String, String> externalMock() {
        var result = (java.util.function.Function<String, String>) org.mockito.Mockito.mock(java.util.function.Function.class);
        org.mockito.Mockito.doAnswer(call -> call.getArgument(0)).when(result).apply(org.mockito.ArgumentMatchers.any());
        return result;
    }

    private static List<String> externalArguments(java.util.function.Function<String, String> callback) {
        return org.mockito.Mockito.mockingDetails(callback).getInvocations().stream()
                .map(call -> (String) call.getArguments()[0]).toList();
    }

    private static String oneFunctionalAtomFixture(String source) {
        String method = "public static String trace(List<String> values) {";
        String tail = "return result.toString();\n    }\n}";
        assertEquals(1, source.split(java.util.regex.Pattern.quote(method), -1).length - 1);
        assertEquals(1, source.split(java.util.regex.Pattern.quote(tail), -1).length - 1);
        String fixture = source.replace(method,
                "private static final java.util.function.Function<List<String>, String> TRACE = values -> {")
                .replace(tail, "return result.toString();\n    };\n"
                        + "    public static String trace(List<String> values) { return TRACE.apply(values); }\n}");
        var parsed = JavaParser.fromJavaVersion().build().parse(context(), fixture).findFirst().orElseThrow();
        var wrapper = ((org.openrewrite.java.tree.J.CompilationUnit) parsed).getClasses().getFirst().getBody()
                .getStatements().stream().filter(org.openrewrite.java.tree.J.MethodDeclaration.class::isInstance)
                .map(org.openrewrite.java.tree.J.MethodDeclaration.class::cast)
                .filter(m -> m.getSimpleName().equals("trace")).findFirst().orElseThrow();
        assertEquals(1, java.util.Objects.requireNonNull(wrapper.getBody()).getStatements().size());
        var returned = (org.openrewrite.java.tree.J.Return) wrapper.getBody().getStatements().getFirst();
        var invoked = (org.openrewrite.java.tree.J.MethodInvocation) returned.getExpression();
        assertEquals("apply", invoked.getSimpleName());
        assertEquals("TRACE", ((org.openrewrite.java.tree.J.Identifier) invoked.getSelect()).getSimpleName());
        return fixture;
    }

    @Test
    void callbackFailureStopsAtSameElementInOriginalStreamAndFunctionalAtom() throws Exception {
        String stream = output(execute(functionalPipeline(), SOURCE, PATH), SOURCE);
        int index = 0;
        for (String source : List.of(SOURCE, stream, oneFunctionalAtomFixture(stream))) {
            Path classes = compile(source, "callback-failure-" + index++);
            try (var isolated = loader(classes)) {
                var callback = externalMock();
                var failure = new IllegalStateException("callee failed");
                org.mockito.Mockito.doAnswer(call -> {
                    String value = call.getArgument(0);
                    if (value.equals("!stop")) { throw failure; }
                    return value;
                }).when(callback).apply(org.mockito.ArgumentMatchers.any());
                var type = isolated.loadClass("example.Example");
                type.getField("external").set(null, callback);
                var thrown = assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> type.getMethod("trace", List.class).invoke(null, List.of("a", " stop ", "c")));
                assertEquals(failure, thrown.getCause());
                assertEquals(List.of("!a", "!stop"), externalArguments(callback));
            }
        }
    }

    private static Recipe functionalPipeline() {
        return new Recipe() {
            @Override public String getDisplayName() { return "Native functional Java proof composition"; }
            @Override public String getDescription() { return "Compose unchanged functional and stream recipes for a bounded executed fixture."; }
            @Override public List<Recipe> getRecipeList() {
                return List.of(new M3JsparrowLoopStreamRecipe(PATH, "FOR_EACH"),
                        new M3JsparrowExistingOpenRewriteRecipe("FunctionalInterfaceRule"),
                        new M3JsparrowExistingOpenRewriteRecipe("StatementLambdaToExpressionRule"),
                        new M3JsparrowExistingOpenRewriteRecipe("LambdaToMethodReferenceRule"));
            }
        };
    }

    @Test
    void nativeFunctionalRecipesActuallyChangeEachForm() throws Exception {
        String lambda = output(execute(new org.openrewrite.staticanalysis.UseLambdaForFunctionalInterface(), SOURCE, PATH), SOURCE);
        assertFalse(lambda.contains("new java.util.function.Function<String, String>()"));
        assertFalse(lambda.equals(SOURCE));
        String expression = output(execute(new org.openrewrite.staticanalysis.LambdaBlockToExpression(), SOURCE, PATH), SOURCE);
        assertFalse(expression.equals(SOURCE));
        String reference = output(execute(new org.openrewrite.staticanalysis.ReplaceLambdaWithMethodReference(), expression, PATH), expression);
        assertTrue(reference.contains("String::trim"));
        assertFalse(reference.equals(expression));
        assertTrue(execute(new org.openrewrite.staticanalysis.ReplaceLambdaWithMethodReference(), reference, PATH)
                .getChangeset().getAllResults().isEmpty());
        Files.writeString(Path.of(System.getProperty("java.io.tmpdir")).resolve("functional-lambda.java"), lambda);
        Files.writeString(Path.of(System.getProperty("java.io.tmpdir")).resolve("functional-expression.java"), expression);
        Files.writeString(Path.of(System.getProperty("java.io.tmpdir")).resolve("functional-reference.java"), reference);
    }

    @Test
    void plausibleLazyAndCaptureMutantsAreDetected() throws Exception {
        String after = output(execute(functionalPipeline(), SOURCE, PATH), SOURCE);
        String lazy = after.replace(".forEachOrdered(", ".peek(");
        String capture = after.replace("\"!\"", "\"?\"");
        assertFalse(lazy.equals(after));
        assertFalse(capture.equals(after));
        Path lazyClasses = compile(lazy, "lazy-mutant");
        Path captureClasses = compile(capture, "capture-mutant");
        try (var lazyLoader = loader(lazyClasses); var captureLoader = loader(captureClasses)) {
            var values = List.of(" a ", "b");
            assertFalse("!a|!b|".equals(lazyLoader.loadClass("example.Example").getMethod("trace", List.class).invoke(null, values)));
            assertFalse("!a|!b|".equals(captureLoader.loadClass("example.Example").getMethod("trace", List.class).invoke(null, values)));
        }
    }


    private static Recipe inventoryComposition() {
        return new Recipe() {
            @Override public String getDisplayName() { return "Focused existing inventory composition"; }
            @Override public String getDescription() { return "Exercises existing read-only leaves in the focused fixture."; }
            @Override public List<Recipe> getRecipeList() {
                return List.of(new M3RecipeFirstJavaAtomInventoryRecipe(),
                        new M3JavaPatternInventoryRecipe(), new M3FrameworkRecipePatternReviewRecipe());
            }
        };
    }
    private static String tortureFile() {
        String begin = "public final class Example {";
        String body = SOURCE.substring(SOURCE.indexOf(begin) + begin.length(), SOURCE.lastIndexOf('}'));
        String effect = "result.append(external.apply(prefix + trim.apply(normalizer.apply(value)))).append('|');";
        String loop = "for (String value : values) {";
        StringBuilder file = new StringBuilder("package example;\nimport java.util.List;\npublic final class Example {\n");
        int id = 0;
        for (int branch = 0; branch < 3; branch++) {
            for (int iteration = 0; iteration < 3; iteration++) {
                for (int callable = 0; callable < 3; callable++) {
                    String leaf = body;
                    if (branch == 1) {
                        leaf = leaf.replace(effect, "if (value == null || !value.isBlank()) { " + effect + " }");
                    } else if (branch == 2) {
                        leaf = leaf.replace(effect, "final String selected = value == null ? \"NULL\" : value; "
                                + effect.replace("normalizer.apply(value)", "normalizer.apply(selected)"));
                    }
                    if (iteration == 1) {
                        leaf = leaf.replace(loop, "for (int index = 0; index < values.size(); index++) { String value = values.get(index);");
                    } else if (iteration == 2) {
                        leaf = leaf.replace(loop, "int index = 0; while (index < values.size()) { String value = values.get(index++);");
                    }
                    String anonymous = "new java.util.function.Function<String, String>() {\n"
                            + "            public String apply(String item) { return String.valueOf(item); }\n"
                            + "        }";
                    assertTrue(leaf.contains(anonymous));
                    if (callable == 1) {
                        leaf = leaf.replace(anonymous, "item -> { return String.valueOf(item); }");
                    } else if (callable == 2) {
                        leaf = leaf.replace(anonymous, "String::valueOf");
                    }
                    file.append("public static final class Variant").append(id++).append(" {").append(leaf).append("}\n");
                }
            }
        }
        file.append("public static String traceAll(List<String> values) { StringBuilder result = new StringBuilder();\n");
        for (int i = 0; i < id; i++) file.append("result.append(Variant").append(i).append(".trace(values));\n");
        return file.append("return result.toString(); }\n}\n").toString();
    }

    @Test
    void oneTortureFileCombinationsPreserveBehaviorThroughRecipePermutations() throws Exception {
        String before = tortureFile();
        Files.writeString(Path.of(System.getProperty("java.io.tmpdir")).resolve("functional-torture-before.java"), before);
        Path originalClasses = compile(before, "torture-before");
        String[] rules = {"FunctionalInterfaceRule", "StatementLambdaToExpressionRule", "LambdaToMethodReferenceRule"};
        int[][] orders = {{0,1,2}, {0,2,1}, {1,0,2}, {1,2,0}, {2,0,1}, {2,1,0}};
        List<List<String>> cases = List.of(List.of(), List.of(" a ", "b"), List.of("", "a", "a", "\u03bb", " \u2003 "),
                java.util.Arrays.asList("a", null, "b"));
        try (var original = loader(originalClasses)) {
            for (int order = 0; order < orders.length; order++) {
                String current = before;
                boolean fixed = false;
                for (int cycle = 0; cycle < 6; cycle++) {
                    String input = current;
                    current = output(execute(new M3JsparrowLoopStreamRecipe(PATH, "FOR_EACH"), current, PATH), current);
                    for (int next : orders[order]) {
                        current = output(execute(new M3JsparrowExistingOpenRewriteRecipe(rules[next]), current, PATH), current);
                    }
                    if (input.equals(current)) { fixed = true; break; }
                }
                assertTrue(fixed, "Six-cycle bounded convergence, recipe order " + order);
                assertFalse(before.equals(current));
                Files.writeString(Path.of(System.getProperty("java.io.tmpdir")).resolve("functional-torture-after-" + order + ".java"), current);
                Path candidateClasses = compile(current, "torture-after-" + order);
                try (var transformed = loader(candidateClasses)) {
                    for (int variant = 0; variant < 27; variant++) {
                        String typeName = "example.Example$Variant" + variant;
                        var beforeType = original.loadClass(typeName);
                        var afterType = transformed.loadClass(typeName);
                        var beforeMock = externalMock();
                        var afterMock = externalMock();
                        beforeType.getField("external").set(null, beforeMock);
                        afterType.getField("external").set(null, afterMock);
                        var beforeMethod = beforeType.getMethod("trace", List.class);
                        var afterMethod = afterType.getMethod("trace", List.class);
                        for (List<String> values : cases) {
                            org.mockito.Mockito.clearInvocations((Object) beforeMock, (Object) afterMock);
                            assertEquals(beforeMethod.invoke(null, values), afterMethod.invoke(null, values));
                            assertEquals(externalArguments(beforeMock), externalArguments(afterMock));
                        }
                        org.mockito.Mockito.clearInvocations((Object) beforeMock, (Object) afterMock);
                        var a = assertThrows(java.lang.reflect.InvocationTargetException.class,
                                () -> beforeMethod.invoke(null, (Object) null));
                        var b = assertThrows(java.lang.reflect.InvocationTargetException.class,
                                () -> afterMethod.invoke(null, (Object) null));
                        assertEquals(a.getCause().getClass(), b.getCause().getClass());
                        org.mockito.Mockito.verifyNoInteractions(beforeMock, afterMock);
                    }
                    var input = List.of("a", " b ");
                    assertEquals(original.loadClass("example.Example").getMethod("traceAll", List.class).invoke(null, input),
                            transformed.loadClass("example.Example").getMethod("traceAll", List.class).invoke(null, input));
                }
            }
        }
    }

    @Test
    void independentInventoryFanOutHasDeterministicVerifiedFanIn() throws Exception {
        String source = tortureFile();
        var owners = List.<Recipe>of(new M3RecipeFirstJavaAtomInventoryRecipe(),
                new M3JavaPatternInventoryRecipe(), new M3FrameworkRecipePatternReviewRecipe());
        var serial = owners.stream().map(owner -> tableRows(execute(owner, source, PATH))).toList();
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = owners.stream().map(owner -> executor.submit(() -> {
                var run = execute(owner, source, PATH);
                assertTrue(run.getChangeset().getAllResults().isEmpty());
                return tableRows(run);
            })).toList();
            var fanIn = new java.util.ArrayList<List<String>>();
            for (var task : tasks) fanIn.add(task.get(90, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(serial, fanIn);
        }
    }

    @Test
    void registeredCompoundRecipeMatchesTheVerifiedComposition() throws Exception {
        var location = M3FunctionalJavaProofTest.class.getResource("/META-INF/rewrite/m3-functional-java.yml");
        org.junit.jupiter.api.Assertions.assertNotNull(location);
        Recipe compound;
        try (var input = location.openStream()) {
            compound = org.openrewrite.config.Environment.builder().load(new org.openrewrite.config.YamlResourceLoader(
                    input, location.toURI(), new java.util.Properties())).build()
                    .activateRecipes("com.synexia.rewrite.M3FunctionalJavaCandidate");
        }
        String direct = output(execute(functionalPipeline(), SOURCE, PATH), SOURCE);
        String named = output(execute(compound, SOURCE, PATH), SOURCE);
        assertEquals(direct, named);
        assertTrue(execute(compound, named, PATH).getChangeset().getAllResults().isEmpty());
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
    }

    private static SourceFile parse(String source, String path) {
        return JavaParser.fromJavaVersion().build().parse(context(), source)
                .findFirst().orElseThrow().withSourcePath(Path.of(path));
    }

    private static RecipeRun execute(Recipe recipe, String source, String path) {
        return recipe.run(new InMemoryLargeSourceSet(List.of(parse(source, path))), context(), 1);
    }

    private static List<String> tableRows(RecipeRun run) {
        return run.getDataTables().entrySet().stream()
                .filter(entry -> entry.getKey().getName().startsWith("com.synexia."))
                .flatMap(entry -> entry.getValue().stream()
                        .map(row -> entry.getKey().getName() + "=" + json(row)))
                .sorted().toList();
    }

    private static String json(Object row) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(row);
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    private static String output(RecipeRun run, String original) {
        var changes = run.getChangeset().getAllResults();
        assertTrue(changes.size() <= 1);
        return changes.isEmpty() ? original : java.util.Objects.requireNonNull(changes.getFirst().getAfter()).printAll();
    }

    private Path compile(String source, String name) throws Exception {
        Path root = Files.createDirectory(temporary.resolve(name));
        Path file = root.resolve("Example.java");
        Files.writeString(file, source, StandardCharsets.UTF_8);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "21", "-proc:none", "-d", root.toString(), file.toString()));
        return root;
    }

    private static URLClassLoader loader(Path classes) throws Exception {
        return new URLClassLoader(new java.net.URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }
}
