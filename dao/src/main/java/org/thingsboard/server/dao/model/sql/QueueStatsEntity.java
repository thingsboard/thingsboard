/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.QUEUE_STATS_TABLE_NAME)
public class QueueStatsEntity extends BaseSqlEntity<QueueStats> {

    @Column(name = ModelConstants.QUEUE_STATS_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.QUEUE_STATS_QUEUE_NAME_PROPERTY)
    private String queueName;

    @Column(name = ModelConstants.QUEUE_STATS_SERVICE_ID_PROPERTY)
    private String serviceId;

    public QueueStatsEntity() {
    }

    public QueueStatsEntity(QueueStats queueStats) {
        if (queueStats.getId() != null) {
            this.setId(queueStats.getId().getId());
        }
        this.setCreatedTime(queueStats.getCreatedTime());
        this.tenantId = DaoUtil.getId(queueStats.getTenantId());
        this.queueName = queueStats.getQueueName();
        this.serviceId = queueStats.getServiceId();
    }

    @Override
    public QueueStats toData() {
        QueueStats queueStats = new QueueStats(new QueueStatsId(getUuid()));
        queueStats.setCreatedTime(createdTime);
        queueStats.setTenantId(TenantId.fromUUID(tenantId));
        queueStats.setQueueName(queueName);
        queueStats.setServiceId(serviceId);
        return queueStats;
    }
}