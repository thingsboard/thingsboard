// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TbAiOperationsTest {

    TbAiTurnContext context = new TbAiTurnContext(mock(SecurityUser.class), "Bearer jwt", null, () -> "token",
            new TbAiClientRequest("https://tb.example.com", Map.of()));
    TbAiResponse httpResponse = TbAiResponse.builder().success(true).build();
    TbAiResponse channelResponse = TbAiResponse.builder().success(true).build();
    TbAiOperation operation = new TbAiOperation(ChannelProtocol.SOLUTION_INSTALL, null, () -> httpResponse);
    ChannelTbAiOperations channel = mock(ChannelTbAiOperations.class);
    TbAiChannelAvailability availability = mock(TbAiChannelAvailability.class);
    TbAiCallbackOriginValidator callbackOriginValidator = mock(TbAiCallbackOriginValidator.class);
    TbAiClientRequestFactory clientRequestFactory = mock(TbAiClientRequestFactory.class);
    TbAiOperations operations = new TbAiOperations(channel, availability, callbackOriginValidator, clientRequestFactory);

    @Test
    void shouldUseChannel_whenUsable() {
        // GIVEN
        given(availability.isUsable()).willReturn(true);
        given(channel.execute(operation, context)).willReturn(channelResponse);

        // WHEN-THEN
        assertThat(operations.execute(operation, context)).isSameAs(channelResponse);
        verify(availability, never()).markUnavailable();
        verify(callbackOriginValidator, never()).validate(any());
    }

    @Test
    void shouldUseHttp_whenChannelIsNotUsable() {
        // GIVEN
        given(availability.isUsable()).willReturn(false);

        // WHEN-THEN
        assertThat(operations.execute(operation, context)).isSameAs(httpResponse);
        verify(channel, never()).execute(any(), any());
        verify(callbackOriginValidator).validate(context.user());
    }

    @Test
    void shouldNotCallHttp_whenCallbackOriginIsInvalid() {
        // GIVEN
        given(availability.isUsable()).willReturn(false);
        willThrow(new ThingsboardRuntimeException("Please configure the base URL", ThingsboardErrorCode.BAD_REQUEST_PARAMS))
                .given(callbackOriginValidator).validate(context.user());
        TbAiOperation guarded = new TbAiOperation(ChannelProtocol.SOLUTION_INSTALL, null, () -> {
            throw new AssertionError("HTTP must not be used");
        });

        // WHEN-THEN
        assertThatThrownBy(() -> operations.execute(guarded, context)).hasMessageContaining("Please configure the base URL");
    }

    @Test
    void shouldFallBackToHttpAndBackOff_whenChannelIsUnavailable() {
        // GIVEN
        given(availability.isUsable()).willReturn(true);
        given(channel.execute(operation, context)).willThrow(new TbAiChannelUnavailableException("404", null));

        // WHEN-THEN
        assertThat(operations.execute(operation, context)).isSameAs(httpResponse);
        verify(availability).markUnavailable();
    }

    @Test
    void shouldFallBackToHttpWithoutBackOff_whenOperationsAreUnsupported() {
        // GIVEN
        given(availability.isUsable()).willReturn(true);
        given(channel.execute(operation, context)).willThrow(new TbAiOperationsUnsupportedException("old TB AI"));

        // WHEN-THEN
        assertThat(operations.execute(operation, context)).isSameAs(httpResponse);
        verify(availability, never()).markUnavailable();
    }

    @Test
    void shouldReturnRejection_whenChannelHandshakeIsRefused() {
        // GIVEN
        given(availability.isUsable()).willReturn(true);
        given(channel.execute(operation, context)).willThrow(new TbAiChannelRejectedException("Unauthorized"));

        // WHEN
        TbAiResponse response = operations.execute(operation, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Unauthorized");
        verify(availability, never()).markUnavailable();
    }

    @Test
    void shouldNotRequirePublicOrigin_whenOperationHasNoCallback() {
        // GIVEN
        given(availability.isUsable()).willReturn(false);
        TbAiOperation listChats = new TbAiOperation(ChannelProtocol.CHAT_LIST, null, () -> httpResponse);

        // WHEN-THEN
        assertThat(operations.execute(listChats, context)).isSameAs(httpResponse);
        verify(callbackOriginValidator, never()).validate(any());
    }

    @Test
    void shouldBuildDefaultOriginContext_whenCalledForUser() {
        // GIVEN
        SecurityUser user = mock(SecurityUser.class);
        TbAiClientRequest defaultRequest = TbAiClientRequest.withDefaultOrigin("https://tb.example.com");
        given(clientRequestFactory.forUser(user)).willReturn(defaultRequest);
        given(availability.isUsable()).willReturn(true);
        given(channel.execute(any(), any())).willReturn(channelResponse);

        // WHEN
        operations.execute(operation, user, () -> "token");

        // THEN
        ArgumentCaptor<TbAiTurnContext> captured = ArgumentCaptor.forClass(TbAiTurnContext.class);
        verify(channel).execute(same(operation), captured.capture());
        assertThat(captured.getValue().clientRequest()).isSameAs(defaultRequest);
        assertThat(captured.getValue().clientRequest().exactOrigin()).isFalse();
        assertThat(captured.getValue().tbAccessToken()).isNull();
    }

}
