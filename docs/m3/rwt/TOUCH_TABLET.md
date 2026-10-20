<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others. -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Touch paradigms on RWT (the tablet case)

Behaviours catalogue (brief rule 29, `docs/m3/behaviours/BEHAVIOURS.md` on the integration branch): "Touch paradigms:
pull-to-refresh, swipe actions, long-press menus, pinch zoom (needed for 'Eclipse on a tablet')", owners custom-widgets
and RWT. On a tablet the toolkit is RWT in the tablet's browser, so this file records what RAP 4.8.0 delivers today,
read in its sources; rows are in `TOUCH_RWT.tsv`. Nothing here was run on a touch device.

## What RAP 4.8.0 does with touch

- The browser client translates touches into mouse events (`rwt/runtime/MobileWebkitSupport.js`, 589 lines):
  a tap becomes mousedown, mouseup and click (`_handleTouchEnd`, lines 293-324); two taps in a short time become a
  dblclick (`_isDoubleClick`, line 314). A drag on the row container of a Tree, Table or Grid scrolls it virtually
  (`_getSessionType` 326-349, `_initVirtualScroll` 353): only the rows that enter are requested, so touch scrolling is
  already a viewport delta. A drag on a scroll bar thumb or a column header becomes mousemove (`_draggableTypes`
  23-34, `_handleTouchMove` 277-280). Any other drag scrolls natively, if allowed, and reports no pointer movement.
- iOS gesture events (`gesturestart/change/end`, lines 145-147, 500-512) only switch mouse emulation off and on;
  they reach neither the server nor scripting.
- The server has SWT's `GestureEvent`, `TouchEvent`, `Control.addGestureListener`, `addTouchListener` and
  `setTouchEnabled` (`Control.java:2386-2478`), but no life-cycle adapter renders or reads them (no `Gesture` or
  `Touch` in any RWT LCA), so those listeners never fire.
- Client scripting (`ClientListener`) sees KeyDown/Up, MouseDown/Up/Move/Enter/Exit/Wheel/DoubleClick, Paint,
  FocusIn/Out, Show/Hide, Dispose, Selection, Modify, Verify and Resize (`rwt/scripting/EventBinding.js:112-257`).
  Because a touch drag on a Canvas or a plain Composite produces no MouseMove, a ClientListener cannot see a swipe,
  a pull or a long press either.

## The four paradigms

| paradigm | SWT contract | RWT 4.8.0 | route |
|---|---|---|---|
| long-press menu | on touch screens a press-and-hold opens the context menu (win32 WM_CONTEXTMENU, SWT.MenuDetect) | no long press: a held tap ends as a click | BLOCKED until the client emits MenuDetect on a timed hold |
| swipe actions (rows) | none in stock SWT (a behaviour over Tree/Table rows) | horizontal drags on a row container are virtual scrolling; nothing reaches the server | BLOCKED: needs a client-side gesture recogniser |
| pull-to-refresh | none in stock SWT | an over-scroll at the top is not reported | BLOCKED: same |
| pinch zoom | SWT.Gesture with GESTURE_MAGNIFY (Control.addGestureListener) | gesture events end in the client; server GestureListeners never fire | BLOCKED: same, plus rendering GestureEvent through the LCA |

## What unblocks it (decision needed)

All four need code in the browser that recognises the gesture and sends one committed event (MenuDetect, Selection
with a detail, a Gesture event) to the server: high-frequency touch moves must stay in the browser (same reasoning as
the 2026-10-13 RWT research: only committed state round-trips). Two ways, both outside "Java only for new code" as
written, so the user decides:

1. **GWT-compiled Java** (preferred, it keeps one Java source): the recogniser is Java in the shared layer, compiled by
   GWT and loaded through RAP's `JavaScriptLoader` with a small remote object per widget. Open technical question from
   2026-10-13: whether GWT output can be handed to RAP's client as a script; the SWT GWT gate proves only the model
   classes.
2. **A patch to RAP's client** (`MobileWebkitSupport.js`, a timer for long press and a two-finger tracker), sent
   upstream to RAP: JavaScript.

The server side (rendering `GestureEvent`, MenuDetect from touch) is Java in the RWT compat bundle once the client
sends something. Tap, double tap and touch scrolling of Tree/Table/Grid already work and are covered by the RAP_NATIVE
rows of `RWT_COLUMN.tsv`.
