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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionValue;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.RepeatingAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.AlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.ComplexOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.SimpleAlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.TbelAlarmConditionExpression;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.NumericFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.NumericFilterPredicate.NumericOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.StringFilterPredicate;
import org.thingsboard.server.common.data.alarm.rule.condition.expression.predicate.StringFilterPredicate.StringOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.AlarmSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.schedule.SpecificTimeSchedule;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.AlarmCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.CurrentOwnerDynamicSourceConfiguration;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.event.CalculatedFieldDebugEvent;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

    private Device device;
    private DeviceId deviceId;
    private EntityId originatorId;
    private EventId latestEventId;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        device = createDevice("Device A", "aaa");
        deviceId = device.getId();
        originatorId = deviceId;
    }

    @Test
    public void testCreateAlarm_severityUpdate_clear() throws Exception {
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

    @Test
    public void testCreateAlarm_simpleConditionExpression() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument
        );

        SimpleAlarmConditionExpression simpleExpression = new SimpleAlarmConditionExpression();
        AlarmConditionFilter filter = new AlarmConditionFilter();
        filter.setArgument("temperature");
        filter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericOperation.GREATER_OR_EQUAL);
        AlarmConditionValue<Double> thresholdValue = new AlarmConditionValue<>();
        thresholdValue.setStaticValue(100.0);
        predicate.setValue(thresholdValue);
        filter.setPredicate(predicate);
        simpleExpression.setFilters(List.of(filter));
        simpleExpression.setOperation(ComplexOperation.AND);
        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition(simpleExpression, null, null)
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, null);

        postTelemetry(deviceId, "{\"temperature\":100}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });
    }

    @Test
    public void testCreateAlarm_repeatingCondition() throws Exception {
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

        for (int i = 0; i < 4; i++) {
            postTelemetry(deviceId, "{\"temperature\":50}");
            Thread.sleep(10);
        }
        checkAlarmResult(calculatedField, alarmResult -> alarmResult.getConditionRepeats() == 9, alarmResult -> {
            assertThat(alarmResult.isUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.MAJOR);
        });
        postTelemetry(deviceId, "{\"temperature\":50}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isSeverityUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
            assertThat(alarmResult.getConditionRepeats()).isEqualTo(10);
        });
    }

    @Test
    public void testCreateAlarm_dynamicRepeatingCondition() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");

        Argument eventsCountArgument = new Argument();
        eventsCountArgument.setRefEntityKey(new ReferencedEntityKey("eventsCount", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        eventsCountArgument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument,
                "eventsCount", eventsCountArgument
        );

        int eventsCount = 5;
        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return temperature >= 50;", null,
                        new AlarmConditionValue<>(null, "eventsCount"), null, null)
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, null);
        postAttributes(deviceId, AttributeScope.SERVER_SCOPE, "{\"eventsCount\":" + eventsCount + "}");
        for (int i = 0; i < eventsCount; i++) {
            postTelemetry(deviceId, "{\"temperature\":50}");
            Thread.sleep(10);
        }
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
            assertThat(alarmResult.getConditionRepeats()).isEqualTo(eventsCount);
        });
    }

    @Test
    public void testCreateAlarm_durationCondition() throws Exception {
        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("powerConsumption", ArgumentType.TS_LATEST, null));
        argument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "powerConsumption", argument
        );

        long createDurationMs = 5000L;
        long clearDurationMs = 3000L;
        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return powerConsumption >= 3000;", null, createDurationMs)
        );
        Condition clearRule = new Condition("return powerConsumption < 3000;", null, clearDurationMs);

        CalculatedField calculatedField = createAlarmCf(deviceId, "High power consumption during 5 seconds",
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

        postTelemetry(deviceId, "{\"powerConsumption\":2000}");
        Thread.sleep(clearDurationMs - 2000);
        assertThat(getLatestAlarmResult(calculatedField.getId())).isNull();

        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCleared()).isTrue();
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.CLEARED_UNACK);
            assertThat(alarmResult.getConditionDuration()).isBetween(clearDurationMs, clearDurationMs + 2000);
        });
    }

    @Test
    public void testCreateAlarm_dynamicDurationCondition() throws Exception {
        Argument powerConsumptionArgument = new Argument();
        powerConsumptionArgument.setRefEntityKey(new ReferencedEntityKey("powerConsumption", ArgumentType.TS_LATEST, null));
        powerConsumptionArgument.setDefaultValue("0");

        Argument durationArgument = new Argument();
        durationArgument.setRefEntityKey(new ReferencedEntityKey("duration", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        durationArgument.setDefaultValue("-1");
        Map<String, Argument> arguments = Map.of(
                "powerConsumption", powerConsumptionArgument,
                "duration", durationArgument
        );

        long createDurationMs = 2000L;
        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return powerConsumption >= 3000;", null, null,
                        new AlarmConditionValue<Long>(null, "duration"), null)
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "High power consumption during 2 seconds",
                arguments, createRules, null);
        postTelemetry(deviceId, "{\"powerConsumption\":3500}");
        postAttributes(deviceId, AttributeScope.SERVER_SCOPE, "{\"duration\":" + createDurationMs + "}");

        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
            assertThat(alarmResult.getConditionDuration()).isBetween(createDurationMs, createDurationMs + 2000);
        });
    }

    @Test
    public void testCreateAlarm_currentOwnerArgument() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");

        Argument temperatureThresholdArgument = new Argument();
        temperatureThresholdArgument.setRefEntityKey(new ReferencedEntityKey("temperatureThreshold", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        temperatureThresholdArgument.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());
        temperatureThresholdArgument.setDefaultValue("1000");

        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument,
                "temperatureThreshold", temperatureThresholdArgument
        );

        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return temperature >= temperatureThreshold;", null, null)
        );

        device.setCustomerId(customerId);
        device = doPost("/api/device", device, Device.class);
        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, null);
        postAttributes(customerId, AttributeScope.SERVER_SCOPE, "{\"temperatureThreshold\":50}");

        postTelemetry(deviceId, "{\"temperature\":51}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });
    }

    @Test
    public void testCreateAndClearAlarm_customerAlarmRule_simpleExpression() throws Exception {
        Argument locationArgument = new Argument();
        locationArgument.setRefEntityKey(new ReferencedEntityKey("location", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        locationArgument.setDefaultValue("unknown");
        originatorId = customerId;

        Argument locationFilterArgument = new Argument();
        locationFilterArgument.setRefEntityKey(new ReferencedEntityKey("locationFilter", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        locationFilterArgument.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());
        locationFilterArgument.setDefaultValue("None");

        Map<String, Argument> arguments = Map.of(
                "location", locationArgument,
                "locationFilter", locationFilterArgument
        );

        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.INDETERMINATE, new Condition(createSimpleExpression(
                        "location", StringOperation.CONTAINS, new AlarmConditionValue<>(null, "locationFilter")
                ), null, null)
        );
        Condition clearRule = new Condition(createSimpleExpression(
                "location", StringOperation.NOT_CONTAINS, new AlarmConditionValue<>(null, "locationFilter")
        ), null, null);

        CalculatedField calculatedField = createAlarmCf(customerId, "New resident",
                arguments, createRules, clearRule);

        loginSysAdmin();
        postAttributes(tenantId, AttributeScope.SERVER_SCOPE, "{\"locationFilter\":\"Kyiv\"}");
        loginTenantAdmin();
        postAttributes(customerId, AttributeScope.SERVER_SCOPE, "{\"location\":\"Ukraine, Kyiv\"}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.INDETERMINATE);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        postAttributes(customerId, AttributeScope.SERVER_SCOPE, "{\"location\":\"Ukraine, Lviv\"}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCleared()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.INDETERMINATE);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.CLEARED_UNACK);
        });
    }

    @Test
    public void testCreateAlarm_dynamicSchedule() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");
        Argument scheduleArgument = new Argument();
        scheduleArgument.setRefEntityKey(new ReferencedEntityKey("schedule", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        scheduleArgument.setDefaultValue("None");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument,
                "schedule", scheduleArgument
        );

        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return temperature >= 50;", null, null, null,
                        new AlarmConditionValue<>(null, "schedule"))
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, null);
        String schedule = """
                {"timezone":"Europe/Kiev","items":[{"enabled":false,"dayOfWeek":1,"startsOn":0,"endsOn":0},{"enabled":false,"dayOfWeek":2,"startsOn":0,"endsOn":0},{"enabled":false,"dayOfWeek":3,"startsOn":0,"endsOn":0},{"enabled":false,"dayOfWeek":4,"startsOn":0,"endsOn":0},{"enabled":false,"dayOfWeek":5,"startsOn":0,"endsOn":0},{"enabled":false,"dayOfWeek":6,"startsOn":0,"endsOn":0},{"enabled":false,"dayOfWeek":7,"startsOn":0,"endsOn":0}]}
                """;
        postAttributes(deviceId, AttributeScope.SERVER_SCOPE, "{\"schedule\":" + schedule + "}");
        postTelemetry(deviceId, "{\"temperature\":50}");

        Thread.sleep(1000);
        assertThat(getLatestAlarmResult(calculatedField.getId())).isNull();

        schedule = schedule.replace("\"enabled\":false", "\"enabled\":true");
        postAttributes(deviceId, AttributeScope.SERVER_SCOPE, "{\"schedule\":" + schedule + "}");

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            // checking multiple debug events due to scheduled reevaluation (which also produces debug events)
            CalculatedFieldDebugEvent debugEvent = getDebugEvents(calculatedField.getId(), 5).stream()
                    .filter(event -> event.getResult() != null)
                    .findFirst().orElse(null);
            assertThat(debugEvent).isNotNull();
            TbAlarmResult alarmResult = JacksonUtil.fromString(debugEvent.getResult(), TbAlarmResult.class);
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });
    }

    @Test
    public void testChangeAlarmType() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument
        );

        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return temperature >= 50;", null, null)
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, null);

        postTelemetry(deviceId, "{\"temperature\":50}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });

        calculatedField.setName("New alarm type");
        calculatedField = saveCalculatedField(calculatedField);
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });
    }

    @Test
    public void testChangeRuleExpression() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument
        );

        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return temperature >= 100;", null, null)
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, null);

        postTelemetry(deviceId, "{\"temperature\":50}");
        Thread.sleep(1000);
        assertThat(getLatestAlarmResult(calculatedField.getId())).isNull();

        AlarmCalculatedFieldConfiguration configuration = (AlarmCalculatedFieldConfiguration) calculatedField.getConfiguration();
        ((TbelAlarmConditionExpression) configuration.getCreateRules().get(AlarmSeverity.CRITICAL).getCondition().getExpression())
                .setExpression("return temperature >= 50;");
        calculatedField = saveCalculatedField(calculatedField);
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });
    }

    @Test
    public void testChangeRequiredEventsCountForRepeatingCondition() throws Exception {
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
        for (int i = 0; i < eventsCountMajor; i++) {
            postTelemetry(deviceId, "{\"temperature\":50}");
            Thread.sleep(10);
        }
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.MAJOR);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
            assertThat(alarmResult.getConditionRepeats()).isEqualTo(5);
        });

        postTelemetry(deviceId, "{\"temperature\":50}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.MAJOR);
            assertThat(alarmResult.getConditionRepeats()).isEqualTo(6);
        });

        // decreasing required events count for critical rule
        AlarmCalculatedFieldConfiguration configuration = (AlarmCalculatedFieldConfiguration) calculatedField.getConfiguration();
        ((RepeatingAlarmCondition) configuration.getCreateRules().get(AlarmSeverity.CRITICAL).getCondition())
                .setCount(new AlarmConditionValue<>(6, null));
        calculatedField = saveCalculatedField(calculatedField);
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isSeverityUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
            assertThat(alarmResult.getConditionRepeats()).isEqualTo(6);
        });
    }

    @Test
    public void testChangeConditionArgumentSource() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");

        Argument temperatureThresholdArgument = new Argument();
        temperatureThresholdArgument.setRefEntityKey(new ReferencedEntityKey("temperatureThreshold", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        temperatureThresholdArgument.setRefDynamicSourceConfiguration(new CurrentOwnerDynamicSourceConfiguration());
        temperatureThresholdArgument.setDefaultValue("100");
        loginSysAdmin();
        postAttributes(tenantId, AttributeScope.SERVER_SCOPE, "{\"temperatureThreshold\":100}");
        loginTenantAdmin();
        postAttributes(deviceId, AttributeScope.SERVER_SCOPE, "{\"temperatureThreshold\":50}");

        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument,
                "temperatureThreshold", temperatureThresholdArgument
        );

        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return temperature >= temperatureThreshold;", null, null)
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, null);

        postTelemetry(deviceId, "{\"temperature\":50}");
        Thread.sleep(1000);
        // not created because tenant's threshold 100 is used
        assertThat(getLatestAlarmResult(calculatedField.getId())).isNull();

        ((AlarmCalculatedFieldConfiguration) calculatedField.getConfiguration()).getArguments().get("temperatureThreshold")
                .setRefDynamicSourceConfiguration(null);
        // using threshold 50 on device level
        calculatedField = saveCalculatedField(calculatedField);

        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });
    }

    @Test
    public void testAlarmDetails() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");
        Argument humidityArgument = new Argument();
        humidityArgument.setRefEntityKey(new ReferencedEntityKey("humidity", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        humidityArgument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument,
                "humidity", humidityArgument
        );

        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return temperature >= 50 && humidity >= 50;", null, null)
        );
        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature and Humidity Alarm",
                arguments, createRules, null, configuration -> {
                    configuration.getCreateRules().get(AlarmSeverity.CRITICAL).setAlarmDetails(
                            "temperature is ${temperature}, humidity is ${humidity}"
                    );
                });

        postTelemetry(deviceId, "{\"temperature\":50}");
        postAttributes(deviceId, AttributeScope.SERVER_SCOPE, "{\"humidity\":50}");

        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getDetails().get("data").asText())
                    .isEqualTo("temperature is 50, humidity is 50");
        });

        ((AlarmCalculatedFieldConfiguration) calculatedField.getConfiguration()).getCreateRules().get(AlarmSeverity.CRITICAL).setAlarmDetails(
                "UPDATED temperature is ${temperature}, humidity is ${humidity}"
        );
        calculatedField = saveCalculatedField(calculatedField);
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isFalse();
            assertThat(alarmResult.isUpdated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getDetails().get("data").asText())
                    .isEqualTo("UPDATED temperature is 50, humidity is 50");
        });
    }

    @Test
    public void testCreateAlarm_scheduleStarted() throws Exception {
        Argument parkingSpotOccupiedArgument = new Argument();
        parkingSpotOccupiedArgument.setRefEntityKey(new ReferencedEntityKey("parkingSpotOccupied", ArgumentType.ATTRIBUTE, AttributeScope.SERVER_SCOPE));
        parkingSpotOccupiedArgument.setDefaultValue("false");
        Map<String, Argument> arguments = Map.of(
                "parkingSpotOccupied", parkingSpotOccupiedArgument
        );

        SpecificTimeSchedule schedule = new SpecificTimeSchedule();
        schedule.setTimezone(ZoneId.systemDefault().getId());
        schedule.setDaysOfWeek(Set.of(1, 2, 3, 4, 5, 6, 7));
        long startsOn = Duration.between(LocalDate.now().atStartOfDay(), LocalDateTime.now())
                .plus(15, ChronoUnit.SECONDS).toMillis();
        schedule.setStartsOn(startsOn);
        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return parkingSpotOccupied == true;", null, null, null,
                        new AlarmConditionValue<>(schedule, null))
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "Illegal parking alarm",
                arguments, createRules, null);

        postAttributes(deviceId, AttributeScope.SERVER_SCOPE, "{\"parkingSpotOccupied\":true}");

        Thread.sleep(10000);
        assertThat(getLatestAlarmResult(calculatedField.getId())).isNull();

        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            CalculatedFieldDebugEvent debugEvent = getDebugEvents(calculatedField.getId(), 5).stream()
                    .filter(event -> event.getResult() != null)
                    .findFirst().orElse(null);
            assertThat(debugEvent).isNotNull();
            TbAlarmResult alarmResult = JacksonUtil.fromString(debugEvent.getResult(), TbAlarmResult.class);
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });
    }

    @Test
    public void testManualClearAlarm() throws Exception {
        Argument temperatureArgument = new Argument();
        temperatureArgument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
        temperatureArgument.setDefaultValue("0");
        Map<String, Argument> arguments = Map.of(
                "temperature", temperatureArgument
        );

        Map<AlarmSeverity, Condition> createRules = Map.of(
                AlarmSeverity.CRITICAL, new Condition("return temperature >= 50;", null, null)
        );

        CalculatedField calculatedField = createAlarmCf(deviceId, "High Temperature Alarm",
                arguments, createRules, null);

        postTelemetry(deviceId, "{\"temperature\":50}");
        Alarm alarm = checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        }).getAlarm();

        doPost("/api/alarm/" + alarm.getId() + "/clear", AlarmInfo.class);
        Thread.sleep(1000);
        postTelemetry(deviceId, "{\"temperature\":50}");
        checkAlarmResult(calculatedField, alarmResult -> {
            assertThat(alarmResult.getAlarm().getId()).isNotEqualTo(alarm.getId());
            assertThat(alarmResult.isCreated()).isTrue();
            assertThat(alarmResult.getAlarm().getSeverity()).isEqualTo(AlarmSeverity.CRITICAL);
            assertThat(alarmResult.getAlarm().getStatus()).isEqualTo(AlarmStatus.ACTIVE_UNACK);
        });
    }

    // TODO: MSA tests

    private TbAlarmResult checkAlarmResult(CalculatedField calculatedField, Consumer<TbAlarmResult> assertion) {
        return checkAlarmResult(calculatedField, null, assertion);
    }

    private TbAlarmResult checkAlarmResult(CalculatedField calculatedField,
                                           Predicate<TbAlarmResult> waitFor,
                                           Consumer<TbAlarmResult> assertion) {
        TbAlarmResult alarmResult = await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getLatestAlarmResult(calculatedField.getId()), result ->
                        result != null && (waitFor == null || waitFor.test(result)));
        assertion.accept(alarmResult);

        Alarm alarm = alarmResult.getAlarm();
        assertThat(alarm.getOriginator()).isEqualTo(originatorId);
        assertThat(alarm.getType()).isEqualTo(calculatedField.getName());
        return alarmResult;
    }

    private TbAlarmResult getLatestAlarmResult(CalculatedFieldId calculatedFieldId) {
        List<CalculatedFieldDebugEvent> debugEvents = getDebugEvents(calculatedFieldId, 1);
        if (debugEvents.isEmpty()) {
            return null;
        }
        CalculatedFieldDebugEvent debugEvent = debugEvents.get(0);
        if (debugEvent.getError() != null) {
            throw new RuntimeException(debugEvent.getError());
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
                                          Condition clearCondition,
                                          Consumer<AlarmCalculatedFieldConfiguration>... modifier) {
        Map<AlarmSeverity, AlarmRule> createRules = new HashMap<>();
        createConditions.forEach((severity, condition) -> {
            createRules.put(severity, toAlarmRule(condition));
        });
        AlarmRule clearRule = clearCondition != null ? toAlarmRule(clearCondition) : null;

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
        if (modifier.length > 0) {
            modifier[0].accept(configuration);
        }
        CalculatedField savedCalculatedField = saveCalculatedField(calculatedField);

        CalculatedFieldDebugEvent debugEvent = await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> getDebugEvents(savedCalculatedField.getId(), 1),
                        events -> !events.isEmpty()).get(0);
        latestEventId = debugEvent.getId();
        return savedCalculatedField;
    }

    private AlarmRule toAlarmRule(Condition condition) {
        AlarmRule rule = new AlarmRule();
        AlarmConditionExpression expression;
        if (condition.getTbelExpression() != null) {
            TbelAlarmConditionExpression tbelExpression = new TbelAlarmConditionExpression();
            tbelExpression.setExpression(condition.getTbelExpression());
            expression = tbelExpression;
        } else {
            expression = condition.getSimpleExpression();
        }
        if (condition.getEventsCount() != null) {
            RepeatingAlarmCondition alarmCondition = new RepeatingAlarmCondition();
            alarmCondition.setExpression(expression);
            alarmCondition.setCount(condition.getEventsCount());
            rule.setCondition(alarmCondition);
        } else if (condition.getDuration() != null) {
            DurationAlarmCondition alarmCondition = new DurationAlarmCondition();
            alarmCondition.setExpression(expression);
            alarmCondition.setUnit(TimeUnit.MILLISECONDS);
            alarmCondition.setValue(condition.getDuration());
            rule.setCondition(alarmCondition);
        } else {
            SimpleAlarmCondition alarmCondition = new SimpleAlarmCondition();
            alarmCondition.setExpression(expression);
            rule.setCondition(alarmCondition);
        }
        if (condition.getSchedule() != null) {
            rule.getCondition().setSchedule(condition.getSchedule());
        }
        return rule;
    }

    private SimpleAlarmConditionExpression createSimpleExpression(String argument, StringOperation stringOperation, AlarmConditionValue<String> conditionValue) {
        SimpleAlarmConditionExpression simpleExpression = new SimpleAlarmConditionExpression();
        AlarmConditionFilter filter = new AlarmConditionFilter();
        filter.setArgument(argument);
        filter.setValueType(EntityKeyValueType.STRING);
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setOperation(stringOperation);
        predicate.setValue(conditionValue);
        filter.setPredicate(predicate);
        simpleExpression.setFilters(List.of(filter));
        return simpleExpression;
    }

    private List<CalculatedFieldDebugEvent> getDebugEvents(CalculatedFieldId calculatedFieldId, int limit) {
        return eventDao.findLatestEvents(tenantId.getId(), calculatedFieldId.getId(), EventType.DEBUG_CALCULATED_FIELD, limit).stream()
                .map(e -> (CalculatedFieldDebugEvent) e).toList();
    }

    @Getter
    @AllArgsConstructor
    private static final class Condition {

        private final String tbelExpression;
        private final SimpleAlarmConditionExpression simpleExpression;
        private AlarmConditionValue<Integer> eventsCount;
        private AlarmConditionValue<Long> duration;
        private AlarmConditionValue<AlarmSchedule> schedule;

        private Condition(String tbelExpression, Integer eventsCount, Long durationMs) {
            this.tbelExpression = tbelExpression;
            this.simpleExpression = null;
            if (eventsCount != null) {
                this.eventsCount = new AlarmConditionValue<>(eventsCount, null);
            }
            if (durationMs != null) {
                this.duration = new AlarmConditionValue<>(durationMs, null);
            }
        }

        private Condition(SimpleAlarmConditionExpression simpleExpression, Integer eventsCount, Long durationMs) {
            this.tbelExpression = null;
            this.simpleExpression = simpleExpression;
            if (eventsCount != null) {
                this.eventsCount = new AlarmConditionValue<>(eventsCount, null);
            }
            if (durationMs != null) {
                this.duration = new AlarmConditionValue<>(durationMs, null);
            }
        }

    }

}
