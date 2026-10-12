// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.PskIdentity;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.request.UpdateRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MTransportUnavailableException;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class TbLwM2MAuthorizerTest {

    private static final InetSocketAddress PEER = new InetSocketAddress("10.0.0.1", 5684);
    private static final String ENDPOINT = "client";

    private final TbMainSecurityStore securityStore = mock(TbMainSecurityStore.class);
    private final TransportRateLimitService rateLimitService = mock(TransportRateLimitService.class);
    private final TbLwM2MAuthorizer authorizer = new TbLwM2MAuthorizer(mock(TbLwM2MDtlsSessionStore.class), securityStore,
            mock(LwM2mClientContext.class), rateLimitService);
    private final Registration registration = mock(Registration.class);

    @BeforeEach
    public void setUp() {
        when(registration.getEndpoint()).thenReturn(ENDPOINT);
        when(rateLimitService.checkAddress(PEER)).thenReturn(true);
    }

    @Test
    public void givenBlockedAddress_whenRegistering_thenDeclinedWithoutLookup() {
        when(rateLimitService.checkAddress(PEER)).thenReturn(false);

        assertThat(authorizer.isAuthorized(mock(RegisterRequest.class), registration, new IpPeer(PEER, new PskIdentity(ENDPOINT))).isDeclined()).isTrue();
        verifyNoInteractions(securityStore);
    }

    @Test
    public void givenBlockedAddress_whenRegisteredClientUpdates_thenItIsNotBlocked() {
        // A block stops new registrations only, like a block of new MQTT connections
        when(rateLimitService.checkAddress(PEER)).thenReturn(false);
        when(securityStore.getByEndpoint(ENDPOINT)).thenReturn(SecurityInfo.newPreSharedKeyInfo(ENDPOINT, ENDPOINT, new byte[]{1, 2, 3}));

        assertThat(authorizer.isAuthorized(mock(UpdateRequest.class), registration, new IpPeer(PEER, new PskIdentity(ENDPOINT))).isApproved()).isTrue();
    }

    @Test
    public void givenSecureClientWithExpectedCredentials_whenAuthorized_thenSuccessIsReported() {
        when(securityStore.getByEndpoint(ENDPOINT)).thenReturn(SecurityInfo.newPreSharedKeyInfo(ENDPOINT, ENDPOINT, new byte[]{1, 2, 3}));

        assertThat(authorizer.isAuthorized(null, registration, new IpPeer(PEER, new PskIdentity(ENDPOINT))).isApproved()).isTrue();
        verify(rateLimitService).onAuthSuccess(PEER);
    }

    @Test
    public void givenSecureClientWithOtherCredentials_whenAuthorized_thenFailureIsCounted() {
        when(securityStore.getByEndpoint(ENDPOINT)).thenReturn(SecurityInfo.newPreSharedKeyInfo(ENDPOINT, "other", new byte[]{1, 2, 3}));

        assertThat(authorizer.isAuthorized(null, registration, new IpPeer(PEER, new PskIdentity(ENDPOINT))).isDeclined()).isTrue();
        verify(rateLimitService).onAuthFailure(PEER);
    }

    @Test
    public void givenUnsecuredClient_whenDeclined_thenFailureIsNotCounted() {
        // The source address of an unsecured request can be forged
        when(securityStore.getByEndpoint(ENDPOINT)).thenReturn(SecurityInfo.newPreSharedKeyInfo(ENDPOINT, ENDPOINT, new byte[]{1, 2, 3}));

        assertThat(authorizer.isAuthorized(null, registration, new IpPeer(PEER)).isDeclined()).isTrue();
        verify(rateLimitService, never()).onAuthFailure(any());
    }

    @Test
    public void givenCoreUnavailable_whenAuthorized_thenDeclinedAndFailureIsNotCounted() {
        when(securityStore.getByEndpoint(ENDPOINT)).thenThrow(new LwM2MTransportUnavailableException());

        assertThat(authorizer.isAuthorized(null, registration, new IpPeer(PEER, new PskIdentity(ENDPOINT))).isDeclined()).isTrue();
        verify(rateLimitService, never()).onAuthFailure(any());
    }

}
