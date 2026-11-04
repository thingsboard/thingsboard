// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Getter;
import org.thingsboard.server.common.data.id.HasUUID;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * Lightweight cache representation of an Asset Profile containing only fields needed by Integration Executor.
 * This significantly reduces memory footprint compared to storing full AssetProfile objects.
 */
public record AssetProfileCacheInfo(
        @Getter UUID id,
        TenantId tenantId,
        @Getter String name,
        RuleChainId defaultRuleChainId,
        String defaultQueueName
) implements HasUUID, HasName {

    public AssetProfileCacheInfo(UUID id, UUID tenantId, String name, UUID defaultRuleChainId, String defaultQueueName) {
        this(id, TenantId.fromUUID(tenantId), name, defaultRuleChainId != null ? new RuleChainId(defaultRuleChainId) : null, defaultQueueName);
    }

}
