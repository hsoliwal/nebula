// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.ClasspathScanningLoader;

/**
 * The externally published OpenRewrite static-analysis surface admitted to the M3 full-file
 * candidate lane.
 *
 * <p>The list is deliberately data, not copied recipe code.  The Maven plugin supplies the
 * recipe pack at runtime; this class only gives M3 a stable order, a fail-closed availability
 * check, and a one-file source fence.  A missing recipe is an admission error rather than a
 * silently skipped rule.</p>
 */
public final class M3StaticAnalysisRules {
    private static final List<String> RECIPE_NAMES = List.of(
            "org.openrewrite.staticanalysis.AtomicPrimitiveEqualsUsesGet",
            "org.openrewrite.staticanalysis.BigDecimalRoundingConstantsToEnums",
            "org.openrewrite.staticanalysis.BooleanChecksNotInverted",
            "org.openrewrite.staticanalysis.CaseInsensitiveComparisonsDoNotChangeCase",
            "org.openrewrite.staticanalysis.CompareEnumsWithEqualityOperator",
            "org.openrewrite.staticanalysis.EqualsAvoidsNull",
            "org.openrewrite.staticanalysis.SimplifyCompoundStatement",
            "org.openrewrite.staticanalysis.SimplifyConsecutiveAssignments",
            "org.openrewrite.staticanalysis.SimplifyConstantIfBranchExecution",
            "org.openrewrite.staticanalysis.SimplifyDurationCreationUnits",
            "org.openrewrite.staticanalysis.InstanceOfPatternMatch",
            "org.openrewrite.staticanalysis.IndexOfReplaceableByContains",
            "org.openrewrite.staticanalysis.IndexOfShouldNotCompareGreaterThanZero",
            "org.openrewrite.staticanalysis.IsEmptyCallOnCollections",
            "org.openrewrite.staticanalysis.LambdaBlockToExpression",
            "org.openrewrite.staticanalysis.ForLoopControlVariablePostfixOperators",
            "org.openrewrite.staticanalysis.ControlFlowIndentation",
            "org.openrewrite.staticanalysis.DefaultComesLast",
            "org.openrewrite.staticanalysis.EmptyBlock",
            "org.openrewrite.staticanalysis.ExplicitInitialization",
            "org.openrewrite.staticanalysis.ExplicitLambdaArgumentTypes",
            "org.openrewrite.staticanalysis.FallThrough",
            "org.openrewrite.staticanalysis.ModifierOrder",
            "org.openrewrite.staticanalysis.MultipleVariableDeclarations",
            "org.openrewrite.staticanalysis.RemoveEmptyJavaDocParameters",
            "org.openrewrite.staticanalysis.RemoveExtraSemicolons",
            "org.openrewrite.staticanalysis.RemoveJavaDocAuthorTag",
            "org.openrewrite.staticanalysis.TypecastParenPad",
            "org.openrewrite.staticanalysis.UnnecessaryExplicitTypeArguments",
            "org.openrewrite.staticanalysis.UnnecessaryPrimitiveAnnotations",
            "org.openrewrite.staticanalysis.UpperCaseLiteralSuffixes",
            "org.openrewrite.staticanalysis.UseJavaStyleArrayDeclarations",
            "org.openrewrite.staticanalysis.WriteOctalValuesAsDecimal",
            "org.openrewrite.staticanalysis.NoEmptyCollectionWithRawType",
            "org.openrewrite.staticanalysis.NoRedundantJumpStatements",
            "org.openrewrite.staticanalysis.UseForEachRemoveInsteadOfSetRemoveAll",
            "org.openrewrite.staticanalysis.UseLambdaForFunctionalInterface",
            "org.openrewrite.staticanalysis.UseListSort",
            "org.openrewrite.staticanalysis.UseMapContainsKey",
            "org.openrewrite.staticanalysis.UseStringReplace",
            "org.openrewrite.staticanalysis.NoToStringOnStringType",
            "org.openrewrite.staticanalysis.NoValueOfOnStringType",
            "org.openrewrite.staticanalysis.NeedBraces",
            "org.openrewrite.staticanalysis.SimplifyBooleanExpression",
            "org.openrewrite.staticanalysis.SimplifyBooleanReturn",
            "org.openrewrite.staticanalysis.OperatorWrap",
            "org.openrewrite.staticanalysis.UseDiamondOperator",
            "org.openrewrite.staticanalysis.UseCollectionInterfaces",
            "org.openrewrite.staticanalysis.ReplaceLambdaWithMethodReference",
            "org.openrewrite.staticanalysis.UnnecessaryParentheses",
            "org.openrewrite.staticanalysis.MissingOverrideAnnotation",
            "org.openrewrite.staticanalysis.RemoveUnusedLocalVariables",
            "org.openrewrite.staticanalysis.RemoveUnusedPrivateMethods",
            "org.openrewrite.staticanalysis.IndexOfChecksShouldUseAStartPosition",
            "org.openrewrite.staticanalysis.PrimitiveWrapperClassConstructorToValueOf",
            "org.openrewrite.staticanalysis.FinalizeLocalVariables");

