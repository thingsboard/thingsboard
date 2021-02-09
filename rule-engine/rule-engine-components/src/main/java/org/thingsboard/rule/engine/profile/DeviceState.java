/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.profile.state.PersistedAlarmState;
import org.thingsboard.rule.engine.profile.state.PersistedDeviceState;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
        Set<EntityKey> oldKeys = this.deviceProfile.getEntityKeys();
        this.deviceProfile.updateDeviceProfile(deviceProfile);
        if (latestValues != null) {
            Set<EntityKey> keysToFetch = new HashSet<>(this.deviceProfile.getEntityKeys());
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

    public void harvestAlarms(TbContext ctx, long ts) throws ExecutionException, InterruptedException {
        log.debug("[{}] Going to harvest alarms: {}", ctx.getSelfId(), ts);
        boolean stateChanged = false;
        for (AlarmState state : alarmStates.values()) {
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
        if (msg.getType().equals(SessionMsgType.POST_TELEMETRY_REQUEST.name())) {
            stateChanged = processTelemetry(ctx, msg);
        } else if (msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name())) {
            stateChanged = processAttributesUpdateRequest(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ATTRIBUTES_UPDATED)) {
            stateChanged = processAttributesUpdateNotification(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ATTRIBUTES_DELETED)) {
            stateChanged = processAttributesDeleteNotification(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ALARM_CLEAR)) {
            stateChanged = processAlarmClearNotification(ctx, msg);
        } else if (msg.getType().equals(DataConstants.ALARM_ACK)) {
            processAlarmAckNotification(ctx, msg);
        } else {
            if (msg.getType().equals(DataConstants.ENTITY_ASSIGNED) || msg.getType().equals(DataConstants.ENTITY_UNASSIGNED)) {
                dynamicPredicateValueCtx.resetCustomer();
            }
            ctx.tellSuccess(msg);
        }
        if (persistState && stateChanged) {
            state.setStateData(JacksonUtil.toString(pds));
            state = ctx.saveRuleNodeState(state);
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

    private boolean processAttributesUpdateNotification(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(msg.getData()));
        String scope = msg.getMetaData().getValue("scope");
        if (StringUtils.isEmpty(scope)) {
            scope = DataConstants.CLIENT_SCOPE;
        }
        return processAttributesUpdate(ctx, msg, attributes, scope);
    }

    private boolean processAttributesDeleteNotification(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        List<String> keys = new ArrayList<>();
        new JsonParser().parse(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray().forEach(e -> keys.add(e.getAsString()));
        String scope = msg.getMetaData().getValue("scope");
        if (StringUtils.isEmpty(scope)) {
            scope = DataConstants.CLIENT_SCOPE;
        }
        if (!keys.isEmpty()) {
            EntityKeyType keyType = getKeyTypeFromScope(scope);
            keys.forEach(key -> latestValues.removeValue(new EntityKey(keyType, key)));
            for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                stateChanged |= alarmState.process(ctx, msg, latestValues, null);
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    protected boolean processAttributesUpdateRequest(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(msg.getData()));
        return processAttributesUpdate(ctx, msg, attributes, DataConstants.CLIENT_SCOPE);
    }

    private boolean processAttributesUpdate(TbContext ctx, TbMsg msg, Set<AttributeKvEntry> attributes, String scope) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
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

    protected boolean processTelemetry(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToSortedTelemetry(new JsonParser().parse(msg.getData()), TbMsgTimeseriesNode.getTs(msg));
        // iterate over data by ts (ASC order).
        for (Map.Entry<Long, List<KvEntry>> entry : tsKvMap.entrySet()) {
            Long ts = entry.getKey();
            List<KvEntry> data = entry.getValue();
            SnapshotUpdate update = merge(latestValues, ts, data);
            if (update.hasUpdate()) {
                for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                    AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                            a -> new AlarmState(this.deviceProfile, deviceId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                    stateChanged |= alarmState.process(ctx, msg, latestValues, update);
                }
            }
        }
        ctx.tellSuccess(msg);
        return stateChanged;
    }

    private SnapshotUpdate merge(DataSnapshot latestValues, Long newTs, List<KvEntry> data) {
        Set<EntityKey> keys = new HashSet<>();
        for (KvEntry entry : data) {
            EntityKey entityKey = new EntityKey(EntityKeyType.TIME_SERIES, entry.getKey());
            if (latestValues.putValue(entityKey, newTs, toEntityValue(entry))) {
                keys.add(entityKey);
            }
        }
        latestValues.setTs(newTs);
        return new SnapshotUpdate(EntityKeyType.TIME_SERIES, keys);
    }

    private SnapshotUpdate merge(DataSnapshot latestValues, Set<AttributeKvEntry> attributes, String scope) {
        long newTs = 0;
        Set<EntityKey> keys = new HashSet<>();
        for (AttributeKvEntry entry : attributes) {
            newTs = Math.max(newTs, entry.getLastUpdateTs());
            EntityKey entityKey = new EntityKey(getKeyTypeFromScope(scope), entry.getKey());
            if (latestValues.putValue(entityKey, newTs, toEntityValue(entry))) {
                keys.add(entityKey);
            }
        }
        latestValues.setTs(newTs);
        return new SnapshotUpdate(EntityKeyType.ATTRIBUTE, keys);
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

    private DataSnapshot fetchLatestValues(TbContext ctx, EntityId originator) throws ExecutionException, InterruptedException {
        Set<EntityKey> entityKeysToFetch = deviceProfile.getEntityKeys();
        DataSnapshot result = new DataSnapshot(entityKeysToFetch);
        addEntityKeysToSnapshot(ctx, originator, entityKeysToFetch, result);
        return result;
    }

    private void addEntityKeysToSnapshot(TbContext ctx, EntityId originator, Set<EntityKey> entityKeysToFetch, DataSnapshot result) throws InterruptedException, ExecutionException {
        Set<String> serverAttributeKeys = new HashSet<>();
        Set<String> clientAttributeKeys = new HashSet<>();
        Set<String> sharedAttributeKeys = new HashSet<>();
        Set<String> commonAttributeKeys = new HashSet<>();
        Set<String> latestTsKeys = new HashSet<>();

        Device device = null;
        for (EntityKey entityKey : entityKeysToFetch) {
            String key = entityKey.getKey();
            switch (entityKey.getType()) {
                case SERVER_ATTRIBUTE:
                    serverAttributeKeys.add(key);
                    break;
                case CLIENT_ATTRIBUTE:
                    clientAttributeKeys.add(key);
                    break;
                case SHARED_ATTRIBUTE:
                    sharedAttributeKeys.add(key);
                    break;
                case ATTRIBUTE:
                    serverAttributeKeys.add(key);
                    clientAttributeKeys.add(key);
                    sharedAttributeKeys.add(key);
                    commonAttributeKeys.add(key);
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
                    result.putValue(new EntityKey(EntityKeyType.TIME_SERIES, entry.getKey()), entry.getTs(), toEntityValue(entry));
                }
            }
        }
        if (!clientAttributeKeys.isEmpty()) {
            addToSnapshot(result, commonAttributeKeys,
                    ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.CLIENT_SCOPE, clientAttributeKeys).get());
        }
        if (!sharedAttributeKeys.isEmpty()) {
            addToSnapshot(result, commonAttributeKeys,
                    ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.SHARED_SCOPE, sharedAttributeKeys).get());
        }
        if (!serverAttributeKeys.isEmpty()) {
            addToSnapshot(result, commonAttributeKeys,
                    ctx.getAttributesService().find(ctx.getTenantId(), originator, DataConstants.SERVER_SCOPE, serverAttributeKeys).get());
        }
    }

    private void addToSnapshot(DataSnapshot snapshot, Set<String> commonAttributeKeys, List<AttributeKvEntry> data) {
        for (AttributeKvEntry entry : data) {
            if (entry.getValue() != null) {
                EntityKeyValue value = toEntityValue(entry);
                snapshot.putValue(new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, entry.getKey()), entry.getLastUpdateTs(), value);
                if (commonAttributeKeys.contains(entry.getKey())) {
                    snapshot.putValue(new EntityKey(EntityKeyType.ATTRIBUTE, entry.getKey()), entry.getLastUpdateTs(), value);
                }
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
