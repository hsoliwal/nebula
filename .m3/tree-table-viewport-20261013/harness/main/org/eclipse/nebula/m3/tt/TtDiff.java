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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Differential API runner: probes every API row of one widget in the unmodified
 * pre-image, again in a fresh pre-image loader (baseline determinism), and in
 * the post-image, then classifies each row.
 *
 * <pre>
 * TtDiff probe &lt;api.tsv&gt; &lt;widget&gt; &lt;fixtureClass&gt; &lt;probeDir&gt; &lt;classesDir&gt; &lt;out.tsv&gt; &lt;visible|hidden&gt;
 * TtDiff compare &lt;api.tsv&gt; &lt;widget&gt; &lt;pre.tsv&gt; &lt;pre2.tsv&gt; &lt;post.tsv&gt; &lt;out.tsv&gt;
 * </pre>
 * One JVM per probe pass, so native handles leaked by one image never reach the next.
 */
public final class TtDiff {
	private TtDiff() {
	}

	public static void main(String[] args) throws Exception {
		List<String[]> rows = rows(args[1], args[2]);
		if ("compare".equals(args[0])) {
			write(args[6], rows, read(args[3]), read(args[4]), read(args[5]));
			return;
		}
		boolean visible = "visible".equals(args[7]);
		Display display = new Display();
		Shell host = new Shell(display);
		try {
			Map<String, String[]> pass = probe(host, args[3], args[4], args[5], rows, visible);
			List<String> lines = new ArrayList<>();
			for (String[] r : pass.values()) {
				lines.add(r[0] + "\t" + escape(r[1]) + "\t" + escape(r[2]));
			}
			Files.write(Paths.get(args[6]), lines, StandardCharsets.UTF_8);
			System.out.println("probed " + lines.size() + " rows");
		} finally {
			host.dispose();
			display.dispose();
		}
	}

	private static String escape(String s) {
		return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
	}

	private static Map<String, String[]> read(String path) throws IOException {
		Map<String, String[]> byId = new LinkedHashMap<>();
		for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)) {
			String[] c = line.split("\t", -1);
			byId.put(c[0], new String[] { c[0], c[1], c.length > 2 ? c[2] : "" });
		}
		return byId;
	}

	private static List<String[]> rows(String tsv, String widget) throws IOException {
		List<String[]> out = new ArrayList<>();
		List<String> lines = Files.readAllLines(Paths.get(tsv), StandardCharsets.UTF_8);
		for (String line : lines.subList(1, lines.size())) {
			String[] c = line.split("\t", -1);
			if (c.length > 7 && c[1].equals(widget)) {
				out.add(new String[] { c[0], c[3], c[4], c[5], c[7] });
			}
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String[]> probe(Shell host, String fixture, String probeDir, String classesDir,
			List<String[]> rows, boolean visible) throws Exception {
		URL[] urls = { new File(probeDir).toURI().toURL(), new File(classesDir).toURI().toURL() };
		try (URLClassLoader loader = new URLClassLoader(urls, TtDiff.class.getClassLoader())) {
			Class<?> probe = Class.forName("org.eclipse.nebula.m3.tt.ApiProbe", true, loader);
			Method run = probe.getMethod("run", Shell.class, String.class, List.class, boolean.class);
			List<String[]> result = (List<String[]>) run.invoke(null, host, fixture, rows, visible);
			Map<String, String[]> byId = new LinkedHashMap<>();
			for (String[] r : result) {
				byId.put(r[0], r);
			}
			return byId;
		}
	}

	private static void write(String out, List<String[]> rows, Map<String, String[]> pre,
			Map<String, String[]> pre2, Map<String, String[]> post) throws IOException {
		List<String> lines = new ArrayList<>();
		lines.add("api_id\tkind\tverdict\tpre_outcome\tpost_outcome\tsnapshot_diff");
		int[] counts = new int[5];
		for (String[] row : rows) {
			String[] a = pre.get(row[0]);
			String[] b = pre2.get(row[0]);
			String[] c = post.get(row[0]);
			String verdict = verdict(a, b, c);
			counts[index(verdict)]++;
			String diff = c != null && a != null && !a[2].equals(c[2]) ? firstDiff(a[2], c[2]) : "";
			lines.add(row[0] + "\t" + row[3] + "\t" + verdict + "\t" + clip(a) + "\t" + clip(c) + "\t" + diff);
		}
		Files.write(Paths.get(out), lines, StandardCharsets.UTF_8);
		System.out.println("rows=" + rows.size() + " EQUAL=" + counts[0] + " EQUAL_NONINVOKED=" + counts[1]
				+ " DIFF=" + counts[2] + " NONDETERMINISTIC_BASELINE=" + counts[3] + " MISSING=" + counts[4]);
	}

	private static String verdict(String[] pre, String[] pre2, String[] post) {
		if (pre == null || post == null) {
			return "MISSING";
		}
		if (!pre[1].equals(pre2[1]) || !pre[2].equals(pre2[2])) {
			return "NONDETERMINISTIC_BASELINE";
		}
		if (!pre[1].equals(post[1]) || !pre[2].equals(post[2])) {
			return "DIFF";
		}
		String o = pre[1];
		boolean invoked = o.startsWith("R=") || o.startsWith("X=") || o.startsWith("NEW=") || o.startsWith("F:")
				|| o.startsWith("T:") || o.startsWith("E:");
		return invoked ? "EQUAL" : "EQUAL_NONINVOKED";
	}

	private static int index(String verdict) {
		switch (verdict) {
		case "EQUAL":
			return 0;
		case "EQUAL_NONINVOKED":
			return 1;
		case "DIFF":
			return 2;
		case "NONDETERMINISTIC_BASELINE":
			return 3;
		default:
			return 4;
		}
	}

	private static String clip(String[] r) {
		if (r == null) {
			return "";
		}
		String s = r[1].replace('\t', ' ');
		return s.length() > 400 ? s.substring(0, 400) + "..." : s;
	}

	private static String firstDiff(String a, String b) {
		String[] x = a.split(";");
		String[] y = b.split(";");
		for (int i = 0; i < Math.min(x.length, y.length); i++) {
			if (!x[i].equals(y[i])) {
				String s = x[i] + " => " + y[i];
				return s.length() > 300 ? s.substring(0, 300) : s.replace('\t', ' ');
			}
		}
		return "len " + x.length + " => " + y.length;
	}
}
