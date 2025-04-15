/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.edqs.repo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEvent;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.stats.EdqsStatsService;
import org.thingsboard.server.queue.edqs.EdqsComponent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

@EdqsComponent
@AllArgsConstructor
@Service
@Slf4j
public class DefaultEdqsRepository implements EdqsRepository {

    @Getter
    private final static ConcurrentMap<TenantId, TenantRepo> repos = new ConcurrentHashMap<>();
    private final EdqsStatsService statsService;

    public TenantRepo get(TenantId tenantId) {
        return repos.computeIfAbsent(tenantId, id -> new TenantRepo(id, statsService));
    }

    @Override
    public void processEvent(EdqsEvent event) {
        if (event.getEventType() == EdqsEventType.DELETED && event.getObjectType() == ObjectType.TENANT) {
            log.info("Tenant {} deleted", event.getTenantId());
            repos.remove(event.getTenantId());
            statsService.reportRemoved(ObjectType.TENANT);
        } else {
            get(event.getTenantId()).processEvent(event);
        }
    }

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query, boolean ignorePermissionCheck) {
        long startNs = System.nanoTime();
        long result = get(tenantId).countEntitiesByQuery(customerId, query, ignorePermissionCheck);
        statsService.reportEdqsCountQuery(tenantId, query, System.nanoTime() - startNs);
        return result;
    }

    @Override
    public PageData<QueryResult> findEntityDataByQuery(TenantId tenantId, CustomerId customerId,
                                                       EntityDataQuery query, boolean ignorePermissionCheck) {
        long startNs = System.nanoTime();
        var result = get(tenantId).findEntityDataByQuery(customerId, query, ignorePermissionCheck);
        statsService.reportEdqsDataQuery(tenantId, query, System.nanoTime() - startNs);
        return result;
    }

    @Override
    public void clearIf(Predicate<TenantId> predicate) {
        repos.keySet().removeIf(predicate);
    }

    @Override
    public void clear() {
        repos.clear();
    }

}
