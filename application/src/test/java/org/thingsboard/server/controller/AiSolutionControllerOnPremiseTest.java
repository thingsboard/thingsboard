// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.data.solution.SolutionStep;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiSolutionControllerOnPremiseTest extends AbstractOnPremiseAiControllerTest {

    @Test
    public void shouldStartNewSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        given(tbAiClient.startNewSolution(any(TbAiClient.TokenProvider.class))).willReturn(success(JacksonUtil.newObjectNode()));

        // WHEN
        doPost("/api/ai/solution/start").andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).startNewSolution(tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldGetSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        given(tbAiClient.getSolution(eq(solutionId), any(TbAiClient.TokenProvider.class))).willReturn(success(JacksonUtil.newObjectNode()));

        // WHEN
        doGet("/api/ai/solution/" + solutionId).andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).getSolution(eq(solutionId), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldListSolutionInfosWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        given(tbAiClient.getSolutions(any(TbAiClient.TokenProvider.class))).willReturn(success(JacksonUtil.newArrayNode()));

        // WHEN
        doGet("/api/ai/solution/infos").andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).getSolutions(tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldSendSolutionMessageWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        SolutionStep step = SolutionStep.INITIAL_CONFIGURATION;
        String message = "Add humidity monitoring to the solution.";
        given(tbAiClient.sendSolutionMessage(eq(solutionId), eq(step), eq(message), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(JacksonUtil.newObjectNode()));

        // WHEN
        performChat(solutionId, step, message).andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).sendSolutionMessage(eq(solutionId), eq(step), eq(message), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldCreateSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        given(tbAiClient.createSolution(eq(solutionId), any(TbAiClient.TokenProvider.class))).willReturn(success(JacksonUtil.newObjectNode()));

        // WHEN
        doPost("/api/ai/solution/" + solutionId + "/create").andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).createSolution(eq(solutionId), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
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
        given(tbAiClient.updateData(eq(solutionId), eq(dataKey), eq(value), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(JacksonUtil.newObjectNode()));

        // WHEN
        doPut("/api/ai/solution/" + solutionId + "/" + dataKey, value).andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).updateData(eq(solutionId), eq(dataKey), eq(value), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldClearStepWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        SolutionStep step = SolutionStep.DASHBOARDS_CONFIGURATION;
        given(tbAiClient.clearStep(eq(solutionId), eq(step), any(TbAiClient.TokenProvider.class))).willReturn(success());

        // WHEN
        doDelete("/api/ai/solution/" + solutionId + "/" + step + "/clear").andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).clearStep(eq(solutionId), eq(step), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldInstallSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        given(tbAiClient.installSolution(eq(solutionId), eq("Bearer " + token), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(JacksonUtil.newObjectNode()));

        // WHEN
        doPost("/api/ai/solution/" + solutionId + "/install").andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).installSolution(eq(solutionId), eq("Bearer " + token), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldUninstallSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        given(tbAiClient.uninstallSolution(eq(solutionId), eq("Bearer " + token), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(JacksonUtil.newObjectNode()));

        // WHEN
        doDelete("/api/ai/solution/" + solutionId + "/uninstall").andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).uninstallSolution(eq(solutionId), eq("Bearer " + token), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

    @Test
    public void shouldDeleteSolutionWithOnPremiseToken_whenTenantAdmin() throws Exception {
        // GIVEN
        loginTenantAdmin();
        givenSubscriptionProvidesAiToken();
        UUID solutionId = UUID.randomUUID();
        given(tbAiClient.deleteSolution(eq(solutionId), any(TbAiClient.TokenProvider.class))).willReturn(success());

        // WHEN
        doDelete("/api/ai/solution/" + solutionId).andExpect(status().isOk());

        // THEN
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProvider = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).deleteSolution(eq(solutionId), tokenProvider.capture());
        assertTokenProviderBelongsToTenantAdmin(tokenProvider.getValue());
    }

}
