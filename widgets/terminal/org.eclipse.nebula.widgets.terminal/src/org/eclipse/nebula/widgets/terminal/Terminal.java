/*
 * SPDX-License-Identifier: EPL-2.0
 *
 * Nebula SWT adaptor for the Eclipse Platform Terminal Control.
 * Upstream control: https://github.com/eclipse-platform/eclipse.platform/tree/master/terminal
 */
package org.eclipse.nebula.widgets.terminal;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.ITerminalListener;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.control.TerminalTitleRequestor;
import org.eclipse.terminal.control.TerminalViewControlFactory;

/**
 * Embeddable SWT terminal that uses Eclipse's real VT100/ANSI emulator.
 * Connector implementations (local process, SSH, Telnet, etc.) are supplied by
 * the caller; no shell or network connection is opened by construction.
 *
 * <p>Like every SWT widget, public methods are used on the creating UI thread.
 * Listener notifications are marshalled onto that thread. Dispose this
 * composite to dispose the underlying terminal and its resources.</p>
 */
public class Terminal extends Composite {
    private final ITerminalViewControl control;
    private final Display uiDisplay;
    private final CopyOnWriteArrayList<ITerminalListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates an embedded terminal using Eclipse's registered terminal connectors.
     *
     * @param parent an SWT parent
     * @param style normal SWT Composite styles
     * @param connectors terminal connectors supplied by the application
     */
    public Terminal(Composite parent, int style, ITerminalConnector... connectors) {
        super(parent, style);
        Objects.requireNonNull(connectors, "connectors");
        uiDisplay = getDisplay();
        setLayout(new FillLayout());
        ITerminalConnector[] supported = connectors.clone();
        for (ITerminalConnector connector : supported) {
            Objects.requireNonNull(connector, "connector");
        }

        control = TerminalViewControlFactory.makeControl(new ITerminalListener() {
            @Override
            public void setState(TerminalState state) {
                dispatch(listener -> listener.setState(state));
            }

            @Override
            public void setTerminalSelectionChanged() {
                dispatch(ITerminalListener::setTerminalSelectionChanged);
            }

            @Override
            public void setTerminalTitle(String title, TerminalTitleRequestor requestor) {
                dispatch(listener -> listener.setTerminalTitle(title, requestor));
            }
        }, this, supported, false);
        if (supported.length > 0 && control.getTerminalConnector() == null) {
            control.setConnector(supported[0]);
        }
        addDisposeListener(event -> {
            listeners.clear();
            if (!control.isDisposed()) {
                control.disposeTerminal();
            }
        });
    }

    private void dispatch(Consumer<ITerminalListener> notification) {
        Runnable task = () -> {
            if (!isDisposed()) {
                for (ITerminalListener listener : listeners) {
                    notification.accept(listener);
                }
            }
        };
        if (Thread.currentThread() == uiDisplay.getThread()) {
            task.run();
        } else if (!uiDisplay.isDisposed()) {
            uiDisplay.asyncExec(task);
        }
    }

    public void addTerminalListener(ITerminalListener listener) {
        checkWidget();
        listeners.addIfAbsent(Objects.requireNonNull(listener, "listener"));
    }

    public void removeTerminalListener(ITerminalListener listener) {
        checkWidget();
        listeners.remove(listener);
    }

    /** The upstream control, for advanced behavior without a duplicate facade. */
    public ITerminalViewControl getTerminalControl() {
        checkWidget();
        return control;
    }

    public void setConnector(ITerminalConnector connector) {
        checkWidget();
        control.setConnector(Objects.requireNonNull(connector, "connector"));
    }

    public void connect() {
        checkWidget();
        control.connectTerminal();
    }

    public void disconnect() {
        checkWidget();
        control.disconnectTerminal();
    }

    public boolean isConnected() {
        checkWidget();
        return control.isConnected();
    }

    public void setCharset(Charset charset) {
        checkWidget();
        control.setCharset(Objects.requireNonNull(charset, "charset"));
    }

    public void setBufferLineLimit(int lineLimit) {
        checkWidget();
        control.setBufferLineLimit(lineLimit);
    }

    public void setScrollLock(boolean locked) {
        checkWidget();
        control.setScrollLock(locked);
    }

    public void clear() {
        checkWidget();
        control.clearTerminal();
    }

    public void selectAll() {
        checkWidget();
        control.selectAll();
    }

    public String getSelection() {
        checkWidget();
        return control.getSelection();
    }

    public void copy() {
        checkWidget();
        control.copy();
    }

    public void paste() {
        checkWidget();
        control.paste();
    }

    public boolean paste(String text) {
        checkWidget();
        return control.paste(Objects.requireNonNull(text, "text"));
    }

    @Override
    public boolean setFocus() {
        checkWidget();
        control.setFocus();
        return control.getControl().isFocusControl();
    }

    @Override
    protected void checkSubclass() {
        // Nebula follows the standard SWT convention for extending widgets.
    }
}
