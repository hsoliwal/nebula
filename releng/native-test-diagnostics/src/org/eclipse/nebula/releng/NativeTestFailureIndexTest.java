/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.releng;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Standalone JDK21 regression test, separate from actual SWT/Tycho-native runs. */
public final class NativeTestFailureIndexTest {
    private static int checks;
    private NativeTestFailureIndexTest() { }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-native-index-test-");
        try {
            failClosedWhenNoReports(root);
            parsesSortedModulesAndPreservesEveryFailure(root);
            rejectsMalformedOrUntrustedXml(root);
            runsExactCliOutput(root);
            System.out.println("M3_NATIVE_FAILURE_INDEX_SELF_TEST_PASS checks=" + checks);
        } finally {
            try (var files = Files.walk(root)) {
                for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }

    private static void failClosedWhenNoReports(Path root) {
        reject(() -> NativeTestFailureIndex.scan(root));
    }

    private static Path write(Path root, String module, String file, String xml) throws IOException {
        Path report = root.resolve(module).resolve("target/surefire-reports").resolve(file);
        Files.createDirectories(report.getParent());
        Files.writeString(report, xml, StandardCharsets.UTF_8);
        return report;
    }

    private static void parsesSortedModulesAndPreservesEveryFailure(Path root) throws Exception {
        write(root, "widgets/zeta.test", "TEST-Z.xml", """
                <testsuite name="pkg.SuiteZ" tests="3" failures="1" skipped="1">
                  <testcase classname="pkg.SuiteZ" name="ok"/>
                  <testcase classname="pkg.SuiteZ" name="paint"><failure message="red | blue\t mismatch">java.lang.AssertionError: pixels differ\n  at line 1</failure></testcase>
                  <testcase classname="pkg.SuiteZ" name="notRun"><skipped message="no server"/></testcase>
                </testsuite>
                """);
        write(root, "widgets/alpha.test", "TEST-A.xml", """
                <testsuites><testsuite name="pkg.SuiteA"><testcase classname="pkg.SuiteA" name="bad">
                <error type="java.lang.IllegalStateException" message="not ready">java.lang.IllegalStateException: no SWT image</error>
                </testcase></testsuite></testsuites>
                """);
        // Exclude arbitrary similarly named reports outside target/surefire-reports.
        Path ignored = root.resolve("widgets/zeta.test/other/TEST-X.xml");
        Files.createDirectories(ignored.getParent());
        Files.writeString(ignored, "malformed content", StandardCharsets.UTF_8);
        NativeTestFailureIndex.Report r = NativeTestFailureIndex.scan(root);
        eq(2, r.reportFiles()); eq(4, r.tests()); eq(1, r.passed());
        eq(1, r.failures()); eq(1, r.errors()); eq(1, r.skipped());
        eq(3, r.findings().size());
        yes(r.findings().getFirst().module().equals("widgets/alpha.test"));
        String rows = NativeTestFailureIndex.tsv(r);
        yes(rows.startsWith("module\tsuite\ttest\tstatus\tmessage"));
        yes(rows.contains("red / blue  mismatch"));
        yes(rows.contains("java.lang.AssertionError: pixels differ"));
        yes(NativeTestFailureIndex.markdown(r).contains("Native Nebula JUnit/Tycho source reports"));
        eq(rows, NativeTestFailureIndex.tsv(NativeTestFailureIndex.scan(root)));
    }

    private static void rejectsMalformedOrUntrustedXml(Path root) throws Exception {
        Path attack = write(root, "widgets/beta.test", "TEST-B.xml", """
                <!DOCTYPE testsuite [<!ENTITY payload SYSTEM "file:///etc/passwd">]>
                <testsuite><testcase classname="Malicious" name="x"><failure>&payload;</failure></testcase></testsuite>
                """);
        reject(() -> NativeTestFailureIndex.scan(root));
        Files.writeString(attack, "<testsuite><testcase", StandardCharsets.UTF_8);
        reject(() -> NativeTestFailureIndex.scan(root));
        Files.delete(attack);
        NativeTestFailureIndex.Report r = NativeTestFailureIndex.scan(root);
        eq(2, r.reportFiles());
    }

    private static void runsExactCliOutput(Path root) throws Exception {
        Path tsv = root.resolve("reports/index.tsv"), markdown = root.resolve("reports/index.md");
        NativeTestFailureIndex.main(new String[]{root.toString(), tsv.toString(), markdown.toString()});
        yes(Files.exists(tsv) && Files.exists(markdown));
        yes(Files.readString(tsv).contains("widgets/alpha.test"));
        yes(Files.readString(markdown).contains("failed: 1"));
    }

    private interface Unsafe { void run() throws Exception; }
    private static void reject(Unsafe task) {
        try { task.run(); throw new AssertionError("Expected fail-closed rejection"); }
        catch (Exception expected) { checks++; }
    }
    private static void yes(boolean condition) { checks++; if (!condition) throw new AssertionError("check #" + checks); }
    private static void eq(int expected, int actual) { yes(expected == actual); }
    private static void eq(String expected, String actual) { yes(expected.equals(actual)); }
}
