// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

/**
 * Created by Valerii Sosliuk on 3/17/2018.
 */
@Data
@ToString(exclude = "payload")
public class OpcUaIntegrationMsg {

    private Map<String,String> deviceMetadata;
    private JsonNode json;
    private byte[] payload;

    OpcUaIntegrationMsg(JsonNode json, Map<String,String> deviceMetadata) {
        this.json = json;
        this.payload = json.toString().getBytes();
        this.deviceMetadata = deviceMetadata;
    }

    JsonNode toJson() {
        return json;
    }

    byte[] getPayload() {
        return payload;
    }
}
