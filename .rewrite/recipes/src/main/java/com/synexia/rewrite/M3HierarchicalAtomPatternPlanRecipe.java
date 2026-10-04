// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/**
 * Read-only M3 plan that expands every source into FILE -> PACKAGE -> MODULE -> PROJECT ->
 * REPOSITORY scope passes while retaining source specificity.
 */
public final class M3HierarchicalAtomPatternPlanRecipe
        extends ScanningRecipe<M3HierarchicalAtomPatternPlanRecipe.Accumulator> {

    private final transient ScopePlanTable table = new ScopePlanTable(this);

    @Override
    public String getDisplayName() {
        return "M3 hierarchical atom and pattern scope plan";
    }

    @Override
    public String getDescription() {
        return "Inventories every admitted text source, routes it by source specificity, and emits "
                + "the mandatory FILE to PACKAGE to MODULE to PROJECT to REPOSITORY promotion order. "
                + "This recipe is planning-only and grants no source mutation or promotion authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "recipe-first",
                "atom",
                "pattern",
                "file",
                "package",
                "module",
                "project",
                "repository",
                "read-only");
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
    public Accumulator getInitialValue(final ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(final Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(final Tree tree, final ExecutionContext ctx) {
                if (!(tree instanceof SourceFile source)) {
                    return tree;
                }
                stopAfterPreVisit();
                final String path = normalize(source.getSourcePath().toString());
                final M3SourceSpecificity specificity = M3SourceSpecificity.classify(path);
                final ScopeKeys keys = ScopeKeys.from(path, specificity);
                acc.rows.addAll(rows(path, specificity, keys));
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            final Accumulator acc, final ExecutionContext ctx) {
        acc.rows.stream()
                .sorted(Comparator.comparing(ScopeRow::sourcePath)
                        .thenComparing(row -> row.scope().ordinal()))
                .forEach(row -> table.insertRow(ctx, row));
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(final Accumulator acc) {
        return TreeVisitor.noop();
    }

    public boolean mutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private static List<ScopeRow> rows(
            final String sourcePath,
            final M3SourceSpecificity specificity,
            final ScopeKeys keys) {
        final ArrayList<ScopeRow> rows = new ArrayList<>(M3AtomPromotionScope.values().length);
        for (M3AtomPromotionScope scope : M3AtomPromotionScope.values()) {
            rows.add(new ScopeRow(
                    sourcePath,
                    specificity.name(),
                    specificity.parserLane(),
                    specificity.atomGranularity(),
                    specificity.code(),
                    specificity.documentation(),
                    specificity.patternizable(),
                    scope,
                    keys.key(scope),
                    scope == M3AtomPromotionScope.FILE ? "" : scope.previous().name(),
                    "INVENTORY_THEN_DRY_RUN_THEN_VERIFY",
                    false,
                    false));
        }
        return List.copyOf(rows);
    }

    private static String normalize(final String path) {
        final String value = Objects.requireNonNull(path, "path").replace('\\', '/');
        if (value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourcePath");
        }
        return value;
    }

    static final class Accumulator {
        private final List<ScopeRow> rows = new ArrayList<>();
    }

    private record ScopeKeys(
            String file,
            String packageKey,
            String module,
            String project,
            String repository) {

        static ScopeKeys from(
                final String path, final M3SourceSpecificity specificity) {
            final String file = path;
            final String parent = parent(path);
            final String module = module(path);
            final String packageKey = packageKey(path, parent, specificity);
            final String project = project(module);
            return new ScopeKeys(file, packageKey, module, project, ".");
        }

        String key(final M3AtomPromotionScope scope) {
            return switch (scope) {
                case FILE -> file;
                case PACKAGE -> packageKey;
                case MODULE -> module;
                case PROJECT -> project;
                case REPOSITORY -> repository;
            };
        }

        private static String packageKey(
                final String path,
                final String parent,
                final M3SourceSpecificity specificity) {
            if (specificity == M3SourceSpecificity.JAVA) {
                final String[] roots = {
                    "/src/main/java/", "/src/test/java/", "/src/it/java/", "/src/sdk/java/"
                };
                for (String root : roots) {
                    final int index = path.indexOf(root);
                    if (index >= 0) {
                        final int start = index + root.length();
                        final int end = path.lastIndexOf('/');
                        if (end <= start) {
                            return "<default>";
                        }
                        final String relative = path.substring(start, end);
                        return relative.isBlank() ? "<default>" : relative.replace('/', '.');
                    }
                }
            }
            return parent.isBlank() ? "." : parent;
        }

        private static String module(final String path) {
            final int source = path.indexOf("/src/");
            if (source > 0) {
                return path.substring(0, source);
            }
            final int slash = path.indexOf('/');
            return slash < 0 ? "." : path.substring(0, slash);
        }

        private static String project(final String module) {
            if (".".equals(module)) {
                return ".";
            }
            final int slash = module.indexOf('/');
            return slash < 0 ? "." : module.substring(0, slash);
        }

        private static String parent(final String path) {
            final int slash = path.lastIndexOf('/');
            return slash < 0 ? "" : path.substring(0, slash);
        }
    }

    public static final class ScopePlanTable extends DataTable<ScopeRow> {
        ScopePlanTable(final org.openrewrite.Recipe recipe) {
            super(
                    recipe,
                    "M3 hierarchical atom-pattern scope plan",
                    "Ordered source-specific FILE/PACKAGE/MODULE/PROJECT/REPOSITORY passes.");
        }
    }

    public record ScopeRow(
            @Column(displayName = "Source path", description = "Repository-relative source path.")
                    String sourcePath,
            @Column(displayName = "Source specificity", description = "Source routing family.")
                    String sourceSpecificity,
            @Column(displayName = "Parser lane", description = "Parser/analysis lane.")
                    String parserLane,
            @Column(displayName = "Atom granularity", description = "Smallest intended atom kind.")
                    String atomGranularity,
            @Column(displayName = "Code", description = "Whether source is executable code.")
                    boolean code,
            @Column(displayName = "Documentation", description = "Whether source is documentation.")
                    boolean documentation,
            @Column(displayName = "Patternizable", description = "Whether pattern mining is admitted.")
                    boolean patternizable,
            @Column(displayName = "Scope", description = "Ordered promotion scope.")
                    M3AtomPromotionScope scope,
            @Column(displayName = "Scope key", description = "Deterministic owner at this scope.")
                    String scopeKey,
            @Column(displayName = "Requires scope", description = "Immediately prior proof scope.")
                    String requiresScope,
            @Column(displayName = "Disposition", description = "Required verification disposition.")
                    String disposition,
            @Column(displayName = "Mutation authority", description = "Always false in planning.")
                    boolean mutationAuthority,
            @Column(displayName = "Promotion authority", description = "Always false in planning.")
                    boolean promotionAuthority) {}
}
