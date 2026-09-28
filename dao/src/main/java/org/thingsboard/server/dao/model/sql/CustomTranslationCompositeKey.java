// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomTranslationCompositeKey implements Serializable {

    private UUID tenantId;
    private UUID customerId;
    private String localeCode;

    public CustomTranslationCompositeKey(TenantId tenantId, String localeCode) {
        this(tenantId, null, localeCode);
    }

    public CustomTranslationCompositeKey(TenantId tenantId, CustomerId customerId, String localeCode) {
        this.tenantId = tenantId.getId();
        this.customerId = customerId != null ? customerId.getId() : EntityId.NULL_UUID;
        this.localeCode = localeCode;
    }
}
