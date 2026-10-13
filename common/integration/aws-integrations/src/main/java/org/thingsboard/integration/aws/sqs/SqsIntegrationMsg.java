// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.aws.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;

/*
 * Created by Valerii Sosliuk on 05.06.19
 */
@Data
public class SqsIntegrationMsg {

    private Map<String,String> deviceMetadata;
    private JsonNode json;
    private byte[] payload;

    public SqsIntegrationMsg(JsonNode json, Map<String,String> deviceMetadata) {
        this.json = json;
        this.payload = json.toString().getBytes();
        this.deviceMetadata = deviceMetadata;
    }

}
