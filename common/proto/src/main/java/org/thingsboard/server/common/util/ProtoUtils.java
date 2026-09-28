// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.AssetCacheInfo;
import org.thingsboard.server.common.data.AssetProfileCacheInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceCacheInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileCacheInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ResourceSubType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.integration.AbstractIntegration;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.sync.vc.RepositoryAuthMethod;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.edge.EdgeHighPriorityMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceDeleteMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.gen.integration.ConverterProto;
import org.thingsboard.server.gen.integration.IntegrationInfoProto;
import org.thingsboard.server.gen.integration.IntegrationProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ApiUsageRecordKeyProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.GATEWAY_PARAMETER;

@Slf4j
public class ProtoUtils {

    public static TransportProtos.ComponentLifecycleMsgProto toProto(ComponentLifecycleMsg msg) {
        var builder = TransportProtos.ComponentLifecycleMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setEntityType(toProto(msg.getEntityId().getEntityType()))
                .setEntityIdMSB(msg.getEntityId().getId().getMostSignificantBits())
                .setEntityIdLSB(msg.getEntityId().getId().getLeastSignificantBits())
                .setEvent(toProto(msg.getEvent()));
        if (msg.getProfileId() != null) {
            builder.setProfileIdMSB(msg.getProfileId().getId().getMostSignificantBits());
            builder.setProfileIdLSB(msg.getProfileId().getId().getLeastSignificantBits());
        }
        if (msg.getOldProfileId() != null) {
            builder.setOldProfileIdMSB(msg.getOldProfileId().getId().getMostSignificantBits());
            builder.setOldProfileIdLSB(msg.getOldProfileId().getId().getLeastSignificantBits());
        }
        builder.setOwnerChanged(msg.isOwnerChanged());
        if (msg.getName() != null) {
            builder.setName(msg.getName());
        }
        if (msg.getOldName() != null) {
            builder.setOldName(msg.getOldName());
        }
        if (msg.getInfo() != null) {
            builder.setInfo(JacksonUtil.toString(msg.getInfo()));
        }
        return builder.build();
    }

    public static TransportProtos.EntityTypeProto toProto(EntityType entityType) {
        return TransportProtos.EntityTypeProto.forNumber(entityType.getProtoNumber());
    }

