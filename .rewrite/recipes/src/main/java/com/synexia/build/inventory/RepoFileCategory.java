// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.Locale;
import java.util.Set;

/** Stable primary role assigned to each retained repository file. */
public enum RepoFileCategory {
  SOURCE(1, "source"),
  SCHEMA(2, "schema"),
  CONFIGURATION(3, "configuration"),
  RESOURCE(4, "resource"),
  DOCUMENTATION(5, "documentation"),
  DATA(6, "data"),
  BUILD(7, "build"),
  WORKFLOW(8, "workflow"),
  BINARY(9, "binary"),
  OTHER(10, "other");

  private static final Set<String> SCHEMA_EXTENSIONS =
      Set.of(
          ".avsc", ".dtd", ".gql", ".graphql", ".json-schema", ".jsonschema", ".proto", ".schema",
          ".thrift", ".wsdl", ".xsd");
  private static final Set<String> SCHEMA_DOCUMENT_EXTENSIONS =
      Set.of(".json", ".xml", ".yaml", ".yml");
  private static final Set<String> DOCUMENTATION_EXTENSIONS =
      Set.of(".adoc", ".asciidoc", ".markdown", ".md", ".mdx", ".org", ".rst");
  private static final Set<String> DATA_EXTENSIONS =
      Set.of(
          ".csv", ".dat", ".dict", ".dictionary", ".jsonl", ".lex", ".lexicon", ".lst", ".ndjson",
          ".parquet", ".tsv");
  private static final Set<String> CONFIGURATION_EXTENSIONS =
      Set.of(
          ".args", ".cfg", ".conf", ".config", ".editorconfig", ".env", ".factorypath",
          ".gitattributes", ".gitignore", ".gitmodules", ".ini", ".json", ".jsonc", ".lock", ".mod",
          ".npmrc", ".nvmrc", ".properties", ".py-version", ".service", ".spdx", ".sum", ".toml",
          ".xml", ".yaml", ".yml");
  private static final Set<String> RESOURCE_EXTENSIONS =
      Set.of(
          ".bmp", ".css", ".eot", ".ftl", ".gif", ".hbs", ".html", ".htm", ".ico", ".jpeg", ".jpg",
          ".jinja", ".less", ".mp3", ".mp4", ".mustache", ".ogg", ".otf", ".png", ".scss", ".svelte",
          ".svg", ".tif", ".tiff", ".ttf", ".vue", ".wav", ".webm", ".webp", ".woff", ".woff2",
          ".xhtml");
  private static final Set<String> BUILD_NAMES =
      Set.of(
          "build.gradle", "build.gradle.kts", "build.xml", "cmakelists.txt", "containerfile",
          "dockerfile", "gnumakefile", "gradlew", "makefile", "meson.build", "mvnw", "pom.xml",
          "settings.gradle", "settings.gradle.kts");

  private final int code;
  private final String text;

  RepoFileCategory(int code, String text) {
    this.code = code;
    this.text = text;
  }

  public int code() {
    return code;
  }

  public String text() {
    return text;
  }

  /** Classifies using normalized path, extension, and the scanner's bounded text detection. */
  public static RepoFileCategory classify(String path, String extension, boolean textual) {
    String normalizedPath =
        java.util.Objects.requireNonNull(path, "path")
            .replace('\\', '/')
            .toLowerCase(Locale.ROOT);
    String checkedExtension =
        java.util.Objects.requireNonNull(extension, "extension").toLowerCase(Locale.ROOT);
    String name = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);

    if (containsPath(normalizedPath, "/.github/workflows/")) return WORKFLOW;
    if (BUILD_NAMES.contains(name) || checkedExtension.equals(".gradle")) return BUILD;
    if (SCHEMA_EXTENSIONS.contains(checkedExtension)
        || (hasSchemaPath(normalizedPath) && SCHEMA_DOCUMENT_EXTENSIONS.contains(checkedExtension))
        || name.endsWith(".schema.json")) {
      return SCHEMA;
    }
    if (isDocumentationPath(normalizedPath)
        || DOCUMENTATION_EXTENSIONS.contains(checkedExtension)
        || isDocumentationName(name)) {
      return DOCUMENTATION;
    }
    RepoLanguage language = RepoLanguage.fromExtension(checkedExtension, textual);
    if (DATA_EXTENSIONS.contains(checkedExtension)
        || (hasDataPath(normalizedPath) && !language.isSource())) {
      return DATA;
    }
    if (language.isSource()) return SOURCE;
    if (RESOURCE_EXTENSIONS.contains(checkedExtension)) return RESOURCE;
    if (CONFIGURATION_EXTENSIONS.contains(checkedExtension)
        || isConfigurationPath(normalizedPath)
        || isConfigurationName(name)) {
      return CONFIGURATION;
    }
    return textual ? OTHER : BINARY;
  }

  private static boolean containsPath(String path, String segmentPath) {
    return ("/" + path + "/").contains(segmentPath);
  }

  private static boolean hasSchemaPath(String path) {
    return hasSegment(path, "schema")
        || hasSegment(path, "schemas")
        || hasSegment(path, "openapi")
        || hasSegment(path, "swagger");
  }

  private static boolean hasDataPath(String path) {
    return hasSegment(path, "data")
        || hasSegment(path, "dataset")
        || hasSegment(path, "datasets")
        || hasSegment(path, "dictionary")
        || hasSegment(path, "dictionaries")
        || hasSegment(path, "lexicon")
        || hasSegment(path, "lexicons")
        || hasSegment(path, "precomputed");
  }

  private static boolean isDocumentationPath(String path) {
    return hasSegment(path, "doc")
        || hasSegment(path, "docs")
        || hasSegment(path, "documentation")
        || hasSegment(path, "license")
        || hasSegment(path, "licenses");
  }

  private static boolean isConfigurationPath(String path) {
    return hasSegment(path, "config")
        || hasSegment(path, "configuration")
        || hasSegment(path, "configs")
        || hasSegment(path, ".github");
  }

  private static boolean hasSegment(String path, String segment) {
    return ("/" + path + "/").contains("/" + segment + "/");
  }

  private static boolean isDocumentationName(String name) {
    return name.equals("readme")
        || name.startsWith("readme.")
        || name.equals("license")
        || name.startsWith("license.")
        || name.equals("licence")
        || name.startsWith("licence.")
        || name.equals("notice")
        || name.startsWith("notice.")
        || name.startsWith("changelog.")
        || name.startsWith("contributing.")
        || name.startsWith("code_of_conduct.")
        || name.equals("copying");
  }

  private static boolean isConfigurationName(String name) {
    return name.equals(".editorconfig")
        || name.equals(".dockerignore")
        || name.equals(".env")
        || name.startsWith(".env.")
        || name.equals(".gitmodules")
        || name.equals("cargo.toml")
        || name.equals("cargo.lock")
        || name.equals("package.json")
        || name.equals("package-lock.json")
        || name.equals("requirements.txt")
        || name.equals("go.mod")
        || name.equals("go.sum")
        || name.equals("pyproject.toml");
  }
}
