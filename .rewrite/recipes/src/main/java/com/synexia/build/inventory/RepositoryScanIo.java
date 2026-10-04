// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import com.synexia.job.IProgressMonitor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Shared bounded traversal/read mechanics for the two existing inventory projections.
 * No application code is executed. Source roots protect legal package names such as build/target.
 * This detects ordinary concurrent file changes, not an atomic snapshot of a hostile filesystem.
 */
final class RepositoryScanIo {
  private static final Set<String> SOURCE_ROOTS = Set.of("src", "source", "sources");
  private static final Set<String> VCS_DIRECTORIES = Set.of(".git", ".hg", ".svn");
  private RepositoryScanIo() {}

  record Omission(String path, String reason) {}

  record Discovery(List<Path> files, List<Omission> omissions, long bytes) {
    Discovery {
      files = List.copyOf(files);
      omissions = List.copyOf(omissions);
    }
  }

  /** Caller owns begin/worked/done; these mechanics only poll progress/cancellation. */
  static Discovery discover(Path repositoryRoot, Set<String> excludedNames,
      Predicate<Path> includeFile, long maxFiles, long maxBytes, long maxFileBytes,
      IProgressMonitor monitor) throws IOException {
    Objects.requireNonNull(excludedNames, "excludedNames");
    Objects.requireNonNull(includeFile, "includeFile");
    if (maxFiles < 1 || maxFiles > Integer.MAX_VALUE || maxBytes < 1 || maxFileBytes < 1)
      throw new IllegalArgumentException("invalid discovery limits");
    Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot").toAbsolutePath().normalize();
    check(monitor);
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
      throw new IOException("inventory root must be a real non-symlink directory");
    List<Path> files = new ArrayList<>();
    List<Omission> omissions = new ArrayList<>();
    long[] total = {0L};
    long[] visited = {0L};
    // Explicit failure rather than unlimited traversal of fileless/excluded-content directory trees.
    long maxEntries = Math.addExact(Math.multiplyExact(maxFiles, 32L), 1024L);
    Files.walkFileTree(root, new SimpleFileVisitor<>() {
      private void visit() throws IOException {
        check(monitor);
        if (++visited[0] > maxEntries) throw new IOException("REPOSITORY_INVENTORY_ENTRY_LIMIT");
      }
      @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        visit();
        if (!dir.equals(root) && excluded(root.relativize(dir), excludedNames)) {
          omissions.add(new Omission(relative(root, dir), "EXCLUDED_DIRECTORY"));
          return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
      }
      @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
          throws IOException {
        visit();
        if (!attrs.isRegularFile() || attrs.isSymbolicLink()) {
          omissions.add(new Omission(relative(root, file),
              attrs.isSymbolicLink() ? "SYMLINK_FILE" : "NONREGULAR_FILE"));
          return FileVisitResult.CONTINUE;
        }
        if (!includeFile.test(root.relativize(file))) {
          omissions.add(new Omission(relative(root, file), "POLICY_FILTERED_FILE"));
          return FileVisitResult.CONTINUE;
        }
        if (files.size() >= maxFiles) throw new IOException("REPOSITORY_INVENTORY_FILE_LIMIT");
        if (attrs.size() > maxFileBytes) throw new IOException("REPOSITORY_INVENTORY_SINGLE_FILE_LIMIT");
        if (attrs.size() > maxBytes - total[0]) throw new IOException("REPOSITORY_INVENTORY_BYTE_LIMIT");
        total[0] += attrs.size();
        files.add(file);
        return FileVisitResult.CONTINUE;
      }
    });
    check(monitor);
    files.sort(Comparator.comparing(path -> relative(root, path)));
    omissions.sort(Comparator.comparing(Omission::path).thenComparing(Omission::reason));
    return new Discovery(files, omissions, total[0]);
  }

