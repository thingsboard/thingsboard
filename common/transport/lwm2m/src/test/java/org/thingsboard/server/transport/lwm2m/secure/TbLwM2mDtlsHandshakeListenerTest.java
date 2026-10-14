// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import org.eclipse.californium.elements.auth.PreSharedKeyIdentity;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.Connection;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.ServerHandshaker;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;

import java.net.InetSocketAddress;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbLwM2mDtlsHandshakeListenerTest {

    private static final InetSocketAddress PEER = new InetSocketAddress("10.0.0.1", 5684);

    private final TransportRateLimitService rateLimitService = mock(TransportRateLimitService.class);
    private final LwM2mClientCertificateRegistry certificateRegistry = new LwM2mClientCertificateRegistry();
    private final TbLwM2mDtlsHandshakeListener listener = new TbLwM2mDtlsHandshakeListener(rateLimitService, certificateRegistry);
    private final ConnectionId cid = new ConnectionId(new byte[]{1, 2, 3, 4});
    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());

    @Test
    public void givenBlockedAddress_whenHandshakeStarts_thenItIsRejected() {
        when(rateLimitService.checkAddress(PEER)).thenReturn(false);

        assertThatThrownBy(() -> listener.handshakeStarted(handshaker(ServerHandshaker.class)))
                .isInstanceOf(HandshakeException.class)
                .extracting(e -> ((HandshakeException) e).getAlert().getDescription())
                .isEqualTo(AlertDescription.ACCESS_DENIED);
    }

    @Test
    public void givenUnknownPskIdentity_whenReportedForTheConnection_thenFailureIsCountedOnceForTheHandshakeAddress() throws Exception {
        when(rateLimitService.checkAddress(PEER)).thenReturn(true);
        Handshaker handshaker = handshaker(ServerHandshaker.class);
        listener.handshakeStarted(handshaker);

        listener.onUnknownPskIdentity(cid);
        verify(rateLimitService).onAuthFailure(PEER);

        // The client retransmits its key exchange, and the store is asked again
        listener.onUnknownPskIdentity(cid);
        listener.handshakeFailed(handshaker, new RuntimeException("Handshake flight timeout"));
        listener.onUnknownPskIdentity(cid);
        verify(rateLimitService).onAuthFailure(PEER);
    }

    @Test
    public void givenX509OrPskIdentity_whenContextIsEstablished_thenSuccessIsReported() throws Exception {
        listener.contextEstablished(handshaker(ServerHandshaker.class), context(new PreSharedKeyIdentity("client")));

        verify(rateLimitService).onAuthSuccess(PEER);
    }

    @Test
    public void givenRawPublicKey_whenContextIsEstablished_thenSuccessIsNotReported() throws Exception {
        // Raw public keys are trusted during the handshake and checked at registration
        RawPublicKeyIdentity identity = new RawPublicKeyIdentity(KeyPairGenerator.getInstance("EC").generateKeyPair().getPublic());

        listener.contextEstablished(handshaker(ServerHandshaker.class), context(identity));

        verify(rateLimitService, never()).onAuthSuccess(any());
    }

    @Test
    public void givenClientCertificate_whenHandshakeSucceedsOrFails_thenItIsRegisteredOnlyOnSuccess() throws Exception {
        certificateRegistry.registerPending(cid, certificate(), System.currentTimeMillis());
        listener.handshakeFailed(handshaker(ServerHandshaker.class), new RuntimeException("Handshake flight timeout"));
        listener.contextEstablished(handshaker(ServerHandshaker.class), context(new PreSharedKeyIdentity("client")));
        assertThat(certificateRegistry.findByCertificate(tenantId, "hash")).isEmpty();

        certificateRegistry.registerPending(cid, certificate(), System.currentTimeMillis());
        listener.contextEstablished(handshaker(ServerHandshaker.class), context(new PreSharedKeyIdentity("client")));
        assertThat(certificateRegistry.findByCertificate(tenantId, "hash")).hasSize(1);
    }

    @Test
    public void givenCertificateRevokedDuringHandshake_whenContextIsEstablished_thenHandshakeFails() {
        // The core answered before the revocation, which reaches the transport before the client's Finished message
        certificateRegistry.registerPending(cid, certificate(), System.currentTimeMillis() - 1);
        certificateRegistry.revoke(tenantId, "hash");

        assertThatThrownBy(() -> listener.contextEstablished(handshaker(ServerHandshaker.class), context(new PreSharedKeyIdentity("client"))))
                .isInstanceOf(HandshakeException.class)
                .extracting(e -> ((HandshakeException) e).getAlert().getDescription())
                .isEqualTo(AlertDescription.CERTIFICATE_REVOKED);
        assertThat(certificateRegistry.findByCertificate(tenantId, "hash")).isEmpty();
        verify(rateLimitService, never()).onAuthSuccess(any());
    }

    @Test
    public void givenHandshakeFailures_whenClassified_thenOnlyFailedProofOfPossessionIsCounted() {
        // The classification itself is covered by DtlsHandshakeRateLimitsTest
        listener.handshakeFailed(handshaker(ServerHandshaker.class), failure("Verification of signature failed", AlertDescription.DECRYPT_ERROR));
        verify(rateLimitService).onAuthFailure(PEER);

        listener.handshakeFailed(handshaker(ServerHandshaker.class), failure("Received 'fatal alert/DECRYPT_ERROR'", AlertDescription.DECRYPT_ERROR));
        verify(rateLimitService).onAuthFailure(PEER);
    }

    private LwM2mClientCertificateRegistry.ClientCertificate certificate() {
        return new LwM2mClientCertificateRegistry.ClientCertificate("client", tenantId, "hash", Long.MAX_VALUE);
    }

    private <T extends Handshaker> T handshaker(Class<T> type) {
        T handshaker = mock(type);
        Connection connection = mock(Connection.class);
        when(connection.getConnectionId()).thenReturn(cid);
        when(handshaker.getConnection()).thenReturn(connection);
        when(handshaker.getPeerAddress()).thenReturn(PEER);
        when(handshaker.isFullHandshake()).thenReturn(true);
        return handshaker;
    }

    private static DTLSContext context(Principal peerIdentity) {
        DTLSSession session = mock(DTLSSession.class);
        when(session.getPeerIdentity()).thenReturn(peerIdentity);
        DTLSContext context = mock(DTLSContext.class);
        when(context.getSession()).thenReturn(session);
        return context;
    }

    private static HandshakeException failure(String message, AlertDescription description) {
        return new HandshakeException(message, new AlertMessage(AlertLevel.FATAL, description));
    }

}
