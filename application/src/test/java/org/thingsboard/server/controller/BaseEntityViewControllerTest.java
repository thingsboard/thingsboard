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
package org.thingsboard.server.controller;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Slf4j
public abstract class BaseEntityViewControllerTest extends AbstractControllerTest {

    private IdComparator<EntityView> idComparator;
    private Tenant savedTenant;
    private User tenantAdmin;
    private Device testDevice;
    private TelemetryEntityView telemetry;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();
        idComparator = new IdComparator<>();

        savedTenant = doPost("/api/tenant", getNewTenant("My tenant"), Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        Device device = new Device();
        device.setName("Test device");
        device.setType("default");
        testDevice = doPost("/api/device", device, Device.class);

        telemetry = new TelemetryEntityView(
                Arrays.asList("tsKey1", "tsKey2", "tsKey3"),
                new AttributesEntityView(
                        Arrays.asList("caKey1", "caKey2", "caKey3", "caKey4"),
                        Arrays.asList("saKey1", "saKey2", "saKey3", "saKey4"),
                        Arrays.asList("shKey1", "shKey2", "shKey3", "shKey4")));
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindEntityViewById() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");
        EntityView foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        Assert.assertNotNull(foundView);
        assertEquals(savedView, foundView);
    }

    @Test
    public void testSaveEntityView() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");

        Assert.assertNotNull(savedView);
        Assert.assertNotNull(savedView.getId());
        Assert.assertTrue(savedView.getCreatedTime() > 0);
        assertEquals(savedTenant.getId(), savedView.getTenantId());
        Assert.assertNotNull(savedView.getCustomerId());
        assertEquals(NULL_UUID, savedView.getCustomerId().getId());
        assertEquals(savedView.getName(), savedView.getName());

