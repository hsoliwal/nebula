// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import com.synexia.job.IProgressMonitor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Checks whether an inventory saw every tracked blob in one pinned Git tree, or recorded why it
 * intentionally skipped it. Git object IDs identify this checkout; the scanner's separate
 * SHA-256 lanes identify file bytes. This is a path-completeness gate, not a semantic/API proof.
 */
public final class RepositoryGitTreeCoverage {
  private static final int MAX_GIT_OUTPUT_BYTES = 256 * 1024 * 1024;
  private RepositoryGitTreeCoverage() {}

  /** Exact tracked blob identity at one pinned Git revision. */
  public record TrackedBlob(String path, String objectId) {
    public TrackedBlob {
      path = trackedPath(path);
      objectId = RepositoryGitTreeCoverage.objectId(objectId);
    }
  }

  /** Sorted immutable blob tree for one exact commit/tree pair. */
  public record BlobTree(String commit, String tree, List<TrackedBlob> blobs) {
    public BlobTree {
      commit = RepositoryGitTreeCoverage.objectId(commit);
      tree = RepositoryGitTreeCoverage.objectId(tree);
      blobs = List.copyOf(Objects.requireNonNull(blobs, "blobs"));
      String previous = null;
      for (TrackedBlob blob : blobs) {
        Objects.requireNonNull(blob, "blob");
        if (previous != null && previous.compareTo(blob.path()) >= 0) {
          throw new IllegalArgumentException("Git blob rows must be strictly path-sorted");
        }
        previous = blob.path();
      }
    }

    public List<TrackedBlob> javaBlobs() {
      return blobs.stream().filter(blob -> blob.path().endsWith(".java")).toList();
    }
  }

  public record Coverage(String commit, String tree, int trackedBlobs, int inventoriedBlobs,
      int acknowledgedOmissions, List<String> untrackedInventoriedPaths,
      List<String> missingTracked) {
    public Coverage {
      commit = objectId(commit);
      tree = objectId(tree);
      untrackedInventoriedPaths = List.copyOf(Objects.requireNonNull(
          untrackedInventoriedPaths, "untrackedInventoriedPaths"));
      missingTracked = List.copyOf(Objects.requireNonNull(missingTracked, "missingTracked"));
      if (trackedBlobs < 0 || inventoriedBlobs < 0 || acknowledgedOmissions < 0
          || trackedBlobs != inventoriedBlobs + acknowledgedOmissions
              + missingTracked.size()) {
        throw new IllegalArgumentException("inconsistent Git inventory coverage");
      }
    }
    public int untrackedInventoried() { return untrackedInventoriedPaths.size(); }
    public boolean complete() {
      return missingTracked.isEmpty() && untrackedInventoriedPaths.isEmpty();
    }
  }

  /** Call before scanning to bind the later coverage check to the same HEAD. */
  public static String head(Path root, IProgressMonitor monitor) throws IOException {
    // Full-tree inventory coordinates must start at the worktree root, not a nested module.
    String prefix = new String(git(root, monitor, "rev-parse", "--show-prefix"),
        StandardCharsets.UTF_8).strip();
    if (!prefix.isEmpty()) {
      throw new IOException("Git tree inventory requires the worktree root");
    }
    // Status alone can hide tracked edits behind assume-unchanged or skip-worktree bits.
    // Do not clear those bits or expand a sparse checkout on the caller's behalf.
    byte[] tracked = git(root, monitor, "ls-files", "-v", "-z");
    for (int begin = 0, end; begin < tracked.length; begin = end + 1) {
      check(monitor);
      end = begin;
      while (end < tracked.length && tracked[end] != 0) {
        if ((end & 65535) == 0) check(monitor);
        end++;
      }
      if (end == tracked.length || end - begin < 3 || tracked[begin + 1] != ' ') {
        throw new IOException("invalid Git index visibility entry");
      }
      int tag = Byte.toUnsignedInt(tracked[begin]);
      if (tag == 'S' || (tag >= 'a' && tag <= 'z')) {
        throw new IOException("tracked index hides worktree verification: "
            + escape(decode(tracked, begin + 2, end - begin - 2)));
      }
    }
    // Ignore cached fsmonitor assertions for this status invocation only; leave config intact.
    if (git(root, monitor, "-c", "core.fsmonitor=false", "status",
        "--porcelain=v1", "--untracked-files=no", "-z").length != 0) {
      throw new IOException("tracked checkout contains modifications");
    }
    return objectId(new String(git(root, monitor, "rev-parse", "--verify", "HEAD"),
        StandardCharsets.UTF_8).strip());
  }

