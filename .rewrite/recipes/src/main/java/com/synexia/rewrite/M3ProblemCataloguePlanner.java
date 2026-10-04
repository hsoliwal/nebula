// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.corpus.ChallengeBalancedEvidence;
import com.synexia.algorithms.corpus.ChallengeSearchPrimitiveCatalog;
import com.synexia.algorithms.corpus.CompetitiveProblemCategory;
import com.synexia.algorithms.corpus.CorpusSourceEntry;
import com.synexia.algorithms.corpus.JavaProblemCorpus;
import com.synexia.algorithms.corpus.ProblemOptimizationCatalog;
import com.synexia.fastsearch.problem.ProblemCatalogue;
import com.synexia.fastsearch.problem.ProblemCategory;
import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Deterministic file-local problem-catalogue to recipe planner.
 *
 * <p>Challenge sites provide category/problem evidence only. Local primitives, native donor
 * evidence and the M3 recipe catalogue are evaluated separately. No plan grants replacement
 * authority.</p>
 */
public final class M3ProblemCataloguePlanner {
    public enum Stage {
        DISCOVER,
        CATALOGUE,
        PINNED_JAVA_CORPUS,
        CLASSIFY,
        SERIAL_DONOR_FILE_REVIEW,
        PLAN_RECIPE,
        DRY_RUN,
        REVIEW_PASS,
        APPLY_CANDIDATE,
        VERIFY_CONTRACT,
        BENCHMARK,
        PROMOTE
    }

