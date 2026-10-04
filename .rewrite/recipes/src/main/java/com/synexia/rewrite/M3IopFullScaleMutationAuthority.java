// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;

/**
 * Single mechanical mutation authority for M3Scale over Synexia IOP pattern code.
 *
 * <p>IOP owns semantic contracts, pattern vocabulary and architecture roles. OpenRewrite owns
 * mechanical candidate generation. This class binds the source fence, pattern class hooks,
 * canonical pass sequence and proof-stage contract into one executable authority surface.</p>
 */
public final class M3IopFullScaleMutationAuthority {
    private static final String RECEIPT_DOMAIN = "M3_IOP_OPENREWRITE_AUTHORITY_RECEIPT_V1";

    private static final List<String> FULL_SCALE_PROOF_PATH = List.of(
            "FILE",
            "PACKAGE",
            "MODULE",
            "REACTOR",
            "API",
            "PROJECT",
            "REPOSITORY",
            "GITHUB_ACCOUNT",
            "SOURCE_HOST",
            "INTERNET",
            "EARTH",
            "SOLAR_SYSTEM",
            "UNIVERSE");

    public enum Stage {
        INVENTORY,
        SOURCE_FENCE,
        CLASS_HOOK_ADMISSION,
        REWRITE_CANDIDATE,
        DIFF,
        LINT,
        COMPILE,
        TEST,
        RUNTIME_IF_REQUIRED,
        SERIAL_PROMOTION
    }

