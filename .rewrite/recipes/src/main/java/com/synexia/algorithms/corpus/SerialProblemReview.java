// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.job.IProgressMonitor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic per-file review: inventory, compare peers, then evaluate replacement eligibility.
 *
 * <p>The method is intentionally serial. A file is completely reviewed before the next file is
 * visited, and the pass order is fixed. It never mutates a source file or replaces an artifact;
 * it emits review evidence for a later caller.</p>
 *
 * <p>Comparison groups are reduced once before the serial passes. A homogeneous group no longer
 * scans all of its peers again for every member. Reports, roots, admission rules and public
 * contracts are unchanged; a category match alone is not semantic equivalence evidence.</p>
 */
public final class SerialProblemReview {
    public static final String CONTRACT_ID = "SYNEXIA_PROBLEM_ADAPTER_CONTRACT_V1";
    public static final String CONTRACT_SHA256 = sha256(CONTRACT_ID
            + "|executeCanonical(Object,IProgressMonitor)->Object"
            + "|bind(Encoder,Decoder)->ProgressAlgorithm");

    public enum Pass {
        INVENTORY,
        COMPARE,
        REPLACEMENT_GATE
    }

    public enum Status {
        INVENTORY_OK,
        NO_COMPARABLE_PEER,
        CONTRACT_MATCH,
        CONTRACT_CONFLICT,
        BLOCKED_NO_PROOF,
        BLOCKED_SOURCE_CHANGED,
        BLOCKED_INTERFACE_CHANGED,
        REPLACE_CANDIDATE_ADMITTED
    }

    public record SourceFile(
            String sourceId,
            String platform,
            String repository,
            String commit,
            String path,
            String problemKey,
            String categoryKey,
            String atomSha256,
            String interfaceSha256,
            String differentialProofSha256) {
        public SourceFile {
            sourceId = required(sourceId, "sourceId");
            platform = required(platform, "platform").toUpperCase(Locale.ROOT);
            repository = required(repository, "repository");
            commit = required(commit, "commit");
            path = required(path, "path");
            problemKey = required(problemKey, "problemKey");
            categoryKey = required(categoryKey, "categoryKey").toUpperCase(Locale.ROOT);
            atomSha256 = hash(atomSha256, "atomSha256");
            interfaceSha256 = hash(interfaceSha256, "interfaceSha256");
            if (differentialProofSha256 != null && !differentialProofSha256.isBlank()) {
                differentialProofSha256 = hash(differentialProofSha256, "differentialProofSha256");
            } else {
                differentialProofSha256 = "";
            }
        }

        public String stableKey() {
            return platform + "\t" + repository + "\t" + commit + "\t" + path;
        }

        public String comparisonKey() {
            return categoryKey + "\t" + normalize(problemKey);
        }
    }

    public record Event(
            int ordinal,
            String sourceId,
            Pass pass,
            String platform,
            String categoryKey,
            String comparisonKey,
            int comparablePeers,
            String interfaceSha256,
            Status status,
            String rationale) {
        public Event {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            sourceId = required(sourceId, "sourceId");
            Objects.requireNonNull(pass, "pass");
            platform = required(platform, "platform");
            categoryKey = required(categoryKey, "categoryKey");
            comparisonKey = required(comparisonKey, "comparisonKey");
            if (comparablePeers < 0) throw new IllegalArgumentException("comparablePeers");
            interfaceSha256 = hash(interfaceSha256, "interfaceSha256");
            Objects.requireNonNull(status, "status");
            rationale = required(rationale, "rationale");
        }
    }

    public record Coverage(
            CompetitiveProblemCategoryCatalog.Platform platform,
            String categoryKey,
            int sourceFiles,
            int distinctProblems,
            String status) {
        public Coverage {
            Objects.requireNonNull(platform, "platform");
            categoryKey = required(categoryKey, "categoryKey");
            if (sourceFiles < 0 || distinctProblems < 0) {
                throw new IllegalArgumentException("coverage counts");
            }
            status = required(status, "status");
        }
    }

