/****************************************************************************
 * Copyright (c) 2008 Jeremy Dowdall
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Jeremy Dowdall <jeremyd@aspencloud.com> - initial API and implementation
 *****************************************************************************/

package org.eclipse.nebula.cwt.v;

import org.eclipse.nebula.cwt.test.AbstractVTestCase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;



public class ButtonFocusTests extends AbstractVTestCase {

	private Button button1;
	private Button button2;
	private VButton vbutton1;
	private VButton vbutton2;

	public void setUp1() throws Exception {
		Shell shell = getShell();
		shell.setLayout(new GridLayout(2, true));

		// row 1
		button1 = new Button(shell, SWT.PUSH);
		button1.setText("B1");
		button1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		VCanvas c1 = new VCanvas(shell, SWT.NONE);
		c1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		c1.getPanel().setLayout(new VSimpleLayout());
		
		vbutton1 = new VButton(c1.getPanel(), SWT.PUSH);
		vbutton1.setText("VB1");

		// row 2
		button2 = new Button(shell, SWT.PUSH);
		button2.setText("B2");
		button2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		VCanvas c2 = new VCanvas(shell, SWT.NONE);
		c2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		c2.getPanel().setLayout(new VSimpleLayout());

		vbutton2 = new VButton(c2.getPanel(), SWT.PUSH);
		vbutton2.setText("VB2");
	}
	
	public void setUp2() throws Exception {
		Shell shell = getShell();
		shell.setLayout(new GridLayout(2, false));

		// row 1
		VCanvas c1 = new VCanvas(shell, SWT.NONE);
		c1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		c1.getPanel().setLayout(new VSimpleLayout());
		
		vbutton1 = new VButton(c1.getPanel(), SWT.PUSH);
		vbutton1.setText("VB1");

		button1 = new Button(shell, SWT.PUSH);
		button1.setText("B1");
		button1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// row 2
		VCanvas c2 = new VCanvas(shell, SWT.NONE);
		c2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		c2.getPanel().setLayout(new VSimpleLayout());

		vbutton2 = new VButton(c2.getPanel(), SWT.PUSH);
		vbutton2.setText("VB2");

		button2 = new Button(shell, SWT.PUSH);
		button2.setText("B2");
		button2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	public void setUp3() throws Exception {
		Shell shell = getShell();
		shell.setLayout(new GridLayout(2, false));

		VCanvas c1 = new VCanvas(shell, SWT.NONE);
		c1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		c1.getPanel().setLayout(new VGridLayout(2, true));
		
		vbutton1 = new VButton(c1.getPanel(), SWT.PUSH);
		vbutton1.setText("VB1");

		vbutton2 = new VButton(c1.getPanel(), SWT.PUSH);
		vbutton2.setText("VB2");

		moveTo(10, 10);
		move(5, 5);
	}
	
	/* the same check as assertTrue(hasFocus(expected)), reporting where the focus is instead */
	private void assertFocus(Button expected) {
		boolean focused = hasFocus(expected);
		assertTrue("focus on " + expected.getText() + ", but " + describeFocus(), focused);
	}

	private void assertFocus(VButton expected) {
		boolean focused = hasFocus(expected);
		assertTrue("focus on " + expected.getText() + ", but " + describeFocus(), focused);
	}

	private String describeFocus() {
		Control control = getFocusControl();
		VControl vcontrol = VTracker.getFocusControl();
		return "the display's focus control is " + name(control) + " and the virtual focus control is "
				+ (vcontrol instanceof VButton ? ((VButton) vcontrol).getText() : String.valueOf(vcontrol));
	}

	private static String name(Control control) {
		return control instanceof Button ? ((Button) control).getText() : String.valueOf(control);
	}

	public void testMouse_1() {
		assertFocus(button1);

		click(vbutton1);
		assertFocus(vbutton1);

		click(button2);
		assertFocus(button2);

		click(vbutton2);
		assertFocus(vbutton2);
		
		click(button1);
		assertFocus(button1);
	}
	
	public void testMouse_3() {
		assertFocus(vbutton1);

		click(vbutton2);
		assertFocus(vbutton2);
		assertFalse(hasFocus(vbutton1));

		click(vbutton1);
		assertFocus(vbutton1);
		assertFalse(hasFocus(vbutton2));
	}
	
	public void testTabKeyForward_1() {
		assertFocus(button1);

		keyPress('\t');
		assertFocus(vbutton1);

		keyPress('\t');
		assertFocus(button2);

		keyPress('\t');
		assertFocus(vbutton2);
		
		keyPress('\t');
		assertFocus(button1);
	}

	public void testTabKeyReverse_1() {
		assertFocus(button1);

		keyDown(SWT.SHIFT);
		
		keyPress('\t');
		assertFocus(vbutton2);

		keyPress('\t');
		assertFocus(button2);

		keyPress('\t');
		assertFocus(vbutton1);
		
		keyPress('\t');
		assertFocus(button1);

		keyUp(SWT.SHIFT);
	}

	public void testTabKeyForward_2() {
		assertFocus(vbutton1);

		keyPress('\t');
		assertFocus(button1);

		keyPress('\t');
		assertFocus(vbutton2);

		keyPress('\t');
		assertFocus(button2);
		
		keyPress('\t');
		assertFocus(vbutton1);
	}

	public void testTabKeyReverse_2() {
		// setUp2's tab order is vbutton1, button1, vbutton2, button2: Shift+Tab walks it backwards,
		// as testTabKeyReverse_1 does for setUp1 (this case repeated testTabKeyForward_2's order)
		assertFocus(vbutton1);

		keyDown(SWT.SHIFT);
		
		keyPress('\t');
		assertFocus(button2);

		keyPress('\t');
		assertFocus(vbutton2);

		keyPress('\t');
		assertFocus(button1);
		
		keyPress('\t');
		assertFocus(vbutton1);

		keyUp(SWT.SHIFT);
	}

}
