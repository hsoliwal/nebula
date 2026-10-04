package com.synexia.m3.api;

import java.util.Locale;

/** Small deterministic scorer: exact > prefix > normalized edit similarity. */
public final class FuzzyScorer {
    public ApiCandidate score(String query, ApiDescriptor api) {
        String q = normalize(query);
        String name = normalize(api.name());
        String id = normalize(api.id());
        int distance = Math.min(distance(q, name), distance(q, id));
        boolean exact = q.equals(name) || q.equals(id);
        boolean prefix = !q.isEmpty() && (name.startsWith(q) || id.startsWith(q));
        double edit = 1.0 - ((double) distance / Math.max(1, Math.max(q.length(), Math.max(name.length(), id.length()))));
        double score = exact ? 1.0 : prefix ? Math.max(0.90, edit) : Math.max(0.0, edit);
        return new ApiCandidate(api, score, distance, exact, prefix);
    }

    static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    static int distance(String a, String b) {
        if (a.equals(b)) return 0;
        int[] prev = new int[b.length() + 1];
        int[] next = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            next[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                next[j] = Math.min(Math.min(next[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev; prev = next; next = swap;
        }
        return prev[b.length()];
    }
}
