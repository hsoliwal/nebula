package com.synexia.iop.patterns;

import com.synexia.iop.patterns.PatternDescriptor.Authority;
import com.synexia.iop.patterns.PatternDescriptor.Family;
import com.synexia.iop.patterns.PatternRecipe.Step;
import com.synexia.iop.patterns.PatternRecipe.StepKind;
import java.util.ArrayList;
import java.util.List;

/** Family-aware deterministic recipe generator for every canonical pattern descriptor. */
public final class PatternRecipeBook {
    private static final List<String> INVARIANTS = List.of(
            "PATTERN_MATCH_IS_NOT_TRUTH",
            "PATTERN_DESCRIPTOR_IS_NOT_EXECUTION_AUTHORITY",
            "RECIPE_IS_NOT_SIDE_EFFECT",
            "EXTERNAL_EFFECT_REQUIRES_OWNER_ADMISSION",
            "RANKING_IS_NOT_ADMISSION");

    private PatternRecipeBook() {}

    public static PatternRecipe recipeFor(PatternDescriptor descriptor) {
        List<Step> steps = new ArrayList<>();
        int n = 0;
        steps.add(Step.of(n++, "inventory", StepKind.INVENTORY, descriptor.targetModule()));
        steps.add(Step.of(n++, "match-contract", StepKind.MATCH, descriptor.targetModule()));
        steps.add(Step.of(n++, "validate-preconditions", StepKind.VALIDATE, descriptor.targetModule()));

        switch (descriptor.family()) {
            case GOF -> {
                steps.add(Step.of(n++, "construct-gof-primitive", StepKind.CONSTRUCT, "synexia-iop"));
                steps.add(Step.of(n++, "execute-local-primitive", StepKind.EXECUTE_LOCAL, "synexia-iop"));
            }
            case EIP -> {
                steps.add(Step.of(n++, "construct-route-candidate", StepKind.ROUTE, "synexia-camel-dag-bridge"));
                steps.add(Step.of(n++, "delegate-route-runtime", StepKind.DELEGATE_TO_OWNER, descriptor.targetModule()));
            }
            case DAG -> {
                steps.add(Step.of(n++, "construct-dag-candidate", StepKind.COMPOSE, "synexia-flldcim"));
                steps.add(Step.of(n++, "delegate-dag-runtime", StepKind.DELEGATE_TO_OWNER, descriptor.targetModule()));
            }
            case DISTRIBUTED -> {
                steps.add(Step.of(n++, "construct-distributed-candidate", StepKind.CONSTRUCT, "synexia-cop"));
                steps.add(Step.of(n++, "delegate-distributed-runtime", StepKind.DELEGATE_TO_OWNER, descriptor.targetModule()));
            }
            case MICROSERVICE -> {
                steps.add(Step.of(n++, "construct-service-candidate", StepKind.COMPOSE, "synexia-cop"));
                steps.add(Step.of(n++, "delegate-service-runtime", StepKind.DELEGATE_TO_OWNER, descriptor.targetModule()));
            }
            case SPRING -> {
                steps.add(Step.of(n++, "construct-framework-adapter", StepKind.CONSTRUCT, "synexia-iop"));
                steps.add(Step.of(n++, "delegate-spring-runtime", StepKind.DELEGATE_TO_OWNER, descriptor.targetModule()));
            }
            case AOP_ADVICE -> {
                steps.add(Step.of(n++, "construct-advice-candidate", StepKind.COMPOSE, "synexia-aop"));
                steps.add(Step.of(n++, "delegate-weaver-runtime", StepKind.DELEGATE_TO_OWNER, descriptor.targetModule()));
            }
            case SYNEXIA -> {
                steps.add(Step.of(n++, "construct-synexia-pattern", StepKind.COMPOSE, descriptor.targetModule()));
                if (descriptor.authority() == Authority.PURE_LOCAL
                        || descriptor.authority() == Authority.CONTROL_ONLY) {
                    steps.add(Step.of(n++, "execute-local-pattern", StepKind.EXECUTE_LOCAL, descriptor.targetModule()));
                } else {
                    steps.add(Step.of(n++, "delegate-owning-module", StepKind.DELEGATE_TO_OWNER, descriptor.targetModule()));
                }
            }
        }

        steps.add(Step.of(n++, "verify-pattern-result", StepKind.VERIFY, descriptor.targetModule()));
        boolean ownerAdmission = requiresAdmission(descriptor.authority());
        if (ownerAdmission) {
            steps.add(Step.of(n++, "owner-local-admission", StepKind.ADMIT, descriptor.targetModule()));
        }
        steps.add(Step.of(n, "emit-pattern-receipt", StepKind.EMIT_RECEIPT, "synexia-iop"));

        return new PatternRecipe(descriptor.id(), steps, INVARIANTS, ownerAdmission, "");
    }

    private static boolean requiresAdmission(Authority authority) {
        return switch (authority) {
            case PURE_LOCAL, CONTROL_ONLY -> false;
            case CANDIDATE_ONLY, LOCAL_ADMISSION_REQUIRED, EXTERNAL_EFFECT -> true;
        };
    }
}
