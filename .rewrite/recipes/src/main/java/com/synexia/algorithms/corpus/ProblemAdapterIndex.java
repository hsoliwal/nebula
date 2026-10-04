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
 * Immutable precomputed indexes over the production problem-adapter corpus.
 *
 * <p>The source {@link ProblemAdapter} instances remain authoritative. Secondary postings store
 * primitive row ordinals into one master adapter list instead of repeating adapter references.</p>
 */
public final class ProblemAdapterIndex {

    public record Snapshot(
            int adapters,
            int executable,
            int unresolved,
            int logicalProblems,
            int repositories,
            int platforms,
            long postingOrdinals,
            long postingOrdinalBytes,
            String root) {

        public Snapshot {
            if (adapters < 0
                    || executable < 0
                    || unresolved < 0
                    || logicalProblems < 0
                    || repositories < 0
                    || platforms < 0
                    || postingOrdinals < 0
                    || postingOrdinalBytes != Math.multiplyExact(postingOrdinals, Integer.BYTES)
                    || executable + unresolved != adapters) {
                throw new IllegalArgumentException("problem adapter index counts");
            }
            root = sha256(root, "root");
        }
    }

    private final List<ProblemAdapter> all;
    private final List<ProblemAdapter> executable;
    private final List<ProblemAdapter> unresolved;
    private final Map<String, Integer> rowById;
    private final Map<AlgorithmShape, List<ProblemAdapter>> byShape;
    private final Map<EnterpriseTemplateKind, List<ProblemAdapter>> byTemplate;
    private final Map<String, List<ProblemAdapter>> byProblem;
    private final Map<String, List<ProblemAdapter>> byRepository;
    private final Map<String, List<ProblemAdapter>> byPlatform;
    private final Map<CompetitiveProblemCategory, List<ProblemAdapter>> byCategory;
    private final Map<String, List<ProblemAdapter>> byPlatformCategory;
    private final Snapshot snapshot;

    private ProblemAdapterIndex(
            List<ProblemAdapter> all,
            IntAccumulator executable,
            IntAccumulator unresolved,
            Map<String, Integer> rowById,
            Map<AlgorithmShape, IntAccumulator> byShape,
            Map<EnterpriseTemplateKind, IntAccumulator> byTemplate,
            Map<String, IntAccumulator> byProblem,
            Map<String, IntAccumulator> byRepository,
            Map<String, IntAccumulator> byPlatform,
            Map<CompetitiveProblemCategory, IntAccumulator> byCategory,
            Map<String, IntAccumulator> byPlatformCategory,
            String root) {
        this.all = List.copyOf(all);
        this.executable = view(this.all, executable);
        this.unresolved = view(this.all, unresolved);
        this.rowById = Map.copyOf(rowById);
        this.byShape = freezeEnumViews(AlgorithmShape.class, this.all, byShape);
        this.byTemplate =
                freezeEnumViews(EnterpriseTemplateKind.class, this.all, byTemplate);
        this.byProblem = freezeStringViews(this.all, byProblem);
        this.byRepository = freezeStringViews(this.all, byRepository);
        this.byPlatform = freezeStringViews(this.all, byPlatform);
        this.byCategory = freezeEnumViews(CompetitiveProblemCategory.class, this.all, byCategory);
        this.byPlatformCategory = freezeStringViews(this.all, byPlatformCategory);

        long ordinalCount = Math.addExact(executable.size(), unresolved.size());
        ordinalCount = Math.addExact(ordinalCount, postings(byShape));
        ordinalCount = Math.addExact(ordinalCount, postings(byTemplate));
        ordinalCount = Math.addExact(ordinalCount, postings(byProblem));
        ordinalCount = Math.addExact(ordinalCount, postings(byRepository));
        ordinalCount = Math.addExact(ordinalCount, postings(byPlatform));
        ordinalCount = Math.addExact(ordinalCount, postings(byCategory));
        final long postingOrdinals = Math.addExact(ordinalCount, postings(byPlatformCategory));
        this.snapshot = new Snapshot(
                this.all.size(),
                executable.size(),
                unresolved.size(),
                this.byProblem.size(),
                this.byRepository.size(),
                this.byPlatform.size(),
                postingOrdinals,
                Math.multiplyExact(postingOrdinals, Integer.BYTES),
                root);
    }

