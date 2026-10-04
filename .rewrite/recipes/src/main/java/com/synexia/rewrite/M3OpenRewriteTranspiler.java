// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;

/**
 * Canonical parser -> LST/SourceFile -> recipe -> printer loop for exactly one source atom.
 *
 * <p>Java, YAML, XML, JSON and properties use their language-specific lossless parser. Any other
 * textual source uses the OpenRewrite plain-text SourceFile lane. The runner reparses between
 * passes so each candidate is independently attributable and print-idempotent before M3 proof.</p>
 */
public final class M3OpenRewriteTranspiler {
    private M3OpenRewriteTranspiler() {}

    /** Compatibility M3 entry point. Java mutation authority remains explicit IOP pattern-only. */
    public static M3TranspileRun runMechanical(String sourcePath, String source) {
        return runIopPatternMechanical(sourcePath, source);
    }

    /** Canonical M3Scale Java mutation entry point for explicit IOP pattern code. */
    public static M3TranspileRun runIopPatternMechanical(String sourcePath, String source) {
        return run(sourcePath, source, List.of(), M3IopPatternMechanicalPasses.forFile(sourcePath));
    }

    /** Deterministic IOP source-fence/class-hook authority evidence without applying a mutation. */
    public static M3IopFullScaleMutationAuthority.AuthorityReceipt inspectIopAuthority(
            String sourcePath,
            String source) {
        return inspectIopAuthority(sourcePath, source, List.of());
    }

    /** Deterministic IOP authority evidence with caller-supplied compile classpath. */
    public static M3IopFullScaleMutationAuthority.AuthorityReceipt inspectIopAuthority(
            String sourcePath,
            String source,
            List<Path> classpath) {
        String normalizedPath = normalizeSourcePath(sourcePath);
        requireTextSource(source);
        Parsed parsed = parseLosslessly(
                normalizedPath,
                source,
                classpath(classpath),
                M3SourceKind.JAVA);
        if (!(parsed.source() instanceof J.CompilationUnit unit)) {
            throw new IllegalStateException("Java parser did not return a compilation unit");
        }
        return M3IopFullScaleMutationAuthority.receipt(unit, normalizedPath);
    }

    /** Compatibility alias. IOP path membership alone does not grant mutation authority. */
    public static M3TranspileRun runIopMechanical(String sourcePath, String source) {
        return runIopPatternMechanical(sourcePath, source);
    }

    /** Compatibility full M3 Java entry point; all passes are pattern-class-hooked. */
    public static M3TranspileRun runFullMechanical(String sourcePath, String source) {
        return runIopPatternMechanical(sourcePath, source);
    }

    /**
     * Existing Java-only explicit recipe runner.
     *
     * <p>This method preserves the original Java contract. Use {@link #runSourceCandidate} for
     * YAML/XML/JSON/properties/plain-text SourceFile candidates.</p>
     */
    public static M3TranspileRun run(
            String sourcePath,
            String source,
            List<M3TranspilePass> passes) {
        return run(sourcePath, source, List.of(), passes);
    }

    /** Existing Java-only explicit recipe runner with caller-supplied compile classpath. */
    public static M3TranspileRun run(
            String sourcePath,
            String source,
            List<Path> classpath,
            List<M3TranspilePass> passes) {
        return runInternal(sourcePath, source, classpath, passes, true);
    }

    /** Universal one-source candidate runner. Every mutation is still an OpenRewrite Recipe. */
    public static M3TranspileRun runSourceCandidate(
            String sourcePath,
            String source,
            List<M3TranspilePass> passes) {
        return runSourceCandidate(sourcePath, source, List.of(), passes);
    }

    /**
     * Universal one-source candidate runner with Java classpath support when the source is Java.
     *
     * <p>Unknown textual extensions intentionally fall back to PlainText. NUL-bearing/binary input
     * is rejected from this String-based mutation API.</p>
     */
    public static M3TranspileRun runSourceCandidate(
            String sourcePath,
            String source,
            List<Path> classpath,
            List<M3TranspilePass> passes) {
        return runInternal(sourcePath, source, classpath, passes, false);
    }

