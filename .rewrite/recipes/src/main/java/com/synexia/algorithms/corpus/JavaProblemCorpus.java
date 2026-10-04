// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Loads the pinned Java donor-corpus manifests and exposes precomputed immutable projections. */
public final class JavaProblemCorpus {

    private static final String RESOURCE_ROOT = "/com/synexia/algorithms/corpus/";
    private static final String RESOURCE_INDEX = RESOURCE_ROOT + "corpus-resources.txt";
    private static final List<CorpusSourceEntry> ENTRIES = load();
    private static final JavaProblemCorpusIndex INDEX =
            JavaProblemCorpusIndex.compileDirect(ENTRIES);

    private JavaProblemCorpus() {}

    public static List<CorpusSourceEntry> entries() {
        return INDEX.all();
    }

    /** Shared direct-classification index over the complete pinned source corpus. */
    public static JavaProblemCorpusIndex index() {
        return INDEX;
    }

    public static JavaProblemCorpusIndex.Snapshot indexSnapshot() {
        return INDEX.snapshot();
    }

    public static List<CorpusSourceEntry> byPlatform(String platform) {
        return INDEX.byPlatform(platform);
    }

    public static List<CorpusSourceEntry> byRepository(String repository) {
        return INDEX.byRepository(repository);
    }

    public static List<CorpusSourceEntry> byShape(AlgorithmShape shape) {
        return INDEX.byShape(shape);
    }

    public static List<CorpusSourceEntry> unclassified() {
        return INDEX.unclassified();
    }

    public static Map<AlgorithmShape, Long> shapeCounts() {
        return INDEX.shapeCounts();
    }

    public static Map<EnterpriseTemplateKind, Long> templateCounts() {
        return new EnumMap<>(INDEX.templateCounts());
    }

    /** Corpus-global resolved view; initialized only after cross-donor consensus is available. */
    public static List<CorpusSourceEntry> resolvedByShape(AlgorithmShape shape) {
        return resolvedIndex().byShape(shape);
    }

    /** Entries still unresolved after conservative cross-donor consensus. */
    public static List<CorpusSourceEntry> resolvedUnclassified() {
        return resolvedIndex().unclassified();
    }

    /** Shape counts after conservative cross-donor resolution. */
    public static Map<AlgorithmShape, Long> resolvedShapeCounts() {
        return resolvedIndex().shapeCounts();
    }

    public static JavaProblemCorpusIndex.Resolved resolvedIndex() {
        return ResolvedHolder.INDEX;
    }

    public static JavaProblemCorpusIndex.Snapshot resolvedIndexSnapshot() {
        return resolvedIndex().snapshot();
    }

    /**
     * Bounded pinned donor evidence for one resolved shape.
     *
     * <p>Rows are pre-sorted once. Selection first retains one available row from LeetCode,
     * HackerRank and GeeksForGeeks, then fills the remaining budget in canonical
     * platform/repository/commit/path order. These rows are evidence only.</p>
     */
    public static List<CorpusSourceEntry> resolvedEvidenceByShape(
            AlgorithmShape shape, int limit) {
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("limit");
        }
        List<CorpusSourceEntry> candidates =
                EvidenceHolder.BY_SHAPE.get(
                        java.util.Objects.requireNonNull(shape, "shape"));
        LinkedHashSet<CorpusSourceEntry> selected = new LinkedHashSet<>();
        for (String platform : List.of("LEETCODE", "HACKERRANK", "GEEKSFORGEEKS")) {
            if (selected.size() >= limit) break;
            candidates.stream()
                    .filter(entry -> entry.platform().equalsIgnoreCase(platform))
                    .findFirst()
                    .ifPresent(selected::add);
        }
        for (CorpusSourceEntry entry : candidates) {
            if (selected.size() >= limit) break;
            selected.add(entry);
        }
        return List.copyOf(selected);
    }

    private static final class EvidenceHolder {
        private static final Map<AlgorithmShape, List<CorpusSourceEntry>> BY_SHAPE =
                buildEvidenceViews();

        private static Map<AlgorithmShape, List<CorpusSourceEntry>> buildEvidenceViews() {
            EnumMap<AlgorithmShape, List<CorpusSourceEntry>> result =
                    new EnumMap<>(AlgorithmShape.class);
            Comparator<CorpusSourceEntry> order =
                    Comparator.comparing(CorpusSourceEntry::platform)
                            .thenComparing(CorpusSourceEntry::repository)
                            .thenComparing(CorpusSourceEntry::commit)
                            .thenComparing(CorpusSourceEntry::path);
            for (AlgorithmShape shape : AlgorithmShape.values()) {
                result.put(
                        shape,
                        resolvedByShape(shape).stream().sorted(order).toList());
            }
            return Collections.unmodifiableMap(result);
        }

        private EvidenceHolder() {}
    }

    private static final class ResolvedHolder {
        private static final JavaProblemCorpusIndex.Resolved INDEX =
                JavaProblemCorpus.INDEX.compileResolved(CrossDonorShapeResolver::resolve);

        private ResolvedHolder() {}
    }

    private static List<CorpusSourceEntry> load() {
        final List<CorpusSourceEntry> entries = new ArrayList<>();
        for (String resource : readLines(RESOURCE_INDEX)) {
            if (resource.isBlank() || resource.startsWith("#")) continue;
            final String path = RESOURCE_ROOT + resource.trim();
            final List<String> lines = readLines(path);
            for (int index = 1; index < lines.size(); index++) {
                final String line = lines.get(index);
                if (line.isBlank()) continue;
                final String[] parts = line.split("\t", 4);
                if (parts.length != 4) {
                    throw new ExceptionInInitializerError(
                            "invalid corpus row in " + resource + " at line " + (index + 1));
                }
                entries.add(new CorpusSourceEntry(parts[0], parts[1], parts[2], parts[3]));
            }
        }
        String additional = System.getProperty(PinnedJavaCorpusManifest.PROPERTY);
        if (additional != null && !additional.isBlank()) {
            try {
                return mergeAdditional(entries, PinnedJavaCorpusManifest.read(Path.of(additional), null));
            } catch (IOException failure) {
                throw new IllegalStateException("failed reading additional pinned corpus manifest", failure);
            }
        }
        return List.copyOf(entries);
    }

    /** Temporary snapshot-build union; all consumers still share INDEX and its canonical rows. */
    static List<CorpusSourceEntry> mergeAdditional(
            List<CorpusSourceEntry> baseline, List<String> manifest) {
        List<CorpusSourceEntry> union = new ArrayList<>(baseline);
        LinkedHashSet<CorpusSourceEntry> seen = new LinkedHashSet<>(baseline);
        for (int index = 1; index < manifest.size(); index++) {
            String[] parts = manifest.get(index).split("\\t", -1);
            if (parts.length != 4) throw new IllegalArgumentException("invalid supplemental corpus row");
            CorpusSourceEntry entry = new CorpusSourceEntry(parts[0], parts[1], parts[2], parts[3]);
            if (seen.add(entry)) union.add(entry);
        }
        return List.copyOf(union);
    }

    private static List<String> readLines(String resource) {
        try (InputStream stream = JavaProblemCorpus.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("missing resource " + resource);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().toList();
            }
        } catch (IOException failure) {
            throw new IllegalStateException("failed reading " + resource, failure);
        }
    }
}
