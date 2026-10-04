/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.synexia.specgrep;

import com.synexia.specgrep.spec.SafePattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Extracts deterministic semantic inventory facts from Java source documentation.
 *
 * <p>This is a lexical/build-time extractor. It does not compile source, load application classes,
 * execute reflection, infer runtime behavior, or grant operational authority. It inventories
 * module/package names, documented classes/interfaces, method signatures and Javadoc contracts.
 * The source file remains authoritative.</p>
 *
 * @since 1.0
 */
public final class DocumentationExtractor {

    private static final SafePattern JAVADOC = SafePattern.compile("/\\*\\*[\\s\\S]*?\\*/");
    private static final SafePattern MODULE_PATTERN = SafePattern.compile("module\\s+(\\S+)\\s*\\{");
    private static final SafePattern PACKAGE_PATTERN = SafePattern.compile("package\\s+([\\w.]+);");
    private static final SafePattern CLASS_PATTERN = SafePattern.compile(
            "(?:public|private|protected)?\\s*(?:static)?\\s*(?:final)?\\s*(?:abstract)?\\s*class\\s+(\\w+)");
    private static final SafePattern INTERFACE_PATTERN =
            SafePattern.compile("(?:public|private|protected)?\\s*interface\\s+(\\w+)");
    private static final SafePattern METHOD_PATTERN = SafePattern.compile(
            "(?:public|private|protected)?\\s*(?:static)?\\s*(?:default)?\\s*"
                    + "(?:<[^>]+>\\s*)?([\\w.$<>?\\[\\], ]+)\\s+(\\w+)\\s*\\(([^)]*)\\)");

    /**
     * Extracts a semantic mapping from one UTF-8 Java source file.
     *
     * @param file source file
     * @return deterministic semantic mapping in source order
     * @throws Exception if the file cannot be read
     */
    public SemanticMapping extract(Path file) throws Exception {
        Objects.requireNonNull(file, "file");
        return extractContent(Files.readString(file));
    }

    /**
     * Extracts a semantic mapping from supplied source text.
     *
     * @param content Java source text
     * @return semantic mapping in source order
     */
    public SemanticMapping extractContent(String content) {
        Objects.requireNonNull(content, "content");
        return new SemanticMapping(
                extractModuleName(content),
                extractPackageName(content),
                extractPackagePurpose(content),
                extractClasses(content),
                extractInterfaces(content),
                extractMethods(content));
    }

