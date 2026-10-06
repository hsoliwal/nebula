// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NebulaM3MasteryFanInTest {
    @TempDir
    Path temp;

    @Test
    void portableV6RoundTripsAndPinsRoot() throws Exception {
        NebulaM3MasteryFanIn.Receipt receipt = receipt();
        String tsv = receipt.toTsv();

        assertEquals(receipt, NebulaM3MasteryFanIn.parse(tsv));
        assertEquals(NebulaM3MasteryFanIn.REGEX_CASES, receipt.regexCaseCount());
        assertEquals("EXHAUSTIVE", receipt.scheduleCoverage());
        assertFalse(receipt.sourceMutationAuthority());
        assertFalse(receipt.semanticAuthority());
        assertFalse(receipt.donorSourceCopyAuthority());
        assertFalse(receipt.replacementAuthority());
        assertFalse(receipt.mergeAuthority());
        assertFalse(receipt.promotionAuthority());

        Path file = temp.resolve("mastery.tsv");
        Files.writeString(file, tsv);
        assertEquals(
                receipt,
                NebulaM3MasteryFanIn.read(file, receipt.root()));
        assertThrows(
                IllegalStateException.class,
                () ->
                        NebulaM3MasteryFanIn.read(
                                file, "f".repeat(64)));
    }

    @Test
    void authorityCoverageCountBooleanAndRootDriftFailClosed() {
        String tsv = receipt().toTsv();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3MasteryFanIn.parse(
                                tsv.replace(
                                        "promotionAuthority\tfalse",
                                        "promotionAuthority\ttrue")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3MasteryFanIn.parse(
                                tsv.replace(
                                        "scheduleCoverage\tEXHAUSTIVE",
                                        "scheduleCoverage\tPAIRWISE")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3MasteryFanIn.parse(
                                tsv.replace(
                                        "regexCaseCount\t10000",
                                        "regexCaseCount\t9999")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3MasteryFanIn.parse(
                                tsv.replace(
                                        "jniRequired\tfalse",
                                        "jniRequired\tFALSE")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        NebulaM3MasteryFanIn.parse(
                                tsv.replace(
                                        "root\t" + receipt().root(),
                                        "root\t" + "0".repeat(64))));
    }

    @Test
    void optionalJniParityShapeIsRootBound() {
        NebulaM3MasteryFanIn.Receipt receipt =
                new NebulaM3MasteryFanIn.Receipt(
                        "1".repeat(64),
                        "2".repeat(64),
                        "3".repeat(64),
                        "4".repeat(64),
                        "5".repeat(64),
                        "6".repeat(64),
                        "EXHAUSTIVE",
                        "7".repeat(64),
                        "8".repeat(64),
                        "9".repeat(64),
                        "a".repeat(64),
                        "b".repeat(64),
                        3,
                        2,
                        4,
                        "c".repeat(64),
                        "d".repeat(64),
                        NebulaM3MasteryFanIn.REGEX_CASES,
                        true,
                        "e".repeat(64),
                        16,
                        true,
                        "");

        assertTrue(receipt.jniRequired());
        assertEquals(receipt, NebulaM3MasteryFanIn.parse(receipt.toTsv()));
    }

    static NebulaM3MasteryFanIn.Receipt receipt() {
        return new NebulaM3MasteryFanIn.Receipt(
                "1".repeat(64),
                "2".repeat(64),
                "3".repeat(64),
                "4".repeat(64),
                "5".repeat(64),
                "6".repeat(64),
                "EXHAUSTIVE",
                "7".repeat(64),
                "8".repeat(64),
                "9".repeat(64),
                "a".repeat(64),
                "b".repeat(64),
                3,
                2,
                4,
                "c".repeat(64),
                "d".repeat(64),
                NebulaM3MasteryFanIn.REGEX_CASES,
                false,
                "",
                0,
                true,
                "");
    }
}
