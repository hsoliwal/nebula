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
 * Content-addressed promotion receipt joining work-order, recipe catalogue and verification proof.
 *
 * <p>All required canonical gates need explicit evidence. Gates already represented by
 * {@link M3RecipePassReceipt} are cross-checked against that receipt so caller evidence cannot
 * override compiler/test/oracle/fixed-point failures.</p>
 */
public record M3RecipePromotionReceipt(
        String capabilityId,
        String workRoot,
        String recipeDescriptorRoot,
        String recipeCatalogueRoot,
        String recipePassRoot,
        M3RecipeGateSet requiredGates,
        List<GateEvidence> evidence,
        boolean promotable,
        String root) {

    public record GateEvidence(M3RecipeGate gate, boolean passed, String evidenceSha256) {
        public GateEvidence {
            gate = Objects.requireNonNull(gate, "gate");
            evidenceSha256 = sha(evidenceSha256, "evidenceSha256");
        }
    }

    public M3RecipePromotionReceipt {
        capabilityId = token(capabilityId, "capabilityId");
        workRoot = sha(workRoot, "workRoot");
        recipeDescriptorRoot = sha(recipeDescriptorRoot, "recipeDescriptorRoot");
        recipeCatalogueRoot = sha(recipeCatalogueRoot, "recipeCatalogueRoot");
        recipePassRoot = sha(recipePassRoot, "recipePassRoot");
        requiredGates = Objects.requireNonNull(requiredGates, "requiredGates");
        evidence = normalizeEvidence(evidence);
        validateCoverage(requiredGates, evidence);
        boolean computedPromotable = evidence.stream().allMatch(GateEvidence::passed);
        if (promotable != computedPromotable) {
            throw new IllegalArgumentException("promotable flag mismatch");
        }
        String expected = root(
                capabilityId,
                workRoot,
                recipeDescriptorRoot,
                recipeCatalogueRoot,
                recipePassRoot,
                requiredGates,
                evidence,
                promotable);
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!expected.equals(root)) {
            throw new IllegalArgumentException("promotion receipt root mismatch");
        }
    }

    public static M3RecipePromotionReceipt evaluateProblem(
            M3ProblemRecipeWorkOrder.WorkOrder workOrder,
            M3RecipeDescriptor descriptor,
            M3RecipeCatalogue catalogue,
            M3RecipePassReceipt pass,
            List<GateEvidence> evidence) {
        Objects.requireNonNull(workOrder, "workOrder");
        return evaluate(
                workOrder.capabilityId(),
                workOrder.root(),
                descriptor,
                catalogue,
                pass,
                M3RecipeGateSet.fromProblem(workOrder),
                evidence);
    }

    public static M3RecipePromotionReceipt evaluateRepository(
            M3RepositoryRecipeWorkOrder.WorkOrder workOrder,
            M3RecipeDescriptor descriptor,
            M3RecipeCatalogue catalogue,
            M3RecipePassReceipt pass,
            List<GateEvidence> evidence) {
        Objects.requireNonNull(workOrder, "workOrder");
        return evaluate(
                workOrder.capabilityId(),
                workOrder.root(),
                descriptor,
                catalogue,
                pass,
                M3RecipeGateSet.fromRepository(workOrder),
                evidence);
    }

    public static M3RecipePromotionReceipt evaluate(
            String capabilityId,
            String workRoot,
            M3RecipeDescriptor descriptor,
            M3RecipeCatalogue catalogue,
            M3RecipePassReceipt pass,
            M3RecipeGateSet requiredGates,
            List<GateEvidence> evidence) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(catalogue, "catalogue");
        Objects.requireNonNull(pass, "pass");
        bindRecipeEvidence(capabilityId, descriptor, catalogue, pass);
        crossCheckPass(pass, requiredGates, evidence);
        List<GateEvidence> normalized = normalizeEvidence(evidence);
        boolean promotable = normalized.stream().allMatch(GateEvidence::passed);
        return new M3RecipePromotionReceipt(
                capabilityId,
                workRoot,
                descriptor.identitySha256(),
                catalogue.root(),
                passRoot(pass),
                requiredGates,
                normalized,
                promotable,
                "");
    }

    private static void bindRecipeEvidence(
            String capabilityId,
            M3RecipeDescriptor descriptor,
            M3RecipeCatalogue catalogue,
            M3RecipePassReceipt pass) {
        String capability = token(capabilityId, "capabilityId");
        if (!descriptor.capabilityId().equals(capability)) {
            throw new IllegalArgumentException("recipe descriptor capability mismatch");
        }
        boolean admitted =
                catalogue.find(capability).stream()
                        .anyMatch(candidate ->
                                candidate.identitySha256().equals(descriptor.identitySha256()));
        if (!admitted) {
            throw new IllegalArgumentException("recipe descriptor not present in catalogue");
        }
        if (!pass.recipeClassName().equals(descriptor.recipeClassName())) {
            throw new IllegalArgumentException("recipe pass class mismatch");
        }
        if (!pass.recipeArtifactSha256().equals(descriptor.recipeArtifactSha256())) {
            throw new IllegalArgumentException("recipe pass artifact mismatch");
        }
        if (!pass.catalogueRootSha256().equals(catalogue.root())) {
            throw new IllegalArgumentException("recipe pass catalogue root mismatch");
        }
    }

    private static void crossCheckPass(
            M3RecipePassReceipt pass,
            M3RecipeGateSet gates,
            List<GateEvidence> evidence) {
        Map<M3RecipeGate, Boolean> supplied = new EnumMap<>(M3RecipeGate.class);
        for (GateEvidence item : evidence) {
            Boolean prior = supplied.put(item.gate(), item.passed());
            if (prior != null) throw new IllegalArgumentException("duplicate gate evidence");
        }
        for (M3RecipeGate gate : gates.gates()) {
            Boolean expected = passBackedStatus(pass, gate);
            if (expected != null && !Objects.equals(expected, supplied.get(gate))) {
                throw new IllegalArgumentException("gate evidence contradicts recipe pass: " + gate);
            }
        }
    }

    private static Boolean passBackedStatus(M3RecipePassReceipt pass, M3RecipeGate gate) {
        M3RecipePassReceipt.Verification verification = pass.verification();
        return switch (gate) {
            case RECIPE_DRY_RUN -> verification.dryRunReviewed();
            case SECOND_PASS_FIXED_POINT -> pass.fixedPoint();
            case API_CONTRACT -> pass.atoms().stream()
                    .allMatch(M3RecipePassReceipt.AtomResult::contractPreserved);
            case COMPILE -> verification.compilePassed();
            case STATIC_ANALYSIS -> verification.staticAnalysisPassed();
            case UNIT_TEST, INTEGRATION_TEST -> verification.testsPassed();
            case JAVA_ORACLE -> verification.javaOraclePassed();
            case NATIVE_ABI ->
                    !verification.nativeParityRequired() || verification.nativeParityPassed();
            case BENCHMARK ->
                    !verification.benchmarkRequired() || verification.benchmarkPassed();
            default -> null;
        };
    }

    public static String passRoot(M3RecipePassReceipt pass) {
        Objects.requireNonNull(pass, "pass");
        MessageDigest digest = digest();
        frame(digest, "SYNEXIA_M3_RECIPE_PASS_RECEIPT_V1");
        frame(digest, pass.recipeClassName());
        frame(digest, pass.recipeArtifactSha256());
        frame(digest, pass.catalogueRootSha256());
        frame(digest, pass.catalogueSnapshotSha256());
        frame(digest, pass.preimageRootSha256());
        frame(digest, pass.postimageRootSha256());
        for (M3RecipePassReceipt.AtomResult atom : pass.atoms()) {
            frame(digest, atom.state().sourcePath());
            frame(digest, atom.state().contractSha256());
            frame(digest, Boolean.toString(atom.changed()));
            frame(digest, atom.postContractSha256());
        }
        M3RecipePassReceipt.Verification v = pass.verification();
        frame(digest, Boolean.toString(v.dryRunReviewed()));
        frame(digest, Boolean.toString(v.compilePassed()));
        frame(digest, Boolean.toString(v.staticAnalysisPassed()));
        frame(digest, Boolean.toString(v.testsPassed()));
        frame(digest, Boolean.toString(v.javaOraclePassed()));
        frame(digest, Boolean.toString(v.nativeParityRequired()));
        frame(digest, Boolean.toString(v.nativeParityPassed()));
        frame(digest, Boolean.toString(v.benchmarkRequired()));
        frame(digest, Boolean.toString(v.benchmarkPassed()));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static List<GateEvidence> normalizeEvidence(List<GateEvidence> evidence) {
        ArrayList<GateEvidence> copy =
                new ArrayList<>(Objects.requireNonNull(evidence, "evidence"));
        copy.sort(Comparator.comparing(item -> item.gate().ordinal()));
        for (int index = 1; index < copy.size(); index++) {
            if (copy.get(index - 1).gate() == copy.get(index).gate()) {
                throw new IllegalArgumentException("duplicate gate evidence");
            }
        }
        return List.copyOf(copy);
    }

    private static void validateCoverage(
            M3RecipeGateSet required, List<GateEvidence> evidence) {
        EnumMap<M3RecipeGate, GateEvidence> byGate = new EnumMap<>(M3RecipeGate.class);
        evidence.forEach(item -> byGate.put(item.gate(), item));
        for (M3RecipeGate gate : required.gates()) {
            if (!byGate.containsKey(gate)) {
                throw new IllegalArgumentException("missing gate evidence: " + gate);
            }
        }
        if (byGate.size() != required.gates().size()) {
            throw new IllegalArgumentException("unexpected gate evidence");
        }
    }

    private static String root(
            String capabilityId,
            String workRoot,
            String recipeDescriptorRoot,
            String recipeCatalogueRoot,
            String recipePassRoot,
            M3RecipeGateSet requiredGates,
            List<GateEvidence> evidence,
            boolean promotable) {
        MessageDigest digest = digest();
        frame(digest, "SYNEXIA_M3_RECIPE_PROMOTION_RECEIPT_V1");
        frame(digest, capabilityId);
        frame(digest, workRoot);
        frame(digest, recipeDescriptorRoot);
        frame(digest, recipeCatalogueRoot);
        frame(digest, recipePassRoot);
        frame(digest, requiredGates.sourceKind());
        requiredGates.gates().forEach(gate -> frame(digest, gate.name()));
        evidence.forEach(item -> {
            frame(digest, item.gate().name());
            frame(digest, Boolean.toString(item.passed()));
            frame(digest, item.evidenceSha256());
        });
        frame(digest, Boolean.toString(promotable));
        return HexFormat.of().formatHex(digest.digest());
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

    private static String sha(String value, String field) {
        String checked = token(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
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
}
