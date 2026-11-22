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
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void testParseAttributesProtoWithTypeCastDisabledKeys() {
        var json = JsonParser.parseString("{\"devAddr\": \"01020304\", \"fCnt\": \"42\", \"rssi\": -110}");
        var kvList = JsonConverter.convertToAttributesProto(json, Set.of("devAddr")).getKvList();
        assertKv(kvList.get(0), KeyValueType.STRING_V, "01020304");
        assertKv(kvList.get(1), KeyValueType.LONG_V, "42");
        assertKv(kvList.get(2), KeyValueType.LONG_V, "-110");
    }

    @Test
    public void testParseTelemetryProtoWithTypeCastDisabledKeys() {
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

    @Test
    public void testAttributesFromMsgWithTypeCastDisabledKeys() {
        var msg = msg("{\"joinEui\": \"0000000000000000\", \"hardwareVersion\": \"1.0\", \"fCnt\": \"42\", \"rssi\": -110}", Set.of("joinEui", "hardwareVersion"));
        Map<String, KvEntry> kvs = toMap(JsonConverter.attributesFromMsg(msg));
        assertEntry(kvs.get("joinEui"), DataType.STRING, "0000000000000000");
        assertEntry(kvs.get("hardwareVersion"), DataType.STRING, "1.0");
        assertEntry(kvs.get("fCnt"), DataType.LONG, 42L);
        assertEntry(kvs.get("rssi"), DataType.LONG, -110L);
    }

    @Test
    public void testConvertToAttributesCastsNumericStrings() {
        var json = JsonParser.parseString("{\"joinEui\": \"0000000000000000\", \"hardwareVersion\": \"1.0\"}");
        Map<String, KvEntry> kvs = toMap(JsonConverter.convertToAttributes(json, 1000L));
        assertEntry(kvs.get("joinEui"), DataType.LONG, 0L);
        assertEntry(kvs.get("hardwareVersion"), DataType.DOUBLE, 1.0);
    }

    @Test
    public void testAttributesFromMsgWithoutTypeCastDisabledKeysCastsNumericStrings() {
        var msg = msg("{\"joinEui\": \"0000000000000000\", \"hardwareVersion\": \"1.0\"}", Set.of());
        Map<String, KvEntry> kvs = toMap(JsonConverter.attributesFromMsg(msg));
        assertEntry(kvs.get("joinEui"), DataType.LONG, 0L);
        assertEntry(kvs.get("hardwareVersion"), DataType.DOUBLE, 1.0);
    }

    @Test
    public void testTelemetryFromMsgWithTypeCastDisabledKeys() {
        var msg = msg("{\"devAddr\": \"01020304\", \"fCnt\": \"42\"}", Set.of("devAddr"));
        Map<Long, List<KvEntry>> tsKv = JsonConverter.telemetryFromMsg(msg, 1000L);
        Map<String, KvEntry> kvs = toMap(tsKv.get(1000L));
        assertEntry(kvs.get("devAddr"), DataType.STRING, "01020304");
        assertEntry(kvs.get("fCnt"), DataType.LONG, 42L);
    }

    @Test
    public void testSortedTelemetryFromMsgWithTsAndTypeCastDisabledKeys() {
        var msg = msg("[{\"ts\": 2000, \"values\": {\"devAddr\": \"01020304\", \"snr\": \"9.5\"}}, {\"ts\": 1000, \"values\": {\"devAddr\": \"01020305\"}}]", Set.of("devAddr"));
        Map<Long, List<KvEntry>> tsKv = JsonConverter.sortedTelemetryFromMsg(msg, 0L);
        Assertions.assertEquals(List.of(1000L, 2000L), List.copyOf(tsKv.keySet()));
        assertEntry(toMap(tsKv.get(1000L)).get("devAddr"), DataType.STRING, "01020305");
        Map<String, KvEntry> kvs = toMap(tsKv.get(2000L));
        assertEntry(kvs.get("devAddr"), DataType.STRING, "01020304");
        assertEntry(kvs.get("snr"), DataType.DOUBLE, 9.5);
    }

    @Test
    public void testConvertToTelemetryCastsNumericStrings() {
        var json = JsonParser.parseString("{\"ts\": 1000, \"values\": {\"devAddr\": \"01020304\"}}");
        assertEntry(JsonConverter.convertToTelemetry(json, 0L).get(1000L).get(0), DataType.LONG, 1020304L);
        assertEntry(JsonConverter.convertToSortedTelemetry(json, 0L).get(1000L).get(0), DataType.LONG, 1020304L);
    }

    @Test
    public void testTelemetryFromMsgWithoutTypeCastDisabledKeysCastsNumericStrings() {
        var msg = msg("{\"ts\": 1000, \"values\": {\"devAddr\": \"01020304\"}}", Set.of());
        assertEntry(JsonConverter.telemetryFromMsg(msg, 0L).get(1000L).get(0), DataType.LONG, 1020304L);
        assertEntry(JsonConverter.sortedTelemetryFromMsg(msg, 0L).get(1000L).get(0), DataType.LONG, 1020304L);
    }

    @Test
    public void testTelemetryFromMsgWithTypeCastDisabledKeysWhenTypeCastDisabled() {
        JsonConverter.setTypeCastEnabled(false);
        var msg = msg("{\"devAddr\": \"01020304\", \"fCnt\": \"42\"}", Set.of("devAddr"));
        Map<String, KvEntry> kvs = toMap(JsonConverter.telemetryFromMsg(msg, 1000L).get(1000L));
        assertEntry(kvs.get("devAddr"), DataType.STRING, "01020304");
        assertEntry(kvs.get("fCnt"), DataType.STRING, "42");
    }

    @Test
    public void testNumericStringKeysListsOnlyStringsThatWouldBeCast() {
        assertThat(JsonConverter.numericStringKeys(List.of(
                new BaseAttributeKvEntry(new StringDataEntry("joinEui", "0000000000000000"), 0L),
                new BaseAttributeKvEntry(new StringDataEntry("firmwareVersion", "1.10"), 0L),
                new BaseAttributeKvEntry(new StringDataEntry("devEui", "70B3D57ED0012345"), 0L),
                new BaseAttributeKvEntry(new LongDataEntry("fCnt", 42L), 0L),
                new BaseAttributeKvEntry(new DoubleDataEntry("rssi", -97.5), 0L))))
                .containsExactly("joinEui", "firmwareVersion");
    }

    @Test
    public void testNumericStringKeysIsEmptyWithoutNumericStrings() {
        assertThat(JsonConverter.numericStringKeys(List.of(
                new BaseAttributeKvEntry(new StringDataEntry("devEui", "70B3D57ED0012345"), 0L),
                new BaseAttributeKvEntry(new LongDataEntry("fCnt", 42L), 0L)))).isEmpty();
        assertThat(JsonConverter.numericStringKeys(null)).isEmpty();
    }

    private static TbMsg msg(String data, Set<String> typeCastDisabledKeys) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putTypeCastDisabledKeys(typeCastDisabledKeys);
        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .metaData(metaData)
                .data(data)
                .build();
    }

    private static Map<String, KvEntry> toMap(List<? extends KvEntry> entries) {
        return entries.stream().collect(Collectors.toMap(KvEntry::getKey, Function.identity()));
    }

    private static void assertEntry(KvEntry entry, DataType type, Object value) {
        Assertions.assertNotNull(entry);
        Assertions.assertEquals(type, entry.getDataType());
        Assertions.assertEquals(value, entry.getValue());
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
