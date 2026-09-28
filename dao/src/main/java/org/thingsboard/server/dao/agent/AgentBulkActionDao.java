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
import org.thingsboard.server.dao.Dao;

public interface AgentBulkActionDao extends Dao<AgentBulkAction> {

    int cleanUpExpiredBulkActions(long expirationTs, int batchSize);

    int failIfStillStuck(AgentBulkActionId id, AgentBulkActionStatus status, String errorMsg);

    PageData<AgentBulkAction> findStuckBulkActions(long threshold, PageLink pageLink);

    PageData<AgentBulkAction> findByAgentProfileId(TenantId tenantId, AgentProfileId profileId, PageLink pageLink);

    PageData<AgentBulkAction> findByAgentProfileIdAndApplicationProfileId(TenantId tenantId,
                                                                         AgentProfileId profileId,
                                                                         AgentAppProfileId applicationProfileId,
                                                                         PageLink pageLink);
}
