// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.ttl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.queue.discovery.PartitionService;


@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCleanUpService {

    private final PartitionService partitionService;

    protected boolean isSystemTenantPartitionMine() {
        return partitionService.isSystemTenantPartitionMine(ServiceType.TB_CORE);
    }

    protected boolean isTenantPartitionMine(TenantId tenantId) {
        return partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition();
    }

}
