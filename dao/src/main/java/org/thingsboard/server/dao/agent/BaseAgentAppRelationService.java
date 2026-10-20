// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.config.GatewayEnvSchema;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.BasicMqttCredentialsIds;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BaseAgentAppRelationService implements AgentAppRelationService {

    private static final String ACCESS_TOKEN_SECURITY_TYPE = "accessToken";
    private static final String USERNAME_PASSWORD_SECURITY_TYPE = "usernamePassword";

    private final EdgeService edgeService;
    private final DeviceService deviceService;
    private final DeviceCredentialsService deviceCredentialsService;
    private final AgentApplicationDao agentApplicationDao;
    private final RelationService relationService;

    @Override
    public EntityId findRelatedEntityByConfig(TenantId tenantId, AgentApplication app) {
        if (app.getAppType() == null || app.getConfig() == null) {
            return null;
        }
        EntityType relatedType = app.getAppType().getRelatedEntityType();
        if (relatedType == null) {
            return null;
        }
        return switch (relatedType) {
            case EDGE -> resolveEdgeId(tenantId, app);
            case DEVICE -> resolveDeviceId(tenantId, app);
            default -> null;
        };
    }

    @Override
    public EntityId findRelatedEntity(TenantId tenantId, AgentApplication app) {
        if (app.getId() == null) {
            return null;
        }
        return relationService.findByTo(tenantId, app.getId(), RelationTypeGroup.AGENT)
                .stream().map(EntityRelation::getFrom).findFirst().orElse(null);
    }

    @Override
    public void createOrUpdateRelation(TenantId tenantId, AgentApplicationId appId, EntityId relatedEntityId) {
        deleteRelation(tenantId, appId);
        relationService.saveRelation(tenantId, new EntityRelation(relatedEntityId, appId, EntityRelation.MANAGED_BY_AGENT_APP_TYPE, RelationTypeGroup.AGENT));
    }

    @Override
    public void deleteRelation(TenantId tenantId, AgentApplicationId appId) {
        relationService.findByTo(tenantId, appId, RelationTypeGroup.AGENT)
                .forEach(relation -> relationService.deleteRelation(tenantId, relation));
    }

    @Override
    public void validateRelatedEntityInTenant(TenantId tenantId, EntityId relatedEntityId) {
        boolean ownedByTenant = switch (relatedEntityId.getEntityType()) {
            case DEVICE -> deviceService.findDeviceById(tenantId, new DeviceId(relatedEntityId.getId())) != null;
            case EDGE -> {
                Edge edge = edgeService.findEdgeById(tenantId, new EdgeId(relatedEntityId.getId()));
                yield edge != null && tenantId.equals(edge.getTenantId());
            }
            default -> false;
        };
        if (!ownedByTenant) {
            throw new DataValidationException("Entity " + relatedEntityId + " is not found in tenant " + tenantId);
        }
    }

    @Override
    public void validateRelatedEntityNotManaged(TenantId tenantId, AgentApplication app, EntityId relatedEntityId) {
        AgentApplication existing = agentApplicationDao.findByRelatedEntity(tenantId, relatedEntityId.getId());
        if (existing != null && (app.getId() == null || !existing.getUuidId().equals(app.getUuidId()))) {
            throw new DataValidationException("Entity is already managed by another agent application");
        }
    }

    private EntityId resolveEdgeId(TenantId tenantId, AgentApplication app) {
        String routingKey = app.getConfig().getEdgeRoutingKey();
        if (routingKey == null) {
            return null;
        }
        return edgeService.findEdgeByRoutingKey(tenantId, routingKey)
                .map(Edge::getId)
                .orElse(null);
    }

    private EntityId resolveDeviceId(TenantId tenantId, AgentApplication app) {
        if (!(app.getConfig() instanceof DockerComposeConfig composeConfig)) {
            return null;
        }
        String credentialsId = resolveGatewayCredentialsId(composeConfig);
        if (credentialsId == null) {
            return null;
        }
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(credentialsId);
        if (credentials == null || credentials.getDeviceId() == null) {
            return null;
        }
        if (deviceService.findDeviceById(tenantId, credentials.getDeviceId()) == null) {
            return null;
        }
        return credentials.getDeviceId();
    }

    private String resolveGatewayCredentialsId(DockerComposeConfig config) {
        var imagePattern = AgentApplicationType.GATEWAY.getMainImagePattern();
        JsonNode compose = config.getCompose();
        GatewayEnvSchema schema = GatewayEnvSchema.detect(compose, imagePattern);
        String accessToken = DockerComposeUtils.getEnvVariable(compose, imagePattern, schema.getAccessTokenKey());
        String clientId = DockerComposeUtils.getEnvVariable(compose, imagePattern, schema.getClientIdKey());
        String userName = DockerComposeUtils.getEnvVariable(compose, imagePattern, schema.getUsernameKey());
        String securityType = resolveGatewaySecurityType(compose, imagePattern, schema, accessToken, clientId, userName);
        if (ACCESS_TOKEN_SECURITY_TYPE.equals(securityType)) {
            return accessToken;
        } else if (USERNAME_PASSWORD_SECURITY_TYPE.equals(securityType)) {
            return BasicMqttCredentialsIds.toCredentialsId(clientId, userName);
        }
        return null;
    }

    // legacy gateway images have no security-type variable; the auth type is implied by which credential keys are set
    private static String resolveGatewaySecurityType(JsonNode compose, Pattern imagePattern, GatewayEnvSchema schema,
                                                     String accessToken, String clientId, String userName) {
        if (schema.getSecurityTypeKey() != null) {
            String securityType = DockerComposeUtils.getEnvVariable(compose, imagePattern, schema.getSecurityTypeKey());
            if (securityType != null) {
                return securityType;
            }
        }
        if (accessToken != null) {
            return ACCESS_TOKEN_SECURITY_TYPE;
        }
        return clientId != null || userName != null ? USERNAME_PASSWORD_SECURITY_TYPE : null;
    }

}
