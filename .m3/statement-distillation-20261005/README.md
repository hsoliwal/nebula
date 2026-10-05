# Executed Nebula statement distillation

The canonical Synexia OpenRewrite recipes processed all 1,768 application Java files and all
72 owned Java tooling files: **1,840 files, 167,615 statement/block/control atoms**. The other
43 Java files are immutable donor and parser reference fixtures, catalogued separately.
`all-files.tsv` seals every delivered source and its original canonical composition packet.
`statement-atoms.tsv.gz` retains ordered occurrence, parent, pattern and source-range data;
the canonical task crate's `RestoreAtoms` reconstructs every original JSON packet byte for byte.
All 1,840 packets passed restoration. No executable body was replaced by an inventory row.

## Source custody and compatibility

The branch retains the original `c34a6fe3b6ca5c0377e60ab90a370fc3c6a0f80b` history,
merges master `a46dd1e7a12740ae9dc330e14752480175c18ee1`, and retains PR #60's
`7f01b6fde09ab4e9db36361912025fc0a85b88e7` primitive GC assertion recipe fix.
Existing Grid CSS modernization, viewport/style behavior and recipe gates survive.

`CssCompatRecipe` reuses `NebulaM3ExactJavaSnapshotRecipe` for the remaining three CSS handlers.
It replaces the removed internal Measure API with W3C CSSPrimitiveValue, retaining float access,
rounding and the existing CDateTime classifier semantics. It is exact-source sealed, reaches a
fixed point, and refuses drift for every target. The canonical generic Java snapshot recipe
refused widget paths; its whitelist was not widened. The owner itself was materialized through
the canonical absent-before snapshot recipe.

The line-terminator parser's original tests, byte counts and pinned hashes remain intact.
Their two original sources are preserved as immutable fixtures. `CurrentSourceTest` separately
checks lossless parsing of the delivered sources. The parser POM was changed through the
existing hash-pinned POM recipe, with replay and refusal verified.

## Executed gates

- Exact packet reconstruction, reparse and recipe fixed point for all processed Java sources.
- Independent javac body equivalence for all 1,768 application files, against master plus the
  separately sealed CSS/GC compatibility repairs. Independent atom census: 157,208; zero mismatches.
- Full **314-module Maven compile and native Linux GTK verify: BUILD SUCCESS**.
- Native reactor: **211 tests, zero failures, errors or skips** in its aggregate module summaries.
- Source-owned parser and recipe reactor: **86 tests, zero failures/errors**.
- Before/after executable class equivalence for 25 baseline-complete modules; the original Grid
  CSS build failure prevented a complete baseline class inventory. This is explicitly partial.
- All 497 catalogued image assets, including 344 PNGs, remain byte exact.

`native-verify.json` binds the test receipt to the SHA-256 of `all-files.tsv`. Selected summaries
are retained; browser/environment dumps and built native libraries are excluded.

## Reproduction and boundaries

The recipe and proof adapters are owned by `hsoliwal/com.synexia` under
`synexia-openrewrite-recipes/recipe-crates/swt-statement-distillation-20261005`.
Run the canonical `M3JavaStatementCompositionCli` on a sorted exact Java path list, with
`M3StatementBlockNormalizationRecipe` as the preceding recipe, writing to a new external output.
Normalize, reparse, atomize, restore, reparse, and replay are separate verified phases.
Restore with `RestoreAtoms <checkout> <statement-atoms.tsv.gz> <all-files.tsv>`.
Build the source-owned tooling with `mvn -f m3/reactor.xml verify`, then the SDK with `mvn verify`
using JDK 21 and a functioning native GTK display/toolchain.

This completes the sealed Java composition pass and these Linux reactors. Windows/macOS native
execution, complete native C semantic distillation, canonical strict repository promotion,
the full Synexia reactor and repository-owned offline dependency closure remain separate gates.
Finite corpus convergence does not establish arbitrary semantic equivalence or a speedup.

Original parser fixtures intentionally retain upstream whitespace and original line endings;
those exact test inputs can trigger `git diff --check`. New code and proof summaries are checked
separately; no fixture bytes, source hash or CI setting were changed to hide that distinction.
