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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** CPU laws for exact failure identities and cycle-safe exception attachment. */
public final class WebCanvasFilterFailureGraphTest {
 private static final List<Map<String,Object>> CALLER_OBSERVATIONS=new ArrayList<>();
 private static final class Disabled extends RuntimeException {
  private static final long serialVersionUID=1L;
  Disabled(){super("disabled",null,false,false);}
 }
 private static final class ExplosiveCause extends RuntimeException {
  private static final long serialVersionUID=1L;
  final RuntimeException unavailable=new RuntimeException("inspection unavailable"); int inspections;
  @Override public synchronized Throwable getCause(){inspections++;throw unavailable;}
 }
 private static void append(Throwable first,Throwable later)throws Exception{
  Method method=WebCanvas.class.getDeclaredMethod("appendCleanupFailure",Throwable.class,Throwable.class);
  method.setAccessible(true);method.invoke(null,first,later);
 }
 private static void appendUnder(Throwable first,Throwable later,Throwable outer)throws Exception{
  Method method=WebCanvas.class.getDeclaredMethod("appendCleanupFailure",Throwable.class,Throwable.class,Throwable.class);
  method.setAccessible(true);method.invoke(null,first,later,outer);
 }
 private static RuntimeException chain(int size){
  RuntimeException value=new RuntimeException("tail");
  for(int i=1;i<size;i++)value=new RuntimeException("node",value);
  return value;
 }
 private static int chainLength(Throwable value){
  int count=0;while(value!=null){count++;value=value.getCause();}return count;
 }
 @Test public void distinctCleanupOrder()throws Exception{
  RuntimeException first=new RuntimeException("first"),second=new RuntimeException("second");
  AssertionError third=new AssertionError("third");
  append(first,second);append(first,third);append(first,second);
  assertArrayEquals(new Throwable[]{second,third},first.getSuppressed());
 }
 @Test public void sameIdentityAndOriginalPrimary()throws Exception{
  RuntimeException original=new RuntimeException("ADD");
  append(original,original);append(original,original);
  assertEquals(0,original.getSuppressed().length);
  RuntimeException cleanup=new RuntimeException("H1");appendUnder(cleanup,original,original);append(original,cleanup);
  assertArrayEquals(new Throwable[]{cleanup},original.getSuppressed());assertEquals(0,cleanup.getSuppressed().length);
 }
 @Test public void mixedErrorFirst()throws Exception{
  AssertionError first=new AssertionError("first error");RuntimeException later=new RuntimeException("later");
  append(first,later);assertArrayEquals(new Throwable[]{later},first.getSuppressed());
 }
 @Test public void alreadyReachableViaCause()throws Exception{
  RuntimeException later=new RuntimeException("later"),first=new RuntimeException("first",later);
  append(first,later);assertSame(later,first.getCause());assertEquals(0,first.getSuppressed().length);
  RuntimeException causeLater=new RuntimeException("I1"),causeOuter=new RuntimeException("ADD",causeLater),causeCleanup=new RuntimeException("H1");
  appendUnder(causeCleanup,causeLater,causeOuter);append(causeOuter,causeCleanup);recordCaller("cause",causeOuter,causeCleanup,causeLater);
  RuntimeException suppressedLater=new RuntimeException("I1"),suppressedOuter=new RuntimeException("ADD"),suppressedCleanup=new RuntimeException("H1");suppressedOuter.addSuppressed(suppressedLater);
  appendUnder(suppressedCleanup,suppressedLater,suppressedOuter);append(suppressedOuter,suppressedCleanup);recordCaller("suppressed",suppressedOuter,suppressedCleanup,suppressedLater);
  assertEquals("H1 must not duplicate a cleanup already retained by the caller cause",0,causeCleanup.getSuppressed().length);
  assertEquals("H1 must not duplicate a cleanup already retained by caller suppression",0,suppressedCleanup.getSuppressed().length);
  assertSame(causeLater,causeOuter.getCause());assertArrayEquals(new Throwable[]{causeCleanup},causeOuter.getSuppressed());
  assertArrayEquals(new Throwable[]{suppressedLater,suppressedCleanup},suppressedOuter.getSuppressed());
 }
 @Test public void wouldCycleViaSuppression()throws Exception{
  RuntimeException first=new RuntimeException("first"),later=new RuntimeException("later");
  later.addSuppressed(first);append(first,later);
  assertEquals(0,first.getSuppressed().length);assertArrayEquals(new Throwable[]{first},later.getSuppressed());
  RuntimeException outer=new RuntimeException("ADD"),cleanup=new RuntimeException("H1"),wrapper=new RuntimeException("wrapper");wrapper.addSuppressed(outer);
  appendUnder(cleanup,wrapper,outer);append(outer,cleanup);
  assertArrayEquals(new Throwable[]{cleanup},outer.getSuppressed());assertEquals(0,cleanup.getSuppressed().length);assertArrayEquals(new Throwable[]{outer},wrapper.getSuppressed());
 }
 @Test public void wouldCycleViaCause()throws Exception{
  RuntimeException first=new RuntimeException("first"),later=new RuntimeException("later",first);
  append(first,later);assertEquals(0,first.getSuppressed().length);assertSame(first,later.getCause());
  RuntimeException outer=new RuntimeException("ADD"),cleanup=new RuntimeException("H1"),wrapper=new RuntimeException("wrapper",outer);
  appendUnder(cleanup,wrapper,outer);append(outer,cleanup);
  assertArrayEquals(new Throwable[]{cleanup},outer.getSuppressed());assertEquals(0,cleanup.getSuppressed().length);assertSame(outer,wrapper.getCause());
 }
 @Test public void longFinitePrimaryGraph4096()throws Exception{
  RuntimeException first=chain(4096),later=new RuntimeException("later");
  append(first,later);assertArrayEquals(new Throwable[]{later},first.getSuppressed());assertEquals(4096,chainLength(first));
 }
 @Test public void longFiniteLaterGraph4096()throws Exception{
  RuntimeException first=new RuntimeException("first"),later=chain(4096);
  append(first,later);assertArrayEquals(new Throwable[]{later},first.getSuppressed());assertEquals(4096,chainLength(later));
 }
 @Test public void sharedDescendantsAndExistingCycle()throws Exception{
  RuntimeException first=new RuntimeException("first"),a=new RuntimeException("a"),b=new RuntimeException("b"),shared=new RuntimeException("shared"),later=new RuntimeException("later");
  first.addSuppressed(a);first.addSuppressed(b);a.addSuppressed(shared);b.addSuppressed(shared);shared.addSuppressed(a);
  append(first,later);append(first,shared);
  assertArrayEquals(new Throwable[]{a,b,later},first.getSuppressed());
  assertArrayEquals(new Throwable[]{shared},a.getSuppressed());assertArrayEquals(new Throwable[]{a},shared.getSuppressed());
 }
 @Test public void unrelatedExistingCycle()throws Exception{
  RuntimeException first=new RuntimeException("first"),a=new RuntimeException("a"),b=new RuntimeException("b");
  a.addSuppressed(b);b.addSuppressed(a);append(first,a);
  assertArrayEquals(new Throwable[]{a},first.getSuppressed());assertArrayEquals(new Throwable[]{b},a.getSuppressed());assertArrayEquals(new Throwable[]{a},b.getSuppressed());
 }
 @Test public void disabledSuppressionBoundary()throws Exception{
  Disabled first=new Disabled();RuntimeException later=new RuntimeException("later");
  append(first,later);assertEquals(0,first.getSuppressed().length);
 }
 @Test public void inspectionFailureBoundary()throws Exception{
  ExplosiveCause first=new ExplosiveCause();RuntimeException later=new RuntimeException("later"),inspection=first.unavailable;
  append(first,later);assertSame(inspection,first.unavailable);assertEquals(1,first.inspections);assertEquals(0,first.getSuppressed().length);
  RuntimeException outer=new RuntimeException("ADD"),cleanup=new RuntimeException("H1");ExplosiveCause unavailable=new ExplosiveCause();
  appendUnder(cleanup,unavailable,outer);append(outer,cleanup);
  assertEquals(1,unavailable.inspections);assertEquals(0,cleanup.getSuppressed().length);assertArrayEquals(new Throwable[]{cleanup},outer.getSuppressed());
 }
 private static List<String> labels(Throwable[] values,Throwable cleanup,Throwable later){
  List<String> result=new ArrayList<>();for(Throwable value:values)result.add(value==cleanup?"H1":value==later?"I1":"UNKNOWN");return List.copyOf(result);
 }
 private static void recordCaller(String mode,RuntimeException outer,RuntimeException cleanup,Throwable later){
  List<String> caller=labels(outer.getSuppressed(),cleanup,later),child=labels(cleanup.getSuppressed(),cleanup,later);
  CALLER_OBSERVATIONS.add(Map.of("mode",mode,"beforeTargetAssertion",true,"outerSuppressed",caller,"cleanupSuppressed",child,"outerCauseIsI1",outer.getCause()==later,"newSharedPath",child.contains("I1"),"firstCleanupRetained",caller.contains("H1")));
 }
 public static Map<String,Object> observations(){return Map.of("schema","synexia.webcanvas.caller-retained-cleanup.cpu-observations.v1","cases",List.copyOf(CALLER_OBSERVATIONS),"inspectionHasObservableCalls",true);}
}
