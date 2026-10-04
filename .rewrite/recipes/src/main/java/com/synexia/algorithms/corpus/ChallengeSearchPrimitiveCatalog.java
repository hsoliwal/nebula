// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Specialized prepared/native search hot paths already owned by Synexia.
 *
 * <p>This catalogue is deliberately smaller than {@link ProblemOptimizationCatalog}: a shape can
 * be classified without there being a reusable local primitive for it. Entries identify actual
 * Java owners/entry points plus the optional stable native C symbol when one exists. They are
 * candidate reuse evidence only and never authorize atom replacement.</p>
 */
public final class ChallengeSearchPrimitiveCatalog {
    private static final String CONVERGENCE_ALGORITHM_KERNEL =
            "com.synexia.convergence.donor.AlgorithmKernel";
    private static final String CONVERGENCE_JNI_SYMBOL_PREFIX =
            "Java_com_synexia_convergence_donor_JniAlgorithmKernel_";

    public enum Representation {
        SIGNED_I32_ASCENDING,
        BINARY_U8,
        UTF16_STRING
    }

    public record Primitive(
            String id,
            List<AlgorithmShape> shapes,
            Representation representation,
            String javaOwner,
            String javaEntryPoint,
            String nativeSymbol,
            boolean prepared,
            String semantics,
            String root) {

        public Primitive {
            id = token(id, "id");
            EnumSet<AlgorithmShape> stable = EnumSet.noneOf(AlgorithmShape.class);
            stable.addAll(Objects.requireNonNull(shapes, "shapes"));
            if (stable.isEmpty()) throw new IllegalArgumentException("primitive shapes required");
            shapes = List.copyOf(stable);
            representation = Objects.requireNonNull(representation, "representation");
            javaOwner = token(javaOwner, "javaOwner");
            javaEntryPoint = token(javaEntryPoint, "javaEntryPoint");
            nativeSymbol = Objects.requireNonNullElse(nativeSymbol, "").strip();
            if (!nativeSymbol.isEmpty()
                    && !nativeSymbol.matches("[A-Za-z_][A-Za-z0-9_]{0,127}")) {
                throw new IllegalArgumentException("nativeSymbol");
            }
            semantics = token(semantics, "semantics");
            CatalogueDigest digest =
                    new CatalogueDigest("SYNEXIA_EXECUTABLE_SEARCH_PRIMITIVE_V1")
                            .text(id)
                            .text(representation.name())
                            .text(javaOwner)
                            .text(javaEntryPoint)
                            .text(nativeSymbol)
                            .text(Boolean.toString(prepared))
                            .text(semantics);
            shapes.forEach(shape -> digest.text(shape.name()));
            String expected = digest.finish();
            root = root == null || root.isBlank() ? expected : sha256(root);
            if (!root.equals(expected)) throw new IllegalArgumentException("primitive root");
        }

        public boolean nativeOptional() {
            return !nativeSymbol.isEmpty();
        }

        public boolean replacementAuthority() {
            return false;
        }
    }

