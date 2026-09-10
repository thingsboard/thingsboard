// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.With;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.ServiceType;

@Data
@AllArgsConstructor
public class QueueKey {

    private final ServiceType type;
    @With
    private final String queueName;
    private final TenantId tenantId;

    public QueueKey(ServiceType type, Queue queue) {
        this.type = type;
        this.queueName = queue.getName();
        this.tenantId = queue.getTenantId();
    }

    public QueueKey(ServiceType type, QueueRoutingInfo queueRoutingInfo) {
        this.type = type;
        this.queueName = queueRoutingInfo.getQueueName();
        this.tenantId = queueRoutingInfo.getTenantId();
    }

    public QueueKey(ServiceType type, TenantId tenantId) {
        this.type = type;
        this.queueName = DataConstants.MAIN_QUEUE_NAME;
        this.tenantId = tenantId != null ? tenantId : TenantId.SYS_TENANT_ID;
    }

    public QueueKey(ServiceType type) {
        this.type = type;
        this.queueName = DataConstants.MAIN_QUEUE_NAME;
        this.tenantId = TenantId.SYS_TENANT_ID;
    }

    public QueueKey(ServiceType type, String queueName) {
        this.type = type;
        this.queueName = queueName;
        this.tenantId = TenantId.SYS_TENANT_ID;
    }

    @Override
    public String toString() {
        return "QK(" + queueName + "," + type + "," +
                (TenantId.SYS_TENANT_ID.equals(tenantId) ? "system" : tenantId) +
                ')';
    }
}
