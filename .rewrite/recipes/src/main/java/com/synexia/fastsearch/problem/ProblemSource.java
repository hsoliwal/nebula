// SPDX-License-Identifier: Apache-2.0
package com.synexia.fastsearch.problem;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Metadata-only problem sources. No source grants solution-code reuse rights. */
public enum ProblemSource {
    LEETCODE("leetcode.com"),
    HACKERRANK("hackerrank.com"),
    GEEKSFORGEEKS("geeksforgeeks.org"),
    OTHER("");

    private static final Pattern LEETCODE_PAGE = Pattern.compile(
            "/problems/[a-z0-9]+(?:-[a-z0-9]+)*/");
    private static final Pattern HACKERRANK_PAGE = Pattern.compile(
            "/challenges/[a-z0-9]+(?:-[a-z0-9]+)*/problem");
    private static final Pattern GFG_PAGE = Pattern.compile(
            "/problems/[a-z0-9]+(?:-+[a-z0-9]+)*/[1-9][0-9]*");

    private final String expectedHostSuffix;

    ProblemSource(String expectedHostSuffix) {
        this.expectedHostSuffix = expectedHostSuffix;
    }

    public void validateUri(URI uri) {
        Objects.requireNonNull(uri, "uri");
        if (this == OTHER) return;
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("public problem URLs require HTTPS without user info");
        }
        String host = Objects.toString(uri.getHost(), "").toLowerCase(Locale.ROOT);
        if (!host.equals(expectedHostSuffix) && !host.endsWith("." + expectedHostSuffix)) {
            throw new IllegalArgumentException("URI host does not match " + name());
        }
    }

    /** Accepted manual metadata-intake routes; this checks URL shape, not page identity or rights. */
    public void validateProblemPage(URI uri) {
        validateUri(uri);
        if (this == OTHER) return;
        if (uri.getPort() != -1 || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("public problem URL must be canonical and untracked");
        }
        String path = Objects.toString(uri.getRawPath(), "");
        boolean admitted = switch (this) {
            case LEETCODE -> LEETCODE_PAGE.matcher(path).matches();
            case HACKERRANK -> HACKERRANK_PAGE.matcher(path).matches();
            case GEEKSFORGEEKS -> GFG_PAGE.matcher(path).matches();
            case OTHER -> true;
        };
        if (!admitted) {
            throw new IllegalArgumentException("URL is not an admitted " + name() + " problem page");
        }
    }

    /** These sources are catalogue/evidence donors unless an independently verified license says otherwise. */
    public boolean requiresEvidenceOnly() {
        return this != OTHER;
    }
}
