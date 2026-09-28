// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.secret;

import org.jetbrains.annotations.NotNull;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

public record SecretCacheKey(TenantId tenantId, String name) implements Serializable {

    @NotNull
    @Override
    public String toString() {
        return tenantId + "_" + name;
    }

}
