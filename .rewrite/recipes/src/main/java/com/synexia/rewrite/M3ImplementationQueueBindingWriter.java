// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Deterministic writer for queue-to-recipe binding evidence. */
public final class M3ImplementationQueueBindingWriter {
    private M3ImplementationQueueBindingWriter() {
        throw new AssertionError("No instances");
    }

    public static void write(M3ImplementationQueueBinding binding, Path outputDirectory)
            throws IOException {
        Objects.requireNonNull(binding, "binding");
        Path output =
                Objects.requireNonNull(outputDirectory, "outputDirectory")
                        .toAbsolutePath()
                        .normalize();
        Files.createDirectories(output);
        Files.writeString(
                output.resolve("M3_QUEUE_RECIPE_BINDINGS.tsv"),
                entries(binding),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("M3_QUEUE_RECIPE_BINDING_SUMMARY.tsv"),
                summary(binding),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("M3_QUEUE_RECIPE_BINDING.sha256"),
                "ROOT  " + binding.root() + "\n",
                StandardCharsets.UTF_8);
        M3RepositoryRecipeWorkOrderWriter.write(binding, output);
    }

    static String entries(M3ImplementationQueueBinding binding) {
        StringBuilder out =
                new StringBuilder(
                        "ordinal\tqueue_stable_id\tsource_path\tpreimage_sha256\tdecision"
                                + "\trecipe_state\trequired_recipe_capability"
                                + "\tcandidate_mechanical_pass_ids\thold_reasons\trequired_gates"
                                + "\tdonor_identities\tdonor_shapes\tdonor_repositories\treason\troot\n");
        for (M3ImplementationQueueBinding.Entry entry : binding.entries()) {
            row(
                    out,
                    Integer.toString(entry.ordinal()),
                    entry.queueStableId(),
                    entry.sourcePath(),
                    entry.preimageSha256(),
                    entry.decision().name(),
                    entry.recipeState().name(),
                    entry.requiredRecipeCapability(),
                    String.join(",", entry.candidateMechanicalPassIds()),
                    String.join(",", entry.holdReasons()),
                    String.join(",", entry.requiredGates()),
                    String.join(",", entry.donorIdentities()),
                    String.join(",", entry.donorShapes()),
                    String.join(",", entry.donorRepositories()),
                    entry.reason(),
                    entry.root());
        }
        return out.toString();
    }

    static String summary(M3ImplementationQueueBinding binding) {
        StringBuilder out = new StringBuilder("metric\tvalue\n");
        out.append("entries\t").append(binding.summary().entries()).append('\n');
        for (M3ImplementationQueueBinding.RecipeState state
                : M3ImplementationQueueBinding.RecipeState.values()) {
            out.append("state.")
                    .append(state.name())
                    .append('\t')
                    .append(binding.summary().byState().get(state))
                    .append('\n');
        }
        out.append("queueRoot\t").append(binding.summary().queueRoot()).append('\n');
        out.append("inventoryRoot\t").append(binding.summary().inventoryRoot()).append('\n');
        out.append("rewritePlanRoot\t").append(binding.summary().rewritePlanRoot()).append('\n');
        out.append("root\t").append(binding.root()).append('\n');
        return out.toString();
    }

    private static void row(StringBuilder out, String... cells) {
        for (int index = 0; index < cells.length; index++) {
            if (index > 0) out.append('\t');
            out.append(tsv(cells[index]));
        }
        out.append('\n');
    }

    private static String tsv(String value) {
        return Objects.toString(value, "")
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
