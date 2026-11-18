/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License 2.0 or the Eclipse Public License 2.0.
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.drawnbrowser.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The columns of a CSS grid container (CSS Grid Layout 1, the grid track sizing algorithm, columns only): a
 * {@code grid-template-columns} track list and the sizes of its tracks. Pure, Display-free. Pattern: Taffy (MIT,
 * DioxusLabs/taffy 1abd8eeaf6, src/compute/grid/track_sizing.rs), its steps 11.4 initialise track sizes, 11.6
 * maximise tracks, 11.7 expand flexible tracks (11.7.1 find the size of an fr) and 11.8 stretch auto tracks.
 */
public final class GridTracks {

	/**
	 * A track is a length in px (0 to {@link #MAX_PX}), {@link #AUTO}, or {@link #FR} or {@link #PERCENT} with
	 * hundredths (of an fr, of a percent) in the low bits.
	 */
	public static final int AUTO = -1, FR = 1 << 30, PERCENT = 1 << 29, MAX_PX = PERCENT - 1;

	/** At most this many tracks: a longer list (or repeat) is not a template. */
	public static final int MAX_TRACKS = 1000;

	private static final int VALUE = PERCENT - 1, INVALID = Integer.MIN_VALUE;

	private GridTracks() {
	}

	/** Whether a track is flexible (fr). */
	public static boolean isFr(int track) {
		return track >= FR;
	}

	/**
	 * The tracks of a {@code grid-template-columns} value, or null when it is none or not understood (the declaration
	 * is then ignored): px, 0, %, fr (to hundredths), auto, min-content and max-content (as auto), fit-content() (as
	 * auto), minmax(min, max) (as its max), and repeat(count, tracks...). Named lines, auto-fill and auto-fit are not
	 * understood.
	 */
	public static int[] parse(String value) {
		return value == null ? null : list(value.trim().toLowerCase(Locale.ROOT));
	}

	private static int[] list(String text) {
		int[] out = new int[0];
		for (String part : split(text)) {
			int[] tracks = part.startsWith("repeat(") && part.endsWith(")") ? repeat(part.substring(7, part.length() - 1)) : one(part);
			if (tracks == null || out.length + tracks.length > MAX_TRACKS) return null;
			int at = out.length;
			out = java.util.Arrays.copyOf(out, at + tracks.length);
			System.arraycopy(tracks, 0, out, at, tracks.length);
		}
		return out.length == 0 ? null : out;
	}

	/* the space-separated parts at parenthesis depth 0 (a function call stays whole) */
	private static List<String> split(String text) {
		List<String> parts = new ArrayList<>();
		int depth = 0, start = -1;
		for (int i = 0, n = text.length(); i <= n; i++) {
			char c = i < n ? text.charAt(i) : ' ';
			if (c == '(') depth++;
			if (c == ')') depth--;
			boolean blank = Character.isWhitespace(c) && depth == 0;
			if (!blank && start < 0) start = i;
			if (blank && start >= 0) {
				parts.add(text.substring(start, i));
				start = -1;
			}
		}
		return depth == 0 ? parts : List.of("(");
	}

	private static int[] repeat(String arguments) {
		int comma = arguments.indexOf(',');
		if (comma < 0) return null;
		String count = arguments.substring(0, comma).trim();
		if (!count.matches("\\d{1,4}")) return null; // auto-fill and auto-fit are not understood
		int times = Integer.parseInt(count);
		int[] body = list(arguments.substring(comma + 1).trim());
		if (body == null || times == 0 || (long) times * body.length > MAX_TRACKS) return null;
		int[] out = new int[times * body.length];
		for (int k = 0; k < times; k++) System.arraycopy(body, 0, out, k * body.length, body.length);
		return out;
	}

	private static int[] one(String token) {
		int track = track(token);
		return track == INVALID ? null : new int[] { track };
	}

	/* one track size, INVALID when it is not understood */
	private static int track(String token) {
		if (token.startsWith("minmax(") && token.endsWith(")")) {
			String[] bounds = token.substring(7, token.length() - 1).split(",");
			return bounds.length == 2 && track(bounds[0].trim()) != INVALID ? track(bounds[1].trim()) : INVALID;
		}
		if (token.equals("auto") || token.equals("min-content") || token.equals("max-content")
				|| token.startsWith("fit-content(") && token.endsWith(")")) return AUTO;
		if (token.equals("0")) return 0;
		if (token.endsWith("fr")) return flagged(FR, number(token, 2));
		if (token.endsWith("%")) return flagged(PERCENT, number(token, 1));
		if (token.endsWith("px")) {
			int hundredths = number(token, 2);
			return hundredths == INVALID ? INVALID : Math.min(MAX_PX, (hundredths + 50) / 100);
		}
		return INVALID;
	}

	/* the number before a unit of {@code unit} characters, in hundredths; INVALID when it is not one */
	private static int number(String token, int unit) {
		String digits = token.substring(0, token.length() - unit);
		if (!digits.matches("\\d{1,6}(\\.\\d{1,4})?")) return INVALID;
		return (int) Math.min(VALUE, Math.round(Double.parseDouble(digits) * 100));
	}

