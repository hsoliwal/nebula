/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.m3.generated.TransferProgressNativeSwt;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

/** Native SWT display-backed acceptance; RAP browser sessions are a separate gate. */
public final class TransferProgressNativeSwtTest {

    @Test
    public void determinateProgressUsesOneOriginalSwtWidgetAndNoInputListener() {
        Display display = new Display();
        try {
            Shell shell = new Shell(display);
            try {
                List<String> actions = new ArrayList<>();
                List<String> changes = new ArrayList<>();
                Composite content = TransferProgressNativeSwt.create(
                        shell, SWT.NONE, actions::add,
                        (id, value) -> changes.add(id + "=" + value));
                Control[] widgets = content.getChildren();

                assertEquals(3, widgets.length);
                assertTrue(widgets[0] instanceof Label);
                assertTrue(widgets[1] instanceof ProgressBar);
                assertTrue(widgets[2] instanceof Button);
                assertEquals("title", widgets[0].getData("m3.web.id"));
                assertEquals("upload", widgets[1].getData("m3.web.id"));
                assertEquals("cancel", widgets[2].getData("m3.web.id"));
                assertEquals("File transfer", ((Label) widgets[0]).getText());

                ProgressBar progress = (ProgressBar) widgets[1];
                assertEquals(0, progress.getMinimum());
                assertEquals(100, progress.getMaximum());
                assertEquals(65, progress.getSelection());
                assertTrue(changes.isEmpty());
                assertTrue(actions.isEmpty());

                ((Button) widgets[2]).notifyListeners(SWT.Selection, new Event());
                assertEquals(List.of("cancel"), actions);
                assertTrue(changes.isEmpty());
            } finally {
                shell.dispose();
            }
        } finally {
            display.dispose();
        }
    }
}
