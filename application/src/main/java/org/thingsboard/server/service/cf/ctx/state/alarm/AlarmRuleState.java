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
package org.thingsboard.server.service.cf.ctx.state.alarm;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.KvUtil;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.RepeatingAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AlarmSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AlarmScheduleType;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AnyTimeSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.CustomTimeSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.CustomTimeScheduleItem;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.SpecificTimeSchedule;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.thingsboard.server.service.cf.ctx.state.alarm.AlarmEvalResult.Status.TRUE;

@Data
@Slf4j
public class AlarmRuleState {

    private final AlarmSeverity severity;
    private AlarmRule alarmRule;
    private AlarmCalculatedFieldState state;

    private AlarmCondition condition;

    private long eventCount;
    private long firstEventTs; // when duration condition started
    private long lastCheckTs;
    private transient long duration;
    private ScheduledFuture<?> durationCheckFuture;
    private Boolean active;

    public AlarmRuleState(AlarmSeverity severity, AlarmRule alarmRule, AlarmCalculatedFieldState state) {
        this.severity = severity;
        if (alarmRule != null) {
            setAlarmRule(alarmRule);
        }
        this.state = state;
    }

    public AlarmEvalResult eval(boolean newEvent, CalculatedFieldCtx ctx) { // on event or config change
        long ts = newEvent ? state.getLatestTimestamp() : System.currentTimeMillis();
        active = isActive(ts);
        if (!active) {
            return AlarmEvalResult.FALSE;
        }
        return doEval(newEvent, ctx);
    }

    public AlarmEvalResult reeval(long ts, CalculatedFieldCtx ctx) { // on scheduled duration check or periodic re-eval for rules with schedule
        boolean active = isActive(ts);
        switch (condition.getType()) {
            case SIMPLE, REPEATING -> {
                boolean activeChanged = this.active == null || active != this.active;
                this.active = active;
                if (!active) {
                    return AlarmEvalResult.EMPTY;
                }

                if ((condition.hasSchedule() && activeChanged) ||
                    condition.getExpression().requiresScheduledReevaluation()) {
                    AlarmEvalResult result = doEval(false, ctx);
                    if (result.getStatus() == TRUE) {
                        return result;
                    } else {
                        return AlarmEvalResult.EMPTY;
                    }
                }
            }
            case DURATION -> {
                if (!active) {
                    return AlarmEvalResult.FALSE;
                }
                long requiredDuration = getRequiredDurationInMs();
                if (requiredDuration > 0 && firstEventTs > 0 && ts > firstEventTs) {
                    duration = ts - firstEventTs;
                    long prevDuration = lastCheckTs - firstEventTs;
                    lastCheckTs = ts;

                    long leftDuration = requiredDuration - duration;
                    if (leftDuration <= 0) {
                        if (prevDuration >= requiredDuration) {
                            // already evaluated as true on previous check, no need to update alarm
                            return AlarmEvalResult.EMPTY;
                        }
                        return AlarmEvalResult.TRUE;
                    } else {
                        return AlarmEvalResult.notYetTrue(0, leftDuration);
                    }
                }
            }
        }
        return AlarmEvalResult.EMPTY;
    }

    public AlarmEvalResult doEval(boolean newEvent, CalculatedFieldCtx ctx) {
        return switch (condition.getType()) {
            case SIMPLE -> evalSimple(ctx);
            case DURATION -> evalDuration(ctx);
            case REPEATING -> evalRepeating(newEvent, ctx);
        };
    }

    private AlarmEvalResult evalSimple(CalculatedFieldCtx ctx) {
        return eval(condition.getExpression(), ctx) ? AlarmEvalResult.TRUE : AlarmEvalResult.FALSE;
    }

