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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** The wizard's rules without a widget, and its window created but never opened. */
public class WizardTest {

	/* a step that records what happens to it and can refuse to be left */
	static final class Step extends WizardPage {
		final List<String> log;
		boolean mayLeave = true;
		WizardPage next;
		boolean chooses;
		int created;

		Step(final String title, final List<String> log) {
			super(title);
			this.log = log;
		}

		@Override
		public Control createContent(final Composite parent) {
			created++;
			final Composite content = new Composite(parent, SWT.NONE);
			content.setLayout(new FillLayout());
			new Label(content, SWT.NONE).setText(getTitle());
			return content;
		}

		@Override
		public void enter() {
			log.add("enter " + getTitle());
		}

		@Override
		public boolean leave(final boolean forward) {
			log.add((forward ? "leave forward " : "leave back ") + getTitle());
			return mayLeave;
		}

		@Override
		public WizardPage getNextPage() {
			return chooses ? next : super.getNextPage();
		}
	}

	final List<String> log = new ArrayList<>();
	Wizard wizard;
	Step one, two, three;
	Display display;
	Shell parent;

	@Before
	public void setUp() {
		wizard = new Wizard("New thing");
		one = new Step("One", log);
		two = new Step("Two", log);
		three = new Step("Three", log);
		wizard.addPage(one);
		wizard.addPage(two);
		wizard.addPage(three);
	}

	@After
	public void tearDown() {
		if (parent != null) {
			parent.dispose();
		}
	}

	WizardWindow window() {
		display = Display.getDefault();
		parent = new Shell(display);
		final WizardWindow window = new WizardWindow(parent, wizard);
		window.create();
		return window;
	}

	static void press(final Button button) {
		button.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
	}

	@Test
	public void theButtonsFollowJFacesRules() {
		assertSame(one, wizard.getCurrentPage());
		assertFalse("no step was taken before the first", wizard.canGoBack());
		assertTrue(wizard.canGoNext());
		assertTrue("every step is complete", wizard.canFinish());
		two.setComplete(false);
		assertFalse("a step is not complete", wizard.canFinish());
		assertTrue(wizard.next());
		assertSame(two, wizard.getCurrentPage());
		assertTrue(wizard.canGoBack());
		assertFalse("the current step is not complete", wizard.canGoNext());
		assertFalse(wizard.next());
		two.setComplete(true);
		assertTrue(wizard.next());
		assertFalse("the last step has no next step", wizard.canGoNext());
		assertEquals(List.of("leave forward One", "enter Two", "leave forward Two", "enter Three"), log);
	}

	@Test
	public void aStepMayRefuseToBeLeftAndBackFollowsTheStepsTaken() {
		one.chooses = true;
		one.next = three;
		assertTrue(wizard.next());
		assertSame("the first step chose the third", three, wizard.getCurrentPage());
		three.mayLeave = false;
		assertFalse(wizard.back());
		assertSame(three, wizard.getCurrentPage());
		three.mayLeave = true;
		assertTrue(wizard.back());
		assertSame("back along the steps taken, not the list", one, wizard.getCurrentPage());
		one.mayLeave = false;
		assertFalse(wizard.next());
		assertFalse("finishing asks the step too", wizard.finish());
		assertFalse(wizard.isDone());
	}

	@Test
	public void finishAndCancelRunTheirActionsOnce() {
		final int[] runs = new int[2];
		final boolean[] agree = { false };
		wizard.setFinishAction(() -> {
			runs[0]++;
			return agree[0];
		});
		wizard.setCancelAction(() -> runs[1]++);
		assertFalse("the finish action refused", wizard.finish());
		assertFalse(wizard.isDone());
		agree[0] = true;
		assertTrue(wizard.finish());
		assertTrue(wizard.isFinished());
		assertFalse("done: nothing more is possible", wizard.canFinish() || wizard.canGoNext() || wizard.canGoBack());
		wizard.cancel();
		assertEquals(2, runs[0]);
		assertEquals("a finished wizard is not cancelled", 0, runs[1]);
		wizard.setCanFinish(() -> false);
		final Wizard other = new Wizard("x");
		other.addPage(new Step("only", log));
		other.setCancelAction(() -> runs[1]++);
		other.cancel();
		other.cancel();
		assertEquals(1, runs[1]);
		assertTrue(other.isDone());
		assertFalse(other.isFinished());
	}