    public static ProblemAdapterIndex compile(List<ProblemAdapter> adapters) {
        Objects.requireNonNull(adapters, "adapters");
        final List<ProblemAdapter> stable = List.copyOf(adapters);

        final LinkedHashMap<String, Integer> rowById = new LinkedHashMap<>();
        final EnumMap<AlgorithmShape, IntAccumulator> byShape =
                emptyEnumBuckets(AlgorithmShape.class);
        final EnumMap<EnterpriseTemplateKind, IntAccumulator> byTemplate =
                emptyEnumBuckets(EnterpriseTemplateKind.class);
        final LinkedHashMap<String, IntAccumulator> byProblem = new LinkedHashMap<>();
        final LinkedHashMap<String, IntAccumulator> byRepository = new LinkedHashMap<>();
        final LinkedHashMap<String, IntAccumulator> byPlatform = new LinkedHashMap<>();
        final EnumMap<CompetitiveProblemCategory, IntAccumulator> byCategory =
                emptyEnumBuckets(CompetitiveProblemCategory.class);
        final LinkedHashMap<String, IntAccumulator> byPlatformCategory = new LinkedHashMap<>();
        final IntAccumulator executable = new IntAccumulator();
        final IntAccumulator unresolved = new IntAccumulator();

        for (int row = 0; row < stable.size(); row++) {
            final ProblemAdapter adapter = Objects.requireNonNull(stable.get(row), "adapter");
            if (rowById.putIfAbsent(adapter.id(), row) != null) {
                throw new IllegalArgumentException("duplicate problem adapter id: " + adapter.id());
            }

            if (adapter.executable()) {
                executable.add(row);
            } else {
                unresolved.add(row);
            }

            String platform = platformKey(adapter.source().platform());
            if (adapter.classification().classified()) {
                AlgorithmShape shape = adapter.classification().shape();
                byShape.get(shape).add(row);
                CompetitiveProblemCategory category = CompetitiveProblemCategory.fromShape(shape);
                byCategory.get(category).add(row);
                append(byPlatformCategory, platform + '\t' + category.name(), row);
            }
            byTemplate.get(adapter.templateStyle().kind()).add(row);
            append(byProblem, problemKey(adapter), row);
            append(byRepository, adapter.source().repository(), row);
            append(byPlatform, platform, row);
        }

        return new ProblemAdapterIndex(
                stable,
                executable,
                unresolved,
                rowById,
                byShape,
                byTemplate,
                byProblem,
                byRepository,
                byPlatform,
                byCategory,
                byPlatformCategory,
                digest(stable));
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public List<ProblemAdapter> all() {
        return all;
    }

    public List<ProblemAdapter> executable() {
        return executable;
    }

    public List<ProblemAdapter> unresolved() {
        return unresolved;
    }

    public ProblemAdapter require(String id) {
        final Integer row = rowById.get(Objects.requireNonNull(id, "id"));
        if (row == null) {
            throw new IllegalArgumentException("unknown problem adapter: " + id);
        }
        return all.get(row);
    }

    public List<ProblemAdapter> byShape(AlgorithmShape shape) {
        return byShape.get(Objects.requireNonNull(shape, "shape"));
    }

    public List<ProblemAdapter> byTemplate(EnterpriseTemplateKind kind) {
        return byTemplate.get(Objects.requireNonNull(kind, "kind"));
    }

    public Map<String, List<ProblemAdapter>> implementationsByProblem() {
        return byProblem;
    }

    public List<ProblemAdapter> byProblem(String problemKey) {
        return byProblem.getOrDefault(required(problemKey, "problemKey"), List.of());
    }

    public List<ProblemAdapter> byRepository(String repository) {
        return byRepository.getOrDefault(required(repository, "repository"), List.of());
    }

    public List<ProblemAdapter> byPlatform(String platform) {
        return byPlatform.getOrDefault(platformKey(platform), List.of());
    }

    /** Constant-time lookup of classified adapters; unresolved rows have no category posting. */
    public List<ProblemAdapter> byCategory(CompetitiveProblemCategory category) {
        return byCategory.get(Objects.requireNonNull(category, "category"));
    }

    /** Constant-time lookup of one site's classified adapters in deterministic corpus order. */
    public List<ProblemAdapter> byPlatformCategory(
            String platform, CompetitiveProblemCategory category) {
        return byPlatformCategory.getOrDefault(platformKey(platform) + '\t'
                + Objects.requireNonNull(category, "category").name(), List.of());
    }

    static String problemKey(ProblemAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        if (adapter.source().platform().equalsIgnoreCase("LEETCODE")) {
            final java.util.OptionalInt number =
                    CrossDonorShapeResolver.problemNumber(adapter.source());
            if (number.isPresent()) return "LEETCODE:" + number.getAsInt();
        }
        return adapter.source().platform() + ":" + normalize(adapter.problemName());
    }

    private static String digest(List<ProblemAdapter> adapters) {
        final ArrayList<ProblemAdapter> ordered = new ArrayList<>(adapters);
        ordered.sort(Comparator.comparing(ProblemAdapter::id));
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, "SYNEXIA_PROBLEM_ADAPTER_INDEX_V1");
            update(digest, Integer.toString(ordered.size()));
            for (ProblemAdapter adapter : ordered) {
                final ProblemShape classification = adapter.classification();
                update(digest, adapter.id());
                update(digest, adapter.source().platform());
                update(digest, adapter.source().repository());
                update(digest, adapter.source().commit());
                update(digest, adapter.source().path());
                update(digest, problemKey(adapter));
                update(digest, classification.shape() == null ? "" : classification.shape().name());
                update(digest, classification.template().name());
                update(digest, classification.confidence().name());
                update(digest, classification.rationale());
                update(digest, adapter.templateStyle().kind().name());
                update(digest, String.join(",", adapter.templateStyle().businessVerbs()));
                update(digest, adapter.templateStyle().templatePattern());
                update(digest, Boolean.toString(adapter.executable()));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static <E extends Enum<E>> EnumMap<E, IntAccumulator> emptyEnumBuckets(
            Class<E> type) {
        final EnumMap<E, IntAccumulator> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            result.put(value, new IntAccumulator());
        }
        return result;
    }

    private static <E extends Enum<E>> Map<E, List<ProblemAdapter>> freezeEnumViews(
            Class<E> type,
            List<ProblemAdapter> rows,
            Map<E, IntAccumulator> source) {
        final EnumMap<E, List<ProblemAdapter>> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            result.put(value, view(rows, source.get(value)));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, List<ProblemAdapter>> freezeStringViews(
            List<ProblemAdapter> rows,
            Map<String, IntAccumulator> source) {
        final LinkedHashMap<String, List<ProblemAdapter>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, view(rows, value)));
        return Collections.unmodifiableMap(result);
    }

    private static List<ProblemAdapter> view(
            List<ProblemAdapter> rows,
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

    private static String normalize(String value) {
        final StringBuilder out = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            final char item = Character.toLowerCase(value.charAt(index));
            if (Character.isLetterOrDigit(item)) out.append(item);
        }
        return out.toString();
    }

    private static String required(String value, String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String sha256(String value, String field) {
        final String checked = required(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
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
            if (value < 0) throw new IllegalArgumentException("negative adapter row");
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
            extends AbstractList<ProblemAdapter>
            implements RandomAccess {
        private final List<ProblemAdapter> rows;
        private final int[] ordinals;

        OrdinalView(
                List<ProblemAdapter> rows,
                int[] ordinals) {
            this.rows = Objects.requireNonNull(rows, "rows");
            this.ordinals = Objects.requireNonNull(ordinals, "ordinals");
            for (int ordinal : ordinals) {
                if (ordinal < 0 || ordinal >= rows.size()) {
                    throw new IllegalArgumentException("adapter row ordinal");
                }
            }
        }

        @Override
        public ProblemAdapter get(int index) {
            return rows.get(ordinals[index]);
        }

        @Override
        public int size() {
            return ordinals.length;
        }
    }
}
