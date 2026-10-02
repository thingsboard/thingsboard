// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
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

    @MockitoBean
    protected TbAiClient tbAiClient;
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
