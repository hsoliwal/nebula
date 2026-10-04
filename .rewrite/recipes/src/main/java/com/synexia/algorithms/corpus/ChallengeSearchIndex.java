// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.corpus.ChallengePostingIntersection.Prepared;
import com.synexia.algorithms.shapes.AlgorithmShape;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable precomputed category/search index over the canonical logical challenge catalogue.
 *
 * <p>Rows are {@link ChallengeProblem} identities. Implementation repositories remain provenance;
 * query results do not grant source, execution, rewrite or promotion authority.</p>
 */
public final class ChallengeSearchIndex {

    public record Query(
            Set<ChallengePlatform> platforms,
            Set<AlgorithmShape> shapes,
            Set<EnterpriseTemplateKind> templates,
            Set<ChallengeProblem.Status> statuses,
            List<String> terms,
            List<String> repositories,
            int limit) {

        public Query {
            platforms = enumSet(platforms, ChallengePlatform.class);
            shapes = enumSet(shapes, AlgorithmShape.class);
            templates = enumSet(templates, EnterpriseTemplateKind.class);
            statuses = enumSet(statuses, ChallengeProblem.Status.class);
            terms = normalizedTerms(terms);
            repositories = normalizedRepositories(repositories);
            if (limit < 1 || limit > 100_000) throw new IllegalArgumentException("limit");
        }

        public static Query all(int limit) {
            return new Query(
                    Set.of(), Set.of(), Set.of(), Set.of(), List.of(), List.of(), limit);
        }
    }

    public record Hit(
            ChallengeProblem problem,
            List<String> donorRepositories,
            int sourceImplementations) {
        public Hit {
            problem = Objects.requireNonNull(problem, "problem");
            donorRepositories = sortedStrings(donorRepositories, "donorRepositories");
            if (sourceImplementations != problem.implementationCount()) {
                throw new IllegalArgumentException("sourceImplementations");
            }
        }
    }

    public record Evaluation(
            Query query,
            List<Hit> hits,
            int sourceImplementations,
            int executable,
            int unresolved,
            int conflicts,
            Map<ChallengePlatform, Integer> platforms,
            Map<AlgorithmShape, Integer> shapes,
            List<String> donorRepositories,
            String root) {

        public Evaluation {
            query = Objects.requireNonNull(query, "query");
            hits = List.copyOf(Objects.requireNonNull(hits, "hits"));
            if (sourceImplementations < 0
                    || executable < 0
                    || unresolved < 0
                    || conflicts < 0
                    || executable + unresolved + conflicts != hits.size()) {
                throw new IllegalArgumentException("evaluation counts");
            }
            platforms = immutableEnumCounts(platforms, ChallengePlatform.class);
            shapes = immutableEnumCounts(shapes, AlgorithmShape.class);
            donorRepositories = sortedStrings(donorRepositories, "donorRepositories");
            String expected = evaluationRoot(
                    query,
                    hits,
                    sourceImplementations,
                    executable,
                    unresolved,
                    conflicts,
                    platforms,
                    shapes,
                    donorRepositories);
            root = root == null || root.isBlank() ? expected : sha64(root, "root");
            if (!expected.equals(root)) throw new IllegalArgumentException("evaluation root mismatch");
        }

        public boolean substitutionAuthority() {
            return false;
        }
    }

    public record Snapshot(
            int logicalProblems,
            int tokens,
            int repositories,
            long postingOrdinals,
            long postingBytes,
            String root) {
        public Snapshot {
            if (logicalProblems < 0
                    || tokens < 0
                    || repositories < 0
                    || postingOrdinals < 0
                    || postingBytes != Math.multiplyExact(postingOrdinals, Integer.BYTES)) {
                throw new IllegalArgumentException("snapshot counts");
            }
            root = sha64(root, "root");
        }
    }

    private static final int[] EMPTY = new int[0];

    private static final ChallengeSearchIndex CANONICAL =
            compile(ChallengeCatalog.all());

