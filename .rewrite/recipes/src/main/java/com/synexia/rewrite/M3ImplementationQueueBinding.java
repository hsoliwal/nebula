// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.build.inventory.RepoSupersetInventory;
import com.synexia.build.inventory.RepositoryImplementationQueue;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Binds one repository implementation queue to exact source preimages and recipe readiness.
 *
 * <p>The implementation queue is planning evidence, not source-mutation authority. This bridge
 * makes the recipe-first invariant explicit: semantic enhancement/implementation work requires a
 * purpose-built recipe; identity/contract/runtime holds remain blocked; already-reusable APIs
 * require no mutation. Existing generic mechanical passes are retained only as candidate cleanup
 * evidence and never substitute for a missing semantic recipe.</p>
 */
public final class M3ImplementationQueueBinding {
    public enum RecipeState {
        NO_MUTATION,
        RECIPE_REQUIRED,
        BLOCKED_REVIEW
    }

    public record Entry(
            int ordinal,
            String queueStableId,
            String sourcePath,
            String preimageSha256,
            RepositoryImplementationQueue.Decision decision,
            RecipeState recipeState,
            String requiredRecipeCapability,
            List<String> candidateMechanicalPassIds,
            List<String> holdReasons,
            List<String> requiredGates,
            List<String> donorIdentities,
            List<String> donorShapes,
            List<String> donorRepositories,
            String reason,
            String root) {
        public Entry {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            queueStableId = sha(queueStableId, "queueStableId");
            sourcePath = M3OpenRewriteTranspiler.normalizeSourcePath(sourcePath);
            preimageSha256 = sha(preimageSha256, "preimageSha256");
            decision = Objects.requireNonNull(decision, "decision");
            recipeState = Objects.requireNonNull(recipeState, "recipeState");
            requiredRecipeCapability = text(requiredRecipeCapability, "requiredRecipeCapability");
            candidateMechanicalPassIds = ordered(candidateMechanicalPassIds);
            holdReasons = stable(holdReasons);
            requiredGates = ordered(requiredGates);
            donorIdentities = stable(donorIdentities);
            donorShapes = stable(donorShapes);
            donorRepositories = stable(donorRepositories);
            reason = text(reason, "reason");

            if (recipeState == RecipeState.NO_MUTATION
                    && !requiredRecipeCapability.equals("none")) {
                throw new IllegalArgumentException("no-mutation row cannot require a recipe");
            }
            if (recipeState == RecipeState.RECIPE_REQUIRED
                    && requiredRecipeCapability.equals("none")) {
                throw new IllegalArgumentException("recipe-required row needs a capability");
            }
            if (recipeState == RecipeState.BLOCKED_REVIEW && holdReasons.isEmpty()) {
                throw new IllegalArgumentException("blocked row requires hold evidence");
            }

            String expected =
                    hash(
                            String.join(
                                    "\n",
                                    "M3-IMPLEMENTATION-QUEUE-BINDING-ENTRY/1",
                                    Integer.toString(ordinal),
                                    queueStableId,
                                    sourcePath,
                                    preimageSha256,
                                    decision.name(),
                                    recipeState.name(),
                                    requiredRecipeCapability,
                                    String.join(",", candidateMechanicalPassIds),
                                    String.join(",", holdReasons),
                                    String.join(",", requiredGates),
                                    String.join(",", donorIdentities),
                                    String.join(",", donorShapes),
                                    String.join(",", donorRepositories),
                                    reason));
            root = root == null || root.isBlank() ? expected : sha(root, "root");
            if (!expected.equals(root)) {
                throw new IllegalArgumentException("implementation queue binding root mismatch");
            }
        }

        public boolean mutationAuthority() {
            return false;
        }

        public boolean readyForRecipeAuthoring() {
            return recipeState == RecipeState.RECIPE_REQUIRED;
        }
    }

    public record Summary(
            int entries,
            Map<RecipeState, Integer> byState,
            String queueRoot,
            String inventoryRoot,
            String rewritePlanRoot,
            String root) {
        public Summary {
            if (entries < 0) throw new IllegalArgumentException("entries");
            EnumMap<RecipeState, Integer> stable = new EnumMap<>(RecipeState.class);
            stable.putAll(Objects.requireNonNull(byState, "byState"));
            for (RecipeState state : RecipeState.values()) stable.putIfAbsent(state, 0);
            if (stable.values().stream().mapToInt(Integer::intValue).sum() != entries) {
                throw new IllegalArgumentException("binding summary count mismatch");
            }
            byState = java.util.Collections.unmodifiableMap(stable);
            queueRoot = sha(queueRoot, "queueRoot");
            inventoryRoot = sha(inventoryRoot, "inventoryRoot");
            rewritePlanRoot = sha(rewritePlanRoot, "rewritePlanRoot");
            root = sha(root, "root");
        }
    }

    private final List<Entry> entries;
    private final Summary summary;

