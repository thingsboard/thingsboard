/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.function.Function;

@Data
class AlarmRuleState {

    private final AlarmSeverity severity;
    private final AlarmRule alarmRule;
    private final AlarmConditionSpec spec;
    private final long requiredDurationInMs;
    private final long requiredRepeats;
    private final Set<EntityKey> entityKeys;
    private PersistedAlarmRuleState state;
    private boolean updateFlag;
    private final DynamicPredicateValueCtx dynamicPredicateValueCtx;

    AlarmRuleState(AlarmSeverity severity, AlarmRule alarmRule, Set<EntityKey> entityKeys, PersistedAlarmRuleState state, DynamicPredicateValueCtx dynamicPredicateValueCtx) {
        this.severity = severity;
        this.alarmRule = alarmRule;
        this.entityKeys = entityKeys;
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
        this.dynamicPredicateValueCtx = dynamicPredicateValueCtx;
    }

    public boolean validateTsUpdate(Set<EntityKey> changedKeys) {
        for (EntityKey key : changedKeys) {
            if (entityKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean validateAttrUpdate(Set<EntityKey> changedKeys) {
        //If the attribute was updated, but no new telemetry arrived - we ignore this until new telemetry is there.
        for (EntityKey key : entityKeys) {
            if (key.getType().equals(EntityKeyType.TIME_SERIES)) {
                return false;
            }
        }
        for (EntityKey key : changedKeys) {
            EntityKeyType keyType = key.getType();
            if (EntityKeyType.CLIENT_ATTRIBUTE.equals(keyType) || EntityKeyType.SERVER_ATTRIBUTE.equals(keyType) || EntityKeyType.SHARED_ATTRIBUTE.equals(keyType)) {
                key = new EntityKey(EntityKeyType.ATTRIBUTE, key.getKey());
            }
            if (entityKeys.contains(key)) {
                return true;
            }
        }
        return false;
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

    public AlarmEvalResult eval(DataSnapshot data) {
        boolean active = isActive(data.getTs());
        switch (spec.getType()) {
            case SIMPLE:
                return (active && eval(alarmRule.getCondition(), data)) ? AlarmEvalResult.TRUE : AlarmEvalResult.FALSE;
            case DURATION:
                return evalDuration(data, active);
            case REPEATING:
                return evalRepeating(data, active);
            default:
                return AlarmEvalResult.FALSE;
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
        return isActive(eventTs, zoneId, zdt, schedule.getStartsOn(), schedule.getEndsOn());
    }

    private boolean isActiveCustom(CustomTimeSchedule schedule, long eventTs) {
        ZoneId zoneId = SchedulerUtils.getZoneId(schedule.getTimezone());
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(eventTs), zoneId);
        int dayOfWeek = zdt.toLocalDate().getDayOfWeek().getValue();
        for (CustomTimeScheduleItem item : schedule.getItems()) {
            if (item.getDayOfWeek() == dayOfWeek) {
                if (item.isEnabled()) {
                    return isActive(eventTs, zoneId, zdt, item.getStartsOn(), item.getEndsOn());
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
            return state.getDuration() > requiredDurationInMs ? AlarmEvalResult.TRUE : AlarmEvalResult.NOT_YET_TRUE;
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    public AlarmEvalResult eval(long ts) {
        switch (spec.getType()) {
            case SIMPLE:
            case REPEATING:
                return AlarmEvalResult.NOT_YET_TRUE;
            case DURATION:
                if (requiredDurationInMs > 0 && state.getLastEventTs() > 0 && ts > state.getLastEventTs()) {
                    long duration = state.getDuration() + (ts - state.getLastEventTs());
                    if (isActive(ts)) {
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
        boolean eval = true;
        for (KeyFilter keyFilter : condition.getCondition()) {
            EntityKeyValue value = data.getValue(keyFilter.getKey());
            if (value == null) {
                return false;
            }
            eval = eval && eval(data, value, keyFilter.getPredicate());
        }
        return eval;
    }

    private boolean eval(DataSnapshot data, EntityKeyValue value, KeyFilterPredicate predicate) {
        switch (predicate.getType()) {
            case STRING:
                return evalStrPredicate(data, value, (StringFilterPredicate) predicate);
            case NUMERIC:
                return evalNumPredicate(data, value, (NumericFilterPredicate) predicate);
            case BOOLEAN:
                return evalBoolPredicate(data, value, (BooleanFilterPredicate) predicate);
            case COMPLEX:
                return evalComplexPredicate(data, value, (ComplexFilterPredicate) predicate);
            default:
                return false;
        }
    }

    private boolean evalComplexPredicate(DataSnapshot data, EntityKeyValue ekv, ComplexFilterPredicate predicate) {
        switch (predicate.getOperation()) {
            case OR:
                for (KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (eval(data, ekv, kfp)) {
                        return true;
                    }
                }
                return false;
            case AND:
                for (KeyFilterPredicate kfp : predicate.getPredicates()) {
                    if (!eval(data, ekv, kfp)) {
                        return false;
                    }
                }
                return true;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalBoolPredicate(DataSnapshot data, EntityKeyValue ekv, BooleanFilterPredicate predicate) {
        Boolean val = getBoolValue(ekv);
        if (val == null) {
            return false;
        }
        Boolean predicateValue = getPredicateValue(data, predicate.getValue(), AlarmRuleState::getBoolValue);
        switch (predicate.getOperation()) {
            case EQUAL:
                return val.equals(predicateValue);
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalNumPredicate(DataSnapshot data, EntityKeyValue ekv, NumericFilterPredicate predicate) {
        Double val = getDblValue(ekv);
        if (val == null) {
            return false;
        }
        Double predicateValue = getPredicateValue(data, predicate.getValue(), AlarmRuleState::getDblValue);
        switch (predicate.getOperation()) {
            case NOT_EQUAL:
                return !val.equals(predicateValue);
            case EQUAL:
                return val.equals(predicateValue);
            case GREATER:
                return val > predicateValue;
            case GREATER_OR_EQUAL:
                return val >= predicateValue;
            case LESS:
                return val < predicateValue;
            case LESS_OR_EQUAL:
                return val <= predicateValue;
            default:
                throw new RuntimeException("Operation not supported: " + predicate.getOperation());
        }
    }

    private boolean evalStrPredicate(DataSnapshot data, EntityKeyValue ekv, StringFilterPredicate predicate) {
        String val = getStrValue(ekv);
        if (val == null) {
            return false;
        }
        String predicateValue = getPredicateValue(data, predicate.getValue(), AlarmRuleState::getStrValue);
        if (predicate.isIgnoreCase()) {
            val = val.toLowerCase();
            predicateValue = predicateValue.toLowerCase();
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

    private <T> T getPredicateValue(DataSnapshot data, FilterPredicateValue<T> value, Function<EntityKeyValue, T> transformFunction) {
        EntityKeyValue ekv = getDynamicPredicateValue(data, value);
        if (ekv != null) {
            T result = transformFunction.apply(ekv);
            if (result != null) {
                return result;
            }
        }
        return value.getDefaultValue();
    }

    private <T> EntityKeyValue getDynamicPredicateValue(DataSnapshot data, FilterPredicateValue<T> value) {
        EntityKeyValue ekv = null;
        if (value.getDynamicValue() != null) {
            switch (value.getDynamicValue().getSourceType()) {
                case CURRENT_DEVICE:
                    ekv = data.getValue(new EntityKey(EntityKeyType.ATTRIBUTE, value.getDynamicValue().getSourceAttribute()));
                    if (ekv == null) {
                        ekv = data.getValue(new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, value.getDynamicValue().getSourceAttribute()));
                        if (ekv == null) {
                            ekv = data.getValue(new EntityKey(EntityKeyType.SHARED_ATTRIBUTE, value.getDynamicValue().getSourceAttribute()));
                            if (ekv == null) {
                                ekv = data.getValue(new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, value.getDynamicValue().getSourceAttribute()));
                            }
                        }
                    }
                    if(ekv != null || !value.getDynamicValue().isInherit()) {
                        break;
                    }
                case CURRENT_CUSTOMER:
                    ekv = dynamicPredicateValueCtx.getCustomerValue(value.getDynamicValue().getSourceAttribute());
                    if(ekv != null || !value.getDynamicValue().isInherit()) {
                       break;
                    }
                case CURRENT_TENANT:
                    ekv = dynamicPredicateValueCtx.getTenantValue(value.getDynamicValue().getSourceAttribute());
            }
        }
        return ekv;
    }

    private static String getStrValue(EntityKeyValue ekv) {
        switch (ekv.getDataType()) {
            case LONG:
                return ekv.getLngValue() != null ? ekv.getLngValue().toString() : null;
            case DOUBLE:
                return ekv.getDblValue() != null ? ekv.getDblValue().toString() : null;
            case BOOLEAN:
                return ekv.getBoolValue() != null ? ekv.getBoolValue().toString() : null;
            case STRING:
                return ekv.getStrValue();
            case JSON:
                return ekv.getJsonValue();
            default:
                return null;
        }
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

}
