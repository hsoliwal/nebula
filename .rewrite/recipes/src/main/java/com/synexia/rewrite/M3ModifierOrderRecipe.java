// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.openrewrite.java.tree.Space;

/**
 * Put Java modifiers into the JLS-recommended order while retaining positional whitespace.
 *
 * <p>Declarations whose modifier prefixes or modifier-attached annotations contain comments are
 * deliberately left unchanged so a mechanical pass never re-associates a comment with another
 * modifier.</p>
 */
public final class M3ModifierOrderRecipe extends Recipe {
    @Option(
            displayName = "Source file pattern",
            description = "Glob relative to the project root. Use an exact path for one-file M3 execution.",
            required = false)
    private final String sourceFilePattern;

    public M3ModifierOrderRecipe() {
        this(M3MechanicalJavaRecipe.DEFAULT_SOURCE_FILE_PATTERN);
    }

    @JsonCreator
    public M3ModifierOrderRecipe(String sourceFilePattern) {
        this.sourceFilePattern = pattern(sourceFilePattern);
    }

    @Override
    public String getDisplayName() {
        return "M3 canonical Java modifier order";
    }

    @Override
    public String getDescription() {
        return "Orders class, method, and variable modifiers using OpenRewrite's JLS modifier order.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("synexia", "m3", "mechanical", "modifiers", "class-backed");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> visitor = new JavaIsoVisitor<>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration classDecl, ExecutionContext context) {
                J.ClassDeclaration visited = super.visitClassDeclaration(classDecl, context);
                return visited.withModifiers(canonical(visited.getModifiers()));
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                J.MethodDeclaration visited = super.visitMethodDeclaration(method, context);
                return visited.withModifiers(canonical(visited.getModifiers()));
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(
                    J.VariableDeclarations variables, ExecutionContext context) {
                J.VariableDeclarations visited = super.visitVariableDeclarations(variables, context);
                return visited.withModifiers(canonical(visited.getModifiers()));
            }
        };
        return Preconditions.check(new FindSourceFiles(sourceFilePattern).getVisitor(), visitor);
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }

    private static List<J.Modifier> canonical(List<J.Modifier> modifiers) {
        if (modifiers.size() < 2 || unsafeToMove(modifiers)) {
            return modifiers;
        }

        List<J.Modifier> sorted = new ArrayList<>(modifiers);
        sorted.sort(Comparator.comparingInt(modifier -> modifier.getType().ordinal()));
        boolean alreadyCanonical = true;
        for (int index = 0; index < modifiers.size(); index++) {
            if (modifiers.get(index).getType() != sorted.get(index).getType()) {
                alreadyCanonical = false;
                break;
            }
        }
        if (alreadyCanonical) {
            return modifiers;
        }

        List<J.Modifier> result = new ArrayList<>(sorted.size());
        for (int index = 0; index < sorted.size(); index++) {
            Space positionalPrefix = modifiers.get(index).getPrefix();
            result.add(sorted.get(index).withPrefix(positionalPrefix));
        }
        return List.copyOf(result);
    }

    private static boolean unsafeToMove(List<J.Modifier> modifiers) {
        return modifiers.stream().anyMatch(modifier ->
                !modifier.getPrefix().getComments().isEmpty()
                        || !modifier.getAnnotations().isEmpty());
    }

    private static String pattern(String value) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        return value.strip();
    }
}
