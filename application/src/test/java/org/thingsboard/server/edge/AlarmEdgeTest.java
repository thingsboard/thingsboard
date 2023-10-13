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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AlarmEdgeTest extends AbstractEdgeTest {

    @Test
    public void testSendAlarmToCloud() throws Exception {
        Device device = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        Alarm edgeAlarm = buildAlarmForUplinkMsg(device.getId());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        AlarmUpdateMsg.Builder alarmUpdateMgBuilder = AlarmUpdateMsg.newBuilder();
        alarmUpdateMgBuilder.setIdMSB(edgeAlarm.getUuidId().getMostSignificantBits());
        alarmUpdateMgBuilder.setIdLSB(edgeAlarm.getUuidId().getLeastSignificantBits());
        alarmUpdateMgBuilder.setEntity(JacksonUtil.toString(edgeAlarm));
        alarmUpdateMgBuilder.setOriginatorName(device.getName());
        testAutoGeneratedCodeByProtobuf(alarmUpdateMgBuilder);
        uplinkMsgBuilder.addAlarmUpdateMsg(alarmUpdateMgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        List<AlarmInfo> alarms = doGetTypedWithPageLink("/api/alarm/{entityType}/{entityId}?",
                new TypeReference<PageData<AlarmInfo>>() {},
                new PageLink(100), device.getId().getEntityType().name(), device.getUuidId())
                .getData();
        Optional<AlarmInfo> foundAlarm = alarms.stream().filter(alarm -> alarm.getType().equals("alarm from edge")).findAny();
        Assert.assertTrue(foundAlarm.isPresent());
        AlarmInfo alarmInfo = foundAlarm.get();
        Assert.assertEquals(edgeAlarm.getId(), alarmInfo.getId());
        Assert.assertEquals(device.getId(), alarmInfo.getOriginator());
        Assert.assertEquals(AlarmStatus.ACTIVE_UNACK, alarmInfo.getStatus());
        Assert.assertEquals(AlarmSeverity.CRITICAL, alarmInfo.getSeverity());
    }

    @Test
    public void testAlarms() throws Exception {
        // create alarm
        Device device = findDeviceByName("Edge Device 1");
        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setType("alarm");
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        edgeImitator.expectMessageAmount(1);
        Alarm savedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        AlarmUpdateMsg alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        Alarm alarmMsg = JacksonUtil.fromStringIgnoreUnknownProperties(alarmUpdateMsg.getEntity(), Alarm.class);
        Assert.assertNotNull(alarmMsg);
        Assert.assertEquals(savedAlarm, alarmMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, alarmUpdateMsg.getMsgType());

        // update alarm
        String updatedDetails = "{\"testKey\":\"testValue\"}";
        savedAlarm.setDetails(JacksonUtil.OBJECT_MAPPER.readTree(updatedDetails));
        edgeImitator.expectMessageAmount(1);
        savedAlarm = doPost("/api/alarm", savedAlarm, Alarm.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        alarmMsg = JacksonUtil.fromStringIgnoreUnknownProperties(alarmUpdateMsg.getEntity(), Alarm.class);
        Assert.assertNotNull(alarmMsg);
        Assert.assertEquals(savedAlarm, alarmMsg);
        Assert.assertEquals(updatedDetails, alarmMsg.getDetails().toString());
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, alarmUpdateMsg.getMsgType());

        // ack alarm
        edgeImitator.expectMessageAmount(1);
        doPost("/api/alarm/" + savedAlarm.getUuidId() + "/ack");
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ALARM_ACK_RPC_MESSAGE, alarmUpdateMsg.getMsgType());
        alarmMsg = JacksonUtil.fromStringIgnoreUnknownProperties(alarmUpdateMsg.getEntity(), Alarm.class);
        Assert.assertNotNull(alarmMsg);
        Assert.assertEquals(savedAlarm.getType(), alarmMsg.getType());
        Assert.assertEquals(savedAlarm.getName(), alarmMsg.getName());
        Assert.assertEquals(device.getName(), alarmUpdateMsg.getOriginatorName());
        Assert.assertEquals(AlarmStatus.ACTIVE_ACK, alarmMsg.getStatus());

        // clear alarm
        edgeImitator.expectMessageAmount(1);
        doPost("/api/alarm/" + savedAlarm.getUuidId() + "/clear");
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE, alarmUpdateMsg.getMsgType());
        alarmMsg = JacksonUtil.fromStringIgnoreUnknownProperties(alarmUpdateMsg.getEntity(), Alarm.class);
        Assert.assertNotNull(alarmMsg);
        Assert.assertEquals(savedAlarm.getType(), alarmMsg.getType());
        Assert.assertEquals(savedAlarm.getName(), alarmMsg.getName());
        Assert.assertEquals(device.getName(), alarmUpdateMsg.getOriginatorName());
        Assert.assertEquals(AlarmStatus.CLEARED_ACK, alarmMsg.getStatus());

        // delete alarm
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/alarm/" + savedAlarm.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AlarmUpdateMsg);
        alarmUpdateMsg = (AlarmUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, alarmUpdateMsg.getMsgType());
        alarmMsg = JacksonUtil.fromStringIgnoreUnknownProperties(alarmUpdateMsg.getEntity(), Alarm.class);
        Assert.assertNotNull(alarmMsg);
        Assert.assertEquals(savedAlarm.getType(), alarmMsg.getType());
        Assert.assertEquals(savedAlarm.getName(), alarmMsg.getName());
        Assert.assertEquals(device.getName(), alarmUpdateMsg.getOriginatorName());
        Assert.assertEquals(AlarmStatus.CLEARED_ACK, alarmMsg.getStatus());
    }

    private Alarm buildAlarmForUplinkMsg(DeviceId deviceId) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(UUID.randomUUID()));
        alarm.setTenantId(tenantId);
        alarm.setType("alarm from edge");
        alarm.setOriginator(deviceId);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        return alarm;
    }

}
