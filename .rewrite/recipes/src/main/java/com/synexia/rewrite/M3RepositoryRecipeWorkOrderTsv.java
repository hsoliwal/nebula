// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Strict TSV codec for repository-wide capability recipe work orders. */
public final class M3RepositoryRecipeWorkOrderTsv {
    public static final String HEADER =
            "work_root\tbinding_root\tcapability_id\tsuggested_recipe_class"
                    + "\tsuggested_declarative_recipe\tsuggested_test_class"
                    + "\tsuggested_fixture_directory\tsource_path\tpreimage_sha256"
                    + "\tqueue_stable_id\tbinding_entry_root\tcandidate_mechanical_pass_ids"
                    + "\tdonor_identities\tdonor_shapes\tdonor_repositories"
                    + "\trequired_gates\treasons";

    private static final int MAX_CHARS = 16_000_000;
    private static final int MAX_ROWS = 500_000;

    private M3RepositoryRecipeWorkOrderTsv() {}

    public static String render(List<M3RepositoryRecipeWorkOrder.WorkOrder> workOrders) {
        ArrayList<M3RepositoryRecipeWorkOrder.WorkOrder> ordered =
                new ArrayList<>(Objects.requireNonNull(workOrders, "workOrders"));
        ordered.sort(
                Comparator.comparing(M3RepositoryRecipeWorkOrder.WorkOrder::capabilityId)
                        .thenComparing(M3RepositoryRecipeWorkOrder.WorkOrder::root));

        StringBuilder out = new StringBuilder(HEADER).append('\n');
        for (M3RepositoryRecipeWorkOrder.WorkOrder work : ordered) {
            for (M3RepositoryRecipeWorkOrder.SourceEvidence source : work.sources()) {
                row(
                        out,
                        work.root(),
                        work.bindingRoot(),
                        work.capabilityId(),
                        work.suggestedRecipeClassName(),
                        work.suggestedDeclarativeRecipeName(),
                        work.suggestedTestClassName(),
                        work.suggestedFixtureDirectory(),
                        source.sourcePath(),
                        source.preimageSha256(),
                        source.queueStableId(),
                        source.bindingEntryRoot(),
                        encodeList(work.candidateMechanicalPassIds()),
                        encodeList(work.donorIdentities()),
                        encodeList(work.donorShapes()),
                        encodeList(work.donorRepositories()),
                        encodeList(work.requiredGates()),
                        encodeList(work.reasons()));
            }
        }
        return out.toString();
    }

    public static List<M3RepositoryRecipeWorkOrder.WorkOrder> parse(String tsv) {
        String text = Objects.toString(tsv, "");
        if (text.length() > MAX_CHARS) {
            throw new IllegalArgumentException("repository recipe work-order TSV too large");
        }
        List<String> lines =
                text.lines()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();
        if (lines.isEmpty()) return List.of();
        if (!HEADER.equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected repository work-order TSV header");
        }
        if (lines.size() - 1 > MAX_ROWS) {
            throw new IllegalArgumentException("too many repository work-order TSV rows");
        }

        Map<String, Mutable> grouped = new LinkedHashMap<>();
        for (int row = 1; row < lines.size(); row++) {
            String[] fields = lines.get(row).split("\\t", -1);
            if (fields.length != 17) {
                throw new IllegalArgumentException(
                        "expected 17 repository work-order columns at row " + (row + 1));
            }

            Parsed parsed =
                    new Parsed(
                            sha(fields[0], "workRoot"),
                            sha(fields[1], "bindingRoot"),
                            text(fields[2], "capabilityId"),
                            text(fields[3], "suggestedRecipeClassName"),
                            text(fields[4], "suggestedDeclarativeRecipeName"),
                            text(fields[5], "suggestedTestClassName"),
                            text(fields[6], "suggestedFixtureDirectory"),
                            text(fields[7], "sourcePath"),
                            sha(fields[8], "preimageSha256"),
                            sha(fields[9], "queueStableId"),
                            sha(fields[10], "bindingEntryRoot"),
                            decodeList(fields[11]),
                            decodeList(fields[12]),
                            decodeList(fields[13]),
                            decodeList(fields[14]),
                            decodeList(fields[15]),
                            decodeList(fields[16]));

            grouped.computeIfAbsent(parsed.workRoot(), ignored -> new Mutable(parsed))
                    .add(parsed);
        }

        ArrayList<M3RepositoryRecipeWorkOrder.WorkOrder> result =
                new ArrayList<>(grouped.size());
        grouped.values().forEach(value -> result.add(value.freeze()));
        result.sort(
                Comparator.comparing(M3RepositoryRecipeWorkOrder.WorkOrder::capabilityId)
                        .thenComparing(M3RepositoryRecipeWorkOrder.WorkOrder::root));
        return List.copyOf(result);
    }

