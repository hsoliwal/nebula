// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.List;
import java.util.Objects;

/** Production-facing projection for one algorithm shape. */
public record TemplateStyle(
        EnterpriseTemplateKind kind,
        List<String> businessVerbs,
        String templatePattern) {

    public TemplateStyle {
        kind = Objects.requireNonNull(kind, "kind");
        businessVerbs = List.copyOf(Objects.requireNonNull(businessVerbs, "businessVerbs"));
        templatePattern = Objects.requireNonNull(templatePattern, "templatePattern");
    }
}
