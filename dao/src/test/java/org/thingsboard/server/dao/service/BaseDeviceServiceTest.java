/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseDeviceServiceTest extends AbstractServiceTest {
    
    private IdComparator<Device> idComparator = new IdComparator<>();
    
    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveDevice() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        
        Assert.assertNotNull(savedDevice);
        Assert.assertNotNull(savedDevice.getId());
        Assert.assertTrue(savedDevice.getCreatedTime() > 0);
        Assert.assertEquals(device.getTenantId(), savedDevice.getTenantId());
        Assert.assertNotNull(savedDevice.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedDevice.getCustomerId().getId());
        Assert.assertEquals(device.getName(), savedDevice.getName());
        
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNotNull(deviceCredentials);
        Assert.assertNotNull(deviceCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, deviceCredentials.getCredentialsType());
        Assert.assertNotNull(deviceCredentials.getCredentialsId());
        Assert.assertEquals(20, deviceCredentials.getCredentialsId().length());
        
        savedDevice.setName("My new device");
        
        deviceService.saveDevice(savedDevice);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
        
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveDeviceWithEmptyName() {
        Device device = new Device();
        device.setType("default");
        device.setTenantId(tenantId);
        deviceService.saveDevice(device);
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveDeviceWithEmptyTenant() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        deviceService.saveDevice(device);
    }
    
    @Test(expected = DataValidationException.class)
    public void testSaveDeviceWithInvalidTenant() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(new TenantId(Uuids.timeBased()));
        deviceService.saveDevice(device);
    }
    
    @Test(expected = DataValidationException.class)
    public void testAssignDeviceToNonExistentCustomer() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        try {
            deviceService.assignDeviceToCustomer(tenantId, device.getId(), new CustomerId(Uuids.timeBased()));
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }
    
    @Test(expected = DataValidationException.class)
    public void testAssignDeviceToCustomerFromDifferentTenant() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        customer = customerService.saveCustomer(customer);
        try {
            deviceService.assignDeviceToCustomer(tenantId, device.getId(), customer.getId());
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }
    
    @Test
    public void testFindDeviceById() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertNotNull(foundDevice);
        Assert.assertEquals(savedDevice, foundDevice);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }

    @Test
    public void testFindDeviceTypesByTenantId() throws Exception {
        List<Device> devices = new ArrayList<>();
        try {
            for (int i=0;i<3;i++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device B"+i);
                device.setType("typeB");
                devices.add(deviceService.saveDevice(device));
            }
            for (int i=0;i<7;i++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device C"+i);
                device.setType("typeC");
                devices.add(deviceService.saveDevice(device));
            }
            for (int i=0;i<9;i++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device A"+i);
                device.setType("typeA");
                devices.add(deviceService.saveDevice(device));
            }
            List<EntitySubtype> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId).get();
            Assert.assertNotNull(deviceTypes);
            Assert.assertEquals(3, deviceTypes.size());
            Assert.assertEquals("typeA", deviceTypes.get(0).getType());
            Assert.assertEquals("typeB", deviceTypes.get(1).getType());
            Assert.assertEquals("typeC", deviceTypes.get(2).getType());
        } finally {
            devices.forEach((device) -> { deviceService.deleteDevice(tenantId, device.getId()); });
        }
    }
    
    @Test
    public void testDeleteDevice() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertNotNull(foundDevice);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
        foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertNull(foundDevice);
        DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNull(foundDeviceCredentials);
    }
    
    @Test
    public void testFindDevicesByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);
        
        TenantId tenantId = tenant.getId();
        
        List<Device> devices = new ArrayList<>();
        for (int i=0;i<178;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device"+i);
            device.setType("default");
            devices.add(deviceService.saveDevice(device));
        }
        
        List<Device> loadedDevices = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantId(tenantId, pageLink);
            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(devices, idComparator);
        Collections.sort(loadedDevices, idComparator);
        
        Assert.assertEquals(devices, loadedDevices);
        
        deviceService.deleteDevicesByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = deviceService.findDevicesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindDevicesByTenantIdAndName() {
        String title1 = "Device title 1";
        List<DeviceInfo> devicesTitle1 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            devicesTitle1.add(new DeviceInfo(deviceService.saveDevice(device), null, false));
        }
        String title2 = "Device title 2";
        List<DeviceInfo> devicesTitle2 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            devicesTitle2.add(new DeviceInfo(deviceService.saveDevice(device), null, false));
        }
        
        List<DeviceInfo> loadedDevicesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<DeviceInfo> pageData = null;
        do {
            pageData = deviceService.findDeviceInfosByTenantId(tenantId, pageLink);
            loadedDevicesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(devicesTitle1, idComparator);
        Collections.sort(loadedDevicesTitle1, idComparator);
        
        Assert.assertEquals(devicesTitle1, loadedDevicesTitle1);
        
        List<DeviceInfo> loadedDevicesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = deviceService.findDeviceInfosByTenantId(tenantId, pageLink);
            loadedDevicesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle2, idComparator);
        Collections.sort(loadedDevicesTitle2, idComparator);
        
        Assert.assertEquals(devicesTitle2, loadedDevicesTitle2);

        for (Device device : loadedDevicesTitle1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }
        
        pageLink = new PageLink(4, 0, title1);
        pageData = deviceService.findDeviceInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        
        for (Device device : loadedDevicesTitle2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }
        
        pageLink = new PageLink(4, 0, title2);
        pageData = deviceService.findDeviceInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindDevicesByTenantIdAndType() {
        String title1 = "Device title 1";
        String type1 = "typeA";
        List<Device> devicesType1 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            devicesType1.add(deviceService.saveDevice(device));
        }
        String title2 = "Device title 2";
        String type2 = "typeB";
        List<Device> devicesType2 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type2);
            devicesType2.add(deviceService.saveDevice(device));
        }

        List<Device> loadedDevicesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type1, pageLink);
            loadedDevicesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType1, idComparator);
        Collections.sort(loadedDevicesType1, idComparator);

        Assert.assertEquals(devicesType1, loadedDevicesType1);

        List<Device> loadedDevicesType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type2, pageLink);
            loadedDevicesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType2, idComparator);
        Collections.sort(loadedDevicesType2, idComparator);

        Assert.assertEquals(devicesType2, loadedDevicesType2);

        for (Device device : loadedDevicesType1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Device device : loadedDevicesType2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
    
    @Test
    public void testFindDevicesByTenantIdAndCustomerId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);
        
        TenantId tenantId = tenant.getId();
        
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();
        
        List<DeviceInfo> devices = new ArrayList<>();
        for (int i=0;i<278;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device"+i);
            device.setType("default");
            device = deviceService.saveDevice(device);
            devices.add(new DeviceInfo(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId), customer.getTitle(), customer.isPublic()));
        }
        
        List<DeviceInfo> loadedDevices = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<DeviceInfo> pageData = null;
        do {
            pageData = deviceService.findDeviceInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(devices, idComparator);
        Collections.sort(loadedDevices, idComparator);
        
        Assert.assertEquals(devices, loadedDevices);
        
        deviceService.unassignCustomerDevices(tenantId, customerId);

        pageLink = new PageLink(33);
        pageData = deviceService.findDeviceInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
        
        tenantService.deleteTenant(tenantId);
    }
    
    @Test
    public void testFindDevicesByTenantIdCustomerIdAndName() {
        
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();
        
        String title1 = "Device title 1";
        List<Device> devicesTitle1 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            device = deviceService.saveDevice(device);
            devicesTitle1.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }
        String title2 = "Device title 2";
        List<Device> devicesTitle2 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            device = deviceService.saveDevice(device);
            devicesTitle2.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }
        
        List<Device> loadedDevicesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDevicesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(devicesTitle1, idComparator);
        Collections.sort(loadedDevicesTitle1, idComparator);
        
        Assert.assertEquals(devicesTitle1, loadedDevicesTitle1);
        
        List<Device> loadedDevicesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDevicesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle2, idComparator);
        Collections.sort(loadedDevicesTitle2, idComparator);
        
        Assert.assertEquals(devicesTitle2, loadedDevicesTitle2);

        for (Device device : loadedDevicesTitle1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }
        
        pageLink = new PageLink(4, 0, title1);
        pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        
        for (Device device : loadedDevicesTitle2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }
        
        pageLink = new PageLink(4, 0, title2);
        pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    @Test
    public void testFindDevicesByTenantIdCustomerIdAndType() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        String title1 = "Device title 1";
        String type1 = "typeC";
        List<Device> devicesType1 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            device = deviceService.saveDevice(device);
            devicesType1.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }
        String title2 = "Device title 2";
        String type2 = "typeD";
        List<Device> devicesType2 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type2);
            device = deviceService.saveDevice(device);
            devicesType2.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }

        List<Device> loadedDevicesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
            loadedDevicesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType1, idComparator);
        Collections.sort(loadedDevicesType1, idComparator);

        Assert.assertEquals(devicesType1, loadedDevicesType1);

        List<Device> loadedDevicesType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
            loadedDevicesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType2, idComparator);
        Collections.sort(loadedDevicesType2, idComparator);

        Assert.assertEquals(devicesType2, loadedDevicesType2);

        for (Device device : loadedDevicesType1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Device device : loadedDevicesType2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

}
