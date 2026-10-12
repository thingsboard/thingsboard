// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.coapserver;

import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.Connection;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.ResumingServerHandshaker;
import org.eclipse.californium.scandium.dtls.ServerHandshaker;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbCoapDtlsHandshakeListenerTest {

    private static final InetSocketAddress PEER = new InetSocketAddress("10.0.0.1", 5684);

    private final TbCoapDtlsCertificateVerifier verifier = mock(TbCoapDtlsCertificateVerifier.class);
    private final TransportRateLimitService rateLimitService = mock(TransportRateLimitService.class);
    private final TbCoapDtlsHandshakeListener listener = new TbCoapDtlsHandshakeListener(verifier, rateLimitService);
    private final ConnectionId cid = new ConnectionId(new byte[]{1, 2, 3, 4});

    @Test
    public void givenBlockedAddress_whenHandshakeStarts_thenItIsRejected() {
        when(rateLimitService.checkAddress(PEER)).thenReturn(false);

        assertThatThrownBy(() -> listener.handshakeStarted(handshaker(ServerHandshaker.class)))
                .isInstanceOf(HandshakeException.class)
                .extracting(e -> ((HandshakeException) e).getAlert().getDescription())
                .isEqualTo(AlertDescription.ACCESS_DENIED);
    }

    @Test
    public void givenAllowedAddress_whenHandshakeStarts_thenItProceeds() {
        when(rateLimitService.checkAddress(PEER)).thenReturn(true);

        assertThatCode(() -> listener.handshakeStarted(handshaker(ServerHandshaker.class))).doesNotThrowAnyException();
    }

    @Test
    public void givenFailedCertificateVerify_whenHandshakeFails_thenFailureIsCounted() {
        listener.handshakeFailed(handshaker(ServerHandshaker.class), failure("Verification of signature failed", AlertDescription.DECRYPT_ERROR));

        verify(rateLimitService).onAuthFailure(PEER);
        verify(verifier).dropSession(cid);
    }

    @Test
    public void givenAlertSentByClient_whenHandshakeFails_thenFailureIsNotCounted() {
        // How DTLSConnector reports a fatal alert received from the peer, e.g. a client that does not trust the server certificate
        listener.handshakeFailed(handshaker(ServerHandshaker.class), failure("Received 'fatal alert/DECRYPT_ERROR'", AlertDescription.DECRYPT_ERROR));

        verify(rateLimitService, never()).onAuthFailure(any());
    }

    @Test
    public void givenFailuresOtherThanProofOfPossession_whenHandshakeFails_thenTheyAreNotCounted() {
        listener.handshakeFailed(handshaker(ServerHandshaker.class), failure("Certificate chain could not be validated", AlertDescription.BAD_CERTIFICATE));
        listener.handshakeFailed(handshaker(ServerHandshaker.class), failure("IP address is blocked", AlertDescription.ACCESS_DENIED));
        listener.handshakeFailed(handshaker(ServerHandshaker.class), new RuntimeException("Handshake flight timeout"));
        listener.handshakeFailed(handshaker(ResumingServerHandshaker.class), failure("Verification of FINISHED message failed", AlertDescription.DECRYPT_ERROR));

        verify(rateLimitService, never()).onAuthFailure(any());
    }

    @Test
    public void givenDeviceCertificate_whenContextIsEstablished_thenSessionIsStoredAndSuccessReported() {
        when(verifier.commitSession(cid)).thenReturn(true);

        listener.contextEstablished(handshaker(ServerHandshaker.class), mock(DTLSContext.class));

        verify(rateLimitService).onAuthSuccess(PEER);
    }

    @Test
    public void givenUnknownCertificate_whenContextIsEstablished_thenSuccessIsNotReported() {
        // The client may still authenticate its requests with an access token
        when(verifier.commitSession(cid)).thenReturn(false);

        listener.contextEstablished(handshaker(ServerHandshaker.class), mock(DTLSContext.class));

        verify(rateLimitService, never()).onAuthSuccess(any());
    }

    private <T extends Handshaker> T handshaker(Class<T> type) {
        T handshaker = mock(type);
        Connection connection = mock(Connection.class);
        when(connection.getConnectionId()).thenReturn(cid);
        when(handshaker.getConnection()).thenReturn(connection);
        when(handshaker.getPeerAddress()).thenReturn(PEER);
        return handshaker;
    }

    private static HandshakeException failure(String message, AlertDescription description) {
        return new HandshakeException(message, new AlertMessage(AlertLevel.FATAL, description));
    }

}
