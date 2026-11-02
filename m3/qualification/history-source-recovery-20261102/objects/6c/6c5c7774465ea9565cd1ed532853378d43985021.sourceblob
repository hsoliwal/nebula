// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Deterministic scheduler plans projected from the canonical Nebula M3 recipe DAG.
 *
 * <p>The plans carry ordering and recipe identity only. They never implement source mutation:
 * OpenRewrite recipes remain the source-changing atoms and repository proof/promotion remains
 * outside every scheduler.</p>
 */
public final class NebulaM3OrchestratorPlans {
    private static final String ROOT = sha256(
            frame(NebulaM3RecipeDagManifest.root())
                    + frame(camelYaml())
                    + frame(airflowPython())
                    + frame(droolsDrl()));

    private NebulaM3OrchestratorPlans() {}

    /** Apache Camel YAML route that serially dispatches admitted recipe atoms to one adapter endpoint. */
    public static String camelYaml() {
        StringBuilder out = new StringBuilder();
        out.append("# generated from DAG ")
                .append(NebulaM3RecipeDagManifest.root())
                .append('\n');
        out.append("- route:\n");
        out.append("    id: nebula-m3-file-convergence\n");
        out.append("    from:\n");
        out.append("      uri: \"direct:nebula-m3-file-convergence\"\n");
        out.append("      steps:\n");
        for (NebulaM3RecipeDagManifest.Node node : NebulaM3RecipeDagManifest.nodes()) {
            out.append("        - setProperty:\n")
                    .append("            name: m3RecipeOrdinal\n")
                    .append("            constant: \"").append(node.ordinal()).append("\"\n")
                    .append("        - setProperty:\n")
                    .append("            name: m3RecipeId\n")
                    .append("            constant: \"").append(node.id()).append("\"\n")
                    .append("        - setProperty:\n")
                    .append("            name: m3RecipeClass\n")
                    .append("            constant: \"").append(node.recipeClass()).append("\"\n")
                    .append("        - setProperty:\n")
                    .append("            name: m3Authority\n")
                    .append("            constant: \"").append(node.authority()).append("\"\n")
                    .append("        - to:\n")
                    .append("            uri: \"direct:nebula-m3-openrewrite-atom\"\n");
        }
        return out.toString();
    }

    /**
     * Apache Airflow DAG source using EmptyOperator nodes.
     *
     * <p>The generated Python intentionally performs no mutation. An external adapter may replace
     * an EmptyOperator with an OpenRewrite invocation after enforcing the same receipt/authority
     * checks.</p>
     */
    public static String airflowPython() {
        StringBuilder out = new StringBuilder();
        out.append("# generated from DAG ")
                .append(NebulaM3RecipeDagManifest.root())
                .append('\n')
                .append("from airflow import DAG\n")
                .append("from airflow.operators.empty import EmptyOperator\n")
                .append("from datetime import datetime\n\n")
                .append("with DAG(\n")
                .append("    dag_id=\"nebula_m3_file_convergence\",\n")
                .append("    schedule=None,\n")
                .append("    start_date=datetime(2026, 1, 1),\n")
                .append("    catchup=False,\n")
                .append("    params={\"dag_root\": \"")
                .append(NebulaM3RecipeDagManifest.root())
                .append("\", \"mutation_authority\": False, \"promotion_authority\": False},\n")
                .append(") as dag:\n");

        for (NebulaM3RecipeDagManifest.Node node : NebulaM3RecipeDagManifest.nodes()) {
            out.append("    ")
                    .append(pythonId(node.id()))
                    .append(" = EmptyOperator(\n")
                    .append("        task_id=\"").append(node.id()).append("\",\n")
                    .append("        params={\"recipe_class\": \"")
                    .append(node.recipeClass())
                    .append("\", \"scope\": \"")
                    .append(node.scope())
                    .append("\", \"authority\": \"")
                    .append(node.authority())
                    .append("\"},\n")
                    .append("    )\n");
        }
        out.append('\n');
        List<NebulaM3RecipeDagManifest.Node> nodes = NebulaM3RecipeDagManifest.nodes();
        for (int index = 1; index < nodes.size(); index++) {
            out.append(pythonId(nodes.get(index - 1).id()))
                    .append(" >> ")
                    .append(pythonId(nodes.get(index).id()))
                    .append('\n');
        }
        return out.toString();
    }

    /**
     * Drools agenda that marks recipe atoms ready only after their declared predecessor is ready.
     *
     * <p>The DRL is scheduling evidence only; it does not execute OpenRewrite or write source.</p>
     */
    public static String droolsDrl() {
        StringBuilder out = new StringBuilder();
        out.append("// generated from DAG ")
                .append(NebulaM3RecipeDagManifest.root())
                .append('\n')
                .append("package org.eclipse.nebula.m3.scheduler\n\n")
                .append("declare M3RecipeNode\n")
                .append("    id : String\n")
                .append("    ordinal : int\n")
                .append("    ready : boolean\n")
                .append("end\n\n");

        List<NebulaM3RecipeDagManifest.Node> nodes = NebulaM3RecipeDagManifest.nodes();
        for (int index = 0; index < nodes.size(); index++) {
            NebulaM3RecipeDagManifest.Node node = nodes.get(index);
            out.append("rule \"m3-")
                    .append(node.id())
                    .append("\"\n")
                    .append("salience ")
                    .append(1000 - index)
                    .append('\n')
                    .append("when\n");
            if (index > 0) {
                out.append("    $previous : M3RecipeNode(id == \"")
                        .append(nodes.get(index - 1).id())
                        .append("\", ready == true)\n");
            }
            out.append("    $current : M3RecipeNode(id == \"")
                    .append(node.id())
                    .append("\", ready == false)\n")
                    .append("then\n")
                    .append("    modify($current) { setReady(true) };\n")
                    .append("end\n\n");
        }
        return out.toString();
    }

    /** Content root of all scheduler-specific views plus the canonical DAG root. */
    public static String root() {
        return ROOT;
    }

    private static String pythonId(String value) {
        return value.replace('-', '_');
    }

    private static String frame(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return bytes.length + ":" + value;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
