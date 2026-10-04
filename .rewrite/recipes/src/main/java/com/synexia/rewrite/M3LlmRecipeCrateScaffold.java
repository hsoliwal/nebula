// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Deterministic five-artifact renderer for one grouped generic LLM recipe crate. */
public final class M3LlmRecipeCrateScaffold {
    public record Artifact(String path, String content) {
        public Artifact {
            path = checkedPath(path);
            content = Objects.requireNonNull(content, "content");
        }
    }

    public record Scaffold(
            M3LlmRecipeBatchWorkOrder.Batch batch,
            List<Artifact> artifacts,
            String root) {

        public Scaffold {
            batch = Objects.requireNonNull(batch, "batch");
            artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts"));
            if (artifacts.size() != 5) {
                throw new IllegalArgumentException("expected five recipe-crate artifacts");
            }
            if (artifacts.stream().map(Artifact::path).distinct().count() != artifacts.size()) {
                throw new IllegalArgumentException("duplicate scaffold artifact path");
            }
            String expected = digest(
                    "M3_LLM_RECIPE_CRATE_SCAFFOLD_V1",
                    batch.root(),
                    artifacts.stream()
                            .map(value -> value.path() + "\0" + sha256(value.content()))
                            .reduce("", (a, b) -> a + b + "\n"));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) throw new IllegalArgumentException("scaffold root mismatch");
        }

        public boolean targetSourceMutationAuthority() { return false; }
        public boolean donorSourceCopyAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    private M3LlmRecipeCrateScaffold() {}

    public static Scaffold render(M3LlmRecipeBatchWorkOrder.Batch batch) {
        M3LlmRecipeBatchWorkOrder.Batch work = Objects.requireNonNull(batch, "batch");
        String recipeFqcn = work.suggestedRecipeClass();
        String testFqcn = work.suggestedTestClass();
        requireRewriteClass(recipeFqcn, "suggestedRecipeClass");
        requireRewriteClass(testFqcn, "suggestedTestClass");

        String recipeSimple = simpleName(recipeFqcn);
        String testSimple = simpleName(testFqcn);
        String declarative = "com.synexia." + recipeSimple.replace("Recipe", "");

        String javaPath = javaPath(recipeFqcn, "src/main/java");
        String yamlPath = "synexia-openrewrite-recipes/src/main/resources/META-INF/rewrite/"
                + "m3-" + slug(work.capabilityId()) + "-candidate.yml";
        String testPath = javaPath(testFqcn, "src/test/java");
        String fixturePath = work.suggestedFixtureDirectory() + "BATCH_WORK_ORDER.txt";
        String cataloguePath = work.suggestedFixtureDirectory() + "m3-recipe-catalogue.fragment.tsv";

        String java = javaSource(work, recipeSimple);
        String yaml = yaml(work, recipeSimple, declarative);
        String test = testSource(work, recipeSimple, testSimple);
        String fixture = fixtureManifest(work);
        String catalogue = catalogueFragment(work, java, fixture);

        return new Scaffold(
                work,
                List.of(
                        new Artifact(javaPath, java),
                        new Artifact(yamlPath, yaml),
                        new Artifact(testPath, test),
                        new Artifact(fixturePath, fixture),
                        new Artifact(cataloguePath, catalogue)),
                "");
    }

    private static String javaSource(M3LlmRecipeBatchWorkOrder.Batch work, String simpleName) {
        return """
                // SPDX-License-Identifier: Apache-2.0
                package com.synexia.rewrite;

                /**
                 * Generated recipe-development leaf for %s.
                 *
                 * <p>Read-only and candidate-only until capability-specific transformation logic
                 * is implemented, wrapped by M3IopOperatorRecipeWriter, and proven through M3.</p>
                 */
                public final class %s extends M3IopCapabilityCandidateRecipe {
                    public %s() {
                        super("%s", "%s");
                    }
                }
                """.formatted(
                M3GeneratedSourceEscapes.javaString(work.capabilityId()),
                simpleName,
                simpleName,
                M3GeneratedSourceEscapes.javaString(work.capabilityId()),
                M3GeneratedSourceEscapes.javaString(work.sourceFilePattern()));
    }

    private static String yaml(
            M3LlmRecipeBatchWorkOrder.Batch work,
            String recipeSimple,
            String declarativeName) {
        return """
                ---
                type: specs.openrewrite.org/v1beta/recipe
                name: %s
                displayName: M3 %s candidate
                description: >-
                  Generated read-only IOP recipe-development leaf for %s. Target-source mutation,
                  donor-source copy and canonical promotion remain forbidden until the leaf is
                  implemented, operator-wrapped and M3-proven.
                recipeList:
                  - com.synexia.rewrite.%s
                """.formatted(
                declarativeName,
                work.capabilityId(),
                work.capabilityId(),
                recipeSimple);
    }

