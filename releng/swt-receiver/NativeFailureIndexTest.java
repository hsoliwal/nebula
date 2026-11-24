/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Self-contained JDK21 admission, lifetime, deterministic inventory and XXE proof. */
public final class NativeFailureIndexTest {
    private static int assertions;
    private NativeFailureIndexTest() { }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-nebula-report-root-");
        Path output = Files.createTempDirectory("m3-nebula-report-output-");
        try {
            Path widgets = root.resolve("widgets");
            Files.createDirectories(widgets);
            denied(IOException.class, () -> NativeFailureIndex.index(root, output));
            Path vnc = widgets.resolve("vnc/org.eclipse.nebula.widgets.vnc.tests/target/surefire-reports/TEST-native.xml");
            Path web = widgets.resolve("web/org.eclipse.nebula.widgets.web.tests/target/surefire-reports/TEST-clip.xml");
            Path pshelf = widgets.resolve("pshelf/org.eclipse.nebula.widgets.pshelf.tests/target/surefire-reports/TEST-pane.xml");
            write(vnc, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <testsuite name="VncViewerNativeTest" tests="3" failures="1" errors="0" skipped="1">
                      <testcase classname="org.eclipse.nebula.widgets.vnc.VncViewerNativeTest" name="capturesDecodedRfbPixelsFromTheVisibleScreen">
                        <failure type="java.lang.AssertionError" message="SWT visible-screen pixels did not match the RFB frame">pixel mismatch</failure>
                      </testcase>
                      <testcase classname="org.eclipse.nebula.widgets.vnc.VncViewerNativeTest" name="rawFramePasses"/>
                      <testcase classname="org.eclipse.nebula.widgets.vnc.VncViewerNativeTest" name="cocoaConditional"><skipped message="mac-only"/></testcase>
                    </testsuite>
                    """);
            write(web, """
                    <testsuite name="WebCanvasExactClip_Test" tests="2" failures="2" errors="0" skipped="0">
                     <testcase classname="org.eclipse.nebula.widgets.web.WebCanvasExactClip_Test" name="originallyUnclippedStateRemainsUnclipped"><failure type="AssertionError" message="GC clip remains set"/></testcase>
                     <testcase classname="org.eclipse.nebula.widgets.web.WebCanvasExactClip_Test" name="warmCache120FramesRespectCanvasAndRecordedGCOwnership"><failure type="AssertionError" message="fixture must observe actual recorded GC copies"/></testcase>
                    </testsuite>
                    """);
            write(pshelf, """
                    <testsuite name="PShelfViewportTest" tests="1" failures="0" errors="1" skipped="0">
                     <testcase classname="org.eclipse.nebula.widgets.pshelf.PShelfViewportTest" name="hoverDamagesTheChangedHeadersOnly"><error type="java.lang.IllegalStateException" message="native widget disposed"/></testcase>
                    </testsuite>
                    """);
            NativeFailureIndex.Summary result = NativeFailureIndex.index(root, output);
            eq(3, result.reports());
            eq(6, result.tests());
            eq(3, result.failed());
            eq(1, result.errors());
            eq(1, result.skipped());
            eq(3, result.modules());
            byte[] cases = Files.readAllBytes(output.resolve("cases.tsv"));
            byte[] reports = Files.readAllBytes(output.resolve("reports.tsv"));
            byte[] summary = Files.readAllBytes(output.resolve("summary.md"));
            String text = new String(cases, StandardCharsets.UTF_8);
            yes(text.contains("native-pixel-comparison"));
            yes(text.contains("paint-clipping-or-lifetime"));
            yes(!text.contains("NATIVE_SUCCESS")); // no synthetic native acceptance
            yes(text.contains("SKIPPED"));
            yes(text.contains("SWT visible-screen pixels did not match the RFB frame"));
            yes(new String(summary, StandardCharsets.UTF_8).contains("Failing modules"));
            yes(new String(reports, StandardCharsets.UTF_8).contains("sha256"));
            NativeFailureIndex.Summary replay = NativeFailureIndex.index(root, output);
            eq(result, replay);
            yes(java.util.Arrays.equals(cases, Files.readAllBytes(output.resolve("cases.tsv"))));
            yes(java.util.Arrays.equals(reports, Files.readAllBytes(output.resolve("reports.tsv"))));
            yes(java.util.Arrays.equals(summary, Files.readAllBytes(output.resolve("summary.md"))));
            Path malicious = widgets.resolve("unsafe/target/surefire-reports/TEST-doctype.xml");
            write(malicious, """
                    <!DOCTYPE testsuite [<!ENTITY exploit SYSTEM "file:///etc/passwd">]>
                    <testsuite tests="1"><testcase classname="Unsafe" name="unsafe"><failure message="&exploit;"/></testcase></testsuite>
                    """);
            denied(IOException.class, () -> NativeFailureIndex.index(root, output));
            Files.delete(malicious);
            Path linked = widgets.resolve("linked/target/surefire-reports/TEST-linked.xml");
            Files.createDirectories(linked.getParent());
            Files.createSymbolicLink(linked, vnc);
            denied(IOException.class, () -> NativeFailureIndex.index(root, output));
            Files.delete(linked);
            denied(IllegalArgumentException.class, () -> NativeFailureIndex.index(root, widgets.resolve("diagnostics")));
            eq(3, NativeFailureIndex.index(root, output).reports());
        } finally {
            delete(root);
            delete(output);
        }
        System.out.println("NEBULA_NATIVE_FAILURE_INDEX_PROOF_PASS assertions=" + assertions);
    }

    private static void write(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }
    private static void delete(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
    private interface Operation { void run() throws Exception; }
    private static void denied(Class<? extends Throwable> type, Operation action) throws Exception {
        assertions++;
        try { action.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Wrong refusal type", error);
        }
        throw new AssertionError("Expected refusal: " + type);
    }
    private static void eq(Object a, Object b) { assertions++; if (!java.util.Objects.equals(a,b)) throw new AssertionError(a+" != "+b); }
    private static void yes(boolean test) { assertions++; if (!test) throw new AssertionError("Check " + assertions); }
}
