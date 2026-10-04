// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.openrewrite.Recipe;

/**
 * Deterministic operator-to-recipe writer for recipe-first M3 IOP work.
 *
 * <p>An LLM/task may supply a class-backed leaf recipe as an operator candidate. This writer does
 * not let that operator choose target files, bypass IOP class hooks, copy donor source, or promote
 * canonical state. It binds the operator to an existing recipe work order, wraps the leaf in the
 * canonical IOP pattern fence/class hook, and emits one task-local transpile pass plus a
 * content-addressed receipt.</p>
 */
public final class M3IopOperatorRecipeWriter {
    public static final String RECEIPT_VERSION = "M3_IOP_OPERATOR_RECIPE_WRITER_V1";

    public record ProblemBinding(
            String capabilityId,
            String shape,
            String workOrderRoot,
            List<String> existingRecipeCandidates,
            List<String> existingRecipeCandidateRoots,
            String nativeLane,
            List<String> nativeDonors,
            List<String> requiredGates,
            String root) {

        public ProblemBinding {
            capabilityId = text(capabilityId, "capabilityId");
            shape = text(shape, "shape");
            workOrderRoot = sha(workOrderRoot, "workOrderRoot");
            existingRecipeCandidates = orderedText(existingRecipeCandidates, "existingRecipeCandidates");
            existingRecipeCandidateRoots = orderedHashes(existingRecipeCandidateRoots, "existingRecipeCandidateRoots");
            if (existingRecipeCandidates.size() != existingRecipeCandidateRoots.size()) {
                throw new IllegalArgumentException("problem recipe candidate identity mismatch");
            }
            nativeLane = text(nativeLane, "nativeLane");
            nativeDonors = stable(nativeDonors);
            requiredGates = List.copyOf(Objects.requireNonNull(requiredGates, "requiredGates"));
            String expected = digest(
                    "M3_IOP_OPERATOR_PROBLEM_BINDING_V1",
                    capabilityId,
                    shape,
                    workOrderRoot,
                    String.join("\u001f", existingRecipeCandidates),
                    String.join("\u001f", existingRecipeCandidateRoots),
                    nativeLane,
                    String.join("\u001f", nativeDonors),
                    String.join("\u001f", requiredGates));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) throw new IllegalArgumentException("problem binding root mismatch");
        }

        public static ProblemBinding from(M3ProblemRecipeWorkOrder.WorkOrder workOrder) {
            Objects.requireNonNull(workOrder, "workOrder");
            if (workOrder.mode() != M3ProblemRecipeWorkOrder.Mode.AUTHOR_OR_IMPROVE_RECIPE) {
                throw new IllegalArgumentException(
                        "existing problem recipe must be reused/dry-run, not rewritten by operator writer");
            }
            return new ProblemBinding(
                    workOrder.capabilityId(),
                    workOrder.shape().name(),
                    workOrder.root(),
                    workOrder.existingRecipeCandidates(),
                    workOrder.existingRecipeCandidateRoots(),
                    "UNBOUND_FROM_GROUPED_WORK_ORDER",
                    List.of(),
                    workOrder.requiredGates(),
                    "");
        }

