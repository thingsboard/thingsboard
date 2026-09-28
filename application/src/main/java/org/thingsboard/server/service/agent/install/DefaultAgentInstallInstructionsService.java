// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.install;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.agent.upgrade.AgentUpgradeVersionService;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentInstructions;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.dao.util.DeviceConnectivityUtil;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Service
@TbCoreComponent
@Slf4j
public class DefaultAgentInstallInstructionsService implements AgentInstallInstructionsService {

    private final AgentUpgradeVersionService agentUpgradeVersionService;

    public DefaultAgentInstallInstructionsService(AgentUpgradeVersionService agentUpgradeVersionService) {
        this.agentUpgradeVersionService = agentUpgradeVersionService;
    }

    private static final String METHOD_DOCKER = "docker";

    private static final String DOCKER_ADD_HOST_OPTION = "  --add-host=" + DeviceConnectivityUtil.HOST_DOCKER_INTERNAL + ":host-gateway \\\n";

    private static final String DOCKER_INSTALL_TEMPLATE = """
            docker run -d \\
            ${EXTRA_HOSTS}  --name=tb-agent \\
              --restart=always \\
              -v /var/run/docker.sock:/var/run/docker.sock:ro \\
              -v tb-agent-data:/root/.tb-agent \\
              -v /:/host:ro \\
              -e TB_SERVER_ADDR=${BASE_URL}:${RPC_PORT} \\
              -e TB_RPC_SSL_ENABLED=${RPC_SSL_ENABLED} \\
              -e TB_AGENT_ROUTING_KEY=${ROUTING_KEY} \\
              -e TB_AGENT_ROUTING_SECRET=${ROUTING_SECRET} \\
              ${AGENT_IMAGE}""";

    private static final String DOCKER_PROVISION_TEMPLATE = """
            docker run -d \\
            ${EXTRA_HOSTS}  --name=tb-agent \\
              --restart=always \\
              -v /var/run/docker.sock:/var/run/docker.sock:ro \\
              -v tb-agent-data:/root/.tb-agent \\
              -v /:/host:ro \\
              -e TB_SERVER_ADDR=${BASE_URL}:${RPC_PORT} \\
              -e TB_RPC_SSL_ENABLED=${RPC_SSL_ENABLED} \\
              -e AUTO_PROVISION=true \\
              -e TB_PROVISION_KEY=${PROVISION_KEY} \\
              -e TB_PROVISION_SECRET=${PROVISION_SECRET} \\
              ${AGENT_IMAGE}""";

    @Value("${edges.rpc.port:7070}")
    private int rpcPort;

    @Value("${edges.rpc.ssl.enabled:false}")
    private boolean sslEnabled;

    @Override
    public AgentInstructions getInstallInstructions(Agent agent, String method, HttpServletRequest request) {
        if (!METHOD_DOCKER.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("Unsupported installation method for Agent: " + method);
        }
        String resolved = resolveHost(DOCKER_INSTALL_TEMPLATE, request)
                .replace("${AGENT_IMAGE}", agentUpgradeVersionService.getLatestImageRef())
                .replace("${RPC_PORT}", Integer.toString(rpcPort))
                .replace("${RPC_SSL_ENABLED}", Boolean.toString(sslEnabled))
                .replace("${ROUTING_KEY}", nullSafe(agent.getRoutingKey()))
                .replace("${ROUTING_SECRET}", nullSafe(agent.getSecret()));
        return new AgentInstructions(resolved);
    }

    @Override
    public AgentInstructions getProvisionInstructions(AgentProfile profile, String method, HttpServletRequest request) {
        if (!METHOD_DOCKER.equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("Unsupported provision method for Agent profile: " + method);
        }
        String resolved = resolveHost(DOCKER_PROVISION_TEMPLATE, request)
                .replace("${AGENT_IMAGE}", agentUpgradeVersionService.getLatestImageRef())
                .replace("${RPC_PORT}", Integer.toString(rpcPort))
                .replace("${RPC_SSL_ENABLED}", Boolean.toString(sslEnabled))
                .replace("${PROVISION_KEY}", nullSafe(profile.getProvisionKey()))
                .replace("${PROVISION_SECRET}", nullSafe(profile.getProvisionSecret()));
        return new AgentInstructions(resolved);
    }

    private String resolveHost(String template, HttpServletRequest request) {
        String serverName = request.getServerName();
        if (DeviceConnectivityUtil.isLocalhost(serverName)) {
            return template
                    .replace("${EXTRA_HOSTS}", DOCKER_ADD_HOST_OPTION)
                    .replace("${BASE_URL}", DeviceConnectivityUtil.HOST_DOCKER_INTERNAL);
        }
        return template
                .replace("${EXTRA_HOSTS}", "")
                .replace("${BASE_URL}", serverName);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

}
