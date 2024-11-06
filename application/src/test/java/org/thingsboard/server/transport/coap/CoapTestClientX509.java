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
package org.thingsboard.server.transport.coap;

import lombok.Getter;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.scandium.DTLSConnector;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.io.IOException;

public class CoapTestClientX509 {

    private static final String COAPS_BASE_URL = "coaps://localhost:5684/api/v1/";
    private static final long CLIENT_REQUEST_TIMEOUT = 60000L;

    private final CoapClient clientX509;
    private final DTLSConnector dtlsConnector;

    @Getter
    private CoAP.Type type = CoAP.Type.CON;

    public CoapTestClientX509(DTLSConnector dtlsConnector) {
        this.dtlsConnector = dtlsConnector;
        this.clientX509 = createClient();
    }

    public CoapTestClientX509(DTLSConnector dtlsConnector, String accessToken, FeatureType featureType) {
        this.dtlsConnector = dtlsConnector;
        this.clientX509 = createClient(getFeatureTokenUrl(accessToken, featureType));
    }

    public CoapTestClientX509(DTLSConnector dtlsConnector, String featureTokenUrl) {
        this.dtlsConnector = dtlsConnector;
        this.clientX509 = createClient(featureTokenUrl);

    }

    public void connectToCoap(String accessToken) {
        setURI(accessToken, null);
    }

    public void connectToCoap(String accessToken, FeatureType featureType) {
        setURI(accessToken, featureType);
    }

    public void disconnect() {
        if (clientX509 != null) {
            clientX509.shutdown();
        }
    }

    public CoapResponse postMethod(String requestBody) throws ConnectorException, IOException {
        return this.postMethod(requestBody.getBytes());
    }

    public CoapResponse postMethod(byte[] requestBodyBytes) throws ConnectorException, IOException {
        return clientX509.setTimeout(CLIENT_REQUEST_TIMEOUT).post(requestBodyBytes, MediaTypeRegistry.APPLICATION_JSON);
    }

    public void postMethod(CoapHandler handler, String payload, int format) {
        clientX509.post(handler, payload, format);
    }

    public void postMethod(CoapHandler handler, byte[] payload, int format) {
        clientX509.post(handler, payload, format);
    }

    public CoapResponse getMethod() throws ConnectorException, IOException {
        return clientX509.setTimeout(CLIENT_REQUEST_TIMEOUT).get();
    }

    public CoapObserveRelation getObserveRelation(CoapTestCallback callback) {
        return getObserveRelation(callback, true);
    }

    public CoapObserveRelation getObserveRelation(CoapTestCallback callback, boolean confirmable) {
        Request request = Request.newGet().setObserve();
        request.setType(confirmable ? CoAP.Type.CON : CoAP.Type.NON);
        return clientX509.observe(request, callback);
    }

    public void setURI(String featureTokenUrl) {
        if (clientX509 == null) {
            throw new RuntimeException("Failed to connect! CoapClient is not initialized!");
        }
        clientX509.setURI(featureTokenUrl);
    }

    public void setURI(String accessToken, FeatureType featureType) {
        if (featureType == null) {
            featureType = FeatureType.ATTRIBUTES;
        }
        setURI(getFeatureTokenUrl(accessToken, featureType));
    }

    public void useCONs() {
        if (clientX509 == null) {
            throw new RuntimeException("Failed to connect! CoapClient is not initialized!");
        }
        type = CoAP.Type.CON;
        clientX509.useCONs();
    }

    public void useNONs() {
        if (clientX509 == null) {
            throw new RuntimeException("Failed to connect! CoapClient is not initialized!");
        }
        type = CoAP.Type.NON;
        clientX509.useNONs();
    }

    private CoapClient createClient() {
        return new CoapClient();
    }

    private CoapClient createClient(String featureTokenUrl) {
        CoapClient client = new CoapClient(featureTokenUrl);
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(dtlsConnector);
        client.setEndpoint(builder.build());
        return client;
    }

    public static String getFeatureTokenUrl(String token, FeatureType featureType) {
        return COAPS_BASE_URL + token + "/" + featureType.name().toLowerCase();
    }

    public static String getFeatureTokenUrl(String token, FeatureType featureType, int requestId) {
        return COAPS_BASE_URL + token + "/" + featureType.name().toLowerCase() + "/" + requestId;
    }
}
