// SPDX-License-Identifier: Apache-2.0
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Java21 source-only verifier for the 2026-10-29 Nebula merge-recovery census.
 * No GitHub credentials, unbounded network work, source rewrites or Git ref changes.
 * Verifies audited inputs; a green result never authorizes cherry-picking code.
 */
public final class M3NebulaMergeRecoveryAudit {
    private static final String MASTER = "82b7a52c756e26d6075293043766df23291c00a5";
    private static final Pattern SHA = Pattern.compile("[0-9a-f]{40}");
    private static final int PRS = 297, STACKS = 26, SOURCE = 82, CANDIDATES = 17;

    private M3NebulaMergeRecoveryAudit() { }

    static final class Tsv {
        final String[] head;
        final List<String[]> rows;
        final Map<String, Integer> columns = new HashMap<>();
        Tsv(String data) {
            var lines = data.lines().filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
            require(!lines.isEmpty(), "TSV header absent");
            head = lines.get(0).split("\t", -1);
            for (int i = 0; i < head.length; i++) {
                require(!head[i].isEmpty() && columns.putIfAbsent(head[i], i) == null,
                        "duplicate/blank column");
            }
            rows = new ArrayList<>(Math.max(0, lines.size() - 1));
            for (int i = 1; i < lines.size(); i++) {
                String[] row = lines.get(i).split("\t", -1);
                require(row.length == head.length, "bad column count at data row " + i);
                rows.add(row);
            }
        }
        String get(String[] row, String field) {
            Integer i = columns.get(field);
            require(i != null, "missing required column " + field);
            return row[i];
        }
        static Tsv read(Path path) throws IOException {
            require(Files.isRegularFile(path) && !Files.isSymbolicLink(path),
                    "untrusted input file " + path);
            require(Files.size(path) < 2_000_000, "TSV byte budget");
            return new Tsv(Files.readString(path, StandardCharsets.UTF_8));
        }
    }

    static void require(boolean ok, String message) {
        if (!ok) throw new IllegalArgumentException(message);
    }

    static int positive(String value, String field) {
        require(value.matches("[0-9]{1,8}"), "invalid numeric " + field);
        return Integer.parseInt(value);
    }

    static void sha(String value, String field) {
        require(SHA.matcher(value).matches(), "invalid Git SHA " + field);
    }

    static void unique(Set<String> seen, String key, String category) {
        require(seen.add(key), "duplicate " + category + ": " + key);
    }

    static Map<String, String[]> prs(Tsv t) {
        require(t.rows.size() == PRS, "incomplete PR_HISTORY: " + t.rows.size());
        Map<String, String[]> result = new HashMap<>();
        int prev = 0, nonMaster = 0, closed = 0, master = 0;
        for (String[] r : t.rows) {
            String number = t.get(r, "number");
            int n = positive(number, "PR number");
            require(n == prev + 1, "non-contiguous PR number " + n);
            prev = n;
            sha(t.get(r, "head_sha"), "PR head");
            String status = t.get(r, "status");
            String base = t.get(r, "base_ref");
            if ("MERGED_TO_MASTER".equals(status)) {
                require("master".equals(base), "wrong mainline base for PR " + n);
                master++;
            } else if ("MERGED_TO_NONMASTER".equals(status)) {
                require(!"master".equals(base), "wrong feature base for PR " + n);
                nonMaster++;
            } else if ("CLOSED_UNMERGED".equals(status)) {
                closed++;
            } else throw new IllegalArgumentException("unexpected PR status: " + status);
            require(t.get(r, "url").equals("https://github.com/hsoliwal/nebula/pull/" + n),
                    "PR URL mismatch " + n);
            result.put(number, r);
        }
        require(master == 247 && nonMaster == 26 && closed == 24,
                "snapshot PR status drift: " + master + "/" + nonMaster + "/" + closed);
        return result;
    }

    static int stacks(Tsv t, Tsv history, Map<String, String[]> prRows) {
        require(t.rows.size() == STACKS, "incomplete STACK_MERGES");
        Set<String> seen = new HashSet<>();
        int uniqueHeads = 0;
        for (String[] r : t.rows) {
            String n = t.get(r, "number");
            unique(seen, n, "stacked PR");
            String[] pr = prRows.get(n);
            require(pr != null && "MERGED_TO_NONMASTER".equals(history.get(pr, "status")),
                    "stack reference not a non-master merged PR " + n);
            require(t.get(r, "pr_head_sha").equals(history.get(pr, "head_sha")),
                    "branch head not source-pinned to PR " + n);
            require(MASTER.equals(t.get(r, "master_head_sha")), "changed master snapshot");
            int ahead = positive(t.get(r, "ahead"), "ahead");
            positive(t.get(r, "behind"), "behind");
            String decision = t.get(r, "classification");
            require(decision.equals(ahead > 0 ? "UNRECEIVED_PR_HEAD_ANCESTRY_HOLD"
                    : "NO_UNIQUE_PR_HEAD_COMMITS"), "ancestry classification drift " + n);
            if (ahead > 0) uniqueHeads++;
        }
        require(uniqueHeads == 15, "expected 15 uniquely divergent PR-head histories");
        return uniqueHeads;
    }

