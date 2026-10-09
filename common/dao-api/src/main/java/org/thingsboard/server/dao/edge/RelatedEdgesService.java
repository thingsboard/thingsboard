// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.edge;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

public interface RelatedEdgesService {

    PageData<EdgeId> findEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndGroupEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink);

    PageData<EdgeId> findEdgeIdsByTenantIdAndEntityGroupIds(TenantId tenantId, EntityGroupId entityGroupId, EntityType groupType, PageLink pageLink);

    void publishRelatedEdgeIdsEvictEvent(TenantId tenantId, EntityId entityId);

    void publishEdgeIdsEvictEventByTenantId(TenantId tenantId);

}
