// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.queue;

import org.thingsboard.server.common.data.id.QueueStatsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.List;

public interface QueueStatsDao extends Dao<QueueStats>, TenantEntityDao<QueueStats> {

    QueueStats findByTenantIdQueueNameAndServiceId(TenantId tenantId, String queueName, String serviceId);

    void deleteByTenantId(TenantId tenantId);

    List<QueueStats> findByIds(TenantId tenantId, List<QueueStatsId> queueStatsIds);

}