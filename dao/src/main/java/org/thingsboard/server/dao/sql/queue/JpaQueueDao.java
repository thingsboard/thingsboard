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

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.QueueEntity;
import org.thingsboard.server.dao.queue.QueueDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class JpaQueueDao extends JpaAbstractDao<QueueEntity, Queue> implements QueueDao {

    @Autowired
    private QueueRepository queueRepository;

    @Override
    protected Class<QueueEntity> getEntityClass() {
        return QueueEntity.class;
    }

    @Override
    protected CrudRepository<QueueEntity, UUID> getCrudRepository() {
        return queueRepository;
    }

    @Override
    public Queue findQueueByTenantIdAndTopic(TenantId tenantId, String topic) {
        return DaoUtil.getData(queueRepository.findByTenantIdAndTopic(tenantId.getId(), topic));
    }

    @Override
    public Queue findQueueByTenantIdAndName(TenantId tenantId, String name) {
        return DaoUtil.getData(queueRepository.findByTenantIdAndName(tenantId.getId(), name));
    }

    @Override
    public List<Queue> findAllByTenantId(TenantId tenantId) {
        List<QueueEntity> entities = queueRepository.findByTenantId(tenantId.getId());
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<Queue> findAllMainQueues() {
        List<QueueEntity> entities = Lists.newArrayList(queueRepository.findAllByName("Main"));
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<Queue> findAllQueues() {
        List<QueueEntity> entities = Lists.newArrayList(queueRepository.findAll());
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public PageData<Queue> findQueuesByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(queueRepository
                .findByTenantId(tenantId.getId(), Objects.toString(pageLink.getTextSearch(), ""), DaoUtil.toPageable(pageLink)));
    }
}