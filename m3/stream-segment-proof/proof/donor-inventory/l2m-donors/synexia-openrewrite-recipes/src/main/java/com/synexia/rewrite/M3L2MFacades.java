// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

/** Eligibility and symbol-preserving transplantation for the finite Function facade contract. */
final class M3L2MFacades {
    private M3L2MFacades() {}

    static boolean safeInitialization(J.ClassDeclaration owner) {
        if (owner.getKind() != J.ClassDeclaration.Kind.Type.Class || owner.getExtends() != null
                || owner.getImplements() != null && !owner.getImplements().isEmpty()) return false;
        for (Statement member : owner.getBody().getStatements()) {
            if (member instanceof J.Block block && block.isStatic()) return false;
            if (!(member instanceof J.VariableDeclarations fields)
                    || !fields.hasModifier(J.Modifier.Type.Static)) continue;
            for (J.VariableDeclarations.NamedVariable field : fields.getVariables()) {
                Expression initializer = field.getInitializer();
                // Executable static initialization can call a facade before its lambda exists.
                if (initializer != null && !(initializer instanceof J.Literal)
                        && (!(initializer instanceof J.Lambda)
                            || !TypeUtils.isOfClassType(field.getType(), "java.util.function.Function"))) return false;
            }
        }
        return true;
    }

    static J.MethodDeclaration unfold(J.VariableDeclarations field, J.MethodDeclaration method, J.CompilationUnit unit) {
        if (!field.hasModifier(J.Modifier.Type.Private) || !field.hasModifier(J.Modifier.Type.Static)
                || !field.hasModifier(J.Modifier.Type.Final) || !field.getLeadingAnnotations().isEmpty()
                || field.getVariables().size() != 1 || !field.getPrefix().getComments().isEmpty()
                || !method.hasModifier(J.Modifier.Type.Static) || method.getBody() == null
                || method.getParameters().size() != 1 || method.getTypeParameters() != null
                    && !method.getTypeParameters().isEmpty()
                || method.getBody().getStatements().size() != 1) return method;
        J.VariableDeclarations.NamedVariable variable = field.getVariables().getFirst();
        if (variable.getVariableType() == null || !(variable.getInitializer() instanceof J.Lambda lambda)
                || !(variable.getType() instanceof JavaType.Parameterized function)
                || !TypeUtils.isOfClassType(function, "java.util.function.Function")
                || function.getTypeParameters().size() != 2
                || !concrete(function.getTypeParameters().get(0)) || !concrete(function.getTypeParameters().get(1))
                || scaffoldComments(field, lambda.getBody())) return method;
        if (!(method.getParameters().getFirst() instanceof J.VariableDeclarations parameter)
                || parameter.getVariables().size() != 1 || parameter.getVarargs() != null
                || !(method.getBody().getStatements().getFirst() instanceof J.Return returned)
                || !(returned.getExpression() instanceof J.MethodInvocation call)
                || !(call.getSelect() instanceof J.Identifier selected)
                || !variable.getVariableType().equals(selected.getFieldType())
                || !call.getSimpleName().equals("apply") || call.getArguments().size() != 1
                || M3SemanticAttribution.resolvedMethod(call.getMethodType()) == null
                || !TypeUtils.isOfClassType(call.getMethodType().getDeclaringType(), "java.util.function.Function")
                || !(call.getArguments().getFirst() instanceof J.Identifier argument)
                || !returned.getPrefix().getComments().isEmpty()
                || scaffoldComments(call, null)) return method;
        J.VariableDeclarations.NamedVariable target = parameter.getVariables().getFirst();
        J.Identifier source = lambdaParameter(lambda);
        JavaType.Method signature = M3SemanticAttribution.resolvedMethod(method.getMethodType());
        if (source == null || source.getFieldType() == null || target.getVariableType() == null || signature == null
                || !source.getSimpleName().equals(target.getSimpleName())
                || !target.getVariableType().equals(argument.getFieldType())
                || !TypeUtils.isOfType(source.getType(), target.getType())
                || !TypeUtils.isOfType(function.getTypeParameters().get(0), target.getType())
                || !TypeUtils.isOfType(function.getTypeParameters().get(1), signature.getReturnType())
                || !TypeUtils.isOfType(call.getType(), signature.getReturnType())
                || uses(unit, variable.getVariableType()) != 1) return method;

        Map<JavaType.Variable, JavaType.Variable> symbols = new HashMap<>();
        symbols.put(source.getFieldType(), target.getVariableType());
        new JavaIsoVisitor<Map<JavaType.Variable, JavaType.Variable>>() {
            @Override public J.VariableDeclarations.NamedVariable visitVariable(
                    J.VariableDeclarations.NamedVariable local, Map<JavaType.Variable, JavaType.Variable> bindings) {
                if (local.getVariableType() != null && getCursor().firstEnclosing(J.Lambda.class) == null
                        && getCursor().firstEnclosing(J.ClassDeclaration.class) == null) {
                    bindings.put(local.getVariableType(), local.getVariableType().withOwner(signature));
                }
                return super.visitVariable(local, bindings);
            }
        }.visit(lambda.getBody(), symbols);
        J transplanted = new JavaIsoVisitor<Map<JavaType.Variable, JavaType.Variable>>() {
            @Override public J.Identifier visitIdentifier(J.Identifier identifier, Map<JavaType.Variable, JavaType.Variable> bindings) {
                J.Identifier visited = super.visitIdentifier(identifier, bindings);
                JavaType.Variable replacement = bindings.get(visited.getFieldType());
                return replacement == null ? visited : visited.withFieldType(replacement).withType(replacement.getType());
            }
            @Override public J.VariableDeclarations.NamedVariable visitVariable(
                    J.VariableDeclarations.NamedVariable local, Map<JavaType.Variable, JavaType.Variable> bindings) {
                J.VariableDeclarations.NamedVariable visited = super.visitVariable(local, bindings);
                JavaType.Variable replacement = bindings.get(local.getVariableType());
                return replacement == null ? visited : visited.withVariableType(replacement);
            }
        }.visit(lambda.getBody(), symbols);
        J.Block body = method.getBody();
        if (transplanted instanceof J.Block block) {
            // Preserve both original closing comment lanes; whitespace is formatted by the owner.
            java.util.ArrayList<org.openrewrite.java.tree.Comment> comments = new java.util.ArrayList<>(block.getEnd().getComments());
            comments.addAll(body.getEnd().getComments());
            java.util.ArrayList<org.openrewrite.java.tree.Comment> prefix = new java.util.ArrayList<>(body.getPrefix().getComments());
            prefix.addAll(block.getPrefix().getComments());
            return method.withBody(body.withPrefix(Space.build(body.getPrefix().getWhitespace(), prefix))
                    .withStatements(block.getStatements())
                    .withEnd(Space.build(body.getEnd().getWhitespace(), comments)));
        }
        return transplanted instanceof Expression expression
                ? method.withBody(body.withStatements(List.of(returned.withExpression(expression.withPrefix(
                    Space.build(" ", expression.getPrefix().getComments())))))) : method;
    }