    private static String testSource(
            M3LlmRecipeBatchWorkOrder.Batch work,
            String recipeSimple,
            String testSimple) {
        return """
                // SPDX-License-Identifier: Apache-2.0
                package com.synexia.rewrite;

                import static org.junit.jupiter.api.Assertions.assertEquals;
                import static org.junit.jupiter.api.Assertions.assertFalse;
                import static org.junit.jupiter.api.Assertions.assertTrue;
                import org.junit.jupiter.api.Test;

                class %s {
                    @Test
                    void generatedLeafIsReadOnlyAndRecipeFirst() {
                        var recipe = new %s();
                        assertEquals("%s", recipe.getCapabilityId());
                        assertEquals("%s", recipe.getSourceFilePattern());
                        assertEquals(1, recipe.maxCycles());
                        assertTrue(recipe.getRecipeList().isEmpty());
                        assertFalse(recipe.replacementAuthority());
                        assertFalse(recipe.targetFileMutationAuthority());
                        assertFalse(recipe.promotionAuthority());
                    }
                }
                """.formatted(
                testSimple,
                recipeSimple,
                M3GeneratedSourceEscapes.javaString(work.capabilityId()),
                M3GeneratedSourceEscapes.javaString(work.sourceFilePattern()));
    }

    private static String fixtureManifest(M3LlmRecipeBatchWorkOrder.Batch work) {
        StringBuilder out = new StringBuilder()
                .append("M3-LLM-RECIPE-BATCH-WORK-ORDER/1\n")
                .append("batchRoot=").append(work.root()).append('\n')
                .append("capabilityId=").append(work.capabilityId()).append('\n')
                .append("sourceFilePattern=").append(work.sourceFilePattern()).append('\n')
                .append("challengeSearchRoot=").append(work.challengeSearchRoot()).append('\n')
                .append("donorReviewRoot=").append(work.donorReviewRoot()).append('\n')
                .append("requiredStages=").append(String.join(",", work.requiredStages())).append('\n')
                .append("workOrders=").append(work.sourceReceipts().size()).append('\n');

        for (int index = 0; index < work.sourceReceipts().size(); index++) {
            M3LlmRecipeBatchWorkOrder.SourceReceipt receipt = work.sourceReceipts().get(index);
            out.append(index)
                    .append('\t')
                    .append(receipt.workOrderRoot())
                    .append('\t')
                    .append(receipt.authorityRoot())
                    .append('\t')
                    .append(receipt.executionContractRoot())
                    .append('\t')
                    .append(receipt.root())
                    .append('\n');
        }
        return out.toString();
    }

    private static String catalogueFragment(
            M3LlmRecipeBatchWorkOrder.Batch work,
            String javaSource,
            String fixtureManifest) {
        return M3RecipeCatalogueTsv.HEADER
                + "\n"
                + work.capabilityId()
                + "\t"
                + work.suggestedRecipeClass()
                + "\t"
                + sha256(javaSource)
                + "\t"
                + sha256(fixtureManifest)
                + "\tm3,recipe-first,candidate,iop,class-hooked,generic-capability\n";
    }

    private static String javaPath(String fqcn, String sourceRoot) {
        String relative = fqcn.replace('.', '/') + ".java";
        return "synexia-openrewrite-recipes/" + sourceRoot + "/" + relative;
    }

    private static void requireRewriteClass(String fqcn, String field) {
        if (fqcn == null
                || !fqcn.startsWith("com.synexia.rewrite.")
                || !fqcn.matches("[A-Za-z_$][A-Za-z0-9_$.]*")) {
            throw new IllegalArgumentException(field);
        }
    }

    private static String simpleName(String fqcn) {
        return fqcn.substring(fqcn.lastIndexOf('.') + 1);
    }

    private static String slug(String value) {
        String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "capability" : slug;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String checkedPath(String value) {
        String path = Objects.requireNonNull(value, "path").replace('\\', '/').strip();
        if (path.isEmpty()
                || path.startsWith("/")
                || path.equals("..")
                || path.startsWith("../")
                || path.endsWith("/..")
                || path.contains("/../")
                || path.indexOf('\0') >= 0
                || path.indexOf('\n') >= 0
                || path.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("path");
        }
        return path;
    }
}
