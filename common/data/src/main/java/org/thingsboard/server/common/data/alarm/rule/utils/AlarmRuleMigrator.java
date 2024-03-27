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
package org.thingsboard.server.common.data.alarm.rule.utils;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.AnyTimeSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentType;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType;
import org.thingsboard.server.common.data.alarm.rule.condition.AttributeArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ComplexAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.ConstantArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.CustomTimeSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.FromMessageArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.SpecificTimeSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.ValueSourceType;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.device.profile.alarm.rule.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.alarm.rule.OldAlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.alarm.rule.OldAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.alarm.rule.OldAlarmRule;
import org.thingsboard.server.common.data.device.profile.alarm.rule.OldAlarmSchedule;
import org.thingsboard.server.common.data.device.profile.alarm.rule.OldCustomTimeSchedule;
import org.thingsboard.server.common.data.device.profile.alarm.rule.OldDurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.alarm.rule.OldRepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.alarm.rule.OldSpecificTimeSchedule;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.SimpleKeyFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class AlarmRuleMigrator {

    private AlarmRuleMigrator() {
    }

    public static AlarmRule migrate(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileAlarm oldRule) {
        var alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType(oldRule.getAlarmType());
        alarmRule.setName(deviceProfile.getName() + " - " + oldRule.getAlarmType());
        alarmRule.setEnabled(true);

        var configuration = new AlarmRuleConfiguration();
        configuration.setSourceEntityFilters(Collections.singletonList(new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()))));
        configuration.setPropagate(oldRule.isPropagate());
        configuration.setPropagateToOwner(oldRule.isPropagateToOwner());
        configuration.setPropagateToTenant(oldRule.isPropagateToTenant());
        configuration.setPropagateRelationTypes(oldRule.getPropagateRelationTypes());

        Map<TbPair<String, Integer>, AlarmRuleArgument> arguments = new HashMap<>();

        TreeMap<AlarmSeverity, AlarmRuleCondition> createRules = new TreeMap<>();
        oldRule.getCreateRules().forEach((severity, oldAlarmRule) -> createRules.put(severity, getAlarmRuleCondition(oldAlarmRule, arguments)));
        configuration.setCreateRules(createRules);
        if (oldRule.getClearRule() != null) {
            configuration.setClearRule(getAlarmRuleCondition(oldRule.getClearRule(), arguments));
        }

        configuration.setArguments(arguments.entrySet().stream().collect(Collectors.toMap(entry -> getArgumentId(entry.getKey()), Map.Entry::getValue)));
        alarmRule.setConfiguration(configuration);
        return alarmRule;
    }

    private static AlarmRuleCondition getAlarmRuleCondition(OldAlarmRule oldRule, Map<TbPair<String, Integer>, AlarmRuleArgument> arguments) {
        var newCondition = new AlarmRuleCondition();
        newCondition.setDashboardId(oldRule.getDashboardId());
        newCondition.setAlarmDetails(oldRule.getAlarmDetails());

        newCondition.setSchedule(getSchedule(oldRule.getSchedule(), arguments));

        var oldAlarmCondition = oldRule.getCondition();

        var alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(getAlarmConditionSpec(oldAlarmCondition.getSpec(), arguments));

        List<AlarmConditionFilter> filters = oldAlarmCondition.getCondition().stream().map(oldFilter -> getConditionFilter(oldFilter, arguments)).collect(Collectors.toList());

        AlarmConditionFilter rootCondition;

        if (filters.size() > 1) {
            rootCondition = new ComplexAlarmConditionFilter(filters, ComplexAlarmConditionFilter.ComplexOperation.AND);
        } else {
            rootCondition = filters.get(0);
        }

        alarmCondition.setConditionFilter(rootCondition);
        newCondition.setAlarmCondition(alarmCondition);

        return newCondition;
    }

    private static AlarmConditionFilter getConditionFilter(OldAlarmConditionFilter oldFilter, Map<TbPair<String, Integer>, AlarmRuleArgument> arguments) {
        ArgumentValueType valueType = getValueType(oldFilter.getValueType());

        AlarmRuleArgument leftArg;

        var leftArgKey = oldFilter.getKey();

        if (leftArgKey.getType() == AlarmConditionKeyType.CONSTANT) {
            leftArg = new ConstantArgument(valueType, oldFilter.getValue(), leftArgKey.getKey());
        } else {
            leftArg = new FromMessageArgument(leftArgKey, valueType);
        }

        String leftArgId = addArgumentAndGetId(leftArg, arguments);

        return getConditionFilter(oldFilter.getPredicate(), leftArgId, valueType, arguments);
    }

    private static AlarmConditionFilter getConditionFilter(KeyFilterPredicate predicate, String leftArgId, ArgumentValueType valueType, Map<TbPair<String, Integer>, AlarmRuleArgument> arguments) {
        if (predicate.getType() == FilterPredicateType.COMPLEX) {
            return getComplexConditionFilter((ComplexFilterPredicate) predicate, leftArgId, valueType, arguments);
        } else {
            return getSimpleConditionFilter((SimpleKeyFilterPredicate<?>) predicate, leftArgId, valueType, arguments);
        }
    }

    private static ComplexAlarmConditionFilter getComplexConditionFilter(ComplexFilterPredicate complexPredicate, String leftArgId, ArgumentValueType valueType, Map<TbPair<String, Integer>, AlarmRuleArgument> arguments) {
        var conditions = complexPredicate.getPredicates().stream().map(predicate -> getConditionFilter(predicate, leftArgId, valueType, arguments)).toList();
        return new ComplexAlarmConditionFilter(conditions, ComplexAlarmConditionFilter.ComplexOperation.valueOf(complexPredicate.getOperationName()));
    }

    private static SimpleAlarmConditionFilter getSimpleConditionFilter(SimpleKeyFilterPredicate<?> simplePredicate, String leftArgId, ArgumentValueType valueType, Map<TbPair<String, Integer>, AlarmRuleArgument> arguments) {
        var simpleFilter = new SimpleAlarmConditionFilter();
        simpleFilter.setLeftArgId(leftArgId);
        simpleFilter.setOperation(ArgumentOperation.valueOf(simplePredicate.getOperationName()));

        if (simplePredicate.getType() == FilterPredicateType.STRING) {
            simpleFilter.setIgnoreCase(((StringFilterPredicate) simplePredicate).isIgnoreCase());
        }

        var predicateValue = simplePredicate.getValue();
        var leftArgument = arguments.get(new TbPair<>(leftArgId.substring(0, leftArgId.lastIndexOf("_1")), 1));
        String constantDescription;
        if (leftArgument.getType() == ArgumentType.CONSTANT) {
            constantDescription = ((ConstantArgument)leftArgument).getDescription();
        } else {
            constantDescription = leftArgument.getKey().getKey();
        }
        var rightArgument = createArgument(predicateValue.getDynamicValue(), valueType, predicateValue.getDefaultValue(), constantDescription);

        simpleFilter.setRightArgId(addArgumentAndGetId(rightArgument, arguments));

        return simpleFilter;
    }

    private static ArgumentValueType getValueType(EntityKeyValueType keyValueType) {
        return switch (keyValueType) {
            case STRING -> ArgumentValueType.STRING;
            case BOOLEAN -> ArgumentValueType.BOOLEAN;
            case NUMERIC -> ArgumentValueType.NUMERIC;
            case DATE_TIME -> ArgumentValueType.DATE_TIME;
        };
    }

    private static AlarmConditionSpec getAlarmConditionSpec(OldAlarmConditionSpec oldSpec, Map<TbPair<String, Integer>, AlarmRuleArgument> arguments) {
        return switch (oldSpec.getType()) {
            case SIMPLE -> new SimpleAlarmConditionSpec();
            case REPEATING -> {
                var oldRepeating = (OldRepeatingAlarmConditionSpec) oldSpec;
                var repeating = new RepeatingAlarmConditionSpec();
                var predicate = oldRepeating.getPredicate();
                AlarmRuleArgument argument = createArgument(predicate.getDynamicValue(), ArgumentValueType.NUMERIC, predicate.getDefaultValue(), "repeating");
                repeating.setArgumentId(addArgumentAndGetId(argument, arguments));
                yield repeating;
            }
            case DURATION -> {
                var oldDuration = (OldDurationAlarmConditionSpec) oldSpec;
                var duration = new DurationAlarmConditionSpec();
                var predicate = oldDuration.getPredicate();
                AlarmRuleArgument argument = createArgument(predicate.getDynamicValue(), ArgumentValueType.NUMERIC, predicate.getDefaultValue(), "duration");
                duration.setArgumentId(addArgumentAndGetId(argument, arguments));
                duration.setUnit(oldDuration.getUnit());
                yield duration;
            }
            case NO_UPDATE -> null;
        };
    }

    private static AlarmSchedule getSchedule(OldAlarmSchedule oldSchedule, Map<TbPair<String, Integer>, AlarmRuleArgument> arguments) {
        if (oldSchedule == null) {
            return null;
        }

        return switch (oldSchedule.getType()) {
            case ANY_TIME -> new AnyTimeSchedule();
            case SPECIFIC_TIME -> {
                var oldSpecific = (OldSpecificTimeSchedule) oldSchedule;
                var specific = new SpecificTimeSchedule();
                specific.setTimezone(oldSpecific.getTimezone());
                specific.setDaysOfWeek(oldSpecific.getDaysOfWeek());
                specific.setStartsOn(oldSpecific.getStartsOn());
                specific.setEndsOn(oldSpecific.getEndsOn());
                AlarmRuleArgument argument = createArgument(oldSpecific.getDynamicValue(), ArgumentValueType.STRING, null);
                specific.setArgumentId(addArgumentAndGetId(argument, arguments));
                yield specific;
            }
            case CUSTOM -> {
                var oldCustom = (OldCustomTimeSchedule) oldSchedule;
                var custom = new CustomTimeSchedule();
                custom.setTimezone(oldCustom.getTimezone());
                custom.setItems(oldCustom.getItems());
                AlarmRuleArgument argument = createArgument(oldCustom.getDynamicValue(), ArgumentValueType.STRING, null);
                custom.setArgumentId(addArgumentAndGetId(argument, arguments));
                yield custom;
            }
        };
    }

    private static String addArgumentAndGetId(AlarmRuleArgument argument, Map<TbPair<String, Integer>, AlarmRuleArgument> arguments) {
        if (argument == null) {
            return null;
        }

        String argumentId = getArgumentIdIgnoreValue(argument);
        var keyPair = new TbPair<>(argumentId, 1);

        AlarmRuleArgument value;

        while ((value = arguments.get(keyPair)) != null) {
            if (!argument.equals(value)) {
                keyPair.setSecond(keyPair.getSecond() + 1);
            } else {
                break;
            }
        }

        arguments.putIfAbsent(keyPair, argument);
        return getArgumentId(keyPair);
    }

    private static String getArgumentId(TbPair<String, Integer> keyPair) {
        return String.format("%s_%d", keyPair.getFirst(), keyPair.getSecond());
    }

    private static AlarmRuleArgument createArgument(DynamicValue<?> dynamicValue, ArgumentValueType argumentValueType, Object value) {
        return createArgument(dynamicValue, argumentValueType, value, null);
    }

    private static AlarmRuleArgument createArgument(DynamicValue<?> dynamicValue, ArgumentValueType argumentValueType, Object value, String constantDescription) {
        if (dynamicValue != null && dynamicValue.getSourceAttribute() != null) {
            return new AttributeArgument(dynamicValue.getSourceAttribute(), argumentValueType, getValueSourceType(dynamicValue.getSourceType()), value, dynamicValue.isInherit());
        } else if (value != null) {
            return new ConstantArgument(argumentValueType, value, constantDescription);
        } else {
            return null;
        }
    }

    private static ValueSourceType getValueSourceType(DynamicValueSourceType dynamicValueSourceType) {
        return switch (dynamicValueSourceType) {
            case CURRENT_TENANT -> ValueSourceType.CURRENT_TENANT;
            case CURRENT_CUSTOMER -> ValueSourceType.CURRENT_CUSTOMER;
            case CURRENT_DEVICE, CURRENT_USER -> ValueSourceType.CURRENT_ENTITY;
        };
    }

    private static String getArgumentIdIgnoreValue(AlarmRuleArgument argument) {
        StringJoiner sj = new StringJoiner("_");
        sj.add(argument.getType().name().toLowerCase());
        sj.add(argument.getValueType().name().toLowerCase());

        switch (argument.getType()) {
            case CONSTANT -> {
                var constArg = (ConstantArgument) argument;
                if (constArg.getDescription() != null) {
                    sj.add(constArg.getDescription());
                }
            }
            case ATTRIBUTE -> {
                var attrArg = (AttributeArgument) argument;
                sj.add(attrArg.getAttribute());
                sj.add(attrArg.getSourceType().name().toLowerCase());
            }
            case FROM_MESSAGE -> {
                sj.add(argument.getKey().getType().name().toLowerCase());
                sj.add(argument.getKey().getKey());
            }
        }

        return sj.toString();
    }
}
