// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Objects;

/**
 * Module-local Maven toolchain contract for the M3 IOP OpenRewrite lane.
 *
 * <p>The toolchain is intentionally model-agnostic. Exactly one Maven plugin may create source
 * mutation candidates: OpenRewrite. Every other plugin is verification, evidence, packaging, or
 * build infrastructure and has no M3 mutation authority.</p>
 */
public final class M3IopMavenToolchain {

    public enum Role {
        CANDIDATE_MUTATOR,
        COMPILE,
        UNIT_TEST,
        INTEGRATION_TEST,
        BUILD_POLICY,
        STYLE_VERIFY,
        STATIC_ANALYSIS,
        BUG_ANALYSIS,
        FORMAT_VERIFY,
        COVERAGE,
        DEPENDENCY_EVIDENCE,
        SBOM_EVIDENCE
    }

    public record Plugin(
            String groupId,
            String artifactId,
            Role role,
            boolean sourceMutationAuthority,
            List<String> canonicalGoals) {

        public Plugin {
            groupId = text(groupId, "groupId");
            artifactId = text(artifactId, "artifactId");
            role = Objects.requireNonNull(role, "role");
            canonicalGoals = List.copyOf(Objects.requireNonNull(canonicalGoals, "canonicalGoals"));
            if (canonicalGoals.isEmpty()) {
                throw new IllegalArgumentException("canonicalGoals required");
            }
            if (sourceMutationAuthority != (role == Role.CANDIDATE_MUTATOR)) {
                throw new IllegalArgumentException("mutation authority/role mismatch");
            }
        }

        public String coordinate() {
            return groupId + ":" + artifactId;
        }
    }

    private M3IopMavenToolchain() {}

    /** The canonical OpenRewrite goal only emits a reviewable patch; it does not apply it. */
    public static List<Plugin> plugins() {
        return List.of(
                plugin(
                        "org.openrewrite.maven",
                        "rewrite-maven-plugin",
                        Role.CANDIDATE_MUTATOR,
                        true,
                        "dryRunNoFork"),
                plugin(
                        "org.apache.maven.plugins",
                        "maven-compiler-plugin",
                        Role.COMPILE,
                        false,
                        "compile",
                        "testCompile"),
                plugin(
                        "org.apache.maven.plugins",
                        "maven-surefire-plugin",
                        Role.UNIT_TEST,
                        false,
                        "test"),
                plugin(
                        "org.apache.maven.plugins",
                        "maven-failsafe-plugin",
                        Role.INTEGRATION_TEST,
                        false,
                        "integration-test",
                        "verify"),
                plugin(
                        "org.apache.maven.plugins",
                        "maven-enforcer-plugin",
                        Role.BUILD_POLICY,
                        false,
                        "enforce"),
                plugin(
                        "org.apache.maven.plugins",
                        "maven-checkstyle-plugin",
                        Role.STYLE_VERIFY,
                        false,
                        "check"),
                plugin(
                        "org.apache.maven.plugins",
                        "maven-pmd-plugin",
                        Role.STATIC_ANALYSIS,
                        false,
                        "check"),
                plugin(
                        "com.github.spotbugs",
                        "spotbugs-maven-plugin",
                        Role.BUG_ANALYSIS,
                        false,
                        "check"),
                plugin(
                        "com.diffplug.spotless",
                        "spotless-maven-plugin",
                        Role.FORMAT_VERIFY,
                        false,
                        "check"),
                plugin(
                        "org.jacoco",
                        "jacoco-maven-plugin",
                        Role.COVERAGE,
                        false,
                        "prepare-agent",
                        "report"),
                plugin(
                        "org.apache.maven.plugins",
                        "maven-dependency-plugin",
                        Role.DEPENDENCY_EVIDENCE,
                        false,
                        "tree"),
                plugin(
                        "org.cyclonedx",
                        "cyclonedx-maven-plugin",
                        Role.SBOM_EVIDENCE,
                        false,
                        "makeBom"));
    }

    public static Plugin mutationPlugin() {
        List<Plugin> mutation = plugins().stream().filter(Plugin::sourceMutationAuthority).toList();
        if (mutation.size() != 1) {
            throw new IllegalStateException("exactly one Maven source candidate mutator is required");
        }
        return mutation.get(0);
    }

    public static List<Plugin> verificationPlugins() {
        return plugins().stream().filter(plugin -> !plugin.sourceMutationAuthority()).toList();
    }

    /** Canonical post-diff proof order: lint/static analysis, compile, then tests. */
    public static List<Plugin> canonicalProofPlugins() {
        return List.of(
                require("maven-checkstyle-plugin"),
                require("maven-pmd-plugin"),
                require("spotbugs-maven-plugin"),
                require("spotless-maven-plugin"),
                require("maven-compiler-plugin"),
                require("maven-surefire-plugin"),
                require("maven-failsafe-plugin"));
    }

    public static List<String> canonicalProofCoordinates() {
        return canonicalProofPlugins().stream().map(Plugin::coordinate).toList();
    }

    public static Plugin require(String artifactId) {
        String id = text(artifactId, "artifactId");
        return plugins().stream()
                .filter(plugin -> plugin.artifactId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown M3 IOP Maven helper: " + id));
    }

    private static Plugin plugin(
            String groupId,
            String artifactId,
            Role role,
            boolean sourceMutationAuthority,
            String... goals) {
        return new Plugin(groupId, artifactId, role, sourceMutationAuthority, List.of(goals));
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " required");
        }
        return value.strip();
    }
}
