/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.edge.EdgeHighPriorityMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProtoUtilsTest {

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("35e10f77-16e7-424d-ae46-ee780f87ac4f"));
    EntityId entityId = new RuleChainId(UUID.fromString("c640b635-4f0f-41e6-b10b-25a86003094e"));
    DeviceId deviceId = new DeviceId(UUID.fromString("ceebb9e5-4239-437c-a507-dc5f71f1232d"));
    EdgeId edgeId = new EdgeId(UUID.fromString("364be452-2183-459b-af93-1ddb325feac1"));
    UUID id = UUID.fromString("31a07d85-6ed5-46f8-83c0-6715cb0a8782");
    static EasyRandom easyRandom;

    @BeforeAll
    static void init() {
        EasyRandomParameters parameters = new EasyRandomParameters()
                .randomize(DeviceConfiguration.class, DefaultDeviceConfiguration::new)
                .randomize(DeviceTransportConfiguration.class, DefaultDeviceTransportConfiguration::new)
                .randomize(JsonNode.class, JacksonUtil::newObjectNode)
                .randomize(DeviceProfileData.class, DeviceProfileData::new)
                .randomize(TenantProfileConfiguration.class, DefaultTenantProfileConfiguration::new)
                .randomize(EntityId.class, () -> new DeviceId(UUID.randomUUID()));
        easyRandom = new EasyRandom(parameters);
    }

    @Test
    void protoComponentLifecycleSerialization() {
        ComponentLifecycleMsg msg = new ComponentLifecycleMsg(tenantId, entityId, ComponentLifecycleEvent.UPDATED);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
        msg = new ComponentLifecycleMsg(tenantId, entityId, ComponentLifecycleEvent.STARTED);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoEntityTypeSerialization() {
        for (EntityType entityType : EntityType.values()) {
            assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(entityType))).as(entityType.getNormalName()).isEqualTo(entityType);
        }
    }

    @Test
    void protoComponentLifecycleEventSerialization() {
        for (ComponentLifecycleEvent event : ComponentLifecycleEvent.values()) {
            assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(event))).isEqualTo(event);
        }
    }

    @Test
    void protoEdgeHighPrioritySerialization() {
        EdgeHighPriorityMsg msg = new EdgeHighPriorityMsg(tenantId, EdgeUtils.constructEdgeEvent(tenantId, edgeId,
                EdgeEventType.DEVICE, EdgeEventActionType.ADDED, deviceId, JacksonUtil.newObjectNode()));
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoEdgeEventUpdateSerialization() {
        EdgeEventUpdateMsg msg = new EdgeEventUpdateMsg(tenantId, edgeId);
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoToEdgeSyncRequestSerialization() {
        ToEdgeSyncRequest msg = new ToEdgeSyncRequest(id, tenantId, edgeId, "serviceId");
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoFromEdgeSyncResponseSerialization() {
        FromEdgeSyncResponse msg = new FromEdgeSyncResponse(id, tenantId, edgeId, true, "Error Msg");
        assertThat(ProtoUtils.fromProto(ProtoUtils.toProto(msg))).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoDeviceEdgeUpdateSerialization() {
        DeviceEdgeUpdateMsg msg = new DeviceEdgeUpdateMsg(tenantId, deviceId, edgeId);
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoDeviceNameOrTypeSerialization() {
        String deviceName = "test", deviceType = "test";
        DeviceNameOrTypeUpdateMsg msg = new DeviceNameOrTypeUpdateMsg(tenantId, deviceId, deviceName, deviceType);
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoDeviceAttributesEventSerialization() {
        DeviceAttributesEventNotificationMsg msg = new DeviceAttributesEventNotificationMsg(tenantId, deviceId, null, "CLIENT_SCOPE",
                List.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry("key", "value"))), false);
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);

        msg = new DeviceAttributesEventNotificationMsg(tenantId, deviceId, null, "SERVER_SCOPE",
                List.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("doubleEntry", 231.5)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new JsonDataEntry("jsonEntry", "jsonValue"))), false);
        serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);

        msg = new DeviceAttributesEventNotificationMsg(tenantId, deviceId, null, "SERVER_SCOPE",
                List.of(new BaseAttributeKvEntry(System.currentTimeMillis(), new DoubleDataEntry("entry", 11.3)),
                        new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("jsonEntry", true))), false);
        serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);

        msg = new DeviceAttributesEventNotificationMsg(tenantId, deviceId, Set.of(new AttributeKey("SHARED_SCOPE", "attributeKey")), null, null, true);
        serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoDeviceCredentialsUpdateSerialization() {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(deviceId);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsValue("test");
        deviceCredentials.setCredentialsId("test");
        DeviceCredentialsUpdateNotificationMsg msg = new DeviceCredentialsUpdateNotificationMsg(tenantId, deviceId, deviceCredentials);
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoToDeviceRpcRequestSerialization() {
        String serviceId = "cadcaac6-85c3-4211-9756-f074dcd1e7f7";
        ToDeviceRpcRequest request = new ToDeviceRpcRequest(id, tenantId, deviceId, true, 0, new ToDeviceRpcRequestBody("method", "params"), false, 0, "");
        ToDeviceRpcRequestActorMsg msg = new ToDeviceRpcRequestActorMsg(serviceId, request);
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoFromDeviceRpcResponseSerialization() {
        FromDeviceRpcResponseActorMsg msg = new FromDeviceRpcResponseActorMsg(23, tenantId, deviceId, new FromDeviceRpcResponse(id, "response", RpcError.NOT_FOUND));
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    @Test
    void protoRemoveRpcActorSerialization() {
        RemoveRpcActorMsg msg = new RemoveRpcActorMsg(tenantId, deviceId, id);
        TransportProtos.ToDeviceActorNotificationMsgProto serializedMsg = ProtoUtils.toProto(msg);
        Assertions.assertNotNull(serializedMsg);
        assertThat(ProtoUtils.fromProto(serializedMsg)).as("deserialized").isEqualTo(msg);
    }

    private static final String description = "Failed to deserialize %s, because found some new fields which absent in %sProto!!!";

    @Test
    void protoSerializationDeserializationEntities() {
        Device expectedDevice = easyRandom.nextObject(Device.class);
        TransportProtos.DeviceProto deviceProto = ProtoUtils.toProto(expectedDevice);
        Device actualDevice = ProtoUtils.fromProto(deviceProto);
        assertEqualDeserializedEntity(expectedDevice, actualDevice, "Device");

        DeviceCredentials expectedCredentials = easyRandom.nextObject(DeviceCredentials.class);
        TransportProtos.DeviceCredentialsProto credentialsProto = ProtoUtils.toProto(expectedCredentials);
        DeviceCredentials actualCredentials = ProtoUtils.fromProto(credentialsProto);
        assertEqualDeserializedEntity(expectedCredentials, actualCredentials, "DeviceCredentials");

        DeviceProfile expectedDeviceProfile = easyRandom.nextObject(DeviceProfile.class);
        TransportProtos.DeviceProfileProto deviceProfileProto = ProtoUtils.toProto(expectedDeviceProfile);
        DeviceProfile actualDeviceProfile = ProtoUtils.fromProto(deviceProfileProto);
        assertEqualDeserializedEntity(expectedDeviceProfile, actualDeviceProfile, "DeviceProfile");

        Tenant expectedTenant = easyRandom.nextObject(Tenant.class);
        TransportProtos.TenantProto tenantProto = ProtoUtils.toProto(expectedTenant);
        Tenant actualTenant = ProtoUtils.fromProto(tenantProto);
        assertEqualDeserializedEntity(expectedTenant, actualTenant, "Tenant");

        TenantProfile expectedTenantProfile = easyRandom.nextObject(TenantProfile.class);
        TransportProtos.TenantProfileProto tenantProfileProto = ProtoUtils.toProto(expectedTenantProfile);
        TenantProfile actualTenantProfile = ProtoUtils.fromProto(tenantProfileProto);
        assertEqualDeserializedEntity(expectedTenantProfile, actualTenantProfile, "TenantProfile");

        TbResource expectedResource = easyRandom.nextObject(TbResource.class);
        TransportProtos.TbResourceProto resourceProto = ProtoUtils.toProto(expectedResource);
        TbResource actualResource = ProtoUtils.fromProto(resourceProto);
        assertEqualDeserializedEntity(expectedResource, actualResource, "TbResource");

        ApiUsageState expectedState = easyRandom.nextObject(ApiUsageState.class);
        TransportProtos.ApiUsageStateProto stateProto = ProtoUtils.toProto(expectedState);
        ApiUsageState actualState = ProtoUtils.fromProto(stateProto);
        assertEqualDeserializedEntity(expectedState, actualState, "ApiUsageState");

        RepositorySettings expectedSettings = easyRandom.nextObject(RepositorySettings.class);
        TransportProtos.RepositorySettingsProto settingsProto = ProtoUtils.toProto(expectedSettings);
        RepositorySettings actualSettings = ProtoUtils.fromProto(settingsProto);
        assertEqualDeserializedEntity(expectedSettings, actualSettings, "RepositorySettings");
    }

    private void assertEqualDeserializedEntity(Object expected, Object actual, String entityName) {
        assertThat(actual).as(String.format(description, entityName, entityName)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"{\"key\":\"value\"}"})
    void testRpcWithVariousAdditionalInfoToProtoAndBack(String additionalInfo) {
        UUID requestId = UUID.fromString("93405c57-5787-46ff-806e-670bb60f49b6");
        String methodName = "reboot";
        String params = "";
        String serviceId = "serviceId";
        long expirationTime = System.currentTimeMillis();
        int retries = 3;

        ToDeviceRpcRequest request = new ToDeviceRpcRequest(
                requestId,
                tenantId,
                deviceId,
                false,
                expirationTime,
                new ToDeviceRpcRequestBody(methodName, params),
                true,
                retries,
                additionalInfo
        );
        ToDeviceRpcRequestActorMsg msg = new ToDeviceRpcRequestActorMsg(serviceId, request);

        // Serialize
        TransportProtos.ToDeviceActorNotificationMsgProto toProto = ProtoUtils.toProto(msg);
        assertThat(toProto).isNotNull();
        assertThat(toProto.hasToDeviceRpcRequestMsg()).isTrue();

        TransportProtos.ToDeviceRpcRequestActorMsgProto toDeviceRpcRequestActorMsgProto = toProto.getToDeviceRpcRequestMsg();
        assertThat(toDeviceRpcRequestActorMsgProto.hasToDeviceRpcRequestMsg()).isTrue();

        TransportProtos.ToDeviceRpcRequestMsg toDeviceRpcRequestMsg = toDeviceRpcRequestActorMsgProto.getToDeviceRpcRequestMsg();
        assertThat(toDeviceRpcRequestMsg.getRequestIdMSB()).isEqualTo(requestId.getMostSignificantBits());
        assertThat(toDeviceRpcRequestMsg.getRequestIdLSB()).isEqualTo(requestId.getLeastSignificantBits());
        assertThat(toDeviceRpcRequestMsg.getMethodName()).isEqualTo(methodName);
        assertThat(toDeviceRpcRequestMsg.getParams()).isEqualTo(params);
        assertThat(toDeviceRpcRequestMsg.getExpirationTime()).isEqualTo(expirationTime);
        assertThat(toDeviceRpcRequestMsg.getOneway()).isFalse();
        assertThat(toDeviceRpcRequestMsg.getPersisted()).isTrue();
        assertThat(toDeviceRpcRequestMsg.getRetries()).isEqualTo(retries);

        if (additionalInfo != null) {
            assertThat(toDeviceRpcRequestMsg.hasAdditionalInfo()).isTrue();
            assertThat(toDeviceRpcRequestMsg.getAdditionalInfo()).isEqualTo(additionalInfo);
        } else {
            assertThat(toDeviceRpcRequestMsg.hasAdditionalInfo()).isFalse();
        }

        // Deserialize
        ToDeviceActorNotificationMsg fromProto = ProtoUtils.fromProto(toProto);
        assertThat(fromProto).isNotNull();
        assertThat(fromProto).isInstanceOf(ToDeviceRpcRequestActorMsg.class);
        ToDeviceRpcRequestActorMsg toDeviceRpcRequestActorMsg = (ToDeviceRpcRequestActorMsg) fromProto;

        assertThat(toDeviceRpcRequestActorMsg.getDeviceId()).isEqualTo(deviceId);
        assertThat(toDeviceRpcRequestActorMsg.getTenantId()).isEqualTo(tenantId);
        assertThat(toDeviceRpcRequestActorMsg.getServiceId()).isEqualTo(serviceId);
        assertThat(toDeviceRpcRequestActorMsg.getMsg()).isEqualTo(request);
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void testEntityIdProto_toProto_fromProto(EntityType entityType) {
        UUID uuid = UUID.fromString("51a514d7-ea8f-496d-b567-f6e76f0f9b83");

        EntityId original = EntityIdFactory.getByTypeAndUuid(entityType, uuid);
        assertThat(original).isNotNull();

        // toProto
        TransportProtos.EntityIdProto proto = ProtoUtils.toProto(original);
        assertThat(proto).isNotNull();
        assertThat(proto.getType().getNumber()).isEqualTo(entityType.getProtoNumber());
        assertThat(proto.getEntityIdMSB()).isEqualTo(uuid.getMostSignificantBits());
        assertThat(proto.getEntityIdLSB()).isEqualTo(uuid.getLeastSignificantBits());

        // fromProto
        EntityId restored = ProtoUtils.fromProto(proto);
        assertThat(restored).isNotNull().isEqualTo(original);
    }

}
