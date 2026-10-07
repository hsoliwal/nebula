// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class NebulaM3A3MasteryRecipeTest {
    @Test
    void generatesExactReviewedLabAndSecondRunIsFixedPoint() {
        var context =
                new InMemoryExecutionContext(
                        failure -> {
                            throw new AssertionError(failure);
                        });
        SourceFile anchor =
                parse(
                        "src/main/java/org/eclipse/nebula/m3/rewrite/NebulaM3Java21ConvergenceRecipe.java",
                        """
                        package org.eclipse.nebula.m3.rewrite;
                        public final class NebulaM3Java21ConvergenceRecipe {}
                        """,
                        context);

        var recipe = new NebulaM3A3MasteryRecipe();
        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(anchor)),
                        context,
                        2);
        var results = first.getChangeset().getAllResults();

        assertEquals(3, results.size());
        Map<String, String> targets = NebulaM3A3MasteryRecipe.reviewedTargets();
        assertEquals(3, targets.size());

        List<SourceFile> generated =
                results.stream()
                        .map(result -> Objects.requireNonNull(result.getAfter(), "after"))
                        .toList();
        generated.forEach(
                file ->
                        assertEquals(
                                targets.get(
                                        file.getSourcePath()
                                                .toString()
                                                .replace('\\', '/')),
                                gitBlob(file.printAll())));

        assertTrue(
                generated.stream()
                        .anyMatch(file -> file.printAll().contains("48, NebulaM3A3Lab.fixtureCount()")));
        assertTrue(
                generated.stream()
                        .anyMatch(file -> file.printAll().contains("assertEquals(288, results.size())")));

        var repeat =
                recipe.run(
                        new InMemoryLargeSourceSet(
                                java.util.stream.Stream.concat(
                                                java.util.stream.Stream.of(anchor),
                                                generated.stream())
                                        .toList()),
                        context,
                        2);
        assertTrue(repeat.getChangeset().getAllResults().isEmpty());
        assertFalse(recipe.productSourceMutationAuthority());
        assertFalse(recipe.donorSourceCopyAuthority());
        assertFalse(recipe.promotionAuthority());
    }

    @Test
    void namedConvergenceOwnerRemainsTheAnchor() {
        assertTrue(
                new NebulaM3Java21ConvergenceRecipe()
                        .getTags()
                        .contains("atomization"));
        assertTrue(
                new NebulaM3Java21ConvergenceRecipe()
                        .getTags()
                        .contains("patternization"));
    }

    private static SourceFile parse(
            String path,
            String source,
            InMemoryExecutionContext context) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path), source)),
                                null,
                                context)
                        .toList();
        assertEquals(1, parsed.size());
        return parsed.getFirst();
    }

    private static String gitBlob(String text) {
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] prefix =
                ("blob " + bytes.length + "\0")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-1");
            digest.update(prefix);
            digest.update(bytes);
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
