// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.queue;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.Dao;

import java.util.List;

public interface QueueDao extends Dao<Queue> {
    Queue findQueueByTenantIdAndTopic(TenantId tenantId, String topic);

    Queue findQueueByTenantIdAndName(TenantId tenantId, String name);

    List<Queue> findAllMainQueues();

    List<Queue> findAllQueues();

    List<Queue> findAllByTenantId(TenantId tenantId);

    PageData<Queue> findQueuesByTenantId(TenantId tenantId, PageLink pageLink);
}