        public static ProblemBinding from(M3ProblemCataloguePlanner.ShapePlan shapePlan) {
            Objects.requireNonNull(shapePlan, "shapePlan");
            if (shapePlan.recipeDisposition()
                    != M3RecipeCatalogue.Disposition.CREATE_OR_IMPROVE_RECIPE) {
                throw new IllegalArgumentException(
                        "existing problem recipe must be reused/dry-run, not rewritten by operator writer");
            }
            return new ProblemBinding(
                    shapePlan.capabilityId(),
                    shapePlan.shape().name(),
                    shapePlan.root(),
                    shapePlan.recipeCandidates(),
                    shapePlan.recipeCandidateRoots(),
                    shapePlan.nativeLane().name(),
                    shapePlan.nativeDonors(),
                    M3ProblemRecipeWorkOrder.gates(),
                    "");
        }
    }

    public record OperatorSpec(
            String capabilityId,
            String operatorId,
            Recipe leafRecipe,
            boolean nativeParityRequired,
            List<String> donorRefs,
            String root) {

        public OperatorSpec {
            capabilityId = text(capabilityId, "capabilityId");
            operatorId = text(operatorId, "operatorId");
            leafRecipe = requireLeaf(leafRecipe);
            donorRefs = stable(donorRefs);
            String expected = digest(
                    "M3_IOP_OPERATOR_SPEC_V1",
                    capabilityId,
                    operatorId,
                    leafRecipe.getClass().getName(),
                    leafRecipe.getName(),
                    Boolean.toString(nativeParityRequired),
                    String.join("\u001f", donorRefs));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) throw new IllegalArgumentException("operator spec root mismatch");
        }

        public static OperatorSpec of(
                String capabilityId,
                String operatorId,
                Recipe leafRecipe,
                boolean nativeParityRequired,
                List<String> donorRefs) {
            return new OperatorSpec(
                    capabilityId,
                    operatorId,
                    leafRecipe,
                    nativeParityRequired,
                    donorRefs,
                    "");
        }
    }

    public record Receipt(
            String capabilityId,
            String operatorId,
            String sourceFilePattern,
            String llmWorkOrderRoot,
            String challengeSearchRoot,
            String donorReviewRoot,
            String problemBindingRoot,
            String problemNativeLane,
            List<String> problemNativeDonors,
            String leafRecipeClass,
            String wrappedRecipeClass,
            List<String> requiredClassHookIds,
            List<String> donorRefs,
            List<String> requiredStages,
            List<String> problemGates,
            boolean nativeParityRequired,
            boolean candidateOnly,
            boolean serialPromotionRequired,
            boolean targetFileEditAuthority,
            boolean donorSourceCopyAuthority,
            boolean promotionAuthority,
            String root) {

        public Receipt {
            capabilityId = text(capabilityId, "capabilityId");
            operatorId = text(operatorId, "operatorId");
            sourceFilePattern = text(sourceFilePattern, "sourceFilePattern");
            llmWorkOrderRoot = sha(llmWorkOrderRoot, "llmWorkOrderRoot");
            challengeSearchRoot = sha(challengeSearchRoot, "challengeSearchRoot");
            donorReviewRoot = sha(donorReviewRoot, "donorReviewRoot");
            problemBindingRoot = optionalSha(problemBindingRoot, "problemBindingRoot");
            problemNativeLane = text(problemNativeLane, "problemNativeLane");
            problemNativeDonors = stable(problemNativeDonors);
            leafRecipeClass = text(leafRecipeClass, "leafRecipeClass");
            wrappedRecipeClass = text(wrappedRecipeClass, "wrappedRecipeClass");
            requiredClassHookIds = stable(requiredClassHookIds);
            donorRefs = stable(donorRefs);
            requiredStages = List.copyOf(Objects.requireNonNull(requiredStages, "requiredStages"));
            problemGates = List.copyOf(Objects.requireNonNull(problemGates, "problemGates"));
            if (!requiredStages.equals(M3RecipeFirstInvariant.canonicalStages())) {
                throw new IllegalArgumentException("non-canonical recipe-first stage order");
            }
            if (!requiredClassHookIds.equals(M3IopRecipeMutationFence.canonicalClassHookIds())) {
                throw new IllegalArgumentException("IOP operator receipt requires exact canonical class hooks");
            }
            if (!candidateOnly || !serialPromotionRequired) {
                throw new IllegalArgumentException("IOP operator must remain candidate-only with serial promotion");
            }
            if (targetFileEditAuthority || donorSourceCopyAuthority || promotionAuthority) {
                throw new IllegalArgumentException("IOP operator writer cannot own mutation/copy/promotion authority");
            }
            if (nativeParityRequired && !problemGates.isEmpty()
                    && !problemGates.contains("NATIVE_PARITY_IF_DECLARED")) {
                throw new IllegalArgumentException("native operator is missing native parity gate");
            }
            String expected = digest(
                    RECEIPT_VERSION,
                    capabilityId,
                    operatorId,
                    sourceFilePattern,
                    llmWorkOrderRoot,
                    challengeSearchRoot,
                    donorReviewRoot,
                    problemBindingRoot,
                    problemNativeLane,
                    String.join("\u001f", problemNativeDonors),
                    leafRecipeClass,
                    wrappedRecipeClass,
                    String.join("\u001f", requiredClassHookIds),
                    String.join("\u001f", donorRefs),
                    String.join("\u001f", requiredStages),
                    String.join("\u001f", problemGates),
                    Boolean.toString(nativeParityRequired),
                    Boolean.toString(candidateOnly),
                    Boolean.toString(serialPromotionRequired));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) throw new IllegalArgumentException("operator writer receipt root mismatch");
        }
    }

    public record WrittenOperator(M3TranspilePass pass, Receipt receipt) {
        public WrittenOperator {
            pass = Objects.requireNonNull(pass, "pass");
            receipt = Objects.requireNonNull(receipt, "receipt");
            if (!pass.id().equals(receipt.operatorId())) {
                throw new IllegalArgumentException("operator pass/receipt id mismatch");
            }
            if (!(pass.recipe() instanceof M3IopPatternScopedRecipe scoped)) {
                throw new IllegalArgumentException("operator pass must be IOP pattern scoped");
            }
            if (!scoped.sourceFilePattern().equals(receipt.sourceFilePattern())) {
                throw new IllegalArgumentException("operator source pattern mismatch");
            }
        }

        /**
         * Immutable proof that this task operator remains subordinate to the canonical IOP class
         * hooks and full-scale serial M3 promotion.
         */
        public M3IopRecipeMutationFence.Receipt mutationAuthorityReceipt() {
            return M3IopRecipeMutationFence.taskOperator(this);
        }

        /**
         * Binds the operator writer, challenge/donor evidence, canonical recipe-first stages,
         * class hooks, and complete M3 FILE-to-UNIVERSE proof path into one immutable receipt.
         */
        public M3IopOperatorFullScaleAuthority.Receipt fullScaleAuthorityReceipt() {
            return M3IopOperatorFullScaleAuthority.from(this);
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    private M3IopOperatorRecipeWriter() {}

    public static WrittenOperator write(
            M3LlmRecipeWorkOrder.WorkOrder workOrder,
            OperatorSpec operator) {
        return write(workOrder, null, operator);
    }

    public static WrittenOperator write(
            M3LlmRecipeWorkOrder.WorkOrder workOrder,
            ProblemBinding problem,
            OperatorSpec operator) {
        Objects.requireNonNull(workOrder, "workOrder");
        Objects.requireNonNull(operator, "operator");

        if (workOrder.mode() != M3LlmRecipeWorkOrder.Mode.AUTHOR_OR_IMPROVE_RECIPE) {
            throw new IllegalArgumentException(
                    "operator writer is only for missing/improvable recipe capabilities");
        }
        if (!workOrder.capabilityId().equals(operator.capabilityId())) {
            throw new IllegalArgumentException("operator capability does not match LLM work order");
        }
        if (!workOrder.recipeCrateIsWorkUnit()) {
            throw new IllegalArgumentException("M3 recipe crate must remain the work unit");
        }
        if (problem != null && !problem.capabilityId().equals(workOrder.capabilityId())) {
            throw new IllegalArgumentException("problem capability does not match LLM work order");
        }

        Recipe leaf = requireLeaf(operator.leafRecipe());
        M3IopPatternScopedRecipe wrapped =
                new M3IopPatternScopedRecipe(workOrder.sourceFilePattern(), leaf);
        M3TranspilePass pass = new M3TranspilePass(operator.operatorId(), wrapped);

        Receipt receipt = new Receipt(
                workOrder.capabilityId(),
                operator.operatorId(),
                workOrder.sourceFilePattern(),
                workOrder.root(),
                workOrder.challengeSearchRoot(),
                workOrder.donorReviewRoot(),
                problem == null ? "" : problem.root(),
                problem == null ? "NO_PROBLEM_BINDING" : problem.nativeLane(),
                problem == null ? List.of() : problem.nativeDonors(),
                leaf.getClass().getName(),
                wrapped.getClass().getName(),
                M3IopPatternClassHooks.hookIds(),
                merge(operator.donorRefs(), problem == null ? List.of() : problem.nativeDonors()),
                workOrder.requiredStages(),
                problem == null ? List.of() : problem.requiredGates(),
                operator.nativeParityRequired(),
                true,
                true,
                false,
                false,
                false,
                "");

        return new WrittenOperator(pass, receipt);
    }

    /**
     * Compile one or more unknown-capability LLM work orders into one deterministic recipe crate.
     *
     * <p>The returned artifacts are recipe-development files only. No target application source is
     * edited and no donor-copy or promotion authority is granted.</p>
     */
    public static M3LlmRecipeCrateScaffold.Scaffold scaffold(
            List<M3LlmRecipeWorkOrder.WorkOrder> workOrders) {
        return M3LlmRecipeCrateScaffold.render(M3LlmRecipeBatchWorkOrder.compile(workOrders));
    }

    /**
     * Compose baseline mechanical transforms, task-specific operator passes, then IOP static
     * analysis. Operator arrival order does not affect the resulting pass order.
     */
    public static List<M3TranspilePass> composePlan(
            String sourceFilePattern,
            List<WrittenOperator> operators) {
        String pattern = text(sourceFilePattern, "sourceFilePattern");
        List<M3TranspilePass> taskPasses = Objects.requireNonNull(operators, "operators").stream()
                .map(value -> Objects.requireNonNull(value, "operator"))
                .sorted(Comparator.comparing(value -> value.receipt().operatorId()))
                .map(WrittenOperator::pass)
                .toList();
        return M3IopPatternMechanicalPasses.forPattern(pattern, taskPasses);
    }

    private static Recipe requireLeaf(Recipe recipe) {
        Recipe checked = Objects.requireNonNull(recipe, "leafRecipe");
        if (!checked.getRecipeList().isEmpty()) {
            throw new IllegalArgumentException("operator must be a class-backed leaf recipe");
        }
        if (checked.maxCycles() != 1 || checked.causesAnotherCycle()) {
            throw new IllegalArgumentException("operator leaf must be bounded to one cycle");
        }
        if (checked instanceof M3IopPatternOnlySerialMechanicalJavaRecipe
                || checked instanceof M3IopOnlySerialMechanicalJavaRecipe) {
            throw new IllegalArgumentException("operator cannot recursively wrap the canonical pipeline");
        }
        return checked;
    }

    private static List<String> merge(List<String> left, List<String> right) {
        ArrayList<String> values = new ArrayList<>();
        values.addAll(Objects.requireNonNullElse(left, List.of()));
        values.addAll(Objects.requireNonNullElse(right, List.of()));
        return stable(values);
    }

    private static List<String> stable(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private static List<String> orderedText(List<String> values, String field) {
        return Objects.requireNonNull(values, field).stream()
                .map(value -> text(value, field))
                .toList();
    }

    private static List<String> orderedHashes(List<String> values, String field) {
        return Objects.requireNonNull(values, field).stream()
                .map(value -> sha(value, field))
                .toList();
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String optionalSha(String value, String field) {
        String checked = Objects.requireNonNullElse(value, "").strip();
        return checked.isEmpty() ? "" : sha(checked, field);
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNullElse(value, "").strip();
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
