// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SseTbAiTransportTest {

    @Mock
    TbAiClient tbAiClient;
    @Mock
    TbAiClient.TokenProvider tokenProvider;
    @Mock
    SecurityUser user;

    @InjectMocks
    SseTbAiTransport transport;

    @Test
    void shouldDelegateToClientWithAccessTokenAndAcceptLanguage_whenSendChatMessageCalled() {
        // GIVEN
        UUID chatId = UUID.randomUUID();
        JsonNode request = JacksonUtil.newObjectNode().put("message", "Hello");
        Flux<ServerSentEvent<String>> expectedStream = Flux.just(ServerSentEvent.<String>builder()
                .event("assistantMessage")
                .data("{\"message\":\"Hi\"}")
                .build());
        given(tbAiClient.sendChatMessage(eq(chatId), same(request), eq("tb-access-token"), eq("de-DE"), same(tokenProvider)))
                .willReturn(expectedStream);

        // WHEN
        Flux<ServerSentEvent<String>> result = transport.sendChatMessage(chatId, request,
                new TbAiTurnContext(user, "tb-access-token", "de-DE", tokenProvider));

        // THEN
        assertThat(result).isSameAs(expectedStream);
        then(tbAiClient).should().sendChatMessage(eq(chatId), same(request), eq("tb-access-token"), eq("de-DE"), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
    }

}
