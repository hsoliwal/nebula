// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

/** Canonical audit manifest for exact method-atom recipe-crate custody. */
public final class M3AlgorithmAtomRecipeCrateManifest {
    public static final String FILE_HEADER =
            "recordType\tfileOrdinal\tsourcePath\tfileSourceSha256\tmethodBodies\tclassifiedMethods"
                    + "\tcandidateBindings\tunresolvedMethodBodies\tnativeDeclarations\trowRoot";
    public static final String BINDING_HEADER =
            "recordType\tserialOrdinal\tfileOrdinal\tatomOrdinal\tcandidateOrdinal\tsourcePath"
                    + "\tfileSourceSha256\townerType\tmethodName\tmethodDescriptor\tmethodSourceSha256"
                    + "\tcontractSurface\tshape\tscore\tproblemCategory\tcapabilityPlanRoot"
                    + "\tbalancedEvidenceRoot"
                    + "\tchallengeReviewRoot\tchallengeDonorLedgerRoot\tdonorEvidenceRoot"
                    + "\tdonorManifestRoot\tjavaDonorCorpusRoot\tjavaDonorEvidence"
                    + "\tnativeMechanicsDonors\tnativeDonorCatalogueRoot"
                    + "\tnativeDonorReviewRoot\tmechanicalDonorRequired"
                    + "\tmechanicalDonorCatalogueRoot\tmechanicalDonorReviewRoot"
                    + "\tcrateRoot\tcrateDisposition\trecipeIds"
                    + "\texecutionLane\taction\tlaneGates\tbindingRoot";

    private M3AlgorithmAtomRecipeCrateManifest() {}

    public static String render(M3AlgorithmAtomRecipeCratePlanner.Plan plan) {
        M3AlgorithmAtomPlanGate.Receipt gate = plan.verifiedReceipt();
        StringBuilder out = new StringBuilder();
        out.append("#M3_ALGORITHM_ATOM_RECIPE_CRATE_PLAN_V2\t")
                .append(plan.root()).append('\t')
                .append(plan.recipeCatalogueRoot()).append('\t')
                .append(plan.problemCatalogueRoot()).append('\t')
                .append(plan.donorCorpusRoot()).append('\t')
                .append(plan.donorManifestRoot()).append('\t')
                .append(plan.challengeDonorLedgerRoot()).append('\t')
                .append(cell(plan.invariantId())).append('\n');
        out.append("#M3_ALGORITHM_ATOM_PLAN_GATE_V1\t")
                .append(gate.root()).append('\t')
                .append(gate.fileCount()).append('\t')
                .append(gate.bindingCount()).append('\t')
                .append(gate.classifiedMethodCount()).append('\n');
        out.append(FILE_HEADER).append('\n');
        for (M3AlgorithmAtomRecipeCratePlanner.FileCoverage file : plan.files()) {
            out.append("FILE\t")
                    .append(file.fileOrdinal()).append('\t')
                    .append(cell(file.sourcePath())).append('\t')
                    .append(file.fileSourceSha256()).append('\t')
                    .append(file.methodBodies()).append('\t')
                    .append(file.classifiedMethods()).append('\t')
                    .append(file.candidateBindings()).append('\t')
                    .append(file.unresolvedMethods()).append('\t')
                    .append(file.nativeDeclarations()).append('\t')
                    .append(file.root()).append('\n');
        }
        out.append(BINDING_HEADER).append('\n');
        for (M3AlgorithmAtomRecipeCrateBinding binding : plan.bindings()) {
            M3AlgorithmAtomRecipeCrateBinding.AtomIdentity atom = binding.atom();
            M3LlmTaskRecipeCrate crate = binding.crate();
            M3NativeDonorSerialReview.Receipt nativeDonorReview =
                    binding.nativeDonorReviewReceipt();
            boolean mechanicalRequired =
                    M3AlgorithmAtomMechanicalDonorBinding
                            .isMechanicalReviewRequired(binding.shape());
            M3MechanicalDonorShapeReview.Receipt mechanicalReview =
                    mechanicalRequired
                            ? M3MechanicalDonorShapeReview.review(binding.shape().name())
                            : null;
            out.append("BINDING\t")
                    .append(binding.serialOrdinal()).append('\t')
                    .append(binding.fileOrdinal()).append('\t')
                    .append(binding.atomOrdinal()).append('\t')
                    .append(binding.candidateOrdinal()).append('\t')
                    .append(cell(atom.sourcePath())).append('\t')
                    .append(atom.fileSourceSha256()).append('\t')
                    .append(cell(atom.ownerType())).append('\t')
                    .append(cell(atom.methodName())).append('\t')
                    .append(cell(atom.methodDescriptor())).append('\t')
                    .append(atom.methodSourceSha256()).append('\t')
                    .append(cell(atom.contractSurface())).append('\t')
                    .append(binding.shape().name()).append('\t')
                    .append(binding.score()).append('\t')
                    .append(binding.problemCategory().name()).append('\t')
                    .append(binding.capabilityPlanRoot()).append('\t')
                    .append(binding.balancedEvidence().root()).append('\t')
                    .append(binding.competitiveEvidence().review().root()).append('\t')
                    .append(binding.challengeDonorLedgerRoot()).append('\t')
                    .append(binding.donorEvidenceRoot()).append('\t')
                    .append(binding.competitiveEvidence().problemEvidence().donorEvidence().manifestRoot()).append('\t')
                    .append(binding.javaDonorCorpusRoot()).append('\t')
                    .append(cell(String.join(",", binding.javaDonorEvidence()))).append('\t')
                    .append(cell(String.join(",", binding.nativeMechanicsDonors()))).append('\t')
                    .append(nativeDonorReview.donorCatalogueRoot()).append('\t')
                    .append(nativeDonorReview.root()).append('\t')
                    .append(mechanicalRequired).append('\t')
                    .append(mechanicalRequired ? mechanicalReview.catalogueRoot() : "NONE")
                    .append('\t')
                    .append(mechanicalRequired ? mechanicalReview.reviewRoot() : "NONE")
                    .append('\t')
                    .append(binding.crateRoot()).append('\t')
                    .append(crate.disposition().name()).append('\t')
                    .append(cell(String.join(",", crate.recipeIds()))).append('\t')
                    .append(binding.executionLane().name()).append('\t')
                    .append(binding.action().name()).append('\t')
                    .append(cell(String.join(",", binding.laneGates()))).append('\t')
                    .append(binding.root()).append('\n');
        }
        return out.toString();
    }

    private static String cell(String value) {
        if (value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("manifest value is not one TSV cell");
        }
        return value;
    }
}
