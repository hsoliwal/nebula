// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Structural serial-review gate for one algorithm atom recipe-crate plan. */
public final class M3AlgorithmAtomPlanGate {
    public record Receipt(
            int fileCount,
            int bindingCount,
            int classifiedMethodCount,
            String planRoot,
            String root) {
        public Receipt {
            if (fileCount < 0 || bindingCount < 0 || classifiedMethodCount < 0) {
                throw new IllegalArgumentException("negative plan gate count");
            }
            planRoot = sha(planRoot, "planRoot");
            root = sha(root, "root");
        }

        public boolean sourceMutationAuthority() { return false; }
        public boolean promotionAuthority() { return false; }
    }

    private M3AlgorithmAtomPlanGate() {}

    public static Receipt verify(M3AlgorithmAtomRecipeCratePlanner.Plan plan) {
        M3AlgorithmAtomRecipeCratePlanner.Plan checked = Objects.requireNonNull(plan, "plan");
        List<M3AlgorithmAtomRecipeCratePlanner.FileCoverage> files = checked.files();
        List<M3AlgorithmAtomRecipeCrateBinding> bindings = checked.bindings();

        Map<Integer, Integer> bindingCounts = new HashMap<>();
        Map<Integer, Set<Integer>> classifiedAtoms = new HashMap<>();
        Map<String, Integer> nextCandidate = new HashMap<>();
        Set<String> exactCandidates = new HashSet<>();
        Set<String> bindingRoots = new HashSet<>();

        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            M3AlgorithmAtomRecipeCratePlanner.FileCoverage file = files.get(fileIndex);
            if (file.fileOrdinal() != fileIndex) {
                throw new IllegalStateException("non-contiguous file ordinal at " + fileIndex);
            }
        }

        for (int serial = 0; serial < bindings.size(); serial++) {
            M3AlgorithmAtomRecipeCrateBinding binding = bindings.get(serial);
            if (binding.serialOrdinal() != serial) {
                throw new IllegalStateException("non-contiguous serial binding ordinal at " + serial);
            }
            if (binding.fileOrdinal() < 0 || binding.fileOrdinal() >= files.size()) {
                throw new IllegalStateException("binding references missing file ordinal");
            }
            M3AlgorithmAtomRecipeCratePlanner.FileCoverage file = files.get(binding.fileOrdinal());
            if (!file.sourcePath().equals(binding.atom().sourcePath())
                    || !file.fileSourceSha256().equals(binding.atom().fileSourceSha256())) {
                throw new IllegalStateException("binding atom/file preimage drift");
            }

            String atomKey = binding.fileOrdinal() + "\u001f" + binding.atomOrdinal()
                    + "\u001f" + binding.atom().root();
            int expectedCandidate = nextCandidate.getOrDefault(atomKey, 0);
            if (binding.candidateOrdinal() != expectedCandidate) {
                throw new IllegalStateException("non-contiguous candidate ordinal for atom");
            }
            nextCandidate.put(atomKey, Math.addExact(expectedCandidate, 1));

            String candidateKey = atomKey + "\u001f" + binding.candidateOrdinal();
            if (!exactCandidates.add(candidateKey)) {
                throw new IllegalStateException("duplicate exact atom candidate");
            }
            if (!bindingRoots.add(binding.root())) {
                throw new IllegalStateException("duplicate binding root");
            }

            M3NativeDonorSerialReview.Receipt nativeReceipt =
                    binding.nativeDonorReviewReceipt();
            if (nativeReceipt.shape() != binding.shape()
                    || nativeReceipt.category() != binding.competitiveCategory()
                    || M3AlgorithmAtomRecipeCrateBinding.lane(nativeReceipt.nativeLane())
                            != binding.executionLane()) {
                throw new IllegalStateException("native donor receipt/binding drift");
            }

            bindingCounts.merge(binding.fileOrdinal(), 1, Math::addExact);
            classifiedAtoms
                    .computeIfAbsent(binding.fileOrdinal(), ignored -> new HashSet<>())
                    .add(binding.atomOrdinal());
        }

        int classifiedCount = 0;
        int declaredBindings = 0;
        for (M3AlgorithmAtomRecipeCratePlanner.FileCoverage file : files) {
            int actualBindings = bindingCounts.getOrDefault(file.fileOrdinal(), 0);
            int actualClassified =
                    classifiedAtoms.getOrDefault(file.fileOrdinal(), Set.of()).size();
            if (file.candidateBindings() != actualBindings) {
                throw new IllegalStateException("file candidate-binding count drift: " + file.sourcePath());
            }
            if (file.classifiedMethods() != actualClassified) {
                throw new IllegalStateException("file classified-method count drift: " + file.sourcePath());
            }
            declaredBindings = Math.addExact(declaredBindings, file.candidateBindings());
            classifiedCount = Math.addExact(classifiedCount, file.classifiedMethods());
        }
        if (declaredBindings != bindings.size()) {
            throw new IllegalStateException("plan candidate-binding total drift");
        }

        String root = digest(
                "M3_ALGORITHM_ATOM_PLAN_GATE_V1",
                checked.root(),
                Integer.toString(files.size()),
                Integer.toString(bindings.size()),
                Integer.toString(classifiedCount),
                files.stream().map(M3AlgorithmAtomRecipeCratePlanner.FileCoverage::root)
                        .reduce("", M3AlgorithmAtomPlanGate::join),
                bindings.stream().map(M3AlgorithmAtomRecipeCrateBinding::root)
                        .reduce("", M3AlgorithmAtomPlanGate::join),
                bindings.stream()
                        .map(binding -> binding.nativeDonorReviewReceipt().root())
                        .reduce("", M3AlgorithmAtomPlanGate::join));
        return new Receipt(files.size(), bindings.size(), classifiedCount, checked.root(), root);
    }

    private static String join(String left, String right) {
        return left.isEmpty() ? right : left + "\u001f" + right;
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = Objects.requireNonNull(value, "digest field")
                        .getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
