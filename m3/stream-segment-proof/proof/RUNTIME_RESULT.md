# Real-source RoundedToolbar runtime result

The final runtime-only run passes 32 tests with zero failures, errors or skips. The authoritative log is `runtime-agent-final-maven.log` (completed 2026-10-04 20:41:30 Asia/Calcutta). Root owns the separate combined recipe/naming execution and broader reactor result.

The actual full before/after RoundedToolbar source and three real source neighbors compile in four isolated lanes: before, after, enhanced-for mutant and early-coordinate mutant. The historic JavaSE-17 source lane uses installed Microsoft javac 17.0.19 with `--release 17 -proc:none -Xlint:all -Werror`. The new recipe and test sources retain the focused POM's Java21 `-Xlint:all -Werror` gate. Mockito substitutes external widget callees; the real private `addListeners` implementation registers the real SWT listener objects, which are then invoked through SWT's actual Listener interface. No copied predicate surrogate or rendered UI is the oracle.

The executed comparisons cover 16 MouseHover scenarios and six primary-button cases each for MouseDown and MouseUp: empty, first overlapping match, missed/disabled items before a match, getter-mutated Event coordinates, matching-predicate ArrayList mutation, and the exact supplied getter throwable. Four additional nonprimary MouseDown/MouseUp guard invocations return before search/effects, and actual listener registration is lazy.

There are 28 before/after differential cases and 62 recorded callback observations, including the three mutant discriminators. Both matched and missed list mutations expose the enhanced-for mutant: the original throws ConcurrentModificationException, while the mutant returns without that exception. Early coordinate evaluation loses the tooltip after getBounds mutates the same Event. All three probes are detected in `target/runtime-evidence/mutations.tsv`.

The observable owner API remains identical: 15 nonprivate methods, two constructors and three nonprivate fields, totaling 20 signatures. `api-census.tsv` and `api-signatures.txt` retain that evidence. `source-bindings.tsv` pins both full toolbar sources and all three real neighbors. `compiler-binding.tsv` pins the compiler version, path and executable hash.

The SDK lane is explicitly historic and test-only: official Maven Central SWT win32 x86_64 3.126.0 and JFace 3.33.0; every one of their 787 and 613 classes respectively has major version 61. Exact jar/POM hashes, embedded notices and manifests are retained under `proof-sdk/SDK_CUSTODY.json` and its metadata directories. The shippable `m3/stream-segment-proof/tools/resolve_sdk.py` reconstructs and checks this custody, defaults to cache-only operation and admits downloads only with `--download`. A fresh official download replay passes; a deliberately corrupted cached artifact is refused. SDK binaries stay outside Git.

The initially selected current SWT/JFace SDK has Java21 bytecode. The original unchanged widget source fails strict Java21 compilation with four preexisting `this-escape` warnings. That failure is retained at `proof-sdk/baseline-java21-this-escape.log`; no warning is suppressed and no global gate is weakened. The green historic-SDK proof does not claim all current-SDK/platform behavior, UI rendering, zero allocations or universal semantic equivalence. The harness is contract-driven actual-source execution and source-aware fixture review, not an independent source-blind validation run.

Final test SHA256: `5f08977f4ab923bb16f583f7dc8f9d7be4a2a793dab55c93bf0e2e3349b26221`.

Resolver SHA256: `e1561432c5a76471c404c0536126747179075d66135810aa5352a02f70fa6a61`.

Toolbar before SHA256: `fd00029ccb595dfedd0ec1c0604856359e065c9d2ca3fcbbe86382b15bbc0c59`.

Toolbar after SHA256: `c02686f975553876c50392f013759d95bf44b792b2b77bc7bba5cb9eddb10407`.
