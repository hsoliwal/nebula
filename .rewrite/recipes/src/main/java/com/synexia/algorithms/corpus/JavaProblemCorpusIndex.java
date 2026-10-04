// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Immutable primitive-row indexes over the pinned Java source corpus.
 *
 * <p>Direct classification and resolved cross-donor classification are compiled separately so the
 * resolved projection can initialize lazily after {@link CrossDonorShapeResolver} completes.</p>
 */
public final class JavaProblemCorpusIndex {

    public record Snapshot(
            int rows,
            int classified,
            int unclassified,
            int repositories,
            int platforms,
            long postingOrdinals,
            long postingOrdinalBytes,
            String root) {

        public Snapshot {
            if (rows < 0
                    || classified < 0
                    || unclassified < 0
                    || repositories < 0
                    || platforms < 0
                    || classified + unclassified != rows
                    || postingOrdinals < 0
                    || postingOrdinalBytes != Math.multiplyExact(postingOrdinals, Integer.BYTES)) {
                throw new IllegalArgumentException("java problem corpus index counts");
            }
            root = sha256(root, "root");
        }
    }

    public static final class Resolved {
        private final List<CorpusSourceEntry> rows;
        private final Map<AlgorithmShape, List<CorpusSourceEntry>> byShape;
        private final List<CorpusSourceEntry> unclassified;
        private final Map<AlgorithmShape, Long> shapeCounts;
        private final Snapshot snapshot;

        private Resolved(
                List<CorpusSourceEntry> rows,
                Map<AlgorithmShape, IntAccumulator> byShape,
                IntAccumulator unclassified,
                String root,
                int repositories,
                int platforms) {
            this.rows = Objects.requireNonNull(rows, "rows");
            this.byShape = freezeEnumViews(AlgorithmShape.class, rows, byShape);
            this.unclassified = view(rows, unclassified);
            this.shapeCounts = counts(AlgorithmShape.class, byShape);

            final long shapePostings = postings(byShape);
            final long postingOrdinals = Math.addExact(shapePostings, unclassified.size());
            this.snapshot = new Snapshot(
                    rows.size(),
                    Math.toIntExact(shapePostings),
                    unclassified.size(),
                    repositories,
                    platforms,
                    postingOrdinals,
                    Math.multiplyExact(postingOrdinals, Integer.BYTES),
                    root);
        }

        public List<CorpusSourceEntry> byShape(AlgorithmShape shape) {
            return byShape.get(Objects.requireNonNull(shape, "shape"));
        }

        public List<CorpusSourceEntry> unclassified() {
            return unclassified;
        }

        public Map<AlgorithmShape, Long> shapeCounts() {
            return shapeCounts;
        }

        public Snapshot snapshot() {
            return snapshot;
        }
    }

    private final List<CorpusSourceEntry> rows;
    private final Map<String, List<CorpusSourceEntry>> byPlatform;
    private final Map<String, List<CorpusSourceEntry>> byRepository;
    private final Map<AlgorithmShape, List<CorpusSourceEntry>> byShape;
    private final List<CorpusSourceEntry> unclassified;
    private final Map<AlgorithmShape, Long> shapeCounts;
    private final Map<EnterpriseTemplateKind, Long> templateCounts;
    private final Snapshot snapshot;

    private JavaProblemCorpusIndex(
            List<CorpusSourceEntry> rows,
            Map<String, IntAccumulator> byPlatform,
            Map<String, IntAccumulator> byRepository,
            Map<AlgorithmShape, IntAccumulator> byShape,
            Map<EnterpriseTemplateKind, Long> templateCounts,
            IntAccumulator unclassified,
            String root) {
        this.rows = List.copyOf(rows);
        this.byPlatform = freezeStringViews(this.rows, byPlatform);
        this.byRepository = freezeStringViews(this.rows, byRepository);
        this.byShape = freezeEnumViews(AlgorithmShape.class, this.rows, byShape);
        this.unclassified = view(this.rows, unclassified);
        this.shapeCounts = counts(AlgorithmShape.class, byShape);

        final EnumMap<EnterpriseTemplateKind, Long> stableTemplates =
                new EnumMap<>(EnterpriseTemplateKind.class);
        for (EnterpriseTemplateKind value : EnterpriseTemplateKind.values()) {
            final long count = templateCounts.getOrDefault(value, 0L);
            if (count > 0L) stableTemplates.put(value, count);
        }
        this.templateCounts = Collections.unmodifiableMap(stableTemplates);

        final long postingOrdinals = Math.addExact(
                Math.addExact(postings(byPlatform), postings(byRepository)),
                Math.addExact(postings(byShape), unclassified.size()));
        this.snapshot = new Snapshot(
                this.rows.size(),
                Math.toIntExact(postings(byShape)),
                unclassified.size(),
                this.byRepository.size(),
                this.byPlatform.size(),
                postingOrdinals,
                Math.multiplyExact(postingOrdinals, Integer.BYTES),
                root);
    }

