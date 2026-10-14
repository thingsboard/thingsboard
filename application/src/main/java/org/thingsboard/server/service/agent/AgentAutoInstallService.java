// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.TbVersionUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationOrigin;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.config.GatewayEnvSchema;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.agent.AgentProfileService;
import org.thingsboard.server.dao.agent.AgentService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.edge.TbEdgeService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@TbCoreComponent
@RequiredArgsConstructor
public class AgentAutoInstallService {

    private final AgentService agentService;
    private final AgentProfileService agentProfileService;
    private final AgentAppProfileService profileService;
    private final AgentAppProvisioner appProvisioner;
    private final AgentEventRateLimiter agentEventRateLimiter;
    private final TbEdgeService tbEdgeService;
    private final RuleChainService ruleChainService;
    private final DeviceService deviceService;
    private final SystemSecurityService systemSecurityService;
    private final SubscriptionService subscriptionService;

    // Edge releases older than 4.3 don't know EdgeConfiguration.licenseVersion: they always require an Edge license
    // key and a cloud endpoint, which an add-on Edge never carries, and terminate right after connecting
    static final String MIN_ADD_ON_EDGE_VERSION = "4.3";

    public void autoInstall(TenantId tenantId, AgentId agentId) {
        Agent agent = agentService.findAgentById(tenantId, agentId);
        if (agent == null || agent.getAgentProfileId() == null) {
            log.trace("[{}][{}] Skipping auto-install: agent not found or has no profile", tenantId, agentId);
            return;
        }
        AgentProfile agentProfile = agentProfileService.findProfileById(tenantId, agent.getAgentProfileId());
        if (isNotEligibleForAppAutoInstall(agentProfile)) {
            log.trace("[{}][{}] Skipping auto-install: provision type does not support app auto-install", tenantId, agentId);
            return;
        }
        List<AgentAppProfile> pendingProfiles = findPendingProfiles(tenantId, agent, agentProfile.getProvisionType());

        if (pendingProfiles.isEmpty()) {
            log.trace("[{}][{}] No profiles pending auto-install", tenantId, agentId);
            return;
        }

        log.info("[{}][{}] Auto-installing {} profile(s)", tenantId, agentId, pendingProfiles.size());
        for (AgentAppProfile profile : pendingProfiles) {
            try {
                provisionForProfile(tenantId, agent, profile);
            } catch (Exception e) {
                log.error("[{}][{}] Failed to auto-install profile [{}]", tenantId, agentId, profile.getId(), e);
            }
        }
    }

    private boolean isNotEligibleForAppAutoInstall(AgentProfile agentProfile) {
        return agentProfile == null || agentProfile.getProvisionType() == null || !agentProfile.getProvisionType().isAppAutoInstallSupported();
    }

    private List<AgentAppProfile> findPendingProfiles(TenantId tenantId, Agent agent, AgentProvisionType provisionType) {
        return switch (provisionType) {
            case AUTO_INSTALL_PER_APP_PROFILE -> profileService.findUninstalledAppProfilesForAgentProfile(
                    tenantId, agent.getAgentProfileId(), agent.getId());
            case AUTO_INSTALL_PER_APP_TYPE -> dedupeNonGenericByAppType(
                    profileService.findUninstalledAppProfilesByAppTypeForAgentProfile(tenantId, agent.getAgentProfileId(), agent.getId()));
            default -> throw new IllegalStateException("Unknown provision type: " + provisionType);
        };
    }

    private List<AgentAppProfile> dedupeNonGenericByAppType(List<AgentAppProfile> profiles) {
        Map<AgentApplicationType, AgentAppProfile> selectedByType = profiles.stream()
                .filter(profile -> profile.getAppType() != AgentApplicationType.GENERIC)
                .collect(Collectors.toMap(
                        AgentAppProfile::getAppType,
                        Function.identity(),
                        (selected, duplicate) -> selected,
                        () -> new EnumMap<>(AgentApplicationType.class))
                );

        return profiles.stream()
                .filter(profile -> profile.getAppType() == AgentApplicationType.GENERIC
                        || selectedByType.get(profile.getAppType()) == profile)
                .toList();
    }

