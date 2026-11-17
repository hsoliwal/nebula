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
package org.eclipse.nebula.widgets.opal.wizard;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * One step of a {@link Wizard}: a title for the steps list and the header, a description, the
 * content, and whether the step is complete.
 * <p>
 * The content is created the first time the step is shown. A step that is not complete keeps the
 * wizard from going to the next step and from finishing. {@link #leave(boolean)} may refuse to
 * leave, for example after validating the input, and say why with {@link #setErrorMessage(String)}.
 * {@link #getNextPage()} may choose the next step from the input; Back then returns along the steps
 * actually taken.
 * </p>
 */
public abstract class WizardPage {

	private final String title;
	private String description;
	private String errorMessage;
	private boolean complete = true;
	Wizard wizard;

	/**
	 * Constructs a step.
	 *
	 * @param title the title, shown in the steps list and in the header
	 */
	protected WizardPage(final String title) {
		this.title = title == null ? "" : title;
	}

	/**
	 * Creates the content of the step.
	 *
	 * @param parent the parent of the content
	 * @return the content
	 */
	public abstract Control createContent(Composite parent);

	/**
	 * Called when the step is shown.
	 */
	public void enter() {
	}

	/**
	 * Called before the step is left.
	 *
	 * @param forward <code>true</code> when going to the next step or finishing, <code>false</code> when going back
	 * @return <code>false</code> to stay on the step
	 */
	public boolean leave(final boolean forward) {
		return true;
	}

	/**
	 * Returns the step that follows this one; by default the next step of the wizard.
	 *
	 * @return the next step, or <code>null</code> when this is the last one
	 */
	public WizardPage getNextPage() {
		return wizard == null ? null : wizard.following(this);
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the description, or <code>null</code>
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description shown in the header, or <code>null</code>
	 */
	public void setDescription(final String description) {
		this.description = description;
		changed();
	}

	/**
	 * @return the error message, or <code>null</code>
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage the message shown under the content, or <code>null</code> to remove it
	 */
	public void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
		changed();
	}

	/**
	 * @return whether the step is complete
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * @param complete whether the step is complete
	 */
	public void setComplete(final boolean complete) {
		if (this.complete == complete) {
			return;
		}
		this.complete = complete;
		changed();
	}

	/**
	 * @return the wizard of the step, or <code>null</code> before it is added to one
	 */
	public Wizard getWizard() {
		return wizard;
	}

	private void changed() {
		if (wizard != null) {
			wizard.fireChanged();
		}
	}
}
