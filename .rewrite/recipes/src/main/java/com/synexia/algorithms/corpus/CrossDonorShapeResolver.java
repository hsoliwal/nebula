// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;

/**
 * Resolves previously-unclassified donor files from independent GitHub donor agreement.
 *
 * <p>Direct file classification always wins. Cross-donor consensus is deliberately conservative:
 * one exact vote per independent repository, at least two supporting repositories, and a strict
 * lead over the runner-up.
 */
public final class CrossDonorShapeResolver {

    public record Consensus(
            int problemNumber,
            AlgorithmShape shape,
            int supportingRepositories,
            int runnerUpRepositories,
            List<String> supporters) {

        public Consensus {
            if (problemNumber < 1) throw new IllegalArgumentException("problemNumber");
            Objects.requireNonNull(shape, "shape");
            if (supportingRepositories < 2) {
                throw new IllegalArgumentException("consensus requires >= 2 repositories");
            }
            if (runnerUpRepositories >= supportingRepositories) {
                throw new IllegalArgumentException("consensus requires a strict lead");
            }
            supporters = List.copyOf(Objects.requireNonNull(supporters, "supporters"));
            if (supporters.size() != supportingRepositories) {
                throw new IllegalArgumentException("supporter count mismatch");
            }
        }
    }

    public record Stats(
            int files,
            int directExact,
            int directHeuristic,
            int crossDonorResolved,
            int unresolved,
            int consensusProblems) {

        public Stats {
            if (files < 0 || directExact < 0 || directHeuristic < 0
                    || crossDonorResolved < 0 || unresolved < 0 || consensusProblems < 0) {
                throw new IllegalArgumentException("negative stats");
            }
            if (directExact + directHeuristic + crossDonorResolved + unresolved != files) {
                throw new IllegalArgumentException("classification partition mismatch");
            }
        }
    }

    private record State(
            Map<CorpusSourceEntry, ProblemShape> resolved,
            Map<CorpusSourceEntry, LeetCodeProblemIdentity.Identity> identities,
            Map<Integer, Consensus> consensus,
            Stats stats) {}

    private static final State STATE = build(JavaProblemCorpus.entries());

    private CrossDonorShapeResolver() {}

    public static ProblemShape resolve(CorpusSourceEntry source) {
        ProblemShape resolved = STATE.resolved().get(Objects.requireNonNull(source, "source"));
        return resolved != null ? resolved : source.classification();
    }

    public static OptionalInt problemNumber(CorpusSourceEntry source) {
        LeetCodeProblemIdentity.Identity identity =
                STATE.identities().get(Objects.requireNonNull(source, "source"));
        return identity == null ? OptionalInt.empty() : OptionalInt.of(identity.number());
    }

    public static Map<Integer, Consensus> consensus() {
        return STATE.consensus();
    }

    public static Stats stats() {
        return STATE.stats();
    }

    /** Deterministic donor-evidence table for every accepted problem-number consensus. */
    public static String consensusTsv() {
        StringBuilder out = new StringBuilder();
        out.append("problem_number\tshape\tsupporting_repositories\trunner_up_repositories\tsupporters\n");
        for (Map.Entry<Integer, Consensus> entry : STATE.consensus().entrySet()) {
            Consensus value = entry.getValue();
            out.append(value.problemNumber()).append('\t')
                    .append(value.shape()).append('\t')
                    .append(value.supportingRepositories()).append('\t')
                    .append(value.runnerUpRepositories()).append('\t')
                    .append(String.join(",", value.supporters())).append('\n');
        }
        return out.toString();
    }

    /** Deterministic target-file evidence for classifications inherited from donor consensus. */
    public static String promotedFilesTsv() {
        StringBuilder out = new StringBuilder();
        out.append("repository\tpath\tproblem_number\tshape\tidentity_evidence\trationale\n");
        for (CorpusSourceEntry source : JavaProblemCorpus.entries()) {
            ProblemShape direct = source.classification();
            if (direct.classified()) continue;
            LeetCodeProblemIdentity.Identity identity = STATE.identities().get(source);
            ProblemShape resolved = STATE.resolved().get(source);
            if (identity == null || resolved == null || !resolved.classified()) continue;
            out.append(clean(source.repository())).append('\t')
                    .append(clean(source.path())).append('\t')
                    .append(identity.number()).append('\t')
                    .append(resolved.shape()).append('\t')
                    .append(identity.evidence()).append('\t')
                    .append(clean(resolved.rationale())).append('\n');
        }
        return out.toString();
    }

