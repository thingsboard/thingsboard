// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.adaptor;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;

import java.util.Set;

@Isolated("JsonConverter static settings being modified")
public class JsonConverterTest {

    @BeforeEach
    public void before() {
        JsonConverter.setTypeCastEnabled(true);
    }

    @AfterEach
    public void after() {
        //restore default state for a static class
        JsonConverter.setTypeCastEnabled(true);
    }

    @Test
    public void testParseBigDecimalAsLong() {
        var result = JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 1E+1}"), 0L);
        Assertions.assertEquals(10L, result.get(0L).get(0).getLongValue().get().longValue());
    }

    @Test
    public void testParseBigDecimalAsDouble() {
        var result = JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 101E-1}"), 0L);
        Assertions.assertEquals(10.1, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    @Test
    public void testParseAttributesBigDecimalAsLong() {
        var result = JsonConverter.convertToAttributes(JsonParser.parseString("{\"meterReadingDelta\": 1E1}"));
        Assertions.assertEquals(10L, result.get(0).getLongValue().get().longValue());
    }

    @Test
    public void testParseAsDoubleWithZero() {
        var result = JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 42.0}"), 0L);
        Assertions.assertEquals(42.0, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    @Test
    public void testParseAsDouble() {
        var result = JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 1.1}"), 0L);
        Assertions.assertEquals(1.1, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    @Test
    public void testParseAsLong() {
        var result = JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 11}"), 0L);
        Assertions.assertEquals(11L, result.get(0L).get(0).getLongValue().get().longValue());
    }

    @Test
    public void testParseBigDecimalAsStringOutOfLongRange() {
        var result = JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 9.9701010061400066E19}"), 0L);
        Assertions.assertEquals("99701010061400066000", result.get(0L).get(0).getStrValue().get());
    }

    @Test
    public void testParseBigDecimalAsStringOutOfLongRange2() {
        var result = JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 99701010061400066001}"), 0L);
        Assertions.assertEquals("99701010061400066001", result.get(0L).get(0).getStrValue().get());
    }

    @Test
    public void testParseBigDecimalAsStringOutOfLongRange3() {
        var result = JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 1E19}"), 0L);
        Assertions.assertEquals("10000000000000000000", result.get(0L).get(0).getStrValue().get());
    }

    @Test
    public void testParseBigDecimalOutOfLongRangeWithoutParsing() {
        JsonConverter.setTypeCastEnabled(false);
        Assertions.assertThrows(JsonSyntaxException.class, () -> {
            JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 89701010051400054084}"), 0L);
        });
    }

    @Test
    public void testParseBigDecimalOutOfLongRangeWithoutParsing2() {
        JsonConverter.setTypeCastEnabled(false);
        Assertions.assertThrows(JsonSyntaxException.class, () -> {
            JsonConverter.convertToTelemetry(JsonParser.parseString("{\"meterReadingDelta\": 9.9701010061400066E19}"), 0L);
        });
    }

    @Test
    public void testParseAttributesProtoWithNoCastKeys() {
        var json = JsonParser.parseString("{\"devAddr\": \"01020304\", \"fCnt\": \"42\", \"rssi\": -110}");
        var kvList = JsonConverter.convertToAttributesProto(json, Set.of("devAddr")).getKvList();
        assertKv(kvList.get(0), KeyValueType.STRING_V, "01020304");
        assertKv(kvList.get(1), KeyValueType.LONG_V, "42");
        assertKv(kvList.get(2), KeyValueType.LONG_V, "-110");
    }

    @Test
    public void testParseTelemetryProtoWithNoCastKeys() {
        var json = JsonParser.parseString("{\"ts\": 1000, \"values\": {\"eui\": \"1000000000000001\", \"fCnt\": \"42\", \"snr\": 9.5}}");
        var tsKvList = JsonConverter.convertToTelemetryProto(json, Set.of("eui")).getTsKvList(0);
        Assertions.assertEquals(1000L, tsKvList.getTs());
        assertKv(tsKvList.getKv(0), KeyValueType.STRING_V, "1000000000000001");
        assertKv(tsKvList.getKv(1), KeyValueType.LONG_V, "42");
        assertKv(tsKvList.getKv(2), KeyValueType.DOUBLE_V, "9.5");
    }

    @Test
    public void testParseAttributesProtoCastsNumericStrings() {
        var kvList = JsonConverter.convertToAttributesProto(JsonParser.parseString("{\"devAddr\": \"01020304\"}")).getKvList();
        assertKv(kvList.get(0), KeyValueType.LONG_V, "1020304");
    }

    private static void assertKv(KeyValueProto kv, KeyValueType type, String value) {
        Assertions.assertEquals(type, kv.getType());
        String actual = switch (type) {
            case STRING_V -> kv.getStringV();
            case LONG_V -> String.valueOf(kv.getLongV());
            case DOUBLE_V -> String.valueOf(kv.getDoubleV());
            default -> throw new IllegalArgumentException("Unexpected type: " + type);
        };
        Assertions.assertEquals(value, actual);
    }

}
