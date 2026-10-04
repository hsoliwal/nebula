// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Deterministic writer for repository capability-level recipe work. */
public final class M3RepositoryRecipeWorkOrderWriter {
    public static final String ENTRIES_FILE = "M3_REPOSITORY_RECIPE_WORK_ORDERS.tsv";
    public static final String SUMMARY_FILE = "M3_REPOSITORY_RECIPE_WORK_ORDER_SUMMARY.tsv";
    public static final String ROOT_FILE = "M3_REPOSITORY_RECIPE_WORK_ORDER.sha256";

    private M3RepositoryRecipeWorkOrderWriter() {
        throw new AssertionError("No instances");
    }

    public static Summary write(
            M3ImplementationQueueBinding binding, Path outputDirectory) throws IOException {
        Objects.requireNonNull(binding, "binding");
        Path output =
                Objects.requireNonNull(outputDirectory, "outputDirectory")
                        .toAbsolutePath()
                        .normalize();
        Files.createDirectories(output);

        List<M3RepositoryRecipeWorkOrder.WorkOrder> workOrders =
                M3RepositoryRecipeWorkOrder.compile(binding);
        String entries = M3RepositoryRecipeWorkOrderTsv.render(workOrders);
        Summary summary = summary(binding, workOrders);

        Files.writeString(output.resolve(ENTRIES_FILE), entries, StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve(SUMMARY_FILE), renderSummary(summary), StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve(ROOT_FILE),
                "ROOT  " + summary.root() + "\n",
                StandardCharsets.UTF_8);
        return summary;
    }

    public static Summary summary(
            M3ImplementationQueueBinding binding,
            List<M3RepositoryRecipeWorkOrder.WorkOrder> workOrders) {
        Objects.requireNonNull(binding, "binding");
        List<M3RepositoryRecipeWorkOrder.WorkOrder> ordered =
                Objects.requireNonNull(workOrders, "workOrders").stream()
                        .sorted(
                                java.util.Comparator.comparing(
                                                M3RepositoryRecipeWorkOrder.WorkOrder::capabilityId)
                                        .thenComparing(
                                                M3RepositoryRecipeWorkOrder.WorkOrder::root))
                        .toList();
        int sourceRows =
                ordered.stream().mapToInt(work -> work.sources().size()).sum();
        MessageDigest digest = sha256();
        frame(digest, "M3-REPOSITORY-RECIPE-WORK-ORDERS/1");
        frame(digest, binding.root());
        frame(digest, Integer.toString(ordered.size()));
        frame(digest, Integer.toString(sourceRows));
        ordered.forEach(work -> frame(digest, work.root()));
        String root = HexFormat.of().formatHex(digest.digest());
        return new Summary(
                ordered.size(),
                sourceRows,
                binding.root(),
                binding.summary().queueRoot(),
                binding.summary().inventoryRoot(),
                binding.summary().rewritePlanRoot(),
                root);
    }

    static String renderSummary(Summary summary) {
        StringBuilder out = new StringBuilder("metric\tvalue\n");
        metric(out, "capabilities", summary.capabilities());
        metric(out, "sourceRows", summary.sourceRows());
        metric(out, "bindingRoot", summary.bindingRoot());
        metric(out, "queueRoot", summary.queueRoot());
        metric(out, "inventoryRoot", summary.inventoryRoot());
        metric(out, "rewritePlanRoot", summary.rewritePlanRoot());
        metric(out, "root", summary.root());
        return out.toString();
    }

    public record Summary(
            int capabilities,
            int sourceRows,
            String bindingRoot,
            String queueRoot,
            String inventoryRoot,
            String rewritePlanRoot,
            String root) {
        public Summary {
            if (capabilities < 0 || sourceRows < 0 || sourceRows < capabilities) {
                throw new IllegalArgumentException("invalid repository recipe work counts");
            }
            bindingRoot = sha(bindingRoot, "bindingRoot");
            queueRoot = sha(queueRoot, "queueRoot");
            inventoryRoot = sha(inventoryRoot, "inventoryRoot");
            rewritePlanRoot = sha(rewritePlanRoot, "rewritePlanRoot");
            root = sha(root, "root");
        }
    }

    private static void metric(StringBuilder out, String name, Object value) {
        out.append(name).append('\t').append(value).append('\n');
    }

    private static MessageDigest sha256() {
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

    private static String sha(String value, String field) {
        String checked = Objects.toString(value, "").strip().toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }
}
