// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.SolutionStepRequest;
import org.thingsboard.ai.common.channel.SolutionDataRequest;
import org.thingsboard.ai.common.channel.SolutionChatRequest;
import org.thingsboard.ai.common.channel.SolutionOperationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.data.solution.SolutionStep;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiSolutionControllerTest extends AbstractAiControllerTest {

    // ------------------------------------------------------------------------
    // Happy paths
    // ------------------------------------------------------------------------

    @Test
    public void shouldReturnSolutionWithTenantAdminToken_whenStartNew() throws Exception {
        // GIVEN
        loginTenantAdmin();
        ObjectNode solution = solutionResponse(UUID.randomUUID(), "Energy Monitoring", false, false);
        givenOperation(ChannelProtocol.SOLUTION_START, null, success(solution));

        // WHEN
        JsonNode result = readResponse(doPost("/api/ai/solution/start").andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        assertTokenProviderBelongsToTenantAdmin(captureStartNewTokenProvider());
    }

    @Test
    public void shouldReturnSolution_whenGetSolution() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        ObjectNode solution = solutionResponse(solutionId, "Energy Monitoring", false, false);
        givenOperation(ChannelProtocol.SOLUTION_GET, new SolutionOperationRequest(solutionId), success(solution));

        // WHEN
        JsonNode result = doGet("/api/ai/solution/" + solutionId, JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_GET, new SolutionOperationRequest(solutionId));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldReturnSolutionInfos_whenGetSolutions() throws Exception {
        // GIVEN
        loginTenantAdmin();
        ArrayNode infos = solutionInfosResponse(UUID.randomUUID(), "Energy Monitoring");
        givenOperation(ChannelProtocol.SOLUTION_LIST, null, success(infos));

        // WHEN
        JsonNode result = doGet("/api/ai/solution/infos", JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(infos);
        verifyOperation(ChannelProtocol.SOLUTION_LIST, null);
    }

    @Test
    public void shouldDelegateMessageAndStep_whenChat() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        SolutionStep step = SolutionStep.INITIAL_CONFIGURATION;
        String message = "Add humidity monitoring to the solution.";
        ObjectNode solution = solutionResponse(solutionId, "Energy Monitoring", false, false);
        givenOperation(ChannelProtocol.SOLUTION_CHAT, new SolutionChatRequest(solutionId, step, message), success(solution));

        // WHEN
        JsonNode result = readResponse(performChat(solutionId, step, message).andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_CHAT, new SolutionChatRequest(solutionId, step, message));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldDelegate_whenCreateSolution() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        ObjectNode solution = solutionResponse(solutionId, "Energy Monitoring", true, false);
        givenOperation(ChannelProtocol.SOLUTION_CREATE, new SolutionOperationRequest(solutionId), success(solution));

        // WHEN
        JsonNode result = readResponse(doPost("/api/ai/solution/" + solutionId + "/create").andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        verifyOperation(ChannelProtocol.SOLUTION_CREATE, new SolutionOperationRequest(solutionId));
    }

    @Test
    public void shouldDelegateDataKeyAndValue_whenUpdateData() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        String dataKey = "entities";
        ObjectNode value = JacksonUtil.newObjectNode();
        value.putArray("devices").add("Thermostat");
        ObjectNode solution = solutionResponse(solutionId, "Energy Monitoring", false, false);
        givenOperation(ChannelProtocol.SOLUTION_DATA_UPDATE, new SolutionDataRequest(solutionId, dataKey, value), success(solution));

        // WHEN
        JsonNode result = doPut("/api/ai/solution/" + solutionId + "/" + dataKey, value, JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        verifyOperation(ChannelProtocol.SOLUTION_DATA_UPDATE, new SolutionDataRequest(solutionId, dataKey, value));
    }

    @Test
    public void shouldDelegateAndReturnOk_whenClearStep() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        SolutionStep step = SolutionStep.DASHBOARDS_CONFIGURATION;
        givenOperation(ChannelProtocol.SOLUTION_STEP_CLEAR, new SolutionStepRequest(solutionId, step), success());

        // WHEN
        doDelete("/api/ai/solution/" + solutionId + "/" + step + "/clear").andExpect(status().isOk());

        // THEN
        verifyOperation(ChannelProtocol.SOLUTION_STEP_CLEAR, new SolutionStepRequest(solutionId, step));
    }

    @Test
    public void shouldForwardTbAccessToken_whenInstallSolution() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        ObjectNode installResult = solutionInstallResultResponse(UUID.randomUUID());
        givenOperation(ChannelProtocol.SOLUTION_INSTALL, new SolutionOperationRequest(solutionId), success(installResult));

        // WHEN
        JsonNode result = readResponse(doPost("/api/ai/solution/" + solutionId + "/install").andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(installResult);
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_INSTALL, new SolutionOperationRequest(solutionId));
        assertThat(context.tbAccessToken()).isEqualTo("Bearer " + token);
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldForwardTbAccessToken_whenUninstallSolution() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        ObjectNode solution = solutionResponse(solutionId, "Energy Monitoring", true, false);
        givenOperation(ChannelProtocol.SOLUTION_UNINSTALL, new SolutionOperationRequest(solutionId), success(solution));

        // WHEN
        JsonNode result = readResponse(doDelete("/api/ai/solution/" + solutionId + "/uninstall").andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_UNINSTALL, new SolutionOperationRequest(solutionId));
        assertThat(context.tbAccessToken()).isEqualTo("Bearer " + token);
    }

    @Test
    public void shouldDelegateAndReturnOk_whenDeleteSolution() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        givenOperation(ChannelProtocol.SOLUTION_DELETE, new SolutionOperationRequest(solutionId), success());

        // WHEN
        doDelete("/api/ai/solution/" + solutionId).andExpect(status().isOk());

        // THEN
        verifyOperation(ChannelProtocol.SOLUTION_DELETE, new SolutionOperationRequest(solutionId));
    }

    // ------------------------------------------------------------------------
    // AI credit-check semantics
    // ------------------------------------------------------------------------

    @Test
    public void shouldReturnTooManyRequestsAndNotCallClient_whenChatAndAiCreditsExhausted() throws Exception {
        // GIVEN — chat is credit-metered (process is called with checkCredits=true)
        loginTenantAdmin();
        givenAiCreditsExhausted();

        // WHEN
        performChat(UUID.randomUUID(), SolutionStep.INITIAL_CONFIGURATION, "Add humidity monitoring.")
                .andExpect(status().isTooManyRequests())
                .andExpect(statusReason(equalTo(MSG_OUT_OF_CREDITS)));

        // THEN
        verifyNoAiCalls();
    }

    @Test
    public void shouldReturnSolution_whenStartNewAndAiCreditsExhausted() throws Exception {
        // GIVEN — starting a solution is exempt from the credit check (checkCredits=false)
        loginTenantAdmin();
        givenAiCreditsExhausted();
        ObjectNode solution = solutionResponse(UUID.randomUUID(), "Energy Monitoring", false, false);
        givenOperation(ChannelProtocol.SOLUTION_START, null, success(solution));

        // WHEN
        JsonNode result = readResponse(doPost("/api/ai/solution/start").andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        verifyOperation(ChannelProtocol.SOLUTION_START, null);
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
        doPost("/api/ai/solution/start")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(MSG_AI_DISABLED)));

        // THEN
        verifyNoAiCalls();
    }

    @Test
    public void shouldReturnInternalServerErrorWithErrorMessage_whenAiClientReturnsFailureResponse() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        String error = "Solution service unavailable";
        givenOperation(ChannelProtocol.SOLUTION_GET, new SolutionOperationRequest(solutionId), failure(error));

        // WHEN
        doGet("/api/ai/solution/" + solutionId)
                .andExpect(status().isInternalServerError())
                .andExpect(statusReason(equalTo(error)));

        // THEN
        verifyOperation(ChannelProtocol.SOLUTION_GET, new SolutionOperationRequest(solutionId));
    }

    // ------------------------------------------------------------------------
    // Authorization
    // ------------------------------------------------------------------------

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenSysAdmin() throws Exception {
        // GIVEN
        loginSysAdmin();

        // WHEN
        doPost("/api/ai/solution/start")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));

        // THEN
        verifyNoAiCalls();
    }

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenCustomerUser() throws Exception {
        // GIVEN
        loginCustomerUser();

        // WHEN
        doPost("/api/ai/solution/start")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));

        // THEN
        verifyNoAiCalls();
    }

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenTenantAdminRoleExcludesAiWrite() throws Exception {
        // GIVEN
        loginTenantAdmin();
        loginAsRestrictedTenantAdmin(tenantId,
                Map.of(Resource.ALL, List.of(Operation.ALL)),
                Map.of(Resource.AI, List.of(Operation.ALL)));

        // WHEN
        doPost("/api/ai/solution/start")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermissionWrite + AI_RESOURCE)));

        // THEN
        verifyNoAiCalls();
    }

    @Test
    public void shouldReturnForbiddenAndNotCallClient_whenTenantAdminRoleExcludesAiRead() throws Exception {
        // GIVEN
        loginTenantAdmin();
        loginAsRestrictedTenantAdmin(tenantId,
                Map.of(Resource.ALL, List.of(Operation.ALL)),
                Map.of(Resource.AI, List.of(Operation.ALL)));

        // WHEN
        doGet("/api/ai/solution/infos")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermissionRead + AI_RESOURCE)));

        // THEN
        verifyNoAiCalls();
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private TbAiClient.TokenProvider captureStartNewTokenProvider() {
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_START, null);
        return context.tokenProvider();
    }

    private static ObjectNode solutionResponse(UUID solutionId, String solutionTitle, boolean built, boolean installed) {
        ObjectNode response = JacksonUtil.newObjectNode()
                .put("id", solutionId.toString())
                .put("createdTime", 1716460800000L)
                .put("tenantId", UUID.randomUUID().toString());
        response.set("states", solutionStatesResponse());
        response.set("data", solutionDataResponse(solutionTitle));
        response.set("metadata", solutionMetadataResponse(built, installed));
        return response;
    }

    private static ObjectNode solutionStatesResponse() {
        ObjectNode state = JacksonUtil.newObjectNode()
                .put("chatId", UUID.randomUUID().toString())
                .put("status", "IN_PROGRESS")
                .put("built", false)
                .put("skipInterviewAllowed", true);
        state.set("messages", JacksonUtil.newArrayNode()
                .add(chatMessage("AI", "Let's configure this solution.")));
        state.set("pendingChanges", JacksonUtil.newArrayNode()
                .add("Create humidity alarm rule"));

        ObjectNode states = JacksonUtil.newObjectNode();
        states.set("INITIAL_CONFIGURATION", state);
        return states;
    }

    private static ObjectNode solutionDataResponse(String solutionTitle) {
        return JacksonUtil.newObjectNode()
                .put("solutionTitle", solutionTitle)
                .put("solutionCounter", 7)
                .put("summary", "Monitor energy usage and humidity for the building.")
                .put("demoRequirements", "Use simulated telemetry.")
                .put("dashboardRequirements", "Show temperature, humidity and energy consumption.");
    }

    private static ObjectNode solutionMetadataResponse(boolean built, boolean installed) {
        ObjectNode metadata = JacksonUtil.newObjectNode()
                .put("built", built)
                .put("installed", installed);
        metadata.set("failures", JacksonUtil.newObjectNode());
        return metadata;
    }

    private static ArrayNode solutionInfosResponse(UUID solutionId, String solutionTitle) {
        return JacksonUtil.newArrayNode()
                .add(JacksonUtil.newObjectNode()
                        .put("id", solutionId.toString())
                        .put("createdTime", 1716460800000L)
                        .put("solutionTitle", solutionTitle)
                        .put("built", true)
                        .put("installed", false));
    }

    private static ObjectNode solutionInstallResultResponse(UUID dashboardId) {
        ObjectNode mainDashboardId = JacksonUtil.newObjectNode()
                .put("entityType", "DASHBOARD")
                .put("id", dashboardId.toString());
        ObjectNode response = JacksonUtil.newObjectNode();
        response.set("createdEntities", JacksonUtil.newArrayNode());
        response.set("mainDashboardId", mainDashboardId);
        response.set("entityResults", JacksonUtil.newObjectNode());
        return response;
    }

}
