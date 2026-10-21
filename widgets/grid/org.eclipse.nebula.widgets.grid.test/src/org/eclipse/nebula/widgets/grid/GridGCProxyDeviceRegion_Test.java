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
import org.junit.Test;


public class GridGCProxyDeviceRegion_Test {
 @org.junit.Rule public final org.junit.rules.ErrorCollector failures=new org.junit.rules.ErrorCollector();
 @org.junit.BeforeClass public static void requireViewport() {
  assertTrue("Exact device repair fixture requires the existing viewport mode",GridViewport.UPSTREAM_FIXES);
 }
 public static void main(String[] args) {
  boolean original=args.length==1&&"original".equals(args[0]);
  assertTrue("Only the original suite selector is accepted",args.length==0||original);
  assertEquals("startup mode",!original,GridViewport.UPSTREAM_FIXES);
  System.out.println("MODE\t"+(original?"original":"viewport")+"\t"+GridViewport.UPSTREAM_FIXES);
  System.out.println("PROXY_CLASS_ORIGIN\t"+GridGCProxy.class.getProtectionDomain().getCodeSource().getLocation());
  Display display=null;int exit=1;
  try {
   display=new Display();
   org.junit.runner.Result result=original?org.junit.runner.JUnitCore.runClasses(GridGCProxy_Test.class):
     org.junit.runner.JUnitCore.runClasses(GridGCProxy_Test.class,GridGCProxyDeviceRegion_Test.class);
   for(org.junit.runner.notification.Failure failure:result.getFailures())System.out.println(failure.getTrace());
   System.out.println("NATIVE_JUNIT\t"+result.getRunCount()+"\t"+result.getFailureCount()+"\t"+result.getIgnoreCount());
   if(result.wasSuccessful()&&result.getRunCount()==(original?9:23)&&result.getIgnoreCount()==0)exit=0;
  } finally {if(display!=null&&!display.isDisposed())display.dispose();}
  System.exit(exit);
 }

 @Test public void identityGdi() {
  deviceCase("identity-gdi",false,g->{},p->{},g->{},new Rectangle(0,0,160,100));
 }
 @Test public void identityAdvanced() {
  deviceCase("identity-advanced",true,g->{},p->{},g->{},new Rectangle(0,0,160,100));
 }
 @Test public void pendingIntegerTranslate() {
  deviceCase("pending-integer-translate",true,g->{},p->p.translate(7,3),
    g->multiply(g,1,0,0,1,7,3),new Rectangle(0,0,160,100));
 }
 @Test public void entryAffinePendingFractionalTranslate() {
  deviceCase("entry-affine-pending-fractional",true,
    g->affine(g,1.5f,.25f,-.2f,1.25f,7.5f,9.25f),
    p->p.translate(11.25f,-3.5f),g->multiply(g,1,0,0,1,11.25f,-3.5f),
    new Rectangle(0,0,160,100));
 }
 @Test public void pendingNonCommutingScaleThenTranslate() {
  deviceCase("pending-scale-then-translate",true,g->affine(g,1,0,0,1,3,5),
    p->{p.transform(GridTransform.scale(2,3));p.translate(7,11);},
    g->{multiply(g,2,0,0,3,0,0);multiply(g,1,0,0,1,7,11);},
    new Rectangle(0,0,160,100));
 }
 @Test public void directGcTransformBeforeClip() {
  deviceCase("direct-gc-transform",true,g->{},
    p->affine(p.gc(),1.25f,.1f,-.2f,1.5f,9.5f,-2.25f),
    g->affine(g,1.25f,.1f,-.2f,1.5f,9.5f,-2.25f),
    new Rectangle(0,0,160,100));
  deviceCase("direct-advanced-reset-after-affine-entry",true,
    g->affine(g,1.5f,.25f,-.2f,1.25f,7.5f,9.25f),
    p->p.gc().setAdvanced(false),g->g.setAdvanced(false),new Rectangle(0,0,160,100));
 }
 @Test public void interveningGcClipRegainsEntry() {
  deviceCase("intervening-gc-clip-regains-entry",true,g->{},
    p->p.gc().setClipping(new Rectangle(5,5,3,3)),g->{},
    new Rectangle(0,0,160,100));
 }
 @Test public void repeatedSmallThenLargerClipRegainsEntry() {
  deviceCase("repeat-larger-regains-entry",true,g->{},
    p->p.clip(new Rectangle(5,5,3,3)),g->{},new Rectangle(0,0,160,100));
 }
 @Test public void emptyRequest() {
  deviceCase("empty-request",true,g->{},p->{},g->{},new Rectangle(2,2,0,7));
 }

