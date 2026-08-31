/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.coapserver;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.thingsboard.server.common.transport.config.ssl.KeystoreSslCredentials;
import org.thingsboard.server.common.transport.config.ssl.PemSslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsType;

import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.SERVER_ONLY;

public class CoapDtlsCertificateReloadIntegrationTest {

    private static final String TEST_RESOURCE_PATH = "test";
    private static final String TEST_PAYLOAD = "hello-dtls";

    @TempDir
    Path tempDir;

    private CoapServer coapServer;

    @AfterEach
    public void teardown() {
        if (coapServer != null) {
            coapServer.destroy();
        }
    }

    @Test
    public void givenDtlsServer_whenCertFileChangedAndReloadTriggered_thenNewEndpointServesNewCert() throws Exception {
        KeyPair keyPairA = generateKeyPair();
        X509Certificate certA = generateSelfSignedCert(keyPairA, "CN=ServerA");
        KeyPair keyPairB = generateKeyPair();
        X509Certificate certB = generateSelfSignedCert(keyPairB, "CN=ServerB");

        Path certFile = tempDir.resolve("server-cert.pem");
        Path keyFile = tempDir.resolve("server-key.pem");
        writeCertPem(certFile, certA);
        writeKeyPem(keyFile, keyPairA);

        SslCredentialsConfig credentialsConfig = createSslCredentialsConfig(certFile, keyFile);

        Configuration config = createServerConfig();
        coapServer = new CoapServer(config);
        coapServer.add(new TestResource());

        int dtlsPort = findAvailablePort();
        CoapEndpoint endpointA = buildDtlsEndpointFromCredentials(config, credentialsConfig.getCredentials(), dtlsPort);
        coapServer.addEndpoint(endpointA);
        coapServer.start();

        CoapResponse responseA = doDtlsRequest(dtlsPort, certA);
        assertThat(responseA).isNotNull();
        assertThat(responseA.getCode()).isEqualTo(CoAP.ResponseCode.CONTENT);
        assertThat(responseA.getResponseText()).isEqualTo(TEST_PAYLOAD);

        writeCertPem(certFile, certB);
        writeKeyPem(keyFile, keyPairB);
        credentialsConfig.onCertificateFileChanged();

        coapServer.getEndpoints().remove(endpointA);
        endpointA.stop();

        CoapEndpoint endpointB = buildDtlsEndpointFromCredentials(config, credentialsConfig.getCredentials(), dtlsPort);
        coapServer.addEndpoint(endpointB);
        endpointB.start();
        endpointA.destroy();

        CoapResponse responseB = doDtlsRequest(dtlsPort, certB);
        assertThat(responseB).isNotNull();
        assertThat(responseB.getCode()).isEqualTo(CoAP.ResponseCode.CONTENT);
        assertThat(responseB.getResponseText()).isEqualTo(TEST_PAYLOAD);
    }

    @Test
    public void givenDtlsServer_whenCertReloaded_thenOldCertClientFails() throws Exception {
        KeyPair keyPairA = generateKeyPair();
        X509Certificate certA = generateSelfSignedCert(keyPairA, "CN=ServerA");
        KeyPair keyPairB = generateKeyPair();
        X509Certificate certB = generateSelfSignedCert(keyPairB, "CN=ServerB");

        Path certFile = tempDir.resolve("server-cert.pem");
        Path keyFile = tempDir.resolve("server-key.pem");
        writeCertPem(certFile, certA);
        writeKeyPem(keyFile, keyPairA);

        SslCredentialsConfig credentialsConfig = createSslCredentialsConfig(certFile, keyFile);

        Configuration config = createServerConfig();
        coapServer = new CoapServer(config);
        coapServer.add(new TestResource());

        int dtlsPort = findAvailablePort();
        CoapEndpoint endpointA = buildDtlsEndpointFromCredentials(config, credentialsConfig.getCredentials(), dtlsPort);
        coapServer.addEndpoint(endpointA);
        coapServer.start();

        CoapResponse responseA = doDtlsRequest(dtlsPort, certA);
        assertThat(responseA).isNotNull();

        writeCertPem(certFile, certB);
        writeKeyPem(keyFile, keyPairB);
        credentialsConfig.onCertificateFileChanged();

        coapServer.getEndpoints().remove(endpointA);
        endpointA.stop();
        CoapEndpoint endpointB = buildDtlsEndpointFromCredentials(config, credentialsConfig.getCredentials(), dtlsPort);
        coapServer.addEndpoint(endpointB);
        endpointB.start();
        endpointA.destroy();

        CoapResponse failedResponse = doDtlsRequest(dtlsPort, certA);
        assertThat(failedResponse).isNull();

        CoapResponse responseB = doDtlsRequest(dtlsPort, certB);
        assertThat(responseB).isNotNull();
        assertThat(responseB.getCode()).isEqualTo(CoAP.ResponseCode.CONTENT);
    }

