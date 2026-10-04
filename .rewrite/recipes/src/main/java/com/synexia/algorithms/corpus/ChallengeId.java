// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.Locale;
import java.util.Objects;

/** Stable logical challenge identity independent of donor repository or source-file path. */
public record ChallengeId(
        ChallengePlatform platform,
        String externalId,
        String slug) implements Comparable<ChallengeId> {

    public ChallengeId {
        platform = Objects.requireNonNull(platform, "platform");
        externalId = requireToken(externalId, "externalId");
        slug = requireToken(slug, "slug");
    }

    public String stableId() {
        return platform.name().toLowerCase(Locale.ROOT) + ":" + externalId;
    }

    @Override
    public int compareTo(ChallengeId other) {
        Objects.requireNonNull(other, "other");
        int platformOrder = platform.compareTo(other.platform);
        if (platformOrder != 0) return platformOrder;
        int idOrder = naturalTokenCompare(externalId, other.externalId);
        return idOrder != 0 ? idOrder : slug.compareTo(other.slug);
    }

    private static int naturalTokenCompare(String left, String right) {
        boolean leftNumeric = left.chars().allMatch(Character::isDigit);
        boolean rightNumeric = right.chars().allMatch(Character::isDigit);
        if (leftNumeric != rightNumeric) return leftNumeric ? -1 : 1;
        if (leftNumeric) {
            String l = stripLeadingZeros(left);
            String r = stripLeadingZeros(right);
            int length = Integer.compare(l.length(), r.length());
            int value = length != 0 ? length : l.compareTo(r);
            return value != 0 ? value : left.compareTo(right);
        }
        return left.compareTo(right);
    }

    static String stripLeadingZeros(String value) {
        int index = 0;
        while (index + 1 < value.length() && value.charAt(index) == '0') index++;
        return value.substring(index);
    }

    private static String requireToken(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return checked;
    }
}
