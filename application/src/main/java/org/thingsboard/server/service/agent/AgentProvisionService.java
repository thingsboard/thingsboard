// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@Slf4j
@TbCoreComponent
@RequiredArgsConstructor
public class AgentProvisionService {

    public static final String INVALID_CREDENTIALS = "Invalid provisioning credentials";
    public static final String PROVISIONING_DISABLED = "Auto-provisioning is disabled";

    private final AgentProfileService agentProfileService;
    private final AgentService agentService;

    public ProvisionResult provision(String provisionKey, String provisionSecret) {
        if (StringUtils.isEmpty(provisionKey) || StringUtils.isEmpty(provisionSecret)) {
            return ProvisionResult.failure(INVALID_CREDENTIALS);
        }
        AgentProfile agentProfile = agentProfileService.findProfileByProvisionKey(provisionKey);
        if (agentProfile == null || agentProfile.getProvisionSecret() == null
                || !MessageDigest.isEqual(provisionSecret.getBytes(StandardCharsets.UTF_8),
                agentProfile.getProvisionSecret().getBytes(StandardCharsets.UTF_8))) {
            return ProvisionResult.failure(INVALID_CREDENTIALS);
        }
        if (agentProfile.getProvisionType() == null || agentProfile.getProvisionType() == AgentProvisionType.DISABLED) {
            return ProvisionResult.failure(PROVISIONING_DISABLED);
        }
        String routingKey = StringUtils.randomAlphanumeric(20);
        String routingSecret = StringUtils.randomAlphanumeric(20);

        Agent agent = new Agent();
        agent.setTenantId(agentProfile.getTenantId());
        agent.setAgentProfileId(agentProfile.getId());
        agent.setName("Agent-" + routingKey.substring(0, 8));
        agent.setRoutingKey(routingKey);
        agent.setSecret(routingSecret);
        agentService.saveAgent(agent);

        log.info("Provisioned new agent [{}] for agentProfile [{}]", agent.getName(), agentProfile.getId());
        return ProvisionResult.success(routingKey, routingSecret);
    }

    public record ProvisionResult(boolean success, String routingKey, String routingSecret, String errorMessage) {
        public static ProvisionResult success(String routingKey, String routingSecret) {
            return new ProvisionResult(true, routingKey, routingSecret, null);
        }

        public static ProvisionResult failure(String errorMessage) {
            return new ProvisionResult(false, null, null, errorMessage);
        }
    }
}
