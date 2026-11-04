// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.dao.agent.AgentApplicationService;
import org.thingsboard.server.gen.agent.v1.ProjectStateSync;
import org.thingsboard.server.service.agent.AgentInboundMsgCtx;

@Component
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class ComposeProjectRemovedMessageHandler implements AgentInboundMessageHandler {

    private final AgentAppUnitService agentAppUnitService;
    private final AgentApplicationService appService;

    @Override
    public boolean canHandle(AgentInboundMsgCtx msgCtx) {
        var msg = msgCtx.msg();
        return msg.hasProjectSync() && msg.getProjectSync().getRemoved();
    }

    @Override
    public void handle(AgentInboundMsgCtx msgCtx) {
        TenantId tenantId = msgCtx.sessionState().getTenantId();
        AgentId agentId = msgCtx.sessionState().getAgentId();
        ProjectStateSync projectSync = msgCtx.msg().getProjectSync();

        try {
            AgentApplication app = appService.findByProjectName(tenantId, agentId, projectSync.getProjectName());
            if (app == null) {
                return;
            }
            log.trace("[{}][{}] Processing project removal [{}]", tenantId, agentId, projectSync.getProjectName());
            agentAppUnitService.deleteByAgentApplicationId(tenantId, app.getId());
        } catch (Exception e) {
            log.error("[{}][{}] Failed to process project removal for project: {}", tenantId, agentId, projectSync.getProjectName(), e);
        }
    }
}
