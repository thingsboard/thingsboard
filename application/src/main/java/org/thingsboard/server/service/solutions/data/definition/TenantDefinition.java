// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

@Data
public class TenantDefinition extends BaseEntityDefinition {

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}
