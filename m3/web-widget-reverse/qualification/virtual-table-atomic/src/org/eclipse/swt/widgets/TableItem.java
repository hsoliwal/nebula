// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.swt.widgets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
public final class TableItem {
    private final Table owner;
    private final int index;
    private final String[] values;
    private final Map<String,Object> data=new HashMap<>();
    TableItem(Table table,int index,int columns){
        owner=table;this.index=index;values=new String[columns];Arrays.fill(values,"");
    }
    public int getIndex(){return index;}
    public void setData(String key,Object value){data.put(key,value);}
    public Object getData(String key){return data.get(key);}
    public void setText(int column,String text){values[column]=text;owner.onTextWrite();}
    public String getText(int column){return values[column];}
}
