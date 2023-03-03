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
package org.thingsboard.server.service.entitiy.alarm;

import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    protected TbNotificationEntityService notificationEntityService;
    @MockBean
    protected EdgeService edgeService;
    @MockBean
    protected AlarmService alarmService;
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
        var alarm = new Alarm();
        when(alarmSubscriptionService.createOrUpdateAlarm(alarm)).thenReturn(alarm);
        service.save(alarm, new User());

        verify(notificationEntityService, times(1)).notifyCreateOrUpdateAlarm(any(), any(), any());
        verify(alarmSubscriptionService, times(1)).createOrUpdateAlarm(eq(alarm));
    }

    @Test
    public void testAck() {
        var alarm = new Alarm();
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        when(alarmSubscriptionService.ackAlarm(any(), any(), anyLong())).thenReturn(Futures.immediateFuture(true));
        service.ack(alarm, new User());

        verify(notificationEntityService, times(1)).notifyCreateOrUpdateAlarm(any(), any(), any());
        verify(alarmSubscriptionService, times(1)).ackAlarm(any(), any(), anyLong());
    }

    @Test
    public void testClear() {
        var alarm = new Alarm();
        alarm.setStatus(AlarmStatus.ACTIVE_ACK);
        when(alarmSubscriptionService.clearAlarm(any(), any(), any(), anyLong())).thenReturn(Futures.immediateFuture(true));
        service.clear(alarm, new User());

        verify(notificationEntityService, times(1)).notifyCreateOrUpdateAlarm(any(), any(), any());
        verify(alarmSubscriptionService, times(1)).clearAlarm(any(), any(), any(), anyLong());
    }

    @Test
    public void testDelete() {
        service.delete(new Alarm(), new User());

        verify(notificationEntityService, times(1)).notifyDeleteAlarm(any(), any(), any(), any(), any(), any(), anyString());
        verify(alarmSubscriptionService, times(1)).deleteAlarm(any(), any());
    }
}