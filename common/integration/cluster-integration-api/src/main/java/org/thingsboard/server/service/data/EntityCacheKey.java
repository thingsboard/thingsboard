// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.data;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;
import org.thingsboard.server.common.data.id.TenantId;

@Builder
public record EntityCacheKey(TenantId tenantId, String name) {

    @NotNull
    @Override
    public String toString() {
        return tenantId + "_" + name;
    }

}
