// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs.fields;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class AssetProfileFields extends AbstractEntityFields {

    private boolean isDefault;

    public AssetProfileFields(UUID id, long createdTime, UUID tenantId, String name, Long version, boolean isDefault) {
        super(id, createdTime, tenantId, null, name, version);
        this.isDefault = isDefault;
    }
}
