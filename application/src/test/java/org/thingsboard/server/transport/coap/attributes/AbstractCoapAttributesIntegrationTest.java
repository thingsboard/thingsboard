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
package org.thingsboard.server.transport.coap.attributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractCoapAttributesIntegrationTest extends AbstractCoapIntegrationTest {

    protected static final long CLIENT_REQUEST_TIMEOUT = 60000L;

    protected static final String POST_ATTRIBUTES_PAYLOAD = "{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73," +
            "\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}";

    protected List<TransportProtos.TsKvProto> getTsKvProtoList() {
        TransportProtos.TsKvProto tsKvProtoAttribute1 = getTsKvProto("attribute1", "value1", TransportProtos.KeyValueType.STRING_V);
        TransportProtos.TsKvProto tsKvProtoAttribute2 = getTsKvProto("attribute2", "true", TransportProtos.KeyValueType.BOOLEAN_V);
        TransportProtos.TsKvProto tsKvProtoAttribute3 = getTsKvProto("attribute3", "42.0", TransportProtos.KeyValueType.DOUBLE_V);
        TransportProtos.TsKvProto tsKvProtoAttribute4 = getTsKvProto("attribute4", "73", TransportProtos.KeyValueType.LONG_V);
        TransportProtos.TsKvProto tsKvProtoAttribute5 = getTsKvProto("attribute5", "{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}", TransportProtos.KeyValueType.JSON_V);
        List<TransportProtos.TsKvProto> tsKvProtoList = new ArrayList<>();
        tsKvProtoList.add(tsKvProtoAttribute1);
        tsKvProtoList.add(tsKvProtoAttribute2);
        tsKvProtoList.add(tsKvProtoAttribute3);
        tsKvProtoList.add(tsKvProtoAttribute4);
        tsKvProtoList.add(tsKvProtoAttribute5);
        return tsKvProtoList;
    }

    protected TransportProtos.TsKvProto getTsKvProto(String key, String value, TransportProtos.KeyValueType keyValueType) {
        TransportProtos.TsKvProto.Builder tsKvProtoBuilder = TransportProtos.TsKvProto.newBuilder();
        TransportProtos.KeyValueProto keyValueProto = getKeyValueProto(key, value, keyValueType);
        tsKvProtoBuilder.setKv(keyValueProto);
        return tsKvProtoBuilder.build();
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
        //String featureTokenUrl = getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + keys + "&sharedKeys=" + keys;
        //client = getCoapClient(featureTokenUrl);

        //CoapResponse getAttributesResponse = client.setTimeout(CLIENT_REQUEST_TIMEOUT).get();
        //validateResponse(getAttributesResponse);

        //TODO - NEW -- YURIY
        String featureTokenUrl = CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.ATTRIBUTES) + "?clientKeys=" + keys + "&sharedKeys=" + keys;
        CoapTestClient client = new CoapTestClient(featureTokenUrl);

        validateResponse(client.GetMethod());
    }

    protected void postAttributes() throws Exception {
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId() + "/attributes/SHARED_SCOPE", POST_ATTRIBUTES_PAYLOAD, String.class, status().isOk());

        //client = getCoapClient(FeatureType.ATTRIBUTES);
        //CoapResponse coapResponse = client.setTimeout(CLIENT_REQUEST_TIMEOUT).post(POST_ATTRIBUTES_PAYLOAD.getBytes(), MediaTypeRegistry.APPLICATION_JSON);

        CoapTestClient client = new CoapTestClient(accessToken, FeatureType.ATTRIBUTES);
        CoapResponse coapResponse = client.PostMethod(POST_ATTRIBUTES_PAYLOAD);

        assertEquals(CoAP.ResponseCode.CREATED, coapResponse.getCode());
    }

    protected void validateResponse(CoapResponse getAttributesResponse) throws InvalidProtocolBufferException {
        assertEquals(CoAP.ResponseCode.CONTENT, getAttributesResponse.getCode());
        String expectedRequestPayload = "{\"client\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}},\"shared\":{\"attribute1\":\"value1\",\"attribute2\":true,\"attribute3\":42.0,\"attribute4\":73,\"attribute5\":{\"someNumber\":42,\"someArray\":[1,2,3],\"someNestedObject\":{\"key\":\"value\"}}}}";
        assertEquals(JacksonUtil.toJsonNode(expectedRequestPayload), JacksonUtil.toJsonNode(new String(getAttributesResponse.getPayload(), StandardCharsets.UTF_8)));
    }
}
