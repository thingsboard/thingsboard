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

import lombok.Data;
import org.thingsboard.rule.engine.profile.state.PersistedAlarmRuleState;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.CustomTimeSchedule;
import org.thingsboard.server.common.data.device.profile.CustomTimeScheduleItem;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.SpecificTimeSchedule;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;

@Data
public class AlarmRuleState {

    private final AlarmSeverity severity;
    private final AlarmRule alarmRule;
    private final AlarmConditionSpec spec;
    private final long requiredDurationInMs;
    private final long requiredRepeats;
    private PersistedAlarmRuleState state;
    private boolean updateFlag;

    public AlarmRuleState(AlarmSeverity severity, AlarmRule alarmRule, PersistedAlarmRuleState state) {
        this.severity = severity;
        this.alarmRule = alarmRule;
        if (state != null) {
            this.state = state;
        } else {
            this.state = new PersistedAlarmRuleState(0L, 0L, 0L);
        }
        this.spec = getSpec(alarmRule);
        long requiredDurationInMs = 0;
        long requiredRepeats = 0;
        switch (spec.getType()) {
            case DURATION:
                DurationAlarmConditionSpec duration = (DurationAlarmConditionSpec) spec;
                requiredDurationInMs = duration.getUnit().toMillis(duration.getValue());
                break;
            case REPEATING:
                RepeatingAlarmConditionSpec repeating = (RepeatingAlarmConditionSpec) spec;
                requiredRepeats = repeating.getCount();
                break;
        }
        this.requiredDurationInMs = requiredDurationInMs;
        this.requiredRepeats = requiredRepeats;
    }

