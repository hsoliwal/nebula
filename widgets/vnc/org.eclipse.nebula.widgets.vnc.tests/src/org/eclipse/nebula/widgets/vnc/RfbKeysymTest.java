/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.vnc;

import static org.junit.Assert.assertEquals;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Event;
import org.junit.Test;

/** SWT keyboard translation contracts for RFB 3.x KeyEvent keysyms. */
public final class RfbKeysymTest {

    @Test public void ctrlLetterNormalizesPlatformKeycodeCase() {
        assertEquals('a', VncCanvas.toKeysym(key('a', '\u0001', SWT.CTRL, SWT.NONE)));
        assertEquals('a', VncCanvas.toKeysym(key('A', '\u0001', SWT.CTRL, SWT.NONE)));
        assertEquals('A', VncCanvas.toKeysym(key('A', '\u0001',
                SWT.CTRL | SWT.SHIFT, SWT.NONE)));
        assertEquals('z', VncCanvas.toKeysym(key('Z', '\u001a', SWT.CTRL, SWT.NONE)));
    }

    @Test public void modifiersPreserveLeftAndRightKeysyms() {
        assertEquals(0xffe1, VncCanvas.toKeysym(key(SWT.SHIFT, '\0', 0, SWT.LEFT)));
        assertEquals(0xffe2, VncCanvas.toKeysym(key(SWT.SHIFT, '\0', 0, SWT.RIGHT)));
        assertEquals(0xffe3, VncCanvas.toKeysym(key(SWT.CTRL, '\0', 0, SWT.LEFT)));
        assertEquals(0xffe4, VncCanvas.toKeysym(key(SWT.CTRL, '\0', 0, SWT.RIGHT)));
        assertEquals(0xffe9, VncCanvas.toKeysym(key(SWT.ALT, '\0', 0, SWT.LEFT)));
        assertEquals(0xffea, VncCanvas.toKeysym(key(SWT.ALT, '\0', 0, SWT.RIGHT)));
        assertEquals(0xffeb, VncCanvas.toKeysym(key(SWT.COMMAND, '\0', 0, SWT.LEFT)));
        assertEquals(0xffec, VncCanvas.toKeysym(key(SWT.COMMAND, '\0', 0, SWT.RIGHT)));
    }

    @Test public void unicodeScalarValuesUseX11UnicodeKeysyms() {
        assertEquals(0xe9, VncCanvas.toKeysym(key(0, '\u00e9', 0, SWT.NONE)));
        assertEquals(0x010003a9, VncCanvas.toKeysym(key(0, '\u03a9', 0, SWT.NONE)));
        assertEquals(0x010020ac, VncCanvas.toKeysym(key(0, '\u20ac', 0, SWT.NONE)));
        assertEquals(0, VncCanvas.toKeysym(key(0, '\ud83d', 0, SWT.NONE)));
        assertEquals(0, VncCanvas.toKeysym(key(0, '\ude00', 0, SWT.NONE)));
    }

    @Test public void navigationUsesStandardX11SpecialKeys() {
        assertEquals(0xff51, VncCanvas.toKeysym(key(SWT.ARROW_LEFT, '\0', 0, SWT.NONE)));
        assertEquals(0xffc8, VncCanvas.toKeysym(key(SWT.F11, '\0', 0, SWT.NONE)));
        assertEquals(0xffff, VncCanvas.toKeysym(key(SWT.DEL, '\0', 0, SWT.NONE)));
        assertEquals(0, VncCanvas.toKeysym(key(0, '\0', 0, SWT.NONE)));
    }

    private static KeyEvent key(int keyCode, char character, int stateMask, int location) {
        Event event = new Event();
        event.keyCode = keyCode;
        event.character = character;
        event.stateMask = stateMask;
        event.keyLocation = location;
        return new KeyEvent(event);
    }
}
