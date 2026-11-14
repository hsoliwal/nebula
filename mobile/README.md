# Nebula Grid mobile viewport projection (candidate)

MobileGridViewport is an internal, array-backed row residency atom for a
future Android/iOS backend of the **original** Nebula Grid. It does not replace,
rename, wrap or alter the existing Grid widget public API. It supports long
logical row counts, fixed row geometry, O(viewport + overscan) retained physical
row slots, stable slot reuse, selection/dirty-region updates, native row release
and overflow-safe large-scroll arithmetic. A RowSink can bind real native views
once a production SWT mobile backend is available.

Run bash mobile/test-core.sh on Java 21 to compile and execute its assertions.
This proves its Java model/viewport invariants only. Native Grid painting,
columns, sorting, editing, accessibility, RTL, IME, touch and equivalent desktop
behavior on Android/iOS are **not** established; the SWT port and full Nebula
native adapter remain required. No upstream Grid files or widget contracts are
modified in this first PR.
