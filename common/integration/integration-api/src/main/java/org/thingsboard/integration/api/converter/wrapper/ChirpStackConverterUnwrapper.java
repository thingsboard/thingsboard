// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter.wrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.server.common.data.util.TbPair;

import java.util.Base64;

public class ChirpStackConverterUnwrapper extends AbstractConverterUnwrapper {

    private static final ImmutableMap<String, String> KEYS_MAPPING;

    static {
        KEYS_MAPPING = new ImmutableMap.Builder<String, String>()
                .put("deduplicationId", "/deduplicationId")
                .put("time", "/time")
                .put("tenantId", "/deviceInfo/tenantId")
                .put("tenantName", "/deviceInfo/tenantName")
                .put("applicationId", "/deviceInfo/applicationId")
                .put("applicationName", "/deviceInfo/applicationName")
                .put("deviceProfileId", "/deviceInfo/deviceProfileId")
                .put("deviceProfileName", "/deviceInfo/deviceProfileName")
                .put("deviceName", "/deviceInfo/deviceName")
                .put("eui", "/deviceInfo/devEui")
                .put("tags", "/deviceInfo/tags")
                .put("devAddr", "/devAddr")
                .put("adr", "/adr")
                .put("dr", "/dr")
                .put("fCnt", "/fCnt")
                .put("fPort", "/fPort")
                .put("confirmed", "/confirmed")
                .put("data", "/data")
                .put("decoded", "/object")
                .put("rxInfo", "/rxInfo")
                .put("frequency", "/txInfo/frequency")
                .put("bandwidth", "/txInfo/modulation/lora/bandwidth")
                .put("spreadingFactor", "/txInfo/modulation/lora/spreadingFactor")
                .put("codeRate", "/txInfo/modulation/lora/codeRate")
                .put("latitude", "/location/latitude")
                .put("longitude", "/location/longitude")
                .put("altitude", "/location/altitude")
                .put("rssi", "")
                .put("snr", "")
                .build();
    }

    @Override
    protected String getGatewayInfoPath() {
        return "/rxInfo";
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) {
        if (payloadJson.has("object")) {
            var decoded = payloadJson.get("object");
            return TbPair.of(JacksonUtil.writeValueAsBytes(decoded), ContentType.JSON);
        } else if (payloadJson.has("data")) {
            var data = payloadJson.get("data").textValue();
            return TbPair.of(Base64.getDecoder().decode(data), ContentType.BINARY);
        } else {
            return TbPair.of(EMPTY_BYTE_ARRAY, ContentType.BINARY);
        }
    }

    @Override
    protected ImmutableMap<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }
}
