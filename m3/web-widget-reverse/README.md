<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# Web widgets to intrinsic SWT/Nebula: reverse scaffolds

This first pass scaffolds all 45 families present in the pinned Nebula widgets directory. WEB_COMPONENT_SOURCES.tsv preserves every Java source entry in the eight explicitly listed GWT/RWT packages, including support classes, interfaces and package metadata. Entries are source inventories, not claims that every entry is a concrete widget. Name hints are unproved candidates; UNMAPPED is intentional.

Each family has one properties scaffold. Keep viewport, ScrollBar/input listeners and disposal inside the existing widget. Keep primitive/paged data, sorting, searching, residency and swap on the CPU. Present only effectively visible viewport damage; an empty intersection paints nothing. A logical row, cell or character must not create a Control.

The first reverse donor extraction is SWT_CONSTRUCTION_SEEDS.tsv. It extracts actual parent/style construction expressions from the three preserved WindowBuilder descriptors. This proves metadata extraction only. It does not implement a UiBinder frontend, a Java emitter, an arbitrary HTML/JavaScript/TypeScript compiler, event equivalence, Unicode shaping or a native renderer.

Canonical recipe: com.synexia.rewrite.M3NebulaWebWidgetReverseScaffold20261015 in hsoliwal/com.synexia. Apply it to a Nebula source set to generate these absent metadata owners; drift and duplicate ownership must refuse, and replay must reach a fixed point. It creates no runtime wrapper classes and changes no product APIs.

Next passes bind exact SWT/Nebula receiver sources, classify donor declarations and effects, qualify literal text versus HTML/mnemonics, constructor/style and parent ownership, event ordering/removal/reentrancy, theme, localization/Unicode and accessibility. RWT session/lifecycle and GWT composition remain separate. Compiler, native screen, resource, performance, platform and source-aware review gates remain open.

## Cross-framework semantic mapping candidate (2026-10-25; NOT IMPLEMENTED)

The additive, review-only proposal is defined by:
- `FRAMEWORK_DONORS_20261025.tsv` — source-pinned forward compiler / server widget / template donors and legal reuse route.
- `WEB_SEMANTIC_CROSSWALK_20261025.tsv` — conditional web semantics to original SWT/Nebula receiver ownership; all rows are unqualified candidates.
- `WEB_MAPPING_CONTRACT_20261025.md` — typed AST admission, original viewport and protocol invariants, refusal and verification sequence.

These files **do not** amend `WEB_COMPONENT_SOURCES.tsv`, `IMPLEMENTATION_BINDINGS.tsv` or `docs/m3/rwt/RWT_COLUMN.tsv`, and do not claim another widget is implemented. A canonical Synexia Maven/OpenRewrite recipe must be admitted and executed before a product receiver is promoted. Never infer web↔native event equivalence from matching widget names.
