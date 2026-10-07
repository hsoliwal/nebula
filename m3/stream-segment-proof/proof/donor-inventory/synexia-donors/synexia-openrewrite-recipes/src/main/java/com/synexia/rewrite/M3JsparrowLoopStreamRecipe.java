// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/**
 * Native OpenRewrite implementation of the conservative jSparrow enhanced-for/stream family.
 *
 * <p>The implementation is original Apache-2.0 Synexia code. Public jSparrow sources and tests are
 * behavioral donors only. Transformations fail closed when attribution, lambda capture,
 * checked-exception, control-flow, ordering, or primitive-stream semantics are not proven by this
 * implementation. M3 compile/test/runtime admission remains authoritative.</p>
 */
public final class M3JsparrowLoopStreamRecipe extends Recipe {
    public enum Mode {
        ALL,
        TAKE_WHILE,
        FOR_EACH,
        MATCH,
        FIND_FIRST,
        SUM,
        STRING_BUILD,
        FLAT_MAP
    }

    @Option(
            displayName = "Source file pattern",
            description = "Glob limiting Java source candidates.",
            example = "**/*.java",
            required = false)
    private final String sourceFilePattern;

    @Option(
            displayName = "Mode",
            description = "One jSparrow loop-family operation or ALL.",
            example = "TAKE_WHILE",
            required = false)
    private final String mode;

    public M3JsparrowLoopStreamRecipe() {
        this("**/*.java", "ALL");
    }

    @JsonCreator
    public M3JsparrowLoopStreamRecipe(String sourceFilePattern, String mode) {
        this.sourceFilePattern = pattern(sourceFilePattern);
        this.mode = parseMode(mode).name();
    }

    @Override
    public String getDisplayName() {
        return "M3 jSparrow loop-to-stream superset";
    }

