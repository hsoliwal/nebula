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

/** Separate source-frozen baseline for combined failures and removeFilter-call reentry. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class WebCanvasFilterRetirementExpandedLifecycleTest {
 private FaultDisplay display; private Shell shell; private Composite parent; private Canvas foreign;
 private Model model; private ObservedCanvas captured;
 private long capturedHandle,parentHandle,foreignHandle; private Runnable capturedTicker; private Listener capturedAncestor;
 private Set<Object> beforeResources;
 private final IdentityHashMap<Font,Integer> displayOwnedFonts=new IdentityHashMap<>(); private Map<?,?> providerRegistries;
 private int foreignHideCalls,foreignIconifyCalls,disposeHookCalls,caseNumber,reentryCalls,reentryMask;
 private String caseId,identityMode="DISTINCT",faultTiming="BEFORE",reentryStage="NONE";
 private boolean normalDispose,persistent,faultBoth,reenterOnRemove,reentryInvoked,reentryReturned;
 private Throwable reentryFailure;
 private final RuntimeException primary=new RuntimeException("ADD"),sharedRuntime=new RuntimeException("SHARED_RUNTIME");
 private final AssertionError sharedError=new AssertionError("SHARED_ERROR"),hideError=new AssertionError("HIDE_ERROR"),budgetFailure=new AssertionError("OWNED_REMOVAL_BUDGET");
 private final RuntimeException[] hideMarkers={new RuntimeException("H1"),new RuntimeException("H2")};
 private final RuntimeException[] iconifyMarkers={new RuntimeException("I1"),new RuntimeException("I2")};
 private final IdentityHashMap<Throwable,String> exceptionIds=new IdentityHashMap<>();
 private final Listener foreignHide=event->foreignHideCalls++,foreignIconify=event->foreignIconifyCalls++;
 private static final List<Map<String,Object>> OBSERVATIONS=new ArrayList<>(); private static int completedCases,closedCases;
 private static void need(boolean b,String label){if(!b)throw new AssertionError(label);}
 private static Object field(Object owner,Class<?> type,String name)throws ReflectiveOperationException{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 private static Object field(Object owner,String name)throws ReflectiveOperationException{return field(owner,owner.getClass(),name);}
 private String id(Throwable value){return value==null?"NONE":exceptionIds.getOrDefault(value,"UNKNOWN:"+value.getClass().getName());}
 private static void throwExact(Throwable value){if(value instanceof RuntimeException failure)throw failure;if(value instanceof Error failure)throw failure;throw new AssertionError("UNKNOWN_MARKER_TYPE");}
 private static final class FaultDisplay extends Display {
  WebCanvasFilterRetirementExpandedLifecycleTest owner; boolean armAdd,armRemove,budgetExceeded;
  int addAttempts,removeCalls,hideAttempts,iconifyAttempts,hideRefusals,iconifyRefusals,positiveTimers,timerCalls;
  final List<Throwable> removalThrows=new ArrayList<>(); final List<Map<String,Object>> removalTrace=new ArrayList<>();
  FaultDisplay(DeviceData data){super(data);}
  @Override protected void checkSubclass(){ }
  @Override public void addFilter(int type,Listener listener){
   super.addFilter(type,listener);
   if(armAdd&&(type==SWT.Hide||type==SWT.Iconify)&&++addAttempts==2){owner.captureSubject();throw owner.primary;}
  }
  @Override public void removeFilter(int type,Listener listener){
   if(!armRemove||listener!=owner.capturedAncestor||(type!=SWT.Hide&&type!=SWT.Iconify)){super.removeFilter(type,listener);return;}
   boolean hide=type==SWT.Hide;int attempt=hide?++hideAttempts:++iconifyAttempts;removeCalls++;
   removalTrace.add(Map.of("event",hide?"Hide":"Iconify","attempt",attempt,"stage","ENTER"));
   if(attempt>2||removeCalls>4){budgetExceeded=true;removalThrows.add(owner.budgetFailure);throwExact(owner.budgetFailure);}
   boolean selected=hide||owner.faultBoth;int refused=hide?hideRefusals:iconifyRefusals;
   boolean fail=selected&&(owner.persistent||refused==0);
   Throwable marker=null;
   if(fail){if(hide)hideRefusals++;else iconifyRefusals++;marker=owner.marker(type,attempt);}
   if(owner.faultTiming.equals("BEFORE")){
    if(hide)owner.reenter("BEFORE");
    if(fail){removalThrows.add(marker);removalTrace.add(Map.of("event",hide?"Hide":"Iconify","attempt",attempt,"stage","BEFORE_THROW","marker",owner.id(marker)));throwExact(marker);}
   }
   super.removeFilter(type,listener);
   if(owner.faultTiming.equals("AFTER")){
    if(hide)owner.reenter("AFTER");
    if(fail){removalThrows.add(marker);removalTrace.add(Map.of("event",hide?"Hide":"Iconify","attempt",attempt,"stage","AFTER_THROW","marker",owner.id(marker)));throwExact(marker);}
   }
   removalTrace.add(Map.of("event",hide?"Hide":"Iconify","attempt",attempt,"stage","RETURNED"));
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
 private static final class ObservedCanvas extends WebCanvas<Model> {
  int parentQueries,visibleQueries,shellQueries,sizeQueries;
  ObservedCanvas(Composite parent,Model model){super(parent,SWT.NONE,model);}
  @Override public Composite getParent(){parentQueries++;return super.getParent();}
  @Override public boolean isVisible(){visibleQueries++;return super.isVisible();}
  @Override public Shell getShell(){shellQueries++;return super.getShell();}
  @Override public Point getSize(){sizeQueries++;return super.getSize();}
  final void resetQueries(){parentQueries=visibleQueries=shellQueries=sizeQueries=0;}
  final void disposeThroughBase(){super.dispose();}
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


 private Throwable marker(int type,int attempt){
  if(identityMode.equals("SHARED_RUNTIME"))return sharedRuntime;
  if(identityMode.equals("SHARED_ERROR"))return sharedError;
  if(identityMode.equals("ORIGINAL"))return primary;
  if(identityMode.equals("ERROR_RUNTIME")&&type==SWT.Hide)return hideError;
  return (type==SWT.Hide?hideMarkers:iconifyMarkers)[attempt-1];
 }
 private void reenter(String stage){
  if(!reenterOnRemove||reentryInvoked)return;
  reentryInvoked=true;reentryCalls++;reentryStage=stage;
  try{reentryMask=(Integer)field(captured,WebCanvas.class,"clockFilters");}
  catch(ReflectiveOperationException failure){throw new AssertionError("REENTRY_OWNER_FIELD",failure);}
  try{captured.dispose();reentryReturned=true;}
  catch(RuntimeException|Error failure){reentryFailure=failure;throw failure;}
 }
 private void captureSubject(){
  List<Control> own=new ArrayList<>();for(Control child:parent.getChildren())if(child!=foreign)own.add(child);
  need(own.size()==1&&own.get(0) instanceof ObservedCanvas,"CAPTURE_EXACT_OWN_WIDGET");
  captured=(ObservedCanvas)own.get(0);capturedHandle=captured.handle;
  need(capturedHandle!=0&&OS.GetParent(capturedHandle)==parentHandle&&OS.GetWindowThreadProcessId(capturedHandle,null)==OS.GetCurrentThreadId()&&nativeChildren(parentHandle).contains(capturedHandle)&&display.findWidget(capturedHandle)==captured,"CAPTURE_EXACT_NATIVE_BIRTH");
  try{capturedTicker=(Runnable)field(captured,WebCanvas.class,"ticker");capturedAncestor=(Listener)field(captured,WebCanvas.class,"ancestorListener");}
  catch(ReflectiveOperationException failure){throw new AssertionError("CAPTURE_OWNER_FIELDS",failure);}
  captured.addListener(SWT.Dispose,event->disposeHookCalls++);
 }
 @Before public void setup(){
  org.junit.Assume.assumeTrue("Pinned Win32 SWT4974 native ownership and private state; other runtimes unqualified.","win32".equals(SWT.getPlatform())&&SWT.getVersion()==4974);
  DeviceData data=new DeviceData();data.tracking=true;display=new FaultDisplay(data);display.owner=this;
  display.setRuntimeExceptionHandler(error->{throw error;});display.setErrorHandler(error->{throw error;});
  display.getSystemFont();display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
  shell=new Shell(display);parent=new Composite(shell,SWT.NONE);parent.setBounds(0,0,360,240);parentHandle=parent.handle;
  foreign=new Canvas(parent,SWT.NONE);foreign.setBounds(1,1,8,8);foreignHandle=foreign.handle;
  display.addFilter(SWT.Hide,foreignHide);display.addFilter(SWT.Iconify,foreignIconify);beforeResources=resources();
  exceptionIds.put(primary,"ADD");exceptionIds.put(sharedRuntime,"SAME_RUNTIME");exceptionIds.put(sharedError,"SAME_ERROR");exceptionIds.put(hideError,"HIDE_ERROR");exceptionIds.put(budgetFailure,"BUDGET");
  for(int i=0;i<2;i++){exceptionIds.put(hideMarkers[i],"H"+(i+1));exceptionIds.put(iconifyMarkers[i],"I"+(i+1));}
  need(parent.getChildren().length==1&&parent.getChildren()[0]==foreign&&nativeChildren(parentHandle).equals(List.of(foreignHandle)),"FOREIGN_NATIVE_PRECONDITION");
 }
 private Map<String,Object> graph(Throwable actual){
  Throwable expectedRoot=normalDispose&&!display.removalThrows.isEmpty()?display.removalThrows.get(0):primary;
  IdentityHashMap<Throwable,List<Throwable>> expected=new IdentityHashMap<>();expected.put(expectedRoot,new ArrayList<>());
  List<Throwable> uniqueCleanup=new ArrayList<>();
  for(Throwable value:display.removalThrows)if(!expected.containsKey(value)){expected.put(value,new ArrayList<>());uniqueCleanup.add(value);}
  Throwable firstCleanup=display.removalThrows.isEmpty()?null:display.removalThrows.get(0);
  if(!normalDispose&&firstCleanup!=null&&firstCleanup!=expectedRoot){
   expected.get(expectedRoot).add(firstCleanup);
   for(Throwable value:uniqueCleanup)if(value!=firstCleanup)expected.get(firstCleanup).add(value);
  }else expected.get(expectedRoot).addAll(uniqueCleanup);
  List<Map<String,Object>> actualRows=new ArrayList<>(),expectedRows=new ArrayList<>();
  List<Throwable> expectedOrder=new ArrayList<>();expectedOrder.add(expectedRoot);
  if(firstCleanup!=null&&firstCleanup!=expectedRoot)expectedOrder.add(firstCleanup);
  for(Throwable value:uniqueCleanup)if(value!=firstCleanup)expectedOrder.add(value);
  for(Throwable value:expectedOrder){List<String> childIds=new ArrayList<>();for(Throwable child:expected.get(value))childIds.add(id(child));expectedRows.add(Map.of("id",id(value),"suppressed",List.copyOf(childIds)));}
  Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());Deque<Throwable> pendingGraph=new ArrayDeque<>();if(actual!=null)pendingGraph.add(actual);
  boolean bounded=true,unique=true,known=true,causesClear=true;
  while(!pendingGraph.isEmpty()){
   if(seen.size()>=16){bounded=false;break;}
   Throwable value=pendingGraph.removeFirst();if(!seen.add(value)){unique=false;continue;}
   known&=exceptionIds.containsKey(value);causesClear&=value.getCause()==null;
   List<String> childIds=new ArrayList<>();for(Throwable child:value.getSuppressed()){childIds.add(id(child));if(pendingGraph.size()<32)pendingGraph.addLast(child);else bounded=false;}
   actualRows.add(Map.of("id",id(value),"suppressed",List.copyOf(childIds)));
  }
  boolean identities=bounded&&unique&&known&&causesClear&&seen.equals(expected.keySet());
  boolean shape=actual==expectedRoot;
  for(var entry:expected.entrySet()){Throwable[] children=entry.getKey().getSuppressed();List<Throwable> wanted=entry.getValue();if(children.length!=wanted.size())shape=false;else for(int i=0;i<children.length;i++)if(children[i]!=wanted.get(i))shape=false;}
  List<String> actualThrows=new ArrayList<>();for(Throwable value:display.removalThrows)actualThrows.add(id(value));
  Map<String,Object> result=new LinkedHashMap<>();result.put("root",id(actual));result.put("expectedRoot",id(expectedRoot));result.put("actual",List.copyOf(actualRows));result.put("expected",List.copyOf(expectedRows));result.put("throws",List.copyOf(actualThrows));result.put("identitiesRetainedOnce",identities);result.put("shapeAndOrder",shape);result.put("bounded",bounded);
  return Collections.unmodifiableMap(result);
 }
 private void configure(int number){
  caseNumber=number;faultBoth=true;faultTiming="BEFORE";identityMode="DISTINCT";
  switch(number){
   case 1:caseId="ctor_both_before_once_distinct";break;
   case 2:caseId="ctor_both_before_once_shared_runtime";identityMode="SHARED_RUNTIME";break;
   case 3:caseId="ctor_both_before_once_error_runtime";identityMode="ERROR_RUNTIME";break;
   case 4:caseId="ctor_both_before_persistent";persistent=true;break;
   case 5:caseId="normal_dispose_both_before_once";normalDispose=true;break;
   case 6:caseId="ctor_remove_reentry_before";faultBoth=false;reenterOnRemove=true;break;
   case 7:caseId="ctor_remove_reentry_after";faultBoth=false;faultTiming="AFTER";reenterOnRemove=true;break;
   case 8:caseId="normal_remove_reentry_before";normalDispose=true;faultBoth=false;reenterOnRemove=true;break;
   case 9:caseId="ctor_persistent_reentry_shared_error";persistent=true;identityMode="SHARED_ERROR";reenterOnRemove=true;break;
   case 10:caseId="ctor_cleanup_throws_original";identityMode="ORIGINAL";break;
   default:throw new AssertionError("UNKNOWN_CASE");
  }
 }
 private void runCase(int number)throws Exception{
  configure(number);model=new Model();display.armRemove=true;Throwable actual=null;
  try{if(normalDispose){new ObservedCanvas(parent,model);captureSubject();captured.dispose();}else{display.armAdd=true;new ObservedCanvas(parent,model);}}
  catch(RuntimeException|Error failure){actual=failure;}finally{display.armAdd=false;}
  need(captured!=null&&capturedHandle!=0,"CAPTURE_REQUIRED");
  Map<String,Object> failureGraph=graph(actual);
  int ownHide=countFilter(SWT.Hide,capturedAncestor),ownIconify=countFilter(SWT.Iconify,capturedAncestor),mask=(Integer)field(captured,WebCanvas.class,"clockFilters");
  int observedBits=(ownHide>0?1:0)|(ownIconify>0?2:0);
  boolean maskTruthful=(mask&3)==observedBits;
  List<Long> natives=nativeChildren(parentHandle);int ownNative=0;for(long handle:natives)if(handle!=foreignHandle)ownNative++;
  int ownChildren=0;for(Control child:parent.getChildren())if(child!=foreign)ownChildren++;
  boolean registry=display.findWidget(capturedHandle)==captured;int nativeThread=OS.GetWindowThreadProcessId(capturedHandle,null);
  Set<Object> extra=resources();extra.removeAll(beforeResources);
  boolean foreignLive=!foreign.isDisposed()&&foreign.handle==foreignHandle&&OS.GetParent(foreignHandle)==parentHandle&&OS.GetWindowThreadProcessId(foreignHandle,null)==OS.GetCurrentThreadId()&&display.findWidget(foreignHandle)==foreign&&natives.contains(foreignHandle)&&countFilter(SWT.Hide,foreignHide)==1&&countFilter(SWT.Iconify,foreignIconify)==1;
  int lifetimePositive=display.positiveTimers;captured.resetQueries();model.resetReads();display.timerCalls=display.positiveTimers=0;foreignHideCalls=foreignIconifyCalls=0;
  int phase=model.snapshot()[8],generation=model.generation();Throwable postFailure=null;
  try{model.pulse();foreign.setVisible(false);foreign.setVisible(true);}catch(RuntimeException|Error failure){postFailure=failure;}
  boolean cpuInert=model.blitReads==0&&model.damageReads==0&&captured.parentQueries==0&&captured.visibleQueries==0&&captured.shellQueries==0&&captured.sizeQueries==0&&display.timerCalls==0&&queued(capturedTicker)==0&&model.snapshot()[8]==phase&&postFailure==null;
  boolean nativeGone=ownNative==0&&ownChildren==0&&nativeThread==0&&!registry&&captured.isDisposed()&&captured.handle==0;
  boolean budget=!display.budgetExceeded&&display.hideAttempts<=2&&display.iconifyAttempts<=2&&display.removeCalls<=4;
  boolean reentryOK=reenterOnRemove?reentryCalls==1&&reentryReturned&&reentryFailure==null:reentryCalls==0;
  boolean graphOK=Boolean.TRUE.equals(failureGraph.get("identitiesRetainedOnce"))&&Boolean.TRUE.equals(failureGraph.get("shapeAndOrder"));
  boolean filterTarget=persistent?ownHide==1&&ownIconify==1:ownHide==0&&ownIconify==0;
  boolean common=nativeGone&&cpuInert&&extra.isEmpty()&&disposeHookCalls==1&&foreignLive&&foreignHideCalls==1&&foreignIconifyCalls==0&&lifetimePositive==0;
  boolean target=actual!=null&&common&&budget&&reentryOK&&graphOK&&maskTruthful&&filterTarget;
  Map<String,Object> nativeState=Map.of("handle",Long.toUnsignedString(capturedHandle),"children",List.copyOf(strings(natives)),"ownNative",ownNative,"ownPublic",ownChildren,"thread",nativeThread,"registry",registry,"disposed",captured.isDisposed(),"handleZero",captured.handle==0,"gone",nativeGone);
  Map<String,Object> cpu=Map.of("blit",model.blitReads,"damage",model.damageReads,"parent",captured.parentQueries,"visible",captured.visibleQueries,"shell",captured.shellQueries,"size",captured.sizeQueries,"timers",display.timerCalls,"queued",queued(capturedTicker),"inert",cpuInert);
  Map<String,Object> reentry=Map.of("required",reenterOnRemove,"calls",reentryCalls,"stage",reentryStage,"returned",reentryReturned,"failure",id(reentryFailure),"maskAtCall",reentryMask);
  Map<String,Object> row=new LinkedHashMap<>();row.put("case",caseId);row.put("number",caseNumber);row.put("preTeardownAndPump",true);row.put("persistentBoundary",persistent);row.put("normalDispose",normalDispose);row.put("timing",faultTiming);row.put("identityMode",identityMode);
  row.put("graph",failureGraph);row.put("removeTrace",List.copyOf(display.removalTrace));row.put("attempts",Map.of("Hide",display.hideAttempts,"Iconify",display.iconifyAttempts,"total",display.removeCalls,"withinBudget",budget));
  row.put("filters",Map.of("Hide",ownHide,"Iconify",ownIconify,"mask",mask,"observedBits",observedBits,"truthful",maskTruthful,"absence",ownHide==0&&ownIconify==0));
  row.put("native",nativeState);row.put("cpu",cpu);row.put("reentry",reentry);row.put("foreign",Map.of("exactIdentityAndFilters",foreignLive,"hideCalls",foreignHideCalls,"iconifyCalls",foreignIconifyCalls));
  row.put("disposeCalls",disposeHookCalls);row.put("extraGraphics",extra.size());row.put("positiveTimerArms",lifetimePositive);row.put("phaseUnchanged",model.snapshot()[8]==phase);row.put("generationDelta",model.generation()-generation);row.put("modelBindingNonNull",model.installed!=null);row.put("postException",id(postFailure));row.put("target",target);
  OBSERVATIONS.add(Collections.unmodifiableMap(new LinkedHashMap<>(row)));completedCases++;
  String trace=caseId+" target="+target+" filters="+ownHide+"/"+ownIconify+" mask="+mask+" truthful="+maskTruthful+" graph="+graphOK+" reentry="+reentryOK+" nativeGone="+nativeGone+" cpuInert="+cpuInert;
  need(trace.length()<=384,"BOUNDED_PRETEARDOWN_TRACE");System.out.println("FILTER_EXPANDED_PRETEARDOWN "+trace);
  need(target,"FILTER_EXPANDED_TARGET case="+caseId+" filters="+ownHide+"/"+ownIconify+" maskTruthful="+maskTruthful+" graph="+graphOK+" nativeGone="+nativeGone+" cpuInert="+cpuInert);
 }
 @Test public void test01CtorBothBeforeDistinct()throws Exception{runCase(1);}
 @Test public void test02CtorBothBeforeSharedRuntime()throws Exception{runCase(2);}
 @Test public void test03CtorBothBeforeErrorRuntime()throws Exception{runCase(3);}
 @Test public void test04CtorBothPersistentBoundary()throws Exception{runCase(4);}
 @Test public void test05NormalDisposeBothBeforeDistinct()throws Exception{runCase(5);}
 @Test public void test06CtorRemoveReentryBefore()throws Exception{runCase(6);}
 @Test public void test07CtorRemoveReentryAfterControl()throws Exception{runCase(7);}
 @Test public void test08NormalRemoveReentryBefore()throws Exception{runCase(8);}
 @Test public void test09CtorPersistentReentrySharedError()throws Exception{runCase(9);}
 @Test public void test10CtorCleanupThrowsOriginal()throws Exception{runCase(10);}
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
  closedCases++;System.out.println("FILTER_EXPANDED_OWNED_CLEANUP exactAdaptersGone=true nativeAndRegistryGone=true displayDisposed=true");
 }
 public static Map<String,Object> observations(){return Map.of("schema","synexia.webcanvas.filter-retirement.expanded-observations.v1","cases",new ArrayList<>(OBSERVATIONS),"completedCases",completedCases,"closedCases",closedCases,"sourceBindingAtBirth","f5a04cb42c418b72e3cb3ca8dbd5f2276bbeb470","productionSourceChanges",0,"persistentRefusalIsNotGlobalAbsence",true);}
}
