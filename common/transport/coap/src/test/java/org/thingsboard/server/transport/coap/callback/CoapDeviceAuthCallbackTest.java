// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.coap.callback;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.UdpEndpointContext;
import org.eclipse.californium.elements.util.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.transport.coap.CoapTransportContext;

import java.net.InetSocketAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoapDeviceAuthCallbackTest {

    private static final InetSocketAddress PEER = new InetSocketAddress("10.0.0.1", 5684);

    private final TransportRateLimitService rateLimitService = mock(TransportRateLimitService.class);
    private final CoapTransportContext transportContext = new CoapTransportContext();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transportContext, "rateLimitService", rateLimitService);
    }

    @Test
    void givenWrongTokenOverDtls_whenValidated_thenFailureIsCounted() {
        callback(new DtlsEndpointContext(PEER, null, null, new Bytes(new byte[]{1}), 1, "TLS_PSK_WITH_AES_128_CCM_8", 0))
                .onSuccess(ValidateDeviceCredentialsResponse.builder().build());

        verify(rateLimitService).onAuthFailure(PEER);
    }

    @Test
    void givenWrongTokenOverPlainUdp_whenValidated_thenFailureIsNotCounted() {
        // A plain UDP source address can be forged: counting it would let anyone get a victim's address blocked
        callback(new UdpEndpointContext(PEER)).onSuccess(ValidateDeviceCredentialsResponse.builder().build());

        verify(rateLimitService, never()).onAuthFailure(any());
    }

    @Test
    void givenValidToken_whenValidated_thenSuccessIsReported() {
        callback(new UdpEndpointContext(PEER)).onSuccess(ValidateDeviceCredentialsResponse.builder()
                .deviceInfo(new TransportDeviceInfo()).deviceProfile(new DeviceProfile()).build());

        verify(rateLimitService).onAuthSuccess(PEER);
        verify(rateLimitService, never()).onAuthFailure(any());
    }

    private CoapDeviceAuthCallback callback(EndpointContext sourceContext) {
        Request request = Request.newPost();
        request.setSourceContext(sourceContext);
        Exchange advanced = mock(Exchange.class);
        when(advanced.getRequest()).thenReturn(request);
        CoapExchange exchange = mock(CoapExchange.class);
        when(exchange.advanced()).thenReturn(advanced);
        return new CoapDeviceAuthCallback(transportContext, exchange, (msg, deviceProfile) -> {});
    }

}
