// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.build.inventory.RepoAction;
import com.synexia.build.inventory.RepoFileCategory;
import com.synexia.build.inventory.RepoFileRecord;
import com.synexia.build.inventory.RepoLanguage;
import com.synexia.build.inventory.RepoSignal;
import com.synexia.build.inventory.RepoSupersetInventory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Inventory-driven OpenRewrite candidate plan.
 *
 * <p>Every normal Java source receives the conservative file-local mechanical plan. Files carrying
 * unresolved REVIEW/ERROR inventory evidence are held for review and receive no executable pass list.
 * Similarity/duplicate evidence therefore cannot become mutation authority accidentally.
 */
public final class M3InventoryRewritePlan {

    public enum Mode {
        CANDIDATE_ONLY,
        REVIEW_ONLY
    }

    public record Entry(
        String sourcePath,
        String preimageSha256,
        Mode mode,
        List<String> passIds,
        List<String> holdReasons,
        String structureSha256,
        String root) {

      public Entry {
        sourcePath = M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath);
        preimageSha256 = sha(preimageSha256, "preimageSha256");
        mode = Objects.requireNonNull(mode, "mode");
        passIds = List.copyOf(Objects.requireNonNull(passIds, "passIds"));
        holdReasons = List.copyOf(Objects.requireNonNull(holdReasons, "holdReasons"));
        structureSha256 = sha(structureSha256, "structureSha256");
        if (mode == Mode.CANDIDATE_ONLY && passIds.isEmpty()) {
          throw new IllegalArgumentException("candidate entry requires passes");
        }
        if (mode == Mode.REVIEW_ONLY && holdReasons.isEmpty()) {
          throw new IllegalArgumentException("review entry requires hold reasons");
        }
        if (mode == Mode.REVIEW_ONLY && !passIds.isEmpty()) {
          throw new IllegalArgumentException("review entry cannot expose mutation passes");
        }

        String expected = hash(String.join(
            "\n",
            "M3-INVENTORY-REWRITE-ENTRY/1",
            sourcePath,
            preimageSha256,
            mode.name(),
            String.join(",", passIds),
            String.join(",", holdReasons),
            structureSha256));
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!expected.equals(root)) {
          throw new IllegalArgumentException("INVENTORY_REWRITE_ENTRY_ROOT_MISMATCH");
        }
      }

      public boolean executableCandidate() {
        return mode == Mode.CANDIDATE_ONLY;
      }

      public M3TranspilePlan compile() {
        if (!executableCandidate()) {
          throw new IllegalStateException("REVIEW_ONLY_ENTRY_HAS_NO_MUTATION_AUTHORITY");
        }
        M3TranspilePlan plan =
            M3TranspilePlan.Kind.JAVA_MECHANICAL_CANDIDATE.compile(sourcePath);
        if (!plan.structureSha256().equals(structureSha256)) {
          throw new IllegalStateException("INVENTORY_REWRITE_STRUCTURE_DRIFT");
        }
        List<String> actual = plan.passes().stream().map(M3TranspilePass::id).toList();
        if (!actual.equals(passIds)) {
          throw new IllegalStateException("INVENTORY_REWRITE_PASS_DRIFT");
        }
        return plan;
      }
    }

    private final List<Entry> entries;
    private final List<Entry> executableCandidates;
    private final List<Entry> reviewOnly;
    private final String inventoryFingerprint;
    private final String root;

    private M3InventoryRewritePlan(
        String inventoryFingerprint,
        List<Entry> sourceEntries) {
      this.inventoryFingerprint = sha(inventoryFingerprint, "inventoryFingerprint");
      ArrayList<Entry> ordered = new ArrayList<>(Objects.requireNonNull(sourceEntries, "entries"));
      ordered.sort(Comparator.comparing(Entry::sourcePath));
      this.entries = List.copyOf(ordered);
      this.executableCandidates =
          this.entries.stream().filter(Entry::executableCandidate).toList();
      this.reviewOnly =
          this.entries.stream().filter(entry -> !entry.executableCandidate()).toList();

      StringBuilder material = new StringBuilder("M3-INVENTORY-REWRITE-PLAN/1\n")
          .append(this.inventoryFingerprint)
          .append('\n');
      this.entries.forEach(entry -> material.append(entry.root()).append('\n'));
      this.root = hash(material.toString());
    }

    public static M3InventoryRewritePlan from(RepoSupersetInventory inventory) {
      Objects.requireNonNull(inventory, "inventory");
      Map<String, List<String>> holds = holdReasons(inventory);
      ArrayList<Entry> entries = new ArrayList<>();

      for (RepoFileRecord file : inventory.files()) {
        if (file.language() != RepoLanguage.JAVA
            || file.category() != RepoFileCategory.SOURCE
            || file.signals().contains(RepoSignal.TEST_SOURCE)
            || file.signals().contains(RepoSignal.GENERATED_SOURCE)) {
          continue;
        }

        List<String> reasons = holds.getOrDefault(file.path(), List.of());
        if (!reasons.isEmpty()) {
          entries.add(new Entry(
              file.path(),
              file.sha256(),
              Mode.REVIEW_ONLY,
              List.of(),
              reasons,
              identityStructure(file.path()),
              ""));
          continue;
        }

        M3TranspilePlan plan =
            M3TranspilePlan.Kind.JAVA_MECHANICAL_CANDIDATE.compile(file.path());
        entries.add(new Entry(
            file.path(),
            file.sha256(),
            Mode.CANDIDATE_ONLY,
            plan.passes().stream().map(M3TranspilePass::id).toList(),
            List.of(),
            plan.structureSha256(),
            ""));
      }
      return new M3InventoryRewritePlan(inventory.fingerprint(), entries);
    }

    public List<Entry> entries() {
      return entries;
    }

    public List<Entry> executableCandidates() {
      return executableCandidates;
    }

    public List<Entry> reviewOnly() {
      return reviewOnly;
    }

    /** Deterministic O(log n) lookup over the already source-path-sorted immutable plan. */
    public Optional<Entry> entry(String sourcePath) {
      String checked = M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath);
      int low = 0;
      int high = entries.size() - 1;
      while (low <= high) {
        int middle = (low + high) >>> 1;
        int compared = entries.get(middle).sourcePath().compareTo(checked);
        if (compared < 0) {
          low = middle + 1;
        } else if (compared > 0) {
          high = middle - 1;
        } else {
          return Optional.of(entries.get(middle));
        }
      }
      return Optional.empty();
    }

    public String inventoryFingerprint() {
      return inventoryFingerprint;
    }

    public String root() {
      return root;
    }

    private static Map<String, List<String>> holdReasons(RepoSupersetInventory inventory) {
      Map<String, List<String>> result = new TreeMap<>();
      for (RepoAction action : inventory.actions()) {
        if (action.severity() == RepoAction.Severity.INFO) continue;
        result.computeIfAbsent(action.path(), ignored -> new ArrayList<>())
            .add(action.category() + ":" + action.detail());
      }
      result.replaceAll(
          (path, values) ->
              values.stream().sorted().distinct().toList());
      return Map.copyOf(result);
    }

    private static String identityStructure(String path) {
      return hash("M3-INVENTORY-REVIEW-ONLY/1\n" + path);
    }

    private static String hash(String value) {
      try {
        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
      } catch (NoSuchAlgorithmException impossible) {
        throw new IllegalStateException("SHA-256 unavailable", impossible);
      }
    }

    private static String sha(String value, String field) {
      if (value == null || !value.matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException(field);
      }
      return value;
    }
}
