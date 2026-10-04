// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.iop.patterns.PatternCatalog;
import com.synexia.job.IProgressMonitor;
import com.synexia.m3.contract.CodeAtom;
import com.synexia.m3.contract.JavaAtomExtractor;
import com.synexia.m3.contract.JavaFileComposition;
import com.synexia.m3.contract.M3Invariant;
import com.synexia.m3.contract.SerialAtomReview;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Source-bound admission for one recipe-owned behavior atom. Reuses the existing parser,
 * composition, pattern catalogue and serial contract fence; it is not a promotion engine.
 *
 * <p>The language-neutral span/proof contract currently has one Java 21 member adapter.
 * COMPLETE partitions account for source; they do not prove statement-level patternization.
 * An independent host verifier owns semantic truth and external compiler/test receipts.</p>
 */
public final class M3SourceCoverageGate {
    public static final String JAVA_ADAPTER = "jdk21-member-composition/1";
    public static final int MAX_SOURCE_UNITS = 1_048_576;
    public enum Status { VERIFIED_CHANGE, NO_OP, BLOCKED }
    public enum Kind { ATOM, FIELD_GROUP, UNCLASSIFIED_SOURCE }
    public enum Stage { DIFF, LINT, COMPILE, TEST, RUNTIME, SEMANTIC_CONTRACT }
    public enum Outcome { PASS, FAIL, NOT_EXECUTED }

    /** Half-open UTF-16 coordinates; each source position belongs to exactly one span. */
    public record Span(int ordinal, int start, int end, Kind kind, String text, String id) {
        public Span {
            kind = Objects.requireNonNull(kind, "kind");
            text = Objects.requireNonNull(text, "text");
            id = M3CodeAtom.sha256(id, "id");
            if (ordinal < 0 || start < 0 || end < start || text.length() != end - start) {
                throw new IllegalArgumentException("invalid span");
            }
        }
    }

    public record PatternBinding(String patternId, String role, com.synexia.rewrite.sealed.SealedContract contract) {
        public PatternBinding {
            patternId = M3CodeAtom.required(patternId, "patternId");
            role = M3CodeAtom.required(role, "role");
            contract = Objects.requireNonNull(contract, "contract");
        }
        public String contractRoot() { return contract.root(); }
    }

    public record Context(String sourcePath, String parentSourceRoot,
                          String recipeClass, String recipeArtifactRoot, String recipeCatalogueRoot,
                          String producerId, String verifierId, boolean runtimeRequired,
                          M3Invariant invariant) {
        public Context {
            sourcePath = portable(Path.of(sourcePath));
            parentSourceRoot = M3CodeAtom.sha256(parentSourceRoot, "parentSourceRoot");
            recipeClass = M3CodeAtom.required(recipeClass, "recipeClass");
            recipeArtifactRoot = M3CodeAtom.sha256(recipeArtifactRoot, "recipeArtifactRoot");
            recipeCatalogueRoot = M3CodeAtom.sha256(recipeCatalogueRoot, "recipeCatalogueRoot");
            producerId = M3CodeAtom.required(producerId, "producerId");
            verifierId = M3CodeAtom.required(verifierId, "verifierId");
            invariant = Objects.requireNonNull(invariant, "invariant");
            if (producerId.equals(verifierId)) throw new IllegalArgumentException("SELF_VERIFICATION_FORBIDDEN");
        }
    }

    /** Full immutable parent and candidate are retained so a verifier need not trust summaries. */
    public record ProofRequest(String path, String beforeSource, String afterSource,
                               String beforeSourceRoot, String afterSourceRoot,
                               String beforePartitionRoot, String afterPartitionRoot,
                               CodeAtom beforeAtom, CodeAtom afterAtom, PatternBinding pattern,
                               String patternCatalogueRoot, String patternDescriptorRoot,
                               String patternRecipeRoot, String unchangedOutsideRoot,
                               Context context, String root) {}

    public record Evidence(Stage stage, Outcome outcome, String requestRoot,
                           String artifactRoot, String verifierId) {
        public Evidence {
            stage = Objects.requireNonNull(stage, "stage");
            outcome = Objects.requireNonNull(outcome, "outcome");
            requestRoot = M3CodeAtom.sha256(requestRoot, "requestRoot");
            artifactRoot = M3CodeAtom.sha256(artifactRoot, "artifactRoot");
            verifierId = M3CodeAtom.required(verifierId, "verifierId");
        }
    }

