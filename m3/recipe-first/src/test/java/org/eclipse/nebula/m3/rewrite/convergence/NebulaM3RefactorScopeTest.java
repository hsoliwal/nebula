// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class NebulaM3RefactorScopeTest {
    @Test
    void canonicalOrderMatchesTheM3AuthorityLadderExactly() {
        assertEquals(
                List.of(
                        NebulaM3RefactorScope.FILE,
                        NebulaM3RefactorScope.VISIBILITY,
                        NebulaM3RefactorScope.PACKAGE,
                        NebulaM3RefactorScope.MODULE,
                        NebulaM3RefactorScope.MULTI_MODULE,
                        NebulaM3RefactorScope.LIBRARY_API),
                NebulaM3RefactorScope.canonicalOrder());
    }

    @Test
    void authorityContainsOnlyNarrowerOrEqualScopes() {
        for (NebulaM3RefactorScope authority : NebulaM3RefactorScope.values()) {
            for (NebulaM3RefactorScope required : NebulaM3RefactorScope.values()) {
                assertEquals(
                        authority.ordinal() >= required.ordinal(),
                        authority.canContain(required),
                        authority + " -> " + required);
            }
        }
        assertFalse(
                NebulaM3RefactorScope.FILE.canContain(
                        NebulaM3RefactorScope.VISIBILITY));
        assertTrue(
                NebulaM3RefactorScope.LIBRARY_API.canContain(
                        NebulaM3RefactorScope.FILE));
    }

    @Test
    void promotionSelectsTheSmallestAuthorityContainingBothRequirements() {
        assertEquals(
                NebulaM3RefactorScope.FILE,
                NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.FILE,
                        NebulaM3RefactorScope.FILE));
        assertEquals(
                NebulaM3RefactorScope.VISIBILITY,
                NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.FILE,
                        NebulaM3RefactorScope.VISIBILITY));
        assertEquals(
                NebulaM3RefactorScope.MULTI_MODULE,
                NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.PACKAGE,
                        NebulaM3RefactorScope.MULTI_MODULE));
        assertEquals(
                NebulaM3RefactorScope.LIBRARY_API,
                NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.LIBRARY_API,
                        NebulaM3RefactorScope.MODULE));
    }

    @Test
    void nullAuthorityFailsClosed() {
        assertThrows(
                NullPointerException.class,
                () -> NebulaM3RefactorScope.FILE.canContain(null));
        assertThrows(
                NullPointerException.class,
                () -> NebulaM3RefactorScope.max(
                        null,
                        NebulaM3RefactorScope.FILE));
        assertThrows(
                NullPointerException.class,
                () -> NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.FILE,
                        null));
    }

    @Test
    void canonicalFileDagNeverAcquiresBroaderAuthority() {
        assertEquals(
                NebulaM3RefactorScope.FILE,
                NebulaM3FileConvergenceRecipeDag.maximumEditScope());
        assertTrue(
                NebulaM3RecipeDagManifest.nodes().stream()
                        .allMatch(
                                node ->
                                        node.scope()
                                                .equals(
                                                        NebulaM3RefactorScope.FILE
                                                                .name())));
    }
}
