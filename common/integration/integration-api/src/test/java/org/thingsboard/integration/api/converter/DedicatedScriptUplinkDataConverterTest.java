// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.wrapper.ChirpStackConverterUnwrapper;
import org.thingsboard.integration.api.converter.wrapper.ConverterUnwrapper;
import org.thingsboard.integration.api.converter.wrapper.LoriotConverterUnwrapper;
import org.thingsboard.integration.api.converter.wrapper.ThingParkConverterUnwrapper;
import org.thingsboard.integration.api.converter.wrapper.ThingsStackConverterUnwrapper;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.thingsboard.server.gen.transport.TransportProtos.KeyValueType.DOUBLE_V;
import static org.thingsboard.server.gen.transport.TransportProtos.KeyValueType.LONG_V;
import static org.thingsboard.server.gen.transport.TransportProtos.KeyValueType.STRING_V;

public class DedicatedScriptUplinkDataConverterTest {

    private DedicatedScriptUplinkDataConverter uplinkDataConverter;
    private final ConverterUnwrapper thingsStackUnwrapper = new ThingsStackConverterUnwrapper();

    @BeforeEach
    public void startup() {
        uplinkDataConverter = new DedicatedScriptUplinkDataConverter(any(), any(), any());
        ReflectionTestUtils.setField(uplinkDataConverter, "converterUnwrapper", thingsStackUnwrapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"01020304", "01a44d4c"})
    public void parseUplinkDataKeepsStringIdentifiersInAttributesTest(String devAddr) throws Exception {
        setConfig(Set.of("devAddr", "eui", "fCnt", "rssi", "snr", "frequency"), null);

        UplinkData uplink = uplinkDataConverter.parseUplinkData(new JsonObject(), unwrapThingsStack(devAddr));

        Map<String, TransportProtos.KeyValueProto> attributes = toMap(uplink.getAttributesUpdate().getKvList());
        assertKv(attributes.get("devAddr"), STRING_V, devAddr);
        assertKv(attributes.get("eui"), STRING_V, "1000000000000001");
        assertKv(attributes.get("fCnt"), LONG_V, 101L);
        assertKv(attributes.get("rssi"), LONG_V, -22L);
        assertKv(attributes.get("snr"), DOUBLE_V, 11.0);
        assertKv(attributes.get("frequency"), LONG_V, 868100000L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"01020304", "01a44d4c"})
    public void parseUplinkDataKeepsStringIdentifiersInTelemetryTest(String devAddr) throws Exception {
        setConfig(Set.of("eui"), Set.of("devAddr", "fCnt", "rssi", "snr"));

        UplinkData uplink = uplinkDataConverter.parseUplinkData(new JsonObject(), unwrapThingsStack(devAddr));

        Map<String, TransportProtos.KeyValueProto> telemetry = toMap(uplink.getTelemetry().getTsKvList(0).getKvList());
        assertKv(telemetry.get("devAddr"), STRING_V, devAddr);
        assertKv(telemetry.get("fCnt"), LONG_V, 101L);
        assertKv(telemetry.get("rssi"), LONG_V, -22L);
        assertKv(telemetry.get("snr"), DOUBLE_V, 11.0);
        assertKv(toMap(uplink.getAttributesUpdate().getKvList()).get("eui"), STRING_V, "1000000000000001");
    }

    @Test
    public void parseUplinkDataKeepsChirpStackStringIdentifiersTest() throws Exception {
        setConfig(Set.of("devAddr", "eui", "dr", "frequency"), Set.of("rssi", "snr"));

        ObjectNode payload = JacksonUtil.fromString(readPayloadFromFile("ChirpStackPayload.json"), ObjectNode.class);
        payload.put("devAddr", "01020304");
        ((ObjectNode) payload.get("deviceInfo")).put("devEui", "1000000000000001");
        UplinkData uplink = uplinkDataConverter.parseUplinkData(new JsonObject(), unwrap(new ChirpStackConverterUnwrapper(), payload));

        Map<String, TransportProtos.KeyValueProto> attributes = toMap(uplink.getAttributesUpdate().getKvList());
        assertKv(attributes.get("devAddr"), STRING_V, "01020304");
        assertKv(attributes.get("eui"), STRING_V, "1000000000000001");
        assertKv(attributes.get("dr"), LONG_V, 1L);
        assertKv(attributes.get("frequency"), LONG_V, 867100000L);
        Map<String, TransportProtos.KeyValueProto> telemetry = toMap(uplink.getTelemetry().getTsKvList(0).getKvList());
        assertKv(telemetry.get("rssi"), LONG_V, -22L);
        assertKv(telemetry.get("snr"), DOUBLE_V, 10.5);
    }

