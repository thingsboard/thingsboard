// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.gen.agent.v1.AgentLogChunk;
import org.thingsboard.server.gen.agent.v1.AgentMetricsSync;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.log.AgentLogFanout;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionState;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentLogChunkMessageHandlerTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final String PROJECT_NAME = "my-project";
    private static final String UNIT_IDENTIFIER = "my-service";

    @Mock
    private AgentLogFanout logFanout;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private AgentSession session;
    @Mock
    private AgentSessionState sessionState;

    private AgentLogChunkMessageHandler handler;

    @BeforeEach
    void setUp() {
        when(sessionState.getTenantId()).thenReturn(TENANT_ID);
        when(sessionState.getAgentId()).thenReturn(AGENT_ID);
        when(session.getState()).thenReturn(sessionState);
        when(rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS, TENANT_ID)).thenReturn(true);
        when(rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS_PER_AGENT, TENANT_ID, AGENT_ID)).thenReturn(true);
        handler = new AgentLogChunkMessageHandler(logFanout, rateLimitService);
    }

    @Test
    void canHandleReturnsTrueOnLogChunk() {
        AgentToServer msg = AgentToServer.newBuilder()
                .setLogChunk(AgentLogChunk.newBuilder().setUnitId(UNIT_IDENTIFIER).setProjectName(PROJECT_NAME).build())
                .build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        assertThat(handler.canHandle(ctx)).isTrue();
    }

    @Test
    void canHandleReturnsFalseOnAgentMetricsSync() {
        AgentToServer msg = AgentToServer.newBuilder()
                .setAgentMetricsSync(AgentMetricsSync.newBuilder().build())
                .build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        assertThat(handler.canHandle(ctx)).isFalse();
    }

    @Test
    void handleDelegatesToFanout() {
        AgentLogChunk chunk = AgentLogChunk.newBuilder()
                .setUnitId(UNIT_IDENTIFIER).setProjectName(PROJECT_NAME)
                .addLines("hello").addLines("world")
                .setDropped(3)
                .build();
        AgentToServer msg = AgentToServer.newBuilder().setLogChunk(chunk).build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        handler.handle(ctx);

        ArgumentCaptor<AgentLogChunk> captor = ArgumentCaptor.forClass(AgentLogChunk.class);
        verify(logFanout).fanout(eq(sessionState), captor.capture());
        AgentLogChunk passed = captor.getValue();
        assertThat(passed.getUnitId()).isEqualTo(UNIT_IDENTIFIER);
        assertThat(passed.getProjectName()).isEqualTo(PROJECT_NAME);
        assertThat(passed.getLinesList()).containsExactly("hello", "world");
        assertThat(passed.getDropped()).isEqualTo(3);
    }

    @Test
    void handleDropsChunkWhenTenantRateLimited() {
        when(rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS, TENANT_ID)).thenReturn(false);
        AgentToServer msg = AgentToServer.newBuilder()
                .setLogChunk(AgentLogChunk.newBuilder().setUnitId(UNIT_IDENTIFIER).setProjectName(PROJECT_NAME).build())
                .build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        handler.handle(ctx);

        verifyNoInteractions(logFanout);
    }

    @Test
    void handleDropsChunkWhenPerAgentRateLimited() {
        when(rateLimitService.checkRateLimit(LimitedApi.AGENT_LOG_CHUNKS_PER_AGENT, TENANT_ID, AGENT_ID)).thenReturn(false);
        AgentToServer msg = AgentToServer.newBuilder()
                .setLogChunk(AgentLogChunk.newBuilder().setUnitId(UNIT_IDENTIFIER).setProjectName(PROJECT_NAME).build())
                .build();
        AgentInboundMsgCtx ctx = AgentInboundMsgCtx.builder().session(session).msg(msg).build();

        handler.handle(ctx);

        verifyNoInteractions(logFanout);
    }
}
