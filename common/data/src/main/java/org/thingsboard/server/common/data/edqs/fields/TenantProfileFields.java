// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class TenantProfileFields extends AbstractEntityFields {

    private boolean isDefault;

    public TenantProfileFields(UUID id, long createdTime, String name, boolean isDefault) {
        super(id, createdTime, TenantId.SYS_TENANT_ID.getId(), null, name, 0L);
        this.isDefault = isDefault;
    }
}
