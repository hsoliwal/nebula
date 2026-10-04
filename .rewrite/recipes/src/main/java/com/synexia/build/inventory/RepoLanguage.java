// SPDX-License-Identifier: Apache-2.0
package com.synexia.build.inventory;

import java.util.Locale;
import java.util.Map;

/** Stable lexical language/format family for a repository file; not an AST or API claim. */
public enum RepoLanguage {
  UNKNOWN(0, "unknown", false),
  JAVA(1, "java", true),
  TYPESCRIPT(2, "typescript", true),
  JAVASCRIPT(3, "javascript", true),
  PYTHON(4, "python", true),
  GO(5, "go", true),
  RUST(6, "rust", true),
  C(7, "c", true),
  CPP(8, "cpp", true),
  CSHARP(9, "csharp", true),
  KOTLIN(10, "kotlin", true),
  SCALA(11, "scala", true),
  GROOVY(12, "groovy", true),
  RUBY(13, "ruby", true),
  PHP(14, "php", true),
  SWIFT(15, "swift", true),
  SHELL(16, "shell", true),
  SQL(17, "sql", true),
  HTML(18, "html", false),
  CSS(19, "css", false),
  XML(20, "xml", false),
  JSON(21, "json", false),
  YAML(22, "yaml", false),
  TOML(23, "toml", false),
  PROTOBUF(24, "protobuf", false),
  GRAPHQL(25, "graphql", false),
  MARKDOWN(26, "markdown", false),
  PLAIN_TEXT(27, "plain_text", false),
  HCL(28, "hcl", false),
  DART(29, "dart", true),
  R(30, "r", true),
  PERL(31, "perl", true),
  LUA(32, "lua", true),
  CLOJURE(33, "clojure", true),
  ELIXIR(34, "elixir", true),
  ERLANG(35, "erlang", true),
  D(36, "d", true),
  WEB_TEMPLATE(37, "web_template", false),
  OBJECTIVE_C(38, "objective_c", true);

  private static final Map<String, RepoLanguage> BY_EXTENSION =
      Map.ofEntries(
          Map.entry(".java", JAVA),
          Map.entry(".ts", TYPESCRIPT),
          Map.entry(".tsx", TYPESCRIPT),
          Map.entry(".js", JAVASCRIPT),
          Map.entry(".jsx", JAVASCRIPT),
          Map.entry(".mjs", JAVASCRIPT),
          Map.entry(".cjs", JAVASCRIPT),
          Map.entry(".py", PYTHON),
          Map.entry(".pyi", PYTHON),
          Map.entry(".go", GO),
          Map.entry(".rs", RUST),
          Map.entry(".c", C),
          Map.entry(".h", C),
          Map.entry(".cc", CPP),
          Map.entry(".cpp", CPP),
          Map.entry(".cxx", CPP),
          Map.entry(".hh", CPP),
          Map.entry(".hpp", CPP),
          Map.entry(".hxx", CPP),
          Map.entry(".cs", CSHARP),
          Map.entry(".kt", KOTLIN),
          Map.entry(".kts", KOTLIN),
          Map.entry(".scala", SCALA),
          Map.entry(".groovy", GROOVY),
          Map.entry(".gradle", GROOVY),
          Map.entry(".rb", RUBY),
          Map.entry(".php", PHP),
          Map.entry(".swift", SWIFT),
          Map.entry(".sh", SHELL),
          Map.entry(".bash", SHELL),
          Map.entry(".zsh", SHELL),
          Map.entry(".ps1", SHELL),
          Map.entry(".bat", SHELL),
          Map.entry(".cmd", SHELL),
          Map.entry(".sql", SQL),
          Map.entry(".html", HTML),
          Map.entry(".htm", HTML),
          Map.entry(".xhtml", HTML),
          Map.entry(".css", CSS),
          Map.entry(".scss", CSS),
          Map.entry(".less", CSS),
          Map.entry(".xml", XML),
          Map.entry(".svg", XML),
          Map.entry(".xsd", XML),
          Map.entry(".wsdl", XML),
          Map.entry(".json", JSON),
          Map.entry(".jsonc", JSON),
          Map.entry(".jsonl", JSON),
          Map.entry(".ndjson", JSON),
          Map.entry(".avsc", JSON),
          Map.entry(".yaml", YAML),
          Map.entry(".yml", YAML),
          Map.entry(".toml", TOML),
          Map.entry(".proto", PROTOBUF),
          Map.entry(".graphql", GRAPHQL),
          Map.entry(".gql", GRAPHQL),
          Map.entry(".md", MARKDOWN),
          Map.entry(".markdown", MARKDOWN),
          Map.entry(".mdx", MARKDOWN),
          Map.entry(".rst", MARKDOWN),
          Map.entry(".adoc", MARKDOWN),
          Map.entry(".asciidoc", MARKDOWN),
          Map.entry(".tf", HCL),
          Map.entry(".hcl", HCL),
          Map.entry(".dart", DART),
          Map.entry(".r", R),
          Map.entry(".pl", PERL),
          Map.entry(".pm", PERL),
          Map.entry(".lua", LUA),
          Map.entry(".clj", CLOJURE),
          Map.entry(".cljs", CLOJURE),
          Map.entry(".cljc", CLOJURE),
          Map.entry(".ex", ELIXIR),
          Map.entry(".exs", ELIXIR),
          Map.entry(".erl", ERLANG),
          Map.entry(".hrl", ERLANG),
          Map.entry(".d", D),
          Map.entry(".m", OBJECTIVE_C),
          Map.entry(".mm", OBJECTIVE_C),
          Map.entry(".vue", WEB_TEMPLATE),
          Map.entry(".svelte", WEB_TEMPLATE));

  private final int code;
  private final String text;
  private final boolean source;

  RepoLanguage(int code, String text, boolean source) {
    this.code = code;
    this.text = text;
    this.source = source;
  }

  public int code() {
    return code;
  }

  public String text() {
    return text;
  }

  public boolean isSource() {
    return source;
  }

  /** Returns a stable language family, using plain text for unknown but detected text files. */
  public static RepoLanguage fromExtension(String extension, boolean textual) {
    String checked =
        java.util.Objects.requireNonNull(extension, "extension").toLowerCase(Locale.ROOT);
    RepoLanguage language = BY_EXTENSION.get(checked);
    if (language != null) return language;
    if (checked.equals(".txt") || checked.equals(".text")) return PLAIN_TEXT;
    return textual ? PLAIN_TEXT : UNKNOWN;
  }
}
