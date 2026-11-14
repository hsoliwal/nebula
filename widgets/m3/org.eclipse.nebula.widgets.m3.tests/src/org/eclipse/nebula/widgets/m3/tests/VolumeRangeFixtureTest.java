/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** Original SWT display test, not an emulated web event or RAP browser session. */
public final class VolumeRangeFixtureTest {

    @Test
    public void boundedRangeUsesOneNativeScaleAndStableSelectionCallback() {
        Display existing = Display.getCurrent(); // one Display per thread: reuse the one an earlier test left
        Display display = existing != null ? existing : new Display();
        try {
            Shell shell = new Shell(display);
            try {
                List<String> changes = new ArrayList<>();
                List<String> actions = new ArrayList<>();
                Composite content = VolumeRangeFixture.create(
                        shell, SWT.NONE, actions::add,
                        (id, value) -> changes.add(id + "=" + value));
                Control[] widgets = content.getChildren();

                assertEquals(3, widgets.length);
                assertTrue(widgets[0] instanceof Label);
                assertTrue(widgets[1] instanceof Scale);
                assertTrue(widgets[2] instanceof Button);
                assertEquals("title", widgets[0].getData("m3.web.id"));
                assertEquals("volume", widgets[1].getData("m3.web.id"));
                assertEquals("apply", widgets[2].getData("m3.web.id"));
                assertEquals("Playback volume", ((Label) widgets[0]).getText());

                Scale scale = (Scale) widgets[1];
                assertEquals(0, scale.getMinimum());
                assertEquals(100, scale.getMaximum());
                assertEquals(42, scale.getSelection());
                assertEquals(1, scale.getIncrement());
                assertTrue(changes.isEmpty());
                assertTrue(actions.isEmpty());

                scale.setSelection(73);
                scale.notifyListeners(SWT.Selection, new Event());
                assertEquals(List.of("volume=73"), changes);
                assertTrue(actions.isEmpty());

                ((Button) widgets[2]).notifyListeners(SWT.Selection, new Event());
                assertEquals(List.of("apply"), actions);
                assertEquals(List.of("volume=73"), changes);
            } finally {
                shell.dispose();
            }
        } finally {
            if (existing == null) {
                display.dispose();
            }
        }
    }
}
