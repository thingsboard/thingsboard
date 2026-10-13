// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.compose;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.RegexUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentAppRelationService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentAppProvisioner;
import org.thingsboard.server.service.agent.AgentEventRateLimiter;
import org.thingsboard.server.service.agent.template.TbAgentAppTemplateService;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;

import static org.thingsboard.server.dao.agent.AgentProfileService.RELATES_ON_AUTO_DISCOVERY;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeAgentAppCreator {

    private final AgentService agentService;
    private final AgentAppProfileService appProfileService;
    private final AgentAppProvisioner appProvisioner;
    private final AgentEventRateLimiter agentEventRateLimiter;
    private final TbAgentAppTemplateService templateService;
    private final AgentAppRelationService appRelationService;

    public AgentApplication createApp(TenantId tenantId, AgentId agentId, String projectName, ComposeState compose) {
        JsonNode composeJson = extractCompose(compose);
        Agent agent = agentService.findAgentById(tenantId, agentId);

        ComposeInfo composeInfo = getComposeInfo(composeJson);

        AgentAppTemplate template = resolveTemplate(composeInfo);
        if (template == null) {
            log.warn("[{}][{}] No template found for appType [{}], version [{}]",
                    tenantId, agentId, composeInfo.appType(), composeInfo.version());
            return null;
        }

        AgentApplication newApp = new AgentApplication();
        newApp.setTenantId(tenantId);
        newApp.setAgentId(agentId);
        newApp.setName(composeInfo.generatePlaceholderAppName(projectName));
        newApp.setProjectName(projectName);
        newApp.setAppType(template.getAppType());
        newApp.setTemplateVersion(template.getCurrentVersion());
        newApp.setOrigin(AgentApplicationOrigin.DISCOVERED);

        Optional<AgentAppProfileRelationInfo> optAppProfile = getAssignedApplicationProfile(tenantId, agent, template);
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(composeJson);

        newApp.setConfig(config);
        EntityId relatedEntityId = appRelationService.findRelatedEntityByConfig(tenantId, newApp);

        if (optAppProfile.isPresent()) {
            // an assigned app profile means the agent must be told to apply it via a downlink UPDATE event, so it is rate-limited
            agentEventRateLimiter.checkOrThrow(tenantId, agentId);
            newApp.setApplicationProfileId(optAppProfile.get().getId());
            newApp.setConfig(null); // set on appProvisioner.saveWithLifecycleEvent
        }
        // an assigned app profile means the agent must be told to apply it, so a default-state UPDATE event is enqueued atomically with the app
        AgentAppEventActionType eventActionType = optAppProfile.isPresent() ? AgentAppEventActionType.UPDATE : null;
        AgentApplication application = appProvisioner.saveWithLifecycleEvent(tenantId, newApp, relatedEntityId, eventActionType);
        log.info("[{}][{}] Created agent application for project [{}], appType [{}], version [{}]",
                tenantId, agentId, application.getProjectName(), composeInfo.appType(), composeInfo.version());
        return application;
    }

    private Optional<AgentAppProfileRelationInfo> getAssignedApplicationProfile(TenantId tenantId, Agent agent, AgentAppTemplate template) {
        return appProfileService.findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(
                        tenantId, agent.getAgentProfileId(), template.getAppType(), template.getCurrentVersion())
                .stream()
                .filter(r -> r.getAdditionalInfo() != null && r.getAdditionalInfo().path(RELATES_ON_AUTO_DISCOVERY).asBoolean(false))
                .findFirst();
    }

    private AgentAppTemplate resolveTemplate(ComposeInfo composeInfo) {
        AgentAppTemplate byAppType = templateService.findByAppTypeAndConfigTypeAndCurrentVersion(
                composeInfo.appType(), AgentAppConfigType.DOCKER_COMPOSE, composeInfo.version());
        if (byAppType != null) {
            return byAppType;
        }
        // fallback: the application will be created with 'GENERIC' type if there's no template with specified version
        AgentApplicationType genericType = AgentApplicationType.GENERIC;
        return templateService.findByAppTypeAndConfigTypeAndCurrentVersion(genericType, AgentAppConfigType.DOCKER_COMPOSE, genericType.getDefaultVersion());
    }

    private ComposeInfo getComposeInfo(JsonNode compose) {
        if (!compose.isObject()) {
            throw new IllegalStateException("Compose is not an object");
        }

        JsonNode services = compose.get("services");
        if (services == null || !services.isObject() || services.isEmpty()) {
            throw new IllegalStateException("Compose has no services defined");
        }

        Iterator<Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            JsonNode service = it.next().getValue();
            ComposeInfo t = resolveAppTypeAndVersion(service);
            if (t != null) return t;
        }
        return ComposeInfo.GENERIC;
    }

    private ComposeInfo resolveAppTypeAndVersion(JsonNode service) {
        if (service.isObject() && service.has("image")) {
            String image = service.get("image").asText();
            for (var t : AgentApplicationType.values()) {
                if (t.getMainImagePattern() != null && RegexUtils.matches(image, t.getMainImagePattern())) {
                    String version = extractVersion(image);
                    return new ComposeInfo(t, image, version);
                }
            }
        }
        return null;
    }

    private String extractVersion(String image) {
        int lastSlash = image.lastIndexOf('/');
        String ref = lastSlash >= 0 ? image.substring(lastSlash + 1) : image;
        int colonIdx = ref.lastIndexOf(':');
        return colonIdx >= 0 ? ref.substring(colonIdx + 1) : null;
    }

    private JsonNode extractCompose(ComposeState composeState) {
        try {
            return JacksonUtil.OBJECT_MAPPER.readTree(composeState.getComposeJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Couldn't parse docker compose");
        }
    }

    private record ComposeInfo(AgentApplicationType appType, String mainServiceImage, String version) {

        public static final ComposeInfo GENERIC = new ComposeInfo(AgentApplicationType.GENERIC, null, AgentApplicationType.GENERIC.getDefaultVersion());

        public String generatePlaceholderAppName(String projectName) {
            if (mainServiceImage != null) {
                return this.appType + "_" + mainServiceImage + "_" + projectName;
            }
            return this.appType + "_" + projectName;
        }
    }
}
