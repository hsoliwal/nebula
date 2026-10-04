// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressAlgorithm;
import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.job.IProgressMonitor;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One deduplicated logical challenge backed by every pinned Java implementation in the corpus.
 *
 * <p>External source files remain provenance. Execution is allowed only when all classified
 * implementations agree on one canonical computational shape.</p>
 */
public final class ChallengeProblem {

    public enum Status {
        EXECUTABLE,
        UNCLASSIFIED,
        CLASSIFICATION_CONFLICT
    }

    private static final Comparator<ProblemAdapter> REPRESENTATIVE_ORDER =
            Comparator.comparingInt(ChallengeProblem::confidenceRank)
                    .thenComparing(ProblemAdapter::id);

    private final ChallengeId id;
    private final String title;
    private final List<ProblemAdapter> implementations;
    private final Set<AlgorithmShape> classifiedShapes;
    private final Status status;
    private final ProblemAdapter representative;

    ChallengeProblem(
            ChallengeId id,
            String title,
            List<ProblemAdapter> implementations) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = requireTitle(title);
        this.implementations = implementations.stream()
                .sorted(Comparator.comparing(ProblemAdapter::id))
                .toList();
        if (this.implementations.isEmpty()) {
            throw new IllegalArgumentException("challenge requires at least one implementation");
        }
        for (ProblemAdapter adapter : this.implementations) {
            if (ChallengePlatform.from(adapter.source().platform()) != id.platform()) {
                throw new IllegalArgumentException("mixed-platform challenge: " + id.stableId());
            }
        }

        EnumSet<AlgorithmShape> shapes = EnumSet.noneOf(AlgorithmShape.class);
        this.implementations.stream()
                .filter(adapter -> adapter.classification().classified())
                .map(adapter -> adapter.classification().shape())
                .forEach(shapes::add);
        this.classifiedShapes = Set.copyOf(shapes);

        if (shapes.isEmpty()) {
            this.status = Status.UNCLASSIFIED;
        } else if (shapes.size() > 1) {
            this.status = Status.CLASSIFICATION_CONFLICT;
        } else {
            this.status = Status.EXECUTABLE;
        }

        AlgorithmShape resolved =
                status == Status.EXECUTABLE ? shapes.iterator().next() : null;
        this.representative = this.implementations.stream()
                .filter(adapter -> resolved == null
                        || resolved.equals(adapter.classification().shape()))
                .min(REPRESENTATIVE_ORDER)
                .orElse(this.implementations.getFirst());
    }

    public ChallengeId id() {
        return id;
    }

    public String title() {
        return title;
    }

    public List<ProblemAdapter> implementations() {
        return implementations;
    }

    public int implementationCount() {
        return implementations.size();
    }

    /** Every pinned donor origin retained for attribution/provenance. */
    public List<CorpusSourceEntry> sources() {
        return implementations.stream().map(ProblemAdapter::source).toList();
    }

    public Status status() {
        return status;
    }

    public boolean executable() {
        return status == Status.EXECUTABLE;
    }

    public Set<AlgorithmShape> classifiedShapes() {
        return classifiedShapes;
    }

    public Optional<AlgorithmShape> shape() {
        return classifiedShapes.size() == 1
                ? Optional.of(classifiedShapes.iterator().next())
                : Optional.empty();
    }

    public Optional<TemplateStyle> templateStyle() {
        return shape().map(ProblemTemplateCatalog::style);
    }

    public ProblemAdapter representative() {
        return representative;
    }

    public Object executeCanonical(Object donorInput, IProgressMonitor monitor) {
        requireExecutable();
        return representative.executeCanonical(donorInput, monitor);
    }

    public <I, K, V, O> ProgressAlgorithm<I, O> bind(
            ProblemAdapter.Encoder<I, K> encoder,
            ProblemAdapter.Decoder<I, V, O> decoder) {
        requireExecutable();
        return representative.bind(encoder, decoder);
    }

    private void requireExecutable() {
        if (!executable()) {
            throw new IllegalStateException(
                    "challenge is not executable: " + id.stableId()
                            + " status=" + status
                            + " shapes=" + classifiedShapes);
        }
    }

    private static int confidenceRank(ProblemAdapter adapter) {
        return switch (adapter.classification().confidence()) {
            case EXACT -> 0;
            case HEURISTIC -> 1;
            case UNCLASSIFIED -> 2;
        };
    }

    private static String requireTitle(String value) {
        String checked = Objects.requireNonNull(value, "title").trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("title must not be blank");
        return checked;
    }
}
