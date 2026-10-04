// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import com.synexia.specgrep.DocumentationExtractor;
import com.synexia.job.IProgressMonitor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Bounded lexical scanner for a complete Synexia working tree.
 *
 * <p>The scanner does not compile or execute application code. It inventories Maven structure,
 * dependencies, Java APIs, exact file hashes and mechanically visible incompleteness/reuse signals.
 * Build outputs and VCS metadata are excluded; checked-in source/vendor/generated material remains
 * visible.</p>
 */
public final class RepositorySupersetScanner {

  public record Policy(
      int maxFiles,
      long maxTotalBytes,
      long maxTextBytes) {

    public Policy {
      if (maxFiles < 1 || maxFiles > 2_000_000) throw new IllegalArgumentException("maxFiles");
      if (maxTotalBytes < 1L || maxTotalBytes > 64L * 1024 * 1024 * 1024) {
        throw new IllegalArgumentException("maxTotalBytes");
      }
      if (maxTextBytes < 1L || maxTextBytes > 64L * 1024 * 1024) {
        throw new IllegalArgumentException("maxTextBytes");
      }
    }

    public static Policy defaults() {
      return new Policy(1_000_000, 16L * 1024 * 1024 * 1024, 8L * 1024 * 1024);
    }
  }

  private static final Set<String> SKIP_DIRECTORIES =
      Set.of(".git", "target", "build", ".gradle", "node_modules", ".idea", ".metadata");

  public RepositorySupersetInventory scan(Path repositoryRoot) throws Exception {
    return scan(repositoryRoot, Policy.defaults());
  }

  public RepositorySupersetInventory scan(Path repositoryRoot, Policy policy) throws Exception {
    return scan(repositoryRoot, policy, IProgressMonitor.noop());
  }

  /** Existing scanner with explicit progress; null retains the canonical no-op monitor. */
  public RepositorySupersetInventory scan(
      Path repositoryRoot, Policy policy, IProgressMonitor monitor) throws Exception {
    Objects.requireNonNull(policy, "policy");
    IProgressMonitor progress = Objects.requireNonNullElse(monitor, IProgressMonitor.noop());
    progress.beginTask("Repository structural inventory", IProgressMonitor.UNKNOWN);
    try {
      return scanAdmitted(repositoryRoot, policy, progress);
    } finally {
      progress.done();
    }
  }

  private RepositorySupersetInventory scanAdmitted(
      Path repositoryRoot, Policy policy, IProgressMonitor progress) throws Exception {
    Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot")
        .toAbsolutePath().normalize();
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) {
      throw new IllegalArgumentException("repositoryRoot must be a real directory: " + root);
    }

    RepositoryScanIo.Discovery discovery = RepositoryScanIo.discover(root, SKIP_DIRECTORIES,
        path -> true, policy.maxFiles(), policy.maxTotalBytes(), policy.maxTotalBytes(), progress);
    List<Path> discovered = discovery.files();
    ArrayList<RepositorySupersetInventory.Finding> findings = new ArrayList<>();
    for (RepositoryScanIo.Omission omission : discovery.omissions()) {
      findings.add(new RepositorySupersetInventory.Finding(
          omission.reason(), omission.path(), 0, "content not inventoried"));
    }
    progress.setProgress(0L, discovered.size());
    long actualBytes = 0L;

    DocumentationExtractor extractor = new DocumentationExtractor();
    ArrayList<RepositorySupersetInventory.FileRecord> files = new ArrayList<>();
    ArrayList<RepositorySupersetInventory.ApiFact> apiFacts = new ArrayList<>();
    Map<String, List<String>> javaBySha = new TreeMap<>();
    Map<String, RepositorySupersetInventory.FileRecord> filesByPath = new HashMap<>();

