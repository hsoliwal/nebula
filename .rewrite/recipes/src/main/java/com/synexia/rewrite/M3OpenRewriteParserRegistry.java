// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.openrewrite.Parser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.json.JsonParser;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.yaml.YamlParser;

/** Builds the exact OpenRewrite parser for one {@link M3SourceKind}. */
public final class M3OpenRewriteParserRegistry {
    private M3OpenRewriteParserRegistry() {}

    public static Parser parserFor(M3SourceKind kind, List<Path> classpath) {
        Objects.requireNonNull(kind, "kind");
        List<Path> checkedClasspath =
                List.copyOf(Objects.requireNonNull(classpath, "classpath"));
        checkedClasspath.forEach(path -> Objects.requireNonNull(path, "classpath entry"));

        return switch (kind) {
            case JAVA -> javaParser(checkedClasspath);
            case YAML -> YamlParser.builder().build();
            case XML -> XmlParser.builder().build();
            case JSON -> JsonParser.builder().build();
            case PROPERTIES -> PropertiesParser.builder().build();
            case PYTHON, TEXT -> PlainTextParser.builder().build();
        };
    }

    private static Parser javaParser(List<Path> classpath) {
        var builder = JavaParser.fromJavaVersion();
        if (!classpath.isEmpty()) {
            builder.classpath(classpath);
        }
        return builder.build();
    }
}
