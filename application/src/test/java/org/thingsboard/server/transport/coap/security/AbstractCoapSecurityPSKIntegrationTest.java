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
package org.thingsboard.server.transport.coap.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
@TestPropertySource(properties = {
        "coap.enabled=true",
        "coap.dtls.enabled=true",
        "device.connectivity.coaps.enabled=true",
        "service.integrations.supported=ALL",
        "transport.coap.enabled=true",
})
public abstract class AbstractCoapSecurityPSKIntegrationTest extends AbstractCoapIntegrationTest {

    @Value("${coap.dtls.psk.secret_key:server_psk_secret_key_123}")
    private String pskSecretKey;

    protected void clientPSKUpdateFeatureTypeTest(FeatureType featureType) throws Exception {
        CoapClientDTLSTest clientPSK = clientPSKUpdateTest(featureType, null);
        clientPSK.disconnect();
    }
    protected void twoClientPSKConnectTest() throws Exception {
        Integer fixedPort =  getFreePort();
        CoapClientDTLSTest clientPSK = clientPSKUpdateTest(FeatureType.ATTRIBUTES, fixedPort);
        clientPSK.disconnect();
        await("Need to make port " + fixedPort + " free")
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> isPortAvailable(fixedPort));
        CoapClientDTLSTest clientPSK_01 = clientPSKUpdateTest(FeatureType.ATTRIBUTES, fixedPort, PAYLOAD_VALUES_STR_01);
        clientPSK_01.disconnect();
    }

    private CoapClientDTLSTest clientPSKUpdateTest(FeatureType featureType, Integer fixedPort) throws Exception {
        return clientPSKUpdateTest(featureType, fixedPort, null);
    }

    private CoapClientDTLSTest clientPSKUpdateTest(FeatureType featureType, Integer fixedPort, String payload) throws Exception {
        String payloadValuesStr = payload == null ? PAYLOAD_VALUES_STR : payload;
        CoapClientDTLSTest clientPSK = new CoapClientDTLSTest(accessToken, pskSecretKey, featureType, COAPS_BASE_URL + accessToken + "/", fixedPort);
        CoapResponse coapResponsePSK = clientPSK.postMethod(payloadValuesStr);
        assertNotNull(coapResponsePSK);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponsePSK.getCode());

        if (FeatureType.ATTRIBUTES.equals(featureType)) {
            DeviceId deviceId = savedDevice.getId();
            JsonNode expectedNode = JacksonUtil.toJsonNode(payloadValuesStr);
            List<String> expectedKeys = getKeysFromNode(expectedNode);
            List<String> actualKeys = getActualKeysList(deviceId, expectedKeys, "attributes/CLIENT_SCOPE");
            assertNotNull(actualKeys);

            Set<String> actualKeySet = new HashSet<>(actualKeys);
            Set<String> expectedKeySet = new HashSet<>(expectedKeys);
            assertEquals(expectedKeySet, actualKeySet);

            String getAttributesValuesUrl = getAttributesValuesUrl(deviceId, actualKeySet, "attributes/CLIENT_SCOPE");
            List<Map<String, Object>> actualValues = doGetAsyncTyped(getAttributesValuesUrl, new TypeReference<>() {
            });
            assertValuesList(actualValues, expectedNode);
        }
        return clientPSK;
    }

    private List<String> getActualKeysList(DeviceId deviceId, List<String> expectedKeys, String apiSuffix) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/" + apiSuffix, new TypeReference<>() {
            });
            if (actualKeys.size() == expectedKeys.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return actualKeys;
    }

    private String getAttributesValuesUrl(DeviceId deviceId, Set<String> actualKeySet, String apiSuffix) {
        return "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/" + apiSuffix + "?keys=" + String.join(",", actualKeySet);
    }

    private List getKeysFromNode(JsonNode jNode) {
        List<String> jKeys = new ArrayList<>();
        Iterator<String> fieldNames = jNode.fieldNames();
        while (fieldNames.hasNext()) {
            jKeys.add(fieldNames.next());
        }
        return jKeys;
    }

    protected void assertValuesList(List<Map<String, Object>> actualValues, JsonNode expectedValues) {
        assertTrue(actualValues.size() > 0);
        assertEquals(expectedValues.size(), actualValues.size());
        for (Map<String, Object> map : actualValues) {
            String key = (String) map.get("key");
            Object actualValue = map.get("value");
            assertTrue(expectedValues.has(key));
            JsonNode expectedValue = expectedValues.get(key);
            assertExpectedActualValue(expectedValue, actualValue);
        }
    }

    protected void assertExpectedActualValue(JsonNode expectedValue, Object actualValue) {
        switch (expectedValue.getNodeType()) {
            case STRING:
                assertEquals(expectedValue.asText(), actualValue);
                break;
            case NUMBER:
                if (expectedValue.isInt()) {
                    assertEquals(expectedValue.asInt(), actualValue);
                } else if (expectedValue.isLong()) {
                    assertEquals(expectedValue.asLong(), actualValue);
                } else if (expectedValue.isFloat() || expectedValue.isDouble()) {
                    assertEquals(expectedValue.asDouble(), actualValue);
                }
                break;
            case BOOLEAN:
                assertEquals(expectedValue.asBoolean(), actualValue);
                break;
            case ARRAY:
            case OBJECT:
                expectedValue.toString().equals(JacksonUtil.toString(actualValue));
                break;
            default:
                break;
        }
    }

    private static int getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

