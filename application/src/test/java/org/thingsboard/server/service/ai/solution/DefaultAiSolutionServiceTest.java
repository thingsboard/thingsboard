// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.solution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.SolutionChatRequest;
import org.thingsboard.ai.common.channel.SolutionDataRequest;
import org.thingsboard.ai.common.channel.SolutionOperationRequest;
import org.thingsboard.ai.common.channel.SolutionStepRequest;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.ai.common.data.solution.SolutionStep;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiClientRequest;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class DefaultAiSolutionServiceTest {

    @Mock
    TbAiService tbAiService;
    @Mock
    TbAiClient tbAiClient;
    @Mock
    TbAiClient.TokenProvider tokenProvider;
    @Mock
    TbAiClient.TbAiResponse tbAiResponse;
    @Mock
    TbAiOperations operations;
    @Mock
    SecurityUser user;

    TbAiClientRequest clientRequest = new TbAiClientRequest("https://tb.example.com", Map.of());

    DefaultAiSolutionService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAiSolutionService(tbAiService, operations);
        lenient().when(operations.execute(any(TbAiOperation.class), any(SecurityUser.class), any(TbAiClient.TokenProvider.class)))
                .thenAnswer(invocation -> invocation.<TbAiOperation>getArgument(0).httpCall().get());
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenStartNewCalled() {
        // GIVEN
        JsonNode expectedResponse = solutionResponse(UUID.randomUUID(), "Energy Monitoring", false, false);
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(tbAiClient.startNewSolution(same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.startNew(user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCallWithoutCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().startNewSolution(same(tokenProvider));
        then(operations).should().execute(argThat(operation -> ChannelProtocol.SOLUTION_START.equals(operation.type())
                && Objects.equals(null, operation.payload())), same(user), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenGetSolutionCalled() {
        // GIVEN
        UUID solutionId = UUID.randomUUID();
        JsonNode expectedResponse = solutionResponse(solutionId, "Energy Monitoring", false, false);
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(tbAiClient.getSolution(eq(solutionId), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.getSolution(solutionId, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCallWithoutCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().getSolution(eq(solutionId), same(tokenProvider));
        then(operations).should().execute(argThat(operation -> ChannelProtocol.SOLUTION_GET.equals(operation.type())
                && Objects.equals(new SolutionOperationRequest(solutionId), operation.payload())), same(user), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenGetSolutionsCalled() {
        // GIVEN
        JsonNode expectedResponse = solutionInfosResponse(UUID.randomUUID(), "Energy Monitoring");
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(tbAiClient.getSolutions(same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.getSolutions(user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCallWithoutCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().getSolutions(same(tokenProvider));
        then(operations).should().execute(argThat(operation -> ChannelProtocol.SOLUTION_LIST.equals(operation.type())
                && Objects.equals(null, operation.payload())), same(user), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @ParameterizedTest
    @EnumSource(SolutionStep.class)
    void shouldProcessWithCreditCheckAndDelegateToClient_whenChatCalled(SolutionStep step) {
        // GIVEN
        UUID solutionId = UUID.randomUUID();
        String message = "Add humidity monitoring to the solution.";
        JsonNode expectedResponse = solutionResponse(solutionId, "Energy Monitoring", false, false);
        given(tbAiService.process(any(), same(user))).willReturn(expectedResponse);
        given(tbAiClient.sendSolutionMessage(eq(solutionId), eq(step), eq(message), same(tokenProvider)))
                .willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.chat(solutionId, step, message, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCallWithCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().sendSolutionMessage(eq(solutionId), eq(step), eq(message), same(tokenProvider));
        then(operations).should().execute(argThat(operation -> ChannelProtocol.SOLUTION_CHAT.equals(operation.type())
                && Objects.equals(new SolutionChatRequest(solutionId, step, message), operation.payload())), same(user), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithCreditCheckAndDelegateToClient_whenCreateSolutionCalled() {
        // GIVEN
        UUID solutionId = UUID.randomUUID();
        JsonNode expectedResponse = solutionResponse(solutionId, "Energy Monitoring", true, false);
        given(tbAiService.process(any(), same(user))).willReturn(expectedResponse);
        given(tbAiClient.createSolution(eq(solutionId), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.createSolution(solutionId, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCallWithCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().createSolution(eq(solutionId), same(tokenProvider));
        then(operations).should().execute(argThat(operation -> ChannelProtocol.SOLUTION_CREATE.equals(operation.type())
                && Objects.equals(new SolutionOperationRequest(solutionId), operation.payload())), same(user), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithCreditCheckAndDelegateToClient_whenUpdateDataCalled() {
        // GIVEN
        UUID solutionId = UUID.randomUUID();
        String dataKey = "solutionTitle";
        JsonNode value = TextNode.valueOf("Energy Monitoring");
        JsonNode expectedResponse = solutionResponse(solutionId, "Energy Monitoring", false, false);
        given(tbAiService.process(any(), same(user))).willReturn(expectedResponse);
        given(tbAiClient.updateData(eq(solutionId), eq(dataKey), same(value), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.updateData(solutionId, dataKey, value, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCallWithCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().updateData(eq(solutionId), eq(dataKey), same(value), same(tokenProvider));
        then(operations).should().execute(argThat(operation -> ChannelProtocol.SOLUTION_DATA_UPDATE.equals(operation.type())
                && Objects.equals(new SolutionDataRequest(solutionId, dataKey, value), operation.payload())), same(user), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @ParameterizedTest
    @EnumSource(SolutionStep.class)
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenClearStepCalled(SolutionStep step) {
        // GIVEN
        UUID solutionId = UUID.randomUUID();
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(null);
        given(tbAiClient.clearStep(eq(solutionId), eq(step), same(tokenProvider)))
                .willReturn(tbAiResponse);

        // WHEN
        service.clearStep(solutionId, step, user);

        // THEN
        assertThat(captureProcessCallWithoutCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().clearStep(eq(solutionId), eq(step), same(tokenProvider));
        then(operations).should().execute(argThat(operation -> ChannelProtocol.SOLUTION_STEP_CLEAR.equals(operation.type())
                && Objects.equals(new SolutionStepRequest(solutionId, step), operation.payload())), same(user), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenInstallSolutionCalled() {
        // GIVEN
        UUID solutionId = UUID.randomUUID();
        String tbAccessToken = "tb-access-token";
        JsonNode expectedResponse = solutionInstallResultResponse(UUID.randomUUID());
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(tbAiClient.installSolution(eq(solutionId), eq(tbAccessToken), same(tokenProvider))).willReturn(tbAiResponse);
        stubOperationsWithHttpCall();

        // WHEN
        JsonNode result = service.installSolution(solutionId, tbAccessToken, clientRequest, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCallWithoutCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().installSolution(eq(solutionId), eq(tbAccessToken), same(tokenProvider));
        TbAiTurnContext[] context = new TbAiTurnContext[1];
        TbAiOperation operation = captureOperation(context);
        assertThat(operation.type()).isEqualTo(ChannelProtocol.SOLUTION_INSTALL);
        assertThat(operation.payload()).isEqualTo(new SolutionOperationRequest(solutionId));
        assertThat(context[0].clientRequest()).isSameAs(clientRequest);
        assertThat(context[0].tbAccessToken()).isEqualTo(tbAccessToken);
        assertThat(context[0].tokenProvider()).isSameAs(tokenProvider);
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenUninstallSolutionCalled() {
        // GIVEN
        UUID solutionId = UUID.randomUUID();
        String tbAccessToken = "tb-access-token";
        JsonNode expectedResponse = solutionResponse(solutionId, "Energy Monitoring", true, false);
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(tbAiClient.uninstallSolution(eq(solutionId), eq(tbAccessToken), same(tokenProvider))).willReturn(tbAiResponse);
        stubOperationsWithHttpCall();

        // WHEN
        JsonNode result = service.uninstallSolution(solutionId, tbAccessToken, clientRequest, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCallWithoutCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().uninstallSolution(eq(solutionId), eq(tbAccessToken), same(tokenProvider));
        TbAiTurnContext[] context = new TbAiTurnContext[1];
        TbAiOperation operation = captureOperation(context);
        assertThat(operation.type()).isEqualTo(ChannelProtocol.SOLUTION_UNINSTALL);
        assertThat(operation.payload()).isEqualTo(new SolutionOperationRequest(solutionId));
        assertThat(context[0].clientRequest()).isSameAs(clientRequest);
        assertThat(context[0].tbAccessToken()).isEqualTo(tbAccessToken);
        assertThat(context[0].tokenProvider()).isSameAs(tokenProvider);
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenDeleteSolutionCalled() {
        // GIVEN
        UUID solutionId = UUID.randomUUID();
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(null);
        given(tbAiClient.deleteSolution(eq(solutionId), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        service.deleteSolution(solutionId, user);

        // THEN
        assertThat(captureProcessCallWithoutCreditCheck().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().deleteSolution(eq(solutionId), same(tokenProvider));
        then(operations).should().execute(argThat(operation -> ChannelProtocol.SOLUTION_DELETE.equals(operation.type())
                && Objects.equals(new SolutionOperationRequest(solutionId), operation.payload())), same(user), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TbAiService.TbAiCall<TbAiClient.TbAiResponse> captureProcessCallWithoutCreditCheck() {
        ArgumentCaptor<TbAiService.TbAiCall> callCaptor = ArgumentCaptor.forClass(TbAiService.TbAiCall.class);
        then(tbAiService).should().process(callCaptor.capture(), same(user), eq(false));
        return callCaptor.getValue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TbAiService.TbAiCall<TbAiClient.TbAiResponse> captureProcessCallWithCreditCheck() {
        ArgumentCaptor<TbAiService.TbAiCall> callCaptor = ArgumentCaptor.forClass(TbAiService.TbAiCall.class);
        then(tbAiService).should().process(callCaptor.capture(), same(user));
        return callCaptor.getValue();
    }

    private void stubOperationsWithHttpCall() {
        given(operations.execute(any(), any())).willAnswer(invocation -> invocation.<TbAiOperation>getArgument(0).httpCall().get());
    }

    private TbAiOperation captureOperation(TbAiTurnContext[] context) {
        ArgumentCaptor<TbAiOperation> operationCaptor = ArgumentCaptor.forClass(TbAiOperation.class);
        ArgumentCaptor<TbAiTurnContext> contextCaptor = ArgumentCaptor.forClass(TbAiTurnContext.class);
        then(operations).should().execute(operationCaptor.capture(), contextCaptor.capture());
        context[0] = contextCaptor.getValue();
        return operationCaptor.getValue();
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

    private static ObjectNode chatMessage(String from, String content) {
        return JacksonUtil.newObjectNode()
                .put("from", from)
                .put("content", content);
    }

}
