/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Native Nebula/Tycho test-report census. It is a DIAGNOSTIC, not a test gate:
 * failures remain fatal in Maven. This tool must never turn failing or skipped
 * JUnit reports into a successful qualification. Requires only the JDK 21.
 */
public final class NativeSurefireCensus {
    private static final long MAX_REPORT_BYTES = 16L * 1024 * 1024;
    private static final int MAX_REPORTS = 20_000;
    private static final int MAX_TESTS = 1_000_000;

    public record Suite(String module, String name, int tests, int failures, int errors,
            int skipped, String relativePath) { }
    public record Case(String module, String suite, String method, String status, String message) { }
    public record Census(List<Suite> suites, List<Case> nonPassing,
            long tests, long failures, long errors, long skipped) { }

    private NativeSurefireCensus() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: NativeSurefireCensus <repository-root> <output-directory>");
        }
        Census result = collect(Path.of(args[0]), Path.of(args[1]));
        System.out.printf(Locale.ROOT,
                "NEBULA_NATIVE_CENSUS_RECORDED suites=%d tests=%d failures=%d errors=%d skipped=%d%n",
                result.suites().size(), result.tests(), result.failures(), result.errors(), result.skipped());
        if (result.failures() + result.errors() != 0) {
            System.out.println("NATIVE_FULL_REACTOR_FAILED: see native-cases.tsv; Maven remains the failing gate");
        }
        if (result.skipped() != 0) {
            System.out.println("NATIVE_SKIPS_RETAINED: skipped tests are recorded, never relabeled as passes");
        }
    }

    public static Census collect(Path sourceRoot, Path out) throws Exception {
        Path root = sourceRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) throw new IOException("not a repository directory: " + root);
        List<Path> reports;
        try (var walk = Files.walk(root)) {
            reports = walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("TEST-")
                            && p.getFileName().toString().endsWith(".xml"))
                    .filter(p -> p.getParent().getFileName().toString().equals("surefire-reports"))
                    .limit(MAX_REPORTS + 1L).sorted(Comparator.comparing(Path::toString)).toList();
        }
        if (reports.isEmpty() || reports.size() > MAX_REPORTS) {
            throw new IOException("no JUnit reports, or excessive report count: " + reports.size());
        }
        List<Suite> suites = new ArrayList<>();
        List<Case> failures = new ArrayList<>();
        long tests = 0, failed = 0, errors = 0, skipped = 0;
        for (Path report : reports) {
            Path relative = root.relativize(report);
            String reportPath = relative.toString().replace('\\', '/');
            int cut = reportPath.indexOf("/target/surefire-reports/");
            if (cut < 0) throw new IOException("unqualified JUnit report path: " + reportPath);
            String module = reportPath.substring(0, cut);
            Suite suite = readReport(report, reportPath, module, failures);
            suites.add(suite);
            tests += suite.tests(); failed += suite.failures();
            errors += suite.errors(); skipped += suite.skipped();
            if (tests > MAX_TESTS) throw new IOException("unexpectedly large number of tests");
        }
        suites.sort(Comparator.comparing(Suite::module).thenComparing(Suite::name).thenComparing(Suite::relativePath));
        failures.sort(Comparator.comparing(Case::module).thenComparing(Case::suite)
                .thenComparing(Case::method).thenComparing(Case::status));
        Path dir = out.toAbsolutePath().normalize();
        Files.createDirectories(dir);
        StringBuilder suiteTsv = new StringBuilder("module\tsuite\ttests\tfailures\terrors\tskipped\tpath\n");
        for (Suite s : suites) suiteTsv.append(tsv(s.module())).append('\t').append(tsv(s.name()))
                .append('\t').append(s.tests()).append('\t').append(s.failures()).append('\t')
                .append(s.errors()).append('\t').append(s.skipped()).append('\t')
                .append(tsv(s.relativePath())).append('\n');
        StringBuilder caseTsv = new StringBuilder("module\tsuite\ttest\tstatus\tmessage\n");
        for (Case c : failures) caseTsv.append(tsv(c.module())).append('\t').append(tsv(c.suite()))
                .append('\t').append(tsv(c.method())).append('\t').append(c.status())
                .append('\t').append(tsv(c.message())).append('\n');
        Map<String, long[]> modules = new TreeMap<>();
        for (Suite s : suites) {
            long[] tally = modules.computeIfAbsent(s.module(), unused -> new long[5]);
            tally[0]++; tally[1] += s.tests(); tally[2] += s.failures();
            tally[3] += s.errors(); tally[4] += s.skipped();
        }
        StringBuilder moduleTsv = new StringBuilder("module\tsuites\ttests\tfailures\terrors\tskipped\n");
        for (Map.Entry<String, long[]> e : modules.entrySet()) {
            moduleTsv.append(tsv(e.getKey()));
            for (long count : e.getValue()) moduleTsv.append('\t').append(count);
            moduleTsv.append('\n');
        }
        writeAtomically(dir.resolve("native-modules.tsv"), moduleTsv.toString());
        writeAtomically(dir.resolve("native-suites.tsv"), suiteTsv.toString());
        writeAtomically(dir.resolve("native-cases.tsv"), caseTsv.toString());
        return new Census(List.copyOf(suites), List.copyOf(failures), tests, failed, errors, skipped);
    }

    private static Suite readReport(Path source, String path, String module, List<Case> cases) throws Exception {
        if (Files.size(source) > MAX_REPORT_BYTES) throw new IOException("oversized JUnit XML report: " + path);
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setXMLResolver((publicID, systemID, baseURI, namespace) -> {
            throw new XMLStreamException("external entity prohibited: " + path);
        });
        String suiteName = null, testcaseName = null, testcaseSuite = null, caseStatus = null, caseMessage = null;
        int tests = -1, failed = -1, errors = -1, skipped = -1, countCases = 0;
        try (InputStream in = Files.newInputStream(source)) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) {
                        throw new IOException("DTD/entity not permitted: " + path);
                    }
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        switch (reader.getLocalName()) {
                            case "testsuite" -> {
                                if (tests != -1) throw new IOException("multiple nested test suites: " + path);
                                suiteName = attr(reader, "name", path);
                                tests = number(reader, "tests", path);
                                failed = number(reader, "failures", path);
                                errors = number(reader, "errors", path);
                                skipped = number(reader, "skipped", path);
                            }
                            case "testcase" -> {
                                if (testcaseName != null) throw new IOException("nested test case: " + path);
                                testcaseName = attr(reader, "name", path);
                                testcaseSuite = reader.getAttributeValue(null, "classname");
                                if (testcaseSuite == null || testcaseSuite.isBlank()) testcaseSuite = suiteName;
                                caseStatus = "PASS";
                                caseMessage = "";
                                countCases++;
                            }
                            case "failure", "error", "skipped" -> {
                                if (testcaseName != null) {
                                    String type = reader.getLocalName();
                                    caseStatus = switch (type) {
                                        case "failure" -> "FAILURE";
                                        case "error" -> "ERROR";
                                        default -> "SKIPPED";
                                    };
                                    String message = reader.getAttributeValue(null, "message");
                                    caseMessage = message == null ? "" : message;
                                }
                            }
                            default -> { }
                        }
                    } else if (event == XMLStreamConstants.END_ELEMENT
                            && reader.getLocalName().equals("testcase")) {
                        if (testcaseName != null && !"PASS".equals(caseStatus)) {
                            cases.add(new Case(module, testcaseSuite, testcaseName, caseStatus, caseMessage));
                        }
                        testcaseName = null;
                    }
                }
            } finally { reader.close(); }
        } catch (XMLStreamException e) { throw new IOException("malformed JUnit XML: " + path, e); }
        if (suiteName == null || tests < 0 || failed < 0 || errors < 0 || skipped < 0
                || failed + errors + skipped > tests || countCases > tests) {
            throw new IOException("inconsistent JUnit suite header: " + path);
        }
        return new Suite(module, suiteName, tests, failed, errors, skipped, path);
    }

    private static String attr(XMLStreamReader reader, String name, String path) throws IOException {
        String value = reader.getAttributeValue(null, name);
        if (value == null || value.isBlank() || value.length() > 1024) {
            throw new IOException("missing/oversized JUnit attribute " + name + ": " + path);
        }
        return value;
    }

    private static int number(XMLStreamReader reader, String name, String path) throws IOException {
        String value = reader.getAttributeValue(null, name);
        try { int n = Integer.parseInt(value); if (n >= 0) return n; }
        catch (RuntimeException ignored) { }
        throw new IOException("invalid JUnit test count " + name + ": " + path);
    }

    private static String tsv(String value) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder(Math.min(value.length(), 512));
        for (int i = 0; i < value.length() && result.length() < 500; i++) {
            char ch = value.charAt(i);
            result.append(ch == '\n' || ch == '\r' || ch == '\t' || Character.isISOControl(ch) ? ' ' : ch);
        }
        return result.toString();
    }

    private static void writeAtomically(Path target, String value) throws IOException {
        Path tmp = Files.createTempFile(target.getParent(), ".census-", ".tmp");
        try {
            Files.writeString(tmp, value, StandardCharsets.UTF_8);
            try { Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(tmp); }
    }
}
