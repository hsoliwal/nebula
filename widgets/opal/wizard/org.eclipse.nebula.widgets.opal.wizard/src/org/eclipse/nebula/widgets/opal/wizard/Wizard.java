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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * The steps of a wizard and the way through them, without any widget: the model that a
 * {@link WizardWindow} shows.
 * <p>
 * The rules are those of a JFace wizard dialog: Back is possible when a step was taken before the
 * current one, Next when the current step is complete and has a next step, Finish when every step
 * is complete (or as {@link #setCanFinish(BooleanSupplier)} decides). Going forward or finishing
 * asks the current step whether it may be left; going back asks too, and a refusal stays.
 * </p>
 */
public class Wizard {

	private final String title;
	private final List<WizardPage> pages = new ArrayList<>();
	private final Deque<WizardPage> taken = new ArrayDeque<>();
	private final List<Runnable> listeners = new ArrayList<>();
	private WizardPage current;
	private BooleanSupplier canFinish;
	private BooleanSupplier finish = () -> true;
	private Runnable cancel = () -> {
	};
	private boolean done, finished;

	/**
	 * Constructs a wizard without steps.
	 *
	 * @param title the title of the wizard window
	 */
	public Wizard(final String title) {
		this.title = title == null ? "" : title;
	}

	/**
	 * Adds a step after the others. The first step added is the current one.
	 *
	 * @param page the step
	 * @throws IllegalArgumentException if the step is null or belongs to a wizard
	 */
	public void addPage(final WizardPage page) {
		if (page == null || page.wizard != null) {
			throw new IllegalArgumentException("the page is null or belongs to a wizard");
		}
		page.wizard = this;
		pages.add(page);
		if (current == null) {
			current = page;
		}
		fireChanged();
	}

	/**
	 * @return the steps, in the order they were added
	 */
	public List<WizardPage> getPages() {
		return Collections.unmodifiableList(pages);
	}

	/**
	 * @return the current step, or <code>null</code> without steps
	 */
	public WizardPage getCurrentPage() {
		return current;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	WizardPage following(final WizardPage page) {
		final int index = pages.indexOf(page);
		return index < 0 || index + 1 >= pages.size() ? null : pages.get(index + 1);
	}

	/**
	 * @return whether a step was taken before the current one
	 */
	public boolean canGoBack() {
		return !done && !taken.isEmpty();
	}

	/**
	 * @return whether the current step is complete and has a next step
	 */
	public boolean canGoNext() {
		return !done && current != null && current.isComplete() && current.getNextPage() != null;
	}

	/**
	 * @return whether the wizard can finish now
	 */
	public boolean canFinish() {
		if (done || current == null) {
			return false;
		}
		if (canFinish != null) {
			return canFinish.getAsBoolean();
		}
		return pages.stream().allMatch(WizardPage::isComplete);
	}

	/**
	 * Goes to the next step, when possible and the current step may be left.
	 *
	 * @return whether the current step changed
	 */
	public boolean next() {
		if (!canGoNext() || !current.leave(true)) {
			return false;
		}
		taken.push(current);
		show(current.getNextPage());
		return true;
	}

	/**
	 * Goes back to the step taken before the current one, when the current step may be left.
	 *
	 * @return whether the current step changed
	 */
	public boolean back() {
		if (!canGoBack() || !current.leave(false)) {
			return false;
		}
		show(taken.pop());
		return true;
	}

	/**
	 * Finishes, when possible: the current step may be left and the finish action agrees.
	 *
	 * @return whether the wizard is finished
	 */
	public boolean finish() {
		if (!canFinish() || !current.leave(true) || !finish.getAsBoolean()) {
			return false;
		}
		done = finished = true;
		fireChanged();
		return true;
	}

	/**
	 * Cancels: the cancel action runs and the wizard is done.
	 */
	public void cancel() {
		if (done) {
			return;
		}
		done = true;
		cancel.run();
		fireChanged();
	}

	/**
	 * @return whether the wizard finished (not cancelled)
	 */
	public boolean isFinished() {
		return finished;
	}

	/**
	 * @return whether the wizard finished or was cancelled
	 */
	public boolean isDone() {
		return done;
	}

	private void show(final WizardPage page) {
		current = page;
		page.enter();
		fireChanged();
	}

	/**
	 * @param canFinish decides whether the wizard can finish, or <code>null</code> for "every step is complete"
	 */
	public void setCanFinish(final BooleanSupplier canFinish) {
		this.canFinish = canFinish;
		fireChanged();
	}

	/**
	 * @param finish runs when the wizard finishes; returns <code>false</code> to stay open
	 */
	public void setFinishAction(final BooleanSupplier finish) {
		this.finish = finish == null ? () -> true : finish;
	}

	/**
	 * @param cancel runs when the wizard is cancelled
	 */
	public void setCancelAction(final Runnable cancel) {
		this.cancel = cancel == null ? () -> {
		} : cancel;
	}

	/**
	 * @param listener runs whenever the current step, a step's state or the wizard's state changes
	 */
	public void addChangeListener(final Runnable listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener the listener to remove
	 */
	public void removeChangeListener(final Runnable listener) {
		listeners.remove(listener);
	}

	void fireChanged() {
		new ArrayList<>(listeners).forEach(Runnable::run);
	}
}
