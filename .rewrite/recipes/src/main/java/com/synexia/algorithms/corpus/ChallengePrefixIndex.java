// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.job.IProgressMonitor;
import java.util.List;
import java.util.Objects;

/**
 * Immutable prefix-index facade over the canonical prepared Aho-Corasick trie.
 *
 * <p>This type owns no node/edge storage. Preparation delegates to
 * {@link ChallengeMultiPatternSearch#prepare(List, IProgressMonitor)} and every query reuses that
 * immutable automaton. Duplicate input patterns are retained as multiplicity, matching static
 * prefix-count workloads. Online mutable challenge APIs can batch/rebuild through this immutable
 * boundary without creating a competing trie representation.</p>
 */
public final class ChallengePrefixIndex {
    private final ChallengeMultiPatternSearch.Prepared automaton;

    private ChallengePrefixIndex(ChallengeMultiPatternSearch.Prepared automaton) {
        this.automaton = Objects.requireNonNull(automaton, "automaton");
    }

    public static ChallengePrefixIndex prepare(List<byte[]> values) {
        return prepare(values, null);
    }

    public static ChallengePrefixIndex prepare(
            List<byte[]> values, IProgressMonitor monitor) {
        return new ChallengePrefixIndex(ChallengeMultiPatternSearch.prepare(values, monitor));
    }

    public int size() {
        return automaton.patternCount();
    }

    public int prefixCount(byte[] prefix) {
        return prefixCount(prefix, null);
    }

    public int prefixCount(byte[] prefix, IProgressMonitor monitor) {
        return ChallengeMultiPatternSearch.prefixCount(automaton, prefix, monitor);
    }

    public int exactCount(byte[] value) {
        return exactCount(value, null);
    }

    public int exactCount(byte[] value, IProgressMonitor monitor) {
        return ChallengeMultiPatternSearch.exactCount(automaton, value, monitor);
    }

    public boolean contains(byte[] value) {
        return exactCount(value) != 0;
    }

    public boolean startsWith(byte[] prefix) {
        return prefixCount(prefix) != 0;
    }

    ChallengeMultiPatternSearch.Prepared preparedAutomaton() {
        return automaton;
    }
}
