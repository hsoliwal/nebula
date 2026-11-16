/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.web;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.nebula.widgets.web.model.AnimvalueModel;
import org.eclipse.nebula.widgets.web.model.WebStates;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Uses an owned, opened Shell and the real Display timer queue; no screen capture.
 * The seven timer assertion intents supersede the historical unopened-host fixture. */
public final class WebAnimationVisibilityTest {
 private Display display;
 private Shell shell;
 private Composite parent;
 private final IdentityHashMap<Font, Integer> displayOwnedFonts = new IdentityHashMap<>();
 private java.util.Map<?, ?> providerRegistries;
 private AnimvalueModel model;
 private WebCanvas<AnimvalueModel> widget;
 private Runnable ticker;
 private Set<Object> beforeResources;
 private static int widgetsCreated, widgetsClosed, displaysCreated, displaysClosed, forcedCancellations;

 private static void need(boolean condition, String message) {
  if (!condition) throw new AssertionError(message);
 }
 private static Object field(Object owner, Class<?> type, String name) throws Exception {
  Field field = type.getDeclaredField(name);
  field.setAccessible(true);
  return field.get(owner);
 }
 private static Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 private int requireDisplayOwnedFont(org.eclipse.swt.graphics.Font font)throws Exception{
  Field root=Class.forName("org.eclipse.swt.internal.SWTFontProvider").getDeclaredField("fontRegistries");root.setAccessible(true);providerRegistries=(Map<?,?>)root.get(null);Object registry=providerRegistries.get(display);need(registry!=null&&registry.getClass().getName().equals("org.eclipse.swt.internal.ScalingSWTFontRegistry"),"UNKNOWN_FONT_REGISTRY");need(field(registry,"device")==display,"FOREIGN_FONT_REGISTRY");
  Map<?,?> containers=(Map<?,?>)field(registry,"fontsKeyMap");
  for(Object c:containers.values())if(c.getClass().getName().equals("org.eclipse.swt.internal.ScalingSWTFontRegistry$ScaledCustomFontContainer")){
   Class<?> parent=c.getClass().getSuperclass();need(parent.getName().equals("org.eclipse.swt.internal.ScalingSWTFontRegistry$ScaledFontContainer"),"UNKNOWN_FONT_CONTAINER");Field f=parent.getDeclaredField("scaledFonts");f.setAccessible(true);Map<?,?> fonts=(Map<?,?>)f.get(c);for(var e:fonts.entrySet())if(e.getValue()==font){need(e.getKey() instanceof Integer,"FONT_ZOOM_TYPE");return (Integer)e.getKey();}
  }
  throw new AssertionError("UNOWNED_TRACKED_FONT "+font);
 }

