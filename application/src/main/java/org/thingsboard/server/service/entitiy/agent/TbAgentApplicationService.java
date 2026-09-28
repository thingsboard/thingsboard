// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.agent;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.agent.AgentAppEvent;
import org.thingsboard.server.common.data.agent.AgentAppEventRequest;
import org.thingsboard.server.common.data.agent.AgentAppInstallResponse;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.id.AgentAppEventId;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public interface TbAgentApplicationService {

    String EVENT_IN_PROGRESS_ERROR_MSG = "Cannot create event while another event is being processed. Cancel it and try again!";

    AgentApplication update(AgentApplication application, User user) throws Exception;

    AgentAppInstallResponse install(TenantId tenantId, AgentAppEventRequest request, User user) throws Exception;

    AgentAppEvent execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request) throws Exception;

    AgentAppEvent execActionEvent(TenantId tenantId, AgentApplicationId applicationId, AgentAppEventRequest request, boolean skipActiveEventCheck) throws Exception;

    void cancelEvent(TenantId tenantId, AgentAppEventId eventId) throws Exception;

    AgentAppEvent upgradeAgent(TenantId tenantId, AgentId agentId, String imageRef) throws Exception;

    AgentApplication mergeForPreview(TenantId tenantId, AgentApplication application, AppConfigMergeCtx ctx);

    AgentApplication assignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId, EntityId relatedEntityId, User user);

    AgentApplication unassignRelatedEntity(TenantId tenantId, AgentApplicationId agentApplicationId, User user);
}
