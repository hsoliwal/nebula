// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.corpus.ChallengeBalancedEvidence;
import com.synexia.algorithms.corpus.ChallengeDonorEvidenceAudit;
import com.synexia.algorithms.corpus.ChallengeDonorPassLedger;
import com.synexia.algorithms.corpus.CompetitiveCapabilityPlanIndex;
import com.synexia.algorithms.corpus.CompetitiveProblemCategory;
import com.synexia.algorithms.corpus.JavaProblemCorpus;
import com.synexia.algorithms.corpus.PinnedAlgorithmDonorCatalog;
import com.synexia.algorithms.corpus.ProblemOptimizationCatalog;
import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.fastsearch.problem.ProblemCatalogue;
import com.synexia.fastsearch.problem.ProblemCategory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Deterministic exact-method -> competitive evidence -> Maven recipe-crate planner. */
public final class M3AlgorithmAtomRecipeCratePlanner {
    public record AtomInput(
            String sourcePath,
            String fileSourceSha256,
            String ownerType,
            String methodName,
            String methodDescriptor,
            String methodSource,
            String contractSurface) {
        public AtomInput {
            sourcePath = portable(sourcePath);
            fileSourceSha256 = sha(fileSourceSha256, "fileSourceSha256");
            ownerType = text(ownerType, "ownerType");
            methodName = text(methodName, "methodName");
            methodDescriptor = text(methodDescriptor, "methodDescriptor");
            methodSource = Objects.requireNonNull(methodSource, "methodSource");
            if (methodSource.isBlank() || methodSource.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("methodSource");
            }
            contractSurface = text(contractSurface, "contractSurface");
        }

        public M3AlgorithmAtomRecipeCrateBinding.AtomIdentity identity() {
            return new M3AlgorithmAtomRecipeCrateBinding.AtomIdentity(
                    sourcePath,
                    fileSourceSha256,
                    ownerType,
                    methodName,
                    methodDescriptor,
                    M3SourceCoverageGate.hash(methodSource),
                    contractSurface,
                    "");
        }

        String classifierPath() {
            return sourcePath + "#" + ownerType + "." + methodName + methodDescriptor;
        }
    }

    public record FileInput(
            String sourcePath,
            String fileSourceSha256,
            int methodBodies,
            int unresolvedMethodBodies,
            int nativeDeclarations) {
        public FileInput {
            sourcePath = portable(sourcePath);
            fileSourceSha256 = sha(fileSourceSha256, "fileSourceSha256");
            if (methodBodies < 0 || unresolvedMethodBodies < 0 || nativeDeclarations < 0
                    || unresolvedMethodBodies > methodBodies) {
                throw new IllegalArgumentException("file inventory counts");
            }
        }
    }

    public record FileCoverage(
            int fileOrdinal,
            String sourcePath,
            String fileSourceSha256,
            int methodBodies,
            int classifiedMethods,
            int candidateBindings,
            int unresolvedMethods,
            int nativeDeclarations,
            String root) {
        public FileCoverage {
            if (fileOrdinal < 0 || methodBodies < 0 || classifiedMethods < 0
                    || candidateBindings < 0 || unresolvedMethods < 0 || nativeDeclarations < 0
                    || classifiedMethods > methodBodies) {
                throw new IllegalArgumentException("coverage counts");
            }
            sourcePath = portable(sourcePath);
            fileSourceSha256 = sha(fileSourceSha256, "fileSourceSha256");
            String expected = digest(
                    "M3_ALGORITHM_ATOM_RECIPE_FILE_COVERAGE_V1",
                    Integer.toString(fileOrdinal), sourcePath, fileSourceSha256,
                    Integer.toString(methodBodies), Integer.toString(classifiedMethods),
                    Integer.toString(candidateBindings), Integer.toString(unresolvedMethods),
                    Integer.toString(nativeDeclarations));
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("coverage root mismatch");
        }
    }

