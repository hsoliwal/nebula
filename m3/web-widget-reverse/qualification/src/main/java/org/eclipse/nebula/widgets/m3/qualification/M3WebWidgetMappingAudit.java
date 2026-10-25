// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.widgets.m3.qualification;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Read-only gate for cross-framework UI mapping metadata, not a widget translator.
 *
 * <p>Reject unsupported semantic promotion, source drift, ambiguous TSV, unsafe
 * licensing, symlink escape and corpus-count changes before any OpenRewrite
 * receiving stage. An accepted catalogue remains a candidate: no SWT/Java/RWT
 * behavior, native pixels, event equivalence or ABI is inferred.</p>
 */
public final class M3WebWidgetMappingAudit {
    private static final int MAX_BYTES = 1_048_576;
    private static final Counts PRODUCT_COUNTS = new Counts(16, 47, 45, 369, 68);
    // Pinned to the exact target PR #277 input byte streams, not to row-count inference.
    private static final String DONOR_SHA256 =
            "92c31ca254c60feb7f78a236fea8189a5a7b9206cdac1f09d6e2c58765434064";
    private static final String SEMANTIC_SHA256 =
            "4fcff6da7c967284762879334e168bf7abc136e5fa3aef90a1285dbbd48fabe6";
    private static final String WEB_SOURCES_SHA256 =
            "e7ea639b08424cafdc8f59575ee650aef85e284e0cf96d9f287ecbbb9cca9233";
    private static final String RWT_COLUMN_SHA256 =
            "5d700c1d4064ab47b8251b7d1a591cf85befdf416f75e6d7cfae29ae5b7164b0";
    // Official Git tree object: all 45 property files have mode 100644.
    private static final String FAMILIES_GIT_TREE =
            "54942ccf0f9eb3253def12dfcae13dd528681ba0";
    private static final String PREFIX = "m3/web-widget-reverse/";
    private static final String RWT = "docs/m3/rwt/RWT_COLUMN.tsv";
    private static final Set<String> PROFILES = Set.of(
            "STATIC_TEXT", "STATIC_DISABLED_ONLY", "STATIC_STATE_ONLY",
            "DECLARATIVE_CONDITIONAL", "VIRTUAL_COLLECTION_CONDITIONAL",
            "PAINT_OPERATION_GATED", "BROWSER_OPT_IN_ONLY", "HOLD_DYNAMIC", "BLOCKED");
    private static final String DONOR_HEADER =
            "framework\trepository\tpinned_commit\tforward_direction\treverse_evidence\t"
            + "mapping_route\tlicense\treuse_policy\tstatus";
    private static final String SEMANTIC_HEADER =
            "semantic_id\tweb_source_hint\tnative_swt_nebula_owner\trwt_browser_owner\t"
            + "admission_profile\tblocking_contract\tstatus";
    private static final String SOURCE_HEADER =
            "package\trepository\trevision\tpath\tgit_blob\tlicense\t"
            + "source_classification\treceiver_name_hint\timplementation";
    private static final String RWT_HEADER =
            "rank\tconcept_id\tconcept\trwt_route\trwt_class\trwt_status\t"
            + "resize_layout_delta\tnebula_primary\tprimary_file_rwt_errors\t"
            + "primary_bundle_rwt_errors\tprimary_missing_symbols\tother_candidates\tevidence\tnote";

    /** Exact baseline expectations; changed counts need independently reviewed custody. */
    record Counts(int donors, int semantics, int families, int sources, int rwt) {
        Counts {
            if (donors < 1 || semantics < 1 || families < 1 || sources < 1 || rwt < 1) {
                throw new IllegalArgumentException("invalid corpus budget");
            }
        }
    }

    /** Provenance-only outcome; never a statement of widget compatibility. */
    record Receipt(Counts counts, String sha256) {}

    /** Source identity for the exact family set, independent of widget behavior. */
    record FamilyEvidence(int count, String gitTreeSha1) {}

