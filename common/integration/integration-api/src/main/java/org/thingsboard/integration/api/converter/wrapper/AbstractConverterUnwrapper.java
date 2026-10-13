// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.util.TbPair;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class AbstractConverterUnwrapper implements ConverterUnwrapper {

    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @Override
    public TbPair<byte[], UplinkMetaData<Object>> unwrap(byte[] payload, UplinkMetaData metadata) throws Exception {
        JsonNode payloadJson = JacksonUtil.fromBytes(payload);

        Map<String, Object> kvMap = new TreeMap<>(metadata.getKvMap());

        getKeysMapping().forEach((name, path) -> {
            if (path.isEmpty()) {
                return;
            }
            JsonNode value = payloadJson.at(path);
            if (!value.isMissingNode() && !value.isNull()) {
                kvMap.put(name, JacksonUtil.convertValue(value, Object.class));
            }
        });

        addGatewayAdditionalInfo(kvMap, payloadJson);

        postMapping(kvMap);
        kvMap.putAll(metadata.getKvMap());
        TbPair<byte[], ContentType> payloadPair = getPayload(payloadJson);
        kvMap.put("payloadFormat", payloadPair.getSecond().name());
        UplinkMetaData<Object> mergedMetadata = new UplinkMetaData<>(payloadPair.getSecond(), kvMap);

        return TbPair.of(payloadPair.getFirst(), mergedMetadata);
    }

    protected void addGatewayAdditionalInfo(Map<String, Object> kvMap, JsonNode payloadJson) {
        JsonNode rxMetadataArray = payloadJson.at(getGatewayInfoPath());
        if (!rxMetadataArray.isEmpty()) {
            JsonNode rxMetadata = findByMaxRssi(rxMetadataArray);
            if (rxMetadata != null) {
                JsonNode rssiNode = rxMetadata.get("rssi");
                if (rssiNode != null) {
                    kvMap.put("rssi", rssiNode.asInt());
                }

                JsonNode snrNode = rxMetadata.get("snr");
                if (snrNode != null) {
                    kvMap.put("snr", snrNode.asDouble());
                }
            }
        }
    }

    protected String getGatewayInfoPath() {
        return "";
    }

    protected JsonNode findByMaxRssi(JsonNode gwArray) {
        JsonNode result = null;
        int maxRssi = Integer.MIN_VALUE;

        for (JsonNode node : gwArray) {
            JsonNode rssiNode = node.get("rssi");
            if (rssiNode != null && rssiNode.isNumber()) {
                int rssi = rssiNode.asInt();

                if (rssi > maxRssi) {
                    maxRssi = rssi;
                    result = node;
                }
            }
        }
        return result;
    }

    protected abstract TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) throws Exception;

    protected void postMapping(Map<String, Object> kvMap) {
        long ts = 0;
        if (kvMap.containsKey("time")) {
            var timeTs = parseDateToTimestamp(kvMap.get("time").toString());
            kvMap.put("timeTs", timeTs);
            ts = timeTs;
        }
        if (ts == 0) {
            ts = System.currentTimeMillis();
        }
        kvMap.put("ts", ts);
    }

    protected long parseDateToTimestamp(String dateString) {
        try {
            return OffsetDateTime.parse(dateString).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    // Key is a name in metadata, Value is a JSON path to value in payload.
    protected abstract ImmutableMap<String, String> getKeysMapping();

    @Override
    public Set<String> getKeys() {
        return getKeysMapping().keySet();
    }

}
