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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpecType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.ComplexAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.CustomTimeSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.CustomTimeScheduleItem;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.SpecificTimeSchedule;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.service.alarm.rule.state.PersistedAlarmRuleState;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
class AlarmRuleState {

    private final AlarmSeverity severity;
    private final AlarmRuleCondition alarmRule;
    private final AlarmConditionSpec spec;
    private final Set<AlarmConditionFilterKey> entityKeys;
    private PersistedAlarmRuleState state;
    private boolean updateFlag;
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;

    AlarmRuleState(AlarmSeverity severity, AlarmRuleCondition alarmRule, Set<AlarmConditionFilterKey> entityKeys,
                   PersistedAlarmRuleState state, DynamicPredicateValueCtx dynamicPredicateValueCtx) {
        this.severity = severity;
        this.alarmRule = alarmRule;
        this.entityKeys = entityKeys;
        if (state != null) {
            this.state = state;
        } else {
            this.state = new PersistedAlarmRuleState(0L, 0L, 0L);
        }
        this.spec = getSpec(alarmRule);
        this.dynamicPredicateValueCtx = dynamicPredicateValueCtx;
    }

    public boolean validateTsUpdate(Set<AlarmConditionFilterKey> changedKeys) {
        for (AlarmConditionFilterKey key : changedKeys) {
            if (entityKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean validateAttrUpdate(Set<AlarmConditionFilterKey> changedKeys) {
        //If the attribute was updated, but no new telemetry arrived - we ignore this until new telemetry is there.
        for (AlarmConditionFilterKey key : entityKeys) {
            if (key.getType().equals(AlarmConditionKeyType.TIME_SERIES)) {
                return false;
            }
        }
        for (AlarmConditionFilterKey key : changedKeys) {
            if (entityKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public AlarmConditionSpec getSpec(AlarmRuleCondition alarmRule) {
        AlarmConditionSpec spec = alarmRule.getCondition().getSpec();
        if (spec == null) {
            spec = new SimpleAlarmConditionSpec();
        }
        return spec;
    }

    public boolean checkUpdate() {
        if (updateFlag) {
            updateFlag = false;
            return true;
        } else {
            return false;
        }
    }

    public AlarmEvalResult eval(DataSnapshot data) {
        boolean active = isActive(data, data.getTs());
        return switch (spec.getType()) {
            case SIMPLE -> (active && eval(alarmRule.getCondition(), data)) ? AlarmEvalResult.TRUE : AlarmEvalResult.FALSE;
            case DURATION -> evalDuration(data, active);
            case REPEATING -> evalRepeating(data, active);
        };
    }

    private boolean isActive(DataSnapshot data, long eventTs) {
        if (eventTs == 0L) {
            eventTs = System.currentTimeMillis();
        }
        if (alarmRule.getSchedule() == null) {
            return true;
        }
        return switch (alarmRule.getSchedule().getType()) {
            case ANY_TIME -> true;
            case SPECIFIC_TIME -> isActiveSpecific((SpecificTimeSchedule) getSchedule(data, alarmRule), eventTs);
            case CUSTOM -> isActiveCustom((CustomTimeSchedule) getSchedule(data, alarmRule), eventTs);
        };
    }

    private AlarmSchedule getSchedule(DataSnapshot data, AlarmRuleCondition alarmRule) {
        AlarmSchedule schedule = alarmRule.getSchedule();
        EntityKeyValue dynamicValue = getDynamicPredicateValue(data, getArgument(schedule.getArgumentId()));

        if (dynamicValue != null) {
            try {
                return JsonConverter.parse(dynamicValue.getJsonValue(), alarmRule.getSchedule().getClass());
            } catch (Exception e) {
                log.trace("Failed to parse AlarmSchedule from dynamicValue: {}", dynamicValue.getJsonValue(), e);
            }
        }
        return schedule;
    }

    private boolean isActiveSpecific(SpecificTimeSchedule schedule, long eventTs) {
        ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        if (schedule.getDaysOfWeek().size() != 7) {
            int dayOfWeek = zdt.getDayOfWeek().getValue();
            if (!schedule.getDaysOfWeek().contains(dayOfWeek)) {
                return false;
            }
        }
        long endsOn = schedule.getEndsOn();
        if (endsOn == 0) {
            // 24 hours in milliseconds
            endsOn = 86400000;
        }

        return isActive(eventTs, zoneId, zdt, schedule.getStartsOn(), endsOn);
    }

    private boolean isActiveCustom(CustomTimeSchedule schedule, long eventTs) {
        ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        int dayOfWeek = zdt.toLocalDate().getDayOfWeek().getValue();
        for (CustomTimeScheduleItem item : schedule.getItems()) {
            if (item.getDayOfWeek() == dayOfWeek) {
                if (item.isEnabled()) {
                    long endsOn = item.getEndsOn();
                    if (endsOn == 0) {
                        // 24 hours in milliseconds
                        endsOn = 86400000;
                    }
                    return isActive(eventTs, zoneId, zdt, item.getStartsOn(), endsOn);
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isActive(long eventTs, ZoneId zoneId, ZonedDateTime zdt, long startsOn, long endsOn) {
        long startOfDay = zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli();
        long msFromStartOfDay = eventTs - startOfDay;
        if (startsOn <= endsOn) {
            return startsOn <= msFromStartOfDay && endsOn > msFromStartOfDay;
        } else {
            return startsOn < msFromStartOfDay || (0 < msFromStartOfDay && msFromStartOfDay < endsOn);
        }
    }

    public void clear() {
        if (state.getEventCount() > 0 || state.getLastEventTs() > 0 || state.getDuration() > 0) {
            state.setEventCount(0L);
            state.setLastEventTs(0L);
            state.setDuration(0L);
            updateFlag = true;
        }
    }

    private AlarmEvalResult evalRepeating(DataSnapshot data, boolean active) {
        if (active && eval(alarmRule.getCondition(), data)) {
            state.setEventCount(state.getEventCount() + 1);
            updateFlag = true;
            long requiredRepeats = resolveRequiredRepeats(data);
            return state.getEventCount() >= requiredRepeats ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    private AlarmEvalResult evalDuration(DataSnapshot data, boolean active) {
        if (active && eval(alarmRule.getCondition(), data)) {
            if (state.getLastEventTs() > 0) {
                if (data.getTs() > state.getLastEventTs()) {
                    state.setDuration(state.getDuration() + (data.getTs() - state.getLastEventTs()));
                    state.setLastEventTs(data.getTs());
                    updateFlag = true;
                }
            } else {
                state.setLastEventTs(data.getTs());
                state.setDuration(0L);
                updateFlag = true;
            }
            long requiredDurationInMs = resolveRequiredDurationInMs(data);
            return state.getDuration() > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    private long resolveRequiredRepeats(DataSnapshot data) {
        long repeatingTimes = 0;
        AlarmConditionSpec alarmConditionSpec = getSpec();
        AlarmConditionSpecType specType = alarmConditionSpec.getType();
        if (specType.equals(AlarmConditionSpecType.REPEATING)) {
            RepeatingAlarmConditionSpec repeating = (RepeatingAlarmConditionSpec) spec;

            repeatingTimes = resolveDynamicValue(data, repeating.getArgId());
        }
        return repeatingTimes;
    }

    private long resolveRequiredDurationInMs(DataSnapshot data) {
        long durationTimeInMs = 0;
        AlarmConditionSpec alarmConditionSpec = getSpec();
        AlarmConditionSpecType specType = alarmConditionSpec.getType();
        if (specType.equals(AlarmConditionSpecType.DURATION)) {
            DurationAlarmConditionSpec duration = (DurationAlarmConditionSpec) spec;
            TimeUnit timeUnit = duration.getUnit();

            durationTimeInMs = timeUnit.toMillis(resolveDynamicValue(data, duration.getArgId()));
        }
        return durationTimeInMs;
    }

    private Long resolveDynamicValue(DataSnapshot data, String argId) {
        AlarmRuleArgument argument = getArgument(argId);
        Long defaultValue = Double.valueOf(argument.getDefaultValue().toString()).longValue();
        if (argument.isConstant()) {
            return defaultValue;
        }

        EntityKeyValue keyValue = getDynamicPredicateValue(data, argument);
        if (keyValue == null) {
            return defaultValue;
        }

        var longValue = getLongValue(keyValue);
        if (longValue == null) {
            String sourceAttribute = argument.getKey().getKey();
            throw new NumericParseException(String.format("Could not convert attribute '%s' with value '%s' to numeric value!", sourceAttribute, getStrValue(keyValue)));
        }
        return longValue;
    }

    public AlarmEvalResult eval(long ts, DataSnapshot dataSnapshot) {
        switch (spec.getType()) {
            case SIMPLE:
            case REPEATING:
                return AlarmEvalResult.NOT_YET_TRUE;
            case DURATION:
                long requiredDurationInMs = resolveRequiredDurationInMs(dataSnapshot);
                if (requiredDurationInMs > 0 && state.getLastEventTs() > 0 && ts > state.getLastEventTs()) {
                    long duration = state.getDuration() + (ts - state.getLastEventTs());
                    if (isActive(dataSnapshot, ts)) {
                        return duration > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
                    } else {
                        return AlarmEvalResult.FALSE;
                    }
                }
            default:
                return AlarmEvalResult.FALSE;
        }
    }

    private boolean eval(AlarmCondition condition, DataSnapshot data) {
        return eval(condition.getCondition(), data);
    }

    private boolean eval(AlarmConditionFilter filter, DataSnapshot data) {
        return switch (filter.getType()) {
            case SIMPLE -> eval((SimpleAlarmConditionFilter) filter, data);
            case COMPLEX -> eval((ComplexAlarmConditionFilter) filter, data);
        };
    }

    private boolean eval(ComplexAlarmConditionFilter complexFilter, DataSnapshot data) {
        switch (complexFilter.getOperation()) {
            case OR -> {
                for (AlarmConditionFilter filter : complexFilter.getConditions()) {
                    if (eval(filter, data)) {
                        return true;
                    }
                }
                return false;
            }
            case AND -> {
                for (AlarmConditionFilter filter : complexFilter.getConditions()) {
                    if (!eval(filter, data)) {
                        return false;
                    }
                }
                return true;
            }
            default -> throw new RuntimeException("Operation not supported: " + complexFilter.getOperation());
        }
    }

    private boolean eval(SimpleAlarmConditionFilter filter, DataSnapshot data) {
        EntityKeyValue left = getValue(filter.getLeftArgId(), data);
        EntityKeyValue right = getValue(filter.getRightArgId(), data);

        return switch (getArgument(filter.getLeftArgId()).getValueType()) {
            case STRING -> filter.getOperation().process(getStrValue(left), getStrValue(right));
            case BOOLEAN -> filter.getOperation().process(getBoolValue(left), getBoolValue(right));
            case NUMERIC -> filter.getOperation().process(getDblValue(left), getDblValue(right));
        };
    }

    private EntityKeyValue getValue(String argId, DataSnapshot data) {
        var argument = getArgument(argId);
        EntityKeyValue value;
        if (argument.isConstant()) {
            try {
                value = getDefaultValue(argument);
            } catch (RuntimeException e) {
                log.warn("[{}] Failed to parse constant value from argument: {}", argId, argument, e);
                value = null;
            }
        } else if (argument.isDynamic()) {
            value = getDynamicPredicateValue(data, argument);
            if (value == null) {
                value = getDefaultValue(argument);
            }
        } else {
            value = data.getValue(argument.getKey());
        }
        return value;
    }

    private EntityKeyValue getDefaultValue(AlarmRuleArgument argument) {
        EntityKeyValue value = new EntityKeyValue();
        String valueStr = argument.getDefaultValue().toString();
        switch (argument.getValueType()) {
            case STRING -> value.setStrValue(valueStr);
            case NUMERIC -> value.setDblValue(Double.valueOf(valueStr));
            case BOOLEAN -> value.setBoolValue(Boolean.valueOf(valueStr));
        }
        return value;
    }

    private EntityKeyValue getDynamicPredicateValue(DataSnapshot data, AlarmRuleArgument argument) {
        AlarmConditionFilterKey key = argument.getKey();
        EntityKeyValue ekv = null;
        switch (argument.getSourceType()) {
            case CURRENT_DEVICE:
                ekv = data.getValue(key);
                if (ekv != null || !argument.isInherit()) {
                    break;
                }
            case CURRENT_CUSTOMER:
                ekv = dynamicPredicateValueCtx.getCustomerValue(key.getKey());
                if (ekv != null || !argument.isInherit()) {
                    break;
                }
            case CURRENT_TENANT:
                ekv = dynamicPredicateValueCtx.getTenantValue(key.getKey());
                break;
        }
        return ekv;
    }

    private static String getStrValue(EntityKeyValue ekv) {
        return switch (ekv.getDataType()) {
            case LONG -> ekv.getLngValue() != null ? ekv.getLngValue().toString() : null;
            case DOUBLE -> ekv.getDblValue() != null ? ekv.getDblValue().toString() : null;
            case BOOLEAN -> ekv.getBoolValue() != null ? ekv.getBoolValue().toString() : null;
            case STRING -> ekv.getStrValue();
            case JSON -> ekv.getJsonValue();
        };
    }

    private static Double getDblValue(EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue() != null ? ekv.getLngValue().doubleValue() : null;
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue() : null;
            case BOOLEAN:
                return ekv.getBoolValue() != null ? (ekv.getBoolValue() ? 1.0 : 0.0) : null;
            case STRING:
                try {
                    return Double.parseDouble(ekv.getStrValue());
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Double.parseDouble(ekv.getJsonValue());
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private static Boolean getBoolValue(EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue() != null ? ekv.getLngValue() > 0 : null;
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue() > 0 : null;
            case BOOLEAN:
                return ekv.getBoolValue();
            case STRING:
                try {
                    return Boolean.parseBoolean(ekv.getStrValue());
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Boolean.parseBoolean(ekv.getJsonValue());
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private static Long getLongValue(EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue();
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue().longValue() : null;
            case BOOLEAN:
                return ekv.getBoolValue() != null ? (ekv.getBoolValue() ? 1 : 0L) : null;
            case STRING:
                try {
                    return Long.parseLong(ekv.getStrValue());
                } catch (RuntimeException e) {
                    return null;
                }
            case JSON:
                try {
                    return Long.parseLong(ekv.getJsonValue());
                } catch (RuntimeException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private AlarmRuleArgument getArgument(String id) {
        return alarmRule.getCondition().getArguments().get(id);
    }
}
