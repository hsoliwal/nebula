# Shared web-widget paint admission

Hidden widgets and empty viewports must not emit display-list commands or focus rings. This change admits paint only after resetting the reusable display list, using the existing HIDDEN flag and width/height lanes. Visible paint order, exceptions, model state and 53 public/protected API entries per source remain unchanged.

The companion canonical OpenRewrite 8.17.1 recipe is a resource in the existing `synexia-openrewrite-recipes` Maven module. It matches one exact source path and complete preimage; apply through that module's existing Maven/OpenRewrite workflow. The public Nebula receiver has its own exact-path recipe. Do not patch generated source: correct this recipe/template, replay, and compare the output.

Executed qualification: 18 immutable JUnit contracts per source, original 13 failures / candidate zero, total 36 candidate passes and 26 expected baseline failures; JDK21 compilation with all lint warnings treated as errors; unchanged API and visible display-list/model trace; exact stock-engine replacement, fixed point, wrong-path and drift rejection. The unchanged generator's selective emit entrypoint produced the 81-character change and zero-write same/fresh replay in Jimfs.

The million-item hidden-model discriminator records zero emit calls and zero logical-lane visits. Warm timings are observational: canonical median 26 ns/call, public median 108 ns/call on this run; these are not portable thresholds or a fastest-implementation claim. The supplied tests are CPU model tests, not native SWT host validation.

`qualification/WebModelPaintAdmissionProof.java`, its pinned `inputs.json` source closure, and `ram_wrapper.py` reproduce the executed no-disk-output lane using the inherited guard/launcher in immutable ready packet `c0bae8ae4f1fcc53bf015bf58865c3faed837702`. The wrapper expects base64-framed JSON stdin, uses only existing read-only offline jars and waits for ACK after returning its receipt. It creates no test/cache/source files. Run only under the existing storage and memory controls; do not copy caches or use C: build scratch.

Raw executed receipt: `250b73abc5ebe0b5a5d3dee8e93a4797c142e273`; two earlier harness failures and one incomplete observation are retained in the ready-packet lineage. JDK's SecurityManager deprecation and SLF4J no-provider warnings remain in the raw stderr; Java compiler diagnostics were empty.

Native SCREEN/GPU, real SWT visibility/events, JNI swap/cache lifetime, full generator/Maven/Tycho execution, platform/i18n and every-family application behavior remain open. The complete all-web scope is retained: canonical 6323 names / 104 concepts / 11 declared families, with 824 unmapped catalogue rows and missing PICKER/PLOT/EMBED templates. Counts and aliases do not establish implementation completeness. The local donor's QR matrix is explicitly non-scannable; browser/video hosting and richer markdown remain TODOs.

No merge or production promotion is performed by this qualification phase.

Canonical source revision at qualification: c9511205b3345a19420f7ae98445f7c5d1e94b07. Receiver preimage: 5ff1dc1296e3c7aee5997e975405c08b18184fb5. Source change is exactly 81 characters.
