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
package org.thingsboard.server.transport.coap.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestCallback;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.query.EntityKeyType.CLIENT_ATTRIBUTE;
import static org.thingsboard.server.common.data.query.EntityKeyType.SHARED_ATTRIBUTE;

@Slf4j
@DaoSqlTest
public class CoapClientIntegrationTest extends AbstractCoapIntegrationTest {

    private static final List<String> EXPECTED_KEYS = Arrays.asList("key1", "key2", "key3", "key4", "key5");
    private static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Attributes device")
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testConfirmableRequests() throws Exception {
        boolean confirmable = true;
        processAttributesTest(confirmable);
        processTwoWayRpcTest(confirmable);
        processTestRequestAttributesValuesFromTheServer(confirmable);
    }

    @Test
    public void testNonConfirmableRequests() throws Exception {
        boolean confirmable = false;
        processAttributesTest(confirmable);
        processTwoWayRpcTest(confirmable);
        processTestRequestAttributesValuesFromTheServer(confirmable);
    }

    protected void processAttributesTest(boolean confirmable) throws Exception {
        client = createClientForFeatureWithConfirmableParameter(FeatureType.ATTRIBUTES, confirmable);
        CoapResponse coapResponse = client.postMethod(PAYLOAD_VALUES_STR.getBytes());
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());
        assertEquals("CoAP response type is wrong!", client.getType(), coapResponse.advanced().getType());

        DeviceId deviceId = savedDevice.getId();
        List<String> actualKeys = getActualKeysList(deviceId);
        assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(EXPECTED_KEYS);
        assertEquals(expectedKeySet, actualKeySet);

