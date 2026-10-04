// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import com.synexia.job.IProgressMonitor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Repo-wide deterministic mechanical scanner.
 *
 * <p>File-local analysis parallelizes on Java 21 virtual threads. Canonical publication remains
 * serial: results are sorted by path before duplicate promotion/action generation.</p>
 */
public final class RepoSupersetScanner {
  private static final Set<String> EXCLUDED_DIRS =
      Set.of(
          ".git",
          ".merge-preserved",
          ".desktop-reads",
          ".idea",
          ".gradle",
          ".settings",
          ".vscode",
          ".cache",
          ".m2",
          ".pytest_cache",
          ".tox",
          ".venv",
          "target",
          "build",
          "out",
          "dist",
          "node_modules",
          "chat-artifacts",
          "__pycache__");
  private static final Set<String> TEXT_EXTENSIONS =
      Set.of(
          ".java", ".xml", ".md", ".markdown", ".mdx", ".rst", ".adoc", ".asciidoc", ".org",
          ".txt", ".tsv", ".csv", ".json", ".jsonc", ".jsonl", ".ndjson", ".yaml", ".yml",
          ".properties", ".service", ".sh", ".bash", ".zsh", ".ps1", ".bat", ".cmd", ".py",
          ".pyi", ".js", ".jsx", ".mjs", ".cjs", ".ts", ".tsx", ".go", ".rs", ".c", ".h",
          ".cc", ".cpp", ".cxx", ".hh", ".hpp", ".hxx", ".cs", ".kt", ".kts", ".scala",
          ".groovy", ".gradle", ".rb", ".php", ".swift", ".sql", ".toml", ".ini", ".conf",
          ".cfg", ".config", ".cmake", ".mk", ".mod", ".sum", ".lock", ".args", ".factorypath",
          ".spdx", ".sha256", ".dict", ".dictionary", ".lex", ".lexicon", ".lst", ".avsc",
          ".dtd", ".gql", ".graphql", ".jsonschema", ".json-schema", ".proto", ".thrift", ".wsdl",
          ".xsd", ".schema", ".html", ".htm", ".xhtml", ".svg", ".css", ".scss", ".less", ".ftl",
          ".hbs", ".jinja", ".mustache", ".example", ".editorconfig", ".gitignore",
          ".gitattributes", ".gitmodules", ".dockerignore", ".npmrc", ".nvmrc", ".sed", ".awk",
          ".pl", ".pm", ".r", ".lua", ".clj", ".cljs", ".cljc", ".ex", ".exs", ".erl", ".hrl",
          ".dart", ".d", ".m", ".mm", ".vue", ".svelte", ".broken", ".marker",
          ".mindexdbprovider", ".py-version");
  private static final Set<String> TEXT_FILENAMES =
      Set.of(
          "readme", "license", "licence", "notice", "copying", "makefile", "gnumakefile",
          "cmakelists.txt", "meson.build", "dockerfile", "containerfile", "mvnw", "gradlew",
          ".editorconfig", ".gitignore", ".gitattributes", ".gitmodules", ".dockerignore",
          ".npmrc", ".nvmrc", ".python-version", "requirements.txt", "go.mod", "go.sum",
          "cargo.toml", "cargo.lock", "package.json", "package-lock.json",
          "pyproject.toml");

  private static final Pattern NULL_RETURN = Pattern.compile("(?m)\\breturn\\s+null\\s*;");
  private static final Pattern EMPTY_METHOD = Pattern.compile("\\)\\s*\\{\\s*\\}");

