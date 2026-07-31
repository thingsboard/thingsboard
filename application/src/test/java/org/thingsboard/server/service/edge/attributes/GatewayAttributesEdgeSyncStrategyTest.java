/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.attributes;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.service.gateway_device.GatewayFlagCache;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GatewayAttributesEdgeSyncStrategyTest {

    final TenantId tenantId = TenantId.fromUUID(UUID.fromString("a00ec470-c6b4-11ef-8c88-63b5533fb5bc"));
    final DeviceId deviceId = DeviceId.fromString("cc51e450-53e1-11ee-883e-e56b48fd2088");

    @Mock
    TbClusterService clusterService;
    @Mock
    GatewayFlagCache gatewayFlagCache;

    GatewayAttributesEdgeSyncStrategy strategy;

    @BeforeEach
    void setup() {
        strategy = new GatewayAttributesEdgeSyncStrategy(clusterService, gatewayFlagCache);
        lenient().when(gatewayFlagCache.isPotentialGateway(deviceId)).thenReturn(true);
    }

    @Test
    void sharedScopeUpdateRequiresSync() {
        assertThat(strategy.isEdgeSyncRequired(saveRequest(AttributeScope.SHARED_SCOPE, "Modbus"))).isTrue();
    }

    @Test
    void clientScopeUpdateDoesNotRequireSync() {
        assertThat(strategy.isEdgeSyncRequired(saveRequest(AttributeScope.CLIENT_SCOPE, "current_configuration"))).isFalse();
    }

    @Test
    void serverScopeUpdateRequiresSyncExceptActivityKeys() {
        assertThat(strategy.isEdgeSyncRequired(saveRequest(AttributeScope.SERVER_SCOPE, "inactive_connectors"))).isTrue();
        // configuration of a disabled connector is stored in the SERVER_SCOPE attribute named after the connector
        assertThat(strategy.isEdgeSyncRequired(saveRequest(AttributeScope.SERVER_SCOPE, "Modbus"))).isTrue();
        assertThat(strategy.isEdgeSyncRequired(saveRequest(AttributeScope.SERVER_SCOPE,
                "active", "lastConnectTime", "lastDisconnectTime", "lastActivityTime", "inactivityAlarmTime"))).isFalse();
    }

    @Test
    void deleteSyncRequiredChecks() {
        assertThat(strategy.isEdgeSyncRequired(deleteRequest(AttributeScope.SHARED_SCOPE, "Modbus"))).isTrue();
        assertThat(strategy.isEdgeSyncRequired(deleteRequest(AttributeScope.CLIENT_SCOPE, "Modbus"))).isFalse();
        assertThat(strategy.isEdgeSyncRequired(deleteRequest(AttributeScope.SERVER_SCOPE, "inactive_connectors"))).isTrue();
        assertThat(strategy.isEdgeSyncRequired(deleteRequest(AttributeScope.SERVER_SCOPE, "Modbus"))).isTrue();
        assertThat(strategy.isEdgeSyncRequired(deleteRequest(AttributeScope.SERVER_SCOPE, "lastActivityTime"))).isFalse();
    }

    @Test
    void shouldNotRequireSyncWhenDeviceIsKnownToBeNotAGateway() {
        given(gatewayFlagCache.isPotentialGateway(deviceId)).willReturn(false);

        assertThat(strategy.isEdgeSyncRequired(saveRequest(AttributeScope.SHARED_SCOPE, "Modbus"))).isFalse();
        assertThat(strategy.isEdgeSyncRequired(deleteRequest(AttributeScope.SHARED_SCOPE, "Modbus"))).isFalse();
    }

    @Test
    void shouldNotSendNotificationWhenDeviceIsNotGateway() {
        given(gatewayFlagCache.isGateway(tenantId, deviceId)).willReturn(false);

        strategy.onAttributesUpdate(saveRequest(AttributeScope.SHARED_SCOPE, "Modbus"));

        then(clusterService).shouldHaveNoInteractions();
    }

    @Test
    void shouldSendAttributesUpdatedNotificationForGateway() {
        given(gatewayFlagCache.isGateway(tenantId, deviceId)).willReturn(true);

        AttributesSaveRequest request = AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(AttributeScope.SHARED_SCOPE)
                .entries(List.of(
                        new BaseAttributeKvEntry(new StringDataEntry("Modbus", "{\"name\":\"Modbus\"}"), 100L),
                        new BaseAttributeKvEntry(new LongDataEntry("RemoteLoggingLevel", 42L), 200L)
                ))
                .build();
        strategy.onAttributesUpdate(request);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        then(clusterService).should().sendNotificationMsgToEdge(eq(tenantId), isNull(), eq(deviceId), bodyCaptor.capture(),
                eq(EdgeEventType.DEVICE), eq(EdgeEventActionType.ATTRIBUTES_UPDATED), isNull());

        JsonNode body = JacksonUtil.toJsonNode(bodyCaptor.getValue());
        assertThat(body.get("scope").asText()).isEqualTo("SHARED_SCOPE");
        assertThat(body.get("ts").asLong()).isEqualTo(200L);
        assertThat(body.get("kv").get("Modbus").asText()).isEqualTo("{\"name\":\"Modbus\"}");
        assertThat(body.get("kv").get("RemoteLoggingLevel").asLong()).isEqualTo(42L);
    }

    @Test
    void shouldFilterActivityKeysOutOfServerScopeNotification() {
        given(gatewayFlagCache.isGateway(tenantId, deviceId)).willReturn(true);

        strategy.onAttributesUpdate(saveRequest(AttributeScope.SERVER_SCOPE, "inactive_connectors", "Modbus", "lastActivityTime"));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        then(clusterService).should().sendNotificationMsgToEdge(eq(tenantId), isNull(), eq(deviceId), bodyCaptor.capture(),
                eq(EdgeEventType.DEVICE), eq(EdgeEventActionType.ATTRIBUTES_UPDATED), isNull());

        JsonNode kv = JacksonUtil.toJsonNode(bodyCaptor.getValue()).get("kv");
        assertThat(kv.has("inactive_connectors")).isTrue();
        assertThat(kv.has("Modbus")).isTrue();
        assertThat(kv.has("lastActivityTime")).isFalse();
    }

    @Test
    void shouldSendAttributesDeletedNotificationForGateway() {
        given(gatewayFlagCache.isGateway(tenantId, deviceId)).willReturn(true);

        strategy.onAttributesDelete(deleteRequest(AttributeScope.SHARED_SCOPE, "Modbus"));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        then(clusterService).should().sendNotificationMsgToEdge(eq(tenantId), isNull(), eq(deviceId), bodyCaptor.capture(),
                eq(EdgeEventType.DEVICE), eq(EdgeEventActionType.ATTRIBUTES_DELETED), isNull());

        JsonNode body = JacksonUtil.toJsonNode(bodyCaptor.getValue());
        assertThat(body.get("scope").asText()).isEqualTo("SHARED_SCOPE");
        assertThat(body.get("keys")).hasSize(1);
        assertThat(body.get("keys").get(0).asText()).isEqualTo("Modbus");
    }

    AttributesSaveRequest saveRequest(AttributeScope scope, String... keys) {
        return AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(scope)
                .entries(Stream.of(keys)
                        .<AttributeKvEntry>map(key -> new BaseAttributeKvEntry(new StringDataEntry(key, "value"), 100L))
                        .toList())
                .build();
    }

    AttributesDeleteRequest deleteRequest(AttributeScope scope, String... keys) {
        return AttributesDeleteRequest.builder()
                .tenantId(tenantId)
                .entityId(deviceId)
                .scope(scope)
                .keys(List.of(keys))
                .build();
    }

}