  /**
   * Returns exact tracked blobs for any pinned commit/revision without reading file bodies.
   *
   * <p>This is the byte-verbatim outer oracle: Git blob object IDs identify exact tracked bytes.
   * The caller can layer OpenRewrite/parser hashes above this evidence, but parser rendering never
   * replaces the Git object identity.</p>
   */
  public static BlobTree trackedBlobs(
      Path root, String revision, IProgressMonitor monitor) throws IOException {
    Objects.requireNonNull(root, "root");
    String prefix = new String(git(root, monitor, "rev-parse", "--show-prefix"),
        StandardCharsets.UTF_8).strip();
    if (!prefix.isEmpty()) {
      throw new IOException("Git blob inventory requires the worktree root");
    }

    String requested = Objects.requireNonNull(revision, "revision").strip();
    if (requested.isEmpty() || requested.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("revision");
    }
    String commit = objectId(new String(
        git(root, monitor, "rev-parse", "--verify", requested + "^{commit}"),
        StandardCharsets.UTF_8).strip());
    String tree = objectId(new String(
        git(root, monitor, "rev-parse", "--verify", commit + "^{tree}"),
        StandardCharsets.UTF_8).strip());

    byte[] raw = git(root, monitor, "ls-tree", "--full-tree", "-r", "-z", commit);
    ArrayList<TrackedBlob> blobs = new ArrayList<>();
    for (int begin = 0, end; begin < raw.length; begin = end + 1) {
      if ((blobs.size() & 1023) == 0) check(monitor);
      end = begin;
      while (end < raw.length && raw[end] != 0) end++;
      if (end == raw.length) throw new IOException("unterminated Git tree entry");

      String entry = decode(raw, begin, end - begin);
      int tab = entry.indexOf('\t');
      if (tab < 0) throw new IOException("invalid Git tree entry");
      String[] fields = entry.substring(0, tab).split(" ");
      if (fields.length != 3) throw new IOException("invalid Git tree header");
      if (!"blob".equals(fields[1])) continue;
      blobs.add(new TrackedBlob(entry.substring(tab + 1), fields[2]));
    }

    blobs.sort(Comparator.comparing(TrackedBlob::path));
    for (int index = 1; index < blobs.size(); index++) {
      if (blobs.get(index - 1).path().equals(blobs.get(index).path())) {
        throw new IOException("duplicate Git tree path: " + blobs.get(index).path());
      }
    }
    return new BlobTree(commit, tree, blobs);
  }

  /**
   * Omission values are the existing scanner's reasons (EXCLUDED_DIRECTORY, SYMLINK_FILE,
   * POLICY_FILTERED_FILE, etc.). Only EXCLUDED_DIRECTORY covers descendants; all other
   * omissions cover exactly their recorded path. Missing and extra paths are returned for review.
   */
  public static Coverage inspect(Path root, String expectedHead, Set<String> inventoriedPaths,
      Map<String, String> omissions, IProgressMonitor monitor) throws IOException {
    Objects.requireNonNull(root, "root");
    String pinned = objectId(expectedHead);
    Set<String> scanned = Set.copyOf(Objects.requireNonNull(inventoriedPaths, "inventoriedPaths"));
    Map<String, String> acknowledged = Map.copyOf(Objects.requireNonNull(omissions, "omissions"));
    if (!pinned.equals(head(root, monitor))) throw new IOException("repository HEAD changed during inventory");
    String tree = objectId(new String(git(root, monitor, "rev-parse", "--verify", pinned + "^{tree}"),
        StandardCharsets.UTF_8).strip());
    byte[] raw = git(root, monitor, "ls-tree", "--full-tree", "-r", "-z", pinned);
    Set<String> excludedDirectories = new HashSet<>();
    for (var omission : acknowledged.entrySet()) {
      if ("EXCLUDED_DIRECTORY".equals(omission.getValue())) {
        excludedDirectories.add(omission.getKey());
      }
    }
    ArrayList<String> missing = new ArrayList<>();
    Set<String> trackedScanned = new HashSet<>();
    int tracked = 0, present = 0, excluded = 0;
    for (int begin = 0, end; begin < raw.length; begin = end + 1) {
      if ((tracked & 1023) == 0) check(monitor);
      end = begin;
      while (end < raw.length && raw[end] != 0) end++;
      if (end == raw.length) throw new IOException("unterminated Git tree entry");
      String entry = decode(raw, begin, end - begin);
      int tab = entry.indexOf('\t');
      if (tab < 0) throw new IOException("invalid Git tree entry");
      String[] fields = entry.substring(0, tab).split(" ");
      if (fields.length != 3) throw new IOException("invalid Git tree header");
      if (!"blob".equals(fields[1])) continue; // Gitlinks are not file bytes.
      String path = entry.substring(tab + 1);
      if (path.isEmpty() || path.startsWith("/") || path.equals("..")
          || path.startsWith("../") || path.contains("/../")) {
        throw new IOException("invalid tracked Git path");
      }
      tracked++;
      if (scanned.contains(path)) {
        present++;
        trackedScanned.add(path);
      } else if (acknowledged.containsKey(path)
          || inExcludedDirectory(path, excludedDirectories)) {
        excluded++;
      } else missing.add(path);
    }
    if (!pinned.equals(head(root, monitor))) throw new IOException("repository HEAD changed during inventory");
    missing.sort(String::compareTo);
    ArrayList<String> untracked = new ArrayList<>(scanned);
    untracked.removeAll(trackedScanned);
    untracked.sort(String::compareTo);
    return new Coverage(pinned, tree, tracked, present, excluded, untracked, missing);
  }

