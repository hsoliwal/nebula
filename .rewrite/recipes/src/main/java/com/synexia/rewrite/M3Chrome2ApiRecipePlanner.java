// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.chrome2api.Chrome2ApiRecipePlan;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Converts the canonical M3 recipe catalogue decision into an immutable Chrome2api plan. */
public final class M3Chrome2ApiRecipePlanner {
    private final M3RecipeCatalogue catalogue;

    public M3Chrome2ApiRecipePlanner(M3RecipeCatalogue catalogue) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
    }

    public Chrome2ApiRecipePlan plan(
            String taskId,
            String capabilityId,
            List<String> targetPaths,
            List<String> atomIds,
            List<String> donorRepositories) {
        M3RecipeCatalogue.Resolution resolution =
                catalogue.resolveForLlmTask(capabilityId);

        Chrome2ApiRecipePlan.Mode mode =
                resolution.disposition()
                                == M3RecipeCatalogue.Disposition.REUSE_OR_COMPOSE_RECIPE
                        ? Chrome2ApiRecipePlan.Mode.REUSE_OR_COMPOSE
                        : Chrome2ApiRecipePlan.Mode.CREATE_OR_IMPROVE;

        String recipeClassName =
                resolution.candidates().isEmpty()
                        ? "NEW_OR_IMPROVE:" + recipeClassName(capabilityId)
                        : resolution.candidates().getFirst().recipeClassName();

        return new Chrome2ApiRecipePlan(
                Chrome2ApiRecipePlan.SCHEMA,
                taskId,
                capabilityId,
                mode,
                recipeClassName,
                targetPaths,
                atomIds,
                donorRepositories,
                requirementsSha256(
                        taskId, capabilityId, targetPaths, atomIds, donorRepositories),
                false,
                false);
    }

    private static String recipeClassName(String capabilityId) {
        String token =
                capabilityId.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", " ")
                        .strip()
                        .replaceAll("\\s+", " ");
        if (token.isEmpty()) token = "capability";
        StringBuilder result = new StringBuilder("com.synexia.rewrite.");
        for (String part : token.split(" ")) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1));
            }
        }
        return result + "Recipe";
    }

    private static String requirementsSha256(
            String taskId,
            String capabilityId,
            List<String> targetPaths,
            List<String> atomIds,
            List<String> donorRepositories) {
        String canonical =
                taskId
                        + "\n"
                        + capabilityId
                        + "\n"
                        + String.join("\n", targetPaths.stream().sorted().toList())
                        + "\n"
                        + String.join("\n", atomIds.stream().sorted().toList())
                        + "\n"
                        + String.join("\n", donorRepositories.stream().sorted().toList());
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("JDK must provide SHA-256", impossible);
        }
    }
}
