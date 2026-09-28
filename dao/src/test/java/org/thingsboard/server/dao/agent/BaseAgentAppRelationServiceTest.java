// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.BasicMqttCredentialsIds;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseAgentAppRelationServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());

    @Mock
    private EdgeService edgeService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private DeviceCredentialsService deviceCredentialsService;
    @Mock
    private AgentApplicationDao agentApplicationDao;
    @Mock
    private RelationService relationService;

    @InjectMocks
    private BaseAgentAppRelationService service;

    @Test
    void modernGatewayComposeResolvesItsDeviceByAccessToken() {
        stubCredentials("modern-token");

        EntityId resolved = service.findRelatedEntityByConfig(TENANT_ID, gatewayApp("""
                {"services": {"tb-gw": {"image": "thingsboard/tb-gateway:3.7", "environment": {
                  "TB_GW_SECURITY_TYPE": "accessToken", "TB_GW_ACCESS_TOKEN": "modern-token"}}}}
                """));

        assertThat(resolved).isEqualTo(DEVICE_ID);
    }

    @Test
    void legacyGatewayComposeResolvesItsDeviceByAccessToken() {
        stubCredentials("legacy-token");

        EntityId resolved = service.findRelatedEntityByConfig(TENANT_ID, gatewayApp("""
                {"services": {"tb-gw": {"image": "thingsboard/tb-gateway:3.5", "environment": {
                  "host": "tb", "accessToken": "legacy-token"}}}}
                """));

        assertThat(resolved).isEqualTo(DEVICE_ID);
    }

    @Test
    void legacyGatewayComposeResolvesItsDeviceByClientIdAndUserName() {
        stubCredentials(BasicMqttCredentialsIds.toCredentialsId("gw-client", "gw-user"));

        EntityId resolved = service.findRelatedEntityByConfig(TENANT_ID, gatewayApp("""
                {"services": {"tb-gw": {"image": "thingsboard/tb-gateway:3.5", "environment": {
                  "host": "tb", "clientId": "gw-client", "username": "gw-user", "password": "secret"}}}}
                """));

        assertThat(resolved).isEqualTo(DEVICE_ID);
    }

    @Test
    void composeWithoutAnyCredentialKeysResolvesNothing() {
        EntityId resolved = service.findRelatedEntityByConfig(TENANT_ID, gatewayApp("""
                {"services": {"tb-gw": {"image": "thingsboard/tb-gateway:3.5", "environment": {"host": "tb"}}}}
                """));

        assertThat(resolved).isNull();
    }

    private void stubCredentials(String credentialsId) {
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setDeviceId(DEVICE_ID);
        credentials.setCredentialsId(credentialsId);
        lenient().when(deviceCredentialsService.findDeviceCredentialsByCredentialsId(credentialsId)).thenReturn(credentials);
        lenient().when(deviceService.findDeviceById(any(), any())).thenReturn(new Device(DEVICE_ID));
    }

    private static AgentApplication gatewayApp(String compose) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(readTree(compose));
        AgentApplication app = new AgentApplication();
        app.setAppType(AgentApplicationType.GATEWAY);
        app.setConfig(config);
        return app;
    }

    private static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
