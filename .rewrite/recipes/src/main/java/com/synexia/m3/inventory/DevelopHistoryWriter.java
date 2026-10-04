// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.synexia.job.IProgressMonitor;

/** Stable TSV writer for read-only develop history evidence. */
public final class DevelopHistoryWriter {
    public void write(Path outputDirectory, DevelopHistoryAuditor.Report report)
            throws IOException {
        write(outputDirectory, report, IProgressMonitor.noop());
    }

    public void write(
            Path outputDirectory,
            DevelopHistoryAuditor.Report report,
            IProgressMonitor monitor) throws IOException {
        Path output = Objects.requireNonNull(outputDirectory, "outputDirectory")
                .toAbsolutePath().normalize();
        Objects.requireNonNull(report, "report");
        IProgressMonitor progress = Objects.requireNonNull(monitor, "monitor");
        progress.beginTask("Publish develop history evidence", 8L);
        Files.createDirectories(output);

        step(progress, output, "COMMIT_HEADERS.tsv", commitHeaders(report));
        step(progress, output, "COMMIT_CHAIN.tsv", commitChain(report));
        step(progress, output, "MERGE_EVIDENCE.tsv", mergeEvidence(report));
        step(progress, output, "ANCESTRY_ONLY_MERGES.tsv", ancestryOnlyMerges(report));
        step(progress, output, "BEST_KNOWN_HISTORY.tsv", bestKnown(report));
        step(progress, output, "RESTORATION_QUEUE.tsv", restorationQueue(report));
        step(progress, output, "HISTORY_SUMMARY.tsv", summary(report));
        step(
                progress,
                output,
                "HISTORY_ROOT.sha256",
                report.root() + "  DEVELOP_HISTORY_AUDIT\n");
        progress.done();
    }

    private static void step(
            IProgressMonitor monitor, Path output, String name, String text) throws IOException {
        monitor.checkCanceled();
        monitor.subTask(name);
        write(output, name, text);
        monitor.worked(1L);
    }

    static String commitHeaders(DevelopHistoryAuditor.Report report) {
        StringBuilder out =
                new StringBuilder("commit\tparents\ttree\tcommittedAt\tsubject\n");
        for (DevelopHistoryAuditor.CommitEvidence row : report.commits()) {
            out.append(tsv(row.commit())).append('\t')
                    .append(tsv(row.parents())).append('\t')
                    .append(tsv(row.tree())).append('\t')
                    .append(tsv(row.committedAt())).append('\t')
                    .append(tsv(row.subject())).append('\n');
        }
        return out.toString();
    }

    static String commitChain(DevelopHistoryAuditor.Report report) {
        StringBuilder out = new StringBuilder(
                "commit\tparents\tcommittedAt\tstatus\tpath\toldBlob\tnewBlob\tsubject\n");
        for (DevelopHistoryAuditor.Change row : report.changes()) {
            out.append(tsv(row.commit())).append('\t')
                    .append(tsv(row.parents())).append('\t')
                    .append(tsv(row.committedAt())).append('\t')
                    .append(tsv(row.status())).append('\t')
                    .append(tsv(row.path())).append('\t')
                    .append(tsv(row.oldBlob())).append('\t')
                    .append(tsv(row.newBlob())).append('\t')
                    .append(tsv(row.subject())).append('\n');
        }
        return out.toString();
    }

    static String mergeEvidence(DevelopHistoryAuditor.Report report) {
        StringBuilder out = new StringBuilder(
                "commit\tfirstParent\tparentCount\ttree\tfirstParentTree\tcommittedAt"
                        + "\tdisposition\tsubject\trationale\n");
        for (DevelopHistoryAuditor.MergeEvidence row : report.merges()) {
            out.append(tsv(row.commit())).append('\t')
                    .append(tsv(row.firstParent())).append('\t')
                    .append(row.parentCount()).append('\t')
                    .append(tsv(row.tree())).append('\t')
                    .append(tsv(row.firstParentTree())).append('\t')
                    .append(tsv(row.committedAt())).append('\t')
                    .append(row.disposition()).append('\t')
                    .append(tsv(row.subject())).append('\t')
                    .append(tsv(row.rationale())).append('\n');
        }
        return out.toString();
    }

