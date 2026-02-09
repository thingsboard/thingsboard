/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
