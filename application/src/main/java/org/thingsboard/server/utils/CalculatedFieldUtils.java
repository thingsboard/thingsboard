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
package org.thingsboard.server.utils;

import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.geofencing.GeofencingPresenceStatus;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos.AlarmRuleStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.AlarmStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldEntityCtxIdProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldIdProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.EntityIdProto;
import org.thingsboard.server.gen.transport.TransportProtos.GeofencingArgumentProto;
import org.thingsboard.server.gen.transport.TransportProtos.GeofencingZoneProto;
import org.thingsboard.server.gen.transport.TransportProtos.SingleValueArgumentProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsDoubleValProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsRollingArgumentProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsValueProto;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SimpleCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.TsRollingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.alarm.AlarmCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.alarm.AlarmRuleState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.geofencing.GeofencingZoneState;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.propagation.PropagationCalculatedFieldState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.cf.configuration.PropagationCalculatedFieldConfiguration.PROPAGATION_CONFIG_ARGUMENT;

public class CalculatedFieldUtils {

    public static CalculatedFieldIdProto toProto(CalculatedFieldId cfId) {
        return CalculatedFieldIdProto.newBuilder()
                .setCalculatedFieldIdMSB(cfId.getId().getMostSignificantBits())
                .setCalculatedFieldIdLSB(cfId.getId().getLeastSignificantBits())
                .build();
    }

    public static CalculatedFieldEntityCtxIdProto toProto(CalculatedFieldEntityCtxId ctxId) {
        return CalculatedFieldEntityCtxIdProto.newBuilder()
                .setTenantIdMSB(ctxId.tenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(ctxId.tenantId().getId().getLeastSignificantBits())
                .setCalculatedFieldIdMSB(ctxId.cfId().getId().getMostSignificantBits())
                .setCalculatedFieldIdLSB(ctxId.cfId().getId().getLeastSignificantBits())
                .setEntityType(ctxId.entityId().getEntityType().name())
                .setEntityIdMSB(ctxId.entityId().getId().getMostSignificantBits())
                .setEntityIdLSB(ctxId.entityId().getId().getLeastSignificantBits())
                .build();
    }

    public static CalculatedFieldEntityCtxId fromProto(CalculatedFieldEntityCtxIdProto ctxIdProto) {
        TenantId tenantId = TenantId.fromUUID(new UUID(ctxIdProto.getTenantIdMSB(), ctxIdProto.getTenantIdLSB()));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(ctxIdProto.getEntityType(), new UUID(ctxIdProto.getEntityIdMSB(), ctxIdProto.getEntityIdLSB()));
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(ctxIdProto.getCalculatedFieldIdMSB(), ctxIdProto.getCalculatedFieldIdLSB()));
        return new CalculatedFieldEntityCtxId(tenantId, calculatedFieldId, entityId);
    }

    public static CalculatedFieldStateProto toProto(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state) {
        CalculatedFieldStateProto.Builder builder = CalculatedFieldStateProto.newBuilder()
                .setId(toProto(stateId))
                .setType(state.getType().name());

        state.getArguments().forEach((argName, argEntry) -> {
            switch (argEntry.getType()) {
                case SINGLE_VALUE -> builder.addSingleValueArguments(toSingleValueArgumentProto(argName, (SingleValueArgumentEntry) argEntry));
                case TS_ROLLING -> builder.addRollingValueArguments(toRollingArgumentProto(argName, (TsRollingArgumentEntry) argEntry));
                case GEOFENCING -> builder.addGeofencingArguments(toGeofencingArgumentProto(argName, (GeofencingArgumentEntry) argEntry));
                case PROPAGATION -> builder.addAllPropagationEntityIds(toPropagationEntityIdsProto((PropagationArgumentEntry) argEntry));
            }
        });
        if (state instanceof AlarmCalculatedFieldState alarmState) {
            AlarmStateProto.Builder alarmStateProto = AlarmStateProto.newBuilder();
            alarmState.getCreateRuleStates().forEach((severity, ruleState) -> {
                alarmStateProto.addCreateRuleStates(toAlarmRuleStateProto(ruleState));
            });
            if (alarmState.getClearRuleState() != null) {
                alarmStateProto.setClearRuleState(toAlarmRuleStateProto(alarmState.getClearRuleState()));
            }
        }
        return builder.build();
    }

    private static List<EntityIdProto> toPropagationEntityIdsProto(PropagationArgumentEntry argEntry) {
        return argEntry.getPropagationEntityIds().stream().map(ProtoUtils::toProto).collect(Collectors.toList());
    }

    private static AlarmRuleStateProto toAlarmRuleStateProto(AlarmRuleState ruleState) {
        return AlarmRuleStateProto.newBuilder()
                .setSeverity(Optional.ofNullable(ruleState.getSeverity()).map(Enum::name).orElse(""))
                .setLastEventTs(ruleState.getLastEventTs())
                .setDuration(ruleState.getDuration())
                .setEventCount(ruleState.getEventCount())
                .build();
    }

    public static SingleValueArgumentProto toSingleValueArgumentProto(String argName, SingleValueArgumentEntry entry) {
        SingleValueArgumentProto.Builder builder = SingleValueArgumentProto.newBuilder()
                .setArgName(argName);

        if (entry.getKvEntryValue() != null) {
            builder.setValue(KvProtoUtil.toTsValueProto(entry.getTs(), entry.getKvEntryValue()));
        }

        Optional.ofNullable(entry.getVersion()).ifPresent(builder::setVersion);

        return builder.build();
    }

