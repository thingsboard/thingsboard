// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.edge.EdgeProfileInfo;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class EdgeProfileInfoEntity extends EdgeProfileEntity {

    public EdgeProfileInfoEntity() {
        super();
    }

    public EdgeProfileInfoEntity(UUID id, long createdTime, Long version, UUID tenantId, String name, String description, boolean isDefault) {
        super();
        this.id = id;
        this.createdTime = createdTime;
        this.version = version;
        setTenantId(tenantId);
        setName(name);
        setDescription(description);
        setDefault(isDefault);
    }

    @Override
    public EdgeProfileInfo toData() {
        EdgeProfileInfo info = new EdgeProfileInfo(new EdgeProfileId(id));
        info.setCreatedTime(createdTime);
        info.setVersion(version);
        if (getTenantId() != null) {
            info.setTenantId(TenantId.fromUUID(getTenantId()));
        }
        info.setName(getName());
        info.setDescription(getDescription());
        info.setDefault(isDefault());
        return info;
    }

}
