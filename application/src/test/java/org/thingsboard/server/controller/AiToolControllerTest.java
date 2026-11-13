// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiToolControllerTest extends AbstractAiControllerTest {

    @Test
    public void shouldCallTbAiClientWithDecisionAndUserTokenProvider_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();

        ObjectNode request = decision(true);
        ObjectNode response = approvedResponse();
        givenAiClientResolvesToolApproval(request, response);

        // WHEN
        JsonNode result = doPost("/api/ai/tools/resolve-approval", request, JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(response);

        TbAiClient.TokenProvider tokenProvider = verifyAiClientResolvedToolApproval(request);
        assertTokenProviderBelongsToTenantAdmin(tokenProvider);
    }

    @Test
    public void shouldCallTbAiClientWithDecisionAndUserTokenProvider_whenAiCreditsExhausted() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenAiCreditsExhausted();

        ObjectNode request = decision(true);
        ObjectNode response = approvedResponse();
        givenAiClientResolvesToolApproval(request, response);

        // WHEN
        JsonNode result = doPost("/api/ai/tools/resolve-approval", request, JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(response);

        TbAiClient.TokenProvider tokenProvider = verifyAiClientResolvedToolApproval(request);
        assertTokenProviderBelongsToTenantAdmin(tokenProvider);
    }

    @Test
    public void shouldReturnInternalServerErrorWithErrorMessage_whenAiClientReturnsFailureResponse() throws Exception {
        // GIVEN
        loginTenantAdmin();

        ObjectNode request = decision(true);
        String error = "AI service unavailable";
        givenAiClientRejectsToolApproval(request, error);

        // WHEN
        ResultActions result = doPost("/api/ai/tools/resolve-approval", request);

        // THEN
        result.andExpect(status().isInternalServerError())
                .andExpect(statusReason(equalTo(error)));

        TbAiClient.TokenProvider tokenProvider = verifyAiClientResolvedToolApproval(request);
        assertTokenProviderBelongsToTenantAdmin(tokenProvider);
    }

    @Test
    public void shouldReturnForbiddenAndNotCallTbAiClient_whenAiFeatureDisabled() throws Exception {
        // GIVEN
        loginTenantAdmin();
        doReturn(false).when(aiSettings).isEnabled();

        // WHEN
        ResultActions result = doPost("/api/ai/tools/resolve-approval", anyDecision());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(MSG_AI_DISABLED)));

        verifyNoAiCalls();
    }

    @Test
    public void shouldReturnForbidden_whenSysAdmin() throws Exception {
        // GIVEN
        loginSysAdmin();

        // WHEN
        ResultActions result = doPost("/api/ai/tools/resolve-approval", anyDecision());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));

        verifyNoAiCalls();
    }

    @Test
    public void shouldReturnForbidden_whenCustomerUser() throws Exception {
        // GIVEN
        loginCustomerUser();

        // WHEN
        ResultActions result = doPost("/api/ai/tools/resolve-approval", anyDecision());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));

        verifyNoAiCalls();
    }

    @Test
    public void shouldReturnForbidden_whenTenantAdminRoleExcludesAi() throws Exception {
        // GIVEN
        loginTenantAdmin();
        loginAsRestrictedTenantAdmin(tenantId,
                Map.of(Resource.ALL, List.of(Operation.ALL)),
                Map.of(Resource.AI, List.of(Operation.ALL)));

        // WHEN
        ResultActions result = doPost("/api/ai/tools/resolve-approval", anyDecision());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermissionWrite + AI_RESOURCE)));

        verifyNoAiCalls();
    }

    private void givenAiClientResolvesToolApproval(ObjectNode request, ObjectNode response) {
        givenOperation(ChannelProtocol.TOOL_APPROVAL_RESOLVE, request, success(response));
    }

    private void givenAiClientRejectsToolApproval(ObjectNode request, String error) {
        givenOperation(ChannelProtocol.TOOL_APPROVAL_RESOLVE, request, failure(error));
    }

    private TbAiClient.TokenProvider verifyAiClientResolvedToolApproval(ObjectNode request) {
        TbAiTurnContext context = verifyOperation(ChannelProtocol.TOOL_APPROVAL_RESOLVE, request);
        verifyNoMoreInteractions(tbAiTransport);
        return context.tokenProvider();
    }

    private static ObjectNode anyDecision() {
        return decision(false);
    }

    private static ObjectNode decision(boolean approved) {
        return JacksonUtil.newObjectNode()
                .put("executionId", UUID.randomUUID().toString())
                .put("approved", approved);
    }

    private static ObjectNode approvedResponse() {
        return JacksonUtil.newObjectNode()
                .put("status", "APPROVED");
    }

}
