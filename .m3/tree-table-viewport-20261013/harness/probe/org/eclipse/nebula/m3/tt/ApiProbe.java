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
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * Invokes every API row against a fresh fixture and renders a loader-independent
 * outcome: result, thrown exception, and a snapshot of the primary widget's public
 * getters. The same probe bytecode runs in the pre-image and post-image loaders.
 */
public final class ApiProbe {
	private static final Map<Class<?>, List<Method>> GETTERS = new HashMap<>();

	private ApiProbe() {
	}

	/**
	 * @param rows {api_id, package, type, kind, signature}
	 * @return lines {api_id, outcome, snapshot}
	 */
	public static List<String[]> run(Shell host, String fixtureClass, List<String[]> rows, boolean visible) {
		Display display = host.getDisplay();
		Runnable closer = new ModalCloser(display, host);
		display.timerExec(150, closer);
		List<String[]> out = new ArrayList<>();
		try {
			int done = 0;
			for (String[] row : rows) {
				if (++done % 100 == 0) {
					System.err.println("probe " + done + "/" + rows.size());
				}
				long started = System.nanoTime();
				try {
					out.add(probe(host, fixtureClass, row, visible));
					long ms = (System.nanoTime() - started) / 1_000_000L;
					if (ms > 1500) {
						System.err.println("slow " + ms + "ms " + row[0] + " " + row[2] + " " + row[4]);
					}
				} catch (Throwable ex) {
					out.add(new String[] { row[0], "PROBE_ERROR:" + describe(ex), "" });
					for (Shell s : display.getShells()) {
						if (s != host && !s.isDisposed()) {
							s.dispose();
						}
					}
				}
			}
		} finally {
			display.timerExec(-1, closer);
		}
		return out;
	}

	private static String[] probe(Shell host, String fixtureClass, String[] row, boolean visible) {
		String kind = row[3];
		Class<?> type;
		try {
			type = Class.forName(row[1] + "." + row[2].replace('.', '$'), false, ApiProbe.class.getClassLoader());
		} catch (Throwable ex) {
			return new String[] { row[0], "UNRESOLVED_TYPE:" + ex.getClass().getSimpleName(), "" };
		}
		switch (kind) {
		case "class":
		case "interface":
		case "enum":
		case "@interface":
		case "record":
			return new String[] { row[0], typeShape(type), "" };
		case "enum_constant":
			return new String[] { row[0], enumConstant(type, row[4]), "" };
		default:
			return invokeRow(host, fixtureClass, row, type, kind, visible);
		}
	}

	private static String typeShape(Class<?> type) {
		List<String> ifaces = new ArrayList<>();
		for (Class<?> i : type.getInterfaces()) {
			ifaces.add(i.getName());
		}
		ifaces.sort(null);
		Class<?> sup = type.getSuperclass();
		return "T:" + Modifier.toString(type.getModifiers()) + " super=" + (sup == null ? "-" : sup.getName())
				+ " ifaces=" + ifaces;
	}

	private static String enumConstant(Class<?> type, String name) {
		for (Object c : type.getEnumConstants()) {
			Enum<?> e = (Enum<?>) c;
			if (e.name().equals(name)) {
				return "E:" + e.name() + "/" + e.ordinal();
			}
		}
		return "UNRESOLVED_CONSTANT";
	}

