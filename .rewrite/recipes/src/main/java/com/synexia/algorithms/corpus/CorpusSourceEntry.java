// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import java.util.Locale;
import java.util.Objects;

/** One Java implementation discovered in a pinned external donor repository. */
public record CorpusSourceEntry(
        String platform,
        String repository,
        String commit,
        String path) {

    public CorpusSourceEntry {
        platform = require(platform, "platform");
        repository = require(repository, "repository");
        commit = require(commit, "commit");
        path = require(path, "path");
    }

    public String problemName() {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        if (name.endsWith(".java")) name = name.substring(0, name.length() - 5);
        if (name.matches("(?i)(solution|main)(?:\\d+|[_-].*)?")) {
            String[] segments = path.split("/");
            int index = segments.length - 2;
            if (index >= 0 && segments[index].equalsIgnoreCase("src")) index--;
            if (index >= 0) return segments[index];
        }
        return name;
    }

    public ProblemShape classification() {
        return ProblemShapeClassifier.classify(path);
    }

    /** Stable, human-reviewable identity for recipe/donor evidence. */
    public String evidenceId() {
        return platform.toUpperCase(Locale.ROOT)
                + ":"
                + repository
                + "@"
                + commit
                + ":"
                + path;
    }

    private static String require(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        return value;
    }
}
