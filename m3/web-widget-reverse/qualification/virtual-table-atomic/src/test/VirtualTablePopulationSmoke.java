// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
import org.eclipse.nebula.widgets.m3.M3VirtualTableBinding;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import java.util.Objects;
/** Exercises the ACTUAL product Java source using test-only SWT event shapes. */
public final class VirtualTablePopulationSmoke {
    private VirtualTablePopulationSmoke() {}
    private static final class IndexedRows implements M3VirtualTableBinding.Rows {
        int fetches;
        @Override public int size(){return 10;}
        @Override public int columns(){return 2;}
        @Override public M3VirtualTableBinding.Page fetch(int offset,int length){
            fetches++;
            String[] keys=new String[length];String[] text=new String[length*2];
            for(int i=0;i<length;i++){
                int row=offset+i;keys[i]="row-"+row;text[i*2]="name-"+row;text[i*2+1]="status-"+row;
            }
            return new M3VirtualTableBinding.Page(offset,2,keys,text);
        }
        @Override public int indexOfKey(String key){
            if(!key.startsWith("row-"))return -1;
            int index=Integer.parseInt(key.substring(4));return index<10?index:-1;
        }
    }
    private static void eq(Object expected,Object actual){
        if(!Objects.equals(expected,actual))throw new AssertionError("expected "+expected+" actual "+actual);
    }
    private static void refuses(Runnable run,String label){
        try{run.run();}catch(IllegalStateException expected){return;}
        throw new AssertionError("Expected reentrant refusal: "+label);
    }
    public static void main(String[] args){
        Table table=new Table(SWT.VIRTUAL|SWT.MULTI,2);
        IndexedRows rows=new IndexedRows();
        try(M3VirtualTableBinding binding=M3VirtualTableBinding.bind(table,rows)){
            table.fireSetData(0);
            eq("row-0",table.getItem(0).getData(M3VirtualTableBinding.ROW_KEY));
            eq("name-0",table.getItem(0).getText(0));
            eq("status-0",table.getItem(0).getText(1));
            eq(1,binding.getResidentPageCount());
            // Nested SetData dispatched from TableItem.setText AFTER Rows.fetch
            // returned; the old fetching-only guard failed to reject it.
            table.hookOneTextWrite(()->table.fireSetData(2));
            refuses(()->table.fireSetData(1),"nested SetData from native item writer");
            table.fireSetData(2); // guard must have reset after the exception
            eq("name-2",table.getItem(2).getText(0));
            table.hookOneTextWrite(()->binding.invalidate(0,0));
            refuses(()->table.fireSetData(1),"invalidation during SetData");
            table.hookOneTextWrite(()->binding.replaceRows(new IndexedRows()));
            refuses(()->table.fireSetData(1),"snapshot replacement during SetData");
            table.hookOneTextWrite(()->binding.setSelectedKeys("row-1"));
            refuses(()->table.fireSetData(1),"selected-key mutation during SetData");
            table.hookOneTextWrite(()->table.fireSelection());
            refuses(()->table.fireSetData(1),"SWT.Selection capture during SetData");
            table.fireSetData(3);
            eq("status-3",table.getItem(3).getText(1));
            if(binding.getResidentPageCount()>M3VirtualTableBinding.HOT_PAGES)
                throw new AssertionError("Cache budget exceeded");
            System.out.println("PASS original virtual Table SetData atomicity + 5 reentrant event/mutation refusals; indexed fetches="+rows.fetches);
        }
    }
}
