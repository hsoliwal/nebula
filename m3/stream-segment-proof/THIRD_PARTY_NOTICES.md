# Donor inventory attribution

Nebula source at master `f4ebfe7edcc1f953d4dd76312f3a7bc6fbce645b` retains its SPDX EPL-2.0 notices under `nebula-donors/`. The existing exact snapshot, source seal, inventory and FILE-local DAG remain owners. OpenRewrite overlay provenance remains Apache-2.0, as recorded in the unchanged qualified owner.

The exact Nebula root license and notices are retained as `EPL-2.0-LICENSE.txt` and `NEBULA-NOTICES.MD`. JUnit runtime alignment uses the existing SDK's5.14.4 dependency closure under JUnit's EPL-2.0 license; no JUnit implementation source is copied or forked.

Synexia canonical sources at `ec384d29e596402e633b7aec2cf6f46fb6cf31d4` and existing L2M sources at `c921c716219794e62032c882df929fb2d6679903` retain their SPDX Apache-2.0 notices under `synexia-donors/` and `l2m-donors/`. Apache-2.0 text is `APACHE-2.0-LICENSE.txt`.

OpenRewrite static-analysis snapshot `5dc62ae0e5889c62d86f6943cad34941af10e37d` is Apache-2.0. Selected source/tests under `openrewrite-donors/` are byte-verbatim with original copyright headers. The complete existing archive and all 306 entry hashes were verified; no copied native implementation is admitted by a NO_FIT row.

Public OpenJDK21u source at commit `98dbb49926d8e911b0713cefd9a2fc40955fa7f7`, tag `jdk-21.0.9+7`, is retained under `jdk21-donors/` solely as semantic reference evidence. Its original GPL-2.0 with Classpath exception headers remain, along with complete LICENSE, ADDITIONAL_LICENSE_INFO and ASSEMBLY_EXCEPTION. Product code calls JDK runtime APIs rather than porting JDK implementation.

The installed Oracle JDK source archive has proprietary/NFTC headers. It is not the FOSS donor and no Oracle source copy belongs in the PR. Private local reference copies were moved outside this shippable inventory; only their hashes and comparisons with public OpenJDK bodies remain in the JSON receipt.

Pending Nebula PR49's exact public metadata is retained as `NEBULA_PR49.json`; it remains an existing read-only external recipe binding. Oracle API documentation and the OpenClaw org index are linked references; no documentation text or OpenClaw implementation is copied.