    private M3WebWidgetMappingAudit() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: M3WebWidgetMappingAudit <Nebula repository root>");
        }
        Counts expected = PRODUCT_COUNTS;
        Receipt receipt = audit(Path.of(args[0]), expected);
        System.out.println("schema=m3-web-widget-mapping-audit/1");
        System.out.println("donors=" + receipt.counts().donors());
        System.out.println("semantics=" + receipt.counts().semantics());
        System.out.println("families=" + receipt.counts().families());
        System.out.println("web_sources=" + receipt.counts().sources());
        System.out.println("rwt_concepts=" + receipt.counts().rwt());
        System.out.println("source_sha256=" + receipt.sha256());
        System.out.println("result=CANDIDATE_HOLD");
    }

    static Receipt audit(Path requested, Counts expected) throws IOException {
        Path root = requested.toRealPath();
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("repository root is not a directory");
        }
        MessageDigest sha = sha256();
        boolean product = PRODUCT_COUNTS.equals(expected);
        List<String[]> donors = table(root, sha, PREFIX + "FRAMEWORK_DONORS_20261025.tsv",
                DONOR_HEADER, 9, false, product ? DONOR_SHA256 : null);
        List<String[]> semantics = table(root, sha, PREFIX + "WEB_SEMANTIC_CROSSWALK_20261025.tsv",
                SEMANTIC_HEADER, 7, false, product ? SEMANTIC_SHA256 : null);
        List<String[]> sources = table(root, sha, PREFIX + "WEB_COMPONENT_SOURCES.tsv",
                SOURCE_HEADER, 9, false, product ? WEB_SOURCES_SHA256 : null);
        List<String[]> rwt = table(root, sha, RWT, RWT_HEADER, 14, true,
                product ? RWT_COLUMN_SHA256 : null);
        FamilyEvidence families = inspectFamilies(root, sha);
        Counts actual = new Counts(donors.size(), semantics.size(), families.count(),
                sources.size(), rwt.size());
        if (!actual.equals(expected)) {
            throw new IllegalStateException("baseline counts drift: expected=" + expected + ", got=" + actual);
        }
        if (product && !FAMILIES_GIT_TREE.equals(families.gitTreeSha1())) {
            throw new IllegalStateException("Nebula family Git tree changed: " + families.gitTreeSha1());
        }
        verifyDonors(donors);
        verifySemantics(semantics);
        verifySources(sources);
        verifyRwt(rwt, expected.rwt() == 68);
        return new Receipt(actual, HexFormat.of().formatHex(sha.digest()));
    }

    private static List<String[]> table(Path root, MessageDigest sha, String name,
            String header, int columns, boolean allowBlank, String expectedSha256) throws IOException {
        String text = read(root, sha, name);
        if (expectedSha256 != null && !expectedSha256.equals(sha256Text(text))) {
            throw new IllegalStateException("source SHA-256 drift: " + name);
        }
        String[] lines = text.split("\n", -1);
        List<String[]> records = new ArrayList<>();
        boolean seenHeader = false;
        for (int n = 0; n < lines.length; n++) {
            String line = lines[n];
            if (n == lines.length - 1 && line.isEmpty()) break;
            if (!seenHeader && line.startsWith("#")) continue;
            if (!seenHeader) {
                if (!line.equals(header)) throw new IllegalStateException("schema drift: " + name);
                seenHeader = true;
                continue;
            }
            if (line.isBlank() || line.startsWith("#")) {
                throw new IllegalStateException("unexpected blank/comment row: " + name + ":" + (n + 1));
            }
            String[] row = line.split("\t", -1);
            if (row.length != columns) {
                throw new IllegalStateException("column drift: " + name + ":" + (n + 1));
            }
            for (String cell : row) {
                if ((!allowBlank && cell.isBlank()) || !cell.equals(cell.strip())
                        || cell.chars().anyMatch(Character::isISOControl)) {
                    throw new IllegalStateException("invalid cell: " + name + ":" + (n + 1));
                }
            }
            records.add(row);
            if (records.size() > 10_000) throw new IllegalStateException("TSV row budget");
        }
        if (!seenHeader || records.isEmpty()) throw new IllegalStateException("empty table: " + name);
        return records;
    }

    private static void verifyDonors(List<String[]> rows) {
        Set<String> names = new HashSet<>();
        for (String[] row : rows) {
            if (!names.add(row[0]) || !row[0].matches("[A-Z][A-Z0-9_]*")
                    || !row[1].matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                    || !row[2].matches("[0-9a-f]{40}") || !row[8].matches("[A-Z0-9_]+")) {
                throw new IllegalStateException("unqualified donor identity");
            }
            if (row[6].contains("GPL") && !row[7].equals("PATTERN_ONLY")) {
                throw new IllegalStateException("GPL donor code cannot be imported");
            }
            if (row[6].equals("REVIEW_REQUIRED") && !row[7].equals("LICENCE_FILE_REVIEW")) {
                throw new IllegalStateException("missing per-file license review");
            }
            if (row[0].equals("JWT") && !row[7].equals("PATTERN_ONLY")) {
                throw new IllegalStateException("JWt source import prohibited");
            }
        }
    }

    private static void verifySemantics(List<String[]> rows) {
        Set<String> ids = new HashSet<>();
        for (String[] row : rows) {
            if (!row[0].matches("[A-Z][A-Z0-9_]*") || !ids.add(row[0])
                    || !PROFILES.contains(row[4]) || !row[6].equals("PROPOSED_NOT_RUN")) {
                throw new IllegalStateException("semantic promotion or duplicate identity without proof");
            }
            if (row[4].equals("BLOCKED") && !row[3].startsWith("NO_")
                    && !row[3].contains("not implemented")) {
                throw new IllegalStateException("blocked RWT owner must be explicitly absent");
            }
        }
    }

    private static void verifySources(List<String[]> rows) {
        Set<String> paths = new HashSet<>();
        for (String[] row : rows) {
            if (!row[2].matches("[0-9a-f]{40}") || !row[4].matches("[0-9a-f]{40}")
                    || !paths.add(row[1] + ":" + row[3]) || !row[8].equals("NOT_IMPLEMENTED")) {
                throw new IllegalStateException("web source identity or implementation status drift");
            }
        }
    }

    private static void verifyRwt(List<String[]> rows, boolean baseline) {
        Set<String> ids = new HashSet<>();
        Map<String, Integer> routes = new HashMap<>();
        int nextRank = 1;
        for (String[] row : rows) {
            if (!ids.add(row[1]) || !row[0].equals(Integer.toString(nextRank++))) {
                throw new IllegalStateException("RWT rank/concept identity drift");
            }
            String route = row[3].startsWith("BLOCKED(") ? "BLOCKED" : row[3];
            if (!Set.of("RAP_NATIVE", "CANVAS_GC_REPLAY", "REBUILD_AS_PAINTER", "BLOCKED")
                    .contains(route) || !(row[5].startsWith("NOT_RUN") || row[5].startsWith("BLOCKED"))) {
                throw new IllegalStateException("unqualified RWT status/route");
            }
            routes.merge(route, 1, Integer::sum);
        }
        if (baseline && !(routes.getOrDefault("RAP_NATIVE", 0) == 30
                && routes.getOrDefault("CANVAS_GC_REPLAY", 0) == 19
                && routes.getOrDefault("REBUILD_AS_PAINTER", 0) == 17
                && routes.getOrDefault("BLOCKED", 0) == 2)) {
            throw new IllegalStateException("RWT route census drift");
        }
    }

    static FamilyEvidence inspectFamilies(Path root, MessageDigest sha) throws IOException {
        Path directory = root.resolve(PREFIX + "families");
        if (Files.isSymbolicLink(directory) || !Files.isDirectory(directory)) {
            throw new IllegalStateException("missing real Nebula family directory");
        }
        List<Path> paths;
        try (Stream<Path> entries = Files.list(directory)) {
            paths = entries.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
        }
        int count = 0;
        ByteArrayOutputStream gitTreeEntries = new ByteArrayOutputStream();
        for (Path path : paths) {
            if (Files.isSymbolicLink(path) || !Files.isRegularFile(path)
                    || !path.getFileName().toString().matches("[a-z0-9]+[.]properties")) {
                throw new IllegalStateException("foreign Nebula family path: " + path);
            }
            String expected = path.getFileName().toString().replace(".properties", "");
            String value = read(root, sha, PREFIX + "families/" + path.getFileName());
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            gitTreeEntries.writeBytes(("100644 " + path.getFileName() + "\0")
                    .getBytes(StandardCharsets.UTF_8));
            gitTreeEntries.writeBytes(gitObjectSha1("blob", bytes));
            if (Arrays.stream(value.split("\n")).filter(s -> s.startsWith("family="))
                    .filter(s -> s.equals("family=" + expected)).count() != 1L) {
                throw new IllegalStateException("family identity drift: " + expected);
            }
            count++;
        }
        return new FamilyEvidence(count, HexFormat.of().formatHex(
                gitObjectSha1("tree", gitTreeEntries.toByteArray())));
    }

    /** Canonical Git object bytes, used only as an identity witness, not a security grant. */
    private static byte[] gitObjectSha1(String type, byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update((type + " " + content.length + "\0")
                    .getBytes(StandardCharsets.US_ASCII));
            digest.update(content);
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    /** Package-local deterministic SHA-256 oracle, also tested without production fixtures. */
    static String sha256Text(String source) {
        return HexFormat.of().formatHex(sha256().digest(
                source.getBytes(StandardCharsets.UTF_8)));
    }

    private static String read(Path root, MessageDigest sha, String relative) throws IOException {
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root) || Files.isSymbolicLink(file)
                || !Files.isRegularFile(file) || !file.toRealPath().startsWith(root)) {
            throw new IllegalStateException("unsafe source path: " + relative);
        }
        long size = Files.size(file);
        if (size < 1 || size > MAX_BYTES) throw new IllegalStateException("source byte budget: " + relative);
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length != size) throw new IllegalStateException("unstable read: " + relative);
        String decoded;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException invalid) {
            throw new IllegalStateException("invalid UTF-8: " + relative, invalid);
        }
        if (decoded.indexOf('\r') >= 0 || decoded.indexOf('\0') >= 0
                || decoded.indexOf('\ufeff') >= 0 || !decoded.endsWith("\n")) {
            throw new IllegalStateException("ambiguous source encoding: " + relative);
        }
        sha.update(relative.getBytes(StandardCharsets.UTF_8));
        sha.update((byte) 0);
        sha.update(bytes);
        sha.update((byte) 0);
        return decoded;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