    public static ComponentLifecycleMsg fromProto(TransportProtos.ComponentLifecycleMsgProto proto) {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(fromProto(proto.getEntityType()), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
        var builder = ComponentLifecycleMsg.builder()
                .tenantId(TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())))
                .entityId(entityId)
                .event(fromProto(proto.getEvent()));
        if (!StringUtils.isEmpty(proto.getName())) {
            builder.name(proto.getName());
        }
        if (!StringUtils.isEmpty(proto.getOldName())) {
            builder.oldName(proto.getOldName());
        }
        if (proto.getProfileIdMSB() != 0 || proto.getProfileIdLSB() != 0) {
            var profileType = EntityType.DEVICE.equals(entityId.getEntityType()) ? EntityType.DEVICE_PROFILE : EntityType.ASSET_PROFILE;
            builder.profileId(EntityIdFactory.getByTypeAndUuid(profileType, new UUID(proto.getProfileIdMSB(), proto.getProfileIdLSB())));
        }
        if (proto.getOldProfileIdMSB() != 0 || proto.getOldProfileIdLSB() != 0) {
            var profileType = EntityType.DEVICE.equals(entityId.getEntityType()) ? EntityType.DEVICE_PROFILE : EntityType.ASSET_PROFILE;
            builder.oldProfileId(EntityIdFactory.getByTypeAndUuid(profileType, new UUID(proto.getOldProfileIdMSB(), proto.getOldProfileIdLSB())));
        }
        builder.ownerChanged(proto.getOwnerChanged());
        if (proto.hasInfo()) {
            builder.info(JacksonUtil.toJsonNode(proto.getInfo()));
        }
        return builder.build();
    }

    public static EntityType fromProto(TransportProtos.EntityTypeProto entityType) {
        return EntityType.forProtoNumber(entityType.getNumber());
    }

    public static TransportProtos.ComponentLifecycleEvent toProto(ComponentLifecycleEvent event) {
        return TransportProtos.ComponentLifecycleEvent.forNumber(event.getProtoNumber());
    }

    public static ComponentLifecycleEvent fromProto(TransportProtos.ComponentLifecycleEvent eventProto) {
        return ComponentLifecycleEvent.forProtoNumber(eventProto.getNumber());
    }

    public static TransportProtos.ToEdgeSyncRequestMsgProto toProto(ToEdgeSyncRequest request) {
        return TransportProtos.ToEdgeSyncRequestMsgProto.newBuilder()
                .setTenantIdMSB(request.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(request.getTenantId().getId().getLeastSignificantBits())
                .setRequestIdMSB(request.getId().getMostSignificantBits())
                .setRequestIdLSB(request.getId().getLeastSignificantBits())
                .setEdgeIdMSB(request.getEdgeId().getId().getMostSignificantBits())
                .setEdgeIdLSB(request.getEdgeId().getId().getLeastSignificantBits())
                .setServiceId(request.getServiceId())
                .build();
    }

    public static ToEdgeSyncRequest fromProto(TransportProtos.ToEdgeSyncRequestMsgProto proto) {
        return new ToEdgeSyncRequest(
                new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB()),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                EdgeId.fromUUID(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB())),
                proto.getServiceId()
        );
    }

    public static TransportProtos.FromEdgeSyncResponseMsgProto toProto(FromEdgeSyncResponse response) {
        return TransportProtos.FromEdgeSyncResponseMsgProto.newBuilder()
                .setTenantIdMSB(response.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(response.getTenantId().getId().getLeastSignificantBits())
                .setResponseIdMSB(response.getId().getMostSignificantBits())
                .setResponseIdLSB(response.getId().getLeastSignificantBits())
                .setEdgeIdMSB(response.getEdgeId().getId().getMostSignificantBits())
                .setEdgeIdLSB(response.getEdgeId().getId().getLeastSignificantBits())
                .setSuccess(response.isSuccess())
                .setError(response.getError())
                .build();
    }

    public static FromEdgeSyncResponse fromProto(TransportProtos.FromEdgeSyncResponseMsgProto proto) {
        return new FromEdgeSyncResponse(
                new UUID(proto.getResponseIdMSB(), proto.getResponseIdLSB()),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                EdgeId.fromUUID(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB())),
                proto.getSuccess(),
                proto.getError()
        );
    }

    public static TransportProtos.EdgeEventMsgProto toProto(EdgeEvent edgeEvent) {
        TransportProtos.EdgeEventMsgProto.Builder builder = TransportProtos.EdgeEventMsgProto.newBuilder();

        builder.setTenantIdMSB(edgeEvent.getTenantId().getId().getMostSignificantBits());
        builder.setTenantIdLSB(edgeEvent.getTenantId().getId().getLeastSignificantBits());
        builder.setEntityType(edgeEvent.getType().name());
        builder.setAction(edgeEvent.getAction().name());

        if (edgeEvent.getEdgeId() != null) {
            builder.setEdgeIdMSB(edgeEvent.getEdgeId().getId().getMostSignificantBits());
            builder.setEdgeIdLSB(edgeEvent.getEdgeId().getId().getLeastSignificantBits());
        }
        if (edgeEvent.getEntityId() != null) {
            builder.setEntityIdMSB(edgeEvent.getEntityId().getMostSignificantBits());
            builder.setEntityIdLSB(edgeEvent.getEntityId().getLeastSignificantBits());
        }
        if (edgeEvent.getBody() != null) {
            builder.setBody(JacksonUtil.toString(edgeEvent.getBody()));
        }
        if (edgeEvent.getEntityGroupId() != null) {
            builder.setEntityGroupIdMSB(edgeEvent.getEntityGroupId().getMostSignificantBits());
            builder.setEntityGroupIdLSB(edgeEvent.getEntityGroupId().getLeastSignificantBits());
        }

        return builder.build();
    }

    public static EdgeEvent fromProto(TransportProtos.EdgeEventMsgProto proto) {
        EdgeEvent edgeEvent = new EdgeEvent();
        TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setType(EdgeEventType.valueOf(proto.getEntityType()));
        edgeEvent.setAction(EdgeEventActionType.valueOf(proto.getAction()));

        if (proto.hasEdgeIdMSB() && proto.hasEdgeIdLSB()) {
            edgeEvent.setEdgeId(new EdgeId(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB())));
        }
        if (proto.hasEntityIdMSB() && proto.hasEntityIdLSB()) {
            edgeEvent.setEntityId(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
        }
        if (proto.hasBody()) {
            edgeEvent.setBody(JacksonUtil.toJsonNode(proto.getBody()));
        }

        if (proto.hasEntityGroupIdMSB() && proto.hasEntityGroupIdLSB()) {
            edgeEvent.setEntityGroupId(new UUID(proto.getEntityGroupIdMSB(), proto.getEntityGroupIdLSB()));
        }

        return edgeEvent;
    }

    public static TransportProtos.EdgeHighPriorityMsgProto toProto(EdgeHighPriorityMsg msg) {
        TransportProtos.EdgeHighPriorityMsgProto.Builder builder = TransportProtos.EdgeHighPriorityMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setType(msg.getEdgeEvent().getType().name())
                .setAction(msg.getEdgeEvent().getAction().name());

        if (msg.getEdgeEvent().getEntityId() != null) {
            builder.setEntityIdMSB(msg.getEdgeEvent().getEntityId().getMostSignificantBits());
            builder.setEntityIdLSB(msg.getEdgeEvent().getEntityId().getLeastSignificantBits());
        }
        if (msg.getEdgeEvent().getEdgeId() != null) {
            builder.setEdgeIdMSB(msg.getEdgeEvent().getEdgeId().getId().getMostSignificantBits());
            builder.setEdgeIdLSB(msg.getEdgeEvent().getEdgeId().getId().getLeastSignificantBits());
        }
        if (msg.getEdgeEvent().getBody() != null) {
            builder.setBody(JacksonUtil.toString(msg.getEdgeEvent().getBody()));
        }

        return builder.build();
    }

    public static EdgeHighPriorityMsg fromProto(TransportProtos.EdgeHighPriorityMsgProto proto) {
        EdgeEventType type = EdgeEventType.valueOf(proto.getType());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(proto.getAction());
        JsonNode body = proto.hasBody() ? JacksonUtil.toJsonNode(proto.getBody()) : null;

        EdgeId edgeId = null;
        if (proto.hasEdgeIdMSB() && proto.hasEdgeIdLSB()) {
            edgeId = EdgeId.fromUUID(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB()));
        }

        EntityId entityId = null;
        if (proto.hasEntityIdMSB() && proto.hasEntityIdLSB()) {
            entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type, new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
        }

        return new EdgeHighPriorityMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                EdgeUtils.constructEdgeEvent(TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                        edgeId, type, actionType, entityId, body)
        );
    }

    public static TransportProtos.EdgeEventUpdateMsgProto toProto(EdgeEventUpdateMsg msg) {
        return TransportProtos.EdgeEventUpdateMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setEdgeIdMSB(msg.getEdgeId().getId().getMostSignificantBits())
                .setEdgeIdLSB(msg.getEdgeId().getId().getLeastSignificantBits())
                .build();
    }

    public static EdgeEventUpdateMsg fromProto(TransportProtos.EdgeEventUpdateMsgProto proto) {
        return new EdgeEventUpdateMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                EdgeId.fromUUID(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB()))
        );
    }

    private static TransportProtos.DeviceEdgeUpdateMsgProto toProto(DeviceEdgeUpdateMsg msg) {
        TransportProtos.DeviceEdgeUpdateMsgProto.Builder builder = TransportProtos.DeviceEdgeUpdateMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits());

        if (msg.getEdgeId() != null) {
            builder.setEdgeIdMSB(msg.getEdgeId().getId().getMostSignificantBits())
                    .setEdgeIdLSB(msg.getEdgeId().getId().getLeastSignificantBits());
        }

        return builder.build();
    }

    private static DeviceEdgeUpdateMsg fromProto(TransportProtos.DeviceEdgeUpdateMsgProto proto) {
        EdgeId edgeId = null;
        if (proto.hasEdgeIdMSB() && proto.hasEdgeIdLSB()) {
            edgeId = EdgeId.fromUUID(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB()));
        }
        return new DeviceEdgeUpdateMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                edgeId);
    }

    private static TransportProtos.DeviceNameOrTypeUpdateMsgProto toProto(DeviceNameOrTypeUpdateMsg msg) {
        return TransportProtos.DeviceNameOrTypeUpdateMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setDeviceName(msg.getDeviceName())
                .setDeviceType(msg.getDeviceType())
                .build();
    }

    private static DeviceNameOrTypeUpdateMsg fromProto(TransportProtos.DeviceNameOrTypeUpdateMsgProto proto) {
        return new DeviceNameOrTypeUpdateMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                proto.getDeviceName(),
                proto.getDeviceType()
        );
    }

    private static TransportProtos.DeviceAttributesEventMsgProto toProto(DeviceAttributesEventNotificationMsg msg) {
        TransportProtos.DeviceAttributesEventMsgProto.Builder builder = TransportProtos.DeviceAttributesEventMsgProto.newBuilder();
        builder.setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setDeleted(msg.isDeleted());

        if (msg.getScope() != null) {
            builder.setScope(TransportProtos.AttributeScopeProto.valueOf(msg.getScope()));
        }

        if (msg.getDeletedKeys() != null) {
            for (AttributeKey key : msg.getDeletedKeys()) {
                builder.addDeletedKeys(TransportProtos.AttributeKey.newBuilder()
                        .setScope(TransportProtos.AttributeScopeProto.valueOf(key.getScope()))
                        .setAttributeKey(key.getAttributeKey())
                        .build());
            }
        }

        if (msg.getValues() != null) {
            for (AttributeKvEntry attributeKvEntry : msg.getValues()) {
                builder.addValues(toProto(attributeKvEntry));
            }
        }
        return builder.build();
    }

    public static TransportProtos.AttributeValueProto toProto(AttributeKvEntry attributeKvEntry) {
        TransportProtos.AttributeValueProto.Builder builder = TransportProtos.AttributeValueProto.newBuilder()
                .setLastUpdateTs(attributeKvEntry.getLastUpdateTs())
                .setKey(attributeKvEntry.getKey());
        switch (attributeKvEntry.getDataType()) {
            case BOOLEAN:
                attributeKvEntry.getBooleanValue().ifPresent(builder::setBoolV);
                builder.setHasV(attributeKvEntry.getBooleanValue().isPresent());
                builder.setType(TransportProtos.KeyValueType.BOOLEAN_V);
                break;
            case STRING:
                attributeKvEntry.getStrValue().ifPresent(builder::setStringV);
                builder.setHasV(attributeKvEntry.getStrValue().isPresent());
                builder.setType(TransportProtos.KeyValueType.STRING_V);
                break;
            case DOUBLE:
                attributeKvEntry.getDoubleValue().ifPresent(builder::setDoubleV);
                builder.setHasV(attributeKvEntry.getDoubleValue().isPresent());
                builder.setType(TransportProtos.KeyValueType.DOUBLE_V);
                break;
            case LONG:
                attributeKvEntry.getLongValue().ifPresent(builder::setLongV);
                builder.setHasV(attributeKvEntry.getLongValue().isPresent());
                builder.setType(TransportProtos.KeyValueType.LONG_V);
                break;
            case JSON:
                attributeKvEntry.getJsonValue().ifPresent(builder::setJsonV);
                builder.setHasV(attributeKvEntry.getJsonValue().isPresent());
                builder.setType(TransportProtos.KeyValueType.JSON_V);
                break;
        }

        if (attributeKvEntry.getVersion() != null) {
            builder.setVersion(attributeKvEntry.getVersion());
        }

        return builder.build();
    }

    public static ApiUsageRecordKeyProto toProto(ApiUsageRecordKey apiUsageRecordKey) {
        return switch (apiUsageRecordKey) {
            case TRANSPORT_MSG_COUNT -> ApiUsageRecordKeyProto.TRANSPORT_MSG_COUNT;
            case TRANSPORT_DP_COUNT -> ApiUsageRecordKeyProto.TRANSPORT_DP_COUNT;
            case STORAGE_DP_COUNT -> ApiUsageRecordKeyProto.STORAGE_DP_COUNT;
            case RE_EXEC_COUNT -> ApiUsageRecordKeyProto.RE_EXEC_COUNT;
            case JS_EXEC_COUNT -> ApiUsageRecordKeyProto.JS_EXEC_COUNT;
            case TBEL_EXEC_COUNT -> ApiUsageRecordKeyProto.TBEL_EXEC_COUNT;
            case EMAIL_EXEC_COUNT -> ApiUsageRecordKeyProto.EMAIL_EXEC_COUNT;
            case SMS_EXEC_COUNT -> ApiUsageRecordKeyProto.SMS_EXEC_COUNT;
            case CREATED_ALARMS_COUNT -> ApiUsageRecordKeyProto.CREATED_ALARMS_COUNT;
            case ACTIVE_DEVICES -> ApiUsageRecordKeyProto.ACTIVE_DEVICES;
            case INACTIVE_DEVICES -> ApiUsageRecordKeyProto.INACTIVE_DEVICES;
            case GENERATED_REPORTS_COUNT -> ApiUsageRecordKeyProto.GENERATED_REPORTS_COUNT;
            case AI_CREDITS_COUNT -> ApiUsageRecordKeyProto.AI_CREDITS_COUNT;
        };
    }

    public static ApiUsageRecordKey fromProto(ApiUsageRecordKeyProto proto) {
        return switch (proto) {
            case UNRECOGNIZED -> null;
            case TRANSPORT_MSG_COUNT -> ApiUsageRecordKey.TRANSPORT_MSG_COUNT;
            case TRANSPORT_DP_COUNT -> ApiUsageRecordKey.TRANSPORT_DP_COUNT;
            case STORAGE_DP_COUNT -> ApiUsageRecordKey.STORAGE_DP_COUNT;
            case RE_EXEC_COUNT -> ApiUsageRecordKey.RE_EXEC_COUNT;
            case JS_EXEC_COUNT -> ApiUsageRecordKey.JS_EXEC_COUNT;
            case TBEL_EXEC_COUNT -> ApiUsageRecordKey.TBEL_EXEC_COUNT;
            case EMAIL_EXEC_COUNT -> ApiUsageRecordKey.EMAIL_EXEC_COUNT;
            case SMS_EXEC_COUNT -> ApiUsageRecordKey.SMS_EXEC_COUNT;
            case CREATED_ALARMS_COUNT -> ApiUsageRecordKey.CREATED_ALARMS_COUNT;
            case ACTIVE_DEVICES -> ApiUsageRecordKey.ACTIVE_DEVICES;
            case INACTIVE_DEVICES -> ApiUsageRecordKey.INACTIVE_DEVICES;
            case GENERATED_REPORTS_COUNT -> ApiUsageRecordKey.GENERATED_REPORTS_COUNT;
            case AI_CREDITS_COUNT -> ApiUsageRecordKey.AI_CREDITS_COUNT;
        };
    }

    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.DeviceAttributesEventMsgProto proto) {
        return new DeviceAttributesEventNotificationMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                getAttributeKeySetFromProto(proto.getDeletedKeysList()),
                proto.hasScope() ? proto.getScope().name() : null,
                getAttributesKvEntryFromProto(proto.getValuesList()),
                proto.getDeleted()
        );
    }

    private static TransportProtos.DeviceCredentialsUpdateMsgProto toProto(DeviceCredentialsUpdateNotificationMsg msg) {
        TransportProtos.DeviceCredentialsProto.Builder protoBuilder = TransportProtos.DeviceCredentialsProto.newBuilder()
                .setDeviceIdMSB(msg.getDeviceCredentials().getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceCredentials().getDeviceId().getId().getLeastSignificantBits())
                .setCredentialsId(msg.getDeviceCredentials().getCredentialsId())
                .setCredentialsType(TransportProtos.CredentialsType.valueOf(msg.getDeviceCredentials().getCredentialsType().name()));

        if (msg.getDeviceCredentials().getCredentialsValue() != null) {
            protoBuilder.setCredentialsValue(msg.getDeviceCredentials().getCredentialsValue());
        }

        return TransportProtos.DeviceCredentialsUpdateMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setDeviceCredentials(protoBuilder.build())
                .build();
    }

    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.DeviceCredentialsUpdateMsgProto proto) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(new DeviceId(new UUID(proto.getDeviceCredentials().getDeviceIdMSB(), proto.getDeviceCredentials().getDeviceIdLSB())));
        deviceCredentials.setCredentialsId(proto.getDeviceCredentials().getCredentialsId());
        deviceCredentials.setCredentialsValue(proto.getDeviceCredentials().hasCredentialsValue() ? proto.getDeviceCredentials().getCredentialsValue() : null);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(proto.getDeviceCredentials().getCredentialsType().name()));
        return new DeviceCredentialsUpdateNotificationMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                deviceCredentials
        );
    }

    private static TransportProtos.ToDeviceRpcRequestActorMsgProto toProto(ToDeviceRpcRequestActorMsg msg) {
        TransportProtos.ToDeviceRpcRequestMsg.Builder builder = TransportProtos.ToDeviceRpcRequestMsg.newBuilder()
                .setMethodName(msg.getMsg().getBody().getMethod())
                .setParams(msg.getMsg().getBody().getParams())
                .setExpirationTime(msg.getMsg().getExpirationTime())
                .setRequestIdMSB(msg.getMsg().getId().getMostSignificantBits())
                .setRequestIdLSB(msg.getMsg().getId().getLeastSignificantBits())
                .setOneway(msg.getMsg().isOneway())
                .setPersisted(msg.getMsg().isPersisted());
        if (msg.getMsg().getAdditionalInfo() != null) {
            builder.setAdditionalInfo(msg.getMsg().getAdditionalInfo());
        }
        if (msg.getMsg().getRetries() != null) {
            builder.setRetries(msg.getMsg().getRetries());
        }
        TransportProtos.ToDeviceRpcRequestMsg proto = builder.build();

        return TransportProtos.ToDeviceRpcRequestActorMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setServiceId(msg.getServiceId())
                .setToDeviceRpcRequestMsg(proto)
                .build();
    }

    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.ToDeviceRpcRequestActorMsgProto proto) {
        TransportProtos.ToDeviceRpcRequestMsg toDeviceRpcRequestMsg = proto.getToDeviceRpcRequestMsg();
        ToDeviceRpcRequest toDeviceRpcRequest = new ToDeviceRpcRequest(
                new UUID(toDeviceRpcRequestMsg.getRequestIdMSB(), toDeviceRpcRequestMsg.getRequestIdLSB()),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                toDeviceRpcRequestMsg.getOneway(),
                toDeviceRpcRequestMsg.getExpirationTime(),
                new ToDeviceRpcRequestBody(toDeviceRpcRequestMsg.getMethodName(), toDeviceRpcRequestMsg.getParams()),
                toDeviceRpcRequestMsg.getPersisted(),
                toDeviceRpcRequestMsg.hasRetries() ? toDeviceRpcRequestMsg.getRetries() : null,
                toDeviceRpcRequestMsg.hasAdditionalInfo() ? toDeviceRpcRequestMsg.getAdditionalInfo() : null);
        return new ToDeviceRpcRequestActorMsg(proto.getServiceId(), toDeviceRpcRequest);
    }

    private static TransportProtos.FromDeviceRpcResponseActorMsgProto toProto(FromDeviceRpcResponseActorMsg msg) {
        TransportProtos.FromDeviceRPCResponseProto.Builder builder = TransportProtos.FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(msg.getMsg().getId().getMostSignificantBits())
                .setRequestIdLSB(msg.getMsg().getId().getLeastSignificantBits())
                .setError(msg.getMsg().getError().isPresent() ? msg.getMsg().getError().get().ordinal() : -1);
        if (msg.getMsg().getResponse().isPresent()) {
            builder.setResponse(msg.getMsg().getResponse().get());
        }

        return TransportProtos.FromDeviceRpcResponseActorMsgProto.newBuilder()
                .setRequestId(msg.getRequestId())
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setRpcResponse(builder.build())
                .build();
    }

    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.FromDeviceRpcResponseActorMsgProto proto) {
        TransportProtos.FromDeviceRPCResponseProto rpcResponse = proto.getRpcResponse();
        FromDeviceRpcResponse fromDeviceRpcResponse = new FromDeviceRpcResponse(
                new UUID(rpcResponse.getRequestIdMSB(), rpcResponse.getRequestIdLSB()),
                rpcResponse.hasResponse() ? rpcResponse.getResponse() : null,
                RpcError.fromProtoErrorCode(rpcResponse.getError()));
        return new FromDeviceRpcResponseActorMsg(
                proto.getRequestId(),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                fromDeviceRpcResponse
        );
    }

    private static TransportProtos.RemoveRpcActorMsgProto toProto(RemoveRpcActorMsg msg) {
        return TransportProtos.RemoveRpcActorMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .setRequestIdMSB(msg.getRequestId().getMostSignificantBits())
                .setRequestIdLSB(msg.getRequestId().getLeastSignificantBits())
                .build();
    }

    private static ToDeviceActorNotificationMsg fromProto(TransportProtos.RemoveRpcActorMsgProto proto) {
        return new RemoveRpcActorMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())),
                new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
        );
    }

    private static TransportProtos.DeviceDeleteMsgProto toProto(DeviceDeleteMsg msg) {
        return TransportProtos.DeviceDeleteMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceId().getId().getLeastSignificantBits())
                .build();
    }

    private static DeviceDeleteMsg fromProto(TransportProtos.DeviceDeleteMsgProto proto) {
        return new DeviceDeleteMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB())));
    }

    public static TransportProtos.ToDeviceActorNotificationMsgProto toProto(ToDeviceActorNotificationMsg msg) {
        if (msg instanceof DeviceEdgeUpdateMsg updateMsg) {
            TransportProtos.DeviceEdgeUpdateMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceEdgeUpdateMsg(proto).build();
        } else if (msg instanceof DeviceNameOrTypeUpdateMsg updateMsg) {
            TransportProtos.DeviceNameOrTypeUpdateMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceNameOrTypeMsg(proto).build();
        } else if (msg instanceof DeviceAttributesEventNotificationMsg updateMsg) {
            TransportProtos.DeviceAttributesEventMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceAttributesEventMsg(proto).build();
        } else if (msg instanceof DeviceCredentialsUpdateNotificationMsg updateMsg) {
            TransportProtos.DeviceCredentialsUpdateMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceCredentialsUpdateMsg(proto).build();
        } else if (msg instanceof ToDeviceRpcRequestActorMsg updateMsg) {
            TransportProtos.ToDeviceRpcRequestActorMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setToDeviceRpcRequestMsg(proto).build();
        } else if (msg instanceof FromDeviceRpcResponseActorMsg updateMsg) {
            TransportProtos.FromDeviceRpcResponseActorMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setFromDeviceRpcResponseMsg(proto).build();
        } else if (msg instanceof RemoveRpcActorMsg updateMsg) {
            TransportProtos.RemoveRpcActorMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setRemoveRpcActorMsg(proto).build();
        } else if (msg instanceof DeviceDeleteMsg updateMsg) {
            TransportProtos.DeviceDeleteMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceDeleteMsg(proto).build();
        }
        return null;
    }

    public static ToDeviceActorNotificationMsg fromProto(TransportProtos.ToDeviceActorNotificationMsgProto proto) {
        if (proto.hasDeviceEdgeUpdateMsg()) {
            return fromProto(proto.getDeviceEdgeUpdateMsg());
        } else if (proto.hasDeviceNameOrTypeMsg()) {
            return fromProto(proto.getDeviceNameOrTypeMsg());
        } else if (proto.hasDeviceAttributesEventMsg()) {
            return fromProto(proto.getDeviceAttributesEventMsg());
        } else if (proto.hasDeviceCredentialsUpdateMsg()) {
            return fromProto(proto.getDeviceCredentialsUpdateMsg());
        } else if (proto.hasToDeviceRpcRequestMsg()) {
            return fromProto(proto.getToDeviceRpcRequestMsg());
        } else if (proto.hasFromDeviceRpcResponseMsg()) {
            return fromProto(proto.getFromDeviceRpcResponseMsg());
        } else if (proto.hasRemoveRpcActorMsg()) {
            return fromProto(proto.getRemoveRpcActorMsg());
        } else if (proto.hasDeviceDeleteMsg()) {
            return fromProto(proto.getDeviceDeleteMsg());
        }
        return null;
    }

    private static Set<AttributeKey> getAttributeKeySetFromProto(List<TransportProtos.AttributeKey> deletedKeysList) {
        if (deletedKeysList.isEmpty()) {
            return null;
        }
        return deletedKeysList.stream()
                .map(attributeKey -> new AttributeKey(attributeKey.getScope().name(), attributeKey.getAttributeKey()))
                .collect(Collectors.toSet());
    }

    private static List<AttributeKvEntry> getAttributesKvEntryFromProto(List<TransportProtos.AttributeValueProto> valuesList) {
        if (valuesList.isEmpty()) {
            return null;
        }
        List<AttributeKvEntry> result = new ArrayList<>();
        for (TransportProtos.AttributeValueProto kvEntry : valuesList) {
            result.add(fromProto(kvEntry));
        }
        return result;
    }

    public static AttributeKvEntry fromProto(TransportProtos.AttributeValueProto proto) {
        boolean hasValue = proto.getHasV();
        String key = proto.getKey();
        KvEntry entry = switch (proto.getType()) {
            case BOOLEAN_V -> new BooleanDataEntry(key, hasValue ? proto.getBoolV() : null);
            case LONG_V -> new LongDataEntry(key, hasValue ? proto.getLongV() : null);
            case DOUBLE_V -> new DoubleDataEntry(key, hasValue ? proto.getDoubleV() : null);
            case STRING_V -> new StringDataEntry(key, hasValue ? proto.getStringV() : null);
            case JSON_V -> new JsonDataEntry(key, hasValue ? proto.getJsonV() : null);
            default -> null;
        };
        return new BaseAttributeKvEntry(entry, proto.getLastUpdateTs(), proto.hasVersion() ? proto.getVersion() : null);
    }

    public static BasicKvEntry basicKvEntryFromProto(TransportProtos.AttributeValueProto proto) {
        boolean hasValue = proto.getHasV();
        String key = proto.getKey();
        return switch (proto.getType()) {
            case BOOLEAN_V -> new BooleanDataEntry(key, hasValue ? proto.getBoolV() : null);
            case LONG_V -> new LongDataEntry(key, hasValue ? proto.getLongV() : null);
            case DOUBLE_V -> new DoubleDataEntry(key, hasValue ? proto.getDoubleV() : null);
            case STRING_V -> new StringDataEntry(key, hasValue ? proto.getStringV() : null);
            case JSON_V -> new JsonDataEntry(key, hasValue ? proto.getJsonV() : null);
            default -> null;
        };
    }

    public static BasicKvEntry fromProto(KeyValueProto proto) {
        String key = proto.getKey();
        return switch (proto.getType()) {
            case BOOLEAN_V -> new BooleanDataEntry(key, proto.getBoolV());
            case LONG_V -> new LongDataEntry(key, proto.getLongV());
            case DOUBLE_V -> new DoubleDataEntry(key, proto.getDoubleV());
            case STRING_V -> new StringDataEntry(key, proto.getStringV());
            case JSON_V -> new JsonDataEntry(key, proto.getJsonV());
            default -> null;
        };
    }

    public static BasicKvEntry basicKvEntryFromKvEntry(KvEntry kvEntry) {
        String key = kvEntry.getKey();
        return switch (kvEntry.getDataType()) {
            case BOOLEAN -> new BooleanDataEntry(key, kvEntry.getBooleanValue().orElse(null));
            case LONG -> new LongDataEntry(key, kvEntry.getLongValue().orElse(null));
            case DOUBLE -> new DoubleDataEntry(key, kvEntry.getDoubleValue().orElse(null));
            case STRING -> new StringDataEntry(key, kvEntry.getStrValue().orElse(null));
            case JSON -> new JsonDataEntry(key, kvEntry.getJsonValue().orElse(null));
        };
    }

    public static TsKvEntry fromProto(TransportProtos.TsKvProto proto) {
        TransportProtos.KeyValueProto kvProto = proto.getKv();
        String key = kvProto.getKey();
        KvEntry entry = switch (kvProto.getType()) {
            case BOOLEAN_V -> new BooleanDataEntry(key, kvProto.getBoolV());
            case LONG_V -> new LongDataEntry(key, kvProto.getLongV());
            case DOUBLE_V -> new DoubleDataEntry(key, kvProto.getDoubleV());
            case STRING_V -> new StringDataEntry(key, kvProto.getStringV());
            case JSON_V -> new JsonDataEntry(key, kvProto.getJsonV());
            default -> null;
        };
        return new BasicTsKvEntry(proto.getTs(), entry, proto.hasVersion() ? proto.getVersion() : null);
    }

    public static TransportProtos.TsKvProto toTsKvProto(TsKvEntry tsKvEntry) {
        var builder = TransportProtos.TsKvProto.newBuilder()
                .setTs(tsKvEntry.getTs())
                .setKv(toKeyValueProto(tsKvEntry));
        if (tsKvEntry.getVersion() != null) {
            builder.setVersion(tsKvEntry.getVersion());
        }
        return builder.build();
    }

    public static TransportProtos.KeyValueProto toKeyValueProto(KvEntry kvEntry) {
        TransportProtos.KeyValueProto.Builder builder = TransportProtos.KeyValueProto.newBuilder();
        builder.setKey(kvEntry.getKey());
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                builder.setType(TransportProtos.KeyValueType.BOOLEAN_V)
                        .setBoolV(kvEntry.getBooleanValue().orElse(false));
                break;
            case LONG:
                builder.setType(TransportProtos.KeyValueType.LONG_V)
                        .setLongV(kvEntry.getLongValue().orElse(0L));
                break;
            case DOUBLE:
                builder.setType(TransportProtos.KeyValueType.DOUBLE_V)
                        .setDoubleV(kvEntry.getDoubleValue().orElse(0.0));
                break;
            case STRING:
                builder.setType(TransportProtos.KeyValueType.STRING_V)
                        .setStringV(kvEntry.getStrValue().orElse(""));
                break;
            case JSON:
                builder.setType(TransportProtos.KeyValueType.JSON_V)
                        .setJsonV(kvEntry.getJsonValue().orElse("{}"));
                break;
            default:
                throw new IllegalArgumentException("Unsupported KvEntry data type: " + kvEntry.getDataType());
        }
        return builder.build();
    }

    public static TransportProtos.DeviceProto toProto(Device device) {
        var builder = TransportProtos.DeviceProto.newBuilder()
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setCreatedTime(device.getCreatedTime())
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits());

        if (isNotNull(device.getCustomerId())) {
            builder.setCustomerIdMSB(getMsb(device.getCustomerId()))
                    .setCustomerIdLSB(getLsb(device.getCustomerId()));
        }
        if (isNotNull(device.getLabel())) {
            builder.setDeviceLabel(device.getLabel());
        }
        if (isNotNull(device.getAdditionalInfo())) {
            builder.setAdditionalInfo(JacksonUtil.toString(device.getAdditionalInfo()));
        }
        if (isNotNull(device.getFirmwareId())) {
            builder.setFirmwareIdMSB(getMsb(device.getFirmwareId()))
                    .setFirmwareIdLSB(getLsb(device.getFirmwareId()));
        }
        if (isNotNull(device.getSoftwareId())) {
            builder.setSoftwareIdMSB(getMsb(device.getSoftwareId()))
                    .setSoftwareIdLSB(getLsb(device.getSoftwareId()));
        }
        if (isNotNull(device.getExternalId())) {
            builder.setExternalIdMSB(getMsb(device.getExternalId()))
                    .setExternalIdLSB(getLsb(device.getExternalId()));
        }
        if (isNotNull(device.getDeviceDataBytes())) {
            builder.setDeviceData(ByteString.copyFrom(device.getDeviceDataBytes()));
        }
        if (isNotNull(device.getVersion())) {
            builder.setVersion(device.getVersion());
        }
        return builder.build();
    }

    public static Device fromProto(TransportProtos.DeviceProto proto) {
        Device device = new Device(getEntityId(proto.getDeviceIdMSB(), proto.getDeviceIdLSB(), DeviceId::new));
        device.setCreatedTime(proto.getCreatedTime());
        device.setTenantId(getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID));
        device.setName(proto.getDeviceName());
        device.setType(proto.getDeviceType());
        device.setDeviceProfileId(getEntityId(proto.getDeviceProfileIdMSB(), proto.getDeviceProfileIdLSB(), DeviceProfileId::new));
        if (proto.hasCustomerIdMSB() && proto.hasCustomerIdLSB()) {
            device.setCustomerId(getEntityId(proto.getCustomerIdMSB(), proto.getCustomerIdLSB(), CustomerId::new));
        }
        if (proto.hasDeviceLabel()) {
            device.setLabel(proto.getDeviceLabel());
        }
        if (proto.hasAdditionalInfo()) {
            device.setAdditionalInfo(JacksonUtil.toJsonNode(proto.getAdditionalInfo()));
        }
        if (proto.hasFirmwareIdMSB() && proto.hasFirmwareIdLSB()) {
            device.setFirmwareId(getEntityId(proto.getFirmwareIdMSB(), proto.getFirmwareIdLSB(), OtaPackageId::new));
        }
        if (proto.hasSoftwareIdMSB() && proto.hasSoftwareIdLSB()) {
            device.setSoftwareId(getEntityId(proto.getSoftwareIdMSB(), proto.getSoftwareIdLSB(), OtaPackageId::new));
        }
        if (proto.hasExternalIdMSB() && proto.hasExternalIdLSB()) {
            device.setExternalId(getEntityId(proto.getExternalIdMSB(), proto.getExternalIdLSB(), DeviceId::new));
        }
        if (proto.hasDeviceData()) {
            device.setDeviceDataBytes(proto.getDeviceData().toByteArray());
        }
        if (proto.hasVersion()) {
            device.setVersion(proto.getVersion());
        }
        return device;
    }

    public static TransportProtos.DeviceProfileProto toProto(DeviceProfile deviceProfile) {
        var builder = TransportProtos.DeviceProfileProto.newBuilder()
                .setTenantIdMSB(getMsb(deviceProfile.getTenantId()))
                .setTenantIdLSB(getLsb(deviceProfile.getTenantId()))
                .setDeviceProfileIdMSB(getMsb(deviceProfile.getId()))
                .setDeviceProfileIdLSB(getLsb(deviceProfile.getId()))
                .setCreatedTime(deviceProfile.getCreatedTime())
                .setName(deviceProfile.getName())
                .setIsDefault(deviceProfile.isDefault())
                .setType(deviceProfile.getType().name())
                .setTransportType(deviceProfile.getTransportType().name())
                .setProvisionType(deviceProfile.getProvisionType().name());

        if (isNotNull(deviceProfile.getProfileDataBytes())) {
            builder.setDeviceProfileData(ByteString.copyFrom(deviceProfile.getProfileDataBytes()));
        }
        if (isNotNull(deviceProfile.getDescription())) {
            builder.setDescription(deviceProfile.getDescription());
        }
        if (isNotNull(deviceProfile.getImage())) {
            builder.setImage(deviceProfile.getImage());
        }
        if (isNotNull(deviceProfile.getDefaultRuleChainId())) {
            builder.setDefaultRuleChainIdMSB(getMsb(deviceProfile.getDefaultRuleChainId()))
                    .setDefaultRuleChainIdLSB(getLsb(deviceProfile.getDefaultRuleChainId()));
        }
        if (isNotNull(deviceProfile.getDefaultDashboardId())) {
            builder.setDefaultDashboardIdMSB(getMsb(deviceProfile.getDefaultDashboardId()))
                    .setDefaultDashboardIdLSB(getLsb(deviceProfile.getDefaultDashboardId()));
        }
        if (isNotNull(deviceProfile.getDefaultQueueName())) {
            builder.setDefaultQueueName(deviceProfile.getDefaultQueueName());
        }
        if (isNotNull(deviceProfile.getProvisionDeviceKey())) {
            builder.setProvisionDeviceKey(deviceProfile.getProvisionDeviceKey());
        }
        if (isNotNull(deviceProfile.getFirmwareId())) {
            builder.setFirmwareIdMSB(getMsb(deviceProfile.getFirmwareId()))
                    .setFirmwareIdLSB(getLsb(deviceProfile.getFirmwareId()));
        }
        if (isNotNull(deviceProfile.getSoftwareId())) {
            builder.setSoftwareIdMSB(getMsb(deviceProfile.getSoftwareId()))
                    .setSoftwareIdLSB(getLsb(deviceProfile.getSoftwareId()));
        }
        if (isNotNull(deviceProfile.getExternalId())) {
            builder.setExternalIdMSB(getMsb(deviceProfile.getExternalId()))
                    .setExternalIdLSB(getLsb(deviceProfile.getExternalId()));
        }
        if (isNotNull(deviceProfile.getDefaultEdgeRuleChainId())) {
            builder.setDefaultEdgeRuleChainIdMSB(getMsb(deviceProfile.getDefaultEdgeRuleChainId()))
                    .setDefaultEdgeRuleChainIdLSB(getLsb(deviceProfile.getDefaultEdgeRuleChainId()));
        }
        if (isNotNull(deviceProfile.getVersion())) {
            builder.setVersion(deviceProfile.getVersion());
        }
        return builder.build();
    }

    public static DeviceProfile fromProto(TransportProtos.DeviceProfileProto proto) {
        DeviceProfile deviceProfile = new DeviceProfile(getEntityId(proto.getDeviceProfileIdMSB(), proto.getDeviceProfileIdLSB(), DeviceProfileId::new));
        deviceProfile.setCreatedTime(proto.getCreatedTime());
        deviceProfile.setTenantId(getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID));
        deviceProfile.setName(proto.getName());
        deviceProfile.setDefault(proto.getIsDefault());
        deviceProfile.setType(DeviceProfileType.valueOf(proto.getType()));
        deviceProfile.setTransportType(DeviceTransportType.valueOf(proto.getTransportType()));
        deviceProfile.setProvisionType(DeviceProfileProvisionType.valueOf(proto.getProvisionType()));
        if (proto.hasDeviceProfileData()) {
            deviceProfile.setProfileDataBytes(proto.getDeviceProfileData().toByteArray());
        }
        if (proto.hasDescription()) {
            deviceProfile.setDescription(proto.getDescription());
        }
        if (proto.hasImage()) {
            deviceProfile.setImage(proto.getImage());
        }
        if (proto.hasDefaultRuleChainIdMSB() && proto.hasDefaultRuleChainIdLSB()) {
            deviceProfile.setDefaultRuleChainId(getEntityId(proto.getDefaultRuleChainIdMSB(), proto.getDefaultRuleChainIdLSB(), RuleChainId::new));
        }
        if (proto.hasDefaultDashboardIdMSB() && proto.hasDefaultDashboardIdLSB()) {
            deviceProfile.setDefaultDashboardId(getEntityId(proto.getDefaultDashboardIdMSB(), proto.getDefaultDashboardIdLSB(), DashboardId::new));
        }
        if (proto.hasDefaultQueueName()) {
            deviceProfile.setDefaultQueueName(proto.getDefaultQueueName());
        }
        if (proto.hasProvisionDeviceKey()) {
            deviceProfile.setProvisionDeviceKey(proto.getProvisionDeviceKey());
        }
        if (proto.hasFirmwareIdMSB() && proto.hasFirmwareIdLSB()) {
            deviceProfile.setFirmwareId(getEntityId(proto.getFirmwareIdMSB(), proto.getFirmwareIdLSB(), OtaPackageId::new));
        }
        if (proto.hasSoftwareIdMSB() && proto.hasSoftwareIdLSB()) {
            deviceProfile.setSoftwareId(getEntityId(proto.getSoftwareIdMSB(), proto.getSoftwareIdLSB(), OtaPackageId::new));
        }
        if (proto.hasExternalIdMSB() && proto.hasExternalIdLSB()) {
            deviceProfile.setExternalId(getEntityId(proto.getExternalIdMSB(), proto.getExternalIdLSB(), DeviceProfileId::new));
        }
        if (proto.hasDefaultEdgeRuleChainIdMSB() && proto.hasDefaultEdgeRuleChainIdLSB()) {
            deviceProfile.setDefaultEdgeRuleChainId(getEntityId(proto.getDefaultEdgeRuleChainIdMSB(), proto.getDefaultEdgeRuleChainIdLSB(), RuleChainId::new));
        }
        if (proto.hasVersion()) {
            deviceProfile.setVersion(proto.getVersion());
        }
        return deviceProfile;
    }

    public static TransportProtos.DeviceProfileCacheInfoProto toCacheProto(DeviceProfile deviceProfile) {
        var builder = TransportProtos.DeviceProfileCacheInfoProto.newBuilder()
                .setDeviceProfileIdMSB(getMsb(deviceProfile.getId()))
                .setDeviceProfileIdLSB(getLsb(deviceProfile.getId()))
                .setName(deviceProfile.getName())
                .setTenantIdMSB(getMsb(deviceProfile.getTenantId()))
                .setTenantIdLSB(getLsb(deviceProfile.getTenantId()));

        if (isNotNull(deviceProfile.getDefaultRuleChainId())) {
            builder.setDefaultRuleChainIdMSB(getMsb(deviceProfile.getDefaultRuleChainId()))
                    .setDefaultRuleChainIdLSB(getLsb(deviceProfile.getDefaultRuleChainId()));
        }
        if (isNotNull(deviceProfile.getDefaultQueueName())) {
            builder.setDefaultQueueName(deviceProfile.getDefaultQueueName());
        }
        return builder.build();
    }

    public static TransportProtos.DeviceProfileCacheInfoProto toCacheProto(DeviceProfileCacheInfo cacheInfo) {
        var builder = TransportProtos.DeviceProfileCacheInfoProto.newBuilder()
                .setDeviceProfileIdMSB(cacheInfo.id().getMostSignificantBits())
                .setDeviceProfileIdLSB(cacheInfo.id().getLeastSignificantBits())
                .setName(cacheInfo.name())
                .setTenantIdMSB(cacheInfo.tenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(cacheInfo.tenantId().getId().getLeastSignificantBits());

        if (isNotNull(cacheInfo.defaultRuleChainId())) {
            builder.setDefaultRuleChainIdMSB(getMsb(cacheInfo.defaultRuleChainId()))
                    .setDefaultRuleChainIdLSB(getLsb(cacheInfo.defaultRuleChainId()));
        }
        if (isNotNull(cacheInfo.defaultQueueName())) {
            builder.setDefaultQueueName(cacheInfo.defaultQueueName());
        }
        return builder.build();
    }

    public static DeviceProfileCacheInfo fromCacheProto(TransportProtos.DeviceProfileCacheInfoProto proto) {
        DeviceProfileId deviceProfileId = getEntityId(proto.getDeviceProfileIdMSB(), proto.getDeviceProfileIdLSB(), DeviceProfileId::new);
        TenantId tenantId = getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID);
        RuleChainId defaultRuleChainId = null;
        if (proto.hasDefaultRuleChainIdMSB() && proto.hasDefaultRuleChainIdLSB()) {
            defaultRuleChainId = getEntityId(proto.getDefaultRuleChainIdMSB(), proto.getDefaultRuleChainIdLSB(), RuleChainId::new);
        }
        String defaultQueueName = proto.hasDefaultQueueName() ? proto.getDefaultQueueName() : null;
        return new DeviceProfileCacheInfo(deviceProfileId.getId(), tenantId, proto.getName(), defaultRuleChainId, defaultQueueName);
    }

    public static TransportProtos.DeviceCacheInfoProto toCacheProto(Device device) {
        var builder = TransportProtos.DeviceCacheInfoProto.newBuilder()
                .setTenantIdMSB(getMsb(device.getTenantId()))
                .setTenantIdLSB(getLsb(device.getTenantId()))
                .setDeviceIdMSB(getMsb(device.getId()))
                .setDeviceIdLSB(getLsb(device.getId()))
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(getMsb(device.getDeviceProfileId()))
                .setDeviceProfileIdLSB(getLsb(device.getDeviceProfileId()));

        if (isNotNull(device.getCustomerId())) {
            builder.setCustomerIdMSB(getMsb(device.getCustomerId()))
                    .setCustomerIdLSB(getLsb(device.getCustomerId()));
        }

        return builder.build();
    }

    public static TransportProtos.DeviceCacheInfoProto toCacheProto(org.thingsboard.server.common.data.DeviceCacheInfo cacheInfo) {
        var builder = TransportProtos.DeviceCacheInfoProto.newBuilder()
                .setTenantIdMSB(cacheInfo.tenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(cacheInfo.tenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(cacheInfo.getId().getMostSignificantBits())
                .setDeviceIdLSB(cacheInfo.getId().getLeastSignificantBits())
                .setDeviceName(cacheInfo.name())
                .setDeviceType(cacheInfo.type())
                .setDeviceProfileIdMSB(cacheInfo.deviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(cacheInfo.deviceProfileId().getId().getLeastSignificantBits());

        if (isNotNull(cacheInfo.customerId())) {
            builder.setCustomerIdMSB(cacheInfo.customerId().getId().getMostSignificantBits())
                    .setCustomerIdLSB(cacheInfo.customerId().getId().getLeastSignificantBits());
        }

        return builder.build();
    }

    public static org.thingsboard.server.common.data.DeviceCacheInfo fromCacheProto(TransportProtos.DeviceCacheInfoProto proto) {
        TenantId tenantId = getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID);
        DeviceId deviceId = getEntityId(proto.getDeviceIdMSB(), proto.getDeviceIdLSB(), DeviceId::new);
        DeviceProfileId deviceProfileId = getEntityId(proto.getDeviceProfileIdMSB(), proto.getDeviceProfileIdLSB(), DeviceProfileId::new);
        CustomerId customerId = null;
        if (proto.hasCustomerIdMSB() && proto.hasCustomerIdLSB()) {
            customerId = getEntityId(proto.getCustomerIdMSB(), proto.getCustomerIdLSB(), CustomerId::new);
        }
        return new org.thingsboard.server.common.data.DeviceCacheInfo(deviceId.getId(), tenantId, customerId, proto.getDeviceName(), proto.getDeviceType(), deviceProfileId);
    }

    public static TransportProtos.AssetCacheInfoProto toCacheProto(Asset asset) {
        var builder = TransportProtos.AssetCacheInfoProto.newBuilder()
                .setTenantIdMSB(getMsb(asset.getTenantId()))
                .setTenantIdLSB(getLsb(asset.getTenantId()))
                .setAssetIdMSB(getMsb(asset.getId()))
                .setAssetIdLSB(getLsb(asset.getId()))
                .setAssetName(asset.getName())
                .setAssetType(asset.getType())
                .setAssetProfileIdMSB(getMsb(asset.getAssetProfileId()))
                .setAssetProfileIdLSB(getLsb(asset.getAssetProfileId()));

        if (isNotNull(asset.getCustomerId())) {
            builder.setCustomerIdMSB(getMsb(asset.getCustomerId()))
                    .setCustomerIdLSB(getLsb(asset.getCustomerId()));
        }

        return builder.build();
    }

    public static TransportProtos.AssetCacheInfoProto toCacheProto(org.thingsboard.server.common.data.AssetCacheInfo cacheInfo) {
        var builder = TransportProtos.AssetCacheInfoProto.newBuilder()
                .setTenantIdMSB(cacheInfo.tenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(cacheInfo.tenantId().getId().getLeastSignificantBits())
                .setAssetIdMSB(cacheInfo.getId().getMostSignificantBits())
                .setAssetIdLSB(cacheInfo.getId().getLeastSignificantBits())
                .setAssetName(cacheInfo.name())
                .setAssetType(cacheInfo.type())
                .setAssetProfileIdMSB(cacheInfo.assetProfileId().getId().getMostSignificantBits())
                .setAssetProfileIdLSB(cacheInfo.assetProfileId().getId().getLeastSignificantBits());

        if (isNotNull(cacheInfo.customerId())) {
            builder.setCustomerIdMSB(cacheInfo.customerId().getId().getMostSignificantBits())
                    .setCustomerIdLSB(cacheInfo.customerId().getId().getLeastSignificantBits());
        }

        return builder.build();
    }

    public static AssetCacheInfo fromCacheProto(TransportProtos.AssetCacheInfoProto proto) {
        TenantId tenantId = getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID);
        AssetId assetId = getEntityId(proto.getAssetIdMSB(), proto.getAssetIdLSB(), AssetId::new);
        AssetProfileId assetProfileId = getEntityId(proto.getAssetProfileIdMSB(), proto.getAssetProfileIdLSB(), AssetProfileId::new);
        CustomerId customerId = null;
        if (proto.hasCustomerIdMSB() && proto.hasCustomerIdLSB()) {
            customerId = getEntityId(proto.getCustomerIdMSB(), proto.getCustomerIdLSB(), CustomerId::new);
        }
        return new AssetCacheInfo(assetId.getId(), tenantId, customerId, proto.getAssetName(), proto.getAssetType(), assetProfileId);
    }

    public static TransportProtos.AssetProfileCacheInfoProto toCacheProto(AssetProfile assetProfile) {
        var builder = TransportProtos.AssetProfileCacheInfoProto.newBuilder()
                .setAssetProfileIdMSB(getMsb(assetProfile.getId()))
                .setAssetProfileIdLSB(getLsb(assetProfile.getId()))
                .setName(assetProfile.getName())
                .setTenantIdMSB(getMsb(assetProfile.getTenantId()))
                .setTenantIdLSB(getLsb(assetProfile.getTenantId()));

        if (isNotNull(assetProfile.getDefaultRuleChainId())) {
            builder.setDefaultRuleChainIdMSB(getMsb(assetProfile.getDefaultRuleChainId()))
                    .setDefaultRuleChainIdLSB(getLsb(assetProfile.getDefaultRuleChainId()));
        }
        if (isNotNull(assetProfile.getDefaultQueueName())) {
            builder.setDefaultQueueName(assetProfile.getDefaultQueueName());
        }
        return builder.build();
    }

    public static TransportProtos.AssetProfileCacheInfoProto toCacheProto(AssetProfileCacheInfo cacheInfo) {
        var builder = TransportProtos.AssetProfileCacheInfoProto.newBuilder()
                .setAssetProfileIdMSB(cacheInfo.id().getMostSignificantBits())
                .setAssetProfileIdLSB(cacheInfo.id().getLeastSignificantBits())
                .setName(cacheInfo.name())
                .setTenantIdMSB(cacheInfo.tenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(cacheInfo.tenantId().getId().getLeastSignificantBits());

        if (isNotNull(cacheInfo.defaultRuleChainId())) {
            builder.setDefaultRuleChainIdMSB(getMsb(cacheInfo.defaultRuleChainId()))
                    .setDefaultRuleChainIdLSB(getLsb(cacheInfo.defaultRuleChainId()));
        }
        if (isNotNull(cacheInfo.defaultQueueName())) {
            builder.setDefaultQueueName(cacheInfo.defaultQueueName());
        }
        return builder.build();
    }

    public static AssetProfileCacheInfo fromCacheProto(TransportProtos.AssetProfileCacheInfoProto proto) {
        AssetProfileId assetProfileId = getEntityId(proto.getAssetProfileIdMSB(), proto.getAssetProfileIdLSB(), AssetProfileId::new);
        TenantId tenantId = getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID);
        RuleChainId defaultRuleChainId = null;
        if (proto.hasDefaultRuleChainIdMSB() && proto.hasDefaultRuleChainIdLSB()) {
            defaultRuleChainId = getEntityId(proto.getDefaultRuleChainIdMSB(), proto.getDefaultRuleChainIdLSB(), RuleChainId::new);
        }
        String defaultQueueName = proto.hasDefaultQueueName() ? proto.getDefaultQueueName() : null;
        return new AssetProfileCacheInfo(assetProfileId.getId(), tenantId, proto.getName(), defaultRuleChainId, defaultQueueName);
    }

    public static TransportProtos.TenantProto toProto(Tenant tenant) {
        var builder = TransportProtos.TenantProto.newBuilder()
                .setTenantIdMSB(getMsb(tenant.getTenantId()))
                .setTenantIdLSB(getLsb(tenant.getTenantId()))
                .setCreatedTime(tenant.getCreatedTime())
                .setTenantProfileIdMSB(getMsb(tenant.getTenantProfileId()))
                .setTenantProfileIdLSB(getLsb(tenant.getTenantProfileId()))
                .setTitle(tenant.getTitle());

        if (isNotNull(tenant.getRegion())) {
            builder.setRegion(tenant.getRegion());
        }
        if (isNotNull(tenant.getCountry())) {
            builder.setCountry(tenant.getCountry());
        }
        if (isNotNull(tenant.getState())) {
            builder.setState(tenant.getState());
        }
        if (isNotNull(tenant.getCity())) {
            builder.setCity(tenant.getCity());
        }
        if (isNotNull(tenant.getAddress())) {
            builder.setAddress(tenant.getAddress());
        }
        if (isNotNull(tenant.getAddress2())) {
            builder.setAddress2(tenant.getAddress2());
        }
        if (isNotNull(tenant.getZip())) {
            builder.setZip(tenant.getZip());
        }
        if (isNotNull(tenant.getPhone())) {
            builder.setPhone(tenant.getPhone());
        }
        if (isNotNull(tenant.getEmail())) {
            builder.setEmail(tenant.getEmail());
        }
        if (isNotNull(tenant.getAdditionalInfo())) {
            builder.setAdditionalInfo(JacksonUtil.toString(tenant.getAdditionalInfo()));
        }
        if (isNotNull(tenant.getVersion())) {
            builder.setVersion(tenant.getVersion());
        }
        return builder.build();
    }

    public static Tenant fromProto(TransportProtos.TenantProto proto) {
        Tenant tenant = new Tenant(getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID));
        tenant.setCreatedTime(proto.getCreatedTime());
        tenant.setTenantProfileId(getEntityId(proto.getTenantProfileIdMSB(), proto.getTenantProfileIdLSB(), TenantProfileId::new));
        tenant.setTitle(proto.getTitle());

        if (proto.hasRegion()) {
            tenant.setRegion(proto.getRegion());
        }
        if (proto.hasCountry()) {
            tenant.setCountry(proto.getCountry());
        }
        if (proto.hasState()) {
            tenant.setState(proto.getState());
        }
        if (proto.hasCity()) {
            tenant.setCity(proto.getCity());
        }
        if (proto.hasAddress()) {
            tenant.setAddress(proto.getAddress());
        }
        if (proto.hasAddress2()) {
            tenant.setAddress2(proto.getAddress2());
        }
        if (proto.hasZip()) {
            tenant.setZip(proto.getZip());
        }
        if (proto.hasPhone()) {
            tenant.setPhone(proto.getPhone());
        }
        if (proto.hasEmail()) {
            tenant.setEmail(proto.getEmail());
        }
        if (proto.hasAdditionalInfo()) {
            tenant.setAdditionalInfo(JacksonUtil.toJsonNode(proto.getAdditionalInfo()));
        }
        if (proto.hasVersion()) {
            tenant.setVersion(proto.getVersion());
        }
        return tenant;
    }

    public static TransportProtos.TenantProfileProto toProto(TenantProfile tenantProfile) {
        var builder = TransportProtos.TenantProfileProto.newBuilder()
                .setTenantProfileIdMSB(getMsb(tenantProfile.getId()))
                .setTenantProfileIdLSB(getLsb(tenantProfile.getId()))
                .setCreatedTime(tenantProfile.getCreatedTime())
                .setName(tenantProfile.getName())
                .setIsDefault(tenantProfile.isDefault())
                .setIsolatedTbRuleEngine(tenantProfile.isIsolatedTbRuleEngine());

        if (isNotNull(tenantProfile.getDescription())) {
            builder.setDescription(tenantProfile.getDescription());
        }
        if (isNotNull(tenantProfile.getProfileDataBytes())) {
            builder.setProfileData(ByteString.copyFrom(tenantProfile.getProfileDataBytes()));
        }
        return builder.build();
    }

    public static TenantProfile fromProto(TransportProtos.TenantProfileProto proto) {
        TenantProfile tenantProfile = new TenantProfile(getEntityId(proto.getTenantProfileIdMSB(), proto.getTenantProfileIdLSB(), TenantProfileId::new));
        tenantProfile.setCreatedTime(proto.getCreatedTime());
        tenantProfile.setName(proto.getName());
        tenantProfile.setDefault(proto.getIsDefault());
        tenantProfile.setIsolatedTbRuleEngine(proto.getIsolatedTbRuleEngine());
        if (proto.hasDescription()) {
            tenantProfile.setDescription(proto.getDescription());
        }
        if (proto.hasProfileData()) {
            tenantProfile.setProfileDataBytes(proto.getProfileData().toByteArray());
        }
        return tenantProfile;
    }

    public static TransportProtos.TbResourceProto toProto(TbResource resource) {
        var builder = TransportProtos.TbResourceProto.newBuilder()
                .setTenantIdMSB(getMsb(resource.getTenantId()))
                .setTenantIdLSB(getLsb(resource.getTenantId()))
                .setResourceIdMSB(getMsb(resource.getId()))
                .setResourceIdLSB(getLsb(resource.getId()))
                .setCreatedTime(resource.getCreatedTime())
                .setTitle(resource.getTitle())
                .setResourceType(resource.getResourceType().name())
                .setResourceKey(resource.getResourceKey())
                .setIsPublic(resource.isPublic())
                .setSearchText(resource.getSearchText())
                .setFileName(resource.getFileName());
        if (isNotNull(resource.getPublicResourceKey())) {
            builder.setPublicResourceKey(resource.getPublicResourceKey());
        }
        if (isNotNull(resource.getEtag())) {
            builder.setEtag(resource.getEtag());
        }
        if (isNotNull(resource.getDescriptor())) {
            builder.setResourceDescriptor(JacksonUtil.toString(resource.getDescriptor()));
        }
        if (isNotNull(resource.getExternalId())) {
            builder.setExternalIdMSB(getMsb(resource.getExternalId()))
                    .setExternalIdLSB(getLsb(resource.getExternalId()));
        }
        if (isNotNull(resource.getData())) {
            builder.setData(ByteString.copyFrom(resource.getData()));
        }
        if (isNotNull(resource.getPreview())) {
            builder.setPreview(ByteString.copyFrom(resource.getPreview()));
        }
        if (isNotNull(resource.getCustomerId())) {
            builder.setCustomerIdMSB(getMsb(resource.getCustomerId()))
                    .setCustomerIdLSB(getLsb(resource.getCustomerId()));
        }
        if (isNotNull(resource.getResourceSubType())) {
            builder.setResourceSubType(resource.getResourceSubType().name());
        }
        return builder.build();
    }

    public static TbResource fromProto(TransportProtos.TbResourceProto proto) {
        TbResource resource = new TbResource(getEntityId(proto.getResourceIdMSB(), proto.getResourceIdLSB(), TbResourceId::new));
        resource.setTenantId(getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID));
        resource.setCreatedTime(proto.getCreatedTime());
        resource.setTitle(proto.getTitle());
        resource.setResourceType(ResourceType.valueOf(proto.getResourceType()));
        resource.setResourceKey(proto.getResourceKey());
        resource.setPublic(proto.getIsPublic());
        resource.setSearchText(proto.getSearchText());
        resource.setFileName(proto.getFileName());
        if (proto.hasPublicResourceKey()) {
            resource.setPublicResourceKey(proto.getPublicResourceKey());
        }
        if (proto.hasEtag()) {
            resource.setEtag(proto.getEtag());
        }
        if (proto.hasResourceDescriptor()) {
            resource.setDescriptor(JacksonUtil.toJsonNode(proto.getResourceDescriptor()));
        }
        if (proto.hasExternalIdMSB() && proto.hasExternalIdLSB()) {
            resource.setExternalId(getEntityId(proto.getExternalIdMSB(), proto.getExternalIdLSB(), TbResourceId::new));
        }
        if (proto.hasData()) {
            resource.setData(proto.getData().toByteArray());
        }
        if (proto.hasPreview()) {
            resource.setPreview(proto.getPreview().toByteArray());
        }
        if (proto.hasCustomerIdMSB() && proto.hasCustomerIdLSB()) {
            resource.setCustomerId(getEntityId(proto.getCustomerIdMSB(), proto.getCustomerIdLSB(), CustomerId::new));
        }
        if (proto.hasResourceSubType()) {
            resource.setResourceSubType(ResourceSubType.valueOf(proto.getResourceSubType()));
        }
        return resource;
    }

    public static TransportProtos.ApiUsageStateProto toProto(ApiUsageState apiUsageState) {
        return TransportProtos.ApiUsageStateProto.newBuilder()
                .setTenantProfileIdMSB(getMsb(apiUsageState.getTenantId()))
                .setTenantProfileIdLSB(getLsb(apiUsageState.getTenantId()))
                .setApiUsageStateIdMSB(getMsb(apiUsageState.getId()))
                .setApiUsageStateIdLSB(getLsb(apiUsageState.getId()))
                .setCreatedTime(apiUsageState.getCreatedTime())
                .setEntityType(toProto(apiUsageState.getEntityId().getEntityType()))
                .setEntityIdMSB(getMsb(apiUsageState.getEntityId()))
                .setEntityIdLSB(getLsb(apiUsageState.getEntityId()))
                .setTransportState(apiUsageState.getTransportState().name())
                .setDbStorageState(apiUsageState.getDbStorageState().name())
                .setReExecState(apiUsageState.getReExecState().name())
                .setJsExecState(apiUsageState.getJsExecState().name())
                .setTbelExecState(apiUsageState.getTbelExecState().name())
                .setEmailExecState(apiUsageState.getEmailExecState().name())
                .setSmsExecState(apiUsageState.getSmsExecState().name())
                .setAlarmExecState(apiUsageState.getAlarmExecState().name())
                .setReportExecState(apiUsageState.getReportExecState().name())
                .setAiState(apiUsageState.getAiState().name())
                .setVersion(apiUsageState.getVersion())
                .build();
    }

    public static ApiUsageState fromProto(TransportProtos.ApiUsageStateProto proto) {
        ApiUsageState apiUsageState = new ApiUsageState(getEntityId(proto.getApiUsageStateIdMSB(), proto.getApiUsageStateIdLSB(), ApiUsageStateId::new));
        apiUsageState.setTenantId(getEntityId(proto.getTenantProfileIdMSB(), proto.getTenantProfileIdLSB(), TenantId::fromUUID));
        apiUsageState.setCreatedTime(proto.getCreatedTime());
        apiUsageState.setEntityId(EntityIdFactory.getByTypeAndUuid(fromProto(proto.getEntityType()), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB())));
        apiUsageState.setTransportState(ApiUsageStateValue.valueOf(proto.getTransportState()));
        apiUsageState.setDbStorageState(ApiUsageStateValue.valueOf(proto.getDbStorageState()));
        apiUsageState.setReExecState(ApiUsageStateValue.valueOf(proto.getReExecState()));
        apiUsageState.setJsExecState(ApiUsageStateValue.valueOf(proto.getJsExecState()));
        apiUsageState.setTbelExecState(ApiUsageStateValue.valueOf(proto.getTbelExecState()));
        apiUsageState.setEmailExecState(ApiUsageStateValue.valueOf(proto.getEmailExecState()));
        apiUsageState.setSmsExecState(ApiUsageStateValue.valueOf(proto.getSmsExecState()));
        apiUsageState.setAlarmExecState(ApiUsageStateValue.valueOf(proto.getAlarmExecState()));
        apiUsageState.setReportExecState(ApiUsageStateValue.valueOf(proto.getReportExecState()));
        apiUsageState.setAiState(proto.getAiState().isEmpty() ? ApiUsageStateValue.ENABLED : ApiUsageStateValue.valueOf(proto.getAiState()));
        apiUsageState.setVersion(proto.getVersion());
        return apiUsageState;
    }

    public static TransportProtos.RepositorySettingsProto toProto(RepositorySettings repositorySettings) {
        var builder = TransportProtos.RepositorySettingsProto.newBuilder()
                .setRepositoryUri(repositorySettings.getRepositoryUri())
                .setAuthMethod(repositorySettings.getAuthMethod().name())
                .setReadOnly(repositorySettings.isReadOnly())
                .setShowMergeCommits(repositorySettings.isShowMergeCommits())
                .setLocalOnly(repositorySettings.isLocalOnly());

        if (isNotNull(repositorySettings.getUsername())) {
            builder.setUsername(repositorySettings.getUsername());
        }
        if (isNotNull(repositorySettings.getPassword())) {
            builder.setPassword(repositorySettings.getPassword());
        }
        if (isNotNull(repositorySettings.getPrivateKeyFileName())) {
            builder.setPrivateKeyFileName(repositorySettings.getPrivateKeyFileName());
        }
        if (isNotNull(repositorySettings.getPrivateKey())) {
            builder.setPrivateKey(repositorySettings.getPrivateKey());
        }
        if (isNotNull(repositorySettings.getPrivateKeyPassword())) {
            builder.setPrivateKeyPassword(repositorySettings.getPrivateKeyPassword());
        }
        if (isNotNull(repositorySettings.getDefaultBranch())) {
            builder.setDefaultBranch(repositorySettings.getDefaultBranch());
        }
        return builder.build();
    }

    public static RepositorySettings fromProto(TransportProtos.RepositorySettingsProto proto) {
        RepositorySettings repositorySettings = new RepositorySettings();
        repositorySettings.setRepositoryUri(proto.getRepositoryUri());
        repositorySettings.setAuthMethod(RepositoryAuthMethod.valueOf(proto.getAuthMethod()));
        repositorySettings.setReadOnly(proto.getReadOnly());
        repositorySettings.setShowMergeCommits(proto.getShowMergeCommits());
        repositorySettings.setLocalOnly(proto.getLocalOnly());
        if (proto.hasUsername()) {
            repositorySettings.setUsername(proto.getUsername());
        }
        if (proto.hasPassword()) {
            repositorySettings.setPassword(proto.getPassword());
        }
        if (proto.hasPrivateKeyFileName()) {
            repositorySettings.setPrivateKeyFileName(proto.getPrivateKeyFileName());
        }
        if (proto.hasPrivateKey()) {
            repositorySettings.setPrivateKey(proto.getPrivateKey());
        }
        if (proto.hasPrivateKeyPassword()) {
            repositorySettings.setPrivateKeyPassword(proto.getPrivateKeyPassword());
        }
        if (proto.hasDefaultBranch()) {
            repositorySettings.setDefaultBranch(proto.getDefaultBranch());
        }
        return repositorySettings;
    }

    public static TransportProtos.DeviceCredentialsProto toProto(DeviceCredentials deviceCredentials) {
        TransportProtos.DeviceCredentialsProto.Builder builder = TransportProtos.DeviceCredentialsProto.newBuilder()
                .setCredentialsIdMSB(deviceCredentials.getId().getId().getMostSignificantBits())
                .setCredentialsIdLSB(deviceCredentials.getId().getId().getLeastSignificantBits())
                .setCreatedTime(deviceCredentials.getCreatedTime())
                .setDeviceIdMSB(getMsb(deviceCredentials.getDeviceId()))
                .setDeviceIdLSB(getLsb(deviceCredentials.getDeviceId()))
                .setCredentialsId(deviceCredentials.getCredentialsId())
                .setCredentialsType(TransportProtos.CredentialsType.valueOf(deviceCredentials.getCredentialsType().name()));

        if (deviceCredentials.getCredentialsValue() != null) {
            builder.setCredentialsValue(deviceCredentials.getCredentialsValue());
        }
        if (deviceCredentials.getVersion() != null) {
            builder.setVersion(deviceCredentials.getVersion());
        }
        return builder.build();
    }

    public static DeviceCredentials fromProto(TransportProtos.DeviceCredentialsProto proto) {
        DeviceCredentials deviceCredentials =
                new DeviceCredentials(new DeviceCredentialsId(new UUID(proto.getCredentialsIdMSB(), proto.getCredentialsIdLSB())));
        deviceCredentials.setCreatedTime(proto.getCreatedTime());
        deviceCredentials.setDeviceId(getEntityId(proto.getDeviceIdMSB(), proto.getDeviceIdLSB(), DeviceId::new));
        deviceCredentials.setCredentialsId(proto.getCredentialsId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(proto.getCredentialsType().name()));
        deviceCredentials.setCredentialsValue(proto.hasCredentialsValue() ? proto.getCredentialsValue() : null);
        deviceCredentials.setVersion(proto.hasVersion() ? proto.getVersion() : null);
        return deviceCredentials;
    }

    public static <T> TransportProtos.EntityUpdateMsg toEntityUpdateProto(T entity) {
        var builder = TransportProtos.EntityUpdateMsg.newBuilder();
        if (entity instanceof Device) {
            builder.setDevice(toProto((Device) entity));
        } else if (entity instanceof DeviceProfile) {
            builder.setDeviceProfile(toProto((DeviceProfile) entity));
        } else if (entity instanceof Tenant) {
            builder.setTenant(toProto((Tenant) entity));
        } else if (entity instanceof TenantProfile) {
            builder.setTenantProfile(toProto((TenantProfile) entity));
        } else if (entity instanceof ApiUsageState) {
            builder.setApiUsageState(toProto((ApiUsageState) entity));
        } else {
            log.warn("[{}] entity does not support toProto serialization .", entity.getClass().getSimpleName());
        }
        return builder.build();
    }

    public static TransportProtos.DeviceInfoProto toDeviceInfoProto(Device device) throws JsonProcessingException {
        TransportProtos.DeviceInfoProto.Builder builder = TransportProtos.DeviceInfoProto.newBuilder()
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(getMsb(device.getCustomerId()))
                .setCustomerIdLSB(getLsb(device.getCustomerId()))
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits())
                .setAdditionalInfo(JacksonUtil.toString(device.getAdditionalInfo()));

        if (device.getAdditionalInfo().has(GATEWAY_PARAMETER)) {
            builder.setIsGateway(device.getAdditionalInfo().get(GATEWAY_PARAMETER).booleanValue());
        }

        PowerSavingConfiguration psmConfiguration = switch (device.getDeviceData().getTransportConfiguration().getType()) {
            case LWM2M -> (Lwm2mDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
            case COAP -> (CoapDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
            default -> null;
        };

        if (psmConfiguration != null) {
            PowerMode powerMode = psmConfiguration.getPowerMode();
            if (powerMode != null) {
                builder.setPowerMode(powerMode.name());
                if (powerMode.equals(PowerMode.PSM)) {
                    builder.setPsmActivityTimer(checkLong(psmConfiguration.getPsmActivityTimer()));
                } else if (powerMode.equals(PowerMode.E_DRX)) {
                    builder.setEdrxCycle(checkLong(psmConfiguration.getEdrxCycle()));
                    builder.setPagingTransmissionWindow(checkLong(psmConfiguration.getPagingTransmissionWindow()));
                }
            }
        }
        return builder.build();
    }

    /**
     * Builds the lightweight {@link IntegrationInfoProto} that carries only the base integration scalars
     * (id, tenantId, name, type, enabled, remote, allowCreateDevicesOrAssets). Named explicitly rather than
     * overloading {@code toProto} because {@link Integration} also has a {@code toProto(Integration)} overload
     * that produces the full {@link IntegrationProto}; overload resolution on an {@link Integration} argument
     * would silently pick that fuller mapping, so callers wanting the info projection must call this by name.
     */
    public static IntegrationInfoProto toIntegrationInfoProto(AbstractIntegration integrationInfo) {
        return IntegrationInfoProto.newBuilder()
                .setIntegrationIdMSB(integrationInfo.getId().getId().getMostSignificantBits())
                .setIntegrationIdLSB(integrationInfo.getId().getId().getLeastSignificantBits())
                .setTenantIdMSB(integrationInfo.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(integrationInfo.getTenantId().getId().getLeastSignificantBits())
                .setName(integrationInfo.getName())
                .setType(integrationInfo.getType().name())
                .setEnabled(integrationInfo.isEnabled())
                .setRemote(integrationInfo.isRemote())
                .setAllowCreateDevicesOrAssets(integrationInfo.isAllowCreateDevicesOrAssets())
                .build();
    }

    public static IntegrationInfo fromProto(IntegrationInfoProto proto) {
        var result = new IntegrationInfo(new IntegrationId(new UUID(proto.getIntegrationIdMSB(), proto.getIntegrationIdLSB())));
        result.setTenantId(TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())));
        result.setName(proto.getName());
        result.setType(IntegrationType.valueOf(proto.getType()));
        result.setRemote(proto.getRemote());
        result.setEnabled(proto.getEnabled());
        result.setAllowCreateDevicesOrAssets(proto.getAllowCreateDevicesOrAssets());
        return result;
    }

    /**
     * WARNING: returns a PARTIALLY populated {@link Integration}. Only the base fields carried by
     * {@link IntegrationInfoProto} are set (id, tenantId, name, type, enabled, remote,
     * allowCreateDevicesOrAssets); configuration, defaultConverterId and additionalInfo are left null.
     * Do not hand the result to consumers that expect a fully loaded Integration.
     */
    public static Integration fromProtoToIntegration(IntegrationInfoProto proto) {
        return new Integration(
                new UUID(proto.getIntegrationIdMSB(), proto.getIntegrationIdLSB()),
                new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                proto.getName(),
                IntegrationType.valueOf(proto.getType()),
                proto.getEnabled(),
                proto.getRemote(),
                proto.getAllowCreateDevicesOrAssets());
    }

    public static IntegrationProto toProto(Integration integration) {
        var builder = IntegrationProto.newBuilder()
                .setIntegrationIdMSB(getMsb(integration.getId()))
                .setIntegrationIdLSB(getLsb(integration.getId()))
                .setCreatedTime(integration.getCreatedTime())
                .setTenantIdMSB(getMsb(integration.getTenantId()))
                .setTenantIdLSB(getLsb(integration.getTenantId()))
                .setType(integration.getType().name())
                .setName(integration.getName())
                .setEnabled(integration.isEnabled())
                .setRemote(integration.isRemote())
                .setAllowCreateDevicesOrAssets(integration.isAllowCreateDevicesOrAssets())
                .setIsEdgeTemplate(integration.isEdgeTemplate())
                .setDefaultConverterIdMSB(getMsb(integration.getDefaultConverterId()))
                .setDefaultConverterIdLSB(getLsb(integration.getDefaultConverterId()))
                .setRoutingKey(integration.getRoutingKey())
                .setConfiguration(JacksonUtil.toString(integration.getConfiguration()));

        if (isNotNull(integration.getDebugSettings())) {
            builder.setDebugSettings(JacksonUtil.toString(integration.getDebugSettings()));
        }

        if (isNotNull(integration.getSecret())) {
            builder.setSecret(integration.getSecret());
        }

        if (isNotNull(integration.getDownlinkConverterId())) {
            builder.setDownlinkConverterIdMSB(getMsb(integration.getDownlinkConverterId()))
                    .setDownlinkConverterIdLSB(getLsb(integration.getDownlinkConverterId()));
        }
        if (isNotNull(integration.getAdditionalInfo())) {
            builder.setAdditionalInfo(JacksonUtil.toString(integration.getAdditionalInfo()));
        }
        if (isNotNull(integration.getExternalId())) {
            builder.setExternalIdMSB(getMsb(integration.getExternalId()))
                    .setExternalIdLSB(getLsb(integration.getExternalId()));
        }
        if (isNotNull(integration.getVersion())) {
            builder.setVersion(integration.getVersion());
        }

        return builder.build();
    }

    public static Integration fromProto(IntegrationProto proto) {
        var integration = new Integration(getEntityId(proto.getIntegrationIdMSB(), proto.getIntegrationIdLSB(), IntegrationId::new));
        integration.setCreatedTime(proto.getCreatedTime());
        integration.setTenantId(getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID));
        integration.setType(IntegrationType.valueOf(proto.getType()));
        integration.setName(proto.getName());
        integration.setEnabled(proto.getEnabled());
        integration.setRemote(proto.getRemote());
        integration.setAllowCreateDevicesOrAssets(proto.getAllowCreateDevicesOrAssets());
        integration.setEdgeTemplate(proto.getIsEdgeTemplate());
        integration.setDefaultConverterId(getEntityId(proto.getDefaultConverterIdMSB(), proto.getDefaultConverterIdLSB(), ConverterId::new));
        integration.setRoutingKey(proto.getRoutingKey());
        integration.setConfiguration(JacksonUtil.toJsonNode(proto.getConfiguration()));

        if (proto.hasDebugSettings()) {
            integration.setDebugSettings(JacksonUtil.fromString(proto.getDebugSettings(), DebugSettings.class));
        }

        if (proto.hasSecret()) {
            integration.setSecret(proto.getSecret());
        }

        if (proto.hasDownlinkConverterIdMSB() && proto.hasDownlinkConverterIdLSB()) {
            integration.setDownlinkConverterId(getEntityId(proto.getDownlinkConverterIdMSB(), proto.getDownlinkConverterIdLSB(), ConverterId::new));
        }
        if (proto.hasAdditionalInfo()) {
            integration.setAdditionalInfo(JacksonUtil.toJsonNode(proto.getAdditionalInfo()));
        }
        if (proto.hasExternalIdMSB() && proto.hasExternalIdLSB()) {
            integration.setExternalId(getEntityId(proto.getExternalIdMSB(), proto.getExternalIdLSB(), IntegrationId::new));
        }
        if (proto.hasVersion()) {
            integration.setVersion(proto.getVersion());
        }

        return integration;
    }

    public static ConverterProto toProto(Converter converter) {
        var builder = ConverterProto.newBuilder()
                .setConverterIdMSB(getMsb(converter.getId()))
                .setConverterIdLSB(getLsb(converter.getId()))
                .setCreatedTime(converter.getCreatedTime())
                .setTenantIdMSB(getMsb(converter.getTenantId()))
                .setTenantIdLSB(getLsb(converter.getTenantId()))
                .setType(converter.getType().name())
                .setName(converter.getName())
                .setIsEdgeTemplate(converter.isEdgeTemplate())
                .setConfiguration(JacksonUtil.toString(converter.getConfiguration()));

        if (isNotNull(converter.getIntegrationType())) {
            builder.setIntegrationType(converter.getIntegrationType().name());
        }
        if (isNotNull(converter.getDebugSettings())) {
            builder.setDebugSettings(JacksonUtil.toString(converter.getDebugSettings()));
        }
        if (isNotNull(converter.getAdditionalInfo())) {
            builder.setAdditionalInfo(JacksonUtil.toString(converter.getAdditionalInfo()));
        }
        if (isNotNull(converter.getExternalId())) {
            builder.setExternalIdMSB(getMsb(converter.getExternalId()))
                    .setExternalIdLSB(getLsb(converter.getExternalId()));
        }
        if (isNotNull(converter.getVersion())) {
            builder.setVersion(converter.getVersion());
        }
        builder.setConverterVersion(converter.getConverterVersion());
        return builder.build();
    }

    public static Converter fromProto(ConverterProto proto) {
        var converter = new Converter(getEntityId(proto.getConverterIdMSB(), proto.getConverterIdLSB(), ConverterId::new));
        converter.setCreatedTime(proto.getCreatedTime());
        converter.setTenantId(getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::fromUUID));
        converter.setType(ConverterType.valueOf(proto.getType()));
        converter.setName(proto.getName());
        converter.setEdgeTemplate(proto.getIsEdgeTemplate());
        converter.setConfiguration(JacksonUtil.toJsonNode(proto.getConfiguration()));

        if (proto.hasIntegrationType()) {
            converter.setIntegrationType(IntegrationType.valueOf(proto.getIntegrationType()));
        }
        if (proto.hasDebugSettings()) {
            converter.setDebugSettings(JacksonUtil.fromString(proto.getDebugSettings(), DebugSettings.class));
        }
        if (proto.hasAdditionalInfo()) {
            converter.setAdditionalInfo(JacksonUtil.toJsonNode(proto.getAdditionalInfo()));
        }
        if (proto.hasExternalIdMSB() && proto.hasExternalIdLSB()) {
            converter.setExternalId(getEntityId(proto.getExternalIdMSB(), proto.getExternalIdLSB(), ConverterId::new));
        }
        if (proto.hasVersion()) {
            converter.setVersion(proto.getVersion());
        }
        converter.setConverterVersion(proto.getConverterVersion());
        return converter;
    }

    public static SessionInfoProto toSessionInfo(UUID sessionId, Device device) {
        return toSessionInfo(sessionId, null, device, null, false);
    }

    public static SessionInfoProto toSessionInfo(UUID sessionId, String nodeId, Device device) {
        return toSessionInfo(sessionId, nodeId, device, null, false);
    }

    public static SessionInfoProto toSessionInfo(UUID sessionId, String nodeId, Device device, DeviceId gatewayId, boolean isGateway) {
        TransportProtos.SessionInfoProto.Builder builder = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits())
                .setIsGateway(isGateway);

        if (device.getCustomerId() != null && !device.getCustomerId().isNullUid()) {
            builder.setCustomerIdMSB(device.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(device.getCustomerId().getId().getLeastSignificantBits());
        }

        if (nodeId != null) {
            builder.setNodeId(nodeId);
        }

        if (gatewayId != null && !gatewayId.isNullUid()) {
            builder.setGatewayIdMSB(gatewayId.getId().getMostSignificantBits());
            builder.setGatewayIdLSB(gatewayId.getId().getLeastSignificantBits());
        }

        return builder.build();
    }

    public static SessionInfoProto toSessionInfo(UUID sessionId, DeviceCacheInfo deviceCacheInfo) {
        TransportProtos.SessionInfoProto.Builder builder = TransportProtos.SessionInfoProto.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setTenantIdMSB(deviceCacheInfo.tenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(deviceCacheInfo.tenantId().getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceCacheInfo.id().getMostSignificantBits())
                .setDeviceIdLSB(deviceCacheInfo.id().getLeastSignificantBits())
                .setDeviceName(deviceCacheInfo.name())
                .setDeviceType(deviceCacheInfo.type())
                .setDeviceProfileIdMSB(deviceCacheInfo.deviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(deviceCacheInfo.deviceProfileId().getId().getLeastSignificantBits());

        if (deviceCacheInfo.customerId() != null && !deviceCacheInfo.customerId().isNullUid()) {
            builder.setCustomerIdMSB(deviceCacheInfo.customerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(deviceCacheInfo.customerId().getId().getLeastSignificantBits());
        }

        return builder.build();
    }

    @Deprecated(forRemoval = true, since = "4.1")
    public static MsgProtos.TbMsgProto getTbMsgProto(TransportProtos.ToRuleEngineMsg ruleEngineMsg) throws InvalidProtocolBufferException {
        if (ruleEngineMsg.getTbMsg().isEmpty()) {
            return ruleEngineMsg.getTbMsgProto();
        } else {
            return MsgProtos.TbMsgProto.parseFrom(ruleEngineMsg.getTbMsg());
        }
    }

    @SneakyThrows
    @Deprecated(forRemoval = true, since = "4.1") // inline to TbMsg.fromProto(queueName, ruleEngineMsg.getTbMsgProto(), callback)
    public static TbMsg fromTbMsgProto(String queueName, TransportProtos.ToRuleEngineMsg ruleEngineMsg, TbMsgCallback callback) {
        return TbMsg.fromProto(queueName, getTbMsgProto(ruleEngineMsg), callback);
    }

    public static TransportProtos.EntityIdProto toProto(EntityId entityId) {
        return TransportProtos.EntityIdProto.newBuilder()
                .setEntityIdMSB(getMsb(entityId))
                .setEntityIdLSB(getLsb(entityId))
                .setType(toProto(entityId.getEntityType()))
                .build();
    }

    public static EntityId fromProto(TransportProtos.EntityIdProto entityIdProto) {
        UUID uuid = new UUID(entityIdProto.getEntityIdMSB(), entityIdProto.getEntityIdLSB());
        if (TransportProtos.EntityTypeProto.UNSPECIFIED.equals(entityIdProto.getType()) ||
                TransportProtos.EntityTypeProto.UNRECOGNIZED.equals(entityIdProto.getType())) {
            return EntityIdFactory.getByTypeAndUuid(entityIdProto.getEntityType(), uuid);
        }
        return EntityIdFactory.getByTypeAndUuid(fromProto(entityIdProto.getType()), uuid);
    }

    private static boolean isNotNull(Object obj) {
        return obj != null;
    }

    private static <T extends EntityId> T getEntityId(long msb, long lsb, Function<UUID, T> entityId) {
        return entityId.apply(new UUID(msb, lsb));
    }

    private static Long getMsb(EntityId entityId) {
        if (isNotNull(entityId)) {
            return entityId.getId().getMostSignificantBits();
        }
        return 0L;
    }

    private static Long getLsb(EntityId entityId) {
        if (isNotNull(entityId)) {
            return entityId.getId().getLeastSignificantBits();
        }
        return 0L;
    }

    private static Long checkLong(Long l) {
        return isNotNull(l) ? l : 0;
    }

}
