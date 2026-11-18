<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula Terminal Widget

Embeds the maintained Eclipse Platform terminal emulator rather than copying
its emulator, connector implementations, or workbench view into Nebula.

The module requires Java 21, SWT 3.136+, and \`org.eclipse.terminal.control\`
from the Eclipse Platform target. Install desired connector bundles separately;
available connectors depend on the installed Eclipse target and native PTY
support. Some recent local connector bundles require a newer Java runtime.

\`\`\`java
TerminalWidget terminal = TerminalWidget.withRegisteredConnectors(parent, SWT.NONE);
terminal.connect("registered.connector.id"); // resolve the ID in the host product
\`\`\`

The control, input, clipboard, scrollback and focus semantics remain those of
\`ITerminalViewControl\`. Only an optional embeddable Nebula host widget is added.