    private static String clean(String value) {
        return Objects.requireNonNull(value, "value")
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    static State build(List<CorpusSourceEntry> sources) {
        Map<CorpusSourceEntry, LeetCodeProblemIdentity.Identity> identities =
                LeetCodeProblemIdentity.resolveAll(sources);

        LinkedHashMap<CorpusSourceEntry, ProblemShape> direct = new LinkedHashMap<>();
        LinkedHashMap<Integer, List<CorpusSourceEntry>> byNumber = new LinkedHashMap<>();
        int directExact = 0;
        int directHeuristic = 0;

        for (CorpusSourceEntry source : sources) {
            ProblemShape shape = source.classification();
            direct.put(source, shape);
            if (shape.confidence() == ClassificationConfidence.EXACT) directExact++;
            if (shape.confidence() == ClassificationConfidence.HEURISTIC) directHeuristic++;

            LeetCodeProblemIdentity.Identity identity = identities.get(source);
            if (identity != null) {
                byNumber.computeIfAbsent(identity.number(), ignored -> new ArrayList<>()).add(source);
            }
        }

        LinkedHashMap<Integer, Consensus> consensus = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<CorpusSourceEntry>> group : byNumber.entrySet()) {
            Consensus value = consensus(group.getKey(), group.getValue(), direct);
            if (value != null) consensus.put(group.getKey(), value);
        }

        LinkedHashMap<CorpusSourceEntry, ProblemShape> resolved = new LinkedHashMap<>();
        int crossDonorResolved = 0;
        int unresolved = 0;

        for (CorpusSourceEntry source : sources) {
            ProblemShape local = direct.get(source);
            if (local.classified()) {
                resolved.put(source, local);
                continue;
            }

            LeetCodeProblemIdentity.Identity identity = identities.get(source);
            Consensus inherited = identity == null ? null : consensus.get(identity.number());
            if (inherited == null) {
                resolved.put(source, local);
                unresolved++;
                continue;
            }

            crossDonorResolved++;
            resolved.put(
                    source,
                    new ProblemShape(
                            source.path(),
                            inherited.shape(),
                            ProblemTemplateCatalog.style(inherited.shape()).kind(),
                            ClassificationConfidence.HEURISTIC,
                            "cross-donor LeetCode #"
                                    + inherited.problemNumber()
                                    + " consensus: "
                                    + inherited.shape()
                                    + " from "
                                    + inherited.supportingRepositories()
                                    + " independent exact donor repositories; runner-up="
                                    + inherited.runnerUpRepositories()
                                    + "; identity="
                                    + identity.evidence()));
        }

        return new State(
                Collections.unmodifiableMap(resolved),
                Collections.unmodifiableMap(new LinkedHashMap<>(identities)),
                Collections.unmodifiableMap(consensus),
                new Stats(
                        sources.size(),
                        directExact,
                        directHeuristic,
                        crossDonorResolved,
                        unresolved,
                        consensus.size()));
    }

    private static Consensus consensus(
            int problemNumber,
            List<CorpusSourceEntry> sources,
            Map<CorpusSourceEntry, ProblemShape> direct) {

        TreeMap<String, AlgorithmShape> repositoryVotes = new TreeMap<>();
        Set<String> repositories = new java.util.TreeSet<>();
        for (CorpusSourceEntry source : sources) repositories.add(source.repository());

        for (String repository : repositories) {
            java.util.EnumSet<AlgorithmShape> exact = java.util.EnumSet.noneOf(AlgorithmShape.class);
            for (CorpusSourceEntry source : sources) {
                if (!source.repository().equals(repository)) continue;
                ProblemShape shape = direct.get(source);
                if (shape.confidence() == ClassificationConfidence.EXACT) {
                    exact.add(shape.shape());
                }
            }
            if (exact.size() == 1) {
                repositoryVotes.put(repository, exact.iterator().next());
            }
        }

        EnumMap<AlgorithmShape, Integer> counts = new EnumMap<>(AlgorithmShape.class);
        for (AlgorithmShape shape : repositoryVotes.values()) counts.merge(shape, 1, Integer::sum);
        if (counts.isEmpty()) return null;

        List<Map.Entry<AlgorithmShape, Integer>> ranked = new ArrayList<>(counts.entrySet());
        ranked.sort(Comparator
                .<Map.Entry<AlgorithmShape, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(entry -> entry.getKey().name()));

        Map.Entry<AlgorithmShape, Integer> winner = ranked.get(0);
        int runnerUp = ranked.size() > 1 ? ranked.get(1).getValue() : 0;
        if (winner.getValue() < 2 || winner.getValue() <= runnerUp) return null;

        List<String> supporters = repositoryVotes.entrySet().stream()
                .filter(entry -> entry.getValue() == winner.getKey())
                .map(Map.Entry::getKey)
                .toList();

        return new Consensus(
                problemNumber,
                winner.getKey(),
                winner.getValue(),
                runnerUp,
                supporters);
    }
}
