// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

/**
 * Opt-in Maven profile atom for the exact Nebula DAG evidence publisher.
 *
 * <p>Only the pinned XML LST is accepted, including when Maven presents this module as pom.xml.
 * There is no lifecycle-bound source rewrite or backport/promotion authority.</p>
 */
public final class NebulaM3DagAtomicMavenProfileRecipe extends Recipe {
    public static final String PATH = "m3/recipe-first/pom.xml";
    public static final String BEFORE_SHA256 =
            "46b3eb273ec86cebd01a2b6210b8366e586620bd10864a16dd1c6f775a27e9da";
    public static final String AFTER_SHA256 =
            "2b0af28c4bcb4db571185f80337ba97f77b89d1bd74de3bea4946678e92431fc";
    public static final String POSTIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/dag-atomic-evidence/pom.after.xml.txt";

    @Override
    public String getDisplayName() {
        return "Enable explicit Maven proof of Nebula atomic DAG evidence";
    }

    @Override
    public String getDescription() {
        return "Add the reviewed opt-in Maven/OpenRewrite profile that loads the exact "
                + "atomic DAG evidence recipe without binding mutation to a lifecycle phase.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "nebula", "java21", "maven", "xml",
                "openrewrite", "recipe-first", "module-scope", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String postimage = resource(POSTIMAGE);
        if (!AFTER_SHA256.equals(NebulaM3ExactJavaSnapshotRecipe.sha256(postimage))) {
            throw new IllegalStateException("M3 atomic DAG Maven profile resource drift");
        }
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext context) {
                String path = document.getSourcePath().normalize().toString().replace('\\', '/');
                String hash = NebulaM3ExactJavaSnapshotRecipe.sha256(document.printAll());
                boolean target = PATH.equals(path)
                        || ("pom.xml".equals(path)
                                && (BEFORE_SHA256.equals(hash) || AFTER_SHA256.equals(hash)));
                if (!target) return document;
                if (document.isCharsetBomMarked()
                        || document.getCharset() != null
                                && !StandardCharsets.UTF_8.equals(document.getCharset())) {
                    throw new IllegalStateException("M3 atomic DAG POM requires UTF-8 no BOM");
                }
                if (AFTER_SHA256.equals(hash)) return document;
                if (!BEFORE_SHA256.equals(hash)) {
                    context.getOnError().accept(
                            new IllegalStateException("M3 atomic DAG Maven POM preimage drift"));
                    return document;
                }

                List<SourceFile> parsed = XmlParser.builder().build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of("pom.xml"), postimage)),
                                null,
                                context)
                        .toList();
                if (parsed.size() != 1
                        || !(parsed.getFirst() instanceof Xml.Document replacement)
                        || !postimage.equals(parsed.getFirst().printAll())) {
                    throw new IllegalStateException("M3 atomic DAG Maven XML LST roundtrip drift");
                }
                return replacement.withId(document.getId())
                        .withSourcePath(document.getSourcePath())
                        .withFileAttributes(document.getFileAttributes());
            }
        };
    }

    private static String resource(String path) {
        try (InputStream input =
                NebulaM3DagAtomicMavenProfileRecipe.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException("missing Maven XML recipe postimage");
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(input.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot load Maven profile postimage", failure);
        }
    }
}
