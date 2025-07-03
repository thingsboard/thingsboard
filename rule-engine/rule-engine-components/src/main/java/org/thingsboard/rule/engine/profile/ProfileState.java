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

import lombok.AccessLevel;
import lombok.Getter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpecType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AlarmSchedule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.SimpleKeyFilterPredicate;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


class ProfileState {

    private DeviceProfile deviceProfile;
    @Getter(AccessLevel.PACKAGE)
    private final List<DeviceProfileAlarm> alarmSettings = new CopyOnWriteArrayList<>();
    @Getter(AccessLevel.PACKAGE)
    private final Set<AlarmConditionFilterKey> entityKeys = ConcurrentHashMap.newKeySet();

    private final Map<String, Map<AlarmSeverity, Set<AlarmConditionFilterKey>>> alarmCreateKeys = new HashMap<>();
    private final Map<String, Set<AlarmConditionFilterKey>> alarmClearKeys = new HashMap<>();

    ProfileState(DeviceProfile deviceProfile) {
        updateDeviceProfile(deviceProfile);
    }

    void updateDeviceProfile(DeviceProfile deviceProfile) {
        this.deviceProfile = deviceProfile;
        alarmSettings.clear();
        alarmCreateKeys.clear();
        alarmClearKeys.clear();
        entityKeys.clear();
        if (deviceProfile.getProfileData().getAlarms() != null) {
            alarmSettings.addAll(deviceProfile.getProfileData().getAlarms());
            for (DeviceProfileAlarm alarm : deviceProfile.getProfileData().getAlarms()) {
                Map<AlarmSeverity, Set<AlarmConditionFilterKey>> createAlarmKeys = alarmCreateKeys.computeIfAbsent(alarm.getId(), id -> new HashMap<>());
                alarm.getCreateRules().forEach(((severity, alarmRule) -> {
                    var ruleKeys = createAlarmKeys.computeIfAbsent(severity, id -> new HashSet<>());
                    for (var keyFilter : alarmRule.getCondition().getCondition()) {
                        entityKeys.add(keyFilter.getKey());
                        ruleKeys.add(keyFilter.getKey());
                        addDynamicValuesRecursively(keyFilter.getPredicate(), entityKeys, ruleKeys);
                    }
                    addEntityKeysFromAlarmConditionSpec(alarmRule);
                    AlarmSchedule schedule = alarmRule.getSchedule();
                    if (schedule != null) {
                        addScheduleDynamicValues(schedule, entityKeys);
                    }
                }));
                if (alarm.getClearRule() != null) {
                    var clearAlarmKeys = alarmClearKeys.computeIfAbsent(alarm.getId(), id -> new HashSet<>());
                    for (var keyFilter : alarm.getClearRule().getCondition().getCondition()) {
                        entityKeys.add(keyFilter.getKey());
                        clearAlarmKeys.add(keyFilter.getKey());
                        addDynamicValuesRecursively(keyFilter.getPredicate(), entityKeys, clearAlarmKeys);
                    }
                    addEntityKeysFromAlarmConditionSpec(alarm.getClearRule());
                }
            }
        }
    }

    void addScheduleDynamicValues(AlarmSchedule schedule, final Set<AlarmConditionFilterKey> entityKeys) {
        DynamicValue<String> dynamicValue = schedule.getDynamicValue();
        if (dynamicValue != null && dynamicValue.getSourceAttribute() != null) {
            entityKeys.add(
                    new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                            dynamicValue.getSourceAttribute())
            );
        }
    }

    private void addEntityKeysFromAlarmConditionSpec(AlarmRule alarmRule) {
        AlarmConditionSpec spec = alarmRule.getCondition().getSpec();
        if (spec == null) {
            return;
        }
        AlarmConditionSpecType specType = spec.getType();
        switch (specType) {
            case DURATION:
                DurationAlarmConditionSpec duration = (DurationAlarmConditionSpec) spec;
                if(duration.getPredicate().getDynamicValue() != null
                        && duration.getPredicate().getDynamicValue().getSourceAttribute() != null) {
                    entityKeys.add(
                            new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                                    duration.getPredicate().getDynamicValue().getSourceAttribute())
                    );
                }
                break;
            case REPEATING:
                RepeatingAlarmConditionSpec repeating = (RepeatingAlarmConditionSpec) spec;
                if(repeating.getPredicate().getDynamicValue() != null
                        && repeating.getPredicate().getDynamicValue().getSourceAttribute() != null) {
                    entityKeys.add(
                            new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                                    repeating.getPredicate().getDynamicValue().getSourceAttribute())
                    );
                }
                break;
        }

    }

    void addDynamicValuesRecursively(KeyFilterPredicate predicate, Set<AlarmConditionFilterKey> entityKeys, Set<AlarmConditionFilterKey> ruleKeys) {
        switch (predicate.getType()) {
            case STRING:
            case NUMERIC:
            case BOOLEAN:
                DynamicValue value = ((SimpleKeyFilterPredicate) predicate).getValue().getDynamicValue();
                if (value != null && value.getSourceAttribute() != null && (
                        value.getSourceType() == DynamicValueSourceType.CURRENT_TENANT ||
                        value.getSourceType() == DynamicValueSourceType.CURRENT_CUSTOMER ||
                        value.getSourceType() == DynamicValueSourceType.CURRENT_DEVICE)) {
                    AlarmConditionFilterKey entityKey = new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, value.getSourceAttribute());
                    entityKeys.add(entityKey);
                    ruleKeys.add(entityKey);
                }
                break;
            case COMPLEX:
                for (KeyFilterPredicate child : ((ComplexFilterPredicate) predicate).getPredicates()) {
                    addDynamicValuesRecursively(child, entityKeys, ruleKeys);
                }
                break;
        }
    }

    DeviceProfileId getProfileId() {
        return deviceProfile.getId();
    }

    Set<AlarmConditionFilterKey> getCreateAlarmKeys(String id, AlarmSeverity severity) {
        Map<AlarmSeverity, Set<AlarmConditionFilterKey>> sKeys = alarmCreateKeys.get(id);
        if (sKeys == null) {
            return Collections.emptySet();
        } else {
            Set<AlarmConditionFilterKey> keys = sKeys.get(severity);
            if (keys == null) {
                return Collections.emptySet();
            } else {
                return keys;
            }
        }
    }

    Set<AlarmConditionFilterKey> getClearAlarmKeys(String id) {
        Set<AlarmConditionFilterKey> keys = alarmClearKeys.get(id);
        if (keys == null) {
            return Collections.emptySet();
        } else {
            return keys;
        }
    }
}
