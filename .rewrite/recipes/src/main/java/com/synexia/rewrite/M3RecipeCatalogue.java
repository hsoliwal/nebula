// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic, read-only recipe catalogue used before any LLM-assisted coding task.
 *
 * <p>M3 invariant: search admitted recipes first. A miss opens a recipe-improvement/new-recipe
 * candidate lane; it does not authorize direct target-file mutation.</p>
 */
public final class M3RecipeCatalogue {
    private final List<M3RecipeDescriptor> entries;
    private final String root;

    public M3RecipeCatalogue(List<M3RecipeDescriptor> entries) {
        Objects.requireNonNull(entries, "entries");
        ArrayList<M3RecipeDescriptor> copy = new ArrayList<>(entries);
        copy.sort(Comparator.comparing(M3RecipeDescriptor::capabilityId)
                .thenComparing(M3RecipeDescriptor::recipeClassName)
                .thenComparing(M3RecipeDescriptor::recipeArtifactSha256));
        this.entries = List.copyOf(copy);
        this.root = root(this.entries);
    }

    public List<M3RecipeDescriptor> entries() {
        return entries;
    }

    public String root() {
        return root;
    }

    public List<M3RecipeDescriptor> find(String capabilityId) {
        if (capabilityId == null || capabilityId.isBlank() || capabilityId.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("capabilityId required");
        }
        String key = capabilityId.strip();
        return entries.stream()
                .filter(entry -> entry.capabilityId().equals(key))
                .toList();
    }

    public Resolution resolveForLlmTask(String capabilityId) {
        List<M3RecipeDescriptor> matches = find(capabilityId);
        return matches.isEmpty()
                ? new Resolution(Disposition.CREATE_OR_IMPROVE_RECIPE, List.of())
                : new Resolution(Disposition.REUSE_OR_COMPOSE_RECIPE, matches);
    }

    private static String root(List<M3RecipeDescriptor> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "SYNEXIA_M3_RECIPE_CATALOGUE_V1");
            frame(digest, Integer.toString(entries.size()));
            entries.forEach(entry -> frame(digest, entry.identitySha256()));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    public enum Disposition {
        REUSE_OR_COMPOSE_RECIPE,
        CREATE_OR_IMPROVE_RECIPE
    }

    public record Resolution(Disposition disposition, List<M3RecipeDescriptor> candidates) {
        public Resolution {
            disposition = Objects.requireNonNull(disposition, "disposition");
            candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        }

        /** Canonical source mutation is deliberately outside catalogue authority. */
        public boolean replacementAuthority() {
            return false;
        }
    }
}