    private void provisionForProfile(TenantId tenantId, Agent agent, AgentAppProfile profile) {
        // rate-limit before creating any related entity so an over-quota reject doesn't orphan an Edge/Device
        agentEventRateLimiter.checkOrThrow(tenantId, agent.getId());
        String projectName = AgentApplication.generateProjectName();
        GroupEntity<? extends EntityId> relatedEntity = createRelatedEntityIfNeeded(tenantId, profile, projectName);
        try {
            EntityId relatedEntityId = relatedEntity == null ? null : relatedEntity.getId();

            AgentApplication app = new AgentApplication();
            app.setTenantId(tenantId);
            app.setAgentId(agent.getId());
            app.setName(resolveAppName(profile, relatedEntity, projectName));
            app.setAppType(profile.getAppType());
            app.setTemplateVersion(profile.getTemplateVersion());
            app.setApplicationProfileId(profile.getId());
            app.setOrigin(AgentApplicationOrigin.AUTO_PROVISIONED);
            app.setProjectName(projectName);
            // profile's config will be set to the application implicitly inside applicationService#saveWithRelatedEntity
            AgentApplication savedApp = appProvisioner.saveWithLifecycleEvent(tenantId, app, relatedEntityId, AgentAppEventActionType.INSTALL);
            log.info("[{}][{}] Auto-provisioned application [{}] for profile [{}]",
                    tenantId, agent.getId(), savedApp.getId(), profile.getId());
        } catch (Exception e) {
            rollbackRelatedEntity(tenantId, relatedEntity);
            throw e;
        }
    }

    private void rollbackRelatedEntity(TenantId tenantId, GroupEntity<? extends EntityId> relatedEntity) {
        if (relatedEntity == null) {
            return;
        }
        try {
            switch (relatedEntity) {
                case Edge edge -> tbEdgeService.delete(edge, null);
                case Device device -> deviceService.deleteDevice(tenantId, device.getId());
                default -> {
                    log.warn("[{}] No rollback handler for auto-created entity [{}]", tenantId, relatedEntity.getId());
                    return;
                }
            }
            log.info("[{}] Rolled back auto-created entity [{}] after failed provisioning", tenantId, relatedEntity.getId());
        } catch (Exception e) {
            log.error("[{}] Failed to roll back auto-created entity [{}]", tenantId, relatedEntity.getId(), e);
        }
    }

    private String resolveAppName(AgentAppProfile profile, GroupEntity<? extends EntityId> relatedEntity, String projectName) {
        return relatedEntity == null
                ? profile.getAppType() + " " + projectName
                : relatedEntity.getName() + " application";
    }

    private GroupEntity<? extends EntityId> createRelatedEntityIfNeeded(TenantId tenantId, AgentAppProfile profile, String projectName) {
        if (profile.getAppType() == null) {
            return null;
        }
        var relatedType = profile.getAppType().getRelatedEntityType();
        if (relatedType == null) {
            return null;
        }
        return switch (relatedType) {
            case EDGE -> createEdge(tenantId, profile, projectName);
            case DEVICE -> createGatewayDevice(tenantId, profile, projectName);
            default -> null;
        };
    }

