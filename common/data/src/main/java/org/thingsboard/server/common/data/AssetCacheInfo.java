// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Getter;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.HasUUID;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * Lightweight cache representation of an Asset containing only fields needed by Integration Executor.
 * This significantly reduces memory footprint compared to storing full Asset objects.
 */
public record AssetCacheInfo(
        @Getter UUID id,
        TenantId tenantId,
        CustomerId customerId,
        @Getter String name,
        String type,
        AssetProfileId assetProfileId
) implements HasUUID, HasName {

    public AssetCacheInfo(UUID id, UUID tenantId, UUID customerId, String name, String type, UUID assetProfileId) {
        this(id, TenantId.fromUUID(tenantId), customerId != null ? new CustomerId(customerId) : null, name, type, new AssetProfileId(assetProfileId));
    }

}
