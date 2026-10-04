// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.synexia.m3.api.ChallengeCategoryFastSearch;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Search evidence binding the JNI-capable category index to one LLM recipe task. */
public record M3ChallengeCategorySearchEvidence(
        String platform,
        String query,
        List<Hit> hits,
        String root) {

    public record Hit(
            String platform,
            String categoryId,
            String displayName,
            List<String> shapes,
            String taxonomySource,
            double score,
            int editDistance,
            String backend,
            String searchEvidenceSha256) {
        public Hit {
            platform = text(platform, "platform");
            categoryId = text(categoryId, "categoryId");
            displayName = text(displayName, "displayName");
            shapes = List.copyOf(Objects.requireNonNull(shapes, "shapes"));
            taxonomySource = text(taxonomySource, "taxonomySource");
            backend = text(backend, "backend");
            searchEvidenceSha256 = sha(searchEvidenceSha256, "searchEvidenceSha256");
        }
    }

    public M3ChallengeCategorySearchEvidence {
        platform = text(platform, "platform");
        query = Objects.requireNonNull(query, "query").strip();
        hits = List.copyOf(Objects.requireNonNull(hits, "hits"));
        String expected = digest(
                "M3_CHALLENGE_CATEGORY_SEARCH_EVIDENCE_V1",
                platform,
                query,
                hits.stream()
                        .map(hit -> hit.platform() + "|" + hit.categoryId() + "|"
                                + String.join(",", hit.shapes()) + "|" + hit.backend() + "|"
                                + hit.searchEvidenceSha256())
                        .reduce("", (a, b) -> a + b + "\n"),
                "sourceCopyAuthority=false",
                "mutationAuthority=false");
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!expected.equals(root)) throw new IllegalArgumentException("category evidence root mismatch");
    }

    public static M3ChallengeCategorySearchEvidence search(
            String platform, String category, int limit) {
        String checkedPlatform = platform == null || platform.isBlank() ? "ALL" : platform.strip();
        String checkedCategory = category == null ? "" : category.strip();
        if (checkedCategory.isEmpty()) {
            return new M3ChallengeCategorySearchEvidence(checkedPlatform, "", List.of(), "");
        }
        int bounded = Math.max(1, Math.min(limit, 1_000));
        List<Hit> hits = new ChallengeCategoryFastSearch()
                .search(checkedPlatform, checkedCategory, bounded).stream()
                .map(hit -> new Hit(
                        hit.platform().name(),
                        hit.categoryId(),
                        hit.displayName(),
                        hit.shapes().stream().map(Enum::name).toList(),
                        hit.taxonomySource(),
                        hit.score(),
                        hit.editDistance(),
                        hit.backend().name(),
                        hit.evidenceSha256()))
                .toList();
        return new M3ChallengeCategorySearchEvidence(checkedPlatform, checkedCategory, hits, "");
    }

    public boolean sourceCopyAuthority() { return false; }
    public boolean mutationAuthority() { return false; }

    public String tsv() {
        StringBuilder out = new StringBuilder(
                "ordinal\tplatform\tcategoryId\tdisplayName\tshapes\tbackend\tsearchEvidenceSha256\n");
        for (int i = 0; i < hits.size(); i++) {
            Hit hit = hits.get(i);
            out.append(i).append('\t')
                    .append(hit.platform()).append('\t')
                    .append(hit.categoryId()).append('\t')
                    .append(hit.displayName()).append('\t')
                    .append(String.join(",", hit.shapes())).append('\t')
                    .append(hit.backend()).append('\t')
                    .append(hit.searchEvidenceSha256()).append('\n');
        }
        return out.toString();
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0 || checked.indexOf('\t') >= 0
                || checked.indexOf('\n') >= 0 || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return value;
    }

    private static String digest(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
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
