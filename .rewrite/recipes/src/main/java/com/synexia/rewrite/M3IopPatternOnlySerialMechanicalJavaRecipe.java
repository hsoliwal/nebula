// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Canonical M3Scale mutation recipe for Synexia IOP pattern code.
 *
 * <p>This is the only named class-backed M3 recipe intended for mutation authority. It is
 * file-local, pattern-class-hooked, single-cycle per pass, and candidate-only. M3 proof and serial
 * promotion remain outside OpenRewrite.</p>
 */
public final class M3IopPatternOnlySerialMechanicalJavaRecipe extends Recipe {
    @Option(
            displayName = "Source file pattern",
            description = "Additional file glob; IOP source + pattern class-hook admission remains mandatory.",
            required = false,
            example = "synexia-iop/src/main/java/com/synexia/iop/patterns/**/*.java")
    private final String sourceFilePattern;
    private transient M3IopAuthorityTable authorityTable = new M3IopAuthorityTable(this);

    public M3IopPatternOnlySerialMechanicalJavaRecipe() {
        this(M3MechanicalJavaRecipe.DEFAULT_SOURCE_FILE_PATTERN);
    }

    @JsonCreator
    public M3IopPatternOnlySerialMechanicalJavaRecipe(String sourceFilePattern) {
        if (sourceFilePattern == null
                || sourceFilePattern.isBlank()
                || sourceFilePattern.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        this.sourceFilePattern = sourceFilePattern.strip();
    }

    @Override
    public String getDisplayName() {
        return "M3 IOP pattern-only serial mechanical Java pipeline";
    }

    @Override
    public String getDescription() {
        return "Runs the canonical class-backed M3 OpenRewrite sequence only on explicit Synexia "
                + "IOP pattern owners/participants. Output is a candidate until M3 "
                + "diff/lint/compile/test/runtime proof and serial promotion.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "iop",
                "pattern",
                "pattern-only",
                "class-hooked",
                "mechanical",
                "serial",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> evidence = new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit,
                    ExecutionContext context) {
                J.CompilationUnit visited = super.visitCompilationUnit(compilationUnit, context);
                if (!M3IopSourceFence.admits(visited.getSourcePath())) {
                    return visited;
                }

                M3IopFullScaleMutationAuthority.AuthorityReceipt receipt =
                        M3IopFullScaleMutationAuthority.receipt(visited, sourceFilePattern);
                String passIds = String.join("\u001f", receipt.passIds());
                String stages = receipt.stages().stream()
                        .map(Enum::name)
                        .collect(java.util.stream.Collectors.joining("\u001f"));

                if (receipt.classes().isEmpty()) {
                    authorityTable.insertRow(
                            context,
                            new M3IopAuthorityTable.Row(
                                    receipt.sourcePath(),
                                    receipt.sourceFilePattern(),
                                    "",
                                    false,
                                    "",
                                    "",
                                    "",
                                    false,
                                    passIds,
                                    stages,
                                    receipt.reason(),
                                    receipt.root(),
                                    "CANDIDATE_EVIDENCE_ONLY"));
                    return visited;
                }

                for (M3IopFullScaleMutationAuthority.ClassEvidence classEvidence : receipt.classes()) {
                    authorityTable.insertRow(
                            context,
                            new M3IopAuthorityTable.Row(
                                    receipt.sourcePath(),
                                    receipt.sourceFilePattern(),
                                    classEvidence.className(),
                                    classEvidence.admitted(),
                                    String.join("\u001f", classEvidence.hookIds()),
                                    classEvidence.catalogReceiptRoot(),
                                    classEvidence.root(),
                                    receipt.admitted(),
                                    passIds,
                                    stages,
                                    receipt.reason(),
                                    receipt.root(),
                                    "CANDIDATE_EVIDENCE_ONLY"));
                }
                return visited;
            }
        };

        return Preconditions.check(
                new FindSourceFiles(sourceFilePattern).getVisitor(),
                evidence);
    }

    @Override
    public List<Recipe> getRecipeList() {
        return M3IopFullScaleMutationAuthority.canonicalPasses(sourceFilePattern);
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }

    /**
     * Receipt binding this named recipe to the exact canonical IOP class hooks and FILE-to-UNIVERSE
     * M3 proof path. The receipt grants candidate generation only.
     */
    public M3IopRecipeMutationFence.Receipt mutationAuthorityReceipt() {
        return M3IopRecipeMutationFence.canonicalPipeline(this, sourceFilePattern);
    }
}
