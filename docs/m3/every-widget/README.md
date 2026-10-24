# Every widget is a viewport (Nebula)

Owner's rule, 2026-10-24 (repeated): every widget must be a viewport. What "viewport" means per widget shape (aspects
R resident window, G logical geometry, D damage-only repaint, P retained planes, H hidden gating) is defined in the SWT
fork's `docs/m3/ALL_WIDGETS_VIEWPORT_PLAN.md` (sections 1, 2 and 5). This folder holds the Nebula side: the inventory of
every widget class, the waves that convert them, and the tools the waves use.

## Tools

- `tools/build_bundle.sh [--tests] <widget folder>...`: compiles the plugins of the given `widgets/<folder>` trees (nested
  folders such as `opal` included) plus every in-repository plugin they need through Require-Bundle or Import-Package,
  transitively, in one javac run against the 2026-09 platform jars of the local p2 pool; `--tests` adds the test plugins.
  Output `S:/m3-scratch/every-widget/out/<label>` (classes, javac.log, classpath.txt). Measured 2026-10-24: led 2 plugins
  (opal.commons through Import-Package), opal 25 plugins, cdatetime 4, visualization 2, m3 with tests 3: 0 errors each.
- `tools/run_bundle_tests.sh <label> <test class>...`: runs JUnit 4 or 5 classes of a `--tests` build on the hidden test
  desktop through one `ui_lock.sh` slot (heartbeat every 30 s while waiting), `MODE=viewport|original`, `SWT_JAR=<jar>`
  to put the M3 SWT fork first (part visibility events `SWT.PartVisible`/`SWT.PartHidden`). Smoke run: m3 tests 13 of 13.

## Inventory

The per-widget cards (shape, required and present aspects, evidence, next leaf), verified against the code, land here as
`LEDGER.tsv` when the inventory run of 2026-10-24 ends.