 @Test public void nestedLiveAndClose() { nestedCase(false); }
 @Test public void nestedThrowThenNextRenderer() { nestedCase(true); }
 private void nestedCase(boolean throwing) {
  String name=throwing?"nested-throw-next-renderer":"nested-live-and-close";
  Display display=Display.getDefault();Image image=null;GC gc=null;
  Region caller=null,outerClip=null,observedClip=null;
  try {
   image=new Image(display,192,160);gc=new GC(image);gc.setAdvanced(true);
   caller=new Region(display);outerClip=new Region(display);observedClip=new Region(display);
   caller.add(new Rectangle(4,4,80,64));caller.add(new Rectangle(100,4,48,64));
   caller.subtract(new Rectangle(29,19,14,18));
   affine(gc,1,0,0,1,3,5);gc.setClipping(caller);captureDevice(gc,caller);
   float[] callerTransform=elements(gc);int callerAlpha=gc.getAlpha();
   LineAttributes callerStroke=gc.getLineAttributes();GC observed=gc;
   try(GridGCProxy outer=GridGCProxy.wrap(gc)) {
    outer.translate(7,11);outer.clip(new Rectangle(10,10,100,90));
    liveNative(name+".outer",gc,caller,new Rectangle(10,10,100,90),
      new float[][]{{1,0,0,1,3,5},{1,0,0,1,7,11}});
    outer.alpha(177);outer.lineAttributes(new LineAttributes(4f));
    captureDevice(gc,outerClip);float[] outerTransform=elements(gc);
    LineAttributes outerStroke=gc.getLineAttributes();
    try {
     try(GridGCProxy inner=GridGCProxy.wrap(gc)) {
      inner.translate(-3,9);inner.clip(new Rectangle(15,15,9,9));
      liveNative(name+".inner",gc,outerClip,new Rectangle(15,15,9,9),
        new float[][]{{1,0,0,1,3,5},{1,0,0,1,7,11},{1,0,0,1,-3,9}});
      inner.alpha(23);inner.lineAttributes(new LineAttributes(7f));
      if(throwing)throw new IllegalStateException("simulated renderer failure");
     }
     if(throwing)fail("renderer must throw");
    } catch(IllegalStateException expected) {
     if(!throwing)throw expected;
     assertEquals("simulated renderer failure",expected.getMessage());
    }
    captureDevice(gc,observedClip);
    checkRegion(name,"inner-restored",outerClip,observedClip,display);
    check(name,"inner-state",()->{
     assertArrayEquals(outerTransform,elements(observed),.0001f);
     assertEquals(177,observed.getAlpha());assertLineAttributes(outerStroke,observed.getLineAttributes());
    });
    if(throwing) {
     try(GridGCProxy nextRenderer=GridGCProxy.wrap(gc)) {
      nextRenderer.translate(2,-3);nextRenderer.gc();
     }
     captureDevice(gc,observedClip);
     checkRegion(name,"next-renderer-restored",outerClip,observedClip,display);
     check(name,"next-renderer-state",()->{
      assertArrayEquals(outerTransform,elements(observed),.0001f);
      assertEquals(177,observed.getAlpha());assertLineAttributes(outerStroke,observed.getLineAttributes());
     });
    }
   }
   captureDevice(gc,observedClip);checkRegion(name,"restored",caller,observedClip,display);
   check(name,"state",()->{
    assertArrayEquals(callerTransform,elements(observed),.0001f);
    assertEquals(callerAlpha,observed.getAlpha());assertLineAttributes(callerStroke,observed.getLineAttributes());
   });
  } finally {
   if(observedClip!=null)observedClip.dispose();if(outerClip!=null)outerClip.dispose();
   if(caller!=null)caller.dispose();if(gc!=null)gc.dispose();if(image!=null)image.dispose();
  }
 }

