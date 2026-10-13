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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Widget;

/**
 * Loader-independent canonical rendering of probe results. Identity hash codes
 * never appear; widgets render through the fixture's stable ids.
 */
final class Canon {
	private static final int MAX_ELEMENTS = 64;
	private static final int MAX_STRING = 240;

	private Canon() {
	}

	static String of(Object value, TtFixture fixture) {
		StringBuilder out = new StringBuilder();
		append(out, value, fixture, 0);
		return out.toString();
	}

	private static void append(StringBuilder out, Object v, TtFixture f, int depth) {
		if (v == null) {
			out.append("null");
		} else if (depth > 4) {
			out.append('~').append(v.getClass().getSimpleName());
		} else if (v instanceof String) {
			appendString(out, (String) v);
		} else if (v instanceof Number || v instanceof Boolean || v instanceof Character) {
			out.append(v);
		} else if (v instanceof Enum) {
			out.append(((Enum<?>) v).name());
		} else if (v.getClass().isArray()) {
			appendArray(out, v, f, depth);
		} else if (v instanceof Collection) {
			appendIterable(out, 'C', ((Collection<?>) v).toArray(), f, depth);
		} else if (v instanceof Map) {
			appendMap(out, (Map<?, ?>) v, f, depth);
		} else if (v instanceof Point || v instanceof Rectangle || v instanceof RGB || v instanceof RGBA) {
			out.append(v);
		} else if (v instanceof Color) {
			out.append(((Color) v).isDisposed() ? "Color(disposed)" : "Color" + ((Color) v).getRGBA());
		} else if (v instanceof Font) {
			appendFont(out, (Font) v);
		} else if (v instanceof Image) {
			out.append(((Image) v).isDisposed() ? "Image(disposed)" : "Image" + ((Image) v).getBounds());
		} else if (v instanceof Widget) {
			appendWidget(out, (Widget) v, f);
		} else {
			appendObject(out, v, f);
		}
	}

	private static void appendString(StringBuilder out, String s) {
		if (s.length() > MAX_STRING) {
			out.append("S(len=").append(s.length()).append(",sha=").append(sha(s)).append(')');
			return;
		}
		out.append('"').append(s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")
				.replace("\t", "\\t")).append('"');
	}

	private static void appendArray(StringBuilder out, Object array, TtFixture f, int depth) {
		int n = Array.getLength(array);
		Object[] boxed = new Object[n];
		for (int i = 0; i < n; i++) {
			boxed[i] = Array.get(array, i);
		}
		appendIterable(out, 'A', boxed, f, depth);
	}

	private static void appendIterable(StringBuilder out, char tag, Object[] items, TtFixture f, int depth) {
		out.append(tag).append(items.length).append('[');
		int shown = Math.min(items.length, MAX_ELEMENTS);
		for (int i = 0; i < shown; i++) {
			if (i > 0) {
				out.append(',');
			}
			append(out, items[i], f, depth + 1);
		}
		if (shown < items.length) {
			StringBuilder rest = new StringBuilder();
			for (int i = shown; i < items.length; i++) {
				append(rest, items[i], f, depth + 1);
				rest.append(',');
			}
			out.append(",...sha=").append(sha(rest.toString()));
		}
		out.append(']');
	}

	private static void appendMap(StringBuilder out, Map<?, ?> map, TtFixture f, int depth) {
		List<String> entries = new ArrayList<>();
		for (Map.Entry<?, ?> e : map.entrySet()) {
			entries.add(of(e.getKey(), f) + "=" + of(e.getValue(), f));
		}
		entries.sort(null);
		out.append("M").append(entries.size()).append(entries.size() > MAX_ELEMENTS
				? "{sha=" + sha(String.join(",", entries)) + "}" : entries.toString());
	}

	private static void appendFont(StringBuilder out, Font font) {
		if (font.isDisposed()) {
			out.append("Font(disposed)");
			return;
		}
		out.append("Font[");
		for (FontData d : font.getFontData()) {
			out.append(d.getName()).append('/').append(d.getHeight()).append('/').append(d.getStyle()).append(';');
		}
		out.append(']');
	}

	private static void appendWidget(StringBuilder out, Widget w, TtFixture f) {
		String name = w.getClass().getSimpleName();
		if (w.isDisposed()) {
			out.append(name).append("(disposed)");
			return;
		}
		String id = f == null ? null : f.idOf(w);
		out.append(name).append('#').append(id == null ? "?" : id);
	}

	private static void appendObject(StringBuilder out, Object v, TtFixture f) {
		String id = f == null ? null : f.idOf(v);
		if (id != null) {
			out.append(v.getClass().getSimpleName()).append('#').append(id);
			return;
		}
		if (overridesToString(v.getClass())) {
			String s;
			try {
				s = String.valueOf(v);
			} catch (RuntimeException ex) {
				s = "toString!" + ex.getClass().getSimpleName();
			}
			appendString(out, stripIdentity(s));
			return;
		}
		out.append("obj:").append(v.getClass().getName());
	}

	static String stripIdentity(String s) {
		return s.replaceAll("@[0-9a-f]{4,}", "@#").replaceAll("\\{Id=\\d+", "{Id=#");
	}

	private static boolean overridesToString(Class<?> c) {
		try {
			Method m = c.getMethod("toString");
			return m.getDeclaringClass() != Object.class;
		} catch (NoSuchMethodException ex) {
			return false;
		}
	}

	static String sha(String s) {
		try {
			byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (int i = 0; i < 6; i++) {
				hex.append(String.format("%02x", d[i] & 0xff));
			}
			return hex.toString();
		} catch (Exception ex) {
			return "nosha";
		}
	}
}
