// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class DedicatedScriptUplinkDataConverterTest {

    private DedicatedScriptUplinkDataConverter uplinkDataConverter;

    @BeforeEach
    public void startup() {
        uplinkDataConverter = new DedicatedScriptUplinkDataConverter(any(), any(), any());
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
