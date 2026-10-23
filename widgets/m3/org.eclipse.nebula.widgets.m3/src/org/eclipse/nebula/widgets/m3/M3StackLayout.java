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
package org.eclipse.nebula.widgets.m3;

import org.eclipse.nebula.widgets.m3.internal.M3ResidentPages;
import org.eclipse.nebula.widgets.m3.internal.SwtLog;
import org.eclipse.nebula.widgets.m3.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A {@link StackLayout} that materializes only the top control.
 * <p>
 * <b>Drop-in use.</b> Set it on a composite exactly like a <code>StackLayout</code>: the fields
 * {@link #topControl}, {@link #marginWidth} and {@link #marginHeight} and the preferred size are the same. A layout
 * gives bounds to the top control only; every other child is hidden (parked) and receives its bounds when it becomes
 * the top control, so a layout costs one <code>setBounds</code> instead of one per child.
 * </p>
 * <p>
 * <b>Pages.</b> {@link #setPages(Composite, int, M3PageProvider)} declares any number of pages without creating
 * them. {@link #showPage(int)} creates a page through the provider when it is not resident and makes it the top
 * control. At most {@link #getResidencyBudget()} pages stay resident; beyond that the page shown least recently (never
 * the top page) is handed to {@link M3PageProvider#releasePage(int, Control)}. The cost of {@link #showPage(int)}
 * depends on the budget, not on the page count.
 * </p>
 * <p>
 * Differences from <code>StackLayout</code>: a hidden child's bounds (and its Move and Resize events) are updated when
 * it becomes the top control rather than on every layout; in page mode only resident pages exist as controls, so
 * {@link Composite#getChildren()} and the preferred size cover the resident pages. Visibility, Show and Hide events,
 * the top control's bounds, the preferred size and the pixels are the same as with <code>StackLayout</code>.
 * </p>
 * <p>
 * Startup switch: <code>-Dswt.widget.mode.M3StackLayout=original</code> (or <code>-Dswt.widget.mode=original</code>)
 * lays out every child on every layout, as <code>StackLayout</code> does, and keeps every page that was created.
 * </p>
 *
 * @see M3PageProvider
 */
public class M3StackLayout extends StackLayout {

	/** The residency budget of a new layout: the number of pages kept as controls. */
	public static final int DEFAULT_RESIDENCY_BUDGET = 8;

	static final boolean VIEWPORT = WidgetMode.of("M3StackLayout", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	private static final SwtLog LOG = SwtLog.getLog("org.eclipse.nebula.widgets.m3.M3StackLayout"); //$NON-NLS-1$

	private final M3ResidentPages resident = new M3ResidentPages();
	private Composite parent;
	private M3PageProvider provider;
	private int pageCount;
	private int topPage = -1;
	private int budget = DEFAULT_RESIDENCY_BUDGET;

	@Override
	protected void layout(Composite composite, boolean flushCache) {
		if (!VIEWPORT) {
			super.layout(composite, flushCache);
			return;
		}
		Rectangle area = area(composite);
		for (Control child : composite.getChildren()) {
			if (child == topControl) {
				child.setBounds(area);
				child.setVisible(true);
			} else if (child.getVisible()) {
				child.setVisible(false);
			}
		}
	}

	private Rectangle area(Composite composite) {
		Rectangle area = composite.getClientArea();
		area.x += marginWidth;
		area.y += marginHeight;
		area.width -= 2 * marginWidth;
		area.height -= 2 * marginHeight;
		return area;
	}

	/**
	 * Declares the pages of <code>parent</code>, which must use this layout. Pages that were resident are released
	 * first; no page is created until it is shown.
	 *
	 * @param parent the composite the pages are created in
	 * @param count the number of pages, at least 0
	 * @param provider creates and releases the pages
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the parent or the provider is null</li>
	 *                <li>ERROR_INVALID_ARGUMENT - if the parent is disposed or the count is negative</li>
	 *                </ul>
	 */
	public void setPages(Composite parent, int count, M3PageProvider provider) {
		if (parent == null || provider == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (parent.isDisposed() || count < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		releaseFrom(0);
		this.parent = parent;
		this.provider = provider;
		this.pageCount = count;
	}

	/** @return the page provider, or <code>null</code> when the layout is not in page mode */
	public M3PageProvider getPageProvider() {
		return provider;
	}

	/** @return the number of pages (0 when the layout is not in page mode) */
	public int getPageCount() {
		return pageCount;
	}

	/**
	 * Changes the number of pages. Resident pages at or beyond the new count are released; when the top page is one of
	 * them, no page is on top afterwards.
	 *
	 * @param count the number of pages, at least 0
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if the layout is not in page mode or the count is
	 *                negative
	 */
	public void setPageCount(int count) {
		checkPages();
		if (count < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		boolean topGone = getTopPage() >= count;
		releaseFrom(count);
		pageCount = count;
		if (topGone && !parent.isDisposed()) {
			parent.layout();
		}
	}

	/**
	 * Makes a page the top control, creating it when it is not resident, and lays out the parent. Pages beyond the
	 * residency budget are released, least recently shown first.
	 *
	 * @param index the page index
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_INVALID_ARGUMENT - if the layout is not in page mode, or the provider returned null or a
	 *                control that is not a child of the parent</li>
	 *                <li>ERROR_INVALID_RANGE - if the index is not between 0 and the page count - 1</li>
	 *                </ul>
	 */
	public void showPage(int index) {
		checkIndex(index);
		Control page = materialize(index);
		topPage = index;
		topControl = page;
		parent.layout();
		trim();
	}

	/**
	 * @return the index of the page that is the top control, or -1 when the top control is not a resident page
	 */
	public int getTopPage() {
		if (topPage < 0 || topControl == null || topControl.isDisposed()) {
			return -1;
		}
		int slot = resident.slotOf(topPage);
		return slot >= 0 && resident.control(slot) == topControl ? topPage : -1;
	}

	/**
	 * Answers the control of a page without creating it.
	 *
	 * @param index the page index
	 * @return the page control, or <code>null</code> when the page is not resident
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if the index is not between 0 and the page count - 1
	 */
	public Control getPage(int index) {
		checkIndex(index);
		purge();
		int slot = resident.slotOf(index);
		return slot < 0 ? null : resident.control(slot);
	}

	/** @return the number of pages that exist as controls */
	public int getResidentCount() {
		purge();
		return resident.size();
	}

	/** @return the indices of the pages that exist as controls, ascending */
	public int[] getResidentPages() {
		purge();
		return resident.pages();
	}

	/**
	 * Releases a resident page through the provider. The top page is never released.
	 *
	 * @param index the page index
	 * @return <code>true</code> when the page was resident, not on top, and was released
	 * @exception IllegalArgumentException ERROR_INVALID_RANGE - if the index is not between 0 and the page count - 1
	 */
	public boolean releasePage(int index) {
		checkIndex(index);
		purge();
		int slot = resident.slotOf(index);
		if (slot < 0 || index == getTopPage()) {
			return false;
		}
		release(slot);
		return true;
	}

	/** @return the number of pages kept as controls (the top page included) */
	public int getResidencyBudget() {
		return budget;
	}

	/**
	 * Sets the number of pages kept as controls and releases the pages beyond it, least recently shown first.
	 *
	 * @param budget at least 1
	 * @exception IllegalArgumentException ERROR_INVALID_ARGUMENT - if the budget is less than 1
	 */
	public void setResidencyBudget(int budget) {
		if (budget < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.budget = budget;
		trim();
	}

	private Control materialize(int index) {
		purge();
		int slot = resident.slotOf(index);
		if (slot >= 0) {
			resident.touch(slot);
			return resident.control(slot);
		}
		Control page = provider.createPage(parent, index);
		if (page == null || page.isDisposed() || page.getParent() != parent) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		resident.add(index, page);
		if (LOG.isDebugEnabled()) {
			LOG.debug(() -> "page " + index + " created; resident " + resident.size()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return page;
	}

	private void trim() {
		int limit = VIEWPORT ? budget : Integer.MAX_VALUE;
		int keep = getTopPage();
		while (resident.size() > limit) {
			int slot = resident.leastRecent(keep);
			if (slot < 0) {
				return;
			}
			release(slot);
		}
	}

	private void release(int slot) {
		int index = resident.page(slot);
		Control page = resident.control(slot);
		resident.remove(slot);
		if (page == topControl) {
			topControl = null;
		}
		if (!page.isDisposed()) {
			provider.releasePage(index, page);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug(() -> "page " + index + " released; resident " + resident.size()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/* releases every resident page whose index is at least from */
	private void releaseFrom(int from) {
		for (int slot = resident.size() - 1; slot >= 0; slot--) {
			if (resident.page(slot) >= from) {
				release(slot);
			}
		}
		if (topPage >= from) {
			topPage = -1;
		}
	}

	/* drops the pages the client disposed */
	private void purge() {
		for (int slot = resident.size() - 1; slot >= 0; slot--) {
			if (resident.control(slot).isDisposed()) {
				resident.remove(slot);
			}
		}
	}

	private void checkPages() {
		if (provider == null) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
	}

	private void checkIndex(int index) {
		checkPages();
		if (index < 0 || index >= pageCount) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
	}
}
