// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.widgets.m3.qualification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Pure Java21 negative-control and fixed-point oracle, no SWT fake runtime. */
public final class M3WebWidgetMappingAuditTest {
    private static final String PREFIX = "m3/web-widget-reverse/";
    private static final String DONOR = PREFIX + "FRAMEWORK_DONORS_20261025.tsv";
    private static final String CROSSWALK = PREFIX + "WEB_SEMANTIC_CROSSWALK_20261025.tsv";
    private static final String SOURCE = PREFIX + "WEB_COMPONENT_SOURCES.tsv";
    private static final String RWT = "docs/m3/rwt/RWT_COLUMN.tsv";
    private static final M3WebWidgetMappingAudit.Counts ONE =
            new M3WebWidgetMappingAudit.Counts(1, 1, 1, 1, 1);
    private static int assertions;

    private M3WebWidgetMappingAuditTest() {}

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("nebula-web-mapping-test-");
        try {
            fixture(root);
            var a = M3WebWidgetMappingAudit.audit(root, ONE);
            var b = M3WebWidgetMappingAudit.audit(root, ONE);
            check(a.equals(b), "fixed-point source digest");
            check(a.sha256().matches("[0-9a-f]{64}"), "source digest shape");
            reject(root, CROSSWALK, "PROPOSED_NOT_RUN", "QUALIFIED", "false semantic promotion");
            reject(root, CROSSWALK, "STATIC_TEXT", "UNKNOWN_PROFILE", "unknown profile");
            reject(root, CROSSWALK, "LABEL", "INVALID ID", "invalid identity");
            reject(root, DONOR, "0123456789abcdef0123456789abcdef01234567", "1234", "unpinned revision");
            reject(root, DONOR, "PATTERN_ONLY", "ADAPT_WITH_NOTICE", "license laundering");
            reject(root, SOURCE, "NOT_IMPLEMENTED", "IMPLEMENTED", "unproven implementation");
            reject(root, RWT, "NOT_RUN test", "PASS", "unrun RWT promotion");
            reject(root, RWT, "RAP_NATIVE", "FOREIGN_RENDERER", "foreign RWT route");
            reject(root, CROSSWALK, "LABEL\tspan", "LABEL\tspan\tEXTRA", "TSV column mutation");
            reject(root, CROSSWALK, "STATIC_TEXT", "STATIC_TEXT\r", "CRLF encoding");
            checkReject(() -> M3WebWidgetMappingAudit.audit(root,
                    new M3WebWidgetMappingAudit.Counts(2, 1, 1, 1, 1)), "count baseline drift");
            byte[] input = Files.readAllBytes(root.resolve(CROSSWALK));
            Files.write(root.resolve(CROSSWALK), new byte[] {(byte) 0xc3, (byte) 0x28});
            checkReject(() -> M3WebWidgetMappingAudit.audit(root, ONE), "invalid UTF-8");
            Files.write(root.resolve(CROSSWALK), input);
            Path table = root.resolve(CROSSWALK);
            Path other = root.resolve("foreign.tsv");
            Files.writeString(other, "foreign\n");
            Files.delete(table);
            try {
                Files.createSymbolicLink(table, other);
                checkReject(() -> M3WebWidgetMappingAudit.audit(root, ONE), "symlink source");
            } finally {
                Files.deleteIfExists(table);
                Files.write(table, input);
            }
            check(M3WebWidgetMappingAudit.audit(root, ONE).equals(a), "fixture restored");
            System.out.println("schema=m3-web-widget-mapping-audit-test/1");
            System.out.println("assertions=" + assertions);
            System.out.println("result=PASS_METADATA_ONLY");
        } finally {
            deleteTree(root);
        }
    }

    private static void reject(Path root, String relative, String source, String replacement,
            String label) throws Exception {
        Path target = root.resolve(relative);
        String before = Files.readString(target);
        check(before.contains(source), "fixture contains " + source);
        try {
            Files.writeString(target, before.replace(source, replacement));
            checkReject(() -> M3WebWidgetMappingAudit.audit(root, ONE), label);
        } finally {
            Files.writeString(target, before);
        }
    }

    private static void fixture(Path root) throws IOException {
        write(root, DONOR,
                "# SPDX-License-Identifier: Apache-2.0\n"
                + "framework\trepository\tpinned_commit\tforward_direction\treverse_evidence\t"
                + "mapping_route\tlicense\treuse_policy\tstatus\n"
                + "JWT\temweb/jwt\t0123456789abcdef0123456789abcdef01234567\tJAVA_TO_WEB\t"
                + "JAVA_UI\tSHAPE_ONLY\tGPL-OR-COMMERCIAL\tPATTERN_ONLY\tLICENSE_BOUNDARY_NO_SOURCE_COPY\n");
        write(root, CROSSWALK,
                "# SPDX-License-Identifier: Apache-2.0\n"
                + "semantic_id\tweb_source_hint\tnative_swt_nebula_owner\trwt_browser_owner\t"
                + "admission_profile\tblocking_contract\tstatus\n"
                + "LABEL\tspan\tSWT Label\tRWT Label\tSTATIC_TEXT\tUnicode proof open\tPROPOSED_NOT_RUN\n");
        write(root, SOURCE,
                "# SPDX-License-Identifier: Apache-2.0\n"
                + "package\trepository\trevision\tpath\tgit_blob\tlicense\t"
                + "source_classification\treceiver_name_hint\timplementation\n"
                + "widgets\tgwtproject/gwt\t0123456789abcdef0123456789abcdef01234567\t"
                + "src/Label.java\t0123456789abcdef0123456789abcdef01234567\t"
                + "Apache-2.0\tSOURCE_KIND_UNCLASSIFIED\tSWT Label\tNOT_IMPLEMENTED\n");
        write(root, RWT,
                "rank\tconcept_id\tconcept\trwt_route\trwt_class\trwt_status\t"
                + "resize_layout_delta\tnebula_primary\tprimary_file_rwt_errors\t"
                + "primary_bundle_rwt_errors\tprimary_missing_symbols\tother_candidates\tevidence\tnote\n"
                + "1\tLABEL\ttext label\tRAP_NATIVE\tLabel\tNOT_RUN test\tPROPERTY_DELTA\t"
                + "Label\t0\t0\t-\t-\tfixture\t-\n");
        write(root, PREFIX + "families/grid.properties", "schema=nebula-family/1\nfamily=grid\n");
    }

    private static void write(Path root, String file, String text) throws IOException {
        Path target = root.resolve(file);
        Files.createDirectories(target.getParent());
        Files.writeString(target, text, StandardCharsets.UTF_8);
    }

    private static void deleteTree(Path root) throws IOException {
        if (Files.exists(root)) {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        assertions++;
    }

    private static void checkReject(ThrowingStep step, String message) throws Exception {
        boolean refused = false;
        try {
            step.run();
        } catch (IllegalStateException expected) {
            refused = true;
        }
        check(refused, message);
    }

    @FunctionalInterface
    private interface ThrowingStep {
        void run() throws Exception;
    }
}
