# Nebula exact historical source recovery (2026-11-02)

**STATUS: historical Git-object custody, NOT product activation.**

Previous Synexia audit `NEBULA_TIP_CONTENT.tsv`, pinned at `6f54420f3d716f6ec3ba480fb45514833a73128a`, marked 244 historical source versions as LOST. They represent 212 original-path/blob versions and 212 distinct Git blob objects. The exact old file contents have been restored as inert `.sourceblob` files under `objects/`; `INDEX.tsv` maps every original path and SHA.

This read-only preservation branch starts from Nebula master `c08f92fc093e87b1e8912ef0846d09a659084baf`. All active SWT/Nebula widgets, their Java interfaces, viewport state, event ordering, native renderer, JVM/JNI boundaries and tests remain unchanged.

The 2026-10-30 audit ran against an older master. Several once-absent source paths, including `Grid.java`, `TableCombo.java`, `GridVisibleRangeSupport.java` and `RowSparseBits.java`, are **already present** at the latest master. Do not replay them wholesale. Treat all archived versions as review candidates, verify the current head and intentional deletions, then execute original-source Synexia Maven/OpenRewrite recipes + JUnit/SWT/ABI/CI gates before any behavioral restoration. Public product targets must not house canonical recipe sources. No `master` ref was changed.
