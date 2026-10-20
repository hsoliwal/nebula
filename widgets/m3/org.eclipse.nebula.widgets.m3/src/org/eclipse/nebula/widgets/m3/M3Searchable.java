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

import java.util.function.IntConsumer;
import java.util.function.IntFunction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Table;

/**
 * What an {@link M3FindBar} searches: rows of text in the caller's model, by index (the SWT counterpart of SwingX
 * <code>Searchable</code>). Rows are read from the model, never from widget items, so a virtual table of 10^6 rows is
 * searched without materializing items.
 */
public interface M3Searchable {

	/** @return the number of rows */
	int getRowCount();

	/**
	 * @param row a row index
	 * @return the searchable text of the row, never null
	 */
	String getText(int row);

	/**
	 * Selects and reveals a found row.
	 *
	 * @param row the row index
	 */
	void reveal(int row);

	/**
	 * A searchable over model lanes.
	 *
	 * @param count the row count supplier
	 * @param text the text of a row
	 * @param reveal selects and reveals a row
	 * @return the searchable
	 */
	static M3Searchable of(java.util.function.IntSupplier count, IntFunction<String> text, IntConsumer reveal) {
		return new M3Searchable() {
			@Override
			public int getRowCount() {
				return count.getAsInt();
			}

			@Override
			public String getText(int row) {
				String value = text.apply(row);
				return value == null ? "" : value; //$NON-NLS-1$
			}

			@Override
			public void reveal(int row) {
				reveal.accept(row);
			}
		};
	}

	/**
	 * A searchable over a table whose row texts come from the caller's model (for a virtual table) and whose found row
	 * is selected and shown in the table.
	 *
	 * @param table the table
	 * @param text the text of a row in the model
	 * @return the searchable
	 */
	static M3Searchable of(Table table, IntFunction<String> text) {
		if (table == null || text == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		return of(table::getItemCount, text, row -> {
			table.setSelection(row);
			table.showSelection();
		});
	}

	/**
	 * A searchable over the items of a list.
	 *
	 * @param list the list
	 * @return the searchable
	 */
	static M3Searchable of(List list) {
		if (list == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		return of(list::getItemCount, list::getItem, row -> {
			list.setSelection(row);
			list.showSelection();
		});
	}
}
