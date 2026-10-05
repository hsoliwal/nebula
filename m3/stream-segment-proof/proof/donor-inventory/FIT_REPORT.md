# Nebula typed stream segment naming and bounded direct lowering

The admitted execution owner remains Nebula's `m3/recipe-first` module on OpenRewrite **8.90.4**, with its existing qualified line-terminator overlay. The missing delta is a typed stream-chain inventory/name leaf and an exact-source transformation for one reviewed file. No new parser, recipe DAG, generic stream optimizer or Patternizer is needed.

`FOSS_REUSE_DECISION.tsv` contains thirteen capability rows sealed before admission of their respective gaps. `INVENTORY.json` binds twelve Synexia owners and ten pinned native donor source/test surfaces. `NEBULA_INVENTORY.json` binds thirteen Nebula owners, three existing L2M sources and six public JDK semantic reference sources. Existing helper interfaces and exact source hashes are retained under the donor folders. This inventory lane makes no production changes.

## Existing owners and dependency boundaries

- Nebula master is pinned to `f4ebfe7edcc1f953d4dd76312f3a7bc6fbce645b`.
- `NebulaM3InventoryRecipe/Table` already owns file-local read-only structural signals. Add a stream-specific leaf beside this owner rather than another repository scanner.
- `NebulaM3PureIntAtomEligibility` shares discovery and mutation predicates, while `NebulaM3AtomizePureIntReturnRecipe` demonstrates the existing context-sensitive `JavaTemplate` edit lane. Their primitive-only matcher is NO_FIT as a direct stream matcher.
- `NebulaM3ExactJavaSnapshotRecipe` already enforces matching source path and before/after SHA, reparses the reviewed postimage as Java, preserves source metadata and reaches a postimage fixed point. Its SHA is `e11813b817e43b1745b490e8a49099931302519ddec486bcd4156644e1bbf3bd`. Use this exact-source owner for the bounded RoundedToolbar transformation.
- `NebulaM3VerbatimSourceSeal`, `NebulaM3FileConvergenceRecipeDag` and `NebulaM3RecipeDagManifest` already supply source custody and serial orchestration. Keep them as owners. No extra DAG is admitted.
- The baseline `NebulaM3DeclarativeConvergenceControlRecipe` chained `parsed.withId(...).withSourcePath(...)` fails javac generic type inference. The existing snapshot donor uses a typed `SourceFile replacement` intermediate. The ledger separately admits this minimal interface-compilation repair, preserving snapshot semantics and SDK version.
- Nebula has OpenRewrite **8.90.4** and the qualified coordinate `com.synexia.recipe:openrewrite-line-terminators:8.90.4-m3-f3e4bfe19654`. Earlier Synexia evidence used **8.17.1**. Do not mix those runtime jars in the same classloader or infer compatibility from source resemblance.
- After the minimal compile repair, the root baseline reached a JUnit startup failure: `NamespacedHierarchicalStore.CloseAction.closeAutoCloseables` was missing with the old5.10.1 engine and the pinned rewrite-test8.90.4 transitive5.14.4 API. The runtime-alignment ledger row admits only matching the existing JUnit property to the SDK donor's5.14.4 closure. This baseline executedzero tests; do not count it as a pass. Exact baseline log hashes are retained in `RUNTIME_ALIGNMENT_BASELINE.json`; fresh owning-module execution remains required.

