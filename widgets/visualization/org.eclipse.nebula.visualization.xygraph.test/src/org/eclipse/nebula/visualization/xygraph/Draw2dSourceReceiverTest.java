/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.visualization.xygraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.draw2d.Figure;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Verifies the Draw2D bundle selected by the opt-in source-receiver build.
 * <p>
 * The ordinary XYGraph test run excludes this class. The
 * {@code draw2d-source-receiver} profile enables it and passes identities measured
 * from the actual GEF build. Missing identity inputs, an exploded replacement,
 * another source commit or another resolved bundle are failures. Passing this
 * test establishes artifact custody; the other XYGraph tests establish their
 * own rendering and behavior contracts.
 * </p>
 */
public class Draw2dSourceReceiverTest {

	private static final String SOURCE_REPOSITORY = "scm:git:https://github.com/hsoliwal/gef-classic";

	@Test
	public void loadedDraw2dMatchesThePinnedSourceBuild() throws Exception {
		assertEquals("Source-receiver test must be explicitly enabled", "true",
				System.getProperty("nebula.draw2d.receiver.required"));
		String expectedVersion = required("bundle.version",
				"(?!.*(?:qualifier|SNAPSHOT))[0-9]+\\.[0-9]+\\.[0-9]+\\.[A-Za-z0-9_-]+");
		String expectedCommit = required("source.commit", "[0-9a-f]{40}");
		String expectedBundleHash = required("bundle.sha256", "[0-9a-f]{64}");
		String expectedFigureHash = required("figure.sha256", "[0-9a-f]{64}");

		Bundle bundle = FrameworkUtil.getBundle(Figure.class);
		assertNotNull("Figure was not loaded from an OSGi bundle", bundle);
		assertEquals("org.eclipse.draw2d", bundle.getSymbolicName());
		assertEquals("Tycho selected a different Draw2D version", expectedVersion,
				bundle.getVersion().toString());
		assertSourceReference(bundle.getHeaders("").get("Eclipse-SourceReferences"), expectedCommit);

		Path artifact = FileLocator.getBundleFileLocation(bundle)
				.orElseThrow(() -> new AssertionError("Cannot locate the resolved Draw2D artifact"))
				.toPath().toRealPath();
		assertTrue("Source-receiver proof requires the packaged Draw2D JAR: " + artifact,
				Files.isRegularFile(artifact));
		String actualBundleHash;
		try (InputStream input = Files.newInputStream(artifact)) {
			actualBundleHash = sha256(input);
		}
		assertEquals("The resolved Draw2D JAR differs from the source-built artifact",
				expectedBundleHash, actualBundleHash);

		URL resource = Figure.class.getResource("Figure.class");
		assertNotNull("Cannot inspect the loaded Figure bytecode", resource);
		assertTrue("Expected an OSGi resource for the loaded Figure: " + resource,
				"bundleentry".equals(resource.getProtocol()) || "bundleresource".equals(resource.getProtocol()));
		String actualFigureHash;
		try (InputStream input = resource.openStream()) {
			actualFigureHash = sha256(input);
		}
		assertEquals("The loaded Figure bytecode differs from the source build",
				expectedFigureHash, actualFigureHash);
		System.out.println("DRAW2D_SOURCE_RECEIVER_PASS sourceCommit=" + expectedCommit
				+ " bundleVersion=" + expectedVersion + " bundleSha256=" + actualBundleHash
				+ " figureSha256=" + actualFigureHash + " resource=" + resource);
	}

	private static String required(String suffix, String expression) {
		String key = "nebula.draw2d." + suffix;
		String value = System.getProperty(key);
		assertNotNull("Missing source-receiver identity: " + key, value);
		assertTrue("Invalid source-receiver identity: " + key, value.matches(expression));
		return value;
	}

	private static void assertSourceReference(String header, String expectedCommit) {
		assertNotNull("Built Draw2D has no Eclipse-SourceReferences header", header);
		String[] parts = header.split(";");
		String repository = parts[0].trim();
		assertTrue("Draw2D source reference does not identify the fork: " + repository,
				SOURCE_REPOSITORY.equals(repository) || (SOURCE_REPOSITORY + ".git").equals(repository));
		Map<String, String> attributes = new HashMap<>();
		for (int index = 1; index < parts.length; index++) {
			String part = parts[index].trim();
			int separator = part.indexOf('=');
			assertTrue("Malformed Draw2D source reference attribute: " + part, separator > 0);
			String name = part.substring(0, separator).trim();
			String value = part.substring(separator + 1).trim();
			if (value.startsWith("\"")) {
				assertTrue("Unclosed Draw2D source reference attribute", value.length() >= 2 && value.endsWith("\""));
				value = value.substring(1, value.length() - 1);
			}
			assertTrue("Duplicate Draw2D source reference attribute: " + name,
					attributes.put(name, value) == null);
		}
		assertEquals("Draw2D source reference names a different module", "org.eclipse.draw2d", attributes.get("path"));
		assertEquals("Draw2D source reference names a different commit", expectedCommit, attributes.get("commitId"));
	}

	private static String sha256(InputStream input) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] buffer = new byte[8192];
		int count;
		while ((count = input.read(buffer)) != -1) {
			digest.update(buffer, 0, count);
		}
		StringBuilder result = new StringBuilder(64);
		for (byte value : digest.digest()) {
			result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
			result.append(Character.forDigit(value & 0x0f, 16));
		}
		return result.toString();
	}
}
