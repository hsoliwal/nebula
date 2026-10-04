// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Deduplicated repository-wide recipe work derived from M3 implementation queue bindings.
 *
 * <p>Only RECIPE_REQUIRED rows become work orders. NO_MUTATION rows remain reuse evidence and
 * BLOCKED_REVIEW rows remain held. One capability shared by many source files yields one recipe
 * work order.</p>
 */
public final class M3RepositoryRecipeWorkOrder {
    public record SourceEvidence(
            String sourcePath,
            String preimageSha256,
            String queueStableId,
            String bindingEntryRoot) {
        public SourceEvidence {
            sourcePath = M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath);
            preimageSha256 = sha(preimageSha256, "preimageSha256");
            queueStableId = sha(queueStableId, "queueStableId");
            bindingEntryRoot = sha(bindingEntryRoot, "bindingEntryRoot");
        }
    }

    public record WorkOrder(
            String capabilityId,
            String suggestedRecipeClassName,
            String suggestedDeclarativeRecipeName,
            String suggestedTestClassName,
            String suggestedFixtureDirectory,
            List<SourceEvidence> sources,
            List<String> candidateMechanicalPassIds,
            List<String> donorIdentities,
            List<String> donorShapes,
            List<String> donorRepositories,
            List<String> requiredGates,
            List<String> reasons,
            String bindingRoot,
            String root) {

        public WorkOrder {
            capabilityId = text(capabilityId, "capabilityId");
            suggestedRecipeClassName = text(suggestedRecipeClassName, "suggestedRecipeClassName");
            suggestedDeclarativeRecipeName =
                    text(suggestedDeclarativeRecipeName, "suggestedDeclarativeRecipeName");
            suggestedTestClassName = text(suggestedTestClassName, "suggestedTestClassName");
            suggestedFixtureDirectory = text(suggestedFixtureDirectory, "suggestedFixtureDirectory");
            sources =
                    Objects.requireNonNull(sources, "sources").stream()
                            .sorted(
                                    Comparator.comparing(SourceEvidence::sourcePath)
                                            .thenComparing(SourceEvidence::preimageSha256)
                                            .thenComparing(SourceEvidence::queueStableId))
                            .toList();
            if (sources.isEmpty()) throw new IllegalArgumentException("sources");
            candidateMechanicalPassIds = ordered(candidateMechanicalPassIds);
            donorIdentities = stable(donorIdentities);
            donorShapes = stable(donorShapes);
            donorRepositories = stable(donorRepositories);
            requiredGates = ordered(requiredGates);
            if (requiredGates.isEmpty()) throw new IllegalArgumentException("requiredGates");
            reasons = stable(reasons);
            if (reasons.isEmpty()) throw new IllegalArgumentException("reasons");
            bindingRoot = sha(bindingRoot, "bindingRoot");

            String expected =
                    M3RepositoryRecipeWorkOrder.root(
                            capabilityId,
                            suggestedRecipeClassName,
                            suggestedDeclarativeRecipeName,
                            suggestedTestClassName,
                            suggestedFixtureDirectory,
                            sources,
                            candidateMechanicalPassIds,
                            donorIdentities,
                            donorShapes,
                            donorRepositories,
                            requiredGates,
                            reasons,
                            bindingRoot);
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("repository recipe work-order root mismatch");
            }
        }

        public boolean mutationAuthority() {
            return false;
        }
    }

    private M3RepositoryRecipeWorkOrder() {}

    public static List<WorkOrder> compile(M3ImplementationQueueBinding binding) {
        Objects.requireNonNull(binding, "binding");
        Map<String, Mutable> grouped = new TreeMap<>();
        for (M3ImplementationQueueBinding.Entry entry : binding.entries()) {
            if (entry.recipeState() != M3ImplementationQueueBinding.RecipeState.RECIPE_REQUIRED) {
                continue;
            }
            grouped.computeIfAbsent(
                            entry.requiredRecipeCapability(),
                            capability -> new Mutable(capability, binding.root()))
                    .add(entry);
        }

        ArrayList<WorkOrder> result = new ArrayList<>(grouped.size());
        grouped.values().forEach(value -> result.add(value.freeze()));
        return List.copyOf(result);
    }

    private static final class Mutable {
        private final String capabilityId;
        private final String bindingRoot;
        private final ArrayList<SourceEvidence> sources = new ArrayList<>();
        private final LinkedHashSet<String> passes = new LinkedHashSet<>();
        private final TreeSet<String> donorIds = new TreeSet<>();
        private final TreeSet<String> donorShapes = new TreeSet<>();
        private final TreeSet<String> donorRepos = new TreeSet<>();
        private final LinkedHashSet<String> gates = new LinkedHashSet<>();
        private final TreeSet<String> reasons = new TreeSet<>();

        Mutable(String capabilityId, String bindingRoot) {
            this.capabilityId = capabilityId;
            this.bindingRoot = bindingRoot;
        }

        void add(M3ImplementationQueueBinding.Entry entry) {
            if (!entry.requiredRecipeCapability().equals(capabilityId)) {
                throw new IllegalArgumentException("capability grouping drift");
            }
            if (entry.recipeState()
                    != M3ImplementationQueueBinding.RecipeState.RECIPE_REQUIRED) {
                throw new IllegalArgumentException("non-recipe row entered work order");
            }
            sources.add(
                    new SourceEvidence(
                            entry.sourcePath(),
                            entry.preimageSha256(),
                            entry.queueStableId(),
                            entry.root()));
            passes.addAll(entry.candidateMechanicalPassIds());
            donorIds.addAll(entry.donorIdentities());
            donorShapes.addAll(entry.donorShapes());
            donorRepos.addAll(entry.donorRepositories());
            gates.addAll(entry.requiredGates());
            reasons.add(entry.reason());
        }

        WorkOrder freeze() {
            String stem = stem(capabilityId);
            String recipeClass =
                    "com.synexia.rewrite.M3Repository" + stem + "CandidateRecipe";
            String declarative = "com.synexia.M3Repository" + stem + "Candidate";
            String testClass = recipeClass + "Test";
            String fixture =
                    "synexia-openrewrite-recipes/src/test/resources/com/synexia/rewrite/repository/"
                            + kebab(capabilityId)
                            + "/";
            return new WorkOrder(
                    capabilityId,
                    recipeClass,
                    declarative,
                    testClass,
                    fixture,
                    sources,
                    List.copyOf(passes),
                    List.copyOf(donorIds),
                    List.copyOf(donorShapes),
                    List.copyOf(donorRepos),
                    List.copyOf(gates),
                    List.copyOf(reasons),
                    bindingRoot,
                    "");
        }
    }

    private static String root(
            String capabilityId,
            String recipeClass,
            String declarative,
            String testClass,
            String fixture,
            List<SourceEvidence> sources,
            List<String> passes,
            List<String> donorIds,
            List<String> donorShapes,
            List<String> donorRepos,
            List<String> gates,
            List<String> reasons,
            String bindingRoot) {
        MessageDigest digest = sha256();
        frame(digest, "M3-REPOSITORY-RECIPE-WORK-ORDER/1");
        frame(digest, capabilityId);
        frame(digest, recipeClass);
        frame(digest, declarative);
        frame(digest, testClass);
        frame(digest, fixture);
        frame(digest, bindingRoot);
        for (SourceEvidence source : sources) {
            frame(digest, source.sourcePath());
            frame(digest, source.preimageSha256());
            frame(digest, source.queueStableId());
            frame(digest, source.bindingEntryRoot());
        }
        passes.forEach(value -> frame(digest, value));
        donorIds.forEach(value -> frame(digest, value));
        donorShapes.forEach(value -> frame(digest, value));
        donorRepos.forEach(value -> frame(digest, value));
        gates.forEach(value -> frame(digest, value));
        reasons.forEach(value -> frame(digest, value));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String stem(String capability) {
        StringBuilder out = new StringBuilder();
        for (String token : capability.split("[^A-Za-z0-9]+")) {
            if (token.isEmpty()) continue;
            String lower = token.toLowerCase(Locale.ROOT);
            out.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        if (out.isEmpty()) throw new IllegalArgumentException("capabilityId");
        return out.toString();
    }

    private static String kebab(String capability) {
        String value =
                capability.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-+|-+$", "");
        if (value.isEmpty()) throw new IllegalArgumentException("capabilityId");
        return value;
    }

    private static List<String> ordered(List<String> source) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : Objects.requireNonNullElse(source, List.<String>of())) {
            result.add(text(value, "listValue"));
        }
        return List.copyOf(result);
    }

    private static List<String> stable(List<String> source) {
        return Objects.requireNonNullElse(source, List.<String>of()).stream()
                .map(value -> text(value, "listValue"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String text(String value, String field) {
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
        String checked = text(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
