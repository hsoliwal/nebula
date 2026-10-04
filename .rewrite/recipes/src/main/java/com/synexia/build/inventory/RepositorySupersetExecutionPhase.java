// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

/**
 * Additive execution phases layered over the retained repository inventory/pass APIs.
 *
 * <p>This enum is intentionally separate from the older public pass/stage enums so existing
 * exhaustive switch expressions and serialized values remain source compatible.</p>
 */
public enum RepositorySupersetExecutionPhase {
  API_COVERAGE,
  STRUCTURAL_SUPERSET,
  DATA_PRECOMPUTE,
  DESIGN_ABSTRACTION,
  CONSISTENCY,
  CONSOLIDATION,
  VERIFICATION,
  POLISH
}
