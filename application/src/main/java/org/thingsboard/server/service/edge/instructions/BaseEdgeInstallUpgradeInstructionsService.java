// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.instructions;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.service.install.InstallScripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.thingsboard.edge.rpc.EdgeGrpcClient.getNewestEdgeVersion;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseEdgeInstallUpgradeInstructionsService {

    private static final String EDGE_DIR = "edge";
    private static final String INSTRUCTIONS_DIR = "instructions";

    private final InstallScripts installScripts;

    @Setter
    protected String platformEdgeVersion = convertEdgeVersionToDocsFormat(getNewestEdgeVersion().name());

    protected String convertEdgeVersionToDocsFormat(String edgeVersion) {
        return edgeVersion.replace("_", ".").substring(2);
    }

    protected String readFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            log.warn("Failed to read file: {}", file, e);
            throw new RuntimeException(e);
        }
    }

    protected String getTagVersion(String version) {
        return version.endsWith(".0") ? version.substring(0, version.length() - 2) : version;
    }

    protected Path resolveFile(String subDir, String... subDirs) {
        return getEdgeInstructionsDir().resolve(Paths.get(subDir, subDirs));
    }

    protected Path getEdgeInstructionsDir() {
        return Paths.get(installScripts.getDataDir(), InstallScripts.JSON_DIR, EDGE_DIR, INSTRUCTIONS_DIR, getBaseDirName());
    }

    protected abstract String getBaseDirName();

}
