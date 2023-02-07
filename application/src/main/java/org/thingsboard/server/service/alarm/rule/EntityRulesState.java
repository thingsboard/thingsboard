/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.AccessLevel;
import lombok.Getter;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpecType;
import org.thingsboard.server.common.data.device.profile.AlarmRuleCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.device.profile.AlarmSchedule;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.id.AlarmRuleId;
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

class EntityRulesState {

    @Getter(AccessLevel.PACKAGE)
    private final Map<AlarmRuleId, AlarmRule> alarmRules = new ConcurrentHashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Set<AlarmConditionFilterKey> entityKeys = ConcurrentHashMap.newKeySet();

    private final Map<AlarmRuleId, Map<AlarmSeverity, Set<AlarmConditionFilterKey>>> alarmCreateKeys = new HashMap<>();
    private final Map<AlarmRuleId, Set<AlarmConditionFilterKey>> alarmClearKeys = new HashMap<>();

    EntityRulesState(List<AlarmRule> alarmRules) {
        alarmRules.forEach(this::addAlarmRule);
    }

    void addAlarmRule(AlarmRule alarmRule) {
        alarmRules.computeIfAbsent(alarmRule.getId(), key -> {
            addAlarmRuleKeys(alarmRule);
            return alarmRule;
        });
    }

    void updateAlarmRule(AlarmRule alarmRule) {
        alarmRules.put(alarmRule.getId(), alarmRule);
        alarmCreateKeys.get(alarmRule.getId()).clear();
        alarmClearKeys.get(alarmRule.getId()).clear();
        entityKeys.clear();

        alarmRules.values().forEach(this::addAlarmRuleKeys);
    }

    void removeAlarmRule(AlarmRuleId alarmRuleId) {
        alarmRules.remove(alarmRuleId);
        alarmCreateKeys.remove(alarmRuleId);
        alarmClearKeys.remove(alarmRuleId);
        entityKeys.clear();

        alarmRules.values().forEach(this::addAlarmRuleKeys);
    }

    private void addAlarmRuleKeys(AlarmRule alarmRule) {
        AlarmRuleConfiguration configuration = alarmRule.getConfiguration();
        Map<AlarmSeverity, Set<AlarmConditionFilterKey>> createAlarmKeys = alarmCreateKeys.computeIfAbsent(alarmRule.getId(), id -> new HashMap<>());
        configuration.getCreateRules().forEach(((severity, alarmRuleCondition) -> {
            var ruleKeys = createAlarmKeys.computeIfAbsent(severity, id -> new HashSet<>());
            for (var keyFilter : alarmRuleCondition.getCondition().getCondition()) {
                entityKeys.add(keyFilter.getKey());
                ruleKeys.add(keyFilter.getKey());
                addDynamicValuesRecursively(keyFilter.getPredicate(), entityKeys, ruleKeys);
            }
            addEntityKeysFromAlarmConditionSpec(alarmRuleCondition);
            AlarmSchedule schedule = alarmRuleCondition.getSchedule();
            if (schedule != null) {
                addScheduleDynamicValues(schedule);
            }
        }));
        if (configuration.getClearRule() != null) {
            var clearAlarmKeys = alarmClearKeys.computeIfAbsent(alarmRule.getId(), id -> new HashSet<>());
            for (var keyFilter : configuration.getClearRule().getCondition().getCondition()) {
                entityKeys.add(keyFilter.getKey());
                clearAlarmKeys.add(keyFilter.getKey());
                addDynamicValuesRecursively(keyFilter.getPredicate(), entityKeys, clearAlarmKeys);
            }
            addEntityKeysFromAlarmConditionSpec(configuration.getClearRule());
        }
    }

    private void addScheduleDynamicValues(AlarmSchedule schedule) {
        DynamicValue<String> dynamicValue = schedule.getDynamicValue();
        if (dynamicValue != null) {
            entityKeys.add(
                    new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                            dynamicValue.getSourceAttribute())
            );
        }
    }

    private void addEntityKeysFromAlarmConditionSpec(AlarmRuleCondition alarmRule) {
        AlarmConditionSpec spec = alarmRule.getCondition().getSpec();
        if (spec == null) {
            return;
        }
        AlarmConditionSpecType specType = spec.getType();
        switch (specType) {
            case DURATION:
                DurationAlarmConditionSpec duration = (DurationAlarmConditionSpec) spec;
                if (duration.getPredicate().getDynamicValue() != null
                        && duration.getPredicate().getDynamicValue().getSourceAttribute() != null) {
                    entityKeys.add(
                            new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                                    duration.getPredicate().getDynamicValue().getSourceAttribute())
                    );
                }
                break;
            case REPEATING:
                RepeatingAlarmConditionSpec repeating = (RepeatingAlarmConditionSpec) spec;
                if (repeating.getPredicate().getDynamicValue() != null
                        && repeating.getPredicate().getDynamicValue().getSourceAttribute() != null) {
                    entityKeys.add(
                            new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE,
                                    repeating.getPredicate().getDynamicValue().getSourceAttribute())
                    );
                }
                break;
        }
    }

    private void addDynamicValuesRecursively(KeyFilterPredicate predicate, Set<AlarmConditionFilterKey> entityKeys, Set<AlarmConditionFilterKey> ruleKeys) {
        switch (predicate.getType()) {
            case STRING:
            case NUMERIC:
            case BOOLEAN:
                DynamicValue value = ((SimpleKeyFilterPredicate) predicate).getValue().getDynamicValue();
                if (value != null && (value.getSourceType() == DynamicValueSourceType.CURRENT_TENANT ||
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

    Set<AlarmConditionFilterKey> getCreateAlarmKeys(AlarmRuleId id, AlarmSeverity severity) {
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

    Set<AlarmConditionFilterKey> getClearAlarmKeys(AlarmRuleId id) {
        Set<AlarmConditionFilterKey> keys = alarmClearKeys.get(id);
        if (keys == null) {
            return Collections.emptySet();
        } else {
            return keys;
        }
    }
}