    /** Trusted host boundary, separate from the candidate producer; names alone prove nothing. */
    public interface Verifier {
        String id();
        Evidence verify(Stage stage, ProofRequest request) throws Exception;
    }

    /** Instances can only be obtained by executing this gate, never by supplying proof booleans. */
    public static final class Result {
        private final Status status;
        private final String reason;
        private final String path;
        private final String beforeRoot;
        private final String afterRoot;
        private final Context context;
        private final List<Span> beforeSpans;
        private final List<Span> afterSpans;
        private final List<M3RecipePassReceipt.AtomResult> atoms;
        private final List<Evidence> evidence;
        private final ProofRequest request;
        private Result(Status status, String reason, String path, String beforeRoot, String afterRoot,
                       Context context, List<Span> beforeSpans, List<Span> afterSpans,
                       List<M3RecipePassReceipt.AtomResult> atoms, List<Evidence> evidence, ProofRequest request) {
            this.status = status; this.reason = reason; this.path = path;
            this.beforeRoot = beforeRoot; this.afterRoot = afterRoot; this.context = context;
            this.beforeSpans = List.copyOf(beforeSpans); this.afterSpans = List.copyOf(afterSpans);
            this.atoms = List.copyOf(atoms); this.evidence = List.copyOf(evidence); this.request = request;
        }
        public Status status() { return status; }
        public String reason() { return reason; }
        public String path() { return path; }
        public String beforeRoot() { return beforeRoot; }
        public String afterRoot() { return afterRoot; }
        public List<Span> beforeSpans() { return beforeSpans; }
        public List<Span> afterSpans() { return afterSpans; }
        public List<M3RecipePassReceipt.AtomResult> atoms() { return atoms; }
        public List<Evidence> evidence() { return evidence; }
        public ProofRequest request() { return request; }
        public boolean sourceMutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
        boolean matches(M3RecipePassReceipt receipt) {
            return context.recipeClass().equals(receipt.recipeClassName())
                    && context.recipeArtifactRoot().equals(receipt.recipeArtifactSha256())
                    && context.recipeCatalogueRoot().equals(receipt.catalogueRootSha256())
                    && context.recipeCatalogueRoot().equals(receipt.catalogueSnapshotSha256())
                    && beforeRoot.equals(receipt.preimageRootSha256())
                    && afterRoot.equals(receipt.postimageRootSha256())
                    && atoms.equals(receipt.atoms());
        }
    }

