// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * One grouped recipe-development work unit for an unknown LLM capability across many source plans.
 *
 * <p>The batch deliberately loses per-file edit authority. It retains only immutable source
 * authority/work-order roots needed to build one reusable recipe crate.</p>
 */
public final class M3LlmRecipeBatchWorkOrder {
    public record SourceReceipt(
            String workOrderRoot,
            String authorityRoot,
            String executionContractRoot,
            String root) {

        public SourceReceipt {
            workOrderRoot = sha(workOrderRoot, "workOrderRoot");
            authorityRoot = sha(authorityRoot, "authorityRoot");
            executionContractRoot = sha(executionContractRoot, "executionContractRoot");
            String expected = digest(
                    "M3_LLM_RECIPE_BATCH_SOURCE_RECEIPT_V1",
                    workOrderRoot,
                    authorityRoot,
                    executionContractRoot);
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("source receipt root mismatch");
            }
        }
    }

    public record Batch(
            String capabilityId,
            String sourceFilePattern,
            String suggestedRecipeClass,
            String suggestedTestClass,
            String suggestedFixtureDirectory,
            String challengeSearchRoot,
            String donorReviewRoot,
            List<SourceReceipt> sourceReceipts,
            List<String> requiredStages,
            boolean recipeCrateIsWorkUnit,
            boolean candidateOnly,
            boolean serialPromotionRequired,
            boolean targetFileEditAuthority,
            boolean donorSourceCopyAuthority,
            String root) {

        public Batch {
            capabilityId = text(capabilityId, "capabilityId");
            sourceFilePattern = text(sourceFilePattern, "sourceFilePattern");
            suggestedRecipeClass = text(suggestedRecipeClass, "suggestedRecipeClass");
            suggestedTestClass = text(suggestedTestClass, "suggestedTestClass");
            suggestedFixtureDirectory = text(suggestedFixtureDirectory, "suggestedFixtureDirectory");
            challengeSearchRoot = sha(challengeSearchRoot, "challengeSearchRoot");
            donorReviewRoot = sha(donorReviewRoot, "donorReviewRoot");
            sourceReceipts = Objects.requireNonNull(sourceReceipts, "sourceReceipts").stream()
                    .map(value -> Objects.requireNonNull(value, "sourceReceipt"))
                    .distinct()
                    .sorted(Comparator.comparing(SourceReceipt::workOrderRoot))
                    .toList();
            requiredStages = List.copyOf(Objects.requireNonNull(requiredStages, "requiredStages"));

            if (sourceReceipts.isEmpty()) throw new IllegalArgumentException("sourceReceipts required");
            if (!requiredStages.equals(M3RecipeFirstInvariant.canonicalStages())) {
                throw new IllegalArgumentException("non-canonical recipe stage order");
            }
            if (!recipeCrateIsWorkUnit || !candidateOnly || !serialPromotionRequired) {
                throw new IllegalArgumentException("batch must remain recipe-crate/candidate-only/serial");
            }
            if (targetFileEditAuthority || donorSourceCopyAuthority) {
                throw new IllegalArgumentException("batch cannot own target-file or donor-copy authority");
            }

            String expected = digest(
                    "M3_LLM_RECIPE_BATCH_WORK_ORDER_V1",
                    capabilityId,
                    sourceFilePattern,
                    suggestedRecipeClass,
                    suggestedTestClass,
                    suggestedFixtureDirectory,
                    challengeSearchRoot,
                    donorReviewRoot,
                    sourceReceipts.stream().map(SourceReceipt::root).reduce("", (a, b) -> a + b + "\u001f"),
                    String.join("\u001f", requiredStages),
                    Boolean.toString(recipeCrateIsWorkUnit),
                    Boolean.toString(candidateOnly),
                    Boolean.toString(serialPromotionRequired));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("batch work-order root mismatch");
            }
        }
    }

    private M3LlmRecipeBatchWorkOrder() {}

    public static Batch compile(List<M3LlmRecipeWorkOrder.WorkOrder> workOrders) {
        List<M3LlmRecipeWorkOrder.WorkOrder> works =
                Objects.requireNonNull(workOrders, "workOrders").stream()
                        .map(value -> Objects.requireNonNull(value, "workOrder"))
                        .sorted(Comparator.comparing(M3LlmRecipeWorkOrder.WorkOrder::root))
                        .toList();
        if (works.isEmpty()) throw new IllegalArgumentException("workOrders required");

        M3LlmRecipeWorkOrder.WorkOrder first = works.getFirst();
        if (first.mode() != M3LlmRecipeWorkOrder.Mode.AUTHOR_OR_IMPROVE_RECIPE) {
            throw new IllegalArgumentException("batch requires recipe-author/improve work");
        }

        for (M3LlmRecipeWorkOrder.WorkOrder work : works) {
            if (work.mode() != M3LlmRecipeWorkOrder.Mode.AUTHOR_OR_IMPROVE_RECIPE) {
                throw new IllegalArgumentException("mixed work-order modes");
            }
            requireSame(first.capabilityId(), work.capabilityId(), "capabilityId");
            requireSame(first.sourceFilePattern(), work.sourceFilePattern(), "sourceFilePattern");
            requireSame(first.suggestedRecipeClass(), work.suggestedRecipeClass(), "suggestedRecipeClass");
            requireSame(first.suggestedTestClass(), work.suggestedTestClass(), "suggestedTestClass");
            requireSame(first.suggestedFixtureDirectory(), work.suggestedFixtureDirectory(), "suggestedFixtureDirectory");
            requireSame(first.challengeSearchRoot(), work.challengeSearchRoot(), "challengeSearchRoot");
            requireSame(first.donorReviewRoot(), work.donorReviewRoot(), "donorReviewRoot");
            if (!work.requiredStages().equals(M3RecipeFirstInvariant.canonicalStages())) {
                throw new IllegalArgumentException("work order lost canonical stage order");
            }
            if (!work.recipeCrateIsWorkUnit()
                    || !work.candidateOnly()
                    || !work.serialPromotionRequired()
                    || work.targetFileEditAuthority()
                    || work.donorSourceCopyAuthority()) {
                throw new IllegalArgumentException("work order lost recipe-crate authority constraints");
            }
            if (work.authorityRoot().isEmpty()) {
                throw new IllegalArgumentException("recipe-author work requires IOP authority root");
            }
        }

        return new Batch(
                first.capabilityId(),
                first.sourceFilePattern(),
                first.suggestedRecipeClass(),
                first.suggestedTestClass(),
                first.suggestedFixtureDirectory(),
                first.challengeSearchRoot(),
                first.donorReviewRoot(),
                works.stream()
                        .map(work -> new SourceReceipt(
                                work.root(),
                                work.authorityRoot(),
                                work.executionContract().root(),
                                ""))
                        .toList(),
                M3RecipeFirstInvariant.canonicalStages(),
                true,
                true,
                true,
                false,
                false,
                "");
    }

    private static void requireSame(String expected, String actual, String field) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalArgumentException("batch " + field + " mismatch");
        }
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNullElse(value, "").strip();
        if (checked.isEmpty()
                || checked.length() > 8192
                || checked.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNullElse(value, "").strip();
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String digest(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                byte[] bytes = field.getBytes(StandardCharsets.UTF_8);
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
}
