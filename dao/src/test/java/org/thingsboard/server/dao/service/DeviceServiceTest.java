// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasOtaPackage;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.dao.service.validator.DeviceCredentialsDataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class DeviceServiceTest extends AbstractServiceTest {

    @Autowired
    CustomerService customerService;
    @Autowired
    DeviceCredentialsService deviceCredentialsService;
    @Autowired
    DeviceProfileService deviceProfileService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    OtaPackageService otaPackageService;
    @Autowired
    TenantProfileService tenantProfileService;
    @Autowired
    TbTenantProfileCache tenantProfileCache;
    @Autowired
    private CalculatedFieldService calculatedFieldService;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    @MockitoSpyBean
    private DeviceCredentialsDataValidator validator;
    @Autowired
    private EntityGroupService entityGroupService;
    @Autowired
    private OwnerService ownerService;

    private final IdComparator<Device> idComparator = new IdComparator<>();
    private TenantId anotherTenantId;
    private static ListeningExecutorService executor;

    @BeforeClass
    public static void beforeClass() {
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10, ThingsBoardThreadFactory.forName("DeviceServiceTestScope")));
    }

    @AfterClass
    public static void afterClass() {
        executor.shutdownNow();
    }

    @Before
    public void before() {
        anotherTenantId = createTenant().getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantService.deleteTenant(anotherTenantId);

        tenantProfileService.deleteTenantProfiles(tenantId);
        tenantProfileService.deleteTenantProfiles(anotherTenantId);
    }

    @Test
    public void testSaveDevicesWithoutMaxDeviceLimit() {
        Device device = this.saveDevice(tenantId, "My device");
        deleteDevice(tenantId, device);
    }

    @Test
    public void testSaveDevicesWithInfiniteMaxDeviceLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxDevices(Long.MAX_VALUE).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);
        tenantProfileCache.evict(defaultTenantProfile.getId());

        Device device = this.saveDevice(tenantId, "My device");
        deleteDevice(tenantId, device);
    }

    @Test
    public void testDeviceLimitOnTenantProfileLevel() throws InterruptedException {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxDevices(5l).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);

        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName(StringUtils.randomAlphabetic(10));
                device.setType("default");
                deviceService.saveDevice(device);
            });
        }

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            long countByTenantId = deviceService.countByTenantId(tenantId);
            return countByTenantId == 5;
        });

        Thread.sleep(2000);
        assertThat(deviceService.countByTenantId(tenantId)).isEqualTo(5);
    }

    @Test
    public void testSaveDevicesWithMaxDeviceOutOfLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxDevices(1).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);
        tenantProfileCache.evict(defaultTenantProfile.getId());

        Assert.assertEquals(0, deviceService.countByTenantId(tenantId));

        this.saveDevice(tenantId, "My first device");
        Assert.assertEquals(1, deviceService.countByTenantId(tenantId));

        Assertions.assertThrows(DataValidationException.class, () -> {
            this.saveDevice(tenantId, "My second device that out of maxDeviceCount limit");
        });
    }

    @Test
    public void testSaveDevicesWithTheSameAccessToken() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(StringUtils.randomAlphabetic(10));
        device.setType("default");
        String accessToken = StringUtils.generateSafeToken(10);
        Device savedDevice = deviceService.saveDeviceWithAccessToken(device, accessToken);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(accessToken, deviceCredentials.getCredentialsId());

        Device duplicatedDevice = new Device();
        duplicatedDevice.setTenantId(tenantId);
        duplicatedDevice.setName(StringUtils.randomAlphabetic(10));
        duplicatedDevice.setType("default");
        assertThatThrownBy(() -> deviceService.saveDeviceWithAccessToken(duplicatedDevice, accessToken))
                .isInstanceOf(DeviceCredentialsValidationException.class)
                .hasMessageContaining("Device credentials are already assigned to another device!");

        Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, duplicatedDevice.getName());
        Assertions.assertNull(deviceByName);
    }

    @Test
    public void testShouldRollbackValidatedDeviceIfDeviceCredentialsValidationFailed() {
        Mockito.reset(validator);
        Mockito.doThrow(new DataValidationException("mock message"))
                .when(validator).validate(any(), any());

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(StringUtils.randomAlphabetic(10));
        device.setType("default");

        assertThatThrownBy(() -> deviceService.saveDevice(device))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("mock message");

        Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, device.getName());
        Assertions.assertNull(deviceByName);
    }

    @Test
    public void testCountByTenantId() {
        Assert.assertEquals(0, deviceService.countByTenantId(tenantId));
        Assert.assertEquals(0, deviceService.countByTenantId(anotherTenantId));
        Assert.assertEquals(0, deviceService.countByTenantId(TenantId.SYS_TENANT_ID));

        Device anotherDevice = this.saveDevice(anotherTenantId, "My device 1");
        Assert.assertEquals(1, deviceService.countByTenantId(anotherTenantId));

        int maxDevices = 8;
        List<Device> devices = new ArrayList<>(maxDevices);

        for (int i = 1; i <= maxDevices; i++) {
            devices.add(this.saveDevice(tenantId, "My device " + i));
            Assert.assertEquals(i, deviceService.countByTenantId(tenantId));
        }

        Assert.assertEquals(maxDevices, deviceService.countByTenantId(tenantId));
        Assert.assertEquals(1, deviceService.countByTenantId(anotherTenantId));
        Assert.assertEquals(0, deviceService.countByTenantId(TenantId.SYS_TENANT_ID));

        devices.forEach(device -> deleteDevice(tenantId, device));
        deleteDevice(anotherTenantId, anotherDevice);
    }

    @Test
    public void testCountDevicesWithoutFirmware() {
        testCountDevicesWithoutOta(FIRMWARE);
    }

    @Test
    public void testCountDevicesWithoutSoftware() {
        testCountDevicesWithoutOta(SOFTWARE);
    }

    public void testCountDevicesWithoutOta(OtaPackageType type) {
        var defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        var deviceProfileId = defaultDeviceProfile.getId();
        Assert.assertEquals(0, deviceService.countByTenantId(tenantId));
        Assert.assertEquals(0, deviceService.countByDeviceProfileAndEmptyOtaPackage(tenantId, deviceProfileId, type));

        int maxDevices = 8;
        List<Device> devices = new ArrayList<>(maxDevices);

        for (int i = 1; i <= maxDevices; i++) {
            devices.add(this.saveDevice(tenantId, "My device " + i));
            Assert.assertEquals(i, deviceService.countByDeviceProfileAndEmptyOtaPackage(tenantId, deviceProfileId, type));
        }

        Assert.assertEquals(maxDevices, deviceService.countByDeviceProfileAndEmptyOtaPackage(tenantId, deviceProfileId, type));

        var otaPackageId = createOta(deviceProfileId, type);

        int devicesWithOta = maxDevices / 2;

        for (int i = 0; i < devicesWithOta; i++) {
            var device = devices.get(i);
            setOtaPackageId(device, type, otaPackageId);
            deviceService.saveDevice(device);
        }

        Assert.assertEquals(maxDevices - devicesWithOta, deviceService.countByDeviceProfileAndEmptyOtaPackage(tenantId, deviceProfileId, type));

        devices.forEach(device -> deleteDevice(tenantId, device));
    }

    @Test
    public void testFindDevicesWithoutFirmware() {
        testFindDevicesWithoutOta(FIRMWARE);
    }

    @Test
    public void testFindDevicesWithoutSoftware() {
        testFindDevicesWithoutOta(SOFTWARE);
    }

    public void testFindDevicesWithoutOta(OtaPackageType type) {
        var defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        var deviceProfileId = defaultDeviceProfile.getId();

        PageLink pageLink = new PageLink(100);

        Assert.assertEquals(0, deviceService.countByTenantId(tenantId));
        Assert.assertEquals(0, deviceService.findByDeviceProfileAndEmptyOtaPackage(tenantId, deviceProfileId, type, pageLink).getData().size());

        int maxDevices = 8;
        List<Device> devices = new ArrayList<>(maxDevices);

        for (int i = 1; i <= maxDevices; i++) {
            devices.add(this.saveDevice(tenantId, "My device " + i));
        }

        var foundDevices = deviceService.findByDeviceProfileAndEmptyOtaPackage(tenantId, deviceProfileId, type, pageLink).getData();
        Assert.assertEquals(maxDevices, foundDevices.size());

        devices.sort(idComparator);
        foundDevices.sort(idComparator);

        Assert.assertEquals(devices, foundDevices);

        var otaPackageId = createOta(deviceProfileId, type);

        int devicesWithOta = maxDevices / 2;

        for (int i = 0; i < devicesWithOta; i++) {
            var device = devices.get(i);
            setOtaPackageId(device, type, otaPackageId);
            deviceService.saveDevice(device);
        }

        foundDevices = deviceService.findByDeviceProfileAndEmptyOtaPackage(tenantId, deviceProfileId, type, pageLink).getData();

        Assert.assertEquals(maxDevices - devicesWithOta, foundDevices.size());

        foundDevices.sort(idComparator);

        for (int i = 0; i < foundDevices.size(); i++) {
            Assert.assertEquals(devices.get(i + devicesWithOta), foundDevices.get(i));
        }

        devices.forEach(device -> deleteDevice(tenantId, device));
    }

    private <T extends HasOtaPackage> void setOtaPackageId(T obj, OtaPackageType type, OtaPackageId otaPackageId) {
        switch (type) {
            case FIRMWARE -> obj.setFirmwareId(otaPackageId);
            case SOFTWARE -> obj.setSoftwareId(otaPackageId);
        }
    }

    private OtaPackageId createOta(DeviceProfileId deviceProfileId, OtaPackageType type) {
        OtaPackageInfo ota = new OtaPackageInfo();
        ota.setTenantId(tenantId);
        ota.setDeviceProfileId(deviceProfileId);
        ota.setType(type);
        ota.setTitle("Test_" + type);
        ota.setVersion("v1.0");
        ota.setUrl("http://ota.test.org");
        ota.setDataSize(0L);
        OtaPackageInfo savedOta = otaPackageService.saveOtaPackageInfo(ota, true);
        Assert.assertNotNull(savedOta);
        return savedOta.getId();
    }

    void deleteDevice(TenantId tenantId, Device device) {
        deviceService.deleteDevice(tenantId, device.getId());
    }

    @Test
    public void testFindDeviceIdsByTenantIdAndCustomerId() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("DeviceIds customer " + StringUtils.randomAlphabetic(8));
        CustomerId customerId = customerService.saveCustomer(customer).getId();

        Device tenantDevice1 = saveDevice(tenantId, "Tenant device 1");
        Device tenantDevice2 = saveDevice(tenantId, "Tenant device 2");
        Device customerDevice1 = saveDeviceForCustomer(tenantId, customerId, "Customer device 1");
        Device customerDevice2 = saveDeviceForCustomer(tenantId, customerId, "Customer device 2");

        try {
            // Customer-scoped: only devices assigned to that customer
            List<DeviceId> customerDeviceIds = collectDeviceIds(
                    pageLink -> deviceService.findDeviceIdsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            Assert.assertEquals(Set.of(customerDevice1.getId(), customerDevice2.getId()),
                    new HashSet<>(customerDeviceIds));

            // NULL_UUID customer: only tenant-level devices (those with customer_id = NULL_UUID, as set by DeviceDataValidator)
            List<DeviceId> tenantLevelDeviceIds = collectDeviceIds(
                    pageLink -> deviceService.findDeviceIdsByTenantIdAndCustomerId(tenantId, new CustomerId(NULL_UUID), pageLink));
            Assert.assertEquals(Set.of(tenantDevice1.getId(), tenantDevice2.getId()),
                    new HashSet<>(tenantLevelDeviceIds));
        } finally {
            deleteDevice(tenantId, tenantDevice1);
            deleteDevice(tenantId, tenantDevice2);
            deleteDevice(tenantId, customerDevice1);
            deleteDevice(tenantId, customerDevice2);
            customerService.deleteCustomer(tenantId, customerId);
        }
    }

    @Test
    public void testGetOwnerReturnsNullForMissingDevice() {
        // Race: lifecycle event fires for an entity that has been deleted; getOwner must return null, not NPE.
        DeviceId missingDeviceId = new DeviceId(Uuids.timeBased());
        Assert.assertNull(ownerService.getOwner(tenantId, missingDeviceId));
    }

    private List<DeviceId> collectDeviceIds(Function<PageLink, PageData<DeviceId>> fetch) {
        List<DeviceId> result = new ArrayList<>();
        PageLink pageLink = new PageLink(100);
        PageData<DeviceId> page;
        do {
            page = fetch.apply(pageLink);
            result.addAll(page.getData());
            pageLink = pageLink.nextPageLink();
        } while (page.hasNext());
        return result;
    }

    private Device saveDeviceForCustomer(TenantId tenantId, CustomerId customerId, String name) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCustomerId(customerId);
        device.setName(name);
        device.setType("default");
        return deviceService.saveDevice(device);
    }

    Device saveDevice(TenantId tenantId, final String name) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(name);
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

        savedDevice.setName("New " + savedDevice.getName());

        deviceService.saveDevice(savedDevice);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
        return foundDevice;
    }

    @Test
    public void testSaveDeviceWithFirmware() {
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


        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(device.getDeviceProfileId());
        firmware.setType(FIRMWARE);
        firmware.setTitle("my firmware");
        firmware.setVersion("v1.0");
        firmware.setFileName("test.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{1}));
        firmware.setDataSize(1L);
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        savedDevice.setFirmwareId(savedFirmware.getId());

        deviceService.saveDevice(savedDevice);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
    }

    @Test
    public void testAssignFirmwareToDeviceWithDifferentDeviceProfile() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);

        Assert.assertNotNull(savedDevice);

        DeviceProfile deviceProfile = createDeviceProfile(tenantId, "New device Profile");
        DeviceProfile savedProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedProfile);

        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(savedProfile.getId());
        firmware.setType(FIRMWARE);
        firmware.setTitle("my firmware");
        firmware.setVersion("v1.0");
        firmware.setFileName("test.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{1}));
        firmware.setDataSize(1L);
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        savedDevice.setFirmwareId(savedFirmware.getId());

        assertThatThrownBy(() -> deviceService.saveDevice(savedDevice))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Can't assign firmware with different deviceProfile!");
    }

    @Test
    public void testSaveDeviceWithEmptyName() {
        Device device = new Device();
        device.setType("default");
        device.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceService.saveDevice(device);
        });
    }

    @Test
    public void testSaveDeviceWithEmptyTenant() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceService.saveDevice(device);
        });
    }

    @Test
    public void testSaveDeviceWithNameContains0x00_thenDataValidationException() {
        Device device = new Device();
        device.setType("default");
        device.setTenantId(tenantId);
        device.setName("F0929906\000\000\000\000\000\000\000\000\000");
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceService.saveDevice(device);
        });
    }

    @Test
    public void testSaveDeviceWithJSInjection_thenDataValidationException() {
        Device device = new Device();
        device.setType("default");
        device.setTenantId(tenantId);
        device.setName("{{constructor.constructor('location.href=\"https://evil.com\"')()}}");
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceService.saveDevice(device);
        });
    }

    @Test
    public void testSaveDeviceWithInvalidTenant() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceService.saveDevice(device);
        });
    }

    @Test
    public void testShouldNotPutInCacheRolledbackDeviceProfile() {
        DeviceProfile deviceProfile = createDeviceProfile(tenantId, "New device Profile" + StringUtils.randomAlphabetic(5));


        Device device = new Device();
        device.setType(deviceProfile.getName());
        device.setTenantId(tenantId);
        device.setName("My device" + StringUtils.randomAlphabetic(5));

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = platformTransactionManager.getTransaction(def);
        try {
            deviceProfileService.saveDeviceProfile(deviceProfile);
            deviceService.saveDevice(device);
        } finally {
            platformTransactionManager.rollback(status);
        }
        DeviceProfile deviceProfileByName = deviceProfileService.findDeviceProfileByName(tenantId, deviceProfile.getName());
        Assert.assertNull(deviceProfileByName);
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
            for (int i = 0; i < 3; i++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device B" + i);
                device.setType("typeB");
                devices.add(deviceService.saveDevice(device));
            }
            for (int i = 0; i < 7; i++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device C" + i);
                device.setType("typeC");
                devices.add(deviceService.saveDevice(device));
            }
            for (int i = 0; i < 9; i++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device A" + i);
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
            devices.forEach((device) -> {
                deviceService.deleteDevice(tenantId, device.getId());
            });
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
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
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
    }

    @Test
    public void testFindDevicesByTenantIdAndName() {
        String title1 = "Device title 1";
        List<DeviceInfo> devicesTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            devicesTitle1.add(new DeviceInfo(deviceService.saveDevice(device), null, Collections.emptyList(), false));
        }
        String title2 = "Device title 2";
        List<DeviceInfo> devicesTitle2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            devicesTitle2.add(new DeviceInfo(deviceService.saveDevice(device), null, Collections.emptyList(), false));
        }

        List<DeviceInfo> loadedDevicesTitle1 = new ArrayList<>();

        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<DeviceInfo> pageData = null;
        do {
            pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
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
            pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
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
        pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Device device : loadedDevicesTitle2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindDevicesByTenantIdAndType() {
        String title1 = "Device title 1";
        String type1 = "typeA";
        List<Device> devicesType1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            devicesType1.add(deviceService.saveDevice(device));
        }
        String title2 = "Device title 2";
        String type2 = "typeB";
        List<Device> devicesType2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
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
    public void testCleanCacheIfDeviceRenamed() {
        String deviceNameBeforeRename = StringUtils.randomAlphanumeric(15);
        String deviceNameAfterRename = StringUtils.randomAlphanumeric(15);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(deviceNameBeforeRename);
        device.setType("default");
        deviceService.saveDevice(device);

        Device savedDevice = deviceService.findDeviceByTenantIdAndName(tenantId, deviceNameBeforeRename);

        savedDevice.setName(deviceNameAfterRename);
        deviceService.saveDevice(savedDevice);

        Device renamedDevice = deviceService.findDeviceByTenantIdAndName(tenantId, deviceNameBeforeRename);

        Assert.assertNull("Can't find device by name in cache if it was renamed", renamedDevice);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }

    @Test
    public void testCountDevicesInGroupWithoutFirmware() {
        testCountDevicesInGroupWithoutOta(FIRMWARE);
    }

    @Test
    public void testCountDevicesInGroupWithoutSoftware() {
        testCountDevicesInGroupWithoutOta(SOFTWARE);
    }

    public void testCountDevicesInGroupWithoutOta(OtaPackageType type) {
        var defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        var deviceProfileId = defaultDeviceProfile.getId();
        var entityGroup = entityGroupService.findEntityGroupsByType(tenantId, EntityType.DEVICE, new PageLink(1)).getData().get(0);
        var entityGroupId = entityGroup.getId();

        int maxDevices = 8;
        List<Device> devices = new ArrayList<>(maxDevices);

        for (int i = 1; i <= maxDevices; i++) {
            devices.add(this.saveDevice(tenantId, "My device " + i));
            Assert.assertEquals(i, deviceService.countByDeviceProfileAndEmptyOtaPackage(tenantId, deviceProfileId, type));
        }

        var otaPackageId = createOta(deviceProfileId, type);

        Assert.assertEquals(maxDevices, deviceService.countByEntityGroupAndEmptyOtaPackage(entityGroupId, otaPackageId, type));

        int devicesWithOta = maxDevices / 2;

        for (int i = 0; i < devicesWithOta; i++) {
            var device = devices.get(i);
            setOtaPackageId(device, type, otaPackageId);
            deviceService.saveDevice(device);
        }

        Assert.assertEquals(maxDevices - devicesWithOta, deviceService.countByEntityGroupAndEmptyOtaPackage(entityGroupId, otaPackageId, type));

        devices.forEach(device -> deleteDevice(tenantId, device));
    }

    @Test
    public void testFindDevicesInGroupWithoutFirmware() {
        testFindDevicesInGroupWithoutOta(FIRMWARE);
    }

    @Test
    public void testFindDevicesInGroupWithoutSoftware() {
        testFindDevicesInGroupWithoutOta(SOFTWARE);
    }

    public void testFindDevicesInGroupWithoutOta(OtaPackageType type) {
        var defaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(tenantId);
        var deviceProfileId = defaultDeviceProfile.getId();
        var entityGroup = entityGroupService.findEntityGroupsByType(tenantId, EntityType.DEVICE, new PageLink(1)).getData().get(0);
        var entityGroupId = entityGroup.getId();

        PageLink pageLink = new PageLink(100);

        int maxDevices = 8;
        List<Device> devices = new ArrayList<>(maxDevices);

        for (int i = 1; i <= maxDevices; i++) {
            devices.add(this.saveDevice(tenantId, "My device " + i));
        }

        var foundDevices = deviceService.findByEntityGroupAndDeviceProfileAndEmptyOtaPackage(entityGroupId, deviceProfileId, type, pageLink).getData();
        Assert.assertEquals(maxDevices, foundDevices.size());

        devices.sort(idComparator);
        foundDevices.sort(idComparator);

        Assert.assertEquals(devices, foundDevices);

        var otaPackageId = createOta(deviceProfileId, type);

        int devicesWithOta = maxDevices / 2;

        for (int i = 0; i < devicesWithOta; i++) {
            var device = devices.get(i);
            setOtaPackageId(device, type, otaPackageId);
            deviceService.saveDevice(device);
        }

        foundDevices = deviceService.findByEntityGroupAndDeviceProfileAndEmptyOtaPackage(entityGroupId, deviceProfileId, type, pageLink).getData();

        Assert.assertEquals(maxDevices - devicesWithOta, foundDevices.size());

        foundDevices.sort(idComparator);

        for (int i = 0; i < foundDevices.size(); i++) {
            Assert.assertEquals(devices.get(i + devicesWithOta), foundDevices.get(i));
        }

        devices.forEach(device -> deleteDevice(tenantId, device));
    }

    @Test
    public void testDeleteDeviceIfReferencedInCalculatedField() {
        Device device = saveDevice(tenantId, "Test Device");
        Device deviceWithCf = saveDevice(tenantId, "Device with CF");

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(tenantId);
        calculatedField.setName("Test CF");
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setEntityId(deviceWithCf.getId());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        argument.setRefEntityId(device.getId());
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);

        config.setArguments(Map.of("T", argument));

        config.setExpression("T - (100 - H) / 5");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("output");

        config.setOutput(output);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = calculatedFieldService.save(calculatedField);

        assertThatThrownBy(() -> deviceService.deleteDevice(tenantId, device.getId()))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't delete device that has entity views or is referenced in calculated fields!");

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());
    }

}
