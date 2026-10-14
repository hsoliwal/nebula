<!-- SPDX-License-Identifier: EPL-2.0 -->
# M3 portable Maven/OpenRewrite recipe DAG

The **one** canonical M3 FILE recipe DAG is host-independent. Windows, WSL2, macOS and Linux
must produce the same semantic DAG and scheduler-plan SHA-256 roots when supplied with the same
Java 21 source tree and recipe artifact versions. Other Unix hosts may use the same Maven entry
point, but require separate runtime/build evidence before they are described as verified.

## Canonical work unit

```text
INVENTORY
  -> ATOMIZATION
  -> PATTERNIZATION / IOP
  -> DOCUMENTATION
  -> FILE FIXED POINT
  -> (separate, explicitly admitted wider-scope proof)
```

The already-owned `NebulaM3RecipeDagManifest` emits the exact recipe order, classes, dependencies,
scope and authority. `NebulaM3OrchestratorPlans` projects that same immutable DAG into Camel,
Airflow and Drools views. Scheduler selection never grants source mutation, equivalence or
promotion authority; OpenRewrite remains the source-change mechanism and JUnit/contract proof
remains the acceptance gate.

The platform-neutral unit is **Java 21 + Maven + OpenRewrite + the same content-addressed
recipe DAG**. Do not build separate operating-system-specific copies of the recipe logic.

## Local proof on supported hosts

Install Java 21 and Maven 3.9.11. From the repository root, run this same one-line command in
PowerShell, Command Prompt, macOS Terminal, Linux shell or a WSL2 Linux shell:

```text
mvn -B -ntp -f m3/reactor.xml -pl recipe-first -am -Pdag-manifest,transfer-contract -Dtest=NebulaM3HostPortabilityTest,NebulaM3RefactorScopeTest,NebulaM3OrchestratorPlansTest,NebulaM3RecipeDagManifestTest,NebulaM3RecipeDagManifestCliTest,NebulaM3TransferContractTest -Dsurefire.failIfNoSpecifiedTests=false verify
```

The source-first `m3/reactor.xml` first resolves/builds the qualified OpenRewrite parser\ndependency, then compiles the Java 21 recipe module, runs focused JUnit semantics/authority tests, writes
`m3/recipe-first/target/nebula-m3-dag/`, and does **not** apply a source-changing recipe.

Expected host-independent evidence:

- `recipe-dag.tsv` and `recipe-dag.sha256`;
- `orchestrators.tsv`;
- `camel-route.yaml`, `airflow-dag.py`, `drools-agenda.drl`;
- `orchestrator-plans.sha256`;
- JUnit Surefire reports with zero failures, errors or skipped selected classes.

UTF-8, LF line endings, canonical recipe identifiers and dependencies are part of the hashed
evidence. Paths with spaces must work. The semantic SHA-256 roots must agree across hosts.

## GitHub CI acceptance

`.github/workflows/m3-portable-recipe-dag-java21.yml` builds the same module with the same
Maven and Java versions on:

| Runner | What it qualifies |
| --- | --- |
| `windows-2025` | Windows native JVM/Maven/OpenRewrite recipe proof |
| `ubuntu-24.04` | Linux JVM/Maven/OpenRewrite recipe proof |
| `macos-15` | macOS JVM/Maven/OpenRewrite recipe proof |

The final fan-in job rejects any missing host receipt or divergent DAG/scheduler roots. It must
not infer a PASS from an absent, skipped, stale or failed workflow job.

**WSL2** uses the Linux toolchain and the same local command; GitHub's Ubuntu runner does not
independently certify the WSL virtualization/mount boundary. A real WSL2 acceptance run must
produce and compare its own receipt. Other Unix systems (for example BSD/AIX) require compatible
Java 21/Maven distributions and separately recorded runs; they are not silently certified by
this three-runner matrix.

## Native and UI boundaries

A Java recipe JAR and the OpenRewrite DAG are platform-neutral. SWT/GTK/Win32/Cocoa rendering,
JNI/C/C++ binaries, OpenCL/CUDA and native library loading are **not**. They require
platform/architecture-specific builds, lifecycle/fallback checks, Java/JNI differential tests
and runtime acceptance. A Linux WSL2 native binary is an ELF/Linux artifact, not a Windows DLL.

Airflow DAG generation is portable; operating an Airflow scheduler on Windows typically uses
WSL2 or a Linux container. Camel and Drools can run as JVM components. Their generated plans
are candidate scheduling evidence only, never a source-mutation shortcut.

A recipe proven here becomes eligible for bounded FILE fan-out, not automatic source promotion.
The original Tycho/SWT product build stays authoritative and the non-destructive `master`
history remains intact.


## Saved transfer artifact and backport order

The saved proving branch is:

`m3/final-portable-recipe-transfer-20261014`

The transfer bundle is emitted by the existing Maven profile:

```text
mvn -B -ntp -f m3/reactor.xml -pl recipe-first -am -Ptransfer-contract verify
```

and binds:

- the Java 21/OpenRewrite recipe DAG root;
- Camel, Airflow and Drools scheduler-plan root;
- the exact recipe coordinate and root Maven profile;
- the proof repository and saved proof branch;
- the two transfer targets: `hsoliwal/M3jdk21` and `hsoliwal/com.synexia`;
- `HOLD_UNTIL_PROVEN` transfer state.

The host-portability matrix now compares the transfer root as well as the DAG and scheduler roots.
A transfer artifact is therefore not considered portable if Windows, macOS and Linux disagree on
any of those identities.

Backporting has **not** been abandoned. It is deliberately downstream of this proof:

