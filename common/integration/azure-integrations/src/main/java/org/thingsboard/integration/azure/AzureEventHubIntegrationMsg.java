// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.azure;

import com.azure.messaging.eventhubs.EventData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Map;

@Data
public class AzureEventHubIntegrationMsg {

    private final EventData eventData;

    public AzureEventHubIntegrationMsg(EventData eventData) {
        this.eventData = eventData;
    }

    public byte[] getPayload() {
        return this.eventData.getBody();
    }

    public Map<String, Object> getSystemProperties() {
        return this.eventData.getSystemProperties();
    }

    public JsonNode toJson() {
        ObjectNode json = JacksonUtil.newObjectNode();
        Map<String, Object> properties = this.eventData.getSystemProperties();
        ObjectNode sysPropsJson = JacksonUtil.newObjectNode();
        properties.forEach(
                (key, val) -> {
                    if (val != null) {
                        sysPropsJson.put(key, val.toString());
                    }
                }
        );
        json.set("systemProperties", sysPropsJson);
        JsonNode payloadJson = null;
        try {
            payloadJson = JacksonUtil.fromBytes(this.eventData.getBody());
        } catch (IllegalArgumentException e) {
        }
        if (payloadJson != null) {
            json.set("payload", payloadJson);
        } else {
            json.put("payload", this.eventData.getBody());
        }
        return json;
    }

}
