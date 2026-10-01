// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiClientRequest;
import org.thingsboard.server.service.ai.transport.TbAiTransport;
import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.server.service.security.model.SecurityUser;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DefaultAiChatServiceTest {

    @Mock
    TbAiService tbAiService;
    @Mock
    TbAiClient tbAiClient;
    @Mock
    TbAiClient.TokenProvider tokenProvider;
    @Mock
    TbAiClient.TbAiResponse tbAiResponse;
    @Mock
    SecurityUser user;
    @Mock
    TbAiTransport tbAiTransport;

    DefaultAiChatService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAiChatService(tbAiService, tbAiTransport);
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenCreateChatCalled() {
        // GIVEN
        JsonNode request = createChatRequest("New chat");
        UUID chatId = UUID.randomUUID();
        JsonNode expectedResponse = TextNode.valueOf(chatId.toString());
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(tbAiClient.createChat(same(request), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.createChat(request, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCall().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().createChat(same(request), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenUpdateChatCalled() {
        // GIVEN
        UUID chatId = UUID.randomUUID();
        JsonNode request = updateChatRequest("Updated chat");
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(null);
        given(tbAiClient.updateChat(eq(chatId), same(request), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        service.updateChat(chatId, request, user);

        // THEN
        assertThat(captureProcessCall().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().updateChat(eq(chatId), same(request), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenListChatsCalled() {
        // GIVEN
        UUID chatId = UUID.randomUUID();
        JsonNode expectedResponse = chatSummariesResponse(chatId, 1716460800000L, "My Test Chat");
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(tbAiClient.listChats(same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.listChats(user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCall().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().listChats(same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenGetChatMessagesCalled() {
        // GIVEN
        UUID chatId = UUID.randomUUID();
        JsonNode expectedResponse = chatMessagesResponse("Hello", "Hi there!");
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(tbAiClient.getChatMessages(eq(chatId), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.getChatMessages(chatId, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCall().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().getChatMessages(eq(chatId), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenDeleteChatCalled() {
        // GIVEN
        UUID chatId = UUID.randomUUID();
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(null);
        given(tbAiClient.deleteChat(eq(chatId), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        service.deleteChat(chatId, user);

        // THEN
        assertThat(captureProcessCall().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().deleteChat(eq(chatId), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessStreamAndDelegateToTransportWithTurnContext_whenSendChatMessageWithClientContextCalled() {
        // GIVEN
        UUID chatId = UUID.randomUUID();
        JsonNode request = sendMessageBody("Generate a dashboard", "DASHBOARD_LIST");
        String tbAccessToken = "tb-access-token";
        String acceptLanguage = "es-ES";
        Flux<ServerSentEvent<String>> expectedStream = Flux.just(ServerSentEvent.<String>builder()
                .event("assistantMessage")
                .data("{\"message\":\"ok\"}")
                .build());
        given(tbAiService.processStream(eq(chatId), any(), same(user))).willReturn(expectedStream);
        var clientRequest = new TbAiClientRequest("https://tb.example.com", Map.of("Host", "tb.example.com"));
        var expectedContext = new TbAiTurnContext(user, tbAccessToken, acceptLanguage, tokenProvider, clientRequest);
        given(tbAiTransport.sendChatMessage(eq(chatId), same(request), eq(expectedContext))).willReturn(expectedStream);

        // WHEN
        Flux<ServerSentEvent<String>> result = service.sendChatMessage(chatId, request, tbAccessToken, acceptLanguage, clientRequest, user);

        // THEN
        assertThat(result).isSameAs(expectedStream);

        assertThat(captureProcessStreamCall(chatId).apply(tbAiClient, tokenProvider)).isSameAs(expectedStream);
        then(tbAiTransport).should().sendChatMessage(eq(chatId), same(request), eq(expectedContext));
        then(tbAiTransport).shouldHaveNoMoreInteractions();
        then(tbAiClient).shouldHaveNoInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TbAiService.TbAiCall<TbAiClient.TbAiResponse> captureProcessCall() {
        ArgumentCaptor<TbAiService.TbAiCall> callCaptor = ArgumentCaptor.forClass(TbAiService.TbAiCall.class);
        then(tbAiService).should().process(callCaptor.capture(), same(user), eq(false));
        return callCaptor.getValue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TbAiService.TbAiCall<Flux<ServerSentEvent<String>>> captureProcessStreamCall(UUID chatId) {
        ArgumentCaptor<TbAiService.TbAiCall> callCaptor = ArgumentCaptor.forClass(TbAiService.TbAiCall.class);
        then(tbAiService).should().processStream(eq(chatId), callCaptor.capture(), same(user));
        return callCaptor.getValue();
    }

    private static ObjectNode createChatRequest(String title) {
        return JacksonUtil.newObjectNode()
                .put("title", title);
    }

    private static ObjectNode updateChatRequest(String title) {
        return JacksonUtil.newObjectNode()
                .put("title", title);
    }

    private static ObjectNode sendMessageBody(String message, String viewType) {
        ObjectNode body = JacksonUtil.newObjectNode().put("message", message);
        if (viewType != null) {
            ObjectNode clientContext = JacksonUtil.newObjectNode();
            clientContext.putObject("view").put("type", viewType);
            body.set("clientContext", clientContext);
        }
        return body;
    }

    private static ArrayNode chatSummariesResponse(UUID chatId, long createdTime, String title) {
        return JacksonUtil.newArrayNode()
                .add(JacksonUtil.newObjectNode()
                        .put("id", chatId.toString())
                        .put("createdTime", createdTime)
                        .put("title", title));
    }

    private static ArrayNode chatMessagesResponse(String userMessage, String assistantMessage) {
        return JacksonUtil.newArrayNode()
                .add(chatMessage("USER", userMessage))
                .add(chatMessage("AI", assistantMessage));
    }

    private static ObjectNode chatMessage(String from, String content) {
        return JacksonUtil.newObjectNode()
                .put("from", from)
                .put("content", content);
    }

}
