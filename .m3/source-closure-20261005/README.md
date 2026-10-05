# Nebula complete source packet closure — 2026-10-05

Base commit: `307e12a56eb323bb1e1f08b19097d7b8de59b0b8`.
Parser: `com.synexia.recipe:openrewrite-line-terminators:8.90.4-m3-javadoc-e898332e8f88`.

All **1,891 Git-tracked Java files** have statement/block/loop composition packets,
containing **173,883 non-root atoms**. `all-files.tsv` equals the complete tracked Java
path set. Every source hash matches; missing, stale and parse-error counts are zero.
Reference/donor files and negative fixtures are included without changing their bytes.

`statement-atoms.tsv.gz` stores the existing canonical packet fields. The existing
`RestoreAtoms` implementation reconstructs all original JSON packets from the sealed
sources and index, then checks packet hashes and exact source recomposition. The CLI
also reparses each recomposed file and proves the composition recipe reaches a fixed point.
`patterns.tsv` counts the existing pattern categories, including root-file atoms.

`native-and-images.tsv` seals 0 C-family source files and 497
image files, including 345 PNGs, against the base Git bytes. PNGs remain binary
assets; they are preserved, not presented as Java atoms. Native lexical/structural
reconstruction has a separate receipt; it is not a C/C++ preprocessor semantics proof.

Reproduce from the paired canonical `ui-source-closure-20261005` Maven recipe crate:
install the source-owned parser from Nebula's `m3/reactor.xml`, build the crate and its
runtime dependency classpath, then use `run.py` on this checkout. Use `RestoreAtoms`
with this index and manifest to check the packed representation independently.
The Nebula candidate is materialized by five existing hash-pinned OpenRewrite recipe
stages; `replay.py` reproduces it without mutating the source checkout.

The completion properties describe source composition. The ordinary Java/native reactors
remain separate gates, recorded in `verification.json`. Windows/macOS native execution,
strict canonical repository-wide promotion, universal equivalence and a new performance
speedup are not established by packet coverage.

This tree adds the regression test and updates the existing source-owned Javadoc visitor.
The other two parser overlay classes and the original donor/source/license files stay sealed.
All three parser class descriptors are unchanged. The new Maven version binds the repair
to its source; predecessor checksum and patch receipts remain retained as prior evidence.

The existing plan-binding test expected fixed point at ordinal 7 although PLAN.tsv
declares ordinal 8 after the review DAG insertion. A fifth hash-pinned recipe changes
only that expected ordinal. The initial failing full run and passing five-test focused
rerun are retained; `recipe-qualification.json` records all 97 passing suite receipts
and the 288-case mastery checks without representing the rerun as another full run.

The final source inventory also includes merged Grid graphics PR #68. The three changed
Grid files are freshly reparsed; 1,888 unchanged packets are reused only after exact source
and packet hash checks. Integration base: `afb626069dd3cc029050e3088ebdfdb0b29b1c3a`.
Two additional frozen recipe stages repair exact canonical quadrant rotation and CI parser
build order/custody admission. Existing Grid assertions and all historical custody checks
remain intact. The new custody branch admits only four exact unchanged postimages from
`48846be`; independent preimage/postimage byte-drift refusals are tested. See `INTEGRATION.json`.

The final fresh Linux GTK reactor passes all 314 modules and 212 tests, with zero
failures, errors or skips. Its summary and raw-log hash are in the final verification receipt.
