// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.api.ThingsboardApi.AssignDeviceToCustomerArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteDeviceArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerDevicesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDeviceByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDeviceCredentialsByDeviceIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantDevicesArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceWithCredentialsArgs;
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

            Device createdDevice = client.saveDevice(SaveDeviceArgs.builder()
                    .device(device)
                    .build());
            assertNotNull(createdDevice);
            assertNotNull(createdDevice.getId());
            assertEquals(deviceName, createdDevice.getName());

            createdDevices.add(createdDevice);
        }

        // find all, check count
        PageDataDevice allDevices = client.getTenantDevices(GetTenantDevicesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());

        assertNotNull(allDevices);
        assertNotNull(allDevices.getData());
        int initialSize = allDevices.getData().size();
        assertEquals("Expected at least 20 devices, but got " + allDevices.getData().size(), 20, initialSize);

        //find all with search text, check count
        PageDataDevice allDevicesBySearchText = client.getTenantDevices(GetTenantDevicesArgs.builder()
                .pageSize(10)
                .page(0)
                .textSearch(TEST_PREFIX_2)
                .build());
        assertEquals("Expected exactly 10 test devices", 10, allDevicesBySearchText.getData().size());

        // find by id
        Device searchDevice = createdDevices.get(10);
        Device device = client.getDeviceById(GetDeviceByIdArgs.builder()
                .deviceId(searchDevice.getId().getId().toString())
                .build());
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

        Device savedDeviceWithCreds = client.saveDeviceWithCredentials(SaveDeviceWithCredentialsArgs.builder()
                .saveDeviceWithCredentialsRequest(request)
                .build());
        assertEquals("device-with-creds", savedDeviceWithCreds.getName());

        // find credentials by device id
        DeviceCredentials fetchedCreds = client.getDeviceCredentialsByDeviceId(GetDeviceCredentialsByDeviceIdArgs.builder()
                .deviceId(savedDeviceWithCreds.getId().getId().toString())
                .build());
        assertEquals(creds.getCredentialsId(), fetchedCreds.getCredentialsId());

        // delete device
        UUID deviceToDeleteId = createdDevices.get(0).getId().getId();
        client.deleteDevice(DeleteDeviceArgs.builder()
                .deviceId(deviceToDeleteId.toString())
                .build());

        // Verify the device is deleted
        PageDataDevice devicesAfterDelete = client.getTenantDevices(GetTenantDevicesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize, devicesAfterDelete.getData().size());

        assertReturns404(() ->
                client.getDeviceById(GetDeviceByIdArgs.builder()
                        .deviceId(deviceToDeleteId.toString())
                        .build()));

        // assign device to customer
        client.assignDeviceToCustomer(AssignDeviceToCustomerArgs.builder()
                .customerId(savedClientCustomer.getId().getId().toString())
                .deviceId(savedDeviceWithCreds.getId().getId().toString())
                .build());

        // check customer devices
        PageDataDevice pageDataDevice = client.getCustomerDevices(GetCustomerDevicesArgs.builder()
                .customerId(savedClientCustomer.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        List<Device> data = pageDataDevice.getData();
        assertEquals(1, data.size());
        assertEquals(savedDeviceWithCreds.getName(), data.get(0).getName());
    }

}
