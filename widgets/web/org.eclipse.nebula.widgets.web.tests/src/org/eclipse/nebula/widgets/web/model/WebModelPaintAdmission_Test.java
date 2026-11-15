// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package org.eclipse.nebula.widgets.web.model;

import static org.junit.Assert.*;
import static org.eclipse.nebula.widgets.web.model.WebStates.*;
import java.util.Arrays;
import org.junit.Test;

/** Source-level paint admission contract; no native toolkit host is created. */
public final class WebModelPaintAdmission_Test {
 public static final class Probe extends WebModel {
  int emissions; long visited; boolean failEmit, walkLogical;
  Probe(){super("visibility-probe",0,0,RECT,V_DEFAULT);}
  @Override public int aspects(){return A_D;}
  void loadSample(String sample){clear();add(sample);}
  @Override protected void doLayout(){if(count>0)setRect(0,0,0,12,12);}
  @Override protected void emit(WebPaint p){
   emissions++;
   if(failEmit)throw new IllegalStateException("visible emit");
   if(walkLogical)for(int i=0;i<count;i++)visited+=flags[i]+1L;
   p.rect(0,0,10,10,0x123456);
  }
  void negativeWidth(){width=-1;}
 }
 static Probe sample(){Probe m=new Probe();m.loadSample("sample");m.layout(64,32);return m;}
 static void empty(Probe m){WebPaint p=new WebPaint();m.paint(p);assertEquals(0,p.count());assertEquals(0,m.emissions);}
 @Test public void hiddenWidgetEmitsNoPaint(){Probe m=sample();m.setState(-1,HIDDEN,true);empty(m);}
 @Test public void hiddenClearsPreviousDisplayList(){Probe m=sample();WebPaint p=new WebPaint();m.paint(p);assertTrue(p.count()>0);m.setState(-1,HIDDEN,true);m.paint(p);assertEquals(0,p.count());assertEquals(1,m.emissions);}
 @Test public void hiddenFocusDoesNotPaint(){Probe m=sample();m.setFocus(0);m.setState(-1,FOCUS|HIDDEN,true);empty(m);}
 @Test public void emptyWidthEmitsNoPaint(){Probe m=sample();m.layout(0,32);empty(m);}
 @Test public void emptyHeightEmitsNoPaint(){Probe m=sample();m.layout(64,0);empty(m);}
 @Test public void emptyBothEmitsNoPaint(){Probe m=sample();m.layout(0,0);empty(m);}
 @Test public void negativeLayoutIsEmpty(){Probe m=sample();m.layout(-1,-17);empty(m);}
 @Test public void negativeProtectedExtentIsEmpty(){Probe m=sample();m.negativeWidth();empty(m);}
 @Test public void visibleKeepsEmissionAndState(){Probe m=sample();m.setFocus(0);m.setState(-1,FOCUS,true);int[] before=m.snapshot();WebPaint p=new WebPaint();m.paint(p);assertEquals(1,m.emissions);assertEquals(2,p.count());assertArrayEquals(before,m.snapshot());}
 @Test public void disabledVisibleStillPaints(){Probe m=sample();m.setState(-1,DISABLED,true);WebPaint p=new WebPaint();m.paint(p);assertEquals(1,m.emissions);assertTrue(p.count()>0);}
 @Test public void hiddenRoundTripRetainsModel(){Probe m=sample();m.setState(-1,HIDDEN,true);WebPaint p=new WebPaint();m.paint(p);assertEquals(0,p.count());m.setState(-1,HIDDEN,false);m.paint(p);assertEquals(1,m.emissions);assertEquals("sample",m.text(0));assertTrue(p.count()>0);}
 @Test public void nullDestinationStillThrows(){Probe m=sample();m.setState(-1,HIDDEN,true);try{m.paint(null);fail("null paint");}catch(NullPointerException expected){}m.layout(0,0);try{m.paint(null);fail("empty null paint");}catch(NullPointerException expected){}}
 @Test(expected=IllegalStateException.class) public void visibleEmitterFailureStillPropagates(){Probe m=sample();m.failEmit=true;m.paint(new WebPaint());}
 @Test public void hiddenEmitterNotCalledWhenItWouldThrow(){Probe m=sample();m.failEmit=true;m.setState(-1,HIDDEN,true);empty(m);}
 @Test public void millionItemHiddenPaintHasNoLogicalWalk(){Probe m=sample();m.setLogicalCount(1_000_000);m.walkLogical=true;m.setState(-1,HIDDEN,true);m.paint(new WebPaint());assertEquals(0,m.emissions);assertEquals(0,m.visited);assertEquals(1_000_000,m.count());}
 @Test public void zeroViewportWithFocusDoesNotPaint(){Probe m=sample();m.setFocus(0);m.setState(-1,FOCUS,true);m.layout(0,0);empty(m);}
 @Test public void otherStateBitsRemainVisible(){for(int bit:new int[]{HOVER,PRESSED,DISABLED,SELECTED,ERROR,FOCUS,CHECKED,OPEN}){Probe m=sample();m.setState(-1,bit,true);m.paint(new WebPaint());assertEquals("bit "+bit,1,m.emissions);}}
 @Test public void hiddenLogicalWorkIsBoundedAcrossCounts(){for(int n:new int[]{0,1,1_000_000}){Probe m=sample();m.setLogicalCount(n);m.walkLogical=true;m.setState(-1,HIDDEN,true);m.paint(new WebPaint());assertEquals(0,m.emissions);assertEquals(0,m.visited);}}
 public static String visibleTranscript(){Probe m=sample();m.setFocus(0);m.setState(-1,FOCUS,true);WebPaint p=new WebPaint();m.paint(p);return p.count()+":"+p.hash()+":"+Arrays.hashCode(m.snapshot());}
 public static long[] hiddenBenchmark(){Probe m=sample();m.setLogicalCount(1_000_000);m.walkLogical=true;m.setState(-1,HIDDEN,true);WebPaint p=new WebPaint();for(int i=0;i<1000;i++)m.paint(p);long[] ns=new long[7];for(int j=0;j<ns.length;j++){long t=System.nanoTime();for(int i=0;i<10000;i++)m.paint(p);ns[j]=(System.nanoTime()-t)/10000;}if(m.emissions!=0||m.visited!=0)throw new AssertionError("hidden benchmark performed model work");Arrays.sort(ns);return new long[]{ns[3],ns[6]};}
}
