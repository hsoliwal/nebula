<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula Terminal

The `org.eclipse.nebula.widgets.terminal.Terminal` SWT Composite embeds the
**upstream Eclipse Platform terminal emulator**, rather than maintaining a second
VT100/ANSI parser in Nebula. The application supplies
`org.eclipse.terminal.connector.ITerminalConnector` instances; a terminal does not
launch a process or connect to a host until `connect()` is called.

The parent `Terminal` owns all SWT resources and delegates copy/paste, selected
text, font, charset, scrollback, and transport lifecycle to the upstream
`ITerminalViewControl`. State/title callbacks are delivered on the SWT UI
thread. The upstream connector bundles for local process, process, SSH and Telnet
can be provisioned separately by the product.

**Origin:** Eclipse Platform terminal,
https://github.com/eclipse-platform/eclipse.platform/tree/master/terminal
(`org.eclipse.terminal.control`, EPL-2.0). No upstream sources are duplicated.
The new adapter is EPL-2.0. The runtime dependency is a bundle from the Eclipse
Platform p2 repository already configured by Nebula.

Build with `mvn -f releng/org.eclipse.nebula.nebula-parent/pom.xml verify`.
This is an incubating Nebula widget until native SWT tests qualify its behavior
on supported operating systems.

## TerminalWidget: embeddable host widget

The bundle also provides `org.eclipse.nebula.widgets.terminal.TerminalWidget`, an
independently developed host widget over the same upstream control. Both classes
share this bundle and its manifest; `Terminal` above is unchanged.

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

### API and lifecycle constraints

- This widget embeds the upstream Eclipse terminal emulator rather than
  implementing another VT100 state machine. External process/local/SSH/Telnet
  connectors must be installed and licensed independently; their native PTY
  compatibility depends on the chosen Eclipse Platform/CDT distribution.
- `TerminalWidget.paste(String)` uses the widely available upstream
  `ITerminalViewControl.pasteString(String)` API for cross-version
  compatibility. That method is a programmatic send-text operation; it is not
  a guarantee of bracketed-paste semantics. Do not send untrusted commands.
- Core runtime/Equinox dependencies must expose `CoreException` through the
  OSGi manifest. Native lifecycle and UI thread confinement remain those of
  SWT and Eclipse's terminal control.
- Java 21 compile verification is covered by the dedicated
  `.github/workflows/terminal-vnc-contracts.yml` workflow. Native launch
  connectors and real PTY behavior still require per-OS acceptance.
