// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Deterministic evidence writer for exact repository recipe capability coverage. */
public final class M3RepositoryRecipeCoverageWriter {
    public static final String ENTRIES_FILE = "M3_REPOSITORY_RECIPE_COVERAGE.tsv";
    public static final String SUMMARY_FILE = "M3_REPOSITORY_RECIPE_COVERAGE_SUMMARY.tsv";
    public static final String ROOT_FILE = "M3_REPOSITORY_RECIPE_COVERAGE.sha256";

    private M3RepositoryRecipeCoverageWriter() {
        throw new AssertionError("No instances");
    }

    public static void write(M3RepositoryRecipeCoverage coverage, Path outputDirectory)
            throws IOException {
        Objects.requireNonNull(coverage, "coverage");
        Path output =
                Objects.requireNonNull(outputDirectory, "outputDirectory")
                        .toAbsolutePath()
                        .normalize();
        Files.createDirectories(output);
        Files.writeString(
                output.resolve(ENTRIES_FILE), entries(coverage), StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve(SUMMARY_FILE), summary(coverage), StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve(ROOT_FILE),
                "ROOT  " + coverage.root() + "\n",
                StandardCharsets.UTF_8);
    }

    static String entries(M3RepositoryRecipeCoverage coverage) {
        StringBuilder out =
                new StringBuilder(
                        "capability_id\twork_root\tcapability_tag\tstatus"
                                + "\tregistered_recipes\tsource_rows\trequired_gates\troot\n");
        for (M3RepositoryRecipeCoverage.Row row : coverage.rows()) {
            row(
                    out,
                    row.workOrder().capabilityId(),
                    row.workOrder().root(),
                    row.capabilityTag(),
                    row.status().name(),
                    String.join(",", row.registeredRecipes()),
                    Integer.toString(row.workOrder().sources().size()),
                    String.join(",", row.workOrder().requiredGates()),
                    row.root());
        }
        return out.toString();
    }

    static String summary(M3RepositoryRecipeCoverage coverage) {
        StringBuilder out = new StringBuilder("metric\tvalue\n");
        out.append("capabilities\t").append(coverage.summary().capabilities()).append('\n');
        out.append("sourceRows\t").append(coverage.summary().sourceRows()).append('\n');
        for (M3RepositoryRecipeCoverage.Status status :
                M3RepositoryRecipeCoverage.Status.values()) {
            out.append("status.")
                    .append(status.name())
                    .append('\t')
                    .append(coverage.summary().byStatus().get(status))
                    .append('\n');
        }
        out.append("executionReady\t")
                .append(coverage.summary().executionReady())
                .append('\n');
        out.append("workOrderRoot\t").append(coverage.summary().workOrderRoot()).append('\n');
        out.append("recipeInventoryRoot\t")
                .append(coverage.summary().recipeInventoryRoot())
                .append('\n');
        out.append("root\t").append(coverage.root()).append('\n');
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
