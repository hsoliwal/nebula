// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

/**
 * Deterministic class-level admission hooks for the M3 IOP pattern mutation lane.
 *
 * <p>IOP path membership alone is not mutation authority. Every top-level type in the source atom
 * must be an explicit pattern owner or pattern participant. Nested types inherit the admitted
 * top-level pattern boundary and remain inside the same file/contract atom.</p>
 *
 * <p>Structural participant markers keep their existing annotation-name independence, but
 * each Name/PartRole declaration must identify one canonical pattern and a nonblank literal
 * role. An invalid explicit declaration cannot borrow admission from another hook.</p>
 *
 * @implNote pattern=gof:strategy; role=Context for the Hook strategy interface and its four private
 *     ConcreteStrategies; algorithm=source fence, literal/cardinality validation, sorted hook-ID
 *     union and all-top-level-class conjunction; data.structures=immutable Hook list, owner-name
 *     set and immutable admission records/lists. Metadata admission is not semantic proof of
 *     participant behavior and grants no canonical promotion authority.
 */
public final class M3IopPatternClassHooks {
    private static final String PATTERN_PACKAGE_SEGMENT = "/com/synexia/iop/patterns/";

    private static final Set<String> PATTERN_OWNER_FILES = Set.of(
            "AIPattern.java",
            "AIPatterns.java",
            "AOPAdvice.java",
            "DAGPattern.java",
            "Distributed.java",
            "EIP.java",
            "GoF.java",
            "IAIPattern.java",
            "IAIPatterns.java",
            "IIOPPattern.java",
            "Microservice.java",
            "Pattern.java",
            "PatternSurvival.java",
            "Spring.java");

    /** One deterministic admission mechanism. */
    public interface Hook {
        String id();

        boolean admits(J.CompilationUnit unit, J.ClassDeclaration type);
    }

    /** Evidence for one top-level class. */
    public record ClassAdmission(
            String className,
            boolean admitted,
            List<String> hookIds) {

        public ClassAdmission {
            if (className == null || className.isBlank()) {
                throw new IllegalArgumentException("className required");
            }
            hookIds = hookIds == null
                    ? List.of()
                    : hookIds.stream().filter(Objects::nonNull).distinct().sorted().toList();
            if (admitted != !hookIds.isEmpty()) {
                throw new IllegalArgumentException("class admission/hook evidence mismatch");
            }
        }
    }

