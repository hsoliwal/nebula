/*******************************************************************************
 *  Copyright (c) 2010, 2012 Weltevree Beheer BV, Remain Software & Industrial-TSI
 *
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Wim S. Jongman - initial API and implementation
 ******************************************************************************/
package org.eclipse.nebula.widgets.oscilloscope.multichannel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

/**
 * Animated widget that tries to mimic an Oscilloscope.
 *
 * <i>An oscilloscope (also known as a scope, CRO or, an O-scope) is a type of
 * electronic test instrument that allows observation of constantly varying
 * signal voltages, usually as a two-dimensional graph of one or more electrical
 * potential differences using the vertical or 'Y' axis, plotted as a function
 * of time, (horizontal or 'x' axis).</i>
 * <p/>
 * <a href="http://en.wikipedia.org/wiki/Oscilloscope">http://en.wikipedia.org/
 * wiki/Oscilloscope<a/>
 *
 * @author Wim.Jongman (@remainsoftware.com)
 *
 */
public class Oscilloscope extends Canvas {

	/**
	 * This class holds the data per channel.
	 *
	 * @author Wim Jongman
	 *
	 */
	private class Data {

		private int base;
		private int baseOffset = BASE_CENTER;
		private boolean connect;
		private int cursor = CURSOR_START_DEFAULT;
		private OscilloscopeDispatcher dispatcher;
		private boolean fade;
		private Color fg;
		private int height = DEFAULT_HEIGHT;
		private int lineWidth = LINE_WIDTH_DEFAULT;
		private int originalSteadyPosition = STEADYPOSITION_75PERCENT;

		/**
		 * This contains the actual values that where input by the user before
		 * scaling. If the user resized we can calculate how the tail would have
		 * looked with the new window dimensions.
		 *
		 * @see Oscilloscope#tail
		 */
		private int originalTailSize;

		private boolean percentage = false;
		private int progression = PROGRESSION_DEFAULT;
		private IntegerFiFoCircularStack stack;
		private List<OscilloscopeStackAdapter> stackListeners;
		private boolean steady;
		/**
		 * This contains the old or historical input and is used to paint the
		 * tail of the graph.
		 */
		private int[] tail;
		private int tailFade = TAILFADE_PERCENTAGE;
		private int tailSize;
		private int width = DEFAULT_WIDTH;
		private boolean antiAlias = false;

		/** Frame lines, reused while their length does not change (steady scopes allocate nothing per frame). */
		private int[] l1 = NO_POINTS;
		private int[] l2 = NO_POINTS;
		/** Envelope mode (see {@link Oscilloscope#setSamplesPerColumn(int, int)}); 0 = off. */
		private int samplesPerColumn;
		/** Guards stack + env together in envelope mode. */
		private final Object envLock = new Object();
		/** first/min/max of each queued column, in lock step with {@link #stack} (null when off). */
		private EnvelopeRing env;
		/** first/min/max per tail entry, three ints per entry, in lock step with {@link #tail}. */
		private int[] tailEnv;
		private int accFirst;
		private int accMin;
		private int accMax;
		private int accCount;

	}

	private static final int[] NO_POINTS = new int[0];

	/**
	 * The stack can hold a limited number of values but will never overflow.
	 * Think of the stack as a tube with a given length. If you push too many
	 * values in, the oldest will be pushed out at the end.
	 *
	 */
	public class IntegerFiFoCircularStack {
		private int bottom;
		private final int capacity;
		private final int[] stack;
		private int storedValues;
		private int top;

		/**
		 * Creates a stack with the indicated capacity.
		 *
		 * @param capacity
		 *            must be greater than 1
		 */
		public IntegerFiFoCircularStack(int capacity) {
			if (capacity <= 1) {
				throw new RuntimeException("Stack capacity must be > 1");
			}
			this.capacity = capacity;
			stack = new int[capacity];
			top = 0;
			bottom = 0;
		}

		/**
		 * Creates stack with the indicated capacity and copies the old stack
		 * into the new stack and the old stack will be empty after this action.
		 *
		 * @param capacity
		 *            must be greater than 1
		 * @param oldStack
		 */
		public IntegerFiFoCircularStack(int capacity, IntegerFiFoCircularStack oldStack) {
			this(capacity);
			while (!oldStack.isEmpty()) {
				push(oldStack.pop(0));
			}
		}

		/**
		 * Clears the stack.
		 */
		public void clear() {
			synchronized (stack) {
				for (int i = 0; i < stack.length; i++) {
					stack[i] = 0;
				}
				top = 0;
				bottom = 0;
			}
		}

		public int getCapacity() {
			return capacity;
		}

		/**
		 *
		 * @return boolean
		 */
		public int getLoad() {
			return storedValues;
		}

		/**
		 *
		 * @return boolean
		 */
		public boolean isEmpty() {
			return storedValues == 0;
		}

		/**
		 *
		 * @return boolean
		 */
		public boolean isFull() {
			return storedValues == capacity;
		}

