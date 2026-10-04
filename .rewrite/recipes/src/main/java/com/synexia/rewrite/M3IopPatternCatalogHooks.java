// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.iop.patterns.PatternCatalog;
import com.synexia.iop.patterns.PatternDescriptor;
import com.synexia.iop.patterns.PatternRecipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/**
 * Canonical IOP pattern-catalog binding used by the sole M3 OpenRewrite mutation lane.
 *
 * <p>OpenRewrite may mutate a participant only when every declared IOP pattern resolves to the
 * canonical {@link PatternCatalog}. Unknown declarations become typed residue and block mutation.
 * Pattern owner files are handled separately by {@link M3IopPatternClassHooks} because they define
 * the vocabulary itself.</p>
 *
 * @implNote pattern=synexia:iop; role=canonical pattern-vocabulary binding and admission collaborator;
 *     algorithm=literal/enum name resolution, normalized keyed lookup and unique-family admission;
 *     data.structures=immutable descriptor/enum maps, source-local name sets and sorted receipt
 *     lists. This collaborator is not a GoF Strategy implementation. Metadata binding is not
 *     semantic proof of roles, algorithms or behavior and grants no canonical promotion authority.
 */
public final class M3IopPatternCatalogHooks {
    private static final Set<String> PATTERN_ANNOTATIONS = Set.of(
            "AIPattern",
            "AIPatterns",
            "IAIPattern",
            "IAIPatterns");

    private static final PatternCatalog CATALOG = PatternCatalog.canonical();
    private record Coordinate(PatternDescriptor.Family family, String token) {}
    private static final Map<Coordinate, PatternDescriptor> DESCRIPTORS = descriptorIndex();
    private static final Map<String, Set<String>> ENUM_MEMBERS = Map.of(
            "AIPattern.GoF", names(com.synexia.iop.AIPattern.GoF.values()),
            "AIPattern.EIP", names(com.synexia.iop.AIPattern.EIP.values()),
            "AIPattern.Microservice", names(com.synexia.iop.AIPattern.Microservice.values()),
            "AIPattern.DAG", names(com.synexia.iop.AIPattern.DAG.values()),
            "IAIPattern.GoF", names(com.synexia.iop.IAIPattern.GoF.values()),
            "IAIPattern.EIP", names(com.synexia.iop.IAIPattern.EIP.values()),
            "IAIPattern.Microservice", names(com.synexia.iop.IAIPattern.Microservice.values()),
            "IAIPattern.DAG", names(com.synexia.iop.IAIPattern.DAG.values()));

