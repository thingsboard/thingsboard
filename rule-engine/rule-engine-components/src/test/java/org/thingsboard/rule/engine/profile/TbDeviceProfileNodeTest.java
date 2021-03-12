/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.rule.engine.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbDeviceProfileNodeTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbDeviceProfileNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private RuleEngineDeviceProfileCache cache;
    @Mock
    private TimeseriesService timeseriesService;
    @Mock
    private RuleEngineAlarmService alarmService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private AttributesService attributesService;

    private TenantId tenantId = new TenantId(UUID.randomUUID());
    private DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private CustomerId customerId = new CustomerId(UUID.randomUUID());
    private DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());

    @Test
    public void testRandomMessageType() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setAlarms(Collections.emptyList());
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg("123456789", deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testEmptyProfile() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setAlarms(Collections.emptyList());
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testAlarmCreate() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(30.0));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTemperaturePredicate = new NumericFilterPredicate();
        lowTemperaturePredicate.setOperation(NumericFilterPredicate.NumericOperation.LESS);
        lowTemperaturePredicate.setValue(new FilterPredicateValue<>(10.0));
        lowTempFilter.setPredicate(lowTemperaturePredicate);
        AlarmRule clearRule = new AlarmRule();
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(Collections.singletonList(lowTempFilter));
        clearRule.setCondition(clearCondition);
        dpa.setClearRule(clearRule);

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm")).thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());

        TbMsg theMsg2 = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "2");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(theMsg2);


        TbMsg msg2 = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);
        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).tellNext(theMsg2, "Alarm Updated");

    }

    @Test
    public void testConstantKeyFilterSimple() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "alarmEnabled"
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setBooleanValue(Boolean.TRUE);
        attributeKvEntity.setLastUpdateTs(System.currentTimeMillis());

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> attrListListenableFuture = Futures.immediateFuture(Collections.singletonList(entry));

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
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("alarmEnabledAlarmID");
        dpa.setAlarmType("alarmEnabledAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "alarmEnabledAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(attrListListenableFuture);

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 21);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testConstantKeyFilterInherited() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, tenantId.getId(), "SERVER_SCOPE", "alarmEnabled"
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setBooleanValue(Boolean.TRUE);
        attributeKvEntity.setLastUpdateTs(System.currentTimeMillis());

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<Optional<AttributeKvEntry>> attrListListenableFuture = Futures.immediateFuture(Optional.of(entry));

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
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("alarmEnabledAlarmID");
        dpa.setAlarmType("alarmEnabledAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);
        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "alarmEnabledAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Futures.immediateFuture(Optional.empty()));
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(attrListListenableFuture);

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 21);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicValue() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "greaterAttribute"
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.singletonList(entry));

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
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentCustomersAttributeForDynamicValue() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "lessAttribute"
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.singletonList(entry));
        ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

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
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("lesstempID");
        dpa.setAlarmType("lessTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), eq(DataConstants.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 25);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentTenantAttributeForDynamicValue() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "lessAttribute"
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(50L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.singletonList(entry));
        ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

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
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("lesstempID");
        dpa.setAlarmType("lessTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any()))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), eq(DataConstants.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 40);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testTenantInheritModeForDynamicValues() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "tenantAttribute"
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(100L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.singletonList(entry));

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
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("lesstempID");
        dpa.setAlarmType("lessTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any()))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 150L);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }


    @Test
    public void testCustomerInheritModeForDynamicValues() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                EntityType.TENANT, deviceId.getId(), "SERVER_SCOPE", "customerAttribute"
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setLongValue(100L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.singletonList(entry));
        ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        0.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_CUSTOMER, "customerAttribute", true))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("lesstempID");
        dpa.setAlarmType("lessTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(Futures.immediateFuture(null));
        Mockito.when(alarmService.createOrUpdateAlarm(Mockito.any()))
                .thenAnswer(AdditionalAnswers.returnsFirstArg());
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.anyString(), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), eq(DataConstants.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg("ALARM", deviceId, new TbMsgMetaData(), "");
        Mockito.when(ctx.newMsg(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = mapper.createObjectNode();
        data.put("temperature", 150L);
        TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                TbMsgDataType.JSON, mapper.writeValueAsString(data), null, null);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).tellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    private void init() throws TbNodeException {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getDeviceProfileCache()).thenReturn(cache);
        Mockito.when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
        Mockito.when(ctx.getAlarmService()).thenReturn(alarmService);
        Mockito.when(ctx.getDeviceService()).thenReturn(deviceService);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.createObjectNode());
        node = new TbDeviceProfileNode();
        node.init(ctx, nodeConfiguration);
    }

}
