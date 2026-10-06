# Nebula M3 A3 mastery receipt

Status: M3 recipe-first tooling contract. The ordinary Nebula Tycho reactor remains authoritative.

## Purpose

Nebula already has an A3 compiler/runtime laboratory, local Atomize/Patternize/Document recipes,
source-sealed recipe delivery, and a no-root-write candidate materializer.

This gate makes recipe mastery mechanically precede candidate materialization.

## Live mastery campaign

The hostile fixture corpus stays deterministic and contains executable Java mixed with Java-looking:

- String data;
- comments;
- text blocks;
- regular expressions;
- direct and Java-21 modern caller forms;
- LF/CRLF;
- deterministic member-order permutations.

The live FILE leaf set is:

```text
A = Atomize
P = Patternize
D = Document
```

The mastery campaign covers all non-empty ordered subsets of A/P/D plus the retained
`A>P>A` and `P>A>P` already-achieved-operation stress schedules: 17 schedules for every
fixture.

Every changing pass must:

- parse/print losslessly through OpenRewrite Java LST;
- compile with Java 21 `--release 21 -Xlint:all -Werror`;
- preserve public/protected surface;
- preserve runtime behavior;
- preserve code-looking data;
- preserve a deterministic precompiled regex/string observation root;
- reach a complete unchanged sweep;
- replay at a second-pass textual fixed point;
- converge to the same normal form for equal operation sets.

## Content-addressed receipt

Mastery evidence is written only below:

```text
m3/recipe-first/target/a3-mastery/
  receipt.tsv
  pins.tsv
  lab/results.tsv
```

The receipt binds the lab result bytes and exact SHA-256 pins for the active FILE semantic closure:

- A3 lab/cases/apply;
- mastery receipt + strict codec;
- regex matrix;
- recipe-first Maven descriptor;
- declarative convergence catalogue and YAML;
- FILE DAG;
- inventory/candidate-table/eligibility support;
- Atomize/Patternize/Document leaves;
- proven SvgLoader FILE leaf.

If any bound owner changes, the receipt is stale until mastery is rerun.

## Apply gate

The command-line `NebulaM3A3Apply` front door requires a current mastery receipt before it writes
candidate copies. The lower-level programmatic `run(...)` API remains unchanged for focused tests
and internal composition.

The original Nebula Java source remains read-only. Candidate output remains confined below
`m3/recipe-first/target/`.

## Authority boundary

A mastery receipt grants no:

- source-root mutation authority;
- API or compatibility-change authority;
- JNI/native execution or equivalence authority;
- SWT lifecycle/threading equivalence;
- promotion or merge authority.

The existing Java-before-JNI policy remains mandatory. Any native/JNI enhancement must establish the
Java contract first and then pass the native differential/lifecycle lane.

## Recipe-first delivery

The existing `NebulaM3A3MasteryRecipe` is the canonical installer. It is evolved to support exact
reviewed preimage -> postimage transitions for its occupied mastery owners, create missing receipt
owners, and refuse all other source states.

The recipe transforms its own prior source as one reviewed target, so the mastery installer is
self-hosting. A second run over the resulting packet must produce zero changes.

## Product build boundary

Maven/OpenRewrite is the M3 authoring and proof plane. It does not replace Nebula's Tycho/product
build. Completion still requires the normal Nebula compile/test/UI/platform gates relevant to each
changed widget or subsystem.
