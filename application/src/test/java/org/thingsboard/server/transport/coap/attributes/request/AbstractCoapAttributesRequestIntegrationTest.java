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
package org.thingsboard.server.transport.coap.attributes.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.transport.coap.attributes.AbstractCoapAttributesIntegrationTest;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapAttributesRequestIntegrationTest extends AbstractCoapAttributesIntegrationTest {

    protected static final long CLIENT_REQUEST_TIMEOUT = 60000L;

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest("Test Request attribute values from the server", null, null);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        processTestRequestAttributesValuesFromTheServer();
    }

    protected void processTestRequestAttributesValuesFromTheServer() throws Exception {
        postAttributes();

        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 5000;

        List<String> savedAttributeKeys = null;
        while (start <= end) {
            savedAttributeKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/keys/attributes/CLIENT_SCOPE", new TypeReference<>() {});
            if (savedAttributeKeys.size() == 5) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        assertNotNull(savedAttributeKeys);

        String keys = "attribute1,attribute2,attribute3,attribute4,attribute5";
        String featureTokenUrl = getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + keys + "&sharedKeys=" + keys;
        CoapClient client = getCoapClient(featureTokenUrl);

        CoapResponse getAttributesResponse = client.setTimeout(CLIENT_REQUEST_TIMEOUT).get();
        validateResponse(getAttributesResponse);
    }

    protected void postAttributes() throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        CoapClient client = getCoapClient(FeatureType.ATTRIBUTES);
        CoapResponse coapResponse = client.setTimeout(CLIENT_REQUEST_TIMEOUT).post(POST_ATTRIBUTES_PAYLOAD.getBytes(), MediaTypeRegistry.APPLICATION_JSON);
        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());
    }

    protected void validateResponse(CoapResponse getAttributesResponse) throws InvalidProtocolBufferException {
        assertEquals(CoAP.ResponseCode.CONTENT, getAttributesResponse.getCode());
        String expectedRequestPayload = "{\"client\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}},\"shared\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(getAttributesResponse.getPayload(), StandardCharsets.UTF_8)));
    }
}
