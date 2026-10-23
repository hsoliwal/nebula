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
package org.eclipse.nebula.widgets.m3.rwt.examples;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/** WIZARD on RWT: RAP JFace WizardDialog with two pages, opened without blocking. */
public final class WizardExample extends RwtExample {

	public WizardExample () {
		super ("WIZARD", "org.eclipse.jface.wizard.WizardDialog (RAP JFace 4.8.0)");
	}

	@Override
	public Control create (Composite parent) {
		Button button = new Button (parent, SWT.PUSH);
		button.setText ("Wizard");
		Wizard wizard = new Wizard () {
			@Override
			public boolean performFinish () {
				return true;
			}
		};
		for (int i = 1; i <= 2; i++) wizard.addPage (page (i));
		WizardDialog dialog = new WizardDialog (parent.getShell (), wizard);
		dialog.setBlockOnOpen (false);
		dialog.open ();
		return button;
	}

	private static WizardPage page (int number) {
		return new WizardPage ("page" + number, "Step " + number, null) {
			@Override
			public void createControl (Composite parent) {
				Label label = new Label (parent, SWT.NONE);
				label.setText ("Content of step " + number);
				setControl (label);
			}
		};
	}

	@Override
	public void change (Control control) {
		((Button) control).setText ("Wizard again " + next ());
	}
}