    private final List<ChallengeProblem> rows;
    private final Map<ChallengePlatform, Prepared> byPlatform;
    private final Map<AlgorithmShape, Prepared> byShape;
    private final Map<EnterpriseTemplateKind, Prepared> byTemplate;
    private final Map<ChallengeProblem.Status, Prepared> byStatus;
    private final Map<String, Prepared> byToken;
    private final Map<String, Prepared> byRepository;
    private final Snapshot snapshot;
    private final Prepared emptyPosting;
    private final List<List<String>> repositoriesByRow;

    private ChallengeSearchIndex(
            List<ChallengeProblem> rows,
            Map<ChallengePlatform, int[]> byPlatform,
            Map<AlgorithmShape, int[]> byShape,
            Map<EnterpriseTemplateKind, int[]> byTemplate,
            Map<ChallengeProblem.Status, int[]> byStatus,
            Map<String, int[]> byToken,
            Map<String, int[]> byRepository,
            String root) {
        this.rows = List.copyOf(rows);
        this.emptyPosting = ChallengePostingIntersection.snapshotForIndex(EMPTY);
        this.repositoriesByRow = this.rows.stream().map(ChallengeSearchIndex::donorRepositories).toList();
        this.byPlatform = freezeEnumArrays(byPlatform, ChallengePlatform.class);
        this.byShape = freezeEnumArrays(byShape, AlgorithmShape.class);
        this.byTemplate = freezeEnumArrays(byTemplate, EnterpriseTemplateKind.class);
        this.byStatus = freezeEnumArrays(byStatus, ChallengeProblem.Status.class);
        this.byToken = freezeStringArrays(byToken);
        this.byRepository = freezeStringArrays(byRepository);

        long postings = 0L;
        postings = Math.addExact(postings, postingCount(this.byPlatform));
        postings = Math.addExact(postings, postingCount(this.byShape));
        postings = Math.addExact(postings, postingCount(this.byTemplate));
        postings = Math.addExact(postings, postingCount(this.byStatus));
        postings = Math.addExact(postings, postingCount(this.byToken));
        postings = Math.addExact(postings, postingCount(this.byRepository));
        this.snapshot = new Snapshot(
                rows.size(),
                this.byToken.size(),
                this.byRepository.size(),
                postings,
                Math.multiplyExact(postings, Integer.BYTES),
                root);
    }

    public static ChallengeSearchIndex canonical() {
        return CANONICAL;
    }

