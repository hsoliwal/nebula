/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Diagnostic-only, fail-closed report index for the original Nebula Surefire/Tycho
 * reactor. Parses existing XML without modifying it, adding or disabling tests,
 * changing Maven flags, or claiming native paint acceptance. Pure JDK21.
 *
 * <p>The output is a deterministic inventory and symptom grouping, NOT a
 * source-level root-cause diagnosis or a pass gate. Any failed XML testcase
 * remains a failure in the unchanged native Maven reactor.</p>
 */
public final class NativeFailureIndex {
    private static final int MAX_REPORTS = 20_000;
    private static final long MAX_REPORT_BYTES = 32L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 512L * 1024 * 1024;
    private static final int MAX_VALUE = 512;

    public record Summary(int reports, int tests, int failed, int errors, int skipped, int modules) { }
    private record Case(String module, String source, String suite, String test,
            String status, String type, String message, String symptom) { }
    private record Report(String module, String path, String sha256, int tests,
            int failures, int errors, int skipped) { }

    private NativeFailureIndex() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java NativeFailureIndex.java <repo-root> <output-dir>");
        }
        Summary summary = index(Path.of(args[0]), Path.of(args[1]));
        System.out.printf("NEBULA_NATIVE_REPORT_INDEX reports=%d tests=%d failures=%d errors=%d skipped=%d modules=%d status=%s%n",
                summary.reports(), summary.tests(), summary.failed(), summary.errors(),
                summary.skipped(), summary.modules(),
                summary.failed() == 0 && summary.errors() == 0 ? "NO_REPORTED_FAILURES" : "NATIVE_FAILURES_PRESENT");
    }

    /** Native code/test owners are never modified; only the destination is written. */
    public static Summary index(Path root, Path destination) throws Exception {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(destination, "destination");
        root = root.toAbsolutePath().normalize();
        destination = destination.toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || Files.isSymbolicLink(root)) {
            throw new IOException("Repository root missing or linked: " + root);
        }
        if (destination.startsWith(root)) {
            throw new IllegalArgumentException("Evidence directory must not be inside the scanned repository");
        }
        Path widgets = root.resolve("widgets");
        if (!Files.isDirectory(widgets) || Files.isSymbolicLink(widgets)) {
            throw new IOException("Missing original widgets tree: " + widgets);
        }
        List<Path> paths = new ArrayList<>();
        Files.walkFileTree(widgets, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
                if (path.getFileName().toString().startsWith("TEST-")
                        && path.getFileName().toString().endsWith(".xml")
                        && path.getParent() != null
                        && "surefire-reports".equals(path.getParent().getFileName().toString())) {
                    if (!attributes.isRegularFile() || attributes.isSymbolicLink() || Files.isSymbolicLink(path)) {
                        throw new IOException("Linked/irregular test result refused: " + path);
                    }
                    if (attributes.size() > MAX_REPORT_BYTES) throw new IOException("Oversized Surefire report: " + path);
                    if (paths.size() == MAX_REPORTS) throw new IOException("Too many Surefire reports");
                    paths.add(path);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        final Path repoRoot = root;
        paths.sort(Comparator.comparing(path -> repoRoot.relativize(path).toString().replace('\\', '/')));
        if (paths.isEmpty()) throw new IOException("No original Surefire XML reports found");
        List<Case> cases = new ArrayList<>();
        List<Report> reports = new ArrayList<>();
        long bytes = 0;
        for (Path report : paths) {
            bytes += Files.size(report);
            if (bytes > MAX_TOTAL_BYTES) throw new IOException("Aggregate Surefire evidence exceeds limit");
            String relative = root.relativize(report).toString().replace('\\', '/');
            String module = module(relative);
            Element suite = xml(report);
            NodeList entries = suite.getElementsByTagName("testcase");
            int failures = 0, errors = 0, skips = 0;
            for (int i = 0; i < entries.getLength(); i++) {
                Element element = (Element) entries.item(i);
                Element failure = firstChild(element, "failure");
                Element error = firstChild(element, "error");
                Element skipped = firstChild(element, "skipped");
                if ((failure != null ? 1 : 0) + (error != null ? 1 : 0) + (skipped != null ? 1 : 0) > 1) {
                    throw new IOException("Contradictory testcase outcome: " + relative);
                }
                String state = failure != null ? "FAILURE" : error != null ? "ERROR" : skipped != null ? "SKIPPED" : "PASSED";
                if (failure != null) failures++;
                if (error != null) errors++;
                if (skipped != null) skips++;
                Element detail = failure != null ? failure : error != null ? error : skipped;
                String suiteName = field(element, "classname", field(suite, "name", "UNKNOWN"));
                String testName = field(element, "name", "UNKNOWN");
                String type = detail == null ? "" : field(detail, "type", "");
                String message = detail == null ? "" : field(detail, "message", "");
                if ((state.equals("FAILURE") || state.equals("ERROR")) && message.isBlank()) {
                    message = detail == null ? "" : shortText(detail.getTextContent());
                }
                cases.add(new Case(module, relative, suiteName, testName, state, type, message,
                        symptom(suiteName, testName, type, message)));
            }
            int count = entries.getLength();
            // Tycho may supply nested testsuites or a report with no testcase nodes.
            // Refuse a report claiming tests but carrying none of their identities.
            if (count == 0 && parseCount(suite.getAttribute("tests")) > 0) {
                throw new IOException("Surefire XML claims tests without testcase identities: " + relative);
            }
            reports.add(new Report(module, relative, digest(report), count, failures, errors, skips));
        }
        Files.createDirectories(destination);
        write(destination.resolve("reports.tsv"), "module\treport_path\tsha256\ttests\tfailures\terrors\tskipped\n",
                reports.stream().map(x -> row(x.module(), x.path(), x.sha256(), x.tests(), x.failures(),
                        x.errors(), x.skipped())).toList());
        write(destination.resolve("cases.tsv"),
                "module\treport_path\tsuite\ttest\tstatus\ttype\tmessage\tsymptom\n",
                cases.stream().map(x -> row(x.module(), x.source(), x.suite(), x.test(),
                        x.status(), x.type(), x.message(), x.symptom())).toList());
        int failures = 0, errors = 0, skipped = 0;
        for (Report r : reports) { failures += r.failures(); errors += r.errors(); skipped += r.skipped(); }
        Map<String, Integer> groups = new TreeMap<>(), failingModules = new TreeMap<>();
        for (Case c : cases) {
            if ("FAILURE".equals(c.status()) || "ERROR".equals(c.status())) {
                groups.merge(c.symptom(), 1, Integer::sum);
                failingModules.merge(c.module(), 1, Integer::sum);
            }
        }
        StringBuilder markdown = new StringBuilder("### Original Nebula native Surefire evidence\n\n");
        markdown.append("Test run result is owned by the unchanged Tycho/Maven job; this index is diagnostics only.\n\n")
                .append("| Reports | Testcases | Failures | Errors | Skips | Failing modules |\n")
                .append("|---:|---:|---:|---:|---:|---:|\n|")
                .append(reports.size()).append('|').append(cases.size()).append('|').append(failures)
                .append('|').append(errors).append('|').append(skipped).append('|')
                .append(failingModules.size()).append("|\n\n");
        markdown.append("**Failure symptom groups (heuristic, not root-cause attribution):**\n\n");
        for (var entry : groups.entrySet()) {
            markdown.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        markdown.append("\n**Modules reporting failures/errors:**\n\n");
        for (var entry : failingModules.entrySet()) {
            markdown.append("- `").append(entry.getKey()).append("`: ").append(entry.getValue()).append("\n");
        }
        markdown.append("\nSource XML byte identities and individual test method details are in `reports.tsv` and `cases.tsv`.\n");
        Files.writeString(destination.resolve("summary.md"), markdown.toString(), StandardCharsets.UTF_8);
        return new Summary(reports.size(), cases.size(), failures, errors, skipped, failingModules.size());
    }

    private static int parseCount(String raw) { try { return Integer.parseInt(raw); } catch (NumberFormatException ignored) { return 0; } }
    private static String field(Element element, String key, String fallback) {
        String v = element.getAttribute(key);
        return v == null || v.isEmpty() ? fallback : shortText(v);
    }
    private static Element firstChild(Element element, String name) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) return (Element) node;
        }
        return null;
    }
    private static Element xml(Path file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void fatalError(SAXParseException error) throws SAXException { throw error; }
                @Override public void error(SAXParseException error) throws SAXException { throw error; }
            });
            try (InputStream in = Files.newInputStream(file)) {
                Element result = builder.parse(in).getDocumentElement();
                if (!"testsuite".equals(result.getNodeName()) && !"testsuites".equals(result.getNodeName())) {
                    throw new IOException("Not a Surefire XML test suite: " + file);
                }
                return result;
            }
        } catch (ParserConfigurationException | SAXException exception) {
            throw new IOException("Invalid/untrusted Surefire XML: " + file, exception);
        }
    }
    private static String digest(Path file) throws IOException {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                for (int n; (n = input.read(buffer)) >= 0;) if (n > 0) sha.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(sha.digest());
        } catch (NoSuchAlgorithmException e) { throw new AssertionError("JDK SHA-256 missing", e); }
    }
    private static String shortText(String value) {
        String cleaned = value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').replace('|', '/').strip();
        return cleaned.length() > MAX_VALUE ? cleaned.substring(0, MAX_VALUE) : cleaned;
    }
    private static String module(String path) {
        int end = path.indexOf("/target/surefire-reports/");
        if (end < 0 || !path.startsWith("widgets/")) throw new IllegalArgumentException("Invalid report location: " + path);
        return path.substring(0, end);
    }
    private static String symptom(String suite, String test, String type, String message) {
        String t = (suite + " " + test + " " + type + " " + message).toLowerCase(java.util.Locale.ROOT);
        if (t.contains("pixel") || t.contains("rgb") || t.contains("screen") || t.contains("colour") || t.contains("color")) return "native-pixel-comparison";
        if (t.contains("paint") || t.contains("gc") || t.contains("clip") || t.contains("redraw")) return "paint-clipping-or-lifetime";
        if (t.contains("popup") || t.contains("focus") || t.contains("selection") || t.contains("click")) return "interaction-or-focus";
        if (t.contains("state") || t.contains("scroll") || t.contains("identity")) return "viewport-state";
        if (t.contains("envelope") || t.contains("column") || t.contains("width") || t.contains("layout")) return "geometry-or-layout";
        return "other-or-unclassified";
    }
    private static String row(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) { if (i > 0) sb.append('\t'); sb.append(shortText(String.valueOf(values[i]))); }
        return sb.append('\n').toString();
    }
    private static void write(Path path, String header, List<String> rows) throws IOException {
        StringBuilder out = new StringBuilder(header);
        for (String line : rows) out.append(line);
        Files.writeString(path, out, StandardCharsets.UTF_8);
    }
}
