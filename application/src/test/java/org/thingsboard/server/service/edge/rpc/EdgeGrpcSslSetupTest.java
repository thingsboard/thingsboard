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

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.TestSocketUtils;
import org.thingsboard.server.gen.edge.v1.EdgeRpcServiceGrpc;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
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

class EdgeGrpcSslSetupTest {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final List<Path> tempFiles = new ArrayList<>();

    @AfterEach
    void cleanup() throws Exception {
        for (Path p : tempFiles) {
            Files.deleteIfExists(p);
        }
    }

    @Test
    void serverStartsAndClientConnects_combinedPem() throws Exception {
        CertKey ck = generateSelfSignedCert();
        Path combined = tempPem("edge-combined");
        writePem(combined, ck.cert, ck.key);

        assertTlsHandshake(combined.toString(), null, ck.cert);
    }

    @Test
    void serverStartsAndClientConnects_separatePem() throws Exception {
        CertKey ck = generateSelfSignedCert();
        Path certFile = tempPem("edge-cert");
        Path keyFile = tempPem("edge-key");
        writePem(certFile, ck.cert);
        writePem(keyFile, ck.key);

        assertTlsHandshake(certFile.toString(), keyFile.toString(), ck.cert);
    }

    @Test
    void combinedPemWithoutPrivateKey_throwsIllegalArgumentException() throws Exception {
        CertKey ck = generateSelfSignedCert();
        Path certOnly = tempPem("edge-certonly");
        writePem(certOnly, ck.cert);

        assertThatThrownBy(() -> startServer(certOnly.toString(), null, TestSocketUtils.findAvailableTcpPort()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No private key found");
    }

    // --- infrastructure ---

    private void assertTlsHandshake(String certFile, String privateKeyFile, X509Certificate trustedCert) throws Exception {
        int port = TestSocketUtils.findAvailableTcpPort();
        Server server = startServer(certFile, privateKeyFile, port);
        try {
            StringWriter trustPem = new StringWriter();
            try (JcaPEMWriter w = new JcaPEMWriter(trustPem)) {
                w.writeObject(trustedCert);
            }
            ManagedChannel channel = NettyChannelBuilder.forAddress("localhost", port)
                    .sslContext(GrpcSslContexts.forClient()
                            .trustManager(new ByteArrayInputStream(trustPem.toString().getBytes(StandardCharsets.UTF_8)))
                            .build())
                    .build();
            try {
                channel.getState(true); // trigger connection attempt
                long deadline = System.currentTimeMillis() + 5_000;
                ConnectivityState state;
                do {
                    state = channel.getState(false);
                    if (state == ConnectivityState.READY || state == ConnectivityState.TRANSIENT_FAILURE) break;
                    Thread.sleep(50);
                } while (System.currentTimeMillis() < deadline);
                assertThat(state).as("TLS handshake should succeed").isEqualTo(ConnectivityState.READY);
            } finally {
                channel.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
            }
        } finally {
            server.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    /**
     * Mirrors the logic in {@link EdgeGrpcService#setupSsl}.
     */
    private Server startServer(String certFile, String privateKeyFile, int port) throws Exception {
        ByteArrayInputStream certChainIs;
        ByteArrayInputStream privateKeyIs;

        if (privateKeyFile == null || privateKeyFile.isEmpty()) {
            StringWriter certWriter = new StringWriter();
            StringWriter keyWriter = new StringWriter();
            try (var inStream = Files.newInputStream(Path.of(certFile));
                 PEMParser pemParser = new PEMParser(new InputStreamReader(inStream));
                 JcaPEMWriter certPemWriter = new JcaPEMWriter(certWriter);
                 JcaPEMWriter keyPemWriter = new JcaPEMWriter(keyWriter)) {
                Object object;
                while ((object = pemParser.readObject()) != null) {
                    if (object instanceof X509CertificateHolder) {
                        certPemWriter.writeObject(object);
                    } else {
                        keyPemWriter.writeObject(object);
                    }
                }
            }
            if (keyWriter.toString().isEmpty()) {
                throw new IllegalArgumentException("No private key found in cert file: " + certFile
                        + ". Provide a combined PEM (cert + key) or set edges.rpc.ssl.private_key.");
            }
            certChainIs = new ByteArrayInputStream(certWriter.toString().getBytes(StandardCharsets.UTF_8));
            privateKeyIs = new ByteArrayInputStream(keyWriter.toString().getBytes(StandardCharsets.UTF_8));
        } else {
            certChainIs = new ByteArrayInputStream(Files.readAllBytes(Path.of(certFile)));
            privateKeyIs = new ByteArrayInputStream(Files.readAllBytes(Path.of(privateKeyFile)));
        }

        return NettyServerBuilder.forPort(port)
                .addService(new EdgeRpcServiceGrpc.EdgeRpcServiceImplBase() {})
                .useTransportSecurity(certChainIs, privateKeyIs)
                .build()
                .start();
    }

    // --- cert / PEM helpers ---

    private record CertKey(X509Certificate cert, PrivateKey key) {}

    private CertKey generateSelfSignedCert() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        X500Name subject = new X500Name("CN=edge-grpc-test");
        Date now = new Date();
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(
                new JcaX509v3CertificateBuilder(
                        subject, BigInteger.ONE, now,
                        new Date(now.getTime() + 86_400_000L),
                        subject, kp.getPublic())
                        .build(signer));
        return new CertKey(cert, kp.getPrivate());
    }

    private Path tempPem(String prefix) throws Exception {
        Path p = Files.createTempFile(prefix + "-", ".pem");
        tempFiles.add(p);
        return p;
    }

    private void writePem(Path file, Object... objects) throws Exception {
        try (JcaPEMWriter writer = new JcaPEMWriter(Files.newBufferedWriter(file))) {
            for (Object o : objects) {
                writer.writeObject(o);
            }
        }
    }

}
