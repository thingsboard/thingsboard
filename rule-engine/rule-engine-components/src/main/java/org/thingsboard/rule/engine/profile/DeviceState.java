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
package org.thingsboard.rule.engine.profile;

import com.google.gson.JsonParser;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.dao.sql.query.EntityKeyMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

class DeviceState {

    private final DeviceId deviceId;
    private DeviceProfileState deviceProfile;
    private DeviceDataSnapshot latestValues;
    private final ConcurrentMap<String, DeviceProfileAlarmState> alarmStates = new ConcurrentHashMap<>();

    public DeviceState(DeviceId deviceId, DeviceProfileState deviceProfile) {
        this.deviceId = deviceId;
        this.deviceProfile = deviceProfile;
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
                alarmStates.get(alarm.getId()).updateState(alarm);
            } else {
                alarmStates.putIfAbsent(alarm.getId(), new DeviceProfileAlarmState(deviceId, alarm));
            }
        }
    }

    public void process(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        if (latestValues == null) {
            latestValues = fetchLatestValues(ctx, deviceId);
        }
        if (msg.getType().equals(SessionMsgType.POST_TELEMETRY_REQUEST.name())) {
            processTelemetry(ctx, msg);
        } else {
            ctx.tellSuccess(msg);
        }
    }

    private void processTelemetry(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException {
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToSortedTelemetry(new JsonParser().parse(msg.getData()), TbMsgTimeseriesNode.getTs(msg));
        for (Map.Entry<Long, List<KvEntry>> entry : tsKvMap.entrySet()) {
            Long ts = entry.getKey();
            List<KvEntry> data = entry.getValue();
            latestValues = merge(latestValues, ts, data);
            for (DeviceProfileAlarm alarm : deviceProfile.getAlarmSettings()) {
                DeviceProfileAlarmState alarmState = alarmStates.computeIfAbsent(alarm.getId(), a -> new DeviceProfileAlarmState(deviceId, alarm));
                alarmState.process(ctx, msg, latestValues);
            }
        }
        ctx.tellSuccess(msg);
    }

    private DeviceDataSnapshot merge(DeviceDataSnapshot latestValues, Long ts, List<KvEntry> data) {
        latestValues.setTs(ts);
        for (KvEntry entry : data) {
            latestValues.putValue(new EntityKey(EntityKeyType.TIME_SERIES, entry.getKey()), toEntityValue(entry));
        }
        return latestValues;
    }

    private DeviceDataSnapshot fetchLatestValues(TbContext ctx, EntityId originator) throws ExecutionException, InterruptedException {
        Set<EntityKey> entityKeysToFetch = deviceProfile.getEntityKeys();
        DeviceDataSnapshot result = new DeviceDataSnapshot(entityKeysToFetch);
        addEntityKeysToSnapshot(ctx, originator, entityKeysToFetch, result);
        return result;
    }

    private void addEntityKeysToSnapshot(TbContext ctx, EntityId originator, Set<EntityKey> entityKeysToFetch, DeviceDataSnapshot result) throws InterruptedException, ExecutionException {
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
                                result.putValue(entityKey, EntityKeyValue.fromString(device.getName()));
                                break;
                            case EntityKeyMapping.TYPE:
                                result.putValue(entityKey, EntityKeyValue.fromString(device.getType()));
                                break;
                            case EntityKeyMapping.CREATED_TIME:
                                result.putValue(entityKey, EntityKeyValue.fromLong(device.getCreatedTime()));
                                break;
                            case EntityKeyMapping.LABEL:
                                result.putValue(entityKey, EntityKeyValue.fromString(device.getLabel()));
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
                    result.putValue(new EntityKey(EntityKeyType.TIME_SERIES, entry.getKey()), toEntityValue(entry));
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

    private void addToSnapshot(DeviceDataSnapshot snapshot, Set<String> commonAttributeKeys, List<AttributeKvEntry> data) {
        for (AttributeKvEntry entry : data) {
            if (entry.getValue() != null) {
                EntityKeyValue value = toEntityValue(entry);
                snapshot.putValue(new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, entry.getKey()), value);
                if (commonAttributeKeys.contains(entry.getKey())) {
                    snapshot.putValue(new EntityKey(EntityKeyType.ATTRIBUTE, entry.getKey()), value);
                }
            }
        }
    }

    private EntityKeyValue toEntityValue(KvEntry entry) {
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

}
