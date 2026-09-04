// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.ApiUsageStateFields;

import java.util.UUID;

@ToString(callSuper = true)
public class ApiUsageStateData extends BaseEntityData<ApiUsageStateFields> {

    public ApiUsageStateData(UUID entityId) {
        super(entityId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.API_USAGE_STATE;
    }

    @Override
    public String getEntityName() {
        return getOwnerName();
    }

    @Override
    public String getOwnerName() {
        return repo.getOwnerEntityName(fields.getEntityId());
    }

}
