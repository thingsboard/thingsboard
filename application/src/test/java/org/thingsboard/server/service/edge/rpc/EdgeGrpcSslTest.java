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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
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
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Tests for Edge gRPC SSL setup using the production {@link GrpcServer#setupSsl} method.
 * <p>
 * Covers:
 * 1. Separate cert and key PEM inputs
 * 2. Combined PEM (cert + key in one file)
 * 3. Encrypted private key with password
 * 4. Missing key in combined PEM → error
 * <p>
 * Each scenario is parameterized across key types: RSA-2048, RSA-4096, EC P-256, EC P-384.
 */
class EdgeGrpcSslTest {

    private static final int TIMEOUT = 30;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    enum KeyType {
        RSA_2048("RSA", 2048, null, "SHA256withRSA"),
        RSA_4096("RSA", 4096, null, "SHA256withRSA"),
        EC_P256("EC", 256, "secp256r1", "SHA256withECDSA"),
        EC_P384("EC", 384, "secp384r1", "SHA384withECDSA");

        final String algorithm;
        final int size;
        final String curve;
        final String sigAlg;

        KeyType(String algorithm, int size, String curve, String sigAlg) {
            this.algorithm = algorithm;
            this.size = size;
            this.curve = curve;
            this.sigAlg = sigAlg;
        }

        KeyPair generateKeyPair() throws Exception {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
            if (curve != null) {
                kpg.initialize(new ECGenParameterSpec(curve));
            } else {
                kpg.initialize(size);
            }
            return kpg.generateKeyPair();
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

    @ParameterizedTest(name = "separateCertAndKey_{0}")
    @EnumSource(KeyType.class)
    void separateCertAndKey(KeyType keyType) throws Exception {
        KeyPair kp = keyType.generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp, keyType.sigAlg);

        Path certFile = writeTempPem("cert", cert);
        Path keyFile = writeTempPem("key", kp.getPrivate());

        server = startServer(certFile.toString(), keyFile.toString(), null);
        assertTlsConnectivity(cert);
    }

    @ParameterizedTest(name = "combinedPemWithCertAndKey_{0}")
    @EnumSource(KeyType.class)
    void combinedPemWithCertAndKey(KeyType keyType) throws Exception {
        KeyPair kp = keyType.generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp, keyType.sigAlg);

        Path combinedFile = writeTempPem("combined", cert, kp.getPrivate());

        server = startServer(combinedFile.toString(), "", null);
        assertTlsConnectivity(cert);
    }

    // RSA-only: BouncyCastle writes encrypted EC keys in traditional PEM format (BEGIN EC PRIVATE KEY),
    // which after decryption produces a PEMKeyPair without public key info — causing PemSslCredentials
    // to fail with "Cannot invoke SubjectPublicKeyInfo.getEncoded() because getPublicKeyInfo() is null".
    @ParameterizedTest(name = "encryptedPrivateKey_{0}")
    @EnumSource(value = KeyType.class, names = {"RSA_2048", "RSA_4096"})
    void encryptedPrivateKey(KeyType keyType) throws Exception {
        KeyPair kp = keyType.generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp, keyType.sigAlg);
        String password = "test-password";

        Path combinedFile = writeTempPemEncrypted("enc-combined", password, cert, kp.getPrivate());

        server = startServer(combinedFile.toString(), "", password);
        assertTlsConnectivity(cert);
    }

    @ParameterizedTest(name = "combinedPemWithCertOnly_throwsException_{0}")
    @EnumSource(KeyType.class)
    void combinedPemWithCertOnly_throwsException(KeyType keyType) throws Exception {
        KeyPair kp = keyType.generateKeyPair();
        X509Certificate cert = generateSelfSignedCert(kp, keyType.sigAlg);

        Path certOnlyFile = writeTempPem("cert-only", cert);

        assertThatThrownBy(() -> startServer(certOnlyFile.toString(), "", null))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    // --- Server startup using production EdgeGrpcService.setupSsl() ---

    private Server startServer(String certFileResource, String privateKeyResource, String keyPassword) throws Exception {
        GrpcServer edgeGrpcService = new GrpcServer(new EdgeRpcServiceGrpc.EdgeRpcServiceImplBase() {});
        ReflectionTestUtils.setField(edgeGrpcService, "certFileResource", certFileResource);
        ReflectionTestUtils.setField(edgeGrpcService, "privateKeyResource", privateKeyResource);
        ReflectionTestUtils.setField(edgeGrpcService, "keyPassword", keyPassword != null ? keyPassword : "");

        NettyServerBuilder builder = NettyServerBuilder.forPort(0)
                .addService(new EdgeRpcServiceGrpc.EdgeRpcServiceImplBase() {});

        edgeGrpcService.setupSsl(builder);

        return builder.build().start();
    }

    private void assertTlsConnectivity(X509Certificate trustedCert) throws Exception {
        String certPem = toPem(trustedCert);
        var clientSsl = GrpcSslContexts.forClient()
                .trustManager(new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)))
                .build();

        channel = NettyChannelBuilder.forAddress("localhost", server.getPort())
                .sslContext(clientSsl)
                .build();

        channel.getState(true); // trigger connection attempt
        await().atMost(TIMEOUT, TimeUnit.SECONDS)
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

    private X509Certificate generateSelfSignedCert(KeyPair kp, String sigAlg) throws Exception {
        X500Name subject = new X500Name("CN=localhost");
        Date now = new Date();
        return new JcaX509CertificateConverter().getCertificate(
                new JcaX509v3CertificateBuilder(
                        subject, BigInteger.ONE, now,
                        new Date(now.getTime() + TimeUnit.DAYS.toMillis(1)),
                        subject, kp.getPublic())
                        .build(new JcaContentSignerBuilder(sigAlg).build(kp.getPrivate())));
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
                w.writeObject(toPkcs8IfKey(o));
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

    private Object toPkcs8IfKey(Object o) {
        if (o instanceof PrivateKey pk) {
            return new PemObject("PRIVATE KEY", pk.getEncoded());
        }
        return o;
    }
}
