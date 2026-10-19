// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChatOperationRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.service.DaoSqlTest;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiChatControllerOnPremiseTest extends AbstractOnPremiseAiControllerTest {

    @Test
    public void shouldCreateChatWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        ObjectNode request = createChatRequest("Energy assistant chat");
        givenOperation(ChannelProtocol.CHAT_CREATE, new ChatOperationRequest(null, request), success(TextNode.valueOf(UUID.randomUUID().toString())));

        // WHEN
        doPost("/api/ai/chats", request).andExpect(status().isCreated());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.CHAT_CREATE, new ChatOperationRequest(null, request));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldUpdateChatWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID chatId = UUID.randomUUID();
        ObjectNode request = JacksonUtil.newObjectNode().put("title", "Renamed chat");
        givenOperation(ChannelProtocol.CHAT_UPDATE, new ChatOperationRequest(chatId, request), success());

        // WHEN
        doPatch("/api/ai/chats/" + chatId, request).andExpect(status().isNoContent());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.CHAT_UPDATE, new ChatOperationRequest(chatId, request));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldListChatsWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        givenOperation(ChannelProtocol.CHAT_LIST, null, success(JacksonUtil.newArrayNode()));

        // WHEN
        doGet("/api/ai/chats").andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.CHAT_LIST, null);
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldGetChatMessagesWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID chatId = UUID.randomUUID();
        givenOperation(ChannelProtocol.CHAT_MESSAGES, new ChatOperationRequest(chatId, null), success(JacksonUtil.newArrayNode()));

        // WHEN
        doGet("/api/ai/chats/" + chatId + "/messages").andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.CHAT_MESSAGES, new ChatOperationRequest(chatId, null));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldDeleteChatWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID chatId = UUID.randomUUID();
        givenOperation(ChannelProtocol.CHAT_DELETE, new ChatOperationRequest(chatId, null), success());

        // WHEN
        doDelete("/api/ai/chats/" + chatId).andExpect(status().isNoContent());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.CHAT_DELETE, new ChatOperationRequest(chatId, null));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldSendChatMessageWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID chatId = UUID.randomUUID();
        ObjectNode requestBody = sendMessageBody("What is the device status?", null);
        String acceptLanguage = "es-ES";
        given(tbAiTransport.sendChatMessage(eq(chatId), eq(requestBody), any(TbAiTurnContext.class)))
                .willReturn(Flux.just(ServerSentEvent.<String>builder().event("message").data("Checking device status...").build()));

        // WHEN
        MvcResult asyncStart = mockMvc.perform(sendMessageRequest(chatId, requestBody, acceptLanguage))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncStart)).andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiTurnContext> context = ArgumentCaptor.forClass(TbAiTurnContext.class);
        verify(tbAiTransport).sendChatMessage(eq(chatId), eq(requestBody), context.capture());
        assertThat(context.getValue().tbAccessToken()).isEqualTo("Bearer " + token);
        assertThat(context.getValue().acceptLanguage()).isEqualTo(acceptLanguage);
        assertTokenProviderBelongsToTenantAdmin(context.getValue().tokenProvider());
    }

}
