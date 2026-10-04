// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.config.Environment;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;

/** Deterministic view over class-backed recipes visible on the active OpenRewrite classpath. */
public final class M3OpenRewriteInventory {
    public record Option(String name, String type, boolean required) implements Comparable<Option> {
        public Option {
            name = text(name, "name");
            type = text(type, "type");
        }

        @Override
        public int compareTo(Option other) {
            return name.compareTo(other.name);
        }
    }

    public record Entry(
            String name,
            String displayName,
            Set<String> tags,
            List<Option> options)
            implements Comparable<Entry> {
        public Entry {
            name = text(name, "name");
            displayName = text(displayName, "displayName");
            tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
            options = Objects.requireNonNull(options, "options").stream().sorted().toList();
        }

        @Override
        public int compareTo(Entry other) {
            return name.compareTo(other.name);
        }
    }

    private M3OpenRewriteInventory() {}

    /**
     * Inventory all recipes visible in the accepted packages.
     *
     * <p>An empty package list scans the whole active runtime classpath. The returned order is
     * stable by recipe name so catalogue hashes do not depend on classpath discovery order.</p>
     */
    public static List<Entry> discover(String... acceptedPackages) {
        String[] packages = acceptedPackages == null ? new String[0] : acceptedPackages.clone();
        Environment environment = Environment.builder().scanRuntimeClasspath(packages).build();
        return environment.listRecipeDescriptors().stream()
                .map(M3OpenRewriteInventory::entry)
                .sorted()
                .toList();
    }

    public static Entry require(List<Entry> entries, String recipeName) {
        Objects.requireNonNull(entries, "entries");
        String checked = text(recipeName, "recipeName");
        return entries.stream()
                .filter(entry -> entry.name().equals(checked))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("recipe not found: " + checked));
    }

    public static String toTsv(List<Entry> entries) {
        StringBuilder out = new StringBuilder("name\tdisplay_name\ttags\toptions\n");
        Objects.requireNonNull(entries, "entries").stream()
                .sorted(Comparator.naturalOrder())
                .forEach(entry -> out.append(safe(entry.name()))
                        .append('\t')
                        .append(safe(entry.displayName()))
                        .append('\t')
                        .append(safe(entry.tags().stream().sorted().toList().toString()))
                        .append('\t')
                        .append(safe(entry.options().toString()))
                        .append('\n'));
        return out.toString();
    }

    private static Entry entry(RecipeDescriptor descriptor) {
        List<Option> options = descriptor.getOptions().stream()
                .map(M3OpenRewriteInventory::option)
                .toList();
        return new Entry(
                descriptor.getName(),
                descriptor.getDisplayName(),
                descriptor.getTags(),
                options);
    }

    private static Option option(OptionDescriptor descriptor) {
        return new Option(descriptor.getName(), descriptor.getType(), descriptor.isRequired());
    }

    private static String safe(String value) {
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return value.strip();
    }
}
