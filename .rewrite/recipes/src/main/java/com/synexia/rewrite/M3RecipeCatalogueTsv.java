// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Strict metadata-only codec for content-addressed M3 recipe catalogue rows. */
public final class M3RecipeCatalogueTsv {
    public static final String HEADER =
            "capabilityId\trecipeClassName\trecipeArtifactSha256\tfixtureRootSha256\ttags";
    private static final int MAX_CHARS = 2_000_000;
    private static final int MAX_ROWS = 100_000;

    private M3RecipeCatalogueTsv() {}

    public static M3RecipeCatalogue parse(String tsv) {
        String text = java.util.Objects.toString(tsv, "");
        if (text.length() > MAX_CHARS) {
            throw new IllegalArgumentException("recipe catalogue too large");
        }
        List<String> lines =
                text.lines()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList();
        if (lines.isEmpty()) return new M3RecipeCatalogue(List.of());
        if (!HEADER.equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected recipe catalogue header");
        }
        if (lines.size() - 1 > MAX_ROWS) {
            throw new IllegalArgumentException("too many recipe catalogue rows");
        }

        ArrayList<M3RecipeDescriptor> entries = new ArrayList<>(lines.size() - 1);
        TreeSet<String> identities = new TreeSet<>();
        for (int index = 1; index < lines.size(); index++) {
            String[] fields = lines.get(index).split("\\t", -1);
            if (fields.length != 5) {
                throw new IllegalArgumentException(
                        "expected 5 recipe catalogue columns at row " + (index + 1));
            }
            Set<String> tags = tags(fields[4]);
            M3RecipeDescriptor descriptor =
                    new M3RecipeDescriptor(
                            required(fields[0], "capabilityId"),
                            required(fields[1], "recipeClassName"),
                            hash(fields[2], "recipeArtifactSha256"),
                            hash(fields[3], "fixtureRootSha256"),
                            tags);
            String identity =
                    descriptor.capabilityId()
                            + "\t"
                            + descriptor.recipeClassName()
                            + "\t"
                            + descriptor.recipeArtifactSha256()
                            + "\t"
                            + descriptor.fixtureRootSha256();
            if (!identities.add(identity)) {
                throw new IllegalArgumentException(
                        "duplicate recipe catalogue row: " + descriptor.capabilityId());
            }
            entries.add(descriptor);
        }
        return new M3RecipeCatalogue(entries);
    }

    private static Set<String> tags(String value) {
        TreeSet<String> tags = new TreeSet<>();
        Arrays.stream(java.util.Objects.toString(value, "").split(","))
                .map(String::strip)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .forEach(tags::add);
        return Set.copyOf(tags);
    }

    private static String required(String value, String field) {
        String checked = java.util.Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String hash(String value, String field) {
        String checked = required(value, field).toLowerCase(Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
