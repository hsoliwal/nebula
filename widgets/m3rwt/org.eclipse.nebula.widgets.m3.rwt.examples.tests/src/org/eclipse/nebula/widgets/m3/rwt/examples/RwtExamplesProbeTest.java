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
package org.eclipse.nebula.widgets.m3.rwt.examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.rap.json.JsonArray;
import org.eclipse.rap.json.JsonValue;
import org.eclipse.rap.rwt.internal.protocol.Operation;
import org.eclipse.rap.rwt.internal.protocol.Operation.CallOperation;
import org.eclipse.rap.rwt.internal.protocol.Operation.CreateOperation;
import org.eclipse.rap.rwt.testfixture.internal.Fixture;
import org.eclipse.rap.rwt.testfixture.internal.TestMessage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.widgets.displaykit.DisplayLCA;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Runs every example through RWT's render phase on RAP's test fixture (no browser) and records what the browser
 * would receive: the first render, an idle round, a resize, a visible change, hiding, a change while hidden and
 * showing again. One row per example goes to the TSV named by {@code -Dm3.rwt.evidence}.
 */
class RwtExamplesProbeTest {

	static final String[] ROUNDS = {"initial", "idle", "resize", "change", "hide", "change_hidden", "show"};

	@TestFactory
	Stream<DynamicTest> everyExampleRendersOnRwt () {
		List<String> rows = new ArrayList<> ();
		StringBuilder head = new StringBuilder ("concept\twidget\tcreates\tgc_draw_ops");
		for (String round : ROUNDS) head.append ('\t').append (round).append ("_ops\t").append (round).append ("_bytes");
		rows.add (head.toString ());
		Stream<DynamicTest> tests = RwtExamples.all ().stream ().map (example -> DynamicTest.dynamicTest (example.id (), () -> {
			String row = probe (example);
			synchronized (rows) {
				rows.add (row);
			}
		}));
		return Stream.concat (tests, Stream.of (DynamicTest.dynamicTest ("write evidence", () -> write (rows))));
	}

	private static String probe (RwtExample example) {
		Fixture.setUp ();
		try {
			Display display = new Display ();
			Shell shell = new Shell (display, SWT.NO_TRIM);
			shell.setBounds (0, 0, 1000, 700);
			shell.setLayout (new FillLayout ());
			Control control = example.create (shell);
			shell.layout ();
			shell.open ();
			StringBuilder row = new StringBuilder (example.id ()).append ('\t').append (example.widget);
			long[][] measured = new long[ROUNDS.length][];
			measured[0] = round (display, () -> { });
			int creates = lastCreates;
			int draws = lastDraws;
			measured[1] = round (display, () -> { });
			measured[2] = round (display, () -> {
				shell.setSize (1200, 800);
				shell.layout ();
			});
			measured[3] = round (display, () -> example.change (control));
			measured[4] = round (display, () -> control.setVisible (false));
			measured[5] = round (display, () -> example.change (control));
			measured[6] = round (display, () -> control.setVisible (true));
			assertTrue (creates > 0, example.concept + " created nothing on the client");
			row.append ('\t').append (creates).append ('\t').append (draws);
			for (long[] m : measured) row.append ('\t').append (m[0]).append ('\t').append (m[1]);
			return row.toString ();
		} finally {
			Fixture.tearDown ();
		}
	}

	private static int lastCreates;
	private static int lastDraws;

	/** One request: preserve, change, render; returns {operations, bytes} of the protocol message. */
	private static long[] round (Display display, Runnable change) {
		Fixture.fakeNewRequest ();
		DisplayLCA lca = new DisplayLCA ();
		lca.preserveValues (display);
		change.run ();
		try {
			lca.render (display);
		} catch (IOException failure) {
			throw new UncheckedIOException (failure);
		}
		lca.clearPreserved (display);
		TestMessage message = Fixture.getProtocolMessage ();
		int creates = 0, draws = 0;
		for (int i = 0; i < message.getOperationCount (); i++) {
			Operation operation = message.getOperation (i);
			if (operation instanceof CreateOperation) creates++;
			if (operation instanceof CallOperation && "draw".equals (((CallOperation) operation).getMethodName ())) {
				JsonValue ops = ((CallOperation) operation).getParameters ().get ("operations");
				if (ops != null) draws += ((JsonArray) ops).size ();
			}
		}
		lastCreates = creates;
		lastDraws = draws;
		return new long[] {message.getOperationCount (), message.toString ().getBytes (StandardCharsets.UTF_8).length};
	}

	private static void write (List<String> rows) throws IOException {
		String target = System.getProperty ("m3.rwt.evidence");
		if (target != null) Files.write (Path.of (target), rows, StandardCharsets.UTF_8);
	}
}
