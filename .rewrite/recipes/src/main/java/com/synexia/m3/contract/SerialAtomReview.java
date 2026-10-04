// SPDX-License-Identifier: Apache-2.0
package com.synexia.m3.contract;

import com.synexia.job.IProgressMonitor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only single-file/single-atom M3 convergence engine.
 *
 * <p>Each pass sees a fresh JDK AST atom catalogue. A candidate is admitted only if its complete
 * source delta is inside exactly one non-type atom, parsing succeeds, and the public/protected
 * contract plus locked context remain unchanged. Zero max passes means run until stable, rejected,
 * cycled, canceled, or a producer error. This class never writes the repository.</p>
 */
public final class SerialAtomReview {

    public enum Status {
        STABLE, ACCEPTED, REJECTED_NO_ATOM, REJECTED_CONTRACT, REJECTED_CYCLE
    }

    public record Request(
            Path sourcePath,
            String source,
            int pass,
            List<CodeAtom> atoms,
            M3Invariant invariant,
            String sourceSha256,
            IProgressMonitor monitor) {

        public Request {
            sourcePath = Objects.requireNonNull(sourcePath, "sourcePath").normalize();
            source = Objects.requireNonNull(source, "source");
            if (pass < 0) throw new IllegalArgumentException("pass");
            atoms = List.copyOf(Objects.requireNonNull(atoms, "atoms"));
            invariant = Objects.requireNonNull(invariant, "invariant");
            sourceSha256 = sha(sourceSha256, "sourceSha256");
            monitor = Objects.requireNonNull(monitor, "monitor");
        }

        public List<CodeAtom> replaceableAtoms() {
            return atoms.stream().filter(atom -> atom.kind() != CodeAtomKind.TYPE).toList();
        }
    }

    @FunctionalInterface
    public interface CandidateProducer {
        Optional<String> propose(Request request) throws Exception;
    }

    public record Decision(
            Status status,
            int pass,
            String beforeSha256,
            String afterSha256,
            CodeAtom atom,
            M3Invariant beforeInvariant,
            M3Invariant afterInvariant,
            ApiContractDiff.Result contractDiff,
            String source,
            String reason,
            String root) {

        public Decision {
            status = Objects.requireNonNull(status, "status");
            if (pass < 0) throw new IllegalArgumentException("pass");
            beforeSha256 = sha(beforeSha256, "beforeSha256");
            afterSha256 = sha(afterSha256, "afterSha256");
            atom = Objects.requireNonNull(atom, "atom");
            beforeInvariant = Objects.requireNonNull(beforeInvariant, "beforeInvariant");
            afterInvariant = Objects.requireNonNull(afterInvariant, "afterInvariant");
            contractDiff = Objects.requireNonNull(contractDiff, "contractDiff");
            source = Objects.requireNonNull(source, "source");
            reason = Objects.requireNonNull(reason, "reason");
            String expected = digest(
                    "SYNEXIA-SERIAL-ATOM-DECISION/2",
                    status.name(),
                    Integer.toString(pass),
                    beforeSha256,
                    afterSha256,
                    atom.sha256(),
                    beforeInvariant.root(),
                    afterInvariant.root(),
                    Boolean.toString(contractDiff.compatible()),
                    reason);
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("SERIAL_ATOM_DECISION_ROOT_MISMATCH");
            }
        }

