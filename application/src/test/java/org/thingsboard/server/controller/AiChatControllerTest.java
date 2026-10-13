// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiChatControllerTest extends AbstractAiControllerTest {

    @MockitoBean
    private TbApiUsageReportClient apiUsageReportClient;

    // ------------------------------------------------------------------------
    // Happy paths
    // ------------------------------------------------------------------------

    @Test
    public void shouldReturnCreatedChatIdWithTenantAdminToken_whenCreateChat() throws Exception {
        // GIVEN
        loginTenantAdmin();
        ObjectNode request = createChatRequest("Energy assistant chat");
        UUID newChatId = UUID.randomUUID();
        given(tbAiClient.createChat(eq(request), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(TextNode.valueOf(newChatId.toString())));

        // WHEN
        JsonNode result = readResponse(doPost("/api/ai/chats", request).andExpect(status().isCreated()), JsonNode.class);

        // THEN
        assertThat(result.isTextual()).isTrue();
        assertThat(result.asText()).isEqualTo(newChatId.toString());

        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).createChat(eq(request), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldReturnNoContent_whenUpdateChat() throws Exception {
        // GIVEN
        loginTenantAdmin();
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
    public void shouldReturnChatSummaries_whenListChats() throws Exception {
        // GIVEN
        loginTenantAdmin();
        ArrayNode summaries = chatSummaries(UUID.randomUUID(), "Energy assistant chat");
        given(tbAiClient.listChats(any(TbAiClient.TokenProvider.class))).willReturn(success(summaries));

        // WHEN
        JsonNode result = doGet("/api/ai/chats", JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(summaries);
        verify(tbAiClient).listChats(any(TbAiClient.TokenProvider.class));
    }

    @Test
    public void shouldReturnChatMessages_whenGetChatMessages() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID chatId = UUID.randomUUID();
        ArrayNode messages = chatMessages();
        given(tbAiClient.getChatMessages(eq(chatId), any(TbAiClient.TokenProvider.class))).willReturn(success(messages));

        // WHEN
        JsonNode result = doGet("/api/ai/chats/" + chatId + "/messages", JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(messages);
        verify(tbAiClient).getChatMessages(eq(chatId), any(TbAiClient.TokenProvider.class));
    }

    @Test
    public void shouldReturnNoContent_whenDeleteChat() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID chatId = UUID.randomUUID();
        given(tbAiClient.deleteChat(eq(chatId), any(TbAiClient.TokenProvider.class))).willReturn(success());

        // WHEN
        doDelete("/api/ai/chats/" + chatId).andExpect(status().isNoContent());

        // THEN
        verify(tbAiClient).deleteChat(eq(chatId), any(TbAiClient.TokenProvider.class));
    }

    @Test
    public void shouldStreamAssistantEventsAndReportCredits_whenSendChatMessage() throws Exception {
        // GIVEN — user is on the alarm rules page and asks the assistant to add an alarm rule
        loginTenantAdmin();
        UUID chatId = UUID.randomUUID();
        String acceptLanguage = "es-ES";
        ObjectNode requestBody = sendMessageBody("Add an alarm rule that raises a Critical alarm when temperature exceeds 50", "ALARM_RULE_LIST");
        Flux<ServerSentEvent<String>> upstream = Flux.just(
                ServerSentEvent.<String>builder().event("message").data("Creating a high-temperature alarm rule...").build(),
                ServerSentEvent.<String>builder().event("creditsUsed").data("{\"creditsUsed\":5}").build()
        );
        given(tbAiClient.sendChatMessage(eq(chatId), eq(requestBody), eq("Bearer " + token), eq(acceptLanguage), any(TbAiClient.TokenProvider.class)))
                .willReturn(upstream);

        // WHEN
        MvcResult asyncStart = mockMvc.perform(sendMessageRequest(chatId, requestBody, acceptLanguage))
                .andExpect(request().asyncStarted())
                .andReturn();
        String body = mockMvc.perform(asyncDispatch(asyncStart))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // THEN — assistant events are forwarded to the caller, while the internal "creditsUsed"
        // event is consumed server-side and reported against the tenant's AI credit usage.
        assertThat(body).contains("Creating a high-temperature alarm rule...");
        assertThat(body).doesNotContain("creditsUsed");

        verify(apiUsageReportClient).report(any(), any(), eq(ApiUsageRecordKey.AI_CREDITS_COUNT), eq(5L));

        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).sendChatMessage(eq(chatId), eq(requestBody), eq("Bearer " + token), eq(acceptLanguage), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    // ------------------------------------------------------------------------
    // AI credit-check semantics
    // ------------------------------------------------------------------------

    @Test
    public void shouldReturnTooManyRequestsAndNotCallClient_whenSendChatMessageAndAiCreditsExhausted() throws Exception {
        // GIVEN — sending a message is credit-metered (processStream checks credits up front)
        loginTenantAdmin();
        givenAiCreditsExhausted();
        UUID chatId = UUID.randomUUID();

        // WHEN
        mockMvc.perform(sendMessageRequest(chatId, sendMessageBody("Hello", null), null))
                .andExpect(status().isTooManyRequests())
                .andExpect(statusReason(equalTo(MSG_OUT_OF_CREDITS)));

        // THEN
        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldReturnCreated_whenCreateChatAndAiCreditsExhausted() throws Exception {
        // GIVEN — creating a chat is exempt from the credit check (checkCredits=false)
        loginTenantAdmin();
        givenAiCreditsExhausted();
        ObjectNode request = createChatRequest("Energy assistant chat");
        UUID newChatId = UUID.randomUUID();
        given(tbAiClient.createChat(eq(request), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(TextNode.valueOf(newChatId.toString())));

        // WHEN
        JsonNode result = readResponse(doPost("/api/ai/chats", request).andExpect(status().isCreated()), JsonNode.class);

        // THEN
        assertThat(result.asText()).isEqualTo(newChatId.toString());
        verify(tbAiClient).createChat(eq(request), any(TbAiClient.TokenProvider.class));
    }

    // ------------------------------------------------------------------------
    // Feature flag / error propagation
    // ------------------------------------------------------------------------

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenAiFeatureDisabled() throws Exception {
        // GIVEN
        loginTenantAdmin();
        doReturn(false).when(aiSettings).isEnabled();

        // WHEN
        doPost("/api/ai/chats", createChatRequest("Energy assistant chat"))
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(MSG_AI_DISABLED)));

        // THEN
        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldReturnInternalServerErrorWithErrorMessage_whenAiClientReturnsFailureResponse() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID chatId = UUID.randomUUID();
        String error = "Chat service unavailable";
        given(tbAiClient.getChatMessages(eq(chatId), any(TbAiClient.TokenProvider.class))).willReturn(failure(error));

        // WHEN
        doGet("/api/ai/chats/" + chatId + "/messages")
                .andExpect(status().isInternalServerError())
                .andExpect(statusReason(equalTo(error)));

        // THEN
        verify(tbAiClient).getChatMessages(eq(chatId), any(TbAiClient.TokenProvider.class));
    }

    // ------------------------------------------------------------------------
    // Authorization
    // ------------------------------------------------------------------------

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenSysAdmin() throws Exception {
        // GIVEN
        loginSysAdmin();

        // WHEN
        doPost("/api/ai/chats", createChatRequest("Energy assistant chat"))
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));

        // THEN
        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenCustomerUser() throws Exception {
        // GIVEN
        loginCustomerUser();

        // WHEN
        doPost("/api/ai/chats", createChatRequest("Energy assistant chat"))
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));

        // THEN
        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenTenantAdminRoleExcludesAiWrite() throws Exception {
        // GIVEN
        loginTenantAdmin();
        loginAsRestrictedTenantAdmin(tenantId,
                Map.of(Resource.ALL, List.of(Operation.ALL)),
                Map.of(Resource.AI, List.of(Operation.ALL)));

        // WHEN
        doPost("/api/ai/chats", createChatRequest("Energy assistant chat"))
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermissionWrite + AI_RESOURCE)));

        // THEN
        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenTenantAdminRoleExcludesAiRead() throws Exception {
        // GIVEN
        loginTenantAdmin();
        loginAsRestrictedTenantAdmin(tenantId,
                Map.of(Resource.ALL, List.of(Operation.ALL)),
                Map.of(Resource.AI, List.of(Operation.ALL)));

        // WHEN
        doGet("/api/ai/chats")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermissionRead + AI_RESOURCE)));

        // THEN
        verifyNoInteractions(tbAiClient);
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static ArrayNode chatSummaries(UUID chatId, String title) {
        return JacksonUtil.newArrayNode()
                .add(JacksonUtil.newObjectNode()
                        .put("id", chatId.toString())
                        .put("createdTime", 1716460800000L)
                        .put("title", title));
    }

    private static ArrayNode chatMessages() {
        return JacksonUtil.newArrayNode()
                .add(chatMessage("USER", "What is the device status?"))
                .add(chatMessage("AI", "The device is online and reporting telemetry."));
    }

}