	private static int flagged(int flag, int hundredths) {
		return hundredths == INVALID ? INVALID : flag | hundredths;
	}

	/**
	 * The x and the width of each column: {@code result[i]} is column i's x from the grid's start and
	 * {@code result[n + i]} its width.
	 * <ul>
	 * <li>A px track is that wide, a percentage of {@code available}; an auto track starts at its items' minimum
	 * and may grow to their content width; an fr track starts at its items' minimum (minmax(auto, Nfr)).</li>
	 * <li>Free space first grows the auto tracks up to their content width, equally (maximise tracks).</li>
	 * <li>The fr tracks then share what the other tracks leave, by their factors (a sum under 1 shares only that
	 * fraction); a track whose share is under its minimum keeps its minimum and the rest share again.</li>
	 * <li>Space still free stretches the auto tracks equally; what is left places the columns by justify-content
	 * ({@link FlexRow} constants), {@code gap} between them. A grid too narrow overflows at the minimums.</li>
	 * </ul>
	 *
	 * @param min per column, the widest minimum content width of its items
	 * @param max per column, the widest unwrapped content width of its items
	 */
	public static int[] layout(int[] tracks, int[] min, int[] max, int available, int gap, int justify) {
		int n = tracks.length;
		int[] out = new int[2 * n];
		if (n == 0) return out;
		long[] base = new long[n], limit = new long[n];
		long space = Math.max(0, available - (long) gap * (n - 1));
		boolean flexible = false;
		for (int i = 0; i < n; i++) {
			int t = tracks[i];
			if (isFr(t)) {
				base[i] = limit[i] = min[i];
				flexible = true;
			} else if (t == AUTO) {
				base[i] = min[i];
				limit[i] = Math.max(min[i], max[i]);
			} else {
				base[i] = limit[i] = t >= PERCENT ? (long) available * (t & VALUE) / 10000 : t;
			}
		}
		maximise(base, limit, space - sum(base));
		if (flexible) expandFlexible(tracks, base, space);
		stretchAuto(tracks, base, space - sum(base));
		long free = space - sum(base);
		long x = free > 0 ? justify == FlexRow.CENTER ? free / 2 : justify == FlexRow.END ? free : 0 : 0;
		long extraGap = free > 0 && justify == FlexRow.SPACE_BETWEEN && n > 1 ? free / (n - 1) : 0;
		for (int i = 0; i < n; i++) {
			out[i] = (int) x;
			out[n + i] = (int) base[i];
			x += base[i] + gap + extraGap;
		}
		return out;
	}

	private static long sum(long[] values) {
		long total = 0;
		for (long v : values) total += v;
		return total;
	}

	/* 11.6: free space to the tracks below their limit, equally, each frozen when it reaches its limit */
	private static void maximise(long[] base, long[] limit, long free) {
		while (free > 0) {
			int growing = 0;
			for (int i = 0; i < base.length; i++) if (base[i] < limit[i]) growing++;
			if (growing == 0) return;
			long done = 0, given = 0;
			for (int i = 0, k = 0; i < base.length; i++) {
				if (base[i] >= limit[i]) continue;
				long share = free * ++k / growing - done; // cumulative: the shares add up to free exactly
				done += share;
				long take = Math.min(share, limit[i] - base[i]);
				base[i] += take;
				given += take;
			}
			free -= given;
			if (given == done) return; // nothing reached its limit: all of it was given
		}
	}

	/* 11.7: the fr tracks share the space the others leave; one whose share is under its base keeps its base */
	private static void expandFlexible(int[] tracks, long[] base, long space) {
		int n = tracks.length;
		boolean[] inflexible = new boolean[n];
		long leftover = 0, factors = 0;
		for (int round = 0; round <= n; round++) {
			leftover = space;
			factors = 0;
			for (int i = 0; i < n; i++) {
				if (isFr(tracks[i]) && !inflexible[i]) factors += tracks[i] & VALUE;
				else leftover -= base[i];
			}
			long divisor = Math.max(factors, 100); // a factor sum under 1 shares only that fraction
			boolean restart = false;
			for (int i = 0; i < n; i++) {
				if (isFr(tracks[i]) && !inflexible[i] && (tracks[i] & VALUE) * leftover < base[i] * divisor) {
					inflexible[i] = true;
					restart = true;
				}
			}
			if (!restart) break;
		}
		if (leftover <= 0 || factors == 0) return;
		long divisor = Math.max(factors, 100), weight = 0, done = 0;
		for (int i = 0; i < n; i++) {
			if (!isFr(tracks[i]) || inflexible[i]) continue;
			weight += tracks[i] & VALUE;
			long share = leftover * weight / divisor - done; // cumulative: the line ends exactly when the factors reach 1
			done += share;
			base[i] = Math.max(base[i], share);
		}
	}

	/* 11.8: space still free shared equally by the auto tracks */
	private static void stretchAuto(int[] tracks, long[] base, long free) {
		if (free <= 0) return;
		int autos = 0;
		for (int t : tracks) if (t == AUTO) autos++;
		long done = 0;
		for (int i = 0, k = 0; i < tracks.length; i++) {
			if (tracks[i] != AUTO) continue;
			long share = free * ++k / autos - done;
			done += share;
			base[i] += share;
		}
	}
}
