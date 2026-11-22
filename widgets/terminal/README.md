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

## API and lifecycle constraints

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
