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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.KvUtil;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.RepeatingAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AlarmSchedule;
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

@Data
@Slf4j
public class AlarmRuleState {

    private final AlarmSeverity severity;
    private AlarmRule alarmRule;
    private AlarmCalculatedFieldState state;

    private AlarmCondition condition;

    private long eventCount;
    private long firstEventTs; // when duration condition started
    private long lastEventTs;
    private transient long duration;
    private ScheduledFuture<?> durationCheckFuture;

    public AlarmRuleState(AlarmSeverity severity, AlarmRule alarmRule, AlarmCalculatedFieldState state) {
        this.severity = severity;
        if (alarmRule != null) {
            setAlarmRule(alarmRule);
        }
        this.state = state;
    }

    public AlarmEvalResult eval(boolean newEvent, CalculatedFieldCtx ctx) { // on event or config change
        boolean active = isActive(state.getLatestTimestamp());
        return switch (condition.getType()) {
            case SIMPLE -> evalSimple(active, ctx);
            case DURATION -> evalDuration(active, ctx);
            case REPEATING -> evalRepeating(active, newEvent, ctx);
        };
    }

    public AlarmEvalResult reeval(long ts) {
        switch (condition.getType()) {
            case SIMPLE, REPEATING -> {
                return AlarmEvalResult.NOT_YET_TRUE;
            }
            case DURATION -> {
                long requiredDuration = getRequiredDurationInMs();
                if (requiredDuration > 0 && lastEventTs > 0 && ts > lastEventTs) {
                    duration = ts - firstEventTs;
                    if (isActive(ts)) {
                        long leftDuration = requiredDuration - duration;
                        if (leftDuration <= 0) {
                            return AlarmEvalResult.TRUE;
                        } else {
                            return AlarmEvalResult.notYetTrue(0, leftDuration);
                        }
                    } else {
                        return AlarmEvalResult.FALSE;
                    }
                }
            }
        }
        return AlarmEvalResult.FALSE;
    }

    private AlarmEvalResult evalSimple(boolean active, CalculatedFieldCtx ctx) {
        return (active && eval(condition.getExpression(), ctx)) ?
                AlarmEvalResult.TRUE : AlarmEvalResult.FALSE;
    }

    private AlarmEvalResult evalRepeating(boolean active, boolean newEvent, CalculatedFieldCtx ctx) {
        if (active && eval(condition.getExpression(), ctx)) {
            if (newEvent) {
                eventCount++;
            }
            long requiredRepeats = getIntValue(((RepeatingAlarmCondition) condition).getCount());
            long leftRepeats = requiredRepeats - eventCount;
            return leftRepeats <= 0 ? AlarmEvalResult.TRUE : AlarmEvalResult.notYetTrue(leftRepeats, 0);
        } else {
            return AlarmEvalResult.FALSE;
        }
    }

    private AlarmEvalResult evalDuration(boolean active, CalculatedFieldCtx ctx) {
        if (active && eval(condition.getExpression(), ctx)) {
            long eventTs = state.getLatestTimestamp();
            if (lastEventTs > 0) {
                if (eventTs > lastEventTs) {
                    if (firstEventTs == 0) {
                        firstEventTs = lastEventTs;
                    }
                    lastEventTs = eventTs;
                }
            } else {
                firstEventTs = eventTs;
                lastEventTs = eventTs;
            }
            duration = lastEventTs - firstEventTs;
            long requiredDuration = getRequiredDurationInMs();
            long leftDuration = requiredDuration - duration;
            if (leftDuration <= 0) {
                return AlarmEvalResult.TRUE;
            } else {
                return AlarmEvalResult.notYetTrue(0, leftDuration);
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
                .map(str -> JsonConverter.parse(str, AlarmSchedule.class))
                .orElse(null));
        return switch (schedule.getType()) {
            case ANY_TIME -> true;
            case SPECIFIC_TIME -> isActiveSpecific((SpecificTimeSchedule) schedule, eventTs);
            case CUSTOM -> isActiveCustom((CustomTimeSchedule) schedule, eventTs);
        };
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
        eventCount = 0L;
        firstEventTs = 0L;
        lastEventTs = 0L;
        duration = 0L;
        if (durationCheckFuture != null) {
            durationCheckFuture.cancel(true);
            durationCheckFuture = null;
        }
    }

    public boolean isEmpty() {
        return eventCount == 0L && firstEventTs == 0L && lastEventTs == 0L && durationCheckFuture == null;
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
               ", lastEventTs=" + lastEventTs +
               ", duration=" + duration +
               ", durationCheckFuture=" + durationCheckFuture +
               '}';
    }

    public record StateInfo(Long eventCount, Long duration) {
        static final StateInfo EMPTY = new StateInfo(null, null);

    }

}
