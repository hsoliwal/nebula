// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.jdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.MethodMatcher;
import java.util.List;
/** Candidate transformation; independent compile/test/runtime gates remain mandatory. */
public final class M3SingleCharacterSearchRecipe extends M3BoundedJdkRecipe {
    public M3SingleCharacterSearchRecipe() { this(null, null); }
    @JsonCreator public M3SingleCharacterSearchRecipe(
            @JsonProperty("sourcePath") String sourcePath,
            @JsonProperty("maxChangesPerFile") Integer maxChangesPerFile) {
        super(sourcePath, maxChangesPerFile);
    }
    @Override public String getDisplayName() { return "M3 use character overloads for one-unit literal searches"; }
    @Override public String getDescription() { return "Uses String.indexOf/lastIndexOf character overloads only for literal needles of exactly one UTF-16 unit."; }
    @Override protected Visitor newVisitor() {
        return new Visitor() {
            private final List<MethodMatcher> searches = List.of(
                    new MethodMatcher("java.lang.String indexOf(java.lang.String)"),
                    new MethodMatcher("java.lang.String indexOf(java.lang.String,int)"),
                    new MethodMatcher("java.lang.String lastIndexOf(java.lang.String)"),
                    new MethodMatcher("java.lang.String lastIndexOf(java.lang.String,int)"));
            @Override public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J visited = super.visitMethodInvocation(method, context);
                if (!(visited instanceof J.MethodInvocation current) || !admitted(current, context)
                        || current.getSelect() == null || searches.stream().noneMatch(matcher -> matcher.matches(current))) {
                    return visited;
                }
                if (!(current.getArguments().getFirst().unwrap() instanceof J.Literal literal)) return current;
                Character needle = M3JdkExpressionSupport.singleUtf16Unit(literal);
                if (needle == null) return current;
                String template = "#{any(java.lang.String)}." + current.getSimpleName() + "("
                        + M3JdkExpressionLaws.charLiteral(needle);
                if (current.getArguments().size() == 1) {
                    return replace(current, template + ")", context, current.getSelect());
                }
                return replace(current, template + ", #{any(int)})", context,
                        current.getSelect(), current.getArguments().get(1));
            }
        };
    }
}
