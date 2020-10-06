/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.queue;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.List;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Service
@Slf4j
public class BaseQueueStatsService extends AbstractEntityService implements QueueStatsService {

    @Autowired
    private QueueStatsDao queueStatsDao;

    @Override
    public QueueStats save(TenantId tenantId, QueueStats queueStats) {
        return queueStatsDao.save(tenantId, queueStats);
    }

    @Override
    public QueueStats findByTenantIdAndName(TenantId tenantId, String queueStatsName) {
        log.trace("Executing findByTenantIdAndName, tenantId: [{}], name: [{}]", tenantId, queueStatsName);
        return queueStatsDao.findByTenantIdAndName(tenantId, queueStatsName);
    }

    @Override
    public PageData<QueueStats> findQueueStats(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findQueues pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return queueStatsDao.findQueueStatsByTenantId(tenantId, pageLink);
    }

    @Override
    public QueueStats findQueueStatsById(TenantId tenantId, QueueStatsId queueStatsId) {
        Validator.validateId(queueStatsId, "Incorrect queue stats id for search request.");
        return queueStatsDao.findById(tenantId, queueStatsId.getId());
    }

    @Override
    public ListenableFuture<QueueStats> findQueueStatsByIdAsync(TenantId tenantId, QueueStatsId queueStatsId) {
        log.trace("Executing findQueueStatsByIdAsync [{}]", queueStatsId);
        validateId(queueStatsId, "Incorrect queueStatsId" + queueStatsId);
        return queueStatsDao.findByIdAsync(tenantId, queueStatsId.getId());
    }

    @Override
    public ListenableFuture<List<QueueStats>> findQueueStatsByTenantIdAndIdsAsync(TenantId tenantId, List<QueueStatsId> queueStatsIds) {
        log.trace("Executing findQueueStatsByTenantIdAndIdsAsync, tenantId [{}], queueStatsIds [{}]", tenantId, queueStatsIds);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validateIds(queueStatsIds, "Incorrect queueStatsIds " + queueStatsIds);
        return queueStatsDao.findQueueStatsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(queueStatsIds));
    }

    @Override
    public void deleteQueueStats(TenantId tenantId, QueueStatsId queueStatsId) {
        log.trace("Executing deleteQueueStats, queueStatsId: [{}]", queueStatsId);
        queueStatsDao.removeById(tenantId, queueStatsId.getId());
    }

    @Override
    public void deleteQueueStatsByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete queue stats request.");
        tenantQueueStatsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteQueueStatsByQueueId(TenantId tenantId, QueueId queueId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete queue stats request.");
        queueQueueStatsRemover.removeEntities(tenantId, queueId);
    }

    private PaginatedRemover<QueueId, QueueStats> queueQueueStatsRemover =
            new PaginatedRemover<QueueId, QueueStats>() {

                @Override
                protected PageData<QueueStats> findEntities(TenantId tenantId, QueueId queueId, PageLink pageLink) {
                    return queueStatsDao.findQueueStatsByQueueId(queueId, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, QueueStats entity) {
                    deleteQueueStats(tenantId, entity.getId());
                }
            };

    private PaginatedRemover<TenantId, QueueStats> tenantQueueStatsRemover =
            new PaginatedRemover<TenantId, QueueStats>() {

                @Override
                protected PageData<QueueStats> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return queueStatsDao.findQueueStatsByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, QueueStats entity) {
                    deleteQueueStats(tenantId, entity.getId());
                }
            };
}