    private static final List<Primitive> PRIMITIVES =
            List.of(
                    primitive(
                            "challenge.lower-bound-i32.v1",
                            List.of(AlgorithmShape.BINARY_SEARCH),
                            Representation.SIGNED_I32_ASCENDING,
                            ChallengeNativeSearch.class.getName(),
                            "lowerBound(PreparedInt32,int,IProgressMonitor)",
                            "snx_search_lower_bound_i32",
                            true,
                            "first rank whose signed int32 value is >= target"),
                    primitive(
                            "challenge.galloping-lower-bound-i32.v1",
                            List.of(
                                    AlgorithmShape.EXPONENTIAL_SEARCH,
                                    AlgorithmShape.GALLOPING_SEARCH),
                            Representation.SIGNED_I32_ASCENDING,
                            ChallengeNativeSearch.class.getName(),
                            "gallopingLowerBound(PreparedInt32,int,int,IProgressMonitor)",
                            "snx_search_galloping_lower_bound_i32",
                            true,
                            "first suffix rank >= target; exponential search is from-index zero"),
                    primitive(
                            "challenge.interpolation-index-i32.v1",
                            List.of(AlgorithmShape.INTERPOLATION_SEARCH),
                            Representation.SIGNED_I32_ASCENDING,
                            ChallengeNativeSearch.class.getName(),
                            "interpolationIndexOf(PreparedInt32,int,IProgressMonitor)",
                            "",
                            true,
                            "exact target index using interpolation probing; absence returns -1"),
                    primitive(
                            "convergence.jump-exact-i32.v1",
                            List.of(AlgorithmShape.JUMP_SEARCH),
                            Representation.SIGNED_I32_ASCENDING,
                            CONVERGENCE_ALGORITHM_KERNEL,
                            "jumpSearchExact(int[],int,int,int)",
                            CONVERGENCE_JNI_SYMBOL_PREFIX + "jumpSearchExact0",
                            false,
                            "first equal index in a validated ascending int32 subrange using jump search; absence returns -1"),
                    primitive(
                            "convergence.fibonacci-exact-i32.v1",
                            List.of(AlgorithmShape.FIBONACCI_SEARCH),
                            Representation.SIGNED_I32_ASCENDING,
                            CONVERGENCE_ALGORITHM_KERNEL,
                            "fibonacciSearchExact(int[],int,int,int)",
                            CONVERGENCE_JNI_SYMBOL_PREFIX + "fibonacciSearchExact0",
                            false,
                            "first equal index in a validated ascending int32 subrange using Fibonacci partitioning; absence returns -1"),
                    primitive(
                            "convergence.ternary-exact-i32.v1",
                            List.of(AlgorithmShape.TERNARY_SEARCH),
                            Representation.SIGNED_I32_ASCENDING,
                            CONVERGENCE_ALGORITHM_KERNEL,
                            "ternarySearchExact(int[],int,int,int)",
                            CONVERGENCE_JNI_SYMBOL_PREFIX + "ternarySearchExact0",
                            false,
                            "first equal index in a validated ascending int32 subrange using ternary probing; absence returns -1"),
                    primitive(
                            "challenge.literal-u8-bmh.v1",
                            List.of(AlgorithmShape.BOYER_MOORE_HORSPOOL),
                            Representation.BINARY_U8,
                            ChallengeNativeSearch.class.getName(),
                            "indexOfBytes(byte[],byte[],IProgressMonitor)",
                            "snx_search_index_of_u8",
                            false,
                            "first exact binary literal occurrence; empty pattern matches zero"),
                    primitive(
                            "challenge.literal-u8-kmp.v1",
                            List.of(AlgorithmShape.KMP),
                            Representation.BINARY_U8,
                            ChallengeKmpSearch.class.getName(),
                            "indexOf(PreparedBytes,Prepared,IProgressMonitor)",
                            "snx_search_kmp_first_u8",
                            true,
                            "first exact binary literal occurrence using one reusable KMP prefix table"),
                    primitive(
                            "challenge.prefix-trie-u8.v1",
                            List.of(AlgorithmShape.TRIE),
                            Representation.BINARY_U8,
                            ChallengePrefixIndex.class.getName(),
                            "prefixCount(byte[],IProgressMonitor)",
                            "",
                            true,
                            "immutable exact/prefix multiplicity view over the prepared Aho trie; duplicate patterns retained"),
                    primitive(
                            "challenge.multi-first-u8-aho.v1",
                            List.of(AlgorithmShape.AHO_CORASICK),
                            Representation.BINARY_U8,
                            ChallengeMultiPatternSearch.class.getName(),
                            "firstOffsets(Prepared,byte[],IProgressMonitor)",
                            "snx_search_multi_first_u8",
                            true,
                            "first exact binary literal occurrence for every prepared pattern"),
                    primitive(
                            "challenge.rabin-karp-utf16.v1",
                            List.of(AlgorithmShape.RABIN_KARP),
                            Representation.UTF16_STRING,
                            ChallengeRabinKarpSearch.class.getName(),
                            "indexOf(String,String,IProgressMonitor)",
                            "snx_search_rabin_karp_u16",
                            false,
                            "first exact UTF-16 code-unit occurrence using rolling hash with equality verification"),
                    primitive(
                            "challenge.z-search-utf16.v1",
                            List.of(AlgorithmShape.Z_ALGORITHM),
                            Representation.UTF16_STRING,
                            ChallengeZSearch.class.getName(),
                            "indexOf(String,String,IProgressMonitor)",
                            "snx_search_z_u16",
                            false,
                            "first exact UTF-16 code-unit occurrence using virtual pattern/sentinel/text Z boxes"),
                    primitive(
                            "mindex.manacher-palindrome-utf16.v1",
                            List.of(AlgorithmShape.MANACHER),
                            Representation.UTF16_STRING,
                            "com.synexia.mindex.MIndexPalindromeIndex",
                            "compile(MIndexString)->longestPalindromeRange()",
                            "",
                            true,
                            "frozen UTF-16 odd/even Manacher radii; longest query returns the earliest half-open maximum palindrome range"),
                    primitive(
                            "challenge.posting-intersection-i32.v1",
                            List.of(AlgorithmShape.MERGE_WALK),
                            Representation.SIGNED_I32_ASCENDING,
                            ChallengePostingIntersection.class.getName(),
                            "intersect(Prepared,Prepared,int,IProgressMonitor)",
                            "snx_search_intersect_i32",
                            true,
                            "ascending unique signed int32 set intersection"));

