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
package org.thingsboard.server.transport.coap.x509;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.x509.CertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.CoapTestCallback;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.elements.config.CertificateAuthenticationMode.WANTED;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CIPHER_SUITES;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.CLIENT_ONLY;

@Slf4j
public class CoapClientX509Test {

    private static final String COAPS_BASE_URL = "coaps://localhost:5684/api/v1/";
    private static final long CLIENT_REQUEST_TIMEOUT = 60000L;

    private final CoapClient clientX509;
    private final DTLSConnector dtlsConnector;
    private final CertPrivateKey certPrivateKey;

    @Getter
    private CoAP.Type type = CoAP.Type.CON;

    public CoapClientX509Test(CertPrivateKey certPrivateKey) {
        this.certPrivateKey = certPrivateKey;
        this.dtlsConnector = createDTLSConnector();
        this.clientX509 = createClient(getFeatureTokenUrl(FeatureType.ATTRIBUTES));
    }

    public CoapClientX509Test(CertPrivateKey certPrivateKey, FeatureType featureType) {
        this.certPrivateKey = certPrivateKey;
        this.dtlsConnector = createDTLSConnector();
        this.clientX509 = createClient(getFeatureTokenUrl(featureType));
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

    private DTLSConnector createDTLSConnector() {
        try {
            // Create DTLS config client
            DtlsConnectorConfig.Builder configBuilder = new DtlsConnectorConfig.Builder(new Configuration());
            configBuilder.set(DTLS_CLIENT_AUTHENTICATION_MODE, WANTED);
            configBuilder.set(DTLS_RETRANSMISSION_TIMEOUT, 60000, MILLISECONDS);
            configBuilder.set(DTLS_ROLE, CLIENT_ONLY);
            configBuilder.set(DTLS_CIPHER_SUITES, Arrays.asList(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256));
            configBuilder.setAdvancedCertificateVerifier(new TbAdvancedCertificateVerifier());
            X509Certificate[] certificateChainClient = new X509Certificate[]{this.certPrivateKey.getCert()};
            CertificateProvider certificateProvider = new SingleCertificateProvider(this.certPrivateKey.getPrivateKey(), certificateChainClient, Collections.singletonList(CertificateType.X_509));
            configBuilder.setCertificateIdentityProvider(certificateProvider);
            return new DTLSConnector(configBuilder.build());
        } catch (Exception e) {
            throw new RuntimeException("", e);
        }
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

    public static String getFeatureTokenUrl(FeatureType featureType) {
        return COAPS_BASE_URL + featureType.name().toLowerCase();
    }

    public static String getFeatureTokenUrl(String featureType) {
        return COAPS_BASE_URL + featureType.toLowerCase();
    }

    public static String getFeatureTokenUrl(String token, FeatureType featureType) {
        return COAPS_BASE_URL + token + "/" + featureType.name().toLowerCase();
    }

    public static String getFeatureTokenUrl(String token, FeatureType featureType, int requestId) {
        return COAPS_BASE_URL + token + "/" + featureType.name().toLowerCase() + "/" + requestId;
    }
}
