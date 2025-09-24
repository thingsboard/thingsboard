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
package org.thingsboard.server.cf;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.RepeatingAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.TbelAlarmConditionExpression;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.CalculatedFieldDebugEvent;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "actors.alarms.reevaluation_interval=1"
})
public class AlarmRulesTest extends AbstractControllerTest {

    @MockitoSpyBean
    private ActorSystemContext actorSystemContext;

    @Autowired
    private EventDao eventDao;

    private DeviceId deviceId;
    private EventId latestEventId;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Device A", "aaa");
        deviceId = device.getId();
    }

    @Test
    public void testCreateAndSeverityUpdateAndClear() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument
        );

        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.MAJOR, new Condition("return temperature >= 50;", null, null),
                AlarmSeverity.CRITICAL, new Condition("return temperature >= 100;", null, null)
        );

        Condition clearRule = new Condition("return temperature <= 25;", null, null);
        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, clearRule);

        postTelemetry(deviceId, "{\"temperature\":50}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.MAJOR);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        postTelemetry(deviceId, "{\"temperature\":100}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isSeverityUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        postTelemetry(deviceId, "{\"temperature\":101}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        postTelemetry(deviceId, "{\"temperature\":20}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCleared()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.CLEARED_UNACK);
        });
    }

    /*
     * todo: state restore (event count)
     * */
    @Test
    public void testCreateAlarmForRepeatingCondition() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument
        );

        int eventsCountMajor = 5;
        int eventsCountCritical = 10;
        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.MAJOR, new Condition("return temperature >= 50;", eventsCountMajor, null),
                AlarmSeverity.CRITICAL, new Condition("return temperature >= 50;", eventsCountCritical, null)
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, null);
        for (int i = 0; i < 4; i++) {
            postTelemetry(deviceId, "{\"temperature\":50}");
            Thread.sleep(10);
        }
        assertThat(getLatestAlarmResult(calculatedField.getId())).isNull();
        postTelemetry(deviceId, "{\"temperature\":50}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.MAJOR);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
            assertThat(alarmResult.getConditionRepeats()).isEqualTo(5);
        });

        for (int i = 0; i < 5; i++) {
            postTelemetry(deviceId, "{\"temperature\":50}");
            Thread.sleep(10);
        }
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isSeverityUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
            assertThat(alarmResult.getConditionRepeats()).isEqualTo(10);
        });
    }

    @Test
    public void testCreateAlarmForDurationCondition() throws Exception {
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("powerConsumption", ArgumentType.TS_LATEST, null));
        argument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "powerConsumption", argument
        );

        long createDurationMs = 5000L;
        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return powerConsumption >= 3000;", null, createDurationMs)
        );
        long clearDurationMs = 2000L;
        Condition clearRule = new Condition("return powerConsumption < 3000;", null, createDurationMs);

        CalculatedField calculatedField = createAlarmCf(deviceId, "High power consumption during 3 seconds",
                arguments, createRules, clearRule);
        postTelemetry(deviceId, "{\"powerConsumption\":3500}");
        Thread.sleep(createDurationMs - 2000);
        assertThat(getLatestAlarmResult(calculatedField.getId())).isNull();

        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
            assertThat(alarmResult.getConditionDuration()).isBetween(createDurationMs, createDurationMs + 2000);
        });
    }

    private void checkAlarmResult(CalculatedField calculatedField, Consumer<TbAlarmResult> assertion) {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            TbAlarmResult alarmResult = getLatestAlarmResult(calculatedField.getId());
            assertThat(alarmResult).isNotNull();
            assertion.accept(alarmResult);

            Alarm alarm = alarmResult.getAlarm();
            assertThat(alarm.getOriginator()).isEqualTo(deviceId);
            assertThat(alarm.getType()).isEqualTo(calculatedField.getName());
        });
    }

    private TbAlarmResult getLatestAlarmResult(CalculatedFieldId calculatedFieldId) {
        List<CalculatedFieldDebugEvent> debugEvents = getDebugEvents(calculatedFieldId, 1);
        if (debugEvents.isEmpty()) {
            return null;
        }
        CalculatedFieldDebugEvent debugEvent = debugEvents.get(0);
        if (debugEvent.getError() != null) {
            System.err.println("CF error: " + debugEvent.getError());
            Assertions.fail();
        }
        if (debugEvent.getId().equals(latestEventId)) {
            return null;
        }
        latestEventId = debugEvent.getId();
        return JacksonUtil.fromString(debugEvent.getResult(), TbAlarmResult.class);
    }

    private CalculatedField createAlarmCf(EntityId entityId,
                                          String alarmType,
                                          Map<String, Argument> arguments,
                                          Map<AlarmSeverity, Condition> createConditions,
                                          Condition clearCondition) {
        Map<AlarmSeverity, AlarmRule> createRules = new HashMap<>();
        createConditions.forEach((severity, condition) -> {
            createRules.put(severity, toAlarmRule(condition));
        });
        AlarmRule clearRule = clearCondition != null ? toAlarmRule(clearCondition) : null;
        CalculatedField calculatedField = createAlarmCf(entityId, alarmType, arguments, createRules, clearRule);

        CalculatedFieldDebugEvent debugEvent = await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> getDebugEvents(calculatedField.getId(), 1), events -> !events.isEmpty()).get(0);
        latestEventId = debugEvent.getId();
        return calculatedField;
    }

    private CalculatedField createAlarmCf(EntityId entityId,
                                          String alarmType,
                                          Map<String, Argument> arguments,
                                          Map<AlarmSeverity, AlarmRule> createRules,
                                          AlarmRule clearRule) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setName(alarmType);
        calculatedField.setType(CalculatedFieldType.ALARM);
        AlarmCalculatedFieldConfiguration configuration = new AlarmCalculatedFieldConfiguration();
        configuration.setArguments(arguments);
        configuration.setCreateRules(createRules);
        configuration.setClearRule(clearRule);
        calculatedField.setConfiguration(configuration);
        calculatedField.setDebugSettings(DebugSettings.all());
        return saveCalculatedField(calculatedField);
    }

    private AlarmRule toAlarmRule(Condition condition) {
        AlarmRule rule = new AlarmRule();
        TbelAlarmConditionExpression expression = new TbelAlarmConditionExpression();
        expression.setExpression(condition.expression());
        if (condition.eventsCount() != null) {
            RepeatingAlarmCondition alarmCondition = new RepeatingAlarmCondition();
            alarmCondition.setExpression(expression);
            AlarmConditionValue<Integer> count = new AlarmConditionValue<>();
            count.setStaticValue(condition.eventsCount());
            alarmCondition.setCount(count);
            rule.setCondition(alarmCondition);
        } else if (condition.durationMs() != null) {
            DurationAlarmCondition alarmCondition = new DurationAlarmCondition();
            alarmCondition.setExpression(expression);
            alarmCondition.setUnit(TimeUnit.MILLISECONDS);
            AlarmConditionValue<Long> duration = new AlarmConditionValue<>();
            duration.setStaticValue(condition.durationMs());
            alarmCondition.setValue(duration);
            rule.setCondition(alarmCondition);
        } else {
            SimpleAlarmCondition alarmCondition = new SimpleAlarmCondition();
            alarmCondition.setExpression(expression);
            rule.setCondition(alarmCondition);
        }
        return rule;
    }

    private List<CalculatedFieldDebugEvent> getDebugEvents(CalculatedFieldId calculatedFieldId, int limit) {
        return eventDao.findLatestEvents(tenantId.getId(), calculatedFieldId.getId(), EventType.DEBUG_CALCULATED_FIELD, limit).stream()
                .map(e -> (CalculatedFieldDebugEvent) e).toList();
    }

    private record Condition(String expression, Integer eventsCount, Long durationMs) {}

}
