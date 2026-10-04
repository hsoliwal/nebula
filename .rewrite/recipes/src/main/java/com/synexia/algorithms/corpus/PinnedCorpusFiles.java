// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Internal file-custody operations. No subprocess per source and no replacement of existing bytes. */
final class PinnedCorpusFiles {
    private static final int BUFFER_BYTES = 64 * 1024;

    record Verified(String gitBlob, String sha256, long bytes) {}

    private PinnedCorpusFiles() {}

    /** Parse unquoted, NUL-delimited `git ls-tree -r -z --full-tree` records once per repository. */
    static Map<String, String> javaTree(String tree) {
        Objects.requireNonNull(tree, "tree");
        TreeMap<String, String> result = new TreeMap<>();
        int start = 0;
        while (start < tree.length()) {
            int end = tree.indexOf('\0', start);
            if (end < 0) throw new IllegalArgumentException("unterminated Git tree record");
            String record = tree.substring(start, end);
            int tab = record.indexOf('\t');
            if (tab < 0) throw new IllegalArgumentException("Git tree record has no path");
            String path = record.substring(tab + 1);
            if (path.endsWith(".java")) {
                relative(path);
                String[] header = record.substring(0, tab).split(" ", -1);
                if (header.length != 3
                        || !(header[0].equals("100644") || header[0].equals("100755"))
                        || !header[1].equals("blob")
                        || !header[2].matches("[0-9a-f]{40}|[0-9a-f]{64}")) {
                    throw new IllegalArgumentException("not a regular Java Git blob: " + path);
                }
                if (result.putIfAbsent(path, header[2]) != null) {
                    throw new IllegalArgumentException("duplicate Java Git path: " + path);
                }
            }
            start = end + 1;
        }
        return Collections.unmodifiableMap(result);
    }

    /** Stream once: verify the Git object digest and SHA-256 over the exact bytes staged for publication. */
    static Verified copyVerified(
            Path sourceRoot, Path destinationRoot, String relativePath,
            String expectedGitBlob, Runnable checkCanceled) throws IOException {
        Objects.requireNonNull(checkCanceled, "checkCanceled").run();
        if (!Objects.requireNonNull(expectedGitBlob, "expectedGitBlob")
                .matches("[0-9a-f]{40}|[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid Git blob id");
        }
        Path source = child(sourceRoot, relativePath);
        Path destination = child(destinationRoot, relativePath);
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("missing regular Java source: " + source);
        }
        if (source.equals(destination)) throw new IOException("source and destination overlap");
        ensureDirectory(destination.getParent());
        long expectedSize = Files.size(source);
        MessageDigest git = digest(expectedGitBlob.length() == 40 ? "SHA-1" : "SHA-256");
        MessageDigest sha256 = digest("SHA-256");
        git.update(("blob " + expectedSize + "\0").getBytes(StandardCharsets.US_ASCII));
        Path staged = Files.createTempFile(destination.getParent(), ".donor-", ".tmp");
        try {
            long size = 0L;
            try (InputStream in = Files.newInputStream(source);
                    OutputStream out = Files.newOutputStream(staged)) {
                byte[] buffer = new byte[BUFFER_BYTES];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    checkCanceled.run();
                    if (count == 0) continue;
                    git.update(buffer, 0, count);
                    sha256.update(buffer, 0, count);
                    out.write(buffer, 0, count);
                    size = Math.addExact(size, count);
                }
            }
            String actualGitBlob = HexFormat.of().formatHex(git.digest());
            if (size != expectedSize || !expectedGitBlob.equals(actualGitBlob)) {
                throw new IOException("Git blob mismatch: " + relativePath);
            }
            checkCanceled.run();
            publish(staged, destination);
            return new Verified(actualGitBlob, HexFormat.of().formatHex(sha256.digest()), size);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    static void writeUtf8Once(Path root, String relativePath, String text) throws IOException {
        Path destination = child(root, relativePath);
        ensureDirectory(destination.getParent());
        Path staged = Files.createTempFile(destination.getParent(), ".donor-", ".tmp");
        try {
            Files.writeString(staged, text, StandardCharsets.UTF_8);
            publish(staged, destination);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    private static void publish(Path staged, Path destination) throws IOException {
        rejectSymlinks(destination);
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(destination, LinkOption.NOFOLLOW_LINKS)
                    || Files.mismatch(staged, destination) != -1L) {
                throw new IOException("refusing to overwrite differing artifact: " + destination);
            }
            return; // Identical immutable content is reusable; leave its metadata untouched.
        }
        // Do not use REPLACE_EXISTING or ATOMIC_MOVE (target-replacement semantics vary by provider).
        Files.move(staged, destination);
    }

    static Path child(Path root, String relativePath) throws IOException {
        Objects.requireNonNull(root, "root");
        relative(relativePath);
        Path absolute = root.toAbsolutePath().normalize();
        Path child = absolute.resolve(relativePath).normalize();
        if (!child.startsWith(absolute) || child.equals(absolute)) {
            throw new IOException("donor path escapes root: " + relativePath);
        }
        rejectSymlinks(child);
        return child;
    }

    static void ensureDirectory(Path root) throws IOException {
        rejectSymlinks(root);
        Files.createDirectories(root);
        rejectSymlinks(root);
    }

    static void rejectSymlinks(Path path) throws IOException {
        for (Path part = path.toAbsolutePath().normalize(); part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IOException("symbolic link in donor path: " + part);
        }
    }

    static void relative(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isEmpty() || path.startsWith("/") || path.indexOf('\\') >= 0
                || path.indexOf(':') >= 0 || path.indexOf('\uFFFD') >= 0
                || path.chars().anyMatch(c -> c < 32 || c == 127)) {
            throw new IllegalArgumentException("unsafe or non-portable donor path: " + path);
        }
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw new IllegalArgumentException("unsafe donor path: " + path);
            }
        }
    }

    static String javaString(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 32 || c == 127) {
                        out.append('\\').append((char) ('0' + ((c >>> 6) & 7)))
                                .append((char) ('0' + ((c >>> 3) & 7)))
                                .append((char) ('0' + (c & 7)));
                    } else out.append(c);
                }
            }
        }
        return out.toString();
    }

    static String javadoc(String text) {
        return text.replace("&", "&amp;").replace("\\", "&#92;")
                .replace("<", "&lt;").replace(">", "&gt;").replace("*/", "*&#47;")
                .replace("\r", "&#13;").replace("\n", "&#10;");
    }

    static String tsv(String text) {
        return text.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static MessageDigest digest(String name) {
        try {
            return MessageDigest.getInstance(name);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(name + " unavailable", impossible);
        }
    }
}