    @Test
    public void parseUplinkDataKeepsLoriotStringIdentifiersTest() throws Exception {
        setConfig(Set.of("eui", "fPort", "frequency", "dr"), Set.of("battery", "rssi"));

        ObjectNode payload = JacksonUtil.fromString(readPayloadFromFile("LoriotPayload.json"), ObjectNode.class);
        payload.put("EUI", "1000000000000001");
        UplinkData uplink = uplinkDataConverter.parseUplinkData(new JsonObject(), unwrap(new LoriotConverterUnwrapper(), payload));

        Map<String, TransportProtos.KeyValueProto> attributes = toMap(uplink.getAttributesUpdate().getKvList());
        assertKv(attributes.get("eui"), STRING_V, "1000000000000001");
        assertKv(attributes.get("fPort"), LONG_V, 80L);
        assertKv(attributes.get("frequency"), LONG_V, 868100000L);
        assertKv(attributes.get("dr"), STRING_V, "SF12 BW125 4/5");
        Map<String, TransportProtos.KeyValueProto> telemetry = toMap(uplink.getTelemetry().getTsKvList(0).getKvList());
        assertKv(telemetry.get("battery"), LONG_V, 143L);
        assertKv(telemetry.get("rssi"), LONG_V, -49L);
    }

    @Test
    public void parseUplinkDataKeepsThingParkStringIdentifiersTest() throws Exception {
        setConfig(Set.of("devAddr", "eui", "lrcid", "fPort", "spreadingFactor", "frequency"), Set.of("rssi", "snr"));

        ObjectNode payload = JacksonUtil.fromString(readPayloadFromFile("ThingsParkPayload.json"), ObjectNode.class);
        ObjectNode uplinkNode = (ObjectNode) payload.get("DevEUI_uplink");
        uplinkNode.put("DevAddr", "01020304");
        uplinkNode.put("DevEUI", "1000000000000001");
        UplinkData uplink = uplinkDataConverter.parseUplinkData(new JsonObject(), unwrap(new ThingParkConverterUnwrapper(), payload));

        Map<String, TransportProtos.KeyValueProto> attributes = toMap(uplink.getAttributesUpdate().getKvList());
        assertKv(attributes.get("devAddr"), STRING_V, "01020304");
        assertKv(attributes.get("eui"), STRING_V, "1000000000000001");
        assertKv(attributes.get("lrcid"), STRING_V, "00000211");
        assertKv(attributes.get("fPort"), LONG_V, 1L);
        assertKv(attributes.get("spreadingFactor"), LONG_V, 9L);
        assertKv(attributes.get("frequency"), DOUBLE_V, 902.5);
        Map<String, TransportProtos.KeyValueProto> telemetry = toMap(uplink.getTelemetry().getTsKvList(0).getKvList());
        assertKv(telemetry.get("rssi"), DOUBLE_V, -114.0);
        assertKv(telemetry.get("snr"), DOUBLE_V, 4.75);
    }

    @Test
    public void parseUplinkDataKeepsNumericIdentifierAsNumberTest() throws Exception {
        setConfig(Set.of("devAddr"), null);

        ObjectNode payload = JacksonUtil.fromString(readPayloadFromFile("ThingsStackPayload.json"), ObjectNode.class);
        UplinkMetaData unwrapped = thingsStackUnwrapper.unwrap(JacksonUtil.writeValueAsBytes(payload), new UplinkMetaData<>(ContentType.JSON, Map.of())).getSecond();
        UplinkData uplink = uplinkDataConverter.parseUplinkData(new JsonObject(), unwrapped);

        assertKv(toMap(uplink.getAttributesUpdate().getKvList()).get("devAddr"), LONG_V, 27000020L);
    }

    @Test
    public void parseUplinkDataCastsDecoderOutputTest() throws Exception {
        setConfig(Set.of("devAddr"), null);

        JsonObject attributes = new JsonObject();
        attributes.addProperty("devAddr", "01020304");
        JsonObject uplinkJson = new JsonObject();
        uplinkJson.add("attributes", attributes);

        UplinkData uplink = uplinkDataConverter.parseUplinkData(uplinkJson, unwrapThingsStack("05060708"));

        assertKv(toMap(uplink.getAttributesUpdate().getKvList()).get("devAddr"), LONG_V, 1020304L);
    }

