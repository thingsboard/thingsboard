// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.event.LifeCycleEventFilter;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.cache.DefaultIntegrationExecutorTenantProfileCache;
import org.thingsboard.server.service.cache.IntegrationExecutorTenantProfileCache;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.service.integration.IntegrationDebugMessageStatus.ANY;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL",
        "integrations.statistics.persist_frequency=3000"
})
@Slf4j
@DaoSqlTest
@ContextConfiguration(classes = {HttpIntegrationTest.Config.class})
public class HttpIntegrationTest extends AbstractIntegrationTest {

    private static final String LOCALHOST = "localhost";

    @MockitoSpyBean
    private DefaultIntegrationRateLimitService limitService;

    @Autowired
    private DefaultIntegrationExecutorTenantProfileCache tenantProfileCache;

    @Autowired
    private SecretService secretService;

    private static final String DEVICE_HTTP_UPLINK_CONVERTER_FILEPATH = "http/device_default_converter_configuration.json";
    private static final String ASSET_HTTP_UPLINK_CONVERTER_FILEPATH = "http/asset_default_converter_configuration.json";
    private static final String HTTP_UPLINK_CONVERTER_NAME = "Default test uplink converter";
    private static final JsonNode TEST_DATA = JacksonUtil.fromString("{\"test\":1}", JsonNode.class);

    static class Config {

        @Primary
        @Bean
        public IntegrationExecutorTenantProfileCache tenantProfileCache(IntegrationConfigurationService integrationConfigurationService) {
            return new DefaultIntegrationExecutorTenantProfileCache(integrationConfigurationService);
        }

    }

    @Before
    public void beforeTest() throws Exception {
        updateDefaultTenantProfileConfig(profileConfig -> {
            profileConfig.setIntegrationMsgsPerTenantRateLimit("100:1,2000:60");
            profileConfig.setIntegrationMsgsPerDeviceRateLimit("100:1,2000:60");
            profileConfig.setIntegrationMsgsPerAssetRateLimit("100:1,2000:60");
        });

        ReflectionTestUtils.setField(limitService, "integrationEventsRateLimitsConf", "100:1,2000:60");
        ReflectionTestUtils.setField(limitService, "converterEventsRateLimitsConf", "100:1,2000:60");
    }

    @After
    public void afterTest() throws Exception {
        disableIntegration();
        removeIntegration(integration);
    }

    @Override
    protected JsonNode createIntegrationClientConfiguration() {
        ObjectNode clientConfiguration = JacksonUtil.newObjectNode();
        clientConfiguration.put("baseUrl", toSecretPlaceholder(LOCALHOST, SecretType.TEXT));
        clientConfiguration.set("metadata", JacksonUtil.newObjectNode());
        return clientConfiguration;
    }

    @Test
    public void testIntegrationRateLimitPerTenant() throws Exception {
        createIntegration(true);

        long startTime = System.currentTimeMillis();
        updateDefaultTenantProfileConfig(profileConfig -> {
            profileConfig.setIntegrationMsgsPerTenantRateLimit("10:1,20:60");
        });
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> tenantProfileCache.get(tenantId).getDefaultProfileConfiguration().getIntegrationMsgsPerTenantRateLimit() != null);