    public record Report(List<Event> events, List<Coverage> coverage, String rootSha256) {
        public Report {
            events = List.copyOf(events);
            coverage = List.copyOf(coverage);
            rootSha256 = hash(rootSha256, "rootSha256");
        }
    }

    /**
     * Explicit candidate identity. The atom digest names the generated adapter atom; the complete
     * target file digest is compared against a separate observation from the file-reading host.
     * Neither category similarity nor a proof digest is a substitute for this evidence.
     */
    public record CandidateEvidence(
            String sourceId,
            String expectedAtomSha256,
            String candidateAtomSha256,
            String expectedFileSha256,
            String expectedInterfaceSha256,
            String candidateInterfaceSha256,
            String differentialProofSha256) {
        public CandidateEvidence {
            sourceId = required(sourceId, "sourceId");
            expectedAtomSha256 = hash(expectedAtomSha256, "expectedAtomSha256");
            candidateAtomSha256 = hash(candidateAtomSha256, "candidateAtomSha256");
            expectedFileSha256 = hash(expectedFileSha256, "expectedFileSha256");
            expectedInterfaceSha256 = hash(expectedInterfaceSha256, "expectedInterfaceSha256");
            candidateInterfaceSha256 = hash(candidateInterfaceSha256, "candidateInterfaceSha256");
            differentialProofSha256 = differentialProofSha256 == null
                    || differentialProofSha256.isBlank()
                    ? "" : hash(differentialProofSha256, "differentialProofSha256");
        }

        /** Domain-separated declaration, including candidate identity even when observation fails. */
        public String declaredRootSha256() {
            return sha256("SYNEXIA_SERIAL_CANDIDATE_DECLARED_V1\n" + sourceId + '\n'
                    + expectedAtomSha256 + '\n' + candidateAtomSha256 + '\n'
                    + expectedFileSha256 + '\n' + expectedInterfaceSha256 + '\n'
                    + candidateInterfaceSha256 + '\n' + differentialProofSha256 + '\n');
        }

        /** Domain-separated binding to declared candidate and independently observed file. */
        public String rootSha256(FileObservation observation) {
            if (!sourceId.equals(Objects.requireNonNull(observation, "observation").sourceId())) {
                throw new IllegalArgumentException("observation source mismatch");
            }
            return sha256("SYNEXIA_SERIAL_CANDIDATE_OBSERVED_V1\n"
                    + declaredRootSha256() + '\n' + observation.observedFileSha256() + '\n');
        }
    }

    /** An independent current observation of the complete target file. */
    public record FileObservation(String sourceId, String observedFileSha256) {
        public FileObservation {
            sourceId = required(sourceId, "sourceId");
            observedFileSha256 = hash(observedFileSha256, "observedFileSha256");
        }
    }

    /** Implemented by the host that reads and hashes the target file on the review pass. */
    @FunctionalInterface
    public interface FileObserver {
        FileObservation observe(SourceFile source);
    }

    /** Receipt from an independently executed differential checker; the digest alone is insufficient. */
    public record ProofAttestation(
            String verifierId, String evidenceRootSha256, int cases, int passed) {
        public ProofAttestation {
            verifierId = required(verifierId, "verifierId");
            evidenceRootSha256 = hash(evidenceRootSha256, "evidenceRootSha256");
            if (cases < 1 || passed < 0 || passed > cases) {
                throw new IllegalArgumentException("proof case counts");
            }
        }

        public boolean matches(CandidateEvidence evidence, FileObservation observation) {
            return evidenceRootSha256.equals(evidence.rootSha256(observation)) && cases == passed;
        }
    }

    /** Supplied by the host that runs the actual comparison, never by category classification. */
    @FunctionalInterface
    public interface DifferentialProofVerifier {
        ProofAttestation verify(
                SourceFile source, CandidateEvidence evidence, FileObservation observation);
    }

