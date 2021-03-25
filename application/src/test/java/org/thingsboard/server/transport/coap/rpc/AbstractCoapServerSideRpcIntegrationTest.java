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
package org.thingsboard.server.transport.coap.rpc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.junit.Assert;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        CoapClient client = getCoapClient(FeatureType.RPC);
        client.useCONs();

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback testCoapCallback = new TestCoapCallback(client, latch, true);

        Request request = Request.newGet().setObserve();
        CoapObserveRelation observeRelation = client.observe(request, testCoapCallback);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/plugins/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        Assert.assertTrue(StringUtils.isEmpty(result));
        latch.await(3, TimeUnit.SECONDS);
        assertEquals(0, testCoapCallback.getObserve().intValue());
        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    protected void processTwoWayRpcTest() throws Exception {
        CoapClient client = getCoapClient(FeatureType.RPC);
        client.useCONs();

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback testCoapCallback = new TestCoapCallback(client, latch, false);

        Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        CoapObserveRelation observeRelation = client.observe(request, testCoapCallback);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();

        String expected = "{\"value1\":\"A\",\"value2\":\"B\"}";

        String result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);

        assertEquals(expected, result);
        assertEquals(0, testCoapCallback.getObserve().intValue());
        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());

//        // TODO: 3/11/21 Fix test to validate next RPC
//        latch = new CountDownLatch(1);
//
//        result = doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
//        latch.await(3, TimeUnit.SECONDS);
//
//        assertEquals(expected, result);
//        assertEquals(1, testCoapCallback.getObserve().intValue());
    }

    protected void processOnLoadResponse(CoapResponse response, CoapClient client, Integer observe, CountDownLatch latch) {
        client.setURI(getRpcResponseFeatureTokenUrl(accessToken, observe));
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

    private class TestCoapCallback implements CoapHandler {

        private final CoapClient client;
        private final CountDownLatch latch;
        private final boolean isOneWayRpc;

        public Integer getObserve() {
            return observe;
        }

        private Integer observe;

        private TestCoapCallback(CoapClient client, CountDownLatch latch, boolean isOneWayRpc) {
            this.client = client;
            this.latch = latch;
            this.isOneWayRpc = isOneWayRpc;
        }

        @Override
        public void onLoad(CoapResponse response) {
            log.warn("coap response: {}, {}", response, response.getCode());
            assertNotNull(response.getPayload());
            assertEquals(response.getCode(), CoAP.ResponseCode.CONTENT);
            observe = response.getOptions().getObserve();
            if (!isOneWayRpc) {
                processOnLoadResponse(response, client, observe, latch);
            } else {
                latch.countDown();
            }
        }

        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }

    }

}
