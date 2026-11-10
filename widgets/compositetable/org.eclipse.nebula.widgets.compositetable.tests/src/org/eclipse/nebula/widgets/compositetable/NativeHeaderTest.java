/*******************************************************************************
 * Copyright (c) 2009 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     compeople AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.compositetable;

import junit.framework.TestCase;

import org.eclipse.nebula.widgets.compositetable.AbstractNativeHeader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

public class NativeHeaderTest extends TestCase {
	private boolean createdDisplay = false;

    private static class Header extends AbstractNativeHeader {
	public Header(Composite parent, int style) {
	    super(parent, style);
	    setWeights(new int[] { 100, 200 });
	    setColumnText(new String[] { "A", "B" });
	}
    }

    private Display display;
    private Shell shell;

    @Override
	protected void setUp() throws Exception {
		display = Display.getCurrent();
		if (display == null) {
			display = new Display();
			createdDisplay = true;
		}
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
	}

    @Override
	protected void tearDown() throws Exception {
		shell.dispose();
		if (createdDisplay) {
			display.dispose();
		}
	}

    // test methods
    // /////////////

    public void testGetColumns() {
	Header header = new Header(shell, SWT.NONE);

	assertEquals(2, header.getColumns().length);
    }

    public void testSetSortColumn() {
	Header header = new Header(shell, SWT.NONE);

	assertEquals(-1, header.indexOfSortColumn());

	header.setSortColumn(0);

	assertEquals(0, header.indexOfSortColumn());

	header.setSortColumn(1);

	assertEquals(1, header.indexOfSortColumn());

	header.setSortColumn(-1);

	assertEquals(-1, header.indexOfSortColumn());

	try {
	    header.setSortColumn(42);
	    fail();
	} catch (RuntimeException rex) {
	    // expected
	}
    }

    public void testHiddenWideningLayoutsKeepOnePendingRelayout() {
	Composite holder = new Composite(shell, SWT.NONE);
	holder.setLayout(new FillLayout());
	Header header = new Header(holder, SWT.NONE);
	Table table = header.headerTable;
	int ownPaintListeners = table.getListeners(SWT.Paint).length;
	holder.setVisible(false);
	shell.setSize(200, 100);
	shell.open();
	for (int width = 220; width <= 400; width += 20) {
	    shell.setSize(width, 100);
	    while (display.readAndDispatch()) {
		// flush
	    }
	}
	assertEquals("a hidden header keeps one pending widening relayout", ownPaintListeners + 1,
		table.getListeners(SWT.Paint).length);
	holder.setVisible(true);
	long end = System.currentTimeMillis() + 2000;
	while (table.getListeners(SWT.Paint).length > ownPaintListeners && System.currentTimeMillis() < end) {
	    if (!display.readAndDispatch()) {
		try {
		    Thread.sleep(5);
		} catch (InterruptedException e) {
		    break;
		}
	    }
	}
	assertEquals("the first paint runs the pending relayout", ownPaintListeners, table.getListeners(SWT.Paint).length);
	assertEquals("the header table fills the header", header.getSize().x, table.getSize().x);
    }

    public void testSetSortDirection() {
	Header header = new Header(shell, SWT.NONE);

	assertEquals(SWT.NONE, header.getSortDirection());

	int[] values = { SWT.UP, SWT.DOWN, SWT.NONE };
	for (int value : values) {
	    header.setSortDirection(value);

	    assertEquals(value, header.getSortDirection());
	}

	try {
	    header.setSortDirection(-1);
	    fail();
	} catch (RuntimeException rex) {
	    // expected
	}
    }

}