    public record Plan(
            List<M3AlgorithmAtomRecipeCrateBinding> bindings,
            List<FileCoverage> files,
            String recipeCatalogueRoot,
            String problemCatalogueRoot,
            String donorCorpusRoot,
            String donorManifestRoot,
            String invariantId,
            String root) {
        public Plan {
            bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
            files = List.copyOf(Objects.requireNonNull(files, "files"));
            recipeCatalogueRoot = sha(recipeCatalogueRoot, "recipeCatalogueRoot");
            problemCatalogueRoot = sha(problemCatalogueRoot, "problemCatalogueRoot");
            donorCorpusRoot = sha(donorCorpusRoot, "donorCorpusRoot");
            donorManifestRoot = sha(donorManifestRoot, "donorManifestRoot");
            invariantId = text(invariantId, "invariantId");
            String challengeDonorLedgerRoot =
                    ChallengeDonorPassLedger.review(null, java.util.Map.of(), null).root();
            if (bindings.stream().anyMatch(
                    binding -> !binding.challengeDonorLedgerRoot().equals(challengeDonorLedgerRoot))) {
                throw new IllegalArgumentException("challenge donor ledger root drift");
            }
            String expected = digest(
                    "M3_ALGORITHM_ATOM_RECIPE_CRATE_PLAN_V3",
                    recipeCatalogueRoot,
                    problemCatalogueRoot,
                    donorCorpusRoot,
                    donorManifestRoot,
                    challengeDonorLedgerRoot,
                    invariantId,
                    bindings.stream().map(M3AlgorithmAtomRecipeCrateBinding::root)
                            .reduce("", (a, b) -> a + b),
                    files.stream().map(FileCoverage::root).reduce("", (a, b) -> a + b));
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("plan root mismatch");
        }
        public String challengeDonorLedgerRoot() {
            return ChallengeDonorPassLedger.review(null, java.util.Map.of(), null).root();
        }

        public M3AlgorithmAtomPlanGate.Receipt verifiedReceipt() {
            return M3AlgorithmAtomPlanGate.verify(this);
        }
        public boolean sourceMutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    public Plan plan(
            List<AtomInput> inputs,
            List<FileInput> files,
            M3RecipeCatalogue recipeCatalogue,
            ProblemCatalogue problemCatalogue,
            int evidenceLimit) {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(files, "files");
        Objects.requireNonNull(recipeCatalogue, "recipeCatalogue");
        ProblemCatalogue checkedProblems = Objects.requireNonNull(problemCatalogue, "problemCatalogue");
        if (evidenceLimit < 1 || evidenceLimit > 1000) throw new IllegalArgumentException("evidenceLimit");

        List<AtomInput> ordered = inputs.stream()
                .sorted(Comparator.comparing(AtomInput::sourcePath)
                        .thenComparing(AtomInput::ownerType)
                        .thenComparing(AtomInput::methodName)
                        .thenComparing(AtomInput::methodDescriptor)
                        .thenComparing(input -> M3SourceCoverageGate.hash(input.methodSource())))
                .toList();
        List<FileInput> orderedFiles = files.stream()
                .sorted(Comparator.comparing(FileInput::sourcePath))
                .toList();
        if (orderedFiles.stream().map(FileInput::sourcePath).distinct().count() != orderedFiles.size()) {
            throw new IllegalArgumentException("duplicate file inventory path");
        }
        java.util.Map<String, FileInput> filesByPath = orderedFiles.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(FileInput::sourcePath, value -> value));
        for (AtomInput input : ordered) {
            FileInput file = filesByPath.get(input.sourcePath());
            if (file == null || !file.fileSourceSha256().equals(input.fileSourceSha256())) {
                throw new IllegalArgumentException("atom input outside exact file inventory: " + input.sourcePath());
            }
        }

        ArrayList<M3AlgorithmAtomRecipeCrateBinding> bindings = new ArrayList<>();
        M3LlmTaskRecipeCratePlanner cratePlanner = new M3LlmTaskRecipeCratePlanner();
        String donorCorpusRoot = JavaProblemCorpus.resolvedIndexSnapshot().root();
        String donorManifestRoot = PinnedAlgorithmDonorCatalog.snapshot().root();
        String challengeDonorLedgerRoot =
                ChallengeDonorPassLedger.review(null, java.util.Map.of(), null).root();
        java.util.Map<String, Integer> classifiedByPath = new java.util.HashMap<>();
        java.util.Map<String, Integer> bindingsByPath = new java.util.HashMap<>();
        java.util.Map<String, Integer> atomOrdinalByPath = new java.util.HashMap<>();
        java.util.Map<String, Integer> fileOrdinalByPath = new java.util.HashMap<>();
        for (int i = 0; i < orderedFiles.size(); i++) {
            fileOrdinalByPath.put(orderedFiles.get(i).sourcePath(), i);
        }
        int serialOrdinal = 0;

        for (AtomInput input : ordered) {
            int fileOrdinal = fileOrdinalByPath.get(input.sourcePath());
            int atomOrdinal = atomOrdinalByPath.merge(input.sourcePath(), 1, Integer::sum) - 1;
            M3ProblemSignature signature =
                    M3ProblemSignature.fromSource(input.classifierPath(), input.methodSource());
            if (!signature.candidates().isEmpty()) {
                classifiedByPath.merge(input.sourcePath(), 1, Integer::sum);
            }
            int candidateOrdinal = 0;
            for (M3ProblemSignature.Candidate candidate : signature.candidates()) {
                AlgorithmShape shape = candidate.shape();
                CompetitiveCapabilityPlanIndex.Row capabilityPlan =
                        CompetitiveCapabilityPlanIndex.canonical().require(shape);
                CompetitiveProblemCategory competitiveCategory = capabilityPlan.category();
                ProblemCategory problemCategory =
                        ProblemCategory.valueOf(competitiveCategory.name());
                ChallengeBalancedEvidence.Snapshot balanced =
                        ChallengeBalancedEvidence.forShape(shape, evidenceLimit);
                ProblemOptimizationCatalog.Plan optimization =
                        capabilityPlan.optimization();
                ChallengeDonorEvidenceAudit.Snapshot donorAudit =
                        ChallengeDonorEvidenceAudit.forShape(shape, evidenceLimit);
                List<String> javaDonorEvidence = donorAudit.rows().stream()
                        .map(ChallengeDonorEvidenceAudit.EvidenceRow::evidenceId)
                        .sorted()
                        .toList();
                Set<String> donorRepositories = donorAudit.rows().stream()
                        .filter(ChallengeDonorEvidenceAudit.EvidenceRow::pinned)
                        .map(ChallengeDonorEvidenceAudit.EvidenceRow::repository)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
                String capabilityId =
                        M3AlgorithmAtomRecipeCrateBinding.capability(shape);
                M3AlgorithmAtomRecipeCrateBinding.AtomIdentity identity =
                        input.identity();
                String requirementRoot = digest(
                        "M3_ALGORITHM_ATOM_RECIPE_REQUIREMENT_V2",
                        M3RecipeFirstInvariant.INVARIANT_ID,
                        identity.root(),
                        shape.name(),
                        balanced.root(),
                        capabilityPlan.root(),
                        optimization.root(),
                        donorCorpusRoot,
                        donorManifestRoot,
                        challengeDonorLedgerRoot,
                        donorAudit.root());
                String taskId = "m3.algorithm.atom." + requirementRoot;
                M3CompetitiveProblemCrateEvidence competitive =
                        cratePlanner.planWithCompetitiveEvidenceDetailed(
                                taskId,
                                capabilityId,
                                requirementRoot,
                                recipeCatalogue,
                                Set.of(input.sourcePath()),
                                donorRepositories,
                                "ALL",
                                shape.name(),
                                "",
                                "",
                                evidenceLimit,
                                checkedProblems,
                                problemCategory);
                M3AlgorithmAtomRecipeCrateBinding.ExecutionLane lane =
                        M3AlgorithmAtomRecipeCrateBinding.lane(
                                optimization.nativeLane());
                M3AlgorithmAtomRecipeCrateBinding.Action action =
                        M3AlgorithmAtomRecipeCrateBinding.action(
                                competitive.problemEvidence().crate(),
                                competitive,
                                lane);
                List<String> nativeDonors =
                        optimization.nativeMechanicsDonors().stream()
                                .map(match ->
                                        match.donor().repository()
                                                + "@"
                                                + match.donor().revision())
                                .distinct()
                                .sorted()
                                .toList();
                bindings.add(new M3AlgorithmAtomRecipeCrateBinding(
                        serialOrdinal++,
                        fileOrdinal,
                        atomOrdinal,
                        candidateOrdinal++,
                        identity,
                        shape,
                        candidate.score(),
                        candidate.evidence(),
                        problemCategory,
                        competitiveCategory,
                        balanced,
                        optimization,
                        javaDonorEvidence,
                        donorCorpusRoot,
                        nativeDonors,
                        competitive,
                        lane,
                        action,
                        M3AlgorithmAtomRecipeCrateBinding.gates(lane),
                        ""));
                bindingsByPath.merge(input.sourcePath(), 1, Integer::sum);
            }
        }

        ArrayList<FileCoverage> coverages = new ArrayList<>(orderedFiles.size());
        for (int fileOrdinal = 0; fileOrdinal < orderedFiles.size(); fileOrdinal++) {
            FileInput file = orderedFiles.get(fileOrdinal);
            long resolvedMethodBodies = ordered.stream()
                    .filter(input -> input.sourcePath().equals(file.sourcePath()))
                    .count();
            if (resolvedMethodBodies + file.unresolvedMethodBodies() != file.methodBodies()) {
                throw new IllegalArgumentException(
                        "file method-body coverage mismatch: " + file.sourcePath());
            }
            coverages.add(new FileCoverage(
                    fileOrdinal,
                    file.sourcePath(),
                    file.fileSourceSha256(),
                    file.methodBodies(),
                    classifiedByPath.getOrDefault(file.sourcePath(), 0),
                    bindingsByPath.getOrDefault(file.sourcePath(), 0),
                    file.unresolvedMethodBodies(),
                    file.nativeDeclarations(),
                    ""));
        }
        return new Plan(
                bindings,
                coverages,
                recipeCatalogue.root(),
                checkedProblems.root(),
                donorCorpusRoot,
                donorManifestRoot,
                M3RecipeFirstInvariant.INVARIANT_ID,
                "");
    }

    private static String portable(String value) {
        String checked = text(value, "sourcePath").replace('\\', '/');
        if (checked.startsWith("/") || checked.equals("..") || checked.startsWith("../")
                || checked.endsWith("/..") || checked.contains("/../")) {
            throw new IllegalArgumentException("sourcePath");
        }
        return checked;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0 || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked =
                Objects.requireNonNull(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes =
                        Objects.requireNonNull(value, "digest value")
                                .getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
