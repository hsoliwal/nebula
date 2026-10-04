// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mechanical admission rules for repeatable M3 source-changing transformations.
 *
 * <p>Inventory is authoritative before recipe selection: DISCOVER -> INVENTORY -> CATALOGUE ->
 * CLASSIFY -> CREATE/IMPROVE RECIPE -> DRY RUN -> REVIEW -> APPLY -> VERIFY CONTRACT/BEHAVIOR ->
 * DIFF -> LINT/STATIC ANALYSIS -> COMPILE -> TEST -> RUNTIME WHEN REQUIRED -> JAVA/JNI PARITY ->
 * BENCHMARK WHEN CLAIMED -> SECOND-PASS FIXED POINT -> PROMOTE.</p>
 */
public final class M3RecipeFirstInvariant {
    public static final String INVARIANT_ID =
            "M3-RECIPE-FIRST-SOURCE-CHANGE-1";

    private static final List<String> LAWS =
            List.of(
                    "RECIPE_IS_PRODUCT",
                    "INVENTORY_BEFORE_RECIPE",
                    "API_OPERATION_DATA_PRECOMPUTE_MAP_BEFORE_RECIPE_WORK",
                    "UNRESOLVED_API_CAPABILITY_BINDS_REPOSITORY_RECIPE_COVERAGE",
                    "ONLY_AUTHOR_RECIPE_COVERAGE_ROWS_ENTER_SCAFFOLD_GENERATION",
                    "AUTHOR_RECIPE_WORK_ORDER_BINDS_LLM_TASK_RECIPE_CRATE_BEFORE_IMPLEMENTATION",
                    "AUTHOR_RECIPE_LLM_CRATE_BINDS_API_BEHAVIOR_REVIEW_BEFORE_IMPLEMENTATION",
                    "API_IMPLEMENTATION_REVIEW_REQUIRES_EXACT_TASK_AND_REVIEW_PLAN_ADMISSION",
                    "IMPLEMENTATION_RECIPE_EDIT_REQUIRES_SOURCE_BOUND_API_REVIEW_RECEIPT",
                    "RECIPE_BOOTSTRAP_MATCHES_TARGET_SOURCE_KIND",
                    "ATOM_LOCALITY",
                    "PARTIAL_AST_SEALED_LEAF_SCOPE",
                    "PARTIAL_AST_STALE_PREIMAGE_REJECTED",
                    "PARTIAL_AST_PARENT_FAN_IN_SEPARATE_ADMISSION",
                    "CONTRACT_LOCK",
                    "LLM_CANDIDATE_ONLY",
                    "LLM_SOURCE_CHANGE_REQUIRES_MAVEN_RECIPE_CRATE",
                    "LLM_TASK_MUTATES_RECIPE_CRATE_NOT_TARGET_FILES",
                    "LLM_MAVEN_ADMISSION_REQUIRES_CONTENT_ADDRESSED_CRATE",
                    "FILE_LOCAL_PARALLEL_CANDIDATE_SERIAL_PROMOTION",
                    "CONTRACT_PRESERVING_FILE_ATOMIZATION_AND_PATTERNIZATION_UNBOUNDED",
                    "EXTERNAL_DONOR_MAVENIZE_ATOMIZE_PATTERNIZE_BEFORE_ABSORPTION",
                    "EXTERNAL_DONOR_INVENTORY_BINDS_STRUCTURE_SEMANTICS_JNI_AND_PATTERN_EVIDENCE",
                    "EXTERNAL_DONOR_ABSORPTION_PLAN_IS_CANDIDATE_ONLY",
                    "EXTERNAL_DONOR_ABSORPTION_PLAN_PRECEDES_CAPABILITY_RECIPE",
                    "REFACTOR_SCOPE_FILE_VISIBILITY_PACKAGE_MODULE_MULTI_MODULE_LIBRARY",
                    "SCOPE_PROMOTES_ONLY_WHEN_BOUNDARY_CROSSES",
                    "RECIPE_JUNIT_PROOF_PRECEDES_REPOSITORY_APPLICATION",
                    "TRUSTED_JAVA21_RECIPE_DAG_PRECEDES_CI_FANOUT",
                    "OPENREWRITE_RECIPE_ATOMS_COMPILE_ONCE_TO_CANONICAL_DAG",
                    "CAMEL_AIRFLOW_DROOLS_SHARE_CANONICAL_RECIPE_DAG_ROOT",
                    "SCHEDULER_AND_POLICY_BACKENDS_NEVER_GAIN_PROMOTION_AUTHORITY",
                    "DERIVABLE_DOCUMENTATION_CONVERGES_BY_RECIPE",
                    "EDIT_SCOPE_PROMOTION_FILE_VISIBILITY_PACKAGE_MODULE_MULTI_MODULE_LIBRARY_API",
                    "FILE_SCOPE_RECIPE_MAY_NOT_ESCAPE_TARGET",
                    "VERBATIM_PREIMAGE_BINDS_RECIPE_TARGET_SET",
                    "MULTIPASS_SCOPE_PROMOTION_REQUIRES_PRIOR_FIXED_POINT",
                    "JUNIT_PROVES_RECIPE_BEHAVIOR",
                    "NINETY_NINE_PERCENT_ATOM_PATTERN_IOP_COVERAGE",
                    "FILE_CONVERGENCE_ORDER_INVENTORY_ATOMIZATION_PATTERNIZATION_DOCUMENTATION_FIXED_POINT",
                    "M3INDEX_ARTIFACT_MIGRATION_ALIAS_FIRST_AND_ADDITIVE",
                    "PROJECT_MODULE_PACKAGE_FILE_METHOD_COMPLETE_INVENTORY",
                    "DONOR_PROVENANCE_AND_LICENSE_GATE",
                    "DONOR_LICENSE_PROVENANCE_CONSISTENCY_BEFORE_SOURCE_REUSE",
                    "NATIVE_CPP_DONOR_CATALOGUE_BINDS_ALGORITHM_ATOM",
                    "MECHANICAL_DONOR_SHAPE_REVIEW_PRECEDES_RECIPE_CRATE",
                    "MECHANICAL_DONOR_REVIEW_ROOT_BINDS_RECIPE_CRATE",
                    "ALGORITHM_ATOM_BINDS_MECHANICAL_DONOR_REVIEW_WHEN_CATALOGUED",
                    "MECHANICAL_DONOR_BINDING_ADMISSION_PRECEDES_RECIPE_EXECUTION",
                    "CATALOGUED_ALGORITHM_ATOM_SOURCE_BOUND_REVIEW_REQUIRES_MECHANICAL_BINDING",
                    "MECHANICAL_DONOR_BINDING_PERSISTS_THROUGH_EXECUTED_CONTRACT",
                    "MECHANICAL_DONOR_CUSTODY_PERSISTS_THROUGH_PROMOTION_AND_CANONICAL_READBACK",
                    "CLEAN_ROOM_SPEC_EVIDENCE_NEVER_GRANTS_CODE_COPY_AUTHORITY",
                    "UNKNOWN_OR_REJECTED_SOURCE_FAILS_CLOSED",
                    "CATEGORY_FAST_SEARCH_TO_SERIAL_FILE_REVIEW",
                    "CHALLENGE_EVIDENCE_BALANCED_BY_SOURCE_WHEN_AVAILABLE",
                    "CHALLENGE_REVIEW_ORDER_LEETCODE_HACKERRANK_GEEKSFORGEEKS",
                    "CHALLENGE_DONOR_PASS_LEDGER_BINDS_ONE_NEXT_ACTION",
                    "PROBLEM_CATALOGUE_ALL_CATEGORIES_BIND_ONE_NEXT_ACTION",
                    "DONOR_BEST_OF_BEST_SELECTION_PRECEDES_RECIPE_WORK_ORDER",
                    "CHALLENGE_PROBLEM_AND_EDITORIAL_BODIES_REFERENCE_ONLY",
                    "FULL_ALGORITHM_REVIEW_MATERIALIZES_PROBLEM_RECIPE_WORK_ORDERS",
                    "JNI_HOT_PATH_REVIEW_FOLLOWS_JAVA_ORACLE_AND_WORK_ORDER",
                    "EVERY_RECIPE_CRATE_TARGET_REVIEWED_EXACTLY_ONCE_IN_SERIAL_PATH_ORDER",
                    "CAPABILITY_DEDUPLICATES_FILE_WORK_INTO_ONE_RECIPE",
                    "PUBLIC_API_AND_EXTERNAL_BEHAVIOR_LOCKED_BY_DEFAULT",
                    "M3_TEXT_STRUCTURAL_VIEWS_RETAIN_CANONICAL_PARENT_RANGES",
                    "M3_TEXT_MATERIALIZATION_AND_JDK_BOUNDARIES_ARE_EXPLICIT",
                    "M3_STRING_PUBLISHED_COMPATIBILITY_METHODS_REMAIN_CONTRACT_LOCKED",
                    "M3_SEGMENTED_TEXT_PRECOMPUTE_REQUIRES_EXPLICIT_MATERIALIZATION",
                    "M3_SUBSTRING_INDEX_QUERIES_DO_NOT_INTERN_MATCHES",
                    "M3_STRING_PRECOMPUTE_SEARCH_REUSES_MINDEX_OWNERS",
                    "M3_TEXT_CANNOT_BYPASS_CANONICAL_INDEXSTRING_STORAGE_GATE",
                    "M3_CANONICAL_ADMISSION_IS_EXPLICIT_MATERIALIZATION_BOUNDARY",
                    "M3_NATIVE_UTF16_SEARCH_REQUIRES_CANONICAL_DIRECT_STORAGE",
                    "M3_CANONICAL_SEARCH_JAVA_FALLBACK_PRESERVES_SEMANTICS",
                    "COLLECTION_STORAGE_HAS_NO_RETAINED_ENTRY_NODE_BUCKET_OR_CELL_PER_ELEMENT",
                    "JDK_MAP_ENTRY_PROJECTION_IS_FRESH_UNPOOLED_AND_UNRETAINED",
                    "COLLECTION_CONSTRUCTION_USES_PRIMITIVE_ASPECT_WORD",
                    "COLLECTION_PHYSICAL_INDEX_STRATEGY_IS_PROVIDER_OWNED",
                    "COLLECTION_PRESSURE_REUSES_MINDEX_SWAP_PLANE",
                    "COLLECTION_SQLITE_COLD_PAGE_IDENTITY_SURVIVES_ZERO_RESIDENCY",
                    "COLLECTION_DONOR_REVIEW_REUSES_CANONICAL_THREE_PLATFORM_CATALOGUE",
                    "COLLECTION_HASH_IS_OPTIONAL_INDEX_NOT_DEFAULT_STORAGE",
                    "COLLECTION_PRESSURE_SHRINK_RELEASES_BACKING",
                    "COLLECTION_PRESSURE_RECOVERY_IS_LAZY",
                    "LLM_REPOSITORY_EXECUTION_REQUIRES_GIT_TREE_AND_RECIPE_COVERAGE",
                    "PRIVATE_ATOM_DECOMPOSITION_UNBOUNDED_UNDER_CONTRACT_LOCK",
                    "ATOM_REPLACEMENT_REQUIRES_DIFFERENTIAL_PROOF",
                    "ATOM_TO_FILE_PACKAGE_MODULE_PROJECT_RECOMPOSITION",
                    "JAVA_ORACLE_PRECEDES_NATIVE_ACCELERATION",
                    "NATIVE_DONOR_SERIAL_REVIEW_PRECEDES_JNI_CRATE",
                    "ALGORITHM_ATOM_MANIFEST_EXPOSES_NATIVE_DONOR_REVIEW_ROOTS",
                    "ALGORITHM_ATOM_PLAN_SERIAL_STRUCTURE_VERIFIED_BEFORE_MANIFEST",
                    "NATIVE_ACCELERATION_REQUIRES_PARITY_LIFECYCLE_AND_FALLBACK",
                    "PERFORMANCE_CLAIM_REQUIRES_SETUP_INCLUDED_BENCHMARK",
                    "PINNED_JAVA_CORPUS_BINDS_RECIPE_WORK_ORDER",
                    "PINNED_JAVA_CORPUS_BINDS_ATOM_REVIEW",
                    "SERIAL_DONOR_REVIEW_RECEIPT_BINDS_RECIPE_WORK_ORDER",
                    "MATERIALIZED_DONOR_ATOM_REVIEW_BINDS_WORK_ORDER",
                    "MATERIALIZED_DONOR_ATOM_REVIEW_BINDS_RECIPE_CRATE",
                    "RECIPE_CRATE_TARGET_SET_REVIEWED_SERIAL_FILE_BY_FILE",
                    "LINT_BEFORE_COMPILE",
                    "JDT_TO_RECIPE",
                    "CATALOGUE_TO_RECIPE",
                    "MULTIPASS_FIXED_POINT",
                    "POST_MERGE_CANONICAL_TREE_READBACK_REQUIRED",
                    "VERIFY_BEFORE_PROMOTE");

