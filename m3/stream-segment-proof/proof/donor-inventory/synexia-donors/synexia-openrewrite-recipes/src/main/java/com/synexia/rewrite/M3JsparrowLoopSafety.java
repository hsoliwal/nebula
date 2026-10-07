// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/** Shared conservative safety analysis for loop-to-functional OpenRewrite recipes. */
final class M3JsparrowLoopSafety {
    private M3JsparrowLoopSafety() {}

    static Set<JavaType.Variable> modifiedMethodLocals(J.MethodDeclaration method) {
        HashSet<JavaType.Variable> result = new HashSet<>();
        if (method == null || method.getBody() == null) {
            return Set.of();
        }
        new JavaIsoVisitor<Set<JavaType.Variable>>() {
            @Override
            public J.Assignment visitAssignment(
                    J.Assignment assignment,
                    Set<JavaType.Variable> variables) {
                addMethodLocal(assignment.getVariable(), variables);
                return super.visitAssignment(assignment, variables);
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(
                    J.AssignmentOperation operation,
                    Set<JavaType.Variable> variables) {
                addMethodLocal(operation.getVariable(), variables);
                return super.visitAssignmentOperation(operation, variables);
            }

            @Override
            public J.Unary visitUnary(
                    J.Unary unary,
                    Set<JavaType.Variable> variables) {
                if (unary.getOperator().isModifying()) {
                    addMethodLocal(unary.getExpression(), variables);
                }
                return super.visitUnary(unary, variables);
            }
        }.visit(method.getBody(), result);
        return Set.copyOf(result);
    }

    static boolean safeExpression(
            Expression expression,
            String loopVariable,
            Set<JavaType.Variable> modifiedLocals) {
        SafetyState state = new SafetyState(loopVariable, Set.of(), modifiedLocals, false);
        new SafetyVisitor().visit(expression, state);
        return !state.unsafe;
    }

    static boolean safeStatements(
            List<Statement> statements,
            String loopVariable,
            Set<JavaType.Variable> modifiedLocals) {
        HashSet<JavaType.Variable> declaredInside = new HashSet<>();
        JavaIsoVisitor<Set<JavaType.Variable>> collector =
                new JavaIsoVisitor<>() {
                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(
                            J.VariableDeclarations.NamedVariable variable,
                            Set<JavaType.Variable> variables) {
                        if (variable.getVariableType() != null) {
                            variables.add(variable.getVariableType());
                        }
                        return super.visitVariable(variable, variables);
                    }
                };
        for (Statement statement : statements) {
            collector.visit(statement, declaredInside);
        }

        SafetyState state =
                new SafetyState(loopVariable, declaredInside, modifiedLocals, true);
        SafetyVisitor visitor = new SafetyVisitor();
        for (Statement statement : statements) {
            visitor.visit(statement, state);
            if (state.unsafe) {
                return false;
            }
        }
        return true;
    }

    static boolean usesVariable(Statement statement, JavaType.Variable target) {
        if (target == null) {
            return true;
        }
        boolean[] found = {false};
        new JavaIsoVisitor<boolean[]>() {
            @Override
            public J.Identifier visitIdentifier(
                    J.Identifier identifier,
                    boolean[] state) {
                if (target.equals(identifier.getFieldType())) {
                    state[0] = true;
                }
                return super.visitIdentifier(identifier, state);
            }
        }.visit(statement, found);
        return found[0];
    }

    private static void addMethodLocal(
            Expression expression,
            Set<JavaType.Variable> variables) {
        JavaType.Variable variable = identifierVariable(expression);
        if (methodLocal(variable)) {
            variables.add(variable);
        }
    }

    private static JavaType.Variable identifierVariable(Expression expression) {
        Expression unwrapped = expression.unwrap();
        return unwrapped instanceof J.Identifier identifier
                ? identifier.getFieldType()
                : null;
    }

    private static boolean methodLocal(JavaType.Variable variable) {
        return variable != null && variable.getOwner() instanceof JavaType.Method;
    }

    private static boolean checkedException(JavaType type) {
        if (type == null) {
            return true;
        }
        if (TypeUtils.isAssignableTo("java.lang.RuntimeException", type)
                || TypeUtils.isAssignableTo("java.lang.Error", type)) {
            return false;
        }
        return TypeUtils.isAssignableTo("java.lang.Exception", type)
                || TypeUtils.isAssignableTo("java.lang.Throwable", type);
    }

    private static final class SafetyVisitor extends JavaIsoVisitor<SafetyState> {
        @Override
        public J.Break visitBreak(J.Break breakStatement, SafetyState state) {
            if (state.rejectControl) {
                state.unsafe = true;
            }
            return breakStatement;
        }

        @Override
        public J.Continue visitContinue(J.Continue continueStatement, SafetyState state) {
            if (state.rejectControl) {
                state.unsafe = true;
            }
            return continueStatement;
        }

        @Override
        public J.Return visitReturn(J.Return returnStatement, SafetyState state) {
            if (state.rejectControl) {
                state.unsafe = true;
            }
            return returnStatement;
        }

        @Override
        public J.Throw visitThrow(J.Throw thrown, SafetyState state) {
            state.unsafe = true;
            return thrown;
        }

        @Override
        public J.Try visitTry(J.Try tryStatement, SafetyState state) {
            state.unsafe = true;
            return tryStatement;
        }

        @Override
        public J.Synchronized visitSynchronized(
                J.Synchronized synchronizedStatement,
                SafetyState state) {
            state.unsafe = true;
            return synchronizedStatement;
        }

        @Override
        public J.ForEachLoop visitForEachLoop(
                J.ForEachLoop nested,
                SafetyState state) {
            if (state.rejectControl) {
                state.unsafe = true;
            }
            return nested;
        }

        @Override
        public J.ForLoop visitForLoop(J.ForLoop nested, SafetyState state) {
            if (state.rejectControl) {
                state.unsafe = true;
            }
            return nested;
        }

        @Override
        public J.WhileLoop visitWhileLoop(J.WhileLoop nested, SafetyState state) {
            if (state.rejectControl) {
                state.unsafe = true;
            }
            return nested;
        }

        @Override
        public J.DoWhileLoop visitDoWhileLoop(
                J.DoWhileLoop nested,
                SafetyState state) {
            if (state.rejectControl) {
                state.unsafe = true;
            }
            return nested;
        }

        @Override
        public J.Assignment visitAssignment(
                J.Assignment assignment,
                SafetyState state) {
            rejectCapturedMutation(assignment.getVariable(), state);
            return super.visitAssignment(assignment, state);
        }

        @Override
        public J.AssignmentOperation visitAssignmentOperation(
                J.AssignmentOperation operation,
                SafetyState state) {
            rejectCapturedMutation(operation.getVariable(), state);
            return super.visitAssignmentOperation(operation, state);
        }

        @Override
        public J.Unary visitUnary(J.Unary unary, SafetyState state) {
            if (unary.getOperator().isModifying()) {
                rejectCapturedMutation(unary.getExpression(), state);
            }
            return super.visitUnary(unary, state);
        }

        @Override
        public J.Identifier visitIdentifier(
                J.Identifier identifier,
                SafetyState state) {
            JavaType.Variable variable = identifier.getFieldType();
            if (methodLocal(variable)
                    && !state.declaredInside.contains(variable)
                    && !identifier.getSimpleName().equals(state.loopVariable)
                    && state.modifiedLocals.contains(variable)) {
                state.unsafe = true;
            }
            return super.visitIdentifier(identifier, state);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(
                J.MethodInvocation method,
                SafetyState state) {
            JavaType.Method methodType = method.getMethodType();
            if (methodType == null) {
                state.unsafe = true;
                return method;
            }
            for (JavaType thrown : methodType.getThrownExceptions()) {
                if (checkedException(thrown)) {
                    state.unsafe = true;
                    return method;
                }
            }
            return super.visitMethodInvocation(method, state);
        }

        private void rejectCapturedMutation(
                Expression expression,
                SafetyState state) {
            JavaType.Variable variable = identifierVariable(expression);
            if (methodLocal(variable) && !state.declaredInside.contains(variable)) {
                state.unsafe = true;
            }
        }
    }

    private static final class SafetyState {
        private final String loopVariable;
        private final Set<JavaType.Variable> declaredInside;
        private final Set<JavaType.Variable> modifiedLocals;
        private final boolean rejectControl;
        private boolean unsafe;

        private SafetyState(
                String loopVariable,
                Set<JavaType.Variable> declaredInside,
                Set<JavaType.Variable> modifiedLocals,
                boolean rejectControl) {
            this.loopVariable = loopVariable;
            this.declaredInside = declaredInside;
            this.modifiedLocals = modifiedLocals;
            this.rejectControl = rejectControl;
        }
    }
}
