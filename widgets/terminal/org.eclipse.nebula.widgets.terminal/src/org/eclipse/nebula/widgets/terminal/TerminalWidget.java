/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Nebula project.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.terminal;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.TerminalConnectorExtension;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.ITerminalListener;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.control.TerminalTitleRequestor;
import org.eclipse.terminal.control.TerminalViewControlFactory;

/**
 * SWT embeddable terminal. The Eclipse terminal emulator remains the sole
 * implementation of VT100/ANSI, stream and connector semantics.
 *
 * A TerminalWidget is owned by the SWT UI thread; connector workers are owned
 * by the Eclipse terminal implementation. Listeners are notified on the SWT
 * event thread and are not permitted to mutate disposed controls.
 *
 * Unlike the workbench Terminal view, this widget may be embedded in any
 * Composite, including a Nebula layered/virtualized viewport.
 */
public final class TerminalWidget extends Composite implements ITerminalListener {

    private final Display display;
    private final ITerminalViewControl terminal;
    private final CopyOnWriteArrayList<ITerminalListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a terminal using explicitly supplied Eclipse terminal connectors.
     * Connectors may come from extension points, the local process, SSH or
     * Telnet bundles. No protocol implementation is duplicated.
     */
    public TerminalWidget(Composite parent, int style, ITerminalConnector... connectors) {
        super(parent, style);
        this.display = getDisplay();
        setLayout(new FillLayout());
        ITerminalConnector[] initial = Objects.requireNonNull(connectors, "connectors").clone();
        terminal = TerminalViewControlFactory.makeControl(this, this, initial, false);
    }

    /**
     * Uses connectors registered with Eclipse's terminal extension point.
     */
    public static TerminalWidget withRegisteredConnectors(Composite parent, int style) {
        return new TerminalWidget(parent, style,
                TerminalConnectorExtension.makeTerminalConnectors());
    }

    public ITerminalViewControl getTerminalControl() {
        checkWidget();
        return terminal;
    }

    public void connect(ITerminalConnector connector) {
        checkWidget();
        terminal.setConnector(Objects.requireNonNull(connector, "connector"));
        terminal.connectTerminal();
    }

    public void connect(String connectorId) throws CoreException {
        connect(TerminalConnectorExtension.makeTerminalConnector(
                Objects.requireNonNull(connectorId, "connectorId")));
    }

    public void disconnect() {
        checkWidget();
        terminal.disconnectTerminal();
    }

    public void setCharset(Charset charset) {
        checkWidget();
        terminal.setCharset(Objects.requireNonNull(charset, "charset"));
    }

    public void setScrollbackLimit(int lines) {
        checkWidget();
        if (lines < -1) {
            throw new IllegalArgumentException("lines must be >= -1");
        }
        terminal.setBufferLineLimit(lines);
    }

    public boolean paste(String text) {
        checkWidget();
        return terminal.pasteString(Objects.requireNonNull(text, "text"));
    }

    public String getSelectionText() {
        checkWidget();
        return terminal.getSelection();
    }

    public void addTerminalListener(ITerminalListener listener) {
        checkWidget();
        listeners.addIfAbsent(Objects.requireNonNull(listener, "listener"));
    }

    public void removeTerminalListener(ITerminalListener listener) {
        checkWidget();
        listeners.remove(listener);
    }

    @Override
    public boolean setFocus() {
        checkWidget();
        terminal.setFocus();
        return terminal.getControl().isFocusControl();
    }

    @Override
    public void dispose() {
        if (!isDisposed()) {
            listeners.clear();
            terminal.disposeTerminal();
        }
        super.dispose();
    }

    private void notifyOnUi(Consumer<ITerminalListener> event) {
        if (display.isDisposed()) {
            return;
        }
        display.asyncExec(() -> {
            if (!isDisposed()) {
                for (ITerminalListener listener : listeners) {
                    event.accept(listener);
                }
            }
        });
    }

    @Override
    public void setState(TerminalState state) {
        notifyOnUi(listener -> listener.setState(state));
    }

    @Override
    public void setTerminalSelectionChanged() {
        notifyOnUi(ITerminalListener::setTerminalSelectionChanged);
    }

    @Override
    public void setTerminalTitle(String title, TerminalTitleRequestor requestor) {
        notifyOnUi(listener -> listener.setTerminalTitle(title, requestor));
    }
}
