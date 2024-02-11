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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbAlarmRuleStateService;
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
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleOriginatorTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleRelationTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleSpecifiedTargetEntity;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmConditionKeyType;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleArgument;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleCondition;
import org.thingsboard.server.common.data.alarm.rule.condition.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.alarm.rule.condition.ArgumentValueType;
import org.thingsboard.server.common.data.alarm.rule.condition.ComplexAlarmConditionFilter;
import org.thingsboard.server.common.data.alarm.rule.condition.CustomTimeSchedule;
import org.thingsboard.server.common.data.alarm.rule.condition.CustomTimeScheduleItem;
import org.thingsboard.server.common.data.alarm.rule.condition.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.NoUpdateAlarmConditionSpec;
import org.thingsboard.server.common.data.alarm.rule.condition.Operation;
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
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
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

@DaoSqlTest
public class DefaultTbAlarmRuleStateServiceTest extends AbstractControllerTest {

    @Autowired
    private AlarmRuleService alarmRuleService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private TbAlarmRuleStateService alarmRuleStateService;

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(Operation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument temperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter tempFilter = new SimpleAlarmConditionFilter();
        tempFilter.setLeftArgId("temperatureKey");
        tempFilter.setRightArgId("temperatureConst");
        tempFilter.setOperation(Operation.GREATER);
        AlarmCondition tempAlarmCondition = new AlarmCondition();
        tempAlarmCondition.setCondition(tempFilter);
        AlarmRuleCondition tempAlarmRuleCondition = new AlarmRuleCondition();
        tempAlarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureConst", temperatureConst));
        tempAlarmRuleCondition.setCondition(tempAlarmCondition);

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(50.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(Operation.GREATER);
        AlarmCondition highTempAlarmCondition = new AlarmCondition();
        highTempAlarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition highTempAlarmRuleCondition = new AlarmRuleCondition();
        highTempAlarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
        highTempAlarmRuleCondition.setCondition(highTempAlarmCondition);

        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Map.of(AlarmSeverity.WARNING, tempAlarmRuleCondition, AlarmSeverity.CRITICAL, highTempAlarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument alarmEnabledConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "alarmEnabled"))
                .valueType(ArgumentValueType.BOOLEAN)
                .defaultValue(Boolean.TRUE)
                .build();
        AlarmRuleArgument alarmEnabledKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "alarmEnabled"))
                .valueType(ArgumentValueType.BOOLEAN)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter alarmEnabledFilter = new SimpleAlarmConditionFilter();
        alarmEnabledFilter.setLeftArgId("alarmEnabledConst");
        alarmEnabledFilter.setRightArgId("alarmEnabledKey");
        alarmEnabledFilter.setOperation(Operation.EQUAL);

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument temperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(20.0)
                .build();

        SimpleAlarmConditionFilter temperatureFilter = new SimpleAlarmConditionFilter();
        temperatureFilter.setLeftArgId("temperatureKey");
        temperatureFilter.setRightArgId("temperatureConst");
        temperatureFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(new ComplexAlarmConditionFilter(Arrays.asList(alarmEnabledFilter, temperatureFilter), ComplexAlarmConditionFilter.ComplexOperation.AND));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of(
                "alarmEnabledConst", alarmEnabledConst,
                "alarmEnabledKey", alarmEnabledKey,
                "temperatureKey", temperatureKey,
                "temperatureConst", temperatureConst));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument alarmEnabledConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "alarmEnabled"))
                .valueType(ArgumentValueType.BOOLEAN)
                .defaultValue(Boolean.TRUE)
                .build();
        AlarmRuleArgument alarmEnabledKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "alarmEnabled"))
                .valueType(ArgumentValueType.BOOLEAN)
                .inherit(true)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter alarmEnabledFilter = new SimpleAlarmConditionFilter();
        alarmEnabledFilter.setLeftArgId("alarmEnabledConst");
        alarmEnabledFilter.setRightArgId("alarmEnabledKey");
        alarmEnabledFilter.setOperation(Operation.EQUAL);

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument temperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(20.0)
                .build();

        SimpleAlarmConditionFilter temperatureFilter = new SimpleAlarmConditionFilter();
        temperatureFilter.setLeftArgId("temperatureKey");
        temperatureFilter.setRightArgId("temperatureConst");
        temperatureFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(new ComplexAlarmConditionFilter(Arrays.asList(alarmEnabledFilter, temperatureFilter), ComplexAlarmConditionFilter.ComplexOperation.AND));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of(
                "alarmEnabledConst", alarmEnabledConst,
                "alarmEnabledKey", alarmEnabledKey,
                "temperatureKey", temperatureKey,
                "temperatureConst", temperatureConst));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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
    @Ignore
    public void testCreateAlarmForSpecifiedEntity() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
        deviceProfile.setTenantId(tenantId);
        deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("temperature sensor");
        device.setDeviceProfileId(deviceProfile.getId());
        device = deviceService.saveDevice(device);

        Device targetEntity = new Device();
        targetEntity.setTenantId(tenantId);
        targetEntity.setName("alarm owner");
        targetEntity.setDeviceProfileId(deviceProfile.getId());
        targetEntity = deviceService.saveDevice(targetEntity);

        DeviceId deviceId = device.getId();

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("temperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleSpecifiedTargetEntity(targetEntity.getId()));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(targetEntity.getId()), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(targetEntity.getId(),
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
    }

    @Test
    @Ignore
    public void testCreateAlarmForRelatedEntities() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfile("test profile");
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

        Asset relatedAsset1 = new Asset();
        relatedAsset1.setTenantId(tenantId);
        relatedAsset1.setName("alarm owner 1");
        relatedAsset1.setAssetProfileId(assetProfile.getId());
        relatedAsset1 = assetService.saveAsset(relatedAsset1);

        AssetId relatedAsset1Id = relatedAsset1.getId();

        Asset relatedAsset2 = new Asset();
        relatedAsset2.setTenantId(tenantId);
        relatedAsset2.setName("alarm owner 2");
        relatedAsset2.setAssetProfileId(assetProfile.getId());
        relatedAsset2 = assetService.saveAsset(relatedAsset2);

        AssetId relatedAsset2Id = relatedAsset2.getId();

        EntityRelation relation1 = new EntityRelation();
        relation1.setFrom(deviceId);
        relation1.setTo(relatedAsset1Id);
        relation1.setType("test type");
        relation1.setTypeGroup(RelationTypeGroup.COMMON);

        EntityRelation relation2 = new EntityRelation();
        relation2.setFrom(deviceId);
        relation2.setTo(relatedAsset2Id);
        relation2.setType("test type");
        relation2.setTypeGroup(RelationTypeGroup.COMMON);

        relationService.saveRelations(tenantId, Arrays.asList(relation1, relation2));

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("temperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleRelationTargetEntity(EntitySearchDirection.TO, "test type"));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(relatedAsset1Id), any(), eq(Collections.singleton("Alarm Created")), any());

        PageData<AlarmInfo> pageData = alarmService.findAlarms(tenantId, new AlarmQuery(relatedAsset1Id,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());


        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(relatedAsset2Id), any(), eq(Collections.singleton("Alarm Created")), any());

        pageData = alarmService.findAlarms(tenantId, new AlarmQuery(relatedAsset2Id,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        alarms = pageData.getData();
        Assert.equals(1, alarms.size());

        alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleSingleEntityFilter sourceFilter = new AlarmRuleSingleEntityFilter(sourceEntity.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msgFromDifferentDevice = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromDifferentDevice);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(sourceDeviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msgFromDifferentDevice = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromDifferentDevice);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleAssetTypeEntityFilter(sourceAssetProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msgFromDifferentDevice = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromDifferentDevice);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleAllDevicesEntityFilter();
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);

        TbMsg msgFromAsset = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), assetId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromAsset);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(assetId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleAllAssetsEntityFilter();
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);

        TbMsg msgFromDevice = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msgFromDevice);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleEntityFilter sourceFilter = new AlarmRuleEntityListEntityFilter(EntityType.DEVICE, Arrays.asList(device1Id, device2Id));
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument highTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(30.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("highTemperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "highTemperatureConst", highTemperatureConst));
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

        AlarmRuleEntityFilter deviceTypeFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        AlarmRuleEntityFilter assetTypeFilter = new AlarmRuleAssetTypeEntityFilter(assetProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Arrays.asList(deviceTypeFilter, assetTypeFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument greaterAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "greaterAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument greaterAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "greaterAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);

        AlarmRuleArgument alarmDelayKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "alarm_delay"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(10)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setArgumentId("alarmDelayKey");
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmDelayKey", alarmDelayKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument greaterAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "greaterAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);

        AlarmRuleArgument alarmDelayKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "alarm_delay"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(10)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .inherit(true)
                .build();

        DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setArgumentId("alarmDelayKey");
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmDelayKey", alarmDelayKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument greaterAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "greaterAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(Operation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);

        AlarmRuleArgument alarmRepeatingKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "alarm_repeating"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(10)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setArgumentId("alarmRepeatingKey");
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmRepeatingKey", alarmRepeatingKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument greaterAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "greaterAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(Operation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);

        AlarmRuleArgument alarmRepeatingKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "alarm_repeating"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(10)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .inherit(true)
                .build();

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setArgumentId("alarmRepeatingKey");
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmRepeatingKey", alarmRepeatingKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument greaterAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "greaterAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);

        AlarmRuleArgument alarmDelayKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, null))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(alarmDelayInSeconds)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setArgumentId("alarmDelayKey");
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmDelayKey", alarmDelayKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument greaterAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "greaterAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("greaterAttributeKey");
        highTempFilter.setOperation(Operation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);

        AlarmRuleArgument alarmRepeatingKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, null))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(2)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .inherit(true)
                .build();

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setArgumentId("alarmRepeatingKey");
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "greaterAttributeKey", greaterAttributeKey, "alarmRepeatingKey", alarmRepeatingKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        saveJsonAttribute(deviceId, "dynamicValueActiveSchedule", "{\"timezone\":\"Europe/Kiev\",\"items\":[{\"enabled\":true,\"dayOfWeek\":1,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":2,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":3,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":4,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":5,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":6,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":7,\"startsOn\":0,\"endsOn\":8.64e+7}],\"dynamicValue\":null}"
        );

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();
        AlarmRuleArgument temperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("temperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);

        AlarmRuleArgument scheduleKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "dynamicValueActiveSchedule"))
                .valueType(ArgumentValueType.NUMERIC)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        CustomTimeSchedule schedule = new CustomTimeSchedule();
        schedule.setItems(Collections.emptyList());
        schedule.setArgumentId("scheduleKey");

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureConst", temperatureConst, "scheduleKey", scheduleKey));
        alarmRuleCondition.setCondition(alarmCondition);
        alarmRuleCondition.setSchedule(schedule);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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
                "{\"timezone\":\"Europe/Kiev\",\"items\":[{\"enabled\":false,\"dayOfWeek\":1,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":2,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":3,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":4,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":5,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":6,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":7,\"startsOn\":0,\"endsOn\":0}],\"dynamicValue\":null}"
        );

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();
        AlarmRuleArgument temperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");
        highTempFilter.setRightArgId("temperatureConst");
        highTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);

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

        AlarmRuleArgument scheduleKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "dynamicValueInactiveSchedule"))
                .valueType(ArgumentValueType.NUMERIC)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        schedule.setItems(items);
        schedule.setArgumentId("scheduleKey");

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "temperatureConst", temperatureConst, "scheduleKey", scheduleKey));
        alarmRuleCondition.setCondition(alarmCondition);
        alarmRuleCondition.setSchedule(schedule);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();
        AlarmRuleArgument lessAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "lessAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(20.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_CUSTOMER)
                .inherit(true)
                .build();

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lessAttributeKey");
        lowTempFilter.setOperation(Operation.LESS);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(lowTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "lessAttributeKey", lessAttributeKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();
        AlarmRuleArgument lessAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "lessAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(32.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_TENANT)
                .inherit(true)
                .build();

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("lessAttributeKey");
        lowTempFilter.setOperation(Operation.LESS);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(lowTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "lessAttributeKey", lessAttributeKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();
        AlarmRuleArgument tenantAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "tenantAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .inherit(true)
                .build();

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("tenantAttributeKey");
        lowTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(lowTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "tenantAttributeKey", tenantAttributeKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();
        AlarmRuleArgument tenantAttributeKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "tenantAttribute"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(0.0)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_CUSTOMER)
                .inherit(true)
                .build();

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureKey");
        lowTempFilter.setRightArgId("tenantAttributeKey");
        lowTempFilter.setOperation(Operation.GREATER);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(lowTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "tenantAttributeKey", tenantAttributeKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        AlarmRuleArgument temperatureKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureKey");

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);

        AlarmRuleArgument alarmDelayKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "alarm_delay"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(10)
                .sourceType(AlarmRuleArgument.ValueSourceType.CURRENT_ENTITY)
                .build();

        NoUpdateAlarmConditionSpec durationSpec = new NoUpdateAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setArgumentId("alarmDelayKey");
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureKey", temperatureKey, "alarmDelayKey", alarmDelayKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

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

        AlarmRuleArgument temperatureLeftKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperatureLeft"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        AlarmRuleArgument temperatureRightKey = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperatureRight"))
                .valueType(ArgumentValueType.NUMERIC)
                .build();

        SimpleAlarmConditionFilter highTempFilter = new SimpleAlarmConditionFilter();
        highTempFilter.setLeftArgId("temperatureLeftKey");
        highTempFilter.setRightArgId("temperatureRightKey");
        highTempFilter.setOperation(Operation.GREATER);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(highTempFilter);
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setArguments(Map.of("temperatureLeftKey", temperatureLeftKey, "temperatureRightKey", temperatureRightKey));
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleArgument lowTemperatureConst = AlarmRuleArgument.builder()
                .key(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "temperature"))
                .valueType(ArgumentValueType.NUMERIC)
                .defaultValue(10.0)
                .build();

        SimpleAlarmConditionFilter lowTempFilter = new SimpleAlarmConditionFilter();
        lowTempFilter.setLeftArgId("temperatureLeftKey");
        lowTempFilter.setRightArgId("lowTemperatureConst");
        lowTempFilter.setOperation(Operation.LESS);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearRule.setArguments(Map.of("temperatureLeftKey", temperatureLeftKey, "lowTemperatureConst", lowTemperatureConst));
        clearCondition.setCondition(lowTempFilter);
        clearRule.setCondition(clearCondition);
        alarmRuleConfiguration.setClearRule(clearRule);

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

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