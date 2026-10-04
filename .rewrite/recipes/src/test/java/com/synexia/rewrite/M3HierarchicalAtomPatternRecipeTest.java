// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class M3HierarchicalAtomPatternRecipeTest {

    @Test
    void sourceSpecificitySeparatesCodeBuildDocumentationAndData() {
        assertEquals(
                M3SourceSpecificity.JAVA,
                M3SourceSpecificity.classify("module/src/main/java/a/b/C.java"));
        assertEquals(
                M3SourceSpecificity.JNI_CPP,
                M3SourceSpecificity.classify("module/src/main/native/jni_bridge.cpp"));
        assertEquals(
                M3SourceSpecificity.MAVEN_POM,
                M3SourceSpecificity.classify("module/pom.xml"));
        assertEquals(
                M3SourceSpecificity.MARKDOWN,
                M3SourceSpecificity.classify("docs/ARCHITECTURE.md"));
        assertEquals(
                M3SourceSpecificity.DATA,
                M3SourceSpecificity.classify("catalog/problem-shapes.tsv"));

        assertTrue(M3SourceSpecificity.JAVA.code());
        assertTrue(M3SourceSpecificity.MARKDOWN.documentation());
        assertFalse(M3SourceSpecificity.MARKDOWN.code());
    }

    @Test
    void promotionOrderCannotSkipRequiredScope() {
        assertEquals(M3AtomPromotionScope.FILE, M3AtomPromotionScope.PACKAGE.previous());
        assertEquals(M3AtomPromotionScope.PACKAGE, M3AtomPromotionScope.MODULE.previous());
        assertEquals(M3AtomPromotionScope.MODULE, M3AtomPromotionScope.PROJECT.previous());
        assertEquals(M3AtomPromotionScope.PROJECT, M3AtomPromotionScope.REPOSITORY.previous());

        assertTrue(M3AtomPromotionScope.FILE.mayPromoteTo(M3AtomPromotionScope.PACKAGE));
        assertFalse(M3AtomPromotionScope.FILE.mayPromoteTo(M3AtomPromotionScope.MODULE));
        assertFalse(M3AtomPromotionScope.MODULE.mayPromoteTo(M3AtomPromotionScope.FILE));
        assertTrue(M3AtomPromotionScope.REPOSITORY.terminal());
    }

    @Test
    void documentationAtomsPreserveDocumentStructureInsteadOfJavaSemantics() {
        final String backticks = Character.toString(96).repeat(3);
        final String markdown = String.join(
                "\n",
                "# Title",
                "",
                "Paragraph one.",
                "Paragraph two.",
                "",
                backticks + "java",
                "int x = 1;",
                backticks,
                "",
                "## Next",
                "");
        final List<M3DocumentationAtomInventoryRecipe.DocumentationAtom> atoms =
                M3DocumentationAtomInventoryRecipe.parse(
                        "docs/example.md",
                        M3SourceSpecificity.MARKDOWN,
                        markdown);

        assertEquals(
                List.of("HEADING", "PARAGRAPH", "CODE_FENCE", "HEADING"),
                atoms.stream()
                        .map(M3DocumentationAtomInventoryRecipe.DocumentationAtom::atomKind)
                        .toList());
        assertEquals(1, atoms.get(0).headingLevel());
        assertEquals(2, atoms.get(3).headingLevel());
        assertTrue(atoms.stream().allMatch(atom -> !atom.mutationAuthority()));
    }

    @Test
    void canonicalBundleKeepsHierarchyAndExistingRepositoryInventoryTogether() {
        final M3HierarchicalAtomPatternRecipe recipe =
                new M3HierarchicalAtomPatternRecipe("**", "mindex search");
        final List<org.openrewrite.Recipe> children = recipe.getRecipeList();

        assertEquals(3, children.size());
        assertInstanceOf(M3HierarchicalAtomPatternPlanRecipe.class, children.get(0));
        assertInstanceOf(M3DocumentationAtomInventoryRecipe.class, children.get(1));
        assertInstanceOf(M3RepositoryAtomizePatternizeRecipe.class, children.get(2));
        assertFalse(recipe.mutationAuthority());
        assertFalse(recipe.promotionAuthority());
    }
}
