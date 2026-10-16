// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

/** JUnit whole-DAG proof: Maven XML admission precedes exact Java publication. */
final class NebulaM3DagAtomicConvergenceRecipeTest {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/dag-atomic-evidence/";
    private static final String POM_BEFORE = ROOT + "pom.before.xml.txt";
    private static final String JAVA_BEFORE =
            ROOT + "NebulaM3RecipeDagManifestCli.before.java.txt";

    @Test
    void singleNamedDagHasTwoSmallOneCycleAtomsAndOptInProfile() {
        var dag = new NebulaM3DagAtomicConvergenceRecipe();
        var recipes = dag.getRecipeList();
        assertEquals(1, dag.maxCycles());
        assertEquals(2, recipes.size());
        assertEquals(NebulaM3DagAtomicMavenProfileRecipe.class,
                recipes.getFirst().getClass());
        assertEquals(NebulaM3DagEvidenceAtomicRecipe.class,
                recipes.get(1).getClass());
        assertTrue(recipes.stream().allMatch(recipe -> recipe.maxCycles() == 1));
        assertTrue(dag.getTags().contains("candidate-only"));

        String before = resource(POM_BEFORE);
        String after = resource(NebulaM3DagAtomicMavenProfileRecipe.POSTIMAGE);
        assertEquals(NebulaM3DagAtomicMavenProfileRecipe.BEFORE_SHA256,
                NebulaM3ExactJavaSnapshotRecipe.sha256(before));
        assertEquals(NebulaM3DagAtomicMavenProfileRecipe.AFTER_SHA256,
                NebulaM3ExactJavaSnapshotRecipe.sha256(after));
        assertTrue(after.contains("<id>dag-atomic-evidence</id>"));
        assertTrue(after.contains("<recipe>org.eclipse.nebula.m3.DagAtomicEvidence</recipe>"));
        assertTrue(after.contains("<failOnInvalidActiveRecipes>true</failOnInvalidActiveRecipes>"));
    }

    @Test
    void exactPomAndJavaReplayAsStructuredLstsAndReachOneFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile pom = xml(NebulaM3DagAtomicMavenProfileRecipe.PATH,
                resource(POM_BEFORE), context);
        SourceFile javaFile = java(NebulaM3DagEvidenceAtomicRecipe.PATH,
                resource(JAVA_BEFORE), context);

        var first = new NebulaM3DagAtomicConvergenceRecipe()
                .run(new InMemoryLargeSourceSet(List.of(pom, javaFile)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(2, changes.size());
        Map<String, SourceFile> post = changes.stream()
                .collect(Collectors.toMap(
                        result -> result.getAfter().getSourcePath().toString().replace('\\', '/'),
                        result -> result.getAfter()));
        assertEquals(
                Set.of(NebulaM3DagAtomicMavenProfileRecipe.PATH,
                        NebulaM3DagEvidenceAtomicRecipe.PATH),
                post.keySet());
        assertTrue(post.get(NebulaM3DagAtomicMavenProfileRecipe.PATH) instanceof Xml.Document);
        assertTrue(post.get(NebulaM3DagEvidenceAtomicRecipe.PATH) instanceof J.CompilationUnit);
        assertEquals(resource(NebulaM3DagAtomicMavenProfileRecipe.POSTIMAGE),
                post.get(NebulaM3DagAtomicMavenProfileRecipe.PATH).printAll());
        assertEquals(resource(NebulaM3DagEvidenceAtomicRecipe.POSTIMAGE),
                post.get(NebulaM3DagEvidenceAtomicRecipe.PATH).printAll());

        var secondErrors = new ArrayList<Throwable>();
        var second = new NebulaM3DagAtomicConvergenceRecipe().run(
                new InMemoryLargeSourceSet(List.copyOf(post.values())),
                new InMemoryExecutionContext(secondErrors::add));
        assertTrue(secondErrors.isEmpty(), secondErrors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void mavenDriftFailsClosedAndOtherPomHasNoAuthority() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        String drift = resource(POM_BEFORE).replace(
                "<name>Nebula M3 Recipe First</name>", "<name>drift</name>");
        SourceFile pom = xml(NebulaM3DagAtomicMavenProfileRecipe.PATH, drift, context);
        var rejected = new NebulaM3DagAtomicMavenProfileRecipe()
                .run(new InMemoryLargeSourceSet(List.of(pom)), context);
        assertFalse(errors.isEmpty());
        assertTrue(rejected.getChangeset().getAllResults().isEmpty());

        var otherErrors = new ArrayList<Throwable>();
        var otherContext = new InMemoryExecutionContext(otherErrors::add);
        SourceFile foreign = xml("other/pom.xml", resource(POM_BEFORE), otherContext);
        var noop = new NebulaM3DagAtomicMavenProfileRecipe()
                .run(new InMemoryLargeSourceSet(List.of(foreign)), otherContext);
        assertTrue(otherErrors.isEmpty(), otherErrors.toString());
        assertTrue(noop.getChangeset().getAllResults().isEmpty());
    }

    private static SourceFile xml(String path, String text, InMemoryExecutionContext context) {
        List<SourceFile> results = XmlParser.builder().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), text)), null, context)
                .toList();
        assertEquals(1, results.size());
        assertTrue(results.getFirst() instanceof Xml.Document);
        return results.getFirst();
    }

    private static SourceFile java(String path, String text, InMemoryExecutionContext context) {
        List<SourceFile> results = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), text)), null, context)
                .toList();
        assertEquals(1, results.size());
        assertTrue(results.getFirst() instanceof J.CompilationUnit);
        return results.getFirst();
    }

    private static String resource(String name) {
        try (var input = NebulaM3DagAtomicConvergenceRecipeTest.class
                .getResourceAsStream(name)) {
            if (input == null) throw new IllegalStateException("missing exact recipe image " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