		/**
		 * Returns the oldest value from the stack without removing the value
		 * from the stack. Returns the supplied entry if the stack is empty.
		 *
		 * @param valueIfEmpty
		 * @return int
		 */
		public int peek(int valueIfEmpty) {
			if (storedValues > 0) {
				return stack[bottom];
			}
			return valueIfEmpty;
		}

		/**
		 * Returns the oldest value from the stack. Returns the supplied entry
		 * if the stack is empty.
		 *
		 * @param valueIfEmpty
		 * @return int
		 */
		public int pop(int valueIfEmpty) {
			if (isEmpty()) {
				return valueIfEmpty;
			}

			storedValues--;
			int result = stack[bottom++];

			if (bottom == capacity) {
				bottom = 0;
			}

			return result;
		}

		/**
		 * Returns the oldest value from the stack and negates the value.
		 * Returns the supplied entry if the stack is empty.
		 *
		 * @param valueIfEmpty
		 * @return int
		 */
		public int popNegate(int valueIfEmpty) {
			return pop(valueIfEmpty) * -1;
		}

		/**
		 * Puts a value on the stack.
		 *
		 * @param value
		 */
		public void push(int value) {
			if (storedValues == capacity) {
				top = bottom;
				bottom++;
				if (bottom == capacity) {
					bottom = 0;
				}
			} else {
				storedValues++;
			}

			if (top == capacity) {
				top = 0;
			}

			stack[top++] = value;
		}
	}

	/**
	 * The base of the line is positioned at the center of the widget.
	 *
	 * @see #setBaseOffset(int)
	 */
	public static final int BASE_CENTER = 50;

	/**
	 * The default cursor starting position.
	 */
	public static final int CURSOR_START_DEFAULT = 50;

	/**
	 * The default comfortable widget height.
	 */
	public static final int DEFAULT_HEIGHT = 100;

	/**
	 * The default comfortable widget width.
	 */
	public static final int DEFAULT_WIDTH = 180;

	/**
	 * This set of values will draw a figure that is similar to the heart beat
	 * that you see on hospital monitors.
	 */
	public static final int[] HEARTBEAT = new int[] { 2, 10, 2, -16, 16, 44, 49, 44, 32, 14, -16, -38, -49, -47, -32, -10, 8, 6, 6, -2, 6,
			4, 2, 0, 0, 6, 8, 6 };

	/**
	 * The default line width.
	 */
	public static final int LINE_WIDTH_DEFAULT = 1;

	/**
	 * The default tail fade percentage
	 */
	public static final int PROGRESSION_DEFAULT = 1;

	/**
	 * Steady position @ 75% of graph.
	 */
	public static final int STEADYPOSITION_75PERCENT = -1;
	/**
	 * The default amount of tail fading in percentages (25).
	 */
	public static final int TAILFADE_DEFAULT = 25;

	/**
	 * No tailfade.
	 */
	public static final int TAILFADE_NONE = 0;

	/**
	 * The default tail fade percentage
	 */
	public static final int TAILFADE_PERCENTAGE = 25;

	/**
	 * The default tail size is 75% of the width.
	 */
	public static final int TAILSIZE_DEFAULT = -3;

	/**
	 * Will draw a tail from the left border but is only valid if the boolean in
	 * {@link #setSteady(boolean, int)} was set to true, will default to
	 * {@link #TAILSIZE_MAX} otherwise.
	 */
	public static final int TAILSIZE_FILL = -2;

	/**
	 * Will draw a maximum tail.
	 */
	public static final int TAILSIZE_MAX = -1;

	private Color bg;

	private final Data[] chan;

	// Blocks painting if true
	private boolean paintBlock;

	private int width;

	private int gridSquareSize;
	private int gridLineWidth;
	private Color gridBackground, gridForeground;
	
	/**
	 * Creates a scope with one channel.
	 *
	 * @param parent
	 * @param style
	 */
	public Oscilloscope(Composite parent, int style) {
		this(1, null, parent, style);
	}

	/**
	 * Creates a scope with <code>channels</code> channels.
	 *
	 * @param channels
	 * @param parent
	 * @param style
	 */
	public Oscilloscope(int channels, Composite parent, int style) {
		this(channels, null, parent, style);
	}