    private record Group(int size, String interfaceSha256, boolean sameInterface) {}

    private record GateResult(
            ProblemReplacementGate.Decision decision,
            FileObservation observation,
            ProofAttestation attestation) {}

    private SerialProblemReview() {}

    /** Deterministic three-pass TSV over the packaged file catalogue. */
    public static String renderTsv() {
        return renderTsv(null);
    }

    /** One platform's review events; null selects all packaged files. */
    public static String renderTsv(ChallengePlatform platform) {
        return renderTsv(platform, IProgressMonitor.noop());
    }

    /** The same deterministic file review with an explicit cancellation boundary. */
    public static String renderTsv(ChallengePlatform platform, IProgressMonitor suppliedMonitor) {
        IProgressMonitor monitor = suppliedMonitor == null ? IProgressMonitor.noop() : suppliedMonitor;
        check(monitor);
        List<ProblemAdapter> adapters = platform == null
                ? ProblemAdapterCatalog.all()
                : ProblemAdapterCatalog.byPlatform(platform.name());
        List<SourceFile> files = new ArrayList<>(adapters.size());
        for (ProblemAdapter adapter : adapters) {
            check(monitor);
            files.add(ProblemAdapterReviewBridge.fromAdapter(adapter));
            check(monitor);
        }
        files.sort(Comparator.comparing(SourceFile::stableKey));
        check(monitor);
        Report report = reviewMonitored(files, monitor);
        StringBuilder out = new StringBuilder(4096);
        out.append("source_id\tplatform\trepository\tcommit\tpath\tcategory")
                .append("\tpass\tcomparable_peers\tstatus\trationale\n");
        int passes = Pass.values().length;
        for (int i = 0; i < files.size(); i++) {
            check(monitor);
            SourceFile file = files.get(i);
            for (int p = 0; p < passes; p++) {
                Event event = report.events().get(i * passes + p);
                if (!event.sourceId().equals(file.sourceId())) {
                    throw new IllegalStateException("review events lost source order");
                }
                out.append(file.sourceId()).append('\t')
                        .append(file.platform()).append('\t')
                        .append(file.repository()).append('\t')
                        .append(file.commit()).append('\t')
                        .append(file.path()).append('\t')
                        .append(event.categoryKey()).append('\t')
                        .append(event.pass()).append('\t')
                        .append(event.comparablePeers()).append('\t')
                        .append(event.status()).append('\t')
                        .append(event.rationale()).append('\n');
            }
        }
        check(monitor);
        return out.toString();
    }

    public static Report review(List<SourceFile> suppliedFiles) {
        return reviewMonitored(suppliedFiles, IProgressMonitor.noop());
    }

    public static Report reviewMonitored(List<SourceFile> suppliedFiles, IProgressMonitor monitor) {
        return review(suppliedFiles, CompetitiveProblemCategoryCatalog.all(), List.of(),
                source -> null, (source, evidence, observation) -> null, monitor);
    }

    public static Report review(
            List<SourceFile> suppliedFiles,
            List<CompetitiveProblemCategoryCatalog.Category> categories) {
        return review(suppliedFiles, categories, List.of(),
                source -> null, (source, evidence, observation) -> null);
    }

    /**
     * Reviews one file at a time. A candidate is considered for admission only if its exact atom,
     * current whole-file observation, interface, and independently checked proof all agree.
     * Admission remains a review signal; a later write requires the existing M3 file proof.
     */
    public static Report review(
            List<SourceFile> suppliedFiles,
            List<CompetitiveProblemCategoryCatalog.Category> categories,
            List<CandidateEvidence> candidates,
            FileObserver observer,
            DifferentialProofVerifier verifier) {
        return review(suppliedFiles, categories, candidates, observer, verifier,
                IProgressMonitor.noop());
    }

