// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.PskSecretResult;
import org.eclipse.leshan.server.registration.RegistrationStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MTransportUnavailableException;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TbLwM2mPskStoreTest {

    private final TbSecurityStore securityStore = mock(TbSecurityStore.class);
    private final TbLwM2mDtlsHandshakeListener handshakeListener = mock(TbLwM2mDtlsHandshakeListener.class);
    private final TbLwM2mPskStore pskStore = new TbLwM2mPskStore(securityStore, mock(RegistrationStore.class), handshakeListener);
    private final ConnectionId cid = new ConnectionId(new byte[]{1, 2, 3, 4});

    @Test
    public void givenKnownIdentity_whenSecretRequested_thenItIsReturned() {
        when(securityStore.getByPskIdentity("client")).thenReturn(SecurityInfo.newPreSharedKeyInfo("client", "client", new byte[]{1, 2, 3}));

        PskSecretResult result = request("client");

        assertThat(result.getSecret()).isNotNull();
        verify(handshakeListener, never()).onUnknownPskIdentity(any());
    }

    @Test
    public void givenUnknownIdentity_whenSecretRequested_thenFailureIsReported() {
        PskSecretResult result = request("unknown");

        assertThat(result.getSecret()).isNull();
        verify(handshakeListener).onUnknownPskIdentity(cid);
    }

    @Test
    public void givenCoreUnavailable_whenSecretRequested_thenFailureIsNotReported() {
        when(securityStore.getByPskIdentity("client")).thenThrow(new LwM2MTransportUnavailableException());

        PskSecretResult result = request("client");

        assertThat(result.getSecret()).isNull();
        verify(handshakeListener, never()).onUnknownPskIdentity(any());
    }

    private PskSecretResult request(String identity) {
        return pskStore.requestPskSecretResult(cid, null, new PskPublicInformation(identity), "HmacSHA256", null, null, false);
    }

}