    /** One top-level class authority binding; class-hook and PatternCatalog evidence cannot be mixed. */
    public record ClassEvidence(
            String className,
            boolean admitted,
            List<String> hookIds,
            String catalogReceiptRoot,
            String root) {

        public ClassEvidence {
            className = requireText(className, "className");
            hookIds = stable(hookIds);
            if (hookIds.stream().anyMatch(value -> !M3IopPatternClassHooks.hookIds().contains(value))) {
                throw new IllegalArgumentException("unknown IOP class hook id");
            }
            catalogReceiptRoot = requireSha(catalogReceiptRoot, "catalogReceiptRoot");
            if (admitted != !hookIds.isEmpty()) {
                throw new IllegalArgumentException("class evidence admission/hook mismatch");
            }
            String expected = sha256("M3_IOP_OPENREWRITE_CLASS_EVIDENCE_V1\0"
                    + className + "\0"
                    + admitted + "\0"
                    + String.join("\u001f", hookIds) + "\0"
                    + catalogReceiptRoot);
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("IOP class evidence root mismatch");
            }
        }
    }

    /**
     * Deterministic evidence envelope for one IOP compilation-unit admission decision.
     *
     * <p>The receipt records why a source atom can or cannot enter the candidate mutation lane.
     * It binds the exact immutable recipe configuration and may never grant canonical promotion
     * authority.</p>
     */
    public record AuthorityReceipt(
            String sourcePath,
            String sourceFilePattern,
            boolean admitted,
            List<ClassEvidence> classes,
            List<String> passIds,
            List<Stage> stages,
            String reason,
            boolean candidateOnly,
            boolean serialPromotionRequired,
            String root) {

        public AuthorityReceipt {
            sourcePath = requireText(sourcePath, "sourcePath");
            if (!M3IopSourceFence.admits(Path.of(sourcePath))) {
                throw new IllegalArgumentException("authority receipt source is outside IOP production fence");
            }
            sourceFilePattern = requireText(sourceFilePattern, "sourceFilePattern");
            classes = classes == null
                    ? List.of()
                    : classes.stream()
                            .filter(Objects::nonNull)
                            .sorted(java.util.Comparator.comparing(ClassEvidence::className))
                            .toList();
            boolean allClassesAdmitted =
                    !classes.isEmpty() && classes.stream().allMatch(ClassEvidence::admitted);
            if (admitted != allClassesAdmitted) {
                throw new IllegalArgumentException("authority receipt class admission mismatch");
            }
            passIds = passIds == null ? List.of() : List.copyOf(passIds);
            List<String> canonicalPassIds =
                    M3IopFullScaleMutationAuthority.canonicalPassIds(sourceFilePattern);
            if (!passIds.equals(canonicalPassIds)) {
                throw new IllegalArgumentException("authority receipt pass plan is not canonical");
            }
            stages = stages == null ? List.of() : List.copyOf(stages);
            if (!stages.equals(M3IopFullScaleMutationAuthority.stages())) {
                throw new IllegalArgumentException("authority receipt must contain the canonical M3 stage order");
            }
            reason = requireText(reason, "reason");
            if (!candidateOnly || !serialPromotionRequired) {
                throw new IllegalArgumentException(
                        "IOP OpenRewrite authority must remain candidate-only with serial promotion");
            }
            String expected = sha256(RECEIPT_DOMAIN + "\0"
                    + sourcePath + "\0"
                    + sourceFilePattern + "\0"
                    + admitted + "\0"
                    + classes.stream().map(ClassEvidence::root).reduce("", (a, b) -> a + b + "\u001f") + "\0"
                    + String.join("\u001f", passIds) + "\0"
                    + stages.stream().map(Enum::name).reduce("", (a, b) -> a + b + "\u001f") + "\0"
                    + reason + "\0"
                    + candidateOnly + "\0"
                    + serialPromotionRequired);
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("IOP authority receipt root mismatch");
            }
        }

        public List<String> classNames() {
            return classes.stream().map(ClassEvidence::className).toList();
        }

        public List<String> classHookIds() {
            return classes.stream().flatMap(value -> value.hookIds().stream()).distinct().sorted().toList();
        }

        public List<String> catalogReceiptRoots() {
            return classes.stream().map(ClassEvidence::catalogReceiptRoot).sorted().toList();
        }
    }

    private M3IopFullScaleMutationAuthority() {}

    public static List<Stage> stages() {
        return List.of(Stage.values());
    }

    /** Complete leaf-first M3Scale proof ladder required for serial IOP promotion. */
    public static List<String> fullScaleProofPath() {
        return FULL_SCALE_PROOF_PATH;
    }

    public static void requireIopSource(Path sourcePath) {
        M3IopSourceFence.requireIop(sourcePath);
    }

    public static M3IopPatternClassHooks.Admission inspect(J.CompilationUnit unit) {
        Objects.requireNonNull(unit, "unit");
        M3IopSourceFence.requireIop(unit.getSourcePath());
        return M3IopPatternClassHooks.inspect(unit);
    }

    /**
     * Produces deterministic class-hook/pass evidence for an IOP source atom.
     *
     * <p>Rejected IOP classes still receive a receipt so typed residue is observable. Non-IOP
     * source is rejected by the hard source fence before any receipt can be created.</p>
     */
    public static AuthorityReceipt receipt(J.CompilationUnit unit, String sourceFilePattern) {
        Objects.requireNonNull(unit, "unit");
        String pattern = requireText(sourceFilePattern, "sourceFilePattern");
        M3IopSourceFence.requireIop(unit.getSourcePath());

        M3IopPatternClassHooks.Admission admission = M3IopPatternClassHooks.inspect(unit);
        Map<String, J.ClassDeclaration> types = new TreeMap<>();
        for (J.ClassDeclaration type : unit.getClasses()) {
            if (types.putIfAbsent(type.getSimpleName(), type) != null) {
                throw new IllegalArgumentException("duplicate top-level IOP class name");
            }
        }

        List<ClassEvidence> classes = new ArrayList<>(admission.classes().size());
        for (M3IopPatternClassHooks.ClassAdmission classAdmission : admission.classes()) {
            J.ClassDeclaration type = types.get(classAdmission.className());
            if (type == null) {
                throw new IllegalStateException("class-hook evidence has no matching compilation-unit type");
            }
            String catalogRoot = M3IopPatternCatalogHooks.inspect(unit, type).root();
            classes.add(new ClassEvidence(
                    classAdmission.className(),
                    classAdmission.admitted(),
                    classAdmission.hookIds(),
                    catalogRoot,
                    ""));
        }

        List<String> passes = canonicalPassIds(pattern);

        return new AuthorityReceipt(
                portable(unit.getSourcePath()),
                pattern,
                admission.admitted(),
                classes,
                passes,
                stages(),
                admission.reason(),
                candidateOnly(),
                serialPromotionRequired(),
                "");
    }

    public static void requirePatternAdmission(J.CompilationUnit unit) {
        M3IopPatternClassHooks.Admission admission = inspect(unit);
        if (!admission.admitted()) {
            throw new IllegalArgumentException(
                    "M3Scale IOP mutation rejected unadmitted pattern class: " + admission.reason());
        }
    }

    public static List<M3TranspilePass> canonicalPassPlan(String sourceFilePattern) {
        return M3IopPatternMechanicalPasses.forPattern(requireText(sourceFilePattern, "sourceFilePattern"));
    }

    public static List<Recipe> canonicalPasses(String sourceFilePattern) {
        return canonicalPassPlan(sourceFilePattern).stream()
                .map(M3TranspilePass::recipe)
                .toList();
    }

    public static List<String> canonicalPassIds(String sourceFilePattern) {
        return canonicalPassPlan(sourceFilePattern).stream()
                .map(M3TranspilePass::id)
                .toList();
    }

    public static boolean candidateOnly() {
        return true;
    }

    public static boolean serialPromotionRequired() {
        return true;
    }

    public static boolean iopSemanticCodeOwnsMutation() {
        return false;
    }

    public static String canonicalRecipeClassName() {
        return M3IopPatternOnlySerialMechanicalJavaRecipe.class.getName();
    }

    private static List<String> stable(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .distinct()
                        .sorted()
                        .toList();
    }

    private static String requireSha(String value, String name) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
        return value;
    }

    private static String portable(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(name + " required");
        }
        return value.strip();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
