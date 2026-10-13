// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.netty.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.mqtt.basic.BasicMqttIntegration;
import org.thingsboard.mqtt.MqttClient;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractMqttIntegrationTest {

    AbstractMqttIntegration basicMqttIntegration;
    MqttClient mqttClient;

    String config = "{\n" +
            "   \"clientConfiguration\":{\n" +
            "      \"host\":\"localhost\",\n" +
            "      \"port\":1883,\n" +
            "      \"cleanSession\":false,\n" +
            "      \"ssl\":false,\n" +
            "      \"connectTimeoutSec\":10,\n" +
            "      \"clientId\":\"clientId\",\n" +
            "      \"maxBytesInMessage\":32368,\n" +
            "      \"credentials\":{\n" +
            "         \"type\":\"anonymous\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"downlinkTopicPattern\":\"${topic}\",\n" +
            "   \"topicFilters\":[\n" +
            "      {\n" +
            "         \"filter\":\"my/data\",\n" +
            "         \"qos\":1\n" +
            "      },\n" +
            "      {\n" +
            "         \"filter\":\"test/topic\",\n" +
            "         \"qos\":1\n" +
            "      }\n" +
            "   ],\n" +
            "   \"metadata\":{\n" +
            "      \n" +
            "   }\n" +
            "}";

    String configCleanSession = "{\n" +
            "   \"clientConfiguration\":{\n" +
            "      \"host\":\"localhost\",\n" +
            "      \"port\":1883,\n" +
            "      \"cleanSession\":true,\n" +
            "      \"ssl\":false,\n" +
            "      \"connectTimeoutSec\":10,\n" +
            "      \"clientId\":\"clientId\",\n" +
            "      \"maxBytesInMessage\":32368,\n" +
            "      \"credentials\":{\n" +
            "         \"type\":\"anonymous\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"downlinkTopicPattern\":\"${topic}\",\n" +
            "   \"topicFilters\":[\n" +
            "      {\n" +
            "         \"filter\":\"my/data\",\n" +
            "         \"qos\":1\n" +
            "      },\n" +
            "      {\n" +
            "         \"filter\":\"test/topic\",\n" +
            "         \"qos\":1\n" +
            "      }\n" +
            "   ],\n" +
            "   \"metadata\":{\n" +
            "      \n" +
            "   }\n" +
            "}";

    String configNoClientId = "{\n" +
            "   \"clientConfiguration\":{\n" +
            "      \"host\":\"localhost\",\n" +
            "      \"port\":1883,\n" +
            "      \"cleanSession\":false,\n" +
            "      \"ssl\":false,\n" +
            "      \"connectTimeoutSec\":10,\n" +
            "      \"clientId\":\"\",\n" +
            "      \"maxBytesInMessage\":32368,\n" +
            "      \"credentials\":{\n" +
            "         \"type\":\"anonymous\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"downlinkTopicPattern\":\"${topic}\",\n" +
            "   \"topicFilters\":[\n" +
            "      {\n" +
            "         \"filter\":\"my/data\",\n" +
            "         \"qos\":1\n" +
            "      },\n" +
            "      {\n" +
            "         \"filter\":\"test/topic\",\n" +
            "         \"qos\":1\n" +
            "      }\n" +
            "   ],\n" +
            "   \"metadata\":{\n" +
            "      \n" +
            "   }\n" +
            "}";

    String configClientIdLong = "{\n" +
            "   \"clientConfiguration\":{\n" +
            "      \"host\":\"localhost\",\n" +
            "      \"port\":1883,\n" +
            "      \"cleanSession\":true,\n" +
            "      \"ssl\":false,\n" +
            "      \"connectTimeoutSec\":10,\n" +
            "      \"clientId\":\"" + "X".repeat(257) + "\",\n" +
            "      \"maxBytesInMessage\":32368,\n" +
            "      \"credentials\":{\n" +
            "         \"type\":\"anonymous\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"downlinkTopicPattern\":\"${topic}\",\n" +
            "   \"topicFilters\":[\n" +
            "      {\n" +
            "         \"filter\":\"my/data\",\n" +
            "         \"qos\":1\n" +
            "      },\n" +
            "      {\n" +
            "         \"filter\":\"test/topic\",\n" +
            "         \"qos\":1\n" +
            "      }\n" +
            "   ],\n" +
            "   \"metadata\":{\n" +
            "      \n" +
            "   }\n" +
            "}";

    @BeforeEach
    void setUp() throws Exception {
        Integration configuration = getIntegration(config);

        mqttClient = mock(MqttClient.class);
        Future<Void> future = mock(Future.class);
        when(mqttClient.off(anyString())).thenReturn(future);
        when(future.get(anyInt(), any())).thenReturn(null);

        basicMqttIntegration = new BasicMqttIntegration();
        basicMqttIntegration.setMqttClient(mqttClient);
        basicMqttIntegration.setConfiguration(configuration);
    }

    @Test
    void testMqttIntegrationUnsubscribe1() throws Exception {
        processTest(config, 1);
    }

    @Test
    void testMqttIntegrationUnsubscribe2() throws Exception {
        processTest(configCleanSession, 0);
    }

    @Test
    void testMqttIntegrationUnsubscribe3() throws Exception {
        processTest(configNoClientId, 0);
    }

    @Test
    void testMqttIntegrationClientIdTooLong() {
        Integration integration = getIntegration(configClientIdLong);

        String clientId = "X".repeat(257);
        assertThatThrownBy(() -> basicMqttIntegration.validateConfiguration(integration, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The length of Client ID cannot be longer than 256, but current length is " + clientId.length() + ".");
    }

    private void processTest(String config, int wantedNumberOfInvocations) throws Exception {
        JsonNode jsonNode = JacksonUtil.toJsonNode(config);
        ArrayNode topicFilters = (ArrayNode) jsonNode.get("topicFilters");
        topicFilters.remove(1);

        Integration configuration = getIntegration(JacksonUtil.toString(jsonNode));

        basicMqttIntegration.sendUnsubscribeRequestsIfNeeded(configuration);

        verify(mqttClient, times(wantedNumberOfInvocations)).off(any());
    }

    private Integration getIntegration(String configuration) {
        Integration integration = new Integration();
        integration.setName("test");
        integration.setType(IntegrationType.MQTT);
        integration.setConfiguration(JacksonUtil.toJsonNode(configuration));
        return integration;
    }
}