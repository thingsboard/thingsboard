/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.script.api.tbel;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionHashMap;
import org.mvel2.execution.ExecutionLinkedHashSet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static java.lang.Character.MAX_RADIX;
import static java.lang.Character.MIN_RADIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class TbUtilsTest {

    private ExecutionContext ctx;

    private final Float floatVal = 29.29824f;

    private final float floatValRev = -5.948442E7f;

    private final long longVal = 0x409B04B10CB295EAL;
    private final String longValHex = "409B04B10CB295EA";
    private final String longValHexRev = "EA95B20CB1049B40";

    private final double doubleVal = 1729.1729;
    private final double doubleValRev = -2.7208640774822924E205;


    @BeforeEach
    public void before() {
        SandboxedParserConfiguration parserConfig = ParserContext.enableSandboxedMode();
        parserConfig.addImport("JSON", TbJson.class);
        parserConfig.registerDataType("Date", TbDate.class, date -> 8L);
        parserConfig.registerDataType("Random", Random.class, date -> 8L);
        parserConfig.registerDataType("Calendar", Calendar.class, date -> 8L);
        try {
            TbUtils.register(parserConfig);
        } catch (Exception e) {
            log.error("Cannot register functions", e);
        }
        ctx = new ExecutionContext(parserConfig);
        Assertions.assertNotNull(ctx);
    }

    @AfterEach
    public void after() {
        ctx.stop();
    }

    @Test
    public void parseHexToInt() {
        Assertions.assertEquals(0xAB, TbUtils.parseHexToInt("AB"));

        Assertions.assertEquals(0xABBA, TbUtils.parseHexToInt("ABBA", true));
        Assertions.assertEquals(0xBAAB, TbUtils.parseHexToInt("ABBA", false));
        Assertions.assertEquals(0xAABBCC, TbUtils.parseHexToInt("AABBCC", true));
        Assertions.assertEquals(0xAABBCC, TbUtils.parseHexToInt("CCBBAA", false));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseHexToInt("AABBCCDD", true));
        Assertions.assertEquals(0x11BBCC22, TbUtils.parseHexToInt("11BBCC22", true));
        Assertions.assertEquals(0x11BBCC22, TbUtils.parseHexToInt("22CCBB11", false));
    }

    @Test
    public void parseBytesToInt_checkPrimitives() {
        int expected = 257;
        byte[] data = ByteBuffer.allocate(4).putInt(expected).array();
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4));
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 2, 2, true));
        Assertions.assertEquals(1, TbUtils.parseBytesToInt(data, 3, 1, true));

        expected = Integer.MAX_VALUE;
        data = ByteBuffer.allocate(4).putInt(expected).array();
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));

        expected = 0xAABBCCDD;
        data = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));
        data = new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA};
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, false));

        expected = 0xAABBCC;
        data = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC};
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, true));
        data = new byte[]{(byte) 0xCC, (byte) 0xBB, (byte) 0xAA};
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, false));
    }

    @Test
    public void parseBytesToInt_checkLists() {
        int expected = 257;
        List<Byte> data = toList(ByteBuffer.allocate(4).putInt(expected).array());
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4));
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 2, 2, true));
        Assertions.assertEquals(1, TbUtils.parseBytesToInt(data, 3, 1, true));

        expected = Integer.MAX_VALUE;
        data = toList(ByteBuffer.allocate(4).putInt(expected).array());
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));

        expected = 0xAABBCCDD;
        data = toList(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD});
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));
        data = toList(new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, false));

        expected = 0xAABBCC;
        data = toList(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC});
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, true));
        data = toList(new byte[]{(byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        Assertions.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, false));
    }

    @Test
    public void toFlatMap() {
        ExecutionHashMap<String, Object> inputMap = new ExecutionHashMap<>(16, ctx);
        inputMap.put("name", "Alice");
        inputMap.put("age", 30);
        inputMap.put("devices", new ExecutionArrayList<>(List.of(
                new ExecutionHashMap<>(16, ctx) {{
                    put("id", "dev001");
                    put("type", "sensor");
                }},
                new ExecutionHashMap<>(16, ctx) {{
                    put("id", "dev002");
                    put("type", "actuator");
                }}
        ), ctx));
        inputMap.put("settings", new ExecutionHashMap<>(16, ctx) {{
            put("notifications", true);
            put("timezone", "UTC-5");
            put("params", new ExecutionHashMap<>(16, ctx) {{
                put("param1", "value1");
                put("param2", "value2");
                put("param3", new ExecutionHashMap<>(16, ctx) {{
                    put("subParam1", "value1");
                    put("subParam2", "value2");
                }});
            }});
        }});
        ExecutionArrayList<String> excludeList = new ExecutionArrayList<>(ctx);
        excludeList.addAll(List.of("age", "id", "param1", "subParam2"));

        ExecutionHashMap<String, Object> expectedMapWithPath = new ExecutionHashMap<>(16, ctx);
        expectedMapWithPath.put("name", "Alice");
        expectedMapWithPath.put("devices.0.type", "sensor");
        expectedMapWithPath.put("devices.1.type", "actuator");
        expectedMapWithPath.put("settings.notifications", true);
        expectedMapWithPath.put("settings.timezone", "UTC-5");
        expectedMapWithPath.put("settings.params.param2", "value2");
        expectedMapWithPath.put("settings.params.param3.subParam1", "value1");

        ExecutionHashMap<String, Object> actualMapWithPaths = TbUtils.toFlatMap(ctx, inputMap, excludeList, true);

        Assertions.assertEquals(expectedMapWithPath, actualMapWithPaths);

        ExecutionHashMap<String, Object> expectedMapWithoutPaths = new ExecutionHashMap<>(16, ctx);
        expectedMapWithoutPaths.put("timezone", "UTC-5");
        expectedMapWithoutPaths.put("name", "Alice");
        expectedMapWithoutPaths.put("id", "dev002");
        expectedMapWithoutPaths.put("subParam2", "value2");
        expectedMapWithoutPaths.put("type", "actuator");
        expectedMapWithoutPaths.put("subParam1", "value1");
        expectedMapWithoutPaths.put("param1", "value1");
        expectedMapWithoutPaths.put("notifications", true);
        expectedMapWithoutPaths.put("age", 30);
        expectedMapWithoutPaths.put("param2", "value2");

        ExecutionHashMap<String, Object> actualMapWithoutPaths = TbUtils.toFlatMap(ctx, inputMap, false);

        Assertions.assertEquals(expectedMapWithoutPaths, actualMapWithoutPaths);
    }

    @Test
    public void parseInt() {
        Assertions.assertNull(TbUtils.parseInt(null));
        Assertions.assertNull(TbUtils.parseInt(""));
        Assertions.assertNull(TbUtils.parseInt(" "));

        Assertions.assertEquals((Integer) 0, TbUtils.parseInt("0"));
        Assertions.assertEquals((Integer) 0, TbUtils.parseInt("-0"));
        Assertions.assertEquals((Integer) 0, TbUtils.parseInt("+0"));
        Assertions.assertEquals(java.util.Optional.of(473).get(), TbUtils.parseInt("473"));
        Assertions.assertEquals(java.util.Optional.of(-255).get(), TbUtils.parseInt("-0xFF"));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("-0xFF123"));
        Assertions.assertEquals(java.util.Optional.of(-255).get(), TbUtils.parseInt("-FF"));
        Assertions.assertEquals(java.util.Optional.of(255).get(), TbUtils.parseInt("FF"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TbUtils.parseInt("FFF"));
        Assertions.assertEquals(java.util.Optional.of(-2578).get(), TbUtils.parseInt("-0A12"));
        Assertions.assertEquals(java.util.Optional.of(-2578).get(), TbUtils.parseHexToInt("-0A12"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TbUtils.parseHexToInt("A12", false));
        Assertions.assertEquals(java.util.Optional.of(-14866).get(), TbUtils.parseBigEndianHexToInt("-3A12"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TbUtils.parseLittleEndianHexToInt("-A12"));

        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("0xFG"));

        Assertions.assertEquals(java.util.Optional.of(102).get(), TbUtils.parseInt("1100110", MIN_RADIX));
        Assertions.assertEquals(java.util.Optional.of(-102).get(), TbUtils.parseInt("1111111111111111111111111111111111111111111111111111111110011010", MIN_RADIX));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("1100210", MIN_RADIX));
        Assertions.assertEquals(java.util.Optional.of(13158).get(), TbUtils.parseInt("11001101100110", MIN_RADIX));
        Assertions.assertEquals(java.util.Optional.of(-13158).get(), TbUtils.parseInt("1111111111111111111111111111111111111111111111111100110010011010", MIN_RADIX));


        Assertions.assertEquals(java.util.Optional.of(63).get(), TbUtils.parseInt("77", 8));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("18", 8));

        Assertions.assertEquals(java.util.Optional.of(-255).get(), TbUtils.parseInt("-FF", 16));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("FG", 16));

        Assertions.assertEquals(java.util.Optional.of(Integer.MAX_VALUE).get(), TbUtils.parseInt(Integer.toString(Integer.MAX_VALUE), 10));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.valueOf(1)).toString(10), 10));
        Assertions.assertEquals(java.util.Optional.of(Integer.MIN_VALUE).get(), TbUtils.parseInt(Integer.toString(Integer.MIN_VALUE), 10));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt(BigInteger.valueOf(Integer.MIN_VALUE).subtract(BigInteger.valueOf(1)).toString(10), 10));

        Assertions.assertEquals(java.util.Optional.of(506070563).get(), TbUtils.parseInt("KonaIn", 30));

        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("KonaIn", 10));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt(".456", 10));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("4562.", 10));

        Assertions.assertThrows(IllegalArgumentException.class, () -> TbUtils.parseInt("KonaIn", MAX_RADIX + 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TbUtils.parseInt("KonaIn", MIN_RADIX - 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TbUtils.parseInt("KonaIn", 12));
    }

    @Test
    public void parseFloat() {
        String floatValStr = floatVal.toString();
        Assertions.assertEquals(java.util.Optional.of(floatVal).get(), TbUtils.parseFloat(floatValStr));
        String floatValHex = "41EA62CC";
        Assertions.assertEquals(0, Float.compare(floatVal, TbUtils.parseHexToFloat(floatValHex)));
        Assertions.assertEquals(0, Float.compare(floatValRev, TbUtils.parseHexToFloat(floatValHex, false)));
        Assertions.assertEquals(0, Float.compare(floatVal, TbUtils.parseBigEndianHexToFloat(floatValHex)));
        String floatValHexRev = "CC62EA41";
        Assertions.assertEquals(0, Float.compare(floatVal, TbUtils.parseLittleEndianHexToFloat(floatValHexRev)));
    }

    @Test
    public void toFixedFloat() {
        float actualF = TbUtils.toFixed(floatVal, 3);
        Assertions.assertEquals(1, Float.compare(floatVal, actualF));
        Assertions.assertEquals(0, Float.compare(29.298f, actualF));
    }

    @Test
    public void parseBytesToFloat() {
        byte[] floatValByte = {0x0A};
        Assertions.assertEquals(0, Float.compare(1.4E-44f, TbUtils.parseBytesToFloat(floatValByte)));

        floatValByte = new byte[]{65, -22, 98, -52};
        Assertions.assertEquals(0, Float.compare(floatVal, TbUtils.parseBytesToFloat(floatValByte, 0)));
        Assertions.assertEquals(0, Float.compare(floatValRev, TbUtils.parseBytesToFloat(floatValByte, 0, 4, false)));

        List<Byte> floatValList = Bytes.asList(floatValByte);
        Assertions.assertEquals(0, Float.compare(floatVal, TbUtils.parseBytesToFloat(floatValList, 0)));
        Assertions.assertEquals(0, Float.compare(floatValRev, TbUtils.parseBytesToFloat(floatValList, 0, 4, false)));


        // 1.1803216E8f == 0x4CE120E4
        floatValByte = new byte[]{0x4C, (byte) 0xE1, (byte) 0x20, (byte) 0xE4};
        float floatExpectedBe = 118.03216f;
        float actualBe = TbUtils.parseBytesToFloat(floatValByte, 0, 4, true);
        Assertions.assertEquals(0, Float.compare(floatExpectedBe, actualBe / 1000000));
        floatValList = Bytes.asList(floatValByte);
        actualBe = TbUtils.parseBytesToFloat(floatValList, 0);
        Assertions.assertEquals(0, Float.compare(floatExpectedBe, actualBe / 1000000));

        float floatExpectedLe = 8.0821E-41f;
        Assertions.assertEquals(0, Float.compare(floatExpectedLe, TbUtils.parseBytesToFloat(floatValByte, 0, 2, false)));
        floatExpectedBe = 2.7579E-41f;
        Assertions.assertEquals(0, Float.compare(floatExpectedBe, TbUtils.parseBytesToFloat(floatValByte, 0, 2)));


        floatExpectedLe = 3.019557E-39f;
        Assertions.assertEquals(0, Float.compare(floatExpectedLe, TbUtils.parseBytesToFloat(floatValList, 0, 3, false)));

        // 4 294 967 295L == {0xFF, 0xFF, 0xFF, 0xFF}
        floatValByte = new byte[]{-1, -1, -1, -1};
        String message = "is a Not-a-Number (NaN) value";
        try {
            TbUtils.parseBytesToFloat(floatValByte, 0, 4, true);
            Assertions.fail("Should throw NumberFormatException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains(message));
        }

        // "01752B0367FA000500010488 FFFFFFFF FFFFFFFF 33";
        String intToHexBe = "01752B0367FA000500010488FFFFFFFFFFFFFFFF33";
        floatValList = TbUtils.hexToBytes(ctx, intToHexBe);
        try {
            TbUtils.parseBytesToFloat(floatValList, 12, 4, false);
            Assertions.fail("Should throw NumberFormatException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains(message));
        }
    }

    @Test
    public void parseBytesIntToFloat() {
        byte[] intValByte = {0x00, 0x00, 0x00, 0x0A};
        float valueExpected = 10.0f;
        float valueActual = TbUtils.parseBytesIntToFloat(intValByte, 3, 1, true);
        Assertions.assertEquals(valueExpected, valueActual);
        valueActual = TbUtils.parseBytesIntToFloat(intValByte, 3, 1, false);
        Assertions.assertEquals(valueExpected, valueActual);

        valueActual = TbUtils.parseBytesIntToFloat(intValByte, 2, 2, true);
        Assertions.assertEquals(valueExpected, valueActual);
        valueExpected = 2560.0f;
        valueActual = TbUtils.parseBytesIntToFloat(intValByte, 2, 2, false);
        Assertions.assertEquals(valueExpected, valueActual);

        valueExpected = 10.0f;
        valueActual = TbUtils.parseBytesIntToFloat(intValByte, 0, 4, true);
        Assertions.assertEquals(valueExpected, valueActual);
        valueExpected = 167772.16f;
        double factor = 1e3;
        valueActual = (float) (TbUtils.parseBytesIntToFloat(intValByte, 0, 4, false) / factor);

        Assertions.assertEquals(valueExpected, valueActual);

        String dataAT101 = "0x01756403671B01048836BF7701F000090722050000";
        List<Byte> byteAT101 = TbUtils.hexToBytes(ctx, dataAT101);
        float latitudeExpected = 24.62495f;
        int offset = 9;
        factor = 1e6;
        valueActual = (float) (TbUtils.parseBytesIntToFloat(byteAT101, offset, 4, false) / factor);
        Assertions.assertEquals(latitudeExpected, valueActual);

        float longitudeExpected = 118.030576f;
        valueActual = (float) (TbUtils.parseBytesIntToFloat(byteAT101, offset + 4, 4, false) / factor);
        Assertions.assertEquals(longitudeExpected, valueActual);

        valueExpected = 9.185175E8f;
        valueActual = TbUtils.parseBytesIntToFloat(byteAT101, offset);
        Assertions.assertEquals(valueExpected, valueActual);
        // 0x36BF
        valueExpected = 14015.0f;
        valueActual = TbUtils.parseBytesIntToFloat(byteAT101, offset, 2);
        Assertions.assertEquals(valueExpected, valueActual);
        // 0xBF36
        valueExpected = 48950.0f;
        valueActual = TbUtils.parseBytesIntToFloat(byteAT101, offset, 2, false);
        Assertions.assertEquals(valueExpected, valueActual);

        valueExpected = 0.0f;
        valueActual = TbUtils.parseBytesIntToFloat(byteAT101, byteAT101.size());
        Assertions.assertEquals(valueExpected, valueActual);

        try {
            TbUtils.parseBytesIntToFloat(byteAT101, byteAT101.size() + 1);
            Assertions.fail("Should throw NumberFormatException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("is out of bounds for array with length:"));
        }
    }

    @Test
    public void parseLong() {
        Assertions.assertNull(TbUtils.parseLong(null));
        Assertions.assertNull(TbUtils.parseLong(""));
        Assertions.assertNull(TbUtils.parseLong(" "));

        Assertions.assertEquals((Long) 0L, TbUtils.parseLong("0"));
        Assertions.assertEquals((Long) 0L, TbUtils.parseLong("-0"));
        Assertions.assertEquals(java.util.Optional.of(473L).get(), TbUtils.parseLong("473"));
        Assertions.assertEquals(java.util.Optional.of(-65535L).get(), TbUtils.parseLong("-0xFFFF"));
        Assertions.assertEquals(java.util.Optional.of(4294967295L).get(), TbUtils.parseLong("FFFFFFFF"));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("0xFGFFFFFF"));

        Assertions.assertEquals(java.util.Optional.of(13158L).get(), TbUtils.parseLong("11001101100110", MIN_RADIX));
        Assertions.assertEquals(java.util.Optional.of(-13158L).get(), TbUtils.parseLong("1111111111111111111111111111111111111111111111111100110010011010", MIN_RADIX));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("11001101100210", MIN_RADIX));

        Assertions.assertEquals(java.util.Optional.of(9223372036854775807L).get(), TbUtils.parseLong("777777777777777777777", 8));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("1787", 8));

        Assertions.assertEquals(java.util.Optional.of(-255L).get(), TbUtils.parseLong("-FF", 16));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("FG", 16));


        Assertions.assertEquals(java.util.Optional.of(Long.MAX_VALUE).get(), TbUtils.parseLong(Long.toString(Long.MAX_VALUE), 10));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(1)).toString(10), 10));
        Assertions.assertEquals(java.util.Optional.of(Long.MIN_VALUE).get(), TbUtils.parseLong(Long.toString(Long.MIN_VALUE), 10));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(1)).toString(10), 10));

        Assertions.assertEquals(java.util.Optional.of(218840926543L).get(), TbUtils.parseLong("KonaLong", 27));
        Assertions.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("KonaLong", 10));

        Assertions.assertEquals(longVal, TbUtils.parseHexToLong(longValHex));
        Assertions.assertEquals(longVal, TbUtils.parseHexToLong(longValHexRev, false));
        Assertions.assertEquals(longVal, TbUtils.parseBigEndianHexToLong(longValHex));
        Assertions.assertEquals(longVal, TbUtils.parseLittleEndianHexToLong(longValHexRev));
    }

    @Test
    public void parseBytesToLong() {
        byte[] longValByte = {64, -101, 4, -79, 12, -78, -107, -22};
        Assertions.assertEquals(longVal, TbUtils.parseBytesToLong(longValByte, 0, 8));
        Bytes.reverse(longValByte);
        Assertions.assertEquals(longVal, TbUtils.parseBytesToLong(longValByte, 0, 8, false));

        List<Byte> longVaList = Bytes.asList(longValByte);
        Assertions.assertEquals(longVal, TbUtils.parseBytesToLong(longVaList, 0, 8, false));
        long longValRev = 0xEA95B20CB1049B40L;
        Assertions.assertEquals(longValRev, TbUtils.parseBytesToLong(longVaList, 0, 8));
    }

    @Test
    public void parsDouble() {
        String doubleValStr = "1.1428250947E8";
        Assertions.assertEquals(Double.parseDouble(doubleValStr), TbUtils.parseDouble(doubleValStr));
        doubleValStr = "1729.1729";
        Assertions.assertEquals(0, Double.compare(doubleVal, TbUtils.parseHexToDouble(longValHex)));
        Assertions.assertEquals(0, Double.compare(doubleValRev, TbUtils.parseHexToDouble(longValHex, false)));
        Assertions.assertEquals(0, Double.compare(doubleVal, TbUtils.parseBigEndianHexToDouble(longValHex)));
        Assertions.assertEquals(0, Double.compare(doubleVal, TbUtils.parseLittleEndianHexToDouble(longValHexRev)));
    }

    @Test
    public void toFixedDouble() {
        double actualD = TbUtils.toFixed(doubleVal, 3);
        Assertions.assertEquals(-1, Double.compare(doubleVal, actualD));
        Assertions.assertEquals(0, Double.compare(1729.173, actualD));
    }

    @Test
    public void parseBytesToDouble() {
        byte[] doubleValByte = {0x0A};
        Assertions.assertEquals(0, Double.compare(4.9E-323, TbUtils.parseBytesToDouble(doubleValByte)));

        doubleValByte = new byte[]{64, -101, 4, -79, 12, -78, -107, -22};
        Assertions.assertEquals(0, Double.compare(doubleVal, TbUtils.parseBytesToDouble(doubleValByte, 0)));
        Assertions.assertEquals(0, Double.compare(doubleValRev, TbUtils.parseBytesToDouble(doubleValByte, 0, 8, false)));

        List<Byte> doubleValList = Bytes.asList(doubleValByte);
        Assertions.assertEquals(0, Double.compare(doubleVal, TbUtils.parseBytesToDouble(doubleValList, 0)));
        Assertions.assertEquals(0, Double.compare(doubleValRev, TbUtils.parseBytesToDouble(doubleValList, 0, 8, false)));

        doubleValByte = new byte[]{0x7F, (byte) 0xC0, (byte) 0xFF, 0x00, 0x7F, (byte) 0xC0, (byte) 0xFF, 0x00};
        double doubleExpectedBe = 2387013.651780523d;
        double doubleExpectedLe = 7.234601680440024E-304d;
        double actualBe = TbUtils.parseBytesToDouble(doubleValByte, 0, 8, true);
        BigDecimal bigDecimal = new BigDecimal(actualBe);
        // We move the decimal point to the left by 301 positions
        actualBe = bigDecimal.movePointLeft(301).doubleValue();
        Assertions.assertEquals(0, Double.compare(doubleExpectedBe, actualBe));
        Assertions.assertEquals(0, Double.compare(doubleExpectedLe, TbUtils.parseBytesToDouble(doubleValByte, 0, 8, false)));

        doubleValList = Bytes.asList(doubleValByte);
        actualBe = TbUtils.parseBytesToDouble(doubleValList, 0);
        bigDecimal = new BigDecimal(actualBe);
        actualBe = bigDecimal.movePointLeft(301).doubleValue();
        Assertions.assertEquals(0, Double.compare(doubleExpectedBe, actualBe));
        doubleExpectedLe = 26950.174646662283d;
        double actualLe = TbUtils.parseBytesToDouble(doubleValList, 0, 5, false);
        bigDecimal = new BigDecimal(actualLe);
        actualLe = bigDecimal.movePointRight(316).doubleValue();
        Assertions.assertEquals(0, Double.compare(doubleExpectedLe, actualLe));

        // 4 294 967 295L == {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF}
        doubleValByte = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1};
        String message = "is a Not-a-Number (NaN) value";
        try {
            TbUtils.parseBytesToDouble(doubleValByte, 0, 8, true);
            Assertions.fail("Should throw NumberFormatException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains(message));
        }
    }

    @Test
    public void parseBytesLongToDouble() {
        byte[] longValByte = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0A};
        Double valueExpected = 10.0d;
        Double valueActual = TbUtils.parseBytesLongToDouble(longValByte);
        Assertions.assertEquals(valueExpected, valueActual);
        valueActual = TbUtils.parseBytesLongToDouble(longValByte, 7, 1, true);
        Assertions.assertEquals(valueExpected, valueActual);
        valueActual = TbUtils.parseBytesLongToDouble(longValByte, 7, 1, false);
        Assertions.assertEquals(valueExpected, valueActual);

        valueActual = TbUtils.parseBytesLongToDouble(longValByte, 6, 2, true);
        Assertions.assertEquals(valueExpected, valueActual);
        valueExpected = 2560.0d;
        valueActual = TbUtils.parseBytesLongToDouble(longValByte, 6, 2, false);
        Assertions.assertEquals(valueExpected, valueActual);

        valueExpected = 10.0d;
        valueActual = TbUtils.parseBytesLongToDouble(longValByte, 0, 8, true);
        Assertions.assertEquals(valueExpected, valueActual);
        valueExpected = 7.2057594037927936E17d;
        valueActual = TbUtils.parseBytesLongToDouble(longValByte, 0, 8, false);
        Assertions.assertEquals(valueExpected, valueActual);

        String dataPalacKiyv = "0x32D009423F23B300B0106E08D96B6C00";
        List<Byte> byteAT101 = TbUtils.hexToBytes(ctx, dataPalacKiyv);
        double latitudeExpected = 50.422775429058610d;
        int offset = 0;
        double factor = 1e15;
        valueActual = TbUtils.parseBytesLongToDouble(byteAT101, offset, 8, false);
        Assertions.assertEquals(latitudeExpected, valueActual / factor);

        double longitudeExpected = 30.517877378257072d;
        valueActual = TbUtils.parseBytesLongToDouble(byteAT101, offset + 8, 8, false);
        Assertions.assertEquals(longitudeExpected, valueActual / factor);
    }

    @Test
    public void parseBytesDecodeToJson() throws IOException, IllegalAccessException {
        String expectedStr = "{\"hello\": \"world\"}";
        ExecutionHashMap<String, Object> expectedJson = new ExecutionHashMap<>(1, ctx);
        expectedJson.put("hello", "world");
        List<Byte> expectedBytes = TbUtils.stringToBytes(ctx, expectedStr);
        Object actualJson = TbUtils.decodeToJson(ctx, expectedBytes);
        Assertions.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void parseStringDecodeToJson() throws IOException {
        String expectedStr = "{\"hello\": \"world\"}";
        ExecutionHashMap<String, Object> expectedJson = new ExecutionHashMap<>(1, ctx);
        expectedJson.put("hello", "world");
        Object actualJson = TbUtils.decodeToJson(ctx, expectedStr);
        Assertions.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void stringToBytesInputObjectTest() throws IOException, IllegalAccessException {
        String expectedStr = "{\"hello\": \"world\"}";
        Object inputJson = TbUtils.decodeToJson(ctx, expectedStr);
        byte[] arrayBytes = {119, 111, 114, 108, 100};
        List<Byte> expectedBytes = Bytes.asList(arrayBytes);
        List<Byte> actualBytes = TbUtils.stringToBytes(ctx, ((ExecutionHashMap) inputJson).get("hello"));
        Assertions.assertEquals(expectedBytes, actualBytes);
        actualBytes = TbUtils.stringToBytes(ctx, ((ExecutionHashMap) inputJson).get("hello"), "UTF-8");
        Assertions.assertEquals(expectedBytes, actualBytes);

        expectedStr = "{\"hello\": 1234}";
        inputJson = TbUtils.decodeToJson(ctx, expectedStr);
        Object finalInputJson = inputJson;
        Assertions.assertThrows(IllegalAccessException.class, () -> TbUtils.stringToBytes(ctx, ((ExecutionHashMap) finalInputJson).get("hello")));
        Assertions.assertThrows(IllegalAccessException.class, () -> TbUtils.stringToBytes(ctx, ((ExecutionHashMap) finalInputJson).get("hello"), "UTF-8"));
    }

    @Test
    public void bytesFromList() {
        byte[] arrayBytes = {(byte) 0x00, (byte) 0x08, (byte) 0x10, (byte) 0x1C, (byte) 0xFF, (byte) 0xFC, (byte) 0xAD, (byte) 0x88, (byte) 0x75, (byte) 0x74, (byte) 0x8A, (byte) 0x82};
        Object[] arrayMix = {"0x00", 8, "16", "0x1C", 255, (byte) 0xFC, 173, 136, 117, 116, -118, "-126"};

        String expected = new String(arrayBytes);
        ArrayList<Byte> listBytes = new ArrayList<>(arrayBytes.length);
        for (Byte element : arrayBytes) {
            listBytes.add(element);
        }
        Assertions.assertEquals(expected, TbUtils.bytesToString(listBytes));

        ArrayList<Object> listMix = new ArrayList<>(arrayMix.length);
        Collections.addAll(listMix, arrayMix);
        Assertions.assertEquals(expected, TbUtils.bytesToString(listMix));
    }

    @Test
    public void bytesFromList_SpecSymbol() {
        List<String> listHex = new ArrayList<>(Arrays.asList("1D", "0x1D", "1F", "0x1F", "0x20", "0x20"));
        byte[] expectedBytes = new byte[]{29, 29, 31, 31, 32, 32};
        String actualStr = TbUtils.bytesToString(listHex);
        byte[] actualBytes = actualStr.getBytes();
        Assertions.assertArrayEquals(expectedBytes, actualBytes);
        assertTrue(actualStr.isBlank());
        listHex = new ArrayList<>(Arrays.asList("0x21", "0x21"));
        expectedBytes = new byte[]{33, 33};
        actualStr = TbUtils.bytesToString(listHex);
        actualBytes = actualStr.getBytes();
        Assertions.assertArrayEquals(expectedBytes, actualBytes);
        assertFalse(actualStr.isBlank());
        Assertions.assertEquals("!!", actualStr);
        listHex = new ArrayList<>(Arrays.asList("21", "0x21"));
        expectedBytes = new byte[]{21, 33};
        actualStr = TbUtils.bytesToString(listHex);
        actualBytes = actualStr.getBytes();
        Assertions.assertArrayEquals(expectedBytes, actualBytes);
        assertFalse(actualStr.isBlank());
        Assertions.assertEquals("!", actualStr.substring(1));
        Assertions.assertEquals('\u0015', actualStr.charAt(0));
        Assertions.assertEquals(21, actualStr.charAt(0));
    }

    @Test
    public void bytesFromList_Error() {
        List<String> listHex = new ArrayList<>();
        listHex.add("0xFG");
        try {
            TbUtils.bytesToString(listHex);
            Assertions.fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            assertTrue(e.getMessage().contains("Value: \"FG\" is not numeric or hexDecimal format!"));
        }

        List<String> listIntString = new ArrayList<>();
        listIntString.add("-129");
        try {
            TbUtils.bytesToString(listIntString);
            Assertions.fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            assertTrue(e.getMessage().contains("The value '-129' could not be correctly converted to a byte. " +
                    "Integer to byte conversion requires the use of only 8 bits (with a range of min/max = -128/255)!"));
        }

        listIntString.add(0, "256");
        try {
            TbUtils.bytesToString(listIntString);
            Assertions.fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            assertTrue(e.getMessage().contains("The value '256' could not be correctly converted to a byte. " +
                    "Integer to byte conversion requires the use of only 8 bits (with a range of min/max = -128/255)!"));
        }

        ArrayList<Integer> listIntBytes = new ArrayList<>();
        listIntBytes.add(-129);
        try {
            TbUtils.bytesToString(listIntBytes);
            Assertions.fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            assertTrue(e.getMessage().contains("The value '-129' could not be correctly converted to a byte. " +
                    "Integer to byte conversion requires the use of only 8 bits (with a range of min/max = -128/255)!"));
        }

        listIntBytes.add(0, 256);
        try {
            TbUtils.bytesToString(listIntBytes);
            Assertions.fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            assertTrue(e.getMessage().contains("The value '256' could not be correctly converted to a byte. " +
                    "Integer to byte conversion requires the use of only 8 bits (with a range of min/max = -128/255)!"));
        }

        ArrayList<Object> listObjects = new ArrayList<>();
        ArrayList<String> listStringObjects = new ArrayList<>();
        listStringObjects.add("0xFD");
        listObjects.add(listStringObjects);
        try {
            TbUtils.bytesToString(listObjects);
            Assertions.fail("Should throw NumberFormatException");
        } catch (NumberFormatException e) {
            assertTrue(e.getMessage().contains("The value '[0xFD]' could not be correctly converted to a byte. " +
                    "Must be a HexDecimal/String/Integer/Byte format !"));
        }
    }

    @Test
    public void encodeDecodeUri_Test() {
        String uriOriginal = "-_.!~*'();/?:@&=+$,#ht://example.ж д a/path with spaces/?param1=Київ 1&param2=Україна2";
        String uriEncodeExpected = "-_.!~*'();/?:@&=+$,#ht://example.%D0%B6%20%D0%B4%20a/path%20with%20spaces/?param1=%D0%9A%D0%B8%D1%97%D0%B2%201&param2=%D0%A3%D0%BA%D1%80%D0%B0%D1%97%D0%BD%D0%B02";
        String uriEncodeActual = TbUtils.encodeURI(uriOriginal);
        Assertions.assertEquals(uriEncodeExpected, uriEncodeActual);

        String uriDecodeActual = TbUtils.decodeURI(uriEncodeActual);
        Assertions.assertEquals(uriOriginal, uriDecodeActual);
    }

    @Test
    public void intToHex_Test() {
        Assertions.assertEquals("FFF5EE", TbUtils.intToHex(-2578));
        Assertions.assertEquals("0xFFD8FFA6", TbUtils.intToHex(0xFFD8FFA6, true, true));
        Assertions.assertEquals("0xA6FFD8FF", TbUtils.intToHex(0xFFD8FFA6, false, true));
        Assertions.assertEquals("0x7FFFFFFF", TbUtils.intToHex(Integer.MAX_VALUE, true, true));
        Assertions.assertEquals("0x80000000", TbUtils.intToHex(Integer.MIN_VALUE, true, true));
        Assertions.assertEquals("0xAB", TbUtils.intToHex(0xAB, true, true));
        Assertions.assertEquals("0xABCD", TbUtils.intToHex(0xABCD, true, true));
        Assertions.assertEquals("0xABCDEF", TbUtils.intToHex(0xABCDEF, true, true));
        Assertions.assertEquals("0xCDAB", TbUtils.intToHex(0xABCDEF, false, true, 4));
        Assertions.assertEquals("0xAB", TbUtils.intToHex(171, true, true));
        Assertions.assertEquals("0xAB", TbUtils.intToHex(0xAB, false, true));
        Assertions.assertEquals("0xAB", TbUtils.intToHex(0xAB, true, true, 2));
        Assertions.assertEquals("AB", TbUtils.intToHex(0xAB, false, false, 2));
        Assertions.assertEquals("AB", TbUtils.intToHex(171, true, false));
        Assertions.assertEquals("0xAB", TbUtils.intToHex(0xAB, true, true));
        Assertions.assertEquals("0xAB", TbUtils.intToHex(0xAB, false, true));
        Assertions.assertEquals("AB", TbUtils.intToHex(0xAB, false, false));

        Assertions.assertEquals("0xABCD", TbUtils.intToHex(0xABCD, true, true));
        Assertions.assertEquals("0xCDAB", TbUtils.intToHex(0xABCD, false, true));
        Assertions.assertEquals("0xCD", TbUtils.intToHex(0xABCD, true, true, 2));
        Assertions.assertEquals("AB", TbUtils.intToHex(0xABCD, false, false, 2));
    }

    @Test
    public void longToHex_Test() {
        Assertions.assertEquals("0x7FFFFFFFFFFFFFFF", TbUtils.longToHex(Long.MAX_VALUE, true, true));
        Assertions.assertEquals("0x8000000000000000", TbUtils.longToHex(Long.MIN_VALUE, true, true));
        Assertions.assertEquals("0xFFD8FFA6FFD8FFA6", TbUtils.longToHex(0xFFD8FFA6FFD8FFA6L, true, true));
        Assertions.assertEquals("0xA6FFD8FFA6FFCEFF", TbUtils.longToHex(0xFFCEFFA6FFD8FFA6L, false, true));
        Assertions.assertEquals("0xAB", TbUtils.longToHex(0xABL, true, true));
        Assertions.assertEquals("0xABCD", TbUtils.longToHex(0xABCDL, true, true));
        Assertions.assertEquals("0xABCDEF", TbUtils.longToHex(0xABCDEFL, true, true));
        Assertions.assertEquals("0xABEFCDAB", TbUtils.longToHex(0xABCDEFABCDEFL, false, true, 8));
        Assertions.assertEquals("0xAB", TbUtils.longToHex(0xABL, true, true, 2));
        Assertions.assertEquals("AB", TbUtils.longToHex(0xABL, false, false, 2));

        Assertions.assertEquals("0xFFA6", TbUtils.longToHex(0xFFD8FFA6FFD8FFA6L, true, true, 4));
        Assertions.assertEquals("D8FF", TbUtils.longToHex(0xFFD8FFA6FFD8FFA6L, false, false, 4));
    }

    @Test
    public void numberToString_Test() {
        Assertions.assertEquals("00111010", TbUtils.intLongToRadixString(58L, MIN_RADIX));
        Assertions.assertEquals("0000000010011110", TbUtils.intLongToRadixString(158L, MIN_RADIX));
        Assertions.assertEquals("00000000000000100000001000000001", TbUtils.intLongToRadixString(131585L, MIN_RADIX));
        Assertions.assertEquals("0111111111111111111111111111111111111111111111111111111111111111", TbUtils.intLongToRadixString(Long.MAX_VALUE, MIN_RADIX));
        Assertions.assertEquals("1000000000000000000000000000000000000000000000000000000000000001", TbUtils.intLongToRadixString(-Long.MAX_VALUE, MIN_RADIX));
        Assertions.assertEquals("1111111111111111111111111111111111111111111111111111111110011010", TbUtils.intLongToRadixString(-102L, MIN_RADIX));
        Assertions.assertEquals("1111111111111111111111111111111111111111111111111100110010011010", TbUtils.intLongToRadixString(-13158L, MIN_RADIX));
        Assertions.assertEquals("777777777777777777777", TbUtils.intLongToRadixString(Long.MAX_VALUE, 8));
        Assertions.assertEquals("1000000000000000000000", TbUtils.intLongToRadixString(Long.MIN_VALUE, 8));
        Assertions.assertEquals("9223372036854775807", TbUtils.intLongToRadixString(Long.MAX_VALUE));
        Assertions.assertEquals("-9223372036854775808", TbUtils.intLongToRadixString(Long.MIN_VALUE));
        Assertions.assertEquals("3366", TbUtils.intLongToRadixString(13158L, 16));
        Assertions.assertEquals("FFCC9A", TbUtils.intLongToRadixString(-13158L, 16));
        Assertions.assertEquals("0xFFCC9A", TbUtils.intLongToRadixString(-13158L, 16, true, true));

        Assertions.assertEquals("0x0400", TbUtils.intLongToRadixString(1024L, 16, true, true));
        Assertions.assertNotEquals("400", TbUtils.intLongToRadixString(1024L, 16));
        Assertions.assertEquals("0xFFFC00", TbUtils.intLongToRadixString(-1024L, 16, true, true));
        Assertions.assertNotEquals("0xFC00", TbUtils.intLongToRadixString(-1024L, 16, true, true));

        Assertions.assertEquals("hazelnut", TbUtils.intLongToRadixString(1356099454469L, MAX_RADIX));
    }

    @Test
    public void intToHexWithPrintUnsignedBytes_Test() {
        Integer value = -40;
        String intToHexLe = TbUtils.intToHex(value, false, true);
        String intToHexBe = TbUtils.intToHex(value, true, true);

        List<Byte> hexTopByteLe = TbUtils.hexToBytes(ctx, intToHexLe);
        List<Byte> hexTopByteBe = TbUtils.hexToBytes(ctx, intToHexBe);

        byte[] arrayBytes = {-40, -1};
        List<Byte> expectedHexTopByteLe = Bytes.asList(arrayBytes);
        List<Byte> expectedHexTopByteBe = Lists.reverse(expectedHexTopByteLe);
        Assertions.assertEquals(expectedHexTopByteLe, hexTopByteLe);
        Assertions.assertEquals(expectedHexTopByteBe, hexTopByteBe);

        List<Integer> actualLe = TbUtils.printUnsignedBytes(ctx, hexTopByteLe);
        List<Integer> actualBe = TbUtils.printUnsignedBytes(ctx, hexTopByteBe);
        List<Integer> expectedLe = Ints.asList(216, 255);
        List<Integer> expectedBe = Lists.reverse(expectedLe);
        Assertions.assertEquals(expectedLe, actualLe);
        Assertions.assertEquals(expectedBe, actualBe);
    }

    @Test
    public void hexToBytes_Test() {
        String input = "0x01752B0367FA000500010488FFFFFFFFFFFFFFFF33";
        byte[] expected = {1, 117, 43, 3, 103, -6, 0, 5, 0, 1, 4, -120, -1, -1, -1, -1, -1, -1, -1, -1, 51};
        List<Byte> actualList = TbUtils.hexToBytes(ctx, input);
        Assertions.assertEquals(toList(expected), actualList);
        String validInput = "AABBCCDDEE";
        expected = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE};
        byte[] actualBytes = TbUtils.hexToBytesArray(validInput);
        Assertions.assertArrayEquals(expected, actualBytes);
        try {
            input = "0x01752B0367FA000500010488FFFFFFFFFFFFFFFF3";
            TbUtils.hexToBytes(ctx, input);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Hex string must be even-length."));
        }
        try {
            input = "0x01752B0367KA000500010488FFFFFFFFFFFFFFFF33";
            TbUtils.hexToBytes(ctx, input);
        } catch (NumberFormatException e) {
            assertTrue(e.getMessage().contains("Value: \"" + input + "\" is not numeric or hexDecimal format!"));
        }
        try {
            input = "";
            TbUtils.hexToBytes(ctx, input);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Hex string must be not empty"));
        }
        try {
            input = null;
            TbUtils.hexToBytes(ctx, input);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Hex string must be not empty"));
        }
    }

    @Test
    public void floatToHex_Test() {
        Float value = 123456789.00f;
        String expectedHex = "0x4CEB79A3";
        String valueHexRev = "0xA379EB4C";
        String actual = TbUtils.floatToHex(value);
        Assertions.assertEquals(expectedHex, actual);
        Float valueActual = TbUtils.parseHexToFloat(actual);
        Assertions.assertEquals(value, valueActual);
        valueActual = TbUtils.parseHexToFloat(valueHexRev, false);
        Assertions.assertEquals(value, valueActual);
        value = 123456789.67f;
        expectedHex = "0x4CEB79A3";
        valueHexRev = "0xA379EB4C";
        actual = TbUtils.floatToHex(value);
        Assertions.assertEquals(expectedHex, actual);
        valueActual = TbUtils.parseHexToFloat(actual);
        Assertions.assertEquals(value, valueActual);
        valueActual = TbUtils.parseHexToFloat(valueHexRev, false);
        Assertions.assertEquals(value, valueActual);
        value = 10.0f;
        expectedHex = "0x41200000";
        valueHexRev = "0x00002041";
        actual = TbUtils.floatToHex(value);
        Assertions.assertEquals(expectedHex, actual);
        valueActual = TbUtils.parseHexToFloat(actual);
        Assertions.assertEquals(value, valueActual);
        valueActual = TbUtils.parseHexToFloat(valueHexRev, false);
        Assertions.assertEquals(value, valueActual);

        String valueHex = "0x0000000A";
        float expectedValue = 1.4E-44f;
        valueActual = TbUtils.parseHexToFloat(valueHex);
        Assertions.assertEquals(expectedValue, valueActual);
        actual = TbUtils.floatToHex(expectedValue);
        Assertions.assertEquals(valueHex, actual);
    }

    // If the length is not equal to 8 characters, we process it as an integer (eg "0x0A" for 10.0f).
    @Test
    public void parseHexIntLongToFloat_Test() {
        Float valueExpected = 10.0f;
        Float valueActual = TbUtils.parseHexIntLongToFloat("0x0A", true);
        Assertions.assertEquals(valueExpected, valueActual);
        valueActual = TbUtils.parseHexIntLongToFloat("0x0A", false);
        Assertions.assertEquals(valueExpected, valueActual);
        valueActual = TbUtils.parseHexIntLongToFloat("0x00000A", true);
        Assertions.assertEquals(valueExpected, valueActual);
        valueActual = TbUtils.parseHexIntLongToFloat("0x0A0000", false);
        Assertions.assertEquals(valueExpected, valueActual);
        valueExpected = 2570.0f;
        valueActual = TbUtils.parseHexIntLongToFloat("0x000A0A", true);
        Assertions.assertEquals(valueExpected, valueActual);
        valueActual = TbUtils.parseHexIntLongToFloat("0x0A0A00", false);
        Assertions.assertEquals(valueExpected, valueActual);
    }

    @Test
    public void doubleToHex_Test() {
        String expectedHex = "0x409B04B10CB295EA";
        String actual = TbUtils.doubleToHex(doubleVal);
        Assertions.assertEquals(expectedHex, actual);
        Double valueActual = TbUtils.parseHexToDouble(actual);
        Assertions.assertEquals(doubleVal, valueActual);
        actual = TbUtils.doubleToHex(doubleVal, false);
        String expectedHexRev = "0xEA95B20CB1049B40";
        Assertions.assertEquals(expectedHexRev, actual);

        String valueHex = "0x000000000000000A";
        Double expectedValue = 4.9E-323;
        valueActual = TbUtils.parseHexToDouble(valueHex);
        Assertions.assertEquals(expectedValue, valueActual);
        actual = TbUtils.doubleToHex(expectedValue);
        Assertions.assertEquals(valueHex, actual);
    }

    @Test
    public void raiseError_Test() {
        Object value = 4;
        String message = "frequency_weighting_type must be 0, 1 or 2. A value of " + value.toString() + " is invalid.";

        try {
            TbUtils.raiseError(message);
            Assertions.fail("Should throw NumberFormatException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("frequency_weighting_type must be 0, 1 or 2. A value of 4 is invalid."));
        }
        message = "frequency_weighting_type must be 0, 1 or 2.";
        try {
            TbUtils.raiseError(message);
            Assertions.fail("Should throw NumberFormatException");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("frequency_weighting_type must be 0, 1 or 2."));
        }
    }

    @Test
    public void isBinary_Test() {
        Assertions.assertEquals(2, TbUtils.isBinary("1100110"));
        Assertions.assertEquals(-1, TbUtils.isBinary("2100110"));
    }

    @Test
    public void isOctal_Test() {
        Assertions.assertEquals(8, TbUtils.isOctal("4567734"));
        Assertions.assertEquals(-1, TbUtils.isOctal("8100110"));
    }

    @Test
    public void isDecimal_Test() {
        Assertions.assertEquals(10, TbUtils.isDecimal("4567039"));
        Assertions.assertEquals(10, TbUtils.isDecimal("1.1428250947E8"));
        Assertions.assertEquals(10, TbUtils.isDecimal("123.45"));
        Assertions.assertEquals(10, TbUtils.isDecimal("-1.23E-4"));
        Assertions.assertEquals(10, TbUtils.isDecimal("1E5"));
        Assertions.assertEquals(-1, TbUtils.isDecimal("C100110"));
        Assertions.assertEquals(-1, TbUtils.isDecimal("abc"));
        Assertions.assertEquals(-1, TbUtils.isDecimal(null));
    }

    @Test
    public void isHexadecimal_Test() {
        Assertions.assertEquals(16, TbUtils.isHexadecimal("F5D7039"));
        Assertions.assertEquals(-1, TbUtils.isHexadecimal("K100110"));
    }

    @Test
    public void padStart_Test() {
        String binaryString = "010011";
        String expected = "00010011";
        Assertions.assertEquals(expected, TbUtils.padStart(binaryString, 8, '0'));
        binaryString = "1001010011";
        expected = "1001010011";
        Assertions.assertEquals(expected, TbUtils.padStart(binaryString, 8, '0'));
        binaryString = "1001010011";
        expected = "******1001010011";
        Assertions.assertEquals(expected, TbUtils.padStart(binaryString, 16, '*'));
        String fullNumber = "203439900FFCD5581";
        String last4Digits = fullNumber.substring(11);
        expected = "***********CD5581";
        Assertions.assertEquals(expected, TbUtils.padStart(last4Digits, fullNumber.length(), '*'));
    }

    @Test
    public void padEnd_Test() {
        String binaryString = "010011";
        String expected = "01001100";
        Assertions.assertEquals(expected, TbUtils.padEnd(binaryString, 8, '0'));
        binaryString = "1001010011";
        expected = "1001010011";
        Assertions.assertEquals(expected, TbUtils.padEnd(binaryString, 8, '0'));
        binaryString = "1001010011";
        expected = "1001010011******";
        Assertions.assertEquals(expected, TbUtils.padEnd(binaryString, 16, '*'));
        String fullNumber = "203439900FFCD5581";
        String last4Digits = fullNumber.substring(0, 11);
        expected = "203439900FF******";
        Assertions.assertEquals(expected, TbUtils.padEnd(last4Digits, fullNumber.length(), '*'));
    }

    @Test
    public void parseByteToBinaryArray_Test() {
        byte byteVal = 0x39;
        byte[] bitArray = {0, 0, 1, 1, 1, 0, 0, 1};
        List expected = toList(bitArray);
        byte[] actualArray = TbUtils.parseByteToBinaryArray(byteVal);
        List actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);

        bitArray = new byte[]{1, 1, 1, 0, 0, 1};
        expected = toList(bitArray);
        actualArray = TbUtils.parseByteToBinaryArray(byteVal, bitArray.length);
        actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);

        bitArray = new byte[]{1, 0, 0, 1, 1, 1};
        expected = toList(bitArray);
        actualArray = TbUtils.parseByteToBinaryArray(byteVal, bitArray.length, false);
        actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void parseBytesToBinaryArray_Test() {
        long longValue = 57L;
        byte[] bytesValue = new byte[]{0x39};   // 57
        List<Byte> listValue = toList(bytesValue);
        byte[] bitArray = {0, 0, 1, 1, 1, 0, 0, 1};
        List expected = toList(bitArray);
        byte[] actualArray = TbUtils.parseBytesToBinaryArray(listValue);
        List actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);

        actualArray = TbUtils.parseLongToBinaryArray(longValue, listValue.size() * 8);
        actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);

        bitArray = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1};
        expected = toList(bitArray);
        actualArray = TbUtils.parseLongToBinaryArray(longValue);
        actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);

        longValue = 52914L;
        bytesValue = new byte[]{(byte) 0xCE, (byte) 0xB2};   // 52914
        listValue = toList(bytesValue);
        bitArray = new byte[]{1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0};
        expected = toList(bitArray);
        actualArray = TbUtils.parseBytesToBinaryArray(listValue);
        actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);

        actualArray = TbUtils.parseLongToBinaryArray(longValue, listValue.size() * 8);
        actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);

        bitArray = new byte[]{0, 0, 1, 0};
        expected = toList(bitArray);
        actualArray = TbUtils.parseLongToBinaryArray(longValue, 4);
        actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);

        bitArray = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0};
        expected = toList(bitArray);
        actualArray = TbUtils.parseLongToBinaryArray(longValue);
        actual = toList(actualArray);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void parseBinaryArrayToInt_Test() {
        byte byteVal = (byte) 0x9F;
        int expectedVolt = 31;
        byte[] actualArray = TbUtils.parseByteToBinaryArray(byteVal);
        int actual = TbUtils.parseBinaryArrayToInt(actualArray);
        Assertions.assertEquals(byteVal, actual);
        int actualVolt = TbUtils.parseBinaryArrayToInt(actualArray, 1, 7);
        Assertions.assertEquals(expectedVolt, actualVolt);
        boolean expectedLowVoltage = true;
        boolean actualLowVoltage = actualArray[7] == 1;
        Assertions.assertEquals(expectedLowVoltage, actualLowVoltage);

        byteVal = (byte) 0x39;
        actualArray = TbUtils.parseByteToBinaryArray(byteVal, 6, false);
        int expectedLowCurrent1Alarm = 1;  //  0x39 = 00 11 10 01 (Bin0): 0 == 1
        int expectedHighCurrent1Alarm = 0; //  0x39 = 00 11 10 01 (Bin1): 0 == 0
        int expectedLowCurrent2Alarm = 0;  //  0x39 = 00 11 10 01 (Bin2): 0 == 0
        int expectedHighCurrent2Alarm = 1; //  0x39 = 00 11 10 01 (Bin3): 0 == 1
        int expectedLowCurrent3Alarm = 1;  //  0x39 = 00 11 10 01 (Bin4): 0 == 1
        int expectedHighCurrent3Alarm = 1; //  0x39 = 00 11 10 01 (Bin5): 0 == 1
        int actualLowCurrent1Alarm = actualArray[0];
        int actualHighCurrent1Alarm = actualArray[1];
        int actualLowCurrent2Alarm = actualArray[2];
        int actualHighCurrent2Alarm = actualArray[3];
        int actualLowCurrent3Alarm = actualArray[4];
        int actualHighCurrent3Alarm = actualArray[5];
        Assertions.assertEquals(expectedLowCurrent1Alarm, actualLowCurrent1Alarm);
        Assertions.assertEquals(expectedHighCurrent1Alarm, actualHighCurrent1Alarm);
        Assertions.assertEquals(expectedLowCurrent2Alarm, actualLowCurrent2Alarm);
        Assertions.assertEquals(expectedHighCurrent2Alarm, actualHighCurrent2Alarm);
        Assertions.assertEquals(expectedLowCurrent3Alarm, actualLowCurrent3Alarm);
        Assertions.assertEquals(expectedHighCurrent3Alarm, actualHighCurrent3Alarm);

        byteVal = (byte) 0x36;
        actualArray = TbUtils.parseByteToBinaryArray(byteVal);
        int expectedMultiplier1 = 2;
        int expectedMultiplier2 = 1;
        int expectedMultiplier3 = 3;
        int actualMultiplier1 = TbUtils.parseBinaryArrayToInt(actualArray, 6, 2);
        int actualMultiplier2 = TbUtils.parseBinaryArrayToInt(actualArray, 4, 2);
        int actualMultiplier3 = TbUtils.parseBinaryArrayToInt(actualArray, 2, 2);
        Assertions.assertEquals(expectedMultiplier1, actualMultiplier1);
        Assertions.assertEquals(expectedMultiplier2, actualMultiplier2);
        Assertions.assertEquals(expectedMultiplier3, actualMultiplier3);
    }

    @Test
    public void hexToBase64_Test() {
        String hex = "014A000A02202007060000014A019F03E800C81B5801014A029F030A0000000000014A032405DD05D41B5836014A049F39000000000000";
        String expected = "AUoACgIgIAcGAAABSgGfA+gAyBtYAQFKAp8DCgAAAAAAAUoDJAXdBdQbWDYBSgSfOQAAAAAAAA==";
        String actual = TbUtils.hexToBase64(hex);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void base64ToBytesList_Test() {
        String validInput = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4, 5});
        ExecutionArrayList<Byte> actual = TbUtils.base64ToBytesList(ctx, validInput);
        ExecutionArrayList<Byte> expected = new ExecutionArrayList<>(ctx);
        expected.addAll(List.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5));
        Assertions.assertEquals(expected, actual);

        String emptyInput = Base64.getEncoder().encodeToString(new byte[]{});
        actual = TbUtils.base64ToBytesList(ctx, emptyInput);
        assertTrue(actual.isEmpty());
        String invalidInput = "NotAValidBase64String";
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            TbUtils.base64ToBytesList(ctx, invalidInput);
        });
        Assertions.assertThrows(NullPointerException.class, () -> {
            TbUtils.base64ToBytesList(ctx, null);
        });
    }

    @Test
    public void bytesToHex_Test() {
        byte[] bb = {(byte) 0xBB, (byte) 0xAA};
        String expected = "BBAA";
        String actual = TbUtils.bytesToHex(bb);
        Assertions.assertEquals(expected, actual);
        ExecutionArrayList<Integer> expectedList = new ExecutionArrayList<>(ctx);
        expectedList.addAll(List.of(-69, 83));
        expected = "BB53";
        actual = TbUtils.bytesToHex(expectedList);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void toInt() {
        Assertions.assertEquals(1729, TbUtils.toInt(doubleVal));
        Assertions.assertEquals(13, TbUtils.toInt(12.8));
        Assertions.assertEquals(28, TbUtils.toInt(28.0));
    }

    @Test
    public void roundResult() {
        Assertions.assertEquals(1729.1729, TbUtils.roundResult(doubleVal, null));
        Assertions.assertEquals(1729, TbUtils.roundResult(doubleVal, 0));
        Assertions.assertEquals(1729.17, TbUtils.roundResult(doubleVal, 2));
    }

    @Test
    public void isNaN() {
        assertFalse(TbUtils.isNaN(doubleVal));
        assertTrue(TbUtils.isNaN(Double.NaN));
    }

    @Test
    public void isInsidePolygon() {
        // outside the polygon
        String perimeter = "[[[50.75581142688204,29.097910166341073],[50.16785158177623,29.35066098977171],[50.164329922384674,29.773743889862114],[50.16785158177623,30.801230932938843],[50.459245308833495,30.92760634465418],[50.486522489629564,30.68548421850448],[50.703612031034005,30.872660513473573]],[[50.606017492632766,29.36165015600782],[50.54317104075835,29.762754723626013],[50.41021974600505,29.455058069014804]]]";
        assertFalse(TbUtils.isInsidePolygon(50.50869555168039, 30.80123093293884, perimeter));
        // inside the polygon
        assertTrue(TbUtils.isInsidePolygon(50.50520628167696, 30.339685951022016, perimeter));
        // inside the hole
        assertFalse(TbUtils.isInsidePolygon(50.52265651287081, 29.488025567723156, perimeter));
    }

    @Test
    public void isInsideCircle() {
        // outside the circle
        String perimeter = "{\"latitude\":50.32254778825905,\"longitude\":28.207787701215757,\"radius\":47477.33130420423}";
        assertFalse(TbUtils.isInsideCircle(50.81490715736681, 28.05943395702824, perimeter));
        // inside the circle
        assertTrue(TbUtils.isInsideCircle(50.599397971892444, 28.086906872618542, perimeter));
    }

    @Test
    public void isMap() throws ExecutionException, InterruptedException {
        LinkedHashMap<String, Object> msg = new LinkedHashMap<>(Map.of("temperature", 42, "nested", "508"));
        assertTrue(TbUtils.isMap(msg));
        assertFalse(TbUtils.isList(msg));
    }

    @Test
    public void isList() throws ExecutionException, InterruptedException {
        List<Integer> list = List.of(0x35);
        assertTrue(TbUtils.isList(list));
        assertFalse(TbUtils.isMap(list));
        assertFalse(TbUtils.isArray(list));
        assertFalse(TbUtils.isSet(list));
    }

    @Test
    public void isArray() throws ExecutionException, InterruptedException {
        byte [] array = new byte[]{1, 2, 3};
        assertTrue(TbUtils.isArray(array));
        assertFalse(TbUtils.isList(array));
        assertFalse(TbUtils.isSet(array));
    }

    @Test
    public void isSet() throws ExecutionException, InterruptedException {
        Set<Byte> set = toSet(new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        assertTrue(TbUtils.isSet(set));
        assertFalse(TbUtils.isList(set));
        assertFalse(TbUtils.isArray(set));
    }
    @Test
    public void setTest() throws ExecutionException, InterruptedException {
        Set actual = TbUtils.newSet(ctx);
        Set expected = toSet(new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xCC});
        actual.add((byte) 0xDD);
        actual.add((byte) 0xCC);
        actual.add((byte) 0xCC);
        assertTrue(expected.containsAll(actual));
        List list = toList(new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        actual.addAll(list);
        assertEquals(4, actual.size());
        assertTrue(actual.containsAll(expected));
        actual = TbUtils.toSet(ctx, list);
        expected = toSet(new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xDA});
        actual.add((byte) 0xDA);
        actual.remove((byte) 0xBB);
        actual.remove((byte) 0xAA);
        assertTrue(expected.containsAll(actual));
        assertEquals(actual.size(), 3);
        actual.clear();
        assertTrue(actual.isEmpty());
        actual = TbUtils.toSet(ctx, list);
        Set actualClone = TbUtils.toSet(ctx, list);
        Set actualClone_asc = TbUtils.toSet(ctx, list);
        Set actualClone_desc = TbUtils.toSet(ctx, list);
        ((ExecutionLinkedHashSet<?>)actualClone).sort();
        ((ExecutionLinkedHashSet<?>)actualClone_asc).sort(true);
        ((ExecutionLinkedHashSet<?>)actualClone_desc).sort(false);
        assertEquals(list.toString(), actual.toString());
        assertNotEquals(list.toString(), actualClone.toString());
        Collections.sort(list);
        assertEquals(list.toString(), actualClone.toString());
        assertEquals(list.toString(), actualClone_asc.toString());
        Collections.sort(list, Collections.reverseOrder());
        assertNotEquals(list.toString(), actualClone_asc.toString());
        assertEquals(list.toString(), actualClone_desc.toString());
    }

    private static List<Byte> toList(byte[] data) {
        List<Byte> result = new ArrayList<>(data.length);
        for (Byte b : data) {
            result.add(b);
        }
        return result;
    }

    private static Set<Byte> toSet(byte[] data) {
        Set<Byte> result = new LinkedHashSet<>();
        for (Byte b : data) {
            result.add(b);
        }
        return result;
    }
}

