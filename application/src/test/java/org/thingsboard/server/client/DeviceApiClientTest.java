// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.ApiException;
import org.thingsboard.client.api.ThingsboardApi.AddEntitiesToEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.AssignDeviceToTenantArgs;
import org.thingsboard.client.api.ThingsboardApi.CountByDeviceGroupAndEmptyOtaPackageArgs;
import org.thingsboard.client.api.ThingsboardApi.CountByDeviceProfileAndEmptyOtaPackageArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteDeviceArgs;
import org.thingsboard.client.api.ThingsboardApi.DeleteTenantArgs;
import org.thingsboard.client.api.ThingsboardApi.FindEntityRelationsByQueryArgs;
import org.thingsboard.client.api.ThingsboardApi.GetAllDeviceInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerDeviceInfosArgs;
import org.thingsboard.client.api.ThingsboardApi.GetCustomerDevicesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDeviceByIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDeviceCredentialsByDeviceIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDevicesByEntityGroupIdArgs;
import org.thingsboard.client.api.ThingsboardApi.GetDevicesByIdsArgs;
import org.thingsboard.client.api.ThingsboardApi.GetTenantDevicesArgs;
import org.thingsboard.client.api.ThingsboardApi.GetUserDevicesArgs;
import org.thingsboard.client.api.ThingsboardApi.ProcessDevicesBulkImportArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveAssetArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveDeviceWithCredentialsArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveEntityGroupArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveRelationArgs;
import org.thingsboard.client.api.ThingsboardApi.SaveTenantArgs;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.BulkImportColumnType;
import org.thingsboard.client.model.BulkImportRequest;
import org.thingsboard.client.model.BulkImportResultDevice;
import org.thingsboard.client.model.ColumnMapping;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.DeviceCredentials;
import org.thingsboard.client.model.DeviceCredentialsType;
import org.thingsboard.client.model.EntityGroup;
import org.thingsboard.client.model.EntityGroupInfo;
import org.thingsboard.client.model.EntityRelation;
import org.thingsboard.client.model.EntityRelationsQuery;
import org.thingsboard.client.model.EntitySearchDirection;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.Mapping;
import org.thingsboard.client.model.PageDataDevice;
import org.thingsboard.client.model.PageDataDeviceInfo;
import org.thingsboard.client.model.RelationTypeGroup;
import org.thingsboard.client.model.RelationsSearchParameters;
import org.thingsboard.client.model.SaveDeviceWithCredentialsRequest;
import org.thingsboard.client.model.Tenant;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class DeviceApiClientTest extends AbstractApiClientTest {

    @Test
    public void testDeviceLifecycle() throws Exception {
        long timestamp = System.currentTimeMillis();
        List<Device> createdDevices = new ArrayList<>();

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

        PageDataDevice allDevices = client.getTenantDevices(GetTenantDevicesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(allDevices);
        assertNotNull(allDevices.getData());
        int initialSize = allDevices.getData().size();
        assertEquals("Expected at least 20 devices, but got " + allDevices.getData().size(), 20, initialSize);

        PageDataDevice allDevicesBySearchText = client.getTenantDevices(GetTenantDevicesArgs.builder()
                .pageSize(10)
                .page(0)
                .textSearch(TEST_PREFIX_2)
                .build());
        assertEquals("Expected exactly 10 test devices", 10, allDevicesBySearchText.getData().size());

        Device searchDevice = createdDevices.get(10);
        Device device = client.getDeviceById(GetDeviceByIdArgs.builder()
                .deviceId(searchDevice.getId().getId().toString())
                .build());
        assertEquals(searchDevice.getName(), device.getName());

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

        DeviceCredentials fetchedCreds = client.getDeviceCredentialsByDeviceId(GetDeviceCredentialsByDeviceIdArgs.builder()
                .deviceId(savedDeviceWithCreds.getId().getId().toString())
                .build());
        assertEquals(creds.getCredentialsId(), fetchedCreds.getCredentialsId());

        UUID deviceToDeleteId = createdDevices.get(0).getId().getId();
        client.deleteDevice(DeleteDeviceArgs.builder()
                .deviceId(deviceToDeleteId.toString())
                .build());

        PageDataDevice devicesAfterDelete = client.getTenantDevices(GetTenantDevicesArgs.builder()
                .pageSize(100)
                .page(0)
                .build());
        assertEquals(initialSize, devicesAfterDelete.getData().size());

        assertReturns404(() ->
                client.getDeviceById(GetDeviceByIdArgs.builder()
                        .deviceId(deviceToDeleteId.toString())
                        .build()));
    }

    @Test
    public void testGetCustomerDevices() throws Exception {
        PageDataDevice result = client.getCustomerDevices(GetCustomerDevicesArgs.builder()
                .customerId(savedClientCustomer.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .build());
        assertNotNull(result);
        assertNotNull(result.getData());
        assertTrue("Expected no devices assigned to the test customer", result.getData().isEmpty());
    }

    @Test
    public void testGetUserDevices() throws Exception {
        createNewDevice(TEST_PREFIX + System.currentTimeMillis());

        PageDataDevice result = client.getUserDevices(GetUserDevicesArgs.builder()
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(result);
        assertEquals(1, result.getData().size());
    }

    @Test
    public void testGetAllDeviceInfos() throws Exception {
        createNewDevice(TEST_PREFIX + System.currentTimeMillis());

        PageDataDeviceInfo result = client.getAllDeviceInfos(GetAllDeviceInfosArgs.builder()
                .pageSize(100)
                .page(0)
                .includeCustomers(true)
                .build());
        assertNotNull(result);
        assertEquals(1, result.getData().size());
    }

    @Test
    public void testGetCustomerDeviceInfos() throws Exception {
        client.login(CUSTOMER_USERNAME, TEST_PASSWORD);
        createNewDevice(TEST_PREFIX + System.currentTimeMillis());

        PageDataDeviceInfo result = client.getCustomerDeviceInfos(GetCustomerDeviceInfosArgs.builder()
                .customerId(savedClientCustomer.getId().getId().toString())
                .pageSize(100)
                .page(0)
                .includeCustomers(true)
                .build());
        assertNotNull(result);
        assertEquals(1, result.getData().size());
    }

    @Test
    public void testGetDevicesByIds() throws Exception {
        Device device = createNewDevice(TEST_PREFIX + System.currentTimeMillis());

        List<Device> result = client.getDevicesByIds(GetDevicesByIdsArgs.builder()
                .deviceIds(List.of(device.getId().getId().toString()))
                .build());
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(device.getId().getId(), result.get(0).getId().getId());
    }

    @Test
    public void testFindByQuery() throws Exception {
        Asset building = new Asset();
        building.setName(TEST_PREFIX + "Building");
        building.setType("building");
        building = client.saveAsset(SaveAssetArgs.builder()
                .asset(building)
                .build());

        Device device = new Device();
        device.setName(TEST_PREFIX + "Sensor");
        device.setType("sensor");
        device = client.saveDevice(SaveDeviceArgs.builder()
                .device(device)
                .build());

        EntityRelation buildingToDevice = new EntityRelation();
        buildingToDevice.setFrom(building.getId());
        buildingToDevice.setTo(device.getId());
        buildingToDevice.setType("Contains");
        buildingToDevice.setTypeGroup(RelationTypeGroup.COMMON);
        client.saveRelation(SaveRelationArgs.builder()
                .entityRelation(buildingToDevice)
                .build());

        RelationsSearchParameters params = new RelationsSearchParameters();
        params.setRootId(device.getId().getId());
        params.setRootType(EntityType.DEVICE);
        params.setDirection(EntitySearchDirection.TO);
        params.setMaxLevel(1);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(params);

        List<EntityRelation> result = client.findEntityRelationsByQuery(FindEntityRelationsByQueryArgs.builder()
                .entityRelationsQuery(query)
                .build());
        assertEquals(1, result.size());
    }

    @Test
    public void testGetDevicesByEntityGroupId() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setType(EntityGroup.TypeEnum.DEVICE);
        entityGroup.setName("Test Device Group");
        EntityGroupInfo savedGroup = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(entityGroup)
                .build());
        String groupId = savedGroup.getId().getId().toString();

        Device device = createNewDevice(TEST_PREFIX + System.currentTimeMillis());
        client.addEntitiesToEntityGroup(AddEntitiesToEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .requestBody(List.of(device.getId().getId().toString()))
                .build());

        PageDataDevice result = client.getDevicesByEntityGroupId(GetDevicesByEntityGroupIdArgs.builder()
                .entityGroupId(groupId)
                .pageSize("100")
                .page("0")
                .build());
        assertNotNull(result);
        assertEquals("Expected exactly one device in the entity group", 1, result.getData().size());
        assertEquals(device.getId().getId(), result.getData().get(0).getId().getId());
    }

    @Test
    public void testAssignDeviceToTenant() throws Exception {
        client.login("sysadmin@thingsboard.org", "sysadmin");
        Tenant secondTenant = new Tenant();
        secondTenant.setTitle("Second Test Tenant");
        Tenant savedSecondTenant = client.saveTenant(SaveTenantArgs.builder()
                .tenant(secondTenant)
                .build());

        client.login(TENANT_ADMIN_USERNAME, TEST_PASSWORD);
        Device device = createNewDevice(TEST_PREFIX + System.currentTimeMillis());

        Device assignedDevice = client.assignDeviceToTenant(AssignDeviceToTenantArgs.builder()
                .tenantId(savedSecondTenant.getId().getId().toString())
                .deviceId(device.getId().getId().toString())
                .build());
        assertNotNull(assignedDevice);
        assertEquals(savedSecondTenant.getId().getId(), assignedDevice.getTenantId().getId());

        client.login("sysadmin@thingsboard.org", "sysadmin");
        client.deleteTenant(DeleteTenantArgs.builder()
                .tenantId(savedSecondTenant.getId().getId().toString())
                .build());
    }

    @Test
    public void testCountByDeviceProfileAndEmptyOtaPackage() throws Exception {
        Device device = createNewDevice(TEST_PREFIX + System.currentTimeMillis());
        String deviceProfileId = device.getDeviceProfileId().getId().toString();

        Long count = client.countByDeviceProfileAndEmptyOtaPackage(CountByDeviceProfileAndEmptyOtaPackageArgs.builder()
                .otaPackageType("FIRMWARE")
                .deviceProfileId(deviceProfileId)
                .build());
        assertEquals(Long.valueOf(1), count);
    }

    @Test
    public void testCountByDeviceGroupAndEmptyOtaPackage() throws Exception {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setType(EntityGroup.TypeEnum.DEVICE);
        entityGroup.setName("OTA Test Group");
        EntityGroupInfo savedGroup = client.saveEntityGroup(SaveEntityGroupArgs.builder()
                .entityGroup(entityGroup)
                .build());
        String groupId = savedGroup.getId().getId().toString();

        Device device = createNewDevice(TEST_PREFIX + System.currentTimeMillis());
        client.addEntitiesToEntityGroup(AddEntitiesToEntityGroupArgs.builder()
                .entityGroupId(groupId)
                .requestBody(List.of(device.getId().getId().toString()))
                .build());

        String placeholderOtaPackageId = UUID.randomUUID().toString();
        Long count = client.countByDeviceGroupAndEmptyOtaPackage(CountByDeviceGroupAndEmptyOtaPackageArgs.builder()
                .otaPackageType("FIRMWARE")
                .otaPackageId(placeholderOtaPackageId)
                .entityGroupId(groupId)
                .build());
        assertEquals(Long.valueOf(0), count);
    }

    @Test
    public void testProcessDevicesBulkImport() throws Exception {
        long ts = System.currentTimeMillis();
        String deviceName = TEST_PREFIX + ts + "_bulk";

        String csv = "name,type\n" + deviceName + ",default";

        ColumnMapping nameCol = new ColumnMapping();
        nameCol.setType(BulkImportColumnType.NAME);

        ColumnMapping typeCol = new ColumnMapping();
        typeCol.setType(BulkImportColumnType.TYPE);

        Mapping mapping = new Mapping();
        mapping.setHeader(true);
        mapping.setDelimiter(",");
        mapping.setUpdate(false);
        mapping.setColumns(List.of(nameCol, typeCol));

        BulkImportRequest request = new BulkImportRequest();
        request.setFile(csv);
        request.setMapping(mapping);

        BulkImportResultDevice result = client.processDevicesBulkImport(ProcessDevicesBulkImportArgs.builder()
                .bulkImportRequest(request)
                .build());
        assertNotNull(result);
        assertEquals("Expected one device to be created", 1, ((Number) result.getCreated()).intValue());
        assertEquals("Expected no import errors", 0, ((Number) result.getErrors()).intValue());
    }

    private Device createNewDevice(String name) throws ApiException {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        return client.saveDevice(SaveDeviceArgs.builder()
                .device(device)
                .build());
    }

}
