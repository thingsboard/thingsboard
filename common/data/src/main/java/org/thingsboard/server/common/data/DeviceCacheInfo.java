// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Getter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.HasUUID;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * Lightweight cache representation of a Device containing only fields needed by Integration Executor.
 * This significantly reduces memory footprint compared to storing full Device objects.
 */
public record DeviceCacheInfo(
        @Getter UUID id,
        TenantId tenantId,
        CustomerId customerId,
        @Getter String name,
        String type,
        DeviceProfileId deviceProfileId
) implements HasUUID, HasName {

    public DeviceCacheInfo(UUID id, UUID tenantId, UUID customerId, String name, String type, UUID deviceProfileId) {
        this(id, TenantId.fromUUID(tenantId), customerId != null ? new CustomerId(customerId) : null, name, type, new DeviceProfileId(deviceProfileId));
    }

}