 @Test public void negativeRequestUsesNativeRegionAddSemantics() {
  Display display=Display.getDefault();Image image=null,reference=null;GC gc=null,oracle=null;
  Region entry=null,invalid=null,expected=null,live=null,restored=null;
  try {
   image=new Image(display,96,64);reference=new Image(display,96,64);
   gc=new GC(image);oracle=new GC(reference);gc.setAdvanced(true);oracle.setAdvanced(true);
   entry=new Region(display);entry.add(new Rectangle(4,4,16,16));entry.add(new Rectangle(28,4,16,16));
   gc.setClipping(entry);captureDevice(gc,entry);
   invalid=new Region(display);Rectangle request=new Rectangle(4,4,-1,8);
   RuntimeException expectedFailure=null;
   try{invalid.add(request);invalid.getBounds();}catch(RuntimeException failure){expectedFailure=failure;}
   multiply(oracle,1,0,0,1,7,3);
   expected=new Region(display);live=new Region(display);restored=new Region(display);
   try(GridGCProxy proxy=GridGCProxy.wrap(gc)) {
    proxy.translate(7,3);
    if(expectedFailure!=null) {
     RuntimeException actual=assertThrows(RuntimeException.class,()->proxy.clip(request));
     assertEquals(expectedFailure.getClass(),actual.getClass());
     assertEquals(expectedFailure.getMessage(),actual.getMessage());
     assertEquals(1,proxy.nativeStateTransitionCount());
     assertArrayEquals(elements(oracle),elements(gc),.0001f);
     captureDevice(gc,live);checkRegion("negative-request","live-after-rejection",entry,live,display);
    } else {
     nativeExpected(oracle,entry,request,expected);proxy.clip(request);
     captureDevice(gc,live);checkRegion("negative-request","live",expected,live,display);
    }
   }
   captureDevice(gc,restored);checkRegion("negative-request","restored",entry,restored,display);
   GC observed=gc;
   check("negative-request","state",()->assertArrayEquals(new float[]{1,0,0,1,0,0},elements(observed),.0001f));
  } finally {
   if(restored!=null)restored.dispose();if(live!=null)live.dispose();if(expected!=null)expected.dispose();
   if(invalid!=null)invalid.dispose();if(entry!=null)entry.dispose();
   if(oracle!=null)oracle.dispose();if(gc!=null)gc.dispose();
   if(reference!=null)reference.dispose();if(image!=null)image.dispose();
  }
 }
 @Test public void nullAndClosedPrecedence() {
  assertThrows(IllegalArgumentException.class,()->GridGCProxy.wrap(null));
  Display display=Display.getDefault();Image image=null;GC gc=null;
  try {
   image=new Image(display,32,32);gc=new GC(image);gc.setAdvanced(true);
   float[] entry=elements(gc);GC observed=gc;
   GridGCProxy proxy=GridGCProxy.wrap(gc);
   try {
    proxy.translate(7,3);
    assertThrows(IllegalArgumentException.class,()->proxy.clip(null));
    assertThrows(IllegalArgumentException.class,()->proxy.transform(null));
    assertEquals(0,proxy.nativeStateTransitionCount());
    assertArrayEquals(entry,elements(observed),.0001f);
   } finally {proxy.close();}
   proxy.close();
   assertThrows(IllegalStateException.class,()->proxy.clip(null));
   assertThrows(IllegalStateException.class,()->proxy.transform(null));
   assertThrows(IllegalStateException.class,proxy::gc);
   System.out.println("CASE\tnull-closed\tcontrols\tPASS");
  } finally {if(gc!=null)gc.dispose();if(image!=null)image.dispose();}
 }
 @Test public void disposedWrapAndActiveGcDisposal() {
  Display display=Display.getDefault();Image image=null;GC gc=null,disposed=null;
  try {
   image=new Image(display,32,32);disposed=new GC(image);disposed.dispose();
   GC rejected=disposed;assertThrows(IllegalArgumentException.class,()->GridGCProxy.wrap(rejected));
   gc=new GC(image);GridGCProxy proxy=GridGCProxy.wrap(gc);
   try {
    gc.dispose();
    assertThrows(IllegalStateException.class,()->proxy.clip(null));
    assertThrows(IllegalStateException.class,()->proxy.transform(null));
    assertThrows(IllegalStateException.class,proxy::gc);
   } finally {proxy.close();}
   proxy.close();
   System.out.println("CASE\tdisposed\tcontrols\tPASS");
  } finally {
   if(gc!=null&&!gc.isDisposed())gc.dispose();
   if(disposed!=null&&!disposed.isDisposed())disposed.dispose();
   if(image!=null)image.dispose();
  }
 }

