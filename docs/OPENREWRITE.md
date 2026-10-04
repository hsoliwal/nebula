# OpenRewrite atom and pattern recipes

The root POM and `releng/org.eclipse.nebula.nebula-parent/pom.xml` declare the inherited OpenRewrite Maven plugin. `rewrite.yml` composes the existing `M3DonorMavenizedAtomPatternRecipe` and `M3HierarchicalAtomPatternRecipe`. The recipe implementation is built from the checked-in `.rewrite/recipes` sources.

With JDK21 and Maven, build and install that recipe module first:

```sh
mvn -B -ntp -f .rewrite/recipes/pom.xml clean install
```

Run the configured recipes from the repository root:

```sh
mvn -B -ntp rewrite:dryRunNoFork
mvn -B -ntp rewrite:runNoFork
```

`runPerSubmodule` applies the configuration to each Maven module. `synexia.rewrite.module` supplies that module's artifact ID to the recipe ledger. The recipe supplier and generated `target` directories are excluded from the repository scan. CSV atom, hierarchy, source-visibility, documentation and framework-pattern tables are written below the relevant `target/rewrite/datatables` directories.

These two existing default compositions observe source and export tables; they do not rewrite source or create Maven POMs. Java declaration/algorithm mapping is Java-only. The hierarchy and documentation inventories can observe other supported source types; an unsupported Java mapping remains explicitly unsupported. Native Tycho compilation and UI tests retain their original build configuration and run separately with `mvn verify`. Recipe execution does not prove native build success.

The supplier is an explicitly named candidate containing the source-bound bootstrap repairs disclosed in its provenance and effect audit. Its modern Maven compilation, contract tests, plugin discovery and CSV coverage must be recorded before treating a run as proved. There is no dependency on a developer drive path or unpublished remote Maven package.

The Maven integration follows the [OpenRewrite Maven plugin configuration](https://docs.openrewrite.org/reference/rewrite-maven-plugin) and [recipe development/distribution guidance](https://docs.openrewrite.org/authoring-recipes/recipe-development-environment).
