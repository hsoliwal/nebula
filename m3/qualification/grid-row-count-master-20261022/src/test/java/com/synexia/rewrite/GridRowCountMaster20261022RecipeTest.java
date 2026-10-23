// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
final class GridRowCountMaster20261022RecipeTest {
 private static final String CRATE="grid-row-count-master-20261022";
 private static final String PATH="widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/GridRowAddressedItemDataVisualizer.java";
 private static final String BEFORE="e299be202777451c0a9eccbdea2978fb377d5d68e2d53a6718bee957c7902a27";
 private static final String AFTER="eb609f07d7fd3e0ee0fe3da143f2de6378f34dfd71518a65c91647b43c38497b";
 private static String resource(String file) throws Exception {
  try(var stream=GridRowCountMaster20261022RecipeTest.class.getResourceAsStream("/com/synexia/rewrite/hash-pinned-java/"+CRATE+"/"+file)) {
   assertNotNull(stream,file); return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
  }
 }
 private static String hash(String s) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}
 private static InMemoryExecutionContext ctx(){return new InMemoryExecutionContext(t->{throw new IllegalStateException("OpenRewrite failure",t);});}
 private static List<SourceFile> parsed(String src,String path){return JavaParser.fromJavaVersion().build().parseInputs(List.of(Parser.Input.fromString(Path.of(path),src)),null,ctx()).toList();}
 private static List<Result> executed(List<SourceFile> before){return new M3HashPinnedJavaSnapshotRecipe(CRATE).run(new InMemoryLargeSourceSet(before),ctx(),1).getChangeset().getAllResults();}
 @Test void generateExactCurrentMasterAndReceipt() throws Exception {
  String before=resource("before.java.txt"),candidate=resource("GridRowAddressedItemDataVisualizer.java.txt");
  assertEquals(BEFORE,hash(before)); assertEquals(AFTER,hash(candidate));
  assertEquals(List.of(PATH),new M3HashPinnedJavaSnapshotRecipe(CRATE).targetPaths());
  var result=executed(parsed(before,PATH));assertEquals(1,result.size());
  SourceFile out=result.get(0).getAfter();assertNotNull(out);assertEquals(PATH,out.getSourcePath().toString().replace('\\','/'));
  assertEquals(candidate,out.printAll());assertEquals(AFTER,hash(out.printAll()));
  assertTrue(out.printAll().contains("if (id < rows.length && rows[id] != null) {\n"));
  Path dest=Path.of(System.getProperty("m3.receiving.output")).resolve(PATH);
  Files.createDirectories(dest.getParent());Files.writeString(dest,out.printAll(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
  System.out.println("OPENREWRITE_NEBULA_GRID_COUNT_SHA256="+hash(out.printAll()));
 }
 @Test void fixedPoint() throws Exception {assertTrue(executed(parsed(resource("GridRowAddressedItemDataVisualizer.java.txt"),PATH)).isEmpty());}
 @Test void refusesSourceDrift() throws Exception {assertThrows(RuntimeException.class,()->executed(parsed(resource("before.java.txt")+"\n",PATH)));}
 @Test void refusesMissingSource(){assertThrows(RuntimeException.class,()->executed(List.of()));}
 @Test void refusesDuplicateSource() throws Exception {var a=new ArrayList<>(parsed(resource("before.java.txt"),PATH));a.addAll(parsed(resource("before.java.txt"),PATH));assertThrows(RuntimeException.class,()->executed(a));}
 @Test void refusesForeignPath() throws Exception {assertThrows(RuntimeException.class,()->executed(parsed(resource("before.java.txt"),"widgets/grid/org.eclipse.nebula.widgets.grid/src/org/eclipse/nebula/widgets/grid/Another.java")));}
 @Test void keepsAllOtherCodeAndLicensing() throws Exception {
  String before=resource("before.java.txt"),after=resource("GridRowAddressedItemDataVisualizer.java.txt");
  String atom="\t\t\t\t\tfor (final Object value : rows[id]) {\n\t\t\t\t\t\tif (value != null) {\n\t\t\t\t\t\t\tcount++;\n\t\t\t\t\t\t\tbreak;\n\t\t\t\t\t\t}\n\t\t\t\t\t}";
  assertEquals(after,before.replace(atom,"\t\t\t\t\tcount++;")); assertTrue(after.contains("SPDX-License-Identifier: Apache-2.0 OR EPL-2.0"));
 }
}
