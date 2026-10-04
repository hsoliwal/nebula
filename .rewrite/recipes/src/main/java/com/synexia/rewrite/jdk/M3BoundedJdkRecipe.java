// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

/** Candidate-only, exact-path, method-body expression recipe with a per-file edit budget. */
public abstract class M3BoundedJdkRecipe extends Recipe {
    public static final String CHECKPOINT = "com.synexia.rewrite.jdk.checkpoint";
    private static final Runnable NOOP = () -> {};

    @Option(displayName = "Exact source path", required = false,
            description = "Repository-relative Java path, compared literally, not as a glob. Empty selects Java files.",
            example = "src/main/java/example/Worker.java")
    private final String sourcePath;

    @Option(displayName = "Maximum changes per file", required = false,
            description = "Expression replacements per file in one cycle. The default is one.", example = "1")
    private final int maxChangesPerFile;

    protected M3BoundedJdkRecipe(String sourcePath, Integer maxChangesPerFile) {
        this.sourcePath = normalizePath(sourcePath);
        this.maxChangesPerFile = maxChangesPerFile == null ? 1 : maxChangesPerFile;
        if (this.maxChangesPerFile < 1 || this.maxChangesPerFile > 1024) {
            throw new IllegalArgumentException("maxChangesPerFile must be between 1 and 1024");
        }
    }

    public static String normalizePath(String sourcePath) {
        if (sourcePath == null || sourcePath.isEmpty()) return "";
        String portable = sourcePath.replace('\\', '/');
        if (portable.length() > 4096 || portable.isBlank() || portable.indexOf('\0') >= 0 || portable.indexOf(':') >= 0
                || portable.startsWith("/") || portable.indexOf('*') >= 0 || portable.indexOf('?') >= 0) {
            throw new IllegalArgumentException("an exact repository-relative Java path is required");
        }
        Path path = Path.of(portable);
        for (Path part : path) {
            if (part.toString().equals("..")) throw new IllegalArgumentException("source path traversal");
        }
        String normalized = path.normalize().toString().replace('\\', '/');
        if (!normalized.endsWith(".java")) throw new IllegalArgumentException("Java source required");
        return normalized;
    }

    public final String getSourcePath() { return sourcePath; }
    public final int getMaxChangesPerFile() { return maxChangesPerFile; }
    @Override public final int maxCycles() { return 1; }
    @Override public Set<String> getTags() { return Set.of("synexia", "m3", "jdk", "candidate-only"); }
    @Override public final TreeVisitor<?, ExecutionContext> getVisitor() { return newVisitor(); }
    protected abstract Visitor newVisitor();

    /** OpenRewrite 8.17.1 discovers only concrete declared fields, not inherited @Option fields. */
    @Override protected RecipeDescriptor createRecipeDescriptor() {
        RecipeDescriptor descriptor = super.createRecipeDescriptor();
        List<OptionDescriptor> options = List.of(
                new OptionDescriptor("sourcePath", "String", "Exact source path",
                        "Repository-relative Java path, compared literally, not as a glob. Empty selects Java files.",
                        "src/main/java/example/Worker.java", null, false, sourcePath),
                new OptionDescriptor("maxChangesPerFile", "int", "Maximum changes per file",
                        "Expression replacements per file in one cycle. The default is one.",
                        "1", null, false, maxChangesPerFile));
        return new RecipeDescriptor(descriptor.getName(), descriptor.getDisplayName(), descriptor.getInstanceName(), descriptor.getDescription(),
                descriptor.getTags(), descriptor.getEstimatedEffortPerOccurrence(), options, descriptor.getRecipeList(),
                descriptor.getDataTables(), descriptor.getMaintainers(), descriptor.getContributors(),
                descriptor.getExamples(), descriptor.getSource());
    }

    @Override public final boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        M3BoundedJdkRecipe that = (M3BoundedJdkRecipe) other;
        return sourcePath.equals(that.sourcePath) && maxChangesPerFile == that.maxChangesPerFile;
    }

    @Override public final int hashCode() { return Objects.hash(getName(), sourcePath, maxChangesPerFile); }

    protected abstract class Visitor extends JavaVisitor<ExecutionContext> {
        private int changes;
        private final Map<String, JavaTemplate> templates = new HashMap<>();

        @Override public J visitCompilationUnit(J.CompilationUnit unit, ExecutionContext context) {
            checkpoint(context);
            changes = 0;
            String path = unit.getSourcePath().toString().replace('\\', '/');
            if (!path.endsWith(".java") || (!sourcePath.isEmpty() && !sourcePath.equals(path))) return unit;
            return super.visitCompilationUnit(unit, context);
        }

        protected final boolean admitted(Expression expression, ExecutionContext context) {
            checkpoint(context);
            return changes < maxChangesPerFile && insideMethodBody(getCursor());
        }

        protected final J replace(Expression before, String template, ExecutionContext context, Object... arguments) {
            checkpoint(context);
            // Walk comments only for a semantically admitted candidate, not every visited subtree.
            if (!commentFree(before)) return before;
            JavaTemplate parsed = templates.computeIfAbsent(template, text -> JavaTemplate.builder(text).build());
            J after = parsed.apply(updateCursor(before), before.getCoordinates().replace(), arguments);
            changes++;
            return after.withPrefix(before.getPrefix());
        }
    }

    private static boolean insideMethodBody(Cursor cursor) {
        for (Cursor current = cursor; current != null; current = current.getParent()) {
            if (current.getValue() instanceof J.Block
                    && current.getParentTreeCursor().getValue() instanceof J.MethodDeclaration) return true;
            // A local/anonymous class field or annotation must not borrow the outer method's body.
            // A genuine nested method was admitted at its own body before reaching these boundaries.
            if (current.getValue() instanceof J.MethodDeclaration
                    || current.getValue() instanceof J.ClassDeclaration
                    || current.getValue() instanceof J.NewClass) return false;
        }
        return false;
    }

    private static boolean commentFree(J tree) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaVisitor<AtomicBoolean>() {
            @Override public Space visitSpace(Space space, Space.Location location, AtomicBoolean comments) {
                if (!space.getComments().isEmpty()) comments.set(true);
                return super.visitSpace(space, location, comments);
            }
        }.visit(tree, found);
        return !found.get();
    }

    private static void checkpoint(ExecutionContext context) {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("recipe interrupted");
        Runnable checkpoint = context.getMessage(CHECKPOINT, NOOP);
        checkpoint.run();
    }
}
