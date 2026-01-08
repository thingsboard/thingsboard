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
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class BasePageableEdgeEventFetcher<T> implements EdgeEventFetcher {

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) {
        log.trace("[{}][{}] start fetching edge events [{}], pageLink {}", getClass().getSimpleName(), tenantId, edge.getId(), pageLink);
        PageData<T> entities = fetchEntities(tenantId, edge, pageLink);
        List<EdgeEvent> result = new ArrayList<>();
        if (!entities.getData().isEmpty()) {
            for (T entity : entities.getData()) {
                result.add(constructEdgeEvent(tenantId, edge, entity));
            }
        }
        return new PageData<>(result, entities.getTotalPages(), entities.getTotalElements(), entities.hasNext());
    }

    abstract PageData<T> fetchEntities(TenantId tenantId, Edge edge, PageLink pageLink);

    abstract EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, T entity);

}
