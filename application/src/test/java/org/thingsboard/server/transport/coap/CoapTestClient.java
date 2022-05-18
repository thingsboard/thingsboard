package org.thingsboard.server.transport.coap;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.io.IOException;

public class CoapTestClient {

    private static final String COAP_BASE_URL = "coap://localhost:5683/api/v1/";
    private static final long CLIENT_REQUEST_TIMEOUT = 60000L;

    private final CoapClient client;

    public CoapTestClient(){
        this.client = createClient();
    }

    public CoapTestClient(String accessToken, FeatureType featureType) {
        this.client = createClient(getFeatureTokenUrl(accessToken, featureType));
    }

    public CoapTestClient(String featureTokenUrl) {
        this.client = createClient(featureTokenUrl);
    }

    public void connectToCoap(String accessToken, FeatureType featureType) {
        connect(accessToken, featureType);
    }

    public void connectToCoap(String accessToken) {
        connect(accessToken, null);
    }

    public void disconnect() {
        if (client != null) {
            client.shutdown();
        };
    }

    public CoapResponse PostMethod(String requestBody) throws ConnectorException, IOException {
        return client.setTimeout(CLIENT_REQUEST_TIMEOUT).post(requestBody.getBytes(), MediaTypeRegistry.APPLICATION_JSON);
    }

    public CoapResponse GetMethod() throws ConnectorException, IOException {
        return client.setTimeout(CLIENT_REQUEST_TIMEOUT).get();
    }

    private void connect(String accessToken, FeatureType featureType) {
        if (client == null) {
            throw new RuntimeException("Failed to connect! CoapClient is not initialized!");
        }
        if (featureType == null){
            featureType = FeatureType.ATTRIBUTES;
        }
        client.setURI(getFeatureTokenUrl(accessToken, featureType));
    }

    private CoapClient createClient() {
        return new CoapClient();
    }

    private CoapClient createClient(String featureTokenUrl) {
        return new CoapClient(featureTokenUrl);
    }

    public static String getFeatureTokenUrl(String token, FeatureType featureType) {
        return COAP_BASE_URL + token + "/" + featureType.name().toLowerCase();
    }
}
