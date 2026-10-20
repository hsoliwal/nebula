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

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.engine.RWTServlet;
import org.eclipse.rap.rwt.engine.RWTServletContextListener;

/**
 * Runs the examples as a RAP application in an embedded Jetty on 127.0.0.1, for a browser (headless Chrome takes
 * the screenshots). Arguments: port, web directory (RAP writes its client resources there).
 */
public final class RwtServer {

	private static final Logger LOG = Logger.getLogger ("org.eclipse.nebula.widgets.m3.rwt.examples");

	private RwtServer () {
	}

	public static void main (String[] args) throws Exception {
		int port = Integer.parseInt (args[0]);
		Path web = Files.createDirectories (Path.of (args[1]));
		Server server = new Server (new InetSocketAddress ("127.0.0.1", port));
		ServletContextHandler context = new ServletContextHandler (ServletContextHandler.SESSIONS);
		context.setContextPath ("/");
		context.setBaseResourceAsPath (web);
		context.setInitParameter (ApplicationConfiguration.CONFIGURATION_PARAM, RwtExamples.class.getName ());
		context.setAttribute (ApplicationConfiguration.RESOURCE_ROOT_LOCATION, web.toString ());
		context.addEventListener (new RWTServletContextListener ());
		ServletHolder rwt = new ServletHolder ("rwt", new RWTServlet ());
		for (RwtExample example : RwtExamples.all ()) context.addServlet (rwt, example.path ());
		context.addServlet (new ServletHolder ("resources", new DefaultServlet ()), "/rwt-resources/*");
		server.setHandler (context);
		server.start ();
		LOG.info (() -> "RWT examples ready on http://127.0.0.1:" + port + " (" + RwtExamples.all ().size () + " entry points)");
		server.join ();
	}
}
