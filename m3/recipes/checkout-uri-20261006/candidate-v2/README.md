# Qualified Nebula checkout URL recipe

This candidate qualifies **checkout URL relocation only**. It is bound to the
proven owner at HEAD `74f97f3e7feb29239dbc0332e9206586d31f4722`, all 5,399 current
source paths and the inherited original 314-project reactor. The prior actual
default `clean verify` completed 314 SUCCESS, zero FAILURE and zero SKIPPED. Its
receipt is retained as `INHERITED_DEFAULT_BUILD_RESULT.json`; that result belongs
to the original owner path and is not a relocated-runtime result.

The exact source recipe changes two existing address owners: the parent POM and
`m3/qualified-mixed-original435-target/mixed.target`. Five address values share
one early session property, `m3.nebula.checkout.uri`: three qualified repository
defaults, the target repository location and the existing dynamic Batik URL.
The root POM, all modules, tests, lifecycle configuration, JavaSE-21, three target
environments, 125 explicit target IUs and 129 artifact mappings retain their bytes.
The parent target-file binding remains a file path owned by the existing Maven
root property. Every source delta is an address substitution, with exact reversals
proved against the preimages.

Maven's checkout-root property is a directory string. Its Windows backslashes
and raw spaces are invalid in Tycho's direct `URI` target parser. A POM-only
property is also unavailable to that parser. The existing CPU launch pattern
therefore computes `Path(root).resolve().as_uri()` and supplies it in `MAVEN_OPTS`
before Tycho starts. It binds `MAVEN_BASEDIR` to that verified checkout directory,
replacing a caller's root override. Parent URLs use `${m3.nebula.checkout.uri}`; the target uses
`${system_property:m3.nebula.checkout.uri}`. The recipe launcher retains the
inherited complete reactor argv, including `clean verify` and
`-Dtycho.localArtifacts=ignore`, and preserves the tests. An ordinary Maven command
without this URI binding does not meet this candidate's launch contract.
The launch adapter clears inherited JVM/Maven option injection sources before
assigning its own options. Both Python admission helpers explicitly refuse
optimized execution; the production recipe invokes the context helper with
`-I -B` so caller Python optimization options cannot suppress the guards.

The focused proofs exercise Maven root/child inheritance from root, nested child
and unrelated working directories, using plain, space-containing and percent/hash
paths. The exact installed Tycho 4.0.12 resolver parses relocated sealed target
files, retains the 125-IU selection, opens the fixture-owned exact metadata and
rejects raw Windows root expansion. These are address/metadata discriminators;
they do not execute a relocated 314-project reactor. The separate Unicode-path
Maven launcher refusal is retained. The full replay launcher explicitly refuses
Unicode checkout names until that launcher context is independently qualified.
The URI oracle separately checks encoded Unicode without claiming that Maven
supports the complete Unicode checkout.

`artifacts.xml` still contains 129 exact absolute mappings into the inherited
shared runtime inputs. They are frozen and verified, without copying a repository,
SDK or cache. This candidate does not establish independently relocatable runtime
artifacts or complete portable output. The current machine's one shared Maven
mirror, JDK 21 and qualified runtime files remain required. Native UI/JNI/Grid
effect admission is outside this address recipe.

The borrowed Maven AntRun 3.1.0 recipe rejects caller-defined internal properties,
unknown operations, HEAD drift, changed candidate inputs, reparse ancestors,
missing/drifted/mixed source states and complete original source-context drift.
Its checksums bind the complete executable source-effect input closure, including
both nested POM images, helpers, manifests and qualified metadata. The independent
source-only publication seal binds all files and final receipts; proof receipts
are outputs rather than self-referential execution inputs.
The context check is bound only to the mutation root; a caller `m3.context`
property is refused. It checks all 5,398 retained paths, the exact tracked source
set, and new untracked or ignored inputs outside Maven `target` outputs. Only the
four frozen qualified metadata files are admitted outside the tracked source set.
Generated output admission is restricted to `target` directories adjacent to
frozen original `pom.xml` or Eclipse `build.properties` project descriptors, including
Tycho pomless plugins. A Java source package named `target` is still checked.
`check` is read-only; `apply` installs only the two exact postimages; `verify` and
`fixedpoint` require complete postimages; repeated postimage apply writes nothing.
There are no new owner source paths. The real recipe's complete owner `check`
and negative controls are separate from a labelled tiny writer rehearsal. The
writer rehearsal substitutes a metadata-only fixture admission helper and proves
the borrowed byte-copy protocol; it cannot admit production source application.
The production recipe refuses that incomplete fixture. Actual production
`apply`/`verify`/`fixedpoint` and the full 314-project replay remain root-owned.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
$env:MAVEN_SKIP_RC = '1'
$env:MAVEN_OPTS = '-Xmx1g -XX:+UseG1GC'
& 'C:/ProgramData/chocolatey/lib/maven/apache-maven-3.9.14/bin/mvn.cmd' -o -B -ntp `
  '-Dmaven.repo.local=S:/codex-work/m3-nebula-20261004/nebula-offline-mirror/repository' `
  -f S:/m3-nebula-portable-recipe-20261006/candidate-v2/pom.xml `
  '-Dm3.root=S:/gm435314v3/owner74f' '-Dm3.operation=check' verify
```

Root owns review and exact source application. After reviewed recipe apply,
the source-bound full replay command is:

```powershell
& 'C:/Program Files/Python312/python.exe' -B `
  S:/m3-nebula-portable-recipe-20261006/candidate-v2/run_portable_default.py `
  --root S:/gm435314v3/owner74f `
  --output S:/m3-nebula-portable-recipe-20261006/full-default-replay --run
```

Omit `--run` to inspect the exact command/environment after source apply. The
launcher requires a fresh external output directory and at least 8 GiB free;
it verifies the complete source and inherited runtime inputs before execution.
No owner source edits, builds, stages, commits or pushes are performed during
candidate preparation. Full replay remains pending root's actual execution.