  /** One bounded, deterministic receipt plus full lists of missing and extra paths. */
  public static void write(Coverage coverage, Path output) throws IOException {
    Objects.requireNonNull(coverage, "coverage");
    Path directory = Objects.requireNonNull(output, "output").toAbsolutePath().normalize();
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("GIT_TREE_COVERAGE.tsv"),
        "commit\ttree\ttracked_blobs\tinventoried_blobs\tacknowledged_omissions"
            + "\tuntracked_inventoried\tmissing_tracked\n"
            + coverage.commit() + "\t" + coverage.tree() + "\t" + coverage.trackedBlobs()
            + "\t" + coverage.inventoriedBlobs() + "\t" + coverage.acknowledgedOmissions()
            + "\t" + coverage.untrackedInventoried() + "\t"
            + coverage.missingTracked().size() + "\n", StandardCharsets.UTF_8);
    StringBuilder missing = new StringBuilder("path\n");
    for (String path : coverage.missingTracked()) {
      missing.append(escape(path)).append('\n');
    }
    Files.writeString(directory.resolve("GIT_TREE_MISSING.tsv"), missing, StandardCharsets.UTF_8);
    StringBuilder untracked = new StringBuilder("path\n");
    for (String path : coverage.untrackedInventoriedPaths()) {
      untracked.append(escape(path)).append('\n');
    }
    Files.writeString(directory.resolve("GIT_TREE_UNTRACKED.tsv"),
        untracked, StandardCharsets.UTF_8);
  }

  private static String escape(String path) {
    return path.replace("\\", "\\\\").replace("\t", "\\t")
        .replace("\r", "\\r").replace("\n", "\\n");
  }

  private static boolean inExcludedDirectory(String path, Set<String> excluded) {
    int slash = path.lastIndexOf('/');
    while (slash > 0) {
      if (excluded.contains(path.substring(0, slash))) return true;
      slash = path.lastIndexOf('/', slash - 1);
    }
    return false;
  }

  private static String decode(byte[] raw, int start, int length) throws IOException {
    try {
      return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(raw, start, length)).toString();
    } catch (CharacterCodingException invalid) {
      throw new IOException("tracked Git path is not UTF-8", invalid);
    }
  }

  private static String trackedPath(String value) {
    String path = Objects.requireNonNull(value, "path");
    if (path.isEmpty() || path.startsWith("/") || path.indexOf('\0') >= 0
        || path.equals("..") || path.startsWith("../") || path.contains("/../")) {
      throw new IllegalArgumentException("invalid tracked Git path");
    }
    return path;
  }

  private static String objectId(String id) {
    if (id == null || !(id.matches("[0-9a-f]{40}") || id.matches("[0-9a-f]{64}"))) {
      throw new IllegalArgumentException("invalid Git object ID");
    }
    return id;
  }

  private static byte[] git(Path root, IProgressMonitor monitor, String... command)
      throws IOException {
    check(monitor);
    String[] args = new String[command.length + 3];
    args[0] = "git"; args[1] = "-C";
    args[2] = root.toAbsolutePath().normalize().toString();
    System.arraycopy(command, 0, args, 3, command.length);
    Process process = new ProcessBuilder(args).redirectErrorStream(true).start();
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try (InputStream input = process.getInputStream()) {
        byte[] buffer = new byte[64 * 1024];
        for (int n; (n = input.read(buffer)) >= 0; ) {
          check(monitor);
          if (n > MAX_GIT_OUTPUT_BYTES - output.size()) {
            process.destroyForcibly();
            throw new IOException("Git tree output exceeds inventory bound");
          }
          output.write(buffer, 0, n);
        }
      }
      if (process.waitFor() != 0) throw new IOException("Git tree command failed");
      return output.toByteArray();
    } catch (InterruptedException interrupted) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw new InterruptedIOException("Git tree inventory interrupted");
    } finally {
      if (process.isAlive()) process.destroyForcibly();
    }
  }

  private static void check(IProgressMonitor monitor) throws InterruptedIOException {
    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("inventory interrupted");
    if (monitor != null) monitor.checkCanceled();
  }
}
