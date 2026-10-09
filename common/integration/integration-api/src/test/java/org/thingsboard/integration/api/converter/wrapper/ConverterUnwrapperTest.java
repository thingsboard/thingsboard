// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter.wrapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.util.TbPair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConverterUnwrapperTest {

    @Test
    public void loriotConverterWrapperTest() throws Exception {
        ConverterUnwrapper wrapper = ConverterUnwrapperFactory.getUnwrapper(IntegrationType.LORIOT).get();

        ObjectNode payloadMsg = JacksonUtil.fromString(readPayloadFromFile("LoriotPayload.json"), ObjectNode.class);

        UplinkMetaData<String> uplinkMetaData = new UplinkMetaData<>(ContentType.JSON, Map.of("integrationName", "Loriot integration"));

        Map<String, Object> expectedKvMap = new HashMap<>(uplinkMetaData.getKvMap());
        expectedKvMap.put("data", "2A3F");
        expectedKvMap.put("rssi", -49);
        expectedKvMap.put("snr", 9.2);
        expectedKvMap.put("fPort", 80);
        expectedKvMap.put("eui", "BE7A123456789");
        expectedKvMap.put("gws", List.of(Map.of("rssi", -49, "snr", 9.2), Map.of("rssi", -49, "snr", 8.8)));
        expectedKvMap.put("seqno", 3);
        expectedKvMap.put("toa", 1319);
        expectedKvMap.put("ack", false);
        expectedKvMap.put("dr", "SF12 BW125 4/5");
        expectedKvMap.put("frequency", 868100000);
        expectedKvMap.put("battery", 143);
        expectedKvMap.put("cmd", "gw");
        expectedKvMap.put("fСnt", 2);
        expectedKvMap.put("ts", 1690901187375L);
        expectedKvMap.put("payloadFormat", "BINARY");

        TbPair<byte[], UplinkMetaData<Object>> result = wrapper.unwrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(Hex.decodeHex("2A3F"), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.BINARY, result.getSecond().getContentType());

        Map<String, Object> decoded = Map.of("temperature", 42, "humidity", 63);

        payloadMsg.set("decoded", JacksonUtil.valueToTree(decoded));
        expectedKvMap.put("decoded", decoded);
        expectedKvMap.put("payloadFormat", "JSON");

        result = wrapper.unwrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(JacksonUtil.writeValueAsBytes(decoded), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.JSON, result.getSecond().getContentType());
    }

    @Test
    public void chirpStackConverterWrapperTest() throws Exception {
        ConverterUnwrapper wrapper = ConverterUnwrapperFactory.getUnwrapper(IntegrationType.CHIRPSTACK).get();

        ObjectNode payloadMsg = JacksonUtil.fromString(readPayloadFromFile("ChirpStackPayload.json"), ObjectNode.class);

        UplinkMetaData<String> uplinkMetaData = new UplinkMetaData<>(ContentType.JSON, Map.of("integrationName", "Chirpstack integration"));

        Map<String, Object> expectedKvMap = new HashMap<>(uplinkMetaData.getKvMap());
        expectedKvMap.put("data", "MkEzRg==");
        expectedKvMap.put("fPort", 80);
        expectedKvMap.put("eui", "BE7A123456789");
        expectedKvMap.put("deviceName", "Chirpstack");
        expectedKvMap.put("deviceProfileName", "default");
        expectedKvMap.put("devAddr", "00189440");
        expectedKvMap.put("rssi", -22);
        expectedKvMap.put("snr", 10.5);
        expectedKvMap.put("bandwidth", 125000);
        expectedKvMap.put("deduplicationId", "3ac7e3c4-4401-4b8d-9386-a5c902f9202d");
        expectedKvMap.put("deviceProfileId", "14855bf7-d10d-4aee-b618-ebfcb64dc7ad");
        expectedKvMap.put("dr", 1);
        expectedKvMap.put("tags", Map.of("key", "value"));
        expectedKvMap.put("frequency", 867100000);
        expectedKvMap.put("codeRate", "CR_4_5");
        expectedKvMap.put("spreadingFactor", 11);
        expectedKvMap.put("tenantName", "ChirpStack");
        expectedKvMap.put("tenantId", "52f14cd4-c6f1-4fbd-8f87-4025e1d49242");
        expectedKvMap.put("time", "2022-07-18T09:34:15.775023242+00:00");
        expectedKvMap.put("timeTs", 1658136855775L);
        expectedKvMap.put("ts", 1658136855775L);
        expectedKvMap.put("applicationId", "17c82e96-be03-4f38-aef3-f83d48582d97");
        expectedKvMap.put("applicationName", "Test application");
        expectedKvMap.put("rxInfo", List.of(Map.of("uplinkId", 2, "rssi",-36, "snr", 10.5), Map.of("uplinkId", 1, "rssi", -22, "snr", 10.5)));
        expectedKvMap.put("payloadFormat", "BINARY");

        TbPair<byte[], UplinkMetaData<Object>> result = wrapper.unwrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(Base64.getDecoder().decode("MkEzRg=="), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.BINARY, result.getSecond().getContentType());

        Map<String, Object> decoded = Map.of("temperature", 42, "humidity", 63);

        payloadMsg.set("object", JacksonUtil.valueToTree(decoded));
        expectedKvMap.put("decoded", decoded);
        expectedKvMap.put("payloadFormat", "JSON");

        result = wrapper.unwrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(JacksonUtil.writeValueAsBytes(decoded), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.JSON, result.getSecond().getContentType());
    }

    @Test
    public void thingsStackConverterWrapperTest() throws Exception {
        ConverterUnwrapper wrapper = ConverterUnwrapperFactory.getUnwrapper(IntegrationType.TTN).get();

        ObjectNode payloadMsg = JacksonUtil.fromString(readPayloadFromFile("ThingsStackPayload.json"), ObjectNode.class);

        UplinkMetaData<String> uplinkMetaData = new UplinkMetaData<>(ContentType.JSON, Map.of("integrationName", "Chirpstack integration"));

        List<Map> rxMetadata = new ArrayList<>();
        rxMetadata.add(Map.of("gateway_ids", Map.of("gateway_id", "eui-6A7E111A10000000", "eui", "6A7E111A10000000"), "rssi", -22, "channel_rssi", -22,"snr", 11));
        rxMetadata.add(Map.of("gateway_ids", Map.of("gateway_id", "packetbroker"), "rssi", -24,"channel_rssi", -24,"snr", 12));

        Map<String, Object> expectedKvMap = new HashMap<>(uplinkMetaData.getKvMap());
        expectedKvMap.put("deviceId", "03022");
        expectedKvMap.put("applicationId", "test-decentlab");
        expectedKvMap.put("eui", "70B3D57BA0000BCE");
        expectedKvMap.put("joinEui", "70B3D57ED00006B2");
        expectedKvMap.put("devAddr", 27000020);
        expectedKvMap.put("correlationIds", List.of("as:up:01E0CY8V864TP36Q130RSQQJBY"));
        expectedKvMap.put("receivedAt", "2020-02-06T09:46:05.447941836Z");
        expectedKvMap.put("receivedAtTs", 1580982365447L);
        expectedKvMap.put("sessionKeyId", "AXAWYbtxgUllLtJWdZrW0Q==");
        expectedKvMap.put("fPort", 1);
        expectedKvMap.put("fCnt", 101);
        expectedKvMap.put("data", "AgI7AAMANwJxDGA=");
        expectedKvMap.put("rxMetadata", rxMetadata);
        expectedKvMap.put("bandwidth", 125000);
        expectedKvMap.put("spreadingFactor", 11);
        expectedKvMap.put("dataRateIndex", 1);
        expectedKvMap.put("codeRate", "4/5");
        expectedKvMap.put("frequency", 867700000);
        expectedKvMap.put("timestamp", 436812492);
        expectedKvMap.put("time", "2020-02-06T09:46:05Z");
        expectedKvMap.put("timeTs", 1580982365000L);
        expectedKvMap.put("uplinkMessageReceivedAt", "2020-02-06T09:46:05.234172599Z");
        expectedKvMap.put("uplinkMessageReceivedAtTs", 1580982365234L);
        expectedKvMap.put("ts", 1580982365234L);
        expectedKvMap.put("rssi", -22);
        expectedKvMap.put("snr", 11.0);
        expectedKvMap.put("payloadFormat", "BINARY");

        TbPair<byte[], UplinkMetaData<Object>> result = wrapper.unwrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(Base64.getDecoder().decode("AgI7AAMANwJxDGA="), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.BINARY, result.getSecond().getContentType());

        Map<String, Object> decoded = Map.of("temperature", 42, "humidity", 63);

        ((ObjectNode) payloadMsg.get("uplink_message")).set("decoded_payload", JacksonUtil.valueToTree(decoded));
        expectedKvMap.put("decoded", decoded);
        expectedKvMap.put("payloadFormat", "JSON");

        result = wrapper.unwrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(JacksonUtil.writeValueAsBytes(decoded), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.JSON, result.getSecond().getContentType());
    }

    @Test
    public void thingsParkConverterWrapperTest() throws Exception {
        ConverterUnwrapper wrapper = ConverterUnwrapperFactory.getUnwrapper(IntegrationType.THINGPARK).get();

        ObjectNode payloadMsg = JacksonUtil.fromString(readPayloadFromFile("ThingsParkPayload.json"), ObjectNode.class);

        UplinkMetaData<String> uplinkMetaData = new UplinkMetaData<>(ContentType.JSON, Map.of("integrationName", "Chirpstack integration"));

        Map<String, Object> expectedKvMap = new HashMap<>(uplinkMetaData.getKvMap());
        expectedKvMap.put("time", "2024-11-28T21:08:22.138+00:00");
        expectedKvMap.put("timeTs", 1732828102138L);
        expectedKvMap.put("ts", 1732828102138L);
        expectedKvMap.put("eui", "70B3D57BA000156B");
        expectedKvMap.put("fPort", 1);
        expectedKvMap.put("fCnt", 26);
        expectedKvMap.put("lostUplinksAs", 0);
        expectedKvMap.put("adr", 1);
        expectedKvMap.put("mType", 2);
        expectedKvMap.put("fCntDn", 2);
        expectedKvMap.put("data", "02023b0003003702710c60");
        expectedKvMap.put("micHex", "e7214986");
        expectedKvMap.put("lrcid", "00000211");
        expectedKvMap.put("rssi", -114.0);
        expectedKvMap.put("snr", 4.75);
        expectedKvMap.put("esp", -115.2547);
        expectedKvMap.put("spreadingFactor", 9);
        expectedKvMap.put("bandwidth", "G0");
        expectedKvMap.put("channel", "LC1");
        expectedKvMap.put("lrrId", "100019D4");
        expectedKvMap.put("late", 0);
        expectedKvMap.put("latitude", 32.516357);
        expectedKvMap.put("longitude", -106.824348);
        expectedKvMap.put("lrr", List.of());
        expectedKvMap.put("devLrrCnt", 1);
        expectedKvMap.put("customerId", 100045194);
        expectedKvMap.put("customerData", Map.of());
        expectedKvMap.put("baseStationData", Map.of("doms", List.of(), "name", "iStation US #6_CDRRC_Summerford"));
        expectedKvMap.put("modelCfg", 0);
        expectedKvMap.put("driverCfg", Map.of());
        expectedKvMap.put("instantPer", 0.0);
        expectedKvMap.put("meanPer", 0.037037);
        expectedKvMap.put("devAddr", "00FDA112");
        expectedKvMap.put("txPower", 18.0);
        expectedKvMap.put("nbTrans", 2);
        expectedKvMap.put("frequency", 902.5);
        expectedKvMap.put("dynamicClass", "A");
        expectedKvMap.put("payloadFormat", "BINARY");

        TbPair<byte[], UplinkMetaData<Object>> result = wrapper.unwrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(Base64.getDecoder().decode("AgI7AAMANwJxDGA="), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.BINARY, result.getSecond().getContentType());

        Map<String, Object> decoded = Map.of("temperature", 42, "humidity", 63);

        ((ObjectNode) payloadMsg.get("DevEUI_uplink")).set("payload", JacksonUtil.valueToTree(decoded));
        expectedKvMap.put("decoded", decoded);
        expectedKvMap.put("payloadFormat", "JSON");

        result = wrapper.unwrap(JacksonUtil.writeValueAsBytes(payloadMsg), uplinkMetaData);

        assertArrayEquals(JacksonUtil.writeValueAsBytes(decoded), result.getFirst());

        assertThat(result.getSecond().getKvMap()).containsExactlyInAnyOrderEntriesOf(expectedKvMap);

        assertEquals(ContentType.JSON, result.getSecond().getContentType());
    }

    private String readPayloadFromFile(String fileName) throws Exception {
        var uri = this.getClass().getClassLoader().getResource(fileName).toURI();
        return Files.readString(Path.of(uri));

    }

}
