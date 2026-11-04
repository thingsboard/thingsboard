// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.integration;

import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.sample.pubSub.TestTopicListener;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.common.data.DataConstants.DEVICE;
import static org.thingsboard.server.common.data.DataConstants.SHARED_SCOPE;
import static org.thingsboard.server.common.data.integration.IntegrationType.AWS_IOT;
import static org.thingsboard.server.msa.prototypes.AwsIotIntegrationPrototypes.defaultConfig;

@Slf4j
public class AwsIotIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTING_KEY = "routing-key-1234599";
    private static final String SECRET_KEY = "secret-key-1234599";

    private static final JsonNode CONFIG_CONVERTER = JacksonUtil.newObjectNode().put("decoder", "var payloadStr = decodeToString(payload);\n" +
            "var data = decodeToJson(payload);\n" +
            "var result = {};\n" +
            "var topic = metadata['topic'].split('/');\n" +
            "result.deviceName = topic[1];\n" +
            "if (topic[2] == 'temperature'){\n" +
            "    result.deviceType = 'termostat';\n" +
            "    result.telemetry = {temperature: data.value};\n" +
            "}\n" +
            "function decodeToString(payload) {\n" +
            "   return String.fromCharCode.apply(String, payload);\n" +
            "}\n" +
            "function decodeToJson(payload) {   \n" +
            "   var str = decodeToString(payload);\n" +
            "   var data = JSON.parse(str);\n" +
            "   return data;\n" +
            "}\n" +
            "\n" +
            "return result;");

    private final JsonNode DOWNLINK_CONVERTER_CONFIGURATION = JacksonUtil.newObjectNode()
            .put("encoder", "var data = {};\n" +
                    "data.booleanKey = msg.booleanKey;\n" +
                    "data.stringKey = msg.stringKey;\n" +
                    "data.doubleKey = msg.doubleKey;\n" +
                    "data.longKey = msg.longKey;\n" +
                    "\n" +
                    "data.devSerialNumber = metadata['ss_serialNumber'];\n" +
                    "var result = {\n" +
                    "    contentType: \"JSON\",\n" +
                    "    data: JSON.stringify(data),\n" +
                    "    metadata: {\n" +
                    "            topic: 'sensors/device/upload'\n" +
                    "    }\n" +
                    "};\n" +
                    "return result;");
    private static final String CLIENT_ENDPOINT = System.getProperty("blackBoxTests.aws.endpoint", "");
    private static final String ROOT_CA = System.getProperty("blackBoxTests.aws.rootCA", "");
    private static final String CERTIFICATE = System.getProperty("blackBoxTests.aws.cert", "");
    private static final String PRIVATE_KEY = System.getProperty("blackBoxTests.aws.privateKey", "");

    @BeforeClass
    public void beforeClass() {
        if (Boolean.parseBoolean(System.getProperty("blackBoxTests.integrations.skip", "true"))) {
            throw new SkipException("Aws iot integration tests are skipped");
        }
    }

    @Test
    public void telemetryUploadWithLocalIntegration() throws Exception {
        createIntegration(AWS_IOT, getIntegrationConfig(), CONFIG_CONVERTER, ROUTING_KEY, SECRET_KEY, false);

        String value = "13";
        String content = createPayloadForUplink(value).toString();

        //send payload to aws iot instance
        AWSIotMqttClient awsIotClient = getAwsIotClient();
        awsIotClient.connect();
        awsIotClient.publish("sensors/" + device.getName() + "/temperature", content);

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> !testRestClient.getTimeseriesKeys(device.getId()).isEmpty());

        List<TsKvEntry> latestTimeseries = testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY));
        assertThat(latestTimeseries).hasSize(1);
        assertThat(latestTimeseries.get(0).getKey()).isEqualTo(TELEMETRY_KEY);
        assertThat(latestTimeseries.get(0).getValue().toString()).isEqualTo(value);

        //upload second telemetry to aws iot instance
        String value2 = "45";
        String content2 = createPayloadForUplink(value2).toString();

        awsIotClient.publish("sensors/" + device.getName() + "/temperature", content2);
        awsIotClient.disconnect();

        Awaitility
                .await()
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> value2.equals(testRestClient.getLatestTimeseries(device.getId(), List.of(TELEMETRY_KEY)).get(0).getValue().toString()));
    }

    @Test
    public void checkDownlinkMessageWasSent() throws Exception {
        createIntegration(AWS_IOT, getIntegrationConfig(), CONFIG_CONVERTER, DOWNLINK_CONVERTER_CONFIGURATION, ROUTING_KEY, SECRET_KEY, false);

        //subscribe for aws iot topic
        AWSIotMqttClient awsIotClient = getAwsIotClient();
        awsIotClient.connect();

        TopicListener topicListener = new TopicListener("sensors/device/upload", AWSIotQos.QOS0);
        awsIotClient.subscribe(topicListener, true);

        //add downlink node
        RuleChainId ruleChainId = createRootRuleChainWithIntegrationDownlinkNode(integration.getId());

        //create 4 attributes (stringKey, booleanKey, doubleKey, longKey)
        JsonNode attributes = JacksonUtil.toJsonNode(createPayload().toString());
        testRestClient.saveEntityAttributes(DEVICE, device.getId().toString(), SHARED_SCOPE, attributes);

        RuleChainMetaData ruleChainMetadata = testRestClient.getRuleChainMetadata(ruleChainId);
        RuleNode downlinkNode = ruleChainMetadata.getNodes().stream().filter(ruleNode -> ruleNode.getType().equals("org.thingsboard.rule.engine.integration.TbIntegrationDownlinkNode")).findFirst().get();
        waitTillRuleNodeReceiveMsg(downlinkNode.getId(), EventType.DEBUG_RULE_NODE, integration.getTenantId(), "ATTRIBUTES_UPDATED");

        //check downlink message
        JsonNode actual = JacksonUtil.toJsonNode(topicListener.getMessages().take().getStringPayload());

        assertThat(actual.get("stringKey")).isEqualTo(attributes.get("stringKey"));
        assertThat(actual.get("booleanKey")).isEqualTo(attributes.get("booleanKey"));
        assertThat(actual.get("doubleKey")).isEqualTo(attributes.get("doubleKey"));
        assertThat(actual.get("longKey")).isEqualTo(attributes.get("longKey"));
    }

    @Override
    protected String getDevicePrototypeSufix() {
        return "mqtt_";
    }

    private static class TopicListener extends TestTopicListener {
        private final BlockingQueue<AWSIotMessage> messages;

        private TopicListener(String topic, AWSIotQos qos) {
            super(topic, qos);
            messages = new ArrayBlockingQueue<>(100);
        }

        @Override
        public void onMessage(AWSIotMessage message) {
            log.info("Message received[{}]", message);
            messages.add(message);
        }

        public BlockingQueue<AWSIotMessage> getMessages() {
            return messages;
        }
    }

    private JsonNode getIntegrationConfig() throws IOException {
        String rootCA = new String(Files.readAllBytes(Paths.get(ROOT_CA))).replace("\n", "\\n");
        String cert = new String(Files.readAllBytes(Paths.get(CERTIFICATE))).replace("\n", "\\n");
        String privateKey = new String(Files.readAllBytes(Paths.get(PRIVATE_KEY))).replace("\n", "\\n");
        return defaultConfig(CLIENT_ENDPOINT, rootCA, cert, privateKey);
    }

    private JsonNode createPayloadForUplink(String value) {
        ObjectNode values = JacksonUtil.newObjectNode();
        values.put("value", value);
        return values;
    }

    private AWSIotMqttClient getAwsIotClient() {
        String clientId = RandomStringUtils.randomAlphanumeric(10);

        SampleUtil.KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(CERTIFICATE, PRIVATE_KEY);
        AWSIotMqttClient awsIotClient = new AWSIotMqttClient(CLIENT_ENDPOINT, clientId, pair.keyStore, pair.keyPassword);

        return awsIotClient;
    }
}
