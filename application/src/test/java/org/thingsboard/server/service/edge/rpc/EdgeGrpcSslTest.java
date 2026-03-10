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
package org.thingsboard.server.service.edge.rpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.config.ssl.PemSslCredentials;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Tests for Edge gRPC SSL setup using PemSslCredentials.
 * Covers all test plan scenarios:
 * 1. Separate cert and key files (existing behavior)
 * 2. Combined PEM file (cert + key)
 * 3. Encrypted private key + key_password
 * 4. Error when combined PEM has no private key and private_key is empty
 */
class EdgeGrpcSslTest {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final List<Path> tempFiles = new ArrayList<>();
    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void cleanup() throws Exception {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
        }
        for (Path p : tempFiles) {
            Files.deleteIfExists(p);
        }
    }

    @Test
    void separateCertAndKeyFiles() throws Exception {
        KeyPair kp = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp);

        Path certFile = writeTempPem("cert", cert);
        Path keyFile = writeTempPem("key", kp.getPrivate());

        server = startServer(certFile.toString(), keyFile.toString(), null);
        assertTlsConnectivity(cert);
    }

    @Test
    void combinedPemFile() throws Exception {
        KeyPair kp = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp);

        Path combinedFile = writeTempPem("combined", cert, kp.getPrivate());

        server = startServer(combinedFile.toString(), "", null);
        assertTlsConnectivity(cert);
    }

    @Test
    void encryptedPrivateKey() throws Exception {
        KeyPair kp = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp);
        String password = "test-password";

        Path combinedFile = writeTempPemEncrypted("enc-combined", password, cert, kp.getPrivate());

        server = startServer(combinedFile.toString(), "", password);
        assertTlsConnectivity(cert);
    }

    @Test
    void combinedPemWithoutKey_throwsException() throws Exception {
        KeyPair kp = generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp);

        Path certOnlyFile = writeTempPem("cert-only", cert);

        assertThatThrownBy(() -> startServer(certOnlyFile.toString(), "", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Helpers that mirror EdgeGrpcService.setupSsl() ---

    private Server startServer(String certFileResource, String privateKeyResource, String keyPassword) throws Exception {
        PemSslCredentials credentials = new PemSslCredentials();
        credentials.setCertFile(certFileResource);
        credentials.setKeyFile(StringUtils.isEmpty(privateKeyResource) ? null : privateKeyResource);
        credentials.setKeyPassword(keyPassword);
        credentials.init(false);

        SslContext sslContext = GrpcSslContexts.configure(
                SslContextBuilder.forServer(credentials.createKeyManagerFactory())).build();

        return NettyServerBuilder.forPort(0)
                .sslContext(sslContext)
                .addService(new EdgeRpcServiceGrpc.EdgeRpcServiceImplBase() {})
                .build()
                .start();
    }

    private void assertTlsConnectivity(X509Certificate trustedCert) throws Exception {
        String certPem = toPem(trustedCert);
        SslContext clientSsl = GrpcSslContexts.forClient()
                .trustManager(new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)))
                .build();

        channel = NettyChannelBuilder.forAddress("localhost", server.getPort())
                .sslContext(clientSsl)
                .build();

        channel.getState(true); // trigger connection attempt
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var state = channel.getState(false);
                    if (state == io.grpc.ConnectivityState.TRANSIENT_FAILURE) {
                        throw new AssertionError("TLS handshake failed: channel in TRANSIENT_FAILURE");
                    }
                    assertThat(state).isEqualTo(io.grpc.ConnectivityState.READY);
                });
    }

    // --- Cert/key generation ---

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    private X509Certificate generateSelfSignedCert(KeyPair kp) throws Exception {
        X500Name subject = new X500Name("CN=localhost");
        Date now = new Date();
        return new JcaX509CertificateConverter().getCertificate(
                new JcaX509v3CertificateBuilder(
                        subject, BigInteger.ONE, now,
                        new Date(now.getTime() + TimeUnit.DAYS.toMillis(1)),
                        subject, kp.getPublic())
                        .build(new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate())));
    }

    // --- PEM file helpers ---

    private String toPem(Object obj) throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        try (JcaPEMWriter w = new JcaPEMWriter(sw)) {
            w.writeObject(obj);
        }
        return sw.toString();
    }

    private Path writeTempPem(String prefix, Object... objects) throws Exception {
        Path p = Files.createTempFile(prefix + "-", ".pem");
        tempFiles.add(p);
        try (JcaPEMWriter w = new JcaPEMWriter(Files.newBufferedWriter(p))) {
            for (Object o : objects) {
                w.writeObject(o);
            }
        }
        return p;
    }

    private Path writeTempPemEncrypted(String prefix, String password, Object... objects) throws Exception {
        Path p = Files.createTempFile(prefix + "-", ".pem");
        tempFiles.add(p);
        var encryptor = new JcePEMEncryptorBuilder("AES-256-CBC")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(password.toCharArray());
        try (JcaPEMWriter w = new JcaPEMWriter(Files.newBufferedWriter(p))) {
            for (Object o : objects) {
                if (o instanceof PrivateKey) {
                    w.writeObject(o, encryptor);
                } else {
                    w.writeObject(o);
                }
            }
        }
        return p;
    }
}
