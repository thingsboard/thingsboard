// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class QueueStatsFields extends AbstractEntityFields {

    private String queueName;
    private String serviceId;

    @Override
    public String getName() {
        return queueName + '_' + serviceId;
    }

    public QueueStatsFields(UUID id, long createdTime, UUID tenantId, String queueName, String serviceId) {
        super(id, createdTime, tenantId);
        this.queueName = queueName;
        this.serviceId = serviceId;
    }
}
