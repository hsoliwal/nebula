// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Full immutable Git-tree intake into the existing four-column corpus format.
 * No donor code is executed, no network access is performed, and no source is promoted.
 * The optional manifest is consumed once by the existing shared JavaProblemCorpus index.
 */
public final class PinnedJavaCorpusManifest {
    public static final String PROPERTY = "synexia.algorithms.corpus.additionalManifest";
    private static final String HEADER = "platform\trepository\tcommit\tpath\n";
    private static final String TRAILER = "#sha256\t";
    private static final int MAX_BYTES = 64 * 1024 * 1024;
    private static final long GIT_TIMEOUT_SECONDS = 30L;

    private PinnedJavaCorpusManifest() {}

    /**
     * Enumerates ALL regular Java paths at the pin, including tests and helpers.
     * The output is create-only (or byte-identical on a repeated call).
     * A checkout inside another repository is rejected; an uninitialized submodule
     * must never accidentally enumerate its parent repository.
     *
     * @return SHA-256 of the canonical manifest body, not a source-body receipt
     */
    public static String capture(
            Path checkout, String platform, String repository, String revision,
            Path output, IProgressMonitor suppliedMonitor) throws IOException {
        Objects.requireNonNull(checkout, "checkout");
        Objects.requireNonNull(output, "output");
        validateIdentity(platform, repository, revision);
        IProgressMonitor monitor = ProgressMonitors.nonNull(suppliedMonitor);
        monitor.beginTask("capture-pinned-java-tree", IProgressMonitor.UNKNOWN);
        try {
            ProgressMonitors.checkCanceled(monitor);
            Path root = checkout.toRealPath();
            Path actual = Path.of(decode(git(root, monitor, "rev-parse", "--show-toplevel")).strip())
                    .toRealPath();
            if (!actual.equals(root)) {
                throw new IOException("checkout must be an initialized repository root: " + root);
            }
            String pin = decode(git(root, monitor, "rev-parse", "--verify", revision + "^{commit}"))
                    .strip();
            if (!pin.equals(revision)) throw new IOException("immutable commit mismatch");
            byte[] raw = git(root, monitor, "ls-tree", "-r", "-z", "--full-tree", revision);
            String tree = decode(raw);
            if (!tree.isEmpty() && tree.charAt(tree.length() - 1) != '\0') {
                throw new IOException("truncated Git tree");
            }
            TreeSet<String> paths = new TreeSet<>();
            int start = 0;
            for (int end = tree.indexOf('\0'); end >= 0; end = tree.indexOf('\0', start)) {
                ProgressMonitors.checkCanceled(monitor);
                String entry = tree.substring(start, end);
                start = end + 1;
                int tab = entry.indexOf('\t');
                if (tab < 0) throw new IOException("malformed Git tree entry");
                String path = entry.substring(tab + 1);
                if (!path.endsWith(".java")) continue;
                String metadata = entry.substring(0, tab);
                if (!metadata.matches("100(?:644|755) blob [0-9a-f]{40}(?:[0-9a-f]{24})?")) {
                    throw new IOException("non-regular Java entry is not silently omitted: " + path);
                }
                validatePath(path);
                if (!paths.add(path)) throw new IOException("duplicate Git path: " + path);
                monitor.worked(1L);
            }
            if (paths.isEmpty()) throw new IOException("pinned tree contains no regular Java files");
            String prefix = platform + "\t" + repository + "\t" + revision + "\t";
            StringBuilder body = new StringBuilder(HEADER);
            for (String path : paths) {
                ProgressMonitors.checkCanceled(monitor);
                body.append(prefix).append(path).append('\n');
                if (body.length() > MAX_BYTES) throw new IOException("manifest size budget exceeded");
            }
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            if (payload.length > MAX_BYTES - 128) throw new IOException("manifest size budget exceeded");
            String hash = sha256(payload);
            byte[] encoded = (body + TRAILER + hash + "\n").getBytes(StandardCharsets.UTF_8);
            Path destination = output.toAbsolutePath().normalize();
            if (destination.startsWith(root)) {
                throw new IOException("manifest output must be outside the donor checkout");
            }
            Files.createDirectories(destination.getParent());
            destination = destination.getParent().toRealPath().resolve(destination.getFileName());
            if (destination.startsWith(root)) throw new IOException("output resolves inside donor checkout");
            ProgressMonitors.checkCanceled(monitor);
            try {
                Files.write(destination, encoded, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException exists) {
                if (!Arrays.equals(readBytes(destination), encoded)) {
                    throw new IOException("refusing to overwrite a different manifest: " + destination, exists);
                }
            }
            return hash;
        } finally {
            monitor.done();
        }
    }

    /** Reads and validates a bounded, hash-bound manifest, including its header but not trailer. */
    public static List<String> read(Path manifest, IProgressMonitor suppliedMonitor) throws IOException {
        IProgressMonitor monitor = ProgressMonitors.nonNull(suppliedMonitor);
        monitor.beginTask("read-pinned-java-manifest", IProgressMonitor.UNKNOWN);
        try {
            ProgressMonitors.checkCanceled(monitor);
            String text = decode(readBytes(Objects.requireNonNull(manifest, "manifest")));
            int footer = text.lastIndexOf(TRAILER);
            if (!text.startsWith(HEADER) || footer < HEADER.length()
                    || text.indexOf('\r') >= 0 || text.charAt(footer - 1) != '\n' || !text.endsWith("\n")) {
                throw new IOException("missing manifest header or terminal receipt");
            }
            String digest = text.substring(footer + TRAILER.length(), text.length() - 1);
            String body = text.substring(0, footer);
            if (!digest.matches("[0-9a-f]{64}")
                    || !sha256(body.getBytes(StandardCharsets.UTF_8)).equals(digest)) {
                throw new IOException("manifest checksum mismatch");
            }
            List<String> lines = body.lines().toList();
            if (lines.size() < 2) throw new IOException("empty corpus manifest");
            String previous = null;
            for (int i = 1; i < lines.size(); i++) {
                ProgressMonitors.checkCanceled(monitor);
                String row = lines.get(i);
                String[] columns = row.split("\t", -1);
                if (columns.length != 4) throw new IOException("invalid corpus row " + (i + 1));
                validateIdentity(columns[0], columns[1], columns[2]);
                validatePath(columns[3]);
                if (previous != null && previous.compareTo(row) >= 0) {
                    throw new IOException("manifest rows must be unique and sorted");
                }
                previous = row;
                monitor.worked(1L);
            }
            return lines;
        } finally {
            monitor.done();
        }
    }

    private static void validateIdentity(String platform, String repository, String revision) {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(revision, "revision");
        if (!platform.matches("[A-Z][A-Z0-9_]*")
                || !repository.matches("[A-Za-z0-9][A-Za-z0-9_.-]*/[A-Za-z0-9][A-Za-z0-9_.-]*")
                || !revision.matches("[0-9a-f]{40}(?:[0-9a-f]{24})?")) {
            throw new IllegalArgumentException("explicit platform, owner/repository and immutable SHA required");
        }
    }

    private static void validatePath(String path) throws IOException {
        if (!path.endsWith(".java") || path.indexOf('\\') >= 0 || path.indexOf(':') >= 0
                || path.chars().anyMatch(Character::isISOControl)) {
            throw new IOException("unsupported Java path in TSV: " + path);
        }
        for (String component : path.split("/", -1)) {
            if (component.isEmpty() || component.equals(".") || component.equals("..")) {
                throw new IOException("non-relative or traversing Java path: " + path);
            }
        }
    }

    private static byte[] readBytes(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("manifest must be a regular non-symlink file: " + path);
        }
        try (InputStream in = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            return bounded(in);
        }
    }