  private static final Pattern PACKAGE =
      Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_$][A-Za-z0-9_$.]*)\\s*;");
  private static final Pattern TYPE =
      Pattern.compile(
          "(?m)^\\s*(?:(?:public|protected|private|abstract|final|sealed|non-sealed|static)\\s+)*"
              + "(class|interface|enum|record|@interface)\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
  private static final Pattern METHOD =
      Pattern.compile(
          "^(public|protected)\\s+"
              + "(?:(?:static|final|abstract|synchronized|native|default|strictfp)\\s+)*"
              + "(?:<[^>{};]+>\\s+)?"
              + "([A-Za-z_$][A-Za-z0-9_$.<>\\[\\],?& ]*?)\\s+"
              + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\((.*)\\)"
              + "(?:\\s+throws\\s+[^;{]+)?\\s*[;{].*$");
  private static final Pattern CONSTRUCTOR =
      Pattern.compile(
          "^(public|protected)\\s+"
              + "(?:(?:strictfp)\\s+)*"
              + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\((.*)\\)"
              + "(?:\\s+throws\\s+[^;{]+)?\\s*\\{.*$");

  private static final Set<String> JAVA_KEYWORDS =
      Set.of(
          "abstract","assert","boolean","break","byte","case","catch","char","class","const",
          "continue","default","do","double","else","enum","extends","final","finally","float",
          "for","goto","if","implements","import","instanceof","int","interface","long","native",
          "new","package","private","protected","public","record","return","short","static",
          "strictfp","super","switch","synchronized","this","throw","throws","transient","try",
          "var","void","volatile","while","sealed","permits","non","yield","true","false","null");

  private RepoSupersetScanner() {}

  public record Config(
      long maxFileBytes,
      int maxFiles,
      long maxRetainedBytes,
      boolean includeNonJava,
      boolean includeGenerated) {
    public static final Config DEFAULT =
        new Config(64L * 1024L * 1024L, 1_000_000, 16L * 1024L * 1024L * 1024L, true, true);

    /** Source-compatible constructor retained for existing scanner clients. */
    public Config(
        long maxFileBytes, int maxFiles, boolean includeNonJava, boolean includeGenerated) {
      this(maxFileBytes, maxFiles, 16L * 1024L * 1024L * 1024L, includeNonJava, includeGenerated);
    }

    public Config {
      if (maxFileBytes < 1L || maxFiles < 1 || maxRetainedBytes < 1L) {
        throw new IllegalArgumentException("invalid scan config");
      }
    }
  }

  /** Bounded execution controls kept separate from source-admission semantics. */
  public record ExecutionPolicy(int maxInFlightTasks, int streamBufferBytes) {
    private static final int MIN_STREAM_BUFFER = 4 * 1024;
    private static final int MAX_STREAM_BUFFER = 1024 * 1024;
    private static final int MAX_IN_FLIGHT_TASKS = 4096;

    public ExecutionPolicy {
      if (maxInFlightTasks < 1 || maxInFlightTasks > MAX_IN_FLIGHT_TASKS) {
        throw new IllegalArgumentException("maxInFlightTasks");
      }
      if (streamBufferBytes < MIN_STREAM_BUFFER || streamBufferBytes > MAX_STREAM_BUFFER) {
        throw new IllegalArgumentException("streamBufferBytes");
      }
    }

    public static ExecutionPolicy defaults() {
      int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
      int inFlight = Math.max(2, Math.min(4, processors));
      return new ExecutionPolicy(inFlight, 64 * 1024);
    }
  }

  private record StreamDigest(String sha256, long byteSize, int firstUnsignedByte) {
    private StreamDigest {
      if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException("sha256");
      }
      if (byteSize < 0 || firstUnsignedByte < -1 || firstUnsignedByte > 255) {
        throw new IllegalArgumentException("stream digest");
      }
      if ((byteSize == 0) != (firstUnsignedByte == -1)) {
        throw new IllegalArgumentException("first byte");
      }
    }
  }

  public static RepoSupersetInventory scan(Path root) throws IOException {
    return scan(root, Config.DEFAULT, IProgressMonitor.noop());
  }

  public static RepoSupersetInventory scan(
      Path root, Config config, IProgressMonitor monitor) throws IOException {
    return scan(root, config, ExecutionPolicy.defaults(), monitor);
  }

  /** Scan with explicit bounded I/O scheduling while retaining the existing semantic config. */
  public static RepoSupersetInventory scan(
      Path root,
      Config config,
      ExecutionPolicy executionPolicy,
      IProgressMonitor monitor)
      throws IOException {
    Path checkedRoot = java.util.Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    Config checkedConfig = config == null ? Config.DEFAULT : config;
    ExecutionPolicy checkedExecution =
        executionPolicy == null ? ExecutionPolicy.defaults() : executionPolicy;
    IProgressMonitor progress = monitor == null ? IProgressMonitor.noop() : monitor;
    progress.beginTask("Repository superset inventory", IProgressMonitor.UNKNOWN);
    try {
      return scanAdmitted(checkedRoot, checkedConfig, checkedExecution, progress);
    } finally {
      progress.done();
    }
  }

  private static RepoSupersetInventory scanAdmitted(
      Path root, Config config, ExecutionPolicy executionPolicy, IProgressMonitor progress)
      throws IOException {
    RepositoryScanIo.Discovery discovery = RepositoryScanIo.discover(root, EXCLUDED_DIRS,
        path -> (config.includeNonJava() || path.toString().endsWith(".java"))
            && (config.includeGenerated() || !isGeneratedPath(path)),
        config.maxFiles(), config.maxRetainedBytes(), config.maxFileBytes(), progress);
    List<Path> paths = discovery.files();
    progress.setProgress(0L, paths.size());
    ArrayList<RepoFileRecord> records = new ArrayList<>(paths.size());
    long actualRetainedBytes = 0L;
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      int window = executionPolicy.maxInFlightTasks();
      for (int start = 0; start < paths.size(); start += window) {
        RepositoryScanIo.check(progress);
        int end = Math.min(paths.size(), Math.addExact(start, window));
        ArrayList<Future<RepoFileRecord>> futures = new ArrayList<>(end - start);
        try {
          for (int index = start; index < end; index++) {
            Path path = paths.get(index);
            futures.add(executor.submit(() -> scanFile(root, path, config,
                executionPolicy.streamBufferBytes(), progress)));
          }
          for (Future<RepoFileRecord> future : futures) {
            RepoFileRecord record = await(future, progress);
            if (record.sizeBytes() > config.maxRetainedBytes() - actualRetainedBytes)
              throw new IOException("repository byte total changed beyond configured maximum");
            actualRetainedBytes += record.sizeBytes();
            records.add(record);
            progress.worked(1L);
          }
        } catch (IOException | RuntimeException failure) {
          cancel(futures);
          throw failure;
        }
      }
    }
    RepositoryScanIo.check(progress);
    records.sort(java.util.Comparator.comparing(RepoFileRecord::path));
    ArrayList<RepoAction> actions = actions(records);
    for (RepositoryScanIo.Omission omission : discovery.omissions()) {
      actions.add(new RepoAction(RepoAction.Severity.INFO, "scan-omission", omission.path(),
          0, omission.reason()));
    }
    return RepoSupersetInventory.fromScan(records, actions);
  }

  private static RepoFileRecord scanFile(
      Path root,
      Path path,
      Config config,
      int streamBufferBytes,
      IProgressMonitor progress)
      throws IOException {
    RepositoryScanIo.check(progress);
    String relative = root.relativize(path).toString().replace('\\', '/');
    String module = module(relative);
    String extension = extension(relative);
    boolean textFile = isText(relative, extension);
    RepoFileCategory category = RepoFileCategory.classify(relative, extension, textFile);
    RepoLanguage language = RepoLanguage.fromExtension(extension, textFile);

    RepositoryScanIo.Snapshot snapshot = RepositoryScanIo.read(path, config.maxFileBytes(),
        Math.min(config.maxFileBytes(), Integer.MAX_VALUE - 8L), textFile, streamBufferBytes, progress);
    long size = snapshot.byteSize();
    String sha256 = snapshot.sha256();
    String text = textFile ? snapshot.text() : "";
    String binaryLogic = textFile ? null : legacyBinaryNormalizedToken(
        new StreamDigest(sha256, size, snapshot.firstUnsignedByte()));

    long lines = text.isEmpty() ? 0L : text.lines().count();
    String packageName = "";
    ArrayList<String> types = new ArrayList<>();
    ArrayList<RepoApiRecord> apis = new ArrayList<>();
    EnumSet<RepoSignal> signals = EnumSet.noneOf(RepoSignal.class);

    if (".java".equals(extension)) {
      Matcher packageMatcher = PACKAGE.matcher(text);
      if (packageMatcher.find()) packageName = packageMatcher.group(1);
      Matcher typeMatcher = TYPE.matcher(text);
      while (typeMatcher.find()) types.add(typeMatcher.group(2));
      apis.addAll(extractApis(relative, types, text));
      classifyJava(relative, text, types, signals);
    } else {
      classifyGeneral(relative, extension, text, signals);
    }
    classifyFileRoles(relative, language, signals);

    String structural = structuralHash(packageName, types, apis, extension, text);
    String logicSource = textFile ? text : binaryLogic;
    String normalizedLogic = normalizeLogic(logicSource);
    String logic = sha256(normalizedLogic.getBytes(StandardCharsets.UTF_8));
    long simHash = simHashOfNormalized(normalizedLogic);

    return new RepoFileRecord(
        relative,
        module,
        extension,
        category,
        language,
        size,
        lines,
        sha256,
        structural,
        logic,
        simHash,
        packageName,
        types,
        apis,
        signals);
  }

  private static List<RepoApiRecord> extractApis(
      String path, List<String> types, String source) {
    ArrayList<RepoApiRecord> result = new ArrayList<>();
    String owner = types.isEmpty() ? "(unknown)" : types.get(0);
    String[] lines = source.split("\\R", -1);
    StringBuilder declaration = new StringBuilder();
    int startLine = 0;
    int parenDepth = 0;
    boolean collecting = false;

    for (int index = 0; index < lines.length; index++) {
      String stripped = lines[index].strip();
      if (!collecting) {
        if (!(stripped.startsWith("public ") || stripped.startsWith("protected "))) continue;
        if (!stripped.contains("(")) continue;
        collecting = true;
        startLine = index + 1;
        declaration.setLength(0);
        parenDepth = 0;
      }
      if (declaration.length() != 0) declaration.append(' ');
      declaration.append(stripped);
      parenDepth += count(stripped, '(') - count(stripped, ')');
      if (parenDepth > 0 || !(stripped.contains("{") || stripped.endsWith(";"))) continue;

      String normalized = declaration.toString().replaceAll("\\s+", " ").strip();
      RepoApiRecord api = parseApi(path, owner, normalized, startLine);
      if (api != null) result.add(api);
      collecting = false;
    }
    return result;
  }

  private static RepoApiRecord parseApi(
      String path, String owner, String declaration, int line) {
    if (declaration.contains(" class ")
        || declaration.contains(" interface ")
        || declaration.contains(" enum ")
        || declaration.contains(" record ")) {
      return null;
    }
    Matcher method = METHOD.matcher(declaration);
    if (method.matches()) {
      return new RepoApiRecord(
          path,
          owner,
          method.group(1),
          "method",
          method.group(3),
          declaration,
          line);
    }
    Matcher constructor = CONSTRUCTOR.matcher(declaration);
    if (constructor.matches() && constructor.group(2).equals(owner)) {
      return new RepoApiRecord(
          path,
          owner,
          constructor.group(1),
          "constructor",
          constructor.group(2),
          declaration,
          line);
    }
    return null;
  }

  private static void classifyJava(
      String path, String text, List<String> types, EnumSet<RepoSignal> signals) {
    if (path.contains("/src/test/") || path.contains("/src/testFixtures/")) {
      signals.add(RepoSignal.TEST_SOURCE);
    } else if (path.contains("/src/main/")) {
      signals.add(RepoSignal.MAIN_SOURCE);
    }
    if (isGeneratedPath(Path.of(path))) signals.add(RepoSignal.GENERATED_SOURCE);

    for (String type : types) {
      String lower = type.toLowerCase(java.util.Locale.ROOT);
      if (type.startsWith("MIndex") || lower.contains("mindex")) signals.add(RepoSignal.MINDEX_OWNER);
      if (type.startsWith("Minex") || lower.contains("minex")) signals.add(RepoSignal.MINEX_OWNER);
    }
    classifyText(text, signals);
  }

  private static void classifyGeneral(
      String path, String extension, String text, EnumSet<RepoSignal> signals) {
    if ("pom.xml".equals(Path.of(path).getFileName().toString())) signals.add(RepoSignal.MAVEN_BUILD);
    if (path.startsWith(".github/workflows/")
        && (".yml".equals(extension) || ".yaml".equals(extension))) {
      signals.add(RepoSignal.GITHUB_WORKFLOW);
    }
    classifyText(text, signals);
  }

  private static void classifyText(String text, EnumSet<RepoSignal> signals) {
    if (text.contains("TODO")) signals.add(RepoSignal.TODO);
    if (text.contains("FIXME")) signals.add(RepoSignal.FIXME);
    if (text.contains("HACK")) signals.add(RepoSignal.HACK);
    if (text.contains("UnsupportedOperationException")) signals.add(RepoSignal.UNSUPPORTED_OPERATION);
    if (NULL_RETURN.matcher(text).find()) {
      signals.add(RepoSignal.RETURN_NULL);
    }
    if (EMPTY_METHOD.matcher(text).find()) {
      signals.add(RepoSignal.EMPTY_METHOD);
    }
    if (text.contains("not implemented")
        || text.contains("NOT_IMPLEMENTED")
        || text.contains("IMPLEMENT ME")
        || text.contains("placeholder implementation")) {
      signals.add(RepoSignal.MISSING_IMPLEMENTATION_MARKER);
    }
    String lower = text.toLowerCase(java.util.Locale.ROOT);
    if (lower.contains("net.jini") || lower.contains("jgdms") || lower.contains("jeri")) {
      signals.add(RepoSignal.JINI);
    }
    if (lower.contains("precompute") || lower.contains("precomputed")) {
      signals.add(RepoSignal.PRECOMPUTE);
    }
    if (hasIndexEvidence(lower)) {
      signals.add(RepoSignal.INDEX);
    }
    if (lower.contains("cache") || lower.contains("memoiz")) signals.add(RepoSignal.CACHE);
    if (lower.contains("serializable")
        || lower.contains("objectinputstream")
        || lower.contains("objectoutputstream")
        || lower.contains("serializer")
        || lower.contains("deserializer")) {
      signals.add(RepoSignal.SERIALIZATION);
    }
    if (lower.contains("jni") || lower.contains("jnabridge") || lower.contains("system.loadlibrary")) {
      signals.add(RepoSignal.JNI);
    }
  }

  /** Naming an MIndex owner alone does not establish an implemented index. */
  private static boolean hasIndexEvidence(String lower) {
    for (int offset = 0; offset < lower.length();) {
      if (!Character.isJavaIdentifierPart(lower.charAt(offset))) {
        offset++;
        continue;
      }
      boolean ownerName = false;
      boolean indexEvidence = false;
      do {
        ownerName |= lower.startsWith("mindex", offset);
        indexEvidence |= lower.startsWith("index", offset)
            || lower.startsWith("posting", offset) || lower.startsWith("inverted", offset);
        offset++;
      } while (offset < lower.length() && Character.isJavaIdentifierPart(lower.charAt(offset)));
      if (!ownerName && indexEvidence) return true;
    }
    return false;
  }

  private static ArrayList<RepoAction> actions(List<RepoFileRecord> records) {
    ArrayList<RepoAction> result = new ArrayList<>();
    for (RepoFileRecord file : records) {
      addSignalAction(result, file, RepoSignal.UNSUPPORTED_OPERATION, RepoAction.Severity.REVIEW,
          "api-coverage", "UnsupportedOperationException requires implementation/review");
      addSignalAction(result, file, RepoSignal.MISSING_IMPLEMENTATION_MARKER, RepoAction.Severity.REVIEW,
          "api-coverage", "Explicit missing/placeholder implementation marker");
      addSignalAction(result, file, RepoSignal.EMPTY_METHOD, RepoAction.Severity.REVIEW,
          "stub", "Potential empty method body");
      addSignalAction(result, file, RepoSignal.RETURN_NULL, RepoAction.Severity.REVIEW,
          "null-semantics", "Potential null placeholder/result path");
      addSignalAction(result, file, RepoSignal.TODO, RepoAction.Severity.INFO,
          "todo", "TODO marker");
      addSignalAction(result, file, RepoSignal.FIXME, RepoAction.Severity.REVIEW,
          "fixme", "FIXME marker");
      addSignalAction(result, file, RepoSignal.HACK, RepoAction.Severity.REVIEW,
          "hack", "HACK marker");
    }

    return result;
  }

  private static void addSignalAction(
      List<RepoAction> target,
      RepoFileRecord file,
      RepoSignal signal,
      RepoAction.Severity severity,
      String category,
      String detail) {
    if (file.signals().contains(signal)) {
      target.add(new RepoAction(severity, category, file.path(), 0, detail));
    }
  }

  private static String structuralHash(
      String packageName,
      List<String> types,
      List<RepoApiRecord> apis,
      String extension,
      String text) {
    MessageDigest digest = digest();
    update(digest, "RepoStructural/v1");
    update(digest, extension);
    update(digest, packageName);
    ArrayList<String> sortedTypes = new ArrayList<>(types);
    sortedTypes.sort(String::compareTo);
    for (String type : sortedTypes) update(digest, type);
    ArrayList<String> signatures = new ArrayList<>();
    for (RepoApiRecord api : apis) signatures.add(api.kind() + ":" + api.signature());
    signatures.sort(String::compareTo);
    for (String signature : signatures) update(digest, signature);
    if (types.isEmpty() && apis.isEmpty() && !text.isEmpty()) {
      update(digest, normalizeWhitespace(text));
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  static String normalizeLogic(String text) {
    StringBuilder output = new StringBuilder(text.length());
    int index = 0;
    boolean space = false;
    while (index < text.length()) {
      char ch = text.charAt(index);
      if (ch == '/' && index + 1 < text.length() && text.charAt(index + 1) == '/') {
        index += 2;
        while (index < text.length() && text.charAt(index) != '\n') index++;
        space = true;
        continue;
      }
      if (ch == '/' && index + 1 < text.length() && text.charAt(index + 1) == '*') {
        index += 2;
        while (index + 1 < text.length()
            && !(text.charAt(index) == '*' && text.charAt(index + 1) == '/')) index++;
        index = Math.min(text.length(), index + 2);
        space = true;
        continue;
      }
      if (ch == '"' || ch == '\'') {
        char quote = ch;
        if (space && output.length() != 0) output.append(' ');
        space = false;
        output.append(quote == '"' ? "STR" : "CHR");
        index++;
        boolean escaped = false;
        while (index < text.length()) {
          char current = text.charAt(index++);
          if (escaped) {
            escaped = false;
          } else if (current == '\\') {
            escaped = true;
          } else if (current == quote) {
            break;
          }
        }
        continue;
      }
      if (Character.isJavaIdentifierStart(ch)) {
        if (space && output.length() != 0) output.append(' ');
        space = false;
        int start = index++;
        while (index < text.length() && Character.isJavaIdentifierPart(text.charAt(index))) index++;
        String token = text.substring(start, index);
        output.append(JAVA_KEYWORDS.contains(token) ? token : "_");
        continue;
      }
      if (Character.isDigit(ch)) {
        if (space && output.length() != 0) output.append(' ');
        space = false;
        output.append('#');
        index++;
        while (index < text.length()) {
          char current = text.charAt(index);
          if (!(Character.isDigit(current)
              || Character.isLetter(current)
              || current == '.'
              || current == '_'
              || current == '+' || current == '-')) break;
          index++;
        }
        continue;
      }
      if (Character.isWhitespace(ch)) {
        space = true;
        index++;
        continue;
      }
      if (space && output.length() != 0) output.append(' ');
      space = false;
      output.append(ch);
      index++;
    }
    return output.toString().strip();
  }

  static long simHash64(String text) {
    return simHashOfNormalized(normalizeLogic(text));
  }

  private static long simHashOfNormalized(String normalized) {
    if (normalized.isEmpty()) return 0L;
    int[] weights = new int[Long.SIZE];
    for (String token : normalized.split("\\s+")) {
      if (token.isEmpty()) continue;
      long hash = fnv1a64(token);
      for (int bit = 0; bit < Long.SIZE; bit++) {
        weights[bit] += ((hash >>> bit) & 1L) == 0L ? -1 : 1;
      }
    }
    long result = 0L;
    for (int bit = 0; bit < Long.SIZE; bit++) {
      if (weights[bit] >= 0) result |= 1L << bit;
    }
    return result;
  }

  private static long fnv1a64(String value) {
    long hash = 0xcbf29ce484222325L;
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    for (byte current : bytes) {
      hash ^= current & 0xffL;
      hash *= 0x100000001b3L;
    }
    return hash;
  }

  private static int count(String value, char target) {
    int result = 0;
    for (int index = 0; index < value.length(); index++) if (value.charAt(index) == target) result++;
    return result;
  }

  private static String module(String relative) {
    int slash = relative.indexOf('/');
    return slash < 0 ? "(root)" : relative.substring(0, slash);
  }

  private static String extension(String relative) {
    String name = Path.of(relative).getFileName().toString();
    if ("pom.xml".equals(name)) return ".xml";
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot).toLowerCase(java.util.Locale.ROOT);
  }

  private static boolean isText(String relative, String extension) {
    if (TEXT_EXTENSIONS.contains(extension)) return true;
    String name = Path.of(relative).getFileName().toString().toLowerCase(java.util.Locale.ROOT);
    return TEXT_FILENAMES.contains(name)
        || name.startsWith("readme.")
        || name.startsWith("license.")
        || name.startsWith("licence.")
        || name.startsWith("notice.")
        || name.startsWith("changelog.");
  }

  private static void classifyFileRoles(
      String path, RepoLanguage language, EnumSet<RepoSignal> signals) {
    String normalized = path.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
    if (isExamplePath(normalized)) signals.add(RepoSignal.EXAMPLE_FILE);
    if (!language.isSource()) return;
    if (isTestPath(normalized)) signals.add(RepoSignal.TEST_SOURCE);
    else if (("/" + normalized).contains("/src/main/")) signals.add(RepoSignal.MAIN_SOURCE);
    if (isGeneratedPath(Path.of(path))) signals.add(RepoSignal.GENERATED_SOURCE);
  }

  private static boolean isTestPath(String path) {
    String framed = "/" + path + "/";
    if (framed.contains("/src/test/")
        || framed.contains("/src/it/")
        || framed.contains("/test/")
        || framed.contains("/tests/")
        || framed.contains("/testfixtures/")) {
      return true;
    }
    String name = Path.of(path).getFileName().toString().toLowerCase(java.util.Locale.ROOT);
    int dot = name.lastIndexOf('.');
    String stem = dot < 0 ? name : name.substring(0, dot);
    return stem.endsWith(".test") || stem.endsWith(".spec");
  }

  private static boolean isExamplePath(String path) {
    String framed = "/" + path + "/";
    return framed.contains("/example/")
        || framed.contains("/examples/")
        || framed.contains("/sample/")
        || framed.contains("/samples/")
        || framed.contains("/demo/")
        || framed.contains("/demos/");
  }

  private static boolean isGeneratedPath(Path relative) {
    String value = relative.toString().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
    return value.contains("/generated/")
        || value.contains("/generated-sources/")
        || value.contains("/codegen/")
        || value.contains(".generated.")
        || value.startsWith("generated/")
        || value.startsWith("codegen/");
  }

  private static String normalizeWhitespace(String value) {
    return value.replaceAll("\\s+", " ").strip();
  }

  private static RepoFileRecord await(
      Future<RepoFileRecord> future,
      IProgressMonitor progress)
      throws IOException {
    while (true) {
      RepositoryScanIo.check(progress);
      try {
        return future.get(100L, TimeUnit.MILLISECONDS);
      } catch (TimeoutException waiting) {
        // Periodic cancellation check keeps the monitor authoritative while I/O is in flight.
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new IOException("repository inventory interrupted", interrupted);
      } catch (ExecutionException failure) {
        Throwable cause = failure.getCause();
        if (cause instanceof IOException io) throw io;
        if (cause instanceof RuntimeException runtime) throw runtime;
        throw new IOException("repository inventory worker failed", cause);
      }
    }
  }

  private static void cancel(List<? extends Future<?>> futures) {
    futures.forEach(future -> future.cancel(true));
  }

  /**
   * Preserve the previous full-hex logic/SimHash result without materializing hexadecimal text.
   */
  private static String legacyBinaryNormalizedToken(StreamDigest evidence) {
    if (evidence.byteSize() == 0) return "";
    int highNibble = evidence.firstUnsignedByte() >>> 4;
    return highNibble <= 9 ? "#" : "_";
  }

  private static String sha256(byte[] bytes) {
    return HexFormat.of().formatHex(digest().digest(bytes));
  }

  private static MessageDigest digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static void update(MessageDigest digest, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    for (int shift = 24; shift >= 0; shift -= 8) digest.update((byte) (bytes.length >>> shift));
    digest.update(bytes);
  }
}
