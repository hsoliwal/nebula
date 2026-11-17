/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

/**
 * A lenient HTML tokenizer for the tags the drawn browser understands. It never fails: an unclosed tag, a stray
 * {@code <} or an unknown entity is treated as text. Tag and attribute names are lower-cased; comments, the doctype
 * and processing instructions are skipped; character references are decoded.
 * <p>
 * Pattern: prasser/swtbrowser (Apache-2.0, 92120bef61) parses with TagSoup's SAX parser; this tokenizer keeps the
 * same callbacks (start tag with attributes, end tag, characters) without a dependency, since a Nebula widget
 * depends on SWT alone.
 * </p>
 */
public final class HtmlTokenizer {

	/** What the tokenizer reports, in document order. */
	public interface Handler {
		/**
		 * A start tag; {@code attributes} holds name and value pairs; a self-closing tag reports
		 * {@code selfClosing}.
		 */
		void startTag(String name, String[] attributes, boolean selfClosing);

		void endTag(String name);

		/** Text, entities decoded, whitespace as written. */
		void text(String text);
	}

	private static final String[] NO_ATTRIBUTES = new String[0];

	private HtmlTokenizer() {
	}

	/** Reports the tokens of {@code html} to {@code handler}. */
	public static void tokenize(String html, Handler handler) {
		int n = html.length(), i = 0, textStart = 0;
		while (i < n) {
			if (html.charAt(i) != '<' || i + 1 >= n) {
				i++;
				continue;
			}
			char next = html.charAt(i + 1);
			int end;
			if (html.startsWith("<!--", i)) {
				end = html.indexOf("-->", i + 4);
				end = end < 0 ? n : end + 3;
			} else if (next == '!' || next == '?') {
				end = html.indexOf('>', i);
				end = end < 0 ? n : end + 1;
			} else if (next == '/' || Character.isLetter(next)) {
				end = tagEnd(html, i);
				if (end < 0) {
					i++;
					continue;
				}
			} else {
				i++;
				continue;
			}
			if (i > textStart) handler.text(decode(html, textStart, i));
			if (next == '/' || Character.isLetter(next)) {
				String raw = tag(html, i, end, handler);
				if (raw != null) {
					// script and style hold raw text up to their end tag: markup inside is not parsed
					int close = indexOfIgnoreCase(html, "</" + raw, end);
					int contentEnd = close < 0 ? n : close;
					if (contentEnd > end) handler.text(html.substring(end, contentEnd));
					end = contentEnd;
				}
			}
			i = textStart = end;
		}
		if (n > textStart) handler.text(decode(html, textStart, n));
	}

	/* the index after the tag's '>', quotes respected; -1 when the tag never closes */
	private static int tagEnd(String html, int start) {
		char quote = 0;
		for (int i = start + 1, n = html.length(); i < n; i++) {
			char c = html.charAt(i);
			if (quote != 0) {
				if (c == quote) quote = 0;
			} else if (c == '"' || c == '\'') {
				quote = c;
			} else if (c == '>') {
				return i + 1;
			}
		}
		return -1;
	}

	/* reports one tag; answers the name of a raw-text element (script, style) whose content follows, else null */
	private static String tag(String html, int start, int end, Handler handler) {
		int i = start + 1, close = end - 1;
		boolean endTag = html.charAt(i) == '/';
		if (endTag) i++;
		int nameStart = i;
		while (i < close && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '/') i++;
		String name = html.substring(nameStart, i).toLowerCase(java.util.Locale.ROOT);
		if (endTag) {
			handler.endTag(name);
			return null;
		}
		boolean selfClosing = html.charAt(close - 1) == '/';
		String[] attributes = attributes(html, i, selfClosing ? close - 1 : close);
		handler.startTag(name, attributes, selfClosing);
		return !selfClosing && (name.equals("script") || name.equals("style")) ? name : null;
	}

	private static int indexOfIgnoreCase(String html, String what, int from) {
		for (int i = from, last = html.length() - what.length(); i <= last; i++) {
			if (html.regionMatches(true, i, what, 0, what.length())) return i;
		}
		return -1;
	}

	private static String[] attributes(String html, int i, int end) {
		String[] pairs = NO_ATTRIBUTES;
		int count = 0;
		while (i < end) {
			while (i < end && Character.isWhitespace(html.charAt(i))) i++;
			int nameStart = i;
			while (i < end && html.charAt(i) != '=' && !Character.isWhitespace(html.charAt(i))) i++;
			if (i == nameStart) break;
			String name = html.substring(nameStart, i).toLowerCase(java.util.Locale.ROOT), value = "";
			while (i < end && Character.isWhitespace(html.charAt(i))) i++;
			if (i < end && html.charAt(i) == '=') {
				i++;
				while (i < end && Character.isWhitespace(html.charAt(i))) i++;
				char quote = i < end ? html.charAt(i) : 0;
				int valueStart, valueEnd;
				if (quote == '"' || quote == '\'') {
					valueStart = i + 1;
					valueEnd = html.indexOf(quote, valueStart);
					if (valueEnd < 0 || valueEnd > end) valueEnd = end;
					i = Math.min(end, valueEnd + 1);
				} else {
					valueStart = i;
					while (i < end && !Character.isWhitespace(html.charAt(i))) i++;
					valueEnd = i;
				}
				value = decode(html, valueStart, valueEnd);
			}
			if (2 * count + 2 > pairs.length) pairs = java.util.Arrays.copyOf(pairs, Math.max(4, 2 * pairs.length));
			pairs[2 * count] = name;
			pairs[2 * count + 1] = value;
			count++;
		}
		return count == 0 ? NO_ATTRIBUTES : java.util.Arrays.copyOf(pairs, 2 * count);
	}

	/** The value of an attribute, or null. */
	public static String attribute(String[] attributes, String name) {
		for (int i = 0; i + 1 < attributes.length; i += 2) {
			if (attributes[i].equals(name)) return attributes[i + 1];
		}
		return null;
	}

	/* text with character references decoded; an unknown reference stays as written */
	static String decode(String html, int start, int end) {
		int amp = html.indexOf('&', start);
		if (amp < 0 || amp >= end) return html.substring(start, end);
		StringBuilder out = new StringBuilder(end - start);
		int i = start;
		while (i < end) {
			char c = html.charAt(i);
			int semi = c == '&' ? html.indexOf(';', i) : -1;
			if (semi > i && semi < end && semi - i <= 10) {
				int code = reference(html.substring(i + 1, semi));
				if (code >= 0) {
					out.appendCodePoint(code);
					i = semi + 1;
					continue;
				}
			}
			out.append(c);
			i++;
		}
		return out.toString();
	}

	private static int reference(String name) {
		if (name.startsWith("#x") || name.startsWith("#X")) return number(name.substring(2), 16);
		if (name.startsWith("#")) return number(name.substring(1), 10);
		switch (name) {
			case "amp": return '&';
			case "lt": return '<';
			case "gt": return '>';
			case "quot": return '"';
			case "apos": return '\'';
			case "nbsp": return '\u00a0';
			case "copy": return '\u00a9';
			case "reg": return '\u00ae';
			case "mdash": return '\u2014';
			case "ndash": return '\u2013';
			case "hellip": return '\u2026';
			case "laquo": return '\u00ab';
			case "raquo": return '\u00bb';
			default: return -1;
		}
	}

	private static int number(String digits, int radix) {
		try {
			int code = Integer.parseInt(digits, radix);
			return Character.isValidCodePoint(code) ? code : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
