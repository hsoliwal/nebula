// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.inventory;

import com.synexia.job.IProgressMonitor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only commit-chain and merge-tree auditor for the canonical develop evidence history.
 *
 * <p>The auditor invokes only Git read commands. It never checks out, merges, rebases, resets,
 * commits, updates refs, or pushes. Historical blobs are evidence candidates only; this class has
 * no replacement or promotion authority.</p>
 */
public final class DevelopHistoryAuditor {
    private static final String HEADER_PREFIX = "@M3@";
    private static final char FIELD_SEPARATOR = '\u001f';
    private static final String ZERO = "0";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);
    private static final Pattern RAW_CHANGE = Pattern.compile(
            "^:[0-7]{6} [0-7]{6} ([0-9a-f]+) ([0-9a-f]+) ([A-Z][0-9]*)\\t(.+)$");
    private static final Pattern TREE_ROW = Pattern.compile(
            "^[0-7]{6}\\s+blob\\s+([0-9a-f]+)\\t(.+)$");

    private static final Set<String> PERFORMANCE_WORDS = Set.of(
            "perf", "performance", "optim", "optimiz", "fast", "speed", "zero-copy",
            "precompute", "cache", "index", "simd", "jni", "gpu", "allocation", "intern");
    private static final Set<String> RESTORE_WORDS = Set.of(
            "restore", "recover", "replay", "reintroduce", "resurrect");
    private static final Set<String> CHANGE_WORDS = Set.of(
            "feat", "fix", "refactor", "converge", "enhance", "improve");

    /** Stable history disposition. SUPERSEDED_PROVEN is intentionally never inferred here. */
    public enum HistoryState {
        SURVIVES,
        SUPERSEDED_PROVEN,
        LOST,
        OVERWRITTEN_UNPROVEN,
        REVERTED,
        RESTORED,
        REPLAYED,
        PROOF_REQUIRED
    }

    /** Commit header/tree evidence, including merge commits with no raw file delta. */
    public record CommitEvidence(
            String commit,
            String parents,
            String tree,
            String committedAt,
            String subject) {
        public CommitEvidence {
            commit = required(commit, "commit");
            parents = Objects.requireNonNullElse(parents, "").strip();
            tree = normalizeBlob(tree);
            if (tree.equals(ZERO)) throw new IllegalArgumentException("tree");
            committedAt = required(committedAt, "committedAt");
            subject = Objects.requireNonNullElse(subject, "");
        }

        public List<String> parentList() {
            return parents.isBlank() ? List.of() : List.of(parents.split(" +"));
        }

        public boolean merge() {
            return parentList().size() > 1;
        }
    }

    public enum MergeDisposition {
        SOURCE_APPLIED,
        ANCESTRY_ONLY,
        FIRST_PARENT_TREE_UNAVAILABLE
    }

    /** Merge-level source-tree evidence; this has no merge or replacement authority. */
    public record MergeEvidence(
            String commit,
            String firstParent,
            int parentCount,
            String tree,
            String firstParentTree,
            String committedAt,
            String subject,
            MergeDisposition disposition,
            String rationale) {
        public MergeEvidence {
            commit = required(commit, "commit");
            firstParent = required(firstParent, "firstParent");
            if (parentCount < 2) throw new IllegalArgumentException("parentCount");
            tree = normalizeBlob(tree);
            firstParentTree = normalizeBlob(firstParentTree);
            committedAt = required(committedAt, "committedAt");
            subject = Objects.requireNonNullElse(subject, "");
            disposition = Objects.requireNonNull(disposition, "disposition");
            rationale = required(rationale, "rationale");
        }

        public boolean sourceTreeChanged() {
            return disposition == MergeDisposition.SOURCE_APPLIED;
        }
    }

    /** Parsed bounded history, including commits whose merge introduced no raw file delta. */
    public record ParsedHistory(List<CommitEvidence> commits, List<Change> changes) {
        public ParsedHistory {
            commits = List.copyOf(commits);
            changes = List.copyOf(changes);
        }
    }

    /** One immutable raw file transition extracted from Git ancestry. */
    public record Change(
            String commit,
            String parents,
            String committedAt,
            String subject,
            String status,
            String path,
            String oldBlob,
            String newBlob) {
        public Change {
            commit = required(commit, "commit");
            parents = Objects.requireNonNullElse(parents, "");
            committedAt = required(committedAt, "committedAt");
            subject = Objects.requireNonNullElse(subject, "");
            status = required(status, "status");
            path = normalizePath(required(path, "path"));
            oldBlob = normalizeBlob(oldBlob);
            newBlob = normalizeBlob(newBlob);
        }
    }

    /** Current blob identity on the audited ref. */
    public record CurrentBlob(String path, String blob) {
        public CurrentBlob {
            path = normalizePath(required(path, "path"));
            blob = normalizeBlob(blob);
            if (blob.equals(ZERO)) throw new IllegalArgumentException("current blob is zero");
        }
    }

    /**
     * Candidate historical atom. This is evidence only; it never authorizes replacement.
     */
    public record Candidate(
            int score,
            HistoryState state,
            String signal,
            String commit,
            String committedAt,
            String subject,
            String path,
            String historicalBlob,
            String currentBlob,
            String rationale) {
        public Candidate {
            if (score < 0) throw new IllegalArgumentException("score");
            state = Objects.requireNonNull(state, "state");
            signal = required(signal, "signal");
            commit = required(commit, "commit");
            committedAt = required(committedAt, "committedAt");
            subject = Objects.requireNonNullElse(subject, "");
            path = normalizePath(required(path, "path"));
            historicalBlob = normalizeBlob(historicalBlob);
            currentBlob = normalizeBlob(currentBlob);
            rationale = required(rationale, "rationale");
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    /** Complete bounded audit result. */
    public record Report(
            String ref,
            int maxCommits,
            List<CommitEvidence> commits,
            List<Change> changes,
            List<CurrentBlob> currentBlobs,
            List<Candidate> candidates,
            List<MergeEvidence> merges,
            String root) {
        public Report {
            ref = required(ref, "ref");
            if (maxCommits < 1) throw new IllegalArgumentException("maxCommits");
            commits = List.copyOf(commits);
            changes = List.copyOf(changes);
            currentBlobs = List.copyOf(currentBlobs);
            candidates = List.copyOf(candidates);
            merges = List.copyOf(merges);
            root = required(root, "root");
        }
    }

    public Report audit(
            Path repositoryRoot,
            String ref,
            int maxCommits,
            List<String> pathSpecs,
            IProgressMonitor monitor) throws IOException {
        Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot")
                .toAbsolutePath().normalize();
        if (!Files.exists(root.resolve(".git"))) {
            throw new IllegalArgumentException("not a Git worktree: " + root);
        }
        String checkedRef = required(ref, "ref");
        if (maxCommits < 1) throw new IllegalArgumentException("maxCommits");
        List<String> paths = pathSpecs == null
                ? List.of()
                : pathSpecs.stream().map(DevelopHistoryAuditor::normalizePath).sorted().toList();
        IProgressMonitor progress = monitor == null ? IProgressMonitor.noop() : monitor;
        progress.beginTask("Audit develop history", 4);
        try {
            progress.checkCanceled();
            requireCompleteHistory(root);
            String rawLog = runGit(root, historyCommand(checkedRef, maxCommits, paths), DEFAULT_TIMEOUT);
            progress.worked(1);

            progress.checkCanceled();
            String tree = runGit(root, treeCommand(checkedRef, paths), DEFAULT_TIMEOUT);
            progress.worked(1);

            progress.checkCanceled();
            ParsedHistory history = parseHistory(rawLog);
            List<CurrentBlob> current = parseLsTree(tree);
            progress.worked(1);

            progress.checkCanceled();
            List<Candidate> candidates = classify(history.changes(), current);
            List<MergeEvidence> merges = classifyMerges(history.commits());
            String rootHash =
                    reportRoot(
                            checkedRef,
                            maxCommits,
                            history.commits(),
                            history.changes(),
                            current,
                            candidates,
                            merges);
            progress.worked(1);
            return new Report(
                    checkedRef,
                    maxCommits,
                    history.commits(),
                    history.changes(),
                    current,
                    candidates,
                    merges,
                    rootHash);
        } finally {
            progress.done();
        }
    }

    static List<String> historyCommand(String ref, int maxCommits, List<String> paths) {
        ArrayList<String> command = new ArrayList<>();
        command.add("log");
        command.add(required(ref, "ref"));
        command.add("--topo-order");
        command.add("--date-order");
        command.add("--max-count=" + maxCommits);
        command.add("--format=" + HEADER_PREFIX + "%H%x1f%P%x1f%T%x1f%cI%x1f%s");
        command.add("--raw");
        command.add("--no-abbrev");
        command.add("--full-index");
        command.add("--no-renames");
        command.add("--");
        command.addAll(paths);
        return List.copyOf(command);
    }

    static List<String> treeCommand(String ref, List<String> paths) {
        ArrayList<String> command = new ArrayList<>(
                List.of("ls-tree", "-r", "--full-tree", required(ref, "ref"), "--"));
        command.addAll(paths);
        return List.copyOf(command);
    }

    static boolean isReadOnlyGitCommand(List<String> command) {
        if (command == null || command.isEmpty()) return false;
        return Set.of("log", "ls-tree", "show", "cat-file", "rev-parse", "merge-base")
                .contains(command.getFirst());
    }

    static ParsedHistory parseHistory(String text) {
        ArrayList<CommitEvidence> commits = new ArrayList<>();
        ArrayList<Change> changes = new ArrayList<>();
        String commit = "";
        String parents = "";
        String committedAt = "";
        String subject = "";
        for (String line : Objects.requireNonNullElse(text, "").split("\\R")) {
            if (line.startsWith(HEADER_PREFIX)) {
                String[] fields = line.substring(HEADER_PREFIX.length())
                        .split(String.valueOf(FIELD_SEPARATOR), 5);
                if (fields.length != 5) throw new IllegalArgumentException("invalid Git header");
                commit = fields[0];
                parents = fields[1];
                committedAt = fields[3];
                subject = fields[4];
                commits.add(
                        new CommitEvidence(
                                commit,
                                parents,
                                fields[2],
                                committedAt,
                                subject));
                continue;
            }
            Matcher raw = RAW_CHANGE.matcher(line);
            if (!raw.matches()) continue;
            if (commit.isEmpty()) throw new IllegalArgumentException("raw change before commit header");
            changes.add(new Change(
                    commit,
                    parents,
                    committedAt,
                    subject,
                    raw.group(3),
                    raw.group(4),
                    raw.group(1),
                    raw.group(2)));
        }
        return new ParsedHistory(commits, changes);
    }

    static List<Change> parseRawLog(String text) {
        return parseHistory(text).changes();
    }

    static List<CurrentBlob> parseLsTree(String text) {
        ArrayList<CurrentBlob> blobs = new ArrayList<>();
        for (String line : Objects.requireNonNullElse(text, "").split("\\R")) {
            Matcher row = TREE_ROW.matcher(line);
            if (row.matches()) blobs.add(new CurrentBlob(row.group(2), row.group(1)));
        }
        blobs.sort(Comparator.comparing(CurrentBlob::path));
        return List.copyOf(blobs);
    }

    static List<Candidate> classify(List<Change> changes, List<CurrentBlob> current) {
        Objects.requireNonNull(changes, "changes");
        Map<String, String> currentByPath = new HashMap<>();
        for (CurrentBlob blob : current) currentByPath.put(blob.path(), blob.blob());

        ArrayList<Candidate> result = new ArrayList<>();
        for (Change change : changes) {
            int score = candidateScore(change);
            if (score < 2 || !auditablePath(change.path())) continue;

            String historical = change.newBlob().equals(ZERO) ? change.oldBlob() : change.newBlob();
            String currentBlob = currentByPath.getOrDefault(change.path(), ZERO);
            HistoryState state;
            String lower = change.subject().toLowerCase(Locale.ROOT);
            if (!currentBlob.equals(ZERO) && currentBlob.equals(historical)) {
                if (containsAny(lower, "replay")) state = HistoryState.REPLAYED;
                else if (containsAny(lower, "restore", "recover", "reintroduce", "resurrect")) {
                    state = HistoryState.RESTORED;
                } else {
                    state = HistoryState.SURVIVES;
                }
            } else if (currentBlob.equals(ZERO)) {
                state = HistoryState.LOST;
            } else {
                state = HistoryState.OVERWRITTEN_UNPROVEN;
            }

            String signal = signal(change.subject());
            String rationale = switch (state) {
                case SURVIVES -> "historical candidate blob is the current blob";
                case SUPERSEDED_PROVEN -> "external proof established a stronger current implementation";
                case LOST -> "historical candidate path is absent on current ref";
                case OVERWRITTEN_UNPROVEN ->
                        "historical candidate blob differs from current blob; supersession proof required";
                case REVERTED -> "historical candidate was explicitly reverted";
                case RESTORED -> "restore/recovery candidate blob survives on current ref";
                case REPLAYED -> "replay candidate blob survives on current ref";
                case PROOF_REQUIRED -> "candidate requires external proof";
            };
            result.add(new Candidate(
                    score,
                    state,
                    signal,
                    change.commit(),
                    change.committedAt(),
                    change.subject(),
                    change.path(),
                    historical,
                    currentBlob,
                    rationale));
        }
        result.sort(Comparator
                .comparingInt(Candidate::score).reversed()
                .thenComparing(Candidate::path)
                .thenComparing(Candidate::committedAt)
                .thenComparing(Candidate::commit));
        return List.copyOf(result);
    }

    static List<MergeEvidence> classifyMerges(List<CommitEvidence> commits) {
        Objects.requireNonNull(commits, "commits");
        Map<String, String> treeByCommit = new HashMap<>();
        for (CommitEvidence commit : commits) treeByCommit.put(commit.commit(), commit.tree());

        ArrayList<MergeEvidence> result = new ArrayList<>();
        for (CommitEvidence commit : commits) {
            List<String> parents = commit.parentList();
            if (parents.size() < 2) continue;
            String firstParent = parents.getFirst();
            String parentTree = treeByCommit.getOrDefault(firstParent, ZERO);
            MergeDisposition disposition;
            String rationale;
            if (parentTree.equals(ZERO)) {
                disposition = MergeDisposition.FIRST_PARENT_TREE_UNAVAILABLE;
                rationale =
                        "first-parent tree is outside the bounded audit window; source application is unknown";
            } else if (commit.tree().equals(parentTree)) {
                disposition = MergeDisposition.ANCESTRY_ONLY;
                rationale =
                        "merge tree equals first-parent tree; ancestry advanced but canonical source tree did not";
            } else {
                disposition = MergeDisposition.SOURCE_APPLIED;
                rationale =
                        "merge tree differs from first-parent tree; canonical source tree changed";
            }
            result.add(
                    new MergeEvidence(
                            commit.commit(),
                            firstParent,
                            parents.size(),
                            commit.tree(),
                            parentTree,
                            commit.committedAt(),
                            commit.subject(),
                            disposition,
                            rationale));
        }
        result.sort(
                Comparator.comparing(MergeEvidence::committedAt)
                        .thenComparing(MergeEvidence::commit));
        return List.copyOf(result);
    }

    private static int candidateScore(Change change) {
        String subject = change.subject().toLowerCase(Locale.ROOT);
        int score = 0;
        if (containsAny(subject, RESTORE_WORDS)) score += 5;
        if (containsAny(subject, PERFORMANCE_WORDS)) score += 4;
        if (containsAny(subject, CHANGE_WORDS)) score += 2;
        if (subject.contains("recipe") || subject.contains("test") || subject.contains("ci")) score += 1;
        if (change.status().startsWith("D")) score += 2;
        return score;
    }

    private static String signal(String subject) {
        String lower = subject.toLowerCase(Locale.ROOT);
        ArrayList<String> signals = new ArrayList<>();
        if (containsAny(lower, RESTORE_WORDS)) signals.add("RESTORE");
        if (containsAny(lower, PERFORMANCE_WORDS)) signals.add("PERFORMANCE");
        if (containsAny(lower, CHANGE_WORDS)) signals.add("CHANGE");
        if (lower.contains("recipe")) signals.add("RECIPE");
        if (lower.contains("test")) signals.add("TEST");
        if (lower.contains("ci")) signals.add("CI");
        return signals.isEmpty() ? "OTHER" : String.join(",", signals);
    }

    private static boolean auditablePath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".java")
                || lower.endsWith(".c")
                || lower.endsWith(".cc")
                || lower.endsWith(".cpp")
                || lower.endsWith(".h")
                || lower.endsWith(".hpp")
                || lower.endsWith(".xml")
                || lower.endsWith(".yml")
                || lower.endsWith(".yaml")
                || lower.endsWith(".properties")
                || lower.endsWith(".md")
                || lower.endsWith(".tsv");
    }

    private static boolean containsAny(String value, Set<String> needles) {
        return needles.stream().anyMatch(value::contains);
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static String reportRoot(
            String ref,
            int maxCommits,
            List<CommitEvidence> commits,
            List<Change> changes,
            List<CurrentBlob> current,
            List<Candidate> candidates,
            List<MergeEvidence> merges) {
        StringBuilder canonical = new StringBuilder("M3-DEVELOP-HISTORY-AUDIT/2\n")
                .append(ref).append('\n').append(maxCommits).append('\n');
        commits.forEach(value -> canonical
                .append("H\t").append(value.commit()).append('\t')
                .append(value.parents()).append('\t').append(value.tree()).append('\t')
                .append(value.committedAt()).append('\t').append(value.subject()).append('\n'));
        changes.forEach(value -> canonical
                .append("C\t").append(value.commit()).append('\t')
                .append(value.status()).append('\t').append(value.path()).append('\t')
                .append(value.oldBlob()).append('\t').append(value.newBlob()).append('\n'));
        current.forEach(value -> canonical
                .append("T\t").append(value.path()).append('\t').append(value.blob()).append('\n'));
        candidates.forEach(value -> canonical
                .append("B\t").append(value.score()).append('\t').append(value.state()).append('\t')
                .append(value.commit()).append('\t').append(value.path()).append('\t')
                .append(value.historicalBlob()).append('\t').append(value.currentBlob()).append('\n'));
        merges.forEach(value -> canonical
                .append("M\t").append(value.commit()).append('\t')
                .append(value.firstParent()).append('\t').append(value.parentCount()).append('\t')
                .append(value.tree()).append('\t').append(value.firstParentTree()).append('\t')
                .append(value.disposition()).append('\n'));
        return Hashing.sha256(canonical.toString());
    }

    private static void requireCompleteHistory(Path root) throws IOException {
        String shallow =
                runGit(
                                root,
                                List.of("rev-parse", "--is-shallow-repository"),
                                DEFAULT_TIMEOUT)
                        .strip();
        if (shallow.equals("true")) {
            throw new IOException(
                    "develop history audit requires a complete Git history; shallow repository detected");
        }
        if (!shallow.equals("false")) {
            throw new IOException("unexpected Git shallow-history response: " + shallow);
        }
    }

    private static String runGit(Path root, List<String> gitArguments, Duration timeout)
            throws IOException {
        if (!isReadOnlyGitCommand(gitArguments)) {
            throw new IllegalArgumentException("Git write command is forbidden: " + gitArguments);
        }
        ArrayList<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(root.toString());
        command.addAll(gitArguments);

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<byte[]> output = executor.submit(() -> process.getInputStream().readAllBytes());
            boolean finished;
            try {
                finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
                throw new IOException("Git history audit interrupted", interrupted);
            }
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Git history audit timed out after " + timeout);
            }
            byte[] bytes;
            try {
                bytes = output.get(10, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Git output read interrupted", interrupted);
            } catch (ExecutionException | TimeoutException failed) {
                throw new IOException("Git output read failed", failed);
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IOException("Git read failed (" + process.exitValue() + "): " + text.strip());
            }
            return text;
        }
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String normalizePath(String value) {
        return value.replace('\\', '/');
    }

    private static String normalizeBlob(String value) {
        String checked = Objects.requireNonNullElse(value, ZERO).strip().toLowerCase(Locale.ROOT);
        if (checked.matches("0+")) return ZERO;
        if (!checked.matches("[0-9a-f]{7,64}")) throw new IllegalArgumentException("blob");
        return checked;
    }
}
