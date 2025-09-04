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
package org.thingsboard.rule.engine.profile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.rule.engine.api.RuleEngineDeviceProfileCache;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmModificationRequest;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.CustomTimeSchedule;
import org.thingsboard.server.common.data.device.profile.CustomTimeScheduleItem;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DurationAlarmConditionSpec;
import org.thingsboard.server.common.data.device.profile.RepeatingAlarmConditionSpec;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntryAggWrapper;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.NumericFilterPredicate.NumericOperation;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType.ATTRIBUTE;
import static org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType.TIME_SERIES;
import static org.thingsboard.server.common.data.query.NumericFilterPredicate.NumericOperation.GREATER;
import static org.thingsboard.server.common.data.query.NumericFilterPredicate.NumericOperation.LESS;

@ExtendWith(MockitoExtension.class)
public class TbDeviceProfileNodeTest extends AbstractRuleNodeUpgradeTest {

    @Spy
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

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private final CustomerId customerId = new CustomerId(UUID.randomUUID());
    private final DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());

    @Test
    public void testRandomMessageType() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setAlarms(Collections.emptyList());
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg()
                .type("123456789")
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();
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
        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testAlarmCreate() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AlarmCondition alarmCreateCondition = getNumericAlarmCondition(TIME_SERIES, "temperature", GREATER, 30.0);
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCreateCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        AlarmRule clearRule = new AlarmRule();
        AlarmCondition clearCondition = getNumericAlarmCondition(TIME_SERIES, "temperature", LESS, 10.0);
        clearRule.setCondition(clearCondition);
        dpa.setClearRule(clearRule);

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm")).thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString())).thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());

        TbMsg theMsg2 = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data("2")
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString())).thenReturn(theMsg2);

        registerCreateAlarmMock(alarmService.updateAlarm(any()), false);

        Thread.sleep(1);
        TbMsg msg2 = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();
        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg2, "Alarm Updated");

    }

    @Test
    public void testAlarmSeverityUpdate() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AlarmCondition alarmTempCondition = getNumericAlarmCondition(TIME_SERIES, "temperature", GREATER, 30.0);
        AlarmRule alarmTempRule = new AlarmRule();
        alarmTempRule.setCondition(alarmTempCondition);

        AlarmConditionFilter highTempFilter = getAlarmConditionFilter(TIME_SERIES, "temperature", GREATER, 50.0);
        AlarmCondition alarmHighTempCondition = new AlarmCondition();
        alarmHighTempCondition.setCondition(Collections.singletonList(highTempFilter));
        AlarmRule alarmHighTempRule = new AlarmRule();
        alarmHighTempRule.setCondition(alarmHighTempCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID1");
        dpa.setAlarmType("highTemperatureAlarm1");

        TreeMap<AlarmSeverity, AlarmRule> createRules = new TreeMap<>();

        createRules.put(AlarmSeverity.WARNING, alarmTempRule);
        createRules.put(AlarmSeverity.CRITICAL, alarmHighTempRule);

        dpa.setCreateRules(createRules);

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm1")).thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString())).thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());

        TbMsg theMsg2 = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString())).thenReturn(theMsg2);

        AlarmInfo alarm = new AlarmInfo(new Alarm(new AlarmId(UUID.randomUUID())));
        alarm.setSeverity(AlarmSeverity.CRITICAL);

        Alarm oldAlarm = new Alarm(new AlarmId(UUID.randomUUID()));
        oldAlarm.setSeverity(AlarmSeverity.WARNING);
        var result = AlarmApiCallResult.builder()
                .successful(true)
                .created(false)
                .modified(true)
                .alarm(alarm)
                .old(oldAlarm)
                .build();

        when(alarmService.updateAlarm(any())).thenReturn(result);

        data.put("temperature", 52);
        TbMsg msg2 = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();
        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg2, "Alarm Severity Updated");
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
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setBooleanValue(Boolean.TRUE);
        attributeKvEntity.setStrKey("alarmEnabled");
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
        temperatureFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        temperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate temperaturePredicate = new NumericFilterPredicate();
        temperaturePredicate.setOperation(GREATER);
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "alarmEnabledAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(attrListListenableFuture);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        Mockito.when(ctx.newMsg(Mockito.any(), Mockito.any(TbMsgType.class), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 21);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
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
                tenantId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("alarmEnabled");
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
        temperatureFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        temperatureFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate temperaturePredicate = new NumericFilterPredicate();
        temperaturePredicate.setOperation(GREATER);
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "alarmEnabledAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), Mockito.any(AttributeScope.class), Mockito.anyString()))
                .thenReturn(Futures.immediateFuture(Optional.empty()));
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), Mockito.any(AttributeScope.class), Mockito.anyString()))
                .thenReturn(attrListListenableFuture);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 21);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicValue() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("greaterAttribute");
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.singletonList(entry));

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(GREATER);
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicDurationValue() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("greaterAttribute");
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvCompositeKey alarmDelayCompositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 11
        );

        AttributeKvEntity alarmDelayAttributeKvEntity = new AttributeKvEntity();
        alarmDelayAttributeKvEntity.setId(alarmDelayCompositeKey);
        alarmDelayAttributeKvEntity.setStrKey("alarm_delay");
        long alarmDelayInSeconds = 5L;
        alarmDelayAttributeKvEntity.setLongValue(alarmDelayInSeconds);
        alarmDelayAttributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();

        AttributeKvEntry alarmDelayAttributeKvEntry = alarmDelayAttributeKvEntity.toData();

        ListenableFuture<List<AttributeKvEntry>> listListenableFuture =
                Futures.immediateFuture(Arrays.asList(entry, alarmDelayAttributeKvEntry));

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(GREATER);
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFuture);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        int halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .intValueExact();
        Thread.sleep(halfOfAlarmDelay);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        Thread.sleep(halfOfAlarmDelay + 1);

        TbMsg msg2 = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testInheritTenantAttributeForDuration() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);


        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("greaterAttribute");
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvCompositeKey alarmDelayCompositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 11
        );

        AttributeKvEntity alarmDelayAttributeKvEntity = new AttributeKvEntity();
        alarmDelayAttributeKvEntity.setId(alarmDelayCompositeKey);
        alarmDelayAttributeKvEntity.setStrKey("alarm_delay");
        long alarmDelayInSeconds = 5L;
        alarmDelayAttributeKvEntity.setLongValue(alarmDelayInSeconds);
        alarmDelayAttributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();

        AttributeKvEntry alarmDelayAttributeKvEntry = alarmDelayAttributeKvEntity.toData();

        ListenableFuture<Optional<AttributeKvEntry>> optionalDurationAttribute =
                Futures.immediateFuture(Optional.of(alarmDelayAttributeKvEntry));
        ListenableFuture<List<AttributeKvEntry>> listNoDurationAttribute =
                Futures.immediateFuture(Collections.singletonList(entry));
        ListenableFuture<Optional<AttributeKvEntry>> emptyOptional =
                Futures.immediateFuture(Optional.empty());

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(GREATER);
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), Mockito.any(AttributeScope.class), Mockito.anyString()))
                .thenReturn(optionalDurationAttribute);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), eq(AttributeScope.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(emptyOptional);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listNoDurationAttribute);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 150);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        int halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .intValueExact();
        Thread.sleep(halfOfAlarmDelay);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        Thread.sleep(halfOfAlarmDelay + 1);

        TbMsg msg2 = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForDynamicRepeatingValue() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("greaterAttribute");
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvCompositeKey alarmDelayCompositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 11
        );

        AttributeKvEntity alarmDelayAttributeKvEntity = new AttributeKvEntity();
        alarmDelayAttributeKvEntity.setId(alarmDelayCompositeKey);
        alarmDelayAttributeKvEntity.setStrKey("alarm_delay");
        long alarmRepeating = 2;
        alarmDelayAttributeKvEntity.setLongValue(alarmRepeating);
        alarmDelayAttributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();

        AttributeKvEntry alarmDelayAttributeKvEntry = alarmDelayAttributeKvEntity.toData();

        ListenableFuture<List<AttributeKvEntry>> listListenableFuture =
                Futures.immediateFuture(Arrays.asList(entry, alarmDelayAttributeKvEntry));

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        FilterPredicateValue<Integer> filterPredicateValue = new FilterPredicateValue<>(
                10,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_delay", false)
        );


        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(repeatingSpec);

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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFuture);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 150);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        data.put("temperature", 151);
        TbMsg msg2 = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testInheritTenantAttributeForRepeating() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("greaterAttribute");
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvCompositeKey alarmDelayCompositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 11
        );

        AttributeKvEntity alarmDelayAttributeKvEntity = new AttributeKvEntity();
        alarmDelayAttributeKvEntity.setId(alarmDelayCompositeKey);
        alarmDelayAttributeKvEntity.setStrKey("alarm_delay");
        long repeatingCondition = 2;
        alarmDelayAttributeKvEntity.setLongValue(repeatingCondition);
        alarmDelayAttributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();

        AttributeKvEntry alarmDelayAttributeKvEntry = alarmDelayAttributeKvEntity.toData();

        ListenableFuture<Optional<AttributeKvEntry>> optionalDurationAttribute =
                Futures.immediateFuture(Optional.of(alarmDelayAttributeKvEntry));
        ListenableFuture<List<AttributeKvEntry>> listNoDurationAttribute =
                Futures.immediateFuture(Collections.singletonList(entry));
        ListenableFuture<Optional<AttributeKvEntry>> emptyOptional =
                Futures.immediateFuture(Optional.empty());

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute", false)
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        FilterPredicateValue<Integer> filterPredicateValue = new FilterPredicateValue<>(
                10,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_delay", true)
        );

        RepeatingAlarmConditionSpec repeatingSpec = new RepeatingAlarmConditionSpec();
        repeatingSpec.setPredicate(filterPredicateValue);
        alarmCondition.setSpec(repeatingSpec);

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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), Mockito.any(AttributeScope.class), Mockito.anyString()))
                .thenReturn(optionalDurationAttribute);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), eq(AttributeScope.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(emptyOptional);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listNoDurationAttribute);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 150);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        data.put("temperature", 151);
        TbMsg msg2 = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForUseDefaultDurationWhenDynamicDurationValueIsNull() throws Exception {
        init();

        long alarmDelayInSeconds = 5;
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("greaterAttribute");
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();

        ListenableFuture<List<AttributeKvEntry>> listListenableFuture =
                Futures.immediateFuture(Collections.singletonList(entry));

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute")
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFuture);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        int halfOfAlarmDelay = new BigDecimal(alarmDelayInSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(2), 3, RoundingMode.HALF_EVEN)
                .intValueExact();
        Thread.sleep(halfOfAlarmDelay);

        verify(ctx, Mockito.never()).tellNext(theMsg, "Alarm Created");

        Thread.sleep(halfOfAlarmDelay + 1);

        TbMsg msg2 = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg2);
        verify(ctx).tellSuccess(msg2);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentDeviceAttributeForUseDefaultRepeatingWhenDynamicDurationValueIsNull() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("greaterAttribute");
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();

        ListenableFuture<List<AttributeKvEntry>> listListenableFuture =
                Futures.immediateFuture(Collections.singletonList(entry));

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(GREATER);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(
                0.0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "greaterAttribute")
        ));
        highTempFilter.setPredicate(highTemperaturePredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(highTempFilter));

        RepeatingAlarmConditionSpec repeating = new RepeatingAlarmConditionSpec();
        repeating.setPredicate(new FilterPredicateValue<>(
                0,
                null,
                new DynamicValue<>(DynamicValueSourceType.CURRENT_DEVICE, "alarm_rule", false)
        ));
        alarmCondition.setSpec(repeating);

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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFuture);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testActiveAlarmScheduleFromDynamicValuesWhenDefaultScheduleIsInactive() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvCompositeKey compositeKeyActiveSchedule = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntityActiveSchedule = new AttributeKvEntity();
        attributeKvEntityActiveSchedule.setId(compositeKeyActiveSchedule);
        attributeKvEntityActiveSchedule.setStrKey("dynamicValueActiveSchedule");
        attributeKvEntityActiveSchedule.setJsonValue(
                "{\"timezone\":\"Europe/Kiev\",\"items\":[{\"enabled\":true,\"dayOfWeek\":1,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":2,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":3,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":4,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":5,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":6,\"startsOn\":0,\"endsOn\":8.64e+7},{\"enabled\":true,\"dayOfWeek\":7,\"startsOn\":0,\"endsOn\":8.64e+7}],\"dynamicValue\":null}"
        );
        attributeKvEntityActiveSchedule.setLastUpdateTs(0L);

        AttributeKvEntry entryActiveSchedule = attributeKvEntityActiveSchedule.toData();

        ListenableFuture<List<AttributeKvEntry>> listListenableFutureActiveSchedule =
                Futures.immediateFuture(Collections.singletonList(entryActiveSchedule));

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(GREATER);
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

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        alarmRule.setSchedule(schedule);
        DeviceProfileAlarm deviceProfileAlarmActiveSchedule = new DeviceProfileAlarm();
        deviceProfileAlarmActiveSchedule.setId("highTemperatureAlarmID");
        deviceProfileAlarmActiveSchedule.setAlarmType("highTemperatureAlarm");
        deviceProfileAlarmActiveSchedule.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(deviceProfileAlarmActiveSchedule));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFutureActiveSchedule);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))

                .build();

