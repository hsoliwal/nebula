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
mvn -B -ntp -f m3/recipe-first/pom.xml -Pdag-manifest -Dtest=NebulaM3HostPortabilityTest,NebulaM3RefactorScopeTest,NebulaM3OrchestratorPlansTest,NebulaM3RecipeDagManifestTest,NebulaM3RecipeDagManifestCliTest verify
```

It compiles the Java 21 recipe module, runs focused JUnit semantics/authority tests, writes
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
