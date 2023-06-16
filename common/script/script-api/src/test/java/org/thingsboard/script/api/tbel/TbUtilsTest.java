/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionHashMap;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

@Slf4j

public class TbUtilsTest {

    private ExecutionContext ctx;

    @Before
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
        Assert.assertNotNull(ctx);
    }

    @After
    public void after() {
        ctx.stop();
    }

    @Test
    public void parseHexToInt() {
        Assert.assertEquals(0xAB, TbUtils.parseHexToInt("AB"));
        Assert.assertEquals(0xABBA, TbUtils.parseHexToInt("ABBA", true));
        Assert.assertEquals(0xBAAB, TbUtils.parseHexToInt("ABBA", false));
        Assert.assertEquals(0xAABBCC, TbUtils.parseHexToInt("AABBCC", true));
        Assert.assertEquals(0xAABBCC, TbUtils.parseHexToInt("CCBBAA", false));
        Assert.assertEquals(0xAABBCCDD, TbUtils.parseHexToInt("AABBCCDD", true));
        Assert.assertEquals(0xAABBCCDD, TbUtils.parseHexToInt("DDCCBBAA", false));
        Assert.assertEquals(0xDDCCBBAA, TbUtils.parseHexToInt("DDCCBBAA", true));
        Assert.assertEquals(0xDDCCBBAA, TbUtils.parseHexToInt("AABBCCDD", false));
    }

    @Test
    public void parseBytesToInt_checkPrimitives() {
        int expected = 257;
        byte[] data = ByteBuffer.allocate(4).putInt(expected).array();
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4));
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 2, 2, true));
        Assert.assertEquals(1, TbUtils.parseBytesToInt(data, 3, 1, true));

        expected = Integer.MAX_VALUE;
        data = ByteBuffer.allocate(4).putInt(expected).array();
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));

        expected = 0xAABBCCDD;
        data = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));
        data = new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, false));

        expected = 0xAABBCC;
        data = new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, true));
        data = new byte[]{(byte) 0xCC, (byte) 0xBB, (byte) 0xAA};
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, false));
    }

    @Test
    public void parseBytesToInt_checkLists() {
        int expected = 257;
        List<Byte> data = toList(ByteBuffer.allocate(4).putInt(expected).array());
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4));
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 2, 2, true));
        Assert.assertEquals(1, TbUtils.parseBytesToInt(data, 3, 1, true));

        expected = Integer.MAX_VALUE;
        data = toList(ByteBuffer.allocate(4).putInt(expected).array());
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));

        expected = 0xAABBCCDD;
        data = toList(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, true));
        data = toList(new byte[]{(byte) 0xDD, (byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 4, false));

        expected = 0xAABBCC;
        data = toList(new byte[]{(byte) 0xAA, (byte) 0xBB, (byte) 0xCC});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, true));
        data = toList(new byte[]{(byte) 0xCC, (byte) 0xBB, (byte) 0xAA});
        Assert.assertEquals(expected, TbUtils.parseBytesToInt(data, 0, 3, false));
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

        Assert.assertEquals(expectedMapWithPath, actualMapWithPaths);

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

        Assert.assertEquals(expectedMapWithoutPaths, actualMapWithoutPaths);
    }

    @Test
    public void parseInt() {
        Assert.assertNull(TbUtils.parseInt(null));
        Assert.assertNull(TbUtils.parseInt(""));
        Assert.assertNull(TbUtils.parseInt(" "));

        Assert.assertEquals(java.util.Optional.of(0).get(), TbUtils.parseInt("0"));
        Assert.assertEquals(java.util.Optional.of(0).get(), TbUtils.parseInt("-0"));
        Assert.assertEquals(java.util.Optional.of(473).get(), TbUtils.parseInt("473"));
        Assert.assertEquals(java.util.Optional.of(-255).get(), TbUtils.parseInt("-0xFF"));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("FF"));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("0xFG"));

        Assert.assertEquals(java.util.Optional.of(102).get(), TbUtils.parseInt("1100110", 2));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("1100210", 2));

        Assert.assertEquals(java.util.Optional.of(63).get(), TbUtils.parseInt("77", 8));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("18", 8));

        Assert.assertEquals(java.util.Optional.of(-255).get(), TbUtils.parseInt("-FF", 16));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("FG", 16));


        Assert.assertEquals(java.util.Optional.of(Integer.MAX_VALUE).get(), TbUtils.parseInt(Integer.toString(Integer.MAX_VALUE), 10));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt(BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.valueOf(1)).toString(10), 10));
        Assert.assertEquals(java.util.Optional.of(Integer.MIN_VALUE).get(), TbUtils.parseInt(Integer.toString(Integer.MIN_VALUE), 10));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt(BigInteger.valueOf(Integer.MIN_VALUE).subtract(BigInteger.valueOf(1)).toString(10), 10));

        Assert.assertEquals(java.util.Optional.of(506070563).get(), TbUtils.parseInt("KonaIn", 30));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("KonaIn", 10));
    }

    @Test
    public void parseLong() {
        Assert.assertNull(TbUtils.parseLong(null));
        Assert.assertNull(TbUtils.parseLong(""));
        Assert.assertNull(TbUtils.parseLong(" "));

        Assert.assertEquals(java.util.Optional.of(0L).get(), TbUtils.parseLong("0"));
        Assert.assertEquals(java.util.Optional.of(0L).get(), TbUtils.parseLong("-0"));
        Assert.assertEquals(java.util.Optional.of(473L).get(), TbUtils.parseLong("473"));
        Assert.assertEquals(java.util.Optional.of(-65535L).get(), TbUtils.parseLong("-0xFFFF"));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("FFFF"));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseInt("0xFGFF"));

        Assert.assertEquals(java.util.Optional.of(13158L).get(), TbUtils.parseLong("11001101100110", 2));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("11001101100210", 2));

        Assert.assertEquals(java.util.Optional.of(9223372036854775807L).get(), TbUtils.parseLong("777777777777777777777", 8));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("1787", 8));

        Assert.assertEquals(java.util.Optional.of(-255L).get(), TbUtils.parseLong("-FF", 16));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("FG", 16));


        Assert.assertEquals(java.util.Optional.of(Long.MAX_VALUE).get(), TbUtils.parseLong(Long.toString(Long.MAX_VALUE), 10));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(1)).toString(10), 10));
        Assert.assertEquals(java.util.Optional.of(Long.MIN_VALUE).get(), TbUtils.parseLong(Long.toString(Long.MIN_VALUE), 10));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(1)).toString(10), 10));

        Assert.assertEquals(java.util.Optional.of(218840926543L).get(), TbUtils.parseLong("KonaLong", 27));
        Assert.assertThrows(NumberFormatException.class, () -> TbUtils.parseLong("KonaLong", 10));
    }


                // parseLong String.class, int.class
