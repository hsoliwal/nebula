// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Collapses file-local problem plans into one recipe-development unit per algorithm capability.
 *
 * <p>A hundred files with the same missing capability yield one AUTHOR_OR_IMPROVE_RECIPE work
 * order, not a hundred target-file edits. Existing recipe hits yield one DRY_RUN_EXISTING work
 * order. Neither mode grants source replacement authority.</p>
 */
public final class M3ProblemRecipeWorkOrder {
    public enum Mode {
        DRY_RUN_EXISTING,
        AUTHOR_OR_IMPROVE_RECIPE
    }

    public record SourceEvidence(
            String sourcePath,
            String signatureRoot,
            String shapePlanRoot) {
        public SourceEvidence {
            sourcePath = path(sourcePath);
            signatureRoot = sha(signatureRoot, "signatureRoot");
            shapePlanRoot = sha(shapePlanRoot, "shapePlanRoot");
        }
    }

    public record WorkOrder(
            String capabilityId,
            AlgorithmShape shape,
            Mode mode,
            String suggestedRecipeClassName,
            String suggestedDeclarativeRecipeName,
            String suggestedTestClassName,
            String suggestedFixtureDirectory,
            List<String> existingRecipeCandidates,
            List<String> existingRecipeCandidateRoots,
            List<String> donorJavaSources,
            String donorCorpusRoot,
            List<SourceEvidence> sources,
            List<String> requiredGates,
            String root) {

        public WorkOrder {
            capabilityId = text(capabilityId, "capabilityId");
            shape = Objects.requireNonNull(shape, "shape");
            mode = Objects.requireNonNull(mode, "mode");
            suggestedRecipeClassName =
                    text(suggestedRecipeClassName, "suggestedRecipeClassName");
            suggestedDeclarativeRecipeName =
                    text(suggestedDeclarativeRecipeName, "suggestedDeclarativeRecipeName");
            suggestedTestClassName = text(suggestedTestClassName, "suggestedTestClassName");
            suggestedFixtureDirectory =
                    text(suggestedFixtureDirectory, "suggestedFixtureDirectory");
            existingRecipeCandidates = ordered(existingRecipeCandidates);
            existingRecipeCandidateRoots = hashes(existingRecipeCandidateRoots);
            if (existingRecipeCandidates.size() != existingRecipeCandidateRoots.size()) {
                throw new IllegalArgumentException("existing recipe identity mismatch");
            }
            donorJavaSources = stable(donorJavaSources);
            donorCorpusRoot =
                    donorJavaSources.isEmpty()
                            ? optionalSha(donorCorpusRoot, "donorCorpusRoot")
                            : sha(donorCorpusRoot, "donorCorpusRoot");
            sources = sources.stream()
                    .sorted(Comparator.comparing(SourceEvidence::sourcePath)
                            .thenComparing(SourceEvidence::shapePlanRoot))
                    .toList();
            if (sources.isEmpty()) throw new IllegalArgumentException("sources");
            requiredGates = List.copyOf(Objects.requireNonNull(requiredGates, "requiredGates"));
            if (!requiredGates.equals(gates())) {
                throw new IllegalArgumentException("non-canonical required gates");
            }
            if (mode == Mode.DRY_RUN_EXISTING && existingRecipeCandidates.isEmpty()) {
                throw new IllegalArgumentException("dry-run work requires an existing recipe");
            }
            String expected =
                    M3ProblemRecipeWorkOrder.root(
                            capabilityId,
                            shape,
                            mode,
                            suggestedRecipeClassName,
                            suggestedDeclarativeRecipeName,
                            suggestedTestClassName,
                            suggestedFixtureDirectory,
                            existingRecipeCandidates,
                            existingRecipeCandidateRoots,
                            donorJavaSources,
                            donorCorpusRoot,
                            sources);
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("work-order root mismatch");
            }
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean donorSourceCopyAuthority() {
            return false;
        }

        public boolean donorExecutionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }

        /** Deterministic serial-review receipt over the exact pinned donor files in this work unit. */
        public M3ProblemDonorReviewReceipt.Receipt donorReviewReceipt() {
            return M3ProblemDonorReviewReceipt.review(donorJavaSources, donorCorpusRoot);
        }

        public String donorSerialReviewRoot() {
            return donorReviewReceipt().serialReviewReportRoot();
        }

        public String donorReviewRoot() {
            return donorReviewReceipt().root();
        }

        /** Missing algorithm-shape recipes are authored once through the same IOP operator writer. */
        public boolean operatorWriterRequired() {
            return mode == Mode.AUTHOR_OR_IMPROVE_RECIPE;
        }

