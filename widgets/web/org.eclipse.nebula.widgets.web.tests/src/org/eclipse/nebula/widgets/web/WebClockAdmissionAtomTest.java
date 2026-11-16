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
import org.eclipse.nebula.widgets.web.model.AnimvalueModel;
import org.eclipse.nebula.widgets.web.model.WebPaint;
import org.eclipse.nebula.widgets.web.model.WebStates;
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Focused native clock admission/identity probes; no opened-window or screen claim. */
public final class WebClockAdmissionAtomTest {
 private FilterDisplay display; private Shell shell; private Composite parent;
 private Model model; private CanvasProbe widget; private Set<Object> beforeResources;
 private final IdentityHashMap<Font,Integer> displayOwnedFonts=new IdentityHashMap<>();
 private Map<?,?> providerRegistries;
 private final Listener foreignHide=event->{},foreignIconify=event->{};
 private static void need(boolean b,String label){if(!b)throw new AssertionError(label);}
 private static Object field(Object owner,Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 private static Object field(Object owner,String name)throws Exception{return field(owner,owner.getClass(),name);}
 private static final class FilterDisplay extends Display {
  boolean armed,cleanupFailure; int failAt,attempts,timerCalls,positiveTimers;
  Composite failureParent; Control constructorChild; long constructorHWND;
  final RuntimeException primary=new RuntimeException("controlled addFilter failure");
  final RuntimeException cleanup=new RuntimeException("controlled removeFilter failure");
  final List<Listener> added=new ArrayList<>(),removed=new ArrayList<>();
  final List<Integer> addedTypes=new ArrayList<>(),removedTypes=new ArrayList<>();
  FilterDisplay(DeviceData data){super(data);}
  @Override protected void checkSubclass(){ }
  @Override public void addFilter(int type,Listener listener){
   super.addFilter(type,listener);
   if(armed&&(type==SWT.Hide||type==SWT.Iconify)){
    added.add(listener);addedTypes.add(type);
    if(++attempts==failAt){
     Control[] children=failureParent.getChildren();
     need(children.length==1&&children[0] instanceof WebCanvas<?>,"FAILED_CONSTRUCTOR_CAPTURE_PRECONDITION");
     constructorChild=children[0];constructorHWND=constructorChild.handle;
     need(constructorHWND!=0&&OS.GetWindowThreadProcessId(constructorHWND,null)==OS.GetCurrentThreadId()&&OS.GetParent(constructorHWND)==failureParent.handle&&findWidget(constructorHWND)==constructorChild,"FAILED_CONSTRUCTOR_NATIVE_BIRTH_CAPTURE");
     throw primary;
    }
   }
  }
  @Override public void removeFilter(int type,Listener listener){
   super.removeFilter(type,listener);
   if(armed&&(type==SWT.Hide||type==SWT.Iconify)){removed.add(listener);removedTypes.add(type);if(cleanupFailure&&type==SWT.Hide){cleanupFailure=false;throw cleanup;}}
  }
  @Override public void timerExec(int ms,Runnable callback){timerCalls++;if(ms>=0)positiveTimers++;super.timerExec(ms,callback);}
  void resetTimers(){timerCalls=positiveTimers=0;}
 }
 private static final class Model extends AnimvalueModel {
  final List<String> order=new ArrayList<>();
  int blitReads,damageReads,damageHits,emits,pulses,loads,advances; RuntimeException layoutFailure; Runnable animationQueryHook; boolean forcedAnimating; int queryActions;
  Model(){super("clock-admission-atom",0,SPINNER,0,0);}
  @Override public boolean isAnimating(){Runnable hook=animationQueryHook;if(hook!=null){animationQueryHook=null;queryActions++;hook.run();return true;}return forcedAnimating||super.isAnimating();}
  @Override public void load(String sample){loads++;super.load(sample);}
  @Override protected boolean advance(int ms){advances++;return super.advance(ms);}
  @Override public void layout(int w,int h){order.add("L:"+w+":"+h);if(layoutFailure!=null)throw layoutFailure;super.layout(w,h);}
  @Override public int takeBlit(){blitReads++;return super.takeBlit();}
  @Override public boolean takeDamage(int[] out){damageReads++;boolean hit=super.takeDamage(out);if(hit)damageHits++;return hit;}
  @Override protected void emit(WebPaint paint){emits++;super.emit(paint);}
  void pulse(){pulses++;animate();}
  void resetCost(){blitReads=damageReads=damageHits=emits=pulses=0;}
 }
 private static final class CanvasProbe extends WebCanvas<Model> {
  int visibleQueries,shellQueries,sizeQueries; boolean scripted;
  Point first,second; RuntimeException firstFailure,secondFailure;
  CanvasProbe(Composite parent,Model model){super(parent,SWT.NONE,model);}
  @Override public boolean isVisible(){visibleQueries++;return super.isVisible();}
  @Override public Shell getShell(){shellQueries++;return super.getShell();}
  @Override public Point getSize(){
   int index=++sizeQueries;
   if(scripted){model.order.add("Q"+index);if(index==1&&firstFailure!=null)throw firstFailure;if(index==2&&secondFailure!=null)throw secondFailure;need(index<=2,"EXCESS_SIZE_QUERY");return index==1?first:second;}
   return super.getSize();
  }
  void resetQueries(){visibleQueries=shellQueries=sizeQueries=0;}
 }
 private Set<Object> resources(){
  Set<Object> result=Collections.newSetFromMap(new IdentityHashMap<>());Object[] objects=display.getDeviceData().objects;
  if(objects!=null)for(Object object:objects)if(object instanceof Resource resource&&!resource.isDisposed()&&!(object instanceof Color))result.add(object);
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
 private int countFilter(int type,Listener listener)throws Exception{int n=0;for(Listener entry:filters(type))if(entry==listener)n++;return n;}
 private Runnable ticker()throws Exception{return (Runnable)field(widget,WebCanvas.class,"ticker");}
 private int queued(Runnable own)throws Exception{Runnable[] list=(Runnable[])field(display,Display.class,"timerList");int count=0;if(list!=null)for(Runnable value:list)if(value==own)count++;return count;}
 private void createWidget(){model=new Model();widget=new CanvasProbe(parent,model);widget.setBounds(0,0,360,240);}
 private void drainDamage(){int[] out=new int[4];while(model.takeDamage(out)){}model.takeBlit();}
 @Before public void setup(){
  org.junit.Assume.assumeTrue("Pinned Win32 SWT4974 native clock/private-resource introspection; other runtimes are not qualified.", "win32".equals(SWT.getPlatform()) && SWT.getVersion() == 4974);
  DeviceData data=new DeviceData();data.tracking=true;display=new FilterDisplay(data);display.getSystemFont();display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
  shell=new Shell(display);parent=new Composite(shell,SWT.NONE);parent.setBounds(0,0,360,240);display.failureParent=parent;beforeResources=resources();
  display.addFilter(SWT.Hide,foreignHide);display.addFilter(SWT.Iconify,foreignIconify);
 }
 @After public void close()throws Exception{
  try{
   if(display!=null&&!display.isDisposed()){
    display.armed=false;
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
 }
 private Map<String,Integer> prepareBlocked()throws Exception{
  createWidget();Map<String,Integer> construction=Map.of("isVisible",widget.visibleQueries,"getShell",widget.shellQueries,"getSize",widget.sizeQueries);
  need(!shell.isVisible()&&!widget.isVisible(),"ACTUAL_UNOPENED_NATIVE_HOST_REQUIRED");
  widget.setPartVisible(false);model.load("spinning");need(model.isAnimating(),"REAL_SPINNER_ANIMATION_REQUIRED");drainDamage();
  widget.resetQueries();model.resetCost();display.resetTimers();widget.setPartVisible(true);
  need(widget.visibleQueries==1&&widget.shellQueries==0&&widget.sizeQueries==0,"FIRST_NATIVE_DENIAL_QUERY_COUNTS");
  need(model.blitReads==1&&model.damageReads==1&&model.damageHits==0&&model.emits==0&&display.positiveTimers==0&&queued(ticker())==0,"FIRST_DENIAL_NO_DAMAGE_PAINT_OR_TICKER");
  return construction;
 }
 @Test public void blockedNativeQueriesAreCached()throws Exception{
  Map<String,Integer> construction=prepareBlocked();int generation=model.generation(),phase=model.snapshot()[8];
  widget.resetQueries();model.resetCost();display.resetTimers();
  for(int i=0;i<256;i++)model.pulse();
  need(widget.visibleQueries==0&&widget.shellQueries==0&&widget.sizeQueries==0,"BLOCKED_ADDITIONAL_NATIVE_QUERY");
  need(model.pulses==256&&model.blitReads==256&&model.damageReads==256&&model.damageHits==0&&model.emits==0&&model.generation()==generation+256&&model.snapshot()[8]==phase&&display.timerCalls==0&&queued(ticker())==0,"BLOCKED_DAMAGE_FREE_INPUT_LAWS");
  System.out.println("CLOCK_BLOCKED_QUERY_COUNTS constructionAndResize="+construction+" firstProbe={isVisible=1,getShell=0,getSize=0} repeated256={isVisible=0,getShell=0,getSize=0} phaseUnchanged=true timers=0");
 }
 @Test public void exactListenerAndFilterIdentitiesDetach()throws Exception{
  createWidget();int[] events=(int[])field(null,WebCanvas.class,"EVENTS");Set<Listener> identities=Collections.newSetFromMap(new IdentityHashMap<>());
  for(int type:events){Listener[] listeners=widget.getListeners(type);need(listeners.length==1,"OWN_LISTENER_REGISTRATION_COUNT");identities.add(listeners[0]);}
  need(events.length==14&&identities.size()==1,"ONE_OWN_LISTENER_IDENTITY");
  Listener own=(Listener)field(widget,WebCanvas.class,"ownListener"),ancestor=(Listener)field(widget,WebCanvas.class,"ancestorListener");need(identities.contains(own)&&own!=ancestor,"OWN_AND_ANCESTOR_IDENTITIES_DISTINCT");
  need(countFilter(SWT.Hide,ancestor)==1&&countFilter(SWT.Iconify,ancestor)==1&&countFilter(SWT.Hide,foreignHide)==1&&countFilter(SWT.Iconify,foreignIconify)==1,"EXACT_OWN_AND_FOREIGN_FILTERS");
  Runnable callback=ticker();widget.dispose();
  need(countFilter(SWT.Hide,ancestor)==0&&countFilter(SWT.Iconify,ancestor)==0&&countFilter(SWT.Hide,foreignHide)==1&&countFilter(SWT.Iconify,foreignIconify)==1&&queued(callback)==0,"EXACT_FILTER_DETACH_FOREIGN_RETAINED");
  need(field(widget,Widget.class,"eventTable")==null,"DISPOSED_OWN_LISTENER_TABLE_RETAINED");
 }
 private void registrationFailure(int failAt,boolean cleanupFailure)throws Exception{
  display.armed=true;display.failAt=failAt;display.cleanupFailure=cleanupFailure;Throwable actual=null;
  try{model=new Model();widget=new CanvasProbe(parent,model);}catch(RuntimeException|Error failure){actual=failure;}
  finally{display.armed=false;}
  need(actual==display.primary,"ADD_FILTER_PRIMARY_IDENTITY");
  need(display.attempts==failAt&&display.added.size()==failAt,"EXACT_PARTIAL_REGISTRATION_ATTEMPTS");
  Listener ancestor=display.added.get(0);for(Listener entry:display.added)need(entry==ancestor,"PARTIAL_REGISTRATION_SAME_ADAPTER");
  need(countFilter(SWT.Hide,ancestor)==0&&countFilter(SWT.Iconify,ancestor)==0&&countFilter(SWT.Hide,foreignHide)==1&&countFilter(SWT.Iconify,foreignIconify)==1,"PARTIAL_ROLLBACK_EXACT_FILTERS");
  List<Integer> expectedRemovals=new ArrayList<>(display.addedTypes);if(cleanupFailure)expectedRemovals.add(SWT.Hide);
  int hideRemovals=Collections.frequency(display.removedTypes,SWT.Hide),iconifyRemovals=Collections.frequency(display.removedTypes,SWT.Iconify);
  System.out.println("CLOCK_ROLLBACK_RETRY_TRACE failAt="+failAt+" cleanupFailure="+cleanupFailure+" added="+display.addedTypes+" removed="+display.removedTypes+" expected="+expectedRemovals+" perType="+hideRemovals+"/"+iconifyRemovals);
  need(display.removed.size()==expectedRemovals.size()&&display.removedTypes.size()==expectedRemovals.size()&&hideRemovals<=2&&iconifyRemovals<=2&&display.removed.size()<=4,"BOUNDED_ROLLBACK_REMOVE_COUNT");
  need(display.addedTypes.equals(failAt==1?List.of(SWT.Hide):List.of(SWT.Hide,SWT.Iconify))&&display.removedTypes.equals(expectedRemovals),"EXACT_FILTER_FIRST_PASS_AND_RETRY_ORDER");for(Listener entry:display.removed)need(entry==ancestor,"ROLLBACK_WRONG_ADAPTER");
  if(cleanupFailure)need(actual.getSuppressed().length==1&&actual.getSuppressed()[0]==display.cleanup,"SUPPRESSED_CLEANUP_IDENTITY");else need(actual.getSuppressed().length==0,"UNEXPECTED_SUPPRESSED_FAILURE");
  need(display.constructorChild!=null&&display.constructorHWND!=0,"FAILED_CONSTRUCTOR_CAPTURE_REQUIRED");
  need(parent.getChildren().length==0&&OS.GetWindow(parent.handle,OS.GW_CHILD)==0&&display.findWidget(display.constructorHWND)==null&&OS.GetWindowThreadProcessId(display.constructorHWND,null)==0&&display.constructorChild.isDisposed()&&display.constructorChild.handle==0,"FAILED_CONSTRUCTOR_NATIVE_CHILD_RETAINED");
 }
 @Test public void partialHideRegistrationRollsBack()throws Exception{registrationFailure(1,false);}
 @Test public void partialIconifyRegistrationRollsBack()throws Exception{registrationFailure(2,false);}
 @Test public void rollbackPreservesPrimaryAndSuppressedCleanup()throws Exception{registrationFailure(2,true);}
 private void sizeCase(int mode)throws Exception{
  createWidget();widget.scripted=true;widget.resetQueries();model.order.clear();widget.first=new Point(17,99);widget.second=new Point(88,37);
  RuntimeException marker=new RuntimeException("controlled getSize failure");
  if(mode==1)widget.first=null;if(mode==2)widget.second=null;if(mode==3)widget.firstFailure=marker;if(mode==4)widget.secondFailure=marker;if(mode==5)model.layoutFailure=marker;
  Throwable actual=null;try{widget.notifyListeners(SWT.Resize,new Event());}catch(RuntimeException|Error failure){actual=failure;}finally{widget.scripted=false;model.layoutFailure=null;}
  int calls=mode==1||mode==3?1:2;need(widget.sizeQueries==calls,"ORIGINAL_SIZE_CALL_COUNT");
  if(mode==5)need(actual==marker&&model.order.equals(List.of("Q1","Q2","L:17:37"))&&widget.visibleQueries==0&&widget.shellQueries==0,"LAYOUT_FAILURE_IDENTITY_ORDER_NO_POST_SUCCESS_WAKE");
  else if(mode==0)need(actual==null&&model.order.equals(List.of("Q1","Q2","L:17:37")),"FIRST_X_SECOND_Y_LAYOUT_ORDER");
  else{need(model.order.equals(calls==1?List.of("Q1"):List.of("Q1","Q2")),"FAILURE_MUST_PRECEDE_LAYOUT");if(mode>=3)need(actual==marker,"SIZE_THROW_IDENTITY");else need(actual instanceof NullPointerException,"NULL_SIZE_EXCEPTION_TYPE");}
 }
 @Test public void resizeTakesFirstXAndSecondY()throws Exception{sizeCase(0);}
 @Test public void firstSizeNullStopsSecondAndLayout()throws Exception{sizeCase(1);}
 @Test public void secondSizeNullStopsLayout()throws Exception{sizeCase(2);}
 @Test public void firstSizeThrowIdentity()throws Exception{sizeCase(3);}
 @Test public void secondSizeThrowIdentity()throws Exception{sizeCase(4);}
 @Test public void layoutFailureKeepsTwoSizeQueriesAndSkipsClockWake()throws Exception{sizeCase(5);}

 private void pump(int ms)throws Exception{long end=System.nanoTime()+ms*1_000_000L;while(System.nanoTime()<end){while(display.readAndDispatch()){}Thread.sleep(1);}}
 @Test public void loadBeforeFirstNativeShellOpenResumesWithoutAnotherMutation()throws Exception{
  createWidget();var trim=shell.computeTrim(0,0,360,240);shell.setSize(trim.width,trim.height);
  model.load("spinning");need(model.loads==1&&model.isAnimating()&&!shell.isVisible()&&!widget.isVisible(),"LOAD_BEFORE_FIRST_OPEN_REQUIRED");
  int phase=model.snapshot()[8],beforeQueue=queued(ticker());pump(80);
  System.out.println("CLOCK_FIRST_OPEN_UNOPENED_OBSERVATION advances="+model.advances+" queued="+queued(ticker())+" phaseUnchanged="+(model.snapshot()[8]==phase));
  need(model.advances==0&&model.snapshot()[8]==phase&&queued(ticker())==0,"UNOPENED_AUTOMATIC_CLOCK_BEFORE_FIRST_OPEN");
  shell.open();pump(160);
  need(shell.isVisible()&&widget.isVisible()&&!shell.getMinimized()&&model.loads==1&&model.pulses==0&&model.advances>0&&model.snapshot()[8]!=phase&&queued(ticker())==1,"FIRST_NATIVE_OPEN_DID_NOT_RESUME_WITHOUT_MODEL_MUTATION");
  System.out.println("CLOCK_FIRST_NATIVE_OPEN loadedBeforeOpen=true loadCalls=1 manualPulses=0 queuedBeforeOpen="+beforeQueue+" actualModelAdvances="+model.advances+" queuedAfterOpen=1 noExtraModelMutation=true");
 }

 private void wakeQueryCase(int mode)throws Exception{
  createWidget();var trim=shell.computeTrim(0,0,360,240);shell.setSize(trim.width,trim.height);shell.open();pump(40);
  need(widget.isVisible()&&!shell.getMinimized()&&queued(ticker())==0&&!model.isAnimating(),"ELIGIBLE_IDLE_WAKE_HOST_REQUIRED");
  Composite hidden=null;if(mode==2){hidden=new Composite(shell,SWT.NONE);hidden.setBounds(0,0,360,240);hidden.setVisible(false);}
  Composite destination=hidden;Runnable callback=ticker();
  if(mode==3)model.forcedAnimating=true;
  model.animationQueryHook=()->{if(mode==0)widget.dispose();else if(mode==1)display.dispose();else if(mode==2)need(widget.setParent(destination),"QUERY_REPARENT_REFUSED");else model.pulse();};
  display.resetTimers();Throwable actual=null;
  try{widget.notifyListeners(SWT.Resize,new Event());}catch(RuntimeException|Error failure){actual=failure;}
  int ownQueued=queued(callback);
  System.out.println("CLOCK_WAKE_QUERY_OBSERVATION mode="+mode+" queryActions="+model.queryActions+" exception="+(actual==null?"NONE":actual.getClass().getName())+" timerArms="+display.positiveTimers+" queued="+ownQueued);
  need(model.queryActions==1,"WAKE_QUERY_CALLBACK_NOT_REACHED");
  need(actual==null,"WAKE_QUERY_LIFETIME_EXCEPTION");
  if(mode==0)need(widget.isDisposed()&&ownQueued==0&&display.positiveTimers==0,"WAKE_QUERY_DISPOSED_WIDGET_TICKER");
  else if(mode==1)need(display.isDisposed()&&widget.isDisposed()&&ownQueued==0&&display.positiveTimers==0,"WAKE_QUERY_DISPOSED_DISPLAY_TICKER");
  else if(mode==2)need(widget.getParent()==destination&&!widget.isVisible()&&ownQueued==0&&display.positiveTimers==0,"WAKE_QUERY_HIDDEN_REPARENT_TICKER");
  else need(ownQueued==1&&display.positiveTimers==1,"WAKE_QUERY_REENTRANT_DOUBLE_ARM");
 }
 @Test public void wakeQueryWidgetDisposeDoesNotResurrectTicker()throws Exception{wakeQueryCase(0);}
 @Test public void wakeQueryDisplayDisposeDoesNotScheduleOrThrow()throws Exception{wakeQueryCase(1);}
 @Test public void wakeQueryHiddenReparentDoesNotArmTicker()throws Exception{wakeQueryCase(2);}
 @Test public void wakeQueryReentrantDeltaArmsOnlyOnce()throws Exception{wakeQueryCase(3);}

 /** One bounded diagnostic on the actual D_ANIM callback path, after its first native denial. */
 public static Map<String,Object> runBlockedDiagnostic()throws Exception{
  WebClockAdmissionAtomTest test=new WebClockAdmissionAtomTest();test.setup();
  try{
   Map<String,Integer> construction=test.prepareBlocked();CanvasProbe widget=test.widget;Model model=test.model;FilterDisplay display=test.display;Runnable ticker=test.ticker();
   final int samples=20,batch=5000,warm=1000;long[] elapsed=new long[samples],before=new long[samples],after=new long[samples],allocated=new long[samples];String[] statuses=new String[samples];
   java.lang.management.ThreadMXBean base=java.lang.management.ManagementFactory.getThreadMXBean();com.sun.management.ThreadMXBean bean=base instanceof com.sun.management.ThreadMXBean ext?ext:null;String reason="AVAILABLE";if(bean==null)reason="NOT_RUN_EXTENSION";else if(!bean.isThreadAllocatedMemorySupported())reason="NOT_RUN_UNSUPPORTED";else if(!bean.isThreadAllocatedMemoryEnabled())reason="NOT_RUN_DISABLED";boolean measure=reason.equals("AVAILABLE");long thread=Thread.currentThread().threadId();
   if(measure)for(int i=0;i<32;i++)bean.getThreadAllocatedBytes(thread);
   int generation=model.generation(),phase=model.snapshot()[8];widget.resetQueries();model.resetCost();display.resetTimers();
   for(int i=0;i<warm;i++)model.pulse();
   for(int sample=0;sample<samples;sample++){
    long a0=measure?bean.getThreadAllocatedBytes(thread):-1,start=System.nanoTime();
    for(int i=0;i<batch;i++)model.pulse();
    long ns=System.nanoTime()-start,a1=measure?bean.getThreadAllocatedBytes(thread):-1;
    elapsed[sample]=ns;before[sample]=a0;after[sample]=a1;
    if(measure&&a0>=0&&a1>=a0){allocated[sample]=a1-a0;statuses[sample]="MEASURED_APPROXIMATE";}else{allocated[sample]=-1;statuses[sample]=measure?"NOT_RUN_INVALID_READING":reason;}
   }
   int total=warm+samples*batch;
   need(model.pulses==total&&model.blitReads==total&&model.damageReads==total&&model.damageHits==0&&model.emits==0,"PERF_EXACT_DAMAGE_FREE_CALLBACK_COUNTS");
   need(widget.visibleQueries==0&&widget.shellQueries==0&&widget.sizeQueries==0&&display.timerCalls==0&&test.queued(ticker)==0,"PERF_BLOCKED_NATIVE_QUERY_TIMER_LAW");
   need(model.snapshot()[8]==phase&&model.generation()==generation+total,"PERF_PHASE_AND_DELTA_GENERATION_LAW");
   List<Double> nsPerInput=new ArrayList<>(),bytesPerInput=new ArrayList<>();for(int i=0;i<samples;i++){nsPerInput.add(elapsed[i]/(double)batch);bytesPerInput.add(allocated[i]<0?null:allocated[i]/(double)batch);}
   Map<String,Object> result=new LinkedHashMap<>();result.put("nsPerInput",nsPerInput);result.put("allocatedBytesPerInput",bytesPerInput);result.put("samples",samples);result.put("inputsPerSample",batch);result.put("warmInputs",warm);result.put("totalInputs",total);result.put("constructionAndResizeQueries",construction);result.put("firstDenialQueries",Map.of("isVisible",1,"getShell",0,"getSize",0));result.put("additionalQueries",Map.of("isVisible",0,"getShell",0,"getSize",0));result.put("elapsedNs",elapsed);result.put("allocationBeforeBytes",before);result.put("allocationAfterBytes",after);result.put("allocatedBytes",allocated);result.put("allocationStatus",statuses);result.put("allocationAvailability",reason);result.put("allocationBean",base.getClass().getName());result.put("platformThread",!Thread.currentThread().isVirtual());result.put("modelCallbacks",total);result.put("blitReads",model.blitReads);result.put("damageReads",model.damageReads);result.put("damageHits",0);result.put("emitCalls",0);result.put("timerCalls",0);result.put("queuedOwnTicker",0);result.put("phaseUnchanged",true);result.put("generationDelta",model.generation()-generation);result.put("timingThreshold","NONE");result.put("scope","Unopened actual native host; repeated no-damage D_ANIM inputs only. Not whole-widget performance or zero-allocation guarantee.");return result;
  }finally{test.close();}
 }
}
