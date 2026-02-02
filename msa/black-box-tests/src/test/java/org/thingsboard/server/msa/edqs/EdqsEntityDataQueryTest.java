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
package org.thingsboard.server.msa.edqs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
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
    private CustomerId customerId2;
    private UserId tenantAdminId;
    private UserId customerUserId;
    private UserId tenant2AdminId;
    private UserId customer2UserId;
    private final List<Device> tenantDevices = new ArrayList<>();
    private final List<Device> tenant2Devices = new ArrayList<>();
    private final String deviceProfile = "LoRa-" + RandomStringUtils.randomAlphabetic(10);

    @BeforeClass
    public void beforeClass() throws Exception {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        await().atMost(60, TimeUnit.SECONDS).until(() -> testRestClient.getEdqsState().isApiEnabled());

        tenantId = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant")).getId();
        tenantAdminId = testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId, "tenantAdmin@thingsboard.org"), "tenant");
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfile));
        createDevices(deviceProfile, tenantDevices, 97);
        customerId = testRestClient.postCustomer(defaultCustomer(tenantId, "Customer")).getId();
        customerUserId = testRestClient.postUser(defaultCustomerAdmin(tenantId, customerId,  "customerUser@thingsboard.org")).getId();
        assignDevicesToCustomer(customerId, tenantDevices, 12);

        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        tenantId2 = testRestClient.postTenant(EntityPrototypes.defaultTenantPrototype("Tenant")).getId();
        tenant2AdminId = testRestClient.createUserAndLogin(defaultTenantAdmin(tenantId2, "tenant2Admin@thingsboard.org"), "tenant");
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfile));
        createDevices(deviceProfile, tenant2Devices, 97);
        customerId2 = testRestClient.postCustomer(defaultCustomer(tenantId2, "Customer")).getId();
        customer2UserId = testRestClient.postUser(defaultCustomerAdmin(tenantId2, customerId2,  "customer2User@thingsboard.org")).getId();
        assignDevicesToCustomer(customerId2, tenant2Devices, 12);
    }

    @BeforeMethod
    public void beforeMethod() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
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
                .until(() -> testRestClient.postCountDataQuery(query).compareTo(97L * 2) >= 0);

        testRestClient.getAndSetUserToken(tenantAdminId);
        await("Waiting for total device count")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> testRestClient.postCountDataQuery(query).equals(97L));

        testRestClient.resetToken();
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        testRestClient.getAndSetUserToken(tenant2AdminId);
        await("Waiting for total device count")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> testRestClient.postCountDataQuery(query).equals(97L));
    }

    @Test
    public void testRetrieveTenantDevicesByDeviceTypeFilter() {
        // login tenant admin
        testRestClient.getAndSetUserToken(tenantAdminId);
        checkUserDevices(tenantDevices);

        // login customer user
        testRestClient.getAndSetUserToken(customerUserId);
        checkUserDevices(tenantDevices.subList(0, 12));

        // login other tenant admin
        testRestClient.resetToken();
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
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

    private String createDevices(String deviceType, List<Device> tenantDevices, int deviceCount) throws InterruptedException {
        String prefix = StringUtils.randomAlphabetic(5);
        for (int i = 0; i < deviceCount; i++) {
            Device device = new Device();
            device.setName(prefix + "Device" + i);
            device.setType(deviceType);
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            //TO make sure devices have different created time
            Thread.sleep(1);
            String token = RandomStringUtils.randomAlphabetic(10);
            Device saved = testRestClient.postDevice(token, device);
            tenantDevices.add(saved);

            // save timeseries data
            testRestClient.postTelemetry(token, createDeviceTelemetry(i));
        }
        return deviceType;
    }

    private void assignDevicesToCustomer(CustomerId customerId, List<Device> devices, int deviceCount) {
        for (int i = 0; i < deviceCount; i++) {
            Device device = devices.get(i);
            testRestClient.assignDeviceToCustomer(customerId, device.getId());
        }
    }

    protected ObjectNode createDeviceTelemetry(int temperature) {
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("temperature", temperature);
        return objectNode;
    }

}
