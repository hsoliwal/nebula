# Nebula complete source packet closure — 2026-10-05

Base commit: `307e12a56eb323bb1e1f08b19097d7b8de59b0b8`.
Parser: `com.synexia.recipe:openrewrite-line-terminators:8.90.4-m3-javadoc-e898332e8f88`.

All **1,891 Git-tracked Java files** have statement/block/loop composition packets,
containing **173,811 non-root atoms**. `all-files.tsv` equals the complete tracked Java
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
The Nebula candidate is materialized by four existing hash-pinned OpenRewrite recipe
stages; `replay.py` reproduces it without mutating the source checkout.

The completion properties describe source composition. The ordinary Java/native reactors
remain separate gates, recorded in `verification.json`. Windows/macOS native execution,
strict canonical repository-wide promotion, universal equivalence and a new performance
speedup are not established by packet coverage.

This tree adds the regression test and updates the existing source-owned Javadoc visitor.
The other two parser overlay classes and the original donor/source/license files stay sealed.
All three parser class descriptors are unchanged. The new Maven version binds the repair
to its source; predecessor checksum and patch receipts remain retained as prior evidence.
