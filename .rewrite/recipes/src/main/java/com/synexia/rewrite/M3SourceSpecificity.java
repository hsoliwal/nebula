// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Source-specific routing for M3 atomization/patternization.
 *
 * <p>Code, native code, build descriptors, documentation, structured configuration and generic
 * text are deliberately separated so Java AST recipes are never applied to Markdown or other
 * prose.</p>
 */
public enum M3SourceSpecificity {
    JAVA("JAVA_AST", "DECLARATION_AND_STATEMENT", true, true),
    JNI_CPP("NATIVE_CPP", "DECLARATION_AND_FUNCTION", true, true),
    C_CPP("NATIVE_CPP", "DECLARATION_AND_FUNCTION", true, true),
    MAVEN_POM("MAVEN_XML", "MODEL_AND_PLUGIN", true, true),
    GRADLE("BUILD_TEXT", "BLOCK_AND_DEPENDENCY", true, true),
    MARKDOWN("DOCUMENT_TEXT", "HEADING_SECTION_PARAGRAPH_CODE_FENCE", false, true),
    ASCIIDOC("DOCUMENT_TEXT", "HEADING_SECTION_PARAGRAPH_CODE_FENCE", false, true),
    YAML("STRUCTURED_DATA", "PATH_AND_NODE", false, true),
    XML("STRUCTURED_DATA", "PATH_AND_ELEMENT", false, true),
    JSON("STRUCTURED_DATA", "PATH_AND_NODE", false, true),
    PROPERTIES("STRUCTURED_DATA", "KEY_VALUE", false, true),
    SHELL("SCRIPT_TEXT", "COMMAND_AND_BLOCK", false, true),
    PYTHON("PYTHON_TEXT", "DECLARATION_AND_BLOCK", false, true),
    TEXT("PLAIN_TEXT", "LINE_AND_SECTION", false, false),
    DATA("DATA_TEXT", "ROW_AND_FIELD", false, false);

    private final String parserLane;
    private final String atomGranularity;
    private final boolean code;
    private final boolean patternizable;

    M3SourceSpecificity(
            final String parserLane,
            final String atomGranularity,
            final boolean code,
            final boolean patternizable) {
        this.parserLane = parserLane;
        this.atomGranularity = atomGranularity;
        this.code = code;
        this.patternizable = patternizable;
    }

    public String parserLane() {
        return parserLane;
    }

    public String atomGranularity() {
        return atomGranularity;
    }

    public boolean code() {
        return code;
    }

    public boolean documentation() {
        return this == MARKDOWN || this == ASCIIDOC;
    }

    public boolean patternizable() {
        return patternizable;
    }

    public static M3SourceSpecificity classify(final String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank() || sourcePath.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourcePath required");
        }
        return classify(Path.of(sourcePath.replace('\\', '/')));
    }

    public static M3SourceSpecificity classify(final Path sourcePath) {
        Objects.requireNonNull(sourcePath, "sourcePath");
        final String path =
                sourcePath.normalize().toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        final String name = sourcePath.getFileName() == null
                ? path
                : sourcePath.getFileName().toString().toLowerCase(Locale.ROOT);

        if (path.endsWith(".java")) return JAVA;
        if (path.endsWith(".cc") || path.endsWith(".cpp") || path.endsWith(".cxx")
                || path.endsWith(".hh") || path.endsWith(".hpp") || path.endsWith(".hxx")) {
            return path.contains("/jni/") || path.contains("-jni/") || path.contains("/native/")
                    ? JNI_CPP : C_CPP;
        }
        if (path.endsWith(".c") || path.endsWith(".h")) {
            return path.contains("/jni/") || path.contains("-jni/") || path.contains("/native/")
                    ? JNI_CPP : C_CPP;
        }
        if ("pom.xml".equals(name)) return MAVEN_POM;
        if ("build.gradle".equals(name) || "build.gradle.kts".equals(name)
                || "settings.gradle".equals(name) || "settings.gradle.kts".equals(name)) {
            return GRADLE;
        }
        if (path.endsWith(".md") || path.endsWith(".markdown")) return MARKDOWN;
        if (path.endsWith(".adoc") || path.endsWith(".asciidoc")) return ASCIIDOC;
        if (path.endsWith(".yaml") || path.endsWith(".yml")) return YAML;
        if (path.endsWith(".json")) return JSON;
        if (path.endsWith(".properties")) return PROPERTIES;
        if (path.endsWith(".xml") || path.endsWith(".xsd") || path.endsWith(".xsl")
                || path.endsWith(".xslt") || path.endsWith(".wsdl")) return XML;
        if (path.endsWith(".sh") || path.endsWith(".bash") || path.endsWith(".cmd")
                || path.endsWith(".bat") || path.endsWith(".ps1")) return SHELL;
        if (path.endsWith(".py") || path.endsWith(".pyi")) return PYTHON;
        if (path.endsWith(".csv") || path.endsWith(".tsv") || path.endsWith(".ndjson")) return DATA;
        return TEXT;
    }
}
