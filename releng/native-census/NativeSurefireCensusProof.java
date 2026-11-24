/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** A real JDK21 proof; never uses a fake JUnit4/SWT API or a native display. */
public final class NativeSurefireCensusProof {
    private NativeSurefireCensusProof() { }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("nebula-native-junit-census-");
        try {
            Path out = root.resolve("diagnostics");
            failed(() -> NativeSurefireCensus.collect(root, out)); // Missing reports fail closed
            Path first = root.resolve("widgets/web/org.eclipse.nebula.widgets.web.tests/target/surefire-reports/TEST-WebCanvasExactClip_Test.xml");
            Path second = root.resolve("widgets/vnc/org.eclipse.nebula.widgets.vnc.tests/target/surefire-reports/TEST-VncViewerNativeTest.xml");
            Files.createDirectories(first.getParent());
            Files.createDirectories(second.getParent());
            Files.writeString(first, """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="org.eclipse.nebula.widgets.web.WebCanvasExactClip_Test" tests="4" failures="1" errors="1" skipped="1">
                  <testcase classname="org.eclipse.nebula.widgets.web.WebCanvasExactClip_Test" name="ok"/>
                  <testcase classname="org.eclipse.nebula.widgets.web.WebCanvasExactClip_Test" name="clip"><failure message="paint failed\tSWT: native"/></testcase>
                  <testcase classname="org.eclipse.nebula.widgets.web.WebCanvasExactClip_Test" name="dispose"><error message="dispose &amp; exit"/></testcase>
                  <testcase classname="org.eclipse.nebula.widgets.web.WebCanvasExactClip_Test" name="unsupported"><skipped message="not available"/></testcase>
                </testsuite>
                """);
            Files.writeString(second, """
                <testsuite name="org.eclipse.nebula.widgets.vnc.VncViewerNativeTest" tests="1" failures="1" errors="0" skipped="0">
                  <testcase classname="org.eclipse.nebula.widgets.vnc.VncViewerNativeTest" name="capturesDecodedRfbPixelsFromTheVisibleScreen"><failure message="SWT screen mismatched"/></testcase>
                </testsuite>
                """);
            var result = NativeSurefireCensus.collect(root, out);
            eq(2, result.suites().size()); eq(5, result.tests());
            eq(2, result.failures()); eq(1, result.errors()); eq(1, result.skipped());
            eq(4, result.nonPassing().size());
            String a = Files.readString(out.resolve("native-suites.tsv"));
            String b = Files.readString(out.resolve("native-cases.tsv"));
            yes(a.contains("widgets/web/org.eclipse.nebula.widgets.web.tests"));
            yes(a.contains("widgets/vnc/org.eclipse.nebula.widgets.vnc.tests"));
            yes(b.contains("clip\tFAILURE\tpaint failed SWT: native"));
            yes(b.contains("dispose\tERROR\tdispose & exit"));
            yes(b.contains("unsupported\tSKIPPED"));
            String baseline = a + "\n" + b;
            NativeSurefireCensus.collect(root, out);
            yes(baseline.equals(Files.readString(out.resolve("native-suites.tsv")) + "\n"
                    + Files.readString(out.resolve("native-cases.tsv"))));
            Files.writeString(first, """
                <!DOCTYPE testsuite [<!ENTITY trap SYSTEM "file:///etc/passwd">]>
                <testsuite name="invalid" tests="1" failures="0" errors="0" skipped="0">
                <testcase classname="bad" name="x">&trap;</testcase></testsuite>
                """);
            failed(() -> NativeSurefireCensus.collect(root, out)); // XML DTD/XXE denied
            Files.writeString(first, "<testsuite name=\"broken\" tests=\"1\"/>");
            failed(() -> NativeSurefireCensus.collect(root, out)); // Incomplete counters denied
            Files.writeString(first, "<testsuite><testcase></testsuite>");
            failed(() -> NativeSurefireCensus.collect(root, out)); // Malformed XML denied
            System.out.println("NEBULA_NATIVE_CENSUS_PROOF_PASS suites=2 statuses=4, strict, stable, XXE denied");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void yes(boolean value) { if (!value) throw new AssertionError("expected true"); }
    private static void eq(long expected, long value) {
        if (expected != value) throw new AssertionError(expected + " expected but got " + value);
    }
    @FunctionalInterface private interface Checked { void execute() throws Exception; }
    private static void failed(Checked action) throws Exception {
        try { action.execute(); } catch (IOException expected) { return; }
        throw new AssertionError("Expected a closed-fail IOException");
    }
}
