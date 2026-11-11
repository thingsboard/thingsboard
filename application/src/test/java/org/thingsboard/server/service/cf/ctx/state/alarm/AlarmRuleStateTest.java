/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.SimpleAlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.NumericFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.NumericFilterPredicate.NumericOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.SpecificTimeSchedule;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AlarmRuleStateTest {

    private static final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("5512071d-5abc-411d-a907-4cdb6539c2eb"));

    // Schedule window: every day from 10:00 to 24:00 UTC
    private static final long DAY_START_TS = Instant.parse("2024-01-15T00:00:00Z").toEpochMilli();
    private static final long WINDOW_STARTS_ON_MS = TimeUnit.HOURS.toMillis(10);
    private static final long TELEMETRY_TS = DAY_START_TS + TimeUnit.HOURS.toMillis(8);
    private static final long WINDOW_OPENED_TS = DAY_START_TS + WINDOW_STARTS_ON_MS + TimeUnit.SECONDS.toMillis(1);

    private static final long SHORT_DURATION_MS = TimeUnit.MINUTES.toMillis(1);
    // AlarmRuleState.evalDuration() measures the duration against the wall clock, so to have a duration
    // that has not elapsed yet for a telemetry ts fixed in the past, it has to be far longer than the time since then.
    private static final long LONG_DURATION_MS = TimeUnit.DAYS.toMillis(365 * 1000);

    private final CalculatedFieldCtx ctx = mock(CalculatedFieldCtx.class);

    private AlarmCalculatedFieldState state;

    @BeforeEach
    void setUp() {
        AlarmCalculatedFieldConfiguration configuration = new AlarmCalculatedFieldConfiguration();
        configuration.setCreateRules(Map.of());
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setName("Test alarm");
        calculatedField.setConfiguration(configuration);
        when(ctx.getCalculatedField()).thenReturn(calculatedField);
        when(ctx.getArgNames()).thenReturn(List.of("temperature"));

        state = new AlarmCalculatedFieldState(DEVICE_ID);
        state.setCtx(ctx, null);
        // stored telemetry that satisfies the condition, received while the schedule window was closed
        state.update(Map.of("temperature", new SingleValueArgumentEntry(TELEMETRY_TS, new DoubleDataEntry("temperature", 30.0), 1L)), ctx);
    }

    @Test
    void testReevalDurationWithoutSchedule_doesNotStartTimerFromStoredValue() {
        // re-evaluated without a new event because another rule of the same calculated field has a schedule
        AlarmRuleState ruleState = createRuleState(SHORT_DURATION_MS, false);

        AlarmEvalResult result = ruleState.reeval(WINDOW_OPENED_TS, ctx);

        assertThat(result.getStatus()).isNull();
        assertThat(ruleState.getFirstEventTs()).isZero();
    }

    @Test
    void testReevalDurationWithSchedule_whenWindowOpensAndDurationElapsed_thenTrue() {
        AlarmRuleState ruleState = createRuleState(SHORT_DURATION_MS, true);

        AlarmEvalResult result = ruleState.reeval(WINDOW_OPENED_TS, ctx);

        assertThat(result.getStatus()).isEqualTo(AlarmEvalResult.Status.TRUE);
        assertThat(ruleState.getFirstEventTs()).isEqualTo(TELEMETRY_TS);
    }

    @Test
    void testReevalDurationWithSchedule_whenWindowOpensAndDurationNotElapsed_thenTimerStarted() {
        // leftDuration of the result is what AlarmCalculatedFieldState uses to schedule the next duration check
        AlarmRuleState ruleState = createRuleState(LONG_DURATION_MS, true);

        AlarmEvalResult result = ruleState.reeval(WINDOW_OPENED_TS, ctx);

        assertThat(result.getStatus()).isEqualTo(AlarmEvalResult.Status.NOT_YET_TRUE);
        assertThat(result.getLeftDuration()).isPositive();
        assertThat(ruleState.getFirstEventTs()).isEqualTo(TELEMETRY_TS);
    }

    private AlarmRuleState createRuleState(long durationMs, boolean withSchedule) {
        DurationAlarmCondition condition = new DurationAlarmCondition();
        condition.setExpression(createTemperatureGreaterThanExpression(25.0));
        condition.setUnit(TimeUnit.MILLISECONDS);
        condition.setValue(new AlarmConditionValue<>(durationMs, null));
        if (withSchedule) {
            SpecificTimeSchedule schedule = new SpecificTimeSchedule();
            schedule.setTimezone("UTC");
            schedule.setDaysOfWeek(Set.of(1, 2, 3, 4, 5, 6, 7));
            schedule.setStartsOn(WINDOW_STARTS_ON_MS);
            condition.setSchedule(new AlarmConditionValue<>(schedule, null));
        }
        AlarmRule rule = new AlarmRule();
        rule.setCondition(condition);
        return new AlarmRuleState(AlarmSeverity.CRITICAL, rule, state);
    }

    private SimpleAlarmConditionExpression createTemperatureGreaterThanExpression(double threshold) {
        AlarmConditionFilter filter = new AlarmConditionFilter();
        filter.setArgument("temperature");
        filter.setValueType(EntityKeyValueType.NUMERIC);
        filter.setPredicates(List.of(new NumericFilterPredicate(NumericOperation.GREATER, new AlarmConditionValue<>(threshold, null))));
        SimpleAlarmConditionExpression expression = new SimpleAlarmConditionExpression();
        expression.setFilters(List.of(filter));
        return expression;
    }

}