//        Mockito.reset(ctx);

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testInactiveAlarmScheduleFromDynamicValuesWhenDefaultScheduleIsActive() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(deviceProfileId);
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvCompositeKey compositeKeyInactiveSchedule = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntityInactiveSchedule = new AttributeKvEntity();
        attributeKvEntityInactiveSchedule.setId(compositeKeyInactiveSchedule);
        attributeKvEntityInactiveSchedule.setStrKey("dynamicValueInactiveSchedule");
        attributeKvEntityInactiveSchedule.setJsonValue(
                "{\"timezone\":\"Europe/Kiev\",\"items\":[{\"enabled\":false,\"dayOfWeek\":1,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":2,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":3,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":4,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":5,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":6,\"startsOn\":0,\"endsOn\":0},{\"enabled\":false,\"dayOfWeek\":7,\"startsOn\":0,\"endsOn\":0}],\"dynamicValue\":null}"
        );

        attributeKvEntityInactiveSchedule.setLastUpdateTs(0L);

        AttributeKvEntry entryInactiveSchedule = attributeKvEntityInactiveSchedule.toData();

        ListenableFuture<List<AttributeKvEntry>> listListenableFutureInactiveSchedule =
                Futures.immediateFuture(Collections.singletonList(entryInactiveSchedule));

        AlarmConditionFilter highTempFilter = new AlarmConditionFilter();
        highTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        highTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(GREATER);
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

        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        alarmRule.setSchedule(schedule);
        DeviceProfileAlarm deviceProfileAlarmNonactiveSchedule = new DeviceProfileAlarm();
        deviceProfileAlarmNonactiveSchedule.setId("highTemperatureAlarmID");
        deviceProfileAlarmNonactiveSchedule.setAlarmType("highTemperatureAlarm");
        deviceProfileAlarmNonactiveSchedule.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(deviceProfileAlarmNonactiveSchedule));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm"))
                .thenReturn(null);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFutureInactiveSchedule);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 35);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx, Mockito.never()).enqueueForTellNext(theMsg, "Alarm Created");
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
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("lessAttribute");
        attributeKvEntity.setLongValue(30L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.emptyList());
        ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(LESS);
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), eq(AttributeScope.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 25);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testCurrentTenantAttributeForDynamicValue() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("lessAttribute");
        attributeKvEntity.setLongValue(50L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.emptyList());
        ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(LESS);
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 40);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Test
    public void testTenantInheritModeForDynamicValues() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("tenantAttribute");
        attributeKvEntity.setLongValue(100L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.emptyList());
        ListenableFuture<Optional<AttributeKvEntry>> emptyOptionalFuture =
                Futures.immediateFuture(Optional.empty());
        ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(GREATER);
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "lessTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), Mockito.any(AttributeScope.class), Mockito.anyString()))
                .thenReturn(emptyOptionalFuture);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 150L);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());

    }


    @Test
    public void testCustomerInheritModeForDynamicValues() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AttributeKvCompositeKey compositeKey = new AttributeKvCompositeKey(
                deviceId.getId(), AttributeScope.SERVER_SCOPE.getId(), 10
        );

        Device device = new Device();
        device.setId(deviceId);
        device.setCustomerId(customerId);

        AttributeKvEntity attributeKvEntity = new AttributeKvEntity();
        attributeKvEntity.setId(compositeKey);
        attributeKvEntity.setStrKey("tenantAttribute");
        attributeKvEntity.setLongValue(100L);
        attributeKvEntity.setLastUpdateTs(0L);

        AttributeKvEntry entry = attributeKvEntity.toData();
        ListenableFuture<List<AttributeKvEntry>> listListenableFutureWithLess =
                Futures.immediateFuture(Collections.emptyList());
        ListenableFuture<Optional<AttributeKvEntry>> emptyOptionalFuture =
                Futures.immediateFuture(Optional.empty());
        ListenableFuture<Optional<AttributeKvEntry>> optionalListenableFutureWithLess =
                Futures.immediateFuture(Optional.of(entry));

        AlarmConditionFilter lowTempFilter = new AlarmConditionFilter();
        lowTempFilter.setKey(new AlarmConditionFilterKey(TIME_SERIES, "temperature"));
        lowTempFilter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate lowTempPredicate = new NumericFilterPredicate();
        lowTempPredicate.setOperation(GREATER);
        lowTempPredicate.setValue(
                new FilterPredicateValue<>(
                        0.0,
                        null,
                        new DynamicValue<>(DynamicValueSourceType.CURRENT_CUSTOMER, "tenantAttribute", true))
        );
        lowTempFilter.setPredicate(lowTempPredicate);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(lowTempFilter));
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setCondition(alarmCondition);
        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("lesstempID");
        dpa.setAlarmType("greaterTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, alarmRule)));

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(Futures.immediateFuture(Collections.emptyList()));
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "greaterTemperatureAlarm"))
                .thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);
        Mockito.when(ctx.getAttributesService()).thenReturn(attributesService);
        Mockito.when(ctx.getDeviceService().findDeviceById(tenantId, deviceId))
                .thenReturn(device);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), Mockito.any(AttributeScope.class), Mockito.anySet()))
                .thenReturn(listListenableFutureWithLess);
        Mockito.when(attributesService.find(eq(tenantId), eq(customerId), Mockito.any(AttributeScope.class), Mockito.anyString()))
                .thenReturn(emptyOptionalFuture);
        Mockito.when(attributesService.find(eq(tenantId), eq(tenantId), eq(AttributeScope.SERVER_SCOPE), Mockito.anyString()))
                .thenReturn(optionalListenableFutureWithLess);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString()))
                .thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 150L);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    private void init() throws TbNodeException {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getDeviceProfileCache()).thenReturn(cache);
        Mockito.lenient().when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
        Mockito.lenient().when(ctx.getAlarmService()).thenReturn(alarmService);
        Mockito.when(ctx.getDeviceService()).thenReturn(deviceService);
        Mockito.lenient().when(ctx.getAttributesService()).thenReturn(attributesService);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.newObjectNode());
        node = new TbDeviceProfileNode();
        node.init(ctx, nodeConfiguration);
    }

    private void registerCreateAlarmMock(AlarmApiCallResult a, boolean created) {
        registerCreateAlarmMock(a, created, false);
    }

    private void registerCreateAlarmMock(AlarmApiCallResult a, boolean created, boolean cleared) {
        when(a).thenAnswer(invocationOnMock -> {
            AlarmInfo alarm = new AlarmInfo(new Alarm(new AlarmId(UUID.randomUUID())));
            AlarmModificationRequest request = invocationOnMock.getArgument(0);
            alarm.setSeverity(request.getSeverity());
            return AlarmApiCallResult.builder()
                    .successful(true)
                    .created(created)
                    .modified(true)
                    .cleared(cleared)
                    .alarm(alarm)
                    .build();
        });
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"persistAlarmRulesState\":false,\"fetchAlarmRulesStateOnStart\":false}",
                        true,
                        "{\"persistAlarmRulesState\":false,\"fetchAlarmRulesStateOnStart\":false}"),
                // config for version 1 with upgrade from version 0 (persistAlarmRulesState and fetchAlarmRulesStateOnStart - true)
                Arguments.of(0,
                        "{\"persistAlarmRulesState\":true,\"fetchAlarmRulesStateOnStart\":true}",
                        false,
                        "{\"persistAlarmRulesState\":true,\"fetchAlarmRulesStateOnStart\":true}"),
                // config for version 1 with upgrade from version 0 (persistAlarmRulesState - true, fetchAlarmRulesStateOnStart - false)
                Arguments.of(0,
                        "{\"persistAlarmRulesState\":true,\"fetchAlarmRulesStateOnStart\":false}",
                        false,
                        "{\"persistAlarmRulesState\":true,\"fetchAlarmRulesStateOnStart\":false}"),
                // config for version 1 with upgrade from version 0 (persistAlarmRulesState - false, fetchAlarmRulesStateOnStart - true)
                Arguments.of(0,
                        "{\"persistAlarmRulesState\":false,\"fetchAlarmRulesStateOnStart\":true}",
                        true,
                        "{\"persistAlarmRulesState\":false,\"fetchAlarmRulesStateOnStart\":false}")
        );

    }

    @Test
    public void testAlarmCreateWithAttrAndTsCondition() throws Exception {
        init();

        DeviceProfile deviceProfile = new DeviceProfile();
        DeviceProfileData deviceProfileData = new DeviceProfileData();

        AlarmConditionFilter filter = getAlarmConditionFilter(TIME_SERIES, "temperature", GREATER, 30.0);
        AlarmConditionFilter filter2 = getAlarmConditionFilter(ATTRIBUTE, "battery", LESS, 10.0);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(List.of(filter, filter2));
        AlarmRule createRule = new AlarmRule();
        createRule.setCondition(alarmCondition);

        AlarmConditionFilter filter3 = getAlarmConditionFilter(TIME_SERIES, "temperature", LESS, 10.0);
        AlarmConditionFilter filter4 = getAlarmConditionFilter(ATTRIBUTE, "battery", GREATER, 50.0);
        AlarmCondition clearCondition = new AlarmCondition();
        clearCondition.setCondition(List.of(filter3, filter4));
        AlarmRule clearRule = new AlarmRule();
        clearRule.setCondition(clearCondition);

        DeviceProfileAlarm dpa = new DeviceProfileAlarm();
        dpa.setId("highTemperatureAlarmID");
        dpa.setAlarmType("highTemperatureAlarm");
        dpa.setCreateRules(new TreeMap<>(Collections.singletonMap(AlarmSeverity.CRITICAL, createRule)));
        dpa.setClearRule(clearRule);

        deviceProfileData.setAlarms(Collections.singletonList(dpa));
        deviceProfile.setProfileData(deviceProfileData);

        ListenableFuture<List<TsKvEntry>> tsKvList =
                Futures.immediateFuture(Collections.singletonList(getTsKvEntry("temperature", 35L)));
        ListenableFuture<List<AttributeKvEntry>> attrList =
                Futures.immediateFuture(Collections.emptyList());

        Mockito.when(cache.get(tenantId, deviceId)).thenReturn(deviceProfile);
        Mockito.when(timeseriesService.findLatest(tenantId, deviceId, Collections.singleton("temperature")))
                .thenReturn(tsKvList);
        Mockito.when(attributesService.find(eq(tenantId), eq(deviceId), any(), anySet()))
                .thenReturn(attrList);
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm")).thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString())).thenReturn(theMsg);

        // send attribute
        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("battery", 8);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    private TsKvEntry getTsKvEntry(String key, Long value) {
        TsKvEntity tsKv = new TsKvEntity();
        tsKv.setKey(10);
        tsKv.setLongValue(value);
        tsKv.setStrKey(key);
        tsKv.setTs(System.currentTimeMillis());
        return tsKv.toData();
    }

    private AlarmCondition getNumericAlarmCondition(AlarmConditionKeyType alarmConditionKeyType, String key, NumericOperation operation, Double value) {
        AlarmConditionFilter filter = getAlarmConditionFilter(alarmConditionKeyType, key, operation, value);
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setCondition(Collections.singletonList(filter));
        return alarmCondition;
    }

    private AlarmConditionFilter getAlarmConditionFilter(AlarmConditionKeyType alarmConditionKeyType, String key, NumericOperation operation, Double value) {
        AlarmConditionFilter filter = new AlarmConditionFilter();
        filter.setKey(new AlarmConditionFilterKey(alarmConditionKeyType, key));
        filter.setValueType(EntityKeyValueType.NUMERIC);
        NumericFilterPredicate highTemperaturePredicate = new NumericFilterPredicate();
        highTemperaturePredicate.setOperation(operation);
        highTemperaturePredicate.setValue(new FilterPredicateValue<>(value));
        filter.setPredicate(highTemperaturePredicate);
        return filter;
    }

    @Test
    public void testAlarmCreateAfterTimeseriesUpdated() throws Exception {
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
        Mockito.when(alarmService.findLatestActiveByOriginatorAndType(tenantId, deviceId, "highTemperatureAlarm")).thenReturn(null);
        registerCreateAlarmMock(alarmService.createAlarm(any()), true);

        TbMsg theMsg = TbMsg.newMsg()
                .type(TbMsgType.ALARM)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .build();
        when(ctx.newMsg(any(), any(TbMsgType.class), any(), any(), any(), Mockito.anyString())).thenReturn(theMsg);

        ObjectNode data = JacksonUtil.newObjectNode();
        data.put("temperature", 42);
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.TIMESERIES_UPDATED)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .dataType(TbMsgDataType.JSON)
                .data(JacksonUtil.toString(data))
                .build();
        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        verify(ctx).enqueueForTellNext(theMsg, "Alarm Created");
        verify(ctx, Mockito.never()).tellFailure(Mockito.any(), Mockito.any());
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
