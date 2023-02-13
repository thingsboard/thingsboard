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
package org.thingsboard.server.service.alarm.rule;

import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleEntityState;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.exception.ApiUsageLimitsExceededException;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;
import org.thingsboard.server.service.alarm.rule.state.PersistedAlarmState;
import org.thingsboard.server.service.alarm.rule.state.PersistedEntityState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
class EntityState {

    @Getter
    private final TenantId tenantId;
    @Getter
    private final EntityId entityId;
    private final TbAlarmRuleContext ctx;
    private final EntityRulesState entityRulesState;
    private final AlarmRuleEntityState state;
    private final PersistedEntityState pes;
    private DataSnapshot latestValues;
    private final ConcurrentMap<AlarmRuleId, AlarmState> alarmStates = new ConcurrentHashMap<>();
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;
    private final Lock lock = new ReentrantLock();

    EntityState(TenantId tenantId, EntityId entityId, TbAlarmRuleContext ctx, EntityRulesState entityRulesState, AlarmRuleEntityState state) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.ctx = ctx;
        this.entityRulesState = entityRulesState;

        this.dynamicPredicateValueCtx = new DynamicPredicateValueCtxImpl(tenantId, entityId, ctx);

        if (state != null) {
            this.state = state;
            this.pes = JacksonUtil.fromString(this.state.getData(), PersistedEntityState.class);

            for (AlarmRule alarmRule : entityRulesState.getAlarmRules().values()) {
                alarmStates.computeIfAbsent(alarmRule.getId(),
                        a -> new AlarmState(entityRulesState, tenantId, entityId, alarmRule, getOrInitPersistedAlarmState(alarmRule), dynamicPredicateValueCtx));
            }
        } else {
            this.state = new AlarmRuleEntityState();
            this.state.setTenantId(tenantId);
            this.state.setEntityId(entityId);

            pes = new PersistedEntityState();
            pes.setAlarmStates(new HashMap<>());
        }
    }

    public void addAlarmRule(AlarmRule alarmRule) {
        if (!alarmStates.containsKey(alarmRule.getId())) {
            lock.lock();
            try {
                entityRulesState.addAlarmRule(alarmRule);
                alarmStates.put(alarmRule.getId(),
                        new AlarmState(entityRulesState, tenantId, entityId, alarmRule, getOrInitPersistedAlarmState(alarmRule), dynamicPredicateValueCtx));
            } finally {
                lock.unlock();
            }
        }
    }

    //TODO: check if we need to recalculate all keys
    public void updateAlarmRule(AlarmRule alarmRule) throws ExecutionException, InterruptedException {
        lock.lock();
        try {
            Set<AlarmConditionFilterKey> oldKeys = Set.copyOf(this.entityRulesState.getEntityKeys());
            this.entityRulesState.updateAlarmRule(alarmRule);
            if (latestValues != null) {
                Set<AlarmConditionFilterKey> keysToFetch = new HashSet<>(this.entityRulesState.getEntityKeys());
                keysToFetch.removeAll(oldKeys);
                if (!keysToFetch.isEmpty()) {
                    addEntityKeysToSnapshot(ctx, entityId, keysToFetch, latestValues);
                }
            }

            if (alarmStates.containsKey(alarmRule.getId())) {
                alarmStates.get(alarmRule.getId()).updateState(alarmRule, getOrInitPersistedAlarmState(alarmRule));
            } else {
                alarmStates.putIfAbsent(alarmRule.getId(), new AlarmState(this.entityRulesState, tenantId, entityId, alarmRule, getOrInitPersistedAlarmState(alarmRule), dynamicPredicateValueCtx));
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeAlarmRule(AlarmRuleId alarmRuleId) {
        lock.lock();
        try {
            entityRulesState.removeAlarmRule(alarmRuleId);
            alarmStates.remove(alarmRuleId);
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        return alarmStates.isEmpty();
    }

    public void harvestAlarms(long ts) throws ExecutionException, InterruptedException {
        log.debug("[{}] Going to harvest alarms: {}", entityId, ts);
        lock.lock();
        try {
            boolean stateChanged = false;
            for (AlarmState state : alarmStates.values()) {
                stateChanged |= state.process(ctx, ts);
            }
            if (stateChanged) {
                state.setData(JacksonUtil.toString(pes));
                ctx.getStateService().save(tenantId, state);
            }
        } finally {
            lock.unlock();
        }
    }

    public void process(TbContext tbContext, TbMsg msg) throws ExecutionException, InterruptedException {
        lock.lock();
        try {
            if (latestValues == null) {
                latestValues = fetchLatestValues(ctx, entityId);
            }
            boolean stateChanged = false;
            if (msg.getType().equals(SessionMsgType.POST_TELEMETRY_REQUEST.name())) {
                stateChanged = processTelemetry(tbContext, msg);
            } else if (msg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name())) {
                stateChanged = processAttributesUpdateRequest(tbContext, msg);
            } else if (msg.getType().equals(DataConstants.ACTIVITY_EVENT) || msg.getType().equals(DataConstants.INACTIVITY_EVENT)) {
                stateChanged = processDeviceActivityEvent(tbContext, msg);
            } else if (msg.getType().equals(DataConstants.ATTRIBUTES_UPDATED)) {
                stateChanged = processAttributesUpdateNotification(tbContext, msg);
            } else if (msg.getType().equals(DataConstants.ATTRIBUTES_DELETED)) {
                stateChanged = processAttributesDeleteNotification(tbContext, msg);
            } else if (msg.getType().equals(DataConstants.ALARM_CLEAR)) {
                stateChanged = processAlarmClearNotification(msg);
            } else if (msg.getType().equals(DataConstants.ALARM_ACK)) {
                processAlarmAckNotification(msg);
            } else if (msg.getType().equals(DataConstants.ALARM_DELETE)) {
                processAlarmDeleteNotification(msg);
            } else {
                if (msg.getType().equals(DataConstants.ENTITY_ASSIGNED) || msg.getType().equals(DataConstants.ENTITY_UNASSIGNED)) {
                    dynamicPredicateValueCtx.resetCustomer();
                }
            }
            if (stateChanged) {
                state.setData(JacksonUtil.toString(pes));
                ctx.getStateService().save(tenantId, state);
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean processDeviceActivityEvent(TbContext tbContext, TbMsg msg) throws ExecutionException, InterruptedException {
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            return processTelemetry(tbContext, msg);
        } else {
            return processAttributes(tbContext, msg, scope);
        }
    }

    private boolean processAlarmClearNotification(TbMsg msg) {
        boolean stateChanged = false;
        Alarm alarmNf = JacksonUtil.fromString(msg.getData(), Alarm.class);
        for (AlarmRule alarm : entityRulesState.getAlarmRules().values()) {
            AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                    a -> new AlarmState(this.entityRulesState, tenantId, entityId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            stateChanged |= alarmState.processAlarmClear(alarmNf);
        }
        return stateChanged;
    }

    private void processAlarmAckNotification(TbMsg msg) {
        Alarm alarmNf = JacksonUtil.fromString(msg.getData(), Alarm.class);
        for (AlarmRule alarm : entityRulesState.getAlarmRules().values()) {
            AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                    a -> new AlarmState(this.entityRulesState, tenantId, entityId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
            alarmState.processAckAlarm(alarmNf);
        }
    }

    private void processAlarmDeleteNotification(TbMsg msg) {
        Alarm alarm = JacksonUtil.fromString(msg.getData(), Alarm.class);
        alarmStates.values().removeIf(alarmState -> alarmState.getCurrentAlarm() != null
                && alarmState.getCurrentAlarm().getId().equals(alarm.getId()));
    }

    private boolean processAttributesUpdateNotification(TbContext tbContext, TbMsg msg) throws ExecutionException, InterruptedException {
        String scope = msg.getMetaData().getValue(DataConstants.SCOPE);
        if (StringUtils.isEmpty(scope)) {
            scope = DataConstants.CLIENT_SCOPE;
        }
        return processAttributes(tbContext, msg, scope);
    }

    private boolean processAttributesDeleteNotification(TbContext tbContext, TbMsg msg) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        List<String> keys = new ArrayList<>();
        new JsonParser().parse(msg.getData()).getAsJsonObject().get("attributes").getAsJsonArray().forEach(e -> keys.add(e.getAsString()));
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

            for (AlarmRule alarm : entityRulesState.getAlarmRules().values()) {
                AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(this.entityRulesState, tenantId, entityId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                stateChanged |= alarmState.process(tbContext, ctx, msg, latestValues, update);
            }
        }
        return stateChanged;
    }

    private boolean processAttributesUpdateRequest(TbContext tbContext, TbMsg msg) throws ExecutionException, InterruptedException {
        return processAttributes(tbContext, msg, DataConstants.CLIENT_SCOPE);
    }

    private boolean processAttributes(TbContext tbContext, TbMsg msg, String scope) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        Set<AttributeKvEntry> attributes = JsonConverter.convertToAttributes(new JsonParser().parse(msg.getData()));
        if (!attributes.isEmpty()) {
            SnapshotUpdate update = merge(latestValues, attributes, scope);
            for (AlarmRule alarm : entityRulesState.getAlarmRules().values()) {
                AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                        a -> new AlarmState(this.entityRulesState, tenantId, entityId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                stateChanged |= alarmState.process(tbContext, ctx, msg, latestValues, update);
            }
        }
        return stateChanged;
    }

    private boolean processTelemetry(TbContext tbContext, TbMsg msg) throws ExecutionException, InterruptedException {
        boolean stateChanged = false;
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToSortedTelemetry(new JsonParser().parse(msg.getData()), msg.getMetaDataTs());
        // iterate over data by ts (ASC order).
        for (Map.Entry<Long, List<KvEntry>> entry : tsKvMap.entrySet()) {
            Long ts = entry.getKey();
            List<KvEntry> data = entry.getValue();
            SnapshotUpdate update = merge(latestValues, ts, data);
            if (update.hasUpdate()) {
                for (AlarmRule alarm : entityRulesState.getAlarmRules().values()) {
                    AlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(),
                            a -> new AlarmState(this.entityRulesState, tenantId, entityId, alarm, getOrInitPersistedAlarmState(alarm), dynamicPredicateValueCtx));
                    try {
                        stateChanged |= alarmState.process(tbContext, ctx, msg, latestValues, update);
                    } catch (ApiUsageLimitsExceededException e) {
                        alarmStates.remove(alarm.getId());
                        throw e;
                    }
                }
            }
        }
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

    private SnapshotUpdate merge(DataSnapshot latestValues, Set<AttributeKvEntry> attributes, String scope) {
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

    private DataSnapshot fetchLatestValues(TbAlarmRuleContext ctx, EntityId originator) throws ExecutionException, InterruptedException {
        Set<AlarmConditionFilterKey> entityKeysToFetch = entityRulesState.getEntityKeys();
        DataSnapshot result = new DataSnapshot(entityKeysToFetch);
        addEntityKeysToSnapshot(ctx, originator, entityKeysToFetch, result);
        return result;
    }

    private void addEntityKeysToSnapshot(TbAlarmRuleContext ctx, EntityId originator, Set<AlarmConditionFilterKey> entityKeysToFetch, DataSnapshot result) throws InterruptedException, ExecutionException {
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
                //TODO: add other entities
                case ENTITY_FIELD:
                    if (device == null) {
                        device = ctx.getDeviceService().findDeviceById(tenantId, new DeviceId(originator.getId()));
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
            List<TsKvEntry> data = ctx.getTimeseriesService().findLatest(tenantId, originator, latestTsKeys).get();
            for (TsKvEntry entry : data) {
                if (entry.getValue() != null) {
                    result.putValue(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, entry.getKey()), entry.getTs(), toEntityValue(entry));
                }
            }
        }
        if (!attributeKeys.isEmpty()) {
            addToSnapshot(result, ctx.getAttributesService().find(tenantId, originator, DataConstants.CLIENT_SCOPE, attributeKeys).get());
            addToSnapshot(result, ctx.getAttributesService().find(tenantId, originator, DataConstants.SHARED_SCOPE, attributeKeys).get());
            addToSnapshot(result, ctx.getAttributesService().find(tenantId, originator, DataConstants.SERVER_SCOPE, attributeKeys).get());
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

    private PersistedAlarmState getOrInitPersistedAlarmState(AlarmRule alarm) {
        if (pes != null) {
            PersistedAlarmState alarmState = pes.getAlarmStates().get(alarm.getUuidId().toString());
            if (alarmState == null) {
                alarmState = new PersistedAlarmState();
                alarmState.setCreateRuleStates(new HashMap<>());
                pes.getAlarmStates().put(alarm.getUuidId().toString(), alarmState);
            }
            return alarmState;
        } else {
            return null;
        }
    }

}
