// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NebulaM3SecondPassBindingTest {
    @Test
    void exactBindingIsAuthorityFreeAndContentIsPinned() {
        assertEquals("hsoliwal/com.synexia", NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY);
        assertEquals(
                "feat/m3-second-pass-signal-chain-20261004",
                NebulaM3SecondPassBinding.UPSTREAM_BRANCH);
        assertTrue(NebulaM3SecondPassBinding.UPSTREAM_COMMIT.matches("[0-9a-f]{40}"));
        assertEquals(8891, NebulaM3SecondPassBinding.UPSTREAM_PR);
        assertEquals(
                "com.synexia.rewrite.M3SecondPassAtomPatternRecipe",
                NebulaM3SecondPassBinding.RECIPE);
        assertEquals(2, NebulaM3SecondPassBinding.PASS_BUDGET);
        assertFalse(NebulaM3SecondPassBinding.sourceMutationAuthority());
        assertFalse(NebulaM3SecondPassBinding.replacementAuthority());
        assertFalse(NebulaM3SecondPassBinding.promotionAuthority());
    }

    @Test
    void checkedInBindingLedgerMatchesJavaOwnerExactly() throws Exception {
        Path repository = repositoryRoot();
        Path ledger = repository.resolve("m3/catalogue/second-pass-recipe-binding.tsv");

        assertTrue(Files.isRegularFile(ledger));
        assertEquals(NebulaM3SecondPassBinding.tsv(), Files.readString(ledger));
    }

    @Test
    void exactBindingFailsClosedOnAnyDrift() {
        NebulaM3SecondPassBinding.requireExact(
                NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY,
                NebulaM3SecondPassBinding.UPSTREAM_BRANCH,
                NebulaM3SecondPassBinding.UPSTREAM_COMMIT,
                NebulaM3SecondPassBinding.RECIPE,
                NebulaM3SecondPassBinding.PASS_BUDGET);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3SecondPassBinding.requireExact(
                                NebulaM3SecondPassBinding.UPSTREAM_REPOSITORY,
                                NebulaM3SecondPassBinding.UPSTREAM_BRANCH,
                                "0".repeat(40),
                                NebulaM3SecondPassBinding.RECIPE,
                                NebulaM3SecondPassBinding.PASS_BUDGET));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("widgets"))
                    && Files.isDirectory(current.resolve("m3"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Nebula repository root not found");
    }
}
