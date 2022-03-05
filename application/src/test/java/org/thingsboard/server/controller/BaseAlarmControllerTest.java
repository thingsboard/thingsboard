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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseAlarmControllerTest extends AbstractControllerTest {

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

    @Test
    public void testCreateAlarm() throws Exception {
        loginCustomerUser();
        createAlarm();
        logout();
    }

    @Test
    public void testUpdateAlarm() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm();
        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());
        logout();
    }

    @Test
    public void testUpdateAlarmViaAnotherCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm();
        loginDifferentCustomer();
        alarm.setSeverity(AlarmSeverity.MAJOR);
        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
        logout();
    }

    @Test
    public void testDeleteAlarm() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm();
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());
        logout();
    }

    @Test
    public void testDeleteAlarmVieAnotherCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm();
        loginDifferentCustomer();
        alarm.setSeverity(AlarmSeverity.MAJOR);
        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isForbidden());
        logout();
    }

    @Test
    public void testClearAlarm() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm();
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());
        logout();
    }

    @Test
    public void testAcknowledgeAlarm() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm();
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/" + alarm.getId(), Alarm.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.ACTIVE_ACK, foundAlarm.getStatus());
        logout();
    }

    @Test
    public void testClearAlarmViaAnotherCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm();
        loginDifferentCustomer();
        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isForbidden());
        logout();
    }

    @Test
    public void testAcknowledgeAlarmViaAnotherCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm();
        loginDifferentCustomer();
        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isForbidden());
        logout();
    }

    private Alarm createAlarm() throws Exception {
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(AlarmSeverity.CRITICAL)
                .type("Test")
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        return alarm;
    }
}
