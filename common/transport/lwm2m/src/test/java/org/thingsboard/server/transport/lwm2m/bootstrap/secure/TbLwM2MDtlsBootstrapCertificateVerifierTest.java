// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.californium.elements.auth.X509CertPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TbLwM2MDtlsBootstrapCertificateVerifierTest {

    private static final InetSocketAddress PEER = new InetSocketAddress("10.0.0.1", 5686);

    private final LwM2MBootstrapSecurityStore bsSecurityStore = mock(LwM2MBootstrapSecurityStore.class);
    private TbLwM2MDtlsBootstrapCertificateVerifier verifier;
    private KeyPair rootKeys;
    private KeyPair intermediateKeys;
    private X509Certificate root;
    private X509Certificate intermediate;

    @BeforeEach
    void setUp() throws Exception {
        verifier = new TbLwM2MDtlsBootstrapCertificateVerifier(mock(LwM2MTransportServerConfig.class), bsSecurityStore,
                mock(TransportRateLimitService.class));
        rootKeys = generateKeyPair();
        intermediateKeys = generateKeyPair();
        root = generateCertificate("CN=root", rootKeys, "CN=root", rootKeys, true);
        intermediate = generateCertificate("CN=intermediate", intermediateKeys, "CN=root", rootKeys, true);
    }

    @Test
    void givenSessionChainNotInIssuerOrder_whenCertificateRechecked_thenLeafIsLookedUp() throws Exception {
        X509Certificate leaf = generateCertificate("CN=endpoint", generateKeyPair(), "CN=intermediate", intermediateKeys, false);
        String leafHash = EncryptionUtil.getSha3Hash(SslUtil.getCertificateString(leaf));
        when(bsSecurityStore.getX509ByEndpoint(leafHash, leafHash)).thenReturn(securityInfo());

        // A DTLS client may send its chain in any order, which the handshake accepts
        boolean registered = verifier.isRegistered(certPath(leaf, root, intermediate), PEER);

        assertThat(registered).isTrue();
        verify(bsSecurityStore).getX509ByEndpoint(leafHash, leafHash);
    }

    @Test
    void givenSessionCertificateNotRegistered_whenCertificateRechecked_thenNotRegistered() throws Exception {
        X509Certificate leaf = generateCertificate("CN=endpoint", generateKeyPair(), "CN=intermediate", intermediateKeys, false);

        assertThat(verifier.isRegistered(certPath(leaf, intermediate), PEER)).isFalse();
    }

    private static X509CertPath certPath(X509Certificate... chain) throws Exception {
        return new X509CertPath(CertificateFactory.getInstance("X.509").generateCertPath(List.of(chain)));
    }

    private static TbLwM2MSecurityInfo securityInfo() {
        TbLwM2MSecurityInfo securityInfo = new TbLwM2MSecurityInfo();
        securityInfo.setMsg(ValidateDeviceCredentialsResponse.builder().credentials("credentials").build());
        return securityInfo;
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        return generator.generateKeyPair();
    }

    private static X509Certificate generateCertificate(String subject, KeyPair subjectKeys, String issuer, KeyPair issuerKeys, boolean ca) throws Exception {
        long now = System.currentTimeMillis();
        var builder = new JcaX509v3CertificateBuilder(new X500Name(issuer), BigInteger.valueOf(now), new Date(now - TimeUnit.DAYS.toMillis(1)),
                new Date(now + TimeUnit.DAYS.toMillis(30)), new X500Name(subject), subjectKeys.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
        return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(issuerKeys.getPrivate())));
    }

}
