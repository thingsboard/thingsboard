// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@Data
public class EntitySubtype implements Serializable {

    private static final long serialVersionUID = 8057240243059922101L;

    private final TenantId tenantId;
    private final EntityType entityType;
    private final String type;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EntitySubtype{");
        sb.append("tenantId=").append(tenantId);
        sb.append(", entityType=").append(entityType);
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
