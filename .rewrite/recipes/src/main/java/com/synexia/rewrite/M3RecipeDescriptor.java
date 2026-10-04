// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable catalogue entry for a deterministic M3 transformation.
 *
 * <p>The catalogue records applicability and evidence; it never grants canonical mutation
 * authority. Promotion remains an M3 verification/authorization concern.</p>
 */
public record M3RecipeDescriptor(
        String capabilityId,
        String recipeClassName,
        String recipeArtifactSha256,
        String fixtureRootSha256,
        Set<String> tags) {

    public M3RecipeDescriptor {
        capabilityId = required(capabilityId, "capabilityId");
        recipeClassName = required(recipeClassName, "recipeClassName");
        recipeArtifactSha256 = sha256(recipeArtifactSha256, "recipeArtifactSha256");
        fixtureRootSha256 = sha256(fixtureRootSha256, "fixtureRootSha256");
        tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
    }

    public String identitySha256() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "SYNEXIA_M3_RECIPE_DESCRIPTOR_V1");
            frame(digest, capabilityId);
            frame(digest, recipeClassName);
            frame(digest, recipeArtifactSha256);
            frame(digest, fixtureRootSha256);
            tags.stream().sorted().forEach(value -> frame(digest, value));
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

    private static String required(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(name + " required");
        }
        return value.strip();
    }

    private static String sha256(String value, String name) {
        String normalized = required(value, name).toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(name + " must be a 64-hex SHA-256");
        }
        return normalized;
    }
}
