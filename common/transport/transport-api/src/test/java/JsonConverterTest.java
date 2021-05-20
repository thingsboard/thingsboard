/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;

@RunWith(MockitoJUnitRunner.class)
public class JsonConverterTest {

    private static final JsonParser JSON_PARSER = new JsonParser();

    @Test
    public void testParseBigDecimalAsLong() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 1E+1}"), 0L);
        Assert.assertEquals(10L, result.get(0L).get(0).getLongValue().get().longValue());
    }

    @Test
    public void testParseBigDecimalAsDouble() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 101E-1}"), 0L);
        Assert.assertEquals(10.1, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    @Test
    public void testParseAsDouble() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 1.1}"), 0L);
        Assert.assertEquals(1.1, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    @Test
    public void testParseAsLong() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 11}"), 0L);
        Assert.assertEquals(11L, result.get(0L).get(0).getLongValue().get().longValue());
    }

}
