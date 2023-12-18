/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.ByteString;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
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
import org.thingsboard.server.common.msg.rule.engine.DeviceDeleteMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProtoUtils {

    private static final EntityType[] entityTypeByProtoNumber;

    static {
        int arraySize = Arrays.stream(EntityType.values()).mapToInt(EntityType::getProtoNumber).max().orElse(0);
        entityTypeByProtoNumber = new EntityType[arraySize + 1];
        Arrays.stream(EntityType.values()).forEach(entityType -> entityTypeByProtoNumber[entityType.getProtoNumber()] = entityType);
    }

    public static TransportProtos.ComponentLifecycleMsgProto toProto(ComponentLifecycleMsg msg) {
        return TransportProtos.ComponentLifecycleMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setEntityType(toProto(msg.getEntityId().getEntityType()))
                .setEntityIdMSB(msg.getEntityId().getId().getMostSignificantBits())
                .setEntityIdLSB(msg.getEntityId().getId().getLeastSignificantBits())
                .setEvent(TransportProtos.ComponentLifecycleEvent.forNumber(msg.getEvent().ordinal()))
                .build();
    }

    public static TransportProtos.EntityTypeProto toProto(EntityType entityType) {
        return TransportProtos.EntityTypeProto.forNumber(entityType.getProtoNumber());
    }

    public static ComponentLifecycleMsg fromProto(TransportProtos.ComponentLifecycleMsgProto proto) {
        return new ComponentLifecycleMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                EntityIdFactory.getByTypeAndUuid(fromProto(proto.getEntityType()), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB())),
                ComponentLifecycleEvent.values()[proto.getEventValue()]
        );
    }

    public static EntityType fromProto(TransportProtos.EntityTypeProto entityType) {
        return entityTypeByProtoNumber[entityType.getNumber()];
    }


    public static TransportProtos.ToEdgeSyncRequestMsgProto toProto(ToEdgeSyncRequest request) {
        return TransportProtos.ToEdgeSyncRequestMsgProto.newBuilder()
                .setTenantIdMSB(request.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(request.getTenantId().getId().getLeastSignificantBits())
                .setRequestIdMSB(request.getId().getMostSignificantBits())
                .setRequestIdLSB(request.getId().getLeastSignificantBits())
                .setEdgeIdMSB(request.getEdgeId().getId().getMostSignificantBits())
                .setEdgeIdLSB(request.getEdgeId().getId().getLeastSignificantBits())
                .build();
    }

    public static ToEdgeSyncRequest fromProto(TransportProtos.ToEdgeSyncRequestMsgProto proto) {
        return new ToEdgeSyncRequest(
                new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB()),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new EdgeId(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB()))
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
                .build();
    }

    public static FromEdgeSyncResponse fromProto(TransportProtos.FromEdgeSyncResponseMsgProto proto) {
        return new FromEdgeSyncResponse(
                new UUID(proto.getResponseIdMSB(), proto.getResponseIdLSB()),
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                new EdgeId(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB())),
                proto.getSuccess()
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
                new EdgeId(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB()))
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
            edgeId = new EdgeId(new UUID(proto.getEdgeIdMSB(), proto.getEdgeIdLSB()));
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
                TransportProtos.AttributeValueProto.Builder attributeValueBuilder = TransportProtos.AttributeValueProto.newBuilder()
                        .setLastUpdateTs(attributeKvEntry.getLastUpdateTs())
                        .setKey(attributeKvEntry.getKey());
                switch (attributeKvEntry.getDataType()) {
                    case BOOLEAN:
                        attributeKvEntry.getBooleanValue().ifPresent(attributeValueBuilder::setBoolV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getBooleanValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.BOOLEAN_V);
                        break;
                    case STRING:
                        attributeKvEntry.getStrValue().ifPresent(attributeValueBuilder::setStringV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getStrValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.STRING_V);
                        break;
                    case DOUBLE:
                        attributeKvEntry.getDoubleValue().ifPresent(attributeValueBuilder::setDoubleV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getDoubleValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.DOUBLE_V);
                        break;
                    case LONG:
                        attributeKvEntry.getLongValue().ifPresent(attributeValueBuilder::setLongV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getLongValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.LONG_V);
                        break;
                    case JSON:
                        attributeKvEntry.getJsonValue().ifPresent(attributeValueBuilder::setJsonV);
                        attributeValueBuilder.setHasV(attributeKvEntry.getJsonValue().isPresent());
                        attributeValueBuilder.setType(TransportProtos.KeyValueType.JSON_V);
                        break;
                }
                builder.addValues(attributeValueBuilder.build());
            }
        }
        return builder.build();
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
        TransportProtos.ToDeviceRpcRequestMsg proto = TransportProtos.ToDeviceRpcRequestMsg.newBuilder()
                .setMethodName(msg.getMsg().getBody().getMethod())
                .setParams(msg.getMsg().getBody().getParams())
                .setExpirationTime(msg.getMsg().getExpirationTime())
                .setRequestIdMSB(msg.getMsg().getId().getMostSignificantBits())
                .setRequestIdLSB(msg.getMsg().getId().getLeastSignificantBits())
                .setOneway(msg.getMsg().isOneway())
                .build();

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
                toDeviceRpcRequestMsg.getPersisted(), 0, "");
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
        FromDeviceRpcResponse fromDeviceRpcResponse = new FromDeviceRpcResponse(
                new UUID(proto.getRpcResponse().getRequestIdMSB(), proto.getRpcResponse().getRequestIdLSB()),
                proto.getRpcResponse().getResponse(),
                proto.getRpcResponse().getError() >= 0 ? RpcError.values()[proto.getRpcResponse().getError()] : null);
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
        if (msg instanceof DeviceEdgeUpdateMsg) {
            DeviceEdgeUpdateMsg updateMsg = (DeviceEdgeUpdateMsg) msg;
            TransportProtos.DeviceEdgeUpdateMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceEdgeUpdateMsg(proto).build();
        } else if (msg instanceof DeviceNameOrTypeUpdateMsg) {
            DeviceNameOrTypeUpdateMsg updateMsg = (DeviceNameOrTypeUpdateMsg) msg;
            TransportProtos.DeviceNameOrTypeUpdateMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceNameOrTypeMsg(proto).build();
        } else if (msg instanceof DeviceAttributesEventNotificationMsg) {
            DeviceAttributesEventNotificationMsg updateMsg = (DeviceAttributesEventNotificationMsg) msg;
            TransportProtos.DeviceAttributesEventMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceAttributesEventMsg(proto).build();
        } else if (msg instanceof DeviceCredentialsUpdateNotificationMsg) {
            DeviceCredentialsUpdateNotificationMsg updateMsg = (DeviceCredentialsUpdateNotificationMsg) msg;
            TransportProtos.DeviceCredentialsUpdateMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setDeviceCredentialsUpdateMsg(proto).build();
        } else if (msg instanceof ToDeviceRpcRequestActorMsg) {
            ToDeviceRpcRequestActorMsg updateMsg = (ToDeviceRpcRequestActorMsg) msg;
            TransportProtos.ToDeviceRpcRequestActorMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setToDeviceRpcRequestMsg(proto).build();
        } else if (msg instanceof FromDeviceRpcResponseActorMsg) {
            FromDeviceRpcResponseActorMsg updateMsg = (FromDeviceRpcResponseActorMsg) msg;
            TransportProtos.FromDeviceRpcResponseActorMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setFromDeviceRpcResponseMsg(proto).build();
        } else if (msg instanceof RemoveRpcActorMsg) {
            RemoveRpcActorMsg updateMsg = (RemoveRpcActorMsg) msg;
            TransportProtos.RemoveRpcActorMsgProto proto = toProto(updateMsg);
            return TransportProtos.ToDeviceActorNotificationMsgProto.newBuilder().setRemoveRpcActorMsg(proto).build();
        } else if (msg instanceof DeviceDeleteMsg) {
            DeviceDeleteMsg updateMsg = (DeviceDeleteMsg) msg;
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
            boolean hasValue = kvEntry.getHasV();
            KvEntry entry = null;
            switch (kvEntry.getType()) {
                case BOOLEAN_V:
                    entry = new BooleanDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getBoolV() : null);
                    break;
                case LONG_V:
                    entry = new LongDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getLongV() : null);
                    break;
                case DOUBLE_V:
                    entry = new DoubleDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getDoubleV() : null);
                    break;
                case STRING_V:
                    entry = new StringDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getStringV() : null);
                    break;
                case JSON_V:
                    entry = new JsonDataEntry(kvEntry.getKey(), hasValue ? kvEntry.getJsonV() : null);
                    break;
            }
            result.add(new BaseAttributeKvEntry(kvEntry.getLastUpdateTs(), entry));
        }
        return result;
    }

    public static TransportProtos.DeviceProto toDeviceProto(Device device) {
        var builder = TransportProtos.DeviceProto.newBuilder()
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(getMsb(device.getCustomerId()))
                .setCustomerIdLSB(getLsb(device.getCustomerId()))
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setCreatedTime(device.getCreatedTime())
                .setDeviceName(device.getName())
                .setDeviceLabel(toProtoString(device.getLabel()))
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits())
                .setAdditionalInfo(JacksonUtil.toString(device.getAdditionalInfo()))
                .setFirmwareIdMSB(getMsb(device.getFirmwareId()))
                .setFirmwareIdLSB(getLsb(device.getFirmwareId()))
                .setSoftwareIdMSB(getMsb(device.getSoftwareId()))
                .setSoftwareIdLSB(getLsb(device.getSoftwareId()))
                .setExternalIdMSB(getMsb(device.getExternalId()))
                .setExternalIdLSB(getLsb(device.getExternalId()));

        if (device.getDeviceDataBytes() != null) {
            builder.setDeviceData(ByteString.copyFrom(device.getDeviceDataBytes()));
        }

        return builder.build();
    }

    public static Device fromDeviceProto(TransportProtos.DeviceProto proto) {
        Device device = new Device(getEntityId(proto.getDeviceIdMSB(), proto.getDeviceIdLSB(), DeviceId::new));
        device.setCreatedTime(proto.getCreatedTime());
        device.setTenantId(getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::new));
        device.setCustomerId(getEntityId(proto.getCustomerIdMSB(), proto.getCustomerIdLSB(), CustomerId::new));
        device.setName(proto.getDeviceName());
        device.setLabel(fromProtoString(proto.getDeviceLabel()));
        device.setType(proto.getDeviceType());
        device.setDeviceProfileId(getEntityId(proto.getDeviceProfileIdMSB(), proto.getDeviceProfileIdLSB(), DeviceProfileId::new));
        device.setAdditionalInfo(JacksonUtil.toJsonNode(proto.getAdditionalInfo()));
        device.setFirmwareId(getEntityId(proto.getFirmwareIdMSB(), proto.getFirmwareIdLSB(), OtaPackageId::new));
        device.setSoftwareId(getEntityId(proto.getSoftwareIdMSB(), proto.getSoftwareIdLSB(), OtaPackageId::new));
        device.setExternalId(getEntityId(proto.getExternalIdMSB(), proto.getExternalIdLSB(), DeviceId::new));
        device.setDeviceDataBytes(proto.getDeviceData().toByteArray());
        return device;
    }

    public static TransportProtos.DeviceProfileProto toDeviceProfileProto(DeviceProfile deviceProfile) {
        return TransportProtos.DeviceProfileProto.newBuilder()
                .setTenantIdMSB(getMsb(deviceProfile.getTenantId()))
                .setTenantIdLSB(getLsb(deviceProfile.getTenantId()))
                .setDeviceProfileIdMSB(getMsb(deviceProfile.getId()))
                .setDeviceProfileIdLSB(getLsb(deviceProfile.getId()))
                .setCreatedTime(deviceProfile.getCreatedTime())
                .setName(deviceProfile.getName())
                .setIsDefault(deviceProfile.isDefault())
                .setType(deviceProfile.getType().name())
                .setTransportType(deviceProfile.getTransportType().name())
                .setProvisionType(deviceProfile.getProvisionType().name())
                .setDeviceProfileData(ByteString.copyFrom(deviceProfile.getProfileDataBytes()))
                .setDescription(toProtoString(deviceProfile.getDescription()))
                .setImage(toProtoString(deviceProfile.getImage()))
                .setDefaultRuleChainIdMSB(getMsb(deviceProfile.getDefaultRuleChainId()))
                .setDefaultRuleChainIdLSB(getMsb(deviceProfile.getDefaultRuleChainId()))
                .setDefaultDashboardIdMSB(getMsb(deviceProfile.getDefaultDashboardId()))
                .setDefaultDashboardIdLSB(getLsb(deviceProfile.getDefaultDashboardId()))
                .setDefaultQueueName(toProtoString(deviceProfile.getDefaultQueueName()))
                .setProvisionDeviceKey(toProtoString(deviceProfile.getProvisionDeviceKey()))
                .setFirmwareIdMSB(getMsb(deviceProfile.getFirmwareId()))
                .setFirmwareIdLSB(getLsb(deviceProfile.getFirmwareId()))
                .setSoftwareIdMSB(getMsb(deviceProfile.getSoftwareId()))
                .setSoftwareIdLSB(getLsb(deviceProfile.getSoftwareId()))
                .setDefaultEdgeRuleChainIdMSB(getMsb(deviceProfile.getDefaultEdgeRuleChainId()))
                .setDefaultEdgeRuleChainIdLSB(getLsb(deviceProfile.getDefaultEdgeRuleChainId()))
                .setExternalIdMSB(getMsb(deviceProfile.getExternalId()))
                .setExternalIdLSB(getLsb(deviceProfile.getExternalId())).build();
    }

    public static DeviceProfile fromDeviceProfileProto(TransportProtos.DeviceProfileProto proto) {
        DeviceProfile deviceProfile = new DeviceProfile(getEntityId(proto.getDeviceProfileIdMSB(), proto.getDeviceProfileIdLSB(), DeviceProfileId::new));
        deviceProfile.setCreatedTime(proto.getCreatedTime());
        deviceProfile.setTenantId(getEntityId(proto.getTenantIdMSB(), proto.getTenantIdLSB(), TenantId::new));
        deviceProfile.setName(proto.getName());
        deviceProfile.setDefault(proto.getIsDefault());
        deviceProfile.setType(DeviceProfileType.valueOf(proto.getType()));
        deviceProfile.setTransportType(DeviceTransportType.valueOf(proto.getTransportType()));
        deviceProfile.setProvisionType(DeviceProfileProvisionType.valueOf(proto.getProvisionType()));
        deviceProfile.setProfileDataBytes(proto.getDeviceProfileData().toByteArray());
        deviceProfile.setDescription(fromProtoString(proto.getDescription()));
        deviceProfile.setImage(fromProtoString(proto.getImage()));
        deviceProfile.setDefaultRuleChainId(getEntityId(proto.getDefaultRuleChainIdMSB(), proto.getDefaultRuleChainIdLSB(), RuleChainId::new));
        deviceProfile.setDefaultDashboardId(getEntityId(proto.getDefaultDashboardIdMSB(), proto.getDefaultDashboardIdLSB(), DashboardId::new));
        deviceProfile.setDefaultQueueName(fromProtoString(proto.getDefaultQueueName()));
        deviceProfile.setProvisionDeviceKey(fromProtoString(proto.getProvisionDeviceKey()));
        deviceProfile.setFirmwareId(getEntityId(proto.getFirmwareIdMSB(), proto.getFirmwareIdLSB(), OtaPackageId::new));
        deviceProfile.setSoftwareId(getEntityId(proto.getSoftwareIdMSB(), proto.getSoftwareIdLSB(), OtaPackageId::new));
        deviceProfile.setDefaultEdgeRuleChainId(getEntityId(proto.getDefaultEdgeRuleChainIdMSB(), proto.getDefaultEdgeRuleChainIdLSB(), RuleChainId::new));
        deviceProfile.setExternalId(getEntityId(proto.getExternalIdMSB(), proto.getExternalIdLSB(), DeviceProfileId::new));

        return deviceProfile;
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

        PowerSavingConfiguration psmConfiguration = null;
        switch (device.getDeviceData().getTransportConfiguration().getType()) {
            case LWM2M:
                psmConfiguration = (Lwm2mDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
                break;
            case COAP:
                psmConfiguration = (CoapDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
                break;
        }

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

    private static <T extends EntityId> T getEntityId(long msb, long lsb, Function<UUID, T> entityId) {
        if (msb != 0 || lsb != 0) {
            return entityId.apply(new UUID(msb, lsb));
        }
        return null;
    }

    private static String toProtoString(String str) {
        return str != null ? str : "";
    }

    private static String fromProtoString(String str) {
        return StringUtils.isNotEmpty(str) ? str : null;
    }

    private static Long getMsb(EntityId entityId) {
        if (entityId != null) {
            return entityId.getId().getMostSignificantBits();
        }
        return 0L;
    }

    private static Long getLsb(EntityId entityId) {
        if (entityId != null) {
            return entityId.getId().getLeastSignificantBits();
        }
        return 0L;
    }

    private static Long checkLong(Long l) {
        return l != null ? l : 0;
    }
}