    private static M3TranspileRun runInternal(
            String sourcePath,
            String source,
            List<Path> classpath,
            List<M3TranspilePass> passes,
            boolean javaOnly) {
        String normalizedPath = javaOnly
                ? normalizeSourcePath(sourcePath)
                : normalizeSourcePathOrDirectory(sourcePath);
        requireTextSource(source);
        List<Path> checkedClasspath = classpath(classpath);
        List<M3TranspilePass> checkedPasses =
                List.copyOf(Objects.requireNonNull(passes, "passes"));
        requireUniquePassIds(checkedPasses);

        M3SourceKind sourceKind = javaOnly ? M3SourceKind.JAVA : M3SourceKind.classify(normalizedPath);
        String inputHash = sha256(source);
        String current = source;
        ArrayList<M3TranspilePassResult> receipts = new ArrayList<>(checkedPasses.size());

        if (checkedPasses.isEmpty()) {
            parseLosslessly(normalizedPath, current, checkedClasspath, sourceKind);
        }

        for (int index = 0; index < checkedPasses.size(); index++) {
            M3TranspilePass pass = checkedPasses.get(index);
            String beforeHash = sha256(current);
            M3OpenRewriteExecution execution = executeSingleInternal(
                    normalizedPath,
                    current,
                    checkedClasspath,
                    pass,
                    sourceKind);
            current = execution.after();
            String afterHash = sha256(current);
            receipts.add(new M3TranspilePassResult(
                    index + 1,
                    pass.id(),
                    pass.recipe().getName(),
                    normalizedPath,
                    beforeHash,
                    afterHash,
                    !beforeHash.equals(afterHash)));
        }

        return new M3TranspileRun(
                normalizedPath,
                inputHash,
                sha256(current),
                current,
                receipts);
    }

    /** Execute exactly one Java scheduler cycle and return diagnostic recipe/diff evidence. */
    public static M3OpenRewriteExecution executeSingle(
            String sourcePath,
            String source,
            List<Path> classpath,
            M3TranspilePass pass) {
        String normalizedPath = normalizeSourcePath(sourcePath);
        requireTextSource(source);
        return executeSingleInternal(
                normalizedPath,
                source,
                classpath(classpath),
                Objects.requireNonNull(pass, "pass"),
                M3SourceKind.JAVA);
    }

    /** Execute exactly one recipe cycle against any admitted textual SourceFile kind. */
    public static M3OpenRewriteExecution executeSingleSource(
            String sourcePath,
            String source,
            List<Path> classpath,
            M3TranspilePass pass) {
        String normalizedPath = normalizeSourcePathOrDirectory(sourcePath);
        requireTextSource(source);
        return executeSingleInternal(
                normalizedPath,
                source,
                classpath(classpath),
                Objects.requireNonNull(pass, "pass"),
                M3SourceKind.classify(normalizedPath));
    }

    private static M3OpenRewriteExecution executeSingleInternal(
            String normalizedPath,
            String source,
            List<Path> classpath,
            M3TranspilePass pass,
            M3SourceKind sourceKind) {
        Recipe recipe = pass.recipe();
        if (sourceKind == M3SourceKind.PYTHON
                && !(recipe instanceof M3PythonRecipe)) {
            throw new IllegalArgumentException(
                    "Python source requires an admitted M3PythonRecipe");
        }
        if (recipe.validateAll().stream().anyMatch(validation -> !validation.isValid())) {
            throw new IllegalArgumentException(
                    "invalid OpenRewrite recipe configuration: " + pass.id());
        }

        Parsed parsed = parseLosslessly(normalizedPath, source, classpath, sourceKind);
        List<Result> changes =
                recipe.run(
                                new InMemoryLargeSourceSet(List.of(parsed.source())),
                                parsed.context(),
                                1)
                        .getChangeset()
                        .getAllResults();

        if (changes.isEmpty()) {
            return new M3OpenRewriteExecution(
                    normalizedPath,
                    source,
                    source,
                    recipe.getClass().getName(),
                    recipe.getDisplayName(),
                    List.of(),
                    "");
        }
        if (changes.size() != 1) {
            throw new IllegalStateException(
                    "single-file pass attempted " + changes.size() + " source changes");
        }

        Result result = changes.getFirst();
        SourceFile before = result.getBefore();
        SourceFile after = result.getAfter();
        Path expectedPath = Path.of(normalizedPath);
        if (before == null || after == null) {
            throw new IllegalStateException("single-file pass may not create or delete sources");
        }
        if (!before.getSourcePath().equals(expectedPath)
                || !after.getSourcePath().equals(expectedPath)) {
            throw new IllegalStateException("single-file pass may not move or rename the source");
        }
        if (sourceKind.structured() && after instanceof PlainText) {
            throw new IllegalStateException(
                    "structured source recipe may not downgrade " + sourceKind + " to plain text");
        }

        String output = after.printAll();
        requireTextSource(output);
        parseLosslessly(normalizedPath, output, classpath, sourceKind);
        List<String> descriptors =
                result.getRecipeDescriptorsThatMadeChanges().stream()
                        .map(descriptor -> descriptor.getName())
                        .toList();
        return new M3OpenRewriteExecution(
                normalizedPath,
                source,
                output,
                recipe.getClass().getName(),
                recipe.getDisplayName(),
                descriptors,
                result.diff());
    }

