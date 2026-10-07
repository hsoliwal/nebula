<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula M3 agent instructions

For reusable M3/OpenRewrite source transformations, hsoliwal/com.synexia is the canonical recipe
owner and Nebula is the proving/product target. Do not create a competing Nebula recipe when an
equivalent admitted Synexia recipe exists.

For FILE-local atomization/patternization/IOP/documentation, use the exact Synexia binding recorded
in m3/catalogue/pure-int-recipe-binding.tsv. The compatibility profile
m3-local-file-convergence invokes com.synexia.rewrite.M3PureIntConvergenceRecipe.
Nebula-local pure-int recipe classes remain historical proving fixtures only.

Preserve the canonical sequence:

INVENTORY -> ATOMIZATION -> PATTERNIZATION/IOP -> DOCUMENTATION -> FIXED_POINT.

Small recipes may be composed into the canonical content-addressed DAG. Camel may execute admitted
dependency waves, Airflow may schedule/export that same DAG, and Drools/KIE may perform candidate
admission. None receives semantic-equivalence, direct source-mutation, or promotion authority.

Nebula's original Maven/Tycho build and tests remain product authority. A Synexia recipe handoff is
candidate machinery only; Nebula must still prove compile/tests/runtime behavior and contract
preservation before promotion. Repeatable target defects feed back into the canonical Synexia
recipe/corpus before broad reapplication. Never rebase or rewrite useful proving history.
