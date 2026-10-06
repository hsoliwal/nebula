// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Writes the portable Nebula M3 recipe-transfer evidence bundle. */
public final class NebulaM3TransferContractCli {
    private NebulaM3TransferContractCli() {}

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "usage: NebulaM3TransferContractCli <output-directory>");
        }
        Path output =
                Path.of(Objects.requireNonNull(args[0], "output"))
                        .toAbsolutePath()
                        .normalize();
        Files.createDirectories(output);

        Files.writeString(
                output.resolve("transfer-metadata.tsv"),
                NebulaM3TransferContract.metadataTsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("transfer-dag.tsv"),
                NebulaM3RecipeDagManifest.tsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("transfer-targets.tsv"),
                NebulaM3TransferContract.targetsTsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("orchestrators.tsv"),
                NebulaM3TransferContract.orchestratorsTsv(),
                StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("transfer.sha256"),
                NebulaM3TransferContract.root() + "
",
                StandardCharsets.UTF_8);
    }
}
