// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;
import java.util.UUID;

@Data
@Slf4j
public class ProfileEntityIdInfo implements Serializable, HasTenantId {

    private static final long serialVersionUID = 8532058281983868003L;

    private final TenantId tenantId;
    private final EntityId ownerId;
    private final EntityId profileId;
    private final EntityId entityId;

    private ProfileEntityIdInfo(UUID tenantId, EntityId ownerId, EntityId profileId, EntityId entityId) {
        this.tenantId = TenantId.fromUUID(tenantId);
        this.ownerId = ownerId;
        this.profileId = profileId;
        this.entityId = entityId;
    }

    public static ProfileEntityIdInfo create(UUID tenantId, EntityId ownerId, DeviceProfileId profileId, DeviceId entityId) {
        return new ProfileEntityIdInfo(tenantId, ownerId, profileId, entityId);
    }

    public static ProfileEntityIdInfo create(UUID tenantId, EntityId ownerId, AssetProfileId profileId, AssetId entityId) {
        return new ProfileEntityIdInfo(tenantId, ownerId, profileId, entityId);
    }

}
