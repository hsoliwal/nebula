// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import com.synexia.job.IProgressMonitor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Deterministic serial join across the challenge corpus, canonical Java shapes, executable local
 * primitives and pinned native mechanics donors.
 *
 * <p>This is review evidence only. It never reads challenge sites at runtime, copies problem
 * solutions, loads native libraries, changes dependencies, rewrites source or grants replacement
 * authority. One file is reviewed completely before the next file is visited.</p>
 */
public final class ChallengeDonorReviewPlan {

    public enum Disposition {
        HOLD_UNCLASSIFIED,
        REUSE_LOCAL_EXECUTABLE,
        REVIEW_NATIVE_MECHANICS,
        RETAIN_CANONICAL_JAVA
    }

    public record CategoryRow(
            int ordinal,
            ChallengePlatform platform,
            String categoryId,
            List<AlgorithmShape> shapes,
            List<String> localPrimitiveIds,
            List<String> nativeDonors,
            List<String> nativeMechanics,
            String root) {

        public CategoryRow {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            platform = concrete(platform);
            categoryId = token(categoryId, "categoryId");
            shapes = stableShapes(shapes);
            localPrimitiveIds = stableText(localPrimitiveIds, "localPrimitiveIds");
            nativeDonors = stableText(nativeDonors, "nativeDonors");
            nativeMechanics = stableText(nativeMechanics, "nativeMechanics");
            String expected = categoryRoot(
                    platform, categoryId, shapes, localPrimitiveIds, nativeDonors, nativeMechanics);
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("CATEGORY_REVIEW_ROOT");
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    public record FileRow(
            int ordinal,
            String adapterId,
            ChallengePlatform platform,
            CompetitiveProblemCategory category,
            String shape,
            List<String> platformCategoryIds,
            List<String> localPrimitiveIds,
            List<String> nativeDonors,
            List<String> nativeMechanics,
            ProblemOptimizationCatalog.NativeLane nativeLane,
            Disposition disposition,
            String rationale,
            String root) {

        public FileRow {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            adapterId = token(adapterId, "adapterId");
            platform = Objects.requireNonNull(platform, "platform");
            category = Objects.requireNonNull(category, "category");
            shape = Objects.requireNonNullElse(shape, "").strip();
            if (!shape.isEmpty()) AlgorithmShape.valueOf(shape);
            platformCategoryIds = stableText(platformCategoryIds, "platformCategoryIds");
            localPrimitiveIds = stableText(localPrimitiveIds, "localPrimitiveIds");
            nativeDonors = stableText(nativeDonors, "nativeDonors");
            nativeMechanics = stableText(nativeMechanics, "nativeMechanics");
            nativeLane = Objects.requireNonNull(nativeLane, "nativeLane");
            disposition = Objects.requireNonNull(disposition, "disposition");
            rationale = token(rationale, "rationale");
            String expected = fileRoot(
                    adapterId,
                    platform,
                    category,
                    shape,
                    platformCategoryIds,
                    localPrimitiveIds,
                    nativeDonors,
                    nativeMechanics,
                    nativeLane,
                    disposition,
                    rationale);
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("FILE_REVIEW_ROOT");
        }

        public boolean replacementAuthority() {
            return false;
        }

        public boolean nativeExecutionAuthority() {
            return false;
        }
    }

    public record Report(
            List<CategoryRow> categories,
            List<FileRow> files,
            String challengeCategoryRoot,
            String optimizationRoot,
            String localPrimitiveRoot,
            String nativeDonorRoot,
            String root) {
        public Report {
            categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
            files = List.copyOf(Objects.requireNonNull(files, "files"));
            challengeCategoryRoot = sha256(challengeCategoryRoot, "challengeCategoryRoot");
            optimizationRoot = sha256(optimizationRoot, "optimizationRoot");
            localPrimitiveRoot = sha256(localPrimitiveRoot, "localPrimitiveRoot");
            nativeDonorRoot = sha256(nativeDonorRoot, "nativeDonorRoot");
            CatalogueDigest digest = new CatalogueDigest("SYNEXIA_CHALLENGE_DONOR_REVIEW_REPORT_V1")
                    .text(challengeCategoryRoot)
                    .text(optimizationRoot)
                    .text(localPrimitiveRoot)
                    .text(nativeDonorRoot);
            categories.forEach(row -> digest.text(row.root()));
            files.forEach(row -> digest.text(row.root()));
            String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha256(root, "root");
            if (!root.equals(expected)) throw new IllegalArgumentException("REVIEW_REPORT_ROOT");
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    private ChallengeDonorReviewPlan() {}

    public static Report reviewAll(IProgressMonitor monitor) {
        return review(null, monitor);
    }

    public static Report review(
            ChallengePlatform platform,
            IProgressMonitor suppliedMonitor) {
        IProgressMonitor monitor =
                suppliedMonitor == null ? IProgressMonitor.noop() : suppliedMonitor;
        if (platform == ChallengePlatform.OTHER) {
            throw new IllegalArgumentException("OTHER has no public challenge taxonomy");
        }

        List<ChallengeCategoryCatalog.Category> selectedCategories =
                platform == null
                        ? ChallengeCategoryCatalog.categories()
                        : ChallengeCategoryCatalog.byPlatform(platform);
        List<ProblemAdapter> selectedFiles =
                platform == null
                        ? ProblemAdapterCatalog.all()
                        : ProblemAdapterCatalog.byPlatform(platform.name());

        int total = Math.addExact(selectedCategories.size(), selectedFiles.size());
        monitor.beginTask("serial challenge/native donor review", total);
        try {
            monitor.checkCanceled();
            List<CategoryRow> categoryRows = reviewCategories(selectedCategories, monitor);
            List<FileRow> fileRows = reviewFiles(selectedFiles, monitor);
            return new Report(
                    categoryRows,
                    fileRows,
                    ChallengeCategoryCatalog.root(),
                    ProblemOptimizationCatalog.root(),
                    ChallengeSearchPrimitiveCatalog.root(),
                    NativeMechanicsDonorCatalog.root(),
                    "");
        } finally {
            monitor.done();
        }
    }

    /** One fully reviewed row per public challenge taxonomy category. */
    public static List<CategoryRow> reviewCategories(
            List<ChallengeCategoryCatalog.Category> supplied,
            IProgressMonitor suppliedMonitor) {
        Objects.requireNonNull(supplied, "supplied");
        IProgressMonitor monitor =
                suppliedMonitor == null ? IProgressMonitor.noop() : suppliedMonitor;
        ArrayList<ChallengeCategoryCatalog.Category> categories = new ArrayList<>(supplied);
        categories.sort(
                Comparator.comparing(
                                (ChallengeCategoryCatalog.Category row) ->
                                        row.platform().name())
                        .thenComparing(ChallengeCategoryCatalog.Category::id));

        ArrayList<CategoryRow> result = new ArrayList<>(categories.size());
        int ordinal = 0;
        for (ChallengeCategoryCatalog.Category category : categories) {
            monitor.checkCanceled();
            monitor.subTask(category.platform() + ":" + category.id());
            result.add(categoryRow(ordinal++, category));
            monitor.worked(1);
        }
        return List.copyOf(result);
    }

    /** One completely reviewed source file before advancing to the next file. */
    public static List<FileRow> reviewFiles(
            List<ProblemAdapter> supplied,
            IProgressMonitor suppliedMonitor) {
        Objects.requireNonNull(supplied, "supplied");
        IProgressMonitor monitor =
                suppliedMonitor == null ? IProgressMonitor.noop() : suppliedMonitor;
        ArrayList<ProblemAdapter> files = new ArrayList<>(supplied);
        files.sort(Comparator.comparing(ProblemAdapter::id));

        ArrayList<FileRow> result = new ArrayList<>(files.size());
        int ordinal = 0;
        for (ProblemAdapter adapter : files) {
            monitor.checkCanceled();
            monitor.subTask(adapter.id());
            result.add(fileRow(ordinal++, adapter));
            monitor.worked(1);
        }
        return List.copyOf(result);
    }

    public static String renderCategoryTsv(ChallengePlatform platform) {
        Report report = review(platform, IProgressMonitor.noop());
        StringBuilder out = new StringBuilder(8192);
        out.append("ordinal\tplatform\tcategory\tshapes\tlocal_primitives\tnative_donors")
                .append("\tnative_mechanics\treplacement_authority\troot\n");
        for (CategoryRow row : report.categories()) {
            row(out,
                    Integer.toString(row.ordinal()),
                    row.platform().name(),
                    row.categoryId(),
                    joinShapes(row.shapes()),
                    String.join(",", row.localPrimitiveIds()),
                    String.join(",", row.nativeDonors()),
                    String.join(",", row.nativeMechanics()),
                    Boolean.toString(row.replacementAuthority()),
                    row.root());
        }
        return out.toString();
    }

    public static String renderFileTsv(ChallengePlatform platform) {
        Report report = review(platform, IProgressMonitor.noop());
        StringBuilder out = new StringBuilder(16384);
        out.append("ordinal\tadapter_id\tplatform\tcanonical_category\tshape")
                .append("\tplatform_categories\tlocal_primitives\tnative_donors")
                .append("\tnative_mechanics\tnative_lane\tdisposition\trationale")
                .append("\treplacement_authority\troot\n");
        for (FileRow value : report.files()) {
            row(out,
                    Integer.toString(value.ordinal()),
                    value.adapterId(),
                    value.platform().name(),
                    value.category().name(),
                    value.shape(),
                    String.join(",", value.platformCategoryIds()),
                    String.join(",", value.localPrimitiveIds()),
                    String.join(",", value.nativeDonors()),
                    String.join(",", value.nativeMechanics()),
                    value.nativeLane().name(),
                    value.disposition().name(),
                    value.rationale(),
                    Boolean.toString(value.replacementAuthority()),
                    value.root());
        }
        return out.toString();
    }

    private static CategoryRow categoryRow(
            int ordinal,
            ChallengeCategoryCatalog.Category category) {
        LinkedHashSet<String> primitives = new LinkedHashSet<>();
        LinkedHashSet<String> donors = new LinkedHashSet<>();
        EnumSet<NativeMechanicsDonorCatalog.Mechanic> mechanics =
                EnumSet.noneOf(NativeMechanicsDonorCatalog.Mechanic.class);

        for (AlgorithmShape shape : category.shapes()) {
            ChallengeSearchPrimitiveCatalog.forShape(shape)
                    .forEach(value -> primitives.add(value.id()));
            ProblemOptimizationCatalog.Plan plan = ProblemOptimizationCatalog.plan(shape);
            for (NativeMechanicsDonorCatalog.Match match : plan.nativeMechanicsDonors()) {
                donors.add(match.donor().repository() + "@" + match.donor().revision());
                mechanics.addAll(match.matchedMechanics());
            }
        }

        return new CategoryRow(
                ordinal,
                category.platform(),
                category.id(),
                category.shapes(),
                List.copyOf(primitives),
                List.copyOf(donors),
                mechanics.stream().map(Enum::name).toList(),
                "");
    }

    private static FileRow fileRow(int ordinal, ProblemAdapter adapter) {
        ChallengePlatform platform = ChallengePlatform.from(adapter.source().platform());
        if (!adapter.classification().classified()) {
            return new FileRow(
                    ordinal,
                    adapter.id(),
                    platform,
                    CompetitiveProblemCategory.OTHER,
                    "",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    ProblemOptimizationCatalog.NativeLane.JAVA_ONLY,
                    Disposition.HOLD_UNCLASSIFIED,
                    "shape unresolved; preserve current source and classify before any donor nomination",
                    "");
        }

        AlgorithmShape shape = adapter.classification().shape();
        ProblemOptimizationCatalog.Plan plan = ProblemOptimizationCatalog.plan(shape);
        List<String> categoryIds =
                platform == ChallengePlatform.OTHER
                        ? List.of()
                        : ChallengeCategoryCatalog.forShape(shape).stream()
                                .filter(row -> row.platform() == platform)
                                .map(ChallengeCategoryCatalog.Category::id)
                                .sorted()
                                .toList();
        List<String> primitives =
                ChallengeSearchPrimitiveCatalog.forShape(shape).stream()
                        .map(ChallengeSearchPrimitiveCatalog.Primitive::id)
                        .sorted()
                        .toList();

        List<NativeMechanicsDonorCatalog.Match> matches = plan.nativeMechanicsDonors();
        List<String> donors =
                matches.stream()
                        .map(match ->
                                match.donor().repository()
                                        + "@"
                                        + match.donor().revision())
                        .distinct()
                        .sorted()
                        .toList();
        List<String> mechanics =
                matches.stream()
                        .flatMap(match -> match.matchedMechanics().stream())
                        .map(Enum::name)
                        .distinct()
                        .sorted()
                        .toList();

        Disposition disposition;
        String rationale;
        if (!primitives.isEmpty()) {
            disposition = Disposition.REUSE_LOCAL_EXECUTABLE;
            rationale =
                    "reuse existing executable primitive first; challenge/native donors remain "
                            + "comparison evidence and later replacement still requires exact proof";
        } else if (plan.nativeEligible() && !matches.isEmpty()) {
            disposition = Disposition.REVIEW_NATIVE_MECHANICS;
            rationale =
                    "no exact local primitive registered; native mechanics may be benchmarked only "
                            + "through license/ABI/bounds/parity/determinism/speedup admission";
        } else {
            disposition = Disposition.RETAIN_CANONICAL_JAVA;
            rationale =
                    "retain canonical Java donor; no reviewed executable evidence justifies a "
                            + "native or source replacement";
        }

        return new FileRow(
                ordinal,
                adapter.id(),
                platform,
                CompetitiveProblemCategory.fromShape(shape),
                shape.name(),
                categoryIds,
                primitives,
                donors,
                mechanics,
                plan.nativeLane(),
                disposition,
                rationale,
                "");
    }

    private static String categoryRoot(
            ChallengePlatform platform,
            String categoryId,
            List<AlgorithmShape> shapes,
            List<String> primitiveIds,
            List<String> nativeDonors,
            List<String> nativeMechanics) {
        CatalogueDigest digest =
                new CatalogueDigest("SYNEXIA_CHALLENGE_DONOR_CATEGORY_REVIEW_V1")
                        .text(platform.name())
                        .text(categoryId);
        shapes.forEach(value -> digest.text(value.name()));
        primitiveIds.forEach(digest::text);
        nativeDonors.forEach(digest::text);
        nativeMechanics.forEach(digest::text);
        return digest.finish();
    }

    private static String fileRoot(
            String adapterId,
            ChallengePlatform platform,
            CompetitiveProblemCategory category,
            String shape,
            List<String> categoryIds,
            List<String> primitiveIds,
            List<String> nativeDonors,
            List<String> nativeMechanics,
            ProblemOptimizationCatalog.NativeLane nativeLane,
            Disposition disposition,
            String rationale) {
        CatalogueDigest digest =
                new CatalogueDigest("SYNEXIA_CHALLENGE_DONOR_FILE_REVIEW_V1")
                        .text(adapterId)
                        .text(platform.name())
                        .text(category.name())
                        .text(shape)
                        .text(nativeLane.name())
                        .text(disposition.name())
                        .text(rationale);
        categoryIds.forEach(digest::text);
        primitiveIds.forEach(digest::text);
        nativeDonors.forEach(digest::text);
        nativeMechanics.forEach(digest::text);
        return digest.finish();
    }

    private static ChallengePlatform concrete(ChallengePlatform platform) {
        ChallengePlatform checked = Objects.requireNonNull(platform, "platform");
        if (checked == ChallengePlatform.OTHER) {
            throw new IllegalArgumentException("concrete challenge platform required");
        }
        return checked;
    }

    private static List<AlgorithmShape> stableShapes(List<AlgorithmShape> values) {
        TreeSet<AlgorithmShape> stable =
                new TreeSet<>(Comparator.comparing(Enum::name));
        stable.addAll(Objects.requireNonNull(values, "shapes"));
        return List.copyOf(stable);
    }

    private static List<String> stableText(List<String> values, String field) {
        TreeSet<String> stable = new TreeSet<>();
        for (String value : Objects.requireNonNull(values, field)) {
            stable.add(token(value, field));
        }
        return List.copyOf(stable);
    }

    private static String joinShapes(List<AlgorithmShape> shapes) {
        return shapes.stream().map(Enum::name).reduce("", (left, right) ->
                left.isEmpty() ? right : left + "," + right);
    }

    private static void row(StringBuilder out, String... fields) {
        for (int index = 0; index < fields.length; index++) {
            if (index > 0) out.append('\t');
            out.append(escape(fields[index]));
        }
        out.append('\n');
    }

    private static String escape(String value) {
        return Objects.requireNonNull(value, "value")
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.length() > 8192
                || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha256(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "INVALID_" + field.toUpperCase(Locale.ROOT));
        }
        return value;
    }
}
