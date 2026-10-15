// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.aws.kinesis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.util.ConvertUtil;

import java.nio.ByteBuffer;

@Data
public class KinesisIntegrationMsg {

    private final String shardId;
    private final String sequenceNumber;
    private final String partitionKey;
    private final byte[] payload;

    public KinesisIntegrationMsg(String shardId, String sequenceNumber, ByteBuffer payload, String partitionKey) {
        this.shardId = shardId;
        this.sequenceNumber = sequenceNumber;
        this.payload = new byte[payload.remaining()];
        payload.get(this.payload);
        this.partitionKey = partitionKey;
    }

    public JsonNode toJson() {
        ObjectNode json = JacksonUtil.newObjectNode();
        json.put("shardId", shardId);
        json.put("sequenceNumber", sequenceNumber);
        json.put("partitionKey", partitionKey);
        ConvertUtil.putJson(json, payload);
        return json;
    }
}
