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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.action.TbAlarmResult;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
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
import org.thingsboard.server.common.data.page.PageLink;
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
        assertThat(getCalculatedFields(deviceId, CalculatedFieldType.ALARM, new PageLink(1)).getData())
                .singleElement().isEqualTo(calculatedField);

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

    /*
     * todo: test state restore (event count)
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
        Condition clearRule = new Condition("return powerConsumption < 3000;", null, createDurationMs);

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

    private void checkAlarmResult(CalculatedField calculatedField, Consumer<TbAlarmResult> assertion) {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).untilAsserted(() -> {
            TbAlarmResult alarmResult = getLatestAlarmResult(calculatedField.getId());
            assertThat(alarmResult).isNotNull();
            assertion.accept(alarmResult);

            Alarm alarm = alarmResult.getAlarm();
            assertThat(alarm.getOriginator()).isEqualTo(originatorId);
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
            AlarmConditionValue<Integer> count = new AlarmConditionValue<>();
            count.setStaticValue(condition.getEventsCount());
            alarmCondition.setCount(count);
            rule.setCondition(alarmCondition);
        } else if (condition.getDurationMs() != null) {
            DurationAlarmCondition alarmCondition = new DurationAlarmCondition();
            alarmCondition.setExpression(expression);
            alarmCondition.setUnit(TimeUnit.MILLISECONDS);
            AlarmConditionValue<Long> duration = new AlarmConditionValue<>();
            duration.setStaticValue(condition.getDurationMs());
            alarmCondition.setValue(duration);
            rule.setCondition(alarmCondition);
        } else {
            SimpleAlarmCondition alarmCondition = new SimpleAlarmCondition();
            alarmCondition.setExpression(expression);
            rule.setCondition(alarmCondition);
        }
        return rule;
    }

    private SimpleAlarmConditionExpression createSimpleExpression(String argument, StringOperation stringOperation, AlarmConditionValue<String> conditionValue) {
        SimpleAlarmConditionExpression simpleExpression = new SimpleAlarmConditionExpression();
        AlarmConditionFilter filter = new AlarmConditionFilter();
        filter.setArgument(argument);
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
    private static final class Condition {

        private final String tbelExpression;
        private final SimpleAlarmConditionExpression simpleExpression;
        private final Integer eventsCount;
        private final Long durationMs;

        private Condition(String tbelExpression, Integer eventsCount, Long durationMs) {
            this.tbelExpression = tbelExpression;
            this.simpleExpression = null;
            this.eventsCount = eventsCount;
            this.durationMs = durationMs;
        }

        private Condition(SimpleAlarmConditionExpression simpleExpression, Integer eventsCount, Long durationMs) {
            this.tbelExpression = null;
            this.simpleExpression = simpleExpression;
            this.eventsCount = eventsCount;
            this.durationMs = durationMs;
        }

    }

}