        public String operatorWriterClassName() {
            return M3IopOperatorRecipeWriter.class.getName();
        }
    }

    private M3ProblemRecipeWorkOrder() {}

    public static List<WorkOrder> compile(List<M3ProblemCataloguePlanner.Plan> plans) {
        Objects.requireNonNull(plans, "plans");
        Map<String, Mutable> grouped = new TreeMap<>();
        for (M3ProblemCataloguePlanner.Plan plan : plans) {
            Objects.requireNonNull(plan, "plan");
            for (M3ProblemCataloguePlanner.ShapePlan shape : plan.shapes()) {
                Mutable work =
                        grouped.computeIfAbsent(
                                shape.capabilityId(),
                                ignored -> new Mutable(shape, plan.javaCorpusIndexRoot()));
                work.add(
                        plan.signature().sourcePath(),
                        plan.signature().root(),
                        plan.javaCorpusIndexRoot(),
                        shape);
            }
        }

        ArrayList<WorkOrder> result = new ArrayList<>(grouped.size());
        grouped.values().forEach(value -> result.add(value.freeze()));
        return List.copyOf(result);
    }

    /**
     * Builds one canonical recipe work order from already-admitted external algorithm evidence.
     *
     * <p>This is used by API/domain evidence reconstruction and preserves the same naming, root,
     * gate, and no-replacement-authority contract as the problem-catalogue path.</p>
     */
    public static WorkOrder fromEvidence(
            String capabilityId,
            AlgorithmShape shape,
            M3RecipeFirstInvariant.Action action,
            List<String> recipeCandidates,
            List<String> recipeCandidateRoots,
            List<SourceEvidence> sources) {
        return fromEvidence(
                capabilityId,
                shape,
                action,
                recipeCandidates,
                recipeCandidateRoots,
                List.of(),
                "",
                sources);
    }

    public static WorkOrder fromEvidence(
            String capabilityId,
            AlgorithmShape shape,
            M3RecipeFirstInvariant.Action action,
            List<String> recipeCandidates,
            List<String> recipeCandidateRoots,
            List<String> donorJavaSources,
            String donorCorpusRoot,
            List<SourceEvidence> sources) {
        String checkedCapability = text(capabilityId, "capabilityId");
        AlgorithmShape checkedShape = Objects.requireNonNull(shape, "shape");
        M3RecipeFirstInvariant.Action checkedAction =
                Objects.requireNonNull(action, "action");
        List<String> checkedRecipes = ordered(recipeCandidates);
        List<String> checkedRoots = hashes(recipeCandidateRoots);
        if (checkedRecipes.size() != checkedRoots.size()) {
            throw new IllegalArgumentException(
                    "existing recipe identity mismatch");
        }
        List<SourceEvidence> checkedSources =
                Objects.requireNonNull(sources, "sources").stream()
                        .sorted(
                                Comparator.comparing(SourceEvidence::sourcePath)
                                        .thenComparing(SourceEvidence::shapePlanRoot))
                        .toList();
        if (checkedSources.isEmpty()) {
            throw new IllegalArgumentException("sources");
        }

        Mode mode =
                switch (checkedAction) {
                    case DRY_RUN_RECIPE -> Mode.DRY_RUN_EXISTING;
                    case CREATE_OR_IMPROVE_RECIPE ->
                            Mode.AUTHOR_OR_IMPROVE_RECIPE;
                    case HOLD_FOR_REINVENTORY ->
                            throw new IllegalStateException(
                                    "reinventoried atoms must not enter recipe work order");
                };
        if (mode == Mode.DRY_RUN_EXISTING && checkedRecipes.isEmpty()) {
            throw new IllegalArgumentException(
                    "dry-run work requires an existing recipe");
        }

        String stem = camel(checkedShape.name());
        String className =
                "com.synexia.rewrite.M3" + stem + "CandidateRecipe";
        String declarative = "com.synexia.M3" + stem + "Candidate";
        String test =
                "com.synexia.rewrite.M3" + stem + "CandidateRecipeTest";
        String fixtures =
                "synexia-openrewrite-recipes/src/test/resources/com/synexia/rewrite/algorithm/"
                        + checkedShape.name()
                                .toLowerCase(Locale.ROOT)
                                .replace('_', '-')
                        + "/";
        return new WorkOrder(
                checkedCapability,
                checkedShape,
                mode,
                className,
                declarative,
                test,
                fixtures,
                checkedRecipes,
                checkedRoots,
                stable(donorJavaSources),
                Objects.toString(donorCorpusRoot, ""),
                checkedSources,
                gates(),
                "");
    }

    private static final class Mutable {
        private final String capabilityId;
        private final AlgorithmShape shape;
        private final M3RecipeFirstInvariant.Action action;
        private final ArrayList<String> recipes = new ArrayList<>();
        private final ArrayList<String> recipeRoots = new ArrayList<>();
        private final java.util.TreeSet<String> donorJavaSources = new java.util.TreeSet<>();
        private final ArrayList<SourceEvidence> sources = new ArrayList<>();
        private final String donorCorpusRoot;

        Mutable(M3ProblemCataloguePlanner.ShapePlan seed, String donorCorpusRoot) {
            capabilityId = seed.capabilityId();
            shape = seed.shape();
            action = seed.recipeAction();
            this.donorCorpusRoot = sha(donorCorpusRoot, "donorCorpusRoot");
            appendRecipes(seed);
            donorJavaSources.addAll(seed.javaCorpusSources());
        }

        void add(
                String sourcePath,
                String signatureRoot,
                String currentDonorCorpusRoot,
                M3ProblemCataloguePlanner.ShapePlan shapePlan) {
            if (shapePlan.shape() != shape
                    || !shapePlan.capabilityId().equals(capabilityId)
                    || shapePlan.recipeAction() != action) {
                throw new IllegalArgumentException(
                        "inconsistent recipe work for capability " + capabilityId);
            }
            if (!donorCorpusRoot.equals(sha(currentDonorCorpusRoot, "donorCorpusRoot"))) {
                throw new IllegalArgumentException(
                        "inconsistent donor corpus for capability " + capabilityId);
            }
            appendRecipes(shapePlan);
            donorJavaSources.addAll(shapePlan.javaCorpusSources());
            sources.add(
                    new SourceEvidence(
                            sourcePath,
                            signatureRoot,
                            shapePlan.root()));
        }

        private void appendRecipes(M3ProblemCataloguePlanner.ShapePlan shapePlan) {
            for (int index = 0; index < shapePlan.recipeCandidates().size(); index++) {
                String recipe = shapePlan.recipeCandidates().get(index);
                String root = shapePlan.recipeCandidateRoots().get(index);
                boolean duplicate = false;
                for (int existing = 0; existing < recipes.size(); existing++) {
                    if (recipes.get(existing).equals(recipe)
                            && recipeRoots.get(existing).equals(root)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    recipes.add(recipe);
                    recipeRoots.add(root);
                }
            }
        }

        WorkOrder freeze() {
            Mode mode =
                    switch (action) {
                        case DRY_RUN_RECIPE -> Mode.DRY_RUN_EXISTING;
                        case CREATE_OR_IMPROVE_RECIPE -> Mode.AUTHOR_OR_IMPROVE_RECIPE;
                        case HOLD_FOR_REINVENTORY ->
                                throw new IllegalStateException(
                                        "reinventoried atoms must not enter recipe work order");
                    };
            String stem = camel(shape.name());
            String className = "com.synexia.rewrite.M3" + stem + "CandidateRecipe";
            String declarative = "com.synexia.M3" + stem + "Candidate";
            String test = "com.synexia.rewrite.M3" + stem + "CandidateRecipeTest";
            String fixtures =
                    "synexia-openrewrite-recipes/src/test/resources/com/synexia/rewrite/algorithm/"
                            + shape.name().toLowerCase(Locale.ROOT).replace('_', '-')
                            + "/";
            List<Integer> order =
                    java.util.stream.IntStream.range(0, recipes.size())
                            .boxed()
                            .sorted(Comparator.comparing((Integer index) -> recipes.get(index))
                                    .thenComparing(index -> recipeRoots.get(index)))
                            .toList();
            List<String> existing =
                    order.stream().map(recipes::get).toList();
            List<String> existingRoots =
                    order.stream().map(recipeRoots::get).toList();
            List<SourceEvidence> orderedSources =
                    sources.stream()
                            .sorted(Comparator.comparing(SourceEvidence::sourcePath)
                                    .thenComparing(SourceEvidence::shapePlanRoot))
                            .toList();
            String root =
                    M3ProblemRecipeWorkOrder.root(
                            capabilityId,
                            shape,
                            mode,
                            className,
                            declarative,
                            test,
                            fixtures,
                            existing,
                            existingRoots,
                            List.copyOf(donorJavaSources),
                            donorCorpusRoot,
                            orderedSources);
            return new WorkOrder(
                    capabilityId,
                    shape,
                    mode,
                    className,
                    declarative,
                    test,
                    fixtures,
                    existing,
                    existingRoots,
                    List.copyOf(donorJavaSources),
                    donorCorpusRoot,
                    orderedSources,
                    gates(),
                    root);
        }
    }

    public static List<String> gates() {
        return List.of(
                "BALANCED_CHALLENGE_EVIDENCE",
                "PINNED_DONOR_PROVENANCE",
                "SERIAL_DONOR_FILE_REVIEW",
                "SERIAL_DONOR_ATOM_REVIEW",
                "SEALED_PARTIAL_AST_SCOPE_IF_DECLARED",
                "STALE_PREIMAGE_REJECTION_IF_AST_LEAF",
                "EXACT_POSTIMAGE_UNCHANGED_OUTSIDE_IF_AST_LEAF",
                "PARENT_SCOPE_FAN_IN_IF_AST_LEAF",
                "RECIPE_DRY_RUN",
                "DIFFERENTIAL_PROOF",
                "FORMAT_AND_STATIC_ANALYSIS",
                "COMPILER",
                "UNIT_TESTS",
                "INTEGRATION_TESTS",
                "API_AND_CONTRACT_TESTS",
                "RUNTIME_IF_REQUIRED",
                "JAVA_ORACLE",
                "NATIVE_PARITY_IF_DECLARED",
                "BENCHMARK_IF_PERFORMANCE_CLAIM",
                "SECOND_PASS_FIXED_POINT");
    }

    static String legacyRootV2(
            String capabilityId,
            AlgorithmShape shape,
            Mode mode,
            String recipeClass,
            String declarative,
            String testClass,
            String fixtures,
            List<String> recipes,
            List<String> recipeRoots,
            List<String> donorJavaSources,
            String donorCorpusRoot,
            List<SourceEvidence> sources) {
        MessageDigest digest = digest();
        frame(digest, "SYNEXIA_M3_PROBLEM_RECIPE_WORK_ORDER_V2");
        frame(digest, capabilityId);
        frame(digest, shape.name());
        frame(digest, mode.name());
        frame(digest, recipeClass);
        frame(digest, declarative);
        frame(digest, testClass);
        frame(digest, fixtures);
        for (int index = 0; index < recipes.size(); index++) {
            frame(digest, recipes.get(index));
            frame(digest, recipeRoots.get(index));
        }
        frame(digest, donorCorpusRoot);
        donorJavaSources.forEach(value -> frame(digest, value));
        sources.forEach(
                value -> {
                    frame(digest, value.sourcePath());
                    frame(digest, value.signatureRoot());
                    frame(digest, value.shapePlanRoot());
                });
        gates().forEach(value -> frame(digest, value));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String root(
            String capabilityId,
            AlgorithmShape shape,
            Mode mode,
            String recipeClass,
            String declarative,
            String testClass,
            String fixtures,
            List<String> recipes,
            List<String> recipeRoots,
            List<String> donorJavaSources,
            String donorCorpusRoot,
            List<SourceEvidence> sources) {
        MessageDigest digest = digest();
        frame(digest, "SYNEXIA_M3_PROBLEM_RECIPE_WORK_ORDER_V3");
        frame(digest, capabilityId);
        frame(digest, shape.name());
        frame(digest, mode.name());
        frame(digest, recipeClass);
        frame(digest, declarative);
        frame(digest, testClass);
        frame(digest, fixtures);
        for (int index = 0; index < recipes.size(); index++) {
            frame(digest, recipes.get(index));
            frame(digest, recipeRoots.get(index));
        }
        frame(digest, donorCorpusRoot);
        donorJavaSources.forEach(value -> frame(digest, value));
        M3ProblemDonorReviewReceipt.Receipt donorReview =
                M3ProblemDonorReviewReceipt.review(donorJavaSources, donorCorpusRoot);
        frame(digest, donorReview.serialReviewReportRoot());
        frame(digest, donorReview.root());
        sources.forEach(
                value -> {
                    frame(digest, value.sourcePath());
                    frame(digest, value.signatureRoot());
                    frame(digest, value.shapePlanRoot());
                });
        gates().forEach(value -> frame(digest, value));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String camel(String value) {
        StringBuilder result = new StringBuilder();
        for (String token : value.toLowerCase(Locale.ROOT).split("_")) {
            if (!token.isEmpty()) {
                result.append(Character.toUpperCase(token.charAt(0)))
                        .append(token.substring(1));
            }
        }
        return result.toString();
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
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

    private static List<String> ordered(List<String> values) {
        return Objects.requireNonNull(values, "values").stream()
                .map(value -> text(value, "value"))
                .toList();
    }

    private static List<String> hashes(List<String> values) {
        return Objects.requireNonNull(values, "values").stream()
                .map(value -> sha(value, "recipeCandidateRoot"))
                .toList();
    }

    private static List<String> stable(List<String> values) {
        return Objects.requireNonNull(values, "values").stream()
                .map(value -> text(value, "value"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String path(String value) {
        String result = text(value, "sourcePath").replace('\\', '/');
        if (result.startsWith("/") || result.contains("/../") || result.startsWith("../")) {
            throw new IllegalArgumentException("sourcePath");
        }
        return result;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }

    private static String optionalSha(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        return checked.isEmpty() ? "" : sha(checked, field);
    }
}
