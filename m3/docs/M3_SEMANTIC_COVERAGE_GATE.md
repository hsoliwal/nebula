<!-- SPDX-License-Identifier: EPL-2.0 -->
# M3 semantic atom/pattern/IOP coverage gate

The authoritative product build remains Eclipse Nebula's Maven/Tycho reactor. The independent
`m3/recipe-first` reactor owns reusable OpenRewrite mutation candidates and JUnit proof.

## Policy

For the scoped semantic recipe kernel, the `semantic-coverage` Maven profile now requires both:

- JaCoCo **LINE COVEREDRATIO ≥ 0.99**;
- JaCoCo **BRANCH COVEREDRATIO ≥ 0.99**.

Both are checked at Maven `verify`. This is a threshold, **not** a claim that current tests or the
entire Nebula repository have already achieved 99% coverage. The measured classes are the recipe
kernel configured in `m3/recipe-first/pom.xml`, not all SWT widgets.

## Recipe-first implementation

`NebulaM3SemanticCoverageGateRecipe` is the FILE-local XML-LST owner for the Maven policy change.
It accepts only an exact, SHA-256-pinned preimage of `m3/recipe-first/pom.xml`, or the exact
postimage as a fixed point. A modified Maven owner fails closed. Unrelated root POMs and all
widget Java sources remain untouched.

The recipe is registered in the opt-in `m3-semantic-branch-coverage-recipe` profile; no OpenRewrite
source-changing goal is bound to the normal Maven lifecycle.

The JUnit receipt `NebulaM3SemanticCoverageGateRecipeTest` proves pre/post bytes, structured XML
replay, fixed point, source-path fences, and stale-preimage rejection. The independent
`m3/ci/test_semantic_branch_coverage_gate.py` fixture rejects omitted/duplicate counters,
weakened ratios, unapproved class exclusions and implicit recipe mutation goals.

## Commands

From the Nebula repository root with Java 21 and Maven 3.9.11:

```bash
mvn -B -ntp -f m3/reactor.xml -pl recipe-first -am \
  -Dtest=NebulaM3SemanticCoverageGateRecipeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -B -ntp -f m3/reactor.xml -pl recipe-first -am \
  -Psemantic-coverage verify

python3 -m unittest discover -s m3/ci -p 'test_*.py' -v
```

The OpenRewrite source-changing candidate is opt-in and only appropriate for a checkout
matching the reviewed preimage. Never run it as an unconditional build lifecycle goal.

## Boundary with the full M3 programme

The proof ladder is unchanged:

```text
INVENTORY
 -> FILE atomization
 -> patternization and IOP role conformance
 -> Javadoc/documentation
 -> recipe fixed point
 -> diff/lint/compile/JUnit and scoped semantic coverage
 -> Nebula Tycho/SWT runtime proof
 -> cross-host portability when declared
 -> explicit scope promotion and downstream backport transfer
```

Camel schedules approved dependency waves, Airflow may project the same DAG, and Drools/KIE may
select eligible candidates. They do not establish semantic equivalence, grant mutation authority,
or promote code. Likewise, a 99% JaCoCo ratio is not proof of public-contract equivalence, JNI
parity, full jtreg coverage, or backport compatibility.

M3JDK21 continues to require its own Java-21/JEP/PR/tooling compatibility inventory and
OpenJDK build/jtreg evidence. Synexia remains the canonical reusable recipe owner; Nebula is the
bounded product proving target.

**Acceptance state:** CANDIDATE until exact Java-21 Maven/JUnit/JaCoCo and Nebula product
checks are recorded as passing. Do not record an absent or blocked CI job as a PASS.
