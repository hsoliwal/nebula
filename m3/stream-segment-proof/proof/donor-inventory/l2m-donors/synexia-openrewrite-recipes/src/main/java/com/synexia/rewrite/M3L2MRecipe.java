// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

/** L2M: unfold a proved closed lambda facade after first-pass functional normalization. */
public final class M3L2MRecipe extends Recipe {
    private final String sourceFilePattern;

    public M3L2MRecipe() { this("**/*.java"); }

    @JsonCreator
    public M3L2MRecipe(@JsonProperty("sourceFilePattern") String sourceFilePattern) {
        String pattern = Objects.toString(sourceFilePattern, "").strip();
        if (pattern.isEmpty() || pattern.length() > 4096 || pattern.indexOf('\0') >= 0
                || pattern.indexOf('\r') >= 0 || pattern.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern");
        }
        this.sourceFilePattern = pattern;
    }

    @Override public String getDisplayName() { return "M3 L2M closed lambda unfolding"; }
    @Override public String getDescription() {
        return "Restores ordinary method bodies from exclusively owned typed Function facades; "
                + "refuses escaping, conversion and initialization hazards. Candidate proof remains required.";
    }
    @Override public Set<String> getTags() { return Set.of("synexia", "m3", "l2m", "candidate-only"); }
    @Override public int maxCycles() { return 1; }
    @Override public boolean causesAnotherCycle() { return false; }
    public boolean promotionAuthority() { return false; }
    public String getSourceFilePattern() { return sourceFilePattern; }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(sourceFilePattern), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration declaration, ExecutionContext ctx) {
                J.ClassDeclaration owner = super.visitClassDeclaration(declaration, ctx);
                if (!M3L2MFacades.safeInitialization(owner)) return owner;
                J.CompilationUnit unit = getCursor().firstEnclosing(J.CompilationUnit.class);
                if (unit == null) return owner;
                ArrayList<Statement> members = new ArrayList<>(owner.getBody().getStatements());
                boolean changed = false;
                for (Statement statement : owner.getBody().getStatements()) {
                    if (!(statement instanceof J.VariableDeclarations field)) continue;
                    for (int i = 0; i < members.size(); i++) {
                        if (!(members.get(i) instanceof J.MethodDeclaration method)) continue;
                        J.MethodDeclaration replacement = M3L2MFacades.unfold(field, method, unit);
                        if (replacement == method) continue;
                        members.set(i, replacement);
                        members.remove(field);
                        changed = true;
                        break;
                    }
                }
                return changed ? maybeAutoFormat(owner,
                        owner.withBody(owner.getBody().withStatements(members)), ctx) : owner;
            }
        });
    }
}
