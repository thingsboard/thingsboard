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
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
public abstract class BaseDeviceControllerTest extends AbstractControllerTest {
    static final TypeReference<PageData<Device>> PAGE_DATA_DEVICE_TYPE_REF = new TypeReference<>() {
    };

    ListeningExecutorService executor;

    List<ListenableFuture<Device>> futures;
    PageData<Device> pageData;

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        log.debug("beforeTest");
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));

        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        log.debug("afterTest...");
        executor.shutdownNow();

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        Device savedDevice = doPost("/api/device", device, Device.class);

        Device oldDevice = new Device(savedDevice);
        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedDevice, savedDevice.getId(), savedDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);
        testNotificationUpdateGatewayNever();

        Assert.assertNotNull(savedDevice);
        Assert.assertNotNull(savedDevice.getId());
        Assert.assertTrue(savedDevice.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedDevice.getTenantId());
        Assert.assertNotNull(savedDevice.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedDevice.getCustomerId().getId());
        Assert.assertEquals(device.getName(), savedDevice.getName());

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        Assert.assertNotNull(deviceCredentials);
        Assert.assertNotNull(deviceCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, deviceCredentials.getCredentialsType());
        Assert.assertNotNull(deviceCredentials.getCredentialsId());
        Assert.assertEquals(20, deviceCredentials.getCredentialsId().length());

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        savedDevice.setName("My new device");
        doPost("/api/device", savedDevice, Device.class);

        testNotifyEntityAllOneTime(savedDevice, savedDevice.getId(), savedDevice.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);
        testNotificationUpdateGatewayOneTime(savedDevice, oldDevice);

        Device foundDevice = doGet("/api/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
    }

    @Test
    public void saveDeviceWithViolationOfValidation() throws Exception {
        Device device = new Device();
        device.setName(RandomStringUtils.randomAlphabetic(300));
        device.setType("default");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = "length of name must be equal or less than 255";
        doPost("/api/device", device).andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        testNotificationUpdateGatewayNever();
        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        device.setTenantId(savedTenant.getId());
        msgError = "length of type must be equal or less than 255";
        device.setType(RandomStringUtils.randomAlphabetic(300));
        doPost("/api/device", device).andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        testNotificationUpdateGatewayNever();
        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        msgError = "length of label must be equal or less than 255";
        device.setType("Normal type");
        device.setLabel(RandomStringUtils.randomAlphabetic(300));
        doPost("/api/device", device).andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testUpdateDeviceFromDifferentTenant() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/device", savedDevice, Device.class, status().isNotFound());

        testNotifyEntityNever(savedDevice.getId(), savedDevice);
        testNotificationUpdateGatewayNever();

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doDelete("/api/device/" + savedDevice.getId().getId().toString())
                .andExpect(status().isNotFound());

        testNotifyEntityNever(savedDevice.getId(), savedDevice);
        testNotificationUpdateGatewayNever();

        deleteDifferentTenant();
    }

    @Test
    public void testFindDeviceById() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        Device foundDevice = doGet("/api/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertNotNull(foundDevice);
        Assert.assertEquals(savedDevice, foundDevice);
    }

    @Test
    public void testFindDeviceTypesByTenantId() throws Exception {
        List<Device> devices = new ArrayList<>();

        int cntEntity = 3;

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        for (int i = 0; i < cntEntity; i++) {
            Device device = new Device();
            device.setName("My device B" + i);
            device.setType("typeB");
            devices.add(doPost("/api/device", device, Device.class));
        }

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new Device(), new Device(),
                savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity);
        testNotificationUpdateGatewayNever();

        for (int i = 0; i < 7; i++) {
            Device device = new Device();
            device.setName("My device C" + i);
            device.setType("typeC");
            devices.add(doPost("/api/device", device, Device.class));
        }
        for (int i = 0; i < 9; i++) {
            Device device = new Device();
            device.setName("My device A" + i);
            device.setType("typeA");
            devices.add(doPost("/api/device", device, Device.class));
        }
        List<EntitySubtype> deviceTypes = doGetTyped("/api/device/types",
                new TypeReference<>() {
                });

        Assert.assertNotNull(deviceTypes);
        Assert.assertEquals(3, deviceTypes.size());
        Assert.assertEquals("typeA", deviceTypes.get(0).getType());
        Assert.assertEquals("typeB", deviceTypes.get(1).getType());
        Assert.assertEquals("typeC", deviceTypes.get(2).getType());

        deleteEntitiesAsync("/api/device/", devices, executor).get(TIMEOUT, TimeUnit.SECONDS);
    }

    @Test
    public void testDeleteDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doDelete("/api/device/" + savedDevice.getId().getId())
                .andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedDevice, savedDevice.getId(), savedDevice.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.DELETED, savedDevice.getId().getId().toString());
        testNotificationDeleteGatewayOneTime(savedDevice);

        doGet("/api/device/" + savedDevice.getId().getId())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveDeviceWithEmptyType() throws Exception {
        Device device = new Device();
        device.setName("My device");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        Device savedDevice = doPost("/api/device", device, Device.class);
        Assert.assertEquals("default", savedDevice.getType());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(savedDevice, savedDevice.getId(), savedDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveDeviceWithEmptyName() throws Exception {
        Device device = new Device();
        device.setType("default");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = "Device name should be specified";
        doPost("/api/device", device)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testAssignUnassignDeviceToCustomer() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        Device assignedDevice = doPost("/api/customer/" + savedCustomer.getId().getId()
                + "/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertEquals(savedCustomer.getId(), assignedDevice.getCustomerId());

        testNotifyEntityAllOneTime(assignedDevice, assignedDevice.getId(), assignedDevice.getId(), savedTenant.getId(),
                savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ASSIGNED_TO_CUSTOMER,
                assignedDevice.getId().getId().toString(), savedCustomer.getId().getId().toString(),
                savedCustomer.getTitle());
        testNotificationUpdateGatewayNever();

        Device foundDevice = doGet("/api/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertEquals(savedCustomer.getId(), foundDevice.getCustomerId());

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        Device unassignedDevice =
                doDelete("/api/customer/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, unassignedDevice.getCustomerId().getId());

        testNotifyEntityAllOneTime(unassignedDevice, unassignedDevice.getId(), unassignedDevice.getId(), savedTenant.getId(),
                savedCustomer.getId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UNASSIGNED_FROM_CUSTOMER,
                unassignedDevice.getId().getId().toString(), savedCustomer.getId().getId().toString(),
                savedCustomer.getTitle());
        testNotificationDeleteGatewayNever();

        foundDevice = doGet("/api/device/" + savedDevice.getId().getId(), Device.class);
        Assert.assertEquals(ModelConstants.NULL_UUID, foundDevice.getCustomerId().getId());
    }

    @Test
    public void testAssignDeviceToNonExistentCustomer() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/customer/" + Uuids.timeBased().toString()
                + "/device/" + savedDevice.getId().getId())
                .andExpect(status().isNotFound());

        testNotifyEntityNever(savedDevice.getId(), savedDevice);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testAssignDeviceToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");

        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/customer/" + savedCustomer.getId().getId()
                + "/device/" + savedDevice.getId().getId())
                .andExpect(status().isForbidden());

        testNotifyEntityNever(savedDevice.getId(), savedDevice);
        testNotificationUpdateGatewayNever();

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant2.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindDeviceCredentialsByDeviceId() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
    }

    @Test
    public void testSaveDeviceCredentials() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId("access_token");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isOk());

        testNotifyEntityMsgToEdgePushMsgToCoreOneTime(savedDevice, savedDevice.getId(), savedDevice.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.CREDENTIALS_UPDATED, deviceCredentials);
        testNotificationUpdateGatewayNever();

        DeviceCredentials foundDeviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);

        Assert.assertEquals(deviceCredentials, foundDeviceCredentials);
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyDevice() throws Exception {
        DeviceCredentials deviceCredentials = new DeviceCredentials();

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isBadRequest());

        testNotifyEntityNever(deviceCredentials.getDeviceId(), new Device());
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsType() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        deviceCredentials.setCredentialsType(null);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = "Device credentials type should be specified";
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.CREDENTIALS_UPDATED,
                new DataValidationException(msgError), deviceCredentials);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsId() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        deviceCredentials.setCredentialsId(null);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = "Device credentials id should be specified";
        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.CREDENTIALS_UPDATED,
                new DeviceCredentialsValidationException(msgError), deviceCredentials);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveNonExistentDeviceCredentials() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        DeviceCredentials newDeviceCredentials = new DeviceCredentials(new DeviceCredentialsId(Uuids.timeBased()));
        newDeviceCredentials.setCreatedTime(deviceCredentials.getCreatedTime());
        newDeviceCredentials.setDeviceId(deviceCredentials.getDeviceId());
        newDeviceCredentials.setCredentialsType(deviceCredentials.getCredentialsType());
        newDeviceCredentials.setCredentialsId(deviceCredentials.getCredentialsId());

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        String msgError = "Unable to update non-existent device credentials";
        doPost("/api/device/credentials", newDeviceCredentials)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityIsNullOneTimeError(device, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.CREDENTIALS_UPDATED,
                new DeviceCredentialsValidationException(msgError), newDeviceCredentials);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testSaveDeviceCredentialsWithNonExistentDevice() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + savedDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        deviceCredentials.setDeviceId(new DeviceId(Uuids.timeBased()));

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/device/credentials", deviceCredentials)
                .andExpect(status().isNotFound());

        testNotifyEntityNever(savedDevice.getId(), savedDevice);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testFindTenantDevices() throws Exception {
        log.debug("testFindTenantDevices");
        int cntEntity = 178;

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
        }

        List<Device> devices = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        testNotifyManyEntityManyTimeMsgToEdgeServiceNever(new Device(), new Device(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity);
        testNotificationUpdateGatewayNever();

        List<Device> loadedDevices = new ArrayList<>(cntEntity);
        PageLink pageLink = new PageLink(23);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);

            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devices).containsExactlyInAnyOrderElementsOf(loadedDevices);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        deleteEntitiesAsync("/api/device/", loadedDevices, executor).get(TIMEOUT, TimeUnit.SECONDS);

        testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(new Device(), new Device(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.DELETED, cntEntity, 1);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testFindTenantDevicesByName() throws Exception {
        String title1 = "Device title 1";

        futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
        }
        List<Device> devicesTitle1 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        String title2 = "Device title 2";
        futures = new ArrayList<>(75);
        for (int i = 0; i < 75; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
        }
        List<Device> devicesTitle2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Device> loadedDevicesTitle1 = new ArrayList<>(143);
        PageLink pageLink = new PageLink(15, 0, title1);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);
            loadedDevicesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesTitle1).as(title1).containsExactlyInAnyOrderElementsOf(loadedDevicesTitle1);

        List<Device> loadedDevicesTitle2 = new ArrayList<>(75);
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);
            loadedDevicesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesTitle2).as(title2).containsExactlyInAnyOrderElementsOf(loadedDevicesTitle2);

        deleteEntitiesAsync("/api/device/", loadedDevicesTitle1, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteEntitiesAsync("/api/device/", loadedDevicesTitle2, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/devices?",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindTenantDevicesByType() throws Exception {
        String title1 = "Device title 1";
        String type1 = "typeA";
        futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
            if (i == 0) {
                futures.get(0).get(TIMEOUT, TimeUnit.SECONDS); // wait for the device profile created first time
            }
        }
        List<Device> devicesType1 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        String title2 = "Device title 2";
        String type2 = "typeB";
        futures = new ArrayList<>(75);
        for (int i = 0; i < 75; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type2);
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
            if (i == 0) {
                futures.get(0).get(TIMEOUT, TimeUnit.SECONDS); // wait for the device profile created first time
            }
        }

        List<Device> devicesType2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Device> loadedDevicesType1 = new ArrayList<>(143);
        PageLink pageLink = new PageLink(15);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?type={type}&",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink, type1);
            loadedDevicesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesType1).as(title1).containsExactlyInAnyOrderElementsOf(loadedDevicesType1);

        List<Device> loadedDevicesType2 = new ArrayList<>(75);
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/devices?type={type}&",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink, type2);
            loadedDevicesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesType2).as(title2).containsExactlyInAnyOrderElementsOf(loadedDevicesType2);

        deleteEntitiesAsync("/api/device/", loadedDevicesType1, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/devices?type={type}&",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteEntitiesAsync("/api/device/", loadedDevicesType2, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/devices?type={type}&",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerDevices() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();
        int cntEntity = 128;

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            ListenableFuture<Device> future = executor.submit(() -> doPost("/api/device", device, Device.class));
            futures.add(Futures.transform(future, (dev) ->
                    doPost("/api/customer/" + customerId.getId()
                            + "/device/" + dev.getId().getId(), Device.class), MoreExecutors.directExecutor()));
        }

        List<Device> devices = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Device(), new Device(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, ActionType.ASSIGNED_TO_CUSTOMER, cntEntity, cntEntity, cntEntity * 2);
        testNotificationUpdateGatewayNever();

        List<Device> loadedDevices = new ArrayList<>(cntEntity);
        PageLink pageLink = new PageLink(23);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);
            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devices).containsExactlyInAnyOrderElementsOf(loadedDevices);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        deleteEntitiesAsync("/api/customer/device/", loadedDevices, executor).get(TIMEOUT, TimeUnit.SECONDS);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new Device(), new Device(),
                savedTenant.getId(), customerId, tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UNASSIGNED_FROM_CUSTOMER, cntEntity, cntEntity, 3);
        testNotificationUpdateGatewayNever();
    }

    @Test
    public void testFindCustomerDevicesByName() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Device title 1";
        futures = new ArrayList<>(125);
        for (int i = 0; i < 125; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            ListenableFuture<Device> future = executor.submit(() -> doPost("/api/device", device, Device.class));
            futures.add(Futures.transform(future, (dev) ->
                    doPost("/api/customer/" + customerId.getId()
                            + "/device/" + dev.getId().getId(), Device.class), MoreExecutors.directExecutor()));
        }
        List<Device> devicesTitle1 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        String title2 = "Device title 2";
        futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            ListenableFuture<Device> future = executor.submit(() -> doPost("/api/device", device, Device.class));
            futures.add(Futures.transform(future, (dev) ->
                    doPost("/api/customer/" + customerId.getId()
                            + "/device/" + dev.getId().getId(), Device.class), MoreExecutors.directExecutor()));
        }
        List<Device> devicesTitle2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Device> loadedDevicesTitle1 = new ArrayList<>(125);
        PageLink pageLink = new PageLink(15, 0, title1);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);
            loadedDevicesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesTitle1).as(title1).containsExactlyInAnyOrderElementsOf(loadedDevicesTitle1);

        List<Device> loadedDevicesTitle2 = new ArrayList<>(143);
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/devices?",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink);
            loadedDevicesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesTitle2).as(title2).containsExactlyInAnyOrderElementsOf(loadedDevicesTitle2);

        deleteEntitiesAsync("/api/customer/device/", loadedDevicesTitle1, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/devices?",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteEntitiesAsync("/api/customer/device/", loadedDevicesTitle2, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/devices?",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerDevicesByType() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Device title 1";
        String type1 = "typeC";
        futures = new ArrayList<>(125);
        for (int i = 0; i < 125; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            ListenableFuture<Device> future = executor.submit(() -> doPost("/api/device", device, Device.class));
            futures.add(Futures.transform(future, (dev) ->
                    doPost("/api/customer/" + customerId.getId()
                            + "/device/" + dev.getId().getId(), Device.class), MoreExecutors.directExecutor()));
            if (i == 0) {
                futures.get(0).get(TIMEOUT, TimeUnit.SECONDS); // wait for the device profile created first time
            }
        }
        List<Device> devicesType1 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        String title2 = "Device title 2";
        String type2 = "typeD";
        futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type2);
            ListenableFuture<Device> future = executor.submit(() -> doPost("/api/device", device, Device.class));
            futures.add(Futures.transform(future, (dev) ->
                    doPost("/api/customer/" + customerId.getId()
                            + "/device/" + dev.getId().getId(), Device.class), MoreExecutors.directExecutor()));
            if (i == 0) {
                futures.get(0).get(TIMEOUT, TimeUnit.SECONDS); // wait for the device profile created first time
            }
        }
        List<Device> devicesType2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Device> loadedDevicesType1 = new ArrayList<>(125);
        PageLink pageLink = new PageLink(15);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/devices?type={type}&",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink, type1);
            loadedDevicesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesType1).as(title1).containsExactlyInAnyOrderElementsOf(loadedDevicesType1);

        List<Device> loadedDevicesType2 = new ArrayList<>(143);
        pageLink = new PageLink(4);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/devices?type={type}&",
                    PAGE_DATA_DEVICE_TYPE_REF, pageLink, type2);
            loadedDevicesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(devicesType2).as(title2).containsExactlyInAnyOrderElementsOf(loadedDevicesType2);

        deleteEntitiesAsync("/api/customer/device/", loadedDevicesType1, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/devices?type={type}&",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteEntitiesAsync("/api/customer/device/", loadedDevicesType2, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId() + "/devices?type={type}&",
                PAGE_DATA_DEVICE_TYPE_REF, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testAssignDeviceToTenant() throws Exception {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Device anotherDevice = new Device();
        anotherDevice.setName("My device1");
        anotherDevice.setType("default");
        Device savedAnotherDevice = doPost("/api/device", anotherDevice, Device.class);

        EntityRelation relation = new EntityRelation();
        relation.setFrom(savedDevice.getId());
        relation.setTo(savedAnotherDevice.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType("Contains");
        doPost("/api/relation", relation).andExpect(status().isOk());

        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("Different tenant");
        Tenant savedDifferentTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedDifferentTenant);

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(savedDifferentTenant.getId());
        user.setEmail("tenant9@thingsboard.org");
        user.setFirstName("Sam");
        user.setLastName("Downs");

        createUserAndLogin(user, "testPassword1");

        login("tenant2@thingsboard.org", "testPassword1");

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        Device assignedDevice = doPost("/api/tenant/" + savedDifferentTenant.getId().getId() + "/device/" + savedDevice.getId().getId(), Device.class);

        doGet("/api/device/" + assignedDevice.getId().getId(), Device.class, status().isNotFound());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(assignedDevice, assignedDevice.getId(), assignedDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ASSIGNED_TO_TENANT, savedDifferentTenant.getId().getId().toString(), savedDifferentTenant.getTitle());
        testNotificationUpdateGatewayNever();

        login("tenant9@thingsboard.org", "testPassword1");

        Device foundDevice1 = doGet("/api/device/" + assignedDevice.getId().getId(), Device.class);
        Assert.assertNotNull(foundDevice1);

        doGet("/api/relation?fromId=" + savedDevice.getId().getId() + "&fromType=DEVICE&relationType=Contains&toId=" + savedAnotherDevice.getId().getId() + "&toType=DEVICE", EntityRelation.class, status().isNotFound());

        loginSysAdmin();
        doDelete("/api/tenant/" + savedDifferentTenant.getId().getId())
                .andExpect(status().isOk());
    }

    @Test
    public void testAssignDeviceToEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doPost("/api/edge/" + savedEdge.getId().getId()
                + "/device/" + savedDevice.getId().getId(), Device.class);

         testNotifyEntityAllOneTime(savedDevice, savedDevice.getId(), savedDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ASSIGNED_TO_EDGE,
                savedDevice.getId().getId().toString(), savedEdge.getId().getId().toString(), savedEdge.getName());
        testNotificationUpdateGatewayNever();

        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/devices?",
                PAGE_DATA_DEVICE_TYPE_REF, new PageLink(100));

        Assert.assertEquals(1, pageData.getData().size());

        Mockito.reset(tbClusterService, auditLogService, gatewayNotificationsService);

        doDelete("/api/edge/" + savedEdge.getId().getId()
                + "/device/" + savedDevice.getId().getId(), Device.class);

        testNotifyEntityAllOneTime(savedDevice, savedDevice.getId(), savedDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UNASSIGNED_FROM_EDGE, savedDevice.getId().getId().toString(), savedEdge.getId().getId().toString(), savedEdge.getName());
        testNotificationUpdateGatewayNever();

        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId() + "/devices?",
                PAGE_DATA_DEVICE_TYPE_REF, new PageLink(100));

        Assert.assertEquals(0, pageData.getData().size());
    }
}
