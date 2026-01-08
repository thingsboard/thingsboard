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
package org.thingsboard.server.dao.sql.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.QueueStatsFields;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.QueueStatsEntity;
import org.thingsboard.server.dao.queue.QueueStatsDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;

@Slf4j
@Component
@SqlDao
public class JpaQueueStatsDao extends JpaAbstractDao<QueueStatsEntity, QueueStats> implements QueueStatsDao {

    @Autowired
    private QueueStatsRepository queueStatsRepository;

    @Override
    protected Class<QueueStatsEntity> getEntityClass() {
        return QueueStatsEntity.class;
    }

    @Override
    protected JpaRepository<QueueStatsEntity, UUID> getRepository() {
        return queueStatsRepository;
    }

    @Override
    public QueueStats findByTenantIdQueueNameAndServiceId(TenantId tenantId, String queueName, String serviceId) {
        return DaoUtil.getData(queueStatsRepository.findByTenantIdAndQueueNameAndServiceId(tenantId.getId(), queueName, serviceId));
    }

    @Override
    public PageData<QueueStats> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(queueStatsRepository.findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        queueStatsRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public List<QueueStats> findByIds(TenantId tenantId, List<QueueStatsId> queueStatsIds) {
        return DaoUtil.convertDataList(queueStatsRepository.findByTenantIdAndIdIn(tenantId.getId(), toUUIDs(queueStatsIds)));
    }

    @Override
    public List<QueueStatsFields> findNextBatch(UUID id, int batchSize) {
        return queueStatsRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.QUEUE_STATS;
    }

}