    public static ChallengeSearchIndex compile(List<ChallengeProblem> problems) {
        ArrayList<ChallengeProblem> stable =
                new ArrayList<>(Objects.requireNonNull(problems, "problems"));
        stable.sort(Comparator.comparing(problem -> problem.id().stableId()));
        for (int index = 1; index < stable.size(); index++) {
            if (stable.get(index - 1).id().stableId().equals(stable.get(index).id().stableId())) {
                throw new IllegalArgumentException(
                        "duplicate challenge id: " + stable.get(index).id().stableId());
            }
        }

        EnumMap<ChallengePlatform, IntAccumulator> platforms =
                enumAccumulators(ChallengePlatform.class);
        EnumMap<AlgorithmShape, IntAccumulator> shapes =
                enumAccumulators(AlgorithmShape.class);
        EnumMap<EnterpriseTemplateKind, IntAccumulator> templates =
                enumAccumulators(EnterpriseTemplateKind.class);
        EnumMap<ChallengeProblem.Status, IntAccumulator> statuses =
                enumAccumulators(ChallengeProblem.Status.class);
        TreeMap<String, IntAccumulator> tokens = new TreeMap<>();
        TreeMap<String, IntAccumulator> repositories = new TreeMap<>();

        for (int row = 0; row < stable.size(); row++) {
            final int ordinal = row;
            ChallengeProblem problem = stable.get(row);
            platforms.get(problem.id().platform()).add(row);
            statuses.get(problem.status()).add(row);
            problem.shape().ifPresent(shape -> shapes.get(shape).add(ordinal));
            problem.templateStyle().ifPresent(style -> templates.get(style.kind()).add(ordinal));

            for (String token : problemTokens(problem)) append(tokens, token, row);
            for (String repository : donorRepositories(problem)) {
                append(repositories, repositoryKey(repository), row);
            }
        }

        return new ChallengeSearchIndex(
                stable,
                freezeAccumulators(platforms),
                freezeAccumulators(shapes),
                freezeAccumulators(templates),
                freezeAccumulators(statuses),
                freezeAccumulators(tokens),
                freezeAccumulators(repositories),
                indexRoot(stable));
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public List<ChallengeProblem> all() {
        return rows;
    }

    /** Evaluate selected posting clauses, never a freshly allocated corpus-sized universe. */
    public List<Hit> search(Query query) {
        Query checked = Objects.requireNonNull(query, "query");
        ArrayList<Prepared> clauses = new ArrayList<>();
        ArrayList<List<Prepared>> anyOf = new ArrayList<>();
        if (!checked.platforms().isEmpty()) {
            addFacet(checked.platforms().stream().map(byPlatform::get).filter(Objects::nonNull).toList(),
                    clauses, anyOf);
        }
        if (!checked.shapes().isEmpty()) {
            addFacet(checked.shapes().stream().map(byShape::get).filter(Objects::nonNull).toList(),
                    clauses, anyOf);
        }
        if (!checked.templates().isEmpty()) {
            addFacet(checked.templates().stream().map(byTemplate::get).filter(Objects::nonNull).toList(),
                    clauses, anyOf);
        }
        if (!checked.statuses().isEmpty()) {
            addFacet(checked.statuses().stream().map(byStatus::get).filter(Objects::nonNull).toList(),
                    clauses, anyOf);
        }
        for (String term : checked.terms()) clauses.add(byToken.getOrDefault(term, emptyPosting));
        if (!checked.repositories().isEmpty()) {
            addFacet(checked.repositories().stream().map(value -> byRepository.get(repositoryKey(value)))
                    .filter(Objects::nonNull).toList(), clauses, anyOf);
        }
        if (clauses.isEmpty() && anyOf.isEmpty()) {
            int count = Math.min(rows.size(), checked.limit());
            ArrayList<Hit> hits = new ArrayList<>(count);
            for (int ordinal = 0; ordinal < count; ordinal++) hits.add(hit(ordinal));
            return List.copyOf(hits);
        }
        int[] ordinals = ChallengePostingQuery.intersectGrouped(
                clauses, anyOf, checked.limit(), rows.size(), null);
        ArrayList<Hit> hits = new ArrayList<>(ordinals.length);
        for (int ordinal : ordinals) hits.add(hit(ordinal));
        return List.copyOf(hits);
    }

    private static void addFacet(List<Prepared> selected, List<Prepared> clauses,
            List<List<Prepared>> anyOf) {
        if (selected.size() == 1) clauses.add(selected.getFirst());
        else anyOf.add(selected);
    }
    private Hit hit(int ordinal) {
        ChallengeProblem problem = rows.get(ordinal);
        return new Hit(problem, repositoriesByRow.get(ordinal), problem.implementationCount());
    }

    public Evaluation evaluate(Query query) {
        Query checked = Objects.requireNonNull(query, "query");
        List<Hit> hits = search(checked);
        int sourceImplementations = 0;
        int executable = 0;
        int unresolved = 0;
        int conflicts = 0;
        EnumMap<ChallengePlatform, Integer> platforms =
                new EnumMap<>(ChallengePlatform.class);
        EnumMap<AlgorithmShape, Integer> shapes = new EnumMap<>(AlgorithmShape.class);
        TreeSet<String> repositories = new TreeSet<>();

        for (Hit hit : hits) {
            ChallengeProblem problem = hit.problem();
            sourceImplementations =
                    Math.addExact(sourceImplementations, hit.sourceImplementations());
            platforms.merge(problem.id().platform(), 1, Math::addExact);
            problem.shape().ifPresent(shape -> shapes.merge(shape, 1, Math::addExact));
            repositories.addAll(hit.donorRepositories());
            switch (problem.status()) {
                case EXECUTABLE -> executable++;
                case UNCLASSIFIED -> unresolved++;
                case CLASSIFICATION_CONFLICT -> conflicts++;
            }
        }
        return new Evaluation(
                checked,
                hits,
                sourceImplementations,
                executable,
                unresolved,
                conflicts,
                platforms,
                shapes,
                List.copyOf(repositories),
                "");
    }

    /** Search an official platform category or alias using prepared executable postings. */
    public List<Hit> searchCategory(ChallengePlatform platform, String category, int limit) {
        Objects.requireNonNull(platform, "platform");
        if (limit < 1 || limit > 100_000) throw new IllegalArgumentException("limit");
        ChallengeCategoryCatalog.Category matched =
                ChallengeCategoryCatalog.find(platform, category).orElse(null);
        if (matched == null || matched.shapes().isEmpty()) return List.of();
        return search(new Query(
                Set.of(platform),
                Set.copyOf(matched.shapes()),
                Set.of(),
                Set.of(ChallengeProblem.Status.EXECUTABLE),
                List.of(),
                List.of(),
                limit));
    }

    /**
     * Search one category requiring one exact mechanical shape. A category which does not map to
     * the requested shape yields no hits; it never broadens the candidate set.
     */
    public List<Hit> searchCategory(
            ChallengePlatform platform, String category, AlgorithmShape requiredShape, int limit) {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(requiredShape, "requiredShape");
        if (limit < 1 || limit > 100_000) throw new IllegalArgumentException("limit");
        ChallengeCategoryCatalog.Category matched =
                ChallengeCategoryCatalog.find(platform, category).orElse(null);
        if (matched == null || !matched.mapsTo(requiredShape)) return List.of();
        return search(new Query(
                Set.of(platform),
                Set.of(requiredShape),
                Set.of(),
                Set.of(ChallengeProblem.Status.EXECUTABLE),
                List.of(),
                List.of(),
                limit));
    }

    public List<ChallengeProblem> byPlatform(ChallengePlatform platform) {
        return project(byPlatform.get(Objects.requireNonNull(platform, "platform")));
    }

    public List<ChallengeProblem> byShape(AlgorithmShape shape) {
        return project(byShape.get(Objects.requireNonNull(shape, "shape")));
    }

    public List<ChallengeProblem> byTemplate(EnterpriseTemplateKind template) {
        return project(byTemplate.get(Objects.requireNonNull(template, "template")));
    }

    public List<ChallengeProblem> byStatus(ChallengeProblem.Status status) {
        return project(byStatus.get(Objects.requireNonNull(status, "status")));
    }

    public List<ChallengeProblem> byRepository(String repository) {
        return project(byRepository.get(repositoryKey(repository)));
    }

    private List<ChallengeProblem> project(Prepared ordinals) {
        if (ordinals == null || ordinals.size() == 0) return List.of();
        ArrayList<ChallengeProblem> result = new ArrayList<>(ordinals.size());
        for (int position = 0; position < ordinals.size(); position++) {
            result.add(rows.get(ordinals.valueAt(position)));
        }
        return List.copyOf(result);
    }

    private Prepared unionStrings(List<String> values, Map<String, Prepared> postings) {
        ArrayList<Prepared> selected = new ArrayList<>(values.size());
        for (String value : values) {
            Prepared posting = postings.get(repositoryKey(value));
            if (posting != null) selected.add(posting);
        }
        return ChallengePostingQuery.union(selected, rows.size(), null);
    }

    private <E extends Enum<E>> Prepared unionEnum(Set<E> values, Map<E, Prepared> postings) {
        ArrayList<Prepared> selected = new ArrayList<>(values.size());
        for (E value : values) {
            Prepared posting = postings.get(value);
            if (posting != null) selected.add(posting);
        }
        return ChallengePostingQuery.union(selected, rows.size(), null);
    }

    private static Set<String> problemTokens(ChallengeProblem problem) {
        TreeSet<String> result = new TreeSet<>();
        tokenize(problem.title(), result);
        tokenize(problem.id().stableId(), result);
        tokenize(problem.id().externalId(), result);
        tokenize(problem.id().slug(), result);
        return result;
    }

    private static void tokenize(String value, Set<String> output) {
        StringBuilder token = new StringBuilder();
        String text = Objects.toString(value, "");
        for (int index = 0; index < text.length(); index++) {
            char current = Character.toLowerCase(text.charAt(index));
            if (Character.isLetterOrDigit(current)) {
                token.append(current);
            } else {
                flushToken(token, output);
            }
        }
        flushToken(token, output);
    }

    private static void flushToken(StringBuilder token, Set<String> output) {
        if (!token.isEmpty()) output.add(token.toString());
        token.setLength(0);
    }

    private static List<String> donorRepositories(ChallengeProblem problem) {
        TreeSet<String> repositories = new TreeSet<>();
        for (CorpusSourceEntry source : problem.sources()) {
            repositories.add(source.repository());
        }
        return List.copyOf(repositories);
    }

    private static String repositoryKey(String value) {
        String checked = Objects.requireNonNull(value, "repository").trim();
        if (checked.isEmpty()) throw new IllegalArgumentException("repository");
        return checked.toLowerCase(Locale.ROOT);
    }

    private static List<String> normalizedTerms(List<String> values) {
        TreeSet<String> result = new TreeSet<>();
        for (String value : Objects.requireNonNull(values, "terms")) {
            TreeSet<String> tokens = new TreeSet<>();
            tokenize(Objects.requireNonNull(value, "term"), tokens);
            result.addAll(tokens);
        }
        return List.copyOf(result);
    }

    private static List<String> normalizedRepositories(List<String> values) {
        TreeSet<String> result = new TreeSet<>();
        for (String value : Objects.requireNonNull(values, "repositories")) {
            result.add(repositoryKey(value));
        }
        return List.copyOf(result);
    }

    private static <E extends Enum<E>> Set<E> enumSet(Set<E> values, Class<E> type) {
        Objects.requireNonNull(values, type.getSimpleName());
        TreeSet<E> ordered = new TreeSet<>(Comparator.comparing(Enum::name));
        for (E value : values) ordered.add(Objects.requireNonNull(value, "enum value"));
        return Collections.unmodifiableSet(ordered);
    }

    private static List<String> sortedStrings(List<String> values, String field) {
        TreeSet<String> result = new TreeSet<>();
        for (String value : Objects.requireNonNull(values, field)) {
            String checked = Objects.requireNonNull(value, field).trim();
            if (checked.isEmpty()) throw new IllegalArgumentException(field);
            result.add(checked);
        }
        return List.copyOf(result);
    }

    private static <E extends Enum<E>> EnumMap<E, IntAccumulator> enumAccumulators(
            Class<E> type) {
        EnumMap<E, IntAccumulator> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) result.put(value, new IntAccumulator());
        return result;
    }

