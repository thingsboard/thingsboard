/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap.client;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecureClientX509 {

    private final DTLSConnector dtlsConnector;
    private ExecutorService executor = Executors.newFixedThreadPool(1, ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
    private CoapClient coapClient;

    public SecureClientX509(DTLSConnector dtlsConnector, String host, int port, String clientKeys, String sharedKeys) throws URISyntaxException {
        this.dtlsConnector = dtlsConnector;
        this.coapClient = getCoapClient(host, port, clientKeys, sharedKeys);
    }

    public void test() {
        executor.submit(() -> {
            try {
                while (!Thread.interrupted()) {
                    CoapResponse response = null;
                    try {
                        response = coapClient.get();
                    } catch (ConnectorException | IOException e) {
                        System.err.println("Error occurred while sending request: " + e);
                        System.exit(-1);
                    }
                    if (response != null) {

                        System.out.println(response.getCode() + " - " + response.getCode().name());
                        System.out.println(response.getOptions());
                        System.out.println(response.getResponseText());
                        System.out.println();
                        System.out.println("ADVANCED:");
                        EndpointContext context = response.advanced().getSourceContext();
                        Principal identity = context.getPeerIdentity();
                        if (identity != null) {
                            System.out.println(context.getPeerIdentity());
                        } else {
                            System.out.println("anonymous");
                        }
                        System.out.println(context.get(DtlsEndpointContext.KEY_CIPHER));
                        System.out.println(Utils.prettyPrint(response));
                    } else {
                        System.out.println("No response received.");
                    }
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                System.out.println("Error occurred while sending COAP requests.");
            }
        });
    }

    private CoapClient getCoapClient(String host, Integer port, String clientKeys, String sharedKeys) throws URISyntaxException {
        URI uri = new URI(getFutureUrl(host, port, clientKeys, sharedKeys));
        CoapClient client = new CoapClient(uri);
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(dtlsConnector);

        client.setEndpoint(builder.build());
        return client;
    }

    private String getFutureUrl(String host, Integer port, String clientKeys, String sharedKeys) {
        return "coaps://" + host + ":" + port + "/api/v1/attributes?clientKeys=" + clientKeys + "&sharedKeys=" + sharedKeys;
    }

    public static void main(String[] args) throws URISyntaxException {
        System.out.println("Usage: java -cp ... org.thingsboard.server.transport.coap.client.SecureClientX509 " +
                "host port keyStoreUriPath keyStoreAlias trustedAliasPattern clientKeys sharedKeys");

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String clientKeys = args[6];
        String sharedKeys = args[7];

        String keyStoreUriPath = args[2];
        String keyStoreAlias = args[3];
        String trustedAliasPattern = args[4];
        String keyStorePassword = args[5];


        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new Configuration());
        setupCredentials(builder, keyStoreUriPath, keyStoreAlias, trustedAliasPattern, keyStorePassword);
        DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
        SecureClientX509 client = new SecureClientX509(dtlsConnector, host, port, clientKeys, sharedKeys);
        client.test();
    }

    private static void setupCredentials(DtlsConnectorConfig.Builder config, String keyStoreUriPath, String keyStoreAlias, String trustedAliasPattern, String keyStorePassword) {
        StaticNewAdvancedCertificateVerifier.Builder trustBuilder = StaticNewAdvancedCertificateVerifier.builder();
        try {
            SslContextUtil.Credentials serverCredentials = SslContextUtil.loadCredentials(
                    keyStoreUriPath, keyStoreAlias, keyStorePassword.toCharArray(), keyStorePassword.toCharArray());
            Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(
                    keyStoreUriPath, trustedAliasPattern, keyStorePassword.toCharArray());
            trustBuilder.setTrustedCertificates(trustedCertificates);
            config.setAdvancedCertificateVerifier(trustBuilder.build());
            config.setCertificateIdentityProvider(new SingleCertificateProvider(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(), Collections.singletonList(CertificateType.X_509)));
        } catch (GeneralSecurityException e) {
            System.err.println("certificates are invalid!");
            throw new IllegalArgumentException(e.getMessage());
        } catch (IOException e) {
            System.err.println("certificates are missing!");
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
