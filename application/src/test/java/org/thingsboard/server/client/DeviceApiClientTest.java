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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.DeviceCredentials;
import org.thingsboard.client.model.DeviceCredentialsType;
import org.thingsboard.client.model.PageDataDevice;
import org.thingsboard.client.model.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@DaoSqlTest
public class DeviceApiClientTest extends AbstractApiClientTest {

    @Test
    public void testDeviceLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<Device> createdDevices = new ArrayList<>();

        // create 20 devices
        for (int i = 0; i < 20; i++) {
            Device device = new Device();
            String deviceName = ((i % 2 == 0) ? TEST_PREFIX : TEST_PREFIX_2) + timestamp + "_" + i;
            device.setName(deviceName);
            device.setLabel("Test Device " + i);
            device.setType(((i % 2 == 0) ? "default" : "thermostat"));

            Device createdDevice = client.saveDevice(device, null, null, null, null);
            assertNotNull(createdDevice);
            assertNotNull(createdDevice.getId());
            assertEquals(deviceName, createdDevice.getName());

            createdDevices.add(createdDevice);
        }

        // find all, check count
        PageDataDevice allDevices = client.getTenantDevices(100, 0, null, null, null, null);

        assertNotNull(allDevices);
        assertNotNull(allDevices.getData());
        int initialSize = allDevices.getData().size();
        assertEquals("Expected at least 20 devices, but got " + allDevices.getData().size(), 20, initialSize);

        //find all with search text, check count
        PageDataDevice allDevicesBySearchText = client.getTenantDevices(10, 0, null, TEST_PREFIX_2, null, null);
        assertEquals("Expected exactly 10 test devices", 10, allDevicesBySearchText.getData().size());

        // find by id
        Device searchDevice = createdDevices.get(10);
        Device device = client.getDeviceById(searchDevice.getId().getId().toString());
        assertEquals(searchDevice.getName(), device.getName());

        // create device with credentials
        Device deviceWithCreds = new Device();
        deviceWithCreds.setName("device-with-creds");

        DeviceCredentials creds = new DeviceCredentials();
        creds.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        creds.setCredentialsId("TEST_ACCESS_TOKEN");

        SaveDeviceWithCredentialsRequest request = new SaveDeviceWithCredentialsRequest();
        request.setDevice(deviceWithCreds);
        request.setCredentials(creds);

        Device savedDeviceWithCreds = client.saveDeviceWithCredentials(request, null, null, null);
        assertEquals("device-with-creds", savedDeviceWithCreds.getName());

        // find credentials by device id
        DeviceCredentials fetchedCreds = client.getDeviceCredentialsByDeviceId(savedDeviceWithCreds.getId().getId().toString());
        assertEquals(creds.getCredentialsId(), fetchedCreds.getCredentialsId());

        // delete device
        UUID deviceToDeleteId = createdDevices.get(0).getId().getId();
        client.deleteDevice(deviceToDeleteId.toString());

        // Verify the device is deleted
        PageDataDevice devicesAfterDelete = client.getTenantDevices(100, 0, null, null, null, null);
        assertEquals(initialSize, devicesAfterDelete.getData().size());

        assertReturns404(() ->
                client.getDeviceById(deviceToDeleteId.toString()));

        // assign device to customer
        client.assignDeviceToCustomer(savedClientCustomer.getId().getId().toString(), savedDeviceWithCreds.getId().getId().toString());

        // check customer devices
        PageDataDevice pageDataDevice = client.getCustomerDevices(savedClientCustomer.getId().getId().toString(), 100, 0, null, null, null, null);
        List<Device> data = pageDataDevice.getData();
        assertEquals(1, data.size());
        assertEquals(savedDeviceWithCreds.getName(), data.get(0).getName());
    }

}
