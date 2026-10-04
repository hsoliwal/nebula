// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.rewrite.sealed.SealHash;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/**
 * Read-only source visibility beside the Java semantic-atom catalogue.
 * Pattern: Facade; role: bounded source-set accounting. Sorted path/hash joins retain each
 * declared source once; this class neither parses source nor grants proof or mutation authority.
 */
public final class M3RecipeFirstSourceCoverageTable
        extends DataTable<M3RecipeFirstSourceCoverageTable.Row> {
    public static final String MAPPING_SCOPE =
            "JAVA_CLASS_METHOD_FIELD_INITIALIZER_LAMBDA_ANONYMOUS_ENUM_ATOMS";

    public M3RecipeFirstSourceCoverageTable(Recipe recipe) {
        super(recipe, "M3 recipe-first source visibility",
                "One row per supplied SourceFile, including unsupported and unresolved residue. "
                        + "File accounting is distinct from AST mapping and executed proof.");
    }

    /** These states describe this mapper's limitations, not a language's general capabilities. */
    public enum Disposition {
        JAVA_DECLARATIONS_MAPPED,
        UNRESOLVED_DECLARATION_ATTRIBUTION,
        PARTIAL_JAVA_ATOM_MAPPING,
        JAVA_NOT_PARSED,
        UNSUPPORTED_SOURCE,
        PARSE_ERROR
    }

    public record Row(
            @Column(displayName = "Source path", description = "Path within the declared workset.")
                    String sourcePath,
            @Column(displayName = "Rendered source SHA-256", description = "Hash of SourceFile.printAll(), not raw disk bytes.")
                    String renderedSourceSha256,
            @Column(displayName = "Source kind", description = "Existing M3SourceKind routing; TEXT is not language admission.")
                    String sourceKind,
            @Column(displayName = "Parser representation", description = "Actual SourceFile implementation class.")
                    String parserRepresentation,
            @Column(displayName = "Class declarations", description = "Observed explicit class declarations.")
                    int classDeclarations,
            @Column(displayName = "Method declarations", description = "Observed method and constructor declarations.")
                    int methodDeclarations,
            @Column(displayName = "Unresolved declarations", description = "Declarations whose owner or method attribution is missing.")
                    int unresolvedDeclarations,
            @Column(displayName = "Unmapped behavioral boundaries", description = "Behavioral boundaries still outside the explicit atom kinds owned by this mapper.")
                    int unmappedBehaviorBoundaries,
            @Column(displayName = "Disposition", description = "Typed source/declaration residue.")
                    Disposition disposition) {
        public Row {
            sourcePath = path(sourcePath);
            renderedSourceSha256 = SealHash.require(renderedSourceSha256);
            sourceKind = M3SourceKind.valueOf(sourceKind).name();
            if (!M3SourceKind.classify(sourcePath).name().equals(sourceKind)) {
                throw new IllegalArgumentException("SOURCE_KIND_PATH_MISMATCH");
            }
            parserRepresentation = text(parserRepresentation);
            disposition = Objects.requireNonNull(disposition, "disposition");
            if (classDeclarations < 0 || methodDeclarations < 0 || unresolvedDeclarations < 0
                    || unmappedBehaviorBoundaries < 0
                    || (long) unresolvedDeclarations > (long) classDeclarations + methodDeclarations) {
                throw new IllegalArgumentException("INVALID_SOURCE_COVERAGE_COUNTS");
            }
            boolean javaState = disposition == Disposition.JAVA_DECLARATIONS_MAPPED
                    || disposition == Disposition.UNRESOLVED_DECLARATION_ATTRIBUTION
                    || disposition == Disposition.PARTIAL_JAVA_ATOM_MAPPING;
            boolean javaCounts = javaState || disposition == Disposition.PARSE_ERROR
                    && sourceKind.equals(M3SourceKind.JAVA.name());
            if (javaState && !sourceKind.equals(M3SourceKind.JAVA.name())
                    || disposition == Disposition.JAVA_DECLARATIONS_MAPPED
                        && (unresolvedDeclarations != 0 || unmappedBehaviorBoundaries != 0)
                    || disposition == Disposition.UNRESOLVED_DECLARATION_ATTRIBUTION
                        && unresolvedDeclarations == 0
                    || disposition == Disposition.PARTIAL_JAVA_ATOM_MAPPING
                        && (unmappedBehaviorBoundaries == 0 || unresolvedDeclarations != 0)
                    || !javaCounts && (classDeclarations != 0 || methodDeclarations != 0
                        || unresolvedDeclarations != 0 || unmappedBehaviorBoundaries != 0)) {
                throw new IllegalArgumentException("INCONSISTENT_SOURCE_COVERAGE_DISPOSITION");
            }
        }

        public String mappingScope() { return MAPPING_SCOPE; }
        public boolean blocksMappedDeclarationAdmission() {
            return disposition != Disposition.JAVA_DECLARATIONS_MAPPED;
        }
        public boolean executedContractProof() { return false; }
        public boolean promotionAuthority() { return false; }

        public String root() {
            return SealHash.frame("M3-SOURCE-VISIBILITY-ROW/1", sourcePath, renderedSourceSha256,
                    sourceKind, parserRepresentation, Integer.toString(classDeclarations),
                    Integer.toString(methodDeclarations), Integer.toString(unresolvedDeclarations),
                    Integer.toString(unmappedBehaviorBoundaries), disposition.name(), MAPPING_SCOPE);
        }
    }

    /**
     * Exact accounting for the host-declared bounded workset. An edit-local scope need not
     * materialize, reparse or claim the whole repository. The host owns the independent expected
     * source list and must bind a larger scope separately if its contract crosses this boundary.
     */
    public static String requireVisibleSources(String scopeId, Map<String, String> expected,
            List<Row> rows, BooleanSupplier canceled) {
        checkCanceled(canceled);
        String scope = text(scopeId);
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(rows, "rows");
        TreeMap<String, String> sourceHashes = new TreeMap<>();
        for (var entry : expected.entrySet()) {
            checkCanceled(canceled);
            if (sourceHashes.putIfAbsent(path(entry.getKey()), SealHash.require(entry.getValue())) != null) {
                throw new IllegalArgumentException("DUPLICATE_EXPECTED_SOURCE");
            }
        }
        TreeMap<String, Row> observed = new TreeMap<>();
        for (Row row : rows) {
            checkCanceled(canceled);
            Objects.requireNonNull(row, "row");
            if (observed.putIfAbsent(row.sourcePath(), row) != null) {
                throw new IllegalArgumentException("DUPLICATE_OBSERVED_SOURCE:" + row.sourcePath());
            }
            if (!row.renderedSourceSha256().equals(sourceHashes.get(row.sourcePath()))) {
                throw new IllegalArgumentException("UNEXPECTED_OR_STALE_SOURCE:" + row.sourcePath());
            }
        }
        if (!sourceHashes.keySet().equals(observed.keySet())) {
            throw new IllegalArgumentException("MISSING_SOURCE_VISIBILITY");
        }
        ArrayList<String> roots = new ArrayList<>();
        roots.add("M3-BOUNDED-SOURCE-VISIBILITY/1");
        roots.add(scope);
        for (Row row : observed.values()) {
            checkCanceled(canceled);
            roots.add(row.root());
        }
        return SealHash.frame(roots.toArray(String[]::new));
    }

    /**
     * Additional narrow declaration-mapping gate. This never authorizes mutation or promotion;
     * contract/effect/dependency resolution and genuine executed proof remain separate gates.
     */
    public static String requireMappedJavaDeclarations(String scopeId, Map<String, String> expected,
            List<Row> rows, BooleanSupplier canceled) {
        String root = requireVisibleSources(scopeId, expected, rows, canceled);
        if (rows.isEmpty()) throw new IllegalArgumentException("EMPTY_DECLARATION_WORKSET");
        for (Row row : rows) {
            checkCanceled(canceled);
            if (row.blocksMappedDeclarationAdmission()) {
                throw new IllegalArgumentException("SOURCE_MAPPING_RESIDUE:" + row.sourcePath()
                        + ":" + row.disposition());
            }
        }
        return root;
    }

    static void checkCanceled(BooleanSupplier canceled) {
        if (Thread.currentThread().isInterrupted() || canceled != null && canceled.getAsBoolean()) {
            throw new CancellationException("M3_SOURCE_VISIBILITY_CANCELED");
        }
    }

    private static String text(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("INVALID_SOURCE_COVERAGE_TEXT");
        }
        return value;
    }

    private static String path(String value) {
        String normalized = text(value).replace('\\', '/');
        Path path = Path.of(normalized);
        if (path.isAbsolute() || normalized.matches("^[A-Za-z]:.*")
                || normalized.startsWith("/") || normalized.endsWith("/")
                || !path.normalize().toString().replace('\\', '/').equals(normalized)) {
            throw new IllegalArgumentException("INVALID_SOURCE_COVERAGE_PATH");
        }
        for (String part : normalized.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw new IllegalArgumentException("INVALID_SOURCE_COVERAGE_PATH");
            }
        }
        return normalized;
    }
}
