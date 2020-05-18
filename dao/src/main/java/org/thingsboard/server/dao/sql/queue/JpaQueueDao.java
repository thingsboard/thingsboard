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
package org.thingsboard.server.dao.sql.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.QueueEntity;
import org.thingsboard.server.dao.queue.QueueDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

@Slf4j
@Component
@SqlDao
public class JpaQueueDao extends JpaAbstractDao<QueueEntity, Queue> implements QueueDao {

    @Autowired
    private QueueRepository queueRepository;

    @Override
    protected Class<QueueEntity> getEntityClass() {
        return QueueEntity.class;
    }

    @Override
    protected CrudRepository<QueueEntity, String> getCrudRepository() {
        return queueRepository;
    }

    @Override
    public Queue findQueueByTopic(String topic) {
        return DaoUtil.getData(queueRepository.findByTopic(topic));
    }
}
