// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies complete UTF-8 writes, no-op replay and fail-closed path fencing. */
final class NebulaM3RecipeDagAtomicPublicationTest {
    @TempDir Path sandbox;

    @Test
    void publishesIdenticalEvidenceAndSkipsByteIdenticalSecondRun() throws Exception {
        Path output = sandbox.resolve("evidence");
        NebulaM3RecipeDagManifestCli.main(new String[] {output.toString()});
        Path manifest = output.resolve("recipe-dag.tsv");
        assertEquals(NebulaM3RecipeDagManifest.tsv(),
                Files.readString(manifest, StandardCharsets.UTF_8));
        assertEquals(NebulaM3OrchestratorPlans.droolsDrl(),
                Files.readString(output.resolve("drools-agenda.drl"), StandardCharsets.UTF_8));
        assertEquals(7, Files.list(output).count());
        FileTime marker = FileTime.fromMillis(1_600_000_000_000L);
        Files.setLastModifiedTime(manifest, marker);
        FileTime retained = Files.getLastModifiedTime(manifest);
        NebulaM3RecipeDagManifestCli.main(new String[] {output.toString()});
        assertEquals(retained, Files.getLastModifiedTime(manifest));
        assertEquals(7, Files.list(output).count());
    }

    @Test
    void overwritesAStaleRegularTargetViaCompleteReplacement() throws Exception {
        Path output = Files.createDirectory(sandbox.resolve("output"));
        Path manifest = output.resolve("recipe-dag.tsv");
        Files.writeString(manifest, "stale-old-content", StandardCharsets.UTF_8);
        NebulaM3RecipeDagManifestCli.main(new String[] {output.toString()});
        assertEquals(NebulaM3RecipeDagManifest.tsv(),
                Files.readString(manifest, StandardCharsets.UTF_8));
    }

    @Test
    void rejectsSpecialTargetBeforePublishingAnyArtifact() throws Exception {
        Path output = Files.createDirectory(sandbox.resolve("output"));
        Path existing = output.resolve("recipe-dag.tsv");
        Files.writeString(existing, "untouched", StandardCharsets.UTF_8);
        Files.createDirectory(output.resolve("airflow-dag.py"));
        assertThrows(IOException.class,
                () -> NebulaM3RecipeDagManifestCli.main(new String[] {output.toString()}));
        assertEquals("untouched", Files.readString(existing, StandardCharsets.UTF_8));
        assertTrue(Files.isDirectory(output.resolve("airflow-dag.py")));
    }

    @Test
    void refusesSymlinkedOutputRoot() throws Exception {
        Path real = Files.createDirectory(sandbox.resolve("real"));
        Path symlink = sandbox.resolve("link");
        try {
            Files.createSymbolicLink(symlink, real);
        } catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
            Assumptions.assumeTrue(false, "symlinks unavailable in this environment");
        }
        assertThrows(IOException.class,
                () -> NebulaM3RecipeDagManifestCli.main(new String[] {symlink.toString()}));
        assertEquals(0, Files.list(real).count());
    }

    @Test
    void refusesSymlinkedTargetWithoutChangingItsOutsideContents() throws Exception {
        Path outside = sandbox.resolve("outside.txt");
        Files.writeString(outside, "external", StandardCharsets.UTF_8);
        Path output = Files.createDirectory(sandbox.resolve("output"));
        try {
            Files.createSymbolicLink(output.resolve("recipe-dag.tsv"), outside);
        } catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
            Assumptions.assumeTrue(false, "symlinks unavailable in this environment");
        }
        assertThrows(IOException.class,
                () -> NebulaM3RecipeDagManifestCli.main(new String[] {output.toString()}));
        assertEquals("external", Files.readString(outside, StandardCharsets.UTF_8));
    }

    @Test
    void replacementDoesNotMutateAnotherHardLinkToTheOldTarget() throws Exception {
        Path outside = sandbox.resolve("old.txt");
        Files.writeString(outside, "old-shared-bytes", StandardCharsets.UTF_8);
        Path output = Files.createDirectory(sandbox.resolve("output"));
        try {
            Files.createLink(output.resolve("recipe-dag.tsv"), outside);
        } catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
            Assumptions.assumeTrue(false, "hard links unavailable in this environment");
        }
        NebulaM3RecipeDagManifestCli.main(new String[] {output.toString()});
        assertEquals("old-shared-bytes", Files.readString(outside, StandardCharsets.UTF_8));
        assertEquals(NebulaM3RecipeDagManifest.tsv(),
                Files.readString(output.resolve("recipe-dag.tsv"), StandardCharsets.UTF_8));
    }

    @Test
    void invalidFilenameAndInterruptedExecutionDoNotMutateDestination() throws Exception {
        Path output = Files.createDirectory(sandbox.resolve("output"));
        Map<String, String> images = new LinkedHashMap<>();
        images.put("normal.tsv", "good");
        images.put("../escape", "bad");
        assertThrows(IOException.class, () -> NebulaM3RecipeDagManifestCli.publish(output, images));
        assertEquals(0, Files.list(output).count());
        assertThrows(IOException.class,
                () -> NebulaM3RecipeDagManifestCli.publish(sandbox.getRoot(), Map.of("x.tsv", "x")));

        Thread.currentThread().interrupt();
        try {
            assertThrows(IOException.class,
                    () -> NebulaM3RecipeDagManifestCli.publish(output, Map.of("safe.tsv", "value")));
        } finally {
            Thread.interrupted(); // Restore a clean JUnit worker after the negative-control test.
        }
        assertEquals(0, Files.list(output).count());
    }

    @Test
    void utf8PublicationPreservesExactNonAsciiBytes() throws Exception {
        Path output = sandbox.resolve("utf8");
        NebulaM3RecipeDagManifestCli.publish(output, Map.of("document.tsv", "λ-😀-東京\n"));
        assertArrayEquals("λ-😀-東京\n".getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(output.resolve("document.tsv")));
    }
}