    private static byte[] bounded(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) throw new IOException("intake byte budget exceeded");
        return bytes;
    }

    private static String decode(byte[] bytes) throws IOException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException failure) {
            throw new IOException("intake requires valid UTF-8", failure);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static byte[] git(Path root, IProgressMonitor monitor, String... args) throws IOException {
        ProgressMonitors.checkCanceled(monitor);
        List<String> command = new ArrayList<>(List.of("git", "--no-replace-objects", "-C", root.toString()));
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().remove("GIT_DIR");
        builder.environment().remove("GIT_WORK_TREE");
        Process process = builder.start();
        process.getOutputStream().close();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor();
                InputStream stdout = process.getInputStream()) {
            try {
                var output = executor.submit(() -> bounded(stdout));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(GIT_TIMEOUT_SECONDS);
                while (!process.waitFor(100L, TimeUnit.MILLISECONDS)) {
                    ProgressMonitors.checkCanceled(monitor);
                    if (output.isDone()) output.get();
                    if (System.nanoTime() - deadline >= 0L) throw new IOException("Git intake timed out");
                }
                ProgressMonitors.checkCanceled(monitor);
                byte[] result = output.get(1L, TimeUnit.SECONDS);
                if (process.exitValue() != 0) {
                    throw new IOException("Git intake failed: " + decode(result));
                }
                return result;
            } finally {
                process.destroyForcibly();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Git intake interrupted", interrupted);
        } catch (ExecutionException | TimeoutException failure) {
            throw new IOException("Git output could not be captured within budget", failure);
        } finally {
            process.destroyForcibly();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 5) {
            throw new IllegalArgumentException("usage: PinnedJavaCorpusManifest checkout platform repository commit output.tsv");
        }
        String hash = capture(Path.of(args[0]), args[1], args[2], args[3], Path.of(args[4]), null);
        System.out.printf(Locale.ROOT, "MANIFEST javaFiles=%d sha256=%s%n",
                read(Path.of(args[4]), null).size() - 1, hash);
    }
}
