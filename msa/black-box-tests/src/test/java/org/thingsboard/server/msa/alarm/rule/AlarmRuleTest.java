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
package org.thingsboard.server.msa.alarm.rule;

import org.awaitility.Awaitility;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleOriginatorTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType;
import org.thingsboard.server.common.data.alarm.rule.condition.Operation;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.WsClient;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
public class AlarmRuleTest extends AbstractContainerTest {

    private Device device;

    @BeforeMethod
    public void setUp() {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("alarmRule_"));
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(device.getId());
    }

    @Test
    public void testCreateAndClearAlarm() throws Exception {
        var alarmRule = createAlarmRule();
        alarmRule = testRestClient.postAlarmRule(alarmRule);

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 42.0)));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(42.0));

        AlarmInfo alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 5.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(5.0));

        alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(true);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        testRestClient.deleteAlarmRule(alarmRule.getId());
    }

    @Test
    public void testCreateAndClearAlarmAfterAlarmRuleUpdate() throws Exception {
        var alarmRule = createAlarmRule();

        AlarmRuleCondition clearRule = alarmRule.getConfiguration().getClearRule();

        alarmRule.getConfiguration().setClearRule(null);

        alarmRule = testRestClient.postAlarmRule(alarmRule);

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 42.0)));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(42.0));

        AlarmInfo alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 5.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(5.0));

        alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        alarmRule.getConfiguration().setClearRule(clearRule);

        alarmRule = testRestClient.postAlarmRule(alarmRule);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 5.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(5.0));

        alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(true);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        testRestClient.deleteAlarmRule(alarmRule.getId());
    }

    @Test
    public void testCreateAndNotClearAlarmAfterAlarmRuleUpdate() throws Exception {
        var alarmRule = createAlarmRule();
        alarmRule = testRestClient.postAlarmRule(alarmRule);

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 42.0)));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(42.0));

        AlarmInfo alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        alarmRule.getConfiguration().setClearRule(null);

        alarmRule = testRestClient.postAlarmRule(alarmRule);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 5.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(5.0));

        alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        testRestClient.deleteAlarmRule(alarmRule.getId());
    }

    @Test
    public void testCreateAndClearAlarmWithDynamicValueAfterAttributeUpdate() throws Exception {
        var alarmRule = createAlarmRule();

        AlarmRuleCondition clearRule = alarmRule.getConfiguration().getClearRule();

        alarmRule.getConfiguration().setClearRule(null);

        alarmRule = testRestClient.postAlarmRule(alarmRule);

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 22.0)));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(22.0));

        PageData<AlarmInfo> data = testRestClient.getEntityAlarms(device.getId(), new TimePageLink(10));

        List<AlarmInfo> alarms = data.getData();
        assertThat(CollectionsUtil.isEmpty(alarms)).isTrue();

        wsClient = subscribeToWebSocket(device.getId(), "SERVER_SCOPE", CmdsType.ATTR_SUB_CMDS);
        testRestClient.postTelemetryAttribute(device.getId(), "SERVER_SCOPE", mapper.valueToTree(Map.of("temperatureThreshold", 20.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperatureThreshold").get(1)).isEqualTo(Double.toString(20.0));

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 22.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(22.0));

        AlarmInfo alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 5.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(5.0));

        alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        alarmRule.getConfiguration().setClearRule(clearRule);

        alarmRule = testRestClient.postAlarmRule(alarmRule);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 5.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(5.0));

        alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(true);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        testRestClient.deleteAlarmRule(alarmRule.getId());
    }

    @Test
    public void testCreateAndClearAlarmWhenOldAlarmRemoved() throws Exception {
        var alarmRule = createAlarmRule();
        alarmRule = testRestClient.postAlarmRule(alarmRule);

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 42.0)));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(42.0));

        AlarmInfo alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        testRestClient.deleteAlarm(alarm.getId());

        var data = testRestClient.getEntityAlarms(device.getId(), new TimePageLink(10));

        var alarms = data.getData();
        assertThat(CollectionsUtil.isEmpty(alarms)).isTrue();

        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 42.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(42.0));

        alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 5.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(5.0));

        alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(true);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        testRestClient.deleteAlarmRule(alarmRule.getId());
    }

    @Test
    public void testNotCreateAlarmWhenAlarmRuleRemoved() throws Exception {
        var alarmRule = createAlarmRule();
        alarmRule = testRestClient.postAlarmRule(alarmRule);

        DeviceCredentials deviceCredentials = testRestClient.getDeviceCredentialsByDeviceId(device.getId());

        WsClient wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 42.0)));

        WsTelemetryResponse actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(42.0));

        AlarmInfo alarm = getSingleAlarm(device.getId());

        assertThat(alarm.getType()).isEqualTo("highTemperatureAlarm");
        assertThat(alarm.isCleared()).isEqualTo(false);
        assertThat(alarm.isAcknowledged()).isEqualTo(false);

        testRestClient.deleteAlarmRule(alarmRule.getId());
        testRestClient.deleteAlarm(alarm.getId());

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentials.getCredentialsId(), mapper.valueToTree(Map.of("temperature", 42.0)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey("temperature").get(1)).isEqualTo(Double.toString(42.0));

        var data = testRestClient.getEntityAlarms(device.getId(), new TimePageLink(10));

        assertThat(CollectionsUtil.isEmpty(data.getData())).isTrue();
    }

    private AlarmInfo getSingleAlarm(EntityId entityId) {
        Awaitility
                .await()
                .alias("Get Alarm")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> CollectionsUtil.isNotEmpty(testRestClient.getEntityAlarms(entityId, new TimePageLink(10)).getData()));

        PageData<AlarmInfo> data = testRestClient.getEntityAlarms(entityId, new TimePageLink(10));
        List<AlarmInfo> alarms = data.getData();
        assertThat(alarms.size()).isEqualTo(1);
        AlarmInfo alarm = alarms.get(0);
        assertThat(alarm).isNotNull();
        return alarm;
    }

    private AlarmRule createAlarmRule() {
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument temperatureThreshold = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "temperatureThreshold"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("temperatureThreshold");
        highTempFilter.setOperation(Operation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureThreshold", temperatureThreshold));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleArgument lowTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(10.0)
                .build();

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(Operation.LESS);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearRule.setArguments(Map.of("temperatureKey", temperatureKey, "lowTemperatureConst", lowTemperatureConst));
        clearCondition.setCondition(lowTempFilter);
        clearRule.setCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(device.getDeviceProfileId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);
        return alarmRule;
    }

}
