// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TbAiOperationsTest {

    TbAiTurnContext context = new TbAiTurnContext(mock(SecurityUser.class), "Bearer jwt", null, () -> "token",
            new TbAiClientRequest("https://tb.example.com", Map.of()));
    TbAiResponse channelResponse = TbAiResponse.builder().success(true).build();
    TbAiOperation operation = new TbAiOperation(ChannelProtocol.SOLUTION_INSTALL, null);
    ChannelTbAiOperations channel = mock(ChannelTbAiOperations.class);
    TbAiClientRequestFactory clientRequestFactory = mock(TbAiClientRequestFactory.class);
    TbAiOperations operations = new TbAiOperations(channel, clientRequestFactory);

    @Test
    void shouldReturnChannelResponse_whenChannelAnswers() {
        // GIVEN
        given(channel.execute(operation, context)).willReturn(channelResponse);

        // WHEN-THEN
        assertThat(operations.execute(operation, context)).isSameAs(channelResponse);
    }

    @Test
    void shouldReturnServiceUnavailable_whenChannelCannotBeOpened() {
        // GIVEN
        given(channel.execute(operation, context)).willThrow(new TbAiChannelUnavailableException("404", null));

        // WHEN
        TbAiResponse response = operations.execute(operation, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Service unavailable");
    }

    @Test
    void shouldReturnUnsupported_whenAiServiceLacksTheOperation() {
        // GIVEN
        given(channel.execute(operation, context)).willThrow(new TbAiOperationsUnsupportedException("old TB AI"));

        // WHEN
        TbAiResponse response = operations.execute(operation, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("The AI service does not support this ThingsBoard version");
    }

    @Test
    void shouldReturnRejection_whenChannelHandshakeIsRefused() {
        // GIVEN
        given(channel.execute(operation, context)).willThrow(new TbAiChannelRejectedException("Unauthorized"));

        // WHEN
        TbAiResponse response = operations.execute(operation, context);

        // THEN
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError()).isEqualTo("Unauthorized");
    }

    @Test
    void shouldBuildDefaultOriginContext_whenCalledForUser() {
        // GIVEN
        SecurityUser user = mock(SecurityUser.class);
        TbAiClientRequest defaultRequest = TbAiClientRequest.withDefaultOrigin("https://tb.example.com");
        given(clientRequestFactory.forUser(user)).willReturn(defaultRequest);
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
