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
import java.util.Set;
import org.eclipse.nebula.widgets.web.model.AnimvalueModel;
import org.eclipse.nebula.widgets.web.model.WebStates;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Uses an owned, unopened Shell and the real Display timer queue; no screen capture. */
public final class WebAnimationVisibilityTest {
 private Display display;
 private Shell shell;
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
 private Set<Object> resources() {
  Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
  Object[] objects = display.getDeviceData().objects;
  if (objects != null) for (Object object : objects) if (object != null) result.add(object);
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
  while (System.nanoTime() < until) {
   while (display.readAndDispatch()) { }
   Thread.sleep(1);
  }
 }
 private void openWidget(int mode) throws Exception {
  model = new AnimvalueModel("animation-proof", 0, mode, 0, 0);
  widget = new WebCanvas<>(shell, SWT.NONE, model);
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
  Set<Object> extra = resources();
  extra.removeAll(beforeResources);
  need(extra.isEmpty(), "UNEXPECTED_TRACKED_NATIVE_RESOURCE " + extra);
 }
 private int phase() { return model.snapshot()[8]; }
 private void startClock() throws Exception {
  model.load("spinning");
  need(model.isAnimating() && queued() == 1, "SWT_START_PRECONDITION");
  pump(80);
  need(phase() != 0 && queued() == 1, "ACTUAL_DISPLAY_CLOCK_PRECONDITION");
 }
 @Before public void setup() throws Exception {
  DeviceData data = new DeviceData();
  data.tracking = true;
  display = new Display(data);
  displaysCreated++;
  shell = new Shell(display);
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
}
