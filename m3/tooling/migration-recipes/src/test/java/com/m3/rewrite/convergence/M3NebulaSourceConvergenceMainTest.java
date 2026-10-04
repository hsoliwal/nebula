// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M3NebulaSourceConvergenceMainTest {
    @Test
    void admittedNebulaFileRunsAtomPatternDocumentationAndFixedPoint() {
        String before =
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return (a + b) * 31;
                    }
                }
                """;

        var receipt =
                M3NebulaSourceConvergenceMain.convergeSource(
                        "widgets/demo/src/example/Sample.java", before);

        assertTrue(receipt.changed());
        assertTrue(receipt.atomizationChanged());
        assertTrue(receipt.patternizationChanged());
        assertTrue(receipt.documentationChanged());
        assertTrue(receipt.fixedPoint());
        assertTrue(receipt.candidateSource().contains("int m3$pureIntAtom ="));
        assertTrue(receipt.candidateSource().contains("M3-IOP: PURE_INT_EXPRESSION"));
        assertTrue(receipt.candidateSource().contains("M3-ATOM: m3$pureIntAtom"));
        assertFalse(receipt.preSha256().equals(receipt.postSha256()));
    }

    @Test
    void unsupportedNebulaFileIsStableAndReceipted() {
        String source =
                """
                package example;
                public final class Stable {
                    public String value() {
                        return "stable";
                    }
                }
                """;

        var receipt =
                M3NebulaSourceConvergenceMain.convergeSource(
                        "widgets/demo/src/example/Stable.java", source);

        assertFalse(receipt.changed());
        assertFalse(receipt.hold());
        assertTrue(receipt.fixedPoint());
        assertEquals(receipt.preSha256(), receipt.postSha256());
        assertTrue(receipt.candidateSource().isEmpty());
    }

    @Test
    void treeRunNeverWritesProductSourceAndSkipsControlAndGeneratedTrees() throws Exception {
        Path root = Files.createTempDirectory("m3-nebula-root-");
        Files.writeString(root.resolve("pom.xml"), "<project/>\n");
        Path source = root.resolve("widgets/demo/src/example/Sample.java");
        Path generated = root.resolve("widgets/demo/target/generated/Generated.java");
        Path sidecar = root.resolve("m3/tooling/ignored/Sidecar.java");
        Files.createDirectories(source.getParent());
        Files.createDirectories(generated.getParent());
        Files.createDirectories(sidecar.getParent());
        Files.writeString(
                source,
                """
                package example;
                final class Sample {
                    private static int compute(int a, int b) {
                        return a + b;
                    }
                }
                """);
        Files.writeString(generated, "final class Generated {}\n");
        Files.writeString(sidecar, "final class Sidecar {}\n");
        String original = Files.readString(source);
        Path output =
                root.resolve(
                        "m3/tooling/migration-recipes/target/m3-nebula-source-convergence");

        var first = M3NebulaSourceConvergenceMain.convergeTree(root, output, 2);

        assertEquals(1, first.files());
        assertEquals(1, first.changed());
        assertEquals(0, first.holds());
        assertEquals(original, Files.readString(source));
        assertTrue(
                Files.exists(
                        output.resolve(
                                "candidates/widgets/demo/src/example/Sample.java")));
        assertTrue(Files.exists(output.resolve(M3NebulaSourceConvergenceMain.MANIFEST)));
        assertTrue(Files.exists(output.resolve(M3NebulaSourceConvergenceMain.SUMMARY)));

        Path replay =
                root.resolve(
                        "m3/tooling/migration-recipes/target/m3-nebula-source-convergence-replay");
        var second = M3NebulaSourceConvergenceMain.convergeTree(root, replay, 1);
        assertEquals(first.root(), second.root());
        assertEquals(
                Files.readString(output.resolve(M3NebulaSourceConvergenceMain.MANIFEST)),
                Files.readString(replay.resolve(M3NebulaSourceConvergenceMain.MANIFEST)));
    }

    @Test
    void outputFenceRejectsProductOrNonTargetLocations() throws Exception {
        Path root = Files.createTempDirectory("m3-nebula-root-");
        Files.writeString(root.resolve("pom.xml"), "<project/>\n");
        Files.createDirectories(root.resolve("widgets"));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3NebulaSourceConvergenceMain.convergeTree(
                                root, root.resolve("widgets/generated"), 1));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3NebulaSourceConvergenceMain.convergeTree(
                                root, root.resolve("m3/evidence"), 1));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3NebulaSourceConvergenceMain.convergeTree(
                                root,
                                root.resolve(
                                        "m3/tooling/migration-recipes/target/out"),
                                0));
    }
}
