// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

/**
 * Read-only M3 IOP class-hook authority evidence.
 *
 * <p>Rows describe why an IOP top-level class was admitted or rejected before the canonical
 * OpenRewrite candidate passes run. This table grants no mutation or promotion authority.</p>
 */
public final class M3IopAuthorityTable extends DataTable<M3IopAuthorityTable.Row> {

    public M3IopAuthorityTable(Recipe recipe) {
        super(
                recipe,
                "M3 IOP OpenRewrite authority evidence",
                "IOP source-fence, class-hook, PatternCatalog and recipe-plan evidence; candidate evidence only.");
    }

    public record Row(
            @Column(displayName = "Source path", description = "Repository-relative IOP Java source path.")
                    String sourcePath,
            @Column(displayName = "Recipe source pattern", description = "Exact immutable source-file pattern configured on the canonical recipe.")
                    String sourceFilePattern,
            @Column(displayName = "Class", description = "Top-level Java class represented by this evidence row.")
                    String className,
            @Column(displayName = "Class admitted", description = "Whether deterministic IOP class hooks admitted this top-level class.")
                    boolean classAdmitted,
            @Column(displayName = "Hook ids", description = "Sorted deterministic class-hook ids separated by unit separator.")
                    String hookIds,
            @Column(displayName = "Pattern catalog receipt", description = "SHA-256 root of this class's PatternCatalog binding/residue receipt.")
                    String catalogReceiptRoot,
            @Column(displayName = "Class evidence root", description = "SHA-256 root binding class, hook ids and PatternCatalog evidence.")
                    String classEvidenceRoot,
            @Column(displayName = "File admitted", description = "Whether every top-level class in this source atom was admitted.")
                    boolean fileAdmitted,
            @Column(displayName = "Pass ids", description = "Canonical immutable OpenRewrite pass ids separated by unit separator.")
                    String passIds,
            @Column(displayName = "M3 stages", description = "Canonical M3 authority stages separated by unit separator.")
                    String stages,
            @Column(displayName = "Reason", description = "Deterministic file-level admission or rejection reason.")
                    String reason,
            @Column(displayName = "Authority receipt root", description = "SHA-256 root of the complete immutable authority receipt.")
                    String authorityReceiptRoot,
            @Column(displayName = "Authority", description = "Always CANDIDATE_EVIDENCE_ONLY.")
                    String authority) {}
}
