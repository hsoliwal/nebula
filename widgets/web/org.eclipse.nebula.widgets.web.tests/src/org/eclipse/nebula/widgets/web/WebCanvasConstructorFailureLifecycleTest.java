/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.web;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import org.eclipse.nebula.widgets.web.model.AnimvalueModel;
import org.eclipse.nebula.widgets.web.model.WebDelta;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.widgets.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Pre-teardown native constructor-failure observations; no application repair or screen proof. */
public final class WebCanvasConstructorFailureLifecycleTest {
 private FaultDisplay display;
 private Shell shell;
 private Composite parent;
 private Canvas foreign;
 private final Listener foreignHide=event->{},foreignIconify=event->{};
 private Model model;
 private Control captured;
 private long capturedHandle,foreignHandle,parentHandle;
 private Runnable capturedTicker;
 private Listener capturedAncestor;
 private Set<Object> beforeResources;
 private final IdentityHashMap<Font,Integer> displayOwnedFonts=new IdentityHashMap<>();
 private Map<?,?> providerRegistries;
 private static final List<Map<String,Object>> OBSERVATIONS=new ArrayList<>();
 private static int completedCases,closedCases;
 private static void need(boolean condition,String label){if(!condition)throw new AssertionError(label);}
 private static Object field(Object owner,Class<?> type,String name)throws ReflectiveOperationException{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 private static Object field(Object owner,String name)throws ReflectiveOperationException{return field(owner,owner.getClass(),name);}
 private static final class FaultDisplay extends Display {
  WebCanvasConstructorFailureLifecycleTest owner;
  boolean armed,cleanupFailure;
  int failAt,attempts,timerCalls,positiveTimers;
  final RuntimeException primary=new RuntimeException("controlled late addFilter failure");
  final RuntimeException cleanup=new RuntimeException("controlled removeFilter failure");
  final List<Listener> added=new ArrayList<>(),removed=new ArrayList<>();
  final List<Integer> addedTypes=new ArrayList<>(),removedTypes=new ArrayList<>();
  FaultDisplay(DeviceData data){super(data);}
  @Override protected void checkSubclass(){ }
  @Override public void addFilter(int type,Listener listener){
   super.addFilter(type,listener);
   if(armed&&(type==SWT.Hide||type==SWT.Iconify)){
    added.add(listener);addedTypes.add(type);
    if(++attempts==failAt){
     owner.captureAtFault();
     throw primary;
    }
   }
  }
  @Override public void removeFilter(int type,Listener listener){
   super.removeFilter(type,listener);
   if(armed&&(type==SWT.Hide||type==SWT.Iconify)){
    removed.add(listener);removedTypes.add(type);
    if(cleanupFailure&&type==SWT.Hide){cleanupFailure=false;throw cleanup;}
   }
  }
  @Override public void timerExec(int ms,Runnable callback){timerCalls++;if(ms>=0)positiveTimers++;super.timerExec(ms,callback);}
 }
 private static final class Model extends AnimvalueModel {
  WebDelta installed;
  int deltaInstalls,pulses,blitReads,damageReads;
  Model(){super("constructor-failure-lifecycle",0,SPINNER,0,0);}
  @Override public void setDelta(WebDelta value){installed=value;deltaInstalls++;super.setDelta(value);}
  @Override public int takeBlit(){blitReads++;return super.takeBlit();}
  @Override public boolean takeDamage(int[] out){damageReads++;return super.takeDamage(out);}
  void pulse(){pulses++;animate();}
  void resetReads(){blitReads=damageReads=0;}
 }
 private Set<Object> resources(){
  Set<Object> result=Collections.newSetFromMap(new IdentityHashMap<>());
  Object[] values=display.getDeviceData().objects;
  if(values!=null)for(Object value:values)if(value instanceof Resource resource&&!resource.isDisposed()&&!(value instanceof Color))result.add(value);
  return result;
 }
 private int requireDisplayOwnedFont(Font font)throws Exception{
  Field root=Class.forName("org.eclipse.swt.internal.SWTFontProvider").getDeclaredField("fontRegistries");root.setAccessible(true);providerRegistries=(Map<?,?>)root.get(null);Object registry=providerRegistries.get(display);
  need(registry!=null&&registry.getClass().getName().equals("org.eclipse.swt.internal.ScalingSWTFontRegistry"),"UNKNOWN_FONT_REGISTRY");need(field(registry,"device")==display,"FOREIGN_FONT_REGISTRY");
  Map<?,?> containers=(Map<?,?>)field(registry,"fontsKeyMap");
  for(Object c:containers.values())if(c.getClass().getName().equals("org.eclipse.swt.internal.ScalingSWTFontRegistry$ScaledCustomFontContainer")){
   Class<?> base=c.getClass().getSuperclass();need(base.getName().equals("org.eclipse.swt.internal.ScalingSWTFontRegistry$ScaledFontContainer"),"UNKNOWN_FONT_CONTAINER");Field f=base.getDeclaredField("scaledFonts");f.setAccessible(true);Map<?,?> fonts=(Map<?,?>)f.get(c);
   for(var e:fonts.entrySet())if(e.getValue()==font){need(e.getKey() instanceof Integer,"FONT_ZOOM_TYPE");return (Integer)e.getKey();}
  }
  throw new AssertionError("UNOWNED_TRACKED_FONT");
 }
 private Listener[] filters(int type)throws Exception{
  Object table=field(display,Display.class,"filterTable");if(table==null)return new Listener[0];Method m=table.getClass().getDeclaredMethod("getListeners",int.class);m.setAccessible(true);return (Listener[])m.invoke(table,type);
 }
 private int countFilter(int type,Listener listener)throws Exception{int n=0;for(Listener value:filters(type))if(value==listener)n++;return n;}
 private int queued(Runnable own)throws Exception{
  Runnable[] values=(Runnable[])field(display,Display.class,"timerList");int n=0;if(values!=null)for(Runnable value:values)if(value==own)n++;return n;
 }
 private static List<Long> nativeChildren(long parent){
  List<Long> result=new ArrayList<>();Set<Long> seen=new HashSet<>();
  for(long handle=OS.GetWindow(parent,OS.GW_CHILD);handle!=0;handle=OS.GetWindow(handle,OS.GW_HWNDNEXT)){
   need(result.size()<32&&seen.add(handle),"NATIVE_CHILD_ENUMERATION_UNCERTAIN");result.add(handle);
  }
  return result;
 }
 private static List<String> strings(List<Long> handles){List<String> out=new ArrayList<>();for(long handle:handles)out.add(Long.toUnsignedString(handle));return out;}
 private void captureAtFault(){
  List<Control> own=new ArrayList<>();for(Control value:parent.getChildren())if(value!=foreign)own.add(value);
  need(own.size()==1&&own.get(0) instanceof WebCanvas<?>,"CAPTURE_EXACT_OWN_PARTIAL_WIDGET");
  captured=own.get(0);capturedHandle=captured.handle;
  need(capturedHandle!=0&&!captured.isDisposed()&&OS.GetParent(capturedHandle)==parentHandle&&OS.GetWindowThreadProcessId(capturedHandle,null)==OS.GetCurrentThreadId()&&nativeChildren(parentHandle).contains(capturedHandle)&&display.findWidget(capturedHandle)==captured,"CAPTURE_NATIVE_BIRTH_BEFORE_THROW");
  try{
   capturedTicker=(Runnable)field(captured,WebCanvas.class,"ticker");
   capturedAncestor=(Listener)field(captured,WebCanvas.class,"ancestorListener");
  }catch(ReflectiveOperationException error){throw new AssertionError("CAPTURE_INITIALIZED_OWNER_FIELDS",error);}
 }
 @Before public void setup(){
  org.junit.Assume.assumeTrue("Pinned Win32 SWT4974 independent native handle/private resource probe; other runtimes are unqualified.","win32".equals(SWT.getPlatform())&&SWT.getVersion()==4974);
  DeviceData data=new DeviceData();data.tracking=true;display=new FaultDisplay(data);display.owner=this;
  display.getSystemFont();display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
  shell=new Shell(display);parent=new Composite(shell,SWT.NONE);parent.setBounds(0,0,360,240);parentHandle=parent.handle;
  foreign=new Canvas(parent,SWT.NONE);foreign.setBounds(1,1,8,8);foreignHandle=foreign.handle;
  display.addFilter(SWT.Hide,foreignHide);display.addFilter(SWT.Iconify,foreignIconify);
  beforeResources=resources();
  need(parent.getChildren().length==1&&parent.getChildren()[0]==foreign&&nativeChildren(parentHandle).equals(List.of(foreignHandle)),"FOREIGN_NATIVE_PRECONDITION");
 }
 private void observeFailure(int failAt,boolean cleanupFailure)throws Exception{
  display.armed=true;display.failAt=failAt;display.cleanupFailure=cleanupFailure;Throwable actual=null;
  try{model=new Model();new WebCanvas<>(parent,SWT.NONE,model);}catch(RuntimeException|Error failure){actual=failure;}
  finally{display.armed=false;}
  need(actual==display.primary,"PRIMARY_FAILURE_IDENTITY");
  need(captured!=null&&capturedHandle!=0&&capturedAncestor==display.added.get(0),"CAPTURED_NATIVE_OWNER_REQUIRED");
  List<Integer> expectedRemovals=new ArrayList<>(display.addedTypes);if(cleanupFailure)expectedRemovals.add(SWT.Hide);
  int hideRemovals=Collections.frequency(display.removedTypes,SWT.Hide),iconifyRemovals=Collections.frequency(display.removedTypes,SWT.Iconify);
  System.out.println("CTOR_ROLLBACK_RETRY_TRACE failAt="+failAt+" cleanupFailure="+cleanupFailure+" added="+display.addedTypes+" removed="+display.removedTypes+" expected="+expectedRemovals+" perType="+hideRemovals+"/"+iconifyRemovals);
  need(display.attempts==failAt&&display.addedTypes.equals(failAt==1?List.of(SWT.Hide):List.of(SWT.Hide,SWT.Iconify))&&display.removedTypes.equals(expectedRemovals)&&display.removed.size()==expectedRemovals.size()&&hideRemovals<=2&&iconifyRemovals<=2&&display.removed.size()<=4,"EXACT_FILTER_REGISTRATION_AND_BOUNDED_RETRY_ORDER");
  for(Listener value:display.added)need(value==capturedAncestor,"REGISTERED_ADAPTER_IDENTITY");
  for(Listener value:display.removed)need(value==capturedAncestor,"REMOVED_ADAPTER_IDENTITY");
  need(actual.getSuppressed().length==(cleanupFailure?1:0),"SUPPRESSED_CLEANUP_COUNT");
  if(cleanupFailure)need(actual.getSuppressed()[0]==display.cleanup,"SUPPRESSED_CLEANUP_IDENTITY");
  Control[] children=parent.getChildren();int ownChildren=0;for(Control child:children)if(child!=foreign)ownChildren++;
  List<Long> nativeHandles=nativeChildren(parentHandle);int ownNative=0;for(long handle:nativeHandles)if(handle!=foreignHandle)ownNative++;
  int[] process=new int[1];int nativeThread=OS.GetWindowThreadProcessId(capturedHandle,process);
  boolean registered=display.findWidget(capturedHandle)==captured,foreignLive=!foreign.isDisposed()&&foreign.handle==foreignHandle&&OS.GetParent(foreignHandle)==parentHandle&&OS.GetWindowThreadProcessId(foreignHandle,null)==OS.GetCurrentThreadId()&&display.findWidget(foreignHandle)==foreign;
  need(foreignLive&&nativeHandles.contains(foreignHandle)&&countFilter(SWT.Hide,foreignHide)==1&&countFilter(SWT.Iconify,foreignIconify)==1,"FOREIGN_CONTROL_FILTER_LIFETIME_CHANGED");
  int ownHide=countFilter(SWT.Hide,capturedAncestor),ownIconify=countFilter(SWT.Iconify,capturedAncestor),ownQueued=queued(capturedTicker);
  Set<Object> extra=resources();extra.removeAll(beforeResources);
  Object cache=field(captured,WebCanvas.class,"cache");
  int cacheColors=(Integer)field(cache,"nc"),cacheFonts=(Integer)field(cache,"nf");
  model.resetReads();display.timerCalls=display.positiveTimers=0;int phase=model.snapshot()[8],generation=model.generation();Throwable pulseFailure=null;
  try{model.pulse();}catch(RuntimeException|Error failure){pulseFailure=failure;}
  Map<String,Object> row=new LinkedHashMap<>();
  row.put("case",cleanupFailure?"iconify-plus-cleanup-failure":failAt==1?"hide-registration-failure":"iconify-registration-failure");
  row.put("observationBeforeAnyTeardownOrPump",true);row.put("primaryIdentity",actual==display.primary);row.put("suppressedIdentity",!cleanupFailure||actual.getSuppressed()[0]==display.cleanup);
  row.put("capturedHWND",Long.toUnsignedString(capturedHandle));row.put("parentHWND",Long.toUnsignedString(parentHandle));row.put("foreignHWND",Long.toUnsignedString(foreignHandle));row.put("nativeChildHWNDs",List.copyOf(strings(nativeHandles)));
  row.put("ownPublicChildren",ownChildren);row.put("ownNativeChildren",ownNative);row.put("capturedNativeThread",nativeThread);row.put("capturedNativeProcess",process[0]);row.put("nativeParentMatches",OS.GetParent(capturedHandle)==parentHandle);row.put("capturedDisposed",captured.isDisposed());row.put("capturedJavaHandleZero",captured.handle==0);row.put("displayRegistryRetainsCaptured",registered);
  row.put("ownHideFilters",ownHide);row.put("ownIconifyFilters",ownIconify);row.put("queuedOwnTicker",ownQueued);row.put("trackedExtraGraphics",extra.size());row.put("cacheColors",cacheColors);row.put("cacheFonts",cacheFonts);row.put("foreignControlAndFiltersUnchanged",foreignLive);
  row.put("modelDeltaBindingNonNull",model.installed!=null);row.put("modelDeltaInstalls",model.deltaInstalls);row.put("postThrowPulseBlitReads",model.blitReads);row.put("postThrowPulseDamageReads",model.damageReads);row.put("postThrowPositiveTimerArms",display.positiveTimers);row.put("postThrowQueuedTicker",queued(capturedTicker));row.put("postThrowPhaseUnchanged",model.snapshot()[8]==phase);row.put("postThrowGenerationDelta",model.generation()-generation);row.put("postThrowPulseException",pulseFailure==null?"NONE":pulseFailure.getClass().getName());
  boolean target=ownChildren==0&&ownNative==0&&nativeThread==0&&captured.isDisposed()&&captured.handle==0&&!registered&&ownHide==0&&ownIconify==0&&ownQueued==0&&extra.isEmpty()&&model.blitReads==0&&model.damageReads==0&&display.positiveTimers==0&&queued(capturedTicker)==0&&pulseFailure==null;
  row.put("atomicLifetimeTarget",target);row.put("callbackDetachmentNotAsserted",true);
  Map<String,Object> frozen=Collections.unmodifiableMap(new LinkedHashMap<>(row));OBSERVATIONS.add(frozen);completedCases++;
  String trace=frozen.toString();need(trace.length()<=4096,"BOUNDED_PRETEARDOWN_JDK_TRACE");
  System.out.println("CTOR_FAILURE_PRETEARDOWN_TRACE "+trace);
  need(target,"FAILED_CTOR_ATOMIC_LIFETIME_TARGET ownPublic="+ownChildren+" ownNative="+ownNative+" registered="+registered+" disposed="+captured.isDisposed()+" pulseReads="+model.blitReads+"/"+model.damageReads);
 }
 @Test public void hideRegistrationFailureHasNoNativeSurvivor()throws Exception{observeFailure(1,false);}
 @Test public void iconifyRegistrationFailureHasNoNativeSurvivor()throws Exception{observeFailure(2,false);}
 @Test public void cleanupFailurePreservesPrimaryWithoutNativeSurvivor()throws Exception{observeFailure(2,true);}
 @After public void close()throws Exception{
  try{
   if(display!=null&&!display.isDisposed()){
    display.armed=false;
    if(captured!=null&&!captured.isDisposed())captured.dispose();
    if(capturedHandle!=0){
     need(OS.GetWindowThreadProcessId(capturedHandle,null)==0&&!nativeChildren(parentHandle).contains(capturedHandle)&&display.findWidget(capturedHandle)==null,"FAILED_WIDGET_NATIVE_CLEANUP_NOT_COMPLETE");
     need(queued(capturedTicker)==0&&countFilter(SWT.Hide,capturedAncestor)==0&&countFilter(SWT.Iconify,capturedAncestor)==0,"FAILED_WIDGET_CALLBACK_CLEANUP_NOT_COMPLETE");
     need(!foreign.isDisposed()&&OS.GetWindowThreadProcessId(foreignHandle,null)==OS.GetCurrentThreadId()&&display.findWidget(foreignHandle)==foreign&&countFilter(SWT.Hide,foreignHide)==1&&countFilter(SWT.Iconify,foreignIconify)==1,"FOREIGN_CHANGED_DURING_OWNED_CLEANUP");
    }
    if(parent!=null&&!parent.isDisposed())parent.dispose();
    if(shell!=null&&!shell.isDisposed())shell.dispose();
    Set<Object> extra=resources();extra.removeAll(beforeResources);
    for(Object value:extra){need(value instanceof Font,"UNCLASSIFIED_TRACKED_RESOURCE");Font font=(Font)value;displayOwnedFonts.put(font,requireDisplayOwnedFont(font));}
   }
  }finally{
   if(display!=null&&!display.isDisposed())display.dispose();
   need(display==null||display.isDisposed(),"OWNED_DISPLAY_RETAINED");
   for(Font font:displayOwnedFonts.keySet())need(font.isDisposed(),"DISPLAY_FONT_NOT_RELEASED");
   if(providerRegistries!=null)need(!providerRegistries.containsKey(display),"DISPLAY_FONT_REGISTRY_RETAINED");
  }
  closedCases++;
  System.out.println("CTOR_FAILURE_OWNED_CLEANUP nativeHWNDGone=true registryGone=true tickerGone=true foreignRetainedUntilOwnerTeardown=true displayDisposed=true");
 }
 public static Map<String,Object> observations(){
  return Map.of("schema","synexia.webcanvas.constructor-failure.preteardown-observations.v1","cases",new ArrayList<>(OBSERVATIONS),"completedCases",completedCases,"closedCases",closedCases,"sourceOwner","756141e76c3f7b19804443295bc50316f0c9e19f","applicationRepair","NOT_RUN","callbackDetachment","NOT_ASSERTED_SEPARATE_OWNERSHIP_CONTRACT");
 }
}
