# Stream segments to an ordinary search method

This branch tests the naming proposal on the actual Nebula `RoundedToolbar` source. Three searches have the same ordered segments:

```text
items.stream()
  -> filter(item.getBounds().contains(event.x, event.y) && item.isEnabled())
  -> findFirst()
  -> findFirstEnabledItemContainingEvent(event)
```

The read-only `NebulaM3StreamSegmentNames` leaf uses attributed receiver, argument, capture and predicate-order facts to suggest the name. A name never grants equivalence, replacement or promotion authority. Unsupported or unresolved shapes remain `UNKNOWN`; existing names cause a collision refusal. The fourth `getItem(Point)` search does not filter by enabled state and remains separate.

`NebulaM3RoundedToolbarStreamRecipe` uses the existing exact Java snapshot owner to replay only the pinned full-file preimage. It shares the three searches through one private ordinary method and an ordinary `Consumer.accept` method. The existing deferred SWT listeners remain deferred. No new parser, executor or DAG owner is added.

The candidate keeps the `Event` reference and reads its coordinates after `getBounds`, for every evaluated item. It uses the original ArrayList traversal boundary through `spliterator().tryAdvance`: an enhanced `for` loop can return a match before checking a predicate-induced modification. The finite compiled mutants expose both coordinate hoisting and this missing modification check. Public/protected outer-class signatures, mock effects, exception identity and short circuit are compared across isolated actual-source class loaders.

## Reproduce the focused proof

Use Java21 for the recipe/test build, and an installed Java17 `javac` for the historical JavaSE-17 widget source. The explicit test SDK is SWT3.126.0/JFace3.33.0; its jars, POMs, notices, class versions and hashes are pinned by the resolver/custody receipt. They are test inputs, not bundled production dependencies.

```text
python m3/stream-segment-proof/tools/resolve_sdk.py --output <sdk-cache-outside-git> --download
mvn -f m3/openrewrite-line-terminators/pom.xml -DskipTests install
mvn -f m3/stream-segment-proof/pom.xml \
  -Dm3.stream.classpath=<swt-jar><path-separator><jface-jar> \
  -Dm3.stream.javac=<jdk17>/bin/javac test
```

The owned parser overlay and new Java21 code retain `-Xlint:all -Werror`. The four complete widget source lanes also retain `-Xlint:all -Werror` with Java17. The combined suite passes 41 tests: four naming checks, five exact-recipe checks and 32 runtime checks. It executes 28 before/after cases, three detected mutants and 20 matching observable API signatures. The exact before/after resources, source bindings, named refusal inventory, raw command receipts and runtime traces are retained in the proof directory. Replay the lowering recipe once more to require a fixed point.

## Scope and unresolved checks

The candidate eliminates three repeated stream/filter/find-first scaffolds, but it still creates a callback object, a spliterator and optional result. No zero-allocation, GC, throughput or latency improvement is claimed.

The current SDK21 lane independently records four pre-existing constructor `this-escape` warnings in the original widget; the strict Java17-compatible lane is not a current-SDK21 strict-compilation pass. The inherited recipe reactor first failed to compile a fluent `Tree`/`SourceFile` inference and then failed to start a mixed JUnit engine. This branch repairs the typed intermediate and aligns the existing JUnit version with OpenRewrite 8.90.4's 5.14.4 dependency. The unrelated CDateTime stale snapshot and older refusal-exception tests remain four failures in the broader reactor; they are not hidden or weakened.

The proof runs mocked real callbacks without creating a display. SWT rendering, full Tycho verification, platform coverage and structured review are separate gates. The branch remains a draft candidate, and Patternizer work is deferred.
