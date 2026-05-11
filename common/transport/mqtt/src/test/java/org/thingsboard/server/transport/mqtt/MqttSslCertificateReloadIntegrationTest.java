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
package org.thingsboard.server.transport.mqtt;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.PemSslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class MqttSslCertificateReloadIntegrationTest {

    @TempDir
    Path tempDir;

    @Mock
    private TransportService transportService;

    @Test
    public void givenMqttSslProvider_whenCertFileChangedAndReloadTriggered_thenNewConnectionSeesNewCert() throws Exception {
        KeyPair keyPairA = generateKeyPair();
        X509Certificate certA = generateSelfSignedCert(keyPairA, "CN=CertA");

        KeyPair keyPairB = generateKeyPair();
        X509Certificate certB = generateSelfSignedCert(keyPairB, "CN=CertB");

        Path certFile = tempDir.resolve("server-cert.pem");
        Path keyFile = tempDir.resolve("server-key.pem");
        writeCertPem(certFile, certA);
        writeKeyPem(keyFile, keyPairA);

        SslCredentialsConfig credentialsConfig = createSslCredentialsConfig(certFile, keyFile);
        MqttSslHandlerProvider provider = createMqttSslHandlerProvider(credentialsConfig);

        SSLContext ctxA = getProviderSslContext(provider);
        X509Certificate servedA;
        try (SSLServerSocket ss = createServerSocket(ctxA)) {
            servedA = doHandshakeAndGetServerCert(ss);
        }
        assertThat(servedA.getSubjectX500Principal()).isEqualTo(certA.getSubjectX500Principal());

        writeCertPem(certFile, certB);
        writeKeyPem(keyFile, keyPairB);

        credentialsConfig.onCertificateFileChanged();

        SSLContext ctxB = getProviderSslContext(provider);
        assertThat(ctxB).isNotSameAs(ctxA);
        X509Certificate servedB;
        try (SSLServerSocket ss = createServerSocket(ctxB)) {
            servedB = doHandshakeAndGetServerCert(ss);
        }
        assertThat(servedB.getSubjectX500Principal()).isEqualTo(certB.getSubjectX500Principal());
        assertThat(servedB.getSubjectX500Principal()).isNotEqualTo(servedA.getSubjectX500Principal());
    }

    @Test
    public void givenMqttSslProvider_whenReloadCalledWithSameFiles_thenSslContextIsRecreated() throws Exception {
        KeyPair keyPair = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(keyPair, "CN=SameCert");

        Path certFile = tempDir.resolve("server-cert.pem");
        Path keyFile = tempDir.resolve("server-key.pem");
        writeCertPem(certFile, cert);
        writeKeyPem(keyFile, keyPair);

        SslCredentialsConfig credentialsConfig = createSslCredentialsConfig(certFile, keyFile);
        MqttSslHandlerProvider provider = createMqttSslHandlerProvider(credentialsConfig);

        SSLContext ctx1 = getProviderSslContext(provider);
        assertThat(ctx1).isNotNull();

        credentialsConfig.onCertificateFileChanged();

        SSLContext ctx2 = getProviderSslContext(provider);
        assertThat(ctx2).isNotSameAs(ctx1);

        X509Certificate served;
        try (SSLServerSocket ss = createServerSocket(ctx2)) {
            served = doHandshakeAndGetServerCert(ss);
        }
        assertThat(served.getSubjectX500Principal()).isEqualTo(cert.getSubjectX500Principal());
    }

    @Test
    public void givenMqttSslProvider_whenMultipleReloads_thenEachProducesNewContext() throws Exception {
        KeyPair keyPairA = generateKeyPair();
        X509Certificate certA = generateSelfSignedCert(keyPairA, "CN=CertA");
        KeyPair keyPairB = generateKeyPair();
        X509Certificate certB = generateSelfSignedCert(keyPairB, "CN=CertB");
        KeyPair keyPairC = generateKeyPair();
        X509Certificate certC = generateSelfSignedCert(keyPairC, "CN=CertC");

        Path certFile = tempDir.resolve("server-cert.pem");
        Path keyFile = tempDir.resolve("server-key.pem");
        writeCertPem(certFile, certA);
        writeKeyPem(keyFile, keyPairA);

        SslCredentialsConfig credentialsConfig = createSslCredentialsConfig(certFile, keyFile);
        MqttSslHandlerProvider provider = createMqttSslHandlerProvider(credentialsConfig);

        SSLContext ctx1 = getProviderSslContext(provider);

        writeCertPem(certFile, certB);
        writeKeyPem(keyFile, keyPairB);
        credentialsConfig.onCertificateFileChanged();
        SSLContext ctx2 = getProviderSslContext(provider);

        writeCertPem(certFile, certC);
        writeKeyPem(keyFile, keyPairC);
        credentialsConfig.onCertificateFileChanged();
        SSLContext ctx3 = getProviderSslContext(provider);

        assertThat(ctx1).isNotSameAs(ctx2);
        assertThat(ctx2).isNotSameAs(ctx3);

        X509Certificate served;
        try (SSLServerSocket ss = createServerSocket(ctx3)) {
            served = doHandshakeAndGetServerCert(ss);
        }
        assertThat(served.getSubjectX500Principal()).isEqualTo(certC.getSubjectX500Principal());
    }

    private SslCredentialsConfig createSslCredentialsConfig(Path certFile, Path keyFile) throws Exception {
        PemSslCredentials pem = new PemSslCredentials();
        pem.setCertFile(certFile.toAbsolutePath().toString());
        pem.setKeyFile(keyFile.toAbsolutePath().toString());

        SslCredentialsConfig config = new SslCredentialsConfig("MQTT SSL Test", false);
        config.setEnabled(true);
        config.setType(org.thingsboard.server.common.transport.config.ssl.SslCredentialsType.PEM);
        config.setPem(pem);
        config.setKeystore(new org.thingsboard.server.common.transport.config.ssl.KeystoreSslCredentials());
        config.init();
        return config;
    }

    private MqttSslHandlerProvider createMqttSslHandlerProvider(SslCredentialsConfig credentialsConfig) {
        MqttSslHandlerProvider provider = new MqttSslHandlerProvider();
        ReflectionTestUtils.setField(provider, "sslProtocol", "TLSv1.2");
        ReflectionTestUtils.setField(provider, "mqttSslCredentialsConfig", credentialsConfig);
        ReflectionTestUtils.setField(provider, "transportService", transportService);
        provider.afterSingletonsInstantiated();
        return provider;
    }

    /**
     * Triggers SSLContext creation through the provider's getSslHandler() path,
     * then extracts the cached SSLContext for direct server socket use.
     */
    private SSLContext getProviderSslContext(MqttSslHandlerProvider provider) {
        provider.getSslHandler();
        return (SSLContext) ReflectionTestUtils.getField(provider, "sslContext");
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
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
                        .build(new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate())));
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

    private SSLServerSocket createServerSocket(SSLContext ctx) throws Exception {
        return (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(0, 1, InetAddress.getLoopbackAddress());
    }

    private X509Certificate doHandshakeAndGetServerCert(SSLServerSocket serverSocket) throws Exception {
        Thread acceptor = new Thread(() -> {
            try (var conn = serverSocket.accept()) {
                conn.getInputStream().read();
            } catch (Exception ignored) {}
        });
        acceptor.setDaemon(true);
        acceptor.start();

        SSLContext clientCtx = SSLContext.getInstance("TLSv1.2");
        clientCtx.init(null, new TrustManager[]{new TrustAllManager()}, null);

        try (SSLSocket client = (SSLSocket) clientCtx.getSocketFactory()
                .createSocket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort())) {
            client.setSoTimeout(5000);
            client.startHandshake();

            Certificate[] peerCerts = client.getSession().getPeerCertificates();
            assertThat(peerCerts).isNotEmpty();
            return (X509Certificate) peerCerts[0];
        } finally {
            acceptor.join(5000);
        }
    }

    private static class TrustAllManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

    }

}
