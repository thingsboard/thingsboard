// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

@Component
public class QueueStatsDataValidator extends DataValidator<QueueStats> {

    @Override
    protected void validateDataImpl(TenantId tenantId, QueueStats queueStats) {
        if (queueStats.getTenantId() == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (queueStats.getQueueName() == null) {
            throw new DataValidationException("Queue name should be specified!.");
        }
        if (StringUtils.isEmpty(queueStats.getServiceId())) {
            throw new DataValidationException("Service id should be specified!.");
        }
    }
}
