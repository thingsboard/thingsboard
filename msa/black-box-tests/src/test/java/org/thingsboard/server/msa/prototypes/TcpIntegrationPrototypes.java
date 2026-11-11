// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.prototypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;

public class TcpIntegrationPrototypes {
    private static final String JSON_INTEGRATION_CONFIG = "{\"clientConfiguration\":{" +
            "\"port\":%d," +
            "\"soBacklogOption\":128," +
            "\"soRcvBuf\":64," +
            "\"soSndBuf\":64," +
            "\"soKeepaliveOption\":false," +
            "\"tcpNoDelay\":true," +
            "\"cacheSize\":1000," +
            "\"timeToLiveInMinutes\":1440," +
            "\"handlerConfiguration\":{\"handlerType\":\"JSON\"}},\"metadata\":{}}";

    private static final String TEXT_INTEGRATION_CONFIG = "{\"clientConfiguration\":{" +
            "\"port\":%d," +
            "\"soBacklogOption\":128," +
            "\"soRcvBuf\":64," +
            "\"soSndBuf\":64," +
            "\"soKeepaliveOption\":false," +
            "\"tcpNoDelay\":true," +
            "\"cacheSize\":1000," +
            "\"timeToLiveInMinutes\":1440," +
            "\"handlerConfiguration\":{" +
            "  \"handlerType\": \"TEXT\",\n" +
            "  \"byteOrder\": \"LITTLE_ENDIAN\",\n" +
            "  \"maxFrameLength\": 128,\n" +
            "  \"lengthFieldOffset\": 0,\n" +
            "  \"lengthFieldLength\": 2,\n" +
            "  \"lengthAdjustment\": 0,\n" +
            "  \"initialBytesToStrip\": 0,\n" +
            "  \"failFast\": false,\n" +
            "  \"stripDelimiter\": true,\n" +
            "  \"messageSeparator\": \"SYSTEM_LINE_SEPARATOR\"}},\"metadata\":{}}";

    private static final String BINARY_INTEGRATION_CONFIG = "{\"clientConfiguration\":{" +
            "\"port\":%d," +
            "\"soBacklogOption\":128," +
            "\"soRcvBuf\":64," +
            "\"soSndBuf\":64," +
            "\"soKeepaliveOption\":false," +
            "\"tcpNoDelay\":true," +
            "\"cacheSize\":1000," +
            "\"timeToLiveInMinutes\":1440," +
            "\"handlerConfiguration\":{\n" +
            "  \"handlerType\": \"BINARY\",\n" +
            "  \"byteOrder\": \"LITTLE_ENDIAN\",\n" +
            "  \"maxFrameLength\": 128,\n" +
            "  \"lengthFieldOffset\": 4,\n" +
            "  \"lengthFieldLength\": 1,\n" +
            "  \"lengthAdjustment\": 0,\n" +
            "  \"initialBytesToStrip\": 5,\n" +
            "  \"failFast\": false,\n" +
            "  \"stripDelimiter\": true,\n" +
            "  \"messageSeparator\": \"SYSTEM_LINE_SEPARATOR\"\n" +
            "}},\"metadata\":{}}";

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