        savedView.setName("New test entity view");
        doPost("/api/entityView", savedView, EntityView.class);
        EntityView foundEntityView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);

        assertEquals(foundEntityView.getName(), savedView.getName());
        assertEquals(foundEntityView.getKeys(), telemetry);
    }


    @Test
    public void testUpdateEntityViewFromDifferentTenant() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");
        loginDifferentTenant();
        doPost("/api/entityView", savedView, EntityView.class, status().isForbidden());
        deleteDifferentTenant();
    }

    @Test
    public void testDeleteEntityView() throws Exception {
        EntityView view = getNewSavedEntityView("Test entity view");
        Customer customer = doPost("/api/customer", getNewCustomer("My customer"), Customer.class);
        view.setCustomerId(customer.getId());
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        doDelete("/api/entityView/" + savedView.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/entityView/" + savedView.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveEntityViewWithEmptyName() throws Exception {
        EntityView entityView = new EntityView();
        entityView.setType("default");
        doPost("/api/entityView", entityView)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Entity view name should be specified!")));
    }

    @Test
    public void testAssignAndUnAssignedEntityViewToCustomer() throws Exception {
        EntityView view = getNewSavedEntityView("Test entity view");
        Customer savedCustomer = doPost("/api/customer", getNewCustomer("My customer"), Customer.class);
        view.setCustomerId(savedCustomer.getId());
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        EntityView assignedView = doPost(
                "/api/customer/" + savedCustomer.getId().getId().toString() + "/entityView/" + savedView.getId().getId().toString(),
                EntityView.class);
        assertEquals(savedCustomer.getId(), assignedView.getCustomerId());

        EntityView foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        assertEquals(savedCustomer.getId(), foundView.getCustomerId());

        EntityView unAssignedView = doDelete("/api/customer/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        assertEquals(ModelConstants.NULL_UUID, unAssignedView.getCustomerId().getId());

        foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        assertEquals(ModelConstants.NULL_UUID, foundView.getCustomerId().getId());
    }

    @Test
    public void testAssignEntityViewToNonExistentCustomer() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");
        doPost("/api/customer/" + UUIDs.timeBased().toString() + "/device/" + savedView.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAssignEntityViewToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = getNewTenant("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");
        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = getNewCustomer("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        EntityView savedView = getNewSavedEntityView("Test entity view");

        doPost("/api/customer/" + savedCustomer.getId().getId().toString() + "/entityView/" + savedView.getId().getId().toString())
                .andExpect(status().isForbidden());

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testGetCustomerEntityViews() throws Exception {
        CustomerId customerId = doPost("/api/customer", getNewCustomer("Test customer"), Customer.class).getId();
        String urlTemplate = "/api/customer/" + customerId.getId().toString() + "/entityViews?";

        List<EntityView> views = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            views.add(doPost("/api/customer/" + customerId.getId().toString() + "/entityView/"
                    + getNewSavedEntityView("Test entity view " + i).getId().getId().toString(), EntityView.class));
        }

        List<EntityView> loadedViews = loadListOf(new TextPageLink(23), urlTemplate);

        Collections.sort(views, idComparator);
        Collections.sort(loadedViews, idComparator);

        assertEquals(views, loadedViews);
    }

    @Test
    public void testGetCustomerEntityViewsByName() throws Exception {
        CustomerId customerId = doPost("/api/customer", getNewCustomer("Test customer"), Customer.class).getId();
        String urlTemplate = "/api/customer/" + customerId.getId().toString() + "/entityViews?";

        String name1 = "Entity view name1";
        List<EntityView> namesOfView1 = fillListOf(125, name1, "/api/customer/" + customerId.getId().toString()
                + "/entityView/");
        List<EntityView> loadedNamesOfView1 = loadListOf(new TextPageLink(15, name1), urlTemplate);
        Collections.sort(namesOfView1, idComparator);
        Collections.sort(loadedNamesOfView1, idComparator);
        assertEquals(namesOfView1, loadedNamesOfView1);

        String name2 = "Entity view name2";
        List<EntityView> NamesOfView2 = fillListOf(143, name2, "/api/customer/" + customerId.getId().toString()
                + "/entityView/");
        List<EntityView> loadedNamesOfView2 = loadListOf(new TextPageLink(4, name2), urlTemplate);
        Collections.sort(NamesOfView2, idComparator);
        Collections.sort(loadedNamesOfView2, idComparator);
        assertEquals(NamesOfView2, loadedNamesOfView2);

        for (EntityView view : loadedNamesOfView1) {
            doDelete("/api/customer/entityView/" + view.getId().getId().toString()).andExpect(status().isOk());
        }
        TextPageData<EntityView> pageData = doGetTypedWithPageLink(urlTemplate,
                new TypeReference<TextPageData<EntityView>>() {
                }, new TextPageLink(4, name1));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());

        for (EntityView view : loadedNamesOfView2) {
            doDelete("/api/customer/entityView/" + view.getId().getId().toString()).andExpect(status().isOk());
        }
        pageData = doGetTypedWithPageLink(urlTemplate, new TypeReference<TextPageData<EntityView>>() {
                },
                new TextPageLink(4, name2));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testGetTenantEntityViews() throws Exception {

        List<EntityView> views = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            views.add(getNewSavedEntityView("Test entity view" + i));
        }
        List<EntityView> loadedViews = loadListOf(new TextPageLink(23), "/api/tenant/entityViews?");

        Collections.sort(views, idComparator);
        Collections.sort(loadedViews, idComparator);

        assertEquals(views, loadedViews);
    }

    @Test
    public void testGetTenantEntityViewsByName() throws Exception {
        String name1 = "Entity view name1";
        List<EntityView> namesOfView1 = fillListOf(143, name1);
        List<EntityView> loadedNamesOfView1 = loadListOf(new TextPageLink(15, name1), "/api/tenant/entityViews?");
        Collections.sort(namesOfView1, idComparator);
        Collections.sort(loadedNamesOfView1, idComparator);
        assertEquals(namesOfView1, loadedNamesOfView1);

        String name2 = "Entity view name2";
        List<EntityView> NamesOfView2 = fillListOf(75, name2);
        List<EntityView> loadedNamesOfView2 = loadListOf(new TextPageLink(4, name2), "/api/tenant/entityViews?");
        Collections.sort(NamesOfView2, idComparator);
        Collections.sort(loadedNamesOfView2, idComparator);
        assertEquals(NamesOfView2, loadedNamesOfView2);

        for (EntityView view : loadedNamesOfView1) {
            doDelete("/api/entityView/" + view.getId().getId().toString()).andExpect(status().isOk());
        }
        TextPageData<EntityView> pageData = doGetTypedWithPageLink("/api/tenant/entityViews?",
                new TypeReference<TextPageData<EntityView>>() {
                }, new TextPageLink(4, name1));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());

        for (EntityView view : loadedNamesOfView2) {
            doDelete("/api/entityView/" + view.getId().getId().toString()).andExpect(status().isOk());
        }
        pageData = doGetTypedWithPageLink("/api/tenant/entityViews?", new TypeReference<TextPageData<EntityView>>() {
                },
                new TextPageLink(4, name2));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testTheCopyOfAttrsIntoTSForTheView() throws Exception {
        Set<String> actualAttributesSet =
                getAttributesByKeys("{\"caKey1\":\"value1\", \"caKey2\":true, \"caKey3\":42.0, \"caKey4\":73}");

        Set<String> expectedActualAttributesSet =
                new HashSet<>(Arrays.asList("caKey1", "caKey2", "caKey3", "caKey4"));
        assertTrue(actualAttributesSet.containsAll(expectedActualAttributesSet));

        EntityView savedView = getNewSavedEntityView("Test entity view");

        Thread.sleep(1000);

        List<Map<String, Object>> values = doGetAsync("/api/plugins/telemetry/ENTITY_VIEW/" + savedView.getId().getId().toString() +
                "/values/attributes?keys=" + String.join(",", actualAttributesSet), List.class);

        assertEquals("value1", getValue(values, "caKey1"));
        assertEquals(true, getValue(values, "caKey2"));
        assertEquals(42.0, getValue(values, "caKey3"));
        assertEquals(73, getValue(values, "caKey4"));
    }

    @Test
    public void testTheCopyOfAttrsOutOfTSForTheView() throws Exception {
        Set<String> actualAttributesSet =
                getAttributesByKeys("{\"caKey1\":\"value1\", \"caKey2\":true, \"caKey3\":42.0, \"caKey4\":73}");

        Set<String> expectedActualAttributesSet = new HashSet<>(Arrays.asList("caKey1", "caKey2", "caKey3", "caKey4"));
        assertTrue(actualAttributesSet.containsAll(expectedActualAttributesSet));

        List<Map<String, Object>> valueTelemetryOfDevices = doGetAsync("/api/plugins/telemetry/DEVICE/" + testDevice.getId().getId().toString() +
                "/values/attributes?keys=" + String.join(",", actualAttributesSet), List.class);

        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setTenantId(savedTenant.getId());
        view.setName("Test entity view");
        view.setType("default");
        view.setKeys(telemetry);
        view.setStartTimeMs((long) getValue(valueTelemetryOfDevices, "lastActivityTime") * 10);
        view.setEndTimeMs((long) getValue(valueTelemetryOfDevices, "lastActivityTime") / 10);
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        Thread.sleep(1000);

        List<Map<String, Object>> values = doGetAsync("/api/plugins/telemetry/ENTITY_VIEW/" + savedView.getId().getId().toString() +
                "/values/attributes?keys=" + String.join(",", actualAttributesSet), List.class);
        assertEquals(0, values.size());
    }


    @Test
    public void testGetTelemetryWhenEntityViewTimeRangeInsideTimestampRange() throws Exception {
        uploadTelemetry("{\"tsKey1\":\"value1\", \"tsKey2\":true, \"tsKey3\":40.0}");
        Thread.sleep(1000);
        long startTimeMs = System.currentTimeMillis();
        uploadTelemetry("{\"tsKey1\":\"value2\", \"tsKey2\":false, \"tsKey3\":80.0}");
        Thread.sleep(1000);
        uploadTelemetry("{\"tsKey1\":\"value3\", \"tsKey2\":false, \"tsKey3\":120.0}");
        long endTimeMs = System.currentTimeMillis();
        uploadTelemetry("{\"tsKey1\":\"value4\", \"tsKey2\":true, \"tsKey3\":160.0}");

        String deviceId = testDevice.getId().getId().toString();
        Set<String> keys = getTelemetryKeys("DEVICE", deviceId);
        Thread.sleep(1000);

        EntityView view = createEntityView("Test entity view", startTimeMs, endTimeMs);
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);
        String entityViewId = savedView.getId().getId().toString();

        Map<String, List<Map<String, String>>> expectedValues = getTelemetryValues("DEVICE", deviceId, keys, 0L, (startTimeMs + endTimeMs) / 2);
        Assert.assertEquals(2, expectedValues.get("tsKey1").size());
        Assert.assertEquals(2, expectedValues.get("tsKey2").size());
        Assert.assertEquals(2, expectedValues.get("tsKey3").size());

        Map<String, List<Map<String, String>>> actualValues = getTelemetryValues("ENTITY_VIEW", entityViewId, keys, 0L, (startTimeMs + endTimeMs) / 2);
        Assert.assertEquals(1, actualValues.get("tsKey1").size());
        Assert.assertEquals(1, actualValues.get("tsKey2").size());
        Assert.assertEquals(1, actualValues.get("tsKey3").size());
    }

    private void uploadTelemetry(String strKvs) throws Exception {
        String viewDeviceId = testDevice.getId().getId().toString();
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + viewDeviceId + "/credentials", DeviceCredentials.class);
        assertEquals(testDevice.getId(), deviceCredentials.getDeviceId());

        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        awaitConnected(client, TimeUnit.SECONDS.toMillis(30));
        MqttMessage message = new MqttMessage();
        message.setPayload(strKvs.getBytes());
        client.publish("v1/devices/me/telemetry", message);
        Thread.sleep(1000);
        client.disconnect();
    }

    private void awaitConnected(MqttAsyncClient client, long ms) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!client.isConnected()) {
            Thread.sleep(100);
            if (start + ms < System.currentTimeMillis()) {
                throw new RuntimeException("Client is not connected!");
            }
        }
    }

    private Set<String> getTelemetryKeys(String type, String id) throws Exception {
        return new HashSet<>(doGetAsync("/api/plugins/telemetry/" + type + "/" + id + "/keys/timeseries", List.class));
    }

    private Map<String, List<Map<String, String>>> getTelemetryValues(String type, String id, Set<String> keys, Long startTs, Long endTs) throws Exception {
        return doGetAsync("/api/plugins/telemetry/" + type + "/" + id +
                "/values/timeseries?keys=" + String.join(",", keys) + "&startTs=" + startTs + "&endTs=" + endTs, Map.class);
    }

    private Set<String> getAttributesByKeys(String stringKV) throws Exception {
        String viewDeviceId = testDevice.getId().getId().toString();
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + viewDeviceId + "/credentials", DeviceCredentials.class);
        assertEquals(testDevice.getId(), deviceCredentials.getDeviceId());

        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        awaitConnected(client, TimeUnit.SECONDS.toMillis(30));

        MqttMessage message = new MqttMessage();
        message.setPayload((stringKV).getBytes());
        client.publish("v1/devices/me/attributes", message);
        Thread.sleep(1000);
        client.disconnect();
        return new HashSet<>(doGetAsync("/api/plugins/telemetry/DEVICE/" + viewDeviceId + "/keys/attributes", List.class));
    }

    private Object getValue(List<Map<String, Object>> values, String stringValue) {
        return values.size() == 0 ? null :
                values.stream()
                        .filter(value -> value.get("key").equals(stringValue))
                        .findFirst().get().get("value");
    }

    private EntityView getNewSavedEntityView(String name) throws Exception {
        EntityView view = createEntityView(name, 0, 0);
        return doPost("/api/entityView", view, EntityView.class);
    }

    private EntityView createEntityView(String name, long startTimeMs, long endTimeMs) {
        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setTenantId(savedTenant.getId());
        view.setName(name);
        view.setType("default");
        view.setKeys(telemetry);
        view.setStartTimeMs(startTimeMs);
        view.setEndTimeMs(endTimeMs);
        return view;
    }

    private Customer getNewCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return customer;
    }

    private Tenant getNewTenant(String title) {
        Tenant tenant = new Tenant();
        tenant.setTitle(title);
        return tenant;
    }

    private List<EntityView> fillListOf(int limit, String partOfName, String urlTemplate) throws Exception {
        List<EntityView> views = new ArrayList<>();
        for (EntityView view : fillListOf(limit, partOfName)) {
            views.add(doPost(urlTemplate + view.getId().getId().toString(), EntityView.class));
        }
        return views;
    }

    private List<EntityView> fillListOf(int limit, String partOfName) throws Exception {
        List<EntityView> viewNames = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String fullName = partOfName + ' ' + RandomStringUtils.randomAlphanumeric(15);
            fullName = i % 2 == 0 ? fullName.toLowerCase() : fullName.toUpperCase();
            EntityView view = getNewSavedEntityView(fullName);
            Customer customer = getNewCustomer("Test customer " + String.valueOf(Math.random()));
            view.setCustomerId(doPost("/api/customer", customer, Customer.class).getId());
            viewNames.add(doPost("/api/entityView", view, EntityView.class));
        }
        return viewNames;
    }

    private List<EntityView> loadListOf(TextPageLink pageLink, String urlTemplate) throws Exception {
        List<EntityView> loadedItems = new ArrayList<>();
        TextPageData<EntityView> pageData;
        do {
            pageData = doGetTypedWithPageLink(urlTemplate, new TypeReference<TextPageData<EntityView>>() {
            }, pageLink);
            loadedItems.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        return loadedItems;
    }

    @Test
    public void testAssignEntityViewToEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        EntityView savedEntityView = getNewSavedEntityView("My entityView");

        doPost("/api/edge/" + savedEdge.getId().getId().toString()
                + "/device/" + testDevice.getId().getId().toString(), Device.class);

        doPost("/api/edge/" + savedEdge.getId().getId().toString()
                + "/entityView/" + savedEntityView.getId().getId().toString(), EntityView.class);

        TimePageData<EntityView> pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/entityViews?",
                new TypeReference<TimePageData<EntityView>>() {}, new TextPageLink(100));

        Assert.assertEquals(1, pageData.getData().size());

        doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                + "/entityView/" + savedEntityView.getId().getId().toString(), EntityView.class);

        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/entityViews?",
                new TypeReference<TimePageData<EntityView>>() {}, new TextPageLink(100));

        Assert.assertEquals(0, pageData.getData().size());
    }
}
