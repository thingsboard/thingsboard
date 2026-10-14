// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.transport.limits;

import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.ResumingServerHandshaker;
import org.eclipse.californium.scandium.dtls.ServerHandshaker;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DtlsHandshakeRateLimitsTest {

    private static final InetSocketAddress PEER = new InetSocketAddress("10.0.0.1", 5684);

    private final TransportRateLimitService rateLimitService = mock(TransportRateLimitService.class);

    @Test
    void givenBlockedAddress_whenChecked_thenHandshakeIsRejectedWithAccessDenied() {
        when(rateLimitService.checkAddress(PEER)).thenReturn(false);

        assertThatThrownBy(() -> DtlsHandshakeRateLimits.checkAddress(rateLimitService, handshaker(ServerHandshaker.class, true)))
                .isInstanceOf(HandshakeException.class)
                .extracting(e -> ((HandshakeException) e).getAlert().getDescription())
                .isEqualTo(AlertDescription.ACCESS_DENIED);
    }

    @Test
    void givenAllowedAddress_whenChecked_thenHandshakeProceeds() {
        when(rateLimitService.checkAddress(PEER)).thenReturn(true);

        assertThatCode(() -> DtlsHandshakeRateLimits.checkAddress(rateLimitService, handshaker(ServerHandshaker.class, true)))
                .doesNotThrowAnyException();
    }

    @Test
    void givenFailedCertificateVerifyOrFinished_whenClassified_thenItIsFailedProofOfPossession() {
        assertThat(DtlsHandshakeRateLimits.isFailedProofOfPossession(handshaker(ServerHandshaker.class, true),
                failure("Verification of signature failed", AlertDescription.DECRYPT_ERROR))).isTrue();
    }

    @Test
    void givenResumptionThatFellBackToFullHandshake_whenItFails_thenItIsFailedProofOfPossession() {
        // A ClientHello with an unknown session id goes through the cookie exchange and then a full handshake
        assertThat(DtlsHandshakeRateLimits.isFailedProofOfPossession(handshaker(ResumingServerHandshaker.class, true),
                failure("Verification of signature failed", AlertDescription.DECRYPT_ERROR))).isTrue();
    }

    @Test
    void givenOtherFailures_whenClassified_thenTheyAreNotFailedProofOfPossession() {
        Handshaker fullHandshake = handshaker(ServerHandshaker.class, true);
        // How DTLSConnector reports a fatal alert received from the peer, e.g. a client that does not trust the server certificate
        assertThat(DtlsHandshakeRateLimits.isFailedProofOfPossession(fullHandshake,
                failure("Received 'fatal alert/DECRYPT_ERROR'", AlertDescription.DECRYPT_ERROR))).isFalse();
        assertThat(DtlsHandshakeRateLimits.isFailedProofOfPossession(fullHandshake,
                failure("Certificate chain could not be validated", AlertDescription.BAD_CERTIFICATE))).isFalse();
        assertThat(DtlsHandshakeRateLimits.isFailedProofOfPossession(fullHandshake, new RuntimeException("Handshake flight timeout"))).isFalse();
        // An abbreviated handshake may have skipped the cookie exchange that verifies the address
        assertThat(DtlsHandshakeRateLimits.isFailedProofOfPossession(handshaker(ResumingServerHandshaker.class, false),
                failure("Verification of FINISHED message failed", AlertDescription.DECRYPT_ERROR))).isFalse();
    }

    private static <T extends Handshaker> T handshaker(Class<T> type, boolean fullHandshake) {
        T handshaker = mock(type);
        when(handshaker.getPeerAddress()).thenReturn(PEER);
        when(handshaker.isFullHandshake()).thenReturn(fullHandshake);
        return handshaker;
    }

    private static HandshakeException failure(String message, AlertDescription description) {
        return new HandshakeException(message, new AlertMessage(AlertLevel.FATAL, description));
    }

}
