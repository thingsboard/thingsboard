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
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class SseTbAiTransportTest {

    @Mock
    TbAiClient tbAiClient;
    @Mock
    TbAiClient.TokenProvider tokenProvider;
    @Mock
    SecurityUser user;
    @Mock
    TbAiCallbackOriginValidator callbackOriginValidator;

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
                new TbAiTurnContext(user, "tb-access-token", "de-DE", tokenProvider,
                        new TbAiClientRequest("https://tb.example.com", Map.of())));

        // THEN
        assertThat(result.collectList().block()).isEqualTo(expectedStream.collectList().block());
        then(callbackOriginValidator).should().validate(same(user));
        then(tbAiClient).should().sendChatMessage(eq(chatId), same(request), eq("tb-access-token"), eq("de-DE"), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldFailStreamWithoutCallingClient_whenCallbackOriginIsInvalid() {
        // GIVEN
        willThrow(new ThingsboardRuntimeException("Please configure the base URL", ThingsboardErrorCode.BAD_REQUEST_PARAMS))
                .given(callbackOriginValidator).validate(same(user));

        // WHEN
        Flux<ServerSentEvent<String>> result = transport.sendChatMessage(UUID.randomUUID(), JacksonUtil.newObjectNode(),
                new TbAiTurnContext(user, "tb-access-token", null, tokenProvider,
                        new TbAiClientRequest("http://localhost:8080", Map.of())));

        // THEN
        assertThatThrownBy(result::blockLast).hasMessageContaining("Please configure the base URL");
        then(tbAiClient).shouldHaveNoInteractions();
    }

}
