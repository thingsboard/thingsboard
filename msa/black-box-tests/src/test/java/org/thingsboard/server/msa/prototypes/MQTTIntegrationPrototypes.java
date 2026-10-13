// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.prototypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;

public class MQTTIntegrationPrototypes {

    private static final String CONFIG_INTEGRATION = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"%s\",\n" +
            "    \"port\":%d ,\n" +
            "    \"cleanSession\": true,\n" +
            "    \"ssl\": false,\n" +
            "    \"connectTimeoutSec\": 30,\n" +
            "    \"clientId\": \"\",\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"anonymous\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"downlinkTopicPattern\": \"%s\",\n" +
            "  \"topicFilters\": [\n" +
            "    {\n" +
            "      \"filter\": \"tb/mqtt/device\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";

    private static final String CONFIG_INTEGRATION_WITH_BASIC_CREDS = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"%s\",\n" +
            "    \"port\":%d ,\n" +
            "    \"cleanSession\": true,\n" +
            "    \"ssl\": false,\n" +
            "    \"connectTimeoutSec\": 30,\n" +
            "    \"clientId\": \"\",\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"basic\",\n" +
            "      \"username\": \"username\",\n" +
            "      \"password\": \"%s\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"downlinkTopicPattern\": \"%s\",\n" +
            "  \"topicFilters\": [\n" +
            "    {\n" +
            "      \"filter\": \"tb/mqtt/device\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";

    private static final String CONFIG_INTEGRATION_WITH_PEM_CREDS = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"%s\",\n" +
            "    \"port\":%d ,\n" +
            "    \"cleanSession\": true,\n" +
            "    \"ssl\": false,\n" +
            "    \"connectTimeoutSec\": 30,\n" +
            "    \"clientId\": \"\",\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"cert.PEM\",\n" +
            "      \"caCert\": \"%s\",\n" +
            "      \"caCertFileName\": \"caCertFileName.pem\",\n" +
            "      \"cert\": \"%s\",\n" +
            "      \"certFileName\": \"certFileName.pem\",\n" +
            "      \"privateKey\": \"%s\",\n" +
            "      \"privateKeyFileName\": \"privateKeyFileName.pem\",\n" +
            "      \"privateKeyPassword\": \"12345\",\n" +
            "    }\n" +
            "  },\n" +
            "  \"downlinkTopicPattern\": \"%s\",\n" +
            "  \"topicFilters\": [\n" +
            "    {\n" +
            "      \"filter\": \"tb/mqtt/device\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";

    public static JsonNode defaultConfig(String serviceName, int servicePort, String topic) {
        return JacksonUtil.toJsonNode(String.format(CONFIG_INTEGRATION, serviceName, servicePort, topic));
    }

    public static JsonNode configWithBasicCreds(String serviceName, int servicePort, String password, String topic) {
        return JacksonUtil.toJsonNode(String.format(CONFIG_INTEGRATION_WITH_BASIC_CREDS, serviceName, servicePort, password, topic));
    }

    public static JsonNode configWithPemCreds(String serviceName, int servicePort, String topic, String pem) {
        return JacksonUtil.toJsonNode(String.format(CONFIG_INTEGRATION_WITH_PEM_CREDS, serviceName, servicePort, topic, pem, pem, pem));
    }

}
