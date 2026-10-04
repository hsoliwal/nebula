// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Explicit boundary/task admission. Larger scopes do not arise from a hash or a recipe suggestion. */
public record SealedTask(String id, Scope scope, String boundary, Set<String> readPaths,
        Set<String> writePaths, SealedContract contract, List<SealedRecipe.Identity> recipes,
        String oracleRoot, String sourceDocSha256, String invariantId, int maxPasses, String root) {
    public enum Scope { FILE, PACKAGE, MODULE, PROJECT, AST_LEAF }
    public SealedTask {
        if (id == null || !id.matches("[A-Za-z0-9_.-]{1,160}")) throw new IllegalArgumentException("TASK_ID");
        Objects.requireNonNull(scope, "scope"); boundary = SealedSources.path(boundary);
        readPaths = paths(readPaths); writePaths = paths(writePaths);
        if (writePaths.isEmpty() || !readPaths.containsAll(writePaths)) throw new IllegalArgumentException("READ_WRITE_FENCE");
        if ((scope == Scope.FILE || scope == Scope.AST_LEAF)
                && (writePaths.size() != 1 || !writePaths.contains(boundary))) {
            throw new IllegalArgumentException("FILE_BOUNDARY");
        }
        for (String path : writePaths) {
            for (String component : path.split("/")) {
                if (Set.of(".git", ".m3", ".ai-bridge", ".merge-preserved", "target", "build", "node_modules", "generated-sources").contains(component)) {
                    throw new IllegalArgumentException("EXCLUDED_WRITE_PATH");
                }
            }
            if (!path.contains("/")) throw new IllegalArgumentException("ROOT_WRITE_FORBIDDEN");
            if (scope != Scope.FILE && scope != Scope.AST_LEAF
                    && !path.startsWith(boundary + "/")) throw new IllegalArgumentException("BOUNDARY_ESCAPE");
            if (path.contains("/src/test/") || path.endsWith("/pom.xml") || path.endsWith(".yml")) {
                throw new IllegalArgumentException("ORACLE_OR_BUILD_WRITE_REQUIRES_SEPARATE_PLAN");
            }
        }
        Objects.requireNonNull(contract, "contract"); recipes = List.copyOf(recipes);
        if (recipes.isEmpty() || recipes.size() > 64 || maxPasses < 1 || maxPasses > 128) {
            throw new IllegalArgumentException("PASS_BUDGET");
        }
        TreeSet<String> ids = new TreeSet<>();
        for (var recipe : recipes) if (!ids.add(recipe.id())) throw new IllegalArgumentException("DUPLICATE_RECIPE");
        SealHash.require(oracleRoot); SealHash.require(sourceDocSha256);
        if (invariantId == null || !invariantId.matches("[A-Za-z0-9_./:-]{1,160}")) throw new IllegalArgumentException("INVARIANT_ID");
        String expected = SealHash.frame("SEALED-TASK/1", id, scope.name(), boundary,
                String.join("\n", readPaths), String.join("\n", writePaths), contract.root(),
                recipes.stream().map(SealedRecipe.Identity::root).reduce("", (a,b) -> a+b), oracleRoot, sourceDocSha256, invariantId,
                Integer.toString(maxPasses));
        if (root == null || root.isEmpty()) root = expected;
        if (!root.equals(expected)) throw new IllegalArgumentException("TASK_ROOT_MISMATCH");
    }
    public SealedTask(String id, Scope scope, String boundary, Set<String> reads, Set<String> writes,
            SealedContract contract, List<SealedRecipe.Identity> recipes, String oracleRoot, String sourceDocSha256, String invariantId, int maxPasses) {
        this(id, scope, boundary, reads, writes, contract, recipes, oracleRoot, sourceDocSha256, invariantId, maxPasses, "");
    }
    private static Set<String> paths(Set<String> input) {
        TreeSet<String> sorted = new TreeSet<>(); input.forEach(path -> sorted.add(SealedSources.path(path)));
        return Collections.unmodifiableSet(sorted);
    }
}
