// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.edge;

import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface EdgeProfileDao extends Dao<EdgeProfile> {

    PageData<EdgeProfile> findByTenantId(UUID tenantId, PageLink pageLink);

    EdgeProfile findByTenantIdAndName(UUID tenantId, String name);

    void lockNameForCreate(UUID tenantId, String name);

    EdgeProfile findDefaultEdgeProfile(UUID tenantId);

    List<EdgeProfile> findByTenantIdAndReferencedEntityId(UUID tenantId, UUID entityId);

    boolean existsByTenantIdAndRootRuleChainId(UUID tenantId, UUID ruleChainId);

    Long countByTenantId(TenantId tenantId);

}