    @Test
    public void givenDtlsServer_whenReloadWithSameCert_thenConnectionStillWorks() throws Exception {
        KeyPair keyPair = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(keyPair, "CN=Server");

        Path certFile = tempDir.resolve("server-cert.pem");
        Path keyFile = tempDir.resolve("server-key.pem");
        writeCertPem(certFile, cert);
        writeKeyPem(keyFile, keyPair);

        SslCredentialsConfig credentialsConfig = createSslCredentialsConfig(certFile, keyFile);

        Configuration config = createServerConfig();
        coapServer = new CoapServer(config);
        coapServer.add(new TestResource());

        int dtlsPort = findAvailablePort();
        CoapEndpoint endpoint1 = buildDtlsEndpointFromCredentials(config, credentialsConfig.getCredentials(), dtlsPort);
        coapServer.addEndpoint(endpoint1);
        coapServer.start();

        CoapResponse response1 = doDtlsRequest(dtlsPort, cert);
        assertThat(response1).isNotNull();
        assertThat(response1.getCode()).isEqualTo(CoAP.ResponseCode.CONTENT);

        credentialsConfig.onCertificateFileChanged();

        coapServer.getEndpoints().remove(endpoint1);
        endpoint1.stop();
        CoapEndpoint endpoint2 = buildDtlsEndpointFromCredentials(config, credentialsConfig.getCredentials(), dtlsPort);
        coapServer.addEndpoint(endpoint2);
        endpoint2.start();
        endpoint1.destroy();

        CoapResponse response2 = doDtlsRequest(dtlsPort, cert);
        assertThat(response2).isNotNull();
        assertThat(response2.getCode()).isEqualTo(CoAP.ResponseCode.CONTENT);
    }

    private SslCredentialsConfig createSslCredentialsConfig(Path certFile, Path keyFile) {
        PemSslCredentials pem = new PemSslCredentials();
        pem.setCertFile(certFile.toAbsolutePath().toString());
        pem.setKeyFile(keyFile.toAbsolutePath().toString());

        SslCredentialsConfig config = new SslCredentialsConfig("CoAP DTLS Test", false);
        config.setEnabled(true);
        config.setType(SslCredentialsType.PEM);
        config.setPem(pem);
        config.setKeystore(new KeystoreSslCredentials());
        config.init();
        return config;
    }

