// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.shapes.AlgorithmShape;
import java.util.Objects;

/** Classification of one problem/source implementation into a reusable computational shape. */
public record ProblemShape(
        String problem,
        AlgorithmShape shape,
        EnterpriseTemplateKind template,
        ClassificationConfidence confidence,
        String rationale) {

    public ProblemShape {
        problem = Objects.requireNonNull(problem, "problem");
        template = Objects.requireNonNull(template, "template");
        confidence = Objects.requireNonNull(confidence, "confidence");
        rationale = Objects.requireNonNull(rationale, "rationale");
        if (confidence == ClassificationConfidence.UNCLASSIFIED && shape != null) {
            throw new IllegalArgumentException("unclassified entries must not claim a shape");
        }
        if (confidence != ClassificationConfidence.UNCLASSIFIED && shape == null) {
            throw new IllegalArgumentException("classified entries require a shape");
        }
    }

    public boolean classified() {
        return shape != null;
    }
}
