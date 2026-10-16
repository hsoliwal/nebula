// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;

/** Writes the content-addressed Nebula M3 recipe DAG evidence for build/CI consumption. */
public final class NebulaM3RecipeDagManifestCli {
    private NebulaM3RecipeDagManifestCli() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: NebulaM3RecipeDagManifestCli <output-directory>");
        }
        Path output = Path.of(Objects.requireNonNull(args[0], "output")).toAbsolutePath().normalize();

        StringBuilder orchestrators =
                new StringBuilder("orchestrator\tmutationAuthority\tpromotionAuthority\n");
        NebulaM3RecipeDagManifest.orchestrationTargets().stream()
                .sorted(Comparator.comparing(Enum::name))
                .forEach(target -> orchestrators.append(target.name()).append("\tfalse\tfalse\n"));

        // Construct the complete deterministic postimage before touching the output directory.
        Map<String, String> images = new LinkedHashMap<>();
        images.put("recipe-dag.tsv", NebulaM3RecipeDagManifest.tsv());
        images.put("recipe-dag.sha256", NebulaM3RecipeDagManifest.root() + "\n");
        images.put("orchestrators.tsv", orchestrators.toString());
        images.put("camel-route.yaml", NebulaM3OrchestratorPlans.camelYaml());
        images.put("airflow-dag.py", NebulaM3OrchestratorPlans.airflowPython());
        images.put("drools-agenda.drl", NebulaM3OrchestratorPlans.droolsDrl());
        images.put("orchestrator-plans.sha256", NebulaM3OrchestratorPlans.root() + "\n");
        publish(output, images);
    }

    /**
     * Publishes each UTF-8 artifact through an atomic same-directory rename.
     *
     * <p>This is per-file atomicity, not a transaction across all seven artifacts. An identical
     * replay leaves files untouched. Symlinked directories/targets and special target files fail
     * closed, and unsupported atomic moves are not silently downgraded to ordinary writes.</p>
     */
    static void publish(Path output, Map<String, String> images) throws IOException {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(images, "images");
        Path directory = output.toAbsolutePath().normalize();
        if (directory.equals(directory.getRoot())) {
            throw new IOException("M3 DAG output cannot be a filesystem root");
        }
        rejectSymlinkAncestors(directory);
        Files.createDirectories(directory);
        rejectSymlinkAncestors(directory);

        // Preflight every destination before staging any bytes.
        for (Map.Entry<String, String> image : images.entrySet()) {
            Path target = destination(directory, image.getKey());
            Objects.requireNonNull(image.getValue(), "image");
            requireRegularOrMissing(target);
        }

        Map<Path, Path> staged = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, String> image : images.entrySet()) {
                checkInterrupted();
                Path target = destination(directory, image.getKey());
                String content = image.getValue();
                if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)
                        && java.util.Arrays.equals(
                                content.getBytes(StandardCharsets.UTF_8),
                                Files.readAllBytes(target))) {
                    continue; // Byte-identical replay is a filesystem fixed point.
                }
                Path temporary = Files.createTempFile(directory, ".m3-dag-", ".tmp");
                staged.put(target, temporary);
                Files.writeString(temporary, content, StandardCharsets.UTF_8);
            }
            for (Map.Entry<Path, Path> entry : staged.entrySet()) {
                checkInterrupted();
                requireRegularOrMissing(entry.getKey());
                Files.move(
                        entry.getValue(),
                        entry.getKey(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            for (Path temporary : staged.values()) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static Path destination(Path directory, String filename) throws IOException {
        if (filename == null || !filename.matches("[a-z][a-z0-9.-]*")
                || ".".equals(filename) || "..".equals(filename)) {
            throw new IOException("unsafe M3 DAG evidence filename: " + filename);
        }
        return directory.resolve(filename);
    }

    private static void requireRegularOrMissing(Path path) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("unsafe M3 DAG evidence target: " + path);
        }
    }

    private static void rejectSymlinkAncestors(Path directory) throws IOException {
        for (Path candidate = directory; candidate != null; candidate = candidate.getParent()) {
            if (Files.isSymbolicLink(candidate)) {
                throw new IOException("symlinked M3 DAG output directory: " + candidate);
            }
        }
    }

    private static void checkInterrupted() throws IOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new IOException("M3 DAG evidence publication interrupted");
        }
    }
}
