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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUpgradeInfo;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class DefaultEdgeUpgradeInstructionsService extends BaseEdgeInstallUpgradeInstructionsService implements EdgeUpgradeInstructionsService {

    private static final Map<String, EdgeUpgradeInfo> upgradeVersionHashMap = new HashMap<>();

    private static final String UPGRADE_DIR = "upgrade";

    private final AttributesService attributesService;

    public DefaultEdgeUpgradeInstructionsService(AttributesService attributesService, InstallScripts installScripts) {
        super(installScripts);
        this.attributesService = attributesService;
    }

    @Override
    public EdgeInstructions getUpgradeInstructions(String edgeVersion, String upgradeMethod) {
        String currentEdgeVersion = convertEdgeVersionToDocsFormat(edgeVersion);
        return switch (upgradeMethod.toLowerCase()) {
            case "docker" -> getDockerUpgradeInstructions(this.edgeVersion, currentEdgeVersion);
            case "ubuntu", "centos" ->
                    getLinuxUpgradeInstructions(this.edgeVersion, currentEdgeVersion, upgradeMethod.toLowerCase());
            default -> throw new IllegalArgumentException("Unsupported upgrade method for Edge: " + upgradeMethod);
        };
    }

    @Override
    public void updateInstructionMap(Map<String, EdgeUpgradeInfo> map) {
        for (String key : map.keySet()) {
            upgradeVersionHashMap.put(key, map.get(key));
        }
    }

    @Override
    public boolean isUpgradeAvailable(TenantId tenantId, EdgeId edgeId) throws Exception {
        Optional<AttributeKvEntry> attributeKvEntryOpt = attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, DataConstants.EDGE_VERSION_ATTR_KEY).get();
        if (attributeKvEntryOpt.isPresent()) {
            String edgeVersionFormatted = convertEdgeVersionToDocsFormat(attributeKvEntryOpt.get().getValueAsString());
            return isVersionGreaterOrEqualsThan(edgeVersionFormatted, "3.6.0") && !isVersionGreaterOrEqualsThan(edgeVersionFormatted, edgeVersion);
        }
        return false;
    }

    private boolean isVersionGreaterOrEqualsThan(String version1, String version2) {
        String[] v1 = version1.split("\\.");
        String[] v2 = version2.split("\\.");

        int length = Math.max(v1.length, v2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < v1.length ? Integer.parseInt(v1[i]) : 0;
            int num2 = i < v2.length ? Integer.parseInt(v2[i]) : 0;

            if (num1 < num2) {
                return false;
            } else if (num1 > num2) {
                return true;
            }
        }
        return true;
    }

    private EdgeInstructions getDockerUpgradeInstructions(String tbVersion, String currentEdgeVersion) {
        EdgeUpgradeInfo edgeUpgradeInfo = upgradeVersionHashMap.get(currentEdgeVersion);
        if (edgeUpgradeInfo == null || edgeUpgradeInfo.getNextEdgeVersion() == null || tbVersion.equals(currentEdgeVersion)) {
            return new EdgeInstructions("Edge upgrade instruction for " + currentEdgeVersion + "EDGE is not available.");
        }
        StringBuilder result = new StringBuilder(readFile(resolveFile("docker", "upgrade_preparing.md")));
        while (edgeUpgradeInfo.getNextEdgeVersion() != null && !tbVersion.equals(currentEdgeVersion)) {
            String edgeVersion = edgeUpgradeInfo.getNextEdgeVersion();
            String dockerUpgradeInstructions = readFile(resolveFile("docker", "instructions.md"));
            if (edgeUpgradeInfo.isRequiresUpdateDb()) {
                String upgradeDb = readFile(resolveFile("docker", "upgrade_db.md"));
                dockerUpgradeInstructions = dockerUpgradeInstructions.replace("${UPGRADE_DB}", upgradeDb);
            } else {
                dockerUpgradeInstructions = dockerUpgradeInstructions.replace("${UPGRADE_DB}", "");
            }
            dockerUpgradeInstructions = dockerUpgradeInstructions.replace("${TB_EDGE_VERSION}", edgeVersion + "EDGE");
            dockerUpgradeInstructions = dockerUpgradeInstructions.replace("${FROM_TB_EDGE_VERSION}", currentEdgeVersion + "EDGE");
            currentEdgeVersion = edgeVersion;
            edgeUpgradeInfo = upgradeVersionHashMap.get(edgeUpgradeInfo.getNextEdgeVersion());
            result.append(dockerUpgradeInstructions);
        }
        String startService = readFile(resolveFile("docker", "start_service.md"));
        startService = startService.replace("${TB_EDGE_VERSION}", currentEdgeVersion + "EDGE");
        result.append(startService);
        return new EdgeInstructions(result.toString());
    }

    private EdgeInstructions getLinuxUpgradeInstructions(String tbVersion, String currentEdgeVersion, String os) {
        EdgeUpgradeInfo edgeUpgradeInfo = upgradeVersionHashMap.get(currentEdgeVersion);
        if (edgeUpgradeInfo == null || edgeUpgradeInfo.getNextEdgeVersion() == null || tbVersion.equals(currentEdgeVersion)) {
            return new EdgeInstructions("Edge upgrade instruction for " + currentEdgeVersion + "EDGE is not available.");
        }
        String upgrade_preparing = readFile(resolveFile("upgrade_preparing.md"));
        upgrade_preparing = upgrade_preparing.replace("${OS}", os.equals("centos") ? "RHEL/CentOS 7/8" : "Ubuntu");
        StringBuilder result = new StringBuilder(upgrade_preparing);
        while (edgeUpgradeInfo.getNextEdgeVersion() != null && !tbVersion.equals(currentEdgeVersion)) {
            String edgeVersion = edgeUpgradeInfo.getNextEdgeVersion();
            String linuxUpgradeInstructions = readFile(resolveFile(os, "instructions.md"));
            if (edgeUpgradeInfo.isRequiresUpdateDb()) {
                String upgradeDb = readFile(resolveFile("upgrade_db.md"));
                linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${UPGRADE_DB}", upgradeDb);
            } else {
                linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${UPGRADE_DB}", "");
            }
            linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${TB_EDGE_TAG}", getTagVersion(edgeVersion));
            linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${FROM_TB_EDGE_TAG}", getTagVersion(currentEdgeVersion));
            linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
            linuxUpgradeInstructions = linuxUpgradeInstructions.replace("${FROM_TB_EDGE_VERSION}", currentEdgeVersion);
            currentEdgeVersion = edgeVersion;
            edgeUpgradeInfo = upgradeVersionHashMap.get(edgeUpgradeInfo.getNextEdgeVersion());
            result.append(linuxUpgradeInstructions);
        }
        String startService = readFile(resolveFile("start_service.md"));
        result.append(startService);
        return new EdgeInstructions(result.toString());
    }

    @Override
    protected String getBaseDirName() {
        return UPGRADE_DIR;
    }

}