    public static Report review(
            List<SourceFile> suppliedFiles,
            List<CompetitiveProblemCategoryCatalog.Category> categories,
            List<CandidateEvidence> candidates,
            FileObserver observer,
            DifferentialProofVerifier verifier,
            IProgressMonitor suppliedMonitor) {
        Objects.requireNonNull(suppliedFiles, "suppliedFiles");
        Objects.requireNonNull(categories, "categories");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(observer, "observer");
        Objects.requireNonNull(verifier, "verifier");
        IProgressMonitor monitor = suppliedMonitor == null ? IProgressMonitor.noop() : suppliedMonitor;
        check(monitor);
        List<SourceFile> files = suppliedFiles.stream()
                .map(Objects::requireNonNull)
                .sorted(Comparator.comparing(SourceFile::stableKey))
                .toList();
        Set<String> identities = new HashSet<>();
        Set<String> sourceIds = new HashSet<>();
        for (SourceFile file : files) {
            check(monitor);
            if (!identities.add(file.stableKey())) {
                throw new IllegalArgumentException("duplicate source file: " + file.stableKey());
            }
            if (!sourceIds.add(file.sourceId()) && !candidates.isEmpty()) {
                throw new IllegalArgumentException("ambiguous sourceId: " + file.sourceId());
            }
        }

        Map<String, CandidateEvidence> bySourceId = new HashMap<>();
        for (CandidateEvidence candidate : candidates) {
            check(monitor);
            CandidateEvidence checked = Objects.requireNonNull(candidate, "candidate");
            if (!sourceIds.contains(checked.sourceId())) {
                throw new IllegalArgumentException("candidate for unknown sourceId: " + checked.sourceId());
            }
            if (bySourceId.putIfAbsent(checked.sourceId(), checked) != null) {
                throw new IllegalArgumentException("duplicate candidate for sourceId: " + checked.sourceId());
            }
        }

        monitor.beginTask("review competitive problem files", files.size());
        try {
            check(monitor);
            Map<String, Group> grouped = precomputeGroups(files, monitor);

            List<Event> events = new ArrayList<>(Math.multiplyExact(files.size(), Pass.values().length));
            Map<String, FileObservation> observations = new HashMap<>();
            Map<String, ProofAttestation> attestations = new HashMap<>();
            int ordinal = 0;
            for (SourceFile file : files) {
                check(monitor);
                monitor.subTask(file.stableKey());
                Group group = grouped.get(file.comparisonKey());
                int peers = group.size() - 1;
                events.add(event(ordinal++, file, Pass.INVENTORY, peers,
                        Status.INVENTORY_OK, "identity/category/interface fields validated"));
                check(monitor);

                boolean sameInterface = group.sameInterface();
                Status compareStatus = peers == 0
                        ? Status.NO_COMPARABLE_PEER
                        : sameInterface ? Status.CONTRACT_MATCH : Status.CONTRACT_CONFLICT;
                String compareReason = peers == 0
                        ? "no same-category normalized problem peer"
                        : sameInterface
                                ? "all comparable peers expose the same declared adapter contract"
                                : "comparable peer interface hash differs; replacement is forbidden";
                events.add(event(ordinal++, file, Pass.COMPARE, peers, compareStatus, compareReason));
                check(monitor);

                CandidateEvidence candidate = bySourceId.get(file.sourceId());
                boolean peerConflict = peers > 0 && !sameInterface;
                GateResult gate = candidate != null && peerConflict
                        ? result(ProblemReplacementGate.Decision.INTERFACE_CHANGED)
                        : gate(file, candidate, observer, verifier, monitor);
                check(monitor);
                ProblemReplacementGate.Decision decision = gate.decision();
                if (gate.observation() != null) observations.put(file.sourceId(), gate.observation());
                if (gate.attestation() != null) attestations.put(file.sourceId(), gate.attestation());
                Status gateStatus = switch (decision) {
                    case ADMIT_CANDIDATE -> Status.REPLACE_CANDIDATE_ADMITTED;
                    case SOURCE_CHANGED -> Status.BLOCKED_SOURCE_CHANGED;
                    case INTERFACE_CHANGED -> Status.BLOCKED_INTERFACE_CHANGED;
                    case PROOF_MISSING -> Status.BLOCKED_NO_PROOF;
                };
                events.add(event(ordinal++, file, Pass.REPLACEMENT_GATE, peers, gateStatus,
                        decision == ProblemReplacementGate.Decision.ADMIT_CANDIDATE
                                ? "explicit atom/file/interface candidate and verified differential receipt; "
                                        + "later file proof still required"
                                : candidate != null && peerConflict
                                        ? "comparable peer interface hash differs; replacement is forbidden"
                                        : "explicit fresh candidate, matching interface and verified proof required"));
                monitor.worked(1);
            }

            List<Coverage> reportedCoverage = coverage(files, categories, monitor);
            String digest = root(files, events, bySourceId, observations, attestations,
                    reportedCoverage, monitor);
            check(monitor);
            return new Report(List.copyOf(events), reportedCoverage, digest);
        } finally {
            monitor.done();
        }
    }

