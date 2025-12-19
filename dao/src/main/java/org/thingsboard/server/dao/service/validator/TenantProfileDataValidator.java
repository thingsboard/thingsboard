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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantProfileDao;
import org.thingsboard.server.dao.tenant.TenantProfileService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class TenantProfileDataValidator extends DataValidator<TenantProfile> {

    @Autowired
    private TenantProfileDao tenantProfileDao;

    @Autowired
    @Lazy
    private TenantProfileService tenantProfileService;

    @Override
    protected void validateDataImpl(TenantId tenantId, TenantProfile tenantProfile) {
        validateString("Tenant profile name", tenantProfile.getName());
        if (tenantProfile.getProfileData() == null) {
            throw new DataValidationException("Tenant profile data should be specified!");
        }

        Optional<DefaultTenantProfileConfiguration> profileConfiguration = tenantProfile.getProfileConfiguration();
        if (profileConfiguration.isEmpty()) {
            throw new DataValidationException("Tenant profile data configuration should be specified!");
        }

        if (tenantProfile.isDefault()) {
            TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
            if (defaultTenantProfile != null && !defaultTenantProfile.getId().equals(tenantProfile.getId())) {
                throw new DataValidationException("Another default tenant profile is present!");
            }
        }

        if (tenantProfile.isIsolatedTbRuleEngine()) {
            List<TenantProfileQueueConfiguration> queueConfiguration = tenantProfile.getProfileData().getQueueConfiguration();
            if (queueConfiguration == null) {
                throw new DataValidationException("Tenant profile data queue configuration should be specified!");
            }

            Optional<TenantProfileQueueConfiguration> mainQueueConfig =
                    queueConfiguration
                            .stream()
                            .filter(q -> q.getName().equals(DataConstants.MAIN_QUEUE_NAME))
                            .findAny();
            if (mainQueueConfig.isEmpty()) {
                throw new DataValidationException("Main queue configuration should be specified!");
            }

            queueConfiguration.forEach(this::validateQueueConfiguration);

            Set<String> queueNames = new HashSet<>(queueConfiguration.size());

            queueConfiguration.forEach(q -> {
                String name = q.getName();
                if (queueNames.contains(name)) {
                    throw new DataValidationException(String.format("Queue configuration name '%s' already present!", name));
                } else {
                    queueNames.add(name);
                }
            });
        }
    }

    @Override
    protected TenantProfile validateUpdate(TenantId tenantId, TenantProfile tenantProfile) {
        TenantProfile old = tenantProfileDao.findById(TenantId.SYS_TENANT_ID, tenantProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing tenant profile!");
        }
        return old;
    }

    private void validateQueueConfiguration(TenantProfileQueueConfiguration queue) {
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