    private record Parsed(
            String workRoot,
            String bindingRoot,
            String capabilityId,
            String suggestedRecipeClassName,
            String suggestedDeclarativeRecipeName,
            String suggestedTestClassName,
            String suggestedFixtureDirectory,
            String sourcePath,
            String preimageSha256,
            String queueStableId,
            String bindingEntryRoot,
            List<String> candidateMechanicalPassIds,
            List<String> donorIdentities,
            List<String> donorShapes,
            List<String> donorRepositories,
            List<String> requiredGates,
            List<String> reasons) {}

    private static final class Mutable {
        private final Parsed first;
        private final ArrayList<M3RepositoryRecipeWorkOrder.SourceEvidence> sources =
                new ArrayList<>();

        Mutable(Parsed first) {
            this.first = first;
        }

        void add(Parsed row) {
            same(first.bindingRoot(), row.bindingRoot(), "bindingRoot");
            same(first.capabilityId(), row.capabilityId(), "capabilityId");
            same(first.suggestedRecipeClassName(), row.suggestedRecipeClassName(), "recipe class");
            same(
                    first.suggestedDeclarativeRecipeName(),
                    row.suggestedDeclarativeRecipeName(),
                    "declarative recipe");
            same(first.suggestedTestClassName(), row.suggestedTestClassName(), "test class");
            same(
                    first.suggestedFixtureDirectory(),
                    row.suggestedFixtureDirectory(),
                    "fixture directory");
            same(first.workRoot(), row.workRoot(), "workRoot");
            equalList(
                    first.candidateMechanicalPassIds(),
                    row.candidateMechanicalPassIds(),
                    "candidate passes");
            equalList(first.donorIdentities(), row.donorIdentities(), "donor identities");
            equalList(first.donorShapes(), row.donorShapes(), "donor shapes");
            equalList(first.donorRepositories(), row.donorRepositories(), "donor repositories");
            equalList(first.requiredGates(), row.requiredGates(), "required gates");
            equalList(first.reasons(), row.reasons(), "reasons");
            sources.add(
                    new M3RepositoryRecipeWorkOrder.SourceEvidence(
                            row.sourcePath(),
                            row.preimageSha256(),
                            row.queueStableId(),
                            row.bindingEntryRoot()));
        }

        M3RepositoryRecipeWorkOrder.WorkOrder freeze() {
            return new M3RepositoryRecipeWorkOrder.WorkOrder(
                    first.capabilityId(),
                    first.suggestedRecipeClassName(),
                    first.suggestedDeclarativeRecipeName(),
                    first.suggestedTestClassName(),
                    first.suggestedFixtureDirectory(),
                    sources,
                    first.candidateMechanicalPassIds(),
                    first.donorIdentities(),
                    first.donorShapes(),
                    first.donorRepositories(),
                    first.requiredGates(),
                    first.reasons(),
                    first.bindingRoot(),
                    first.workRoot());
        }
    }

    private static String encodeList(List<String> values) {
        return Objects.requireNonNullElse(values, List.<String>of()).stream()
                .map(value -> text(value, "listValue"))
                .map(
                        value ->
                                Base64.getUrlEncoder()
                                        .withoutPadding()
                                        .encodeToString(value.getBytes(StandardCharsets.UTF_8)))
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static List<String> decodeList(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(",", -1))
                .map(token -> text(token, "encodedListValue"))
                .map(
                        token -> {
                            try {
                                return new String(
                                        Base64.getUrlDecoder().decode(token),
                                        StandardCharsets.UTF_8);
                            } catch (IllegalArgumentException invalid) {
                                throw new IllegalArgumentException(
                                        "invalid Base64 list member", invalid);
                            }
                        })
                .map(token -> text(token, "decodedListValue"))
                .toList();
    }

    private static void equalList(List<String> left, List<String> right, String field) {
        if (!left.equals(right)) throw new IllegalArgumentException(field + " drift");
    }

    private static void same(String left, String right, String field) {
        if (!left.equals(right)) throw new IllegalArgumentException(field + " drift");
    }

    private static void row(StringBuilder out, String... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) out.append('\t');
            out.append(cell(values[index]));
        }
        out.append('\n');
    }

    private static String cell(String value) {
        String checked = Objects.toString(value, "");
        if (checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("TSV cell");
        }
        return checked;
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = text(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }
}
