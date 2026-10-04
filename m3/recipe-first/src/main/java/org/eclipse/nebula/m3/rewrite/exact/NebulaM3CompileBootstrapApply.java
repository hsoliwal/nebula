// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Verification-only materializer for the two sealed repairs in an exclusively owned worktree. */
public final class NebulaM3CompileBootstrapApply {
    private NebulaM3CompileBootstrapApply() {}

    public static int apply(Path root, BooleanSupplier canceled, LongConsumer worked) throws IOException {
        Path checkedRoot = root.toRealPath();
        var expected = new LinkedHashMap<String, String>();
        var candidates = new LinkedHashMap<String, String>();
        var inputs = new ArrayList<Parser.Input>();
        for (var snapshot : NebulaM3CompileBootstrapRecipe.snapshots()) {
            checkpoint(canceled);
            String name = snapshot.repositoryPath();
            Path file = checkedRoot.resolve(name);
            if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)
                    || !file.toRealPath().startsWith(checkedRoot)) {
                throw new IllegalStateException("regular checkout source required: " + name);
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            String hash = NebulaM3ExactJavaSnapshotRecipe.sha256(text);
            if (!hash.equals(snapshot.expectedBeforeSha256())
                    && !hash.equals(snapshot.expectedAfterSha256())) {
                throw new IllegalStateException("bootstrap source drift: " + name);
            }
            expected.put(name, hash);
            inputs.add(Parser.Input.fromString(Path.of(name), text));
        }
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
        if (parsed.size() != inputs.size() || parsed.stream().anyMatch(file -> !(file instanceof J.CompilationUnit))) {
            throw new IllegalStateException("bootstrap Java parsing coverage failed");
        }
        requireNoErrors(errors);
        checkpoint(canceled);
        var run = new NebulaM3CompileBootstrapRecipe().run(new InMemoryLargeSourceSet(parsed), context);
        requireNoErrors(errors);
        for (var result : run.getChangeset().getAllResults()) {
            if (result.getBefore() == null || result.getAfter() == null) {
                throw new IllegalStateException("bootstrap cannot add or delete source files");
            }
            String name = result.getAfter().getSourcePath().toString().replace('\\', '/');
            String text = result.getAfter().printAll();
            var snapshot = NebulaM3CompileBootstrapRecipe.snapshots().stream()
                    .filter(candidate -> candidate.repositoryPath().equals(name)).findFirst().orElseThrow();
            if (!NebulaM3ExactJavaSnapshotRecipe.sha256(text).equals(snapshot.expectedAfterSha256())
                    || candidates.putIfAbsent(name, text) != null) {
                throw new IllegalStateException("bootstrap output drift or duplicate: " + name);
            }
        }
        // Validate every postimage and source before any write. The isolated worktree is not promoted here.
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            checkpoint(canceled);
            if (!entry.getValue().equals(NebulaM3ExactJavaSnapshotRecipe.sha256(
                    Files.readString(checkedRoot.resolve(entry.getKey()), StandardCharsets.UTF_8)))) {
                throw new IllegalStateException("source changed during bootstrap: " + entry.getKey());
            }
        }
        for (Map.Entry<String, String> entry : candidates.entrySet()) {
            checkpoint(canceled);
            Files.writeString(checkedRoot.resolve(entry.getKey()), entry.getValue(), StandardCharsets.UTF_8);
            if (worked != null) worked.accept(1);
        }
        return candidates.size();
    }

    private static void requireNoErrors(List<Throwable> errors) {
        if (!errors.isEmpty()) throw new IllegalStateException("OpenRewrite bootstrap failed", errors.getFirst());
    }

    private static void checkpoint(BooleanSupplier canceled) {
        if (Thread.currentThread().isInterrupted() || canceled != null && canceled.getAsBoolean()) {
            throw new CancellationException("bootstrap canceled");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("exact Nebula checkout path required");
        System.out.println("BOOTSTRAP_CHANGED=" + apply(Path.of(args[0]), null, null));
    }
}