	/**
	 * Creates a new scope with <code>channels</code> channels and adds attaches
	 * it to the supplied <code>dispatcher</code>.
	 *
	 * @param channels
	 * @param dispatcher
	 *            may be null
	 * @param parent
	 * @param style
	 */
	public Oscilloscope(int channels, OscilloscopeDispatcher dispatcher, Composite parent, int style) {
		super(parent, SWT.DOUBLE_BUFFERED | style);

		chan = new Data[channels];
		for (int i = 0; i < chan.length; i++) {

			chan[i] = new Data();

			if (dispatcher == null) {
				chan[i].dispatcher = new OscilloscopeDispatcher(i, this);
			} else {
				chan[i].dispatcher = dispatcher;
				dispatcher.setOscilloscope(this);
			}
			bg = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			setBackground(bg);

			chan[i].fg = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

			setTailSize(i, TAILSIZE_DEFAULT);
		}

		addListener(SWT.Dispose, e-> Oscilloscope.this.widgetDisposed(e));
		addListener(SWT.Paint, e -> {
			if (!paintBlock) {
				Oscilloscope.this.paintControl(e);
			}
			paintBlock = false;
		});

		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {

			}
		});

		addListener(SWT.Move, e -> Oscilloscope.this.controlMoved(e));

		addListener(SWT.Resize, e -> {
			paintBlock = true;
			Oscilloscope.this.controlResized(e);
		});

		gridSquareSize = 20;
		gridLineWidth = 1;
		final Color defaultGridBackground = new Color(57, 52, 56);
		gridBackground = defaultGridBackground;
		final Color defaultGridForeground = new Color(81, 96, 77);
		gridForeground = defaultGridForeground;
		addListener(SWT.Dispose, e-> {
			defaultGridBackground.dispose();
			defaultGridForeground.dispose();
		});
		
	}

	/**
	 * Adds a new stack listener to the collection of stack listeners. Adding
	 * the same listener twice will have no effect.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param listener
	 */
	public synchronized void addStackListener(int channel, OscilloscopeStackAdapter listener) {
		if (chan[channel].stackListeners == null) {
			chan[channel].stackListeners = new ArrayList<>();
		}
		if (!chan[channel].stackListeners.contains(listener)) {
			chan[channel].stackListeners.add(listener);
		}
	}

	/**
	 * This method calculates the progression of the line.
	 *
	 * @return
	 */
	private void calculate(int channel) {
		Data ch = chan[channel];
		int progression = getProgression(channel);
		for (int progress = 0; progress < progression; progress++) {
			if (ch.stack.isEmpty() && ch.stackListeners != null) {
				notifyListeners(channel);
			}
			if (!isSteady(channel)) {
				ch.cursor++;
			}
			if (ch.cursor >= ch.width) {
				ch.cursor = 0;
			}
			popIntoTail(channel, ch);
			// only the last step's lines are drawn; earlier steps only advance the tail
			if (progress == progression - 1) {
				buildLines(channel, ch);
			}
			System.arraycopy(ch.tail, 1, ch.tail, 0, ch.tailSize);
			int[] te = ch.tailEnv;
			if (te != null) {
				System.arraycopy(te, 3, te, 0, ch.tailSize * 3);
			}
		}
	}

	private void popIntoTail(int c, Data ch) {
		int t = ch.tailSize;
		int[] te;
		synchronized (ch.envLock) {
			ch.tail[t] = transform(c, ch.width, ch.height, ch.stack.popNegate(0));
			te = ch.tailEnv;
			if (ch.env == null || te == null) {
				return;
			}
			ch.env.pop(te, t * 3);
		}
		for (int k = t * 3; k < t * 3 + 3; k++) {
			te[k] = transform(c, ch.width, ch.height, -te[k]);
		}
	}

	/**
	 * Fills the channel's two frame polylines: the part left of the cursor wrap ({@code l1}) and the rest
	 * ({@code l2}), four ints per tail entry ({@code posx - 1, previous, posx, current}) exactly as before, or in
	 * envelope mode a leading vertex plus four vertices per column ({@code first, min, max, last} at {@code posx}).
	 */
	private void buildLines(int c, Data ch) {
		int t = ch.tailSize;
		int split = Math.max(0, Math.min(t, t - ch.cursor));
		boolean envelope = ch.tailEnv != null;
		ch.l1 = sized(ch.l1, part(split, envelope));
		ch.l2 = sized(ch.l2, part(t - split, envelope));
		int base = getBase(c);
		boolean steady = isSteady(c);
		if (envelope) {
			fillEnvelope(ch, ch.l1, 0, split, base, steady);
			fillEnvelope(ch, ch.l2, split, t, base, false);
			return;
		}
		int[] l1 = ch.l1;
		int[] l2 = ch.l2;
		for (int i = 0; i < t; i++) {
			int posx = ch.cursor - t + i;
			if (posx < 0) {
				posx += ch.width;
				int pos = i * 4;
				l1[pos] = posx - 1;
				l1[pos + 1] = base + (steady ? 0 : ch.tail[i]);
				l1[pos + 2] = posx;
				l1[pos + 3] = base + (steady ? 0 : ch.tail[i + 1]);
			} else {
				int pos = (i - split) * 4;
				l2[pos] = posx - 1;
				l2[pos + 1] = base + ch.tail[i];
				l2[pos + 2] = posx;
				l2[pos + 3] = base + ch.tail[i + 1];
			}
		}
	}

	private static int part(int columns, boolean envelope) {
		if (!envelope) {
			return columns * 4;
		}
		return columns == 0 ? 0 : 2 + columns * 8;
	}

	private static int[] sized(int[] a, int n) {
		return a.length == n ? a : new int[n];
	}

	/** Envelope polyline of tail entries {@code [from, to)}: leading vertex, then first/min/max/last per column. */
	private static void fillEnvelope(Data ch, int[] line, int from, int to, int base, boolean flat) {
		if (from >= to) {
			return;
		}
		int t = ch.tailSize;
		int posx = ch.cursor - t + from;
		if (posx < 0) {
			posx += ch.width;
		}
		line[0] = posx - 1;
		line[1] = base + (flat ? 0 : ch.tail[from]);
		int o = 2;
		for (int i = from; i < to; i++) {
			int px = ch.cursor - t + i;
			if (px < 0) {
				px += ch.width;
			}
			int e = (i + 1) * 3;
			line[o] = px;
			line[o + 1] = base + (flat ? 0 : ch.tailEnv[e]);
			line[o + 2] = px;
			line[o + 3] = base + (flat ? 0 : ch.tailEnv[e + 1]);
			line[o + 4] = px;
			line[o + 5] = base + (flat ? 0 : ch.tailEnv[e + 2]);
			line[o + 6] = px;
			line[o + 7] = base + (flat ? 0 : ch.tail[i + 1]);
			o += 8;
		}
	}

	/** New envelope tail aligned to the newest entries, like the tail copy; degenerate where none existed. */
	private static int[] resizeTailEnv(int[] old, int[] tail) {
		int[] e = new int[tail.length * 3];
		for (int i = 0; i < tail.length; i++) {
			e[i * 3] = tail[i];
			e[i * 3 + 1] = tail[i];
			e[i * 3 + 2] = tail[i];
		}
		if (old != null) {
			int keep = Math.min(old.length, e.length);
			System.arraycopy(old, old.length - keep, e, e.length - keep, keep);
		}
		return e;
	}

	private void calculateBase(int channel) {
		if (chan[channel].height > 2) {
			chan[channel].base = (chan[channel].height * +(100 - getBaseOffset(channel))) / 100;
		}
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();

		int width;
		int height;

		if (wHint != SWT.DEFAULT) {
			width = wHint;
		} else {
			width = DEFAULT_WIDTH;
		}

		if (hHint != SWT.DEFAULT) {
			height = hHint;
		} else {
			height = DEFAULT_HEIGHT;
		}

		return new Point(width + 2, height + 2);
	}

	protected void controlMoved(Event e) {

	}

	protected void controlResized(Event e) {
		setSizeInternal(getSize().x, getSize().y);
		width = getBounds().width;
		if (getBounds().width > 0) {
			for (int channel = 0; channel < chan.length; channel++) {
				setSteady(channel, chan[channel].steady, chan[channel].originalSteadyPosition);
				setTailSizeInternal(channel);
			}
		}
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return the base of the line.
	 */
	public int getBase(int channel) {
		return chan[channel].base;
	}

	/**
	 * Gets the relative location where the line is drawn in the widget. This
	 * method can be called outside of the UI thread.
	 *
	 * @return baseOffset
	 */
	public int getBaseOffset(int channel) {
		return chan[channel].baseOffset;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return int, number of channels.
	 */
	public int getChannels() {
		return chan.length;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 * @return the dispatcher associated with this channel.
	 */
	public OscilloscopeDispatcher getDispatcher(int channel) {
		return chan[channel].dispatcher;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 * @return the foreground color associated with the supplied channel.
	 */
	public Color getForeground(int channel) {
		return chan[channel].fg;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return int, the width of the line.
	 * @see #setLineWidth(int)
	 */
	public int getLineWidth(int channel) {
		return chan[channel].lineWidth;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return the number of internal calculation steps at each draw request.
	 * @see #setProgression(int)
	 */
	public int getProgression(int channel) {
		return chan[channel].progression;
	}

	/**
	 * Gets the percentage of tail that must be faded out. This method can be
	 * called outside of the UI thread.
	 *
	 * @return int percentage
	 * @see #setFade(boolean)
	 */
	public int getTailFade(int channel) {
		return chan[channel].tailFade;
	}

	/**
	 * Returns the size of the tail. This method can be called outside of the UI
	 * thread.
	 *
	 * @return int
	 * @see #setTailSize(int)
	 * @see #TAILSIZE_DEFAULT
	 * @see #TAILSIZE_FILL
	 * @see #TAILSIZE_MAX
	 *
	 */
	public int getTailSize(int channel) {
		return chan[channel].tailSize;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return boolean, true if the tail and the head of the graph must be
	 *         connected if tail size is {@link #TAILSIZE_MAX} no fading graph.
	 */
	public boolean isConnect(int channel) {
		return chan[channel].connect;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @see #setFade(boolean)
	 * @return boolean fade
	 */
	public boolean isFade(int channel) {
		return chan[channel].fade;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return boolean
	 * @see #setPercentage(boolean)
	 */
	public boolean isPercentage(int channel) {
		return chan[channel].percentage;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return boolean steady indicator
	 * @see Oscilloscope#setSteady(boolean, int)
	 */
	public boolean isSteady(int channel) {
		return chan[channel].steady;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return boolean anti-alias indicator
	 * @see Oscilloscope#setAntialias(int, boolean)
	 */
	public boolean isAntiAlias(int channel) {
		return chan[channel].antiAlias;
	}

	public boolean needsRedraw() {
		checkWidget();
		return isDisposed() ? false : true;
	}

	private void notifyListeners(int channel) {
		if (chan[channel].stackListeners == null || chan[channel].stackListeners.size() == 0) {
			return;
		}
		for (int i = 0; i < chan[channel].stackListeners.size(); i++) {
			chan[channel].stackListeners.get(i).stackEmpty(this, channel);
		}
	}

	protected void paintControl(Event e) {

		for (int c = 0; c < chan.length; c++) {

			if (chan[c].tailSize <= 0) {
				chan[c].stack.popNegate(0);
				continue;
			}

			// Go calculate the line
			calculate(c);
			int[] l1 = chan[c].l1;
			int[] l2 = chan[c].l2;

			// Draw it
			GC gc = e.gc;
			gc.setForeground(getForeground(c));
			gc.setAdvanced(true);
			gc.setAntialias(chan[c].antiAlias ? SWT.ON : SWT.OFF);
			gc.setLineWidth(getLineWidth(c));

			// Fade tail
			if (isFade(c)) {
				gc.setAlpha(0);
				double fade = 0;
				double fadeOutStep = (double) 125 / (double) ((getTailSize(c) * (getTailFade(c)) / 100));
				for (int i = 0; i < l1.length - 4;) {
					fade += (fadeOutStep / 2);
					setAlpha(gc, fade);
					gc.drawLine(l1[i], l1[i + 1], l1[i + 2], l1[i + 3]);
					i += 2;
				}

				for (int i = 0; i < l2.length - 4;) {
					fade += (fadeOutStep / 2);
					setAlpha(gc, fade);
					gc.drawLine(l2[i], l2[i + 1], l2[i + 2], l2[i + 3]);
					i += 2;
				}

			} else {
				gc.drawPolyline(l1);
				gc.drawPolyline(l2);
			}

			// Connects the head with the tail
			if (isConnect(c) && !isFade(c) && chan[c].originalTailSize == TAILSIZE_MAX && l1.length > 0 && l2.length > 0) {
				gc.drawLine(l2[l2.length - 2], l2[l2.length - 1], l1[0], l1[1]);
			}
		}
	}

	/**
	 * Removes a stack listener from the collection of stack listeners. This
	 * method can be called outside of the UI thread.
	 *
	 * @param listener
	 */
	public void removeStackListener(int channel, OscilloscopeStackAdapter listener) {
		if (chan[channel].stackListeners != null) {
			chan[channel].stackListeners.remove(listener);
			if (chan[channel].stackListeners.size() == 0) {
				synchronized (chan[channel].stackListeners) {
					chan[channel].stackListeners = null;
				}
			}
		}
	}

	private void setAlpha(GC gc, double fade) {

		if (gc.getAlpha() == fade) {
			return;
		}
		if (fade >= 255) {
			gc.setAlpha(255);
		} else {
			gc.setAlpha((int) fade);
		}
	}

	/**
	 * Gets the relative location where the line is drawn in the widget, the
	 * default is <code>BASE_CENTER</code> which is in the middle of the scope.
	 * This method can be called outside of the UI thread.
	 *
	 * @param baseOffset
	 *            must be between 100 and -100, exceeding values are rounded to
	 *            the closest allowable value.
	 */
	public void setBaseOffset(int channel, int baseOffset) {

		if (baseOffset > 100) {
			baseOffset = 100;
		}

		if (baseOffset < -100) {
			baseOffset = -100;
		}

		chan[channel].baseOffset = baseOffset;

		calculateBase(channel);
	}

	/**
	 * Connects head and tail only if tail size is {@link #TAILSIZE_MAX} and no
	 * fading. This method can be called outside of the UI thread.
	 *
	 * @param connectHeadAndTail
	 */
	public void setConnect(int channel, boolean connectHeadAndTail) {
		chan[channel].connect = connectHeadAndTail;
	}

	/**
	 * Sets if the line must be anti-aliased which uses more processing power in
	 * return of a smoother image. The default value is false. This method can
	 * be called outside of the UI thread.
	 *
	 * @param channel
	 * @param antialias
	 */
	public void setAntialias(int channel, boolean antialias) {
		chan[channel].antiAlias = antialias;
	}

	/**
	 * Sets the dispatcher that is associated with the supplied channel. This
	 * method can be called outside of the UI thread.
	 *
	 * @param channel
	 * @param dispatcher
	 */
	public void setDispatcher(int channel, OscilloscopeDispatcher dispatcher) {
		chan[channel].dispatcher = dispatcher;
	}

	/**
	 * Sets fade mode so that a percentage of the tail will be faded out at the
	 * costs of extra CPU utilization (no beauty without pain or as the Dutch
	 * say: "Wie mooi wil gaan moet pijn doorstaan"). The reason for this is
	 * that each pixel must be drawn separately with alpha faded in instead of
	 * the elegant {@link GC#drawPolygon(int[])} routine which does not support
	 * alpha blending.
	 * <p>
	 * In addition to this, set the percentage of tail that must be faded out
	 * {@link #setTailFade(int)}.
	 * <p>
	 * This method can be called outside of the UI thread.
	 *
	 * @param fade
	 *            true or false
	 * @see #setTailFade(int)
	 */
	public void setFade(int channel, boolean fade) {
		chan[channel].fade = fade;
	}

	/**
	 * Sets the foreground color for the supplied channel.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 * @param color
	 */
	public void setForeground(int channel, Color color) {
		chan[channel].fg = color;
	}

	/**
	 * Sets the line width. A value equal or below zero is ignored. The default
	 * width is 1. This method can be called outside of the UI thread.
	 *
	 * @param lineWidth
	 */
	public void setLineWidth(int channel, int lineWidth) {
		if (lineWidth > 0) {
			chan[channel].lineWidth = lineWidth;
		}
	}

	/**
	 * If set to true then the values are treated as percentages of the
	 * available space rather than absolute values. This will scale the
	 * amplitudes if the control is resized. Default is false.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param percentage
	 *            true if percentages
	 */
	public void setPercentage(int channel, boolean percentage) {
		chan[channel].percentage = percentage;
	}

	/**
	 * The number of internal steps that must be made before drawing. Normally
	 * this will slide the graph one pixel. Setting this to a higher value will
	 * speed up the animation at the cost of a more jerky motion.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param progression
	 */
	public void setProgression(int channel, int progression) {
		if (progression > 0) {
			chan[channel].progression = progression;
		}

	}

	private void setSizeInternal(int width, int height) {

		for (int c = 0; c < chan.length; c++) {

			chan[c].width = width;
			chan[c].height = height;

			// calculate the base of the line
			calculateBase(c);

			if (width > 1) {
				synchronized (chan[c].envLock) {
					if (chan[c].stack == null) {
						chan[c].stack = new IntegerFiFoCircularStack(width);
					} else {
						chan[c].stack = new IntegerFiFoCircularStack(width, chan[c].stack);
					}
					if (chan[c].samplesPerColumn > 0) {
						chan[c].env = chan[c].env == null ? new EnvelopeRing(width)
								: new EnvelopeRing(width, chan[c].env);
					}
				}
			}
		}
	}

	/**
	 * If steady is true the graph will draw on a steady position instead of
	 * advancing.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param steady
	 * @param steadyPosition
	 */
	public void setSteady(int channel, boolean steady, int steadyPosition) {
		chan[channel].steady = steady;
		chan[channel].originalSteadyPosition = steadyPosition;
		if (steady) {
			if (steadyPosition == STEADYPOSITION_75PERCENT) {
				chan[channel].cursor = (int) (chan[channel].width * 0.75);
			} else if (steadyPosition > 0 && steadyPosition < chan[channel].width) {
				chan[channel].cursor = steadyPosition;
			}
		}
	}

	/**
	 * Sets the percentage of tail that must be faded out. If you supply 100
	 * then the tail is faded out all the way to the top. The effect will become
	 * increasingly less obvious.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param tailFade
	 */
	public void setTailFade(int channel, int tailFade) {
		checkWidget();
		if (tailFade > 100) {
			tailFade = 100;
		}
		if (tailFade < 1) {
			tailFade = 1;
		}
		chan[channel].tailFade = tailFade;
	}

	/**
	 * The tail size defaults to TAILSIZE_DEFAULT which is 75% of the width.
	 * Setting it with TAILSIZE_MAX will leave one pixel between the tail and
	 * the head. All values are absolute except TAILSIZE*. If the width is
	 * smaller then the tail size then the tail size will behave like
	 * TAILSIZE_MAX.
	 *
	 * @param size
	 *            the size of the tail
	 * @see #getTailSize()
	 * @see #TAILSIZE_DEFAULT
	 * @see #TAILSIZE_FILL
	 * @see #TAILSIZE_MAX
	 */
	public void setTailSize(int channel, int size) {
		checkWidget();

		if (size == TAILSIZE_FILL && !isSteady(channel)) {
			size = TAILSIZE_MAX;
		}

		if (chan[channel].originalTailSize != size) {
			tailSizeCheck(size);
			chan[channel].originalTailSize = size;
			setTailSizeInternal(channel);
		}
	}

	private void setTailSizeInternal(int channel) {

		if (chan[channel].originalTailSize == TAILSIZE_DEFAULT) {
			chan[channel].tailSize = (chan[channel].width / 4) * 3;
			chan[channel].tailSize--;
		} else if (chan[channel].originalTailSize == TAILSIZE_FILL) {
			if (isSteady(channel)) {
				chan[channel].tailSize = chan[channel].originalSteadyPosition - 1;
			} else {
				// act as if TAILSIZE_MAX
				chan[channel].tailSize = chan[channel].width - 2;
			}
		} else if (chan[channel].originalTailSize == TAILSIZE_MAX || chan[channel].originalTailSize > chan[channel].width) {
			chan[channel].tailSize = chan[channel].width - 2;
		} else if (chan[channel].tailSize != chan[channel].originalTailSize) {
			chan[channel].tailSize = chan[channel].originalTailSize;
		}

		// Transform the old tail. This is we want to see sort of the same form
		// after resize.
		int[] oldTail = chan[channel].tail;
		if (oldTail == null) {
			chan[channel].tail = new int[chan[channel].tailSize + 1];
		} else {
			chan[channel].tail = new int[chan[channel].tailSize + 1];
			if (chan[channel].tail.length >= oldTail.length) {
				for (int i = 0; i < oldTail.length; i++) {
					chan[channel].tail[chan[channel].tail.length - 1 - i] = oldTail[oldTail.length - 1 - i];
				}
			} else {
				for (int i = 0; i < chan[channel].tail.length; i++) {
					chan[channel].tail[chan[channel].tail.length - 1 - i] = oldTail[oldTail.length - 1 - i];
				}
			}
		}
		if (chan[channel].samplesPerColumn > 0) {
			chan[channel].tailEnv = resizeTailEnv(chan[channel].tailEnv, chan[channel].tail);
		}
	}

	/**
	 * Sets a value to be drawn relative to the center of the channel. Supply a
	 * positive or negative value. This method will only accept values if the
	 * width of the scope > 0. The values will be stored in a stack and popped
	 * once a value is needed. The size of the stack is the width of the widget.
	 * If you resize the widget, the old stack will be copied into a new stack
	 * with the new capacity.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 * @param value
	 *            which is an absolute value or a percentage
	 *
	 * @see #isPercentage(int)
	 * @see #setBaseOffset(int, int)
	 */
	public void setValue(int channel, int value) {
		if (width > 0) {
			if (chan[channel].stack.capacity > 0) {
				Data ch = chan[channel];
				if (ch.samplesPerColumn == 0) {
					ch.stack.push(value);
				} else {
					synchronized (ch.envLock) {
						ch.stack.push(value);
						ch.env.push(value, value, value);
					}
				}
			}
		}
	}

	/**
	 * Turns envelope mode on ({@code samplesPerColumn > 0}) or off (0, the default) for a channel. In envelope mode
	 * {@link #pushSamples(int, int[], int, int)} folds every {@code samplesPerColumn} samples into one column and the
	 * scope draws that column's first, minimum, maximum and last value at one x position, which covers the same
	 * pixels as drawing every sample at that x. Samples are not stored, only one packed envelope per queued column
	 * (the queue holds the widget's width in columns, like {@link #setValue(int, int)}), so any sample rate costs a
	 * bounded amount of memory and one comparison pair per sample. {@link #setValue(int, int)} still queues one
	 * column holding that single value.
	 * <p>
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 *            the channel
	 * @param samplesPerColumn
	 *            0 to turn envelope mode off, otherwise the number of samples per column
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_INVALID_ARGUMENT - if samplesPerColumn is negative</li>
	 * </ul>
	 * @since 1.5
	 */
	public synchronized void setSamplesPerColumn(int channel, int samplesPerColumn) {
		if (samplesPerColumn < 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		Data ch = chan[channel];
		synchronized (ch.envLock) {
			ch.accCount = 0;
			if (samplesPerColumn == 0) {
				ch.samplesPerColumn = 0;
				ch.env = null;
				ch.tailEnv = null;
				return;
			}
			if (ch.samplesPerColumn == 0) {
				ch.env = degenerateRing(ch.stack, width);
				ch.tailEnv = ch.tail == null ? null : resizeTailEnv(null, ch.tail);
			}
			ch.samplesPerColumn = samplesPerColumn;
		}
	}

	/** Envelope ring in lock step with the queued values: one degenerate envelope per queued value. */
	private static EnvelopeRing degenerateRing(IntegerFiFoCircularStack stack, int width) {
		if (stack == null) {
			return new EnvelopeRing(Math.max(2, width));
		}
		EnvelopeRing ring = new EnvelopeRing(stack.getCapacity());
		int[] values = new int[stack.getLoad()];
		for (int i = 0; i < values.length; i++) {
			values[i] = stack.pop(0);
		}
		for (int v : values) {
			stack.push(v);
			ring.push(v, v, v);
		}
		return ring;
	}

	/**
	 * Returns the number of samples folded into one column of the channel. This method can be called outside of the
	 * UI thread.
	 *
	 * @param channel
	 *            the channel
	 * @return the samples per column, 0 when envelope mode is off
	 * @see #setSamplesPerColumn(int, int)
	 * @since 1.5
	 */
	public int getSamplesPerColumn(int channel) {
		return chan[channel].samplesPerColumn;
	}

	/**
	 * Pushes {@code count} samples starting at {@code samples[offset]}. In envelope mode they are folded into
	 * column envelopes (see {@link #setSamplesPerColumn(int, int)}); otherwise this is
	 * {@link #setValues(int, int[])} over the slice. No sample object or per-sample storage is created.
	 * <p>
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 *            the channel
	 * @param samples
	 *            the samples, as for {@link #setValue(int, int)}
	 * @param offset
	 *            index of the first sample in the array
	 * @param count
	 *            number of samples
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_NULL_ARGUMENT - if samples is null</li>
	 *    <li>ERROR_INVALID_RANGE - if offset or count lie outside the array</li>
	 * </ul>
	 * @since 1.5
	 */
	public synchronized void pushSamples(int channel, int[] samples, int offset, int count) {
		if (samples == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		if (offset < 0 || count < 0 || offset > samples.length - count) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
		if (width <= 0) {
			return;
		}
		Data ch = chan[channel];
		if (ch.samplesPerColumn == 0) {
			for (int i = offset; i < offset + count; i++) {
				setValue(channel, samples[i]);
			}
			return;
		}
		synchronized (ch.envLock) {
			int spc = ch.samplesPerColumn;
			for (int i = offset; i < offset + count; i++) {
				int v = samples[i];
				if (ch.accCount == 0) {
					ch.accFirst = v;
					ch.accMin = v;
					ch.accMax = v;
				} else if (v < ch.accMin) {
					ch.accMin = v;
				} else if (v > ch.accMax) {
					ch.accMax = v;
				}
				if (++ch.accCount == spc) {
					ch.stack.push(v);
					ch.env.push(ch.accFirst, ch.accMin, ch.accMax);
					ch.accCount = 0;
				}
			}
		}
	}

	/**
	 * Sets a bunch of values that will be drawn. See
	 * {@link #setValue(int, int)} for details.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 * @param values
	 *
	 * @see #setValue(int, int)
	 */
	public synchronized void setValues(int channel, int[] values) {
		if (width > 0) {
			for (int value : values) {
				setValue(channel, value);
			}
		}
	}

	private void tailSizeCheck(int size) {
		if (size < -3 || size == 0) {
			throw new RuntimeException("Invalid tail size " + size);
		}
	}

	/**
	 * Transforms the value before it is drawn.
	 *
	 * @param value
	 *            the next value to be processed
	 * @return the transformed value
	 */
	private int transform(int channel, int vWidth, int vHeight, int value) {
		if (isPercentage(channel)) {
			return (((vHeight / 2) * value) / 100);
		}
		return value;
	}

	protected void widgetDisposed(Event e) {
		bg.dispose();
		for (Data element : chan) {
			element.fg.dispose();
		}
	}

	/**
	 * @return the width of the square displayed in the grid
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 * </ul>
	 */
	public int getGridSquareSize() {
		checkWidget();
		return gridSquareSize;
	}

	/**
	 * Set the size of the square displayed in the grid
	 * @param size the new size
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 *    <li>ERROR_INVALID_ARGUMENT - if size is lower than 1</li>
	 * </ul>
	 */
	public void setGridSquareSize(int size) {
		checkWidget();
		if (size < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.gridSquareSize = size;
		updateGrid();
	}

	private void updateGrid() {
		if (chan.length > 0) {
			OscilloscopeDispatcher dispatcher = chan[0].dispatcher;
			dispatcher.updateBackgroundImage();
			setBackgroundImage(dispatcher.getBackgroundImage());
		}
	}

	/**
	 * @return the width of the lines of the grid
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 * </ul>
	 */
	public int getGridLineWidth() {
		checkWidget();
		return gridLineWidth;
	}

	/**
	 * Set the with of the lines size of the square displayed in the grid
	 * @param gridLineWidth the new size
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 *    <li>ERROR_INVALID_ARGUMENT - if gridLineWidth is lower than 1</li>
	 * </ul>
	 */
	public void setGridLineWidth(int gridLineWidth) {
		checkWidget();
		if (gridLineWidth < 1) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		this.gridLineWidth = gridLineWidth;
		updateGrid();
	}

	/**
	 * Returns the background color of the oscilloscope.
	 *
	 * @return the oscilloscope background color
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 * </ul>
	 */
	public Color getGridBackground() {
		checkWidget();
		return gridBackground;
	}

	/**
	 * Sets the oscilloscope's background color to the color specified
	 * by the argument, or to the default system color for the control
	 * if the argument is null.
	 * @param color the new color (or null)
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 * </ul>
	 */
	public void setGridBackground(Color gridBackground) {
		checkWidget();
		this.gridBackground = gridBackground;
		updateGrid();
	}

	/**
	 * Returns the color of the grid.
	 *
	 * @return the color or the grid
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 * </ul>
	 */
	public Color getGridForeground() {
		checkWidget();
		return gridForeground;
	}

	/**
	 * Sets the color of the grid to the color specified by the argument, or to the default system color for the control
	 * if the argument is null.
	 * @param color the new color (or null)
	 *
	 * @exception IllegalArgumentException <ul>
	 *    <li>ERROR_INVALID_ARGUMENT - if the argument has been disposed</li>
	 * </ul>
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 * </ul>
	 */
	public void setGridForeground(Color gridForeground) {
		checkWidget();
		this.gridForeground = gridForeground;
		updateGrid();
	}
	
	
}