    private static final List<String> STAGES =
            List.of(
                    "DISCOVER",
                    "INVENTORY",
                    "CATALOGUE",
                    "CLASSIFY",
                    "CREATE_OR_IMPROVE_RECIPE",
                    "DRY_RUN",
                    "REVIEW",
                    "APPLY",
                    "VERIFY_CONTRACT_AND_BEHAVIOR",
                    "DIFF",
                    "LINT_AND_STATIC_ANALYSIS",
                    "COMPILE",
                    "TEST",
                    "RUNTIME_IF_REQUIRED",
                    "JAVA_ORACLE",
                    "JNI_PARITY_IF_DECLARED",
                    "BENCHMARK_IF_PERFORMANCE_CLAIM",
                    "SECOND_PASS_FIXED_POINT",
                    "PROMOTE");

    private M3RecipeFirstInvariant() {}

    public static List<String> canonicalLaws() {
        return LAWS;
    }

    public static List<String> canonicalStages() {
        return STAGES;
    }

    /**
     * Canonical M3 execution policy: independent file-local discovery/candidate computation may
     * parallelize, while canonical promotion remains serial and proof-gated.
     */
    public static FileExecutionPolicy canonicalFileExecutionPolicy() {
        return new FileExecutionPolicy(true, true, true, true);
    }

