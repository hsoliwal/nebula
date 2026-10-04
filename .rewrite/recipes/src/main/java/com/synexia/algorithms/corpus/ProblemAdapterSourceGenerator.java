// SPDX-License-Identifier: Apache-2.0
package com.synexia.algorithms.corpus;

import com.synexia.algorithms.core.ProgressMonitors;
import com.synexia.job.IProgressMonitor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Deterministically materializes one Java wrapper class for every indexed corpus source file.
 *
 * <p>The generated wrappers contain no copied third-party implementation body. They preserve the
 * pinned provenance id and delegate execution to {@link ProblemAdapterCatalog}, which in turn uses
 * the canonical monitor-first donor selected for the problem shape.
 */
public final class ProblemAdapterSourceGenerator {

    public record GenerationResult(int files, int executable, int unresolved) {}

    private ProblemAdapterSourceGenerator() {}

    public static GenerationResult generate(Path sourceRoot, IProgressMonitor suppliedMonitor) {
        return generate(sourceRoot, ProblemAdapterCatalog.all(), suppliedMonitor);
    }

    /**
     * Export a complete, ordered projection of supplied adapters without changing their classification.
     * Use a fresh corpus-root directory for a changed snapshot. Unknowns remain explicit; executability
     * means only that a canonical-shape donor exists, not equivalence to the original challenge.
     */
    public static GenerationResult generate(
            Path sourceRoot, List<ProblemAdapter> suppliedAdapters, IProgressMonitor suppliedMonitor) {
        Objects.requireNonNull(sourceRoot, "sourceRoot");
        List<ProblemAdapter> adapters = List.copyOf(suppliedAdapters).stream()
                .sorted(Comparator.comparing(ProblemAdapter::id)).toList();
        Set<String> ids = new HashSet<>();
        Set<String> classes = new HashSet<>();
        for (ProblemAdapter adapter : adapters) {
            if (!ids.add(adapter.id()) || !classes.add(qualifiedClassName(adapter))) {
                throw new IllegalArgumentException("duplicate adapter identity or generated class: " + adapter.id());
            }
        }
        IProgressMonitor monitor = ProgressMonitors.nonNull(suppliedMonitor);
        monitor.beginTask("generate-problem-adapters", 2L * adapters.size());
        int executable = 0;
        int unresolved = 0;
        StringBuilder catalog = new StringBuilder(
                "source_id\tplatform\trepository\tcommit\tpath\tproblem\tshape\ttemplate"
                        + "\tconfidence\tcanonical_executable\tqualified_class\twrapper_sha256\tbinding_scope\trationale\n");
        TreeMap<String, Integer> categories = new TreeMap<>();
        try {
            for (ProblemAdapter adapter : adapters) {
                monitor.checkCanceled();
                boolean supported = adapter.executable();
                if (supported) executable++; else unresolved++;
                String source = sourceFor(adapter);
                String qualified = qualifiedClassName(adapter);
                String shape = adapter.classification().classified()
                        ? adapter.classification().shape().name() : "UNCLASSIFIED";
                String template = adapter.templateStyle().kind().name();
                String confidence = adapter.classification().confidence().name();
                categories.merge(shape + "\t" + template + "\t" + confidence
                        + "\t" + supported, 1, Math::addExact);
                catalog.append(row(adapter.id(), adapter.source().platform(),
                        adapter.source().repository(), adapter.source().commit(), adapter.source().path(),
                        adapter.problemName(), shape, template, confidence, Boolean.toString(supported),
                        qualified, stableHash(source), supported ? "CANONICAL_SHAPE_ONLY" : "UNRESOLVED",
                        adapter.classification().rationale()));
                monitor.worked(1);
            }
            StringBuilder counts = new StringBuilder("shape\ttemplate\tconfidence\tcanonical_executable\tfiles\n");
            for (Map.Entry<String, Integer> entry : categories.entrySet()) {
                counts.append(entry.getKey()).append('\t').append(entry.getValue()).append('\n');
            }
            String generationRoot = stableHash("SYNEXIA_PROBLEM_TEMPLATE_EXPORT_V1\n"
                    + stableHash(catalog.toString()) + "\n" + stableHash(counts.toString()) + "\n") + "\n";
            Path marker = PinnedCorpusFiles.child(sourceRoot, "generation-root.sha256");
            if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS)
                    && !Files.readString(marker, StandardCharsets.UTF_8).equals(generationRoot)) {
                throw new IOException("completed template snapshot differs; use a fresh output root");
            }
            for (ProblemAdapter adapter : adapters) {
                monitor.checkCanceled();
                PinnedCorpusFiles.writeUtf8Once(sourceRoot,
                        qualifiedClassName(adapter).replace('.', '/') + ".java", sourceFor(adapter));
                monitor.worked(1);
            }
            monitor.checkCanceled();
            PinnedCorpusFiles.writeUtf8Once(sourceRoot, "catalog.tsv", catalog.toString());
            PinnedCorpusFiles.writeUtf8Once(sourceRoot, "categories.tsv", counts.toString());
            // Last publication is the completion marker. It transitively binds every wrapper body.
            PinnedCorpusFiles.writeUtf8Once(sourceRoot, "generation-root.sha256", generationRoot);
            return new GenerationResult(adapters.size(), executable, unresolved);
        } catch (IOException failure) {
            throw new IllegalStateException("failed exporting problem templates: " + sourceRoot, failure);
        } finally {
            monitor.done();
        }
    }

    private static String row(String... fields) {
        return java.util.Arrays.stream(fields).map(PinnedCorpusFiles::tsv)
                .collect(java.util.stream.Collectors.joining("\t", "", "\n"));
    }

    public static String sourceFor(ProblemAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        String hash = stableHash(adapter.id());
        String platform = platformIdentifier(adapter.source().platform());
        String bucket = "b_" + hash.substring(0, 2);
        String packageName = "com.synexia.algorithms.generated." + platform + "." + bucket;
        String className = className(adapter.problemName(), hash);
        String id = javaString(adapter.id());
        String problem = PinnedCorpusFiles.javadoc(adapter.problemName());
        String repository = PinnedCorpusFiles.javadoc(adapter.source().repository());
        String commit = PinnedCorpusFiles.javadoc(adapter.source().commit());
        String path = PinnedCorpusFiles.javadoc(adapter.source().path());
        String shape = adapter.classification().classified()
                ? adapter.classification().shape().name()
                : "UNCLASSIFIED";
        String template = adapter.templateStyle().kind().name();

        return """
                // SPDX-License-Identifier: Apache-2.0
                package %s;

                import com.synexia.algorithms.corpus.ProblemAdapter;
                import com.synexia.algorithms.corpus.ProblemAdapterCatalog;
                import com.synexia.job.IProgressMonitor;

                /**
                 * Generated canonical-shape adapter for %s.
                 *
                 * <p>Donor provenance:
                 * <ul>
                 *   <li>repository: %s</li>
                 *   <li>commit: %s</li>
                 *   <li>path: %s</li>
                 *   <li>shape: %s</li>
                 *   <li>template: %s</li>
                 * </ul>
                 *
                 * <p>No external implementation body is copied. Execution delegates to Synexia's
                 * canonical monitor-first donor for the classified shape. This is not a proof of the
                 * original challenge's behavior; its problem-specific encoding and decoding remain required.
                 */
                public final class %s {
                    public static final String SOURCE_ID = "%s";

                    private %s() {}

                    public static ProblemAdapter adapter() {
                        return ProblemAdapterCatalog.require(SOURCE_ID);
                    }

                    public static Object execute(Object donorInput, IProgressMonitor monitor) {
                        return adapter().executeCanonical(donorInput, monitor);
                    }
                }
                """.formatted(
                packageName,
                problem,
                repository,
                commit,
                path,
                shape,
                template,
                className,
                id,
                className);
    }

    public static String qualifiedClassName(ProblemAdapter adapter) {
        String hash = stableHash(adapter.id());
        String platform = platformIdentifier(adapter.source().platform());
        return "com.synexia.algorithms.generated."
                + platform + ".b_" + hash.substring(0, 2) + "."
                + className(adapter.problemName(), hash);
    }

    private static String platformIdentifier(String value) {
        String identifier = javaIdentifier(value.toLowerCase(Locale.ROOT));
        return identifier.isEmpty() || javax.lang.model.SourceVersion.isKeyword(identifier)
                || identifier.equals("_") ? "platform_" + identifier : identifier;
    }

    private static String className(String problemName, String hash) {
        String base = javaIdentifier(problemName);
        if (base.isEmpty()) base = "Problem";
        if (!Character.isJavaIdentifierStart(base.charAt(0))) base = "P_" + base;
        if (base.length() > 96) base = base.substring(0, 96);
        return base + "_" + hash.substring(0, 12);
    }

    private static String javaIdentifier(String value) {
        StringBuilder out = new StringBuilder(value.length() + 1);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((out.length() == 0 && Character.isJavaIdentifierStart(c))
                    || (out.length() > 0 && Character.isJavaIdentifierPart(c))) {
                out.append(c);
            } else if (out.length() > 0 && out.charAt(out.length() - 1) != '_') {
                out.append('_');
            }
        }
        return out.toString();
    }

    private static String javaString(String value) {
        return PinnedCorpusFiles.javaString(value);
    }

    private static String stableHash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
