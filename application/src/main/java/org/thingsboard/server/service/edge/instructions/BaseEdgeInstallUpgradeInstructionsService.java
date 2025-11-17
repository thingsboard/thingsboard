/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.instructions;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.service.install.InstallScripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseEdgeInstallUpgradeInstructionsService {

    private static final String EDGE_DIR = "edge";
    private static final String INSTRUCTIONS_DIR = "instructions";

    private final InstallScripts installScripts;

    @Setter
    protected String edgeVersion = convertEdgeVersionToDocsFormat(EdgeVersion.V_4_2_0.name());

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
