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
package org.thingsboard.server.dao.queue;

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Service("QueueStatsDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseQueueStatsService extends AbstractEntityService implements QueueStatsService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final QueueStatsDao queueStatsDao;

    private final DataValidator<QueueStats> queueStatsValidator;

    @Override
    public QueueStats save(TenantId tenantId, QueueStats queueStats) {
        log.trace("Executing save [{}]", queueStats);
        queueStatsValidator.validate(queueStats, QueueStats::getTenantId);
        QueueStats savedQueueStats = queueStatsDao.save(tenantId, queueStats);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedQueueStats.getTenantId()).entityId(savedQueueStats.getId())
                .entity(savedQueueStats).created(queueStats.getId() == null).build());
        return savedQueueStats;
    }

    @Override
    public QueueStats findQueueStatsById(TenantId tenantId, QueueStatsId queueStatsId) {
        log.trace("Executing findQueueStatsById [{}]", queueStatsId);
        validateId(queueStatsId, id -> "Incorrect queueStatsId " + id);
        return queueStatsDao.findById(tenantId, queueStatsId.getId());
    }

    @Override
    public List<QueueStats> findQueueStatsByIds(TenantId tenantId, List<QueueStatsId> queueStatsIds) {
        log.trace("Executing findQueueStatsByIds, tenantId [{}], queueStatsIds [{}]", tenantId, queueStatsIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(queueStatsIds, ids -> "Incorrect queueStatsIds " + ids);
        return queueStatsDao.findByIds(tenantId, queueStatsIds);
    }

    @Override
    public QueueStats findByTenantIdAndNameAndServiceId(TenantId tenantId, String queueName, String serviceId) {
        log.trace("Executing findByTenantIdAndNameAndServiceId, tenantId: [{}], queueName: [{}], serviceId: [{}]", tenantId, queueName, serviceId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return queueStatsDao.findByTenantIdQueueNameAndServiceId(tenantId, queueName, serviceId);
    }

    @Override
    public PageData<QueueStats> findByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findByTenantId, tenantId: [{}]", tenantId);
        Validator.validatePageLink(pageLink);
        return queueStatsDao.findAllByTenantId(tenantId, pageLink);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        queueStatsDao.deleteByTenantId(tenantId);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        queueStatsDao.removeById(tenantId, id.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(id).build());
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findQueueStatsById(tenantId, new QueueStatsId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(queueStatsDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.QUEUE_STATS;
    }

}