    /** Canonical edit-scope promotion order for source-changing/recomposition work. */
    public static List<M3EditScope> canonicalEditScopeOrder() {
        return List.of(
                M3EditScope.FILE,
                M3EditScope.VISIBILITY,
                M3EditScope.PACKAGE,
                M3EditScope.MODULE,
                M3EditScope.MULTI_MODULE,
                M3EditScope.LIBRARY_API);
    }

    /** Canonical bounded multi-pass plan that consumes the edit-scope order. */
    public static List<M3CanonicalMultiPassPlan.Pass> canonicalMultiPassPlan() {
        return M3CanonicalMultiPassPlan.passes();
    }

    /** Fail closed when a recipe/pass attempts to exceed its granted edit scope. */
    public static void requireEditScope(
            M3EditScope granted,
            M3EditScope required) {
        M3EditScope checkedGranted = Objects.requireNonNull(granted, "granted");
        M3EditScope checkedRequired = Objects.requireNonNull(required, "required");
        if (!checkedGranted.permits(checkedRequired)) {
            throw new IllegalStateException(
                    "M3 edit scope " + checkedGranted + " does not permit " + checkedRequired);
        }
    }

    /** Exact hash-pinned target preimages determine the minimum physical scope of the crate. */
    public static void requireHashPinnedRecipeScope(
            M3HashPinnedJavaSnapshotRecipe recipe,
            M3EditScope granted) {
        M3HashPinnedJavaSnapshotRecipe checked =
                Objects.requireNonNull(recipe, "recipe");
        requireEditScope(granted, checked.requiredScope());
        if (checked.targetPaths().isEmpty() || checked.preimageManifest().isEmpty()) {
            throw new IllegalStateException("hash-pinned recipe has no exact target/preimage set");
        }
    }

