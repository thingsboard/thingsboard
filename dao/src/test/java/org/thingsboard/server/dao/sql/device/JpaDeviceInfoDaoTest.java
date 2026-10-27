// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.device;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceInfoDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class JpaDeviceInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private DeviceInfoDao deviceInfoDao;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private DeviceProfileDao deviceProfileDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private AttributesDao attributesDao;

    private List<Device> devices = new ArrayList<>();

    private Map<String, DeviceProfileId> savedDeviceProfiles = new HashMap<>();

    @After
    public void tearDown() {
        for (Device device : devices) {
            deviceDao.removeById(device.getTenantId(), device.getUuidId());
        }
        devices.clear();
        for (DeviceProfileId deviceProfileId : savedDeviceProfiles.values()) {
            deviceProfileDao.removeById(TenantId.SYS_TENANT_ID, deviceProfileId.getId());
        }
        savedDeviceProfiles.clear();
    }

    @Test
    public void testFindDeviceInfosByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            devices.add(createDevice(tenantId1, null, i));
            devices.add(createDevice(tenantId2, null, i * 2));
        }

        PageLink pageLink = new PageLink(15, 0, "DEVICE");
        PageData<DeviceInfo> deviceInfos1 = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(new TenantId(tenantId1)).build(), pageLink);
        Assert.assertEquals(15, deviceInfos1.getData().size());

        PageData<DeviceInfo> devicesInfos2 = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(new TenantId(tenantId1)).build(), pageLink.nextPageLink());
        Assert.assertEquals(5, devicesInfos2.getData().size());
    }

    @Test
    public void testFindDeviceInfosByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(),1);

        for (int i = 0; i < 20; i++) {
            devices.add(createDevice(tenantId1, customer1.getUuidId(), i));
            devices.add(createDevice(tenantId1, subCustomer2.getUuidId(), 20 + i * 2));
        }

        PageLink pageLink = new PageLink(30, 0, "DEVICE", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<DeviceInfo> deviceInfos1 = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(customer1.getId()).build(), pageLink);
        Assert.assertEquals(30, deviceInfos1.getData().size());
        deviceInfos1.getData().forEach(deviceInfo -> Assert.assertNotEquals("CUSTOMER_0", deviceInfo.getOwnerName()));

        PageData<DeviceInfo> deviceInfos2 = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(customer1.getId()).build(), pageLink.nextPageLink());
        Assert.assertEquals(10, deviceInfos2.getData().size());

        PageData<DeviceInfo> deviceInfos3 = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(subCustomer2.getId()).build(), pageLink);
        Assert.assertEquals(20, deviceInfos3.getData().size());
    }

    @Test
    public void testFindDeviceInfosByTenantIdAndCustomerIdIncludingSubCustomersThreeLevels() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(), 1);
        Customer subSubCustomer3 = createCustomer(tenantId1, subCustomer2.getUuidId(), 2);

        for (int i = 0; i < 5; i++) {
            devices.add(createDevice(tenantId1, customer1.getUuidId(), i));
            devices.add(createDevice(tenantId1, subCustomer2.getUuidId(), 5 + i));
            devices.add(createDevice(tenantId1, subSubCustomer3.getUuidId(), 10 + i));
        }

        PageLink pageLink = new PageLink(30, 0, "DEVICE", new SortOrder("ownerName", SortOrder.Direction.ASC));

        // From the root: all 15 devices (own + child + grandchild) are included
        PageData<DeviceInfo> fromRoot = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(customer1.getId()).build(), pageLink);
        Assert.assertEquals(15, fromRoot.getData().size());
        fromRoot.getData().forEach(deviceInfo -> Assert.assertNotEquals("CUSTOMER_0", deviceInfo.getOwnerName()));

        // From the middle: own + grandchild only (10)
        PageData<DeviceInfo> fromMiddle = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(subCustomer2.getId()).build(), pageLink);
        Assert.assertEquals(10, fromMiddle.getData().size());

        // From the leaf (no children): only its own 5
        PageData<DeviceInfo> fromLeaf = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(subSubCustomer3.getId()).build(), pageLink);
        Assert.assertEquals(5, fromLeaf.getData().size());
    }

    @Test
    public void testFindDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileIdAndActiveIncludingSubCustomers() throws Exception {
        // Exercises the deviceProfileId and filterByActive predicates of the sub-customer listing, so the
        // Citus rerun (CitusJpaDeviceInfoDaoTest) executes those branches of findDeviceInfosByFilterInCustomerIds.
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(), 1);

        List<Device> ownThermostats = new ArrayList<>();
        List<Device> subCustomerThermostats = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ownThermostats.add(createDevice(tenantId1, customer1.getUuidId(), "thermostat", i));
            subCustomerThermostats.add(createDevice(tenantId1, subCustomer2.getUuidId(), "thermostat", 2 + i));
            devices.add(createDevice(tenantId1, subCustomer2.getUuidId(), "sensor", 4 + i));
        }
        devices.addAll(ownThermostats);
        devices.addAll(subCustomerThermostats);

        // device_info_view sources 'active' from the SERVER_SCOPE 'active' attribute (false when absent)
        for (Device device : subCustomerThermostats) {
            attributesDao.save(TenantId.fromUUID(tenantId1), device.getId(), AttributeScope.SERVER_SCOPE,
                    new BaseAttributeKvEntry(System.currentTimeMillis(), new BooleanDataEntry("active", true))).get(30, TimeUnit.SECONDS);
        }

        DeviceProfileId thermostatProfileId = savedDeviceProfiles.get("thermostat");
        PageLink pageLink = new PageLink(30, 0, "DEVICE", new SortOrder("ownerName", SortOrder.Direction.ASC));

        PageData<DeviceInfo> thermostats = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder()
                .tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(customer1.getId())
                .deviceProfileId(thermostatProfileId).build(), pageLink);
        Assert.assertEquals(4, thermostats.getData().size());
        thermostats.getData().forEach(deviceInfo -> Assert.assertEquals(thermostatProfileId, deviceInfo.getDeviceProfileId()));

        PageData<DeviceInfo> activeThermostats = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder()
                .tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(customer1.getId())
                .deviceProfileId(thermostatProfileId).active(true).build(), pageLink);
        Assert.assertEquals(2, activeThermostats.getData().size());
        activeThermostats.getData().forEach(deviceInfo -> {
            Assert.assertEquals(thermostatProfileId, deviceInfo.getDeviceProfileId());
            Assert.assertTrue(deviceInfo.isActive());
        });

        PageData<DeviceInfo> inactiveDevices = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder()
                .tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(customer1.getId())
                .active(false).build(), pageLink);
        Assert.assertEquals(4, inactiveDevices.getData().size());
        inactiveDevices.getData().forEach(deviceInfo -> Assert.assertFalse(deviceInfo.isActive()));
    }

    @Test
    public void testFindDeviceInfosByTenantIdAndNonExistentCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        // Customer that does not exist (covers the Citus empty-Optional short-circuit, avoiding an IN () query)
        CustomerId nonExistentCustomerId = new CustomerId(Uuids.timeBased());

        for (int i = 0; i < 10; i++) {
            devices.add(createDevice(tenantId1, null, i));
        }

        PageLink pageLink = new PageLink(30, 0, "DEVICE", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<DeviceInfo> deviceInfos = deviceInfoDao.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(new TenantId(tenantId1)).includeCustomers(true).customerId(nonExistentCustomerId).build(), pageLink);
        Assert.assertTrue(deviceInfos.getData().isEmpty());
        Assert.assertEquals(0, deviceInfos.getTotalElements());
    }

    private Device createDevice(UUID tenantId, UUID customerId, int index) {
        return this.createDevice(tenantId, customerId, null, index);
    }

    private Device createDevice(UUID tenantId, UUID customerId, String type, int index) {
        if (type == null) {
            type = "default";
        }
        Device device = new Device();
        device.setId(new DeviceId(Uuids.timeBased()));
        device.setTenantId(TenantId.fromUUID(tenantId));
        device.setCustomerId(new CustomerId(customerId));
        device.setName("DEVICE_" + index);
        device.setType(type);
        device.setDeviceProfileId(deviceProfileId(type));
        return deviceDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, device);
    }

    private Customer createCustomer(UUID tenantId, UUID parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        return customerDao.save(TenantId.fromUUID(tenantId), customer);
    }

    private DeviceProfileId deviceProfileId(String type) {
        DeviceProfileId deviceProfileId = savedDeviceProfiles.get(type);
        if (deviceProfileId == null) {
            DeviceProfile deviceProfile = new DeviceProfile();
            deviceProfile.setName(type);
            deviceProfile.setTenantId(TenantId.SYS_TENANT_ID);
            deviceProfile.setDescription("Test");
            DeviceProfile savedDeviceProfile = deviceProfileDao.save(TenantId.SYS_TENANT_ID, deviceProfile);
            deviceProfileId = savedDeviceProfile.getId();
            savedDeviceProfiles.put(type, deviceProfileId);
        }
        return deviceProfileId;
    }

}
