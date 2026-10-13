<!-- SPDX-License-Identifier: EPL-2.0 -->
# GWT gate for the Grid model package

Same pattern as the SWT viewport gate (SWT #117 `gwt-gate`): `run_gate.py` copies
`org.eclipse.nebula.widgets.grid.model` into a scratch GWT module, compiles it with GWT 2.12.2 in
`-strict` mode, runs `GateWorkload` (8 seeds: radix and comparator sorts, filter, pinned rows,
grouping with aggregates, dense and sparse lanes, patterns, packed selection) in the JVM and as the
generated JavaScript in Node, and fails unless every digest line is identical.

```text
python run_gate.py --nebula-root <repo> --gwt-lib <dir with gwt-dev, gwt-user and deps> --work <new dir> \
       [--java-home <JDK 17+>] [--node <node>]
```

What it found: `java.lang.Runtime` is not in GWT's JRE emulation (`HeapGrowth`). The heap sampling
moved to `HeapSampler`, which the module replaces with the super-source in `super/` (no heap
information, so growth doubles).

Last result (win32, JDK 21, GWT 2.12.2, Node 25): `GWT_GATE_OK 8 workloads identical in the JVM and
in GWT JavaScript; 12 model classes compiled by GWT`.
