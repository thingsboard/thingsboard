// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.msa.WsClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.msa.prototypes.ConverterPrototypes.uplinkConverterPrototype;

/**
 * Live integration test that exercises ThingsBoard's PUB_SUB integration against a real
 * Google Cloud Pub/Sub subscription. The required cloud credentials are read from system
 * properties (-DblackBoxTests.pubSub*=...). If any of them is unset, the test is skipped —
 * so the class is safe to keep in CI without leaking secrets.
 */
@Slf4j
public class PubSubIntegrationTest extends AbstractIntegrationTest {

    private static final String ROUTING_KEY = "routing-key-pubsub-integration";
    private static final String SECRET_KEY = "secret-key-pubsub-integration";

    private static final String DEVICE_NAME_PLACEHOLDER = "DEVICE_NAME";

    // We ignore the inbound payload entirely and pin every uplink to the pre-created test device
    // so the test does not depend on anything inside the published Pub/Sub message.
    private static final String UPLINK_CONVERTER_CODE =
            "function decodeToString(payload) { return String.fromCharCode.apply(String, payload); }\n" +
            "function decodeToJson(payload) { return JSON.parse(decodeToString(payload)); }\n" +
            "var data = decodeToJson(payload);\n" +
            "var result = {\n" +
            "   deviceName: '" + DEVICE_NAME_PLACEHOLDER + "',\n" +
            "   deviceType: 'DEFAULT',\n" +
            "   telemetry: { temperature: '42' }\n" +
            "};\n" +
            "return result;";

    private JsonNode uplinkConverterCode;
    private WsClient wsClient;

    @BeforeMethod
    public void setUp() {
        uplinkConverterCode = JacksonUtil.newObjectNode()
                .put("decoder", UPLINK_CONVERTER_CODE.replace(DEVICE_NAME_PLACEHOLDER, device.getName()));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (wsClient != null) {
            wsClient.closeBlocking();
            wsClient = null;
        }
    }

    @Test
    public void shouldReceiveUplinkFromPubSubSubscription() throws Exception {
        String projectId = requireProperty("blackBoxTests.pubSubProjectId");
        String subscriptionId = requireProperty("blackBoxTests.pubSubSubscriptionId");
        String topicId = requireProperty("blackBoxTests.pubSubTopicId");
        String serviceAccountKey = resolveServiceAccountKey(requireProperty("blackBoxTests.pubSubServiceAccountKey"));

        createPubSubIntegration(projectId, subscriptionId, serviceAccountKey);

        wsClient = subscribeToWebSocket(device.getId(), "LATEST_TELEMETRY", CmdsType.TS_SUB_CMDS);

        publishUplink(projectId, topicId, serviceAccountKey);

        Awaitility
                .await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> wsClient.getLastMessage().getDataValuesByKey(TELEMETRY_KEY).get(1).equals(TELEMETRY_VALUE));
    }

    private Integration createPubSubIntegration(String projectId, String subscriptionId, String serviceAccountKey) {
        ObjectNode config = JacksonUtil.newObjectNode();
        ObjectNode clientConfiguration = config.putObject("clientConfiguration");
        clientConfiguration.put("projectId", projectId);
        clientConfiguration.put("subscriptionId", subscriptionId);
        clientConfiguration.put("serviceAccountKey", serviceAccountKey);
        clientConfiguration.put("serviceAccountKeyFileName", "key.json");
        config.putObject("metadata");

        Integration integration = new Integration();
        integration.setConfiguration(config);

        uplinkConverter = testRestClient.postConverter(uplinkConverterPrototype(uplinkConverterCode));
        integration.setDefaultConverterId(uplinkConverter.getId());

        integration.setName("pubsub_" + RandomStringUtils.randomAlphanumeric(7));
        integration.setType(IntegrationType.PUB_SUB);
        integration.setEnabled(true);
        integration.setRemote(false);
        integration.setDebugSettings(DebugSettings.all());
        integration.setAllowCreateDevicesOrAssets(true);
        integration.setRoutingKey(ROUTING_KEY);
        integration.setSecret(SECRET_KEY);

        integration = testRestClient.postIntegration(integration);
        waitUntilIntegrationStarted(integration.getId(), integration.getTenantId());
        return this.integration = integration;
    }

    // Publishes a message to the topic that feeds the configured subscription. We publish to the
    // topic directly (rather than resolving it from the subscription via the admin API) so the
    // service account only needs publish rights — not subscription-admin permissions.
    private void publishUplink(String projectId, String topicId, String serviceAccountKey) throws Exception {
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountKey.getBytes(StandardCharsets.UTF_8)));
        CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

        TopicName topicName = TopicName.of(projectId, topicId);
        Publisher publisher = Publisher.newBuilder(topicName).setCredentialsProvider(credentialsProvider).build();
        try {
            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8("{\"temperature\":42}"))
                    .build();
            publisher.publish(message).get();
        } finally {
            publisher.shutdown();
            publisher.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    // The service-account key is multi-line JSON and cannot be passed verbatim as a single -D
    // system property, so we accept it in whichever form is convenient for the runner:
    //   - raw JSON (value starts with '{') — works when the launcher preserves it as one argument;
    //   - a path to the downloaded key file;
    //   - a base64-encoded blob (e.g. `base64 -w0 key.json`) — the safest for IDE VM-options fields.
    private static String resolveServiceAccountKey(String raw) throws Exception {
        String value = raw.trim();
        if (value.startsWith("{")) {
            return value;
        }
        Path path = Path.of(value);
        if (Files.isRegularFile(path)) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String requireProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new SkipException("Required system property '" + key + "' is not set — skipping Pub/Sub live test.");
        }
        return value;
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "pubsub_";
    }

}
