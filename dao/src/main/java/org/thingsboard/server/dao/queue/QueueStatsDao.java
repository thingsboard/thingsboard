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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.QueueStats;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface QueueStatsDao extends Dao<QueueStats> {

    QueueStats findByTenantIdAndName(TenantId tenantId, String queueStatsName);

    PageData<QueueStats> findQueueStatsByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<QueueStats> findQueueStatsByQueueId(QueueId queueIdId, PageLink pageLink);

    ListenableFuture<List<QueueStats>> findQueueStatsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> queueStatsIds);
}
