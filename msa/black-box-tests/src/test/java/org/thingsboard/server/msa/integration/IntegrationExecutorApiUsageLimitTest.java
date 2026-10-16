// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.exception.IntegrationFeatureDisabledException;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.msa.TestProperties;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.integration.IntegrationType.MQTT;
import static org.thingsboard.server.msa.prototypes.MQTTIntegrationPrototypes.defaultConfig;

@Slf4j
public class IntegrationExecutorApiUsageLimitTest extends AbstractIntegrationTest {

    private static final String SERVICE_NAME = "broker";
    private static final int SERVICE_PORT = 1883;
    private static final String TOPIC = "tb/mqtt/api-limit";

    private static final long MAX_INTEGRATION_MESSAGES = 5;

    private static final String CONFIG_CONVERTER = """
            var payloadStr = decodeToString(payload);
            var data = JSON.parse(payloadStr);
            var deviceName = 'DEVICE_NAME';
            var deviceType = 'DEFAULT';
            var result = {
               deviceName: deviceName,
               deviceType: deviceType,
               telemetry: {
                   temperature: data.temperature,
               }
            };
            
            function decodeToString(payload) {
               return String.fromCharCode.apply(String, payload);
            }
            return result;""";

    private JsonNode configConverter;
    private long originalMaxIntegrationMessages;