    /**
     * Coverage policy follows semantic atom/pattern proof rather than line coverage theater.
     *
     * <p>Executable line/branch coverage stays at >=99%, while declared atom behaviors,
     * pattern/IOP roles and public contracts must all have explicit JUnit proof.</p>
     */
    public static RecipeProofPolicy canonicalRecipeProofPolicy() {
        return new RecipeProofPolicy(
                0.99d,
                0.99d,
                true,
                true,
                true,
                true,
                true);
    }

    public record RecipeProofPolicy(
            double minimumLineCoverage,
            double minimumBranchCoverage,
            boolean junitRequired,
            boolean atomBehaviorProofRequired,
            boolean patternRoleProofRequired,
            boolean iopProofRequired,
            boolean publicContractProofRequired) {
        public RecipeProofPolicy {
            if (minimumLineCoverage < 0.99d
                    || minimumBranchCoverage < 0.99d
                    || !junitRequired
                    || !atomBehaviorProofRequired
                    || !patternRoleProofRequired
                    || !iopProofRequired
                    || !publicContractProofRequired) {
                throw new IllegalArgumentException(
                        "canonical M3 recipe proof policy cannot weaken 99%/semantic proof gates");
            }
        }
    }

    /**
     * Canonical collection/storage policy derived from the repository-wide M3 laws.
     *
     * <p>Storage stays lane/slot based. Existing Java APIs may materialize a fresh Entry only at
     * the caller boundary. Physical lookup layout remains a provider choice. Optional residency
     * may shrink to zero through the existing MIndex swap plane without changing canonical
     * collection identity or durable page bytes.</p>
     */
    public static CollectionStoragePolicy canonicalCollectionStoragePolicy() {
        return new CollectionStoragePolicy(
                true, true, true, true, true, true, true, true, true, true);
    }

    /**
     * Strict whole-repository evidence fence for LLM-assisted source-changing work.
     *
     * <p>Both exact Git-tree inventory coverage and exact recipe-capability coverage are required.
     * This is admission only; it grants no mutation or promotion authority.</p>
     */
    public static void requireStrictLlmRepositoryExecution(
            M3RepositorySupersetExecutionCli.Options options) {
        M3RepositorySupersetExecutionCli.Options checked =
                Objects.requireNonNull(options, "options");
        if (!checked.requireRecipeCoverage() || !checked.requireGitTreeCoverage()) {
            throw new IllegalStateException(
                    "M3 LLM repository execution requires Git-tree and recipe-capability coverage");
        }
    }

    public record CollectionStoragePolicy(
            boolean noRetainedStructuralEntryObjects,
            boolean freshUnpooledJdkEntryProjection,
            boolean primitiveAspectWord,
            boolean providerOwnsPhysicalIndexStrategy,
            boolean hashIsOptionalIndex,
            boolean reusesMIndexSwapPlane,
            boolean sqliteColdPageIdentityStable,
            boolean zeroResidencyExplicitDemandSupported,
            boolean pressureShrinkReleasesBacking,
            boolean pressureRecoveryIsLazy) {
        public CollectionStoragePolicy {
            if (!noRetainedStructuralEntryObjects
                    || !freshUnpooledJdkEntryProjection
                    || !primitiveAspectWord
                    || !providerOwnsPhysicalIndexStrategy
                    || !hashIsOptionalIndex
                    || !reusesMIndexSwapPlane
                    || !sqliteColdPageIdentityStable
                    || !zeroResidencyExplicitDemandSupported
                    || !pressureShrinkReleasesBacking
                    || !pressureRecoveryIsLazy) {
                throw new IllegalArgumentException(
                        "canonical M3 collection policy cannot weaken storage/residency invariants");
            }
        }
    }

    /**
     * Mechanical fence for LLM-assisted source-changing work.
     *
     * <p>The reusable Maven/OpenRewrite recipe crate is the work unit. Neither target files nor
     * donor source receive direct mutation/copy authority from the LLM work order.</p>
     */
    public static void requireLlmRecipeCrate(M3LlmRecipeWorkOrder.WorkOrder workOrder) {
        M3LlmRecipeWorkOrder.WorkOrder checked =
                Objects.requireNonNull(workOrder, "workOrder");
        if (!checked.recipeCrateIsWorkUnit()
                || !checked.candidateOnly()
                || !checked.serialPromotionRequired()
                || checked.targetFileEditAuthority()
                || checked.donorSourceCopyAuthority()
                || checked.mavenModule().isBlank()
                || checked.mavenProfile().isBlank()) {
            throw new IllegalStateException(
                    "M3 LLM source change must be a candidate-only Maven recipe-crate work order "
                            + "with serial proof-gated promotion and no direct file/donor authority");
        }
    }

