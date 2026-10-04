// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.algorithms.corpus.ProblemAdapter;
import com.synexia.algorithms.corpus.ProblemAdapterCatalog;
import com.synexia.algorithms.corpus.ProblemAdapterReviewBridge;
import com.synexia.algorithms.corpus.SerialProblemReview;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Content-addressed custody receipt binding exact pinned donor files to their serial M3 review.
 *
 * <p>The work-order planner already selects exact platform/repository@commit/path identities. This
 * owner resolves those identities back to the canonical {@link ProblemAdapterCatalog}, projects the
 * existing atom/interface contract through {@link ProblemAdapterReviewBridge}, and runs the existing
 * {@link SerialProblemReview}. No donor source is copied, executed, replaced or promoted here.</p>
 */
public final class M3ProblemDonorReviewReceipt {
    private static final String DOMAIN = "SYNEXIA_M3_PROBLEM_DONOR_REVIEW_RECEIPT_V1";

    public record Receipt(
            String donorCorpusRoot,
            List<String> donorJavaSources,
            List<String> reviewedSourceIds,
            String serialReviewReportRoot,
            int reviewedFiles,
            int reviewEvents,
            int coverageRows,
            String root) {

        public Receipt {
            donorCorpusRoot = optionalSha(donorCorpusRoot, "donorCorpusRoot");
            donorJavaSources = stable(donorJavaSources);
            reviewedSourceIds = stable(reviewedSourceIds);
            serialReviewReportRoot =
                    optionalSha(serialReviewReportRoot, "serialReviewReportRoot");
            if (reviewedFiles < 0 || reviewEvents < 0 || coverageRows < 0) {
                throw new IllegalArgumentException("review counts");
            }
            if (donorJavaSources.isEmpty()) {
                if (!donorCorpusRoot.isEmpty()
                        || !reviewedSourceIds.isEmpty()
                        || !serialReviewReportRoot.isEmpty()
                        || reviewedFiles != 0
                        || reviewEvents != 0
                        || coverageRows != 0) {
                    throw new IllegalArgumentException("empty donor review must have empty evidence");
                }
            } else {
                if (donorCorpusRoot.isEmpty()
                        || serialReviewReportRoot.isEmpty()
                        || reviewedFiles != donorJavaSources.size()
                        || reviewedSourceIds.size() != donorJavaSources.size()) {
                    throw new IllegalArgumentException("incomplete donor review evidence");
                }
            }
            String expected =
                    receiptRoot(
                            donorCorpusRoot,
                            donorJavaSources,
                            reviewedSourceIds,
                            serialReviewReportRoot,
                            reviewedFiles,
                            reviewEvents,
                            coverageRows);
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!root.equals(expected)) {
                throw new IllegalArgumentException("donor review receipt root mismatch");
            }
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean donorSourceCopyAuthority() {
            return false;
        }

        public boolean donorExecutionAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    private record Identity(
            String platform,
            String repository,
            String commit,
            String path) {}

    private M3ProblemDonorReviewReceipt() {}

    public static Receipt review(
            List<String> donorJavaSources,
            String donorCorpusRoot) {
        List<String> identities = stable(donorJavaSources);
        if (identities.isEmpty()) {
            return new Receipt("", List.of(), List.of(), "", 0, 0, 0, "");
        }
        String corpusRoot = sha(donorCorpusRoot, "donorCorpusRoot");

        ArrayList<SerialProblemReview.SourceFile> files =
                new ArrayList<>(identities.size());
        for (String encoded : identities) {
            ProblemAdapter adapter = resolve(parse(encoded));
            files.add(ProblemAdapterReviewBridge.fromAdapter(adapter));
        }
        files.sort(Comparator.comparing(SerialProblemReview.SourceFile::stableKey));

        SerialProblemReview.Report report = SerialProblemReview.review(files);
        List<String> reviewedSourceIds =
                files.stream().map(SerialProblemReview.SourceFile::sourceId).toList();
        return new Receipt(
                corpusRoot,
                identities,
                reviewedSourceIds,
                report.rootSha256(),
                files.size(),
                report.events().size(),
                report.coverage().size(),
                "");
    }

    private static ProblemAdapter resolve(Identity identity) {
        List<ProblemAdapter> matches =
                ProblemAdapterCatalog.all().stream()
                        .filter(adapter ->
                                adapter.source().platform().equalsIgnoreCase(identity.platform())
                                        && adapter.source().repository().equals(identity.repository())
                                        && adapter.source().commit().equals(identity.commit())
                                        && adapter.source().path().equals(identity.path()))
                        .toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "donor source must resolve exactly once in ProblemAdapterCatalog: "
                            + render(identity)
                            + " matches="
                            + matches.size());
        }
        return matches.getFirst();
    }

    private static Identity parse(String value) {
        String checked = text(value, "donorJavaSource");
        int platformEnd = checked.indexOf(':');
        int revisionStart = checked.indexOf('@', platformEnd + 1);
        int pathStart = checked.indexOf(':', revisionStart + 1);
        if (platformEnd < 1 || revisionStart <= platformEnd + 1 || pathStart <= revisionStart + 1) {
            throw new IllegalArgumentException("invalid donor Java source identity: " + checked);
        }
        String platform = checked.substring(0, platformEnd);
        String repository = checked.substring(platformEnd + 1, revisionStart);
        String commit = checked.substring(revisionStart + 1, pathStart);
        String path = checked.substring(pathStart + 1);
        if (!platform.matches("[A-Za-z0-9][A-Za-z0-9._-]*")
                || !repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")
                || !commit.matches("[0-9a-f]{40}|[0-9a-f]{64}")
                || path.isBlank()
                || path.startsWith("/")
                || path.contains("../")) {
            throw new IllegalArgumentException("invalid donor Java source identity: " + checked);
        }
        return new Identity(platform, repository, commit, path);
    }

    private static String render(Identity identity) {
        return identity.platform()
                + ":"
                + identity.repository()
                + "@"
                + identity.commit()
                + ":"
                + identity.path();
    }

    private static String receiptRoot(
            String donorCorpusRoot,
            List<String> donorJavaSources,
            List<String> reviewedSourceIds,
            String serialReviewReportRoot,
            int reviewedFiles,
            int reviewEvents,
            int coverageRows) {
        MessageDigest digest = digest();
        frame(digest, DOMAIN);
        frame(digest, donorCorpusRoot);
        donorJavaSources.forEach(value -> frame(digest, value));
        reviewedSourceIds.forEach(value -> frame(digest, value));
        frame(digest, serialReviewReportRoot);
        frame(digest, Integer.toString(reviewedFiles));
        frame(digest, Integer.toString(reviewEvents));
        frame(digest, Integer.toString(coverageRows));
        return HexFormat.of().formatHex(digest.digest());
    }

    private static List<String> stable(List<String> values) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        Objects.requireNonNull(values, "values").stream()
                .map(value -> text(value, "value"))
                .sorted()
                .forEach(unique::add);
        return List.copyOf(unique);
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String optionalSha(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        return checked.isEmpty() ? "" : sha(checked, field);
    }
}
