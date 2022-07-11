/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapServerSideRpcIntegrationTest extends AbstractCoapIntegrationTest {

    protected static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    protected Long asyncContextTimeoutToUseRpcPlugin;

    protected void processBeforeTest(String deviceName, CoapDeviceType coapDeviceType, TransportPayloadType payloadType) throws Exception {
        super.processBeforeTest(deviceName, coapDeviceType, payloadType);
        asyncContextTimeoutToUseRpcPlugin = 10000L;
    }

    protected void processOneWayRpcTest() throws Exception {
        client = getCoapClient(FeatureType.RPC);
        client.useCONs();

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback callback = new TestCoapCallback(client, latch, true);

        Request request = Request.newGet().setObserve();
        CoapObserveRelation observeRelation = client.observe(request, callback);

        latch.await(3, TimeUnit.SECONDS);

        validateCurrentStateNotification(callback);

        latch = new CountDownLatch(1);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());

        latch.await(3, TimeUnit.SECONDS);

        validateOneWayStateChangedNotification(callback, result);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    protected void processTwoWayRpcTest(String expectedResponseResult) throws Exception {
        client = getCoapClient(FeatureType.RPC);
        client.useCONs();

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback callback = new TestCoapCallback(client, latch, false);

        Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        CoapObserveRelation observeRelation = client.observe(request, callback);

        latch.await(3, TimeUnit.SECONDS);

        validateCurrentStateNotification(callback);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);

        validateTwoWayStateChangedNotification(callback, 1, expectedResponseResult, actualResult);

        latch = new CountDownLatch(1);

        actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);

        validateTwoWayStateChangedNotification(callback, 2, expectedResponseResult, actualResult);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    protected void processOnLoadResponse(CoapResponse response, CoapClient client, Integer observe, CountDownLatch latch) {
        JsonNode responseJson = JacksonUtil.fromBytes(response.getPayload());
        client.setURI(getRpcResponseFeatureTokenUrl(accessToken, responseJson.get("id").asInt()));
        client.post(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                log.warn("Command Response Ack: {}, {}", response.getCode(), response.getResponseText());
                latch.countDown();
            }

            @Override
            public void onError() {
                log.warn("Command Response Ack Error, No connect");
            }
        }, DEVICE_RESPONSE, MediaTypeRegistry.APPLICATION_JSON);
    }

    protected String getRpcResponseFeatureTokenUrl(String token, int requestId) {
        return COAP_BASE_URL + token + "/" + FeatureType.RPC.name().toLowerCase() + "/" + requestId;
    }

    protected class TestCoapCallback implements CoapHandler {

        private final CoapClient client;
        private final CountDownLatch latch;
        private final boolean isOneWayRpc;

        private Integer observe;
        private byte[] payloadBytes;
        private CoAP.ResponseCode responseCode;

        public Integer getObserve() {
            return observe;
        }

        public byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public CoAP.ResponseCode getResponseCode() {
            return responseCode;
        }

        TestCoapCallback(CoapClient client, CountDownLatch latch, boolean isOneWayRpc) {
            this.client = client;
            this.latch = latch;
            this.isOneWayRpc = isOneWayRpc;
        }

        @Override
        public void onLoad(CoapResponse response) {
            payloadBytes = response.getPayload();
            responseCode = response.getCode();
            observe = response.getOptions().getObserve();
            if (observe != null) {
                if (!isOneWayRpc && observe > 0) {
                    processOnLoadResponse(response, client, observe, latch);
                } else {
                    latch.countDown();
                }
            }
        }

        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }

    }

    private void validateCurrentStateNotification(TestCoapCallback callback) {
        assertArrayEquals(EMPTY_PAYLOAD, callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(callback.getResponseCode(), CoAP.ResponseCode.VALID);
        assertEquals(0, callback.getObserve().intValue());
    }

    private void validateOneWayStateChangedNotification(TestCoapCallback callback, String result) {
        assertTrue(StringUtils.isEmpty(result));
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(1, callback.getObserve().intValue());
    }

    private void validateTwoWayStateChangedNotification(TestCoapCallback callback, int expectedObserveNumber, String expectedResult, String actualResult) {
        assertEquals(expectedResult, actualResult);
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(expectedObserveNumber, callback.getObserve().intValue());
    }


}
