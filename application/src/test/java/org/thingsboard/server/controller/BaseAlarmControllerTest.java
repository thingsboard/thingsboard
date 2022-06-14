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

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class BaseAlarmControllerTest extends AbstractControllerTest {

    public static final String TEST_ALARM_TYPE = "Test";

    protected Device customerDevice;

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

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);
        logout();
    }

    @Test
    public void testCreateAlarmViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);
        logout();
    }

    @Test
    public void testUpdateAlarmViaCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.UPDATED);
        logout();
    }

    @Test
    public void testUpdateAlarmViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());

        testNotifyEntityOne(updatedAlarm, updatedAlarm.getId(), updatedAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.UPDATED);
        logout();
    }

    @Test
    public void testUpdateAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm", alarm).andExpect(status().isForbidden());

        testNotifyEntityNever(alarm.getId(), alarm);
        logout();
    }

    @Test
    public void testUpdateAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);

        loginDifferentCustomer();
        alarm.setSeverity(AlarmSeverity.MAJOR);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm", alarm).andExpect(status().isForbidden());

        testNotifyEntityNever(alarm.getId(), alarm);
        logout();
    }

    @Test
    public void testDeleteAlarmViaCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());

        testNotifyEntityOneMsgToEdgeServiceNever(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.DELETED);
        logout();
    }

    @Test
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());

        testNotifyEntityOneMsgToEdgeServiceNever(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, tenantAdminCustomerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.DELETED);
        logout();
    }

    @Test
    public void testDeleteAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());

        testNotifyEntityNever(alarm.getId(), alarm);

        logout();
    }

    @Test
    public void testDeleteAlarmViaAnotherCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());

        testNotifyEntityNever(alarm.getId(), alarm);

        logout();
    }

    @Test
    public void testClearAlarmViaCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);

        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());

        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());

        testNotifyEntityOne(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_CLEAR);
        logout();
    }

    @Test
    public void testClearAlarmViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());

        testNotifyEntityOne(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_CLEAR);
        logout();
    }

    @Test
    public void testAcknowledgeAlarmViaCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);

        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isOk());

        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.ACTIVE_ACK, foundAlarm.getStatus());

        testNotifyEntityOne(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_ACK);
        logout();
    }

    @Test
    public void testClearAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isForbidden());

        testNotifyEntityNever(alarm.getId(), alarm);
        logout();
    }

    @Test
    public void testClearAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isForbidden());

        testNotifyEntityNever(alarm.getId(), alarm);
        logout();
    }

    @Test
    public void testAcknowledgeAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isForbidden());

        testNotifyEntityNever(alarm.getId(), alarm);
        logout();
    }

    @Test
    public void testAcknowledgeAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOne(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isForbidden());

        testNotifyEntityNever(alarm.getId(), alarm);
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
