// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Map;

public class LoriotConverterUnwrapper extends AbstractConverterUnwrapper {

    private static final ImmutableMap<String, String> KEYS_MAPPING;

    static {
        KEYS_MAPPING = new ImmutableMap.Builder<String, String>()
                .put("cmd", "/cmd")
                .put("seqno", "/seqno")
                .put("eui", "/EUI")
                .put("ts", "/ts")
                .put("ack", "/ack")
                .put("battery", "/bat")
                .put("fСnt", "/fcnt")
                .put("fPort", "/port")
                .put("offline", "/offline")
                .put("frequency", "/freq")
                .put("dr", "/dr")
                .put("rssi", "/rssi")
                .put("snr", "/snr")
                .put("toa", "/toa")
                .put("data", "/data")
                .put("decoded", "/decoded")
                .put("encdata", "/encdata")
                .put("gws", "/gws")
                .build();
    }

    @Override
    protected String getGatewayInfoPath() {
        return "/gws";
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) throws DecoderException {
        if (payloadJson.has("decoded")) {
            var decoded = payloadJson.get("decoded");
            return TbPair.of(JacksonUtil.writeValueAsBytes(decoded), ContentType.JSON);
        } else if (payloadJson.has("data")) {
            var data = payloadJson.get("data").textValue();
            return TbPair.of(Hex.decodeHex(data.toCharArray()), ContentType.BINARY);
        } else if (payloadJson.has("encdata")) {
            var encoded = payloadJson.get("encdata").textValue();
            return TbPair.of(Hex.decodeHex(encoded.toCharArray()), ContentType.BINARY);
        } else {
            return TbPair.of(EMPTY_BYTE_ARRAY, ContentType.BINARY);
        }
    }

    @Override
    protected void postMapping(Map<String, Object> kvMap) {
        if (!kvMap.containsKey("ts")) {
            kvMap.put("ts", System.currentTimeMillis());
        }
    }

    @Override
    protected ImmutableMap<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }
}
