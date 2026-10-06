// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Exact recipe-first evolution that binds Nebula A3 materialization to portable mastery evidence. */
public final class NebulaM3PortableMasteryFanInRecipe
        extends ScanningRecipe<NebulaM3PortableMasteryFanInRecipe.State> {
    private static final String ANCHOR =
            "src/main/java/org/eclipse/nebula/m3/rewrite/NebulaM3Java21ConvergenceRecipe.java";
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/convergence/portable-mastery-fanin/";

    private static final Map<String, Target> TARGETS =
            Map.of(
                    "src/main/java/org/eclipse/nebula/m3/NebulaM3A3Apply.java",
                    new Target(
                            "NebulaM3A3Apply.java.txt",
                            "8e7a5f1cd249f9d6123ca53ae82eda277d2c2f2eb56b045b0836b12e4a5fd0f5",
                            "c2850e04a95188aacf4c18d42a51ec7704ba7449cc6c14a4be376e6536e9b0a9"),
                    "src/main/java/org/eclipse/nebula/m3/NebulaM3MasteryFanIn.java",
                    new Target(
                            "NebulaM3MasteryFanIn.java.txt",
                            "ABSENT",
                            "503f84c9a229bdcec86a35e5b890ebfc8b12e025607dc5c6f4502151b4926006"),
                    "src/test/java/org/eclipse/nebula/m3/NebulaM3A3ApplyTest.java",
                    new Target(
                            "NebulaM3A3ApplyTest.java.txt",
                            "4ef09a241551ae5a20cefdc9d1c3dee830b7f0826b2f75ce9e222c4de9467f12",
                            "9b19edd98fcc096fb47b39ced6cd13f2e2d98f71d21dc0b16ee7d7fafce2fbeb"),
                    "src/test/java/org/eclipse/nebula/m3/NebulaM3MasteryFanInTest.java",
                    new Target(
                            "NebulaM3MasteryFanInTest.java.txt",
                            "ABSENT",
                            "88d3ab85aeee1a66ed472281435eeaeebf2a9de77fefcc5526f775ae7ff01e6e"));

    static final class State {
        boolean anchorSeen;
        final Map<String, String> seen = new LinkedHashMap<>();
        final List<String> conflicts = new ArrayList<>();
    }

    private record Target(String resource, String before, String after) {
        private Target {
            resource = Objects.requireNonNull(resource, "resource");
            before = Objects.requireNonNull(before, "before");
            after = Objects.requireNonNull(after, "after");
            if (!("ABSENT".equals(before) || before.matches("[0-9a-f]{64}"))
                    || !after.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("portable mastery target hash");
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "Gate Nebula A3 materialization on portable mastery evidence";
    }

    @Override
    public String getDescription() {
        return "Evolves the existing A3 materializer from exact reviewed preimages and adds the "
                + "strict authority-free M3_RECIPE_MASTERY_FANIN_V6 importer.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "a3",
                "recipe-first",
                "mastery",
                "fanin",
                "cross-repository",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public State getInitialValue(ExecutionContext context) {
        TARGETS.forEach(
                (path, target) -> {
                    String body = resource(target.resource());
                    if (!target.after().equals(sha256(body))) {
                        throw new IllegalStateException(
                                "portable mastery resource drift: " + path);
                    }
                });
        return new State();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                synchronized (state) {
                    if (ANCHOR.equals(path)) state.anchorSeen = true;
                    Target target = TARGETS.get(path);
                    if (target == null) return tree;

                    if (!(source instanceof J.CompilationUnit)) {
                        state.conflicts.add("portable mastery target is not Java: " + path);
                    }
                    String current = sha256(source.printAll());
                    if (state.seen.putIfAbsent(path, current) != null) {
                        state.conflicts.add("duplicate portable mastery target: " + path);
                    }
                    boolean admitted =
                            current.equals(target.after())
                                    || (!"ABSENT".equals(target.before())
                                            && current.equals(target.before()));
                    if (!admitted) {
                        state.conflicts.add("portable mastery preimage drift: " + path);
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            State state,
            Collection<SourceFile> generatedInThisCycle,
            ExecutionContext context) {
        prepare(state);
        ArrayList<SourceFile> generated = new ArrayList<>();
        TARGETS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(entry -> !state.seen.containsKey(entry.getKey()))
                .forEach(
                        entry -> {
                            if (!"ABSENT".equals(entry.getValue().before())) {
                                throw new IllegalStateException(
                                        "required portable mastery target missing: "
                                                + entry.getKey());
                            }
                            generated.add(
                                    parse(
                                            entry.getKey(),
                                            entry.getValue(),
                                            context));
                        });
        return List.copyOf(generated);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(State state) {
        prepare(state);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                Target target = TARGETS.get(path);
                if (target == null) return tree;

                String current = sha256(source.printAll());
                if (current.equals(target.after())) return tree;
                if (!current.equals(target.before())) {
                    throw new IllegalStateException(
                            "portable mastery target changed after scan: " + path);
                }

                SourceFile candidate = parse(path, target, context);
                return candidate
                        .withId(source.getId())
                        .withSourcePath(source.getSourcePath())
                        .withMarkers(source.getMarkers())
                        .withFileAttributes(source.getFileAttributes())
                        .withCharset(source.getCharset())
                        .withCharsetBomMarked(source.isCharsetBomMarked())
                        .withChecksum(null);
            }
        };
    }

    public boolean productSourceMutationAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static Map<String, String> reviewedPostimages() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        TARGETS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), entry.getValue().after()));
        return Map.copyOf(result);
    }

    private static void prepare(State state) {
        synchronized (state) {
            if (!state.anchorSeen) {
                throw new IllegalStateException("Nebula recipe-first anchor missing");
            }
            if (!state.conflicts.isEmpty()) {
                throw new IllegalStateException(String.join("; ", state.conflicts));
            }
            for (Map.Entry<String, Target> entry : TARGETS.entrySet()) {
                if (!"ABSENT".equals(entry.getValue().before())
                        && !state.seen.containsKey(entry.getKey())) {
                    throw new IllegalStateException(
                            "required portable mastery target missing: " + entry.getKey());
                }
            }
        }
    }

    private static SourceFile parse(
            String path,
            Target target,
            ExecutionContext context) {
        String body = resource(target.resource());
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path), body)),
                                null,
                                context)
                        .toList();
        if (parsed.size() != 1
                || !(parsed.getFirst() instanceof J.CompilationUnit)
                || !body.equals(parsed.getFirst().printAll())) {
            throw new IllegalStateException(
                    "portable mastery Java roundtrip failed: " + path);
        }
        return parsed.getFirst().withSourcePath(Path.of(path));
    }

    private static String resource(String name) {
        try (InputStream input =
                NebulaM3PortableMasteryFanInRecipe.class.getResourceAsStream(
                        ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing portable mastery resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read portable mastery resource", failure);
        }
    }

    private static String normalized(Path path) {
        String value = path.normalize().toString().replace('\\', '/');
        String prefix = "m3/recipe-first/";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            Objects.requireNonNull(value, "value")
                                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