```text
Nebula
  Mavenize / preserve Tycho authority
  -> inventory
  -> atomize
  -> patternize + IOP
  -> documentation
  -> FILE fixed point
  -> original product build
  -> portable three-host root agreement
  -> TRANSFER ELIGIBLE

then mechanically:

com.synexia
  -> install/improve canonical reusable recipe atoms/DAG
  -> preserve provenance and JUnit/fixed-point proof

M3jdk21
  -> install/verify Maven + OpenRewrite orchestration around the existing OpenJDK build
  -> inventory JDK21 target contracts
  -> classify FILE / VISIBILITY / PACKAGE / MODULE / MULTI_MODULE / LIBRARY_API
  -> replay only admitted compatible recipe atoms
  -> continue JEP/PR/tooling backport ledger through the same proof gates
```

No Nebula result grants automatic source mutation or promotion authority in either target
repository. Each target must bind its own exact inventory/preimages and prove its own compiler,
tests, runtime/JNI parity where applicable, and fixed point.

Until the Nebula one-build proof and three-host matrix execute successfully, the transfer contract
must remain `HOLD_UNTIL_PROVEN`.

### Machine-readable backport plan

The transfer bundle now includes `backport-plan.tsv`, rooted by
`NebulaM3BackportPlan.root()`. It fixes the downstream dependency order:

```text
NEBULA_PROOF
  -> SYNEXIA_RECIPE_ABSORPTION
  -> SYNEXIA_RECIPE_PROOF
  -> M3JDK21_MAVEN_ORCHESTRATION_BOOTSTRAP
  -> M3JDK21_BACKPORT_INVENTORY
  -> M3JDK21_FILE_REPLAY
  -> M3JDK21_SCOPE_PROMOTION
  -> TARGET_PROOF
```

The Synexia stage absorbs or improves **small OpenRewrite recipe atoms** and composes them into the
same scheduler-neutral DAG. Maven/OpenRewrite remains the source-change mechanism; Camel, Airflow
and Drools/KIE remain orchestration/admission projections with no direct mutation or promotion
authority.

The M3JDK21 bootstrap stage installs or verifies the Maven/OpenRewrite orchestration plane around
the existing OpenJDK make/autoconf build; Maven does not replace the JDK product build. The
inventory stage then retains the larger backport programme. It inventories later OpenJDK **JEPs,
PRs, non-JEP tooling changes and compatible dependency closure**, records contract
and Java-21 compatibility, and only then admits recipe atoms. FILE-local compatible work may fan
out independently. Any boundary-crossing change promotes authority only through:

```text
FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API
```

The final target proof remains diff -> lint/static analysis -> compile -> jtreg/JUnit -> runtime,
JNI parity where declared -> setup-inclusive benchmark where a performance claim is made -> second
pass fixed point. The transfer/backport plan itself has no target mutation or promotion authority.

## October 14, 2026 portable-workflow repair and proof status

The saved transfer branch contains a repaired, single-owner portable Actions workflow. The previous
workflow file had duplicate `compare-host-receipts` jobs and truncated PowerShell fragments, which
cannot constitute valid hosted proof. The repaired workflow has exactly one Java-21 worker matrix,
one fan-in reducer, a full three-root UTF-8/LF receipt, a strict selected-JUnit report gate and a
clean-Git-worktree gate. `m3/ci/test_portable_recipe_workflow.py` locks the structure against
the known duplicate-job/truncated-handoff regression; the existing one-build workflow executes the
`m3/ci` Python unit tests.

**Observed status:** structural read-back of the repaired workflow passed, but no new GitHub Actions
run was reported for the repaired PR head during this review. Maven/JUnit, Tycho/SWT runtime,
Windows/macOS/Linux portability and 99% semantic coverage must therefore remain **UNVERIFIED**.
Do not infer that the recipe is deployable or transplant source modifications into either
downstream repository until the hosted proof and target-specific contracts are actually green.


## October 14 — One-build source-first reactor repair

The one-build workflow originally invoked `m3/recipe-first/pom.xml` directly before a clean
runner had installed the pinned `com.synexia.recipe:openrewrite-line-terminators` dependency.
That module is built from source in `m3/openrewrite-line-terminators`, not published as an
independent trusted substitute for the checked-in Nebula dependency. Executing the recipe module
alone therefore cannot establish a reproducible clean-runner dependency closure.

The saved transfer branch now uses the existing source-first reactor for **both** initial proof
and candidate post-application reproof:

```text
mvn -B -ntp -f m3/reactor.xml -pl recipe-first -am \
  -Psemantic-coverage,dag-manifest clean install

mvn -B -ntp -f m3/reactor.xml -pl recipe-first -am \
  -Psemantic-coverage verify
```

The initial `clean install` runs Maven's `verify` lifecycle exactly once; explicitly requesting\n`clean verify install` would duplicate compile/test/verification work.\n\nThe one-build job pins Maven 3.9.11, matching the three-host portability job. The existing
`m3/ci/test_one_build_source_first_reactor.py` locks the source-first reactor topology and
rejects module-only builds, missing dependency closure, skipped JUnit, lost fixed-point replay,
and removal of the Tycho product gate. The Python tests run before recipe candidate application.

This is a **CI admission repair only**, not a passed build claim. The one-build job must still
actually complete the original Tycho/SWT reactor, 99-percent semantic-coverage profile and
second-pass fixed-point proof. The transfer contract remains `HOLD_UNTIL_PROVEN` until that and
the three-host receipt comparison succeed. Neither Camel, Airflow nor Drools receives mutation
or promotion authority from the repair.