    private static void append(Map<String, IntAccumulator> target, String key, int row) {
        target.computeIfAbsent(key, ignored -> new IntAccumulator()).add(row);
    }

    private static <K> Map<K, int[]> freezeAccumulators(Map<K, IntAccumulator> source) {
        LinkedHashMap<K, int[]> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, value.toArray()));
        return Map.copyOf(result);
    }

    private static <E extends Enum<E>> Map<E, Prepared> freezeEnumArrays(
            Map<E, int[]> source, Class<E> type) {
        EnumMap<E, Prepared> result = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            int[] values = source.getOrDefault(value, EMPTY);
            result.put(value, ChallengePostingIntersection.snapshotForIndex(values));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Prepared> freezeStringArrays(Map<String, int[]> source) {
        TreeMap<String, Prepared> result = new TreeMap<>();
        source.forEach((key, value) -> result.put(key, ChallengePostingIntersection.snapshotForIndex(value)));
        return Collections.unmodifiableMap(result);
    }

    private static long postingCount(Map<?, Prepared> postings) {
        long result = 0L;
        for (Prepared values : postings.values()) result = Math.addExact(result, values.size());
        return result;
    }

    private static <E extends Enum<E>> Map<E, Integer> immutableEnumCounts(
            Map<E, Integer> values, Class<E> type) {
        EnumMap<E, Integer> result = new EnumMap<>(type);
        Objects.requireNonNull(values, "values").forEach((key, value) -> {
            E checkedKey = Objects.requireNonNull(key, "key");
            Integer checkedValue = Objects.requireNonNull(value, "value");
            if (checkedValue < 0) throw new IllegalArgumentException("negative count");
            result.put(checkedKey, checkedValue);
        });
        return Collections.unmodifiableMap(result);
    }

    private static String indexRoot(List<ChallengeProblem> rows) {
        ArrayList<String> material = new ArrayList<>();
        material.add("SYNEXIA_CHALLENGE_SEARCH_INDEX_V2");
        for (ChallengeProblem problem : rows) {
            material.add(problem.id().stableId());
            material.add(problem.title());
            material.add(problem.status().name());
            material.add(problem.shape().map(Enum::name).orElse(""));
            material.add(problem.templateStyle().map(style -> style.kind().name()).orElse(""));
            material.add(Integer.toString(problem.implementationCount()));
            material.add(String.join(",", donorRepositories(problem)));
            problem.sources().stream()
                    .sorted(Comparator.comparing(CorpusSourceEntry::platform)
                            .thenComparing(CorpusSourceEntry::repository)
                            .thenComparing(CorpusSourceEntry::commit)
                            .thenComparing(CorpusSourceEntry::path))
                    .forEach(source -> {
                        material.add(source.platform());
                        material.add(source.repository());
                        material.add(source.commit());
                        material.add(source.path());
                    });
        }
        return digest(material);
    }

    private static String evaluationRoot(
            Query query,
            List<Hit> hits,
            int sourceImplementations,
            int executable,
            int unresolved,
            int conflicts,
            Map<ChallengePlatform, Integer> platforms,
            Map<AlgorithmShape, Integer> shapes,
            List<String> donorRepositories) {
        ArrayList<String> material = new ArrayList<>();
        material.add("SYNEXIA_CHALLENGE_SEARCH_EVALUATION_V1");
        material.add(query.platforms().stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse(""));
        material.add(query.shapes().stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse(""));
        material.add(query.templates().stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse(""));
        material.add(query.statuses().stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse(""));
        material.add(String.join(",", query.terms()));
        material.add(String.join(",", query.repositories()));
        material.add(Integer.toString(query.limit()));
        material.add(Integer.toString(sourceImplementations));
        material.add(Integer.toString(executable));
        material.add(Integer.toString(unresolved));
        material.add(Integer.toString(conflicts));
        platforms.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .forEach(entry -> material.add("P|" + entry.getKey().name() + "|" + entry.getValue()));
        shapes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
                .forEach(entry -> material.add("S|" + entry.getKey().name() + "|" + entry.getValue()));
        material.add("R|" + String.join(",", donorRepositories));
        for (Hit hit : hits) material.add("H|" + hit.problem().id().stableId());
        return digest(material);
    }

    private static String digest(List<String> values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String sha64(String value, String field) {
        String checked = Objects.requireNonNull(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static final class IntAccumulator {
        private int[] values = new int[8];
        private int size;

        void add(int value) {
            if (value < 0) throw new IllegalArgumentException("negative row");
            if (size > 0 && values[size - 1] == value) return;
            if (size == values.length) {
                values = Arrays.copyOf(values, Math.multiplyExact(values.length, 2));
            }
            values[size++] = value;
        }

        int[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }
}
