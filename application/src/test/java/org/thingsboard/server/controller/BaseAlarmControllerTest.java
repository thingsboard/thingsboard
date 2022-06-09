/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.audit.AuditLogService;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseAlarmControllerTest extends AbstractControllerTest {

    public static final String TEST_ALARM_TYPE = "Test";

    protected Device customerDevice;

    @SpyBean
    private TbClusterService tbClusterService;

    @SpyBean
    private AuditLogService auditLogService;;

    @Before
    public void setup() throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setLabel("Label");
        device.setType("Type");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        logout();
    }

    @After
    public void teardown() throws Exception {
        loginSysAdmin();
        deleteDifferentTenant();
    }

    @Test
    public void testCreateAlarmViaCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.verify(tbClusterService, times(1)).sendNotificationMsgToEdgeService(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.eq(alarm.getId()), Mockito.isNull(), Mockito.isNull(), Mockito.eq(EdgeEventActionType.ADDED));
        Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                Mockito.eq(customerUserId), Mockito.eq(CUSTOMER_USER_EMAIL), Mockito.eq(alarm.getOriginator()),
                Mockito.eq(alarm), Mockito.eq(ActionType.ADDED), Mockito.isNull());
        Mockito.verify(tbClusterService, times(1)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                Mockito.eq(alarm.getOriginator()), Mockito.any(TbMsg.class), Mockito.isNull());

        logout();
    }

    @Test
    public void testCreateAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        createAlarm(TEST_ALARM_TYPE);
        logout();
    }

    @Test
    public void testUpdateAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);

        Mockito.verify(tbClusterService, times(1)).sendNotificationMsgToEdgeService(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.eq(alarm.getId()), Mockito.isNull(), Mockito.isNull(), Mockito.eq(EdgeEventActionType.UPDATED));
        Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                Mockito.eq(customerUserId), Mockito.eq(CUSTOMER_USER_EMAIL), Mockito.eq(alarm.getOriginator()), Mockito.eq(alarm),
                Mockito.eq(ActionType.UPDATED), Mockito.isNull());
        Mockito.verify(tbClusterService, times(1)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                Mockito.eq(alarm.getOriginator()), Mockito.any(TbMsg.class), Mockito.isNull());

        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());
        logout();
    }

    @Test
    public void testUpdateAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());
        logout();
    }

    @Test
    public void testUpdateAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        alarm.setSeverity(AlarmSeverity.MAJOR);
        loginDifferentTenant();
        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
        logout();
    }

    @Test
    public void testUpdateAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentCustomer();
        alarm.setSeverity(AlarmSeverity.MAJOR);
        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
        logout();
    }

    @Test
    public void testDeleteAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());

        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdgeService(Mockito.any(),
                Mockito.any(), Mockito.any(AlarmId.class), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                Mockito.eq(customerUserId), Mockito.eq(CUSTOMER_USER_EMAIL), Mockito.eq(alarm.getOriginator()), Mockito.eq(alarm),
                Mockito.eq(ActionType.DELETED), Mockito.isNull());
        Mockito.verify(tbClusterService, times(1)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                Mockito.eq(alarm.getOriginator()), Mockito.any(TbMsg.class), Mockito.isNull());

        logout();
    }

    @Test
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());
        logout();
    }

    @Test
    public void testDeleteAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());

        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdgeService(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(auditLogService, never()).logEntityAction(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());
        Mockito.verify(tbClusterService,  never()).pushMsgToRuleEngine((TenantId) Mockito.any(),
                Mockito.any(), Mockito.any(),  Mockito.any());

        logout();
    }

    @Test
    public void testDeleteAlarmViaAnotherCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentCustomer();
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());
        logout();
    }

    @Test
    public void testClearAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());

        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);

        Mockito.verify(tbClusterService, times(1)).sendNotificationMsgToEdgeService(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.eq(alarm.getId()), Mockito.isNull(), Mockito.isNull(), Mockito.eq(EdgeEventActionType.ALARM_CLEAR));
        Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                Mockito.eq(customerUserId), Mockito.eq(CUSTOMER_USER_EMAIL), Mockito.eq(foundAlarm.getOriginator()), Mockito.eq(foundAlarm ),
                Mockito.eq(ActionType.ALARM_CLEAR), Mockito.isNull());
        Mockito.verify(tbClusterService, times(1)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                Mockito.eq(alarm.getOriginator()), Mockito.any(TbMsg.class), Mockito.isNull());

        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());
        logout();
    }

    @Test
    public void testClearAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());
        logout();
    }

    @Test
    public void testAcknowledgeAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isOk());

        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);

        Mockito.verify(tbClusterService, times(1)).sendNotificationMsgToEdgeService(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.eq(alarm.getId()), Mockito.isNull(), Mockito.isNull(), Mockito.eq(EdgeEventActionType.ALARM_ACK));
        Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                Mockito.eq(customerUserId), Mockito.eq(CUSTOMER_USER_EMAIL), Mockito.eq(alarm.getOriginator()), Mockito.eq(foundAlarm),
                Mockito.eq(ActionType.ALARM_ACK), Mockito.isNull());
        Mockito.verify(tbClusterService, times(1)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                Mockito.eq(alarm.getOriginator()), Mockito.any(TbMsg.class), Mockito.isNull());

        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.ACTIVE_ACK, foundAlarm.getStatus());
        logout();
    }

    @Test
    public void testClearAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentCustomer();
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isForbidden());
        logout();
    }

    @Test
    public void testClearAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentTenant();
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isForbidden());
        logout();
    }

    @Test
    public void testAcknowledgeAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentCustomer();
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isForbidden());
        logout();
    }

    @Test
    public void testAcknowledgeAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        loginDifferentTenant();
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isForbidden());
        logout();
    }

    private Alarm createAlarm(String type) throws Exception {
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(AlarmSeverity.CRITICAL)
                .type(type)
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        return alarm;
    }
}
