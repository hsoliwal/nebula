// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Content-addressed Maven/OpenRewrite admission receipt for one generic LLM recipe crate.
 *
 * <p>The receipt proves which exact crate document entered the recipe-first lane. It is still
 * candidate-only evidence and grants no file mutation, donor-copy, execution, merge, or promotion
 * authority.</p>
 */
public record M3LlmTaskAdmissionReceipt(
        String crateDocumentSha256,
        String crateRoot,
        String taskId,
        String capabilityId,
        String requirementSha256,
        String catalogueSha256,
        M3LlmTaskRecipeCrate.Disposition disposition,
        String donorManifestRoot,
        String donorEvidenceRoot,
        int donorRepositoryCount,
        String developmentPlanRoot,
        String mavenModule,
        String mavenProfile,
        List<String> requiredStages,
        boolean recipeCrateIsWorkUnit,
        boolean candidateOnly,
        boolean serialPromotionRequired,
        boolean targetFileEditAuthority,
        boolean donorSourceCopyAuthority,
        boolean promotionAuthority,
        String root) {

    public static final String MAVEN_MODULE = "synexia-openrewrite-recipes";
    public static final String MAVEN_PROFILE = "m3-llm-recipe-first";

    public M3LlmTaskAdmissionReceipt {
        crateDocumentSha256 = sha(crateDocumentSha256, "crateDocumentSha256");
        crateRoot = sha(crateRoot, "crateRoot");
        taskId = text(taskId, "taskId");
        capabilityId = text(capabilityId, "capabilityId");
        requirementSha256 = sha(requirementSha256, "requirementSha256");
        catalogueSha256 = sha(catalogueSha256, "catalogueSha256");
        disposition = Objects.requireNonNull(disposition, "disposition");
        donorManifestRoot = sha(donorManifestRoot, "donorManifestRoot");
        donorEvidenceRoot = sha(donorEvidenceRoot, "donorEvidenceRoot");
        if (donorRepositoryCount < 0 || donorRepositoryCount > 100_000) {
            throw new IllegalArgumentException("donorRepositoryCount");
        }
        developmentPlanRoot = sha(developmentPlanRoot, "developmentPlanRoot");
        mavenModule = text(mavenModule, "mavenModule");
        mavenProfile = text(mavenProfile, "mavenProfile");
        requiredStages = List.copyOf(Objects.requireNonNull(requiredStages, "requiredStages"));
        if (!requiredStages.equals(M3RecipeFirstInvariant.canonicalStages())) {
            throw new IllegalArgumentException("non-canonical M3 stage order");
        }
        if (!recipeCrateIsWorkUnit || !candidateOnly || !serialPromotionRequired) {
            throw new IllegalArgumentException(
                    "admission must remain recipe-crate/candidate-only/serial-promotion");
        }
        if (targetFileEditAuthority || donorSourceCopyAuthority || promotionAuthority) {
            throw new IllegalArgumentException("admission cannot own mutation/copy/promotion authority");
        }

        String expected =
                root(
                        crateDocumentSha256,
                        crateRoot,
                        taskId,
                        capabilityId,
                        requirementSha256,
                        catalogueSha256,
                        disposition,
                        donorManifestRoot,
                        donorEvidenceRoot,
                        donorRepositoryCount,
                        developmentPlanRoot,
                        mavenModule,
                        mavenProfile,
                        requiredStages);
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!expected.equals(root)) {
            throw new IllegalArgumentException("LLM task admission receipt root mismatch");
        }
    }

    public static M3LlmTaskAdmissionReceipt admit(
            byte[] rawCrateDocument, String expectedCrateRoot) {
        byte[] bytes = Objects.requireNonNull(rawCrateDocument, "rawCrateDocument").clone();
        String document = decodeUtf8(bytes);
        M3LlmTaskRecipeCrate crate = M3LlmTaskRecipeCrateCodec.parse(document);
        String semanticRoot = M3LlmTaskRecipeCrateCodec.root(crate);
        String expectedRoot = sha(expectedCrateRoot, "expectedCrateRoot");
        if (!semanticRoot.equals(expectedRoot)) {
            throw new IllegalArgumentException("expected crate root mismatch");
        }

        M3RecipeFirstInvariant.requireLlmTaskRecipeCrate(crate);
        M3PinnedDonorEvidence donorEvidence = M3PinnedDonorEvidence.from(crate.donorReferences());
        if (!donorEvidence.allRepositoriesPinned()) {
            throw new IllegalArgumentException(
                    "unresolved donor repositories: "
                            + String.join(",", donorEvidence.unresolvedRepositories()));
        }
        M3LlmTaskRecipeDevelopmentPlan plan = M3LlmTaskRecipeDevelopmentPlan.from(crate);
        if (!plan.crateRoot().equals(semanticRoot)) {
            throw new IllegalStateException("development plan is bound to another crate");
        }
        if (!plan.requiredStages().equals(M3RecipeFirstInvariant.canonicalStages())) {
            throw new IllegalStateException("development plan lost canonical M3 stage order");
        }

        return new M3LlmTaskAdmissionReceipt(
                sha256(bytes),
                semanticRoot,
                crate.taskId(),
                crate.capabilityId(),
                crate.requirementSha256(),
                crate.catalogueSha256(),
                crate.disposition(),
                donorEvidence.manifestRoot(),
                donorEvidence.root(),
                crate.donorReferences().size(),
                plan.root(),
                MAVEN_MODULE,
                MAVEN_PROFILE,
                plan.requiredStages(),
                true,
                true,
                true,
                false,
                false,
                false,
                "");
    }

    public static M3LlmTaskAdmissionReceipt admit(String crateDocument, String expectedCrateRoot) {
        return admit(strictUtf8(crateDocument), expectedCrateRoot);
    }

    public boolean executionAuthority() {
        return false;
    }

    public boolean mergeAuthority() {
        return false;
    }

    private static String root(
            String crateDocumentSha256,
            String crateRoot,
            String taskId,
            String capabilityId,
            String requirementSha256,
            String catalogueSha256,
            M3LlmTaskRecipeCrate.Disposition disposition,
            String donorManifestRoot,
            String donorEvidenceRoot,
            int donorRepositoryCount,
            String developmentPlanRoot,
            String mavenModule,
            String mavenProfile,
            List<String> stages) {
        MessageDigest digest = digest();
        frame(digest, "M3_LLM_TASK_ADMISSION_RECEIPT_V1");
        frame(digest, crateDocumentSha256);
        frame(digest, crateRoot);
        frame(digest, taskId);
        frame(digest, capabilityId);
        frame(digest, requirementSha256);
        frame(digest, catalogueSha256);
        frame(digest, disposition.name());
        frame(digest, donorManifestRoot);
        frame(digest, donorEvidenceRoot);
        frame(digest, Integer.toString(donorRepositoryCount));
        frame(digest, developmentPlanRoot);
        frame(digest, mavenModule);
        frame(digest, mavenProfile);
        stages.forEach(stage -> frame(digest, stage));
        frame(digest, "recipeCrateIsWorkUnit=true");
        frame(digest, "candidateOnly=true");
        frame(digest, "serialPromotionRequired=true");
        frame(digest, "targetFileEditAuthority=false");
        frame(digest, "donorSourceCopyAuthority=false");
        frame(digest, "promotionAuthority=false");
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("crate document is not strict UTF-8", invalid);
        }
    }

    private static byte[] strictUtf8(String value) {
        try {
            ByteBuffer encoded =
                    StandardCharsets.UTF_8
                            .newEncoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .encode(java.nio.CharBuffer.wrap(Objects.requireNonNull(value, "value")));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("crate document contains malformed Unicode", invalid);
        }
    }

    private static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(digest().digest(bytes));
    }

    private static String sha(String value, String field) {
        String checked = text(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
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
}
