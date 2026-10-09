// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.action;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.dao.agent.AgentAppEventService;

@Component
@TbCoreComponent
@RequiredArgsConstructor
public class DeleteAppActionHandler implements AgentAppActionHandler {

    private final AgentAppEventService appEventService;

    @Override
    public AgentAppEventActionType getActionType() {
        return AgentAppEventActionType.DELETE;
    }

    @Override
    public void handle(AgentApplication application, AgentAppEventRequest request, AgentAppActionContext ctx) {
        appEventService.deleteAllPendingByApplicationId(application.getId());
        application.setPendingDeletion(true);
    }
}
