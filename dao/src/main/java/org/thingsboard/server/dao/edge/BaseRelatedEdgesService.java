// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.edge.RelatedEdgesCacheKey;
import org.thingsboard.server.cache.edge.RelatedEdgesCacheValue;
import org.thingsboard.server.cache.edge.RelatedEdgesEvictEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;

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
        cache.evict(new RelatedEdgesCacheKey(event.getTenantId(), event.getEntityId()));
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
    public void publishRelatedEdgeIdsEvictEvent(TenantId tenantId, EntityId entityId) {
        log.trace("Executing publishRelatedEdgeIdsEvictEvent, tenantId [{}], entityId [{}]", tenantId, entityId);
        publishEvictEvent(new RelatedEdgesEvictEvent(tenantId, entityId));
    }

}
