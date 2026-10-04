# Current master execution proof

Purpose: trigger the unchanged Nebula Java/Tycho and M3 recipe-first workflows against the
current default-branch source after the merged viewport/distillation and compatibility work.

This branch changes no production source and grants no promotion authority.

Expected gates:

1. Nebula-local OpenRewrite recipe crate compile/tests.
2. Whole Java inventory and fixed-point candidate materializer.
3. Original Tycho reactor under Xvfb.
4. Existing widget/runtime tests and screenshot evidence.
5. Transformed-candidate Tycho reactor.
6. Candidate-only/no-source-write custody.

If a gate fails, the failing untouched owner becomes the next serial recipe work item.
