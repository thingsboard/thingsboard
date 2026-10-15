// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.coapserver;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbCoapDtlsCertificateVerifierTest {

    private static final InetSocketAddress PEER = new InetSocketAddress("10.0.0.1", 5684);

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());

    private final TransportService transportService = mock(TransportService.class);
    private final TransportRateLimitService rateLimitService = mock(TransportRateLimitService.class);
    private final TbCoapDtlsCertificateVerifier verifier = new TbCoapDtlsCertificateVerifier(transportService, null, rateLimitService,
            new TbCoapDtlsSessionInMemoryStorage(TimeUnit.DAYS.toMillis(1), TimeUnit.MINUTES.toMillis(30)), false);
    private final ConnectionId cid = new ConnectionId(new byte[]{1, 2, 3, 4});

    @Test
    public void givenDeviceCertificate_whenVerified_thenSessionIsStoredOnlyAfterTheHandshakeProvedTheKey() throws Exception {
        X509Certificate certificate = generateCertificate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        String credentials = SslUtil.getCertificateString(certificate);
        answer(callback -> callback.onSuccess(deviceResponse(credentials)));

        CertificateVerificationResult result = verifyCertificate(certificate);

        assertThat(result.getException()).isNull();
        assertThat(verifier.getTbCoapDtlsSessionsMap()).isEmpty();
        assertThat(verifier.commitSession(cid)).isTrue();
        assertThat(verifier.getTbCoapDtlsSessionsMap()).hasSize(1);
        verify(rateLimitService, never()).onAuthFailure(any());
    }

    @Test
    public void givenFailedHandshake_whenSessionIsDropped_thenItIsNeverStored() throws Exception {
        X509Certificate certificate = generateCertificate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        String credentials = SslUtil.getCertificateString(certificate);
        answer(callback -> callback.onSuccess(deviceResponse(credentials)));
        verifyCertificate(certificate);

        verifier.dropSession(cid);

        assertThat(verifier.commitSession(cid)).isFalse();
        assertThat(verifier.getTbCoapDtlsSessionsMap()).isEmpty();
    }

    @Test
    public void givenCertificateRevokedDuringHandshake_whenCommitted_thenSessionIsNotStored() throws Exception {
        X509Certificate certificate = generateCertificate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        String credentials = SslUtil.getCertificateString(certificate);
        answer(callback -> callback.onSuccess(deviceResponse(credentials)));
        verifyCertificate(certificate);

        // The core answered before the revocation, which reaches the transport before the handshake completes
        verifier.evictSessions(TENANT_ID, EncryptionUtil.getSha3Hash(credentials));

        assertThat(verifier.commitSession(cid)).isFalse();
        assertThat(verifier.getTbCoapDtlsSessionsMap()).isEmpty();
    }

    @Test
    public void givenRegisteredCertificateAfterUnknownLeaf_whenVerified_thenItIsNotMatched() throws Exception {
        X509Certificate leaf = generateCertificate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        X509Certificate deviceCertificate = generateCertificate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        String deviceCredentials = SslUtil.getCertificateString(deviceCertificate);
        doAnswer(invocation -> {
            ValidateDeviceX509CertRequestMsg request = invocation.getArgument(1);
            TransportServiceCallback<ValidateDeviceCredentialsResponse> callback = invocation.getArgument(2);
            callback.onSuccess(request.getHash().equals(EncryptionUtil.getSha3Hash(deviceCredentials)) ?
                    deviceResponse(deviceCredentials) : ValidateDeviceCredentialsResponse.builder().build());
            return null;
        }).when(transportService).process(eq(DeviceTransportType.COAP), any(ValidateDeviceX509CertRequestMsg.class), any());

        // Decoded from the wire the way Scandium does: the certificates are not checked to form a chain
        CertificateMessage message = mock(CertificateMessage.class);
        when(message.getCertificateChain()).thenReturn(CertificateFactory.getInstance("X.509").generateCertPath(List.of(leaf, deviceCertificate)));
        verifier.verifyCertificate(cid, null, PEER, true, false, false, message);

        verify(rateLimitService).onAuthFailure(PEER);
        assertThat(verifier.commitSession(cid)).isFalse();
        assertThat(verifier.getTbCoapDtlsSessionsMap()).isEmpty();
    }

    @Test
    public void givenUnknownCertificate_whenVerified_thenFailureIsCountedAndHandshakeProceeds() throws Exception {
        X509Certificate certificate = generateCertificate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        answer(callback -> callback.onSuccess(ValidateDeviceCredentialsResponse.builder().build()));

        CertificateVerificationResult result = verifyCertificate(certificate);

        assertThat(result.getException()).as("the client may use an access token").isNull();
        verify(rateLimitService).onAuthFailure(PEER);
        assertThat(verifier.commitSession(cid)).isFalse();
    }

    @Test
    public void givenCoreUnavailable_whenVerified_thenFailureIsNotCounted() throws Exception {
        X509Certificate certificate = generateCertificate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        answer(callback -> callback.onError(new RuntimeException("timeout")));

        verifyCertificate(certificate);

        verify(rateLimitService, never()).onAuthFailure(any());
    }

    @Test
    public void givenExpiredCertificate_whenVerified_thenHandshakeFailsAndFailureIsCounted() throws Exception {
        X509Certificate certificate = generateCertificate(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));

        CertificateVerificationResult result = verifyCertificate(certificate);

        assertThat(result.getException().getAlert().getDescription()).isEqualTo(AlertDescription.BAD_CERTIFICATE);
        verify(rateLimitService).onAuthFailure(PEER);
    }

    private CertificateVerificationResult verifyCertificate(X509Certificate certificate) {
        return verifier.verifyCertificate(cid, null, PEER, true, false, false, new CertificateMessage(List.of(certificate)));
    }

    private void answer(Consumer<TransportServiceCallback<ValidateDeviceCredentialsResponse>> answer) {
        doAnswer(invocation -> {
            TransportServiceCallback<ValidateDeviceCredentialsResponse> callback = invocation.getArgument(2);
            answer.accept(callback);
            return null;
        }).when(transportService).process(eq(DeviceTransportType.COAP), any(ValidateDeviceX509CertRequestMsg.class), any());
    }

    private static ValidateDeviceCredentialsResponse deviceResponse(String credentials) {
        TransportDeviceInfo deviceInfo = new TransportDeviceInfo();
        deviceInfo.setTenantId(TENANT_ID);
        return ValidateDeviceCredentialsResponse.builder().deviceInfo(deviceInfo).deviceProfile(new DeviceProfile()).credentials(credentials).build();
    }

    private static X509Certificate generateCertificate(long notAfter) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        KeyPair keyPair = generator.generateKeyPair();
        X500Name name = new X500Name("CN=device");
        var builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, new Date(notAfter - TimeUnit.DAYS.toMillis(30)), new Date(notAfter),
                name, keyPair.getPublic());
        return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate())));
    }

}