    public static JavaProblemCorpusIndex compileDirect(List<CorpusSourceEntry> sources) {
        Objects.requireNonNull(sources, "sources");
        final List<CorpusSourceEntry> stable = List.copyOf(sources);
        final LinkedHashMap<String, IntAccumulator> byPlatform = new LinkedHashMap<>();
        final LinkedHashMap<String, IntAccumulator> byRepository = new LinkedHashMap<>();
        final EnumMap<AlgorithmShape, IntAccumulator> byShape =
                emptyEnumBuckets(AlgorithmShape.class);
        final EnumMap<EnterpriseTemplateKind, Long> templateCounts =
                new EnumMap<>(EnterpriseTemplateKind.class);
        final IntAccumulator unclassified = new IntAccumulator();
        final ProblemShape[] classifications = new ProblemShape[stable.size()];

        for (int row = 0; row < stable.size(); row++) {
            final CorpusSourceEntry source = Objects.requireNonNull(stable.get(row), "source");
            append(byPlatform, platformKey(source.platform()), row);
            append(byRepository, source.repository(), row);

            final ProblemShape shape = source.classification();
            classifications[row] = shape;
            templateCounts.merge(shape.template(), 1L, Long::sum);
            if (shape.classified()) {
                byShape.get(shape.shape()).add(row);
            } else {
                unclassified.add(row);
            }
        }

        return new JavaProblemCorpusIndex(
                stable,
                byPlatform,
                byRepository,
                byShape,
                templateCounts,
                unclassified,
                digest("SYNEXIA_JAVA_PROBLEM_CORPUS_INDEX_V1", stable, classifications));
    }

    public Resolved compileResolved(
            java.util.function.Function<CorpusSourceEntry, ProblemShape> resolver) {
        Objects.requireNonNull(resolver, "resolver");
        final EnumMap<AlgorithmShape, IntAccumulator> byShape =
                emptyEnumBuckets(AlgorithmShape.class);
        final IntAccumulator unclassified = new IntAccumulator();
        final ProblemShape[] classifications = new ProblemShape[rows.size()];

        for (int row = 0; row < rows.size(); row++) {
            final ProblemShape shape =
                    Objects.requireNonNull(resolver.apply(rows.get(row)), "resolved shape");
            classifications[row] = shape;
            if (shape.classified()) {
                byShape.get(shape.shape()).add(row);
            } else {
                unclassified.add(row);
            }
        }

        final String root = digest(
                "SYNEXIA_JAVA_PROBLEM_CORPUS_RESOLVED_INDEX_V1",
                rows,
                classifications);
        return new Resolved(
                rows,
                byShape,
                unclassified,
                root,
                snapshot.repositories(),
                snapshot.platforms());
    }

    public List<CorpusSourceEntry> all() {
        return rows;
    }

    public List<CorpusSourceEntry> byPlatform(String platform) {
        return byPlatform.getOrDefault(platformKey(platform), List.of());
    }

    public List<CorpusSourceEntry> byRepository(String repository) {
        return byRepository.getOrDefault(required(repository, "repository"), List.of());
    }

    public List<CorpusSourceEntry> byShape(AlgorithmShape shape) {
        return byShape.get(Objects.requireNonNull(shape, "shape"));
    }

    public List<CorpusSourceEntry> unclassified() {
        return unclassified;
    }

    public Map<AlgorithmShape, Long> shapeCounts() {
        return shapeCounts;
    }

