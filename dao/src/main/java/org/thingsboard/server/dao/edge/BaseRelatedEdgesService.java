// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.edge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.edge.RelatedEdgesCacheKey;
import org.thingsboard.server.cache.edge.RelatedEdgesCacheValue;
import org.thingsboard.server.cache.edge.RelatedEdgesEvictEvent;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;

import java.util.List;

@Service
@Slf4j
public class BaseRelatedEdgesService extends AbstractCachedEntityService<RelatedEdgesCacheKey, RelatedEdgesCacheValue, RelatedEdgesEvictEvent> implements RelatedEdgesService {

    public static final int RELATED_EDGES_CACHE_ITEMS = 1000;
    public static final PageLink FIRST_PAGE = new PageLink(RELATED_EDGES_CACHE_ITEMS);

    @Autowired
    @Lazy
    private EdgeService edgeService;

    @TransactionalEventListener(classes = RelatedEdgesEvictEvent.class)
    @Override
    public void handleEvictEvent(RelatedEdgesEvictEvent event) {
        if (event.getEntityId() == null) {
            cache.evictByPrefix("{" + event.getTenantId() + "}");
        } else {
            cache.evict(new RelatedEdgesCacheKey(event.getTenantId(), event.getEntityId()));
        }
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        if (!pageLink.equals(FIRST_PAGE)) {
            return edgeService.findEdgeIdsByTenantIdAndEntityId(tenantId, entityId, pageLink);
        }
        return cache.getAndPutInTransaction(new RelatedEdgesCacheKey(tenantId, entityId),
                () -> new RelatedEdgesCacheValue(edgeService.findEdgeIdsByTenantIdAndEntityId(tenantId, entityId, pageLink)), false).getPageData();
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantIdAndGroupEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByTenantIdAndGroupEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        if (!pageLink.equals(FIRST_PAGE)) {
            return edgeService.findEdgeIdsByTenantIdAndGroupEntityId(tenantId, entityId, pageLink);
        }
        return cache.getAndPutInTransaction(new RelatedEdgesCacheKey(tenantId, entityId),
                () -> new RelatedEdgesCacheValue(edgeService.findEdgeIdsByTenantIdAndGroupEntityId(tenantId, entityId, pageLink)), false).getPageData();
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByTenantIdAndEntityGroupIds(TenantId tenantId, EntityGroupId entityGroupId, EntityType groupType, PageLink pageLink) {
        log.trace("Executing findEdgeIdsByTenantIdAndEntityGroupIds, tenantId [{}], entityGroupId [{}], groupType [{}], pageLink [{}]", tenantId, entityGroupId, groupType, pageLink);
        if (!pageLink.equals(FIRST_PAGE)) {
            return edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(tenantId, List.of(entityGroupId), groupType, pageLink);
        }
        return cache.getAndPutInTransaction(new RelatedEdgesCacheKey(tenantId, entityGroupId),
                () -> new RelatedEdgesCacheValue(edgeService.findEdgeIdsByTenantIdAndEntityGroupIds(tenantId, List.of(entityGroupId), groupType, pageLink)), false).getPageData();
    }

    @Override
    public void publishRelatedEdgeIdsEvictEvent(TenantId tenantId, EntityId entityId) {
        log.trace("Executing publishRelatedEdgeIdsEvictEvent, tenantId [{}], entityId [{}]", tenantId, entityId);
        publishEvictEvent(new RelatedEdgesEvictEvent(tenantId, entityId));
    }

    @Override
    public void publishEdgeIdsEvictEventByTenantId(TenantId tenantId) {
        log.trace("Executing publishEdgeIdsEvictEventByTenantId, tenantId [{}]", tenantId);
        publishEvictEvent(new RelatedEdgesEvictEvent(tenantId, null));
    }

}
