// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.gen.agent.v1.AgentToServer;
import org.thingsboard.server.gen.agent.v1.InitialSyncComplete;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.session.AgentSession;
import org.thingsboard.server.service.agent.session.AgentSessionState;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseAgentInboundMessageDispatcherTest {

    @Mock
    private AgentInboundMessageHandler throwingHandler;
    @Mock
    private AgentInboundMessageHandler otherHandler;
    @Mock
    private AgentSession session;

    private AgentInboundMsgCtx ctx() {
        lenient().when(session.getState()).thenReturn(new AgentSessionState());
        return AgentInboundMsgCtx.builder()
                .session(session)
                .msg(AgentToServer.newBuilder().setInitialSyncComplete(InitialSyncComplete.getDefaultInstance()).build())
                .build();
    }

    @Test
    void process_handlerThrows_isSwallowedToKeepStreamAlive() {
        when(throwingHandler.canHandle(any())).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(throwingHandler).handle(any());
        AgentInboundMsgCtx ctx = ctx();

        BaseAgentInboundMessageDispatcher dispatcher = new BaseAgentInboundMessageDispatcher(List.of(throwingHandler));

        assertThatCode(() -> dispatcher.process(ctx)).doesNotThrowAnyException();
        verify(throwingHandler).handle(ctx);
    }

    @Test
    void process_oneHandlerThrows_othersStillRun() {
        when(throwingHandler.canHandle(any())).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(throwingHandler).handle(any());
        when(otherHandler.canHandle(any())).thenReturn(true);
        AgentInboundMsgCtx ctx = ctx();

        BaseAgentInboundMessageDispatcher dispatcher =
                new BaseAgentInboundMessageDispatcher(List.of(throwingHandler, otherHandler));

        assertThatCode(() -> dispatcher.process(ctx)).doesNotThrowAnyException();
        verify(otherHandler).handle(ctx);
    }

    @Test
    void process_nonMatchingHandler_notInvoked() {
        when(otherHandler.canHandle(any())).thenReturn(false);
        AgentInboundMsgCtx ctx = ctx();

        BaseAgentInboundMessageDispatcher dispatcher = new BaseAgentInboundMessageDispatcher(List.of(otherHandler));

        dispatcher.process(ctx);

        verify(otherHandler, never()).handle(any());
    }
}
