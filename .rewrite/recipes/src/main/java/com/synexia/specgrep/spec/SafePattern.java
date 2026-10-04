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
package com.synexia.specgrep.spec;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.Stream;

/**
 * Safe regular-expression wrapper that prefers RE2/J and falls back to the JDK engine only when
 * the requested expression is outside RE2/J's supported grammar.
 *
 * <p>The selected engine is explicit through {@link #engineName()} and {@link #isUsingRE2()}.
 * This leaf performs no logging and owns no policy beyond regex compilation/matching.</p>
 */
public final class SafePattern {

    public static final int CASE_INSENSITIVE = java.util.regex.Pattern.CASE_INSENSITIVE;
    public static final int MULTILINE = java.util.regex.Pattern.MULTILINE;
    public static final int DOTALL = java.util.regex.Pattern.DOTALL;
    public static final int LITERAL = java.util.regex.Pattern.LITERAL;
    public static final int UNICODE_CASE = java.util.regex.Pattern.UNICODE_CASE;
    public static final int COMMENTS = java.util.regex.Pattern.COMMENTS;
    public static final int UNIX_LINES = java.util.regex.Pattern.UNIX_LINES;

    private static final String ENGINE_RE2 = "RE2 (safe)";
    private static final String ENGINE_JAVA = "Java regex (fallback)";

    private final String pattern;
    private final com.google.re2j.Pattern re2Pattern;
    private final java.util.regex.Pattern javaPattern;
    private final boolean usingRE2;

    private SafePattern(
            String pattern,
            com.google.re2j.Pattern re2Pattern,
            java.util.regex.Pattern javaPattern,
            boolean usingRE2) {
        this.pattern = pattern;
        this.re2Pattern = re2Pattern;
        this.javaPattern = javaPattern;
        this.usingRE2 = usingRE2;
    }

    /** Compiles a pattern with JDK-compatible flags. */
    public static SafePattern compile(String regex, int flags) {
        Objects.requireNonNull(regex, "regex");
        if ((flags & LITERAL) != 0) {
            try {
                return new SafePattern(
                        regex,
                        com.google.re2j.Pattern.compile(com.google.re2j.Pattern.quote(regex)),
                        null,
                        true);
            } catch (RuntimeException unsupported) {
                return new SafePattern(regex, null, java.util.regex.Pattern.compile(regex, flags), false);
            }
        }

        String re2Regex = regex;
        if ((flags & CASE_INSENSITIVE) != 0) {
            re2Regex = "(?i)" + re2Regex;
        }
        if ((flags & MULTILINE) != 0) {
            re2Regex = "(?m)" + re2Regex;
        }
        if ((flags & DOTALL) != 0) {
            re2Regex = "(?s)" + re2Regex;
        }
        try {
            return new SafePattern(regex, com.google.re2j.Pattern.compile(re2Regex), null, true);
        } catch (RuntimeException unsupported) {
            return new SafePattern(regex, null, java.util.regex.Pattern.compile(regex, flags), false);
        }
    }

    /** Compiles with multiline JDK fallback semantics. */
    public static SafePattern compile(String regex) {
        Objects.requireNonNull(regex, "regex");
        try {
            return new SafePattern(regex, com.google.re2j.Pattern.compile(regex), null, true);
        } catch (RuntimeException unsupported) {
            return new SafePattern(regex, null, java.util.regex.Pattern.compile(regex, MULTILINE), false);
        }
    }

    public static String quote(String literal) {
        return java.util.regex.Pattern.quote(literal);
    }

    public static java.util.regex.Pattern compileToJava(String regex) {
        return compile(regex).toJavaPattern();
    }

    public String[] split(String input) {
        return split(input, 0);
    }

    public String[] split(String input, int limit) {
        Objects.requireNonNull(input, "input");
        return re2Pattern != null ? re2Pattern.split(input, limit) : javaPattern.split(input, limit);
    }

    public SafeMatcher matcher(String input) {
        Objects.requireNonNull(input, "input");
        return usingRE2
                ? new SafeMatcher(re2Pattern.matcher(input), null, true)
                : new SafeMatcher(null, javaPattern.matcher(input), false);
    }

    public String pattern() {
        return pattern;
    }

    public boolean isUsingRE2() {
        return usingRE2;
    }

    public String engineName() {
        return usingRE2 ? ENGINE_RE2 : ENGINE_JAVA;
    }

    public java.util.regex.Pattern toJavaPattern() {
        return javaPattern != null ? javaPattern : java.util.regex.Pattern.compile(pattern, MULTILINE);
    }

    /** Matcher facade over the selected regex engine. */
    public static final class SafeMatcher {
        private final com.google.re2j.Matcher re2Matcher;
        private final java.util.regex.Matcher javaMatcher;
        private final boolean usingRE2;

