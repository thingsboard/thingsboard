// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.TenantFields;

import java.util.UUID;

public class TenantData extends BaseEntityData<TenantFields> {

    public TenantData(UUID entityId) {
        super(entityId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}
