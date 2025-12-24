/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.exception.DataValidationException;

@DaoSqlTest
public class DeviceCredentialsServiceTest extends AbstractServiceTest {

    @Autowired
    DeviceCredentialsService deviceCredentialsService;
    @Autowired
    DeviceService deviceService;
    @MockitoBean
    ApplicationEventPublisher eventPublisher;

    @Test
    public void testCreateDeviceCredentials() {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
        });
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyDevice() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setDeviceId(null);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsType() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setCredentialsType(null);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsId() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setCredentialsId(null);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testSaveNonExistentDeviceCredentials() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        DeviceCredentials newDeviceCredentials = new DeviceCredentials(new DeviceCredentialsId(Uuids.timeBased()));
        newDeviceCredentials.setCreatedTime(deviceCredentials.getCreatedTime());
        newDeviceCredentials.setDeviceId(deviceCredentials.getDeviceId());
        newDeviceCredentials.setCredentialsType(deviceCredentials.getCredentialsType());
        newDeviceCredentials.setCredentialsId(deviceCredentials.getCredentialsId());
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, newDeviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testSaveDeviceCredentialsWithNonExistentDevice() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setDeviceId(new DeviceId(Uuids.timeBased()));
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    @Test
    public void testFindDeviceCredentialsByDeviceId() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceService.deleteDevice(tenantId, savedDevice.getId());
        deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNull(deviceCredentials);
    }

    @Test
    public void testFindDeviceCredentialsByCredentialsId() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(deviceCredentials.getCredentialsId());
        Assert.assertEquals(deviceCredentials, foundDeviceCredentials);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
        foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(deviceCredentials.getCredentialsId());
        Assert.assertNull(foundDeviceCredentials);
    }

    @Test
    public void testSaveDeviceCredentials() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId("access_token");
        deviceCredentials = deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
        DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(deviceCredentials, foundDeviceCredentials);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }

    @Test
    public void testUpdateDeviceCredentialsWithSameValuesDoesNotPublishEvent() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);

        try {
            DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
            Assert.assertNotNull(deviceCredentials);

            DeviceCredentials updatedCredentials = new DeviceCredentials(deviceCredentials.getId());
            updatedCredentials.setDeviceId(deviceCredentials.getDeviceId());
            updatedCredentials.setCredentialsType(deviceCredentials.getCredentialsType());
            updatedCredentials.setCredentialsId(deviceCredentials.getCredentialsId());
            updatedCredentials.setCredentialsValue(deviceCredentials.getCredentialsValue());

            Mockito.reset(eventPublisher);

            DeviceCredentials result = deviceCredentialsService.updateDeviceCredentials(tenantId, updatedCredentials);

            Assert.assertEquals(deviceCredentials.getCredentialsId(), result.getCredentialsId());
            Assert.assertEquals(deviceCredentials.getCredentialsType(), result.getCredentialsType());
            Assert.assertEquals(deviceCredentials.getCredentialsValue(), result.getCredentialsValue());
            Assert.assertEquals(deviceCredentials.getDeviceId(), result.getDeviceId());

            Mockito.verify(eventPublisher, Mockito.never()).publishEvent(Mockito.any(ActionEntityEvent.class));

        } finally {
            deviceService.deleteDevice(tenantId, savedDevice.getId());
        }
    }

}