    @Override
    public String getDescription() {
        return "Ports conservative jSparrow enhanced-for stream transformations to native "
                + "OpenRewrite LST visitors while preserving sequential encounter order and "
                + "rejecting unproven capture/control-flow/exception cases.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "jsparrow",
                "openrewrite",
                "loop",
                "stream",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Mode selected = parseMode(mode);
        JavaVisitor<ExecutionContext> visitor =
                new JavaVisitor<>() {
                    @Override
                    public J visitBlock(J.Block block, ExecutionContext ctx) {
                        List<Statement> statements = block.getStatements();
                        if (statements.isEmpty()) {
                            return super.visitBlock(block, ctx);
                        }

                        J.MethodDeclaration method =
                                getCursor().firstEnclosing(J.MethodDeclaration.class);
                        Set<JavaType.Variable> modifiedLocals =
                                M3JsparrowLoopSafety.modifiedMethodLocals(method);

                        ArrayList<Statement> out = new ArrayList<>(statements.size());
                        for (int index = 0; index < statements.size(); index++) {
                            Statement statement = statements.get(index);
                            if (!(statement instanceof J.ForEachLoop loop)) {
                                out.add(statement);
                                continue;
                            }

                            LoopShape shape = loopShape(loop);
                            if (shape == null) {
                                out.add(statement);
                                continue;
                            }

                            Statement replacement = null;

                            if ((allows(selected, Mode.MATCH)
                                            || allows(selected, Mode.FIND_FIRST))
                                    && index + 1 < statements.size()
                                    && statements.get(index + 1)
                                            instanceof J.Return followingReturn) {
                                replacement =
                                        rewriteLoopAndFollowingReturn(
                                                loop,
                                                shape,
                                                followingReturn,
                                                modifiedLocals,
                                                selected);
                                if (replacement != null) {
                                    out.add(replacement);
                                    index++;
                                    continue;
                                }
                            }

                            if (index > 0
                                    && statements.get(index - 1)
                                            instanceof J.VariableDeclarations declaration) {
                                if (allows(selected, Mode.MATCH)) {
                                    replacement =
                                            rewriteFlagMatch(
                                                    loop,
                                                    shape,
                                                    declaration,
                                                    modifiedLocals);
                                }
                                if (replacement == null && allows(selected, Mode.SUM)) {
                                    replacement =
                                            rewriteSum(
                                                    loop,
                                                    shape,
                                                    declaration,
                                                    modifiedLocals);
                                }
                                if (replacement == null
                                        && allows(selected, Mode.STRING_BUILD)) {
                                    replacement =
                                            rewriteStringBuild(
                                                    loop,
                                                    shape,
                                                    declaration);
                                }
                                if (replacement != null) {
                                    out.add(replacement);
                                    continue;
                                }
                            }

                            if (allows(selected, Mode.TAKE_WHILE)) {
                                replacement =
                                        rewriteTakeWhile(loop, shape, modifiedLocals);
                            }
                            if (replacement == null && allows(selected, Mode.FLAT_MAP)) {
                                replacement =
                                        rewriteFlatMap(loop, shape, modifiedLocals);
                            }
                            if (replacement == null && allows(selected, Mode.FOR_EACH)) {
                                replacement =
                                        rewriteForEach(loop, shape, modifiedLocals);
                            }
                            out.add(replacement == null ? statement : replacement);
                        }

                        return super.visitBlock(block.withStatements(out), ctx);
                    }

                    private Statement rewriteLoopAndFollowingReturn(
                            J.ForEachLoop loop,
                            LoopShape shape,
                            J.Return followingReturn,
                            Set<JavaType.Variable> modifiedLocals,
                            Mode selectedMode) {
                        J.If condition = singleIf(loop.getBody());
                        if (condition == null
                                || condition.getElsePart() != null
                                || !commentsFree(condition)
                                || !followingReturn.getComments().isEmpty()) {
                            return null;
                        }

                        J.Return innerReturn = singleReturn(condition.getThenPart());
                        if (innerReturn == null
                                || innerReturn.getExpression() == null
                                || followingReturn.getExpression() == null
                                || !innerReturn.getComments().isEmpty()) {
                            return null;
                        }

                        Expression predicate = condition.getIfCondition().getTree();
                        if (!M3JsparrowLoopSafety.safeExpression(
                                predicate,
                                shape.variableName(),
                                modifiedLocals)) {
                            return null;
                        }

                        Boolean innerBoolean = booleanLiteral(innerReturn.getExpression());
                        Boolean followingBoolean =
                                booleanLiteral(followingReturn.getExpression());
                        if (allows(selectedMode, Mode.MATCH)
                                && innerBoolean != null
                                && followingBoolean != null
                                && !innerBoolean.equals(followingBoolean)) {
                            String terminal = innerBoolean ? "anyMatch" : "noneMatch";
                            String source =
                                    pipelineBase(shape)
                                            + "."
                                            + terminal
                                            + "("
                                            + shape.variableName()
                                            + " -> "
                                            + print(predicate, loop)
                                            + ")";
                            return replacement(loop, "return " + source + ";");
                        }

                        if (!allows(selectedMode, Mode.FIND_FIRST)
                                || !isSafePrimitiveValue(innerReturn.getExpression())
                                || !M3JsparrowLoopSafety.safeExpression(
                                        innerReturn.getExpression(),
                                        shape.variableName(),
                                        modifiedLocals)
                                || !M3JsparrowLoopSafety.safeExpression(
                                        followingReturn.getExpression(),
                                        shape.variableName(),
                                        modifiedLocals)) {
                            return null;
                        }

                        String value = print(innerReturn.getExpression(), loop);
                        if (!value.equals(shape.variableName())) {
                            return null;
                        }

                        String source =
                                pipelineBase(shape)
                                        + ".filter("
                                        + shape.variableName()
                                        + " -> "
                                        + print(predicate, loop)
                                        + ")"
                                        + ".findFirst().orElse("
                                        + print(followingReturn.getExpression(), loop)
                                        + ")";
                        return replacement(loop, "return " + source + ";");
                    }

                    private Statement rewriteFlagMatch(
                            J.ForEachLoop loop,
                            LoopShape shape,
                            J.VariableDeclarations declaration,
                            Set<JavaType.Variable> modifiedLocals) {
                        J.If condition = singleIf(loop.getBody());
                        if (condition == null
                                || condition.getElsePart() != null
                                || !commentsFree(condition)) {
                            return null;
                        }

                        AssignBreak assignBreak =
                                assignmentAndBreak(condition.getThenPart());
                        if (assignBreak == null) {
                            return null;
                        }

                        DeclaredVariable declared =
                                findDeclared(declaration, assignBreak.targetName());
                        Boolean initial =
                                declared == null
                                        ? null
                                        : booleanLiteral(declared.initializer());
                        Boolean assigned =
                                booleanLiteral(
                                        assignBreak.assignment().getAssignment());
                        if (initial == null
                                || assigned == null
                                || initial.equals(assigned)
                                || !M3JsparrowLoopSafety.safeExpression(
                                        condition.getIfCondition().getTree(),
                                        shape.variableName(),
                                        modifiedLocals)) {
                            return null;
                        }

                        String terminal = !initial && assigned ? "anyMatch" : "noneMatch";
                        String source =
                                pipelineBase(shape)
                                        + "."
                                        + terminal
                                        + "("
                                        + shape.variableName()
                                        + " -> "
                                        + print(
                                                condition.getIfCondition().getTree(),
                                                loop)
                                        + ")";
                        return replacement(
                                loop,
                                assignBreak.targetName() + " = " + source + ";");
                    }

                    private Statement rewriteSum(
                            J.ForEachLoop loop,
                            LoopShape shape,
                            J.VariableDeclarations declaration,
                            Set<JavaType.Variable> modifiedLocals) {
                        Statement body = singleBodyStatement(loop.getBody());
                        Accumulator accumulator =
                                body == null ? null : accumulator(body);
                        if (accumulator == null
                                || !M3JsparrowLoopSafety.safeExpression(
                                        accumulator.valueExpression(),
                                        shape.variableName(),
                                        modifiedLocals)) {
                            return null;
                        }

                        DeclaredVariable declared =
                                findDeclared(declaration, accumulator.targetName());
                        if (declared == null || !numericZero(declared.initializer())) {
                            return null;
                        }

                        NumericStream numeric = numericStream(declared.type());
                        if (numeric == null) {
                            return null;
                        }

                        String source;
                        if (languagePrimitive(shape.variableType())) {
                            if (!isIdentifier(
                                            accumulator.valueExpression(),
                                            shape.variableName())
                                    || primitiveStream(shape.variableType()) == null) {
                                return null;
                            }
                            source = pipelineBase(shape) + ".sum()";
                        } else {
                            source =
                                    pipelineBaseReference(shape)
                                            + "."
                                            + numeric.mapper()
                                            + "("
                                            + shape.variableName()
                                            + " -> "
                                            + print(
                                                    accumulator.valueExpression(),
                                                    loop)
                                            + ").sum()";
                        }
                        return replacement(
                                loop,
                                accumulator.targetName() + " = " + source + ";");
                    }

                    private Statement rewriteStringBuild(
                            J.ForEachLoop loop,
                            LoopShape shape,
                            J.VariableDeclarations declaration) {
                        if (!isString(shape.variableType())) {
                            return null;
                        }

                        Statement body = singleBodyStatement(loop.getBody());
                        Accumulator accumulator =
                                body == null ? null : accumulator(body);
                        if (accumulator == null
                                || !isIdentifier(
                                        accumulator.valueExpression(),
                                        shape.variableName())) {
                            return null;
                        }

                        DeclaredVariable declared =
                                findDeclared(declaration, accumulator.targetName());
                        if (declared == null
                                || !isString(declared.type())
                                || !(declared.initializer() instanceof J.Literal literal)
                                || !(literal.getValue() instanceof String)) {
                            return null;
                        }

                        String joined =
                                pipelineBaseReference(shape)
                                        + ".collect(java.util.stream.Collectors.joining())";
                        return replacement(
                                loop,
                                accumulator.targetName()
                                        + " = "
                                        + accumulator.targetName()
                                        + " + "
                                        + joined
                                        + ";");
                    }

                    private Statement rewriteTakeWhile(
                            J.ForEachLoop loop,
                            LoopShape shape,
                            Set<JavaType.Variable> modifiedLocals) {
                        if (!(loop.getBody() instanceof J.Block body)
                                || body.getStatements().size() < 2) {
                            return null;
                        }

                        Statement first = body.getStatements().get(0);
                        if (!(first instanceof J.If condition)
                                || condition.getElsePart() != null
                                || !singleUnlabeledBreak(condition.getThenPart())
                                || !commentsFree(condition)
                                || !M3JsparrowLoopSafety.safeExpression(
                                        condition.getIfCondition().getTree(),
                                        shape.variableName(),
                                        modifiedLocals)) {
                            return null;
                        }

                        List<Statement> remaining =
                                body.getStatements()
                                        .subList(1, body.getStatements().size());
                        if (!M3JsparrowLoopSafety.safeStatements(
                                remaining,
                                shape.variableName(),
                                modifiedLocals)) {
                            return null;
                        }

                        Expression stopExpression =
                                condition.getIfCondition().getTree();
                        Expression unwrapped = stopExpression.unwrap();
                        String predicate =
                                unwrapped instanceof J.Unary unary
                                                && unary.getOperator()
                                                        == J.Unary.Type.Not
                                        ? print(unary.getExpression(), loop)
                                        : "!(" + print(stopExpression, loop) + ")";

                        String source =
                                pipelineBase(shape)
                                        + ".takeWhile("
                                        + shape.variableName()
                                        + " -> "
                                        + predicate
                                        + ")"
                                        + ".forEachOrdered("
                                        + shape.variableName()
                                        + " -> "
                                        + lambdaBody(remaining, loop)
                                        + ");";
                        return replacement(loop, source);
                    }

                    private Statement rewriteFlatMap(
                            J.ForEachLoop loop,
                            LoopShape outer,
                            Set<JavaType.Variable> modifiedLocals) {
                        J.ForEachLoop inner = singleNestedLoop(loop.getBody());
                        if (inner == null || languagePrimitive(outer.variableType())) {
                            return null;
                        }

                        LoopShape innerShape = loopShape(inner);
                        if (innerShape == null
                                || languagePrimitive(innerShape.variableType())
                                || M3JsparrowLoopSafety.usesVariable(
                                        inner.getBody(),
                                        outer.variableTypeBinding())
                                || !M3JsparrowLoopSafety.safeExpression(
                                        inner.getControl().getIterable(),
                                        outer.variableName(),
                                        modifiedLocals)) {
                            return null;
                        }

                        List<Statement> innerBody =
                                bodyStatements(inner.getBody());
                        if (innerBody.isEmpty()
                                || !M3JsparrowLoopSafety.safeStatements(
                                        innerBody,
                                        innerShape.variableName(),
                                        modifiedLocals)) {
                            return null;
                        }

                        String source =
                                pipelineBaseReference(outer)
                                        + ".flatMap("
                                        + outer.variableName()
                                        + " -> "
                                        + pipelineBaseReference(innerShape)
                                        + ")"
                                        + ".forEachOrdered("
                                        + innerShape.variableName()
                                        + " -> "
                                        + lambdaBody(innerBody, inner)
                                        + ");";
                        return replacement(loop, source);
                    }

                    private Statement rewriteForEach(
                            J.ForEachLoop loop,
                            LoopShape shape,
                            Set<JavaType.Variable> modifiedLocals) {
                        List<Statement> body = bodyStatements(loop.getBody());
                        if (body.isEmpty()
                                || !M3JsparrowLoopSafety.safeStatements(
                                        body,
                                        shape.variableName(),
                                        modifiedLocals)) {
                            return null;
                        }

                        String source =
                                pipelineBase(shape)
                                        + ".forEachOrdered("
                                        + shape.variableName()
                                        + " -> "
                                        + lambdaBody(body, loop)
                                        + ");";
                        return replacement(loop, source);
                    }

                    private LoopShape loopShape(J.ForEachLoop loop) {
                        J.VariableDeclarations declarations =
                                loop.getControl().getVariable();
                        if (declarations.getVariables().size() != 1) {
                            return null;
                        }

                        J.VariableDeclarations.NamedVariable variable =
                                declarations.getVariables().get(0);
                        if (variable.getVariableType() == null) {
                            return null;
                        }

                        Expression iterable = loop.getControl().getIterable();
                        if (iterable instanceof J.Ternary
                                || iterable.getType() == null
                                || !TypeUtils.isAssignableTo(
                                        "java.util.Collection",
                                        iterable.getType())
                                || concurrentCollection(iterable.getType())) {
                            return null;
                        }

                        JavaType variableType =
                                variable.getVariableType().getType();
                        if (languagePrimitive(variableType)
                                && primitiveStream(variableType) == null) {
                            return null;
                        }

                        return new LoopShape(
                                print(iterable, loop),
                                variable.getSimpleName(),
                                variable.getVariableType(),
                                variableType);
                    }

                    private String pipelineBase(LoopShape shape) {
                        PrimitiveStream primitive =
                                primitiveStream(shape.variableType());
                        if (primitive == null) {
                            return pipelineBaseReference(shape);
                        }
                        return pipelineBaseReference(shape)
                                + "."
                                + primitive.mapper()
                                + "("
                                + shape.variableName()
                                + " -> "
                                + shape.variableName()
                                + ")";
                    }

                    private String pipelineBaseReference(LoopShape shape) {
                        return shape.sourceExpression() + ".stream()";
                    }

                    private Statement replacement(
                            Statement anchor,
                            String code) {
                        Cursor anchorCursor = new Cursor(getCursor(), anchor);
                        Statement result =
                                JavaTemplate.builder(code)
                                        .contextSensitive()
                                        .build()
                                        .apply(
                                                anchorCursor,
                                                anchor.getCoordinates().replace());
                        return result.withPrefix(anchor.getPrefix());
                    }

                    private String lambdaBody(
                            List<Statement> statements,
                            J parent) {
                        J.Block body = parent instanceof J.ForEachLoop loop
                                        && loop.getBody() instanceof J.Block original
                                ? original.withStatements(statements)
                                : J.Block.createEmptyBlock().withStatements(statements);
                        return print(body, parent);
                    }

                    private String print(J tree, J parent) {
                        return tree.printTrimmed(
                                new Cursor(new Cursor(getCursor(), parent), tree));
                    }

                    private J.If singleIf(Statement body) {
                        if (body instanceof J.If condition) {
                            return condition;
                        }
                        if (body instanceof J.Block nested
                                && nested.getStatements().size() == 1
                                && nested.getStatements().get(0)
                                        instanceof J.If condition) {
                            return condition;
                        }
                        return null;
                    }

                    private J.Return singleReturn(Statement body) {
                        if (body instanceof J.Return result) {
                            return result;
                        }
                        if (body instanceof J.Block nested
                                && nested.getStatements().size() == 1
                                && nested.getStatements().get(0)
                                        instanceof J.Return result) {
                            return result;
                        }
                        return null;
                    }

                    private Statement singleBodyStatement(Statement body) {
                        if (body instanceof J.Block nested) {
                            return nested.getStatements().size() == 1
                                    ? nested.getStatements().get(0)
                                    : null;
                        }
                        return body;
                    }

                    private J.ForEachLoop singleNestedLoop(Statement body) {
                        Statement single = singleBodyStatement(body);
                        return single instanceof J.ForEachLoop nested
                                ? nested
                                : null;
                    }

                    private List<Statement> bodyStatements(Statement body) {
                        return body instanceof J.Block nested
                                ? nested.getStatements()
                                : List.of(body);
                    }

                    private boolean singleUnlabeledBreak(Statement body) {
                        if (body instanceof J.Break breakStatement) {
                            return breakStatement.getLabel() == null
                                    && breakStatement.getComments().isEmpty();
                        }
                        return body instanceof J.Block nested
                                && nested.getStatements().size() == 1
                                && nested.getStatements().get(0)
                                        instanceof J.Break breakStatement
                                && breakStatement.getLabel() == null
                                && breakStatement.getComments().isEmpty();
                    }

                    private boolean commentsFree(J.If condition) {
                        return condition.getComments().isEmpty()
                                && condition.getThenPart()
                                        .getComments()
                                        .isEmpty();
                    }

                    private AssignBreak assignmentAndBreak(Statement body) {
                        if (!(body instanceof J.Block nested)
                                || nested.getStatements().size() != 2
                                || !nested.getComments().isEmpty()) {
                            return null;
                        }
                        if (!(nested.getStatements().get(0)
                                        instanceof J.Assignment assignment)
                                || !(nested.getStatements().get(1)
                                        instanceof J.Break breakStatement)
                                || breakStatement.getLabel() != null
                                || !assignment.getComments().isEmpty()
                                || !breakStatement.getComments().isEmpty()) {
                            return null;
                        }
                        String target =
                                identifierName(assignment.getVariable());
                        return target == null
                                ? null
                                : new AssignBreak(target, assignment);
                    }

                    private Accumulator accumulator(Statement statement) {
                        if (statement
                                        instanceof J.AssignmentOperation operation
                                && operation.getOperator()
                                        == J.AssignmentOperation.Type.Addition) {
                            String target =
                                    identifierName(operation.getVariable());
                            return target == null
                                    ? null
                                    : new Accumulator(
                                            target,
                                            operation.getAssignment());
                        }
                        if (statement instanceof J.Assignment assignment
                                && assignment.getAssignment()
                                        instanceof J.Binary binary
                                && binary.getOperator()
                                        == J.Binary.Type.Addition) {
                            String target =
                                    identifierName(assignment.getVariable());
                            if (target == null) {
                                return null;
                            }
                            if (isIdentifier(binary.getLeft(), target)) {
                                return new Accumulator(
                                        target,
                                        binary.getRight());
                            }
                            if (isIdentifier(binary.getRight(), target)) {
                                return new Accumulator(
                                        target,
                                        binary.getLeft());
                            }
                        }
                        return null;
                    }

                    private DeclaredVariable findDeclared(
                            J.VariableDeclarations declaration,
                            String name) {
                        for (J.VariableDeclarations.NamedVariable variable :
                                declaration.getVariables()) {
                            if (variable.getSimpleName().equals(name)) {
                                return new DeclaredVariable(
                                        variable.getInitializer(),
                                        variable.getVariableType() == null
                                                ? null
                                                : variable.getVariableType()
                                                        .getType());
                            }
                        }
                        return null;
                    }

                    private Boolean booleanLiteral(Expression expression) {
                        if (expression instanceof J.Literal literal
                                && literal.getValue()
                                        instanceof Boolean value) {
                            return value;
                        }
                        return null;
                    }

                    private boolean numericZero(Expression expression) {
                        return expression instanceof J.Literal literal
                                && literal.getValue() instanceof Number number
                                && number.doubleValue() == 0.0d;
                    }

                    private boolean isSafePrimitiveValue(
                            Expression expression) {
                        JavaType type = expression.getType();
                        return type instanceof JavaType.Primitive primitive
                                && primitive != JavaType.Primitive.String
                                && primitive != JavaType.Primitive.Void
                                && primitive != JavaType.Primitive.None
                                && primitive != JavaType.Primitive.Null;
                    }

                    private String identifierName(Expression expression) {
                        Expression unwrapped = expression.unwrap();
                        return unwrapped instanceof J.Identifier identifier
                                ? identifier.getSimpleName()
                                : null;
                    }

                    private boolean isIdentifier(
                            Expression expression,
                            String expected) {
                        return expected.equals(identifierName(expression));
                    }

                    private boolean concurrentCollection(JavaType type) {
                        JavaType.FullyQualified fullyQualified =
                                TypeUtils.asFullyQualified(type);
                        return fullyQualified != null
                                && fullyQualified
                                        .getFullyQualifiedName()
                                        .startsWith("java.util.concurrent.");
                    }

                    private boolean isString(JavaType type) {
                        return type == JavaType.Primitive.String
                                || TypeUtils.isOfClassType(
                                        type,
                                        "java.lang.String");
                    }

                    private boolean languagePrimitive(JavaType type) {
                        return type instanceof JavaType.Primitive primitive
                                && primitive != JavaType.Primitive.String;
                    }

                    private PrimitiveStream primitiveStream(JavaType type) {
                        if (type == JavaType.Primitive.Int) {
                            return new PrimitiveStream("mapToInt");
                        }
                        if (type == JavaType.Primitive.Long) {
                            return new PrimitiveStream("mapToLong");
                        }
                        if (type == JavaType.Primitive.Double) {
                            return new PrimitiveStream("mapToDouble");
                        }
                        return null;
                    }

                    private NumericStream numericStream(JavaType type) {
                        if (type == JavaType.Primitive.Int
                                || TypeUtils.isOfClassType(
                                        type,
                                        "java.lang.Integer")) {
                            return new NumericStream("mapToInt");
                        }
                        if (type == JavaType.Primitive.Long
                                || TypeUtils.isOfClassType(
                                        type,
                                        "java.lang.Long")) {
                            return new NumericStream("mapToLong");
                        }
                        return null;
                    }
                };

        return Preconditions.check(
                new FindSourceFiles(sourceFilePattern).getVisitor(),
                visitor);
    }

