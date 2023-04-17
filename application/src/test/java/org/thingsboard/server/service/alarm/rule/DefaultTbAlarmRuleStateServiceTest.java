/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import com.google.common.util.concurrent.ListenableFuture;
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
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAllAssetsEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAllDevicesEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleAssetTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleDeviceTypeEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleEntityListEntityFilter;
import org.thingsboard.server.common.data.alarm.rule.filter.AlarmRuleSingleEntityFilter;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRuleCondition;
import org.thingsboard.server.common.data.device.profile.AlarmRuleConfiguration;
import org.thingsboard.server.common.data.device.profile.CustomTimeSchedule;
import org.thingsboard.server.common.data.device.profile.CustomTimeScheduleItem;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.RepeatingAlarmConditionSpec;
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
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Updated")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());


        data.put("temperature", 5);
        TbMsg msg3 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg3);

        Mockito.verify(clusterService, Mockito.timeout(1000)).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Cleared")), any());

        future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        alarms = future.get().getData();
        Assert.equals(1, alarms.size());

        alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.CLEARED_UNACK, alarm.getStatus());
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

        AlarmConditionFilter alarmEnabledFilter = new AlarmConditionFilter();
        alarmEnabledFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "alarmEnabled"));
        alarmEnabledFilter.setValue(Boolean.TRUE);
        alarmEnabledFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate alarmEnabledPredicate = new BooleanFilterPredicate();
        alarmEnabledPredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        alarmEnabledPredicate.setValue(new FilterPredicateValue<>(
                Boolean.FALSE,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarmEnabled")
        ));
        alarmEnabledFilter.setPredicate(alarmEnabledPredicate);

        AlarmConditionFilter temperatureFilter = new AlarmConditionFilter();
        temperatureFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        temperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate temperaturePredicate = new NumericFilterPredicate();
        temperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        temperaturePredicate.setValue(new FilterPredicateValue<>(20.0, null, null));
        temperatureFilter.setPredicate(temperaturePredicate);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Arrays.asList(alarmEnabledFilter, temperatureFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 21);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter alarmEnabledFilter = new AlarmConditionFilter();
        alarmEnabledFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.CONSTANT, "alarmEnabled"));
        alarmEnabledFilter.setValue(Boolean.TRUE);
        alarmEnabledFilter.setValueType(EntityKeyValueType.BOOLEAN);
        BooleanFilterPredicate alarmEnabledPredicate = new BooleanFilterPredicate();
        alarmEnabledPredicate.setOperation(BooleanFilterPredicate.BooleanOperation.EQUAL);
        alarmEnabledPredicate.setValue(new FilterPredicateValue<>(
                Boolean.FALSE,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarmEnabled", true)
        ));
        alarmEnabledFilter.setPredicate(alarmEnabledPredicate);

        AlarmConditionFilter temperatureFilter = new AlarmConditionFilter();
        temperatureFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        temperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate temperaturePredicate = new NumericFilterPredicate();
        temperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        temperaturePredicate.setValue(new FilterPredicateValue<>(20.0, null, null));
        temperatureFilter.setPredicate(temperaturePredicate);

        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Arrays.asList(alarmEnabledFilter, temperatureFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 21);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(targetEntity.getId(),
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(relatedAsset1Id,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());


        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(relatedAsset2Id), any(), eq(Collections.singleton("Alarm Created")), any());

        future = alarmService.findAlarms(tenantId, new AlarmQuery(relatedAsset2Id,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(sourceEntity.getId(),
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(sourceEntity.getId(),
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(sourceEntity.getId(),
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(assetId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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

        ListenableFuture<PageData<AlarmInfo>> deviceAlarmFuture = alarmService.findAlarms(tenantId, new AlarmQuery(device1Id,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> deviceAlarms = deviceAlarmFuture.get().getData();
        Assert.equals(1, deviceAlarms.size());

        AlarmInfo deviceAlarm = deviceAlarms.get(0);
        Assert.equals("highTemperatureAlarm", deviceAlarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, deviceAlarm.getStatus());

        TbMsg assetMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), device2Id, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, assetMsg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(device2Id), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> assetFuture = alarmService.findAlarms(tenantId, new AlarmQuery(device2Id,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> assetAlarms = assetFuture.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRuleCondition clearRule = new AlarmRuleCondition();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
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

        ListenableFuture<PageData<AlarmInfo>> deviceAlarmFuture = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> deviceAlarms = deviceAlarmFuture.get().getData();
        Assert.equals(1, deviceAlarms.size());

        AlarmInfo deviceAlarm = deviceAlarms.get(0);
        Assert.equals("highTemperatureAlarm", deviceAlarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, deviceAlarm.getStatus());

        TbMsg assetMsg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), assetId, new TbMsgMetaData(),
                TbMsgDataType.JSON, JacksonUtil.toString(data), null, null);

        alarmRuleStateService.process(ctx, assetMsg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(assetId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> assetFuture = alarmService.findAlarms(tenantId, new AlarmQuery(assetId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> assetAlarms = assetFuture.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute")
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        FilterPredicateValue<Long> filterPredicateValue = new FilterPredicateValue<>(
                10L,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_delay", false)
        );

        DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        long halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .longValueExact();


        Thread.sleep(halfOfAlarmDelay);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        Thread.sleep(halfOfAlarmDelay);

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        FilterPredicateValue<Long> filterPredicateValue = new FilterPredicateValue<>(
                10L,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_delay", true)
        );

        DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        long halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .longValueExact();


        Thread.sleep(halfOfAlarmDelay);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        Thread.sleep(halfOfAlarmDelay);

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        FilterPredicateValue<Integer> filterPredicateValue = new FilterPredicateValue<Integer>(
                10,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_repeating", false)
        );

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        FilterPredicateValue<Integer> filterPredicateValue = new FilterPredicateValue<Integer>(
                10,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_repeating", true)
        );

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        FilterPredicateValue<Long> filterPredicateValue = new FilterPredicateValue<>(
                alarmDelayInSeconds,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, null, false)
        );

        DurationAlarmConditionSpec durationSpec = new DurationAlarmConditionSpec();
        durationSpec.setUnit(TimeUnit.SECONDS);
        durationSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(durationSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        long halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .longValueExact();


        Thread.sleep(halfOfAlarmDelay);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        Thread.sleep(halfOfAlarmDelay);

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        FilterPredicateValue<Integer> filterPredicateValue = new FilterPredicateValue<Integer>(
                2,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, null, true)
        );

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(repeatingSpec);

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService, Mockito.never()).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg2);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                null
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        CustomTimeSchedule schedule = new CustomTimeSchedule();
        schedule.setItems(Collections.emptyList());
        schedule.setDynamicValue(new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "dynamicValueActiveSchedule", false));

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        alarmRuleCondition.setSchedule(schedule);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        saveJsonAttribute(deviceId, "dynamicValueActiveSchedule",
                "{\"timezone\":\"Europe/Kiev\",\"items\":[{\"enabled\":false,\"dayOfWeek\":1,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":2,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":3,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":4,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":5,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":6,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":7,\"startsOn\":0,\"endsOn\":0}],\"dynamicValue\":null}"
        );

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setTenantId(tenantId);
        alarmRule.setAlarmType("highTemperatureAlarm");
        alarmRule.setName("highTemperatureAlarmRule");
        alarmRule.setEnabled(true);

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                null
        ));

        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

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

        schedule.setItems(items);
        schedule.setDynamicValue(new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "dynamicValueInactiveSchedule", false));

        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        alarmRuleCondition.setSchedule(schedule);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("highTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
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

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        20.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_CUSTOMER, "lessAttribute"))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 25);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        32.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_TENANT, "lessAttribute"))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 40);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        0.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "tenantAttribute", true))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
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

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 150L);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
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

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        0.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_CUSTOMER, "tenantAttribute", true))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        AlarmRuleCondition alarmRuleCondition = new AlarmRuleCondition();
        alarmRuleCondition.setCondition(alarmCondition);
        AlarmRuleConfiguration alarmRuleConfiguration = new AlarmRuleConfiguration();
        alarmRuleConfiguration.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRuleCondition)));

        AlarmRuleDeviceTypeEntityFilter sourceFilter = new AlarmRuleDeviceTypeEntityFilter(deviceProfile.getId());
        alarmRuleConfiguration.setSourceEntityFilters(Collections.singletonList(sourceFilter));
        alarmRuleConfiguration.setAlarmTargetEntity(new AlarmRuleOriginatorTargetEntity());

        alarmRule.setConfiguration(alarmRuleConfiguration);

        alarmRuleService.saveAlarmRule(tenantId, alarmRule);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 150L);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        alarmRuleStateService.process(ctx, msg);

        Mockito.verify(clusterService).pushMsgToRuleEngine(eq(tenantId), eq(deviceId), any(), eq(Collections.singleton("Alarm Created")), any());

        ListenableFuture<PageData<AlarmInfo>> future = alarmService.findAlarms(tenantId, new AlarmQuery(deviceId,
                new TimePageLink(10), AlarmSearchStatus.ANY, null, null, true));

        List<AlarmInfo> alarms = future.get().getData();
        Assert.equals(1, alarms.size());

        AlarmInfo alarm = alarms.get(0);
        Assert.equals("greaterTemperatureAlarm", alarm.getName());
        Assert.equals(AlarmStatus.ACTIVE_UNACK, alarm.getStatus());
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
        attributesService.save(tenantId, entityId, "SERVER_SCOPE", new BaseAttributeKvEntry(entry, System.currentTimeMillis())).get();
    }

}