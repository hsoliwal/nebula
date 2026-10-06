// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class NebulaM3TransferContractTest {
    @Test
    void contractBindsCanonicalRecipeDagAndTargetRepositories() {
        assertEquals("NEBULA_M3_RECIPE_TRANSFER_V1", NebulaM3TransferContract.SCHEMA);
        assertEquals(21, NebulaM3TransferContract.JAVA_RELEASE);
        assertEquals("8.90.4", NebulaM3TransferContract.OPENREWRITE_VERSION);
        assertEquals(
                "org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe",
                NebulaM3TransferContract.ENTRYPOINT);
        assertEquals(
                List.of("hsoliwal/M3jdk21", "hsoliwal/com.synexia"),
                NebulaM3TransferContract.targets().stream()
                        .map(NebulaM3TransferContract.Target::repository)
                        .toList());
        assertFalse(NebulaM3TransferContract.directSourceMutationAuthority());
        assertFalse(NebulaM3TransferContract.promotionAuthority());
        assertTrue(
                NebulaM3TransferContract.targets().stream()
                        .noneMatch(
                                target ->
                                        target.directSourceMutationAuthority()
                                                || target.promotionAuthority()));
        assertTrue(NebulaM3TransferContract.root().matches("[0-9a-f]{64}"));
    }

    @Test
    void contractCarriesAllSchedulerTargetsWithoutGivingThemAuthority() {
        assertEquals(
                Set.of(
                        NebulaM3RecipeDagManifest.Orchestrator.MAVEN_OPENREWRITE,
                        NebulaM3RecipeDagManifest.Orchestrator.CAMEL,
                        NebulaM3RecipeDagManifest.Orchestrator.AIRFLOW,
                        NebulaM3RecipeDagManifest.Orchestrator.DROOLS),
                NebulaM3RecipeDagManifest.orchestrationTargets());
        String tsv = NebulaM3TransferContract.orchestratorsTsv();
        assertTrue(tsv.contains("MAVEN_OPENREWRITE	false	false"));
        assertTrue(tsv.contains("CAMEL	false	false"));
        assertTrue(tsv.contains("AIRFLOW	false	false"));
        assertTrue(tsv.contains("DROOLS	false	false"));
    }

    @Test
    void transferDagRetainsAtomizePatternizeDocumentAndFixedPointOrder() {
        assertEquals(
                List.of(
                        NebulaM3FileConvergenceRecipeDag.Phase.INVENTORY,
                        NebulaM3FileConvergenceRecipeDag.Phase.ATOMIZATION,
                        NebulaM3FileConvergenceRecipeDag.Phase.PATTERNIZATION,
                        NebulaM3FileConvergenceRecipeDag.Phase.DOCUMENTATION,
                        NebulaM3FileConvergenceRecipeDag.Phase.PROVEN_FILE_CONVERGENCE),
                NebulaM3FileConvergenceRecipeDag.atoms().stream()
                        .map(NebulaM3FileConvergenceRecipeDag.Atom::phase)
                        .toList());
        assertTrue(
                NebulaM3TransferContract.metadataTsv()
                        .contains(
                                "dagRoot	"
                                        + NebulaM3RecipeDagManifest.root()));
    }

    @Test
    void cliWritesReplayableContentAddressedBundle() throws Exception {
        var output = Files.createTempDirectory("nebula-m3-transfer-");

        NebulaM3TransferContractCli.main(new String[] {output.toString()});

        assertEquals(
                NebulaM3TransferContract.metadataTsv(),
                Files.readString(output.resolve("transfer-metadata.tsv")));
        assertEquals(
                NebulaM3RecipeDagManifest.tsv(),
                Files.readString(output.resolve("transfer-dag.tsv")));
        assertEquals(
                NebulaM3TransferContract.targetsTsv(),
                Files.readString(output.resolve("transfer-targets.tsv")));
        assertEquals(
                NebulaM3TransferContract.orchestratorsTsv(),
                Files.readString(output.resolve("orchestrators.tsv")));
        assertEquals(
                NebulaM3TransferContract.root() + "
",
                Files.readString(output.resolve("transfer.sha256")));
    }
}