 private Set<Object> resources() {
  Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
  Object[] objects = display.getDeviceData().objects;
  if (objects != null) for (Object object : objects)
   if (object instanceof Resource resource && !resource.isDisposed() && !(object instanceof Color)) result.add(object);
  return result;
 }
 private int queued() throws Exception {
  Runnable[] callbacks = (Runnable[]) field(display, Display.class, "timerList");
  int count = 0;
  if (callbacks != null) for (Runnable callback : callbacks) if (callback == ticker) count++;
  return count;
 }
 private void pump(int millis) throws Exception {
  long until = System.nanoTime() + millis * 1_000_000L;
  while (!display.isDisposed() && System.nanoTime() < until) {
   while (!display.isDisposed() && display.readAndDispatch()) { }
   Thread.sleep(1);
  }
 }
 private void openWidget(int mode) throws Exception {
  openModel(new AnimvalueModel("animation-proof", 0, mode, 0, 0));
 }
 private void openModel(AnimvalueModel supplied) throws Exception {
  model = supplied;
  widget = new WebCanvas<>(parent, SWT.NONE, model);
  widgetsCreated++;
  widget.setBounds(0, 0, 360, 240);
  widget.setPartVisible(true);
  ticker = (Runnable) field(widget, WebCanvas.class, "ticker");
 }
 private void closeWidget() throws Exception {
  if (widget == null) return;
  try {
   if (!widget.isDisposed()) widget.dispose();
   boolean retained = queued() != 0;
   if (retained) { forcedCancellations++; display.timerExec(-1, ticker); }
   need(!retained, "OWNED_SWT_TICKER_RETAINED");
  } finally {
   widgetsClosed++;
   widget = null;
   model = null;
   ticker = null;
  }
  if (display.isDisposed()) return;
  Set<Object> extra = resources();
  extra.removeAll(beforeResources);
  for (Object value : extra) {
   need(value instanceof Font, "UNCLASSIFIED_TRACKED_RESOURCE " + value.getClass().getName());
   Font font = (Font) value;
   displayOwnedFonts.put(font, requireDisplayOwnedFont(font));
  }
 }
 private int phase() { return model.snapshot()[8]; }
 private void startClock() throws Exception {
  model.load("spinning");
  need(model.isAnimating() && queued() == 1, "SWT_START_PRECONDITION");
  pump(80);
  need(phase() != 0 && queued() == 1, "ACTUAL_DISPLAY_CLOCK_PRECONDITION");
 }
 @Before public void setup() throws Exception {
  org.junit.Assume.assumeTrue("Pinned Win32 SWT4974 native clock/private-resource introspection; other runtimes are not qualified.", "win32".equals(SWT.getPlatform()) && SWT.getVersion() == 4974);
  DeviceData data = new DeviceData();
  data.tracking = true;
  display = new Display(data);
  displaysCreated++;
  display.getSystemFont();
  display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
  shell = new Shell(display);
  shell.setText("Synexia opened animation clock controls");
  var trim = shell.computeTrim(0, 0, 360, 240);
  shell.setSize(trim.width, trim.height);
  shell.setLocation(80, 80);
  parent = new Composite(shell, SWT.NONE);
  parent.setBounds(0, 0, 360, 240);
  shell.open();
  shell.forceActive();
  pump(80);
  need(shell.isVisible() && display.getActiveShell() == shell, "OWNED_OPEN_ACTIVE_SHELL_REQUIRED");
  beforeResources = resources();
  openWidget(AnimvalueModel.SPINNER);
 }
 @After public void close() throws Exception {
  try { closeWidget(); }
  finally {
   try { if (shell != null && !shell.isDisposed()) shell.dispose(); }
   finally {
    if (display != null && !display.isDisposed()) { display.dispose(); displaysClosed++; }
    need(display == null || display.isDisposed(), "OWNED_DISPLAY_RETAINED");
    for (Font font : displayOwnedFonts.keySet()) need(font.isDisposed(), "DISPLAY_FONT_NOT_RELEASED");
    if (providerRegistries != null) need(!providerRegistries.containsKey(display), "OWNED_DISPLAY_REGISTRY_RETAINED");
   }
  }
 }
 @Test public void visibleClockAdvances() throws Exception {
  startClock();
  int before = phase();
  pump(80);
  need(phase() != before, "VISIBLE_SWT_CLOCK_STALLED");
 }
 @Test public void hiddenClockStable() throws Exception {
  startClock();
  widget.setPartVisible(false);
  int before = phase();
  pump(80);
  need(phase() == before && queued() == 0, "HIDDEN_SWT_PHASE_CHANGED");
 }
 @Test public void idleHiddenIntervalResumes() throws Exception {
  startClock();
  widget.setPartVisible(false);
  pump(80);
  int before = phase();
  need(!(boolean) field(widget, WebCanvas.class, "pending") && queued() == 0,
    "IDLE_SWT_PENDING_PRECONDITION");
  widget.setPartVisible(true);
  pump(80);
  need(phase() != before, "IDLE_SWT_RESUME_NO_PHASE_ADVANCE");
 }
 @Test public void pendingHiddenDeltaResumes() throws Exception {
  startClock();
  widget.setPartVisible(false);
  pump(80);
  int before = phase();
  model.setState(-1, WebStates.ERROR, true);
  need((boolean) field(widget, WebCanvas.class, "pending"), "HIDDEN_SWT_DELTA_PRECONDITION");
  widget.setPartVisible(true);
  pump(80);
  need(phase() != before, "PENDING_SWT_RESUME_STALLED");
 }
 @Test public void nonAnimatingNeverStarts() throws Exception {
  closeWidget();
  openWidget(AnimvalueModel.TRANSITION);
  model.load("transition");
  widget.setPartVisible(false);
  widget.setPartVisible(true);
  need(!model.isAnimating() && queued() == 0, "IDLE_SWT_STARTED");
  pump(80);
  need(phase() == 0, "IDLE_SWT_PHASE_CHANGED");
 }
 @Test public void repeatedVisibilityUsesOneRunnable() throws Exception {
  startClock();
  Runnable original = ticker;
  for (int i = 0; i < 20; i++) widget.setPartVisible(true);
  need(field(widget, WebCanvas.class, "ticker") == original && queued() == 1,
    "SWT_TIMER_IDENTITY_OR_COUNT");
 }
 @Test public void disposeCancelsExactQueuedTickerBeforePump() throws Exception {
  model.load("spinning");
  need(queued() == 1, "SWT_DISPOSE_QUEUE_PRECONDITION");
  int before = phase();
  widget.dispose();
  need(queued() == 0, "DISPOSED_SWT_TICKER_RETAINED");
  pump(80);
  need(phase() == before, "DISPOSED_SWT_MODEL_ADVANCED");
 }

