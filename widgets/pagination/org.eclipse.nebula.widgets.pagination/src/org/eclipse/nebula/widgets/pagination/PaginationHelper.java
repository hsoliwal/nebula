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
package org.eclipse.nebula.widgets.pagination;

import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.swt.widgets.Widget;

/**
 * Pagination Utilities.
 * 
 */
public class PaginationHelper {

	private static final String PAGINATION_CONTROLLER_KEY = "___PaginationController";
	public static final int SEPARATOR = -1;

	/**
	 * Returns an array of int with the page indexes for the given
	 * currentPageIndex, totalPages abd nbMax.
	 * 
	 * @param currentPageIndex
	 * @param totalPages
	 * @param nbMax
	 * @return
	 */
	public static int[] getPageIndexes(int currentPageIndex, int totalPages,
			int nbMax) {
		return org.eclipse.nebula.widgets.pagination.internal.PageWindow
				.getPageIndexes(currentPageIndex, totalPages, nbMax);
	}

	/**
	 * Returns the attached {@link PageableController} to the given widget.
	 * 
	 * @param widget
	 * @return
	 */
	public static PageableController getController(Widget widget) {
		return (PageableController) widget.getData(PAGINATION_CONTROLLER_KEY);
	}

	/**
	 * Attach the given {@link PageableController} to the given widget.
	 * 
	 * @param widget
	 * @param controller
	 */
	public static void setController(Widget widget,
			PageableController controller) {
		widget.setData(PAGINATION_CONTROLLER_KEY, controller);
	}

	/**
	 * Returns the results text (ex: "Results 1-5 of 10") for the given
	 * pagination controller. The given locale is used to translate the results
	 * text.
	 * 
	 * @param controller
	 *            the pagination controller.
	 * @param locale
	 *            the locale.
	 * @return
	 */
	public static String getResultsText(PageableController controller,
			Locale locale) {
		String resultsMessage = Resources.getText(
				Resources.PaginationRenderer_results, locale);// "Results {0}-{1} of {2}";
		return getResultsText(controller, resultsMessage);
	}

	/**
	 * Returns the results text (ex: "Results 1-5 of 10") for the given
	 * pagination controller. The resultsMessage (ex: "Results {0}-{1} of {2}")
	 * is used to compute the text.
	 * 
	 * @param controller
	 *            the pagination controller.
	 * @param resultsMessage
	 *            the results message.
	 * @return
	 */
	public static String getResultsText(PageableController controller,
			String resultsMessage) {
		int start = controller.getPageOffset() + 1;
		int end = start + controller.getPageSize() - 1;
		long total = controller.getTotalElements();
		if (end > total) {
			end = (int) total;
		}
		return getResultsText(start, end, total, controller, resultsMessage);
	}

	/**
	 * Returns the results text (ex: "Results 1-5 of 10") for the given
	 * pagination information start, end and total. The resultsMessage (ex:
	 * "Results {0}-{1} of {2}") is used to compute the text.
	 * 
	 * @param start
	 *            first page offset.
	 * @param end
	 *            last page offset
	 * @param total
	 *            total elements.
	 * @param controller
	 * @param resultsMessage
	 * @return
	 */
	public static String getResultsText(int start, int end, long total,
			PageableController controller, String resultsMessage) {
		return MessageFormat.format(resultsMessage, start, end, total);
	}

	/**
	 * urns the page text (ex : Page 1/2).
	 * 
	 * @param pageIndex
	 *            the page index.
	 * @param totalPage
	 *            the total page.
	 * @param locale
	 *            the locale.
	 * @return
	 */
	public static String getPageText(int pageIndex, int totalPage, Locale locale) {
		String message = Resources.getText(Resources.PaginationRenderer_page,
				locale);
		return MessageFormat.format(message, pageIndex, totalPage);
	}
}
