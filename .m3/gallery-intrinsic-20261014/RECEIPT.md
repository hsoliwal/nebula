# M3 — intrinsic Nebula Gallery viewport projection receiver

**Source-sealed first:** [Synexia recipe #11393](https://github.com/hsoliwal/com.synexia/pull/11393), commit `97431e831052a4370b2efe967bc4f8c36193b47c`.
**Receiver input:** `hsoliwal/nebula@c81b687124ac2afa596cfa8a4f69a1760df73bd9`.
**Transformation engine:** `M3GpuToolJoinRecipe` / `M3GpuToolJoinManifest` already in Synexia; recipe `com.synexia.M3NebulaGalleryIntrinsic20261014`.

Three original existing EPL-2.0 Java files are byte-for-byte the exact after templates in the
Synexia recipe. Their public/protected declarations are preserved; the existing Gallery
is still the event/SetData/scrollbar/GC/selection/identity owner. This is NOT a new widget
or generic viewport wrapper. There are NO production JNI methods in this leaf.

| Owner | Input SHA-256 | Reviewed output SHA-256 |
| --- | --- | --- |
| `Gallery.java` | `cee2acc5d7fb663501b4975dfdb165e150214af5e27faffe98c81a6f4f9bbb77` | `6c6d831cb1fa9568c41bd03313e42408bd258abe90484c8af65fe8edee67f7bf` |
| `AbstractGridGroupRenderer.java` | `a32c83429f3562a110381708d9ec6b41cd94ddb033444a1c86b56fb2b012f92e` | `816b025fe18868cdd95de1e84864659264a47f649919bf0046f87ec67a1b77eb` |
| `DefaultGalleryGroupRenderer.java` | `4343728e7305b11fea9a6872039e7fb2cd02e4215088b005e08b4e43e4361388` | `dd0886c9067b3120133eafba510d72a0b5bd27ecb1b123cd1dd7d832e904ad85` |

**Candidate implementation:** primitive root paint index vector instead of boxed
`ArrayList<Integer>`; binary skip only for monotone, fully laid-out default
renderer (custom renderers and virtual placeholders keep serial traversal);
64-bit child clip arithmetic bounded to real item count; empty fill guard.
Visible geometry alone determines paint work; no speculative prefetch or
extra SetData callbacks.

**Qualification:** 149,501 independent Java21 model assertions passed in the
canonical Synexia branch. Source-level signature comparison passed. Here the
contents are an exact manifest-checked template replay, but a real Maven/OpenRewrite
SDK application has NOT been executed in this environment, so the recipe's
SDK replay/idempotence/refusal test remains a separate required gate.
Nebula Gallery Tycho, native SWT Display on Win32/GTK/Cocoa, GWT,
DPI/RTL/disposal/accessibility/reentrant SetData, screenshot parity and
frame timings remain **NOT_RUN**. Promotion is **HOLD**.

Do not infer million-item backing sparsity from faster viewport painting:
the Gallery's logical root `GalleryItem[]` is unchanged. A true paged sparse
root/item-proxy conversion requires its own recipe with public facade identity,
SWT events, selection, clearing and disposal proof. No upstream repo edits,
no default-branch write, no rebase/force-push. SWT/Nebula donor EPL-2.0 retained;
Synexia recipe Apache-2.0; no challenge-site source copied.
