// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.pagination.client;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JsArrayInteger;
import org.eclipse.nebula.widgets.pagination.internal.PageWindow;

/** GWT exports the SAME pure Java atom. JNI and SWT dependencies are not in this source set. */
public final class PageWindowEntryPoint implements EntryPoint {
    @Override public void onModuleLoad() { export(); }
    private static JsArrayInteger calculate(int current,int total,int slots) {
        if(total<0 || current<0 || (total==0?current!=0:current>=total) || slots<5 || slots>31)
            throw new IllegalArgumentException("invalid page window");
        int[] indexes=PageWindow.getPageIndexes(current,total,slots);
        JsArrayInteger result=JsArrayInteger.createArray().cast();
        for(int index:indexes)result.push(index);
        return result;
    }
    private static native void export() /*-{
      $wnd.NebulaPageWindow = $entry(function(current,total,slots) {
        // Check the complete domain BEFORE JSNI narrows any argument to a Java int.
        if (![current,total,slots].every(Number.isInteger) || total<0 || total>2147483647 ||
            current<0 || (total===0 ? current!==0 : current>=total) || slots<5 || slots>31)
          throw new RangeError("invalid page window");
        var result=@org.eclipse.nebula.widgets.pagination.client.PageWindowEntryPoint::calculate(III)(current,total,slots);
        return result; // JsArrayInteger is a native JavaScript array, unlike Java int[].
      });
    }-*/;
}
