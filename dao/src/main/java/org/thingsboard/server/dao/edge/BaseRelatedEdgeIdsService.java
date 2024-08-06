/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.edge.EdgeIdsCacheKey;
import org.thingsboard.server.cache.edge.EdgeIdsEvictEvent;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseRelatedEdgeIdsService extends AbstractCachedEntityService<EdgeIdsCacheKey, PageData<EdgeId>, EdgeIdsEvictEvent> implements RelatedEdgeIdsService {

    public static final int EDGE_IDS_CACHE_ITEMS = 1000;

    @Autowired
    @Lazy
    private EdgeService edgeService;

    @Override
    public void handleEvictEvent(EdgeIdsEvictEvent event) {
        cache.evict(new EdgeIdsCacheKey(event.getTenantId(), event.getEntityId()));
    }

    @Override
    public PageData<EdgeId> findEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        if (pageLink.getPageSize() != EDGE_IDS_CACHE_ITEMS) {
            return findEdgesByEntityIdAndConvertToEdgeId(tenantId, entityId, pageLink);
        }
        return cache.getAndPutInTransaction(new EdgeIdsCacheKey(tenantId, entityId),
                () -> findEdgesByEntityIdAndConvertToEdgeId(tenantId, entityId, pageLink), false);
    }

    @Override
    public void publishRelatedEdgeIdsEvictEvent(TenantId tenantId, EntityId entityId) {
        publishEvictEvent(new EdgeIdsEvictEvent(tenantId, entityId));
    }

    private PageData<EdgeId> findEdgesByEntityIdAndConvertToEdgeId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        PageData<Edge> pageData = edgeService.findEdgesByTenantIdAndEntityId(tenantId, entityId, pageLink);
        if (pageData == null) {
            return new PageData<>(new ArrayList<>(), 0, 0, false);
        }
        List<EdgeId> edgeIds = new ArrayList<>();
        if (pageData.getData() != null && !pageData.getData().isEmpty()) {
            edgeIds = pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        }
        return new PageData<>(edgeIds, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

}