    static Map<String, String[]> receipts(Tsv t) {
        require(t.rows.size() == SOURCE, "incomplete BRANCH_SOURCE_RECEIPTS");
        Map<String, String[]> source = new HashMap<>();
        int absentGrid = 0, absentRwt = 0, different = 0;
        for (String[] r : t.rows) {
            String lane = t.get(r, "family"), path = t.get(r, "path");
            require(source.putIfAbsent(path, r) == null, "duplicate source receipt path " + path);
            sha(t.get(r, "branch_head"), "branch head");
            sha(t.get(r, "branch_blob"), "branch blob");
            require(MASTER.equals(t.get(r, "master_head")), "unqualified master head");
            String old = t.get(r, "master_blob"), status = t.get(r, "status");
            if ("ABSENT_MASTER".equals(status)) {
                require("-".equals(old), "absent path has master blob");
                if ("GRID_BRANCH".equals(lane)) absentGrid++;
                else if ("RWT_BRANCH".equals(lane)) absentRwt++;
                else throw new IllegalArgumentException("unknown source lane");
            } else if ("DIFFERENT_MASTER".equals(status)) {
                sha(old, "master blob");
                require(!old.equals(t.get(r, "branch_blob")), "different status but identical blob");
                different++;
            } else if ("EXACT_MASTER".equals(status)) {
                require(old.equals(t.get(r, "branch_blob")), "exact status but different blob");
            } else throw new IllegalArgumentException("unrecognized source status " + status);
        }
        require(absentGrid == 4 && absentRwt == 65 && different == 13,
                "source census changed: " + absentGrid + "/" + absentRwt + "/" + different);
        return source;
    }

    static void candidates(Tsv t, Tsv sourceTsv, Map<String, String[]> source) {
        require(t.rows.size() == CANDIDATES, "incomplete RECOVERY_MAP");
        Set<String> ids = new HashSet<>();
        for (String[] r : t.rows) {
            String id = t.get(r, "id"), status = t.get(r, "source_status");
            unique(ids, id, "candidate ID");
            require(t.get(r, "priority").matches("P[0-3]"), "invalid priority " + id);
            require(!t.get(r, "qualification_gate").isBlank(), "no qualification gate " + id);
            require(!"MERGE_NOW".equals(t.get(r, "decision")), "blind merge requested " + id);
            if (status.equals("ABSENT_MASTER") || status.equals("DIFFERENT_MASTER")
                    || status.equals("HISTORICAL_UNRESOLVED_SYMBOL")) {
                String path = t.get(r, "path"), sourceSha = t.get(r, "branch_blob");
                String[] proof = source.get(path);
                require(proof != null, "candidate absent from source receipts " + id);
                require(sourceSha.equals(sourceTsv.get(proof, "branch_blob")),
                        "candidate blob not pinned " + id);
                if (status.equals("ABSENT_MASTER") || status.equals("HISTORICAL_UNRESOLVED_SYMBOL")) {
                    require("ABSENT_MASTER".equals(sourceTsv.get(proof, "status")),
                            "candidate absence not verified " + id);
                } else require("DIFFERENT_MASTER".equals(sourceTsv.get(proof, "status")),
                        "candidate change not verified " + id);
            }
            if ("EXACT_MASTER".equals(status)) {
                require(t.get(r, "branch_blob").equals(t.get(r, "master_blob")),
                        "claimed exact source differs " + id);
            }
        }
    }

    private static void selfTest() {
        Tsv x = new Tsv("# SPDX-License-Identifier: Apache-2.0\na\tb\n1\tfirst\n2\tsecond\n");
        require(x.rows.size() == 2 && "second".equals(x.get(x.rows.get(1), "b")),
                "TSV decode");
        checkFailure(() -> new Tsv("a\ta\n1\t2\n"));
        checkFailure(() -> new Tsv("a\tb\n1\n"));
        checkFailure(() -> sha("0000", "fake"));
        Set<String> seen = new HashSet<>();
        unique(seen, "atom", "test");
        checkFailure(() -> unique(seen, "atom", "test"));
        checkFailure(() -> positive("-1", "bad"));
        System.out.println("PASS M3 Nebula merge-recovery TSV self-test");
    }

    private static void checkFailure(Runnable task) {
        try { task.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Negative validation test did not refuse");
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--self-test".equals(args[0])) { selfTest(); return; }
        require(args.length == 4,
                "usage: M3NebulaMergeRecoveryAudit PR_HISTORY STACK_MERGES BRANCH_SOURCE_RECEIPTS RECOVERY_MAP");
        Tsv history = Tsv.read(Path.of(args[0]));
        Tsv stack = Tsv.read(Path.of(args[1]));
        Tsv source = Tsv.read(Path.of(args[2]));
        Tsv map = Tsv.read(Path.of(args[3]));
        Map<String, String[]> rows = prs(history);
        int heads = stacks(stack, history, rows);
        Map<String, String[]> receipts = receipts(source);
        candidates(map, source, receipts);
        System.out.println("PASS NEBULA_MERGE_RECOVERY PRs=" + history.rows.size()
                + " nonmaster=" + stack.rows.size() + " uniquePRHeads=" + heads
                + " branchPaths=" + source.rows.size() + " candidates=" + map.rows.size());
        System.out.println("HOLD: source absence or unique history is NOT a verified functional regression");
    }
}
