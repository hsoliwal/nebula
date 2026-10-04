// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Frozen file-local OpenRewrite plan with deterministic structure and instance fingerprints.
 *
 * <p>The structure fingerprint excludes the source path and is therefore reusable across files.
 * The instance fingerprint binds that structure to one normalized repository-relative source atom.</p>
 */
public record M3TranspilePlan(
        Kind kind,
        String sourcePath,
        List<M3TranspilePass> passes,
        String structureSha256,
        String instanceSha256) {

    public enum Kind {
        IDENTITY,
        IOP_MECHANICAL,
        JAVA_MECHANICAL_CANDIDATE,
        JAVA_MECHANICAL_STATIC_ANALYSIS_CANDIDATE;

        public M3TranspilePlan compile(String sourcePath) {
            return M3TranspilePlan.compile(this, sourcePath);
        }
    }

    public M3TranspilePlan {
        kind = Objects.requireNonNull(kind, "kind");
        sourcePath = M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath);
        passes = List.copyOf(Objects.requireNonNull(passes, "passes"));
        structureSha256 = hash(structureSha256, "structureSha256");
        instanceSha256 = hash(instanceSha256, "instanceSha256");
    }

    public static M3TranspilePlan compile(Kind kind, String sourcePath) {
        Kind checkedKind = Objects.requireNonNull(kind, "kind");
        String path = M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath);
        List<M3TranspilePass> passes =
                switch (checkedKind) {
                    case IDENTITY -> List.of();
                    case IOP_MECHANICAL -> M3IopMechanicalPasses.forFile(path);
                    case JAVA_MECHANICAL_CANDIDATE -> M3MechanicalPasses.forFile(path);
                    case JAVA_MECHANICAL_STATIC_ANALYSIS_CANDIDATE ->
                            M3MechanicalPasses.forFileWithStaticAnalysis(path);
                };
        String structure = structureFingerprint(checkedKind, passes);
        String instance = sha256(structure + "\n" + path);
        return new M3TranspilePlan(checkedKind, path, passes, structure, instance);
    }

    public int passCount() {
        return passes.size();
    }

    public M3TranspileRun run(String source, List<java.nio.file.Path> classpath) {
        return M3OpenRewriteTranspiler.run(sourcePath, source, classpath, passes);
    }

    private static String structureFingerprint(Kind kind, List<M3TranspilePass> passes) {
        StringBuilder canonical = new StringBuilder(kind.name()).append('\n');
        for (M3TranspilePass pass : passes) {
            canonical.append(pass.id())
                    .append('\t')
                    .append(pass.recipe().getClass().getName())
                    .append('\t')
                    .append(pass.recipe().getName())
                    .append('\n');
        }
        return sha256(canonical.toString());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String hash(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        }
        return value;
    }
}