    private Edge createEdge(TenantId tenantId, AgentAppProfile profile, String projectName) {
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(tenantId);
        if (edgeTemplateRootRuleChain == null) {
            throw new NoSuchElementException("Root edge rule chain is not available!");
        }
        if (!isSupportedEdgeVersion(subscriptionService.getLicenseVersion(), profile.getTemplateVersion())) {
            throw new IllegalStateException(String.format(
                    "Edge version %s of profile %s is not supported for add-on Edges, use Edge version %s or later",
                    profile.getTemplateVersion(), profile.getId(), MIN_ADD_ON_EDGE_VERSION));
        }
        Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName("Edge-" + projectName);
        edge.setType("default");
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setEdgeLicenseKey(EdgeUtils.DEFAULT_EDGE_LICENSE_KEY);
        edge.setCloudEndpoint(systemSecurityService.getBaseUrl(tenantId, null, null));
        Edge saved;
        try {
            saved = tbEdgeService.save(edge, edgeTemplateRootRuleChain, Collections.emptyList(), null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to auto-create Edge for profile " + profile.getId(), e);
        }
        log.info("[{}] Auto-created Edge [{}] for profile [{}]", tenantId, saved.getId(), profile.getId());
        return saved;
    }

    static boolean isSupportedEdgeVersion(int licenseVersion, String templateVersion) {
        return !EdgeUtils.isAddonEdge(licenseVersion)
                || TbVersionUtils.compare(TbVersionUtils.extractStartingDigits(templateVersion), MIN_ADD_ON_EDGE_VERSION) >= 0;
    }

    /**
     * Reverse-engineers credentials type from the profile's compose env vars so that the
     * device credentials match what MergeCredentialsToConfigRule expects to inject into compose.
     */
    private Device createGatewayDevice(TenantId tenantId, AgentAppProfile profile, String projectName) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Gateway-" + projectName);
        device.setAdditionalInfo(JacksonUtil.newObjectNode().put(DataConstants.GATEWAY_PARAMETER, true));
        // deviceProfileId auto-resolved to default by DeviceServiceImpl when null

        String securityType = extractGatewaySecurityType(profile);
        if ("usernamePassword".equals(securityType)) {
            DeviceCredentials creds = new DeviceCredentials();
            creds.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);
            BasicMqttCredentials basic = new BasicMqttCredentials();
            basic.setClientId(StringUtils.randomAlphanumeric(20));
            basic.setUserName(StringUtils.randomAlphanumeric(20));
            basic.setPassword(StringUtils.randomAlphanumeric(20));
            creds.setCredentialsValue(JacksonUtil.toString(basic));
            // credentialsId for MQTT_BASIC is computed by DeviceCredentialsService
            Device saved = deviceService.saveDeviceWithCredentials(device, creds);
            log.info("[{}] Auto-created gateway Device [{}] (MQTT_BASIC) for profile [{}]", tenantId, saved.getId(), profile.getId());
            return saved;
        }

        // Default: ACCESS_TOKEN — null lets DeviceServiceImpl generate a 20-char random token
        Device saved = deviceService.saveDeviceWithAccessToken(device, null);
        log.info("[{}] Auto-created gateway Device [{}] (ACCESS_TOKEN) for profile [{}]", tenantId, saved.getId(), profile.getId());
        return saved;
    }

    private String extractGatewaySecurityType(AgentAppProfile profile) {
        if (!(profile.getConfig() instanceof DockerComposeConfig cfg) || cfg.getCompose() == null) {
            return null;
        }
        var imagePattern = AgentApplicationType.GATEWAY.getMainImagePattern();
        GatewayEnvSchema schema = GatewayEnvSchema.detect(cfg.getCompose(), imagePattern);
        if (schema.getSecurityTypeKey() != null) {
            String securityType = DockerComposeUtils.getEnvVariable(cfg.getCompose(), imagePattern, schema.getSecurityTypeKey());
            if (securityType != null) {
                return securityType;
            }
        }
        // legacy schema has no security-type variable; infer the auth type from the credential keys
        boolean usernamePassword = DockerComposeUtils.getEnvVariable(cfg.getCompose(), imagePattern, schema.getUsernameKey()) != null
                && DockerComposeUtils.getEnvVariable(cfg.getCompose(), imagePattern, schema.getAccessTokenKey()) == null;
        return usernamePassword ? "usernamePassword" : "accessToken";
    }
}