// parseLittleEndianHexToLong String.class
    // parseBigEndianHexToLong String.class
    // parseHexToLong String.class
    // parseHexToLong String.class boolean.class
    // parseBytesToLong List.class, int.class, int.class
    // parseBytesToLong List.class, int.class, int.class, boolean.class
    // parseBytesToLong byte[].class, int.class, int.class
    // parseBytesToLong byte[].class, int.class, int.class, boolean.class
    // parseLittleEndianHexToFloat String.class
    // parseBigEndianHexToFloat String.class
    // parseHexToFloat String.class
    // parseHexToFloat String.class boolean.class
    // parseBytesToFloat byte[].class, int.class, boolean.class
    // parseBytesToFloat byte[].class, int.class
    // parseBytesToFloat List.class, int.class, boolean.class
    // parseBytesToFloat List.class, int.class
    // toFixed float.class, int.class
    // parseLittleEndianHexToDouble String.class
    // parseBigEndianHexToDouble String.class
    // parseHexToDouble String.class
    // parseHexToDouble String.class boolean.class
    // parseBytesToDouble byte[].class, int.class
    // parseBytesToDouble byte[].class, int.class, boolean.class
    // parseBytesToDouble List.class, int.class
    // parseBytesToDouble List.class, int.class boolean.class


    private static String keyToValue(String key, String extraSymbol) {
        return key + "Value" + (extraSymbol == null ? "" : extraSymbol);
    }

    private static List<Byte> toList(byte[] data) {
        List<Byte> result = new ArrayList<>(data.length);
        for (Byte b : data) {
            result.add(b);
        }
        return result;
    }

}
