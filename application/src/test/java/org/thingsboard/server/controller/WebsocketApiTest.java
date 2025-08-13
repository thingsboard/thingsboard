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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;
import org.thingsboard.server.service.subscription.TbAttributeSubscriptionScope;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "server.ws.alarms_per_alarm_status_subscription_cache_size=5",
        "server.ws.dynamic_page_link.refresh_interval=15"
})
public class WebsocketApiTest extends AbstractControllerTest {
    @Autowired
    private TelemetrySubscriptionService tsService;

    Device device;
    DeviceTypeFilter dtf;

    @Before
    public void setUp() throws Exception {
        loginTenantAdmin();

        device = new Device();
        device.setName("Device");
        device.setType("default");
        device.setLabel("testLabel" + (int) (Math.random() * 1000));
        device = doPost("/api/device", device, Device.class);
        dtf = new DeviceTypeFilter(List.of(device.getType()), device.getName());
    }

    @After
    public void tearDown() throws Exception {
        loginTenantAdmin();
        doDelete("/api/device/" + device.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testEntityDataHistoryWsCmd() throws Exception {
        List<String> keys = List.of("temperature");
        long now = System.currentTimeMillis();

        EntityDataUpdate update = getWsClient().sendHistoryCmd(keys, now, TimeUnit.HOURS.toMillis(1), dtf);

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertEquals(0, pageData.getData().get(0).getTimeseries().get("temperature").length);

        TsKvEntry dataPoint1 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("temperature", 42L));
        TsKvEntry dataPoint2 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(2), new LongDataEntry("temperature", 42L));
        TsKvEntry dataPoint3 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(3), new LongDataEntry("temperature", 42L));
        List<TsKvEntry> tsData = Arrays.asList(dataPoint1, dataPoint2, dataPoint3);

        sendTelemetry(device, tsData);

