/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class DefaultEdgeUpgradeService implements EdgeUpgradeService {

    private static final HashMap<String, UpgradeInfo> upgradeVersionHashMap;

    static {
        upgradeVersionHashMap = new HashMap<>();
        upgradeVersionHashMap.put("3.6.0", new UpgradeInfo(true, "3.6.1"));
        upgradeVersionHashMap.put("3.6.1", new UpgradeInfo(true, "3.6.2"));
        upgradeVersionHashMap.put("3.6.2", new UpgradeInfo(true, null));
    }

    private static final String EDGE_DIR = "edge";
    private static final String INSTRUCTIONS_DIR = "instructions";
    private static final String UPGRADE_DIR = "upgrade";

    private final InstallScripts installScripts;

    @Value("${app.version:unknown}")
    private String appVersion;

    @Override
    public EdgeInstructions getUpgradeInstructions(String edgeVersion, String upgradeMethod) {
        String tbVersion = appVersion.replace("-SNAPSHOT", "");
        String currentEdgeVersion = convertEdgeVersionToDocsFormat(edgeVersion);
        switch (upgradeMethod.toLowerCase()) {
            case "docker":
                return getDockerUpgradeInstructions(tbVersion, currentEdgeVersion);
            case "ubuntu":
            case "centos":
                return getLinuxUpgradeInstructions(tbVersion, currentEdgeVersion, upgradeMethod.toLowerCase());
            default:
                throw new IllegalArgumentException("Unsupported upgrade method for Edge: " + upgradeMethod);
        }
    }

    private EdgeInstructions getDockerUpgradeInstructions(String tbVersion, String currentEdgeVersion) {
        UpgradeInfo upgradeInfo = upgradeVersionHashMap.get(currentEdgeVersion);
        if (upgradeInfo.getNextVersion() == null || tbVersion.equals(currentEdgeVersion)) {
            return null;
        }
        boolean stoppedService = false;
        StringBuilder result = new StringBuilder(readFile(resolveFile("docker", "upgrade_preparing.md")));
        while (upgradeInfo.getNextVersion() != null || !tbVersion.equals(currentEdgeVersion)) {
            String edgeVersion = upgradeInfo.getNextVersion();
            String ubuntuUpgradeInstructions = readFile(resolveFile("docker", "instructions.md"));
            if (upgradeInfo.isUpgradeDb()) {
                String upgradeDb = readFile(resolveFile("docker", "upgrade_db.md"));
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${UPGRADE_DB}", upgradeDb);
            }
            if (!stoppedService) {
                stoppedService = true;
                String stopService = readFile(resolveFile("docker", "stop_service.md"));
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${STOP_SERVICE}", stopService);
            } else {
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${STOP_SERVICE}", "");
            }
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${TB_EDGE_VERSION}", edgeVersion + "EDGE");
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${CURRENT_TB_EDGE_VERSION}", currentEdgeVersion + "EDGE");
            currentEdgeVersion = edgeVersion;
            upgradeInfo = upgradeVersionHashMap.get(upgradeInfo.getNextVersion());
            result.append(ubuntuUpgradeInstructions);
        }
        String startService = readFile(resolveFile("docker", "start_service.md"));
        result.append(startService);
        return new EdgeInstructions(result.toString());
    }

    private EdgeInstructions getLinuxUpgradeInstructions(String tbVersion, String currentEdgeVersion, String os) {
        UpgradeInfo upgradeInfo = upgradeVersionHashMap.get(currentEdgeVersion);
        if (upgradeInfo.getNextVersion() == null || tbVersion.equals(currentEdgeVersion)) {
            return null;
        }
        boolean stoppedService = false;
        StringBuilder result = new StringBuilder(readFile(resolveFile("upgrade_preparing.md")));
        while (upgradeInfo.getNextVersion() != null || !tbVersion.equals(currentEdgeVersion)) {
            String edgeVersion = upgradeInfo.getNextVersion();
            String ubuntuUpgradeInstructions = readFile(resolveFile(os, "instructions.md"));
            if (upgradeInfo.isUpgradeDb()) {
                String upgradeDb = readFile(resolveFile("upgrade_db.md"));
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${UPGRADE_DB}", upgradeDb);
            }
            if (!stoppedService) {
                stoppedService = true;
                String stopService = readFile(resolveFile("stop_service.md"));
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${STOP_SERVICE}", stopService);
            } else {
                ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${STOP_SERVICE}", "");
            }
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${TB_EDGE_TAG}", getTagVersion(edgeVersion));
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${CURRENT_TB_EDGE_TAG}", getTagVersion(currentEdgeVersion));
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${CURRENT_TB_EDGE_VERSION}", currentEdgeVersion);
            ubuntuUpgradeInstructions = ubuntuUpgradeInstructions.replace("${TB_EDGE_VERSION_TITLE}", edgeVersion + "EDGE");
            currentEdgeVersion = edgeVersion;
            upgradeInfo = upgradeVersionHashMap.get(upgradeInfo.getNextVersion());
            result.append(ubuntuUpgradeInstructions);
        }
        String startService = readFile(resolveFile("start_service.md"));
        result.append(startService);
        return new EdgeInstructions(result.toString());
    }

    private String getTagVersion(String version) {
        return version.endsWith(".0") ? version.substring(0, version.length() - 2) : version;
    }

    private String convertEdgeVersionToDocsFormat(String edgeVersion) {
        return edgeVersion.replace("_", ".").substring(2);
    }

    private String readFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            log.warn("Failed to read file: {}", file, e);
            throw new RuntimeException(e);
        }
    }

    private Path resolveFile(String subDir, String... subDirs) {
        return getEdgeInstallInstructionsDir().resolve(Paths.get(subDir, subDirs));
    }

    private Path getEdgeInstallInstructionsDir() {
        return Paths.get(installScripts.getDataDir(), InstallScripts.JSON_DIR, EDGE_DIR, INSTRUCTIONS_DIR, UPGRADE_DIR);
    }

    @AllArgsConstructor
    @Getter
    public static class UpgradeInfo {
        private boolean upgradeDb;
        private String nextVersion;
    }
}