    /** Generic task-crate fence used before Maven/OpenRewrite task admission. */
    public static void requireLlmTaskRecipeCrate(M3LlmTaskRecipeCrate crate) {
        M3LlmTaskRecipeCrate checked = Objects.requireNonNull(crate, "crate");
        if (!checked.recipeCrateIsWorkUnit()
                || !checked.candidateOnly()
                || !checked.serialPromotionRequired()
                || checked.directFileMutationAllowed()
                || checked.donorSourceCopyAllowed()
                || checked.canonicalPromotionAllowed()
                || checked.targetPaths().isEmpty()
                || checked.requiredGates().size() != M3LlmTaskRecipeCrate.Gate.values().length) {
            throw new IllegalStateException(
                    "M3 LLM task must enter through one bounded candidate-only recipe crate "
                            + "with at least one target, all proof gates and serial promotion");
        }
    }

    /**
     * Exact custody fence joining one admitted recipe crate to its mechanical donor-shape review.
     *
     * <p>This is evidence-only. It grants no target mutation, donor-copy, replacement or canonical
     * promotion authority.</p>
     */
    public static void requireMechanicalDonorRecipeCrateBinding(
            M3MechanicalDonorRecipeCrateBinding binding) {
        M3MechanicalDonorRecipeCrateBinding checked =
                Objects.requireNonNull(binding, "binding");
        if (checked.reviewedRepositories().isEmpty()
                || !checked.reviewedRepositories()
                        .containsAll(checked.crateMechanicalRepositories())
                || checked.sourceMutationAuthority()
                || checked.donorSourceCopyAuthority()
                || checked.replacementAuthority()
                || checked.promotionAuthority()) {
            throw new IllegalStateException(
                    "M3 recipe crate requires exact authority-free mechanical donor review binding");
        }
    }

    /**
     * Exact custody fence joining one catalogued algorithm atom to its mechanical donor review.
     *
     * <p>Shapes absent from the mechanical catalogue do not require this receipt. When present,
     * the receipt remains candidate-only and authority-free.</p>
     */
    public static void requireAlgorithmAtomMechanicalDonorBinding(
            M3AlgorithmAtomMechanicalDonorBinding binding) {
        M3AlgorithmAtomMechanicalDonorBinding checked =
                Objects.requireNonNull(binding, "binding");
        if (!M3AlgorithmAtomMechanicalDonorBinding
                        .isMechanicalReviewRequired(checked.shape())
                || checked.algorithmAtomBindingRoot().isBlank()
                || checked.atomRoot().isBlank()
                || checked.crateRoot().isBlank()
                || checked.admissionRoot().isBlank()
                || checked.mechanicalCrateBindingRoot().isBlank()
                || checked.mechanicalCatalogueRoot().isBlank()
                || checked.mechanicalReviewRoot().isBlank()
                || checked.nativeDonorReviewRoot().isBlank()
                || checked.sourceMutationAuthority()
                || checked.donorSourceCopyAuthority()
                || checked.replacementAuthority()
                || checked.promotionAuthority()) {
            throw new IllegalStateException(
                    "M3 catalogued algorithm atom requires exact authority-free mechanical donor binding");
        }
    }

    /**
     * Source-bound serial review fence for one algorithm atom.
     *
     * <p>Catalogued shapes must carry the exact mechanical binding into the executed review.
     * Uncatalogued shapes preserve the existing source-bound lane.</p>
     */
    public static void requireAlgorithmAtomSourceBoundCandidate(
            M3RecipeCrateSerialAtomReview.AtomSourceBoundCandidate candidate) {
        M3RecipeCrateSerialAtomReview.AtomSourceBoundCandidate checked =
                Objects.requireNonNull(candidate, "candidate");
        boolean required =
                M3AlgorithmAtomMechanicalDonorBinding
                        .isMechanicalReviewRequired(checked.atomBinding().shape());
        if (required != checked.mechanicalBinding().isPresent()
                || checked.coverageRoot().isBlank()
                || checked.sourceMutationAuthority()
                || checked.donorSourceCopyAuthority()
                || checked.promotionAuthority()) {
            throw new IllegalStateException(
                    "catalogued algorithm atom source-bound review requires exact mechanical binding");
        }
        checked.mechanicalBinding()
                .ifPresent(M3RecipeFirstInvariant::requireAlgorithmAtomMechanicalDonorBinding);
    }

    /**
     * Final executed-contract custody fence for one algorithm atom.
     *
     * <p>Catalogued shapes must retain the exact mechanical donor binding through the already
     * verified Java contract receipt. Uncatalogued shapes must carry no fabricated mechanical
     * roots. This receipt remains evidence-only and authority-free.</p>
     */
    public static void requireMechanicalExecutedContractReceipt(
            M3AlgorithmAtomMechanicalExecutedContractReceipt receipt) {
        M3AlgorithmAtomMechanicalExecutedContractReceipt checked =
                Objects.requireNonNull(receipt, "receipt");
        boolean required =
                M3AlgorithmAtomMechanicalDonorBinding
                        .isMechanicalReviewRequired(checked.shape());
        if (checked.mechanicalReviewRequired() != required
                || checked.algorithmAtomBindingRoot().isBlank()
                || checked.atomRoot().isBlank()
                || checked.crateRoot().isBlank()
                || checked.sourceBoundCandidateRoot().isBlank()
                || checked.executedContractRoot().isBlank()
                || checked.sourceMutationAuthority()
                || checked.donorSourceCopyAuthority()
                || checked.recipePromotionAuthority()
                || checked.repositoryPromotionAuthority()
                || checked.mergeAuthority()) {
            throw new IllegalStateException(
                    "executed algorithm contract lost mechanical donor custody");
        }
        if (required) {
            M3MechanicalDonorShapeReview.Receipt canonical =
                    M3MechanicalDonorShapeReview.review(checked.shape().name());
            if (!checked.mechanicalCatalogueRoot().equals(canonical.catalogueRoot())
                    || !checked.mechanicalReviewRoot().equals(canonical.reviewRoot())) {
                throw new IllegalStateException(
                        "executed contract mechanical review roots are not canonical");
            }
            if ("NONE".equals(checked.mechanicalBindingRoot())
                    || "NONE".equals(checked.mechanicalCatalogueRoot())
                    || "NONE".equals(checked.mechanicalReviewRoot())
                    || "NONE".equals(checked.nativeDonorReviewRoot())) {
                throw new IllegalStateException(
                        "catalogued executed contract requires mechanical donor roots");
            }
        } else if (!"NONE".equals(checked.mechanicalBindingRoot())
                || !"NONE".equals(checked.mechanicalCatalogueRoot())
                || !"NONE".equals(checked.mechanicalReviewRoot())
                || !"NONE".equals(checked.nativeDonorReviewRoot())) {
            throw new IllegalStateException(
                    "uncatalogued executed contract cannot invent mechanical donor roots");
        }
    }

