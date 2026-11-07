// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.swt.widgets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.swt.SWT;
/** TEST-ONLY lightweight SetData event-shape simulator; never product SWT. */
public final class Table {
    private final int style, columns;
    private final List<TableItem> items=new ArrayList<>();
    private final Map<Integer,List<Listener>> listeners=new HashMap<>();
    private int[] selected={};
    private boolean disposed;
    private Runnable duringTextWrite;
    public Table(int style,int columns){this.style=style;this.columns=columns;}
    public int getStyle(){return style;}
    public int getColumnCount(){return columns;}
    public int getItemCount(){return items.size();}
    public TableItem getItem(int i){return items.get(i);}
    public void setItemCount(int count){
        if(count<0)throw new IllegalArgumentException("count");
        while(items.size()>count)items.remove(items.size()-1);
        while(items.size()<count)items.add(new TableItem(this,items.size(),Math.max(1,columns)));
    }
    public void addListener(int type,Listener listener){listeners.computeIfAbsent(type,k->new ArrayList<>()).add(listener);}
    public void removeListener(int type,Listener listener){listeners.getOrDefault(type,List.of()).remove(listener);}
    public void fireSetData(int index){
        Event e=new Event(index,items.get(index));
        for(Listener l:List.copyOf(listeners.getOrDefault(SWT.SetData,List.of())))l.handleEvent(e);
    }
    public void hookOneTextWrite(Runnable action){duringTextWrite=action;}
    void onTextWrite(){
        Runnable run=duringTextWrite;
        duringTextWrite=null;
        if(run!=null)run.run();
    }
    public int[] getSelectionIndices(){return selected.clone();}
    public void select(int[] indices){selected=indices.clone();}
    public void deselectAll(){selected=new int[0];}
    public void clearAll(){}
    public void clear(int first,int last){}
    public boolean isDisposed(){return disposed;}
    public void dispose(){
        disposed=true;
        for(Listener l:List.copyOf(listeners.getOrDefault(SWT.Dispose,List.of())))l.handleEvent(new Event(-1,null));
    }
}
