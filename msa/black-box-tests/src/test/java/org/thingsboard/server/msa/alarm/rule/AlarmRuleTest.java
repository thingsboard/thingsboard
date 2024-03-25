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
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleInfo;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType;
import org.thingsboard.server.common.data.alarm.rule.condition.AttributeArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ConstantArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.FromMessageArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
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
import static org.thingsboard.server.common.data.alarm.rule.condition.ValueSourceType.CURRENT_ENTITY;
import static org.thingsboard.server.msa.prototypes.DevicePrototypes.defaultDevicePrototype;

@DisableUIListeners
public class AlarmRuleTest extends AbstractContainerTest {

    private Device device;
    private DeviceId deviceId;
    private String deviceCredentialsId;

    @BeforeMethod
    public void setUp() {
        testRestClient.login("tenant@thingsboard.org", "tenant");
        device = testRestClient.postDevice("", defaultDevicePrototype("alarmRule_"));
        deviceId = device.getId();
        deviceCredentialsId = testRestClient.getDeviceCredentialsByDeviceId(deviceId).getCredentialsId();
    }

    @AfterMethod
    public void tearDown() {
        testRestClient.deleteDeviceIfExists(deviceId);
        PageData<AlarmRuleInfo> data = testRestClient.getAlarmRules(new PageLink(100));
        data.getData().forEach(info -> testRestClient.deleteAlarmRule(info.getId()));
    }

    @Test
    public void testCreateAndClearAlarm() throws Exception {
        var alarmRule = createAlarmRule();
        testRestClient.postAlarmRule(alarmRule);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 42.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", false);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 5.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", true);
    }

    @Test
    public void testCreateAndClearAlarmAfterAlarmRuleUpdate() throws Exception {
        var alarmRule = createAlarmRule();
        AlarmRuleCondition clearRule = alarmRule.getConfiguration().getClearRule();
        alarmRule.getConfiguration().setClearRule(null);
        alarmRule = testRestClient.postAlarmRule(alarmRule);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 42.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", false);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 5.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", false);

        alarmRule.getConfiguration().setClearRule(clearRule);
        testRestClient.postAlarmRule(alarmRule);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 5);

        checkLastAlarm(deviceId, "highTemperatureAlarm", true);
    }

    @Test
    public void testCreateAndNotClearAlarmAfterAlarmRuleUpdate() throws Exception {
        var alarmRule = createAlarmRule();
        alarmRule = testRestClient.postAlarmRule(alarmRule);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 42.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", false);

        alarmRule.getConfiguration().setClearRule(null);
        testRestClient.postAlarmRule(alarmRule);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 5.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", false);
    }

    @Test
    public void testCreateAndClearAlarmWithDynamicValueAfterAttributeUpdate() throws Exception {
        var alarmRule = createAlarmRule();
        testRestClient.postAlarmRule(alarmRule);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 22.0);

        PageData<AlarmInfo> data = testRestClient.getEntityAlarms(deviceId, new TimePageLink(10));
        List<AlarmInfo> alarms = data.getData();
        assertThat(CollectionsUtil.isEmpty(alarms)).isTrue();

        postAttributeAndAwait(deviceCredentialsId, "temperatureThreshold", 20.0);
        postTelemetryAndAwait(deviceCredentialsId, "temperature", 22.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", false);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 5.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", true);
    }

    @Test
    public void testCreateAndClearAlarmWhenOldAlarmRemoved() throws Exception {
        var alarmRule = createAlarmRule();
        testRestClient.postAlarmRule(alarmRule);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 42.0);

        var alarm = checkLastAlarm(deviceId, "highTemperatureAlarm", false);
        testRestClient.deleteAlarm(alarm.getId());

        var data = testRestClient.getEntityAlarms(deviceId, new TimePageLink(10));
        var alarms = data.getData();
        assertThat(CollectionsUtil.isEmpty(alarms)).isTrue();

        Thread.sleep(2000);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 42.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", false);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 5.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", true);
    }

    @Test
    public void testNotCreateAlarmWhenAlarmRuleRemoved() throws Exception {
        var alarmRule = createAlarmRule();
        alarmRule = testRestClient.postAlarmRule(alarmRule);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 42.0);

        var alarm = checkLastAlarm(deviceId, "highTemperatureAlarm", false);

        testRestClient.deleteAlarmRule(alarmRule.getId());
        testRestClient.deleteAlarm(alarm.getId());

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 42.0);

        var data = testRestClient.getEntityAlarms(deviceId, new TimePageLink(10));
        assertThat(CollectionsUtil.isEmpty(data.getData())).isTrue();
    }

    @Test
    public void testCreateAndClearAlarmAfterManualClear() throws Exception {
        var alarmRule = createAlarmRule();
        testRestClient.postAlarmRule(alarmRule);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 42.0);

        var alarm = checkLastAlarm(deviceId, "highTemperatureAlarm", false);

        testRestClient.clearAlarm(alarm.getId());

        checkLastAlarm(deviceId, "highTemperatureAlarm", true);

        Thread.sleep(2000);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 42.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", false);

        postTelemetryAndAwait(deviceCredentialsId, "temperature", 5.0);

        checkLastAlarm(deviceId, "highTemperatureAlarm", true);
    }

    private void postTelemetryAndAwait(String deviceCredentialsId, String key, double value) throws Exception {
        WsClient wsClient;
        WsTelemetryResponse actualLatestTelemetry;
        wsClient = subscribeToWebSocket(deviceId, "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);
        testRestClient.postTelemetry(deviceCredentialsId, mapper.valueToTree(Map.of(key, value)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey(key).get(1)).isEqualTo(Double.toString(value));
    }

    private void postAttributeAndAwait(String deviceCredentialsId, String key, double value) throws Exception {
        WsClient wsClient;
        WsTelemetryResponse actualLatestTelemetry;
        wsClient = subscribeToWebSocket(deviceId, "CLIENT_SCOPE", CmdsType.ATTR_SUB_CMDS);
        testRestClient.postAttribute(deviceCredentialsId, mapper.valueToTree(Map.of(key, value)));

        actualLatestTelemetry = wsClient.getLastMessage();
        wsClient.closeBlocking();

        assertThat(actualLatestTelemetry.getDataValuesByKey(key).get(1)).isEqualTo(Double.toString(value));
    }

    private AlarmInfo checkLastAlarm(EntityId entityId, String alarmType, boolean cleared) {
        Awaitility
                .await()
                .alias("Get Alarm")
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    var data = testRestClient.getEntityAlarms(entityId, new TimePageLink(1, 0, null, SortOrder.BY_CREATED_TIME_DESC)).getData();
                    return CollectionsUtil.isNotEmpty(data) && data.get(0).isCleared() == cleared;
                });

        PageData<AlarmInfo> data = testRestClient.getEntityAlarms(entityId, new TimePageLink(1));
        List<AlarmInfo> alarms = data.getData();
        AlarmInfo alarm = alarms.get(0);
        assertThat(alarm).isNotNull();
        assertThat(alarm.getType()).isEqualTo(alarmType);
        return alarm;
    }

    private AlarmRule createAlarmRule() {
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", ArgumentValueType.NUMERIC);
        var temperatureThreshold = new AttributeArgument("temperatureThreshold", ArgumentValueType.NUMERIC, CURRENT_ENTITY, 30.0, false);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("temperatureThreshold");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(ArgumentValueType.NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureThreshold", temperatureThreshold, "lowTemperatureConst", lowTemperatureConst));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(device.getDeviceProfileId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);
        return alarmRule;
    }

}
