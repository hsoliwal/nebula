// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Canonical source-kind routing for the universal OpenRewrite mutation fabric.
 *
 * <p>Known structured formats use their lossless semantic parser. Every other textual path falls
 * back to plain text. Binary content is intentionally outside this API because it is not a
 * SourceFile text mutation problem.</p>
 */
public enum M3SourceKind {
    JAVA(true, "org.openrewrite:rewrite-java"),
    YAML(true, "org.openrewrite:rewrite-yaml"),
    XML(true, "org.openrewrite:rewrite-xml"),
    JSON(true, "org.openrewrite:rewrite-json"),
    PROPERTIES(true, "org.openrewrite:rewrite-properties"),
    PYTHON(false, "org.openrewrite:rewrite-core"),
    TEXT(false, "org.openrewrite:rewrite-core");

    private final boolean structured;
    private final String parserArtifact;

    M3SourceKind(boolean structured, String parserArtifact) {
        this.structured = structured;
        this.parserArtifact = parserArtifact;
    }

    public boolean structured() {
        return structured;
    }

    public String parserArtifact() {
        return parserArtifact;
    }

    public static M3SourceKind classify(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank() || sourcePath.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourcePath required");
        }
        return classify(Path.of(sourcePath.replace('\\', '/')));
    }

    public static M3SourceKind classify(Path sourcePath) {
        Objects.requireNonNull(sourcePath, "sourcePath");
        String path = sourcePath.normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (path.endsWith(".java")) {
            return JAVA;
        }
        if (path.endsWith(".py") || path.endsWith(".pyi")) {
            return PYTHON;
        }
        if (path.endsWith(".yaml") || path.endsWith(".yml")) {
            return YAML;
        }
        if (path.endsWith(".json")) {
            return JSON;
        }
        if (path.endsWith(".properties")) {
            return PROPERTIES;
        }
        if (path.endsWith(".xml")
                || path.endsWith(".wsdl")
                || path.endsWith(".xhtml")
                || path.endsWith(".xsd")
                || path.endsWith(".xsl")
                || path.endsWith(".xslt")
                || path.endsWith(".tld")
                || path.endsWith(".xjb")) {
            return XML;
        }
        return TEXT;
    }
}
