// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.subscription.SubEventObserver;
import org.thingsboard.server.service.subscription.TbAgentUnitRemoteSubsInfo;
import org.thingsboard.server.service.subscription.TbAgentUnitRemoteSubsInfo.TbAgentUnitSubsUpdateInfo;
import org.thingsboard.server.service.subscription.TbEntityRemoteSubsInfo;
import org.thingsboard.server.service.subscription.TbEntityRemoteSubsInfo.TbEntitySubsUpdateInfo;
import org.thingsboard.server.service.subscription.TbEntitySubEvent;

import java.util.function.Supplier;

@Slf4j
@TbCoreComponent
@Component
@RequiredArgsConstructor
public class AgentLogSubEventCoordinator implements SubEventObserver {

    private final AgentAppUnitService unitService;
    private final TbClusterService tbClusterService;

    @Override
    public EntityType entityType() {
        return EntityType.AGENT_APP_UNIT;
    }

    @Override
    public TbEntityRemoteSubsInfo createSubsInfo(TenantId tenantId, EntityId entityId) {
        return new TbAgentUnitRemoteSubsInfo(tenantId, entityId);
    }

    @Override
    public void onSubEvent(TbEntitySubEvent event, TbEntitySubsUpdateInfo subsUpdateInfo,
                           Supplier<TbEntityRemoteSubsInfo> committedSubsInfo) {
        onLogSubsChange(event.getTenantId(), event.getEntityId(), subsUpdateInfo, committedSubsInfo);
    }

    @Override
    public void onSubsRemoved(TenantId tenantId, EntityId entityId, TbEntitySubsUpdateInfo subsUpdateInfo,
                              Supplier<TbEntityRemoteSubsInfo> committedSubsInfo) {
        onLogSubsChange(tenantId, entityId, subsUpdateInfo, committedSubsInfo);
    }

    private void onLogSubsChange(TenantId tenantId, EntityId entityId, TbEntitySubsUpdateInfo subsUpdateInfo,
                                 Supplier<TbEntityRemoteSubsInfo> committedSubsInfo) {
        if (!(subsUpdateInfo instanceof TbAgentUnitSubsUpdateInfo updInfo)) {
            log.warn("[{}] Expected TbAgentUnitSubsUpdateInfo but got {}",
                    entityId, subsUpdateInfo.getClass().getSimpleName());
            return;
        }
        if (updInfo.isDuplicate()) {
            return;
        }
        boolean isEmptyLogSubsBeforeEvent = updInfo.isEmptyLogSubsBeforeEvent();
        boolean isEmptyLogSubsAfterEvent = updInfo.isEmptyLogSubsAfterEvent();
        boolean firstSub = isEmptyLogSubsBeforeEvent && !isEmptyLogSubsAfterEvent;
        boolean lastSub = !isEmptyLogSubsBeforeEvent && isEmptyLogSubsAfterEvent;
        if (!firstSub && !lastSub) {
            return;
        }
        AgentAppUnitId unitId = (AgentAppUnitId) entityId;
        AgentAppUnitInfo info = unitService.findAgentAppUnitInfoById(tenantId, unitId);
        if (info == null || info.getAgentId() == null) {
            log.warn("[{}] Cannot resolve unit info for {}; dropping log stream {}",
                    tenantId, unitId, lastSub ? "Stop" : "Start");
            return;
        }
        sendLogStreamRequest(tenantId, info, lastSub);
        correctIfLogSubsChanged(tenantId, info, lastSub, committedSubsInfo);
    }

    /**
     * The transition above was computed under the subscriptions lock, but this observer runs after the lock is
     * released, so a concurrent event may have already moved the log subscriptions the other way. Re-reads the
     * committed state and, if it disagrees with the request just sent, sends the opposite one — so the last
     * request on the wire always matches the committed state, whichever order the two arrive in at the agent.
     * The correction duplicates the request the concurrent event already sent; that is harmless only because
     * Start on a running stream and Stop on an absent one are both no-ops on the agent.
     */
    private void correctIfLogSubsChanged(TenantId tenantId, AgentAppUnitInfo info, boolean stopSent,
                                         Supplier<TbEntityRemoteSubsInfo> committedSubsInfo) {
        boolean hasLogSubs = committedSubsInfo.get() instanceof TbAgentUnitRemoteSubsInfo unitSubsInfo
                && unitSubsInfo.coversLogs();
        if (hasLogSubs != stopSent) {
            return;
        }
        log.info("[{}][{}] Log subscriptions are {} after sending {}; sending {}", tenantId, info.getId(),
                hasLogSubs ? "present" : "gone", stopSent ? "Stop" : "Start", stopSent ? "Start" : "Stop");
        sendLogStreamRequest(tenantId, info, !stopSent);
    }

    private void sendLogStreamRequest(TenantId tenantId, AgentAppUnitInfo info, boolean stop) {
        if (stop) {
            tbClusterService.stopAgentLogStream(tenantId, info);
        } else {
            tbClusterService.startAgentLogStream(tenantId, info);
        }
    }
}