    @Test
    public void parseUplinkDataWithTsTest() {
        DedicatedConverterConfig config = new DedicatedConverterConfig();
        config.setType(EntityType.DEVICE);
        config.setName("Device ${eui}");
        config.setAttributes(Set.of("eui", "fPort", "rssi"));
        config.setTelemetry(Set.of("data"));

        ReflectionTestUtils.setField(uplinkDataConverter, "config", config);

        JsonObject telemetry = new JsonObject();
        JsonObject telemetryValue = new JsonObject();
        telemetryValue.addProperty("temperature", "42");
        telemetryValue.addProperty("humidity", "63");

        long ts = System.currentTimeMillis();

        telemetry.addProperty("ts", ts);
        telemetry.add("values", telemetryValue);

        JsonObject uplinkJson = new JsonObject();
        uplinkJson.add("attributes", new JsonObject());
        uplinkJson.add("telemetry", telemetry);

        Map<String, String> kvMap = Map.of(
                "data", "2A3F",
                "rssi", "-130",
                "fPort", "80",
                "eui", "BE7A123456789");

        UplinkMetaData uplinkMetaData = new UplinkMetaData(ContentType.JSON, kvMap);

        Map<String, Object> expectedTelemetry = Map.of(
                "data", "2A3F",
                "temperature", 42L,
                "humidity", 63L
        );

        UplinkData uplink = uplinkDataConverter.parseUplinkData(uplinkJson, uplinkMetaData);
        var tsKvList = uplink.getTelemetry().getTsKvListList().get(0);
        assertEquals(ts, tsKvList.getTs());

        var kvList = tsKvList.getKvList();
        assertEquals(expectedTelemetry.size(), tsKvList.getKvList().size());

        kvList.forEach(kv -> {
            assertTrue(expectedTelemetry.containsKey(kv.getKey()));
            assertEquals(expectedTelemetry.get(kv.getKey()), getValue(kv));
        });
    }

    @Test
    public void parseUplinkDataWithoutTsTest() {
        DedicatedConverterConfig config = new DedicatedConverterConfig();
        config.setType(EntityType.DEVICE);
        config.setName("Device ${eui}");
        config.setAttributes(Set.of("eui", "fPort", "rssi"));
        config.setTelemetry(Set.of("data"));

        ReflectionTestUtils.setField(uplinkDataConverter, "config", config);

        JsonObject telemetry = new JsonObject();
        telemetry.addProperty("temperature", "42");
        telemetry.addProperty("humidity", "63");

        JsonObject uplinkJson = new JsonObject();
        uplinkJson.add("attributes", new JsonObject());
        uplinkJson.add("telemetry", telemetry);

        Map<String, String> kvMap = Map.of(
                "data", "2A3F",
                "rssi", "-130",
                "fPort", "80",
                "eui", "BE7A123456789");

        UplinkMetaData uplinkMetaData = new UplinkMetaData(ContentType.JSON, kvMap);

        Map<String, Object> expectedTelemetry = Map.of(
                "data", "2A3F",
                "temperature", 42L,
                "humidity", 63L
        );

        UplinkData uplink = uplinkDataConverter.parseUplinkData(uplinkJson, uplinkMetaData);
        var tsKvList = uplink.getTelemetry().getTsKvListList().get(0);
        assertTrue(tsKvList.getTs() > 0);

        var kvList = tsKvList.getKvList();
        assertEquals(expectedTelemetry.size(), tsKvList.getKvList().size());

        kvList.forEach(kv -> {
            assertTrue(expectedTelemetry.containsKey(kv.getKey()));
            assertEquals(expectedTelemetry.get(kv.getKey()), getValue(kv));
        });
    }

    @Test
    public void parseUplinkDataWithTelemetryArrayTest() {
        DedicatedConverterConfig config = new DedicatedConverterConfig();
        config.setType(EntityType.DEVICE);
        config.setName("Device ${eui}");
        config.setAttributes(Set.of("eui", "fPort", "rssi"));
        config.setTelemetry(Set.of("data"));

        ReflectionTestUtils.setField(uplinkDataConverter, "config", config);

        JsonObject telemetry = new JsonObject();
        telemetry.addProperty("temperature", "42");
        telemetry.addProperty("humidity", "63");

        JsonArray telemetryArray = new JsonArray();
        telemetryArray.add(telemetry);

        JsonObject uplinkJson = new JsonObject();
        uplinkJson.add("attributes", new JsonObject());
        uplinkJson.add("telemetry", telemetryArray);

        Map<String, String> kvMap = Map.of(
                "data", "2A3F",
                "rssi", "-130",
                "fPort", "80",
                "eui", "BE7A123456789");

        UplinkMetaData uplinkMetaData = new UplinkMetaData(ContentType.JSON, kvMap);

        Map<String, Object> expectedTelemetry = Map.of(
                "data", "2A3F",
                "temperature", 42L,
                "humidity", 63L
        );

        UplinkData uplink = uplinkDataConverter.parseUplinkData(uplinkJson, uplinkMetaData);

        List<TransportProtos.KeyValueProto> kvList = new ArrayList<>();

        uplink.getTelemetry().getTsKvListList().forEach(tsKvList -> {
            assertTrue(tsKvList.getTs() > 0);
            kvList.addAll(tsKvList.getKvList());
        });

        assertEquals(expectedTelemetry.size(), kvList.size());

        kvList.forEach(kv -> {
            assertTrue(expectedTelemetry.containsKey(kv.getKey()));
            assertEquals(expectedTelemetry.get(kv.getKey()), getValue(kv));
        });
    }

