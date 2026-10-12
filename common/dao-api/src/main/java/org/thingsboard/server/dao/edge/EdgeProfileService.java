// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.edge;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface EdgeProfileService extends EntityDaoService {

    EdgeProfile saveEdgeProfile(EdgeProfile profile);

    EdgeProfile saveEdgeProfile(EdgeProfile profile, boolean doValidate);

    EdgeProfile findEdgeProfileById(TenantId tenantId, EdgeProfileId edgeProfileId);

    EdgeProfile findEdgeProfileByName(TenantId tenantId, String name);

    EdgeProfile findDefaultEdgeProfile(TenantId tenantId);

    /** The tenant default profile, rooted on the stock edge rule chain when the tenant already has one. */
    EdgeProfile createDefaultEdgeProfile(TenantId tenantId);

    EdgeProfile findOrCreateEdgeProfile(TenantId tenantId, String name);

    /** The profile an edge is saved with: {@code edgeProfileId}, else a profile named {@code type}, else the tenant default; missing ones are created. */
    EdgeProfile resolveEdgeProfile(TenantId tenantId, EdgeProfileId edgeProfileId, String type);

    boolean setDefaultEdgeProfile(TenantId tenantId, EdgeProfileId edgeProfileId);

    PageData<EdgeProfile> findEdgeProfilesByTenantId(TenantId tenantId, PageLink pageLink);

    void deleteEdgeProfile(TenantId tenantId, EdgeProfileId edgeProfileId);

    void deleteEdgeProfilesByTenantId(TenantId tenantId);

    EdgeProfile addRuleChain(TenantId tenantId, EdgeProfileId edgeProfileId, RuleChainId ruleChainId);

    EdgeProfile removeRuleChain(TenantId tenantId, EdgeProfileId edgeProfileId, RuleChainId ruleChainId);

    EdgeProfile setRootRuleChain(TenantId tenantId, EdgeProfileId edgeProfileId, RuleChainId ruleChainId);

    EdgeProfile addEntityGroup(TenantId tenantId, EdgeProfileId edgeProfileId, EntityGroupId entityGroupId, EntityType groupType);

    EdgeProfile removeEntityGroup(TenantId tenantId, EdgeProfileId edgeProfileId, EntityGroupId entityGroupId);

    EdgeProfile addIntegration(TenantId tenantId, EdgeProfileId edgeProfileId, IntegrationId integrationId);

    EdgeProfile removeIntegration(TenantId tenantId, EdgeProfileId edgeProfileId, IntegrationId integrationId);

    EdgeProfile addSchedulerEvent(TenantId tenantId, EdgeProfileId edgeProfileId, SchedulerEventId schedulerEventId);

    EdgeProfile removeSchedulerEvent(TenantId tenantId, EdgeProfileId edgeProfileId, SchedulerEventId schedulerEventId);

}
