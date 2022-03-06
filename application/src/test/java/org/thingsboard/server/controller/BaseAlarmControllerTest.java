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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        createAlarm(TEST_ALARM_TYPE);
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
        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
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
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());
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
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());
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
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
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
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
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
