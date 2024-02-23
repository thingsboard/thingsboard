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
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentType;
import org.thingsboard.server.common.data.alarm.rule.condition.FromMessageArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.ComplexAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.id.AlarmRuleId;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
            for (var argument : getArguments(alarmRuleCondition, configuration.getArguments())) {
                var key = argument.getKey();
                if (key != null) {
                    entityKeys.add(key);
                    ruleKeys.add(key);
                }
            }
        }));
        if (configuration.getClearRule() != null) {
            var clearAlarmKeys = alarmClearKeys.computeIfAbsent(alarmRule.getId(), id -> new HashSet<>());
            for (var argument : getArguments(configuration.getClearRule(), configuration.getArguments())) {
                var key = argument.getKey();
                if (key != null) {
                    entityKeys.add(key);
                    clearAlarmKeys.add(key);
                }
            }
        }
    }

    private Set<AlarmRuleArgument> getArguments(AlarmRuleCondition condition, Map<String, AlarmRuleArgument> argumentMap) {
        Set<String> argumentIds = new HashSet<>();

        if (condition.getSchedule() != null) {
            String scheduleArgId = condition.getSchedule().getArgumentId();
            if (scheduleArgId != null) {
                argumentIds.add(scheduleArgId);
            }
        }

        if (condition.getAlarmCondition().getSpec() != null) {
            String specArgId = condition.getAlarmCondition().getSpec().getArgumentId();
            if (specArgId != null) {
                argumentIds.add(specArgId);
            }
        }

        addArgumentIds(condition.getAlarmCondition().getConditionFilter(), argumentIds);
        return argumentIds.stream().map(argumentMap::get).collect(Collectors.toSet());
    }

    private void addArgumentIds(AlarmConditionFilter rootCondition, Set<String> argumentIds) {
        Queue<AlarmConditionFilter> queue = new LinkedList<>();
        queue.add(rootCondition);

        while (!queue.isEmpty()) {
            AlarmConditionFilter condition = queue.poll();
            switch (condition.getType()) {
                case SIMPLE -> {
                    var simpleFilter = (SimpleAlarmConditionFilter) condition;
                    argumentIds.add(simpleFilter.getLeftArgId());
                    String rightArgId = simpleFilter.getRightArgId();
                    // No update hasn't rightArgId
                    if (rightArgId != null) {
                        argumentIds.add(rightArgId);
                    }
                }
                case COMPLEX -> {
                    var complexFilter = (ComplexAlarmConditionFilter) condition;
                    queue.addAll(complexFilter.getConditions());
                }
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
