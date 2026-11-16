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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/** Source-frozen native baseline: removal timing, API refusal and disposal reentry. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class WebCanvasFilterRetirementLifecycleTest {
 private FaultDisplay display; private Shell shell; private Composite parent; private Canvas foreign;
 private Model model; private ObservedCanvas captured;
 private long capturedHandle,parentHandle,foreignHandle; private Runnable capturedTicker; private Listener capturedAncestor;
 private Set<Object> beforeResources;
 private final IdentityHashMap<Font,Integer> displayOwnedFonts=new IdentityHashMap<>(); private Map<?,?> providerRegistries;
 private int foreignHideCalls,foreignIconifyCalls,disposeHookCalls,caseNumber;
 private String caseId; private boolean normalDispose,persistent; private int faultType; private String faultTiming="NONE",disposeMode="COUNT";
 private final RuntimeException primary=new RuntimeException("ADD");
 private final RuntimeException disposeFailure=new RuntimeException("DISPOSE");
 private final RuntimeException[] removalMarkers=new RuntimeException[4];
 private final IdentityHashMap<Throwable,String> exceptionIds=new IdentityHashMap<>();
 private final Listener foreignHide=event->foreignHideCalls++,foreignIconify=event->foreignIconifyCalls++;
 private static final List<Map<String,Object>> OBSERVATIONS=new ArrayList<>(); private static int completedCases,closedCases;
 private static void need(boolean b,String label){if(!b)throw new AssertionError(label);}
 private static Object field(Object owner,Class<?> type,String name)throws ReflectiveOperationException{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 private static Object field(Object owner,String name)throws ReflectiveOperationException{return field(owner,owner.getClass(),name);}
 private static final class FaultDisplay extends Display {
  WebCanvasFilterRetirementLifecycleTest owner; boolean armAdd,armRemove; int addAttempts,removeCalls,hideAttempts,iconifyAttempts,positiveTimers,timerCalls;
  final List<Throwable> removalThrows=new ArrayList<>(); final List<Map<String,Object>> removalTrace=new ArrayList<>();
  FaultDisplay(DeviceData data){super(data);}
  @Override protected void checkSubclass(){ }
  @Override public void addFilter(int type,Listener listener){
   super.addFilter(type,listener);
   if(armAdd&&(type==SWT.Hide||type==SWT.Iconify)&&++addAttempts==2){owner.captureAtFault();throw owner.primary;}
  }
  @Override public void removeFilter(int type,Listener listener){
   if(!armRemove||listener!=owner.capturedAncestor||(type!=SWT.Hide&&type!=SWT.Iconify)){super.removeFilter(type,listener);return;}
   int attempt=type==SWT.Hide?++hideAttempts:++iconifyAttempts;removeCalls++;
   need(attempt<=2&&removeCalls<=4,"UNBOUNDED_OWN_REMOVAL_ATTEMPTS");
   boolean fail=type==owner.faultType&&!owner.faultTiming.equals("NONE")&&(owner.persistent||removalThrows.isEmpty());
   if(fail&&owner.faultTiming.equals("BEFORE")){
    RuntimeException marker=owner.removalMarkers[removalThrows.size()];removalThrows.add(marker);
    removalTrace.add(Map.of("type",type,"attempt",attempt,"stage","BEFORE_THROW","marker",owner.exceptionIds.get(marker)));throw marker;
   }
   super.removeFilter(type,listener);
   if(fail){
    RuntimeException marker=owner.removalMarkers[removalThrows.size()];removalThrows.add(marker);
    removalTrace.add(Map.of("type",type,"attempt",attempt,"stage","AFTER_THROW","marker",owner.exceptionIds.get(marker)));throw marker;
   }
   removalTrace.add(Map.of("type",type,"attempt",attempt,"stage","RETURNED"));
  }
  @Override public void timerExec(int ms,Runnable callback){timerCalls++;if(ms>=0)positiveTimers++;super.timerExec(ms,callback);}
 }
 private static final class Model extends AnimvalueModel {
  WebDelta installed; int installs,pulses,blitReads,damageReads;
  Model(){super("filter-retirement-lifecycle",0,SPINNER,0,0);}
  @Override public void setDelta(WebDelta value){installed=value;installs++;super.setDelta(value);}
  @Override public int takeBlit(){blitReads++;return super.takeBlit();}
  @Override public boolean takeDamage(int[] out){damageReads++;return super.takeDamage(out);}
  void pulse(){pulses++;animate();} void resetReads(){blitReads=damageReads=0;}
 }
 private static class ObservedCanvas extends WebCanvas<Model> {
  int parentQueries,visibleQueries,shellQueries,sizeQueries;
  ObservedCanvas(Composite parent,Model model){super(parent,SWT.NONE,model);}
  @Override public Composite getParent(){parentQueries++;return super.getParent();}
  @Override public boolean isVisible(){visibleQueries++;return super.isVisible();}
  @Override public Shell getShell(){shellQueries++;return super.getShell();}
  @Override public Point getSize(){sizeQueries++;return super.getSize();}
  final void resetQueries(){parentQueries=visibleQueries=shellQueries=sizeQueries=0;}
  final void disposeThroughBase(){super.dispose();}
 }
 private static final class TrapCanvas extends ObservedCanvas {
  static int overrideCalls; static RuntimeException trapFailure;
  TrapCanvas(Composite parent,Model model){super(parent,model);}
  @Override public void dispose(){overrideCalls++;throw trapFailure;}
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

 private void captureAtFault(){captureSubject();}
 private void captureSubject(){
  List<Control> own=new ArrayList<>();for(Control child:parent.getChildren())if(child!=foreign)own.add(child);
  need(own.size()==1&&own.get(0) instanceof ObservedCanvas,"CAPTURE_EXACT_OWN_WIDGET");
  captured=(ObservedCanvas)own.get(0);capturedHandle=captured.handle;
  need(capturedHandle!=0&&OS.GetParent(capturedHandle)==parentHandle&&OS.GetWindowThreadProcessId(capturedHandle,null)==OS.GetCurrentThreadId()&&nativeChildren(parentHandle).contains(capturedHandle)&&display.findWidget(capturedHandle)==captured,"CAPTURE_EXACT_NATIVE_BIRTH");
  try{capturedTicker=(Runnable)field(captured,WebCanvas.class,"ticker");capturedAncestor=(Listener)field(captured,WebCanvas.class,"ancestorListener");}
  catch(ReflectiveOperationException failure){throw new AssertionError("CAPTURE_OWNER_FIELDS",failure);}
  captured.addListener(SWT.Dispose,event->{
   disposeHookCalls++;
   if(disposeMode.equals("DISTINCT"))throw disposeFailure;
   if(disposeMode.equals("REENTER"))captured.dispose();
   if(disposeMode.equals("ORIGINAL"))throw primary;
  });
 }
 @Before public void setup(){
  org.junit.Assume.assumeTrue("Pinned Win32 SWT4974 native ownership and private state; other runtimes unqualified.","win32".equals(SWT.getPlatform())&&SWT.getVersion()==4974);
  DeviceData data=new DeviceData();data.tracking=true;display=new FaultDisplay(data);display.owner=this;
  display.setRuntimeExceptionHandler(error->{throw error;});display.setErrorHandler(error->{throw error;});
  display.getSystemFont();display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
  shell=new Shell(display);parent=new Composite(shell,SWT.NONE);parent.setBounds(0,0,360,240);parentHandle=parent.handle;
  foreign=new Canvas(parent,SWT.NONE);foreign.setBounds(1,1,8,8);foreignHandle=foreign.handle;
  display.addFilter(SWT.Hide,foreignHide);display.addFilter(SWT.Iconify,foreignIconify);beforeResources=resources();
  exceptionIds.put(primary,"ADD");exceptionIds.put(disposeFailure,"D");
  for(int i=0;i<removalMarkers.length;i++){removalMarkers[i]=new RuntimeException("REMOVE_"+(i+1));exceptionIds.put(removalMarkers[i],"R"+(i+1));}
  TrapCanvas.overrideCalls=0;TrapCanvas.trapFailure=new RuntimeException("TRAP");exceptionIds.put(TrapCanvas.trapFailure,"TRAP");
  need(parent.getChildren().length==1&&parent.getChildren()[0]==foreign&&nativeChildren(parentHandle).equals(List.of(foreignHandle)),"FOREIGN_NATIVE_PRECONDITION");
 }
 private List<Map<String,Object>> exceptionGraph(Throwable actual){
  Throwable expectedRoot=normalDispose?display.removalThrows.get(0):primary;need(actual==expectedRoot,"EXACT_ORIGINAL_PRIMARY_IDENTITY");
  Set<Throwable> expected=Collections.newSetFromMap(new IdentityHashMap<>());expected.add(expectedRoot);expected.addAll(display.removalThrows);if(disposeMode.equals("DISTINCT"))expected.add(disposeFailure);
  Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());Deque<Throwable> pending=new ArrayDeque<>();pending.add(actual);List<Map<String,Object>> rows=new ArrayList<>();
  while(!pending.isEmpty()){
   Throwable item=pending.removeFirst();need(seen.size()<8&&seen.add(item)&&exceptionIds.containsKey(item),"UNKNOWN_DUPLICATE_OR_CYCLIC_FAILURE_IDENTITY");
   need(item.getCause()==null,"UNEXPECTED_FAILURE_CAUSE");List<String> children=new ArrayList<>();
   for(Throwable child:item.getSuppressed()){need(exceptionIds.containsKey(child),"UNKNOWN_SUPPRESSED_IDENTITY");children.add(exceptionIds.get(child));pending.addLast(child);}
   rows.add(Map.of("id",exceptionIds.get(item),"suppressed",List.copyOf(children)));
  }
  need(seen.equals(expected),"LOST_OR_EXTRA_CLEANUP_FAILURE_IDENTITY");
  if(!normalDispose&&!display.removalThrows.isEmpty())need(actual.getSuppressed().length>0&&actual.getSuppressed()[0]==display.removalThrows.get(0),"FIRST_REMOVAL_CLEANUP_IDENTITY_ORDER");
  return List.copyOf(rows);
 }
 private void configure(int number){
  caseNumber=number;faultType=SWT.Hide;faultTiming="BEFORE";normalDispose=false;persistent=false;disposeMode="COUNT";
  switch(number){
   case 1:caseId="ctor_before_hide_once";break;
   case 2:caseId="ctor_before_iconify_once";faultType=SWT.Iconify;break;
   case 3:caseId="normal_dispose_before_hide_once";normalDispose=true;break;
   case 4:caseId="ctor_after_hide_once";faultTiming="AFTER";break;
   case 5:caseId="ctor_before_hide_persistent";persistent=true;break;
   case 6:caseId="ctor_dispose_listener_throws_distinct";faultTiming="NONE";disposeMode="DISTINCT";break;
   case 7:caseId="ctor_dispose_listener_reenters";faultTiming="NONE";disposeMode="REENTER";break;
   case 8:caseId="ctor_public_dispose_override_trap";faultTiming="NONE";break;
   case 9:caseId="ctor_dispose_listener_throws_original";faultTiming="NONE";disposeMode="ORIGINAL";break;
   default:throw new AssertionError("UNKNOWN_CASE");
  }
 }
 private void runCase(int number)throws Exception{
  configure(number);model=new Model();display.armRemove=true;Throwable actual=null;
  try{
   if(normalDispose){new ObservedCanvas(parent,model);captureSubject();captured.dispose();}
   else{display.armAdd=true;if(number==8)new TrapCanvas(parent,model);else new ObservedCanvas(parent,model);}
  }catch(RuntimeException|Error failure){actual=failure;}finally{display.armAdd=false;}
  need(captured!=null&&capturedHandle!=0&&actual!=null,"CAPTURE_AND_VISIBLE_FAILURE_REQUIRED");
  List<Map<String,Object>> graph=exceptionGraph(actual);
  int ownHide=countFilter(SWT.Hide,capturedAncestor),ownIconify=countFilter(SWT.Iconify,capturedAncestor),mask=(Integer)field(captured,WebCanvas.class,"clockFilters");
  List<Long> natives=nativeChildren(parentHandle);int ownNative=0;for(long handle:natives)if(handle!=foreignHandle)ownNative++;
  int ownChildren=0;for(Control child:parent.getChildren())if(child!=foreign)ownChildren++;
  boolean registry=display.findWidget(capturedHandle)==captured;int nativeThread=OS.GetWindowThreadProcessId(capturedHandle,null);
  Set<Object> extra=resources();extra.removeAll(beforeResources);
  boolean foreignLive=!foreign.isDisposed()&&foreign.handle==foreignHandle&&OS.GetParent(foreignHandle)==parentHandle&&OS.GetWindowThreadProcessId(foreignHandle,null)==OS.GetCurrentThreadId()&&display.findWidget(foreignHandle)==foreign;
  need(foreignLive&&natives.contains(foreignHandle)&&countFilter(SWT.Hide,foreignHide)==1&&countFilter(SWT.Iconify,foreignIconify)==1,"FOREIGN_IDENTITY_OR_FILTER_CHANGED");
  int lifetimeTimerCalls=display.timerCalls,lifetimePositive=display.positiveTimers;
  captured.resetQueries();model.resetReads();display.timerCalls=display.positiveTimers=0;foreignHideCalls=foreignIconifyCalls=0;int phase=model.snapshot()[8],generation=model.generation();
  Throwable postFailure=null;try{model.pulse();foreign.setVisible(false);foreign.setVisible(true);}catch(RuntimeException|Error failure){postFailure=failure;}
  boolean cpuInert=model.blitReads==0&&model.damageReads==0&&captured.parentQueries==0&&captured.visibleQueries==0&&captured.shellQueries==0&&captured.sizeQueries==0&&display.timerCalls==0&&queued(capturedTicker)==0&&model.snapshot()[8]==phase&&postFailure==null;
  boolean nativeGone=ownNative==0&&ownChildren==0&&nativeThread==0&&!registry&&captured.isDisposed()&&captured.handle==0;
  boolean common=nativeGone&&cpuInert&&extra.isEmpty()&&disposeHookCalls==1&&TrapCanvas.overrideCalls==0&&foreignHideCalls==1&&foreignIconifyCalls==0&&lifetimePositive==0&&display.hideAttempts<=2&&display.iconifyAttempts<=2&&display.removeCalls<=4;
  boolean absence=ownHide==0&&ownIconify==0;
  boolean target=common&&(persistent?ownHide==1&&ownIconify==0:absence);
  String verdict=persistent?"API_REFUSAL_BOUNDARY":absence?"OWNED_FILTER_ABSENCE":"ONE_SHOT_RETIREMENT_TARGET_FAIL";
  Map<String,Object> row=new LinkedHashMap<>();row.put("case",caseId);row.put("number",caseNumber);row.put("beforeTeardownOrPump",true);row.put("normalDispose",normalDispose);row.put("removalTiming",faultTiming);row.put("persistentRefusal",persistent);
  row.put("primaryId",exceptionIds.get(actual));row.put("exceptionGraph",graph);row.put("removeTrace",List.copyOf(display.removalTrace));row.put("hideAttempts",display.hideAttempts);row.put("iconifyAttempts",display.iconifyAttempts);
  row.put("ownedHideFilters",ownHide);row.put("ownedIconifyFilters",ownIconify);row.put("clockMask",mask);row.put("retainedWithoutOwnershipBit",ownHide>0&&(mask&1)==0||ownIconify>0&&(mask&2)==0);row.put("filterAbsence",absence);row.put("verdict",verdict);
  row.put("capturedHWND",Long.toUnsignedString(capturedHandle));row.put("nativeChildHWNDs",List.copyOf(strings(natives)));row.put("ownNativeChildren",ownNative);row.put("ownPublicChildren",ownChildren);row.put("nativeThread",nativeThread);row.put("registryRetains",registry);row.put("capturedDisposed",captured.isDisposed());row.put("javaHandleZero",captured.handle==0);row.put("nativeGone",nativeGone);row.put("trackedExtraGraphics",extra.size());row.put("foreignControlAndFiltersUnchanged",foreignLive);
  row.put("disposeHookCalls",disposeHookCalls);row.put("publicDisposeOverrideCalls",TrapCanvas.overrideCalls);row.put("lifetimeTimerCalls",lifetimeTimerCalls);row.put("lifetimePositiveTimerArms",lifetimePositive);
  row.put("postBlitReads",model.blitReads);row.put("postDamageReads",model.damageReads);row.put("postParentQueries",captured.parentQueries);row.put("postVisibleQueries",captured.visibleQueries);row.put("postShellQueries",captured.shellQueries);row.put("postSizeQueries",captured.sizeQueries);row.put("postTimerCalls",display.timerCalls);row.put("queuedTicker",queued(capturedTicker));row.put("foreignHideCalls",foreignHideCalls);row.put("postPhaseUnchanged",model.snapshot()[8]==phase);row.put("postGenerationDelta",model.generation()-generation);row.put("postException",postFailure==null?"NONE":postFailure.getClass().getName());row.put("modelBindingNonNull",model.installed!=null);row.put("cpuInert",cpuInert);row.put("target",target);row.put("persistentNotGlobalAbsenceSuccess",persistent);
  Map<String,Object> frozen=Collections.unmodifiableMap(new LinkedHashMap<>(row));OBSERVATIONS.add(frozen);completedCases++;
  String trace=caseId+" target="+target+" verdict="+verdict+" filters="+ownHide+"/"+ownIconify+" mask="+mask+" nativeGone="+nativeGone+" cpuInert="+cpuInert+" primary="+exceptionIds.get(actual);need(trace.length()<=384,"BOUNDED_PRETEARDOWN_TRACE");
  System.out.println("FILTER_RETIREMENT_PRETEARDOWN "+trace);
  need(target,"FILTER_RETIREMENT_TARGET case="+caseId+" filters="+ownHide+"/"+ownIconify+" nativeGone="+nativeGone+" cpuInert="+cpuInert);
 }
 @Test public void test01CtorBeforeHideOnce()throws Exception{runCase(1);}
 @Test public void test02CtorBeforeIconifyOnce()throws Exception{runCase(2);}
 @Test public void test03NormalDisposeBeforeHideOnce()throws Exception{runCase(3);}
 @Test public void test04CtorAfterHideControl()throws Exception{runCase(4);}
 @Test public void test05PersistentAPIRefusalBoundary()throws Exception{runCase(5);}
 @Test public void test06ThrowingDisposeListener()throws Exception{runCase(6);}
 @Test public void test07ReentrantDisposeListener()throws Exception{runCase(7);}
 @Test public void test08PublicDisposeOverrideTrap()throws Exception{runCase(8);}
 @Test public void test09DisposeThrowsOriginalPrimary()throws Exception{runCase(9);}
 @After public void close()throws Exception{
  try{
   if(display!=null&&!display.isDisposed()){
    display.armAdd=false;display.armRemove=false;
    if(capturedAncestor!=null){display.removeFilter(SWT.Hide,capturedAncestor);display.removeFilter(SWT.Iconify,capturedAncestor);}
    if(captured!=null&&!captured.isDisposed())captured.disposeThroughBase();
    if(capturedHandle!=0){
     need(OS.GetWindowThreadProcessId(capturedHandle,null)==0&&!nativeChildren(parentHandle).contains(capturedHandle)&&display.findWidget(capturedHandle)==null,"OWNED_NATIVE_CLEANUP_INCOMPLETE");
     need(queued(capturedTicker)==0&&countFilter(SWT.Hide,capturedAncestor)==0&&countFilter(SWT.Iconify,capturedAncestor)==0,"OWNED_FILTER_TICKER_CLEANUP_INCOMPLETE");
     need(!foreign.isDisposed()&&OS.GetWindowThreadProcessId(foreignHandle,null)==OS.GetCurrentThreadId()&&display.findWidget(foreignHandle)==foreign&&countFilter(SWT.Hide,foreignHide)==1&&countFilter(SWT.Iconify,foreignIconify)==1,"FOREIGN_CHANGED_DURING_OWNED_CLEANUP");
    }
    if(parent!=null&&!parent.isDisposed())parent.dispose();if(shell!=null&&!shell.isDisposed())shell.dispose();
    Set<Object> extra=resources();extra.removeAll(beforeResources);for(Object value:extra){need(value instanceof Font,"UNCLASSIFIED_TRACKED_RESOURCE");Font font=(Font)value;displayOwnedFonts.put(font,requireDisplayOwnedFont(font));}
   }
  }finally{
   if(display!=null&&!display.isDisposed())display.dispose();need(display==null||display.isDisposed(),"OWNED_DISPLAY_RETAINED");
   for(Font font:displayOwnedFonts.keySet())need(font.isDisposed(),"DISPLAY_FONT_NOT_RELEASED");
   if(providerRegistries!=null)need(!providerRegistries.containsKey(display),"DISPLAY_FONT_REGISTRY_RETAINED");
  }
  closedCases++;System.out.println("FILTER_RETIREMENT_OWNED_CLEANUP exactAdaptersGone=true nativeAndRegistryGone=true displayDisposed=true");
 }
 public static Map<String,Object> observations(){return Map.of("schema","synexia.webcanvas.filter-retirement.observations.v1","cases",new ArrayList<>(OBSERVATIONS),"completedCases",completedCases,"closedCases",closedCases,"sourceBindingAtBirth","f5a04cb42c418b72e3cb3ca8dbd5f2276bbeb470","productionSourceChanges",0,"persistentRefusalIsNotGlobalAbsence",true);}
}
