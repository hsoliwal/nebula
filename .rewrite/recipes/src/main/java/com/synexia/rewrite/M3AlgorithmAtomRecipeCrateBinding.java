// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.corpus.ChallengeBalancedEvidence;
import com.synexia.algorithms.corpus.ChallengeDonorPassLedger;
import com.synexia.algorithms.corpus.CompetitiveCapabilityPlanIndex;
import com.synexia.algorithms.corpus.CompetitiveProblemCategory;
import com.synexia.algorithms.corpus.ProblemOptimizationCatalog;
import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.fastsearch.problem.ProblemCategory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Content-addressed custody for one method atom before serial recipe execution.
 *
 * <p>The binding joins the exact file/method preimage to the canonical algorithm shape,
 * three-site competitive-problem evidence, pinned Git donor evidence, admitted recipe crate and
 * Java/JNI execution lane. It is deliberately candidate-only: it cannot mutate or promote source.</p>
 */
public record M3AlgorithmAtomRecipeCrateBinding(
        int serialOrdinal,
        int fileOrdinal,
        int atomOrdinal,
        int candidateOrdinal,
        AtomIdentity atom,
        AlgorithmShape shape,
        int score,
        List<String> classifierEvidence,
        ProblemCategory problemCategory,
        CompetitiveProblemCategory competitiveCategory,
        ChallengeBalancedEvidence.Snapshot balancedEvidence,
        ProblemOptimizationCatalog.Plan optimization,
        List<String> javaDonorEvidence,
        String javaDonorCorpusRoot,
        List<String> nativeMechanicsDonors,
        M3CompetitiveProblemCrateEvidence competitiveEvidence,
        ExecutionLane executionLane,
        Action action,
        List<String> laneGates,
        String root) {

    public enum ExecutionLane {
        JAVA_ONLY,
        JAVA_PRIMARY_JNI_OPTIONAL,
        JAVA_ORACLE_JNI_CANDIDATE
    }

    public enum Action {
        AUTHOR_OR_IMPROVE_RECIPE,
        HOLD_UNPINNED_DONOR,
        SERIAL_JAVA_ATOM_REVIEW,
        SERIAL_JAVA_ATOM_REVIEW_THEN_JNI_CRATE
    }

    public record AtomIdentity(
            String sourcePath,
            String fileSourceSha256,
            String ownerType,
            String methodName,
            String methodDescriptor,
            String methodSourceSha256,
            String contractSurface,
            String root) {
        public AtomIdentity {
            sourcePath = path(sourcePath);
            fileSourceSha256 = sha(fileSourceSha256, "fileSourceSha256");
            ownerType = text(ownerType, "ownerType");
            methodName = text(methodName, "methodName");
            methodDescriptor = text(methodDescriptor, "methodDescriptor");
            methodSourceSha256 = sha(methodSourceSha256, "methodSourceSha256");
            contractSurface = text(contractSurface, "contractSurface");
            String expected = digest(
                    "M3_ALGORITHM_ATOM_IDENTITY_V1",
                    sourcePath,
                    fileSourceSha256,
                    ownerType,
                    methodName,
                    methodDescriptor,
                    methodSourceSha256,
                    contractSurface);
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("atom identity root mismatch");
            }
        }

        public String methodKey() {
            return ownerType + "#" + methodName + methodDescriptor;
        }
    }

    public M3AlgorithmAtomRecipeCrateBinding {
        if (serialOrdinal < 0 || fileOrdinal < 0 || atomOrdinal < 0 || candidateOrdinal < 0) {
            throw new IllegalArgumentException("negative serial ordinal");
        }
        atom = Objects.requireNonNull(atom, "atom");
        shape = Objects.requireNonNull(shape, "shape");
        if (score < 1 || score > 100) throw new IllegalArgumentException("score");
        classifierEvidence = stable(classifierEvidence);
        if (classifierEvidence.isEmpty()) throw new IllegalArgumentException("classifierEvidence");
        problemCategory = Objects.requireNonNull(problemCategory, "problemCategory");
        competitiveCategory = Objects.requireNonNull(competitiveCategory, "competitiveCategory");
        balancedEvidence = Objects.requireNonNull(balancedEvidence, "balancedEvidence");
        optimization = Objects.requireNonNull(optimization, "optimization");
        CompetitiveCapabilityPlanIndex.Row capabilityPlan =
                CompetitiveCapabilityPlanIndex.canonical().require(shape);
        if (capabilityPlan.category() != competitiveCategory
                || capabilityPlan.optimization().shape() != optimization.shape()
                || !capabilityPlan.optimization().root().equals(optimization.root())) {
            throw new IllegalArgumentException("canonical capability plan drift");
        }
        javaDonorEvidence = stable(javaDonorEvidence);
        javaDonorCorpusRoot = sha(javaDonorCorpusRoot, "javaDonorCorpusRoot");
        nativeMechanicsDonors = stable(nativeMechanicsDonors);
        M3NativeDonorSerialReview.Receipt nativeDonorReview =
                M3NativeDonorSerialReview.requireCanonical(
                        shape, optimization, nativeMechanicsDonors);
        String challengeDonorLedgerRoot =
                ChallengeDonorPassLedger.review(null, java.util.Map.of(), null).root();
        competitiveEvidence = Objects.requireNonNull(competitiveEvidence, "competitiveEvidence");
        executionLane = Objects.requireNonNull(executionLane, "executionLane");
        action = Objects.requireNonNull(action, "action");
        laneGates = List.copyOf(Objects.requireNonNull(laneGates, "laneGates"));
        if (!laneGates.equals(gates(executionLane))) {
            throw new IllegalArgumentException("non-canonical execution-lane gates");
        }

        if (competitiveCategory != CompetitiveProblemCategory.fromShape(shape)
                || problemCategory != ProblemCategory.valueOf(competitiveCategory.name())
                || optimization.shape() != shape
                || optimization.category() != competitiveCategory
                || executionLane != lane(optimization.nativeLane())) {
            throw new IllegalArgumentException("shape/category/native-lane drift");
        }
        if (!balancedEvidence.balancedWithinLimit()) {
            throw new IllegalArgumentException("unbalanced competitive evidence");
        }

        M3LlmTaskRecipeCrate crate = competitiveEvidence.problemEvidence().crate();
        String capabilityId = capability(shape);
        if (!crate.capabilityId().equals(capabilityId)
                || !crate.targetPaths().contains(atom.sourcePath())
                || competitiveEvidence.category() != problemCategory) {
            throw new IllegalArgumentException("competitive recipe crate is not bound to this atom");
        }
        M3RecipeFirstInvariant.requireLlmTaskRecipeCrate(crate);
        Action requiredAction = M3AlgorithmAtomRecipeCrateBinding.action(
                crate, competitiveEvidence, executionLane);
        if (action != requiredAction) {
            throw new IllegalArgumentException("action does not match recipe/donor/native admission");
        }

        String expected = digest(
                "M3_ALGORITHM_ATOM_RECIPE_CRATE_BINDING_V3",
                Integer.toString(serialOrdinal),
                Integer.toString(fileOrdinal),
                Integer.toString(atomOrdinal),
                Integer.toString(candidateOrdinal),
                atom.root(),
                shape.name(),
                Integer.toString(score),
                String.join("\u001f", classifierEvidence),
                problemCategory.name(),
                competitiveCategory.name(),
                balancedEvidence.root(),
                capabilityPlan.root(),
                optimization.root(),
                String.join("\u001f", javaDonorEvidence),
                javaDonorCorpusRoot,
                String.join("\u001f", nativeMechanicsDonors),
                nativeDonorReview.root(),
                challengeDonorLedgerRoot,
                competitiveEvidence.root(),
                competitiveEvidence.problemEvidence().donorEvidence().root(),
                M3LlmTaskRecipeCrateCodec.root(crate),
                executionLane.name(),
                action.name(),
                String.join("\u001f", laneGates),
                "sourceMutationAuthority=false",
                "donorSourceCopyAuthority=false",
                "promotionAuthority=false");
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!expected.equals(root)) throw new IllegalArgumentException("binding root mismatch");
    }

    public M3LlmTaskRecipeCrate crate() {
        return competitiveEvidence.problemEvidence().crate();
    }

    public boolean allDonorsPinned() {
        return competitiveEvidence.problemEvidence().donorEvidence().allRepositoriesPinned();
    }

    public String donorEvidenceRoot() {
        return competitiveEvidence.problemEvidence().donorEvidence().root();
    }

    /** Canonical joined shape/category/taxonomy/optimization plan used by this binding. */
    public String capabilityPlanRoot() {
        return CompetitiveCapabilityPlanIndex.canonical().require(shape).root();
    }

    /** Canonical serial LeetCode -> HackerRank -> GeeksforGeeks donor-pass ledger identity. */
    public String challengeDonorLedgerRoot() {
        return ChallengeDonorPassLedger.review(null, java.util.Map.of(), null).root();
    }

    public String crateRoot() {
        return M3LlmTaskRecipeCrateCodec.root(crate());
    }

    public M3NativeDonorSerialReview.Receipt nativeDonorReviewReceipt() {
        return M3NativeDonorSerialReview.requireCanonical(
                shape, optimization, nativeMechanicsDonors);
    }

    public void requireSerialReviewReady() {
        if (action != Action.SERIAL_JAVA_ATOM_REVIEW
                && action != Action.SERIAL_JAVA_ATOM_REVIEW_THEN_JNI_CRATE) {
            throw new IllegalStateException("atom binding is not admitted for serial review: " + action);
        }
        nativeDonorReviewReceipt();
    }

    public boolean jniFollowupRequired() {
        return executionLane != ExecutionLane.JAVA_ONLY;
    }

    /**
     * Binds this exact planned atom to an executed source-bound serial review.
     *
     * <p>The returned receipt is evidence-only. SerialAtomReview remains the Java external-contract
     * authority and M3SourceCoverageGate remains the source-bound verifier.</p>
     */
    public M3AlgorithmAtomExecutedContractReceipt requireExecutedContract(
            M3RecipeCrateSerialAtomReview.SourceBoundCandidate executed) {
        return M3AlgorithmAtomExecutedContractReceipt.admit(this, executed);
    }

    public boolean sourceMutationAuthority() { return false; }
    public boolean donorSourceCopyAuthority() { return false; }
    public boolean promotionAuthority() { return false; }

    public static String capability(AlgorithmShape shape) {
        return "algorithm.shape." + Objects.requireNonNull(shape, "shape").name().toLowerCase(Locale.ROOT);
    }

    public static ExecutionLane lane(ProblemOptimizationCatalog.NativeLane lane) {
        return switch (Objects.requireNonNull(lane, "lane")) {
            case JAVA_ONLY -> ExecutionLane.JAVA_ONLY;
            case JAVA_PRIMARY_JNI_OPTIONAL -> ExecutionLane.JAVA_PRIMARY_JNI_OPTIONAL;
            case JNI_CANDIDATE -> ExecutionLane.JAVA_ORACLE_JNI_CANDIDATE;
        };
    }

    public static Action action(
            M3LlmTaskRecipeCrate crate,
            M3CompetitiveProblemCrateEvidence evidence,
            ExecutionLane executionLane) {
        Objects.requireNonNull(crate, "crate");
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(executionLane, "executionLane");
        if (crate.disposition() == M3LlmTaskRecipeCrate.Disposition.CREATE_OR_IMPROVE_RECIPE) {
            return Action.AUTHOR_OR_IMPROVE_RECIPE;
        }
        if (!evidence.problemEvidence().donorEvidence().allRepositoriesPinned()) {
            return Action.HOLD_UNPINNED_DONOR;
        }
        return executionLane == ExecutionLane.JAVA_ONLY
                ? Action.SERIAL_JAVA_ATOM_REVIEW
                : Action.SERIAL_JAVA_ATOM_REVIEW_THEN_JNI_CRATE;
    }

    public static List<String> gates(ExecutionLane lane) {
        List<String> base = List.of(
                "EXACT_FILE_AND_METHOD_PREIMAGE",
                "CONTRACT_PRESERVATION",
                "BALANCED_LEETCODE_HACKERRANK_GFG_EVIDENCE",
                "DONOR_LICENSE_PROVENANCE_CONSISTENCY",
                "PINNED_GITHUB_DONOR_PROVENANCE",
                "TRI_PLATFORM_SERIAL_DONOR_LEDGER_ROOT",
                "RECIPE_CRATE_ADMISSION",
                "SERIAL_ONE_ATOM_REVIEW",
                "JAVA_REFERENCE_ORACLE");
        if (lane == ExecutionLane.JAVA_ONLY) {
            return java.util.stream.Stream.concat(
                            base.stream(),
                            java.util.stream.Stream.of("SECOND_PASS_FIXED_POINT"))
                    .toList();
        }
        return java.util.stream.Stream.concat(
                        base.stream(),
                        java.util.stream.Stream.of(
                                "JNI_CONTRACT_INVENTORY",
                                "NATIVE_DONOR_SERIAL_REVIEW_RECEIPT",
                                "NATIVE_RECIPE_CRATE_ADMISSION",
                                "NATIVE_SOURCE_PROVENANCE",
                                "NATIVE_BUILD",
                                "JNI_LINKAGE",
                                "JAVA_JNI_DIFFERENTIAL",
                                "JAVA_FALLBACK",
                                "SECOND_PASS_FIXED_POINT"))
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
        String checked = Objects.requireNonNull(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.requireNonNull(value, "digest value").getBytes(StandardCharsets.UTF_8);
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
