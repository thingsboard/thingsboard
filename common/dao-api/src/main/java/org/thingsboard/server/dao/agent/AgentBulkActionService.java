// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface AgentBulkActionService extends EntityDaoService {

    AgentBulkAction save(TenantId tenantId, AgentBulkAction bulkAction);

    AgentBulkAction findById(TenantId tenantId, AgentBulkActionId id);

    PageData<AgentBulkAction> findByAgentProfileId(TenantId tenantId, AgentProfileId agentProfileId, PageLink pageLink);

    PageData<AgentBulkAction> findByAgentProfileIdAndApplicationProfileId(TenantId tenantId,
                                                                         AgentProfileId agentProfileId,
                                                                         AgentAppProfileId applicationProfileId,
                                                                         PageLink pageLink);

    PageData<AgentBulkAction> findStuckBulkActions(long threshold, PageLink pageLink);

    int cleanUpExpiredBulkActions(long expirationTs, int batchSize);

    int failIfStillStuck(AgentBulkActionId id, AgentBulkActionStatus status, String errorMsg);
}
