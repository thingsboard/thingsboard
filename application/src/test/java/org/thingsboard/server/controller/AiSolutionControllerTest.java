// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        given(tbAiClient.startNewSolution(any(TbAiClient.TokenProvider.class))).willReturn(success(solution));

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
        given(tbAiClient.getSolution(eq(solutionId), any(TbAiClient.TokenProvider.class))).willReturn(success(solution));

        // WHEN
        JsonNode result = doGet("/api/ai/solution/" + solutionId, JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).getSolution(eq(solutionId), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldReturnSolutionInfos_whenGetSolutions() throws Exception {
        // GIVEN
        loginTenantAdmin();
        ArrayNode infos = solutionInfosResponse(UUID.randomUUID(), "Energy Monitoring");
        given(tbAiClient.getSolutions(any(TbAiClient.TokenProvider.class))).willReturn(success(infos));

        // WHEN
        JsonNode result = doGet("/api/ai/solution/infos", JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(infos);
        verify(tbAiClient).getSolutions(any(TbAiClient.TokenProvider.class));
    }

    @Test
    public void shouldDelegateMessageAndStep_whenChat() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        SolutionStep step = SolutionStep.INITIAL_CONFIGURATION;
        String message = "Add humidity monitoring to the solution.";
        ObjectNode solution = solutionResponse(solutionId, "Energy Monitoring", false, false);
        given(tbAiClient.sendSolutionMessage(eq(solutionId), eq(step), eq(message), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(solution));

        // WHEN
        JsonNode result = readResponse(performChat(solutionId, step, message).andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).sendSolutionMessage(eq(solutionId), eq(step), eq(message), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldDelegate_whenCreateSolution() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        ObjectNode solution = solutionResponse(solutionId, "Energy Monitoring", true, false);
        given(tbAiClient.createSolution(eq(solutionId), any(TbAiClient.TokenProvider.class))).willReturn(success(solution));

        // WHEN
        JsonNode result = readResponse(doPost("/api/ai/solution/" + solutionId + "/create").andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        verify(tbAiClient).createSolution(eq(solutionId), any(TbAiClient.TokenProvider.class));
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
        given(tbAiClient.updateData(eq(solutionId), eq(dataKey), eq(value), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(solution));

        // WHEN
        JsonNode result = doPut("/api/ai/solution/" + solutionId + "/" + dataKey, value, JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        verify(tbAiClient).updateData(eq(solutionId), eq(dataKey), eq(value), any(TbAiClient.TokenProvider.class));
    }

    @Test
    public void shouldDelegateAndReturnOk_whenClearStep() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        SolutionStep step = SolutionStep.DASHBOARDS_CONFIGURATION;
        given(tbAiClient.clearStep(eq(solutionId), eq(step), any(TbAiClient.TokenProvider.class))).willReturn(success());

        // WHEN
        doDelete("/api/ai/solution/" + solutionId + "/" + step + "/clear").andExpect(status().isOk());

        // THEN
        verify(tbAiClient).clearStep(eq(solutionId), eq(step), any(TbAiClient.TokenProvider.class));
    }

    @Test
    public void shouldForwardTbAccessToken_whenInstallSolution() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        ObjectNode installResult = solutionInstallResultResponse(UUID.randomUUID());
        given(tbAiClient.installSolution(eq(solutionId), eq("Bearer " + token), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(installResult));

        // WHEN
        JsonNode result = readResponse(doPost("/api/ai/solution/" + solutionId + "/install").andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(installResult);
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).installSolution(eq(solutionId), eq("Bearer " + token), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldForwardTbAccessToken_whenUninstallSolution() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        ObjectNode solution = solutionResponse(solutionId, "Energy Monitoring", true, false);
        given(tbAiClient.uninstallSolution(eq(solutionId), eq("Bearer " + token), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(solution));

        // WHEN
        JsonNode result = readResponse(doDelete("/api/ai/solution/" + solutionId + "/uninstall").andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        verify(tbAiClient).uninstallSolution(eq(solutionId), eq("Bearer " + token), any(TbAiClient.TokenProvider.class));
    }

    @Test
    public void shouldDelegateAndReturnOk_whenDeleteSolution() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        given(tbAiClient.deleteSolution(eq(solutionId), any(TbAiClient.TokenProvider.class))).willReturn(success());

        // WHEN
        doDelete("/api/ai/solution/" + solutionId).andExpect(status().isOk());

        // THEN
        verify(tbAiClient).deleteSolution(eq(solutionId), any(TbAiClient.TokenProvider.class));
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
        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldReturnSolution_whenStartNewAndAiCreditsExhausted() throws Exception {
        // GIVEN — starting a solution is exempt from the credit check (checkCredits=false)
        loginTenantAdmin();
        givenAiCreditsExhausted();
        ObjectNode solution = solutionResponse(UUID.randomUUID(), "Energy Monitoring", false, false);
        given(tbAiClient.startNewSolution(any(TbAiClient.TokenProvider.class))).willReturn(success(solution));

        // WHEN
        JsonNode result = readResponse(doPost("/api/ai/solution/start").andExpect(status().isOk()), JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(solution);
        verify(tbAiClient).startNewSolution(any(TbAiClient.TokenProvider.class));
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
        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldReturnInternalServerErrorWithErrorMessage_whenAiClientReturnsFailureResponse() throws Exception {
        // GIVEN
        loginTenantAdmin();
        UUID solutionId = UUID.randomUUID();
        String error = "Solution service unavailable";
        given(tbAiClient.getSolution(eq(solutionId), any(TbAiClient.TokenProvider.class))).willReturn(failure(error));

        // WHEN
        doGet("/api/ai/solution/" + solutionId)
                .andExpect(status().isInternalServerError())
                .andExpect(statusReason(equalTo(error)));

        // THEN
        verify(tbAiClient).getSolution(eq(solutionId), any(TbAiClient.TokenProvider.class));
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
        verifyNoInteractions(tbAiClient);
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
        doPost("/api/ai/solution/start")
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
        doGet("/api/ai/solution/infos")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermissionRead + AI_RESOURCE)));

        // THEN
        verifyNoInteractions(tbAiClient);
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private TbAiClient.TokenProvider captureStartNewTokenProvider() {
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).startNewSolution(tokenProvider.capture());
        return tokenProvider.getValue();
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
