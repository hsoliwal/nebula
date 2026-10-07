// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NebulaM3OrchestratorPlansTest {
    @Test
    void camelPlanContainsEveryRecipeAtomInCanonicalOrder() {
        String yaml = NebulaM3OrchestratorPlans.camelYaml();
        int previous = -1;

        for (NebulaM3RecipeDagManifest.Node node : NebulaM3RecipeDagManifest.nodes()) {
            int position = yaml.indexOf("constant: \"" + node.id() + "\"");
            assertTrue(position > previous, node.id());
            previous = position;
            assertTrue(yaml.contains("constant: \"" + node.recipeClass() + "\""));
        }
        assertTrue(yaml.contains("direct:nebula-m3-openrewrite-atom"));
        assertFalse(yaml.contains("rewrite:run"));
    }

    @Test
    void airflowPlanIsOneSerialDagAndCarriesNoMutationOperator() {
        String python = NebulaM3OrchestratorPlans.airflowPython();
        var nodes = NebulaM3RecipeDagManifest.nodes();

        assertTrue(python.contains("dag_id=\"nebula_m3_file_convergence\""));
        assertTrue(python.contains("\"mutation_authority\": False"));
        assertTrue(python.contains("\"promotion_authority\": False"));
        assertEquals(nodes.size(), occurrences(python, " = EmptyOperator("));
        for (int index = 1; index < nodes.size(); index++) {
            String edge =
                    nodes.get(index - 1).id().replace('-', '_')
                            + " >> "
                            + nodes.get(index).id().replace('-', '_');
            assertTrue(python.contains(edge), edge);
        }
        assertFalse(python.contains("BashOperator"));
        assertFalse(python.contains("rewrite:run"));
    }

    @Test
    void droolsPlanEncodesPredecessorReadinessWithoutExecutingRecipes() {
        String drl = NebulaM3OrchestratorPlans.droolsDrl();
        var nodes = NebulaM3RecipeDagManifest.nodes();

        assertEquals(nodes.size(), occurrences(drl, "rule \"m3-"));
        assertTrue(drl.contains("modify($current) { setReady(true) };"));
        for (int index = 1; index < nodes.size(); index++) {
            assertTrue(
                    drl.contains(
                            "$previous : M3RecipeNode(id == \""
                                    + nodes.get(index - 1).id()
                                    + "\", ready == true)"));
        }
        assertFalse(drl.contains("Runtime.getRuntime"));
        assertFalse(drl.contains("ProcessBuilder"));
    }

    @Test
    void schedulerPlanRootIsStableAndBoundToCanonicalDag() {
        assertTrue(NebulaM3OrchestratorPlans.root().matches("[0-9a-f]{64}"));
        assertEquals(NebulaM3OrchestratorPlans.root(), NebulaM3OrchestratorPlans.root());
        assertTrue(
                NebulaM3OrchestratorPlans.camelYaml()
                        .contains(NebulaM3RecipeDagManifest.root()));
        assertTrue(
                NebulaM3OrchestratorPlans.airflowPython()
                        .contains(NebulaM3RecipeDagManifest.root()));
        assertTrue(
                NebulaM3OrchestratorPlans.droolsDrl()
                        .contains(NebulaM3RecipeDagManifest.root()));
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