    public Result review(Path sourcePath, String before, String after, Context context,
                         PatternCatalog catalogue, List<PatternBinding> bindings, Verifier verifier,
                         IProgressMonitor monitor) throws Exception {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(catalogue, "catalogue");
        List<PatternBinding> checkedBindings = List.copyOf(bindings);
        Objects.requireNonNull(verifier, "verifier");
        Objects.requireNonNull(monitor, "monitor").checkCanceled();
        String path = portable(sourcePath);
        requireSource(before); requireSource(after);
        String pre = hash(before), post = hash(after);
        if (!context.sourcePath().equals(path) || !context.parentSourceRoot().equals(pre)) {
            throw new IllegalArgumentException("STALE_OR_FOREIGN_PARENT_SOURCE");
        }
        if (!context.verifierId().equals(verifier.id())) throw new IllegalArgumentException("VERIFIER_ID_MISMATCH");
        if (!path.endsWith(".java")) {
            return result(Status.BLOCKED, "UNSUPPORTED_LANGUAGE", path, pre, post, context,
                    List.of(), List.of(), List.of(), List.of(), null);
        }
        var extractor = new JavaAtomExtractor();
        var prior = extractor.compose(Path.of(path), before, monitor);
        var next = extractor.compose(Path.of(path), after, monitor);
        List<Span> beforeSpans = spans(prior), afterSpans = spans(next);
        validatePartition(path, before, beforeSpans); validatePartition(path, after, afterSpans);
        if (prior.status() != JavaFileComposition.Status.COMPLETE || next.status() != JavaFileComposition.Status.COMPLETE) {
            return result(Status.BLOCKED, "UNSUPPORTED_PARTITION:" + prior.status() + "/" + next.status(),
                    path, pre, post, context, beforeSpans, afterSpans, List.of(), List.of(), null);
        }
        var serial = new SerialAtomReview();
        M3Invariant actual = serial.invariantFor(Path.of(path), before, context.invariant().scope(),
                context.invariant().contextRoot(), context.invariant().environmentRoot());
        if (!context.invariant().compatibleWith(actual)) throw new IllegalArgumentException("STALE_PARENT_INVARIANT");
        if (before.equals(after)) {
            if (!checkedBindings.isEmpty()) throw new IllegalArgumentException("UNUSED_PATTERN_BINDING");
            return result(Status.NO_OP, "SOURCE_BOUND_NO_OP; unchanged residue is not semantic proof", path,
                    pre, post, context, beforeSpans, afterSpans, List.of(), List.of(), null);
        }
        int changed = -1;
        String residue = "";
        if (beforeSpans.size() != afterSpans.size()) residue = "ATOM_TOPOLOGY_CHANGED";
        else for (int i = 0; i < beforeSpans.size(); i++) {
            Span b = beforeSpans.get(i), a = afterSpans.get(i);
            if (b.kind() != a.kind()) { residue = "ATOM_TOPOLOGY_CHANGED"; break; }
            if (!b.text().equals(a.text())) {
                if (b.kind() != Kind.ATOM) { residue = "CHANGED_" + b.kind(); break; }
                if (changed != -1) { residue = "MULTI_ATOM_PASS_REQUIRES_SERIAL_DECOMPOSITION"; break; }
                changed = i;
            }
        }
        if (residue.isEmpty() && changed < 0) residue = "UNACCOUNTED_SOURCE_CHANGE";
        if (!residue.isEmpty()) return result(Status.BLOCKED, residue, path, pre, post, context,
                beforeSpans, afterSpans, List.of(), List.of(), null);
        if (checkedBindings.size() != 1) return result(Status.BLOCKED, "CHANGED_ATOM_PATTERN_BINDING_REQUIRED",
                path, pre, post, context, beforeSpans, afterSpans, List.of(), List.of(), null);
        CodeAtom b = prior.segments().get(changed).atoms().getFirst();
        CodeAtom a = next.segments().get(changed).atoms().getFirst();
        if (!b.owner().equals(a.owner()) || b.kind() != a.kind() || !b.name().equals(a.name())) {
            return result(Status.BLOCKED, "ATOM_IDENTITY_DRIFT", path, pre, post, context,
                    beforeSpans, afterSpans, List.of(), List.of(), null);
        }
        String prefix = before.substring(0, Math.toIntExact(b.startOffset()));
        String suffix = before.substring(Math.toIntExact(b.endOffset()));
        if (!prefix.equals(after.substring(0, Math.toIntExact(a.startOffset())))
                || !suffix.equals(after.substring(Math.toIntExact(a.endOffset())))) {
            throw new IllegalStateException("UNCHANGED_OUTSIDE_SCOPE_FAILED");
        }
        var serialResult = serial.converge(Path.of(path), before, context.invariant(),
                ignored -> java.util.Optional.of(after), 1, monitor);
        if (serialResult.decisions().size() != 1 || !serialResult.decisions().getFirst().accepted()) {
            return result(Status.BLOCKED, "SERIAL_CONTRACT_REJECTED", path, pre, post, context,
                    beforeSpans, afterSpans, List.of(), List.of(), null);
        }
        PatternBinding binding = checkedBindings.getFirst();
        var descriptor = catalogue.get(binding.patternId());
        if (descriptor.isEmpty()) return result(Status.BLOCKED, "UNKNOWN_PATTERN", path, pre, post, context,
                beforeSpans, afterSpans, List.of(), List.of(), null);
        String outside = framed("M3_UNCHANGED_OUTSIDE/1", prefix, suffix);
        String requestRoot = framed("M3_SOURCE_BOUND_PROOF/1", path, JAVA_ADAPTER, pre, post,
                prior.root(), next.root(), atomId(b), atomId(a), binding.patternId(), binding.role(),
                binding.contractRoot(), catalogue.root(), descriptor.get().root(), catalogue.recipe(binding.patternId()).root(),
                outside, context.recipeClass(), context.recipeArtifactRoot(), context.recipeCatalogueRoot(),
                context.producerId(), context.verifierId(), Boolean.toString(context.runtimeRequired()), context.invariant().root());
        var request = new ProofRequest(path, before, after, pre, post, prior.root(), next.root(), b, a,
                binding, catalogue.root(), descriptor.get().root(), catalogue.recipe(binding.patternId()).root(),
                outside, context, requestRoot);
        var evidence = new ArrayList<Evidence>();
        for (Stage stage : Stage.values()) {
            if (stage == Stage.RUNTIME && !context.runtimeRequired()) continue;
            monitor.checkCanceled();
            Evidence proof = Objects.requireNonNull(verifier.verify(stage, request), "proof");
            monitor.checkCanceled();
            evidence.add(proof);
            if (proof.stage() != stage || proof.outcome() != Outcome.PASS
                    || !proof.requestRoot().equals(requestRoot) || !proof.verifierId().equals(context.verifierId())) {
                return result(Status.BLOCKED, "UNVERIFIED_" + stage, path, pre, post, context,
                        beforeSpans, afterSpans, List.of(), evidence, request);
            }
        }
        var atom = new M3CodeAtom(atomId(b), path, b.owner() + "#" + b.name(), binding.contractRoot(),
                b.sha256(), M3CodeAtom.State.KNOWN_OLD);
        return result(Status.VERIFIED_CHANGE, "VERIFIED_BY_HOST; serial canonical promotion still required",
                path, pre, post, context, beforeSpans, afterSpans,
                List.of(new M3RecipePassReceipt.AtomResult(atom, true, binding.contractRoot())), evidence, request);
    }

