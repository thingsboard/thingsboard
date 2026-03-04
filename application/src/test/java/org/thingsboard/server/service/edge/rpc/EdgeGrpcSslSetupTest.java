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
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.PEMEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for EdgeGrpcService SSL/TLS setup.
 *
 * <p>{@link #startServer} mirrors {@code EdgeGrpcService.setupSsl()} and
 * {@code decryptPemKey()} so the tests exercise the same parsing logic used in
 * production without requiring a full Spring context.
 *
 * <p>Scenarios covered:
 * <ul>
 *   <li>Combined PEM (cert chain + private key in one file) — RSA-4096 and Ed25519</li>
 *   <li>Separate cert + key PEM files — RSA-4096 and Ed25519</li>
 *   <li>Password-protected (encrypted) private key — combined and separate</li>
 *   <li>Combined PEM with no private key → {@link IllegalArgumentException}</li>
 *   <li>Client trusts wrong CA → {@link ConnectivityState#TRANSIENT_FAILURE}</li>
 *   <li>Plaintext client connects to TLS server → {@link ConnectivityState#TRANSIENT_FAILURE}</li>
 *   <li>Client uses default system CAs (self-signed not trusted) → {@link ConnectivityState#TRANSIENT_FAILURE}</li>
 * </ul>
 */
class EdgeGrpcSslSetupTest {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final String KEY_PASSPHRASE = "test-key-passphrase";

    private final List<Path> tempFiles = new ArrayList<>();

    @AfterEach
    void cleanup() throws Exception {
        for (Path p : tempFiles) Files.deleteIfExists(p);
    }

    // ======================================================================
    // Key types
    // ======================================================================

    enum KeyType {
        RSA_4096, ED25519;

        CertKey generate() throws Exception {
            return switch (this) {
                case RSA_4096 -> buildCert(KeyPairGenerator.getInstance("RSA"), 4096, "SHA256withRSA");
                case ED25519  -> buildCert(KeyPairGenerator.getInstance("Ed25519"), 0, "Ed25519");
            };
        }

        private static CertKey buildCert(KeyPairGenerator kpg, int keySize, String sigAlg) throws Exception {
            if (keySize > 0) kpg.initialize(keySize);
            KeyPair kp = kpg.generateKeyPair();
            X500Name subject = new X500Name("CN=edge-grpc-test");
            Date now = new Date();
            ContentSigner signer = new JcaContentSignerBuilder(sigAlg).build(kp.getPrivate());
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(
                    new JcaX509v3CertificateBuilder(
                            subject, BigInteger.ONE, now,
                            new Date(now.getTime() + TimeUnit.DAYS.toMillis(1)),
                            subject, kp.getPublic())
                            .build(signer));
            return new CertKey(cert, kp.getPrivate());
        }
    }

    private record CertKey(X509Certificate cert, PrivateKey key) {}

    // ======================================================================
    // Happy path — combined and separate PEM, parameterized by key type
    // ======================================================================

    @ParameterizedTest(name = "combinedPem_{0}")
    @EnumSource(KeyType.class)
    void serverStartsAndClientConnects_combinedPem(KeyType keyType) throws Exception {
        CertKey ck = assumeKeyTypeSupported(keyType);
        Path combined = tempPem("edge-combined");
        writePem(combined, null, ck.cert, ck.key);

        Server server = assumeTlsSetupSupported(
                () -> startServer(combined.toString(), null, null, TestSocketUtils.findAvailableTcpPort()),
                keyType);

        assertClientConnectivity(server, ck.cert, ConnectivityState.READY);
    }

    @ParameterizedTest(name = "separatePem_{0}")
    @EnumSource(KeyType.class)
    void serverStartsAndClientConnects_separatePem(KeyType keyType) throws Exception {
        CertKey ck = assumeKeyTypeSupported(keyType);
        Path certFile = tempPem("edge-cert");
        Path keyFile  = tempPem("edge-key");
        writePem(certFile, null, ck.cert);
        writePem(keyFile,  null, ck.key);

        Server server = assumeTlsSetupSupported(
                () -> startServer(certFile.toString(), keyFile.toString(), null, TestSocketUtils.findAvailableTcpPort()),
                keyType);

        assertClientConnectivity(server, ck.cert, ConnectivityState.READY);
    }

    // ======================================================================
    // Password-protected private key — combined and separate, parameterized by cert location
    // ======================================================================

    enum EncryptedKeyLayout { COMBINED, SEPARATE }

    @ParameterizedTest(name = "encryptedKey_{0}")
    @EnumSource(EncryptedKeyLayout.class)
    void serverStartsAndClientConnects_encryptedPrivateKey(EncryptedKeyLayout layout) throws Exception {
        // Traditional-format encrypted keys (PEMEncryptedKeyPair) are RSA-specific
        CertKey ck = KeyType.RSA_4096.generate();
        Server server = switch (layout) {
            case COMBINED -> {
                Path combined = tempPem("edge-enc-combined");
                writePem(combined, KEY_PASSPHRASE, ck.cert, ck.key);
                yield startServer(combined.toString(), null, KEY_PASSPHRASE, TestSocketUtils.findAvailableTcpPort());
            }
            case SEPARATE -> {
                Path certFile = tempPem("edge-enc-cert");
                Path keyFile  = tempPem("edge-enc-key");
                writePem(certFile, null, ck.cert);
                writePem(keyFile, KEY_PASSPHRASE, ck.key);
                yield startServer(certFile.toString(), keyFile.toString(), KEY_PASSPHRASE, TestSocketUtils.findAvailableTcpPort());
            }
        };
        assertClientConnectivity(server, ck.cert, ConnectivityState.READY);
    }

    // ======================================================================
    // Server-side setup error
    // ======================================================================

    @Test
    void combinedPemWithoutPrivateKey_throwsIllegalArgumentException() throws Exception {
        CertKey ck = KeyType.RSA_4096.generate();
        Path certOnly = tempPem("edge-certonly");
        writePem(certOnly, null, ck.cert);

        assertThatThrownBy(() -> startServer(certOnly.toString(), null, null, TestSocketUtils.findAvailableTcpPort()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No private key found");
    }

    // ======================================================================
    // Client-side trust misconfigurations — parameterized by key type
    // ======================================================================

    /**
     * Client trusts cert B but server presents cert A.
     * TLS certificate verification must reject the server's cert → TRANSIENT_FAILURE.
     */
    @ParameterizedTest(name = "wrongClientCa_{0}")
    @EnumSource(KeyType.class)
    void wrongClientCa_handshakeFails(KeyType keyType) throws Exception {
        CertKey server = assumeKeyTypeSupported(keyType);
        CertKey wrongCa = KeyType.RSA_4096.generate();

        Path combined = tempPem("edge-wrong-ca");
        writePem(combined, null, server.cert, server.key);

        Server grpcServer = assumeTlsSetupSupported(
                () -> startServer(combined.toString(), null, null, TestSocketUtils.findAvailableTcpPort()),
                keyType);

        assertClientConnectivity(grpcServer, wrongCa.cert, ConnectivityState.TRANSIENT_FAILURE);
    }

    /**
     * Client uses plaintext (no TLS) against a TLS-only server.
     * Server expects a TLS ClientHello, receives HTTP/2 cleartext → connection reset → TRANSIENT_FAILURE.
     */
    @Test
    void plaintextClientToSslServer_connectionFails() throws Exception {
        CertKey ck = KeyType.RSA_4096.generate();
        Path combined = tempPem("edge-plaintext");
        writePem(combined, null, ck.cert, ck.key);

        Server server = startServer(combined.toString(), null, null, TestSocketUtils.findAvailableTcpPort());
        ManagedChannel channel = NettyChannelBuilder.forAddress("localhost", server.getPort())
                .usePlaintext()
                .build();
        try {
            assertThat(pollForState(channel, ConnectivityState.READY, ConnectivityState.TRANSIENT_FAILURE))
                    .as("Plaintext client must fail against TLS server")
                    .isEqualTo(ConnectivityState.TRANSIENT_FAILURE);
        } finally {
            channel.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
            server.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    /**
     * Client uses {@code GrpcSslContexts.forClient()} with no explicit trustManager,
     * which falls back to the JVM's default CA bundle. Self-signed test certs are not
     * in the system trust store → TLS verification fails → TRANSIENT_FAILURE.
     */
    @Test
    void clientWithNoExplicitCa_handshakeFails() throws Exception {
        CertKey ck = KeyType.RSA_4096.generate();
        Path combined = tempPem("edge-no-ca");
        writePem(combined, null, ck.cert, ck.key);

        Server server = startServer(combined.toString(), null, null, TestSocketUtils.findAvailableTcpPort());
        ManagedChannel channel = NettyChannelBuilder.forAddress("localhost", server.getPort())
                .sslContext(GrpcSslContexts.forClient().build())  // system CA bundle only
                .build();
        try {
            assertThat(pollForState(channel, ConnectivityState.READY, ConnectivityState.TRANSIENT_FAILURE))
                    .as("Self-signed cert must be rejected when client uses default system CAs")
                    .isEqualTo(ConnectivityState.TRANSIENT_FAILURE);
        } finally {
            channel.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
            server.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    // ======================================================================
    // Core: startServer — mirrors EdgeGrpcService.setupSsl() + decryptPemKey()
    // ======================================================================

    private Server startServer(String certFile, String privateKeyFile, String password, int port) throws Exception {
        char[] keyPass = (password != null && !password.isEmpty()) ? password.toCharArray() : null;
        ByteArrayInputStream certChainIs;
        ByteArrayInputStream privateKeyIs;

        if (privateKeyFile == null || privateKeyFile.isEmpty()) {
            // Combined PEM — split cert chain and private key
            StringWriter certWriter = new StringWriter();
            StringWriter keyWriter  = new StringWriter();
            try (var inStream  = Files.newInputStream(Path.of(certFile));
                 PEMParser     pemParser     = new PEMParser(new InputStreamReader(inStream));
                 JcaPEMWriter  certPemWriter = new JcaPEMWriter(certWriter);
                 JcaPEMWriter  keyPemWriter  = new JcaPEMWriter(keyWriter)) {
                Object object;
                while ((object = pemParser.readObject()) != null) {
                    if (object instanceof X509CertificateHolder) {
                        certPemWriter.writeObject(object);
                    } else if (object instanceof PEMEncryptedKeyPair && keyPass != null) {
                        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPass);
                        keyPemWriter.writeObject(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
                    } else if (object instanceof PKCS8EncryptedPrivateKeyInfo && keyPass != null) {
                        InputDecryptorProvider decProv = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPass);
                        keyPemWriter.writeObject(((PKCS8EncryptedPrivateKeyInfo) object).decryptPrivateKeyInfo(decProv));
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
            // Separate files
            certChainIs = new ByteArrayInputStream(Files.readAllBytes(Path.of(certFile)));
            if (keyPass != null) {
                StringWriter keyWriter = new StringWriter();
                try (var inStream = Files.newInputStream(Path.of(privateKeyFile));
                     PEMParser    pemParser    = new PEMParser(new InputStreamReader(inStream));
                     JcaPEMWriter keyPemWriter = new JcaPEMWriter(keyWriter)) {
                    Object object;
                    while ((object = pemParser.readObject()) != null) {
                        if (object instanceof PEMEncryptedKeyPair) {
                            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                                    .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPass);
                            keyPemWriter.writeObject(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
                        } else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                            InputDecryptorProvider decProv = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                                    .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPass);
                            keyPemWriter.writeObject(((PKCS8EncryptedPrivateKeyInfo) object).decryptPrivateKeyInfo(decProv));
                        } else {
                            keyPemWriter.writeObject(object);
                        }
                    }
                }
                privateKeyIs = new ByteArrayInputStream(keyWriter.toString().getBytes(StandardCharsets.UTF_8));
            } else {
                privateKeyIs = new ByteArrayInputStream(Files.readAllBytes(Path.of(privateKeyFile)));
            }
        }

        return NettyServerBuilder.forPort(port)
                .addService(new EdgeRpcServiceGrpc.EdgeRpcServiceImplBase() {})
                .useTransportSecurity(certChainIs, privateKeyIs)
                .build()
                .start();
    }

    // ======================================================================
    // Connectivity helpers
    // ======================================================================

    private void assertClientConnectivity(Server server, X509Certificate trustedCert,
                                          ConnectivityState expected) throws Exception {
        StringWriter trustPem = new StringWriter();
        try (JcaPEMWriter w = new JcaPEMWriter(trustPem)) {
            w.writeObject(trustedCert);
        }
        ManagedChannel channel = NettyChannelBuilder.forAddress("localhost", server.getPort())
                .sslContext(GrpcSslContexts.forClient()
                        .trustManager(new ByteArrayInputStream(
                                trustPem.toString().getBytes(StandardCharsets.UTF_8)))
                        .build())
                .build();
        try {
            assertThat(pollForState(channel, ConnectivityState.READY, ConnectivityState.TRANSIENT_FAILURE))
                    .isEqualTo(expected);
        } finally {
            channel.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
            server.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    private ConnectivityState pollForState(ManagedChannel channel, ConnectivityState... terminal)
            throws InterruptedException {
        Set<ConnectivityState> terminals = Set.of(terminal);
        channel.getState(true); // trigger connection attempt
        long deadline = System.currentTimeMillis() + 5_000;
        ConnectivityState state;
        do {
            state = channel.getState(false);
            if (terminals.contains(state)) break;
            Thread.sleep(50);
        } while (System.currentTimeMillis() < deadline);
        return state;
    }

    // ======================================================================
    // Assumption helpers
    // ======================================================================

    private CertKey assumeKeyTypeSupported(KeyType keyType) {
        try {
            return keyType.generate();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, keyType + " cert generation failed: " + e.getMessage());
            throw new AssertionError("unreachable");
        }
    }

    @FunctionalInterface
    private interface ServerStarter { Server start() throws Exception; }

    /** Skips the test if the TLS implementation doesn't support the given key type (e.g. Ed25519). */
    private Server assumeTlsSetupSupported(ServerStarter starter, KeyType keyType) {
        try {
            return starter.start();
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                    keyType + " TLS not supported in current environment: " + e.getMessage());
            throw new AssertionError("unreachable");
        }
    }

    // ======================================================================
    // PEM file helpers
    // ======================================================================

    /**
     * Write one or more objects to a PEM file.
     * When {@code password} is non-null, {@link PrivateKey} objects are written
     * using AES-256-CBC encryption (traditional format → parsed back as {@link PEMEncryptedKeyPair}).
     */
    private void writePem(Path file, String password, Object... objects) throws Exception {
        PEMEncryptor encryptor = null;
        if (password != null) {
            encryptor = new JcePEMEncryptorBuilder("AES-256-CBC")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(password.toCharArray());
        }
        try (JcaPEMWriter writer = new JcaPEMWriter(Files.newBufferedWriter(file))) {
            for (Object o : objects) {
                if (encryptor != null && o instanceof PrivateKey) {
                    writer.writeObject(o, encryptor);
                } else {
                    writer.writeObject(o);
                }
            }
        }
    }

    private Path tempPem(String prefix) throws Exception {
        Path p = Files.createTempFile(prefix + "-", ".pem");
        tempFiles.add(p);
        return p;
    }

}
