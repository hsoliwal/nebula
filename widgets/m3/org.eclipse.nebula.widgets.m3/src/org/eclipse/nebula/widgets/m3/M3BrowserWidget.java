/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.nebula.widgets.m3.internal.M3BrowserScripts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Opt-in Browser fallback for web components that cannot be compiled into
 * existing SWT/Nebula controls. This is NOT the default UI translation path.
 *
 * <p>A single SWT/RWT Browser owns the web document. DOM elements never
 * become individual SWT Controls. Each instance owns its event function,
 * listeners, and session-local state; nothing is stored in static maps.
 * Use only application-trusted HTML, scripts, and same-origin resources.
 * This class is not a sanitizer, browser security boundary, or arbitrary
 * JavaScript/React-to-SWT transpiler.</p>
 *
 * <p>Mark interactive HTML elements with {@code data-nebula-id}. Click,
 * input, and change are delegated in DOM order to registered Java listeners.
 * Form values and check states are transported as strings. Call
 * {@link #setTexts(Map)} or {@link #setValues(Map)} to send a bounded group
 * of deltas in one browser request, not one call per visible element.</p>
 *
 * <p>On RAP/RWT, script evaluation and event callbacks can incur requests.
 * Prefer RAP's native Grid and other RWT widgets for virtual data, and use
 * the RWT Remote API for custom high-frequency controls. No guarantee of
 * RWT parity is implied without runtime qualification.</p>
 */
public final class M3BrowserWidget extends Composite {

    /** A callback runs on the widget's SWT/RWT UI thread; it must not retain UI state across disposal. */
    @FunctionalInterface
    public interface WebEventListener {
        void onEvent(String type, String id, String value);
    }

    private final Browser browser;
    private final BrowserFunction function;
    private final CopyOnWriteArrayList<WebEventListener> listeners = new CopyOnWriteArrayList<>();
    private boolean documentRequested;
    private boolean ready;
    private SWTException bridgeFailure;

    public M3BrowserWidget(Composite parent, int style) {
        super(parent, style);
        setLayout(new FillLayout());
        browser = new Browser(this, SWT.NONE);
        function = new BrowserFunction(browser, "m3NebulaEvent") {
            @Override
            public Object function(Object[] arguments) {
                if (isDisposed() || !ready || !M3BrowserScripts.validEvent(arguments)) {
                    return Boolean.FALSE;
                }
                String type = (String) arguments[0];
                String id = (String) arguments[1];
                String value = (String) arguments[2];
                for (WebEventListener listener : listeners) {
                    listener.onEvent(type, id, value);
                }
                return Boolean.valueOf(!listeners.isEmpty());
            }
        };
        // A trusted setText is the only supported navigation entrypoint.
        // Do not hand the per-session BrowserFunction to an arbitrary URL.
        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent event) {
                if (!documentRequested) {
                    event.doit = false;
                }
            }
        });
        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                if (documentRequested && !isDisposed() && !browser.isDisposed()) {
                    try {
                        installBridge();
                    } catch (SWTException exception) {
                        ready = false;
                        bridgeFailure = exception;
                    } finally {
                        documentRequested = false;
                    }
                }
            }
        });
        addListener(SWT.Dispose, event -> listeners.clear());
    }

    /**
     * Loads only caller-trusted HTML. Returns whether Browser accepted the
     * document; {@link #isBridgeReady()} becomes true after successful load
     * completion and event delegation installation.
     */
    public boolean setTrustedHtml(String html) {
        checkWidget();
        if (html == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        documentRequested = true;
        ready = false;
        bridgeFailure = null;
        boolean accepted = browser.setText(html);
        if (!accepted) {
            documentRequested = false;
        }
        return accepted;
    }

    /**
     * Explicit retry when an engine does not report Progress.completed.
     * Must be called after the document has loaded.
     */
    public boolean installBridge() {
        checkWidget();
        ready = Boolean.TRUE.equals(browser.evaluate(M3BrowserScripts.install()));
        bridgeFailure = null;
        return ready;
    }

    public boolean isBridgeReady() {
        checkWidget();
        return ready;
    }

    /** The last automatic-installation failure, or null (no failure recorded). */
    public SWTException getBridgeFailure() {
        checkWidget();
        return bridgeFailure;
    }

    public void addWebEventListener(WebEventListener listener) {
        checkWidget();
        if (listener == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        listeners.addIfAbsent(listener);
    }

    public void removeWebEventListener(WebEventListener listener) {
        checkWidget();
        if (listener == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        listeners.remove(listener);
    }

    /** Updates textContent; never treats a value as HTML or executable code. */
    public int setTexts(Map<String, String> patches) {
        checkWidget();
        return evaluatePatch(M3BrowserScripts.patch(patches, false));
    }

    /** Updates input/textarea/select value; does not synthesize an input event. */
    public int setValues(Map<String, String> patches) {
        checkWidget();
        return evaluatePatch(M3BrowserScripts.patch(patches, true));
    }

    /**
     * Returns -1 when the bridge is not ready, otherwise the number of DOM
     * elements updated. Caller values are encoded as JS string literals.
     */
    private int evaluatePatch(String script) {
        if (!ready) {
            return -1;
        }
        Object result = browser.evaluate(script);
        return result instanceof Number ? ((Number) result).intValue() : -1;
    }
}
