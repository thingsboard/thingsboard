// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WhiteLabelingCompositeKey implements Serializable {

    private UUID tenantId;
    private UUID customerId;
    private WhiteLabelingType type;

    public WhiteLabelingCompositeKey(TenantId tenantId, WhiteLabelingType type) {
        this(tenantId, null, type);
    }

    public WhiteLabelingCompositeKey(TenantId tenantId, CustomerId customerId, WhiteLabelingType type) {
        this.tenantId = tenantId.getId();
        this.customerId = customerId != null ? customerId.getId() : EntityId.NULL_UUID;
        this.type = type;
    }
}
