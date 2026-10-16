// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.queue.QueueDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

@Component
public class QueueValidator extends DataValidator<Queue> {

    @Autowired
    private QueueDao queueDao;

    @Lazy
    @Autowired
    private TbTenantProfileCache tenantProfileCache;

    @Override
    protected void validateCreate(TenantId tenantId, Queue queue) {
        if (queueDao.findQueueByTenantIdAndName(tenantId, queue.getName()) != null) {
            throw new DataValidationException(String.format("Queue with name: %s already exists!", queue.getName()));
        }
        if (queueDao.findQueueByTenantIdAndTopic(tenantId, queue.getTopic()) != null) {
            throw new DataValidationException(String.format("Queue with topic: %s already exists!", queue.getTopic()));
        }
    }

    @Override
    protected Queue validateUpdate(TenantId tenantId, Queue queue) {
        Queue foundQueue = queueDao.findById(tenantId, queue.getUuidId());
        if (queueDao.findById(tenantId, queue.getUuidId()) == null) {
            throw new DataValidationException(String.format("Queue with id: %s does not exists!", queue.getId()));
        }
        if (!foundQueue.getName().equals(queue.getName())) {
            throw new DataValidationException("Queue name can't be changed!");
        }
        if (!foundQueue.getTopic().equals(queue.getTopic())) {
            throw new DataValidationException("Queue topic can't be changed!");
        }
        return foundQueue;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Queue queue) {
        if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);

            if (!tenantProfile.isIsolatedTbRuleEngine()) {
                throw new DataValidationException("Tenant should be isolated!");
            }
        }

        validateQueueName(queue.getName());
        validateQueueTopic(queue.getTopic());

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
        if (processingStrategy.getFailurePercentage() < 0 || processingStrategy.getFailurePercentage() > 100) {
            throw new DataValidationException("Queue processing strategy failure percentage should be in a range from 0 to 100!");
        }
        if (processingStrategy.getPauseBetweenRetries() < 0) {
            throw new DataValidationException("Queue processing strategy pause between retries can't be less then 0!");
        }
        if (processingStrategy.getMaxPauseBetweenRetries() < processingStrategy.getPauseBetweenRetries()) {
            throw new DataValidationException("Queue processing strategy MAX pause between retries can't be less then pause between retries!");
        }
    }
}