	@Test
	public void aCanFinishRuleReplacesEveryStepComplete() {
		two.setComplete(false);
		wizard.setCanFinish(() -> wizard.getCurrentPage() == one);
		assertTrue(wizard.canFinish());
		assertThrows(IllegalArgumentException.class, () -> wizard.addPage(one));
		assertThrows(IllegalArgumentException.class, () -> wizard.addPage(null));
	}

	@Test
	public void theWindowShowsTheCurrentStepAndMirrorsTheRules() {
		final WizardWindow window = window();
		assertNotNull(window.getContent(one));
		assertNull("a step's content is created when it is first shown", window.getContent(two));
		final Composite area = (Composite) window.getContent(one).getParent();
		assertSame(window.getContent(one), ((StackLayout) area.getLayout()).topControl);
		assertFalse(window.getButton(WizardWindow.BACK).getEnabled());
		assertTrue(window.getButton(WizardWindow.NEXT).getEnabled());
		assertSame("Next is the default button while enabled", window.getButton(WizardWindow.NEXT), window.getShell().getDefaultButton());
		two.setComplete(false);
		press(window.getButton(WizardWindow.NEXT));
		assertSame(window.getContent(two), ((StackLayout) area.getLayout()).topControl);
		assertTrue(window.getButton(WizardWindow.BACK).getEnabled());
		assertFalse(window.getButton(WizardWindow.NEXT).getEnabled());
		assertFalse(window.getButton(WizardWindow.FINISH).getEnabled());
		two.setErrorMessage("Name is missing");
		assertTrue(findLabel(window.getShell(), "Name is missing").getVisible());
		two.setErrorMessage(null);
		two.setComplete(true);
		press(window.getButton(WizardWindow.NEXT));
		assertSame("the last step: Finish is the default button", window.getButton(WizardWindow.FINISH), window.getShell().getDefaultButton());
		press(window.getButton(WizardWindow.BACK));
		press(window.getButton(WizardWindow.NEXT));
		assertEquals("contents are kept, not created again", 1, three.created);
		assertTrue(findLabel(window.getShell(), "3. Three").getFont().getFontData()[0].getStyle() != SWT.NORMAL);
		assertEquals(SWT.NORMAL, findLabel(window.getShell(), "1. One").getFont().getFontData()[0].getStyle() & SWT.BOLD);
		press(window.getButton(WizardWindow.FINISH));
		assertTrue("finishing closes the window", window.getShell().isDisposed());
		assertTrue(wizard.isFinished());
	}

	@Test
	public void closingTheWindowCancelsTheWizard() {
		final int[] cancelled = new int[1];
		wizard.setCancelAction(() -> cancelled[0]++);
		final WizardWindow window = window();
		window.getShell().close();
		assertTrue(window.getShell().isDisposed());
		assertEquals(1, cancelled[0]);
		assertFalse(wizard.isFinished());
		final Wizard single = new Wizard("one step");
		single.addPage(new Step("Only", log));
		wizard = single;
		final WizardWindow plain = window();
		assertNull("one step: no steps list", findLabel(plain.getShell(), "1. Only"));
		press(plain.getButton(WizardWindow.CANCEL));
		assertTrue(plain.getShell().isDisposed());
	}

	/* the label with this text that is shown (its parent visible), or null */
	static Label findLabel(final Composite composite, final String text) {
		for (final Control child : composite.getChildren()) {
			if (child instanceof Label && text.equals(((Label) child).getText()) && (child.getParent() instanceof Shell || child.getParent().getVisible())) {
				return (Label) child;
			}
			if (child instanceof Composite) {
				final Label found = findLabel((Composite) child, text);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
