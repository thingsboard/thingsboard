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
package org.thingsboard.server.service.alarm.rule;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType;
import org.thingsboard.server.common.data.alarm.rule.condition.AttributeArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.ComplexAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.ConstantArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.CustomTimeSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.CustomTimeScheduleItem;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.FromMessageArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.NoUpdateAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentOperation;
import org.thingsboard.server.common.data.alarm.rule.condition.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.SimpleAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAllAssetsEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAllDevicesEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAssetTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityListEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleSingleEntityFilter;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AlarmRuleId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.queue.DefaultTbClusterService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType.NUMERIC;
import static org.thingsboard.server.common.data.alarm.rule.condition.ValueSourceType.CURRENT_CUSTOMER;
import static org.thingsboard.server.common.data.alarm.rule.condition.ValueSourceType.CURRENT_ENTITY;
import static org.thingsboard.server.common.data.alarm.rule.condition.ValueSourceType.CURRENT_TENANT;
import static org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent.UPDATED;

@DaoSqlTest
@SuppressWarnings("removal")
public class DefaultTbAlarmRuleStateServiceTest extends AbstractControllerTest {

    @Autowired
    private AlarmRuleService alarmRuleService;

    @SpyBean
    private DefaultTbAlarmRuleStateService alarmRuleStateService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AttributesService attributesService;

    @SpyBean
    private DefaultTbClusterService clusterService;

    private TbContext ctx;

