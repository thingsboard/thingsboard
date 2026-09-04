// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.queue;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;

@EqualsAndHashCode(callSuper = true)
@Data
public class QueueStats extends BaseData<QueueStatsId> implements HasTenantId {
    private TenantId tenantId;
    private String queueName;
    private String serviceId;

    public QueueStats() {
    }

    public QueueStats(QueueStatsId id) {
        super(id);
    }

}
