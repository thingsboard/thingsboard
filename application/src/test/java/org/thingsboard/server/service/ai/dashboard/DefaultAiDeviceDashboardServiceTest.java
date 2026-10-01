// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.DashboardOperationRequest;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiClientRequest;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.ai.transport.TbAiTurnContext;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Map;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DefaultAiDeviceDashboardServiceTest {

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

    DefaultAiDeviceDashboardService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAiDeviceDashboardService(tbAiService, operations);
    }

    @Test
    void shouldProcessWithCreditCheckAndDelegateToClient_whenGenerateDashboardCalled() {
        // GIVEN
        UUID deviceId = UUID.randomUUID();
        UUID dashboardId = UUID.randomUUID();
        JsonNode request = generateDashboardRequest("temperature", "humidity");
        String tbAccessToken = "tb-access-token";
        JsonNode expectedResponse = TextNode.valueOf(dashboardId.toString());
        given(tbAiService.process(any(), same(user))).willReturn(expectedResponse);
        given(tbAiClient.generateDashboard(eq(deviceId), same(request), eq(tbAccessToken), same(tokenProvider)))
                .willReturn(tbAiResponse);
        stubOperationsWithHttpCall();

        // WHEN
        JsonNode result = service.generateDashboard(deviceId, request, tbAccessToken, clientRequest, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCall().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().generateDashboard(eq(deviceId), same(request), eq(tbAccessToken), same(tokenProvider));
        TbAiTurnContext[] context = new TbAiTurnContext[1];
        TbAiOperation operation = captureOperation(context);
        assertThat(operation.type()).isEqualTo(ChannelProtocol.DASHBOARD_GENERATE);
        assertThat(operation.payload()).isEqualTo(new DashboardOperationRequest(deviceId, request));
        assertThat(context[0].clientRequest()).isSameAs(clientRequest);
        assertThat(context[0].tbAccessToken()).isEqualTo(tbAccessToken);
        assertThat(context[0].tokenProvider()).isSameAs(tokenProvider);
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TbAiService.TbAiCall<TbAiClient.TbAiResponse> captureProcessCall() {
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

    private static ObjectNode generateDashboardRequest(String... timeseriesKeys) {
        ObjectNode request = JacksonUtil.newObjectNode();
        for (String key : timeseriesKeys) {
            request.withArray("timeseriesKeys").add(key);
        }
        return request;
    }

}