    /** Source-only resolution facts are collected once per inspection, never cached across edits. */
    private record Resolution(String packageName, List<String> imports, List<String> staticImports,
            Set<String> declaredNames) {
        static Resolution of(J.CompilationUnit unit) {
            Set<String> declared = new HashSet<>();
            new JavaIsoVisitor<Set<String>>() {
                @Override public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration type, Set<String> names) {
                    names.add(type.getSimpleName());
                    return super.visitClassDeclaration(type, names);
                }
                @Override public J.VariableDeclarations.NamedVariable visitVariable(
                        J.VariableDeclarations.NamedVariable variable, Set<String> names) {
                    names.add(variable.getSimpleName());
                    return super.visitVariable(variable, names);
                }
            }.visit(unit, declared);
            return new Resolution(unit.getPackageDeclaration() == null ? "" : unit.getPackageDeclaration().getPackageName(),
                    unit.getImports().stream().filter(value -> !value.isStatic())
                            .map(value -> qualifiedName(value.getQualid())).toList(),
                    unit.getImports().stream().filter(J.Import::isStatic)
                            .map(value -> qualifiedName(value.getQualid())).toList(), Set.copyOf(declared));
        }
    }

    /** One canonical pattern binding. */
    public record PatternBinding(
            String declaration,
            String patternId,
            String descriptorRoot,
            String recipeRoot) {

        public PatternBinding {
            declaration = require(declaration, "declaration");
            patternId = require(patternId, "patternId");
            descriptorRoot = sha(descriptorRoot, "descriptorRoot");
            recipeRoot = sha(recipeRoot, "recipeRoot");
        }
    }

    /** One typed unresolved declaration. */
    public record TypedResidue(String declaration, String reason) {
        public TypedResidue {
            declaration = require(declaration, "declaration");
            reason = require(reason, "reason");
        }
    }

    /** Deterministic class-level catalog receipt. */
    public record Receipt(
            String className,
            List<PatternBinding> bindings,
            List<TypedResidue> residues,
            boolean admitted,
            String root) {

        public Receipt {
            className = require(className, "className");
            bindings = bindings == null
                    ? List.of()
                    : bindings.stream()
                            .filter(Objects::nonNull)
                            .distinct()
                            .sorted(Comparator.comparing(PatternBinding::patternId)
                                    .thenComparing(PatternBinding::declaration))
                            .toList();
            residues = residues == null
                    ? List.of()
                    : residues.stream()
                            .filter(Objects::nonNull)
                            .distinct()
                            .sorted(Comparator.comparing(TypedResidue::declaration)
                                    .thenComparing(TypedResidue::reason))
                            .toList();
            boolean expectedAdmission = !bindings.isEmpty() && residues.isEmpty();
            if (admitted != expectedAdmission) {
                throw new IllegalArgumentException("catalog receipt admission mismatch");
            }
            String expectedRoot = sha256("M3_IOP_PATTERN_CATALOG_RECEIPT_V1|"
                    + className + "|"
                    + bindings.stream()
                            .map(value -> value.declaration() + ":" + value.patternId() + ":"
                                    + value.descriptorRoot() + ":" + value.recipeRoot())
                            .reduce("", (a, b) -> a + b + "\n")
                    + "|"
                    + residues.stream()
                            .map(value -> value.declaration() + ":" + value.reason())
                            .reduce("", (a, b) -> a + b + "\n"));
            if (root == null || root.isBlank()) root = expectedRoot;
            if (!root.equals(expectedRoot)) {
                throw new IllegalArgumentException("catalog receipt root mismatch");
            }
        }
    }

    private M3IopPatternCatalogHooks() {}

    public static Receipt inspect(J.CompilationUnit unit, J.ClassDeclaration type) {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(type, "type");

        List<PatternBinding> bindings = new ArrayList<>();
        List<TypedResidue> residues = new ArrayList<>();
        Resolution resolution = null;

        for (J.Annotation annotation : type.getLeadingAnnotations()) {
            if (!PATTERN_ANNOTATIONS.contains(annotation.getSimpleName())) continue;
            if (resolution == null) resolution = Resolution.of(unit);
            if (!canonicalPatternAnnotation(resolution, annotation)) continue;
            inspectAnnotation(resolution, annotation, bindings, residues);
        }

        return new Receipt(
                type.getSimpleName(),
                bindings,
                residues,
                !bindings.isEmpty() && residues.isEmpty(),
                "");
    }

    public static String catalogRoot() {
        return CATALOG.root();
    }

    /**
     * Resolve an explicit participant name without inventing a second pattern vocabulary.
     * Exact canonical IDs disambiguate shared names; unqualified names and existing aliases
     * are admitted only when they identify one descriptor across all canonical families.
     */
    static Optional<PatternDescriptor> resolveParticipantPattern(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String requested = name.strip();
        Optional<PatternDescriptor> exact = CATALOG.get(requested);
        if (exact.isPresent()) return exact;
        List<PatternDescriptor> matches = Arrays.stream(PatternDescriptor.Family.values())
                .map(family -> resolveDescriptor(family, requested))
                .flatMap(Optional::stream)
                .distinct()
                .toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    private static void inspectAnnotation(
            Resolution resolution,
            J.Annotation annotation,
            List<PatternBinding> bindings,
            List<TypedResidue> residues) {

        int initialCount = bindings.size() + residues.size();
        boolean container = annotation.getSimpleName().endsWith("Patterns");
        List<Expression> arguments = annotation.getArguments();
        for (Expression argument : arguments == null ? List.<Expression>of() : arguments) {
            if (argument instanceof J.Empty) continue;
            if (container) {
                Expression value = argument;
                if (argument instanceof J.Assignment assignment) {
                    if (!(assignment.getVariable() instanceof J.Identifier id)
                            || !"value".equals(id.getSimpleName())) {
                        residues.add(new TypedResidue(annotation.getSimpleName(), "INVALID_PATTERN_CONTAINER"));
                        continue;
                    }
                    value = assignment.getAssignment();
                }
                inspectContainer(resolution, annotation.getSimpleName(), value, bindings, residues);
            } else if (argument instanceof J.Assignment assignment
                    && assignment.getVariable() instanceof J.Identifier id) {
                family(id.getSimpleName()).ifPresent(family -> inspectConstants(resolution,
                        annotation.getSimpleName(), id.getSimpleName(), family,
                        assignment.getAssignment(), bindings, residues));
            } else {
                residues.add(new TypedResidue(annotation.getSimpleName(), "EXPECTED_PATTERN_ATTRIBUTE"));
            }
        }
        if (initialCount == bindings.size() + residues.size()) {
            residues.add(new TypedResidue(annotation.getSimpleName(), "NO_PATTERN_CONSTANT"));
        }
    }

    private static void inspectContainer(Resolution resolution, String container, Expression expression,
            List<PatternBinding> bindings, List<TypedResidue> residues) {
        for (Expression value : elements(expression)) {
            if (value instanceof J.Annotation nested
                    && (nested.getSimpleName() + "s").equals(container)
                    && canonicalPatternAnnotation(resolution, nested)) {
                inspectAnnotation(resolution, nested, bindings, residues);
            } else {
                residues.add(new TypedResidue(container, "INVALID_PATTERN_CONTAINER_MEMBER"));
            }
        }
    }

    private static void inspectConstants(Resolution resolution, String annotation, String attribute,
            PatternDescriptor.Family family, Expression expression,
            List<PatternBinding> bindings, List<TypedResidue> residues) {
        String enumName = annotation + "." + switch (family) {
            case GOF -> "GoF";
            case EIP -> "EIP";
            case DAG -> "DAG";
            case MICROSERVICE -> "Microservice";
            default -> throw new IllegalArgumentException("unsupported annotation family");
        };
        for (Expression value : elements(expression)) {
            J.Identifier member = value instanceof J.FieldAccess access ? access.getName()
                    : value instanceof J.Identifier identifier ? identifier : null;
            String constant = member == null ? "" : member.getSimpleName();
            String declaration = annotation + "." + attribute + (constant.isEmpty() ? "" : "=" + constant);
            if (member == null || !ENUM_MEMBERS.get(enumName).contains(constant)
                    || !canonicalEnumMember(resolution, value, member, "com.synexia.iop." + enumName)) {
                residues.add(new TypedResidue(declaration, "EXPECTED_CANONICAL_ENUM_CONSTANT"));
                continue;
            }
            Optional<PatternDescriptor> descriptor = resolveDescriptor(family, constant);
            if (descriptor.isEmpty()) {
                residues.add(new TypedResidue(declaration,
                        "PATTERN_NOT_IN_CANONICAL_CATALOG:" + requestedPatternCoordinate(family, constant)));
                continue;
            }
            PatternDescriptor admitted = descriptor.orElseThrow();
            PatternRecipe recipe = CATALOG.recipe(admitted.id());
            bindings.add(new PatternBinding(declaration, admitted.id(), admitted.root(), recipe.root()));
        }
    }

    private static List<Expression> elements(Expression expression) {
        if (expression instanceof J.NewArray array && array.getInitializer() != null) {
            // OpenRewrite represents an empty initializer with J.Empty on some parser versions.
            return array.getInitializer().stream().filter(value -> !(value instanceof J.Empty)).toList();
        }
        return List.of(expression);
    }

    private static boolean canonicalEnumMember(Resolution resolution, Expression expression,
            J.Identifier member, String owner) {
        JavaType.Variable field = member.getFieldType();
        if (field != null && TypeUtils.asFullyQualified(field.getOwner()) != null) {
            return TypeUtils.isOfClassType(field.getOwner(), owner);
        }
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(expression.getType());
        if (type != null && !TypeUtils.isOfClassType(type, owner)) return false;
        if (expression instanceof J.FieldAccess access) {
            return resolvesType(resolution, qualifiedName(access.getTarget()), owner);
        }
        if (resolution.declaredNames().contains(member.getSimpleName())) return false;
        List<String> explicit = resolution.staticImports().stream()
                .filter(value -> value.endsWith("." + member.getSimpleName())).toList();
        if (!explicit.isEmpty()) {
            return explicit.size() == 1 && explicit.getFirst().equals(owner + "." + member.getSimpleName());
        }
        // An unresolved on-demand import from another owner cannot establish unique binding.
        return resolution.staticImports().stream().filter(value -> value.endsWith(".*"))
                .toList().equals(List.of(owner + ".*"));
    }

    private static Optional<PatternDescriptor.Family> family(String attribute) {
        return switch (attribute) {
            case "gof" -> Optional.of(PatternDescriptor.Family.GOF);
            case "eip" -> Optional.of(PatternDescriptor.Family.EIP);
            case "microservice" -> Optional.of(PatternDescriptor.Family.MICROSERVICE);
            case "dag" -> Optional.of(PatternDescriptor.Family.DAG);
            default -> Optional.empty();
        };
    }

    private static Optional<PatternDescriptor> resolveDescriptor(
            PatternDescriptor.Family family, String constant) {
        String requested = canonicalToken(alias(family, constant));
        return Optional.ofNullable(DESCRIPTORS.get(new Coordinate(family, requested)));
    }

    private static String requestedPatternCoordinate(
            PatternDescriptor.Family family, String constant) {
        return family.name().toLowerCase(Locale.ROOT)
                + ":"
                + alias(family, constant).toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String alias(PatternDescriptor.Family family, String constant) {
        if (family != PatternDescriptor.Family.MICROSERVICE) return constant;
        return switch (constant) {
            case "TRANSACTIONAL_OUTBOX" -> "OUTBOX";
            case "CLIENT_SIDE_DISCOVERY", "SERVER_SIDE_DISCOVERY" -> "SERVICE_DISCOVERY";
            default -> constant;
        };
    }

    private static String canonicalToken(String value) {
        return value == null
                ? ""
                : value.replaceAll("[^A-Za-z0-9]+", "").toLowerCase(Locale.ROOT);
    }

    private static Map<Coordinate, PatternDescriptor> descriptorIndex() {
        Map<Coordinate, PatternDescriptor> out = new HashMap<>();
        for (PatternDescriptor descriptor : CATALOG.patterns()) {
            Coordinate key = new Coordinate(descriptor.family(), canonicalToken(descriptor.name()));
            PatternDescriptor previous = out.putIfAbsent(key, descriptor);
            if (previous != null && !previous.equals(descriptor)) {
                throw new IllegalStateException("ambiguous canonical pattern coordinate: " + key);
            }
        }
        return Map.copyOf(out);
    }

    private static Set<String> names(Enum<?>[] members) {
        return Arrays.stream(members).map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }

    private static boolean canonicalPatternAnnotation(Resolution resolution, J.Annotation annotation) {
        String simpleName = annotation.getSimpleName();
        if (!PATTERN_ANNOTATIONS.contains(simpleName)) return false;

        String expected = "com.synexia.iop." + simpleName;
        JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
        if (type != null) return TypeUtils.isOfClassType(type, expected);
        return resolvesType(resolution, qualifiedName(annotation.getAnnotationType()), expected);
    }

    /** Source-only previews remain supported, but known type attribution always wins. */
    private static boolean resolvesType(Resolution resolution, String written, String expected) {
        if (expected.equals(written)) return true;
        if (written.isEmpty()) return false;
        int dot = written.indexOf('.');
        String first = dot < 0 ? written : written.substring(0, dot);
        if (resolution.declaredNames().contains(first)) return false;
        List<String> imports = resolution.imports();
        // A single-type import shadows on-demand imports and same-package lookup.
        List<String> explicit = imports.stream().filter(value -> value.endsWith("." + first)).toList();
        if (!explicit.isEmpty()) {
            return explicit.size() == 1 && (explicit.getFirst() + written.substring(first.length())).equals(expected);
        }
        if ((resolution.packageName() + "." + written).equals(expected)) return true;
        return imports.stream().filter(value -> value.endsWith(".*"))
                .anyMatch(value -> (value.substring(0, value.length() - 1) + written).equals(expected));
    }

    private static String qualifiedName(J tree) {
        if (tree instanceof J.Identifier identifier) return identifier.getSimpleName();
        if (tree instanceof J.FieldAccess access) {
            String target = qualifiedName(access.getTarget());
            return target.isEmpty() ? "" : target + "." + access.getSimpleName();
        }
        return "";
    }

    private static String sha(String value, String name) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be lowercase SHA-256");
        }
        return value;
    }

    private static String sha256(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " required");
        return value.trim();
    }
}
