// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exact capability-tag coverage between repository recipe work orders and registered OpenRewrite recipes.
 *
 * <p>Recipe names, donor shapes and class-name similarity are not equivalence evidence. A work
 * order is execution-ready only when exactly one visible recipe advertises its exact
 * {@code m3-capability:<capabilityId>} tag.</p>
 */
public final class M3RepositoryRecipeCoverage {
    public enum Status {
        AUTHOR_RECIPE,
        REUSE_REGISTERED_RECIPE,
        HOLD_AMBIGUOUS_RECIPE
    }

    public record Row(
            M3RepositoryRecipeWorkOrder.WorkOrder workOrder,
            String capabilityTag,
            Status status,
            List<String> registeredRecipes,
            String root) {
        public Row {
            workOrder = Objects.requireNonNull(workOrder, "workOrder");
            capabilityTag = token(capabilityTag, "capabilityTag");
            status = Objects.requireNonNull(status, "status");
            registeredRecipes =
                    Objects.requireNonNull(registeredRecipes, "registeredRecipes").stream()
                            .map(value -> token(value, "registeredRecipe"))
                            .distinct()
                            .sorted()
                            .toList();
            if (!capabilityTag.equals(
                    M3RepositoryCapabilityCandidateRecipe.capabilityTag(
                            workOrder.capabilityId()))) {
                throw new IllegalArgumentException("capability tag mismatch");
            }
            switch (status) {
                case AUTHOR_RECIPE -> {
                    if (!registeredRecipes.isEmpty()) {
                        throw new IllegalArgumentException("author status cannot have recipes");
                    }
                }
                case REUSE_REGISTERED_RECIPE -> {
                    if (registeredRecipes.size() != 1) {
                        throw new IllegalArgumentException("reuse status requires one recipe");
                    }
                }
                case HOLD_AMBIGUOUS_RECIPE -> {
                    if (registeredRecipes.size() < 2) {
                        throw new IllegalArgumentException("ambiguous status requires recipes");
                    }
                }
            }
            String expected = rowRoot(workOrder, capabilityTag, status, registeredRecipes);
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("recipe coverage row root mismatch");
            }
        }

        public boolean executionReady() {
            return status == Status.REUSE_REGISTERED_RECIPE;
        }

