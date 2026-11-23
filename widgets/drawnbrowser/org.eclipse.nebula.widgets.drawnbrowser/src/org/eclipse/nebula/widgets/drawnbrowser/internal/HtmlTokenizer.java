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
	static final Runnable UNCHECKED = () -> { };

	private HtmlTokenizer() {
	}

	/** Reports the tokens of {@code html} to {@code handler}. */
	public static void tokenize(String html, Handler handler) {
		tokenize(html, handler, UNCHECKED);
	}

	/* The document parser supplies a detached cancellation checkpoint only for asynchronous input. */
	static void tokenize(String html, Handler handler, Runnable checkpoint) {
		int n = html.length(), i = 0, textStart = 0;
		while (i < n) {
			if ((i & 1023) == 0) checkpoint.run();
			if (html.charAt(i) != '<' || i + 1 >= n) {
				i++;
				continue;
			}
			checkpoint.run();
			char next = html.charAt(i + 1);
			int end;
			if (html.startsWith("<!--", i)) {
				end = indexOf(html, "-->", i + 4, n, checkpoint);
				end = end < 0 ? n : end + 3;
			} else if (next == '!' || next == '?') {
				end = indexOf(html, '>', i, n, checkpoint);
				end = end < 0 ? n : end + 1;
			} else if (next == '/' || Character.isLetter(next)) {
				end = tagEnd(html, i, checkpoint);
				if (end < 0) {
					i++;
					continue;
				}
			} else {
				i++;
				continue;
			}
			if (i > textStart) handler.text(decode(html, textStart, i, checkpoint));
			if (next == '/' || Character.isLetter(next)) {
				String raw = tag(html, i, end, handler, checkpoint);
				if (raw != null) {
					// script and style hold raw text up to their end tag: markup inside is not parsed
					int close = rawEnd(html, raw, end, checkpoint);
					int contentEnd = close < 0 ? n : close;
					if (contentEnd > end) handler.text(html.substring(end, contentEnd));
					end = contentEnd;
				}
			}
			i = textStart = end;
		}
		if (n > textStart) handler.text(decode(html, textStart, n, checkpoint));
	}

	/* Bound asynchronous library scans too; keep the ordinary tokenizer's single intrinsic scan. */
	private static int indexOf(String text, int character, int start, int end, Runnable checkpoint) {
		if (checkpoint == UNCHECKED) return text.indexOf(character, start, end);
		for (int from = start; from < end;) {
			checkpoint.run();
			int limit = from + Math.min(1024, end - from);
			int found = text.indexOf(character, from, limit);
			if (found >= 0) return found;
			from = limit;
		}
		return -1;
	}

	private static int indexOf(String text, String token, int start, int end, Runnable checkpoint) {
		if (checkpoint == UNCHECKED) return text.indexOf(token, start, end);
		for (int from = start; from < end;) {
			checkpoint.run();
			int limit = from + Math.min(1024, end - from);
			// Include the token suffix so a terminator crossing a chunk boundary is still found.
			int found = text.indexOf(token, from, limit + Math.min(token.length() - 1, end - limit));
			if (found >= 0) return found;
			from = limit;
		}
		return -1;
	}

	/* the index after the tag's '>', quotes respected; -1 when the tag never closes */
	private static int tagEnd(String html, int start, Runnable checkpoint) {
		char quote = 0;
		for (int i = start + 1, n = html.length(); i < n; i++) {
			if ((i & 1023) == 0) checkpoint.run();
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
	private static String tag(String html, int start, int end, Handler handler, Runnable checkpoint) {
		int i = start + 1, close = end - 1;
		boolean endTag = html.charAt(i) == '/';
		if (endTag) i++;
		int nameStart = i;
		while (i < close && !Character.isWhitespace(html.charAt(i)) && html.charAt(i) != '/') {
			if ((i & 1023) == 0) checkpoint.run();
			i++;
		}
		String name = html.substring(nameStart, i).toLowerCase(java.util.Locale.ROOT);
		if (endTag) {
			handler.endTag(name);
			return null;
		}
		boolean selfClosing = html.charAt(close - 1) == '/';
		String[] attributes = attributes(html, i, selfClosing ? close - 1 : close, checkpoint);
		handler.startTag(name, attributes, selfClosing);
		return !selfClosing && (name.equals("script") || name.equals("style")) ? name : null;
	}

	/* A raw-text end tag must end its name; </scripture> is not </script>. */
	private static int rawEnd(String html, String name, int from, Runnable checkpoint) {
		String what = "</" + name;
		for (int i = from, last = html.length() - what.length(); i <= last; i++) {
			if ((i & 1023) == 0) checkpoint.run();
			if (!html.regionMatches(true, i, what, 0, what.length())) continue;
			int after = i + what.length();
			if (after < html.length()) {
				char boundary = html.charAt(after);
				if (boundary == '>' || boundary == '/' || Character.isWhitespace(boundary)) return i;
			}
		}
		return -1;
	}

	private static String[] attributes(String html, int i, int end, Runnable checkpoint) {
		String[] pairs = NO_ATTRIBUTES;
		int count = 0;
		while (i < end) {
			checkpoint.run();
			while (i < end && Character.isWhitespace(html.charAt(i))) {
				if ((i & 1023) == 0) checkpoint.run();
				i++;
			}
			int nameStart = i;
			while (i < end && html.charAt(i) != '=' && !Character.isWhitespace(html.charAt(i))) {
				if ((i & 1023) == 0) checkpoint.run();
				i++;
			}
			if (i == nameStart) break;
			String name = html.substring(nameStart, i).toLowerCase(java.util.Locale.ROOT), value = "";
			while (i < end && Character.isWhitespace(html.charAt(i))) {
				if ((i & 1023) == 0) checkpoint.run();
				i++;
			}
			if (i < end && html.charAt(i) == '=') {
				i++;
				while (i < end && Character.isWhitespace(html.charAt(i))) {
					if ((i & 1023) == 0) checkpoint.run();
					i++;
				}
				char quote = i < end ? html.charAt(i) : 0;
				int valueStart, valueEnd;
				if (quote == '"' || quote == '\'') {
					valueStart = i + 1;
					valueEnd = indexOf(html, quote, valueStart, end, checkpoint);
					if (valueEnd < 0 || valueEnd > end) valueEnd = end;
					i = Math.min(end, valueEnd + 1);
				} else {
					valueStart = i;
					while (i < end && !Character.isWhitespace(html.charAt(i))) {
						if ((i & 1023) == 0) checkpoint.run();
						i++;
					}
					valueEnd = i;
				}
				value = decode(html, valueStart, valueEnd, checkpoint);
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

	/*
	 * Text with character references decoded. Named references use bounded lookahead: a document full of '&'
	 * cannot repeatedly scan the unconsumed suffix. Numeric references consume ASCII digits once, and accept
	 * the HTML-standard optional semicolon. Unknown named references remain literal.
	 */
	static String decode(String html, int start, int end) {
		return decode(html, start, end, UNCHECKED);
	}

	private static String decode(String html, int start, int end, Runnable checkpoint) {
		checkpoint.run();
		int amp = indexOf(html, '&', start, end, checkpoint);
		if (amp < 0 || amp >= end) return html.substring(start, end);
		StringBuilder out = new StringBuilder(end - start);
		int i = start;
		while (i < end) {
			checkpoint.run();
			amp = indexOf(html, '&', i, end, checkpoint);
			if (amp < 0 || amp >= end) {
				out.append(html, i, end);
				break;
			}
			out.append(html, i, amp);
			int digitAt = amp + 2;
			if (amp + 1 < end && html.charAt(amp + 1) == '#') {
				int radix = 10;
				if (digitAt < end && (html.charAt(digitAt) == 'x' || html.charAt(digitAt) == 'X')) {
					radix = 16;
					digitAt++;
				}
				int at = digitAt, code = 0;
				while (at < end) {
					if ((at & 1023) == 0) checkpoint.run();
					int digit = asciiDigit(html.charAt(at), radix);
					if (digit < 0) break;
					if (code <= Character.MAX_CODE_POINT) code = code * radix + digit;
					at++;
				}
				if (at > digitAt) {
					out.appendCodePoint(numericCodePoint(code));
					i = at < end && html.charAt(at) == ';' ? at + 1 : at;
					continue;
				}
			}
			// The current named vocabulary fits within a ten-character terminated reference.
			int semi = -1, limit = amp + Math.min(11, end - amp);
			for (int j = amp + 1; j < limit; j++) {
				if (html.charAt(j) == ';') {
					semi = j;
					break;
				}
			}
			if (semi > amp) {
				int code = reference(html.substring(amp + 1, semi));
				if (code >= 0) {
					out.appendCodePoint(code);
					i = semi + 1;
					continue;
				}
			}
			out.append('&');
			i = amp + 1;
		}
		return out.toString();
	}

	private static int asciiDigit(char c, int radix) {
		if (c >= '0' && c <= '9') return c - '0';
		if (radix == 16 && c >= 'a' && c <= 'f') return c - 'a' + 10;
		if (radix == 16 && c >= 'A' && c <= 'F') return c - 'A' + 10;
		return -1;
	}

	/* The HTML numeric-reference end state: U+0000, surrogates and out-of-range values are replacement characters. */
	private static int numericCodePoint(int value) {
		if (value == 0 || value > Character.MAX_CODE_POINT || value >= 0xD800 && value <= 0xDFFF) return 0xFFFD;
		return switch (value) {
			case 0x80 -> 0x20AC;
			case 0x82 -> 0x201A;
			case 0x83 -> 0x0192;
			case 0x84 -> 0x201E;
			case 0x85 -> 0x2026;
			case 0x86 -> 0x2020;
			case 0x87 -> 0x2021;
			case 0x88 -> 0x02C6;
			case 0x89 -> 0x2030;
			case 0x8A -> 0x0160;
			case 0x8B -> 0x2039;
			case 0x8C -> 0x0152;
			case 0x8E -> 0x017D;
			case 0x91 -> 0x2018;
			case 0x92 -> 0x2019;
			case 0x93 -> 0x201C;
			case 0x94 -> 0x201D;
			case 0x95 -> 0x2022;
			case 0x96 -> 0x2013;
			case 0x97 -> 0x2014;
			case 0x98 -> 0x02DC;
			case 0x99 -> 0x2122;
			case 0x9A -> 0x0161;
			case 0x9B -> 0x203A;
			case 0x9C -> 0x0153;
			case 0x9E -> 0x017E;
			case 0x9F -> 0x0178;
			default -> value;
		};
	}

	private static int reference(String name) {
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

}
