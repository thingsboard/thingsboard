// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.tool;

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
class DefaultAiToolServiceTest {

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

    DefaultAiToolService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAiToolService(tbAiService);
    }

    @Test
    void shouldProcessWithoutCreditCheckAndDelegateToClient_whenResolveToolApprovalCalled() {
        // GIVEN
        JsonNode decision = toolApprovalDecision(UUID.randomUUID(), true);
        JsonNode expectedResponse = TextNode.valueOf("APPROVED");
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(tbAiClient.resolveToolApproval(same(decision), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.resolveToolApproval(decision, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCall().apply(tbAiClient, tokenProvider)).isSameAs(tbAiResponse);
        then(tbAiClient).should().resolveToolApproval(same(decision), same(tokenProvider));
        then(tbAiClient).shouldHaveNoMoreInteractions();
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private TbAiService.TbAiCall<TbAiClient.TbAiResponse> captureProcessCall() {
        ArgumentCaptor<TbAiService.TbAiCall> callCaptor = ArgumentCaptor.forClass(TbAiService.TbAiCall.class);
        then(tbAiService).should().process(callCaptor.capture(), same(user), eq(false));
        return callCaptor.getValue();
    }

    private static ObjectNode toolApprovalDecision(UUID executionId, boolean approved) {
        return JacksonUtil.newObjectNode()
                .put("executionId", executionId.toString())
                .put("approved", approved);
    }

}