    for (Path file : discovered) {
      RepositoryScanIo.check(progress);
      String path = relative(root, file);
      long discoveredSize = Files.size(file);
      boolean captureText = discoveredSize <= policy.maxTextBytes() && isTextCandidate(path);
      RepositoryScanIo.Snapshot evidence = RepositoryScanIo.read(file,
          policy.maxTotalBytes() - actualBytes, policy.maxTextBytes(), captureText, 64 * 1024, progress);
      long size = evidence.byteSize();
      actualBytes += size;
      String content = evidence.text();
      boolean java = path.endsWith(".java");
      boolean test = isTestPath(path);
      String packageName = "";
      int classes = 0;
      int interfaces = 0;
      int methods = 0;
      SortedSet<String> roles = roles(path, content);

      if (java && content == null) {
        findings.add(new RepositorySupersetInventory.Finding(
            "JAVA_SOURCE_TOO_LARGE_FOR_LEXICAL_SCAN", path, 0,
            "bytes=" + size + " maxTextBytes=" + policy.maxTextBytes()));
      }
      if (java && content != null) {
        DocumentationExtractor.SemanticMapping mapping =
            extractor.extractContent(content);
        packageName = Objects.toString(mapping.packageName(), "");
        classes = mapping.classes().size();
        interfaces = mapping.interfaces().size();
        methods = mapping.methods().size();
        appendApi(apiFacts, path, mapping);
        appendSourceFindings(findings, path, content);
        javaBySha.computeIfAbsent(evidence.sha256(), ignored -> new ArrayList<>()).add(path);
      }

      RepositorySupersetInventory.FileRecord record = new RepositorySupersetInventory.FileRecord(
          path,
          size,
          evidence.sha256(),
          packageName,
          java,
          test,
          classes,
          interfaces,
          methods,
          roles);
      files.add(record);
      filesByPath.put(path, record);
      progress.worked(1L);
    }

    Map<String, PomModel> poms = new TreeMap<>();
    for (Path file : discovered) {
      RepositoryScanIo.check(progress);
      String path = relative(root, file);
      if (path.equals("pom.xml") || path.endsWith("/pom.xml")) {
        try {
          PomModel model = parsePom(root, file, filesByPath.get(path), policy, progress);
          poms.put(model.modulePath(), model);
        } catch (org.xml.sax.SAXException invalidXml) {
          // Historical and fixture POMs remain byte-inventoried even when they cannot
          // define a Maven module. The root model is required to define the reactor.
          if (path.equals("pom.xml")) {
            throw new IOException("REPOSITORY_INVENTORY_INVALID_ROOT_POM:" + path, invalidXml);
          }
          int line = invalidXml instanceof org.xml.sax.SAXParseException parse
              ? Math.max(0, parse.getLineNumber()) : 0;
          findings.add(new RepositorySupersetInventory.Finding(
              "INVALID_POM_XML", path, line, invalidXml.getMessage()));
        }
      }
    }
    if (!poms.containsKey(".")) {
      throw new IllegalArgumentException("repository root pom.xml is required");
    }

    Set<String> reactor = reactorClosure(root, poms, findings, progress);
    RepositoryModulePathIndex moduleOwnership =
        RepositoryModulePathIndex.ofPaths(poms.keySet());
    Map<String, ModuleCounts> counts = assignModuleCounts(moduleOwnership, files, progress);
    ArrayList<RepositorySupersetInventory.ModuleRecord> modules = new ArrayList<>();
    ArrayList<RepositorySupersetInventory.DependencyRecord> dependencies = new ArrayList<>();
    Map<String, List<String>> modulesByArtifact = new TreeMap<>();

    for (PomModel pom : poms.values()) {
      RepositoryScanIo.check(progress);
      ModuleCounts count = counts.getOrDefault(pom.modulePath(), new ModuleCounts());
      SortedSet<String> moduleRoles = count.roles;
      boolean registered = reactor.contains(pom.modulePath());
      modules.add(new RepositorySupersetInventory.ModuleRecord(
          pom.modulePath(),
          pom.groupId(),
          pom.artifactId(),
          pom.packaging(),
          registered,
          count.mainJava,
          count.testJava,
          count.resources,
          moduleRoles,
          pom.sha256()));
      modulesByArtifact.computeIfAbsent(pom.artifactId(), ignored -> new ArrayList<>())
          .add(pom.modulePath());
      dependencies.addAll(pom.dependencies());

      if (!registered) {
        findings.add(new RepositorySupersetInventory.Finding(
            "UNREGISTERED_MODULE_CANDIDATE", pom.modulePath(), 0, pom.artifactId()));
      }
      if (count.mainJava > 0 && count.testJava == 0) {
        findings.add(new RepositorySupersetInventory.Finding(
            "MODULE_WITHOUT_TEST_SOURCE", pom.modulePath(), 0,
            "mainJava=" + count.mainJava));
      }
    }

