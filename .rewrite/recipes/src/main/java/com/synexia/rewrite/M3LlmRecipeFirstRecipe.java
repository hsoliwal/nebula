// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.synexia.algorithms.corpus.CorpusSourceEntry;
import com.synexia.algorithms.corpus.JavaProblemCorpus;
import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.chrome2api.Chrome2ApiRecipePlan;
import com.synexia.rewrite.sealed.SealedTask;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Canonical recipe-first entry point for an LLM-assisted M3 coding task.
 *
 * <p>The task never receives direct file-mutation authority. Challenge/problem catalogues are
 * evidence-only. A known capability may enter the single IOP class-hooked mutation recipe; an
 * unknown capability must first be implemented or improved as a reusable recipe.</p>
 */
public final class M3LlmRecipeFirstRecipe extends Recipe {
    public static final String FULL_CAPABILITY = "m3.iop.full";
    public static final String DEFAULT_IOP_SOURCE_FILE_PATTERN = "**/src/main/java/com/synexia/iop/**/*.java";

    public enum Action {
        DRY_RUN_CANONICAL_IOP_RECIPE,
        CREATE_OR_IMPROVE_RECIPE,
        HOLD_FOR_REINVENTORY,
        TYPED_RESIDUE_NON_IOP,
        TYPED_RESIDUE_IOP_PATTERN
    }

    @Option(
            displayName = "LLM task capability",
            description = "Canonical M3 recipe capability/pass id. Unknown ids open recipe work instead of direct mutation.",
            example = "01-upper-case-literal-suffixes",
            required = false)
    private final String capabilityId;

    @Option(
            displayName = "Source file pattern",
            description = "Additional source glob. IOP production fence and pattern class hooks remain mandatory.",
            example = "synexia-iop/src/main/java/com/synexia/iop/**/*.java",
            required = false)
    private final String sourceFilePattern;

    @Option(
            displayName = "M3 proof scope",
            description = "Declared proof scope: AST_LEAF, FILE, PACKAGE, MODULE, or PROJECT. Parent scopes still require their own M3 verification.",
            example = "FILE",
            required = false)
    private final String scope;

    @Option(
            displayName = "Problem shape",
            description = "ALL or AlgorithmShape value used by the existing challenge posting index.",
            example = "BINARY_SEARCH",
            required = false)
    private final String problemShape;

    @Option(
            displayName = "Problem category",
            description = "Optional LeetCode/HackerRank/GeeksforGeeks category id, display name, or alias.",
            example = "binary-search",
            required = false)
    private final String problemCategory;

    @Option(
            displayName = "Problem term",
            description = "Optional metadata/search term. Challenge bodies and solutions are never copied.",
            example = "trie",
            required = false)
    private final String problemTerm;

    @Option(
            displayName = "Challenge result limit",
            description = "Maximum candidate evidence rows from the precomputed challenge index.",
            example = "1000",
            required = false)
    private final Integer challengeLimit;

    private final transient PlanTable planTable = new PlanTable(this);
    private final transient WorkOrderTable workOrderTable = new WorkOrderTable(this);

    public M3LlmRecipeFirstRecipe() {
        this(
                System.getProperty("m3.llm.capability", FULL_CAPABILITY),
                System.getProperty("m3.llm.sourceFilePattern", DEFAULT_IOP_SOURCE_FILE_PATTERN),
                System.getProperty("m3.llm.scope", "FILE"),
                System.getProperty("m3.llm.problemShape", "ALL"),
                System.getProperty("m3.llm.problemCategory", ""),
                System.getProperty("m3.llm.problemTerm", ""),
                Integer.getInteger("m3.llm.challengeLimit", 1000));
    }

    public M3LlmRecipeFirstRecipe(
            String capabilityId,
            String sourceFilePattern,
            String problemShape,
            String problemTerm,
            Integer challengeLimit) {
        this(capabilityId, sourceFilePattern, "FILE", problemShape, "", problemTerm, challengeLimit);
    }

    public M3LlmRecipeFirstRecipe(
            String capabilityId,
            String sourceFilePattern,
            String scope,
            String problemShape,
            String problemTerm,
            Integer challengeLimit) {
        this(capabilityId, sourceFilePattern, scope, problemShape, "", problemTerm, challengeLimit);
    }

