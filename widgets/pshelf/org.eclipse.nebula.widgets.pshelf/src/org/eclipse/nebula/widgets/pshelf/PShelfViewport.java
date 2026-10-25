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
package org.eclipse.nebula.widgets.pshelf;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.nebula.widgets.pshelf.internal.WidgetMode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Viewport of a {@link PShelf} (docs/m3/every-widget).
 * <ul>
 * <li>Aspect H: the stock animateOpen is a synchronous loop of about {@code 1 / animationSpeed} frames (50 at the
 * default 0.02); every frame moves the two animated body parents, hides the others, redraws and updates the shelf, also
 * while the shelf is hidden. A hidden shelf (its own or an ancestor's visibility, a zero size, a minimized shell, or
 * SWT.PartHidden on the M3 SWT fork) jumps to the final frame instead: {@link #replay} runs the same double accumulation
 * of the speed and the same int arithmetic as {@link PShelfViewportGeometry#animated} and sizeClients without native
 * calls, and each body parent then receives once the bounds of the last frame that showed it and the visibility of the
 * last frame. Every body, body parent, the selection, the Selection event and computeSize end as after the stock loop.</li>
 * <li>Aspect D: the full redraw of a hover change (MouseMove, MouseExit) and of {@link PShelfItem#setText} becomes the
 * damage of the header rects that change.</li>
 * <li>Aspect R: a paint skips the renderer paint of a header outside its clip; renderer.setBounds, setSelected,
 * setFocus and setHover still run for every header, so the public renderer state after a paint is the stock one. The
 * focused header (its focus ring is drawn at the top of the shelf) and the last header (the GC state after the paint)
 * are always painted.</li>
 * </ul>
 * D and R apply only to the exact built-in renderers ({@link PaletteShelfRenderer}, {@link RedmondShelfRenderer}),
 * only while their text fits the item height (no pixel outside a header's rect), and only while nothing the stock
 * widget shows on its next full repaint alone has changed since the last full paint: renderer colours and fonts (their
 * setters do not redraw), focus (the shelf has no focus listener), the header lane, the item height, the selection and
 * the style. Otherwise the stock full redraw and paint run.
 * <p>
 * {@code -Dswt.widget.mode.PShelf=original} (or the global {@code swt.widget.mode=original}) keeps the stock widget.
 * </p>
 */
final class PShelfViewport implements Listener {

	/** True unless the startup mode of PShelf is original. */
	static final boolean ENABLED = WidgetMode.of("PShelf", WidgetMode.VIEWPORT) == WidgetMode.VIEWPORT; //$NON-NLS-1$

	/** Lanes of a {@link #replay} result: flags, then the y and height of the last bounds of each animated body parent. */
	static final int FLAGS = 0, PREVIOUS_Y = 1, PREVIOUS_HEIGHT = 2, NEXT_Y = 3, NEXT_HEIGHT = 4;
	/** {@link #FLAGS} bits: a frame showed the body parent; the last frame shows it. */
	static final int PREVIOUS_SHOWN = 1, PREVIOUS_LAST = 2, NEXT_SHOWN = 4, NEXT_LAST = 8;

	private static final int NONE = 0, MOVE = 1, EXIT = 2;
	private static final int[] EVENTS = { SWT.Paint, SWT.MouseMove, SWT.MouseExit, SWT.Resize, SWT.Dispose };
	/* Control.isPartVisible of the M3 SWT fork (brief rule 25); absent on stock SWT */
	private static final java.lang.reflect.Method IS_PART_VISIBLE = partVisibleMethod();

	private final PShelf shelf;
	private final int[] frame = new int[5];
	private int clientWidth, clientHeight;

	/* what the last full paint showed; the stock widget shows a change of these on its next full repaint only */
	private final Object[] paintedLook = new Object[12];
	private Object paintedRenderer, paintedLane, paintedOpen, paintedFocus;
	private int paintedItemHeight, paintedStyle;
	private boolean paintedFocusControl, exact, fits;
	private Object measuredRenderer;
	private Font measuredFont, measuredSelectedFont;
	private int measuredHeight;

	/* the paint pass */
	private boolean cull;
	private long clipTop, clipBottom;

	/* the hover redraw of the stock MouseMove and MouseExit listeners, which run after ours */
	private int armed;
	private PShelfItem hoverBefore;
	private boolean damagePending;

	/** Hidden opens that jumped to the final frame (tests and benches). */
	int jumps;
	/** Animation frames the jumps did not run (tests and benches). */
	long framesSkipped;
	/** Hover changes and setText calls that damaged header rects instead of the whole shelf (tests and benches). */
	int headerDamages;
	/** Hover changes and setText calls that kept the stock full redraw (tests and benches). */
	int fullRedraws;
	/** Header paints skipped outside the clip (tests and benches). */
	int culled;
	/** Partial paints that found a silent change after a header damage and asked for the stock full repaint. */
	int heals;

	/** Created by the PShelf constructor before its Paint, MouseMove and MouseExit listeners, so ours run first. */
	PShelfViewport(final PShelf shelf) {
		this.shelf = shelf;
		clientResized();
		for (final int type : EVENTS) {
			shelf.addListener(type, this);
		}
	}

	@Override
	public void handleEvent(final Event event) {
		switch (event.type) {
		case SWT.Paint:
			paintStarts(event.gc);
			break;
		case SWT.MouseMove:
			arm(MOVE);
			break;
		case SWT.MouseExit:
			arm(EXIT);
			break;
		case SWT.Resize:
			clientResized();
			break;
		case SWT.Dispose:
			disposed();
			break;
		default:
			break;
		}
	}

	/**
	 * Start of the stock animateOpen. While the shelf is hidden, gives the body parents the final frame of the stock loop
	 * and returns true (the stock loop then runs no frame); returns false while the shelf is shown.
	 */
	boolean jumpsToFinalFrame(final PShelfItem previousItem, final PShelfItem newItem, final int itemHeight,
			final double speed, final boolean openFromBottom) {
		if (!(speed > 0) || shown()) {
			return false; // a speed of zero or less never ends in the stock loop; NaN ends there after one frame
		}
		final ArrayList<PShelfItem> items = shelf.m3Items();
		final int previous = items.indexOf(previousItem), next = items.indexOf(newItem);
		if (previous < 0 || previous >= next) {
			return false;
		}
		final Rectangle client = shelf.getClientArea();
		framesSkipped += replay(frame, items.size(), previous, next, client.y, client.height, itemHeight, speed,
				openFromBottom);
		jumps++;
		final int flags = frame[FLAGS];
		for (int i = 0; i < items.size(); i++) {
			final Composite bodyParent = items.get(i).getBodyParent();
			if (i == previous) {
				settle(bodyParent, (flags & PREVIOUS_SHOWN) != 0, (flags & PREVIOUS_LAST) != 0, frame[PREVIOUS_Y],
						frame[PREVIOUS_HEIGHT], client.width);
			} else if (i == next) {
				settle(bodyParent, (flags & NEXT_SHOWN) != 0, (flags & NEXT_LAST) != 0, frame[NEXT_Y],
						frame[NEXT_HEIGHT], client.width);
			} else {
				bodyParent.setVisible(false); // every stock frame hides it: it follows its neighbour directly
			}
		}
		return true;
	}

	/**
	 * The final frame of the stock animateOpen loop without its native work: the same double accumulation of the speed and
	 * the same int arithmetic as {@link PShelfViewportGeometry#animated} and PShelf.sizeClients. In every frame only the
	 * body parents at the previous and the next index can have a positive height ({@code space} and
	 * {@code area - space}; every other header follows its neighbour directly), so the result is the last bounds that
	 * showed each of these two and whether the last frame shows it.
	 *
	 * @return the number of frames of the stock loop
	 */
	static long replay(final int[] out, final int count, final int previous, final int next, final int clientY,
			final int clientHeight, final int itemHeight, final double speed, final boolean openFromBottom) {
		final int area = clientHeight - itemHeight * count;
		final int previousY = clientY + (previous + 1) * itemHeight;
		final int nextY = clientY + (next + 1) * itemHeight;
		Arrays.fill(out, 0);
		int flags = 0;
		long frames = 0;
		for (double progress = 0; progress < 1; progress += speed) {
			final int space = (int) (area * (openFromBottom ? 1 - progress : progress));
			flags &= ~(PREVIOUS_LAST | NEXT_LAST);
			if (space > 0) {
				flags |= PREVIOUS_SHOWN | PREVIOUS_LAST;
				out[PREVIOUS_Y] = previousY;
				out[PREVIOUS_HEIGHT] = space;
			}
			if (area - space > 0) {
				flags |= NEXT_SHOWN | NEXT_LAST;
				out[NEXT_Y] = nextY + space;
				out[NEXT_HEIGHT] = area - space;
			}
			frames++;
		}
		out[FLAGS] = flags;
		return frames;
	}

	private static void settle(final Composite bodyParent, final boolean shownOnce, final boolean shownLast, final int y,
			final int height, final int width) {
		if (shownOnce) {
			bodyParent.setVisible(true);
			bodyParent.setBounds(0, y, width, height);
		}
		if (!shownLast) {
			bodyParent.setVisible(false);
		}
	}

	/** {@link PShelf#redraw()}: true when it is the redraw of a hover change and the changed header rects were damaged. */
	boolean redrawsHover() {
		final int was = armed;
		armed = NONE;
		final PShelfItem hover = shelf.m3Hover();
		if (was == NONE || was == MOVE && hover == hoverBefore) {
			return false; // not the redraw of a hover change
		}
		if (!clean()) {
			fullRedraws++;
			return false;
		}
		damage(hoverBefore);
		damage(hover);
		headerDamages++;
		return true;
	}

	/** {@link PShelfItem#setText}: true when the item's header rect was damaged instead of the whole shelf. */
	boolean damageHeader(final PShelfItem item) {
		if (!shelf.isVisible() || !clean()) {
			fullRedraws++;
			return false; // hidden: the stock redraw does no native work there
		}
		damage(item);
		headerDamages++;
		return true;
	}

	/** onPaint before renderer.paint: true when the header misses the clip, so its paint changes no pixel. */
	boolean skips(final int y, final int height, final boolean focus, final boolean last) {
		if (!cull || focus || last || (long) y + height > clipTop && y < clipBottom) {
			return false;
		}
		culled++;
		return true;
	}

	private void paintStarts(final GC gc) {
		cull = false;
		if (gc == null) {
			return;
		}
		final Rectangle clip = gc.getClipping();
		clipTop = clip.y;
		clipBottom = (long) clip.y + clip.height;
		if (clip.x <= 0 && clip.y <= 0 && (long) clip.x + clip.width >= clientWidth && clipBottom >= clientHeight) {
			record(gc);
			cull = exact && fits;
		} else {
			cull = clean();
			if (!cull && damagePending) {
				heals++;
				shelf.redraw(); // a silent change after our damage: the stock full redraw would show it everywhere
			}
		}
		damagePending = false;
	}

	private void record(final GC gc) {
		final AbstractRenderer renderer = shelf.getRenderer();
		final Class<?> type = renderer.getClass();
		exact = type == PaletteShelfRenderer.class || type == RedmondShelfRenderer.class;
		paintedRenderer = renderer;
		paintedLane = shelf.m3Lane();
		paintedItemHeight = shelf.m3ItemHeight();
		paintedOpen = shelf.m3Open();
		paintedFocus = shelf.m3Focus();
		paintedStyle = shelf.getStyle();
		paintedFocusControl = shelf.isFocusControl();
		final int looks = exact ? looks(renderer) : 0;
		for (int i = 0; i < looks; i++) {
			paintedLook[i] = look(renderer, i);
		}
		fits = exact && textHeight(gc, renderer) <= paintedItemHeight;
	}

	private boolean clean() {
		final AbstractRenderer renderer = shelf.getRenderer();
		if (!exact || !fits || renderer != paintedRenderer || shelf.m3Lane() != paintedLane
				|| shelf.m3ItemHeight() != paintedItemHeight || shelf.m3Open() != paintedOpen
				|| shelf.m3Focus() != paintedFocus || shelf.getStyle() != paintedStyle
				|| shelf.isFocusControl() != paintedFocusControl) {
			return false;
		}
		final int looks = looks(renderer);
		for (int i = 0; i < looks; i++) {
			if (paintedLook[i] != look(renderer, i)) {
				return false;
			}
		}
		return true;
	}

	/** Number of fields of an exact built-in renderer that change its pixels; their setters do not redraw. */
	private static int looks(final AbstractRenderer renderer) {
		return renderer instanceof RedmondShelfRenderer ? 12 : 1;
	}

	private static Object look(final AbstractRenderer renderer, final int i) {
		if (!(renderer instanceof RedmondShelfRenderer)) {
			return ((PaletteShelfRenderer) renderer).getShadeColor();
		}
		final RedmondShelfRenderer redmond = (RedmondShelfRenderer) renderer;
		switch (i) {
		case 0: return redmond.getLineColor();
		case 1: return redmond.getFont();
		case 2: return redmond.getGradient1();
		case 3: return redmond.getGradient2();
		case 4: return redmond.getHoverGradient1();
		case 5: return redmond.getHoverGradient2();
		case 6: return redmond.getSelectedFont();
		case 7: return redmond.getSelectedForeground();
		case 8: return redmond.getHoverForeground();
		case 9: return redmond.getForeground();
		case 10: return redmond.getSelectedGradient1();
		default: return redmond.getSelectedGradient2();
		}
	}

	/** Tallest text line of an exact built-in renderer, measured on the paint GC once per font. */
	private int textHeight(final GC gc, final AbstractRenderer renderer) {
		if (renderer instanceof RedmondShelfRenderer) {
			final RedmondShelfRenderer redmond = (RedmondShelfRenderer) renderer;
			final Font font = redmond.getFont(), selected = redmond.getSelectedFont();
			if (measuredRenderer != renderer || measuredFont != font || measuredSelectedFont != selected) {
				final Font restore = gc.getFont();
				gc.setFont(font);
				final int height = gc.getFontMetrics().getHeight();
				gc.setFont(selected);
				measuredHeight = Math.max(height, gc.getFontMetrics().getHeight());
				gc.setFont(restore);
				remember(renderer, font, selected);
			}
			return measuredHeight;
		}
		final Font font = gc.getFont(); // PaletteShelfRenderer draws with the paint GC's font, the shelf's font
		if (measuredRenderer != renderer || measuredFont == null || !measuredFont.equals(font)) {
			measuredHeight = gc.getFontMetrics().getHeight();
			remember(renderer, font, null);
		}
		return measuredHeight;
	}

	private void remember(final AbstractRenderer renderer, final Font font, final Font selected) {
		measuredRenderer = renderer;
		measuredFont = font;
		measuredSelectedFont = selected;
	}

	private void damage(final PShelfItem item) {
		final int index = item == null ? -1 : shelf.m3Items().indexOf(item);
		final int[] lane = shelf.m3Lane();
		if (index >= 0 && index < lane.length) {
			shelf.redraw(0, lane[index], clientWidth, shelf.m3ItemHeight(), false);
			damagePending = true;
		}
	}

	private void arm(final int kind) {
		armed = kind;
		hoverBefore = shelf.m3Hover();
	}

	private void clientResized() {
		final Rectangle client = shelf.getClientArea();
		clientWidth = client.width;
		clientHeight = client.height;
	}

	private void disposed() {
		for (final int type : EVENTS) {
			shelf.removeListener(type, this);
		}
		Arrays.fill(paintedLook, null);
		paintedRenderer = paintedLane = paintedOpen = paintedFocus = measuredRenderer = null;
		measuredFont = measuredSelectedFont = null;
		hoverBefore = null;
	}

	private boolean shown() {
		if (!shelf.isVisible()) {
			return false; // the shelf or an ancestor is hidden (setVisible, tab and stack pages)
		}
		final Point size = shelf.getSize();
		if (size.x <= 0 || size.y <= 0 || shelf.getShell().getMinimized()) {
			return false;
		}
		if (IS_PART_VISIBLE != null) {
			try {
				return (Boolean) IS_PART_VISIBLE.invoke(shelf);
			} catch (final ReflectiveOperationException e) {
				return true;
			}
		}
		return true;
	}

	private static java.lang.reflect.Method partVisibleMethod() {
		try {
			return Control.class.getMethod("isPartVisible"); //$NON-NLS-1$
		} catch (final ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