        public boolean mutationAuthority() {
            return false;
        }
    }

    public record Summary(
            int capabilities,
            int sourceRows,
            Map<Status, Integer> byStatus,
            String workOrderRoot,
            String recipeInventoryRoot,
            boolean executionReady,
            String root) {
        public Summary {
            if (capabilities < 0 || sourceRows < 0 || sourceRows < capabilities) {
                throw new IllegalArgumentException("invalid recipe coverage counts");
            }
            EnumMap<Status, Integer> counts = new EnumMap<>(Status.class);
            counts.putAll(Objects.requireNonNull(byStatus, "byStatus"));
            for (Status status : Status.values()) counts.putIfAbsent(status, 0);
            if (counts.values().stream().mapToInt(Integer::intValue).sum() != capabilities) {
                throw new IllegalArgumentException("recipe coverage status count mismatch");
            }
            boolean expectedReady =
                    counts.get(Status.AUTHOR_RECIPE) == 0
                            && counts.get(Status.HOLD_AMBIGUOUS_RECIPE) == 0;
            if (executionReady != expectedReady) {
                throw new IllegalArgumentException("recipe coverage readiness mismatch");
            }
            byStatus = java.util.Collections.unmodifiableMap(counts);
            workOrderRoot = sha(workOrderRoot, "workOrderRoot");
            recipeInventoryRoot = sha(recipeInventoryRoot, "recipeInventoryRoot");
            root = sha(root, "root");
        }
    }

    private final List<Row> rows;
    private final Summary summary;

    private M3RepositoryRecipeCoverage(
            List<Row> rows, String workOrderRoot, String recipeInventoryRoot) {
        this.rows = List.copyOf(rows);
        EnumMap<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status status : Status.values()) counts.put(status, 0);
        int sourceRows = 0;
        boolean ready = true;
        for (Row row : this.rows) {
            counts.merge(row.status(), 1, Math::addExact);
            sourceRows = Math.addExact(sourceRows, row.workOrder().sources().size());
            ready &= row.executionReady();
        }
        String root = coverageRoot(this.rows, workOrderRoot, recipeInventoryRoot);
        this.summary =
                new Summary(
                        this.rows.size(),
                        sourceRows,
                        counts,
                        workOrderRoot,
                        recipeInventoryRoot,
                        ready,
                        root);
    }

    public static M3RepositoryRecipeCoverage capture(
            List<M3RepositoryRecipeWorkOrder.WorkOrder> workOrders,
            String... acceptedPackages) {
        M3OpenRewriteInventorySnapshot inventory =
                M3OpenRewriteInventorySnapshot.capture(acceptedPackages);
        return compile(workOrders, inventory.entries(), inventory.sha256());
    }

    public static M3RepositoryRecipeCoverage compile(
            List<M3RepositoryRecipeWorkOrder.WorkOrder> workOrders,
            List<M3OpenRewriteInventory.Entry> recipeEntries) {
        List<M3OpenRewriteInventory.Entry> entries =
                Objects.requireNonNull(recipeEntries, "recipeEntries").stream()
                        .sorted()
                        .toList();
        String inventoryRoot = sha256(M3OpenRewriteInventory.toTsv(entries));
        return compile(workOrders, entries, inventoryRoot);
    }

    private static M3RepositoryRecipeCoverage compile(
            List<M3RepositoryRecipeWorkOrder.WorkOrder> workOrders,
            List<M3OpenRewriteInventory.Entry> recipeEntries,
            String recipeInventoryRoot) {
        ArrayList<M3RepositoryRecipeWorkOrder.WorkOrder> ordered =
                new ArrayList<>(Objects.requireNonNull(workOrders, "workOrders"));
        ordered.sort(
                Comparator.comparing(M3RepositoryRecipeWorkOrder.WorkOrder::capabilityId)
                        .thenComparing(M3RepositoryRecipeWorkOrder.WorkOrder::root));
        if (ordered.stream().map(M3RepositoryRecipeWorkOrder.WorkOrder::capabilityId).distinct().count()
                != ordered.size()) {
            throw new IllegalArgumentException("duplicate repository recipe capability work order");
        }

        ArrayList<Row> rows = new ArrayList<>(ordered.size());
        for (M3RepositoryRecipeWorkOrder.WorkOrder workOrder : ordered) {
            String tag =
                    M3RepositoryCapabilityCandidateRecipe.capabilityTag(
                            workOrder.capabilityId());
            List<String> registered =
                    recipeEntries.stream()
                            .filter(entry -> entry.tags().contains(tag))
                            .map(M3OpenRewriteInventory.Entry::name)
                            .distinct()
                            .sorted()
                            .toList();
            Status status =
                    registered.isEmpty()
                            ? Status.AUTHOR_RECIPE
                            : registered.size() == 1
                                    ? Status.REUSE_REGISTERED_RECIPE
                                    : Status.HOLD_AMBIGUOUS_RECIPE;
            rows.add(new Row(workOrder, tag, status, registered, ""));
        }

        return new M3RepositoryRecipeCoverage(
                rows, workOrderRoot(ordered), sha(recipeInventoryRoot, "recipeInventoryRoot"));
    }

    public List<Row> rows() {
        return rows;
    }

    public Summary summary() {
        return summary;
    }

    public String root() {
        return summary.root();
    }

    public void requireExecutionReady() {
        if (summary.executionReady()) return;
        Row blocked = rows.stream().filter(row -> !row.executionReady()).findFirst().orElseThrow();
        throw new IllegalStateException(
                "repository recipe capability coverage incomplete: "
                        + blocked.workOrder().capabilityId()
                        + " -> "
                        + blocked.status());
    }

    private static String workOrderRoot(
            List<M3RepositoryRecipeWorkOrder.WorkOrder> workOrders) {
        MessageDigest digest = sha256Digest();
        frame(digest, "M3-REPOSITORY-RECIPE-WORK-ORDER-SET/1");
        frame(digest, Integer.toString(workOrders.size()));
        for (M3RepositoryRecipeWorkOrder.WorkOrder workOrder : workOrders) {
            frame(digest, workOrder.capabilityId());
            frame(digest, workOrder.root());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String rowRoot(
            M3RepositoryRecipeWorkOrder.WorkOrder workOrder,
            String capabilityTag,
            Status status,
            List<String> registeredRecipes) {
        MessageDigest digest = sha256Digest();
        frame(digest, "M3-REPOSITORY-RECIPE-COVERAGE-ROW/1");
        frame(digest, workOrder.capabilityId());
        frame(digest, workOrder.root());
        frame(digest, capabilityTag);
        frame(digest, status.name());
        registeredRecipes.forEach(value -> frame(digest, value));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String coverageRoot(
            List<Row> rows, String workOrderRoot, String recipeInventoryRoot) {
        MessageDigest digest = sha256Digest();
        frame(digest, "M3-REPOSITORY-RECIPE-COVERAGE/1");
        frame(digest, workOrderRoot);
        frame(digest, recipeInventoryRoot);
        frame(digest, Integer.toString(rows.size()));
        rows.forEach(row -> frame(digest, row.root()));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256(String value) {
        return HexFormat.of()
                .formatHex(
                        sha256Digest()
                                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static String token(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = token(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }
}