    private static GateResult gate(
            SourceFile source, CandidateEvidence candidate, FileObserver observer,
            DifferentialProofVerifier verifier, IProgressMonitor monitor) {
        if (candidate == null) return result(ProblemReplacementGate.Decision.PROOF_MISSING);
        if (!source.atomSha256().equals(candidate.expectedAtomSha256())) {
            return result(ProblemReplacementGate.Decision.SOURCE_CHANGED);
        }
        if (!source.interfaceSha256().equals(candidate.expectedInterfaceSha256())) {
            return result(ProblemReplacementGate.Decision.INTERFACE_CHANGED);
        }
        FileObservation observation = Objects.requireNonNull(observer.observe(source),
                "file observer must produce an observation");
        check(monitor);
        if (!source.sourceId().equals(observation.sourceId())) {
            throw new IllegalArgumentException("observation source mismatch");
        }
        ProblemReplacementGate.Request request = new ProblemReplacementGate.Request(
                source.sourceId(), candidate.expectedFileSha256(), observation.observedFileSha256(),
                candidate.expectedInterfaceSha256(), candidate.candidateInterfaceSha256(),
                candidate.differentialProofSha256());
        ProblemReplacementGate.Decision preliminary = ProblemReplacementGate.evaluate(request);
        if (preliminary != ProblemReplacementGate.Decision.ADMIT_CANDIDATE) {
            return new GateResult(preliminary, observation, null);
        }
        if (!source.differentialProofSha256().isEmpty()
                && !source.differentialProofSha256().equals(candidate.differentialProofSha256())) {
            return new GateResult(ProblemReplacementGate.Decision.PROOF_MISSING, observation, null);
        }
        ProofAttestation proof = verifier.verify(source, candidate, observation);
        check(monitor);
        return new GateResult(proof != null && proof.matches(candidate, observation)
                ? ProblemReplacementGate.Decision.ADMIT_CANDIDATE
                : ProblemReplacementGate.Decision.PROOF_MISSING, observation, proof);
    }

    private static GateResult result(ProblemReplacementGate.Decision decision) {
        return new GateResult(decision, null, null);
    }

    /** Reduce each category/problem group to the facts used by every subsequent file pass. */
    private static Map<String, Group> precomputeGroups(
            List<SourceFile> files, IProgressMonitor monitor) {
        Map<String, Group> groups = new HashMap<>();
        for (SourceFile file : files) {
            check(monitor);
            groups.compute(file.comparisonKey(), (key, group) -> group == null
                    ? new Group(1, file.interfaceSha256(), true)
                    : new Group(group.size() + 1, group.interfaceSha256(),
                            group.sameInterface()
                                    && group.interfaceSha256().equals(file.interfaceSha256())));
        }
        return groups;
    }

