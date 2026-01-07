/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.entitiy.alarmComment;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.entitiy.alarm.DefaultTbAlarmCommentService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbAlarmCommentService.class)
@TestPropertySource(properties = {
        "server.log_controller_error_stack_trace=false"
})
public class DefaultTbAlarmCommentServiceTest {

    @MockBean
    protected DbCallbackExecutorService dbExecutor;
    @MockBean
    protected TbLogEntityActionService logEntityActionService;
    @MockBean
    protected AlarmService alarmService;
    @MockBean
    protected AlarmCommentService alarmCommentService;
    @MockBean
    protected AlarmSubscriptionService alarmSubscriptionService;
    @MockBean
    protected CustomerService customerService;
    @MockBean
    protected TbClusterService tbClusterService;
    @MockBean
    private AccessControlService accessControlService;
    @MockBean
    private TenantService tenantService;
    @MockBean
    private AssetService assetService;
    @MockBean
    private DeviceService deviceService;
    @MockBean
    private AssetProfileService assetProfileService;
    @MockBean
    private DeviceProfileService deviceProfileService;
    @MockBean
    private EntityService entityService;
    @SpyBean
    DefaultTbAlarmCommentService service;

    @Test
    public void testSave() throws ThingsboardException {
        var alarm = new Alarm();
        var alarmComment = new AlarmComment();
        when(alarmCommentService.createOrUpdateAlarmComment(Mockito.any(), eq(alarmComment))).thenReturn(alarmComment);
        service.saveAlarmComment(alarm, alarmComment, new User());

        verify(logEntityActionService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ADDED_COMMENT), any(), any());
    }

    @Test
    public void testDelete() throws ThingsboardException {
        var alarmId = new AlarmId(UUID.randomUUID());
        var alarmComment = new AlarmComment();
        alarmComment.setAlarmId(alarmId);
        alarmComment.setUserId(new UserId(UUID.randomUUID()));
        alarmComment.setType(AlarmCommentType.OTHER);

        when(alarmCommentService.saveAlarmComment(Mockito.any(), eq(alarmComment))).thenReturn(alarmComment);
        service.deleteAlarmComment(new Alarm(alarmId), alarmComment, new User());

        verify(logEntityActionService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.DELETED_COMMENT), any(), any());
    }

    @Test
    public void testShouldNotDeleteSystemComment() {
        var alarmId = new AlarmId(UUID.randomUUID());
        var alarmComment = new AlarmComment();
        alarmComment.setAlarmId(alarmId);
        alarmComment.setType(AlarmCommentType.SYSTEM);

        assertThatThrownBy(() -> service.deleteAlarmComment(new Alarm(alarmId), alarmComment, new User()))
                .isInstanceOf(ThingsboardException.class)
                .hasMessageContaining("System comment could not be deleted");
    }
}