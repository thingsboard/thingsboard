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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInstallInstructions;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class DefaultEdgeInstallService implements EdgeInstallService {

    private static final String EDGE_DIR = "edge";

    private static final String EDGE_INSTALL_INSTRUCTIONS_DIR = "install_instructions";

    private final InstallScripts installScripts;

    @Value("${edges.rpc.port}")
    private int rpcPort;

    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;

    @Value("${app.version:unknown}")
    private String appVersion;

    @Override
    public EdgeInstallInstructions getInstallInstructions(TenantId tenantId, Edge edge, String installationMethod, HttpServletRequest request) {
        switch (installationMethod.toLowerCase()) {
            case "docker":
                return getDockerInstallInstructions(edge, request);
            case "ubuntu":
                return getUbuntuInstallInstructions(edge, request);
            case "centos":
                return getCentosInstallInstructions(edge, request);
            default:
                throw new IllegalArgumentException("Unsupported installation method for Edge: " + installationMethod);
        }
    }

    private EdgeInstallInstructions getDockerInstallInstructions(Edge edge, HttpServletRequest request) {
        String dockerInstallInstructions = readFile(resolveFile("docker", "instructions.md"));
        String baseUrl = request.getServerName();
        if (baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1")) {
            String localhostWarning = readFile(resolveFile("docker", "localhost_warning.md"));
            dockerInstallInstructions = dockerInstallInstructions.replace("${LOCALHOST_WARNING}", localhostWarning);
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", "!!!REPLACE_ME_TO_HOST_IP_ADDRESS!!!");
        } else {
            dockerInstallInstructions = dockerInstallInstructions.replace("${LOCALHOST_WARNING}", "");
            dockerInstallInstructions = dockerInstallInstructions.replace("${BASE_URL}", baseUrl);
        }
        String edgeVersion = appVersion + "EDGE";
        edgeVersion = edgeVersion.replace("-SNAPSHOT", "");
        dockerInstallInstructions = dockerInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        dockerInstallInstructions = replacePlaceholders(dockerInstallInstructions, edge);
        return new EdgeInstallInstructions(dockerInstallInstructions);
    }

    private EdgeInstallInstructions getUbuntuInstallInstructions(Edge edge, HttpServletRequest request) {
        String ubuntuInstallInstructions = readFile(resolveFile("ubuntu", "instructions.md"));
        ubuntuInstallInstructions = replacePlaceholders(ubuntuInstallInstructions, edge);
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${BASE_URL}", request.getServerName());
        String edgeVersion = appVersion.replace("-SNAPSHOT", "");
        ubuntuInstallInstructions = ubuntuInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        return new EdgeInstallInstructions(ubuntuInstallInstructions);
    }


    private EdgeInstallInstructions getCentosInstallInstructions(Edge edge, HttpServletRequest request) {
        String centosInstallInstructions = readFile(resolveFile("centos", "instructions.md"));
        centosInstallInstructions = replacePlaceholders(centosInstallInstructions, edge);
        centosInstallInstructions = centosInstallInstructions.replace("${BASE_URL}", request.getServerName());
        String edgeVersion = appVersion.replace("-SNAPSHOT", "");
        centosInstallInstructions = centosInstallInstructions.replace("${TB_EDGE_VERSION}", edgeVersion);
        return new EdgeInstallInstructions(centosInstallInstructions);
    }

    private String replacePlaceholders(String instructions, Edge edge) {
        instructions = instructions.replace("${CLOUD_ROUTING_KEY}", edge.getRoutingKey());
        instructions = instructions.replace("${CLOUD_ROUTING_SECRET}", edge.getSecret());
        instructions = instructions.replace("${CLOUD_RPC_PORT}", Integer.toString(rpcPort));
        instructions = instructions.replace("${CLOUD_RPC_SSL_ENABLED}", Boolean.toString(sslEnabled));
        return instructions;
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
        return Paths.get(installScripts.getDataDir(), InstallScripts.JSON_DIR, EDGE_DIR, EDGE_INSTALL_INSTRUCTIONS_DIR);
    }
}
