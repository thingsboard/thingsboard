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
package org.thingsboard.monitoring.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WildcardAwareCertificateVerifierTest {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    void verifySubject_sniHostnameMatchesExactCert_returnsNoMismatch() throws Exception {
        X509Certificate cert = selfSignedCertWithDnsSan("coap.example.com");
        ServerNames serverNames = ServerNames.newInstance("coap.example.com");
        InetSocketAddress remotePeer = new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684);

        HandshakeException mismatch = WildcardAwareCertificateVerifier.verifySubject(serverNames, remotePeer, cert);

        assertThat(mismatch).isNull();
    }

    @Test
    void verifySubject_sniHostnameMatchesWildcardCert_returnsNoMismatch() throws Exception {
        X509Certificate cert = selfSignedCertWithDnsSan("*.example.com");
        ServerNames serverNames = ServerNames.newInstance("coap.example.com");
        InetSocketAddress remotePeer = new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684);

        HandshakeException mismatch = WildcardAwareCertificateVerifier.verifySubject(serverNames, remotePeer, cert);

        assertThat(mismatch).isNull();
    }

    @Test
    void verifySubject_sniHostnameMismatch_returnsException() throws Exception {
        X509Certificate cert = selfSignedCertWithDnsSan("other.example.com");
        ServerNames serverNames = ServerNames.newInstance("coap.example.com");
        InetSocketAddress remotePeer = new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684);

        HandshakeException mismatch = WildcardAwareCertificateVerifier.verifySubject(serverNames, remotePeer, cert);

        assertThat(mismatch).isNotNull();
    }

    @Test
    void verifySubject_overlyBroadWildcard_returnsException() throws Exception {
        // A cert for "*.co.uk" must never be accepted for an arbitrary "foo.co.uk" host.
        X509Certificate cert = selfSignedCertWithDnsSan("*.co.uk");
        ServerNames serverNames = ServerNames.newInstance("foo.co.uk");
        InetSocketAddress remotePeer = new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684);

        HandshakeException mismatch = WildcardAwareCertificateVerifier.verifySubject(serverNames, remotePeer, cert);

        assertThat(mismatch).isNotNull();
    }

    @Test
    void verifySubject_overlyBroadWildcardOnUnrelatedSan_doesNotAffectMatchingSan_returnsNoMismatch() throws Exception {
        // Regression test: a multi-SAN cert (e.g. shared/CDN hosting) carrying an unrelated
        // overly-broad wildcard for a different domain must not cause a false rejection of a
        // connection that legitimately matches a different, narrow SAN entry on the same cert.
        X509Certificate cert = selfSignedCert(
                new GeneralName(GeneralName.dNSName, "coap.example.com"),
                new GeneralName(GeneralName.dNSName, "*.co.uk"));
        ServerNames serverNames = ServerNames.newInstance("coap.example.com");
        InetSocketAddress remotePeer = new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684);

        HandshakeException mismatch = WildcardAwareCertificateVerifier.verifySubject(serverNames, remotePeer, cert);

        assertThat(mismatch).isNull();
    }

    @Test
    void verifySubject_ipLiteralTargetWithMatchingIpSan_returnsNoMismatch() throws Exception {
        X509Certificate cert = selfSignedCertWithIpSan("192.0.2.10");
        InetSocketAddress remotePeer = new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684);

        HandshakeException mismatch = WildcardAwareCertificateVerifier.verifySubject(null, remotePeer, cert);

        assertThat(mismatch).isNull();
    }

    @Test
    void verifySubject_ipLiteralTargetAgainstDnsOnlyCert_returnsException() throws Exception {
        // Regression test: an IP-literal coaps:// target (e.g. monitoring's check_domain_ips
        // feature) has no SNI hostname, so it must be matched against IP SANs, not DNS SANs.
        X509Certificate cert = selfSignedCertWithDnsSan("coap.example.com");
        InetSocketAddress remotePeer = new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684);

        HandshakeException mismatch = WildcardAwareCertificateVerifier.verifySubject(null, remotePeer, cert);

        assertThat(mismatch).isNotNull();
    }

    @Test
    void verifyCertificate_delegateReportsMismatch_returnsDelegateExceptionWithoutSubjectCheck() throws Exception {
        NewAdvancedCertificateVerifier delegate = mock(NewAdvancedCertificateVerifier.class);
        AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.BAD_CERTIFICATE);
        CertificateVerificationResult delegateResult = new CertificateVerificationResult(ConnectionId.EMPTY,
                new HandshakeException("untrusted", alert), null);
        when(delegate.verifyCertificate(any(), any(), any(), anyBoolean(), eq(false), anyBoolean(), any())).thenReturn(delegateResult);
        WildcardAwareCertificateVerifier verifier = new WildcardAwareCertificateVerifier(delegate);

        CertificateVerificationResult result = verifier.verifyCertificate(ConnectionId.EMPTY, ServerNames.newInstance("coap.example.com"),
                new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684), true, true, false, mock(CertificateMessage.class));

        assertThat(result).isSameAs(delegateResult);
    }

    @Test
    void verifyCertificate_verifySubjectFalse_skipsHostnameCheckEvenOnMismatch() throws Exception {
        X509Certificate cert = selfSignedCertWithDnsSan("other.example.com");
        NewAdvancedCertificateVerifier delegate = mock(NewAdvancedCertificateVerifier.class);
        CertificateVerificationResult delegateResult = new CertificateVerificationResult(ConnectionId.EMPTY, certPath(cert), null);
        when(delegate.verifyCertificate(any(), any(), any(), anyBoolean(), eq(false), anyBoolean(), any())).thenReturn(delegateResult);
        WildcardAwareCertificateVerifier verifier = new WildcardAwareCertificateVerifier(delegate);

        CertificateVerificationResult result = verifier.verifyCertificate(ConnectionId.EMPTY, ServerNames.newInstance("coap.example.com"),
                new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684), true, false, false, mock(CertificateMessage.class));

        assertThat(result).isSameAs(delegateResult);
    }

    @Test
    void verifyCertificate_emptyCertPath_returnsDelegateResultUnchanged() throws Exception {
        NewAdvancedCertificateVerifier delegate = mock(NewAdvancedCertificateVerifier.class);
        CertificateVerificationResult delegateResult = new CertificateVerificationResult(ConnectionId.EMPTY,
                CertificateFactory.getInstance("X.509").generateCertPath(List.of()), null);
        when(delegate.verifyCertificate(any(), any(), any(), anyBoolean(), eq(false), anyBoolean(), any())).thenReturn(delegateResult);
        WildcardAwareCertificateVerifier verifier = new WildcardAwareCertificateVerifier(delegate);

        CertificateVerificationResult result = verifier.verifyCertificate(ConnectionId.EMPTY, ServerNames.newInstance("coap.example.com"),
                new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684), true, true, false, mock(CertificateMessage.class));

        assertThat(result).isSameAs(delegateResult);
    }

    @Test
    void verifyCertificate_matchingHostname_returnsDelegateResultUnchanged() throws Exception {
        X509Certificate cert = selfSignedCertWithDnsSan("coap.example.com");
        NewAdvancedCertificateVerifier delegate = mock(NewAdvancedCertificateVerifier.class);
        CertificateVerificationResult delegateResult = new CertificateVerificationResult(ConnectionId.EMPTY, certPath(cert), null);
        when(delegate.verifyCertificate(any(), any(), any(), anyBoolean(), eq(false), anyBoolean(), any())).thenReturn(delegateResult);
        WildcardAwareCertificateVerifier verifier = new WildcardAwareCertificateVerifier(delegate);

        CertificateVerificationResult result = verifier.verifyCertificate(ConnectionId.EMPTY, ServerNames.newInstance("coap.example.com"),
                new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684), true, true, false, mock(CertificateMessage.class));

        assertThat(result).isSameAs(delegateResult);
    }

    @Test
    void verifyCertificate_mismatchingHostname_returnsNewExceptionResult() throws Exception {
        X509Certificate cert = selfSignedCertWithDnsSan("other.example.com");
        NewAdvancedCertificateVerifier delegate = mock(NewAdvancedCertificateVerifier.class);
        CertificateVerificationResult delegateResult = new CertificateVerificationResult(ConnectionId.EMPTY, certPath(cert), null);
        when(delegate.verifyCertificate(any(), any(), any(), anyBoolean(), eq(false), anyBoolean(), any())).thenReturn(delegateResult);
        WildcardAwareCertificateVerifier verifier = new WildcardAwareCertificateVerifier(delegate);

        CertificateVerificationResult result = verifier.verifyCertificate(ConnectionId.EMPTY, ServerNames.newInstance("coap.example.com"),
                new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684), true, true, false, mock(CertificateMessage.class));

        assertThat(result).isNotSameAs(delegateResult);
        assertThat(result.getException()).isNotNull();
    }

    @Test
    void verifySubject_certificateParsingExceptionOnSanLookup_failsClosed() throws Exception {
        X509Certificate cert = mock(X509Certificate.class);
        when(cert.getSubjectAlternativeNames()).thenThrow(new CertificateParsingException("malformed SAN"));
        when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN=test"));
        ServerNames serverNames = ServerNames.newInstance("coap.example.com");
        InetSocketAddress remotePeer = new InetSocketAddress(InetAddress.getByName("192.0.2.10"), 5684);

        HandshakeException mismatch = WildcardAwareCertificateVerifier.verifySubject(serverNames, remotePeer, cert);

        assertThat(mismatch).isNotNull();
    }

    private static CertPath certPath(X509Certificate cert) throws Exception {
        return CertificateFactory.getInstance("X.509").generateCertPath(List.of(cert));
    }

    private static X509Certificate selfSignedCertWithDnsSan(String dnsName) throws Exception {
        return selfSignedCert(new GeneralName(GeneralName.dNSName, dnsName));
    }

    private static X509Certificate selfSignedCertWithIpSan(String ipAddress) throws Exception {
        return selfSignedCert(new GeneralName(GeneralName.iPAddress, ipAddress));
    }

    private static X509Certificate selfSignedCert(GeneralName... sanEntries) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name subject = new X500Name("CN=test");
        Date notBefore = Date.from(Instant.now().minusSeconds(3600));
        Date notAfter = Date.from(Instant.now().plusSeconds(3600));
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, BigInteger.ONE, notBefore, notAfter, subject, keyPair.getPublic());
        builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(sanEntries));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(builder.build(signer));
    }

}