    /**
     * Post-promotion custody fence for one algorithm atom.
     *
     * <p>The generic promotion receipt and canonical tree readback remain their own authorities.
     * This fence requires mechanical donor custody to survive through both boundaries without
     * granting mutation, copy, promotion or merge authority.</p>
     */
    public static void requireMechanicalPromotionReadbackReceipt(
            M3AlgorithmAtomMechanicalPromotionReadbackReceipt receipt) {
        M3AlgorithmAtomMechanicalPromotionReadbackReceipt checked =
                Objects.requireNonNull(receipt, "receipt");
        boolean required =
                M3AlgorithmAtomMechanicalDonorBinding
                        .isMechanicalReviewRequired(checked.shape());
        if (checked.mechanicalReviewRequired() != required
                || checked.sourcePath().isBlank()
                || checked.algorithmAtomBindingRoot().isBlank()
                || checked.atomRoot().isBlank()
                || checked.crateRoot().isBlank()
                || checked.executedMechanicalContractRoot().isBlank()
                || checked.promotionReceiptRoot().isBlank()
                || checked.canonicalReadbackRoot().isBlank()
                || !checked.expectedGitBlob().equals(checked.actualGitBlob())
                || checked.sourceMutationAuthority()
                || checked.donorSourceCopyAuthority()
                || checked.recipePromotionAuthority()
                || checked.repositoryPromotionAuthority()
                || checked.mergeAuthority()) {
            throw new IllegalStateException(
                    "algorithm atom lost mechanical custody through promotion/readback");
        }
        if (required) {
            M3MechanicalDonorShapeReview.Receipt canonical =
                    M3MechanicalDonorShapeReview.review(checked.shape().name());
            if (!checked.mechanicalCatalogueRoot().equals(canonical.catalogueRoot())
                    || !checked.mechanicalReviewRoot().equals(canonical.reviewRoot())
                    || "NONE".equals(checked.mechanicalBindingRoot())
                    || "NONE".equals(checked.nativeDonorReviewRoot())) {
                throw new IllegalStateException(
                        "post-promotion mechanical donor roots are not canonical");
            }
        } else if (!"NONE".equals(checked.mechanicalBindingRoot())
                || !"NONE".equals(checked.mechanicalCatalogueRoot())
                || !"NONE".equals(checked.mechanicalReviewRoot())
                || !"NONE".equals(checked.nativeDonorReviewRoot())) {
            throw new IllegalStateException(
                    "uncatalogued promoted atom cannot invent mechanical donor roots");
        }
    }

    /**
     * Canonical private-implementation refinement policy.
     *
     * <p>M3 does not impose an arbitrary iteration limit on private decomposition/refinement.
     * Refinement remains candidate-only until the public contract and external behavior are
     * unchanged and the recipe/differential/compiler/test/native gates are satisfied.</p>
     */
    public static AtomReplacementPolicy canonicalAtomReplacementPolicy() {
        return new AtomReplacementPolicy(true, true, true, true, true, true, -1);
    }

    /**
     * Execution budget corresponding to the policy-unbounded atom-refinement invariant.
     *
     * <p>{@code SerialAtomReview} uses zero to mean continue until stable, rejected, cycled or
     * canceled. This is an execution convention, not promotion authority.</p>
     */
    public static int canonicalSerialAtomMaxPasses() {
        return 0;
    }

    /** Promotion fence for replacing one private implementation atom. */
    public static void requireAtomReplacementPromotion(AtomReplacementEvidence evidence) {
        AtomReplacementEvidence checked =
                Objects.requireNonNull(evidence, "evidence");
        if (!checked.privateAtomOnly()
                || !checked.publicContractStable()
                || !checked.externalBehaviorStable()
                || !checked.recipeOwned()
                || !checked.differentialProofPassed()
                || !checked.compilerPassed()
                || !checked.testsPassed()
                || (checked.jniParityRequired() && !checked.jniParityPassed())
                || !checked.secondPassFixedPoint()) {
            throw new IllegalStateException(
                    "M3 private-atom replacement has unresolved contract, recipe, differential, "
                            + "compiler/test, JNI parity, or fixed-point gates");
        }
    }

