// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.azure;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;

import java.util.Map;

@Data
public class AzureServiceBusIntegrationMsg {

    private final ServiceBusReceivedMessageContext context;

    public AzureServiceBusIntegrationMsg(ServiceBusReceivedMessageContext context) {
        this.context = context;
    }

    public byte[] getPayload() {
        return this.context.getMessage().getBody().toBytes();
    }

    public Map<String, Object> getSystemProperties() {
        return this.context.getMessage().getApplicationProperties();
    }

    public JsonNode toJson() {
        ObjectNode json = JacksonUtil.newObjectNode();
        Map<String, Object> properties = this.context.getMessage().getApplicationProperties();
        ObjectNode sysPropsJson =  JacksonUtil.newObjectNode();
        properties.forEach(
                (key, val) -> {
                    if (val != null) {
                        sysPropsJson.put(key, val.toString());
                    }
                }
        );
        json.set("systemProperties", sysPropsJson);
        JsonNode payloadJson = JacksonUtil.fromBytes(this.context.getMessage().getBody().toBytes());
        if (payloadJson != null) {
            json.set("payload", payloadJson);
        } else {
            json.put("payload", this.context.getMessage().getBody().toBytes());
        }
        return json;
    }

}
