// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.prototypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;

public class UdpIntegrationPrototypes {

    private static final String JSON_INTEGRATION_CONFIG = "{\"clientConfiguration\":{" +
            "\"port\":%d," +
            "\"soBroadcast\":true," +
            "\"soRcvBuf\":64," +
            "\"cacheSize\":1000," +
            "\"timeToLiveInMinutes\":1440," +
            "\"handlerConfiguration\":{\"handlerType\":\"JSON\"}},\"metadata\":{}}";

    private static final String TEXT_INTEGRATION_CONFIG = "{\n" +
            "  \"metadata\": {},\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"port\": 11560,\n" +
            "    \"soBroadcast\": true,\n" +
            "    \"soRcvBuf\": 64,\n" +
            "    \"cacheSize\": 1000,\n" +
            "    \"timeToLiveInMinutes\": 1440,\n" +
            "    \"handlerConfiguration\": {\n" +
            "      \"handlerType\": \"TEXT\",\n" +
            "      \"charsetName\": \"UTF-8\",\n" +
            "      \"maxFrameLength\": 128\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final String BINARY_INTEGRATION_CONFIG = "{\n" +
            "  \"metadata\": {},\n" +
            "  \"clientConfiguration\": {\n" +
            "    \"port\": 11560,\n" +
            "    \"soBroadcast\": true,\n" +
            "    \"soRcvBuf\": 64,\n" +
            "    \"cacheSize\": 1000,\n" +
            "    \"timeToLiveInMinutes\": 1440,\n" +
            "    \"handlerConfiguration\": {\n" +
            "      \"handlerType\": \"BINARY\",\n" +
            "      \"charsetName\": \"UTF-8\",\n" +
            "      \"maxFrameLength\": 128\n" +
            "    }\n" +
            "  }\n" +
            "}";

    public static JsonNode defaultJsonConfig(int port){
        return JacksonUtil.toJsonNode(String.format(JSON_INTEGRATION_CONFIG, port));
    }

    public static JsonNode defaultTextConfig(int port){
        return JacksonUtil.toJsonNode(String.format(TEXT_INTEGRATION_CONFIG, port));
    }

    public static JsonNode defaultBinaryConfig(int port){
        return JacksonUtil.toJsonNode(String.format(BINARY_INTEGRATION_CONFIG, port));
    }
}
