// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Base64;
import java.util.Map;

public class ThingsStackConverterUnwrapper extends AbstractConverterUnwrapper {

    private static final ImmutableMap<String, String> KEYS_MAPPING;

    static {
        KEYS_MAPPING = new ImmutableMap.Builder<String, String>()
                .put("deviceId", "/end_device_ids/device_id")
                .put("applicationId", "/end_device_ids/application_ids/application_id")
                .put("eui", "/end_device_ids/dev_eui")
                .put("joinEui", "/end_device_ids/join_eui")
                .put("devAddr", "/end_device_ids/dev_addr")
                .put("correlationIds", "/correlation_ids")
                .put("receivedAt", "/received_at")
                .put("sessionKeyId", "/uplink_message/session_key_id")
                .put("fPort", "/uplink_message/f_port")
                .put("fCnt", "/uplink_message/f_cnt")
                .put("data", "/uplink_message/frm_payload")
                .put("decoded", "/uplink_message/decoded_payload")
                .put("rxMetadata", "/uplink_message/rx_metadata")
                .put("bandwidth", "/uplink_message/settings/data_rate/lora/bandwidth")
                .put("spreadingFactor", "/uplink_message/settings/data_rate/lora/spreading_factor")
                .put("dataRateIndex", "/uplink_message/settings/data_rate_index")
                .put("codeRate", "/uplink_message/settings/coding_rate")
                .put("frequency", "/uplink_message/settings/frequency")
                .put("timestamp", "/uplink_message/settings/timestamp")
                .put("time", "/uplink_message/settings/time")
                .put("consumedAirtime", "/uplink_message/consumed_airtime")
                .put("latitude", "/uplink_message/locations/user/latitude")
                .put("longitude", "/uplink_message/locations/user/longitude")
                .put("altitude", "/uplink_message/locations/user/altitude")
                .put("source", "/uplink_message/locations/user/source")
                .put("brandId", "/uplink_message/version_ids/brand_id")
                .put("modelId", "/uplink_message/version_ids/model_id")
                .put("hardwareVersion", "/uplink_message/version_ids/hardware_version")
                .put("firmwareVersion", "/uplink_message/version_ids/firmware_version")
                .put("bandId", "/uplink_message/version_ids/band_id")
                .put("netId", "/uplink_message/network_ids/net_id")
                .put("nsId", "/uplink_message/network_ids/ns_id")
                .put("tenantId", "/uplink_message/network_ids/tenant_id")
                .put("clusterId", "/uplink_message/network_ids/cluster_id")
                .put("clusterAddress", "/uplink_message/network_ids/cluster_address")
                .put("tenantAddress", "/uplink_message/network_ids/tenant_address")
                .put("attributes", "/uplink_message/attributes")
                .put("uplinkMessageReceivedAt", "/uplink_message/received_at")
                .put("simulated", "/simulated")
                .put("rssi", "")
                .put("snr", "")
                .build();
    }

    @Override
    protected String getGatewayInfoPath() {
        return "/uplink_message/rx_metadata";
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) {
        var uplink = payloadJson.get("uplink_message");
        if (uplink.has("decoded_payload")) {
            var decoded = uplink.get("decoded_payload");
            return TbPair.of(JacksonUtil.writeValueAsBytes(decoded), ContentType.JSON);
        } else if (uplink.has("frm_payload")) {
            var data = uplink.get("frm_payload").textValue();
            return TbPair.of(Base64.getDecoder().decode(data), ContentType.BINARY);
        } else {
            return TbPair.of(EMPTY_BYTE_ARRAY, ContentType.BINARY);
        }
    }

    @Override
    protected void postMapping(Map<String, Object> kvMap) {
        long ts = 0;
        if (kvMap.containsKey("uplinkMessageReceivedAt")) {
            var uplinkMessageReceivedAtTs = parseDateToTimestamp(kvMap.get("uplinkMessageReceivedAt").toString());
            kvMap.put("uplinkMessageReceivedAtTs", uplinkMessageReceivedAtTs);
            ts = uplinkMessageReceivedAtTs;
        }
        if (kvMap.containsKey("time")) {
            var timeTs = parseDateToTimestamp(kvMap.get("time").toString());
            kvMap.put("timeTs", timeTs);
        }
        if (kvMap.containsKey("receivedAt")) {
            var receivedAtTs = parseDateToTimestamp(kvMap.get("receivedAt").toString());
            kvMap.put("receivedAtTs", receivedAtTs);
            if (ts == 0) {
                ts = receivedAtTs;
            }
        }
        if (ts == 0) {
            ts = System.currentTimeMillis();
        }
        kvMap.put("ts", ts);
    }

    @Override
    protected ImmutableMap<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }
}
