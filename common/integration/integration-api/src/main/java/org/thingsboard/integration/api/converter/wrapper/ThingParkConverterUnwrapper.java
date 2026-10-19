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

public class ThingParkConverterUnwrapper extends AbstractConverterUnwrapper {

    private static final ImmutableMap<String, String> KEYS_MAPPING;

    static {
        KEYS_MAPPING = new ImmutableMap.Builder<String, String>()
                .put("time", "/DevEUI_uplink/Time")
                .put("eui", "/DevEUI_uplink/DevEUI")
                .put("fPort", "/DevEUI_uplink/FPort")
                .put("fCnt", "/DevEUI_uplink/FCntUp")
                .put("lostUplinksAs", "/DevEUI_uplink/LostUplinksAS")
                .put("adr", "/DevEUI_uplink/ADRbit")
                .put("mType", "/DevEUI_uplink/MType")
                .put("fCntDn", "/DevEUI_uplink/FCntDn")
                .put("data", "/DevEUI_uplink/payload_hex")
                .put("micHex", "/DevEUI_uplink/mic_hex")
                .put("lrcid", "/DevEUI_uplink/Lrcid")
                .put("rssi", "/DevEUI_uplink/LrrRSSI")
                .put("snr", "/DevEUI_uplink/LrrSNR")
                .put("esp", "/DevEUI_uplink/LrrESP")
                .put("spreadingFactor", "/DevEUI_uplink/SpFact")
                .put("bandwidth", "/DevEUI_uplink/SubBand")
                .put("channel", "/DevEUI_uplink/Channel")
                .put("lrrId", "/DevEUI_uplink/Lrrid")
                .put("late", "/DevEUI_uplink/Late")
                .put("latitude", "/DevEUI_uplink/LrrLAT")
                .put("longitude", "/DevEUI_uplink/LrrLON")
                .put("lrr", "/DevEUI_uplink/Lrrs/Lrr")
                .put("devLrrCnt", "/DevEUI_uplink/DevLrrCnt")
                .put("customerId", "/DevEUI_uplink/CustomerID")
                .put("customerData", "/DevEUI_uplink/CustomerData")
                .put("baseStationData", "/DevEUI_uplink/BaseStationData")
                .put("modelCfg", "/DevEUI_uplink/ModelCfg")
                .put("driverCfg", "/DevEUI_uplink/DriverCfg")
                .put("instantPer", "/DevEUI_uplink/InstantPER")
                .put("meanPer", "/DevEUI_uplink/MeanPER")
                .put("devAddr", "/DevEUI_uplink/DevAddr")
                .put("ackRequested", "/DevEUI_uplink/AckRequested")
                .put("rawMacCommands", "/DevEUI_uplink/rawMacCommands")
                .put("txPower", "/DevEUI_uplink/TxPower")
                .put("nbTrans", "/DevEUI_uplink/NbTrans")
                .put("frequency", "/DevEUI_uplink/Frequency")
                .put("dynamicClass", "/DevEUI_uplink/DynamicClass")
                .put("payloadEncryption", "/DevEUI_uplink/PayloadEncryption")
                .put("decoded", "/DevEUI_uplink/payload")
                .put("points", "/DevEUI_uplink/points")
                .put("downlinkUrl", "/DevEUI_uplink/downlinkUrl")
                .build();
    }

    @Override
    protected TbPair<byte[], ContentType> getPayload(JsonNode payloadJson) throws DecoderException {
        var uplink = payloadJson.get("DevEUI_uplink");
        if (uplink.has("payload")) {
            var decoded = uplink.get("payload");
            return TbPair.of(JacksonUtil.writeValueAsBytes(decoded), ContentType.JSON);
        } else if (uplink.has("payload_hex")) {
            var data = uplink.get("payload_hex").textValue();
            return TbPair.of(Hex.decodeHex(data.toCharArray()), ContentType.BINARY);
        } else {
            return TbPair.of(EMPTY_BYTE_ARRAY, ContentType.BINARY);
        }
    }

    @Override
    protected ImmutableMap<String, String> getKeysMapping() {
        return KEYS_MAPPING;
    }
}
