<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Exact WebCanvas filter retirement

A filter removal that threw before mutation left the global Display listener installed after the native widget was gone. The receiver keeps pending ownership until an exact API return, retries a failed event once, and bounds the complete constructor/dispose/reentry operation to two attempts per event and four total. Persistent API refusal remains truthful ownership debt.

The first cleanup and original constructor failures retain their identities. Caller-aware cause/suppression traversal avoids newly creating reverse edges or duplicate later-cleanup paths. Temporary identity scratch exists only on exception paths. Public APIs, fields, painting and model storage are unchanged.

Two old tests required one removal call per registered event. Their successor clauses require the exact first pass followed by one Hide retry for the injected cleanup failure. Every original identity, native, CPU, cache, ticker, foreign-owner and resource assertion remains. Originals and the earlier 50/52 failure are retained.

Executed Win32 SWT 4974/JDK 21 evidence: candidate 52 tests pass with no skips, ignores or assumptions; adapted baseline 40 has 16 expected failures; the rejected predecessor has one expected failure in 12 CPU laws. Selected profiles compile 49 sources/70 classes, preserving 938 public/protected API entries and three serialization IDs across 47 production classes. Only WebCanvas production bytecode changes.

Canonical propagation also passes: 55 templates, 104 members, 630 sources/634 classes; 633 sibling bodies, 2982 API entries and 70 IDs stay stable; replay writes zero and 632 mtimes stay unchanged. Canonical companion PR publication is pending in [com.synexia](https://github.com/hsoliwal/com.synexia) at `docs/M3-SCALE/runs/20261116-webcanvas-filter-retirement`.

See [BUILD.md](BUILD.md), [BINDINGS.json](BINDINGS.json) and the qualified leaf recipes listed there. Exact proof inputs, controllers, raw results and all scoped failures are retained through the evidence inventory.

SWT remains EPL-2.0; source/test headers retain Apache-2.0 OR EPL-2.0. Full Maven/Tycho, other platforms, timer/cache failure masking, strong model-callback GC ownership and whole UI/GPU behavior remain outside this proof.