    private static final Map<String, Primitive> BY_ID = byId(PRIMITIVES);
    private static final Map<AlgorithmShape, List<Primitive>> BY_SHAPE = byShape(PRIMITIVES);
    private static final Map<Representation, List<Primitive>> BY_REPRESENTATION =
            byRepresentation(PRIMITIVES);
    private static final List<Primitive> PREPARED =
            PRIMITIVES.stream().filter(Primitive::prepared).toList();
    private static final List<Primitive> NATIVE_OPTIONAL =
            PRIMITIVES.stream().filter(Primitive::nativeOptional).toList();
    private static final String ROOT = root(PRIMITIVES);

    private ChallengeSearchPrimitiveCatalog() {}

    public static List<Primitive> all() {
        return PRIMITIVES;
    }

    /** Precomputed immutable primitive list for one canonical shape. */
    public static List<Primitive> forShape(AlgorithmShape shape) {
        return BY_SHAPE.getOrDefault(Objects.requireNonNull(shape, "shape"), List.of());
    }

    /** Precomputed immutable primitive list for one storage/input representation. */
    public static List<Primitive> forRepresentation(Representation representation) {
        return BY_REPRESENTATION.getOrDefault(
                Objects.requireNonNull(representation, "representation"),
                List.of());
    }

    /** All reusable primitives with an owned/precomputed query structure. */
    public static List<Primitive> prepared() {
        return PREPARED;
    }

    /** All primitives exposing a stable optional native symbol. */
    public static List<Primitive> nativeOptional() {
        return NATIVE_OPTIONAL;
    }

    public static Primitive require(String id) {
        String checked = token(id, "id");
        Primitive primitive = BY_ID.get(checked);
        if (primitive == null) {
            throw new IllegalArgumentException("unknown primitive: " + checked);
        }
        return primitive;
    }

    public static String root() {
        return ROOT;
    }

    private static Map<String, Primitive> byId(List<Primitive> primitives) {
        LinkedHashMap<String, Primitive> result = new LinkedHashMap<>();
        for (Primitive primitive : primitives) {
            if (result.putIfAbsent(primitive.id(), primitive) != null) {
                throw new ExceptionInInitializerError("duplicate primitive id: " + primitive.id());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<AlgorithmShape, List<Primitive>> byShape(List<Primitive> primitives) {
        EnumMap<AlgorithmShape, ArrayList<Primitive>> mutable =
                new EnumMap<>(AlgorithmShape.class);
        for (Primitive primitive : primitives) {
            for (AlgorithmShape shape : primitive.shapes()) {
                mutable.computeIfAbsent(shape, ignored -> new ArrayList<>()).add(primitive);
            }
        }
        EnumMap<AlgorithmShape, List<Primitive>> frozen =
                new EnumMap<>(AlgorithmShape.class);
        mutable.forEach((shape, values) -> frozen.put(shape, List.copyOf(values)));
        return Collections.unmodifiableMap(frozen);
    }

    private static Map<Representation, List<Primitive>> byRepresentation(
            List<Primitive> primitives) {
        EnumMap<Representation, ArrayList<Primitive>> mutable =
                new EnumMap<>(Representation.class);
        for (Primitive primitive : primitives) {
            mutable.computeIfAbsent(primitive.representation(), ignored -> new ArrayList<>())
                    .add(primitive);
        }
        EnumMap<Representation, List<Primitive>> frozen =
                new EnumMap<>(Representation.class);
        mutable.forEach((representation, values) ->
                frozen.put(representation, List.copyOf(values)));
        return Collections.unmodifiableMap(frozen);
    }

    private static Primitive primitive(
            String id,
            List<AlgorithmShape> shapes,
            Representation representation,
            String javaOwner,
            String javaEntryPoint,
            String nativeSymbol,
            boolean prepared,
            String semantics) {
        return new Primitive(
                id,
                shapes,
                representation,
                javaOwner,
                javaEntryPoint,
                nativeSymbol,
                prepared,
                semantics,
                "");
    }

    private static String root(List<Primitive> primitives) {
        ArrayList<Primitive> ordered = new ArrayList<>(primitives);
        ordered.sort(Comparator.comparing(Primitive::id));
        CatalogueDigest digest =
                new CatalogueDigest("SYNEXIA_EXECUTABLE_SEARCH_PRIMITIVE_CATALOG_V1");
        ordered.forEach(value -> digest.text(value.root()));
        return digest.finish();
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.length() > 1024
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha256(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("root");
        }
        return value;
    }
}
