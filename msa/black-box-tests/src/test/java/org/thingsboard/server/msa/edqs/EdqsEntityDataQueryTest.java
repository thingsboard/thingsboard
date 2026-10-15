// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa.edqs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomer;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerAdmin;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultDeviceProfile;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultTenantAdmin;

@DisableUIListeners
public class EdqsEntityDataQueryTest extends AbstractContainerTest {

    private TenantId tenantId;
    private CustomerId customerId;
    private TenantId tenantId2;
    private UserId tenantAdminId;
    private UserId customerAdminId;
    private UserId tenant2AdminId;
    private final List<Device> tenantDevices = new ArrayList<>();
    private final List<Device> customerDevices = new ArrayList<>();
    private final List<Device> tenant2Devices = new ArrayList<>();
    private final String deviceProfile = "LoRa-" + RandomStringUtils.secure().nextAlphabetic(10);

    @BeforeClass
    public void beforeClass() throws Exception {
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> testRestClient.getEdqsState().isApiEnabled());

        tenantId = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant")).getId();
        tenantAdminId = testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId, "tenantAdmin@thingsboard.org"), "tenant");
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfile));
        createDevices(deviceProfile, tenantDevices, 97);
        customerId = testRestClient.postCustomer(defaultCustomer(tenantId, "Customer")).getId();
        EntityGroupInfo customerAdminsGroup = testRestClient.findCustomerAdminsGroup(customerId);
        customerAdminId = testRestClient.postUser(defaultCustomerAdmin(tenantId, customerId,  "customerUser@thingsboard.org"), customerAdminsGroup.getId()).getId();
        createDevices(customerId, deviceProfile, customerDevices, 12);

        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        tenantId2 = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant 2")).getId();
        tenant2AdminId = testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId2, "tenant2Admin@thingsboard.org"), "tenant");
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfile));
        createDevices(deviceProfile, tenant2Devices, 97);
    }

    @BeforeMethod
    public void beforeMethod() {
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        testRestClient.deleteTenant(tenantId);
        testRestClient.deleteTenant(tenantId2);
    }

    @Test
    public void testSysAdminCountEntitiesByQuery() {
        EntityTypeFilter allDeviceFilter = new EntityTypeFilter();
        allDeviceFilter.setEntityType(EntityType.DEVICE);
        EntityCountQuery query = new EntityCountQuery(allDeviceFilter);
        await("Waiting for total device count")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> testRestClient.postCountDataQuery(query).compareTo(97L * 2 + 12) >= 0);

        testRestClient.getAndSetUserToken(tenantAdminId);
        await("Waiting for total device count")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> testRestClient.postCountDataQuery(query).equals(97L + 12L));

        testRestClient.resetToken();
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        testRestClient.getAndSetUserToken(tenant2AdminId);
        await("Waiting for total device count")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> testRestClient.postCountDataQuery(query).equals(97L));
    }

    @Test
    public void testRetrieveTenantDevicesByDeviceTypeFilter() {
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);
        List<Device> allTenantDevices = Stream.concat(tenantDevices.stream(), customerDevices.stream()).toList();
        checkUserDevices(allTenantDevices);

        // login customer user
        testRestClient.getAndSetUserToken(customerAdminId);
        checkUserDevices(customerDevices);

        // login other tenant admin
        testRestClient.resetToken();
        testRestClient.login(SYS_ADMIN_EMAIL, SYS_ADMIN_PASSWORD);
        testRestClient.getAndSetUserToken(tenant2AdminId);
        checkUserDevices(tenant2Devices);
    }

    private void checkUserDevices(List<Device> devices) {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of(deviceProfile));
        filter.setDeviceNameFilter("");

        EntityDataSortOrder sortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        List<EntityKey> latestFields = Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestFields, null);

        EntityTypeFilter allDeviceFilter = new EntityTypeFilter();
        allDeviceFilter.setEntityType(EntityType.DEVICE);
        EntityCountQuery countQuery = new EntityCountQuery(allDeviceFilter);
        await("Waiting for total device count")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> testRestClient.postCountDataQuery(countQuery).intValue() == devices.size());

        PageData<EntityData> result = testRestClient.postEntityDataQuery(query);
        assertThat(result.getTotalElements()).isEqualTo(devices.size());
        List<EntityData> retrievedDevices = result.getData();

        assertThat(retrievedDevices).hasSize(10);
        List<String> retrievedDeviceNames = retrievedDevices.stream().map(entityData -> entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).toList();
        assertThat(retrievedDeviceNames).containsExactlyInAnyOrderElementsOf(devices.stream().map(Device::getName).toList().subList(0, 10));

        //check temperature
        for (int i = 0; i < 10; i++) {
            Map<EntityKeyType, Map<String, TsValue>> latest = retrievedDevices.get(i).getLatest();
            String name = latest.get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
            assertThat(latest.get(EntityKeyType.TIME_SERIES).get("temperature").getValue()).isEqualTo(name.substring(name.length() - 1));
        }
    }

    private void createDevices(CustomerId customerId, String deviceType, List<Device> tenantDevices, int deviceCount) throws InterruptedException {
        String prefix = StringUtils.randomAlphabetic(5);
        for (int i = 0; i < deviceCount; i++) {
            Device device = new Device();
            device.setName(prefix + "Device" + i);
            device.setCustomerId(customerId);
            device.setType(deviceType);
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            //TO make sure devices have different created time
            Thread.sleep(1);
            String token = RandomStringUtils.secure().nextAlphabetic(10);
            Device saved = testRestClient.postDevice(token, device);
            tenantDevices.add(saved);

            // save timeseries data
            testRestClient.postTelemetry(token, createDeviceTelemetry(i));
        }
    }

    private void createDevices(String deviceType, List<Device> tenantDevices, int deviceCount) throws InterruptedException {
        createDevices(null, deviceType, tenantDevices, deviceCount);
    }

    protected ObjectNode createDeviceTelemetry(int temperature) {
        ObjectNode objectNode = JacksonUtil.newObjectNode();
        objectNode.put("temperature", temperature);
        return objectNode;
    }

}