        private SafeMatcher(
                com.google.re2j.Matcher re2Matcher,
                Matcher javaMatcher,
                boolean usingRE2) {
            this.re2Matcher = re2Matcher;
            this.javaMatcher = javaMatcher;
            this.usingRE2 = usingRE2;
        }

        public boolean find() {
            return usingRE2 ? re2Matcher.find() : javaMatcher.find();
        }

        public String group(int group) {
            return usingRE2 ? re2Matcher.group(group) : javaMatcher.group(group);
        }

        public String group() {
            return usingRE2 ? re2Matcher.group() : javaMatcher.group();
        }

        public String group(String name) {
            return usingRE2 ? null : javaMatcher.group(name);
        }

        public int groupCount() {
            return usingRE2 ? re2Matcher.groupCount() : javaMatcher.groupCount();
        }

        public boolean matches() {
            return usingRE2 ? re2Matcher.matches() : javaMatcher.matches();
        }

        public String replaceAll(String replacement) {
            return usingRE2
                    ? re2Matcher.replaceAll(replacement)
                    : javaMatcher.replaceAll(replacement);
        }

        public String replaceAll(java.util.function.Function<java.util.regex.MatchResult, String> function) {
            if (usingRE2) {
                throw new UnsupportedOperationException("replaceAll(Function) not supported by RE2/J facade");
            }
            return javaMatcher.replaceAll(function);
        }

        public String replaceFirst(String replacement) {
            return usingRE2
                    ? re2Matcher.replaceFirst(replacement)
                    : javaMatcher.replaceFirst(replacement);
        }

        public SafeMatcher reset(String input) {
            Objects.requireNonNull(input, "input");
            if (usingRE2) {
                re2Matcher.reset(input);
            } else {
                javaMatcher.reset(input);
            }
            return this;
        }

        public int start() {
            return usingRE2 ? re2Matcher.start() : javaMatcher.start();
        }

        public int end() {
            return usingRE2 ? re2Matcher.end() : javaMatcher.end();
        }

        public int start(int group) {
            return usingRE2 ? re2Matcher.start(group) : javaMatcher.start(group);
        }

        public int end(int group) {
            return usingRE2 ? re2Matcher.end(group) : javaMatcher.end(group);
        }

        public boolean requireEnd() {
            return !usingRE2 && javaMatcher.requireEnd();
        }

        public boolean lookingAt() {
            return usingRE2 ? re2Matcher.lookingAt() : javaMatcher.lookingAt();
        }

        public static String quoteReplacement(String value) {
            return java.util.regex.Matcher.quoteReplacement(value);
        }

        /** Returns immutable match snapshots as a stream. */
        public Stream<java.util.regex.MatchResult> results() {
            if (!usingRE2) {
                return javaMatcher.results();
            }
            ArrayList<java.util.regex.MatchResult> matches = new ArrayList<>();
            while (find()) {
                int start = start();
                int end = end();
                int count = groupCount();
                String[] groups = new String[count + 1];
                for (int index = 0; index <= count; index++) {
                    groups[index] = group(index);
                }
                matches.add(new ImmutableMatchResult(start, end, groups));
            }
            return matches.stream();
        }

        public SafeMatcher appendReplacement(StringBuffer buffer, String replacement) {
            if (usingRE2) {
                re2Matcher.appendReplacement(buffer, replacement);
            } else {
                javaMatcher.appendReplacement(buffer, replacement);
            }
            return this;
        }

        public StringBuffer appendTail(StringBuffer buffer) {
            if (usingRE2) {
                re2Matcher.appendTail(buffer);
            } else {
                javaMatcher.appendTail(buffer);
            }
            return buffer;
        }
    }

    private static final class ImmutableMatchResult implements java.util.regex.MatchResult {
        private final int start;
        private final int end;
        private final String[] groups;

        private ImmutableMatchResult(int start, int end, String[] groups) {
            this.start = start;
            this.end = end;
            this.groups = groups.clone();
        }

        @Override
        public int start() {
            return start;
        }

        @Override
        public int start(int group) {
            return group == 0 ? start : -1;
        }

        @Override
        public int end() {
            return end;
        }

        @Override
        public int end(int group) {
            return group == 0 ? end : -1;
        }

        @Override
        public String group() {
            return groups[0];
        }

        @Override
        public String group(int group) {
            return groups[group];
        }

        @Override
        public int groupCount() {
            return groups.length - 1;
        }
    }

    /** Small diagnostic entry point; never used as production authority. */
    public static void main(String[] args) {
        if (args.length < 2) {
            return;
        }
        SafePattern pattern = compile(args[0]);
        SafeMatcher matcher = pattern.matcher(args[1]);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        System.out.printf("matches=%d engine=%s%n", count, pattern.engineName());
    }
}