 private static final class CallbackModel extends AnimvalueModel {
  Runnable next;
  CallbackModel() { super("clock-callback-proof", 0, SPINNER, 0, 0); }
  @Override protected boolean advance(int ms) {
   boolean again = super.advance(ms);
   Runnable action = next;
   next = null;
   if (action != null) action.run();
   return again;
  }
  void pulse() { animate(); }
 }
 private void stableHidden(String label) throws Exception {
  int before = phase();
  pump(100);
  need(phase() == before && queued() == 0, label);
 }
 @Test public void ancestorHiddenStopsAndShowResumes() throws Exception {
  startClock();
  parent.setVisible(false);
  need(widget.getVisible() && !widget.isVisible() && widget.isPartVisible(), "PARENT_ONLY_HIDE_STATE");
  stableHidden("ANCESTOR_HIDDEN_CLOCK_ADVANCED");
  int before = phase();
  parent.setVisible(true);
  pump(100);
  need(phase() != before, "ANCESTOR_SHOW_NO_MODEL_MUTATION_STALLED");
 }
 @Test public void initiallyHiddenParentFirstShowStarts() throws Exception {
  closeWidget();
  parent.setVisible(false);
  openWidget(AnimvalueModel.SPINNER);
  model.load("spinning");
  stableHidden("INITIALLY_HIDDEN_CLOCK_STARTED");
  int before = phase();
  parent.setVisible(true);
  pump(100);
  need(phase() != before, "FIRST_NATIVE_PARENT_SHOW_STALLED");
 }
 @Test public void ancestorHiddenDeltaDoesNotRestart() throws Exception {
  startClock();
  parent.setVisible(false);
  model.setState(-1, WebStates.ERROR, true);
  stableHidden("ANCESTOR_HIDDEN_DELTA_RESTARTED_CLOCK");
  int before = phase();
  parent.setVisible(true);
  pump(100);
  need(phase() != before, "ANCESTOR_DELTA_REVEAL_STALLED");
 }
 @Test public void zeroAreaPausesAndPositiveResizeResumes() throws Exception {
  startClock();
  for (int[] size : new int[][] { { 0, 240 }, { 360, 0 } }) {
   widget.setSize(size[0], size[1]);
   stableHidden("ZERO_AREA_CLOCK_ADVANCED");
   int before = phase();
   widget.setSize(360, 240);
   pump(100);
   need(phase() != before, "POSITIVE_RESIZE_CLOCK_STALLED");
  }
 }
 @Test public void nativeMinimizeAndDeiconifyResume() throws Exception {
  startClock();
  shell.setMinimized(true);
  need(shell.getMinimized(), "NATIVE_MINIMIZED_PRECONDITION");
  stableHidden("MINIMIZED_CLOCK_ADVANCED");
  int before = phase();
  shell.setMinimized(false);
  shell.forceActive();
  pump(100);
  need(!shell.getMinimized() && phase() != before, "NATIVE_DEICONIFY_CLOCK_STALLED");
 }
 @Test public void sameZoomReparentWakesAndIgnoresOldAncestor() throws Exception {
  startClock();
  Composite hidden = new Composite(shell, SWT.NONE);
  hidden.setBounds(0, 0, 360, 240);
  hidden.setVisible(false);
  int[] events = new int[2];
  Listener resize = event -> events[0]++;
  Listener paint = event -> events[1]++;
  widget.addListener(SWT.Resize, resize);
  widget.addListener(SWT.Paint, paint);
  try {
   var extent = widget.getBounds();
   need(widget.setParent(hidden), "NATIVE_REPARENT_HIDDEN_REFUSED");
   need(widget.getBounds().equals(extent) && !widget.isVisible(), "SAME_EXTENT_HIDDEN_REPARENT");
   stableHidden("HIDDEN_REPARENT_CLOCK_ADVANCED");
   int before = phase();
   int resizeBefore = events[0], paintBefore = events[1];
   need(widget.setParent(parent), "NATIVE_REPARENT_VISIBLE_REFUSED");
   pump(100);
   System.out.println("CLOCK_REPARENT_WAKE resizeDelta=" + (events[0] - resizeBefore) +
     " paintDelta=" + (events[1] - paintBefore) + " phaseAdvanced=" + (phase() != before));
   need(widget.getBounds().equals(extent) && widget.isVisible(), "SAME_EXTENT_VISIBLE_REPARENT");
   need(phase() != before, "SAME_ZOOM_REPARENT_VISIBLE_CLOCK_STALLED");
   int current = phase();
   hidden.setVisible(true);
   hidden.setVisible(false);
   pump(100);
   need(phase() != current, "OLD_ANCESTOR_EVENT_PARKED_CURRENT_CLOCK");
  } finally {
   if (!widget.isDisposed()) {
    widget.removeListener(SWT.Resize, resize);
    widget.removeListener(SWT.Paint, paint);
   }
   if (!hidden.isDisposed()) hidden.dispose();
  }
 }
 private CallbackModel callbackModel() throws Exception {
  closeWidget();
  CallbackModel callbacks = new CallbackModel();
  openModel(callbacks);
  startClock();
  return callbacks;
 }
 @Test public void midTickAncestorHideCannotResurrectTicker() throws Exception {
  CallbackModel callbacks = callbackModel();
  callbacks.next = () -> parent.setVisible(false);
  pump(80);
  need(!parent.getVisible(), "MID_TICK_PARENT_ACTION_NOT_RUN");
  stableHidden("MID_TICK_ANCESTOR_HIDE_RESURRECTED");
 }
 @Test public void midTickLogicalHideCannotResurrectTicker() throws Exception {
  CallbackModel callbacks = callbackModel();
  callbacks.next = () -> widget.setPartVisible(false);
  pump(80);
  need(!widget.isPartVisible(), "MID_TICK_LOGICAL_ACTION_NOT_RUN");
  stableHidden("MID_TICK_LOGICAL_HIDE_RESURRECTED");
 }
 @Test public void midTickWidgetDisposeCannotResurrectTicker() throws Exception {
  CallbackModel callbacks = callbackModel();
  callbacks.next = widget::dispose;
  pump(80);
  int before = phase();
  need(widget.isDisposed() && queued() == 0, "MID_TICK_DISPOSE_TICKER_RETAINED");
  pump(80);
  need(phase() == before, "MID_TICK_DISPOSE_MODEL_ADVANCED");
 }
 @Test public void midTickDisplayDisposeCannotResurrectTicker() throws Exception {
  CallbackModel callbacks = callbackModel();
  Set<Object> owned = resources();
  callbacks.next = display::dispose;
  pump(80);
  need(display.isDisposed() && widget.isDisposed() && queued() == 0, "MID_TICK_DISPLAY_DISPOSE_TICKER_RETAINED");
  for (Object value : owned) need(((Resource) value).isDisposed(), "DISPLAY_DISPOSE_TRACKED_RESOURCE_RETAINED");
 }
 @Test public void explicitUnopenedPaintDoesNotGrantClock() throws Exception {
  closeWidget();
  shell.dispose();
  shell = new Shell(display);
  parent = new Composite(shell, SWT.NONE);
  parent.setBounds(0, 0, 360, 240);
  openWidget(AnimvalueModel.SPINNER);
  need(!shell.isVisible() && !widget.isVisible(), "TRULY_UNOPENED_HOST_REQUIRED");
  model.load("spinning");
  Image image = new Image(display, 360, 240);
  GC gc = new GC(image);
  try {
   gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
   gc.fillRectangle(0, 0, 360, 240);
   Event event = new Event();
   event.gc = gc;
   event.width = 360;
   event.height = 240;
   widget.notifyListeners(SWT.Paint, event);
  } finally { gc.dispose(); }
  try {
   var data = image.getImageData();
   int sentinel = data.palette.getPixel(new org.eclipse.swt.graphics.RGB(255, 0, 0)), changed = 0;
   int rgb = data.palette.redMask | data.palette.greenMask | data.palette.blueMask;
   for (int y = 0; y < 240; y++) for (int x = 0; x < 360; x++)
    if ((data.getPixel(x, y) & rgb) != sentinel) changed++;
   need(changed > 0, "EXPLICIT_UNOPENED_PAINT_DID_NOT_RENDER");
   stableHidden("EXPLICIT_UNOPENED_PAINT_GRANTED_CLOCK");
  } finally { image.dispose(); }
 }

