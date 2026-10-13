/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.m3.tt;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.nebula.widgets.xviewer.XViewer;
import org.eclipse.nebula.widgets.xviewer.XViewerFactory;
import org.eclipse.nebula.widgets.xviewer.XViewerLabelProvider;
import org.eclipse.nebula.widgets.xviewer.core.model.CustomizeData;
import org.eclipse.nebula.widgets.xviewer.core.model.SortDataType;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerAlign;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerColumn;
import org.eclipse.nebula.widgets.xviewer.customize.CustomizeManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/** XViewer with 3 columns, 12 roots x 4 children, first root expanded. */
public final class XViewerFixture implements TtFixture {
	private static final Pattern SKIP_NAME = Pattern.compile(
			"(?i)^(open|run|launch|print.*|.*report.*|.*html.*|viewSelected.*|.*dialog.*|prompt.*|handle.*Click.*)$");
	private static final Pattern SKIP_TYPE = Pattern.compile(
			"XViewerHtmlReport|HtmlDialog|XViewerTreeReport|XViewerLoadingReport|.*Dialog|XPromptChange|.*Action");

	/** Sample model node; identity is its path. */
	public static final class Node {
		final String path;
		final List<Node> children = new ArrayList<>();
		final Node parent;

		Node(Node parent, String path) {
			this.parent = parent;
			this.path = path;
		}

		@Override
		public String toString() {
			return "Node(" + path + ")";
		}
	}

	private XViewer viewer;
	private XViewerFactory factory;
	private final List<Node> roots = new ArrayList<>();
	private XViewerColumn nameCol;

	@Override
	public void build(Composite stage) {
		Composite parent = new Composite(stage, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		nameCol = new XViewerColumn("tt.name", "Name", 120, XViewerAlign.Left, true, SortDataType.String, false, null);
		XViewerColumn valueCol = new XViewerColumn("tt.value", "Value", 80, XViewerAlign.Left, true,
				SortDataType.Integer, false, null);
		XViewerColumn noteCol = new XViewerColumn("tt.note", "Note", 80, XViewerAlign.Left, true, SortDataType.String,
				false, "note");
		factory = new XViewerFactory("tt.fixture") {
			@Override
			public boolean isAdmin() {
				return true;
			}
		};
		factory.registerColumns(nameCol, valueCol, noteCol);
		viewer = new XViewer(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER, factory);
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(new Content());
		viewer.setLabelProvider(new Labels(viewer));
		for (int i = 0; i < 12; i++) {
			Node r = new Node(null, "r" + i);
			for (int j = 0; j < 4; j++) {
				r.children.add(new Node(r, "r" + i + "/" + j));
			}
			roots.add(r);
		}
		viewer.setForcePend(true);
		viewer.setInputXViewer(roots);
		viewer.expandToLevel(roots.get(0), 1);
	}

	@Override
	public Object instanceOf(Class<?> type) {
		if (viewer == null) {
			return null;
		}
		if (type == Object.class) {
			return roots.get(1);
		}
		Object[] candidates = { viewer, viewer.getTree(), factory, nameCol, viewer.getCustomizeMgr(),
				firstItem(), firstColumn(), roots.get(1), viewer.getLabelProvider(), viewer.getContentProvider(),
				viewer.getFilterDataUI(), viewer.getColumnFilterDataUI(), customizeData() };
		for (Object c : candidates) {
			if (c != null && type.isInstance(c)) {
				return c;
			}
		}
		return null;
	}

	private CustomizeData customizeData() {
		CustomizeManager mgr = viewer.getCustomizeMgr();
		return mgr == null ? null : mgr.getCurrentCustomizeData();
	}

	private TreeItem firstItem() {
		Tree t = viewer.getTree();
		return t.isDisposed() || t.getItemCount() == 0 ? null : t.getItem(0);
	}

	private TreeColumn firstColumn() {
		Tree t = viewer.getTree();
		return t.isDisposed() || t.getColumnCount() == 0 ? null : t.getColumn(0);
	}

	@Override
	public String idOf(Object o) {
		if (o == viewer) {
			return "viewer";
		}
		if (viewer != null && o == viewer.getTree()) {
			return "tree";
		}
		if (o instanceof TreeItem) {
			StringBuilder path = new StringBuilder();
			TreeItem item = (TreeItem) o;
			while (item != null) {
				TreeItem parent = item.getParentItem();
				int index = parent == null ? item.getParent().indexOf(item) : parent.indexOf(item);
				path.insert(0, "/" + index);
				item = parent;
			}
			return "item" + path;
		}
		if (o instanceof TreeColumn) {
			return "col" + ((TreeColumn) o).getParent().indexOf((TreeColumn) o);
		}
		if (o instanceof Node) {
			return ((Node) o).path;
		}
		if (o instanceof XViewerColumn) {
			return ((XViewerColumn) o).getId();
		}
		return null;
	}

	@Override
	public Object primary() {
		return viewer;
	}

	@Override
	public String skipReason(Member member) {
		String type = member.getDeclaringClass().getSimpleName();
		if (SKIP_TYPE.matcher(type).matches()) {
			return "MODAL_OR_NATIVE_TYPE";
		}
		if (SKIP_NAME.matcher(member.getName()).matches()) {
			return "MODAL_OR_NATIVE_NAME";
		}
		return null;
	}

	private static final class Content implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object input) {
			return input instanceof List ? ((List<?>) input).toArray() : new Object[0];
		}

		@Override
		public Object[] getChildren(Object parent) {
			if (parent instanceof Node) {
				return ((Node) parent).children.toArray();
			}
			return getElements(parent);
		}

		@Override
		public Object getParent(Object element) {
			return element instanceof Node ? ((Node) element).parent : null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof Node && !((Node) element).children.isEmpty();
		}
	}

	private static final class Labels extends XViewerLabelProvider {
		Labels(XViewer viewer) {
			super(viewer);
		}

		@Override
		public Image getColumnImage(Object element, XViewerColumn xCol, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, XViewerColumn xCol, int columnIndex) {
			Node n = (Node) element;
			switch (xCol.getId()) {
			case "tt.name":
				return n.path;
			case "tt.value":
				return Integer.toString(n.path.length() * 7 % 13);
			default:
				return n.children.isEmpty() ? "leaf" : "branch";
			}
		}

		@Override
		public void addListener(org.eclipse.jface.viewers.ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(org.eclipse.jface.viewers.ILabelProviderListener listener) {
		}
	}
}
