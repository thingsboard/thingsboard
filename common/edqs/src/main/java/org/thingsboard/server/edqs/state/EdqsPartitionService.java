// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.state;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.queue.discovery.HashPartitionService;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.edqs.EdqsConfig.EdqsPartitioningStrategy;

@Service
@RequiredArgsConstructor
public class EdqsPartitionService {

    private final HashPartitionService hashPartitionService;
    private final EdqsConfig edqsConfig;

    public Integer resolvePartition(TenantId tenantId, Object key) {
        if (edqsConfig.getPartitioningStrategy() == EdqsPartitioningStrategy.TENANT) {
            return hashPartitionService.resolvePartitionIndex(tenantId.getId(), edqsConfig.getPartitions());
        } else {
            if (key == null) {
                throw new IllegalArgumentException("Partitioning key is missing but partitioning strategy is not TENANT");
            }
            return hashPartitionService.resolvePartitionIndex(key.toString(), edqsConfig.getPartitions());
        }
    }

}