    public static TsRollingArgumentProto toRollingArgumentProto(String argName, TsRollingArgumentEntry entry) {
        TsRollingArgumentProto.Builder builder = TsRollingArgumentProto.newBuilder()
                .setKey(argName)
                .setLimit(entry.getLimit())
                .setTimeWindow(entry.getTimeWindow());

        entry.getTsRecords().forEach((ts, value) -> builder.addTsValue(TsDoubleValProto.newBuilder().setTs(ts).setValue(value).build()));

        return builder.build();
    }

    private static GeofencingArgumentProto toGeofencingArgumentProto(String argName, GeofencingArgumentEntry geofencingArgumentEntry) {
        Map<EntityId, GeofencingZoneState> zoneStates = geofencingArgumentEntry.getZoneStates();
        GeofencingArgumentProto.Builder builder = GeofencingArgumentProto.newBuilder()
                .setArgName(argName);
        zoneStates.forEach((entityId, zoneState) ->
                builder.addZones(toGeofencingZoneProto(entityId, zoneState)));
        return builder.build();
    }

    private static GeofencingZoneProto toGeofencingZoneProto(EntityId entityId, GeofencingZoneState zoneState) {
        GeofencingZoneProto.Builder builder = GeofencingZoneProto.newBuilder()
                .setZoneId(ProtoUtils.toProto(entityId))
                .setTs(zoneState.getTs())
                .setVersion(zoneState.getVersion())
                .setPerimeterDefinition(JacksonUtil.toString(zoneState.getPerimeterDefinition()));
        if (zoneState.getLastPresence() != null) {
            builder.setInside(zoneState.getLastPresence().equals(GeofencingPresenceStatus.INSIDE));
        }
        return builder.build();
    }

    public static CalculatedFieldState fromProto(CalculatedFieldEntityCtxId id, CalculatedFieldStateProto proto) {
        if (StringUtils.isEmpty(proto.getType())) {
            return null;
        }

        CalculatedFieldType type = CalculatedFieldType.valueOf(proto.getType());

        CalculatedFieldState state = switch (type) {
            case SIMPLE -> new SimpleCalculatedFieldState(id.entityId());
            case SCRIPT -> new ScriptCalculatedFieldState(id.entityId());
            case GEOFENCING -> new GeofencingCalculatedFieldState(id.entityId());
            case ALARM -> new AlarmCalculatedFieldState(id.entityId());
            case PROPAGATION -> new PropagationCalculatedFieldState(id.entityId());
        };

        proto.getSingleValueArgumentsList().forEach(argProto ->
                state.getArguments().put(argProto.getArgName(), fromSingleValueArgumentProto(argProto)));

        switch (type) {
            case SCRIPT -> {
                proto.getRollingValueArgumentsList().forEach(argProto ->
                        state.getArguments().put(argProto.getKey(), fromRollingArgumentProto(argProto)));
            }
            case GEOFENCING -> {
                proto.getGeofencingArgumentsList().forEach(argProto ->
                        state.getArguments().put(argProto.getArgName(), fromGeofencingArgumentProto(argProto)));
            }
            case ALARM -> {
                AlarmCalculatedFieldState alarmState = (AlarmCalculatedFieldState) state;
                AlarmStateProto alarmStateProto = proto.getAlarmState();
                for (AlarmRuleStateProto ruleStateProto : alarmStateProto.getCreateRuleStatesList()) {
                    AlarmSeverity severity = StringUtils.isNotEmpty(ruleStateProto.getSeverity()) ? AlarmSeverity.valueOf(ruleStateProto.getSeverity()) : null;
                    AlarmRuleState ruleState = new AlarmRuleState(severity, null, alarmState);
                    ruleState.setLastEventTs(ruleStateProto.getLastEventTs());
                    ruleState.setDuration(ruleStateProto.getDuration());
                    ruleState.setEventCount(ruleStateProto.getEventCount());
                    alarmState.getCreateRuleStates().put(severity, ruleState);
                }
            }
            case PROPAGATION -> {
                List<EntityId> propagationEntityIds = proto.getPropagationEntityIdsList().stream().map(ProtoUtils::fromProto).toList();
                state.getArguments().put(PROPAGATION_CONFIG_ARGUMENT, new PropagationArgumentEntry(propagationEntityIds));
            }
        }

        return state;
    }

    public static SingleValueArgumentEntry fromSingleValueArgumentProto(SingleValueArgumentProto proto) {
        if (!proto.hasValue()) {
            return new SingleValueArgumentEntry();
        }
        TsValueProto tsValueProto = proto.getValue();
        return new SingleValueArgumentEntry(
                tsValueProto.getTs(),
                (BasicKvEntry) KvProtoUtil.fromTsValueProto(proto.getArgName(), tsValueProto),
                proto.getVersion()
        );
    }

    public static TsRollingArgumentEntry fromRollingArgumentProto(TsRollingArgumentProto proto) {
        TreeMap<Long, Double> tsRecords = new TreeMap<>();
        proto.getTsValueList().forEach(tsValueProto -> tsRecords.put(tsValueProto.getTs(), tsValueProto.getValue()));
        return new TsRollingArgumentEntry(tsRecords, proto.getLimit(), proto.getTimeWindow());
    }


    private static ArgumentEntry fromGeofencingArgumentProto(GeofencingArgumentProto proto) {
        Map<EntityId, GeofencingZoneState> zoneStates = proto.getZonesList()
                .stream()
                .map(GeofencingZoneState::new)
                .collect(Collectors.toMap(GeofencingZoneState::getZoneId, Function.identity()));
        GeofencingArgumentEntry geofencingArgumentEntry = new GeofencingArgumentEntry();
        geofencingArgumentEntry.setZoneStates(zoneStates);
        return geofencingArgumentEntry;
    }

}