	private static String[] invokeRow(Shell host, String fixtureClass, String[] row, Class<?> type, String kind,
			boolean visible) {
		Display display = host.getDisplay();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());
		shell.setBounds(40, 40, 480, 360);
		Image image = new Image(display, 8, 8);
		List<GC> gcs = new ArrayList<>();
		TtFixture fx;
		try {
			fx = (TtFixture) Class.forName(fixtureClass, true, ApiProbe.class.getClassLoader())
					.getDeclaredConstructor().newInstance();
			fx.build(shell);
			if (visible) {
				shell.open();
			} else {
				shell.layout(true, true);
			}
			pump(display);
		} catch (Throwable ex) {
			shell.dispose();
			image.dispose();
			return new String[] { row[0], "FIXTURE_FAILED:" + describe(ex), "" };
		}
		try {
			String outcome;
			if ("field".equals(kind)) {
				outcome = field(type, row[4], fx);
			} else {
				Executable member = Signatures.resolve(type, kind, row[4]);
				if (member == null) {
					outcome = "UNRESOLVED_MEMBER";
				} else {
					String skip = fx.skipReason(member);
					outcome = skip != null ? "SKIPPED:" + skip + " sig=" + Signatures.shape(member)
							: call(member, fx, shell, image, gcs);
				}
			}
			pump(display);
			String snapshot = snapshot(fx);
			return new String[] { row[0], outcome + " | S=" + Canon.sha(snapshot), snapshot };
		} finally {
			for (GC gc : gcs) {
				if (!gc.isDisposed()) {
					gc.dispose();
				}
			}
			for (Shell s : display.getShells()) {
				if (s != host && !s.isDisposed()) {
					s.dispose();
				}
			}
			image.dispose();
			pump(display);
		}
	}

	private static String field(Class<?> type, String signature, TtFixture fx) {
		String name = Signatures.fieldName(signature);
		try {
			Field f = type.getDeclaredField(name);
			f.setAccessible(true);
			Object receiver = Modifier.isStatic(f.getModifiers()) ? null : fx.instanceOf(type);
			if (receiver == null && !Modifier.isStatic(f.getModifiers())) {
				return "F:" + Modifier.toString(f.getModifiers()) + " " + f.getType().getName() + " NOINSTANCE";
			}
			return "F:" + Modifier.toString(f.getModifiers()) + " " + f.getType().getName() + "="
					+ Canon.of(f.get(receiver), fx);
		} catch (Throwable ex) {
			return "UNRESOLVED_FIELD:" + describe(ex);
		}
	}

	private static String call(Executable member, TtFixture fx, Shell shell, Image image, List<GC> gcs) {
		String shape = Signatures.shape(member);
		Object receiver = null;
		boolean isStatic = Modifier.isStatic(member.getModifiers());
		if (member instanceof Method && !isStatic) {
			receiver = fx.instanceOf(member.getDeclaringClass());
			if (receiver == null) {
				receiver = Args.construct(member.getDeclaringClass(), fx, shell, image, gcs);
			}
			if (receiver == null) {
				return "NOINSTANCE sig=" + shape;
			}
		}
		Class<?>[] params = member.getParameterTypes();
		Object[] args = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			args[i] = Args.gen(params[i], fx, shell, image, gcs, member.getDeclaringClass().getClassLoader());
		}
		try {
			member.setAccessible(true);
			Object result;
			if (member instanceof Constructor) {
				if (Modifier.isAbstract(member.getDeclaringClass().getModifiers())) {
					return "ABSTRACT_CTOR sig=" + shape;
				}
				result = ((Constructor<?>) member).newInstance(args);
				return "NEW=" + newShape(result, fx) + " sig=" + shape;
			}
			result = ((Method) member).invoke(receiver, args);
			return "R=" + Canon.of(result, fx) + " sig=" + shape;
		} catch (InvocationTargetException ex) {
			return "X=" + describe(ex.getCause()) + " sig=" + shape;
		} catch (Throwable ex) {
			return "XI=" + describe(ex) + " sig=" + shape;
		}
	}

	private static String newShape(Object created, TtFixture fx) {
		if (created instanceof Widget) {
			Widget w = (Widget) created;
			return created.getClass().getSimpleName() + (w.isDisposed() ? "(disposed)" : "(style=" + w.getStyle() + ")");
		}
		return created == null ? "null" : created.getClass().getSimpleName();
	}

	static String describe(Throwable ex) {
		if (ex == null) {
			return "null";
		}
		String msg = ex.getMessage();
		return ex.getClass().getName() + (msg == null ? "" : ":" + Canon.stripIdentity(msg));
	}

	/** Public zero-argument getters of the primary widget, rendered canonically. */
	static String snapshot(TtFixture fx) {
		Object primary = fx.primary();
		if (primary == null) {
			return "nil";
		}
		if (primary instanceof Widget && ((Widget) primary).isDisposed()) {
			return "disposed";
		}
		StringBuilder out = new StringBuilder();
		for (Method m : getters(primary.getClass())) {
			out.append(m.getName()).append('=');
			try {
				out.append(Canon.of(m.invoke(primary), fx));
			} catch (InvocationTargetException ex) {
				out.append("X:").append(describe(ex.getCause()));
			} catch (Throwable ex) {
				out.append("XI:").append(ex.getClass().getSimpleName());
			}
			out.append(';');
		}
		return out.toString();
	}

	private static final Set<String> SWT_GETTERS = new HashSet<>(
			Arrays.asList("getSize", "getVisible", "getEnabled", "isFocusControl", "getToolTipText", "getStyle"));

	private static synchronized List<Method> getters(Class<?> c) {
		List<Method> cached = GETTERS.get(c);
		if (cached != null) {
			return cached;
		}
		List<Method> list = new ArrayList<>();
		for (Method m : c.getMethods()) {
			String n = m.getName();
			boolean named = n.startsWith("get") || n.startsWith("is") || n.startsWith("has");
			boolean nebula = m.getDeclaringClass().getName().startsWith("org.eclipse.nebula");
			if (named && m.getParameterCount() == 0 && m.getReturnType() != void.class
					&& (nebula || SWT_GETTERS.contains(n)) && !Modifier.isStatic(m.getModifiers())) {
				list.add(m);
			}
		}
		list.sort((a, b) -> a.toGenericString().compareTo(b.toGenericString()));
		GETTERS.put(c, list);
		return list;
	}

	static void pump(Display display) {
		for (int i = 0; i < 400 && display.readAndDispatch(); i++) {
			// bounded flush of async work
		}
	}

	/** Closes modal SWT shells opened by a probed call so nested loops return. */
	private static final class ModalCloser implements Runnable {
		private final Display display;
		private final Shell host;

		ModalCloser(Display display, Shell host) {
			this.display = display;
			this.host = host;
		}

		@Override
		public void run() {
			for (Shell s : display.getShells()) {
				int modal = SWT.APPLICATION_MODAL | SWT.PRIMARY_MODAL | SWT.SYSTEM_MODAL;
				if (s != host && !s.isDisposed() && (s.getStyle() & modal) != 0) {
					s.close();
					if (!s.isDisposed()) {
						s.dispose();
					}
				}
			}
			display.timerExec(150, this);
		}
	}

	/** Canonical argument values per parameter type. */
	static final class Args {
		private Args() {
		}

		static Object gen(Class<?> t, TtFixture fx, Shell shell, Image image, List<GC> gcs, ClassLoader loader) {
			Object primitive = primitive(t);
			if (primitive != null) {
				return primitive;
			}
			if (t == String.class) {
				return "probe";
			}
			Object fromFixture = fx.instanceOf(t);
			if (fromFixture != null) {
				return fromFixture;
			}
			if (t.isArray()) {
				Object e = gen(t.getComponentType(), fx, shell, image, gcs, loader);
				Object arr = Array.newInstance(t.getComponentType(), e == null ? 0 : 1);
				if (e != null) {
					Array.set(arr, 0, e);
				}
				return arr;
			}
			Object swt = swt(t, fx, shell, image, gcs);
			if (swt != null) {
				return swt;
			}
			Object jdk = jdk(t);
			if (jdk != null) {
				return jdk;
			}
			if (t.isEnum()) {
				Object[] c = t.getEnumConstants();
				return c.length == 0 ? null : c[0];
			}
			if (t.isInterface()) {
				return Proxy.newProxyInstance(loader == null ? ApiProbe.class.getClassLoader() : loader,
						new Class<?>[] { t }, (proxy, m, a) -> proxyAnswer(proxy, m, a));
			}
			return construct(t, fx, shell, image, gcs);
		}

		private static int depth;

		/** Public constructor with the fewest parameters, arguments generated recursively (depth 2). */
		static Object construct(Class<?> t, TtFixture fx, Shell shell, Image image, List<GC> gcs) {
			if (depth >= 2 || t.isInterface() || Modifier.isAbstract(t.getModifiers())
					|| !t.getName().startsWith("org.eclipse.nebula")) {
				return null;
			}
			Constructor<?>[] ctors = t.getConstructors();
			Arrays.sort(ctors, (a, b) -> a.getParameterCount() != b.getParameterCount()
					? a.getParameterCount() - b.getParameterCount() : Signatures.shape(a).compareTo(Signatures.shape(b)));
			depth++;
			try {
				for (Constructor<?> c : ctors) {
					if (fx.skipReason(c) != null) {
						continue;
					}
					Class<?>[] p = c.getParameterTypes();
					Object[] a = new Object[p.length];
					for (int i = 0; i < p.length; i++) {
						a[i] = gen(p[i], fx, shell, image, gcs, t.getClassLoader());
					}
					try {
						return c.newInstance(a);
					} catch (Throwable ex) {
						// try the next constructor
					}
				}
				return null;
			} finally {
				depth--;
			}
		}

		private static Object primitive(Class<?> t) {
			if (t == int.class || t == Integer.class) {
				return 1;
			}
			if (t == long.class || t == Long.class) {
				return 1L;
			}
			if (t == boolean.class || t == Boolean.class) {
				return Boolean.TRUE;
			}
			if (t == short.class || t == Short.class) {
				return (short) 1;
			}
			if (t == byte.class || t == Byte.class) {
				return (byte) 1;
			}
			if (t == char.class || t == Character.class) {
				return 'a';
			}
			if (t == float.class || t == Float.class) {
				return 1f;
			}
			if (t == double.class || t == Double.class) {
				return 1d;
			}
			return null;
		}

		private static Object swt(Class<?> t, TtFixture fx, Shell shell, Image image, List<GC> gcs) {
			Display d = shell.getDisplay();
			if (t == Point.class) {
				return new Point(5, 5);
			}
			if (t == Rectangle.class) {
				return new Rectangle(0, 0, 40, 20);
			}
			if (t == RGB.class) {
				return new RGB(1, 2, 3);
			}
			if (t == Color.class) {
				return d.getSystemColor(SWT.COLOR_BLUE);
			}
			if (t == Font.class) {
				return shell.getFont();
			}
			if (t == Image.class) {
				return image;
			}
			if (t == GC.class) {
				GC gc = new GC(image);
				gcs.add(gc);
				return gc;
			}
			if (t == Event.class) {
				Event e = new Event();
				e.display = d;
				e.widget = fx.primary() instanceof Widget ? (Widget) fx.primary() : shell;
				return e;
			}
			if (t == Display.class) {
				return d;
			}
			if (t.isInstance(shell)) {
				return shell;
			}
			return null;
		}

		private static Object jdk(Class<?> t) {
			if (t == Object.class) {
				return "probe";
			}
			if (t == List.class || t == Collection.class || t == Iterable.class || t == ArrayList.class) {
				return new ArrayList<>();
			}
			if (t == Set.class) {
				return new HashSet<>();
			}
			if (t == Map.class) {
				return new HashMap<>();
			}
			if (t == Date.class) {
				return new Date(1_700_000_000_000L);
			}
			if (t == Calendar.class) {
				Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
				c.setTimeInMillis(1_700_000_000_000L);
				return c;
			}
			if (t == Locale.class) {
				return Locale.US;
			}
			if (t == Class.class) {
				return Object.class;
			}
			return null;
		}

		private static Object proxyAnswer(Object proxy, Method m, Object[] a) {
			switch (m.getName()) {
			case "equals":
				return a != null && a.length == 1 && proxy == a[0];
			case "hashCode":
				return 7;
			case "toString":
				return "proxy:" + m.getDeclaringClass().getSimpleName();
			default:
				Class<?> r = m.getReturnType();
				if (r == void.class) {
					return null;
				}
				Object p = primitive(r);
				if (p != null) {
					return r == boolean.class || r == Boolean.class ? Boolean.FALSE : (r.isPrimitive() ? zero(r) : null);
				}
				return r == String.class ? "" : null;
			}
		}

		private static Object zero(Class<?> r) {
			if (r == int.class) {
				return 0;
			}
			if (r == long.class) {
				return 0L;
			}
			if (r == short.class) {
				return (short) 0;
			}
			if (r == byte.class) {
				return (byte) 0;
			}
			if (r == char.class) {
				return (char) 0;
			}
			if (r == float.class) {
				return 0f;
			}
			return 0d;
		}
	}

	/** Header-only member resolution from the extracted signature column. */
	static final class Signatures {
		private Signatures() {
		}

		static String fieldName(String signature) {
			String head = signature.split("=")[0].trim();
			String[] tokens = head.split("\\s+");
			return tokens[tokens.length - 1];
		}

		static Executable resolve(Class<?> type, String kind, String signature) {
			int open = signature.indexOf('(');
			String before = signature.substring(0, open).trim();
			String name = before.substring(before.lastIndexOf(' ') + 1);
			List<String> params = params(signature.substring(open + 1, signature.lastIndexOf(')')));
			Executable[] candidates = "constructor".equals(kind) ? type.getDeclaredConstructors()
					: type.getDeclaredMethods();
			Executable fallback = null;
			int fallbackCount = 0;
			for (Executable e : candidates) {
				if (e.isSynthetic() || !(e instanceof Constructor || e.getName().equals(name))
						|| e.getParameterCount() != params.size()) {
					continue;
				}
				fallback = e;
				fallbackCount++;
				if (matches(e.getParameterTypes(), params)) {
					return e;
				}
			}
			return fallbackCount == 1 ? fallback : null;
		}

		private static boolean matches(Class<?>[] actual, List<String> declared) {
			for (int i = 0; i < actual.length; i++) {
				String want = declared.get(i);
				String have = simple(actual[i]);
				if (!have.equals(want) && !isTypeVariable(want, actual[i])) {
					return false;
				}
			}
			return true;
		}

		private static boolean isTypeVariable(String want, Class<?> actual) {
			String base = want.replace("[]", "");
			return base.length() <= 2 && Character.isUpperCase(base.charAt(0))
					&& (actual == Object.class || actual.getComponentType() == Object.class);
		}

		private static String simple(Class<?> c) {
			if (c.isArray()) {
				return simple(c.getComponentType()) + "[]";
			}
			String n = c.getName();
			n = n.substring(n.lastIndexOf('.') + 1);
			return n.substring(n.lastIndexOf('$') + 1);
		}

		static List<String> params(String list) {
			List<String> out = new ArrayList<>();
			String s = list.replaceAll("@\\w+(\\([^)]*\\))?", " ").trim();
			if (s.isEmpty()) {
				return out;
			}
			int depth = 0;
			int start = 0;
			for (int i = 0; i <= s.length(); i++) {
				char c = i == s.length() ? ',' : s.charAt(i);
				if (c == '<') {
					depth++;
				} else if (c == '>') {
					depth--;
				} else if (c == ',' && depth == 0) {
					out.add(paramType(s.substring(start, i)));
					start = i + 1;
				}
			}
			return out;
		}

		private static String paramType(String param) {
			String p = param.replaceAll("<.*>", "").replace("final ", " ").trim();
			boolean varargs = p.contains("...");
			p = p.replace("...", " ").trim();
			String[] tokens = p.split("\\s+");
			String type = tokens.length > 1 ? String.join("", Arrays.copyOf(tokens, tokens.length - 1)) : tokens[0];
			String dims = "";
			while (type.endsWith("[]")) {
				dims += "[]";
				type = type.substring(0, type.length() - 2);
			}
			String lastToken = tokens[tokens.length - 1];
			while (lastToken.endsWith("[]")) {
				dims += "[]";
				lastToken = lastToken.substring(0, lastToken.length() - 2);
			}
			type = type.substring(type.lastIndexOf('.') + 1);
			return type + dims + (varargs ? "[]" : "");
		}

		static String shape(Member m) {
			if (m instanceof Executable) {
				Executable e = (Executable) m;
				StringBuilder s = new StringBuilder(Modifier.toString(e.getModifiers())).append(' ');
				if (e instanceof Method) {
					s.append(((Method) e).getReturnType().getName()).append(' ');
				}
				s.append(e.getName()).append('(');
				Class<?>[] p = e.getParameterTypes();
				for (int i = 0; i < p.length; i++) {
					s.append(i == 0 ? "" : ",").append(p[i].getName());
				}
				s.append(')');
				Class<?>[] x = e.getExceptionTypes();
				if (x.length > 0) {
					String[] names = new String[x.length];
					for (int i = 0; i < x.length; i++) {
						names[i] = x[i].getName();
					}
					Arrays.sort(names);
					s.append(" throws ").append(String.join(",", names));
				}
				return s.toString();
			}
			return m.toString();
		}
	}
}
