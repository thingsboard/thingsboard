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
package org.thingsboard.server.dao.audit;

import lombok.Getter;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.model.nosql.AuditLogEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuditLogQueryCursor {
    @Getter
    private final UUID tenantId;
    @Getter
    private final List<AuditLogEntity> data;
    @Getter
    private final TimePageLink pageLink;

    private final List<Long> partitions;

    private int partitionIndex;
    private int currentLimit;

    public AuditLogQueryCursor(UUID tenantId, TimePageLink pageLink, List<Long> partitions) {
        this.tenantId = tenantId;
        this.partitions = partitions;
        this.partitionIndex = partitions.size() - 1;
        this.data = new ArrayList<>();
        this.currentLimit = pageLink.getLimit();
        this.pageLink = pageLink;
    }

    public boolean hasNextPartition() {
        return partitionIndex >= 0;
    }

    public boolean isFull() {
        return currentLimit <= 0;
    }

    public long getNextPartition() {
        long partition = partitions.get(partitionIndex);
        partitionIndex--;
        return partition;
    }

    public int getCurrentLimit() {
        return currentLimit;
    }

    public void addData(List<AuditLogEntity> newData) {
        currentLimit -= newData.size();
        data.addAll(newData);
    }
}
