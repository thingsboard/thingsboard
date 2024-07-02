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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpecType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType;
import org.thingsboard.server.common.data.alarm.rule.condition.AttributeArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ConstantArgument;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpecType.DURATION;
import static org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionSpecType.NO_UPDATE;

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
    private final Map<String, AlarmRuleArgument> arguments;

    AlarmRuleState(AlarmSeverity severity, AlarmRuleCondition alarmRule, Set<AlarmConditionFilterKey> entityKeys,
                   PersistedAlarmRuleState state, DynamicPredicateValueCtx dynamicPredicateValueCtx, Map<String, AlarmRuleArgument> arguments) {
        this.severity = severity;
        this.alarmRule = alarmRule;
        this.entityKeys = entityKeys;
        this.state = Objects.requireNonNullElseGet(state, () -> new PersistedAlarmRuleState(0L, 0L, 0L));
        this.spec = getSpec(alarmRule);
        this.dynamicPredicateValueCtx = dynamicPredicateValueCtx;
        this.arguments = arguments;
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
        AlarmConditionSpec spec = alarmRule.getAlarmCondition().getSpec();
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
        if (!isActive(data, data.getTs())) {
            return AlarmEvalResult.FALSE;
        }
        return switch (spec.getType()) {
            case SIMPLE -> eval(alarmRule.getAlarmCondition(), data) ? AlarmEvalResult.TRUE : AlarmEvalResult.FALSE;
            case DURATION -> evalDuration(data);
            case REPEATING -> evalRepeating(data);
            case NO_UPDATE -> evalNoUpdate(data);
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
        EntityKeyValue value = getAttributeValue(data, (AttributeArgument) arguments.get(schedule.getArgumentId()));

        if (value != null) {
            try {
                return JsonConverter.parse(value.getJsonValue(), alarmRule.getSchedule().getClass());
            } catch (Exception e) {
                log.trace("Failed to parse AlarmSchedule from value: {}", value.getJsonValue(), e);
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

    private AlarmEvalResult evalRepeating(DataSnapshot data) {
        if (eval(alarmRule.getAlarmCondition(), data)) {
            state.setEventCount(state.getEventCount() + 1);
            updateFlag = true;
            long requiredRepeats = resolveRequiredRepeats(data);
            return state.getEventCount() >= requiredRepeats ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    private AlarmEvalResult evalDuration(DataSnapshot data) {
        if (eval(alarmRule.getAlarmCondition(), data)) {
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

    private AlarmEvalResult evalNoUpdate(DataSnapshot data) {
        Queue<AlarmConditionFilter> queue = new LinkedList<>();
        queue.add(alarmRule.getAlarmCondition().getConditionFilter());

        List<String> argumentIds = new ArrayList<>();

        while (!queue.isEmpty()) {
            AlarmConditionFilter condition = queue.poll();
            switch (condition.getType()) {
                case SIMPLE -> {
                    var simpleFilter = (SimpleAlarmConditionFilter) condition;
                    argumentIds.add(simpleFilter.getLeftArgId());
                }
                case COMPLEX -> {
                    var complexFilter = (ComplexAlarmConditionFilter) condition;
                    queue.addAll(complexFilter.getConditions());
                }
            }
        }

        for (String argId : argumentIds) {
            EntityKeyValue value = getValue(data, argId);
            if (value == null && state.getLastEventTs() > 0) {
                return getNoUpdateResult(data);
            } else if (value != null && data.getTs() > state.getLastEventTs()) {
                var result = state.getLastEventTs() > 0 ? getNoUpdateResult(data) : AlarmEvalResult.NOT_YET_TRUE;
                state.setLastEventTs(data.getTs());
                updateFlag = true;
                return result;
            }
        }

        return AlarmEvalResult.FALSE;
    }

    @NotNull
    private AlarmEvalResult getNoUpdateResult(DataSnapshot data) {
        long requiredDurationInMs = resolveRequiredDurationInMs(data);
        return data.getTs() - state.getLastEventTs() > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
    }

    private long resolveRequiredRepeats(DataSnapshot data) {
        long repeatingTimes = 0;
        AlarmConditionSpec alarmConditionSpec = getSpec();
        AlarmConditionSpecType specType = alarmConditionSpec.getType();
        if (specType.equals(AlarmConditionSpecType.REPEATING)) {
            RepeatingAlarmConditionSpec repeating = (RepeatingAlarmConditionSpec) spec;

            repeatingTimes = resolveValueForSpec(data, repeating.getArgumentId());
        }
        return repeatingTimes;
    }

    private long resolveRequiredDurationInMs(DataSnapshot data) {
        long durationTimeInMs = 0;
        AlarmConditionSpec alarmConditionSpec = getSpec();
        AlarmConditionSpecType specType = alarmConditionSpec.getType();
        if (specType == DURATION || specType == NO_UPDATE) {
            DurationAlarmConditionSpec duration = (DurationAlarmConditionSpec) spec;
            TimeUnit timeUnit = duration.getUnit();

            durationTimeInMs = timeUnit.toMillis(resolveValueForSpec(data, duration.getArgumentId()));
        }
        return durationTimeInMs;
    }

    private Long resolveValueForSpec(DataSnapshot data, String argId) {
        AlarmRuleArgument argument = arguments.get(argId);
        EntityKeyValue keyValue = getValue(data, argId);

        var longValue = getLongValue(keyValue);
        if (longValue == null) {
            throw new NumericParseException(String.format("Could not convert attribute '%s' with value '%s' to numeric value!", argument, getStrValue(keyValue)));
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
                    }
                }
                return AlarmEvalResult.FALSE;
            case NO_UPDATE:
                long requiredNoUpdateDurationInMs = resolveRequiredDurationInMs(dataSnapshot);
                if (requiredNoUpdateDurationInMs > 0 && state.getLastEventTs() > 0 && ts > state.getLastEventTs()) {
                    return ts - state.getLastEventTs() > requiredNoUpdateDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
                }
            default:
                return AlarmEvalResult.FALSE;
        }
    }

    private boolean eval(AlarmCondition condition, DataSnapshot data) {
        return eval(condition.getConditionFilter(), data);
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
        EntityKeyValue left = getValue(data, filter.getLeftArgId());
        EntityKeyValue right = getValue(data, filter.getRightArgId());

        if (left == null || right == null) {
            return false;
        }

        return switch (arguments.get(filter.getLeftArgId()).getValueType()) {
            case STRING -> filter.getOperation().process(getStrValue(left), getStrValue(right), filter.isIgnoreCase());
            case BOOLEAN -> filter.getOperation().process(getBoolValue(left), getBoolValue(right));
            case NUMERIC -> filter.getOperation().process(getDblValue(left), getDblValue(right));
            case DATE_TIME -> filter.getOperation().process(getLongValue(left), getLongValue(right));
        };
    }

    private EntityKeyValue getValue(DataSnapshot data, String argId) {
        var argument = arguments.get(argId);

        return switch (argument.getType()) {
            case CONSTANT -> {
                var constantArg = (ConstantArgument) argument;
                yield getValue(constantArg.getValueType(), constantArg.getValue());
            }
            case ATTRIBUTE -> {
                var attributeArg = (AttributeArgument) argument;
                var value = getAttributeValue(data, attributeArg);
                if (value == null) {
                    value = getValue(attributeArg.getValueType(), attributeArg.getDefaultValue());
                }
                yield value;
            }
            case FROM_MESSAGE -> data.getValue(argument.getKey());
        };
    }

    private EntityKeyValue getValue(ArgumentValueType valueType, Object value) {
        if (value == null) {
            return null;
        }
        try {
            EntityKeyValue keyValue = new EntityKeyValue();
            String valueStr = value.toString();
            switch (valueType) {
                case STRING -> keyValue.setStrValue(valueStr);
                case NUMERIC -> keyValue.setDblValue(Double.valueOf(valueStr));
                case BOOLEAN -> keyValue.setBoolValue(Boolean.valueOf(valueStr));
                case DATE_TIME -> keyValue.setLngValue(Long.valueOf(valueStr));
            }
            return keyValue;
        } catch (NumberFormatException e) {
            log.warn("[{}] Failed to parse value from argument: {}", valueType, value, e);
            return null;
        }
    }

    private EntityKeyValue getAttributeValue(DataSnapshot data, AttributeArgument argument) {
        AlarmConditionFilterKey key = argument.getKey();
        EntityKeyValue ekv = null;
        switch (argument.getSourceType()) {
            case CURRENT_ENTITY:
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
}