 @Test public void sameZoomCrossShellReparentWakes() throws Exception {
  startClock();
  Shell other = new Shell(display);
  var trim = other.computeTrim(0, 0, 360, 240);
  other.setSize(trim.width, trim.height);
  other.setLocation(100, 100);
  Composite target = new Composite(other, SWT.NONE);
  target.setBounds(0, 0, 360, 240);
  int[] events = new int[2];
  Listener resize = event -> events[0]++;
  Listener paint = event -> events[1]++;
  widget.addListener(SWT.Resize, resize);
  widget.addListener(SWT.Paint, paint);
  try {
   other.open();
   pump(40);
   other.setVisible(false);
   var extent = widget.getBounds();
   need(widget.setParent(target), "NATIVE_CROSS_SHELL_HIDDEN_REPARENT_REFUSED");
   need(widget.getBounds().equals(extent) && !widget.isVisible(), "CROSS_SHELL_HIDDEN_EXTENT");
   stableHidden("CROSS_SHELL_HIDDEN_CLOCK_ADVANCED");
   int before = phase(), resizeBefore = events[0], paintBefore = events[1];
   need(widget.setParent(parent), "NATIVE_CROSS_SHELL_VISIBLE_REPARENT_REFUSED");
   shell.forceActive();
   pump(100);
   System.out.println("CLOCK_CROSS_SHELL_REPARENT_WAKE resizeDelta=" + (events[0] - resizeBefore) +
     " paintDelta=" + (events[1] - paintBefore) + " phaseAdvanced=" + (phase() != before));
   need(widget.getBounds().equals(extent) && widget.isVisible(), "CROSS_SHELL_VISIBLE_EXTENT");
   need(phase() != before, "CROSS_SHELL_VISIBLE_CLOCK_STALLED");
  } finally {
   if (!widget.isDisposed()) {
    widget.removeListener(SWT.Resize, resize);
    widget.removeListener(SWT.Paint, paint);
   }
   if (!other.isDisposed()) other.dispose();
  }
 }
}
