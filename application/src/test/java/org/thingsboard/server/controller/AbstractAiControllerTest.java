// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.data.solution.SolutionStep;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.service.ai.TbAiSettings;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.ai.transport.TbAiTransport;
import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@TestPropertySource(properties = {
        // base64-encoded ASCII "shared-test-jwt-signing-key-for-ai-controller-tests-not-secret!!"
        // (64 bytes — required minimum for HS512; tests only JWT.decode(), never verify the signature)
        "ai.jwt.signing_key=c2hhcmVkLXRlc3Qtand0LXNpZ25pbmcta2V5LWZvci1haS1jb250cm9sbGVyLXRlc3RzLW5vdC1zZWNyZXQhIQ=="
})
public abstract class AbstractAiControllerTest extends AbstractControllerTest {

    protected static final String AI_RESOURCE = "'AI' resource!";
    protected static final String MSG_AI_DISABLED = "AI feature is disabled";
    protected static final String MSG_OUT_OF_CREDITS = "Out of AI credits";

    // Every TB AI call goes through these two seams: operations (spied, so the per-user overload still builds the
    // real context) and the chat turn transport.
    @MockitoSpyBean
    protected TbAiOperations tbAiOperations;
    @MockitoBean
    protected TbAiTransport tbAiTransport;
    @MockitoSpyBean
    protected TbAiSettings aiSettings;
    @MockitoSpyBean
    protected TbApiUsageStateService tbApiUsageStateService;

    protected void givenAiCreditsExhausted() {
        ApiUsageState exhaustedUsageState = new ApiUsageState(tbApiUsageStateService.getApiUsageState(tenantId));
        exhaustedUsageState.setAiState(ApiUsageStateValue.DISABLED);
        assertThat(exhaustedUsageState.isAiEnabled()).isFalse();
        doReturn(exhaustedUsageState).when(tbApiUsageStateService).getApiUsageState(tenantId);
    }

    protected void assertTokenProviderBelongsToTenantAdmin(TbAiClient.TokenProvider tokenProvider) {
        var decoded = JWT.decode(tokenProvider.getToken());
        assertThat(decoded.getSubject()).isEqualTo(tenantAdminUserId.getId().toString());
        assertThat(decoded.getClaim("userId").asString()).isEqualTo(tenantAdminUserId.getId().toString());
        assertThat(decoded.getClaim("tenantId").asString()).isEqualTo(tenantId.getId().toString());
        assertThat(decoded.getClaim("email").asString()).isEqualTo(TENANT_ADMIN_EMAIL);
        assertThat(tokenProvider.getAdditionalInfo()).isEmpty();
    }

    protected void givenOperation(String type, Object payload, TbAiClient.TbAiResponse response) {
        doReturn(response).when(tbAiOperations).execute(argThat(operation(type, payload)), any(TbAiTurnContext.class));
    }

    protected void givenOperation(String type, TbAiClient.TbAiResponse response) {
        doReturn(response).when(tbAiOperations).execute(argThat(operation -> operation != null && type.equals(operation.type())),
                any(TbAiTurnContext.class));
    }

    /**
     * Verifies the operation ran once and returns the context it ran with (user, TB access token, token provider).
     */
    protected TbAiTurnContext verifyOperation(String type, Object payload) {
        ArgumentCaptor<TbAiTurnContext> context = ArgumentCaptor.forClass(TbAiTurnContext.class);
        verify(tbAiOperations).execute(argThat(operation(type, payload)), context.capture());
        return context.getValue();
    }

    protected TbAiTurnContext verifyOperation(String type) {
        ArgumentCaptor<TbAiTurnContext> context = ArgumentCaptor.forClass(TbAiTurnContext.class);
        verify(tbAiOperations).execute(argThat(operation -> operation != null && type.equals(operation.type())), context.capture());
        return context.getValue();
    }

    protected void verifyNoAiCalls() {
        verify(tbAiOperations, never()).execute(any(TbAiOperation.class), any(TbAiTurnContext.class));
        verifyNoInteractions(tbAiTransport);
    }

    private static ArgumentMatcher<TbAiOperation> operation(String type, Object payload) {
        return operation -> operation != null && type.equals(operation.type()) && Objects.equals(payload, operation.payload());
    }

    protected static TbAiClient.TbAiResponse success(JsonNode value) {
        return TbAiClient.TbAiResponse.builder().success(true).value(value).build();
    }

    protected static TbAiClient.TbAiResponse success() {
        return TbAiClient.TbAiResponse.builder().success(true).build();
    }

    protected static TbAiClient.TbAiResponse failure(String error) {
        return TbAiClient.TbAiResponse.builder().success(false).error(error).build();
    }

    protected static ObjectNode chatMessage(String from, String content) {
        return JacksonUtil.newObjectNode()
                .put("from", from)
                .put("content", content);
    }

    protected MockHttpServletRequestBuilder sendMessageRequest(UUID chatId, JsonNode body, String acceptLanguage) {
        MockHttpServletRequestBuilder request = post("/api/ai/chats/" + chatId + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content(JacksonUtil.toString(body));
        if (acceptLanguage != null) {
            request.header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);
        }
        setJwtToken(request);
        return request;
    }

    protected static ObjectNode sendMessageBody(String message, String viewType) {
        ObjectNode body = JacksonUtil.newObjectNode().put("message", message);
        if (viewType != null) {
            ObjectNode clientContext = JacksonUtil.newObjectNode();
            clientContext.putObject("view").put("type", viewType);
            body.set("clientContext", clientContext);
        }
        return body;
    }

    protected ResultActions performChat(UUID solutionId, SolutionStep step, String message) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/ai/solution/" + solutionId + "/" + step + "/chat")
                .contentType(MediaType.TEXT_PLAIN)
                .content(message);
        setJwtToken(request);
        return mockMvc.perform(request);
    }

    protected static ObjectNode createChatRequest(String title) {
        return JacksonUtil.newObjectNode()
                .put("title", title);
    }

}