    @JsonCreator
    public M3LlmRecipeFirstRecipe(
            String capabilityId,
            String sourceFilePattern,
            String scope,
            String problemShape,
            String problemCategory,
            String problemTerm,
            Integer challengeLimit) {
        this.capabilityId = token(capabilityId, "capabilityId");
        this.sourceFilePattern = token(sourceFilePattern, "sourceFilePattern");
        this.scope = normalizeScope(scope);
        this.problemShape = normalizeShape(problemShape);
        this.problemCategory = query(problemCategory);
        this.problemTerm = query(problemTerm);
        this.challengeLimit = Objects.requireNonNullElse(challengeLimit, 1000);
        if (this.challengeLimit < 1 || this.challengeLimit > 100_000) {
            throw new IllegalArgumentException("challengeLimit must be between 1 and 100000");
        }
        // Constructor validation is delegated to the existing indexed review recipe too.
        new M3ChallengeFastSearchReviewRecipe(
                "ALL", this.problemShape, this.problemCategory, this.problemTerm, "", this.challengeLimit);
    }

    @Override
    public String getDisplayName() {
        return "M3 LLM recipe-first IOP task";
    }

    @Override
    public String getDescription() {
        return "Plans every LLM-assisted coding task through challenge evidence, recipe capability "
                + "resolution, IOP source/class hooks, candidate-only OpenRewrite mutation, and "
                + "external M3 proof/serial promotion. Unknown capabilities produce recipe work.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "llm",
                "recipe-first",
                "iop",
                "class-hooked",
                "candidate-only",
                "leetcode",
                "hackerrank",
                "geeksforgeeks");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> visitor = new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit unit, ExecutionContext context) {
                J.CompilationUnit visited = super.visitCompilationUnit(unit, context);
                Plan plan = inspect(visited);
                planTable.insertRow(context, PlanRow.from(plan));
                workOrderTable.insertRow(context, WorkOrderRow.from(recipeWorkOrder(plan)));
                return visited;
            }
        };
        return Preconditions.check(new FindSourceFiles(sourceFilePattern).getVisitor(), visitor);
    }

    @Override
    public List<Recipe> getRecipeList() {
        ArrayList<Recipe> recipes = new ArrayList<>();
        // Indexed category search nominates donor evidence before donor-body review.
        recipes.add(new M3ChallengeFastSearchReviewRecipe(
                "ALL", problemShape, problemCategory, problemTerm, "", challengeLimit));
        recipes.add(new M3ChallengeDonorReviewRecipe("ALL", "BOTH"));
        recipes.add(new M3ProblemDonorRepositoryReviewRecipe(
                "ALL", false, challengeLimit));
        if (knownCapability()) {
            recipes.add(new M3IopPatternOnlySerialMechanicalJavaRecipe(sourceFilePattern));
        }
        return List.copyOf(recipes);
    }

    public Plan inspect(J.CompilationUnit unit) {
        Objects.requireNonNull(unit, "unit");
        String path = portable(unit.getSourcePath());
        String challengeRoot = challengeSearchRoot();
        String donorRoot = donorReviewRoot();

        if (!M3IopSourceFence.admits(unit.getSourcePath())) {
            return plan(
                    path,
                    false,
                    "",
                    Action.TYPED_RESIDUE_NON_IOP,
                    "NON_IOP_SOURCE",
                    challengeRoot,
                    donorRoot);
        }

        M3IopFullScaleMutationAuthority.AuthorityReceipt authority =
                M3IopFullScaleMutationAuthority.receipt(unit, sourceFilePattern);

        if (!authority.admitted()) {
            return plan(
                    path,
                    false,
                    authority.root(),
                    Action.TYPED_RESIDUE_IOP_PATTERN,
                    authority.reason(),
                    challengeRoot,
                    donorRoot);
        }

        if (!knownCapability()) {
            return plan(
                    path,
                    true,
                    authority.root(),
                    Action.CREATE_OR_IMPROVE_RECIPE,
                    "CAPABILITY_NOT_IN_CANONICAL_IOP_RECIPE_PLAN",
                    challengeRoot,
                    donorRoot);
        }

        return plan(
                path,
                true,
                authority.root(),
                Action.DRY_RUN_CANONICAL_IOP_RECIPE,
                "ADMITTED_RECIPE_CANDIDATE_ONLY",
                challengeRoot,
                donorRoot);
    }

    /**
     * Compiles this source plan into the Maven/OpenRewrite recipe-crate work unit.
     *
     * <p>This is the only work-order projection exposed to LLM-assisted source-changing tasks.
     * It never grants target-file mutation, donor-copy, or promotion authority.</p>
     */
    public M3LlmRecipeWorkOrder.WorkOrder recipeWorkOrder(Plan plan) {
        M3LlmRecipeWorkOrder.WorkOrder workOrder =
                M3LlmRecipeWorkOrder.from(this, Objects.requireNonNull(plan, "plan"));
        M3RecipeFirstInvariant.requireLlmRecipeCrate(workOrder);
        return workOrder;
    }

    /**
     * Builds the bounded Chrome2api recipe plan for this M3 task.
     *
     * <p>The plan is candidate-only. Canonical mutation and promotion remain under existing M3
     * proof gates.</p>
     */
    public Chrome2ApiRecipePlan chrome2ApiRecipePlan(
            String taskId,
            M3RecipeCatalogue catalogue,
            List<String> targetPaths,
            List<String> atomIds,
            List<String> donorRepositories) {
        Objects.requireNonNull(catalogue, "catalogue");
        return new M3Chrome2ApiRecipePlanner(catalogue)
                .plan(taskId, capabilityId, targetPaths, atomIds, donorRepositories);
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }

    public String getScope() {
        return scope;
    }

    public String getProblemShape() {
        return problemShape;
    }

    public String getProblemCategory() {
        return problemCategory;
    }

    public String getProblemTerm() {
        return problemTerm;
    }

    public Integer getChallengeLimit() {
        return challengeLimit;
    }

    public boolean knownCapability() {
        return FULL_CAPABILITY.equals(capabilityId)
                || M3IopFullScaleMutationAuthority.canonicalRecipeClassName().equals(capabilityId)
                || M3IopFullScaleMutationAuthority.canonicalPassIds(sourceFilePattern).contains(capabilityId);
    }

    public List<String> canonicalCapabilities() {
        ArrayList<String> values = new ArrayList<>();
        values.add(FULL_CAPABILITY);
        values.add(M3IopFullScaleMutationAuthority.canonicalRecipeClassName());
        values.addAll(M3IopFullScaleMutationAuthority.canonicalPassIds(sourceFilePattern));
        return values.stream().distinct().toList();
    }

    public String challengeSearchRoot() {
        return new M3ChallengeFastSearchReviewRecipe(
                        "ALL", problemShape, problemCategory, problemTerm, "", challengeLimit)
                .searchRoot();
    }

    public String javaCorpusReviewRoot() {
        return JavaProblemCorpus.resolvedIndexSnapshot().root();
    }

    /**
     * Exact pinned Java donor files for a concrete problem shape.
     *
     * <p>ALL still binds the global corpus root but does not explode every shape's donor rows into
     * one task packet.</p>
     */
    public List<String> javaCorpusDonorSources() {
        if ("ALL".equals(problemShape)) {
            return List.of();
        }
        AlgorithmShape shape = AlgorithmShape.valueOf(problemShape);
        int limit = Math.min(1000, challengeLimit);
        return JavaProblemCorpus.resolvedEvidenceByShape(shape, limit).stream()
                .map(CorpusSourceEntry::evidenceId)
                .sorted()
                .toList();
    }

    public String donorReviewRoot() {
        String challengeDonorRoot =
                new M3ChallengeDonorReviewRecipe("ALL", "BOTH").reviewRoot();
        String pinnedRepositoryRoot =
                new M3ProblemDonorRepositoryReviewRecipe("ALL", false, challengeLimit)
                        .catalogueRoot();
        return digest(
                "M3_LLM_DONOR_REVIEW_ROOT_V4",
                challengeDonorRoot,
                pinnedRepositoryRoot,
                javaCorpusReviewRoot(),
                String.join("\u001f", javaCorpusDonorSources()),
                problemCategory,
                String.join("\u001f", categoryDonorRepositories()));
    }

    /** Indexed category/shape/term donor references; evidence only, never copy authority. */
    public List<String> categoryDonorRepositories() {
        return new M3ChallengeFastSearchReviewRecipe(
                        "ALL", problemShape, problemCategory, problemTerm, "", challengeLimit)
                .plannedRows().stream()
                .flatMap(row -> java.util.Arrays.stream(row.getDonorRepositories().split(",")))
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    public boolean directFileMutationAuthority() {
        return false;
    }

    public boolean replacementAuthority() {
        return false;
    }

    public boolean sourceCopyAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private Plan plan(
            String sourcePath,
            boolean iopAdmitted,
            String authorityRoot,
            Action action,
            String reason,
            String challengeRoot,
            String donorRoot) {
        return new Plan(
                capabilityId,
                sourcePath,
                scope,
                iopAdmitted,
                authorityRoot,
                action,
                reason,
                M3IopFullScaleMutationAuthority.canonicalRecipeClassName(),
                M3IopFullScaleMutationAuthority.canonicalPassIds(sourceFilePattern),
                challengeRoot,
                donorRoot,
                javaCorpusReviewRoot(),
                javaCorpusDonorSources(),
                true,
                true,
                false,
                false,
                false,
                "");
    }

    public record Plan(
            String capabilityId,
            String sourcePath,
            String scope,
            boolean iopAdmitted,
            String authorityRoot,
            Action action,
            String reason,
            String canonicalRecipeClass,
            List<String> passIds,
            String challengeSearchRoot,
            String donorReviewRoot,
            String javaCorpusReviewRoot,
            List<String> javaCorpusDonorSources,
            boolean candidateOnly,
            boolean serialPromotionRequired,
            boolean directFileMutationAuthority,
            boolean sourceCopyAuthority,
            boolean promotionAuthority,
            String root) {

        public Plan {
            capabilityId = token(capabilityId, "capabilityId");
            sourcePath = token(sourcePath, "sourcePath");
            scope = normalizeScope(scope);
            authorityRoot = optionalSha(authorityRoot, "authorityRoot");
            action = Objects.requireNonNull(action, "action");
            reason = token(reason, "reason");
            canonicalRecipeClass = token(canonicalRecipeClass, "canonicalRecipeClass");
            passIds = passIds == null ? List.of() : List.copyOf(passIds);
            challengeSearchRoot = sha(challengeSearchRoot, "challengeSearchRoot");
            donorReviewRoot = sha(donorReviewRoot, "donorReviewRoot");
            javaCorpusReviewRoot = sha(javaCorpusReviewRoot, "javaCorpusReviewRoot");
            javaCorpusDonorSources = stableDonorSources(javaCorpusDonorSources);
            if (!candidateOnly || !serialPromotionRequired) {
                throw new IllegalArgumentException("M3 LLM plan must remain candidate-only with serial promotion");
            }
            if (directFileMutationAuthority || sourceCopyAuthority || promotionAuthority) {
                throw new IllegalArgumentException("M3 LLM plan cannot own direct mutation/copy/promotion authority");
            }
            if (iopAdmitted && authorityRoot.isEmpty()) {
                throw new IllegalArgumentException("admitted IOP plan requires authorityRoot");
            }
            String expected = digest(
                    "M3_LLM_RECIPE_FIRST_PLAN_V3",
                    capabilityId,
                    sourcePath,
                    scope,
                    Boolean.toString(iopAdmitted),
                    authorityRoot,
                    action.name(),
                    reason,
                    canonicalRecipeClass,
                    String.join("\u001f", passIds),
                    challengeSearchRoot,
                    donorReviewRoot,
                    javaCorpusReviewRoot,
                    String.join("\u001f", javaCorpusDonorSources),
                    Boolean.toString(candidateOnly),
                    Boolean.toString(serialPromotionRequired));
            if (root == null || root.isBlank()) root = expected;
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("M3 LLM plan root mismatch");
            }
        }

        public boolean recipeWorkRequired() {
            return action == Action.CREATE_OR_IMPROVE_RECIPE;
        }

        public boolean dryRunAllowed() {
            return action == Action.DRY_RUN_CANONICAL_IOP_RECIPE;
        }
    }

    public static final class PlanTable extends DataTable<PlanRow> {
        PlanTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 LLM recipe-first task plan",
                    "Per-source admission showing challenge evidence, IOP class hooks, recipe disposition, "
                            + "and the absence of direct mutation/copy/promotion authority.");
        }
    }

    public static final class PlanRow {
        @Column(displayName = "Capability", description = "Requested M3 recipe capability.")
        private final String capabilityId;

        @Column(displayName = "Source", description = "Repository-relative source path.")
        private final String sourcePath;

        @Column(displayName = "Scope", description = "Declared M3 proof scope; parent proof remains separate.")
        private final String scope;

        @Column(displayName = "IOP admitted", description = "True only after source fence + class hooks.")
        private final boolean iopAdmitted;

        @Column(displayName = "Action", description = "Recipe-first task disposition.")
        private final String action;

        @Column(displayName = "Reason", description = "Deterministic disposition rationale.")
        private final String reason;

        @Column(displayName = "Canonical recipe", description = "Only class-backed mutation recipe allowed.")
        private final String canonicalRecipeClass;

        @Column(displayName = "Challenge root", description = "Precomputed LeetCode/HackerRank/GFG search root.")
        private final String challengeSearchRoot;

        @Column(displayName = "Donor review root", description = "Candidate-only donor/category review root.")
        private final String donorReviewRoot;

        @Column(displayName = "Java corpus root", description = "Resolved JavaProblemCorpus snapshot root.")
        private final String javaCorpusReviewRoot;

        @Column(displayName = "Java donor sources", description = "Exact pinned donor files for a concrete requested shape.")
        private final String javaCorpusDonorSources;

        @Column(displayName = "Plan root", description = "SHA-256 identity of the complete task plan.")
        private final String planRoot;

        @Column(displayName = "Replacement authority", description = "Always false.")
        private final boolean replacementAuthority;

        private PlanRow(
                String capabilityId,
                String sourcePath,
                String scope,
                boolean iopAdmitted,
                String action,
                String reason,
                String canonicalRecipeClass,
                String challengeSearchRoot,
                String donorReviewRoot,
                String javaCorpusReviewRoot,
                String javaCorpusDonorSources,
                String planRoot) {
            this.capabilityId = capabilityId;
            this.sourcePath = sourcePath;
            this.scope = scope;
            this.iopAdmitted = iopAdmitted;
            this.action = action;
            this.reason = reason;
            this.canonicalRecipeClass = canonicalRecipeClass;
            this.challengeSearchRoot = challengeSearchRoot;
            this.donorReviewRoot = donorReviewRoot;
            this.javaCorpusReviewRoot = javaCorpusReviewRoot;
            this.javaCorpusDonorSources = javaCorpusDonorSources;
            this.planRoot = planRoot;
            this.replacementAuthority = false;
        }

        static PlanRow from(Plan plan) {
            return new PlanRow(
                    plan.capabilityId(),
                    plan.sourcePath(),
                    plan.scope(),
                    plan.iopAdmitted(),
                    plan.action().name(),
                    plan.reason(),
                    plan.canonicalRecipeClass(),
                    plan.challengeSearchRoot(),
                    plan.donorReviewRoot(),
                    plan.javaCorpusReviewRoot(),
                    String.join(",", plan.javaCorpusDonorSources()),
                    plan.root());
        }

        public String getCapabilityId() { return capabilityId; }
        public String getSourcePath() { return sourcePath; }
        public String getScope() { return scope; }
        public boolean isIopAdmitted() { return iopAdmitted; }
        public String getAction() { return action; }
        public String getReason() { return reason; }
        public String getCanonicalRecipeClass() { return canonicalRecipeClass; }
        public String getChallengeSearchRoot() { return challengeSearchRoot; }
        public String getDonorReviewRoot() { return donorReviewRoot; }
        public String getJavaCorpusReviewRoot() { return javaCorpusReviewRoot; }
        public String getJavaCorpusDonorSources() { return javaCorpusDonorSources; }
        public String getPlanRoot() { return planRoot; }
        public boolean isReplacementAuthority() { return replacementAuthority; }
    }

    public static final class WorkOrderTable extends DataTable<WorkOrderRow> {
        WorkOrderTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 LLM Maven recipe work orders",
                    "Recipe-crate work units emitted by the canonical LLM recipe-first profile. "
                            + "Rows never grant target-file edit, donor-copy, or promotion authority.");
        }
    }

    public static final class WorkOrderRow {
        @Column(displayName = "Capability", description = "Requested capability id.")
        private final String capabilityId;

        @Column(displayName = "Mode", description = "Dry-run canonical recipe, author/improve recipe, or typed residue.")
        private final String mode;

        @Column(displayName = "Scope", description = "Declared M3 proof scope.")
        private final String scope;

        @Column(displayName = "Maven module", description = "Recipe crate Maven module.")
        private final String mavenModule;

        @Column(displayName = "Maven profile", description = "Canonical recipe-first Maven profile.")
        private final String mavenProfile;

        @Column(displayName = "Recipe class", description = "Canonical or suggested class-backed recipe.")
        private final String recipeClass;

        @Column(displayName = "Test class", description = "Required recipe regression test class.")
        private final String testClass;

        @Column(displayName = "Fixture directory", description = "Required bounded before/after fixture directory.")
        private final String fixtureDirectory;

        @Column(displayName = "Java corpus root", description = "Resolved JavaProblemCorpus root bound to the recipe crate.")
        private final String javaCorpusReviewRoot;

        @Column(displayName = "Java donor sources", description = "Exact pinned Java donor files bound to this LLM work order.")
        private final String javaCorpusDonorSources;

        @Column(displayName = "Serial atom reviewer", description = "Canonical serial atom convergence owner.")
        private final String serialAtomReviewer;

        @Column(displayName = "Work-order root", description = "SHA-256 identity of the complete recipe-crate work unit.")
        private final String root;

        @Column(displayName = "Target-file edit authority", description = "Always false.")
        private final boolean targetFileEditAuthority;

        private WorkOrderRow(
                String capabilityId,
                String mode,
                String scope,
                String mavenModule,
                String mavenProfile,
                String recipeClass,
                String testClass,
                String fixtureDirectory,
                String javaCorpusReviewRoot,
                String javaCorpusDonorSources,
                String serialAtomReviewer,
                String root) {
            this.capabilityId = capabilityId;
            this.mode = mode;
            this.scope = scope;
            this.mavenModule = mavenModule;
            this.mavenProfile = mavenProfile;
            this.recipeClass = recipeClass;
            this.testClass = testClass;
            this.fixtureDirectory = fixtureDirectory;
            this.javaCorpusReviewRoot = javaCorpusReviewRoot;
            this.javaCorpusDonorSources = javaCorpusDonorSources;
            this.serialAtomReviewer = serialAtomReviewer;
            this.root = root;
            this.targetFileEditAuthority = false;
        }

        static WorkOrderRow from(M3LlmRecipeWorkOrder.WorkOrder work) {
            return new WorkOrderRow(
                    work.capabilityId(),
                    work.mode().name(),
                    work.scope(),
                    work.mavenModule(),
                    work.mavenProfile(),
                    work.suggestedRecipeClass(),
                    work.suggestedTestClass(),
                    work.suggestedFixtureDirectory(),
                    work.javaCorpusReviewRoot(),
                    String.join(",", work.javaCorpusDonorSources()),
                    work.serialAtomReviewerClassName(),
                    work.root());
        }

        public String getCapabilityId() { return capabilityId; }
        public String getMode() { return mode; }
        public String getScope() { return scope; }
        public String getMavenModule() { return mavenModule; }
        public String getMavenProfile() { return mavenProfile; }
        public String getRecipeClass() { return recipeClass; }
        public String getTestClass() { return testClass; }
        public String getFixtureDirectory() { return fixtureDirectory; }
        public String getJavaCorpusReviewRoot() { return javaCorpusReviewRoot; }
        public String getJavaCorpusDonorSources() { return javaCorpusDonorSources; }
        public String getSerialAtomReviewer() { return serialAtomReviewer; }
        public String getRoot() { return root; }
        public boolean isTargetFileEditAuthority() { return targetFileEditAuthority; }
    }

    private static List<String> stableDonorSources(List<String> values) {
        return Objects.requireNonNull(values, "javaCorpusDonorSources").stream()
                .map(value -> token(value, "javaCorpusDonorSource"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String normalizeScope(String value) {
        String normalized = Objects.requireNonNullElse(value, "FILE").strip().toUpperCase(Locale.ROOT);
        try {
            return SealedTask.Scope.valueOf(normalized).name();
        } catch (IllegalArgumentException unsupported) {
            throw new IllegalArgumentException(
                    "scope must be AST_LEAF, FILE, PACKAGE, MODULE, or PROJECT", unsupported);
        }
    }

    private static String normalizeShape(String value) {
        String normalized = Objects.requireNonNullElse(value, "ALL").strip().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) normalized = "ALL";
        return normalized.replace('-', '_').replace(' ', '_');
    }

    private static String query(String value) {
        String text = Objects.requireNonNullElse(value, "").strip();
        if (text.length() > 4096 || text.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("problemTerm contains invalid control/length");
        }
        return text;
    }

    private static String token(String value, String name) {
        String text = Objects.requireNonNullElse(value, "").strip();
        if (text.isEmpty() || text.length() > 8192 || text.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name + " required");
        }
        return text;
    }

    private static String optionalSha(String value, String name) {
        String text = Objects.requireNonNullElse(value, "").strip();
        if (text.isEmpty()) return "";
        return sha(text, name);
    }

    private static String sha(String value, String name) {
        String text = Objects.requireNonNullElse(value, "").strip();
        if (!text.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
        return text;
    }

    private static String portable(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static String digest(String... fields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String field : fields) {
                byte[] bytes = Objects.requireNonNull(field, "field").getBytes(StandardCharsets.UTF_8);
                digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) ':');
                digest.update(bytes);
                digest.update((byte) '\n');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
