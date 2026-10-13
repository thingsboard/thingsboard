// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.service.DaoSqlTest;
import reactor.core.publisher.Flux;

import java.util.UUID;

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
        given(tbAiClient.createChat(eq(request), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(TextNode.valueOf(UUID.randomUUID().toString())));

        // WHEN
        doPost("/api/ai/chats", request).andExpect(status().isCreated());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).createChat(eq(request), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldUpdateChatWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID chatId = UUID.randomUUID();
        ObjectNode request = JacksonUtil.newObjectNode().put("title", "Renamed chat");
        given(tbAiClient.updateChat(eq(chatId), eq(request), any(TbAiClient.TokenProvider.class))).willReturn(success());

        // WHEN
        doPatch("/api/ai/chats/" + chatId, request).andExpect(status().isNoContent());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).updateChat(eq(chatId), eq(request), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldListChatsWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        given(tbAiClient.listChats(any(TbAiClient.TokenProvider.class)))
                .willReturn(success(JacksonUtil.newArrayNode()));

        // WHEN
        doGet("/api/ai/chats").andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).listChats(tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldGetChatMessagesWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID chatId = UUID.randomUUID();
        given(tbAiClient.getChatMessages(eq(chatId), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(JacksonUtil.newArrayNode()));

        // WHEN
        doGet("/api/ai/chats/" + chatId + "/messages").andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).getChatMessages(eq(chatId), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldDeleteChatWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID chatId = UUID.randomUUID();
        given(tbAiClient.deleteChat(eq(chatId), any(TbAiClient.TokenProvider.class))).willReturn(success());

        // WHEN
        doDelete("/api/ai/chats/" + chatId).andExpect(status().isNoContent());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).deleteChat(eq(chatId), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldSendChatMessageWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID chatId = UUID.randomUUID();
        ObjectNode requestBody = sendMessageBody("What is the device status?", null);
        String acceptLanguage = "es-ES";
        given(tbAiClient.sendChatMessage(eq(chatId), eq(requestBody), eq("Bearer " + token), eq(acceptLanguage), any(TbAiClient.TokenProvider.class)))
                .willReturn(Flux.just(ServerSentEvent.<String>builder().event("message").data("Checking device status...").build()));

        // WHEN
        MvcResult asyncStart = mockMvc.perform(sendMessageRequest(chatId, requestBody, acceptLanguage))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(asyncStart)).andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).sendChatMessage(eq(chatId), eq(requestBody), eq("Bearer " + token), eq(acceptLanguage), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

}
