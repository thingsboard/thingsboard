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
package org.thingsboard.server.transport.coap.attributes.updates;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.coap.attributes.AbstractCoapAttributesIntegrationTest;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapAttributesUpdatesIntegrationTest extends AbstractCoapAttributesIntegrationTest {

    private static final String RESPONSE_ATTRIBUTES_PAYLOAD_DELETED = "{\"deleted\":[\"attribute5\"]}";

    protected static final String POST_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION = "{\"attribute1\":\"value\",\"attribute2\":false,\"attribute3\":41.0,\"attribute4\":72," +
            "\"attribute5\":{\"someNumber\":41,\"someArray\":[],\"someNestedObject\":{\"key\":\"value\"}}}";

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Subscribe to attribute updates", null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processTestSubscribeToAttributesUpdates(false);
    }

    @Test
    public void testSubscribeToAttributesUpdatesFromTheServerWithEmptyCurrentStateNotification() throws Exception {
        processTestSubscribeToAttributesUpdates(true);
    }

    protected void processTestSubscribeToAttributesUpdates(boolean emptyCurrentStateNotification) throws Exception {
        if (!emptyCurrentStateNotification) {
            doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION, String.class, status().isOk());
        }
        client = getCoapClient(FeatureType.ATTRIBUTES);

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback callback = new TestCoapCallback(latch);

        Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        CoapObserveRelation observeRelation = client.observe(request, callback);

        latch.await(3, TimeUnit.SECONDS);

        if (emptyCurrentStateNotification) {
            validateEmptyCurrentStateAttributesResponse(callback);
        } else {
            validateCurrentStateAttributesResponse(callback);
        }

        latch = new CountDownLatch(1);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);

        validateUpdateAttributesResponse(callback);

        latch = new CountDownLatch(1);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        latch.await(3, TimeUnit.SECONDS);

        validateDeleteAttributesResponse(callback);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    protected void validateCurrentStateAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(0, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(POST_ATTRIBUTES_PAYLOAD_ON_CURRENT_STATE_NOTIFICATION), JacksonUtil.toJsonNode(response));
    }

    protected void validateEmptyCurrentStateAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(0, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals("{}", response);
    }

    protected void validateUpdateAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(1, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(POST_ATTRIBUTES_PAYLOAD), JacksonUtil.toJsonNode(response));
    }

    protected void validateDeleteAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(CoAP.ResponseCode.CONTENT, callback.getResponseCode());
        assertEquals(2, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(RESPONSE_ATTRIBUTES_PAYLOAD_DELETED), JacksonUtil.toJsonNode(response));
    }

    protected static class TestCoapCallback implements CoapHandler {

        private final CountDownLatch latch;

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

        private TestCoapCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onLoad(CoapResponse response) {
            observe = response.getOptions().getObserve();
            payloadBytes = response.getPayload();
            responseCode = response.getCode();
            latch.countDown();
        }

        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }

    }
}