        public boolean accepted() {
            return status == Status.ACCEPTED;
        }
    }

    public record Result(String source, M3Invariant invariant, List<Decision> decisions, String root) {
        public Result {
            source = Objects.requireNonNull(source, "source");
            invariant = Objects.requireNonNull(invariant, "invariant");
            decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions"));
            String expected = digest(
                    "SYNEXIA-SERIAL-ATOM-RESULT/2",
                    invariant.root(),
                    sha256(source),
                    decisions.stream().map(Decision::root).toList().toString());
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("SERIAL_ATOM_RESULT_ROOT_MISMATCH");
            }
        }
    }

    /**
     * Creates the locked invariant for a source snapshot using this engine's canonical external
     * API fingerprint. Callers therefore cannot accidentally define a weaker contract root.
     */
    public M3Invariant invariantFor(
            Path sourcePath,
            String source,
            M3Invariant.Scope scope,
            String contextRoot,
            String environmentRoot) throws IOException {
        Path path = Objects.requireNonNull(sourcePath, "sourcePath").normalize();
        Parsed parsed = parse(path, Objects.requireNonNull(source, "source"));
        return M3Invariant.of(
                Objects.requireNonNull(scope, "scope"),
                contextRoot,
                publicContractRoot(parsed.api()),
                environmentRoot);
    }

    public Result converge(
            Path sourcePath,
            String baselineSource,
            M3Invariant invariant,
            CandidateProducer producer) throws Exception {
        return converge(
                sourcePath, baselineSource, invariant, producer, 0, IProgressMonitor.noop());
    }

    public Result converge(
            Path sourcePath,
            String baselineSource,
            M3Invariant invariant,
            CandidateProducer producer,
            int maxPasses,
            IProgressMonitor monitor) throws Exception {
        Path path = Objects.requireNonNull(sourcePath, "sourcePath").normalize();
        String source = Objects.requireNonNull(baselineSource, "baselineSource");
        M3Invariant baseInvariant = Objects.requireNonNull(invariant, "invariant");
        CandidateProducer checkedProducer = Objects.requireNonNull(producer, "producer");
        IProgressMonitor checkedMonitor =
                monitor == null ? IProgressMonitor.noop() : monitor;
        if (maxPasses < 0) throw new IllegalArgumentException("maxPasses");

        Set<String> seen = new HashSet<>();
        List<Decision> decisions = new ArrayList<>();
        seen.add(sha256(source));
        checkedMonitor.beginTask(
                "m3-serial-atom-convergence",
                maxPasses > 0 ? maxPasses : IProgressMonitor.UNKNOWN);
        try {
            int pass = 0;
            while (maxPasses == 0 || pass < maxPasses) {
                checkedMonitor.checkCanceled();
                Request request =
                        makeRequest(path, source, pass, baseInvariant, checkedMonitor);
                Optional<String> proposed = checkedProducer.propose(request);
                checkedMonitor.checkCanceled();
                if (proposed.isEmpty() || proposed.get().equals(source)) {
                    decisions.add(stable(request, pass));
                    break;
                }

                Decision decision = reviewCandidate(request, proposed.get(), pass);
                decisions.add(decision);
                if (!decision.accepted()) break;
                if (!seen.add(decision.afterSha256())) {
                    decisions.add(new Decision(
                            Status.REJECTED_CYCLE,
                            pass,
                            decision.afterSha256(),
                            decision.afterSha256(),
                            decision.atom(),
                            decision.afterInvariant(),
                            decision.afterInvariant(),
                            decision.contractDiff(),
                            source,
                            "source root already seen; convergence would cycle",
                            ""));
                    break;
                }

                source = proposed.get();
                pass++;
                checkedMonitor.worked(1L);
            }
        } finally {
            checkedMonitor.done();
        }
        return new Result(
                source,
                currentInvariant(path, source, baseInvariant),
                decisions,
                "");
    }

    private Request makeRequest(
            Path path,
            String source,
            int pass,
            M3Invariant invariant,
            IProgressMonitor monitor) throws IOException {
        Parsed parsed = parse(path, source);
        return new Request(
                path, source, pass, parsed.atoms(), invariant, sha256(source), monitor);
    }

    private Decision reviewCandidate(
            Request request, String candidateSource, int pass) throws IOException {
        Change change = change(request.source(), candidateSource);
        List<CodeAtom> containing = request.replaceableAtoms().stream()
                .filter(atom -> change.start() >= atom.startOffset()
                        && change.endBefore() <= atom.endOffset())
                .sorted(Comparator.comparingLong(CodeAtom::startOffset))
                .toList();
        if (containing.size() != 1) {
            CodeAtom evidence = containing.isEmpty()
                    ? request.replaceableAtoms().stream().findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "SERIAL_ATOM_NO_REPLACEABLE_ATOM"))
                    : containing.getFirst();
            return rejected(
                    Status.REJECTED_NO_ATOM,
                    request,
                    candidateSource,
                    pass,
                    evidence,
                    "candidate changed outside exactly one replaceable atom");
        }

        Parsed before = parse(request.sourcePath(), request.source());
        Parsed candidate = parse(request.sourcePath(), candidateSource);
        M3Invariant afterInvariant = M3Invariant.of(
                request.invariant().scope(),
                request.invariant().contextRoot(),
                publicContractRoot(candidate.api()),
                request.invariant().environmentRoot());
        ApiContractDiff.Result diff = new ApiContractDiff()
                .compare(before.api(), candidate.api(), Set.of());

        if (!request.invariant().compatibleWith(afterInvariant) || !diff.compatible()) {
            return new Decision(
                    Status.REJECTED_CONTRACT,
                    pass,
                    request.sourceSha256(),
                    sha256(candidateSource),
                    containing.getFirst(),
                    request.invariant(),
                    afterInvariant,
                    diff,
                    request.source(),
                    !request.invariant().compatibleWith(afterInvariant)
                            ? "context/interface/environment invariant changed"
                            : "external API contract removal",
                    "");
        }

        return new Decision(
                Status.ACCEPTED,
                pass,
                request.sourceSha256(),
                sha256(candidateSource),
                containing.getFirst(),
                request.invariant(),
                afterInvariant,
                diff,
                candidateSource,
                "exactly one replaceable atom changed with compatible public contract",
                "");
    }

    private Decision stable(Request request, int pass) throws IOException {
        Parsed parsed = parse(request.sourcePath(), request.source());
        M3Invariant current = M3Invariant.of(
                request.invariant().scope(),
                request.invariant().contextRoot(),
                publicContractRoot(parsed.api()),
                request.invariant().environmentRoot());
        ApiContractDiff.Result diff =
                new ApiContractDiff().compare(parsed.api(), parsed.api(), Set.of());
        CodeAtom atom = request.replaceableAtoms().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "SERIAL_ATOM_NO_REPLACEABLE_ATOM"));
        return new Decision(
                Status.STABLE,
                pass,
                request.sourceSha256(),
                request.sourceSha256(),
                atom,
                current,
                current,
                diff,
                request.source(),
                "candidate producer reached stability",
                "");
    }

    private Decision rejected(
            Status status,
            Request request,
            String candidateSource,
            int pass,
            CodeAtom atom,
            String reason) throws IOException {
        Parsed before = parse(request.sourcePath(), request.source());
        M3Invariant current = M3Invariant.of(
                request.invariant().scope(),
                request.invariant().contextRoot(),
                publicContractRoot(before.api()),
                request.invariant().environmentRoot());
        ApiContractDiff.Result diff =
                new ApiContractDiff().compare(before.api(), before.api(), Set.of());
        return new Decision(
                status,
                pass,
                request.sourceSha256(),
                sha256(candidateSource),
                atom,
                request.invariant(),
                current,
                diff,
                request.source(),
                reason,
                "");
    }

    private Parsed parse(Path path, String source) throws IOException {
        Path temp = Files.createTempDirectory("synexia-m3-atom-");
        try {
            Path file = temp.resolve(path);
            if (!file.normalize().startsWith(temp.normalize())) {
                throw new IllegalArgumentException("SERIAL_ATOM_PATH_TRAVERSAL");
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, source, StandardCharsets.UTF_8);
            return new Parsed(
                    new JavaAtomExtractor().extract(temp),
                    new JavaApiExtractor().snapshot(temp));
        } finally {
            deleteTree(temp);
        }
    }

    private record Parsed(List<CodeAtom> atoms, JavaApiSnapshot api) {}
    private record Change(int start, int endBefore, int endAfter) {}

    private static Change change(String before, String after) {
        int common = 0;
        int prefixLimit = Math.min(before.length(), after.length());
        while (common < prefixLimit && before.charAt(common) == after.charAt(common)) common++;
        if (common == before.length() && common == after.length()) {
            return new Change(common, common, common);
        }
        int suffix = 0;
        int suffixLimit = Math.min(before.length() - common, after.length() - common);
        while (suffix < suffixLimit
                && before.charAt(before.length() - 1 - suffix)
                        == after.charAt(after.length() - 1 - suffix)) suffix++;
        return new Change(
                common, before.length() - suffix, after.length() - suffix);
    }

    private static M3Invariant currentInvariant(
            Path path, String source, M3Invariant base) throws IOException {
        Parsed parsed = new SerialAtomReview().parse(path, source);
        return M3Invariant.of(
                base.scope(),
                base.contextRoot(),
                publicContractRoot(parsed.api()),
                base.environmentRoot());
    }

    private static String publicContractRoot(JavaApiSnapshot snapshot) {
        return digest(
                "SYNEXIA-M3-PUBLIC-CONTRACT/1",
                snapshot.members().stream()
                        .filter(ApiMember::externalContract)
                        .map(ApiMember::fingerprint)
                        .sorted()
                        .toList()
                        .toString());
    }

    private static void deleteTree(Path root) throws IOException {
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException failure) {
                    throw new java.io.UncheckedIOException(failure);
                }
            });
        } catch (java.io.UncheckedIOException failure) {
            throw failure.getCause();
        }
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.requireNonNull(value, "value")
                        .getBytes(StandardCharsets.UTF_8);
                digest.update(Integer.toString(bytes.length)
                        .getBytes(StandardCharsets.US_ASCII));
                digest.update((byte) ':');
                digest.update(bytes);
                digest.update((byte) '\n');
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
