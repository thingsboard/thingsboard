/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap.telemetry.timeseries;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Slf4j
public abstract class AbstractCoapTimeseriesIntegrationTest extends AbstractCoapIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device")
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testPushTelemetry() throws Exception {
        processJsonPayloadTelemetryTest(PAYLOAD_VALUES_STR.getBytes(), false);
    }

    @Test
    public void testPushTelemetryWithTs() throws Exception {
        String payloadStr = "{\"ts\": 10000, \"values\": " + PAYLOAD_VALUES_STR + "}";
        processJsonPayloadTelemetryTest(payloadStr.getBytes(), true);
    }

    protected void processJsonPayloadTelemetryTest(byte[] payloadBytes, boolean withTs) throws Exception {
        List<String> expectedKeys = Arrays.asList("key1", "key2", "key3", "key4", "key5");
        processTestPostTelemetry(payloadBytes, expectedKeys, withTs, false);
    }

    protected void processTestPostTelemetry(byte[] payloadBytes, List<String> expectedKeys, boolean withTs, boolean presenceFieldsTest) throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.TELEMETRY);
        CoapResponse coapResponse = client.postMethod(payloadBytes);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        DeviceId deviceId = savedDevice.getId();
        List<String> actualKeys = getActualKeysList(deviceId, expectedKeys);
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedKeys);
        assertEquals(expectedKeySet, actualKeySet);

        String getTelemetryValuesUrl;
        if (withTs) {
            getTelemetryValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?startTs=0&endTs=15000&keys=" + String.join(",", actualKeySet);
        } else {
            getTelemetryValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=" + String.join(",", actualKeySet);
        }
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;
        Map<String, List<Map<String, Object>>> values = null;
        while (start <= end) {
            values = doGetAsyncTyped(getTelemetryValuesUrl, new TypeReference<>() {});
            boolean valid = values.size() == expectedKeys.size();
            if (valid) {
                for (String key : expectedKeys) {
                    List<Map<String, Object>> tsValues = values.get(key);
                    if (tsValues != null && tsValues.size() > 0) {
                        Object ts = tsValues.get(0).get("ts");
                        if (ts == null) {
                            valid = false;
                            break;
                        }
                    } else {
                        valid = false;
                        break;
                    }
                }
            }
            if (valid) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        assertNotNull(values);

        if (withTs) {
            assertTs(values, expectedKeys, 10000, 0);
        }
        if (presenceFieldsTest) {
            assertExplicitProtoFieldValues(values);
        } else {
            assertValues(values, 0);
        }
    }

    private void assertTs(Map<String, List<Map<String, Object>>> deviceValues, List<String> expectedKeys, int ts, int arrayIndex) {
        assertEquals(ts, deviceValues.get(expectedKeys.get(0)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(1)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(2)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(3)).get(arrayIndex).get("ts"));
        assertEquals(ts, deviceValues.get(expectedKeys.get(4)).get(arrayIndex).get("ts"));
    }

    private void assertValues(Map<String, List<Map<String, Object>>> deviceValues, int arrayIndex) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : deviceValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> tsKv = entry.getValue();
            String value = (String) tsKv.get(arrayIndex).get("value");
            switch (key) {
                case "key1":
                    assertEquals("value1", value);
                    break;
                case "key2":
                    assertEquals("true", value);
                    break;
                case "key3":
                    assertEquals("3.0", value);
                    break;
                case "key4":
                    assertEquals("4", value);
                    break;
                case "key5":
                    assertEquals("{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", value);
                    break;
            }
        }
    }

    private void assertExplicitProtoFieldValues(Map<String, List<Map<String, Object>>> deviceValues) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : deviceValues.entrySet()) {
            String key = entry.getKey();
            List<Map<String, Object>> tsKv = entry.getValue();
            String value = (String) tsKv.get(0).get("value");
            switch (key) {
                case "key1":
                    assertEquals("", value);
                    break;
                case "key2":
                    assertEquals("false", value);
                    break;
                case "key3":
                    assertEquals("0.0", value);
                    break;
                case "key4":
                    assertEquals("0", value);
                    break;
                case "key5":
                    assertEquals("{\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", value);
                    break;
            }
        }
    }

    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/timeseries", new TypeReference<>() {});
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return actualKeys;
    }

}