    private M3StaticAnalysisRules() {}

    /** Stable, duplicate-free recipe IDs in execution order. */
    public static List<String> recipeNames() {
        return RECIPE_NAMES;
    }

    /**
     * Resolve the admitted pack and scope every leaf to one repository-relative source path.
     *
     * <p>The pack is loaded from the active Maven/OpenRewrite runtime classpath.  Its source is
     * never copied into Synexia.  Composite descriptors are flattened so each returned pass has
     * the same exact-file fence used by the programmatic transpiler.</p>
     */
    public static List<M3TranspilePass> passesForFile(String sourcePath) {
        return passesForFile(sourcePath, RECIPE_NAMES);
    }

    /**
     * Resolve only requested leaves from the admitted static-analysis catalogue.
     *
     * <p>Diagnostic bridges may narrow the pack, but arbitrary runtime recipe names cannot enter
     * through this method.</p>
     */
    public static List<M3TranspilePass> passesForFile(
            String sourcePath, Collection<String> requestedRecipeNames) {
        String pattern = M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath);
        return passes(pattern, false, requested(requestedRecipeNames));
    }

    /**
     * Resolve the full admitted static-analysis pack for the canonical IOP pattern-only lane.
     *
     * <p>Every external leaf is wrapped by both the requested source pattern and
     * {@link M3IopPatternClassHooks}. A matching IOP path without pattern-class admission is still
     * read-only.</p>
     */
    public static List<M3TranspilePass> passesForIopPattern(String sourceFilePattern) {
        if (sourceFilePattern == null
                || sourceFilePattern.isBlank()
                || sourceFilePattern.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern required");
        }
        return passes(sourceFilePattern.strip(), true, RECIPE_NAMES);
    }

    private static List<M3TranspilePass> passes(
            String pattern, boolean iopPatternOnly, List<String> recipeNames) {
        List<Recipe> leaves = loadRecipes(recipeNames, ClasspathScanningLoader.class.getClassLoader());
        List<M3TranspilePass> passes = new ArrayList<>(leaves.size());
        for (int index = 0; index < leaves.size(); index++) {
            Recipe recipe = leaves.get(index);
            String id = String.format(java.util.Locale.ROOT,
                    "07-static-analysis-%03d-%s", index + 1, simpleName(recipe.getName()));
            Recipe fenced = iopPatternOnly
                    ? new M3IopPatternScopedRecipe(pattern, recipe)
                    : new M3ScopedRecipe(pattern, recipe);
            passes.add(new M3TranspilePass(id, fenced));
        }
        return List.copyOf(passes);
    }

    /**
     * Discovery-boundary Guard Clause: inspect only an existing dedicated local pack root.
     * The runtime parent resolves dependencies, but its unrelated recipes/resources are not scanned.
     * This is discovery isolation, not a sandbox for trusted third-party recipe implementations.
     * Each invocation constructs fresh recipe instances; no recipe/environment cache is shared.
     */
    private static List<Recipe> loadRecipes(List<String> names, ClassLoader runtime) {
        List<String> recipeNames = requested(names);
        try (java.net.URLClassLoader discovery = packDiscoveryLoader(runtime)) {
            Environment environment = Environment.builder().scanClassLoader(discovery).build();
            Set<String> available = new HashSet<>();
            environment.listRecipeDescriptors().forEach(descriptor -> available.add(descriptor.getName()));
            List<String> missing = recipeNames.stream()
                    .filter(name -> !available.contains(name))
                    .toList();
            if (!missing.isEmpty()) {
                throw new IllegalStateException("M3 static-analysis recipe pack is incomplete: " + missing);
            }

            Recipe activated = environment.activateRecipes(recipeNames);
            List<Recipe> leaves = new ArrayList<>();
            flatten(activated, leaves);
            for (Recipe leaf : leaves) {
                ClassLoader owner = leaf.getClass().getClassLoader();
                boolean parentOwned = owner == null;
                for (ClassLoader parent = runtime; parent != null; parent = parent.getParent()) {
                    if (owner == parent) parentOwned = true;
                }
                if (!parentOwned) throw unsupported("recipe implementation is not runtime-owned: " + leaf.getName());
            }
            return List.copyOf(leaves);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("M3 static-analysis discovery loader close failed", e);
        }
    }

    private static final String PACK_PREFIX = "org/openrewrite/staticanalysis/";
    private static final String PACK_ANCHOR = PACK_PREFIX + "AtomicPrimitiveEqualsUsesGet.class";
    private static final int MAX_PACK_ENTRIES = 20_000;
    private static final int MAX_MANIFEST_BYTES = 65_536;

    private static java.net.URLClassLoader packDiscoveryLoader(ClassLoader runtime) {
        Objects.requireNonNull(runtime, "runtime");
        try {
            Class<?> anchor = Class.forName(RECIPE_NAMES.getFirst(), false, runtime).asSubclass(Recipe.class);
            java.security.CodeSource source = anchor.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null
                    || !"file".equals(source.getLocation().getProtocol())) {
                throw unsupported("pack must have an existing local file CodeSource");
            }
            java.nio.file.Path root = java.nio.file.Path.of(source.getLocation().toURI());
            requireDedicatedPack(root);
            return new java.net.URLClassLoader(new java.net.URL[]{root.toRealPath().toUri().toURL()}, runtime);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IllegalStateException("M3 static-analysis recipe pack is incomplete: " + RECIPE_NAMES.getFirst(), e);
        } catch (java.io.IOException | java.net.URISyntaxException e) {
            throw new IllegalStateException("M3 static-analysis pack location cannot be inspected", e);
        }
    }

    /**
     * Linear entry validation with a hash set rejects layout expansion before OpenRewrite scans.
     * Dedicated ordinary jars and exploded roots are supported. Co-located, nested, versioned,
     * symlinked or manifest-expanded packs are explicit residue, never a broad-scan fallback.
     */
    private static void requireDedicatedPack(java.nio.file.Path root) {
        try {
            if (java.nio.file.Files.isSymbolicLink(root)) throw unsupported("symlinked pack root");
            HashSet<String> entries = new HashSet<>();
            if (java.nio.file.Files.isDirectory(root, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                try (var paths = java.nio.file.Files.walk(root)) {
                    var iterator = paths.iterator();
                    while (iterator.hasNext()) {
                        java.nio.file.Path path = iterator.next();
                        if (path.equals(root)) continue;
                        if (java.nio.file.Files.isSymbolicLink(path)) throw unsupported("symlinked pack entry");
                        String name = root.relativize(path).toString().replace('\\', '/');
                        requirePackEntry(name, entries);
                        if (name.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                            try (var input = java.nio.file.Files.newInputStream(path)) { requirePackManifest(input); }
                        }
                    }
                }
            } else if (java.nio.file.Files.isRegularFile(root, java.nio.file.LinkOption.NOFOLLOW_LINKS)
                    && root.getFileName().toString().endsWith(".jar")) {
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(root.toFile())) {
                    var iterator = jar.entries();
                    while (iterator.hasMoreElements()) {
                        var entry = iterator.nextElement();
                        requirePackEntry(entry.getName(), entries);
                        if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                            try (var input = jar.getInputStream(entry)) { requirePackManifest(input); }
                        }
                    }
                }
            } else throw unsupported("pack root is not a dedicated jar or directory");
            if (!entries.contains(PACK_ANCHOR)) throw unsupported("pack anchor missing from discovery root");
        } catch (java.io.IOException | java.io.UncheckedIOException e) {
            throw new IllegalStateException("M3 static-analysis pack layout cannot be inspected", e);
        }
    }

    private static void requirePackEntry(String name, Set<String> entries) {
        if (!entries.add(name) || entries.size() > MAX_PACK_ENTRIES) throw unsupported("duplicate or excessive pack entries");
        if (name.startsWith("/") || name.indexOf('\\') >= 0
                || java.util.Arrays.asList(name.split("/", -1)).contains("..")) {
            throw unsupported("non-local pack entry");
        }
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".jar") || lower.endsWith(".zip") || lower.endsWith(".war")
                || lower.endsWith(".ear") || lower.endsWith(".jmod")) throw unsupported("nested pack archive");
        if (name.endsWith(".class") && !name.startsWith(PACK_PREFIX)) throw unsupported("co-located non-pack class: " + name);
    }

    private static void requirePackManifest(java.io.InputStream input) throws java.io.IOException {
        byte[] bytes = input.readNBytes(MAX_MANIFEST_BYTES + 1);
        if (bytes.length > MAX_MANIFEST_BYTES) throw unsupported("oversized pack manifest");
        var manifest = new java.util.jar.Manifest(new java.io.ByteArrayInputStream(bytes));
        var attributes = manifest.getMainAttributes();
        for (String name : List.of("Class-Path", "Bundle-ClassPath")) {
            String value = attributes.getValue(name);
            if (value != null && !value.isBlank()) throw unsupported("manifest classpath expansion: " + name);
        }
    }

    private static IllegalStateException unsupported(String detail) {
        return new IllegalStateException("M3 static-analysis unsupported pack layout: " + detail);
    }

    private static List<String> requested(
            Collection<String> requestedRecipeNames) {
        Objects.requireNonNull(requestedRecipeNames, "requestedRecipeNames");
        ArrayList<String> ordered = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (String name : requestedRecipeNames) {
            if (name == null || !RECIPE_NAMES.contains(name)) {
                throw new IllegalArgumentException(
                        "recipe is not admitted: " + name);
            }
            if (seen.add(name)) ordered.add(name);
        }
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one admitted recipe is required");
        }
        ordered.sort(
                java.util.Comparator.comparingInt(RECIPE_NAMES::indexOf));
        return List.copyOf(ordered);
    }

    /** Validate a previously captured inventory without touching the runtime classpath. */
    public static List<String> requireAvailable(Collection<String> availableRecipeNames) {
        Objects.requireNonNull(availableRecipeNames, "availableRecipeNames");
        List<String> missing = RECIPE_NAMES.stream()
                .filter(name -> !availableRecipeNames.contains(name))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("missing admitted recipes: " + missing);
        }
        return RECIPE_NAMES;
    }

    /** Stable TSV suitable for the M3 donor/evidence ledger. */
    public static String toTsv() {
        StringBuilder out = new StringBuilder("ordinal\trecipe_name\tsurface\tmode\n");
        for (int index = 0; index < RECIPE_NAMES.size(); index++) {
            out.append(index + 1).append('\t')
                    .append(RECIPE_NAMES.get(index)).append('\t')
                    .append("rewrite-static-analysis").append('\t')
                    .append("CANDIDATE_ONLY").append('\n');
        }
        return out.toString();
    }

    private static void flatten(Recipe recipe, List<Recipe> leaves) {
        List<Recipe> children = recipe.getRecipeList();
        if (children.isEmpty()) {
            leaves.add(recipe);
            return;
        }
        children.forEach(child -> flatten(child, leaves));
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }
}
