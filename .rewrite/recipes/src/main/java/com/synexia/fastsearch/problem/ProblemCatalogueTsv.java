// SPDX-License-Identifier: Apache-2.0
package com.synexia.fastsearch.problem;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Strict metadata-only TSV codec for independently collected problem catalogue rows. */
public final class ProblemCatalogueTsv {
    public static final String HEADER =
            "source\texternalId\ttitle\turl\tcategories\tasymptoticTarget\tevidenceOnly\tlicenseNote";

    private ProblemCatalogueTsv() {}

    public static List<ProblemDescriptor> parse(String tsv) {
        List<String> lines = Objects.toString(tsv, "").lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        if (lines.isEmpty()) return List.of();
        if (!HEADER.equals(lines.get(0))) throw new IllegalArgumentException("unexpected TSV header");
        return lines.stream().skip(1).map(ProblemCatalogueTsv::parseRow).toList();
    }

    private static ProblemDescriptor parseRow(String row) {
        String[] fields = row.split("\\t", -1);
        if (fields.length != 8) throw new IllegalArgumentException("expected 8 metadata columns");
        ProblemSource source = ProblemSource.valueOf(fields[0].strip().toUpperCase(Locale.ROOT));
        Set<ProblemCategory> categories = parseCategories(fields[4]);
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("problem category review is required");
        }
        URI uri = URI.create(fields[3].strip());
        source.validateProblemPage(uri);
        boolean evidenceOnly = switch (fields[6].strip().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("evidenceOnly must be true/false");
        };
        return new ProblemDescriptor(
                source,
                fields[1],
                fields[2],
                uri,
                categories,
                fields[5],
                evidenceOnly,
                fields[7]);
    }

    private static Set<ProblemCategory> parseCategories(String value) {
        String normalized = Objects.toString(value, "").strip();
        if (normalized.isEmpty()) return Set.of();
        EnumSet<ProblemCategory> result = EnumSet.noneOf(ProblemCategory.class);
        Arrays.stream(normalized.split(","))
                .map(String::strip)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toUpperCase(Locale.ROOT))
                .map(ProblemCategory::valueOf)
                .forEach(result::add);
        return Set.copyOf(result);
    }
}
