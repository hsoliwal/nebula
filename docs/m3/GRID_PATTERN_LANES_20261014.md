# Grid parent-owned pattern lanes — source-sealed receiver

Stack base: PR #174, `m3/grid-word-mask-moves-20261014`, commit de5068308a765f930749476baefb9fd6637050bf. This preserves the existing RowBitLanes word-movement change and its separate qualification gates.

Canonical source/recipe owner: [hsoliwal/com.synexia#11406](https://github.com/hsoliwal/com.synexia/pull/11406), commit 0505992a5cb8978144603ba5448056c7ffaff31f, committed before this receiver. Named recipe `com.synexia.M3NebulaPatternLanes20261014` reuses unchanged M3GpuToolJoinRecipe / M3GpuToolJoinManifest.

Exactly two existing production sources change:

- RowStateLanes.gather uses one binary seek and a forward pattern cursor for the requested mask window. Same override > pattern > default, scalar behavior, split phases, mask OR and caller-buffer tail; no per-item object or extra persistent state. Output blob 0a5289dce073bb99f9817a2e80b1079fce424bf1, SHA-256 4da40cff4ffb2cb840a4da7e7d8c4589cb47c7c6119326b6cf79be8c12b1d458.
- RowSparseBits stages four replacement directory arrays before publishing. It also reuses SWT ItemBitSet's already-existing runEnd clamp before adding a chunk base, fixing the maximum-int overflow. Output blob 97dc64767bf1a50cabdbf49fde4b577cf01ef7a9, SHA-256 2e6c1940f4647f219b30b59b6881f8bb39b3b92343156aec04bb097211730505.

Both outputs exactly equal the committed canonical templates. HeapGrowth, HeapSampler, Grid Item identities, row/column windows, scrollbar/event/lifecycle owners and public/protected APIs are unchanged. No additional viewport wrapper, renderer, native symbol or runtime controller.

Executed cross-toolkit finite evidence in Synexia: 3,671,313 assertions / 33,184 windows at three heap limits; 982 canonical kernel checks; both original sparse stores fail the second injected allocation while candidates retain all four references and permit retry; three faulty gather variants detected; original Nebula max-int regression reproduced and fixed. Real C17 JNI scalar test oracle covers 1,516 windows plus seven invalid inputs with UBSan and -Xcheck:jni. JNI is proof-only, not production acceleration. The SWT side uses an explicitly disclosed SWT.error fixture only, not a native Display model.

Reproduce from the pinned Synexia checkout:

```sh
python3 synexia-openrewrite-recipes/recipes/widget-pattern-lanes-20261014/verify.py --jni
mvn -o -B -ntp -f synexia-openrewrite-recipes/recipes/widget-pattern-lanes-20261014/pom.xml test
```

The first command was executed; actual Maven/OpenRewrite SDK, full Grid/Tycho/GWT, SWT native display, selection/focus/pinning/disposal, DPI/IME/accessibility, row/column painting/overscan and application performance remain NOT_RUN. Prior #174 evidence is not fresh combined-tree evidence. Keep draft and retain the base PR dependency.

The entire SWT/Nebula request remains the scope goal; this receiver does not implement every widget family. Existing Synexia all-widget inventory and this task's COVERAGE_DELTA record the remaining work. CATEGORY_EVALUATION stores the selected algorithm and LeetCode/HackerRank/GeeksforGeeks taxonomy comparisons; no challenge or editorial source copied. Existing Nebula EPL-2.0 notices are retained. No default-branch writes, force-push, rebase or automatic merge.
