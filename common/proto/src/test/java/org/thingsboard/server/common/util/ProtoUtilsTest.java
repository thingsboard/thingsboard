// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.AssetProfileCacheInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
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
import org.thingsboard.server.gen.integration.ConverterProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.IntegrationProto;
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
    void protoFromDeviceRpcResponseOnewaySerialization() {
        // Oneway RPC success: response and error are both null. Relies on the proto
        // 'optional string response' presence bit so the receiver round-trips null
        // rather than seeing the proto3 default "".
        FromDeviceRpcResponseActorMsg msg = new FromDeviceRpcResponseActorMsg(23, tenantId, deviceId, new FromDeviceRpcResponse(id, null, null));
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

        Integration expectedIntegration = easyRandom.nextObject(Integration.class);
        expectedIntegration.setDebugMode(false); // Debug Mode is always false until removed.
        expectedIntegration.setDebugSettings(DebugSettings.failures());
        IntegrationProto integrationProto = ProtoUtils.toProto(expectedIntegration);
        Integration actualIntegration = ProtoUtils.fromProto(integrationProto);
        assertEqualDeserializedEntity(expectedIntegration, actualIntegration, "Integration");

        Converter expectedConverter = easyRandom.nextObject(Converter.class);
        expectedConverter.setDebugMode(false); // Debug Mode is always false until removed.
        expectedConverter.setDebugSettings(DebugSettings.failures());
        ConverterProto converterProto = ProtoUtils.toProto(expectedConverter);
        Converter actualConverter = ProtoUtils.fromProto(converterProto);
        assertEqualDeserializedEntity(expectedConverter, actualConverter, "Converter");
    }

    private void assertEqualDeserializedEntity(Object expected, Object actual, String entityName) {
        assertThat(actual).as(String.format(description, entityName, entityName)).isEqualTo(expected);
    }

    @Test
    void integrationInfoToProtoCarriesEveryProtoScalar() {
        // The broadcast integration list maps each IntegrationInfo (loaded config-free from the integration info
        // view) via ProtoUtils.toIntegrationInfoProto. This pins that the mapping reads — and so the source
        // query must supply — every scalar IntegrationInfoProto carries, guarding against a newly added proto
        // field shipping empty or a dropped source column.
        IntegrationInfo info = new IntegrationInfo(new IntegrationId(UUID.fromString("0a4a3b2c-1d2e-4f5a-8b9c-0d1e2f3a4b5c")));
        info.setTenantId(TenantId.fromUUID(UUID.fromString("35e10f77-16e7-424d-ae46-ee780f87ac4f")));
        info.setName("Modbus North");
        info.setType(IntegrationType.MQTT);
        // Alternating boolean pattern so a transposition of adjacent boolean fields in the mapping fails.
        info.setEnabled(true);
        info.setRemote(false);
        info.setAllowCreateDevicesOrAssets(true);

        IntegrationInfoProto proto = ProtoUtils.toIntegrationInfoProto(info);

        assertThat(proto.getIntegrationIdMSB()).isEqualTo(info.getId().getId().getMostSignificantBits());
        assertThat(proto.getIntegrationIdLSB()).isEqualTo(info.getId().getId().getLeastSignificantBits());
        assertThat(proto.getTenantIdMSB()).isEqualTo(info.getTenantId().getId().getMostSignificantBits());
        assertThat(proto.getTenantIdLSB()).isEqualTo(info.getTenantId().getId().getLeastSignificantBits());
        assertThat(proto.getName()).isEqualTo(info.getName());
        assertThat(proto.getType()).isEqualTo(info.getType().name());
        assertThat(proto.getEnabled()).isEqualTo(info.isEnabled());
        assertThat(proto.getRemote()).isEqualTo(info.isRemote());
        assertThat(proto.getAllowCreateDevicesOrAssets()).isEqualTo(info.isAllowCreateDevicesOrAssets());

        // Consumer side of the broadcast: the proto round-trips back into an equivalent lightweight Integration.
        Integration back = ProtoUtils.fromProtoToIntegration(proto);
        assertThat(back.getId()).isEqualTo(info.getId());
        assertThat(back.getTenantId()).isEqualTo(info.getTenantId());
        assertThat(back.getName()).isEqualTo(info.getName());
        assertThat(back.getType()).isEqualTo(info.getType());
        assertThat(back.isEnabled()).isEqualTo(info.isEnabled());
        assertThat(back.isRemote()).isEqualTo(info.isRemote());
        assertThat(back.isAllowCreateDevicesOrAssets()).isEqualTo(info.isAllowCreateDevicesOrAssets());
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


    @ParameterizedTest
    @EnumSource(EntityType.class)
    void testEntityIDProto_fromProto_withLegacyField(EntityType entityType) throws Exception {
        UUID uuid = UUID.fromString("51a514d7-ea8f-496d-b567-f6e76f0f9b83");
        EntityId original = EntityIdFactory.getByTypeAndUuid(entityType, uuid);

        TransportProtos.EntityIdProto legacyOnly =
                TransportProtos.EntityIdProto.newBuilder()
                        .setEntityIdMSB(uuid.getMostSignificantBits())
                        .setEntityIdLSB(uuid.getLeastSignificantBits())
                        .setEntityType(entityType.name())
                        .build();

        // fromProto should restore via legacy string
        EntityId restored = ProtoUtils.fromProto(legacyOnly);

        assertThat(restored).isNotNull().isEqualTo(original);

        // Also, verify fromBytes path behaves the same
        byte[] bytes = legacyOnly.toByteArray();
        EntityId restoredFromBytes = ProtoUtils.fromProto(TransportProtos.EntityIdProto.parseFrom(bytes));
        assertThat(restoredFromBytes).isEqualTo(original);
    }

    @Test
    void testToSessionInfoWithNodeId() {
        UUID sessionId = UUID.randomUUID();
        String nodeId = "test-node";
        Device device = easyRandom.nextObject(Device.class);

        TransportProtos.SessionInfoProto sessionInfo = ProtoUtils.toSessionInfo(sessionId, nodeId, device);

        assertThat(sessionInfo.getSessionIdMSB()).isEqualTo(sessionId.getMostSignificantBits());
        assertThat(sessionInfo.getSessionIdLSB()).isEqualTo(sessionId.getLeastSignificantBits());
        assertThat(sessionInfo.getTenantIdMSB()).isEqualTo(device.getTenantId().getId().getMostSignificantBits());
        assertThat(sessionInfo.getTenantIdLSB()).isEqualTo(device.getTenantId().getId().getLeastSignificantBits());
        assertThat(sessionInfo.getDeviceIdMSB()).isEqualTo(device.getId().getId().getMostSignificantBits());
        assertThat(sessionInfo.getDeviceIdLSB()).isEqualTo(device.getId().getId().getLeastSignificantBits());
        assertThat(sessionInfo.getDeviceName()).isEqualTo(device.getName());
        assertThat(sessionInfo.getDeviceType()).isEqualTo(device.getType());
        assertThat(sessionInfo.getDeviceProfileIdMSB()).isEqualTo(device.getDeviceProfileId().getId().getMostSignificantBits());
        assertThat(sessionInfo.getDeviceProfileIdLSB()).isEqualTo(device.getDeviceProfileId().getId().getLeastSignificantBits());
        assertThat(sessionInfo.getNodeId()).isEqualTo(nodeId);
        assertThat(sessionInfo.getIsGateway()).isFalse();

        if (device.getCustomerId() != null && !device.getCustomerId().isNullUid()) {
            assertThat(sessionInfo.getCustomerIdMSB()).isEqualTo(device.getCustomerId().getId().getMostSignificantBits());
            assertThat(sessionInfo.getCustomerIdLSB()).isEqualTo(device.getCustomerId().getId().getLeastSignificantBits());
        }
    }

    @Test
    void protoCacheInfoSerialization() {
        Device expectedDevice = easyRandom.nextObject(Device.class);
        TransportProtos.DeviceCacheInfoProto deviceCacheProto = ProtoUtils.toCacheProto(expectedDevice);
        DeviceCacheInfo actualDeviceCacheInfo = ProtoUtils.fromCacheProto(deviceCacheProto);

        assertThat(actualDeviceCacheInfo.getId()).isEqualTo(expectedDevice.getUuidId());
        assertThat(actualDeviceCacheInfo.name()).isEqualTo(expectedDevice.getName());
        assertThat(actualDeviceCacheInfo.type()).isEqualTo(expectedDevice.getType());
        assertThat(actualDeviceCacheInfo.tenantId()).isEqualTo(expectedDevice.getTenantId());
        assertThat(actualDeviceCacheInfo.customerId()).isEqualTo(expectedDevice.getCustomerId());
        assertThat(actualDeviceCacheInfo.deviceProfileId()).isEqualTo(expectedDevice.getDeviceProfileId());

        Asset expectedAsset = easyRandom.nextObject(Asset.class);
        TransportProtos.AssetCacheInfoProto assetCacheProto = ProtoUtils.toCacheProto(expectedAsset);
        AssetCacheInfo actualAssetCacheInfo = ProtoUtils.fromCacheProto(assetCacheProto);

        assertThat(actualAssetCacheInfo.getId()).isEqualTo(expectedAsset.getUuidId());
        assertThat(actualAssetCacheInfo.name()).isEqualTo(expectedAsset.getName());
        assertThat(actualAssetCacheInfo.type()).isEqualTo(expectedAsset.getType());
        assertThat(actualAssetCacheInfo.tenantId()).isEqualTo(expectedAsset.getTenantId());
        assertThat(actualAssetCacheInfo.customerId()).isEqualTo(expectedAsset.getCustomerId());
        assertThat(actualAssetCacheInfo.assetProfileId()).isEqualTo(expectedAsset.getAssetProfileId());

        DeviceProfile expectedDeviceProfile = easyRandom.nextObject(DeviceProfile.class);
        TransportProtos.DeviceProfileCacheInfoProto deviceProfileCacheProto = ProtoUtils.toCacheProto(expectedDeviceProfile);
        DeviceProfileCacheInfo actualDeviceProfileCacheInfo = ProtoUtils.fromCacheProto(deviceProfileCacheProto);

        assertThat(actualDeviceProfileCacheInfo.getId()).isEqualTo(expectedDeviceProfile.getUuidId());
        assertThat(actualDeviceProfileCacheInfo.tenantId()).isEqualTo(expectedDeviceProfile.getTenantId());
        assertThat(actualDeviceProfileCacheInfo.name()).isEqualTo(expectedDeviceProfile.getName());
        assertThat(actualDeviceProfileCacheInfo.defaultRuleChainId()).isEqualTo(expectedDeviceProfile.getDefaultRuleChainId());
        assertThat(actualDeviceProfileCacheInfo.defaultQueueName()).isEqualTo(expectedDeviceProfile.getDefaultQueueName());

        AssetProfile expectedAssetProfile = easyRandom.nextObject(AssetProfile.class);
        TransportProtos.AssetProfileCacheInfoProto assetProfileCacheProto = ProtoUtils.toCacheProto(expectedAssetProfile);
        org.thingsboard.server.common.data.AssetProfileCacheInfo actualAssetProfileCacheInfo = ProtoUtils.fromCacheProto(assetProfileCacheProto);

        assertThat(actualAssetProfileCacheInfo.getId()).isEqualTo(expectedAssetProfile.getUuidId());
        assertThat(actualAssetProfileCacheInfo.tenantId()).isEqualTo(expectedAssetProfile.getTenantId());
        assertThat(actualAssetProfileCacheInfo.name()).isEqualTo(expectedAssetProfile.getName());
        assertThat(actualAssetProfileCacheInfo.defaultRuleChainId()).isEqualTo(expectedAssetProfile.getDefaultRuleChainId());
        assertThat(actualAssetProfileCacheInfo.defaultQueueName()).isEqualTo(expectedAssetProfile.getDefaultQueueName());
    }

    @Test
    void protoCacheInfoSerialization_withNullOptionalFields() {
        Device device = easyRandom.nextObject(Device.class);
        device.setCustomerId(null);
        TransportProtos.DeviceCacheInfoProto deviceProto = ProtoUtils.toCacheProto(device);
        DeviceCacheInfo deviceCacheInfo = ProtoUtils.fromCacheProto(deviceProto);
        assertThat(deviceCacheInfo.customerId()).isNull();
        assertThat(deviceCacheInfo.name()).isEqualTo(device.getName());

        Asset asset = easyRandom.nextObject(Asset.class);
        asset.setCustomerId(null);
        TransportProtos.AssetCacheInfoProto assetProto = ProtoUtils.toCacheProto(asset);
        AssetCacheInfo assetCacheInfo = ProtoUtils.fromCacheProto(assetProto);
        assertThat(assetCacheInfo.customerId()).isNull();
        assertThat(assetCacheInfo.name()).isEqualTo(asset.getName());

        DeviceProfile deviceProfile = easyRandom.nextObject(DeviceProfile.class);
        deviceProfile.setDefaultRuleChainId(null);
        deviceProfile.setDefaultQueueName(null);
        TransportProtos.DeviceProfileCacheInfoProto dpProto = ProtoUtils.toCacheProto(deviceProfile);
        DeviceProfileCacheInfo dpCacheInfo = ProtoUtils.fromCacheProto(dpProto);
        assertThat(dpCacheInfo.defaultRuleChainId()).isNull();
        assertThat(dpCacheInfo.defaultQueueName()).isNull();
        assertThat(dpCacheInfo.name()).isEqualTo(deviceProfile.getName());

        AssetProfile assetProfile = easyRandom.nextObject(AssetProfile.class);
        assetProfile.setDefaultRuleChainId(null);
        assetProfile.setDefaultQueueName(null);
        TransportProtos.AssetProfileCacheInfoProto apProto = ProtoUtils.toCacheProto(assetProfile);
        org.thingsboard.server.common.data.AssetProfileCacheInfo apCacheInfo = ProtoUtils.fromCacheProto(apProto);
        assertThat(apCacheInfo.defaultRuleChainId()).isNull();
        assertThat(apCacheInfo.defaultQueueName()).isNull();
        assertThat(apCacheInfo.name()).isEqualTo(assetProfile.getName());
    }

    @Test
    void protoCacheInfoRoundTrip_viaCacheInfoOverload() {
        UUID deviceId = UUID.randomUUID();
        TenantId tid = TenantId.fromUUID(UUID.randomUUID());
        CustomerId custId = new CustomerId(UUID.randomUUID());
        DeviceProfileId dpId = new DeviceProfileId(UUID.randomUUID());
        DeviceCacheInfo originalDevice = new DeviceCacheInfo(deviceId, tid, custId, "dev1", "type1", dpId);

        TransportProtos.DeviceCacheInfoProto deviceProto = ProtoUtils.toCacheProto(originalDevice);
        DeviceCacheInfo restored = ProtoUtils.fromCacheProto(deviceProto);
        assertThat(restored.getId()).isEqualTo(originalDevice.getId());
        assertThat(restored.name()).isEqualTo(originalDevice.name());
        assertThat(restored.type()).isEqualTo(originalDevice.type());
        assertThat(restored.tenantId()).isEqualTo(originalDevice.tenantId());
        assertThat(restored.customerId()).isEqualTo(originalDevice.customerId());
        assertThat(restored.deviceProfileId()).isEqualTo(originalDevice.deviceProfileId());

        UUID assetId = UUID.randomUUID();
        org.thingsboard.server.common.data.id.AssetProfileId apId = new org.thingsboard.server.common.data.id.AssetProfileId(UUID.randomUUID());
        AssetCacheInfo originalAsset = new AssetCacheInfo(assetId, tid, custId, "asset1", "assetType", apId);
        TransportProtos.AssetCacheInfoProto assetProto = ProtoUtils.toCacheProto(originalAsset);
        AssetCacheInfo restoredAsset = ProtoUtils.fromCacheProto(assetProto);
        assertThat(restoredAsset.getId()).isEqualTo(originalAsset.getId());
        assertThat(restoredAsset.name()).isEqualTo(originalAsset.name());
        assertThat(restoredAsset.type()).isEqualTo(originalAsset.type());
        assertThat(restoredAsset.tenantId()).isEqualTo(originalAsset.tenantId());
        assertThat(restoredAsset.customerId()).isEqualTo(originalAsset.customerId());
        assertThat(restoredAsset.assetProfileId()).isEqualTo(originalAsset.assetProfileId());

        RuleChainId rcId = new RuleChainId(UUID.randomUUID());
        DeviceProfileCacheInfo originalDp = new DeviceProfileCacheInfo(UUID.randomUUID(), tid, "dpName", rcId, "myQueue");
        TransportProtos.DeviceProfileCacheInfoProto dpProto = ProtoUtils.toCacheProto(originalDp);
        DeviceProfileCacheInfo restoredDp = ProtoUtils.fromCacheProto(dpProto);
        assertThat(restoredDp.getId()).isEqualTo(originalDp.getId());
        assertThat(restoredDp.name()).isEqualTo(originalDp.name());
        assertThat(restoredDp.tenantId()).isEqualTo(originalDp.tenantId());
        assertThat(restoredDp.defaultRuleChainId()).isEqualTo(originalDp.defaultRuleChainId());
        assertThat(restoredDp.defaultQueueName()).isEqualTo(originalDp.defaultQueueName());

        AssetProfileCacheInfo originalAp =
                new AssetProfileCacheInfo(UUID.randomUUID(), tid, "apName", rcId, "apQueue");
        TransportProtos.AssetProfileCacheInfoProto apProto = ProtoUtils.toCacheProto(originalAp);
        AssetProfileCacheInfo restoredAp = ProtoUtils.fromCacheProto(apProto);
        assertThat(restoredAp.getId()).isEqualTo(originalAp.getId());
        assertThat(restoredAp.name()).isEqualTo(originalAp.name());
        assertThat(restoredAp.tenantId()).isEqualTo(originalAp.tenantId());
        assertThat(restoredAp.defaultRuleChainId()).isEqualTo(originalAp.defaultRuleChainId());
        assertThat(restoredAp.defaultQueueName()).isEqualTo(originalAp.defaultQueueName());
    }

    @Test
    void testToSessionInfoWithGateway() {
        UUID sessionId = UUID.randomUUID();
        Device device = easyRandom.nextObject(Device.class);
        DeviceId gatewayId = new DeviceId(UUID.randomUUID());
        boolean isGateway = true;

        TransportProtos.SessionInfoProto sessionInfo = ProtoUtils.toSessionInfo(sessionId, null, device, gatewayId, isGateway);

        assertThat(sessionInfo.getSessionIdMSB()).isEqualTo(sessionId.getMostSignificantBits());
        assertThat(sessionInfo.getSessionIdLSB()).isEqualTo(sessionId.getLeastSignificantBits());
        assertThat(sessionInfo.getTenantIdMSB()).isEqualTo(device.getTenantId().getId().getMostSignificantBits());
        assertThat(sessionInfo.getTenantIdLSB()).isEqualTo(device.getTenantId().getId().getLeastSignificantBits());
        assertThat(sessionInfo.getDeviceIdMSB()).isEqualTo(device.getId().getId().getMostSignificantBits());
        assertThat(sessionInfo.getDeviceIdLSB()).isEqualTo(device.getId().getId().getLeastSignificantBits());
        assertThat(sessionInfo.getDeviceName()).isEqualTo(device.getName());
        assertThat(sessionInfo.getDeviceType()).isEqualTo(device.getType());
        assertThat(sessionInfo.getDeviceProfileIdMSB()).isEqualTo(device.getDeviceProfileId().getId().getMostSignificantBits());
        assertThat(sessionInfo.getDeviceProfileIdLSB()).isEqualTo(device.getDeviceProfileId().getId().getLeastSignificantBits());
        assertThat(sessionInfo.getNodeId()).isEmpty();
        assertThat(sessionInfo.getIsGateway()).isEqualTo(isGateway);
        assertThat(sessionInfo.getGatewayIdMSB()).isEqualTo(gatewayId.getId().getMostSignificantBits());
        assertThat(sessionInfo.getGatewayIdLSB()).isEqualTo(gatewayId.getId().getLeastSignificantBits());

        if (device.getCustomerId() != null && !device.getCustomerId().isNullUid()) {
            assertThat(sessionInfo.getCustomerIdMSB()).isEqualTo(device.getCustomerId().getId().getMostSignificantBits());
            assertThat(sessionInfo.getCustomerIdLSB()).isEqualTo(device.getCustomerId().getId().getLeastSignificantBits());
        }
    }

}
