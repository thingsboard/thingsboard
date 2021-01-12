/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.subscription;

import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;
import org.thingsboard.server.gen.transport.TransportProtos.SubscriptionMgrMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAttributeSubscriptionProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAttributeUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAttributeDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbSubscriptionCloseProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbSubscriptionKeyStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbSubscriptionProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbSubscriptionUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbTimeSeriesSubscriptionProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbTimeSeriesUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAlarmUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAlarmDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.EntityKeyTypeProto;
import org.thingsboard.server.service.telemetry.sub.AlarmSubscriptionUpdate;
import org.thingsboard.server.service.telemetry.sub.SubscriptionErrorCode;
import org.thingsboard.server.service.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class TbSubscriptionUtils {

    public static ToCoreMsg toNewSubscriptionProto(TbSubscription subscription) {
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        TbSubscriptionProto subscriptionProto = TbSubscriptionProto.newBuilder()
                .setServiceId(subscription.getServiceId())
                .setSessionId(subscription.getSessionId())
                .setSubscriptionId(subscription.getSubscriptionId())
                .setTenantIdMSB(subscription.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(subscription.getTenantId().getId().getLeastSignificantBits())
                .setEntityType(subscription.getEntityId().getEntityType().name())
                .setEntityIdMSB(subscription.getEntityId().getId().getMostSignificantBits())
                .setEntityIdLSB(subscription.getEntityId().getId().getLeastSignificantBits()).build();

        switch (subscription.getType()) {
            case TIMESERIES:
                TbTimeseriesSubscription tSub = (TbTimeseriesSubscription) subscription;
                TbTimeSeriesSubscriptionProto.Builder tSubProto = TbTimeSeriesSubscriptionProto.newBuilder()
                        .setSub(subscriptionProto)
                        .setAllKeys(tSub.isAllKeys());
                tSub.getKeyStates().forEach((key, value) -> tSubProto.addKeyStates(
                        TbSubscriptionKeyStateProto.newBuilder()
                                .setKey(key)
                                .setTs(value.getLastUpdatedTs())
                                .setKeyType(EntityKeyTypeProto.TIME_SERIES)
                                .setRestrictConversion(value.isRestrictConversion())
                                .build()));
                tSubProto.setStartTime(tSub.getStartTime());
                tSubProto.setEndTime(tSub.getEndTime());
                msgBuilder.setTelemetrySub(tSubProto.build());
                break;
            case ATTRIBUTES:
                TbAttributeSubscription aSub = (TbAttributeSubscription) subscription;
                TbAttributeSubscriptionProto.Builder aSubProto = TbAttributeSubscriptionProto.newBuilder()
                        .setSub(subscriptionProto)
                        .setAllKeys(aSub.isAllKeys())
                        .setScope(aSub.getScope().name());
                aSub.getKeyStates().forEach((key, value) -> aSubProto.addKeyStates(
                        TbSubscriptionKeyStateProto.newBuilder()
                                .setKey(key)
                                .setTs(value.getLastUpdatedTs())
                                .setKeyType(toEntityAttributeKeyTypeProto(value.getEntityKeyType()))
                                .setRestrictConversion(value.isRestrictConversion())
                                .build()));
                msgBuilder.setAttributeSub(aSubProto.build());
                break;
            case ALARMS:
                TbAlarmsSubscription alarmSub = (TbAlarmsSubscription) subscription;
                TransportProtos.TbAlarmSubscriptionProto.Builder alarmSubProto = TransportProtos.TbAlarmSubscriptionProto.newBuilder()
                        .setSub(subscriptionProto)
                        .setTs(alarmSub.getTs());
                msgBuilder.setAlarmSub(alarmSubProto.build());
                break;
        }
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    private static TransportProtos.EntityKeyTypeProto toEntityAttributeKeyTypeProto(EntityKeyType entityKeyType) {
        switch (entityKeyType) {
            case ATTRIBUTE:
                return TransportProtos.EntityKeyTypeProto.ATTRIBUTE;
            case CLIENT_ATTRIBUTE:
                return TransportProtos.EntityKeyTypeProto.CLIENT_ATTRIBUTE;
            case SHARED_ATTRIBUTE:
                return TransportProtos.EntityKeyTypeProto.SERVER_ATTRIBUTE;
            case SERVER_ATTRIBUTE:
                return TransportProtos.EntityKeyTypeProto.SHARED_ATTRIBUTE;
        }
        return null;
    }

    private static EntityKeyType fromEntityKeyTypeProto(EntityKeyTypeProto entityKeyTypeProto) {
        switch (entityKeyTypeProto) {
            case ATTRIBUTE:
                return EntityKeyType.ATTRIBUTE;
            case CLIENT_ATTRIBUTE:
                return EntityKeyType.CLIENT_ATTRIBUTE;
            case SHARED_ATTRIBUTE:
                return EntityKeyType.SERVER_ATTRIBUTE;
            case SERVER_ATTRIBUTE:
                return EntityKeyType.SHARED_ATTRIBUTE;
            case TIME_SERIES:
                return EntityKeyType.TIME_SERIES;
            case ENTITY_FIELD:
                return EntityKeyType.ENTITY_FIELD;
            case ALARM_FIELD:
                return EntityKeyType.ALARM_FIELD;
        }
        return null;
    }

    public static ToCoreMsg toCloseSubscriptionProto(TbSubscription subscription) {
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        TbSubscriptionCloseProto closeProto = TbSubscriptionCloseProto.newBuilder()
                .setSessionId(subscription.getSessionId())
                .setSubscriptionId(subscription.getSubscriptionId()).build();
        msgBuilder.setSubClose(closeProto);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static TbSubscription fromProto(TbAttributeSubscriptionProto attributeSub) {
        TbSubscriptionProto subProto = attributeSub.getSub();
        TbAttributeSubscription.TbAttributeSubscriptionBuilder builder = TbAttributeSubscription.builder()
                .serviceId(subProto.getServiceId())
                .sessionId(subProto.getSessionId())
                .subscriptionId(subProto.getSubscriptionId())
                .entityId(EntityIdFactory.getByTypeAndUuid(subProto.getEntityType(), new UUID(subProto.getEntityIdMSB(), subProto.getEntityIdLSB())))
                .tenantId(new TenantId(new UUID(subProto.getTenantIdMSB(), subProto.getTenantIdLSB())));

        builder.scope(TbAttributeSubscriptionScope.valueOf(attributeSub.getScope()));
        builder.allKeys(attributeSub.getAllKeys());
        Map<String, TbSubscriptionKeyState> keyStates = new HashMap<>();
        attributeSub.getKeyStatesList().forEach(ksProto -> keyStates.put(ksProto.getKey(), new TbSubscriptionKeyState(ksProto.getTs(), fromEntityKeyTypeProto(ksProto.getKeyType()), ksProto.getRestrictConversion())));
        builder.keyStates(keyStates);
        return builder.build();
    }

    public static TbSubscription fromProto(TbTimeSeriesSubscriptionProto telemetrySub) {
        TbSubscriptionProto subProto = telemetrySub.getSub();
        TbTimeseriesSubscription.TbTimeseriesSubscriptionBuilder builder = TbTimeseriesSubscription.builder()
                .serviceId(subProto.getServiceId())
                .sessionId(subProto.getSessionId())
                .subscriptionId(subProto.getSubscriptionId())
                .entityId(EntityIdFactory.getByTypeAndUuid(subProto.getEntityType(), new UUID(subProto.getEntityIdMSB(), subProto.getEntityIdLSB())))
                .tenantId(new TenantId(new UUID(subProto.getTenantIdMSB(), subProto.getTenantIdLSB())));

        builder.allKeys(telemetrySub.getAllKeys());
        Map<String, TbSubscriptionKeyState> keyStates = new HashMap<>();
        telemetrySub.getKeyStatesList().forEach(ksProto -> keyStates.put(ksProto.getKey(), new TbSubscriptionKeyState(ksProto.getTs(), fromEntityKeyTypeProto(ksProto.getKeyType()), ksProto.getRestrictConversion())));
        builder.startTime(telemetrySub.getStartTime());
        builder.endTime(telemetrySub.getEndTime());
        builder.keyStates(keyStates);
        return builder.build();
    }

    public static TbSubscription fromProto(TransportProtos.TbAlarmSubscriptionProto alarmSub) {
        TbSubscriptionProto subProto = alarmSub.getSub();
        TbAlarmsSubscription.TbAlarmsSubscriptionBuilder builder = TbAlarmsSubscription.builder()
                .serviceId(subProto.getServiceId())
                .sessionId(subProto.getSessionId())
                .subscriptionId(subProto.getSubscriptionId())
                .entityId(EntityIdFactory.getByTypeAndUuid(subProto.getEntityType(), new UUID(subProto.getEntityIdMSB(), subProto.getEntityIdLSB())))
                .tenantId(new TenantId(new UUID(subProto.getTenantIdMSB(), subProto.getTenantIdLSB())));
        builder.ts(alarmSub.getTs());
        return builder.build();
    }

    public static TelemetrySubscriptionUpdate fromProto(TbSubscriptionUpdateProto proto) {
        if (proto.getErrorCode() > 0) {
            return new TelemetrySubscriptionUpdate(proto.getSubscriptionId(), SubscriptionErrorCode.forCode(proto.getErrorCode()), proto.getErrorMsg());
        } else {
            Map<String, List<Object>> data = new TreeMap<>();
            proto.getDataList().forEach(v -> {
                List<Object> values = data.computeIfAbsent(v.getKey(), k -> new ArrayList<>());
                for (int i = 0; i < v.getTsCount(); i++) {
                    Object[] value = new Object[2];
                    value[0] = v.getTs(i);
                    value[1] = v.getValue(i);
                    values.add(value);
                }
            });
            return new TelemetrySubscriptionUpdate(proto.getSubscriptionId(), data);
        }
    }

    public static AlarmSubscriptionUpdate fromProto(TransportProtos.TbAlarmSubscriptionUpdateProto proto) {
        if (proto.getErrorCode() > 0) {
            return new AlarmSubscriptionUpdate(proto.getSubscriptionId(), SubscriptionErrorCode.forCode(proto.getErrorCode()), proto.getErrorMsg());
        } else {
            Alarm alarm = JacksonUtil.fromString(proto.getAlarm(), Alarm.class);
            return new AlarmSubscriptionUpdate(proto.getSubscriptionId(), alarm);
        }
    }


    public static ToCoreMsg toTimeseriesUpdateProto(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts) {
        TbTimeSeriesUpdateProto.Builder builder = TbTimeSeriesUpdateProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        ts.forEach(v -> builder.addData(toKeyValueProto(v.getTs(), v).build()));
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setTsUpdate(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static ToCoreMsg toAttributesUpdateProto(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        TbAttributeUpdateProto.Builder builder = TbAttributeUpdateProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setScope(scope);
        attributes.forEach(v -> builder.addData(toKeyValueProto(v.getLastUpdateTs(), v).build()));

        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAttrUpdate(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static ToCoreMsg toAttributesDeleteProto(TenantId tenantId, EntityId entityId, String scope, List<String> keys) {
        TbAttributeDeleteProto.Builder builder = TbAttributeDeleteProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setScope(scope);
        builder.addAllKeys(keys);

        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAttrDelete(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }


    private static TsKvProto.Builder toKeyValueProto(long ts, KvEntry attr) {
        KeyValueProto.Builder dataBuilder = KeyValueProto.newBuilder();
        dataBuilder.setKey(attr.getKey());
        dataBuilder.setType(KeyValueType.forNumber(attr.getDataType().ordinal()));
        switch (attr.getDataType()) {
            case BOOLEAN:
                attr.getBooleanValue().ifPresent(dataBuilder::setBoolV);
                break;
            case LONG:
                attr.getLongValue().ifPresent(dataBuilder::setLongV);
                break;
            case DOUBLE:
                attr.getDoubleValue().ifPresent(dataBuilder::setDoubleV);
                break;
            case JSON:
                attr.getJsonValue().ifPresent(dataBuilder::setJsonV);
                break;
            case STRING:
                attr.getStrValue().ifPresent(dataBuilder::setStringV);
                break;
        }
        return TsKvProto.newBuilder().setTs(ts).setKv(dataBuilder);
    }

    public static EntityId toEntityId(String entityType, long entityIdMSB, long entityIdLSB) {
        return EntityIdFactory.getByTypeAndUuid(entityType, new UUID(entityIdMSB, entityIdLSB));
    }

    public static List<TsKvEntry> toTsKvEntityList(List<TsKvProto> dataList) {
        List<TsKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BasicTsKvEntry(proto.getTs(), getKvEntry(proto.getKv()))));
        return result;
    }

    public static List<AttributeKvEntry> toAttributeKvList(List<TsKvProto> dataList) {
        List<AttributeKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BaseAttributeKvEntry(getKvEntry(proto.getKv()), proto.getTs())));
        return result;
    }

    private static KvEntry getKvEntry(KeyValueProto proto) {
        KvEntry entry = null;
        DataType type = DataType.values()[proto.getType().getNumber()];
        switch (type) {
            case BOOLEAN:
                entry = new BooleanDataEntry(proto.getKey(), proto.getBoolV());
                break;
            case LONG:
                entry = new LongDataEntry(proto.getKey(), proto.getLongV());
                break;
            case DOUBLE:
                entry = new DoubleDataEntry(proto.getKey(), proto.getDoubleV());
                break;
            case STRING:
                entry = new StringDataEntry(proto.getKey(), proto.getStringV());
                break;
            case JSON:
                entry = new JsonDataEntry(proto.getKey(), proto.getJsonV());
                break;
        }
        return entry;
    }

    public static ToCoreMsg toAlarmUpdateProto(TenantId tenantId, EntityId entityId, Alarm alarm) {
        TbAlarmUpdateProto.Builder builder = TbAlarmUpdateProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setAlarm(JacksonUtil.toString(alarm));
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAlarmUpdate(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    public static ToCoreMsg toAlarmDeletedProto(TenantId tenantId, EntityId entityId, Alarm alarm) {
        TbAlarmDeleteProto.Builder builder = TbAlarmDeleteProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setAlarm(JacksonUtil.toString(alarm));
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAlarmDelete(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }
}
