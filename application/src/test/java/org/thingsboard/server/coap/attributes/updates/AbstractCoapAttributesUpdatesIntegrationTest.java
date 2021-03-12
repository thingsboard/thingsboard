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
package org.thingsboard.server.coap.attributes.updates;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.coap.attributes.AbstractCoapAttributesIntegrationTest;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapAttributesUpdatesIntegrationTest extends AbstractCoapAttributesIntegrationTest {

    private static final String RESPONSE_ATTRIBUTES_PAYLOAD_DELETED = "{\"deleted\":[\"attribute5\"]}";

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
        processTestSubscribeToAttributesUpdates();
    }

    protected void processTestSubscribeToAttributesUpdates() throws Exception {

        CoapClient client = getCoapClient(FeatureType.ATTRIBUTES);

        CountDownLatch latch = new CountDownLatch(1);
        TestCoapCallback testCoapCallback = new TestCoapCallback(latch);

        Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        client.observe(request, testCoapCallback);

        Thread.sleep(1000);

        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        latch.await(3, TimeUnit.SECONDS);

        validateUpdateAttributesResponse(testCoapCallback);

        latch = new CountDownLatch(1);

        doDelete("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/SHARED_SCOPE?keys=attribute5", String.class);
        latch.await(3, TimeUnit.SECONDS);

        validateDeleteAttributesResponse(testCoapCallback);
    }

    protected void validateUpdateAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(0, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(POST_ATTRIBUTES_PAYLOAD), JacksonUtil.toJsonNode(response));
    }

    protected void validateDeleteAttributesResponse(TestCoapCallback callback) throws InvalidProtocolBufferException {
        assertNotNull(callback.getPayloadBytes());
        assertNotNull(callback.getObserve());
        assertEquals(1, callback.getObserve().intValue());
        String response = new String(callback.getPayloadBytes(), StandardCharsets.UTF_8);
        assertEquals(JacksonUtil.toJsonNode(RESPONSE_ATTRIBUTES_PAYLOAD_DELETED), JacksonUtil.toJsonNode(response));
    }

    protected class TestCoapCallback implements CoapHandler {

        private final CountDownLatch latch;

        private Integer observe;
        private byte[] payloadBytes;

        public byte[] getPayloadBytes() {
            return payloadBytes;
        }

        public Integer getObserve() {
            return observe;
        }

        private TestCoapCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onLoad(CoapResponse response) {
            assertNotNull(response.getPayload());
            assertEquals(response.getCode(), CoAP.ResponseCode.CONTENT);
            observe = response.getOptions().getObserve();
            payloadBytes = response.getPayload();
//            if (!isOneWayRpc) {
//                processOnLoadResponse(response, client, observe, latch);
//            } else {
//                latch.countDown();
//            }
            latch.countDown();
        }

        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }

    }
}
