// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.prototypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;

public class AwsIotIntegrationPrototypes {
    private static final String CONFIG_INTEGRATION = "{\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"host\": \"%s\",\n" +
            "    \"port\": 8883,\n" +
            "    \"clientId\": \"\",\n" +
            "    \"connectTimeoutSec\": 10,\n" +
            "    \"ssl\": true,\n" +
            "    \"maxBytesInMessage\": 32368,\n" +
            "    \"credentials\": {\n" +
            "      \"type\": \"cert.PEM\",\n" +
            "      \"caCertFileName\": \"rootCA.pem\",\n" +
            "      \"caCert\": \"%s\",\n" +
            "      \"certFileName\": \"cert.crt\",\n" +
            "      \"cert\": \"%s\",\n" +
            "      \"privateKeyFileName\": \"private.key\",\n" +
            "      \"privateKey\": \"%s\",\n" +
            "      \"password\": \"\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"downlinkTopicPattern\": \"${topic}\",\n" +
            "  \"topicFilters\": [\n" +
            "    {\n" +
            "      \"filter\": \"sensors/+/temperature\",\n" +
            "      \"qos\": 0\n" +
            "    }\n" +
            "  ],\n" +
            "  \"metadata\": {}\n" +
            "}";

    public static JsonNode defaultConfig(String endpoint, String caCert, String cert, String privateKey){
        return JacksonUtil.toJsonNode(String.format(CONFIG_INTEGRATION, endpoint, caCert, cert, privateKey));
    }
}