    private static Event event(
            int ordinal, SourceFile file, Pass pass, int peers, Status status, String rationale) {
        return new Event(ordinal, file.sourceId(), pass, file.platform(), file.categoryKey(),
                file.comparisonKey(), peers, file.interfaceSha256(), status, rationale);
    }

    private static List<Coverage> coverage(
            List<SourceFile> files,
            List<CompetitiveProblemCategoryCatalog.Category> categories,
            IProgressMonitor monitor) {
        Map<String, Set<String>> problems = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        for (SourceFile file : files) {
            check(monitor);
            String key = file.platform() + "\t" + file.categoryKey();
            counts.merge(key, 1, Math::addExact);
            problems.computeIfAbsent(key, ignored -> new HashSet<>()).add(normalize(file.problemKey()));
        }
        return categories.stream()
                .sorted(Comparator.comparing(CompetitiveProblemCategoryCatalog.Category::stableKey))
                .map(category -> {
                    check(monitor);
                    String key = category.platform().name() + "\t" + category.key();
                    int count = counts.getOrDefault(key, 0);
                    int distinct = problems.getOrDefault(key, Set.of()).size();
                    return new Coverage(category.platform(), category.key(), count, distinct,
                            count == 0 ? "NOT_INGESTED" : "INGESTED");
                })
                .toList();
    }

    private static String root(
            List<SourceFile> files, List<Event> events,
            Map<String, CandidateEvidence> bySourceId,
            Map<String, FileObservation> observations,
            Map<String, ProofAttestation> attestations,
            List<Coverage> coverage,
            IProgressMonitor monitor) {
        StringBuilder value = new StringBuilder();
        value.append("SYNEXIA_SERIAL_REVIEW_V2\n");
        for (SourceFile file : files) {
            check(monitor);
            value.append(file.sourceId()).append('\t').append(file.stableKey()).append('\t')
                    .append(file.problemKey()).append('\t').append(file.categoryKey()).append('\t')
                    .append(file.atomSha256()).append('\t').append(file.interfaceSha256()).append('\t')
                    .append(file.differentialProofSha256()).append('\t');
            CandidateEvidence candidate = bySourceId.get(file.sourceId());
            FileObservation observation = observations.get(file.sourceId());
            ProofAttestation attestation = attestations.get(file.sourceId());
            value.append(candidate == null ? "" : candidate.declaredRootSha256()).append('\t');
            value.append(observation == null ? "" : candidate.rootSha256(observation)).append('\t');
            if (attestation != null) {
                value.append(attestation.verifierId()).append('\t')
                        .append(attestation.evidenceRootSha256()).append('\t')
                        .append(attestation.cases()).append('\t').append(attestation.passed());
            }
            value.append('\n');
        }
        for (Event event : events) {
            check(monitor);
            value.append(event.ordinal()).append('\t').append(event.sourceId()).append('\t')
                    .append(event.pass()).append('\t').append(event.status()).append('\t')
                    .append(event.interfaceSha256()).append('\t').append(event.rationale()).append('\n');
        }
        for (Coverage row : coverage) {
            check(monitor);
            value.append(row.platform()).append('\t').append(row.categoryKey()).append('\t')
                    .append(row.sourceFiles()).append('\t').append(row.distinctProblems())
                    .append('\t').append(row.status()).append('\n');
        }
        return sha256(value.toString());
    }

    private static void check(IProgressMonitor monitor) {
        if (Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("problem review interrupted");
        }
        monitor.checkCanceled();
    }

    private static String normalize(String value) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char item = Character.toLowerCase(value.charAt(index));
            if (Character.isLetterOrDigit(item)) result.append(item);
        }
        return result.toString();
    }

    private static String hash(String value, String label) {
        String checked = required(value, label).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(label);
        return checked;
    }

    private static String required(String value, String label) {
        String checked = Objects.requireNonNull(value, label).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(label);
        return checked;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