    modulesByArtifact.forEach((artifact, paths) -> {
      if (paths.size() > 1) {
        paths.stream().sorted().forEach(path ->
            findings.add(new RepositorySupersetInventory.Finding(
                "DUPLICATE_ARTIFACT_ID", path, 0, artifact + " -> " + String.join(",", paths))));
      }
    });

    int duplicateFiles = 0;
    for (Map.Entry<String, List<String>> entry : javaBySha.entrySet()) {
      RepositoryScanIo.check(progress);
      List<String> paths = entry.getValue().stream().sorted().toList();
      if (paths.size() < 2) continue;
      duplicateFiles += paths.size();
      String evidence = entry.getKey() + " -> " + String.join(",", paths);
      for (String path : paths) {
        findings.add(new RepositorySupersetInventory.Finding(
            "EXACT_DUPLICATE_JAVA", path, 0, evidence));
      }
    }

    modules.sort(Comparator.comparing(RepositorySupersetInventory.ModuleRecord::path));
    dependencies.sort(
        Comparator.comparing(RepositorySupersetInventory.DependencyRecord::modulePath)
            .thenComparing(RepositorySupersetInventory.DependencyRecord::groupId)
            .thenComparing(RepositorySupersetInventory.DependencyRecord::artifactId)
            .thenComparing(RepositorySupersetInventory.DependencyRecord::scope)
            .thenComparing(RepositorySupersetInventory.DependencyRecord::version));
    files.sort(Comparator.comparing(RepositorySupersetInventory.FileRecord::path));
    apiFacts.sort(
        Comparator.comparing(RepositorySupersetInventory.ApiFact::path)
            .thenComparing(RepositorySupersetInventory.ApiFact::kind)
            .thenComparing(RepositorySupersetInventory.ApiFact::name)
            .thenComparing(RepositorySupersetInventory.ApiFact::signature));
    findings.sort(
        Comparator.comparing(RepositorySupersetInventory.Finding::kind)
            .thenComparing(RepositorySupersetInventory.Finding::path)
            .thenComparingInt(RepositorySupersetInventory.Finding::line)
            .thenComparing(RepositorySupersetInventory.Finding::evidence));

