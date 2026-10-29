// SPDX-License-Identifier: EPL-2.0
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Source-only, no-network Java 21 check for a pinned enhancement catalogue.
 * It does not approve restores, rewrite branches, or substitute for SWT tests.
 */
public final class M3PostmergeEnhancementAudit {
    private static final int FIELDS = 10;
    private static final int EXPECTED_ROWS = 12;
    private static final String HEADER = String.join("\t",
            "id", "priority", "pr_history", "current_owner", "observed_source",
            "provenance", "potential_enhancement", "dependency_or_semantic_risk",
            "qualification_gate", "decision");
    private static final Set<String> STATUSES = Set.of(
            "ABSENT_ON_MASTER", "DIFFERENT_MASTER", "EXACT_PRODUCT_PRESENT",
            "RESULTS_PRESENT_NEED_FULL_PARITY", "CURRENT_SOURCE_PRESENT",
            "MERGED_IN_MASTER", "MERGED_SOURCE_PRESENT", "FALSE_POSITIVE");
    private static final Set<String> DECISIONS = Set.of(
            "HOLD_API_CLOSURE", "HOLD_UNRESOLVED_METHOD", "HOLD_RAP_SESSION",
            "HOLD_TEST_OWNER", "KEEP_CURRENT_PLUS_SOURCE_RECIPE",
            "HOLD_QUALIFICATION", "KEEP_CURRENT", "KEEP_CURRENT_NEW_IDENTITY_IDEA",
            "CATALOGUE_WAVES", "NO_RECOVERY");

    private M3PostmergeEnhancementAudit() {
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static int validate(List<String> lines) {
        Set<String> identifiers = new HashSet<>();
        Set<String> withheld = Set.of(
                "ABSENT_ON_MASTER", "DIFFERENT_MASTER", "RESULTS_PRESENT_NEED_FULL_PARITY");
        int observed = 0;
        boolean header = false;
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            if (!header) {
                require(line.equals(HEADER), "Unexpected catalogue schema");
                header = true;
                continue;
            }
            String[] fields = line.split("\t", -1);
            require(fields.length == FIELDS, "Wrong column count: " + line);
            for (String field : fields) {
                require(!field.isBlank(), "Empty recovery field " + fields[0]);
                require(!field.contains("\r") && !field.contains("\n"),
                        "Malformed field");
            }
            require(identifiers.add(fields[0]), "Duplicate candidate " + fields[0]);
            require(fields[1].matches("P[0-3]"), "Invalid priority");
            require(fields[2].matches("[0-9]+(?:,[0-9]+)*"), "Invalid PR references");
            require(STATUSES.contains(fields[4]), "Unsupported source classification");
            require(DECISIONS.contains(fields[9]), "Unsupported admission decision");
            if (withheld.contains(fields[4])) {
                require(fields[9].startsWith("HOLD_"),
                        "Unqualified historical source may not auto-promote");
            }
            if (fields[4].equals("FALSE_POSITIVE")) {
                require(fields[9].equals("NO_RECOVERY"),
                        "A proven non-loss must not be restored");
            }
            if (fields[4].equals("CURRENT_SOURCE_PRESENT")) {
                require(fields[9].startsWith("KEEP_CURRENT"),
                        "Current source owner must be preserved");
            }
            require(!fields[6].toLowerCase().contains("cherry-pick"),
                    "No blind cherry-pick authorization");
            observed++;
        }
        require(header, "Missing catalogue heading");
        require(observed == EXPECTED_ROWS, "Expected 12 reviewed candidates, got " + observed);
        require(identifiers.containsAll(Set.of(
                "GRID-ROW-VIEW", "GRID-SORT-HEADER", "RWT-GC-FLAGS",
                "RWT-CANVAS", "RWT-PROTOCOL", "NATIVE-SWT-RECEIVERS",
                "WEB-ADMISSION", "VIRTUAL-TABLE", "VIRTUAL-TREE",
                "VIEWPORT-INVENTORY", "BROWSER-LIFECYCLE", "GALLERY-LINE-ENDINGS")),
                "Missing required recovery categories");
        return observed;
    }

    private static void selfTest() {
        String safe = HEADER + "\n" + String.join("\t",
                "X", "P0", "262", "widgets/grid/X.java",
                "ABSENT_ON_MASTER", "hash", "Converge atoms", "API mismatch",
                "Native SWT test", "HOLD_API_CLOSURE");
        require(validateSingle(safe) == 1, "One-row sample refused");
        expectRefusal(safe.replace("HOLD_API_CLOSURE", "KEEP_CURRENT"));
        expectRefusal(safe.replace("ABSENT_ON_MASTER", "FALSE_POSITIVE"));
        expectRefusal(safe + "\n" + safe.substring(safe.indexOf("\n") + 1));
        expectRefusal(safe.replace("Native SWT test", ""));
        System.out.println("PASS postmerge audit self-test (one admit, four refusals)");
    }

    private static int validateSingle(String input) {
        int rows = 0;
        boolean heading = false;
        for (String line : input.split("\n")) {
            if (!heading) {
                require(line.equals(HEADER), "Invalid header");
                heading = true;
                continue;
            }
            String[] fields = line.split("\t", -1);
            require(fields.length == FIELDS, "Invalid row width");
            require(fields[1].matches("P[0-3]"), "Invalid priority");
            require(STATUSES.contains(fields[4]), "Invalid status");
            require(DECISIONS.contains(fields[9]), "Invalid decision");
            if (fields[4].equals("ABSENT_ON_MASTER")
                    || fields[4].equals("DIFFERENT_MASTER")) {
                require(fields[9].startsWith("HOLD_"), "Unqualified recovery");
            }
            if (fields[4].equals("FALSE_POSITIVE")) {
                require(fields[9].equals("NO_RECOVERY"), "False loss restored");
            }
            for (String field : fields) {
                require(!field.isBlank(), "Blank evidence");
            }
            rows++;
        }
        require(heading, "Missing header");
        require(rows == 1, "Expected one independent test row");
        return rows;
    }

    private static void expectRefusal(String value) {
        boolean refused = false;
        try {
            validateSingle(value);
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        require(refused, "Unsafe sample was admitted");
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && "--self-test".equals(args[0])) {
            selfTest();
            return;
        }
        require(args.length == 1, "Usage: audit --self-test|POSTMERGE_ENHANCEMENT_IDEAS.tsv");
        Path path = Path.of(args[0]);
        require(!Files.isSymbolicLink(path)
                && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                && Files.size(path) <= 262_144, "Untrusted catalogue input");
        int rows = validate(Files.readAllLines(path, StandardCharsets.UTF_8));
        System.out.println("PASS postmerge recovery candidates=" + rows
                + " with no automatic restoration");
    }
}
