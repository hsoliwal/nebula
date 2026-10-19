# Original Grid viewport endpoint geometry — 2026-10-19

Base:d101f614d7010524afde1fdf18064cc0afc1d9b1.
Canonical recipe: https://github.com/hsoliwal/com.synexia/pull/12470
Exact source pin:f3cebda868c7c15459f295223ff6abb3e987b95e.
Class-backed recipe:com.synexia.rewrite.M3NebulaViewportGeometryRecipe.
Crate:synexia-openrewrite-recipes/recipes/widget-backing-20261014/viewport-geometry-20261019/.
The existing canonical M3GpuToolJoinRecipe/Manifest remain unchanged.

Only GridRowExtents.rankAtExtent changes: after the existing negative check,
return visibleRows for offsets at or beyond totalExtent. These valid viewport
endpoint queries become O(1). Interior Fenwick descent, extentOfRank, rank/select,
append/update, hidden rows, heap growth and fail-closed rebuild remain unchanged.
This modifies Grid's existing Fenwick geometry implementation, not GridRowTree,
GridRowSpanIndex, a new Grid, a Canvas wrapper or a copied collection backend.
Current row-span cache and GC ownership repairs are inherited unchanged.

Original blob:c9c1db07f34ce877f8105b8f2609d6dddb6204c1
Candidate blob:fb64f9829256405c5306064f46a4de1faf3b04da
Before SHA256:d5650465c5001bbda7ab6cf1039df9a4f9ba22d23dd7bfbe61479f3afd52a144
After SHA256:84db339ce29dd0c2995f67e8a614854e790799829e1daee8efc1fdeefaa76f63
Proofs:7796b40faafe49809ef9b478a54f4a91744d0593 and49ebcdb78e3383ca036d347068731eddcc474a56.

Fresh full-source component execution uses real GridRowIndex/HeapGrowth/HeapSampler,
not widget-shaped doubles. Strict Java21 --release21 -proc:none -Xlint:all -Werror;
355,404 geometry checks and68,288 endpoint checks both before/after. Digests:
15300954161628219929 and16505807334434896952. Full declared javap -p -s surface
is unchanged. Rebuild, set, append, MAX_VALUE extents, Long.MAX_VALUE offsets and
empty/all-hidden rows are covered. An incorrect half-total guard compiled and
failed the endpoint proof twice. Counts are assertions, not independent UI tests.

The initial fused extentOfRank candidate was rejected after slowing both measured
process pairs. Selected endpoint CPU/query:42.97->3.77ns and43.47->0.65ns; mixed
endpoint/interior55.44->16.33ns and56.17->15.04ns. Interior-only83.56->96.77ns
and93.59->91.79ns: one adverse pair retained. No universal performance acceptance,
JMH claim or native Grid frame-rate result. Hot JIT can hoist endpoint-only reads.
Warm query allocations remained zero in this bounded geometry probe.

The canonical verifier emits this exact source and both standalone proofs from
six sealed inputs (three generated outputs, three unchanged dependencies). It
passed99 initial/105 replay checks,64 mixed states and refusal/fixed point.
Run python3 verify.py reviewer01 in the canonical task/archive to reproduce fresh
component evidence. Its Maven SDK command requires a complete canonical checkout.

DRAFT / COMPONENT_PASS / NATIVE_WIDGET_HOLD. Actual Maven attempt exited127 because
mvn was absent. Authored SDK/JUnit fixture is NOT_RUN; standalone proofs are not
registered in default Tycho/JUnit discovery. Native Grid/SWT Display, scrolling,
SetData/events, screen captures, pixel validators, UI/native resource budgets,
full Tycho/OSGi and GWT/RAP remain NOT_RUN. No JNI source, ABI or artificial JNI
oracle added. Existing source dual Apache-2.0/EPL-2.0 notices retained.
