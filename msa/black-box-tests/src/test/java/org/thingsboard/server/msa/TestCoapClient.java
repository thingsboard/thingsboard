/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.msa;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.io.IOException;

public class TestCoapClient {

    private static final String COAP_BASE_URL = "coap://localhost:5683/api/v1/";
    private static final long CLIENT_REQUEST_TIMEOUT = 60000L;

    private final CoapClient client;

    public TestCoapClient(){
        this.client = createClient();
    }

    public TestCoapClient(String accessToken, FeatureType featureType) {
        this.client = createClient(getFeatureTokenUrl(accessToken, featureType));
    }

    public TestCoapClient(String featureTokenUrl) {
        this.client = createClient(featureTokenUrl);
    }

    public void connectToCoap(String accessToken) {
        setURI(accessToken, null);
    }

    public void connectToCoap(String accessToken, FeatureType featureType) {
        setURI(accessToken, featureType);
    }

    public void disconnect() {
        if (client != null) {
            client.shutdown();
        }
    }

    public CoapResponse postMethod(String requestBody) throws ConnectorException, IOException {
        return this.postMethod(requestBody.getBytes());
    }

    public CoapResponse postMethod(byte[] requestBodyBytes) throws ConnectorException, IOException {
        return client.setTimeout(CLIENT_REQUEST_TIMEOUT).post(requestBodyBytes, MediaTypeRegistry.APPLICATION_JSON);
    }

    public void postMethod(CoapHandler handler, String payload, int format) {
        client.post(handler, payload, format);
    }

    public void postMethod(CoapHandler handler, byte[] payload, int format) {
        client.post(handler, payload, format);
    }

    public CoapResponse getMethod() throws ConnectorException, IOException {
        return client.setTimeout(CLIENT_REQUEST_TIMEOUT).get();
    }

    public CoapObserveRelation getObserveRelation(TestCoapClientCallback callback){
        Request request = Request.newGet().setObserve();
        request.setType(CoAP.Type.CON);
        return client.observe(request, callback);
    }

    public void setURI(String featureTokenUrl) {
        if (client == null) {
            throw new RuntimeException("Failed to connect! CoapClient is not initialized!");
        }
        client.setURI(featureTokenUrl);
    }

    public void setURI(String accessToken, FeatureType featureType) {
        if (featureType == null){
            featureType = FeatureType.ATTRIBUTES;
        }
        setURI(getFeatureTokenUrl(accessToken, featureType));
    }

    private CoapClient createClient() {
        return new CoapClient();
    }

    private CoapClient createClient(String featureTokenUrl) {
        return new CoapClient(featureTokenUrl);
    }

    public static String getFeatureTokenUrl(FeatureType featureType) {
        return COAP_BASE_URL + featureType.name().toLowerCase();
    }

    public static String getFeatureTokenUrl(String token, FeatureType featureType) {
        return COAP_BASE_URL + token + "/" + featureType.name().toLowerCase();
    }

    public static String getFeatureTokenUrl(String token, FeatureType featureType, int requestId) {
        return COAP_BASE_URL + token + "/" + featureType.name().toLowerCase() + "/" + requestId;
    }
}
