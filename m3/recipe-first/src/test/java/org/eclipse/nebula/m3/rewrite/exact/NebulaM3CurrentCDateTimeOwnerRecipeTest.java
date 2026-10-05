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
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class NebulaM3CurrentCDateTimeOwnerRecipeTest {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/cdatetime-current-owner/";

    private record Case(String path, String before, String after, boolean java) {}

    private static final List<Case> CASES = List.of(
            new Case(
                    "releng/org.eclipse.nebula.nebula-parent/pom.xml",
                    "pre-00-nebula-parent-pom.xml.txt",
                    "00-nebula-parent-pom.xml.txt",
                    false),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/src/org/eclipse/nebula/widgets/cdatetime/css/CDateTimePropertyHandler.java",
                    "pre-01-CDateTimePropertyHandler.java.txt",
                    "01-CDateTimePropertyHandler.java.txt",
                    true),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/META-INF/MANIFEST.MF",
                    "pre-02-MANIFEST.MF.txt",
                    "02-MANIFEST.MF.txt",
                    false),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/org.eclipse.nebula.widgets.cdatetime.example.e4.product",
                    "pre-03-cdatetime-e4.product.txt",
                    "03-cdatetime-e4.product.txt",
                    false),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/BigWidgetsPart.java",
                    "pre-04-BigWidgetsPart.java.txt",
                    "04-BigWidgetsPart.java.txt",
                    true),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/SimpleWidgetsPart.java",
                    "pre-05-SimpleWidgetsPart.java.txt",
                    "05-SimpleWidgetsPart.java.txt",
                    true),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/css/BaseCSSThemingTest.java",
                    "pre-06-BaseCSSThemingTest.java.txt",
                    "06-BaseCSSThemingTest.java.txt",
                    true));

    @Test
    void everyExistingTargetReplaysExactlyAndReachesFixedPoint() {
        List<Recipe> steps = new NebulaM3CurrentCDateTimeOwnerRecipe().getRecipeList();
        assertEquals(8, steps.size());

        for (int index = 0; index < CASES.size(); index++) {
            Case fixture = CASES.get(index);
            String beforeText = resource(fixture.before());
            String afterText = resource(fixture.after());

            var firstErrors = new ArrayList<Throwable>();
            var firstContext = new InMemoryExecutionContext(firstErrors::add);
            SourceFile before = source(fixture, beforeText, firstContext);
            String expectedAfter =
                    fixture.java()
                            ? javaSource(fixture.path(), afterText, firstContext).printAll()
                            : afterText;
            var first = steps.get(index).run(
                    new InMemoryLargeSourceSet(List.of(before)), firstContext);
            assertTrue(firstErrors.isEmpty(), fixture.path() + ": " + firstErrors);
            var changes = first.getChangeset().getAllResults();
            assertEquals(1, changes.size(), fixture.path());
            SourceFile after = changes.getFirst().getAfter();
            assertEquals(expectedAfter, after.printAll(), fixture.path());

            var secondErrors = new ArrayList<Throwable>();
            var secondContext = new InMemoryExecutionContext(secondErrors::add);
            var second = steps.get(index).run(
                    new InMemoryLargeSourceSet(List.of(after)), secondContext);
            assertTrue(secondErrors.isEmpty(), fixture.path() + ": " + secondErrors);
            assertTrue(second.getChangeset().getAllResults().isEmpty(), fixture.path());
        }
    }

    @Test
    void providerIsGeneratedOnlyBesideReviewedTestAnchorAndThenStops() {
        List<Recipe> steps = new NebulaM3CurrentCDateTimeOwnerRecipe().getRecipeList();
        Recipe materializer = steps.get(7);
        Case anchorCase = CASES.get(6);

        var context = new InMemoryExecutionContext(failure -> {
            throw new AssertionError(failure);
        });
        SourceFile anchor = source(anchorCase, resource(anchorCase.after()), context);
        var first = materializer.run(
                new InMemoryLargeSourceSet(List.of(anchor)), context, 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile provider = changes.getFirst().getAfter();
        assertEquals(
                "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/tests/css/CSSPropertyHandlerSimpleProviderImpl.java",
                normalized(provider.getSourcePath()));
        assertEquals(
                javaSource(
                                "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/tests/css/CSSPropertyHandlerSimpleProviderImpl.java",
                                resource("07-CSSPropertyHandlerSimpleProviderImpl.java.txt"),
                                context)
                        .printAll(),
                provider.printAll());

        var second = materializer.run(
                new InMemoryLargeSourceSet(List.of(anchor, provider)), context, 1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void sourceDriftIsRefusedWithoutCandidateMutation() {
        Recipe javaStep = new NebulaM3CurrentCDateTimeOwnerRecipe().getRecipeList().get(1);
        Case fixture = CASES.get(1);
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        String driftText = resource(fixture.before()) + "// drift\n";
        SourceFile drift = source(fixture, driftText, context);
        String original = drift.printAll();

        var run = javaStep.run(new InMemoryLargeSourceSet(List.of(drift)), context);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty());
        assertEquals(original, drift.printAll());
    }

    @Test
    void recipeRemainsAnExplicitEightStepCurrentOwnerPacket() {
        NebulaM3CurrentCDateTimeOwnerRecipe recipe =
                new NebulaM3CurrentCDateTimeOwnerRecipe();
        assertEquals(8, recipe.getRecipeList().size());
        assertTrue(recipe.getTags().contains("recipe-first"));
        assertTrue(recipe.getTags().contains("current-master"));
    }

    private static SourceFile source(Case fixture, String text, InMemoryExecutionContext context) {
        if (!fixture.java()) {
            return PlainText.builder()
                    .sourcePath(Path.of(fixture.path()))
                    .text(text)
                    .build();
        }
        return javaSource(fixture.path(), text, context);
    }

    private static SourceFile javaSource(
            String path, String text, InMemoryExecutionContext context) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), text)),
                        null,
                        context)
                .toList();
        assertEquals(1, parsed.size(), path);
        return parsed.getFirst();
    }

    private static String resource(String name) {
        try (var stream =
                NebulaM3CurrentCDateTimeOwnerRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IllegalStateException("missing resource " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
