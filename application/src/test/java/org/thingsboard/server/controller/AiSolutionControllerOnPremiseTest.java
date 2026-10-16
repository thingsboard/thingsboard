// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.SolutionStepRequest;
import org.thingsboard.ai.common.channel.SolutionDataRequest;
import org.thingsboard.ai.common.channel.SolutionChatRequest;
import org.thingsboard.ai.common.channel.SolutionOperationRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.thingsboard.ai.common.data.solution.SolutionStep;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiSolutionControllerOnPremiseTest extends AbstractOnPremiseAiControllerTest {

    @Test
    public void shouldStartNewSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        givenOperation(ChannelProtocol.SOLUTION_START, null, success(JacksonUtil.newObjectNode()));

        // WHEN
        doPost("/api/ai/solution/start").andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_START, null);
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldGetSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        givenOperation(ChannelProtocol.SOLUTION_GET, new SolutionOperationRequest(solutionId), success(JacksonUtil.newObjectNode()));

        // WHEN
        doGet("/api/ai/solution/" + solutionId).andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_GET, new SolutionOperationRequest(solutionId));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldListSolutionInfosWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        givenOperation(ChannelProtocol.SOLUTION_LIST, null, success(JacksonUtil.newArrayNode()));

        // WHEN
        doGet("/api/ai/solution/infos").andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_LIST, null);
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldSendSolutionMessageWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        SolutionStep step = SolutionStep.INITIAL_CONFIGURATION;
        String message = "Add humidity monitoring to the solution.";
        givenOperation(ChannelProtocol.SOLUTION_CHAT, new SolutionChatRequest(solutionId, step, message), success(JacksonUtil.newObjectNode()));

        // WHEN
        performChat(solutionId, step, message).andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_CHAT, new SolutionChatRequest(solutionId, step, message));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldCreateSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        givenOperation(ChannelProtocol.SOLUTION_CREATE, new SolutionOperationRequest(solutionId), success(JacksonUtil.newObjectNode()));

        // WHEN
        doPost("/api/ai/solution/" + solutionId + "/create").andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_CREATE, new SolutionOperationRequest(solutionId));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldUpdateSolutionDataWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        String dataKey = "entities";
        ObjectNode value = JacksonUtil.newObjectNode();
        value.putArray("devices").add("Thermostat");
        givenOperation(ChannelProtocol.SOLUTION_DATA_UPDATE, new SolutionDataRequest(solutionId, dataKey, value), success(JacksonUtil.newObjectNode()));

        // WHEN
        doPut("/api/ai/solution/" + solutionId + "/" + dataKey, value).andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_DATA_UPDATE, new SolutionDataRequest(solutionId, dataKey, value));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldClearStepWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        SolutionStep step = SolutionStep.DASHBOARDS_CONFIGURATION;
        givenOperation(ChannelProtocol.SOLUTION_STEP_CLEAR, new SolutionStepRequest(solutionId, step), success());

        // WHEN
        doDelete("/api/ai/solution/" + solutionId + "/" + step + "/clear").andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_STEP_CLEAR, new SolutionStepRequest(solutionId, step));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldInstallSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        givenOperation(ChannelProtocol.SOLUTION_INSTALL, new SolutionOperationRequest(solutionId), success(JacksonUtil.newObjectNode()));

        // WHEN
        doPost("/api/ai/solution/" + solutionId + "/install").andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_INSTALL, new SolutionOperationRequest(solutionId));
        assertThat(context.tbAccessToken()).isEqualTo("Bearer " + token);
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldUninstallSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        givenOperation(ChannelProtocol.SOLUTION_UNINSTALL, new SolutionOperationRequest(solutionId), success(JacksonUtil.newObjectNode()));

        // WHEN
        doDelete("/api/ai/solution/" + solutionId + "/uninstall").andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_UNINSTALL, new SolutionOperationRequest(solutionId));
        assertThat(context.tbAccessToken()).isEqualTo("Bearer " + token);
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

    @Test
    public void shouldDeleteSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        givenOperation(ChannelProtocol.SOLUTION_DELETE, new SolutionOperationRequest(solutionId), success());

        // WHEN
        doDelete("/api/ai/solution/" + solutionId).andExpect(status().isOk());

        // THEN
        TbAiTurnContext context = verifyOperation(ChannelProtocol.SOLUTION_DELETE, new SolutionOperationRequest(solutionId));
        assertTokenProviderBelongsToTenantAdmin(context.tokenProvider());
    }

}
