/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.QueueStatsEntity;
import org.thingsboard.server.dao.queue.QueueStatsDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class JpaQueueStatsDao extends JpaAbstractDao<QueueStatsEntity, QueueStats> implements QueueStatsDao {

    @Autowired
    private QueueStatsRepository queueStatsRepository;

    @Override
    protected Class<QueueStatsEntity> getEntityClass() {
        return QueueStatsEntity.class;
    }

    @Override
    protected CrudRepository<QueueStatsEntity, UUID> getCrudRepository() {
        return queueStatsRepository;
    }

    @Override
    public QueueStats findByTenantIdAndName(TenantId tenantId, String queueStatsName) {
        return DaoUtil.getData(queueStatsRepository.findByTenantIdAndName(tenantId.getId(), queueStatsName));
    }

    @Override
    public PageData<QueueStats> findQueueStatsByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(queueStatsRepository
                .findByTenantId(tenantId.getId(), Objects.toString(pageLink.getTextSearch(), ""), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<QueueStats> findQueueStatsByQueueId(QueueId queueIdId, PageLink pageLink) {
        return DaoUtil.toPageData(queueStatsRepository
                .findByQueueId(queueIdId.getId(), Objects.toString(pageLink.getTextSearch(), ""), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public ListenableFuture<List<QueueStats>> findQueueStatsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> queueStatsIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                queueStatsRepository.findAllByTenantIdAndIdIn(tenantId, queueStatsIds)));
    }
}