    public record ShapePlan(
            AlgorithmShape shape,
            int score,
            List<String> evidence,
            List<String> challengeCategories,
            List<String> challengeProblemIds,
            List<String> metadataProblemIds,
            List<String> javaCorpusSources,
            List<String> localPrimitiveIds,
            List<String> nativeDonors,
            ProblemOptimizationCatalog.NativeLane nativeLane,
            String capabilityId,
            M3RecipeCatalogue.Disposition recipeDisposition,
            M3RecipeFirstInvariant.Action recipeAction,
            List<String> recipeCandidates,
            List<String> recipeCandidateRoots,
            String root) {

        public ShapePlan {
            shape = Objects.requireNonNull(shape, "shape");
            if (score < 1) throw new IllegalArgumentException("score");
            evidence = stable(evidence);
            challengeCategories = stable(challengeCategories);
            challengeProblemIds = stable(challengeProblemIds);
            metadataProblemIds = stable(metadataProblemIds);
            javaCorpusSources = stable(javaCorpusSources);
            localPrimitiveIds = stable(localPrimitiveIds);
            nativeDonors = stable(nativeDonors);
            nativeLane = Objects.requireNonNull(nativeLane, "nativeLane");
            capabilityId = text(capabilityId, "capabilityId");
            recipeDisposition = Objects.requireNonNull(recipeDisposition, "recipeDisposition");
            recipeAction = Objects.requireNonNull(recipeAction, "recipeAction");
            recipeCandidates = ordered(recipeCandidates);
            recipeCandidateRoots = hashes(recipeCandidateRoots);
            if (recipeCandidates.size() != recipeCandidateRoots.size()) {
                throw new IllegalArgumentException("recipe candidate identity mismatch");
            }
            root = sha(root, "root");
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    public record Plan(
            M3ProblemSignature signature,
            List<ShapePlan> shapes,
            List<Stage> stages,
            String recipeCatalogueRoot,
            String javaCorpusIndexRoot,
            String root) {
        public Plan {
            signature = Objects.requireNonNull(signature, "signature");
            shapes = List.copyOf(Objects.requireNonNull(shapes, "shapes"));
            stages = List.copyOf(Objects.requireNonNull(stages, "stages"));
            if (!stages.equals(List.of(Stage.values()))) {
                throw new IllegalArgumentException("non-canonical M3 stages");
            }
            recipeCatalogueRoot = sha(recipeCatalogueRoot, "recipeCatalogueRoot");
            javaCorpusIndexRoot = sha(javaCorpusIndexRoot, "javaCorpusIndexRoot");
            root = sha(root, "root");
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean requiresRecipeWork() {
            return shapes.stream().anyMatch(
                    shape -> shape.recipeDisposition()
                            == M3RecipeCatalogue.Disposition.CREATE_OR_IMPROVE_RECIPE);
        }
    }

    private M3ProblemCataloguePlanner() {}

    public static Plan plan(
            M3ProblemSignature signature,
            M3RecipeCatalogue recipeCatalogue,
            int evidenceLimit) {
        return plan(signature, recipeCatalogue, null, evidenceLimit);
    }

    public static Plan plan(
            M3ProblemSignature signature,
            M3RecipeCatalogue recipeCatalogue,
            ProblemCatalogue metadataCatalogue,
            int evidenceLimit) {
        Objects.requireNonNull(signature, "signature");
        Objects.requireNonNull(recipeCatalogue, "recipeCatalogue");
        if (evidenceLimit < 1 || evidenceLimit > 1000) {
            throw new IllegalArgumentException("evidenceLimit");
        }

        ArrayList<ShapePlan> shapePlans = new ArrayList<>();
        for (M3ProblemSignature.Candidate candidate : signature.candidates()) {
            shapePlans.add(
                    shapePlan(candidate, recipeCatalogue, metadataCatalogue, evidenceLimit));
        }
        shapePlans.sort(
                Comparator.comparingInt(ShapePlan::score).reversed()
                        .thenComparing(plan -> plan.shape().name()));

        List<Stage> stages = List.of(Stage.values());
        String javaCorpusIndexRoot = JavaProblemCorpus.resolvedIndexSnapshot().root();
        String root =
                root(
                        signature,
                        shapePlans,
                        stages,
                        recipeCatalogue.root(),
                        javaCorpusIndexRoot);
        return new Plan(
                signature,
                shapePlans,
                stages,
                recipeCatalogue.root(),
                javaCorpusIndexRoot,
                root);
    }

    private static ShapePlan shapePlan(
            M3ProblemSignature.Candidate candidate,
            M3RecipeCatalogue recipeCatalogue,
            ProblemCatalogue metadataCatalogue,
            int evidenceLimit) {
        AlgorithmShape shape = candidate.shape();

        ChallengeBalancedEvidence.Snapshot challengeEvidence =
                ChallengeBalancedEvidence.forShape(shape, evidenceLimit);
        if (!challengeEvidence.balancedWithinLimit()) {
            throw new IllegalStateException(
                    "challenge evidence is not balanced within limit for " + shape);
        }
        List<String> categories = challengeEvidence.categoryIds();
        List<String> problemIds = challengeEvidence.problemIds();

        List<String> metadataProblemIds =
                metadataProblemIds(metadataCatalogue, shape, evidenceLimit);

        List<String> javaCorpusSources =
                JavaProblemCorpus.resolvedEvidenceByShape(shape, evidenceLimit).stream()
                        .map(CorpusSourceEntry::evidenceId)
                        .sorted()
                        .toList();

        List<String> localPrimitives =
                ChallengeSearchPrimitiveCatalog.forShape(shape).stream()
                        .map(ChallengeSearchPrimitiveCatalog.Primitive::id)
                        .sorted()
                        .toList();

        ProblemOptimizationCatalog.Plan optimization = ProblemOptimizationCatalog.plan(shape);
        List<String> nativeDonors =
                optimization.nativeMechanicsDonors().stream()
                        .map(match -> match.donor().repository() + "@" + match.donor().revision())
                        .distinct()
                        .sorted()
                        .toList();

        String capabilityId =
                "algorithm.shape." + shape.name().toLowerCase(Locale.ROOT);
        M3RecipeCatalogue.Resolution recipe = recipeCatalogue.resolveForLlmTask(capabilityId);
        M3RecipeFirstInvariant.Decision decision =
                M3RecipeFirstInvariant.admit(recipeCatalogue, capabilityId, List.of());
        List<M3RecipeDescriptor> orderedRecipes =
                recipe.candidates().stream()
                        .sorted(Comparator.comparing(M3RecipeDescriptor::recipeClassName)
                                .thenComparing(M3RecipeDescriptor::identitySha256))
                        .toList();
        List<String> recipeCandidates =
                orderedRecipes.stream()
                        .map(M3RecipeDescriptor::recipeClassName)
                        .toList();
        List<String> recipeCandidateRoots =
                orderedRecipes.stream()
                        .map(M3RecipeDescriptor::identitySha256)
                        .toList();

        String root =
                shapeRoot(
                        shape,
                        candidate.score(),
                        candidate.evidence(),
                        categories,
                        problemIds,
                        metadataProblemIds,
                        javaCorpusSources,
                        localPrimitives,
                        nativeDonors,
                        optimization.nativeLane(),
                        capabilityId,
                        recipe.disposition(),
                        decision.action(),
                        recipeCandidates,
                        recipeCandidateRoots,
                        challengeEvidence.root());
        return new ShapePlan(
                shape,
                candidate.score(),
                candidate.evidence(),
                categories,
                problemIds,
                metadataProblemIds,
                javaCorpusSources,
                localPrimitives,
                nativeDonors,
                optimization.nativeLane(),
                capabilityId,
                recipe.disposition(),
                decision.action(),
                recipeCandidates,
                recipeCandidateRoots,
                root);
    }

    private static String root(
            M3ProblemSignature signature,
            List<ShapePlan> shapes,
            List<Stage> stages,
            String recipeCatalogueRoot,
            String javaCorpusIndexRoot) {
        MessageDigest digest = digest();
        frame(digest, "SYNEXIA_M3_PROBLEM_RECIPE_PLAN_V2");
        frame(digest, signature.root());
        frame(digest, recipeCatalogueRoot);
        frame(digest, javaCorpusIndexRoot);
        for (ShapePlan shape : shapes) frame(digest, shape.root());
        for (Stage stage : stages) frame(digest, stage.name());
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String shapeRoot(
            AlgorithmShape shape,
            int score,
            List<String> evidence,
            List<String> categories,
            List<String> problems,
            List<String> metadataProblems,
            List<String> javaCorpusSources,
            List<String> primitives,
            List<String> donors,
            ProblemOptimizationCatalog.NativeLane lane,
            String capabilityId,
            M3RecipeCatalogue.Disposition disposition,
            M3RecipeFirstInvariant.Action action,
            List<String> recipeCandidates,
            List<String> recipeCandidateRoots,
            String challengeEvidenceRoot) {
        MessageDigest digest = digest();
        frame(digest, "SYNEXIA_M3_PROBLEM_SHAPE_PLAN_V3");
        frame(digest, shape.name());
        frame(digest, Integer.toString(score));
        evidence.forEach(value -> frame(digest, value));
        categories.forEach(value -> frame(digest, value));
        problems.forEach(value -> frame(digest, value));
        metadataProblems.forEach(value -> frame(digest, value));
        javaCorpusSources.forEach(value -> frame(digest, value));
        primitives.forEach(value -> frame(digest, value));
        donors.forEach(value -> frame(digest, value));
        frame(digest, lane.name());
        frame(digest, capabilityId);
        frame(digest, disposition.name());
        frame(digest, action.name());
        frame(digest, challengeEvidenceRoot);
        for (int index = 0; index < recipeCandidates.size(); index++) {
            frame(digest, recipeCandidates.get(index));
            frame(digest, recipeCandidateRoots.get(index));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static List<String> metadataProblemIds(
            ProblemCatalogue catalogue,
            AlgorithmShape shape,
            int evidenceLimit) {
        if (catalogue == null) return List.of();
        ProblemCategory category = metadataCategory(shape);
        if (category == null) return List.of();
        return catalogue.search(null, "", java.util.Set.of(category), evidenceLimit).stream()
                .map(problem -> problem.source().name() + ":" + problem.externalId())
                .toList();
    }

    private static ProblemCategory metadataCategory(AlgorithmShape shape) {
        String name = CompetitiveProblemCategory.fromShape(shape).name();
        try {
            return ProblemCategory.valueOf(name);
        } catch (IllegalArgumentException unsupported) {
            return null;
        }
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
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        values.stream().map(value -> text(value, "value")).sorted().forEach(unique::add);
        return List.copyOf(unique);
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
}
