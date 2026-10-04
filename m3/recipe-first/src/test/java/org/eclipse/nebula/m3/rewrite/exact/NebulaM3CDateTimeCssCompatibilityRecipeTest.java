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
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class NebulaM3CDateTimeCssCompatibilityRecipeTest {
    private static final String PREIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/cdatetime-css/"
                    + "CDateTimePropertyHandler.before.java.txt";
    private static final String POSTIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/cdatetime-css/"
                    + "CDateTimePropertyHandler.after.java.txt";

    @Test
    void exactHashesPathsAndDonorDecisionArePinned() throws Exception {
        String before = resource(PREIMAGE);
        String after = resource(POSTIMAGE);
        var recipe = new NebulaM3CDateTimeCssCompatibilityRecipe();

        assertEquals(
                NebulaM3CDateTimeCssCompatibilityRecipe.BEFORE,
                NebulaM3ExactJavaSnapshotRecipe.sha256(before));
        assertEquals(
                NebulaM3CDateTimeCssCompatibilityRecipe.AFTER,
                NebulaM3ExactJavaSnapshotRecipe.sha256(after));
        assertTrue(recipe.matches(Path.of(
                NebulaM3CDateTimeCssCompatibilityRecipe.REPOSITORY_PATH)));
        assertTrue(recipe.matches(Path.of(
                NebulaM3CDateTimeCssCompatibilityRecipe.MODULE_PATH)));
        assertFalse(recipe.matches(Path.of("src/example/CDateTimePropertyHandler.java")));

        // OpenRewrite stock ChangeType is the reviewed donor for the mechanical
        // Measure -> CSSPrimitiveValue sub-atom. It is deliberately not the whole
        // migration because the current CSS2FontHelper classifies more values than
        // the historical Nebula/Eclipse helper did.
        assertTrue(recipe.getTags().contains("change-type-donor"));
        assertTrue(after.contains("legacyFontProperty(final CSSPrimitiveValue value)"));
        assertFalse(after.contains("org.eclipse.e4.ui.css.core.impl.dom.Measure"));
        assertFalse(after.contains("CSS2FontHelper.getCSSFontPropertyName"));
    }

    @Test
    void reviewedPreimageTransformsExactlyAndPostimageIsFixedPoint() throws Exception {
        String before = resource(PREIMAGE);
        String after = resource(POSTIMAGE);
        var recipe = new NebulaM3CDateTimeCssCompatibilityRecipe();

        Replay first = run(recipe, before);
        assertTrue(first.errors().isEmpty(), first.errors().toString());
        assertEquals(1, first.results().size());
        assertEquals(after, first.results().getFirst().getAfter().printAll());

        Replay second = run(recipe, after);
        assertTrue(second.errors().isEmpty(), second.errors().toString());
        assertTrue(second.results().isEmpty());
    }

    @Test
    void reviewedPostimagePreservesHistoricalFontClassificationAndNumericAccess()
            throws Exception {
        String after = resource(POSTIMAGE);

        assertTrue(after.contains("case CSSPrimitiveValue.CSS_PT:"));
        assertTrue(after.contains("case CSSPrimitiveValue.CSS_NUMBER:"));
        assertTrue(after.contains("case CSSPrimitiveValue.CSS_PX:"));
        assertFalse(after.contains("case CSSPrimitiveValue.CSS_EMS:"));
        assertFalse(after.contains("case CSSPrimitiveValue.CSS_PERCENTAGE:"));
        assertTrue(after.contains("final CSSPrimitiveValue primitive = (CSSPrimitiveValue) value;"));
        assertTrue(after.contains("primitive.getFloatValue((short) 0)"));
    }

    @Test
    void stalePreimageFailsClosed() throws Exception {
        Replay replay = run(
                new NebulaM3CDateTimeCssCompatibilityRecipe(),
                resource(PREIMAGE).replace(
                        "return true;",
                        "return Boolean.TRUE.booleanValue();"));

        assertFalse(replay.errors().isEmpty());
        assertTrue(replay.results().isEmpty());
    }

    private static Replay run(
            NebulaM3CDateTimeCssCompatibilityRecipe recipe, String source) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(
                                                        NebulaM3CDateTimeCssCompatibilityRecipe
                                                                .REPOSITORY_PATH),
                                                source)),
                                null,
                                context)
                        .toList();
        try {
            List<Result> results =
                    recipe.run(new InMemoryLargeSourceSet(parsed), context)
                            .getChangeset()
                            .getAllResults();
            return new Replay(results, errors);
        } catch (RuntimeException | Error failure) {
            errors.add(failure);
            return new Replay(List.of(), errors);
        }
    }

    private static String resource(String name) throws IOException {
        try (var input =
                NebulaM3CDateTimeCssCompatibilityRecipeTest.class
                        .getResourceAsStream(name)) {
            if (input == null) throw new IOException("missing resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Replay(List<Result> results, List<Throwable> errors) {}
}