    /**
     * Canonical source-bound private-atom promotion fence.
     *
     * <p>The first pass must be one independently verified changed atom. The next pass must start
     * from the exact applied postimage, use the same recipe artifact/catalogue, and prove a
     * source-bound no-op fixed point. Serial canonical promotion remains outside this method.</p>
     */
    public static void requireAtomReplacementPromotion(
            M3RecipePassReceipt appliedPass,
            M3SourceCoverageGate.Result appliedCoverage,
            M3RecipePassReceipt secondPass,
            M3SourceCoverageGate.Result secondCoverage) {
        Objects.requireNonNull(appliedPass, "appliedPass");
        Objects.requireNonNull(appliedCoverage, "appliedCoverage");
        Objects.requireNonNull(secondPass, "secondPass");
        Objects.requireNonNull(secondCoverage, "secondCoverage");

        requirePromotable(appliedPass, appliedCoverage);
        requireFixedPoint(secondPass, secondCoverage);

        if (appliedCoverage.request() == null
                || appliedCoverage.atoms().size() != 1
                || appliedCoverage.sourceMutationAuthority()
                || appliedCoverage.promotionAuthority()
                || !appliedCoverage.path().equals(secondCoverage.path())
                || !appliedPass.postimageRootSha256().equals(secondPass.preimageRootSha256())
                || !appliedPass.recipeClassName().equals(secondPass.recipeClassName())
                || !appliedPass.recipeArtifactSha256().equals(secondPass.recipeArtifactSha256())
                || !appliedPass.catalogueRootSha256().equals(secondPass.catalogueRootSha256())
                || !appliedPass.catalogueSnapshotSha256()
                        .equals(secondPass.catalogueSnapshotSha256())) {
            throw new IllegalStateException(
                    "M3 private-atom promotion requires one source-bound changed atom followed by "
                            + "the same recipe/artifact/catalogue at an exact fixed point");
        }
    }

    public record AtomReplacementPolicy(
            boolean publicContractLocked,
            boolean externalBehaviorLocked,
            boolean privateDecompositionAllowed,
            boolean recipeRequired,
            boolean differentialProofRequired,
            boolean serialPromotionRequired,
            int maxRefinementPasses) {
        public AtomReplacementPolicy {
            if (!publicContractLocked
                    || !externalBehaviorLocked
                    || !privateDecompositionAllowed
                    || !recipeRequired
                    || !differentialProofRequired
                    || !serialPromotionRequired
                    || maxRefinementPasses < -1
                    || maxRefinementPasses == 0) {
                throw new IllegalArgumentException(
                        "canonical M3 atom-replacement policy cannot weaken contract/proof gates");
            }
        }

        public boolean policyUnboundedRefinement() {
            return maxRefinementPasses == -1;
        }
    }

    public record AtomReplacementEvidence(
            boolean privateAtomOnly,
            boolean publicContractStable,
            boolean externalBehaviorStable,
            boolean recipeOwned,
            boolean differentialProofPassed,
            boolean compilerPassed,
            boolean testsPassed,
            boolean jniParityRequired,
            boolean jniParityPassed,
            boolean secondPassFixedPoint) {}

    public record FileExecutionPolicy(
            boolean fileLocalAnalysisParallel,
            boolean candidateTransformsParallel,
            boolean canonicalPromotionSerial,
            boolean proofGateRequired) {
        public FileExecutionPolicy {
            if (!fileLocalAnalysisParallel
                    || !candidateTransformsParallel
                    || !canonicalPromotionSerial
                    || !proofGateRequired) {
                throw new IllegalArgumentException(
                        "canonical M3 file execution policy cannot weaken parallel-candidate/serial-promotion gates");
            }
        }
    }

    /** Python contract fence for PlainText-backed recipes and external Maven workspaces. */
    public static void requirePythonContractPreserved(
            String sourcePath, String before, String after) {
        String prior =
                M3PythonAtomizer.contractFingerprint(
                        M3PythonAtomizer.analyze(sourcePath, before));
        String candidate =
                M3PythonAtomizer.contractFingerprint(
                        M3PythonAtomizer.analyze(sourcePath, after));
        if (!prior.equals(candidate)) {
            throw new IllegalStateException(
                    "M3 Python recipe changed declaration contract/interface");
        }
    }

    public static Decision admit(
            M3RecipeCatalogue catalogue,
            String capabilityId,
            List<M3CodeAtom> atoms) {
        Objects.requireNonNull(catalogue, "catalogue");
        List<M3CodeAtom> checkedAtoms =
                List.copyOf(Objects.requireNonNull(atoms, "atoms"));

        if (checkedAtoms.stream().anyMatch(M3CodeAtom::blocksMutation)) {
            return new Decision(Action.HOLD_FOR_REINVENTORY, checkedAtoms);
        }

        M3RecipeCatalogue.Resolution resolution =
                catalogue.resolveForLlmTask(capabilityId);
        if (resolution.disposition()
                == M3RecipeCatalogue.Disposition.CREATE_OR_IMPROVE_RECIPE) {
            return new Decision(
                    Action.CREATE_OR_IMPROVE_RECIPE,
                    checkedAtoms);
        }
        return new Decision(Action.DRY_RUN_RECIPE, checkedAtoms);
    }

