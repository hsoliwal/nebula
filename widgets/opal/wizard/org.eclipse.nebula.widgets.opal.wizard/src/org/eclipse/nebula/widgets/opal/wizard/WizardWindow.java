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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.nebula.widgets.opal.commons.ResourceManager;
import org.eclipse.nebula.widgets.opal.header.Header;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * The window of a {@link Wizard}: a header with the current step's title and description, the
 * list of steps on the left with the current one in bold (as NetBeans wizards show it), the
 * current step's content, its error message, and the Back, Next, Finish and Cancel buttons.
 * <p>
 * The buttons follow the wizard's rules; the default button is Next while it is enabled, else
 * Finish. Closing the window cancels the wizard. A step's content is created the first time the
 * step is shown and kept until the window closes.
 * </p>
 */
public class WizardWindow {

	/** The Back button, for {@link #getButton(int)}. */
	public static final int BACK = 0;
	/** The Next button. */
	public static final int NEXT = 1;
	/** The Finish button. */
	public static final int FINISH = 2;
	/** The Cancel button. */
	public static final int CANCEL = 3;

	private static final ResourceBundle LABELS = ResourceBundle.getBundle("org.eclipse.nebula.widgets.opal.wizard.resources.wizard");

	private final Shell parent;
	private final Wizard wizard;
	private final Runnable update = this::update;
	private final Map<WizardPage, Control> contents = new IdentityHashMap<>();
	private final List<Label> steps = new ArrayList<>();
	private final Button[] buttons = new Button[4];
	private Shell shell;
	private Header header;
	private Composite pageArea;
	private Label error;
	private Font bold;
	private Image image;

	/**
	 * Constructs the window of a wizard.
	 *
	 * @param parent the parent shell, or <code>null</code>
	 * @param wizard the wizard
	 */
	public WizardWindow(final Shell parent, final Wizard wizard) {
		if (wizard == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		this.parent = parent;
		this.wizard = wizard;
	}

	/**
	 * @param image the image of the header, or <code>null</code>
	 */
	public void setImage(final Image image) {
		this.image = image;
		if (header != null && !header.isDisposed()) {
			header.setImage(image);
		}
	}

	/**
	 * Creates the window without opening it.
	 *
	 * @return the window's shell
	 */
	public Shell create() {
		shell = parent == null ? new Shell(Display.getCurrent(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL)
				: new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText(wizard.getTitle());
		shell.setLayout(new GridLayout(2, false));
		header = new Header(shell, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		header.setImage(image);
		createSteps();
		pageArea = new Composite(shell, SWT.NONE);
		pageArea.setLayout(new StackLayout());
		pageArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		error = new Label(shell, SWT.WRAP);
		error.setForeground(shell.getDisplay().getSystemColor(SWT.COLOR_RED));
		error.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		createButtons();
		wizard.addChangeListener(update);
		shell.addListener(SWT.Close, e -> wizard.cancel());
		shell.addListener(SWT.Dispose, e -> disposed());
		update();
		return shell;
	}

	private void createSteps() {
		final Composite list = new Composite(shell, SWT.NONE);
		list.setLayout(new GridLayout());
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		final FontData[] data = shell.getFont().getFontData();
		for (final FontData each : data) {
			each.setStyle(each.getStyle() | SWT.BOLD);
		}
		bold = new Font(shell.getDisplay(), data);
		final Label title = new Label(list, SWT.NONE);
		title.setText(LABELS.getString("steps"));
		title.setFont(bold);
		final List<WizardPage> pages = wizard.getPages();
		for (int i = 0; i < pages.size(); i++) {
			final Label step = new Label(list, SWT.NONE);
			step.setText(i + 1 + ". " + pages.get(i).getTitle());
			steps.add(step);
		}
		list.setVisible(pages.size() > 1);
		((GridData) list.getLayoutData()).exclude = pages.size() <= 1;
	}

	private void createButtons() {
		final Composite bar = new Composite(shell, SWT.NONE);
		final RowLayout layout = new RowLayout();
		layout.pack = false;
		bar.setLayout(layout);
		bar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 2, 1));
		final String[] texts = { LABELS.getString("back"), LABELS.getString("next"), LABELS.getString("finish"), ResourceManager.getLabel(ResourceManager.CANCEL) };
		final Runnable[] actions = { wizard::back, wizard::next, wizard::finish, wizard::cancel };
		for (int i = 0; i < buttons.length; i++) {
			final Runnable action = actions[i];
			buttons[i] = new Button(bar, SWT.PUSH);
			buttons[i].setText(texts[i]);
			buttons[i].addListener(SWT.Selection, e -> action.run());
		}
	}

	/**
	 * Creates the window if needed, opens it and runs the event loop until it closes.
	 *
	 * @return <code>true</code> if the wizard finished, <code>false</code> if it was cancelled
	 */
	public boolean open() {
		if (shell == null || shell.isDisposed()) {
			create();
		}
		shell.pack();
		final Point size = shell.getSize();
		final Rectangle area = (parent == null ? shell.getDisplay().getPrimaryMonitor() : parent.getMonitor()).getClientArea();
		shell.setLocation(area.x + (area.width - size.x) / 2, area.y + (area.height - size.y) / 2);
		shell.open();
		final Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return wizard.isFinished();
	}

	/**
	 * @return the window's shell, or <code>null</code> before {@link #create()}
	 */
	public Shell getShell() {
		return shell;
	}

	/**
	 * @param which {@link #BACK}, {@link #NEXT}, {@link #FINISH} or {@link #CANCEL}
	 * @return the button, or <code>null</code> before {@link #create()}
	 */
	public Button getButton(final int which) {
		return buttons[which];
	}

	/**
	 * @param page a step of the wizard
	 * @return the step's content, or <code>null</code> when the step was not shown yet
	 */
	public Control getContent(final WizardPage page) {
		return contents.get(page);
	}

	private void update() {
		if (shell == null || shell.isDisposed()) {
			return;
		}
		if (wizard.isDone()) {
			shell.dispose();
			return;
		}
		final WizardPage page = wizard.getCurrentPage();
		if (page != null) {
			final Control content = contents.computeIfAbsent(page, p -> p.createContent(pageArea));
			((StackLayout) pageArea.getLayout()).topControl = content;
			header.setTitle(page.getTitle());
			header.setDescription(page.getDescription() == null ? "" : page.getDescription());
			final String message = page.getErrorMessage();
			error.setText(message == null ? "" : message);
			error.setVisible(message != null);
			((GridData) error.getLayoutData()).exclude = message == null;
		}
		final int index = wizard.getPages().indexOf(page);
		for (int i = 0; i < steps.size(); i++) {
			steps.get(i).setFont(i == index ? bold : null);
		}
		buttons[BACK].setEnabled(wizard.canGoBack());
		buttons[NEXT].setEnabled(wizard.canGoNext());
		buttons[FINISH].setEnabled(wizard.canFinish());
		shell.setDefaultButton(wizard.canGoNext() ? buttons[NEXT] : wizard.canFinish() ? buttons[FINISH] : null);
		pageArea.layout(true, true);
		shell.layout(true, true);
	}

	private void disposed() {
		wizard.removeChangeListener(update);
		if (bold != null) {
			bold.dispose();
		}
	}
}
