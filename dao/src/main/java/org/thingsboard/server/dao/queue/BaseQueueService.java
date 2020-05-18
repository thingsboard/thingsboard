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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.queue.TbQueueAdmin;

import java.util.List;

@Service
@Slf4j
public class BaseQueueService extends AbstractEntityService implements QueueService {

    @Autowired
    private QueueDao queueDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private TbQueueAdmin tbQueueAdmin;

    @Override
    public Queue createOrUpdateQueue(Queue queue) {
        log.trace("Executing createOrUpdateQueue [{}]", queue);
        queueValidator.validate(queue, Queue::getTenantId);
        if (queue.getId() == null) {
            return createQueue(queue);
        } else {
            return updateQueue(queue);
        }
    }

    private Queue createQueue(Queue queue) {
        TenantId tenantId = TenantId.SYS_TENANT_ID.equals(queue.getTenantId()) ? null : queue.getTenantId();
        if (queue.getPartitions() > 0) {
            for (int i = 0; i < queue.getPartitions(); i++) {
                tbQueueAdmin.createTopicIfNotExists(new TopicPartitionInfo(queue.getTopic(), tenantId, i, false).getFullTopicName());
            }
        } else {
            tbQueueAdmin.createTopicIfNotExists(new TopicPartitionInfo(queue.getTopic(), tenantId, 0, false).getFullTopicName());
        }
        return queueDao.save(queue.getTenantId(), queue);
    }

    private Queue updateQueue(Queue queue) {
        return queueDao.save(queue.getTenantId(), queue);
    }

    @Override
    public Boolean deleteQueue(TenantId tenantId, QueueId queueId) {
        log.trace("Executing deleteQueue, queueId: [{}]", queueId);
        return queueDao.removeById(tenantId, queueId.getId());
    }

    @Override
    public List<Queue> findQueues(TenantId tenantId) {
        log.trace("Executing findQueues, tenantId: [{}]", tenantId);
        return queueDao.find(tenantId);
    }

    private DataValidator<Queue> queueValidator =
            new DataValidator<Queue>() {

                @Override
                protected void validateDataImpl(TenantId tenantId, Queue queue) {
                    if (StringUtils.isEmpty(queue.getName())) {
                        throw new DataValidationException("Queue name should be non empty!");
                    }
                    if (StringUtils.isBlank(queue.getTopic())) {
                        throw new DataValidationException("Queue topic should be non empty and without spaces!");
                    }
                    if (queue.getPollInterval() < 1) {
                        throw new DataValidationException("Queue poll interval should be more then 0!");
                    }
                    if (queue.getPartitions() < 0) {
                        throw new DataValidationException("Queue partitions can't be less then 0!");
                    }
                    if (queue.getPackProcessingTimeout() < 1) {
                        throw new DataValidationException("Queue pack processing timeout should be more then 0!");
                    }

                    SubmitStrategy submitStrategy = queue.getSubmitStrategy();
                    if (submitStrategy == null) {
                        throw new DataValidationException("Queue submit strategy can't be null!");
                    }
                    if (submitStrategy.getType() == null) {
                        throw new DataValidationException("Queue submit strategy type can't be null!");
                    }
                    if (submitStrategy.getType() == SubmitStrategyType.BATCH && submitStrategy.getBatchSize() < 1) {
                        throw new DataValidationException("Queue submit strategy batch size should be more then 0!");
                    }
                    ProcessingStrategy processingStrategy = queue.getProcessingStrategy();
                    if (processingStrategy == null) {
                        throw new DataValidationException("Queue processing strategy can't be null!");
                    }
                    if (processingStrategy.getType() == null) {
                        throw new DataValidationException("Queue processing strategy type can't be null!");
                    }
                    if (processingStrategy.getRetries() < 0) {
                        throw new DataValidationException("Queue processing strategy retries can't be less then 0!");
                    }
                    if (processingStrategy.getFailurePercentage() < 0) {
                        throw new DataValidationException("Queue processing strategy failure percentage can't be less then 0!");
                    }
                    if (processingStrategy.getFailurePercentage() > 100) {
                        throw new DataValidationException("Queue processing strategy failure percentage can't be more then 100!");
                    }
                    if (processingStrategy.getPauseBetweenRetries() < 0) {
                        throw new DataValidationException("Queue processing strategy pause between retries can't be less then 0!");
                    }

                    if (queue.getId() == null) {
                        Queue existingQueue = queueDao.findQueueByTopic(queue.getTopic());
                        if (existingQueue != null && existingQueue.getTopic().equals(queue.getTopic())) {
                            throw new DataValidationException(String.format("Queue with topic: %s already exists!", queue.getTopic()));
                        }
                    }

                    if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                        Tenant queueTenant = tenantDao.findById(TenantId.SYS_TENANT_ID, tenantId.getId());
                        if (queue.getId() == null) {
                            List<Queue> existingQueues = findQueues(tenantId);
                            if (existingQueues.size() >= queueTenant.getNumberOfQueues()) {
                                throw new DataValidationException("The limit for creating new queue has been exceeded!");
                            }
                        }

                        if (queue.getPartitions() > queueTenant.getMaxNumberOfPartitionsPerQueue()) {
                            throw new DataValidationException(String.format("Queue partitions can't be more then %d", queueTenant.getMaxNumberOfPartitionsPerQueue()));
                        }
                    }
                }
            };
}