        update = getWsClient().sendHistoryCmd(keys, now, TimeUnit.HOURS.toMillis(1), dtf);

        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> dataList = update.getUpdate();
        Assert.assertNotNull(dataList);
        Assert.assertEquals(1, dataList.size());
        Assert.assertEquals(device.getId(), dataList.get(0).getEntityId());
        TsValue[] tsArray = dataList.get(0).getTimeseries().get("temperature");
        Assert.assertEquals(3, tsArray.length);
        Assert.assertEquals(new TsValue(dataPoint1.getTs(), dataPoint1.getValueAsString()), tsArray[0]);
        Assert.assertEquals(new TsValue(dataPoint2.getTs(), dataPoint2.getValueAsString()), tsArray[1]);
        Assert.assertEquals(new TsValue(dataPoint3.getTs(), dataPoint3.getValueAsString()), tsArray[2]);
    }

    @Test
    public void testEntityDataTimeSeriesWsCmd() throws Exception {
        long now = System.currentTimeMillis();

        EntityDataUpdate update = getWsClient().sendEntityDataQuery(dtf);

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());

        TsKvEntry dataPoint1 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("temperature", 42L));
        TsKvEntry dataPoint2 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(2), new LongDataEntry("temperature", 43L));
        TsKvEntry dataPoint3 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(3), new LongDataEntry("temperature", 44L));
        List<TsKvEntry> tsData = Arrays.asList(dataPoint1, dataPoint2, dataPoint3);

        sendTelemetry(device, tsData);

        update = getWsClient().subscribeTsUpdate(List.of("temperature"), now, TimeUnit.HOURS.toMillis(1));
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(device.getId(), listData.get(0).getEntityId());
        TsValue[] tsArray = listData.get(0).getTimeseries().get("temperature");
        Assert.assertEquals(3, tsArray.length);
        Assert.assertEquals(new TsValue(dataPoint1.getTs(), dataPoint1.getValueAsString()), tsArray[0]);
        Assert.assertEquals(new TsValue(dataPoint2.getTs(), dataPoint2.getValueAsString()), tsArray[1]);
        Assert.assertEquals(new TsValue(dataPoint3.getTs(), dataPoint3.getValueAsString()), tsArray[2]);

        now = System.currentTimeMillis();
        TsKvEntry dataPoint4 = new BasicTsKvEntry(now, new LongDataEntry("temperature", 45L));
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, Arrays.asList(dataPoint4));
        String msg = getWsClient().waitForUpdate();

        update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getTimeseries());
        TsValue[] tsValues = eData.get(0).getTimeseries().get("temperature");
        Assert.assertNotNull(tsValues);
        Assert.assertEquals(new TsValue(dataPoint4.getTs(), dataPoint4.getValueAsString()), tsValues[0]);
    }

    @Test
    public void testEntityCountWsCmd() throws Exception {
        AttributeKvEntry dataPoint1 = new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("temperature", 42L));
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, Collections.singletonList(dataPoint1));

        EntityCountQuery edq1 = new EntityCountQuery(dtf, Collections.emptyList());
        EntityCountCmd cmd1 = new EntityCountCmd(1, edq1);

        getWsClient().send(cmd1);

        EntityCountUpdate update1 = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update1.getCmdId());
        Assert.assertEquals(1, update1.getCount());

        DeviceTypeFilter dtf2 = new DeviceTypeFilter(List.of("non-existing-device-type"), "D");
        EntityCountQuery edq2 = new EntityCountQuery(dtf2, Collections.emptyList());
        EntityCountCmd cmd2 = new EntityCountCmd(2, edq2);

        getWsClient().send(cmd2);

        String msg2 = getWsClient().waitForReply();
        EntityCountUpdate update2 = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(2, update2.getCmdId());
        Assert.assertEquals(0, update2.getCount());

        KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(40));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        highTemperatureFilter.setValueType(EntityKeyValueType.NUMERIC);

        DeviceTypeFilter dtf3 = new DeviceTypeFilter(List.of("default"), "D");
        EntityCountQuery edq3 = new EntityCountQuery(dtf3, Collections.singletonList(highTemperatureFilter));
        EntityCountCmd cmd3 = new EntityCountCmd(3, edq3);
        getWsClient().send(cmd3);

        EntityCountUpdate update3 = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(3, update3.getCmdId());
        Assert.assertEquals(1, update3.getCount());

        KeyFilter highTemperatureFilter2 = new KeyFilter();
        highTemperatureFilter2.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate2 = new NumericFilterPredicate();
        predicate2.setValue(FilterPredicateValue.fromDouble(50));
        predicate2.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter2.setPredicate(predicate2);
        highTemperatureFilter2.setValueType(EntityKeyValueType.NUMERIC);

        DeviceTypeFilter dtf4 = new DeviceTypeFilter(List.of("default"), "D");
        EntityCountQuery edq4 = new EntityCountQuery(dtf4, Collections.singletonList(highTemperatureFilter2));
        EntityCountCmd cmd4 = new EntityCountCmd(4, edq4);

        getWsClient().send(cmd4);

        EntityCountUpdate update4 = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(4, update4.getCmdId());
        Assert.assertEquals(0, update4.getCount());
    }

    @Test
    public void testAlarmCountWsCmd() throws Exception {
        loginTenantAdmin();

        AlarmCountCmd cmd1 = new AlarmCountCmd(1, new AlarmCountQuery());

        getWsClient().send(cmd1);

        AlarmCountUpdate update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(0, update.getCount());

        Alarm alarm = new Alarm();
        alarm.setOriginator(tenantId);
        alarm.setType("TEST ALARM");
        alarm.setSeverity(AlarmSeverity.WARNING);

        alarm = doPost("/api/alarm", alarm, Alarm.class);

        AlarmCountCmd cmd2 = new AlarmCountCmd(2, new AlarmCountQuery());

        getWsClient().send(cmd2);

        update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(2, update.getCmdId());
        Assert.assertEquals(1, update.getCount());

        AlarmCountCmd cmd3 = new AlarmCountCmd(3, AlarmCountQuery.builder().assigneeId(tenantAdminUserId).build());

        getWsClient().send(cmd3);

        update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(3, update.getCmdId());
        Assert.assertEquals(0, update.getCount());

        alarm.setAssigneeId(tenantAdminUserId);
        alarm = doPost("/api/alarm", alarm, Alarm.class);

        AlarmCountCmd cmd4 = new AlarmCountCmd(4, AlarmCountQuery.builder().assigneeId(tenantAdminUserId).build());

        getWsClient().send(cmd4);

        update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(4, update.getCmdId());
        Assert.assertEquals(1, update.getCount());

        AlarmCountCmd cmd5 = new AlarmCountCmd(5,
                AlarmCountQuery.builder().severityList(Collections.singletonList(AlarmSeverity.CRITICAL)).build());

        getWsClient().send(cmd5);

        update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(5, update.getCmdId());
        Assert.assertEquals(0, update.getCount());

        alarm.setSeverity(AlarmSeverity.CRITICAL);
        doPost("/api/alarm", alarm, Alarm.class);

        AlarmCountCmd cmd6 = new AlarmCountCmd(6,
                AlarmCountQuery.builder().severityList(Collections.singletonList(AlarmSeverity.CRITICAL)).build());

        getWsClient().send(cmd6);

        update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(6, update.getCmdId());
        Assert.assertEquals(1, update.getCount());
    }

    @Test
    public void testAlarmCountWsCmdWithSingleEntityFilter() throws Exception {
        loginTenantAdmin();

        SingleEntityFilter singleEntityFilter = new SingleEntityFilter();
        singleEntityFilter.setSingleEntity(AliasEntityId.fromEntityId(tenantId));
        AlarmCountQuery alarmCountQuery = new AlarmCountQuery(singleEntityFilter);
        AlarmCountCmd cmd1 = new AlarmCountCmd(1, alarmCountQuery);

        getWsClient().send(cmd1);

        AlarmCountUpdate update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(0, update.getCount());

        //create alarm, check count = 1
        getWsClient().registerWaitForUpdate();

        Alarm alarm = new Alarm();
        alarm.setOriginator(tenantId);
        alarm.setType("TEST ALARM");
        alarm.setSeverity(AlarmSeverity.WARNING);
        alarm = doPost("/api/alarm", alarm, Alarm.class);

        update = getWsClient().parseAlarmCountReply(getWsClient().waitForUpdate());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(1, update.getCount());

        // set wrong entity id in filter, check count = 0
        singleEntityFilter.setSingleEntity(AliasEntityId.fromEntityId(tenantAdminUserId));
        AlarmCountCmd cmd3 = new AlarmCountCmd(2, alarmCountQuery);

        getWsClient().send(cmd3);

        update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(2, update.getCmdId());
        Assert.assertEquals(0, update.getCount());
    }

    @Test
    public void testAlarmCountWsCmdWithDeviceType() throws Exception {
        loginTenantAdmin();

        DeviceTypeFilter deviceTypeFilter = new DeviceTypeFilter();
        deviceTypeFilter.setDeviceTypes(List.of("default"));
        AlarmCountQuery alarmCountQuery = new AlarmCountQuery(deviceTypeFilter);
        AlarmCountCmd cmd1 = new AlarmCountCmd(1, alarmCountQuery);

        getWsClient().send(cmd1);

        AlarmCountUpdate update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(0, update.getCount());

        getWsClient().registerWaitForUpdate();

        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setType("TEST ALARM");
        alarm.setSeverity(AlarmSeverity.WARNING);

        alarm = doPost("/api/alarm", alarm, Alarm.class);

        update = getWsClient().parseAlarmCountReply(getWsClient().waitForUpdate());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(1, update.getCount());

        deviceTypeFilter.setDeviceTypes(List.of("non-existing"));
        AlarmCountCmd cmd3 = new AlarmCountCmd(3, alarmCountQuery);

        getWsClient().send(cmd3);

        update = getWsClient().parseAlarmCountReply(getWsClient().waitForReply());
        Assert.assertEquals(3, update.getCmdId());
        Assert.assertEquals(0, update.getCount());
    }

    @Test
    public void testAlarmStatusWsCmd() throws Exception {
        loginTenantAdmin();

        AlarmStatusCmd cmd = new AlarmStatusCmd(1, device.getId(), List.of("TEST ALARM", "TEST ALARM 2"), List.of(AlarmSeverity.WARNING));

        getWsClient().send(cmd);

        AlarmStatusUpdate update = JacksonUtil.fromString(getWsClient().waitForReply(), AlarmStatusUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertFalse(update.isActive());

        //create alarm
        getWsClient().registerWaitForUpdate();

        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setType("TEST ALARM");
        alarm.setSeverity(AlarmSeverity.WARNING);

        alarm = doPost("/api/alarm", alarm, Alarm.class);

        AlarmStatusUpdate alarmStatusUpdate = JacksonUtil.fromString(getWsClient().waitForUpdate(), AlarmStatusUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertTrue(alarmStatusUpdate.isActive());

        //clear alarm
        getWsClient().registerWaitForUpdate();

        String alarmId = alarm.getId().getId().toString();
        Alarm clearedAlarm = doPost("/api/alarm/" + alarmId + "/clear", Alarm.class);
        Assert.assertNotNull(clearedAlarm);
        Assert.assertTrue(clearedAlarm.isCleared());

        AlarmStatusUpdate alarmStatusUpdate2 = JacksonUtil.fromString(getWsClient().waitForUpdate(), AlarmStatusUpdate.class);
        Assert.assertEquals(1, alarmStatusUpdate2.getCmdId());
        Assert.assertFalse(alarmStatusUpdate2.isActive());

        // add second type alarm
        getWsClient().registerWaitForUpdate();

        Alarm alarm2 = new Alarm();
        alarm2.setOriginator(device.getId());
        alarm2.setType("TEST ALARM 2");
        alarm2.setSeverity(AlarmSeverity.WARNING);

        doPost("/api/alarm", alarm2, Alarm.class);

        AlarmStatusUpdate alarmStatusUpdate3 = JacksonUtil.fromString(getWsClient().waitForUpdate(), AlarmStatusUpdate.class);
        Assert.assertEquals(1, alarmStatusUpdate3.getCmdId());
        Assert.assertTrue(alarmStatusUpdate3.isActive());

        //change severity
        getWsClient().registerWaitForUpdate();
        alarm2.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm2, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());

        AlarmStatusUpdate alarmStatusUpdate4 = JacksonUtil.fromString(getWsClient().waitForUpdate(), AlarmStatusUpdate.class);
        Assert.assertEquals(1, alarmStatusUpdate4.getCmdId());
        Assert.assertFalse(alarmStatusUpdate4.isActive());

        //subscribe for critical alarms
        AlarmStatusCmd cmd3 = new AlarmStatusCmd(2, device.getId(), List.of("TEST ALARM"), List.of(AlarmSeverity.CRITICAL));

        getWsClient().send(cmd3);

        AlarmStatusUpdate alarmStatusUpdate5 = JacksonUtil.fromString(getWsClient().waitForReply(), AlarmStatusUpdate.class);
        Assert.assertEquals(2, alarmStatusUpdate5.getCmdId());
        Assert.assertFalse(alarmStatusUpdate5.isActive());
    }

    @Test
    public void testAlarmStatusWsCmdWithMaxAlarmsCacheSize() throws Exception {
        loginTenantAdmin();

        AlarmStatusCmd cmd = new AlarmStatusCmd(1, device.getId(), null, List.of(AlarmSeverity.CRITICAL));

        getWsClient().send(cmd);

        AlarmStatusUpdate update = JacksonUtil.fromString(getWsClient().waitForReply(), AlarmStatusUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertFalse(update.isActive());

        getWsClient().registerWaitForUpdate();
        //create 5+1 alarms
        List<Alarm> alarms = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Alarm alarm = new Alarm();
            alarm.setOriginator(device.getId());
            alarm.setType(RandomStringUtils.randomAlphabetic(10));
            alarm.setSeverity(AlarmSeverity.CRITICAL);
            alarm = doPost("/api/alarm", alarm, Alarm.class);
            alarms.add(alarm);
        }

        AlarmStatusUpdate updateAfterAlarmsAdded = JacksonUtil.fromString(getWsClient().waitForReply(), AlarmStatusUpdate.class);
        Assert.assertEquals(1, updateAfterAlarmsAdded.getCmdId());
        Assert.assertTrue(updateAfterAlarmsAdded.isActive());

        getWsClient().registerWaitForUpdate();
        //clear first 5 alarms
        for (int i = 0; i < 5; i++) {
            String alarmId = alarms.get(i).getId().getId().toString();
            doPost("/api/alarm/" + alarmId + "/clear", Alarm.class);
        }
        AlarmStatusUpdate alarmStatusUpdate = JacksonUtil.fromString(getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(5)), AlarmStatusUpdate.class);
        Assert.assertNull(alarmStatusUpdate);

        //clear 6-th alarm should send update
        String alarmId6 = alarms.get(5).getId().getId().toString();
        doPost("/api/alarm/" + alarmId6 + "/clear", Alarm.class);

        AlarmStatusUpdate alarmStatusUpdate2 = JacksonUtil.fromString(getWsClient().waitForUpdate(), AlarmStatusUpdate.class);
        Assert.assertEquals(1, alarmStatusUpdate2.getCmdId());
        Assert.assertFalse(alarmStatusUpdate2.isActive());
    }

    @Test
    public void testEntityDataLatestWidgetFlow() throws Exception {
        List<EntityKey> keys = List.of(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        long now = System.currentTimeMillis() - 100;

        EntityDataQuery edq = new EntityDataQuery(dtf, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), keys, Collections.emptyList());

        EntityDataUpdate update = getWsClient().sendEntityDataQuery(edq);
        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue());

        TsKvEntry dataPoint1 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("temperature", 42L));
        List<TsKvEntry> tsData = Arrays.asList(dataPoint1);
        sendTelemetry(device, tsData);

        update = getWsClient().subscribeLatestUpdate(keys);

        Assert.assertEquals(1, update.getCmdId());

        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(device.getId(), listData.get(0).getEntityId());
        Assert.assertNotNull(listData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        TsValue tsValue = listData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature");
        Assert.assertEquals(new TsValue(dataPoint1.getTs(), dataPoint1.getValueAsString()), tsValue);

        now = System.currentTimeMillis();
        TsKvEntry dataPoint2 = new BasicTsKvEntry(now, new LongDataEntry("temperature", 52L));

        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, Arrays.asList(dataPoint2));

        update = getWsClient().parseDataReply(getWsClient().waitForUpdate());
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature");
        Assert.assertEquals(new TsValue(dataPoint2.getTs(), dataPoint2.getValueAsString()), tsValue);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, Arrays.asList(dataPoint1));
        String msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending duplicate update again
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, Arrays.asList(dataPoint2));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);
    }

    @Test
    public void testTimeseriesSubscriptionCmd() throws Exception {
        long now = System.currentTimeMillis() - 100;

        long lastTs = now - TimeUnit.MINUTES.toMillis(1);
        TsKvEntry dataPoint1 = new BasicTsKvEntry(lastTs, new LongDataEntry("temperature", 42L));
        sendTelemetry(device, List.of(dataPoint1));

        JsonNode update = getWsClient().sendTimeseriesCmd(device.getId(), "LATEST_TELEMETRY");
        JsonNode data = update.get("data");
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(JacksonUtil.newArrayNode().add(lastTs).add("42"), data.get("temperature").get(0));

        //Sending update from the past, while latest value has new timestamp;
        TsKvEntry dataPoint4 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(5), new LongDataEntry("temperature", 45L));
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, List.of(dataPoint4));
        String msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending duplicate update again
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, List.of(dataPoint4));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);
    }

    @Test
    public void testEntityDataLatestTsWsCmd() throws Exception {
        long now = System.currentTimeMillis();
        List<EntityKey> keys = List.of(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));

        EntityDataUpdate update = getWsClient().subscribeLatestUpdate(keys, dtf);

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue());

        getWsClient().registerWaitForUpdate();
        TsKvEntry dataPoint1 = new BasicTsKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("temperature", 42L));
        List<TsKvEntry> tsData = Arrays.asList(dataPoint1);
        sendTelemetry(device, tsData);

        update = getWsClient().parseDataReply(getWsClient().waitForUpdate());

        Assert.assertEquals(1, update.getCmdId());

        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(device.getId(), listData.get(0).getEntityId());
        Assert.assertNotNull(listData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        TsValue tsValue = listData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature");
        Assert.assertEquals(new TsValue(dataPoint1.getTs(), dataPoint1.getValueAsString()), tsValue);

        now = System.currentTimeMillis();
        TsKvEntry dataPoint2 = new BasicTsKvEntry(now, new LongDataEntry("temperature", 52L));
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, Arrays.asList(dataPoint2));
        update = getWsClient().parseDataReply(getWsClient().waitForUpdate());
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("temperature");
        Assert.assertEquals(new TsValue(dataPoint2.getTs(), dataPoint2.getValueAsString()), tsValue);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, Arrays.asList(dataPoint1));
        String msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending duplicate update again
        getWsClient().registerWaitForUpdate();
        sendTelemetry(device, Arrays.asList(dataPoint2));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);
    }

    @Test
    public void testEntityDataLatestAttrWsCmd() throws Exception {
        long now = System.currentTimeMillis();
        List<EntityKey> keys = List.of(new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, "serverAttributeKey"));

        EntityDataUpdate update = getWsClient().subscribeLatestUpdate(keys, dtf);
        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey").getValue());

        getWsClient().registerWaitForUpdate();

        // Pushing update with wrong scope and make sure it will not arrive.
        AttributeKvEntry invalidDataPoint = new BaseAttributeKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("serverAttributeKey", 55L));
        sendAttributes(device, TbAttributeSubscriptionScope.CLIENT_SCOPE, Arrays.asList(invalidDataPoint));

        Assert.assertNull(getWsClient().waitForUpdate(3000));

        AttributeKvEntry dataPoint1 = new BaseAttributeKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("serverAttributeKey", 42L));
        List<AttributeKvEntry> tsData = Arrays.asList(dataPoint1);
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, tsData);

        String msg = getWsClient().waitForUpdate();
        Assert.assertNotNull(msg);
        update = JacksonUtil.fromString(msg, EntityDataUpdate.class);

        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(device.getId(), listData.get(0).getEntityId());
        Assert.assertNotNull(listData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE));
        TsValue tsValue = listData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint1.getLastUpdateTs(), dataPoint1.getValueAsString()), tsValue);

        now = System.currentTimeMillis();
        AttributeKvEntry dataPoint2 = new BaseAttributeKvEntry(now, new LongDataEntry("serverAttributeKey", 52L));

        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, Arrays.asList(dataPoint2));
        msg = getWsClient().waitForUpdate();
        Assert.assertNotNull(msg);

        update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE));
        tsValue = eData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint2.getLastUpdateTs(), dataPoint2.getValueAsString()), tsValue);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, Arrays.asList(dataPoint1));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending duplicate update again
        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, Arrays.asList(dataPoint2));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);
    }

    @Test
    public void testEntityDataLatestAttrTypesWsCmd() throws Exception {
        long now = System.currentTimeMillis();
        List<EntityKey> keys = List.of(
                new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, "serverAttributeKey"),
                new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, "clientAttributeKey"),
                new EntityKey(EntityKeyType.SHARED_ATTRIBUTE, "sharedAttributeKey"),
                new EntityKey(EntityKeyType.ATTRIBUTE, "anyAttributeKey"));

        EntityDataUpdate update = getWsClient().subscribeLatestUpdate(keys, dtf);

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(device.getId(), pageData.getData().get(0).getEntityId());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey").getValue());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("clientAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("clientAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("clientAttributeKey").getValue());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE).get("sharedAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE).get("sharedAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE).get("sharedAttributeKey").getValue());
        Assert.assertNotNull(pageData.getData().get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey"));
        Assert.assertEquals(0, pageData.getData().get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey").getTs());
        Assert.assertEquals("", pageData.getData().get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey").getValue());

        getWsClient().registerWaitForUpdate();
        AttributeKvEntry dataPoint1 = new BaseAttributeKvEntry(now - TimeUnit.MINUTES.toMillis(1), new LongDataEntry("serverAttributeKey", 42L));
        List<AttributeKvEntry> tsData = Arrays.asList(dataPoint1);

        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, tsData);

        String msg = getWsClient().waitForUpdate();
        Assert.assertNotNull(msg);
        update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE));
        TsValue attrValue = eData.get(0).getLatest().get(EntityKeyType.SERVER_ATTRIBUTE).get("serverAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint1.getLastUpdateTs(), dataPoint1.getValueAsString()), attrValue);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.SHARED_SCOPE, Arrays.asList(dataPoint1));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending duplicate update again
        getWsClient().registerWaitForUpdate();
        sendAttributes(device, TbAttributeSubscriptionScope.CLIENT_SCOPE, Arrays.asList(dataPoint1));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        Assert.assertNull(msg);

        //Sending update from the past, while latest value has new timestamp;
        getWsClient().registerWaitForUpdate();
        AttributeKvEntry dataPoint2 = new BaseAttributeKvEntry(now, new LongDataEntry("sharedAttributeKey", 42L));
        sendAttributes(device, TbAttributeSubscriptionScope.SHARED_SCOPE, Arrays.asList(dataPoint2));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE));
        attrValue = eData.get(0).getLatest().get(EntityKeyType.SHARED_ATTRIBUTE).get("sharedAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint2.getLastUpdateTs(), dataPoint2.getValueAsString()), attrValue);

        getWsClient().registerWaitForUpdate();
        AttributeKvEntry dataPoint3 = new BaseAttributeKvEntry(now, new LongDataEntry("clientAttributeKey", 42L));
        sendAttributes(device, TbAttributeSubscriptionScope.CLIENT_SCOPE, Arrays.asList(dataPoint3));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE));
        attrValue = eData.get(0).getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("clientAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint3.getLastUpdateTs(), dataPoint3.getValueAsString()), attrValue);

        getWsClient().registerWaitForUpdate();
        AttributeKvEntry dataPoint4 = new BaseAttributeKvEntry(now, new LongDataEntry("anyAttributeKey", 42L));
        sendAttributes(device, TbAttributeSubscriptionScope.CLIENT_SCOPE, Arrays.asList(dataPoint4));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.ATTRIBUTE));
        attrValue = eData.get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint4.getLastUpdateTs(), dataPoint4.getValueAsString()), attrValue);

        getWsClient().registerWaitForUpdate();
        AttributeKvEntry dataPoint5 = new BaseAttributeKvEntry(now, new LongDataEntry("anyAttributeKey", 43L));
        sendAttributes(device, TbAttributeSubscriptionScope.SERVER_SCOPE, Arrays.asList(dataPoint5));
        msg = getWsClient().waitForUpdate(TimeUnit.SECONDS.toMillis(1));
        update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.ATTRIBUTE));
        attrValue = eData.get(0).getLatest().get(EntityKeyType.ATTRIBUTE).get("anyAttributeKey");
        Assert.assertEquals(new TsValue(dataPoint5.getLastUpdateTs(), dataPoint5.getValueAsString()), attrValue);
    }

    @Test
    public void testAttributesSubscription_sysAdmin() throws Exception {
        loginSysAdmin();
        SingleEntityFilter entityFilter = new SingleEntityFilter();
        entityFilter.setSingleEntity(AliasEntityId.fromEntityId(tenantId));

        assertThatNoException().as("subscribeForAttributes").isThrownBy(() -> {
            JsonNode update = getWsClient().subscribeForAttributes(tenantId, TbAttributeSubscriptionScope.SERVER_SCOPE.name(), List.of("attr"));
            assertThat(update.get("errorMsg").isNull()).isTrue();
            assertThat(update.get("errorCode").asInt()).isEqualTo(SubscriptionErrorCode.NO_ERROR.getCode());
        });

        getWsClient().registerWaitForUpdate();
        String expectedAttrValue = "42";
        sendAttributes(TenantId.SYS_TENANT_ID, tenantId, TbAttributeSubscriptionScope.SERVER_SCOPE, List.of(
                new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry("attr", expectedAttrValue))
        ));
        JsonNode update = JacksonUtil.toJsonNode(getWsClient().waitForUpdate());
        assertThat(update).as("waitForUpdate").isNotNull();
        assertThat(update.get("data").get("attr").get(0).get(1).asText()).isEqualTo(expectedAttrValue);
    }

    @Test
    public void testEntityCountCmd_filterTypeSingularCompatibilityTest() throws Exception {
        ObjectNode oldFormatDeviceTypeFilterSingular = JacksonUtil.newObjectNode();
        oldFormatDeviceTypeFilterSingular.put("type", "deviceType");
        oldFormatDeviceTypeFilterSingular.put("deviceType", "default");
        oldFormatDeviceTypeFilterSingular.put("deviceNameFilter", "Device");

        ObjectNode query = JacksonUtil.newObjectNode();
        query.set("entityFilter", oldFormatDeviceTypeFilterSingular);

        ObjectNode entityCountCmd = JacksonUtil.newObjectNode();
        entityCountCmd.put("cmdId", 1);
        entityCountCmd.set("query", query);

        ArrayNode entityCountCmds = JacksonUtil.newArrayNode();
        entityCountCmds.add(entityCountCmd);

        ObjectNode wrapperNode = JacksonUtil.newObjectNode();
        wrapperNode.set("entityCountCmds", entityCountCmds);

        wsClient = buildAndConnectWebSocketClient("/api/ws/plugins/telemetry?token=" + token);
        wsClient.send(JacksonUtil.toString(wrapperNode));

        EntityCountUpdate update = wsClient.parseCountReply(wsClient.waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(1, update.getCount());

    }

    private void sendTelemetry(Device device, List<TsKvEntry> tsData) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        tsService.saveTimeseries(TimeseriesSaveRequest.builder()
                .tenantId(device.getTenantId())
                .entityId(device.getId())
                .entries(tsData)
                .callback(new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        log.debug("sendTelemetry callback onSuccess");
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Failed to send telemetry", t);
                        latch.countDown();
                    }
                })
                .build());
        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).as("await sendTelemetry callback");
    }

    private void sendAttributes(Device device, TbAttributeSubscriptionScope scope, List<AttributeKvEntry> attrData) throws InterruptedException {
        sendAttributes(device.getTenantId(), device.getId(), scope, attrData);
    }

    private void sendAttributes(TenantId tenantId, EntityId entityId, TbAttributeSubscriptionScope scope, List<AttributeKvEntry> attrData) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        tsService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(entityId)
                .scope(scope.getAttributeScope())
                .entries(attrData)
                .callback(new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        log.debug("sendAttributes callback onSuccess");
                        latch.countDown();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("Failed to sendAttributes", t);
                        latch.countDown();
                    }
                })
                .build());
        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS)).as("await sendAttributes callback").isTrue();
    }

}
