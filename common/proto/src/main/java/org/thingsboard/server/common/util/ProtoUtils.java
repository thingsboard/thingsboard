/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
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
}
