// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.junit.jupiter.api.Test;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.client.TbAiClient.TbAiResponse;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
    TbAiOperations operations = new TbAiOperations(channel, availability);

    @Test
    void shouldUseChannel_whenUsable() {
        // GIVEN
        given(availability.isUsable()).willReturn(true);
        given(channel.execute(operation, context)).willReturn(channelResponse);

        // WHEN-THEN
        assertThat(operations.execute(operation, context)).isSameAs(channelResponse);
        verify(availability, never()).markUnavailable();
    }

    @Test
    void shouldUseHttp_whenChannelIsNotUsable() {
        // GIVEN
        given(availability.isUsable()).willReturn(false);

        // WHEN-THEN
        assertThat(operations.execute(operation, context)).isSameAs(httpResponse);
        verify(channel, never()).execute(any(), any());
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

}