    /** File-atom admission. All top-level classes must be explicit pattern classes. */
    public record Admission(
            boolean admitted,
            List<ClassAdmission> classes,
            String reason) {

        public Admission {
            classes = classes == null ? List.of() : List.copyOf(classes);
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("reason required");
            }
            boolean all = !classes.isEmpty() && classes.stream().allMatch(ClassAdmission::admitted);
            if (admitted != all) {
                throw new IllegalArgumentException("unit admission/class evidence mismatch");
            }
        }
    }

    private static final List<Hook> HOOKS = List.of(
            new PatternPackageOwnerHook(),
            new CanonicalPatternOwnerFileHook(),
            new CanonicalPatternCatalogHook(),
            new DeclaredPatternRoleHook());

    private M3IopPatternClassHooks() {}

    public static Admission inspect(J.CompilationUnit unit) {
        Objects.requireNonNull(unit, "unit");
        Path sourcePath = unit.getSourcePath();
        if (!M3IopSourceFence.admits(sourcePath)) {
            return new Admission(false, List.of(), "NON_IOP_SOURCE");
        }

        List<J.ClassDeclaration> topLevel = unit.getClasses();
        if (topLevel.isEmpty()) {
            return new Admission(false, List.of(), "NO_TOP_LEVEL_PATTERN_CLASS");
        }

        List<ClassAdmission> classes = new ArrayList<>(topLevel.size());
        for (J.ClassDeclaration type : topLevel) {
            M3IopPatternCatalogHooks.Receipt catalog = M3IopPatternCatalogHooks.inspect(unit, type);
            boolean invalidRole = type.getLeadingAnnotations().stream()
                    .filter(M3IopPatternClassHooks::declaresPatternRole)
                    .anyMatch(annotation -> !validPatternRole(annotation));
            // An unresolved or malformed declaration vetoes every alternative admission mechanism.
            List<String> matched = catalog.residues().isEmpty() && !invalidRole ? HOOKS.stream()
                    .filter(hook -> hook instanceof CanonicalPatternCatalogHook
                            ? catalog.admitted() : hook.admits(unit, type))
                    .map(Hook::id)
                    .sorted()
                    .toList() : List.of();
            classes.add(new ClassAdmission(type.getSimpleName(), !matched.isEmpty(), matched));
        }

        List<ClassAdmission> stable = classes.stream()
                .sorted(Comparator.comparing(ClassAdmission::className))
                .toList();
        boolean admitted = stable.stream().allMatch(ClassAdmission::admitted);
        return new Admission(
                admitted,
                stable,
                admitted ? "ALL_TOP_LEVEL_CLASSES_PATTERN_ADMITTED" : "UNMARKED_TOP_LEVEL_CLASS");
    }

    public static boolean admits(J.CompilationUnit unit) {
        return inspect(unit).admitted();
    }

    public static List<String> hookIds() {
        return HOOKS.stream().map(Hook::id).toList();
    }

    private static final class PatternPackageOwnerHook implements Hook {
        @Override
        public String id() {
            return "IOP_PATTERN_PACKAGE_OWNER";
        }

        @Override
        public boolean admits(J.CompilationUnit unit, J.ClassDeclaration type) {
            return portable(unit.getSourcePath()).contains(PATTERN_PACKAGE_SEGMENT);
        }
    }

    private static final class CanonicalPatternOwnerFileHook implements Hook {
        @Override
        public String id() {
            return "IOP_CANONICAL_PATTERN_OWNER_FILE";
        }

        @Override
        public boolean admits(J.CompilationUnit unit, J.ClassDeclaration type) {
            Path sourcePath = unit.getSourcePath();
            Path file = sourcePath.getFileName();
            if (file == null || !PATTERN_OWNER_FILES.contains(file.toString())) {
                return false;
            }
            String path = portable(sourcePath);
            int slash = path.lastIndexOf('/');
            if (slash < 0) {
                return false;
            }
            String parent = path.substring(0, slash);
            return parent.endsWith("/com/synexia/iop")
                    || parent.equals("com/synexia/iop");
        }
    }

    private static final class CanonicalPatternCatalogHook implements Hook {
        @Override
        public String id() {
            return "IOP_CANONICAL_PATTERN_CATALOG";
        }

        @Override
        public boolean admits(J.CompilationUnit unit, J.ClassDeclaration type) {
            return M3IopPatternCatalogHooks.inspect(unit, type).admitted();
        }
    }

    private static final class DeclaredPatternRoleHook implements Hook {
        @Override
        public String id() {
            return "IOP_DECLARED_NAME_PARTROLE";
        }

        @Override
        public boolean admits(J.CompilationUnit unit, J.ClassDeclaration type) {
            return type.getLeadingAnnotations().stream()
                    .anyMatch(M3IopPatternClassHooks::declaresPatternRole);
        }
    }

    private static boolean declaresPatternRole(J.Annotation annotation) {
        List<Expression> arguments = annotation.getArguments();
        if (arguments == null || arguments.isEmpty()) {
            return false;
        }

        for (Expression argument : arguments) {
            if (argument instanceof J.Assignment assignment
                    && assignment.getVariable() instanceof J.Identifier identifier
                    && "PartRole".equals(identifier.getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    /** Single-pass literal/cardinality validation for the participant-hook strategy. */
    private static boolean validPatternRole(J.Annotation annotation) {
        int names = 0;
        int roles = 0;
        String name = null;
        String role = null;
        for (Expression argument : annotation.getArguments()) {
            if (argument instanceof J.Assignment assignment
                    && assignment.getVariable() instanceof J.Identifier identifier) {
                String literal = assignment.getAssignment() instanceof J.Literal value
                        && value.getValue() instanceof String text ? text : null;
                if ("Name".equals(identifier.getSimpleName())) {
                    names++;
                    name = literal;
                } else if ("PartRole".equals(identifier.getSimpleName())) {
                    roles++;
                    role = literal;
                }
            }
        }
        return names == 1 && roles == 1 && role != null && !role.isBlank()
                && M3IopPatternCatalogHooks.resolveParticipantPattern(name).isPresent();
    }

    private static String portable(Path sourcePath) {
        return sourcePath.normalize().toString().replace('\\', '/');
    }
}
