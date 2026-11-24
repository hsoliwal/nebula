/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.nebula.releng;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Loss-preserving CI diagnostics for real JUnit4/Tycho native test failures.
 * This class is a report indexer, not a test runner or failure suppressor.
 * It parses only original surefire TEST-*.xml reports under widget reactors,
 * fails closed on absent/malformed XML, and emits deterministic TSV + Markdown.
 */
public final class NativeTestFailureIndex {
    public enum Status { FAILURE, ERROR, SKIPPED }

    public record Case(String module, String suite, String name, Status status,
            String message, String detail, String source) { }

    public record Report(int reportFiles, int tests, int passed, int failures,
            int errors, int skipped, List<Case> findings) {
        public Report { findings = List.copyOf(findings); }
    }

    private static final int MAX_REPORT_BYTES = 16 * 1024 * 1024;
    private static final Comparator<Case> ORDER = Comparator.comparing(Case::module)
            .thenComparing(Case::suite).thenComparing(Case::name)
            .thenComparing(Case::status).thenComparing(Case::source);

    private NativeTestFailureIndex() { }

    public static Report scan(Path reactorRoot) throws Exception {
        Objects.requireNonNull(reactorRoot, "reactorRoot");
        Path root = reactorRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) throw new IOException("widget reactor directory does not exist: " + root);
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(root)) {
            paths = walk.filter(Files::isRegularFile).filter(NativeTestFailureIndex::isReport)
                    .sorted().toList();
        }
        if (paths.isEmpty()) throw new IOException("No original JUnit TEST-*.xml reports under " + root);
        int count = 0, failed = 0, errors = 0, skipped = 0;
        List<Case> findings = new ArrayList<>();
        DocumentBuilderFactory parser = secureParser();
        for (Path path : paths) {
            if (Files.size(path) > MAX_REPORT_BYTES) throw new IOException("Oversized JUnit report: " + path);
            Element document;
            try (var input = Files.newInputStream(path)) {
                var builder = parser.newDocumentBuilder();
                builder.setErrorHandler(new DefaultHandler() {
                    @Override public void fatalError(SAXParseException e) throws SAXParseException {
                        throw e;
                    }
                });
                document = builder.parse(input).getDocumentElement();
            }
            String rootTag = document.getTagName();
            if (!("testsuite".equals(rootTag) || "testsuites".equals(rootTag))) {
                throw new IOException("Not a JUnit suite document: " + path);
            }
            String module = module(root.relativize(path));
            String source = root.relativize(path).toString().replace('\\', '/');
            NodeList tests = document.getElementsByTagName("testcase");
            for (int i = 0; i < tests.getLength(); i++) {
                Element test = (Element) tests.item(i);
                count++;
                String suite = test.getAttribute("classname");
                if (suite.isBlank()) suite = enclosingSuite(test);
                String name = test.getAttribute("name");
                NodeList children = test.getChildNodes();
                Status status = null;
                Element result = null;
                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (!(child instanceof Element tag)) continue;
                    Status candidate = switch (tag.getTagName()) {
                        case "failure" -> Status.FAILURE;
                        case "error" -> Status.ERROR;
                        case "skipped" -> Status.SKIPPED;
                        default -> null;
                    };
                    if (candidate != null) {
                        if (status != null) throw new IOException("Multiple test statuses: " + path + " " + name);
                        status = candidate;
                        result = tag;
                    }
                }
                if (status != null) {
                    switch (status) {
                        case FAILURE -> failed++;
                        case ERROR -> errors++;
                        case SKIPPED -> skipped++;
                    }
                    findings.add(new Case(module, suite, name, status,
                            clean(result.getAttribute("message"), 280),
                            clean(firstLine(result.getTextContent()), 280), source));
                }
            }
        }
        findings.sort(ORDER);
        return new Report(paths.size(), count, count - failed - errors - skipped,
                failed, errors, skipped, findings);
    }

    private static String enclosingSuite(Element test) {
        Node parent = test.getParentNode();
        while (parent != null) {
            if (parent instanceof Element element && "testsuite".equals(element.getTagName())) {
                return element.getAttribute("name");
            }
            parent = parent.getParentNode();
        }
        return "unknown";
    }

    private static boolean isReport(Path path) {
        String n = path.getFileName().toString();
        Path parent = path.getParent();
        return n.startsWith("TEST-") && n.endsWith(".xml") && parent != null
                && parent.getFileName().toString().equals("surefire-reports")
                && parent.getParent() != null
                && parent.getParent().getFileName().toString().equals("target");
    }

    private static String module(Path relative) {
        String p = relative.toString().replace('\\', '/');
        int target = p.indexOf("/target/surefire-reports/");
        return target < 0 ? "unknown" : p.substring(0, target);
    }

    private static DocumentBuilderFactory secureParser() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static String firstLine(String value) {
        if (value == null) return "";
        String text = value.strip();
        int line = text.indexOf('\n');
        return line < 0 ? text : text.substring(0, line).strip();
    }

    private static String clean(String value, int max) {
        if (value == null) return "";
        String cleaned = value.replace('\t', ' ').replace('\n', ' ')
                .replace('\r', ' ').replace('|', '/').strip();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max) + "...";
    }

    public static String tsv(Report report) {
        StringWriter out = new StringWriter();
        out.append("module\tsuite\ttest\tstatus\tmessage\tfirst_trace_line\treport_path\n");
        for (Case c : report.findings()) {
            out.append(c.module()).append('\t').append(c.suite()).append('\t')
                    .append(c.name()).append('\t').append(c.status().name()).append('\t')
                    .append(c.message()).append('\t').append(c.detail()).append('\t')
                    .append(c.source()).append('\n');
        }
        return out.toString();
    }

    public static String markdown(Report report) {
        StringBuilder out = new StringBuilder("### Native Nebula JUnit/Tycho source reports\n\n");
        out.append("Source reports: ").append(report.reportFiles())
                .append("; executed cases: ").append(report.tests())
                .append("; passed: ").append(report.passed())
                .append("; failed: ").append(report.failures())
                .append("; errors: ").append(report.errors())
                .append("; skipped: ").append(report.skipped()).append(".\n\n")
                .append("This diagnostic is **not** a substitute for native Tycho acceptance. ")
                .append("The original Maven job must remain red when any test fails.\n\n")
                .append("| Module | Test | Status | First error |\n")
                .append("|---|---|---|---|\n");
        int included = 0;
        for (Case c : report.findings()) {
            if (c.status() == Status.SKIPPED) continue;
            if (++included > 75) break;
            out.append('|').append(clean(c.module(), 80))
                    .append('|').append(clean(c.suite() + "." + c.name(), 100))
                    .append('|').append(c.status()).append('|')
                    .append(clean(c.message().isEmpty() ? c.detail() : c.message(), 140))
                    .append("|\n");
        }
        if (included == 0) out.append("| — | No failures found | — | — |\n");
        if (report.failures() + report.errors() > 75)
            out.append("\nAdditional failures appear in the full TSV artifact.\n");
        return out.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: NativeTestFailureIndex <widgets-root> <output.tsv> <summary.md>");
        Report report = scan(Path.of(args[0]));
        Path output = Path.of(args[1]);
        Path summary = Path.of(args[2]);
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        if (summary.getParent() != null) Files.createDirectories(summary.getParent());
        Files.writeString(output, tsv(report), StandardCharsets.UTF_8);
        Files.writeString(summary, markdown(report), StandardCharsets.UTF_8);
        System.out.printf("NEBULA_NATIVE_REPORT_INDEX reports=%d cases=%d passed=%d failures=%d errors=%d skipped=%d%n",
                report.reportFiles(), report.tests(), report.passed(), report.failures(), report.errors(), report.skipped());
        // Do not turn failures green: original Build All Widgets owns its failure status.
        // Do not hide incomplete reports: invalid or absent XML already throws.
    }
}