    @BeforeClass
    public void beforeApiUsageLimitTestClass() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        updateDefaultTenantProfile(tenantProfile -> {
            TenantProfileData profileData = tenantProfile.getProfileData();
            DefaultTenantProfileConfiguration config = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
            originalMaxIntegrationMessages = config.getMaxIntegrationMessages();
            config.setMaxIntegrationMessages(MAX_INTEGRATION_MESSAGES);
            tenantProfile.setProfileData(profileData);
        });
        testRestClient.login(LOGIN, PASSWORD);
    }

    @AfterClass
    public void afterApiUsageLimitTestClass() {
        testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
        updateDefaultTenantProfile(tenantProfile -> {
            TenantProfileData profileData = tenantProfile.getProfileData();
            DefaultTenantProfileConfiguration config = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
            config.setMaxIntegrationMessages(originalMaxIntegrationMessages);
            tenantProfile.setProfileData(profileData);
        });
        testRestClient.login(LOGIN, PASSWORD);
    }

    @BeforeMethod
    public void setUp() {
        configConverter = JacksonUtil.newObjectNode().put("decoder", CONFIG_CONVERTER.replaceAll("DEVICE_NAME", device.getName()));
    }

    @Test
    public void testIntegrationStopsWhenApiLimitExceeded() throws Exception {
        String routingKey = "ie-limit-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-limit-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        createIntegration(MQTT, defaultConfig(SERVICE_NAME, SERVICE_PORT, TOPIC), configConverter, routingKey, secretKey, false);

        // Verify integration works initially
        publishToBroker("{\"temperature\":\"10\"}".getBytes());

        Awaitility.await()
                .alias("Wait for initial telemetry via MQTT integration")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> !testRestClient.getTimeseriesKeys(device.getId()).isEmpty());

        List<TsKvEntry> latestTimeseries = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        Assert.assertFalse(latestTimeseries.isEmpty());

        // Send enough messages to exceed the limit
        for (int i = 0; i < MAX_INTEGRATION_MESSAGES + 5; i++) {
            publishToBroker(("{\"temperature\":\"" + (20 + i) + "\"}").getBytes());
        }

        // Wait for integration to be stopped due to API limits
        Awaitility.await()
                .alias("Wait for integration STOPPED event due to API limits")
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    var events = testRestClient.getEvents(
                            integration.getId(), EventType.LC_EVENT, integration.getTenantId(), new TimePageLink(1024));
                    if (events.getData().isEmpty()) {
                        return false;
                    }
                    EventInfo latestEvent = events.getData().stream()
                            .max(Comparator.comparingLong(EventInfo::getCreatedTime))
                            .orElse(null);
                    return latestEvent != null && "STOPPED".equals(latestEvent.getBody().get("event").asText());
                });

        // Verify that new messages are not processed after the integration is stopped
        String valueAfterDisable = "999";
        String latestBefore = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY))
                .getFirst().getValue().toString();
        publishToBroker(("{\"temperature\":\"" + valueAfterDisable + "\"}").getBytes());

        Awaitility.await()
                .alias("Latest telemetry value must not change after integration is stopped")
                .during(5, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<TsKvEntry> ts = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
                    Assert.assertFalse(ts.isEmpty());
                    Assert.assertEquals(ts.getFirst().getValue().toString(), latestBefore,
                            "Message should not be processed after integration is disabled due to API limits");
                });

        var allEvents = testRestClient.getEvents(
                integration.getId(), EventType.LC_EVENT, integration.getTenantId(), new TimePageLink(1024));
        long duplicateUpdatedFailed = allEvents.getData().stream()
                .filter(e -> "UPDATED".equals(e.getBody().path("event").asText()))
                .filter(e -> e.getBody().path("error").asText("").contains(IntegrationFeatureDisabledException.DEFAULT_MESSAGE))
                .count();
        Assert.assertEquals(duplicateUpdatedFailed, 0L,
                "Disable transition must persist a single STOPPED event, not STOPPED + UPDATED-FAILED.");
    }

    @Test
    public void testIntegrationRestartsWhenApiLimitLifted() throws Exception {
        String routingKey = "ie-restart-routing-" + RandomStringUtils.secure().nextAlphanumeric(10);
        String secretKey = "ie-restart-secret-" + RandomStringUtils.secure().nextAlphanumeric(10);

        createIntegration(MQTT, defaultConfig(SERVICE_NAME, SERVICE_PORT, TOPIC), configConverter, routingKey, secretKey, false);

        publishToBroker("{\"temperature\":\"10\"}".getBytes());
        Awaitility.await()
                .alias("Wait for initial telemetry via MQTT integration")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> !testRestClient.getTimeseriesKeys(device.getId()).isEmpty());

        for (int i = 0; i < MAX_INTEGRATION_MESSAGES + 5; i++) {
            publishToBroker(("{\"temperature\":\"" + (20 + i) + "\"}").getBytes());
        }

        Awaitility.await()
                .alias("Wait for integration STOPPED event due to API limits")
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> latestLifecycleEventIs("STOPPED"));

        long stoppedAt = latestLifecycleCreatedTime();

        try {
            testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
            updateDefaultTenantProfile(tenantProfile -> {
                TenantProfileData profileData = tenantProfile.getProfileData();
                DefaultTenantProfileConfiguration config = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
                config.setMaxIntegrationMessages(10_000_000);
                tenantProfile.setProfileData(profileData);
            });
            testRestClient.login(LOGIN, PASSWORD);

            Awaitility.await()
                    .alias("Wait for STARTED event after limit lifted")
                    .atMost(90, TimeUnit.SECONDS)
                    .pollInterval(1, TimeUnit.SECONDS)
                    .until(() -> {
                        var events = testRestClient.getEvents(
                                integration.getId(), EventType.LC_EVENT, integration.getTenantId(), new TimePageLink(1024));
                        return events.getData().stream()
                                .filter(e -> e.getCreatedTime() > stoppedAt)
                                .anyMatch(e -> "STARTED".equals(e.getBody().path("event").asText()));
                    });

            String valueAfterRestart = "777";
            publishToBroker(("{\"temperature\":\"" + valueAfterRestart + "\"}").getBytes());

            Awaitility.await()
                    .alias("New telemetry must be processed after integration restarts")
                    .atMost(30, TimeUnit.SECONDS)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .until(() -> {
                        List<TsKvEntry> ts = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
                        return !ts.isEmpty() && valueAfterRestart.equals(ts.getFirst().getValue().toString());
                    });
        } finally {
            // Restore the small limit so subsequent tests in the class see the same fixture as @BeforeClass.
            testRestClient.login("sysadmin@thingsboard.org", "sysadmin");
            updateDefaultTenantProfile(tenantProfile -> {
                TenantProfileData profileData = tenantProfile.getProfileData();
                DefaultTenantProfileConfiguration config = (DefaultTenantProfileConfiguration) profileData.getConfiguration();
                config.setMaxIntegrationMessages(MAX_INTEGRATION_MESSAGES);
                tenantProfile.setProfileData(profileData);
            });
            testRestClient.login(LOGIN, PASSWORD);
        }
    }

    private boolean latestLifecycleEventIs(String expected) {
        var events = testRestClient.getEvents(
                integration.getId(), EventType.LC_EVENT, integration.getTenantId(), new TimePageLink(1024));
        if (events.getData().isEmpty()) {
            return false;
        }
        EventInfo latest = events.getData().stream()
                .max(Comparator.comparingLong(EventInfo::getCreatedTime))
                .orElse(null);
        return latest != null && expected.equals(latest.getBody().path("event").asText());
    }

    private long latestLifecycleCreatedTime() {
        var events = testRestClient.getEvents(
                integration.getId(), EventType.LC_EVENT, integration.getTenantId(), new TimePageLink(1024));
        return events.getData().stream()
                .mapToLong(EventInfo::getCreatedTime)
                .max()
                .orElse(0L);
    }

    private void publishToBroker(byte[] payload) throws Exception {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        MqttClient client = new MqttClient(TestProperties.getMqttBrokerUrl(), StringUtils.randomAlphanumeric(10), new MemoryPersistence());
        client.connect(connOpts);
        MqttMessage message = new MqttMessage(payload);
        message.setQos(0);
        client.publish(TOPIC, message);
        client.disconnect();
        client.close();
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "ie_api_limit_";
    }

}
