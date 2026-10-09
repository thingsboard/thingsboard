// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.ComposeState;
import org.thingsboard.server.gen.agent.v1.ContainerInfo;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.gen.agent.v1.VolumeInfo;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;
import org.thingsboard.server.service.agent.compose.AgentAppUnitKey;
import org.thingsboard.server.service.agent.compose.ComposeAgentAppCreator;
import org.thingsboard.server.service.agent.compose.ComposeApplicationMetricsRecorder;
import org.thingsboard.server.service.agent.compose.ComposeUnitMetricsRecorder;
import org.thingsboard.server.service.agent.compose.ComposeUnitStateWriter;
import org.thingsboard.server.service.agent.compose.ComposeUnitsSynchronizer;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeProjectSyncMessageHandler implements AgentInboundMessageHandler {

    private static final long AUTO_INSTALL_LOCK_TIMEOUT_MS = 100;

    private final AgentApplicationService appService;
    private final ComposeAgentAppCreator appCreator;
    private final ComposeUnitsSynchronizer unitsSynchronizer;
    private final ComposeUnitStateWriter unitStateWriter;
    private final ComposeUnitMetricsRecorder unitMetricsRecorder;
    private final ComposeApplicationMetricsRecorder appMetricsRecorder;
    private final AgentAppAutoInstallLockRegistry autoInstallLockRegistry;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        var msg = msgCtx.msg();
        return msg.hasProjectSync()
                && msg.getProjectSync().hasCompose()
                && !msg.getProjectSync().getRemoved();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        ProjectStateSync projectSync = msgCtx.msg().getProjectSync();
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();

        Lock readLock = autoInstallLockRegistry.forAgent(agentId).readLock();
        boolean acquired;
        try {
            acquired = readLock.tryLock(AUTO_INSTALL_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (!acquired) {
            log.debug("[{}][{}] Auto-install holds the lock, skipping compose sync for project [{}]; the agent re-reports it on the next sync",
                    tenantId, agentId, projectSync.getProjectName());
            return;
        }
        try {
            doHandle(tenantId, agentId, projectSync);
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process compose sync for project: {}", tenantId, agentId, projectSync.getProjectName(), e);
        } finally {
            readLock.unlock();
        }
    }

    private void doHandle(TenantId tenantId, AgentId agentId, ProjectStateSync projectSync) {
        String projectName = projectSync.getProjectName();
        ComposeState composeState = projectSync.getCompose();
        log.trace("[{}][{}] Processing composeState sync for project [{}], hasComposeJson: {}, containerStates: {}",
                tenantId, agentId, projectName, composeState.hasComposeJson(), composeState.getContainerStatesMap().keySet());

        JsonNode composeJson = composeState.hasComposeJson() ? jsonStringToJsonNode(composeState.getComposeJson()) : null;

        AgentApplication app = appService.findByProjectName(tenantId, agentId, projectName);
        if (app == null) {
            if (composeJson == null) {
                log.warn("[{}][{}] No agent application found for project [{}] and no compose provided, skipping",
                        tenantId, agentId, projectName);
                return;
            }
            app = getOrCreateApplication(tenantId, agentId, composeState, projectName);
            if (app == null) {
                log.info("[{}][{}] Could not resolve an application for reported project [{}]: no matching app template " +
                        "is registered yet, skipping", tenantId, agentId, projectName);
                return;
            }
        }

        Map<String, ContainerInfo> containerStates = composeState.getContainerStatesMap();
        Map<String, VolumeInfo> volumeStates = composeState.getVolumeStatesMap();

        Map<AgentAppUnitKey, AgentAppUnit> units;
        if (composeJson != null) {
            units = unitsSynchronizer.syncUnits(tenantId, app, composeJson);
        } else if (!containerStates.isEmpty() || !volumeStates.isEmpty()) {
            units = unitsSynchronizer.loadUnits(tenantId, app.getId());
        } else {
            units = Map.of();
        }

        unitStateWriter.writeStates(tenantId, units, containerStates);
        unitMetricsRecorder.record(tenantId, units, containerStates, volumeStates);
        if (composeState.hasApplicationMetrics()) {
            appMetricsRecorder.record(tenantId, app.getId(), composeState.getApplicationMetrics());
        }
    }

    private AgentApplication getOrCreateApplication(TenantId tenantId, AgentId agentId,
                                                    ComposeState externalCompose, String projectName) {
        Lock lock = autoInstallLockRegistry.forAppCreation(agentId);
        lock.lock();
        try {
            AgentApplication app = appService.findByProjectName(tenantId, agentId, projectName);
            if (app != null) {
                return app;
            }
            return appCreator.createApp(tenantId, agentId, projectName, externalCompose);
        } finally {
            lock.unlock();
        }
    }

    private JsonNode jsonStringToJsonNode(String json) {
        try {
            JsonNode node = JacksonUtil.OBJECT_MAPPER.readTree(json);
            return node != null && node.isObject() ? node : null;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Couldn't parse docker compose JSON", e);
        }
    }
}
