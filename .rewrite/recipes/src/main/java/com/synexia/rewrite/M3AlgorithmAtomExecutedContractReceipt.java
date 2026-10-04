// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.m3.contract.CodeAtom;
import com.synexia.m3.contract.CodeAtomKind;
import com.synexia.m3.contract.SerialAtomReview;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Executed proof that one planned competitive-problem atom survived the canonical M3 contract lane.
 *
 * <p>This receipt does not grant mutation or promotion authority. It joins an exact algorithm
 * plan to already-executed SerialAtomReview and M3SourceCoverageGate evidence.</p>
 */
public record M3AlgorithmAtomExecutedContractReceipt(
        String bindingRoot,
        String atomRoot,
        String crateRoot,
        String serialResultRoot,
        String initialSourceSha256,
        String finalSourceSha256,
        String interfaceRoot,
        int acceptedPasses,
        List<String> acceptedDecisionRoots,
        List<String> sourceBoundCoverageRoots,
        boolean fixedPoint,
        boolean jniFollowupRequired,
        String root) {

    public M3AlgorithmAtomExecutedContractReceipt {
        bindingRoot = sha(bindingRoot, "bindingRoot");
        atomRoot = sha(atomRoot, "atomRoot");
        crateRoot = sha(crateRoot, "crateRoot");
        serialResultRoot = sha(serialResultRoot, "serialResultRoot");
        initialSourceSha256 = sha(initialSourceSha256, "initialSourceSha256");
        finalSourceSha256 = sha(finalSourceSha256, "finalSourceSha256");
        interfaceRoot = sha(interfaceRoot, "interfaceRoot");
        if (acceptedPasses < 1) throw new IllegalArgumentException("acceptedPasses");
        acceptedDecisionRoots = roots(acceptedDecisionRoots, "acceptedDecisionRoots");
        sourceBoundCoverageRoots = roots(sourceBoundCoverageRoots, "sourceBoundCoverageRoots");
        if (acceptedDecisionRoots.size() != acceptedPasses) {
            throw new IllegalArgumentException("accepted decision/pass count mismatch");
        }
        if (sourceBoundCoverageRoots.size() != acceptedPasses + 1) {
            throw new IllegalArgumentException("source-bound coverage/pass count mismatch");
        }
        if (!fixedPoint) throw new IllegalArgumentException("fixedPoint");

        String expected =
                digest(
                        "M3_ALGORITHM_ATOM_EXECUTED_CONTRACT_V1",
                        bindingRoot,
                        atomRoot,
                        crateRoot,
                        serialResultRoot,
                        initialSourceSha256,
                        finalSourceSha256,
                        interfaceRoot,
                        Integer.toString(acceptedPasses),
                        String.join("\u001f", acceptedDecisionRoots),
                        String.join("\u001f", sourceBoundCoverageRoots),
                        Boolean.toString(fixedPoint),
                        Boolean.toString(jniFollowupRequired),
                        "sourceMutationAuthority=false",
                        "donorSourceCopyAuthority=false",
                        "recipePromotionAuthority=false",
                        "repositoryPromotionAuthority=false",
                        "mergeAuthority=false");
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!expected.equals(root)) {
            throw new IllegalArgumentException("executed algorithm contract receipt root mismatch");
        }
    }

    public static M3AlgorithmAtomExecutedContractReceipt admit(
            M3AlgorithmAtomRecipeCrateBinding binding,
            M3RecipeCrateSerialAtomReview.SourceBoundCandidate executed) {
        M3AlgorithmAtomRecipeCrateBinding planned =
                Objects.requireNonNull(binding, "binding");
        M3RecipeCrateSerialAtomReview.SourceBoundCandidate proof =
                Objects.requireNonNull(executed, "executed");

        planned.requireSerialReviewReady();
        if (!proof.fixedPoint()) {
            throw new IllegalArgumentException("executed atom review has no source-bound fixed point");
        }

        M3RecipeCrateSerialAtomReview.ReviewedCandidate reviewed = proof.candidate();
        if (!reviewed.receipt().stable()) {
            throw new IllegalArgumentException("executed atom review is not serially stable");
        }
        if (!planned.crateRoot().equals(reviewed.receipt().crateRoot())) {
            throw new IllegalArgumentException("executed recipe crate does not match planned atom");
        }
        if (!planned.atom().sourcePath().equals(reviewed.receipt().sourcePath())) {
            throw new IllegalArgumentException("executed source path does not match planned atom");
        }
        if (!reviewed.receipt().serialResultRoot().equals(reviewed.serialResult().root())) {
            throw new IllegalArgumentException("serial result root does not match executed receipt");
        }

        List<SerialAtomReview.Decision> accepted =
                reviewed.serialResult().decisions().stream()
                        .filter(SerialAtomReview.Decision::accepted)
                        .toList();
        if (accepted.isEmpty()
                || accepted.size() != reviewed.receipt().acceptedPasses()) {
            throw new IllegalArgumentException("accepted serial pass count mismatch");
        }

        SerialAtomReview.Decision first = accepted.getFirst();
        M3AlgorithmAtomRecipeCrateBinding.AtomIdentity atom = planned.atom();
        if (!atom.fileSourceSha256().equals(first.beforeSha256())) {
            throw new IllegalArgumentException("executed file preimage does not match planned file");
        }
        requirePlannedFirstAtom(atom, first.atom());

        String interfaceRoot = first.beforeInvariant().interfaceRoot();
        ArrayList<String> decisionRoots = new ArrayList<>(accepted.size());
        for (SerialAtomReview.Decision decision : accepted) {
            requireSameMethod(atom, decision.atom());
            if (!decision.contractDiff().compatible()) {
                throw new IllegalArgumentException("accepted serial decision has incompatible contract diff");
            }
            if (!interfaceRoot.equals(decision.beforeInvariant().interfaceRoot())
                    || !interfaceRoot.equals(decision.afterInvariant().interfaceRoot())) {
                throw new IllegalArgumentException("accepted serial decision changed interface root");
            }
            decisionRoots.add(sha(decision.root(), "decisionRoot"));
        }
        if (!interfaceRoot.equals(reviewed.serialResult().invariant().interfaceRoot())) {
            throw new IllegalArgumentException("final serial invariant changed interface root");
        }

        String finalSource = M3SourceCoverageGate.hash(reviewed.source());
        List<M3SourceCoverageGate.Result> coverage = proof.coverage();
        if (coverage.size() != accepted.size() + 1) {
            throw new IllegalArgumentException("source-bound coverage does not align with serial passes");
        }

        ArrayList<String> coverageRoots = new ArrayList<>(coverage.size());
        for (int index = 0; index < accepted.size(); index++) {
            M3SourceCoverageGate.Result sourceProof = coverage.get(index);
            SerialAtomReview.Decision decision = accepted.get(index);
            if (sourceProof.status() != M3SourceCoverageGate.Status.VERIFIED_CHANGE
                    || !atom.sourcePath().equals(sourceProof.path())
                    || !decision.beforeSha256().equals(sourceProof.beforeRoot())
                    || !decision.afterSha256().equals(sourceProof.afterRoot())) {
                throw new IllegalArgumentException(
                        "source-bound verified change does not align with accepted serial decision");
            }
            coverageRoots.add(coverageRoot(sourceProof));
        }

        M3SourceCoverageGate.Result terminal = coverage.getLast();
        if (terminal.status() != M3SourceCoverageGate.Status.NO_OP
                || !atom.sourcePath().equals(terminal.path())
                || !finalSource.equals(terminal.beforeRoot())
                || !finalSource.equals(terminal.afterRoot())) {
            throw new IllegalArgumentException("terminal source-bound proof is not the final no-op");
        }
        coverageRoots.add(coverageRoot(terminal));

        String initial = accepted.getFirst().beforeSha256();
        if (!initial.equals(atom.fileSourceSha256())) {
            throw new IllegalArgumentException("initial source root drift");
        }

        return new M3AlgorithmAtomExecutedContractReceipt(
                planned.root(),
                atom.root(),
                planned.crateRoot(),
                reviewed.serialResult().root(),
                initial,
                finalSource,
                interfaceRoot,
                accepted.size(),
                decisionRoots,
                coverageRoots,
                true,
                planned.jniFollowupRequired(),
                "");
    }

    public boolean javaContractVerified() {
        return fixedPoint;
    }

    public boolean sourceMutationAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean recipePromotionAuthority() {
        return false;
    }

    public boolean repositoryPromotionAuthority() {
        return false;
    }

    public boolean mergeAuthority() {
        return false;
    }

    private static void requirePlannedFirstAtom(
            M3AlgorithmAtomRecipeCrateBinding.AtomIdentity planned,
            CodeAtom actual) {
        requireSameMethod(planned, actual);
        if (!planned.methodSourceSha256().equals(actual.sha256())) {
            throw new IllegalArgumentException("executed method preimage does not match planned method");
        }
    }

    private static void requireSameMethod(
            M3AlgorithmAtomRecipeCrateBinding.AtomIdentity planned,
            CodeAtom actual) {
        if (!planned.sourcePath().equals(actual.sourcePath())
                || !planned.ownerType().equals(actual.owner())
                || actual.kind() != CodeAtomKind.METHOD
                || !planned.methodName().equals(actual.name())) {
            throw new IllegalArgumentException("executed serial atom is not the planned method");
        }
    }

    private static String coverageRoot(M3SourceCoverageGate.Result result) {
        ArrayList<String> values = new ArrayList<>();
        values.add("M3_ALGORITHM_ATOM_SOURCE_BOUND_PROOF_V1");
        values.add(result.status().name());
        values.add(result.reason());
        values.add(result.path());
        values.add(result.beforeRoot());
        values.add(result.afterRoot());
        values.add(
                result.request() == null
                        ? "-"
                        : Objects.requireNonNull(result.request().root(), "requestRoot"));

        for (M3RecipePassReceipt.AtomResult atom : result.atoms()) {
            M3CodeAtom state = atom.state();
            values.add(
                    String.join(
                            "\u001e",
                            state.atomId(),
                            state.sourcePath(),
                            state.symbol(),
                            state.contractSha256(),
                            state.implementationSha256(),
                            state.state().name(),
                            Boolean.toString(atom.changed()),
                            atom.postContractSha256()));
        }
        for (M3SourceCoverageGate.Evidence evidence : result.evidence()) {
            values.add(
                    String.join(
                            "\u001e",
                            evidence.stage().name(),
                            evidence.outcome().name(),
                            evidence.requestRoot(),
                            evidence.artifactRoot(),
                            evidence.verifierId()));
        }
        return digest(values.toArray(String[]::new));
    }

    private static List<String> roots(List<String> values, String field) {
        List<String> checked = List.copyOf(Objects.requireNonNull(values, field));
        if (checked.isEmpty()) throw new IllegalArgumentException(field);
        for (String value : checked) sha(value, field);
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
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
