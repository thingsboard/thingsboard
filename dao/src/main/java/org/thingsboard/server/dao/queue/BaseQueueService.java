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
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueClusterService;

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

    @Autowired
    private TbQueueClusterService queueClusterService;

    @Override
    @Transactional
    public Queue createOrUpdateQueue(Queue queue) {
        log.trace("Executing createOrUpdateQueue [{}]", queue);
        queueValidator.validate(queue, Queue::getTenantId);
        Queue savedQueue;
        if (queue.getId() == null) {
            savedQueue = createQueue(queue);
        } else {
            savedQueue = updateQueue(queue);
        }

        queueClusterService.onQueueChange(savedQueue, null);
        return savedQueue;
    }

    private Queue createQueue(Queue queue) {
        Queue createdQueue = queueDao.save(queue.getTenantId(), queue);
        for (int i = 0; i < queue.getPartitions(); i++) {
            tbQueueAdmin.createTopicIfNotExists(new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName());
        }
        return createdQueue;
    }

    private Queue updateQueue(Queue queue) {
        Queue oldQueue = queueDao.findById(queue.getTenantId(), queue.getUuidId());
        Queue updatedQueue = queueDao.save(queue.getTenantId(), queue);

        int oldPartitions = oldQueue.getPartitions();
        int currentPartitions = queue.getPartitions();

        if (currentPartitions != oldPartitions) {
            if (currentPartitions > oldPartitions) {
                for (int i = oldPartitions; i < currentPartitions; i++) {
                    tbQueueAdmin.createTopicIfNotExists(new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName());
                }
            } else {
                for (int i = currentPartitions; i < oldPartitions; i++) {
                    tbQueueAdmin.deleteTopic(new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName());
                }
            }
        }

        return updatedQueue;
    }

    @Override
    @Transactional
    public void deleteQueue(TenantId tenantId, QueueId queueId) {
        log.trace("Executing deleteQueue, queueId: [{}]", queueId);
        Queue queue = findQueueById(tenantId, queueId);
        queueClusterService.onQueueDelete(queue, null);
        boolean result = queueDao.removeById(tenantId, queueId.getId());
        if (result) {
            for (int i = 0; i < queue.getPartitions(); i++) {
                String fullTopicName = new TopicPartitionInfo(queue.getTopic(), queue.getTenantId(), i, false).getFullTopicName();
                log.debug("Deleting queue [{}]", fullTopicName);
                try {
                    tbQueueAdmin.deleteTopic(fullTopicName);
                } catch (Exception e) {
                    log.error("Failed to delete queue [{}]", fullTopicName);
                }
            }
        }
    }

    @Override
    public List<Queue> findQueues(TenantId tenantId) {
        log.trace("Executing findQueues, tenantId: [{}]", tenantId);
        if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
            Tenant tenant = tenantDao.findById(TenantId.SYS_TENANT_ID, tenantId.getId());
            if (tenant.isIsolatedTbRuleEngine()) {
                return queueDao.findAllByTenantId(tenantId);
            }
        }

        return queueDao.findAllByTenantId(TenantId.SYS_TENANT_ID);
    }

    @Override
    public List<Queue> findAllMainQueues() {
        log.trace("Executing findAllMainQueues");
        return queueDao.findAllMainQueues();
    }

    @Override
    public List<Queue> findAllQueues() {
        log.trace("Executing findAllQueues");
        return queueDao.findAllQueues();    }

    @Override
    public Queue findQueueById(TenantId tenantId, QueueId queueId) {
        log.trace("Executing findQueueById, queueId: [{}]", queueId);
        return queueDao.findById(tenantId, queueId.getId());
    }

    @Override
    public Queue findQueueByTenantIdAndName(TenantId tenantId, String queueName) {
        log.trace("Executing findQueueByTenantIdAndName, tenantId: [{}] queueName: [{}]", tenantId, queueName);
        return queueDao.findQueueByTenantIdAndName(tenantId, queueName);
    }

    @Override
    public void deleteQueuesByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete rule chains request.");
        tenantQueuesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    @Transactional
    public Queue createDefaultMainQueue(Tenant tenant) {
        Queue mainQueue = new Queue();
        mainQueue.setTenantId(tenant.getTenantId());
        mainQueue.setName("Main");
        mainQueue.setTopic("tb_rule_engine.main");
        mainQueue.setPollInterval(25);
        mainQueue.setPartitions(tenant.getMaxNumberOfPartitionsPerQueue());
        mainQueue.setPackProcessingTimeout(60000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueue.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueue.setProcessingStrategy(mainQueueProcessingStrategy);
        return createOrUpdateQueue(mainQueue);
    }

    private DataValidator<Queue> queueValidator =
            new DataValidator<Queue>() {

                @Override
                protected void validateCreate(TenantId tenantId, Queue queue) {
                    if (queueDao.findQueueByTenantIdAndTopic(tenantId, queue.getTopic()) != null) {
                        throw new DataValidationException(String.format("Queue with topic: %s already exists!", queue.getTopic()));
                    }
                    if (queueDao.findQueueByTenantIdAndName(tenantId, queue.getName()) != null) {
                        throw new DataValidationException(String.format("Queue with name: %s already exists!", queue.getName()));
                    }
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Queue queue) {
                    if (queueDao.findById(tenantId, queue.getUuidId()) == null) {
                        throw new DataValidationException(String.format("Queue with id: %s does not exists!", queue.getId()));
                    }
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Queue queue) {
                    if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
                        Tenant queueTenant = tenantDao.findById(TenantId.SYS_TENANT_ID, tenantId.getId());

                        if (!queueTenant.isIsolatedTbRuleEngine()) {
                            throw new DataValidationException("Tenant should be isolated!");
                        }

                        if (queue.getId() == null) {
                            List<Queue> existingQueues = findQueues(tenantId);
                            if (existingQueues.size() >= queueTenant.getMaxNumberOfQueues()) {
                                throw new DataValidationException("The limit for creating new queue has been exceeded!");
                            }
                        }

                        if (queue.getPartitions() > queueTenant.getMaxNumberOfPartitionsPerQueue()) {
                            throw new DataValidationException(String.format("Queue partitions can't be more then %d", queueTenant.getMaxNumberOfPartitionsPerQueue()));
                        }
                    }

                    if (StringUtils.isEmpty(queue.getName())) {
                        throw new DataValidationException("Queue name should be non empty!");
                    }
                    if (StringUtils.isBlank(queue.getTopic())) {
                        throw new DataValidationException("Queue topic should be non empty and without spaces!");
                    }
                    if (queue.getPollInterval() < 1) {
                        throw new DataValidationException("Queue poll interval should be more then 0!");
                    }
                    if (queue.getPartitions() < 1) {
                        throw new DataValidationException("Queue partitions should be more then 0!");
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
                }
            };

    private PaginatedRemover<TenantId, Queue> tenantQueuesRemover =
            new PaginatedRemover<TenantId, Queue>() {

                @Override
                protected PageData<Queue> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return queueDao.findQueuesByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Queue entity) {
                    deleteQueue(tenantId, entity.getId());
                }
            };
}
