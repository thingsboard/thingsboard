// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.mqtt;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import javax.net.ssl.X509TrustManager;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class MqttX509TrustManagerTest {

    private final MqttSslHandlerProvider.ThingsboardMqttX509TrustManager trustManager =
            new MqttSslHandlerProvider.ThingsboardMqttX509TrustManager(mock(X509TrustManager.class));

    @Test
    public void givenChainSignedByEachOther_whenCheckClientTrusted_thenAccepted() throws Exception {
        KeyPair caKeys = generateKeyPair();
        X509Certificate ca = generateCertificate("CN=CA", caKeys, "CN=CA", caKeys.getPrivate(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        KeyPair deviceKeys = generateKeyPair();
        // Expired: the device is rejected at CONNECT, the handshake must not fail because of it
        X509Certificate device = generateCertificate("CN=device", deviceKeys, "CN=CA", caKeys.getPrivate(), System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));

        assertThatCode(() -> trustManager.checkClientTrusted(new X509Certificate[]{device, ca}, "RSA")).doesNotThrowAnyException();
    }

    @Test
    public void givenChainNotSignedByEachOther_whenCheckClientTrusted_thenRejected() throws Exception {
        KeyPair caKeys = generateKeyPair();
        X509Certificate ca = generateCertificate("CN=CA", caKeys, "CN=CA", caKeys.getPrivate(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        KeyPair deviceKeys = generateKeyPair();
        X509Certificate selfSigned = generateCertificate("CN=device", deviceKeys, "CN=device", deviceKeys.getPrivate(), System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));

        assertThatThrownBy(() -> trustManager.checkClientTrusted(new X509Certificate[]{selfSigned, ca}, "RSA"))
                .isInstanceOf(CertificateException.class);
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static X509Certificate generateCertificate(String subject, KeyPair subjectKeys, String issuer, PrivateKey issuerKey, long notAfter) throws Exception {
        long notBefore = notAfter - TimeUnit.DAYS.toMillis(30);
        var builder = new JcaX509v3CertificateBuilder(new X500Name(issuer), BigInteger.valueOf(System.nanoTime()), new Date(notBefore), new Date(notAfter),
                new X500Name(subject), subjectKeys.getPublic());
        return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey)));
    }

}
