<!-- SPDX-License-Identifier: EPL-2.0 -->
# M3 recipe DAG dependency waves

## Purpose

The Nebula proving crate already has a Maven/Tycho product build, Java 21 OpenRewrite atomization,
patternization/IOP and documentation recipes, a fixed-point convergence recipe, and deterministic
Camel/Airflow/Drools projections.

This extension does **not** create a second recipe engine or new source mutation mechanism.

`NebulaM3DagWavePlanner` reads the canonical `NebulaM3RecipeDagManifest.Node` records,
validates their dependency structure, and derives immutable scheduling-readiness waves. Its input
is metadata about **already-admitted** recipe atoms; it never inspects, changes or publishes
product source.

## Mechanical contract

```text
INVENTORY
    |
ATOMIZATION
    |
PATTERNIZATION / IOP
    |
DOCUMENTATION
    |
PROVEN FILE CONVERGENCE
```

The ordering and the complete set of phases remain mandatory. Multiple atoms may belong to a
single phase. A later-phase node must transitively depend on **every** node in the immediately
preceding phase.

The validator rejects wrong ordinals, duplicate or noncanonical node IDs, unresolved/forward/
cyclic dependencies, duplicate dependencies, missing phases, phase-barrier bypass, scope above
FILE, forged read-only/candidate authority, and oversize graphs.

The implementation uses an integer-indexed `BitSet` dependency closure. It constructs deterministic
waves ordered by original source ordinal, and returns immutable lists. It never interprets waves
as permission for parallel source mutation.

## Current Nebula behavior

The existing five recipe atoms still form **five serial waves** because there is currently one
atom in each phase. The manifest now derives edges from the phase barrier rather than list
adjacency. If several admitted atoms occupy one phase, they share the preceding phase dependency
frontier and can appear in the same readiness wave.

Camel carries the validated wave number and exact `dependsOn` set with every dispatch. Airflow
edges are emitted from `dependsOn`, and Drools readiness rules require every declared dependency.
These are scheduling projections only: source mutation inside one FILE candidate remains serial.
No SWT widget source, Java public API, JNI ABI or graphics contract is modified.

To exercise branching validation, JUnit uses a synthetic graph with two independent ATOMIZATION
nodes, a PATTERNIZATION node depending on both, and subsequent DOCUMENTATION and fixed-point
nodes. The resulting wave sizes are `1, 2, 1, 1, 1`.

## Authority and ownership

Wave readiness is **not** proof that two recipes can run concurrently on the same file. Source
changes remain FILE-local, serial per file, contract-locked and owned by OpenRewrite. Independent
files may fan out only after separate path/contract fences and JUnit/differential proof.
Canonical promotion remains serial.

Camel/Airflow/Drools can project the same dependency DAG and readiness waves, but cannot certify
semantic equivalence, mutate source independently or promote code. Maven/OpenRewrite remains the
execution owner; JUnit/compiler/fixed-point/Tycho evidence remains the proof owner.

`hsoliwal/com.synexia` remains the canonical reusable recipe owner. This Nebula validation is a
product/proving-side contribution under Nebula's existing license. Once the recipe family and its
proof are complete, Synexia may assimilate the mechanism without copying Nebula product code
without provenance review.

## Proof and backport disposition

The Maven proof unit is:

```bash
mvn -B -ntp -f m3/reactor.xml -pl recipe-first -am verify
```

The original Nebula/Tycho product acceptance remains:

```bash
xvfb-run -a mvn -V -B -ntp clean verify -Dtycho.localArtifacts=ignore
```

The backport target order is **Synexia recipe custody** then **M3JDK21 Java 21 JEP/PR/tooling
inventory and compatibility lanes**. The current proof bundle and this new scheduling guard grant
no authority to modify either target automatically. Full verification must include recipe/JUnit
proof, atom/pattern/IOP/contract coverage, unchanged second-pass output, SWT runtime/resource
tests where applicable, and explicit review.

A PR or CI workflow that has not executed these checks must remain **HOLD/NOT_RUN**, not PASSED.