    @Test
    public void parseUplinkDataTest() {
        DedicatedConverterConfig config = new DedicatedConverterConfig();
        config.setType(EntityType.DEVICE);
        config.setName("Device $eui");
        config.setProfile("$deviceProfile");
        config.setLabel("$eui");
        config.setAttributes(Set.of("eui", "fPort", "rssi"));

        ReflectionTestUtils.setField(uplinkDataConverter, "config", config);

        JsonObject uplinkJson = new JsonObject();
        uplinkJson.add("attributes", new JsonObject());
        uplinkJson.add("telemetry", new JsonObject());

        Map<String, String> kvMap = Map.of(
                "eui", "BE7A123456789",
                "deviceProfile", "default");

        UplinkMetaData uplinkMetaData = new UplinkMetaData(ContentType.JSON, kvMap);

        UplinkData uplink = uplinkDataConverter.parseUplinkData(uplinkJson, uplinkMetaData);
        assertEquals("Device BE7A123456789", uplink.getDeviceName());
        assertEquals("default", uplink.getDeviceType());
        assertEquals("BE7A123456789", uplink.getDeviceLabel());
    }

    private void setConfig(Set<String> attributes, Set<String> telemetry) {
        DedicatedConverterConfig config = new DedicatedConverterConfig();
        config.setType(EntityType.DEVICE);
        config.setName("Device $eui");
        config.setAttributes(attributes);
        config.setTelemetry(telemetry);
        ReflectionTestUtils.setField(uplinkDataConverter, "config", config);
    }

    private UplinkMetaData unwrapThingsStack(String devAddr) throws Exception {
        ObjectNode payload = JacksonUtil.fromString(readPayloadFromFile("ThingsStackPayload.json"), ObjectNode.class);
        ObjectNode endDeviceIds = (ObjectNode) payload.get("end_device_ids");
        endDeviceIds.put("dev_addr", devAddr);
        endDeviceIds.put("dev_eui", "1000000000000001");
        ((ObjectNode) payload.at("/uplink_message/settings")).put("frequency", "868100000");
        return thingsStackUnwrapper.unwrap(JacksonUtil.writeValueAsBytes(payload), new UplinkMetaData<>(ContentType.JSON, Map.of())).getSecond();
    }

    private UplinkMetaData unwrap(ConverterUnwrapper unwrapper, ObjectNode payload) throws Exception {
        ReflectionTestUtils.setField(uplinkDataConverter, "converterUnwrapper", unwrapper);
        return unwrapper.unwrap(JacksonUtil.writeValueAsBytes(payload), new UplinkMetaData<>(ContentType.JSON, Map.of())).getSecond();
    }

    private Map<String, TransportProtos.KeyValueProto> toMap(List<TransportProtos.KeyValueProto> kvList) {
        return kvList.stream().collect(Collectors.toMap(TransportProtos.KeyValueProto::getKey, kv -> kv));
    }

    private void assertKv(TransportProtos.KeyValueProto kv, TransportProtos.KeyValueType type, Object value) {
        assertEquals(type, kv.getType());
        assertEquals(value, getValue(kv));
    }

    private String readPayloadFromFile(String fileName) throws Exception {
        var uri = this.getClass().getClassLoader().getResource(fileName).toURI();
        return Files.readString(Path.of(uri));
    }

    private Object getValue(TransportProtos.KeyValueProto kv) {
        return switch (kv.getType()) {
            case STRING_V -> kv.getStringV();
            case LONG_V -> kv.getLongV();
            case DOUBLE_V -> kv.getDoubleV();
            case BOOLEAN_V -> kv.getBoolV();
            case JSON_V -> kv.getJsonV();
            default -> throw new IllegalStateException("Unexpected type: " + kv.getType());
        };
    }
}
