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
package org.thingsboard.server.dao.sql.psql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.*;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.service.BaseTenantProfileServiceTest;
import org.thingsboard.server.dao.sqlts.sql.JpaSqlTimeseriesDao;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantProfileDao;
import org.thingsboard.server.dao.timeseries.TimeseriesDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JpaSqlTimeseriesDaoTest extends AbstractJpaDaoTest {

    public static final int DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY = 1;
    public static final int TIMEOUT = 30;

    public static final long TTL = 0;
    public static final String KEY_FOR_CLEANUP = "CLEANUP";
    public static final String EXCLUDED_KEY = "TESTED_TELEMETRY";
    @Autowired
    JpaSqlTimeseriesDao sqlTimeseriesDao;

    @Autowired
    TimeseriesDao timeseriesDao;

    @Autowired
    AttributesDao attributesDao;

    @Autowired
    TenantProfileDao tenantProfileDao;

    @Autowired
    DeviceProfileDao deviceProfileDao;

    @Autowired
    TenantDao tenantDao;

    @Autowired
    CustomerDao customerDao;

    @Autowired
    AssetDao assetDao;

    @Autowired
    DeviceDao deviceDao;

    List<Device> createdDevices = new ArrayList<>();

    List<Asset> createdAssets = new ArrayList<>();

    TenantProfile tenantProfile;

    Customer customer;

    DeviceProfile deviceProfile;

    Tenant tenant;

    @Before
    public void setUp() throws Exception {
        tenantProfile = tenantProfileDao.save(TenantId.SYS_TENANT_ID, BaseTenantProfileServiceTest.createTenantProfile("default tenant profile"));
        tenant = createTenant();

        deviceProfile = deviceProfileDao.save(TenantId.SYS_TENANT_ID, createDeviceProfile(tenant.getId()));
    }

    @After
    public void tearDown() throws Exception {
        createdAssets.forEach(asset -> assetDao.removeById(asset.getTenantId(), asset.getUuidId()));

        createdDevices.forEach(device -> deviceDao.removeById(device.getTenantId(), device.getUuidId()));
        deviceProfileDao.removeById(TenantId.SYS_TENANT_ID, deviceProfile.getUuidId());

        if (customer != null) {
            customerDao.removeById(tenant.getTenantId(), customer.getUuidId());
        }

        tenantDao.removeById(TenantId.SYS_TENANT_ID, tenant.getUuidId());
        tenantProfileDao.removeById(TenantId.SYS_TENANT_ID, tenantProfile.getUuidId());
    }

    @Test
    public void testCleanupTsForTenantsDevice() throws ExecutionException, InterruptedException, TimeoutException {
        Device device = createDevice(tenant.getUuidId(), CustomerId.NULL_UUID, "device");

        saveTimeseries(device.getTenantId(), device.getId());

        long beforeCleanup = checkSavedTs(device.getTenantId(), device.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(device.getTenantId(), device.getId());

        Assert.assertEquals(beforeCleanup, afterCleanup + DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY);

    }

    @Test
    public void testCleanupTsForTenantsDeviceWithCustomTtl() throws ExecutionException, InterruptedException, TimeoutException {
        saveTtlAttribute(tenant.getTenantId(), tenant.getId(), System.currentTimeMillis());

        Device device = createDevice(tenant.getUuidId(), CustomerId.NULL_UUID, "device");

        saveTimeseries(device.getTenantId(), device.getId());

        long beforeCleanup = checkSavedTs(device.getTenantId(), device.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(device.getTenantId(), device.getId());

        Assert.assertEquals(beforeCleanup, afterCleanup);
    }


    @Test
    public void testCleanupTsForTenantAsset() throws ExecutionException, InterruptedException, TimeoutException {
        Asset asset = createAsset(tenant.getUuidId(), CustomerId.NULL_UUID, "asset");

        saveTimeseries(asset.getTenantId(), asset.getId());

        long beforeCleanup = checkSavedTs(asset.getTenantId(), asset.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(asset.getTenantId(), asset.getId());

        Assert.assertEquals(beforeCleanup, afterCleanup + DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY);
    }

    @Test
    public void testCleanupTsForTenantAssetAndDevice() throws ExecutionException, InterruptedException, TimeoutException {
        Device device = createDevice(tenant.getUuidId(), CustomerId.NULL_UUID, "device");
        Asset asset = createAsset(tenant.getUuidId(), CustomerId.NULL_UUID, "asset");

        saveTimeseries(device.getTenantId(), device.getId());
        saveTimeseries(asset.getTenantId(), asset.getId());

        long beforeCleanup = checkSavedTs(device.getTenantId(), device.getId())
                + checkSavedTs(asset.getTenantId(), asset.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(device.getTenantId(), device.getId())
                + checkSavedTs(asset.getTenantId(), asset.getId());

        // multiply DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY to 32 because save timeseries for 2 entity
        Assert.assertEquals(beforeCleanup, afterCleanup + 2 * DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY);
    }

    @Test
    public void testCleanupTsForCustomerDevice() throws ExecutionException, InterruptedException, TimeoutException {
        customer = createCustomer(tenant.getId());

        Device device = createDevice(tenant.getUuidId(), customer.getUuidId(), "device");

        saveTimeseries(device.getTenantId(), device.getId());

        long beforeCleanup = checkSavedTs(device.getTenantId(), device.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(device.getTenantId(), device.getId());

        Assert.assertEquals(beforeCleanup, afterCleanup + DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY);
    }

    @Test
    public void testCleanupTsForCustomerDeviceWithCustomCustomerTtl() throws ExecutionException, InterruptedException, TimeoutException {
        customer = createCustomer(tenant.getId());

        saveTtlAttribute(customer.getTenantId(), customer.getId(), System.currentTimeMillis());

        Device device = createDevice(tenant.getUuidId(), customer.getUuidId(), "device");

        saveTimeseries(device.getTenantId(), device.getId());

        long beforeCleanup = checkSavedTs(device.getTenantId(), device.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(device.getTenantId(), device.getId());

        Assert.assertEquals(beforeCleanup, afterCleanup);
    }

    @Test
    public void testCleanupTsForCustomerDeviceWithCustomTenantTtl() throws ExecutionException, InterruptedException, TimeoutException {
        saveTtlAttribute(tenant.getTenantId(), tenant.getId(), System.currentTimeMillis());

        customer = createCustomer(tenant.getId());

        Device device = createDevice(tenant.getUuidId(), customer.getUuidId(), "device");

        saveTimeseries(device.getTenantId(), device.getId());

        long beforeCleanup = checkSavedTs(device.getTenantId(), device.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(device.getTenantId(), device.getId());

        Assert.assertEquals(beforeCleanup, afterCleanup);
    }

    @Test
    public void testCleanupTsForCustomerDeviceWithCustomTenantAndCustomerTtl() throws ExecutionException, InterruptedException, TimeoutException {
        saveTtlAttribute(tenant.getTenantId(), tenant.getId(), System.currentTimeMillis());

        customer = createCustomer(tenant.getId());
        saveTtlAttribute(customer.getTenantId(), customer.getId(), TTL);

        Device device = createDevice(tenant.getUuidId(), customer.getUuidId(), "device");

        saveTimeseries(device.getTenantId(), device.getId());

        long beforeCleanup = checkSavedTs(device.getTenantId(), device.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(device.getTenantId(), device.getId());

        Assert.assertEquals(beforeCleanup, afterCleanup + DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY);
    }

    @Test
    public void testCleanupTsForCustomerAsset() throws ExecutionException, InterruptedException, TimeoutException {
        customer = createCustomer(tenant.getId());

        Asset asset = createAsset(tenant.getUuidId(), customer.getUuidId(), "asset");

        saveTimeseries(asset.getTenantId(), asset.getId());

        long beforeCleanup = checkSavedTs(asset.getTenantId(), asset.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(asset.getTenantId(), asset.getId());

        Assert.assertEquals(beforeCleanup, afterCleanup + DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY);
    }

    @Test
    public void testCleanupTsForCustomer() throws ExecutionException, InterruptedException, TimeoutException {
        customer = createCustomer(tenant.getId());

        saveTimeseries(customer.getTenantId(), customer.getId());

        long beforeCleanup = checkSavedTs(customer.getTenantId(), customer.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(customer.getTenantId(), customer.getId());

        Assert.assertEquals(beforeCleanup, afterCleanup + DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY);
    }

    @Test
    public void testCleanupTsForCustomerAndAssetAndDevice() throws ExecutionException, InterruptedException, TimeoutException {
        customer = createCustomer(tenant.getId());

        Asset asset = createAsset(tenant.getUuidId(), customer.getUuidId(), "asset");
        Device device = createDevice(tenant.getUuidId(), customer.getUuidId(), "device");

        saveTimeseries(customer.getTenantId(), customer.getId());
        saveTimeseries(device.getTenantId(), device.getId());
        saveTimeseries(asset.getTenantId(), asset.getId());

        long beforeCleanup = checkSavedTs(customer.getTenantId(), customer.getId())
                + checkSavedTs(device.getTenantId(), device.getId())
                + checkSavedTs(asset.getTenantId(), asset.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(customer.getTenantId(), customer.getId())
                + checkSavedTs(device.getTenantId(), device.getId())
                + checkSavedTs(asset.getTenantId(), asset.getId());

        // multiply DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY to 3, because save timeseries for 3 entity
        Assert.assertEquals(beforeCleanup, afterCleanup + 3 * DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY);
    }

    @Test
    public void testCleanupTsForTenantAssetAndDeviceAndForCustomerAndAssetAndDevice() throws ExecutionException, InterruptedException, TimeoutException {
        customer = createCustomer(tenant.getId());

        Device deviceFromTenant = createDevice(tenant.getUuidId(), CustomerId.NULL_UUID, "deviceFromTenant");
        Asset assetFromTenant = createAsset(tenant.getUuidId(), CustomerId.NULL_UUID, "assetFromTenant");

        Device deviceFromCustomer = createDevice(tenant.getUuidId(), customer.getUuidId(), "deviceFromCustomer");
        Asset assetFromCustomer = createAsset(tenant.getUuidId(), customer.getUuidId(), "assetFromCustomer");

        saveTimeseries(deviceFromTenant.getTenantId(), deviceFromTenant.getId());
        saveTimeseries(assetFromTenant.getTenantId(), assetFromTenant.getId());
        saveTimeseries(customer.getTenantId(), customer.getId());
        saveTimeseries(deviceFromCustomer.getTenantId(), deviceFromCustomer.getId());
        saveTimeseries(assetFromCustomer.getTenantId(), assetFromCustomer.getId());

        long beforeCleanup = checkSavedTs(deviceFromTenant.getTenantId(), deviceFromTenant.getId())
                + checkSavedTs(assetFromTenant.getTenantId(), assetFromTenant.getId())
                + checkSavedTs(customer.getTenantId(), customer.getId())
                + checkSavedTs(deviceFromCustomer.getTenantId(), deviceFromCustomer.getId())
                + checkSavedTs(assetFromCustomer.getTenantId(), assetFromCustomer.getId());
        sqlTimeseriesDao.cleanup(TTL, List.of("TESTED_TELEMETRY"));
        long afterCleanup = checkSavedTs(deviceFromTenant.getTenantId(), deviceFromTenant.getId())
                + checkSavedTs(assetFromTenant.getTenantId(), assetFromTenant.getId())
                + checkSavedTs(customer.getTenantId(), customer.getId())
                + checkSavedTs(deviceFromCustomer.getTenantId(), deviceFromCustomer.getId())
                + checkSavedTs(assetFromCustomer.getTenantId(), assetFromCustomer.getId());

        // multiply DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY to 5, because save timeseries for 5 entity
        Assert.assertEquals(beforeCleanup, afterCleanup + 5 * DIFF_AFTER_CLEANUP_FOR_ONE_ENTITY);
    }

    void saveTimeseries(TenantId tenantId, EntityId entityId) throws ExecutionException, InterruptedException, TimeoutException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(EXCLUDED_KEY, "VALUE_FOR_TESTED_TELEMETRY"));
        timeseriesDao.save(tenantId, entityId, tsKvEntry, 0).get(TIMEOUT, TimeUnit.SECONDS);

        TsKvEntry tsKvEntryForCleanup = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(KEY_FOR_CLEANUP, "VALUE_CLEANUP"));
        timeseriesDao.save(tenantId, entityId, tsKvEntryForCleanup, 0).get(TIMEOUT, TimeUnit.SECONDS);
    }

    void saveTtlAttribute(TenantId tenantId, EntityId entityId, long ttl) {
        AttributeKvEntry attributeKvEntry = new BaseAttributeKvEntry(System.currentTimeMillis(), new LongDataEntry("TTL", ttl));
        attributesDao.save(tenantId, entityId, DataConstants.SERVER_SCOPE, attributeKvEntry);
    }

    long checkSavedTs(TenantId tenantId, EntityId entityId) throws InterruptedException, ExecutionException, TimeoutException {
        long startTs = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(TIMEOUT);
        long endTs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT);
        ReadTsKvQuery readTsKvQuery1 = new BaseReadTsKvQuery(EXCLUDED_KEY, startTs, endTs);
        ReadTsKvQuery readTsKvQuery2 = new BaseReadTsKvQuery(KEY_FOR_CLEANUP, startTs, endTs);
        ListenableFuture<List<TsKvEntry>> saved = timeseriesDao.findAllAsync(tenantId, entityId, List.of(readTsKvQuery1, readTsKvQuery2));
        return saved.get(TIMEOUT, TimeUnit.SECONDS).size();
    }

    Tenant createTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TenantId.fromUUID(Uuids.timeBased()));
        tenant.setTitle("TENANT_FOR_TEST");
        tenant.setTenantProfileId(tenantProfile.getId());
        return tenantDao.save(TenantId.SYS_TENANT_ID, tenant);
    }

    Device createDevice(UUID tenantId, UUID customerID, String title) {
        Device device = new Device();
        device.setTenantId(TenantId.fromUUID(tenantId));
        device.setCustomerId(new CustomerId(customerID));
        device.setName("TEST_" + title);
        device.setDeviceProfileId(deviceProfile.getId());
        Device savedDevice = deviceDao.save(TenantId.fromUUID(tenantId), device);
        createdDevices.add(savedDevice);
        return savedDevice;
    }

    DeviceProfile createDeviceProfile(TenantId tenantId) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setName("TEST_DEVICE_PROFILE");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDescription("TEST_TYPE_DEVICE_PROFILE");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        deviceProfileData.setConfiguration(configuration);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(null);
        return deviceProfile;
    }

    Asset createAsset(UUID tenantId, UUID customerId, String suffix) {
        Asset asset = new Asset();
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setCustomerId(new CustomerId(customerId));
        asset.setName("TEST_" + suffix);
        asset.setType("TEST_TYPE" + suffix);
        Asset savedAsset = assetDao.save(TenantId.fromUUID(tenantId), asset);
        createdAssets.add(savedAsset);
        return savedAsset;
    }

    Customer createCustomer(TenantId tenantId) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        customer.setTenantId(tenantId);
        customer.setTitle("CUSTOMER_FOR_TEST");
        return customerDao.save(tenantId, customer);
    }

}
