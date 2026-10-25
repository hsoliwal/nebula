<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# Web widgets to intrinsic SWT/Nebula: reverse scaffolds

This first pass scaffolds all 45 families present in the pinned Nebula widgets directory. WEB_COMPONENT_SOURCES.tsv preserves every Java source entry in the eight explicitly listed GWT/RWT packages, including support classes, interfaces and package metadata. Entries are source inventories, not claims that every entry is a concrete widget. Name hints are unproved candidates; UNMAPPED is intentional.

Each family has one properties scaffold. Keep viewport, ScrollBar/input listeners and disposal inside the existing widget. Keep primitive/paged data, sorting, searching, residency and swap on the CPU. Present only effectively visible viewport damage; an empty intersection paints nothing. A logical row, cell or character must not create a Control.

The first reverse donor extraction is SWT_CONSTRUCTION_SEEDS.tsv. It extracts actual parent/style construction expressions from the three preserved WindowBuilder descriptors. This proves metadata extraction only. It does not implement a UiBinder frontend, a Java emitter, an arbitrary HTML/JavaScript/TypeScript compiler, event equivalence, Unicode shaping or a native renderer.

Canonical recipe: com.synexia.rewrite.M3NebulaWebWidgetReverseScaffold20261015 in hsoliwal/com.synexia. Apply it to a Nebula source set to generate these absent metadata owners; drift and duplicate ownership must refuse, and replay must reach a fixed point. It creates no runtime wrapper classes and changes no product APIs.

Next passes bind exact SWT/Nebula receiver sources, classify donor declarations and effects, qualify literal text versus HTML/mnemonics, constructor/style and parent ownership, event ordering/removal/reentrancy, theme, localization/Unicode and accessibility. RWT session/lifecycle and GWT composition remain separate. Compiler, native screen, resource, performance, platform and source-aware review gates remain open.

## 2026-10-25: native-first web/RWT interoperability

The additional framework and widget route decisions live in
`docs/m3/web-interoperability/` (`README.md`, `WIDGET_ROUTES.tsv`,
`FRAMEWORK_ROUTES.tsv`). These extend the original reverse scaffold
without treating GWT, J2CL, TeaVM, JWt, Wicket, Tapestry, MyFaces,
Apache Royale or Spring/Vaadin as arbitrary SWT transpilers.

Direct generation into existing SWT/Nebula controls remains preferred.
The opt-in `org.eclipse.nebula.widgets.m3.M3BrowserWidget` is only a
bounded dynamic-web fallback; no new recipe, full DOM compiler or
RWT runtime-equivalence claim is introduced.

The 2026-10-25 additive native-source receiver from Synexia #13402
is compiled into the existing Nebula M3 plugin as
`org.eclipse.nebula.widgets.m3.generated.OrdersNativeSwt` and is
tracked by Nebula #274. It constructs SWT Label/Button controls directly
from the admitted static JSX grammar, while preserving this existing
single-Canvas semantic-row receiver. This is **not** a broader
React/framework transpiler and has no runtime Synexia dependency.
