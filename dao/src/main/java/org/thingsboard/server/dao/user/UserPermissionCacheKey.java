// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.user;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@EqualsAndHashCode
@RequiredArgsConstructor
public class UserPermissionCacheKey implements Serializable {

    private final TenantId tenantId;
    private final CustomerId customerId;
    private final EntityId userId;

    @Override
    public String toString() {
        return (tenantId != null ? tenantId.getId().toString() : "null") + "-" +
                (customerId != null ? customerId.getId().toString() : "null") + "-" +
                (userId != null ? userId.getId().toString() : "null") + "-";
    }

}