    private M3ImplementationQueueBinding(
            List<Entry> source,
            String queueRoot,
            String inventoryRoot,
            String rewritePlanRoot) {
        ArrayList<Entry> ordered = new ArrayList<>(Objects.requireNonNull(source, "source"));
        ordered.sort(
                Comparator.comparingInt(Entry::ordinal)
                        .thenComparing(Entry::sourcePath)
                        .thenComparing(Entry::queueStableId));
        this.entries = List.copyOf(ordered);

        EnumMap<RecipeState, Integer> counts = new EnumMap<>(RecipeState.class);
        for (RecipeState state : RecipeState.values()) counts.put(state, 0);
        for (Entry entry : entries) counts.merge(entry.recipeState(), 1, Math::addExact);

        String root = root(this.entries, queueRoot, inventoryRoot, rewritePlanRoot);
        this.summary =
                new Summary(
                        this.entries.size(),
                        counts,
                        queueRoot,
                        inventoryRoot,
                        rewritePlanRoot,
                        root);
    }

    public static M3ImplementationQueueBinding build(
            RepoSupersetInventory inventory, RepositoryImplementationQueue queue) {
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(queue, "queue");

        M3InventoryRewritePlan rewritePlan = M3InventoryRewritePlan.from(inventory);
        Map<String, M3InventoryRewritePlan.Entry> rewriteByPath = new HashMap<>();
        for (M3InventoryRewritePlan.Entry entry : rewritePlan.entries()) {
            M3InventoryRewritePlan.Entry previous = rewriteByPath.put(entry.sourcePath(), entry);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "duplicate rewrite-plan source path: " + entry.sourcePath());
            }
        }

        ArrayList<Entry> result = new ArrayList<>(queue.items().size());
        for (RepositoryImplementationQueue.Item item : queue.items()) {
            M3InventoryRewritePlan.Entry rewrite = rewriteByPath.get(item.path());
            if (rewrite == null) {
                throw new IllegalArgumentException(
                        "implementation queue source is not a rewrite-plan Java source: " + item.path());
            }
            RecipeState state = state(item.decision());
            List<String> holds = holds(item, rewrite, state);
            result.add(
                    new Entry(
                            item.ordinal(),
                            item.stableId(),
                            item.path(),
                            rewrite.preimageSha256(),
                            item.decision(),
                            state,
                            requiredRecipeCapability(item, state),
                            rewrite.executableCandidate() ? rewrite.passIds() : List.of(),
                            holds,
                            item.requiredGates().stream().map(Enum::name).toList(),
                            item.donorIdentities(),
                            item.donorShapes(),
                            item.donorRepositories(),
                            item.reason(),
                            ""));
        }

        return new M3ImplementationQueueBinding(
                result, queue.rootSha256(), inventory.fingerprint(), rewritePlan.root());
    }

    public List<Entry> entries() {
        return entries;
    }

    public Summary summary() {
        return summary;
    }

    public String root() {
        return summary.root();
    }

    private static RecipeState state(RepositoryImplementationQueue.Decision decision) {
        return switch (decision) {
            case REUSE_EXISTING -> RecipeState.NO_MUTATION;
            case ENHANCE_EXISTING, IMPLEMENT_MISSING -> RecipeState.RECIPE_REQUIRED;
            case BRIDGE_IDENTITY, HOLD_CONTRACT_REVIEW, HOLD_RUNTIME_REVIEW ->
                    RecipeState.BLOCKED_REVIEW;
        };
    }

    private static String requiredRecipeCapability(
            RepositoryImplementationQueue.Item item, RecipeState state) {
        if (state == RecipeState.NO_MUTATION) return "none";
        if (state == RecipeState.BLOCKED_REVIEW) {
            return "blocked." + item.decision().name().toLowerCase(java.util.Locale.ROOT);
        }
        String base =
                switch (item.decision()) {
                    case ENHANCE_EXISTING ->
                            "repository-api.enhance."
                                    + item.gapAction().name().toLowerCase(java.util.Locale.ROOT);
                    case IMPLEMENT_MISSING -> "repository-api.implement-missing";
                    default -> throw new IllegalArgumentException("unexpected recipe state");
                };
        if (item.donorShapes().isEmpty()) return base;
        return base + "." + item.donorShapes().getFirst().toLowerCase(java.util.Locale.ROOT);
    }

    private static List<String> holds(
            RepositoryImplementationQueue.Item item,
            M3InventoryRewritePlan.Entry rewrite,
            RecipeState state) {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(rewrite.holdReasons());
        if (state == RecipeState.BLOCKED_REVIEW) {
            result.add(item.decision().name() + ":" + item.reason());
        }
        return result.stream().distinct().sorted().toList();
    }

    private static String root(
            List<Entry> entries, String queueRoot, String inventoryRoot, String rewritePlanRoot) {
        MessageDigest digest = sha256();
        update(digest, "M3-IMPLEMENTATION-QUEUE-BINDING/1");
        update(digest, queueRoot);
        update(digest, inventoryRoot);
        update(digest, rewritePlanRoot);
        update(digest, Integer.toString(entries.size()));
        for (Entry entry : entries) update(digest, entry.root());
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String hash(String value) {
        return HexFormat.of()
                .formatHex(sha256().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }


    private static List<String> ordered(List<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : Objects.requireNonNullElse(values, List.<String>of())) {
            result.add(text(value, "listValue"));
        }
        return List.copyOf(result);
    }

    private static List<String> stable(List<String> values) {
        return Objects.requireNonNullElse(values, List.<String>of()).stream()
                .map(value -> text(value, "listValue"))
                .distinct()
                .sorted()
                .toList();
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = text(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }
}
