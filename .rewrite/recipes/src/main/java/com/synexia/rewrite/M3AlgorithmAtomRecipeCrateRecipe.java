// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.synexia.fastsearch.problem.ProblemCatalogue;
import com.synexia.fastsearch.problem.ProblemCatalogueTsv;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.text.PlainText;

/**
 * Inventory-first method-atom recipe-crate binder.
 *
 * <p>It scans exact Java file/method preimages, joins the canonical challenge/Git-donor/recipe
 * catalogues, emits deterministic serial work custody and optionally writes an audit TSV. Java
 * source is never changed.</p>
 */
public final class M3AlgorithmAtomRecipeCrateRecipe
        extends ScanningRecipe<M3AlgorithmAtomRecipeCrateRecipe.Inventory> {

    @Option(
            displayName = "Evidence limit",
            description = "Maximum per-shape challenge/donor evidence retained in each atom binding.",
            example = "20",
            required = false)
    private final Integer evidenceLimit;

    @Option(
            displayName = "Manifest output path",
            description = "Optional repository-relative TSV path created or verified at fixed point.",
            example = "m3-algorithm-atom-recipe-crates.tsv",
            required = false)
    private final String manifestOutputPath;

    private final transient BindingTable bindingTable = new BindingTable(this);
    private final transient FileTable fileTable = new FileTable(this);

    public static final class Inventory {
        private final Map<String, M3AlgorithmAtomRecipeCratePlanner.FileInput> files =
                new TreeMap<>();
        private final List<M3AlgorithmAtomRecipeCratePlanner.AtomInput> atoms =
                new ArrayList<>();
        private String recipeCatalogueTsv;
        private String problemCatalogueTsv;
        private String existingManifest;
    }

    public M3AlgorithmAtomRecipeCrateRecipe() {
        this(20, "");
    }

    public M3AlgorithmAtomRecipeCrateRecipe(Integer evidenceLimit) {
        this(evidenceLimit, "");
    }

    @JsonCreator
    public M3AlgorithmAtomRecipeCrateRecipe(
            Integer evidenceLimit, String manifestOutputPath) {
        this.evidenceLimit = Objects.requireNonNullElse(evidenceLimit, 20);
        if (this.evidenceLimit < 1 || this.evidenceLimit > 1000) {
            throw new IllegalArgumentException("evidenceLimit");
        }
        this.manifestOutputPath =
                normalizeOptionalPath(
                        Objects.requireNonNullElse(manifestOutputPath, ""));
    }

    @Override
    public String getDisplayName() {
        return "M3 algorithm atom recipe-crate binding";
    }

    @Override
    public String getDescription() {
        return "Binds exact Java method atoms to balanced LeetCode/HackerRank/GeeksforGeeks "
                + "evidence, pinned GitHub donors, Maven/OpenRewrite recipe crates and the canonical "
                + "Java/JNI lane before serial atom review, without changing Java source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "recipe-first",
                "atom",
                "serial-review",
                "java",
                "jni",
                "leetcode",
                "hackerrank",
                "geeksforgeeks",
                "github-donor",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    public Integer getEvidenceLimit() {
        return evidenceLimit;
    }

    public String getManifestOutputPath() {
        return manifestOutputPath;
    }

    public boolean sourceMutationAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (tree instanceof J.CompilationUnit unit) {
                    stopAfterPreVisit();
                    scanJava(inventory, unit, context);
                } else if (tree instanceof PlainText text) {
                    stopAfterPreVisit();
                    scanPlainText(inventory, text);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory, ExecutionContext context) {
        M3RecipeCatalogue recipes =
                inventory.recipeCatalogueTsv == null
                        ? new M3RecipeCatalogue(List.of())
                        : M3RecipeCatalogueTsv.parse(inventory.recipeCatalogueTsv);
        ProblemCatalogue problems =
                inventory.problemCatalogueTsv == null
                        ? new ProblemCatalogue(List.of())
                        : new ProblemCatalogue(
                                ProblemCatalogueTsv.parse(
                                        inventory.problemCatalogueTsv));
        M3AlgorithmAtomRecipeCratePlanner.Plan plan =
                new M3AlgorithmAtomRecipeCratePlanner()
                        .plan(
                                List.copyOf(inventory.atoms),
                                List.copyOf(inventory.files.values()),
                                recipes,
                                problems,
                                evidenceLimit);

        plan.files().forEach(
                file -> fileTable.insertRow(context, FileRow.from(file)));
        plan.bindings().forEach(
                binding ->
                        bindingTable.insertRow(
                                context, BindingRow.from(binding)));

        if (manifestOutputPath.isEmpty()) {
            return List.of();
        }
        String manifest = M3AlgorithmAtomRecipeCrateManifest.render(plan);
        if (inventory.existingManifest == null) {
            return List.of(
                    PlainText.builder()
                            .sourcePath(Path.of(manifestOutputPath))
                            .text(manifest)
                            .build());
        }
        if (!inventory.existingManifest.equals(manifest)) {
            throw new IllegalStateException(
                    "algorithm atom recipe-crate manifest drift: "
                            + manifestOutputPath);
        }
        return List.of();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {};
    }

    private static void scanJava(
            Inventory inventory,
            J.CompilationUnit unit,
            ExecutionContext context) {
        String path =
                unit.getSourcePath()
                        .normalize()
                        .toString()
                        .replace('\\', '/');
        String source = unit.printAll();
        String fileRoot = M3SourceCoverageGate.hash(source);
        ArrayList<M3AlgorithmAtomRecipeCratePlanner.AtomInput> fileAtoms =
                new ArrayList<>();
        int[] counts = new int[3];

        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration visited =
                        super.visitMethodDeclaration(method, ctx);
                if (visited.hasModifier(J.Modifier.Type.Native)) {
                    counts[2]++;
                }
                if (visited.getBody() == null) {
                    return visited;
                }
                counts[0]++;

                J.ClassDeclaration owner =
                        getCursor()
                                .firstEnclosingOrThrow(
                                        J.ClassDeclaration.class);
                JavaType.FullyQualified ownerType =
                        TypeUtils.asFullyQualified(owner.getType());
                JavaType.Method methodType = visited.getMethodType();
                if (ownerType == null
                        || !TypeUtils.isWellFormedType(methodType)) {
                    counts[1]++;
                    return visited;
                }
                try {
                    String descriptor =
                            M3JniContractInventoryRecipe.descriptor(visited);
                    String methodSource =
                            visited.printTrimmed(getCursor());
                    fileAtoms.add(
                            new M3AlgorithmAtomRecipeCratePlanner.AtomInput(
                                    path,
                                    fileRoot,
                                    ownerType.getFullyQualifiedName(),
                                    visited.getSimpleName(),
                                    descriptor,
                                    methodSource,
                                    M3RecipeFirstJavaAtomInventoryRecipe
                                            .contractSurface(visited)));
                } catch (IllegalArgumentException
                        | IllegalStateException unresolved) {
                    counts[1]++;
                }
                return visited;
            }
        }.visit(unit, context);

        synchronized (inventory) {
            if (inventory.files.putIfAbsent(
                            path,
                            new M3AlgorithmAtomRecipeCratePlanner.FileInput(
                                    path,
                                    fileRoot,
                                    counts[0],
                                    counts[1],
                                    counts[2]))
                    != null) {
                throw new IllegalStateException(
                        "duplicate Java source path: " + path);
            }
            inventory.atoms.addAll(fileAtoms);
        }
    }

    private void scanPlainText(Inventory inventory, PlainText text) {
        String path =
                text.getSourcePath()
                        .normalize()
                        .toString()
                        .replace('\\', '/');
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        synchronized (inventory) {
            if (lower.equals("m3-recipe-catalogue.tsv")
                    || lower.endsWith("/m3-recipe-catalogue.tsv")) {
                if (inventory.recipeCatalogueTsv != null) {
                    throw new IllegalStateException(
                            "duplicate recipe catalogue");
                }
                inventory.recipeCatalogueTsv = text.getText();
            } else if (lower.equals("problem-catalogue.tsv")
                    || lower.endsWith("/problem-catalogue.tsv")) {
                if (inventory.problemCatalogueTsv != null) {
                    throw new IllegalStateException(
                            "duplicate problem catalogue");
                }
                inventory.problemCatalogueTsv = text.getText();
            } else if (!manifestOutputPath.isEmpty()
                    && path.equals(manifestOutputPath)) {
                if (inventory.existingManifest != null) {
                    throw new IllegalStateException(
                            "duplicate manifest output path");
                }
                inventory.existingManifest = text.getText();
            }
        }
    }

    private static String normalizeOptionalPath(String value) {
        String path = value.replace('\\', '/').strip();
        if (path.isEmpty()) {
            return "";
        }
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        if (path.startsWith("/")
                || path.equals("..")
                || path.startsWith("../")
                || path.endsWith("/..")
                || path.contains("/../")
                || path.indexOf('\0') >= 0
                || path.indexOf('\t') >= 0
                || path.indexOf('\n') >= 0
                || path.indexOf('\r') >= 0
                || !lower.endsWith(".tsv")
                || lower.equals("problem-catalogue.tsv")
                || lower.endsWith("/problem-catalogue.tsv")
                || lower.equals("m3-recipe-catalogue.tsv")
                || lower.endsWith("/m3-recipe-catalogue.tsv")) {
            throw new IllegalArgumentException("manifestOutputPath");
        }
        return path;
    }

    public static final class BindingTable extends DataTable<BindingRow> {
        BindingTable(org.openrewrite.Recipe recipe) {
            super(
                    recipe,
                    "M3 algorithm atom recipe-crate bindings",
                    "Exact method preimages joined to challenge evidence, pinned donors, "
                            + "recipe crates and Java/JNI lanes.");
        }
    }

    public record BindingRow(
            @Column(
                            displayName = "Serial ordinal",
                            description = "Canonical serial candidate order.")
                    int serialOrdinal,
            @Column(
                            displayName = "File ordinal",
                            description = "Normalized file order.")
                    int fileOrdinal,
            @Column(
                            displayName = "Atom ordinal",
                            description = "Method atom order within the file.")
                    int atomOrdinal,
            @Column(
                            displayName = "Candidate ordinal",
                            description = "Ranked shape candidate order for the atom.")
                    int candidateOrdinal,
            @Column(
                            displayName = "Source path",
                            description = "Repository-relative Java source path.")
                    String sourcePath,
            @Column(
                            displayName = "File preimage",
                            description = "SHA-256 of the complete Java source preimage.")
                    String fileSourceSha256,
            @Column(
                            displayName = "Method key",
                            description = "Owner, method and JVM descriptor.")
                    String methodKey,
            @Column(
                            displayName = "Method preimage",
                            description = "SHA-256 of the exact method source.")
                    String methodSourceSha256,
            @Column(
                            displayName = "Contract surface",
                            description = "PUBLIC_API, PROTECTED_API or internal/package surface.")
                    String contractSurface,
            @Column(
                            displayName = "Shape",
                            description = "Canonical AlgorithmShape candidate.")
                    String shape,
            @Column(
                            displayName = "Score",
                            description = "Bounded classifier score.")
                    int score,
            @Column(
                            displayName = "Problem category",
                            description = "Cross-site metadata category.")
                    String problemCategory,
            @Column(
                            displayName = "Balanced evidence root",
                            description = "Balanced LeetCode/HackerRank/GFG evidence root.")
                    String balancedEvidenceRoot,
            @Column(
                            displayName = "Competitive review root",
                            description = "Fixed three-site problem review root.")
                    String competitiveReviewRoot,
            @Column(
                            displayName = "Donor evidence root",
                            description = "Pinned Git donor evidence root.")
                    String donorEvidenceRoot,
            @Column(
                            displayName = "Crate root",
                            description = "Content-addressed LLM Maven/OpenRewrite recipe crate root.")
                    String crateRoot,
            @Column(
                            displayName = "Recipe disposition",
                            description = "Reuse/compose or create/improve recipe.")
                    String recipeDisposition,
            @Column(
                            displayName = "Recipe ids",
                            description = "Admitted recipe classes.")
                    String recipeIds,
            @Column(
                            displayName = "Execution lane",
                            description = "Java/JNI execution lane.")
                    String executionLane,
            @Column(
                            displayName = "Action",
                            description = "Next serial recipe-first action.")
                    String action,
            @Column(
                            displayName = "Lane gates",
                            description = "Additional Java/JNI proof gates.")
                    String laneGates,
            @Column(
                            displayName = "Binding root",
                            description = "SHA-256 identity of the complete binding.")
                    String bindingRoot,
            @Column(
                            displayName = "Authority",
                            description = "Always CANDIDATE_ONLY.")
                    String authority) {
        static BindingRow from(
                M3AlgorithmAtomRecipeCrateBinding binding) {
            return new BindingRow(
                    binding.serialOrdinal(),
                    binding.fileOrdinal(),
                    binding.atomOrdinal(),
                    binding.candidateOrdinal(),
                    binding.atom().sourcePath(),
                    binding.atom().fileSourceSha256(),
                    binding.atom().methodKey(),
                    binding.atom().methodSourceSha256(),
                    binding.atom().contractSurface(),
                    binding.shape().name(),
                    binding.score(),
                    binding.problemCategory().name(),
                    binding.balancedEvidence().root(),
                    binding.competitiveEvidence().review().root(),
                    binding.donorEvidenceRoot(),
                    binding.crateRoot(),
                    binding.crate().disposition().name(),
                    String.join(",", binding.crate().recipeIds()),
                    binding.executionLane().name(),
                    binding.action().name(),
                    String.join(",", binding.laneGates()),
                    binding.root(),
                    "CANDIDATE_ONLY");
        }
    }

    public static final class FileTable extends DataTable<FileRow> {
        FileTable(org.openrewrite.Recipe recipe) {
            super(
                    recipe,
                    "M3 algorithm atom file coverage",
                    "Exact per-file method coverage before atom-level "
                            + "recipe-crate planning.");
        }
    }

    public record FileRow(
            @Column(
                            displayName = "File ordinal",
                            description = "Normalized file order.")
                    int fileOrdinal,
            @Column(
                            displayName = "Source path",
                            description = "Repository-relative Java source path.")
                    String sourcePath,
            @Column(
                            displayName = "File preimage",
                            description = "SHA-256 source preimage.")
                    String fileSourceSha256,
            @Column(
                            displayName = "Method bodies",
                            description = "All Java methods with bodies.")
                    int methodBodies,
            @Column(
                            displayName = "Classified methods",
                            description = "Methods with at least one algorithm-shape candidate.")
                    int classifiedMethods,
            @Column(
                            displayName = "Candidate bindings",
                            description = "Ranked atom-to-crate bindings.")
                    int candidateBindings,
            @Column(
                            displayName = "Unresolved method bodies",
                            description = "Bodies held because declaration attribution is incomplete.")
                    int unresolvedMethodBodies,
            @Column(
                            displayName = "Native declarations",
                            description = "Native declarations requiring JNI contract inventory.")
                    int nativeDeclarations,
            @Column(
                            displayName = "Coverage root",
                            description = "SHA-256 identity of file coverage.")
                    String coverageRoot) {
        static FileRow from(
                M3AlgorithmAtomRecipeCratePlanner.FileCoverage file) {
            return new FileRow(
                    file.fileOrdinal(),
                    file.sourcePath(),
                    file.fileSourceSha256(),
                    file.methodBodies(),
                    file.classifiedMethods(),
                    file.candidateBindings(),
                    file.unresolvedMethods(),
                    file.nativeDeclarations(),
                    file.root());
        }
    }
}
