/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package org.eclipse.nebula.widgets.grid;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.eclipse.swt.graphics.Point;

/** Actual Nebula GridCellSelectionList source contract on Java21 with a test-only SWT Point. */
public final class GridCellSelectionHashContract {
    private GridCellSelectionHashContract() { }
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    private static long[] keys(GridCellSelectionList list) throws Exception {
        Field field=GridCellSelectionList.class.getDeclaredField("keys");
        field.setAccessible(true);
        return (long[]) field.get(list);
    }

    private static void growth() throws Exception {
        GridCellSelectionList list=new GridCellSelectionList();
        for(int i=0;i<16;i++)list.add(new Point(i,i+100));
        long[] original=keys(list);
        check(original.length==32,"half-full unique key table");
        for(int i=0;i<500;i++)list.add(new Point(0,100));
        check(keys(list)==original,"duplicate must not expand unique-key hash");
        check(list.size()==516 && list.contains(new Point(0,100)),"duplicate presence and count");
        check(list.remove(new Point(0,100)),"first occurrence remove");
        check(list.contains(new Point(0,100)),"remaining duplicate");
        list.reindex();
        check(keys(list)==original,"reindex after duplicates");
        list.add(new Point(1000,1001));
        check(keys(list).length==64,"new distinct key must still grow");
        System.out.println("GRID_CELL_HASH_RESOURCE beforeDuplicateSlots=64"
                +" afterDuplicateSlots=32 afterDistinctSlots=64");
    }

    private static void differential() {
        Random random=new Random(0x20261116L);
        GridCellSelectionList list=new GridCellSelectionList();
        List<Point> oracle=new ArrayList<>();
        for(int step=0;step<40_000;step++){
            Point p=new Point(random.nextInt(24),random.nextInt(25));
            if(random.nextInt(4)!=0){
                list.add(p);
                oracle.add(new Point(p.x,p.y));
            }else{
                boolean removed=oracle.remove(p);
                check(list.remove(p)==removed,"first occurrence removal");
            }
            if(step%151==0){
                check(list.size()==oracle.size(),"item count");
                check(list.contains(p)==oracle.contains(p),"membership");
                if(!oracle.isEmpty())check(list.get(0).equals(oracle.get(0)),"first item order");
            }
        }
        check(list.size()==oracle.size(),"final count");
        System.out.println("GRID_CELL_HASH_DIFFERENTIAL operations=40000 result=PASS");
    }

    public static void main(String[] args) throws Exception {
        growth();
        differential();
        System.out.println("GRID_CELL_HASH_CONTRACT result=PASS");
    }
}