    static String ancestryOnlyMerges(DevelopHistoryAuditor.Report report) {
        StringBuilder out = new StringBuilder(
                "commit\tfirstParent\tparentCount\ttree\tcommittedAt\tsubject\trationale\n");
        for (DevelopHistoryAuditor.MergeEvidence row : report.merges()) {
            if (row.disposition() != DevelopHistoryAuditor.MergeDisposition.ANCESTRY_ONLY) continue;
            out.append(tsv(row.commit())).append('\t')
                    .append(tsv(row.firstParent())).append('\t')
                    .append(row.parentCount()).append('\t')
                    .append(tsv(row.tree())).append('\t')
                    .append(tsv(row.committedAt())).append('\t')
                    .append(tsv(row.subject())).append('\t')
                    .append(tsv(row.rationale())).append('\n');
        }
        return out.toString();
    }

    static String bestKnown(DevelopHistoryAuditor.Report report) {
        StringBuilder out = new StringBuilder(
                "score\tstate\tsignal\tcommit\tcommittedAt\tpath\thistoricalBlob\tcurrentBlob"
                        + "\treplacementAuthority\tsubject\trationale\n");
        for (DevelopHistoryAuditor.Candidate row : report.candidates()) {
            out.append(row.score()).append('\t')
                    .append(row.state()).append('\t')
                    .append(tsv(row.signal())).append('\t')
                    .append(tsv(row.commit())).append('\t')
                    .append(tsv(row.committedAt())).append('\t')
                    .append(tsv(row.path())).append('\t')
                    .append(tsv(row.historicalBlob())).append('\t')
                    .append(tsv(row.currentBlob())).append('\t')
                    .append(row.replacementAuthority()).append('\t')
                    .append(tsv(row.subject())).append('\t')
                    .append(tsv(row.rationale())).append('\n');
        }
        return out.toString();
    }

    static String restorationQueue(DevelopHistoryAuditor.Report report) {
        StringBuilder out = new StringBuilder(
                "score\tstate\tcommit\tpath\thistoricalBlob\tcurrentBlob\tsubject\trationale\n");
        for (DevelopHistoryAuditor.Candidate row : report.candidates()) {
            if (row.state() != DevelopHistoryAuditor.HistoryState.LOST
                    && row.state() != DevelopHistoryAuditor.HistoryState.OVERWRITTEN_UNPROVEN
                    && row.state() != DevelopHistoryAuditor.HistoryState.PROOF_REQUIRED) {
                continue;
            }
            out.append(row.score()).append('\t')
                    .append(row.state()).append('\t')
                    .append(tsv(row.commit())).append('\t')
                    .append(tsv(row.path())).append('\t')
                    .append(tsv(row.historicalBlob())).append('\t')
                    .append(tsv(row.currentBlob())).append('\t')
                    .append(tsv(row.subject())).append('\t')
                    .append(tsv(row.rationale())).append('\n');
        }
        return out.toString();
    }

    static String summary(DevelopHistoryAuditor.Report report) {
        Map<DevelopHistoryAuditor.HistoryState, Integer> counts =
                new EnumMap<>(DevelopHistoryAuditor.HistoryState.class);
        for (DevelopHistoryAuditor.Candidate row : report.candidates()) {
            counts.merge(row.state(), 1, Integer::sum);
        }
        StringBuilder out = new StringBuilder("key\tvalue\n")
                .append("ref\t").append(tsv(report.ref())).append('\n')
                .append("maxCommits\t").append(report.maxCommits()).append('\n')
                .append("commits\t").append(report.commits().size()).append('\n')
                .append("changes\t").append(report.changes().size()).append('\n')
                .append("currentBlobs\t").append(report.currentBlobs().size()).append('\n')
                .append("candidates\t").append(report.candidates().size()).append('\n')
                .append("merges\t").append(report.merges().size()).append('\n')
                .append("ancestryOnlyMerges\t")
                .append(report.merges().stream()
                        .filter(row -> row.disposition()
                                == DevelopHistoryAuditor.MergeDisposition.ANCESTRY_ONLY)
                        .count())
                .append('\n')
                .append("historyRoot\t").append(report.root()).append('\n');
        counts.forEach((state, count) ->
                out.append("state.").append(state).append('\t').append(count).append('\n'));
        return out.toString();
    }

    private static void write(Path output, String name, String text) throws IOException {
        Files.writeString(output.resolve(name), text, StandardCharsets.UTF_8);
    }

    private static String tsv(String value) {
        return Objects.toString(value, "").replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }
}