    public String getSourceFilePattern() {
        return sourceFilePattern;
    }

    public String getMode() {
        return mode;
    }

    public boolean sourceCopyAuthority() {
        return false;
    }

    public boolean semanticEquivalenceAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    private static boolean allows(Mode selected, Mode candidate) {
        return selected == Mode.ALL || selected == candidate;
    }

    private static Mode parseMode(String value) {
        String checked =
                Objects.requireNonNullElse(value, "ALL")
                        .strip()
                        .toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_');
        try {
            return Mode.valueOf(checked);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("mode", invalid);
        }
    }

    private static String pattern(String value) {
        String checked =
                Objects.requireNonNullElse(value, "**/*.java").strip();
        if (checked.isEmpty()
                || checked.length() > 4096
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("sourceFilePattern");
        }
        return checked;
    }

    private record LoopShape(
            String sourceExpression,
            String variableName,
            JavaType.Variable variableTypeBinding,
            JavaType variableType) {}

    private record AssignBreak(
            String targetName,
            J.Assignment assignment) {}

    private record DeclaredVariable(
            Expression initializer,
            JavaType type) {}

    private record Accumulator(
            String targetName,
            Expression valueExpression) {}

    private record PrimitiveStream(String mapper) {}

    private record NumericStream(String mapper) {}
}