    RepositorySupersetInventory.Summary summary = summary(
        files, modules, apiFacts, findings, actualBytes, duplicateFiles);
    RepositoryScanIo.check(progress);
    return new RepositorySupersetInventory(
        modules, dependencies, files, apiFacts, findings, summary);
  }

  private static Set<String> reactorClosure(
      Path root,
      Map<String, PomModel> poms,
      List<RepositorySupersetInventory.Finding> findings,
      IProgressMonitor progress) throws IOException {
    HashSet<String> reactor = new HashSet<>();
    ArrayDeque<String> queue = new ArrayDeque<>();
    reactor.add(".");
    queue.add(".");

    while (!queue.isEmpty()) {
      RepositoryScanIo.check(progress);
      String owner = queue.removeFirst();
      PomModel pom = poms.get(owner);
      if (pom == null) continue;
      HashSet<String> local = new HashSet<>();
      for (String declared : pom.modules()) {
        String resolved = resolveModulePath(root, owner, declared);
        if (!local.add(resolved)) {
          findings.add(new RepositorySupersetInventory.Finding(
              "DUPLICATE_REACTOR_DECLARATION", owner, 0, resolved));
          continue;
        }
        if (!poms.containsKey(resolved)) {
          findings.add(new RepositorySupersetInventory.Finding(
              "MISSING_REACTOR_MODULE", owner, 0, declared + " -> " + resolved));
          continue;
        }
        if (reactor.add(resolved)) queue.addLast(resolved);
      }
    }
    return Set.copyOf(reactor);
  }

  private static String resolveModulePath(Path root, String owner, String declared) {
    Path base = ".".equals(owner) ? root : root.resolve(owner);
    Path resolved = base.resolve(declared).normalize();
    if (!resolved.startsWith(root)) {
      throw new IllegalArgumentException("module path escapes repository: " + declared);
    }
    return relative(root, resolved);
  }

  private static Map<String, ModuleCounts> assignModuleCounts(
      RepositoryModulePathIndex ownership,
      List<RepositorySupersetInventory.FileRecord> files,
      IProgressMonitor progress) throws IOException {
    Map<String, ModuleCounts> result = new HashMap<>();
    ownership.modulePaths().forEach(path -> result.put(path, new ModuleCounts()));
    for (RepositorySupersetInventory.FileRecord file : files) {
      RepositoryScanIo.check(progress);
      String owner = ownership.ownerOf(file.path());
      ModuleCounts count = result.get(owner);
      count.roles.addAll(file.roles());
      if (file.javaSource()) {
        if (file.testSource()) count.testJava++;
        else count.mainJava++;
      }
      if (isResourcePath(file.path())) count.resources++;
    }
    return result;
  }

  private static boolean isResourcePath(String path) {
    return path.startsWith("src/main/resources/")
        || path.startsWith("src/test/resources/")
        || path.contains("/src/main/resources/")
        || path.contains("/src/test/resources/");
  }

  private static PomModel parsePom(Path root, Path pom,
      RepositorySupersetInventory.FileRecord expected, Policy policy,
      IProgressMonitor progress) throws Exception {
    RepositoryScanIo.Snapshot snapshot = RepositoryScanIo.read(pom, expected.bytes(),
        policy.maxTextBytes(), true, 64 * 1024, progress);
    if (!snapshot.sha256().equals(expected.sha256()))
      throw new IOException("REPOSITORY_INVENTORY_POM_CHANGED:" + expected.path());
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);

    Element project;
    try (InputStream input = snapshot.input()) {
      var builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
        @Override public void error(org.xml.sax.SAXParseException failure)
            throws org.xml.sax.SAXException { throw failure; }
        @Override public void fatalError(org.xml.sax.SAXParseException failure)
            throws org.xml.sax.SAXException { throw failure; }
      });
      project = builder.parse(input).getDocumentElement();
    }
    if (!"project".equals(project.getTagName())) {
      throw new IllegalArgumentException("not a Maven project: " + pom);
    }

    String modulePath = relative(root, pom.getParent());
    Element parent = direct(project, "parent");
    String groupId = text(project, "groupId");
    if (groupId.isBlank() && parent != null) groupId = text(parent, "groupId");
    String artifactId = text(project, "artifactId");
    String packaging = text(project, "packaging");
    if (packaging.isBlank()) packaging = "jar";

    ArrayList<String> modules = new ArrayList<>();
    Element modulesNode = direct(project, "modules");
    if (modulesNode != null) {
      for (Element child : directChildren(modulesNode, "module")) {
        String value = child.getTextContent().strip();
        if (!value.isEmpty()) modules.add(value);
      }
    }

    ArrayList<RepositorySupersetInventory.DependencyRecord> dependencies = new ArrayList<>();
    Element dependenciesNode = direct(project, "dependencies");
    if (dependenciesNode != null) {
      for (Element dependency : directChildren(dependenciesNode, "dependency")) {
        String depArtifact = text(dependency, "artifactId");
        if (depArtifact.isBlank()) continue;
        dependencies.add(new RepositorySupersetInventory.DependencyRecord(
            modulePath,
            text(dependency, "groupId"),
            depArtifact,
            text(dependency, "version"),
            text(dependency, "scope"),
            Boolean.parseBoolean(text(dependency, "optional"))));
      }
    }

    return new PomModel(
        modulePath,
        groupId,
        artifactId,
        packaging,
        List.copyOf(modules),
        List.copyOf(dependencies),
        snapshot.sha256());
  }

  private static Element direct(Element parent, String name) {
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element && name.equals(element.getTagName())) return element;
    }
    return null;
  }

  private static List<Element> directChildren(Element parent, String name) {
    ArrayList<Element> result = new ArrayList<>();
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element && name.equals(element.getTagName())) result.add(element);
    }
    return List.copyOf(result);
  }

  private static String text(Element parent, String name) {
    Element child = direct(parent, name);
    return child == null ? "" : child.getTextContent().strip();
  }

  private static void appendApi(
      List<RepositorySupersetInventory.ApiFact> api,
      String path,
      DocumentationExtractor.SemanticMapping mapping) {
    String packageName = Objects.toString(mapping.packageName(), "");
    if (mapping.packagePurpose() != null && !mapping.packagePurpose().isBlank()) {
      api.add(new RepositorySupersetInventory.ApiFact(
          path, packageName, "PACKAGE", packageName, "", mapping.packagePurpose()));
    }
    mapping.classes().forEach(type -> api.add(new RepositorySupersetInventory.ApiFact(
        path, packageName, "CLASS", type.name(), "", type.purpose())));
    mapping.interfaces().forEach(type -> {
      api.add(new RepositorySupersetInventory.ApiFact(
          path, packageName, "INTERFACE", type.name(), "", type.contract()));
      type.methods().forEach(signature -> api.add(new RepositorySupersetInventory.ApiFact(
          path, packageName, "INTERFACE_METHOD", type.name(), signature, type.contract())));
    });
    mapping.methods().forEach(method -> api.add(new RepositorySupersetInventory.ApiFact(
        path,
        packageName,
        "METHOD",
        method.name(),
        methodSignature(method),
        method.description())));
  }

  private static String methodSignature(DocumentationExtractor.MethodMapping method) {
    String parameters = method.parameters().stream()
        .map(parameter -> parameter.type() + " " + parameter.name())
        .reduce((left, right) -> left + ", " + right)
        .orElse("");
    return method.returnType() + " " + method.name() + "(" + parameters + ")";
  }

  private static void appendSourceFindings(
      List<RepositorySupersetInventory.Finding> findings,
      String path,
      String text) {
    int line = 0;
    for (String raw : text.split("\\R", -1)) {
      line++;
      String trimmed = raw.strip();
      if (containsWord(trimmed, "TODO") || containsWord(trimmed, "FIXME") || containsWord(trimmed, "XXX")) {
        findings.add(new RepositorySupersetInventory.Finding(
            "PLACEHOLDER_MARKER", path, line, clip(trimmed)));
      }
      if (trimmed.contains("UnsupportedOperationException")
          || trimmed.contains("NotImplementedException")) {
        findings.add(new RepositorySupersetInventory.Finding(
            "UNSUPPORTED_OPERATION", path, line, clip(trimmed)));
      }
      if (trimmed.matches(".*\\breturn\\s+null\\s*;.*")) {
        findings.add(new RepositorySupersetInventory.Finding(
            "NULL_RETURN", path, line, clip(trimmed)));
      }
    }
  }

  private static boolean containsWord(String text, String word) {
    return text.matches(".*(?:^|[^A-Za-z0-9_])" + word + "(?:$|[^A-Za-z0-9_]).*");
  }

  private static String clip(String value) {
    return value.length() <= 240 ? value : value.substring(0, 240);
  }

  private static SortedSet<String> roles(String path, String content) {
    TreeSet<String> roles = new TreeSet<>();
    String lower = path.toLowerCase(Locale.ROOT);
    String text = Objects.toString(content, "");
    if (lower.endsWith(".java")) roles.add("JAVA");
    if (isTestPath(path)) roles.add("TEST");
    if (lower.endsWith("pom.xml")) roles.add("MAVEN");
    if (lower.startsWith(".github/workflows/") && (lower.endsWith(".yml") || lower.endsWith(".yaml"))) {
      roles.add("WORKFLOW");
    }
    if (lower.endsWith(".md") || lower.endsWith(".adoc")) roles.add("DOCUMENTATION");
    if (lower.contains("/resources/")) roles.add("RESOURCE");
    signal(roles, "MINDEX", text, path, "MIndex", "MatIndex", "IndexString");
    signal(roles, "INDEX", text, path, "Index", "index");
    signal(roles, "PRECOMPUTE", text, path, "Precompute", "precompute");
    signal(roles, "CACHE", text, path, "Cache", "cache");
    signal(roles, "ADAPTER", text, path, "Adapter", "adapter");
    signal(roles, "WRAPPER", text, path, "Wrapper", "wrapper");
    signal(roles, "SERIALIZATION", text, path,
        "Serializable", "ObjectInputStream", "ObjectOutputStream", "Codec", "serialize", "Json", "JSON");
    signal(roles, "JINI", text, path, "net.jini", "Jini", "JERI", "jgdms", "Apache River");
    signal(roles, "NATIVE", text, path, "JNI", "JNA", "native");
    signal(roles, "PROGRESS_MONITOR", text, path, "IProgressMonitor", "ProgressMonitor");
    return java.util.Collections.unmodifiableSortedSet(roles);
  }

  private static void signal(
      Set<String> roles, String role, String text, String path, String... needles) {
    for (String needle : needles) {
      if (text.contains(needle) || path.contains(needle)) {
        roles.add(role);
        return;
      }
    }
  }

  private static RepositorySupersetInventory.Summary summary(
      List<RepositorySupersetInventory.FileRecord> files,
      List<RepositorySupersetInventory.ModuleRecord> modules,
      List<RepositorySupersetInventory.ApiFact> api,
      List<RepositorySupersetInventory.Finding> findings,
      long bytes,
      int duplicateFiles) {
    return new RepositorySupersetInventory.Summary(
        files.size(),
        bytes,
        modules.size(),
        (int) modules.stream().filter(RepositorySupersetInventory.ModuleRecord::reactorRegistered).count(),
        (int) files.stream().filter(RepositorySupersetInventory.FileRecord::javaSource).count(),
        (int) files.stream().filter(RepositorySupersetInventory.FileRecord::testSource).count(),
        api.size(),
        findings.size(),
        duplicateFiles,
        countFinding(findings, "PLACEHOLDER_MARKER"),
        countFinding(findings, "UNSUPPORTED_OPERATION"),
        countRole(files, "MINDEX"),
        countRole(files, "INDEX"),
        countRole(files, "PRECOMPUTE"),
        countRole(files, "CACHE"),
        countRole(files, "ADAPTER"),
        countRole(files, "SERIALIZATION"),
        countRole(files, "JINI"),
        countRole(files, "PROGRESS_MONITOR"));
  }

  private static int countFinding(
      List<RepositorySupersetInventory.Finding> findings, String kind) {
    return (int) findings.stream().filter(finding -> finding.kind().equals(kind)).count();
  }

  private static int countRole(
      List<RepositorySupersetInventory.FileRecord> files, String role) {
    return (int) files.stream().filter(file -> file.roles().contains(role)).count();
  }

  private static boolean isTextCandidate(String path) {
    String lower = path.toLowerCase(Locale.ROOT);
    String name = Path.of(path).getFileName().toString();
    return lower.endsWith(".java")
        || lower.endsWith(".xml")
        || lower.endsWith(".md")
        || lower.endsWith(".adoc")
        || lower.endsWith(".yml")
        || lower.endsWith(".yaml")
        || lower.endsWith(".properties")
        || lower.endsWith(".txt")
        || lower.endsWith(".json")
        || lower.endsWith(".csv")
        || lower.endsWith(".tsv")
        || lower.endsWith(".gradle")
        || lower.endsWith(".kts")
        || lower.endsWith(".sh")
        || lower.endsWith(".ps1")
        || lower.endsWith(".py")
        || lower.endsWith(".js")
        || lower.endsWith(".ts")
        || lower.endsWith(".html")
        || lower.endsWith(".css")
        || lower.endsWith(".sql")
        || lower.endsWith(".c")
        || lower.endsWith(".h")
        || lower.endsWith(".cpp")
        || lower.endsWith(".hpp")
        || name.equals("Dockerfile")
        || name.equals("Makefile")
        || name.equals(".gitmodules");
  }

  private static boolean isTestPath(String path) {
    return path.contains("/src/test/") || path.startsWith("src/test/");
  }

  private static String relative(Path root, Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    if (!normalized.startsWith(root)) throw new IllegalArgumentException("path outside repository");
    String value = root.relativize(normalized).toString().replace('\\', '/');
    return value.isEmpty() ? "." : value;
  }

  private record PomModel(
      String modulePath,
      String groupId,
      String artifactId,
      String packaging,
      List<String> modules,
      List<RepositorySupersetInventory.DependencyRecord> dependencies,
      String sha256) {}

  private static final class ModuleCounts {
    int mainJava;
    int testJava;
    int resources;
    final SortedSet<String> roles = new TreeSet<>();
  }
}