    public AlarmConditionSpec getSpec(AlarmRule alarmRule) {
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

    public boolean eval(DeviceDataSnapshot data) {
        boolean active = isActive(data.getTs());
        switch (spec.getType()) {
            case SIMPLE:
                return active && eval(alarmRule.getCondition(), data);
            case DURATION:
                return evalDuration(data, active);
            case REPEATING:
                return evalRepeating(data, active);
            default:
                return false;
        }
    }

    private boolean isActive(long eventTs) {
        if (eventTs == 0L) {
            eventTs = System.currentTimeMillis();
        }
        if (alarmRule.getSchedule() == null) {
            return true;
        }
        switch (alarmRule.getSchedule().getType()) {
            case ANY_TIME:
                return true;
            case SPECIFIC_TIME:
                return isActiveSpecific((SpecificTimeSchedule) alarmRule.getSchedule(), eventTs);
            case CUSTOM:
                return isActiveCustom((CustomTimeSchedule) alarmRule.getSchedule(), eventTs);
            default:
                throw new RuntimeException("Unsupported schedule type: " + alarmRule.getSchedule().getType());
        }
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
        long startOfDay = zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli();
        long msFromStartOfDay = eventTs - startOfDay;
        return schedule.getStartsOn() <= msFromStartOfDay && schedule.getEndsOn() > msFromStartOfDay;
    }

    private boolean isActiveCustom(CustomTimeSchedule schedule, long eventTs) {
        ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        int dayOfWeek = zdt.toLocalDate().getDayOfWeek().getValue();
        for (CustomTimeScheduleItem item : schedule.getItems()) {
            if (item.getDayOfWeek() == dayOfWeek) {
                if (item.isEnabled()) {
                    long startOfDay = zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli();
                    long msFromStartOfDay = eventTs - startOfDay;
                    return item.getStartsOn() <= msFromStartOfDay && item.getEndsOn() > msFromStartOfDay;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean evalRepeating(DeviceDataSnapshot data, boolean active) {
        if (active && eval(alarmRule.getCondition(), data)) {
            state.setEventCount(state.getEventCount() + 1);
            updateFlag = true;
            return state.getEventCount() > requiredRepeats;
        } else {
            if (state.getEventCount() > 0) {
                state.setEventCount(0L);
                updateFlag = true;
            }
            return false;
        }
    }

    private boolean evalDuration(DeviceDataSnapshot data, boolean active) {
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
            return state.getDuration() > requiredDurationInMs;
        } else {
            if (state.getLastEventTs() > 0 || state.getDuration() > 0) {
                state.setLastEventTs(0L);
                state.setDuration(0L);
                updateFlag = true;
            }
            return false;
        }
    }

    public boolean eval(long ts) {
        switch (spec.getType()) {
            case SIMPLE:
            case REPEATING:
                return false;
            case DURATION:
                if (requiredDurationInMs > 0 && state.getLastEventTs() > 0 && ts > state.getLastEventTs()) {
                    long duration = state.getDuration() + (ts - state.getLastEventTs());
                    boolean result = duration > requiredDurationInMs && isActive(ts);
                    if (result) {
                        state.setLastEventTs(0L);
                        state.setDuration(0L);
                        updateFlag = true;
                    }
                    return result;
                }
            default:
                return false;
        }
    }

    private boolean eval(AlarmCondition condition, DeviceDataSnapshot data) {
        boolean eval = true;
        for (KeyFilter keyFilter : condition.getCondition()) {
            EntityKeyValue value = data.getValue(keyFilter.getKey());
            if (value == null) {
                return false;
            }
            eval = eval && eval(value, keyFilter.getPredicate());
        }
        return eval;
    }

    private boolean eval(EntityKeyValue value, KeyFilterPredicate predicate) {
        switch (predicate.getType()) {
            case STRING:
                return evalStrPredicate(value, (StringFilterPredicate) predicate);
            case NUMERIC:
                return evalNumPredicate(value, (NumericFilterPredicate) predicate);
            case COMPLEX:
                return evalComplexPredicate(value, (ComplexFilterPredicate) predicate);
            case BOOLEAN:
                return evalBoolPredicate(value, (BooleanFilterPredicate) predicate);
            default:
                return false;
        }
    }

    private boolean evalComplexPredicate(EntityKeyValue ekv, ComplexFilterPredicate predicate) {
        switch (predicate.getOperation()) {
            case OR:
                for (KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (eval(ekv, kfp)) {
                        return true;
                    }
                }
                return false;
            case AND:
                for (KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (!eval(ekv, kfp)) {
                        return false;
                    }
                }
                return true;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalBoolPredicate(EntityKeyValue ekv, BooleanFilterPredicate predicate) {
        Boolean value;
        switch (ekv.getDataType()) {
            case LONG:
                value = ekv.getLngValue() > 0;
                break;
            case DOUBLE:
                value = ekv.getDblValue() > 0;
                break;
            case BOOLEAN:
                value = ekv.getBoolValue();
                break;
            case STRING:
                try {
                    value = Boolean.parseBoolean(ekv.getStrValue());
                    break;
                } catch (RuntimeException e) {
                    return false;
                }
            case JSON:
                try {
                    value = Boolean.parseBoolean(ekv.getJsonValue());
                    break;
                } catch (RuntimeException e) {
                    return false;
                }
            default:
                return false;
        }
        if (value == null) {
            return false;
        }
        switch (predicate.getOperation()) {
            case EQUAL:
                return value.equals(predicate.getValue().getDefaultValue());
            case NOT_EQUAL:
                return !value.equals(predicate.getValue().getDefaultValue());
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalNumPredicate(EntityKeyValue ekv, NumericFilterPredicate predicate) {
        Double value;
        switch (ekv.getDataType()) {
            case LONG:
                value = ekv.getLngValue().doubleValue();
                break;
            case DOUBLE:
                value = ekv.getDblValue();
                break;
            case BOOLEAN:
                value = ekv.getBoolValue() ? 1.0 : 0.0;
                break;
            case STRING:
                try {
                    value = Double.parseDouble(ekv.getStrValue());
                    break;
                } catch (RuntimeException e) {
                    return false;
                }
            case JSON:
                try {
                    value = Double.parseDouble(ekv.getJsonValue());
                    break;
                } catch (RuntimeException e) {
                    return false;
                }
            default:
                return false;
        }
        if (value == null) {
            return false;
        }

        Double predicateValue = predicate.getValue().getDefaultValue();
        switch (predicate.getOperation()) {
            case NOT_EQUAL:
                return !value.equals(predicateValue);
            case EQUAL:
                return value.equals(predicateValue);
            case GREATER:
                return value > predicateValue;
            case GREATER_OR_EQUAL:
                return value >= predicateValue;
            case LESS:
                return value < predicateValue;
            case LESS_OR_EQUAL:
                return value <= predicateValue;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalStrPredicate(EntityKeyValue ekv, StringFilterPredicate predicate) {
        String val;
        String predicateValue;
        if (predicate.isIgnoreCase()) {
            val = ekv.getStrValue().toLowerCase();
            predicateValue = predicate.getValue().getDefaultValue().toLowerCase();
        } else {
            val = ekv.getStrValue();
            predicateValue = predicate.getValue().getDefaultValue();
        }
        switch (predicate.getOperation()) {
            case CONTAINS:
                return val.contains(predicateValue);
            case EQUAL:
                return val.equals(predicateValue);
            case STARTS_WITH:
                return val.startsWith(predicateValue);
            case ENDS_WITH:
                return val.endsWith(predicateValue);
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            case NOT_CONTAINS:
                return !val.contains(predicateValue);
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }
}
