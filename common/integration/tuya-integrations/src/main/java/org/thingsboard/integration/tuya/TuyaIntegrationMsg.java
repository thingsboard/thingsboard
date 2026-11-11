// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Map;

@Data
public class TuyaIntegrationMsg {

    private Map<String, String> deviceMetadata;
    private JsonNode json;

    TuyaIntegrationMsg(JsonNode json, Map<String, String> deviceMetadata) {
        this.json = json;
        this.deviceMetadata = deviceMetadata;
    }

    public String toString() {
        ObjectNode msgInJson = JacksonUtil.newObjectNode();
        msgInJson.set("metadata", JacksonUtil.convertValue(deviceMetadata, ObjectNode.class));
        msgInJson.set("data", json);
        return msgInJson.toString();
    }
}
