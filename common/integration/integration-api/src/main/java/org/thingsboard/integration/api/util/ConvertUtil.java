// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ConvertUtil {

    public static String toDebugMessage(ContentType messageType, byte[] message) {
        return toDebugMessage(messageType.name(), message);
    }

    public static String toDebugMessage(String messageType, byte[] message) {
        if (message == null) {
            return null;
        }
        switch (messageType) {
            case "JSON":
            case "TEXT":
                return new String(message, StandardCharsets.UTF_8);
            case "BINARY":
                return Base64.getEncoder().encodeToString(message);
            default:
                throw new RuntimeException("Message type: " + messageType + " is not supported!");
        }
    }

    public static void putJson(ObjectNode root, byte[] payload) {
        try {
            JsonNode payloadJson = JacksonUtil.fromBytes(payload);
            root.set("payload", payloadJson);
        } catch (IllegalArgumentException e) {
            root.put("payload", toDebugMessage("JSON", payload));
        }
    }

}
