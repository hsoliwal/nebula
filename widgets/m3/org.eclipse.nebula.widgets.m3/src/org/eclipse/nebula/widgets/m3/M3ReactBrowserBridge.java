/*******************************************************************************
 * Copyright (c) 2026 Synexia and contributors.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.eclipse.nebula.widgets.m3.internal.M3ReactFrameProtocol;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.events.DisposeListener;

/**
 * Opt-in React presentation bridge attached to an existing SWT Browser.
 *
 * <p>The caller owns the Nebula widget/model/viewport and supplies each
 * immutable visible-frame JSON projection. This bridge retains only the most
 * recent frame, never a second row model, sorting index, scroll state or
 * per-row SWT Control. The trusted React bundle is loaded from a local file
 * or a developer's loopback HTTP server.
 *
 * <p>All calls and callbacks are on the SWT UI thread. Existing Nebula widget
 * public interfaces and the RAP/RWT renderer are not modified.
 */
public final class M3ReactBrowserBridge implements AutoCloseable {
    /** The CPU owner must independently verify the target row and revision. */
    @FunctionalInterface
    public interface InputHandler {
        void onInput(String action, long frameRevision, int absoluteRowIndex);
    }

    private final Browser browser;
    private final Display display;
    private final URI app;
    private final InputHandler input;
    private final BrowserFunction function;
    private final LocationAdapter navigation;
    private final DisposeListener disposal;
    private long revision;
    private String latestFrame;
    private boolean ready;
    private boolean closed;

    /**
     * Binds one trusted local React entry document to an existing Browser.
     *
     * @throws IllegalArgumentException if the entry is not a regular local
     *         file or an explicit loopback development HTTP URL
     */
    public static M3ReactBrowserBridge attach(
            Browser browser, URI entry, InputHandler input) {
        return new M3ReactBrowserBridge(browser, entry, input);
    }

    private M3ReactBrowserBridge(Browser browser, URI entry, InputHandler input) {
        this.browser = Objects.requireNonNull(browser, "browser");
        this.display = browser.getDisplay();
        requireUiThread();
        if (browser.isDisposed()) {
            SWT.error(SWT.ERROR_WIDGET_DISPOSED);
        }
        this.app = checkedEntry(entry);
        this.input = Objects.requireNonNull(input, "input");

        this.function = new BrowserFunction(browser, "m3NebulaDispatch") {
            @Override
            public Object function(Object[] args) {
                return Boolean.valueOf(accept(args));
            }
        };
        this.navigation = new LocationAdapter() {
            @Override
            public void changing(LocationEvent event) {
                if (!allowedNavigation(event.location)) {
                    event.doit = false;
                } else {
                    ready = false;
                }
            }
        };
        this.disposal = event -> {
            closed = true;
            ready = false;
            latestFrame = null;
        };
        browser.addLocationListener(navigation);
        browser.addDisposeListener(disposal);
        if (!browser.setUrl(app.toASCIIString())) {
            close();
            throw new IllegalStateException("SWT Browser refused trusted React entry");
        }
    }

    /** The caller publishes a newer CPU-owned visible window, never all rows. */
    public void publishFrame(long nextRevision, String visibleFrameJson) {
        requireOpen();
        M3ReactFrameProtocol.validateFrame(nextRevision, visibleFrameJson);
        if (nextRevision <= revision) {
            throw new IllegalArgumentException("non-increasing viewport revision");
        }
        revision = nextRevision;
        latestFrame = visibleFrameJson;
        if (ready) {
            deliver();
        }
    }

    /** Only the current, immutable CPU frame revision accepts React input. */
    private boolean accept(Object[] args) {
        if (closed || !ready && (args == null || args.length != 3
                || !"ready".equals(args[1])) || args == null || args.length != 3) {
            return false;
        }
        long supplied = M3ReactFrameProtocol.exactInteger(
                args[0], M3ReactFrameProtocol.MAX_JS_INTEGER);
        String action = args[1] instanceof String ? (String) args[1] : null;
        long row = M3ReactFrameProtocol.exactInteger(args[2], Integer.MAX_VALUE);
        if (!M3ReactFrameProtocol.allowedAction(action) || supplied < 0 || row < 0) {
            return false;
        }
        if ("ready".equals(action)) {
            if (supplied != 0 || row != 0) {
                return false;
            }
            ready = true;
            if (latestFrame != null) {
                deliver();
            }
            return true;
        }
        if (supplied != revision || !ready) {
            return false;
        }
        input.onInput(action, supplied, (int) row);
        return true;
    }

    private void deliver() {
        if (!browser.execute(M3ReactFrameProtocol.frameScript(revision, latestFrame))) {
            ready = false;
            throw new IllegalStateException("SWT Browser did not accept viewport frame");
        }
    }

    /** Native/browser navigation is constrained to the same trusted entry. */
    private boolean allowedNavigation(String candidate) {
        if ("about:blank".equals(candidate)) {
            return true;
        }
        try {
            URI target = URI.create(candidate);
            if ("file".equalsIgnoreCase(app.getScheme())) {
                return "file".equalsIgnoreCase(target.getScheme())
                        && Path.of(app).toRealPath().equals(Path.of(target).toRealPath());
            }
            return app.getScheme().equalsIgnoreCase(target.getScheme())
                    && app.getHost().equalsIgnoreCase(target.getHost())
                    && app.getPort() == target.getPort()
                    && app.getRawPath().equals(target.getRawPath())
                    && Objects.equals(app.getRawQuery(), target.getRawQuery());
        } catch (RuntimeException | java.io.IOException invalid) {
            return false;
        }
    }

    private static URI checkedEntry(URI entry) {
        URI uri = Objects.requireNonNull(entry, "entry");
        if (uri.getRawUserInfo() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("untrusted React entry");
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            try {
                if (!Files.isRegularFile(Path.of(uri).toRealPath())) {
                    throw new IllegalArgumentException("React bundle entry is not a regular file");
                }
            } catch (java.io.IOException invalid) {
                throw new IllegalArgumentException("missing React bundle entry", invalid);
            }
            return uri;
        }
        String host = uri.getHost();
        if ("http".equalsIgnoreCase(uri.getScheme()) && uri.getPort() > 0
                && ("127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host)
                        || "[::1]".equals(host))) {
            return uri;
        }
        throw new IllegalArgumentException("React entry must be file or loopback HTTP");
    }

    private void requireUiThread() {
        if (Display.getCurrent() != display) {
            SWT.error(SWT.ERROR_THREAD_INVALID_ACCESS);
        }
    }

    private void requireOpen() {
        requireUiThread();
        if (closed || browser.isDisposed()) {
            SWT.error(SWT.ERROR_WIDGET_DISPOSED);
        }
    }

    @Override
    public void close() {
        requireUiThread();
        if (closed) {
            return;
        }
        closed = true;
        ready = false;
        latestFrame = null;
        if (!browser.isDisposed()) {
            browser.removeDisposeListener(disposal);
            browser.removeLocationListener(navigation);
            function.dispose();
        }
    }
}
