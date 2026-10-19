// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.pgroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Independent JDK21 PGroup selection-atom source/contract oracle (no SWT native peers). */
public final class PGroupSelectionParity {
    private static long checks;

    private static class Group {
        final List<Item> items = new ArrayList<>();
        boolean originalClassOwner;
        boolean plainItems = true;
        int getItemsCalls;
        int redraws;
        boolean disposed;
        boolean optimized;
        int itemSnapshots;
        final StringBuilder events = new StringBuilder();

        Group(boolean originalClassOwner) { this.originalClassOwner = originalClassOwner; }
        void add(Item item) {
            items.add(item);
            // Conservative sticky downgrade when any overriding child appeared.
            if (item == null || !item.originalClassItem) plainItems = false;
        }
        void remove(Item item) { items.remove(item); }
        Item[] getToolItems() {
            getItemsCalls++;
            if (disposed) throw new IllegalStateException("disposed group");
            itemSnapshots++;
            return items.toArray(new Item[0]);
        }
        void redraw() {
            redraws++;
            events.append('r');
        }
        boolean clearSelectionWithoutCopy() {
            if (!originalClassOwner || !plainItems) return false;
            int length = items.size();
            for (int i=0; i<length; i++) {
                items.get(i).setSelection(false);
            }
            return true;
        }
    }

    private static class DerivedGroup extends Group {
        DerivedGroup() { super(false); }
        @Override Item[] getToolItems() {
            getItemsCalls++;
            if (disposed) throw new IllegalStateException("disposed group");
            itemSnapshots++;
            Item[] out = items.toArray(new Item[0]);
            if (out.length > 1) {
                Item first = out[0];out[0] = out[out.length-1];out[out.length-1] = first;
            }
            events.append('g');
            return out;
        }
    }

    private static class Item {
        final Group parent;
        final boolean originalClassItem;
        boolean selected;
        int style;
        int calls;
        int callbacks;
        boolean disposed;
        Item(Group parent, int style, boolean originalClassItem) {
            this.parent = parent;
            this.style = style;
            this.originalClassItem = originalClassItem;
            parent.add(this);
        }
        void setSelection(boolean selected) {
            calls++;
            if (disposed) throw new IllegalStateException("disposed item");
            if ((style & 3) == 0) return;
            if (selected) {
                if (!(parent.optimized && originalClassItem && parent.clearSelectionWithoutCopy())) {
                    Item[] selection = parent.getToolItems();
                    for (Item item : selection) item.setSelection(false);
                }
            }
            this.selected = selected;
            parent.events.append(selected ? '+' : '-');
            parent.redraw();
        }
    }

    private static class DerivedItem extends Item {
        DerivedItem(Group parent, int style) { super(parent,style,false); }
        @Override void setSelection(boolean selected) {
            callbacks++;
            parent.events.append('v');
            super.setSelection(selected);
        }
    }

    private record Result(String events, String selected, String failure, int redraws,
                          int getterCalls, int snapCount, int derivedCallbacks) {}

    private static Result execute(int size,int selection,int style,boolean derivedGroup,
                                  int derivedIndex,boolean disposeTarget,boolean removeDerived,
                                  boolean addNull, boolean optimized) {
        Group group = derivedGroup?new DerivedGroup():new Group(true);
        group.optimized = optimized;
        List<Item> all = new ArrayList<>();
        for(int i=0;i<size;i++){
            all.add(i==derivedIndex?new DerivedItem(group,style):new Item(group,style,true));
        }
        if(removeDerived && derivedIndex>=0) group.remove(all.get(derivedIndex));
        if(addNull) group.add(null);
        Item target = all.get(selection);
        target.disposed = disposeTarget;
        String failure = null;
        try {target.setSelection(true);}
        catch(RuntimeException e){failure=e.getClass().getName()+":"+e.getMessage();}
        StringBuilder status = new StringBuilder();
        for(Item item:all)status.append(item.selected?'1':'0');
        int derivedCallbacks=0;
        for(Item item:all)derivedCallbacks+=item.callbacks;
        return new Result(group.events.toString(),status.toString(),failure,
                group.redraws,group.getItemsCalls,group.itemSnapshots,derivedCallbacks);
    }

    private static void eq(Result old, Result now, int size, boolean plain) {
        checks++;
        if (!old.events.equals(now.events)||!old.selected.equals(now.selected)
                ||!java.util.Objects.equals(old.failure,now.failure)
                ||old.redraws!=now.redraws||old.derivedCallbacks!=now.derivedCallbacks) {
            throw new AssertionError("behavior differs size="+size+" old="+old+" new="+now);
        }
        if(plain && size>0 && old.failure==null) {
            checks++;
            if(now.getterCalls!=0||now.snapCount!=0||old.snapCount!=1){
                throw new AssertionError("exact original group retained defensive allocation");
            }
        }
    }

    public static void main(String[] args) {
        boolean quick = args.length == 1 && "--quick".equals(args[0]);
        for (int size=1;size<=(quick?15:50);size++)
            for(int selected=0;selected<size;selected++)
                for (int style: new int[]{0,1,2,3})
                    for(boolean derivedGroup:new boolean[]{false,true})
                        for(int variant=0;variant<5;variant++) {
                            int derivedIndex = variant==0||variant==4?-1:Math.min(variant-1,size-1);
                            boolean removed = variant==3;
                            boolean addNull = variant==4;
                            boolean isPlain = !derivedGroup && derivedIndex==-1 && !addNull && style!=0;
                            Result old=execute(size,selected,style,derivedGroup,derivedIndex,false,removed,addNull,false);
                            Result now=execute(size,selected,style,derivedGroup,derivedIndex,false,removed,addNull,true);
                            eq(old,now,size,isPlain);
                        }
        Random random=new Random(0x4d33504752504cL);
        for(int i=0;i<(quick?1000:180000);i++){
            int size=1+random.nextInt(300);
            int selected=random.nextInt(size);
            int style=random.nextInt(4);
            boolean derivedGroup=random.nextInt(3)==0;
            int derivedIndex=random.nextInt(4)==0?random.nextInt(size):-1;
            boolean removed=derivedIndex>=0&&random.nextBoolean();
            boolean disposed=random.nextInt(150)==0;
            boolean addNull=random.nextInt(70)==0;
            Result old=execute(size,selected,style,derivedGroup,derivedIndex,disposed,removed,addNull,false);
            Result now=execute(size,selected,style,derivedGroup,derivedIndex,disposed,removed,addNull,true);
            eq(old,now,size,!derivedGroup&&derivedIndex<0&&!addNull&&!disposed&&style!=0);
        }
        System.out.println("PGROUP_INTRINSIC_SELECTION_PASS checks="+checks+" quick="+quick);
    }
}
