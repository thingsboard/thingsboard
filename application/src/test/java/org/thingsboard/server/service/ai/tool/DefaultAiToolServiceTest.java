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
import org.thingsboard.ai.common.channel.ChannelFrame;
import org.thingsboard.ai.common.channel.ChannelProtocol;
import org.thingsboard.ai.common.channel.ChannelSession;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.service.ai.TbAiService;
import org.thingsboard.server.service.ai.transport.TbAiChannelRegistry;
import org.thingsboard.server.service.ai.transport.TbAiOperation;
import org.thingsboard.server.service.ai.transport.TbAiOperations;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DefaultAiToolServiceTest {

    @Mock
    TbAiService tbAiService;
    @Mock
    TbAiClient.TokenProvider tokenProvider;
    @Mock
    TbAiClient.TbAiResponse tbAiResponse;
    @Mock
    SecurityUser user;
    @Mock
    TbAiChannelRegistry channelRegistry;
    @Mock
    TbAiOperations operations;

    DefaultAiToolService service;

    @BeforeEach
    void setUp() {
        service = new DefaultAiToolService(tbAiService, channelRegistry, operations);
    }

    @Test
    void shouldResolveThroughOperation_whenApprovalIsNotOnThisNode() {
        // GIVEN
        JsonNode decision = toolApprovalDecision(UUID.randomUUID(), true);
        JsonNode expectedResponse = TextNode.valueOf("APPROVED");
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);
        given(operations.execute(any(TbAiOperation.class), same(user), same(tokenProvider))).willReturn(tbAiResponse);

        // WHEN
        JsonNode result = service.resolveToolApproval(decision, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);

        assertThat(captureProcessCall().apply(tokenProvider)).isSameAs(tbAiResponse);
        then(operations).should().execute(argThat(operation -> ChannelProtocol.TOOL_APPROVAL_RESOLVE.equals(operation.type())
                && operation.payload() == decision), same(user), same(tokenProvider));
        then(tbAiService).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldResolveOverChannel_whenThisNodeHoldsThePendingApproval() {
        // GIVEN
        UUID executionId = UUID.randomUUID();
        JsonNode decision = toolApprovalDecision(executionId, true);
        ChannelSession session = mock(ChannelSession.class);
        given(channelRegistry.findApprovalRoute(executionId)).willReturn(Optional.of(new TbAiChannelRegistry.ApprovalRoute(session, "op-1")));
        given(session.request(any(), any())).willAnswer(invocation -> {
            ChannelFrame request = invocation.getArgument(0);
            return CompletableFuture.completedFuture(ChannelFrame.reply(ChannelProtocol.TOOL_APPROVAL_RESULT, request, "\"APPROVED\""));
        });

        // WHEN
        JsonNode result = service.resolveToolApproval(decision, user);

        // THEN
        assertThat(result).isEqualTo(TextNode.valueOf("APPROVED"));
        ArgumentCaptor<ChannelFrame> frame = ArgumentCaptor.forClass(ChannelFrame.class);
        then(session).should().request(frame.capture(), any());
        assertThat(frame.getValue().type()).isEqualTo(ChannelProtocol.TOOL_APPROVAL);
        assertThat(frame.getValue().op()).isEqualTo("op-1");
        assertThat(JacksonUtil.toJsonNode(frame.getValue().payload())).isEqualTo(decision);
        then(tbAiService).shouldHaveNoInteractions();
    }

    @Test
    void shouldFallBackToHttp_whenChannelApprovalFails() {
        // GIVEN
        UUID executionId = UUID.randomUUID();
        JsonNode decision = toolApprovalDecision(executionId, false);
        ChannelSession session = mock(ChannelSession.class);
        given(channelRegistry.findApprovalRoute(executionId)).willReturn(Optional.of(new TbAiChannelRegistry.ApprovalRoute(session, null)));
        given(session.request(any(), any())).willReturn(CompletableFuture.failedFuture(new IllegalStateException("closed")));
        JsonNode expectedResponse = TextNode.valueOf("DENIED");
        given(tbAiService.process(any(), same(user), eq(false))).willReturn(expectedResponse);

        // WHEN
        JsonNode result = service.resolveToolApproval(decision, user);

        // THEN
        assertThat(result).isSameAs(expectedResponse);
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
