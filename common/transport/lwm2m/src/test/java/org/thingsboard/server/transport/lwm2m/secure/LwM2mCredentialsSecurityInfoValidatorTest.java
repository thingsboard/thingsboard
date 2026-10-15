// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.secure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MTransportUnavailableException;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.CLIENT;

public class LwM2mCredentialsSecurityInfoValidatorTest {

    private final TransportService transportService = mock(TransportService.class);
    private LwM2mCredentialsSecurityInfoValidator validator;

    @BeforeEach
    public void setUp() {
        LwM2mTransportContext context = mock(LwM2mTransportContext.class);
        when(context.getTransportService()).thenReturn(transportService);
        LwM2MTransportServerConfig config = mock(LwM2MTransportServerConfig.class);
        when(config.getTimeout()).thenReturn(100L);
        validator = new LwM2mCredentialsSecurityInfoValidator(context, config);
    }

    @Test
    public void givenNoSuchCredentials_whenValidated_thenNotFoundIsReported() {
        answer(callback -> callback.onSuccess(ValidateDeviceCredentialsResponse.builder().build()));

        assertThatThrownBy(() -> validator.getEndpointSecurityInfoByCredentialsId("client", CLIENT))
                .isInstanceOf(LwM2MAuthException.class)
                .isNotInstanceOf(LwM2MTransportUnavailableException.class);
    }

    @Test
    public void givenCoreError_whenValidated_thenUnavailabilityIsReported() {
        answer(callback -> callback.onError(new RuntimeException("failed")));

        assertThatThrownBy(() -> validator.getEndpointSecurityInfoByCredentialsId("client", CLIENT))
                .isInstanceOf(LwM2MTransportUnavailableException.class);
    }

    @Test
    public void givenNoAnswer_whenValidated_thenUnavailabilityIsReported() {
        assertThatThrownBy(() -> validator.getEndpointSecurityInfoByCredentialsId("client", CLIENT))
                .isInstanceOf(LwM2MTransportUnavailableException.class);
    }

    private void answer(Consumer<TransportServiceCallback<ValidateDeviceCredentialsResponse>> answer) {
        doAnswer(invocation -> {
            TransportServiceCallback<ValidateDeviceCredentialsResponse> callback = invocation.getArgument(1);
            answer.accept(callback);
            return null;
        }).when(transportService).process(any(ValidateDeviceLwM2MCredentialsRequestMsg.class), any());
    }

}