    @Before
    public void setup() {
        ctx = Mockito.mock(TbContext.class);
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getSelf()).thenReturn(new RuleNode());
    }

    @Test
    public void testCreateAndClearAlarm() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst, "lowTemperatureConst", lowTemperatureConst));
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Updated")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());

        data.put("temperature", 5);
        TbMsg msg3 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg3);

        Mockito.verify(clusterService, Mockito.timeout(1000)).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Cleared")), any());

        pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.CLEARED_UNACK, alarm.getStatus());
    }

    @Test
    public void testAlarmSeverityUpdate() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var temperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter tempFilter = new SimpleAlarmConditionFilter();
        tempFilter.setLeftArgId("temperatureKey");
        tempFilter.setRightArgId("temperatureConst");
        tempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition tempAlarmCondition = new AlarmCondition();
        tempAlarmCondition.setConditionFilter(tempFilter);
        AlarmRuleCondition tempAlarmRuleCondition = new AlarmRuleCondition();
        tempAlarmRuleCondition.setAlarmCondition(tempAlarmCondition);

        var highTemperatureConst = new ConstantArgument(NUMERIC, 50.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition highTempAlarmCondition = new AlarmCondition();
        highTempAlarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition highTempAlarmRuleCondition = new AlarmRuleCondition();
        highTempAlarmRuleCondition.setAlarmCondition(highTempAlarmCondition);

        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureConst", temperatureConst, "highTemperatureConst", highTemperatureConst));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Map.of(AlarmSeverity.WARNING, tempAlarmRuleCondition, AlarmSeverity.CRITICAL, highTempAlarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        data.put("temperature", 52);
        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Severity Updated")), any());
    }

    @Test
    public void testConstantKeyFilterSimple() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(deviceId, "alarmEnabled", true);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("alarmEnabledAlarm");
        alarmRule.setName("alarmEnabledAlarmRule");
        alarmRule.setEnabled(true);

        var alarmEnabledConst = new ConstantArgument(ArgumentValueType.BOOLEAN, Boolean.TRUE);
        var alarmEnabledKey = new AttributeArgument("alarmEnabled", ArgumentValueType.BOOLEAN, CURRENT_ENTITY, null, false);

        SimpleAlarmConditionFilter alarmEnabledFilter = new SimpleAlarmConditionFilter();
        alarmEnabledFilter.setLeftArgId("alarmEnabledConst");
        alarmEnabledFilter.setRightArgId("alarmEnabledKey");
        alarmEnabledFilter.setOperation(ArgumentOperation.EQUAL);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var temperatureConst = new ConstantArgument(NUMERIC, 20.0);

        SimpleAlarmConditionFilter temperatureFilter = new SimpleAlarmConditionFilter();
        temperatureFilter.setLeftArgId("temperatureKey");
        temperatureFilter.setRightArgId("temperatureConst");
        temperatureFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(new ComplexAlarmConditionFilter(Arrays.asList(alarmEnabledFilter, temperatureFilter), ComplexAlarmConditionFilter.ComplexOperation.AND));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of(
                "alarmEnabledConst", alarmEnabledConst,
                "alarmEnabledKey", alarmEnabledKey,
                "temperatureKey", temperatureKey,
                "temperatureConst", temperatureConst));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 21);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("alarmEnabledAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testConstantKeyFilterInherited() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(tenantId, "alarmEnabled", true);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("alarmEnabledAlarm");
        alarmRule.setName("alarmEnabledAlarmRule");
        alarmRule.setEnabled(true);

        var alarmEnabledConst = new ConstantArgument(ArgumentValueType.BOOLEAN, Boolean.TRUE);
        var alarmEnabledKey = new AttributeArgument("alarmEnabled", ArgumentValueType.BOOLEAN, CURRENT_ENTITY, null, true);

        SimpleAlarmConditionFilter alarmEnabledFilter = new SimpleAlarmConditionFilter();
        alarmEnabledFilter.setLeftArgId("alarmEnabledConst");
        alarmEnabledFilter.setRightArgId("alarmEnabledKey");
        alarmEnabledFilter.setOperation(ArgumentOperation.EQUAL);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var temperatureConst = new ConstantArgument(NUMERIC, 20.0);

        SimpleAlarmConditionFilter temperatureFilter = new SimpleAlarmConditionFilter();
        temperatureFilter.setLeftArgId("temperatureKey");
        temperatureFilter.setRightArgId("temperatureConst");
        temperatureFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(new ComplexAlarmConditionFilter(Arrays.asList(alarmEnabledFilter, temperatureFilter), ComplexAlarmConditionFilter.ComplexOperation.AND));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of(
                "alarmEnabledConst", alarmEnabledConst,
                "alarmEnabledKey", alarmEnabledKey,
                "temperatureKey", temperatureKey,
                "temperatureConst", temperatureConst));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 21);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("alarmEnabledAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testSingleEntityFilter() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        Device sourceEntity = new Device();
        sourceEntity.setTenantId(tenantId);
        sourceEntity.setName("alarm owner");
        sourceEntity.setDeviceProfileId(deviceProfile.getId());
        sourceEntity = deviceService.saveDevice(sourceEntity);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);

        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst, "lowTemperatureConst", lowTemperatureConst));

        AlarmRuleSingleEntityFilter sourceFilter = new AlarmRuleSingleEntityFilter(sourceEntity.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msgFromDifferentDevice = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromDifferentDevice);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msgFromSourceEntity = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), sourceEntity.getId(), new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromSourceEntity);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(sourceEntity.getId()), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(sourceEntity.getId(),
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testDeviceTypeEntityFilter() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceProfile sourceDeviceProfile = createDeviceProfile("source device profile");
        sourceDeviceProfile.setTenantId(tenantId);
        sourceDeviceProfile = deviceProfileService.saveDeviceProfile(sourceDeviceProfile);

        Device sourceEntity = new Device();
        sourceEntity.setTenantId(tenantId);
        sourceEntity.setName("alarm owner");
        sourceEntity.setDeviceProfileId(sourceDeviceProfile.getId());
        sourceEntity = deviceService.saveDevice(sourceEntity);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);

        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst, "lowTemperatureConst", lowTemperatureConst));

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(sourceDeviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msgFromDifferentDevice = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromDifferentDevice);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msgFromSourceEntity = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), sourceEntity.getId(), new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromSourceEntity);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(sourceEntity.getId()), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(sourceEntity.getId(),
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testAssetTypeEntityFilter() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        AssetProfile sourceAssetProfile = createAssetProfile("source asset profile");
        sourceAssetProfile.setTenantId(tenantId);
        sourceAssetProfile = assetProfileService.saveAssetProfile(sourceAssetProfile);

        Asset sourceEntity = new Asset();
        sourceEntity.setTenantId(tenantId);
        sourceEntity.setName("alarm owner");
        sourceEntity.setAssetProfileId(sourceAssetProfile.getId());
        sourceEntity = assetService.saveAsset(sourceEntity);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);

        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleAssetTypeEntityFilter(List.of(sourceAssetProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst, "lowTemperatureConst", lowTemperatureConst));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msgFromDifferentDevice = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromDifferentDevice);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msgFromSourceEntity = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), sourceEntity.getId(), new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromSourceEntity);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(sourceEntity.getId()), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(sourceEntity.getId(),
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testAllDevicesEntityFilter() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("device profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AssetProfile assetProfile = createAssetProfile("asset profile");
        assetProfile.setTenantId(tenantId);
        assetProfile = assetProfileService.saveAssetProfile(assetProfile);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("asset");
        asset.setAssetProfileId(assetProfile.getId());
        asset = assetService.saveAsset(asset);

        AssetId assetId = asset.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);

        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst, "lowTemperatureConst", lowTemperatureConst));

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleAllDevicesEntityFilter();
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);

        TbMsg msgFromAsset = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), assetId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromAsset);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(assetId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, tbMsg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testAllAssetsEntityFilter() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("device profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AssetProfile assetProfile = createAssetProfile("asset profile");
        assetProfile.setTenantId(tenantId);
        assetProfile = assetProfileService.saveAssetProfile(assetProfile);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("asset");
        asset.setAssetProfileId(assetProfile.getId());
        asset = assetService.saveAsset(asset);

        AssetId assetId = asset.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);

        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst, "lowTemperatureConst", lowTemperatureConst));

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleAllAssetsEntityFilter();
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);

        TbMsg msgFromDevice = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromDevice);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg tbMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), assetId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, tbMsg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(assetId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(assetId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testEntityListEntityFilter() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test device profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device1 = new Device();
        device1.setTenantId(tenantId);
        device1.setName("test device 1");
        device1.setDeviceProfileId(deviceProfile.getId());
        device1 = deviceService.saveDevice(device1);

        DeviceId device1Id = device1.getId();

        Device device2 = new Device();
        device2.setTenantId(tenantId);
        device2.setName("test device 2");
        device2.setDeviceProfileId(deviceProfile.getId());
        device2 = deviceService.saveDevice(device2);

        DeviceId device2Id = device2.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);

        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst, "lowTemperatureConst", lowTemperatureConst));

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleEntityListEntityFilter(EntityType.DEVICE, Arrays.asList(device1Id, device2Id));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg deviceMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), device1Id, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, deviceMsg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(device1Id), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> deviceAlarmPageData = alarmService.findAlarms(tenantId, new AlarmQuery(device1Id,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> deviceAlarms = deviceAlarmPageData.getData();
        Assert.equals(1, deviceAlarms.size());

        AlarmInfo deviceAlarm = deviceAlarms.get(0);
        Assert.equals("highTemperatureAlarm", deviceAlarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, deviceAlarm.getStatus());

        TbMsg assetMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), device2Id, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, assetMsg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(device2Id), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> assetPageData = alarmService.findAlarms(tenantId, new AlarmQuery(device2Id,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> assetAlarms = assetPageData.getData();
        Assert.equals(1, assetAlarms.size());

        AlarmInfo assetAlarm = assetAlarms.get(0);
        Assert.equals("highTemperatureAlarm", assetAlarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, assetAlarm.getStatus());
    }

    @Test
    public void testDeviceTypeAndAssetTypeEntityFilters() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test device profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AssetProfile assetProfile = createAssetProfile("test asset profile");
        assetProfile.setTenantId(tenantId);
        assetProfile = assetProfileService.saveAssetProfile(assetProfile);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("test asset");
        asset.setAssetProfileId(assetProfile.getId());
        asset = assetService.saveAsset(asset);

        AssetId assetId = asset.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);

        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst, "lowTemperatureConst", lowTemperatureConst));

        AlarmRuleEntityFilter deviceTypeFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        AlarmRuleEntityFilter assetTypeFilter = new AlarmRuleAssetTypeEntityFilter(List.of(assetProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Arrays.asList(deviceTypeFilter, assetTypeFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg deviceMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, deviceMsg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> deviceAlarmPageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> deviceAlarms = deviceAlarmPageData.getData();
        Assert.equals(1, deviceAlarms.size());

        AlarmInfo deviceAlarm = deviceAlarms.get(0);
        Assert.equals("highTemperatureAlarm", deviceAlarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, deviceAlarm.getStatus());

        TbMsg assetMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), assetId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, assetMsg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(assetId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> assetPageData = alarmService.findAlarms(tenantId, new AlarmQuery(assetId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> assetAlarms = assetPageData.getData();
        Assert.equals(1, assetAlarms.size());

        AlarmInfo assetAlarm = assetAlarms.get(0);
        Assert.equals("highTemperatureAlarm", assetAlarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, assetAlarm.getStatus());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicValue() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(tenantId, "greaterAttribute", 30L);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var greaterAttributeKey = new AttributeArgument("greaterAttribute", NUMERIC, CURRENT_ENTITY, 0.0, false);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicDurationValue() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(deviceId, "greaterAttribute", 30L);

        long alarmDelayInSeconds = 5L;

        saveAttribute(deviceId, "alarm_delay", alarmDelayInSeconds);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var greaterAttributeKey = new AttributeArgument("greaterAttribute", NUMERIC, CURRENT_ENTITY, 0.0, false);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        var alarmDelayKey = new AttributeArgument("alarm_delay", NUMERIC, CURRENT_ENTITY, 10, false);

        DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setArgumentId("alarmDelayKey");
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmDelayKey", alarmDelayKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        long halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .longValueExact();

        Thread.sleep(halfOfAlarmDelay);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        Thread.sleep(halfOfAlarmDelay);

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testInheritTenantAttributeForDuration() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(deviceId, "greaterAttribute", 30L);

        long alarmDelayInSeconds = 5L;

        saveAttribute(tenantId, "alarm_delay", alarmDelayInSeconds);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var greaterAttributeKey = new AttributeArgument("greaterAttribute", NUMERIC, CURRENT_ENTITY, 0.0, false);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        var alarmDelayKey = new AttributeArgument("alarm_delay", NUMERIC, CURRENT_ENTITY, 10, true);

        DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setArgumentId("alarmDelayKey");
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmDelayKey", alarmDelayKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        long halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .longValueExact();

        Thread.sleep(halfOfAlarmDelay);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        Thread.sleep(halfOfAlarmDelay);

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicRepeatingValue() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(deviceId, "greaterAttribute", 30L);
        saveAttribute(deviceId, "alarm_repeating", 2L);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var greaterAttributeKey = new AttributeArgument("greaterAttribute", NUMERIC, CURRENT_ENTITY, 0.0, false);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        var alarmRepeatingKey = new AttributeArgument("alarm_repeating", NUMERIC, CURRENT_ENTITY, 10, false);

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setArgumentId("alarmRepeatingKey");
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmRepeatingKey", alarmRepeatingKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testInheritTenantAttributeForDynamicRepeatingValue() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(deviceId, "greaterAttribute", 30L);
        saveAttribute(tenantId, "alarm_repeating", 2L);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var greaterAttributeKey = new AttributeArgument("greaterAttribute", NUMERIC, CURRENT_ENTITY, 0.0, false);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        var alarmRepeatingKey = new AttributeArgument("alarm_repeating", NUMERIC, CURRENT_ENTITY, 10, true);

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setArgumentId("alarmRepeatingKey");
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmRepeatingKey", alarmRepeatingKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Ignore
    @Test
    public void testCurrentDeviceAttributeForUseDefaultDurationWhenDynamicDurationValueIsNull() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(deviceId, "greaterAttribute", 30L);

        long alarmDelayInSeconds = 5L;

        saveAttribute(deviceId, "alarm_delay", alarmDelayInSeconds);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var greaterAttributeKey = new AttributeArgument("greaterAttribute", NUMERIC, CURRENT_ENTITY, 0.0, false);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        var alarmDelayKey = new AttributeArgument(null, NUMERIC, CURRENT_ENTITY, alarmDelayInSeconds, false);

        DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setArgumentId("alarmDelayKey");
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmDelayKey", alarmDelayKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        long halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .longValueExact();

        Thread.sleep(halfOfAlarmDelay);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        Thread.sleep(halfOfAlarmDelay);

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Ignore
    @Test
    public void testCurrentDeviceAttributeForUseDefaultRepeatingWhenDynamicRepeatingValueIsNull() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(deviceId, "greaterAttribute", 30L);
        saveAttribute(tenantId, "alarm_repeating", 2L);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var greaterAttributeKey = new AttributeArgument("greaterAttribute", NUMERIC, CURRENT_ENTITY, 0.0, false);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        var alarmRepeatingKey = new AttributeArgument(null, NUMERIC, CURRENT_ENTITY, 2, true);

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setArgumentId("alarmRepeatingKey");
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmRepeatingKey", alarmRepeatingKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testRepeatingWithConstValue() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var temperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("temperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        var alarmRepeatingKey = new ConstantArgument(NUMERIC, 2);

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setArgumentId("alarmRepeatingKey");
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureConst", temperatureConst, "alarmRepeatingKey", alarmRepeatingKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ObjectNode differentData = JacksonUtil.newObjectNode();
        data.put("humidity", 85);
        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(differentData), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg3 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg3);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testActiveAlarmScheduleFromDynamicValuesWhenDefaultScheduleIsInactive() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveJsonAttribute(deviceId, "dynamicValueActiveSchedule", "{\"timezone\":\"Europe/Kiev\",\"items\":[{\"enabled\":true,\"dayOfWeek\":1,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":2,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":3,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":4,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":5,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":6,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":7,\"startsOn\":0,\"endsOn\":8.64e+7}],\"dynamicValue\":null}");

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var temperatureConst = new ConstantArgument(NUMERIC, 0.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("temperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        var scheduleKey = new AttributeArgument("dynamicValueActiveSchedule", NUMERIC, CURRENT_ENTITY, null, false);

        CustomTimeSchedule schedule = new CustomTimeSchedule();
        schedule.setItems(Collections.emptyList());
        schedule.setArgumentId("scheduleKey");

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        alarmRuleCondition.setSchedule(schedule);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureConst", temperatureConst, "scheduleKey", scheduleKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testInactiveAlarmScheduleFromDynamicValuesWhenDefaultScheduleIsActive() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveJsonAttribute(deviceId, "dynamicValueInactiveSchedule",
                "{\"timezone\":\"Europe/Kiev\",\"items\":[{\"enabled\":false,\"dayOfWeek\":1,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":2,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":3,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":4,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":5,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":6,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":7,\"startsOn\":0,\"endsOn\":0}],\"dynamicValue\":null}");

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var temperatureConst = new ConstantArgument(NUMERIC, 0.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("temperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        CustomTimeSchedule schedule = new CustomTimeSchedule();

        List<CustomTimeScheduleItem> items = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            CustomTimeScheduleItem item = new CustomTimeScheduleItem();
            item.setEnabled(true);
            item.setDayOfWeek(i + 1);
            item.setEndsOn(0);
            item.setStartsOn(0);
            items.add(item);
        }

        var scheduleKey = new AttributeArgument("dynamicValueInactiveSchedule", NUMERIC, CURRENT_ENTITY, null, false);

        schedule.setItems(items);
        schedule.setArgumentId("scheduleKey");

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        alarmRuleCondition.setSchedule(schedule);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureConst", temperatureConst, "scheduleKey", scheduleKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(0, alarms.size());
    }

    @Test
    public void testCurrentCustomersAttributeForDynamicValue() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(customerId, "lessAttribute", 30L);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("lessTemperatureAlarm");
        alarmRule.setName("lessTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var lessAttributeKey = new AttributeArgument("lessAttribute", NUMERIC, CURRENT_CUSTOMER, 20.0, true);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lessAttributeKey");
        lowTempFilter.setOperation(ArgumentOperation.LESS);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(lowTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "lessAttributeKey", lessAttributeKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 25);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("lessTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testCurrentTenantAttributeForDynamicValue() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(tenantId, "lessAttribute", 50L);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("lessTemperatureAlarm");
        alarmRule.setName("lessTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var lessAttributeKey = new AttributeArgument("lessAttribute", NUMERIC, CURRENT_TENANT, 32.0, true);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lessAttributeKey");
        lowTempFilter.setOperation(ArgumentOperation.LESS);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(lowTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "lessAttributeKey", lessAttributeKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 40);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("lessTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testTenantInheritModeForDynamicValues() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(tenantId, "tenantAttribute", 100L);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("lessTemperatureAlarm");
        alarmRule.setName("lessTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var tenantAttributeKey = new AttributeArgument("tenantAttribute", NUMERIC, CURRENT_ENTITY, 0.0, true);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("tenantAttributeKey");
        lowTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(lowTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "tenantAttributeKey", tenantAttributeKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 150L);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("lessTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testCustomerInheritModeForDynamicValues() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(tenantId, "tenantAttribute", 100L);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("greaterTemperatureAlarm");
        alarmRule.setName("greaterTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var tenantAttributeKey = new AttributeArgument("tenantAttribute", NUMERIC, CURRENT_CUSTOMER, 0.0, true);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("tenantAttributeKey");
        lowTempFilter.setOperation(ArgumentOperation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(lowTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "tenantAttributeKey", tenantAttributeKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 150L);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("greaterTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicNoUpdateDurationValue() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(deviceId, "greaterAttribute", 30L);

        long alarmDelayInSeconds = 5L;

        saveAttribute(deviceId, "alarm_delay", alarmDelayInSeconds);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);

        var alarmDelayKey = new AttributeArgument("alarm_delay", NUMERIC, CURRENT_ENTITY, 10, false);

        NoUpdateAlarmConditionSpec durationSpec = new NoUpdateAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setArgumentId("alarmDelayKey");
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "alarmDelayKey", alarmDelayKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        long halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .longValueExact();

        Thread.sleep(halfOfAlarmDelay);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        Thread.sleep(TimeUnit.SECONDS.toMillis(alarmDelayInSeconds) + 1);

        TbMsg msg3 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg3); //no update temperature

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicNoUpdateDurationValueWithCompositeCondition() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName("test device");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        saveAttribute(deviceId, "greaterAttribute", 30L);

        long alarmDelayInSeconds = 5L;

        saveAttribute(deviceId, "alarm_delay", alarmDelayInSeconds);

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var temperatureThreshold = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperatureThreshold", NUMERIC);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");

        SimpleAlarmConditionFilter temperatureThresholdFilter = new SimpleAlarmConditionFilter();
        temperatureThresholdFilter.setLeftArgId("temperatureThreshold");

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(new ComplexAlarmConditionFilter(List.of(highTempFilter, temperatureThresholdFilter), ComplexAlarmConditionFilter.ComplexOperation.AND));

        var alarmDelayKey = new AttributeArgument("alarm_delay", NUMERIC, CURRENT_ENTITY, 10, false);

        NoUpdateAlarmConditionSpec durationSpec = new NoUpdateAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setArgumentId("alarmDelayKey");
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureThreshold", temperatureThreshold, "alarmDelayKey", alarmDelayKey));
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode temperatureData = JacksonUtil.newObjectNode();
        temperatureData.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(temperatureData), null, null);

        alarmRuleStateService.process(ctx, msg);

        long halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .longValueExact();

        Thread.sleep(halfOfAlarmDelay);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ObjectNode temperatureThresholdData = JacksonUtil.newObjectNode();
        temperatureData.put("temperatureThreshold", 100);

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(temperatureThresholdData), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        Thread.sleep(TimeUnit.SECONDS.toMillis(alarmDelayInSeconds) + 1);

        TbMsg msg3 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(temperatureData), null, null);

        alarmRuleStateService.process(ctx, msg3); //no update temperature

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    public void testCreateAndClearAlarmWithTelemetryArguments() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureLeftKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperatureLeft", NUMERIC);
        var temperatureRightKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperatureRight", NUMERIC);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureLeftKey");
        highTempFilter.setRightArgId("temperatureRightKey");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        var lowTemperatureConst = new ConstantArgument(NUMERIC, 10.0);

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureLeftKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(ArgumentOperation.LESS);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setConditionFilter(lowTempFilter);
        clearRule.setAlarmCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);
        alarmRuleConfiguration.setArguments(Map.of("temperatureLeftKey", temperatureLeftKey, "temperatureRightKey", temperatureRightKey, "lowTemperatureConst", lowTemperatureConst));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperatureLeft", 42);
        data.put("temperatureRight", 39);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Updated")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());


        data.put("temperatureLeft", 5);
        TbMsg msg3 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg3);

        Mockito.verify(clusterService, Mockito.timeout(1000)).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Cleared")), any());

        pageData = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.CLEARED_UNACK, alarm.getStatus());
    }

    @Test
    public void testAddNewArgumentsAndUpdateAlarm() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRule = alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        var humidityKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "humidity", NUMERIC);
        var lowHumidityConst = new ConstantArgument(NUMERIC, 55.0);

        alarmRuleConfiguration.setArguments(Map.of(
                "temperatureKey", temperatureKey,
                "highTemperatureConst", highTemperatureConst,
                "humidityKey", humidityKey,
                "lowHumidityConst", lowHumidityConst));

        SimpleAlarmConditionFilter lowHumidityFilter = new SimpleAlarmConditionFilter();
        lowHumidityFilter.setLeftArgId("humidityKey");
        lowHumidityFilter.setRightArgId("lowHumidityConst");
        lowHumidityFilter.setOperation(ArgumentOperation.LESS);

        alarmCondition.setConditionFilter(new ComplexAlarmConditionFilter(List.of(highTempFilter, lowHumidityFilter), ComplexAlarmConditionFilter.ComplexOperation.AND));
        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        AlarmRuleId alarmRuleId = alarmRule.getId();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    Mockito.verify(alarmRuleStateService).onComponentLifecycleEvent(eq(new ComponentLifecycleMsg(tenantId, alarmRuleId, UPDATED)));
                    return true;
                });

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Updated")), any());

        ObjectNode updateData = JacksonUtil.newObjectNode();
        updateData.put("temperature", 42);
        updateData.put("humidity", 54);
        TbMsg updateMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(updateData), null, null);

        alarmRuleStateService.process(ctx, updateMsg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Updated")), any());
    }

    @Test
    public void testRemoveArgumentAndCreateAlarm() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);
        var humidityKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "humidity", NUMERIC);
        var lowHumidityConst = new ConstantArgument(NUMERIC, 55.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);

        SimpleAlarmConditionFilter lowHumidityFilter = new SimpleAlarmConditionFilter();
        lowHumidityFilter.setLeftArgId("humidityKey");
        lowHumidityFilter.setRightArgId("lowHumidityConst");
        lowHumidityFilter.setOperation(ArgumentOperation.LESS);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(new ComplexAlarmConditionFilter(List.of(highTempFilter, lowHumidityFilter), ComplexAlarmConditionFilter.ComplexOperation.AND));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        alarmRuleConfiguration.setArguments(Map.of(
                "temperatureKey", temperatureKey,
                "highTemperatureConst", highTemperatureConst,
                "humidityKey", humidityKey,
                "lowHumidityConst", lowHumidityConst));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRule = alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        alarmRuleConfiguration.setArguments(Map.of(
                "temperatureKey", temperatureKey,
                "highTemperatureConst", highTemperatureConst));
        alarmCondition.setConditionFilter(highTempFilter);
        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        AlarmRuleId alarmRuleId = alarmRule.getId();

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    Mockito.verify(alarmRuleStateService).onComponentLifecycleEvent(eq(new ComponentLifecycleMsg(tenantId, alarmRuleId, UPDATED)));
                    return true;
                });

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());
    }

    @Test
    public void testUpdateDeviceTypeAndNoUpdateAlarm() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        var temperatureKey = new FromMessageArgument(AlarmConditionKeyType.TIME_SERIES, "temperature", NUMERIC);
        var highTemperatureConst = new ConstantArgument(NUMERIC, 30.0);

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(ArgumentOperation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setConditionFilter(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setAlarmCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));
        alarmRuleConfiguration.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(List.of(deviceProfile.getId()));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        DeviceProfile newProfile = createDeviceProfile("new profile");
        newProfile.setTenantId(tenantId);
        newProfile = deviceProfileService.saveDeviceProfile(newProfile);
        device.setDeviceProfileId(newProfile.getId());
        deviceService.saveDevice(device);

        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> {
                    Mockito.verify(alarmRuleStateService).onComponentLifecycleEvent(eq(new ComponentLifecycleMsg(tenantId, deviceId, UPDATED)));
                    return true;
                });

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService, never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Updated")), any());
    }

    private void saveAttribute(EntityId entityId, String key, Boolean value) {
        saveAttribute(entityId, new BooleanDataEntry(key, value));
    }

    private void saveAttribute(EntityId entityId, String key, Long value) {
        saveAttribute(entityId, new LongDataEntry(key, value));
    }

    private void saveAttribute(EntityId entityId, String key, String value) {
        saveAttribute(entityId, new StringDataEntry(key, value));
    }

    private void saveJsonAttribute(EntityId entityId, String key, String value) {
        saveAttribute(entityId, new JsonDataEntry(key, value));
    }

    @SneakyThrows
    private void saveAttribute(EntityId entityId, KvEntry entry) {
        attributesService.save(tenantId, entityId, AttributeScope.SERVER_SCOPE, new BaseAttributeKvEntry(entry, System.currentTimeMillis())).get();
    }
}