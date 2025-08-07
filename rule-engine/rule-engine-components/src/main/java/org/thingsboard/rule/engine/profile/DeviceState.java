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
package org.thingsboard.rule.engine.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.profile.state.PersistedAlarmState;
import org.thingsboard.rule.engine.profile.state.PersistedDeviceState;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpecType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.exception.ApiUsageLimitsExceededException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.common.data.msg.TbMsgType.ACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM_ACK;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM_CLEAR;
import static org.thingsboard.server.common.data.msg.TbMsgType.ALARM_DELETE;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_DELETED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_ASSIGNED;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_UNASSIGNED;
import static org.thingsboard.server.common.data.msg.TbMsgType.INACTIVITY_EVENT;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;
import static org.thingsboard.server.common.data.msg.TbMsgType.TIMESERIES_UPDATED;

@Slf4j
class DeviceState {

    private final boolean persistState;
    private final DeviceId deviceId;
    private final ProfileState deviceProfile;
    private RuleNodeState state;
    private PersistedDeviceState pds;
    private DataSnapshot latestValues;
    private final ConcurrentMap<String, AlarmState> alarmStates = new ConcurrentHashMap<>();
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;

    DeviceState(TbContext ctx, TbDeviceProfileNodeConfiguration config, DeviceId deviceId, ProfileState deviceProfile, RuleNodeState state) {
        this.persistState = config.isPersistAlarmRulesState();
        this.deviceId = deviceId;
        this.deviceProfile = deviceProfile;

        if (hasDurationRulesWithDynamicValueFromCurrentDevice(deviceProfile)) {
            latestValues = fetchLatestValues(ctx, deviceId);
        }

        this.dynamicPredicateValueCtx = new DynamicPredicateValueCtxImpl(ctx.getTenantId(), deviceId, ctx);

        if (config.isPersistAlarmRulesState()) {
            if (state != null) {
                this.state = state;
            } else {
                this.state = ctx.findRuleNodeStateForEntity(deviceId);
            }
            if (this.state != null) {
                pds = JacksonUtil.fromString(this.state.getStateData(), PersistedDeviceState.class);
            } else {
                this.state = new RuleNodeState();
                this.state.setRuleNodeId(ctx.getSelfId());
                this.state.setEntityId(deviceId);
                pds = new PersistedDeviceState();
                pds.setAlarmStates(new HashMap<>());
            }
        }
        if (pds != null) {
            for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            }
        }
    }

    public void updateProfile(TbContext ctx, DeviceProfile deviceProfile) throws ExecutionException, InterruptedException {
        Set<AlarmConditionFilterKey> oldKeys = Set.copyOf(this.deviceProfile.getEntityKeys());
        this.deviceProfile.updateDeviceProfile(deviceProfile);

        if (latestValues == null && hasDurationRulesWithDynamicValueFromCurrentDevice(this.deviceProfile)) {
            latestValues = fetchLatestValues(ctx, deviceId);
        } else if (latestValues != null) {
            Set<AlarmConditionFilterKey> keysToFetch = new HashSet<>(this.deviceProfile.getEntityKeys());
            keysToFetch.removeAll(oldKeys);
            if (!keysToFetch.isEmpty()) {
                addEntityKeysToSnapshot(ctx, deviceId, keysToFetch, latestValues);
            }
        }
        Set<String> newAlarmStateIds = this.deviceProfile.getAlarmSettings().stream().map(DeviceProfileAlarm::getId).collect(Collectors.toSet());
        alarmStates.keySet().removeIf(id -> !newAlarmStateIds.contains(id));
        for (DeviceProfileAlarm alarm : this.deviceProfile.getAlarmSettings()) {
            if (alarmStates.containsKey(alarm.getId())) {
                alarmStates.get(alarm.getId()).updateState(alarm, getOrInitPersistedAlarmState(alarm));
            } else {
                alarmStates.putIfAbsent(alarm.getId(), new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            }
        }
    }

    private static boolean hasDurationRulesWithDynamicValueFromCurrentDevice(ProfileState deviceProfile) {
        return deviceProfile.getAlarmSettings().stream().anyMatch(DeviceState::isDurationRuleWithDynamicValueFromCurrentDevice);
    }

    private static boolean isDurationRuleWithDynamicValueFromCurrentDevice(DeviceProfileAlarm alarm) {
        return Stream.concat(alarm.getCreateRules().values().stream(), Stream.ofNullable(alarm.getClearRule()))
                .map(AlarmRule::getCondition)
                .map(AlarmCondition::getSpec)
                .anyMatch(spec -> isDurationRule(spec) && hasDynamicDurationValueFromCurrentDevice((DurationAlarmConditionSpec) spec));
    }

    private static boolean isDurationRule(AlarmConditionSpec spec) {
        return spec instanceof DurationAlarmConditionSpec durationSpec && durationSpec.getType() == AlarmConditionSpecType.DURATION;
    }

    private static boolean hasDynamicDurationValueFromCurrentDevice(DurationAlarmConditionSpec spec) {
        DynamicValue<Long> dynamicValue = spec.getPredicate().getDynamicValue();
        return dynamicValue != null && dynamicValue.getSourceType() == DynamicValueSourceType.CURRENT_DEVICE;
    }

    public void harvestAlarms(TbContext ctx, long ts) throws ExecutionException, InterruptedException {
        log.debug("[{}] Going to harvest alarms: {}", ctx.getSelfId(), ts);
        boolean stateChanged = false;
        for (AlarmState state : alarmStates.values()) {
            state.setDataSnapshot(latestValues);
            stateChanged |= state.process(ctx, ts);
        }
        if (persistState && stateChanged) {
            state.setStateData(JacksonUtil.toString(pds));
            state = ctx.saveRuleNodeState(state);
        }
    }

    public void process(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        if (latestValues == null) {
            latestValues = fetchLatestValues(ctx, deviceId);
        }
        boolean stateChanged = false;
        if (msg.isTypeOf(POST_TELEMETRY_REQUEST)) {
            stateChanged = processTelemetryRequest(ctx, msg);
        } else if (msg.isTypeOf(TIMESERIES_UPDATED)) {
            stateChanged = processTelemetryUpdatedNotification(ctx, msg);
        } else if (msg.isTypeOf(POST_ATTRIBUTES_REQUEST)) {
            stateChanged = processAttributesUpdateRequest(ctx, msg);
        } else if (msg.isTypeOneOf(ACTIVITY_EVENT, INACTIVITY_EVENT)) {
            stateChanged = processDeviceActivityEvent(ctx, msg);
        } else if (msg.isTypeOf(ATTRIBUTES_UPDATED)) {
            stateChanged = processAttributesUpdateNotification(ctx, msg);
        } else if (msg.isTypeOf(ATTRIBUTES_DELETED)) {
            stateChanged = processAttributesDeleteNotification(ctx, msg);
        } else if (msg.isTypeOf(ALARM_CLEAR)) {
            stateChanged = processAlarmClearNotification(ctx, msg);
        } else if (msg.isTypeOf(ALARM_ACK)) {
            processAlarmAckNotification(ctx, msg);
        } else if (msg.isTypeOf(ALARM_DELETE)) {
            processAlarmDeleteNotification(ctx, msg);
        } else {
            if (msg.isTypeOneOf(ENTITY_ASSIGNED, ENTITY_UNASSIGNED)) {
                dynamicPredicateValueCtx.resetCustomer();
            }
            ctx.tellSuccess(msg);
        }
        if (persistState && stateChanged) {
            state.setStateData(JacksonUtil.toString(pds));
            state = ctx.saveRuleNodeState(state);
        }
    }

    private boolean processDeviceActivityEvent(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            return processTelemetryRequest(ctx, msg);
        } else {
            return processAttributes(ctx, msg, scope);
        }
    }

    private boolean processAlarmClearNotification(TbContext ctx, TbMsg msg) {
        boolean stateChanged = false;
        Alarm alarmNf = JacksonUtil.fromString(msg.getData(), Alarm.class);
        for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
            AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                    a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            stateChanged |= alarmState.processAlarmClear(ctx, alarmNf);
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    private void processAlarmAckNotification(TbContext ctx, TbMsg msg) {
        Alarm alarmNf = JacksonUtil.fromString(msg.getData(), Alarm.class);
        for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
            AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                    a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            alarmState.processAckAlarm(alarmNf);
        }
        ctx.tellSuccess(msg);
    }

    private void processAlarmDeleteNotification(TbContext ctx, TbMsg msg) {
        Alarm alarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
        alarmStates.values().removeIf(alarmState -> alarmState.getCurrentAlarm() != null
                && alarmState.getCurrentAlarm().getId().equals(alarm.getId()));
        ctx.tellSuccess(msg);
    }

    private boolean processAttributesUpdateNotification(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = DataConstants.CLIENT_SCOPE;
        }
        return processAttributes(ctx, msg, scope);
    }

    private boolean processAttributesDeleteNotification(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        List<String> keys = new ArrayList<>();
        JsonParser.parseString(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray().forEach(e -> keys.add(e.getAsString()));
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = DataConstants.CLIENT_SCOPE;
        }
        if (!keys.isEmpty()) {
            EntityKeyType keyType = getKeyTypeFromScope(scope);
            Set<AlarmConditionFilterKey> removedKeys = keys.stream().map(key -> new EntityKey(keyType, key))
                    .peek(latestValues::removeValue)
                    .map(DataSnapshot::toConditionKey).collect(Collectors.toSet());
            SnapshotUpdate update = new SnapshotUpdate(AlarmConditionKeyType.ATTRIBUTE, removedKeys);

            for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                stateChanged |= alarmState.process(ctx, msg, latestValues, update);
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    protected boolean processAttributesUpdateRequest(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        return processAttributes(ctx, msg, DataConstants.CLIENT_SCOPE);
    }

    private boolean processAttributes(TbContext ctx, TbMsg msg, String scope) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        List<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(JsonParser.parseString(msg.getData()));
        if (!attributes.isEmpty()) {
            SnapshotUpdate update = merge(latestValues, attributes, scope);
            for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                stateChanged |= alarmState.process(ctx, msg, latestValues, update);
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    protected boolean processTelemetryRequest(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        return processTelemetryUpdate(ctx, msg, JsonParser.parseString(msg.getData()));
    }

    protected boolean processTelemetryUpdatedNotification(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        JsonElement msgData = JsonParser.parseString(msg.getData());
        JsonElement telemetryData = Optional.ofNullable(JsonParser.parseString(msg.getData()))
                .filter(JsonElement::isJsonObject)
                .map(e -> e.getAsJsonObject().get("timeseries"))
                .orElse(msgData);
        return processTelemetryUpdate(ctx, msg, telemetryData);
    }

    private boolean processTelemetryUpdate(TbContext ctx, TbMsg msg, JsonElement telemetryData) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToSortedTelemetry(telemetryData, msg.getMetaDataTs());
        // iterate over data by ts (ASC order).
        for (Map.Entry<Long, List<KvEntry>> entry : tsKvMap.entrySet()) {
            Long ts = entry.getKey();
            List<KvEntry> data = entry.getValue();
            SnapshotUpdate update = merge(latestValues, ts, data);
            if (update.hasUpdate()) {
                for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                    AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                            a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                    try {
                        stateChanged |= alarmState.process(ctx, msg, latestValues, update);
                    } catch (ApiUsageLimitsExceededException e) {
                        alarmStates.remove(alarm.getId());
                        throw e;
                    }
                }
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    private SnapshotUpdate merge(DataSnapshot latestValues, Long newTs, List<KvEntry> data) {
        Set<AlarmConditionFilterKey> keys = new HashSet<>();
        for (KvEntry entry : data) {
            AlarmConditionFilterKey entityKey = new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, entry.getKey());
            if (latestValues.putValue(entityKey, newTs, toEntityValue(entry))) {
                keys.add(entityKey);
            }
        }
        latestValues.setTs(newTs);
        return new SnapshotUpdate(AlarmConditionKeyType.TIME_SERIES, keys);
    }

    private SnapshotUpdate merge(DataSnapshot latestValues, List<AttributeKvEntry> attributes, String scope) {
        long newTs = 0;
        Set<AlarmConditionFilterKey> keys = new HashSet<>();
        for (AttributeKvEntry entry : attributes) {
            newTs = Math.max(newTs, entry.getLastUpdateTs());
            AlarmConditionFilterKey entityKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, entry.getKey());
            if (latestValues.putValue(entityKey, newTs, toEntityValue(entry))) {
                keys.add(entityKey);
            }
        }
        latestValues.setTs(newTs);
        return new SnapshotUpdate(AlarmConditionKeyType.ATTRIBUTE, keys);
    }

    private static EntityKeyType getKeyTypeFromScope(String scope) {
        switch (scope) {
            case DataConstants.CLIENT_SCOPE:
                return EntityKeyType.CLIENT_ATTRIBUTE;
            case DataConstants.SHARED_SCOPE:
                return EntityKeyType.SHARED_ATTRIBUTE;
            case DataConstants.SERVER_SCOPE:
                return EntityKeyType.SERVER_ATTRIBUTE;
        }
        return EntityKeyType.ATTRIBUTE;
    }

    @SneakyThrows
    private DataSnapshot fetchLatestValues(TbContext ctx, EntityId originator) {
        Set<AlarmConditionFilterKey> entityKeysToFetch = deviceProfile.getEntityKeys();
        DataSnapshot result = new DataSnapshot(entityKeysToFetch);
        addEntityKeysToSnapshot(ctx, originator, entityKeysToFetch, result);
        return result;
    }

    private void addEntityKeysToSnapshot(TbContext ctx, EntityId originator, Set<AlarmConditionFilterKey> entityKeysToFetch, DataSnapshot result) throws InterruptedException, ExecutionException {
        Set<String> attributeKeys = new HashSet<>();
        Set<String> latestTsKeys = new HashSet<>();

        Device device = null;
        for (AlarmConditionFilterKey entityKey : entityKeysToFetch) {
            String key = entityKey.getKey();
            switch (entityKey.getType()) {
                case ATTRIBUTE:
                    attributeKeys.add(key);
                    break;
                case TIME_SERIES:
                    latestTsKeys.add(key);
                    break;
                case ENTITY_FIELD:
                    if (device == null) {
                        device = ctx.getDeviceService().findDeviceById(ctx.getTenantId(), new DeviceId(originator.getId()));
                    }
                    if (device != null) {
                        switch (key) {
                            case EntityKeyMapping.NAME:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromString(device.getName()));
                                break;
                            case EntityKeyMapping.TYPE:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromString(device.getType()));
                                break;
                            case EntityKeyMapping.CREATED_TIME:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromLong(device.getCreatedTime()));
                                break;
                            case EntityKeyMapping.LABEL:
                                result.putValue(entityKey, device.getCreatedTime(), EntityKeyValue.fromString(device.getLabel()));
                                break;
                        }
                    }
                    break;
            }
        }

        if (!latestTsKeys.isEmpty()) {
            List<TsKvEntry> data = ctx.getTimeseriesService().findLatest(ctx.getTenantId(), originator, latestTsKeys).get();
            for (TsKvEntry entry : data) {
                if (entry.getValue() != null) {
                    result.putValue(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, entry.getKey()), entry.getTs(), toEntityValue(entry));
                }
            }
        }
        if (!attributeKeys.isEmpty()) {
            addToSnapshot(result, ctx.getAttributesService().find(ctx.getTenantId(), originator, AttributeScope.CLIENT_SCOPE, attributeKeys).get());
            addToSnapshot(result, ctx.getAttributesService().find(ctx.getTenantId(), originator, AttributeScope.SHARED_SCOPE, attributeKeys).get());
            addToSnapshot(result, ctx.getAttributesService().find(ctx.getTenantId(), originator, AttributeScope.SERVER_SCOPE, attributeKeys).get());
        }
    }

    private void addToSnapshot(DataSnapshot snapshot, List<AttributeKvEntry> data) {
        for (AttributeKvEntry entry : data) {
            if (entry.getValue() != null) {
                EntityKeyValue value = toEntityValue(entry);
                snapshot.putValue(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, entry.getKey()), entry.getLastUpdateTs(), value);
            }
        }
    }

    public static EntityKeyValue toEntityValue(KvEntry entry) {
        switch (entry.getDataType()) {
            case STRING:
                return EntityKeyValue.fromString(entry.getStrValue().get());
            case LONG:
                return EntityKeyValue.fromLong(entry.getLongValue().get());
            case DOUBLE:
                return EntityKeyValue.fromDouble(entry.getDoubleValue().get());
            case BOOLEAN:
                return EntityKeyValue.fromBool(entry.getBooleanValue().get());
            case JSON:
                return EntityKeyValue.fromJson(entry.getJsonValue().get());
            default:
                throw new RuntimeException("Can't parse entry: " + entry.getDataType());
        }
    }

    public DeviceProfileId getProfileId() {
        return deviceProfile.getProfileId();
    }

    private PersistedAlarmState getOrInitPersistedAlarmState(DeviceProfileAlarm alarm) {
        if (pds != null) {
            PersistedAlarmState alarmState = pds.getAlarmStates().get(alarm.getId());
            if (alarmState == null) {
                alarmState = new PersistedAlarmState();
                alarmState.setCreateRuleStates(new HashMap<>());
                pds.getAlarmStates().put(alarm.getId(), alarmState);
            }
            return alarmState;
        } else {
            return null;
        }
    }

}
