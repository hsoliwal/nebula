// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class NebulaM3PortableMasteryFanInRecipeTest {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/convergence/portable-mastery-fanin/";

    @Test
    void exactPreimagesEvolveGenerateAndReachFixedPoint() {
        NebulaM3PortableMasteryFanInRecipe recipe =
                new NebulaM3PortableMasteryFanInRecipe();
        List<SourceFile> before =
                javaSources(
                        List.of(
                                entry(
                                        "src/main/java/org/eclipse/nebula/m3/rewrite/NebulaM3Java21ConvergenceRecipe.java",
                                        null),
                                entry(
                                        "src/main/java/org/eclipse/nebula/m3/NebulaM3A3Apply.java",
                                        "pre-NebulaM3A3Apply.java.txt"),
                                entry(
                                        "src/test/java/org/eclipse/nebula/m3/NebulaM3A3ApplyTest.java",
                                        "pre-NebulaM3A3ApplyTest.java.txt")));

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(before),
                        context(),
                        1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(4, changes.size());

        List<SourceFile> after = new ArrayList<>();
        after.add(before.getFirst());
        changes.stream()
                .map(result -> result.getAfter())
                .forEach(after::add);

        assertTrue(
                after.stream()
                        .map(file -> normalized(file.getSourcePath()))
                        .anyMatch(path -> path.endsWith("NebulaM3MasteryFanIn.java")));
        assertTrue(
                after.stream()
                        .map(file -> normalized(file.getSourcePath()))
                        .anyMatch(path -> path.endsWith("NebulaM3MasteryFanInTest.java")));
        assertTrue(
                recipe.run(
                                new InMemoryLargeSourceSet(after),
                                context(),
                                1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());

        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.replacementAuthority());
        assertFalse(recipe.promotionAuthority());
        assertEquals(4, NebulaM3PortableMasteryFanInRecipe.reviewedPostimages().size());
    }

    private static List<SourceFile> javaSources(List<Entry> entries) {
        List<Parser.Input> inputs = new ArrayList<>();
        for (Entry entry : entries) {
            String body =
                    entry.resource() == null
                            ? """
                              package org.eclipse.nebula.m3.rewrite;
                              public final class NebulaM3Java21ConvergenceRecipe {}
                              """
                            : resource(entry.resource());
            inputs.add(
                    Parser.Input.fromString(
                            Path.of(entry.path()), body));
        }
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(inputs, null, context())) {
            return parsed.toList();
        }
    }

    private static Entry entry(String path, String resource) {
        return new Entry(path, resource);
    }

    private static String resource(String name) {
        try (var input =
                NebulaM3PortableMasteryFanInRecipeTest.class.getResourceAsStream(
                        ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException("missing test resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                failure -> {
                    throw new AssertionError(failure);
                });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    private record Entry(String path, String resource) {}
}