    private CoapEndpoint buildDtlsEndpointFromCredentials(Configuration config, SslCredentials credentials, int port) {
        DtlsConnectorConfig.Builder dtlsBuilder = new DtlsConnectorConfig.Builder(config);
        dtlsBuilder.setAddress(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        dtlsBuilder.set(DTLS_ROLE, SERVER_ONLY);
        dtlsBuilder.set(DTLS_RETRANSMISSION_TIMEOUT, 3000, MILLISECONDS);
        dtlsBuilder.set(DTLS_CLIENT_AUTHENTICATION_MODE,
                org.eclipse.californium.elements.config.CertificateAuthenticationMode.WANTED);

        SslContextUtil.Credentials serverCreds = new SslContextUtil.Credentials(
                credentials.getPrivateKey(), null, credentials.getCertificateChain());

        dtlsBuilder.setCertificateIdentityProvider(
                new SingleCertificateProvider(serverCreds.getPrivateKey(), serverCreds.getCertificateChain(),
                        Collections.singletonList(CertificateType.X_509)));

        dtlsBuilder.setAdvancedCertificateVerifier(
                StaticNewAdvancedCertificateVerifier.builder()
                        .setTrustAllCertificates()
                        .build());

        DTLSConnector connector = new DTLSConnector(dtlsBuilder.build());

        CoapEndpoint.Builder endpointBuilder = new CoapEndpoint.Builder();
        endpointBuilder.setConfiguration(config);
        endpointBuilder.setConnector(connector);
        return endpointBuilder.build();
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        return kpg.generateKeyPair();
    }

    private X509Certificate generateSelfSignedCert(KeyPair kp, String subjectDn) throws Exception {
        X500Name subject = new X500Name(subjectDn);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + TimeUnit.DAYS.toMillis(1));
        return new JcaX509CertificateConverter().getCertificate(
                new JcaX509v3CertificateBuilder(
                        subject, BigInteger.valueOf(System.nanoTime()), now, expiry,
                        subject, kp.getPublic())
                        .build(new JcaContentSignerBuilder("SHA256withECDSA").build(kp.getPrivate())));
    }

    private void writeCertPem(Path path, X509Certificate cert) throws Exception {
        try (PemWriter writer = new PemWriter(new OutputStreamWriter(Files.newOutputStream(path)))) {
            writer.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
        }
    }

    private void writeKeyPem(Path path, KeyPair keyPair) throws Exception {
        try (PemWriter writer = new PemWriter(new OutputStreamWriter(Files.newOutputStream(path)))) {
            writer.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        }
    }

    private Configuration createServerConfig() {
        Configuration config = new Configuration();
        config.set(CoapConfig.MAX_RETRANSMIT, 2);
        config.set(CoapConfig.RESPONSE_MATCHING, CoapConfig.MatcherMode.RELAXED);
        return config;
    }

    private CoapResponse doDtlsRequest(int port, X509Certificate trustedCert) {
        try {
            Configuration clientConfig = new Configuration();
            clientConfig.set(CoapConfig.MAX_RETRANSMIT, 1);
            clientConfig.set(DtlsConfig.DTLS_ROLE, DtlsConfig.DtlsRole.CLIENT_ONLY);
            clientConfig.set(DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT, 2000, MILLISECONDS);
            clientConfig.set(DtlsConfig.DTLS_USE_HELLO_VERIFY_REQUEST, false);
            clientConfig.set(DtlsConfig.DTLS_VERIFY_SERVER_CERTIFICATES_SUBJECT, false);

            DtlsConnectorConfig.Builder clientDtls = new DtlsConnectorConfig.Builder(clientConfig);
            clientDtls.setAdvancedCertificateVerifier(
                    StaticNewAdvancedCertificateVerifier.builder()
                            .setTrustedCertificates(trustedCert)
                            .build());

            DTLSConnector clientConnector = new DTLSConnector(clientDtls.build());
            CoapEndpoint clientEndpoint = new CoapEndpoint.Builder()
                    .setConfiguration(clientConfig)
                    .setConnector(clientConnector)
                    .build();

            CoapClient client = new CoapClient("coaps://127.0.0.1:" + port + "/" + TEST_RESOURCE_PATH);
            client.setEndpoint(clientEndpoint);
            client.setTimeout((long) 5000);

            try {
                clientEndpoint.start();
                return client.get();
            } finally {
                client.shutdown();
                clientEndpoint.destroy();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private int findAvailablePort() throws Exception {
        try (java.net.DatagramSocket socket = new java.net.DatagramSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static class TestResource extends CoapResource {
        TestResource() {
            super(TEST_RESOURCE_PATH);
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            exchange.respond(CoAP.ResponseCode.CONTENT, TEST_PAYLOAD);
        }

    }

}