    public Map<EnterpriseTemplateKind, Long> templateCounts() {
        return templateCounts;
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    private static String digest(
            String domain,
            List<CorpusSourceEntry> rows,
            ProblemShape[] classifications) {
        if (rows.size() != classifications.length) {
            throw new IllegalArgumentException("classification row count");
        }
        final ArrayList<Integer> order = new ArrayList<>(rows.size());
        for (int row = 0; row < rows.size(); row++) order.add(row);
        order.sort(Comparator.comparing((Integer row) -> rows.get(row).platform())
                .thenComparing(row -> rows.get(row).repository())
                .thenComparing(row -> rows.get(row).commit())
                .thenComparing(row -> rows.get(row).path()));
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, domain);
            update(digest, Integer.toString(rows.size()));
            for (int row : order) {
                final CorpusSourceEntry source = rows.get(row);
                final ProblemShape shape =
                        Objects.requireNonNull(classifications[row], "classification");
                update(digest, source.platform());
                update(digest, source.repository());
                update(digest, source.commit());
                update(digest, source.path());
                update(digest, source.problemName());
                update(digest, shape.shape() == null ? "" : shape.shape().name());
                update(digest, shape.template().name());
                update(digest, shape.confidence().name());
                update(digest, shape.rationale());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static <E extends Enum<E>> EnumMap<E, IntAccumulator> emptyEnumBuckets(
            Class<E> type) {
        final EnumMap<E, IntAccumulator> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) result.put(value, new IntAccumulator());
        return result;
    }

    private static <E extends Enum<E>> Map<E, List<CorpusSourceEntry>> freezeEnumViews(
            Class<E> type,
            List<CorpusSourceEntry> rows,
            Map<E, IntAccumulator> source) {
        final EnumMap<E, List<CorpusSourceEntry>> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            result.put(value, view(rows, source.get(value)));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, List<CorpusSourceEntry>> freezeStringViews(
            List<CorpusSourceEntry> rows,
            Map<String, IntAccumulator> source) {
        final LinkedHashMap<String, List<CorpusSourceEntry>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, view(rows, value)));
        return Collections.unmodifiableMap(result);
    }

    private static <E extends Enum<E>> Map<E, Long> counts(
            Class<E> type,
            Map<E, IntAccumulator> source) {
        final EnumMap<E, Long> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            final long count = source.get(value).size();
            if (count > 0L) result.put(value, count);
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<CorpusSourceEntry> view(
            List<CorpusSourceEntry> rows,
            IntAccumulator ordinals) {
        if (ordinals == null || ordinals.size() == 0) return List.of();
        return new OrdinalView(rows, ordinals.toArray());
    }

    private static void append(
            Map<String, IntAccumulator> target,
            String key,
            int row) {
        target.computeIfAbsent(key, ignored -> new IntAccumulator()).add(row);
    }

    private static long postings(Map<?, IntAccumulator> values) {
        long result = 0L;
        for (IntAccumulator value : values.values()) {
            result = Math.addExact(result, value.size());
        }
        return result;
    }

    private static String platformKey(String value) {
        return required(value, "platform").toUpperCase(Locale.ROOT);
    }

    private static String required(String value, String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String sha256(String value, String field) {
        final String checked = required(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static void update(MessageDigest digest, String value) {
        final byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static final class IntAccumulator {
        private int[] values = new int[8];
        private int size;

        void add(int value) {
            if (value < 0) throw new IllegalArgumentException("negative corpus row");
            if (size == values.length) {
                values = java.util.Arrays.copyOf(values, Math.multiplyExact(values.length, 2));
            }
            values[size++] = value;
        }

        int size() {
            return size;
        }

        int[] toArray() {
            return java.util.Arrays.copyOf(values, size);
        }
    }

    private static final class OrdinalView
            extends AbstractList<CorpusSourceEntry>
            implements RandomAccess {
        private final List<CorpusSourceEntry> rows;
        private final int[] ordinals;

        OrdinalView(List<CorpusSourceEntry> rows, int[] ordinals) {
            this.rows = Objects.requireNonNull(rows, "rows");
            this.ordinals = Objects.requireNonNull(ordinals, "ordinals");
            for (int ordinal : ordinals) {
                if (ordinal < 0 || ordinal >= rows.size()) {
                    throw new IllegalArgumentException("corpus row ordinal");
                }
            }
        }

        @Override
        public CorpusSourceEntry get(int index) {
            return rows.get(ordinals[index]);
        }

        @Override
        public int size() {
            return ordinals.length;
        }
    }
}
