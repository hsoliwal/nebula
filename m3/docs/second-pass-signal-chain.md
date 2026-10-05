# Nebula M3 second-pass signal-chain binding

Nebula reuses the canonical Synexia OpenRewrite atom/pattern signal chain. It does not fork or copy
that implementation.

## Exact reviewed Synexia snapshot

The current Nebula binding is source-bound to this reviewed snapshot:

| Evidence | Value |
| --- | --- |
| repository | `hsoliwal/com.synexia` |
| branch | `develop` |
| reviewed commit | `1cd108647f1eb1d5c288d917acbf7e8e635c8ce9` |
| integration lineage | PR #8925, integrating #8891 / #8897 / #8915 |
| integration PR head | `5f63a7a6a4541d055df5071de22d5edf9ee23c7a` |
| recipe class | `com.synexia.rewrite.M3AtomPatternSignalChainRecipe` |
| named recipe | `com.synexia.rewrite.M3AtomPatternSignalChain` |
| stable repository entry point | `com.synexia.rewrite.M3RepositoryAtomizePatternizeRecipe` |
| configured pass budget | 4 |
| required fixed-point passes | 3 |
| external recognizer-leaf fan-out | false |

The historical PR numbers are provenance. The exact current implementation is identified by the
reviewed commit plus Git blob identities in
`m3/catalogue/second-pass-recipe-binding.tsv`.

## Source custody

The binding pins these current Synexia blobs:

- signal-chain recipe:
  `975cddc7d5365c6483f56f5b9c551387c5af40eb`;
- named OpenRewrite recipe YAML:
  `1f06981c5b12373770df0f2bcecf0fc7b3fbdd5b`;
- repository atomize/patternize entry point:
  `83cf30e8243f8e4822e45909e77f3479a7b742c3`;
- Java lexical mask:
  `60a401e0ff6540b9d3e9f104ee5771a7a26e4683`;
- hostile/torture fixture:
  `5e18c97ec6d3762b1f9450d271a92e0012666d8c`;
- focused proof workflow:
  `8ee2012f765f077fb4d811ed053196e6ce049daa`.

`NebulaM3SecondPassBinding.requireCurrentBlobs(...)` fails closed if those reviewed owners drift.

## Execution path

The Nebula root profile `m3-atomize-patternize` activates the current stable Synexia repository
entry point:

`com.synexia.rewrite.M3RepositoryAtomizePatternizeRecipe`

The removed historical alias `M3NebulaAtomizePatternizeRecipe` is not used.

The repository recipe contains the read-only second-pass chain as:

`new M3AtomPatternSignalChainRecipe(sourceFilePattern, 4)`

The signal chain is a single OpenRewrite `ScanningRecipe` whose internal evidence path is:

```text
AST/LST inventory
  -> structural recognizer
  -> control-flow recognizer
  -> effect/contract recognizer
  -> length-preserving masked-regex recognizer
       \________________ parallel internal nomination ________________/
                              |
                              v
                   deterministic signal fan-in
                              |
                              v
                      pattern candidates
                              |
                              v
                   contract/risk typed residue
                              |
                              v
                  content-addressed full root
                              |
                              v
                    fixed-point recheck
```

The configured budget is four passes. The implementation requires three passes for fixed-point
evidence and fails closed if the configured budget is lower.

## Regex authority

Regex is a bounded nomination source only. `M3JavaLexicalMask` masks comments, strings, chars and
text blocks without changing source length or line terminators before lexical regex cues are
evaluated.

Structural/control/effect facts come from the OpenRewrite Java tree. A regex-only cue cannot certify
semantic equivalence or authorize a transformation. Fake code in the torture fixture remains
masked; real code outside literals/comments remains visible.

## Authority

This binding is read-only evidence:

- source mutation authority: false;
- semantic-equivalence authority: false;
- replacement authority: false;
- promotion authority: false.

The Nebula-local FILE convergence DAG remains a separate candidate mutation lane. It still requires
its own source custody, compiler/tests, fixed-point replay, original Tycho verification and manual
serial promotion.

## External orchestration locality

The recognizers are internal implementation stages of one OpenRewrite `ScanningRecipe`. Although
the Java implementation may use parallel streams internally, those recognizers are not independent
cross-process tasks.

Camel, Airflow, or another external scheduler may schedule the **whole repository recipe / signal
chain as one recipe atom**. External structural/control/effect/regex leaf fan-out remains forbidden
unless a future content-addressed handoff contract is explicitly implemented and proven. Drools/KIE
remains admission evidence only and receives no mutation or promotion authority.

## Donor and JNI boundary

Nebula's existing repository review remains ordered:

```text
LeetCode
  -> HackerRank
  -> GeeksforGeeks
  -> pinned GitHub donor + license/disposition
```

Challenge problem/editorial/solution bodies are reference-only. JNI/native work remains gated by:

```text
Java semantic oracle
  -> differential corpus
  -> lifecycle/fallback proof
  -> setup-inclusive benchmark
  -> bounded native candidate
  -> Java/JNI parity
```

No native implementation is admitted merely because a JNI path is possible.

## Running the review

Install the exact reviewed Synexia recipe artifact into the Maven repository used by Nebula, then:

```bash
mvn -B -ntp -Pm3-atomize-patternize rewrite:dryRunNoFork
```

The profile is opt-in; the ordinary Nebula Tycho reactor is unchanged when it is not selected.

## Verification truth

The machine binding is
`m3/catalogue/second-pass-recipe-binding.tsv`; the Java owner is
`NebulaM3SecondPassBinding`.

Hosted green proof is not inferred from merge state. The exact reviewed Synexia commit and the exact
Nebula PR head must still complete their focused Maven/JUnit/Tycho workflows successfully before
promotion evidence can be claimed.