        repeat(20, i -> {
            try {
                doPost("/api/v1/integrations/http/{routingKey}", TEST_DATA, integration.getRoutingKey()).andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        await().atMost(20, TimeUnit.SECONDS).until(() ->
                getIntegrationDebugMessages(startTime, "Uplink", ANY, 10).size() >= 11
        );

        Mockito.verify(limitService, times(20)).checkLimit(eq(tenantId), any(Supplier.class));

        List<EventInfo> events = getIntegrationDebugMessages(startTime, "Uplink", "ERROR", 10);

        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("TENANT rate limits reached!", events.get(0).getBody().get("error").asText());

        // check integration stats
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> getIntegrationInfos(new PageLink(10))
                        .getData().get(0).getStats().equals(JacksonUtil.fromString("[20]", ArrayNode.class)));
    }

    @Test
    public void testIntegrationRateLimitPerDevice() throws Exception {
        createIntegration(true);

        long startTime = System.currentTimeMillis();
        updateDefaultTenantProfileConfig(profileConfig -> {
            profileConfig.setIntegrationMsgsPerDeviceRateLimit("10:1,20:60");
        });
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> tenantProfileCache.get(tenantId).getDefaultProfileConfiguration().getIntegrationMsgsPerDeviceRateLimit() != null);

        repeat(20, i -> {
            try {
                doPost("/api/v1/integrations/http/{routingKey}", TEST_DATA, integration.getRoutingKey()).andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        await().atMost(20, TimeUnit.SECONDS).until(() ->
                getIntegrationDebugMessages(startTime, "Uplink", ANY, 10).size() >= 11
        );

        Mockito.verify(limitService, times(20)).checkLimitPerDevice(eq(tenantId), eq("http_device"), any());

        List<EventInfo> events = getIntegrationDebugMessages(startTime, "Uplink", "ERROR", 10);

        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("DEVICE rate limits reached!", events.get(0).getBody().get("error").asText());
    }

    @Test
    public void testIntegrationRateLimitPerAsset() throws Exception {
        createIntegration(false);

        long startTime = System.currentTimeMillis();
        updateDefaultTenantProfileConfig(profileConfig -> {
            profileConfig.setIntegrationMsgsPerAssetRateLimit("10:1,20:60");
        });
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> tenantProfileCache.get(tenantId).getDefaultProfileConfiguration().getIntegrationMsgsPerAssetRateLimit() != null);

        repeat(20, i -> {
            try {
                doPost("/api/v1/integrations/http/{routingKey}", TEST_DATA, integration.getRoutingKey()).andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        await().atMost(20, TimeUnit.SECONDS).until(() ->
                getIntegrationDebugMessages(startTime, "Uplink", ANY, 10).size() >= 11
        );

        Mockito.verify(limitService, times(20)).checkLimitPerAsset(eq(tenantId), eq("http_asset"), any());

        List<EventInfo> events = getIntegrationDebugMessages(startTime, "Uplink", "ERROR", 10);

        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("ASSET rate limits reached!", events.get(0).getBody().get("error").asText());
    }

    @Test
    public void testIntegrationDebugEventRateLimit() throws Exception {
        createIntegration(true);

        long startTime = System.currentTimeMillis();
        ReflectionTestUtils.setField(limitService, "integrationEventsRateLimitsConf", "10:1,20:60");

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> tenantProfileCache.get(tenantId).getDefaultProfileConfiguration().getIntegrationMsgsPerTenantRateLimit() != null);

        repeat(20, i -> {
            try {
                doPost("/api/v1/integrations/http/{routingKey}", TEST_DATA, integration.getRoutingKey()).andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        await().atMost(20, TimeUnit.SECONDS).until(() ->
                getIntegrationDebugMessages(startTime, "Uplink", ANY, 10).size() >= 11
        );

        Mockito.verify(limitService, times(20)).checkLimit(eq(tenantId), any(Supplier.class));

        List<EventInfo> events = getIntegrationDebugMessages(startTime, "Uplink", "ERROR", 10);

        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("Integration debug rate limits reached!", events.get(0).getBody().get("error").asText());
    }

    @Test
    public void testConverterDebugEventRateLimit() throws Exception {
        createIntegration(true);

        long startTime = System.currentTimeMillis();
        ReflectionTestUtils.setField(limitService, "converterEventsRateLimitsConf", "10:1,20:60");

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> tenantProfileCache.get(tenantId).getDefaultProfileConfiguration().getIntegrationMsgsPerTenantRateLimit() != null);

        repeat(20, i -> {
            try {
                doPost("/api/v1/integrations/http/{routingKey}", TEST_DATA, integration.getRoutingKey()).andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        await().atMost(20, TimeUnit.SECONDS).until(() ->
                getConverterDebugMessages(startTime, "Uplink", ANY, 10).size() >= 11
        );

        Mockito.verify(limitService, times(20)).checkLimit(eq(tenantId), any(Supplier.class));

        List<EventInfo> events = getConverterDebugMessages(startTime, "Uplink", ANY, 10)
                .stream()
                .filter(event -> event.getBody().has("error")).toList();

        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());
        Assert.assertEquals("Converter debug rate limits reached!", events.get(0).getBody().get("error").asText());
    }

    @Test
    public void testUpdateSecretUsedInIntegration_thenReceiveIntegrationLifecycleEvent() throws Exception {
        createIntegration(true);

        cleanUpEvents(integration.getId(), new LifeCycleEventFilter());
        Awaitility
                .await()
                .alias("Get cleaned up integration events")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = getEvents(tenantId, integration.getId());
                    return events.getData().isEmpty();
                });

        // should trigger new update event
        Secret secret = secretService.findSecretByName(tenantId, LOCALHOST);
        secret.setValue("updated");
        secretService.saveSecret(tenantId, secret);

        Awaitility
                .await()
                .alias("Get integration events")
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> {
                    PageData<EventInfo> events = getEvents(tenantId, integration.getId());
                    if (events.getData().isEmpty()) {
                        return false;
                    }

                    EventInfo event = events.getData().stream().max(Comparator.comparingLong(EventInfo::getCreatedTime)).orElse(null);
                    return event != null
                            && "UPDATED".equals(event.getBody().get("event").asText())
                            && "true".equals(event.getBody().get("success").asText());
                });
    }

    private void createIntegration(boolean isDevice) throws Exception {
        loginTenantAdmin();

        Secret secret = constructSecret(LOCALHOST, "127.0.0.1");
        secretService.saveSecret(tenantId, secret);

        String filePath = isDevice ? DEVICE_HTTP_UPLINK_CONVERTER_FILEPATH : ASSET_HTTP_UPLINK_CONVERTER_FILEPATH;

        InputStream resourceAsStream = ObjectNode.class.getClassLoader().getResourceAsStream(filePath);
        ObjectNode jsonFile = new ObjectMapper().readValue(resourceAsStream, ObjectNode.class);
        Assert.assertNotNull(jsonFile);

        if (jsonFile.has("configuration") && jsonFile.get("configuration").has("decoder")) {
            createConverter(HTTP_UPLINK_CONVERTER_NAME, ConverterType.UPLINK, jsonFile.get("configuration"));
        }
        Assert.assertNotNull(uplinkConverter);

        createIntegration("Test HTTP integration", IntegrationType.HTTP);
        Assert.assertNotNull(integration);

        enableIntegration();
        waitUntilIntegrationStarted(tenantId, integration.getId());

        if (isDevice) {
            Device device = new Device();
            device.setName("http_device");
            device.setType("http");

            doPost("/api/device", device).andExpect(status().isOk());
        } else {
            Asset asset = new Asset();
            asset.setName("http_asset");
            asset.setType("http");

            doPost("/api/asset", asset).andExpect(status().isOk());
        }
    }

    private void repeat(int n, IntConsumer i) {
        IntStream.range(0, n).forEach(i);
    }

    private Secret constructSecret(String name, String value) {
        Secret secret = new Secret();
        secret.setTenantId(tenantId);
        secret.setName(name);
        secret.setValue(value);
        secret.setType(SecretType.TEXT);
        return secret;
    }

    private String toSecretPlaceholder(String name, SecretType type) {
        return String.format("${secret:%s;type:%s}", name, type);
    }

}
