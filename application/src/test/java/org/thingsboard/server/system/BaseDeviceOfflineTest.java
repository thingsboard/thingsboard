/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.controller.AbstractControllerTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class BaseDeviceOfflineTest  extends AbstractControllerTest {

    private Device deviceA;
    private Device deviceB;
    private DeviceCredentials credA;
    private DeviceCredentials credB;

    @Before
    public void before() throws Exception {
        loginTenantAdmin();
        deviceA = createDevice("DevA", "VMS");
        credA = getCredentials(deviceA.getUuidId());
        deviceB = createDevice("DevB", "SOLAR");
        credB = getCredentials(deviceB.getUuidId());
    }

    @Test
    public void offlineDevicesCanBeFoundByLastConnectField() throws Exception {
        makeDeviceContact(credA);
        Thread.sleep(1000);
        makeDeviceContact(credB);
        Thread.sleep(100);
        List<Device> devices = doGetTyped("/api/device/offline?contactType=CONNECT&threshold=700", new TypeReference<List<Device>>() {
        });

        assertEquals(devices.toString(),1, devices.size());
        assertEquals("DevA", devices.get(0).getName());
    }

    @Test
    public void offlineDevicesCanBeFoundByLastUpdateField() throws Exception {
        makeDeviceUpdate(credA);
        Thread.sleep(1000);
        makeDeviceUpdate(credB);
        makeDeviceContact(credA);
        Thread.sleep(100);
        List<Device> devices = doGetTyped("/api/device/offline?contactType=UPLOAD&threshold=700", new TypeReference<List<Device>>() {
        });

        assertEquals(devices.toString(),1, devices.size());
        assertEquals("DevA", devices.get(0).getName());
    }

    @Test
    public void onlineDevicesCanBeFoundByLastConnectField() throws Exception {
        makeDeviceContact(credB);
        Thread.sleep(1000);
        makeDeviceContact(credA);
        Thread.sleep(100);
        List<Device> devices = doGetTyped("/api/device/online?contactType=CONNECT&threshold=700", new TypeReference<List<Device>>() {
        });

        assertEquals(devices.toString(),1, devices.size());
        assertEquals("DevA", devices.get(0).getName());
    }

    @Test
    public void onlineDevicesCanBeFoundByLastUpdateField() throws Exception {
        makeDeviceUpdate(credB);
        Thread.sleep(1000);
        makeDeviceUpdate(credA);
        makeDeviceContact(credB);
        Thread.sleep(100);
        List<Device> devices = doGetTyped("/api/device/online?contactType=UPLOAD&threshold=700", new TypeReference<List<Device>>() {
        });

        assertEquals(devices.toString(),1, devices.size());
        assertEquals("DevA", devices.get(0).getName());
    }

    private Device createDevice(String name, String type) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        long currentTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10);
        device.setLastConnectTs(currentTime);
        device.setLastUpdateTs(currentTime);
        return doPost("/api/device", device, Device.class);
    }

    private DeviceCredentials getCredentials(UUID deviceId) throws Exception {
        return doGet("/api/device/" + deviceId.toString() + "/credentials", DeviceCredentials.class);
    }

    private void makeDeviceUpdate(DeviceCredentials credentials) throws Exception {
        doPost("/api/v1/" + credentials.getCredentialsId() + "/attributes", ImmutableMap.of("keyA", "valueA"), new String[]{});
    }

    private void makeDeviceContact(DeviceCredentials credentials) throws Exception {
        doGet("/api/v1/" + credentials.getCredentialsId() + "/attributes?clientKeys=keyA,keyB,keyC");
    }
}