    static String normalizeSourcePath(String sourcePath) {
        String result = normalizeSourcePathOrDirectory(sourcePath);
        if (!result.endsWith(".java")) {
            throw new IllegalArgumentException("sourcePath must identify a Java source file");
        }
        return result;
    }

    static String normalizeSourcePathOrDirectory(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank() || sourcePath.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("sourcePath required");
        }
        String portable = sourcePath.strip().replace('\\', '/');
        if (portable.startsWith("//") || portable.matches("^[A-Za-z]:/.*")) {
            throw new IllegalArgumentException("sourcePath must be repository-relative");
        }
        Path raw = Path.of(portable);
        if (raw.isAbsolute()) {
            throw new IllegalArgumentException("sourcePath must be repository-relative");
        }
        for (Path segment : raw) {
            if ("..".equals(segment.toString())) {
                throw new IllegalArgumentException("sourcePath traversal is not allowed");
            }
        }
        Path normalized = raw.normalize();
        String result = normalized.toString().replace('\\', '/');
        if (result.isBlank() || ".".equals(result)) {
            throw new IllegalArgumentException("sourcePath required");
        }
        return result;
    }

    private static Parsed parseLosslessly(
            String sourcePath,
            String source,
            List<Path> classpath,
            M3SourceKind sourceKind) {
        Path path = Path.of(sourcePath);
        ExecutionContext context = new InMemoryExecutionContext(throwable -> {
            throw new IllegalStateException(
                    "OpenRewrite execution failed for " + sourcePath,
                    throwable);
        });
        Parser parser = M3OpenRewriteParserRegistry.parserFor(sourceKind, classpath);
        List<SourceFile> parsed =
                parser.parseInputs(
                                List.of(Parser.Input.fromString(path, source)),
                                null,
                                context)
                        .toList();
        if (parsed.size() != 1) {
            throw new IllegalStateException(
                    "expected exactly one parsed source: " + sourcePath);
        }
        SourceFile parsedSource = parsed.getFirst();
        if (parsedSource instanceof ParseError) {
            throw new IllegalArgumentException(
                    "OpenRewrite could not parse " + sourceKind + " source " + sourcePath);
        }
        if (!parsedSource.getSourcePath().equals(path)) {
            throw new IllegalStateException("parser changed source path");
        }
        if (!parsedSource.printAll().equals(source)) {
            throw new IllegalArgumentException(
                    "OpenRewrite parse/print was not lossless for "
                            + sourceKind + " source " + sourcePath);
        }
        return new Parsed(parsedSource, context);
    }

    private static List<Path> classpath(List<Path> classpath) {
        List<Path> checked = List.copyOf(Objects.requireNonNull(classpath, "classpath"));
        checked.forEach(path -> Objects.requireNonNull(path, "classpath entry"));
        return checked;
    }

    private static void requireUniquePassIds(List<M3TranspilePass> passes) {
        Set<String> ids = new HashSet<>();
        for (M3TranspilePass pass : passes) {
            Objects.requireNonNull(pass, "pass");
            if (!ids.add(pass.id())) {
                throw new IllegalArgumentException("duplicate pass id: " + pass.id());
            }
        }
    }

    private static void requireTextSource(String source) {
        Objects.requireNonNull(source, "source");
        if (source.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("binary/NUL-bearing source is outside OpenRewrite text mutation authority");
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private record Parsed(SourceFile source, ExecutionContext context) {}
}