Pending [Nebula PR49](https://github.com/hsoliwal/nebula/pull/49), head `3310de21b63cff8e329d3a785fb7a34d36087b73`, binds upstream `M3SecondPassAtomPatternRecipe@9d33ce5229bee70872110b7a03e4ee99bf0e0878` and inserts a read-only signal stage. The exact PR scope/body is retained in `NEBULA_PR49.json`. It does not implement typed stream-segment names or extraction, and its own report leaves hosted execution unverified. Preserve this binding; names and read-only signals grant no promotion authority.

Existing [Synexia L2M PR8913](https://github.com/hsoliwal/com.synexia/pull/8913), head `c921c716219794e62032c882df929fb2d6679903`, supplies the owned nonescaping field-Function inverse with 47 focused tests and 32 named refusal cases. Its three exact sources are retained. It fits only that specific facade shape. A deferred `Stream.filter` predicate is not an immediate owned field facade, and porting the existing source into Nebula8.90.4 still needs compilation/runtime proof. This owner must not become a second generic lambda inverse.

## Concrete RoundedToolbar fit

The reviewed source path is:

`widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar/src/org/eclipse/nebula/widgets/opal/roundedtoolbar/RoundedToolbar.java`

Exact preimage SHA: `fd00029ccb595dfedd0ec1c0604856359e065c9d2ca3fcbbe86382b15bbc0c59`.

The file owns a private final `List<RoundedToolItem> items = new ArrayList<>()`. MouseDown, MouseUp and MouseHover each contain the same pipeline:

`items.stream().filter(element -> element.getBounds().contains(event.x, event.y) && element.isEnabled()).findFirst()`

These three instances can share one private ordinary helper taking the original `Event` reference. A descriptive candidate name such as `findFirstEnabledItemContainingEvent` should be derived from the typed terminal and bound predicate callees. It is an index label only. Grouping must bind the original ordered expression and receiver identity; lexical method spelling alone cannot establish equivalence. The distinct fourth Point-based bounds pipeline remains outside this source-bound delta.

The helper should retain `items.spliterator().tryAdvance(...)` with a named Consumer state object, so traversal follows the same ArrayList callback boundary as the original short-circuit pipeline. An ordinary enhanced-for loop may miss the original concurrent-modification check when a callback mutates the list and then immediately selects an item. Do not claim that replacing stream allocation with a named Consumer makes the helper allocation-free.

The helper must keep the Event object, rather than snapshotting `event.x/event.y` into primitive arguments: item callbacks can modify the event between iterations, while the original reads its fields during each predicate evaluation. Preserve `getBounds -> contains -> isEnabled` ordering and short circuit. A true match stops further predicates; empty/all-miss results remain empty. Callback exceptions and callback-induced list modification must be tested at both matching and nonmatching elements.

## Typed extraction and naming constraints

Reuse OpenRewrite `J.MethodInvocation` select links, attributed `JavaType.Method` declaring owner/parameter/result types and exact receiver types. `M3CanonicalSemanticIdentity`/`M3SemanticAttribution` and `M3AttributedAtomizer` provide existing typed identity/slice-custody mechanisms; `M3Hash` and `AtomFingerprints` provide framed deterministic source/logic identities. Their source APIs are package-scoped or have nontrivial closures, so a cross-project direct import is not assumed.

Recognize only actual Stream/primitive-stream method contracts. Do not classify a widget `Display.map(...)`, user `filter(...)` method, string/comment, nested lambda's unrelated pipeline or unsupported receiver as a stream segment. Preserve ordered source/intermediate/terminal operations, callback identity, generic/primitive transitions and the original lexical owner. Unknown/raw/generic attribution should produce residue instead of a fabricated resolved name. The implemented leaf bounds segment count/depth, emits one finite private helper hint, and refuses declared or inherited collisions. It does not introduce content identity or a collision suffix: that donor capability is inventoried as NO_FIT for this hint-only implementation. Ordered, typed source binding and execution receipts remain separate from the name.

The complete Apache-2.0 native static-analysis donor snapshot `5dc62ae0e5889c62d86f6943cad34941af10e37d` is verified at all 306 archive entries. Its `SortedSetStreamToLinkedHashSet` demonstrates exact MethodMatcher chaining; `ReplaceStreamToListWithCollect` demonstrates version/API-scoped stream templates. Neither assigns typed composed operation names or performs this ArrayList find-first direct lowering. Lambda normalizers retain callable objects. Native recipes may be composed after separate fit proof; they are NO_FIT as a replacement naming/lowering engine.

## JDK source and license custody

The public FOSS semantic donor is [OpenJDK21u](https://github.com/openjdk/jdk21u/tree/98dbb49926d8e911b0713cefd9a2fc40955fa7f7), tag `jdk-21.0.9+7`, commit `98dbb49926d8e911b0713cefd9a2fc40955fa7f7`, GPL-2.0-only with Classpath exception. ArrayList source SHA is `c7db659273f9130e08b0d4508c34ff655e157b9ed48cdc7f5f5a2146ebb1c137`. Full public source and license texts are retained in `jdk21-donors/` as reference evidence. Application code calls existing JDK APIs; it does not copy or port JDK implementation.

All six public source bodies from their package declaration onward are byte-identical to the installed Oracle21.0.9+7 runtime source references. Oracle's installed source headers are proprietary/NFTC; those local reference copies are outside the shippable donor inventory and must not be published. The initial inferred GPL label for the installed Oracle archive was corrected before publication; `NEBULA_INVENTORY.json` distinguishes runtime identity from the public FOSS donor.

OpenJDK `ReferencePipeline.forEachWithCancel` uses `spliterator.tryAdvance` while checking cancellation. `ArrayList.ArrayListSpliterator.tryAdvance` calls the Consumer before checking `modCount`. `FindOps` requests cancellation after the first accepted item. These are concrete donor mechanisms supporting the required callback/CME discriminator; every new source transform still needs executed before/after proof.

Primary semantic references: [Java21 Stream API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html), [Java21 stream package](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html), [pinned OpenJDK ArrayList](https://github.com/openjdk/jdk21u/blob/98dbb49926d8e911b0713cefd9a2fc40955fa7f7/src/java.base/share/classes/java/util/ArrayList.java). Streams remain lazy, short-circuiting operations consume only needed elements, and callback side effects cannot generally be used as optimization evidence. The finite refusal policy here is a conservative inference from those contracts and the exact reviewed source.

No universal stream lowering, public API rename, zero-allocation, GC improvement, SWT device/UI behavior, Patternizer execution or canonical promotion is claimed by this inventory.
