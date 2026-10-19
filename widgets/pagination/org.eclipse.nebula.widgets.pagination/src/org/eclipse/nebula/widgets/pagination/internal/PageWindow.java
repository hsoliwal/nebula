/*******************************************************************************
 * Copyright (C) 2011 Angelo Zerr <angelo.zerr@gmail.com>, Pascal Leclercq <pascal.leclercq@gmail.com>
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo ZERR - initial API and implementation
 *     Pascal Leclercq - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.pagination.internal;

/** Dependency-free projection of the original PaginationHelper page-window atom.
 * The method body is preserved verbatim, including legacy edge cases and fresh arrays.
 * No data items, controls, event listeners or native resources are owned here.
 * Extracted through the Synexia web-widget recipe, 2026-10-15.
 */
public final class PageWindow {
    private PageWindow() { }
    public static final int SEPARATOR = -1;
	public static int[] getPageIndexes(int currentPageIndex, int totalPages,
			int nbMax) {
		nbMax = nbMax > totalPages ? totalPages : nbMax;
		int[] indexes = new int[nbMax];
		if (totalPages <= nbMax) {
			// ALl indexes must be filled
			for (int i = 0; i < indexes.length; i++) {
				indexes[i] = i;
			}
		} else {
			if (currentPageIndex > (totalPages - nbMax) + 3) {
				int index = totalPages - 1;
				for (int i = indexes.length - 1; i >= 0; i--) {
					if (i == 0) {
						indexes[i] = i;
					} else if (i == 1) {
						indexes[i] = SEPARATOR;
					} else {
						indexes[i] = index--;
					}
				}
			} else {

				if (nbMax - currentPageIndex > 2) {
					for (int i = 0; i < indexes.length; i++) {
						if (i == nbMax - 1) {
							indexes[i] = totalPages - 1;
						} else if (i == nbMax - 2) {
							indexes[i] = SEPARATOR;
						} else {
							indexes[i] = i;
						}
					}
				} else {
					// Total page is > to nb max of pages to display
					// Compute list to have for instance 1 ... 10, 11, 12, 13,
					// 14,
					// ...
					// 20
					int middle = nbMax / 2;
					int index = currentPageIndex;
					int nbItems = 0;
					for (int i = middle; i > 0 && index > 0; i--) {
						if (i == 1) {
							// before last item, check if it's 2 item
							indexes[i] = index == 2 ? index : SEPARATOR;
						} else if (i == 0) {
							indexes[i] = 0;
						} else if (index > 0) {
							indexes[i] = index--;
						}
						nbItems++;
					}

					index = currentPageIndex;
					for (int i = nbItems; i < nbMax; i++) {
						if (i == nbMax - 2) {
							indexes[i] = index == totalPages ? index : SEPARATOR;
						} else if (i == nbMax - 1) {
							indexes[i] = totalPages - 1;
						} else {
							indexes[i] = index++;
						}
					}
				}
			}

		}
		return indexes;
	}
}