    private static J.Identifier lambdaParameter(J.Lambda lambda) {
        if (lambda.getParameters().getParameters().size() != 1) return null;
        J parameter = lambda.getParameters().getParameters().getFirst();
        if (parameter instanceof J.Identifier identifier) return identifier;
        return parameter instanceof J.VariableDeclarations declaration && declaration.getVariables().size() == 1
                ? declaration.getVariables().getFirst().getName() : null;
    }

    private static boolean concrete(JavaType type) {
        if (!M3SemanticAttribution.isResolvedType(type) || type instanceof JavaType.GenericTypeVariable) return false;
        if (type instanceof JavaType.Parameterized parameterized)
            return parameterized.getTypeParameters().stream().allMatch(M3L2MFacades::concrete);
        return !(type instanceof JavaType.Array array) || concrete(array.getElemType());
    }

    private static int uses(J.CompilationUnit unit, JavaType.Variable field) {
        int[] count = {0};
        new JavaIsoVisitor<int[]>() {
            @Override public J.Identifier visitIdentifier(J.Identifier identifier, int[] result) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (field.equals(identifier.getFieldType())
                        && !(parent instanceof J.VariableDeclarations.NamedVariable declared
                            && declared.getName().getId().equals(identifier.getId()))) result[0]++;
                return super.visitIdentifier(identifier, result);
            }
        }.visit(unit, count);
        return count[0];
    }

    private static boolean scaffoldComments(J tree, J retainedBody) {
        boolean[] found = {false};
        new JavaIsoVisitor<boolean[]>() {
            @Override public J visit(Tree node, boolean[] state) {
                return node == retainedBody ? (J) node : super.visit(node, state);
            }
            @Override public Space visitSpace(Space space, Space.Location location, boolean[] state) {
                if (!space.getComments().isEmpty()) state[0] = true;
                return space;
            }
        }.visit(tree, found);
        return found[0];
    }
}
