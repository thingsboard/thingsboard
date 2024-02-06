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
package org.thingsboard.server.service.alarm.rule;

import lombok.AccessLevel;
import lombok.Getter;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.id.AlarmRuleId;

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
        var keys = alarmClearKeys.get(alarmRule.getId());
        if (keys != null) {
            keys.clear();
        }
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
            for (var argument : alarmRuleCondition.getArguments().values()) {
                if (!argument.isConstant()) {
                    entityKeys.add(argument.getKey());
                    ruleKeys.add(argument.getKey());
                }
            }
        }));
        if (configuration.getClearRule() != null) {
            var clearAlarmKeys = alarmClearKeys.computeIfAbsent(alarmRule.getId(), id -> new HashSet<>());
            for (var argument : configuration.getClearRule().getArguments().values()) {
                if (!argument.isConstant())
                    entityKeys.add(argument.getKey());
                clearAlarmKeys.add(argument.getKey());
            }
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
