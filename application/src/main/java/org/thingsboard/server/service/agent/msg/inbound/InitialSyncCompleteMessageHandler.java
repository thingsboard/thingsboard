// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.agent.AgentAutoInstallService;
import org.thingsboard.server.service.agent.AgentContextComponent;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class InitialSyncCompleteMessageHandler implements AgentInboundMessageHandler {

    private final AgentAutoInstallService autoInstallService;
    private final AgentContextComponent agentCtx;
    private final AgentAppAutoInstallLockRegistry autoInstallLockRegistry;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        return msgCtx.msg().hasInitialSyncComplete();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();
        log.debug("[{}][{}] Received InitialSyncComplete, scheduling auto-install", tenantId, agentId);
        try {
            agentCtx.getAgentEventExecutor().execute(() -> runAutoInstall(tenantId, agentId));
        } catch (RejectedExecutionException e) {
            log.warn("[{}][{}] Failed to schedule auto-install: {}", tenantId, agentId, e.getMessage());
        }
    }

    private void runAutoInstall(TenantId tenantId, AgentId agentId) {
        Lock writeLock = autoInstallLockRegistry.forAgent(agentId).writeLock();
        writeLock.lock();
        try {
            autoInstallService.autoInstall(tenantId, agentId);
        } catch (Exception e) {
            log.error("[{}][{}] Auto-install failed", tenantId, agentId, e);
        } finally {
            writeLock.unlock();
        }
    }
}
