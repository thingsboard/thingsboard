// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.edge.instructions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.TbVersionUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInstructions;
import org.thingsboard.server.dao.edge.EdgeEditionStyle;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class DefaultEdgeInstallInstructionsService extends BaseEdgeInstallUpgradeInstructionsService implements EdgeInstallInstructionsService {

    private static final String INSTALL_DIR = "install";

    @Value("${edges.rpc.port}")
    private int rpcPort;

    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;

    public DefaultEdgeInstallInstructionsService(InstallScripts installScripts) {
        super(installScripts);
    }

    @Override
    public EdgeInstructions getInstallInstructions(Edge edge, String installationMethod, HttpServletRequest request) {
        return switch (installationMethod.toLowerCase()) {
            case "docker" -> getDockerInstallInstructions(edge, request);
            case "ubuntu", "centos" -> getLinuxInstallInstructions(edge, request, installationMethod.toLowerCase());
            default ->
                    throw new IllegalArgumentException("Unsupported installation method for Edge: " + installationMethod);
        };
    }

    private EdgeInstructions getDockerInstallInstructions(Edge edge, HttpServletRequest request) {
        String dockerInstallInstructions = readFile(resolveFile("docker", "instructions.md"));
        String baseUrl = request.getServerName();

        if (DeviceConnectivityUtil.isLocalhost(baseUrl)) {
            dockerInstallInstructions = dockerInstallInstructions.replace("${EXTRA_HOSTS}", "extra_hosts:\n      - \"host.docker.internal:host-gateway\"\n");
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", "host.docker.internal");
        } else {
            dockerInstallInstructions = dockerInstallInstructions.replace("${EXTRA_HOSTS}", "");
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", baseUrl);
        }
        String edgeVersion = TbVersionUtils.extractStartingDigits(platformEdgeVersion);
        EdgeEditionStyle edition = EdgeEditionStyle.getEdgeEditionStyle(edgeVersion);
        dockerInstallInstructions = edition.replacePlaceholders(dockerInstallInstructions);
        dockerInstallInstructions = dockerInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion + edition.getVersionSuffix());
        dockerInstallInstructions = replacePlaceholders(dockerInstallInstructions, edge);
        return new EdgeInstructions(dockerInstallInstructions);
    }

    private EdgeInstructions getLinuxInstallInstructions(Edge edge, HttpServletRequest request, String os) {
        String ubuntuInstallInstructions = readFile(resolveFile(os, "instructions.md"));
        ubuntuInstallInstructions = replacePlaceholders(ubuntuInstallInstructions, edge);
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${BASE_URL}", request.getServerName());
        String edgeVersion = TbVersionUtils.extractStartingDigits(platformEdgeVersion);
        ubuntuInstallInstructions = EdgeEditionStyle.getEdgeEditionStyle(edgeVersion).replacePlaceholders(ubuntuInstallInstructions);
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${TB_EDGE_TAG}", getTagVersion(edgeVersion));
        return new EdgeInstructions(ubuntuInstallInstructions);
    }

    private String replacePlaceholders(String instructions, Edge edge) {
        instructions = instructions.replace("${CLOUD_ROUTING_KEY}", edge.getRoutingKey());
        instructions = instructions.replace("${CLOUD_ROUTING_SECRET}", edge.getSecret());
        instructions = instructions.replace("${CLOUD_RPC_PORT}", Integer.toString(rpcPort));
        instructions = instructions.replace("${CLOUD_RPC_SSL_ENABLED}", Boolean.toString(sslEnabled));
        return instructions;
    }

    @Override
    protected String getBaseDirName() {
        return INSTALL_DIR;
    }

}
