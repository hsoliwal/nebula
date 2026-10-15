// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Content-addressed Java-grade qualification over the canonical Nebula M3 proof stack.
 *
 * <p>This class does not run recipes, builds or tests. It binds evidence produced by those systems
 * and fails closed when any root/scope/orchestration contract drifts. Even a qualified receipt
 * grants no source-mutation, merge or release-promotion authority.</p>
 */
public final class NebulaM3JavaGradeQualification {
    public static final String SCHEMA = "NEBULA_M3_JAVA_GRADE_V1";
    public static final double MINIMUM_SEMANTIC_LINE_COVERAGE = 0.99d;

    public record Receipt(
            NebulaM3ProofGate.Receipt proof,
            String transferRoot,
            String orchestratorPlansRoot,
            List<NebulaM3RefactorScope> scopeOrder,
            Set<NebulaM3RecipeDagManifest.Orchestrator> orchestrators) {
        public Receipt {
            proof = Objects.requireNonNull(proof, "proof");
            transferRoot = sha(transferRoot, "transferRoot");
            orchestratorPlansRoot = sha(orchestratorPlansRoot, "orchestratorPlansRoot");
            scopeOrder = List.copyOf(Objects.requireNonNull(scopeOrder, "scopeOrder"));
            orchestrators = Set.copyOf(Objects.requireNonNull(orchestrators, "orchestrators"));

            if (!NebulaM3RecipeDagManifest.root().equals(proof.dagRoot())) {
                throw new IllegalArgumentException("proof DAG root does not match canonical DAG");
            }
            if (!NebulaM3TransferContract.root().equals(transferRoot)) {
                throw new IllegalArgumentException("transfer root drift");
            }
            if (!NebulaM3OrchestratorPlans.root().equals(orchestratorPlansRoot)) {
                throw new IllegalArgumentException("orchestrator plans root drift");
            }
            if (!scopeOrder.equals(NebulaM3RefactorScope.canonicalOrder())) {
                throw new IllegalArgumentException("refactor scope order drift");
            }
            if (!orchestrators.equals(NebulaM3RecipeDagManifest.orchestrationTargets())) {
                throw new IllegalArgumentException("orchestrator target set drift");
            }
            if (NebulaM3TransferContract.JAVA_RELEASE != 21
                    || !"8.90.4".equals(NebulaM3TransferContract.OPENREWRITE_VERSION)) {
                throw new IllegalArgumentException("Java/OpenRewrite baseline drift");
            }
            if (NebulaM3RecipeDagManifest.orchestratorMutationAuthority()
                    || NebulaM3RecipeDagManifest.orchestratorPromotionAuthority()
                    || NebulaM3TransferContract.directSourceMutationAuthority()
                    || NebulaM3TransferContract.promotionAuthority()) {
                throw new IllegalArgumentException("authority drift");
            }
        }

        /**
         * True only when every hosted proof stage is PASS and all canonical content roots agree.
         * This means ready for manual transfer review, never auto-merge or auto-promotion.
         */
        public boolean qualified() {
            return proof.complete();
        }

        public String disposition() {
            return qualified() ? "MANUAL_TRANSFER_REVIEW_REQUIRED" : "HOLD";
        }

        public boolean mutationAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }

        public String root() {
            StringBuilder framed = new StringBuilder();
            append(framed, SCHEMA);
            append(framed, Integer.toString(NebulaM3TransferContract.JAVA_RELEASE));
            append(framed, NebulaM3TransferContract.OPENREWRITE_VERSION);
            append(framed, Double.toString(MINIMUM_SEMANTIC_LINE_COVERAGE));
            append(framed, proof.root());
            append(framed, proof.dagRoot());
            append(framed, transferRoot);
            append(framed, orchestratorPlansRoot);
            append(framed, NebulaM3TransferContract.refactorScopeOrder());
            orchestrators.stream()
                    .sorted(java.util.Comparator.comparing(Enum::name))
                    .forEach(value -> append(framed, value.name()));
            NebulaM3TransferContract.targets().forEach(
                    target -> {
                        append(framed, target.repository());
                        append(framed, target.adapterLane());
                    });
            append(framed, disposition());
            append(framed, "mutation=false;promotion=false");
            return sha256(framed.toString());
        }

        public String toTsv() {
            return """
                    key	value
                    schema	%s
                    javaRelease	%d
                    openRewriteVersion	%s
                    minimumSemanticLineCoverage	%.2f
                    proofRoot	%s
                    dagRoot	%s
                    transferRoot	%s
                    orchestratorPlansRoot	%s
                    refactorScopeOrder	%s
                    orchestrators	%s
                    transferTargets	%s
                    disposition	%s
                    mutationAuthority	false
                    promotionAuthority	false
                    qualificationRoot	%s
                    """
                    .formatted(
                            SCHEMA,
                            NebulaM3TransferContract.JAVA_RELEASE,
                            NebulaM3TransferContract.OPENREWRITE_VERSION,
                            MINIMUM_SEMANTIC_LINE_COVERAGE,
                            proof.root(),
                            proof.dagRoot(),
                            transferRoot,
                            orchestratorPlansRoot,
                            NebulaM3TransferContract.refactorScopeOrder(),
                            orchestrators.stream()
                                    .sorted(java.util.Comparator.comparing(Enum::name))
                                    .map(Enum::name)
                                    .collect(java.util.stream.Collectors.joining(",")),
                            NebulaM3TransferContract.targets().stream()
                                    .map(NebulaM3TransferContract.Target::repository)
                                    .collect(java.util.stream.Collectors.joining(",")),
                            disposition(),
                            root());
        }
    }

    private NebulaM3JavaGradeQualification() {}

    public static Receipt fromProof(NebulaM3ProofGate.Receipt proof) {
        return new Receipt(
                proof,
                NebulaM3TransferContract.root(),
                NebulaM3OrchestratorPlans.root(),
                NebulaM3RefactorScope.canonicalOrder(),
                NebulaM3RecipeDagManifest.orchestrationTargets());
    }

    public static Receipt pending() {
        return fromProof(NebulaM3ProofGate.pending(NebulaM3RecipeDagManifest.root()));
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }

    private static void append(StringBuilder target, String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        target.append(bytes.length).append(':').append(value);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