    private AlarmEvalResult evalRepeating(boolean newEvent, CalculatedFieldCtx ctx) {
        if (eval(condition.getExpression(), ctx)) {
            if (newEvent) {
                eventCount++;
            }
            long requiredRepeats = getIntValue(((RepeatingAlarmCondition) condition).getCount());
            if (requiredRepeats > 0) {
                long leftRepeats = requiredRepeats - eventCount;
                return leftRepeats <= 0 ? AlarmEvalResult.TRUE : AlarmEvalResult.notYetTrue(leftRepeats, 0);
            } else {
                return AlarmEvalResult.NOT_YET_TRUE;
            }
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    private AlarmEvalResult evalDuration(CalculatedFieldCtx ctx) {
        if (eval(condition.getExpression(), ctx)) {
            long ts = System.currentTimeMillis();
            if (firstEventTs == 0) {
                firstEventTs = state.getLatestTimestamp();
            }
            lastCheckTs = ts;

            long requiredDuration = getRequiredDurationInMs();
            if (requiredDuration > 0 && firstEventTs > 0 && ts > firstEventTs) {
                duration = ts - firstEventTs;
                long leftDuration = requiredDuration - duration;
                if (leftDuration <= 0) {
                    return AlarmEvalResult.TRUE;
                } else {
                    return AlarmEvalResult.notYetTrue(0, leftDuration);
                }
            } else {
                return AlarmEvalResult.NOT_YET_TRUE;
            }
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    private boolean isActive(long eventTs) {
        if (condition.getSchedule() == null) {
            return true;
        }
        AlarmSchedule schedule = state.resolveValue(condition.getSchedule(), entry -> Optional.ofNullable(KvUtil.getStringValue(entry))
                .map(this::parseSchedule).orElse(null));
        boolean active = switch (schedule.getType()) {
            case ANY_TIME -> true;
            case SPECIFIC_TIME -> isActiveSpecific((SpecificTimeSchedule) schedule, eventTs);
            case CUSTOM -> isActiveCustom((CustomTimeSchedule) schedule, eventTs);
        };
        log.trace("Alarm rule active = {} for schedule {}", active, schedule);
        return active;
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
        clearRepeatingConditionState();
        clearDurationConditionState();
    }

    private void clearRepeatingConditionState() {
        eventCount = 0L;
    }

    private void clearDurationConditionState() {
        firstEventTs = 0L;
        lastCheckTs = 0L;
        duration = 0L;
        if (durationCheckFuture != null) {
            durationCheckFuture.cancel(true);
            durationCheckFuture = null;
        }
    }

    public boolean isEmpty() {
        return eventCount == 0L && firstEventTs == 0L && lastCheckTs == 0L && durationCheckFuture == null;
    }

    private AlarmSchedule parseSchedule(String str) {
        ObjectNode json = (ObjectNode) JacksonUtil.toJsonNode(str);
        if (json.isEmpty()) {
            return new AnyTimeSchedule(); // only if valid json, fail otherwise
        }

        if (!json.hasNonNull("type")) {
            // deducting the schedule type
            AlarmScheduleType type;
            if (json.hasNonNull("daysOfWeek")) {
                type = AlarmScheduleType.SPECIFIC_TIME;
            } else if (json.hasNonNull("items")) {
                type = AlarmScheduleType.CUSTOM;
            } else {
                throw new IllegalArgumentException("Failed to parse alarm schedule from '" + str + "'");
            }
            json.put("type", type.name());
        }

        return JacksonUtil.treeToValue(json, AlarmSchedule.class);
    }

    private Integer getIntValue(AlarmConditionValue<Integer> value) {
        return state.resolveValue(value, entry -> Optional.ofNullable(KvUtil.getLongValue(entry)).map(Long::intValue).orElse(null));
    }

    private long getRequiredDurationInMs() {
        DurationAlarmCondition durationCondition = (DurationAlarmCondition) condition;
        return durationCondition.getUnit().toMillis(state.resolveValue(durationCondition.getValue(), KvUtil::getLongValue));
    }

    private boolean eval(AlarmConditionExpression expression, CalculatedFieldCtx ctx) {
        return state.eval(expression, ctx);
    }

    public void setAlarmRule(AlarmRule alarmRule) {
        this.alarmRule = alarmRule;
        this.condition = alarmRule.getCondition();

        // clearing state for other condition types (possibly left from a previous condition type)
        switch (condition.getType()) {
            case SIMPLE -> {
                clearRepeatingConditionState();
                clearDurationConditionState();
            }
            case REPEATING -> {
                clearDurationConditionState();
            }
            case DURATION -> {
                clearRepeatingConditionState();
            }
        }
    }

    public StateInfo getStateInfo() {
        if (condition.getType() == AlarmConditionType.REPEATING) {
            return new StateInfo(eventCount, null);
        } else if (condition.getType() == AlarmConditionType.DURATION) {
            return new StateInfo(null, duration);
        } else {
            return StateInfo.EMPTY;
        }
    }

    @Override
    public String toString() {
        return "AlarmRuleState{" +
               "severity=" + severity +
               ", condition=" + condition +
               ", eventCount=" + eventCount +
               ", firstEventTs=" + firstEventTs +
               ", lastCheckTs=" + lastCheckTs +
               ", duration=" + duration +
               ", durationCheckFuture=" + durationCheckFuture +
               '}';
    }

    public record StateInfo(Long eventCount, Long duration) {

        static final StateInfo EMPTY = new StateInfo(null, null);

    }

}
