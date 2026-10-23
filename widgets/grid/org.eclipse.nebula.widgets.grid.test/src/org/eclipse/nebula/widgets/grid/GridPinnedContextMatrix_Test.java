/*******************************************************************************
 * Copyright (c) 2026 Eclipse Nebula contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.junit.Assert.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Event;


public class GridPinnedContextMatrix_Test {
 public static void main(String[] args) {
  assertEquals("one matrix selector",0,args.length);
  assertTrue("matrix uses viewport owner1",GridViewport.UPSTREAM_FIXES);
  System.out.println("MODE\tviewport\t"+GridViewport.UPSTREAM_FIXES);
  System.out.println("PROXY_CLASS_ORIGIN\t"+GridGCProxy.class.getProtectionDomain().getCodeSource().getLocation());
  System.out.println("GRID_CLASS_ORIGIN\t"+Grid.class.getProtectionDomain().getCodeSource().getLocation());
  Display display=null;int failed=0,rows=0;
  try {
   display=new Display();
   for(int entry=0;entry<3;entry++)for(int current=0;current<3;current++)
    for(int clipped=0;clipped<2;clipped++)for(int entryFill:new int[]{SWT.FILL_EVEN_ODD,SWT.FILL_WINDING})
     for(int currentFill:new int[]{SWT.FILL_EVEN_ODD,SWT.FILL_WINDING}) {
      String name="e"+entry+"-c"+current+"-clip"+clipped+"-ef"+entryFill+"-cf"+currentFill;
      rows++;
      try {matrixCase(name,entry,current,clipped!=0,entryFill,currentFill);
       System.out.println("MATRIX_CASE\t"+name+"\tPASS");
      } catch(RuntimeException|AssertionError failure) {
       failed++;System.out.println("MATRIX_CASE\t"+name+"\tFAIL");
       System.out.println(failure.toString());failure.printStackTrace(System.out);
      }
     }
   for(int mode=0;mode<3;mode++) {
    try {bodyPrecondition(mode);System.out.println("BODY_PRECONDITION\t"+mode+"\tPASS");}
    catch(RuntimeException|AssertionError failure) {
     failed++;System.out.println("BODY_PRECONDITION\t"+mode+"\tFAIL");
     System.out.println(failure.toString());failure.printStackTrace(System.out);
    }
   }
  } finally {if(display!=null&&!display.isDisposed())display.dispose();}
  System.out.println("NATIVE_MATRIX\t"+rows+"\t3\t"+failed);
  System.exit(rows==72&&failed==0?0:1);
 }
 private static void configure(GC gc,int mode) {
  if(mode==0){gc.setAdvanced(false);return;}
  gc.setAdvanced(true);
  Transform transform=new Transform(gc.getDevice());
  try {
   if(mode==2)transform.setElements(1.25f,.125f,-.25f,1.5f,9,7);
   gc.setTransform(transform);
  } finally {transform.dispose();}
 }
 private static void matrixCase(String name,int entryMode,int currentMode,boolean explicit,
   int entryFill,int currentFill) {
  Display display=Display.getDefault();
  Image image=new Image(display,224,176),reference=null;GC gc=null,oracle=null;
  Region entryDamage=null,requested=null,result=null,expected=null,caller=null;
  State before=null;
  try {
   gc=new GC(image);configure(gc,entryMode);gc.setFillRule(entryFill);
   caller=new Region(display);caller.add(new Rectangle(3,4,88,128));caller.add(new Rectangle(111,4,88,128));
   caller.subtract(new Rectangle(29,22,19,33));
   if(explicit)gc.setClipping(caller);else gc.setClipping((Region)null);
   boolean entryAdvanced=gc.getAdvanced();float[] entryTransform=elements(gc);
   entryDamage=new Region(display);captureDevice(gc,entryDamage);
   configure(gc,currentMode);gc.setFillRule(currentFill);
   gc.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
   gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_BLUE));
   gc.setFont(display.getSystemFont());gc.setLineWidth(3);gc.setLineStyle(SWT.LINE_DASH);
   if(currentMode!=0)gc.setAlpha(137);
   if(explicit)gc.setClipping(caller);else gc.setClipping((Region)null);
   assertEquals(name+" current mode",currentMode!=0,gc.getAdvanced());
   before=new State(gc);
   Rectangle request=new Rectangle(13,17,139,101);
   reference=new Image(display,224,176);oracle=new GC(reference);configure(oracle,entryMode);
   requested=new Region(display);requested.add(request);oracle.setClipping(requested);
   expected=new Region(display);captureDevice(oracle,expected);expected.intersect(entryDamage);
   result=new Region(display);
   // Exercise the existing full-state owner, not any owner2 Grid helper.
   try(GridGCProxy temporary=GridGCProxy.wrap(gc)) {
    GC target=temporary.gc();
    if(entryAdvanced) {
     target.setAdvanced(true);
     Transform saved=new Transform(display,entryTransform[0],entryTransform[1],entryTransform[2],
       entryTransform[3],entryTransform[4],entryTransform[5]);
     try {target.setTransform(saved);}finally {saved.dispose();}
    } else target.setAdvanced(false);
    target.setClipping(requested);captureDevice(target,result);result.intersect(entryDamage);
   }
   assertSameRegion(name+" physical fence",expected,result,display);
   before.assertRestored(name,gc,explicit);
   System.out.println("MATRIX_OBSERVATION\t"+name+"\tisClipped="+before.clipped+"->"+gc.isClipped()
     +"\tfillRule="+before.fill+"->"+gc.getFillRule()+"\tadvanced="+before.advanced+"->"+gc.getAdvanced());
  } finally {
   if(before!=null)before.dispose();
   if(caller!=null)caller.dispose();if(expected!=null)expected.dispose();if(result!=null)result.dispose();
   if(requested!=null)requested.dispose();if(entryDamage!=null)entryDamage.dispose();
   if(oracle!=null)oracle.dispose();if(gc!=null)gc.dispose();if(reference!=null)reference.dispose();image.dispose();
  }
 }
 private static final class State {
  final boolean advanced,clipped;final int fill,alpha,width,style;
  final Color foreground,background;final Font font;final float[] transform;final Region clip;
  State(GC gc) {
   advanced=gc.getAdvanced();clipped=gc.isClipped();fill=gc.getFillRule();alpha=gc.getAlpha();
   width=gc.getLineWidth();style=gc.getLineStyle();foreground=gc.getForeground();
   background=gc.getBackground();font=gc.getFont();transform=elements(gc);
   clip=new Region(gc.getDevice());captureDevice(gc,clip);
  }
  void assertRestored(String name,GC gc,boolean explicit) {
   assertEquals(name+" advanced",advanced,gc.getAdvanced());
   assertArrayEquals(name+" transform",transform,elements(gc),.0001f);
   assertEquals(name+" fillRule",fill,gc.getFillRule());assertEquals(name+" alpha",alpha,gc.getAlpha());
   assertEquals(name+" width",width,gc.getLineWidth());assertEquals(name+" style",style,gc.getLineStyle());
   assertEquals(name+" foreground",foreground,gc.getForeground());
   assertEquals(name+" background",background,gc.getBackground());assertEquals(name+" font",font,gc.getFont());
   if(explicit)assertEquals(name+" explicit clipping precondition",clipped,gc.isClipped());
   Region actual=new Region(gc.getDevice());
   try {captureDevice(gc,actual);assertSameRegion(name+" current device clipping",clip,actual,Display.getDefault());}
   finally {actual.dispose();}
  }
  void dispose(){clip.dispose();}
 }
 private static void bodyPrecondition(int mode) {
  Display display=Display.getDefault();Shell shell=new Shell(display);
  Image image=null;GC gc=null;Region damage=null;
  try {
   Grid grid=new Grid(shell,SWT.H_SCROLL|SWT.V_SCROLL);
   grid.setSize(320,220);grid.setHeaderVisible(true);grid.setFooterVisible(true);
   grid.setRowHeaderVisible(true);grid.setLinesVisible(true);
   final java.util.Set<String> planes=new java.util.HashSet<>();
   for(int column=0;column<3;column++) {
    final int c=column;GridColumn field=new GridColumn(grid,SWT.NONE);
    field.setWidth(150);field.setText("c"+column);field.setFooterText("f"+column);
    field.setCellRenderer(new DefaultCellRenderer(){
     @Override public void paint(GC value,Object element) {
      assertTrue("actual pinned BODY must have explicit clipping",value.isClipped());
      int row=grid.indexOf((GridItem)element);
      planes.add((row==0?"top":row==19?"bottom":"body")+(c==0?"-fixed":"-regular"));
     }
    });
   }
   grid.getColumn(0).setFixed(true);
   for(int row=0;row<20;row++){GridItem item=new GridItem(grid,SWT.NONE);item.setText("row"+row);}
   grid.setPinnedRowCount(1,1);grid.setTopIndex(4);
   image=new Image(display,640,480);gc=new GC(image);
   // Settle existing scrollbar geometry before choosing disjoint native damage.
   Event event=new Event();event.gc=gc;event.width=320;event.height=220;
   grid.notifyListeners(SWT.Paint,event);planes.clear();
   grid.getHorizontalBar().setSelection(190);grid.getHorizontalBar().notifyListeners(SWT.Selection,new Event());
   configure(gc,mode);damage=new Region(display);
   damage.add(new Rectangle(0,0,160,240));damage.add(new Rectangle(180,0,180,240));gc.setClipping(damage);
   grid.notifyListeners(SWT.Paint,event);
   for(String plane:new String[]{"top-fixed","top-regular","bottom-fixed","bottom-regular","body-fixed","body-regular"})
    assertTrue("actual baseline BODY witness "+mode+" "+plane+" observed="+planes,planes.contains(plane));
   System.out.println("BODY_PLANES\t"+mode+"\t"+new java.util.TreeSet<>(planes));
  } finally {
   if(damage!=null)damage.dispose();if(gc!=null)gc.dispose();if(image!=null)image.dispose();shell.dispose();
  }
 }
 private static void captureDevice(GC gc,Region destination) {
  if(!gc.getAdvanced()){gc.getClipping(destination);return;}
  Transform saved=new Transform(gc.getDevice());
  try {
   gc.getTransform(saved);
   try{gc.setTransform(null);gc.getClipping(destination);}
   finally{gc.setTransform(saved);}
  } finally {saved.dispose();}
 }
	private static void assertSameRegion(String message, Region expected, Region actual, Display display) {
		Region missing = new Region(display);
		Region extra = null;
		try {
			extra = new Region(display);
			missing.add(expected);
			missing.subtract(actual);
			extra.add(actual);
			extra.subtract(expected);
			assertTrue(message + ": missing pixels " + missing.getBounds(), missing.isEmpty());
			assertTrue(message + ": extra pixels " + extra.getBounds(), extra.isEmpty());
		} finally {
			missing.dispose();
			if (extra != null) extra.dispose();
		}
	}

	private static float[] elements(GC gc) {
		Transform transform = new Transform(gc.getDevice());
		try {
			gc.getTransform(transform);
			float[] values = new float[6];
			transform.getElements(values);
			return values;
		} finally {
			transform.dispose();
		}
	}

}
