// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.service.subscription.TbAgentUnitRemoteSubsInfo;
import org.thingsboard.server.service.subscription.TbAgentUnitRemoteSubsInfo.TbAgentUnitSubsUpdateInfo;
import org.thingsboard.server.service.subscription.TbEntityRemoteSubsInfo;
import org.thingsboard.server.service.subscription.TbEntitySubEvent;
import org.thingsboard.server.service.subscription.TbSubscriptionsInfo;

import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentLogSubEventCoordinatorTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.randomUUID());
    private static final AgentAppUnitId UNIT_ID = new AgentAppUnitId(UUID.randomUUID());
    private static final AgentId AGENT_ID = new AgentId(UUID.randomUUID());
    private static final String PROJECT_NAME = "my-project";
    private static final String UNIT_IDENTIFIER = "my-service";

    @Mock
    private AgentAppUnitService unitService;
    @Mock
    private TbClusterService tbClusterService;

    @InjectMocks
    private AgentLogSubEventCoordinator coordinator;

    @Test
    void firstSubProducesStartRequest() {
        AgentAppUnitInfo info = unitInfo(UNIT_IDENTIFIER, PROJECT_NAME, AGENT_ID);
        when(unitService.findAgentAppUnitInfoById(TENANT_ID, UNIT_ID)).thenReturn(info);

        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(false, false, true, false),
                committedWithLogSubs());

        verify(tbClusterService).startAgentLogStream(TENANT_ID, info);
        verifyNoMoreInteractions(tbClusterService);
    }

    @Test
    void lastSubEmitsStop() {
        AgentAppUnitInfo info = unitInfo(UNIT_IDENTIFIER, PROJECT_NAME, AGENT_ID);
        when(unitService.findAgentAppUnitInfoById(TENANT_ID, UNIT_ID)).thenReturn(info);

        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.DELETED), updInfo(false, true, false, true),
                committedWithoutLogSubs());

        verify(tbClusterService).stopAgentLogStream(TENANT_ID, info);
        verifyNoMoreInteractions(tbClusterService);
    }

    @Test
    void startIsFollowedByStopWhenLogSubsAreAlreadyGone() {
        AgentAppUnitInfo info = unitInfo(UNIT_IDENTIFIER, PROJECT_NAME, AGENT_ID);
        when(unitService.findAgentAppUnitInfoById(TENANT_ID, UNIT_ID)).thenReturn(info);

        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(false, false, true, false),
                committedWithoutLogSubs());

        InOrder inOrder = inOrder(tbClusterService);
        inOrder.verify(tbClusterService).startAgentLogStream(TENANT_ID, info);
        inOrder.verify(tbClusterService).stopAgentLogStream(TENANT_ID, info);
        verifyNoMoreInteractions(tbClusterService);
    }

    @Test
    void stopIsFollowedByStartWhenLogSubsAreAlreadyBack() {
        AgentAppUnitInfo info = unitInfo(UNIT_IDENTIFIER, PROJECT_NAME, AGENT_ID);
        when(unitService.findAgentAppUnitInfoById(TENANT_ID, UNIT_ID)).thenReturn(info);

        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.DELETED), updInfo(false, true, false, true),
                committedWithLogSubs());

        InOrder inOrder = inOrder(tbClusterService);
        inOrder.verify(tbClusterService).stopAgentLogStream(TENANT_ID, info);
        inOrder.verify(tbClusterService).startAgentLogStream(TENANT_ID, info);
        verifyNoMoreInteractions(tbClusterService);
    }

    @Test
    void committedSubsWithoutLogsDoNotKeepTheStreamAlive() {
        AgentAppUnitInfo info = unitInfo(UNIT_IDENTIFIER, PROJECT_NAME, AGENT_ID);
        when(unitService.findAgentAppUnitInfoById(TENANT_ID, UNIT_ID)).thenReturn(info);

        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.DELETED), updInfo(false, true, false, true),
                committedWithSubsButNoLogs());

        verify(tbClusterService).stopAgentLogStream(TENANT_ID, info);
        verifyNoMoreInteractions(tbClusterService);
    }

    @Test
    void noTransitionDoesNothing() {
        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(false, false, false, false),
                committedWithoutLogSubs());

        verifyNoInteractions(tbClusterService);
    }

    @Test
    void stillEmptyAfterEventDoesNothing() {
        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(false, false, true, true),
                committedWithoutLogSubs());

        verifyNoInteractions(tbClusterService);
    }

    @Test
    void duplicateEventIgnored() {
        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(true, false, true, false),
                committedWithoutLogSubs());

        verifyNoInteractions(tbClusterService);
    }

    @Test
    void unitNotResolvableSilentlyDropped() {
        when(unitService.findAgentAppUnitInfoById(TENANT_ID, UNIT_ID)).thenReturn(null);

        coordinator.onSubEvent(subEvent(ComponentLifecycleEvent.CREATED), updInfo(false, false, true, false),
                committedWithLogSubs());

        verify(tbClusterService, never()).startAgentLogStream(any(), any());
        verify(tbClusterService, never()).stopAgentLogStream(any(), any());
    }

    private TbEntitySubEvent subEvent(ComponentLifecycleEvent type) {
        return TbEntitySubEvent.builder()
                .tenantId(TENANT_ID)
                .entityId(UNIT_ID)
                .type(type)
                .info(new TbSubscriptionsInfo(false, false, false,false, null, false, null, 1))
                .seqNumber(1)
                .build();
    }

    private Supplier<TbEntityRemoteSubsInfo> committedWithLogSubs() {
        TbAgentUnitRemoteSubsInfo subsInfo = new TbAgentUnitRemoteSubsInfo(TENANT_ID, UNIT_ID);
        subsInfo.getSubs().put("tb-core-1", subsInfoWithLogs(true));
        return () -> subsInfo;
    }

    private Supplier<TbEntityRemoteSubsInfo> committedWithSubsButNoLogs() {
        TbAgentUnitRemoteSubsInfo subsInfo = new TbAgentUnitRemoteSubsInfo(TENANT_ID, UNIT_ID);
        subsInfo.getSubs().put("tb-core-1", subsInfoWithLogs(false));
        return () -> subsInfo;
    }

    private Supplier<TbEntityRemoteSubsInfo> committedWithoutLogSubs() {
        return () -> null;
    }

    private TbSubscriptionsInfo subsInfoWithLogs(boolean logs) {
        return new TbSubscriptionsInfo(false, false, logs, false, null, false, null, 1);
    }

    private TbAgentUnitSubsUpdateInfo updInfo(boolean duplicate, boolean empty,
                                              boolean emptyLogSubsBeforeEvent, boolean emptyLogSubsAfterEvent) {
        return new TbAgentUnitSubsUpdateInfo(duplicate, empty, emptyLogSubsBeforeEvent, emptyLogSubsAfterEvent);
    }

    private AgentAppUnitInfo unitInfo(String identifier, String projectName, AgentId agentId) {
        AgentAppUnit unit = new AgentAppUnit();
        unit.setIdentifier(identifier);
        return new AgentAppUnitInfo(unit, agentId, projectName);
    }
}
