// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Candidate-only review surface for one repository API capability.
 *
 * <p>Every fenced source is content-addressed. The recipe fails closed when the current
 * lossless-print source does not match its inventory preimage. No source mutation is performed.</p>
 */
public class M3RepositoryCapabilityCandidateRecipe extends Recipe {
    public static final String CAPABILITY_TAG_PREFIX = "m3-capability:";

    private final String capabilityId;
    private final String workRoot;
    private final List<String> sourcePaths;
    private final List<String> preimageSha256s;
    private final transient CandidateTable candidateTable = new CandidateTable(this);

    public M3RepositoryCapabilityCandidateRecipe() {
        this(
                System.getProperty("m3.repository.capability", "repository-api.review"),
                System.getProperty("m3.repository.workRoot", "0".repeat(64)),
                splitProperty(System.getProperty("m3.repository.sources", "")),
                splitProperty(System.getProperty("m3.repository.preimages", "")));
    }

    @JsonCreator
    public M3RepositoryCapabilityCandidateRecipe(
            String capabilityId,
            String workRoot,
            List<String> sourcePaths,
            List<String> preimageSha256s) {
        this.capabilityId = token(capabilityId, "capabilityId");
        this.workRoot = sha(workRoot, "workRoot");
        this.sourcePaths = normalizePaths(sourcePaths);
        this.preimageSha256s = hashes(preimageSha256s);
        if (this.sourcePaths.size() != this.preimageSha256s.size()) {
            throw new IllegalArgumentException("source/preimage shape mismatch");
        }
    }

    @Override
    public String getDisplayName() {
        return "M3 repository capability candidate";
    }

    @Override
    public String getDescription() {
        return "Reviews exact repository Java preimages for one API capability and emits "
                + "content-addressed candidate evidence without changing source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "repository",
                "api",
                "recipe-first",
                "preimage",
                "candidate-only",
                capabilityTag(capabilityId));
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public String getCapabilityTag() {
        return capabilityTag(capabilityId);
    }

    public static String capabilityTag(String capabilityId) {
        return CAPABILITY_TAG_PREFIX + token(capabilityId, "capabilityId");
    }

    public String getWorkRoot() {
        return workRoot;
    }

    public List<String> getSourcePaths() {
        return sourcePaths;
    }

    public List<String> getPreimageSha256s() {
        return preimageSha256s;
    }

    public boolean replacementAuthority() {
        return false;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Map<String, String> expected = expectedPreimages();
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit unit, ExecutionContext context) {
                J.CompilationUnit visited = super.visitCompilationUnit(unit, context);
                String path = normalizePath(visited.getSourcePath().toString());
                String expectedSha = expected.get(path);
                if (expectedSha == null) return visited;

                String currentSha = sha256(visited.printAll());
                if (!expectedSha.equals(currentSha)) {
                    throw new IllegalStateException(
                            "repository capability source preimage drift: "
                                    + path
                                    + " expected="
                                    + expectedSha
                                    + " actual="
                                    + currentSha);
                }

                candidateTable.insertRow(
                        context,
                        new CandidateRow(
                                capabilityId,
                                path,
                                expectedSha,
                                currentSha,
                                workRoot));
                return visited;
            }
        };
    }

    private Map<String, String> expectedPreimages() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < sourcePaths.size(); index++) {
            String path = sourcePaths.get(index);
            String sha = preimageSha256s.get(index);
            String previous = result.putIfAbsent(path, sha);
            if (previous != null && !previous.equals(sha)) {
                throw new IllegalArgumentException(
                        "same source path has conflicting preimage hashes: " + path);
            }
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    private static List<String> normalizePaths(List<String> values) {
        ArrayList<String> result = new ArrayList<>();
        for (String value : Objects.requireNonNullElse(values, List.<String>of())) {
            result.add(normalizePath(value));
        }
        return List.copyOf(result);
    }

    private static List<String> hashes(List<String> values) {
        return Objects.requireNonNullElse(values, List.<String>of()).stream()
                .map(value -> sha(value, "preimageSha256"))
                .toList();
    }

    private static List<String> splitProperty(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(",", -1))
                .map(String::strip)
                .toList();
    }

    private static String normalizePath(String value) {
        String path =
                Objects.toString(value, "")
                        .replace('\\', '/')
                        .strip();
        if (path.isEmpty()
                || path.indexOf('\0') >= 0
                || path.startsWith("/")
                || path.equals("..")
                || path.startsWith("../")
                || path.endsWith("/..")
                || path.contains("/../")
                || path.indexOf(',') >= 0
                || path.indexOf('"') >= 0
                || path.indexOf('\n') >= 0
                || path.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("sourcePath");
        }
        return path;
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

    private static String sha(String value, String field) {
        String checked = token(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static final class CandidateTable extends DataTable<CandidateRow> {
        CandidateTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 repository capability candidates",
                    "Exact preimage-verified source evidence for one repository API capability.");
        }
    }

    public static final class CandidateRow {
        @Column(displayName = "Capability", description = "Repository recipe capability.")
        private final String capabilityId;

        @Column(displayName = "Source path", description = "Exact normalized Java source path.")
        private final String sourcePath;

        @Column(displayName = "Expected preimage", description = "Inventory SHA-256.")
        private final String expectedPreimageSha256;

        @Column(displayName = "Current preimage", description = "OpenRewrite lossless-print SHA-256.")
        private final String currentPreimageSha256;

        @Column(displayName = "Work root", description = "Repository capability work-order root.")
        private final String workRoot;

        @Column(displayName = "Replacement authority", description = "Always false.")
        private final boolean replacementAuthority;

        CandidateRow(
                String capabilityId,
                String sourcePath,
                String expectedPreimageSha256,
                String currentPreimageSha256,
                String workRoot) {
            this.capabilityId = token(capabilityId, "capabilityId");
            this.sourcePath = normalizePath(sourcePath);
            this.expectedPreimageSha256 = sha(expectedPreimageSha256, "expectedPreimageSha256");
            this.currentPreimageSha256 = sha(currentPreimageSha256, "currentPreimageSha256");
            this.workRoot = sha(workRoot, "workRoot");
            if (!this.expectedPreimageSha256.equals(this.currentPreimageSha256)) {
                throw new IllegalArgumentException("preimage mismatch");
            }
            this.replacementAuthority = false;
        }

        public String getCapabilityId() { return capabilityId; }
        public String getSourcePath() { return sourcePath; }
        public String getExpectedPreimageSha256() { return expectedPreimageSha256; }
        public String getCurrentPreimageSha256() { return currentPreimageSha256; }
        public String getWorkRoot() { return workRoot; }
        public boolean isReplacementAuthority() { return replacementAuthority; }
    }
}
