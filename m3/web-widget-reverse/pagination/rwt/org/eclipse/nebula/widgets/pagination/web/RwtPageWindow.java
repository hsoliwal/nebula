// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.pagination.web;

import java.util.Locale;
import java.util.Objects;
import org.eclipse.nebula.widgets.pagination.PageableController;
import org.eclipse.nebula.widgets.pagination.PageChangedAdapter;
import org.eclipse.rap.json.JsonObject;
import org.eclipse.rap.json.JsonValue;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.remote.AbstractOperationHandler;
import org.eclipse.rap.rwt.remote.RemoteObject;
import org.eclipse.rap.rwt.widgets.WidgetUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

/** Opt-in projection on an application-supplied EMPTY RWT Composite, not an overlay on
 * an already painted native pager. The existing PageableController remains authoritative.
 * Bootstrap web/rwt-handler.mjs before constructing this bridge. UI-thread confined.
 */
public final class RwtPageWindow implements AutoCloseable {
    private final Composite host;
    private final PageableController controller;
    private final RemoteObject remote;
    private final int slots;
    private final String label;
    private final Thread thread;
    private final PageChangedAdapter changes;
    private final Listener dispose;
    private long revision;
    private boolean closed;

    public RwtPageWindow(Composite host,PageableController controller,int slots,String label) {
        this.host=Objects.requireNonNull(host,"host");
        this.controller=Objects.requireNonNull(controller,"controller");
        this.slots=slots;this.label=Objects.requireNonNull(label,"label");
        this.thread=host.getDisplay().getThread();checkThread();
        if(host.isDisposed() || host.getChildren().length!=0)
            throw new IllegalArgumentException("empty live host required");
        JsonObject initial=model(1); // Validate before allocating a remote resource.
        this.changes=new PageChangedAdapter() {
            @Override public void pageIndexChanged(int old,int value,PageableController c){publish();}
            @Override public void pageSizeChanged(int old,int value,PageableController c){publish();}
            @Override public void totalElementsChanged(long old,long value,PageableController c){publish();}
            @Override public void sortChanged(String old,String value,int oldDirection,int direction,PageableController c){publish();}
            @Override public void localeChanged(Locale old,Locale value,PageableController c){publish();}
        };
        this.dispose=e->close();
        this.remote=RWT.getUISession().getConnection().createRemoteObject("nebula.pagination.PageWindow");
        try {
            remote.set("parent",WidgetUtil.getId(host));
            remote.setHandler(new AbstractOperationHandler() {
                @Override public void handleNotify(String event,JsonObject properties) {
                    checkThread();
                    if(closed || host.isDisposed() || !"PageRequest".equals(event) || properties==null)return;
                    JsonValue token=properties.get("revision"),value=properties.get("page");
                    if(token==null || !token.isString() || !Long.toString(revision).equals(token.asString())
                            || value==null || !value.isNumber())return;
                    final int requested;
                    try { requested=value.asInt(); }
                    catch(NumberFormatException invalid){return;}
                    if(requested<0)return; // Preserve the original short-circuit callback boundary.
                    long observed=revision;
                    int total=controller.getTotalPages();
                    // A virtual controller getter may dispose us or publish a newer snapshot.
                    if(closed || host.isDisposed() || revision!=observed || requested<0 || requested>=total)return;
                    controller.setCurrentPage(requested); // Original callbacks, once; no post-callback access.
                }
            });
            remote.listen("PageRequest",true);
            remote.set("model",initial);revision=1;
            controller.addPageChangedListener(changes);
            host.addListener(SWT.Dispose,dispose);
        } catch(RuntimeException | Error failure) {
            try {close();}catch(RuntimeException | Error cleanup){if(cleanup!=failure)failure.addSuppressed(cleanup);}
            throw failure;
        }
    }
    private JsonObject model(long next) {
        int total=controller.getTotalPages(),current=controller.getCurrentPage();
        if(total==0 && current==-1)current=0; // Empty presentation, no controller mutation.
        if(total<0 || current<0 || (total==0?current!=0:current>=total) || slots<5 || slots>31 || label.length()>256)
            throw new IllegalArgumentException("controller not representable by web page window");
        return new JsonObject().add("current",current).add("total",total).add("slots",slots)
                .add("revision",Long.toString(next)).add("label",label);
    }
    private void publish() {
        checkThread();if(closed || host.isDisposed())return;
        long observed=revision;
        long next=Math.addExact(observed,1);JsonObject snapshot=model(next);
        if(closed || host.isDisposed() || revision!=observed)return;
        remote.set("model",snapshot);revision=next;
    }
    private void checkThread() {
        if(Thread.currentThread()!=thread)throw new IllegalStateException("RWT UI thread required");
    }
    @Override public void close() {
        checkThread();if(closed)return;closed=true;
        Throwable failure=null;
        try {controller.removePageChangedListener(changes);}
        catch(RuntimeException | Error problem){failure=problem;}
        try {if(!host.isDisposed())host.removeListener(SWT.Dispose,dispose);}
        catch(RuntimeException | Error problem){failure=retain(failure,problem);}
        try {remote.destroy();}
        catch(RuntimeException | Error problem){failure=retain(failure,problem);}
        if(failure instanceof RuntimeException problem)throw problem;
        if(failure!=null)throw (Error)failure;
    }
    private static Throwable retain(Throwable first,Throwable next) {
        if(first==null)return next;
        if(first!=next)first.addSuppressed(next);
        return first;
    }
}
