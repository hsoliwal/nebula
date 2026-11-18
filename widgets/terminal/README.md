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
