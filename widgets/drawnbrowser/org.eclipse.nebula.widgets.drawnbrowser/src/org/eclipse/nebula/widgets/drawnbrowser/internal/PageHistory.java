/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The drawn browser's location and history. It holds no display or widget, so it is tested without a
 * {@code Display}.
 * <p>
 * A page loads off the UI thread. Each navigation takes a generation ({@link #begin()}); {@link #setText()},
 * {@link #stop()} and a newer navigation bump it, so a page that arrives for an older one is dropped
 * ({@link #arrive(String, int, int)} answers false). A page that arrives in time becomes the location: a new
 * navigation ({@link #PUSH}) drops the forward entries and adds itself, {@link #BACK} and {@link #FORWARD} move the
 * position only.
 * </p>
 * <p>
 * Pattern: the back and forward history of {@link org.eclipse.swt.browser.Browser}, with a generation counter for
 * loads that finish out of order.
 * </p>
 */
public final class PageHistory {

	/** The location of a page given as text. */
	public static final String ABOUT_BLANK = "about:blank";

	/** How a navigation moves in the history. */
	public static final int PUSH = 0, BACK = -1, FORWARD = 1;

	private String location = ABOUT_BLANK;
	private final List<String> entries = new ArrayList<>();
	private int index = -1;
	private int generation;

	/** The location of the page shown. */
	public String location() {
		return location;
	}

	/** The number of history entries. */
	public int size() {
		return entries.size();
	}

	public boolean canBack() {
		return index > 0;
	}

	public boolean canForward() {
		return index >= 0 && index < entries.size() - 1;
	}

	/** The entry {@code back()} goes to; only when {@link #canBack()}. */
	public String backTarget() {
		return entries.get(index - 1);
	}

	/** The entry {@code forward()} goes to; only when {@link #canForward()}. */
	public String forwardTarget() {
		return entries.get(index + 1);
	}

	/** Starts a navigation: loads begun before it become stale. Answers its generation. */
	public int begin() {
		return ++generation;
	}

	/** Drops the page being loaded, if any (stop, dispose). */
	public void stop() {
		generation++;
	}

	/** A page given as text is shown: it is at {@link #ABOUT_BLANK}, the history stays, loads become stale. */
	public void setText() {
		generation++;
		location = ABOUT_BLANK;
	}

	/** Whether a load of generation {@code expected} is still the one wanted. */
	public boolean isCurrent(int expected) {
		return expected == generation;
	}

	/**
	 * A page of generation {@code expected} arrived. When it is current it becomes the location and the history
	 * moves by {@code mode}.
	 *
	 * @return whether it is shown (false: a newer navigation, a stop or a text replaced it)
	 */
	public boolean arrive(String url, int mode, int expected) {
		if (!isCurrent(expected)) return false;
		location = url;
		if (mode == PUSH) {
			while (entries.size() > index + 1) entries.remove(entries.size() - 1);
			entries.add(url);
			index++;
		} else {
			index += mode;
		}
		return true;
	}

	/** A link or image reference resolved against the location; as written at {@link #ABOUT_BLANK} or when invalid. */
	public String resolve(String reference) {
		if (location.equals(ABOUT_BLANK)) return reference;
		try {
			return URI.create(location).resolve(reference).toString();
		} catch (IllegalArgumentException e) {
			return reference;
		}
	}
}