    private static Result result(Status status, String reason, String path, String pre, String post,
                                 Context context, List<Span> before, List<Span> after,
                                 List<M3RecipePassReceipt.AtomResult> atoms, List<Evidence> evidence, ProofRequest request) {
        return new Result(status, reason, path, pre, post, context, before, after, atoms, evidence, request);
    }
    private static List<Span> spans(JavaFileComposition composition) {
        return composition.segments().stream().map(s -> new Span(s.ordinal(), s.start(), s.end(),
                Kind.valueOf(s.kind().name()), s.text(), spanId(composition.sourcePath(), composition.sourceSha256(),
                        s.ordinal(), s.start(), s.end(), Kind.valueOf(s.kind().name())))).toList();
    }
    static void validatePartition(String path, String source, List<Span> spans) {
        if (spans.isEmpty()) throw new IllegalArgumentException("EMPTY_PARTITION");
        int cursor = 0;
        String sourceRoot = hash(source);
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            if (span.ordinal() != i || span.start() != cursor || span.end() > source.length()
                    || (span.end() == span.start() && !(source.isEmpty() && spans.size() == 1))
                    || !span.text().equals(source.substring(span.start(), span.end()))
                    || !span.id().equals(spanId(path, sourceRoot, i, span.start(), span.end(), span.kind()))) {
                throw new IllegalArgumentException("SOURCE_PARTITION_MISMATCH");
            }
            cursor = span.end();
        }
        if (cursor != source.length()) throw new IllegalArgumentException("UNCOVERED_SOURCE");
    }
    static String spanId(String path, String sourceRoot, int ordinal, int start, int end, Kind kind) {
        return framed("M3_SOURCE_SPAN/1", path, sourceRoot, JAVA_ADAPTER, Integer.toString(ordinal),
                Integer.toString(start), Integer.toString(end), kind.name());
    }
    static String atomId(CodeAtom atom) {
        return framed("M3_BOUND_ATOM/1", atom.sourcePath(), atom.owner(), atom.kind().name(), atom.name(),
                Long.toString(atom.startOffset()), Long.toString(atom.endOffset()), atom.sha256());
    }
    static String portable(Path path) {
        return com.synexia.rewrite.sealed.SealedSources.path(Objects.requireNonNull(path, "sourcePath").toString());
    }
    private static void requireSource(String source) {
        Objects.requireNonNull(source, "source");
        if (source.length() > MAX_SOURCE_UNITS) throw new IllegalArgumentException("SOURCE_BUDGET");
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i == source.length() || !Character.isLowSurrogate(source.charAt(i))) throw new IllegalArgumentException("MALFORMED_UTF16");
            } else if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("MALFORMED_UTF16");
        }
    }
    static String framed(String... values) {
        return com.synexia.rewrite.sealed.SealHash.frame(values);
    }
    public static String hash(String value) {
        return com.synexia.rewrite.sealed.SealHash.text(value);
    }
}
