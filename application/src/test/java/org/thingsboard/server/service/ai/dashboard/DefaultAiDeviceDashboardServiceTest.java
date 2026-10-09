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
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.security.model.SecurityUser;

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
    SecurityUser user;

    DefaultAiDeviceDashboardService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAiDeviceDashboardService(tbAiService);
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

        // WHEN
        JsonNode result = service.generateDashboard(deviceId, request, tbAccessToken, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCall().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().generateDashboard(eq(deviceId), same(request), eq(tbAccessToken), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TbAiService.TbAiCall<TbAiClient.TbAiResponse> captureProcessCall() {
        ArgumentCaptor<TbAiService.TbAiCall> callCaptor = ArgumentCaptor.forClass(TbAiService.TbAiCall.class);
        then(tbAiService).should().process(callCaptor.capture(), same(user));
        return callCaptor.getValue();
    }

    private static ObjectNode generateDashboardRequest(String... timeseriesKeys) {
        ObjectNode request = JacksonUtil.newObjectNode();
        for (String key : timeseriesKeys) {
            request.withArray("timeseriesKeys").add(key);
        }
        return request;
    }

}