        String attributesValuesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        ;
        List<Map<String, Object>> values = doGetAsyncTyped(attributesValuesUrl, new TypeReference<>() {
        });
        assertAttributesValues(values, actualKeySet);
        String deleteAttributesUrl = "/api/plugins/telemetry/DEVICE/" + deviceId + "/CLIENT_SCOPE?keys=" + String.join(",", actualKeySet);
        doDelete(deleteAttributesUrl);
    }

    protected void processTwoWayRpcTest(boolean confirmable) throws Exception {
        client = createClientForFeatureWithConfirmableParameter(FeatureType.RPC, confirmable);
        CoapTestCallback callbackCoap = new TestCoapCallbackForRPC(client);

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap, confirmable);
        String awaitAlias = "await Two Way Rpc (client.getObserveRelation)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.VALID.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        0 == callbackCoap.getObserve());
        validateCurrentStateNotification(callbackCoap);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        int expectedObserveCountAfterGpioRequest1 = callbackCoap.getObserve() + 1;
        String actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await Two Way Rpc (setGpio(method, params, value) first";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest1 == callbackCoap.getObserve());
        validateTwoWayStateChangedNotification(callbackCoap, actualResult);

        int expectedObserveCountAfterGpioRequest2 = callbackCoap.getObserve() + 1;
        actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await Two Way Rpc (setGpio(method, params, value) second";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest2 == callbackCoap.getObserve());

        validateTwoWayStateChangedNotification(callbackCoap, actualResult);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    protected void processTestRequestAttributesValuesFromTheServer(boolean confirmable) throws Exception {
        client = createClientForFeatureWithConfirmableParameter(FeatureType.ATTRIBUTES, confirmable);
        SingleEntityFilter dtf = new SingleEntityFilter();
        dtf.setSingleEntity(AliasEntityId.fromEntityId(savedDevice.getId()));
        List<EntityKey> csKeys = getEntityKeys(CLIENT_ATTRIBUTE);
        List<EntityKey> shKeys = getEntityKeys(SHARED_ATTRIBUTE);
        List<EntityKey> keys = new ArrayList<>();
        keys.addAll(csKeys);
        keys.addAll(shKeys);
        getWsClient().subscribeLatestUpdate(keys, dtf);
        getWsClient().registerWaitForUpdate(2);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE",
                PAYLOAD_VALUES_STR, String.class, status().isOk());

        CoapResponse coapResponse = client.postMethod(PAYLOAD_VALUES_STR);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());

        String update = getWsClient().waitForUpdate();
        assertThat(update).as("ws update received").isNotBlank();

        String keysParam = String.join(",", EXPECTED_KEYS);
        String featureTokenUrl = CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + keysParam + "&sharedKeys=" + keysParam;
        client.setURI(featureTokenUrl);
        CoapResponse response = client.getMethod();
        assertEquals("CoAP response type is wrong!", client.getType(), response.advanced().getType());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void assertAttributesValues(List<Map<String, Object>> deviceValues, Set<String> keySet) {
        for (Map<String, Object> map : deviceValues) {
            String key = (String) map.get("key");
            Object value = map.get("value");
            assertTrue(keySet.contains(key));
            switch (key) {
                case "key1":
                    assertEquals("value1", value);
                    break;
                case "key2":
                    assertEquals(true, value);
                    break;
                case "key3":
                    assertEquals(3.0, value);
                    break;
                case "key4":
                    assertEquals(4, value);
                    break;
                case "key5":
                    assertNotNull(value);
                    assertEquals(3, ((LinkedHashMap) value).size());
                    assertEquals(42, ((LinkedHashMap) value).get("someNumber"));
                    assertEquals(Arrays.asList(1, 2, 3), ((LinkedHashMap) value).get("someArray"));
                    LinkedHashMap<String, String> someNestedObject = (LinkedHashMap) ((LinkedHashMap) value).get("someNestedObject");
                    assertEquals("value", someNestedObject.get("key"));
                    break;
            }
        }
    }

    private List<String> getActualKeysList(DeviceId deviceId) throws Exception {
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {
            });
            if (actualKeys.size() == EXPECTED_KEYS.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        return actualKeys;
    }

    private void validateCurrentStateNotification(CoapTestCallback callback) {
        assertArrayEquals(EMPTY_PAYLOAD, callback.getPayloadBytes());
    }

    private void validateTwoWayStateChangedNotification(CoapTestCallback callback, String actualResult) {
        assertEquals(DEVICE_RESPONSE, actualResult);
        assertNotNull(callback.getPayloadBytes());
    }

    protected class TestCoapCallbackForRPC extends CoapTestCallback {

        private final CoapTestClient client;

        @Getter
        private boolean wasSuccessful = false;

        TestCoapCallbackForRPC(CoapTestClient client) {
            this.client = client;
        }

        @Override
        public void onLoad(CoapResponse response) {
            payloadBytes = response.getPayload();
            responseCode = response.getCode();
            observe = response.getOptions().getObserve();
            wasSuccessful = client.getType().equals(response.advanced().getType());
            if (observe != null) {
                if (observe > 0) {
                    processOnLoadResponse(response, client);
                }
            }
        }

        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }
    }

    protected void processOnLoadResponse(CoapResponse response, CoapTestClient client) {
        JsonNode responseJson = JacksonUtil.fromBytes(response.getPayload());
        int requestId = responseJson.get("id").asInt();
        client.setURI(CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.RPC, requestId));
        client.postMethod(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                log.warn("RPC {} command response ack: {}", requestId, response.getCode());
            }

            @Override
            public void onError() {
                log.warn("RPC {} command response ack error, no connect", requestId);
            }
        }, DEVICE_RESPONSE, MediaTypeRegistry.APPLICATION_JSON);
    }

    private CoapTestClient createClientForFeatureWithConfirmableParameter(FeatureType featureType, boolean confirmable) {
        CoapTestClient coapTestClient = new CoapTestClient(accessToken, featureType);
        if (confirmable) {
            coapTestClient.useCONs();
        } else {
            coapTestClient.useNONs();
        }
        return coapTestClient;
    }

    private List<EntityKey> getEntityKeys(EntityKeyType scope) {
        return CoapClientIntegrationTest.EXPECTED_KEYS.stream().map(key -> new EntityKey(scope, key)).collect(Collectors.toList());
    }
}