    /**
     * Fails unless a source-changing task has reached recipe dry-run admission.
     *
     * <p>This method is not mutation authority. It prevents inventory or a recipe-catalogue miss
     * from being treated as permission to edit target files directly.</p>
     */
    public static void requireDryRunAdmission(Decision decision) {
        Decision checked = Objects.requireNonNull(decision, "decision");
        if (checked.action() != Action.DRY_RUN_RECIPE) {
            throw new IllegalStateException(
                    "M3 source change has not reached recipe dry-run admission: "
                            + checked.action());
        }
    }

    public static void requireFixedPoint(M3RecipePassReceipt secondPass) {
        Objects.requireNonNull(secondPass, "secondPass");
        if (!secondPass.fixedPoint()) {
            throw new IllegalStateException(
                    "M3 recipe is not at a fixed point; improve the recipe before promotion");
        }
    }

    public static void requirePromotable(M3RecipePassReceipt appliedPass) {
        Objects.requireNonNull(appliedPass, "appliedPass");
        if (!appliedPass.promotable()) {
            throw new IllegalStateException(
                    "M3 pass has unresolved contract, verification, JNI, or benchmark gates");
        }
    }

    public static void requireFixedPoint(M3RecipePassReceipt secondPass,
                                         M3SourceCoverageGate.Result coverage) {
        Objects.requireNonNull(secondPass, "secondPass");
        if (!secondPass.fixedPoint(coverage)) throw new IllegalStateException("M3 source-bound fixed point required");
    }

    public static void requirePromotable(M3RecipePassReceipt appliedPass,
                                         M3SourceCoverageGate.Result coverage) {
        Objects.requireNonNull(appliedPass, "appliedPass");
        if (!appliedPass.promotable(coverage)) throw new IllegalStateException("M3 source-bound atom/pattern proof required");
    }

    /**
     * Canonical post-promotion closure fence.
     *
     * <p>A merge/PR state or ancestry relation is provenance only. After serial canonical
     * promotion, every expected target path must be reread from the canonical branch/tree and its
     * Git blob identity must exactly match the reviewed expected postimage.</p>
     */
    public static void requirePostMergeCanonicalTreeReadback(
            CanonicalTreeReadbackEvidence evidence) {
        CanonicalTreeReadbackEvidence checked =
                Objects.requireNonNull(evidence, "evidence");
        if (!checked.exactTargetBlobsMatch()) {
            throw new IllegalStateException(
                    "M3 post-merge closure requires exact canonical-tree target blob readback");
        }
    }

    /** Immutable evidence supplied by the Git transport/API layer after canonical promotion. */
    public record CanonicalTreeReadbackEvidence(
            String canonicalBranch,
            String promotionCommitSha,
            String canonicalHeadSha,
            Map<String, String> expectedTargetBlobs,
            Map<String, String> actualTargetBlobs) {
        public CanonicalTreeReadbackEvidence {
            canonicalBranch = requireNonBlank(canonicalBranch, "canonicalBranch");
            promotionCommitSha = requireGitSha(promotionCommitSha, "promotionCommitSha");
            canonicalHeadSha = requireGitSha(canonicalHeadSha, "canonicalHeadSha");
            expectedTargetBlobs = validatedBlobMap(expectedTargetBlobs, "expectedTargetBlobs");
            actualTargetBlobs = validatedBlobMap(actualTargetBlobs, "actualTargetBlobs");
            if (expectedTargetBlobs.isEmpty()) {
                throw new IllegalArgumentException("expectedTargetBlobs");
            }
        }

        public boolean exactTargetBlobsMatch() {
            return expectedTargetBlobs.equals(actualTargetBlobs);
        }

        private static Map<String, String> validatedBlobMap(
                Map<String, String> input, String field) {
            Map<String, String> checked = Map.copyOf(Objects.requireNonNull(input, field));
            for (Map.Entry<String, String> entry : checked.entrySet()) {
                requireRepositoryPath(entry.getKey(), field + ".path");
                requireGitSha(entry.getValue(), field + ".blob");
            }
            return checked;
        }

        private static String requireRepositoryPath(String value, String field) {
            String checked = requireNonBlank(value, field);
            if (checked.startsWith("/") || checked.contains("\\")) {
                throw new IllegalArgumentException(field);
            }
            String[] segments = checked.split("/", -1);
            for (String segment : segments) {
                if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                    throw new IllegalArgumentException(field);
                }
            }
            return checked;
        }

        private static String requireGitSha(String value, String field) {
            String checked = requireNonBlank(value, field);
            if (!checked.matches("[0-9a-f]{40}")) {
                throw new IllegalArgumentException(field);
            }
            return checked;
        }

        private static String requireNonBlank(String value, String field) {
            String checked = Objects.requireNonNull(value, field).strip();
            if (checked.isEmpty()) throw new IllegalArgumentException(field);
            return checked;
        }
    }

    public enum Action {
        CREATE_OR_IMPROVE_RECIPE,
        HOLD_FOR_REINVENTORY,
        DRY_RUN_RECIPE
    }

    public record Decision(Action action, List<M3CodeAtom> atoms) {
        public Decision {
            action = Objects.requireNonNull(action, "action");
            atoms =
                    List.copyOf(
                            Objects.requireNonNull(atoms, "atoms"));
        }

        /** Admission is candidate-only; canonical mutation remains outside this decision. */
        public boolean replacementAuthority() {
            return false;
        }
    }
}
