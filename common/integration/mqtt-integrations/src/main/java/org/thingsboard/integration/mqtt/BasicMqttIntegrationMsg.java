// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.util.ConvertUtil;

import java.nio.charset.StandardCharsets;

@Data
public class BasicMqttIntegrationMsg implements MqttIntegrationMsg {

    private final String topic;
    private final byte[] payload;

    public BasicMqttIntegrationMsg(String topic, ByteBuf payload) {
        this.topic = topic;
        this.payload = new byte[payload.readableBytes()];
        payload.readBytes(this.payload);
    }

    @Override
    public JsonNode toJson() {
        ObjectNode json = JacksonUtil.newObjectNode().put("topic", topic);
        ConvertUtil.putJson(json, payload);
        return json;
    }

    @Override
    public ContentType getContentType() {
        try {
            JsonNode node = JacksonUtil.fromBytes(payload);
            if (node != null) {
                return ContentType.JSON;
            }
        } catch (Exception ignored) {
        }
        if (StringUtils.isAsciiPrintable(new String(payload, StandardCharsets.UTF_8))) {
            return ContentType.TEXT;
        }
        return ContentType.BINARY;
    }
}
