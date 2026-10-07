# Nebula A3 mastery

Status: M3 tooling/test contract. No widget/product API is introduced here.

## Purpose

Nebula already has:

```text
inventory -> atomize -> patternize -> document -> fixed point
```

through the existing recipe-first convergence DAG and explicit-file A3 materializer.

This mastery layer improves those existing recipes against generated hostile Java 21 projects rather
than repeatedly editing Nebula widget files.

## Mechanical oracle

For each fixture and recipe schedule:

```text
immutable Java source
  -> lossless OpenRewrite parse/print
  -> Atomize / Patternize schedule
  -> javac --release 21 -Xlint:all -Werror
  -> public/protected contract observation
  -> runtime probe comparison
  -> regex behavior comparison
  -> code-looking payload equality
  -> bounded multipass convergence
  -> second replay fixed point
```

Compiler, JUnit and runtime observations are authoritative. Regexes, static signals, donor evidence
and LLM proposals may nominate work but cannot certify a transformation.

## Hostile code-as-data

Fixtures contain Java-looking material that must remain data:

- comments with fake classes/returns/control flow;
- ordinary strings with fake methods;
- text blocks with fake source;
- regex strings containing Java-looking tokens and metacharacters.

The executable source also includes Java 21 local records, lambdas and switch expressions around the
same private arithmetic leaf used by the current atomizer/patternizer proof.

## Permutations

The bounded campaign uses six schedules:

```text
A
P
A -> P
P -> A
A -> P -> A
P -> A -> P
```

Repeated A/P steps intentionally revisit an already-achieved operation. All mixed schedules describe
the same operation set and must converge to byte-identical source.

## Corpus geometry

The first retained campaign is:

```text
2 lexical hostility modes
x 2 line endings
x 2 caller shapes
x 6 declaration orders
= 48 fixtures
```

With six schedules this produces 288 mechanically checked result rows.

## JNI boundary

Each generated fixture retains a native method declaration so public/native contract observation is
part of the lab. The native method is not invoked.

This does not establish JNI implementation equivalence. Any real native/JNI Nebula change still
requires the existing Java-before-JNI policy, source custody, compilation and native parity gates.

## Donor/category policy

Existing Nebula fast-search catalogue and ordered review remain authoritative:

```text
LeetCode -> HackerRank -> GeeksForGeeks
```

Challenge bodies are reference evidence only. No solution/editorial code is copied.

## Recipe-first delivery

The lab itself is installed by one reusable OpenRewrite recipe under the existing
`m3/recipe-first` module. The installer owns only lab/tooling/test paths, refuses occupied drift,
and becomes a fixed point once its reviewed resources are present.

No widget/product source path is an installer target.