 private void deviceCase(String name,boolean advanced,java.util.function.Consumer<GC> initial,
   java.util.function.Consumer<GridGCProxy> changes,java.util.function.Consumer<GC> nativeChanges,
   Rectangle request) {
  Display display=Display.getDefault();
  Image image=null,reference=null;
  GC gc=null,oracle=null;
  Region entry=null,expected=null,live=null,restored=null;
  try {
   image=new Image(display,192,160);reference=new Image(display,192,160);
   gc=new GC(image);oracle=new GC(reference);
   gc.setAdvanced(advanced);oracle.setAdvanced(advanced);
   initial.accept(gc);initial.accept(oracle);
   entry=new Region(display);
   entry.add(new Rectangle(4,4,80,64));
   entry.add(new Rectangle(100,4,48,64));
   entry.subtract(new Rectangle(29,19,14,18));
   gc.setClipping(entry);captureDevice(gc,entry);
   assertRegionHasInteriorGap("entry has islands and a hole",entry,display);
   boolean savedAdvanced=gc.getAdvanced();
   Color savedForeground=gc.getForeground(),savedBackground=gc.getBackground();
   Font savedFont=gc.getFont();
   LineAttributes savedStroke=gc.getLineAttributes();
   int savedAlpha=gc.getAlpha();
   float[] savedTransform=savedAdvanced?elements(gc):new float[]{1,0,0,1,0,0};
   expected=new Region(display);live=new Region(display);restored=new Region(display);
   nativeChanges.accept(oracle);nativeExpected(oracle,entry,request,expected);
   boolean expectedLiveAdvanced=oracle.getAdvanced();
   try(GridGCProxy proxy=GridGCProxy.wrap(gc)) {
    changes.accept(proxy);proxy.clip(request);
    captureDevice(gc,live);checkRegion(name,"live",expected,live,display);
    boolean liveAdvanced=gc.getAdvanced();
    check(name,"live-mode",()->assertEquals("clip must preserve the renderer mode",expectedLiveAdvanced,liveAdvanced));
   }
   captureDevice(gc,restored);checkRegion(name,"restored",entry,restored,display);
   GC observed=gc;
   check(name,"state",()->{
    assertEquals(savedAdvanced,observed.getAdvanced());
    assertEquals(savedForeground,observed.getForeground());
    assertEquals(savedBackground,observed.getBackground());
    assertEquals(savedFont,observed.getFont());
    assertEquals(savedAlpha,observed.getAlpha());
    assertLineAttributes(savedStroke,observed.getLineAttributes());
    if(savedAdvanced)assertArrayEquals(savedTransform,elements(observed),.0001f);
   });
  } finally {
   if(restored!=null)restored.dispose();if(live!=null)live.dispose();
   if(expected!=null)expected.dispose();if(entry!=null)entry.dispose();
   if(oracle!=null)oracle.dispose();if(gc!=null)gc.dispose();
   if(reference!=null)reference.dispose();if(image!=null)image.dispose();
  }
 }
 private void check(String name,String kind,Runnable assertion) {
  try {assertion.run();System.out.println("CASE\t"+name+"\t"+kind+"\tPASS");}
  catch(AssertionError failure) {
   System.out.println("CASE\t"+name+"\t"+kind+"\tFAIL");
   failures.addError(failure);
  }
 }
 private void checkRegion(String name,String kind,Region expected,Region actual,Display display) {
  check(name,kind,()->assertSameRegion(name+"."+kind,expected,actual,display));
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
 private static void affine(GC gc,float... values) {
  Transform nativeTransform=new Transform(gc.getDevice(),values[0],values[1],values[2],values[3],values[4],values[5]);
  try{gc.setTransform(nativeTransform);}finally{nativeTransform.dispose();}
 }
 private static void multiply(GC gc,float... values) {
  Transform current=new Transform(gc.getDevice()),delta=null;
  try {
   gc.getTransform(current);
   delta=new Transform(gc.getDevice(),values[0],values[1],values[2],values[3],values[4],values[5]);
   current.multiply(delta);gc.setTransform(current);
  } finally {if(delta!=null)delta.dispose();current.dispose();}
 }
 private static void nativeExpected(GC oracle,Region entry,Rectangle request,Region expected) {
  Region requested=new Region(oracle.getDevice());
  try {
   requested.add(request);oracle.setClipping(requested);
   captureDevice(oracle,expected);expected.intersect(entry);
  } finally {requested.dispose();}
 }
 private void liveNative(String name,GC gc,Region entry,Rectangle request,float[][] transforms) {
  Display display=Display.getDefault();Image reference=null;GC oracle=null;
  Region expected=null,actual=null;
  try {
   reference=new Image(display,192,160);oracle=new GC(reference);oracle.setAdvanced(true);
   affine(oracle,transforms[0]);
   for(int i=1;i<transforms.length;i++)multiply(oracle,transforms[i]);
   expected=new Region(display);actual=new Region(display);
   nativeExpected(oracle,entry,request,expected);captureDevice(gc,actual);
   checkRegion(name,"live",expected,actual,display);
  } finally {
   if(actual!=null)actual.dispose();if(expected!=null)expected.dispose();
   if(oracle!=null)oracle.dispose();if(reference!=null)reference.dispose();
  }
 }
	private static void assertRegionHasInteriorGap(String message, Region region, Display display) {
		Region rectangle = new Region(display);
		try {
			rectangle.add(region.getBounds());
			rectangle.subtract(region);
			assertFalse(message, rectangle.isEmpty());
		} finally {
			rectangle.dispose();
		}
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

	private static void assertLineAttributes(LineAttributes expected, LineAttributes actual) {
		assertEquals(expected.width, actual.width, 0.0001f);
		assertEquals(expected.cap, actual.cap);
		assertEquals(expected.join, actual.join);
		assertEquals(expected.style, actual.style);
		assertEquals(expected.dashOffset, actual.dashOffset, 0.0001f);
		assertEquals(expected.miterLimit, actual.miterLimit, 0.0001f);
		if (expected.dash == null) {
			assertNull(actual.dash);
		} else {
			assertArrayEquals(expected.dash, actual.dash, 0.0001f);
		}
	}
}
