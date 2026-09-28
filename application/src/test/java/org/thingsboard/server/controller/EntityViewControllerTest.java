// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@TestPropertySource(properties = {
        "transport.mqtt.enabled=true",
        "js.evaluator=mock",
})
@Slf4j
@ContextConfiguration(classes = {EntityViewControllerTest.Config.class})
@DaoSqlTest
public class EntityViewControllerTest extends AbstractControllerTest {
    // Must NOT be imported from AbstractMqttIntegrationTest. That field is a static final initialized
    // once per JVM. Other test classes (e.g. MqttGatewayRateLimitsTest, DeviceEdgeTest) share the same
    // constant but produce a different Spring context cache key, so Spring creates a separate
    // ApplicationContext for each of them. Every context starts its own MqttTransportService and tries
    // to bind the same port, causing BindException when tests run in the same Surefire JVM fork.
    // Declaring the port here gives this context its own independently allocated port.
    static final int MQTT_PORT = TestSocketUtils.findAvailableTcpPort();
    static final String MQTT_URL = "tcp://localhost:" + MQTT_PORT;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        log.warn("transport.mqtt.bind_port = {}", MQTT_PORT);
        registry.add("transport.mqtt.bind_port", () -> MQTT_PORT);
    }

    static final TypeReference<PageData<EntityView>> PAGE_DATA_ENTITY_VIEW_TYPE_REF = new TypeReference<>() {
    };

    private Device testDevice;
    private TelemetryEntityView telemetry;

    List<ListenableFuture<ResultActions>> deleteFutures = new ArrayList<>();
    ListeningExecutorService executor;

    @Autowired
    private EntityViewDao entityViewDao;

    static class Config {
        @Bean
        @Primary
        public EntityViewDao entityViewDao(EntityViewDao entityViewDao) {
            return Mockito.mock(EntityViewDao.class, AdditionalAnswers.delegatesTo(entityViewDao));
        }
    }

    @Before
    public void beforeTest() throws Exception {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));

        loginTenantAdmin();

        Device device = new Device();
        device.setName("Test device 4view");
        device.setType("default");
        testDevice = doPost("/api/device", device, Device.class);

        telemetry = new TelemetryEntityView(
                List.of("tsKey1", "tsKey2", "tsKey3"),
                new AttributesEntityView(
                        List.of("caKey1", "caKey2", "caKey3", "caKey4"),
                        List.of("saKey1", "saKey2", "saKey3", "saKey4"),
                        List.of("shKey1", "shKey2", "shKey3", "shKey4")));
    }

    @After
    public void afterTest() throws Exception {
        executor.shutdownNow();
    }

    @Test
    public void testFindEntityViewById() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");
        EntityView foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        Assert.assertNotNull(foundView);
        assertEquals(savedView, foundView);
    }

    @Test
    public void testFindEntityViewByIds() throws Exception {
        List<EntityView> assetProfiles = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            assetProfiles.add(getNewSavedEntityView("Test entity view " + i));
        }

        List<EntityView> expected = assetProfiles.subList(5, 15);

        String idsParam = expected.stream()
                .map(ap -> ap.getId().getId().toString())
                .collect(Collectors.joining(","));
        EntityView[] foundEntityViews = doGet("/api/entityViews?entityViewIds=" + idsParam, EntityView[].class);

        Assert.assertNotNull(foundEntityViews);
        Assert.assertEquals(expected.size(), foundEntityViews.length);

        Map<UUID, EntityView> infoById = Arrays.stream(foundEntityViews)
                .collect(Collectors.toMap(info -> info.getId().getId(), Function.identity()));

        for (EntityView entityView : expected) {
            UUID id = entityView.getId().getId();
            EntityView view = infoById.get(id);
            Assert.assertNotNull("Entity view not found for id " + id, view);

            Assert.assertEquals(entityView.getId(), view.getId());
            Assert.assertEquals(entityView.getName(), view.getName());
        }
    }

    @Test
    public void testSaveEntityView() throws Exception {
        String name = "Test entity view";

        Mockito.reset(tbClusterService, auditLogService);

        EntityView savedView = getNewSavedEntityView(name);

        Assert.assertNotNull(savedView);
        Assert.assertNotNull(savedView.getId());
        Assert.assertTrue(savedView.getCreatedTime() > 0);
        assertEquals(tenantId, savedView.getTenantId());
        Assert.assertNotNull(savedView.getCustomerId());
        assertEquals(NULL_UUID, savedView.getCustomerId().getId());
        assertEquals(name, savedView.getName());

        EntityView foundEntityView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);

        assertEquals(savedView, foundEntityView);

        testBroadcastEntityStateChangeEventTime(foundEntityView.getId(), tenantId, 1);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundEntityView, foundEntityView,
                tenantId, tenantAdminCustomerId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ADDED, 1, 1, 1);
        Mockito.reset(tbClusterService, auditLogService);

        savedView.setName("New test entity view");

        savedView = doPost("/api/entityView", savedView, EntityView.class);
        foundEntityView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);

        assertEquals(savedView, foundEntityView);

        testBroadcastEntityStateChangeEventTime(foundEntityView.getId(), tenantId, 1);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundEntityView, foundEntityView,
                tenantId, tenantAdminCustomerId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.UPDATED, 1, 1, 5);

        doGet("/api/tenant/entityViews?entityViewName=" + name)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));
    }

    @Test
    public void testSaveEntityViewWithViolationOfValidation() throws Exception {
        EntityView entityView = createEntityView(StringUtils.randomAlphabetic(300), 0, 0);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("name");
        doPost("/api/entityView", entityView)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(entityView,
                tenantId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        entityView.setName("Normal name");
        msgError = msgErrorFieldLength("type");
        entityView.setType(StringUtils.randomAlphabetic(300));
        doPost("/api/entityView", entityView)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(entityView,
                tenantId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testUpdateEntityViewFromDifferentTenant() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");
        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorPermissionWrite + "ENTITY_VIEW" + " '" + savedView.getName() + "'!";
        doPost("/api/entityView", savedView)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(savedView, savedDifferentTenant.getId(), savedDifferentTenantUser.getId(),
                DIFFERENT_TENANT_ADMIN_EMAIL, ActionType.UPDATED, new ThingsboardException(msgError, ThingsboardErrorCode.PERMISSION_DENIED));

        deleteDifferentTenant();
    }

    @Test
    public void testSaveEntityViewWithDuplicateExternalId() throws Exception {
        // Per-tenant entity view external_id uniqueness is enforced at the application layer
        // (EntityViewDataValidator), the only guard under Citus where entity_view_external_id_unq_key is dropped.
        EntityViewId externalId = new EntityViewId(UUID.randomUUID());

        EntityView view = createEntityView("Entity view with external id", 0, 0);
        view.setExternalId(externalId);
        EntityView saved = doPost("/api/entityView", view, EntityView.class);
        assertThat(saved.getExternalId()).isEqualTo(externalId);

        EntityView duplicate = createEntityView("Another entity view with the same external id", 0, 0);
        duplicate.setExternalId(externalId);
        doPost("/api/entityView", duplicate)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Entity view with such external id already exists!")));
    }

    @Test
    public void testSaveEntityViewWithNonExistingPresetIdFails() throws Exception {
        // A save with a client-supplied id that does not exist is rejected: the WRITE permission check
        // resolves the id before saving and fails with a not-found error, matching the device/asset controllers.
        EntityView view = createEntityView("Entity view with preset id", 0, 0);
        EntityViewId presetId = new EntityViewId(UUID.randomUUID());
        view.setId(presetId);
        doPost("/api/entityView", view)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound(EntityType.ENTITY_VIEW.getNormalName(), presetId.getId().toString()))));
    }

    @Test
    public void testUpdateEntityViewWithSameNameAsExistingFails() throws Exception {
        // Per-tenant entity view name uniqueness is enforced at the application layer (EntityViewDataValidator),
        // the only guard under Citus where the entity_view_name_unq_key DB constraint is dropped.
        getNewSavedEntityView("Entity view A");
        EntityView savedViewB = getNewSavedEntityView("Entity view B");

        savedViewB.setName("Entity view A");
        doPost("/api/entityView", savedViewB)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Entity view with such name already exists!")));
    }

    @Test
    public void testSaveEntityViewWithUnchangedNameSucceeds() throws Exception {
        EntityView savedView = getNewSavedEntityView("My entity view");

        // Re-saving the same entity view unchanged must not trip the name uniqueness check.
        EntityView resaved = doPost("/api/entityView", savedView, EntityView.class);
        Assert.assertEquals(savedView.getId(), resaved.getId());
        Assert.assertEquals("My entity view", resaved.getName());

        // Modifying a non-name field (time range) and re-saving must also succeed.
        resaved.setStartTimeMs(1000L);
        EntityView updated = doPost("/api/entityView", resaved, EntityView.class);
        Assert.assertEquals(savedView.getId(), updated.getId());
        Assert.assertEquals("My entity view", updated.getName());
        Assert.assertEquals(1000L, updated.getStartTimeMs());
    }

    @Test
    public void testSameEntityViewNameAllowedAcrossTenants() throws Exception {
        getNewSavedEntityView("Shared entity view name");

        loginDifferentTenant();
        Device differentTenantDevice = new Device();
        differentTenantDevice.setName("Different tenant device 4view");
        differentTenantDevice.setType("default");
        Device savedDifferentTenantDevice = doPost("/api/device", differentTenantDevice, Device.class);

        EntityView differentTenantView = new EntityView();
        differentTenantView.setEntityId(savedDifferentTenantDevice.getId());
        differentTenantView.setName("Shared entity view name");
        differentTenantView.setType("default");
        differentTenantView.setKeys(telemetry);
        differentTenantView.setStartTimeMs(0);
        differentTenantView.setEndTimeMs(0);
        EntityView savedDifferentTenantView = doPost("/api/entityView", differentTenantView, EntityView.class);
        Assert.assertNotNull(savedDifferentTenantView);
        Assert.assertEquals("Shared entity view name", savedDifferentTenantView.getName());

        deleteDifferentTenant();
    }

    @Test
    public void testDeleteEntityView() throws Exception {
        Customer customer = doPost("/api/customer", getNewCustomer("My customer"), Customer.class);

        EntityView view = createEntityView("Test entity view", 0, 0);
        view.setCustomerId(customer.getId());
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        Mockito.reset(tbClusterService, auditLogService);

        String entityIdStr = savedView.getId().getId().toString();
        doDelete("/api/entityView/" + entityIdStr)
                .andExpect(status().isOk());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedView, savedView.getId(), savedView.getId(),
                tenantId, view.getCustomerId(), tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.DELETED, entityIdStr);

        doGet("/api/entityView/" + entityIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound(EntityType.ENTITY_VIEW.getNormalName(), entityIdStr))));
    }

    @Test
    public void testSaveEntityViewWithEmptyName() throws Exception {
        EntityView entityView = new EntityView();
        entityView.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Entity view name " + msgErrorShouldBeSpecified;
        doPost("/api/entityView", entityView)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(entityView,
                tenantId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    public void testGetTenantEntityViews() throws Exception {
        List<ListenableFuture<EntityView>> entityViewInfoFutures = new ArrayList<>(178);
        for (int i = 0; i < 178; i++) {
            entityViewInfoFutures.add(getNewSavedEntityViewAsync("Test entity view" + i));
        }
        List<EntityView> entityViewInfos = Futures.allAsList(entityViewInfoFutures).get(TIMEOUT, SECONDS);
        List<EntityView> loadedViews = loadListOf(new PageLink(23), "/api/tenant/entityViews?");
        assertThat(entityViewInfos).containsExactlyInAnyOrderElementsOf(loadedViews);
    }

    @Test
    public void testGetTenantEntityViewsByName() throws Exception {
        String name1 = "Entity view name1";
        List<EntityView> namesOfView1 = Futures.allAsList(fillListOf(17, name1)).get(TIMEOUT, SECONDS);
        List<EntityView> loadedNamesOfView1 = loadListOf(new PageLink(5, 0, name1), "/api/tenant/entityViews?");
        assertThat(namesOfView1).as(name1).containsExactlyInAnyOrderElementsOf(loadedNamesOfView1);

        String name2 = "Entity view name2";
        List<EntityView> namesOfView2 = Futures.allAsList(fillListOf(15, name2)).get(TIMEOUT, SECONDS);
        ;
        List<EntityView> loadedNamesOfView2 = loadListOf(new PageLink(4, 0, name2), "/api/tenant/entityViews?");
        assertThat(namesOfView2).as(name2).containsExactlyInAnyOrderElementsOf(loadedNamesOfView2);

        deleteFutures.clear();
        for (EntityView view : loadedNamesOfView1) {
            deleteFutures.add(executor.submit(() ->
                    doDelete("/api/entityView/" + view.getId().getId().toString()).andExpect(status().isOk())));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, SECONDS);

        PageData<EntityView> pageData = doGetTypedWithPageLink("/api/tenant/entityViews?", PAGE_DATA_ENTITY_VIEW_TYPE_REF,
                new PageLink(4, 0, name1));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());

        deleteFutures.clear();
        for (EntityView view : loadedNamesOfView2) {
            deleteFutures.add(executor.submit(() ->
                    doDelete("/api/entityView/" + view.getId().getId().toString()).andExpect(status().isOk())));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, SECONDS);

        pageData = doGetTypedWithPageLink("/api/tenant/entityViews?", PAGE_DATA_ENTITY_VIEW_TYPE_REF,
                new PageLink(4, 0, name2));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testTheCopyOfAttrsIntoTSForTheView() throws Exception {
        Set<String> expectedActualAttributesSet = Set.of("caKey1", "caKey2", "caKey3", "caKey4");
        Set<String> actualAttributesSet =
                putAttributesAndWait("{\"caKey1\":\"value1\", \"caKey2\":true, \"caKey3\":42.0, \"caKey4\":73}", expectedActualAttributesSet);
        EntityView savedView = getNewSavedEntityView("Test entity view");

        List<Map<String, Object>> values = await("telemetry/ENTITY_VIEW")
                .atMost(TIMEOUT, SECONDS)
                .until(() -> doGetAsyncTyped("/api/plugins/telemetry/ENTITY_VIEW/" + savedView.getId().getId().toString() +
                                "/values/attributes?keys=" + String.join(",", actualAttributesSet), new TypeReference<>() {
                        }),
                        x -> x.size() >= expectedActualAttributesSet.size());

        assertEquals("value1", getValue(values, "caKey1"));
        assertEquals(true, getValue(values, "caKey2"));
        assertEquals(42.0, getValue(values, "caKey3"));
        assertEquals(73, getValue(values, "caKey4"));
    }

    @Test
    public void testTheCopyOfAttrsOutOfTSForTheView() throws Exception {
        long now = System.currentTimeMillis();
        Set<String> expectedActualAttributesSet = Set.of("caKey1", "caKey2", "caKey3", "caKey4");
        Set<String> actualAttributesSet =
                putAttributesAndWait("{\"caKey1\":\"value1\", \"caKey2\":true, \"caKey3\":42.0, \"caKey4\":73}", expectedActualAttributesSet);

        List<Map<String, Object>> values = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + testDevice.getId() +
                "/values/attributes?keys=" + String.join(",", expectedActualAttributesSet), new TypeReference<>() {
        });
        assertEquals(expectedActualAttributesSet.size(), values.size());

        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setTenantId(tenantId);
        view.setName("Test entity view");
        view.setType("default");
        view.setKeys(telemetry);
        view.setStartTimeMs(now - HOURS.toMillis(1));
        view.setEndTimeMs(now - 1);
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        values = doGetAsyncTyped("/api/plugins/telemetry/ENTITY_VIEW/" + savedView.getId().getId().toString() +
                "/values/attributes?keys=" + String.join(",", expectedActualAttributesSet), new TypeReference<>() {
        });
        assertEquals(0, values.size());
    }


    @Test
    public void testGetTelemetryWhenEntityViewTimeRangeInsideTimestampRange() throws Exception {
        DeviceTypeFilter dtf = new DeviceTypeFilter(List.of(testDevice.getType()), testDevice.getName());
        List<String> tsKeys = List.of("tsKey1", "tsKey2", "tsKey3");

        DeviceCredentials deviceCredentials = doGet("/api/device/" + testDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        assertEquals(testDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        long now = System.currentTimeMillis();
        getWsClient().subscribeTsUpdate(tsKeys, now, TimeUnit.HOURS.toMillis(1), dtf);

        getWsClient().registerWaitForUpdate();
        uploadTelemetry("{\"tsKey1\":\"value1\", \"tsKey2\":true, \"tsKey3\":40.0}", accessToken);
        getWsClient().waitForUpdate();

        long startTimeMs = getCurTsButNotPrevTs(now);

        getWsClient().registerWaitForUpdate();
        uploadTelemetry("{\"tsKey1\":\"value2\", \"tsKey2\":false, \"tsKey3\":80.0}", accessToken);
        getWsClient().waitForUpdate();

        long middleOfTestMs = getCurTsButNotPrevTs(startTimeMs);

        getWsClient().registerWaitForUpdate();
        uploadTelemetry("{\"tsKey1\":\"value3\", \"tsKey2\":false, \"tsKey3\":120.0}", accessToken);
        getWsClient().waitForUpdate();

        long endTimeMs = getCurTsButNotPrevTs(middleOfTestMs);
        getWsClient().registerWaitForUpdate();
        uploadTelemetry("{\"tsKey1\":\"value4\", \"tsKey2\":true, \"tsKey3\":160.0}", accessToken);
        getWsClient().waitForUpdate();

        String deviceId = testDevice.getId().getId().toString();
        Set<String> keys = getTelemetryKeys("DEVICE", deviceId);

        EntityView view = createEntityView("Test entity view", startTimeMs, endTimeMs);
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);
        String entityViewId = savedView.getId().getId().toString();

        Map<String, List<Map<String, String>>> actualDeviceValues = getTelemetryValues("DEVICE", deviceId, keys, 0L, middleOfTestMs);
        Assert.assertEquals(2, actualDeviceValues.get("tsKey1").size());
        Assert.assertEquals(2, actualDeviceValues.get("tsKey2").size());
        Assert.assertEquals(2, actualDeviceValues.get("tsKey3").size());

        Map<String, List<Map<String, String>>> actualEntityViewValues = getTelemetryValues("ENTITY_VIEW", entityViewId, keys, 0L, middleOfTestMs);
        Assert.assertEquals(1, actualEntityViewValues.get("tsKey1").size());
        Assert.assertEquals(1, actualEntityViewValues.get("tsKey2").size());
        Assert.assertEquals(1, actualEntityViewValues.get("tsKey3").size());
    }

    private static long getCurTsButNotPrevTs(long prevTs) throws InterruptedException {
        long result = System.currentTimeMillis();
        if (prevTs == result) {
            Thread.sleep(1);
            return getCurTsButNotPrevTs(prevTs);
        } else {
            return result;
        }
    }

    private void uploadTelemetry(String strKvs, String accessToken) throws Exception {
        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        awaitConnected(client, SECONDS.toMillis(30));
        MqttMessage message = new MqttMessage();
        message.setPayload(strKvs.getBytes());
        IMqttDeliveryToken token = client.publish("v1/devices/me/telemetry", message);
        await("mqtt ack").pollInterval(5, MILLISECONDS).atMost(TIMEOUT, SECONDS).until(() -> token.getMessage() == null);
        client.disconnect();
    }

    private void awaitConnected(MqttAsyncClient client, long ms) throws InterruptedException {
        await("awaitConnected").pollInterval(5, MILLISECONDS).atMost(TIMEOUT, SECONDS)
                .until(client::isConnected);
    }

    private Set<String> getTelemetryKeys(String type, String id) throws Exception {
        return new HashSet<>(doGetAsyncTyped("/api/plugins/telemetry/" + type + "/" + id + "/keys/timeseries", new TypeReference<>() {
        }));
    }

    private Set<String> getAttributeKeys(String type, String id) throws Exception {
        return new HashSet<>(doGetAsyncTyped("/api/plugins/telemetry/" + type + "/" + id + "/keys/attributes", new TypeReference<>() {
        }));
    }

    private Map<String, List<Map<String, String>>> getTelemetryValues(String type, String id, Set<String> keys, Long startTs, Long endTs) throws Exception {
        return doGetAsyncTyped("/api/plugins/telemetry/" + type + "/" + id +
                "/values/timeseries?keys=" + String.join(",", keys) + "&startTs=" + startTs + "&endTs=" + endTs, new TypeReference<>() {
        });
    }

    private Set<String> putAttributesAndWait(String stringKV, Set<String> expectedKeySet) throws Exception {
        DeviceTypeFilter dtf = new DeviceTypeFilter(List.of(testDevice.getType()), testDevice.getName());
        List<EntityKey> keysToSubscribe = expectedKeySet.stream()
                .map(key -> new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, key))
                .collect(Collectors.toList());

        getWsClient().subscribeLatestUpdate(keysToSubscribe, dtf);

        String viewDeviceId = testDevice.getId().getId().toString();
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + viewDeviceId + "/credentials", DeviceCredentials.class);
        assertEquals(testDevice.getId(), deviceCredentials.getDeviceId());

        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        awaitConnected(client, SECONDS.toMillis(30));
        MqttMessage message = new MqttMessage();
        message.setPayload((stringKV).getBytes());
        getWsClient().registerWaitForUpdate();
        IMqttDeliveryToken token = client.publish("v1/devices/me/attributes", message);
        await("mqtt ack").pollInterval(5, MILLISECONDS).atMost(TIMEOUT, SECONDS).until(() -> token.getMessage() == null);
        assertThat(getWsClient().waitForUpdate()).as("ws update received").isNotBlank();
        return getAttributeKeys("DEVICE", viewDeviceId);
    }

    private Object getValue(List<Map<String, Object>> values, String stringValue) {
        return values.size() == 0 ? null :
                values.stream()
                        .filter(value -> value.get("key").equals(stringValue))
                        .findFirst().get().get("value");
    }

    private EntityView getNewSavedEntityView(String name) {
        EntityView view = createEntityView(name, 0, 0);
        return doPost("/api/entityView", view, EntityView.class);
    }

    private ListenableFuture<EntityView> getNewSavedEntityViewAsync(String name) {
        return executor.submit(() -> getNewSavedEntityView(name));
    }

    private EntityView createEntityView(String name, long startTimeMs, long endTimeMs) {
        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setTenantId(tenantId);
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

    private List<ListenableFuture<EntityView>> fillListOf(int limit, String partOfName) {
        List<ListenableFuture<EntityView>> viewNameFutures = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            boolean even = i % 2 == 0;
            ListenableFuture<CustomerId> customerFuture = executor.submit(() -> {
                Customer customer = getNewCustomer("Test customer " + Math.random());
                return doPost("/api/customer", customer, Customer.class).getId();
            });

            viewNameFutures.add(Futures.transform(customerFuture, customerId -> {
                String fullName = partOfName + ' ' + StringUtils.randomAlphanumeric(15);
                fullName = even ? fullName.toLowerCase() : fullName.toUpperCase();
                EntityView view = createEntityView(fullName, 0, 0);
                view.setCustomerId(customerId);
                return doPost("/api/entityView", view, EntityView.class);
            }, MoreExecutors.directExecutor()));
        }
        return viewNameFutures;
    }

    private List<EntityView> loadListOf(PageLink pageLink, String urlTemplate) throws Exception {
        List<EntityView> loadedItems = new ArrayList<>();
        PageData<EntityView> pageData;
        do {
            pageData = doGetTypedWithPageLink(urlTemplate, PAGE_DATA_ENTITY_VIEW_TYPE_REF, pageLink);
            loadedItems.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        return loadedItems;
    }

    @Test
    public void testDeleteEntityViewWithDeleteRelationsOk() throws Exception {
        EntityViewId entityViewId = getNewSavedEntityView("EntityView for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(tenantId, entityViewId, "/api/entityView/" + entityViewId);
    }

    @Ignore
    @Test
    public void testDeleteEntityViewExceptionWithRelationsTransactional() throws Exception {
        EntityViewId entityViewId = getNewSavedEntityView("EntityView for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(entityViewDao, tenantId, entityViewId, "/api/entityView/" + entityViewId);
    }

    @Test
    public void testSaveEntityViewWithUniquifyStrategy() throws Exception {
        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setTenantId(tenantId);
        view.setType("default");
        view.setName("My unique view");

        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        doPost("/api/entityView?nameConflictPolicy=FAIL", view).andExpect(status().isBadRequest());

        EntityView secondView = doPost("/api/entityView?nameConflictPolicy=UNIQUIFY", view, EntityView.class);
        assertThat(secondView.getName()).startsWith("My unique view_");

        EntityView thirdView = doPost("/api/entityView?nameConflictPolicy=UNIQUIFY&uniquifySeparator=-", view, EntityView.class);
        assertThat(thirdView.getName()).startsWith("My unique view-");

        EntityView fourthView = doPost("/api/entityView?nameConflictPolicy=UNIQUIFY&uniquifyStrategy=INCREMENTAL", view, EntityView.class);
        assertThat(fourthView.getName()).isEqualTo("My unique view_1");

        EntityView fifthEntityView = doPost("/api/entityView?nameConflictPolicy=UNIQUIFY&uniquifyStrategy=INCREMENTAL", view, EntityView.class);
        assertThat(fifthEntityView.getName()).isEqualTo("My unique view_2");
    }
}