    private static String extractModuleName(String content) {
        SafePattern.SafeMatcher matcher = MODULE_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractPackageName(String content) {
        SafePattern.SafeMatcher matcher = PACKAGE_PATTERN.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractPackagePurpose(String content) {
        SafePattern.SafeMatcher matcher = JAVADOC.matcher(content);
        return matcher.find() ? cleanJavadoc(matcher.group()) : null;
    }

    private static List<ClassMapping> extractClasses(String content) {
        return CLASS_PATTERN.matcher(content).results().map(match -> {
            String javadoc = findJavadocBefore(content, match.start());
            Map<String, String> tags = extractJavadocTags(javadoc);
            return new ClassMapping(
                    match.group(1),
                    cleanJavadoc(javadoc),
                    valuesFor(tags, "see"),
                    valuesFor(tags, "author"),
                    firstValue(tags, "since"),
                    tags);
        }).toList();
    }

    private static List<InterfaceMapping> extractInterfaces(String content) {
        return INTERFACE_PATTERN.matcher(content).results().map(match -> {
            String javadoc = findJavadocBefore(content, match.start());
            Map<String, String> tags = extractJavadocTags(javadoc);
            return new InterfaceMapping(
                    match.group(1),
                    cleanJavadoc(javadoc),
                    valuesFor(tags, "implNote"),
                    extractMethodSignatures(content, match.end()),
                    tags);
        }).toList();
    }

    private static List<MethodMapping> extractMethods(String content) {
        return METHOD_PATTERN.matcher(content).results().map(match -> {
            String javadoc = findJavadocBefore(content, match.start());
            Map<String, String> tags = extractJavadocTags(javadoc);
            return new MethodMapping(
                    match.group(2),
                    normalizeType(match.group(1)),
                    cleanJavadoc(javadoc),
                    extractParameters(match.group(3), tags),
                    firstValue(tags, "return"),
                    exceptionValues(tags),
                    tags);
        }).toList();
    }

    private static String findJavadocBefore(String content, int position) {
        int start = content.lastIndexOf("/**", position);
        if (start < 0) {
            return "";
        }
        int end = content.indexOf("*/", start);
        if (end < 0 || end > position) {
            return "";
        }
        String between = content.substring(end + 2, position).trim();
        if (!between.isEmpty() && !between.matches("(?:@[\\w.$]+(?:\\([^)]*\\))?\\s*)*")) {
            return "";
        }
        return content.substring(start, end + 2);
    }

    private static Map<String, String> extractJavadocTags(String javadoc) {
        LinkedHashMap<String, String> tags = new LinkedHashMap<>();
        String currentTag = null;
        StringBuilder currentBody = new StringBuilder();
        for (String line : javadocLines(javadoc)) {
            if (line.startsWith("@")) {
                if (currentTag != null) {
                    addTag(tags, currentTag, currentBody.toString());
                }
                int split = firstWhitespace(line, 1);
                currentTag = split < 0 ? line.substring(1) : line.substring(1, split);
                currentBody = new StringBuilder(split < 0 ? "" : line.substring(split + 1).trim());
            } else if (currentTag != null && !line.isBlank()) {
                if (!currentBody.isEmpty()) {
                    currentBody.append(' ');
                }
                currentBody.append(line.trim());
            }
        }
        if (currentTag != null) {
            addTag(tags, currentTag, currentBody.toString());
        }
        return Map.copyOf(tags);
    }

    private static void addTag(LinkedHashMap<String, String> tags, String tag, String rawBody) {
        String body = normalizeWhitespace(rawBody);
        String key = canonicalTagKey(tag, body);
        String value = canonicalTagValue(tag, body);
        putUnique(tags, key, value);
    }

    private static int firstWhitespace(String value, int start) {
        for (int index = start; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private static String canonicalTagKey(String tag, String body) {
        if (("param".equals(tag) || "throws".equals(tag) || "exception".equals(tag)) && !body.isBlank()) {
            int split = body.indexOf(' ');
            String subject = split < 0 ? body : body.substring(0, split);
            return tag + " " + subject;
        }
        return tag;
    }

    private static String canonicalTagValue(String tag, String body) {
        if (("param".equals(tag) || "throws".equals(tag) || "exception".equals(tag)) && !body.isBlank()) {
            int split = body.indexOf(' ');
            return split < 0 ? "" : body.substring(split + 1).trim();
        }
        return body;
    }

    private static void putUnique(LinkedHashMap<String, String> tags, String key, String value) {
        if (!tags.containsKey(key)) {
            tags.put(key, value);
            return;
        }
        int ordinal = 2;
        while (tags.containsKey(key + "#" + ordinal)) {
            ordinal++;
        }
        tags.put(key + "#" + ordinal, value);
    }

    private static String cleanJavadoc(String javadoc) {
        StringBuilder description = new StringBuilder();
        for (String line : javadocLines(javadoc)) {
            if (line.startsWith("@")) {
                break;
            }
            if (line.isBlank()) {
                continue;
            }
            if (!description.isEmpty()) {
                description.append(' ');
            }
            description.append(line.trim());
        }
        return normalizeWhitespace(description.toString());
    }

    private static List<String> javadocLines(String javadoc) {
        if (javadoc == null || javadoc.isBlank()) {
            return List.of();
        }
        String body = javadoc.trim();
        if (body.startsWith("/**")) {
            body = body.substring(3);
        }
        if (body.endsWith("*/")) {
            body = body.substring(0, body.length() - 2);
        }
        return body.lines().map(line -> {
            String normalized = line.stripLeading();
            if (normalized.startsWith("*")) {
                normalized = normalized.substring(1).stripLeading();
            }
            return normalized.stripTrailing();
        }).toList();
    }

    private static List<String> valuesFor(Map<String, String> tags, String key) {
        return tags.entrySet().stream()
                .filter(entry -> entry.getKey().equals(key) || entry.getKey().startsWith(key + "#"))
                .map(Map.Entry::getValue)
                .toList();
    }

    private static String firstValue(Map<String, String> tags, String key) {
        return tags.entrySet().stream()
                .filter(entry -> entry.getKey().equals(key) || entry.getKey().startsWith(key + "#"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static List<String> extractMethodSignatures(String content, int declarationEnd) {
        int openBrace = content.indexOf('{', declarationEnd);
        if (openBrace < 0) {
            return List.of();
        }
        int closeBrace = matchingBrace(content, openBrace);
        if (closeBrace < 0) {
            return List.of();
        }
        String body = content.substring(openBrace + 1, closeBrace);
        return METHOD_PATTERN.matcher(body).results()
                .map(match -> String.format(
                        "%s %s(%s)",
                        normalizeType(match.group(1)),
                        match.group(2),
                        normalizeWhitespace(match.group(3))))
                .toList();
    }

    private static int matchingBrace(String content, int openBrace) {
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        for (int index = openBrace; index < content.length(); index++) {
            char current = content.charAt(index);
            char next = index + 1 < content.length() ? content.charAt(index + 1) : '\0';

            if (inLineComment) {
                if (current == '\n') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    index++;
                }
                continue;
            }
            if (inString || inChar) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (current == '\\') {
                    escaped = true;
                    continue;
                }
                if (inString && current == '"') {
                    inString = false;
                } else if (inChar && current == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (current == '/' && next == '/') {
                inLineComment = true;
                index++;
                continue;
            }
            if (current == '/' && next == '*') {
                inBlockComment = true;
                index++;
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '\'') {
                inChar = true;
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static List<ParameterMapping> extractParameters(String params, Map<String, String> tags) {
        if (params == null || params.isBlank()) {
            return List.of();
        }
        return Arrays.stream(params.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(part -> part.split("\\s+"))
                .filter(tokens -> tokens.length >= 2)
                .map(tokens -> {
                    String name = tokens[tokens.length - 1];
                    String type = String.join(" ", Arrays.copyOf(tokens, tokens.length - 1));
                    return new ParameterMapping(name, normalizeType(type), tags.get("param " + name));
                })
                .toList();
    }

    private static List<String> exceptionValues(Map<String, String> tags) {
        return tags.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("throws ") || entry.getKey().startsWith("exception "))
                .map(entry -> {
                    int split = entry.getKey().indexOf(' ');
                    String type = entry.getKey().substring(split + 1).replaceFirst("#\\d+$", "");
                    return entry.getValue().isBlank() ? type : type + ": " + entry.getValue();
                })
                .toList();
    }

    private static String normalizeType(String value) {
        return normalizeWhitespace(value == null ? "" : value);
    }

    private static String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    /** Semantic mapping extracted from documentation. */
    public record SemanticMapping(
            String moduleName,
            String packageName,
            String packagePurpose,
            List<ClassMapping> classes,
            List<InterfaceMapping> interfaces,
            List<MethodMapping> methods) {
        public SemanticMapping {
            classes = List.copyOf(classes);
            interfaces = List.copyOf(interfaces);
            methods = List.copyOf(methods);
        }

        /** Converts the mapping to deterministic searchable text. */
        public String toSearchableText() {
            StringBuilder text = new StringBuilder();
            if (moduleName != null) {
                text.append("MODULE: ").append(moduleName).append('\n');
            }
            if (packageName != null) {
                text.append("PACKAGE: ").append(packageName).append('\n');
            }
            if (packagePurpose != null) {
                text.append("PACKAGE_PURPOSE: ").append(packagePurpose).append('\n');
            }
            classes.forEach(mapping -> {
                text.append("CLASS: ").append(mapping.name()).append('\n');
                text.append("CLASS_PURPOSE: ").append(mapping.purpose()).append('\n');
                mapping.seeAlso().forEach(value -> text.append("SEE_ALSO: ").append(value).append('\n'));
            });
            interfaces.forEach(mapping -> {
                text.append("INTERFACE: ").append(mapping.name()).append('\n');
                text.append("CONTRACT: ").append(mapping.contract()).append('\n');
                mapping.methods().forEach(value -> text.append("METHOD_SIGNATURE: ").append(value).append('\n'));
            });
            methods.forEach(mapping -> {
                text.append("METHOD: ").append(mapping.name()).append('\n');
                text.append("METHOD_DESC: ").append(mapping.description()).append('\n');
                mapping.parameters().forEach(parameter -> text.append("PARAM: ")
                        .append(parameter.name())
                        .append(" - ")
                        .append(parameter.doc() == null ? "" : parameter.doc())
                        .append('\n'));
            });
            return text.toString();
        }
    }

    /** Class/Javadoc inventory row. */
    public record ClassMapping(
            String name,
            String purpose,
            List<String> seeAlso,
            List<String> authors,
            String since,
            Map<String, String> allTags) {
        public ClassMapping {
            seeAlso = List.copyOf(seeAlso);
            authors = List.copyOf(authors);
            allTags = Map.copyOf(allTags);
        }
    }

    /** Interface/Javadoc inventory row. */
    public record InterfaceMapping(
            String name,
            String contract,
            List<String> implementors,
            List<String> methods,
            Map<String, String> allTags) {
        public InterfaceMapping {
            implementors = List.copyOf(implementors);
            methods = List.copyOf(methods);
            allTags = Map.copyOf(allTags);
        }
    }

    /** Method/Javadoc inventory row. */
    public record MethodMapping(
            String name,
            String returnType,
            String description,
            List<ParameterMapping> parameters,
            String returnDoc,
            List<String> exceptions,
            Map<String, String> allTags) {
        public MethodMapping {
            parameters = List.copyOf(parameters);
            exceptions = List.copyOf(exceptions);
            allTags = Map.copyOf(allTags);
        }
    }

    /** Parameter/Javadoc inventory row. */
    public record ParameterMapping(String name, String type, String doc) {}
}
