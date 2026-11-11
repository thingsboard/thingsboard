// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.menu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomMenuCacheKey implements HasTenantId {

    private TenantId tenantId;
    private CustomerId customerId;
    private UserId userId;

    public static CustomMenuCacheKey forUser(TenantId tenantId, CustomerId customerId, UserId userId) {
        return new CustomMenuCacheKey(tenantId, customerId, userId);
    }

    public static CustomMenuCacheKey forTenant(TenantId tenantId) {
        return new CustomMenuCacheKey(tenantId, null, null);
    }

}
