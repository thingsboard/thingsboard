/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.edge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultEdgeInstallService implements EdgeInstallService {

    private static final String EDGE_DIR = "edge";

    private static final String EDGE_INSTALL_INSTRUCTIONS_DIR = "install_instructions";

    private final InstallScripts installScripts;
    private final SystemSecurityService systemSecurityService;

    @Override
    public String getDockerInstallInstructions(TenantId tenantId, Edge edge, HttpServletRequest request) {
        String baseUrl = request.getServerName();
        String template = readFile(resolveFile("docker", "instructions.md"));
        template = template.replace("${BASE_URL}", baseUrl);
        template = template.replace("${CLOUD_ROUTING_KEY}", edge.getRoutingKey());
        template = template.replace("${CLOUD_ROUTING_SECRET}", edge.getSecret());
        return template;
    }

    private String readFile(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
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