  /** Only directory components are tested; a regular source/config file named 'build' is not a cache. */
  static boolean excluded(Path relativeDirectory, Set<String> excludedNames) {
    boolean source = false;
    for (Path component : relativeDirectory) {
      String name = component.toString();
      if (VCS_DIRECTORIES.contains(name)) return true;
      if (!source && (excludedNames.contains(name) || name.startsWith("target_nuke_"))) return true;
      if (SOURCE_ROOTS.contains(name)) source = true;
    }
    return false;
  }

  /** Immutable observation: digest, length and optional text all derive from the same bounded read. */
  static final class Snapshot {
    private final String sha256;
    private final long byteSize;
    private final int firstUnsignedByte;
    private final byte[] contents;
    private Snapshot(String sha256, long byteSize, int firstUnsignedByte, byte[] contents) {
      this.sha256 = sha256;
      this.byteSize = byteSize;
      this.firstUnsignedByte = firstUnsignedByte;
      this.contents = contents;
    }
    String sha256() { return sha256; }
    long byteSize() { return byteSize; }
    int firstUnsignedByte() { return firstUnsignedByte; }
    String text() { return contents == null ? null : new String(contents, StandardCharsets.UTF_8); }
    InputStream input() {
      if (contents == null) throw new IllegalStateException("snapshot bytes were not retained");
      return new ByteArrayInputStream(contents);
    }
  }

  static Snapshot read(Path file, long maxBytes, long maxCapturedBytes, boolean capture,
      int bufferBytes, IProgressMonitor monitor) throws IOException {
    Objects.requireNonNull(file, "file");
    if (maxBytes < 0 || maxCapturedBytes < 0 || maxCapturedBytes > Integer.MAX_VALUE - 8L
        || bufferBytes < 4096 || bufferBytes > 1_048_576)
      throw new IllegalArgumentException("invalid read limits");
    check(monitor);
    BasicFileAttributes before = attributes(file);
    if (!before.isRegularFile() || before.isSymbolicLink())
      throw new IOException("REPOSITORY_INVENTORY_NONREGULAR_SOURCE");
    if (before.size() > maxBytes) throw new IOException("REPOSITORY_INVENTORY_READ_BYTE_LIMIT");
    if (capture && before.size() > maxCapturedBytes)
      throw new IOException("REPOSITORY_INVENTORY_CAPTURE_LIMIT");
    MessageDigest digest = digest();
    ByteArrayOutputStream retained = capture
        ? new ByteArrayOutputStream((int) Math.min(before.size(), 8192L)) : null;
    byte[] buffer = new byte[bufferBytes];
    long count = 0;
    int first = -1;
    try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
      for (;;) {
        check(monitor);
        int read = input.read(buffer);
        if (read < 0) break;
        if (read == 0) continue;
        if (read > maxBytes - count) throw new IOException("REPOSITORY_INVENTORY_READ_BYTE_LIMIT");
        if (retained != null && read > maxCapturedBytes - count)
          throw new IOException("REPOSITORY_INVENTORY_CAPTURE_LIMIT");
        if (first < 0) first = buffer[0] & 255;
        count += read;
        digest.update(buffer, 0, read);
        if (retained != null) retained.write(buffer, 0, read);
      }
    }
    check(monitor);
    BasicFileAttributes after = attributes(file);
    if (!after.isRegularFile() || after.isSymbolicLink() || count != before.size()
        || count != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())
        || !Objects.equals(before.fileKey(), after.fileKey()))
      throw new IOException("REPOSITORY_INVENTORY_SOURCE_CHANGED");
    return new Snapshot(HexFormat.of().formatHex(digest.digest()), count, first,
        retained == null ? null : retained.toByteArray());
  }

  static void check(IProgressMonitor monitor) throws InterruptedIOException {
    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("inventory interrupted");
    if (monitor != null) monitor.checkCanceled();
  }

  private static BasicFileAttributes attributes(Path file) throws IOException {
    return Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
  }
  private static String relative(Path root, Path file) {
    return root.relativize(file).toString().replace('\\', '/');
  }
  private static MessageDigest digest() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
  }
}
