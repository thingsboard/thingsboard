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
package org.thingsboard.server.service.entitiy.alarm;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbAlarmService.class)
@TestPropertySource(properties = {
        "server.log_controller_error_stack_trace=false"
})
public class DefaultTbAlarmServiceTest {

    @MockBean
    protected DbCallbackExecutorService dbExecutor;
    @MockBean
    protected TbLogEntityActionService logEntityActionService;
    @MockBean
    protected EdgeService edgeService;
    @MockBean
    protected AlarmService alarmService;
    @MockBean
    protected TbAlarmCommentService alarmCommentService;
    @MockBean
    protected AlarmSubscriptionService alarmSubscriptionService;
    @MockBean
    protected CustomerService customerService;
    @MockBean
    protected TbClusterService tbClusterService;
    @MockBean
    private EntitiesVersionControlService vcService;
    @SpyBean
    DefaultTbAlarmService service;

    @Test
    public void testSave() throws ThingsboardException {
        var alarm = new AlarmInfo();
        when(alarmSubscriptionService.createAlarm(any())).thenReturn(AlarmApiCallResult.builder()
                .successful(true)
                .modified(true)
                .alarm(alarm)
                .build());
        service.save(alarm, new User());

        verify(logEntityActionService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ADDED), any());
        verify(alarmSubscriptionService, times(1)).createAlarm(any());
    }

    @Test
    public void testAck() throws ThingsboardException {
        var alarm = new Alarm();
        when(alarmSubscriptionService.acknowledgeAlarm(any(), any(), anyLong()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).modified(true).alarm(new AlarmInfo()).build());
        service.ack(alarm, new User(new UserId(UUID.randomUUID())));

        verify(alarmCommentService, times(1)).saveAlarmComment(any(), any(), any());
        verify(logEntityActionService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ALARM_ACK), any());
        verify(alarmSubscriptionService, times(1)).acknowledgeAlarm(any(), any(), anyLong());
    }

    @Test
    public void testClear() throws ThingsboardException {
        var alarm = new Alarm();
        alarm.setAcknowledged(true);
        when(alarmSubscriptionService.clearAlarm(any(), any(), anyLong(), any()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).cleared(true).alarm(new AlarmInfo()).build());
        service.clear(alarm, new User(new UserId(UUID.randomUUID())));

        verify(alarmCommentService, times(1)).saveAlarmComment(any(), any(), any());
        verify(logEntityActionService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ALARM_CLEAR), any());
        verify(alarmSubscriptionService, times(1)).clearAlarm(any(), any(), anyLong(), any());
    }

    @Test
    public void testDelete() {
        service.delete(new Alarm(), new User());

        verify(logEntityActionService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ALARM_DELETE), any(), any());
        verify(alarmSubscriptionService, times(1)).deleteAlarm(any(), any());
    }

    @Test
    public void testUnassignAlarm() throws ThingsboardException {
        AlarmInfo alarm = new AlarmInfo();
        alarm.setId(new AlarmId(UUID.randomUUID()));
        when(alarmSubscriptionService.unassignAlarm(any(), any(), anyLong()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).modified(true).alarm(alarm).build());

        User user = new User();
        user.setEmail("testEmail@gmail.com");
        user.setId(new UserId(UUID.randomUUID()));
        service.unassign(new Alarm(), 0L, user);

        ObjectNode commentNode = JacksonUtil.newObjectNode();
        commentNode.put("subtype", "ASSIGN");
        commentNode.put("text", "Alarm was unassigned by user " + user.getTitle());
        commentNode.put("userId", user.getId().getId().toString());
        AlarmComment expectedAlarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(commentNode)
                .build();

        verify(alarmCommentService, times(1))
                .saveAlarmComment(eq(alarm), eq(expectedAlarmComment), eq(user));
    }

    @Test
    public void testUnassignDeletedUserAlarms() throws ThingsboardException {
        AlarmInfo alarm = new AlarmInfo();
        alarm.setId(new AlarmId(UUID.randomUUID()));

        when(alarmSubscriptionService.unassignAlarm(any(), any(), anyLong()))
                .thenReturn(AlarmApiCallResult.builder().successful(true).modified(true).alarm(alarm).build());

        User user = new User();
        user.setEmail("testEmail@gmail.com");
        user.setId(new UserId(UUID.randomUUID()));
        service.unassignDeletedUserAlarms(new TenantId(UUID.randomUUID()), user.getId(), user.getTitle(), List.of(alarm.getUuidId()), System.currentTimeMillis());

        ObjectNode commentNode = JacksonUtil.newObjectNode();
        commentNode.put("subtype", "ASSIGN");
        commentNode.put("text", String.format("Alarm was unassigned because user %s - was deleted", user.getTitle()));
        AlarmComment expectedAlarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(commentNode)
                .build();

        verify(alarmCommentService, times(1))
                .saveAlarmComment(eq(alarm), eq(expectedAlarmComment), eq(null));
    }


}
