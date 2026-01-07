/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.adaptor;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

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

}
